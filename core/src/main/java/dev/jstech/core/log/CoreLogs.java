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
    /** The debug screen's panels. */
    public static final LogSystem DIAGNOSTICS = LogSystems.declare(JsCore.MODID, "diagnostics");
    /** Dimensions made while the game runs, and what each dimension does to those in it. */
    public static final LogSystem DIMENSIONS = LogSystems.declare(JsCore.MODID, "dimensions");
    /** The manuals: their pages, scenes and places. */
    public static final LogSystem GUIDE = LogSystems.declare(JsCore.MODID, "guide");
    /** The programming languages and their registry. */
    public static final LogSystem LANGUAGES = LogSystems.declare(JsCore.MODID, "languages");
    /** The recordings: their store, their sending and their playing. */
    public static final LogSystem MEDIA = LogSystems.declare(JsCore.MODID, "media");
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

    private CoreLogs() {
    }
}
