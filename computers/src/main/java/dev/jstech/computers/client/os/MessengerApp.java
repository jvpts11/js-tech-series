/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.MessengerActionPayload;
import dev.jstech.computers.operation.payload.MessengerStatePayload;
import dev.jstech.computers.program.MessengerLog;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
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
 * on the server running the service rather than on either computer, so a conversation is there when you
 * next sit down at any machine on that network, and it goes with the server if somebody pulls it.
 *
 * <p>The bar at the bottom of the roster carries what the service weighs. That is not decoration: the
 * service really grows on the disk as it keeps what people said and in memory as more of them connect, so
 * a busy chat on a small machine is something the player can watch getting expensive.
 */
@PaletteHolder
public final class MessengerApp implements IDesktopApp {

    private static final int TOOLBAR_H = 14;
    private static final int ROSTER_W = 96;
    private static final int ROW_H = 10;
    private static final int COMPOSE_H = 28;
    private static final int STATUS_H = 11;
    private static final int MARGIN = 3;
    /**
     * The Messenger's own colours, {@code jsc:app/messenger}: a contact online and away, the names on my lines and
     * on theirs, and a nudge's band and words.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/messenger",
            new Colours(0xFF4FA05C, 0xFF9A9A9A, 0xFFB4231F, 0xFF1C4FA8, 0xFFFFF0CC, 0xFF8A4B00));
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
    private int rosterLeft;
    private int rosterTop;
    private int rosterBottom;
    private int rosterRows;

    public MessengerApp(final BlockPos host, final BlockPos monitorPos) {
        this.host = host;
        this.monitorPos = monitorPos;
        send = root.add(new Button(GameText.resolve(SocialTexts.SEND), this::say));
        nudge = root.add(new Button(GameText.resolve(SocialTexts.NUDGE), this::sendNudge));
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
        /*
         * Nothing is sent to a service that is not there. The client alone let a message look sent while no
         * server on the network was running the service, which is the one answer a chat window must never
         * give: the words went nowhere and nobody was ever going to read them.
         */
        final String text = state.service().online() ? compose.edit().strip() : "";
        if (text.isEmpty()) {
            return;
        }
        compose.set("");
        this.lastAskMs = System.currentTimeMillis();
        PacketDistributor.sendToServer(new MessengerActionPayload(
                host, monitorPos, MessengerActionPayload.SAY, room, text));
    }

    private void sendNudge() {
        if (!state.service().online()) {
            return;
        }
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
        final boolean serving = state.service().online();
        g.drawString(font, GameText.resolve(serving ? SocialTexts.ON_THE_NETWORK : SocialTexts.NO_SERVICE_HEADING),
                x + MARGIN, y + 2, serving ? skin.text() : PALETTE.get().mine(), false);
        this.rosterLeft = x;
        this.rosterTop = y + TOOLBAR_H;
        this.rosterBottom = bottom;
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
                g.fill(x + MARGIN, ry + 3, x + MARGIN + 4, ry + 7,
                        online ? PALETTE.get().online() : PALETTE.get().offline());
            }
            final int ink = on ? skin.listRowText(true) : (lobby || online ? skin.text() : skin.dim());
            g.drawString(font, font.plainSubstrByWidth(lobby ? GameText.resolve(SocialTexts.EVERYBODY) : name,
                            ROSTER_W - 14),
                    x + MARGIN + (lobby ? 0 : 7), ry + 1, ink, false);
        }
        if (rows.size() == 1) {
            final int ry = rosterTop + ROW_H;
            g.drawString(font, GameText.resolve(SocialTexts.NOBODY_ELSE), x + MARGIN, ry + 1, skin.dim(), false);
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
                g.fill(x + MARGIN, ry, right - MARGIN, ry + ROW_H - 1, PALETTE.get().nudge());
                final String text = GameText.resolve(SocialTexts.SENT_A_NUDGE.with(line.from()));
                g.drawString(font, text, x + (right - x - font.width(text)) / 2, ry + 1, PALETTE.get().nudgeInk(),
                        false);
            } else {
                final String who = line.from() + ":";
                g.drawString(font, who, x + MARGIN, ry + 1,
                        line.online() ? PALETTE.get().theirs() : PALETTE.get().offline(), false);
                g.drawString(font,
                        font.plainSubstrByWidth(line.text(), right - x - MARGIN * 2 - font.width(who) - 3),
                        x + MARGIN + font.width(who) + 3, ry + 1, skin.text(), false);
            }
            ry += ROW_H;
        }
        if (lines.isEmpty()) {
            final String empty = GameText.resolve(state.service().online()
                    ? SocialTexts.NOTHING_SAID : SocialTexts.NO_MESSENGER);
            g.drawString(font, font.plainSubstrByWidth(empty, right - x - MARGIN * 2),
                    x + MARGIN, y + 3, skin.dim(), false);
        }
    }

    private void layoutCompose(final Font font, final int x, final int y, final int width) {
        final int sendW = font.width(GameText.resolve(SocialTexts.SEND)) + 8;
        final int nudgeW = font.width(GameText.resolve(SocialTexts.NUDGE)) + 8;
        compose.setBounds(x + MARGIN, y + 2, Math.max(20, width - MARGIN * 2), 12);
        nudge.setBounds(x + MARGIN, y + 16, nudgeW, 11);
        send.setBounds(x + width - MARGIN - sendW, y + 16, sendW, 11);
        // With no service on the network there is nothing to say anything to, and the bar says so.
        final boolean serving = state.service().online();
        compose.setEnabled(serving);
        send.setEnabled(serving);
        nudge.setEnabled(serving);
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        g.fill(x, y, x + width, y + STATUS_H, skin.windowBg());
        final MessengerStatePayload.Service service = state.service();
        final String left = GameText.resolve(service.online()
                ? SocialTexts.ON_HOST.with(service.host().isBlank() ? SocialTexts.THE_NETWORK.text() : service.host())
                : SocialTexts.NO_SERVICE.text());
        /*
         * What it weighs, which is the whole point of the program: the history on the disk and the memory
         * it is holding for the people connected to it right now. Drawn first, because it is the number
         * this program exists to show and the name on the left gives way to it rather than over it.
         */
        final String right = bytes(service.historyBytes()) + "   " + service.ramMb() + " MB";
        g.drawString(font, right, x + width - MARGIN - font.width(right), y + 2, skin.text(), false);
        final int room = width - MARGIN * 3 - font.width(right);
        g.drawString(font, font.plainSubstrByWidth(left, Math.max(0, room)), x + MARGIN, y + 2,
                skin.dim(), false);
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
        /*
         * Only inside the roster. Judged by the height alone, a click anywhere in the conversation itself
         * landed on whichever name sat at that height and moved the player into somebody else's window.
         */
        if (button != 0 || mouseY < rosterTop || mouseY >= rosterBottom
                || mouseX < rosterLeft || mouseX >= rosterLeft + ROSTER_W) {
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
            out.append(line.nudge() ? GameText.resolve(SocialTexts.SENT_A_NUDGE.with(line.from()))
                            : line.from() + ": " + line.text())
                    .append('\n');
        }
        return out.toString();
    }

    /** The rooms this window is offering, for a test to read. */
    public List<String> rooms() {
        return new ArrayList<>(state.rooms());
    }

    /** The Messenger's colours, as the palette above names them. */
    private record Colours(int online, int offline, int mine, int theirs, int nudge, int nudgeInk) {
    }
}
