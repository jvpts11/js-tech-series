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
import dev.jstech.core.developer.RunThingTestsPayload;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * The studio's home: the things of every mod on the Core by mod and by kind, each with its problems; the thing chosen
 * on a bench, working, idle or jammed; what it is made of, each part the ordinary file it always was, opened by a
 * click; and the checks across its parts, each opening the file that mends it.
 */
final class ThingsPage extends StudioPage {

    private final Map<BenchState, Button> stateButtons = new EnumMap<>(BenchState.class);
    private final List<Button> openButtons = new ArrayList<>();
    private final StudioBench bench = new StudioBench();
    private Map<StudioThing, StudioSheet> sheets = Map.of();
    private StudioTree tree = new StudioTree(List.of(), Map.of(), Map.of());
    private int partsScroll;
    private int checksScroll;
    private int problems;
    private boolean turning;

    private static final Logger LOGGER = CoreLogs.DEVELOPER.logger();
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final float SMALL = StudioDraw.SMALL;
    private static final int INSET = StudioDraw.INSET;
    /** How far a kind and a thing stand in from their mod in the tree, and the room an arrow takes. */
    private static final int KIND_INDENT = 6;
    private static final int THING_INDENT = 15;
    private static final int ARROW_ROOM = 9;
    private static final int ICON = 16;
    private static final int ICON_ROOM = 22;
    /** The room a part's mark takes at its row's right, and between a check's mark and its words. */
    private static final int BADGE_ROOM = 22;
    private static final int MARK_GAP = 5;
    /** How many Open buttons the checks hold at most at once. */
    private static final int OPENS = (StudioLayout.ACROSS_MAX - StudioLayout.HEAD) / StudioLayout.CHECK_ROW + 1;

    ThingsPage(final StudioScreen studio) {
        super(studio);
        for (final BenchState state : BenchState.values()) {
            this.stateButtons.put(state, this.panel.add(new Button(() -> GameText.resolve(state.title()),
                    () -> this.bench.state(state))));
        }
        for (int i = 0; i < OPENS; i++) {
            final int row = i;
            this.openButtons.add(this.panel.add(new Button(() -> GameText.resolve(StudioTexts.OPEN),
                    () -> this.openCheck(row))));
        }
    }

    @Override
    TextKey tab() {
        return StudioTool.THINGS.title();
    }

    @Override
    List<String> crumbs() {
        return this.tree.chosen().map(this::crumbsOf).orElse(List.of());
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        // The states' buttons stand in their order, working first.
        int place = 0;
        for (final Button state : this.stateButtons.values()) {
            place(state, laid.states().get(place++));
        }
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        this.drawBench(g, colours);
        StudioDraw.panel(g, font(), this.regions.tree(), "");
        StudioDraw.panel(g, font(), this.regions.across(), words(StudioTexts.ACROSS));
        StudioDraw.panel(g, font(), this.regions.madeOf(), words(StudioTexts.MADE_OF));
        this.treeRows(g, colours);
        this.checkRows(g, colours, mouseX, mouseY, tip);
        this.partRows(g, colours, mouseX, mouseY, tip);
        this.placeButtons();
    }

    /** Draws the outline of the state a machine on the bench is in, over its button. */
    void renderOver(final GuiGraphics g) {
        if (this.bench.machine()) {
            final Button lit = this.stateButtons.get(this.bench.state());
            Draw.outline(g, lit.x() - 1, lit.y() - 1, lit.width() + 2, lit.height() + 2, StudioPalette.get()
                    .outline());
        }
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
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
                    .flatMap(all -> all.get(row).file()).ifPresent(this.studio::open);
            return true;
        }
        if (this.regions.bench().contains(mouseX, mouseY) && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.turning = true;
            return true;
        }
        return false;
    }

    @Override
    boolean mouseDragged(final double mouseX, final double mouseY, final double dragX, final double dragY) {
        if (this.turning) {
            this.bench.turn(dragX, dragY);
            return true;
        }
        return false;
    }

    @Override
    void mouseReleased() {
        this.turning = false;
    }

    @Override
    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
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
            return false;
        }
        return true;
    }

    @Override
    void closed() {
        this.bench.close();
    }

    /** Reads every thing, and what each is made of, keeping the thing chosen and the words searched. */
    void read(final String searched) {
        final Optional<ResourceLocation> was = this.tree.chosen().map(StudioThing::id);
        final long started = System.nanoTime();
        final StudioContext context = new StudioContext(Minecraft.getInstance().getResourceManager());
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
        this.tree.search(searched);
        this.bench.show(null);
        was.ifPresent(this::choose);
    }

    /** Shows only the things whose name or id holds {@code word}. */
    void search(final String word) {
        this.tree.search(word);
    }

    /** Chooses the next thing that has a problem. */
    void nextWithProblems() {
        this.tree.nextWithProblems().ifPresent(this::choose);
    }

    /** Chooses the thing of {@code id}; whether there is one. */
    boolean choose(final ResourceLocation id) {
        final Optional<StudioThing> thing = this.sheets.keySet().stream().filter(any -> any.id().equals(id))
                .findFirst();
        thing.ifPresent(this::choose);
        return thing.isPresent();
    }

    /** The thing chosen. */
    Optional<StudioThing> chosen() {
        return this.tree.chosen();
    }

    /** What the thing chosen is made of. */
    Optional<StudioSheet> sheet() {
        return this.tree.chosen().map(this.sheets::get);
    }

    /** How many problems every thing has. */
    int problems() {
        return this.problems;
    }

    /** Has a machine on the bench be in {@code state}. */
    void showState(final BenchState state) {
        this.bench.state(state);
    }

    /** Whether a GameTest names the thing chosen. */
    boolean tested() {
        return this.sheet().map(StudioSheet::tested).orElse(false);
    }

    /** Asks the server to run, near the player, the GameTests that name the thing chosen. */
    void runTests() {
        this.tree.chosen().ifPresent(thing -> {
            PacketDistributor.sendToServer(new RunThingTestsPayload(thing.id()));
            this.studio.say(GameText.resolve(StudioTexts.TESTS_ASKED.with(thing.id().toString())));
        });
    }

    private void choose(final StudioThing thing) {
        this.tree.choose(thing, this.treeVisible());
        this.bench.show(thing);
        this.partsScroll = 0;
        this.checksScroll = 0;
    }

    private List<String> crumbsOf(final StudioThing thing) {
        final String mod = StudioThings.modName(thing.modid());
        return List.of(mod.isEmpty() ? GameText.resolve(StudioTexts.PACK.with(thing.modid())) : mod,
                GameText.resolve(thing.kind().title()), thing.name().getString());
    }

    private void drawBench(final GuiGraphics g, final StudioPalette.Colours colours) {
        final StudioLayout.Rect area = this.regions.bench();
        g.fillGradient(area.x(), area.y(), area.right(), area.bottom(), colours.skyTop(), colours.skyLow());
        this.bench.draw(g, area);
        Draw.outline(g, area.x(), area.y(), area.width(), area.height(), colours.frame());
        Draw.textScaled(g, font(), words(StudioTexts.ON_THE_BENCH), this.regions.label().x(),
                this.regions.label().y(), colours.text(), SMALL);
        if (this.tree.chosen().isEmpty()) {
            Draw.textCentered(g, font(), words(StudioTexts.NOTHING_CHOSEN), area.x() + area.width() / 2,
                    area.y() + area.height() / 2 - 4, colours.text());
        } else if (!this.bench.standing() && this.tree.chosen().get().icon().isEmpty()) {
            Draw.textCentered(g, font(), words(StudioTexts.NOTHING_TO_SHOW), area.x() + area.width() / 2,
                    area.y() + area.height() / 2 - 4, colours.text());
        }
        if (this.bench.standing()) {
            StudioDraw.small(g, font(), words(StudioTexts.BENCH_HINT), area.x() + 4, area.bottom() - 9,
                    area.width() - 8, colours.text());
        }
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
            // A mod's name and the thing chosen stand out in white; a kind and the other things in the label's grey.
            final int words = row.type() == StudioTree.Type.MOD || chosen ? colours.text() : colours.label();
            int x = area.x() + INSET / 2 + indent;
            if (row.type() != StudioTree.Type.THING) {
                StudioMarks.arrow(g, x, y + 3, row.open(), words);
                x += ARROW_ROOM;
            }
            // A mod's and a kind's count, a thing's problems with their mark; the name has the rest of the row.
            final boolean marked = row.type() == StudioTree.Type.THING && row.problems() > 0;
            final String count = row.type() != StudioTree.Type.THING ? String.valueOf(row.count())
                    : marked ? String.valueOf(row.problems()) : "";
            final int countX = area.right() - INSET - Math.round(font().width(count) * SMALL);
            final int markX = marked ? countX - 3 - StudioMarks.WIDTH : countX;
            final int room = (count.isEmpty() ? area.right() - INSET : markX - MARK_GAP) - x;
            StudioDraw.text(g, font(), row.label(), x, y + 2, room, words);
            Draw.textScaled(g, font(), count, countX, y + 3, colours.dim(), SMALL);
            if (marked) {
                StudioMarks.draw(g, font(), markX, y + 1, row.mark());
            }
        }
    }

    private void checkRows(final GuiGraphics g, final StudioPalette.Colours colours, final int mouseX,
                           final int mouseY, final List<Component> tip) {
        final StudioLayout.Rect area = this.regions.across();
        final List<StudioSheet.Check> checks = this.sheet().map(StudioSheet::checks).orElse(List.of());
        if (this.tree.chosen().isPresent() && checks.isEmpty()) {
            StudioDraw.small(g, font(), words(StudioTexts.NOTHING_WRONG), area.x() + INSET,
                    area.y() + StudioLayout.HEAD + 4, area.width() - 2 * INSET, colours.dim());
        }
        final int last = Math.min(checks.size(), this.checksScroll + this.checksVisible());
        for (int i = this.checksScroll; i < last; i++) {
            final StudioSheet.Check check = checks.get(i);
            final int y = area.y() + StudioLayout.HEAD + (i - this.checksScroll) * StudioLayout.CHECK_ROW;
            StudioMarks.draw(g, font(), area.x() + INSET, y + 4, check.mark());
            final int x = area.x() + INSET + StudioMarks.WIDTH + MARK_GAP;
            final int room = StudioLayout.open(this.regions, y).x() - INSET - x;
            StudioDraw.text(g, font(), check.title(), x, y + 2, room, colours.text());
            StudioDraw.small(g, font(), check.detail(), x, y + 13, room, colours.dim());
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
        final List<StudioSheet.Part> parts = this.sheet().map(StudioSheet::parts).orElse(List.of());
        final int last = Math.min(parts.size(), this.partsScroll + this.partsVisible());
        final int room = area.width() - INSET - ICON_ROOM - BADGE_ROOM;
        for (int i = this.partsScroll; i < last; i++) {
            final StudioSheet.Part part = parts.get(i);
            final int y = area.y() + StudioLayout.HEAD + (i - this.partsScroll) * StudioLayout.PART_ROW;
            final boolean pointed = mouseX >= area.x() && mouseX < area.right() && mouseY >= y
                    && mouseY < y + StudioLayout.PART_ROW;
            if (pointed && part.file().isPresent()) {
                g.fill(area.x() + 1, y, area.right() - 1, y + StudioLayout.PART_ROW, colours.pointed());
            }
            g.renderItem(part.icon(), area.x() + INSET, y + (StudioLayout.PART_ROW - ICON) / 2);
            final int x = area.x() + INSET + ICON_ROOM;
            StudioDraw.text(g, font(), part.name(), x, y + 3, room, colours.text());
            StudioDraw.small(g, font(), part.detail(), x, y + 14, room, colours.dim());
            StudioMarks.draw(g, font(), area.right() - INSET - StudioMarks.WIDTH, y + 8, part.mark());
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
                place(open, StudioLayout.open(this.regions, this.regions.across().y() + StudioLayout.HEAD
                        + i * StudioLayout.CHECK_ROW));
            }
        }
    }

    private void openCheck(final int row) {
        final List<StudioSheet.Check> checks = this.sheet().map(StudioSheet::checks).orElse(List.of());
        final int index = this.checksScroll + row;
        if (index < checks.size()) {
            checks.get(index).file().ifPresent(this.studio::open);
        }
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

    /** Scrolls a list of {@code rows} by turns of the wheel, a row a turn, held to what {@code visible} show. */
    static int scrolled(final int at, final double turns, final int rows, final int visible) {
        return Math.max(0, Math.min(Math.max(0, rows - visible), at - (int) Math.signum(turns)));
    }
}
