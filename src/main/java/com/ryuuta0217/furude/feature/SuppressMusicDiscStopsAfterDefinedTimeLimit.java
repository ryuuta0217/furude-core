package com.ryuuta0217.furude.feature;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.GameEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.GenericGameEvent;

/**
 * 音楽ディスクが事前定義された長さで停止してしまうのを抑制します。
 * これにより、ジュークボックスを使用した一部のレッドストーン回路は動かなくなる可能性があります。
 */
public class SuppressMusicDiscStopsAfterDefinedTimeLimit implements Listener {
    @EventHandler
    public void onGenericGameEvent(GenericGameEvent event) {
        if (event.getEvent() == GameEvent.JUKEBOX_PLAY) {
            Block block = event.getLocation().getBlock();
            if (block.getType() == Material.JUKEBOX) {
                BlockState state = MinecraftAdapter.blockState(block);
                if (state.hasBlockEntity()) {
                    BlockEntity entity = MinecraftAdapter.level(block.getWorld()).getBlockEntity(MinecraftAdapter.blockPos(event.getLocation()));
                    if (entity instanceof JukeboxBlockEntity jukebox) {
                        jukebox.isPlaying = false;
                    }
                }
            }
        }
    }
}
