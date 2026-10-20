/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.moving;

import dev.jstech.core.region.Box;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Telling each player what they need of the moving structures they see, and only what changed: the chunks of the
 * yard holding a structure's blocks, sent once as it comes into sight, the blocks that change there after it as the
 * game sends any block that changes ({@code ChunkMapMixin}), and where it is, every tick to a player near it and less
 * often the farther they are ({@link SendRate}). A structure at rest is told once and then costs nothing.
 */
public final class StructureSync {

    private static final String VERSION = "1";
    /* A chunk's width, in blocks: the ring of the yard sent round a structure's blocks is a chunk wide. */
    private static final int CHUNK = 16;
    /* For each player, the plots of the structures they see: the chunks of the yard that are theirs to be sent. */
    private static final Map<ServerPlayer, IntSet> WATCHING = new WeakHashMap<>();
    /* For each structure, who sees it and the server's tick each was last told its pose. */
    private static final Map<MovingStructure, Map<ServerPlayer, Long>> TOLD = new WeakHashMap<>();

    private StructureSync() {
    }

    /** Hands the mod's bus the payloads, once, from the Core's constructor. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar(VERSION)
                .playToClient(StructurePosePayload.TYPE, StructurePosePayload.STREAM_CODEC, StructureSync::onPose)
                .playToClient(StructureHandOffPayload.TYPE, StructureHandOffPayload.STREAM_CODEC,
                        StructureSync::onHandOff)
                .playToServer(StructureFootingPayload.TYPE, StructureFootingPayload.STREAM_CODEC,
                        StructureSync::onFooting));
    }

    /**
     * {@code structure}'s blocks are about to leave {@code from} for its plot: who sees that land is told first, so
     * their game draws it as it was until it draws the structure.
     */
    static void arriving(final MovingStructure structure, final ServerLevel level, final Collection<BlockPos> from) {
        if (from.isEmpty()) {
            return;
        }
        final Box box = holding(from);
        final StructureHandOffPayload payload = new StructureHandOffPayload(structure.plot(), false, structure.pose(),
                box);
        for (final ServerPlayer player : seeing(level, box)) {
            tell(player, payload);
        }
    }

    /**
     * {@code structure} is about to be set down at {@code grid} onto {@code onto}: who sees it, or the land, is told
     * first, so their game draws it there until it draws the land with its blocks.
     */
    static void landing(final MovingStructure structure, final ServerLevel level, final StructurePose grid,
                        final Collection<BlockPos> onto) {
        if (onto.isEmpty()) {
            return;
        }
        final Box box = holding(onto);
        final StructureHandOffPayload payload = new StructureHandOffPayload(structure.plot(), true, grid, box);
        final Set<ServerPlayer> told = seeing(level, box);
        told.addAll(watchers(structure));
        for (final ServerPlayer player : told) {
            tell(player, payload);
        }
    }

    /**
     * Whether {@code player} sees the structure whose plot holds the yard's chunk at {@code (chunkX, chunkZ)}, and the
     * chunk is one they are sent of it: one its blocks are in, or of the ring round them.
     */
    public static boolean watches(final ServerPlayer player, final int chunkX, final int chunkZ) {
        final IntSet plots = WATCHING.get(player);
        final int plot = YardLayout.plotOfChunk(chunkX, chunkZ);
        if (plots == null || !plots.contains(plot)) {
            return false;
        }
        if (!(LevelMovingStructures.of(player.serverLevel()).byPlot(plot) instanceof MovingStructure structure)) {
            return false;
        }
        final Box box = sent(structure.bounds());
        return chunkX >= box.minX() >> 4 && chunkX <= box.maxX() >> 4 && chunkZ >= box.minZ() >> 4
                && chunkZ <= box.maxZ() >> 4;
    }

    /** A chunk of the yard finished loading: the players who see its structure are sent it, if it is one of theirs. */
    public static void chunkReady(final ServerLevel level, final LevelChunk chunk) {
        final ChunkPos at = chunk.getPos();
        final IStructureBody body = LevelMovingStructures.of(level).byPlot(YardLayout.plotOfChunk(at.x, at.z));
        if (!(body instanceof MovingStructure structure)) {
            return;
        }
        final Map<ServerPlayer, Long> told = TOLD.get(structure);
        final Box box = sent(structure.bounds());
        if (told == null || at.x < box.minX() >> 4 || at.x > box.maxX() >> 4 || at.z < box.minZ() >> 4
                || at.z > box.maxZ() >> 4) {
            return;
        }
        for (final ServerPlayer player : told.keySet()) {
            send(level, player, chunk);
        }
    }

    /** {@code player} came to see {@code structure}: they are sent its blocks and where it is. */
    static void watch(final MovingStructure structure, final ServerPlayer player) {
        if (!(structure.level() instanceof ServerLevel level)) {
            return;
        }
        WATCHING.computeIfAbsent(player, any -> new IntOpenHashSet()).add(structure.plot());
        TOLD.computeIfAbsent(structure, any -> new WeakHashMap<>()).put(player, level.getGameTime());
        tell(player, payload(structure, level));
        sendChunks(level, player, sent(structure.bounds()), null);
    }

    /** {@code player} no longer sees {@code structure}: its chunks are taken off their game. */
    static void unwatch(final MovingStructure structure, final ServerPlayer player) {
        final Map<ServerPlayer, Long> told = TOLD.get(structure);
        if (told != null) {
            told.remove(player);
        }
        final IntSet plots = WATCHING.get(player);
        if (plots == null || !plots.remove(structure.plot())) {
            return;
        }
        if (!(structure.level() instanceof ServerLevel level) || player.connection == null) {
            return;
        }
        final Box box = sent(structure.bounds());
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                final ChunkPos chunk = new ChunkPos(x, z);
                EventHooks.fireChunkUnWatch(player, chunk, level);
                player.connection.chunkSender.dropChunk(player, chunk);
            }
        }
    }

    /** {@code structure} left its level: everyone who saw it stops. */
    static void forget(final MovingStructure structure) {
        final Map<ServerPlayer, Long> told = TOLD.get(structure);
        if (told == null) {
            return;
        }
        for (final ServerPlayer player : List.copyOf(told.keySet())) {
            unwatch(structure, player);
        }
        TOLD.remove(structure);
    }

    /** {@code structure} moved, or came to rest: who sees it is told, each as often as their distance asks. */
    static void moved(final MovingStructure structure) {
        final Map<ServerPlayer, Long> told = TOLD.get(structure);
        if (told == null || told.isEmpty() || !(structure.level() instanceof ServerLevel level)) {
            return;
        }
        final long now = level.getGameTime();
        final boolean resting = structure.resting();
        StructurePosePayload payload = null;
        for (final Map.Entry<ServerPlayer, Long> each : told.entrySet()) {
            final ServerPlayer player = each.getKey();
            final double distance = Math.sqrt(structure.worldBox().distanceToSqr(player.position()));
            if (!resting && now - each.getValue() < SendRate.every(distance)) {
                continue;
            }
            if (payload == null) {
                payload = payload(structure, level);
            }
            tell(player, payload);
            each.setValue(now);
        }
    }

    /** {@code structure}'s blocks grew out of {@code before}: who sees it is sent the chunks it grew into. */
    static void grew(final MovingStructure structure, final Box before) {
        final Map<ServerPlayer, Long> told = TOLD.get(structure);
        if (told == null || !(structure.level() instanceof ServerLevel level)) {
            return;
        }
        for (final ServerPlayer player : told.keySet()) {
            sendChunks(level, player, sent(structure.bounds()), sent(before));
        }
    }

    /** The players who see {@code structure} now. */
    public static List<ServerPlayer> watchers(final MovingStructure structure) {
        final Map<ServerPlayer, Long> told = TOLD.get(structure);
        return told == null ? List.of() : new ArrayList<>(told.keySet());
    }

    /*
     * The part of the yard a player who sees a structure filling {@code bounds} is sent: the chunks its blocks are in
     * and the ring of chunks round them. The player's game works its own light out again as blocks change, from the
     * chunks beside, as it does for the land, which always has them; a chunk of the yard sent alone has none, and
     * its light came out dark.
     */
    private static Box sent(final Box bounds) {
        return new Box(bounds.minX() - CHUNK, bounds.minY(), bounds.minZ() - CHUNK, bounds.maxX() + CHUNK,
                bounds.maxY(), bounds.maxZ() + CHUNK);
    }

    /* Sends {@code player} the loaded chunks of the yard holding {@code box} and not {@code skip}. */
    private static void sendChunks(final ServerLevel level, final ServerPlayer player, final Box box,
                                   @Nullable final Box skip) {
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                if (skip != null && x >= skip.minX() >> 4 && x <= skip.maxX() >> 4 && z >= skip.minZ() >> 4
                        && z <= skip.maxZ() >> 4) {
                    continue;
                }
                final LevelChunk chunk = level.getChunkSource().chunkMap.getChunkToSend(ChunkPos.asLong(x, z));
                if (chunk != null) {
                    send(level, player, chunk);
                }
            }
        }
    }

    private static void send(final ServerLevel level, final ServerPlayer player, final LevelChunk chunk) {
        if (player.connection == null) {
            return;
        }
        player.connection.chunkSender.markChunkPendingToSend(chunk);
        EventHooks.fireChunkWatch(player, chunk, level);
    }

    /* Tells a player's game of a structure, if it is a game that hears it: one without the Core does not. */
    private static void tell(final ServerPlayer player, final CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload.type())) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /* The box holding every one of {@code places}, of which there is at least one. */
    private static Box holding(final Collection<BlockPos> places) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (final BlockPos at : places) {
            minX = Math.min(minX, at.getX());
            minY = Math.min(minY, at.getY());
            minZ = Math.min(minZ, at.getZ());
            maxX = Math.max(maxX, at.getX());
            maxY = Math.max(maxY, at.getY());
            maxZ = Math.max(maxZ, at.getZ());
        }
        return new Box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /* The players whose view holds a chunk of the world's box {@code box}. */
    private static Set<ServerPlayer> seeing(final ServerLevel level, final Box box) {
        final Set<ServerPlayer> out = new HashSet<>();
        for (int x = box.minX() >> 4; x <= box.maxX() >> 4; x++) {
            for (int z = box.minZ() >> 4; z <= box.maxZ() >> 4; z++) {
                out.addAll(level.getChunkSource().chunkMap.getPlayers(new ChunkPos(x, z), false));
            }
        }
        return out;
    }

    private static StructurePosePayload payload(final MovingStructure structure, final ServerLevel level) {
        final double[] velocity = structure.velocity();
        final double[] spin = structure.spin();
        return new StructurePosePayload(structure.entity().getId(), level.getGameTime(), structure.pose(),
                new float[] {(float) velocity[0], (float) velocity[1], (float) velocity[2]},
                new float[] {(float) spin[0], (float) spin[1], (float) spin[2]});
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onPose(final StructurePosePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> StructureClientHooks.get().pose(payload));
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onHandOff(final StructureHandOffPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> StructureClientHooks.get().handOff(payload));
    }

    private static void onFooting(final StructureFootingPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                StructureFooting.reported(player, payload);
            }
        });
    }
}
