/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.CopyProgressPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/**
 * The file copies under way on the machines this player started them on, as the machines said: when each starts
 * and ends, which is all a copy window needs to draw its progress smoothly. A machine copies one file after another,
 * so its copies make a run; the run lasts until its last copy ends, and a window shows the whole run.
 */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class DesktopCopies {

    /** The copies of each machine's current run, in the order they run, the ended ones kept until the run ends. */
    private static final Map<BlockPos, Map<Long, Entry>> RUNS = new HashMap<>();

    private DesktopCopies() {
    }

    /** Takes what a machine said of a copy: that it starts, or that it ended. */
    public static void accept(final CopyProgressPayload payload) {
        final Map<Long, Entry> run = RUNS.computeIfAbsent(payload.hostPos().immutable(),
                host -> new LinkedHashMap<>());
        run.put(payload.job(), new Entry(payload, payload.done()));
        if (payload.done()) {
            // The file has arrived, or gone: whatever lists that folder lists it again.
            FilesApps.diskChanged();
            if (run.values().stream().allMatch(Entry::ended)) {
                RUNS.remove(payload.hostPos());
            }
        }
    }

    /** The copies of the run under way on the machine at {@code host}, in order; empty when none is. */
    public static List<CopyProgressPayload> run(final BlockPos host) {
        final Map<Long, Entry> run = RUNS.get(host);
        if (run == null) {
            return List.of();
        }
        final List<CopyProgressPayload> out = new ArrayList<>(run.size());
        for (final Entry entry : run.values()) {
            out.add(entry.ended() ? entry.copy().ended() : entry.copy());
        }
        return out;
    }

    /** Whether a copy is under way on the machine at {@code host}. */
    public static boolean copying(final BlockPos host) {
        final Map<Long, Entry> run = RUNS.get(host);
        return run != null && run.values().stream().anyMatch(entry -> !entry.ended());
    }

    /** The game time now, smooth between ticks, which every copy's progress is read against. */
    public static double now() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return 0.0;
        }
        return mc.level.getGameTime() + mc.getTimer().getGameTimeDeltaPartialTick(true);
    }

    /** Leaving a world leaves its machines' copies behind. */
    @SubscribeEvent
    public static void leftTheWorld(final ClientPlayerNetworkEvent.LoggingOut event) {
        RUNS.clear();
    }

    /** One copy of a run, and whether it has ended. */
    private record Entry(CopyProgressPayload copy, boolean ended) {
    }
}
