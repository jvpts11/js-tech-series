/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.developer.RunThingTestsPayload;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * The studio, in full screen over the world: the things of every mod on the Core by mod and by kind, each with its
 * problems; the thing chosen on a bench, working, idle or jammed; what it is made of, each part the ordinary file it
 * always was, opened by a click; and the checks across its parts, each opening the file that mends it. Reload all
 * reads every file again, and Run its tests runs, near the player, the GameTests that name it.
 */
public final class StudioScreen extends Screen {

    private final Panel panel = new Panel();
    private final TextField search = panel.add(new TextField(SEARCH_LENGTH));
    private final Button checksButton;
    private final Map<BenchState, Button> stateButtons = new EnumMap<>(BenchState.class);
    private final List<Button> openButtons = new ArrayList<>();
    private final Button reloadButton;
    private final Button runButton;
    private final Button doneButton;
    private final StudioBench bench = new StudioBench();
    private Map<StudioThing, StudioSheet> sheets = Map.of();
    private StudioTree tree;
    private StudioLayout.Regions regions = StudioLayout.of(StudioLayout.MIN_WIDTH, StudioLayout.MIN_HEIGHT);
    private int partsScroll;
    private int checksScroll;
    private int problems;
    private boolean turning;
    private String status;

    private static final Logger LOGGER = CoreLogs.DEVELOPER.logger();
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final int SEARCH_LENGTH = 64;
    /** How small the small words are drawn, and how far in from a panel's side its words stand. */
    private static final float SMALL = 0.75F;
    private static final int INSET = 6;
    /** How far a kind and a thing stand in from their mod in the tree, and the room an arrow takes. */
    private static final int KIND_INDENT = 6;
    private static final int THING_INDENT = 15;
    private static final int ARROW_ROOM = 9;
    private static final int ICON = 16;
    private static final int ICON_ROOM = 22;
    /** The room the problems' count and its mark take at a row's right, and between a check's mark and its words. */
    private static final int BADGE_ROOM = 22;
    private static final int MARK_GAP = 5;
    /** How many Open buttons the checks hold at most at once. */
    private static final int OPENS = (StudioLayout.ACROSS_MAX - StudioLayout.HEAD) / StudioLayout.CHECK_ROW + 1;
    private static final String CRUMB = "  >  ";
    private static final String ELLIPSIS = "...";
    /** The level of operator the game's own {@code /reload} asks. */
    private static final int RELOADS = 2;

    /** The studio, every thing read. */
    public StudioScreen() {
        super(GameText.component(StudioTexts.STUDIO));
        this.search.setPlaceholder(GameText.resolve(StudioTexts.SEARCH));
        this.search.setOnEdit(() -> this.tree.search(this.search.edit()));
        this.checksButton = panel.add(new Button(() -> GameText.resolve(StudioTexts.CHECKS.with(this.problems)),
                () -> this.tree.nextWithProblems().ifPresent(this::choose)));
        for (final BenchState state : BenchState.values()) {
            this.stateButtons.put(state, panel.add(new Button(() -> GameText.resolve(state.title()),
                    () -> this.bench.state(state))));
        }
        for (int i = 0; i < OPENS; i++) {
            final int row = i;
            this.openButtons.add(panel.add(new Button(() -> GameText.resolve(StudioTexts.OPEN),
                    () -> this.openCheck(row))));
        }
        this.reloadButton = panel.add(new Button(() -> GameText.resolve(StudioTexts.RELOAD_ALL), this::reloadAll));
        this.runButton = panel.add(new Button(() -> GameText.resolve(StudioTexts.RUN_TESTS), this::runTests));
        this.doneButton = panel.add(new Button(() -> GameText.resolve(StudioTexts.DONE), this::onClose));
        this.status = GameText.resolve(StudioTexts.MODE_ON);
        this.read();
    }

    /** The thing chosen, if any; for a test. */
    public Optional<StudioThing> chosen() {
        return this.tree.chosen();
    }

    /** Chooses the thing of {@code id}, as a click on its row does; whether there is one. For a test as for a click. */
    public boolean choose(final ResourceLocation id) {
        final Optional<StudioThing> thing = this.sheets.keySet().stream().filter(any -> any.id().equals(id))
                .findFirst();
        thing.ifPresent(this::choose);
        return thing.isPresent();
    }

    /** What the thing chosen is made of and what is wrong across its parts; empty for none chosen. */
    public Optional<StudioSheet> sheet() {
        return this.tree.chosen().map(this.sheets::get);
    }

    /** How many problems every thing has, all told. */
    public int problems() {
        return this.problems;
    }

    /** Has a machine on the bench be in {@code state}; for a test as for a click on its button. */
    public void showState(final BenchState state) {
        this.bench.state(state);
    }

    /** What the foot says now. */
    public String status() {
        return this.status;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final StudioPalette.Colours colours = StudioPalette.get();
        this.bar(g, colours);
        this.bench(g, colours);
        this.panelGround(g, this.regions.tree(), colours);
        this.panelGround(g, this.regions.across(), colours);
        this.panelGround(g, this.regions.madeOf(), colours);
        final StudioLayout.Rect foot = this.regions.foot();
        g.fill(foot.x(), foot.y(), foot.right(), foot.bottom(), colours.foot());
        Draw.textScaled(g, font, this.cut(this.status, this.regions.reload().x() - 2 * INSET, SMALL), INSET,
                foot.y() + (StudioLayout.FOOT - 6) / 2, colours.dim(), SMALL);
        this.treeRows(g, colours);
        final List<Component> tip = new ArrayList<>();
        this.checkRows(g, colours, mouseX, mouseY, tip);
        this.partRows(g, colours, mouseX, mouseY, tip);
        this.placeButtons();
        panel.render(g, new UiContext(this.skin(), font, mouseX, mouseY, partialTick));
        if (this.bench.machine()) {
            final Button lit = this.stateButtons.get(this.bench.state());
            Draw.outline(g, lit.x() - 1, lit.y() - 1, lit.width() + 2, lit.height() + 2, colours.outline());
        }
        if (!tip.isEmpty()) {
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (panel.mouseClicked(mouseX, mouseY, button)) {
            panel.mouseReleased(mouseX, mouseY, button);
            return true;
        }
        final StudioLayout.Rect treeArea = this.regions.tree();
        if (treeArea.contains(mouseX, mouseY)) {
            final int row = this.tree.scroll() + (int) ((mouseY - treeArea.y() - INSET / 2) / StudioLayout.TREE_ROW);
            this.tree.click(row).ifPresent(this::choose);
            return true;
        }
        final StudioLayout.Rect parts = this.regions.madeOf();
        if (parts.contains(mouseX, mouseY) && mouseY >= parts.y() + StudioLayout.HEAD) {
            final int row = this.partsScroll + (int) ((mouseY - parts.y() - StudioLayout.HEAD)
                    / StudioLayout.PART_ROW);
            this.sheet().map(StudioSheet::parts).filter(all -> row < all.size())
                    .flatMap(all -> all.get(row).file()).ifPresent(this::open);
            return true;
        }
        if (this.regions.bench().contains(mouseX, mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.turning = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        if (this.turning) {
            this.bench.turn(dragX, dragY);
            return true;
        }
        return panel.mouseDragged(mouseX, mouseY, button) || super.mouseDragged(mouseX, mouseY, button, dragX,
                dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        this.turning = false;
        return panel.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        if (this.regions.bench().contains(mouseX, mouseY)) {
            this.bench.zoom(scrollY);
        } else if (this.regions.tree().contains(mouseX, mouseY)) {
            this.tree.scroll(scrollY, this.treeVisible());
        } else if (this.regions.madeOf().contains(mouseX, mouseY)) {
            final int rows = this.sheet().map(sheet -> sheet.parts().size()).orElse(0);
            this.partsScroll = scrolled(this.partsScroll, scrollY, rows, this.partsVisible());
        } else if (this.regions.across().contains(mouseX, mouseY)) {
            final int rows = this.sheet().map(sheet -> sheet.checks().size()).orElse(0);
            this.checksScroll = scrolled(this.checksScroll, scrollY, rows, this.checksVisible());
        } else {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (panel.focusedChild() != null && key != GLFW.GLFW_KEY_ESCAPE && panel.keyPressed(key, scanCode,
                modifiers)) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        return panel.charTyped(c) || super.charTyped(c, modifiers);
    }

    @Override
    public void removed() {
        this.bench.close();
        panel.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.regions = StudioLayout.of(width, height);
        final StudioLayout.Rect field = this.regions.search();
        this.search.setBounds(field.x(), field.y(), field.width(), field.height());
        set(this.checksButton, this.regions.checks());
        // The states' buttons stand in their order, working first.
        int place = 0;
        for (final Button state : this.stateButtons.values()) {
            set(state, this.regions.states().get(place++));
        }
        set(this.reloadButton, this.regions.reload());
        set(this.runButton, this.regions.runTests());
        set(this.doneButton, this.regions.done());
    }

    /* Reads every thing, and what each is made of, keeping the thing chosen and the words searched. */
    private void read() {
        final Minecraft game = Minecraft.getInstance();
        final Optional<ResourceLocation> was = this.tree == null ? Optional.empty()
                : this.tree.chosen().map(StudioThing::id);
        final long started = System.nanoTime();
        final StudioContext context = new StudioContext(game.getResourceManager());
        final List<StudioThing> things = StudioThings.all();
        final Map<StudioThing, String> names = new IdentityHashMap<>();
        final Map<StudioThing, StudioSheet> read = new HashMap<>();
        int found = 0;
        for (final StudioThing thing : things) {
            names.put(thing, thing.name().getString());
            final StudioSheet sheet = StudioReaders.read(thing, context);
            read.put(thing, sheet);
            found += sheet.checks().size();
        }
        LOGGER.info("The studio read {} things in {} ms, with {} problems", things.size(),
                (System.nanoTime() - started) / NANOS_PER_MILLI, found);
        this.sheets = read;
        this.problems = found;
        this.tree = new StudioTree(things, names, read);
        this.tree.search(this.search.edit());
        this.bench.show(null);
        was.ifPresent(this::choose);
    }

    private void choose(final StudioThing thing) {
        this.tree.choose(thing, this.treeVisible());
        this.bench.show(thing);
        this.partsScroll = 0;
        this.checksScroll = 0;
    }

    private void bar(final GuiGraphics g, final StudioPalette.Colours colours) {
        final StudioLayout.Rect bar = this.regions.bar();
        g.fill(bar.x(), bar.y(), bar.right(), bar.bottom(), colours.bar());
        final String studio = GameText.resolve(StudioTexts.STUDIO);
        final int textY = (StudioLayout.BAR - 8) / 2;
        Draw.text(g, font, studio, StudioLayout.PAD, textY, colours.text());
        final int crumbX = StudioLayout.PAD + font.width(studio) + 2 * INSET;
        this.tree.chosen().ifPresent(thing -> Draw.text(g, font, this.cut(String.join(CRUMB, this.crumbs(thing)),
                this.regions.search().x() - INSET - crumbX, 1.0F), crumbX, textY, colours.text()));
    }

    private void bench(final GuiGraphics g, final StudioPalette.Colours colours) {
        final StudioLayout.Rect area = this.regions.bench();
        g.fill(area.x(), area.y(), area.right(), area.bottom(), colours.sky());
        this.bench.draw(g, area);
        Draw.outline(g, area.x(), area.y(), area.width(), area.height(), colours.line());
        Draw.textScaled(g, font, GameText.resolve(StudioTexts.ON_THE_BENCH), area.x() + 4, area.y() + 4,
                colours.text(), SMALL);
        if (this.tree.chosen().isEmpty()) {
            Draw.textCentered(g, font, GameText.resolve(StudioTexts.NOTHING_CHOSEN), area.x() + area.width() / 2,
                    area.y() + area.height() / 2 - 4, colours.text());
        } else if (!this.bench.standing() && this.tree.chosen().get().icon().isEmpty()) {
            Draw.textCentered(g, font, GameText.resolve(StudioTexts.NOTHING_TO_SHOW), area.x() + area.width() / 2,
                    area.y() + area.height() / 2 - 4, colours.text());
        }
        if (this.bench.standing()) {
            Draw.textScaled(g, font, this.cut(GameText.resolve(StudioTexts.BENCH_HINT), area.width() - 8, SMALL),
                    area.x() + 4, area.bottom() - 9, colours.text(), SMALL);
        }
    }

    private void panelGround(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        g.fill(area.x(), area.y(), area.right(), area.bottom(), colours.panel());
        g.fill(area.x(), area.y(), area.right(), area.y() + 1, colours.line());
    }

    private void treeRows(final GuiGraphics g, final StudioPalette.Colours colours) {
        final StudioLayout.Rect area = this.regions.tree();
        final List<StudioTree.Row> rows = this.tree.rows();
        final int last = Math.min(rows.size(), this.tree.scroll() + this.treeVisible());
        for (int i = this.tree.scroll(); i < last; i++) {
            final StudioTree.Row row = rows.get(i);
            final int y = area.y() + INSET / 2 + (i - this.tree.scroll()) * StudioLayout.TREE_ROW;
            final int indent = switch (row.type()) {
                case MOD -> 0;
                case KIND -> KIND_INDENT;
                case THING -> THING_INDENT;
            };
            final boolean chosen = row.thing() != null && this.tree.chosen().map(any -> any == row.thing())
                    .orElse(false);
            if (chosen) {
                g.fill(area.x() + 2, y, area.right() - 2, y + StudioLayout.TREE_ROW, colours.selected());
                Draw.outline(g, area.x() + 2, y, area.width() - 4, StudioLayout.TREE_ROW, colours.outline());
            }
            int x = area.x() + INSET / 2 + indent;
            if (row.type() != StudioTree.Type.THING) {
                StudioMarks.arrow(g, x, y + 3, row.open(), colours.text());
                x += ARROW_ROOM;
            }
            // A mod's and a kind's count, a thing's problems with their mark; the name has the rest of the row.
            final boolean marked = row.type() == StudioTree.Type.THING && row.problems() > 0;
            final String count = row.type() != StudioTree.Type.THING ? String.valueOf(row.count())
                    : marked ? String.valueOf(row.problems()) : "";
            final int countX = area.right() - INSET - Math.round(font.width(count) * SMALL);
            final int markX = marked ? countX - 3 - StudioMarks.WIDTH : countX;
            final int room = (count.isEmpty() ? area.right() - INSET : markX - MARK_GAP) - x;
            Draw.text(g, font, this.cut(row.label(), room, 1.0F), x, y + 2, colours.text());
            Draw.textScaled(g, font, count, countX, y + 3, colours.dim(), SMALL);
            if (marked) {
                StudioMarks.draw(g, font, markX, y + 1, row.mark());
            }
        }
    }

    private void checkRows(final GuiGraphics g, final StudioPalette.Colours colours, final int mouseX,
                           final int mouseY, final List<Component> tip) {
        final StudioLayout.Rect area = this.regions.across();
        Draw.text(g, font, GameText.resolve(StudioTexts.ACROSS), area.x() + INSET, area.y() + 4, colours.text());
        final List<StudioSheet.Check> checks = this.sheet().map(StudioSheet::checks).orElse(List.of());
        if (this.tree.chosen().isPresent() && checks.isEmpty()) {
            Draw.textScaled(g, font, GameText.resolve(StudioTexts.NOTHING_WRONG), area.x() + INSET,
                    area.y() + StudioLayout.HEAD + 4, colours.dim(), SMALL);
        }
        final int last = Math.min(checks.size(), this.checksScroll + this.checksVisible());
        for (int i = this.checksScroll; i < last; i++) {
            final StudioSheet.Check check = checks.get(i);
            final int y = area.y() + StudioLayout.HEAD + (i - this.checksScroll) * StudioLayout.CHECK_ROW;
            StudioMarks.draw(g, font, area.x() + INSET, y + 4, check.mark());
            final int x = area.x() + INSET + StudioMarks.WIDTH + MARK_GAP;
            final int room = StudioLayout.open(this.regions, y).x() - INSET - x;
            Draw.text(g, font, this.cut(check.title(), room, 1.0F), x, y + 2, colours.text());
            Draw.textScaled(g, font, this.cut(check.detail(), room, SMALL), x, y + 13, colours.dim(), SMALL);
            if (mouseX >= area.x() && mouseX < x + room && mouseY >= y && mouseY < y + StudioLayout.CHECK_ROW) {
                tip.add(Component.literal(check.title()));
                tip.add(Component.literal(check.detail()));
                check.file().ifPresent(file -> tip.add(Component.literal(file.path())));
            }
        }
    }

    private void partRows(final GuiGraphics g, final StudioPalette.Colours colours, final int mouseX,
                          final int mouseY, final List<Component> tip) {
        final StudioLayout.Rect area = this.regions.madeOf();
        Draw.text(g, font, GameText.resolve(StudioTexts.MADE_OF), area.x() + INSET, area.y() + 4, colours.text());
        final List<StudioSheet.Part> parts = this.sheet().map(StudioSheet::parts).orElse(List.of());
        final int last = Math.min(parts.size(), this.partsScroll + this.partsVisible());
        final int room = area.width() - INSET - ICON_ROOM - BADGE_ROOM;
        for (int i = this.partsScroll; i < last; i++) {
            final StudioSheet.Part part = parts.get(i);
            final int y = area.y() + StudioLayout.HEAD + (i - this.partsScroll) * StudioLayout.PART_ROW;
            final boolean pointed = mouseX >= area.x() && mouseX < area.right() && mouseY >= y
                    && mouseY < y + StudioLayout.PART_ROW;
            if (pointed && part.file().isPresent()) {
                g.fill(area.x() + 1, y, area.right() - 1, y + StudioLayout.PART_ROW, colours.line());
            }
            g.renderItem(part.icon(), area.x() + INSET, y + (StudioLayout.PART_ROW - ICON) / 2);
            final int x = area.x() + INSET + ICON_ROOM;
            Draw.text(g, font, this.cut(part.name(), room, 1.0F), x, y + 3, colours.text());
            Draw.textScaled(g, font, this.cut(part.detail(), room, SMALL), x, y + 14, colours.dim(), SMALL);
            StudioMarks.draw(g, font, area.right() - INSET - StudioMarks.WIDTH, y + 8, part.mark());
            if (pointed) {
                tip.add(Component.literal(part.name()));
                tip.add(Component.literal(part.detail()));
                part.file().ifPresent(file -> tip.add(Component.literal(file.path())));
            }
        }
    }

    /* Shows the states' buttons for a machine, the chosen one lit, and an Open for each check shown with a file. */
    private void placeButtons() {
        this.stateButtons.forEach((state, button) -> {
            button.setVisible(this.bench.machine());
            button.setPrimary(this.bench.state() == state);
        });
        final List<StudioSheet.Check> checks = this.sheet().map(StudioSheet::checks).orElse(List.of());
        for (int i = 0; i < this.openButtons.size(); i++) {
            final int index = this.checksScroll + i;
            final boolean shown = i < this.checksVisible() && index < checks.size()
                    && checks.get(index).file().isPresent();
            final Button open = this.openButtons.get(i);
            open.setVisible(shown);
            if (shown) {
                set(open, StudioLayout.open(this.regions, this.regions.across().y() + StudioLayout.HEAD
                        + i * StudioLayout.CHECK_ROW));
            }
        }
        this.runButton.setEnabled(this.sheet().map(StudioSheet::tested).orElse(false)
                && Minecraft.getInstance().getConnection() != null);
    }

    private void openCheck(final int row) {
        final List<StudioSheet.Check> checks = this.sheet().map(StudioSheet::checks).orElse(List.of());
        final int index = this.checksScroll + row;
        if (index < checks.size()) {
            checks.get(index).file().ifPresent(this::open);
        }
    }

    private void open(final StudioSheet.File file) {
        this.status = StudioFiles.open(file) ? GameText.resolve(StudioTexts.OPENED.with(file.path()))
                : GameText.resolve(StudioTexts.NOT_OPENED.with(file.path(), file.modid()));
    }

    /* The resources again, the world's data too when its owner may reload it, and then every thing read again. */
    private void reloadAll() {
        final Minecraft game = Minecraft.getInstance();
        this.status = GameText.resolve(StudioTexts.RELOADING);
        if (game.player != null && game.hasSingleplayerServer() && game.player.hasPermissions(RELOADS)) {
            game.player.connection.sendCommand("reload");
        }
        game.reloadResourcePacks().thenRun(() -> game.execute(this::read));
    }

    private void runTests() {
        this.tree.chosen().ifPresent(thing -> {
            PacketDistributor.sendToServer(new RunThingTestsPayload(thing.id()));
            this.status = GameText.resolve(StudioTexts.TESTS_ASKED.with(thing.id().toString()));
        });
    }

    private List<String> crumbs(final StudioThing thing) {
        final String mod = StudioThings.modName(thing.modid());
        return List.of(mod.isEmpty() ? GameText.resolve(StudioTexts.PACK.with(thing.modid())) : mod,
                GameText.resolve(thing.kind().title()), thing.name().getString());
    }

    /* Cuts {@code text} to {@code width} pixels at {@code scale}, ending it in dots when cut. */
    private String cut(final String text, final int width, final float scale) {
        final int room = Math.max(0, (int) (width / scale));
        if (font.width(text) <= room) {
            return text;
        }
        return font.plainSubstrByWidth(text, Math.max(0, room - font.width(ELLIPSIS))) + ELLIPSIS;
    }

    private int treeVisible() {
        return Math.max(1, (this.regions.tree().height() - INSET) / StudioLayout.TREE_ROW);
    }

    private int partsVisible() {
        return Math.max(1, (this.regions.madeOf().height() - StudioLayout.HEAD) / StudioLayout.PART_ROW);
    }

    private int checksVisible() {
        return Math.max(1, (this.regions.across().height() - StudioLayout.HEAD) / StudioLayout.CHECK_ROW);
    }

    private StyleSkin skin() {
        return Styles.skin(Styles.game());
    }

    private static int scrolled(final int at, final double turns, final int rows, final int visible) {
        return Math.max(0, Math.min(Math.max(0, rows - visible), at - (int) Math.signum(turns)));
    }

    private static void set(final UiComponent component, final StudioLayout.Rect rect) {
        component.setBounds(rect.x(), rect.y(), rect.width(), rect.height());
    }
}
