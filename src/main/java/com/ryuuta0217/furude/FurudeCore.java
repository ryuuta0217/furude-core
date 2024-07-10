package com.ryuuta0217.furude;

import com.ryuuta0217.furude.chat.KanaConvert;
import com.ryuuta0217.furude.feature.SharpCommandListener;
import com.ryuuta0217.furude.feature.death.DeathPointActionbar;
import com.ryuuta0217.furude.feature.tool.ChainDestruction;
import com.ryuuta0217.furude.feature.tool.ModeSwitcher;
import com.ryuuta0217.furude.feature.tool.RangedMining;
import com.ryuuta0217.furude.managers.ListenerManager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ComposterBlock;
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

        if (!ComposterBlock.COMPOSTABLES.containsKey(Items.ROTTEN_FLESH)) {
            getLogger().info("ふーでさば: 腐肉をコンポスターで処分可能にしています...");
            ComposterBlock.COMPOSTABLES.put(Items.ROTTEN_FLESH, 0.5F);
            getLogger().info("ふーでさば: 腐肉をコンポスターで処分可能にしました！");
        }

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

        getLogger().info("ふーでさば: 破壊ツールモード切り替え用のリスナーを登録中...");
        ListenerManager.registerListener(new ModeSwitcher());
        getLogger().info("ふーでさば: 破壊ツールモード切り替え用のリスナーを登録しました！");

        getLogger().info("ふーでさば: 一括破壊用のブロック破壊リスナーを登録中...");
        ListenerManager.registerListener(new ChainDestruction());
        getLogger().info("ふーでさば: 一括破壊用のブロック破壊リスナーを登録しました！");

        getLogger().info("ふーでさば: 範囲破壊用のブロック破壊リスナーを登録中...");
        ListenerManager.registerListener(new RangedMining());
        getLogger().info("ふーでさば: 範囲破壊用のブロック破壊リスナーを登録しました！");

        getLogger().info("ふーでさば: 統合版向けにシャープでコマンドを実行可能にするチャットリスナーを登録中...");
        ListenerManager.registerListener(new SharpCommandListener());
        getLogger().info("ふーでさば: 統合版向けにシャープでコマンドを実行可能にするチャットリスナーを登録しました！");

        getLogger().info("ふーでさば: BungeeCordのプラグインチャンネルを登録中...");
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        getLogger().info("ふーでさば: BungeeCordのプラグインチャンネルを登録しました！");

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
}
