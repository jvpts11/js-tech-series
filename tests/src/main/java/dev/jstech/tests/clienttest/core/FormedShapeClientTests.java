/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.core;

import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.shape.CellSpace;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.IFormedMeshes;
import dev.jstech.core.multiblock.shape.MeshPick;
import dev.jstech.core.multiblock.shape.MeshVoxels;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.TestModels;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.Optional;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The blocks of a formed structure on a player's game, shaped as their part of its model: the crosshair on the test
 * turbine's top meets the model's own faces, not the sixteenths its shape is made of, and the outline round the block
 * aimed at is drawn from the model's faces, not from the boxes of its shape.
 */
public final class FormedShapeClientTests {

    /* The turbine facing north, its terminal here, its body north of it; the player on a barrier south of it. */
    private static final BlockPos TERMINAL = new BlockPos(0, 2, 0);
    private static final BlockPos STAND = new BlockPos(0, 5, 1);
    private static final int WAIT = 300;
    private static final int PUFFS_CLEAR = 40;
    private static final int SIXTEENTHS = MeshVoxels.FINE;
    /* How much lower than its shape's top the model's face must lie where the crosshair is put, in blocks. */
    private static final double BELOW = 0.02;
    /* How close the crosshair's point must be to a face of the model, and how far from the shape's, in blocks. */
    private static final double ON_FACE = 1.0e-3;
    private static final double OFF_SHAPE = 5.0e-3;
    /*
     * The window of the screen read round the crosshair, in the GUI's pixels, its middle left out; how much darker or
     * different a pixel is, at least, and how many such pixels show the outline.
     */
    private static final int REACH = 40;
    private static final int CROSSHAIR = 6;
    private static final double DARKER = 0.85;
    private static final int DIFFERENT = 40;
    private static final int OUTLINED = 12;

    private FormedShapeClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void turbine_isAimedAtAndOutlinedByItsModelsFaces(final ClientTestContext ctx) {
        final Target[] target = new Target[1];
        final int[][][] seen = new int[3][][];
        final IFormedMeshes[] kept = new IFormedMeshes[1];
        ctx.afterTest(() -> {
            ctx.mc().gameRenderer.setRenderBlockOutline(true);
            if (kept[0] != null) {
                FormedShapes.meshes(kept[0]);
            }
        });
        ctx.thenServer(0, level -> build(ctx, level))
                .thenTeleport(0, STAND, Direction.NORTH)
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof TestModels.TurbineEntity turbine && turbine.structure().merged(), WAIT,
                        "the turbine to form and merge", level -> level.getBlockEntity(ctx.abs(TERMINAL))
                        instanceof TestModels.TurbineEntity turbine ? String.valueOf(turbine.structure().structure())
                        : "no turbine")
                .thenWaitUntil(() -> top(ctx).isPresent(), WAIT, "a place on its top lower in the model than its shape")
                .then(PUFFS_CLEAR, () -> {
                    target[0] = top(ctx).orElseThrow();
                    ctx.lookAt(target[0].point());
                })
                .then(3, () -> {
                    final HitResult found = ctx.mc().hitResult;
                    ctx.assertTrue(found instanceof BlockHitResult block && block.getType() == HitResult.Type.BLOCK
                            && block.getBlockPos().equals(target[0].pos()), "the crosshair is on the turbine's block "
                            + target[0].pos() + ": " + found);
                    final Vec3 at = found.getLocation();
                    ctx.assertTrue(at.distanceTo(target[0].point()) < ON_FACE, "on the model's face where it was put,"
                            + " " + target[0].point() + ": " + at);
                    final Vec3 shape = target[0].shapeHit();
                    ctx.assertTrue(shape == null || at.distanceTo(shape) > OFF_SHAPE, "not on the face of the"
                            + " block's shape, " + shape + ", which lies over the model's there");
                })
                .then(3, () -> seen[0] = window(ctx))
                .then(0, () -> ctx.mc().gameRenderer.setRenderBlockOutline(false))
                .then(3, () -> seen[1] = window(ctx))
                .then(0, () -> {
                    ctx.mc().gameRenderer.setRenderBlockOutline(true);
                    kept[0] = FormedShapes.meshes();
                    // The model's faces set aside: the block is outlined by its shape's boxes, as any other block.
                    FormedShapes.meshes(new IFormedMeshes() {
                        @Override
                        public @Nullable float[] triangles(final FormedPlace place, final FormedShape shape) {
                            return null;
                        }

                        @Override
                        public @Nullable float[] lines(final FormedPlace place, final FormedShape shape) {
                            return null;
                        }
                    });
                })
                .then(3, () -> seen[2] = window(ctx))
                .then(0, () -> {
                    FormedShapes.meshes(kept[0]);
                    ctx.assertTrue(darkened(seen[0], seen[1]) >= OUTLINED, "the block's outline is drawn: "
                            + darkened(seen[0], seen[1]) + " pixels darker with it");
                    ctx.assertTrue(differing(seen[0], seen[2]) >= OUTLINED, "from the model's faces, not the boxes"
                            + " of its shape: " + differing(seen[0], seen[2]) + " pixels apart");
                })
                .thenScreenshot(3, "formed-shape-outline")
                .thenServer(0, level -> clear(ctx, level));
    }

    /* The turbine facing north with its terminal at {@link #TERMINAL}, and a barrier for the player to stand on. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        for (int y = 0; y < 2; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -2; z <= 0; z++) {
                    final BlockPos at = TERMINAL.offset(x, y, z);
                    if (!at.equals(TERMINAL)) {
                        level.setBlockAndUpdate(ctx.abs(at), Blocks.IRON_BLOCK.defaultBlockState());
                    }
                }
            }
        }
        level.setBlockAndUpdate(ctx.abs(TERMINAL), TestModels.TURBINE_BLOCK.get().defaultBlockState());
        level.setBlockAndUpdate(ctx.abs(STAND.below()), Blocks.BARRIER.defaultBlockState());
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(TERMINAL.offset(-1, 0, -2)),
                ctx.abs(TERMINAL.offset(1, 1, 0)))) {
            level.removeBlock(at, false);
        }
        level.removeBlock(ctx.abs(STAND.below()), false);
    }

    /*
     * A place on the top of one of the turbine's upper blocks, on the player's game, where the model's face lies
     * lower than the top of the block's shape: where the crosshair meets the model, and where it would have met the
     * shape.
     */
    private static Optional<Target> top(final ClientTestContext ctx) {
        final ClientLevel level = ctx.mc().level;
        if (level == null) {
            return Optional.empty();
        }
        final Vec3 eye = ctx.mc().player.getEyePosition();
        for (int x = -1; x <= 1; x++) {
            for (int z = -2; z <= 0; z++) {
                final BlockPos pos = ctx.abs(TERMINAL.offset(x, 1, z));
                if (!(level.getBlockEntity(pos) instanceof FormedPartBlockEntity part) || part.place() == null) {
                    continue;
                }
                final FormedPlace place = part.place();
                final Optional<float[]> faces = FormedShapes.triangles(level, place);
                final Optional<int[]> file = FormedShapes.get(level, place.model()).flatMap(shape -> shape.boxes(
                        place.cell()));
                if (faces.isEmpty() || file.isEmpty()) {
                    continue;
                }
                final int[] boxes = CellSpace.boxes(file.get(), place.turn());
                for (int u = 2; u < SIXTEENTHS - 2; u++) {
                    for (int v = 2; v < SIXTEENTHS - 2; v++) {
                        final double wx = (u + 0.5) / SIXTEENTHS;
                        final double wz = (v + 0.5) / SIXTEENTHS;
                        final double face = faceUnder(faces.get(), place, wx, wz);
                        final double shapeTop = (double) shapeTop(boxes, u, v) / SIXTEENTHS;
                        if (face < 0 || shapeTop - face < BELOW) {
                            continue;
                        }
                        final Vec3 point = new Vec3(pos.getX() + wx, pos.getY() + face, pos.getZ() + wz);
                        final Vec3 far = eye.add(point.subtract(eye).scale(2));
                        final BlockHitResult shapeHit = level.getBlockState(pos).getShape(level, pos).clip(eye, far,
                                pos);
                        if (shapeHit != null && reachesFirst(ctx, eye, point, pos)) {
                            return Optional.of(new Target(pos, point, shapeHit.getLocation()));
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    /* How high, from 0 to 1, the highest face of the model over the block's place ({@code wx}, {@code wz}) is. */
    private static double faceUnder(final float[] faces, final FormedPlace place, final double wx, final double wz) {
        final double[] from = new double[3];
        final double[] to = new double[3];
        CellSpace.toModel(new double[] {wx, 1, wz}, place.turn(), from);
        CellSpace.toModel(new double[] {wx, 0, wz}, place.turn(), to);
        final double along = MeshPick.along(faces, from, to, new double[3]);
        return along < 0 ? -1 : 1 - along;
    }

    private static int shapeTop(final int[] boxes, final int u, final int v) {
        int top = 0;
        for (int b = 0; b + MeshVoxels.BOX <= boxes.length; b += MeshVoxels.BOX) {
            if (boxes[b] <= u && boxes[b + 3] > u && boxes[b + 2] <= v && boxes[b + 5] > v) {
                top = Math.max(top, boxes[b + 4]);
            }
        }
        return top;
    }

    /* Whether nothing stands between the eye and {@code point} but the block it is on, the model's faces counted. */
    private static boolean reachesFirst(final ClientTestContext ctx, final Vec3 eye, final Vec3 point,
                                        final BlockPos pos) {
        final Vec3 justShort = eye.add(point.subtract(eye).scale(1 - 1.0e-4));
        final BlockHitResult before = ctx.mc().level.clip(new ClipContext(eye, justShort, ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, ctx.mc().player));
        return before.getType() == HitResult.Type.MISS && eye.distanceTo(point) < ctx.mc().player
                .blockInteractionRange() - 0.5;
    }

    /* The brightness of the pixels round the crosshair, its middle left out. */
    private static int[][] window(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final int[][] out = new int[2 * REACH + 1][2 * REACH + 1];
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dy = -REACH; dy <= REACH; dy++) {
                    if (Math.abs(dx) <= CROSSHAIR && Math.abs(dy) <= CROSSHAIR) {
                        out[dx + REACH][dy + REACH] = -1;
                        continue;
                    }
                    final int argb = frame.pixel(x + dx, y + dy);
                    out[dx + REACH][dy + REACH] = (argb >> 16 & 0xFF) + (argb >> 8 & 0xFF) + (argb & 0xFF);
                }
            }
        }
        return out;
    }

    /* How many pixels are clearly darker in {@code with} than in {@code without}. */
    private static int darkened(final int[][] with, final int[][] without) {
        int count = 0;
        for (int i = 0; i < with.length; i++) {
            for (int j = 0; j < with[i].length; j++) {
                if (with[i][j] >= 0 && with[i][j] < without[i][j] * DARKER) {
                    count++;
                }
            }
        }
        return count;
    }

    /* How many pixels clearly differ between {@code a} and {@code b}. */
    private static int differing(final int[][] a, final int[][] b) {
        int count = 0;
        for (int i = 0; i < a.length; i++) {
            for (int j = 0; j < a[i].length; j++) {
                if (a[i][j] >= 0 && Math.abs(a[i][j] - b[i][j]) > DIFFERENT) {
                    count++;
                }
            }
        }
        return count;
    }

    /* A place to aim at: the block, the point on the model's face, and where its shape would be met first. */
    private record Target(BlockPos pos, Vec3 point, @Nullable Vec3 shapeHit) {
    }
}
