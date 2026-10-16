/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.projector.client;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.multiblock.MultiblockPreview;
import dev.jstech.core.jstechseries.projector.ProjectorSettings;
import dev.jstech.core.jstechseries.projector.ProjectorTexts;
import dev.jstech.core.jstechseries.projector.ProjectorView;
import dev.jstech.core.jstechseries.projector.StructureProjector;
import dev.jstech.core.jstechseries.projector.StructureProjectorBlockEntity;
import dev.jstech.core.jstechseries.projector.StructureProjectorLayout;
import dev.jstech.core.jstechseries.projector.StructureProjectorMenu;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.Projectables;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The Structure Projector's screen, as the approved design draws it: no inventory, since a projector holds nothing;
 * on the steel face, its name plate, the multiblock in 3D (the layer shown whole, those below as ghosts, turned by
 * dragging), the list of every multiblock of the installed mods, what is placed, missing and wrong in the world, and
 * the controls: the turn's knob, the mirror (for a multiblock that allows it), "missing only", the layer's wheel, and
 * the switch with its lamp. Each control sends the projector its settings as it is used.
 */
public final class StructureProjectorScreen extends CoreContainerScreen<StructureProjectorMenu> {

    private final MultiblockPreview drawer = new MultiblockPreview();
    private int scroll;
    private boolean dragging;
    private double lastX;
    private double lastY;

    private static final double[] TURN_ANGLES = {-Math.PI / 2, 0, Math.PI / 2, Math.PI};
    private static final TextKey[] TURN_WORDS = {ProjectorTexts.NORTH, ProjectorTexts.EAST, ProjectorTexts.SOUTH,
            ProjectorTexts.WEST};
    private static final int ITEM_INSET = 6;
    private static final int WORDS_INSET = 4;
    private static final int MOST_NAMED = 3;
    private static final double SHADE_HI = 0.3;
    private static final double SHADE_LO = 0.45;
    private static final double DIM = 0.45;
    private static final double SCANLINE = 0.18;

    public StructureProjectorScreen(final StructureProjectorMenu menu, final Inventory inventory,
                                    final Component title) {
        super(menu, inventory, title);
        this.imageWidth = StructureProjectorLayout.WIDTH;
        this.imageHeight = StructureProjectorLayout.HEIGHT;
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = mouseX - this.leftPos;
        final double y = mouseY - this.topPos;
        final ProjectorSettings now = projector().settings();
        final int row = StructureProjectorLayout.rowAt(x, y);
        final List<ResourceLocation> ids = ids();
        if (row >= 0 && this.scroll + row < ids.size()) {
            send(now.choosing(ids.get(this.scroll + row)));
            return true;
        }
        if (StructureProjectorLayout.onKnob(x, y)) {
            send(new ProjectorSettings(now.chosen(), now.turn() + (button == 1 ? -1 : 1), now.mirrored(),
                    now.layer(), now.missingOnly(), now.on()));
            return true;
        }
        if (StructureProjectorLayout.onToggle(x, y, StructureProjectorLayout.MIRROR_Y)) {
            send(new ProjectorSettings(now.chosen(), now.turn(), !now.mirrored(), now.layer(), now.missingOnly(),
                    now.on()));
            return true;
        }
        if (StructureProjectorLayout.onToggle(x, y, StructureProjectorLayout.MISSING_Y)) {
            send(new ProjectorSettings(now.chosen(), now.turn(), now.mirrored(), now.layer(), !now.missingOnly(),
                    now.on()));
            return true;
        }
        if (StructureProjectorLayout.onWheel(x, y)) {
            final int layers = projector().preview().map(ShapePreview::layers).orElse(1);
            final int shown = now.layer() == 0 ? layers : now.layer();
            final boolean up = y < StructureProjectorLayout.WHEEL_Y + StructureProjectorLayout.WHEEL_HEIGHT / 2.0;
            final int next = Math.max(1, Math.min(layers, shown + (up ? 1 : -1)));
            send(new ProjectorSettings(now.chosen(), now.turn(), now.mirrored(), next == layers ? 0 : next,
                    now.missingOnly(), now.on()));
            return true;
        }
        if (StructureProjectorLayout.onRocker(x, y)) {
            send(new ProjectorSettings(now.chosen(), now.turn(), now.mirrored(), now.layer(), now.missingOnly(),
                    !now.on()));
            return true;
        }
        if (onPreview(x, y)) {
            this.dragging = true;
            this.lastX = mouseX;
            this.lastY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        if (this.dragging) {
            this.drawer.drag(mouseX - this.lastX, mouseY - this.lastY);
            this.lastX = mouseX;
            this.lastY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final int most = Math.max(0, ids().size() - StructureProjectorLayout.ROWS);
        this.scroll = Math.max(0, Math.min(most, this.scroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final int x0 = this.leftPos;
        final int y0 = this.topPos;
        MachineArt.window(g, x0, y0 + StructureProjectorLayout.WINDOW_Y, x0 + StructureProjectorLayout.WIDTH,
                y0 + StructureProjectorLayout.HEIGHT, x0 + StructureProjectorLayout.TAB_X,
                x0 + StructureProjectorLayout.TAB_X + StructureProjectorLayout.TAB_SIZE);
        MachineArt.tab(g, x0 + StructureProjectorLayout.TAB_X, y0, true);
        g.renderItem(new ItemStack(StructureProjector.BLOCK.get()), x0 + StructureProjectorLayout.TAB_X + ITEM_INSET,
                y0 + ITEM_INSET);
        MachineArt.shade(g, x0 + StructureProjectorLayout.FACE_LEFT, y0 + StructureProjectorLayout.FACE_TOP,
                x0 + StructureProjectorLayout.FACE_RIGHT, y0 + StructureProjectorLayout.FACE_BOTTOM, c.steel(),
                SHADE_HI, SHADE_LO);
        for (final int bx : new int[] {StructureProjectorLayout.FACE_LEFT + 2,
                StructureProjectorLayout.FACE_RIGHT - 5}) {
            for (final int by : new int[] {StructureProjectorLayout.FACE_TOP + 2,
                    StructureProjectorLayout.FACE_BOTTOM - 5}) {
                MachineArt.bolt(g, x0 + bx, y0 + by);
            }
        }
        MachineArt.plate(g, this.font, x0 + StructureProjectorLayout.PLATE_X, y0 + StructureProjectorLayout.PLATE_Y,
                upper(ProjectorTexts.TITLE.text()), c.aluminium(), c.plateInk());
        final Optional<ShapePreview> preview = projector().preview();
        final ProjectorSettings settings = projector().settings();
        preview(g, c, preview, settings);
        list(g, c, settings);
        world(g, c, preview.isPresent());
        controls(g, c, preview, settings);
        status(g, preview, settings);
    }

    /* Nothing over the face: its words are its own, and it has no inventory. */
    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
    }

    /* The multiblock in its window, its layer at the foot on the left and the hint on the right. */
    private void preview(final GuiGraphics g, final MachineArt.Colours c, final Optional<ShapePreview> preview,
                         final ProjectorSettings settings) {
        final int x = this.leftPos + StructureProjectorLayout.PREVIEW_X;
        final int y = this.topPos + StructureProjectorLayout.PREVIEW_Y;
        final int w = StructureProjectorLayout.PREVIEW_WIDTH;
        final int h = StructureProjectorLayout.PREVIEW_HEIGHT;
        MachineArt.bezel(g, x - 3, y - 3, x + w + 3, y + h + 3, true);
        MachineArt.fill(g, x, y, x + w, y + h, MachineArt.mix(c.lcdGround(), c.black(), DIM));
        if (preview.isPresent()) {
            Draw.pushScissor(g, x + 1, y + 1, x + w - 1, y + h - 1);
            try {
                this.drawer.draw(g, x, y, w, h, preview.get(), settings.layer());
            } finally {
                Draw.popScissor(g);
            }
            final int layers = preview.get().layers();
            MachineArt.text(g, this.font, upper(ProjectorTexts.LAYER_OF.with(settings.layer() == 0 ? layers
                    : settings.layer(), layers)), x + WORDS_INSET, this.topPos
                    + StructureProjectorLayout.PREVIEW_WORDS_Y, c.white());
        }
        final String hint = GameText.resolve(ProjectorTexts.DRAG.text());
        MachineArt.text(g, this.font, hint, x + w - WORDS_INSET - MachineArt.width(this.font, hint),
                this.topPos + StructureProjectorLayout.PREVIEW_WORDS_Y, MachineArt.mix(c.white(), c.black(), DIM));
    }

    /* The multiblocks of the installed mods, the one shown marked. */
    private void list(final GuiGraphics g, final MachineArt.Colours c, final ProjectorSettings settings) {
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.MULTIBLOCK.text()), this.leftPos
                + StructureProjectorLayout.LIST_LABEL_X, this.topPos + StructureProjectorLayout.LIST_LABEL_Y, false);
        final List<ResourceLocation> ids = ids();
        this.scroll = Math.max(0, Math.min(this.scroll, Math.max(0, ids.size() - StructureProjectorLayout.ROWS)));
        final List<Row> rows = new ArrayList<>();
        for (int i = this.scroll; i < Math.min(ids.size(), this.scroll + StructureProjectorLayout.ROWS); i++) {
            final boolean shown = settings.chosen().filter(ids.get(i)::equals).isPresent();
            rows.add(new Row((shown ? "> " : "  ") + GameText.resolve(Projectables.name(ids.get(i))), shown));
        }
        display(g, c, StructureProjectorLayout.LIST_X, StructureProjectorLayout.LIST_Y,
                StructureProjectorLayout.LIST_WIDTH, StructureProjectorLayout.LIST_HEIGHT, rows);
    }

    /* What the projector sees in the world: placed, missing and wrong, and what is missing. */
    private void world(final GuiGraphics g, final MachineArt.Colours c, final boolean chosen) {
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.IN_THE_WORLD.text()), this.leftPos
                + StructureProjectorLayout.WORLD_LABEL_X, this.topPos + StructureProjectorLayout.WORLD_LABEL_Y,
                false);
        final List<Row> rows = new ArrayList<>();
        final ProjectorView view = chosen ? ProjectorClient.seen(projector()) : null;
        if (view == null) {
            rows.add(new Row(GameText.resolve(ProjectorTexts.NOTHING.text()), false));
        } else {
            rows.add(new Row(upper(ProjectorTexts.PLACED.with(view.placed(), view.places().size())), true));
            rows.add(new Row(upper(ProjectorTexts.MISSING.with(view.missing())), true));
            rows.add(new Row(upper(ProjectorTexts.WRONG.with(view.wrong())), true));
            rows.add(new Row(view.missing() == 0 ? GameText.resolve(ProjectorTexts.NOTHING_MISSING.text())
                    : GameText.resolve(ScreenWords.counts(view.missingByBlock(), MOST_NAMED)), false));
        }
        display(g, c, StructureProjectorLayout.WORLD_X, StructureProjectorLayout.WORLD_Y,
                StructureProjectorLayout.WORLD_WIDTH, StructureProjectorLayout.WORLD_HEIGHT, rows);
    }

    /* The knob, the toggles, the layer's wheel, the switch and its lamp, each engraved with what it does. */
    private void controls(final GuiGraphics g, final MachineArt.Colours c, final Optional<ShapePreview> preview,
                          final ProjectorSettings settings) {
        final int x0 = this.leftPos;
        final int y0 = this.topPos;
        final List<MachineArt.KnobLabel> labels = new ArrayList<>();
        for (int i = 0; i < TURN_ANGLES.length; i++) {
            labels.add(new MachineArt.KnobLabel(upper(TURN_WORDS[i].text()), TURN_ANGLES[i], i == settings.turn()));
        }
        MachineArt.knob(g, this.font, x0 + StructureProjectorLayout.KNOB_X, y0 + StructureProjectorLayout.KNOB_Y,
                StructureProjectorLayout.KNOB_RADIUS, TURN_ANGLES[settings.turn()], labels,
                upper(ProjectorTexts.TURN.text()));
        final boolean mirrors = projector().shape().filter(shape -> shape instanceof MultiblockPattern pattern
                && pattern.mirrors()).isPresent();
        MachineArt.toggle(g, x0 + StructureProjectorLayout.TOGGLE_X, y0 + StructureProjectorLayout.MIRROR_Y,
                settings.mirrored());
        final String mirror = upper(ProjectorTexts.MIRROR.text());
        if (mirrors) {
            MachineArt.engrave(g, this.font, mirror, x0 + StructureProjectorLayout.TOGGLE_LABEL_X,
                    y0 + StructureProjectorLayout.MIRROR_Y + 3, false);
        } else {
            MachineArt.text(g, this.font, mirror, x0 + StructureProjectorLayout.TOGGLE_LABEL_X,
                    y0 + StructureProjectorLayout.MIRROR_Y + 3, MachineArt.mix(c.steel(), c.white(), DIM));
        }
        MachineArt.toggle(g, x0 + StructureProjectorLayout.TOGGLE_X, y0 + StructureProjectorLayout.MISSING_Y,
                settings.missingOnly());
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.MISSING_WORD.text()),
                x0 + StructureProjectorLayout.TOGGLE_LABEL_X, y0 + StructureProjectorLayout.MISSING_Y, false);
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.ONLY.text()),
                x0 + StructureProjectorLayout.TOGGLE_LABEL_X, y0 + StructureProjectorLayout.MISSING_Y + 7, false);
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.LAYER.text()), x0 + StructureProjectorLayout.TOGGLE_X,
                y0 + StructureProjectorLayout.LAYER_LABEL_Y, false);
        final int layers = preview.map(ShapePreview::layers).orElse(0);
        final int shown = settings.layer() == 0 ? layers : settings.layer();
        MachineArt.thumbwheel(g, this.font, x0 + StructureProjectorLayout.WHEEL_X,
                y0 + StructureProjectorLayout.WHEEL_Y, Integer.toString(Math.min(9, shown)));
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.OF.with(layers)), x0 + StructureProjectorLayout.OF_X,
                y0 + StructureProjectorLayout.OF_Y, false);
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.ON.text()), x0 + StructureProjectorLayout.ON_WORD_X,
                y0 + StructureProjectorLayout.ON_WORD_Y, false);
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.OFF.text()),
                x0 + StructureProjectorLayout.ON_WORD_X - 2, y0 + StructureProjectorLayout.OFF_WORD_Y, false);
        MachineArt.rocker(g, x0 + StructureProjectorLayout.ROCKER_X, y0 + StructureProjectorLayout.ROCKER_Y,
                settings.on());
        MachineArt.lamp(g, x0 + StructureProjectorLayout.LAMP_X, y0 + StructureProjectorLayout.LAMP_Y, c.lampRun(),
                settings.on() && preview.isPresent());
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.SHOWING.text()), x0 + StructureProjectorLayout.SHOWING_X,
                y0 + StructureProjectorLayout.LAMP_Y, false);
    }

    /* Along the face's foot: the multiblock's name, its size and its mod. */
    private void status(final GuiGraphics g, final Optional<ShapePreview> preview,
                        final ProjectorSettings settings) {
        if (preview.isEmpty() || settings.chosen().isEmpty()) {
            return;
        }
        final ResourceLocation id = settings.chosen().get();
        MachineArt.engrave(g, this.font, upper(ProjectorTexts.STATUS.with(Projectables.name(id),
                preview.get().sizeX(), preview.get().sizeY(), preview.get().sizeZ(), ScreenWords.modName(id))),
                this.leftPos + StructureProjectorLayout.STATUS_X, this.topPos + StructureProjectorLayout.STATUS_Y,
                false);
    }

    /* A green display behind glass in its bezel, each row bright when it is lit and dim when not. */
    private void display(final GuiGraphics g, final MachineArt.Colours c, final int px, final int py, final int w,
                         final int h, final List<Row> rows) {
        final int x = this.leftPos + px;
        final int y = this.topPos + py;
        MachineArt.bezel(g, x - 3, y - 3, x + w + 3, y + h + 3, false);
        MachineArt.fill(g, x, y, x + w, y + h, c.lcdGround());
        for (int gy = y; gy < y + h; gy += 2) {
            MachineArt.hline(g, x, x + w, gy, MachineArt.mix(c.lcdGround(), c.black(), SCANLINE));
        }
        for (int i = 0; i < rows.size(); i++) {
            final Row row = rows.get(i);
            final int colour = row.lit() ? c.lcdGlow() : MachineArt.mix(c.lcdGlow(), c.lcdGround(), DIM);
            MachineArt.text(g, this.font, MachineArt.fit(this.font, row.text(), w - 6), x + 3,
                    y + 3 + i * StructureProjectorLayout.ROW_HEIGHT, colour);
        }
        MachineArt.glassShine(g, x, y, x + w, y + h);
    }

    private boolean onPreview(final double x, final double y) {
        return x >= StructureProjectorLayout.PREVIEW_X && y >= StructureProjectorLayout.PREVIEW_Y
                && x < StructureProjectorLayout.PREVIEW_X + StructureProjectorLayout.PREVIEW_WIDTH
                && y < StructureProjectorLayout.PREVIEW_Y + StructureProjectorLayout.PREVIEW_HEIGHT;
    }

    private void send(final ProjectorSettings settings) {
        StructureProjector.SETTINGS.send(settings);
    }

    private StructureProjectorBlockEntity projector() {
        return this.menu.projector();
    }

    private static List<ResourceLocation> ids() {
        return List.copyOf(Projectables.all().keySet());
    }

    private static String upper(final Text text) {
        return GameText.resolve(text).toUpperCase(Locale.ROOT);
    }

    /** One row of a display: what it says and whether it is lit. */
    private record Row(String text, boolean lit) {
    }
}
