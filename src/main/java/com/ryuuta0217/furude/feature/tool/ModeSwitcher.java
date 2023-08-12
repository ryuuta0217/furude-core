package com.ryuuta0217.furude.feature.tool;

import com.ryuuta0217.furude.FurudeCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;

public class ModeSwitcher implements Listener {
    private static final NamespacedKey TOOL_MODE_KEY = new NamespacedKey(FurudeCore.getInstance(), "tool_mode");

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHeldChanged(PlayerItemHeldEvent event) {
        boolean isWheel = event.getNewSlot() == event.getPreviousSlot() + 1 || event.getNewSlot() == event.getPreviousSlot() - 1 || (event.getNewSlot() == 0 && event.getPreviousSlot() == 8) || (event.getNewSlot() == 8 && event.getPreviousSlot() == 0);
        boolean isPrevious = event.getNewSlot() == event.getPreviousSlot() - 1 || (event.getNewSlot() == 8 && event.getPreviousSlot() == 0);
        boolean isNext = event.getNewSlot() == event.getPreviousSlot() + 1 || (event.getNewSlot() == 0 && event.getPreviousSlot() == 8);
        if (!isWheel || !event.getPlayer().isSneaking()) return;

        org.bukkit.inventory.ItemStack selectedBukkitItem = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
        if (selectedBukkitItem == null) return;

        ItemStack selectedItem = MinecraftAdapter.ItemStack.itemStack(selectedBukkitItem);
        if (selectedItem == null || (!(selectedItem.getItem() instanceof DiggerItem))) return;

        DiggerToolMode currentMode = getMode(selectedItem);
        DiggerToolMode previousMode = DiggerToolMode.values()[(currentMode.ordinal() + DiggerToolMode.values().length - 1) % DiggerToolMode.values().length];
        DiggerToolMode nextMode = DiggerToolMode.values()[(currentMode.ordinal() + 1) % DiggerToolMode.values().length];

        if (isPrevious) setMode(selectedItem, previousMode, event.getPlayer());
        if (isNext) setMode(selectedItem, nextMode, event.getPlayer());
        event.setCancelled(true);
    }

    public static DiggerToolMode getMode(ItemStack stack) {
        return getMode(MinecraftAdapter.ItemStack.itemStack(stack));
    }

    public static DiggerToolMode getMode(org.bukkit.inventory.ItemStack stack) {
        if (stack == null || !stack.hasItemMeta() || !stack.getItemMeta().getPersistentDataContainer().has(TOOL_MODE_KEY, PersistentDataType.STRING)) return DiggerToolMode.OFF;
        return DiggerToolMode.valueOf(stack.getItemMeta().getPersistentDataContainer().get(TOOL_MODE_KEY, PersistentDataType.STRING));
    }

    public static void setMode(ItemStack stack, DiggerToolMode mode, @Nullable Player executor) {
        setMode(MinecraftAdapter.ItemStack.itemStack(stack), mode, executor);
    }

    public static void setMode(org.bukkit.inventory.ItemStack stack, DiggerToolMode mode, @Nullable Player executor) {
        if (stack == null || stack.getItemMeta() == null) return;
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(TOOL_MODE_KEY, PersistentDataType.STRING, mode.name()));
        if (executor != null) executor.sendActionBar(Component.empty().append(stack.displayName()).appendSpace().append(Component.text("モードを切り替えました: " + mode.getDisplayName(), NamedTextColor.GREEN)));
    }
}
