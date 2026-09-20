/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.MessengerActionPayload;
import dev.jstech.computers.operation.payload.MessengerStatePayload;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Midsoft Messenger: the first program here where the other end is another player.
 *
 * <p>Who is on the network down the left, the conversation on the right, and the nudge. The history lives
 * on the network's own machine rather than on either computer, so a conversation is there when you next
 * sit down at any machine on that network.
 *
 * <p>The bar at the bottom of the roster carries what the service weighs. That is not decoration: the
 * service really grows on the disk as it keeps what people said and in memory as more of them connect, so
 * a busy chat on a small machine is something the player can watch getting expensive.
 */
public final class MessengerApp implements IDesktopApp {

    private static final int TOOLBAR_H = 14;
    private static final int ROSTER_W = 96;
    private static final int ROW_H = 10;
    private static final int COMPOSE_H = 28;
    private static final int STATUS_H = 11;
    private static final int MARGIN = 3;
    private static final int ONLINE = 0xFF4FA05C;
    private static final int OFFLINE = 0xFF9A9A9A;
    private static final int MINE_INK = 0xFFB4231F;
    private static final int THEIRS_INK = 0xFF1C4FA8;
    private static final int NUDGE_BG = 0xFFFFF0CC;
    private static final int NUDGE_INK = 0xFF8A4B00;
    /** How often the window asks again, so a conversation somebody else is having still arrives. */
    private static final long POLL_MS = 2000L;

    /** The one window the network's answers belong to. */
    @Nullable
    private static MessengerApp open;

    private final BlockPos host;
    private final BlockPos monitorPos;
    private final Panel root = new Panel();
    /*
     * Held to what one message may hold. The payload's own cap throws when it is handed more than it
     * takes, so the field has to refuse the letters rather than the network refusing the packet.
     */
    private final TextField compose = new TextField(MessengerLog.MAX_TEXT);
    private final Button send;
    private final Button nudge;

    private OsSkin skin = OsSkin.fallback();
    private MessengerStatePayload state = new MessengerStatePayload(
            new MessengerStatePayload.Service(false, "", 0L, 0),
            List.of(), List.of("lobby"), "lobby", List.of());
    private String room = "lobby";
    private long lastAskMs;
    private int scroll;

    /* Where the roster was last drawn, so a click reads the same numbers. */
    private int rosterTop;
    private int rosterRows;

    public MessengerApp(final BlockPos host, final BlockPos monitorPos) {
        this.host = host;
        this.monitorPos = monitorPos;
        send = root.add(new Button("Send", this::say));
        nudge = root.add(new Button("Nudge", this::sendNudge));
        root.add(compose);
        open = this;
        look();
    }

    /** Takes the network's answer, when this window is the one on the glass. */
    public static void accept(final MessengerStatePayload payload) {
        final MessengerApp showing = open;
        if (showing == null) {
            return;
        }
        showing.state = payload;
        if (!payload.room().isBlank()) {
            showing.room = payload.room();
        }
    }

    private void look() {
        this.lastAskMs = System.currentTimeMillis();
        PacketDistributor.sendToServer(new MessengerActionPayload(
                host, monitorPos, MessengerActionPayload.LOOK, room, ""));
    }

    private void say() {
        final String text = compose.edit().strip();
        if (text.isEmpty()) {
            return;
        }
        compose.set("");
        this.lastAskMs = System.currentTimeMillis();
        PacketDistributor.sendToServer(new MessengerActionPayload(
                host, monitorPos, MessengerActionPayload.SAY, room, text));
    }

    private void sendNudge() {
        this.lastAskMs = System.currentTimeMillis();
        PacketDistributor.sendToServer(new MessengerActionPayload(
                host, monitorPos, MessengerActionPayload.NUDGE, room, ""));
    }

    @Override
    public void onClosed() {
        if (open == this) {
            open = null;
        }
        // The service stops counting the memory it was holding for this window the moment it goes.
        PacketDistributor.sendToServer(new MessengerActionPayload(
                host, monitorPos, MessengerActionPayload.LEAVE, room, ""));
    }

    @Override
    public void onRestored() {
        open = this;
        look();
    }

    @Override
    public String title() {
        // A conversation with one person is titled with their name, the way a messenger window is.
        final String other = MessengerLog.otherIn(room, me());
        return other.isEmpty() ? "Messenger" : "Messenger - " + other;
    }

    @Override
    public int defaultWidth() {
        return 280;
    }

    @Override
    public int defaultHeight() {
        return 180;
    }

    @Override
    public int minWidth() {
        return 220;
    }

    @Override
    public int minHeight() {
        return 130;
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
         * The window being drawn claims the network's answers. A program may be open more than once, and
         * claiming only in the constructor left the first window of two deaf for the rest of its life;
         * claiming here hands the answers to whichever is in front, which is the one being looked at.
         */
        open = this;
        if (System.currentTimeMillis() - lastAskMs >= POLL_MS) {
            look();
        }
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        final int bottom = y + height - STATUS_H;
        drawRoster(g, font, x, y, bottom);
        final int chatX = x + ROSTER_W + 1;
        drawConversation(g, font, chatX, y, x + width, bottom - COMPOSE_H);
        layoutCompose(font, chatX, bottom - COMPOSE_H, x + width - chatX);
        root.render(g, ctx);
        drawStatus(g, font, x, bottom, width);
    }

    /**
     * Who is there: the room everybody shares, then everybody else on the network.
     *
     * <p>Clicking a name opens the conversation with that person, which is what a messenger is for. The
     * names are what the list is made of rather than the rooms, because a player thinks in people.
     */
    private void drawRoster(final GuiGraphics g, final Font font, final int x, final int y,
                            final int bottom) {
        g.fill(x, y, x + ROSTER_W, bottom, skin.fieldBg());
        g.fill(x + ROSTER_W, y, x + ROSTER_W + 1, bottom, skin.edge());
        g.drawString(font, state.service().online() ? "On the network" : "No service",
                x + MARGIN, y + 2, state.service().online() ? skin.text() : MINE_INK, false);
        this.rosterTop = y + TOOLBAR_H;
        this.rosterRows = Math.max(0, (bottom - rosterTop) / ROW_H);
        final List<String> rows = rosterEntries();
        for (int i = 0; i < rows.size() && i < rosterRows; i++) {
            final String name = rows.get(i);
            final int ry = rosterTop + i * ROW_H;
            final boolean lobby = i == 0;
            final boolean on = lobby ? MessengerLog.LOBBY.equals(room)
                    : room.equals(MessengerLog.privateRoom(me(), name));
            if (on) {
                g.fill(x, ry, x + ROSTER_W, ry + ROW_H, skin.listSelect());
            }
            final boolean online = !lobby && isOnline(name);
            if (!lobby) {
                g.fill(x + MARGIN, ry + 3, x + MARGIN + 4, ry + 7, online ? ONLINE : OFFLINE);
            }
            final int ink = on ? skin.listRowText(true) : (lobby || online ? skin.text() : skin.dim());
            g.drawString(font, font.plainSubstrByWidth(lobby ? "Everybody" : name, ROSTER_W - 14),
                    x + MARGIN + (lobby ? 0 : 7), ry + 1, ink, false);
        }
        if (rows.size() == 1) {
            final int ry = rosterTop + ROW_H;
            g.drawString(font, "nobody else", x + MARGIN, ry + 1, skin.dim(), false);
        }
    }

    /**
     * The lobby, then everybody on the network, then anybody this player has talked to who is not.
     *
     * <p>Somebody who has gone offline stays on the list, greyed, because the conversation with them is
     * still there and a messenger that forgot people the moment they left would be a strange one.
     */
    private List<String> rosterEntries() {
        final List<String> out = new ArrayList<>();
        out.add(MessengerLog.LOBBY);
        for (final String person : state.people()) {
            if (!person.equalsIgnoreCase(me())) {
                out.add(person);
            }
        }
        for (final String each : state.rooms()) {
            if (!MessengerLog.isPrivate(each)) {
                continue;
            }
            final String other = MessengerLog.otherIn(each, me());
            if (!other.isEmpty() && !other.equalsIgnoreCase(me())
                    && out.stream().noneMatch(seen -> seen.equalsIgnoreCase(other))) {
                out.add(other);
            }
        }
        return out;
    }

    /** Whether somebody on the list has the messenger open right now. */
    private boolean isOnline(final String name) {
        return state.people().stream().anyMatch(person -> person.equalsIgnoreCase(name));
    }

    /** What this player is called, which is how a conversation with somebody else is named. */
    private static String me() {
        return Minecraft.getInstance().getUser().getName();
    }

    private void drawConversation(final GuiGraphics g, final Font font, final int x, final int y,
                                  final int right, final int bottom) {
        g.fill(x, y, right, bottom, skin.fieldBg());
        final List<MessengerStatePayload.Line> lines = state.lines();
        final int rows = Math.max(1, (bottom - y - 2) / ROW_H);
        this.scroll = Math.max(0, Math.min(scroll, Math.max(0, lines.size() - rows)));
        final int start = Math.max(0, lines.size() - rows - scroll);
        int ry = y + 2;
        for (int i = start; i < lines.size() && ry + ROW_H <= bottom; i++) {
            final MessengerStatePayload.Line line = lines.get(i);
            if (line.nudge()) {
                g.fill(x + MARGIN, ry, right - MARGIN, ry + ROW_H - 1, NUDGE_BG);
                final String text = line.from() + " sent a nudge";
                g.drawString(font, text, x + (right - x - font.width(text)) / 2, ry + 1, NUDGE_INK, false);
            } else {
                final String who = line.from() + ":";
                g.drawString(font, who, x + MARGIN, ry + 1,
                        line.online() ? THEIRS_INK : OFFLINE, false);
                g.drawString(font,
                        font.plainSubstrByWidth(line.text(), right - x - MARGIN * 2 - font.width(who) - 3),
                        x + MARGIN + font.width(who) + 3, ry + 1, skin.text(), false);
            }
            ry += ROW_H;
        }
        if (lines.isEmpty()) {
            g.drawString(font, state.service().online() ? "Nothing said yet" : "No Messenger Service here",
                    x + MARGIN, y + 3, skin.dim(), false);
        }
    }

    private void layoutCompose(final Font font, final int x, final int y, final int width) {
        final int sendW = font.width("Send") + 8;
        final int nudgeW = font.width("Nudge") + 8;
        compose.setBounds(x + MARGIN, y + 2, Math.max(20, width - MARGIN * 2), 12);
        nudge.setBounds(x + MARGIN, y + 16, nudgeW, 11);
        send.setBounds(x + width - MARGIN - sendW, y + 16, sendW, 11);
        compose.setEnabled(state.service().online());
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        g.fill(x, y, x + width, y + STATUS_H, skin.windowBg());
        final MessengerStatePayload.Service service = state.service();
        final String left = service.online()
                ? "on " + (service.host().isBlank() ? "the network" : service.host())
                : "no service";
        g.drawString(font, left, x + MARGIN, y + 2, skin.dim(), false);
        /*
         * What it weighs, which is the whole point of the program: the history on the disk and the memory
         * it is holding for the people connected to it right now.
         */
        final String right = bytes(service.historyBytes()) + "   " + service.ramMb() + " MB";
        g.drawString(font, right, x + width - MARGIN - font.width(right), y + 2, skin.text(), false);
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
        if (button != 0 || mouseY < rosterTop) {
            return;
        }
        final int row = (int) ((mouseY - rosterTop) / ROW_H);
        final List<String> rows = rosterEntries();
        if (row >= 0 && row < rows.size() && row < rosterRows) {
            this.room = row == 0 ? MessengerLog.LOBBY
                    : MessengerLog.privateRoom(me(), rows.get(row));
            this.scroll = 0;
            look();
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        this.scroll = Math.max(0, scroll + (int) Math.signum(delta));
        return true;
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            say();
            return true;
        }
        return root.keyPressed(key, scanCode, modifiers);
    }

    /** Everything the conversation is showing, one line after another, for a test to read. */
    public String conversationText() {
        final StringBuilder out = new StringBuilder();
        for (final MessengerStatePayload.Line line : state.lines()) {
            out.append(line.nudge() ? line.from() + " sent a nudge" : line.from() + ": " + line.text())
                    .append('\n');
        }
        return out.toString();
    }

    /** The rooms this window is offering, for a test to read. */
    public List<String> rooms() {
        return new ArrayList<>(state.rooms());
    }
}
