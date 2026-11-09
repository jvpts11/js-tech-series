/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.os.QuinceArt;
import dev.jstech.computers.client.os.QuinceBars;
import dev.jstech.computers.client.os.QuinceLook;
import dev.jstech.computers.client.os.SkinSprites;
import dev.jstech.computers.os.fs.TrashKind;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.computers.os.install.InstallerStyle;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Quince's installers (computers/docs/design/quince.md), which run on the system's own desktop under its menu bar, the
 * disks they read and write lying on the desktop beside them: the Quince System's Easy Install dialog, which asks for
 * each of its three floppies in turn, and the assistants of Quince OS 9 and Quince OS X, their steps listed down the
 * left of the window.
 *
 * <p>The frame and the page are both drawn here, since every page of these is a part of one window; the screen keeps
 * the buttons' hits and the keys.
 */
@TextHolder
final class InstallerQuince {

    private static final TextKey EASY_BODY = TextKey.of("jsc.install.quince.easy_body",
            "Click Install to place %s on the disk named \"%s\". It comes on three disks: %s, %s and %s.");
    private static final TextKey FREE_K = TextKey.of("jsc.install.quince.free_k", "%sK free");
    private static final TextKey SWITCH_DISK = TextKey.of("jsc.install.quince.switch_disk", "Switch Disk");
    private static final TextKey CUSTOMIZE = TextKey.of("jsc.install.quince.customize", "Customize");
    private static final TextKey QUIT = TextKey.of("jsc.install.quince.quit", "Quit");
    private static final TextKey INSTALL = TextKey.of("jsc.install.quince.install", "Install");
    private static final TextKey RESTART = TextKey.of("jsc.install.quince.restart", "Restart");
    private static final TextKey CANCEL = TextKey.of("jsc.install.quince.cancel", "Cancel");
    private static final TextKey OS_9_CD = TextKey.of("jsc.install.quince.os_9_cd", "OS 9 CD");
    private static final TextKey CONTINUE = TextKey.of("jsc.install.quince.continue", "Continue");
    private static final TextKey GO_BACK = TextKey.of("jsc.install.quince.go_back", "Go Back");
    private static final TextKey SELECT = TextKey.of("jsc.install.quince.select", "Select");
    private static final TextKey AGREE = TextKey.of("jsc.install.quince.agree", "Agree");
    private static final TextKey OPTIONS = TextKey.of("jsc.install.quince.options", "Options...");
    private static final TextKey DISK_OF = TextKey.of("jsc.install.quince.disk_of",
            "Disk %s of %s. The one before it has come out of the slot.");
    private static final TextKey NO_DISK = TextKey.of("jsc.install.quince.no_disk",
            "No disk here has room for %s.");
    private static final TextKey DESTINATION = TextKey.of("jsc.install.quince.destination", "Destination Disk:");
    private static final TextKey NEEDS = TextKey.of("jsc.install.quince.needs",
            "%s needs %s; %s has %s free. The system goes into its own System Folder.");
    private static final TextKey CHOOSE_VOLUME = TextKey.of("jsc.install.quince.choose_volume",
            "Select the volume where you want to install the %s software.");
    private static final TextKey AVAILABLE = TextKey.of("jsc.install.quince.available", "%s available");
    private static final TextKey REQUIRES = TextKey.of("jsc.install.quince.requires",
            "Installing this software requires %s of space.");
    private static final TextKey WELCOME_BODY = TextKey.of("jsc.install.quince.welcome_body",
            "This program installs %s on a disk of this computer. You will be guided through the steps it takes.");
    private static final TextKey READ_ME_BODY = TextKey.of("jsc.install.quince.read_me_body",
            "%s runs on Quince's own computers. Programs come on discs, or are dragged into Applications; from the"
                    + " Standard on, the Quince Store and keg bring them from the network's Mirror.");
    private static final TextKey LICENSE_BODY = TextKey.of("jsc.install.quince.license_body",
            "Quince Inc. lets you use %s on the Quince computers you own. Click Agree to go on.");
    private static final TextKey TYPE_BODY = TextKey.of("jsc.install.quince.type_body",
            "This will take %s of space on \"%s\". Click Install to perform a standard installation of %s.");
    private static final TextKey WRITING = TextKey.of("jsc.install.quince.writing", "Installing %s");
    private static final TextKey DONE_BODY = TextKey.of("jsc.install.quince.done_body",
            "%s is on \"%s\". Click Restart to start the computer with it.");

    /* The Quince System's dialog and its insert-the-next-disk alert, and the two assistants, as the mocks lay them. */
    private static final int[] EASY = {30, 30, 300, 170};
    private static final int[] INSERT_ALERT = {90, 100, 310, 192};
    private static final int[] ASSISTANT_9 = {16, 24, 318, 210};
    private static final int[] ASSISTANT_X = {14, 26, 370, 228};
    /* Where the desktop's icons stand, down its right side. */
    private static final int ICON_X = 352;
    private static final int ROW = 11;

    private InstallerQuince() {
    }

    /** The face the installer of that system is drawn in. */
    static QuinceLook lookOf(final InstallerFlow flow) {
        return switch (flow.style()) {
            case QUINCE_SYSTEM -> QuinceLook.ONE_BIT;
            case QUINCE_OS_9 -> QuinceLook.PLATINUM;
            default -> flow.systemId().endsWith(":quinceos") ? QuinceLook.QUINCE_OS : QuinceLook.PULP;
        };
    }

    /**
     * Paints the desktop, its bar, the disks on it and the installer's window, and answers where its buttons are. The
     * page has nothing left for the screen to draw: the content rectangle is empty.
     */
    static InstallerFrames.Frame paint(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                       final int ticksDone, final int sx, final int sy, final int sw, final int sh,
                                       final InstallerFrames.Held held) {
        final QuinceLook look = lookOf(flow);
        final QuinceLook.Colours c = look.colours();
        g.pose().pushPose();
        g.pose().translate(sx, sy, 0.0F);
        g.blit(wallpaper(look), 0, 0, 0.0F, 0.0F, sw, sh, sw, sh);
        final String set = iconSet(look);
        // The installer is the program in front: Quince OS 9 names it at the right of the bar, the later ones bold.
        switch (look) {
            case ONE_BIT -> QuinceBars.menuBar(g, font, look, 0, 0, sw, "", null, set, List.of("File", "Edit"), "",
                    -1);
            case PLATINUM -> QuinceBars.menuBar(g, font, look, 0, 0, sw, "Installer", program("generic"), set,
                    List.of("File", "Edit", "Help"), QuinceBars.clock(), -1);
            default -> QuinceBars.menuBar(g, font, look, 0, 0, sw, "Installer", null, set,
                    List.of("Edit", "Utilities", "Window", "Help"), QuinceBars.clock(), -1);
        }
        if (look.classic()) {
            desktopIcons(g, font, look, flow, set, sh);
        }
        final int[][] buttons = switch (flow.style()) {
            case QUINCE_SYSTEM -> easy(g, font, flow, ticksDone, held);
            case QUINCE_OS_9 -> assistant(g, font, look, flow, ticksDone, ASSISTANT_9, held);
            default -> assistant(g, font, look, flow, ticksDone, ASSISTANT_X, held);
        };
        g.pose().popPose();
        final InstallerFrames.Paint paint = new InstallerFrames.Paint(c.ink(), c.ink(), c.dim(), c.select(),
                c.select(), c.selectInk());
        return new InstallerFrames.Frame(sx, sy, 0, 0, paint, shift(buttons[0], sx, sy), shift(buttons[1], sx, sy),
                shift(buttons[2], sx, sy), shift(buttons[3], sx, sy));
    }

    /** The Quince System's Easy Install: one dialog that asks nothing but which disk, and its alert for the next one. */
    private static int[][] easy(final GuiGraphics g, final Font font, final InstallerFlow flow, final int ticksDone,
                                final InstallerFrames.Held held) {
        final QuinceLook look = QuinceLook.ONE_BIT;
        final QuinceLook.Colours c = look.colours();
        final int x0 = EASY[0];
        final int y0 = EASY[1];
        final int x1 = EASY[2];
        final int y1 = EASY[3];
        final InstallerPage page = flow.page();
        final boolean working = page == InstallerPage.COPY || page == InstallerPage.INSERT;
        QuinceArt.window(g, font, look, x0, y0, x1, y1, "Installer", page != InstallerPage.INSERT);
        int[] next = null;
        int[] erase = null;
        if (page == InstallerPage.WELCOME) {
            Draw.text(g, font, heading(flow), x0 + 12, y0 + 20, c.ink());
            g.fill(x0 + 8, y0 + 31, x1 - 8, y0 + 32, c.frame());
            final InstallerFlow.Disk disk = flow.target();
            final List<Text> disks = flow.style().disks();
            final String body = GameText.resolve(EASY_BODY.with(flow.systemName(),
                    disk == null ? "" : disk.label(), disks.get(0), disks.get(1), disks.get(2)));
            int ty = y0 + 38;
            for (final String line : wrap(font, body, 250)) {
                Draw.text(g, font, line, x0 + 12, ty, c.ink());
                ty += ROW;
            }
            g.fill(x0 + 12, y0 + 76, x1 - 12, y0 + 108, c.face());
            Draw.outline(g, x0 + 12, y0 + 76, x1 - x0 - 24, 32, c.frame());
            if (disk != null && flow.roomOn(disk)) {
                SkinSprites.draw(g, SkinSprites.find("device", "disk", "disk", iconSet(look)), x0 + 20, y0 + 84,
                        16, 16, 16);
                Draw.text(g, font, disk.label(), x0 + 42, y0 + 82, c.ink());
                Draw.text(g, font, GameText.resolve(FREE_K.with(thousands(flow.freeOn(disk) * 1024L))),
                        x0 + 42, y0 + 94, c.ink());
            } else {
                Draw.text(g, font, GameText.resolve(NO_DISK.with(flow.systemName())), x0 + 20, y0 + 88, c.ink());
            }
            final String switchDisk = GameText.resolve(SWITCH_DISK);
            erase = button(g, font, look, x1 - 20 - font.width(switchDisk) - 18, y0 + 85, 0, switchDisk, false,
                    held == InstallerFrames.Held.ERASE, false);
            button(g, font, look, x0 + 16, y1 - 22, 0, GameText.resolve(CUSTOMIZE), false, false, true);
            next = button(g, font, look, x1 - 72, y1 - 22, 56, GameText.resolve(INSTALL), true,
                    held == InstallerFrames.Held.NEXT, !flow.canContinue());
        } else if (working) {
            Draw.text(g, font, GameText.resolve(WRITING.with(flow.systemName())), x0 + 12, y0 + 22, c.ink());
            final int step = flow.stepAt(ticksDone);
            if (step >= 0 && step < flow.steps().size()) {
                Draw.text(g, font, GameText.resolve(flow.steps().get(step).label()), x0 + 12, y0 + 36, c.ink());
            }
            Draw.outline(g, x0 + 12, y0 + 50, x1 - x0 - 24, 10, c.frame());
            final int fill = (x1 - x0 - 26) * flow.permille(ticksDone) / 1000;
            g.fill(x0 + 13, y0 + 51, x0 + 13 + fill, y0 + 59, c.frame());
        } else {
            Draw.text(g, font, heading(flow), x0 + 12, y0 + 22, c.ink());
            final InstallerFlow.Disk disk = flow.target();
            int ty = y0 + 40;
            for (final String line : wrap(font, GameText.resolve(DONE_BODY.with(flow.systemName(),
                    disk == null ? "" : disk.label())), 250)) {
                Draw.text(g, font, line, x0 + 12, ty, c.ink());
                ty += ROW;
            }
            next = button(g, font, look, x1 - 72, y1 - 22, 56, GameText.resolve(RESTART), true,
                    held == InstallerFrames.Held.NEXT, false);
        }
        int[] cancel = null;
        if (page == InstallerPage.INSERT) {
            cancel = insertAlert(g, font, flow, held);
        }
        return new int[][] {next, null, cancel, erase};
    }

    /** The alert asking for the next floppy, the last one having come out of the slot. */
    private static int[] insertAlert(final GuiGraphics g, final Font font, final InstallerFlow flow,
                                     final InstallerFrames.Held held) {
        final QuinceLook look = QuinceLook.ONE_BIT;
        final QuinceLook.Colours c = look.colours();
        final int d0 = INSERT_ALERT[0];
        final int e0 = INSERT_ALERT[1];
        final int d1 = INSERT_ALERT[2];
        final int e1 = INSERT_ALERT[3];
        QuinceArt.dialog(g, look, d0, e0, d1, e1);
        SkinSprites.draw(g, SkinSprites.find("device", "floppy_drive", "disk", iconSet(look)), d0 + 14, e0 + 14,
                16, 16, 16);
        Draw.text(g, font, heading(flow), d0 + 42, e0 + 14, c.ink());
        final List<Text> disks = flow.style().disks();
        final int wanted = flow.disk();
        if (wanted >= 1 && wanted <= disks.size()) {
            Draw.text(g, font, GameText.resolve(disks.get(wanted - 1)), d0 + 42, e0 + 27, c.ink());
        }
        int ty = e0 + 44;
        for (final String line : wrap(font, GameText.resolve(DISK_OF.with(wanted, disks.size())), d1 - d0 - 28)) {
            Draw.text(g, font, line, d0 + 14, ty, c.ink());
            ty += ROW;
        }
        return button(g, font, look, d1 - 66, e1 - 22, 50, GameText.resolve(CANCEL), false,
                held == InstallerFrames.Held.CANCEL, false);
    }

    /**
     * An assistant: the window, the steps down its left with the one it is on picked out, the page on its right, and
     * Go Back with the button that goes on at its foot.
     */
    private static int[][] assistant(final GuiGraphics g, final Font font, final QuinceLook look,
                                     final InstallerFlow flow, final int ticksDone, final int[] box,
                                     final InstallerFrames.Held held) {
        final QuinceLook.Colours c = look.colours();
        final int x0 = box[0];
        final int y0 = box[1];
        final int x1 = box[2];
        final int y1 = box[3];
        final InstallerPage page = flow.page();
        final int top = QuinceArt.window(g, font, look, x0, y0, x1, y1,
                GameText.resolve(flow.style().title(flow.systemName())), true);
        final boolean platinum = look == QuinceLook.PLATINUM;
        if (platinum) {
            QuinceArt.bevel(g, x0 + 8, top + 6, x0 + 118, y1 - 34, c.track(), c.shade(), c.light());
        } else {
            g.fill(x0 + 1, top, x1 - 1, y1 - 1, c.face());
        }
        final List<InstallerPage> listed = listed(flow);
        final int at = listed.indexOf(page == InstallerPage.DONE && platinum ? InstallerPage.COPY : page);
        for (int k = 0; k < listed.size(); k++) {
            final int y = top + 12 + k * 14;
            final String label = GameText.resolve(flow.style().sideLabel(listed.get(k)));
            if (platinum) {
                final String shown = font.width(label) < 100 ? label : InstallerFrames.clip(font, label, 100);
                if (k == at) {
                    g.fill(x0 + 10, y - 2, x0 + 116, y + 10, c.select());
                }
                Draw.text(g, font, shown, x0 + 14, y, k == at ? c.selectInk() : k < at ? c.ink() : c.dim());
            } else {
                QuinceArt.disc(g, x0 + 14, y + 3, 3, k == at ? c.select() : k < at ? c.dim() : c.track(), 0);
                final Component text = k == at ? Component.literal(label).withStyle(ChatFormatting.BOLD)
                        : Component.literal(label);
                Draw.text(g, font, text, x0 + 22, y, k <= at ? c.ink() : c.dim());
            }
        }
        if (!platinum) {
            g.pose().pushPose();
            g.pose().translate(x0 + 30, y1 - 52, 0.0F);
            g.pose().scale(3.0F, 3.0F, 1.0F);
            QuinceArt.fruit(g, look, 0, 0, false);
            g.pose().popPose();
        }
        final int px0 = x0 + 128;
        final int pw = x1 - 8 - px0;
        if (!platinum) {
            g.fill(px0, top + 6, x1 - 8, y1 - 26, c.light());
            Draw.outline(g, px0, top + 6, x1 - 8 - px0, y1 - 26 - top - 6, c.shade());
        }
        final int tx = platinum ? px0 : px0 + 8;
        final int textW = platinum ? pw : pw - 16;
        final Component title = platinum ? Component.literal(heading(flow))
                : Component.literal(heading(flow)).withStyle(ChatFormatting.BOLD);
        Draw.text(g, font, title, tx, top + (platinum ? 8 : 12), c.ink());
        int ty = top + 26;
        final InstallerFlow.Disk disk = flow.target();
        switch (page) {
            case WELCOME -> ty = paragraph(g, font, WELCOME_BODY.with(flow.systemName()), tx, ty, textW, c.ink());
            case READ_ME -> ty = paragraph(g, font, READ_ME_BODY.with(flow.systemName()), tx, ty, textW, c.ink());
            case LICENSE -> ty = paragraph(g, font, LICENSE_BODY.with(flow.systemName()), tx, ty, textW, c.ink());
            case DISK -> ty = destination(g, font, look, flow, tx, ty, textW, y1);
            case COMPONENTS -> ty = paragraph(g, font, TYPE_BODY.with(size(look, flow.footprintMb()),
                    disk == null ? "" : disk.label(), flow.systemName()), tx, ty, textW, c.ink());
            case COPY -> {
                final int step = flow.stepAt(ticksDone);
                if (step >= 0 && step < flow.steps().size()) {
                    Draw.text(g, font, GameText.resolve(flow.steps().get(step).label()), tx, ty, c.ink());
                }
                final int bar = ty + 16;
                QuinceArt.bevel(g, tx, bar, tx + textW, bar + 10, c.track(), c.shade(), c.light());
                g.fill(tx + 1, bar + 1, tx + 1 + (textW - 2) * flow.permille(ticksDone) / 1000, bar + 9, c.select());
            }
            default -> ty = paragraph(g, font, DONE_BODY.with(flow.systemName(), disk == null ? "" : disk.label()),
                    tx, ty, textW, c.ink());
        }
        final boolean working = page == InstallerPage.COPY;
        final int by = y1 - (platinum ? 26 : 20);
        final int bh = platinum ? 14 : 12;
        int[] next = null;
        int[] back = null;
        if (!working) {
            final TextKey goes = switch (page) {
                case DISK -> platinum ? SELECT : CONTINUE;
                case LICENSE -> AGREE;
                case COMPONENTS -> INSTALL;
                case DONE -> RESTART;
                default -> CONTINUE;
            };
            next = button(g, font, look, x1 - (platinum ? 74 : 72), by, platinum ? 58 : 62, GameText.resolve(goes),
                    true, held == InstallerFrames.Held.NEXT, !flow.canContinue() && page != InstallerPage.DONE, bh);
            if (page != InstallerPage.WELCOME && page != InstallerPage.DONE) {
                final String goBack = GameText.resolve(GO_BACK);
                back = button(g, font, look, x1 - (platinum ? 82 + font.width(goBack) + 18 : 140), by,
                        platinum ? 0 : 62, goBack, false, held == InstallerFrames.Held.BACK, false, bh);
            }
            if (platinum) {
                button(g, font, look, x0 + 10, by, 0, GameText.resolve(OPTIONS), false, false, true, bh);
            }
        }
        return new int[][] {next, back, null, page == InstallerPage.DISK ? diskSwitch(look, flow, box) : null};
    }

    /** The destination page: the disk it goes on, what it needs, and what is free there. */
    private static int destination(final GuiGraphics g, final Font font, final QuinceLook look,
                                   final InstallerFlow flow, final int tx, final int ty, final int textW,
                                   final int y1) {
        final QuinceLook.Colours c = look.colours();
        final InstallerFlow.Disk disk = flow.target();
        if (look == QuinceLook.PLATINUM) {
            Draw.text(g, font, GameText.resolve(DESTINATION), tx, ty, c.ink());
            QuinceArt.bevel(g, tx + 82, ty - 4, tx + 166, ty + 10, c.track(), c.light(), c.shade());
            if (disk != null) {
                SkinSprites.draw(g, SkinSprites.find("device", "disk", "disk", iconSet(look)), tx + 85, ty - 4,
                        12, 12, 16);
                Draw.text(g, font, InstallerFrames.clip(font, disk.label(), 64), tx + 101, ty, c.ink());
            }
            final String free = disk == null ? "0 MB" : size(look, flow.freeOn(disk));
            return paragraph(g, font, NEEDS.with(flow.systemName(), size(look, flow.footprintMb()),
                    disk == null ? "" : disk.label(), free), tx, ty + 20, textW, c.ink());
        }
        int y = paragraph(g, font, CHOOSE_VOLUME.with(flow.systemName()), tx, ty, textW, c.ink());
        final List<InstallerFlow.Disk> disks = flow.disks();
        for (int k = 0; k < Math.min(2, disks.size()); k++) {
            final InstallerFlow.Disk one = disks.get(k);
            final int cx = tx + 42 + k * 90;
            final boolean chosen = disk != null && one.slot() == disk.slot();
            if (chosen) {
                QuinceArt.roundRect(g, cx - 26, y + 4, cx + 26, y + 48, 4, c.thumb());
            }
            g.pose().pushPose();
            g.pose().translate(cx - 16, y + 8, 0.0F);
            g.pose().scale(2.0F, 2.0F, 1.0F);
            SkinSprites.draw(g, SkinSprites.find("device", "disk", "disk", iconSet(look)), 0, 0, 16, 16, 16);
            g.pose().popPose();
            Draw.textCentered(g, font, InstallerFrames.clip(font, one.label(), 80), cx, y + 32, c.ink());
            Draw.textCentered(g, font, GameText.resolve(AVAILABLE.with(size(look, flow.freeOn(one)))), cx, y + 52,
                    c.dim());
        }
        return paragraph(g, font, REQUIRES.with(size(look, flow.footprintMb())), tx, y1 - 50, textW, c.ink());
    }

    /**
     * Where a click chooses another disk on the destination page, as {@code {x, y, w, h}} on the glass: the
     * Destination Disk menu of Quince OS 9, or the row of volumes of Quince OS X.
     */
    @Nullable
    private static int[] diskSwitch(final QuinceLook look, final InstallerFlow flow, final int[] box) {
        if (flow.disks().size() < 2) {
            return null;
        }
        return look == QuinceLook.PLATINUM ? new int[] {box[0] + 128 + 82, box[1] + 14 + 22, 84, 14}
                : new int[] {box[0] + 128 + 8 + 16, box[1] + 14 + 40, 180, 48};
    }

    /** The disks and the Trash down the desktop's right side, the medium being read at the top. */
    private static void desktopIcons(final GuiGraphics g, final Font font, final QuinceLook look,
                                     final InstallerFlow flow, final String set, final int sh) {
        final boolean floppy = flow.style() == InstallerStyle.QUINCE_SYSTEM;
        final InstallerPage page = flow.page();
        if (page != InstallerPage.INSERT && page != InstallerPage.DONE) {
            final String medium = floppy ? GameText.resolve(flow.style().disks().get(Math.max(0,
                    Math.min(flow.disk(), flow.style().disks().size()) - 1))) : GameText.resolve(OS_9_CD);
            icon(g, font, look, SkinSprites.find("device", floppy ? "floppy_drive" : "cd_drive", "disk", set),
                    20, medium);
        }
        final InstallerFlow.Disk disk = flow.target();
        icon(g, font, look, SkinSprites.find("device", "disk", "disk", set), 62, disk == null ? "" : disk.label());
        icon(g, font, look, SkinSprites.find("program", "trash", "generic", set), sh - 42,
                GameText.resolve(TrashKind.FREEDESKTOP.titleText()));
    }

    /** One icon on the desktop, its name under it on a white ground as the classic systems wrote it. */
    private static void icon(final GuiGraphics g, final Font font, final QuinceLook look,
                             final ResourceLocation texture, final int y, final String label) {
        SkinSprites.draw(g, texture, ICON_X - 8, y, 16, 16, 16);
        final int w = font.width(label);
        final int ly = y + 18;
        g.fill(ICON_X - w / 2 - 1, ly - 1, ICON_X + w / 2 + 2, ly + 9, look.colours().light());
        Draw.text(g, font, label, ICON_X - w / 2, ly, look.colours().ink());
    }

    /**
     * A button of the face, as wide as its label needs when {@code w} is nought, and answers where it is; one that
     * cannot be pressed is drawn dimmed and answers nothing.
     */
    @Nullable
    private static int[] button(final GuiGraphics g, final Font font, final QuinceLook look, final int x,
                                final int y, final int w, final String label, final boolean primary,
                                final boolean pressed, final boolean dim) {
        return button(g, font, look, x, y, w, label, primary, pressed, dim, 13);
    }

    @Nullable
    private static int[] button(final GuiGraphics g, final Font font, final QuinceLook look, final int x,
                                final int y, final int w, final String label, final boolean primary,
                                final boolean pressed, final boolean dim, final int h) {
        final int width = w > 0 ? w : font.width(label) + 18;
        QuinceArt.button(g, look, x, y, width, h, primary && !dim, pressed);
        final int ink = QuinceArt.buttonInk(look, primary && !dim);
        final int lx = x + (width - font.width(label)) / 2;
        if (dim) {
            QuinceArt.dimmed(g, font, label, lx, y + 3, ink, look.colours().button());
            return null;
        }
        Draw.text(g, font, label, lx, y + (h - 8) / 2 + 1, ink);
        return new int[] {x, y, width, h};
    }

    /** The pages an assistant lists down its side, in order: every page of its own but the last, which it ends on. */
    private static List<InstallerPage> listed(final InstallerFlow flow) {
        final List<InstallerPage> out = new ArrayList<>();
        for (final InstallerStyle.Stage stage : flow.style().stages()) {
            if (!flow.style().sideLabel(stage.page()).isEmpty() && !out.contains(stage.page())
                    && !(flow.style() == InstallerStyle.QUINCE_OS_9 && stage.page() == InstallerPage.DONE)) {
                out.add(stage.page());
            }
        }
        return out;
    }

    private static String heading(final InstallerFlow flow) {
        return GameText.resolve(flow.style().heading(flow.page(), flow.systemName()));
    }

    private static int paragraph(final GuiGraphics g, final Font font, final Text text, final int x, final int y,
                                 final int width, final int ink) {
        int ty = y;
        for (final String line : wrap(font, GameText.resolve(text), width)) {
            Draw.text(g, font, line, x, ty, ink);
            ty += ROW;
        }
        return ty;
    }

    /** A size as the face writes it: K on the classic systems, MB and GB with one decimal after them. */
    private static String size(final QuinceLook look, final long mb) {
        if (look == QuinceLook.ONE_BIT) {
            return thousands(mb * 1024L) + "K";
        }
        if (mb >= 1024) {
            return String.format(Locale.ROOT, "%.1f GB", mb / 1024.0);
        }
        return mb + " MB";
    }

    /** A number with its thousands marked by commas, as the classic systems printed sizes. */
    private static String thousands(final long value) {
        return String.format(Locale.ROOT, "%,d", value);
    }

    private static List<String> wrap(final Font font, final String text, final int width) {
        final List<String> lines = new ArrayList<>();
        String line = "";
        for (final String word : text.split(" ")) {
            final String probe = line.isEmpty() ? word : line + " " + word;
            if (font.width(probe) <= width || line.isEmpty()) {
                line = probe;
            } else {
                lines.add(line);
                line = word;
            }
        }
        if (!line.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    @Nullable
    private static int[] shift(@Nullable final int[] box, final int sx, final int sy) {
        return box == null ? null : new int[] {box[0] + sx, box[1] + sy, box[2], box[3]};
    }

    private static ResourceLocation wallpaper(final QuinceLook look) {
        final String id = switch (look) {
            case ONE_BIT -> "quince_system";
            case PLATINUM -> "quince_os_9";
            case PULP -> "quince_pulp";
            case FLAT -> "quince_flat";
            case QUINCE_OS -> "quinceos";
        };
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/wallpaper/" + id + ".png");
    }

    private static String iconSet(final QuinceLook look) {
        return switch (look) {
            case ONE_BIT -> "quince_system";
            case PLATINUM -> "quince_os_9";
            default -> "quince_os_x";
        };
    }

    private static ResourceLocation program(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
