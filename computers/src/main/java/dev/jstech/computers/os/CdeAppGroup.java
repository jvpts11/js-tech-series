/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.os;

import java.util.Set;

/**
 * The groups CDE's Application Manager sorts a machine's programs into, so that nobody has to know what a
 * program is called to find it: what a desk is used for every day, the tools kept beside it, what speaks to the
 * network, and the games.
 *
 * <p>A program belongs to exactly one group, told from the path of its id alone. Whatever is named in no list is
 * a tool, which is where a program somebody else wrote lands too.
 */
public enum CdeAppGroup {

    DESKTOP_APPS("Desktop_Apps", 0),
    DESKTOP_TOOLS("Desktop_Tools", 1),
    NETWORK("Network", 2),
    GAMES("Games", 3);

    private final String label;

    /** Its place in the Application Manager, said outright rather than read off the order. */
    private final int place;

    private static final Set<String> EVERYDAY = Set.of("files", "editor", "command_prompt", "calculator", "settings");

    private static final Set<String> ON_THE_NETWORK = Set.of("network", "network_manager", "nms", "gateway_manager",
            "remote_control", "cluster_manager", "crafting_manager", "storage_insights", "craft_planner",
            "automation_manager", "pattern_studio");

    private static final Set<String> PLAYED = Set.of("minesweeper");

    CdeAppGroup(final String label, final int place) {
        this.label = label;
        this.place = place;
    }

    /** The group of the program whose id has that path. */
    public static CdeAppGroup of(final String programPath) {
        if (EVERYDAY.contains(programPath)) {
            return DESKTOP_APPS;
        }
        if (ON_THE_NETWORK.contains(programPath)) {
            return NETWORK;
        }
        return PLAYED.contains(programPath) ? GAMES : DESKTOP_TOOLS;
    }

    /** The group so labelled, or null when no group is. */
    public static CdeAppGroup labelled(final String label) {
        for (final CdeAppGroup group : values()) {
            if (group.label.equals(label)) {
                return group;
            }
        }
        return null;
    }

    /** What the group is called, the way CDE wrote its folders. */
    public String label() {
        return this.label;
    }

    public int place() {
        return this.place;
    }
}
