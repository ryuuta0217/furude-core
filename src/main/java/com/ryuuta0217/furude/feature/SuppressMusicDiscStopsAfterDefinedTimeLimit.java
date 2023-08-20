package com.ryuuta0217.furude.feature;

import org.bukkit.GameEvent;
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
        if (event.getEvent() == GameEvent.JUKEBOX_STOP_PLAY) {
            event.setCancelled(true);
        }
    }
}
