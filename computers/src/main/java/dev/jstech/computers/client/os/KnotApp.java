/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.KnotActionPayload;
import dev.jstech.computers.operation.payload.KnotStatePayload;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Knot: what a network is keeping of the source its players are still arguing over.
 *
 * <p>The history along the top, what changed underneath it. It sits below the package manager rather than
 * beside it: a package is a finished thing one player hands to another, and this is the code before it
 * became one.
 *
 * <p>Pushing reads the file off this machine's own disk rather than sending its text, so what the network
 * keeps is what the machine really holds.
 */
public final class KnotApp implements IDesktopApp {

    private static final int TOOLBAR_H = 15;
    private static final int PICK_H = 14;
    private static final int ROW_H = 10;
    private static final int STATUS_H = 11;
    private static final int MARGIN = 3;
    private static final int HISTORY_ROWS = 4;
    private static final int ADDED_BG = 0xFFDCF5DE;
    private static final int ADDED_INK = 0xFF16621F;
    private static final int REMOVED_BG = 0xFFFBDDDB;
    private static final int REMOVED_INK = 0xFF8A1F18;
    private static final int OFFLINE_INK = 0xFFB4231F;

    /** The one window the network's answers belong to. */
    @Nullable
    private static KnotApp open;

    private final BlockPos host;
    private final BlockPos monitorPos;
    private final Panel root = new Panel();
    /*
     * Held to exactly what the request carries, so a window can name any file a disk can hold and can
     * never be handed more than the packet takes, which refuses by throwing rather than by shortening.
     */
    private final TextField file = new TextField(KnotActionPayload.MAX_PATH);
    private final TextField message = new TextField(KnotActionPayload.MAX_MESSAGE);
    private final Button pushButton;
    private final Button pullButton;
    private final Button refreshButton;

    private OsSkin skin = OsSkin.fallback();
    private KnotStatePayload state = new KnotStatePayload(
            new KnotStatePayload.Service(false, "", 0L), List.of(), List.of(), 0, List.of());
    private int picked;
    private int historyScroll;
    private int diffScroll;

    /* Where the history was last drawn, so a click reads the same numbers. */
    private int historyTop;

    public KnotApp(final BlockPos host, final BlockPos monitorPos) {
        this.host = host;
        this.monitorPos = monitorPos;
        pushButton = root.add(new Button("Push", this::push));
        pullButton = root.add(new Button("Pull", this::pull));
        refreshButton = root.add(new Button("Refresh", this::look));
        root.add(file);
        root.add(message);
        file.set("plant.sgs");
        message.set("");
        open = this;
        look();
    }

    /** Takes the network's answer, when this window is the one on the glass. */
    public static void accept(final KnotStatePayload payload) {
        final KnotApp showing = open;
        if (showing == null) {
            return;
        }
        showing.state = payload;
        /*
         * Nothing picked yet lands on the newest, which is what somebody opening this wants to see. So does
         * a revision that is no longer there: a repository taken off and put back numbers from one again,
         * and a window still pointing at the old numbers showed an empty comparison for ever.
         */
        if (!payload.revisions().isEmpty() && !showing.hasRevision(showing.picked)) {
            showing.picked = payload.revisions().get(0).number();
            showing.askFor(showing.picked);
        }
    }

    /** Whether the history the window is showing holds that revision. */
    private boolean hasRevision(final int number) {
        for (final KnotStatePayload.Revision revision : state.revisions()) {
            if (revision.number() == number) {
                return true;
            }
        }
        return false;
    }

    private void look() {
        askFor(picked);
    }

    private void askFor(final int revision) {
        PacketDistributor.sendToServer(new KnotActionPayload(
                host, monitorPos, KnotActionPayload.LOOK, file.edit(), "", revision));
    }

    private void push() {
        // Nothing is pushed to a service that is not there, whatever route reached this method.
        final String named = state.service().online() ? file.edit().strip() : "";
        if (named.isEmpty()) {
            return;
        }
        PacketDistributor.sendToServer(new KnotActionPayload(
                host, monitorPos, KnotActionPayload.PUSH, named, message.edit(), picked));
        message.set("");
    }

    private void pull() {
        if (picked <= 0 || !state.service().online()) {
            return;
        }
        PacketDistributor.sendToServer(new KnotActionPayload(
                host, monitorPos, KnotActionPayload.PULL, file.edit(), "", picked));
        FilesApps.diskChanged();
    }

    @Override
    public void onClosed() {
        if (open == this) {
            open = null;
        }
    }

    @Override
    public void onRestored() {
        open = this;
        look();
    }

    @Override
    public String title() {
        return "Knot";
    }

    @Override
    public int defaultWidth() {
        return 290;
    }

    @Override
    public int defaultHeight() {
        return 190;
    }

    @Override
    public int minWidth() {
        return 240;
    }

    @Override
    public int minHeight() {
        return 140;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        /*
         * The window being drawn claims the network's answers, so that with two open the one in front is
         * the one that hears back rather than whichever was made last.
         */
        open = this;
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        layoutToolbar(font, x, y, width);
        layoutPicker(font, x, y + TOOLBAR_H, width);
        root.render(g, ctx);
        final int historyY = y + TOOLBAR_H + PICK_H;
        drawHistory(g, font, x, historyY, width);
        final int diffY = historyY + HISTORY_ROWS * ROW_H + 2;
        drawDiff(g, font, x, diffY, width, y + height - STATUS_H);
        drawStatus(g, font, x, y + height - STATUS_H, width);
    }

    private void layoutToolbar(final Font font, final int x, final int y, final int width) {
        int bx = x + MARGIN;
        final int pushW = font.width("Push") + 8;
        pushButton.setBounds(bx, y + 1, pushW, 12);
        bx += pushW + 2;
        final int pullW = font.width("Pull") + 8;
        pullButton.setBounds(bx, y + 1, pullW, 12);
        bx += pullW + 2;
        final int refreshW = font.width("Refresh") + 8;
        refreshButton.setBounds(bx, y + 1, refreshW, 12);
        bx += refreshW + 4;
        message.setBounds(bx, y + 1, Math.max(20, x + width - MARGIN - bx), 12);
        pushButton.setEnabled(state.service().online());
        pullButton.setEnabled(state.service().online() && picked > 0);
    }

    private void layoutPicker(final Font font, final int x, final int y, final int width) {
        file.setBounds(x + MARGIN + font.width("File") + 4, y + 1,
                Math.max(40, width / 2 - MARGIN * 2), 12);
    }

    private void drawHistory(final GuiGraphics g, final Font font, final int x, final int y,
                             final int width) {
        g.drawString(font, "File", x + MARGIN, y - PICK_H + 3, skin.dim(), false);
        this.historyTop = y;
        g.fill(x + MARGIN, y, x + width - MARGIN, y + HISTORY_ROWS * ROW_H, skin.fieldBg());
        final List<KnotStatePayload.Revision> revisions = state.revisions();
        this.historyScroll = Math.max(0,
                Math.min(historyScroll, Math.max(0, revisions.size() - HISTORY_ROWS)));
        if (revisions.isEmpty()) {
            g.drawString(font, state.service().online() ? "Nothing pushed yet" : "No KnotHub on this network",
                    x + MARGIN + 3, y + 2, state.service().online() ? skin.dim() : OFFLINE_INK, false);
            return;
        }
        for (int i = 0; i < HISTORY_ROWS && historyScroll + i < revisions.size(); i++) {
            final KnotStatePayload.Revision revision = revisions.get(historyScroll + i);
            final int ry = y + i * ROW_H;
            final boolean on = revision.number() == picked;
            if (on) {
                g.fill(x + MARGIN, ry, x + width - MARGIN, ry + ROW_H, skin.listSelect());
            }
            final int ink = skin.listRowText(on);
            g.drawString(font, "r" + revision.number(), x + MARGIN + 3, ry + 1, ink, false);
            final String text = revision.author() + ": " + revision.message();
            g.drawString(font, font.plainSubstrByWidth(text, width - 80), x + MARGIN + 26, ry + 1,
                    ink, false);
            final String named = revision.file();
            g.drawString(font, font.plainSubstrByWidth(named, 48),
                    x + width - MARGIN - 3 - Math.min(48, font.width(named)), ry + 1,
                    on ? ink : skin.dim(), false);
        }
    }

    private void drawDiff(final GuiGraphics g, final Font font, final int x, final int y,
                          final int width, final int bottom) {
        g.fill(x + MARGIN, y, x + width - MARGIN, bottom, skin.fieldBg());
        final List<KnotStatePayload.DiffLine> lines = state.diff();
        final int rows = Math.max(1, (bottom - y) / ROW_H);
        this.diffScroll = Math.max(0, Math.min(diffScroll, Math.max(0, lines.size() - rows)));
        if (lines.isEmpty()) {
            g.drawString(font, picked > 0 ? "No change in r" + picked : "Pick a revision",
                    x + MARGIN + 3, y + 2, skin.dim(), false);
            return;
        }
        for (int i = 0; i < rows && diffScroll + i < lines.size(); i++) {
            final KnotStatePayload.DiffLine line = lines.get(diffScroll + i);
            final int ry = y + i * ROW_H;
            final int background = switch (line.kind()) {
                case KnotStatePayload.DiffLine.ADDED -> ADDED_BG;
                case KnotStatePayload.DiffLine.REMOVED -> REMOVED_BG;
                default -> 0;
            };
            if (background != 0) {
                g.fill(x + MARGIN, ry, x + width - MARGIN, ry + ROW_H, background);
            }
            final int ink = switch (line.kind()) {
                case KnotStatePayload.DiffLine.ADDED -> ADDED_INK;
                case KnotStatePayload.DiffLine.REMOVED -> REMOVED_INK;
                default -> skin.dim();
            };
            final String mark = switch (line.kind()) {
                case KnotStatePayload.DiffLine.ADDED -> "+";
                case KnotStatePayload.DiffLine.REMOVED -> "-";
                default -> " ";
            };
            g.drawString(font, mark, x + MARGIN + 3, ry + 1, ink, false);
            g.drawString(font, font.plainSubstrByWidth(line.text(), width - MARGIN * 2 - 14),
                    x + MARGIN + 11, ry + 1, ink, false);
        }
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y,
                            final int width) {
        g.fill(x, y, x + width, y + STATUS_H, skin.windowBg());
        final KnotStatePayload.Service service = state.service();
        /*
         * What the machine had to say about the last thing asked of it comes first, because a player who
         * pressed Push wants to know what came of it rather than how many revisions there are.
         */
        final String left = !service.note().isEmpty() ? service.note()
                : service.online()
                        ? "on " + (service.host().isBlank() ? "the network" : service.host())
                        : "no service";
        final int count = state.revisions().size();
        final String right = count + (count == 1 ? " revision   " : " revisions   ") + bytes(service.bytes());
        g.drawString(font, font.plainSubstrByWidth(left, width - MARGIN * 2 - font.width(right) - 8),
                x + MARGIN, y + 2, service.online() ? skin.text() : OFFLINE_INK, false);
        g.drawString(font, right, x + width - MARGIN - font.width(right), y + 2, skin.dim(), false);
    }

    private static String bytes(final long value) {
        if (value < 1024) {
            return value + " B";
        }
        return String.format(Locale.ROOT, "%.1f KB", value / 1024.0);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (root.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        if (button != 0 || mouseY < historyTop || mouseY >= historyTop + HISTORY_ROWS * ROW_H) {
            return;
        }
        final int row = historyScroll + (int) ((mouseY - historyTop) / ROW_H);
        final List<KnotStatePayload.Revision> revisions = state.revisions();
        if (row >= 0 && row < revisions.size()) {
            this.picked = revisions.get(row).number();
            this.diffScroll = 0;
            final String named = revisions.get(row).file();
            if (!named.isEmpty()) {
                file.set(named);
            }
            askFor(picked);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.diffScroll = Math.max(0, diffScroll - (int) Math.signum(delta));
        return true;
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return root.keyPressed(key, scanCode, modifiers);
    }

    /** Which revision is being looked at, for a test to read. */
    public int shownRevision() {
        return picked;
    }
}
