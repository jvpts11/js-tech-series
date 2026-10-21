/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.developer.mcp;

import dev.jstech.core.JsCore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The tools of the MCP servers, the Core's and every mod's, by the game each runs in. A mod's tools are named after it
 * and stay off until the player turns that mod's on, for this run of the game, so a tool whose words another mod wrote
 * never reaches an agent unasked; the Core's are always on.
 */
public final class McpTools {

    private static final Map<McpSide, List<Declared>> DECLARED = new EnumMap<>(McpSide.class);
    private static final Set<String> TURNED_ON = ConcurrentHashMap.newKeySet();

    static {
        for (final McpSide side : McpSide.values()) {
            DECLARED.put(side, new CopyOnWriteArrayList<>());
        }
    }

    private McpTools() {
    }

    /**
     * Declares {@code tool} of the mod {@code modid} for the servers of {@code side}; a mod's tool's name starts with
     * the mod's id and an underscore, so no two mods' tools meet.
     *
     * @throws IllegalArgumentException when its name is taken, or a mod's does not start with the mod's id
     */
    public static void declare(final McpSide side, final String modid, final McpTool tool) {
        if (!JsCore.MODID.equals(modid) && !tool.name().startsWith(modid + "_")) {
            throw new IllegalArgumentException("a tool of " + modid + " is named " + modid + "_...: " + tool.name());
        }
        final List<Declared> tools = DECLARED.get(side);
        if (tools.stream().anyMatch(declared -> declared.tool().name().equals(tool.name()))) {
            throw new IllegalArgumentException("two tools are named " + tool.name());
        }
        tools.add(new Declared(modid, tool));
    }

    /** The tools on for {@code side}, by name, as {@code tools/list} lists them. */
    public static List<McpTool> on(final McpSide side) {
        final List<McpTool> out = new ArrayList<>();
        for (final Declared declared : DECLARED.get(side)) {
            if (isOn(declared.modid())) {
                out.add(declared.tool());
            }
        }
        out.sort(Comparator.comparing(McpTool::name));
        return out;
    }

    /** The tool {@code name} of {@code side}, whether on or off. */
    public static Optional<Declared> find(final McpSide side, final String name) {
        return DECLARED.get(side).stream().filter(declared -> declared.tool().name().equals(name)).findFirst();
    }

    /** The mods other than the Core that declared tools for {@code side}, by id. */
    public static Set<String> mods(final McpSide side) {
        final Set<String> out = new TreeSet<>();
        DECLARED.get(side).forEach(declared -> out.add(declared.modid()));
        out.remove(JsCore.MODID);
        return out;
    }

    /** Whether the tools of {@code modid} are on. */
    public static boolean isOn(final String modid) {
        return JsCore.MODID.equals(modid) || TURNED_ON.contains(modid);
    }

    /** Turns the tools of {@code modid} on or off, for this run of the game; the Core's stay on. */
    public static void turn(final String modid, final boolean on) {
        if (on) {
            TURNED_ON.add(modid);
        } else {
            TURNED_ON.remove(modid);
        }
    }

    /**
     * A tool declared.
     *
     * @param modid the mod that declared it
     * @param tool  the tool
     */
    public record Declared(String modid, McpTool tool) {
    }
}
