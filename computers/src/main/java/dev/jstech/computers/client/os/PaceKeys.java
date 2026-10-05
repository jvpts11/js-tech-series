/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.os.Platform;
import dev.jstech.computers.program.cli.menushell.MenuShellListing;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * PACE on a terminal it has taken whole: Bellwether Labs' menus for UNIX System V, each choice opening a numbered
 * frame over the one it was made in. The function keys are those on the bottom line: HELP, ENTER, PREV-FRM, NEXT-FRM,
 * CANCEL, CMD-MENU. The UNIX System, and anything run from a frame, is a shell of its own over the frames until it is
 * done. The screen is drawn by {@link PacePainter}; the machine is asked for its folders by name, as a file is.
 */
public final class PaceKeys implements TtyEditor.IKeys {

    private final List<Frame> frames = new ArrayList<>();
    /** The frame in front, by its place in the order the frames were opened. */
    private int active;
    private List<MenuShellListing.Program> programs = List.of();
    @Nullable
    private TaskPrompt task;
    /** What is being typed at the command line, or null while it is not being typed at. */
    @Nullable
    private StringBuilder commandLine;
    @Nullable
    private TtyEditor editor;
    /** The cells the glass held when it was last drawn, which is where a click is read against. */
    private int columns = DEFAULT_COLUMNS;
    private int rows = DEFAULT_ROWS;

    private static final int DEFAULT_COLUMNS = 80;
    private static final int DEFAULT_ROWS = 25;
    /** The player whose office it is, the name of their home under /usr. */
    private static final String PLAYER = "player";
    private static final String HOME = "/usr/" + PLAYER;
    private static final String USERS = "/usr";
    private static final String WASTEBASKET = HOME + "/WASTEBASKET";
    /** The folders of /usr that are the system's and not anybody's home. */
    private static final Set<String> NOT_HOMES = Set.of("bin", "lib", PLAYER);

    /** What a frame holds. */
    enum Kind {
        MAIN, OFFICE, FOLDER, PROGRAMS, ADMINISTRATION, USERS, COMMANDS, HELP
    }

    @Override
    public String status(final TtyEditor editor) {
        return "";
    }

    @Override
    public void opened(final TtyEditor editor, final boolean existed) {
        this.editor = editor;
        this.programs = MenuShellListing.read(editor.document().text()).programs();
        frames.add(mainFrame());
        active = 0;
    }

    @Override
    @Nullable
    public TtyEditor inner(final TtyEditor editor) {
        settleTask();
        return task != null ? task.editor() : null;
    }

    @Override
    public TextScreen screen(final TtyEditor editor, final int columns, final int rows) {
        this.editor = editor;
        this.columns = columns;
        this.rows = rows;
        settleTask();
        return PacePainter.paint(this, columns, rows);
    }

    @Override
    public boolean key(final TtyEditor editor, final int key, final int modifiers) {
        this.editor = editor;
        if (task != null) {
            task.key(key, modifiers);
            settleTask();
            return true;
        }
        if (commandLine != null) {
            commandKey(key);
            return true;
        }
        final Frame front = front();
        switch (key) {
            case GLFW.GLFW_KEY_F1 -> help();
            case GLFW.GLFW_KEY_F2, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> open(front);
            case GLFW.GLFW_KEY_F3 -> active = Math.floorMod(active - 1, frames.size());
            case GLFW.GLFW_KEY_F4 -> active = Math.floorMod(active + 1, frames.size());
            case GLFW.GLFW_KEY_F5, GLFW.GLFW_KEY_ESCAPE -> cancel();
            case GLFW.GLFW_KEY_F6 -> commandMenu(front);
            case GLFW.GLFW_KEY_UP -> front.select(front.selected - 1);
            case GLFW.GLFW_KEY_DOWN -> front.select(front.selected + 1);
            case GLFW.GLFW_KEY_HOME -> front.select(0);
            case GLFW.GLFW_KEY_END -> front.select(front.items.size() - 1);
            case GLFW.GLFW_KEY_PAGE_UP -> front.select(front.selected - PacePainter.FRAME_PAGE);
            case GLFW.GLFW_KEY_PAGE_DOWN -> front.select(front.selected + PacePainter.FRAME_PAGE);
            default -> {
                // A key PACE has no use for.
            }
        }
        return true;
    }

    @Override
    public boolean typed(final TtyEditor editor, final char c) {
        if (task != null) {
            task.typed(c);
            return true;
        }
        if (commandLine != null) {
            if (c >= ' ') {
                commandLine.append(c);
            }
            return true;
        }
        // A letter moves to the next item it starts.
        final Frame front = front();
        final String letter = String.valueOf(Character.toUpperCase(c));
        for (int i = 1; i <= front.items.size(); i++) {
            final int at = (front.selected + i) % front.items.size();
            if (GameText.resolve(front.items.get(at).label()).toUpperCase(Locale.ROOT).startsWith(letter)) {
                front.select(at);
                break;
            }
        }
        return true;
    }

    @Override
    public boolean clicked(final TtyEditor editor, final int row, final int column) {
        if (task != null || commandLine != null) {
            return true;
        }
        // A click on a frame brings it to the front; on one of its items, picks it, and a second click opens it.
        final List<Frame> drawn = drawOrder();
        for (int i = drawn.size() - 1; i >= 0; i--) {
            final Frame frame = drawn.get(i);
            final int[] box = PacePainter.box(frame, frames.indexOf(frame), columns, rows);
            if (column >= box[0] && column < box[0] + box[2] && row >= box[1] && row < box[1] + box[3]) {
                final boolean wasFront = frames.indexOf(frame) == active;
                active = frames.indexOf(frame);
                final int item = frame.top + row - box[1] - 1;
                if (item >= 0 && item < frame.items.size()) {
                    final boolean again = wasFront && item == frame.selected;
                    frame.select(item);
                    if (again) {
                        open(frame);
                    }
                }
                return true;
            }
        }
        return true;
    }

    /* Readings for the painter and for tests */

    List<Frame> frames() {
        return frames;
    }

    int active() {
        return active;
    }

    /** The frame in front. */
    Frame front() {
        return frames.get(Math.max(0, Math.min(frames.size() - 1, active)));
    }

    /** The frames in the order they are drawn: as they were opened, the one in front last, over the others. */
    List<Frame> drawOrder() {
        final List<Frame> out = new ArrayList<>(frames);
        final Frame front = front();
        out.remove(front);
        out.add(front);
        return out;
    }

    @Nullable
    TaskPrompt task() {
        return task;
    }

    /** Whether a shell started from a frame, rather than the frames, is in front, for a test. */
    public boolean taskInFront() {
        return task != null;
    }

    /** What is being typed at the command line, or null while it is not being typed at. */
    @Nullable
    String commandLine() {
        return commandLine == null ? null : commandLine.toString();
    }

    /** What the line above the keys says about the item picked in the frame in front. */
    Text says() {
        if (commandLine != null) {
            return PaceTexts.SAYS_COMMAND.text();
        }
        final Frame front = front();
        if (front.items.isEmpty()) {
            return PaceTexts.MOVE_AND_ENTER.text();
        }
        return front.items.get(Math.max(0, Math.min(front.items.size() - 1, front.selected))).says();
    }

    /* The frames */

    private Frame mainFrame() {
        final List<Item> items = new ArrayList<>();
        items.add(item(PaceTexts.OFFICE.with(PLAYER), () -> openFrame(officeFrame())));
        items.add(item(PaceTexts.PROGRAMS.text(), () -> openFrame(programsFrame())));
        items.add(item(PaceTexts.ADMINISTRATION.text(), () -> openFrame(administrationFrame())));
        items.add(item(PaceTexts.UNIX_SYSTEM.text(), this::unixSystem));
        items.add(item(PaceTexts.EXIT.text(), this::quit));
        return new Frame(Kind.MAIN, PaceTexts.NAME.text(), "", items);
    }

    private Frame officeFrame() {
        final List<Item> items = new ArrayList<>();
        items.add(item(PaceTexts.FILECABINET.text(),
                () -> openFolder(PaceTexts.FILECABINET.text(), HOME)));
        items.add(item(PaceTexts.WASTEBASKET.text(),
                () -> openFolder(PaceTexts.WASTEBASKET.text(), WASTEBASKET)));
        items.add(item(PaceTexts.OTHER_USERS.text(), this::openUsers));
        items.add(new Item(PaceTexts.PREFERENCES.text(), Text.EMPTY, PaceTexts.SAYS_NOTHING_HERE.text(), () -> { },
                false, null));
        return new Frame(Kind.OFFICE, PaceTexts.OFFICE.with(PLAYER), "", items);
    }

    private Frame programsFrame() {
        final List<Item> items = new ArrayList<>();
        for (final MenuShellListing.Program program : programs) {
            items.add(new Item(Text.literal(program.label()), Text.literal(program.command()),
                    PaceTexts.SAYS_EXECUTABLE.with(program.command()),
                    () -> run(Text.literal(program.label()), program.command()), true, null));
        }
        return new Frame(Kind.PROGRAMS, PaceTexts.PROGRAMS.text(), "", items);
    }

    private Frame administrationFrame() {
        final List<Item> items = new ArrayList<>();
        items.add(item(PaceTexts.MACHINE.text(), () -> run(PaceTexts.MACHINE.text(), "uname -a")));
        items.add(item(PaceTexts.FILE_SYSTEMS.text(), () -> run(PaceTexts.FILE_SYSTEMS.text(), "df")));
        items.add(item(PaceTexts.SOFTWARE.text(), () -> run(PaceTexts.SOFTWARE.text(), "installpkg")));
        return new Frame(Kind.ADMINISTRATION, PaceTexts.ADMINISTRATION.text(), "", items);
    }

    private void openFolder(final Text name, final String path) {
        final Frame frame = new Frame(Kind.FOLDER, PaceTexts.FOLDER_TITLE.with(name, path), path,
                List.of(new Item(PaceTexts.READING.text(), Text.EMPTY, PaceTexts.MOVE_AND_ENTER.text(), () -> { },
                        false, null)));
        openFrame(frame);
        read(path, listing -> {
            final List<Item> items = new ArrayList<>();
            for (final MenuShellListing.Entry entry : listing.found() ? listing.entries()
                    : List.<MenuShellListing.Entry>of()) {
                items.add(folderItem(path, entry));
            }
            if (items.isEmpty()) {
                items.add(new Item(PaceTexts.EMPTY.text(), Text.EMPTY, PaceTexts.MOVE_AND_ENTER.text(), () -> { },
                        false, null));
            }
            frame.items = items;
            frame.select(0);
        });
    }

    private Item folderItem(final String path, final MenuShellListing.Entry entry) {
        final String name = entry.fullName();
        final String whole = path.endsWith("/") ? path + name : path + "/" + name;
        if (entry.folder()) {
            return new Item(Text.literal(name), PaceTexts.DIRECTORY.text(), PaceTexts.SAYS_DIRECTORY.with(name),
                    () -> openFolder(Text.literal(name), whole), true, null);
        }
        final String ext = entry.ext().toLowerCase(Locale.ROOT);
        if ("sg".equals(ext)) {
            return new Item(Text.literal(name), PaceTexts.SIGMA_LISTING.text(), PaceTexts.SAYS_SIGMA.with(name),
                    () -> edit(name, whole), true, () -> run(Text.literal(name), "sigma " + whole));
        }
        if ("sgs".equals(ext)) {
            return new Item(Text.literal(name), PaceTexts.SIGMA_SHARP_LISTING.text(),
                    PaceTexts.SAYS_SIGMA_SHARP.with(name), () -> edit(name, whole), true, null);
        }
        if (path.startsWith("/usr/bin") || path.startsWith("/bin")) {
            return new Item(Text.literal(name), PaceTexts.EXECUTABLE.text(), PaceTexts.SAYS_EXECUTABLE.with(name),
                    () -> run(Text.literal(name), whole), true, null);
        }
        return new Item(Text.literal(name), PaceTexts.STANDARD_FILE.text(), PaceTexts.SAYS_FILE.with(name),
                () -> edit(name, whole), true, null);
    }

    private void openUsers() {
        final Frame frame = new Frame(Kind.USERS, PaceTexts.OTHER_USERS.text(), USERS,
                List.of(new Item(PaceTexts.READING.text(), Text.EMPTY, PaceTexts.MOVE_AND_ENTER.text(), () -> { },
                        false, null)));
        openFrame(frame);
        read(USERS, listing -> {
            final List<Item> items = new ArrayList<>();
            for (final MenuShellListing.Entry entry : listing.entries()) {
                if (entry.folder() && !NOT_HOMES.contains(entry.fullName().toLowerCase(Locale.ROOT))) {
                    final String user = entry.fullName();
                    items.add(item(Text.literal(user), () -> openFolder(PaceTexts.OFFICE.with(user),
                            USERS + "/" + user)));
                }
            }
            if (items.isEmpty()) {
                items.add(new Item(PaceTexts.NO_OTHER_USERS.text(), Text.EMPTY, PaceTexts.MOVE_AND_ENTER.text(),
                        () -> { }, false, null));
            }
            frame.items = items;
            frame.select(0);
        });
    }

    private void help() {
        final Frame front = front();
        final List<Item> items = new ArrayList<>();
        for (final TextKey page : List.of(PaceTexts.HELP_MAIN, PaceTexts.HELP_FRAMES)) {
            for (final String line : GameText.resolve(page).split("\n")) {
                items.add(new Item(Text.literal(line), Text.EMPTY, PaceTexts.MOVE_AND_ENTER.text(), () -> { }, false,
                        null));
            }
        }
        openFrame(new Frame(Kind.HELP, PaceTexts.HELP_TITLE.with(front.title), "", items));
    }

    private void commandMenu(final Frame front) {
        final Item picked = front.items.isEmpty() ? null
                : front.items.get(Math.max(0, Math.min(front.items.size() - 1, front.selected)));
        if (picked != null && picked.run() != null) {
            picked.run().run();
            return;
        }
        if (front.kind == Kind.COMMANDS) {
            closeFront();
            return;
        }
        final List<Item> items = new ArrayList<>();
        items.add(item(PaceTexts.CANCEL.text(), () -> {
            closeFront();
            cancel();
        }));
        items.add(item(PaceTexts.QUIT.text(), this::quit));
        items.add(item(PaceTexts.HELP.text(), () -> {
            closeFront();
            help();
        }));
        items.add(item(PaceTexts.NEXT.text(), () -> {
            closeFront();
            active = Math.floorMod(active + 1, frames.size());
        }));
        items.add(item(PaceTexts.PREV.text(), () -> {
            closeFront();
            active = Math.floorMod(active - 1, frames.size());
        }));
        items.add(item(PaceTexts.REFRESH.text(), () -> {
            closeFront();
            refresh();
        }));
        items.add(item(PaceTexts.RUN.text(), () -> {
            closeFront();
            commandLine = new StringBuilder();
        }));
        items.add(item(PaceTexts.UNIX.text(), () -> {
            closeFront();
            unixSystem();
        }));
        openFrame(new Frame(Kind.COMMANDS, PaceTexts.COMMAND_MENU.text(), "", items));
    }

    private void refresh() {
        final Frame front = front();
        if (front.kind == Kind.FOLDER) {
            final Text title = front.title;
            frames.remove(front);
            active = Math.max(0, frames.size() - 1);
            openFolder(Text.literal(GameText.resolve(title)), front.path);
        }
    }

    private void commandKey(final int key) {
        final StringBuilder line = commandLine;
        if (line == null) {
            return;
        }
        switch (key) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                commandLine = null;
                final String typed = line.toString().trim();
                if (!typed.isEmpty()) {
                    run(Text.literal(typed), typed);
                }
            }
            case GLFW.GLFW_KEY_ESCAPE, GLFW.GLFW_KEY_F5 -> commandLine = null;
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!line.isEmpty()) {
                    line.setLength(line.length() - 1);
                }
            }
            default -> {
                // Only what is typed goes into the line.
            }
        }
    }

    private void open(final Frame front) {
        if (front.items.isEmpty()) {
            return;
        }
        final Item picked = front.items.get(Math.max(0, Math.min(front.items.size() - 1, front.selected)));
        if (picked.enabled()) {
            picked.open().run();
        }
    }

    private void cancel() {
        if (frames.size() <= 1) {
            quit();
            return;
        }
        closeFront();
    }

    private void closeFront() {
        if (frames.size() <= 1) {
            return;
        }
        frames.remove(Math.max(0, Math.min(frames.size() - 1, active)));
        active = frames.size() - 1;
    }

    private void openFrame(final Frame frame) {
        frames.add(frame);
        active = frames.size() - 1;
    }

    private void quit() {
        if (task != null) {
            task.release();
            task = null;
        }
        if (editor != null) {
            editor.quit();
        }
    }

    /* Shells over the frames */

    private void unixSystem() {
        startTask(PaceTexts.UNIX_SYSTEM.text(), null);
    }

    private void edit(final String name, final String whole) {
        startTask(Text.literal(name), "vi " + whole);
    }

    private void run(final Text name, final String line) {
        startTask(name, line);
    }

    private void startTask(final Text name, @Nullable final String line) {
        final BlockPos host = editor == null ? null : editor.machine();
        if (host == null) {
            return;
        }
        task = new TaskPrompt(host, GameText.resolve(name), Platform.UNIX, line,
                line == null ? null : PaceTexts.RETURN.text());
    }

    private void settleTask() {
        if (task != null && task.ended()) {
            task = null;
        }
    }

    private void read(final String path, final Consumer<MenuShellListing> then) {
        if (editor == null) {
            return;
        }
        editor.read(MenuShellListing.view(path).substring(MenuShellListing.SCHEME.length()),
                (content, existed) -> then.accept(MenuShellListing.read(content)));
    }

    private static Item item(final Text label, final Runnable open) {
        return new Item(label, Text.EMPTY, PaceTexts.MOVE_AND_ENTER.text(), open, true, null);
    }

    /**
     * One item of a frame.
     *
     * @param label   what it says
     * @param detail  what is written beside it, a file's type, or nothing
     * @param says    what the line above the keys says while it is picked
     * @param open    what ENTER on it does
     * @param enabled whether there is anything behind it on this machine
     * @param run     what CMD-MENU does on it instead of opening the commands, or null
     */
    record Item(Text label, Text detail, Text says, Runnable open, boolean enabled, @Nullable Runnable run) {
    }

    /** A frame: what it holds, its title, the folder it shows when it shows one, its items and the one picked. */
    static final class Frame {

        private final Kind kind;
        private final Text title;
        private final String path;
        private List<Item> items;
        private int selected;
        private int top;

        Frame(final Kind kind, final Text title, final String path, final List<Item> items) {
            this.kind = kind;
            this.title = title;
            this.path = path;
            this.items = List.copyOf(items);
        }

        Kind kind() {
            return kind;
        }

        Text title() {
            return title;
        }

        List<Item> items() {
            return items;
        }

        int selected() {
            return selected;
        }

        int top() {
            return top;
        }

        /** Picks item {@code at}, kept to the items there are, and scrolls the frame to keep it shown. */
        void select(final int at) {
            selected = items.isEmpty() ? 0 : Math.max(0, Math.min(items.size() - 1, at));
            if (selected < top) {
                top = selected;
            } else if (selected >= top + PacePainter.FRAME_PAGE) {
                top = selected - PacePainter.FRAME_PAGE + 1;
            }
        }
    }
}
