package com.ryuuta0217.furude.feature.death;

import com.ryuuta0217.furude.FurudeCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathPointActionbar extends BukkitRunnable {
    private static final Map<UUID, BukkitTask> TASKS = new HashMap<>();

    private final UUID whoDied;
    private final String deathWorld;
    private final double x, y, z;
    private long dropItemDespawnTime = 20 * 60 * 5; // 5分
    private long cancelTime = 20 * 60 * 8; // 8分

    private DeathPointActionbar(UUID whoDied, Location deathLocation) {
        this.whoDied = whoDied;
        this.deathWorld = deathLocation.getWorld().getName();
        this.x = deathLocation.getX();
        this.y = deathLocation.getY();
        this.z = deathLocation.getZ();
    }

    @Override
    public void run() {
        if (this.cancelTime <= 0) {
            this.cancel();
            // do not return in here, finally tick will be executed
        }

        boolean isItemDespawned = this.dropItemDespawnTime <= 0;

        boolean isDeathWorldFound = Bukkit.getWorld(this.deathWorld) != null;

        if (isDeathWorldFound) {
            Location deathLocation = new Location(Bukkit.getWorld(this.deathWorld), this.x, this.y, this.z);

            if (!isItemDespawned && deathLocation.isWorldLoaded() && deathLocation.isChunkLoaded()) {
                this.dropItemDespawnTime--;
                this.cancelTime--; // Synchronize the cancel time until the item despawn.
            } else if (isItemDespawned) {
                this.cancelTime--; // After the item despawned, subtract every tick
            }

            Player player = Bukkit.getPlayer(this.whoDied);

            if (player != null) {
                boolean isDiffWorld = !player.getWorld().getName().equals(this.deathWorld);

                double distance = isDiffWorld ? -1 : player.getLocation().distance(deathLocation);
                if (!isDiffWorld && !player.isDead() && distance < 3) {
                    player.sendActionBar(Component.text("死亡地点に到着しました。お疲れ様でした。", NamedTextColor.GREEN));
                    this.cancel();
                } else {
                    Component textSeparator = Component.text(" | ", NamedTextColor.GRAY);
                    Component deathLocationText = Component.text("死亡地点: (" + (int) this.x + "," + (int) this.y + "," + (int) this.z + ")", NamedTextColor.RED);
                    Component deathLocationDistanceText = Component.text("死亡地点まで: " + (isDiffWorld ? "?m" : (int) distance + "m"), NamedTextColor.YELLOW);
                    Component dropItemDespawnRemainTime = isItemDespawned ? Component.text("ｱｲﾃﾑは消滅済", NamedTextColor.RED) : Component.text("ｱｲﾃﾑﾛｽﾄまで: " + (int) (this.dropItemDespawnTime / 20) + "秒", NamedTextColor.AQUA);

                    Component message = Component.empty()
                            .append(deathLocationText)
                            .append(textSeparator)
                            .append(deathLocationDistanceText)
                            .append(textSeparator)
                            .append(dropItemDespawnRemainTime);

                    player.sendActionBar(message);
                }
            }
        } else {
            this.cancel();
        }
    }

    public static class Listener implements org.bukkit.event.Listener {
        @EventHandler(priority = EventPriority.MONITOR)
        public void onDeath(PlayerDeathEvent event) {
            if (TASKS.containsKey(event.getPlayer().getUniqueId())) TASKS.get(event.getPlayer().getUniqueId()).cancel();
            TASKS.put(event.getPlayer().getUniqueId(), new DeathPointActionbar(event.getPlayer().getUniqueId(), event.getPlayer().getLocation()).runTaskTimerAsynchronously(FurudeCore.getInstance(), 0L, 1L));
        }
    }
}
