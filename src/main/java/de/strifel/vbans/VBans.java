package de.strifel.vbans;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import de.strifel.vbans.commands.CommandBan;
import de.strifel.vbans.commands.CommandKick;
import de.strifel.vbans.commands.CommandTempBan;
import org.slf4j.Logger;

import javax.inject.Inject;

@Plugin(id = "vbans", name="VBans", version="1.0-SNAPSHOT", description="Ban players! Its fun!")
public class VBans {

    private final ProxyServer server;

    @Inject
    public VBans(ProxyServer server, Logger logger) {
        this.server = server;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getCommandManager().register(new CommandKick(server), "kick", "vkick");
        server.getCommandManager().register(new CommandBan(server), "ban", "vban");
        server.getCommandManager().register(new CommandTempBan(server), "tban", "tempban", "vtempban", "vtban");
    }

}
