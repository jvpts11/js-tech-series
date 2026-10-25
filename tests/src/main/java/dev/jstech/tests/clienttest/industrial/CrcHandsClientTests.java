/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.industrial;

import dev.jstech.core.client.multiblock.HandsAim;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.hands.HandGeometry;
import dev.jstech.core.multiblock.hands.IHandPart;
import dev.jstech.core.multiblock.hands.StructureHands;
import dev.jstech.industrial.crc.CrcBlockEntity;
import dev.jstech.industrial.crc.CrcContent;
import dev.jstech.industrial.crc.CrcStructure;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The Computational Research Centre worked by hand on a player's game: the first frame's glazed door outlined when the
 * crosshair is on it, opened by a click and drawn swinging out; then the first place of its gate outlined through the
 * doorway, and a heat sink in hand set in it by a click, as the server has it.
 */
public final class CrcHandsClientTests {

    /* The terminal in the middle layer, the body running south of it; the player west of its first frame. */
    private static final BlockPos TERMINAL = new BlockPos(0, 3, 2);
    private static final BlockPos STAND = new BlockPos(-4, 2, 9);
    /* Ticks for the puffs of its blocks merging to clear. */
    private static final int PUFFS_CLEAR = 40;
    private static final int WAIT = 200;
    /*
     * How far under the door's top edge the crosshair rests, in blocks; the columns read either side of it, clear of
     * the crosshair, and how far up and down, in the GUI's pixels; how much darker a pixel the outline crosses is, at
     * least, and how many such pixels show the outline drawn.
     */
    private static final double UNDER_EDGE = 0.03;
    private static final int[] COLUMNS = {-14, -13, -12, -11, -10, 10, 11, 12, 13, 14};
    private static final int ROWS = 8;
    private static final double DARKER = 0.85;
    private static final int OUTLINED = 6;

    private CrcHandsClientTests() {
    }

    @ClientTest(timeoutTicks = 1200)
    public static void door_outlinedOpenedByAClickAndItsPlaceTakesAPart(final ClientTestContext ctx) {
        final int[][][] seen = new int[2][][];
        ctx.afterTest(() -> ctx.mc().options.hideGui = false);
        ctx.thenTeleport(0, STAND, Direction.EAST)
                .thenServer(0, level -> build(ctx, level))
                .thenWaitUntilServer(level -> crc(ctx, level).formed(), WAIT, "the CRC to form",
                        level -> String.valueOf(crc(ctx, level).structure().failure()))
                .thenWaitUntil(() -> hands(ctx).isPresent(), WAIT, "the formed CRC on the player's game")
                .then(PUFFS_CLEAR, () -> ctx.lookAt(middleOf(ctx, partOf("door_0"))))
                .thenWaitUntil(() -> aimedAt("door_0"), WAIT, "the crosshair on the first door")
                .then(0, () -> ctx.lookAt(underNearTopEdge(ctx)))
                .thenWaitUntil(() -> aimedAt("door_0"), WAIT, "the crosshair just under the door's top edge")
                .then(3, () -> seen[0] = window(ctx))
                .then(0, () -> ctx.mc().options.hideGui = true)
                .then(3, () -> seen[1] = window(ctx))
                .then(0, () -> ctx.mc().options.hideGui = false)
                .then(0, () -> ctx.assertTrue(darkened(seen[0], seen[1]) >= OUTLINED, "the door's outline is drawn"
                        + " along its top edge, and hidden with the rest of the game's marks: "
                        + darkened(seen[0], seen[1]) + " pixels darker with it"))
                .thenScreenshot(3, "crc-hands-door-outlined")
                .then(0, () -> KeyMapping.click(ctx.mc().options.keyUse.getKey()))
                .thenWaitUntilServer(level -> crc(ctx, level).hands().orElseThrow().open(0), WAIT,
                        "the server to open the door", level -> "aimed " + HandsAim.aimed())
                .thenWaitUntil(() -> hands(ctx).map(hands -> hands.openness(ctx.mc().level.getGameTime())[0] >= 1)
                        .orElse(false), WAIT, "the door drawn wholly open")
                .then(0, () -> ctx.lookAt(middleOf(ctx, partOf("place_0"))))
                .thenWaitUntil(() -> aimedAt("place_0"), WAIT, "the crosshair on the first place, through the doorway")
                .thenScreenshot(3, "crc-hands-door-open")
                .thenServer(0, level -> ctx.hold(new ItemStack(CrcContent.HEAT_SINK.get())))
                .thenWaitUntil(() -> ctx.mc().player.getMainHandItem().is(CrcContent.HEAT_SINK.get()), WAIT,
                        "the heat sink in hand")
                .then(0, () -> KeyMapping.click(ctx.mc().options.keyUse.getKey()))
                .thenWaitUntilServer(level -> crc(ctx, level).getInventory().getStackInSlot(0)
                        .is(CrcContent.HEAT_SINK.get()), WAIT, "the heat sink set in the first place",
                        level -> "the place holds " + crc(ctx, level).getInventory().getStackInSlot(0))
                .thenScreenshot(5, "crc-hands-part-set")
                .thenServer(0, level -> {
                    ctx.hold(ItemStack.EMPTY);
                    clear(ctx, level);
                });
    }

    /* The CRC facing north with its terminal at {@link #TERMINAL}, its Power Entry in the rear's west bottom place. */
    private static void build(final ClientTestContext ctx, final ServerLevel level) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final BlockPos at = ctx.abs(TERMINAL.offset(x - pattern.terminalX(), y - pattern.terminalY(),
                            z - pattern.terminalZ()));
                    final char letter = pattern.charAt(x, y, z);
                    final BlockState state = letter == '#' ? CrcContent.TERMINAL.get().defaultBlockState()
                            .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                            : letter == 'G' ? CrcContent.GATE.get().defaultBlockState()
                            : letter == 'B' ? CrcContent.BUS.get().defaultBlockState()
                            : CrcContent.CASING.get().defaultBlockState();
                    level.setBlockAndUpdate(at, state);
                }
            }
        }
        level.setBlockAndUpdate(CrcStructure.rearPlace(ctx.abs(TERMINAL), Orientation.NORTH, 0, 0),
                CrcContent.POWER_ENTRY.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,
                        Direction.SOUTH));
    }

    /* Takes the CRC away. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        final MultiblockPattern pattern = CrcStructure.PATTERN;
        final BlockPos corner = TERMINAL.offset(-pattern.terminalX(), -pattern.terminalY(), -pattern.terminalZ());
        for (final BlockPos at : BlockPos.betweenClosed(ctx.abs(corner), ctx.abs(corner.offset(pattern.sizeX() - 1,
                pattern.sizeY() - 1, pattern.sizeZ() - 1)))) {
            level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        }
    }

    /*
     * A point just under the middle of the top edge of the aimed part's outline nearest the eye, on its near face, so
     * the outline's edge crosses the screen beside the crosshair.
     */
    private static Vec3 underNearTopEdge(final ClientTestContext ctx) {
        final Vector3f[] outline = HandsAim.aimed().orElseThrow(() -> new ClientTestFailure("nothing aimed at"))
                .outline();
        final Vec3 terminal = Vec3.atLowerCornerOf(ctx.abs(TERMINAL));
        final Vec3 eye = ctx.mc().player.getEyePosition();
        Vec3 nearest = null;
        for (int corner = 4; corner < 8; corner++) {
            final Vector3f a = outline[corner];
            final Vector3f b = outline[corner == 7 ? 4 : corner + 1];
            final Vec3 middle = terminal.add((a.x + b.x) / 2, (a.y + b.y) / 2, (a.z + b.z) / 2);
            if (nearest == null || middle.distanceToSqr(eye) < nearest.distanceToSqr(eye)) {
                nearest = middle;
            }
        }
        return nearest.subtract(0, UNDER_EDGE, 0);
    }

    /* The brightness of the screen's pixels in the columns either side of the crosshair. */
    private static int[][] window(final ClientTestContext ctx) {
        final int x = ctx.mc().getWindow().getGuiScaledWidth() / 2;
        final int y = ctx.mc().getWindow().getGuiScaledHeight() / 2;
        final int[][] out = new int[COLUMNS.length][2 * ROWS + 1];
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int column = 0; column < COLUMNS.length; column++) {
                for (int row = -ROWS; row <= ROWS; row++) {
                    final int argb = frame.pixel(x + COLUMNS[column], y + row);
                    out[column][row + ROWS] = (argb >> 16 & 0xFF) + (argb >> 8 & 0xFF) + (argb & 0xFF);
                }
            }
        }
        return out;
    }

    /* How many pixels are clearly darker in {@code with} than in {@code without}. */
    private static int darkened(final int[][] with, final int[][] without) {
        int count = 0;
        for (int column = 0; column < with.length; column++) {
            for (int row = 0; row < with[column].length; row++) {
                if (with[column][row] < without[column][row] * DARKER) {
                    count++;
                }
            }
        }
        return count;
    }

    /* Whether the crosshair is on the part of bone {@code bone}. */
    private static boolean aimedAt(final String bone) {
        return HandsAim.aimed().map(aimed -> aimed.part() == partOf(bone)).orElse(false);
    }

    /* The part of the CRC's model of bone {@code bone}, by its place among the parts it is worked by. */
    private static int partOf(final String bone) {
        final List<IHandPart> parts = CrcContent.HANDS.parts();
        for (int i = 0; i < parts.size(); i++) {
            if (parts.get(i).bone().equals(bone)) {
                return i;
            }
        }
        throw new ClientTestFailure("no part " + bone);
    }

    /* The middle of part {@code part} in the world, as the player's game has its doors now. */
    private static Vec3 middleOf(final ClientTestContext ctx, final int part) {
        final StructureHands hands = hands(ctx).orElseThrow(() -> new ClientTestFailure("no formed CRC drawn"));
        final Matrix4f placed = HandGeometry.placed(Orientation.NORTH,
                (float) CrcContent.MODEL.definition().detail().scale(), new Matrix4f());
        final Vector3f sum = new Vector3f();
        int count = 0;
        for (final HandGeometry.Box box : hands.model().geometry().boxes(part,
                hands.openness(ctx.mc().level.getGameTime()))) {
            for (final Vector3f corner : box.corners()) {
                sum.add(placed.transformPosition(corner));
                count++;
            }
        }
        return Vec3.atLowerCornerOf(ctx.abs(TERMINAL)).add(sum.x / count, sum.y / count, sum.z / count);
    }

    /* The hands of the CRC as the player's game has it, once it stands formed and merged there. */
    private static Optional<StructureHands> hands(final ClientTestContext ctx) {
        return ctx.mc().level != null && ctx.mc().level.getBlockEntity(ctx.abs(TERMINAL)) instanceof CrcBlockEntity crc
                && crc.merged() ? crc.hands() : Optional.empty();
    }

    private static CrcBlockEntity crc(final ClientTestContext ctx, final ServerLevel level) {
        if (!(level.getBlockEntity(ctx.abs(TERMINAL)) instanceof CrcBlockEntity crc)) {
            throw new ClientTestFailure("no CRC terminal at " + TERMINAL);
        }
        return crc;
    }
}
