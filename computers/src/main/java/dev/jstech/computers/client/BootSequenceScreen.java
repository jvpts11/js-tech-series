/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.operation.payload.FirmwareActionPayload;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.PostCompletePayload;
import dev.jstech.computers.operation.payload.RequestFirmwareStatePayload;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.core.gui.Phosphor;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The power-on self-test: what the monitor shows between switching the computer on (or a reboot) and the
 * boot target opening. Lines appear over a few seconds in the firmware's era style (the processor and the
 * board, the memory counted over the modules seated, every drive with what it holds, the boot device) and
 * DEL at any point enters the firmware setup instead. When the sequence ends the client reports
 * {@link PostCompletePayload} and the server swaps this screen for whatever boots.
 */
public final class BootSequenceScreen extends AbstractComputerScreen<MonitorSessionMenu> {

    private static final int W = 340;
    private static final int H = 214;

    /** How long a self-test is drawn for when the machine did not say, which only a stale packet leaves. */
    private static final int FALLBACK_TICKS = 70;

    /** Where the wall of text begins, and how much air is left at the right edge. */
    private static final int MARGIN = 10;

    /** The columns a drive row is printed in, from the wall's left edge: role, device, size, contents. */
    private static final int COL_DEVICE = 52;
    private static final int COL_SIZE = 176;
    private static final int COL_HOLDS = 222;

    /** The self-test the machine last reported, kept until the session that shows it is built. */
    @Nullable
    private static Testing pending;

    private final BlockPos computerPos;
    private final BlockPos monitorPos;
    private final FirmwareKind kind;
    private final String machineName;
    /** What the machine said was left of its self-test when this screen opened. */
    private final int postTicks;

    private static BootSequenceScreen active;

    private FirmwareStatePayload state;
    private int ticks;
    private boolean completed;
    private boolean setupRequested;
    /** Whether F12 opened the one-time boot menu over the self-test. */
    private boolean bootMenu;
    /** Which entry the menu is on. */
    private int menuAt;

    public BootSequenceScreen(final MonitorSessionMenu session, final Inventory inventory,
                              final Component title) {
        super(session, inventory, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        this.computerPos = session.hostPos();
        this.monitorPos = session.monitorPos();
        final Testing testing = pending != null ? pending
                : new Testing(FirmwareKind.forEra(HardwareEra.STANDARD), "", FALLBACK_TICKS, false);
        this.kind = testing.kind();
        this.machineName = testing.machineName();
        this.postTicks = testing.remainingTicks() > 0 ? testing.remainingTicks() : FALLBACK_TICKS;
        /*
         * A machine standing at a self-test that found nothing to boot opens at the end of that test rather
         * than playing one through: the test is over, and what is on the glass is the failure it ended on.
         */
        if (testing.halted()) {
            this.ticks = this.postTicks;
            this.completed = true;
        }
    }

    /** The self-test the machine is in, said before the session that shows it is opened. */
    public static void expect(final FirmwareKind kind, final String machineName, final int remainingTicks,
                              final boolean halted) {
        pending = new Testing(kind, machineName, remainingTicks, halted);
    }

    /** One machine testing itself: which firmware it wears, what it is called, and where it has got to. */
    private record Testing(FirmwareKind kind, String machineName, int remainingTicks, boolean halted) {
    }

    /**
     * One printed line of a self-test.
     *
     * <p>A machine of these ages printed in one weight and lifted what it had just found out of it, so a line
     * is what leads into the finding, the finding itself, and whatever trails after it. A line the firmware
     * puts at both ends of the glass carries what belongs on the right as well.
     *
     * <p>A drive row is columns instead, because a variable-width font cannot be made to line up with spaces
     * and a list of drives that does not line up is a list nobody can read down.
     */
    private record PostLine(String head, String hot, String tail, String right, String[] cols) {

        static PostLine of(final String text) {
            return new PostLine(text, "", "", "", null);
        }

        static PostLine blank() {
            return new PostLine("", "", "", "", null);
        }

        static PostLine found(final String head, final String hot, final String tail) {
            return new PostLine(head, hot, tail, "", null);
        }

        static PostLine columns(final String role, final String device, final String size, final String holds) {
            return new PostLine("", "", "", "", new String[]{role, device, size, holds});
        }
    }

    /** The machine's own generation, so the bezel is the monitor that machine would really have. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return this.getMenu().hardwareEra();
    }

    /** The same, never null, for the wording a firmware of that age would really have put up. */
    private HardwareEra bezelEra() {
        final HardwareEra era = this.screenEra();
        return era == null ? HardwareEra.STANDARD : era;
    }

    @Override
    protected void init() {
        super.init();
        active = this;
        PacketDistributor.sendToServer(new RequestFirmwareStatePayload(computerPos));
    }

    @Override
    public void removed() {
        if (active == this) {
            active = null;
        }
        super.removed();
    }

    /** Routes the server's hardware state to the running POST, which lists it as detected devices. */
    public static void accept(final FirmwareStatePayload payload) {
        if (active != null && active.computerPos.equals(payload.hostPos())) {
            active.state = payload;
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ticks++;
        /*
         * The machine is the one that ends its self-test and puts whoever is watching in front of what boots,
         * so this waits rather than reporting. All the screen owes is a way out if nothing ever comes: a
         * machine switched off mid-test leaves this here with nothing else to show.
         */
        if (!completed && ticks >= postTicks) {
            completed = true;
        }
        /*
         * This never sees itself out. It used to, as a way out if the machine never swapped the screen, and that
         * became the reason the machine could not: the session a player holds is how the machine knows who is
         * watching, so closing it is leaving, and leaving guarantees that nothing comes. A self-test ends by the
         * machine handing over, a failed one waits for a key, and a machine switched off mid-test is left on the
         * glass until somebody presses Escape, which is the way out of every other screen here.
         */
    }

    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        if (bootMenu) {
            return bootMenuKey(keyCode);
        }
        if (keyCode == InputConstants.KEY_F12 && !setupRequested && !bootable().isEmpty()) {
            bootMenu = true;
            menuAt = 0;
            return true;
        }
        /*
         * A self-test that ended with nothing to boot waits here, as a machine does. Any key asks the machine
         * to go on, which with nothing on any disk means its setup.
         */
        if (completed && !setupRequested && bootingFrom().isEmpty()) {
            setupRequested = true;
            PacketDistributor.sendToServer(new PostCompletePayload(computerPos, monitorPos, false));
            return true;
        }
        if (keyCode == InputConstants.KEY_DELETE && !completed && !setupRequested) {
            setupRequested = true;
            PacketDistributor.sendToServer(new PostCompletePayload(computerPos, monitorPos, true));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Render

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = this.leftPos;
        final int y = this.topPos;
        MonitorFrame.renderBody(g, x, y, W, H, screenEra(), font);

        /*
         * A Legacy machine posts on black with the maker's badge, the way the boards of that time did; the
         * setup it opens with DEL is the blue one. The two are different screens and only setup was ever blue.
         */
        final int bg = switch (kind) {
            case CLI_BIOS, BLUE_BIOS -> 0xFF000000;
            case UEFI -> 0xFF10121C;
        };
        /*
         * The Vintage machine posts on a green-phosphor tube, which has exactly one colour: its text is
         * that green, brighter or dimmer, never the grey-white of a later monitor.
         */
        final int text = switch (kind) {
            case CLI_BIOS -> Phosphor.green(0xFFB8B8B8);
            case BLUE_BIOS -> 0xFFBDBDBD;
            case UEFI -> 0xFFE6ECF6;
        };
        final int dim = switch (kind) {
            case CLI_BIOS -> Phosphor.green(0xFF707070);
            case BLUE_BIOS -> 0xFF8A8A8A;
            case UEFI -> 0xFF8090A8;
        };
        final int accent = switch (kind) {
            case CLI_BIOS -> Phosphor.green(0xFFE8E8E8);
            case BLUE_BIOS -> 0xFFFFFFFF;
            case UEFI -> 0xFF5FA8D3;
        };
        g.fill(x, y, x + W, y + H, bg);

        if (kind == FirmwareKind.UEFI) {
            renderUefi(g, x, y, text, dim, accent);
        } else {
            renderClassic(g, x, y, text, dim, accent);
        }
        if (kind == FirmwareKind.BLUE_BIOS) {
            /*
             * The badge the boards of that age wore in the corner of their self-test: a framed block, printed
             * on the board and put on the glass, rather than the house's name set in the machine's own font.
             */
            SplashLogos.badge(g, x + W - MARGIN, y + 8);
        }
        if (bootMenu) {
            renderBootMenu(g, x, y, text, dim, accent);
        }
    }

    /** The classic POST wall of text: the banner, the machine, what is in it, and what it will boot. */
    private void renderClassic(final GuiGraphics g, final int x, final int y, final int text, final int dim,
                               final int accent) {
        final List<PostLine> lines = classicLines();
        final int left = x + MARGIN;
        final int right = x + W - MARGIN;
        int ty = y + 10;
        for (int i = 0; i < lines.size(); i++) {
            if (ticks < 4 + i * 3) {
                break; // lines appear one by one, like a machine actually finding its hardware
            }
            final PostLine line = lines.get(i);
            if (line.cols() != null) {
                final String[] cols = line.cols();
                wall(g, cols[0], left + 6, ty, text);
                wall(g, cols[1], left + COL_DEVICE, ty, text);
                wall(g, cols[2], left + COL_SIZE, ty, dim);
                wall(g, cols[3], left + COL_HOLDS, ty, text);
            } else {
                int tx = left;
                if (!line.head().isEmpty()) {
                    wall(g, line.head(), tx, ty, text);
                    tx += wallWidth(line.head());
                }
                if (!line.hot().isEmpty()) {
                    wall(g, line.hot(), tx, ty, accent);
                    tx += wallWidth(line.hot());
                }
                if (!line.tail().isEmpty()) {
                    wall(g, line.tail(), tx, ty, text);
                }
                if (!line.right().isEmpty()) {
                    wallRight(g, line.right(), right, ty, dim);
                }
            }
            ty += WALL_ROW;
        }
        renderClassicHint(g, x, y, dim, accent);
    }

    /**
     * The key hints along the bottom, in the words and the manner of that machine's own firmware.
     *
     * <p>The earliest boards blinked theirs; the ones after them printed a sentence and left it there, with the
     * two keys lifted out of it. Blinking both was one habit borrowed across a decade it did not belong to.
     */
    private void renderClassicHint(final GuiGraphics g, final int x, final int y, final int dim,
                                   final int accent) {
        final int ty = y + H - 14;
        if (setupRequested) {
            wall(g, "Entering SETUP ...", x + MARGIN, ty, accent);
            return;
        }
        if (kind == FirmwareKind.CLI_BIOS) {
            if ((ticks / 10) % 2 == 0) {
                wall(g, "DEL  Setup      F12  Boot Menu", x + MARGIN, ty, dim);
            }
            return;
        }
        final int key = 0xFFFFE14D;
        int tx = x + MARGIN;
        tx = hintRun(g, "Press ", tx, ty, dim);
        tx = hintRun(g, "DEL", tx, ty, key);
        tx = hintRun(g, " to enter SETUP, ", tx, ty, dim);
        tx = hintRun(g, "F12", tx, ty, key);
        hintRun(g, " for Boot Menu", tx, ty, dim);
    }

    /** Draws one run of the hint line and answers where the next one starts. */
    private int hintRun(final GuiGraphics g, final String run, final int x, final int y, final int color) {
        wall(g, run, x, y, color);
        return x + wallWidth(run);
    }

    /**
     * What this machine's firmware prints while it tests itself.
     *
     * <p>The two ages print the same facts in their own hand: the earliest board names the parts in a column of
     * short labels, the one after it writes them out and puts the machine and its board on a line of their own.
     */
    private List<PostLine> classicLines() {
        final boolean legacy = kind == FirmwareKind.BLUE_BIOS;
        final List<PostLine> out = new ArrayList<>();
        out.add(new PostLine("", Branding.biosBanner(bezelEra()), "", legacy ? "" : machineTitle(), null));
        out.add(PostLine.of(Branding.firmwareCopyright(bezelEra())));
        out.add(PostLine.blank());
        if (state == null) {
            out.add(PostLine.of("Reading system configuration ..."));
            return out;
        }
        final FirmwareStatePayload.Machine machine = state.machine();
        if (legacy) {
            out.add(PostLine.found("", machineTitle(), machine.boardName().isEmpty()
                    ? "" : "   " + machine.boardName()));
            out.add(PostLine.blank());
        }
        /*
         * The processor by its own model, then what it is: a self-test reads out the machine it found, and the
         * model is the part of it a player recognises.
         */
        final String noCpu = "not detected";
        out.add(PostLine.found(legacy ? "Main Processor : " : "Processor : ",
                machine.cpuName().isEmpty() ? noCpu : machine.cpuName(),
                machine.hasCpu() ? cpuDetail(machine, legacy) : ""));
        if (!legacy) {
            out.add(PostLine.of("Board     : " + (machine.boardName().isEmpty()
                    ? "not detected" : machine.boardName())));
        }
        out.add(PostLine.of(memoryLine(machine, legacy)));
        out.add(PostLine.of((legacy ? "Video Adapter  : " : "Video     : ")
                + (machine.gpuName().isEmpty() ? "none" : machine.gpuName())));
        out.add(PostLine.blank());
        out.add(PostLine.of(legacy ? "Detecting drives ..." : "Detecting drives..."));
        int slot = 0;
        for (final FirmwareStatePayload.Entry entry : state.entries()) {
            final String role = entry.kind() == FirmwareStatePayload.KIND_DISK ? "Disk " + slot++ : entry.device();
            final String device = entry.kind() == FirmwareStatePayload.KIND_DISK ? entry.device() : "";
            out.add(PostLine.columns(role, device, entry.size(), entry.label()));
        }
        out.add(PostLine.blank());
        if (completed) {
            /*
             * What it is about to boot, by name, or the era's own way of saying there is nothing: a machine of
             * this age told you which drive it was reaching for, and which one it had given up on.
             */
            final String booting = bootingFrom();
            if (!booting.isEmpty()) {
                out.add(PostLine.found("", "Booting from " + booting + " ...", ""));
            } else {
                for (final String line : noBootLines()) {
                    out.add(PostLine.found("", line, ""));
                }
            }
        }
        return out;
    }

    /** What the firmware says about the processor beside its model: cores, clock and architecture. */
    private static String cpuDetail(final FirmwareStatePayload.Machine machine, final boolean legacy) {
        if (legacy) {
            return "  " + machine.cpuMhz() + " MHz  " + machine.cpuArch();
        }
        return "   " + machine.cores() + (machine.cores() == 1 ? " core   " : " cores   ")
                + machine.cpuMhz() + " MHz   " + machine.cpuArch();
    }

    /** The memory line: what the count has reached, and which modules it is counting over. */
    private String memoryLine(final FirmwareStatePayload.Machine machine, final boolean legacy) {
        // The memory test counts up while the POST runs, settling on the installed total.
        final long total = (long) machine.ramMb() * 1024L;
        final long counted = Math.min(total, total * Math.max(0, ticks - 12) / 28L);
        final String amount = String.format(Locale.ROOT, "%,d", counted);
        final String modules = machine.memoryModules();
        if (legacy) {
            return "Memory Testing : " + amount + "K OK" + (modules.isEmpty() ? "" : "  " + modules);
        }
        return "Memory    : " + amount + " KB OK" + (modules.isEmpty() ? "" : "      " + modules);
    }

    /** One thing the one-time menu can boot: the entry, and the disk slot it sits in, or -1 for a medium. */
    private record Choice(FirmwareStatePayload.Entry entry, int slot) {
    }

    /** Everything this machine could boot right now, in the order the firmware found it. */
    private List<Choice> bootable() {
        if (state == null) {
            return List.of();
        }
        final List<Choice> out = new ArrayList<>();
        int slot = 0;
        for (final FirmwareStatePayload.Entry entry : state.entries()) {
            final int here = entry.kind() == FirmwareStatePayload.KIND_DISK ? slot++ : -1;
            if (entry.bootable()) {
                out.add(new Choice(entry, here));
            }
        }
        return out;
    }

    /** What a menu row calls the place an entry boots from: the disk by its slot, or the drive by its kind. */
    private static String whereOf(final Choice choice) {
        return choice.slot() >= 0 ? "Disk " + choice.slot() : choice.entry().device();
    }

    /**
     * The one-time boot menu, drawn over the self-test the way that machine's own firmware drew it.
     *
     * <p>Whatever is picked here is for this boot and no other: the order saved in setup is not touched, which
     * is the whole point of having it apart from the setup.
     */
    private void renderBootMenu(final GuiGraphics g, final int x, final int y, final int text, final int dim,
                                final int accent) {
        switch (kind) {
            case CLI_BIOS -> renderTubeMenu(g, x, y, text, accent);
            case BLUE_BIOS -> renderBlueMenu(g, x, y);
            case UEFI -> renderUefiMenu(g, x, y, text, dim, accent);
        }
    }

    /** The earliest machines: a double-ruled box on the tube, its entries numbered as that firmware numbered. */
    private void renderTubeMenu(final GuiGraphics g, final int x, final int y, final int text, final int accent) {
        final List<Choice> choices = bootable();
        final int boxW = 210;
        final int boxH = 28 + choices.size() * WALL_ROW + 12;
        final int bx = x + (W - boxW) / 2;
        final int by = y + (H - boxH) / 2;
        g.fill(bx, by, bx + boxW, by + boxH, 0xFF000000);
        rule(g, bx, by, boxW, boxH, accent);
        rule(g, bx + 2, by + 2, boxW - 4, boxH - 4, accent);
        wall(g, "Boot Menu  (this boot only)", bx + 8, by + 8, accent);
        int ly = by + 24;
        for (int i = 0; i < choices.size(); i++) {
            final Choice choice = choices.get(i);
            final boolean on = i == menuAt;
            wall(g, (on ? "> " : "  ") + (i + 1) + ". " + whereOf(choice), bx + 8, ly, on ? accent : text);
            wall(g, choice.entry().label(), bx + 92, ly, on ? accent : text);
            ly += WALL_ROW;
        }
        wall(g, "Up/Down  select     Enter  boot", bx + 8, by + boxH - 12, text);
    }

    /** The boards after them: the setup's own blue, a grey title bar, and the choice filled light. */
    private void renderBlueMenu(final GuiGraphics g, final int x, final int y) {
        final int ground = 0xFF0000A8;
        final int frame = 0xFFB9B9B9;
        final int chosen = 0xFFD9D9D9;
        final List<Choice> choices = bootable();
        final int boxW = 212;
        final int rowH = WALL_ROW + 3;
        final int boxH = 14 + choices.size() * rowH + 18;
        final int bx = x + (W - boxW) / 2;
        final int by = y + (H - boxH) / 2;
        g.fill(bx, by, bx + boxW, by + boxH, ground);
        rule(g, bx, by, boxW, boxH, frame);
        g.fill(bx + 1, by + 1, bx + boxW - 1, by + 13, frame);
        wallCentered(g, "Boot Menu", bx + boxW / 2, by + 4, ground);
        int ly = by + 16;
        for (int i = 0; i < choices.size(); i++) {
            final Choice choice = choices.get(i);
            final boolean on = i == menuAt;
            if (on) {
                g.fill(bx + 2, ly - 1, bx + boxW - 2, ly + rowH - 2, chosen);
            }
            wall(g, whereOf(choice), bx + 8, ly, on ? ground : 0xFFFFFFFF);
            wall(g, choice.entry().label(), bx + 84, ly, on ? ground : 0xFFFFFFFF);
            ly += rowH;
        }
        g.fill(bx + 1, ly + 1, bx + boxW - 1, ly + 2, 0xFF6FB7FF);
        wall(g, "Up/Down: Select     Enter: Boot", bx + 8, ly + 6, 0xFFFFE14D);
    }

    /** The modern machines: a dialog over the dimmed splash, each entry marked by what it is. */
    private void renderUefiMenu(final GuiGraphics g, final int x, final int y, final int text, final int dim,
                                final int accent) {
        final List<Choice> choices = bootable();
        final int boxW = 212;
        final int rowH = WALL_ROW + 6;
        final int boxH = 16 + choices.size() * (rowH + 3) + 16;
        final int bx = x + (W - boxW) / 2;
        final int by = y + (H - boxH) / 2;
        g.fill(bx, by, bx + boxW, by + boxH, 0xFF2A2D3E);
        g.fill(bx, by, bx + boxW, by + 14, 0xFF3A4060);
        g.fill(bx, by, bx + 2, by + 14, accent);
        wall(g, "Boot Menu", bx + 7, by + 4, text);
        wallRight(g, "this boot only", bx + boxW - 7, by + 4, dim);
        int ly = by + 17;
        for (int i = 0; i < choices.size(); i++) {
            final Choice choice = choices.get(i);
            final boolean on = i == menuAt;
            g.fill(bx + 5, ly, bx + boxW - 5, ly + rowH, on ? 0xFF23405A : 0xFF3A4060);
            if (on) {
                g.fill(bx + 5, ly, bx + 7, ly + rowH, accent);
            }
            /*
             * Green for a disk that carries a system, amber for a medium: what a player wants to know at a
             * glance is which of these boots what is already installed and which one installs something new.
             */
            final int dot = choice.slot() >= 0 ? 0xFF5FE07A : 0xFFF0B23A;
            g.fill(bx + 12, ly + 5, bx + 16, ly + 9, dot);
            wall(g, choice.entry().label(), bx + 21, ly + 3, text);
            wallRight(g, deviceOf(choice), bx + boxW - 9, ly + 3, dim);
            ly += rowH + 3;
        }
        wall(g, "Up/Down Select   Enter Boot", bx + 7, ly + 3, dim);
    }

    /** How a modern dialog names where an entry lives: the disk by slot and model, or the drive on its own. */
    private static String deviceOf(final Choice choice) {
        if (choice.slot() < 0) {
            return choice.entry().device();
        }
        return "Disk " + choice.slot() + " " + choice.entry().device();
    }

    /** A one-pixel frame around a box, which is how every firmware here draws one. */
    private static void rule(final GuiGraphics g, final int x, final int y, final int w, final int h,
                             final int color) {
        g.fill(x, y, x + w, y + 1, color);
        g.fill(x, y + h - 1, x + w, y + h, color);
        g.fill(x, y, x + 1, y + h, color);
        g.fill(x + w - 1, y, x + w, y + h, color);
    }

    /** Up and down walk the menu, Enter boots what is on, and anything else puts the menu away. */
    private boolean bootMenuKey(final int keyCode) {
        final List<Choice> choices = bootable();
        if (choices.isEmpty()) {
            bootMenu = false;
            return true;
        }
        switch (keyCode) {
            case InputConstants.KEY_UP -> menuAt = (menuAt - 1 + choices.size()) % choices.size();
            case InputConstants.KEY_DOWN -> menuAt = (menuAt + 1) % choices.size();
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                final Choice chosen = choices.get(Math.min(menuAt, choices.size() - 1));
                bootMenu = false;
                setupRequested = true;
                /*
                 * A disk boots this once and the saved order stays where it is; a medium boots the way it does
                 * from the setup, since booting one is already a thing that happens once.
                 */
                PacketDistributor.sendToServer(new FirmwareActionPayload(computerPos, monitorPos,
                        chosen.slot() >= 0 ? FirmwareActionPayload.ACTION_BOOT_ONCE
                                : FirmwareActionPayload.ACTION_BOOT_MEDIA,
                        chosen.slot() >= 0 ? chosen.slot() : chosen.entry().ref(), -1));
            }
            default -> bootMenu = false;
        }
        return true;
    }

    /**
     * What the machine is about to boot, worded as the firmware would say it ("Disk 0: Frames XP"), or nothing
     * when no disk carries a system and no medium in a drive can boot.
     */
    private String bootingFrom() {
        if (state == null) {
            return "";
        }
        FirmwareStatePayload.Entry chosen = null;
        int slot = 0;
        int chosenSlot = 0;
        for (final FirmwareStatePayload.Entry entry : state.entries()) {
            final int here = entry.kind() == FirmwareStatePayload.KIND_DISK ? slot++ : -1;
            if (!entry.bootable()) {
                continue;
            }
            final boolean preferred = here >= 0 && here == state.bootSlot();
            if (chosen == null || preferred) {
                chosen = entry;
                chosenSlot = here;
            }
            if (preferred) {
                break;
            }
        }
        if (chosen == null) {
            return "";
        }
        return (chosenSlot >= 0 ? "Disk " + chosenSlot : "the medium") + ": " + chosen.label();
    }

    /** How this machine's age says that nothing can be booted. */
    private List<String> noBootLines() {
        return switch (kind) {
            case CLI_BIOS -> List.of("Non-system disk or disk error",
                    "Replace and press any key when ready");
            case BLUE_BIOS -> List.of("DISK BOOT FAILURE, INSERT SYSTEM DISK AND PRESS ENTER");
            case UEFI -> List.of("No bootable device found",
                    "Press any key to enter Setup");
        };
    }

    /** What the machine is called: the name its owner gave it, else the kind of machine it is. */
    private String machineTitle() {
        final String named = state == null ? "" : state.machine().name();
        return named.isEmpty() ? machineName : named;
    }

    /** The machine on one line, the way a modern firmware sums a computer up while it comes awake. */
    private String buildSummary() {
        if (state == null || !state.machine().hasCpu()) {
            return "";
        }
        final FirmwareStatePayload.Machine m = state.machine();
        final String memory = m.ramMb() >= 1024 ? m.ramMb() / 1024 + " GB" : m.ramMb() + " MB";
        return m.cpuName() + " · " + m.cores() + (m.cores() == 1 ? " core" : " cores")
                + " · " + memory + " · " + m.cpuArch();
    }

    /**
     * The modern machines hide the wall of text: the maker's mark, the machine's own name, a bar under it,
     * and along the foot what it is made of on one side and the two keys on the other.
     */
    private void renderUefi(final GuiGraphics g, final int x, final int y, final int text, final int dim,
                            final int accent) {
        /*
         * The mark is a picture rather than the house's name set in the game's font: it is a lockup with its
         * own lettering, and writing the words out in one size was the thing that made this screen read as a
         * placeholder. No screen inside the fiction names the mod, and the machine in front of the player is
         * the one it names.
         */
        final int cx = x + W / 2;
        SplashLogos.draw(g, SplashLogos.JSC, cx, y + H / 2 - 46);
        wallCentered(g, machineTitle(), cx, y + H / 2 + 8, 0xFFA8B2C6);
        final int barW = 120;
        final int bx = cx - barW / 2;
        final int by = y + H / 2 + 24;
        g.fill(bx, by, bx + barW, by + 3, 0xFF2A2D3E);
        final int fill = Math.min(barW, barW * ticks / postTicks);
        g.fill(bx, by, bx + fill, by + 3, accent);
        if (completed && bootingFrom().isEmpty()) {
            renderUefiNoBoot(g, x, y, text, dim);
            return;
        }
        /*
         * Along the foot: what the machine is made of where a board of this age prints it, and the two keys
         * at the other end, blinking as they do until one of them is pressed.
         */
        final int fy = y + H - 16;
        wall(g, buildSummary(), x + MARGIN, fy, dim);
        if (setupRequested) {
            wallRight(g, "Entering Setup ...", x + W - MARGIN, fy, accent);
        } else if ((ticks / 10) % 2 == 0) {
            int tx = x + W - MARGIN - wallWidth("DEL Setup   F12 Boot Menu");
            tx = hintRun(g, "DEL", tx, fy, text);
            tx = hintRun(g, " Setup   ", tx, fy, dim);
            tx = hintRun(g, "F12", tx, fy, text);
            hintRun(g, " Boot Menu", tx, fy, dim);
        }
    }

    /**
     * A modern machine with nothing to boot says so in a dialog that names every device and what is on it.
     *
     * <p>Two lines saying no device was found tell a player that something is wrong and nothing about what:
     * the disks are there, they are simply empty, and the one thing worth knowing is which of them is.
     */
    private void renderUefiNoBoot(final GuiGraphics g, final int x, final int y, final int text, final int dim) {
        final List<FirmwareStatePayload.Entry> entries = state == null ? List.of() : state.entries();
        final int boxW = 224;
        final int boxH = 16 + Math.max(1, entries.size()) * WALL_ROW + 22;
        final int bx = x + (W - boxW) / 2;
        final int by = y + (H - boxH) / 2;
        g.fill(bx, by, bx + boxW, by + boxH, 0xFF2A2D3E);
        g.fill(bx, by, bx + boxW, by + 14, 0xFF3A4060);
        g.fill(bx, by, bx + 2, by + 14, 0xFFF0B23A);
        wall(g, "No bootable device", bx + 7, by + 4, 0xFFF0B23A);
        int ly = by + 18;
        if (entries.isEmpty()) {
            wall(g, "No disk and no drive is attached to this computer.", bx + 8, ly, dim);
            ly += WALL_ROW;
        }
        int slot = 0;
        for (final FirmwareStatePayload.Entry entry : entries) {
            final String where = entry.kind() == FirmwareStatePayload.KIND_DISK
                    ? "Disk " + slot++ + " · " + entry.device() : entry.device();
            wall(g, where + ": " + entry.label(), bx + 8, ly, text);
            ly += WALL_ROW;
        }
        wall(g, "Insert installation media and press Enter, or DEL for Setup", bx + 8, ly + 4, dim);
    }
}
