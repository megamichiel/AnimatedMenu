package me.megamichiel.animatedmenu.util;

import com.google.common.base.Charsets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.megamichiel.animatedmenu.AnimatedMenuPlugin;
import me.megamichiel.animationlib.config.ConfigSection;
import me.megamichiel.animationlib.placeholder.IPlaceholder;
import me.megamichiel.animationlib.placeholder.StringBundle;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteConnections implements Runnable {

    private static final int PROTOCOL_VERSION = 335, PROTOCOL_VERSION_LENGTH = varIntLength(PROTOCOL_VERSION); // 1.12
    private static final byte[] PING = new byte[] { 9, 1, 0, 0, 0, 0, 0, 0, 0, 0 };
    
    private final AnimatedMenuPlugin plugin;

    private final Map<String, IServerInfo> statuses = new ConcurrentHashMap<>();
    private final Map<InetSocketAddress, Connection> connections = new ConcurrentHashMap<>();

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
        statuses.clear();
        connections.clear();
    }
    
    @Override
    public void run() {
        while (running) {
            connections.forEach((address, connection) -> {
                try (Socket socket = new Socket(address.getAddress(), address.getPort())) {
                    OutputStream output = socket.getOutputStream();
                    InputStream input = socket.getInputStream();

                    output.write(connection.handshake);

                    // Response
                    readVarInt(input);
                    if (input.read() != 0) { // Weird Packet ID. Don't want it anymore
                        return;
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
                    } else {
                        motd = response.getAsString();
                    }

                    connection.online = true;
                    connection.failed = false;
                    connection.motd = motd;
                    connection.onlinePlayers = online;
                    connection.maxPlayers = max;

                    output.write(PING);

                    // input.skip(10); // Length (9) + ID (1) + Keep Alive ID (eight 0s)
                } catch (Exception ex) {
                    if (!connection.failed) {
                        plugin.nag("Failed to connect to " + address.getHostName() + ':' + address.getPort() + '!');
                        plugin.nag(ex);

                        connection.failed = true;
                    }
                    connection.online = false;
                }
            });
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                // Meh, just keep going
            }
        }
    }
    
    private static void writeVarInt(ByteArrayOutputStream output, int i) {
        while ((i & 0xFFFFFF80) != 0) {
            output.write(i & 0x7F | 0x80);
            i >>>= 7;
        }
        
        output.write(i);
    }

    private static int varIntLength(int i) {
        for (int j = 1; j < 5; ++j) {
            if ((i & (0xFFFFFFFF << (j * 7))) == 0) {
                return j;
            }
        }
        return 5;
    }
    
    private static int readVarInt(InputStream stream) throws IOException {
        int value = 0, offset = 0, read;
        
        do {
            if ((read = stream.read()) == -1) {
                throw new EOFException();
            }
            value |= (read & 0x7F) << offset++ * 7;
            if (offset > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while ((read & 0x80) == 0x80);
        
        return value;
    }
    
    public void add(String name, InetSocketAddress address, ConfigSection config) {
        statuses.put(name, new ServerInfo(connections.computeIfAbsent(address, Connection::new), config));
    }
    
    public IServerInfo get(String name, boolean create) {
        return statuses.computeIfAbsent(name, $name -> {
            if (create) {
                int index = name.lastIndexOf(':');
                InetSocketAddress address;
                try {
                    address = new InetSocketAddress(
                            InetAddress.getByName(index == -1 ? name : name.substring(0, index)),
                            index == -1 ? 25565 : Integer.parseInt(name.substring(index + 1))
                    );
                } catch (UnknownHostException | NumberFormatException ex) {
                    return null;
                }
                return new IServerInfo() {

                    final Connection connection = connections.computeIfAbsent(address, Connection::new);

                    @Override
                    public String get(String key, Player player, String def) {
                        return def;
                    }

                    @Override
                    public boolean isOnline() {
                        return connection.online;
                    }

                    @Override
                    public String getMotd() {
                        return connection.motd;
                    }

                    @Override
                    public int getOnlinePlayers() {
                        return connection.onlinePlayers;
                    }

                    @Override
                    public int getMaxPlayers() {
                        return connection.maxPlayers;
                    }
                };
            }
            return null;
        });
    }

    private class Connection {

        private final byte[] handshake;

        private boolean online, failed;
        private String motd = ChatColor.RED + "Offline";
        private int onlinePlayers, maxPlayers;

        Connection(InetSocketAddress address) {
            byte[] addressBytes = address.getHostString().getBytes(Charsets.UTF_8);

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            int port = address.getPort();

            // Handshake

            writeVarInt(bytes, 4 + PROTOCOL_VERSION_LENGTH + varIntLength(addressBytes.length) + addressBytes.length);
            bytes.write(0);
            writeVarInt(bytes, PROTOCOL_VERSION);
            writeVarInt(bytes, addressBytes.length);
            bytes.write(addressBytes, 0, addressBytes.length);
            bytes.write((port >> 8) & 0xFF);
            bytes.write(port & 0xFF);
            bytes.write(1); // Next protocol state: Status

            bytes.write(new byte[] { 1, 0 }, 0, 2); // Status Request

            handshake = bytes.toByteArray();
        }
    }

    public interface IServerInfo {

        String get(String key, Player player, String def);

        boolean isOnline();

        String getMotd();

        int getOnlinePlayers();

        int getMaxPlayers();
    }

    private class ServerInfo implements IServerInfo {

        private final Connection connection;
        private final Map<String, IPlaceholder<String>> fastValues = new HashMap<>();
        private final Map<StringBundle, IPlaceholder<String>> values = new HashMap<>();

        ServerInfo(Connection connection, ConfigSection config) {
            this.connection = connection;

            String key;
            for (Map.Entry<String, Object> entry : config.values().entrySet()) {
                if (!"ip".equals(key = entry.getKey())) {
                    IPlaceholder<String> value = StringBundle.parse(plugin, entry.getValue().toString()).colorAmpersands().tryCache();
                    switch (key) {
                        case "online":case "offline":case "default":
                            fastValues.put(key, value);
                            break;
                        default:
                            StringBundle keySB = StringBundle.parse(plugin, config.getOriginalKey(key)).colorAmpersands();
                            if (keySB.containsPlaceholders()) {
                                values.put(keySB, value);
                            } else {
                                fastValues.put(keySB.toString(null), value);
                            }
                            break;
                    }
                }
            }
        }

        @Override
        public String get(String key, Player player, String def) {
            IPlaceholder<String> val = fastValues.get(key);
            if (val != null) {
                return val.invoke(plugin, player);
            }
            for (Entry<StringBundle, IPlaceholder<String>> entry : values.entrySet()) {
                if (entry.getKey().toString(player).equals(key)) {
                    return entry.getValue().invoke(plugin, player);
                }
            }
            return def;
        }

        @Override
        public boolean isOnline() {
            return connection.online;
        }

        @Override
        public String getMotd() {
            return connection.motd;
        }

        @Override
        public int getOnlinePlayers() {
            return connection.onlinePlayers;
        }

        @Override
        public int getMaxPlayers() {
            return connection.maxPlayers;
        }
    }
}
