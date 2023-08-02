package com.ryuuta0217.furude.feature.tool;

import com.ryuuta0217.furude.FurudeCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.unknown.core.util.BlockUtil;
import net.unknown.core.util.MinecraftAdapter;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Stream;

public class ChainDestruction implements Listener {
    public static final Set<Block> CHAIN_DESTRUCT_TARGETS = new HashSet<>() {{
        add(Blocks.NETHER_QUARTZ_ORE);
        add(Blocks.GLOWSTONE);
    }};

    public static final Set<TagKey<Block>> CHAIN_DESTRUCT_TARGET_TAGS = new HashSet<>() {{
        add(BlockTags.COAL_ORES);
        add(BlockTags.COPPER_ORES);
        add(BlockTags.IRON_ORES);
        add(BlockTags.REDSTONE_ORES);
        add(BlockTags.LAPIS_ORES);
        add(BlockTags.GOLD_ORES);
        add(BlockTags.DIAMOND_ORES);
        add(BlockTags.EMERALD_ORES);

        add(BlockTags.LOGS);
        add(BlockTags.OVERWORLD_NATURAL_LOGS);
    }};

    private static final NamespacedKey CHAIN_DESTRUCTION_ENABLED_KEY = new NamespacedKey(FurudeCore.getInstance(), "cd_enabled");
    private static final NamespacedKey CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY = new NamespacedKey(FurudeCore.getInstance(), "cd_targets_additional");

    private final Map<UUID, Set<BlockPos>> ignoreEventPositions = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHeldChanged(PlayerItemHeldEvent event) {
        boolean isWheel = event.getNewSlot() == event.getPreviousSlot() + 1 || event.getNewSlot() == event.getPreviousSlot() - 1 || (event.getNewSlot() == 0 && event.getPreviousSlot() == 8);
        if (!isWheel || !event.getPlayer().isSneaking()) return;

        org.bukkit.inventory.ItemStack selectedBukkitItem = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
        if (selectedBukkitItem == null) return;

        ItemStack selectedItem = MinecraftAdapter.ItemStack.itemStack(selectedBukkitItem);
        if (selectedItem == null || (!(selectedItem.getItem() instanceof PickaxeItem) && !(selectedItem.getItem() instanceof AxeItem))) return;

        if (isEnabled(selectedBukkitItem)) {
            setEnabled(selectedBukkitItem, false);
            event.getPlayer().sendActionBar(Component.empty()
                    .append(selectedBukkitItem.displayName())
                    .appendSpace()
                    .append(Component.text("一括破壊を無効化しました", NamedTextColor.RED)));
            event.setCancelled(true);
        } else {
            setEnabled(selectedBukkitItem, true);
            event.getPlayer().sendActionBar(Component.empty()
                    .append(selectedBukkitItem.displayName())
                    .appendSpace()
                    .append(Component.text("一括破壊を有効化しました", NamedTextColor.GREEN)));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClickBlock(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;
        if (event.getClickedBlock() == null) return;

        ItemStack selectedItem = MinecraftAdapter.ItemStack.itemStack(event.getItem());
        if (selectedItem == null || (!(selectedItem.getItem() instanceof PickaxeItem) && !(selectedItem.getItem() instanceof AxeItem))) return;

        BlockState state = MinecraftAdapter.blockState(event.getClickedBlock());
        Block block = state.getBlock();

        if (!selectedItem.getItem().isCorrectToolForDrops(state)) return;

        if (isValidTargetCustom(MinecraftAdapter.ItemStack.itemStack(selectedItem), block) && !event.getPlayer().isSneaking()) {
            if (removeCustomTargetBlock(MinecraftAdapter.ItemStack.itemStack(selectedItem), block)) {
                event.getPlayer().sendMessage(Component.text("一括破壊のカスタム対象から削除しました: " + BuiltInRegistries.BLOCK.getKey(block)));
            }
        } else if (!isValidTargetDefault(block) && event.getPlayer().isSneaking()) {
            if (addCustomTargetBlock(MinecraftAdapter.ItemStack.itemStack(selectedItem), block)) {
                event.getPlayer().sendMessage(Component.text("一括破壊のカスタム対象に追加しました: " + BuiltInRegistries.BLOCK.getKey(block)));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        BlockPos breakBlockPos = MinecraftAdapter.blockPos(event.getBlock().getLocation());

        if (ignoreEventPositions.containsKey(event.getPlayer().getUniqueId())) {
            if (ignoreEventPositions.get(event.getPlayer().getUniqueId()).contains(breakBlockPos)) {
                ignoreEventPositions.get(event.getPlayer().getUniqueId()).remove(breakBlockPos);
                return;
            }
        }

        ServerPlayer player = MinecraftAdapter.player(event.getPlayer());
        if (player == null) return;

        ItemStack selectedItem = player.getMainHandItem();

        boolean enabled = isEnabled(MinecraftAdapter.ItemStack.itemStack(selectedItem));
        int maxBlocks = (selectedItem.getItem() instanceof PickaxeItem ? 64 : 256);

        ServerLevel level = MinecraftAdapter.level(event.getBlock().getWorld());
        BlockState state = MinecraftAdapter.blockState(event.getBlock());
        Block block = state.getBlock();
        if (!isValidTarget(MinecraftAdapter.ItemStack.itemStack(selectedItem), block)) return; // Not Target
        if (state.is(BlockTags.LOGS) && !(selectedItem.getItem() instanceof AxeItem)) return; // Only Axe can break logs
        if (state.requiresCorrectToolForDrops() && !selectedItem.getItem().isCorrectToolForDrops(state)) return; // Not Correct Tool
        if (selectedItem.getItem().getMaxDamage() - selectedItem.getDamageValue() <= 1) return; // No Durability
        if (!enabled) return; // Finally, Not Enabled

        Set<BlockPos> toBreak = new HashSet<>(); // Initialize Set for Break Positions
        BlockUtil.searchBlock(breakBlockPos, maxBlocks, level, block, toBreak); // Search Blocks
        ignoreEventPositions.put(player.getUUID(), new HashSet<>()); // Initialize Ignore Positions

        toBreak.forEach(pos -> {
            if (player.getMainHandItem().equals(selectedItem)) {
                if ((selectedItem.getItem().getMaxDamage() - selectedItem.getDamageValue()) > 1) {
                    ignoreEventPositions.get(player.getUUID()).add(pos);
                    try {
                        player.gameMode.destroyBlock(pos);
                    } finally {
                        ignoreEventPositions.get(player.getUUID()).remove(pos);
                    }
                }
            }
        });

        ignoreEventPositions.getOrDefault(player.getUUID(), Collections.emptySet()).clear();
    }

    private static boolean isValidTarget(org.bukkit.inventory.ItemStack tool, Block targetBlock) {
        return isValidTargetDefault(targetBlock) || isValidTargetCustom(tool, targetBlock);
    }

    private static boolean isValidTargetDefault(Block targetBlock) {
        boolean blockMatch = CHAIN_DESTRUCT_TARGETS.contains(targetBlock);
        boolean tagMatch = CHAIN_DESTRUCT_TARGET_TAGS.stream().anyMatch(tag -> targetBlock.builtInRegistryHolder().is(tag));
        return blockMatch || tagMatch;
    }

    private static boolean isValidTargetCustom(org.bukkit.inventory.ItemStack tool, Block targetBlock) {
        if (tool != null && tool.getItemMeta() != null && tool.getItemMeta().getPersistentDataContainer().has(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING)) {
            String raw = tool.getItemMeta().getPersistentDataContainer().get(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING);
            String[] ids = raw != null ? raw.split(", ?") : new String[0];
            for (String idStr : ids) {
                try {
                    ResourceLocation id = ResourceLocation.tryParse(idStr);
                    Block block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
                    if (block != null && block.equals(targetBlock)) {
                        return true;
                    }
                } catch (Throwable ignored) {}
            }
        }
        return false;
    }

    private static boolean isEnabled(org.bukkit.inventory.ItemStack stack) {
        if (stack.getItemMeta().getPersistentDataContainer().has(CHAIN_DESTRUCTION_ENABLED_KEY)) {
            return Boolean.TRUE.equals(stack.getItemMeta().getPersistentDataContainer().get(CHAIN_DESTRUCTION_ENABLED_KEY, PersistentDataType.BOOLEAN));
        }
        return false;
    }

    private static boolean setEnabled(org.bukkit.inventory.ItemStack stack, boolean enabled) {
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(CHAIN_DESTRUCTION_ENABLED_KEY, PersistentDataType.BOOLEAN, enabled));
        return enabled;
    }

    private static List<String> getCustomTargetBlock(org.bukkit.inventory.ItemStack stack) {
        if (stack.getItemMeta().getPersistentDataContainer().has(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING)) {
            String raw = stack.getItemMeta().getPersistentDataContainer().get(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING);
            return raw != null ? Arrays.asList(raw.split(", ?")) : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    private static boolean addCustomTargetBlock(org.bukkit.inventory.ItemStack stack, Block block) {
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        List<String> blocks = new ArrayList<>(getCustomTargetBlock(stack));
        if (blocks.contains(blockId)) return false; // Already Added
        blocks.add(blockId);
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING, String.join(",", blocks)));
        return true;
    }

    private static boolean removeCustomTargetBlock(org.bukkit.inventory.ItemStack stack, Block block) {
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        List<String> blocks = new ArrayList<>(getCustomTargetBlock(stack));
        if (!blocks.contains(blockId)) return false; // Not Added
        blocks.remove(blockId);
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING, String.join(",", blocks)));
        return true;
    }
}
