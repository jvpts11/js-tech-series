/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.log;

import dev.jstech.core.JsCore;

/** The systems of the Core itself that write to the log, each under its own name. */
public final class CoreLogs {

    /** The cable block and the wires laid in it. */
    public static final LogSystem CABLES = LogSystems.declare(JsCore.MODID, "cables");
    /** The settings files: reading, upgrading and writing them. */
    public static final LogSystem CONFIG = LogSystems.declare(JsCore.MODID, "config");
    /** The tools for those who make mods: data and assets reloaded as they change. */
    public static final LogSystem DEVELOPER = LogSystems.declare(JsCore.MODID, "developer");
    /** The debug screen's panels, the watchdog, the cost of the series' work and what it sends over the network. */
    public static final LogSystem DIAGNOSTICS = LogSystems.declare(JsCore.MODID, "diagnostics");
    /** Dimensions made while the game runs, and what each dimension does to those in it. */
    public static final LogSystem DIMENSIONS = LogSystems.declare(JsCore.MODID, "dimensions");
    /** The energy grid of each dimension, moving energy through the wires. */
    public static final LogSystem ENERGY = LogSystems.declare(JsCore.MODID, "energy");
    /** The fluid grid of each dimension, moving fluids through the pipes. */
    public static final LogSystem FLUIDS = LogSystems.declare(JsCore.MODID, "fluids");
    /** The grids of the other states of matter in each dimension: gases, slurries, plasma and the rest. */
    public static final LogSystem MATTER = LogSystems.declare(JsCore.MODID, "matter");
    /** The item grid of each dimension, moving items along the lines that carry them. */
    public static final LogSystem ITEMS = LogSystems.declare(JsCore.MODID, "items");
    /** The lines that group the machines they join, each run a group. */
    public static final LogSystem GROUPS = LogSystems.declare(JsCore.MODID, "groups");
    /** The screens of the mods: their files, their styles and their layout. */
    public static final LogSystem INTERFACE = LogSystems.declare(JsCore.MODID, "interface");
    /** The manuals: their pages, scenes and places. */
    public static final LogSystem GUIDE = LogSystems.declare(JsCore.MODID, "guide");
    /** The programming languages and their registry. */
    public static final LogSystem LANGUAGES = LogSystems.declare(JsCore.MODID, "languages");
    /** The recordings: their store, their sending and their playing. */
    public static final LogSystem MEDIA = LogSystems.declare(JsCore.MODID, "media");
    /** The moving structures: their yard, their entities, assembling and taking them apart. */
    public static final LogSystem MOVING = LogSystems.declare(JsCore.MODID, "moving");
    /** The models drawn with the Core's models: their files, their reading and their animations. */
    public static final LogSystem MODELS = LogSystems.declare(JsCore.MODID, "models");
    /** What the players' games send the server. */
    public static final LogSystem NETWORK = LogSystems.declare(JsCore.MODID, "network");
    /** The robots and the work they do. */
    public static final LogSystem ROBOTS = LogSystems.declare(JsCore.MODID, "robots");
    /** What is kept with a world: states, saved layouts, block entities' fields. */
    public static final LogSystem SAVES = LogSystems.declare(JsCore.MODID, "saves");
    /** The pictures shown in the world on screens. */
    public static final LogSystem SCREENS = LogSystems.declare(JsCore.MODID, "screens");
    /** Work done away from the tick. */
    public static final LogSystem TASKS = LogSystems.declare(JsCore.MODID, "tasks");
    /** The mods the Core works with when they are installed. */
    public static final LogSystem INTEGRATIONS = LogSystems.declare(JsCore.MODID, "integrations");
    /** Teams, owners and who may use what. */
    public static final LogSystem TEAMS = LogSystems.declare(JsCore.MODID, "teams");
    /** The world changing: generation in old chunks, live changes, large edits, world events and fields. */
    public static final LogSystem WORLD = LogSystems.declare(JsCore.MODID, "world");

    private CoreLogs() {
    }
}
