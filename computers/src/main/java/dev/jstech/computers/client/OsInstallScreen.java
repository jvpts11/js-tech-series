/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.layout.OsInstallLayout;
import dev.jstech.computers.menu.MonitorSessionMenu;
import dev.jstech.computers.operation.payload.FirmwareActionPayload;
import dev.jstech.computers.operation.payload.RequestFirmwarePayload;
import dev.jstech.computers.os.FirmwareKind;
import dev.jstech.core.gui.Phosphor;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Installing a system, made visible. Before this, the firmware wrote the system and closed the
 * window, so a player saw the screen vanish and later found the machine already installed, and the
 * single act that should feel weighty had no moment at all.
 *
 * <p>Three beats: what is about to happen and where, the work itself, and the reboot that ends it.
 * The write only goes to the server when the progress finishes, so closing the window mid-way
 * cancels cleanly. Arch and Gentoo never come here, since their systems are put on the disk by hand
 * from a live shell, which is their whole point.
 */
@PaletteHolder
public final class OsInstallScreen extends AbstractComputerScreen<MonitorSessionMenu> {

    /* The installer in each firmware's look; the Vintage one is written in greys its phosphor lights. */
    private static final Palette<Look> CLI = Palettes.declare(JsComputers.MODID, "firmware/install_cli",
            new Look(0xFF020602, 0xFF141414, 0xFF3A3A3A, 0xFFBEBEBE, 0xFFE8E8E8, 0xFF6A6A6A, 0xFF63C363,
                    0xFF1E1E1E));
    private static final Palette<Look> BIOS = Palettes.declare(JsComputers.MODID, "firmware/install_bios",
            new Look(0xFF06217A, 0xFF0A2C9E, 0xFF6E8BE0, 0xFFD6DEF8, 0xFFFFFFFF, 0xFF9AA9DE, 0xFF9BE29B,
                    0xFF0A1E5E));
    private static final Palette<Look> UEFI = Palettes.declare(JsComputers.MODID, "firmware/install_uefi",
            new Look(0xFF10151B, 0xFF19212B, 0xFF39434F, 0xFFCDD6E2, 0xFF39D6C4, 0xFF7D8A9C, 0xFF5FE07A,
                    0xFF161D25));
    /** How long the bar is drawn over until the machine says otherwise, which is only the moment before it does. */
    private static final int WORK_TICKS = 70;

    /**
     * What this screen can be showing. There is no page that asks first: a machine is already installing by
     * the time anybody sees this, because the firmware sends the machine off and the machine answers with
     * which screen the player belongs on. A system with an installer of its own is not shown here at all.
     */
    private enum Phase { WORKING, DONE, FAILED }

    /** The steps the progress walks through, each claiming a quarter of the work; as many as
     *  {@link OsInstallLayout#STEP_COUNT} reserves rows for. */
    private static final TextKey[] STEPS = {
            InstallerScreenTexts.COPY_PREPARING,
            InstallerScreenTexts.COPY_COPYING,
            InstallerScreenTexts.COPY_FOLDERS,
            InstallerScreenTexts.COPY_BOOT_ENTRY,
    };

    /** The copy the machine last reported, kept until the session that shows it is built. */
    @Nullable
    private static Copying pending;

    static {
        // The layout reserves a row per step; a list of another length would draw past it or leave a row empty.
        if (STEPS.length != OsInstallLayout.STEP_COUNT) {
            throw new IllegalStateException("OsInstallScreen has " + STEPS.length + " steps, the layout has rows for "
                    + OsInstallLayout.STEP_COUNT);
        }
    }

    private final BlockPos computerPos;
    private final BlockPos monitorPos;
    private final FirmwareKind kind;
    private final String osName;
    private final String targetLabel;
    private final int targetSlot;

    private Phase phase = Phase.WORKING;
    /** Why the server refused the write, once it has; shown in place of the reboot prompt. */
    private String failure = "";
    private int ticks;
    /** How long the machine said its copy takes, once it has said so. */
    private int workTicks = WORK_TICKS;
    private int[] primary;
    private int[] secondary;

    public OsInstallScreen(final MonitorSessionMenu session, final Inventory inventory, final Component title) {
        super(session, inventory, title);
        this.imageWidth = OsInstallLayout.WIDTH;
        this.imageHeight = OsInstallLayout.HEIGHT;
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
        this.computerPos = session.hostPos();
        this.monitorPos = session.monitorPos();
        final Copying copying = pending != null ? pending
                : new Copying(FirmwareKind.forEra(HardwareEra.STANDARD), "", "", -1, Phase.WORKING, "",
                        WORK_TICKS, WORK_TICKS);
        this.kind = copying.kind();
        this.osName = copying.osName().isBlank() ? GameText.resolve(InstallerScreenTexts.COPY_INSTALLERS_SYSTEM)
                : copying.osName();
        this.targetLabel = copying.targetLabel().isBlank() ? GameText.resolve(InstallerScreenTexts.COPY_DEFAULT_DISK)
                : copying.targetLabel();
        this.targetSlot = copying.targetSlot();
        this.phase = copying.phase();
        this.failure = copying.failure();
        this.workTicks = Math.max(1, copying.ticksTotal());
        this.ticks = Math.max(0, copying.ticksTotal() - copying.ticksLeft());
    }

    /** The copy the machine is doing, said before the session that shows it is opened. */
    public static void expectWorking(final FirmwareKind kind, final String osName, final String targetLabel,
                                     final int ticksLeft, final int ticksTotal) {
        pending = new Copying(kind, osName, targetLabel, -1, Phase.WORKING, "", ticksLeft, ticksTotal);
    }

    /** The copy that finished and is waiting for the restart that boots it. */
    public static void expectDone(final FirmwareKind kind, final String osName, final String targetLabel,
                                  final int targetSlot) {
        pending = new Copying(kind, osName, targetLabel, targetSlot, Phase.DONE, "", 0, WORK_TICKS);
    }

    /** The copy the machine refused, and why, which the player has to hear instead of a false "complete". */
    public static void expectFailed(final FirmwareKind kind, final String osName, final String targetLabel,
                                    final String failure) {
        pending = new Copying(kind, osName, targetLabel, -1, Phase.FAILED, failure, 0, WORK_TICKS);
    }

    /**
     * Gives the beat just written down to the screen already showing that machine.
     *
     * <p>The copy and the word that ends it are the same session, so opening one does not tear the other down
     * and build it again: without this the screen would sit on the bar after the machine had finished.
     */
    public static void refreshOpen(final BlockPos hostPos) {
        if (Minecraft.getInstance().screen instanceof OsInstallScreen open
                && open.computerPos.equals(hostPos) && pending != null) {
            open.adopt(pending);
        }
    }

    /** Takes that beat: which page it is on, why it was refused, and how far along the copy is. */
    private void adopt(final Copying copying) {
        this.phase = copying.phase();
        this.failure = copying.failure();
        this.workTicks = Math.max(1, copying.ticksTotal());
        this.ticks = Math.max(0, copying.ticksTotal() - copying.ticksLeft());
    }

    /** One copy: what is being written where, which beat it is on, and how far along. */
    private record Copying(FirmwareKind kind, String osName, String targetLabel, int targetSlot, Phase phase,
                           String failure, int ticksLeft, int ticksTotal) {

        Copying {
            osName = osName == null ? "" : osName;
            targetLabel = targetLabel == null ? "" : targetLabel;
            failure = failure == null ? "" : failure;
        }
    }

    /** The machine's own generation, so this wears the skin that machine's screens wear. */
    @Override
    @Nullable
    protected HardwareEra screenEra() {
        return this.getMenu().hardwareEra();
    }

    private int left() {
        return this.leftPos;
    }

    private int top() {
        return this.topPos;
    }

    /** Progress through the write, 0..1000. */
    private int permille() {
        return Math.min(1000, ticks * 1000 / workTicks);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        /*
         * The copy is the machine's, so this only follows it: the bar walks to the end and waits there, and the
         * machine is what writes the system and puts this screen on its next beat. Walking away no longer throws
         * the work out.
         */
        if (phase == Phase.WORKING && ticks < workTicks) {
            ticks++;
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (phase == Phase.DONE && in(primary, mouseX, mouseY)) {
            // Reboot into what was just installed: set it as the boot disk and let the machine come up.
            PacketDistributor.sendToServer(FirmwareActionPayload.of(computerPos, monitorPos,
                    FirmwareActionPayload.ACTION_BOOT_DISK, targetSlot, -1));
            return true;
        }
        if (phase == Phase.DONE && in(secondary, mouseX, mouseY)) {
            onClose();
            return true;
        }
        if (phase == Phase.FAILED && in(primary, mouseX, mouseY)) {
            // Back to the firmware, where the medium's row says what this machine can and cannot install.
            PacketDistributor.sendToServer(new RequestFirmwarePayload(computerPos, monitorPos));
            return true;
        }
        if (phase == Phase.FAILED && in(secondary, mouseX, mouseY)) {
            onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean in(final int[] r, final double mx, final double my) {
        return r != null && mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3];
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = left();
        final int y = top();
        final Look p = Look.of(kind);

        g.fill(x, y, x + OsInstallLayout.WIDTH, y + OsInstallLayout.HEIGHT, p.back);
        g.fill(x, y, x + OsInstallLayout.WIDTH, y + 1, p.edge);
        g.fill(x, y + OsInstallLayout.HEIGHT - 1, x + OsInstallLayout.WIDTH, y + OsInstallLayout.HEIGHT, p.edge);
        g.fill(x, y, x + 1, y + OsInstallLayout.HEIGHT, p.edge);
        g.fill(x + OsInstallLayout.WIDTH - 1, y, x + OsInstallLayout.WIDTH, y + OsInstallLayout.HEIGHT, p.edge);
        g.fill(x + 1, y + 1, x + OsInstallLayout.WIDTH - 1, y + 1 + OsInstallLayout.TITLE_BAR_H, p.bar);
        final String title = GameText.resolve(switch (phase) {
            case WORKING -> InstallerScreenTexts.COPY_INSTALLING.with(osName.toUpperCase(Locale.ROOT));
            case DONE -> InstallerScreenTexts.COPY_COMPLETE.text();
            case FAILED -> InstallerScreenTexts.COPY_FAILED.text();
        });
        g.drawString(font, title, x + OsInstallLayout.TITLE_X, y + OsInstallLayout.TITLE_Y, p.bright, false);

        int ty = y + OsInstallLayout.CONTENT_TOP;
        switch (phase) {
            case WORKING -> {
                final int done = permille() * OsInstallLayout.STEP_COUNT / 1000;
                for (int i = 0; i < OsInstallLayout.STEP_COUNT; i++) {
                    final boolean finished = i < done;
                    final boolean current = i == done;
                    g.drawString(font, GameText.resolve(InstallerScreenTexts.COPY_STEP.with(STEPS[i])),
                            x + OsInstallLayout.STEP_LABEL_X, ty, finished || current ? p.text : p.dim, false);
                    if (finished) {
                        g.drawString(font, GameText.resolve(InstallerScreenTexts.COPY_DONE),
                                x + OsInstallLayout.stepStatusX(), ty, p.ok, false);
                    } else if (current) {
                        g.drawString(font, (permille() % 1000) / 10 + "%", x + OsInstallLayout.stepStatusX(), ty,
                                p.bright, false);
                    }
                    ty += OsInstallLayout.STEP_ROW_H;
                }
                ty += OsInstallLayout.AFTER_STEPS_GAP;
                g.fill(x + OsInstallLayout.BAR_X, ty, x + OsInstallLayout.BAR_X + OsInstallLayout.barW(),
                        ty + OsInstallLayout.BAR_H, p.trackBg);
                g.fill(x + OsInstallLayout.BAR_X, ty,
                        x + OsInstallLayout.BAR_X + OsInstallLayout.barW() * permille() / 1000,
                        ty + OsInstallLayout.BAR_H, p.bright);
                g.drawString(font, GameText.resolve(InstallerScreenTexts.COPY_KEEP_MEDIUM), x + OsInstallLayout.BAR_X,
                        ty + OsInstallLayout.BAR_TO_HINT_DY, p.dim, false);
            }
            case DONE -> {
                g.drawString(font, GameText.resolve(InstallerScreenTexts.COPY_INSTALLED_ON.with(osName, targetLabel)),
                        x + OsInstallLayout.STEP_LABEL_X, ty, p.text, false);
                g.drawString(font, GameText.resolve(InstallerScreenTexts.COPY_TAKE_OUT_FIRST),
                        x + OsInstallLayout.STEP_LABEL_X, ty + OsInstallLayout.LINE2_DY, p.dim, false);
                g.drawString(font, GameText.resolve(InstallerScreenTexts.COPY_TAKE_OUT_SECOND),
                        x + OsInstallLayout.STEP_LABEL_X, ty + OsInstallLayout.LINE3_DY, p.dim, false);
                primary = button(g, x + OsInstallLayout.DONE_PRIMARY_X, y + OsInstallLayout.buttonY(),
                        OsInstallLayout.DONE_PRIMARY_W, OsInstallLayout.BUTTON_H,
                        GameText.resolve(InstallerScreenTexts.COPY_REBOOT), p, true, mouseX, mouseY);
                secondary = button(g, x + OsInstallLayout.DONE_SECONDARY_X, y + OsInstallLayout.buttonY(),
                        OsInstallLayout.DONE_SECONDARY_W, OsInstallLayout.BUTTON_H,
                        GameText.resolve(InstallerScreenTexts.COPY_BACK_TO_SETUP), p, false, mouseX, mouseY);
            }
            case FAILED -> {
                g.drawString(font, GameText.resolve(InstallerScreenTexts.COPY_NOTHING_WRITTEN.with(targetLabel)),
                        x + OsInstallLayout.STEP_LABEL_X, ty, p.dim, false);
                int ly = ty + OsInstallLayout.LINE2_DY;
                for (final String line : failureLines()) {
                    g.drawString(font, line, x + OsInstallLayout.STEP_LABEL_X, ly, p.text, false);
                    ly += OsInstallLayout.FAILURE_WRAP_LINE_H;
                }
                primary = button(g, x + OsInstallLayout.FAILED_PRIMARY_X, y + OsInstallLayout.buttonY(),
                        OsInstallLayout.FAILED_PRIMARY_W, OsInstallLayout.BUTTON_H,
                        GameText.resolve(InstallerScreenTexts.COPY_BACK_TO_SETUP), p, true, mouseX, mouseY);
                secondary = button(g, x + OsInstallLayout.FAILED_SECONDARY_X, y + OsInstallLayout.buttonY(),
                        OsInstallLayout.FAILED_SECONDARY_W, OsInstallLayout.BUTTON_H,
                        GameText.resolve(InstallerScreenTexts.COPY_CLOSE), p, false, mouseX, mouseY);
            }
        }
    }

    /**
     * The failure reason wrapped to the dialog's width, capped at as many lines as fit above the button row; a
     * reason longer than that loses its tail, the last line shown ending in dots instead of running under the
     * buttons.
     */
    private List<String> failureLines() {
        final int width = OsInstallLayout.FAILURE_WRAP_W;
        final List<FormattedText> wrapped = font.getSplitter().splitLines(failure, width, Style.EMPTY);
        final int shown = Math.min(wrapped.size(), OsInstallLayout.mostFailureLines());
        final List<String> lines = new ArrayList<>(shown);
        for (int i = 0; i < shown; i++) {
            String line = wrapped.get(i).getString();
            if (i == shown - 1 && wrapped.size() > shown) {
                while (!line.isEmpty() && font.width(line + "...") > width) {
                    line = line.substring(0, line.length() - 1);
                }
                line = line + "...";
            }
            lines.add(line);
        }
        return lines;
    }

    private void row(final GuiGraphics g, final int x, final int y, final Look p,
                     final String key, final String value) {
        g.drawString(font, key, x + 10, y, p.dim, false);
        g.drawString(font, value, x + 90, y, p.text, false);
    }

    private int[] button(final GuiGraphics g, final int bx, final int by, final int bw, final int bh,
                         final String label, final Look p, final boolean strong,
                         final int mouseX, final int mouseY) {
        final boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
        g.fill(bx, by, bx + bw, by + bh, hovered ? p.bar : p.back);
        final int edge = strong ? p.bright : p.edge;
        g.fill(bx, by, bx + bw, by + 1, edge);
        g.fill(bx, by + bh - 1, bx + bw, by + bh, edge);
        g.fill(bx, by, bx + 1, by + bh, edge);
        g.fill(bx + bw - 1, by, bx + bw, by + bh, edge);
        g.drawString(font, label, bx + (bw - font.width(label)) / 2, by + (bh - 8) / 2,
                strong ? p.bright : p.text, false);
        return new int[]{bx, by, bw, bh};
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static int green(final int color) {
        return Phosphor.green(color);
    }

    /**
     * The three firmware looks, so the installer matches the machine it is installing onto: its ground, the bar of
     * a hovered or chosen row, the edges, the text and the bright text, the quiet lines, what went right, and the
     * progress bar's track.
     */
    private record Look(int back, int bar, int edge, int text, int bright, int dim, int ok, int trackBg) {

        static Look of(final FirmwareKind kind) {
            return switch (kind) {
                /*
                 * The Vintage tube is monochrome: every one of these but the ground is the phosphor, lit to the
                 * brightness the grey in its palette reads at.
                 */
                case CLI_BIOS -> {
                    final Look grey = CLI.get();
                    yield new Look(grey.back(), green(grey.bar()), green(grey.edge()), green(grey.text()),
                            green(grey.bright()), green(grey.dim()), green(grey.ok()), green(grey.trackBg()));
                }
                case BLUE_BIOS -> BIOS.get();
                case UEFI -> UEFI.get();
            };
        }
    }
}
