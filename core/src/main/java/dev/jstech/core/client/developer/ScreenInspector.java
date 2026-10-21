/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.developer;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.data.BuiltNode;
import dev.jstech.core.client.gui.data.DataScreenView;
import dev.jstech.core.client.gui.data.IDataScreen;
import dev.jstech.core.client.gui.data.ScreenWiring;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.developer.DeveloperMode;
import dev.jstech.core.gui.layout.InspectorLayout;
import dev.jstech.core.text.GameText;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The developer's inspector of a screen declared in a file, opened and closed by F12 in a development run: a panel
 * beside the screen with the tree of what it is built of (a blue square for what only lays others out, a green one
 * for a component), the problems found in it, and buttons that draw it at another GUI scale, in the other language
 * of the series and in another of the states its code offers; over the screen, each box dashed in its colour and each
 * problem marked where it is; and a card of the component under the pointer: its box, what it is bound to, its style,
 * when it is drawn again, and its line in the file, which Ctrl+C copies.
 */
public final class ScreenInspector {

    /** How small the small text of the panel is drawn. */
    private static final float SMALL = 0.75F;
    /** How far in front of the screen the inspector draws, over the items in its slots. */
    private static final float IN_FRONT = 400.0F;
    /** Every how many pixels the dashes of a box start, and how long each is. */
    private static final int DASH_EVERY = 4;
    private static final int DASH = 2;
    /** The problems the panel lists at most. */
    private static final int MOST_PROBLEMS = 6;
    /** How wide a problem read whole under the pointer is at most before it wraps. */
    private static final int WHOLE_PROBLEM = 220;
    private static final String MARK_SIGN = "!";
    private static final List<String> LANGUAGES = List.of("en_us", "pt_br");

    private static @Nullable State open;

    private ScreenInspector() {
    }

    /** Listens for the screens' keys, clicks and frames, once, from the Core's client. */
    @ApiStatus.Internal
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ScreenEvent.KeyPressed.Pre.class, ScreenInspector::onKey);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.MouseButtonPressed.Pre.class, ScreenInspector::onClick);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Post.class, ScreenInspector::onRender);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Closing.class, event -> {
            if (open != null && open.screen == event.getScreen()) {
                open = null;
            }
        });
    }

    /** Opens the inspector on {@code screen}, or closes it there; nothing outside a development run. */
    public static void toggle(final Screen screen) {
        if (!DeveloperMode.on() || !(screen instanceof IDataScreen data)) {
            return;
        }
        open = open != null && open.screen == screen ? null : new State(screen, data.view());
    }

    /** Whether the inspector is open on {@code screen}. */
    public static boolean openOn(final Screen screen) {
        return open != null && open.screen == screen;
    }

    /** The tree's rows as the open inspector shows them, for a test to read; empty when it is closed. */
    public static List<String> rows() {
        if (open == null) {
            return List.of();
        }
        return open.model().rows(open.folded).stream().map(row -> row.name() + " " + row.detail()).toList();
    }

    /** The problems the open inspector found, for a test to read; empty when it is closed. */
    public static List<String> problems() {
        return open == null ? List.of() : open.model().problems().stream().map(InspectorModel.Problem::text)
                .toList();
    }

    /** The card of the component under {@code x}, {@code y}, as the open inspector shows it; for a test. */
    public static List<String> cardAt(final double x, final double y) {
        if (open == null) {
            return List.of();
        }
        return open.model().at(x, y).map(node -> open.model().card(node).stream().map(Component::getString)
                .toList()).orElse(List.of());
    }

    /** The node the editor works on: the one chosen in the tree, else none. */
    static Optional<BuiltNode> chosen() {
        return open == null ? Optional.empty() : Optional.ofNullable(open.selected);
    }

    private static void onKey(final ScreenEvent.KeyPressed.Pre event) {
        if (!DeveloperMode.on()) {
            return;
        }
        if (event.getKeyCode() == GLFW.GLFW_KEY_F12) {
            toggle(event.getScreen());
            event.setCanceled(true);
            return;
        }
        final State state = open;
        if (state == null || state.screen != event.getScreen()) {
            return;
        }
        if (event.getKeyCode() == GLFW.GLFW_KEY_C && Screen.hasControlDown()) {
            final Minecraft minecraft = Minecraft.getInstance();
            final double[] mouse = mouse(minecraft);
            state.model().at(mouse[0], mouse[1]).or(() -> Optional.ofNullable(state.selected))
                    .flatMap(state.model()::where)
                    .ifPresent(where -> minecraft.keyboardHandler.setClipboard(where));
            event.setCanceled(true);
        } else if (event.getKeyCode() == GLFW.GLFW_KEY_E && Screen.hasControlDown()) {
            ScreenEditor.open(event.getScreen(), state.view);
            event.setCanceled(true);
        }
    }

    private static void onClick(final ScreenEvent.MouseButtonPressed.Pre event) {
        final State state = open;
        if (state == null || state.screen != event.getScreen()) {
            return;
        }
        final Frame frame = state.frame(event.getScreen().height);
        final double x = event.getMouseX();
        final double y = event.getMouseY();
        if (!frame.holds(x, y)) {
            return;
        }
        event.setCanceled(true);
        if (state.buttons.mouseClicked(x, y, event.getButton())) {
            state.buttons.mouseReleased(x, y, event.getButton());
            return;
        }
        final int index = (int) Math.floor((y - frame.top - InspectorLayout.TREE_Y) / InspectorLayout.ROW);
        if (index < 0 || index >= frame.rows.size()) {
            return;
        }
        final InspectorModel.Row row = frame.rows.get(index);
        final int triangle = frame.left + InspectorLayout.PAD + row.depth() * InspectorLayout.INDENT;
        if (row.parent() && x < triangle + InspectorLayout.TRIANGLE) {
            if (!state.folded.remove(row.node())) {
                state.folded.add(row.node());
            }
        } else {
            state.selected = row.node();
        }
    }

    private static void onRender(final ScreenEvent.Render.Post event) {
        final State state = open;
        if (state == null || state.screen != event.getScreen()) {
            return;
        }
        final GuiGraphics g = event.getGuiGraphics();
        final Minecraft minecraft = Minecraft.getInstance();
        final InspectorPalette.Colours colours = InspectorPalette.get();
        final InspectorModel model = state.model();
        g.pose().pushPose();
        g.pose().translate(0, 0, IN_FRONT);
        state.view.tree().ifPresent(root -> root.forEach(node -> model.boxOf(node).ifPresent(box -> {
            dashed(g, box, node.component() == null ? colours.layout() : colours.component());
            model.problemOf(node).ifPresent(problem -> mark(g, minecraft.font, box[0], box[1], problem, colours));
        })));
        final Frame frame = state.frame(event.getScreen().height);
        panel(g, minecraft, state, frame, colours, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        if (!frame.holds(event.getMouseX(), event.getMouseY())) {
            model.at(event.getMouseX(), event.getMouseY()).ifPresent(node -> g.renderComponentTooltip(
                    minecraft.font, model.card(node), event.getMouseX(), event.getMouseY()));
        } else {
            // A problem cut to the panel's width is read whole under the pointer.
            problemAt(state, frame, event.getMouseY()).ifPresent(problem -> g.renderTooltip(minecraft.font,
                    minecraft.font.split(Component.literal(problem.text()), WHOLE_PROBLEM), event.getMouseX(),
                    event.getMouseY()));
        }
        g.pose().popPose();
    }

    private static Optional<InspectorModel.Problem> problemAt(final State state, final Frame frame, final double y) {
        final int first = frame.top + InspectorLayout.problemsY(frame.rows.size()) + InspectorLayout.HEADING;
        final int index = (int) Math.floor((y - first) / InspectorLayout.PROBLEM);
        final List<InspectorModel.Problem> problems = state.model().problems();
        return y < first || index >= Math.min(MOST_PROBLEMS, problems.size()) ? Optional.empty()
                : Optional.of(problems.get(index));
    }

    /* The panel: its window, title and key, legend, tree, problems and buttons. */
    private static void panel(final GuiGraphics g, final Minecraft minecraft, final State state, final Frame frame,
                              final InspectorPalette.Colours colours, final int mouseX, final int mouseY,
                              final float partialTick) {
        final Font font = minecraft.font;
        final StyleSkin skin = Styles.skin(Styles.game());
        final int left = frame.left;
        final int top = frame.top;
        skin.window(g, left, top, frame.width, frame.height);
        Draw.text(g, font, GameText.component(DeveloperTexts.INSPECTOR), left + InspectorLayout.PAD,
                top + InspectorLayout.TITLE_Y, colours.text());
        final Component key = GameText.component(DeveloperTexts.INSPECTOR_KEY);
        Draw.text(g, font, key, left + frame.width - InspectorLayout.PAD - font.width(key),
                top + InspectorLayout.TITLE_Y, colours.dim());
        legend(g, font, left + InspectorLayout.PAD, top + InspectorLayout.LEGEND_Y, colours);
        for (int i = 0; i < frame.rows.size(); i++) {
            row(g, font, frame.rows.get(i), frame, top + InspectorLayout.TREE_Y + i * InspectorLayout.ROW, state,
                    colours);
        }
        final int problemsY = top + InspectorLayout.problemsY(frame.rows.size());
        Draw.text(g, font, GameText.component(DeveloperTexts.PROBLEMS), left + InspectorLayout.PAD, problemsY,
                colours.text());
        final List<InspectorModel.Problem> problems = state.model().problems();
        if (problems.isEmpty()) {
            Draw.textScaled(g, font, GameText.resolve(DeveloperTexts.NO_PROBLEMS.text()), left + InspectorLayout.PAD,
                    problemsY + InspectorLayout.HEADING, colours.dim(), SMALL);
        }
        for (int i = 0; i < Math.min(MOST_PROBLEMS, problems.size()); i++) {
            final int py = problemsY + InspectorLayout.HEADING + i * InspectorLayout.PROBLEM;
            mark(g, font, left + InspectorLayout.PAD, py, problems.get(i), colours);
            final String said = Texts.clip(font, problems.get(i).text(), Math.round((frame.width
                    - 2 * InspectorLayout.PAD - InspectorLayout.MARK - 3) / SMALL));
            Draw.textScaled(g, font, said, left + InspectorLayout.PAD + InspectorLayout.MARK + 3, py + 1,
                    colours.text(), SMALL);
        }
        state.placeButtons(left, top + InspectorLayout.buttonsY(frame.rows.size(), frame.problems), frame.width);
        state.buttons.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
    }

    private static void legend(final GuiGraphics g, final Font font, final int x, final int y,
                               final InspectorPalette.Colours colours) {
        final int square = InspectorLayout.SQUARE;
        g.fill(x, y + 1, x + square, y + 1 + square, colours.layout());
        final String layout = GameText.resolve(DeveloperTexts.LAYOUT.text());
        Draw.textScaled(g, font, layout, x + square + 3, y, colours.dim(), SMALL);
        final int next = x + square + 3 + Math.round(font.width(layout) * SMALL) + 12;
        g.fill(next, y + 1, next + square, y + 1 + square, colours.component());
        Draw.textScaled(g, font, GameText.resolve(DeveloperTexts.COMPONENT.text()), next + square + 3, y,
                colours.dim(), SMALL);
    }

    private static void row(final GuiGraphics g, final Font font, final InspectorModel.Row row, final Frame frame,
                            final int y, final State state, final InspectorPalette.Colours colours) {
        final int left = frame.left;
        final int right = left + frame.width;
        if (row.node() == state.selected) {
            g.fill(left + 2, y - 1, right - 2, y + InspectorLayout.ROW - 1, colours.selected());
        }
        int x = left + InspectorLayout.PAD + row.depth() * InspectorLayout.INDENT;
        if (row.parent()) {
            triangle(g, x, y + 2, state.folded.contains(row.node()), colours.text());
        }
        x += InspectorLayout.TRIANGLE;
        g.fill(x, y + 2, x + InspectorLayout.SQUARE, y + 2 + InspectorLayout.SQUARE,
                row.layout() ? colours.layout() : colours.component());
        x += InspectorLayout.SQUARE + 3;
        final int room = right - InspectorLayout.PAD - InspectorLayout.MARK - 2 - x;
        final String name = Texts.clip(font, row.name(), room);
        Draw.text(g, font, name, x, y, colours.text());
        final int after = x + font.width(name) + 4;
        if (!row.detail().isEmpty() && after < x + room) {
            Draw.textScaled(g, font, Texts.clip(font, row.detail(), Math.round((x + room - after) / SMALL)),
                    after, y + 1, colours.dim(), SMALL);
        }
        row.problem().ifPresent(problem -> mark(g, font, right - InspectorLayout.PAD - InspectorLayout.MARK, y,
                problem, colours));
    }

    /* A triangle pointing right for a folded node, down for an open one. */
    private static void triangle(final GuiGraphics g, final int x, final int y, final boolean folded,
                                 final int colour) {
        for (int i = 0; i < 3; i++) {
            if (folded) {
                g.fill(x + i, y + i, x + i + 1, y + 5 - i, colour);
            } else {
                g.fill(x + i, y + i, x + 5 - i, y + i + 1, colour);
            }
        }
    }

    private static void mark(final GuiGraphics g, final Font font, final int x, final int y,
                             final InspectorModel.Problem problem, final InspectorPalette.Colours colours) {
        final int size = InspectorLayout.MARK;
        g.fill(x, y, x + size, y + size + 1, problem.error() ? colours.error() : colours.warning());
        Draw.textScaled(g, font, MARK_SIGN, x + 2, y + 1, problem.error() ? colours.errorText()
                : colours.warningText(), SMALL);
    }

    private static void dashed(final GuiGraphics g, final int[] box, final int colour) {
        final int x0 = box[0];
        final int y0 = box[1];
        final int x1 = x0 + box[2];
        final int y1 = y0 + box[3];
        for (int x = x0; x < x1; x += DASH_EVERY) {
            g.fill(x, y0, Math.min(x + DASH, x1), y0 + 1, colour);
            g.fill(x, y1 - 1, Math.min(x + DASH, x1), y1, colour);
        }
        for (int y = y0; y < y1; y += DASH_EVERY) {
            g.fill(x0, y, x0 + 1, Math.min(y + DASH, y1), colour);
            g.fill(x1 - 1, y, x1, Math.min(y + DASH, y1), colour);
        }
    }

    private static double[] mouse(final Minecraft minecraft) {
        return new double[] {minecraft.mouseHandler.xpos() * minecraft.getWindow().getGuiScaledWidth()
                / minecraft.getWindow().getScreenWidth(), minecraft.mouseHandler.ypos()
                * minecraft.getWindow().getGuiScaledHeight() / minecraft.getWindow().getScreenHeight()};
    }

    private static String scaleLabel() {
        final int scale = Minecraft.getInstance().options.guiScale().get();
        return GameText.resolve(DeveloperTexts.GUI_SCALE.with(scale == 0
                ? GameText.resolve(DeveloperTexts.GUI_SCALE_AUTO.text()) : Integer.toString(scale)));
    }

    private static void nextScale() {
        final Minecraft minecraft = Minecraft.getInstance();
        final int most = minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
        minecraft.options.guiScale().set((minecraft.options.guiScale().get() + 1) % (most + 1));
        minecraft.resizeDisplay();
    }

    private static String languageLabel() {
        final Minecraft minecraft = Minecraft.getInstance();
        final String code = minecraft.getLanguageManager().getSelected();
        final LanguageInfo info = minecraft.getLanguageManager().getLanguage(code);
        return GameText.resolve(DeveloperTexts.LANGUAGE.with(info == null ? code : info.name()));
    }

    private static void nextLanguage() {
        final Minecraft minecraft = Minecraft.getInstance();
        final int at = LANGUAGES.indexOf(minecraft.getLanguageManager().getSelected());
        final String next = LANGUAGES.get((at + 1) % LANGUAGES.size());
        minecraft.getLanguageManager().setSelected(next);
        minecraft.options.languageCode = next;
        minecraft.options.save();
        InspectorModel.forgetLanguages();
        minecraft.reloadResourcePacks();
    }

    /* The inspector open on one screen: what it folded and chose, and its buttons. */
    private static final class State {

        private final Screen screen;
        private final DataScreenView view;
        private final Set<BuiltNode> folded = Collections.newSetFromMap(new IdentityHashMap<>());
        private final Panel buttons = new Panel();
        private final Button scale;
        private final Button language;
        private final Button preview;
        private @Nullable InspectorModel model;
        private @Nullable BuiltNode selected;
        private int previewIndex = -1;

        private State(final Screen screen, final DataScreenView view) {
            this.screen = screen;
            this.view = view;
            this.scale = buttons.add(new Button(ScreenInspector::scaleLabel, ScreenInspector::nextScale));
            this.language = buttons.add(new Button(ScreenInspector::languageLabel, ScreenInspector::nextLanguage));
            this.preview = buttons.add(new Button(this::previewLabel, this::nextPreview));
        }

        private InspectorModel model() {
            if (model == null || model.stale()) {
                model = new InspectorModel(view);
                if (selected != null && view.tree().map(root -> !holds(root, selected)).orElse(true)) {
                    selected = null;
                }
            }
            return model;
        }

        private Frame frame(final int screenHeight) {
            final List<InspectorModel.Row> all = model().rows(folded);
            final int problems = Math.min(MOST_PROBLEMS, model().problems().size());
            final int buttonCount = view.previews().isEmpty() ? 2 : 3;
            final int top = Math.max(InspectorLayout.EDGE, Math.min(view.top(), screenHeight
                    - InspectorLayout.height(all.size(), problems, buttonCount) - InspectorLayout.EDGE));
            final int fixed = InspectorLayout.height(0, problems, buttonCount);
            final int fit = Math.max(1, (screenHeight - top - fixed - InspectorLayout.EDGE) / InspectorLayout.ROW);
            final List<InspectorModel.Row> rows = all.subList(0, Math.min(all.size(), fit));
            return new Frame(InspectorLayout.left(view.left()), top, InspectorLayout.width(view.left()), rows,
                    problems, InspectorLayout.height(rows.size(), problems, buttonCount));
        }

        private void placeButtons(final int left, final int top, final int panelWidth) {
            final int width = panelWidth - 2 * InspectorLayout.PAD;
            final int step = InspectorLayout.BUTTON_HEIGHT + InspectorLayout.BUTTON_GAP;
            scale.setBounds(left + InspectorLayout.PAD, top, width, InspectorLayout.BUTTON_HEIGHT);
            language.setBounds(left + InspectorLayout.PAD, top + step, width, InspectorLayout.BUTTON_HEIGHT);
            preview.setBounds(left + InspectorLayout.PAD, top + 2 * step, width, InspectorLayout.BUTTON_HEIGHT);
            preview.setVisible(!view.previews().isEmpty());
        }

        private String previewLabel() {
            final List<ScreenWiring.Preview> previews = view.previews();
            final String name = previewIndex < 0 || previewIndex >= previews.size()
                    ? GameText.resolve(DeveloperTexts.AS_IT_IS.text()) : previews.get(previewIndex).name();
            return GameText.resolve(DeveloperTexts.STATE.with(name));
        }

        private void nextPreview() {
            final List<ScreenWiring.Preview> previews = view.previews();
            if (previews.isEmpty()) {
                return;
            }
            previewIndex = (previewIndex + 1) % previews.size();
            previews.get(previewIndex).show().run();
        }

        private static boolean holds(final BuiltNode root, final BuiltNode node) {
            final boolean[] found = new boolean[1];
            root.forEach(each -> found[0] |= each == node);
            return found[0];
        }
    }

    /* Where the panel is this frame, how wide, and what of the tree and the problems it shows. */
    private record Frame(int left, int top, int width, List<InspectorModel.Row> rows, int problems, int height) {

        private boolean holds(final double x, final double y) {
            return x >= left && x < left + width && y >= top && y < top + height;
        }
    }
}
