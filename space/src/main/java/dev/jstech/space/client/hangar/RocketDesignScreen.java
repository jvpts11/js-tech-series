/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.hangar;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.client.camera.OrbitView;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.dimension.DimensionRules;
import dev.jstech.core.dimension.DimensionRulesData;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.api.Propellant;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketKit;
import dev.jstech.space.api.RocketKits;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.gui.RocketDesignLayout;
import dev.jstech.space.hangar.DesignDestinations;
import dev.jstech.space.hangar.DesignEdits;
import dev.jstech.space.hangar.DesignReport;
import dev.jstech.space.hangar.HangarBay;
import dev.jstech.space.hangar.HangarTerminalBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.hangar.RocketDesignMenu;
import dev.jstech.space.hangar.RocketDesigner;
import dev.jstech.space.rocket.RocketPartModels;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.RocketStages;
import dev.jstech.space.rocket.StageMath;
import dev.jstech.space.universe.SolarBody;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.ToDoubleFunction;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The Rocket Design, a formed Rocket Hangar's design screen, as the approved design draws it: the hangar itself seen
 * in 3D through the middle, the rocket standing on its bay's floor, and the hangar's own plates round it. A part is
 * dragged from its bin to a green node of the rocket, as in Kerbal Space Program's editor, where it snaps; a part of
 * the rocket is dragged off with those on its far side from the root, dropped on another node, or on the bins to be
 * thrown away. The tools turn a part a quarter turn, stretch a tank, and choose the root; the mirror sets how many
 * boosters hang round a part at once, and the snap how close to a node a part must be dropped. The right mouse button
 * turns the view round the rocket, the wheel brings it nearer, the middle button raises and lowers it. The stages'
 * delta-v, the thrust against weight on the world it leaves, the destinations and what the rocket lacks are worked out
 * as it is built ({@link DesignReport}). Every change is sent to the hangar, which keeps the design; it is saved as
 * one of the hangar's plans, loaded from them, and sent to the bay to be built once it is ready.
 */
public final class RocketDesignScreen extends AbstractContainerScreen<RocketDesignMenu> {

    private final HangarTerminalBlockEntity terminal;
    private final Deque<RocketDesign> undone = new ArrayDeque<>();
    private final Deque<RocketDesign> redone = new ArrayDeque<>();
    private final Set<Integer> hiddenDiameters = new HashSet<>();
    private RocketDesign design;
    private DesignReport report;
    private Map<ResourceLocation, RocketPart> parts = Map.of();
    private Map<ResourceLocation, RocketKit> kits = Map.of();
    private List<PartCatalogue.Entry> catalogue = List.of();
    private Tool tool = Tool.PLACE;
    private boolean snap = true;
    private int mirror = 1;
    private int root;
    private PartCatalogue.Category category = PartCatalogue.Category.ENGINES;
    private int binScroll;
    private int stageScroll;
    private @Nullable ResourceLocation chosen;
    private @Nullable Held held;
    private int stretched = -1;
    private double stretchFrom;
    private int stretchLayers;
    private int stretchTo;
    private boolean turning;
    private boolean lifting;
    private double lastX;
    private double lastY;
    private boolean plansOpen;
    private @Nullable StringBuilder naming;
    private @Nullable Text said;
    private long saidUntil;
    private @Nullable OrbitView view;
    private float scale = 1;
    private float originX;
    private float originY;
    private Hit hovered = Hit.NONE;
    private Target target = Target.NONE;
    /* How tall the rocket was when the view last turned round its middle. */
    private int framed = -1;

    private static final int HIDDEN = -10_000;
    /* How near a node, in the window's scaled pixels, a part must be let go to snap to it, the snap on and off. */
    private static final double SNAP_REACH = 24;
    private static final double FREE_REACH = 8;
    private static final long SAID_MILLIS = 3_000;
    private static final float TURN_SPEED = 0.5F;
    private static final double LIFT_SPEED = 0.004;
    private static final double ZOOM_STEP = 1.12;
    private static final int MOST_UNDO = 64;
    private static final int SEGMENTS = 18;
    private static final float START_PITCH = 18.0F;
    private static final double SEEN_FROM = 1.6;
    private static final double LEAST_SEEN_FROM = 10;
    /* The height the view frames when the rocket is shorter, or has no parts yet. */
    private static final int LEAST_HEIGHT = 4;
    /* How many of the window's scaled pixels the mouse moves a tank's top by a layer. */
    private static final double LAYER_PIXELS = 10;
    /* How far a ray looks for a part, in blocks. */
    private static final double REACH = 256;
    /* A card's picture: the angle it is seen from, how much of its box the part fills. */
    private static final float CARD_PITCH = 25.0F;
    private static final float CARD_YAW = 215.0F;
    private static final float CARD_DEPTH = 80.0F;
    private static final double CARD_SPIN = 0.04;
    /* A vacuum engine's impulse at sea level is well under its own in a vacuum. */
    private static final double VACUUM_NOZZLE = 0.7;
    private static final double KILO = 1_000;
    private static final int GLYPH_HEIGHT = 8;
    /* The sign the game's text takes as its formatting mark, which a name must not hold. */
    private static final char FORMATTING_SIGN = (char) 0xA7;

    public RocketDesignScreen(final RocketDesignMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.terminal = menu.terminal();
        this.design = this.terminal.design();
        this.report = DesignReport.of(Map.of(), this.design.stack(), 1, 1, Optional.empty(), Optional.empty());
        this.titleLabelX = HIDDEN;
        this.inventoryLabelY = HIDDEN;
    }

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTick) {
        scene(mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY,
                                 final float partialTick) {
        // The hangar itself is seen through the middle of the screen: nothing dims or blurs it, only the plates.
        renderBg(graphics, partialTick, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = frameX(mouseX);
        final double y = frameY(mouseY);
        if (this.plansOpen) {
            clickPlans(x, y);
            return true;
        }
        if (this.naming != null && !onName(x, y)) {
            finishNaming(true);
        }
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            if (clickTop(x, y) || clickBins(x, y) || clickBottom(x, y) || RocketDesignLayout.onPlate(x, y)) {
                return true;
            }
            return clickWorld(mouseX, mouseY);
        }
        if (!RocketDesignLayout.onPlate(x, y) && (button == InputConstants.MOUSE_BUTTON_RIGHT
                || button == InputConstants.MOUSE_BUTTON_MIDDLE)) {
            this.turning = button == InputConstants.MOUSE_BUTTON_RIGHT;
            this.lifting = button == InputConstants.MOUSE_BUTTON_MIDDLE;
            this.lastX = mouseX;
            this.lastY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        final OrbitView seen = this.view;
        if (seen != null && this.turning) {
            seen.turn((float) (mouseX - this.lastX) * TURN_SPEED, (float) (mouseY - this.lastY) * TURN_SPEED);
        } else if (seen != null && this.lifting) {
            seen.lift((this.lastY - mouseY) * LIFT_SPEED * seen.distance());
        } else if (this.stretched >= 0) {
            this.stretchTo = Math.clamp(this.stretchLayers + Math.round((this.stretchFrom - mouseY) / LAYER_PIXELS),
                    1, DesignEdits.MOST_LAYERS);
        }
        this.lastX = mouseX;
        this.lastY = mouseY;
        return true;
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && this.held != null) {
            // Where it is let go, whatever the last frame saw.
            this.target = target(mouseX, mouseY);
            drop();
        } else if (button == InputConstants.MOUSE_BUTTON_LEFT && this.stretched >= 0) {
            final int place = this.stretched;
            this.stretched = -1;
            if (this.stretchTo != this.stretchLayers) {
                commit(this.design.with(DesignEdits.stretch(this.design.stack(), place, this.stretchTo)));
            }
        }
        this.turning = false;
        this.lifting = false;
        return true;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final double x = frameX(mouseX);
        final double y = frameY(mouseY);
        final int step = scrollY > 0 ? -1 : 1;
        if (this.plansOpen) {
            return true;
        }
        if (x >= RocketDesignLayout.LEFT_X0 && x < RocketDesignLayout.LEFT_X1 && y >= RocketDesignLayout.CARD_Y0
                && y < RocketDesignLayout.SHEET_Y0) {
            final int rows = (shelf().size() + RocketDesignLayout.CARD_COLUMNS - 1) / RocketDesignLayout.CARD_COLUMNS;
            this.binScroll = Math.clamp(this.binScroll + step, 0, Math.max(0, rows - RocketDesignLayout.CARD_ROWS));
        } else if (x >= RocketDesignLayout.RIGHT_X0 && x < RocketDesignLayout.RIGHT_X1
                && y < RocketDesignLayout.DELTA_V_Y) {
            this.stageScroll = Math.clamp(this.stageScroll + step, 0, Math.max(0, this.report.stages().size()
                    - RocketDesignLayout.STAGE_ROWS));
        } else if (!RocketDesignLayout.onPlate(x, y) && this.view != null) {
            this.view.zoom(scrollY > 0 ? 1 / ZOOM_STEP : ZOOM_STEP);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.naming != null) {
            if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER) {
                finishNaming(true);
            } else if (key == InputConstants.KEY_ESCAPE) {
                finishNaming(false);
            } else if (key == InputConstants.KEY_BACKSPACE && !this.naming.isEmpty()) {
                this.naming.setLength(this.naming.length() - 1);
            }
            return true;
        }
        if (hasControlDown() && (key == InputConstants.KEY_Z || key == InputConstants.KEY_Y)) {
            if (key == InputConstants.KEY_Y || hasShiftDown()) {
                redo();
            } else {
                undo();
            }
            return true;
        }
        switch (key) {
            case InputConstants.KEY_E -> confirm();
            case InputConstants.KEY_S -> save();
            case InputConstants.KEY_L -> this.plansOpen = !this.plansOpen;
            case InputConstants.KEY_X, InputConstants.KEY_DELETE -> clear();
            case InputConstants.KEY_R -> turnHeld();
            case InputConstants.KEY_ESCAPE -> {
                if (this.plansOpen) {
                    this.plansOpen = false;
                    return true;
                }
                return super.keyPressed(key, scanCode, modifiers);
            }
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(final char typed, final int modifiers) {
        if (this.naming != null && typed >= ' ' && typed != FORMATTING_SIGN
                && this.naming.length() < RocketDesign.LONGEST_NAME) {
            this.naming.append(typed);
            return true;
        }
        return super.charTyped(typed, modifiers);
    }

    @Override
    public void removed() {
        super.removed();
        if (this.view != null) {
            this.view.release();
            this.view = null;
        }
        RocketDesignScene.hide();
    }

    /** The design on the screen; for a test to look at. */
    public RocketDesign design() {
        return this.design;
    }

    /** What the screen works out of its design; for a test to look at. */
    public DesignReport report() {
        return this.report;
    }

    /** The view the screen turns round its rocket; for a test to look at. */
    public Optional<OrbitView> view() {
        return Optional.ofNullable(this.view);
    }

    /** Sets {@code to} as the design, as a player's edit is: kept to be undone, and sent to the hangar. */
    public void edit(final RocketDesign to) {
        commit(to);
    }

    @Override
    protected void init() {
        final double pixels = Math.min(this.minecraft.getWindow().getWidth() / (double) RocketDesignLayout.WIDTH,
                this.minecraft.getWindow().getHeight() / (double) RocketDesignLayout.HEIGHT);
        // Whole or half screen pixels to a pixel of the frame where the window allows, so its letters stay sharp.
        final double sharp = Math.floor(pixels * 2) / 2;
        this.scale = (float) ((sharp >= 1 ? sharp : pixels) / this.minecraft.getWindow().getGuiScale());
        this.imageWidth = Math.round(RocketDesignLayout.WIDTH * this.scale);
        this.imageHeight = Math.round(RocketDesignLayout.HEIGHT * this.scale);
        super.init();
        this.originX = (this.width - RocketDesignLayout.WIDTH * this.scale) / 2;
        this.originY = (this.height - RocketDesignLayout.HEIGHT * this.scale) / 2;
        if (this.minecraft.level != null) {
            this.parts = RocketParts.all(this.minecraft.level);
            this.kits = RocketKits.all(this.minecraft.level);
            this.catalogue = PartCatalogue.of(this.parts);
        }
        if (this.design.name().isBlank()) {
            this.design = this.design.named(GameText.resolve(RocketDesignTexts.UNNAMED.with(
                    this.terminal.plans().size() + 1)).toUpperCase(Locale.ROOT));
            RocketDesigner.DESIGN.send(this.design);
        }
        recount();
        final Optional<HangarBay> bay = this.terminal.bay();
        if (this.view == null && bay.isPresent()) {
            final BlockPos pad = bay.get().pad();
            final Direction into = this.terminal.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
            final double tall = Math.max(LEAST_HEIGHT, this.report.height());
            this.view = OrbitView.hold(Vec3.atBottomCenterOf(pad).add(0, tall / 2, 0), new AABB(
                    Vec3.atLowerCornerOf(bay.get().low()), Vec3.atLowerCornerOf(bay.get().high().offset(1, 1, 1))),
                    into.toYRot(), START_PITCH, Math.max(LEAST_SEEN_FROM, tall * SEEN_FROM));
            this.framed = this.report.height();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.terminal.bay().isEmpty() || this.terminal.isRemoved()) {
            onClose();
        }
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        g.pose().pushPose();
        g.pose().translate(this.originX, this.originY, 0);
        g.pose().scale(this.scale, this.scale, 1);
        top(g);
        bins(g);
        stages(g);
        bottom(g);
        g.pose().popPose();
        nodes(g);
        if (this.plansOpen) {
            g.pose().pushPose();
            g.pose().translate(this.originX, this.originY, 0);
            g.pose().scale(this.scale, this.scale, 1);
            plans(g);
            g.pose().popPose();
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Every word is on the plates, drawn with them.
    }

    /* ---------------------------------------------------------------------------------------------- the plates */

    /* The hangar's sign, the design's name and counts, the crane's lamp, the tools. */
    private void top(final GuiGraphics g) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        RocketDesignArt.plate(g, this.font, RocketDesignLayout.MIDDLE_X0, RocketDesignLayout.PLATE_Y0,
                RocketDesignLayout.MIDDLE_X1, RocketDesignLayout.TOP_Y1, null);
        final int y0 = RocketDesignLayout.SIGN_Y0;
        final int y1 = RocketDesignLayout.SIGN_Y1;
        RocketDesignArt.hazard(g, RocketDesignLayout.SIGN_X0, y0, RocketDesignLayout.SIGN_X0 + 14, y1);
        g.fill(RocketDesignLayout.SIGN_X0 + 14, y0, RocketDesignLayout.SIGN_X1 - 14, y1, c.yellow());
        RocketDesignArt.frame(g, RocketDesignLayout.SIGN_X0 + 14, y0, RocketDesignLayout.SIGN_X1 - 14, y1,
                c.black());
        Draw.textCentered(g, this.font, up(RocketDesignTexts.HANGAR), (RocketDesignLayout.SIGN_X0
                + RocketDesignLayout.SIGN_X1) / 2, y0 + 4, c.black());
        RocketDesignArt.hazard(g, RocketDesignLayout.SIGN_X1 - 14, y0, RocketDesignLayout.SIGN_X1, y1);
        final int nameWidth = RocketDesignLayout.NAME_X1 - RocketDesignLayout.NAME_X0;
        if (this.naming != null) {
            RocketDesignArt.well(g, RocketDesignLayout.NAME_X0 - 2, y0, RocketDesignLayout.NAME_X1, y1);
            final boolean blink = Util.getMillis() / 500 % 2 == 0;
            final String typed = this.naming.toString().toUpperCase(Locale.ROOT) + (blink ? "_" : "");
            Draw.text(g, this.font, tail(typed, nameWidth - 2), RocketDesignLayout.NAME_X0, y0 + 4, c.yellow());
        } else {
            Draw.text(g, this.font, fit(this.design.name().toUpperCase(Locale.ROOT), nameWidth),
                    RocketDesignLayout.NAME_X0, y0 + 4, c.stencil());
        }
        Draw.text(g, this.font, fit(up(counts(this.report.stages().size(), this.report.height())),
                RocketDesignLayout.LAMP_X - RocketDesignLayout.COUNTS_X - 4),
                RocketDesignLayout.COUNTS_X, y0 + 4, c.dimPaint());
        RocketDesignArt.lamp(g, RocketDesignLayout.LAMP_X, y0 + 3, c.green(), true);
        Draw.text(g, this.font, up(RocketDesignTexts.CRANE_READY), RocketDesignLayout.LAMP_X + 12, y0 + 4,
                c.stencil());
        for (int i = 0; i < Tool.BUTTONS; i++) {
            RocketDesignArt.tool(g, this.font, toolX(i), RocketDesignLayout.TOOLS_Y0, toolIcon(i), toolLabel(i),
                    toolOn(i));
        }
    }

    /* The parts' bins: the categories' tabs, the diameters' toggles, the bins in sight, the chosen part's sheet. */
    private void bins(final GuiGraphics g) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        RocketDesignArt.plate(g, this.font, RocketDesignLayout.LEFT_X0, RocketDesignLayout.PLATE_Y0,
                RocketDesignLayout.LEFT_X1, RocketDesignLayout.PLATE_Y1, up(RocketDesignTexts.PARTS_BIN));
        for (int i = 0; i < RocketDesignLayout.TABS; i++) {
            final int tx = RocketDesignLayout.TAB_X0 + i * RocketDesignLayout.TAB_STEP;
            final int ty = RocketDesignLayout.TAB_Y0;
            final PartCatalogue.Category tab = PartCatalogue.CATEGORIES.get(i);
            final boolean on = tab == this.category;
            RocketDesignArt.well(g, tx, ty, tx + RocketDesignLayout.TAB_SIZE, ty + RocketDesignLayout.TAB_SIZE);
            final int face = on ? c.yellow() : c.paintLo();
            g.fill(tx + 1, ty + 1, tx + 21, ty + 20, face);
            RocketDesignArt.hline(g, tx + 1, tx + 21, ty + 1, RocketDesignArt.mix(face, c.white(), 0.35F));
            RocketDesignArt.icon(g, tab.icon(), tx + 6, ty + 5, on ? c.black() : c.stencil(),
                    on ? c.black() : c.yellow());
            g.fill(tx + 8, ty + 18, tx + 14, ty + 20, c.paintDeep());
        }
        Draw.text(g, this.font, up(this.category.title()), RocketDesignLayout.TAB_X0,
                RocketDesignLayout.CATEGORY_Y + 2, c.stencil());
        final List<Integer> sizes = diameters();
        for (int i = 0; i < sizes.size(); i++) {
            final int x1 = RocketDesignLayout.DIAMETERS_X1 - i * RocketDesignLayout.DIAMETER_STEP;
            final int x0 = x1 - RocketDesignLayout.DIAMETER_W;
            final int size = sizes.get(sizes.size() - 1 - i);
            final boolean on = !this.hiddenDiameters.contains(size);
            final int y0 = RocketDesignLayout.CATEGORY_Y;
            g.fill(x0, y0, x1, y0 + RocketDesignLayout.DIAMETER_H, on ? c.yellow() : c.paintLo());
            RocketDesignArt.frame(g, x0, y0, x1, y0 + RocketDesignLayout.DIAMETER_H, c.black());
            Draw.textCentered(g, this.font, up(RocketDesignTexts.DIAMETER.with(size)), (x0 + x1) / 2, y0 + 2,
                    on ? c.black() : c.dimPaint());
        }
        final List<PartCatalogue.Entry> shelf = shelf();
        for (int i = 0; i < RocketDesignLayout.CARD_COLUMNS * RocketDesignLayout.CARD_ROWS; i++) {
            final int index = this.binScroll * RocketDesignLayout.CARD_COLUMNS + i;
            final int x0 = RocketDesignLayout.cardX(i);
            final int y0 = RocketDesignLayout.cardY(i);
            RocketDesignArt.well(g, x0, y0, x0 + RocketDesignLayout.CARD_W, y0 + RocketDesignLayout.CARD_H);
            if (index < shelf.size()) {
                card(g, shelf.get(index), x0, y0);
            }
        }
        sheet(g);
    }

    /* A part's bin: its picture on the bin's floor, its label card under it, framed yellow when chosen. */
    private void card(final GuiGraphics g, final PartCatalogue.Entry entry, final int x0, final int y0) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        g.fill(x0 + 1, y0 + 1, x0 + RocketDesignLayout.CARD_W - 1, y0 + RocketDesignLayout.CARD_PICTURE_H, c.bin());
        picture(g, entry, x0 + 1, y0 + 1, x0 + RocketDesignLayout.CARD_W - 1, y0 + RocketDesignLayout.CARD_PICTURE_H);
        g.fill(x0 + 3, y0 + 43, x0 + 73, y0 + 69, c.holder());
        g.fill(x0 + 5, y0 + 45, x0 + 71, y0 + 67, c.paper());
        final String code = code(entry);
        Draw.text(g, this.font, code, x0 + 8, y0 + 47, c.ink());
        final String figure = figure(entry.part());
        Draw.text(g, this.font, figure, x0 + 69 - this.font.width(figure), y0 + 47, c.ink());
        Draw.text(g, this.font, fit(up(RocketDesignTexts.CARD_LINE.with(Text.literal(label(entry)),
                entry.part().diameter())), 62), x0 + 8, y0 + 57, c.ink());
        if (entry.id().equals(this.chosen)) {
            RocketDesignArt.frame(g, x0 - 2, y0 - 2, x0 + RocketDesignLayout.CARD_W + 2,
                    y0 + RocketDesignLayout.CARD_H + 2, c.yellow());
            RocketDesignArt.frame(g, x0 - 1, y0 - 1, x0 + RocketDesignLayout.CARD_W + 1,
                    y0 + RocketDesignLayout.CARD_H + 1, c.yellow());
        }
    }

    /* A part's own model in its bin, turned to be seen from above and aside, turning slowly while it is chosen. */
    private void picture(final GuiGraphics g, final PartCatalogue.Entry entry, final int x0, final int y0,
                         final int x1, final int y1) {
        final List<RocketPartModels.Placed> placed = RocketPartModels.of(this.parts, this.kits,
                RocketStack.of(new RocketStack.Part(entry.id(), 1)), false);
        if (placed.isEmpty()) {
            return;
        }
        final RocketPart part = entry.part();
        final float wide = Math.max(part.diameter(), part.topDiameter());
        final float tall = part.height();
        final float fit = Math.min((x1 - x0) / (wide * 1.45F), (y1 - y0) / (tall + wide * 0.5F));
        final float yaw = entry.id().equals(this.chosen) ? (float) (Util.getMillis() * CARD_SPIN % 360) + CARD_YAW
                : CARD_YAW;
        final PoseStack pose = g.pose();
        g.flush();
        g.enableScissor(Math.round(guiX(x0)), Math.round(guiY(y0)), Math.round(guiX(x1)), Math.round(guiY(y1)));
        pose.pushPose();
        pose.translate((x0 + x1) / 2.0F, (y0 + y1) / 2.0F, CARD_DEPTH);
        pose.scale(fit, -fit, fit);
        pose.mulPose(Axis.XP.rotationDegrees(CARD_PITCH));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(0, -tall / 2, 0);
        RenderSystem.enableDepthTest();
        Lighting.setupForEntityInInventory();
        for (final RocketPartModels.Placed one : placed) {
            final Optional<KitModel> model = KitModels.get(one.model());
            if (model.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0, one.y(), 0);
            KitRenderer.draw(KitAnimations.part(this.terminal, "card:" + one.model(), model.get()),
                    ModelDefinition.Variant.NONE, this.terminal, pose, g.bufferSource(), LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, null, null, false);
            pose.popPose();
        }
        g.flush();
        Lighting.setupFor3DItems();
        pose.popPose();
        g.disableScissor();
    }

    /* The chosen part's spec sheet: its name, tier and mass, its figures on inked bars, a note on what it is for. */
    private void sheet(final GuiGraphics g) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        final int x0 = RocketDesignLayout.SHEET_X0;
        final int y0 = RocketDesignLayout.SHEET_Y0;
        final int x1 = RocketDesignLayout.SHEET_X1;
        RocketDesignArt.paper(g, x0, y0, x1, RocketDesignLayout.SHEET_Y1);
        final Optional<PartCatalogue.Entry> found = sheetEntry();
        if (found.isEmpty()) {
            return;
        }
        final PartCatalogue.Entry entry = found.get();
        final RocketPart part = entry.part();
        final int left = x0 + 18;
        final int width = x1 - 6 - left;
        Draw.text(g, this.font, fit(up(RocketDesignTexts.SPEC_SHEET.with(code(entry))), width), left, y0 + 6,
                c.ink());
        Draw.text(g, this.font, fit(name(part).toUpperCase(Locale.ROOT) + " D" + part.diameter(), width), left,
                y0 + 21, c.ink());
        Draw.text(g, this.font, fit(up(RocketDesignTexts.TIER_MASS.with(part.tier(), tonnes(part.mass()))), width),
                left, y0 + 31, c.inkDim());
        final List<Row> rows = rows(part);
        for (int i = 0; i < rows.size(); i++) {
            final int yy = y0 + 44 + i * 20;
            Draw.text(g, this.font, up(rows.get(i).name()), left, yy, c.ink());
            final String value = up(rows.get(i).value());
            Draw.text(g, this.font, value, x1 - 6 - this.font.width(value), yy, c.ink());
            RocketDesignArt.inkBar(g, left, yy + 10, width, rows.get(i).share());
        }
        int yy = y0 + 46 + rows.size() * 20;
        for (final TextKey note : notes(part)) {
            for (final FormattedCharSequence line : this.font.split(Component.literal(up(note)), width)) {
                if (yy + GLYPH_HEIGHT > RocketDesignLayout.SHEET_Y1 - 2) {
                    return;
                }
                Draw.text(g, this.font, line, left, yy, c.inkDim());
                yy += 10;
            }
        }
    }

    /* The stages, the delta-v, the destinations, the checks, the sign and the view's keys. */
    private void stages(final GuiGraphics g) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        RocketDesignArt.plate(g, this.font, RocketDesignLayout.RIGHT_X0, RocketDesignLayout.PLATE_Y0,
                RocketDesignLayout.RIGHT_X1, RocketDesignLayout.PLATE_Y1, up(RocketDesignTexts.STAGES));
        final int x0 = RocketDesignLayout.INNER_X0;
        final int x1 = RocketDesignLayout.INNER_X1;
        final List<DesignReport.Stage> stages = this.report.stages();
        for (int row = 0; row < RocketDesignLayout.STAGE_ROWS; row++) {
            final int index = stages.size() - 1 - this.stageScroll - row;
            if (index < 0) {
                break;
            }
            stage(g, stages.get(index), RocketDesignLayout.STAGE_Y0 + row * RocketDesignLayout.STAGE_STEP);
        }
        RocketDesignArt.heading(g, this.font, x0, RocketDesignLayout.DELTA_V_Y, up(RocketDesignTexts.DELTA_V), x1);
        RocketDesignArt.well(g, x0, RocketDesignLayout.LED_Y0, x1, RocketDesignLayout.LED_Y1);
        final String total = whole(this.report.deltaV());
        Draw.text(g, this.font, total, x0 + 6, RocketDesignLayout.LED_Y0 + 4, c.led(), c.paintDeep(), 2.0F);
        Draw.text(g, this.font, up(RocketDesignTexts.UNIT_SPEED), x0 + 12 + 2 * this.font.width(total),
                RocketDesignLayout.LED_Y0 + 10, c.led());
        final boolean engines = stages.stream().anyMatch(stage -> stage.engine().isPresent());
        final String ratio = String.format(Locale.ROOT, "%.2f", this.report.liftOff());
        final Text lift = !engines ? RocketDesignTexts.NO_ENGINES.text() : this.report.liftOff() > 1
                ? RocketDesignTexts.LEAVES.with(ratio) : RocketDesignTexts.STAYS.with(ratio);
        Draw.text(g, this.font, fit(up(lift), x1 - x0), x0, RocketDesignLayout.LIFT_Y,
                engines && this.report.liftOff() > 1 ? c.green() : c.lightRed());
        RocketDesignArt.heading(g, this.font, x0, RocketDesignLayout.DESTINATIONS_Y,
                up(RocketDesignTexts.DESTINATIONS), x1);
        final List<DesignDestinations.Reach> reaches = this.report.destinations();
        if (reaches.isEmpty()) {
            Draw.text(g, this.font, fit(up(RocketDesignTexts.NO_ROUTES), x1 - x0), x0, RocketDesignLayout.REACH_Y0,
                    c.dimPaint());
        }
        for (int i = 0; i < Math.min(RocketDesignLayout.REACHES, reaches.size()); i++) {
            reach(g, reaches.get(i), RocketDesignLayout.REACH_Y0 + i * RocketDesignLayout.REACH_STEP);
        }
        RocketDesignArt.heading(g, this.font, x0, RocketDesignLayout.CHECKS_Y, up(RocketDesignTexts.CHECKS), x1);
        final List<DesignReport.Check> checks = this.report.checks();
        final boolean more = checks.size() > RocketDesignLayout.CHECK_ROWS;
        final int shown = more ? RocketDesignLayout.CHECK_ROWS - 1 : checks.size();
        for (int i = 0; i < shown; i++) {
            final DesignReport.Check check = checks.get(i);
            final int yy = RocketDesignLayout.CHECK_Y0 + i * RocketDesignLayout.CHECK_STEP;
            RocketDesignArt.lamp(g, x0 + 1, yy, lampOf(check.severity()), check.severity()
                    != DesignReport.Severity.NOTE);
            Draw.text(g, this.font, fit(up(check.text()), x1 - x0 - 14), x0 + 14, yy, c.stencil());
        }
        if (more) {
            Draw.text(g, this.font, up(RocketDesignTexts.MORE.with(checks.size() - shown)), x0 + 14,
                    RocketDesignLayout.CHECK_Y0 + shown * RocketDesignLayout.CHECK_STEP, c.dimPaint());
        }
        sign(g, this.report.ready());
        RocketDesignArt.heading(g, this.font, x0, RocketDesignLayout.VIEW_Y, up(RocketDesignTexts.THE_VIEW), x1);
        for (int i = 0; i < RocketDesignLayout.KEYS; i++) {
            final int yy = RocketDesignLayout.KEY_Y0 + i * RocketDesignLayout.KEY_STEP;
            final String key = up(RocketDesignTexts.VIEW_KEYS.get(i));
            final int kw = this.font.width(key) + 6;
            g.fill(x0, yy, x0 + kw, yy + 11, c.yellow());
            RocketDesignArt.hline(g, x0, x0 + kw, yy + 10, c.keyShade());
            Draw.text(g, this.font, key, x0 + 3, yy + 2, c.black());
            Draw.text(g, this.font, fit(up(RocketDesignTexts.VIEW_DOES.get(i)), x1 - x0 - kw - 6), x0 + kw + 6,
                    yy + 2, c.dimPaint());
        }
    }

    /* A stage in its recess: its lamp, its number, its engine or what it lacks, its thrust and its delta-v. */
    private void stage(final GuiGraphics g, final DesignReport.Stage stage, final int y) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        final int x0 = RocketDesignLayout.INNER_X0;
        final int x1 = RocketDesignLayout.INNER_X1;
        RocketDesignArt.well(g, x0, y, x1, y + RocketDesignLayout.STAGE_H);
        final boolean engine = stage.engine().isPresent();
        final boolean lacking = engine != stage.propellant();
        final boolean payload = !engine && !stage.propellant();
        RocketDesignArt.lamp(g, x0 + 5, y + 9, lacking ? c.red() : payload ? c.yellow() : c.green(), true);
        Draw.text(g, this.font, up(RocketDesignTexts.STAGE.with(stage.number())), x0 + 18, y + 9, c.stencil());
        final String title = engine ? name(this.parts.get(stage.engine().get())).toUpperCase(Locale.ROOT)
                : up(payload ? RocketDesignTexts.PAYLOAD : RocketDesignTexts.NO_ENGINE_YET);
        final int text = x0 + 40;
        Draw.text(g, this.font, fit(title, x1 - 5 - text), text, y + 5, lacking ? c.lightRed() : c.stencil());
        final String sub = engine ? up(RocketDesignTexts.RATIO.with(String.format(Locale.ROOT, "%.2f",
                stage.ratio()))) : up(RocketDesignTexts.NO_THRUST);
        Draw.text(g, this.font, sub, text, y + 15, lacking ? c.lightRed() : c.dimPaint());
        final String speed = up(RocketDesignTexts.SPEED.with(whole(stage.deltaV())));
        Draw.text(g, this.font, speed, x1 - 5 - this.font.width(speed), y + 15,
                stage.deltaV() > 0 ? c.yellow() : c.lightRed());
    }

    /* A destination: its globe, its name, what it asks, a bar of how far the rocket gets, what it is short by. */
    private void reach(final GuiGraphics g, final DesignDestinations.Reach reach, final int y) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        final int x0 = RocketDesignLayout.INNER_X0;
        final int x1 = RocketDesignLayout.INNER_X1;
        final int globe = reach.world().map(world -> switch (world) {
            case OVERWORLD -> c.orbit();
            case MOON -> c.moon();
            case MARS -> c.mars();
        }).orElse(c.orbit());
        RocketDesignArt.planet(g, x0 + 7, y + 5, 6, globe, reach.world().isEmpty());
        Draw.text(g, this.font, up(reach.name()), x0 + 20, y + 1, c.stencil());
        final String need = up(RocketDesignTexts.SPEED.with(whole(reach.need())));
        Draw.text(g, this.font, need, x1 - this.font.width(need), y + 1, c.dimPaint());
        final double short1 = reach.shortBy(this.report.deltaV());
        RocketDesignArt.segments(g, x0 + 20, y + 11, x1 - x0 - 20, this.report.deltaV() / reach.need(),
                short1 > 0 ? c.red() : c.green(), c.paintDeep(), SEGMENTS);
        final Text line = short1 > 0 ? RocketDesignTexts.SHORT.with(whole(short1))
                : RocketDesignTexts.SPARE.with(whole(this.report.deltaV() - reach.need()));
        Draw.text(g, this.font, fit(up(line), x1 - x0 - 20), x0 + 20, y + 18, short1 > 0 ? c.lightRed() : c.green());
    }

    /* The bay's sign, lit green READY or red NOT READY. */
    private void sign(final GuiGraphics g, final boolean ready) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        final int x0 = RocketDesignLayout.INNER_X0;
        final int x1 = RocketDesignLayout.INNER_X1;
        final int y0 = RocketDesignLayout.STATUS_Y0;
        final int y1 = RocketDesignLayout.STATUS_Y1;
        g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, c.paintDeep());
        g.fill(x0, y0, x1, y1, ready ? c.signGreen() : c.signRed());
        RocketDesignArt.hline(g, x0, x1, y0, ready ? c.signGreenHi() : c.signRedHi());
        RocketDesignArt.hline(g, x0, x1, y1 - 1, ready ? c.signGreenLo() : c.signRedLo());
        Draw.textCentered(g, this.font, up(ready ? RocketDesignTexts.READY : RocketDesignTexts.NOT_READY),
                (x0 + x1) / 2, y0 + 7, ready ? c.signGreenInk() : c.signRedInk());
    }

    /* The push buttons, and what the screen says: the last thing done, or what the mouse does now. */
    private void bottom(final GuiGraphics g) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        RocketDesignArt.plate(g, this.font, RocketDesignLayout.MIDDLE_X0, RocketDesignLayout.BOTTOM_Y0,
                RocketDesignLayout.MIDDLE_X1, RocketDesignLayout.PLATE_Y1, null);
        final int y = RocketDesignLayout.BUTTON_Y0;
        final int h = RocketDesignLayout.BUTTON_H;
        RocketDesignArt.pushButton(g, this.font, RocketDesignLayout.buttonX(0), y, RocketDesignLayout.buttonW(0), h,
                up(RocketDesignTexts.CONFIRM), c.confirm(), this.report.ready(), up(RocketDesignTexts.CONFIRM_KEY));
        RocketDesignArt.pushButton(g, this.font, RocketDesignLayout.buttonX(1), y, RocketDesignLayout.buttonW(1), h,
                up(RocketDesignTexts.SAVE), c.yellow(), false, up(RocketDesignTexts.SAVE_KEY));
        RocketDesignArt.pushButton(g, this.font, RocketDesignLayout.buttonX(2), y, RocketDesignLayout.buttonW(2), h,
                up(RocketDesignTexts.PLANS), c.paintHi(), this.plansOpen, up(RocketDesignTexts.PLANS_KEY));
        RocketDesignArt.pushButton(g, this.font, RocketDesignLayout.buttonX(3), y, RocketDesignLayout.buttonW(3), h,
                up(RocketDesignTexts.CLEAR), c.red(), false, up(RocketDesignTexts.CLEAR_KEY));
        final Text hint = this.said != null && Util.getMillis() < this.saidUntil ? this.said
                : this.held != null ? RocketDesignTexts.HINT_DROP.text() : this.tool.hint().text();
        final int room = RocketDesignLayout.HINT_X1 - RocketDesignLayout.buttonX(3) - RocketDesignLayout.buttonW(3)
                - 6;
        final String words = fit(up(hint), room);
        Draw.text(g, this.font, words, RocketDesignLayout.HINT_X1 - this.font.width(words), y + 5, c.yellow());
    }

    /* The plans' list over the middle: each plan, the one in the bay marked, each with its load and delete. */
    private void plans(final GuiGraphics g) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        final int x0 = RocketDesignLayout.PLANS_X0;
        final int x1 = RocketDesignLayout.PLANS_X1;
        RocketDesignArt.plate(g, this.font, x0, RocketDesignLayout.PLANS_Y0, x1, RocketDesignLayout.PLANS_Y1,
                up(RocketDesignTexts.PLANS));
        final List<RocketDesign> plans = this.terminal.plans();
        if (plans.isEmpty()) {
            Draw.text(g, this.font, up(RocketDesignTexts.NO_PLANS), x0 + 12, RocketDesignLayout.PLAN_Y0 + 4,
                    c.dimPaint());
        }
        final Optional<RocketDesign> inBay = this.terminal.plan();
        for (int i = 0; i < Math.min(plans.size(), RocketDesignLayout.PLAN_ROWS); i++) {
            final RocketDesign plan = plans.get(i);
            final int y = RocketDesignLayout.PLAN_Y0 + i * RocketDesignLayout.PLAN_STEP;
            RocketDesignArt.well(g, x0 + 8, y, x1 - 8, y + 16);
            Draw.text(g, this.font, fit(plan.name().toUpperCase(Locale.ROOT), 120), x0 + 12, y + 4, c.stencil());
            final String counts = up(counts(RocketStages.stages(this.parts, plan.stack()).size(),
                    RocketStages.height(this.parts, plan.stack())));
            Draw.text(g, this.font, counts, x0 + 136, y + 4, c.dimPaint());
            if (inBay.isPresent() && inBay.get().equals(plan)) {
                Draw.text(g, this.font, up(RocketDesignTexts.IN_THE_BAY), x0 + 136 + this.font.width(counts) + 6,
                        y + 4, c.green());
            }
            final int load = x1 - 10 - 2 * RocketDesignLayout.PLAN_BUTTON_W - 4;
            RocketDesignArt.pushButton(g, this.font, load, y + 2, RocketDesignLayout.PLAN_BUTTON_W, 12,
                    up(RocketDesignTexts.LOAD), c.yellow(), true, "");
            RocketDesignArt.pushButton(g, this.font, load + RocketDesignLayout.PLAN_BUTTON_W + 4, y + 2,
                    RocketDesignLayout.PLAN_BUTTON_W, 12, up(RocketDesignTexts.DELETE), c.red(), true, "");
        }
    }

    /* The nodes over the view while a part is held, the one it will snap to large and tagged; the root's tag. */
    private void nodes(final GuiGraphics g) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        final OrbitView seen = this.view;
        if (seen == null) {
            return;
        }
        if (this.held != null) {
            final List<Vec3> points = nodePoints();
            for (int k = 0; k < points.size(); k++) {
                final Optional<Vec2> at = seen.project(points.get(k));
                if (at.isEmpty()) {
                    continue;
                }
                final boolean snapping = this.target.kind() != Target.Kind.BIN && this.target.kind()
                        != Target.Kind.NONE && this.target.place() == k;
                g.pose().pushPose();
                g.pose().translate(at.get().x, at.get().y, 0);
                g.pose().scale(this.scale, this.scale, 1);
                RocketDesignArt.node(g, 0, 0, snapping);
                if (snapping) {
                    final String words = up(RocketDesignTexts.SNAPS_HERE);
                    g.fill(7, -5, 14 + this.font.width(words), 6, c.yellow());
                    Draw.text(g, this.font, words, 10, -4, c.black());
                }
                g.pose().popPose();
            }
        }
        if (this.tool == Tool.ROOT && !this.design.stack().sections().isEmpty()) {
            final List<AABB> boxes = boxes();
            final AABB box = boxes.get(Math.clamp(this.root, 0, boxes.size() - 1));
            seen.project(box.getCenter()).ifPresent(at -> {
                g.pose().pushPose();
                g.pose().translate(at.x, at.y, 0);
                g.pose().scale(this.scale, this.scale, 1);
                final String words = up(RocketDesignTexts.ROOT_TAG);
                g.fill(-2, -2, this.font.width(words) + 2, 9, c.yellow());
                Draw.text(g, this.font, words, 0, 0, c.black());
                g.pose().popPose();
            });
        }
    }

    /* ---------------------------------------------------------------------------------------------- the mouse */

    private boolean clickTop(final double x, final double y) {
        if (onName(x, y)) {
            this.naming = new StringBuilder(this.design.name());
            return true;
        }
        if (y < RocketDesignLayout.TOOLS_Y0 || y >= RocketDesignLayout.TOOLS_Y0 + RocketDesignLayout.TOOL_H) {
            return false;
        }
        for (int i = 0; i < Tool.BUTTONS; i++) {
            final int x0 = toolX(i);
            if (x >= x0 && x < x0 + RocketDesignArt.toolWidth(this.font, toolLabel(i))) {
                if (i < Tool.ORDER.size()) {
                    this.tool = Tool.ORDER.get(i);
                } else if (i == Tool.MIRROR_BUTTON) {
                    this.mirror = this.mirror % DesignEdits.MOST_MIRROR + 1;
                } else {
                    this.snap = !this.snap;
                }
                return true;
            }
        }
        return false;
    }

    private boolean clickBins(final double x, final double y) {
        if (y >= RocketDesignLayout.TAB_Y0 && y < RocketDesignLayout.TAB_Y0 + RocketDesignLayout.TAB_SIZE) {
            for (int i = 0; i < RocketDesignLayout.TABS; i++) {
                final int tx = RocketDesignLayout.TAB_X0 + i * RocketDesignLayout.TAB_STEP;
                if (x >= tx && x < tx + RocketDesignLayout.TAB_SIZE) {
                    this.category = PartCatalogue.CATEGORIES.get(i);
                    this.binScroll = 0;
                    return true;
                }
            }
        }
        final List<Integer> sizes = diameters();
        if (y >= RocketDesignLayout.CATEGORY_Y && y < RocketDesignLayout.CATEGORY_Y + RocketDesignLayout.DIAMETER_H) {
            for (int i = 0; i < sizes.size(); i++) {
                final int x1 = RocketDesignLayout.DIAMETERS_X1 - i * RocketDesignLayout.DIAMETER_STEP;
                if (x >= x1 - RocketDesignLayout.DIAMETER_W && x < x1) {
                    final int size = sizes.get(sizes.size() - 1 - i);
                    if (!this.hiddenDiameters.remove(size)) {
                        this.hiddenDiameters.add(size);
                    }
                    this.binScroll = 0;
                    return true;
                }
            }
        }
        final List<PartCatalogue.Entry> shelf = shelf();
        for (int i = 0; i < RocketDesignLayout.CARD_COLUMNS * RocketDesignLayout.CARD_ROWS; i++) {
            final int index = this.binScroll * RocketDesignLayout.CARD_COLUMNS + i;
            final int x0 = RocketDesignLayout.cardX(i);
            final int y0 = RocketDesignLayout.cardY(i);
            if (index < shelf.size() && x >= x0 && x < x0 + RocketDesignLayout.CARD_W && y >= y0
                    && y < y0 + RocketDesignLayout.CARD_H) {
                final PartCatalogue.Entry entry = shelf.get(index);
                this.chosen = entry.id();
                this.held = new Held(List.of(new RocketStack.Section(new RocketStack.Part(entry.id(), 1),
                        List.of())), List.of(), this.design, this.root, -1, false);
                return true;
            }
        }
        return false;
    }

    private boolean clickBottom(final double x, final double y) {
        if (y < RocketDesignLayout.BUTTON_Y0 || y >= RocketDesignLayout.BUTTON_Y0 + RocketDesignLayout.BUTTON_H) {
            return false;
        }
        for (int i = 0; i < 4; i++) {
            final int x0 = RocketDesignLayout.buttonX(i);
            if (x >= x0 && x < x0 + RocketDesignLayout.buttonW(i)) {
                switch (i) {
                    case 0 -> confirm();
                    case 1 -> save();
                    case 2 -> this.plansOpen = !this.plansOpen;
                    default -> clear();
                }
                return true;
            }
        }
        return false;
    }

    private void clickPlans(final double x, final double y) {
        if (x < RocketDesignLayout.PLANS_X0 || x >= RocketDesignLayout.PLANS_X1 || y < RocketDesignLayout.PLANS_Y0
                || y >= RocketDesignLayout.PLANS_Y1) {
            this.plansOpen = false;
            return;
        }
        final List<RocketDesign> plans = this.terminal.plans();
        final int load = RocketDesignLayout.PLANS_X1 - 10 - 2 * RocketDesignLayout.PLAN_BUTTON_W - 4;
        for (int i = 0; i < Math.min(plans.size(), RocketDesignLayout.PLAN_ROWS); i++) {
            final int top = RocketDesignLayout.PLAN_Y0 + i * RocketDesignLayout.PLAN_STEP + 2;
            if (y < top || y >= top + 12) {
                continue;
            }
            if (x >= load && x < load + RocketDesignLayout.PLAN_BUTTON_W) {
                commit(plans.get(i));
                this.root = 0;
                this.plansOpen = false;
                say(RocketDesignTexts.LOADED.text());
            } else if (x >= load + RocketDesignLayout.PLAN_BUTTON_W + 4
                    && x < load + 2 * RocketDesignLayout.PLAN_BUTTON_W + 4) {
                RocketDesigner.DELETE.send(i);
            }
        }
    }

    /* A click on the rocket in the view, as the tool in use does it. */
    private boolean clickWorld(final double mouseX, final double mouseY) {
        final Hit hit = pick(mouseX, mouseY);
        if (hit.place() < 0) {
            return true;
        }
        final RocketStack stack = this.design.stack();
        switch (this.tool) {
            case PLACE -> {
                if (hit.booster()) {
                    final List<List<RocketStack.Part>> boosters = stack.sections().get(hit.place()).boosters();
                    this.held = new Held(List.of(), boosters, this.design, this.root, -1, true);
                    this.design = this.design.with(DesignEdits.unhang(stack, hit.place()));
                } else {
                    final DesignEdits.Picked picked = DesignEdits.pick(stack, hit.place(), this.root);
                    // The whole rocket taken keeps its root where it was among its parts.
                    final int rootInRun = picked.left().sections().isEmpty() ? this.root : -1;
                    this.held = new Held(picked.taken(), List.of(), this.design, this.root, rootInRun, true);
                    this.design = this.design.with(picked.left());
                    this.root = picked.root();
                }
                recount();
            }
            case OFFSET -> {
                final RocketPart part = this.parts.get(stack.sections().get(hit.place()).part().id());
                if (part != null && part.stretches()) {
                    this.stretched = hit.place();
                    this.stretchFrom = mouseY;
                    this.stretchLayers = stack.sections().get(hit.place()).part().layers();
                    this.stretchTo = this.stretchLayers;
                } else {
                    say(RocketDesignTexts.ONE_SIZE.text());
                }
            }
            case ROTATE -> commit(this.design.with(DesignEdits.turn(stack, hit.place())));
            case ROOT -> this.root = hit.place();
        }
        return true;
    }

    /* Lets go of what is held: onto the node it snaps to, onto the bins to throw it away, or back where it was. */
    private void drop() {
        final Held letGo = this.held;
        this.held = null;
        if (letGo == null) {
            return;
        }
        final RocketStack stack = this.design.stack();
        switch (this.target.kind()) {
            case NODE -> {
                final int node = this.target.place();
                final int before = this.root;
                commitFrom(letGo.before(), this.design.with(DesignEdits.insert(stack, node, letGo.run())));
                this.root = letGo.rootInRun() >= 0 ? node + letGo.rootInRun()
                        : stack.sections().isEmpty() ? 0 : node <= before ? before + letGo.run().size() : before;
            }
            case SIDE -> {
                final int place = this.target.place();
                RocketStack hung = DesignEdits.unhang(stack, place);
                if (!letGo.boosters().isEmpty()) {
                    for (final List<RocketStack.Part> booster : letGo.boosters()) {
                        hung = hangOne(hung, place, booster);
                    }
                } else {
                    hung = DesignEdits.hang(stack, place, letGo.run().get(0).part(), this.mirror);
                }
                commitFrom(letGo.before(), this.design.with(hung));
            }
            case BIN -> {
                if (letGo.fromRocket()) {
                    commitFrom(letGo.before(), this.design);
                }
            }
            case NONE -> {
                if (letGo.fromRocket()) {
                    this.design = letGo.before();
                    this.root = letGo.rootBefore();
                    recount();
                }
            }
        }
    }

    /* ---------------------------------------------------------------------------------------------- the actions */

    private void confirm() {
        if (!this.report.ready()) {
            say(RocketDesignTexts.NOT_SENT.text());
            return;
        }
        RocketDesigner.CONFIRM.send(true);
        say(RocketDesignTexts.SENT.text());
    }

    private void save() {
        if (this.design.name().isBlank()) {
            say(RocketDesignTexts.NAME_FIRST.text());
            this.naming = new StringBuilder();
            return;
        }
        final List<RocketDesign> plans = this.terminal.plans();
        if (plans.size() >= HangarTerminalBlockEntity.MOST_PLANS
                && plans.stream().noneMatch(plan -> plan.name().equals(this.design.name()))) {
            say(RocketDesignTexts.FULL.with(HangarTerminalBlockEntity.MOST_PLANS));
            return;
        }
        RocketDesigner.SAVE.send(true);
        say(RocketDesignTexts.SAVED.text());
    }

    private void clear() {
        if (this.held != null) {
            this.target = new Target(Target.Kind.BIN, -1);
            drop();
            return;
        }
        commit(RocketDesign.EMPTY.named(this.design.name()));
        this.root = 0;
        say(RocketDesignTexts.CLEARED.text());
    }

    private void undo() {
        if (!this.undone.isEmpty()) {
            this.redone.push(this.design);
            this.design = this.undone.pop();
            this.root = Math.min(this.root, Math.max(0, this.design.stack().sections().size() - 1));
            changed();
        }
    }

    private void redo() {
        if (!this.redone.isEmpty()) {
            this.undone.push(this.design);
            this.design = this.redone.pop();
            this.root = Math.min(this.root, Math.max(0, this.design.stack().sections().size() - 1));
            changed();
        }
    }

    /* Turns what is held a quarter turn, each part of it. */
    private void turnHeld() {
        final Held now = this.held;
        if (now == null || now.run().isEmpty()) {
            return;
        }
        RocketStack run = new RocketStack(now.run());
        for (int k = 0; k < run.sections().size(); k++) {
            run = DesignEdits.turn(run, k);
        }
        this.held = new Held(run.sections(), now.boosters(), now.before(), now.rootBefore(), now.rootInRun(),
                now.fromRocket());
    }

    private void finishNaming(final boolean keep) {
        final StringBuilder typed = this.naming;
        this.naming = null;
        if (keep && typed != null && !typed.toString().isBlank()) {
            commit(this.design.named(typed.toString().strip().toUpperCase(Locale.ROOT)));
        }
    }

    private void commit(final RocketDesign to) {
        commitFrom(this.design, to);
    }

    /* Sets {@code to} as the design, {@code before} kept to undo back to, and sends it to the hangar. */
    private void commitFrom(final RocketDesign before, final RocketDesign to) {
        if (!to.bounded()) {
            this.design = before;
            recount();
            return;
        }
        this.undone.push(before);
        while (this.undone.size() > MOST_UNDO) {
            this.undone.removeLast();
        }
        this.redone.clear();
        this.design = to;
        changed();
    }

    private void changed() {
        recount();
        RocketDesigner.DESIGN.send(this.design);
        if (this.report.height() != this.framed) {
            frame();
        }
    }

    /* Turns the view round the middle of the rocket as it stands now, so a rocket grown taller stays in sight. */
    private void frame() {
        final Optional<HangarBay> bay = this.terminal.bay();
        if (this.view != null && bay.isPresent()) {
            this.framed = this.report.height();
            this.view.aim(Vec3.atBottomCenterOf(bay.get().pad()).add(0, Math.max(LEAST_HEIGHT, this.framed) / 2.0,
                    0));
        }
    }

    /* Works the design's figures out again, on the world the hangar stands in. */
    private void recount() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        final DimensionRules rules = DimensionRulesData.of(this.minecraft.level);
        final Optional<SolarBody> from = FlightComputerBlockEntity.here(this.minecraft.level);
        this.report = DesignReport.of(this.parts, this.design.stack(), rules.gravity(),
                StageMath.air(rules.pressure()), from, this.terminal.bay());
        this.stageScroll = Math.min(this.stageScroll, Math.max(0, this.report.stages().size()
                - RocketDesignLayout.STAGE_ROWS));
    }

    private void say(final Text words) {
        this.said = words;
        this.saidUntil = Util.getMillis() + SAID_MILLIS;
    }

    /* ---------------------------------------------------------------------------------------------- the view */

    /* What the hangar shows this frame: the rocket, a part held where it will go or under the mouse, the outline. */
    private void scene(final int mouseX, final int mouseY) {
        final Optional<HangarBay> bay = this.terminal.bay();
        if (this.view == null || bay.isEmpty()) {
            return;
        }
        final boolean onPlate = RocketDesignLayout.onPlate(frameX(mouseX), frameY(mouseY)) || this.plansOpen;
        this.hovered = this.held == null && this.stretched < 0 && !onPlate ? pick(mouseX, mouseY) : Hit.NONE;
        this.target = this.held == null ? Target.NONE : target(mouseX, mouseY);
        RocketStack stack = this.design.stack();
        Set<Integer> ghosts = Set.of();
        Optional<RocketDesignScene.Loose> loose = Optional.empty();
        Optional<Integer> outlined = this.hovered.place() >= 0 ? Optional.of(this.hovered.place()) : Optional.empty();
        if (this.stretched >= 0) {
            stack = DesignEdits.stretch(stack, this.stretched, this.stretchTo);
            outlined = Optional.of(this.stretched);
        }
        final Held now = this.held;
        if (now != null) {
            if (this.target.kind() == Target.Kind.NODE) {
                stack = DesignEdits.insert(stack, this.target.place(), now.run());
                final Set<Integer> placing = new HashSet<>();
                for (int k = 0; k < now.run().size(); k++) {
                    placing.add(this.target.place() + k);
                }
                ghosts = Set.copyOf(placing);
            } else if (this.target.kind() == Target.Kind.SIDE) {
                outlined = Optional.of(this.target.place());
            } else if (!now.run().isEmpty() && this.target.kind() == Target.Kind.NONE) {
                loose = free(mouseX, mouseY, now.run()).map(foot -> new RocketDesignScene.Loose(now.run(), foot));
            }
        }
        RocketDesignScene.show(new RocketDesignScene.Shown(this.terminal, bay.get().pad(), this.parts, this.kits,
                stack, ghosts, loose, outlined, Math.max(this.report.width(), 1)));
    }

    /* The part of the rocket under the mouse, and whether it is a booster hung at its side. */
    private Hit pick(final double mouseX, final double mouseY) {
        final OrbitView seen = this.view;
        final Optional<HangarBay> bay = this.terminal.bay();
        if (seen == null || bay.isEmpty()) {
            return Hit.NONE;
        }
        final Optional<OrbitView.Ray> ray = seen.ray(mouseX, mouseY);
        if (ray.isEmpty()) {
            return Hit.NONE;
        }
        final Vec3 from = ray.get().from();
        final Vec3 to = ray.get().at(REACH);
        Hit best = Hit.NONE;
        double nearest = Double.MAX_VALUE;
        final List<AABB> boxes = boxes();
        for (int k = 0; k < boxes.size(); k++) {
            final Optional<Vec3> on = boxes.get(k).clip(from, to);
            if (on.isPresent() && on.get().distanceToSqr(from) < nearest) {
                nearest = on.get().distanceToSqr(from);
                best = new Hit(k, false);
            }
            for (final AABB booster : RocketDesignScene.boosterBoxes(this.parts, this.design.stack().sections()
                    .get(k), boxes.get(k))) {
                final Optional<Vec3> onBooster = booster.clip(from, to);
                if (onBooster.isPresent() && onBooster.get().distanceToSqr(from) < nearest) {
                    nearest = onBooster.get().distanceToSqr(from);
                    best = new Hit(k, true);
                }
            }
        }
        return best;
    }

    /* Where what is held would go if let go now: the nearest node in reach, the bins, or nowhere. */
    private Target target(final double mouseX, final double mouseY) {
        final double x = frameX(mouseX);
        final double y = frameY(mouseY);
        if (x >= RocketDesignLayout.LEFT_X0 && x < RocketDesignLayout.LEFT_X1 && y >= RocketDesignLayout.PLATE_Y0
                && y < RocketDesignLayout.PLATE_Y1) {
            return new Target(Target.Kind.BIN, -1);
        }
        final OrbitView seen = this.view;
        if (seen == null || RocketDesignLayout.onPlate(x, y)) {
            return Target.NONE;
        }
        final double reach = this.snap ? SNAP_REACH : FREE_REACH;
        final List<Vec3> points = nodePoints();
        int best = -1;
        double nearest = reach * reach;
        for (int k = 0; k < points.size(); k++) {
            final Optional<Vec2> at = seen.project(points.get(k));
            if (at.isPresent()) {
                final double dx = at.get().x - mouseX;
                final double dy = at.get().y - mouseY;
                if (dx * dx + dy * dy <= nearest) {
                    nearest = dx * dx + dy * dy;
                    best = k;
                }
            }
        }
        if (best < 0) {
            return Target.NONE;
        }
        return new Target(hangs() ? Target.Kind.SIDE : Target.Kind.NODE, best);
    }

    /* The nodes what is held can go to: the side of each part for boosters, else under each part and on the top. */
    private List<Vec3> nodePoints() {
        final List<AABB> boxes = boxes();
        final List<Vec3> points = new ArrayList<>();
        final Optional<HangarBay> bay = this.terminal.bay();
        if (bay.isEmpty()) {
            return points;
        }
        final Vec3 axis = Vec3.atBottomCenterOf(bay.get().pad());
        if (hangs()) {
            final Vec3 eye = this.view == null ? axis : this.view.eye();
            final Vec3 toward = new Vec3(eye.x - axis.x, 0, eye.z - axis.z);
            final Vec3 side = toward.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : toward.normalize();
            for (final AABB box : boxes) {
                points.add(box.getCenter().add(side.scale(box.getXsize() / 2 + 0.5)));
            }
            return points;
        }
        for (final AABB box : boxes) {
            points.add(new Vec3(axis.x, box.minY, axis.z));
        }
        points.add(new Vec3(axis.x, boxes.isEmpty() ? axis.y : boxes.get(boxes.size() - 1).maxY, axis.z));
        return points;
    }

    /* Whether what is held hangs at a part's side: boosters taken off, or a booster from its bin. */
    private boolean hangs() {
        final Held now = this.held;
        if (now == null) {
            return false;
        }
        if (!now.boosters().isEmpty()) {
            return true;
        }
        if (now.run().size() != 1) {
            return false;
        }
        final RocketPart part = this.parts.get(now.run().get(0).part().id());
        return part != null && PartCatalogue.hangs(part.kind());
    }

    /*
     * Where parts held away from any node stand: on the upright plane through the rocket's axis facing the camera,
     * under the mouse, their middle on it.
     */
    private Optional<Vec3> free(final double mouseX, final double mouseY, final List<RocketStack.Section> run) {
        final OrbitView seen = this.view;
        final Optional<HangarBay> bay = this.terminal.bay();
        if (seen == null || bay.isEmpty()) {
            return Optional.empty();
        }
        final Optional<OrbitView.Ray> ray = seen.ray(mouseX, mouseY);
        if (ray.isEmpty()) {
            return Optional.empty();
        }
        final Vec3 axis = Vec3.atBottomCenterOf(bay.get().pad());
        final Vec3 facing = new Vec3(seen.eye().x - axis.x, 0, seen.eye().z - axis.z);
        if (facing.lengthSqr() < 1e-6) {
            return Optional.empty();
        }
        final Vec3 normal = facing.normalize();
        final double across = ray.get().way().dot(normal);
        final double distance = Math.abs(across) < 1e-6 ? seen.distance()
                : axis.subtract(ray.get().from()).dot(normal) / across;
        final Vec3 middle = ray.get().at(Math.max(1, distance));
        final double tall = RocketStages.height(this.parts, new RocketStack(run));
        return Optional.of(middle.subtract(0, tall / 2, 0));
    }

    private List<AABB> boxes() {
        final Optional<HangarBay> bay = this.terminal.bay();
        return bay.isEmpty() ? List.of() : RocketDesignScene.boxes(this.parts, this.design.stack(),
                bay.get().pad());
    }

    /* ---------------------------------------------------------------------------------------------- the words */

    /* The parts of the open tab, of the diameters shown. */
    private List<PartCatalogue.Entry> shelf() {
        return this.catalogue.stream().filter(entry -> entry.category() == this.category
                && !this.hiddenDiameters.contains(entry.part().diameter())).toList();
    }

    /* The diameters the open tab's parts come in, smallest first. */
    private List<Integer> diameters() {
        final TreeSet<Integer> sizes = new TreeSet<>();
        for (final PartCatalogue.Entry entry : this.catalogue) {
            if (entry.category() == this.category) {
                sizes.add(entry.part().diameter());
            }
        }
        return List.copyOf(sizes);
    }

    /* The part on the spec sheet: the chosen one, else the open tab's first. */
    private Optional<PartCatalogue.Entry> sheetEntry() {
        for (final PartCatalogue.Entry entry : this.catalogue) {
            if (entry.id().equals(this.chosen)) {
                return Optional.of(entry);
            }
        }
        final List<PartCatalogue.Entry> shelf = shelf();
        return shelf.isEmpty() ? Optional.empty() : Optional.of(shelf.get(0));
    }

    /* A part's figures for its sheet, each on a bar against the largest of the catalogue's. */
    private List<Row> rows(final RocketPart part) {
        final List<Row> rows = new ArrayList<>();
        if (part.engine().isPresent()) {
            final RocketPart.Engine engine = part.engine().get();
            final double seaThrust = engine.thrust() * engine.seaLevelIsp() / engine.vacuumIsp();
            final double thrust = most(p -> p.engine().map(RocketPart.Engine::thrust).orElse(0.0));
            final double isp = most(p -> p.engine().map(RocketPart.Engine::vacuumIsp).orElse(0.0));
            rows.add(new Row(RocketDesignTexts.THRUST_VACUUM, RocketDesignTexts.KILONEWTONS.with(whole(
                    engine.thrust() / KILO)), engine.thrust() / thrust));
            rows.add(new Row(RocketDesignTexts.THRUST_SEA, RocketDesignTexts.KILONEWTONS.with(whole(seaThrust / KILO)),
                    seaThrust / thrust));
            rows.add(new Row(RocketDesignTexts.ISP_VACUUM, RocketDesignTexts.SECONDS.with(whole(engine.vacuumIsp())),
                    engine.vacuumIsp() / isp));
            rows.add(new Row(RocketDesignTexts.ISP_SEA, RocketDesignTexts.SECONDS.with(whole(engine.seaLevelIsp())),
                    engine.seaLevelIsp() / isp));
            return rows;
        }
        rows.add(new Row(RocketDesignTexts.MASS, part.stretches() ? RocketDesignTexts.PER_LAYER.with(tonnes(
                part.mass())) : RocketDesignTexts.TONNES.with(tonnes(part.mass())), part.mass()
                / most(RocketPart::mass)));
        if (part.propellant() > 0) {
            rows.add(new Row(RocketDesignTexts.PROPELLANT, part.stretches() ? RocketDesignTexts.PER_LAYER.with(
                    tonnes(part.propellant())) : RocketDesignTexts.TONNES.with(tonnes(part.propellant())),
                    part.propellant() / most(RocketPart::propellant)));
        }
        if (part.seats() > 0) {
            rows.add(new Row(RocketDesignTexts.SEATS, Text.literal(String.valueOf(part.seats())), part.seats()
                    / most(p -> (double) p.seats())));
        }
        rows.add(new Row(RocketDesignTexts.HEIGHT, RocketDesignTexts.BLOCKS.with(part.height()), part.height()
                / most(p -> (double) p.height())));
        return rows;
    }

    /* What a part's sheet notes of it. */
    private static List<TextKey> notes(final RocketPart part) {
        final List<TextKey> notes = new ArrayList<>();
        part.engine().ifPresent(engine -> notes.add(engine.seaLevelIsp() < VACUUM_NOZZLE * engine.vacuumIsp()
                ? RocketDesignTexts.NOTE_VACUUM : RocketDesignTexts.NOTE_SEA));
        if (part.stretches()) {
            notes.add(RocketDesignTexts.NOTE_STRETCH);
        }
        if (part.kind().endsStage()) {
            notes.add(RocketDesignTexts.NOTE_STAGE);
        }
        if (part.seats() > 0) {
            notes.add(RocketDesignTexts.NOTE_CREW);
        }
        if (part.kind() == RocketPart.Kind.LANDING_LEGS) {
            notes.add(RocketDesignTexts.NOTE_LEGS);
        }
        return notes;
    }

    /* The largest of a figure over the catalogue, never nothing. */
    private double most(final ToDoubleFunction<RocketPart> figure) {
        double most = 0;
        for (final PartCatalogue.Entry entry : this.catalogue) {
            most = Math.max(most, figure.applyAsDouble(entry.part()));
        }
        return most <= 0 ? 1 : most;
    }

    /* The figure a bin's card carries: an engine's best impulse, a tank's propellant, else the part's mass. */
    private static String figure(final RocketPart part) {
        if (part.engine().isPresent()) {
            final RocketPart.Engine engine = part.engine().get();
            final double isp = engine.seaLevelIsp() < VACUUM_NOZZLE * engine.vacuumIsp() ? engine.vacuumIsp()
                    : engine.seaLevelIsp();
            return up(RocketDesignTexts.SECONDS.with(whole(isp)));
        }
        return up(RocketDesignTexts.TONNES.with(tonnes(part.propellant() > 0 ? part.propellant() : part.mass())));
    }

    /* What a bin's card says a part is: an engine's propellant, else the part's own name. */
    private String label(final PartCatalogue.Entry entry) {
        final Optional<RocketPart.Engine> engine = entry.part().engine();
        if (engine.isPresent() && this.minecraft != null && this.minecraft.level != null) {
            final Optional<Propellant> burnt = Propellants.get(this.minecraft.level, engine.get().propellant());
            if (burnt.isPresent()) {
                return Component.translatable(burnt.get().name()).getString();
            }
        }
        return name(entry.part());
    }

    /* How many stages a rocket has and how tall it stands: one stage said as one. */
    private static Text counts(final int stages, final int height) {
        return stages == 1 ? RocketDesignTexts.COUNT_ONE.with(height) : RocketDesignTexts.COUNTS.with(stages,
                height);
    }

    private static String code(final PartCatalogue.Entry entry) {
        return String.format(Locale.ROOT, "C-%02d", entry.number());
    }

    private static String name(@Nullable final RocketPart part) {
        return part == null ? "" : Component.translatable(part.name()).getString();
    }

    private int toolX(final int i) {
        int x = RocketDesignLayout.TOOLS_X0;
        for (int k = 0; k < i; k++) {
            x += RocketDesignArt.toolWidth(this.font, toolLabel(k)) + RocketDesignLayout.TOOL_GAP;
        }
        return x;
    }

    private String toolLabel(final int i) {
        if (i < Tool.ORDER.size()) {
            return up(Tool.ORDER.get(i).label());
        }
        return i == Tool.MIRROR_BUTTON ? up(RocketDesignTexts.MIRROR.with(this.mirror)) : up(RocketDesignTexts.SNAP);
    }

    private int toolIcon(final int i) {
        if (i < Tool.ORDER.size()) {
            return Tool.ORDER.get(i).icon();
        }
        return i == Tool.MIRROR_BUTTON ? RocketDesignArt.MIRROR : RocketDesignArt.SNAP;
    }

    private boolean toolOn(final int i) {
        if (i < Tool.ORDER.size()) {
            return Tool.ORDER.get(i) == this.tool;
        }
        return i == Tool.MIRROR_BUTTON ? this.mirror > 1 : this.snap;
    }

    private int lampOf(final DesignReport.Severity severity) {
        final RocketDesignArt.Colours c = RocketDesignArt.PALETTE.get();
        return switch (severity) {
            case FAULT -> c.red();
            case WARNING -> c.yellow();
            case FINE, NOTE -> c.green();
        };
    }

    private boolean onName(final double x, final double y) {
        return x >= RocketDesignLayout.NAME_X0 && x < RocketDesignLayout.NAME_X1 && y >= RocketDesignLayout.SIGN_Y0
                && y < RocketDesignLayout.SIGN_Y1;
    }

    private String fit(final String words, final int width) {
        return this.font.plainSubstrByWidth(words, Math.max(0, width));
    }

    /* The end of {@code words} that fits {@code width}, as a field shows what is typed last. */
    private String tail(final String words, final int width) {
        String shown = words;
        while (!shown.isEmpty() && this.font.width(shown) > width) {
            shown = shown.substring(1);
        }
        return shown;
    }

    private double frameX(final double x) {
        return (x - this.originX) / this.scale;
    }

    private double frameY(final double y) {
        return (y - this.originY) / this.scale;
    }

    private float guiX(final double x) {
        return this.originX + (float) x * this.scale;
    }

    private float guiY(final double y) {
        return this.originY + (float) y * this.scale;
    }

    private static String up(final TextKey key) {
        return GameText.resolve(key).toUpperCase(Locale.ROOT);
    }

    private static String up(final Text text) {
        return GameText.resolve(text).toUpperCase(Locale.ROOT);
    }

    private static String whole(final double value) {
        return String.format(Locale.ROOT, "%,d", Math.round(value));
    }

    private static String tonnes(final double kilograms) {
        return String.format(Locale.ROOT, "%.1f", kilograms / KILO);
    }

    /* Hangs one booster of {@code segments} round the part at {@code place}, beside those it has. */
    private static RocketStack hangOne(final RocketStack stack, final int place,
                                       final List<RocketStack.Part> segments) {
        final List<RocketStack.Section> sections = new ArrayList<>(stack.sections());
        final RocketStack.Section section = sections.get(place);
        final List<List<RocketStack.Part>> boosters = new ArrayList<>(section.boosters());
        boosters.add(segments);
        sections.set(place, new RocketStack.Section(section.part(), boosters));
        return new RocketStack(sections);
    }

    /** What a click on the rocket does. */
    private enum Tool {
        PLACE(RocketDesignArt.PLACE, RocketDesignTexts.PLACE, RocketDesignTexts.HINT_PLACE),
        OFFSET(RocketDesignArt.OFFSET, RocketDesignTexts.OFFSET, RocketDesignTexts.HINT_OFFSET),
        ROTATE(RocketDesignArt.ROTATE, RocketDesignTexts.ROTATE, RocketDesignTexts.HINT_ROTATE),
        ROOT(RocketDesignArt.ROOT, RocketDesignTexts.ROOT, RocketDesignTexts.HINT_ROOT);

        /** The tools, in the order of their buttons, the mirror's and the snap's after them. */
        static final List<Tool> ORDER = List.of(PLACE, OFFSET, ROTATE, ROOT);
        static final int MIRROR_BUTTON = 4;
        static final int BUTTONS = 6;

        private final int icon;
        private final TextKey label;
        private final TextKey hint;

        Tool(final int icon, final TextKey label, final TextKey hint) {
            this.icon = icon;
            this.label = label;
            this.hint = hint;
        }

        int icon() {
            return this.icon;
        }

        TextKey label() {
            return this.label;
        }

        TextKey hint() {
            return this.hint;
        }
    }

    /**
     * What is held: parts taken from the bins or off the rocket, or the boosters taken off a part.
     *
     * @param run        the parts, from the bottom up; none for boosters
     * @param boosters   the boosters taken off a part, each its segments; none for parts
     * @param before     the design before it was taken, to put back
     * @param rootBefore the root's place before it was taken
     * @param rootInRun  the root's place among the parts held, when they took the root with them; -1 otherwise
     * @param fromRocket whether it was taken off the rocket, rather than from the bins
     */
    private record Held(List<RocketStack.Section> run, List<List<RocketStack.Part>> boosters, RocketDesign before,
                        int rootBefore, int rootInRun, boolean fromRocket) {
    }

    /**
     * A part of the rocket under the mouse.
     *
     * @param place   its place in the stack, from the bottom; -1 for none
     * @param booster whether it is a booster hung at that part's side
     */
    private record Hit(int place, boolean booster) {

        static final Hit NONE = new Hit(-1, false);
    }

    /**
     * Where what is held would go.
     *
     * @param kind  a node of the stack, a part's side, the bins, or nowhere
     * @param place the node's or the part's place
     */
    private record Target(Kind kind, int place) {

        static final Target NONE = new Target(Kind.NONE, -1);

        enum Kind {
            NODE,
            SIDE,
            BIN,
            NONE
        }
    }

    /**
     * A figure on the spec sheet.
     *
     * @param name  what it is
     * @param value how much
     * @param share how far along its bar, against the catalogue's largest
     */
    private record Row(TextKey name, Text value, double share) {
    }
}
