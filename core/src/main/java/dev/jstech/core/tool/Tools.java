/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.tool;

import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.module.Modules;
import dev.jstech.core.registry.CoreItems;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

/**
 * The kit of tools: what an item declared as a tool does ({@link ToolSpec}), and the rules every one follows. A tool
 * works while it holds what it works on and has not overheated; working, each block it breaks and each use spends its
 * power and its modules' energy and adds its heat; not working, it breaks blocks no faster than a bare hand and gets
 * no drops a hand would not. One that mines an area or a vein breaks the rest of it with the block it breaks
 * ({@link AreaMining}); one that builds lays blocks out where it is used ({@link BuildingTools}).
 *
 * <p>A mod's own item that does something on use (fires, charges, scans) asks {@link #ready} first and tells
 * {@link #used} after; {@link ChargingItem} does both for an item held to charge.
 */
public final class Tools {

    /** How hot a tool that heats is, and since when. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolHeat.Heated>> HEAT =
            CoreItems.CONTENT.component("tool_heat", ToolHeat.Heated.CODEC, ToolHeat.Heated.STREAM_CODEC);

    /* What each declared tool does, worked out the first time it is asked for, once every item is registered. */
    private static volatile @Nullable Map<Item, ToolSpec> byItem;

    private Tools() {
    }

    /** Hands the game's bus what tools answer, once, from the Core. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.BreakSpeed.class, Tools::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(PlayerEvent.HarvestCheck.class, Tools::onHarvestCheck);
        /* Last, so a protection that stops the block broken stops the area too. */
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BlockEvent.BreakEvent.class, AreaMining::onBreak);
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, BuildingTools::onUse);
        BuildingTools.declare();
    }

    /** What {@code item} does as a tool, or none for an item that is no tool of the Core's. */
    public static Optional<ToolSpec> spec(final Item item) {
        Map<Item, ToolSpec> known = byItem;
        if (known == null) {
            known = new IdentityHashMap<>();
            for (final ModContent content : ModContent.all()) {
                for (final ItemEntry<?> entry : content.declaredItems()) {
                    final Map<Item, ToolSpec> into = known;
                    entry.tool().ifPresent(spec -> into.put(entry.get(), spec));
                }
            }
            byItem = known;
        }
        return Optional.ofNullable(known.get(item));
    }

    /** What the item of {@code stack} does as a tool, or none. */
    public static Optional<ToolSpec> spec(final ItemStack stack) {
        return stack.isEmpty() ? Optional.empty() : spec(stack.getItem());
    }

    /**
     * What {@code stack} mines besides the block it breaks, on the side {@code level} is on: what its mode gives, else
     * what a module in it gives, else none.
     */
    public static Optional<MiningShape> mines(final Level level, final ItemStack stack) {
        final Optional<MiningShape> given = spec(stack).flatMap(spec -> spec.mines(ItemStates.mode(stack)));
        return given.isPresent() ? given : Modules.tool(level, stack).mining();
    }

    /** What {@code stack} builds in the mode it is in, or none. */
    public static Optional<BuildMode> builds(final ItemStack stack) {
        return spec(stack).flatMap(spec -> spec.builds(ItemStates.mode(stack)));
    }

    /** Whether {@code stack} works: it holds what it works on, and has not overheated. Any other item works. */
    public static boolean works(final Level level, final ItemStack stack) {
        final Optional<ToolSpec> spec = spec(stack);
        if (spec.isEmpty()) {
            return true;
        }
        return spec.get().power().map(power -> power.has(level, stack)).orElse(true)
                && !spec.get().heat().map(heat -> heat.overheated(level, stack)).orElse(false);
    }

    /** Whether {@code player} may use {@code stack} now: it works, and it is not waiting since its last use. */
    public static boolean ready(final Player player, final ItemStack stack) {
        return works(player.level(), stack) && !player.getCooldowns().isOnCooldown(stack.getItem());
    }

    /**
     * Spends what one block broken or one use takes of {@code stack} in {@code user}'s hand: its power, its modules'
     * energy, and the heat it adds. A player in creative spends nothing.
     */
    public static void spend(final LivingEntity user, final ItemStack stack) {
        if (user instanceof Player player && player.getAbilities().instabuild) {
            return;
        }
        final Level level = user.level();
        final Optional<ToolSpec> spec = spec(stack);
        spec.flatMap(ToolSpec::power).ifPresent(power -> power.spend(level, stack));
        spec.flatMap(ToolSpec::heat).ifPresent(heat -> heat.use(level, stack));
        Modules.spendForUse(level, stack);
    }

    /** Spends what a use of {@code stack} takes, as {@link #spend} does, and starts its wait before the next. */
    public static void used(final LivingEntity user, final ItemStack stack) {
        spend(user, stack);
        if (user instanceof Player player) {
            startWait(player, stack);
        }
    }

    /** Starts the wait of {@code stack} before its next use, the one the game shows over its icon; none for most. */
    public static void startWait(final Player player, final ItemStack stack) {
        final int cooldown = spec(stack).map(ToolSpec::cooldown).orElse(0);
        if (cooldown > 0) {
            player.getCooldowns().addCooldown(stack.getItem(), cooldown);
        }
    }

    /**
     * How charged {@code stack} is after {@code ticks} held in use, from nothing to whole; whole for a tool that never
     * charges.
     */
    public static float charged(final ItemStack stack, final int ticks) {
        final int full = spec(stack).map(ToolSpec::charge).orElse(0);
        return full <= 0 ? 1.0F : Math.min(1.0F, Math.max(0, ticks) / (float) full);
    }

    /* A tool that does not work breaks no faster than a bare hand. */
    private static void onBreakSpeed(final PlayerEvent.BreakSpeed event) {
        final ItemStack stack = event.getEntity().getMainHandItem();
        if (spec(stack).isPresent() && !works(event.getEntity().level(), stack)) {
            event.setNewSpeed(Math.min(event.getNewSpeed(), 1.0F));
        }
    }

    /* A tool that does not work gets no drops a bare hand would not. */
    private static void onHarvestCheck(final PlayerEvent.HarvestCheck event) {
        final ItemStack stack = event.getEntity().getMainHandItem();
        if (spec(stack).isPresent() && event.getTargetBlock().requiresCorrectToolForDrops()
                && !works(event.getEntity().level(), stack)) {
            event.setCanHarvest(false);
        }
    }
}
