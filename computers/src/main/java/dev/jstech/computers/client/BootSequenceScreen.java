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
import dev.jstech.computers.os.Branding;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.core.gui.Phosphor;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The power-on self-test: what the monitor shows between switching the computer on (or a reboot) and the
 * boot target opening. Lines appear over a few seconds in the firmware's era style (memory count, detected
 * drives, the boot device) and DEL at any point enters the firmware setup instead. When the sequence ends
 * the client reports {@link PostCompletePayload} and the server swaps this screen for whatever boots.
 */
public final class BootSequenceScreen extends Screen {

    private static final int W = 340;
    private static final int H = 214;

    /** How long a self-test is drawn for when the machine did not say, which only a stale packet leaves. */
    private static final int FALLBACK_TICKS = 70;
    /** Safety: if the server never swaps the screen (nothing could open), close on our own. */
    private static final int GRACE_TICKS = 60;

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

    public BootSequenceScreen(final BlockPos computerPos, final BlockPos monitorPos, final FirmwareKind kind,
                              final String machineName, final int remainingTicks) {
        super(Component.literal("Power-On Self-Test"));
        this.computerPos = computerPos;
        this.monitorPos = monitorPos;
        this.kind = kind;
        this.machineName = machineName;
        this.postTicks = remainingTicks > 0 ? remainingTicks : FALLBACK_TICKS;
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
    public void tick() {
        super.tick();
        ticks++;
        /*
         * The machine is the one that ends its self-test and puts whoever is watching in front of what boots,
         * so this waits rather than reporting. All the screen owes is a way out if nothing ever comes: a
         * machine switched off mid-test leaves this here with nothing else to show.
         */
        if (!completed && ticks >= postTicks) {
            completed = true;
        }
        if (ticks >= postTicks + GRACE_TICKS && Minecraft.getInstance().screen == this) {
            onClose();
        }
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
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        /*
         * Screen.render paints the dimmed backdrop itself; painting it again after our own drawing would
         * wash the whole POST out (the double-background bug), so super runs FIRST and the content after.
         */
        super.render(g, mouseX, mouseY, partialTick);
        final int x = (width - W) / 2;
        final int y = (height - H) / 2;
        MonitorFrame.renderBody(g, x, y, W, H, era(), font);

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
            case BLUE_BIOS -> 0xFFE8E8E8;
            case UEFI -> 0xFFE6ECF6;
        };
        final int dim = switch (kind) {
            case CLI_BIOS -> Phosphor.green(0xFF707070);
            case BLUE_BIOS -> 0xFFB9C4D6;
            case UEFI -> 0xFF8090A8;
        };
        final int accent = switch (kind) {
            case CLI_BIOS -> Phosphor.green(0xFFE8E8E8);
            case BLUE_BIOS -> 0xFFFFE14D;
            case UEFI -> 0xFF5FA8D3;
        };
        g.fill(x, y, x + W, y + H, bg);

        if (kind == FirmwareKind.UEFI) {
            renderUefi(g, x, y, text, dim, accent);
        } else {
            renderClassic(g, x, y, text, dim, accent);
        }
        if (kind == FirmwareKind.BLUE_BIOS) {
            renderMakerBadge(g, x, y, accent, dim);
        }
        if (bootMenu) {
            renderBootMenu(g, x, y, text, dim, accent);
        }
    }

    /**
     * The one-time boot menu, drawn over the self-test the way a firmware's own does.
     *
     * <p>Whatever is picked here is for this boot and no other: the order saved in setup is not touched, which
     * is the whole point of having it apart from the setup.
     */
    private void renderBootMenu(final GuiGraphics g, final int x, final int y, final int text, final int dim,
                                final int accent) {
        final List<Choice> choices = bootable();
        final int boxW = 200;
        final int boxH = 30 + choices.size() * 11;
        final int bx = x + (W - boxW) / 2;
        final int by = y + (H - boxH) / 2;
        g.fill(bx, by, bx + boxW, by + boxH, 0xFF000060);
        g.fill(bx, by, bx + boxW, by + 1, accent);
        g.fill(bx, by + boxH - 1, bx + boxW, by + boxH, accent);
        g.drawString(font, "Boot Menu  (this boot only)", bx + 6, by + 6, accent, false);
        int ly = by + 20;
        for (int i = 0; i < choices.size(); i++) {
            final Choice choice = choices.get(i);
            final boolean on = i == menuAt;
            final String where = choice.slot() >= 0 ? "Disk " + choice.slot() : "Drive";
            g.drawString(font, (on ? "> " : "  ") + where + "    " + choice.entry().label(), bx + 6, ly,
                    on ? accent : text, false);
            ly += 11;
        }
        g.drawString(font, "Up/Down: Select     Enter: Boot", bx + 6, by + boxH - 11, dim, false);
    }

    /** The maker's badge in the top right, where a board of that age printed its firmware house's mark. */
    private void renderMakerBadge(final GuiGraphics g, final int x, final int y, final int accent, final int dim) {
        final String house = Branding.HARDWARE_HOUSE.toUpperCase(Locale.ROOT);
        final int space = house.indexOf(' ');
        final String top = space < 0 ? house : house.substring(0, space);
        final String rest = space < 0 ? "" : house.substring(space + 1);
        g.drawString(font, top, x + W - 10 - font.width(top), y + 10, accent, false);
        if (!rest.isEmpty()) {
            g.drawString(font, rest, x + W - 10 - font.width(rest), y + 20, dim, false);
        }
    }

    /** The classic POST wall of text: BIOS banner, memory count, drive detection, boot line. */
    private void renderClassic(final GuiGraphics g, final int x, final int y, final int text, final int dim,
                               final int accent) {
        final List<String[]> lines = classicLines();
        int ty = y + 10;
        for (int i = 0; i < lines.size(); i++) {
            if (ticks < 4 + i * 4) {
                break; // lines appear one by one, like a machine actually finding its hardware
            }
            final String[] line = lines.get(i);
            g.drawString(font, line[0], x + 10, ty, "1".equals(line[1]) ? accent : text, false);
            ty += 10;
        }
        // The setup hint blinks at the bottom for the whole sequence.
        if ((ticks / 10) % 2 == 0 && !setupRequested) {
            g.drawString(font, "DEL  Setup      F12  Boot Menu", x + 10, y + H - 14, dim, false);
        }
        if (setupRequested) {
            g.drawString(font, "Entering SETUP ...", x + 10, y + H - 14, accent, false);
        }
    }

    private List<String[]> classicLines() {
        final List<String[]> out = new ArrayList<>();
        out.add(new String[]{Branding.biosBanner(era()) + " - " + machineName, "1"});
        out.add(new String[]{Branding.firmwareCopyright(era()), "0"});
        out.add(new String[]{"", "0"});
        if (state != null) {
            final FirmwareStatePayload.Machine machine = state.machine();
            /*
             * The processor by its own model, then what it is: a self-test reads out the machine it found, and
             * the model is the part of it a player recognises.
             */
            out.add(new String[]{"Main Processor : "
                    + (machine.cpuName().isEmpty() ? "not detected" : machine.cpuName()), "0"});
            if (machine.hasCpu()) {
                out.add(new String[]{"                 " + machine.cores()
                        + (machine.cores() == 1 ? " core   " : " cores  ")
                        + machine.cpuMhz() + " MHz   " + machine.cpuArch(), "0"});
            }
            // The memory test counts up while the POST runs, settling on the installed total.
            final long total = (long) machine.ramMb() * 1024L;
            final long counted = Math.min(total, total * Math.max(0, ticks - 12) / 28L);
            out.add(new String[]{"Memory Test : " + String.format(Locale.ROOT, "%,d", counted) + " KB OK", "0"});
            out.add(new String[]{"", "0"});
            out.add(new String[]{"Detecting drives ...", "0"});
            int slot = 0;
            for (final FirmwareStatePayload.Entry entry : state.entries()) {
                final String role = entry.kind() == 0 ? ("  Disk " + slot++) : "  Media";
                out.add(new String[]{role + " : " + entry.label()
                        + (entry.detail().isEmpty() ? "" : " (" + entry.detail() + ")"), "0"});
            }
            out.add(new String[]{"", "0"});
            if (completed) {
                /*
                 * What it is about to boot, by name, or the era's own way of saying there is nothing: a machine
                 * of this age told you which drive it was reaching for, and which one it had given up on.
                 */
                final String booting = bootingFrom();
                if (!booting.isEmpty()) {
                    out.add(new String[]{"Booting from " + booting + " ...", "1"});
                } else {
                    for (final String line : noBootLines()) {
                        out.add(new String[]{line, "1"});
                    }
                }
            }
        } else {
            out.add(new String[]{"Reading system configuration ...", "0"});
        }
        return out;
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
        return m.cpuName() + "  -  " + m.cores() + (m.cores() == 1 ? " core" : " cores")
                + "  -  " + memory + "  -  " + m.cpuArch();
    }

    /** The modern machines hide the wall of text: logo line, a progress bar, the setup hint. */
    private void renderUefi(final GuiGraphics g, final int x, final int y, final int text, final int dim,
                            final int accent) {
        /*
         * The maker's name, then the machine's own, then what it is made of on one line. No screen inside the
         * fiction names the mod, and the machine that is in front of the player is the one it names.
         */
        g.drawCenteredString(font, Branding.HARDWARE_HOUSE.toUpperCase(Locale.ROOT), x + W / 2,
                y + H / 2 - 30, dim);
        g.drawCenteredString(font, machineTitle(), x + W / 2, y + H / 2 - 18, text);
        g.drawCenteredString(font, buildSummary(), x + W / 2, y + H / 2 - 6, dim);
        // Progress: a thin bar filling across the POST duration.
        final int barW = 120;
        final int bx = x + (W - barW) / 2;
        final int by = y + H / 2 + 8;
        g.fill(bx, by, bx + barW, by + 3, 0xFF2A2D3E);
        final int fill = Math.min(barW, barW * ticks / postTicks);
        g.fill(bx, by, bx + fill, by + 3, accent);
        if (completed && bootingFrom().isEmpty()) {
            int ly = y + H / 2 + 20;
            for (final String line : noBootLines()) {
                g.drawCenteredString(font, line, x + W / 2, ly, text);
                ly += 11;
            }
            return;
        }
        if (setupRequested) {
            g.drawCenteredString(font, "Entering Setup ...", x + W / 2, y + H - 18, accent);
        } else if ((ticks / 10) % 2 == 0) {
            g.drawCenteredString(font, "DEL  Setup      F12  Boot Menu", x + W / 2, y + H - 18, dim);
        }
    }

    /** The host computer's hardware era, so the monitor bezel matches the machine. */
    private HardwareEra era() {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.level != null && mc.level.getBlockEntity(computerPos)
                instanceof IOsHost be) {
            return be.displayEra();
        }
        return HardwareEra.STANDARD;
    }
}
