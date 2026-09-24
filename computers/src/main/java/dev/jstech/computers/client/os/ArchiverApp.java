/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.ArchiveFilesPayload;
import dev.jstech.computers.operation.payload.ExtractArchivePayload;
import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.os.fs.Archive;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 67ark: many files packed into one, and taken back out.
 *
 * <p>Two things happen in this window and they are kept apart on purpose. Either an archive is open and its
 * contents are listed, or one is being put together out of files chosen from the disk. Mixing the two is how
 * an archiver becomes confusing, so the window says which of them it is doing.
 *
 * <p>The packing itself is the server's, because the space an archive saves has to be real. This window
 * chooses, asks, and shows what came back.
 */
@PaletteHolder
public final class ArchiverApp implements IDesktopApp, CodeFileReplies.IReader {

    private static final int ROW_H = 11;
    private static final int TOOLBAR_H = 16;
    private static final int HEADER_H = 12;
    private static final int STATUS_H = 11;
    private static final int MARGIN = 4;
    /** 67ark's own colour, {@code jsc:app/archiver}: the status line when it reports a space saving. */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/archiver",
            new Colours(0xFF1C7A32));

    /** Which of the two jobs this window is doing. */
    private enum Mode { LIST, BUILD }

    private final BlockPos host;
    private final FileDialog dialog;
    private final Panel root = new Panel();
    private final Button openButton;
    private final Button newButton;
    private final Button addButton;
    private final Button removeButton;
    private final Button packButton;
    private final Button extractButton;
    private final Button extractAllButton;
    private final Button deleteOriginals;

    private OsSkin skin = OsSkin.fallback();
    private Mode mode = Mode.LIST;

    /* The archive being looked at. */
    private String archivePath = "";
    private List<Archive.Entry> entries = List.of();
    private int packedBytes;
    private int originalBytes;
    private int selected = -1;
    private int scroll;

    /* The archive being put together. */
    private final List<String> chosen = new ArrayList<>();
    private boolean removeOriginals;
    private String pendingArchive = "";

    private Text status = ArchiverTexts.OPEN_OR_MAKE.text();
    private boolean statusGood;

    /* Where the list was last drawn, so a click reads the same numbers the drawing did. */
    private int listTop;
    private int listRows;

    public ArchiverApp(final BlockPos host) {
        this.host = host;
        this.dialog = new FileDialog(host, this);
        openButton = root.add(new Button(GameText.resolve(ArchiverTexts.OPEN), this::chooseArchive));
        newButton = root.add(new Button(GameText.resolve(ArchiverTexts.NEW), this::startBuilding));
        addButton = root.add(new Button(GameText.resolve(ArchiverTexts.ADD), this::chooseFile));
        removeButton = root.add(new Button(GameText.resolve(ArchiverTexts.REMOVE), this::removeChosen));
        packButton = root.add(new Button(GameText.resolve(ArchiverTexts.PACK), this::choosePackTarget));
        extractButton = root.add(new Button(GameText.resolve(ArchiverTexts.EXTRACT), this::extractOne));
        extractAllButton = root.add(new Button(GameText.resolve(ArchiverTexts.EXTRACT_ALL), this::extractAll));
        deleteOriginals = root.add(new Button(this::originalsLabel, this::toggleOriginals));
    }

    private String originalsLabel() {
        return GameText.resolve(removeOriginals ? ArchiverTexts.DELETE_ORIGINALS : ArchiverTexts.KEEP_ORIGINALS);
    }

    private void toggleOriginals() {
        this.removeOriginals = !removeOriginals;
    }

    /* Opening an archive */

    private void chooseArchive() {
        dialog.openFile(ArchiverTexts.OPEN_ARCHIVE.text(), "",
                List.of(FileDialog.Filter.of(ArchiverTexts.ARCHIVES, Archive.EXTENSION), FileDialog.Filter.ALL),
                this::openFile);
    }

    @Override
    public void openFile(final String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        this.mode = Mode.LIST;
        this.archivePath = path;
        this.entries = List.of();
        this.packedBytes = 0;
        this.originalBytes = 0;
        this.selected = -1;
        this.scroll = 0;
        this.status = ArchiverTexts.READING.with(Archive.leaf(path));
        this.statusGood = false;
        CodeFileReplies.expectContent(this, path);
        PacketDistributor.sendToServer(new RequestFileContentPayload(host, path));
    }

    @Override
    public void onContent(final String path, final String content, final boolean exists) {
        if (!path.equals(archivePath)) {
            return;
        }
        if (!exists) {
            this.status = ArchiverTexts.NO_SUCH_FILE.text();
            this.statusGood = false;
            return;
        }
        if (!Archive.isArchive(content)) {
            this.entries = List.of();
            this.status = ArchiverTexts.NOT_AN_ARCHIVE.with(Archive.leaf(path));
            this.statusGood = false;
            return;
        }
        this.entries = Archive.entries(content);
        this.packedBytes = content.getBytes(StandardCharsets.UTF_8).length;
        this.originalBytes = Archive.originalBytes(content);
        this.status = (entries.size() == 1 ? ArchiverTexts.ONE_FILE_INSIDE : ArchiverTexts.FILES_INSIDE)
                .with(entries.size());
        this.statusGood = false;
    }

    /* Building one */

    private void startBuilding() {
        this.mode = Mode.BUILD;
        this.chosen.clear();
        this.selected = -1;
        this.scroll = 0;
        this.pendingArchive = "";
        this.status = ArchiverTexts.ADD_FILES.text();
        this.statusGood = false;
    }

    private void chooseFile() {
        if (mode != Mode.BUILD) {
            startBuilding();
        }
        dialog.openFile(ArchiverTexts.ADD_TO_ARCHIVE.text(), "", List.of(FileDialog.Filter.ALL), this::addChosen);
    }

    private void addChosen(final String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        if (chosen.contains(path)) {
            this.status = ArchiverTexts.ALREADY_LISTED.text();
            this.statusGood = false;
            return;
        }
        /*
         * A file keeps only its own name inside an archive, so two of the same name from different folders
         * cannot both go in. Caught here rather than by the server, so the player is told while choosing.
         */
        final String leaf = Archive.leaf(path);
        for (final String already : chosen) {
            if (Archive.leaf(already).equals(leaf)) {
                this.status = ArchiverTexts.ANOTHER_LISTED.with(leaf);
                this.statusGood = false;
                return;
            }
        }
        if (chosen.size() >= ArchiveFilesPayload.MAX_PATHS) {
            this.status = ArchiverTexts.AT_MOST.with(ArchiveFilesPayload.MAX_PATHS);
            this.statusGood = false;
            return;
        }
        chosen.add(path);
        this.status = (chosen.size() == 1 ? ArchiverTexts.ONE_TO_PACK : ArchiverTexts.TO_PACK).with(chosen.size());
        this.statusGood = false;
    }

    private void removeChosen() {
        if (mode != Mode.BUILD || selected < 0 || selected >= chosen.size()) {
            return;
        }
        chosen.remove(selected);
        this.selected = -1;
    }

    private void choosePackTarget() {
        if (mode != Mode.BUILD || chosen.isEmpty()) {
            this.status = ArchiverTexts.NOTHING_TO_PACK.text();
            this.statusGood = false;
            return;
        }
        dialog.saveAs(ArchiverTexts.PACK_INTO.text(), "", "archive." + Archive.EXTENSION,
                List.of(FileDialog.Filter.of(ArchiverTexts.ARCHIVES, Archive.EXTENSION)), this::pack);
    }

    private void pack(final String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        this.pendingArchive = path;
        CodeFileReplies.expectSaved(this);
        PacketDistributor.sendToServer(
                new ArchiveFilesPayload(host, path, List.copyOf(chosen), removeOriginals));
        this.status = ArchiverTexts.PACKING.with(chosen.size());
        this.statusGood = false;
    }

    /* Taking files back out */

    private void extractOne() {
        if (mode != Mode.LIST || selected < 0 || selected >= entries.size()) {
            this.status = ArchiverTexts.PICK_FIRST.text();
            this.statusGood = false;
            return;
        }
        final String name = entries.get(selected).name();
        dialog.openFolder(ArchiverTexts.TAKE_OUT_INTO.text(), "", dir -> send(name, dir));
    }

    private void extractAll() {
        if (mode != Mode.LIST || entries.isEmpty()) {
            this.status = ArchiverTexts.OPEN_FIRST.text();
            this.statusGood = false;
            return;
        }
        dialog.openFolder(ArchiverTexts.TAKE_ALL_OUT_INTO.text(), "", dir -> send("", dir));
    }

    private void send(final String entry, final String dir) {
        CodeFileReplies.expectSaved(this);
        PacketDistributor.sendToServer(new ExtractArchivePayload(host, archivePath, entry,
                dir == null ? "" : dir));
        this.status = ArchiverTexts.TAKING_OUT.text();
        this.statusGood = false;
    }

    @Override
    public void onContentTooLarge(final String path) {
        this.status = EditorTexts.TOO_LARGE.with(Archive.leaf(path));
        this.statusGood = false;
    }

    @Override
    public void onSaved(final boolean ok, final Text message) {
        this.status = message;
        this.statusGood = ok;
        if (ok && !pendingArchive.isEmpty()) {
            // The archive that was just written becomes the one being looked at, which is what a player expects.
            final String written = pendingArchive;
            this.pendingArchive = "";
            this.chosen.clear();
            openFile(written);
        }
    }

    /* The window */

    @Override
    public String title() {
        return archivePath.isEmpty() ? "67ark" : Archive.leaf(archivePath) + " - 67ark";
    }

    /**
     * Wide enough for the longer of the two toolbars.
     *
     * <p>Building an archive offers six buttons where listing one offers four, and the last of them says
     * what happens to the originals. At two hundred and fifty the two on the right were drawn over each
     * other, because the right-pinned one slid back under the one before it.
     */
    @Override
    public int defaultWidth() {
        return 292;
    }

    @Override
    public int defaultHeight() {
        return 170;
    }

    @Override
    public int minWidth() {
        return 260;
    }

    @Override
    public int minHeight() {
        return 120;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void onClosed() {
        CodeFileReplies.forget(this);
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        layoutToolbar(font, x, y, width);
        root.render(g, ctx);
        final int top = y + TOOLBAR_H + HEADER_H;
        final int listBottom = y + height - STATUS_H;
        this.listTop = top;
        this.listRows = Math.max(0, (listBottom - top) / ROW_H);
        drawHeader(g, font, x, y + TOOLBAR_H, width);
        g.fill(x + MARGIN, top, x + width - MARGIN, listBottom, skin.fieldBg());
        if (mode == Mode.LIST) {
            drawArchive(g, font, x, top, width, listBottom);
        } else {
            drawChosen(g, font, x, top, width, listBottom);
        }
        drawStatus(g, font, x, listBottom, width);
    }

    private void layoutToolbar(final Font font, final int x, final int y, final int width) {
        final boolean building = mode == Mode.BUILD;
        int bx = x + MARGIN;
        bx = place(openButton, font, bx, y);
        bx = place(newButton, font, bx, y);
        if (building) {
            bx = place(addButton, font, bx, y);
            bx = place(removeButton, font, bx, y);
            bx = place(packButton, font, bx, y);
            // As wide as the longer of its two words, so the button does not change size when it is pressed.
            final int w = Math.max(font.width(GameText.resolve(ArchiverTexts.DELETE_ORIGINALS)),
                    font.width(GameText.resolve(ArchiverTexts.KEEP_ORIGINALS))) + 8;
            /*
             * Pinned right, but never left of where the buttons before it end: a narrow window runs the
             * label off its own edge rather than drawing it over the button beside it.
             */
            deleteOriginals.setBounds(Math.max(bx, x + width - MARGIN - w), y + 2, w, 12);
            deleteOriginals.setPrimary(removeOriginals);
        } else {
            bx = place(extractButton, font, bx, y);
            place(extractAllButton, font, bx, y);
        }
        addButton.setVisible(building);
        removeButton.setVisible(building);
        packButton.setVisible(building);
        deleteOriginals.setVisible(building);
        extractButton.setVisible(!building);
        extractAllButton.setVisible(!building);
    }

    /* A button as wide as the words it shows, in the player's language. */
    private static int place(final Button button, final Font font, final int bx, final int y) {
        final int w = font.width(button.label()) + 8;
        button.setBounds(bx, y + 2, w, 12);
        return bx + w + 2;
    }

    private void drawHeader(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        g.fill(x + MARGIN, y, x + width - MARGIN, y + HEADER_H, skin.listHover());
        final int[] cols = columns(x, width);
        g.drawString(font, GameText.resolve(ArchiverTexts.NAME_COLUMN), cols[0], y + 2, skin.text(), false);
        if (mode == Mode.LIST) {
            right(g, font, GameText.resolve(ArchiverTexts.SIZE_COLUMN), cols[1], y + 2, skin.text());
            right(g, font, GameText.resolve(ArchiverTexts.SAVED_COLUMN), cols[2], y + 2, skin.text());
        } else {
            right(g, font, GameText.resolve(ArchiverTexts.WHERE_COLUMN), cols[2], y + 2, skin.text());
        }
    }

    private void drawArchive(final GuiGraphics g, final Font font, final int x, final int top,
                             final int width, final int bottom) {
        if (entries.isEmpty()) {
            g.drawString(font, GameText.resolve(archivePath.isEmpty() ? ArchiverTexts.NO_ARCHIVE
                    : ArchiverTexts.NOTHING_IN_IT), x + MARGIN + 4, top + 4, skin.dim(), false);
            return;
        }
        final int[] cols = columns(x, width);
        final int rows = Math.max(0, (bottom - top) / ROW_H);
        clampScroll(entries.size(), rows);
        for (int i = 0; i < rows && scroll + i < entries.size(); i++) {
            final Archive.Entry entry = entries.get(scroll + i);
            final int ry = top + i * ROW_H;
            final boolean on = scroll + i == selected;
            if (on) {
                g.fill(x + MARGIN, ry, x + width - MARGIN, ry + ROW_H, skin.listSelect());
            }
            final int ink = skin.listRowText(on);
            g.drawString(font, font.plainSubstrByWidth(entry.name(), cols[1] - cols[0] - 6),
                    cols[0], ry + 2, ink, false);
            right(g, font, bytes(entry.originalBytes()), cols[1], ry + 2, ink);
            right(g, font, entry.type().extension().isEmpty() ? GameText.resolve(ArchiverTexts.FILE)
                    : entry.type().extension(), cols[2], ry + 2, on ? ink : skin.dim());
        }
    }

    private void drawChosen(final GuiGraphics g, final Font font, final int x, final int top,
                            final int width, final int bottom) {
        if (chosen.isEmpty()) {
            g.drawString(font, GameText.resolve(ArchiverTexts.ADD_FILES), x + MARGIN + 4, top + 4, skin.dim(),
                    false);
            return;
        }
        final int[] cols = columns(x, width);
        final int rows = Math.max(0, (bottom - top) / ROW_H);
        clampScroll(chosen.size(), rows);
        for (int i = 0; i < rows && scroll + i < chosen.size(); i++) {
            final String path = chosen.get(scroll + i);
            final int ry = top + i * ROW_H;
            final boolean on = scroll + i == selected;
            if (on) {
                g.fill(x + MARGIN, ry, x + width - MARGIN, ry + ROW_H, skin.listSelect());
            }
            final int ink = skin.listRowText(on);
            g.drawString(font, font.plainSubstrByWidth(Archive.leaf(path), cols[1] - cols[0] - 6),
                    cols[0], ry + 2, ink, false);
            final String where = folderOf(path);
            right(g, font, font.plainSubstrByWidth(where, cols[2] - cols[1] - 6), cols[2], ry + 2,
                    on ? ink : skin.dim());
        }
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        g.fill(x, y, x + width, y + STATUS_H, skin.windowBg());
        g.drawString(font, font.plainSubstrByWidth(GameText.resolve(status), width - MARGIN * 2 - 70), x + MARGIN,
                y + 2, statusGood ? PALETTE.get().savedGood() : skin.dim(), false);
        if (mode == Mode.LIST && originalBytes > 0) {
            // What the archive is for, said as one number: how much of the disk it handed back.
            final long saved = 100L - Math.min(100L, (long) packedBytes * 100L / originalBytes);
            right(g, font, GameText.resolve(ArchiverTexts.PERCENT_SAVED.with(saved)), x + width - MARGIN, y + 2,
                    PALETTE.get().savedGood());
        }
    }

    private int[] columns(final int x, final int width) {
        final int left = x + MARGIN + 4;
        final int right = x + width - MARGIN - 4;
        final int third = Math.max(40, (right - left) / 4);
        return new int[] {left, right - third, right};
    }

    private static void right(final GuiGraphics g, final Font font, final String text,
                              final int rightEdge, final int y, final int colour) {
        g.drawString(font, text, rightEdge - font.width(text), y, colour, false);
    }

    private static String bytes(final int value) {
        if (value < 1024) {
            return GameText.resolve(ArchiverTexts.BYTES.with(value));
        }
        return GameText.resolve(ArchiverTexts.KILOBYTES.with(String.format(Locale.ROOT, "%.1f", value / 1024.0)));
    }

    private static String folderOf(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash <= 0 ? GameText.resolve(ArchiverTexts.ROOT) : path.substring(0, slash);
    }

    private void clampScroll(final int size, final int rows) {
        this.scroll = Math.max(0, Math.min(scroll, Math.max(0, size - rows)));
    }

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (root.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        if (button != 0) {
            return;
        }
        final int size = mode == Mode.LIST ? entries.size() : chosen.size();
        final int row = (int) ((mouseY - listTop) / ROW_H);
        final int index = scroll + row;
        this.selected = mouseY >= listTop && row >= 0 && row < listRows && index < size ? index : -1;
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        final int size = mode == Mode.LIST ? entries.size() : chosen.size();
        if (size == 0) {
            return false;
        }
        this.scroll = Math.max(0, scroll - (int) Math.signum(delta));
        return true;
    }

    /** 67ark's colour: the status line's ink when it reports a space saving. */
    private record Colours(int savedGood) {
    }
}
