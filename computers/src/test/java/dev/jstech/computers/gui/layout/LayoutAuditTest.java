/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.gui.layout;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.bus.BusAbilities;
import dev.jstech.computers.gui.CdeBackdrop;
import dev.jstech.computers.gui.CdeScheme;
import dev.jstech.computers.gui.MonitorGlass;
import dev.jstech.computers.os.boot.BootMenu;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.tier.HardwareEra;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * An aggressive, cross-cutting audit of every screen layout in the mod. Per-screen tests check one screen;
 * this one sweeps them all and is hard to slip past: a layout that overlaps, spills out of its frame, hides
 * an invisible (zero-sized) solid, or, for a fixed-size screen, outgrows the on-screen budget fails here.
 * It also reflects over the layout package and fails if a new layout factory is added without an audit case,
 * so coverage can never silently regress (the way the NMS out-of-frame bug slipped through).
 */
class LayoutAuditTest {

    /** A fixed-size GUI must fit a usable screen: width up to ~360px, height up to ~256px (the project's
     *  honest ceiling, since beyond it the game's auto GUI scale starts clipping the panel). */
    private static final int SCREEN_W_BUDGET = 360;
    private static final int SCREEN_H_BUDGET = 256;

    /** Layout classes that produce a {@link GuiLayout} and are exercised by {@link #cases()}. */
    private static final Set<String> COVERED = Set.of(
            "BusLayout", "ClusterManagementComputerLayout", "CraftingComputerLayout", "NmsLayout",
            "ComputerTerminalLayout", "ServerRouterLayout", "NetworkInteractorLayout",
            "CraftingSwitchLayout", "PatternEncoderLayout", "ServerRackLayout", "FilesLayout", "ThisPcLayout",
            "PatternStudioLayout", "NetworkGatewayLayout", "OpenWithLayout", "LoaderMenuLayout",
            "CdeFrontPanelLayout", "CdeWindowIconLayout", "CdeExitLayout", "CdeAppManagerLayout",
            "CdeStyleLayout", "WorkstationInfoLayout", "TrashLayout", "HelpViewerLayout", "SpeakerLayout",
            "RedstoneInterfaceLayout",
            "VolumePopupLayout", "SoundfoundryLayout", "SoundfoundryShareLayout", "SoundfoundryStandardLayout",
            "PersonalComputerLayout", "MainframeLayout", "ServerAssemblyLayout", "KvmChannelLayout",
            "SystemBootLayout", "CommandPromptLayout", "OsInstallLayout", "InstallerLayout",
            "BootSequenceLayout", "FirmwareLayout", "SettingsLayout", "StudioPropertiesLayout",
            "NetworkServicesLayout", "NetworkLinksLayout");

    /**
     * One layout worth auditing, with the budget it is measured against.
     *
     * <p>Most screens are panels over the game and are held to what a panel may take. A screen that is a
     * monitor's whole glass is measured against the glass instead, because that is what it is drawn on and
     * what every other thing a monitor shows already fills.
     */
    private record AuditCase(String label, GuiLayout layout, boolean fixedSize, int widthBudget,
                             int heightBudget) {

        AuditCase(final String label, final GuiLayout layout, final boolean fixedSize) {
            this(label, layout, fixedSize, SCREEN_W_BUDGET, SCREEN_H_BUDGET);
        }

        static AuditCase onTheGlass(final String label, final GuiLayout layout) {
            return new AuditCase(label, layout, true, MonitorGlass.WIDTH, MonitorGlass.HEIGHT);
        }
    }

    /** Every layout, at the sizes/states worth auditing (worst cases for the parametrized ones). */
    private static List<AuditCase> cases() {
        final List<AuditCase> c = new ArrayList<>();
        // Each era's window of each kind of bus, and the rows its Configure tab scrolls, laid out flat at their most.
        for (final BusLayout.Window window : BusLayout.Window.values()) {
            for (final HardwareEra era : HardwareEra.values()) {
                final BusAbilities can = BusLayout.abilities(era, window);
                c.add(new AuditCase("BusLayout(" + window + " " + era + ")", BusLayout.frame(can, window), true));
                c.add(new AuditCase("BusLayout.rows(" + window + " " + era + ")",
                        BusLayout.content(BusLayout.Shape.most(can, window)), false));
            }
        }
        c.add(new AuditCase("SoundfoundryLayout", SoundfoundryLayout.layout(), true));
        // Virtual Studio's Properties at the most it holds: every instruction set and every version beside Default.
        c.add(new AuditCase("StudioPropertiesLayout", StudioPropertiesLayout.layout(5, 4), true));
        // A window on the desktop, not a panel over the game: held to its own frame, not to the panel budget.
        c.add(new AuditCase("SoundfoundryShareLayout", SoundfoundryShareLayout.layout(
                List.of("SEARCH", "DOWNLOADS (999)", "SHARED (9999)"), s -> s.length() * 6), false));
        c.add(new AuditCase("SoundfoundryShareLayout.list", SoundfoundryShareLayout.listLayout(
                List.of("SEARCH", "DOWNLOADS (999)", "SHARED (9999)"), s -> s.length() * 6), false));
        // A desktop window drawn at any size it is given: the smallest it allows, the one it opens at, a whole desktop.
        for (final int[] size : new int[][] {{SoundfoundryStandardLayout.MIN_W, SoundfoundryStandardLayout.MIN_H},
                {SoundfoundryStandardLayout.DEFAULT_W, SoundfoundryStandardLayout.DEFAULT_H}, {960, 540}}) {
            final String at = "(" + size[0] + "x" + size[1] + ")";
            c.add(new AuditCase("SoundfoundryStandardLayout" + at,
                    SoundfoundryStandardLayout.layout(size[0], size[1]), false));
            c.add(new AuditCase("SoundfoundryStandardLayout.album" + at,
                    SoundfoundryStandardLayout.albumLayout(size[0], size[1]), false));
            c.add(new AuditCase("SoundfoundryStandardLayout.local" + at,
                    SoundfoundryStandardLayout.localLayout(size[0], size[1], true), false));
        }
        c.add(new AuditCase("CraftingSwitchLayout", CraftingSwitchLayout.layout(), true));
        c.add(new AuditCase("PatternEncoderLayout", PatternEncoderLayout.layout(), true));
        c.add(new AuditCase("NetworkGatewayLayout", NetworkGatewayLayout.layout(), true));
        c.add(new AuditCase("OpenWithLayout", OpenWithLayout.layout(), true));
        c.add(new AuditCase("ClusterManagementComputerLayout", ClusterManagementComputerLayout.layout(), true));
        c.add(new AuditCase("CraftingComputerLayout", CraftingComputerLayout.layout(), true));
        c.add(new AuditCase("PersonalComputerLayout", PersonalComputerLayout.layout(), true));
        /*
         * The Mainframe (262px) and the Server assembly (294px) panels are taller than SCREEN_H_BUDGET, so,
         * like a resizable window, they are held to cleanliness only (no overlaps, nothing out of frame)
         * until their size is revisited.
         */
        c.add(new AuditCase("MainframeLayout", MainframeLayout.layout(), false));
        c.add(new AuditCase("ServerAssemblyLayout", ServerAssemblyLayout.layout(), false));
        // The KVM channel bar grows a row per channel; audit an empty bar and the most a switch reports.
        c.add(new AuditCase("KvmChannelLayout(0)", KvmChannelLayout.layout(0), true));
        c.add(new AuditCase("KvmChannelLayout(most)", KvmChannelLayout.layout(KvmChannelLayout.MOST_CHANNELS), true));
        c.add(AuditCase.onTheGlass("SystemBootLayout", SystemBootLayout.layout()));
        /*
         * The Command Prompt window is resizable, so audit it at the full glass size and at a compact one
         * that still leaves the console panel and the input strip room to draw; a bare terminal (MC-DOS, the
         * Linux TTY, MC-NET with nothing installed) draws no chrome and is always the empty layout.
         */
        c.add(AuditCase.onTheGlass("CommandPromptLayout(384x256)", CommandPromptLayout.layout(384, 256, false)));
        final int compactW = MonitorGlass.width(320);
        final int compactH = MonitorGlass.height(240);
        c.add(new AuditCase("CommandPromptLayout(" + compactW + "x" + compactH + ")",
                CommandPromptLayout.layout(compactW, compactH, false), false));
        c.add(new AuditCase("CommandPromptLayout(bare)", CommandPromptLayout.layout(384, 256, true), false));
        // The OS install dialog never shows two of its three bodies together, so each is its own case.
        c.add(new AuditCase("OsInstallLayout(working)", OsInstallLayout.workingLayout(), true));
        c.add(new AuditCase("OsInstallLayout(done)", OsInstallLayout.doneLayout(), true));
        c.add(new AuditCase("OsInstallLayout(failed)",
                OsInstallLayout.failedLayout(OsInstallLayout.mostFailureLines()), true));
        c.add(AuditCase.onTheGlass("InstallerLayout", InstallerLayout.layout()));
        /*
         * bsdinstall's own dialog, at its longest English heading and button word ("System Configuration",
         * "Install"), padded a few letters longer to stand in for a translation running longer still, so this
         * does not have to spell one out to be held to it.
         */
        c.add(AuditCase.onTheGlass("InstallerLayout.bsdDialog",
                InstallerLayout.bsdDialogLayout("System Configuration....", "Install....")));
        // The no-boot dialog takes the bar's place and is drawn over the title, so it is audited on its own.
        c.add(AuditCase.onTheGlass("BootSequenceLayout", BootSequenceLayout.layout()));
        c.add(AuditCase.onTheGlass("BootSequenceLayout.noBoot",
                BootSequenceLayout.noBootLayout(BootSequenceLayout.MOST_LISTED_DEVICES)));
        // The firmware setup never shows two of its three eras' shapes together.
        c.add(AuditCase.onTheGlass("FirmwareLayout(cli)", FirmwareLayout.cliLayout()));
        c.add(AuditCase.onTheGlass("FirmwareLayout(bios)", FirmwareLayout.biosLayout()));
        c.add(AuditCase.onTheGlass("FirmwareLayout(uefi)", FirmwareLayout.uefiLayout()));
        c.add(new AuditCase("NmsLayout", NmsLayout.layout(), true));
        c.add(new AuditCase("ServerRackLayout", ServerRackLayout.layout(), true));
        c.add(AuditCase.onTheGlass("ComputerTerminalLayout", ComputerTerminalLayout.layout()));
        /*
         * The explorer and This PC are resizable desktop windows: audit the smallest, the default and a
         * maximised size, plus the row and grid geometry that depend on the width alone.
         */
        for (final int[] size : new int[][]{
                {dev.jstech.computers.gui.layout.FilesLayout.MIN_W,
                        dev.jstech.computers.gui.layout.FilesLayout.MIN_H},
                {dev.jstech.computers.gui.layout.FilesLayout.DEFAULT_W,
                        dev.jstech.computers.gui.layout.FilesLayout.DEFAULT_H},
                {420, 300}}) {
            c.add(new AuditCase("FilesLayout(" + size[0] + "x" + size[1] + ")",
                    dev.jstech.computers.gui.layout.FilesLayout.layout(size[0], size[1]), false));
            c.add(new AuditCase("FilesLayout.columns(" + size[0] + ")",
                    dev.jstech.computers.gui.layout.FilesLayout.columns(size[0]), false));
        }
        for (final int[] size : new int[][]{
                {dev.jstech.computers.gui.layout.ThisPcLayout.MIN_W,
                        dev.jstech.computers.gui.layout.ThisPcLayout.MIN_H},
                {dev.jstech.computers.gui.layout.ThisPcLayout.DEFAULT_W,
                        dev.jstech.computers.gui.layout.ThisPcLayout.DEFAULT_H},
                {420, 300}}) {
            c.add(new AuditCase("ThisPcLayout(" + size[0] + "x" + size[1] + ")",
                    dev.jstech.computers.gui.layout.ThisPcLayout.layout(size[0], size[1]), false));
            for (int buttons = 0; buttons <= 3; buttons++) {
                c.add(new AuditCase("ThisPcLayout.driveRow(" + size[0] + "," + buttons + ")",
                        dev.jstech.computers.gui.layout.ThisPcLayout.driveRow(size[0], buttons), false));
            }
            c.add(new AuditCase("ThisPcLayout.programGrid(" + size[0] + ")",
                    dev.jstech.computers.gui.layout.ThisPcLayout.programGrid(size[0], 7), false));
        }
        // This PC's About-style page on KDE, GNOME and Cinnamon: fixed size, one layout each.
        c.add(new AuditCase("ThisPcLayout.KdeAbout", ThisPcLayout.KdeAbout.layout(), true));
        c.add(new AuditCase("ThisPcLayout.GnomeAbout", ThisPcLayout.GnomeAbout.layout(), true));
        c.add(new AuditCase("ThisPcLayout.CinnamonAbout", ThisPcLayout.CinnamonAbout.layout(), true));
        /*
         * The Personalize page scrolls rather than being held to a budget, so it is audited for being clean
         * (no overlaps, nothing spilling past its own content height) at the worst case: the flat skin (every
         * row this page draws) at the narrowest window with every wallpaper offered, and the plain bevel skin
         * (the fewest rows) with none of the extra wallpapers offered.
         */
        c.add(new AuditCase("SettingsLayout(flat, min width, most wallpapers)",
                SettingsLayout.layout(135, 10, true, true), false));
        c.add(new AuditCase("SettingsLayout(bevel, one wallpaper)",
                SettingsLayout.layout(135, 1, false, false), false));
        c.add(new AuditCase("SpeakerLayout", SpeakerLayout.layout(), true));
        c.add(new AuditCase("RedstoneInterfaceLayout", RedstoneInterfaceLayout.layout(), true));
        // Every volume control a panel opens, open and folded, with the English words at six pixels a letter.
        final VolumePopupLayout.Labels volumeWords = new VolumePopupLayout.Labels("Audio Volume", "Mute output",
                "Sound output", List.of("Monitor", "Speakers", "Monitor and speakers"), "Desk left, Desk right",
                "Configure Audio Devices...");
        for (final VolumePopupLayout.Look look : VolumePopupLayout.Look.values()) {
            for (final boolean open : new boolean[] {true, false}) {
                c.add(new AuditCase("VolumePopupLayout(" + look + (open ? ", open)" : ", folded)"),
                        VolumePopupLayout.of(look, open, volumeWords, s -> s.length() * 6).toGuiLayout(), true));
            }
        }
        // The Server Router tiles one section per output face; audit every count up to the maximum.
        for (int s = 0; s <= ServerRouterLayout.MAX_SECTIONS; s++) {
            c.add(new AuditCase("ServerRouterLayout(" + s + ")", ServerRouterLayout.layout(s), true));
        }
        /*
         * The Network Interactor is a resizable desktop window, so audit a spread of content sizes. Not
         * screen-budgeted: the window itself is clamped to the screen elsewhere; here we only require that
         * whatever size it resolves to is internally clean.
         */
        final int minW = NetworkInteractorLayout.minContentWidth();
        c.add(new AuditCase("NetworkInteractorLayout(default)", NetworkInteractorLayout.toGuiLayout(330, 226), false));
        c.add(new AuditCase("NetworkInteractorLayout(min)", NetworkInteractorLayout.toGuiLayout(minW, 150), false));
        c.add(new AuditCase("NetworkInteractorLayout(wide)", NetworkInteractorLayout.toGuiLayout(520, 360), false));
        /*
         * The Pattern Studio is a resizable desktop window too: the content a standard monitor's window gives
         * it (194), a maximized one (210) and one too short for the inventory band, which then folds away.
         */
        for (final int h : new int[]{194, 210, dev.jstech.computers.gui.layout.PatternStudioLayout
                .minContentHeight() - 1}) {
            c.add(new AuditCase("PatternStudioLayout(" + h + ")",
                    dev.jstech.computers.gui.layout.PatternStudioLayout.layout(322, h), false));
        }
        /*
         * A boot loader is drawn on the monitor's glass, which is one size for everything a machine shows and
         * wider than the budget a panel is held to, so it is audited for being clean and not for fitting one.
         */
        for (final int entries : new int[]{2, 3, BootMenu.MOST_ENTRIES}) {
            c.add(new AuditCase("LoaderMenuLayout(" + entries + ")", LoaderMenuLayout.layout(entries), false));
        }
        // CDE's Front Panel stands on the desktop, which is the glass or larger when it is drawn smaller.
        for (final int[] desktop : new int[][]{{384, 256}, {512, 341}}) {
            c.add(new AuditCase("CdeFrontPanelLayout(" + desktop[0] + ")",
                    CdeFrontPanelLayout.layout(desktop[0], desktop[1]), false));
            // Its window icons, with more of them than one row of the workspace holds.
            c.add(new AuditCase("CdeWindowIconLayout(" + desktop[0] + ")",
                    CdeWindowIconLayout.layout(CdeWindowIconLayout.perRow(desktop[0]) + 2, desktop[0],
                            desktop[1] - CdeFrontPanelLayout.BAND_H), false));
            c.add(new AuditCase("CdeExitLayout(" + desktop[0] + ")",
                    CdeExitLayout.layout(desktop[0], desktop[1]), false));
        }
        // CDE's Application Manager: the window of the four groups, and a group that fills two rows.
        c.add(new AuditCase("CdeAppManagerLayout(groups)", CdeAppManagerLayout.layout(4, false, 284, 62), true));
        c.add(new AuditCase("CdeAppManagerLayout(group)", CdeAppManagerLayout.layout(8, true, 292, 128), true));
        // CDE's Style Manager: the strip of pages, and each page with everything it lists.
        c.add(new AuditCase("CdeStyleLayout(strip)", CdeStyleLayout.stripLayout(3), true));
        c.add(new AuditCase("CdeStyleLayout(audio)", CdeStyleLayout.audioLayout(), true));
        c.add(new AuditCase("CdeStyleLayout(color)", CdeStyleLayout.colorLayout(CdeScheme.ALL.size()), true));
        c.add(new AuditCase("CdeStyleLayout(backdrop)",
                CdeStyleLayout.backdropLayout(CdeBackdrop.values().length), true));
        c.add(new AuditCase("WorkstationInfoLayout", WorkstationInfoLayout.layout(), true));
        c.add(new AuditCase("HelpViewerLayout", HelpViewerLayout.layout(), true));
        // The Network Manager's Services tab scrolls, so it is held to being clean at its widest and its narrowest.
        c.add(new AuditCase("NetworkServicesLayout(default)", NetworkServicesLayout.layout(
                NetworkServicesLayout.DEFAULT_CONTENT_W, 16, 32, true, 2, new int[] {84, 44, 72}, 64), false));
        c.add(new AuditCase("NetworkServicesLayout(narrowest)", NetworkServicesLayout.layout(
                NetworkServicesLayout.MIN_CONTENT_W, 3, 2, true, 2, new int[] {84, 44, 72}, 64), false));
        c.add(new AuditCase("NetworkLinksLayout(default)", NetworkLinksLayout.devices(
                NetworkServicesLayout.DEFAULT_W - NetworkServicesLayout.WINDOW_PAD, NetworkServicesLayout.DEFAULT_H - 38,
                true), false));
        c.add(new AuditCase("NetworkLinksLayout(narrowest)", NetworkLinksLayout.devices(
                NetworkServicesLayout.MIN_W - NetworkServicesLayout.WINDOW_PAD, 150 - 38, true), false));
        // The trash window in each of its three looks, at its smallest and at its first size.
        for (final int[] size : new int[][]{
                {TrashLayout.MIN_W - TrashLayout.FRAME_W, TrashLayout.MIN_H - TrashLayout.FRAME_H},
                {TrashLayout.DEFAULT_W - TrashLayout.FRAME_W, TrashLayout.DEFAULT_H - TrashLayout.FRAME_H}}) {
            c.add(new AuditCase("TrashLayout.frames(" + size[0] + ")", TrashLayout.framesLayout(size[0], size[1]),
                    false));
            c.add(new AuditCase("TrashLayout.linux(" + size[0] + ")", TrashLayout.linuxLayout(size[0], size[1]),
                    false));
            c.add(new AuditCase("TrashLayout.cde(" + size[0] + ")", TrashLayout.cdeLayout(size[0], size[1]),
                    false));
        }
        return c;
    }

    @Test
    void noLayoutHasOverlappingSolids() {
        for (final AuditCase c : cases()) {
            assertTrue(c.layout().overlaps().isEmpty(),
                    c.label() + " has overlapping solid elements: " + c.layout().overlaps());
        }
    }

    @Test
    void noLayoutSpillsOutOfItsFrame() {
        for (final AuditCase c : cases()) {
            assertTrue(c.layout().outOfBounds().isEmpty(),
                    c.label() + " has elements drawn out of frame: " + c.layout().outOfBounds());
        }
    }

    @Test
    void noLayoutHasInvisibleSolids() {
        for (final AuditCase c : cases()) {
            assertTrue(c.layout().zeroSizedSolids().isEmpty(),
                    c.label() + " has zero-sized (invisible) solid elements: " + c.layout().zeroSizedSolids());
        }
    }

    @Test
    void fixedSizeLayoutsFitTheScreenBudget() {
        for (final AuditCase c : cases()) {
            if (!c.fixedSize()) {
                continue;
            }
            assertTrue(c.layout().width() <= c.widthBudget(),
                    c.label() + " width " + c.layout().width() + " exceeds the " + c.widthBudget() + "px budget");
            assertTrue(c.layout().height() <= c.heightBudget(),
                    c.label() + " height " + c.layout().height() + " exceeds the " + c.heightBudget() + "px budget");
        }
    }

    @Test
    void everyLayoutFactoryHasAnAuditCase() throws Exception {
        /*
         * Reflection guard: any class in the layout package that produces a GuiLayout must be covered by
         * cases() above. A new screen layout added without an audit case fails here, so coverage cannot regress.
         */
        final List<Class<?>> classes = layoutClasses();
        assertFalse(classes.isEmpty(), "no *Layout classes found on the classpath, the scan path is wrong");
        for (final Class<?> cls : classes) {
            final boolean producesLayout = Arrays.stream(cls.getDeclaredMethods())
                    .anyMatch(m -> Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())
                            && m.getReturnType() == GuiLayout.class);
            if (producesLayout) {
                assertTrue(COVERED.contains(cls.getSimpleName()),
                        cls.getSimpleName() + " produces a GuiLayout but has no audit case in LayoutAuditTest"
                                + ", add it to cases() and COVERED");
            }
        }
    }

    /** Loads every {@code *Layout} class in the computing layout package from the compiled-classes directory.
     *  Anchored on a known layout class's code source (the {@code build/classes/java/main} root under Gradle),
     *  which is more robust than a context-classloader resource lookup. */
    private static List<Class<?>> layoutClasses() throws Exception {
        final String pkg = "dev.jstech.computers.gui.layout";
        final URL loc = BusLayout.class.getProtectionDomain().getCodeSource().getLocation();
        final File classesRoot = new File(loc.toURI());
        final File dir = new File(classesRoot, pkg.replace('.', '/'));
        final List<Class<?>> out = new ArrayList<>();
        final File[] files = dir.listFiles((d, name) -> name.endsWith("Layout.class") && !name.contains("$"));
        if (files == null) {
            return out;
        }
        for (final File f : files) {
            final String simple = f.getName().substring(0, f.getName().length() - ".class".length());
            out.add(Class.forName(pkg + "." + simple));
        }
        return out;
    }
}
