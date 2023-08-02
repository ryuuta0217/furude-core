package com.ryuuta0217.furude.chat;

import com.ryuuta0217.furude.util.YukiKanaConverter;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class KanaConvert implements Listener {
    private static final Set<UUID> DISABLED_PLAYERS = new HashSet<>();

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncChatEvent event) {
        if (DISABLED_PLAYERS.contains(event.getPlayer().getUniqueId())) {
            return;
        }

        ChatRenderer originalRenderer = event.renderer();
        event.renderer(((source, displayName, originalMessage, viewer) -> {
            Component message = originalMessage;
            String plainOriginalMessage = PlainTextComponentSerializer.plainText().serialize(originalMessage);
            if (YukiKanaConverter.isNeedToJapanize(plainOriginalMessage)) {
                message = Component.text(YukiKanaConverter.conv(plainOriginalMessage))
                        .appendSpace()
                        .append(Component.text("(" + plainOriginalMessage + ")", NamedTextColor.GRAY, TextDecoration.ITALIC));
            }
            return originalRenderer.render(source, displayName, message, viewer);
        }));
    }

    public static boolean isKanaConvertEnabled(UUID playerUUID) {
        return !DISABLED_PLAYERS.contains(playerUUID);
    }

    public static void setKanaConvertEnabled(UUID playerUUID, boolean enabled) {
        if (enabled) {
            DISABLED_PLAYERS.remove(playerUUID);
        } else {
            DISABLED_PLAYERS.add(playerUUID);
        }
    }
}
