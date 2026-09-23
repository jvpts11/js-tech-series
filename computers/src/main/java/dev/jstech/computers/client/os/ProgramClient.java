/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.client.NmsApp;
import dev.jstech.computers.os.DesktopEnvironmentDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.PanelStyle;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The client-side half of a program: how its id maps to the desktop window it opens. The
 * platform-independent descriptor ({@code os.ProgramSpec}) lives in common; this registry supplies the
 * one thing that cannot, a factory that builds the {@link IDesktopApp} on the client. Together they are
 * the whole per-program wiring, so a new program is a common {@code ProgramSpec} plus one entry here,
 * and the launcher rail, taskbar and This PC all resolve the window through this one lookup instead of a
 * hardcoded {@code switch}.
 *
 * <p>An add-on registers its own factory the same way from its client setup, keeping the desktop open to
 * third-party programs.
 */
public final class ProgramClient {

    /** Builds a program's window from the desktop context (the host computer, its monitor, and the OS id). */
    @FunctionalInterface
    public interface IDesktopAppFactory {
        IDesktopApp create(BlockPos host, BlockPos monitorPos, ResourceLocation osId);
    }

    private static final Map<ResourceLocation, IDesktopAppFactory> FACTORIES = new LinkedHashMap<>();

    static {
        registerBuiltins();
    }

    private ProgramClient() {
    }

    /** Registers the window factory for {@code id}. Safe to call from any mod's client setup. */
    public static void register(final ResourceLocation id, final IDesktopAppFactory factory) {
        FACTORIES.put(id, factory);
    }

    /** The window factory for {@code id}, or {@code null} when the program opens no desktop window. */
    public static IDesktopAppFactory factory(final ResourceLocation id) {
        return FACTORIES.get(id);
    }

    /** Whether {@code id} opens a desktop window (i.e. has a registered factory). */
    public static boolean hasWindow(final ResourceLocation id) {
        return FACTORIES.containsKey(id);
    }

    private static void registerBuiltins() {
        // Built-in Frames apps.
        register(rl("network"), (host, mon, os) -> new NetworkInteractorApp(host, mon));
        register(rl("this_pc"), (host, mon, os) -> new ThisPcApp(host));
        // CDE's settings are its Style Manager, which the approved desktop keeps to what CDE itself offered.
        register(rl("settings"), (host, mon, os) -> onCde(os) ? new StyleManagerApp() : new SettingsApp(host, mon));
        register(rl("workstation_info"), (host, mon, os) -> new WorkstationInfoApp(host));
        register(rl("help_viewer"), (host, mon, os) -> new HelpViewerApp(host));
        register(rl("files"), (host, mon, os) -> new FilesApp(host, os.getPath(), "", mon));
        register(rl("editor"), (host, mon, os) -> new EditorApp(host));
        register(rl("command_prompt"), (host, mon, os) -> new ShellApp(host, os));
        register(rl("system_monitor"), (host, mon, os) -> new SystemMonitorApp(host));
        register(rl("calculator"), (host, mon, os) -> new CalculatorApp());
        register(rl("network_manager"), (host, mon, os) -> new NetworkManagerApp(host, mon));
        register(rl("task_manager"), (host, mon, os) -> new TaskManagerApp(host, os));
        // Installable programs.
        register(rl("nms"), (host, mon, os) ->
                new NmsApp(host, mon));
        register(rl("crafting_manager"), (host, mon, os) -> new CraftingManagerApp(host));
        register(rl("pattern_studio"), (host, mon, os) -> new PatternStudioApp(host, mon));
        register(rl("cluster_manager"), (host, mon, os) -> new ClusterManagerApp(host));
        register(rl("gateway_manager"), (host, mon, os) -> new GatewayManagerApp(host));
        register(rl("minesweeper"), (host, mon, os) -> new MinesweeperApp(host));
        register(rl("solitaire"), (host, mon, os) -> new SolitaireApp());
        register(rl("snake"), (host, mon, os) -> new SnakeApp());
        register(rl("ark"), (host, mon, os) -> new ArchiverApp(host));
        register(rl("paint"), (host, mon, os) -> new PaintApp(host));
        register(rl("exceed"), (host, mon, os) -> new ExceedApp(host, mon));
        register(rl("messenger"), (host, mon, os) -> new MessengerApp(host, mon));
        register(rl("knot"), (host, mon, os) -> new KnotApp(host, mon));
        register(rl("storage_insights"), (host, mon, os) -> new StorageInsightsApp(host, mon));
        register(rl("craft_planner"), (host, mon, os) -> new CraftPlannerApp(host, mon));
        register(rl("automation_manager"), (host, mon, os) -> new AutomationManagerApp(host, mon));
        register(rl("remote_control"), (host, mon, os) -> new RemoteControlApp(host, mon));
        register(rl("virtual_studio"), (host, mon, os) -> new VirtualStudioApp(host));
        register(rl("virtual_studio_code"), (host, mon, os) -> new VirtualStudioCodeApp(host));
        register(rl("exposure"), (host, mon, os) -> new ExposureApp(host));
        // Linux's disk utility: the same volumes This PC lists, under the name that platform uses.
        register(rl("disks"), (host, mon, os) -> new ThisPcApp(host, "Disks"));
    }

    private static ResourceLocation rl(final String path) {
        return ResourceLocation.fromNamespaceAndPath("jsc", path);
    }

    /** Whether a program is being opened on CDE, whatever system CDE stands on. */
    private static boolean onCde(final ResourceLocation desktopId) {
        final DesktopEnvironmentDef desktop = desktopId == null ? null : OsRegistry.getDesktop(desktopId);
        return desktop != null && desktop.panelStyle() == PanelStyle.CDE;
    }
}
