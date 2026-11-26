/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.CraftManagerStatePayload;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload.WireSource;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.jstech.computers.client.os.CraftingManagerTexts.FROM;
import static dev.jstech.computers.client.os.CraftingManagerTexts.INSERT_A_DISC;
import static dev.jstech.computers.client.os.CraftingManagerTexts.NO_CRAFT_FILES;
import static dev.jstech.computers.client.os.CraftingManagerTexts.NO_DISK;
import static dev.jstech.computers.client.os.CraftingManagerTexts.PICK_SOURCE;
import static dev.jstech.computers.client.os.CraftingManagerTexts.SOURCE;

/**
 * The Crafting Manager's left pane: the disk it reads recipe files from, chosen among the computer's own disk and the
 * medium in each linked drive; the folder of it shown, with its folders and its {@code .craft} files; and which of
 * those files are picked, as the file explorer picks them: a click takes one, Shift+click the run from the one clicked
 * last, Ctrl+click adds or drops one, Ctrl+A takes them all. A double click opens a folder, and {@code ..} the one
 * above. The pane remembers the last folder of each disk.
 *
 * <p>The pane follows the disk the computer shows first until the player chooses one or opens a folder; a disk it
 * chose that leaves its drive gives the pane back to the computer's choice.
 */
final class CraftSourcePane {

    private final Runnable ask;
    private final Map<String, String> lastFolder = new HashMap<>();
    private final Set<String> picked = new LinkedHashSet<>();
    private final Label fromLabel;
    private final Button sourceButton;
    private final ListView<Row> list;
    private final Label empty;
    private final Popup sourcePicker;
    private final ListView<WireSource> sourceList;
    private OsSkin skin = OsSkin.fallback();
    private List<WireSource> sources = List.of();
    private String key = "";
    private Text label = Text.EMPTY;
    private String folder = "";
    private List<String> folders = List.of();
    private List<String> files = List.of();
    /* The disk the player chose, or null while the pane shows the one the computer shows first. */
    @Nullable
    private String chosen;
    /* Counts the places the pane has gone to; a listing that answers an older one is not shown. */
    private int view;
    /* The file a Shift+click runs from: the one clicked last without Shift. */
    private String anchor = "";
    private long lastClickAt;
    private int lastClickRow = -1;
    /* The font the pane was last laid out with, which the choice of disks is sized by; none before the first frame. */
    @Nullable
    private Font font;

    private static final long DOUBLE_CLICK_MS = 350L;
    private static final int ROW_H = 11;
    private static final int HEADER_H = 12;
    /* The file icons are drawn at half their size, to fit a row. */
    private static final int ICON = FileIcons.SIZE / 2;
    private static final int TEXT_X = 2 + ICON + 3;
    private static final int PICKER_MIN_W = 150;
    private static final int PICKER_ROOM = 22;
    private static final int PICKER_TITLE_H = 14;
    private static final int PICKER_PAD = 5;
    private static final String UP = "..";

    /** The pane's components, added to {@code root}; {@code ask} asks the server for the folder shown. */
    CraftSourcePane(final Panel root, final Runnable ask) {
        this.ask = ask;
        fromLabel = root.add(new Label(GameText.resolve(FROM), Label.Tone.DIM));
        sourceButton = root.add(new Button(this::sourceText, this::openPicker));
        list = root.add(new ListView<Row>(this::rows, ROW_H, this::renderRow)
                .setPadding(1)
                .setOnClick(this::rowClicked));
        empty = root.add(new Label(() -> GameText.resolve(key.isEmpty() ? INSERT_A_DISC : NO_CRAFT_FILES),
                Label.Tone.DIM));
        sourceList = new ListView<WireSource>(this::usableSources, ROW_H, this::renderSourceRow)
                .setOnClick(this::sourceClicked);
        sourcePicker = new Popup(() -> GameText.resolve(PICK_SOURCE), PICKER_MIN_W, 60)
                .setLayouter(this::layoutPicker)
                .setCloseOnOutsideClick(true);
        sourcePicker.add(sourceList);
    }

    void setSkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    /** Takes the folder a listed state shows, when it answers where the pane is now. */
    void accept(final CraftManagerStatePayload payload) {
        if (!payload.listed() || payload.view() != view) {
            return;
        }
        sources = payload.sources();
        key = payload.mediaVolumeKey();
        label = payload.mediaLabel();
        folder = payload.folder();
        folders = payload.folders();
        files = payload.mediaFiles();
        // A disk the player chose that is gone was answered with the computer's own choice, which the pane follows.
        if (chosen != null && !chosen.equals(key)) {
            chosen = null;
        }
        if (!key.isEmpty()) {
            lastFolder.put(key, folder);
        }
        picked.retainAll(files);
    }

    /** The disk to ask for: the one the player chose, or {@code ""} for the computer's choice. */
    String askedSource() {
        return chosen == null ? "" : chosen;
    }

    /** The folder to ask for on that disk. */
    String askedFolder() {
        return chosen == null ? "" : folder;
    }

    int view() {
        return view;
    }

    /** The disk shown, or {@code ""} when there is none. */
    String key() {
        return key;
    }

    String folder() {
        return folder;
    }

    List<String> folders() {
        return folders;
    }

    List<String> files() {
        return files;
    }

    /** Every disk the computer reads, as the last listing named them. */
    List<WireSource> sources() {
        return sources;
    }

    /** The files picked, in the folder's order. */
    List<String> picked() {
        final List<String> out = new ArrayList<>();
        for (final String file : files) {
            if (picked.contains(file)) {
                out.add(file);
            }
        }
        return out;
    }

    void clearPicked() {
        picked.clear();
    }

    /** Lays the pane out: its header band at {@code top}, its list below from {@code listTop}. */
    void layout(final Font drawnWith, final boolean visible, final int x, final int top, final int width,
                final int listTop, final int listH) {
        this.font = drawnWith;
        final int fromW = drawnWith.width(GameText.resolve(FROM));
        fromLabel.setVisible(visible);
        fromLabel.setBounds(x + 3, top + 2, fromW, 8);
        sourceButton.setVisible(visible);
        sourceButton.setBounds(x + 3 + fromW + 4, top, width - fromW - 7, HEADER_H);
        list.setVisible(visible);
        list.setBounds(x, listTop, width, listH);
        empty.setVisible(visible && rows().isEmpty());
        empty.setBounds(x + 4, listTop + 4, width - 8, 8);
    }

    /** The header band and the list's ground, under the components. */
    void renderGround(final GuiGraphics g) {
        g.fill(list.x(), sourceButton.y(), list.right(), sourceButton.bottom(), skin.panelBg());
        g.fill(list.x(), list.y(), list.right(), list.bottom(), skin.fieldBg());
        Draw.outline(g, list.x(), list.y(), list.width(), list.height(), skin.edge());
    }

    /** Ctrl+A takes every file of the folder shown. */
    boolean keyPressed(final int keyCode, final int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_A && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && !files.isEmpty()) {
            picked.clear();
            picked.addAll(files);
            return true;
        }
        return false;
    }

    /** Scrolls the list a number of rows. */
    void scroll(final int rows) {
        list.setScroll(list.scroll() + rows);
    }

    int rowCount() {
        return rows().size();
    }

    boolean pickerOpen() {
        return sourcePicker.isOpen();
    }

    Popup picker() {
        return sourcePicker;
    }

    /** The centre of the button that opens the choice of disks. */
    int[] sourceButtonCenter() {
        return sourceButton.center();
    }

    /** The centre of the {@code index}-th disk the choice lists. */
    int[] sourceRowCenter(final int index) {
        return sourceList.rowCenter(index);
    }

    /** The centre of the row of the folder shown's {@code index}-th file. */
    int[] fileRowCenter(final int index) {
        return list.rowCenter(firstFileRow() + index);
    }

    /** The centre of the row of the folder called {@code name}, or of {@code ..} for {@code ".."}. */
    int[] folderRowCenter(final String name) {
        final List<Row> all = rows();
        for (int i = 0; i < all.size(); i++) {
            final Row row = all.get(i);
            if ((row.kind() == Kind.FOLDER || row.kind() == Kind.UP) && row.name().equals(name)) {
                return list.rowCenter(i);
            }
        }
        return list.rowCenter(0);
    }

    /* The rows: where the folder is and the way up, when it is not the disk's top; then its folders and files. */
    private List<Row> rows() {
        final List<Row> out = new ArrayList<>();
        if (!folder.isEmpty()) {
            out.add(new Row(Kind.PATH, pathText()));
            out.add(new Row(Kind.UP, UP));
        }
        folders.forEach(name -> out.add(new Row(Kind.FOLDER, name)));
        files.forEach(name -> out.add(new Row(Kind.FILE, name)));
        return out;
    }

    private int firstFileRow() {
        return (folder.isEmpty() ? 0 : 2) + folders.size();
    }

    private void renderRow(final GuiGraphics g, final UiContext ctx, final Row row, final int index, final int x,
                           final int y, final int w, final int h, final boolean hovered, final boolean selected) {
        if (row.kind() == Kind.PATH) {
            Draw.text(g, ctx.font(), Texts.clip(ctx.font(), row.name(), w - 6), x + 3, y + 2, ctx.skin().dim());
            return;
        }
        final boolean on = row.kind() == Kind.FILE && picked.contains(row.name());
        ctx.skin().listRow(g, x, y, w, h, hovered, on);
        final FileIcons.Kind icon = switch (row.kind()) {
            case UP -> FileIcons.Kind.UP;
            case FOLDER -> FileIcons.Kind.FOLDER;
            default -> FileIcons.Kind.CRAFT;
        };
        g.pose().pushPose();
        g.pose().translate(x + 2, y + (h - ICON) / 2, 0);
        g.pose().scale(0.5F, 0.5F, 1F);
        FileIcons.draw(g, 0, 0, icon, skin.iconSet());
        g.pose().popPose();
        final String name = row.kind() == Kind.FOLDER ? row.name() + separator() : row.name();
        Draw.text(g, ctx.font(), Texts.clip(ctx.font(), name, w - TEXT_X - 3), x + TEXT_X, y + 2,
                ctx.skin().listRowText(on));
    }

    private void rowClicked(final int index, final int button, final double mx, final double my) {
        final List<Row> all = rows();
        if (button != 0 || index < 0 || index >= all.size()) {
            return;
        }
        final Row row = all.get(index);
        final long now = System.currentTimeMillis();
        final boolean twice = index == lastClickRow && now - lastClickAt < DOUBLE_CLICK_MS;
        lastClickAt = now;
        lastClickRow = index;
        switch (row.kind()) {
            case UP -> {
                if (twice) {
                    go(FsPaths.parentDir(folder));
                }
            }
            case FOLDER -> {
                if (twice) {
                    go(FsPaths.join(folder, row.name()));
                }
            }
            case FILE -> pick(row.name());
            default -> {
            }
        }
    }

    /* As the file explorer picks: Shift runs from the anchor, Control adds or drops one, a plain click takes one. */
    private void pick(final String name) {
        if (Screen.hasShiftDown() && files.contains(anchor)) {
            final int from = files.indexOf(anchor);
            final int to = files.indexOf(name);
            picked.clear();
            picked.addAll(files.subList(Math.min(from, to), Math.max(from, to) + 1));
            return;
        }
        if (Screen.hasControlDown()) {
            if (!picked.remove(name)) {
                picked.add(name);
            }
        } else {
            picked.clear();
            picked.add(name);
        }
        anchor = name;
    }

    /* Opens a folder of the disk shown, which the pane keeps showing from then on. */
    private void go(final String to) {
        if (key.isEmpty()) {
            return;
        }
        chosen = key;
        moveTo(to);
    }

    private void choose(final WireSource source) {
        chosen = source.key();
        key = source.key();
        label = source.label();
        moveTo(lastFolder.getOrDefault(source.key(), ""));
    }

    /* Shows nothing until the listing of the new place comes, so nothing picked there is a file of the old one. */
    private void moveTo(final String to) {
        folder = to;
        folders = List.of();
        files = List.of();
        picked.clear();
        anchor = "";
        lastClickRow = -1;
        view++;
        list.setScroll(0);
        ask.run();
    }

    private List<WireSource> usableSources() {
        final List<WireSource> out = new ArrayList<>();
        for (final WireSource source : sources) {
            if (source.usable()) {
                out.add(source);
            }
        }
        return out;
    }

    private void openPicker() {
        final List<WireSource> usable = usableSources();
        if (usable.isEmpty()) {
            return;
        }
        int widest = 0;
        if (font != null) {
            for (final WireSource source : usable) {
                widest = Math.max(widest, font.width(named(source.key(), source.label())));
            }
        }
        sourceList.setScroll(0);
        sourcePicker.setPreferredSize(Math.max(PICKER_MIN_W, widest + PICKER_ROOM),
                PICKER_TITLE_H + usable.size() * ROW_H + PICKER_PAD);
        sourcePicker.open();
    }

    private void layoutPicker(final Popup p) {
        sourceList.setBounds(p.x() + PICKER_PAD, p.contentTop(), p.width() - PICKER_PAD * 2,
                p.bottom() - p.contentTop() - PICKER_PAD);
    }

    private void renderSourceRow(final GuiGraphics g, final UiContext ctx, final WireSource source, final int index,
                                 final int x, final int y, final int w, final int h, final boolean hovered,
                                 final boolean selected) {
        final boolean current = source.key().equals(key);
        ctx.skin().listRow(g, x, y, w, h, hovered, current);
        Draw.text(g, ctx.font(), Texts.clip(ctx.font(), named(source.key(), source.label()), w - 6), x + 3, y + 2,
                ctx.skin().listRowText(current));
    }

    private void sourceClicked(final int index, final int button, final double mx, final double my) {
        final List<WireSource> usable = usableSources();
        if (button != 0 || index < 0 || index >= usable.size()) {
            return;
        }
        sourcePicker.close();
        choose(usable.get(index));
    }

    private String sourceText() {
        return GameText.resolve(SOURCE.with(key.isEmpty() ? NO_DISK.text() : Text.literal(named(key, label))));
    }

    /* A disk's name with its drive letter, as the file explorer names it. */
    private String named(final String sourceKey, final Text sourceLabel) {
        return DrivePaths.onDrive(GameText.resolve(sourceLabel), letterOf(sourceKey));
    }

    private String letterOf(final String sourceKey) {
        final List<String> media = new ArrayList<>();
        for (final WireSource source : sources) {
            if (!source.key().equals(CraftManagerStatePayload.SYSTEM_DISK)) {
                media.add(source.key());
            }
        }
        return DrivePaths.letterAmong(skin.unixLike(), media,
                sourceKey.equals(CraftManagerStatePayload.SYSTEM_DISK) ? "" : sourceKey);
    }

    /* Where the folder is, as the desktop writes a path: C:\Recipes\Furnace, or /Recipes/Furnace. */
    private String pathText() {
        if (skin.unixLike()) {
            final boolean system = key.equals(CraftManagerStatePayload.SYSTEM_DISK);
            return (system ? "/" : GameText.resolve(label) + "/") + folder;
        }
        return letterOf(key) + "\\" + folder.replace('/', '\\');
    }

    private String separator() {
        return skin.unixLike() ? "/" : "\\";
    }

    /* What a row of the list is. */
    private enum Kind { PATH, UP, FOLDER, FILE }

    private record Row(Kind kind, String name) {
    }
}
