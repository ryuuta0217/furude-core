package com.ryuuta0217.furude.feature;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Xbox Achievements を有効化した環境下では、/から始まるコマンドは実行できないため、代わりに#から始まる文字列を/に置き換えて実行します。
 */
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
