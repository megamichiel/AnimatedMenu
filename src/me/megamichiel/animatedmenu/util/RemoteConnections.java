package me.megamichiel.animatedmenu.util;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    boolean running = false;
    private Thread runningThread;

    public RemoteConnections(AnimatedMenuPlugin plugin) {
        this.plugin = plugin;
    }

    public void schedule(long delay)
    {
        running = true;
        this.delay = delay * 50;
        (runningThread = new Thread(this)).start();
    }
    
    public void cancel()
    {
        running = false;
        runningThread.interrupt();
    }
    
    @Override
    public void run() {
        while (running)
        {
            for (Entry<String, ServerInfo> entry : statuses.entrySet())
            {
                ServerInfo info = entry.getValue();
                try
                {
                    Socket socket = new Socket(info.address.getAddress(), info.address.getPort());
                    OutputStream output = socket.getOutputStream();
                    InputStream input = socket.getInputStream();
                    
                    // Handshake
                    ByteArrayOutputStream handshake = new ByteArrayOutputStream();
                    handshake.write(new byte[] { 0, 47 }); // Packet ID + Protocol Version
                    String address = info.address.getHostString();
                    byte[] bytes = address.getBytes(Charsets.UTF_8);
                    writeVarInt(handshake, bytes.length);
                    handshake.write(bytes);
                    int port = info.address.getPort();
                    handshake.write((port >> 8) & 0xFF);
                    handshake.write(port & 0xFF);
                    handshake.write(1); // Next protocol state: Status
                    writeVarInt(output, handshake.size());
                    output.write(handshake.toByteArray());
                    
                    // Request
                    output.write(new byte[] { 1, 0 }); // Length + Packet ID
                    
                    // Response
                    readVarInt(input);
                    if (input.read() != 0) // Weird Packet ID
                    {
                        socket.close();
                        continue;
                    }
                    byte[] jsonBytes = new byte[readVarInt(input)];
                    input.read(jsonBytes);
                    JsonElement response = new JsonParser().parse(new String(jsonBytes, Charsets.UTF_8));
                    String motd;
                    int online = 0, max = 0;
                    if (response.isJsonObject())
                    {
                        JsonObject obj = response.getAsJsonObject();
                        JsonElement desc = obj.get("description");
                        if (desc.isJsonObject())
                            motd = desc.getAsJsonObject().get("text").getAsString();
                        else motd = desc.getAsString();
                        JsonElement players = obj.get("players");
                        if (players.isJsonObject())
                        {
                            JsonObject playersObj = players.getAsJsonObject();
                            online = playersObj.get("online").getAsInt();
                            max = playersObj.get("max").getAsInt();
                        }
                    }
                    else motd = response.getAsString();
                    
                    info.online = true;
                    info.motd = motd;
                    info.onlinePlayers = online;
                    info.maxPlayers = max;

                    output.write(new byte[] { 9, 1 }); // Length + ID
                    long time = System.currentTimeMillis();
                    for (int i = 7; i >= 0; i--)
                        output.write((byte) (time >> (i * 8)) & 0xFF);

                    // input.skip(10); // Length (9) + ID (1) + Keep Alive ID
                    
                    socket.close();
                }
                catch (Exception ex)
                {
                    if (plugin.warnOfflineServers())
                    {
                        plugin.nag("Failed to connect to " + info.address.getHostName() + ":" + info.address.getPort() + "!");
                        plugin.nag(ex);
                    }
                }
            }
            try
            {
                Thread.sleep(delay);
            }
            catch (InterruptedException ex) {}
        }
    }
    
    private void writeVarInt(OutputStream output, int i) throws IOException
    {
        while ((i & -128) != 0) {
            output.write(i & 127 | 128);
            i >>>= 7;
        }
        
        output.write(i);
    }
    
    private int readVarInt(InputStream stream) throws IOException
    {
        int i = 0;
        int j = 0;
        
        int b0;
        
        do {
            b0 = stream.read();
            i |= (b0 & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((b0 & 128) == 128);
        
        return i;
    }
    
    public void clear()
    {
        statuses.clear();
    }
    
    public ServerInfo add(String name, InetSocketAddress address)
    {
        ServerInfo info = new ServerInfo(address);
        statuses.put(name, info);
        return info;
    }
    
    public ServerInfo get(String name)
    {
        return statuses.get(name);
    }

    public class ServerInfo {
        
        private final InetSocketAddress address;
        private final Map<StringBundle, StringBundle> values = new HashMap<>();
        private boolean online = false;
        private String motd = ChatColor.RED + "Offline";
        private int onlinePlayers = 0, maxPlayers = 0;

        public ServerInfo(InetSocketAddress address) {
            this.address = address;
        }

        public StringBundle get(String key, Player who)
        {
            for (Entry<StringBundle, StringBundle> entry : values.entrySet())
                if (entry.getKey().toString(who).equals(key))
                    return entry.getValue();
            return null;
        }

        public Map<StringBundle, StringBundle> getValues() {
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
    }
}
