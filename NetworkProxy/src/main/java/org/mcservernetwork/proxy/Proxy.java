package org.mcservernetwork.proxy;

import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.mcservernetwork.commons.NetworkAPI;
import org.mcservernetwork.commons.net.Channel;
import org.mcservernetwork.commons.net.NetworkLogger;
import org.mcservernetwork.commons.net.packet.PacketAccept;
import org.mcservernetwork.commons.net.packet.PacketPingPong;
import org.mcservernetwork.commons.net.packet.PacketTransferRequest;
import org.mcservernetwork.proxy.command.StatusCommand;
import org.mcservernetwork.proxy.io.ConfigReader;
import org.mcservernetwork.proxy.io.ResourceLoader;
import org.mcservernetwork.proxy.listener.StatusListener;
import org.mcservernetwork.proxy.listener.TransferRequestListener;

import java.io.*;

public class Proxy extends Plugin {

    private static final NetworkLogger logger = new NetworkLogger("PROXY");
    private static Configuration configuration;

    private static Proxy instance;

    public static Proxy getInstance() {
        return instance;
    }

    public static NetworkLogger getNetworkLogger() {
        return logger;
    }

    public static Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void onEnable() {
        instance = this;
        try {
            configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(
                    ResourceLoader.loadResource(this, "config.yml"));
        } catch (IOException e) {
            throw new RuntimeException("Could not load configuration file", e);
        }

        NetworkAPI.Internal.createNetwork(configuration.getString("connectionPattern"));

        getProxy().getPluginManager().registerCommand(this, new StatusCommand());

        logger.listen();

        logger.log("Loading sectors.", NetworkLogger.LogSeverity.INFO);
        ConfigReader.readSectors();

        NetworkAPI.Net.subscribeAndListen(Channel.VERIFY, PacketAccept.class, packet -> {
            packet.sectors = NetworkAPI.Sectors.getSectors();
            NetworkAPI.Net.publish(Channel.SECTOR(packet.sectorName), packet);
        });

        NetworkAPI.Net.subscribeAndListen(Channel.PONG, PacketPingPong.class, new StatusListener());
        NetworkAPI.Net.subscribeAndListen(Channel.TRANSFER_REQUEST, PacketTransferRequest.class, new TransferRequestListener());
    }
}