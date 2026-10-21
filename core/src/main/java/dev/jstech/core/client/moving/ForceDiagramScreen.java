/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.moving;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.api.client.GpuSurface;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.model.kit.SceneRenderer;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.moving.ForceDiagramLayout;
import dev.jstech.core.moving.ForceDiagramTexts;
import dev.jstech.core.moving.StructureForcesPayload;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureReadings;
import dev.jstech.core.moving.physics.ForceSum;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The diagram of forces of a moving structure, as the approved design draws it: the structure itself, turned as it
 * stands, over the ground below it, with an arrow for each force on it where it acts and one for their sum at its
 * centre of mass, its height above the ground and the axes; the list of the forces, each with what it gives now and a
 * switch that hides its arrow; and what it weighs, where the sum pushes it, how fast it climbs and goes, and the air
 * round it. The view turns with the mouse dragged over it and zooms with the wheel. While it is open the server tells
 * the forces a few times a second. Smaller windows draw it smaller, whole.
 */
public final class ForceDiagramScreen extends Screen {

    private final ClientStructure structure;
    private final List<Button> buttons = new ArrayList<>();
    private final Set<ForceSum.ForceKind> hidden = EnumSet.noneOf(ForceSum.ForceKind.class);
    private final Vector3f projected = new Vector3f();
    private final UnitFormatter format = UnitFormatter.forCurrentLocale();
    @Nullable
    private GpuSurface surface;
    @Nullable
    private Button apart;
    private Angle angle = Angle.THREE_D;
    private boolean summed;
    private boolean dragging;
    private boolean repaint = true;
    private float yaw;
    private float pitch;
    private float zoom = 1;
    private float fit = 1;
    private int left;
    private int top;
    private int ticks;

    /** The forces the list shows, in its order; balloons are listed with buoyancy, which they are. */
    private static final List<ForceSum.ForceKind> LISTED = List.of(ForceSum.ForceKind.GRAVITY,
            ForceSum.ForceKind.BUOYANCY, ForceSum.ForceKind.THRUST, ForceSum.ForceKind.DRAG, ForceSum.ForceKind.LIFT,
            ForceSum.ForceKind.LEVITATION, ForceSum.ForceKind.MAGNETISM);
    /* The icon of each force on the list, seven pixels a side. */
    private static final String[] GRAVITY_ICON = {"...#...", "...#...", "...#...", ".#####.", "..###..", "...#...",
            "#######"};
    private static final String[] BUOYANCY_ICON = {"..###..", ".#####.", ".#####.", "..###..", "...#...", "...#...",
            "..#...."};
    private static final String[] THRUST_ICON = {"#..#...", ".#..#..", "..#..#.", "...#..#", "..#..#.", ".#..#..",
            "#..#..."};
    private static final String[] DRAG_ICON = {".......", "######.", ".......", "..#####", ".......", "#####..",
            "......."};
    private static final String[] LIFT_ICON = {".......", "......#", "....###", "..#####", "#####..", "..#....",
            "......."};
    private static final String[] LEVITATION_ICON = {"..###..", ".#...#.", "..###..", ".......", "#.#.#.#",
            ".......", ".#.#.#."};
    private static final String[] MAGNETISM_ICON = {"##...##", "##...##", "##...##", "##...##", "##...##",
            ".##.##.", "..###.."};
    private static final float SMALL = ForceDiagramLayout.SMALL;
    /* The space kept round the diagram on a window too small for it, in the GUI's pixels. */
    private static final int MARGIN = 4;
    /* How often the structure is painted again as it moves, in ticks. */
    private static final int REPAINT_EVERY = 4;
    /* How much of the view the structure fills, unzoomed. */
    private static final double FILL = 0.42;
    /* How far below its centre the ground is drawn at most, as a share of the structure's size. */
    private static final double GROUND_SHOWN = 0.9;
    /* The longest arrow, as a share of the structure's size, and the shortest, as a share of the longest. */
    private static final double LONGEST = 0.6;
    private static final double SHORTEST = 0.3;
    /* A part of a force less than this share of it is not named as one of its ways. */
    private static final double QUIET = 0.1;
    /* Slower than this a structure is still, in metres a second, and its climb is level. */
    private static final double STILL = 0.05;
    private static final double TONNE = 1_000;
    /* The arrows' widths and their heads' lengths, in the GUI's pixels, and the axes' length. */
    private static final float SHAFT = 3;
    private static final float HEAD = 7;
    private static final float EDGE = 1;
    private static final float AXIS = 14;
    private static final int AXES_INSET = 18;
    private static final float DOT = 4;
    private static final float ZOOM_STEP = 1.1F;
    private static final float LEAST_ZOOM = 0.25F;
    private static final float MOST_ZOOM = 8;
    private static final float UPRIGHT = 90;
    /* How tall a label stands on its ground, and how far one is moved off another. */
    private static final int LABEL_LINE = 9;

    public ForceDiagramScreen(final ClientStructure structure) {
        super(GameText.component(ForceDiagramTexts.TITLE.with(structure.entity().getName().getString())));
        this.structure = structure;
        aim(Angle.THREE_D);
    }

    @Override
    public void added() {
        StructureForcesClient.look(structure.entity().getId());
    }

    @Override
    public void removed() {
        StructureForcesClient.stopLooking(structure.entity().getId());
        if (surface != null) {
            surface.close();
            surface = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (structure.entity().isRemoved()) {
            onClose();
            return;
        }
        if (++ticks % REPAINT_EVERY == 0) {
            repaint = true;
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        final int x = (int) localX(mouseX);
        final int y = (int) localY(mouseY);
        final ForceDiagramPalette.Colours colours = ForceDiagramPalette.get();
        final StructureForcesPayload told = StructureForcesClient.of(structure.entity().getId());
        g.pose().pushPose();
        g.pose().translate(left, top, 0);
        g.pose().scale(fit, fit, 1);
        window(g, colours);
        view(g, colours, told);
        list(g, colours, told, x, y);
        tiles(g, colours, told);
        for (final Button button : buttons) {
            button.render(g, x, y, partialTick);
        }
        final int chosen = ForceDiagramLayout.VIEW_BUTTON_X + angle.place * ForceDiagramLayout.VIEW_BUTTON_PITCH;
        Draw.outline(g, chosen - 1, ForceDiagramLayout.VIEW_BUTTON_Y - 1, ForceDiagramLayout.VIEW_BUTTON_WIDTH + 2,
                ForceDiagramLayout.VIEW_BUTTON_HEIGHT + 2, colours.chosen());
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = localX(mouseX);
        final double y = localY(mouseY);
        for (final Button each : buttons) {
            if (each.mouseClicked(x, y, button)) {
                return true;
            }
        }
        if (button == 0) {
            final int row = ForceDiagramLayout.rowAt(x, y);
            if (row >= 0) {
                final ForceSum.ForceKind kind = LISTED.get(row);
                if (!hidden.remove(kind)) {
                    hidden.add(kind);
                }
                return true;
            }
            if (ForceDiagramLayout.onView(x, y)) {
                dragging = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        dragging = false;
        for (final Button each : buttons) {
            each.mouseReleased(localX(mouseX), localY(mouseY), button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        if (dragging) {
            yaw += (float) (dragX / fit);
            pitch = Math.max(-UPRIGHT, Math.min(UPRIGHT, pitch + (float) (dragY / fit)));
            repaint = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        if (ForceDiagramLayout.onView(localX(mouseX), localY(mouseY))) {
            zoom = (float) Math.max(LEAST_ZOOM, Math.min(MOST_ZOOM, zoom * Math.pow(ZOOM_STEP, scrollY)));
            repaint = true;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void init() {
        fit = Math.min(1F, Math.min((width - 2F * MARGIN) / ForceDiagramLayout.WIDTH,
                (height - 2F * MARGIN) / ForceDiagramLayout.HEIGHT));
        left = Math.round((width - ForceDiagramLayout.WIDTH * fit) / 2);
        top = Math.round((height - ForceDiagramLayout.HEIGHT * fit) / 2);
        repaint = true;
        buttons.clear();
        for (final Angle each : Angle.values()) {
            buttons.add(Button.builder(GameText.component(each.text), pressed -> aim(each))
                    .bounds(ForceDiagramLayout.VIEW_BUTTON_X + each.place * ForceDiagramLayout.VIEW_BUTTON_PITCH,
                            ForceDiagramLayout.VIEW_BUTTON_Y, ForceDiagramLayout.VIEW_BUTTON_WIDTH,
                            ForceDiagramLayout.VIEW_BUTTON_HEIGHT).build());
        }
        apart = Button.builder(apartText(), pressed -> sumOrPart()).bounds(ForceDiagramLayout.APART_X,
                ForceDiagramLayout.VIEW_BUTTON_Y, ForceDiagramLayout.APART_WIDTH,
                ForceDiagramLayout.VIEW_BUTTON_HEIGHT).build();
        buttons.add(apart);
        buttons.add(Button.builder(GameText.component(ForceDiagramTexts.RESET), pressed -> {
            zoom = 1;
            aim(angle);
        }).bounds(ForceDiagramLayout.RESET_X, ForceDiagramLayout.FOOT_Y, ForceDiagramLayout.RESET_WIDTH,
                ForceDiagramLayout.FOOT_BUTTON_HEIGHT).build());
        buttons.add(Button.builder(GameText.component(ForceDiagramTexts.DONE), pressed -> onClose())
                .bounds(ForceDiagramLayout.DONE_X, ForceDiagramLayout.FOOT_Y, ForceDiagramLayout.DONE_WIDTH,
                        ForceDiagramLayout.FOOT_BUTTON_HEIGHT).build());
    }

    /* Turns the view to {@code chosen}, seen from the structure's bow round as it says. */
    private void aim(final Angle chosen) {
        angle = chosen;
        final Vec3 ahead = ahead();
        yaw = (float) Math.toDegrees(Math.atan2(ahead.x, -ahead.z)) + chosen.turn;
        pitch = chosen.tilt;
        repaint = true;
    }

    private void sumOrPart() {
        summed = !summed;
        if (apart != null) {
            apart.setMessage(apartText());
        }
    }

    private Component apartText() {
        return GameText.component(summed ? ForceDiagramTexts.SUMMED : ForceDiagramTexts.APART);
    }

    /* The window: its ground, the title and its bar, and the bar along the foot. */
    private void window(final GuiGraphics g, final ForceDiagramPalette.Colours colours) {
        g.fill(0, 0, ForceDiagramLayout.WIDTH, ForceDiagramLayout.HEIGHT, colours.body());
        g.fill(0, 0, ForceDiagramLayout.WIDTH, ForceDiagramLayout.TITLE_HEIGHT, colours.titleBar());
        g.fill(0, ForceDiagramLayout.FOOT_Y - 3, ForceDiagramLayout.WIDTH, ForceDiagramLayout.HEIGHT, colours.foot());
        Draw.textCentered(g, font, title, ForceDiagramLayout.WIDTH / 2, ForceDiagramLayout.TITLE_Y, colours.title());
        Draw.outline(g, 0, 0, ForceDiagramLayout.WIDTH, ForceDiagramLayout.HEIGHT, colours.frame());
    }

    /* The view: the sky, the structure over the ground as the surface holds it, and over it the arrows. */
    private void view(final GuiGraphics g, final ForceDiagramPalette.Colours colours,
                      @Nullable final StructureForcesPayload told) {
        final int x = ForceDiagramLayout.VIEW_X;
        final int y = ForceDiagramLayout.VIEW_Y;
        final int w = ForceDiagramLayout.VIEW_WIDTH;
        final int h = ForceDiagramLayout.VIEW_HEIGHT;
        g.fillGradient(x, y, x + w, y + h, colours.skyTop(), colours.skyBottom());
        final double pixel = Minecraft.getInstance().getWindow().getGuiScale() * fit;
        final int wide = Math.max(1, (int) Math.round(w * pixel));
        final int tall = Math.max(1, (int) Math.round(h * pixel));
        if (surface == null || surface.width() != wide || surface.height() != tall) {
            if (surface != null) {
                surface.close();
            }
            surface = new GpuSurface(wide, tall);
            repaint = true;
        }
        final double extent = ForceDiagramPainter.extent(structure);
        final Vec3 centre = told == null ? structure.worldBox().getCenter() : told.centre();
        final double below = told == null ? 0 : Math.min(told.ground(), extent * GROUND_SHOWN);
        final SceneRenderer.View seen = new SceneRenderer.View(0, 0, 0, yaw, pitch,
                (float) (Math.min(wide, tall) * FILL / extent * zoom), 0, 0);
        if (repaint) {
            g.flush();
            SceneRenderer.draw(surface, seen, new ForceDiagramPainter(structure, centre, below, colours.ground(),
                    colours.grid()));
            repaint = false;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        surface.blit(g, x, y, w, h);
        RenderSystem.disableBlend();
        Draw.pushScissor(g, x, y, x + w, y + h);
        final Shown shown = new Shown(seen, wide, tall, pixel);
        if (told == null) {
            small(g, GameText.resolve(ForceDiagramTexts.WAITING), x + w / 2 - smallWidth(GameText.resolve(
                    ForceDiagramTexts.WAITING)) / 2, y + h / 2, colours.text());
        } else {
            arrows(g, colours, told, shown, extent, below);
        }
        axes(g, colours, shown);
        Draw.popScissor(g);
        Draw.outline(g, x - 1, y - 1, w + 2, h + 2, colours.frame());
    }

    /* Every force's arrow where it acts, or the sum's alone, with their labels, the centre and the height. */
    private void arrows(final GuiGraphics g, final ForceDiagramPalette.Colours colours,
                        final StructureForcesPayload told, final Shown shown, final double extent,
                        final double below) {
        final Map<ForceSum.ForceKind, StructureForcesPayload.Force> forces = merged(told);
        final Vec3 sum = told.sum();
        double most = sum.length();
        for (final StructureForcesPayload.Force force : forces.values()) {
            most = Math.max(most, force.push().length());
        }
        final Vec3 centre = told.centre();
        final List<Label> labels = new ArrayList<>();
        g.flush();
        final BufferBuilder quads = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        final Matrix4f matrix = g.pose().last().pose();
        if (!summed) {
            for (final StructureForcesPayload.Force force : forces.values()) {
                if (!hidden.contains(force.kind())) {
                    arrow(quads, matrix, colours, shown, force.at().subtract(centre), force.push(), most, extent,
                            colours.of(force.kind()), labelOf(nameOf(force.kind()), force.push()), labels);
                }
            }
        }
        arrow(quads, matrix, colours, shown, Vec3.ZERO, sum, most, extent, colours.sum(),
                labelOf(ForceDiagramTexts.SUM, sum), labels);
        final Vector3f middle = shown.at(Vec3.ZERO, projected);
        final float cx = middle.x;
        final float cy = middle.y;
        draw(quads);
        final List<int[]> placed = new ArrayList<>();
        height(g, colours, told, shown, below, placed);
        for (final Label label : labels) {
            label(g, colours, label.text, label.x, label.y, label.right, label.colour, placed);
        }
        final int mx = Math.round(cx);
        final int my = Math.round(cy);
        g.fill(mx - 3, my - 3, mx, my, colours.sum());
        g.fill(mx, my - 3, mx + 3, my, colours.mark());
        g.fill(mx - 3, my, mx, my + 3, colours.mark());
        g.fill(mx, my, mx + 3, my + 3, colours.sum());
        Draw.outline(g, mx - 4, my - 4, 8, 8, colours.mark());
        label(g, colours, GameText.resolve(ForceDiagramTexts.CENTRE), mx - 6, my - 4, false, colours.text(),
                placed);
    }

    /* One arrow from {@code from}, in blocks from the centre of mass, along {@code push}, as long as it weighs. */
    private void arrow(final BufferBuilder quads, final Matrix4f matrix, final ForceDiagramPalette.Colours colours,
                       final Shown shown, final Vec3 from, final Vec3 push, final double most, final double extent,
                       final int colour, final String label, final List<Label> labels) {
        final double strength = push.length();
        if (strength <= 0 || most <= 0) {
            return;
        }
        final double length = extent * LONGEST * (SHORTEST + (1 - SHORTEST) * Math.sqrt(strength / most));
        final Vector3f start = new Vector3f(shown.at(from, projected));
        final Vector3f end = new Vector3f(shown.at(from.add(push.scale(length / strength)), projected));
        if (start.distance(end) < 1) {
            return;
        }
        shape(quads, matrix, start.x, start.y, end.x, end.y, SHAFT + 2 * EDGE, HEAD + 2 * EDGE, EDGE,
                colours.mark());
        shape(quads, matrix, start.x, start.y, end.x, end.y, SHAFT, HEAD, 0, colour);
        labels.add(new Label(label, end.x, end.y, end.x >= start.x, colour));
    }

    /* An arrow's label: its name and how strong it is. */
    private String labelOf(final TextKey name, final Vec3 push) {
        return GameText.resolve(ForceDiagramTexts.LABEL.with(GameText.resolve(name), newtons(push.length())));
    }

    /* The dotted line from the centre of mass down to the ground, and how high it is. */
    private void height(final GuiGraphics g, final ForceDiagramPalette.Colours colours,
                        final StructureForcesPayload told, final Shown shown, final double below,
                        final List<int[]> placed) {
        if (below <= 0) {
            return;
        }
        final Vector3f top = new Vector3f(shown.at(Vec3.ZERO, projected));
        final Vector3f foot = new Vector3f(shown.at(new Vec3(0, -below, 0), projected));
        final float length = top.distance(foot);
        for (float along = 0; along < length; along += DOT) {
            final float t = along / length;
            final int px = Math.round(top.x + (foot.x - top.x) * t);
            final int py = Math.round(top.y + (foot.y - top.y) * t);
            g.fill(px, py, px + 1, py + 2, colours.mark());
        }
        final String text = told.ground() >= StructureReadings.LOOKS
                ? GameText.resolve(ForceDiagramTexts.FAR.with(format.decimal(StructureReadings.LOOKS)))
                : GameText.resolve(ForceDiagramTexts.HEIGHT.with(format.decimal(told.ground())));
        label(g, colours, text, (top.x + foot.x) / 2 + 4, (top.y + foot.y) / 2, true, colours.text(), placed);
    }

    /* The axes in the view's corner: up, and the way the structure's bow looks. */
    private void axes(final GuiGraphics g, final ForceDiagramPalette.Colours colours, final Shown shown) {
        final float bx = ForceDiagramLayout.VIEW_X + AXES_INSET;
        final float by = ForceDiagramLayout.VIEW_Y + ForceDiagramLayout.VIEW_HEIGHT - AXES_INSET;
        final Vector3f origin = new Vector3f(shown.at(Vec3.ZERO, projected));
        final Vector3f up = new Vector3f(shown.at(new Vec3(0, 1, 0), projected)).sub(origin);
        final Vector3f ahead = new Vector3f(shown.at(ahead(), projected)).sub(origin);
        g.flush();
        final BufferBuilder quads = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        final Matrix4f matrix = g.pose().last().pose();
        final List<Label> labels = new ArrayList<>();
        axis(quads, matrix, colours, bx, by, ahead, colours.axisAhead(), ForceDiagramTexts.FORWARD, labels);
        axis(quads, matrix, colours, bx, by, up, colours.axisUp(), ForceDiagramTexts.UP, labels);
        draw(quads);
        for (final Label label : labels) {
            small(g, label.text, Math.round(label.x), Math.round(label.y), label.colour);
        }
    }

    private void axis(final BufferBuilder quads, final Matrix4f matrix, final ForceDiagramPalette.Colours colours,
                      final float bx, final float by, final Vector3f way, final int colour, final TextKey name,
                      final List<Label> labels) {
        final float length = (float) Math.hypot(way.x, way.y);
        final String text = GameText.resolve(name);
        if (length < 1) {
            // Pointing at the viewer, it is a dot.
            shape(quads, matrix, bx - 2, by, bx + 2, by, 4, 0, 0, colour);
            labels.add(new Label(text, bx + 4, by - 3, true, colour));
            return;
        }
        final float ex = bx + way.x / length * AXIS;
        final float ey = by + way.y / length * AXIS;
        shape(quads, matrix, bx, by, ex, ey, 2 + 2 * EDGE, 5 + 2 * EDGE, EDGE, colours.mark());
        shape(quads, matrix, bx, by, ex, ey, 2, 5, 0, colour);
        labels.add(new Label(text, ex + (ex >= bx ? 3 : -3 - smallWidth(text)), ey - 3, true, colour));
    }

    /* The list of the forces: each one's icon, name, value, bar and switch. */
    private void list(final GuiGraphics g, final ForceDiagramPalette.Colours colours,
                      @Nullable final StructureForcesPayload told, final int mouseX, final int mouseY) {
        final int x = ForceDiagramLayout.LIST_X;
        g.fill(x, ForceDiagramLayout.LIST_Y, x + ForceDiagramLayout.LIST_WIDTH, ForceDiagramLayout.LIST_Y
                + ForceDiagramLayout.LIST_HEIGHT, colours.list());
        final String head = GameText.resolve(ForceDiagramTexts.HEAD);
        Draw.text(g, font, head, x + 6, ForceDiagramLayout.LIST_HEAD_Y, colours.text());
        final int hintX = x + 6 + font.width(head) + 6;
        small(g, trimmed(GameText.resolve(ForceDiagramTexts.HINT), x + ForceDiagramLayout.LIST_WIDTH - 4 - hintX),
                hintX, ForceDiagramLayout.LIST_HEAD_Y + 2, colours.dim());
        final Map<ForceSum.ForceKind, StructureForcesPayload.Force> forces = told == null
                ? Map.of() : merged(told);
        double most = 0;
        for (final StructureForcesPayload.Force force : forces.values()) {
            most = Math.max(most, force.push().length());
        }
        final int hovered = ForceDiagramLayout.rowAt(mouseX, mouseY);
        for (int row = 0; row < ForceDiagramLayout.ROWS; row++) {
            final ForceSum.ForceKind kind = LISTED.get(row);
            final int y = ForceDiagramLayout.rowY(row);
            final boolean shownNow = !hidden.contains(kind);
            final StructureForcesPayload.Force force = forces.get(kind);
            final double strength = force == null ? 0 : force.push().length();
            if (row == hovered) {
                g.fill(x + 2, y, x + ForceDiagramLayout.LIST_WIDTH - 2, y + ForceDiagramLayout.ROW_HEIGHT,
                        colours.row());
            }
            final int colour = shownNow ? colours.of(kind) : colours.off();
            icon(g, iconOf(kind), ForceDiagramLayout.ROW_ICON_X, y + 4, colour);
            Draw.text(g, font, GameText.resolve(nameOf(kind)), ForceDiagramLayout.ROW_NAME_X, y + 3,
                    shownNow ? colours.text() : colours.dim());
            final String value = told == null ? "-" : newtons(strength);
            small(g, value, ForceDiagramLayout.ROW_VALUE_RIGHT - smallWidth(value), y + 4,
                    shownNow ? colours.text() : colours.dim());
            final int barY = y + ForceDiagramLayout.ROW_BAR_Y;
            g.fill(ForceDiagramLayout.ROW_BAR_X, barY, ForceDiagramLayout.ROW_BAR_X + ForceDiagramLayout.ROW_BAR_WIDTH,
                    barY + ForceDiagramLayout.ROW_BAR_HEIGHT, colours.barBack());
            final int filled = most <= 0 ? 0 : (int) Math.round(ForceDiagramLayout.ROW_BAR_WIDTH * strength / most);
            g.fill(ForceDiagramLayout.ROW_BAR_X, barY, ForceDiagramLayout.ROW_BAR_X + filled,
                    barY + ForceDiagramLayout.ROW_BAR_HEIGHT, colour);
            toggle(g, colours, ForceDiagramLayout.SWITCH_X, y + ForceDiagramLayout.SWITCH_Y, shownNow);
        }
    }

    /* The five tiles: what it weighs, the sum and its way, its climb, its speed and its way, the air. */
    private void tiles(final GuiGraphics g, final ForceDiagramPalette.Colours colours,
                       @Nullable final StructureForcesPayload told) {
        final TextKey[] names = {ForceDiagramTexts.MASS_TILE, ForceDiagramTexts.SUM_TILE,
                ForceDiagramTexts.CLIMB_TILE, ForceDiagramTexts.SPEED_TILE, ForceDiagramTexts.AIR_TILE};
        for (int tile = 0; tile < ForceDiagramLayout.TILES; tile++) {
            final int x = ForceDiagramLayout.tileX(tile);
            final int w = ForceDiagramLayout.tileWidth(tile);
            g.fill(x, ForceDiagramLayout.TILE_Y, x + w, ForceDiagramLayout.TILE_Y + ForceDiagramLayout.TILE_HEIGHT,
                    colours.tile());
            Draw.outline(g, x, ForceDiagramLayout.TILE_Y, w, ForceDiagramLayout.TILE_HEIGHT, colours.tileEdge());
            small(g, GameText.resolve(names[tile]), x + 4, ForceDiagramLayout.TILE_Y + 4, colours.dim());
        }
        final int valueY = ForceDiagramLayout.TILE_Y + 15;
        if (told == null) {
            for (int tile = 0; tile < ForceDiagramLayout.TILES; tile++) {
                Draw.text(g, font, "-", ForceDiagramLayout.tileX(tile) + 4, valueY, colours.text());
            }
            return;
        }
        final boolean tonnes = told.mass() >= TONNE;
        final String mass = format.decimal(tonnes ? told.mass() / TONNE : told.mass());
        value(g, colours, 0, GameText.resolve((tonnes ? ForceDiagramTexts.TONNES : ForceDiagramTexts.KILOGRAMS)
                .with(mass)), mass, colours.text());
        final Vec3 sum = told.sum();
        final UnitFormatter.Scaled pushed = format.si(Math.round(sum.length()));
        final String newtons = GameText.resolve(ForceDiagramTexts.NEWTONS.with(pushed.number(), pushed.prefix()));
        final String way = way(sum);
        value(g, colours, 1, way == null ? newtons : GameText.resolve(ForceDiagramTexts.SUM_WAY.with(newtons, way)),
                pushed.number(), colours.sumNumber());
        final double climb = told.velocity().y;
        final String climbed = (climb > 0 ? "+" : "") + format.decimal(climb);
        value(g, colours, 2, GameText.resolve(ForceDiagramTexts.CLIMBING.with(climbed)), climbed,
                climb > STILL ? colours.rising() : climb < -STILL ? colours.falling() : colours.text());
        final double speed = told.velocity().length();
        final String going = format.decimal(speed);
        final String goingWay = speed < STILL ? null : way(told.velocity());
        value(g, colours, 3, GameText.resolve(ForceDiagramTexts.GOING.with(going, goingWay == null
                ? GameText.resolve(ForceDiagramTexts.STILL) : goingWay)), going, colours.text());
        final String air = format.decimal(told.air());
        value(g, colours, 4, GameText.resolve(ForceDiagramTexts.AIR.with(air)), air, colours.text());
    }

    /* A tile's value: its number large in its colour, the words round it small. */
    private void value(final GuiGraphics g, final ForceDiagramPalette.Colours colours, final int tile,
                       final String whole, final String number, final int colour) {
        final int at = whole.indexOf(number);
        final String before = at < 0 ? "" : whole.substring(0, at);
        final String after = at < 0 ? whole : whole.substring(at + number.length());
        final String big = at < 0 ? "" : number;
        int x = ForceDiagramLayout.tileX(tile) + 4;
        final int right = ForceDiagramLayout.tileX(tile) + ForceDiagramLayout.tileWidth(tile) - 3;
        final int y = ForceDiagramLayout.TILE_Y + 15;
        small(g, before, x, y + 2, colours.dim());
        x += smallWidth(before);
        Draw.text(g, font, big, x, y, colour);
        x += font.width(big);
        small(g, trimmed(after, right - x), x, y + 2, colours.dim());
    }

    /* A switch, on or off, its knob to the side it is turned to. */
    private void toggle(final GuiGraphics g, final ForceDiagramPalette.Colours colours, final int x, final int y,
                        final boolean on) {
        final int w = ForceDiagramLayout.SWITCH_WIDTH;
        final int h = ForceDiagramLayout.SWITCH_HEIGHT;
        g.fill(x, y, x + w, y + h, colours.mark());
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, on ? colours.switchOn() : colours.switchOff());
        final int knob = on ? x + w - h : x;
        g.fill(knob + 1, y + 1, knob + h - 1, y + h - 1, colours.knob());
        Draw.outline(g, knob + 1, y + 1, h - 2, h - 2, colours.mark());
    }

    /*
     * A label on its dark ground beside a point: to its right, or ending at its left; moved down past the labels
     * already {@code placed}, a line at a time, so two never cover each other.
     */
    private void label(final GuiGraphics g, final ForceDiagramPalette.Colours colours, final String text,
                       final float x, final float y, final boolean right, final int colour,
                       final List<int[]> placed) {
        final int w = smallWidth(text);
        int lx = Math.round(right ? x + 4 : x - 4 - w);
        int ly = Math.round(y - 3);
        lx = Math.max(ForceDiagramLayout.VIEW_X + 2, Math.min(ForceDiagramLayout.VIEW_X
                + ForceDiagramLayout.VIEW_WIDTH - w - 2, lx));
        final int lowest = ForceDiagramLayout.VIEW_Y + ForceDiagramLayout.VIEW_HEIGHT - 9;
        ly = Math.max(ForceDiagramLayout.VIEW_Y + 22, Math.min(lowest, ly));
        for (int tries = 0; tries <= placed.size() && covers(placed, lx - 2, ly - 1, w + 4, LABEL_LINE);
             tries++) {
            ly = ly + LABEL_LINE > lowest ? ly - 2 * LABEL_LINE : ly + LABEL_LINE;
        }
        placed.add(new int[] {lx - 2, ly - 1, w + 4, LABEL_LINE});
        g.fill(lx - 2, ly - 1, lx + w + 2, ly + 7, colours.label());
        small(g, text, lx, ly, colour);
    }

    /* Whether the box {@code (x, y, w, h)} covers any of the {@code placed}. */
    private static boolean covers(final List<int[]> placed, final int x, final int y, final int w, final int h) {
        for (final int[] box : placed) {
            if (x < box[0] + box[2] && box[0] < x + w && y < box[1] + box[3] && box[1] < y + h) {
                return true;
            }
        }
        return false;
    }

    private void icon(final GuiGraphics g, final String[] pixels, final int x, final int y, final int colour) {
        for (int row = 0; row < pixels.length; row++) {
            for (int column = 0; column < pixels[row].length(); column++) {
                if (pixels[row].charAt(column) == '#') {
                    g.fill(x + column, y + row, x + column + 1, y + row + 1, colour);
                }
            }
        }
    }

    /* The words for where {@code push} goes, up or down and ahead, back, left or right; null for nowhere. */
    @Nullable
    private String way(final Vec3 push) {
        final double strength = push.length();
        if (strength <= 0) {
            return null;
        }
        final Vec3 ahead = ahead();
        final Vec3 right = ahead.cross(new Vec3(0, 1, 0));
        final double along = push.dot(ahead);
        final double across = push.dot(right);
        final TextKey upright = Math.abs(push.y) > QUIET * strength
                ? push.y > 0 ? ForceDiagramTexts.WAY_UP : ForceDiagramTexts.WAY_DOWN : null;
        TextKey level = null;
        if (Math.max(Math.abs(along), Math.abs(across)) > QUIET * strength) {
            level = Math.abs(along) >= Math.abs(across)
                    ? along > 0 ? ForceDiagramTexts.WAY_AHEAD : ForceDiagramTexts.WAY_BACK
                    : across > 0 ? ForceDiagramTexts.WAY_RIGHT : ForceDiagramTexts.WAY_LEFT;
        }
        if (upright != null && level != null) {
            return GameText.resolve(ForceDiagramTexts.WAYS.with(GameText.resolve(upright), GameText.resolve(level)));
        }
        if (upright != null) {
            return GameText.resolve(upright);
        }
        return level == null ? null : GameText.resolve(level);
    }

    /* The way the structure's bow looks, level: its yard's north, turned as it stands. */
    private Vec3 ahead() {
        final StructurePose at = structure.frame().pose();
        final Vector3f bow = new Quaternionf((float) at.qx(), (float) at.qy(), (float) at.qz(), (float) at.qw())
                .transform(new Vector3f(0, 0, -1));
        final double level = Math.hypot(bow.x, bow.z);
        return level < 1.0e-3 ? new Vec3(0, 0, -1) : new Vec3(bow.x / level, 0, bow.z / level);
    }

    private String newtons(final double strength) {
        final UnitFormatter.Scaled scaled = format.si(Math.round(strength));
        return GameText.resolve(ForceDiagramTexts.NEWTONS.with(scaled.number(), scaled.prefix()));
    }

    private String trimmed(final String text, final int room) {
        if (room <= 0) {
            return "";
        }
        return font.plainSubstrByWidth(text, (int) (room / SMALL));
    }

    private int smallWidth(final String text) {
        return Math.round(font.width(text) * SMALL);
    }

    private void small(final GuiGraphics g, final String text, final int x, final int y, final int colour) {
        Draw.textScaled(g, font, text, x, y, colour, SMALL);
    }

    private double localX(final double mouseX) {
        return (mouseX - left) / fit;
    }

    private double localY(final double mouseY) {
        return (mouseY - top) / fit;
    }

    /* The forces by kind, the balloons' given with buoyancy, where it acts weighed between them. */
    private static Map<ForceSum.ForceKind, StructureForcesPayload.Force> merged(final StructureForcesPayload told) {
        final Map<ForceSum.ForceKind, StructureForcesPayload.Force> out = new EnumMap<>(ForceSum.ForceKind.class);
        for (final StructureForcesPayload.Force force : told.forces()) {
            final ForceSum.ForceKind kind = force.kind() == ForceSum.ForceKind.BALLOONS
                    ? ForceSum.ForceKind.BUOYANCY : force.kind();
            final StructureForcesPayload.Force known = out.get(kind);
            if (known == null) {
                out.put(kind, new StructureForcesPayload.Force(kind, force.push(), force.at()));
                continue;
            }
            final double a = known.push().length();
            final double b = force.push().length();
            final Vec3 at = a + b <= 0 ? known.at() : known.at().scale(a / (a + b)).add(force.at().scale(b / (a + b)));
            out.put(kind, new StructureForcesPayload.Force(kind, known.push().add(force.push()), at));
        }
        return out;
    }

    private static TextKey nameOf(final ForceSum.ForceKind kind) {
        return switch (kind) {
            case GRAVITY -> ForceDiagramTexts.GRAVITY;
            case BUOYANCY, BALLOONS -> ForceDiagramTexts.BUOYANCY;
            case THRUST -> ForceDiagramTexts.THRUST;
            case DRAG -> ForceDiagramTexts.DRAG;
            case LIFT -> ForceDiagramTexts.LIFT;
            case LEVITATION -> ForceDiagramTexts.LEVITATION;
            case MAGNETISM -> ForceDiagramTexts.MAGNETISM;
            case WHEELS -> ForceDiagramTexts.WHEELS;
            case HELD -> ForceDiagramTexts.HELD;
        };
    }

    private static String[] iconOf(final ForceSum.ForceKind kind) {
        return switch (kind) {
            case GRAVITY, WHEELS, HELD -> GRAVITY_ICON;
            case BUOYANCY, BALLOONS -> BUOYANCY_ICON;
            case THRUST -> THRUST_ICON;
            case DRAG -> DRAG_ICON;
            case LIFT -> LIFT_ICON;
            case LEVITATION -> LEVITATION_ICON;
            case MAGNETISM -> MAGNETISM_ICON;
        };
    }

    /*
     * An arrow from (x1, y1) to (x2, y2), as two quads, its shaft {@code width} wide and its head {@code head} long,
     * the head pushed {@code beyond} past the point so an edge drawn under it shows round its tip too.
     */
    private static void shape(final BufferBuilder quads, final Matrix4f matrix, final float x1, final float y1,
                              final float x2, final float y2, final float width, final float head, final float beyond,
                              final int colour) {
        final float dx = x2 - x1;
        final float dy = y2 - y1;
        final float length = (float) Math.hypot(dx, dy);
        if (length <= 0) {
            return;
        }
        final float ux = dx / length;
        final float uy = dy / length;
        final float nx = -uy;
        final float ny = ux;
        final float tipX = x2 + ux * beyond;
        final float tipY = y2 + uy * beyond;
        final float headLength = Math.min(head, length * 0.6F);
        final float neckX = tipX - ux * headLength;
        final float neckY = tipY - uy * headLength;
        final float half = width / 2;
        final float startX = x1 - ux * beyond;
        final float startY = y1 - uy * beyond;
        quads.addVertex(matrix, startX + nx * half, startY + ny * half, 0).setColor(colour);
        quads.addVertex(matrix, neckX + nx * half, neckY + ny * half, 0).setColor(colour);
        quads.addVertex(matrix, neckX - nx * half, neckY - ny * half, 0).setColor(colour);
        quads.addVertex(matrix, startX - nx * half, startY - ny * half, 0).setColor(colour);
        if (head <= 0) {
            return;
        }
        final float wing = headLength * 0.6F + beyond;
        quads.addVertex(matrix, tipX, tipY, 0).setColor(colour);
        quads.addVertex(matrix, neckX + nx * wing, neckY + ny * wing, 0).setColor(colour);
        quads.addVertex(matrix, neckX, neckY, 0).setColor(colour);
        quads.addVertex(matrix, neckX - nx * wing, neckY - ny * wing, 0).setColor(colour);
    }

    /* Draws what was put in {@code quads}, if anything was. */
    private static void draw(final BufferBuilder quads) {
        final MeshData built = quads.build();
        if (built == null) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(built);
        RenderSystem.disableBlend();
    }

    /* How the structure is seen in the view, to put a point of it on the screen. */
    private record Shown(SceneRenderer.View seen, int wide, int tall, double pixel) {

        /** Where {@code offset}, in blocks from the centre of mass, is on the diagram, written into {@code out}. */
        Vector3f at(final Vec3 offset, final Vector3f out) {
            SceneRenderer.project(seen, wide, tall, (float) offset.x, (float) offset.y, (float) offset.z, out);
            return out.set((float) (ForceDiagramLayout.VIEW_X + out.x / pixel),
                    (float) (ForceDiagramLayout.VIEW_Y + out.y / pixel), out.z);
        }
    }

    /* A label waiting to be written over the arrows, once they are drawn. */
    private record Label(String text, float x, float y, boolean right, int colour) {
    }

    /* The three views: a turn round from the bow and a tilt, and the place of its button. */
    private enum Angle {
        THREE_D(ForceDiagramTexts.VIEW_3D, 0, -125, 25),
        SIDE(ForceDiagramTexts.VIEW_SIDE, 1, -90, 0),
        TOP(ForceDiagramTexts.VIEW_TOP, 2, 0, 90);

        private final TextKey text;
        private final int place;
        private final float turn;
        private final float tilt;

        Angle(final TextKey text, final int place, final float turn, final float tilt) {
            this.text = text;
            this.place = place;
            this.turn = turn;
            this.tilt = tilt;
        }
    }
}
