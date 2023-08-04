package com.ryuuta0217.furude;

import com.mojang.brigadier.CommandDispatcher;
import com.ryuuta0217.furude.chat.KanaConvert;
import com.ryuuta0217.furude.commands.EvaluateCommand;
import com.ryuuta0217.furude.commands.HiraganaCommand;
import com.ryuuta0217.furude.feature.death.DeathPointActionbar;
import com.ryuuta0217.furude.feature.tool.ChainDestruction;
import com.ryuuta0217.furude.managers.ListenerManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

public final class FurudeCore extends JavaPlugin {
    private static FurudeCore INSTANCE;

    @Override
    public void onLoad() {
        long start = System.nanoTime();
        getLogger().info("ふーで鯖のプラグイン、よみこみちゅー！");
        INSTANCE = this;

        getLogger().info("ふーでさば: ローマ字 -> ひらがな変換用のコマンドを登録中...");
        HiraganaCommand.register(getCommandDispatcher());
        getLogger().info("ふーでさば: ローマ字 -> ひらがな変換用のコマンドを登録しました！");

        getLogger().info("ふーでさば: 拡張用のコマンドを登録中...");
        EvaluateCommand.register(getCommandDispatcher());
        getLogger().info("ふーでさば: 拡張用のコマンドを登録しました！");

        getLogger().info("ふーで鯖のプラグイン、よみこみかんりょう！ (" + ((System.nanoTime() - start) / 1000000) + "ミリ秒かかったよ)");
    }

    @Override
    public void onEnable() {
        long start = System.nanoTime();
        getLogger().info("ふーで鯖のプラグイン、有効化ちゅー！");

        getLogger().info("ふーでさば: ローマ字 -> ひらがな変換用のチャットリスナーを登録中...");
        ListenerManager.registerListener(new KanaConvert());
        getLogger().info("ふーでさば: ローマ字 -> ひらがな変換用のチャットリスナーを登録しました！");

        getLogger().info("ふーでさば: 死亡地点表示用のアクションバーリスナーを登録中...");
        ListenerManager.registerListener(new DeathPointActionbar.Listener());
        getLogger().info("ふーでさば: 死亡地点表示用のアクションバーリスナーを登録しました！");

        getLogger().info("ふーでさば: 一括破壊用のブロック破壊リスナーを登録中...");
        ListenerManager.registerListener(new ChainDestruction());
        getLogger().info("ふーでさば: 一括破壊用のブロック破壊リスナーを登録しました！");

        getLogger().info("ふーで鯖のプラグイン、有効化かんりょう！ (" + ((System.nanoTime() - start) / 1000000) + "ミリ秒かかったよ)");
    }

    @Override
    public void onDisable() {
        getLogger().info("ふーで鯖のプラグイン、無効化ちゅー！");
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
        getLogger().info("ふーで鯖のプラグイン、無効化かんりょう！");
    }

    public static FurudeCore getInstance() {
        return INSTANCE;
    }

    public static CommandDispatcher<CommandSourceStack> getCommandDispatcher() {
        return MinecraftServer.getServer().vanillaCommandDispatcher.getDispatcher();
    }
}
