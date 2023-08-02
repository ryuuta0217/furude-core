package com.ryuuta0217.furude.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ryuuta0217.furude.chat.KanaConvert;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class HiraganaCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("hiragana");
        builder.requires(source -> source.hasPermission(0));

        builder.then(Commands.literal("on")
                        .executes(ctx -> setHiraganaMode(ctx, true)))
                .then(Commands.literal("off")
                        .executes(ctx -> setHiraganaMode(ctx, false)));

        dispatcher.register(builder);
    }

    private static int setHiraganaMode(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        if (enabled && KanaConvert.isKanaConvertEnabled(player.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("ローマ字 -> ひらがな 変換はすでに有効になっています"));
            return 1;
        }

        if (!enabled && !KanaConvert.isKanaConvertEnabled(player.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("ローマ字 -> ひらがな 変換はすでに無効になっています"));
            return 2;
        }

        KanaConvert.setKanaConvertEnabled(player.getUUID(), enabled);
        ctx.getSource().sendSuccess(() -> Component.literal("ローマ字 -> ひらがな 変換を" + (enabled ? "有効" : "無効") + "にしました").withStyle(ChatFormatting.GREEN), true);
        return 0;
    }
}
