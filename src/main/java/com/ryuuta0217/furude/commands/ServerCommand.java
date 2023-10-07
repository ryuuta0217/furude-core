package com.ryuuta0217.furude.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ryuuta0217.furude.FurudeCore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ServerCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("server");
        builder.then(Commands.argument("name", StringArgumentType.string())
                .executes(ctx -> execute(ctx, StringArgumentType.getString(ctx, "name"))));
        dispatcher.register(builder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx, String name) throws CommandSyntaxException {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(name);
        ctx.getSource().getPlayerOrException().getBukkitEntity().sendPluginMessage(FurudeCore.getInstance(), "BungeeCord", out.toByteArray());
        return 0;
    }
}
