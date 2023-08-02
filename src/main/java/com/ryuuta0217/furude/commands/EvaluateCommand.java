package com.ryuuta0217.furude.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.ryuuta0217.furude.managers.EvalManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.bukkit.entity.Player;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.ScriptableObject;

public class EvaluateCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("evaluate");
        builder.requires(source -> source.hasPermission(4));
        builder.then(Commands.literal("exec")
                        .then(Commands.argument("code", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ScriptableObject scope = EvalManager.getGlobalScope();

                                    // Minecraft
                                    scope.putConst("ctx", scope, ctx);
                                    scope.putConst("source", scope, ctx.getSource());
                                    scope.putConst("nmsPlayer", scope, ctx.getSource().getPlayer());
                                    scope.putConst("nmsEntity", scope, ctx.getSource().getEntity());

                                    // Bukkit
                                    scope.putConst("player", scope, ctx.getSource().getBukkitEntity() instanceof Player player ? player : null);
                                    scope.putConst("entity", scope, ctx.getSource().getBukkitEntity());
                                    scope.putConst("location", scope, ctx.getSource().getBukkitLocation());
                                    scope.putConst("sender", scope, ctx.getSource().getBukkitSender());
                                    scope.putConst("world", scope, ctx.getSource().getBukkitWorld());

                                    Object result = EvalManager.execFromString("EvaluateCommand", StringArgumentType.getString(ctx, "code"), scope);
                                    ctx.getSource().sendSuccess(() -> Component.literal("正常に完了しました: " + (result instanceof NativeJavaObject nativeJava ? nativeJava.unwrap() : result)), true);
                                    return (result instanceof Integer i ? i : (result instanceof NativeJavaObject java ? (java.unwrap() instanceof Integer i2 ? i2 : java.unwrap().hashCode()) : (result != null ? result.hashCode() : 0)));
                                })))
                .then(Commands.literal("defineFunc")
                        .then(Commands.argument("function", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    EvalManager.defineFunction("EvaluateCommand#defineFunc", StringArgumentType.getString(ctx, "function"));
                                    ctx.getSource().sendSuccess(() -> Component.literal("正常に完了しました"), true);
                                    return 0;
                                })));

        LiteralCommandNode<CommandSourceStack> originalTree = dispatcher.register(builder);

        LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = LiteralArgumentBuilder.literal("eval");
        aliasBuilder.requires(source -> source.hasPermission(4));
        aliasBuilder.redirect(originalTree);
        dispatcher.register(aliasBuilder);
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        // Use Rhino to exec script:

        return 0;
    }
}
