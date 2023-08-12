package com.ryuuta0217.furude.feature.tool;

import com.ryuuta0217.furude.FurudeCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.util.BlockUtil;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class RangedMining implements Listener {
    private static final NamespacedKey FACING_KEY = new NamespacedKey(FurudeCore.getInstance(), "ranged_mining_facing");
    private static final NamespacedKey RANGE_KEY = new NamespacedKey(FurudeCore.getInstance(), "ranged_mining_range");
    private static final NamespacedKey DIG_UNDER_KEY = new NamespacedKey(FurudeCore.getInstance(), "ranged_mining_dig_under");

    private final Map<UUID, Set<BlockPos>> ignoreEventPositions = new HashMap<>();

    @EventHandler
    public void onStartBlockBreak(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND) {
            if (event.getItem() != null && ModeSwitcher.getMode(event.getItem()) == DiggerToolMode.RANGED_MINING) {
                event.getPlayer().getPersistentDataContainer().set(FACING_KEY, PersistentDataType.STRING, event.getBlockFace().name());
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        ServerPlayer player = MinecraftAdapter.player(event.getPlayer());
        if (player == null) return;

        if (ignoreEventPositions.containsKey(player.getUUID())) {
            BlockPos blockPos = MinecraftAdapter.blockPos(event.getBlock().getLocation());
            if (ignoreEventPositions.get(player.getUUID()).contains(blockPos)) {
                ignoreEventPositions.get(player.getUUID()).remove(blockPos);
                return;
            }
        }

        if (!player.getBukkitEntity().getPersistentDataContainer().has(FACING_KEY, PersistentDataType.STRING)) return;

        ItemStack handItem = player.getMainHandItem();
        if (ModeSwitcher.getMode(handItem) != DiggerToolMode.RANGED_MINING) return;

        Direction direction = MinecraftAdapter.direction(BlockFace.valueOf(player.getBukkitEntity().getPersistentDataContainer().get(FACING_KEY, PersistentDataType.STRING)));
        player.getBukkitEntity().getPersistentDataContainer().remove(FACING_KEY);

        if (!ignoreEventPositions.containsKey(player.getUUID())) ignoreEventPositions.put(player.getUUID(), new HashSet<>());

        boolean digUnder = isDigUnder(handItem);

        Iterable<BlockPos> toBreak = withinManhattan(direction, MinecraftAdapter.blockPos(event.getBlock().getLocation()), getRange(handItem));
        toBreak.forEach(breakPos -> {
            if (!digUnder && breakPos.getY() < player.getY()) return;
            int remainingDurability = handItem.getMaxDamage() - handItem.getDamageValue();
            if (remainingDurability != 1) {
                ignoreEventPositions.get(player.getUUID()).add(breakPos);
                try {
                    player.gameMode.destroyBlock(breakPos);
                } finally {
                    ignoreEventPositions.get(player.getUUID()).remove(breakPos);
                }
            } else {
                player.getBukkitEntity().sendActionBar(Component.text("耐久値がなくなりました", NamedTextColor.RED));
            }
        });

        ignoreEventPositions.getOrDefault(player.getUUID(), Collections.emptySet()).clear();
    }

    public static int getRange(ItemStack stack) {
        return getRange(MinecraftAdapter.ItemStack.itemStack(stack));
    }

    public static int getRange(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || !stack.getItemMeta().getPersistentDataContainer().has(RANGE_KEY)) return 1;
        return stack.getItemMeta().getPersistentDataContainer().get(RANGE_KEY, PersistentDataType.INTEGER);
    }

    public static void setRange(ItemStack stack, int range) {
        setRange(MinecraftAdapter.ItemStack.itemStack(stack), range);
    }

    public static void setRange(org.bukkit.inventory.ItemStack stack, int range) {
        if (stack == null) return;
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(RANGE_KEY, PersistentDataType.INTEGER, range));
    }

    public static boolean isDigUnder(ItemStack stack) {
        return isDigUnder(MinecraftAdapter.ItemStack.itemStack(stack));
    }

    public static boolean isDigUnder(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || !stack.getItemMeta().getPersistentDataContainer().has(DIG_UNDER_KEY)) return false;
        return stack.getItemMeta().getPersistentDataContainer().get(DIG_UNDER_KEY, PersistentDataType.BOOLEAN);
    }

    public static void setDigUnder(ItemStack stack, boolean enabled) {
        setDigUnder(MinecraftAdapter.ItemStack.itemStack(stack), enabled);
    }

    public static void setDigUnder(org.bukkit.inventory.ItemStack stack, boolean enabled) {
        if (stack == null) return;
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(DIG_UNDER_KEY, PersistentDataType.BOOLEAN, enabled));
    }

    private static Iterable<BlockPos> withinManhattan(Direction direction, BlockPos center, int range) {
        int x = 0;
        int y = 0;
        int z = 0;

        if (direction == Direction.SOUTH || direction == Direction.NORTH) {
            x = range;
            y = range;
        }

        if (direction == Direction.EAST || direction == Direction.WEST) {
            y = range;
            z = range;
        }

        if (direction == Direction.UP || direction == Direction.DOWN) {
            x = range;
            z = range;
        }
        return BlockPos.withinManhattan(center, x, y, z);
    }
}
