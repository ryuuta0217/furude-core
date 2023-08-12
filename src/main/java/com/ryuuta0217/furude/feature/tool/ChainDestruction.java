package com.ryuuta0217.furude.feature.tool;

import com.ryuuta0217.furude.FurudeCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.TitleCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.stream.Collectors;

public class ChainDestruction implements Listener {
    private static final Set<TagKey<Block>> CHAIN_DESTRUCT_TARGET_TAGS = new HashSet<>() {{
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

    public static final Set<Block> DEFAULT_CHAIN_DESTRUCT_TARGETS = new HashSet<>() {{
        add(Blocks.NETHER_QUARTZ_ORE);
        add(Blocks.GLOWSTONE);

        BuiltInRegistries.BLOCK.forEach(block -> {
            if (CHAIN_DESTRUCT_TARGET_TAGS.stream().anyMatch(tag -> block.defaultBlockState().is(tag))) {
                add(block);
            }
        });
    }};

    private static final Set<String> DEFAULT_CHAIN_DESTRUCT_TARGETS_STRING = DEFAULT_CHAIN_DESTRUCT_TARGETS.stream()
            .map(block -> BuiltInRegistries.BLOCK.getKey(block).toString())
            .collect(Collectors.toSet());

    private static final NamespacedKey CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY = new NamespacedKey(FurudeCore.getInstance(), "cd_targets_additional");
    private static final NamespacedKey CHAIN_DESTRUCTION_MAX_BLOCKS_KEY = new NamespacedKey(FurudeCore.getInstance(), "cd_max_blocks");

    private final Map<UUID, Set<BlockPos>> ignoreEventPositions = new HashMap<>();

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClickBlock(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return; // Only works in main hand
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null) return;
        if (event.getClickedBlock() == null) return;

        ItemStack selectedItem = MinecraftAdapter.ItemStack.itemStack(event.getItem());

        BlockState state = MinecraftAdapter.blockState(event.getClickedBlock());
        Block block = state.getBlock();

        if (!selectedItem.getItem().isCorrectToolForDrops(state)) return;
        if (ModeSwitcher.getMode(selectedItem) != DiggerToolMode.CHAIN_DESTRUCTION) return; // Only if enabled

        if (isValidTarget(MinecraftAdapter.ItemStack.itemStack(selectedItem), block) && !event.getPlayer().isSneaking()) {
            if (removeTargetBlock(MinecraftAdapter.ItemStack.itemStack(selectedItem), block)) {
                event.getPlayer().sendMessage(Component.empty()
                        .append(selectedItem.asBukkitMirror().displayName())
                        .append(Component.text(" 一括破壊対象から削除しました: " + BuiltInRegistries.BLOCK.getKey(block))));
                event.setCancelled(true);
            }
        } else if (event.getPlayer().isSneaking()) {
            if (addTargetBlock(MinecraftAdapter.ItemStack.itemStack(selectedItem), block)) {
                event.getPlayer().sendMessage(Component.empty()
                        .append(selectedItem.asBukkitMirror().displayName())
                        .append(Component.text(" 一括破壊対象に追加しました: " + BuiltInRegistries.BLOCK.getKey(block))));
                event.setCancelled(true);
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

        boolean enabled = ModeSwitcher.getMode(selectedItem) == DiggerToolMode.CHAIN_DESTRUCTION;
        int maxBlocks = getMaxBlocks(MinecraftAdapter.ItemStack.itemStack(selectedItem));

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
                } else {
                    player.getBukkitEntity().sendActionBar(Component.text("耐久値がなくなりました", NamedTextColor.RED));
                }
            }
        });

        ignoreEventPositions.getOrDefault(player.getUUID(), Collections.emptySet()).clear();
    }

    private static boolean isValidTarget(org.bukkit.inventory.ItemStack tool, Block targetBlock) {
        Set<String> targetBlocks = getTargetBlocks(tool);
        return targetBlocks.contains(BuiltInRegistries.BLOCK.getKey(targetBlock).toString());
    }

    public static Set<String> getTargetBlocks(org.bukkit.inventory.ItemStack stack) {
        if (stack.getItemMeta().getPersistentDataContainer().has(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING)) {
            String raw = stack.getItemMeta().getPersistentDataContainer().get(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING);
            return raw != null ? Arrays.stream(raw.split(", ?")).filter(str -> !str.isEmpty() && !str.isBlank()).collect(Collectors.toSet()) : Collections.emptySet();
        }
        return new HashSet<>(DEFAULT_CHAIN_DESTRUCT_TARGETS_STRING).stream()
                .map(id -> new ResourceLocation(id))
                .map(loc -> BuiltInRegistries.BLOCK.get(loc))
                .map(block -> block.defaultBlockState())
                .filter(state -> MinecraftAdapter.item(stack.getType()).isCorrectToolForDrops(state))
                .map(state -> BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString())
                .collect(Collectors.toUnmodifiableSet()); // Returns only blocks that can be broken by the tool by default.
    }

    public static boolean addTargetBlock(org.bukkit.inventory.ItemStack stack, Block block) {
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        Set<String> targetBlocks = new HashSet<>(getTargetBlocks(stack));
        if (targetBlocks.contains(blockId)) return false; // Already Added
        targetBlocks.add(blockId);
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING, String.join(",", targetBlocks)));
        return true;
    }

    public static boolean removeTargetBlock(org.bukkit.inventory.ItemStack stack, Block block) {
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();
        Set<String> targetBlocks = new HashSet<>(getTargetBlocks(stack));
        if (!targetBlocks.contains(blockId)) return false; // Not Added
        targetBlocks.remove(blockId);
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(CHAIN_DESTRUCTION_ADDITIONAL_TARGETS_KEY, PersistentDataType.STRING, String.join(",", targetBlocks)));
        return true;
    }

    public static int getMaxBlocks(org.bukkit.inventory.ItemStack stack) {
        if (stack.getItemMeta().getPersistentDataContainer().has(CHAIN_DESTRUCTION_MAX_BLOCKS_KEY, PersistentDataType.INTEGER)) {
            return stack.getItemMeta().getPersistentDataContainer().get(CHAIN_DESTRUCTION_MAX_BLOCKS_KEY, PersistentDataType.INTEGER);
        }
        return MinecraftAdapter.ItemStack.itemStack(stack).getItem() instanceof AxeItem ? 256 : 64;
    }

    public static void setMaxBlocks(org.bukkit.inventory.ItemStack stack, int maxBlocks) {
        stack.editMeta(meta -> meta.getPersistentDataContainer().set(CHAIN_DESTRUCTION_MAX_BLOCKS_KEY, PersistentDataType.INTEGER, maxBlocks));
    }
}
