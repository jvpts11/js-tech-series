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
 * to the green node of a joint: a hologram of it follows the mouse in cyan, stands where it would be set in green over
 * a joint it snaps to, and turns red over one it does not fit, the words by the mouse saying which and why; a section
 * taken off the spine leaves a gap till it is set down again. The parts are shown by kind and by maker, each card with
 * its own model; the chosen part's figures under them; at the right the flight check, the ship's figures, the room
 * left in its hull, its finish and what the dock needs; along the bottom the keys. The right mouse button turns the
 * view round the ship and the wheel brings it nearer. Every change is sent to the shipyard, which keeps the design;
 * E sends it to the dock to be built once it is ready.
 */
public final class ShipDesignScreen extends AbstractContainerScreen<ShipDesignMenu> {

    private final ShipyardTerminalBlockEntity terminal;
    private final ShipDesignView view = new ShipDesignView();
    /* The faces of the ship's models as last drawn, which the mouse is over or not. */
    private @Nullable ShipDesignMesh mesh;
    private ShipDesign design;
    private Map<ResourceLocation, ShipPart> parts = Map.of();
    private Map<ResourceLocation, ShipKit> kits = Map.of();
    private Map<ResourceLocation, ShipMaker> makers = Map.of();
    private List<ResourceLocation> finishes = List.of();
    private int tab = MODULES_TAB;
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
    /* How near a node, in the frame's points, a part must be let go to snap to it, the snap on and off. */
    private static final double SNAP_REACH = 12;
    private static final double FREE_REACH = 4;
    /* The small letters' size against the game's; what ends a note cut short. */
    private static final float SMALL = 0.75F;
    private static final String MORE_DOTS = "...";
    private static final long SAID_MILLIS = 3_000;
    private static final float TURN_SPEED = 0.5F;
    private static final double ZOOM_STEP = 1.12;
    /* The words by the mouse: how far past it they end, how far under it they stand, over the panels' cards. */
    private static final int CALLOUT_PAST = 18;
    private static final int CALLOUT_UNDER = 6;
    private static final float CALLOUT_DEPTH = 200;
    /* How far right of the mouse a part's name under it begins. */
    private static final int NAMED_PAST = 11;
    /*
     * The measures' tags: how far back of the footprint's side the length's begins, how far on the span's, how much
     * wider than its name a tag is, and how far from the panels it is kept.
     */
    private static final int LENGTH_BACK = 70;
    private static final int SPAN_ON = 8;
    private static final int TAG_EXTRA = 18;
    private static final int TAG_ROOM = 4;
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
    /* The scales of the ship's figures' bars: 60 tonnes, 600 kilonewtons, twice its weight, four aboard. */
    private static final double SHIP_TONNES = 60;
    private static final double SHIP_KILONEWTONS = 600;
    private static final double SHIP_TWR = 2;
    private static final double SHIP_CREW = 4;
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
        final Ghost ghost = ghost(mouseX, mouseY);
        final List<ShipPartModels.Placed> drawn = drawnModels();
        // With nothing held, the part under the mouse is outlined by its own silhouette, its name beside it.
        final int under = this.held == null ? partAt(drawn, mouseX, mouseY) : -1;
        final List<float[]> outline = under >= 0 && this.mesh != null ? this.mesh.outline(under, this.view.eye())
                : List.of();
        this.view.draw(g, this.width, this.height, this.terminal, drawn, outline,
                ShipDesignArt.PALETTE.get().accent(), ghost.models(), ghost.at(), ghost.colour(), FLOOR_Y);
        footprint(g);
        nodes(g);
        super.render(g, mouseX, mouseY, partialTick);
        if (!ghost.words().isEmpty()) {
            callout(g, ghost, mouseX, mouseY);
        } else if (under >= 0) {
            named(g, under, mouseX, mouseY);
        }
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The ship is drawn through the middle of the screen: nothing dims it, only the panels.
        renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override
    protected void init() {
        this.scale = frameScale(this.width, this.height);
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
            if (clickTop(x, y) || clickParts(x, y) || ShipDesignLayout.onPanel(x, y)) {
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

    /*
     * The part held as the view draws it, and the words by the mouse: set where it would be, green, over a joint it
     * snaps to; red over a joint it does not fit, saying why; cyan under the mouse otherwise.
     */
    private Ghost ghost(final int mouseX, final int mouseY) {
        final Held h = this.held;
        final ShipPart part = h == null ? null : this.parts.get(h.id());
        if (h == null || part == null) {
            return Ghost.NONE;
        }
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        if (this.target != null) {
            return new Ghost(heldModels(), Vec3.ZERO, c.holoFits(), GameText.resolve(ShipDesignTexts.SNAPS_HERE),
                    c.node());
        }
        final ShipDesign alone = alone(h.id(), part);
        final List<ShipPartModels.Placed> models = ShipPartModels.of(this.parts, this.kits, alone);
        final Node wrong = wrong(mouseX, mouseY, h);
        if (wrong != null) {
            return new Ghost(models, wrongAt(wrong, part, alone), c.holoBad(), GameText.resolve(why(h.id(), part)),
                    c.red());
        }
        final Vec3 under = this.view.under(mouseX, mouseY, this.width, this.height);
        return new Ghost(models, under.subtract(middle(alone)), c.holo(), GameText.resolve(
                ShipDesignTexts.DRAG_TO_GREEN.with(Text.of(spoken(h.id())))), c.accent());
    }

    /*
     * The words by the mouse over the part numbered {@code part}: its name, and its maker with what a click of the
     * tool does to it, beside the mouse, kept over the view between the panels.
     */
    private void named(final GuiGraphics g, final int part, final int mouseX, final int mouseY) {
        final ResourceLocation id = idOf(part);
        if (id == null) {
            return;
        }
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final String click = GameText.resolve(switch (this.tool) {
            case REMOVE -> ShipDesignTexts.CLICK_REMOVE;
            case FLIP -> ShipDesignTexts.CLICK_FLIP;
            case FINISH -> ShipDesignTexts.CLICK_FINISH;
            default -> ShipDesignTexts.CLICK_PICK;
        });
        final ShipPart known = this.parts.get(id);
        final Optional<String> maker = known == null ? Optional.empty() : known.maker().map(this.makers::get)
                .map(made -> Component.translatable(made.name()).getString());
        final String line = maker.map(by -> GameText.resolve(ShipDesignTexts.MAKER_AND_CLICK.with(Text.of(by),
                Text.of(click)))).orElse(click);
        final String name = spoken(id);
        final int w = Math.max(ShipDesignArt.calloutWidth(this.font, name), ShipDesignArt.calloutWidth(this.font,
                line));
        final int right = Math.round(guiX(ShipDesignLayout.RIGHT_X0)) - 2;
        final int x0 = Math.max(Math.round(guiX(ShipDesignLayout.LEFT_X1)) + 2, Math.min(mouseX + NAMED_PAST,
                right - w));
        g.pose().pushPose();
        g.pose().translate(0, 0, CALLOUT_DEPTH);
        ShipDesignArt.namedCallout(g, this.font, name, line, x0, mouseY + CALLOUT_UNDER, c.accent(), c.text(),
                c.dim());
        g.pose().popPose();
    }

    /* The part numbered {@code part}, the spine's from the nose first, then the hung ones; none past them. */
    private @Nullable ResourceLocation idOf(final int part) {
        final int sections = this.design.spine().size();
        if (part < 0 || part >= sections + this.design.hung().size()) {
            return null;
        }
        return part < sections ? this.design.spine().get(part) : this.design.hung().get(part - sections);
    }

    /* The words of {@code ghost} by the mouse: under it, ending just past it, kept over the view between the panels. */
    private void callout(final GuiGraphics g, final Ghost ghost, final int mouseX, final int mouseY) {
        final int w = ShipDesignArt.calloutWidth(this.font, ghost.words());
        final int left = Math.round(guiX(ShipDesignLayout.LEFT_X1)) + 2;
        final int right = Math.round(guiX(ShipDesignLayout.RIGHT_X0)) - 2;
        final int x0 = Math.max(left, Math.min(right, mouseX + CALLOUT_PAST) - w);
        g.pose().pushPose();
        g.pose().translate(0, 0, CALLOUT_DEPTH);
        ShipDesignArt.callout(g, this.font, ghost.words(), x0, mouseY + CALLOUT_UNDER, ghost.wordsColour());
        g.pose().popPose();
    }

    /* The joint nearest the mouse, within the snap's reach, that {@code h}'s part does not fit; none for none. */
    private @Nullable Node wrong(final double mouseX, final double mouseY, final Held h) {
        final List<Node> fits = targets(h);
        final double reach = (this.snap ? SNAP_REACH : FREE_REACH) * this.scale;
        final int[] drawn = fronts(this.design.spine());
        Node best = null;
        double nearest = Double.MAX_VALUE;
        for (int j = 0; j < drawn.length; j++) {
            final int z = drawn[j];
            if (fits.stream().anyMatch(node -> node.x() == 0 && node.y() == JOINT_Y && node.z() == z)) {
                continue;
            }
            final Optional<float[]> at = this.view.project(0, JOINT_Y, z, this.width, this.height);
            if (at.isEmpty()) {
                continue;
            }
            final double away = Math.hypot(at.get()[0] - mouseX, at.get()[1] - mouseY);
            if (away < reach && away < nearest) {
                nearest = away;
                best = new Node(0, JOINT_Y, z, new Target(false, j));
            }
        }
        return best;
    }

    /*
     * Where the part held over the joint {@code wrong} it does not fit stands: a section over the section that begins
     * there, as the approved design draws a module held on the nose over the cockpit, or behind the tail's joint; a
     * hung part on the joint.
     */
    private Vec3 wrongAt(final Node wrong, final ShipPart part, final ShipDesign alone) {
        if (part.length() <= 0) {
            return new Vec3(wrong.x(), wrong.y(), wrong.z()).subtract(middle(alone));
        }
        final int[] drawn = fronts(this.design.spine());
        final int joint = wrong.target().at();
        return new Vec3(0, 0, joint + 1 < drawn.length ? (drawn[joint] + drawn[joint + 1] - part.length()) / 2.0
                : wrong.z());
    }

    /* Why the part {@code id} does not fit a joint of the spine it is held over. */
    private Text why(final ResourceLocation id, final ShipPart part) {
        if (part.place() == ShipPlace.NOSE) {
            return ShipDesignTexts.COCKPIT_ONLY_AT_NOSE.text();
        }
        if (part.length() > 0) {
            return ShipDesignTexts.ONLY_COCKPIT_AT_NOSE.text();
        }
        return ShipDesignTexts.NOT_IN_SPINE.with(Text.of(spoken(id)));
    }

    /* The middle of what {@code design} lays, in its own frame, as its models stand. */
    private Vec3 middle(final ShipDesign design) {
        final ShipLayout.Laid alone = ShipyardLayout.laid(this.parts, this.kits, design);
        if (alone.cells().isEmpty()) {
            return Vec3.ZERO;
        }
        final int[] box = bounds(alone.cells().keySet().stream().toList());
        return new Vec3((box[0] + box[1] + 1) / 2.0, (box[2] + box[3] + 1) / 2.0, (box[4] + box[5] + 1) / 2.0);
    }

    /* A design of the part {@code id} alone: a section in a spine of its own, or a hung part on none. */
    private static ShipDesign alone(final ResourceLocation id, final ShipPart part) {
        return part.length() > 0 ? new ShipDesign("", List.of(id), List.of())
                : new ShipDesign("", List.of(), List.of(id));
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
        // Each measure's tag beside its side of the footprint, kept over the view between the panels.
        final int left = Math.round(guiX(ShipDesignLayout.LEFT_X1)) + TAG_ROOM;
        final int right = Math.round(guiX(ShipDesignLayout.RIGHT_X0)) - TAG_ROOM;
        final String lengthName = up(ShipDesignTexts.LENGTH);
        final String spanName = up(ShipDesignTexts.SPAN);
        ShipDesignArt.tag(g, this.font, Math.max(left, Math.min(right - tagWidth(lengthName),
                Math.round((b.get()[0] + cc.get()[0]) / 2) - LENGTH_BACK)), Math.round((b.get()[1] + cc.get()[1]) / 2)
                - 4, lengthName, GameText.resolve(ShipDesignTexts.BLK.with(length)), true);
        ShipDesignArt.tag(g, this.font, Math.max(left, Math.min(right - tagWidth(spanName),
                Math.round((a.get()[0] + b.get()[0]) / 2) + SPAN_ON)), Math.round((a.get()[1] + b.get()[1]) / 2) + 4,
                spanName, GameText.resolve(ShipDesignTexts.BLK.with(span)), true);
    }

    /* How wide a measure's tag named {@code name} is. */
    private int tagWidth(final String name) {
        return this.font.width(name) + TAG_EXTRA;
    }

    /*
     * The green nodes of the joints the held part snaps to, and only those, the one it snaps to bigger; none while
     * nothing is held.
     */
    private void nodes(final GuiGraphics g) {
        final Held h = this.held;
        if (h == null) {
            return;
        }
        for (final Node node : targets(h)) {
            final Optional<float[]> at = this.view.project(node.x(), node.y(), node.z(), this.width, this.height);
            at.ifPresent(p -> ShipDesignArt.node(g, Math.round(p[0]), Math.round(p[1]),
                    this.target != null && node.target().equals(this.target)));
        }
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
        final int part = partAt(drawnModels(), mouseX, mouseY);
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

    /*
     * The number of the part of the ship whose own faces the mouse is over, the nearest, the spine's from the nose
     * then the hung parts'; -1 for none, or the mouse over a panel.
     */
    private int partAt(final List<ShipPartModels.Placed> drawn, final double mouseX, final double mouseY) {
        if (ShipDesignLayout.onPanel(frameX(mouseX), frameY(mouseY))) {
            return -1;
        }
        final ShipDesignMesh faces = ShipDesignMesh.of(this.terminal, drawn, this.mesh);
        this.mesh = faces;
        final Vec3[] line = this.view.ray(mouseX, mouseY, this.width, this.height);
        return faces.partAt(line[0], line[1]);
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
        // A hung part is refused before the spine is read: a design may hang parts on a spine with no section yet.
        if (hung) {
            say(ShipDesignTexts.NO_FLIP.text());
            return;
        }
        final ShipPart part = this.parts.get(this.design.spine().get(index));
        if (part == null || part.place() == ShipPlace.NOSE) {
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

    /* The top: the screen's tag, the ship's name, its length and mass, the tools as icons, the chosen one named. */
    private void top(final GuiGraphics g) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        ShipDesignArt.panel(g, this.font, ShipDesignLayout.MIDDLE_X0, ShipDesignLayout.PANEL_Y0,
                ShipDesignLayout.MIDDLE_X1, ShipDesignLayout.TOP_Y1, "", ShipDesignLayout.TOP_CUT);
        ShipDesignArt.chamfer(g, ShipDesignLayout.MIDDLE_X0, ShipDesignLayout.PANEL_Y0, ShipDesignLayout.TAG_X1,
                ShipDesignLayout.TAG_Y1, c.accent(), ShipDesignLayout.TOP_CUT, true, false, false);
        Draw.text(g, this.font, up(ShipDesigner.DESIGN), ShipDesignLayout.TAG_TEXT_X, ShipDesignLayout.TITLE_Y,
                c.dark());
        final String name = this.naming != null ? this.naming + (Util.getMillis() / 500 % 2 == 0 ? "_" : "")
                : this.design.name();
        Draw.text(g, this.font, name, ShipDesignLayout.NAME_X0, ShipDesignLayout.TITLE_Y,
                this.naming != null ? c.accent() : c.text());
        ShipDesignArt.small(g, this.font, GameText.resolve(ShipDesignTexts.SIZE_LINE.with(length(),
                tenth(this.mass / KILO))), ShipDesignLayout.NAME_X0 + this.font.width(name)
                + ShipDesignLayout.FIGURES_GAP, ShipDesignLayout.TITLE_Y + 1, c.dim());
        for (int i = 0; i < Tool.ORDER.size(); i++) {
            final Tool each = Tool.ORDER.get(i);
            final boolean on = each == Tool.MIRROR || each == Tool.SNAP && this.snap
                    || each != Tool.SNAP && each == this.tool;
            ShipDesignArt.toolButton(g, ShipDesignLayout.toolX(i), ShipDesignLayout.TOOLS_Y0,
                    ShipDesignLayout.TOOL_W, ShipDesignLayout.TOOL_H, each.icon, on);
        }
        Draw.text(g, this.font, GameText.resolve(this.tool.name), ShipDesignLayout.toolX(Tool.ORDER.size()) + 2,
                ShipDesignLayout.TOOL_NAME_Y, c.text());
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
        final String searched = this.search.isEmpty() && !this.searching ? GameText.resolve(ShipDesignTexts.SEARCH)
                : this.search + (this.searching ? "_" : "");
        Draw.text(g, this.font, searched, ShipDesignLayout.SEARCH_X0 + 16, ShipDesignLayout.SEARCH_Y0 + 2,
                this.search.isEmpty() && !this.searching ? c.faint() : c.text());
        for (int i = 0; i < ShipDesignLayout.TABS; i++) {
            final int tx = ShipDesignLayout.tabX(i);
            final int ty = ShipDesignLayout.TAB_Y0;
            if (i == this.tab) {
                ShipDesignArt.chamfer(g, tx, ty, tx + ShipDesignLayout.TAB_W, ty + ShipDesignLayout.TAB_SIZE,
                        c.accent(), 3, true, false, false);
                ShipDesignArt.icon(g, KIND_ICONS[i], tx + 2, ty + 3, ShipDesignArt.ICON_SMALL, c.dark(), c.dark());
            } else {
                g.fill(tx, ty, tx + ShipDesignLayout.TAB_W, ty + ShipDesignLayout.TAB_SIZE, c.panelHi());
                ShipDesignArt.icon(g, KIND_ICONS[i], tx + 2, ty + 3, ShipDesignArt.ICON_SMALL, c.dim(), c.accent());
            }
        }
        Draw.text(g, this.font, GameText.resolve(KIND_NAMES[this.tab]), ShipDesignLayout.CARD_X0,
                ShipDesignLayout.KIND_Y, c.text());
        final List<ResourceLocation> shelf = shelf();
        final int shown = ShipDesignLayout.CARD_COLUMNS * ShipDesignLayout.CARD_ROWS;
        for (int i = 0; i < shown; i++) {
            final int index = this.scroll * ShipDesignLayout.CARD_COLUMNS + i;
            if (index < shelf.size()) {
                card(g, shelf.get(index), ShipDesignLayout.cardX(i), ShipDesignLayout.cardY(i));
            }
        }
        final int more = shelf.size() - this.scroll * ShipDesignLayout.CARD_COLUMNS - shown;
        if (more > 0) {
            ShipDesignArt.small(g, this.font, GameText.resolve(ShipDesignTexts.MORE.with(more)),
                    ShipDesignLayout.MORE_X, ShipDesignLayout.MORE_Y, c.dim());
        }
        chosenPart(g);
    }

    /* A part's card: its model over its name and its maker's, framed when chosen. */
    private void card(final GuiGraphics g, final ResourceLocation id, final int x0, final int y0) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final ShipPart part = this.parts.get(id);
        final boolean on = id.equals(this.chosen);
        final int x1 = x0 + ShipDesignLayout.CARD_W;
        final int y1 = y0 + ShipDesignLayout.CARD_H;
        g.fill(x0, y0, x1, y1, on ? c.cardOn() : c.card());
        ShipDesignArt.frame(g, x0, y0, x1, y1, on ? c.accent() : c.line());
        picture(g, id, x0 + 1, y0 + 1, x1 - 1, y0 + 1 + ShipDesignLayout.CARD_PICTURE_H);
        // A name too long for the card in the game's letters is written in the small ones.
        final int room = ShipDesignLayout.CARD_W - 3;
        final String name = spoken(id);
        if (this.font.width(name) <= room) {
            Draw.text(g, this.font, name, x0 + 2, y0 + ShipDesignLayout.CARD_PICTURE_H + 2, c.text());
        } else {
            ShipDesignArt.small(g, this.font, fit(name, room, true), x0 + 2, y0 + ShipDesignLayout.CARD_PICTURE_H + 3,
                    c.text());
        }
        if (part != null) {
            // A maker's name too long for the card is known by its first word, as the yards are.
            final String by = part.maker().map(this::spokenMaker).orElse("");
            final String shown = ShipDesignArt.smallWidth(this.font, by) <= room ? by : by.split(" ")[0];
            ShipDesignArt.small(g, this.font, fit(shown, room, true), x0 + 2,
                    y0 + ShipDesignLayout.CARD_PICTURE_H + 11, c.dim());
        }
    }

    /* A part's own model on its card, seen from above and aside, turning slowly while it is chosen. */
    private void picture(final GuiGraphics g, final ResourceLocation id, final int x0, final int y0, final int x1,
                         final int y1) {
        final ShipPart part = this.parts.get(id);
        if (part == null) {
            return;
        }
        final ShipDesign alone = alone(id, part);
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

    /* The chosen part's name over its figures, its length, mass, blocks and power, and its note. */
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
        final ShipPart.Figures f = part.figures();
        final int blocks = this.kits.containsKey(id) ? ShipyardLayout.blocks(this.kits.get(id)) : 0;
        figure(g, 0, ShipDesignTexts.LENGTH, GameText.resolve(ShipDesignTexts.BLK.with((int) part.length())));
        figure(g, 1, ShipDesignTexts.MASS, GameText.resolve(ShipDesignTexts.TONNES.with(tenth(f.mass() / KILO))));
        figure(g, 2, ShipDesignTexts.BLOCKS, String.valueOf(blocks));
        figure(g, 3, ShipDesignTexts.POWER, GameText.resolve(ShipDesignTexts.MEGAWATTS.with(
                tenth(f.power() / MEGA))));
        final String noteKey = SmallShipNotes.keyOf(part.name());
        if (I18n.exists(noteKey)) {
            final int wide = Math.round((x1 - x0) / SMALL);
            final List<FormattedCharSequence> rows = this.font.split(Component.translatable(noteKey), wide);
            for (int i = 0; i < Math.min(rows.size(), ShipDesignLayout.NOTE_ROWS); i++) {
                g.pose().pushPose();
                g.pose().translate(x0, ShipDesignLayout.NOTE_Y + i * ShipDesignLayout.NOTE_STEP, 0);
                g.pose().scale(SMALL, SMALL, 1);
                if (i == ShipDesignLayout.NOTE_ROWS - 1 && rows.size() > ShipDesignLayout.NOTE_ROWS) {
                    // The rest of a note longer than its lines, cut, its last line saying there is more.
                    final String last = rowText(rows.get(i));
                    String cut = last;
                    while (!cut.isEmpty() && this.font.width(cut + MORE_DOTS) > wide) {
                        cut = cut.substring(0, cut.length() - 1);
                    }
                    Draw.text(g, this.font, cut.stripTrailing() + MORE_DOTS, 0, 0, c.dim());
                } else {
                    Draw.text(g, this.font, rows.get(i), 0, 0, c.dim());
                }
                g.pose().popPose();
            }
        }
    }

    /* The chosen part's figure {@code row}: its name, and its value at the right. */
    private void figure(final GuiGraphics g, final int row, final TextKey name, final String value) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final int y = ShipDesignLayout.figureY(row);
        Draw.text(g, this.font, GameText.resolve(name), ShipDesignLayout.CARD_X0, y, c.dim());
        right(g, value, ShipDesignLayout.CHOSEN_X1, y, c.text());
    }

    /* The right: the flight check, the room in the hull among its lines; the ship's figures; the dock. */
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
            ShipDesignArt.small(g, this.font, check.mark(), x0 + 4 - ShipDesignArt.smallWidth(this.font,
                    check.mark()) / 2, y + 1, colour);
            Draw.text(g, this.font, fit(GameText.resolve(check.line()), x1 - ShipDesignLayout.CHECK_TEXT_X, false),
                    ShipDesignLayout.CHECK_TEXT_X, y, check.met() ? c.text() : colour);
        }
        ShipDesignArt.heading(g, this.font, x0, ShipDesignLayout.SHIP_Y, up(ShipDesignTexts.THE_SHIP), x1);
        final double twr = this.mass > 0 ? this.lift / (this.mass * STANDARD_GRAVITY) : 0;
        shipFigure(g, 0, ShipDesignTexts.MASS, GameText.resolve(ShipDesignTexts.TONNES.with(tenth(this.mass / KILO))),
                this.mass / KILO / SHIP_TONNES, c.accent());
        shipFigure(g, 1, ShipDesignTexts.THRUST, GameText.resolve(ShipDesignTexts.KILONEWTONS.with(
                Math.round(this.thrust / KILO))), this.thrust / KILO / SHIP_KILONEWTONS, c.accent());
        shipFigure(g, 2, ShipDesignTexts.VTOL_TWR, String.format(Locale.ROOT, "%.2f", twr), twr / SHIP_TWR,
                c.accent());
        shipFigure(g, 3, ShipDesignTexts.POWER, GameText.resolve(ShipDesignTexts.POWER_OF.with(
                tenth(this.taken / MEGA), tenth(this.made / MEGA))), this.made > 0 ? this.taken / this.made : 1,
                c.amber());
        shipFigure(g, 4, ShipDesignTexts.CREW, GameText.resolve(ShipDesignTexts.CREW_LINE.with(this.seats,
                this.beds)), (this.seats + this.beds) / SHIP_CREW, c.accent());
        ShipDesignArt.heading(g, this.font, x0, ShipDesignLayout.DOCK_Y, up(ShipDesignTexts.THE_DOCK), x1);
        final int blocks = this.laid.cells().size();
        final int held = this.menu.held();
        final int dy = ShipDesignLayout.DOCK_Y;
        Draw.text(g, this.font, GameText.resolve(ShipDesignTexts.BLOCKS), x0, dy + 9, c.dim());
        right(g, String.valueOf(blocks), x1, dy + 9, c.text());
        Draw.text(g, this.font, GameText.resolve(ShipDesignTexts.IN_THE_PORT), x0, dy + 20, c.dim());
        right(g, String.valueOf(held), x1, dy + 20, c.text());
        ShipDesignArt.bar(g, x0, dy + 29, x1 - x0, blocks > 0 ? held / (double) blocks : 0, c.amber());
        Draw.text(g, this.font, GameText.resolve(ShipDesignTexts.TO_BRING.with(Math.max(0, blocks - held))), x0,
                dy + 36, c.amber());
    }

    /* The ship's figure {@code row}: its name, its value at the right, its bar under them. */
    private void shipFigure(final GuiGraphics g, final int row, final TextKey name, final String value,
                            final double share, final int colour) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        final int y = ShipDesignLayout.shipY(row);
        Draw.text(g, this.font, GameText.resolve(name), ShipDesignLayout.INNER_X0, y, c.dim());
        right(g, value, ShipDesignLayout.INNER_X1, y, c.text());
        ShipDesignArt.bar(g, ShipDesignLayout.INNER_X0, y + 9, ShipDesignLayout.INNER_X1 - ShipDesignLayout.INNER_X0,
                Math.min(1, share), colour);
    }

    /* The bottom: the keys; and over them, a while, what the screen last said. */
    private void bottom(final GuiGraphics g) {
        final ShipDesignArt.Colours c = ShipDesignArt.PALETTE.get();
        ShipDesignArt.panel(g, this.font, ShipDesignLayout.MIDDLE_X0, ShipDesignLayout.BOTTOM_Y0,
                ShipDesignLayout.MIDDLE_X1, ShipDesignLayout.BOTTOM_Y1, "", ShipDesignLayout.BOTTOM_CUT);
        int x = ShipDesignLayout.KEYS_X0;
        x += ShipDesignArt.keycap(g, this.font, x, ShipDesignLayout.KEYS_Y0, keyName(GLFW.GLFW_KEY_E),
                GameText.resolve(ShipDesignTexts.SEND)) + ShipDesignLayout.KEY_GAP;
        x += ShipDesignArt.keycap(g, this.font, x, ShipDesignLayout.KEYS_Y0, keyName(GLFW.GLFW_KEY_S),
                GameText.resolve(ShipDesignTexts.SAVE)) + ShipDesignLayout.KEY_GAP;
        x += ShipDesignArt.keycap(g, this.font, x, ShipDesignLayout.KEYS_Y0, keyName(GLFW.GLFW_KEY_F),
                GameText.resolve(ShipDesignTexts.FINISH_TOOL)) + ShipDesignLayout.KEY_GAP;
        ShipDesignArt.keycap(g, this.font, x, ShipDesignLayout.KEYS_Y0, GameText.resolve(ShipDesignTexts.KEY_DELETE),
                GameText.resolve(ShipDesignTexts.DELETE));
        if (this.said != null && Util.getMillis() < this.saidUntil) {
            final String words = GameText.resolve(this.said);
            final int w = ShipDesignArt.calloutWidth(this.font, words);
            ShipDesignArt.callout(g, this.font, words, (ShipDesignLayout.MIDDLE_X0 + ShipDesignLayout.MIDDLE_X1 - w)
                    / 2, ShipDesignLayout.SAID_Y, c.accent());
        }
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
        for (int i = 0; i < Tool.ORDER.size(); i++) {
            if (x >= ShipDesignLayout.toolX(i) && x < ShipDesignLayout.toolX(i) + ShipDesignLayout.TOOL_W) {
                final Tool each = Tool.ORDER.get(i);
                switch (each) {
                    case MIRROR -> say(ShipDesignTexts.PAIRS.text());
                    case SNAP -> this.snap = !this.snap;
                    default -> this.tool = each;
                }
                return true;
            }
        }
        return false;
    }

    /* A click on the parts: a tab, or a card to take up a part from. */
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

    private boolean onName(final double x, final double y) {
        return x >= ShipDesignLayout.NAME_X0 && x < ShipDesignLayout.NAME_X1 && y >= ShipDesignLayout.PANEL_Y0
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
        out.add(ofKind(ShipPartKind.MAIN_ENGINE).isEmpty() ? Check.red(ShipDesignTexts.NO_MAIN.text())
                : Check.green(ShipDesignTexts.MAIN_OK.text()));
        out.add(ofKind(ShipPartKind.AIR_ENGINE).isEmpty() ? Check.amber(ShipDesignTexts.NO_AIR.text())
                : Check.green(ShipDesignTexts.AIR_OK.text()));
        out.add(ofKind(ShipPartKind.GEAR).isEmpty() ? Check.red(ShipDesignTexts.NO_GEAR.text())
                : Check.green(ShipDesignTexts.GEAR_OK.text()));
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

    /*
     * The parts on the shelf: the chosen kind's whose name or maker's name holds the search, in the catalogue's
     * order.
     */
    private List<ResourceLocation> shelf() {
        final String wanted = this.search.toString().toLowerCase(Locale.ROOT);
        final List<ResourceLocation> out = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, ShipPart> entry : this.parts.entrySet()) {
            final ShipPart part = entry.getValue();
            if (group(part.kind()) != this.tab) {
                continue;
            }
            final String by = part.maker().map(this::spokenMaker).orElse("");
            if (!wanted.isEmpty() && !spoken(entry.getKey()).toLowerCase(Locale.ROOT).contains(wanted)
                    && !by.toLowerCase(Locale.ROOT).contains(wanted)) {
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

    /* The name of the part {@code id} as it is written, for the words by the mouse. */
    private String spoken(final ResourceLocation id) {
        final ShipPart part = this.parts.get(id);
        return part == null ? id.getPath() : Component.translatable(part.name()).getString();
    }

    /* The name of the maker {@code id} as it is written. */
    private String spokenMaker(final ResourceLocation id) {
        final ShipMaker made = this.makers.get(id);
        return made == null ? id.getPath() : Component.translatable(made.name()).getString();
    }

    private void say(final Text text) {
        this.said = text;
        this.saidUntil = Util.getMillis() + SAID_MILLIS;
    }

    /* {@code text} in the game's letters, ending at {@code x1}. */
    private void right(final GuiGraphics g, final String text, final int x1, final int y, final int colour) {
        Draw.text(g, this.font, text, x1 - this.font.width(text), y, colour);
    }

    /* The letters of {@code row}, a line the game's font laid out. */
    private static String rowText(final FormattedCharSequence row) {
        final StringBuilder out = new StringBuilder();
        row.accept((index, style, letter) -> {
            out.appendCodePoint(letter);
            return true;
        });
        return out.toString();
    }

    /* {@code text} cut to fit {@code width} points, in the small letters or the game's. */
    private String fit(final String text, final int width, final boolean small) {
        String out = text;
        while (!out.isEmpty() && (small ? ShipDesignArt.smallWidth(this.font, out) : this.font.width(out)) > width) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    /*
     * How many of the interface's points a point of the frame is on a screen {@code width} by {@code height} of
     * them: whole points where the frame fits, so its letters are the game's own size or a whole times it, never
     * smaller; less only on a screen too small to hold it.
     */
    private static float frameScale(final int width, final int height) {
        final double room = Math.min(width / (double) ShipDesignLayout.WIDTH,
                height / (double) ShipDesignLayout.HEIGHT);
        return (float) (room >= 1 ? Math.floor(room) : room);
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

        /* The buttons left to right across the top, named so a new tool never moves the others by its place. */
        private static final List<Tool> ORDER = List.of(PLACE, REMOVE, FLIP, MIRROR, FINISH, SNAP);

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

    /* The part held as drawn: its models, moved by {@code at}, in {@code colour}; and the words by the mouse. */
    private record Ghost(List<ShipPartModels.Placed> models, Vec3 at, int colour, String words, int wordsColour) {

        static final Ghost NONE = new Ghost(List.of(), Vec3.ZERO, 0, "", 0);
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
