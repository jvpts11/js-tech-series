/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.operation.payload.CraftCatalogPayload;
import dev.jstech.computers.operation.payload.CraftPlanPayload;
import dev.jstech.computers.operation.payload.CraftPlannerPayload;
import dev.jstech.computers.operation.payload.NiCraftPayload;
import dev.jstech.computers.operation.payload.RequestCraftPlannerPayload;
import dev.jstech.computers.printer.PrintLayout;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Label;
import dev.jstech.core.client.gui.component.ListView;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.SearchField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.time.GameCalendar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dev.jstech.computers.client.os.CraftPlannerTexts.BENCH_RUNS;
import static dev.jstech.computers.client.os.CraftPlannerTexts.CRAFT;
import static dev.jstech.computers.client.os.CraftPlannerTexts.CRAFTABLE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.CRAFT_TREE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.HAVE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.INGREDIENTS;
import static dev.jstech.computers.client.os.CraftPlannerTexts.LOADING;
import static dev.jstech.computers.client.os.CraftPlannerTexts.MACHINE_RUNS;
import static dev.jstech.computers.client.os.CraftPlannerTexts.MANY_SHORT;
import static dev.jstech.computers.client.os.CraftPlannerTexts.MISSING;
import static dev.jstech.computers.client.os.CraftPlannerTexts.NEED_HAVE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.NOT_CRAFTABLE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.NO_MATCH;
import static dev.jstech.computers.client.os.CraftPlannerTexts.ONE_SHORT;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PARTIAL;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PICK_AN_ITEM;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PLANNING;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PRINTED_AT;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PRINTED_FROM;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PRINT_HAVE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PRINT_MISSING;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PRINT_NEED;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PRINT_STAGE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PRINT_TARGET;
import static dev.jstech.computers.client.os.CraftPlannerTexts.PRINT_TITLE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.RAW;
import static dev.jstech.computers.client.os.CraftPlannerTexts.SEARCH_ITEM;
import static dev.jstech.computers.client.os.CraftPlannerTexts.STAGES;
import static dev.jstech.computers.client.os.CraftPlannerTexts.STEPS;
import static dev.jstech.computers.client.os.CraftPlannerTexts.SUMMARY;
import static dev.jstech.computers.client.os.CraftPlannerTexts.TREE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.TREE_NODE;
import static dev.jstech.computers.client.os.CraftPlannerTexts.WHEEL_TO_SCROLL;

/**
 * Craft Planner: pick a target from the network's craft catalogue and see its full plan before crafting.
 * The plan reports whether the item is craftable, whether the request is feasible from current stock, the
 * largest feasible amount, the ordered stages, and the raw-ingredient bill (need vs have, short in red).
 * The catalogue and the plan are computed server-side; the Craft button submits the same request the
 * terminal and Network Interactor use.
 */
@PaletteHolder
public final class CraftPlannerApp implements IDesktopApp {

    private static final int REFRESH_FRAMES = 60;
    /* The program's id, which names it as the desktop does on what it prints. */
    private static final String PROGRAM = "jsc:craft_planner";
    /**
     * The planner's own colours, {@code jsc:app/craft_planner}: what can be made, what is missing, what wants the
     * eye, and the pill behind a plan that can run and one that cannot.
     */
    private static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "app/craft_planner",
            new Colours(0xFF2EA043, 0xFFD1495B, 0xFFE0A020, 0x2E2EA043, 0x33E0A020, 0x40D1495B));
    private static final int CATALOG_ROW_H = 15;
    private static final int STAGE_ROW_H = 10;
    private static final int INGREDIENT_ROW_H = 11;
    private static final int TREE_ROW_H = 11;
    private static final int SEARCH_MAX = 32;

    private final BlockPos host;
    private final BlockPos monitorPos;
    private OsSkin skin = OsSkin.fallback();
    private List<CraftCatalogPayload.Entry> catalog = List.of();
    @Nullable
    private CraftPlannerPayload plan;
    private ItemStack selected = ItemStack.EMPTY;
    private long qty = 1;
    private boolean treeMode;
    private int frame;
    private int lastMouseX;
    private int lastMouseY;

    private static CraftPlannerApp active;

    // components
    private final Panel root = new Panel();
    private final SearchField search;
    private final ListView<CraftCatalogPayload.Entry> catalogList;
    private final Label catalogEmpty;
    private final Label pickLabel;
    private final Label nameLabel;
    private final Button qtyMinus;
    private final Label qtyLabel;
    private final Button qtyPlus;
    private final Label planningLabel;
    private final Label notCraftableLabel;
    private final Label pillLabel;
    private final Label summaryLabel;
    private final Button viewToggle;
    private final Label stagesHeader;
    private final ListView<CraftPlannerPayload.Stage> stageList;
    private final Label ingredientsHeader;
    private final ListView<CraftPlanPayload.Row> ingredientList;
    private final Label treeHeader;
    private final Label treeHint;
    private final ListView<CraftPlannerPayload.TreeNode> treeList;
    private final Button craft;
    private final Button print;
    private final PrintDialog printDialog;

    public CraftPlannerApp(final BlockPos host, final BlockPos monitorPos) {
        this.host = host;
        this.monitorPos = monitorPos;

        search = root.add(new SearchField(SEARCH_MAX));
        search.setPlaceholder(GameText.resolve(SEARCH_ITEM));
        catalogList = root.add(new ListView<CraftCatalogPayload.Entry>(this::filtered, CATALOG_ROW_H, this::renderCatalogRow)
                .setOnClick(this::catalogClicked));
        search.setOnEdit(() -> catalogList.setScroll(0));
        catalogEmpty = root.add(new Label(() -> GameText.resolve(catalog.isEmpty() ? LOADING : NO_MATCH),
                Label.Tone.DIM));

        pickLabel = root.add(new Label(GameText.resolve(PICK_AN_ITEM), Label.Tone.DIM));
        nameLabel = root.add(new Label(() -> selected.getHoverName().getString()));
        qtyMinus = root.add(new Button("-", () -> setQty(qty - step())));
        qtyLabel = root.add(new Label(() -> String.valueOf(qty)).setAlign(Label.Align.CENTER));
        qtyPlus = root.add(new Button("+", () -> setQty(qty + step())));
        planningLabel = root.add(new Label(GameText.resolve(PLANNING), Label.Tone.DIM));
        notCraftableLabel = root.add(new Label(GameText.resolve(NOT_CRAFTABLE)).setColor(PALETTE.get().missing()));
        pillLabel = root.add(new Label(this::pillText).setColor(this::pillInk));
        summaryLabel = root.add(new Label(this::summaryText, Label.Tone.DIM));
        viewToggle = root.add(new Button(() -> GameText.resolve(treeMode ? STEPS : TREE), () -> treeMode = !treeMode));
        stagesHeader = root.add(new Label(GameText.resolve(STAGES), Label.Tone.DIM));
        stageList = root.add(new ListView<CraftPlannerPayload.Stage>(() -> plan == null ? List.of() : plan.stages(), STAGE_ROW_H,
                this::renderStageRow));
        ingredientsHeader = root.add(new Label(GameText.resolve(INGREDIENTS), Label.Tone.DIM));
        ingredientList = root.add(new ListView<CraftPlanPayload.Row>(() -> plan == null ? List.of() : plan.ingredients(),
                INGREDIENT_ROW_H, this::renderIngredientRow));
        treeHeader = root.add(new Label(GameText.resolve(CRAFT_TREE), Label.Tone.DIM));
        treeHint = root.add(new Label(GameText.resolve(WHEEL_TO_SCROLL), Label.Tone.DIM).setAlign(Label.Align.RIGHT));
        treeList = root.add(new ListView<CraftPlannerPayload.TreeNode>(() -> plan == null ? List.of() : plan.tree(), TREE_ROW_H,
                this::renderTreeRow));
        craft = root.add(new Button(() -> GameText.resolve(CRAFT.with(qty)), this::craft));
        print = root.add(new Button(GameText.resolve(PrintTexts.PRINT), this::print));
        printDialog = new PrintDialog(host, this);

        root.focus(search);
        active = this;
        PacketDistributor.sendToServer(new RequestCraftPlannerPayload(host, monitorPos, ItemStack.EMPTY, 0));
    }

    public static void acceptCatalog(final List<CraftCatalogPayload.Entry> entries) {
        if (active != null) {
            active.catalog = entries;
        }
    }

    public static void accept(final CraftPlannerPayload payload) {
        if (active != null) {
            active.plan = payload;
        }
    }

    /** What the catalogue lists as it stands, by the names the rows show (client tests pick a target by it). */
    public List<String> catalogNames() {
        final List<String> out = new ArrayList<>();
        for (final CraftCatalogPayload.Entry entry : catalogList.items()) {
            out.add(entry.result().getHoverName().getString());
        }
        return out;
    }

    /** The desktop point of the catalogue's row of that name as the last frame laid it out, or null. */
    @Nullable
    public int[] catalogRowPoint(final String name) {
        final int index = catalogNames().indexOf(name);
        return index < 0 ? null : catalogList.rowCenter(index);
    }

    /** Whether the plan of the picked target has come back and the network can make all of it now. */
    public boolean planFeasible() {
        return plan != null && plan.craftable() && plan.feasible();
    }

    /** How many of the plan's ingredients the network holds less of than the plan needs. */
    public int shortIngredients() {
        return shortRows().size();
    }

    /** How many stages the plan of the picked target runs, or 0 while there is none. */
    public int planStages() {
        return plan == null ? 0 : plan.stages().size();
    }

    /** The desktop point of the Craft button as the last frame laid it out. */
    public int[] craftPoint() {
        return new int[] {craft.x() + craft.width() / 2, craft.y() + craft.height() / 2};
    }

    /** The desktop point of the quantity's + button as the last frame laid it out. */
    public int[] plusPoint() {
        return new int[] {qtyPlus.x() + qtyPlus.width() / 2, qtyPlus.y() + qtyPlus.height() / 2};
    }

    /** The desktop point of the Print button as the last frame laid it out. */
    public int[] printPoint() {
        return new int[] {print.x() + print.width() / 2, print.y() + print.height() / 2};
    }

    /** The Print window this planner opens over itself. */
    public PrintDialog printDialog() {
        return printDialog;
    }

    private void requestPlan() {
        if (!selected.isEmpty()) {
            PacketDistributor.sendToServer(new RequestCraftPlannerPayload(host, monitorPos, selected, qty));
        }
    }

    @Override
    public void onRestored() {
        active = this;
        // The catalog may have grown (recipes loaded meanwhile) and the plan's stock may have moved.
        PacketDistributor.sendToServer(new RequestCraftPlannerPayload(host, monitorPos, ItemStack.EMPTY, 0));
        requestPlan();
    }

    @Override
    public String title() {
        return GameText.resolve(CraftPlannerAppTexts.TITLE);
    }

    @Override
    public int defaultWidth() {
        return 320;
    }

    @Override
    public int defaultHeight() {
        return 200;
    }

    @Override
    public int minWidth() {
        return 280;
    }

    @Override
    public int minHeight() {
        return 170;
    }

    @Override
    public void applySkin(final OsSkin osSkin) {
        this.skin = osSkin;
        active = this;
    }

    // rendering

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y,
                              final int width, final int height, final int mouseX, final int mouseY,
                              final float partialTick) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        frame++;
        if (frame % REFRESH_FRAMES == 0) {
            requestPlan();
        }
        final UiContext ctx = new UiContext(skin, font, mouseX, mouseY, partialTick);
        g.fill(x, y, x + width, y + height, skin.windowBg());
        final int px = x + 6;
        final int py = y + 6;
        final int ph = height - 12;
        final int leftW = Math.max(96, (int) (width * 0.38));
        layout(font, px, py, width - 12, ph, leftW);
        g.fill(px + leftW - 3, py, px + leftW - 2, py + ph, skin.edge());
        if (!selected.isEmpty()) {
            itemIcon(g, selected, px + leftW, py, 16);
            if (pillLabel.visible()) {
                final int pillW = font.width(pillLabel.text()) + 8;
                g.fill(pillLabel.x() - 4, pillLabel.y() - 2, pillLabel.x() - 4 + pillW, pillLabel.y() + 9,
                        plan != null && plan.feasible() ? PALETTE.get().feasiblePill()
                                : shortRows().isEmpty() ? PALETTE.get().infeasiblePill()
                                : PALETTE.get().missingPill());
                g.fill(px + leftW, pillLabel.y() + 12, px + leftW + width - 12 - leftW, pillLabel.y() + 13, skin.edge());
            }
        }
        root.render(g, ctx);
    }

    private void layout(final Font font, final int x, final int y, final int w, final int h, final int leftW) {
        final int catW = leftW - 6;
        search.setBounds(x, y, catW, 13);
        catalogList.setBounds(x, y + 16, catW, Math.max(CATALOG_ROW_H, h - 16));
        final boolean noCatalog = filtered().isEmpty();
        catalogList.setVisible(!noCatalog);
        catalogEmpty.setVisible(noCatalog);
        catalogEmpty.setBounds(x + 2, y + 18, catW - 4, 8);

        final int rx = x + leftW;
        final int rw = w - leftW;
        final boolean picked = !selected.isEmpty();
        pickLabel.setVisible(!picked);
        pickLabel.setBounds(rx + 2, y + 4, rw - 4, 8);
        for (final var c : List.of(nameLabel, qtyMinus, qtyLabel, qtyPlus)) {
            c.setVisible(picked);
        }
        final boolean planned = picked && plan != null;
        planningLabel.setVisible(picked && plan == null);
        final boolean craftable = planned && plan.craftable();
        notCraftableLabel.setVisible(planned && !plan.craftable());
        for (final var c : List.of(pillLabel, summaryLabel, viewToggle, craft, print)) {
            c.setVisible(craftable);
        }
        /*
         * Nothing to craft with while an ingredient is short: the button greys until the network has it all, and the
         * list on paper, what the plan still offers, takes the window's default button.
         */
        final boolean ready = craftable && shortRows().isEmpty();
        craft.setEnabled(ready);
        craft.setPrimary(ready);
        print.setPrimary(!ready);
        final boolean steps = craftable && !treeMode;
        final boolean tree = craftable && treeMode;
        for (final var c : List.of(stagesHeader, stageList, ingredientsHeader, ingredientList)) {
            c.setVisible(steps);
        }
        treeHeader.setVisible(tree);
        treeList.setVisible(tree);
        treeHint.setVisible(tree && plan.tree().size() > treeList.visibleRows());
        if (!picked) {
            return;
        }
        // Header: icon + name + quantity stepper.
        final int stepX = rx + rw - 56;
        nameLabel.setBounds(rx + 20, y + 4, stepX - (rx + 20) - 4, 8);
        qtyMinus.setBounds(stepX, y, 14, 12);
        qtyLabel.setBounds(stepX + 16, y + 3, 24, 8);
        qtyPlus.setBounds(stepX + 44, y, 14, 12);
        int row = y + 20;
        planningLabel.setBounds(rx + 2, row, rw - 4, 8);
        notCraftableLabel.setBounds(rx + 2, row, rw - 4, 8);
        if (!craftable) {
            return;
        }
        // Status line: feasible pill + max + stage count, and the Steps/Tree toggle on the right.
        final int pillW = font.width(pillLabel.text()) + 8;
        pillLabel.setBounds(rx + 4, row + 2, pillW - 8, 8);
        final int togW = font.width(viewToggle.label()) + 10;
        viewToggle.setBounds(rx + rw - togW, row - 1, togW, 11);
        summaryLabel.setBounds(rx + pillW + 6, row + 2, rw - pillW - 6 - togW - 4, 8);
        row += 17;
        final int bottom = y + h - 16;
        final String cap = craft.label();
        final int cw = font.width(cap) + 14;
        craft.setBounds(rx + rw - cw, y + h - 13, cw, 13);
        final int printW = font.width(print.label()) + 14;
        print.setBounds(rx + rw - cw - 4 - printW, y + h - 13, printW, 13);
        if (tree) {
            treeHeader.setBounds(rx + 2, row, rw / 2, 8);
            treeHint.setBounds(rx + rw / 2, row, rw / 2, 8);
            treeList.setBounds(rx, row + 11, rw, Math.max(TREE_ROW_H, bottom - (row + 11)));
            return;
        }
        // Stages then ingredients, sharing the remaining height.
        final int half = (bottom - row) / 2;
        stagesHeader.setBounds(rx + 2, row, rw - 4, 8);
        stageList.setBounds(rx, row + 11, rw, Math.max(STAGE_ROW_H, half - 11));
        final int iy = row + half + 2;
        ingredientsHeader.setBounds(rx + 2, iy, rw - 4, 8);
        ingredientList.setBounds(rx, iy + 11, rw, Math.max(INGREDIENT_ROW_H, bottom - (iy + 11)));
    }

    private void renderCatalogRow(final GuiGraphics g, final UiContext ctx, final CraftCatalogPayload.Entry e, final int index,
                                  final int x, final int y, final int w, final int h, final boolean hovered, final boolean selectedRow) {
        final boolean sel = ItemStack.isSameItemSameComponents(e.result(), selected);
        ctx.skin().listRow(g, x, y, w, h, hovered, sel);
        itemIcon(g, e.result(), x + 1, y, 12);
        Draw.text(g, ctx.font(), Texts.clip(ctx.font(), e.title(), w - 18), x + 15, y + 3,
                sel ? ctx.skin().accent() : ctx.skin().text());
    }

    private void catalogClicked(final int index, final int button, final double mx, final double my) {
        final List<CraftCatalogPayload.Entry> shown = filtered();
        if (button == 0 && index >= 0 && index < shown.size()) {
            select(shown.get(index).result().copy());
        }
    }

    private void renderStageRow(final GuiGraphics g, final UiContext ctx, final CraftPlannerPayload.Stage s, final int index,
                                final int x, final int y, final int w, final int h, final boolean hovered, final boolean selectedRow) {
        final Font font = ctx.font();
        Draw.text(g, font, Texts.clip(font, GameText.resolve(s.name()), w - 60), x + 4, y, ctx.skin().text());
        final String tag = GameText.resolve((s.machine() ? MACHINE_RUNS : BENCH_RUNS).with(s.runs()));
        Draw.text(g, font, tag, x + w - font.width(tag), y, ctx.skin().dim());
    }

    private void renderIngredientRow(final GuiGraphics g, final UiContext ctx, final CraftPlanPayload.Row r, final int index,
                                     final int x, final int y, final int w, final int h, final boolean hovered, final boolean selectedRow) {
        final Font font = ctx.font();
        final boolean ok = r.have() >= r.need();
        final String s = GameText.resolve(ok ? HAVE.with(JsTechTheme.fmt(r.have()))
                : NEED_HAVE.with(JsTechTheme.fmt(r.need()), JsTechTheme.fmt(r.have())));
        itemIcon(g, r.item(), x + 1, y - 1, 11);
        Draw.text(g, font, Texts.clip(font, r.item().getHoverName().getString(), w - 21 - font.width(s)), x + 15, y,
                ctx.skin().text());
        Draw.text(g, font, s, x + w - font.width(s), y, ok ? PALETTE.get().good() : PALETTE.get().missing());
    }

    /** One node of the recipe dependency tree, flattened in pre-order and drawn indented by depth. */
    private void renderTreeRow(final GuiGraphics g, final UiContext ctx, final CraftPlannerPayload.TreeNode n, final int index,
                               final int x, final int y, final int w, final int h, final boolean hovered, final boolean selectedRow) {
        final Font font = ctx.font();
        final int ix = x + 2 + n.depth() * 9;
        if (n.depth() > 0) {
            g.fill(x + 2 + (n.depth() - 1) * 9 + 3, y + 4, ix - 1, y + 5, ctx.skin().edge());
        }
        itemIcon(g, n.item(), ix, y - 1, 10);
        final String label =
                GameText.resolve(TREE_NODE.with(JsTechTheme.fmt(n.qty()), n.item().getHoverName().getString()));
        Draw.text(g, font, Texts.clip(font, label, w - (ix - x) - 13 - 40), ix + 12, y, ctx.skin().text());
        if (!n.craftable()) {
            final String raw = GameText.resolve(RAW);
            Draw.text(g, font, raw, x + w - font.width(raw), y, ctx.skin().dim());
        }
    }

    // state

    private void select(final ItemStack stack) {
        this.selected = stack;
        this.qty = 1;
        this.plan = null;
        treeList.setScroll(0);
        requestPlan();
    }

    private void setQty(final long q) {
        this.qty = Math.max(1, Math.min(100_000, q));
        this.plan = null;
        requestPlan();
    }

    private long step() {
        return qty < 16 ? 1 : qty < 64 ? 8 : qty < 512 ? 64 : 256;
    }

    private void craft() {
        if (!selected.isEmpty() && shortRows().isEmpty()) {
            PacketDistributor.sendToServer(new NiCraftPayload(host, monitorPos, selected, qty));
        }
    }

    /* The ingredients the network has less of than the plan needs, in the plan's order. */
    private List<CraftPlanPayload.Row> shortRows() {
        final List<CraftPlanPayload.Row> out = new ArrayList<>();
        if (plan != null) {
            for (final CraftPlanPayload.Row row : plan.ingredients()) {
                if (row.have() < row.need()) {
                    out.add(row);
                }
            }
        }
        return out;
    }

    /* The pill: the plan can run, it misses ingredients and how many, or it falls short for another reason. */
    private String pillText() {
        if (plan != null && plan.feasible()) {
            return GameText.resolve(CRAFTABLE);
        }
        final int missing = shortRows().size();
        return GameText.resolve(missing > 0 ? MISSING.with(missing) : PARTIAL.text());
    }

    private int pillInk() {
        return plan != null && plan.feasible() ? PALETTE.get().good()
                : shortRows().isEmpty() ? PALETTE.get().amber() : PALETTE.get().missing();
    }

    /* Beside the pill: what is short, by name, or how much the network can make and in how many stages. */
    private String summaryText() {
        if (plan == null) {
            return "";
        }
        final List<CraftPlanPayload.Row> missing = shortRows();
        if (missing.isEmpty()) {
            return GameText.resolve(SUMMARY.with(JsTechTheme.fmt(plan.maxFeasible()), plan.stages().size()));
        }
        final String first = missing.getFirst().item().getHoverName().getString();
        return GameText.resolve(missing.size() == 1 ? ONE_SHORT.with(first)
                : MANY_SHORT.with(first, missing.size() - 1));
    }

    /*
     * The plan on paper, the shopping list: the target, the stages, the ingredients with what the network holds, and
     * what it still lacks, each missing line on a grey band, then where and when it was printed. The three tables share
     * their first column, so the runs, the needs and the missing counts start at one place down the page, and every
     * figure stands right-aligned under its head, a missing count under the needs.
     */
    private void print() {
        if (plan == null || selected.isEmpty()) {
            return;
        }
        final List<CraftPlanPayload.Row> missing = shortRows();
        final String needWord = GameText.resolve(PRINT_NEED);
        final String haveWord = GameText.resolve(PRINT_HAVE);
        int needW = needWord.length();
        int haveW = haveWord.length();
        for (final CraftPlanPayload.Row row : plan.ingredients()) {
            needW = Math.max(needW, JsTechTheme.fmt(row.need()).length());
            haveW = Math.max(haveW, JsTechTheme.fmt(row.have()).length());
        }
        for (final CraftPlanPayload.Row row : missing) {
            needW = Math.max(needW, JsTechTheme.fmt(row.need() - row.have()).length());
        }
        final List<List<String>> stages = new ArrayList<>();
        for (int i = 0; i < plan.stages().size(); i++) {
            final CraftPlannerPayload.Stage stage = plan.stages().get(i);
            stages.add(List.of(GameText.resolve(PRINT_STAGE.with(i + 1, GameText.resolve(stage.name()))),
                    GameText.resolve((stage.machine() ? MACHINE_RUNS : BENCH_RUNS).with(stage.runs()))));
        }
        final List<List<String>> ingredients = new ArrayList<>();
        final List<Integer> shaded = new ArrayList<>();
        ingredients.add(List.of(GameText.resolve(INGREDIENTS), figure(needWord, needW), figure(haveWord, haveW)));
        for (final CraftPlanPayload.Row row : plan.ingredients()) {
            if (row.have() < row.need()) {
                shaded.add(ingredients.size());
            }
            ingredients.add(List.of(row.item().getHoverName().getString(), figure(JsTechTheme.fmt(row.need()), needW),
                    figure(JsTechTheme.fmt(row.have()), haveW)));
        }
        final List<List<String>> lacking = new ArrayList<>();
        final List<Integer> every = new ArrayList<>();
        if (!missing.isEmpty()) {
            lacking.add(List.of(GameText.resolve(PRINT_MISSING), ""));
            every.add(0);
            for (final CraftPlanPayload.Row row : missing) {
                every.add(lacking.size());
                lacking.add(List.of(row.item().getHoverName().getString(),
                        figure(JsTechTheme.fmt(row.need() - row.have()), needW)));
            }
        }
        final int first = firstColumn(stages, ingredients, lacking);
        final List<String> lines = new ArrayList<>();
        lines.add(GameText.resolve(PRINT_TARGET.with(JsTechTheme.fmt(qty), selected.getHoverName().getString())));
        lines.add("");
        lines.add(GameText.resolve(STAGES));
        lines.addAll(tableLines(stages, List.of(), first));
        lines.add("");
        lines.addAll(tableLines(ingredients, shaded, first));
        if (!lacking.isEmpty()) {
            lines.add("");
            lines.addAll(tableLines(lacking, every, first));
        }
        lines.add("");
        final String program = ActiveDesktop.windowName(PROGRAM);
        lines.add(GameText.resolve(PRINTED_FROM.with(program, ActiveDesktop.machineName())));
        final Minecraft mc = Minecraft.getInstance();
        final long dayTime = mc.level == null ? 0L : mc.level.getDayTime();
        // The first day is Day 1, as the desktop's clock counts them.
        lines.add(GameText.resolve(PRINTED_AT.with(GameCalendar.day(dayTime) + 1, String.format(Locale.ROOT,
                "%02d:%02d", GameCalendar.hourOf(dayTime), GameCalendar.minuteOf(dayTime)))));
        printDialog.show(PrintDialog.Document.text(GameText.resolve(PRINT_TITLE), program, String.join("\n", lines)));
    }

    /* A table's lines, its first column {@code first} wide, those of the rows at {@code shaded} on a grey band. */
    private static List<String> tableLines(final List<List<String>> rows, final List<Integer> shaded,
                                           final int first) {
        final List<List<String>> padded = new ArrayList<>();
        for (final List<String> row : rows) {
            final List<String> cells = new ArrayList<>(row);
            cells.set(0, cells.getFirst() + " ".repeat(Math.max(0, first - cells.getFirst().length())));
            padded.add(cells);
        }
        final String[] lines = PrintLayout.table(padded, false).split("\n");
        final List<String> out = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            out.add(shaded.contains(i) ? PrintLayout.shaded(lines[i]) : lines[i]);
        }
        return out;
    }

    /* The widest first cell of these tables, as wide as a printed column goes. */
    @SafeVarargs
    private static int firstColumn(final List<List<String>>... tables) {
        int widest = 0;
        for (final List<List<String>> table : tables) {
            for (final List<String> row : table) {
                widest = Math.max(widest, row.getFirst().length());
            }
        }
        return Math.min(widest, PrintLayout.MAX_COLUMN);
    }

    /* A figure right-aligned in {@code width} characters. */
    private static String figure(final String value, final int width) {
        return " ".repeat(Math.max(0, width - value.length())) + value;
    }

    private List<CraftCatalogPayload.Entry> filtered() {
        final String q = search.query();
        if (q.isEmpty()) {
            return catalog;
        }
        final List<CraftCatalogPayload.Entry> out = new ArrayList<>();
        for (final CraftCatalogPayload.Entry e : catalog) {
            if (e.title().toLowerCase(Locale.ROOT).contains(q)
                    || e.result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(e);
            }
        }
        return out;
    }

    private void itemIcon(final GuiGraphics g, final ItemStack stack, final int x, final int y, final int size) {
        if (stack.isEmpty()) {
            g.fill(x + 1, y + 1, x + size - 1, y + size - 1, skin.fieldBg());
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        final float s = size / 16.0f;
        g.pose().scale(s, s, 1);
        DesktopItems.item(g, stack, 0, 0);
        g.pose().popPose();
    }

    // input

    @Override
    public void mouseClicked(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseClicked(mouseX, mouseY, button);
        if (root.focusedChild() == null) {
            // Typing filters the catalogue whenever nothing else holds the keyboard.
            root.focus(search);
        }
    }

    @Override
    public void mouseReleased(final DesktopWindow window, final double mouseX, final double mouseY, final int button) {
        root.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double delta) {
        if (root.mouseScrolled(lastMouseX, lastMouseY, delta)) {
            return true;
        }
        // The wheel elsewhere moves the tree when it is shown, else the catalogue.
        final int step = delta > 0 ? -1 : 1;
        if (treeList.visible()) {
            treeList.setScroll(treeList.scroll() + step);
        } else {
            catalogList.setScroll(catalogList.scroll() + step);
        }
        return true;
    }

    @Override
    public boolean takesTyping() {
        return IDesktopApp.typingIn(root);
    }

    @Override
    public boolean charTyped(final char c) {
        return root.charTyped(c);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        return root.keyPressed(key, scanCode, modifiers);
    }

    /** The planner's colours, as the palette above names them; the last, the pill of a plan missing ingredients. */
    private record Colours(int good, int missing, int amber, int feasiblePill, int infeasiblePill, int missingPill) {
    }
}
