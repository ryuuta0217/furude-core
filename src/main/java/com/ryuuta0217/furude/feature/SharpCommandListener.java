package com.ryuuta0217.furude.feature;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class SharpCommandListener implements Listener {
    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).startsWith("#")) {
            String[] command = PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).substring(1).split(" ");

            if (Bukkit.getCommandMap().getCommand(command[0]) != null) {
                event.setCancelled(true);
                event.getPlayer().chat("/" + String.join(" ", command));
            }
        }
    }
}
