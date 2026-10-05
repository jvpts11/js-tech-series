/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.machine;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.CopyProgressPayload;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The file copies under way on the machines: each takes the time its size and its slowest volume say, and is carried
 * out when that time is up, so the file arrives when the copy ends and not before. The player who started it is told
 * when it starts and when it ends, which is what the copy windows are drawn from.
 *
 * <p>A copy over in no time is carried out at once and told to nobody. A copy still under way when the server stops
 * is carried out then, so a file is never lost to a copy that was cut short.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class FileCopyJobs {

    private static final Map<Long, Job> JOBS = new LinkedHashMap<>();
    private static long nextId = 1L;

    private FileCopyJobs() {
    }

    /**
     * Starts a copy on the machine at {@code host} for {@code player}, carried out by {@code carryOut} after
     * {@code ticks}, or at once when it takes none. Returns which copy it is, or 0 for one carried out at once.
     */
    public static long start(final ServerLevel level, final BlockPos host, final ServerPlayer player,
                             final Copy copy, final int ticks, final Runnable carryOut) {
        if (ticks <= 0) {
            carryOut.run();
            return 0L;
        }
        // A machine copies one file after another, so a copy asked for during another starts when that one ends.
        long begins = level.getGameTime();
        for (final Job job : JOBS.values()) {
            if (job.dimension().equals(level.dimension()) && job.told().hostPos().equals(host)) {
                begins = Math.max(begins, job.told().endTick());
            }
        }
        final long id = nextId++;
        final CopyProgressPayload told = new CopyProgressPayload(host.immutable(), id, copy.kind(), copy.name(),
                copy.from(), copy.to(), copy.sizeMb(), (float) copy.mbPerSecond(), begins, begins + ticks, false);
        JOBS.put(id, new Job(level.dimension(), player.getUUID(), told, carryOut));
        PacketDistributor.sendToPlayer(player, told);
        return id;
    }

    /**
     * Calls off the copy {@code job} on the machine at {@code host}, which then never happens: the file stays where
     * it was and nothing arrives. The copies queued behind it move up. Whoever started it is told it ended.
     */
    public static void cancel(final ServerLevel level, final BlockPos host, final long job) {
        final Job called = JOBS.get(job);
        if (called == null || !called.dimension().equals(level.dimension()) || !called.told().hostPos().equals(host)) {
            return;
        }
        JOBS.remove(job);
        final long now = level.getGameTime();
        final long saved = called.told().endTick() - Math.max(now, called.told().startTick());
        // What was queued behind it on the same machine starts that much sooner.
        for (final Map.Entry<Long, Job> entry : JOBS.entrySet()) {
            final Job later = entry.getValue();
            if (later.dimension().equals(level.dimension()) && later.told().hostPos().equals(host)
                    && later.told().startTick() >= called.told().endTick()) {
                entry.setValue(later.movedUp(saved));
            }
        }
        final ServerPlayer player = level.getServer().getPlayerList().getPlayer(called.player());
        if (player != null) {
            PacketDistributor.sendToPlayer(player, called.told().ended());
            for (final Job later : JOBS.values()) {
                if (later.player().equals(called.player()) && later.told().hostPos().equals(host)) {
                    PacketDistributor.sendToPlayer(player, later.told());
                }
            }
        }
    }

    /** How many copies are under way on the machine at {@code host}. */
    public static int runningOn(final ServerLevel level, final BlockPos host) {
        int count = 0;
        for (final Job job : JOBS.values()) {
            if (job.dimension().equals(level.dimension()) && job.told().hostPos().equals(host)) {
                count++;
            }
        }
        return count;
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (JOBS.isEmpty()) {
            return;
        }
        final MinecraftServer server = event.getServer();
        final List<Job> due = new ArrayList<>();
        final Iterator<Job> each = JOBS.values().iterator();
        while (each.hasNext()) {
            final Job job = each.next();
            final ServerLevel level = server.getLevel(job.dimension());
            if (level == null || level.getGameTime() >= job.told().endTick()) {
                due.add(job);
                each.remove();
            }
        }
        // Carried out after the sweep, so a copy that starts another one does not upset the list being read.
        for (final Job job : due) {
            finish(server, job);
        }
    }

    @SubscribeEvent
    public static void onServerStopping(final ServerStoppingEvent event) {
        final List<Job> left = new ArrayList<>(JOBS.values());
        JOBS.clear();
        for (final Job job : left) {
            job.carryOut().run();
        }
        nextId = 1L;
    }

    private static void finish(final MinecraftServer server, final Job job) {
        job.carryOut().run();
        final ServerPlayer player = server.getPlayerList().getPlayer(job.player());
        if (player != null) {
            PacketDistributor.sendToPlayer(player, job.told().ended());
        }
    }

    /**
     * What a copy is, as its windows show it.
     *
     * @param kind        {@link CopyProgressPayload#COPY}, {@link CopyProgressPayload#MOVE} or
     *                    {@link CopyProgressPayload#DELETE}
     * @param name        the file's name
     * @param from        the folder it comes from
     * @param to          the folder it goes to, empty for a deletion
     * @param sizeMb      how much it weighs, in the megabytes its disk counts
     * @param mbPerSecond how fast it goes
     */
    public record Copy(byte kind, String name, String from, String to, long sizeMb, double mbPerSecond) {
    }

    /** A copy under way: where, for whom, what its player was told, and what carries it out. */
    private record Job(ResourceKey<Level> dimension, UUID player, CopyProgressPayload told, Runnable carryOut) {

        /* The same copy starting and ending that many ticks sooner. */
        Job movedUp(final long ticks) {
            final CopyProgressPayload t = told;
            return new Job(dimension, player, new CopyProgressPayload(t.hostPos(), t.job(), t.kind(), t.name(),
                    t.from(), t.to(), t.sizeMb(), t.mbPerSecond(), t.startTick() - ticks, t.endTick() - ticks,
                    false), carryOut);
        }
    }
}
