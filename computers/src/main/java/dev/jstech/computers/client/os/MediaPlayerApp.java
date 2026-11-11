/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.audio.MediaLibrary;
import dev.jstech.computers.client.audio.SoundfoundryStates;
import dev.jstech.computers.operation.payload.MediaFramePayload;
import dev.jstech.computers.operation.payload.MediaLibraryPayload;
import dev.jstech.computers.operation.payload.MediaRequestPayload;
import dev.jstech.computers.operation.payload.SoundfoundryActionPayload;
import dev.jstech.computers.operation.payload.SoundfoundryStatePayload;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SoundfoundryState;
import dev.jstech.core.client.gui.component.ContextMenu;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The Media Player: every desktop's player of the computer's songs and of its image sequences, a folder of pictures
 * from Paint or a camera shown in the order of their names, the mod's video (computers/docs/design/programs.md). It
 * looks like the player its desktop really shipped ({@link MediaPlayerLook}); {@link MediaPlayerPainter} draws each.
 *
 * <p>Its songs play through the computer's own player, which Soundfoundry plays through too, so the two share what is
 * playing; a computer with no sound card plays no recordings, and the player says so. Its pictures come from the
 * computer one at a time, a little ahead of the one it shows, at 12 a second at first and from 1 to 20 in its options.
 */
public final class MediaPlayerApp implements IDesktopApp {

    private final BlockPos host;
    private final BlockPos monitor;
    private final MediaPlayerLook look;
    private final MediaFrames frames;
    private final FileDialog dialog;
    private final ContextMenu menu = new ContextMenu(MENU_W, MENU_ITEM_H);
    private final MediaPlayerPainter painter;
    private final List<Hit> hits = new ArrayList<>();
    private OsSkin skin = OsSkin.fallback();
    @Nullable
    private MediaLibraryPayload library;
    private Mode mode = Mode.SONG;
    private MediaLibrary.Sequence sequence = MediaLibrary.Sequence.NONE;
    private int frame;
    private boolean running;
    private long lastTick;
    private double owed;
    private int fps = DEFAULT_FPS;
    private int view;
    private boolean filled;
    private boolean closing;
    private String refused = "";
    private boolean speedOpen;
    private String wanted = "";
    private String startAt = "";
    private boolean addOnly;
    @Nullable
    private MediaFrames.Frame shown;
    private long lastAsk;
    private long lastLibrary;
    private int lastX;
    private int lastY;
    private int lastW;
    private int lastH;

    /** Pictures a second at first, and the slowest and fastest the options allow. */
    static final int DEFAULT_FPS = 12;
    static final int SLOWEST = 1;
    static final int FASTEST = 20;
    /** What a path from the Files menu starts with to be added to the list rather than played. */
    public static final String ADD = "add:";
    /** The program's id, as the desktop opens it. */
    public static final String PROGRAM = "jsc:media_player";
    private static final int MENU_W = 120;
    private static final int MENU_ITEM_H = 12;
    private static final long ASK_MS = 1_000L;
    private static final long ASK_LIBRARY_MS = 10_000L;
    /* How many pictures ahead of the one shown are asked for. */
    private static final int AHEAD = 8;

    private static final List<MediaPlayerApp> OPEN = new ArrayList<>();

    public MediaPlayerApp(final BlockPos host, final BlockPos monitor, @Nullable final ResourceLocation desktop) {
        this.host = host;
        this.monitor = monitor;
        this.look = MediaPlayerLook.of(desktop);
        this.frames = new MediaFrames(host);
        this.dialog = new FileDialog(host, this);
        this.painter = new MediaPlayerPainter(this);
        OPEN.add(this);
        askLibrary("");
    }

    /** What the playing modes are: the computer's songs, or an image sequence. */
    enum Mode { SONG, SEQUENCE }

    /** What a place of the window does when clicked, laid out anew every frame by the painter. */
    enum Kind { PLAY_PAUSE, STOP, PREVIOUS, NEXT, BACK, AHEAD, OPEN, SHUFFLE, REPEAT, FILL, SEEK, VOLUME, MENU, VIEW,
        SONG, SEQUENCE, PLAYLIST, SPEED }

    /** One clickable place, and what clicking it does. */
    record Hit(Kind kind, int index, int x, int y, int w, int h) {

        boolean contains(final double mx, final double my) {
            return mx >= x && my >= y && mx < x + w && my < y + h;
        }
    }

    /** Routes what the computer can play to its open players. */
    public static void accept(final MediaLibraryPayload payload) {
        for (final MediaPlayerApp app : List.copyOf(OPEN)) {
            if (app.host.equals(payload.hostPos())) {
                app.took(payload);
            }
        }
    }

    /** Routes a picture to the open players that asked for it. */
    public static void accept(final MediaFramePayload payload) {
        for (final MediaPlayerApp app : List.copyOf(OPEN)) {
            app.frames.accept(payload);
        }
    }

    @Override
    public void openFile(final String path) {
        if (path.startsWith(ADD)) {
            addOnly = true;
            open(path.substring(ADD.length()));
        } else {
            addOnly = false;
            open(path);
        }
    }

    @Override
    public void onRestored() {
        if (!OPEN.contains(this)) {
            OPEN.add(this);
        }
        askLibrary("");
    }

    @Override
    public void onClosed() {
        OPEN.remove(this);
        frames.clear();
    }

    @Override
    public boolean wantsToClose() {
        return closing;
    }

    @Override
    public String saveState() {
        return fps + ";" + sequence.folder();
    }

    @Override
    public void restoreState(final String state) {
        final int semi = state.indexOf(';');
        try {
            fps = Math.max(SLOWEST, Math.min(FASTEST, Integer.parseInt(semi < 0 ? state : state.substring(0, semi))));
        } catch (final NumberFormatException unreadable) {
            fps = DEFAULT_FPS;
        }
        if (semi >= 0 && semi + 1 < state.length()) {
            open(state.substring(semi + 1));
        }
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public String title() {
        final String name = name();
        final String what = playingName();
        return what.isEmpty() || look == MediaPlayerLook.FRAMES_XP || look == MediaPlayerLook.FRAMES_7
                || look == MediaPlayerLook.FRAMES_10 || look == MediaPlayerLook.FRAMES_11
                || look == MediaPlayerLook.GNOME ? name
                : look == MediaPlayerLook.CINNAMON || look == MediaPlayerLook.QUINCE ? what : what + " - " + name;
    }

    @Override
    public int defaultWidth() {
        return look.width();
    }

    @Override
    public int defaultHeight() {
        return look.height();
    }

    @Override
    public int minWidth() {
        return Math.min(look.width(), 200);
    }

    @Override
    public int minHeight() {
        return Math.min(look.height(), 140);
    }

    @Override
    public boolean waiting() {
        return library == null;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        lastX = x;
        lastY = y;
        lastW = width;
        lastH = height;
        final long now = Util.getMillis();
        if (now - lastAsk >= ASK_MS) {
            lastAsk = now;
            PacketDistributor.sendToServer(SoundfoundryActionPayload.of(host, SoundfoundryActionPayload.LOOK,
                    SoundfoundryStates.revisionOf(host)));
        }
        // The library is asked again now and then, so songs and pictures brought while it is open show up.
        if (now - lastLibrary >= ASK_LIBRARY_MS) {
            lastLibrary = now;
            askLibrary("");
        }
        advance(now);
        hits.clear();
        painter.paint(g, new UiContext(skin, font, mouseX, mouseY, partialTick), x, y, width, height);
        if (menu.isOpen()) {
            menu.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
        }
    }

    @Override
    public boolean modalActive() {
        return !refused.isEmpty() || speedOpen;
    }

    @Override
    public void renderModal(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                            final int height, final int mouseX, final int mouseY) {
        painter.paintModal(g, new UiContext(skin, font, mouseX, mouseY, 0f), x, y, width, height);
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        if (modalActive()) {
            final Hit hit = hitAt(mouseX, mouseY);
            if (hit != null && hit.kind() == Kind.SPEED) {
                speed(hit.index());
            }
            return;
        }
        if (menu.isOpen()) {
            if (!menu.mouseClicked(mouseX, mouseY, button)) {
                menu.close();
            }
            return;
        }
        if (button == 1) {
            openMenu(MediaPlayerTexts.FILE, (int) mouseX, (int) mouseY);
            return;
        }
        final Hit hit = hitAt(mouseX, mouseY);
        if (hit != null) {
            act(hit, mouseX);
        }
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        final Hit hit = hitAt(mouseX, mouseY);
        if (!modalActive() && hit != null && (hit.kind() == Kind.SEEK || hit.kind() == Kind.VOLUME)) {
            act(hit, mouseX);
        }
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return menu.isOpen() && menu.keyPressed(key, scanCode, modifiers);
    }

    /** The dialog this player opens files with, which the desktop shows as its own window. */
    public FileDialog dialog() {
        return dialog;
    }

    // what the painter reads

    /** What this desktop calls the player: Frames Media Player, Dragon Player, Videos and the rest. */
    String name() {
        final String name = ActiveDesktop.windowName(PROGRAM);
        return name.equals(PROGRAM) ? ActiveDesktop.openerName(Programs.MEDIA_PLAYER.getPath()) : name;
    }

    MediaPlayerLook look() {
        return look;
    }

    OsSkin skin() {
        return skin;
    }

    Mode mode() {
        return mode;
    }

    int view() {
        return view;
    }

    boolean filled() {
        return filled;
    }

    int fps() {
        return fps;
    }

    int frame() {
        return frame;
    }

    boolean running() {
        return mode == Mode.SEQUENCE ? running : songStatus() == SoundfoundryStatePayload.PLAYING;
    }

    MediaLibrary.Sequence sequence() {
        return sequence;
    }

    String refused() {
        return refused;
    }

    boolean speedOpen() {
        return speedOpen;
    }

    List<MediaLibrary.Song> songs() {
        return library == null ? List.of() : library.songs();
    }

    List<MediaLibrary.Sequence> sequences() {
        return library == null ? List.of() : library.sequences();
    }

    /** The computer's playlist, as its player holds it. */
    List<SoundfoundryStatePayload.Song> playlist() {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        return known == null ? List.of() : known.songs();
    }

    /** The playlist's song now playing or paused, or -1. */
    int current() {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        return known == null || known.state().status() == SoundfoundryStatePayload.STOPPED ? -1
                : known.state().current();
    }

    /** The picture to show now: the one playing, or the last that came while the next is on its way. */
    @Nullable
    MediaFrames.Frame picture() {
        if (mode != Mode.SEQUENCE || sequence.frames().isEmpty()) {
            return null;
        }
        final MediaFrames.Frame now = frames.get(sequence.frames().get(Math.min(frame, sequence.frames().size() - 1)));
        if (now != null && now.texture() != null) {
            shown = now;
        }
        return shown;
    }

    /** The first picture of a sequence, for its tile, asked for when not here. */
    @Nullable
    MediaFrames.Frame thumbnail(final MediaLibrary.Sequence one) {
        return one.frames().isEmpty() ? null : frames.get(one.frames().getFirst());
    }

    /** What is playing, by its name: the song's file or the sequence's folder; empty for nothing. */
    String playingName() {
        if (mode == Mode.SEQUENCE) {
            return sequence.folder().isEmpty() ? "" : FsPaths.fileName(sequence.folder());
        }
        final int now = current();
        final List<SoundfoundryStatePayload.Song> list = playlist();
        return now >= 0 && now < list.size() ? FsPaths.fileName(list.get(now).path()) : "";
    }

    /** How far through what plays, from 0 to 1. */
    double progress() {
        if (mode == Mode.SEQUENCE) {
            final int n = sequence.frames().size();
            return n <= 1 ? 0.0 : frame / (double) (n - 1);
        }
        final long length = songLength();
        return length <= 0 ? 0.0 : Math.min(1.0, songPosition() / (double) length);
    }

    /** Where what plays is, in milliseconds: the song's position, or the picture's place at its rate. */
    long elapsed() {
        return mode == Mode.SEQUENCE ? frame * 1000L / fps : songPosition();
    }

    /** How long what plays lasts, in milliseconds. */
    long length() {
        return mode == Mode.SEQUENCE ? sequence.frames().size() * 1000L / fps : songLength();
    }

    /** The volume of the computer's player, from 0 to 1. */
    double volume() {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        return known == null ? 0.75 : known.state().volume() / (double) SoundfoundryState.MAX_VOLUME;
    }

    /** A sequence's line: how many pictures and how long it plays at this rate. */
    String sequenceLine(final MediaLibrary.Sequence one) {
        return GameText.resolve(MediaPlayerTexts.PICTURES.with(one.frames().size(),
                clock(one.frames().size() * 1000L / fps)));
    }

    /** Marks a clickable place of the frame being drawn. */
    void hit(final Kind kind, final int index, final int x, final int y, final int w, final int h) {
        hits.add(new Hit(kind, index, x, y, w, h));
    }

    /** A time as a clock reads it, minutes and seconds: 01:12. */
    static String clock(final long millis) {
        final long seconds = Math.max(0L, millis / 1000L);
        return String.format(Locale.ROOT, "%02d:%02d", seconds / 60L, seconds % 60L);
    }

    // actions

    private void took(final MediaLibraryPayload payload) {
        library = payload;
        if (!wanted.isEmpty() && payload.asked().folder().equals(wanted)) {
            wanted = "";
            if (!addOnly && !payload.asked().frames().isEmpty()) {
                play(payload.asked());
                // A picture opened on its own is shown, still, among its folder's.
                final int at = payload.asked().frames().indexOf(startAt);
                if (at >= 0) {
                    frame = at;
                    running = false;
                }
            }
            startAt = "";
        }
    }

    private void askLibrary(final String folder) {
        PacketDistributor.sendToServer(new MediaRequestPayload(host, MediaRequestPayload.LIBRARY, folder));
    }

    /*
     * A path from Files or from the Open dialog: a song is played, or added to the list; a picture shows its folder
     * from itself; a folder plays as a sequence, and its songs are played, or added, through the computer's player.
     */
    private void open(final String path) {
        final FileType type = FileType.ofPath(path);
        if (type.sound()) {
            if (addOnly) {
                send(SoundfoundryActionPayload.ADD, 0, List.of(path));
            } else {
                playSong(path);
            }
            return;
        }
        if (type == FileType.PIX || type == FileType.PNG) {
            wanted = FsPaths.parentDir(path);
            startAt = path;
            askLibrary(wanted);
            return;
        }
        wanted = path;
        startAt = "";
        askLibrary(path);
        send(SoundfoundryActionPayload.ADD_FOLDER, 0, List.of(path));
    }

    private void playSong(final String path) {
        if (library != null && !library.sound()) {
            refused = FsPaths.fileName(path);
            return;
        }
        mode = Mode.SONG;
        running = false;
        send(SoundfoundryActionPayload.ADD, SoundfoundryActionPayload.AND_PLAY, List.of(path));
    }

    private void play(final MediaLibrary.Sequence one) {
        mode = Mode.SEQUENCE;
        sequence = one;
        frame = 0;
        owed = 0.0;
        shown = null;
        running = true;
        lastTick = Util.getMillis();
        // A sequence plays in silence, so whatever song was on stops.
        if (songStatus() == SoundfoundryStatePayload.PLAYING) {
            send(SoundfoundryActionPayload.PAUSE, 0, List.of());
        }
    }

    private void act(final Hit hit, final double mouseX) {
        switch (hit.kind()) {
            case PLAY_PAUSE -> playPause();
            case STOP -> stop();
            case PREVIOUS -> step(-1);
            case NEXT -> step(1);
            case BACK -> jump(-1);
            case AHEAD -> jump(1);
            case OPEN -> chooseFile();
            case SHUFFLE -> send(SoundfoundryActionPayload.SHUFFLE, shuffled() ? 0 : 1, List.of());
            case REPEAT -> send(SoundfoundryActionPayload.REPEAT, repeated() ? 0 : 1, List.of());
            case FILL -> filled = !filled;
            case SEEK -> seek((mouseX - hit.x()) / Math.max(1, hit.w()));
            case VOLUME -> send(SoundfoundryActionPayload.VOLUME, (int) Math.round(Math.max(0.0, Math.min(1.0,
                    (mouseX - hit.x()) / Math.max(1, hit.w()))) * SoundfoundryState.MAX_VOLUME), List.of());
            case MENU -> openMenu(look.menus().get(hit.index()), hit.x(), hit.y() + hit.h());
            case VIEW -> view = hit.index();
            case SONG -> playSong(songs().get(hit.index()).path());
            case SEQUENCE -> play(sequences().get(hit.index()));
            case PLAYLIST -> {
                mode = Mode.SONG;
                running = false;
                send(SoundfoundryActionPayload.PLAY, hit.index(), List.of());
            }
            case SPEED -> speed(hit.index());
        }
    }

    private void playPause() {
        if (mode == Mode.SEQUENCE) {
            if (!running && frame >= sequence.frames().size() - 1) {
                frame = 0;
            }
            running = !running;
            lastTick = Util.getMillis();
            return;
        }
        if (songStatus() == SoundfoundryStatePayload.PLAYING) {
            send(SoundfoundryActionPayload.PAUSE, 0, List.of());
        } else if (library != null && !library.sound()) {
            refused = playingName().isEmpty() ? "" : playingName();
        } else {
            send(SoundfoundryActionPayload.PLAY, -1, List.of());
        }
    }

    private void stop() {
        if (mode == Mode.SEQUENCE) {
            running = false;
            frame = 0;
        } else {
            send(SoundfoundryActionPayload.STOP, 0, List.of());
        }
    }

    /* The next or the previous: song of the list, or sequence of the library. */
    private void step(final int by) {
        if (mode == Mode.SONG) {
            send(by > 0 ? SoundfoundryActionPayload.NEXT : SoundfoundryActionPayload.PREVIOUS, 0, List.of());
            return;
        }
        final List<MediaLibrary.Sequence> all = sequences();
        int at = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).folder().equals(sequence.folder())) {
                at = i;
            }
        }
        if (!all.isEmpty()) {
            play(all.get(Math.floorMod(at + by, all.size())));
        }
    }

    /* Back or ahead by a tenth of what plays. */
    private void jump(final int by) {
        seek(progress() + by * 0.1);
    }

    private void seek(final double where) {
        final double at = Math.max(0.0, Math.min(1.0, where));
        if (mode == Mode.SEQUENCE) {
            frame = (int) Math.round(at * Math.max(0, sequence.frames().size() - 1));
            owed = 0.0;
        } else if (songLength() > 0) {
            send(SoundfoundryActionPayload.SEEK, 0, List.of(), (long) (at * songLength()));
        }
    }

    private void speed(final int choice) {
        switch (choice) {
            case MediaPlayerPainter.SLOWER -> fps = Math.max(SLOWEST, fps - 1);
            case MediaPlayerPainter.FASTER -> fps = Math.min(FASTEST, fps + 1);
            default -> {
                speedOpen = false;
                refused = "";
            }
        }
    }

    private void chooseFile() {
        dialog.openFile(MediaPlayerTexts.OPEN_TITLE.text(), "",
                List.of(FileDialog.Filter.of(MediaPlayerTexts.MEDIA_FILES, "ogg", "wav", PixImage.EXTENSION, "png"),
                        FileDialog.Filter.ALL),
                picked -> {
                    addOnly = false;
                    open(picked);
                });
    }

    private void openMenu(final TextKey which, final int x, final int y) {
        final List<ContextMenu.Item> items = new ArrayList<>();
        if (which == MediaPlayerTexts.FILE) {
            items.add(new ContextMenu.Item(GameText.resolve(MediaPlayerTexts.OPEN), true, this::chooseFile));
            items.add(new ContextMenu.Item(GameText.resolve(MediaPlayerTexts.PLAY_PAUSE), true, this::playPause));
            items.add(new ContextMenu.Item(GameText.resolve(MediaPlayerTexts.STOP), true, this::stop));
            items.add(new ContextMenu.Item(GameText.resolve(MediaPlayerTexts.SPEED), true, () -> speedOpen = true));
            items.add(ContextMenu.Item.separator());
            items.add(new ContextMenu.Item(GameText.resolve(MediaPlayerTexts.CLOSE), true, () -> closing = true));
        } else if (which == MediaPlayerTexts.PLAY) {
            items.add(new ContextMenu.Item(GameText.resolve(MediaPlayerTexts.PLAY_PAUSE), true, this::playPause));
            items.add(new ContextMenu.Item(GameText.resolve(MediaPlayerTexts.STOP), true, this::stop));
        } else if (which == MediaPlayerTexts.HELP) {
            items.add(new ContextMenu.Item(GameText.resolve(MediaPlayerTexts.HELP), true,
                    () -> DesktopScreen.requestOpen("jsc:help_viewer")));
        } else {
            items.add(new ContextMenu.Item(GameText.resolve(MediaPlayerTexts.SPEED), true, () -> speedOpen = true));
        }
        menu.open(items, x, y, lastX, lastY, lastW, lastH);
    }

    /* The pictures of a running sequence move on at its rate, and the next few are asked for ahead. */
    private void advance(final long now) {
        if (mode != Mode.SEQUENCE || sequence.frames().isEmpty()) {
            return;
        }
        if (running) {
            owed += (now - lastTick) * fps / 1000.0;
            final int moved = (int) owed;
            owed -= moved;
            frame += moved;
            if (frame >= sequence.frames().size() - 1) {
                frame = sequence.frames().size() - 1;
                running = false;
            }
        }
        lastTick = now;
        for (int i = 1; i <= AHEAD && frame + i < sequence.frames().size(); i++) {
            frames.ask(sequence.frames().get(frame + i));
        }
    }

    @Nullable
    private Hit hitAt(final double mx, final double my) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(mx, my)) {
                return hits.get(i);
            }
        }
        return null;
    }

    private int songStatus() {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        return known == null ? SoundfoundryStatePayload.STOPPED : known.state().status();
    }

    private long songPosition() {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        return known == null ? 0L : known.position();
    }

    private long songLength() {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        return known == null ? 0L : known.state().playing().millis();
    }

    private boolean shuffled() {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        return known != null && known.state().shuffle();
    }

    private boolean repeated() {
        final SoundfoundryStates.Known known = SoundfoundryStates.of(host);
        return known != null && known.state().repeat();
    }

    private void send(final int action, final int index, final List<String> paths) {
        send(action, index, paths, 0L);
    }

    private void send(final int action, final int index, final List<String> paths, final long value) {
        PacketDistributor.sendToServer(new SoundfoundryActionPayload(host, action, index, value, paths, List.of()));
    }
}
