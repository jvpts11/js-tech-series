/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.machine.ChainFaces;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.core.client.machine.MachineScreen;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The chain machines' own faces (computers/docs/design/survival.md), drawn as their approved designs draw them: each
 * machine with one, formed and idle in turn, its Main tab its picture's idle frame where the game draws no live part
 * (the plates, the lamps, the meter, the LCD, the switch, the sight glasses and the words), pixel for pixel.
 */
public final class ChainFaceClientTests {

    private static final BlockPos CORNER = new BlockPos(2, 1, 2);
    /* The ground the machines stand on, cleared between them: as large as the largest machine. */
    private static final int ROOM = 16;
    private static final int WAIT = 300;
    private static final int SETTLE = 10;
    /* The share of the picture's pixels the face may draw otherwise: its glints and the font's edges. */
    private static final double MAY_DIFFER = 0.02;
    /* How far a colour may be off and still be the picture's, in each channel. */
    private static final int CLOSE = 6;

    private ChainFaceClientTests() {
    }

    @ClientTest(timeoutTicks = 12_000)
    public static void faces_areTheirApprovedPictures(final ClientTestContext ctx) {
        ctx.afterTest(() -> ctx.mc().setScreen(null));
        final List<String> off = new ArrayList<>();
        for (final ChainMachine machine : ChainMachines.all()) {
            ChainFaces.of(machine).ifPresent(picture -> face(ctx, machine, picture, off));
        }
        ctx.then(0, () -> ctx.assertTrue(off.isEmpty(), "every face is its approved picture: " + off))
                .thenServer(0, level -> clear(ctx, level));
    }

    /* Builds {@code machine}, opens its screen and holds its face to its picture; a face that is not, in {@code off}. */
    private static void face(final ClientTestContext ctx, final ChainMachine machine, final FacePicture picture,
                             final List<String> off) {
        final BlockPos terminal = place(machine, '#');
        ctx.thenServer(0, level -> clear(ctx, level))
                .thenBuild(2, world -> lay(world, machine))
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(terminal))
                        instanceof ChainMachineBlockEntity built && built.formed(), WAIT,
                        machine.id() + " to form", level -> "it stands unformed")
                .thenTeleport(0, new BlockPos(terminal.getX(), CORNER.getY(), CORNER.getZ() - 2), Direction.SOUTH)
                .then(2, () -> ctx.lookAt(Vec3.atCenterOf(ctx.abs(terminal))))
                .then(2, () -> ctx.rightClick(terminal, Direction.NORTH))
                .thenWaitUntil(() -> ctx.mc().screen instanceof MachineScreen, WAIT, machine.id() + "'s screen to open")
                .then(0, () -> ctx.pointAt(1, 1))
                .then(SETTLE, () -> {
                    final int left = (ctx.mc().getWindow().getGuiScaledWidth() - MachineScreenLayout.WIDTH) / 2
                            + MachineScreenLayout.FACE_X;
                    final int top = (ctx.mc().getWindow().getGuiScaledHeight() - MachineScreenLayout.HEIGHT) / 2
                            + MachineScreenLayout.FACE_Y;
                    try (NativeImage drawn = ctx.captureGuiPixels(left, top, MachineScreenLayout.FACE_WIDTH,
                            MachineScreenLayout.FACE_HEIGHT); NativeImage idle = idleFrame(ctx, machine.id())) {
                        final int[] counts = compare(drawn, idle, picture);
                        if (counts[1] > counts[0] * MAY_DIFFER) {
                            off.add(machine.id() + " (" + counts[1] + " of " + counts[0] + " pixels off)");
                        }
                    }
                })
                .thenScreenshot(0, "chain-face-" + machine.id())
                .then(0, () -> ctx.mc().setScreen(null));
    }

    /* How many of the picture's pixels were compared, and how many the face draws otherwise. */
    private static int[] compare(final NativeImage drawn, final NativeImage idle, final FacePicture picture) {
        final List<int[]> live = live(picture);
        int compared = 0;
        int off = 0;
        for (int y = 0; y < MachineScreenLayout.FACE_HEIGHT; y++) {
            for (int x = 0; x < MachineScreenLayout.FACE_WIDTH; x++) {
                if (within(live, x, y)) {
                    continue;
                }
                compared++;
                if (!close(drawn.getPixelRGBA(x, y), idle.getPixelRGBA(x, y))) {
                    off++;
                }
            }
        }
        return new int[] {compared, off};
    }

    /* The boxes of the face the game draws live over the picture, each its left, top, right and bottom. */
    private static List<int[]> live(final FacePicture picture) {
        final List<int[]> boxes = new ArrayList<>();
        boxes.add(new int[] {MachineScreenLayout.PLATE_X - 1, MachineScreenLayout.NAME_PLATE_Y - 1, 100, 32});
        boxes.add(new int[] {MachineScreenLayout.LAMP_X - 2, MachineScreenLayout.LAMP_Y - 2,
                MachineScreenLayout.LAMP_X + 50, MachineScreenLayout.LAMP_Y + 30});
        // The meter with its bezel, and its range engraved under it, as wide as its words.
        final FacePicture.Meter meter = picture.meter();
        boxes.add(new int[] {0, meter.y() - 4, meter.x() + meter.w() + 12, meter.y() + meter.h() + 16});
        final FacePicture.Box lcd = picture.lcd();
        boxes.add(new int[] {lcd.x() - 4, lcd.y() - 4, lcd.x() + lcd.w() + 4, lcd.y() + lcd.h() + 4});
        final FacePicture.Toggle toggle = picture.toggle();
        boxes.add(new int[] {toggle.x() - 4, toggle.y() - 12, toggle.x() + 22, toggle.y() + 34});
        for (final FacePicture.Glass glass : picture.glasses()) {
            boxes.add(new int[] {glass.x() - 4, glass.y() - 5, glass.x() + glass.w() + 4, glass.y() + glass.h() + 5});
            glass.label().ifPresent(label -> boxes.add(new int[] {label.x() - 12, label.y() - 1, label.x() + 12,
                    label.y() + 8}));
        }
        for (final FacePicture.Word word : picture.words()) {
            final int x = (int) Math.floor(word.x());
            final int y = (int) Math.floor(word.y());
            boxes.add(new int[] {x - 1, y - 1, x + 5 * Math.max(4, word.text().length()) + 2, y + 8});
        }
        return boxes;
    }

    private static boolean within(final List<int[]> boxes, final int x, final int y) {
        for (final int[] box : boxes) {
            if (x >= box[0] && y >= box[1] && x < box[2] && y < box[3]) {
                return true;
            }
        }
        return false;
    }

    /* Whether two ABGR colours are within {@link #CLOSE} of each other in every channel. */
    private static boolean close(final int a, final int b) {
        for (int shift = 0; shift < 24; shift += 8) {
            if (Math.abs((a >> shift & 0xFF) - (b >> shift & 0xFF)) > CLOSE) {
                return false;
            }
        }
        return true;
    }

    /* The idle frame of the face named {@code face}, as the mod ships it. */
    private static NativeImage idleFrame(final ClientTestContext ctx, final String face) {
        final ResourceLocation frames = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "textures/gui/machine/face/" + face + ".png");
        try (InputStream in = ctx.mc().getResourceManager().open(frames); NativeImage whole = NativeImage.read(in)) {
            final NativeImage idle = new NativeImage(MachineScreenLayout.FACE_WIDTH, MachineScreenLayout.FACE_HEIGHT,
                    true);
            for (int y = 0; y < MachineScreenLayout.FACE_HEIGHT; y++) {
                for (int x = 0; x < MachineScreenLayout.FACE_WIDTH; x++) {
                    idle.setPixelRGBA(x, y, whole.getPixelRGBA(x, y));
                }
            }
            return idle;
        } catch (final IOException e) {
            throw new IllegalStateException("no picture for " + face, e);
        }
    }

    /* Clears the ground the machines stand on. */
    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (final BlockPos at : BlockPos.betweenClosed(CORNER, CORNER.offset(ROOM, ROOM, ROOM))) {
            level.setBlock(ctx.abs(at), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /* Lays {@code machine}'s blocks from {@link #CORNER} facing north, its own terminal at its place. */
    private static void lay(final TestWorldBuilder world, final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    final char letter = row.charAt(x);
                    final Block block = letter == '#' ? ChainMachines.terminalOf(machine).get()
                            : ChainMachineBlocks.byId(machine.legend().get(letter)).get();
                    final BlockState state = block.defaultBlockState();
                    world.setBlock(CORNER.offset(x, y, z), state.hasProperty(HorizontalDirectionalBlock.FACING)
                            ? state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH) : state);
                }
            }
        }
    }

    /* Where {@link #lay} sets the first block of the letter {@code letter} of {@code machine}'s shape. */
    private static BlockPos place(final ChainMachine machine, final char letter) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final int x = layers.get(y).get(z).indexOf(letter);
                if (x >= 0) {
                    return CORNER.offset(x, y, z);
                }
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no " + letter);
    }
}
