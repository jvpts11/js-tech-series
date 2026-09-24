/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.gui.CdePalette;
import dev.jstech.computers.gui.layout.CdeFrontPanelLayout.Rect;
import dev.jstech.computers.gui.layout.WorkstationInfoLayout;
import dev.jstech.computers.operation.payload.RequestWorkstationInfoPayload;
import dev.jstech.computers.operation.payload.WorkstationInfoPayload;
import dev.jstech.computers.os.WorkstationFacts;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * CDE's Workstation Info: what this workstation is, who is at it, the system under the desktop and the hardware
 * the system runs on, each group in a well of its own.
 *
 * <p>Every figure is the machine's, asked for when the window opens and again every few seconds while it stays
 * open, so the memory in use, with the meter beside it, is always current.
 */
@PaletteHolder
public final class WorkstationInfoApp implements IDesktopApp {

    private final BlockPos host;

    /** What the machine last said it is, or null until it has answered. */
    @Nullable
    private WorkstationFacts facts;
    private long askedAt;

    private OsSkin skin;
    private int left;
    private int top;

    /** How often an open window asks again, which is what keeps its memory figure current. */
    private static final long ASK_EVERY_MS = 2_000L;

    /** This window's own colours, {@code jsc:app/workstation_info}: the labels' and headings' quieter ink. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/workstation_info",
            new Colours(0xFF3A3D4A));
    private static final String ELLIPSIS = "...";

    /** Every window of this kind that is up, so an answer from the machine reaches the one that asked. */
    private static final List<WorkstationInfoApp> OPEN = new ArrayList<>();

    public WorkstationInfoApp(final BlockPos host) {
        this.host = host;
        OPEN.add(this);
        ask();
    }

    /** Hands a machine's answer to every open window that is showing that machine. */
    public static void accept(final WorkstationInfoPayload payload) {
        for (final WorkstationInfoApp app : OPEN) {
            if (app.host.equals(payload.hostPos())) {
                app.facts = payload.facts();
            }
        }
    }

    /** Every fact the window shows, as {@code label=value}, or nothing until the machine has answered. */
    public List<String> shownFacts() {
        final List<String> out = new ArrayList<>();
        if (this.facts != null) {
            for (final WorkstationFacts.Group group : this.facts.groups()) {
                for (final WorkstationFacts.Row row : group.rows()) {
                    out.add(GameText.resolve(row.label()) + "=" + GameText.resolve(row.value()));
                }
            }
        }
        return out;
    }

    /** The middle of Close, in desktop pixels. */
    public int[] closeCentre() {
        final Rect r = WorkstationInfoLayout.close();
        return CdeStylePages.centre(r, this.left, this.top);
    }

    @Override
    public String title() {
        return GameText.resolve(WorkstationInfoTexts.TITLE);
    }

    @Override
    public int defaultWidth() {
        return WorkstationInfoLayout.W + WorkstationInfoLayout.FRAME_W;
    }

    @Override
    public int defaultHeight() {
        return WorkstationInfoLayout.H + WorkstationInfoLayout.FRAME_H;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return defaultHeight();
    }

    @Override
    public void applySkin(final OsSkin skin) {
        this.skin = skin;
    }

    @Override
    public void onRestored() {
        if (!OPEN.contains(this)) {
            OPEN.add(this);
        }
        ask();
    }

    @Override
    public void onClosed() {
        OPEN.remove(this);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int w,
                              final int h, final int mouseX, final int mouseY, final float partialTick) {
        this.left = x;
        this.top = y;
        if (System.currentTimeMillis() - this.askedAt >= ASK_EVERY_MS) {
            ask();
        }
        final DesktopScreen desktop = DesktopScreen.current();
        if (this.skin == null || desktop == null) {
            return;
        }
        final CdePalette p = desktop.cdePalette();
        if (this.facts != null) {
            final List<WorkstationFacts.Group> groups = this.facts.groups();
            for (int gi = 0; gi < groups.size() && gi < WorkstationInfoLayout.GROUPS.size(); gi++) {
                drawGroup(g, font, gi, groups.get(gi), p);
            }
        }
        final Rect close = WorkstationInfoLayout.close();
        this.skin.button(g, font, x + close.x(), y + close.y(), close.w(), close.h(),
                GameText.resolve(WorkstationInfoTexts.CLOSE),
                close.holds(mouseX - x, mouseY - y), false, true);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (button == 0 && WorkstationInfoLayout.close().holds(mouseX - this.left, mouseY - this.top)) {
            DesktopScreen.closeWindowFor(this);
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            DesktopScreen.closeWindowFor(this);
            return true;
        }
        return false;
    }

    private void ask() {
        this.askedAt = System.currentTimeMillis();
        PacketDistributor.sendToServer(new RequestWorkstationInfoPayload(this.host));
    }

    private void drawGroup(final GuiGraphics g, final Font font, final int index, final WorkstationFacts.Group group,
                           final CdePalette p) {
        final Rect well = WorkstationInfoLayout.group(index);
        this.skin.panel(g, this.left + well.x(), this.top + well.y(), well.w(), well.h());
        Texts.small(g, font, GameText.resolve(group.title()).toUpperCase(Locale.ROOT),
                this.left + WorkstationInfoLayout.innerX(), this.top + WorkstationInfoLayout.headY(index),
                PALETTE.get().labelInk());
        for (int r = 0; r < group.rows().size(); r++) {
            final WorkstationFacts.Row row = group.rows().get(r);
            final int rowY = this.top + WorkstationInfoLayout.rowY(index, r);
            final String label = GameText.resolve(row.label());
            Texts.small(g, font, label,
                    this.left + WorkstationInfoLayout.LABEL_RIGHT - Texts.smallWidth(font, label), rowY,
                    PALETTE.get().labelInk());
            Texts.small(g, font, fit(font, GameText.resolve(row.value()), WorkstationInfoLayout.valueWidth()),
                    this.left + WorkstationInfoLayout.VALUE_X, rowY, p.ink());
            if (row.meter()) {
                meter(g, WorkstationInfoLayout.meter(index, r), p);
            }
        }
    }

    /** The memory in use as a bar: a sunken track, lit in the active colour as far as the memory is held. */
    private void meter(final GuiGraphics g, final Rect r, final CdePalette p) {
        final int x = this.left + r.x();
        final int y = this.top + r.y();
        MotifChrome.sunken(g, x, y, r.w(), r.h(), p.window(), p);
        final int lit = (int) Math.round((r.w() - 2) * this.facts.memoryShare());
        if (lit > 0) {
            g.fill(x + 1, y + 1, x + 1 + lit, y + r.h() - 1, p.active());
        }
    }

    /** A fact in the small text, cut with an ellipsis when it is wider than its column. */
    private static String fit(final Font font, final String text, final int room) {
        if (Texts.smallWidth(font, text) <= room) {
            return text;
        }
        final int units = Math.max(1, Texts.smallFits(room) - font.width(ELLIPSIS));
        return font.plainSubstrByWidth(text, units) + ELLIPSIS;
    }

    /** This window's colours: the labels' and headings' quieter ink. */
    private record Colours(int labelInk) {
    }
}
