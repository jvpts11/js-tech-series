/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.MessengerActionPayload;
import dev.jstech.computers.operation.payload.MessengerKeptPayload;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.ColumnHeader;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.time.GameCalendar;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The conversations the network's Messenger Service keeps, opened from the Messenger at the service's own server: one
 * row each, with how much was said, what it weighs and when it was last spoken in, and the buttons that delete one or
 * all of them. What it deletes leaves every Messenger on the network with no word to anybody
 * (computers/docs/design/programs.md).
 */
public final class MessengerKeptDialog implements IDesktopApp {

    private final BlockPos host;
    private final BlockPos monitorPos;
    private final Panel root = new Panel();
    private final ColumnHeader header;
    private final ListView<MessengerKeptPayload.Kept> list;
    private final Button delete;
    private final Button deleteAll;
    private final Button close;
    private OsSkin skin = OsSkin.fallback();
    private List<MessengerKeptPayload.Kept> rows = List.of();
    private long bytes;
    private long lastAskMs;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 224;
    private static final int ROW_H = 15;
    private static final int HEADER_H = 13;
    private static final int MARGIN = 8;
    private static final int BUTTON_H = 15;
    /** How often the list is asked for again, so what people say meanwhile shows up in it. */
    private static final long POLL_MS = 2000L;
    /* Where each column begins, as shares of the list's width. */
    private static final double LINES_AT = 0.44;
    private static final double SIZE_AT = 0.56;
    private static final double SAID_AT = 0.72;
    /** The one list of kept conversations open, which the service's answers belong to. */
    @Nullable
    private static MessengerKeptDialog open;

    private MessengerKeptDialog(final BlockPos host, final BlockPos monitorPos) {
        this.host = host;
        this.monitorPos = monitorPos;
        this.header = root.add(new ColumnHeader(List.of(GameText.resolve(SocialTexts.KEPT_CONVERSATION),
                GameText.resolve(SocialTexts.KEPT_LINES), GameText.resolve(SocialTexts.KEPT_SIZE),
                GameText.resolve(SocialTexts.KEPT_LAST_SAID))).setSortable(false));
        this.list = root.add(new ListView<>(() -> rows, ROW_H, this::drawRow).setSelectable(true));
        this.delete = root.add(new Button(GameText.resolve(SocialTexts.KEPT_DELETE), this::deleteChosen));
        this.deleteAll = root.add(new Button(GameText.resolve(SocialTexts.KEPT_DELETE_ALL), this::deleteEvery));
        this.close = root.add(new Button(GameText.resolve(SocialTexts.KEPT_CLOSE),
                () -> ActiveDesktop.closeDialog(this)));
    }

    /** Opens the list over {@code owner}, the Messenger at the service's server, or brings the open one up to date. */
    public static void openFor(final IDesktopApp owner, final BlockPos host, final BlockPos monitorPos) {
        if (open == null) {
            open = new MessengerKeptDialog(host, monitorPos);
            ActiveDesktop.openDialogFor(owner, open);
        }
        open.ask(MessengerActionPayload.KEPT, "");
    }

    /** Takes the service's answer, when a list is open. */
    public static void accept(final MessengerKeptPayload payload) {
        final MessengerKeptDialog showing = open;
        if (showing == null) {
            return;
        }
        showing.rows = payload.conversations();
        showing.bytes = payload.bytes();
        if (showing.list.selected() >= showing.rows.size()) {
            showing.list.setSelected(showing.rows.isEmpty() ? -1 : showing.rows.size() - 1);
        }
    }

    @Override
    public String title() {
        return GameText.resolve(SocialTexts.KEPT_TITLE);
    }

    @Override
    public int defaultWidth() {
        return WIDTH;
    }

    @Override
    public int defaultHeight() {
        return HEIGHT;
    }

    @Override
    public int minWidth() {
        return WIDTH;
    }

    @Override
    public int minHeight() {
        return HEIGHT - 30;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void onClosed() {
        if (open == this) {
            open = null;
        }
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        if (System.currentTimeMillis() - lastAskMs >= POLL_MS) {
            ask(MessengerActionPayload.KEPT, "");
        }
        g.fill(x, y, x + width, y + height, skin.windowBg());
        Draw.text(g, font, GameText.resolve(SocialTexts.KEPT_INTRO), x + MARGIN, y + MARGIN, skin.text());
        final int listX = x + MARGIN;
        final int listW = width - MARGIN * 2;
        final int headY = y + MARGIN + 15;
        final int buttonsY = y + height - MARGIN - BUTTON_H;
        final int notesY = buttonsY - 46;
        header.setBounds(listX, headY, listW, HEADER_H);
        header.setColumnX(listX + 3, columnX(listX, listW, LINES_AT), columnX(listX, listW, SIZE_AT),
                columnX(listX, listW, SAID_AT));
        final int listTop = headY + HEADER_H;
        g.fill(listX, listTop, listX + listW, notesY - 4, skin.fieldBg());
        g.fill(listX, notesY - 4, listX + listW, notesY - 3, skin.edge());
        list.setBounds(listX, listTop, listW, notesY - 4 - listTop);
        layoutButtons(font, x + MARGIN, buttonsY, width - MARGIN * 2);
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        root.render(g, ctx);
        if (rows.isEmpty()) {
            Draw.text(g, font, GameText.resolve(SocialTexts.KEPT_NONE), listX + 3, listTop + 3, skin.dim());
        }
        Draw.text(g, font, GameText.resolve(SocialTexts.KEPT_ON_DISK.with(MessengerLog.sizeText(bytes))),
                listX, notesY, skin.text());
        int lineY = notesY + 13;
        for (final FormattedCharSequence line
                : font.split(Component.literal(GameText.resolve(SocialTexts.KEPT_LEAVES)), listW)) {
            Draw.text(g, font, line, listX, lineY, skin.dim());
            lineY += 11;
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        list.setScroll(list.scroll() - (int) Math.signum(delta));
        return true;
    }

    /** The conversations listed, by the names they are shown under, for a test to read. */
    public List<String> shown() {
        return rows.stream().map(MessengerKeptDialog::nameOf).toList();
    }

    /** Chooses the row of conversation {@code index}, as a click on it does, for a test. */
    public void choose(final int index) {
        list.setSelected(index);
    }

    /** The open list, for a test; null when none is up. */
    @Nullable
    public static MessengerKeptDialog showing() {
        return open;
    }

    /** Deletes the chosen conversation, as its button does. */
    public void deleteChosen() {
        final int chosen = list.selected();
        if (chosen >= 0 && chosen < rows.size()) {
            ask(MessengerActionPayload.DELETE, rows.get(chosen).room());
        }
    }

    /** Deletes every conversation, as its button does. */
    public void deleteEvery() {
        if (!rows.isEmpty()) {
            ask(MessengerActionPayload.DELETE_ALL, "");
        }
    }

    private void ask(final int action, final String room) {
        this.lastAskMs = System.currentTimeMillis();
        PacketDistributor.sendToServer(new MessengerActionPayload(host, monitorPos, action, room, ""));
    }

    private void layoutButtons(final Font font, final int x, final int y, final int width) {
        final int deleteW = font.width(GameText.resolve(SocialTexts.KEPT_DELETE)) + 16;
        final int allW = font.width(GameText.resolve(SocialTexts.KEPT_DELETE_ALL)) + 16;
        final int closeW = font.width(GameText.resolve(SocialTexts.KEPT_CLOSE)) + 16;
        delete.setBounds(x, y, deleteW, BUTTON_H);
        deleteAll.setBounds(x + deleteW + 6, y, allW, BUTTON_H);
        close.setBounds(x + width - closeW, y, closeW, BUTTON_H);
        delete.setEnabled(list.selected() >= 0 && list.selected() < rows.size());
        deleteAll.setEnabled(!rows.isEmpty());
    }

    private void drawRow(final GuiGraphics g, final UiContext ctx, final MessengerKeptPayload.Kept row,
                         final int index, final int x, final int y, final int w, final int h, final boolean hovered,
                         final boolean selected) {
        if (selected) {
            g.fill(x, y, x + w, y + h, skin.listSelect());
        } else if (hovered) {
            g.fill(x, y, x + w, y + h, skin.listHover());
        }
        final int ink = skin.listRowText(selected);
        final Font font = ctx.font();
        final int linesX = columnX(x, w, LINES_AT);
        final int textY = y + (h - 7) / 2;
        Draw.text(g, font, font.plainSubstrByWidth(nameOf(row), linesX - x - 6), x + 3, textY, ink);
        Draw.text(g, font, String.valueOf(row.lines()), linesX, textY, ink);
        Draw.text(g, font, MessengerLog.sizeText(row.bytes()), columnX(x, w, SIZE_AT), textY, ink);
        Draw.text(g, font, saidAt(row.saidAt()), columnX(x, w, SAID_AT), textY, ink);
    }

    private static int columnX(final int x, final int width, final double share) {
        return x + (int) Math.round(width * share);
    }

    /* Everybody's conversation by that word, a private one by its two names. */
    private static String nameOf(final MessengerKeptPayload.Kept row) {
        return row.names().isEmpty() ? GameText.resolve(SocialTexts.EVERYBODY) : row.names();
    }

    private static String saidAt(final long clock) {
        return GameText.resolve(SocialTexts.KEPT_SAID_AT.with(GameCalendar.day(clock) + 1L,
                String.format(Locale.ROOT, "%02d:%02d", GameCalendar.hourOf(clock), GameCalendar.minuteOf(clock))));
    }
}
