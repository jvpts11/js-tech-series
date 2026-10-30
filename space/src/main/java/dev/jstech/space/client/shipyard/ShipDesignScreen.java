/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.client.shipyard;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.space.JsSpace;
import dev.jstech.space.api.ShipClass;
import dev.jstech.space.api.ShipFinishes;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipMaker;
import dev.jstech.space.api.ShipMakers;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipPartKind;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.api.ShipPlace;
import dev.jstech.space.gui.ShipDesignLayout;
import dev.jstech.space.ship.ShipChecks;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.ShipLayout;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.space.ship.SmallShipNotes;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipDesignMenu;
import dev.jstech.space.shipyard.ShipDesigner;
import dev.jstech.space.shipyard.ShipyardLayout;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The Starship Design, a formed Small Shipyard's builder screen, as the approved design draws it: the ship in 3D on its
 * dotted floor through the middle, panels of dark glass round it. A part is dragged from its card, or off the ship,
 * to the green node of a joint, where it snaps, held over the ship see-through in cyan till it is let go; a section
 * taken off the spine leaves a gap till it is set down again. The parts are shown by kind and by maker, each card with
 * its own model; the chosen part's figures under them; at the right the flight check, the ship's figures, the room
 * left in its hull, its finish and what the dock needs; along the bottom the keys. The right mouse button turns the
 * view round the ship and the wheel brings it nearer. Every change is sent to the shipyard, which keeps the design;
 * E sends it to the dock to be built once it is ready.
 */
public final class ShipDesignScreen extends AbstractContainerScreen<ShipDesignMenu> {

    private final ShipyardTerminalBlockEntity terminal;
    private final ShipDesignView view = new ShipDesignView();
    private ShipDesign design;
    private Map<ResourceLocation, ShipPart> parts = Map.of();
    private Map<ResourceLocation, ShipKit> kits = Map.of();
    private Map<ResourceLocation, ShipMaker> makers = Map.of();
    private List<ResourceLocation> finishes = List.of();
    private int tab = MODULES_TAB;
    private @Nullable ResourceLocation maker;
    private final StringBuilder search = new StringBuilder();
    private boolean searching;
    private int scroll;
    private @Nullable ResourceLocation chosen;
    private Tool tool = Tool.PLACE;
    private boolean snap = true;
    private @Nullable Held held;
    private @Nullable Target target;
    private boolean turning;
    private double lastX;
    private double lastY;
    private @Nullable StringBuilder naming;
    private @Nullable Text said;
    private long saidUntil;
    private float scale = 1;
    private float originX;
    private float originY;
    /* What the design comes to, worked out at each change. */
    private ShipLayout.Laid laid = new ShipLayout.Laid(Map.of(), Set.of(), 0);
    private Set<ShipChecks.Problem> problems = Set.of();
    private boolean fits;
    private double mass;
    private double thrust;
    private double lift;
    private double made;
    private double taken;
    private int seats;
    private int beds;

    private static final int HIDDEN = -10_000;
    private static final int MODULES_TAB = 2;
    /* How near a node, in the frame's pixels, a part must be let go to snap to it, the snap on and off. */
    private static final double SNAP_REACH = 24;
    private static final double FREE_REACH = 8;
    private static final long SAID_MILLIS = 3_000;
    private static final float TURN_SPEED = 0.5F;
    private static final double ZOOM_STEP = 1.12;
    /* Where a part held hovers off the place it would snap to, up and to the side, as the design shows it dragged. */
    private static final Vec3 HOVER = new Vec3(-4.2, 2.6, 0);
    /* The floor under the stowed gear, the height of the joints' nodes over the hull's floor, the hull's half width. */
    private static final double FLOOR_Y = -2;
    private static final double JOINT_Y = 3.0;
    private static final double SIDE_X = 2.4;
    private static final double WING_X = 4.0;
    /* A card's picture: the angle it is seen from, how far toward the viewer it is drawn. */
    private static final float CARD_PITCH = 20.0F;
    private static final float CARD_YAW = 214.0F;
    private static final float CARD_DEPTH = 80.0F;
    private static final double CARD_SPIN = 0.04;
    private static final double STANDARD_GRAVITY = 9.80665;
    private static final double KILO = 1_000;
    private static final double MEGA = 1_000_000;
    /* The scales of the figures' bars: a section of 10 blocks, 12 tonnes, 120 blocks, 30 megawatts; a ship of 60. */
    private static final double PART_LENGTH = 10;
    private static final double PART_TONNES = 12;
    private static final double PART_BLOCKS = 120;
    private static final double PART_MEGAWATTS = 30;
    private static final double SHIP_TONNES = 60;
    private static final double SHIP_KILONEWTONS = 600;
    private static final double SHIP_TWR = 2;
    private static final double SHIP_CREW = 4;
    private static final int MAKER_GLYPHS = 9;
    /* The ids of the finishes whose swatches the palette holds. */
    private static final ResourceLocation ORBITER = finish("orbiter");
    private static final ResourceLocation BARE_METAL = finish("bare_metal");
    private static final ResourceLocation ENAMEL = finish("enamel");
    /* The sign the game's text takes as its formatting mark, which a name must not hold. */
    private static final char FORMATTING_SIGN = (char) 0xA7;
    private static final int GAP_LENGTH_UNKNOWN = 0;
    private static final ResourceLocation GAP = ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, "gap");
    private static final int[] KIND_ICONS = {ShipDesignArt.COCKPIT, ShipDesignArt.HULL, ShipDesignArt.MODULE,
        ShipDesignArt.REACTOR, ShipDesignArt.ENGINE, ShipDesignArt.WING, ShipDesignArt.GEAR};
    private static final TextKey[] KIND_NAMES = {ShipDesignTexts.KIND_COCKPITS, ShipDesignTexts.KIND_HULL,
        ShipDesignTexts.KIND_MODULES, ShipDesignTexts.KIND_REACTORS, ShipDesignTexts.KIND_ENGINES,
        ShipDesignTexts.KIND_WINGS, ShipDesignTexts.KIND_GEAR};

    public ShipDesignScreen(final ShipDesignMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.terminal = menu.terminal();
        this.design = this.terminal.design();
        this.titleLabelX = HIDDEN;
        this.inventoryLabelY = HIDDEN;
    }

    /** Sets {@code to} as the design, as a player's edit is, and sends it to the shipyard. */
    public void edit(final ShipDesign to) {
        commit(to);
    }

    /** The design on the screen. */
    public ShipDesign design() {
        return this.design;
    }

    /** Where the node of the spine's joint {@code joint}, the nose's at 0, falls on the screen; none off it. */
    public Optional<float[]> joint(final int joint) {
        final int[] fronts = fronts(this.design.spine());
        if (joint < 0 || joint >= fronts.length) {
            return Optional.empty();
        }
        return this.view.project(0, JOINT_Y, fronts[joint], this.width, this.height);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        this.target = this.held == null ? null : target(mouseX, mouseY);
        this.view.draw(g, this.width, this.height, this.terminal, drawnModels(), heldModels(), heldAt(),
                FLOOR_Y);
        footprint(g);
        nodes(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The ship is drawn through the middle of the screen: nothing dims it, only the panels.
        renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override
    protected void init() {
        final double pixels = Math.min(this.minecraft.getWindow().getWidth() / (double) ShipDesignLayout.WIDTH,
                this.minecraft.getWindow().getHeight() / (double) ShipDesignLayout.HEIGHT);
        // Whole or half screen pixels to a pixel of the frame where the window allows, so its letters stay sharp.
        final double sharp = Math.floor(pixels * 2) / 2;
        this.scale = (float) ((sharp >= 1 ? sharp : pixels) / this.minecraft.getWindow().getGuiScale());
        this.imageWidth = Math.round(ShipDesignLayout.WIDTH * this.scale);
        this.imageHeight = Math.round(ShipDesignLayout.HEIGHT * this.scale);
        super.init();
        this.originX = (this.width - ShipDesignLayout.WIDTH * this.scale) / 2;
        this.originY = (this.height - ShipDesignLayout.HEIGHT * this.scale) / 2;
        if (this.minecraft.level != null) {
            this.parts = ShipParts.all(this.minecraft.level);
            this.kits = ShipKits.all(this.minecraft.level);
            this.makers = ShipMakers.all(this.minecraft.level);
            this.finishes = List.copyOf(ShipFinishes.all(this.minecraft.level).keySet());
        }
        if (this.design.name().isBlank()) {
            this.design = this.design.named(GameText.resolve(ShipDesignTexts.UNNAMED.with(
                    String.format(Locale.ROOT, "%02d", this.terminal.plans().size() + 1))).toUpperCase(Locale.ROOT));
            ShipDesigner.SET.send(this.design);
        }
        recount();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.terminal.isRemoved() || !this.terminal.formed()) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = frameX(mouseX);
        final double y = frameY(mouseY);
        if (this.naming != null && !onName(x, y)) {
            finishNaming(true);
        }
        this.searching = button == InputConstants.MOUSE_BUTTON_LEFT && x >= ShipDesignLayout.SEARCH_X0
                && x < ShipDesignLayout.SEARCH_X1 && y >= ShipDesignLayout.SEARCH_Y0 && y < ShipDesignLayout.SEARCH_Y1;
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            if (clickTop(x, y) || clickParts(x, y) || clickRight(x, y) || ShipDesignLayout.onPanel(x, y)) {
                return true;
            }
            return clickShip(mouseX, mouseY);
        }
        if (button == InputConstants.MOUSE_BUTTON_RIGHT && !ShipDesignLayout.onPanel(x, y)) {
            this.turning = true;
            this.lastX = mouseX;
            this.lastY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        if (this.turning) {
            this.view.turn((float) (mouseX - this.lastX) * TURN_SPEED, (float) (mouseY - this.lastY) * TURN_SPEED);
        }
        this.lastX = mouseX;
        this.lastY = mouseY;
        return true;
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (button == InputConstants.MOUSE_BUTTON_LEFT && this.held != null) {
            drop(mouseX, mouseY);
            return true;
        }
        this.turning = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final double x = frameX(mouseX);
        final double y = frameY(mouseY);
        if (x >= ShipDesignLayout.LEFT_X0 && x < ShipDesignLayout.LEFT_X1 && y >= ShipDesignLayout.CARD_Y0
                && y < ShipDesignLayout.CHOSEN_Y) {
            final int rows = (shelf().size() + ShipDesignLayout.CARD_COLUMNS - 1) / ShipDesignLayout.CARD_COLUMNS;
            this.scroll = Math.clamp(this.scroll - (int) Math.signum(scrollY), 0,
                    Math.max(0, rows - ShipDesignLayout.CARD_ROWS));
            return true;
        }
        if (!ShipDesignLayout.onPanel(x, y)) {
            this.view.zoom(scrollY > 0 ? 1 / ZOOM_STEP : ZOOM_STEP);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (this.naming != null) {
            if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
                finishNaming(true);
            } else if (key == GLFW.GLFW_KEY_ESCAPE) {
                finishNaming(false);
            } else if (key == GLFW.GLFW_KEY_BACKSPACE && !this.naming.isEmpty()) {
                this.naming.setLength(this.naming.length() - 1);
            }
            return true;
        }
        if (this.searching && key != GLFW.GLFW_KEY_ESCAPE) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !this.search.isEmpty()) {
                this.search.setLength(this.search.length() - 1);
                this.scroll = 0;
            } else if (key == GLFW.GLFW_KEY_ENTER) {
                this.searching = false;
            }
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_E -> {
                send();
                return true;
            }
            case GLFW.GLFW_KEY_S -> {
                save();
                return true;
            }
            case GLFW.GLFW_KEY_F -> {
                nextFinish();
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                removeChosen();
                return true;
            }
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
    }

    @Override
    public boolean charTyped(final char letter, final int modifiers) {
        if (letter == FORMATTING_SIGN || Character.isISOControl(letter)) {
            return false;
        }
        if (this.naming != null) {
            if (this.naming.length() < ShipDesign.LONGEST_NAME) {
                this.naming.append(Character.toUpperCase(letter));
            }
            return true;
        }
        if (this.searching) {
            this.search.append(letter);
            this.scroll = 0;
            return true;
        }
        return super.charTyped(letter, modifiers);
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        g.pose().pushPose();
        g.pose().translate(this.originX, this.originY, 0);
        g.pose().scale(this.scale, this.scale, 1);
        top(g);
        partsPanel(g);
        rightPanel(g);
        bottom(g);
        g.pose().popPose();
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Everything is drawn on the panels, in the frame's own pixels.
    }

    // ---- The view ----------------------------------------------------------------------------------------------

    /* The ship as drawn: a section held off the spine leaves its gap, a part held off the ship is not drawn. */
    private List<ShipPartModels.Placed> drawnModels() {
        final Held h = this.held;
        if (h == null || h.from() < 0) {
            return ShipPartModels.of(this.parts, this.kits, this.design);
        }
        if (h.hung()) {
            final List<ResourceLocation> hung = new ArrayList<>(this.design.hung());
            hung.remove(h.from());
            return ShipPartModels.of(this.parts, this.kits, this.design.withHung(hung));
        }
        final List<ResourceLocation> spine = new ArrayList<>(this.design.spine());
        spine.set(h.from(), GAP);
        final Map<ResourceLocation, ShipPart> withGap = new LinkedHashMap<>(this.parts);
        final ShipPart was = this.parts.get(h.id());
        withGap.put(GAP, new ShipPart(GAP.toString(), ShipPartKind.MODULE, ShipClass.SMALL, ShipPlace.SPINE,
                was == null ? GAP_LENGTH_UNKNOWN : was.length(), 0, false, false, Optional.empty()));
        return ShipPartModels.of(withGap, this.kits, this.design.withSpine(spine, this.design.flipped()));
    }

    /* The held part's models, where it would snap; none while it is over no node. */
    private List<ShipPartModels.Placed> heldModels() {
        final Held h = this.held;
        final Target to = this.target;
        if (h == null || to == null) {
            return List.of();
        }
        final ShipDesign base = reduced();
        if (to.hung()) {
            final List<ShipPartModels.Placed> without = ShipPartModels.of(this.parts, this.kits,
                    base.withHung(List.of()));
            final List<ShipPartModels.Placed> with = ShipPartModels.of(this.parts, this.kits,
                    base.withHung(List.of(h.id())));
            return with.subList(without.size(), with.size());
        }
        final List<ResourceLocation> before = new ArrayList<>(base.spine().subList(0, to.at()));
        final int count = ShipPartModels.of(this.parts, this.kits, new ShipDesign("", before, List.of())).size();
        before.add(h.id());
        final List<ShipPartModels.Placed> with = ShipPartModels.of(this.parts, this.kits,
                new ShipDesign("", before, List.of()));
        return with.subList(count, with.size());
    }

    /* Where the held part hovers: off its snapping place, as it is dragged. */
    private Vec3 heldAt() {
        return HOVER;
    }

    /* The ship's footprint on the floor: a dotted rectangle round it, its corners marked, its length and span. */
    private void footprint(final GuiGraphics g) {
        if (this.laid.cells().isEmpty()) {
            return;
        }
        final int[] box = bounds(this.laid.cells().keySet().stream().toList());
        final double x0 = box[0] - 1;
        final double x1 = box[1] + 2;
        final double z0 = box[4] - 1;
        final double z1 = box[5] + 2;
        final Optional<float[]> a = this.view.project(x0, FLOOR_Y, z0, this.width, this.height);
        final Optional<float[]> b = this.view.project(x1, FLOOR_Y, z0, this.width, this.height);
        final Optional<float[]> cc = this.view.project(x1, FLOOR_Y, z1, this.width, this.height);
        final Optional<float[]> d = this.view.project(x0, FLOOR_Y, z1, this.width, this.height);
        if (a.isEmpty() || b.isEmpty() || cc.isEmpty() || d.isEmpty()) {
            return;
        }
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final List<float[]> corners = List.of(a.get(), b.get(), cc.get(), d.get());
        for (int i = 0; i < corners.size(); i++) {
            final float[] p = corners.get(i);
            final float[] q = corners.get((i + 1) % corners.size());
            ShipDesignArt.dotted(g, p[0], p[1], q[0], q[1], c.accent(), 2);
            g.fill(Math.round(p[0]) - 1, Math.round(p[1]) - 1, Math.round(p[0]) + 2, Math.round(p[1]) + 2,
                    c.accent());
        }
        final int length = box[5] - box[4] + 1;
        final int span = box[1] - box[0] + 1;
        ShipDesignArt.tag(g, this.font, Math.round((b.get()[0] + cc.get()[0]) / 2) - 70,
                Math.round((b.get()[1] + cc.get()[1]) / 2) - 4, up(ShipDesignTexts.LENGTH),
                up(ShipDesignTexts.BLK.with(length)), true);
        ShipDesignArt.tag(g, this.font, Math.round((a.get()[0] + b.get()[0]) / 2) + 8,
                Math.round((a.get()[1] + b.get()[1]) / 2) + 4, up(ShipDesignTexts.SPAN),
                up(ShipDesignTexts.BLK.with(span)), true);
    }

    /* The joints' nodes, the nodes the held part snaps to, its dotted lines to them and where it snaps. */
    private void nodes(final GuiGraphics g, final int mouseX, final int mouseY) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final Held h = this.held;
        final List<Node> all = h == null ? joints(this.design.spine().size()) : targets(h);
        for (final Node node : all) {
            final Optional<float[]> at = this.view.project(node.x(), node.y(), node.z(), this.width, this.height);
            at.ifPresent(p -> ShipDesignArt.node(g, Math.round(p[0]), Math.round(p[1]),
                    h != null && this.target != null && node.target().equals(this.target)));
        }
        final Target to = this.target;
        if (h == null || to == null) {
            return;
        }
        for (final Node node : all) {
            if (!node.target().equals(to)) {
                continue;
            }
            final Optional<float[]> there = this.view.project(node.x(), node.y(), node.z(), this.width,
                    this.height);
            final Optional<float[]> from = this.view.project(node.x() + HOVER.x, node.y() + HOVER.y,
                    node.z() + HOVER.z, this.width, this.height);
            if (there.isEmpty() || from.isEmpty()) {
                continue;
            }
            ShipDesignArt.node(g, Math.round(from.get()[0]), Math.round(from.get()[1]), false);
            ShipDesignArt.dotted(g, from.get()[0], from.get()[1], there.get()[0], there.get()[1], c.node(), 3);
            final String snaps = up(ShipDesignTexts.SNAPS_HERE);
            final int sx = Math.round(there.get()[0]) + 7;
            final int sy = Math.round(there.get()[1]) - 5;
            g.fill(sx, sy, sx + ShipDesignArt.spacedWidth(this.font, snaps) + 6, sy + 11, c.label());
            ShipDesignArt.spaced(g, this.font, snaps, sx + 3, sy + 2, c.node());
        }
        if (this.target == null) {
            final String name = partName(h.id());
            Draw.text(g, this.font, name, mouseX + 10, mouseY + 4, c.accent());
        }
    }

    /* The nodes at each joint of a spine of {@code sections}, the nose's and the tail's too. */
    private List<Node> joints(final int sections) {
        final List<Node> out = new ArrayList<>();
        final int[] fronts = fronts(this.design.spine());
        for (int j = 0; j <= sections; j++) {
            out.add(new Node(0, JOINT_Y, fronts[j], new Target(false, j)));
        }
        return out;
    }

    /* The nodes {@code h} may snap to, each with what it snaps to. */
    private List<Node> targets(final Held h) {
        final ShipPart part = this.parts.get(h.id());
        if (part == null) {
            return List.of();
        }
        final List<Node> out = new ArrayList<>();
        final ShipDesign base = reduced();
        // A section held off the spine leaves a gap as long as itself, so the joints stand where the design has them.
        final int[] drawn = fronts(this.design.spine());
        if (part.place() == ShipPlace.NOSE) {
            out.add(new Node(0, JOINT_Y, 0, new Target(false, 0)));
            return out;
        }
        if (part.length() > 0) {
            // Every joint behind the cockpit, the gap's two ends both the gap.
            final int gapAt = h.hung() ? -1 : h.from();
            for (int j = 1; j < drawn.length; j++) {
                final int at = gapAt < 0 || j <= gapAt ? j : j == gapAt + 1 ? gapAt : j - 1;
                if (at <= base.spine().size()) {
                    out.add(new Node(0, JOINT_Y, drawn[j], new Target(false, at)));
                }
            }
            return out;
        }
        final int[] fronts = fronts(base.spine());
        final int sections = fronts.length - 1;
        final double end = fronts[sections];
        final double root = fronts[sections > 2 ? 2 : Math.min(1, sections)];
        final double joint = fronts[Math.min(1, sections)];
        final Target hang = new Target(true, 0);
        switch (part.place()) {
            case WING_ROOT -> out.add(new Node(SIDE_X + 1, 1, root, hang));
            case ON_WINGS -> out.add(new Node(WING_X, 1, root, hang));
            case SPINE_END -> out.add(new Node(0, JOINT_Y / 2, end, hang));
            case UNDER -> out.add(new Node(0, -1, end / 2, hang));
            default -> out.add(new Node(SIDE_X, JOINT_Y / 2, joint, hang));
        }
        return out;
    }

    /* The node nearest the mouse, within the snap's reach, as what the held part snaps to; none for none. */
    private @Nullable Target target(final double mouseX, final double mouseY) {
        final Held h = this.held;
        if (h == null) {
            return null;
        }
        final double reach = (this.snap ? SNAP_REACH : FREE_REACH) * this.scale;
        Target best = null;
        double nearest = Double.MAX_VALUE;
        for (final Node node : targets(h)) {
            final Optional<float[]> at = this.view.project(node.x(), node.y(), node.z(), this.width, this.height);
            if (at.isEmpty()) {
                continue;
            }
            final double away = Math.hypot(at.get()[0] - mouseX, at.get()[1] - mouseY);
            if (away < reach && away < nearest) {
                nearest = away;
                best = node.target();
            }
        }
        return best;
    }

    /* The design without the part held off it. */
    private ShipDesign reduced() {
        final Held h = this.held;
        if (h == null || h.from() < 0) {
            return this.design;
        }
        if (h.hung()) {
            final List<ResourceLocation> hung = new ArrayList<>(this.design.hung());
            hung.remove(h.from());
            return this.design.withHung(hung);
        }
        final List<ResourceLocation> spine = new ArrayList<>(this.design.spine());
        spine.remove(h.from());
        return this.design.withSpine(spine, shiftedFlips(this.design.flipped(), h.from(), -1));
    }

    /* Where each section of {@code spine} begins from the nose, and last where it ends. */
    private int[] fronts(final List<ResourceLocation> spine) {
        final List<Integer> lengths = new ArrayList<>();
        for (final ResourceLocation id : spine) {
            final ShipPart part = this.parts.get(id);
            lengths.add(part == null ? 0 : (int) part.length());
        }
        return ShipLayout.fronts(lengths);
    }

    /* A click on the ship: the tool at work on the part under the mouse, or that part taken up to be moved. */
    private boolean clickShip(final double mouseX, final double mouseY) {
        final int part = partAt(mouseX, mouseY);
        if (this.tool == Tool.FINISH) {
            nextFinish();
            return true;
        }
        if (part < 0) {
            return false;
        }
        final int sections = this.design.spine().size();
        final boolean hung = part >= sections;
        final int index = hung ? part - sections : part;
        final ResourceLocation id = hung ? this.design.hung().get(index) : this.design.spine().get(index);
        this.chosen = id;
        switch (this.tool) {
            case REMOVE -> remove(hung, index);
            case FLIP -> flip(hung, index);
            default -> this.held = new Held(id, index, hung);
        }
        return true;
    }

    /* The part of the ship under the mouse, by its place among the spine's and then the hung parts; -1 for none. */
    private int partAt(final double mouseX, final double mouseY) {
        final Map<Integer, List<ShipLayout.Cell>> byPart = new LinkedHashMap<>();
        this.laid.cells().forEach((cell, spot) -> byPart.computeIfAbsent(spot.part(), any -> new ArrayList<>())
                .add(cell));
        int best = -1;
        double smallest = Double.MAX_VALUE;
        for (final Map.Entry<Integer, List<ShipLayout.Cell>> entry : byPart.entrySet()) {
            final int[] box = bounds(entry.getValue());
            float x0 = Float.MAX_VALUE;
            float y0 = Float.MAX_VALUE;
            float x1 = -Float.MAX_VALUE;
            float y1 = -Float.MAX_VALUE;
            for (int k = 0; k < 8; k++) {
                final Optional<float[]> p = this.view.project((k & 1) == 0 ? box[0] : box[1] + 1,
                        (k & 2) == 0 ? box[2] : box[3] + 1, (k & 4) == 0 ? box[4] : box[5] + 1, this.width,
                        this.height);
                if (p.isPresent()) {
                    x0 = Math.min(x0, p.get()[0]);
                    y0 = Math.min(y0, p.get()[1]);
                    x1 = Math.max(x1, p.get()[0]);
                    y1 = Math.max(y1, p.get()[1]);
                }
            }
            final double area = (x1 - x0) * (double) (y1 - y0);
            if (mouseX >= x0 && mouseX <= x1 && mouseY >= y0 && mouseY <= y1 && area < smallest) {
                smallest = area;
                best = entry.getKey();
            }
        }
        return best;
    }

    /* Lets the held part go: set where it snaps, thrown away on the parts' panel, or put back where it was. */
    private void drop(final double mouseX, final double mouseY) {
        final Held h = this.held;
        final Target to = target(mouseX, mouseY);
        this.held = null;
        this.target = null;
        if (h == null) {
            return;
        }
        final double x = frameX(mouseX);
        final double y = frameY(mouseY);
        if (to == null) {
            if (h.from() >= 0 && x >= ShipDesignLayout.LEFT_X0 && x < ShipDesignLayout.LEFT_X1
                    && y >= ShipDesignLayout.PANEL_Y0 && y < ShipDesignLayout.PANEL_Y1) {
                remove(h.hung(), h.from());
            }
            return;
        }
        final ShipDesign base = h.from() >= 0 ? reducedOf(h) : this.design;
        final ShipPart part = this.parts.get(h.id());
        if (part == null) {
            return;
        }
        if (to.hung()) {
            final List<ResourceLocation> hung = new ArrayList<>();
            for (final ResourceLocation id : base.hung()) {
                final ShipPart other = this.parts.get(id);
                if (other == null || other.kind() != part.kind()) {
                    hung.add(id);
                }
            }
            hung.add(h.id());
            commit(base.withHung(hung));
            return;
        }
        final List<ResourceLocation> spine = new ArrayList<>(base.spine());
        List<Integer> flips = base.flipped();
        if (part.place() == ShipPlace.NOSE) {
            if (!spine.isEmpty() && kindAt(spine, 0) == ShipPartKind.COCKPIT) {
                spine.set(0, h.id());
            } else {
                spine.add(0, h.id());
                flips = shiftedFlips(flips, 0, 1);
            }
        } else {
            final int at = Math.clamp(to.at(), 0, spine.size());
            spine.add(at, h.id());
            flips = shiftedFlips(flips, at, 1);
        }
        commit(base.withSpine(spine, flips));
    }

    /* The design without {@code h}'s part, where it was taken off. */
    private ShipDesign reducedOf(final Held h) {
        final Held was = this.held;
        this.held = h;
        try {
            return reduced();
        } finally {
            this.held = was;
        }
    }

    private @Nullable ShipPartKind kindAt(final List<ResourceLocation> spine, final int at) {
        final ShipPart part = this.parts.get(spine.get(at));
        return part == null ? null : part.kind();
    }

    /* Takes the part at {@code index} off the ship. */
    private void remove(final boolean hung, final int index) {
        if (hung) {
            final List<ResourceLocation> left = new ArrayList<>(this.design.hung());
            left.remove(index);
            commit(this.design.withHung(left));
        } else {
            final List<ResourceLocation> left = new ArrayList<>(this.design.spine());
            left.remove(index);
            commit(this.design.withSpine(left, shiftedFlips(this.design.flipped(), index, -1)));
        }
    }

    /* Flips the section at {@code index} left for right; a cockpit and a hung part never. */
    private void flip(final boolean hung, final int index) {
        final ShipPart part = this.parts.get(this.design.spine().get(hung ? 0 : index));
        if (hung || part == null || part.place() == ShipPlace.NOSE) {
            say(ShipDesignTexts.NO_FLIP.text());
            return;
        }
        final List<Integer> flips = new ArrayList<>(this.design.flipped());
        if (!flips.remove((Integer) index)) {
            flips.add(index);
        }
        commit(this.design.withSpine(this.design.spine(), flips));
    }

    /* The chosen part, if it is on the ship, taken off it. */
    private void removeChosen() {
        final ResourceLocation id = this.chosen;
        if (id == null) {
            return;
        }
        final int hung = this.design.hung().indexOf(id);
        if (hung >= 0) {
            remove(true, hung);
            return;
        }
        final int section = this.design.spine().lastIndexOf(id);
        if (section >= 0) {
            remove(false, section);
        }
    }

    /* The flipped places moved past {@code from} by {@code by}, as a section is put in or taken out there. */
    private static List<Integer> shiftedFlips(final List<Integer> flips, final int from, final int by) {
        final List<Integer> out = new ArrayList<>();
        for (final int place : flips) {
            if (by < 0 && place == from) {
                continue;
            }
            out.add(place >= from ? place + by : place);
        }
        return out;
    }

    // ---- The panels --------------------------------------------------------------------------------------------

    /* The top: the screen's tag, the ship's name, its class, length and mass, and the tools. */
    private void top(final GuiGraphics g) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        ShipDesignArt.panel(g, this.font, ShipDesignLayout.MIDDLE_X0, ShipDesignLayout.PANEL_Y0,
                ShipDesignLayout.MIDDLE_X1, ShipDesignLayout.TOP_Y1, "", ShipDesignLayout.CUT);
        ShipDesignArt.chamfer(g, ShipDesignLayout.MIDDLE_X0, ShipDesignLayout.PANEL_Y0, ShipDesignLayout.TAG_X1,
                ShipDesignLayout.TAG_Y1, c.accent(), ShipDesignLayout.CUT, true, false, false);
        ShipDesignArt.spaced(g, this.font, up(ShipDesigner.DESIGN), ShipDesignLayout.MIDDLE_X0 + 10,
                ShipDesignLayout.TITLE_Y - 1, c.dark());
        final String name = this.naming != null ? this.naming + (Util.getMillis() / 500 % 2 == 0 ? "_" : "")
                : this.design.name();
        final int w = ShipDesignArt.spaced(g, this.font, name, ShipDesignLayout.NAME_X0, ShipDesignLayout.TITLE_Y - 1,
                this.naming != null ? c.accent() : c.text());
        final String line = up(ShipDesignTexts.CLASS_LINE.with(Text.of(up(ShipDesignTexts.SMALL_CLASS)),
                length(), tenth(this.mass / KILO)));
        ShipDesignArt.small(g, this.font, line, Math.max(ShipDesignLayout.FIGURES_X0,
                ShipDesignLayout.NAME_X0 + w + 10), ShipDesignLayout.TITLE_Y + 1, c.dim());
        int x = ShipDesignLayout.TOOLS_X0;
        for (final Tool each : Tool.values()) {
            final boolean on = each == Tool.MIRROR || each == Tool.SNAP && this.snap
                    || each != Tool.SNAP && each == this.tool;
            x += ShipDesignArt.button(g, this.font, x, ShipDesignLayout.TOOLS_Y0, ShipDesignLayout.TOOL_H,
                    each.icon, up(each.name), on) + ShipDesignLayout.TOOL_GAP;
        }
    }

    /* The parts: the search line, the kinds' tabs, the makers, the cards, the chosen part's figures and note. */
    private void partsPanel(final GuiGraphics g) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        ShipDesignArt.panel(g, this.font, ShipDesignLayout.LEFT_X0, ShipDesignLayout.PANEL_Y0,
                ShipDesignLayout.LEFT_X1, ShipDesignLayout.PANEL_Y1, up(ShipDesignTexts.PARTS), ShipDesignLayout.CUT);
        g.fill(ShipDesignLayout.SEARCH_X0, ShipDesignLayout.SEARCH_Y0, ShipDesignLayout.SEARCH_X1,
                ShipDesignLayout.SEARCH_Y1, c.search());
        ShipDesignArt.frame(g, ShipDesignLayout.SEARCH_X0, ShipDesignLayout.SEARCH_Y0, ShipDesignLayout.SEARCH_X1,
                ShipDesignLayout.SEARCH_Y1, this.searching ? c.accent() : c.line());
        ShipDesignArt.icon(g, ShipDesignArt.SEARCH, ShipDesignLayout.SEARCH_X0 + 2, ShipDesignLayout.SEARCH_Y0 - 1,
                c.dim(), c.dim());
        final String searched = this.search.isEmpty() && !this.searching ? up(ShipDesignTexts.SEARCH)
                : this.search.toString().toUpperCase(Locale.ROOT) + (this.searching ? "_" : "");
        ShipDesignArt.small(g, this.font, searched, ShipDesignLayout.SEARCH_X0 + 18, ShipDesignLayout.SEARCH_Y0 + 4,
                this.search.isEmpty() && !this.searching ? c.faint() : c.text());
        for (int i = 0; i < ShipDesignLayout.TABS; i++) {
            final int tx = ShipDesignLayout.tabX(i);
            final int ty = ShipDesignLayout.TAB_Y0;
            if (i == this.tab) {
                ShipDesignArt.chamfer(g, tx, ty, tx + ShipDesignLayout.TAB_W, ty + ShipDesignLayout.TAB_SIZE,
                        c.accent(), 4, true, false, false);
                ShipDesignArt.icon(g, KIND_ICONS[i], tx + 2, ty + 1, c.dark(), c.dark());
            } else {
                g.fill(tx, ty, tx + ShipDesignLayout.TAB_W, ty + ShipDesignLayout.TAB_SIZE, c.panelHi());
                ShipDesignArt.hline(g, tx, tx + ShipDesignLayout.TAB_W, ty + ShipDesignLayout.TAB_SIZE - 1,
                        c.line());
                ShipDesignArt.icon(g, KIND_ICONS[i], tx + 2, ty + 1, c.dim(), c.accent());
            }
        }
        ShipDesignArt.spaced(g, this.font, up(KIND_NAMES[this.tab]), ShipDesignLayout.CARD_X0,
                ShipDesignLayout.KIND_Y, c.text());
        int x = ShipDesignLayout.CARD_X0;
        for (final Map.Entry<ResourceLocation, String> chip : makerChips().entrySet()) {
            final boolean on = chip.getKey().equals(GAP) ? this.maker == null : chip.getKey().equals(this.maker);
            final int w = ShipDesignArt.smallWidth(this.font, chip.getValue()) + 8;
            if (on) {
                g.fill(x, ShipDesignLayout.MAKER_Y0, x + w, ShipDesignLayout.MAKER_Y0 + ShipDesignLayout.MAKER_H,
                        c.accentDeep());
            }
            ShipDesignArt.frame(g, x, ShipDesignLayout.MAKER_Y0, x + w,
                    ShipDesignLayout.MAKER_Y0 + ShipDesignLayout.MAKER_H, on ? c.accent() : c.line());
            ShipDesignArt.small(g, this.font, chip.getValue(), x + 4, ShipDesignLayout.MAKER_Y0 + 2,
                    on ? c.text() : c.dim());
            x += w + ShipDesignLayout.MAKER_GAP;
        }
        final List<ResourceLocation> shelf = shelf();
        final int shown = ShipDesignLayout.CARD_COLUMNS * ShipDesignLayout.CARD_ROWS;
        for (int i = 0; i < shown; i++) {
            final int index = this.scroll * ShipDesignLayout.CARD_COLUMNS + i;
            if (index < shelf.size()) {
                card(g, shelf.get(index), ShipDesignLayout.cardX(i), ShipDesignLayout.cardY(i));
            }
        }
        chosenPart(g);
    }

    /* A part's card: its model over its name and maker, its size, framed when chosen. */
    private void card(final GuiGraphics g, final ResourceLocation id, final int x0, final int y0) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final ShipPart part = this.parts.get(id);
        final boolean on = id.equals(this.chosen);
        final int x1 = x0 + ShipDesignLayout.CARD_W;
        final int y1 = y0 + ShipDesignLayout.CARD_H;
        g.fill(x0, y0, x1, y1, on ? c.cardOn() : c.card());
        ShipDesignArt.frame(g, x0, y0, x1, y1, on ? c.accent() : c.line());
        picture(g, id, x0 + 1, y0 + 1, x1 - 1, y0 + ShipDesignLayout.CARD_PICTURE_H);
        ShipDesignArt.small(g, this.font, fit(partName(id), ShipDesignLayout.CARD_W - 6), x0 + 3,
                y0 + ShipDesignLayout.CARD_PICTURE_H + 2, c.text());
        if (part == null) {
            return;
        }
        final String by = part.maker().map(this::makerName).orElse("");
        ShipDesignArt.small(g, this.font, by.length() > MAKER_GLYPHS ? by.substring(0, MAKER_GLYPHS) : by, x0 + 3,
                y0 + ShipDesignLayout.CARD_PICTURE_H + 10, c.dim());
        if (part.room() > 0) {
            final boolean medium = part.room() > 1;
            final String size = up(medium ? ShipDesignTexts.MEDIUM : ShipDesignTexts.SMALL_CLASS).substring(0, 1);
            g.fill(x1 - 12, y1 - 10, x1 - 2, y1 - 1, medium ? c.amber() : c.accentDeep());
            ShipDesignArt.small(g, this.font, size, x1 - 9, y1 - 8, medium ? c.dark() : c.text());
        }
    }

    /* A part's own model on its card, seen from above and aside, turning slowly while it is chosen. */
    private void picture(final GuiGraphics g, final ResourceLocation id, final int x0, final int y0, final int x1,
                         final int y1) {
        final ShipPart part = this.parts.get(id);
        if (part == null) {
            return;
        }
        final ShipDesign alone = part.length() > 0 ? new ShipDesign("", List.of(id), List.of())
                : new ShipDesign("", List.of(), List.of(id));
        final List<ShipPartModels.Placed> placed = ShipPartModels.of(this.parts, this.kits,
                alone.painted(this.design.finish()));
        final ShipLayout.Laid alone3d = ShipyardLayout.laid(this.parts, this.kits, alone);
        if (placed.isEmpty() || alone3d.cells().isEmpty()) {
            return;
        }
        final int[] box = bounds(alone3d.cells().keySet().stream().toList());
        final float wide = box[1] - box[0] + 1;
        final float tall = box[3] - box[2] + 1;
        final float deep = box[5] - box[4] + 1;
        final float fit = Math.min((x1 - x0) / (Math.max(wide, deep) * 1.15F), (y1 - y0) / (tall + deep * 0.45F));
        final float yaw = id.equals(this.chosen) ? (float) (Util.getMillis() * CARD_SPIN % 360) + CARD_YAW
                : CARD_YAW;
        final PoseStack pose = g.pose();
        g.flush();
        g.enableScissor(Math.round(guiX(x0)), Math.round(guiY(y0)), Math.round(guiX(x1)), Math.round(guiY(y1)));
        pose.pushPose();
        pose.translate((x0 + x1) / 2.0F, (y0 + y1) / 2.0F, CARD_DEPTH);
        pose.scale(fit, -fit, fit);
        pose.mulPose(Axis.XP.rotationDegrees(CARD_PITCH));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-(box[0] + box[1] + 1) / 2.0F, -(box[2] + box[3] + 1) / 2.0F, -(box[4] + box[5] + 1) / 2.0F);
        RenderSystem.enableDepthTest();
        Lighting.setupForEntityInInventory();
        for (final ShipPartModels.Placed one : placed) {
            final Optional<KitModel> model = KitModels.get(one.model());
            if (model.isEmpty()) {
                continue;
            }
            pose.pushPose();
            pose.translate(0, 0, one.z());
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

    /* The chosen part's name, its class and maker, its figures on bars, its note, whose its inside and paint are. */
    private void chosenPart(final GuiGraphics g) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final ResourceLocation id = this.chosen;
        final ShipPart part = id == null ? null : this.parts.get(id);
        if (id == null || part == null) {
            return;
        }
        final int x0 = ShipDesignLayout.CARD_X0;
        final int x1 = ShipDesignLayout.CHOSEN_X1;
        ShipDesignArt.heading(g, this.font, x0, ShipDesignLayout.CHOSEN_Y, partName(id), x1);
        final String by = part.maker().map(this::makerName).orElse("");
        final Text line = part.kind() == ShipPartKind.MODULE
                ? ShipDesignTexts.MODULE_LINE.with(Text.of(up(part.room() > 1 ? ShipDesignTexts.MEDIUM
                : ShipDesignTexts.SMALL_CLASS)), Text.of(by))
                : ShipDesignTexts.PART_LINE.with(Text.of(up(ShipDesignTexts.SMALL_CLASS)), Text.of(by));
        ShipDesignArt.small(g, this.font, up(line), x0, ShipDesignLayout.CHOSEN_Y + 12, c.dim());
        final ShipPart.Figures f = part.figures();
        final int blocks = this.kits.containsKey(id) ? ShipyardLayout.blocks(this.kits.get(id)) : 0;
        figure(g, 0, up(ShipDesignTexts.LENGTH), up(ShipDesignTexts.BLK.with((int) part.length())),
                part.length() / PART_LENGTH, c.accent());
        figure(g, 1, up(ShipDesignTexts.MASS), up(ShipDesignTexts.TONNES.with(tenth(f.mass() / KILO))),
                f.mass() / KILO / PART_TONNES, c.accent());
        figure(g, 2, up(ShipDesignTexts.BLOCKS), String.valueOf(blocks), blocks / PART_BLOCKS, c.accent());
        figure(g, 3, up(ShipDesignTexts.POWER), up(ShipDesignTexts.MEGAWATTS.with(tenth(f.power() / MEGA))),
                Math.abs(f.power()) / MEGA / PART_MEGAWATTS, f.power() >= 0 ? c.green() : c.amber());
        final String noteKey = SmallShipNotes.keyOf(part.name());
        if (I18n.exists(noteKey)) {
            final List<FormattedCharSequence> rows = this.font.split(Component.literal(
                    Component.translatable(noteKey).getString().toUpperCase(Locale.ROOT)),
                    Math.round((x1 - x0) / 0.75F));
            for (int i = 0; i < Math.min(rows.size(), ShipDesignLayout.NOTE_ROWS); i++) {
                g.pose().pushPose();
                g.pose().translate(x0, ShipDesignLayout.NOTE_Y + i * ShipDesignLayout.NOTE_STEP, 0);
                g.pose().scale(0.75F, 0.75F, 1);
                Draw.text(g, this.font, rows.get(i), 0, 0, c.dim());
                g.pose().popPose();
            }
        }
        if (part.length() > 0) {
            ShipDesignArt.small(g, this.font, up(ShipDesignTexts.INSIDE.with(Text.of(by))), x0,
                    ShipDesignLayout.MAKER_LINE_Y, c.accent());
        }
        ShipDesignArt.small(g, this.font, up(ShipDesignTexts.PAINT), x0, ShipDesignLayout.MAKER_LINE_Y
                + ShipDesignLayout.NOTE_STEP, c.accent());
    }

    private void figure(final GuiGraphics g, final int row, final String name, final String value,
                        final double share, final int colour) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final int y = ShipDesignLayout.figureY(row);
        ShipDesignArt.small(g, this.font, name, ShipDesignLayout.CARD_X0, y, c.dim());
        ShipDesignArt.small(g, this.font, value, ShipDesignLayout.CHOSEN_X1 - ShipDesignArt.smallWidth(this.font,
                value), y, c.text());
        ShipDesignArt.bar(g, ShipDesignLayout.CARD_X0, y + 9, ShipDesignLayout.CHOSEN_X1 - ShipDesignLayout.CARD_X0,
                Math.min(1, share), colour);
    }

    /* The right: the flight check, the ship's figures, the room in its hull, its finish, the dock. */
    private void rightPanel(final GuiGraphics g) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        ShipDesignArt.panel(g, this.font, ShipDesignLayout.RIGHT_X0, ShipDesignLayout.PANEL_Y0,
                ShipDesignLayout.RIGHT_X1, ShipDesignLayout.PANEL_Y1, up(ShipDesignTexts.FLIGHT_CHECK),
                ShipDesignLayout.CUT);
        final int x0 = ShipDesignLayout.INNER_X0;
        final int x1 = ShipDesignLayout.INNER_X1;
        final List<Check> checks = checks();
        for (int i = 0; i < Math.min(checks.size(), ShipDesignLayout.CHECK_ROWS); i++) {
            final Check check = checks.get(i);
            final int y = ShipDesignLayout.CHECK_Y0 + i * ShipDesignLayout.CHECK_STEP;
            final int colour = check.colour(c);
            g.fill(x0, y, x0 + ShipDesignLayout.CHECK_BOX, y + ShipDesignLayout.CHECK_BOX, ShipDesignArt.mix(colour,
                    c.black(), 0.6F));
            ShipDesignArt.frame(g, x0, y, x0 + ShipDesignLayout.CHECK_BOX, y + ShipDesignLayout.CHECK_BOX, colour);
            ShipDesignArt.small(g, this.font, check.mark(), x0 + 3, y + 2, colour);
            ShipDesignArt.small(g, this.font, up(check.line()), x0 + 16, y + 2, check.met() ? c.text() : colour);
        }
        ShipDesignArt.heading(g, this.font, x0, ShipDesignLayout.SHIP_Y, up(ShipDesignTexts.THE_SHIP), x1);
        final double twr = this.mass > 0 ? this.lift / (this.mass * STANDARD_GRAVITY) : 0;
        shipFigure(g, 0, up(ShipDesignTexts.MASS), up(ShipDesignTexts.TONNES.with(tenth(this.mass / KILO))),
                this.mass / KILO / SHIP_TONNES, c.accent());
        shipFigure(g, 1, up(ShipDesignTexts.THRUST), up(ShipDesignTexts.KILONEWTONS.with(
                Math.round(this.thrust / KILO))), this.thrust / KILO / SHIP_KILONEWTONS, c.accent());
        shipFigure(g, 2, up(ShipDesignTexts.VTOL_TWR), String.format(Locale.ROOT, "%.2f", twr), twr / SHIP_TWR,
                c.accent());
        shipFigure(g, 3, up(ShipDesignTexts.POWER), up(ShipDesignTexts.POWER_OF.with(tenth(this.taken / MEGA),
                tenth(this.made / MEGA))), this.made > 0 ? this.taken / this.made : 1, c.amber());
        shipFigure(g, 4, up(ShipDesignTexts.CREW), up(ShipDesignTexts.CREW_LINE.with(this.seats, this.beds)),
                (this.seats + this.beds) / SHIP_CREW, c.accent());
        ShipDesignArt.heading(g, this.font, x0, ShipDesignLayout.ROOM_Y, up(ShipDesignTexts.ROOM), x1);
        final List<String> rooms = rooms();
        for (int k = 0; k < ShipDesignLayout.SLOTS; k++) {
            final int sx = x0 + k * ShipDesignLayout.SLOT_STEP;
            final int sy = ShipDesignLayout.ROOM_Y + 14;
            final boolean used = k < rooms.size();
            g.fill(sx, sy, sx + ShipDesignLayout.SLOT_W, sy + ShipDesignLayout.SLOT_H, used ? c.accentDeep()
                    : c.card());
            ShipDesignArt.frame(g, sx, sy, sx + ShipDesignLayout.SLOT_W, sy + ShipDesignLayout.SLOT_H,
                    used ? c.accent() : c.line());
            final String what = used ? fit(rooms.get(k), ShipDesignLayout.SLOT_W - 4) : up(ShipDesignTexts.FREE);
            ShipDesignArt.small(g, this.font, what, sx + ShipDesignLayout.SLOT_W / 2
                    - ShipDesignArt.smallWidth(this.font, what) / 2, sy + 7, used ? c.text() : c.faint());
        }
        ShipDesignArt.small(g, this.font, up(ShipDesignTexts.ROOM_NOTE), x0, ShipDesignLayout.ROOM_Y + 40, c.dim());
        ShipDesignArt.heading(g, this.font, x0, ShipDesignLayout.FINISH_Y, up(ShipDesignTexts.FINISH), x1);
        final List<ResourceLocation> known = swatched();
        for (int k = 0; k < Math.min(known.size(), ShipDesignLayout.SWATCHES); k++) {
            swatch(g, known.get(k), x0 + k * ShipDesignLayout.SWATCH_STEP, ShipDesignLayout.FINISH_Y + 13);
        }
        ShipDesignArt.heading(g, this.font, x0, ShipDesignLayout.DOCK_Y, up(ShipDesignTexts.THE_DOCK), x1);
        final int blocks = this.laid.cells().size();
        final int held = 0;
        final int dy = ShipDesignLayout.DOCK_Y;
        ShipDesignArt.small(g, this.font, up(ShipDesignTexts.BLOCKS), x0, dy + 13, c.dim());
        right(g, String.valueOf(blocks), x1, dy + 13, c.text());
        ShipDesignArt.small(g, this.font, up(ShipDesignTexts.IN_THE_PORT), x0, dy + 26, c.dim());
        right(g, String.valueOf(held), x1, dy + 26, c.text());
        ShipDesignArt.bar(g, x0, dy + 35, x1 - x0, blocks > 0 ? held / (double) blocks : 0, c.amber());
        ShipDesignArt.small(g, this.font, up(ShipDesignTexts.TO_BRING.with(blocks - held)), x0, dy + 46, c.amber());
    }

    private void shipFigure(final GuiGraphics g, final int row, final String name, final String value,
                            final double share, final int colour) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final int y = ShipDesignLayout.shipY(row);
        ShipDesignArt.small(g, this.font, name, ShipDesignLayout.INNER_X0, y, c.dim());
        right(g, value, ShipDesignLayout.INNER_X1, y, c.text());
        ShipDesignArt.bar(g, ShipDesignLayout.INNER_X0, y + 9, ShipDesignLayout.INNER_X1 - ShipDesignLayout.INNER_X0,
                Math.min(1, share), colour);
    }

    /* A finish's swatch: its three colours and its name, framed cyan when it is the ship's. */
    private void swatch(final GuiGraphics g, final ResourceLocation id, final int x, final int y) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final boolean on = id.equals(this.design.finish());
        g.fill(x, y, x + ShipDesignLayout.SWATCH_W, y + ShipDesignLayout.SWATCH_H, c.card());
        ShipDesignArt.frame(g, x, y, x + ShipDesignLayout.SWATCH_W, y + ShipDesignLayout.SWATCH_H,
                on ? c.accent() : c.line());
        final int[] colours = id.equals(ORBITER) ? new int[] {c.orbiterA(), c.orbiterB(), c.orbiterC()}
                : id.equals(BARE_METAL) ? new int[] {c.metalA(), c.metalB(), c.metalC()}
                : new int[] {c.enamelA(), c.enamelB(), c.enamelC()};
        for (int k = 0; k < colours.length; k++) {
            g.fill(x + 3 + k * 9, y + 3, x + 11 + k * 9, y + 11, colours[k]);
        }
        final String name = ShipFinishes.get(this.minecraft.level, id).map(f -> Component.translatable(f.name())
                .getString().toUpperCase(Locale.ROOT)).orElse(id.getPath());
        ShipDesignArt.small(g, this.font, fit(name, ShipDesignLayout.SWATCH_W - 4), x + 3, y + 13,
                on ? c.text() : c.dim());
    }

    /* The bottom: the keys, and what the screen is doing. */
    private void bottom(final GuiGraphics g) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        ShipDesignArt.panel(g, this.font, ShipDesignLayout.MIDDLE_X0, ShipDesignLayout.BOTTOM_Y0,
                ShipDesignLayout.MIDDLE_X1, ShipDesignLayout.PANEL_Y1, "", 5);
        int x = ShipDesignLayout.KEYS_X0;
        x += ShipDesignArt.keycap(g, this.font, x, ShipDesignLayout.KEYS_Y0, keyName(GLFW.GLFW_KEY_E),
                up(ShipDesignTexts.SEND)) + 12;
        x += ShipDesignArt.keycap(g, this.font, x, ShipDesignLayout.KEYS_Y0, keyName(GLFW.GLFW_KEY_S),
                up(ShipDesignTexts.SAVE)) + 12;
        x += ShipDesignArt.keycap(g, this.font, x, ShipDesignLayout.KEYS_Y0, keyName(GLFW.GLFW_KEY_F),
                up(ShipDesignTexts.FINISH_TOOL)) + 12;
        ShipDesignArt.keycap(g, this.font, x, ShipDesignLayout.KEYS_Y0, up(ShipDesignTexts.KEY_DELETE),
                up(ShipDesignTexts.DELETE));
        final Text now;
        final int colour;
        if (this.said != null && Util.getMillis() < this.saidUntil) {
            now = this.said;
            colour = c.accent();
        } else if (this.held != null) {
            now = ShipDesignTexts.DRAG.with(Text.of(partName(this.held.id())));
            colour = c.accent();
        } else if (ready()) {
            now = ShipDesignTexts.READY.text();
            colour = c.green();
        } else {
            now = ShipDesignTexts.NOT_READY.with(Text.of(firstProblem()));
            colour = c.amber();
        }
        final String shown = up(now);
        ShipDesignArt.small(g, this.font, shown, ShipDesignLayout.STATUS_X1 - ShipDesignArt.smallWidth(this.font,
                shown), ShipDesignLayout.STATUS_Y, colour);
    }

    // ---- The clicks ----------------------------------------------------------------------------------------------

    /* A click on the top: the ship's name, to rename it, or a tool. */
    private boolean clickTop(final double x, final double y) {
        if (onName(x, y)) {
            this.naming = new StringBuilder(this.design.name());
            return true;
        }
        if (y < ShipDesignLayout.TOOLS_Y0 || y >= ShipDesignLayout.TOOLS_Y0 + ShipDesignLayout.TOOL_H) {
            return false;
        }
        int at = ShipDesignLayout.TOOLS_X0;
        for (final Tool each : Tool.values()) {
            final int w = ShipDesignArt.buttonWidth(this.font, up(each.name));
            if (x >= at && x < at + w) {
                switch (each) {
                    case MIRROR -> say(ShipDesignTexts.PAIRS.text());
                    case SNAP -> this.snap = !this.snap;
                    default -> this.tool = each;
                }
                return true;
            }
            at += w + ShipDesignLayout.TOOL_GAP;
        }
        return false;
    }

    /* A click on the parts: a tab, a maker, a card to take up a part from. */
    private boolean clickParts(final double x, final double y) {
        if (x < ShipDesignLayout.LEFT_X0 || x >= ShipDesignLayout.LEFT_X1) {
            return false;
        }
        if (y >= ShipDesignLayout.TAB_Y0 && y < ShipDesignLayout.TAB_Y0 + ShipDesignLayout.TAB_SIZE) {
            for (int i = 0; i < ShipDesignLayout.TABS; i++) {
                if (x >= ShipDesignLayout.tabX(i) && x < ShipDesignLayout.tabX(i) + ShipDesignLayout.TAB_W) {
                    this.tab = i;
                    this.scroll = 0;
                    return true;
                }
            }
        }
        if (y >= ShipDesignLayout.MAKER_Y0 && y < ShipDesignLayout.MAKER_Y0 + ShipDesignLayout.MAKER_H) {
            int at = ShipDesignLayout.CARD_X0;
            for (final Map.Entry<ResourceLocation, String> chip : makerChips().entrySet()) {
                final int w = ShipDesignArt.smallWidth(this.font, chip.getValue()) + 8;
                if (x >= at && x < at + w) {
                    this.maker = chip.getKey().equals(GAP) ? null : chip.getKey();
                    this.scroll = 0;
                    return true;
                }
                at += w + ShipDesignLayout.MAKER_GAP;
            }
        }
        final List<ResourceLocation> shelf = shelf();
        for (int i = 0; i < ShipDesignLayout.CARD_COLUMNS * ShipDesignLayout.CARD_ROWS; i++) {
            final int cx = ShipDesignLayout.cardX(i);
            final int cy = ShipDesignLayout.cardY(i);
            final int index = this.scroll * ShipDesignLayout.CARD_COLUMNS + i;
            if (index < shelf.size() && x >= cx && x < cx + ShipDesignLayout.CARD_W && y >= cy
                    && y < cy + ShipDesignLayout.CARD_H) {
                this.chosen = shelf.get(index);
                this.held = new Held(this.chosen, -1, false);
                return true;
            }
        }
        return false;
    }

    /* A click on the right: a finish's swatch. */
    private boolean clickRight(final double x, final double y) {
        final int sy = ShipDesignLayout.FINISH_Y + 13;
        if (y < sy || y >= sy + ShipDesignLayout.SWATCH_H) {
            return false;
        }
        final List<ResourceLocation> known = swatched();
        for (int k = 0; k < Math.min(known.size(), ShipDesignLayout.SWATCHES); k++) {
            final int sx = ShipDesignLayout.INNER_X0 + k * ShipDesignLayout.SWATCH_STEP;
            if (x >= sx && x < sx + ShipDesignLayout.SWATCH_W) {
                commit(this.design.painted(known.get(k)));
                return true;
            }
        }
        return false;
    }

    private boolean onName(final double x, final double y) {
        return x >= ShipDesignLayout.NAME_X0 && x < ShipDesignLayout.FIGURES_X0 && y >= ShipDesignLayout.PANEL_Y0
                && y < ShipDesignLayout.TAG_Y1;
    }

    private void finishNaming(final boolean keep) {
        final StringBuilder typed = this.naming;
        this.naming = null;
        if (keep && typed != null && !typed.toString().isBlank()) {
            commit(this.design.named(typed.toString().strip()));
        }
    }

    // ---- The design ----------------------------------------------------------------------------------------------

    /* Sets {@code to} as the design, and sends it to the shipyard. */
    private void commit(final ShipDesign to) {
        if (!to.bounded() || to.equals(this.design)) {
            return;
        }
        this.design = to;
        ShipDesigner.SET.send(to);
        recount();
    }

    /* Works out what the design comes to: its blocks, what keeps it down, whether it fits, its figures. */
    private void recount() {
        this.laid = ShipyardLayout.laid(this.parts, this.kits, this.design);
        this.problems = this.design.problems(this.parts);
        this.fits = ShipyardLayout.fits(this.laid);
        this.mass = 0;
        this.thrust = 0;
        this.lift = 0;
        this.made = 0;
        this.taken = 0;
        this.seats = 0;
        this.beds = 0;
        final List<ResourceLocation> all = new ArrayList<>(this.design.spine());
        all.addAll(this.design.hung());
        for (final ResourceLocation id : all) {
            final ShipPart part = this.parts.get(id);
            if (part == null) {
                continue;
            }
            final ShipPart.Figures f = part.figures();
            this.mass += f.mass();
            this.thrust += f.thrust();
            this.lift += f.lift();
            this.made += Math.max(0, f.power());
            this.taken += Math.max(0, -f.power());
            this.seats += f.seats();
            this.beds += f.beds();
        }
        final double middle = this.laid.length() / 2.0;
        this.view.aim(new Vec3(0, 1.2, middle));
    }

    private boolean ready() {
        return this.problems.isEmpty() && this.fits;
    }

    /* Sends the design to the dock, when it is ready; says why not otherwise. */
    private void send() {
        if (!ready()) {
            say(ShipDesignTexts.NOT_READY.with(Text.of(firstProblem())));
            return;
        }
        ShipDesigner.CONFIRM.send(true);
        say(ShipDesignTexts.SENT.text());
    }

    /* Saves the design as one of the shipyard's plans, by its name. */
    private void save() {
        final boolean room = this.terminal.plans().size() < ShipyardTerminalBlockEntity.MOST_PLANS
                || this.terminal.plans().stream().anyMatch(plan -> plan.name().equals(this.design.name()));
        if (this.design.name().isBlank() || !room) {
            say(ShipDesignTexts.NOT_SAVED.text());
            return;
        }
        ShipDesigner.SAVE.send(true);
        say(ShipDesignTexts.SAVED.with(Text.of(this.design.name())));
    }

    private void nextFinish() {
        final List<ResourceLocation> known = swatched();
        if (known.isEmpty()) {
            return;
        }
        final int at = known.indexOf(this.design.finish());
        commit(this.design.painted(known.get((at + 1) % known.size())));
    }

    /* The flight check's lines, as the design stands, the held section's gap breaking the spine. */
    private List<Check> checks() {
        final List<Check> out = new ArrayList<>();
        out.add(this.problems.contains(ShipChecks.Problem.NO_COCKPIT) ? Check.red(ShipDesignTexts.NO_COCKPIT.text())
                : Check.green(ShipDesignTexts.COCKPIT_OK.text()));
        out.add(this.problems.contains(ShipChecks.Problem.NO_AIRLOCK) ? Check.red(ShipDesignTexts.NO_AIRLOCK.text())
                : Check.green(ShipDesignTexts.AIRLOCK_OK.text()));
        out.add(this.problems.contains(ShipChecks.Problem.NO_REACTOR) ? Check.red(ShipDesignTexts.NO_REACTOR.text())
                : Check.green(ShipDesignTexts.REACTOR_OK.text()));
        final Optional<ResourceLocation> main = ofKind(ShipPartKind.MAIN_ENGINE);
        out.add(main.isEmpty() ? Check.red(ShipDesignTexts.NO_MAIN.text())
                : Check.green(ShipDesignTexts.MAIN_OK.with(Text.of(partName(main.get())))));
        out.add(ofKind(ShipPartKind.AIR_ENGINE).isEmpty() ? Check.amber(ShipDesignTexts.NO_AIR.text())
                : Check.green(ShipDesignTexts.AIR_OK.text()));
        final Optional<ResourceLocation> gear = ofKind(ShipPartKind.GEAR);
        out.add(gear.isEmpty() ? Check.red(ShipDesignTexts.NO_GEAR.text())
                : Check.green(ShipDesignTexts.GEAR_OK.with(Text.of(partName(gear.get())))));
        out.add(spineCheck());
        out.add(this.problems.contains(ShipChecks.Problem.TOO_MUCH) ? Check.red(ShipDesignTexts.TOO_MUCH.text())
                : Check.amber(ShipDesignTexts.ROOM_LINE.with(rooms().size(), ShipClass.SMALL.room())));
        return out;
    }

    private Check spineCheck() {
        final Held h = this.held;
        if (h != null && h.from() >= 0 && !h.hung()) {
            return Check.red(ShipDesignTexts.SPINE_GAP.text());
        }
        if (this.problems.contains(ShipChecks.Problem.UNKNOWN_PART)) {
            return Check.red(ShipDesignTexts.UNKNOWN.text());
        }
        if (this.problems.contains(ShipChecks.Problem.BROKEN_SPINE)) {
            return Check.red(ShipDesignTexts.SPINE_SHORT.text());
        }
        if (this.problems.contains(ShipChecks.Problem.MISPLACED)) {
            return Check.red(ShipDesignTexts.MISPLACED.text());
        }
        if (this.problems.contains(ShipChecks.Problem.MIXED_CLASSES)) {
            return Check.red(ShipDesignTexts.MIXED.text());
        }
        if (!this.fits) {
            return Check.red(ShipDesignTexts.NO_FIT.text());
        }
        return Check.green(ShipDesignTexts.SPINE_OK.text());
    }

    /* The first red line of the flight check, as words. */
    private String firstProblem() {
        for (final Check check : checks()) {
            if (check.red()) {
                return GameText.resolve(check.line());
            }
        }
        return "";
    }

    /* The modules and holds taking the hull's room, a name for each small room they take. */
    private List<String> rooms() {
        final List<String> out = new ArrayList<>();
        for (final ResourceLocation id : this.design.spine()) {
            final ShipPart part = this.parts.get(id);
            if (part != null) {
                for (int k = 0; k < part.room(); k++) {
                    out.add(partName(id));
                }
            }
        }
        return out;
    }

    private Optional<ResourceLocation> ofKind(final ShipPartKind kind) {
        return this.design.hung().stream().filter(id -> this.parts.containsKey(id)
                && this.parts.get(id).kind() == kind).findFirst();
    }

    /* The parts on the shelf: the chosen kind's, of the chosen maker, matching the search, in the catalogue's order. */
    private List<ResourceLocation> shelf() {
        final String wanted = this.search.toString().toLowerCase(Locale.ROOT);
        final List<ResourceLocation> out = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, ShipPart> entry : this.parts.entrySet()) {
            final ShipPart part = entry.getValue();
            if (group(part.kind()) != this.tab || this.maker != null
                    && !part.maker().map(this.maker::equals).orElse(false)) {
                continue;
            }
            if (!wanted.isEmpty() && !partName(entry.getKey()).toLowerCase(Locale.ROOT).contains(wanted)) {
                continue;
            }
            out.add(entry.getKey());
        }
        // In the catalogue's own order, a data pack's parts after it.
        final List<ResourceLocation> declared = SmallShipParts.order();
        out.sort((a, b) -> Integer.compare(declared.contains(a) ? declared.indexOf(a) : declared.size(),
                declared.contains(b) ? declared.indexOf(b) : declared.size()));
        return out;
    }

    /* The makers' filters: all of them first, under the gap's id, then each maker's mark. */
    private Map<ResourceLocation, String> makerChips() {
        final Map<ResourceLocation, String> out = new LinkedHashMap<>();
        out.put(GAP, up(ShipDesignTexts.ALL));
        this.makers.forEach((id, made) -> out.put(id, Component.translatable(made.mark()).getString()
                .toUpperCase(Locale.ROOT)));
        return out;
    }

    /* The finishes the palette has swatches of, in the approved design's order, those the world knows. */
    private List<ResourceLocation> swatched() {
        return List.of(ORBITER, BARE_METAL, ENAMEL).stream().filter(this.finishes::contains).toList();
    }

    private static int group(final ShipPartKind kind) {
        return switch (kind) {
            case COCKPIT -> 0;
            case HULL, HOLD -> 1;
            case MODULE -> MODULES_TAB;
            case REACTOR -> 3;
            case MAIN_ENGINE, AIR_ENGINE, THRUSTERS -> 4;
            case WINGS, FINS -> 5;
            case GEAR -> 6;
        };
    }

    private int length() {
        if (this.laid.cells().isEmpty()) {
            return 0;
        }
        final int[] box = bounds(this.laid.cells().keySet().stream().toList());
        return box[5] - box[4] + 1;
    }

    /* The least and the most x, y and z of {@code cells}. */
    private static int[] bounds(final List<ShipLayout.Cell> cells) {
        final int[] box = {Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE,
            Integer.MAX_VALUE, Integer.MIN_VALUE};
        for (final ShipLayout.Cell cell : cells) {
            box[0] = Math.min(box[0], cell.x());
            box[1] = Math.max(box[1], cell.x());
            box[2] = Math.min(box[2], cell.y());
            box[3] = Math.max(box[3], cell.y());
            box[4] = Math.min(box[4], cell.z());
            box[5] = Math.max(box[5], cell.z());
        }
        return box;
    }

    private String partName(final ResourceLocation id) {
        final ShipPart part = this.parts.get(id);
        return part == null ? id.getPath() : Component.translatable(part.name()).getString().toUpperCase(Locale.ROOT);
    }

    private String makerName(final ResourceLocation id) {
        final ShipMaker made = this.makers.get(id);
        return made == null ? id.getPath() : Component.translatable(made.name()).getString().toUpperCase(Locale.ROOT);
    }

    private void say(final Text text) {
        this.said = text;
        this.saidUntil = Util.getMillis() + SAID_MILLIS;
    }

    private void right(final GuiGraphics g, final String text, final int x1, final int y, final int colour) {
        ShipDesignArt.small(g, this.font, text, x1 - ShipDesignArt.smallWidth(this.font, text), y, colour);
    }

    /* {@code text} cut to fit {@code width} pixels in the small letters. */
    private String fit(final String text, final int width) {
        String out = text;
        while (!out.isEmpty() && ShipDesignArt.smallWidth(this.font, out) > width) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private double frameX(final double mouseX) {
        return (mouseX - this.originX) / this.scale;
    }

    private double frameY(final double mouseY) {
        return (mouseY - this.originY) / this.scale;
    }

    private float guiX(final double x) {
        return this.originX + (float) x * this.scale;
    }

    private float guiY(final double y) {
        return this.originY + (float) y * this.scale;
    }

    private static String keyName(final int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString().toUpperCase(Locale.ROOT);
    }

    private static String up(final TextKey key) {
        return GameText.resolve(key).toUpperCase(Locale.ROOT);
    }

    private static String up(final Text text) {
        return GameText.resolve(text).toUpperCase(Locale.ROOT);
    }

    private static String tenth(final double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static ResourceLocation finish(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsSpace.MODID, path);
    }

    /* What a click on the ship does: take a part up, take it off, flip it; or paint the ship. */
    private enum Tool {
        PLACE(ShipDesignArt.PLACE, ShipDesignTexts.PLACE),
        REMOVE(ShipDesignArt.REMOVE, ShipDesignTexts.REMOVE),
        FLIP(ShipDesignArt.FLIP, ShipDesignTexts.FLIP),
        MIRROR(ShipDesignArt.MIRROR, ShipDesignTexts.MIRROR),
        FINISH(ShipDesignArt.FINISH, ShipDesignTexts.FINISH_TOOL),
        SNAP(ShipDesignArt.SNAP, ShipDesignTexts.SNAP);

        private final int icon;
        private final TextKey name;

        Tool(final int icon, final TextKey name) {
            this.icon = icon;
            this.name = name;
        }
    }

    /*
     * A part held to be set on the ship: its id, and where it was taken off the ship, its place along the spine or
     * among the hung parts, or -1 for one taken from its card.
     */
    private record Held(ResourceLocation id, int from, boolean hung) {
    }

    /* What a held part snaps to: a place along the spine to be set in, or its own place hung on the ship. */
    private record Target(boolean hung, int at) {
    }

    /* A node in the ship's frame, and what a part let go on it snaps to. */
    private record Node(double x, double y, double z, Target target) {
    }

    /* A line of the flight check: what it says, its colour's kind and its mark. */
    private record Check(Text line, int kind) {

        private static final int GREEN = 0;
        private static final int AMBER = 1;
        private static final int RED = 2;

        static Check green(final Text line) {
            return new Check(line, GREEN);
        }

        static Check amber(final Text line) {
            return new Check(line, AMBER);
        }

        static Check red(final Text line) {
            return new Check(line, RED);
        }

        boolean met() {
            return this.kind == GREEN;
        }

        boolean red() {
            return this.kind == RED;
        }

        int colour(final ShipDesignArt.Colours c) {
            return this.kind == GREEN ? c.green() : this.kind == AMBER ? c.amber() : c.red();
        }

        String mark() {
            return this.kind == GREEN ? "+" : this.kind == AMBER ? "!" : "x";
        }
    }
}
