/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.bus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A bus's settings written the way software writes them: its address, the IQL statements and the Σ calls that would
 * set the bus as it is set now. A bus's window shows them so a player sees what to type for what they set by hand; only
 * what its era can be set to is written, and only what differs from a new bus, except the mode, which is always said.
 */
public final class BusScript {

    /** What the hours are written under when no program named them. */
    private static final String HOURS_JOB = "bus_hours";

    private BusScript() {
    }

    /** Where software finds the bus: {@code bus://Ore in}. */
    public static String address(final String name) {
        return "bus://" + name;
    }

    /** The IQL statements that set the bus as {@code settings} has it, one a line. */
    public static List<String> iql(final BusSettings settings) {
        final BusAbilities can = settings.abilities();
        final String bus = "SET BUS " + quoted(settings.name()) + " ";
        final List<String> lines = new ArrayList<>();
        if (!settings.powered()) {
            lines.add(bus + "OFF");
        }
        if (can.can(BusFeature.FILTER) && settings.listsAny()) {
            lines.add(bus + "FILTER " + (settings.exclude() ? "ALL BUT " : "ONLY ") + String.join(", ",
                    listed(settings)));
        }
        if (can.can(BusFeature.TAGS) && !settings.tags().isEmpty()) {
            lines.add(bus + "FILTER TAG " + String.join(", ", settings.tags()));
        }
        if (can.can(BusFeature.FUZZY) && settings.fuzzy()) {
            lines.add(bus + "MATCH FUZZY");
        }
        if (can.can(BusFeature.QUANTITIES)) {
            final String quantities = quantities("", settings.keep(), settings.max());
            if (!quantities.isEmpty()) {
                lines.add(bus + quantities);
            }
        }
        if (can.can(BusFeature.ITEM_QUANTITIES)) {
            for (int slot = 0; slot < settings.filter().size(); slot++) {
                final String id = settings.filter().get(slot);
                final String quantities = quantities(shortId(id) + " ", settings.itemKeep().get(slot),
                        settings.itemMax().get(slot));
                if (!id.isEmpty() && !quantities.isEmpty()) {
                    lines.add(bus + quantities);
                }
            }
        }
        if (settings.external()) {
            if (can.can(BusFeature.ACCESS)) {
                lines.add(bus + "ACCESS " + switch (settings.access()) {
                    case BusSettings.READ_ONLY -> "READ ONLY";
                    case BusSettings.WRITE_ONLY -> "WRITE ONLY";
                    default -> "READ AND WRITE";
                });
            }
        } else {
            lines.add(bus + "MODE " + (settings.onDemand() ? "ON DEMAND" : "CONTINUOUS"));
        }
        if (can.can(BusFeature.PRIORITY) && settings.priority() != 0) {
            lines.add(bus + "PRIORITY " + settings.priority());
        }
        if (can.can(BusFeature.CONDITIONS)) {
            for (final BusCondition condition : settings.conditions()) {
                lines.add(switch (condition.kind()) {
                    case STOCK -> bus + "WHEN STOCK " + (condition.onTag()
                            ? "TAG " + condition.subject().substring(1) : shortId(condition.subject()))
                            + " < " + condition.below();
                    case AFTER -> bus + "AFTER BUS " + quoted(condition.subject());
                    case HOURS -> "CREATE JOB " + jobName(settings.setByOf(BusSettings.CONDITIONS)) + " AS " + bus
                            + "ON WHEN TIME BETWEEN " + hour(condition.fromHour()) + " AND "
                            + hour(condition.toHour());
                });
            }
        }
        return lines;
    }

    /**
     * The Σ calls that set the bus as {@code settings} has it, as the library names them: chained on the bus when there
     * is one, and on a variable holding it, a line each, when there are more. A filter of several items is one text.
     */
    public static List<String> sigma(final BusSettings settings) {
        final BusAbilities can = settings.abilities();
        final List<String> calls = new ArrayList<>();
        if (!settings.powered()) {
            calls.add("Off()");
        }
        if (can.can(BusFeature.FILTER) && settings.listsAny()) {
            calls.add((settings.exclude() ? "AllBut(" : "Only(") + text(String.join(", ", listed(settings))) + ")");
        }
        if (can.can(BusFeature.TAGS)) {
            settings.tags().forEach(tag -> calls.add("Tag(" + text(tag) + ")"));
        }
        if (can.can(BusFeature.FUZZY) && settings.fuzzy()) {
            calls.add("Fuzzy(true)");
        }
        if (can.can(BusFeature.QUANTITIES)) {
            addQuantities(calls, "", settings.keep(), settings.max());
        }
        if (can.can(BusFeature.ITEM_QUANTITIES)) {
            for (int slot = 0; slot < settings.filter().size(); slot++) {
                final String id = settings.filter().get(slot);
                if (!id.isEmpty()) {
                    addQuantities(calls, "Item(" + text(shortId(id)) + ").", settings.itemKeep().get(slot),
                            settings.itemMax().get(slot));
                }
            }
        }
        if (settings.external() && can.can(BusFeature.ACCESS)) {
            calls.add(switch (settings.access()) {
                case BusSettings.READ_ONLY -> "ReadOnly()";
                case BusSettings.WRITE_ONLY -> "WriteOnly()";
                default -> "ReadWrite()";
            });
        }
        if (!settings.external() && settings.onDemand()) {
            calls.add("OnDemand()");
        }
        if (can.can(BusFeature.PRIORITY) && settings.priority() != 0) {
            calls.add("Priority(" + settings.priority() + ")");
        }
        if (can.can(BusFeature.CONDITIONS)) {
            for (final BusCondition condition : settings.conditions()) {
                calls.add(switch (condition.kind()) {
                    case STOCK -> condition.onTag()
                            ? "WhenStockTag(" + text(condition.subject().substring(1)) + ", " + condition.below() + ")"
                            : "WhenStock(" + text(shortId(condition.subject())) + ", " + condition.below() + ")";
                    case AFTER -> "After(bus(" + text(condition.subject()) + "))";
                    case HOURS -> "Between(" + condition.fromHour() + ", " + condition.toHour() + ")";
                });
            }
        }
        if (calls.isEmpty()) {
            // A bus with nothing to set: an External Storage Bus of the Vintage has nothing to write at all.
            if (settings.external()) {
                return List.of();
            }
            calls.add("On()");
        }
        final String bus = "bus(" + text(settings.name()) + ")";
        if (calls.size() == 1) {
            return List.of(bus + "." + calls.get(0) + ";");
        }
        final List<String> lines = new ArrayList<>();
        // The type written out rather than var, which the smaller language has not: the lines are both languages'.
        lines.add("Bus b = " + bus + ";");
        calls.forEach(call -> lines.add("b." + call + ";"));
        return lines;
    }

    /**
     * How an id is written where a player types it: a vanilla item by its path alone ({@code iron_ore}), anything
     * else by its namespace and path, a fluid or a chemical after its kind ({@code FLUID water}).
     */
    public static String shortId(final String id) {
        final int bar = id.indexOf('|');
        final String kind = bar < 0 ? "item" : id.substring(0, bar);
        final String location = bar < 0 ? id : id.substring(bar + 1);
        final String path = location.startsWith("minecraft:") ? location.substring("minecraft:".length()) : location;
        return "item".equals(kind) ? path : kind.toUpperCase(Locale.ROOT) + " " + path;
    }

    /** The hour of the day as a clock shows it: {@code 06:00}. */
    public static String hour(final int hour) {
        return String.format(Locale.ROOT, "%02d:00", Math.floorMod(hour, 24));
    }

    private static List<String> listed(final BusSettings settings) {
        return settings.filter().stream().filter(id -> !id.isEmpty()).map(BusScript::shortId).toList();
    }

    private static String quantities(final String item, final int keep, final int max) {
        final List<String> parts = new ArrayList<>();
        if (keep > 0) {
            parts.add("KEEP " + item + keep);
        }
        if (max > 0) {
            parts.add("MAX " + item + max);
        }
        return String.join(" ", parts);
    }

    private static void addQuantities(final List<String> calls, final String on, final int keep, final int max) {
        if (keep <= 0 && max <= 0) {
            return;
        }
        final StringBuilder call = new StringBuilder(on);
        if (keep > 0) {
            call.append("Keep(").append(keep).append(')');
        }
        if (max > 0) {
            call.append(keep > 0 ? "." : "").append("Max(").append(max).append(')');
        }
        calls.add(call.toString());
    }

    /* A job's name from the program that set the hours: lower case, words joined by underscores. */
    private static String jobName(final String program) {
        final String name = program.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
        return name.isEmpty() ? HOURS_JOB : name;
    }

    /* A name in IQL's single quotes, a quote in it doubled. */
    private static String quoted(final String name) {
        return "'" + name.replace("'", "''") + "'";
    }

    /* A string in Σ's double quotes, a quote or a backslash in it escaped. */
    private static String text(final String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
