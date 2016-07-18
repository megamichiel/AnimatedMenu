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
        if (runningThread != null)
            runningThread.interrupt();
    }
    
    @Override
    public void run() {
        while (running) {
            for (Entry<String, ServerInfo> entry : statuses.entrySet()) {
                ServerInfo info = entry.getValue();
                try {
                    Socket socket = new Socket(info.address.getAddress(), info.address.getPort());
                    OutputStream output = socket.getOutputStream();
                    InputStream input = socket.getInputStream();
                    
                    // Handshake

                    String address = info.address.getHostString();
                    byte[] addressBytes = address.getBytes(Charsets.UTF_8);

                    ByteArrayOutputStream handshake = new ByteArrayOutputStream();
                    writeVarInt(handshake, 5
                            + varIntLength(addressBytes.length)
                            + addressBytes.length);
                    handshake.write(new byte[] { 0, 47 }); // Packet ID + Protocol Version
                    writeVarInt(handshake, addressBytes.length);
                    handshake.write(addressBytes);
                    int port = info.address.getPort();
                    handshake.write((port >> 8) & 0xFF);
                    handshake.write(port & 0xFF);
                    handshake.write(1); // Next protocol state: Status
                    output.write(handshake.toByteArray());
                    
                    // Request
                    output.write(new byte[] { 1, 0 }); // Length + Packet ID
                    
                    // Response
                    readVarInt(input);
                    if (input.read() != 0) { // Weird Packet ID
                        socket.close();
                        continue;
                    }
                    byte[] jsonBytes = new byte[readVarInt(input)];
                    new DataInputStream(input).readFully(jsonBytes);
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

                    byte[] write = new byte[10];
                    write[0] = 9;
                    write[1] = 1;
                    int index = 2;
                    long time = System.currentTimeMillis();
                    for (int i = 7; i >= 0; i--)
                        write[index++] = (byte) ((time >> (i * 8)) & 0xFF);
                    output.write(write);

                    // input.skip(10); // Length (9) + ID (1) + Keep Alive ID
                    
                    socket.close();
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
            } catch (InterruptedException ex) {}
        }
    }
    
    private void writeVarInt(OutputStream output, int i) throws IOException {
        while ((i & -0x80) != 0) {
            output.write(i & 0x7F | 0x80);
            i >>>= 7;
        }
        
        output.write(i);
    }

    private int varIntLength(int i) {
        for (int j = 1; j < 5; j++)
            if ((i & (0xffffffff << (j * 7))) == 0)
                return j;
        return 5;
    }
    
    private int readVarInt(InputStream stream) throws IOException {
        int i = 0;
        int j = 0;
        
        int b0;
        
        do {
            b0 = stream.read();
            i |= (b0 & 0x7F) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b0 & 0x80) == 0x80);
        
        return i;
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
        private final Map<IPlaceholder<String>, IPlaceholder<String>> values = new HashMap<>();
        private boolean online = false;
        private String motd = ChatColor.RED + "Offline";
        private int onlinePlayers = 0, maxPlayers = 0;

        public ServerInfo(InetSocketAddress address) {
            this.address = address;
        }

        public IPlaceholder<String> get(String key, Player who) {
            for (Entry<IPlaceholder<String>, IPlaceholder<String>> entry : values.entrySet())
                if (entry.getKey().invoke(plugin, who).equals(key))
                    return entry.getValue();
            return null;
        }

        public Map<IPlaceholder<String>, IPlaceholder<String>> getValues() {
            return values;
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
            for (String key : section.keys()) {
                if (!key.equals("ip")) {
                    String val = section.getString(key);
                    if (val != null)
                        values.put(
                                StringBundle.parse(plugin, key).colorAmpersands().tryCache(),
                                StringBundle.parse(plugin, val).colorAmpersands().tryCache()
                        );
                }
            }
        }
    }
}
