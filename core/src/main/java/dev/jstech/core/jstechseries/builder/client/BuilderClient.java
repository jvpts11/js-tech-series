/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder.client;

import dev.jstech.core.jstechseries.builder.BuildStatus;
import dev.jstech.core.jstechseries.builder.BuilderPlan;
import dev.jstech.core.jstechseries.builder.BuilderState;
import dev.jstech.core.jstechseries.builder.MultiblockBuilder;
import dev.jstech.core.jstechseries.projector.client.ProjectorClient;
import dev.jstech.core.jstechseries.projector.client.ScreenWords;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;

/**
 * The Multiblock Builder on the player's side: its screen, held up when it is used; how the server says the building
 * goes; what it makes of the controller it is aimed at, read again every half second; and, while its SHOW key is on,
 * the multiblock's ghosts in the world, drawn as a projector draws them.
 */
public final class BuilderClient {

    /* What the plan is read again after, in ticks: counts need not follow every block. */
    private static final int READ_EVERY = 10;

    private static volatile BuildStatus status = BuildStatus.IDLE;
    @Nullable
    private static Cached cached;

    private BuilderClient() {
    }

    /** Hands the mod's bus and the game's what this answers, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        MultiblockBuilder.useOpener(hand -> Minecraft.getInstance().setScreen(new MultiblockBuilderScreen(hand)));
        MultiblockBuilder.useListener(told -> status = told);
        ProjectorClient.showAlso(BuilderClient::shown);
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> {
            status = BuildStatus.IDLE;
            cached = null;
        });
    }

    /** How the server last said the building goes. */
    public static BuildStatus status() {
        return status;
    }

    /** What the builder in {@code stack} makes of the controller it is aimed at, read again every half second. */
    public static Optional<BuilderPlan> plan(final ItemStack stack) {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return Optional.empty();
        }
        final Optional<BlockPos> aim = MultiblockBuilder.aim(stack, player);
        if (aim.isEmpty()) {
            return Optional.empty();
        }
        final BuilderState state = MultiblockBuilder.state(stack);
        final long now = minecraft.level.getGameTime();
        final Cached known = cached;
        if (known != null && known.at().equals(aim.get()) && known.state().equals(state)
                && now - known.time() < READ_EVERY) {
            return known.plan();
        }
        final Optional<BuilderPlan> plan = BuilderPlan.of(minecraft.level, aim.get(), player, state);
        cached = new Cached(aim.get(), state, now, plan);
        return plan;
    }

    /** Reads the plan again the next time it is asked, as after its player changed what it is set to. */
    public static void forget() {
        cached = null;
    }

    /* The multiblock the builder in either hand is aimed at, while it is on and its SHOW key is. */
    private static Optional<ProjectorClient.Shown> shown() {
        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return Optional.empty();
        }
        for (final InteractionHand hand : InteractionHand.values()) {
            final ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(MultiblockBuilder.ITEM.get())) {
                continue;
            }
            final BuilderState state = MultiblockBuilder.state(stack);
            if (!state.on() || !state.show()) {
                continue;
            }
            final Optional<BuilderPlan> plan = plan(stack);
            final Optional<BlockPos> aim = MultiblockBuilder.aim(stack, player);
            if (plan.isPresent() && aim.isPresent()) {
                return Optional.of(new ProjectorClient.Shown(plan.get().view(), plan.get().name(),
                        ScreenWords.modName(BuiltInRegistries.BLOCK.getKey(minecraft.level.getBlockState(aim.get())
                                .getBlock()))));
            }
        }
        return Optional.empty();
    }

    /** What a plan was read as, of which controller, with the builder set how, and when. */
    private record Cached(BlockPos at, BuilderState state, long time, Optional<BuilderPlan> plan) {
    }
}
