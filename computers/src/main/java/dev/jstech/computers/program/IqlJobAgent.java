/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.advancement.JscEvents;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.eventlog.NetworkWorldEvents;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.program.iql.IIqlCondition;
import dev.jstech.computers.program.iql.IIqlView;
import dev.jstech.computers.program.iql.IqlConditionParser;
import dev.jstech.computers.program.iql.IqlDefinition;
import dev.jstech.computers.program.iql.IqlDuration;
import dev.jstech.computers.program.iql.IqlEventTrigger;
import dev.jstech.computers.program.iql.IqlReachedTrigger;
import dev.jstech.computers.program.iql.IqlSavedObject;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.core.world.event.WorldEvent;
import dev.jstech.core.world.event.WorldEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The IQL Engine's job agent, the "SQL Server Agent". Each Mainframe owns one; it runs on the Mainframe
 * tick (only while the Engine is installed and running) and fires the catalog's jobs by their trigger:
 * {@code EVERY <duration>} on a schedule, {@code WHEN <condition>} on the rising edge of the condition
 * (so it fires once when the condition becomes true, not every tick it stays true), and
 * {@code WHEN EVENT <kind> COMING | UNDER WAY | ENDED} at that step of a world event the network sees
 * ({@link IqlEventTrigger}), and {@code WHEN ERA <era> REACHED} or {@code WHEN TIER <tier> REACHED} as the team
 * that owns the network reaches that step ({@link IqlReachedTrigger}). Firing runs the job's
 * body through the {@link IqlEngine}, producing ordinary network Operations, and there is no special per-job
 * cost beyond those Operations. The agent evaluates every {@value #EVAL_INTERVAL} ticks to keep the
 * condition checks cheap; its scheduling state is transient (a reload reschedules from the next tick).
 */
public final class IqlJobAgent {

    private static final int EVAL_INTERVAL = 10;
    private static final int QUERY_ROW_LIMIT = 64;

    private long clock;
    private final Map<String, Long> lastFired = new HashMap<>();
    private final Map<String, Boolean> lastCondition = new HashMap<>();
    /* The steps the network's team reached since the agent last looked, each heard once by the jobs waiting on it. */
    private final List<Reached> reached = new ArrayList<>();

    public void tick(final MainframeBlockEntity mainframe, final ServerLevel level) {
        clock++;
        if (clock % EVAL_INTERVAL != 0) {
            return;
        }
        /*
         * Jobs are the Automation Engine's: "at this time, or when this happens, do that", the same whichever
         * Network Operations Engine plans the network's work. A job's body is the network's language, so it still
         * needs an engine running to be answered. A step reached while no engine runs goes unheard, as a world event
         * that passes while the network is not watching.
         */
        if (!mainframe.isAutomationEngineActive()) {
            reached.clear();
            return;
        }
        final List<IqlSavedObject> jobs = mainframe.iqlCatalog().ofType(IqlDefinition.ObjectType.JOB);
        if (jobs.isEmpty()) {
            reached.clear();
            return;
        }
        IIqlView machine = null;
        for (final IqlSavedObject job : jobs) {
            if (mainframe.isJobPaused(job.name())) {
                continue; // a paused job never fires until it is restarted from the Processes tab
            }
            if (shouldFire(job, mainframe, level)) {
                if (machine == null) {
                    machine = IqlEngine.viewOf(new ServerCliComputer(mainframe, level));
                }
                final IIqlView caller = machine;
                // What the job sets on a bus is marked with the job's name.
                IqlEngine.asJob(job.name(),
                        () -> mainframe.networkOperations().query(caller, job.body(), QUERY_ROW_LIMIT));
                JscEvents.awardOperator(mainframe, JscEvents.IQL_JOB);
            }
        }
        reached.clear();
    }

    /**
     * Tells the agent that the team owning its network reached {@code step} along the axis {@code axis}, both by the
     * ids the progress saves them by; the jobs waiting on it fire at the agent's next look.
     */
    public void reached(final String axis, final String step) {
        reached.add(new Reached(axis, step));
    }

    /** Re-arms a job's trigger so it reschedules from now (EVERY) or resets its edge detector (WHEN). */
    public void rearm(final String jobName) {
        lastFired.remove(jobName);
        lastCondition.remove(jobName);
    }

    private boolean shouldFire(final IqlSavedObject job, final MainframeBlockEntity mainframe,
                               final ServerLevel level) {
        return switch (job.triggerKind()) {
            case EVERY -> shouldFireEvery(job);
            case WHEN -> shouldFireWhen(job, mainframe, level);
            case NONE -> false;
        };
    }

    private boolean shouldFireEvery(final IqlSavedObject job) {
        final long interval;
        try {
            interval = IqlDuration.toTicks(job.triggerSpec());
        } catch (final IllegalArgumentException e) {
            return false; // a malformed interval never fires (the ISMS surfaces the error on create)
        }
        if (interval <= 0) {
            return false;
        }
        final Long last = lastFired.get(job.name());
        if (last == null) {
            lastFired.put(job.name(), clock); // schedule from now rather than firing immediately
            return false;
        }
        if (clock - last >= interval) {
            lastFired.put(job.name(), clock);
            return true;
        }
        return false;
    }

    private boolean shouldFireWhen(final IqlSavedObject job, final MainframeBlockEntity mainframe,
                                   final ServerLevel level) {
        final IqlReachedTrigger step = IqlReachedTrigger.parse(job.triggerSpec());
        if (step != null) {
            for (final Reached heard : reached) {
                if (step.names(heard.axis(), heard.step())) {
                    return true;
                }
            }
            return false;
        }
        final IqlEventTrigger event = IqlEventTrigger.parse(job.triggerSpec());
        if (event != null) {
            return shouldFireOnEvent(job, event, mainframe, level);
        }
        final IIqlCondition condition;
        try {
            condition = IqlConditionParser.parse(job.triggerSpec());
        } catch (final IllegalArgumentException e) {
            return false;
        }
        final boolean now = condition.matches(field -> resolveField(field, mainframe, level));
        final boolean was = lastCondition.getOrDefault(job.name(), false);
        lastCondition.put(job.name(), now);
        return now && !was; // rising edge: fire once when the condition turns true
    }

    /*
     * A trigger on a world event: COMING and UNDER WAY fire as the network first sees the event so, ENDED as it stops
     * seeing it under way. What the network sees is read off the world's calendar at each evaluation.
     */
    private boolean shouldFireOnEvent(final IqlSavedObject job, final IqlEventTrigger trigger,
                                      final MainframeBlockEntity mainframe, final ServerLevel level) {
        final NetworkUuid network = mainframe.networkUuid();
        if (network == null) {
            return false;
        }
        final long now = level.getGameTime();
        boolean seen = false;
        for (final WorldEvent event : WorldEvents.planned(level)) {
            if (trigger.names(event.type().toString())) {
                seen |= trigger.stage() == IqlEventTrigger.Stage.COMING
                        ? NetworkWorldEvents.coming(level, network, event, now)
                        : NetworkWorldEvents.underWay(level, network, event, now);
            }
        }
        final boolean was = lastCondition.getOrDefault(job.name(), false);
        lastCondition.put(job.name(), seen);
        return trigger.stage() == IqlEventTrigger.Stage.ENDED ? was && !seen : seen && !was;
    }

    /** Resolves the fields a WHEN condition can test today: {@code qty(<item>)} against the live network. */
    private static String resolveField(final String field, final MainframeBlockEntity mainframe,
                                       final ServerLevel level) {
        final String lower = field.toLowerCase(Locale.ROOT);
        if (lower.startsWith("qty(") && lower.endsWith(")")) {
            final String itemName = field.substring(4, field.length() - 1).strip();
            return Long.toString(networkQuantity(mainframe, level, itemName));
        }
        return null; // an unknown field makes the comparison false, so the job stays idle
    }

    private static long networkQuantity(final MainframeBlockEntity mainframe, final ServerLevel level,
                                        final String itemName) {
        final NetworkUuid net = mainframe.networkUuid();
        final Item item = resolveItem(itemName);
        if (net == null || item == null) {
            return 0L;
        }
        return NetworkStorage.of(level, net).query().getOrDefault(StorageKey.of(item), 0L);
    }

    private static Item resolveItem(final String name) {
        final String id = name.contains(":") ? name : "minecraft:" + name;
        final ResourceLocation location = ResourceLocation.tryParse(id.toLowerCase(Locale.ROOT));
        return location == null ? null : BuiltInRegistries.ITEM.getOptional(location).orElse(null);
    }

    /* A step the team reached, by the ids of its axis and of the step. */
    private record Reached(String axis, String step) {
    }
}
