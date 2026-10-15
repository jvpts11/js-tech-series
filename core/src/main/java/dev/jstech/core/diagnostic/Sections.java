/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.diagnostic;

import dev.jstech.core.notification.Notification;
import dev.jstech.core.notification.NotificationKind;
import dev.jstech.core.notification.Notifications;
import dev.jstech.core.team.CorePermissions;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Every section declared, what each thread runs as a section now, and the end of each server tick, where the sections
 * with a budget are paced. A section that slows itself says so in its system's log and to the operators who hold
 * {@code jscore.diagnostics.warnings}, and again when it is back to full pace.
 */
public final class Sections {

    private static final Map<ResourceLocation, Section> BY_ID = new ConcurrentHashMap<>();
    private static final List<Section> ALL = new CopyOnWriteArrayList<>();
    /* Every thread that has run a section, with what it runs now; the watchdog lets go of those that ended. */
    private static final Set<Slot> SLOTS = ConcurrentHashMap.newKeySet();
    private static final ThreadLocal<Slot> SLOT = ThreadLocal.withInitial(Sections::newSlot);

    private Sections() {
    }

    /** Every section declared, in the order they were. */
    public static List<Section> all() {
        return List.copyOf(ALL);
    }

    /** The section declared as {@code id}, or none. */
    public static Optional<Section> byId(final ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    /** The sections their budget slowed now. */
    public static List<Section> slowed() {
        return ALL.stream().filter(Section::slowed).toList();
    }

    /** What runs as a section on each thread now, each thread's innermost first, with how long it has run. */
    public static List<Now> runningNow() {
        final long now = System.nanoTime();
        final List<Now> out = new ArrayList<>();
        for (final Slot slot : SLOTS) {
            for (Running at = slot.top; at != null; at = at.outer) {
                out.add(new Now(at.section, slot.thread.getName(), now - at.start));
            }
        }
        return out;
    }

    /** Hands the game's bus the end of each tick, and starts the watchdog, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> endTick());
        Watchdog.start();
    }

    static Section declare(final Section section) {
        if (BY_ID.putIfAbsent(section.id(), section) != null) {
            throw DeclarationError.of(section.id().getNamespace(), "the section " + section.id(), "is declared twice",
                    "declare it once, as a static final, and run its work through that field");
        }
        ALL.add(section);
        return section;
    }

    static Running enter(final Section section) {
        final Slot slot = SLOT.get();
        final Running running = new Running(section, slot, System.nanoTime(), slot.top);
        slot.top = running;
        return running;
    }

    static void leave(final Running running) {
        running.slot.top = running.outer;
        running.section.spent(System.nanoTime() - running.start, running.seen);
    }

    static Set<Slot> slots() {
        return SLOTS;
    }

    /* Notifies the operators who are to be told, on the server's thread, when there is a server. */
    static void tellOperators(final NotificationKind kind, final Text text) {
        final MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !DiagnosticBalance.warnOperators()) {
            return;
        }
        final Notification notification = kind.of(kind.name().text(), text);
        server.execute(() -> {
            for (final ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (CorePermissions.has(player, CorePermissions.DIAGNOSTIC_WARNINGS)) {
                    Notifications.send(player, notification);
                }
            }
        });
    }

    /* The end of a server tick: each section keeps what it took and is paced, and a change of pace is said. */
    private static void endTick() {
        for (final Section section : ALL) {
            final int before = section.endTick();
            final int after = section.stride();
            if (after == before) {
                continue;
            }
            final Logger logger = section.system().logger();
            final String average = Diagnostics.millis(section.averageNanos());
            if (before == 1) {
                final String budget = Diagnostics.millis(section.budgetNanos());
                logger.warn("{} takes {} ms a tick against its budget of {} ms, and slows itself to one tick in {}",
                        section, average, budget, after);
                tellOperators(DiagnosticNotifications.SLOWED,
                        DiagnosticTexts.SLOWED.with(section.toString(), average, budget, after));
            } else if (after == 1) {
                logger.info("{} is within its budget again, at {} ms a tick, and runs every tick", section, average);
                tellOperators(DiagnosticNotifications.RECOVERED, DiagnosticTexts.RECOVERED.with(section.toString()));
            } else {
                logger.debug("{} takes {} ms a tick and now runs one tick in {}", section, average, after);
            }
        }
    }

    private static Slot newSlot() {
        final Slot slot = new Slot(Thread.currentThread());
        SLOTS.add(slot);
        return slot;
    }

    /**
     * What one thread runs as a section.
     *
     * @param section the section
     * @param thread  the thread's name
     * @param nanos   how long it has run, in nanoseconds
     */
    public record Now(Section section, String thread, long nanos) {
    }

    /* A thread that runs sections, and the innermost of what it runs now. */
    static final class Slot {

        final Thread thread;
        volatile @Nullable Running top;

        private Slot(final Thread thread) {
            this.thread = thread;
        }
    }

    /*
     * One run of a section, inside whatever run its thread was in when it began. Made new for each run, so the
     * watchdog reads a section and its start together, never one run's section with another's start.
     */
    static final class Running {

        final Section section;
        final Slot slot;
        final long start;
        final @Nullable Running outer;
        /* Set by the watchdog once it has counted the run as a stall. */
        volatile boolean seen;

        private Running(final Section section, final Slot slot, final long start, final @Nullable Running outer) {
            this.section = section;
            this.slot = slot;
            this.start = start;
            this.outer = outer;
        }
    }
}
