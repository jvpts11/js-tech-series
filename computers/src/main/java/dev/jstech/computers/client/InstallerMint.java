/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.client.monitor.MonitorPainter;
import dev.jstech.computers.client.os.OffscreenDesktop;
import dev.jstech.computers.client.os.WallpaperStyle;
import dev.jstech.computers.gui.layout.InstallerLayout;
import dev.jstech.computers.operation.payload.InstallerActionPayload;
import dev.jstech.computers.os.FileSystemType;
import dev.jstech.computers.os.disk.PartitionEntry;
import dev.jstech.computers.os.install.DiskSpot;
import dev.jstech.computers.os.install.InstallPlan;
import dev.jstech.computers.os.install.InstallerFlow;
import dev.jstech.computers.os.install.InstallerPage;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Linux Mint's installer: a window on the live desktop its medium started (computers/docs/design/operating-systems.md),
 * in five screens. The installation type, the divider beside another system, who you are while the files are copied,
 * the copy with its slideshow, and the end, with no language, keyboard or time zone to ask. Nothing is written until
 * the changes are agreed to in the dialog that lists them.
 */
@PaletteHolder
@TextHolder
final class InstallerMint implements IOwnInstallerPage {

    private final Consumer<InstallerActionPayload> send;
    private final BlockPos host;
    private final BlockPos monitor;
    private final IntSupplier memoryMb;
    /* Where each clickable thing was drawn last, {x, y, w, h}, with what clicking it does. */
    private final List<Hit> hits = new ArrayList<>();
    private InstallerFlow flow;
    /* On the type page: whether the choice is the disk beside another system rather than a whole disk. */
    private boolean alongside;
    /* The disk the whole-disk choice takes, by its place in the installer's list. */
    private int eraseIndex;
    /* Whether the dialog listing the changes is up, waiting for Continue or Go Back. */
    private boolean confirming;
    /* The computer's name being typed on the page that asks who you are. */
    private String typing = "";
    /* Whether the slideshow was skipped, leaving the plain copy on the window. */
    private boolean skipped;

    /** The window's chrome and page, as Cinnamon draws them on Mint. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "installer/mint",
            new Colours(0xFF2B2B2B, 0xFFF7F7F7, 0xFFB0B0B0, 0xFF69B03B, 0xFF6E6E6E, 0xFFFFFFFF, 0xFFFBFBFB,
                    0xFFD8D8D8, 0xFFC8C8C8, 0xFF3A7BD5, 0xFF555555, 0xFF2E3A23, 0xFF34462A, 0xFFD8E8C8,
                    0xFF2B2B2B));
    private static final ResourceLocation MARK =
            ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/splash/mint_mark.png");
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "textures/gui/program/live_installer/cinnamon.png");
    private static final int MARK_TEX = 192;

    /* The window, on the glass, as the mock places it; its title bar and the page inside it. */
    private static final int WIN_X = 52;
    private static final int WIN_Y = 16;
    private static final int WIN_W = 280;
    private static final int WIN_H = 210;
    private static final int TITLE_H = 16;
    private static final int PAD = 7;
    private static final int ROW = 7;
    /* A gigabyte, which is what the divider moves by. */
    private static final long MB_PER_GB = 1_024L;
    private static final int BUTTON_H = 11;
    private static final int DOTS = 7;
    private static final int CARET_TICKS = 10;

    private static final TextKey INSTALL = TextKey.of("jsc.installer.mint.install", "Install");
    private static final TextKey HAS_SYSTEM = TextKey.of("jsc.installer.mint.has_system",
            "This computer currently has %s on it. What would you like to do?");
    private static final TextKey NO_SYSTEM = TextKey.of("jsc.installer.mint.no_system",
            "This computer currently has no detected operating systems. What would you like to do?");
    private static final TextKey ALONGSIDE = TextKey.of("jsc.installer.mint.alongside", "Install %s alongside %s");
    private static final TextKey ALONGSIDE_SUB = TextKey.of("jsc.installer.mint.alongside_sub",
            "Documents, music, and other personal files will be kept. You can choose which operating system you"
                    + " want each time the computer starts up.");
    private static final TextKey ERASE = TextKey.of("jsc.installer.mint.erase", "Erase disk and install %s");
    private static final TextKey ERASE_SUB = TextKey.of("jsc.installer.mint.erase_sub",
            "Warning: This will delete all your programs, documents, photos, music, and any other files in all"
                    + " operating systems.");
    private static final TextKey DRIVE = TextKey.of("jsc.installer.mint.drive", "Select drive: < %s >");
    private static final TextKey DIVIDER = TextKey.of("jsc.installer.mint.divider",
            "Allocate drive space by dragging the divider below:");
    private static final TextKey SWAP = TextKey.of("jsc.installer.mint.swap",
            "A swap partition as big as this computer's memory (%s) goes beside %s.");
    private static final TextKey WRITE_TITLE = TextKey.of("jsc.installer.mint.write_title",
            "Write the changes to disks?");
    private static final TextKey WRITE_TEXT = TextKey.of("jsc.installer.mint.write_text",
            "If you continue, the changes listed below will be written to the disks. Otherwise, you will be able"
                    + " to make further changes manually.");
    private static final TextKey TABLES = TextKey.of("jsc.installer.mint.tables",
            "The partition tables of the following devices are changed:");
    private static final TextKey RESIZED = TextKey.of("jsc.installer.mint.resized",
            "The following partitions are going to be resized:");
    private static final TextKey RESIZE = TextKey.of("jsc.installer.mint.resize",
            "partition #%s of %s from %s to %s");
    private static final TextKey FORMATTED = TextKey.of("jsc.installer.mint.formatted",
            "The following partitions are going to be formatted:");
    private static final TextKey AS = TextKey.of("jsc.installer.mint.as", "partition #%s of %s as %s");
    private static final TextKey YOUR_NAME = TextKey.of("jsc.installer.mint.your_name", "Your name:");
    private static final TextKey COMPUTER_NAME = TextKey.of("jsc.installer.mint.computer_name",
            "Your computer's name:");
    private static final TextKey COMPUTER_NOTE = TextKey.of("jsc.installer.mint.computer_note",
            "The name it uses when it talks to other computers.");
    private static final TextKey USERNAME = TextKey.of("jsc.installer.mint.username", "Pick a username:");
    private static final TextKey THANKS = TextKey.of("jsc.installer.mint.thanks",
            "Thank you for choosing %s. We hope you will enjoy using it as much as we enjoyed making it.");
    private static final TextKey MANAGERS = TextKey.of("jsc.installer.mint.managers",
            "The Software Manager installs programs from your network's Mirror, and the Update Manager keeps"
                    + " them and the system up to date.");
    private static final TextKey FINISHED = TextKey.of("jsc.installer.mint.finished",
            "Installation has finished. You can continue testing %s now, but until you restart the computer, any"
                    + " changes you make or documents you save will not be preserved.");
    private static final TextKey QUIT = TextKey.of("jsc.installer.mint.quit", "Quit");
    private static final TextKey BACK = TextKey.of("jsc.installer.mint.back", "Back");
    private static final TextKey CONTINUE = TextKey.of("jsc.installer.mint.continue", "Continue");
    private static final TextKey INSTALL_NOW = TextKey.of("jsc.installer.mint.install_now", "Install Now");
    private static final TextKey GO_BACK = TextKey.of("jsc.installer.mint.go_back", "Go Back");
    private static final TextKey SKIP = TextKey.of("jsc.installer.mint.skip", "Skip");
    private static final TextKey CONTINUE_TESTING = TextKey.of("jsc.installer.mint.continue_testing",
            "Continue Testing");
    private static final TextKey RESTART_NOW = TextKey.of("jsc.installer.mint.restart_now", "Restart Now");

    InstallerMint(final InstallerFlow flow, final BlockPos host, final BlockPos monitor,
                  final Consumer<InstallerActionPayload> send, final IntSupplier memoryMb) {
        this.send = send;
        this.host = host;
        this.monitor = monitor;
        this.memoryMb = memoryMb;
        this.accept(flow, false);
    }

    @Override
    public void accept(final InstallerFlow given, final boolean samePage) {
        this.flow = given;
        if (samePage) {
            return;
        }
        this.confirming = false;
        this.typing = given.computerName();
        final int beside = this.besideIndex();
        this.alongside = beside >= 0;
        this.eraseIndex = Math.max(0, given.disks().indexOf(given.target()));
    }

    @Override
    public void paint(final GuiGraphics g, final Font font, final int x, final int y, final int ticksDone,
                      final int blink, final int mouseX, final int mouseY) {
        this.hits.clear();
        final Colours c = PALETTE.get();
        this.desktop(g, x, y);
        final int wx = x + WIN_X;
        final int wy = y + WIN_Y;
        final boolean dialogUp = this.confirming || this.flow.page() == InstallerPage.DONE;
        this.window(g, font, c, wx, wy, !dialogUp);
        final int cx = wx + 2;
        final int cy = wy + TITLE_H;
        final int cw = WIN_W - 4;
        final int ch = WIN_H - TITLE_H - 2;
        switch (this.flow.page()) {
            case DISK -> this.type(g, font, c, cx, cy, cw, ch);
            case ALONGSIDE -> this.divider(g, font, c, cx, cy, cw, ch);
            case NAME -> this.who(g, font, c, cx, cy, cw, ch, blink);
            case COPY, DONE -> this.copy(g, font, c, cx, cy, cw, ch, ticksDone);
            default -> {
            }
        }
        // A dialog takes every click and Enter for itself while it is up: what lies under it waits.
        if (this.confirming) {
            this.hits.clear();
            this.writeDialog(g, font, c, x, y);
        } else if (this.flow.page() == InstallerPage.DONE) {
            this.hits.clear();
            this.doneDialog(g, font, c, x, y);
        }
    }

    @Override
    public boolean key(final int key) {
        if (key == GLFW.GLFW_KEY_BACKSPACE && this.flow.page() == InstallerPage.NAME && !this.typing.isEmpty()) {
            this.typing = this.typing.substring(0, this.typing.length() - 1);
            return true;
        }
        if (this.flow.page() == InstallerPage.ALONGSIDE && !this.confirming
                && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) {
            this.moveDivider(key == GLFW.GLFW_KEY_RIGHT ? 1 : -1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            // Enter is the default button of whatever is up: the dialog's Continue, or the page's own.
            for (final Hit hit : List.copyOf(this.hits)) {
                if (hit.primary()) {
                    hit.action().run();
                    return true;
                }
            }
        }
        return this.flow.page() == InstallerPage.NAME;
    }

    @Override
    public boolean typed(final char letter) {
        if (this.flow.page() == InstallerPage.NAME && !this.confirming && letter >= ' ' && letter != 127
                && this.typing.length() < InstallerFlow.MOST_NAME_LETTERS) {
            this.typing += letter;
            return true;
        }
        return false;
    }

    @Override
    public boolean click(final double mouseX, final double mouseY) {
        for (final Hit hit : List.copyOf(this.hits)) {
            if (mouseX >= hit.x() && mouseX < hit.x() + hit.w() && mouseY >= hit.y() && mouseY < hit.y() + hit.h()) {
                hit.action().run();
                return true;
            }
        }
        return false;
    }

    /* The live desktop behind the window: as the monitor's face last drew it, or its wallpaper when it has none. */
    private void desktop(final GuiGraphics g, final int x, final int y) {
        final OffscreenDesktop desktop = MonitorPainter.desktopOf(this.monitor);
        final Minecraft mc = Minecraft.getInstance();
        if (desktop != null && mc.screen != null) {
            desktop.paint(g, mc.screen.width, mc.screen.height, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2, 0.0F);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0.0F);
        WallpaperStyle.MINT_Y.paint(g, InstallerLayout.WIDTH, InstallerLayout.HEIGHT, false);
        g.pose().popPose();
    }

    /*
     * The window's frame: the edge, green while it has the focus, the title bar with its icon and the three
     * controls.
     */
    private void window(final GuiGraphics g, final Font font, final Colours c, final int wx, final int wy,
                        final boolean active) {
        g.fill(wx, wy, wx + WIN_W, wy + WIN_H, active ? c.accent() : c.edge());
        g.fill(wx + 2, wy + 2, wx + WIN_W - 2, wy + WIN_H - 2, c.window());
        Draw.blended(() -> g.blit(ICON, wx + 3, wy + 2, 12, 12, 0.0F, 0.0F, 16, 16, 16, 16));
        TextWall.draw(g, font, GameText.resolve(INSTALL.text()), wx + 17, wy + 5, c.title());
        TextWall.draw(g, font, "_  o  x", wx + WIN_W - 30, wy + 4, c.title());
        g.fill(wx + 2, wy + TITLE_H - 1, wx + WIN_W - 2, wy + TITLE_H, c.track());
    }

    /* The installation type: beside the system already there, or the whole disk. */
    private void type(final GuiGraphics g, final Font font, final Colours c, final int x, final int y, final int w,
                      final int h) {
        Draw.textScaled(g, font, this.heading(InstallerPage.DISK), x + PAD, y + 4, c.ink(), 1.0F);
        final int beside = this.besideIndex();
        final InstallerFlow.Disk besideDisk = beside >= 0 ? this.flow.disks().get(beside) : null;
        int ty = this.paragraph(g, font, besideDisk != null ? GameText.resolve(HAS_SYSTEM.with(besideDisk.holds()))
                : GameText.resolve(NO_SYSTEM.text()), x + PAD, y + 18, w - 2 * PAD, c.ink()) + 4;
        if (besideDisk != null) {
            ty = this.option(g, font, c, x + PAD, ty, w - 2 * PAD, this.alongside,
                    GameText.resolve(ALONGSIDE.with(this.flow.systemName(), besideDisk.holds())),
                    GameText.resolve(ALONGSIDE_SUB.text()), () -> this.alongside = true);
        }
        ty = this.option(g, font, c, x + PAD, ty, w - 2 * PAD, !this.alongside,
                GameText.resolve(ERASE.with(this.flow.systemName())), GameText.resolve(ERASE_SUB.text()),
                () -> this.alongside = false);
        if (!this.alongside && this.flow.disks().size() > 1) {
            final InstallerFlow.Disk disk = this.flow.disks().get(this.eraseIndex);
            final String drive = GameText.resolve(DRIVE.with(disk.label() + " (" + gigabytes(disk.sizeMb()) + ")"));
            TextWall.draw(g, font, drive, x + PAD + 12, ty, c.dim());
            this.hits.add(new Hit(x + PAD + 12, ty - 1, TextWall.width(font, drive), ROW + 2, false,
                    () -> this.eraseIndex = (this.eraseIndex + 1) % this.flow.disks().size()));
        }
        this.dots(g, c, x + w / 2, y + h - 22, 3);
        this.buttons(g, font, c, x + w - PAD, y + h - BUTTON_H - 5,
                new Button(QUIT, false, this::quit), new Button(BACK, false, null),
                new Button(CONTINUE, true, this::chooseType));
    }

    /* Beside another system: the disk divided between the two, and the swap that goes with the new one. */
    private void divider(final GuiGraphics g, final Font font, final Colours c, final int x, final int y, final int w,
                         final int h) {
        final InstallerFlow.Disk disk = this.flow.target();
        final String other = disk == null ? "" : disk.holds();
        Draw.textScaled(g, font, GameText.resolve(this.flow.style().alongside(this.flow.systemName(), other)),
                x + PAD, y + 4, c.ink(), 1.0F);
        int ty = this.paragraph(g, font, GameText.resolve(DIVIDER.text()), x + PAD, y + 18, w - 2 * PAD, c.ink()) + 2;
        final InstallPlan.Plan plan = this.flow.plan();
        final DiskSpot beside = this.flow.besideSpot();
        final PartitionEntry shrunk = plan.layout() == null ? null : plan.layout().find(plan.shrunk());
        final PartitionEntry system = plan.system();
        if (disk != null && beside != null && shrunk != null && system != null && system.fs() != null) {
            final String device = this.deviceOf(disk);
            TextWall.draw(g, font, disk.label() + " (" + device + ")", x + PAD, ty, c.dim());
            ty += ROW + 2;
            // The bar is the other system's partition as it was: the divider is where it now ends.
            final int barW = w - 2 * PAD;
            final int split = Math.max(20, Math.min(barW - 20,
                    (int) ((long) barW * shrunk.sizeMb() / Math.max(1L, beside.sizeMb()))));
            g.fill(x + PAD, ty, x + PAD + split - 1, ty + 20, c.other());
            g.fill(x + PAD + split + 1, ty, x + PAD + barW, ty + 20, c.accent());
            g.fill(x + PAD + split - 1, ty - 2, x + PAD + split + 1, ty + 22, c.divider());
            TextWall.draw(g, font, other, x + PAD + 4, ty + 3, c.field());
            TextWall.draw(g, font, device + beside.number() + " (" + nameOf(beside.fileSystem()) + ")  "
                    + gigabytes(shrunk.sizeMb()), x + PAD + 4, ty + 11, c.field());
            TextWall.draw(g, font, this.flow.systemName(), x + PAD + split + 5, ty + 3, c.field());
            TextWall.draw(g, font, device + system.number() + " (" + nameOf(system.fs()) + ")  "
                    + gigabytes(system.sizeMb()), x + PAD + split + 5, ty + 11, c.field());
            // A click on either side moves the divider a gigabyte towards it, as LEFT and RIGHT do.
            this.hits.add(new Hit(x + PAD, ty, split, 20, false, () -> this.moveDivider(-1)));
            this.hits.add(new Hit(x + PAD + split, ty, barW - split, 20, false, () -> this.moveDivider(1)));
            ty += 27;
        }
        this.paragraph(g, font, GameText.resolve(SWAP.with(gigabytes(this.memoryMb.getAsInt()),
                this.flow.systemName())), x + PAD, ty, w - 2 * PAD, c.dim());
        this.dots(g, c, x + w / 2, y + h - 22, 3);
        this.buttons(g, font, c, x + w - PAD, y + h - BUTTON_H - 5,
                new Button(QUIT, false, this::quit), new Button(BACK, false, this::back),
                new Button(INSTALL_NOW, true, () -> this.confirming = true));
    }

    /* Who are you: the computer's name typed while the files are copied; the account's own fields wait for accounts. */
    private void who(final GuiGraphics g, final Font font, final Colours c, final int x, final int y, final int w,
                     final int h, final int blink) {
        Draw.textScaled(g, font, this.heading(InstallerPage.NAME), x + PAD, y + 4, c.ink(), 1.0F);
        final int labelRight = x + 92;
        final int fieldX = x + 97;
        int ty = y + 22;
        ty = this.field(g, font, c, labelRight, fieldX, ty, GameText.resolve(YOUR_NAME.text()),
                Minecraft.getInstance().getUser().getName(), false, blink);
        ty = this.field(g, font, c, labelRight, fieldX, ty, GameText.resolve(COMPUTER_NAME.text()), this.typing,
                true, blink);
        TextWall.draw(g, font, GameText.resolve(COMPUTER_NOTE.text()), fieldX, ty - 4, c.dim());
        ty += ROW;
        this.field(g, font, c, labelRight, fieldX, ty, GameText.resolve(USERNAME.text()), "player", false, blink);
        this.dots(g, c, x + w / 2, y + h - 22, 5);
        this.buttons(g, font, c, x + w - PAD, y + h - BUTTON_H - 5, new Button(CONTINUE, true, this::named));
    }

    /* The copy: Mint's welcome as a slideshow over the window, and under it the step and its bar. */
    private void copy(final GuiGraphics g, final Font font, final Colours c, final int x, final int y, final int w,
                      final int h, final int ticksDone) {
        final int slideH = h - 31;
        if (!this.skipped) {
            g.fill(x, y, x + w, y + slideH, c.slide());
            for (int i = 0; i < w; i += 3) {
                if ((i / 3) % 2 == 1) {
                    g.fill(x + i, y, x + Math.min(w, i + 2), y + slideH, c.stripe());
                }
            }
            Draw.blended(() -> g.blit(MARK, x + 15, y + 15, 36, 36, 0.0F, 0.0F, MARK_TEX, MARK_TEX, MARK_TEX,
                    MARK_TEX));
            Draw.textScaled(g, font, this.heading(InstallerPage.COPY), x + 59, y + 18, c.field(), 1.0F);
            this.paragraph(g, font, GameText.resolve(THANKS.with(this.flow.systemName())), x + 59, y + 32,
                    w - 75, c.slideText());
            this.paragraph(g, font, GameText.resolve(MANAGERS.text()), x + 15, y + 70, w - 30, c.slideText());
        }
        final int step = this.flow.stepAt(ticksDone);
        final String label = this.flow.steps().isEmpty() ? ""
                : GameText.resolve(this.flow.steps().get(step).label());
        final int by = y + slideH + 5;
        TextWall.draw(g, font, label, x + PAD, by, c.ink());
        final int barW = w - 55;
        g.fill(x + PAD, by + 9, x + PAD + barW, by + 13, c.track());
        g.fill(x + PAD, by + 9, x + PAD + barW * this.flow.permille(ticksDone) / 1000, by + 13, c.accent());
        if (!this.skipped && this.flow.page() == InstallerPage.COPY) {
            this.button(g, font, c, x + w - 43, by + 5, 36, new Button(SKIP, false, () -> this.skipped = true));
        }
    }

    /* The question before anything is written: the changes, listed as the real one lists them. */
    private void writeDialog(final GuiGraphics g, final Font font, final Colours c, final int x, final int y) {
        final int dx = x + 77;
        final int dy = y + 57;
        final int dw = 230;
        final int dh = 118;
        this.dialog(g, font, c, dx, dy, dw, dh, GameText.resolve(WRITE_TITLE.text()));
        final InstallerFlow.Disk disk = this.choiceDisk();
        int ty = this.paragraph(g, font, GameText.resolve(WRITE_TEXT.text()), dx + 6, dy + TITLE_H + 3, dw - 12,
                c.ink()) + 2;
        final String device = this.deviceOf(disk);
        ty = this.listed(g, font, c, dx + 6, ty, GameText.resolve(TABLES.text()),
                List.of(disk == null ? device : disk.label() + " (" + device + ")"));
        // What the installer is about to write, read off its plan: the partition shrunk and the ones made.
        final InstallPlan.Plan plan = this.alongside || disk == null ? this.flow.plan()
                : InstallPlan.erasing(this.flow, disk);
        final DiskSpot beside = this.flow.besideSpot();
        final PartitionEntry shrunk = plan.layout() == null ? null : plan.layout().find(plan.shrunk());
        if (this.alongside && beside != null && shrunk != null) {
            ty = this.listed(g, font, c, dx + 6, ty, GameText.resolve(RESIZED.text()),
                    List.of(GameText.resolve(RESIZE.with(beside.number(), device, gigabytes(beside.sizeMb()),
                            gigabytes(shrunk.sizeMb())))));
        }
        final List<String> formatted = new ArrayList<>();
        for (final int number : plan.made()) {
            final PartitionEntry made = plan.layout() == null ? null : plan.layout().find(number);
            if (made != null) {
                formatted.add(GameText.resolve(AS.with(number, device, nameOf(made.fs()))));
            }
        }
        this.listed(g, font, c, dx + 6, ty, GameText.resolve(FORMATTED.text()), formatted);
        this.buttons(g, font, c, dx + dw - 6, dy + dh - BUTTON_H - 5,
                new Button(GO_BACK, false, () -> this.confirming = false),
                new Button(CONTINUE, true, this::write));
    }

    /* The end: the system is on the disk; testing goes on until the machine is restarted into it. */
    private void doneDialog(final GuiGraphics g, final Font font, final Colours c, final int x, final int y) {
        final int dx = x + 87;
        final int dy = y + 80;
        final int dw = 210;
        final int dh = 75;
        this.dialog(g, font, c, dx, dy, dw, dh, this.heading(InstallerPage.DONE));
        this.paragraph(g, font, GameText.resolve(FINISHED.with(this.flow.systemName())), dx + 6, dy + TITLE_H + 4,
                dw - 12, c.ink());
        this.buttons(g, font, c, dx + dw - 6, dy + dh - BUTTON_H - 5,
                new Button(CONTINUE_TESTING, false, () -> this.act(InstallerActionPayload.ACTION_CONTINUE_TESTING, 0)),
                new Button(RESTART_NOW, true, () -> this.act(InstallerActionPayload.ACTION_REBOOT, 0)));
    }

    private void dialog(final GuiGraphics g, final Font font, final Colours c, final int dx, final int dy,
                        final int dw, final int dh, final String title) {
        g.fill(dx, dy, dx + dw, dy + dh, c.accent());
        g.fill(dx + 2, dy + 2, dx + dw - 2, dy + dh - 2, c.window());
        TextWall.draw(g, font, title, dx + 6, dy + 5, c.title());
        TextWall.draw(g, font, "x", dx + dw - 10, dy + 4, c.title());
        g.fill(dx + 2, dy + TITLE_H - 1, dx + dw - 2, dy + TITLE_H, c.track());
    }

    /* A heading over the indented rows it lists, as the write dialog lists what changes; answers the next free row. */
    private int listed(final GuiGraphics g, final Font font, final Colours c, final int x, final int y,
                       final String heading, final List<String> rows) {
        TextWall.draw(g, font, heading, x, y, c.ink());
        int ty = y + ROW;
        for (final String row : rows) {
            TextWall.draw(g, font, row, x + 6, ty, c.dim());
            ty += ROW;
        }
        return ty + 2;
    }

    /* A radio choice with its line under it; answers the next free row. */
    private int option(final GuiGraphics g, final Font font, final Colours c, final int x, final int y, final int w,
                       final boolean on, final String head, final String sub, final Runnable choose) {
        Draw.outline(g, x, y, 7, 7, on ? c.accent() : c.dim());
        if (on) {
            g.fill(x + 2, y + 2, x + 5, y + 5, c.accent());
        }
        TextWall.draw(g, font, head, x + 11, y, c.ink());
        final int next = this.paragraph(g, font, sub, x + 11, y + ROW + 1, w - 11, c.dim()) + 4;
        this.hits.add(new Hit(x, y - 1, w, next - y, false, choose));
        return next;
    }

    /* A label right-aligned at {@code labelRight} and the field beside it; answers the next row. */
    private int field(final GuiGraphics g, final Font font, final Colours c, final int labelRight, final int fieldX,
                      final int y, final String label, final String value, final boolean editable, final int blink) {
        TextWall.right(g, font, label, labelRight, y + 3, c.ink());
        g.fill(fieldX, y, fieldX + 110, y + 11, c.edge());
        g.fill(fieldX + 1, y + 1, fieldX + 109, y + 10, editable ? c.field() : c.window());
        final boolean caret = editable && (blink / CARET_TICKS) % 2 == 0;
        TextWall.draw(g, font, value + (caret ? "_" : ""), fieldX + 3, y + 3, editable ? c.ink() : c.dim());
        return y + 16;
    }

    /* The row of dots that marks the step, lit up to the one the page is. */
    private void dots(final GuiGraphics g, final Colours c, final int cx, final int y, final int on) {
        final int x0 = cx - DOTS * 7 / 2;
        for (int i = 0; i < DOTS; i++) {
            g.fill(x0 + i * 7 + 1, y, x0 + i * 7 + 4, y + 3, i <= on ? c.accent() : c.dotOff());
        }
    }

    /* Buttons laid right to left from {@code right}: a null action is drawn greyed and does nothing. */
    private void buttons(final GuiGraphics g, final Font font, final Colours c, final int right, final int y,
                         final Button... buttons) {
        int x = right;
        for (int i = buttons.length - 1; i >= 0; i--) {
            final int w = TextWall.width(font, GameText.resolve(buttons[i].label().text())) + 12;
            x -= w;
            this.button(g, font, c, x, y, w, buttons[i]);
            x -= 4;
        }
    }

    private void button(final GuiGraphics g, final Font font, final Colours c, final int x, final int y, final int w,
                        final Button button) {
        final String label = GameText.resolve(button.label().text());
        if (button.primary()) {
            g.fill(x, y, x + w, y + BUTTON_H, c.accent());
            TextWall.centered(g, font, label, x + w / 2, y + 2, c.field());
        } else {
            g.fill(x, y, x + w, y + BUTTON_H, c.edge());
            g.fill(x + 1, y + 1, x + w - 1, y + BUTTON_H - 1, c.face());
            TextWall.centered(g, font, label, x + w / 2, y + 2, button.action() == null ? c.dim() : c.ink());
        }
        if (button.action() != null) {
            this.hits.add(new Hit(x, y, w, BUTTON_H, button.primary(), button.action()));
        }
    }

    /* Words wrapped to {@code width}; answers the row under the last. */
    private int paragraph(final GuiGraphics g, final Font font, final String text, final int x, final int y,
                          final int width, final int colour) {
        int ty = y;
        String line = "";
        for (final String word : text.split(" ")) {
            final String trial = line.isEmpty() ? word : line + " " + word;
            if (TextWall.width(font, trial) > width && !line.isEmpty()) {
                TextWall.draw(g, font, line, x, ty, colour);
                ty += ROW;
                line = word;
            } else {
                line = trial;
            }
        }
        if (!line.isEmpty()) {
            TextWall.draw(g, font, line, x, ty, colour);
            ty += ROW;
        }
        return ty;
    }

    /*
     * Continue on the type page: the divider beside another system, or the dialog that lists the whole disk's
     * changes.
     */
    private void chooseType() {
        final InstallerFlow.Disk disk = this.choiceDisk();
        if (disk == null) {
            return;
        }
        this.act(InstallerActionPayload.ACTION_SELECT_DISK, disk.slot());
        if (this.alongside) {
            this.act(InstallerActionPayload.ACTION_CANCEL_ERASE, 0);
            this.act(InstallerActionPayload.ACTION_NEXT, 0);
        } else {
            this.confirming = true;
        }
    }

    /* Continue in the dialog: the changes are written, which starts the copy. */
    private void write() {
        final InstallerFlow.Disk disk = this.choiceDisk();
        this.confirming = false;
        if (disk == null) {
            return;
        }
        if (!this.alongside && (disk.hasSystem() || disk.freeMb() < disk.sizeMb())) {
            this.act(InstallerActionPayload.ACTION_ERASE, disk.slot());
        }
        this.act(InstallerActionPayload.ACTION_NEXT, 0);
    }

    private void named() {
        if (this.typing.isBlank()) {
            return;
        }
        this.send.accept(InstallerActionPayload.named(this.host, this.monitor, this.typing));
        this.act(InstallerActionPayload.ACTION_NEXT, 0);
    }

    private void back() {
        this.act(InstallerActionPayload.ACTION_BACK, 0);
    }

    private void quit() {
        this.act(InstallerActionPayload.ACTION_QUIT, 0);
    }

    private void act(final int action, final int value) {
        this.send.accept(InstallerActionPayload.of(this.host, this.monitor, action, value));
    }

    /*
     * Moves the divider a gigabyte: the other system's partition is left that much bigger or smaller, never below
     * what it holds nor past what it was.
     */
    private void moveDivider(final int gigabytes) {
        final InstallerFlow.Disk disk = this.flow.target();
        final DiskSpot beside = this.flow.besideSpot();
        final InstallPlan.Plan plan = this.flow.plan();
        final PartitionEntry shrunk = plan.layout() == null ? null : plan.layout().find(plan.shrunk());
        if (disk == null || beside == null) {
            return;
        }
        final long now = shrunk != null ? shrunk.sizeMb() : InstallPlan.shrinkFor(this.flow, disk);
        final long to = Math.max(beside.usedMb(), Math.min(beside.sizeMb(), now + gigabytes * MB_PER_GB));
        this.send.accept(InstallerActionPayload.of(this.host, this.monitor, InstallerActionPayload.ACTION_SHRINK,
                disk.slot(), to));
    }

    /* A file system as a disk tool names it, in lower case. */
    private static String nameOf(@Nullable final FileSystemType fs) {
        return fs == null ? "" : fs.displayName().toLowerCase(Locale.ROOT);
    }

    /* The disk the choice on the type page is about: the one beside another system, or the one to take whole. */
    @Nullable
    private InstallerFlow.Disk choiceDisk() {
        if (this.alongside) {
            final int beside = this.besideIndex();
            return beside < 0 ? null : this.flow.disks().get(beside);
        }
        return this.flow.disks().isEmpty() ? null
                : this.flow.disks().get(Math.min(this.eraseIndex, this.flow.disks().size() - 1));
    }

    /* The first disk that holds another system with room beside it for this one, or -1 when none does. */
    private int besideIndex() {
        for (int i = 0; i < this.flow.disks().size(); i++) {
            final InstallerFlow.Disk disk = this.flow.disks().get(i);
            if (disk.hasSystem() && disk.freeMb() >= this.flow.footprintMb()) {
                return i;
            }
        }
        return -1;
    }

    /* The page's heading, in the installer's words. */
    private String heading(final InstallerPage page) {
        return GameText.resolve(this.flow.style().heading(page, this.flow.systemName()));
    }

    /* What the installer calls a disk, by its place among the machine's: sda for the first. */
    private String deviceOf(@Nullable final InstallerFlow.Disk disk) {
        final int index = disk == null ? 0 : Math.max(0, this.flow.disks().indexOf(disk));
        return "sd" + (char) ('a' + index);
    }

    private static String gigabytes(final long mb) {
        return String.format(Locale.ROOT, "%.1f GB", mb / (double) MB_PER_GB);
    }

    /** Something drawn that a click lands on, and what it does; the primary one is what Enter presses. */
    private record Hit(int x, int y, int w, int h, boolean primary, Runnable action) {
    }

    /** A button: its word, whether it is the page's own, and what it does; a null action greys it out. */
    private record Button(TextKey label, boolean primary, @Nullable Runnable action) {
    }

    /** The window's colours, as the palette above names them. */
    private record Colours(int title, int window, int edge, int accent, int dim, int field, int face, int track,
                           int dotOff, int other, int divider, int slide, int stripe, int slideText, int ink) {
    }
}
