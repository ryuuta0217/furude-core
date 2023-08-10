package com.ryuuta0217.furude.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.ryuuta0217.furude.FurudeCore;
import com.ryuuta0217.furude.feature.tool.ChainDestruction;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unknown.core.util.MinecraftAdapter;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public class ChainDestructionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        CommandBuildContext buildContext = null;
        try {
            Field commandBuildContextField = ReloadableServerResources.class.getDeclaredField("c");
            if (commandBuildContextField.trySetAccessible()) {
                buildContext = (CommandBuildContext) commandBuildContextField.get(MinecraftServer.getServer().resources.managers());
            } else {
                FurudeCore.getInstance().getLogger().warning("一括破壊用コマンド /chaindestruction の登録でエラーが起きました。Fieldが見つかりません。");
            }
        } catch(NoSuchFieldException | IllegalAccessException e) {
            FurudeCore.getInstance().getLogger().warning("一括破壊用コマンド /chaindestruction の登録でエラーが起きました: " + e.getMessage());
        }

        if (buildContext == null) {
            FurudeCore.getInstance().getLogger().warning("一括破壊用コマンド /chaindestruction の登録でエラーが起きました。CommandBuildContextが取得できていないため、コマンドは登録されません");
            return;
        }

        // /<chaindestroction|cd> <enable|disable|modify>
        // /<chaindestroction|cd> modify max-blocks [int: maxBlocks]
        // /<chaindestroction|cd> modify targets <add|remove|list>
        // /<chaindestroction|cd> modify targets -> redirect into /<chaindestroction|cd> modify targets list
        // /<chaindestroction|cd> modify targets add <target: Item, suggestions: All-Items>
        // /<chaindestroction|cd> modify targets remove <target: Item, suggestions: Registered-Items>
        // /<chaindestroction|cd> modify targets list
        LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.literal("chaindestruction");
        builder.requires(source -> source.hasPermission(0));

        builder.then(Commands.literal("enable")
                .executes(ctx -> setChainDestructionStatus(ctx, true)))
                .then(Commands.literal("disable")
                        .executes(ctx -> setChainDestructionStatus(ctx, false)))
                .then(Commands.literal("modify")
                        .then(Commands.literal("max-blocks")
                                .then(Commands.argument("maxBlocks", IntegerArgumentType.integer(1))
                                        .executes(ctx -> modifyMaxBlocks(ctx, IntegerArgumentType.getInteger(ctx, "maxBlocks"))))))
                .then(Commands.literal("targets")
                        .executes(ChainDestructionCommand::listTargets)
                        .then(Commands.literal("add")
                                .then(Commands.argument("target", ItemArgument.item(buildContext))
                                        .suggests((ctx, suggestionsBuilder) -> {
                                            String input = suggestionsBuilder.getInput().substring(suggestionsBuilder.getStart());
                                            Set<String> chainDestructTargets = ChainDestruction.getTargetBlocks(MinecraftAdapter.ItemStack.itemStack(ctx.getSource().getPlayerOrException().getMainHandItem()));
                                            Set<ResourceLocation> registeredItems = BuiltInRegistries.ITEM.keySet();
                                            registeredItems.stream()
                                                    .filter(id -> !chainDestructTargets.contains(id.toString()))
                                                    .forEach(id -> {
                                                        if (chainDestructTargets.contains(id.toString())) return;
                                                        if (!input.isEmpty() && !input.isBlank() && !id.toString().contains(input)) return;
                                                        Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(id);
                                                        itemOpt.filter(item -> item instanceof BlockItem).ifPresent(item -> suggestionsBuilder.suggest(id.toString(), new ItemStack(item).getDisplayName()));
                                                    });
                                            return suggestionsBuilder.buildFuture();
                                        })
                                        .executes(ctx -> addTarget(ctx, ItemArgument.getItem(ctx, "target").getItem()))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("target", ItemArgument.item(buildContext))
                                        .suggests((ctx, suggestionsBuilder) -> {
                                            String input = suggestionsBuilder.getInput().substring(suggestionsBuilder.getStart());
                                            ChainDestruction.getTargetBlocks(MinecraftAdapter.ItemStack.itemStack(ctx.getSource().getPlayerOrException().getMainHandItem())).forEach(id -> {
                                                if (!input.isEmpty() && !input.isBlank() && !id.contains(input)) return;
                                                Optional<Item> itemOpt = BuiltInRegistries.ITEM.getOptional(new ResourceLocation(id));
                                                itemOpt.ifPresent(item -> suggestionsBuilder.suggest(id, new ItemStack(item).getDisplayName()));
                                            });
                                            return suggestionsBuilder.buildFuture();
                                        })
                                        .executes(ctx -> removeTarget(ctx, ItemArgument.getItem(ctx, "target").getItem()))))
                        .then(Commands.literal("list")
                                .executes(ChainDestructionCommand::listTargets)));

        LiteralCommandNode<CommandSourceStack> rootNode = dispatcher.register(builder);

        LiteralArgumentBuilder<CommandSourceStack> aliasBuilder = LiteralArgumentBuilder.literal("cd");
        aliasBuilder.requires(source -> source.hasPermission(0));
        aliasBuilder.redirect(rootNode);
        dispatcher.register(aliasBuilder);
    }

    private static int setChainDestructionStatus(CommandContext<CommandSourceStack> ctx, boolean enabled) throws CommandSyntaxException {
        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        if ((enabled && ChainDestruction.isEnabled(mainHandItemBukkit)) || (!enabled && !ChainDestruction.isEnabled(mainHandItemBukkit))) {
            ctx.getSource().sendFailure(Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.empty()
                    .append(net.kyori.adventure.text.Component.text("既に"))
                    .append(net.kyori.adventure.text.Component.text((enabled ? "有効" : "無効") + "化", enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                    .append(net.kyori.adventure.text.Component.text("されています"))))), true);
            return 1;
        }

        ChainDestruction.setEnabled(mainHandItemBukkit, enabled);
        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.empty()
                .append(net.kyori.adventure.text.Component.text("一括破壊を"))
                .appendSpace()
                .append(net.kyori.adventure.text.Component.text((enabled ? "有効" : "無効"), enabled ? NamedTextColor.GREEN : NamedTextColor.RED))
                .appendSpace()
                .append(net.kyori.adventure.text.Component.text("化しました"))))), true);
        return 0;
    }

    private static int modifyMaxBlocks(CommandContext<CommandSourceStack> ctx, int maxBlocks) throws CommandSyntaxException {
        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        if (ChainDestruction.getMaxBlocks(mainHandItemBukkit) == maxBlocks) {
            ctx.getSource().sendFailure(Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.text("一括破壊最大ブロック数は既に " + maxBlocks + "ブロック に設定されています")))), true);
            return 1;
        }

        ChainDestruction.setMaxBlocks(mainHandItemBukkit, maxBlocks);
        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.text("一括破壊最大ブロック数を " + maxBlocks + "ブロック に設定しました")))), true);
        return 0;
    }

    private static int addTarget(CommandContext<CommandSourceStack> ctx, Item target) throws CommandSyntaxException {
        if (!(target instanceof BlockItem blockItem)) {
            ctx.getSource().sendFailure(Component.literal("ブロックアイテムを指定してください"), false);
            return 1;
        }

        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        Set<String> chainDestructTargets = ChainDestruction.getTargetBlocks(mainHandItemBukkit);
        String blockItemKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString();

        if (chainDestructTargets.contains(blockItemKey)) {
            ctx.getSource().sendFailure(Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.empty()
                    .append(net.kyori.adventure.text.Component.text("既に一括破壊対象に設定されています:"))
                    .appendSpace()
                    .append(buildDisplayName(MinecraftAdapter.ItemStack.itemStack(new ItemStack(target)), NamedTextColor.RED))))), true);
            return 1;
        }

        ItemStack item = new ItemStack(target);
        org.bukkit.inventory.ItemStack itemBukkit = MinecraftAdapter.ItemStack.itemStack(item);

        ChainDestruction.addTargetBlock(mainHandItemBukkit, blockItem.getBlock());
        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.empty()
                .append(net.kyori.adventure.text.Component.text("一括破壊対象を追加しました:"))
                .appendSpace()
                .append(buildDisplayName(itemBukkit, NamedTextColor.GREEN))))), true);
        return 0;
    }

    private static int removeTarget(CommandContext<CommandSourceStack> ctx, Item target) throws CommandSyntaxException {
        if (!(target instanceof BlockItem blockItem)) {
            ctx.getSource().sendFailure(Component.literal("ブロックアイテムを指定してください"), true);
            return 1;
        }

        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        Set<String> chainDestructTargets = ChainDestruction.getTargetBlocks(mainHandItemBukkit);
        String blockItemKey = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString();

        if (!chainDestructTargets.contains(blockItemKey)) {
            ctx.getSource().sendFailure(Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.empty()
                    .append(net.kyori.adventure.text.Component.text("一括破壊の対象ではありません:"))
                    .appendSpace()
                    .append(buildDisplayName(MinecraftAdapter.ItemStack.itemStack(new ItemStack(target)), NamedTextColor.RED))))), true);
            return 1;
        }

        ItemStack item = new ItemStack(target);
        org.bukkit.inventory.ItemStack itemBukkit = MinecraftAdapter.ItemStack.itemStack(item);

        ChainDestruction.removeTargetBlock(mainHandItemBukkit, blockItem.getBlock());
        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.empty()
                .append(net.kyori.adventure.text.Component.text("一括破壊の対象から削除しました:", NamedTextColor.YELLOW))
                .appendSpace()
                .append(buildDisplayName(itemBukkit, NamedTextColor.YELLOW))))), true);
        return 0;
    }

    private static int listTargets(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack mainHandItem = ctx.getSource().getPlayerOrException().getMainHandItem();
        org.bukkit.inventory.ItemStack mainHandItemBukkit = MinecraftAdapter.ItemStack.itemStack(mainHandItem);

        Set<String> chainDestructTargets = ChainDestruction.getTargetBlocks(mainHandItemBukkit);
        if (chainDestructTargets.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.text("一括破壊対象が登録されていません (おかしい)", NamedTextColor.RED)))), true);
            return 1;
        }

        net.kyori.adventure.text.Component message = buildMessage(mainHandItemBukkit, net.kyori.adventure.text.Component.text(" 一括破壊対象は次の通りです:", NamedTextColor.GOLD))
                .appendSpace();

        NamedTextColor[] useColors = new NamedTextColor[] { NamedTextColor.GREEN, NamedTextColor.YELLOW, NamedTextColor.LIGHT_PURPLE };
        NamedTextColor nextColor = useColors[0];
        for (String target : chainDestructTargets) {
            ItemStack item = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(target)));
            org.bukkit.inventory.ItemStack itemBukkit = MinecraftAdapter.ItemStack.itemStack(item);
            message = message.append(itemBukkit.displayName().style(Style.style(nextColor)).hoverEvent(itemBukkit.asHoverEvent()))
                    .appendSpace();
            nextColor = useColors[(Arrays.asList(useColors).indexOf(nextColor) + 1) % useColors.length];
        }

        net.kyori.adventure.text.Component finalMessage = message;
        ctx.getSource().sendSuccess(() -> Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(finalMessage)), false);
        return 0;
    }

    private static net.kyori.adventure.text.Component buildDisplayName(org.bukkit.inventory.ItemStack stack, @Nullable Style style) {
        if (style != null) return stack.displayName().style(style).hoverEvent(stack.asHoverEvent());
        return stack.displayName();
    }

    private static net.kyori.adventure.text.Component buildDisplayName(org.bukkit.inventory.ItemStack stack, @Nullable TextColor color) {
        if (color != null) return stack.displayName().color(color).hoverEvent(stack.asHoverEvent());
        return stack.displayName();
    }

    private static net.kyori.adventure.text.Component buildMessage(org.bukkit.inventory.ItemStack stack, net.kyori.adventure.text.Component message) {
        return net.kyori.adventure.text.Component.empty()
                .append(stack.displayName().color(NamedTextColor.GOLD).hoverEvent(stack.asHoverEvent()))
                .appendSpace()
                .append(message);
    }

    private static net.kyori.adventure.text.Component buildMessage(ItemStack stack, net.kyori.adventure.text.Component message) {
        return buildMessage(MinecraftAdapter.ItemStack.itemStack(stack), message);
    }

    private static Component buildMessage(ItemStack stack, Component message) {
        return convertAdventure2Minecraft(buildMessage(stack, convertMinecraft2Adventure(message)));
    }

    private static Component convertAdventure2Minecraft(net.kyori.adventure.text.Component component) {
        return Component.Serializer.fromJson(GsonComponentSerializer.gson().serializeToTree(component));
    }

    private static net.kyori.adventure.text.Component convertMinecraft2Adventure(Component component) {
        return GsonComponentSerializer.gson().deserializeFromTree(Component.Serializer.toJsonTree(component));
    }
}
