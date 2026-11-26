/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.cylinder.client;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.jstechseries.cylinder.CylinderTexts;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.jstechseries.cylinder.GasMarking;
import dev.jstech.core.jstechseries.cylinder.MarkGasLayout;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The list an empty Gas Cylinder used in the air opens: every gas of the Core, made yet or not, each a row with the
 * cylinder painted for it, its name and its formula, narrowed by the search; Mark marks the cylinder in that hand for
 * the gas picked.
 */
@PaletteHolder
public final class MarkGasScreen extends Screen {

    private final InteractionHand hand;
    private final List<Row> all = new ArrayList<>();
    private final List<Row> shown = new ArrayList<>();
    private EditBox search;
    private Button mark;
    private int scroll;
    @Nullable
    private Row picked;

    /* The title's ink, the list's ground, the picked row's, a name's, a formula's and the line under the list. */
    static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "gas_mark", new Colours(0xFF404040,
            0xFF6E6E6E, 0xFF3C5AA0, 0xFFFFFFFF, 0xFFB4B4B4, 0xFF808080));
    private static final int MOST_SEARCH = 40;

    public MarkGasScreen(final InteractionHand hand) {
        super(GameText.component(CylinderTexts.MARK_TITLE));
        this.hand = hand;
        for (final Material gas : GasMarking.gases()) {
            this.all.add(new Row(gas, GameText.resolve(gas.name()), gas.value(MaterialProperties.FORMULA)
                    .or(() -> gas.value(MaterialProperties.SYMBOL)).orElse(""), GasCylinders.marked(gas)));
        }
        this.all.sort(Comparator.comparing(row -> row.name().toLowerCase(Locale.ROOT)));
        this.shown.addAll(this.all);
    }

    /** Opens the list for the cylinder in {@code hand}; the Core's client hands this to {@link GasMarking}. */
    public static void open(final InteractionHand hand) {
        Minecraft.getInstance().setScreen(new MarkGasScreen(hand));
    }

    /** The gases the list shows now, as the search narrows them, in their order. */
    public List<Material> shown() {
        return this.shown.stream().map(Row::gas).toList();
    }

    /** The gas picked, or null while none is. */
    public @Nullable Material picked() {
        return this.picked == null ? null : this.picked.gas();
    }

    @Override
    protected void init() {
        final int x0 = MarkGasLayout.x(this.width);
        final int y0 = MarkGasLayout.y(this.height);
        final String was = this.search == null ? "" : this.search.getValue();
        this.search = new EditBox(this.font, x0 + MarkGasLayout.INSET, y0 + MarkGasLayout.SEARCH_Y,
                MarkGasLayout.INNER, MarkGasLayout.SEARCH_HEIGHT, GameText.component(CylinderTexts.MARK_SEARCH));
        this.search.setMaxLength(MOST_SEARCH);
        this.search.setValue(was);
        this.search.setResponder(this::narrow);
        addRenderableWidget(this.search);
        setInitialFocus(this.search);
        this.mark = addRenderableWidget(Button.builder(GameText.component(CylinderTexts.MARK), button -> markPicked())
                .bounds(x0 + MarkGasLayout.MARK_X, y0 + MarkGasLayout.BUTTON_Y, MarkGasLayout.BUTTON_WIDTH,
                        MarkGasLayout.BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(GameText.component(CylinderTexts.CANCEL), button -> onClose())
                .bounds(x0 + MarkGasLayout.CANCEL_X, y0 + MarkGasLayout.BUTTON_Y, MarkGasLayout.BUTTON_WIDTH,
                        MarkGasLayout.BUTTON_HEIGHT).build());
        this.mark.active = this.picked != null;
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        final Colours c = PALETTE.get();
        final int x0 = MarkGasLayout.x(this.width);
        final int y0 = MarkGasLayout.y(this.height);
        MachineArt.window(g, x0, y0, x0 + MarkGasLayout.WIDTH, y0 + MarkGasLayout.HEIGHT, 0, 0);
        final String title = GameText.resolve(CylinderTexts.MARK_TITLE);
        Draw.text(g, this.font, title, x0 + (MarkGasLayout.WIDTH - this.font.width(title)) / 2,
                y0 + MarkGasLayout.TITLE_Y, c.title());
        g.fill(x0 + MarkGasLayout.INSET, y0 + MarkGasLayout.LIST_Y, x0 + MarkGasLayout.INSET + MarkGasLayout.INNER,
                y0 + MarkGasLayout.LIST_Y + MarkGasLayout.LIST_HEIGHT, c.list());
        for (int row = 0; row < MarkGasLayout.ROWS && this.scroll + row < this.shown.size(); row++) {
            row(g, c, this.shown.get(this.scroll + row), x0, y0 + MarkGasLayout.ROW_Y + row * MarkGasLayout.ROW_PITCH);
        }
        g.pose().pushPose();
        g.pose().translate(x0 + MarkGasLayout.INSET, y0 + MarkGasLayout.EVERY_Y, 0);
        g.pose().scale(MarkGasLayout.SMALL, MarkGasLayout.SMALL, 1.0F);
        Draw.text(g, this.font, GameText.resolve(CylinderTexts.MARK_EVERY), 0, 0, c.note());
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int row = MarkGasLayout.rowAt(mouseX - MarkGasLayout.x(this.width),
                mouseY - MarkGasLayout.y(this.height));
        if (button == 0 && row >= 0 && this.scroll + row < this.shown.size()) {
            pick(this.shown.get(this.scroll + row).gas());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final int most = Math.max(0, this.shown.size() - MarkGasLayout.ROWS);
        this.scroll = Math.clamp(this.scroll - (int) Math.signum(scrollY), 0, most);
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && this.picked != null) {
            markPicked();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Picks {@code gas}, as a click on its row does. */
    public void pick(final Material gas) {
        this.picked = this.all.stream().filter(row -> row.gas() == gas).findFirst().orElse(null);
        if (this.mark != null) {
            this.mark.active = this.picked != null;
        }
    }

    /** Marks the cylinder for the gas picked and closes, as Mark does; nothing while none is picked. */
    public void markPicked() {
        if (this.picked != null) {
            GasMarking.mark(this.hand, this.picked.gas());
            onClose();
        }
    }

    /* Shows the gases whose name, formula or id holds what is searched, from the top. */
    private void narrow(final String searched) {
        final String wanted = searched.trim().toLowerCase(Locale.ROOT);
        this.shown.clear();
        for (final Row row : this.all) {
            if (wanted.isEmpty() || row.name().toLowerCase(Locale.ROOT).contains(wanted)
                    || row.formula().toLowerCase(Locale.ROOT).contains(wanted)
                    || row.gas().id().getPath().contains(wanted)) {
                this.shown.add(row);
            }
        }
        this.scroll = 0;
    }

    /* A row: the picked one on its own ground, the cylinder painted for its gas, its name and its formula. */
    private void row(final GuiGraphics g, final Colours c, final Row row, final int x0, final int y) {
        if (row == this.picked) {
            g.fill(x0 + MarkGasLayout.INSET + MarkGasLayout.ROW_INSET, y - 1,
                    x0 + MarkGasLayout.WIDTH - MarkGasLayout.INSET - MarkGasLayout.ROW_INSET,
                    y + MarkGasLayout.ROW_PITCH - 1, c.picked());
        }
        g.renderItem(row.cylinder(), x0 + MarkGasLayout.ICON_X, y);
        final int formulaX = x0 + MarkGasLayout.FORMULA_RIGHT - this.font.width(row.formula());
        final int room = formulaX - (x0 + MarkGasLayout.NAME_X) - 4;
        Draw.text(g, this.font, this.font.plainSubstrByWidth(row.name(), room), x0 + MarkGasLayout.NAME_X,
                y + MarkGasLayout.WORDS_DROP, c.name());
        Draw.text(g, this.font, row.formula(), formulaX, y + MarkGasLayout.WORDS_DROP, c.formula());
    }

    /* A gas of the list: its material, its name and formula as written, and the cylinder painted for it. */
    private record Row(Material gas, String name, String formula, ItemStack cylinder) {
    }

    /**
     * The colours.
     *
     * @param title   the window's title
     * @param list    the list's ground
     * @param picked  the picked row's ground
     * @param name    a gas's name
     * @param formula a gas's formula
     * @param note    the line under the list
     */
    record Colours(int title, int list, int picked, int name, int formula, int note) {
    }
}
