package com.ryuuta0217.furude.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ryuuta0217.furude.managers.ListenerManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v1_20_R1.entity.CraftPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public class HatCommand implements Listener {
    private static final HatCommand INSTANCE = new HatCommand();

    private HatCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("hat");
        builder.executes(ctx -> execute(ctx.getSource()));
        dispatcher.register(builder);
        HandlerList.unregisterAll(INSTANCE);
        ListenerManager.registerListener(INSTANCE);
    }

    private static int execute(CommandSourceStack source) throws CommandSyntaxException {
        ItemStack currentHead = source.getPlayerOrException().getInventory().getItem(39);
        ItemStack currentHand = source.getPlayerOrException().getMainHandItem();

        // Set Hand Item to head item
        source.getPlayerOrException().setItemInHand(InteractionHand.MAIN_HAND, currentHead);

        // Set Head Item to hand item (slot is Minecraft format)
        source.getPlayerOrException().getInventory().setItem(39, currentHand);
        source.sendSuccess(() -> net.minecraft.network.chat.Component.literal("アイテムを頭に被りました"), true);
        return 0;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (PlainTextComponentSerializer.plainText().serialize(event.originalMessage()).equals("#hat")) {
            event.setCancelled(true);
            try {
                execute(((CraftPlayer) event.getPlayer()).getHandle().createCommandSourceStack());
            } catch(CommandSyntaxException e) {
                event.getPlayer().sendMessage(Component.text("コマンドの実行中にエラーが発生しました: " + e.getMessage(), NamedTextColor.RED));
            }
        }
    }
}
