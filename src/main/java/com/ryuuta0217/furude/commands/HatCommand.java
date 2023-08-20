package com.ryuuta0217.furude.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

public class HatCommand {
    private HatCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("hat");
        builder.executes(ctx -> execute(ctx.getSource()));
        dispatcher.register(builder);
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
}
