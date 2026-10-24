/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.multiblock.BuildBalance;
import dev.jstech.core.network.payload.ClientPayloads;
import dev.jstech.core.network.payload.IPayloadGate;
import dev.jstech.core.registry.CoreItems;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The Multiblock Builder, a handheld instrument of the probes' family: used on a multiblock's terminal, it is aimed
 * at it, and its display says what of the multiblock is missing, what the player has to build it with, and asks what
 * goes where a place takes several blocks; OK builds it, block by block, from the player's inventory or another
 * source a mod gives. Its keys hold the building, show the multiblock's ghosts, light the display and turn it off.
 */
public final class MultiblockBuilder {

    /** How the builder is set. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BuilderState>> STATE =
            CoreItems.CONTENT.component("builder_state", BuilderState.CODEC, BuilderState.STREAM_CODEC);
    /** The terminal the builder is aimed at, in its dimension. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> AIM =
            CoreItems.CONTENT.component("builder_aim", GlobalPos.CODEC, GlobalPos.STREAM_CODEC);
    /** The Multiblock Builder. */
    public static final ItemEntry<MultiblockBuilderItem> ITEM = CoreItems.CONTENT.item("multiblock_builder",
                    MultiblockBuilderItem::new).named("Multiblock Builder").look(IItemLook.FLAT)
            .component(STATE, BuilderState.DEFAULT).tab(JsTechTab.BLOCKS_AND_TOOLS).register();
    private static final String NETWORK_VERSION = "1";

    private static volatile Consumer<InteractionHand> opener = hand -> {
    };
    private static volatile Consumer<BuildStatus> told = status -> {
    };

    static {
        // It builds any mod's multiblock, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, ITEM::get);
    }

    private MultiblockBuilder() {
    }

    /** Declares the builder, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, MultiblockBuilder::onRegisterPayloads);
        BuildJobs.listen();
    }

    /** Holds up the builder in {@code hand} on this game's screen; on a dedicated server, nothing. */
    public static void open(final InteractionHand hand) {
        opener.accept(hand);
    }

    /** Says what holds the builder up: the player's game does, once, as it starts. */
    public static void useOpener(final Consumer<InteractionHand> what) {
        opener = Objects.requireNonNull(what, "what");
    }

    /** Says what hears how the building goes: the player's game does, once, as it starts. */
    public static void useListener(final Consumer<BuildStatus> what) {
        told = Objects.requireNonNull(what, "what");
    }

    /** How the builder in {@code stack} is set. */
    public static BuilderState state(final ItemStack stack) {
        return stack.getOrDefault(STATE.get(), BuilderState.DEFAULT);
    }

    /** The terminal the builder in {@code stack} is aimed at, when it is aimed at one in {@code player}'s world. */
    public static Optional<BlockPos> aim(final ItemStack stack, final Player player) {
        final GlobalPos aim = stack.get(AIM.get());
        return aim == null || aim.dimension() != player.level().dimension() ? Optional.empty()
                : Optional.of(aim.pos());
    }

    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final IPayloadGate<BuilderStatePayload> holdingToSet = (player, payload) -> held(player,
                payload.mainHand()).is(ITEM.get());
        final IPayloadGate<BuilderActionPayload> holdingToAct = (player, payload) -> held(player,
                payload.mainHand()).is(ITEM.get());
        event.registrar(NETWORK_VERSION).playToClient(BuildStatus.TYPE, BuildStatus.STREAM_CODEC,
                MultiblockBuilder::onStatus);
        ClientPayloads.accept(event.registrar(NETWORK_VERSION), BuilderStatePayload.TYPE,
                BuilderStatePayload.STREAM_CODEC, holdingToSet, (payload, player, level) -> {
                    held(player, payload.mainHand()).set(STATE.get(), payload.state());
                    if (!payload.state().on()) {
                        BuildJobs.stop(player);
                    }
                });
        ClientPayloads.accept(event.registrar(NETWORK_VERSION), BuilderActionPayload.TYPE,
                BuilderActionPayload.STREAM_CODEC, holdingToAct, MultiblockBuilder::act);
    }

    /* Runs on the server: a key of the builder in the player's hand asks it to build, hold or stop. */
    private static void act(final BuilderActionPayload payload, final ServerPlayer player, final ServerLevel level) {
        final ItemStack builder = held(player, payload.mainHand());
        switch (payload.action()) {
            case BUILD -> {
                final BuilderState state = state(builder);
                final double reach = BuildBalance.reach();
                aim(builder, player).filter(at -> state.on() && player.blockPosition().distSqr(at) <= reach * reach)
                        .ifPresent(at -> BuildJobs.start(player, at, state));
            }
            case HOLD -> BuildJobs.hold(player);
            case STOP -> BuildJobs.stop(player);
        }
    }

    private static void onStatus(final BuildStatus status, final IPayloadContext context) {
        context.enqueueWork(() -> told.accept(status));
    }

    private static ItemStack held(final Player player, final boolean mainHand) {
        return player.getItemInHand(mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }
}
