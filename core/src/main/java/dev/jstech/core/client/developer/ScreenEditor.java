/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.data.BuiltNode;
import dev.jstech.core.client.gui.data.DataScreenView;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.client.gui.data.ScreenFiles;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.gui.layout.ScreenEditorLayout;
import dev.jstech.core.gui.screen.ElementType;
import dev.jstech.core.gui.screen.ElementTypes;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The editor of a screen declared in a file, on the screen itself, in a development run: opened from the inspector
 * with Ctrl+E. A click chooses a component; dragged it moves (in a stack by its margin, pulled onto the guides of the
 * others within reach, in a row or a column to the place of the one it is dropped on), its corners resize it, and
 * guides show where it lines up with the others; its card, beside the screen at its height, holds only what it has
 * (its name, what it is bound to and the fields its kind declares), cycles the screen's style, copies it and takes it
 * out; Add puts a kind of component where the pointer is. Every change is taken by the screen at once and written to
 * the mod's source file, and Undo and Redo go back and forth through them.
 */
public final class ScreenEditor {

    /** How small the small text is drawn. */
    private static final float SMALL = 0.75F;
    /** How far in front of the screen the editor draws, over the items in its slots. */
    private static final float IN_FRONT = 400.0F;
    /** The steps the snap goes through, in pixels. */
    private static final int[] SNAPS = {1, 2, 4, 8};
    /** The size of the squares round the chosen component, and how far round one a press still takes it. */
    private static final int HANDLE = 4;
    private static final int REACH = 3;
    private static final String CORE_NAMESPACE = "jscore";
    private static final Set<String> NOT_A_FIELD = Set.of("type", "id", "bind", "children", "margin", "width",
            "height", "min_width", "min_height", "max_width", "max_height", "grow", "shrink", "align_self", "span",
            "visible_if", "enabled_if", "gap", "padding", "align", "justify", "wrap", "columns");

    private static @Nullable Editing open;

    private ScreenEditor() {
    }

    /** Listens for the screens' keys, clicks and frames, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.KeyPressed.Pre.class, ScreenEditor::onKey);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.CharacterTyped.Pre.class, event -> {
            if (open != null && open.screen == event.getScreen() && open.typing()) {
                open.charTyped(event.getCodePoint());
                event.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, ScreenEditor::onPress);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseDragged.Pre.class, ScreenEditor::onDrag);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonReleased.Pre.class, ScreenEditor::onRelease);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, ScreenEditor::onRender);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Closing.class, event -> {
            if (open != null && open.screen == event.getScreen()) {
                open = null;
            }
        });
    }

    /** Opens the editor on {@code screen}, a screen declared in a file; nothing when its file cannot be read. */
    static void open(final Screen screen, final DataScreenView view) {
        ScreenJson.read(view.file()).ifPresent(json -> {
            if (ScreenInspector.openOn(screen)) {
                ScreenInspector.toggle(screen);
            }
            open = new Editing(screen, view, json);
        });
    }

    /** Opens the editor on {@code screen} at once, for a test; whether it could. */
    public static boolean openFor(final Screen screen) {
        if (!(screen instanceof IDataScreen data)) {
            return false;
        }
        open(screen, data.view());
        return openOn(screen);
    }

    /** Whether the editor is open on {@code screen}. */
    public static boolean openOn(final Screen screen) {
        return open != null && open.screen == screen;
    }

    /** The file as the open editor holds it now, for a test; none when it is closed. */
    public static Optional<String> file() {
        return open == null ? Optional.empty() : Optional.of(ScreenJson.pretty(open.json));
    }

    /** Chooses the component written at {@code path} of the file's tree, as a click on it does; for a test. */
    public static void choose(final List<Integer> path) {
        if (open != null) {
            open.selected = List.copyOf(path);
            open.fillFields();
        }
    }

    /** Sets the field {@code key} of the chosen component, as its panel does; for a test. */
    public static void set(final String key, final String value) {
        if (open != null) {
            open.setField(key, value);
        }
    }

    /** Undoes the last change; for a test. */
    public static void undo() {
        if (open != null) {
            open.undo();
        }
    }

    /** Adds a component of kind {@code kind} where the pointer last was over the screen, as Add does, for a test. */
    public static void add(final String kind) {
        if (open != null) {
            open.addAtPointer(kind);
        }
    }

    /** The keys of the fields on the chosen component's card, in order; for a test. */
    public static List<String> cardFields() {
        return open == null ? List.of() : List.copyOf(open.keys);
    }

    /** The box on the game's screen of the component written at {@code path} of the file's tree; for a test. */
    public static Optional<int[]> boxOf(final List<Integer> path) {
        return open == null ? Optional.empty() : open.model().nodeAt(path).flatMap(open.model()::boxOf);
    }

    private static void onKey(final ScreenEvent.KeyPressed.Pre event) {
        final Editing editing = open;
        if (editing == null || editing.screen != event.getScreen()) {
            return;
        }
        event.setCanceled(true);
        final int key = event.getKeyCode();
        if (editing.typing()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) {
                editing.blurAll();
            } else if (key == GLFW.GLFW_KEY_ENTER && editing.adding) {
                editing.addFirst();
            } else {
                editing.keyPressed(key, event.getScanCode(), event.getModifiers());
            }
            return;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            open = null;
        } else if (key == GLFW.GLFW_KEY_Z && Screen.hasControlDown()) {
            editing.undo();
        } else if (key == GLFW.GLFW_KEY_Y && Screen.hasControlDown()) {
            editing.redo();
        } else if (key == GLFW.GLFW_KEY_DELETE) {
            editing.delete();
        }
    }

    private static void onPress(final ScreenEvent.MouseButtonPressed.Pre event) {
        final Editing editing = open;
        if (editing == null || editing.screen != event.getScreen()) {
            return;
        }
        event.setCanceled(true);
        editing.press(event.getMouseX(), event.getMouseY(), event.getButton());
    }

    private static void onDrag(final ScreenEvent.MouseDragged.Pre event) {
        final Editing editing = open;
        if (editing != null && editing.screen == event.getScreen()) {
            event.setCanceled(true);
            editing.drag(event.getMouseX(), event.getMouseY());
        }
    }

    private static void onRelease(final ScreenEvent.MouseButtonReleased.Pre event) {
        final Editing editing = open;
        if (editing != null && editing.screen == event.getScreen()) {
            event.setCanceled(true);
            editing.release(event.getMouseX(), event.getMouseY(), event.getButton());
        }
    }

    private static void onRender(final ScreenEvent.Render.Post event) {
        final Editing editing = open;
        if (editing != null && editing.screen == event.getScreen()) {
            editing.render(event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
        }
    }

    private static String shortName(final ElementType<?> type) {
        return CORE_NAMESPACE.equals(type.id().getNamespace()) ? type.id().getPath() : type.id().toString();
    }

    private static String label(final TextKey key) {
        return GameText.resolve(key.text());
    }

    /* Whether {@code children} are only the line a new screen's face starts with, saying it is empty. */
    private static boolean saysEmpty(final JsonArray children) {
        return children.size() == 1 && children.get(0).isJsonObject() && children.get(0).getAsJsonObject().has("text")
                && DeveloperTexts.EMPTY_FACE.key().equals(children.get(0).getAsJsonObject().get("text").getAsString());
    }

    /* What a drag does: nothing, move the chosen component, or resize it by a corner. */
    private enum Drag {
        NONE,
        MOVE,
        RESIZE
    }

    /* The editor open on one screen: the file as it is now, the changes back and forth, and what is chosen. */
    private static final class Editing {

        private final Screen screen;
        private final DataScreenView view;
        private final Deque<String> undone = new ArrayDeque<>();
        private final Deque<String> redone = new ArrayDeque<>();
        private final Panel bar = new Panel();
        private final List<Button> barButtons = new ArrayList<>();
        private final Panel adder = new Panel();
        /* The chosen component's fields on its card, and the keys they write, made again as the kind chosen changes. */
        private final List<TextField> boxes = new ArrayList<>();
        private final List<String> keys = new ArrayList<>();
        private final TextField search = new TextField(32);
        private final Button styleButton;
        private final Button duplicate;
        private final Button delete;
        private Panel fields = new Panel();
        private JsonObject json;
        private @Nullable List<Integer> selected;
        private @Nullable InspectorModel model;
        private @Nullable String problem;
        private boolean adding;
        private int snap;
        private Drag drag = Drag.NONE;
        private @Nullable JsonObject dragStart;
        private double grabX;
        private double grabY;
        private int[] startBox = new int[4];
        private double pointerX;
        private double pointerY;

        private Editing(final Screen screen, final DataScreenView view, final JsonObject json) {
            this.screen = screen;
            this.view = view;
            this.json = json;
            barButtons.add(bar.add(new Button(() -> label(DeveloperTexts.UNDO), this::undo)));
            barButtons.add(bar.add(new Button(() -> label(DeveloperTexts.REDO), this::redo)));
            barButtons.add(bar.add(new Button(() -> GameText.resolve(DeveloperTexts.SNAP.with(SNAPS[snap])),
                    () -> snap = (snap + 1) % SNAPS.length)));
            barButtons.add(bar.add(new Button(() -> label(DeveloperTexts.ADD), () -> {
                adding = !adding;
                if (adding) {
                    adder.focus(search);
                }
            })));
            barButtons.add(bar.add(new Button(() -> label(DeveloperTexts.SAVE), this::write)));
            barButtons.add(bar.add(new Button(() -> CommonComponents.GUI_DONE.getString(), () -> open = null)));
            styleButton = new Button(this::styleLabel, this::nextStyle);
            duplicate = new Button(() -> label(DeveloperTexts.DUPLICATE), this::duplicate);
            delete = new Button(() -> label(DeveloperTexts.DELETE), this::delete);
            adder.add(search);
        }

        private InspectorModel model() {
            if (model == null || model.stale()) {
                model = new InspectorModel(view);
            }
            return model;
        }

        private boolean typing() {
            return fields.focusedChild() instanceof TextField || adder.focusedChild() instanceof TextField;
        }

        private void blurAll() {
            fields.focus(null);
            adder.focus(null);
        }

        private void charTyped(final int codePoint) {
            final char c = (char) codePoint;
            if (!fields.charTyped(c)) {
                adder.charTyped(c);
            }
        }

        private void keyPressed(final int key, final int scanCode, final int modifiers) {
            if (!fields.keyPressed(key, scanCode, modifiers)) {
                adder.keyPressed(key, scanCode, modifiers);
            }
        }

        private void press(final double x, final double y, final int button) {
            if (bar.mouseClicked(x, y, button) || adding && adder.mouseClicked(x, y, button)
                    || selected != null && fields.mouseClicked(x, y, button)) {
                return;
            }
            if (onAdder(x, y)) {
                final int row = (int) ((y - addRowsTop()) / ScreenEditorLayout.ADD_ROW);
                final List<String> kinds = kinds();
                if (row >= 0 && row < kinds.size()) {
                    addAtPointer(kinds.get(row));
                }
                return;
            }
            if (onPanel(x, y)) {
                return;
            }
            pointerX = x;
            pointerY = y;
            blurAll();
            if (selected != null) {
                final Optional<int[]> box = chosenBox();
                if (box.isPresent() && onHandle(box.get(), x, y)) {
                    drag = Drag.RESIZE;
                    startDrag(box.get(), x, y);
                    return;
                }
            }
            final Optional<BuiltNode> under = model().filedAt(x, y);
            selected = under.flatMap(model()::pathOf).orElse(null);
            fillFields();
            chosenBox().ifPresent(box -> {
                drag = Drag.MOVE;
                startDrag(box, x, y);
            });
        }

        private void startDrag(final int[] box, final double x, final double y) {
            dragStart = json.deepCopy();
            startBox = box.clone();
            grabX = x;
            grabY = y;
        }

        private void drag(final double x, final double y) {
            fields.mouseDragged(x, y, 0);
            if (drag == Drag.NONE || selected == null || dragStart == null) {
                return;
            }
            final int dx = snapped(x - grabX);
            final int dy = snapped(y - grabY);
            final JsonObject next = dragStart.deepCopy();
            if (drag == Drag.RESIZE) {
                ScreenJson.at(next, selected).ifPresent(component -> {
                    component.addProperty("width", Math.max(1, startBox[2] + dx));
                    component.addProperty("height", Math.max(1, startBox[3] + dy));
                });
                apply(next, false);
                return;
            }
            if (inStack(next, selected)) {
                // Moved freely, it is pulled onto the guides of the others within reach; Alt holds it where it is.
                final int[] pull = Screen.hasAltDown() ? new int[2] : AlignmentGuides.pull(new int[] {
                    startBox[0] + dx, startBox[1] + dy, startBox[2], startBox[3]}, model().boxesBut(selected), REACH);
                ScreenJson.at(next, selected).ifPresent(component -> {
                    final int[] margin = margin(component);
                    final JsonArray moved = new JsonArray();
                    moved.add(Math.max(0, margin[0] + dy + pull[1]));
                    moved.add(margin[1]);
                    moved.add(margin[2]);
                    moved.add(Math.max(0, margin[3] + dx + pull[0]));
                    component.add("margin", moved);
                });
                apply(next, false);
            }
        }

        private void release(final double x, final double y, final int button) {
            fields.mouseReleased(x, y, button);
            bar.mouseReleased(x, y, button);
            if (drag == Drag.MOVE && selected != null && dragStart != null && !inStack(dragStart, selected)) {
                // In a row or a column a component dropped on a sibling takes its place.
                final List<Integer> path = selected;
                final Optional<List<Integer>> onto = model().filedAt(x, y).flatMap(model()::pathOf)
                        .filter(other -> other.size() == path.size() && !other.equals(path)
                                && other.subList(0, other.size() - 1).equals(path.subList(0, path.size() - 1)));
                if (onto.isPresent()) {
                    final JsonObject next = dragStart.deepCopy();
                    selected = ScreenJson.move(next, path, onto.get().get(onto.get().size() - 1));
                    record(dragStart);
                    apply(next, true);
                }
            } else if (drag != Drag.NONE && dragStart != null && !ScreenJson.pretty(dragStart)
                    .equals(ScreenJson.pretty(json))) {
                record(dragStart);
                write();
            }
            drag = Drag.NONE;
            dragStart = null;
        }

        private void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
            final Minecraft minecraft = Minecraft.getInstance();
            final Font font = minecraft.font;
            final StyleSkin skin = Styles.skin(Styles.game());
            final InspectorPalette.Colours colours = InspectorPalette.get();
            final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
            // Where Add puts a component: the pointer's last place over the screen, not over the editor's own parts.
            if (mouseY >= ScreenEditorLayout.PANEL_Y && !onPanel(mouseX, mouseY) && !onAdder(mouseX, mouseY)) {
                pointerX = mouseX;
                pointerY = mouseY;
            }
            g.pose().pushPose();
            g.pose().translate(0, 0, IN_FRONT);
            chosenBox().ifPresent(box -> {
                guides(g, box, colours);
                chosen(g, box, colours);
            });
            // Each button as wide as its label needs in the language chosen, never narrower than its least.
            final int[] widths = new int[barButtons.size()];
            for (int i = 0; i < widths.length; i++) {
                widths[i] = ScreenEditorLayout.barWidth(i, font.width(barButtons.get(i).label()));
            }
            final int barLeft = ScreenEditorLayout.barLeft(screen.width, widths);
            final String editing = Texts.clip(font, GameText.resolve(DeveloperTexts.EDITING.with(
                    view.file().toString())), Math.max(0, barLeft - 2 * ScreenEditorLayout.PAD));
            Draw.text(g, font, editing, ScreenEditorLayout.PAD, ScreenEditorLayout.BAR_Y + 3, colours.handle());
            for (int i = 0; i < widths.length; i++) {
                barButtons.get(i).setBounds(ScreenEditorLayout.barX(screen.width, widths, i),
                        ScreenEditorLayout.BAR_Y, widths[i], ScreenEditorLayout.BUTTON_HEIGHT);
            }
            bar.render(g, ctx);
            if (selected != null) {
                panel(g, font, skin, ctx, colours);
            }
            if (adding) {
                adder(g, font, skin, ctx, colours);
            }
            final String hints = label(DeveloperTexts.HINTS);
            final int hintsWidth = Math.round(font.width(hints) * SMALL);
            Draw.textScaled(g, font, hints, (screen.width - hintsWidth) / 2, screen.height
                    - ScreenEditorLayout.HINTS_UP, colours.handle(), SMALL);
            if (problem != null) {
                Draw.textScaled(g, font, font.plainSubstrByWidth(problem, Math.round(screen.width / SMALL) - 8), 4,
                        screen.height - ScreenEditorLayout.HINTS_UP - 10, colours.error(), SMALL);
            }
            g.pose().popPose();
        }

        /*
         * The card of the chosen component, beside the screen at the component's height: its kind and line, the
         * fields its kind has, the style, its box, and two buttons.
         */
        private void panel(final GuiGraphics g, final Font font, final StyleSkin skin, final UiContext ctx,
                           final InspectorPalette.Colours colours) {
            final int left = ScreenEditorLayout.PANEL_X;
            final int top = panelTop();
            final int pad = ScreenEditorLayout.PAD;
            final int width = panelWidth();
            final int inner = width - 2 * pad;
            final int count = boxes.size();
            skin.window(g, left, top, width, ScreenEditorLayout.panelHeight(count));
            final Optional<BuiltNode> node = model().nodeAt(selected);
            Draw.text(g, font, Texts.clip(font, node.map(model()::name).orElse(""), inner), left + pad,
                    top + ScreenEditorLayout.TITLE_Y, colours.text());
            Draw.textScaled(g, font, Texts.clip(font, node.flatMap(model()::where).orElse(""),
                    Math.round(inner / SMALL)), left + pad, top + ScreenEditorLayout.FILE_Y, colours.dim(), SMALL);
            for (int i = 0; i < count; i++) {
                Draw.textScaled(g, font, Texts.clip(font, fieldLabel(i), Math.round(inner / SMALL)), left + pad,
                        top + ScreenEditorLayout.FIELDS_Y + i * ScreenEditorLayout.FIELD_STEP, colours.text(), SMALL);
                boxes.get(i).setBounds(left + pad, top + ScreenEditorLayout.fieldY(i), inner,
                        ScreenEditorLayout.FIELD_HEIGHT);
            }
            styleButton.setBounds(left + pad, top + ScreenEditorLayout.styleY(count), inner,
                    ScreenEditorLayout.BUTTON_HEIGHT);
            chosenBox().ifPresent(box -> Draw.textScaled(g, font, GameText.resolve(DeveloperTexts.POSITION.with(
                    box[0] - view.left(), box[1] - view.top(), box[2], box[3])), left + pad,
                    top + ScreenEditorLayout.positionY(count), colours.text(), SMALL));
            final int half = (inner - ScreenEditorLayout.BUTTON_GAP) / 2;
            duplicate.setBounds(left + pad, top + ScreenEditorLayout.actionsY(count), half,
                    ScreenEditorLayout.BUTTON_HEIGHT);
            delete.setBounds(left + pad + half + ScreenEditorLayout.BUTTON_GAP, top
                    + ScreenEditorLayout.actionsY(count), half, ScreenEditorLayout.BUTTON_HEIGHT);
            fields.render(g, ctx);
        }

        /* Where the card's top stands: at the chosen component's height, on the screen. */
        private int panelTop() {
            return ScreenEditorLayout.panelTop(chosenBox().map(box -> box[1]).orElse(ScreenEditorLayout.PANEL_Y),
                    boxes.size(), screen.height);
        }

        /* The kinds to add, those the search names, and how to add one. */
        private void adder(final GuiGraphics g, final Font font, final StyleSkin skin, final UiContext ctx,
                           final InspectorPalette.Colours colours) {
            final int left = addLeft();
            final int top = ScreenEditorLayout.PANEL_Y;
            final int pad = ScreenEditorLayout.PAD;
            final int width = addWidth();
            skin.window(g, left, top, width, ScreenEditorLayout.ADD_HEIGHT);
            search.setBounds(left + pad, top + pad, width - 2 * pad, ScreenEditorLayout.FIELD_HEIGHT);
            adder.render(g, ctx);
            final List<String> kinds = kinds();
            for (int i = 0; i < Math.min(ScreenEditorLayout.ADD_ROWS, kinds.size()); i++) {
                final int y = addRowsTop() + i * ScreenEditorLayout.ADD_ROW;
                if (i == 0) {
                    g.fill(left + 2, y - 1, left + width - 2, y + ScreenEditorLayout.ADD_ROW - 1, colours.selected());
                }
                Draw.text(g, font, Texts.clip(font, kinds.get(i), width - 2 * pad), left + pad, y, colours.text());
            }
            Draw.textScaled(g, font, label(DeveloperTexts.ADD_HINT), left + pad, top + ScreenEditorLayout.ADD_HEIGHT
                    - pad - 7, colours.dim(), SMALL);
        }

        /* The lines along which the chosen component lines up with the others, each over both. */
        private void guides(final GuiGraphics g, final int[] box, final InspectorPalette.Colours colours) {
            if (selected == null) {
                return;
            }
            for (final AlignmentGuides.Guide guide : AlignmentGuides.of(box, model().boxesBut(selected))) {
                if (guide.down()) {
                    g.fill(guide.at(), guide.from(), guide.at() + 1, guide.to(), colours.guide());
                } else {
                    g.fill(guide.from(), guide.at(), guide.to(), guide.at() + 1, colours.guide());
                }
            }
        }

        /* The chosen component's box drawn round in dots, and its eight squares. */
        private void chosen(final GuiGraphics g, final int[] box, final InspectorPalette.Colours colours) {
            final int x0 = box[0];
            final int y0 = box[1];
            final int x1 = x0 + box[2];
            final int y1 = y0 + box[3];
            for (int x = x0; x < x1; x += 2) {
                g.fill(x, y0, x + 1, y0 + 1, colours.handle());
                g.fill(x, y1 - 1, x + 1, y1, colours.handle());
            }
            for (int y = y0; y < y1; y += 2) {
                g.fill(x0, y, x0 + 1, y + 1, colours.handle());
                g.fill(x1 - 1, y, x1, y + 1, colours.handle());
            }
            final int mx = (x0 + x1) / 2;
            final int my = (y0 + y1) / 2;
            for (final int[] at : new int[][] {{x0, y0}, {mx, y0}, {x1, y0}, {x0, my}, {x1, my}, {x0, y1}, {mx, y1},
                {x1, y1}}) {
                g.fill(at[0] - HANDLE / 2 - 1, at[1] - HANDLE / 2 - 1, at[0] + HANDLE / 2 + 1,
                        at[1] + HANDLE / 2 + 1, colours.warningText());
                g.fill(at[0] - HANDLE / 2, at[1] - HANDLE / 2, at[0] + HANDLE / 2, at[1] + HANDLE / 2,
                        colours.handle());
            }
        }

        private Optional<int[]> chosenBox() {
            return selected == null ? Optional.empty() : model().nodeAt(selected).flatMap(model()::boxOf);
        }

        private static boolean onHandle(final int[] box, final double x, final double y) {
            final int x1 = box[0] + box[2];
            final int y1 = box[1] + box[3];
            return Math.abs(x - x1) <= REACH && Math.abs(y - y1) <= REACH;
        }

        /*
         * The chosen component's card: its id, what it is bound to, and the fields its kind declares, as many as the
         * card holds, each with what the file says of it; made again when the fields it has change.
         */
        void fillFields() {
            final Optional<JsonObject> component = selected == null ? Optional.empty()
                    : ScreenJson.at(json, selected);
            final List<String> wanted = fieldKeys(component);
            if (!wanted.equals(keys)) {
                keys.clear();
                keys.addAll(wanted);
                boxes.clear();
                fields = new Panel();
                for (final String key : keys) {
                    final TextField box = fields.add(new TextField(256));
                    box.setOnCommit(value -> setField(key, value));
                    boxes.add(box);
                }
                fields.add(styleButton);
                fields.add(duplicate);
                fields.add(delete);
            }
            for (int i = 0; i < keys.size(); i++) {
                final String key = keys.get(i);
                boxes.get(i).set(component.map(found -> found.get(key)).filter(JsonElement::isJsonPrimitive)
                        .map(JsonElement::getAsString).orElse(""));
            }
        }

        /*
         * The keys of a component's card: its id and what it is bound to, then each field its kind declares, read from
         * the kind's own codec, that the file leaves out or writes as a plain value; a list or a table is the file's.
         */
        private List<String> fieldKeys(final Optional<JsonObject> component) {
            final List<String> out = new ArrayList<>(List.of("id", "bind"));
            component.map(found -> found.get("type")).filter(JsonElement::isJsonPrimitive)
                    .flatMap(type -> ElementTypes.get(kindId(type.getAsString())))
                    .ifPresent(kind -> kind.props().keys(JsonOps.INSTANCE).map(JsonElement::getAsString)
                            .filter(key -> !NOT_A_FIELD.contains(key) && !out.contains(key))
                            .filter(key -> component.map(found -> found.get(key)).map(JsonElement::isJsonPrimitive)
                                    .orElse(true))
                            .forEach(out::add));
            return List.copyOf(out.subList(0, Math.min(out.size(), ScreenEditorLayout.MOST_FIELDS)));
        }

        private String fieldLabel(final int index) {
            return switch (index) {
                case 0 -> label(DeveloperTexts.NAME);
                case 1 -> label(DeveloperTexts.BOUND_TO);
                default -> keys.get(index).substring(0, 1).toUpperCase(Locale.ROOT)
                        + keys.get(index).substring(1).replace('_', ' ');
            };
        }

        void setField(final String key, final String value) {
            if (selected == null || key == null) {
                return;
            }
            final JsonObject next = json.deepCopy();
            ScreenJson.at(next, selected).ifPresent(component -> {
                if (value.isBlank()) {
                    component.remove(key);
                } else {
                    component.add(key, typed(value));
                }
            });
            record(json);
            apply(next, true);
        }

        private String styleLabel() {
            final JsonElement style = json.get("style");
            return GameText.resolve(DeveloperTexts.STYLE_IS.with(style == null ? Styles.game().id().getPath()
                    : style.getAsString()));
        }

        private void nextStyle() {
            final List<Style> all = Styles.all();
            if (all.isEmpty()) {
                return;
            }
            final JsonElement now = json.get("style");
            int at = -1;
            for (int i = 0; i < all.size(); i++) {
                if (now != null && all.get(i).id().toString().equals(now.getAsString())) {
                    at = i;
                }
            }
            final JsonObject next = json.deepCopy();
            next.addProperty("style", all.get((at + 1) % all.size()).id().toString());
            record(json);
            apply(next, true);
        }

        private void duplicate() {
            if (selected == null) {
                return;
            }
            final JsonObject next = json.deepCopy();
            final Optional<List<Integer>> copy = ScreenJson.duplicate(next, selected);
            if (copy.isPresent()) {
                record(json);
                selected = copy.get();
                apply(next, true);
            }
        }

        private void delete() {
            if (selected == null || selected.isEmpty()) {
                return;
            }
            final JsonObject next = json.deepCopy();
            ScreenJson.remove(next, selected);
            record(json);
            selected = null;
            apply(next, true);
        }

        private void undo() {
            if (undone.isEmpty()) {
                return;
            }
            redone.push(ScreenJson.pretty(json));
            apply(JsonParser.parseString(undone.pop()).getAsJsonObject(), true);
        }

        private void redo() {
            if (redone.isEmpty()) {
                return;
            }
            undone.push(ScreenJson.pretty(json));
            apply(JsonParser.parseString(redone.pop()).getAsJsonObject(), true);
        }

        private void record(final JsonObject before) {
            undone.push(ScreenJson.pretty(before));
            redone.clear();
        }

        private List<String> kinds() {
            final String wanted = search.edit().toLowerCase(Locale.ROOT);
            final List<String> out = new ArrayList<>();
            for (final ElementType<?> type : ElementTypes.all()) {
                final String name = shortName(type);
                if (name.toLowerCase(Locale.ROOT).contains(wanted)) {
                    out.add(name);
                }
            }
            out.sort(String::compareTo);
            return out;
        }

        private void addFirst() {
            final List<String> kinds = kinds();
            if (!kinds.isEmpty()) {
                addAtPointer(kinds.get(0));
            }
        }

        /* A component of the kind added to the deepest one under the pointer that holds others, else the root. */
        private void addAtPointer(final String kind) {
            List<Integer> container = List.of();
            final Optional<List<Integer>> under = model().filedAt(pointerX, pointerY).flatMap(model()::pathOf);
            if (under.isPresent()) {
                for (int length = under.get().size(); length >= 0; length--) {
                    final List<Integer> path = under.get().subList(0, length);
                    if (holds(path)) {
                        container = List.copyOf(path);
                        break;
                    }
                }
            }
            addKind(kind, container);
        }

        void addKind(final String kind, final List<Integer> container) {
            final JsonObject next = json.deepCopy();
            final JsonObject component = new JsonObject();
            component.addProperty("type", kind);
            // The first component put in a new screen's empty face takes the place of the line saying it is empty.
            ScreenJson.childrenOf(next, container).filter(ScreenEditor::saysEmpty)
                    .ifPresent(children -> children.remove(0));
            final Optional<List<Integer>> added = ScreenJson.append(next, container, component);
            if (added.isPresent()) {
                record(json);
                selected = added.get();
                adding = false;
                apply(next, true);
            }
        }

        private boolean holds(final List<Integer> path) {
            return ScreenJson.at(json, path).map(found -> found.get("type")).filter(JsonElement::isJsonPrimitive)
                    .flatMap(type -> ElementTypes.get(kindId(type.getAsString()))).map(ElementType::holds)
                    .orElse(false);
        }

        /* Takes {@code next} as the file: the screen built from it at once, and written to the source when kept. */
        private void apply(final JsonObject next, final boolean keep) {
            final Optional<String> wrong = ScreenFiles.put(view.file(), next);
            if (wrong.isPresent()) {
                problem = GameText.resolve(DeveloperTexts.NOT_TAKEN.with(wrong.get()));
                return;
            }
            problem = null;
            json = next;
            ((IDataScreen) screen).rebuild();
            fillFields();
            if (keep) {
                write();
            }
        }

        private void write() {
            try {
                if (ScreenJson.write(view.file(), json).isEmpty()) {
                    problem = GameText.resolve(DeveloperTexts.NO_SOURCE.with(view.file().getNamespace()));
                }
            } catch (final IOException unwritten) {
                problem = GameText.resolve(DeveloperTexts.NOT_TAKEN.with(unwritten.getMessage()));
            }
        }

        private int snapped(final double delta) {
            final int step = Screen.hasAltDown() ? 1 : SNAPS[snap];
            return (int) Math.round(delta / step) * step;
        }

        private int addLeft() {
            return screen.width - ScreenEditorLayout.PAD - addWidth();
        }

        private int addWidth() {
            return ScreenEditorLayout.addWidth(screen.width, view.left() + view.width());
        }

        private int panelWidth() {
            return ScreenEditorLayout.panelWidth(view.left());
        }

        private boolean onPanel(final double x, final double y) {
            final int top = panelTop();
            return selected != null && x >= ScreenEditorLayout.PANEL_X && x < ScreenEditorLayout.PANEL_X
                    + panelWidth() && y >= top && y < top + ScreenEditorLayout.panelHeight(boxes.size());
        }

        private boolean onAdder(final double x, final double y) {
            return adding && x >= addLeft() && y >= ScreenEditorLayout.PANEL_Y && y < ScreenEditorLayout.PANEL_Y
                    + ScreenEditorLayout.ADD_HEIGHT;
        }

        private int addRowsTop() {
            return ScreenEditorLayout.PANEL_Y + ScreenEditorLayout.PAD + ScreenEditorLayout.FIELD_HEIGHT + 4;
        }

        private static boolean inStack(final JsonObject file, final List<Integer> path) {
            if (path.isEmpty()) {
                return false;
            }
            return ScreenJson.at(file, path.subList(0, path.size() - 1)).map(parent -> parent.get("type"))
                    .filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsString)
                    .map(type -> kindId(type).getPath().equals("stack")).orElse(false);
        }

        private static ResourceLocation kindId(final String type) {
            return type.contains(":") ? ResourceLocation.parse(type)
                    : ResourceLocation.fromNamespaceAndPath(CORE_NAMESPACE, type);
        }

        /* The margin as top, right, bottom and left, from one, two or four numbers. */
        private static int[] margin(final JsonObject component) {
            final JsonElement margin = component.get("margin");
            if (margin == null) {
                return new int[4];
            }
            if (margin.isJsonPrimitive()) {
                final int all = margin.getAsInt();
                return new int[] {all, all, all, all};
            }
            final JsonArray parts = margin.getAsJsonArray();
            if (parts.size() == 2) {
                return new int[] {parts.get(0).getAsInt(), parts.get(1).getAsInt(), parts.get(0).getAsInt(),
                    parts.get(1).getAsInt()};
            }
            return new int[] {parts.get(0).getAsInt(), parts.get(1).getAsInt(), parts.get(2).getAsInt(),
                parts.get(3).getAsInt()};
        }

        /* A field's text as the file writes it: a number when it is one, true or false, else words. */
        private static JsonElement typed(final String value) {
            final String trimmed = value.trim();
            if ("true".equals(trimmed) || "false".equals(trimmed)) {
                return new JsonPrimitive(Boolean.parseBoolean(trimmed));
            }
            try {
                return new JsonPrimitive(Integer.parseInt(trimmed));
            } catch (final NumberFormatException notWhole) {
                try {
                    return new JsonPrimitive(Double.parseDouble(trimmed));
                } catch (final NumberFormatException notNumber) {
                    return new JsonPrimitive(value);
                }
            }
        }
    }
}
