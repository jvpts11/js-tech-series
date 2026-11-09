/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestSettingsPayload;
import dev.jstech.computers.operation.payload.SettingsSnapshotPayload;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * System Preferences, from Quince OS X on (computers/docs/design/quince.md): the panels of its real one in their four
 * rows, Personal, Hardware, Internet & Network and System. Memory and Mirror are panels of its own, shown in the
 * window; every other opens the page of this computer's settings it stands for.
 */
@TextHolder
public final class QuincePreferencesApp implements IDesktopApp {

    private final BlockPos host;
    private final QuinceLook look;
    /** The panel shown in the window instead of the grid: none, Memory or Mirror. */
    private Pane shown = Pane.ALL;
    @Nullable
    private SettingsSnapshotPayload data;
    /** The panels as last drawn, each {@code {x, y, w, h}}, beside the panel each is. */
    private final List<int[]> boxes = new ArrayList<>();
    private final List<Panel> panels = new ArrayList<>();
    private int[] showAll = new int[4];

    private static final TextKey SHOW_ALL = TextKey.of("jsc.quince.prefs.show_all", "Show All");
    private static final TextKey MEMORY_USED = TextKey.of("jsc.quince.prefs.memory_used", "%s MB of %s MB in use");
    private static final TextKey MIRROR_IS = TextKey.of("jsc.quince.prefs.mirror_is",
            "Programs come from the Mirror at mirror://mainframe.");
    private static final TextKey MIRROR_ON = TextKey.of("jsc.quince.prefs.mirror_on",
            "This computer is on a network, and asks its Mainframe's Mirror for programs.");
    private static final TextKey MIRROR_OFF = TextKey.of("jsc.quince.prefs.mirror_off",
            "This computer is on no network, so no Mirror answers it.");
    private static final TextKey PERSONAL = TextKey.of("jsc.quince.prefs.personal", "Personal");
    private static final TextKey HARDWARE = TextKey.of("jsc.quince.prefs.hardware", "Hardware");
    private static final TextKey NETWORKING = TextKey.of("jsc.quince.prefs.network", "Internet & Network");
    private static final TextKey SYSTEM = TextKey.of("jsc.quince.prefs.system", "System");
    private static final TextKey APPEARANCE = TextKey.of("jsc.quince.prefs.appearance", "Appearance");
    private static final TextKey DESKTOP = TextKey.of("jsc.quince.prefs.desktop", "Desktop");
    private static final TextKey DOCK = TextKey.of("jsc.quince.prefs.dock", "Dock");
    private static final TextKey LANGUAGE = TextKey.of("jsc.quince.prefs.language", "Language");
    private static final TextKey SECURITY = TextKey.of("jsc.quince.prefs.security", "Security");
    private static final TextKey DISPLAYS = TextKey.of("jsc.quince.prefs.displays", "Displays");
    private static final TextKey ENERGY = TextKey.of("jsc.quince.prefs.energy", "Energy");
    private static final TextKey SOUND = TextKey.of("jsc.quince.prefs.sound", "Sound");
    private static final TextKey NETWORK = TextKey.of("jsc.quince.prefs.network_panel", "Network");
    private static final TextKey SHARING = TextKey.of("jsc.quince.prefs.sharing", "Sharing");
    private static final TextKey MIRROR = TextKey.of("jsc.quince.prefs.mirror", "Mirror");
    private static final TextKey ACCOUNTS = TextKey.of("jsc.quince.prefs.accounts", "Accounts");
    private static final TextKey DATE_TIME = TextKey.of("jsc.quince.prefs.date_time", "Date & Time");
    private static final TextKey MEMORY = TextKey.of("jsc.quince.prefs.memory", "Memory");
    private static final TextKey STARTUP = TextKey.of("jsc.quince.prefs.startup", "Startup");
    private static final TextKey UPDATE = TextKey.of("jsc.quince.prefs.update", "Update");
    private static final List<Group> GROUPS = List.of(
            new Group(PERSONAL, List.of(new Panel(APPEARANCE, SettingsApp.PAGE_PERSONALIZE, Pane.ALL),
                    new Panel(DESKTOP, SettingsApp.PAGE_PERSONALIZE, Pane.ALL),
                    new Panel(DOCK, SettingsApp.PAGE_PERSONALIZE, Pane.ALL),
                    new Panel(LANGUAGE, SettingsApp.PAGE_SYSTEM, Pane.ALL),
                    new Panel(SECURITY, SettingsApp.PAGE_SYSTEM, Pane.ALL))),
            new Group(HARDWARE, List.of(new Panel(DISPLAYS, SettingsApp.PAGE_DISPLAY, Pane.ALL),
                    new Panel(ENERGY, SettingsApp.PAGE_SYSTEM, Pane.ALL),
                    new Panel(SOUND, SettingsApp.PAGE_SOUND, Pane.ALL))),
            new Group(NETWORKING, List.of(new Panel(NETWORK, SettingsApp.PAGE_NETWORK, Pane.ALL),
                    new Panel(SHARING, SettingsApp.PAGE_NETWORK, Pane.ALL),
                    new Panel(MIRROR, -1, Pane.MIRROR))),
            new Group(SYSTEM, List.of(new Panel(ACCOUNTS, SettingsApp.PAGE_SYSTEM, Pane.ALL),
                    new Panel(DATE_TIME, SettingsApp.PAGE_SYSTEM, Pane.ALL),
                    new Panel(MEMORY, -1, Pane.MEMORY),
                    new Panel(STARTUP, SettingsApp.PAGE_STORAGE, Pane.ALL),
                    new Panel(UPDATE, SettingsApp.PAGE_PROGRAMS, Pane.ALL))));
    private static final int CELL_W = 58;
    private static final int GROUP_H = 34;

    /** The open window that takes the machine's answers about itself, or null when none is up. */
    @Nullable
    private static QuincePreferencesApp active;

    QuincePreferencesApp(final BlockPos host, final ResourceLocation desktopId) {
        this.host = host;
        final QuinceLook face = QuinceLook.of(desktopId, null);
        this.look = face == null ? QuinceLook.FLAT : face;
        PacketDistributor.sendToServer(new RequestSettingsPayload(host));
        active = this;
    }

    /** Hands the open window the machine's answer about itself, for its Memory panel. */
    public static void accept(final SettingsSnapshotPayload payload) {
        if (active != null && active.host.equals(payload.hostPos())) {
            active.data = payload;
        }
    }

    @Override
    public String title() {
        return QuinceAboutApp.programName("settings");
    }

    @Override
    public int defaultWidth() {
        return 300;
    }

    @Override
    public int defaultHeight() {
        return 160;
    }

    @Override
    public void onClosed() {
        if (active == this) {
            active = null;
        }
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        final QuinceLook.Colours c = this.look.colours();
        final String all = GameText.resolve(SHOW_ALL);
        this.showAll = new int[] {x + 4, y + 2, font.width(all) + 12, 11};
        QuinceArt.button(g, this.look, this.showAll[0], this.showAll[1], this.showAll[2], 11, false, false);
        Draw.text(g, font, all, x + 10, y + 3, c.ink());
        g.fill(x, y + 15, x + width, y + 16, c.shade());
        this.boxes.clear();
        this.panels.clear();
        final int top = y + 18;
        switch (this.shown) {
            case MEMORY -> memory(g, font, x, top, width, c);
            case MIRROR -> mirror(g, font, x, top, width, c);
            default -> grid(g, font, x, top, width, c);
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (inside(this.showAll, mouseX, mouseY)) {
            this.shown = Pane.ALL;
            return;
        }
        for (int i = 0; i < this.boxes.size(); i++) {
            if (inside(this.boxes.get(i), mouseX, mouseY)) {
                final Panel panel = this.panels.get(i);
                if (panel.pane() != Pane.ALL) {
                    this.shown = panel.pane();
                    PacketDistributor.sendToServer(new RequestSettingsPayload(this.host));
                } else {
                    ActiveDesktop.openSettingsPage(panel.page());
                }
                return;
            }
        }
    }

    /** The panels in their rows, each a coloured tile over its name. */
    private void grid(final GuiGraphics g, final Font font, final int x, final int top, final int width,
                      final QuinceLook.Colours c) {
        int gy = top;
        for (final Group group : GROUPS) {
            Draw.text(g, font, Component.literal(GameText.resolve(group.title())).withStyle(ChatFormatting.BOLD),
                    x + 6, gy, c.ink());
            for (int i = 0; i < group.panels().size(); i++) {
                final Panel panel = group.panels().get(i);
                final int cx = x + 6 + i * CELL_W + CELL_W / 2;
                final int ty = gy + 10;
                QuinceArt.roundRect(g, cx - 6, ty, cx + 6, ty + 9, 2, c.select());
                g.fill(cx - 2, ty + 3, cx + 2, ty + 5, c.selectInk());
                final String name = GameText.resolve(panel.name());
                Draw.textCentered(g, font, name, cx, ty + 11, c.ink());
                this.boxes.add(new int[] {cx - CELL_W / 2, ty, CELL_W, 20});
                this.panels.add(panel);
            }
            gy += GROUP_H;
            g.fill(x + 4, gy - 4, x + width - 4, gy - 3, c.shade());
        }
    }

    /** The Memory panel: what the machine holds, and what holds it. */
    private void memory(final GuiGraphics g, final Font font, final int x, final int top, final int width,
                        final QuinceLook.Colours c) {
        final SettingsSnapshotPayload d = this.data;
        if (d == null) {
            return;
        }
        Draw.text(g, font, GameText.resolve(MEMORY_USED.with(d.ramUsedMb(), d.ramMb())), x + 8, top + 2, c.ink());
        final int barW = width - 16;
        g.fill(x + 8, top + 14, x + 8 + barW, top + 22, c.track());
        g.fill(x + 8, top + 14, x + 8 + barW * Math.min(d.ramUsedMb(), d.ramMb()) / Math.max(1, d.ramMb()), top + 22,
                c.select());
        int ry = top + 28;
        for (final SettingsSnapshotPayload.RamUse use : d.ramUses()) {
            if (ry > top + 120) {
                break;
            }
            final String size = use.mb() + " MB";
            Draw.text(g, font, use.label(), x + 8, ry, c.ink());
            Draw.text(g, font, size, x + width - 8 - font.width(size), ry, c.dim());
            ry += 10;
        }
    }

    /** The Mirror panel: where programs come from, and whether it answers. */
    private void mirror(final GuiGraphics g, final Font font, final int x, final int top, final int width,
                        final QuinceLook.Colours c) {
        Draw.text(g, font, GameText.resolve(MIRROR_IS), x + 8, top + 4, c.ink());
        final boolean on = DesktopState.hostNetworked(this.host);
        Draw.text(g, font, GameText.resolve(on ? MIRROR_ON : MIRROR_OFF), x + 8, top + 18, on ? c.ink() : c.dim());
    }

    private static boolean inside(final int[] box, final double mx, final double my) {
        return mx >= box[0] && mx < box[0] + box[2] && my >= box[1] && my < box[1] + box[3];
    }

    /** What the window shows: the grid of panels, or one of its own panels. */
    private enum Pane { ALL, MEMORY, MIRROR }

    /** One panel: its name, the settings page it opens, or the panel of the window's own it shows. */
    private record Panel(TextKey name, int page, Pane pane) {
    }

    /** One row of panels, under its title. */
    private record Group(TextKey title, List<Panel> panels) {
    }
}
