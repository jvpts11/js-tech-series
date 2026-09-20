/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.operation.payload.SaveFilePayload;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.UiContext;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Paint: a real picture rather than a grid of blocks.
 *
 * <p>A canvas of up to 128 by 128 in a palette of 256 colours, every tool the program it is named after
 * had, and one layer. The file it writes is a {@code .pix}, which is about a kilobyte where a colour per
 * pixel would be twenty-four, and that is what makes keeping pictures on a disk here possible at all.
 *
 * <p>What earns it its place beside the games is the last button: a picture can become the desktop's
 * wallpaper, so something a player drew ends up on every screen of that machine.
 */
public final class PaintApp implements IDesktopApp, CodeFileReplies.IReader {

    /** What the pointer does on the canvas, each carrying the two names it is drawn and read by. */
    private enum Tool {
        PENCIL("P", "Pencil"),
        ERASER("E", "Eraser"),
        FILL("F", "Fill"),
        DROPPER("D", "Dropper"),
        LINE("L", "Line"),
        RECTANGLE("R", "Rectangle"),
        ELLIPSE("O", "Ellipse");

        private final String mark;
        private final String label;

        Tool(final String mark, final String label) {
            this.mark = mark;
            this.label = label;
        }

        /** The single letter drawn in its button. */
        String mark() {
            return mark;
        }

        /** What it is called, for the bar along the bottom. */
        String label() {
            return label;
        }
    }

    private static final int TOOLBAR_H = 15;
    private static final int PALETTE_H = 24;
    private static final int STATUS_H = 11;
    private static final int MARGIN = 3;
    private static final int TOOL_SIZE = 13;
    private static final int SWATCH = 8;
    private static final int SWATCH_COLS = 16;
    private static final int SWATCH_ROWS = 2;
    private static final int CHECKER_LIGHT = 0xFFBFBFBF;
    private static final int CHECKER_DARK = 0xFFA0A0A0;
    /** How big one square of the chequer under the canvas is, in pixels of the screen. */
    private static final int CHECKER_SQUARE = 6;
    private static final int CANVAS_EDGE = 0xFF2B2B2B;
    /** How many steps back the program remembers, which is what a drawing hand actually needs. */
    private static final int UNDO_DEPTH = 24;

    /** The tools in the order the column offers them, read the same way by the drawing and the click. */
    private static final List<Tool> TOOLS = List.of(Tool.values());

    private final BlockPos host;
    private final FileDialog dialog;
    private final Panel root = new Panel();
    private final Button newButton;
    private final Button openButton;
    private final Button saveButton;
    private final Button undoButton;
    private final Button zoomButton;
    private final Button wallpaperButton;

    private OsSkin skin = OsSkin.fallback();
    private PixImage image = new PixImage(64, 48);
    private final Deque<PixImage> undo = new ArrayDeque<>();
    private String path = "";
    private String status = "New picture";
    private boolean dirty;

    private Tool tool = Tool.PENCIL;
    private int colour = 1;
    private int zoom = 3;

    /* Where the canvas, the tool column and the colour strip were last drawn, so a click agrees with them. */
    private int canvasX;
    private int canvasY;
    private int toolsX;
    private int toolsY;
    private int paletteX;
    private int paletteY;
    /** Where a shape was started, or -1 while nothing is being dragged. */
    private int dragFromX = -1;
    private int dragFromY = -1;
    private int hoverX = -1;
    private int hoverY = -1;

    public PaintApp(final BlockPos host) {
        this.host = host;
        this.dialog = new FileDialog(host, this);
        newButton = root.add(new Button("New", this::newPicture));
        openButton = root.add(new Button("Open", this::chooseOpen));
        saveButton = root.add(new Button("Save", this::chooseSave));
        undoButton = root.add(new Button("Undo", this::undo));
        zoomButton = root.add(new Button(() -> zoom + "x", this::cycleZoom));
        wallpaperButton = root.add(new Button("Wallpaper", this::setAsWallpaper));
    }

    /* What the picture is */

    private void newPicture() {
        pushUndo();
        this.image = new PixImage(64, 48);
        this.path = "";
        this.dirty = false;
        this.status = "New picture";
    }

    private void cycleZoom() {
        this.zoom = zoom >= 6 ? 1 : zoom + 1;
    }

    private void pushUndo() {
        undo.addLast(image.copy());
        while (undo.size() > UNDO_DEPTH) {
            undo.removeFirst();
        }
    }

    private void undo() {
        final PixImage last = undo.pollLast();
        if (last == null) {
            this.status = "Nothing to undo";
            return;
        }
        this.image = last;
        this.dirty = true;
    }

    /* Files */

    private void chooseOpen() {
        dialog.openFile("Open picture", "",
                List.of(FileDialog.Filter.of("Pictures", PixImage.EXTENSION), FileDialog.Filter.ALL),
                this::openFile);
    }

    @Override
    public void openFile(final String target) {
        if (target == null || target.isEmpty()) {
            return;
        }
        CodeFileReplies.expectContent(this, target);
        PacketDistributor.sendToServer(new RequestFileContentPayload(host, target));
    }

    @Override
    public void onContent(final String target, final String content, final boolean exists) {
        if (!exists) {
            this.status = "No such picture";
            return;
        }
        final PixImage loaded = PixImage.decode(content);
        if (loaded == null) {
            this.status = target.substring(target.lastIndexOf('/') + 1) + " is not a picture";
            return;
        }
        pushUndo();
        this.image = loaded;
        this.path = target;
        this.dirty = false;
        this.status = "Opened " + name();
    }

    private void chooseSave() {
        dialog.saveAs("Save picture", "", path.isEmpty() ? "picture." + PixImage.EXTENSION : name(),
                List.of(FileDialog.Filter.of("Pictures", PixImage.EXTENSION)), this::saveTo);
    }

    private void saveTo(final String target) {
        if (target == null || target.isEmpty()) {
            return;
        }
        final String written = image.encode();
        /*
         * A canvas of this size cannot make a file past the cap, which is why the format compresses; the
         * check stays so that a larger canvas one day fails loudly here rather than at the network.
         */
        if (written.length() > SaveFilePayload.MAX_CONTENT) {
            this.status = "Picture too large to save";
            return;
        }
        this.path = target;
        CodeFileReplies.expectSaved(this);
        PacketDistributor.sendToServer(new SaveFilePayload(host, target, written));
        FilesApps.diskChanged();
        this.status = "Saving " + name();
    }

    @Override
    public void onContentTooLarge(final String target) {
        this.status = "That picture is too large to open here";
    }

    @Override
    public void onSaved(final boolean ok, final String message) {
        this.status = message;
        if (ok) {
            this.dirty = false;
        }
    }

    @Override
    public void onClosed() {
        CodeFileReplies.forget(this);
    }

    /** Hangs this picture on the desktop, which is what the program is for beyond the drawing of it. */
    private void setAsWallpaper() {
        if (path.isEmpty()) {
            this.status = "Save it first";
            return;
        }
        if (dirty) {
            this.status = "Save it first";
            return;
        }
        DesktopScreen.setWallpaperToPicture(path);
        this.status = name() + " is now the wallpaper";
    }

    private String name() {
        final int slash = path.lastIndexOf('/');
        return slash >= 0 && slash < path.length() - 1 ? path.substring(slash + 1) : path;
    }

    /* The window */

    @Override
    public String title() {
        return (dirty ? "*" : "") + (path.isEmpty() ? "untitled." + PixImage.EXTENSION : name()) + " - Paint";
    }

    @Override
    public int defaultWidth() {
        return 260;
    }

    @Override
    public int defaultHeight() {
        return 210;
    }

    @Override
    public int minWidth() {
        return 220;
    }

    @Override
    public int minHeight() {
        return 150;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        layoutToolbar(font, x, y, width);
        root.render(g, ctx);
        drawTools(g, font, x, y + TOOLBAR_H, mouseX, mouseY);

        final int left = x + MARGIN + TOOL_SIZE + 2;
        final int top = y + TOOLBAR_H + 2;
        final int bottom = y + height - STATUS_H - PALETTE_H;
        drawCanvas(g, left, top, x + width - MARGIN, bottom, mouseX, mouseY);
        drawPalette(g, x, bottom, width);
        drawStatus(g, font, x, y + height - STATUS_H, width);
    }

    private void layoutToolbar(final Font font, final int x, final int y, final int width) {
        int bx = x + MARGIN;
        bx = place(newButton, font, "New", bx, y);
        bx = place(openButton, font, "Open", bx, y);
        bx = place(saveButton, font, "Save", bx, y);
        bx = place(undoButton, font, "Undo", bx, y);
        bx = place(zoomButton, font, "0x", bx, y);
        final int w = font.width("Wallpaper") + 8;
        // Pinned right, never back over the button before it; a narrow window clips rather than overlaps.
        wallpaperButton.setBounds(Math.max(bx, x + width - MARGIN - w), y + 1, w, 12);
    }

    private static int place(final Button button, final Font font, final String label,
                             final int bx, final int y) {
        final int w = font.width(label) + 8;
        button.setBounds(bx, y + 1, w, 12);
        return bx + w + 2;
    }

    private void drawTools(final GuiGraphics g, final Font font, final int x, final int y,
                           final int mouseX, final int mouseY) {
        this.toolsX = x + MARGIN;
        this.toolsY = y + 2;
        for (int i = 0; i < TOOLS.size(); i++) {
            final Tool each = TOOLS.get(i);
            final int ty = toolsY + i * (TOOL_SIZE + 1);
            final boolean on = each == tool;
            final boolean hover = mouseX >= toolsX && mouseX < toolsX + TOOL_SIZE
                    && mouseY >= ty && mouseY < ty + TOOL_SIZE;
            g.fill(toolsX, ty, toolsX + TOOL_SIZE, ty + TOOL_SIZE,
                    on ? skin.accent() : (hover ? skin.listHover() : skin.panelBg()));
            g.drawString(font, each.mark(),
                    toolsX + (TOOL_SIZE - font.width(each.mark())) / 2, ty + 3,
                    on ? skin.windowBg() : skin.text(), false);
        }
    }

    private void drawCanvas(final GuiGraphics g, final int left, final int top, final int right,
                            final int bottom, final int mouseX, final int mouseY) {
        final int shownW = Math.min(image.width() * zoom, right - left);
        final int shownH = Math.min(image.height() * zoom, bottom - top);
        this.canvasX = left;
        this.canvasY = top;
        g.fill(left - 1, top - 1, left + shownW + 1, top + shownH + 1, CANVAS_EDGE);
        drawPicture(g, image, left, top, shownW, shownH);
        this.hoverX = (mouseX - left) / zoom;
        this.hoverY = (mouseY - top) / zoom;
        if (!onCanvas(mouseX, mouseY, shownW, shownH)) {
            this.hoverX = -1;
            this.hoverY = -1;
        }
        drawPreview(g, left, top, shownW, shownH);
    }

    /**
     * Draws the picture, one run of colour at a time rather than one pixel at a time.
     *
     * <p>A full canvas is sixteen thousand pixels, and a rectangle each would be sixteen thousand draw
     * calls in every single frame. A drawing is mostly flat areas, so collapsing each row into runs of one
     * colour turns that into a few dozen, and the chequerboard under the transparent parts is the same
     * trick with two colours alternating.
     */
    private void drawPicture(final GuiGraphics g, final PixImage image, final int left,
                             final int top, final int shownW, final int shownH) {
        /*
         * The chequer first, in squares of its own size rather than one per pixel of the picture: an empty
         * canvas is otherwise sixteen thousand alternating squares, which is the worst case of the very
         * thing this method exists to avoid.
         */
        g.fill(left, top, left + shownW, top + shownH, CHECKER_LIGHT);
        for (int sy = 0; sy < shownH; sy += CHECKER_SQUARE) {
            for (int sx = (sy / CHECKER_SQUARE) % 2 == 0 ? CHECKER_SQUARE : 0;
                    sx < shownW; sx += CHECKER_SQUARE * 2) {
                g.fill(left + sx, top + sy,
                        left + Math.min(sx + CHECKER_SQUARE, shownW),
                        top + Math.min(sy + CHECKER_SQUARE, shownH), CHECKER_DARK);
            }
        }
        final int columns = Math.min(image.width(), (shownW + zoom - 1) / zoom);
        final int rows = Math.min(image.height(), (shownH + zoom - 1) / zoom);
        for (int py = 0; py < rows; py++) {
            final int sy = top + py * zoom;
            final int h = Math.min(zoom, shownH - py * zoom);
            int runStart = -1;
            int runColour = 0;
            for (int px = 0; px <= columns; px++) {
                final int index = px < columns ? image.get(px, py) : 0;
                final int colour = px < columns && !PixImage.isTransparent(index)
                        ? PixImage.colourOf(index) : 0;
                if (colour == runColour) {
                    continue;
                }
                if (runStart >= 0 && runColour != 0) {
                    final int sx = left + runStart * zoom;
                    final int w = Math.min((px - runStart) * zoom, shownW - runStart * zoom);
                    if (w > 0 && h > 0) {
                        g.fill(sx, sy, sx + w, sy + h, runColour);
                    }
                }
                runStart = px;
                runColour = colour;
            }
        }
    }

    /** The shape being dragged, drawn over the picture but not into it until the button is let go. */
    private void drawPreview(final GuiGraphics g, final int left, final int top,
                             final int shownW, final int shownH) {
        if (dragFromX < 0 || hoverX < 0 || !isShape(tool)) {
            return;
        }
        /*
         * Only the pixels the shape would change are drawn, so a preview costs what the outline costs
         * rather than what the whole canvas costs.
         */
        final PixImage preview = image.copy();
        paintShape(preview, tool, dragFromX, dragFromY, hoverX, hoverY, colour);
        final int columns = Math.min(image.width(), (shownW + zoom - 1) / zoom);
        final int rows = Math.min(image.height(), (shownH + zoom - 1) / zoom);
        for (int py = 0; py < rows; py++) {
            for (int px = 0; px < columns; px++) {
                final int index = preview.get(px, py);
                if (index != image.get(px, py)) {
                    final int sx = left + px * zoom;
                    final int sy = top + py * zoom;
                    g.fill(sx, sy, sx + Math.min(zoom, shownW - px * zoom),
                            sy + Math.min(zoom, shownH - py * zoom), PixImage.colourOf(index));
                }
            }
        }
    }

    private void drawPalette(final GuiGraphics g, final int x, final int y, final int width) {
        g.fill(x, y, x + width, y + PALETTE_H, skin.windowBg());
        // The colour in hand, big enough to be read at a glance beside the strip it came from.
        g.fill(x + MARGIN, y + 3, x + MARGIN + 18, y + 21, CANVAS_EDGE);
        if (PixImage.isTransparent(colour)) {
            g.fill(x + MARGIN + 1, y + 4, x + MARGIN + 17, y + 20, CHECKER_LIGHT);
        } else {
            g.fill(x + MARGIN + 1, y + 4, x + MARGIN + 17, y + 20, PixImage.colourOf(colour));
        }
        final int stripX = x + MARGIN + 22;
        this.paletteX = stripX;
        this.paletteY = y + 3;
        for (int row = 0; row < SWATCH_ROWS; row++) {
            for (int col = 0; col < SWATCH_COLS; col++) {
                final int index = row * SWATCH_COLS + col;
                final int sx = stripX + col * SWATCH;
                final int sy = y + 3 + row * SWATCH;
                if (sx + SWATCH > x + width - MARGIN) {
                    continue;
                }
                if (PixImage.isTransparent(index)) {
                    g.fill(sx, sy, sx + SWATCH - 1, sy + SWATCH - 1, CHECKER_LIGHT);
                } else {
                    g.fill(sx, sy, sx + SWATCH - 1, sy + SWATCH - 1, PixImage.colourOf(index));
                }
                if (index == colour) {
                    g.fill(sx - 1, sy - 1, sx + SWATCH, sy, skin.accent());
                    g.fill(sx - 1, sy + SWATCH - 1, sx + SWATCH, sy + SWATCH, skin.accent());
                    g.fill(sx - 1, sy - 1, sx, sy + SWATCH, skin.accent());
                    g.fill(sx + SWATCH - 1, sy - 1, sx + SWATCH, sy + SWATCH, skin.accent());
                }
            }
        }
    }

    private void drawStatus(final GuiGraphics g, final Font font, final int x, final int y, final int width) {
        g.fill(x, y, x + width, y + STATUS_H, skin.windowBg());
        final String left = hoverX >= 0 ? "x " + hoverX + ", y " + hoverY : tool.label();
        g.drawString(font, left, x + MARGIN, y + 2, skin.text(), false);
        final String right = image.width() + " x " + image.height();
        g.drawString(font, right, x + width - MARGIN - font.width(right), y + 2, skin.dim(), false);
        final int room = width - MARGIN * 2 - font.width(left) - font.width(right) - 10;
        if (room > 20) {
            g.drawString(font, font.plainSubstrByWidth(status, room),
                    x + MARGIN + font.width(left) + 6, y + 2, skin.dim(), false);
        }
    }

    private boolean onCanvas(final double mouseX, final double mouseY,
                             final int shownW, final int shownH) {
        return mouseX >= canvasX && mouseX < canvasX + shownW
                && mouseY >= canvasY && mouseY < canvasY + shownH;
    }

    private static boolean isShape(final Tool what) {
        return what == Tool.LINE || what == Tool.RECTANGLE || what == Tool.ELLIPSE;
    }

    /** Draws whichever of the three shapes is in hand, which is what both the preview and the drop use. */
    private static void paintShape(final PixImage target, final Tool what, final int x0, final int y0,
                                   final int x1, final int y1, final int ink) {
        switch (what) {
            case LINE -> target.line(x0, y0, x1, y1, ink);
            case RECTANGLE -> target.rectangle(x0, y0, x1, y1, ink);
            case ELLIPSE -> target.ellipse(x0, y0, x1, y1, ink);
            default -> { }
        }
    }

    /* Input */

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (root.mouseClicked(mouseX, mouseY, button)) {
            return;
        }
        if (clickedTool(mouseX, mouseY) || clickedSwatch(mouseX, mouseY)) {
            return;
        }
        if (hoverX < 0 || hoverX >= image.width() || hoverY < 0 || hoverY >= image.height()) {
            return;
        }
        if (tool == Tool.DROPPER) {
            this.colour = image.get(hoverX, hoverY);
            return;
        }
        pushUndo();
        this.dirty = true;
        switch (tool) {
            case PENCIL -> image.set(hoverX, hoverY, button == 1 ? 0 : colour);
            case ERASER -> image.set(hoverX, hoverY, 0);
            case FILL -> image.fill(hoverX, hoverY, button == 1 ? 0 : colour);
            case LINE, RECTANGLE, ELLIPSE -> {
                this.dragFromX = hoverX;
                this.dragFromY = hoverY;
            }
            case DROPPER -> { }
        }
    }

    private boolean clickedTool(final double mouseX, final double mouseY) {
        if (mouseX < toolsX || mouseX >= toolsX + TOOL_SIZE) {
            return false;
        }
        for (int i = 0; i < TOOLS.size(); i++) {
            final int ty = toolsY + i * (TOOL_SIZE + 1);
            if (mouseY >= ty && mouseY < ty + TOOL_SIZE) {
                this.tool = TOOLS.get(i);
                return true;
            }
        }
        return false;
    }

    private boolean clickedSwatch(final double mouseX, final double mouseY) {
        final int index = swatchAt(mouseX, mouseY);
        if (index < 0) {
            return false;
        }
        this.colour = index;
        return true;
    }

    /** Which swatch a pixel is on, or -1 for none; the strip's own geometry, read the same way it is drawn. */
    private int swatchAt(final double mouseX, final double mouseY) {
        if (paletteX <= 0 || mouseX < paletteX || mouseY < paletteY) {
            return -1;
        }
        final int col = (int) ((mouseX - paletteX) / SWATCH);
        final int row = (int) ((mouseY - paletteY) / SWATCH);
        if (col < 0 || col >= SWATCH_COLS || row < 0 || row >= SWATCH_ROWS) {
            return -1;
        }
        return row * SWATCH_COLS + col;
    }

    @Override
    public void mouseDragged(final DesktopWindow window, final double mouseX, final double mouseY,
                             final int button) {
        if (dragFromX >= 0 || hoverX < 0) {
            return;
        }
        if (tool == Tool.PENCIL || tool == Tool.ERASER) {
            this.dirty = true;
            image.set(hoverX, hoverY, tool == Tool.ERASER || button == 1 ? 0 : colour);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY,
                              final int button) {
        root.mouseReleased(mouseX, mouseY, button);
        if (dragFromX < 0) {
            return;
        }
        if (hoverX >= 0) {
            paintShape(image, tool, dragFromX, dragFromY, hoverX, hoverY, button == 1 ? 0 : colour);
            this.dirty = true;
        }
        this.dragFromX = -1;
        this.dragFromY = -1;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_Z) {
            undo();
            return true;
        }
        if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0 && key == GLFW.GLFW_KEY_S) {
            chooseSave();
            return true;
        }
        return false;
    }
}
