/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.program;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Well-known program ids and a thin CLI-facing view over the single program registry
 * ({@link OsRegistry}). The descriptors themselves (platforms, hardware minimums, host scope, display
 * name) live on {@link ProgramSpec} and are registered once through the addon API, so there is no longer
 * a second, parallel program store here. These constants are just the ids the mod's own code refers to.
 */
public final class Programs {

    /** The Command Prompt: the CLI that every computer ships with. */
    public static final ResourceLocation COMMAND_PROMPT =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "command_prompt");

    /** The Task Manager: what the machine is running and spending, reached by right-clicking the panel. */
    public static final ResourceLocation TASK_MANAGER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "task_manager");

    /** The file manager every desktop ships, under whatever name that desktop gives it. */
    public static final ResourceLocation FILES =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "files");

    /** Settings: the machine's own knobs, and the list of everything installed on it. */
    public static final ResourceLocation SETTINGS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "settings");

    /** IQL Server Management Studio: the Midsoft IQL Server's console, installed by the player. */
    public static final ResourceLocation ISMS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "isms");

    /**
     * The Midsoft IQL Server: the Network Operations Engine every Mainframe ships with (the network's "SQL Server").
     * The id is the one the package always had, which is also what the prompt's {@code iqlengine} command names.
     */
    public static final ResourceLocation IQL_ENGINE =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "iqlengine");

    /** NextgreIQL: the explicit engine, which shows how it plans and lets the player take part in it. */
    public static final ResourceLocation NEXTGRE_IQL =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "nextgreiql");

    /** Prophet YourIQL: the engine told what state to keep, which reacts to what changes. */
    public static final ResourceLocation YOURIQL =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "youriql");

    /** Prophet Reactive Console: Prophet YourIQL's own tool, where its states are declared and watched. */
    public static final ResourceLocation PROPHET_CONSOLE =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "prophet_console");

    /** Nextgre Planner Studio: NextgreIQL's own tool, where its plans are explained and its planner is tuned. */
    public static final ResourceLocation NEXTGRE_STUDIO =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "nextgre_studio");

    /**
     * The Crafting Manager: the desktop app that moves {@code .craft} recipe files between removable
     * media and a Crafting Computer's recipe store. It installs only on a Crafting Computer and needs a
     * Crafting Card to run its actions.
     */
    public static final ResourceLocation CRAFTING_MANAGER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "crafting_manager");

    public static final ResourceLocation PATTERN_STUDIO =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "pattern_studio");

    /** Workshop: the personal-use cards of a Personal Computer, worked with the player's own items. */
    public static final ResourceLocation WORKSHOP =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "workshop");

    /** Cluster Manager: the Cluster Management Computer's front for every cluster on its network. */
    public static final ResourceLocation CLUSTER_MANAGER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "cluster_manager");

    /** Gateway Manager: the host computer's front for the Network Gateways on its peripheral ports. */
    public static final ResourceLocation GATEWAY_MANAGER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "gateway_manager");

    /** Minesweeper: the classic game, installed by the player like any other add-on. */
    public static final ResourceLocation MINESWEEPER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "minesweeper");

    /** Storage Insights: a dashboard over the network's contents (totals, top types, low stock). */
    public static final ResourceLocation STORAGE_INSIGHTS =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "storage_insights");

    /** Craft Planner: previews a craft's stages and ingredient bill before the player commits to it. */
    public static final ResourceLocation CRAFT_PLANNER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "craft_planner");

    /** The Automation Engine: a Mainframe service (like the IQL Engine) that runs the saved jobs. */
    public static final ResourceLocation AUTOMATION_ENGINE =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "automation_engine");

    /** The Automation Manager: the desktop front-end that creates and manages jobs for the Engine. */
    public static final ResourceLocation AUTOMATION_MANAGER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "automation_manager");

    /** The Mirror: the Mainframe service every package manager on the network installs from. */
    public static final ResourceLocation MIRROR =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mirror");

    /** The Messenger Service: keeps the network's conversations, on a server in a rack. */
    public static final ResourceLocation MESSENGER_SERVICE =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "messenger_service");

    /** KnotHub: keeps the source a network is working on, revision by revision, on a server in a rack. */
    public static final ResourceLocation KNOT_HUB =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "knothub");

    /** Predictive Cache: a server service that keeps its bay's most-wanted items staged, so reads come back sooner. */
    public static final ResourceLocation PREDICTIVE_CACHE =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "predictive_cache");

    /** Load Balancer: a server service that spreads writes over the bay's drives instead of filling them in turn. */
    public static final ResourceLocation LOAD_BALANCER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "load_balancer");

    /** Integrity Monitor: a server service that re-reads its own bay after a drive is pulled while it runs. */
    public static final ResourceLocation INTEGRITY_MONITOR =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "integrity_monitor");

    /** Soundfoundry: Voidsoft's music player. */
    public static final ResourceLocation SOUNDFOUNDRY =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "soundfoundry");

    /** Soundfoundry Server: streams the catalogue and the network's own songs, on a server in a rack. */
    public static final ResourceLocation SOUNDFOUNDRY_SERVER =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "soundfoundry_server");

    private Programs() {
    }

    /** The descriptor registered under {@code id}, or {@code null} when none is. */
    public static ProgramSpec get(final ResourceLocation id) {
        return id == null ? null : OsRegistry.getProgram(id);
    }

    /** Every registered program descriptor. */
    public static List<ProgramSpec> all() {
        return List.copyOf(OsRegistry.programs());
    }

    /** The programs every computer ships with (no install step). */
    public static List<ProgramSpec> installed() {
        final List<ProgramSpec> out = new ArrayList<>();
        for (final ProgramSpec program : OsRegistry.programs()) {
            if (program.preinstalled()) {
                out.add(program);
            }
        }
        return out;
    }
}
