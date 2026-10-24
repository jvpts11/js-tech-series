/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder;

import dev.jstech.core.multiblock.BuildBalance;
import dev.jstech.core.multiblock.BuildOrder;
import dev.jstech.core.multiblock.MultiblockBlocks;
import dev.jstech.core.protection.Actor;
import dev.jstech.core.protection.ProtectedAction;
import dev.jstech.core.protection.Protection;
import dev.jstech.core.team.CorePermissions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * What the Multiblock Builders are building on the server, one job for each player: a block at a time, at the pace
 * the settings give, each block taken from the player's chosen source and placed as the player would place it, so it
 * is theirs and the claims and protections round it have their say. A job holds when its player asks, and stops when
 * it is built, when the player walks away, when a place is protected or blocked, or when the source runs out; the
 * player is told each time. Read and changed on the server thread only.
 */
public final class BuildJobs {

    private static final Map<UUID, Job> JOBS = new HashMap<>();
    private static final int TICKS_PER_SECOND = 20;

    private BuildJobs() {
    }

    /** Hands the game's bus what this answers, once, from the Core's constructor. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> JOBS.remove(event.getEntity().getUUID()));
    }

    /**
     * Starts building the multiblock of the terminal at {@code terminal} for {@code player}, as their builder is
     * set, in place of anything they were building; tells them when they may not, or when it is not a multiblock's.
     */
    public static void start(final ServerPlayer player, final BlockPos terminal, final BuilderState state) {
        JOBS.remove(player.getUUID());
        if (!CorePermissions.has(player, CorePermissions.BUILD_MULTIBLOCKS)) {
            tell(player, new BuildStatus(terminal, BuildStatus.Phase.STOPPED, 0, BuildStatus.Reason.NOT_ALLOWED, ""));
            return;
        }
        if (BuilderPlan.of(player.level(), terminal, player, state).isEmpty()) {
            tell(player, new BuildStatus(terminal, BuildStatus.Phase.STOPPED, 0, BuildStatus.Reason.UNKNOWN, ""));
            return;
        }
        final Job job = new Job(player, player.level().dimension(), terminal.immutable(), state);
        JOBS.put(player.getUUID(), job);
        tell(player, job.status(BuildStatus.Phase.BUILDING, BuildStatus.Reason.NONE, ""));
    }

    /** Holds {@code player}'s building, or carries it on when it is held. */
    public static void hold(final ServerPlayer player) {
        final Job job = JOBS.get(player.getUUID());
        if (job != null) {
            job.held = !job.held;
            tell(player, job.status(job.held ? BuildStatus.Phase.HELD : BuildStatus.Phase.BUILDING,
                    BuildStatus.Reason.NONE, ""));
        }
    }

    /** Stops {@code player}'s building, if they are building. */
    public static void stop(final ServerPlayer player) {
        final Job job = JOBS.remove(player.getUUID());
        if (job != null) {
            tell(player, job.status(BuildStatus.Phase.STOPPED, BuildStatus.Reason.NONE, ""));
        }
    }

    /** What {@code player} is building, if anything: its terminal. */
    public static Optional<BlockPos> building(final ServerPlayer player) {
        final Job job = JOBS.get(player.getUUID());
        return job == null ? Optional.empty() : Optional.of(job.terminal);
    }

    private static void tick() {
        if (JOBS.isEmpty()) {
            return;
        }
        final List<UUID> done = new ArrayList<>();
        for (final Map.Entry<UUID, Job> entry : JOBS.entrySet()) {
            final Job job = entry.getValue();
            final ServerPlayer player = job.player;
            if (player.isRemoved() || player.hasDisconnected() || player.level().dimension() != job.dimension) {
                done.add(entry.getKey());
                continue;
            }
            if (job.held) {
                continue;
            }
            job.credit += BuildBalance.blocksPerSecond();
            while (job.credit >= TICKS_PER_SECOND) {
                job.credit -= TICKS_PER_SECOND;
                final BuildStatus ended = step(player, job);
                if (ended != null) {
                    tell(player, ended);
                    done.add(entry.getKey());
                    break;
                }
            }
        }
        done.forEach(JOBS::remove);
    }

    /* Places the next block of {@code job}, telling the player how it goes; what it ended with, or none to go on. */
    @Nullable
    private static BuildStatus step(final ServerPlayer player, final Job job) {
        final double reach = BuildBalance.reach();
        if (player.blockPosition().distSqr(job.terminal) > reach * reach) {
            return job.status(BuildStatus.Phase.STOPPED, BuildStatus.Reason.TOO_FAR, "");
        }
        final Level level = player.level();
        final Optional<BuilderPlan> plan = BuilderPlan.of(level, job.terminal, player, job.state);
        if (plan.isEmpty()) {
            return job.status(BuildStatus.Phase.STOPPED, BuildStatus.Reason.UNKNOWN, "");
        }
        final List<BuildOrder.Step> steps = plan.get().order().steps();
        if (steps.isEmpty()) {
            return plan.get().view().wrong() > 0
                    ? job.status(BuildStatus.Phase.STOPPED, BuildStatus.Reason.WRONG_LEFT, "")
                    : job.status(BuildStatus.Phase.BUILT, BuildStatus.Reason.NONE, "");
        }
        final BuildOrder.Step next = steps.getFirst();
        final Block block = MultiblockBlocks.blockOf(next.block());
        if (next.lacking() || block == null || !(block.asItem() instanceof BlockItem item)) {
            return job.status(BuildStatus.Phase.STOPPED, BuildStatus.Reason.RAN_OUT, next.block());
        }
        final BlockPos at = new BlockPos(next.x(), next.y(), next.z());
        if (!Protection.may(Actor.of(player), ProtectedAction.PLACE, player.serverLevel(), at)
                || !level.mayInteract(player, at)) {
            return job.status(BuildStatus.Phase.STOPPED, BuildStatus.Reason.PROTECTED, "");
        }
        final IBuildSource source = BuildSources.get(job.state.source());
        final ItemStack stack = source.take(player, item);
        if (stack.isEmpty()) {
            return job.status(BuildStatus.Phase.STOPPED, BuildStatus.Reason.RAN_OUT, next.block());
        }
        final BlockSnapshot before = BlockSnapshot.create(level.dimension(), level, at);
        final InteractionResult placed = item.place(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND,
                stack, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false)));
        if (!placed.consumesAction()) {
            source.giveBack(player, stack);
            return job.status(BuildStatus.Phase.STOPPED, BuildStatus.Reason.BLOCKED, "");
        }
        // Other mods' claims hear of the block as of one the player placed, and may take it back.
        if (EventHooks.onBlockPlace(player, before, Direction.UP)) {
            before.restore();
            source.giveBack(player, new ItemStack(item));
            return job.status(BuildStatus.Phase.STOPPED, BuildStatus.Reason.PROTECTED, "");
        }
        source.giveBack(player, stack);
        job.placed++;
        tell(player, job.status(BuildStatus.Phase.BUILDING, BuildStatus.Reason.NONE, ""));
        return null;
    }

    /* A player whose game has no Core, or none at all, as a mod's or a test's fake player, is told nothing. */
    private static void tell(final ServerPlayer player, final BuildStatus status) {
        if (!(player instanceof FakePlayer) && player.connection != null
                && player.connection.hasChannel(BuildStatus.TYPE)) {
            PacketDistributor.sendToPlayer(player, status);
        }
    }

    /** One player's building: whose, where, how the builder was set, how far it got, and whether it is held. */
    private static final class Job {

        private final ServerPlayer player;
        private final ResourceKey<Level> dimension;
        private final BlockPos terminal;
        private final BuilderState state;
        private int placed;
        private int credit;
        private boolean held;

        private Job(final ServerPlayer player, final ResourceKey<Level> dimension, final BlockPos terminal,
                    final BuilderState state) {
            this.player = player;
            this.dimension = dimension;
            this.terminal = terminal;
            this.state = state;
            this.credit = TICKS_PER_SECOND;
        }

        private BuildStatus status(final BuildStatus.Phase phase, final BuildStatus.Reason reason,
                                   final String block) {
            return new BuildStatus(this.terminal, phase, this.placed, reason, block);
        }
    }
}
