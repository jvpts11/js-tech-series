/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Control;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.operation.payload.DiskFilesPayload;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The subpanels of CDE's Front Panel: what slides up out of a control when the arrow at its head is pressed.
 * Files keeps the places a File Manager is opened at, the Text Editor keeps the personal applications beside
 * it, and Applications keeps the Application Manager and the tools that tell how the workstation is doing.
 *
 * <p>The Files subpanel also lists each medium that is in one of the machine's drives, as the machine said when
 * the subpanel came up.
 *
 * <p>A subpanel is headed by what it is, stands right above the control it belongs to, and stays up until its
 * arrow is pressed again: choosing something on it starts that and leaves the subpanel where it is, which is
 * what made it a place to keep things and not a menu. Only one is up at a time. It is drawn in the same relief
 * as the panel it rises from, so it reads as a piece of that panel pulled upward.
 */
final class CdeLaunchers {

    /** One line of a subpanel: what it says, whose picture it wears, and what choosing it does. */
    private record Row(String label, @Nullable ResourceLocation icon, Runnable action) {
    }

    private final DesktopScreen desktop;

    /** The control whose subpanel is up, or null while none is. */
    @Nullable
    private Control open;

    /** Wide enough for its longest heading and for "Application Manager" beside a picture. */
    private static final int WIDTH = 142;
    private static final int LABEL_CHARS = 20;
    private static final int HEAD_H = 13;
    private static final int ROW_H = 16;
    private static final int PAD = 3;

    /** The Application Manager's picture, which is CDE's own and no program's. */
    private static final ResourceLocation APPLICATION_MANAGER =
            ResourceLocation.fromNamespaceAndPath("jsc", "application_manager");

    CdeLaunchers(final DesktopScreen desktop) {
        this.desktop = desktop;
    }

    /** Whether that control has anything behind it, which is what earns it an arrow. */
    static boolean hasSubpanel(final Control control) {
        return control == Control.FILES || control == Control.EDITOR || control == Control.APPLICATIONS;
    }

    boolean isOpen() {
        return this.open != null;
    }

    boolean isOpen(final Control control) {
        return this.open == control;
    }

    /** The arrow of that control was pressed: its subpanel comes up, or goes down when it was the one up. */
    void toggle(final Control control) {
        this.open = this.open == control || !hasSubpanel(control) ? null : control;
        // What is in the drives is asked as the Files subpanel comes up, since a disc may have gone in since.
        if (this.open == Control.FILES) {
            desktop.askForMedia();
        }
    }

    void close() {
        this.open = null;
    }

    /** What the subpanel that is up lists, top to bottom, for a test to read. */
    List<String> labels() {
        final List<String> out = new ArrayList<>();
        for (final Row row : rows()) {
            out.add(row.label());
        }
        return out;
    }

    /** The middle of the line so labelled on the subpanel that is up, in desktop pixels, or null. */
    @Nullable
    int[] rowCentre(final String label, final int sw, final int sh) {
        final List<Row> rows = rows();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).label().equals(label)) {
                final Rect box = box(sw, sh, rows.size());
                return new int[] {box.x() + box.w() / 2, box.y() + PAD + HEAD_H + 1 + i * ROW_H + ROW_H / 2};
            }
        }
        return null;
    }

    void render(final GuiGraphics g, final int sw, final int sh, final CdePalette p) {
        if (this.open == null) {
            return;
        }
        final List<Row> rows = rows();
        final Rect box = box(sw, sh, rows.size());
        final String heading = heading(this.open);
        MotifChrome.raised(g, box.x(), box.y(), box.w(), box.h(), p.window(), p);
        g.fill(box.x() + PAD, box.y() + PAD, box.x() + box.w() - PAD, box.y() + PAD + HEAD_H, p.active());
        g.drawString(desktop.textFont(), heading,
                box.x() + (box.w() - desktop.textFont().width(heading)) / 2, box.y() + PAD + 3, p.activeInk(), false);
        int y = box.y() + PAD + HEAD_H + 1;
        for (final Row row : rows) {
            if (desktop.hoverIn(box.x() + PAD, y, box.w() - PAD * 2, ROW_H)) {
                MotifChrome.sunken(g, box.x() + PAD, y, box.w() - PAD * 2, ROW_H, p.inset(), p);
            }
            if (row.icon() != null) {
                ProgramIcons.draw(g, box.x() + PAD + 2, y + 1, 14, 14, row.icon(), desktop.icons());
            }
            g.drawString(desktop.textFont(), desktop.shorten(row.label(), LABEL_CHARS), box.x() + PAD + 20, y + 4,
                    p.ink(), false);
            y += ROW_H;
        }
    }

    /**
     * A click while a subpanel is up. A line starts what it names and the subpanel stays where it is.
     *
     * @return whether the click landed on the subpanel at all, so nothing under it reacts
     */
    boolean click(final double mx, final double my, final int sw, final int sh) {
        if (this.open == null) {
            return false;
        }
        final List<Row> rows = rows();
        final Rect box = box(sw, sh, rows.size());
        if (!box.holds(mx, my)) {
            return false;
        }
        final int first = box.y() + PAD + HEAD_H + 1;
        final int row = (int) Math.floor((my - first) / ROW_H);
        if (my >= first && row >= 0 && row < rows.size()) {
            rows.get(row).action().run();
        }
        return true;
    }

    private static String heading(final Control control) {
        return switch (control) {
            case FILES -> "Files";
            case EDITOR -> "Personal Applications";
            default -> "Applications";
        };
    }

    /** What the subpanel that is up lists; a program this machine does not have is left out. */
    private List<Row> rows() {
        final List<Row> out = new ArrayList<>();
        if (this.open == Control.FILES) {
            final ResourceLocation files = iconOf("files");
            out.add(new Row("Home", files, () -> desktop.openFolder(desktop.homeDir())));
            out.add(new Row("Desktop", files, () -> desktop.openFolder(desktop.desktopDirectory())));
            for (final DiskFilesPayload.WireVolume medium : desktop.media()) {
                out.add(new Row(medium.label(), files, () -> desktop.openFolder(medium.key())));
            }
        } else if (this.open == Control.EDITOR) {
            program(out, "editor");
            program(out, "command_prompt");
            program(out, "calculator");
        } else if (this.open == Control.APPLICATIONS) {
            out.add(new Row(ApplicationManagerApp.KEY, APPLICATION_MANAGER,
                    () -> desktop.openApplicationManager(null)));
            program(out, "system_monitor");
            program(out, "workstation_info");
        }
        return out;
    }

    /** Adds the program whose id has that path, under the name this desktop gives it, when the machine has it. */
    private void program(final List<Row> out, final String path) {
        for (final DesktopScreen.Launcher launcher : desktop.launcherList()) {
            if (launcher.programId() != null && launcher.programId().getPath().equals(path)) {
                out.add(new Row(launcher.label(), launcher.programId(), () -> desktop.launch(launcher)));
                return;
            }
        }
    }

    @Nullable
    private ResourceLocation iconOf(final String path) {
        for (final DesktopScreen.Launcher launcher : desktop.launcherList()) {
            if (launcher.programId() != null && launcher.programId().getPath().equals(path)) {
                return launcher.programId();
            }
        }
        return null;
    }

    /** The subpanel's own rectangle: as tall as what it lists, standing on the head of its control. */
    private Rect box(final int sw, final int sh, final int rows) {
        final Rect control = CdeFrontPanelLayout.control(this.open == null ? Control.APPLICATIONS : this.open, sw, sh);
        final int h = PAD * 2 + HEAD_H + 1 + rows * ROW_H;
        final int x = Math.max(0, Math.min(control.x() + (control.w() - WIDTH) / 2, sw - WIDTH));
        return new Rect(x, control.y() - h - 2, WIDTH, h);
    }
}
