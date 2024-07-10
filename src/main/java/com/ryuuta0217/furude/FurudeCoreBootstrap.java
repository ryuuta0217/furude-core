package com.ryuuta0217.furude;

import com.mojang.brigadier.CommandDispatcher;
import com.ryuuta0217.furude.commands.*;
import io.papermc.paper.command.brigadier.ApiMirrorRootNode;
import io.papermc.paper.command.brigadier.PaperCommands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class FurudeCoreBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(@NotNull BootstrapContext ctx) {
        ctx.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, (e) -> {
            if (e.registrar() instanceof PaperCommands reg) {
                CommandBuildContext buildContext = reg.getBuildContext();
                CommandDispatcher<CommandSourceStack> minecraftDispatcher;
                if (reg.getDispatcherInternal().getRoot() instanceof ApiMirrorRootNode mirror) {
                    minecraftDispatcher = mirror.getDispatcher();

                    if (buildContext != null && minecraftDispatcher != null) {
                        ctx.getLogger().info("ふーでさば: ローマ字 -> ひらがな変換用のコマンドを登録中...");
                        HiraganaCommand.register(minecraftDispatcher);
                        ctx.getLogger().info("ふーでさば: ローマ字 -> ひらがな変換用のコマンドを登録しました！");

                        ctx.getLogger().info("ふーでさば: 拡張用のコマンドを登録中...");
                        EvaluateCommand.register(minecraftDispatcher);
                        ctx.getLogger().info("ふーでさば: 拡張用のコマンドを登録しました！");

                        ctx.getLogger().info("ふーでさば: 一括破壊用のコマンドを登録中...");
                        ChainDestructionCommand.register(minecraftDispatcher, buildContext);
                        ctx.getLogger().info("ふーでさば: 一括破壊用のコマンドを登録しました！");

                        ctx.getLogger().info("ふーでさば: 範囲破壊用のコマンドを登録中...");
                        RangedMiningCommand.register(minecraftDispatcher);
                        ctx.getLogger().info("ふーでさば: 範囲破壊用のコマンドを登録しました！");

                        ctx.getLogger().info("ふーでさば: アイテムを頭に被るコマンドを登録中...");
                        HatCommand.register(minecraftDispatcher);
                        ctx.getLogger().info("ふーでさば: アイテムを頭に被るコマンドを登録しました！");

                        ctx.getLogger().info("ふーでさば: サーバー移動コマンドを登録中...");
                        ServerCommand.register(minecraftDispatcher);
                        ctx.getLogger().info("ふーでさば: サーバー移動コマンドを登録しました！");
                    }
                }
            }
        });
    }
}
