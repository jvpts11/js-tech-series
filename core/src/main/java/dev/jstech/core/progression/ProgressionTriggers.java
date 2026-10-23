/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.progression;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * The axes moving by playing: a mod says what moves a player, and the player's team, to a step the first time they do
 * it. Making an item, at a crafting table, in a furnace, or in a machine they own; or placing a block: a mod's tier
 * moves as a player first makes that tier's circuit, or first places its machine, each declared by the mod that adds
 * them.
 *
 * <pre>{@code
 * ProgressionTriggers.whenMade(MyItems.T2_CIRCUIT.get(), MyAxes.TIER, MyTier.T2);
 * ProgressionTriggers.whenPlaced(MyBlocks.T3_PRESS.get(), MyAxes.TIER, MyTier.T3);
 * }</pre>
 */
public final class ProgressionTriggers {

    private static final List<Trigger<ItemStack, ?>> MADE = new ArrayList<>();
    private static final List<Trigger<BlockState, ?>> PLACED = new ArrayList<>();

    private ProgressionTriggers() {
    }

    /** Hands the game's bus what moves the axes, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(PlayerEvent.ItemCraftedEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                made(player.server, player.getUUID(), event.getCrafting());
            }
        });
        NeoForge.EVENT_BUS.addListener(PlayerEvent.ItemSmeltedEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                made(player.server, player.getUUID(), event.getSmelting());
            }
        });
        NeoForge.EVENT_BUS.addListener(BlockEvent.EntityPlaceEvent.class, event -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                placed(player.server, player.getUUID(), event.getPlacedBlock());
            }
        });
    }

    /** Making {@code item} moves a player along {@code axis} to {@code step}, the first time. */
    public static synchronized <S extends IAxisStep> void whenMade(final ItemLike item, final ProgressionAxis<S> axis,
                                                                   final S step) {
        final Item made = Objects.requireNonNull(item, "item").asItem();
        MADE.add(new Trigger<>(stack -> stack.is(made), axis, step));
    }

    /** Making any item of {@code tag} moves a player along {@code axis} to {@code step}, the first time. */
    public static synchronized <S extends IAxisStep> void whenMade(final TagKey<Item> tag,
                                                                   final ProgressionAxis<S> axis, final S step) {
        Objects.requireNonNull(tag, "tag");
        MADE.add(new Trigger<>(stack -> stack.is(tag), axis, step));
    }

    /** Placing {@code block} moves a player along {@code axis} to {@code step}, the first time. */
    public static synchronized <S extends IAxisStep> void whenPlaced(final Block block,
                                                                     final ProgressionAxis<S> axis, final S step) {
        Objects.requireNonNull(block, "block");
        PLACED.add(new Trigger<>(state -> state.is(block), axis, step));
    }

    /**
     * {@code player} made {@code stack}: at a table, in a furnace, or in a machine they own, which the Core's machines
     * report as they finish. A mod's own way of making things reports here too.
     */
    public static void made(final MinecraftServer server, final UUID player, final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (final Trigger<ItemStack, ?> trigger : snapshot(MADE)) {
            trigger.fire(server, player, stack);
        }
    }

    /** {@code player} placed {@code state}. */
    public static void placed(final MinecraftServer server, final UUID player, final BlockState state) {
        for (final Trigger<BlockState, ?> trigger : snapshot(PLACED)) {
            trigger.fire(server, player, state);
        }
    }

    private static synchronized <T> List<Trigger<T, ?>> snapshot(final List<Trigger<T, ?>> triggers) {
        return List.copyOf(triggers);
    }

    /* What moves a player along an axis to a step: something done that {@code what} names. */
    private record Trigger<T, S extends IAxisStep>(Predicate<T> what, ProgressionAxis<S> axis, S step) {

        Trigger {
            Objects.requireNonNull(axis, "axis");
            Objects.requireNonNull(step, "step");
        }

        void fire(final MinecraftServer server, final UUID player, final T done) {
            if (this.what.test(done)) {
                PlayerProgress.reach(server, player, this.axis, this.step);
            }
        }
    }
}
