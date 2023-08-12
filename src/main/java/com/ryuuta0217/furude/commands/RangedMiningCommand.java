package com.ryuuta0217.furude.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.ryuuta0217.furude.feature.tool.DiggerToolMode;
import com.ryuuta0217.furude.feature.tool.ModeSwitcher;
import com.ryuuta0217.furude.feature.tool.RangedMining;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.util.MinecraftAdapter;

public class RangedMiningCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("rangedmining");
        builder.requires(source -> source.hasPermission(0));

        builder.then(Commands.literal("enable")
                        .executes(ctx -> setRangedMiningStatus(ctx, true)))
                .then(Commands.literal("disable")
                        .executes(ctx -> setRangedMiningStatus(ctx, false)))
                .then(Commands.literal("modify")
                        .then(Commands.literal("range")
                                .executes(ctx -> showRange(ctx))
                                .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                        .executes(ctx -> modifyRange(ctx, IntegerArgumentType.getInteger(ctx, "range")))))
                        .then(Commands.literal("dig-under")
                                .then(Commands.literal("enable")
                                        .executes(ctx -> modifyDigUnder(ctx, true)))
                                .then(Commands.literal("disable")
                                        .executes(ctx -> modifyDigUnder(ctx, false)))));

        LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = LiteralArgumentBuilder.literal("rm");
        aliasBuilder.requires(source -> source.hasPermission(0));
        aliasBuilder.redirect(dispatcher.register(builder));

        dispatcher.register(aliasBuilder);
    }

    private static int setRangedMiningStatus(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        if (!isValidTool(mainHandItem)) {
            ctx.getSource().sendFailure(Component.literal("現在手に持っているアイテムでは、範囲破壊を行えません"), true);
            return 2;
        }

        if ((enabled && ModeSwitcher.getMode(mainHandItem) == DiggerToolMode.RANGED_MINING) || (!enabled && ModeSwitcher.getMode(mainHandItem) != DiggerToolMode.RANGED_MINING)) {
            ctx.getSource().sendFailure(Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(net.kyori.adventure.text.Component.empty()
                    .append(mainHandItemBukkit.displayName())
                    .appendSpace()
                    .append(net.kyori.adventure.text.Component.text("範囲破壊は既に"))
                    .append(net.kyori.adventure.text.Component.text((enabled ? "有効" : "無効") + "化", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(net.kyori.adventure.text.Component.text("されています")))), true);
            return 1;
        }

        ModeSwitcher.setMode(mainHandItem, enabled ? DiggerToolMode.RANGED_MINING : DiggerToolMode.OFF, null);
        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(net.kyori.adventure.text.Component.empty()
                .append(mainHandItemBukkit.displayName())
                .appendSpace()
                .append(net.kyori.adventure.text.Component.text("範囲破壊を"))
                .append(net.kyori.adventure.text.Component.text((enabled ? "有効" : "無効") + "化", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(net.kyori.adventure.text.Component.text("しました")))), true);
        return 0;
    }

    private static int showRange(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        if (!isValidTool(mainHandItem)) {
            ctx.getSource().sendFailure(Component.literal("現在手に持っているアイテムでは、範囲破壊を行えません"), true);
            return 2;
        }

        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(net.kyori.adventure.text.Component.empty()
                .append(mainHandItemBukkit.displayName())
                .appendSpace()
                .append(net.kyori.adventure.text.Component.text("範囲破壊の範囲は"))
                .append(net.kyori.adventure.text.Component.text("上下左右方向に" + RangedMining.getRange(mainHandItem) + "ブロック", NamedTextColor.GREEN))
                .append(net.kyori.adventure.text.Component.text("です")))), true);

        return 0;
    }

    private static int modifyRange(CommandContext<CommandSourceStack> ctx, int range) throws CommandSyntaxException {
        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        if (!isValidTool(mainHandItem)) {
            ctx.getSource().sendFailure(Component.literal("現在手に持っているアイテムでは、範囲破壊を行えません"), true);
            return 2;
        }

        RangedMining.setRange(mainHandItem, range);
        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(net.kyori.adventure.text.Component.empty()
                .append(mainHandItemBukkit.displayName())
                .appendSpace()
                .append(net.kyori.adventure.text.Component.text("範囲破壊の範囲を"))
                .append(net.kyori.adventure.text.Component.text("上下左右方向に" + range + "ブロック", NamedTextColor.GREEN))
                .append(net.kyori.adventure.text.Component.text("に変更しました")))), true);
        return 0;
    }

    private static int showDigUnder(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        if (!isValidTool(mainHandItem)) {
            ctx.getSource().sendFailure(Component.literal("現在手に持っているアイテムでは、範囲破壊を行えません"), true);
            return 2;
        }

        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(net.kyori.adventure.text.Component.empty()
                .append(mainHandItemBukkit.displayName())
                .appendSpace()
                .append(net.kyori.adventure.text.Component.text("範囲破壊で自分の高さより下にあるブロックを破壊する機能は"))
                .append(net.kyori.adventure.text.Component.text((RangedMining.isDigUnder(mainHandItem) ? "有効" : "無効") + "化", RangedMining.isDigUnder(mainHandItem) ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(net.kyori.adventure.text.Component.text("されています")))), true);
        return 0;
    }

    private static int modifyDigUnder(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        if (!isValidTool(mainHandItem)) {
            ctx.getSource().sendFailure(Component.literal("現在手に持っているアイテムでは、範囲破壊を行えません"), true);
            return 2;
        }

        if ((enabled && RangedMining.isDigUnder(mainHandItem)) || (!enabled && !RangedMining.isDigUnder(mainHandItem))) {
            ctx.getSource().sendFailure(Component.empty()
                    .append(mainHandItem.getDisplayName())
                    .append(" ")
                    .append("範囲破壊で自分の高さより下にあるブロックを破壊する機能は既に")
                    .append(Component.literal((enabled ? "有効" : "無効") + "化").withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED))
                    .append("されています"), true);
            return 2;
        }

        RangedMining.setDigUnder(mainHandItem, enabled);
        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(net.kyori.adventure.text.Component.empty()
                .append(mainHandItemBukkit.displayName())
                .appendSpace()
                .append(net.kyori.adventure.text.Component.text("範囲破壊で自分の高さより下にあるブロックを破壊する機能を"))
                .append(net.kyori.adventure.text.Component.text((enabled ? "有効" : "無効") + "化", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .append(net.kyori.adventure.text.Component.text("しました")))), true);
        return 0;
    }

    private static boolean isValidTool(ItemStack stack) {
        return stack.getItem() instanceof DiggerItem;
    }
}
