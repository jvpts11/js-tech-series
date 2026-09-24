/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestWelcomePayload;
import dev.jstech.computers.operation.payload.WelcomePayload;
import dev.jstech.computers.operation.payload.WelcomeStartupPayload;
import dev.jstech.computers.os.boot.WelcomeFacts;
import dev.jstech.core.text.GameText;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * What a system says the first time it comes up: what this computer is, and where to go from here.
 *
 * <p>It asks nothing. The installer already asked the only questions a machine has to be asked, so the welcome
 * is an account of the machine and a handful of doors: every line is read off the computer it is running on, and
 * every button opens a program that is really on it, under the name this desktop gives it.
 *
 * <p>Three editions, three shapes, each the way it was done: a tip that changes with a column of doors beside
 * it, a pane of doors with the machine written out next to them, and four cards. The skin gives them their
 * colours; the shape is what tells them apart.
 */
public final class WelcomeApp implements IDesktopApp {

    /** What the machine calls this window when it asks for it, and what closes it again. */
    public static final String KEY = WelcomeFacts.WINDOW_KEY;

    /** How wide the column of doors down the side of the oldest shape is. */
    private static final int DOOR_COLUMN = 84;

    /** The height of a button, and of a row of facts. */
    private static final int ROW = 13;

    private static final int PAD = 8;

    /** The welcome's own picture: the lamp of the tips on the older editions, the star on the newest. */
    private static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("jsc", "welcome");

    /** The windows waiting on an answer, so a machine's reply reaches the window that asked it. */
    private static final List<WelcomeApp> OPEN = new ArrayList<>();

    private final BlockPos host;
    private final List<Door> doors = new ArrayList<>();

    private OsSkin skin = OsSkin.fallback();
    private WelcomePayload facts;
    private int tip;
    private int[] closeButton;
    private int[] nextTipButton;
    private int[] startupBox;

    public WelcomeApp(final BlockPos host) {
        this.host = host;
        OPEN.add(this);
        PacketDistributor.sendToServer(new RequestWelcomePayload(host));
    }

    /** Hands a machine's answer to the windows waiting on it. */
    public static void accept(final WelcomePayload payload) {
        for (final WelcomeApp app : OPEN) {
            if (app.host.equals(payload.hostPos())) {
                app.facts = payload;
            }
        }
    }

    @Override
    public String title() {
        if (this.skin.form() == OsSkin.Form.FLAT) {
            return GameText.resolve(WelcomeTexts.TITLE_GET_STARTED);
        }
        return this.facts == null || this.facts.systemName().isEmpty()
                ? GameText.resolve(WelcomeTexts.TITLE)
                : GameText.resolve(WelcomeTexts.WELCOME_TO.with(this.facts.systemName()));
    }

    @Override
    public ResourceLocation iconId() {
        return ICON;
    }

    @Override
    public int defaultWidth() {
        return 250;
    }

    @Override
    public int defaultHeight() {
        return 136;
    }

    @Override
    public int minWidth() {
        return 200;
    }

    @Override
    public int minHeight() {
        return 110;
    }

    @Override
    public void applySkin(final OsSkin applied) {
        this.skin = applied;
    }

    @Override
    public void onClosed() {
        OPEN.remove(this);
    }

    @Override
    public void onRestored() {
        OPEN.add(this);
        PacketDistributor.sendToServer(new RequestWelcomePayload(this.host));
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        this.doors.clear();
        this.closeButton = null;
        this.nextTipButton = null;
        this.startupBox = null;
        g.fill(x, y, x + width, y + height, this.skin.windowBg());
        if (this.facts == null) {
            g.drawString(font, GameText.resolve(WelcomeTexts.GETTING_READY), x + PAD, y + PAD, this.skin.dim(), false);
            return;
        }
        switch (this.skin.form()) {
            case LUNA -> this.renderPaned(g, font, x, y, width, height);
            case FLAT -> this.renderCards(g, font, x, y, width, height);
            default -> this.renderTips(g, font, x, y, width, height);
        }
        this.renderFooter(g, font, x, y, width, height);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (this.facts == null) {
            return;
        }
        if (hit(this.startupBox, mouseX, mouseY)) {
            this.setShowAtStartup(!this.facts.showAtStartup());
            return;
        }
        if (hit(this.nextTipButton, mouseX, mouseY) && !this.facts.tips().isEmpty()) {
            this.tip = (this.tip + 1) % this.facts.tips().size();
            return;
        }
        for (final Door door : this.doors) {
            if (hit(door.box(), mouseX, mouseY)) {
                DesktopScreen.openProgramById(door.program());
                return;
            }
        }
        if (hit(this.closeButton, mouseX, mouseY)) {
            DesktopScreen.requestClose(KEY);
        }
    }

    /** The one thing a welcome remembers, told to the machine that keeps it. */
    private void setShowAtStartup(final boolean show) {
        this.facts = new WelcomePayload(this.facts.hostPos(), this.facts.machineName(), this.facts.cpuName(),
                this.facts.memoryMb(), this.facts.systemName(), this.facts.systemSlot(), this.facts.systemDisk(),
                this.facts.others(), this.facts.networkHost(), this.facts.mirrorHost(), show, this.facts.tips());
        PacketDistributor.sendToServer(new WelcomeStartupPayload(this.host, show));
    }

    /** The oldest shape: a greeting, a tip that changes, and a column of doors beside it. */
    private void renderTips(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                            final int h) {
        final int right = x + w - PAD - DOOR_COLUMN;
        g.drawString(font, GameText.resolve(WelcomeTexts.WELCOME_TO.with(this.facts.systemName())), x + PAD, y + PAD,
                this.skin.text(), false);

        final int boxY = y + PAD + 14;
        final int boxW = right - x - PAD * 2;
        final int boxH = h - PAD * 2 - 14 - ROW;
        this.skin.field(g, x + PAD, boxY, boxW, boxH, false);
        g.drawString(font, GameText.resolve(WelcomeTexts.DID_YOU_KNOW), x + PAD + 4, boxY + 4, this.skin.text(),
                false);
        int ty = boxY + 16;
        if (!this.facts.tips().isEmpty()) {
            final String said = GameText.resolve(this.facts.tips().get(this.tip % this.facts.tips().size()));
            for (final FormattedCharSequence line : font.split(Component.literal(said), boxW - 8)) {
                if (ty > boxY + boxH - 10) {
                    break;
                }
                g.drawString(font, line, x + PAD + 4, ty, this.skin.dim(), false);
                ty += 10;
            }
        }

        int by = y + PAD;
        by = this.door(g, font, right, by, DOOR_COLUMN, "this_pc");
        by = this.door(g, font, right, by, DOOR_COLUMN, "files");
        by = this.door(g, font, right, by, DOOR_COLUMN, "command_prompt");
        this.nextTipButton = this.button(g, font, right, by + 3, DOOR_COLUMN, GameText.resolve(WelcomeTexts.NEXT_TIP));
    }

    /** The middle shape: a pane of doors, and the machine itself written out beside them. */
    private void renderPaned(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                             final int h) {
        final int paneW = 88;
        this.skin.panel(g, x, y, paneW, h - ROW - 4);
        g.drawString(font, clip(font, GameText.resolve(WelcomeTexts.GET_GOING), paneW - 10), x + 5, y + 5,
                this.skin.accent(), false);
        int by = y + 18;
        by = this.door(g, font, x + 4, by, paneW - 8, "this_pc");
        by = this.door(g, font, x + 4, by, paneW - 8, "files");
        by = this.door(g, font, x + 4, by, paneW - 8, "command_prompt");
        this.door(g, font, x + 4, by, paneW - 8, "settings");

        final int fx = x + paneW + PAD;
        g.drawString(font, GameText.resolve(WelcomeTexts.IS_READY.with(this.facts.machineName())), fx, y + PAD,
                this.skin.accent(), false);
        this.machineFacts(g, font, fx, y + PAD + 16, x + w - fx - PAD);
    }

    /** The newest shape: one card per door, each saying what is true of this computer. */
    private void renderCards(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                             final int h) {
        g.drawString(font, GameText.resolve(WelcomeTexts.IS_READY.with(this.facts.machineName())), x + PAD, y + PAD,
                this.skin.text(), false);
        g.drawString(font, GameText.resolve(WelcomeTexts.WHAT_IT_HAS), x + PAD, y + PAD + 11, this.skin.dim(),
                false);
        final int cardW = (w - PAD * 2 - 4) / 2;
        final int cardH = Math.max(24, (h - PAD * 2 - 30 - ROW) / 2);
        final int top = y + PAD + 26;
        this.card(g, font, x + PAD, top, cardW, cardH, "this_pc", this.hardwareLine());
        this.card(g, font, x + PAD + cardW + 4, top, cardW, cardH, "command_prompt", this.softwareLine());
        this.card(g, font, x + PAD, top + cardH + 4, cardW, cardH, "network", this.networkLine());
        this.card(g, font, x + PAD + cardW + 4, top + cardH + 4, cardW, cardH, "files",
                GameText.resolve(WelcomeTexts.FOLDERS_ON_DISK.with(this.facts.systemSlot())));
    }

    /** The machine written out as a list of facts, which is the middle shape's way of saying it. */
    private void machineFacts(final GuiGraphics g, final Font font, final int x, final int y, final int w) {
        int ty = y;
        ty = this.fact(g, font, x, ty, w, GameText.resolve(WelcomeTexts.COMPUTER_NAME), this.facts.machineName());
        ty = this.fact(g, font, x, ty, w, GameText.resolve(WelcomeTexts.PROCESSOR),
                GameText.resolve(this.facts.cpuName()));
        ty = this.fact(g, font, x, ty, w, GameText.resolve(WelcomeTexts.MEMORY),
                GameText.resolve(WelcomeTexts.MEGABYTES.with(this.facts.memoryMb())));
        ty = this.fact(g, font, x, ty, w, this.facts.systemName(),
                GameText.resolve(WelcomeTexts.ON_DISK.with(this.facts.systemSlot(), this.facts.systemDisk())));
        if (!this.facts.others().isEmpty()) {
            final WelcomeFacts.Other other = this.facts.others().getFirst();
            ty = this.fact(g, font, x, ty, w, GameText.resolve(WelcomeTexts.ALSO_INSTALLED),
                    GameText.resolve(WelcomeTexts.OTHER_SYSTEM.with(other.system(), other.slot())));
        }
        this.fact(g, font, x, ty, w, GameText.resolve(WelcomeTexts.NETWORK), this.networkLine());
    }

    private int fact(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                     final String key, final String value) {
        final int keyW = Math.min(76, w / 2);
        g.drawString(font, clip(font, key, keyW - 4), x, y, this.skin.dim(), false);
        g.drawString(font, clip(font, value, w - keyW), x + keyW, y, this.skin.text(), false);
        return y + 11;
    }

    private void card(final GuiGraphics g, final Font font, final int x, final int y, final int w, final int h,
                      final String program, final String body) {
        this.skin.panel(g, x, y, w, h);
        final String name = label(program);
        g.drawString(font, name, x + 4, y + 3, this.skin.text(), false);
        int ty = y + 13;
        for (final FormattedCharSequence line : font.split(Component.literal(body), w - 8)) {
            if (ty > y + h - 8) {
                break;
            }
            g.drawString(font, line, x + 4, ty, this.skin.dim(), false);
            ty += 9;
        }
        this.doors.add(new Door(program, new int[]{x, y, w, h}));
    }

    /** The checkbox and the way out, which every shape puts along the bottom. */
    private void renderFooter(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h) {
        final int by = y + h - ROW;
        this.startupBox = new int[]{x + PAD, by, 9, 9};
        this.skin.field(g, x + PAD, by, 9, 9, false);
        if (this.facts.showAtStartup()) {
            g.drawString(font, "x", x + PAD + 2, by, this.skin.accent(), false);
        }
        g.drawString(font, GameText.resolve(WelcomeTexts.SHOW_AT_STARTUP), x + PAD + 13, by, this.skin.dim(), false);
        this.closeButton = this.button(g, font, x + w - PAD - 46, by - 3, 46, GameText.resolve(WelcomeTexts.CLOSE));
    }

    /** A door to one of this machine's programs, under the name this desktop gives it. */
    private int door(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                     final String program) {
        this.doors.add(new Door(program, this.button(g, font, x, y, w, label(program))));
        return y + ROW + 2;
    }

    private int[] button(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                         final String label) {
        this.skin.button(g, font, x, y, w, ROW, clip(font, label, w - 6), false, false, false);
        return new int[]{x, y, w, ROW};
    }

    /** What this computer is made of, in one line, for the card that opens This PC. */
    private String hardwareLine() {
        return GameText.resolve(WelcomeTexts.HARDWARE.with(this.facts.cpuName(), this.facts.memoryMb(),
                this.facts.systemName(), this.facts.systemSlot()));
    }

    /** Where software comes from on this machine, which is not the same answer on every machine. */
    private String softwareLine() {
        return GameText.resolve(this.facts.mirrorAnswers()
                ? WelcomeTexts.MIRROR_ANSWERS.with(this.facts.mirrorHost()) : WelcomeTexts.NO_MIRROR.text());
    }

    /** What this computer is connected to, said plainly, including when the answer is nothing. */
    private String networkLine() {
        if (!this.facts.networked()) {
            return GameText.resolve(WelcomeTexts.NOT_CABLED);
        }
        return GameText.resolve((this.facts.mirrorAnswers() ? WelcomeTexts.CABLED_WITH_MIRROR : WelcomeTexts.CABLED)
                .with(this.facts.networkHost()));
    }

    /** The name this desktop gives that program, falling back to the id when it has none. */
    private static String label(final String program) {
        final String named = DesktopScreen.programLabel(program);
        return named.isEmpty() ? program : named;
    }

    /** A label cut to the room it has, so a long name never runs out of its button. */
    private static String clip(final Font font, final String text, final int room) {
        if (font.width(text) <= room) {
            return text;
        }
        String cut = text;
        while (cut.length() > 1 && font.width(cut + "...") > room) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "...";
    }

    private static boolean hit(final int[] rect, final double mouseX, final double mouseY) {
        return rect != null && mouseX >= rect[0] && mouseX < rect[0] + rect[2]
                && mouseY >= rect[1] && mouseY < rect[1] + rect[3];
    }

    /**
     * A way out of the welcome into a program that is really on this machine.
     *
     * @param program the program's id path, as the desktop knows it
     * @param box     where its door was drawn on the last frame
     */
    private record Door(String program, int[] box) {
    }
}
