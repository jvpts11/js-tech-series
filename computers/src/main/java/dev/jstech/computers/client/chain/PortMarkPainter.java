/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.PortMark;
import dev.jstech.core.gui.ColorContrast;
import dev.jstech.core.jstechseries.cylinder.GasCylinders;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.Materials;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.shape.FormedAim;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Draws a chain machine's pipe markings ({@link PortMark}) on the faces of its ports bound to one product, as plants
 * mark their pipes: a band along the foot of the face, in the colour the product's cylinder shoulder wears (its
 * material's own colour where the code names none), its formula on it and an arrow the way it flows.
 */
@PaletteHolder
public final class PortMarkPainter {

    /* The band's frame, the formula's dark and light inks, and the band of a product with no colour. */
    static final Palette<Colours> PALETTE = Palettes.declare(JsComputers.MODID, "chain/port_marks",
            new Colours(0xFF141414, 0xFF101010, 0xFFFFFFFF, 0xFF8A8A8A));
    /* The band in a face's sixteenths: along its foot, from a quarter in at each side. */
    private static final float LEFT = 0.25F;
    private static final float RIGHT = 15.75F;
    private static final float TOP = 11.0F;
    private static final float BOTTOM = 15.75F;
    private static final float FRAME = 0.25F;
    /* The formula's size and place, and the arrow's. */
    private static final float WORDS = 0.42F;
    private static final float WORDS_X = 1.2F;
    private static final float WORDS_Y = 11.95F;
    private static final float ARROW_TIP = 14.8F;
    private static final float ARROW_DEPTH = 2.0F;
    private static final float ARROW_MID = 13.375F;
    private static final float ARROW_HALF = 1.5F;
    private static final int ARROW_STEPS = 4;
    /*
     * How far in front of the face the band lies, in blocks, so it never flickers into the face; and how much further
     * out, in sixteenths, the arrow and the formula lie over the band.
     */
    private static final float LIFT = 0.004F;
    private static final float OVER = 0.04F;
    private static final float SIXTEENTH = 1.0F / 16.0F;
    /*
     * A port's wall is looked for from this far out of its face, in blocks, and found within this of the face: a
     * port stands half a texel in, and nothing stands in front of it.
     */
    private static final double OUTSIDE = 0.25;
    private static final double WALL_DEPTH = 1.0 / 16.0;
    /* The markings drawn of each machine, kept while the machine is. */
    private static final Map<ChainMachineBlockEntity, Shown> SHOWN = new WeakHashMap<>();

    private PortMarkPainter() {
    }

    /**
     * {@code drawn}, a chain machine's renderer, drawing the machine's markings too, from its terminal's own block:
     * the model is drawn turned as the machine stands, the markings by the world's own faces.
     */
    public static BlockEntityRenderer<ChainMachineBlockEntity> marked(
            final BlockEntityRenderer<ChainMachineBlockEntity> drawn) {
        return new Marked(drawn);
    }

    /** Draws {@code machine}'s markings, the pose standing at its terminal's block. */
    public static void draw(final ChainMachineBlockEntity machine, final PoseStack poses,
                            final MultiBufferSource buffers) {
        final Level level = machine.getLevel();
        if (level == null || machine.marks().isEmpty()) {
            return;
        }
        final BlockPos origin = machine.getBlockPos();
        final Colours colours = PALETTE.get();
        final Font font = Minecraft.getInstance().font;
        for (final PortMark mark : shown(machine, level)) {
            final BlockPos at = BlockPos.of(mark.pos());
            final Optional<Material> product = Materials.byId(mark.product());
            final int band = product.map(PortMarkPainter::bandOf).orElse(colours.unknown());
            final int ink = ColorContrast.ratio(band, colours.dark()) >= ColorContrast.ratio(band, colours.light())
                    ? colours.dark() : colours.light();
            final int light = LevelRenderer.getLightColor(level, at.relative(mark.face()));
            poses.pushPose();
            poses.translate(at.getX() - origin.getX() + 0.5, at.getY() - origin.getY() + 0.5,
                    at.getZ() - origin.getZ() + 0.5);
            poses.mulPose(Axis.YP.rotationDegrees(-mark.face().toYRot()));
            poses.translate(-0.5, 0.5, 0.5 + LIFT);
            // The face's sixteenths now run right and down as one stands before it, and out of it is +z.
            poses.scale(SIXTEENTH, -SIXTEENTH, SIXTEENTH);
            final Matrix4f face = poses.last().pose();
            final VertexConsumer fill = buffers.getBuffer(RenderType.textBackground());
            // The frame round the band, its four sides apart, so nothing lies on anything in one plane.
            rect(fill, face, LEFT, TOP, RIGHT, TOP + FRAME, 0, colours.frame(), light);
            rect(fill, face, LEFT, BOTTOM - FRAME, RIGHT, BOTTOM, 0, colours.frame(), light);
            rect(fill, face, LEFT, TOP + FRAME, LEFT + FRAME, BOTTOM - FRAME, 0, colours.frame(), light);
            rect(fill, face, RIGHT - FRAME, TOP + FRAME, RIGHT, BOTTOM - FRAME, 0, colours.frame(), light);
            rect(fill, face, LEFT + FRAME, TOP + FRAME, RIGHT - FRAME, BOTTOM - FRAME, 0, band, light);
            arrow(fill, face, mark.in(), ink, light);
            final String formula = product.flatMap(PortMarkPainter::formulaOf).orElse(mark.product().getPath());
            final Matrix4f words = new Matrix4f(face).translate(WORDS_X, WORDS_Y, OVER).scale(WORDS, WORDS, 1);
            font.drawInBatch(formula, 0, 0, ink, false, words, buffers, Font.DisplayMode.POLYGON_OFFSET, 0, light);
            poses.popPose();
        }
    }

    /**
     * Of {@code machine}'s markings, those on the faces its formed model shows its ports on, worked out again only
     * when the markings change: a port on a corner had one on a face the model sets back, which hung in the air.
     */
    public static List<PortMark> shown(final ChainMachineBlockEntity machine, final Level level) {
        final List<PortMark> sent = machine.marks();
        final Shown was = SHOWN.get(machine);
        if (was != null && was.sent() == sent) {
            return was.drawn();
        }
        final Optional<Orientation> standing = machine.structure() == null ? Optional.empty()
                : machine.structure().orientation();
        if (standing.isEmpty()) {
            return sent;
        }
        final int orientation = FormedShapes.ORIENTATIONS.indexOf(standing.get());
        final Map<Long, List<PortMark>> byPort = new LinkedHashMap<>();
        sent.forEach(mark -> byPort.computeIfAbsent(mark.pos(), at -> new ArrayList<>()).add(mark));
        final List<PortMark> drawn = new ArrayList<>();
        boolean known = true;
        for (final List<PortMark> port : byPort.values()) {
            final List<PortMark> flush = new ArrayList<>();
            for (final PortMark mark : port) {
                final Optional<FormedPlace> place = FormedShapes.place(machine.structure().shape(), orientation,
                        machine.getBlockPos(), BlockPos.of(mark.pos()));
                if (place.isEmpty() || FormedShapes.triangles(level, place.get()).isEmpty()) {
                    known = false;
                } else if (flush(level, place.get(), mark)) {
                    flush.add(mark);
                }
            }
            // A port whose model shows no face of it flush keeps them all, rather than lose its marking.
            drawn.addAll(flush.isEmpty() ? port : flush);
        }
        // Until its model's faces are known here, it is worked out again at the next frame.
        if (known) {
            SHOWN.put(machine, new Shown(sent, List.copyOf(drawn)));
        }
        return drawn;
    }

    /*
     * Whether the model has a wall at {@code mark}'s face, as its ports stand: at the block's face, a texel in at most,
     * nothing in front, at the middle of the face.
     */
    private static boolean flush(final Level level, final FormedPlace place, final PortMark mark) {
        final BlockPos at = BlockPos.of(mark.pos());
        final Vec3 normal = Vec3.atLowerCornerOf(mark.face().getNormal());
        final Vec3 middle = Vec3.atCenterOf(at).add(normal.scale(0.5));
        final BlockHitResult hit = FormedAim.hit(level, place, middle.add(normal.scale(OUTSIDE)),
                middle.subtract(normal.scale(WALL_DEPTH)), at);
        return hit != null && hit.getLocation().distanceTo(middle) <= WALL_DEPTH;
    }

    /* The band's colour: the product's cylinder shoulder by the code, else its material's own, opaque. */
    private static int bandOf(final Material product) {
        return GasCylinders.shoulder(product).orElseGet(() -> FastColor.ARGB32.opaque(product.colour()));
    }

    private static Optional<String> formulaOf(final Material product) {
        return product.value(MaterialProperties.FORMULA).or(() -> product.value(MaterialProperties.SYMBOL));
    }

    /* The arrow at the band's right end, pointing right into the machine for an inlet and left for an outlet. */
    private static void arrow(final VertexConsumer fill, final Matrix4f face, final boolean in, final int ink,
                              final int light) {
        final float step = ARROW_DEPTH / ARROW_STEPS;
        for (int k = 0; k < ARROW_STEPS; k++) {
            final float half = ARROW_HALF * (ARROW_STEPS - k) / ARROW_STEPS;
            final float near = in ? ARROW_TIP - ARROW_DEPTH + k * step : ARROW_TIP - k * step - step;
            rect(fill, face, near, ARROW_MID - half, near + step, ARROW_MID + half, OVER, ink, light);
        }
    }

    /* A rectangle of the face, {@code z} sixteenths out of it. */
    private static void rect(final VertexConsumer fill, final Matrix4f face, final float x0, final float y0,
                             final float x1, final float y1, final float z, final int argb, final int light) {
        fill.addVertex(face, x0, y0, z).setColor(argb).setLight(light);
        fill.addVertex(face, x0, y1, z).setColor(argb).setLight(light);
        fill.addVertex(face, x1, y1, z).setColor(argb).setLight(light);
        fill.addVertex(face, x1, y0, z).setColor(argb).setLight(light);
    }

    /**
     * The colours.
     *
     * @param frame   the band's frame
     * @param dark    the formula and arrow on a light band
     * @param light   the formula and arrow on a dark band
     * @param unknown the band of a product with no colour of its own
     */
    record Colours(int frame, int dark, int light, int unknown) {
    }

    /* A machine's markings as the server sent them, and those drawn of them. */
    private record Shown(List<PortMark> sent, List<PortMark> drawn) {
    }

    /* A machine's renderer and its markings: everything else it answers as the renderer it wraps does. */
    private record Marked(BlockEntityRenderer<ChainMachineBlockEntity> drawn)
            implements BlockEntityRenderer<ChainMachineBlockEntity> {

        @Override
        public void render(final ChainMachineBlockEntity machine, final float partialTick, final PoseStack poses,
                           final MultiBufferSource buffers, final int light, final int overlay) {
            drawn.render(machine, partialTick, poses, buffers, light, overlay);
            draw(machine, poses, buffers);
        }

        @Override
        public boolean shouldRenderOffScreen(final ChainMachineBlockEntity machine) {
            return drawn.shouldRenderOffScreen(machine);
        }

        @Override
        public int getViewDistance() {
            return drawn.getViewDistance();
        }

        @Override
        public boolean shouldRender(final ChainMachineBlockEntity machine, final Vec3 camera) {
            return drawn.shouldRender(machine, camera);
        }

        @Override
        public AABB getRenderBoundingBox(final ChainMachineBlockEntity machine) {
            return drawn.getRenderBoundingBox(machine);
        }
    }
}
