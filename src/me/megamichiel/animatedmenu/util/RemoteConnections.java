package me.megamichiel.animatedmenu.util;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.config.AbstractConfig;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteConnections implements Runnable {
    
    private final AnimatedMenuPlugin plugin;
    private final Map<String, ServerInfo> statuses = new ConcurrentHashMap<>();
    private long delay;
    private boolean running = false;
    private Thread runningThread;

    public RemoteConnections(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    public void schedule(long delay) {
        running = true;
        this.delay = delay * 50;
        (runningThread = new Thread(this)).start();
    }
    
    public void cancel() {
        running = false;
        if (runningThread != null) {
            runningThread.interrupt();
        }
    }
    
    @Override
    public void run() {
        while (running) {
            for (Entry<String, ServerInfo> entry : statuses.entrySet()) {
                ServerInfo info = entry.getValue();
                try (Socket socket = new Socket(info.address.getAddress(), info.address.getPort())) {
                    OutputStream output = socket.getOutputStream();
                    InputStream input = socket.getInputStream();

                    output.write(info.handshake);
                    
                    // Response
                    readVarInt(input);
                    if (input.read() != 0) { // Weird Packet ID
                        continue;
                    }
                    int length = readVarInt(input), pos = 0;
                    byte[] jsonBytes = new byte[length];

                    // readFully
                    while (pos < length) {
                        if (pos > (pos += input.read(jsonBytes, pos, length - pos))) {
                            throw new EOFException();
                        }
                    }

                    JsonElement response = new JsonParser().parse(new String(jsonBytes, Charsets.UTF_8));
                    String motd;
                    int online = 0, max = 0;
                    if (response.isJsonObject()) {
                        JsonObject obj = response.getAsJsonObject();
                        JsonElement desc = obj.get("description");
                        if (desc.isJsonObject())
                            motd = desc.getAsJsonObject().get("text").getAsString();
                        else motd = desc.getAsString();
                        JsonElement players = obj.get("players");
                        if (players.isJsonObject()) {
                            JsonObject playersObj = players.getAsJsonObject();
                            online = playersObj.get("online").getAsInt();
                            max = playersObj.get("max").getAsInt();
                        }
                    } else motd = response.getAsString();
                    
                    info.online = true;
                    info.motd = motd;
                    info.onlinePlayers = online;
                    info.maxPlayers = max;

                    byte[] ping = info.ping;
                    int index = 10;
                    long time = System.currentTimeMillis();
                    while (index > 1) {
                        ping[--index] = (byte) (time & 0xFFL);
                        time >>>= 8;
                    }
                    output.write(ping);

                    // input.skip(10); // Length (9) + ID (1) + Keep Alive ID
                } catch (Exception ex) {
                    if (plugin.warnOfflineServers()) {
                        plugin.nag("Failed to connect to " + info.address.getHostName() + ":" + info.address.getPort() + "!");
                        plugin.nag(ex);
                    }
                    info.online = false;
                }
            }
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                // Meh, just keep going
            }
        }
    }
    
    private void writeVarInt(ByteArrayOutputStream output, int i) {
        while ((i & -0x80) != 0) {
            output.write(i & 0x7F | 0x80);
            i >>>= 7;
        }
        
        output.write(i);
    }

    private int varIntLength(int i) {
        for (int j = 1; j < 5; j++)
            if ((i & (0xFFFFFFFF << (j * 7))) == 0)
                return j;
        return 5;
    }
    
    private int readVarInt(InputStream stream) throws IOException {
        int value = 0, offset = 0, read;
        
        do {
            read = stream.read();
            value |= (read & 0x7F) << offset++ * 7;
            if (offset > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((read & 0x80) == 0x80);
        
        return value;
    }
    
    public void clear() {
        statuses.clear();
    }
    
    public ServerInfo add(String name, InetSocketAddress address) {
        ServerInfo info = new ServerInfo(address);
        statuses.put(name, info);
        return info;
    }
    
    public ServerInfo get(String name) {
        return statuses.get(name);
    }

    public class ServerInfo {
        
        private final InetSocketAddress address;
        private final byte[] handshake, ping;
        private final Map<String, IPlaceholder<String>> cached = new HashMap<>();
        private final Map<StringBundle, IPlaceholder<String>> values = new HashMap<>();
        private boolean online = false;
        private String motd = ChatColor.RED + "Offline";
        private int onlinePlayers = 0, maxPlayers = 0;

        private ServerInfo(InetSocketAddress address) {
            this.address = address;

            String hostString = address.getHostString();
            byte[] addressBytes = hostString.getBytes(Charsets.UTF_8);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            // Handshake
            writeVarInt(bytes, 5
                    + varIntLength(addressBytes.length)
                    + addressBytes.length);
            bytes.write(new byte[] { 0, 47 }, 0, 2); // Packet ID + Protocol Version
            writeVarInt(bytes, addressBytes.length);
            bytes.write(addressBytes, 0, addressBytes.length);
            int port = address.getPort();
            bytes.write((port >> 8) & 0xFF);
            bytes.write(port & 0xFF);
            bytes.write(1); // Next protocol state: Status

            // Status Request
            bytes.write(new byte[] { 1, 0 }, 0, 2);

            handshake = bytes.toByteArray();
            ping = new byte[10];
            ping[0] = 9;
            ping[1] = 1;
        }

        public String get(String key, Player player) {
            IPlaceholder<String> val = cached.get(key);
            if (val != null) return val.invoke(plugin, player);
            for (Entry<StringBundle, IPlaceholder<String>> entry : values.entrySet())
                if (entry.getKey().toString(player).equals(key))
                    return entry.getValue().invoke(plugin, player);
            return null;
        }

        public boolean isOnline() {
            return online;
        }

        public String getMotd() {
            return motd;
        }

        public int getOnlinePlayers() {
            return onlinePlayers;
        }

        public int getMaxPlayers() {
            return maxPlayers;
        }

        public void load(AbstractConfig section) {
            String val;
            for (String key : section.keys()) {
                if ("ip".equals(key)) continue;
                val = section.getString(key);
                if (val == null) continue;
                IPlaceholder<String> value = StringBundle.parse(plugin, val).colorAmpersands().tryCache();
                switch (key) {
                    case "online":case "offline":case "default":
                        cached.put(key, value);
                        break;
                    default:
                        StringBundle keySB = StringBundle.parse(plugin, section.getOriginalKey(key)).colorAmpersands();
                        if (keySB.containsPlaceholders()) values.put(keySB, value);
                        else cached.put(keySB.toString(null), value);
                        break;
                }
            }
        }
    }
}
