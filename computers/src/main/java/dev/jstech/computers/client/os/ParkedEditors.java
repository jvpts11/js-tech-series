/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import org.jetbrains.annotations.Nullable;

/**
 * The editors somebody looked away from, kept for when they look back.
 *
 * <p>A terminal editor lives on the screen that shows it, and a screen goes when the player stops looking at
 * the monitor. Walking away from a machine closes nothing on it, so an editor that was open is kept, with what
 * was typed into it and not yet written, and the next look at the same machine finds it as it was left.
 *
 * <p>It belongs to one run of the machine. A machine that has been restarted since has no editor open, and a
 * buffer of files that went with the old run would only be written somewhere that no longer exists, so an
 * editor kept from another run is dropped instead of being brought back.
 */
@EventBusSubscriber(modid = JsComputers.MODID, value = Dist.CLIENT)
public final class ParkedEditors {

    private static final Map<BlockPos, Kept> KEPT = new HashMap<>();

    private ParkedEditors() {
    }

    /** An editor, and the run of the machine it was open in. */
    private record Kept(long session, TtyEditor editor) {
    }

    /** Keeps the editor that was open at that machine's terminal. */
    public static void park(final BlockPos machine, final long session, final TtyEditor editor) {
        KEPT.put(machine.immutable(), new Kept(session, editor));
    }

    /**
     * The editor that was left open at that machine, handed back once and forgotten.
     *
     * @return null when none was left, or when the machine has been restarted since it was
     */
    @Nullable
    public static TtyEditor take(final BlockPos machine, final long session) {
        final Kept kept = KEPT.remove(machine);
        return kept != null && kept.session() == session ? kept.editor() : null;
    }

    /** Leaving a world leaves its machines behind, and the same place in another world is another machine. */
    @SubscribeEvent
    public static void leftTheWorld(final ClientPlayerNetworkEvent.LoggingOut event) {
        KEPT.clear();
    }
}
