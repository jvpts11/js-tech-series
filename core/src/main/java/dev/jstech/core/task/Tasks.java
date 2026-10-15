/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.task;

import dev.jstech.core.JsCore;
import dev.jstech.core.diagnostic.Section;
import dev.jstech.core.log.CoreLogs;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * Work the server does away from its tick: a search through a large network, a file read, a plan worked out. The work
 * runs on a virtual thread of its own and must not touch the world; its result comes back on the server's thread at
 * the end of a tick, where it may, within {@value #BUDGET_MILLIS} ms of each tick:
 *
 * <pre>{@code
 * Tasks.onServer(() -> planner.plan(request), plan -> machine.start(plan));
 * }</pre>
 *
 * <p>Whatever has not come back when the server stops is cancelled, so nothing reaches the next world.
 */
@ApiStatus.Experimental
public final class Tasks {

    /** How much of each server tick results may take as they come back, in milliseconds. */
    public static final int BUDGET_MILLIS = 2;

    private static final Logger LOGGER = CoreLogs.TASKS.logger();
    /* The results coming back on the server's thread, timed, named in profilers and watched for stalls. */
    private static final Section SECTION = Section.builder(
            ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "task_results"), CoreLogs.TASKS).declare();
    private static final long BUDGET_NANOS = BUDGET_MILLIS * 1_000_000L;
    private static final TaskRunner SERVER = TaskRunner.onVirtualThreads();

    private Tasks() {
    }

    /** Runs {@code work} away from the tick, and hands its result to {@code onDone} on the server's thread. */
    public static <T> Task<T> onServer(final Callable<? extends T> work, final Consumer<? super T> onDone) {
        return onServer(work, onDone, failure -> LOGGER.error("a task failed away from the tick", failure));
    }

    /** The same, handing what the work threw to {@code onFailure}, on the server's thread too. */
    public static <T> Task<T> onServer(final Callable<? extends T> work, final Consumer<? super T> onDone,
                                       final Consumer<? super Throwable> onFailure) {
        return SERVER.submit(work, onDone, onFailure);
    }

    /** The runner behind {@link #onServer}: how many of its tasks are still out, and how many wait to come back. */
    public static TaskRunner server() {
        return SERVER;
    }

    /** Hands the game's bus the end of each tick and the server's stop, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            if (SERVER.waiting() > 0) {
                SECTION.run(() -> SERVER.drain(BUDGET_NANOS));
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> SERVER.cancelAll());
    }
}
