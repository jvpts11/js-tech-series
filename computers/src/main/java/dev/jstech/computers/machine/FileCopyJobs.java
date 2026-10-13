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
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.HashMap;
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
 * <p>A machine's copies can be paused and taken up again from a copy window that has the button: while paused their
 * time stands still, and taking them up again moves every one of them on by as long as they stood. A copy over in no
 * time is carried out at once and told to nobody. A copy still under way when the server stops is carried out then,
 * so a file is never lost to a copy that was cut short.
 */
@EventBusSubscriber(modid = JsComputers.MODID)
public final class FileCopyJobs {

    private static final Map<Long, Job> JOBS = new LinkedHashMap<>();
    /** The machines whose copies are paused, and the game time each was paused at. */
    private static final Map<Host, Long> PAUSED = new HashMap<>();
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
        final Host at = new Host(level.dimension(), host.immutable());
        long begins = clockOf(level, at);
        for (final Job job : JOBS.values()) {
            if (job.on(at)) {
                begins = Math.max(begins, job.told().endTick());
            }
        }
        final long id = nextId++;
        // A copy joining a machine whose copies are paused waits with them.
        final long paused = PAUSED.getOrDefault(at, CopyProgressPayload.RUNNING);
        final CopyProgressPayload told = new CopyProgressPayload(at.pos(), id, copy.kind(), copy.name(),
                copy.from(), copy.to(), copy.sizeMb(), (float) copy.mbPerSecond(), begins, begins + ticks, paused,
                false);
        JOBS.put(id, new Job(level.dimension(), player.getUUID(), told, carryOut));
        PacketDistributor.sendToPlayer(player, told);
        return id;
    }

    /**
     * Calls off the copy {@code job} on the machine at {@code host}, which then never happens: the file stays where
     * it was and nothing arrives. The copies queued behind it move up. Whoever started it is told it ended.
     */
    public static void cancel(final ServerLevel level, final BlockPos host, final long job) {
        final Host at = new Host(level.dimension(), host.immutable());
        final Job called = JOBS.get(job);
        if (called == null || !called.on(at)) {
            return;
        }
        JOBS.remove(job);
        final long now = clockOf(level, at);
        final long saved = called.told().endTick() - Math.max(now, called.told().startTick());
        // What was queued behind it on the same machine starts that much sooner.
        for (final Map.Entry<Long, Job> entry : JOBS.entrySet()) {
            final Job later = entry.getValue();
            if (later.on(at) && later.told().startTick() >= called.told().endTick()) {
                entry.setValue(later.retold(later.told().shifted(-saved)));
            }
        }
        if (runningOn(level, host) == 0) {
            PAUSED.remove(at);
        }
        final ServerPlayer player = level.getServer().getPlayerList().getPlayer(called.player());
        if (player != null) {
            PacketDistributor.sendToPlayer(player, called.told().ended());
            for (final Job later : JOBS.values()) {
                if (later.player().equals(called.player()) && later.on(at)) {
                    PacketDistributor.sendToPlayer(player, later.told());
                }
            }
        }
    }

    /**
     * Pauses the copies under way on the machine at {@code host} when {@code pause} is set, and takes them up again
     * when it is not. Paused, their time stands still and nothing is carried out; taken up again, each one starts and
     * ends as much later as they stood. Whoever started them is told either way.
     */
    public static void pause(final ServerLevel level, final BlockPos host, final boolean pause) {
        final Host at = new Host(level.dimension(), host.immutable());
        final long now = level.getGameTime();
        final Long since = PAUSED.get(at);
        if (pause == (since != null) || runningOn(level, host) == 0) {
            return;
        }
        if (pause) {
            PAUSED.put(at, now);
        } else {
            PAUSED.remove(at);
        }
        for (final Map.Entry<Long, Job> entry : JOBS.entrySet()) {
            final Job job = entry.getValue();
            if (!job.on(at)) {
                continue;
            }
            final CopyProgressPayload told = pause ? job.told().pausedAt(now)
                    : job.told().shifted(now - since).pausedAt(CopyProgressPayload.RUNNING);
            entry.setValue(job.retold(told));
            final ServerPlayer player = level.getServer().getPlayerList().getPlayer(job.player());
            if (player != null) {
                PacketDistributor.sendToPlayer(player, told);
            }
        }
    }

    /** How many copies are under way on the machine at {@code host}, paused or not. */
    public static int runningOn(final ServerLevel level, final BlockPos host) {
        final Host at = new Host(level.dimension(), host);
        int count = 0;
        for (final Job job : JOBS.values()) {
            if (job.on(at)) {
                count++;
            }
        }
        return count;
    }

    /** Whether the copies on the machine at {@code host} are paused. */
    public static boolean pausedOn(final ServerLevel level, final BlockPos host) {
        return PAUSED.containsKey(new Host(level.dimension(), host));
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
            if (level == null || !PAUSED.containsKey(job.host()) && level.getGameTime() >= job.told().endTick()) {
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
        PAUSED.clear();
        for (final Job job : left) {
            job.carryOut().run();
        }
        nextId = 1L;
    }

    /* The game time the copies on a machine are at: now, or where they stood when they were paused. */
    private static long clockOf(final ServerLevel level, final Host at) {
        return PAUSED.getOrDefault(at, level.getGameTime());
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
     * @param from        the folder it comes from, the volume's name for its root
     * @param to          the folder it goes to, empty for a deletion
     * @param sizeMb      how much it weighs, in the megabytes its disk counts
     * @param mbPerSecond how fast it goes
     */
    public record Copy(byte kind, String name, Text from, Text to, long sizeMb, double mbPerSecond) {
    }

    /* A machine, by the dimension it stands in and where. */
    private record Host(ResourceKey<Level> dimension, BlockPos pos) {
    }

    /** A copy under way: where, for whom, what its player was told, and what carries it out. */
    private record Job(ResourceKey<Level> dimension, UUID player, CopyProgressPayload told, Runnable carryOut) {

        /* The machine it runs on. */
        Host host() {
            return new Host(dimension, told.hostPos());
        }

        /* Whether it runs on that machine. */
        boolean on(final Host at) {
            return dimension.equals(at.dimension()) && told.hostPos().equals(at.pos());
        }

        /* The same copy, its player told something new of it. */
        Job retold(final CopyProgressPayload now) {
            return new Job(dimension, player, now, carryOut);
        }
    }
}
