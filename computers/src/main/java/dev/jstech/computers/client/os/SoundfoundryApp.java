/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.audio.MusicPlayer;
import dev.jstech.computers.client.audio.MusicImporter;
import dev.jstech.computers.client.audio.SoundfoundryStates;
import dev.jstech.computers.gui.layout.SoundfoundryLayout;
import dev.jstech.computers.gui.layout.SoundfoundryLayout.Rect;
import dev.jstech.computers.os.ProgramVersions;
import dev.jstech.computers.operation.payload.SoundfoundryActionPayload;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.client.audio.media.MediaPlayer;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Soundfoundry in its Legacy form: a player in the foundry skin with its playlist docked under it, the way the music
 * players of the time looked, the same on every desktop.
 *
 * <p>Everything it shows is the machine's: the window asks the machine how it stands every second, and every button
 * asks the machine to act, so two players at two screens of the same machine see the same song. What is the window's
 * own is only how it is laid out: the playlist put away, either plate folded to its bar, how far the list is scrolled
 * and which songs are picked in it.
 */
public final class SoundfoundryApp implements IDesktopApp {

    private final BlockPos host;
    private final FileDialog dialog;
    private final ContextMenu menu = new ContextMenu(MENU_W, MENU_ROW_H);
    private final TreeSet<Integer> picked = new TreeSet<>();
    private final float[] levels = new float[SoundfoundryLayout.BARS];
    private OsSkin skin = OsSkin.fallback();
    private boolean playlistShown = true;
    private boolean playerShaded;
    private boolean listShaded;
    private int scroll;
    /** What is being dragged: one of the DRAG constants. */
    private int dragging = DRAG_NONE;
    private double dragValue;
    private int anchor = -1;
    private int lastRow = -1;
    private long lastClickAt;
    private long nextLookAt;
    private boolean focused = true;
    private int pressedControl = DesktopWindow.BUTTON_NONE;
    /** What an import from the player's own computer has got to, for the song display; null while none runs. */
    @Nullable
    private Text importing;
    /** Something to say on the song display for a while, and until when. */
    private Text notice = Text.EMPTY;
    private long noticeUntil;
    private int lastX;
    private int lastY;
    private int mouseX;
    private int mouseY;

    private static final int MENU_W = 160;
    private static final int MENU_ROW_H = 14;
    private static final int DRAG_NONE = 0;
    private static final int DRAG_VOLUME = 1;
    private static final int DRAG_BALANCE = 2;
    private static final int DRAG_POSITION = 3;
    private static final int DRAG_SCROLL = 4;
    /** How often the window asks the machine how it stands, in milliseconds. */
    private static final long LOOK_EVERY = 1000L;
    private static final long DOUBLE_CLICK = 300L;
    private static final long NOTICE_FOR = 4000L;
    /** How fast the song display scrolls, in milliseconds a pixel. */
    private static final long MARQUEE_PACE = 60L;
    private static final String MARQUEE_GAP = "  ***  ";
    private static final String SAVE_NAME = "playlist.m3u";

    public SoundfoundryApp(final BlockPos host) {
        this.host = host;
        this.dialog = new FileDialog(host, this);
        look();
    }

    @Override
    public String title() {
        return ProgramClient.nameOf(Programs.SOUNDFOUNDRY);
    }

    @Override
    public int defaultWidth() {
        return SoundfoundryLayout.WIDTH;
    }

    @Override
    public int defaultHeight() {
        return SoundfoundryLayout.playerHeight(playerShaded)
                + SoundfoundryLayout.playlistHeight(playlistShown, listShaded);
    }

    @Override
    public boolean drawsOwnFrame() {
        return true;
    }

    @Override
    public int frameControlAt(final int x, final int y) {
        if (SoundfoundryLayout.MINIMIZE.contains(x, y)) {
            return DesktopWindow.BUTTON_MINIMIZE;
        }
        if (SoundfoundryLayout.CLOSE.contains(x, y)) {
            return DesktopWindow.BUTTON_CLOSE;
        }
        final int top = playerHeight();
        if (playlistShown && SoundfoundryLayout.MINIMIZE.contains(x, y - top)) {
            return DesktopWindow.BUTTON_MINIMIZE;
        }
        return DesktopWindow.BUTTON_NONE;
    }

    @Override
    public boolean frameDragAt(final int x, final int y) {
        final int top = playerHeight();
        return onBar(x, y) || playlistShown && onBar(x, y - top);
    }

    @Override
    public void frameState(final boolean isFocused, final int pressed) {
        this.focused = isFocused;
        this.pressedControl = pressed;
    }

    @Override
    public void applySkin(final OsSkin value) {
        this.skin = value;
    }

    @Override
    public void openFile(final String path) {
        send(SoundfoundryActionPayload.ADD, SoundfoundryActionPayload.AND_PLAY, 0L, List.of(path), List.of());
    }

    @Override
    public void onRestored() {
        look();
    }

    @Override
    public String saveState() {
        return (playlistShown ? "1" : "0") + (playerShaded ? "1" : "0") + (listShaded ? "1" : "0") + ":" + scroll;
    }

    @Override
    public void restoreState(final String state) {
        if (state.length() < 4 || state.charAt(3) != ':') {
            return;
        }
        playlistShown = state.charAt(0) == '1';
        playerShaded = state.charAt(1) == '1';
        listShaded = state.charAt(2) == '1';
        try {
            scroll = Math.max(0, Integer.parseInt(state.substring(4)));
        } catch (final NumberFormatException malformed) {
            scroll = 0;
        }
    }

    @Override
    public boolean wantsEscape() {
        return menu.isOpen();
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mx, final int my, final float partialTick) {
        this.lastX = x;
        this.lastY = y;
        this.mouseX = mx;
        this.mouseY = my;
        if (Util.getMillis() >= nextLookAt) {
            look();
        }
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        final int top = playerHeight();
        if (playerShaded) {
            renderShadedPlayer(g, font, x, y, known);
        } else {
            renderPlayer(g, font, x, y, known);
        }
        if (playlistShown) {
            if (listShaded) {
                SoundfoundrySkin.plate(g, x, y + top, SoundfoundryLayout.WIDTH, SoundfoundryLayout.SHADE_H);
                SoundfoundrySkin.bar(g, font, x, y + top, words(SoundfoundryAppTexts.PLAYLIST), focused,
                        pressedIndex(true));
            } else {
                renderPlaylist(g, font, x, y + top, known);
            }
        }
        if (menu.isOpen()) {
            menu.render(g, new UiContext(skin, font, mx, my, partialTick));
        }
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mx, final double my, final int button) {
        if (menu.isOpen()) {
            menu.mouseClicked(mx, my, button);
            return;
        }
        final double lx = mx - lastX;
        final double ly = my - lastY;
        final int top = playerHeight();
        if (ly < top) {
            clickedPlayer(lx, ly, button);
        } else if (playlistShown) {
            clickedPlaylist(lx, ly - top, button);
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mx, final double my, final int button) {
        final double lx = mx - lastX;
        final double ly = my - lastY;
        switch (dragging) {
            case DRAG_VOLUME -> dragValue = SoundfoundryLayout.valueAt(SoundfoundryLayout.VOLUME,
                    SoundfoundryLayout.THUMB, lx);
            case DRAG_BALANCE -> dragValue = SoundfoundryLayout.valueAt(SoundfoundryLayout.BALANCE,
                    SoundfoundryLayout.THUMB, lx);
            case DRAG_POSITION -> dragValue = SoundfoundryLayout.valueAt(SoundfoundryLayout.POSITION,
                    SoundfoundryLayout.POSITION_THUMB, lx);
            case DRAG_SCROLL -> scrollTo(ly - playerHeight());
            default -> {
                // Nothing held.
            }
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mx, final double my, final int button) {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        switch (dragging) {
            case DRAG_VOLUME -> send(SoundfoundryActionPayload.VOLUME,
                    (int) Math.round(dragValue * SoundfoundryState.MAX_VOLUME));
            case DRAG_BALANCE -> send(SoundfoundryActionPayload.BALANCE,
                    (int) Math.round((dragValue * 2.0 - 1.0) * SoundfoundryState.MAX_BALANCE));
            case DRAG_POSITION -> {
                if (known != null && known.state().playing().millis() > 0) {
                    send(SoundfoundryActionPayload.SEEK, 0,
                            Math.round(dragValue * known.state().playing().millis()), List.of(), List.of());
                }
            }
            default -> {
                // Nothing held.
            }
        }
        dragging = DRAG_NONE;
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final int top = playerHeight();
        final double ly = mouseY - lastY - top;
        if (!playlistShown || listShaded || ly < 0) {
            return false;
        }
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta) * 3));
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (menu.isOpen()) {
            return menu.keyPressed(key, scanCode, modifiers);
        }
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        final int songs = known == null ? 0 : known.songs().size();
        switch (key) {
            // The keys the players of the time were driven by, along the bottom row of the keyboard.
            case GLFW.GLFW_KEY_Z -> send(SoundfoundryActionPayload.PREVIOUS, 0);
            case GLFW.GLFW_KEY_X -> send(SoundfoundryActionPayload.PLAY, -1);
            case GLFW.GLFW_KEY_C -> send(SoundfoundryActionPayload.PAUSE, 0);
            case GLFW.GLFW_KEY_V -> send(SoundfoundryActionPayload.STOP, 0);
            case GLFW.GLFW_KEY_B -> send(SoundfoundryActionPayload.NEXT, 0);
            case GLFW.GLFW_KEY_DELETE -> removePicked();
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                if (!picked.isEmpty()) {
                    send(SoundfoundryActionPayload.PLAY, picked.first());
                }
            }
            case GLFW.GLFW_KEY_A -> {
                if (!Screen.hasControlDown()) {
                    return false;
                }
                selectAll(songs);
            }
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_DOWN -> {
                final int from = picked.isEmpty() ? -1 : picked.first();
                final int to = Math.max(0, Math.min(songs - 1, from + (key == GLFW.GLFW_KEY_UP ? -1 : 1)));
                if (songs > 0) {
                    pick(to, false, false);
                    reveal(to);
                }
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    /* For the tests: where things are, and what the window shows. */

    /** The middle of a part of the player, by its rectangle in the layout, in desktop pixels. */
    public int[] playerPoint(final Rect part) {
        return new int[] {lastX + part.x() + part.w() / 2, lastY + part.y() + part.h() / 2};
    }

    /** The middle of a part of the playlist, the same way. */
    public int[] playlistPoint(final Rect part) {
        final int top = lastY + playerHeight();
        return new int[] {lastX + part.x() + part.w() / 2, top + part.y() + part.h() / 2};
    }

    /** The middle of a visible row of the playlist. */
    public int[] rowPoint(final int row) {
        final int top = lastY + playerHeight();
        return new int[] {lastX + SoundfoundryLayout.ROW_X + 40,
                top + SoundfoundryLayout.ROW_TOP + row * SoundfoundryLayout.ROW_H + SoundfoundryLayout.ROW_H / 2};
    }

    /** Whether the playlist is up under the player. */
    public boolean playlistShown() {
        return playlistShown;
    }

    /** Whether a menu of the window is open. */
    public boolean menuOpen() {
        return menu.isOpen();
    }

    /** The middle of the open menu's entry so called, or null. */
    @Nullable
    public int[] menuPoint(final String label) {
        final List<ContextMenu.Item> items = menu.items();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).label().equals(label)) {
                return menu.itemCenter(i);
            }
        }
        return null;
    }

    /** The places picked in the playlist. */
    public List<Integer> picked() {
        return List.copyOf(picked);
    }

    private void clickedPlayer(final double lx, final double ly, final int button) {
        if (SoundfoundryLayout.SHADE.contains(lx, ly)) {
            playerShaded = !playerShaded;
            return;
        }
        if (playerShaded) {
            return;
        }
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        if (SoundfoundryLayout.VOLUME.contains(lx, ly)) {
            dragging = DRAG_VOLUME;
            dragValue = SoundfoundryLayout.valueAt(SoundfoundryLayout.VOLUME, SoundfoundryLayout.THUMB, lx);
        } else if (SoundfoundryLayout.BALANCE.contains(lx, ly)) {
            dragging = DRAG_BALANCE;
            dragValue = SoundfoundryLayout.valueAt(SoundfoundryLayout.BALANCE, SoundfoundryLayout.THUMB, lx);
        } else if (SoundfoundryLayout.POSITION.contains(lx, ly) && known != null
                && known.state().status() != SoundfoundryStatePayload.STOPPED) {
            dragging = DRAG_POSITION;
            dragValue = SoundfoundryLayout.valueAt(SoundfoundryLayout.POSITION, SoundfoundryLayout.POSITION_THUMB, lx);
        } else if (SoundfoundryLayout.PL.contains(lx, ly)) {
            playlistShown = !playlistShown;
        } else if (SoundfoundryLayout.NET.contains(lx, ly)) {
            DesktopScreen.openOrFocus(SoundfoundryShareApp.KEY);
        } else if (SoundfoundryLayout.PREVIOUS.contains(lx, ly)) {
            send(SoundfoundryActionPayload.PREVIOUS, 0);
        } else if (SoundfoundryLayout.PLAY.contains(lx, ly)) {
            playPicked(known);
        } else if (SoundfoundryLayout.PAUSE.contains(lx, ly)) {
            send(SoundfoundryActionPayload.PAUSE, 0);
        } else if (SoundfoundryLayout.STOP.contains(lx, ly)) {
            send(SoundfoundryActionPayload.STOP, 0);
        } else if (SoundfoundryLayout.NEXT.contains(lx, ly)) {
            send(SoundfoundryActionPayload.NEXT, 0);
        } else if (SoundfoundryLayout.EJECT.contains(lx, ly)) {
            // Hung from the button's right edge, the way the menu of the time dropped towards the display.
            menu.open(ejectItems(), lastX + SoundfoundryLayout.EJECT.right() - MENU_W,
                    lastY + SoundfoundryLayout.EJECT.bottom(), lastX, lastY, SoundfoundryLayout.WIDTH,
                    defaultHeight());
        } else if (SoundfoundryLayout.SHUFFLE.contains(lx, ly) && known != null) {
            send(SoundfoundryActionPayload.SHUFFLE, known.state().shuffle() ? 0 : 1);
        } else if (SoundfoundryLayout.REPEAT.contains(lx, ly) && known != null) {
            send(SoundfoundryActionPayload.REPEAT, known.state().repeat() ? 0 : 1);
        }
    }

    private void clickedPlaylist(final double lx, final double ly, final int button) {
        if (SoundfoundryLayout.SHADE.contains(lx, ly)) {
            listShaded = !listShaded;
            return;
        }
        if (SoundfoundryLayout.CLOSE.contains(lx, ly)) {
            playlistShown = false;
            return;
        }
        if (listShaded) {
            return;
        }
        final int top = playerHeight();
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        final int songs = known == null ? 0 : known.songs().size();
        final int row = SoundfoundryLayout.rowAt(lx, ly);
        if (row >= 0) {
            final int index = scroll + row;
            if (index >= songs) {
                picked.clear();
                return;
            }
            final long now = Util.getMillis();
            final boolean twice = index == lastRow && now - lastClickAt < DOUBLE_CLICK;
            lastRow = index;
            lastClickAt = now;
            if (twice) {
                send(SoundfoundryActionPayload.PLAY, index);
                return;
            }
            pick(index, Screen.hasControlDown(), Screen.hasShiftDown());
        } else if (SoundfoundryLayout.SCROLLBAR.contains(lx, ly)) {
            dragging = DRAG_SCROLL;
            scrollTo(ly);
        } else if (SoundfoundryLayout.ADD.contains(lx, ly)) {
            openMenu(SoundfoundryLayout.ADD, top, List.of(
                    item(SoundfoundryAppTexts.ADD_FILE, () -> pickSongs(0)),
                    item(SoundfoundryAppTexts.ADD_FOLDER, this::pickFolder)));
        } else if (SoundfoundryLayout.REMOVE.contains(lx, ly)) {
            openMenu(SoundfoundryLayout.REMOVE, top, List.of(
                    item(SoundfoundryAppTexts.REMOVE_SELECTED, this::removePicked),
                    item(SoundfoundryAppTexts.CROP, () -> {
                        send(SoundfoundryActionPayload.CROP, 0, 0L, List.of(), List.copyOf(picked));
                        picked.clear();
                    }),
                    ContextMenu.Item.separator(),
                    item(SoundfoundryAppTexts.CLEAR, this::clear)));
        } else if (SoundfoundryLayout.SELECT.contains(lx, ly)) {
            openMenu(SoundfoundryLayout.SELECT, top, List.of(
                    item(SoundfoundryAppTexts.SELECT_ALL, () -> selectAll(songs)),
                    item(SoundfoundryAppTexts.SELECT_NONE, picked::clear),
                    item(SoundfoundryAppTexts.INVERT, () -> invert(songs))));
        } else if (SoundfoundryLayout.MISC.contains(lx, ly)) {
            openMenu(SoundfoundryLayout.MISC, top, List.of(
                    item(SoundfoundryAppTexts.SORT_TITLE, () -> reorder(SoundfoundryActionPayload.SORT_TITLE)),
                    item(SoundfoundryAppTexts.SORT_FILE, () -> reorder(SoundfoundryActionPayload.SORT_FILE)),
                    item(SoundfoundryAppTexts.REVERSE, () -> reorder(SoundfoundryActionPayload.REVERSE))));
        } else if (SoundfoundryLayout.LISTS.contains(lx, ly)) {
            openMenu(SoundfoundryLayout.LISTS, top, List.of(
                    item(SoundfoundryAppTexts.NEW_LIST, this::clear),
                    item(SoundfoundryAppTexts.OPEN_LIST, this::openList),
                    item(SoundfoundryAppTexts.SAVE_LIST, this::saveList)));
        }
    }

    private List<ContextMenu.Item> ejectItems() {
        return List.of(
                item(SoundfoundryAppTexts.OPEN_FILE, () -> pickSongs(SoundfoundryActionPayload.AND_PLAY)),
                item(SoundfoundryAppTexts.OPEN_FOLDER, this::pickFolder),
                ContextMenu.Item.separator(),
                item(SoundfoundryAppTexts.IMPORT, this::importSongs));
    }

    private void openMenu(final Rect under, final int top, final List<ContextMenu.Item> items) {
        menu.open(items, lastX + under.x(), lastY + top + under.bottom(), lastX, lastY,
                SoundfoundryLayout.WIDTH, defaultHeight());
    }

    private static ContextMenu.Item item(final TextKey label, final Runnable action) {
        return new ContextMenu.Item(GameText.resolve(label), true, action);
    }

    private void pickSongs(final int then) {
        dialog.openFile(SoundfoundryAppTexts.OPEN_TITLE.text(), "",
                List.of(FileDialog.Filter.of(SoundfoundryAppTexts.SONGS, "ogg", "wav"), FileDialog.Filter.ALL),
                path -> send(SoundfoundryActionPayload.ADD, then, 0L, List.of(path), List.of()));
    }

    private void pickFolder() {
        dialog.openFolder(SoundfoundryAppTexts.FOLDER_TITLE.text(), "",
                folder -> send(SoundfoundryActionPayload.ADD_FOLDER, 0, 0L, List.of(folder), List.of()));
    }

    private void openList() {
        dialog.openFile(SoundfoundryAppTexts.LIST_TITLE.text(), "",
                List.of(FileDialog.Filter.of(SoundfoundryAppTexts.PLAYLISTS, "m3u"), FileDialog.Filter.ALL),
                path -> send(SoundfoundryActionPayload.OPEN_LIST, 0, 0L, List.of(path), List.of()));
    }

    private void saveList() {
        dialog.saveAs(SoundfoundryAppTexts.SAVE_TITLE.text(), "", SAVE_NAME,
                List.of(FileDialog.Filter.of(SoundfoundryAppTexts.PLAYLISTS, "m3u")),
                path -> send(SoundfoundryActionPayload.SAVE_LIST, 0, 0L, List.of(path), List.of()));
    }

    /* Songs from the player's own computer, picked in their own system's dialog, onto the disk and the playlist. */
    private void importSongs() {
        MusicImporter.pick(host, "", true, new MusicImporter.IListener() {
            @Override
            public void progress(final int song, final int songs, final long sent, final long total) {
                importing = SoundfoundryAppTexts.IMPORTING.with(song, songs,
                        total <= 0 ? 100 : Math.round(sent * 100.0 / total));
            }

            @Override
            public void finished(final Path file, final boolean ok, final Text message) {
                if (!ok) {
                    say(message);
                }
            }

            @Override
            public void done() {
                importing = null;
                look();
            }
        });
    }

    private void playPicked(@Nullable final SoundfoundryStates.Known known) {
        final boolean stopped = known == null || known.state().status() == SoundfoundryStatePayload.STOPPED;
        send(SoundfoundryActionPayload.PLAY, stopped && !picked.isEmpty() ? picked.first() : -1);
    }

    private void removePicked() {
        if (!picked.isEmpty()) {
            send(SoundfoundryActionPayload.REMOVE, 0, 0L, List.of(), List.copyOf(picked));
            picked.clear();
        }
    }

    private void clear() {
        send(SoundfoundryActionPayload.CLEAR, 0);
        picked.clear();
        scroll = 0;
    }

    private void reorder(final int action) {
        send(action, 0);
        picked.clear();
    }

    private void selectAll(final int songs) {
        picked.clear();
        for (int i = 0; i < songs; i++) {
            picked.add(i);
        }
    }

    private void invert(final int songs) {
        final TreeSet<Integer> inverted = new TreeSet<>();
        for (int i = 0; i < songs; i++) {
            if (!picked.contains(i)) {
                inverted.add(i);
            }
        }
        picked.clear();
        picked.addAll(inverted);
    }

    private void pick(final int index, final boolean toggle, final boolean range) {
        if (range && anchor >= 0) {
            picked.clear();
            for (int i = Math.min(anchor, index); i <= Math.max(anchor, index); i++) {
                picked.add(i);
            }
            return;
        }
        if (toggle) {
            if (!picked.remove(index)) {
                picked.add(index);
            }
        } else {
            picked.clear();
            picked.add(index);
        }
        anchor = index;
    }

    private void reveal(final int index) {
        if (index < scroll) {
            scroll = index;
        } else if (index >= scroll + SoundfoundryLayout.ROWS) {
            scroll = index - SoundfoundryLayout.ROWS + 1;
        }
    }

    private void scrollTo(final double ly) {
        final Rect bar = SoundfoundryLayout.SCROLLBAR;
        final double at = Math.clamp((ly - bar.y()) / Math.max(1, bar.h()), 0.0, 1.0);
        scroll = (int) Math.round(at * maxScroll());
    }

    private int maxScroll() {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        return Math.max(0, (known == null ? 0 : known.songs().size()) - SoundfoundryLayout.ROWS);
    }

    private void say(final Text message) {
        notice = message;
        noticeUntil = Util.getMillis() + NOTICE_FOR;
    }

    private void look() {
        nextLookAt = Util.getMillis() + LOOK_EVERY;
        send(SoundfoundryActionPayload.LOOK, SoundfoundryStates.revisionOf(host));
    }

    private void send(final int action, final int index) {
        send(action, index, 0L, List.of(), List.of());
    }

    private void send(final int action, final int index, final long value, final List<String> paths,
                      final List<Integer> indexes) {
        PacketDistributor.sendToServer(new SoundfoundryActionPayload(host, action, index, value, paths, indexes));
    }

    private int playerHeight() {
        return SoundfoundryLayout.playerHeight(playerShaded);
    }

    private static boolean onBar(final int x, final int y) {
        return y >= 0 && y < SoundfoundryLayout.SHADE_H && x >= 0 && x < SoundfoundryLayout.WIDTH
                && !SoundfoundryLayout.MINIMIZE.contains(x, y) && !SoundfoundryLayout.SHADE.contains(x, y)
                && !SoundfoundryLayout.CLOSE.contains(x, y);
    }

    /* Which of a plate's bar buttons is held: the window's own, which the player's bar and the list's minimise are. */
    private int pressedIndex(final boolean list) {
        if (pressedControl == DesktopWindow.BUTTON_MINIMIZE) {
            final boolean overList = mouseY - lastY >= playerHeight();
            return overList == list ? 0 : -1;
        }
        return pressedControl == DesktopWindow.BUTTON_CLOSE && !list ? 2 : -1;
    }

    private static String words(final TextKey key) {
        return GameText.resolve(key);
    }

    private void renderPlayer(final GuiGraphics g, final Font font, final int x, final int y,
                              @Nullable final SoundfoundryStates.Known known) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final SoundfoundryStatePayload state = known == null ? null : known.state();
        final boolean device = state == null || state.device();
        final int status = state == null ? SoundfoundryStatePayload.STOPPED : state.status();
        SoundfoundrySkin.plate(g, x, y, SoundfoundryLayout.WIDTH, SoundfoundryLayout.PLAYER_H);
        SoundfoundrySkin.bar(g, font, x, y, words(SoundfoundryAppTexts.PLAYER), focused, pressedIndex(false));
        final Rect display = SoundfoundryLayout.DISPLAY;
        SoundfoundrySkin.lcd(g, x + display.x(), y + display.y(), display.w(), display.h());
        if (!device) {
            centred(g, font, words(SoundfoundryAppTexts.NO_SOUND), x + display.x() + display.w() / 2, y + 24,
                    c.amber());
            centred(g, font, words(SoundfoundryAppTexts.DEVICE), x + display.x() + display.w() / 2, y + 35,
                    c.amber());
        } else {
            final SoundfoundrySkin.Glyph glyph = status == SoundfoundryStatePayload.PLAYING
                    ? SoundfoundrySkin.Glyph.PLAY : status == SoundfoundryStatePayload.PAUSED
                    ? SoundfoundrySkin.Glyph.PAUSE : SoundfoundrySkin.Glyph.STOP;
            SoundfoundrySkin.glyph(g, glyph, x + SoundfoundryLayout.STATUS_X, y + SoundfoundryLayout.STATUS_Y,
                    status == SoundfoundryStatePayload.PLAYING ? c.amber()
                            : status == SoundfoundryStatePayload.PAUSED ? c.amberMid() : c.amberDim());
            // A paused time blinks, the way the displays of the time did.
            final boolean blinkOff = status == SoundfoundryStatePayload.PAUSED && Util.getMillis() / 500L % 2L == 1L;
            SoundfoundrySkin.clock(g, x + SoundfoundryLayout.CLOCK_X, y + SoundfoundryLayout.CLOCK_Y,
                    known == null ? 0L : known.position(),
                    status != SoundfoundryStatePayload.STOPPED && !blinkOff);
            final boolean alive = status == SoundfoundryStatePayload.PLAYING
                    && MediaPlayer.levels(musicKey(), levels);
            SoundfoundrySkin.bars(g, x + SoundfoundryLayout.BARS_X, y + SoundfoundryLayout.BARS_BOTTOM, levels,
                    alive);
        }
        renderSongLine(g, font, x, y, known, device);
        final SoundfoundryStatePayload.Playing playing = state == null ? SoundfoundryStatePayload.Playing.NONE
                : state.playing();
        final boolean on = device && playing.channels() > 0;
        final Rect kbps = SoundfoundryLayout.KBPS;
        SoundfoundrySkin.lcd(g, x + kbps.x(), y + kbps.y(), kbps.w(), kbps.h());
        if (on) {
            right(g, font, Integer.toString(playing.kbps()), x + kbps.right() - 2, y + kbps.y() + 2, c.amber());
        }
        SoundfoundrySkin.engraved(g, font, words(SoundfoundryAppTexts.KBPS), x + SoundfoundryLayout.KBPS_LABEL_X,
                y + SoundfoundryLayout.FORMAT_Y, c.inkDim());
        final Rect khz = SoundfoundryLayout.KHZ;
        SoundfoundrySkin.lcd(g, x + khz.x(), y + khz.y(), khz.w(), khz.h());
        if (on) {
            right(g, font, Long.toString(Math.round(playing.sampleRate() / 1000.0)), x + khz.right() - 2,
                    y + khz.y() + 2, c.amber());
        }
        SoundfoundrySkin.engraved(g, font, words(SoundfoundryAppTexts.KHZ), x + SoundfoundryLayout.KHZ_LABEL_X,
                y + SoundfoundryLayout.FORMAT_Y, c.inkDim());
        SoundfoundrySkin.engraved(g, font, words(SoundfoundryAppTexts.MONO), x + SoundfoundryLayout.MONO_X,
                y + SoundfoundryLayout.FORMAT_Y, on && playing.channels() == 1 ? c.amber() : c.amberDim());
        SoundfoundrySkin.engraved(g, font, words(SoundfoundryAppTexts.STEREO), x + SoundfoundryLayout.STEREO_X,
                y + SoundfoundryLayout.FORMAT_Y, on && playing.channels() >= 2 ? c.amber() : c.amberDim());
        final double volume = dragging == DRAG_VOLUME ? dragValue
                : state == null ? 0.8 : state.volume() / 100.0;
        final double balance = dragging == DRAG_BALANCE ? dragValue
                : state == null ? 0.5 : (state.balance() + 100) / 200.0;
        final Rect vol = SoundfoundryLayout.VOLUME;
        SoundfoundrySkin.slider(g, x + vol.x(), y + vol.y(), vol.w(), volume, !device);
        final Rect bal = SoundfoundryLayout.BALANCE;
        SoundfoundrySkin.slider(g, x + bal.x(), y + bal.y(), bal.w(), balance, !device);
        labelled(g, font, x, y, SoundfoundryLayout.PL, SoundfoundryAppTexts.PL, device && playlistShown, !device);
        labelled(g, font, x, y, SoundfoundryLayout.NET, SoundfoundryAppTexts.NET,
                device && DesktopScreen.windowOpen(SoundfoundryShareApp.KEY), !device);
        final Rect pos = SoundfoundryLayout.POSITION;
        final double at = dragging == DRAG_POSITION ? dragValue
                : known == null || playing.millis() <= 0 ? 0.0 : known.position() / (double) playing.millis();
        SoundfoundrySkin.position(g, x + pos.x(), y + pos.y(), pos.w(), at, device);
        transport(g, x, y, SoundfoundryLayout.PREVIOUS, SoundfoundrySkin.Glyph.PREVIOUS, false, device, 7);
        transport(g, x, y, SoundfoundryLayout.PLAY, SoundfoundrySkin.Glyph.PLAY,
                status == SoundfoundryStatePayload.PLAYING, device, 8);
        transport(g, x, y, SoundfoundryLayout.PAUSE, SoundfoundrySkin.Glyph.PAUSE,
                status == SoundfoundryStatePayload.PAUSED, device, 8);
        transport(g, x, y, SoundfoundryLayout.STOP, SoundfoundrySkin.Glyph.STOP, false, device, 8);
        transport(g, x, y, SoundfoundryLayout.NEXT, SoundfoundrySkin.Glyph.NEXT, false, device, 7);
        transport(g, x, y, SoundfoundryLayout.EJECT, SoundfoundrySkin.Glyph.EJECT, menu.isOpen(), device, 7);
        labelled(g, font, x, y, SoundfoundryLayout.SHUFFLE, SoundfoundryAppTexts.SHUFFLE,
                device && state != null && state.shuffle(), !device);
        labelled(g, font, x, y, SoundfoundryLayout.REPEAT, SoundfoundryAppTexts.REPEAT,
                device && state != null && state.repeat(), !device);
        SoundfoundrySkin.engraved(g, font, words(outLabel(state)), x + SoundfoundryLayout.OUT_X,
                y + SoundfoundryLayout.OUT_Y, device ? c.inkDim() : c.amberDim());
        SoundfoundrySkin.anvil(g, x + SoundfoundryLayout.MARK.x(), y + SoundfoundryLayout.MARK.y());
    }

    private void renderShadedPlayer(final GuiGraphics g, final Font font, final int x, final int y,
                                    @Nullable final SoundfoundryStates.Known known) {
        SoundfoundrySkin.plate(g, x, y, SoundfoundryLayout.WIDTH, SoundfoundryLayout.SHADE_H);
        final String time = known == null || known.state().status() == SoundfoundryStatePayload.STOPPED ? ""
                : clockText(known.position()) + "  ";
        SoundfoundrySkin.bar(g, font, x, y, time + words(SoundfoundryAppTexts.PLAYER), focused, pressedIndex(false));
    }

    /* The song display: what is wrong, or an import on its way, or the song it is on, scrolling when it is long. */
    private void renderSongLine(final GuiGraphics g, final Font font, final int x, final int y,
                                @Nullable final SoundfoundryStates.Known known, final boolean device) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        final Rect song = SoundfoundryLayout.SONG;
        SoundfoundrySkin.lcd(g, x + song.x(), y + song.y(), song.w(), song.h());
        final String line;
        if (!device) {
            line = words(SoundfoundryAppTexts.NO_CARD);
        } else if (Util.getMillis() < noticeUntil) {
            line = GameText.resolve(notice);
        } else if (importing != null) {
            line = GameText.resolve(importing);
        } else if (known != null && !known.state().trouble().isEmpty()) {
            line = GameText.resolve(known.state().trouble());
        } else {
            line = songLine(known);
        }
        final String shown = line.toUpperCase(Locale.ROOT);
        final int room = song.w() - 6;
        final int left = x + song.x() + 4;
        final int ink = device ? c.amber() : c.amberDim();
        Draw.pushScissor(g, x + song.x() + 2, y + song.y() + 1, x + song.right() - 2, y + song.bottom() - 1);
        if (font.width(shown) <= room) {
            SoundfoundrySkin.lit(g, font, shown, left, y + song.y() + 3, ink);
        } else {
            final String loop = shown + MARQUEE_GAP;
            final int span = font.width(loop);
            final int offset = (int) (Util.getMillis() / MARQUEE_PACE % span);
            SoundfoundrySkin.lit(g, font, loop, left - offset, y + song.y() + 3, ink);
            SoundfoundrySkin.lit(g, font, loop, left - offset + span, y + song.y() + 3, ink);
        }
        Draw.popScissor(g);
    }

    private String songLine(@Nullable final SoundfoundryStates.Known known) {
        if (known == null || known.state().current() < 0 || known.state().current() >= known.songs().size()) {
            return GameText.resolve(SoundfoundryAppTexts.IDLE.with(
                    ProgramVersions.of(Programs.SOUNDFOUNDRY.toString())));
        }
        final int current = known.state().current();
        final SoundfoundryStatePayload.Song song = known.songs().get(current);
        return GameText.resolve(SoundfoundryAppTexts.SONG.with(current + 1, shownAs(song), clockText(song.millis())));
    }

    private void renderPlaylist(final GuiGraphics g, final Font font, final int x, final int y,
                                @Nullable final SoundfoundryStates.Known known) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        SoundfoundrySkin.plate(g, x, y, SoundfoundryLayout.WIDTH, SoundfoundryLayout.PLAYLIST_H);
        SoundfoundrySkin.bar(g, font, x, y, words(SoundfoundryAppTexts.PLAYLIST), focused, pressedIndex(true));
        final Rect list = SoundfoundryLayout.LIST;
        SoundfoundrySkin.lcd(g, x + list.x(), y + list.y(), list.w(), list.h());
        final List<SoundfoundryStatePayload.Song> songs = known == null ? List.of() : known.songs();
        scroll = Math.max(0, Math.min(scroll, Math.max(0, songs.size() - SoundfoundryLayout.ROWS)));
        final int current = known == null ? -1 : known.state().current();
        final int textRoom = SoundfoundryLayout.LENGTH_RIGHT - SoundfoundryLayout.ROW_X - 34;
        for (int row = 0; row < SoundfoundryLayout.ROWS; row++) {
            final int index = scroll + row;
            if (index >= songs.size()) {
                break;
            }
            final SoundfoundryStatePayload.Song song = songs.get(index);
            final int ry = y + SoundfoundryLayout.ROW_TOP + row * SoundfoundryLayout.ROW_H;
            if (picked.contains(index)) {
                SoundfoundrySkin.fill(g, x + list.x() + 1, ry - 1, SoundfoundryLayout.SCROLLBAR.x() - list.x() - 3,
                        SoundfoundryLayout.ROW_H, c.rowSelected());
            }
            final int ink = index == current ? c.amber() : song.present() ? c.listInk() : c.inkDim();
            final String label = GameText.resolve(SoundfoundryAppTexts.ROW.with(index + 1, shownAs(song)));
            SoundfoundrySkin.lit(g, font, Texts.clip(font, label, textRoom), x + SoundfoundryLayout.ROW_X, ry, ink);
            if (song.millis() > 0) {
                right(g, font, clockText(song.millis()), x + SoundfoundryLayout.LENGTH_RIGHT, ry, ink);
            }
        }
        final Rect bar = SoundfoundryLayout.SCROLLBAR;
        SoundfoundrySkin.fill(g, x + bar.x(), y + bar.y(), bar.w(), bar.h(), c.track());
        final int thumb = songs.size() <= SoundfoundryLayout.ROWS ? bar.h()
                : Math.max(10, bar.h() * SoundfoundryLayout.ROWS / songs.size());
        final int max = Math.max(1, songs.size() - SoundfoundryLayout.ROWS);
        final int thumbY = songs.size() <= SoundfoundryLayout.ROWS ? 0 : (bar.h() - thumb) * scroll / max;
        SoundfoundrySkin.bevel(g, x + bar.x(), y + bar.y() + thumbY, bar.w(), thumb, c.steel(), c.steelHi(),
                c.steelLo());
        labelled(g, font, x, y, SoundfoundryLayout.ADD, SoundfoundryAppTexts.ADD, false, false);
        labelled(g, font, x, y, SoundfoundryLayout.REMOVE, SoundfoundryAppTexts.REMOVE, false, false);
        labelled(g, font, x, y, SoundfoundryLayout.SELECT, SoundfoundryAppTexts.SELECT, false, false);
        labelled(g, font, x, y, SoundfoundryLayout.MISC, SoundfoundryAppTexts.MISC, false, false);
        labelled(g, font, x, y, SoundfoundryLayout.LISTS, SoundfoundryAppTexts.LIST, false, false);
        final Rect total = SoundfoundryLayout.TOTAL;
        SoundfoundrySkin.lcd(g, x + total.x(), y + total.y(), total.w(), total.h());
        long all = 0L;
        for (final SoundfoundryStatePayload.Song song : songs) {
            all += song.millis();
        }
        final long now = known == null || known.state().status() == SoundfoundryStatePayload.STOPPED ? 0L
                : known.position();
        right(g, font, clockText(now) + "/" + clockText(all), x + total.right() - 4, y + total.y() + 2, c.amber());
    }

    private void labelled(final GuiGraphics g, final Font font, final int x, final int y, final Rect at,
                          final TextKey label, final boolean lit, final boolean dim) {
        SoundfoundrySkin.labelButton(g, font, x + at.x(), y + at.y(), at.w(), at.h(), words(label), lit, dim);
    }

    private static void transport(final GuiGraphics g, final int x, final int y, final Rect at,
                                  final SoundfoundrySkin.Glyph glyph, final boolean pressed, final boolean on,
                                  final int inset) {
        final SoundfoundrySkin.Colours c = SoundfoundrySkin.c();
        SoundfoundrySkin.button(g, x + at.x(), y + at.y(), at.w(), at.h(), pressed, false);
        SoundfoundrySkin.glyph(g, glyph, x + at.x() + inset, y + at.y() + 6, on ? c.ink() : c.inkDim());
    }

    private static void centred(final GuiGraphics g, final Font font, final String text, final int cx, final int y,
                                final int colour) {
        SoundfoundrySkin.lit(g, font, text, cx - font.width(text) / 2, y, colour);
    }

    private static void right(final GuiGraphics g, final Font font, final String text, final int rightEdge,
                              final int y, final int colour) {
        SoundfoundrySkin.lit(g, font, text, rightEdge - font.width(text), y, colour);
    }

    private static TextKey outLabel(@Nullable final SoundfoundryStatePayload state) {
        if (state == null) {
            return SoundfoundryAppTexts.OUT_NONE;
        }
        return switch (state.output()) {
            case SoundfoundryStatePayload.OUT_MONITOR -> SoundfoundryAppTexts.OUT_MONITOR;
            case SoundfoundryStatePayload.OUT_SPEAKERS -> SoundfoundryAppTexts.OUT_SPEAKERS;
            case SoundfoundryStatePayload.OUT_BOTH -> SoundfoundryAppTexts.OUT_BOTH;
            default -> SoundfoundryAppTexts.OUT_NONE;
        };
    }

    private static String shownAs(final SoundfoundryStatePayload.Song song) {
        return song.artist().isEmpty() ? song.title()
                : GameText.resolve(SoundfoundryAppTexts.BY.with(song.artist(), song.title()));
    }

    private static String clockText(final long millis) {
        final long seconds = Math.max(0L, millis / 1000L);
        return seconds / 60L + ":" + String.format(Locale.ROOT, "%02d", seconds % 60L);
    }

    /* What this machine's music plays under, as the server names it. */
    private String musicKey() {
        return MusicPlayer.keyOf(host);
    }
}
