/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.client.RedstoneInterfaceScreen;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

/**
 * The Redstone Interfaces, one era to a column from the Vintage at the west, their lenses toward the player, to set
 * beside their approved page. The lower row hangs from a running computer through a chain of hubs behind it, and is
 * set so its lenses and lamps show each thing they can: reading nothing (the green lamp), and emitting at half and at
 * full strength (the amber one). The upper row has no computer, so it is dark. Two more stand aside, one looking up and one looking east,
 * placed the other ways an observer can be. And the window: set from it, and opened in each era's skin.
 */
public final class RedstoneInterfaceClientTests {

    private static final BlockPos STAND = new BlockPos(0, 4, -4);
    private static final BlockPos CLOSE = new BlockPos(0, 2, 0);
    private static final int FIRST_COLUMN = -2;
    private static final int LIVE_Y = 2;
    private static final int ROW_Z = 2;
    private static final int HUB_Z = 3;
    private static final BlockPos COMPUTER = new BlockPos(3, 2, HUB_Z);
    private static final BlockPos LOOKING_UP = new BlockPos(-4, 2, ROW_Z);
    private static final BlockPos LOOKING_EAST = new BlockPos(-5, 3, ROW_Z);
    private static final float LOOK_DOWN = 12.0F;
    private static final int LINK_WAIT = 200;
    private static final int SETTLE = 4;
    /* Every step waits on the server's answer reaching the window, which on a loaded machine takes a while. */
    private static final int SCREEN_WAIT = 160;
    /** What each era's live interface is set to: below 0 reads, else the strength it emits. */
    private static final int[] SETTINGS = {-1, 5, 15, 8, 15};

    private RedstoneInterfaceClientTests() {
    }

    @ClientTest(timeoutTicks = 600)
    public static void interfaces_showWhatTheyDoInEachEra(final ClientTestContext ctx) {
        atTheRows(ctx)
                .thenServer(0, level -> {
                    for (int i = 0; i < SETTINGS.length; i++) {
                        if (level.getBlockEntity(ctx.abs(new BlockPos(FIRST_COLUMN + i, LIVE_Y, ROW_Z)))
                                instanceof RedstoneInterfaceBlockEntity sensor) {
                            if (SETTINGS[i] < 0) {
                                sensor.read("");
                            } else {
                                sensor.emit(SETTINGS[i], "");
                            }
                        }
                    }
                })
                .thenWaitUntil(() -> shownOnTheClient(ctx), LINK_WAIT, "the client to see what each one does")
                .then(0, () -> ctx.player().setXRot(LOOK_DOWN))
                .thenScreenshot(20, "redstone-interfaces");
    }

    /**
     * The window sets the interface: OUT, a strength clicked on its cells, and a name typed, each reaching the server,
     * the Software box writing the same setting; then each era's window, in its own skin.
     */
    @ClientTest(timeoutTicks = 1600)
    public static void window_setsTheModeTheStrengthAndTheName(final ClientTestContext ctx) {
        final BlockPos vintage = new BlockPos(FIRST_COLUMN, LIVE_Y, ROW_Z);
        atTheRows(ctx)
                // Within reach of every interface of the row, which a click from where the rows are seen is not.
                .thenTeleport(SETTLE, CLOSE, Direction.SOUTH)
                .thenRightClick(SETTLE, vintage)
                .thenAwaitScreen(RedstoneInterfaceScreen.class, SCREEN_WAIT)
                .then(SETTLE, () -> ctx.click(window(ctx).outCenter()[0], window(ctx).outCenter()[1]))
                .thenWaitUntilServer(level -> sensorAt(ctx, level, vintage).emits(), SCREEN_WAIT,
                        "OUT to reach the interface", level -> "it still reads")
                .then(SETTLE, () -> ctx.click(window(ctx).cellCenter(9)[0], window(ctx).cellCenter(9)[1]))
                .thenWaitUntilServer(level -> sensorAt(ctx, level, vintage).strength() == 9, SCREEN_WAIT,
                        "the cell clicked to set the strength",
                        level -> "its strength is " + sensorAt(ctx, level, vintage).strength())
                .then(SETTLE, () -> ctx.click(window(ctx).nameCenter()[0], window(ctx).nameCenter()[1]))
                .then(SETTLE, () -> ctx.type("Gate"))
                .thenWaitUntil(() -> window(ctx).softwareLines()[0].equals("SET REDSTONE 'Gate' OUT 9"),
                        SCREEN_WAIT, "the Software box to write the setting and the name")
                .then(0, () -> ctx.assertTrue(window(ctx).softwareLines()[1].equals("redstone(\"Gate\").Out(9);"),
                        "and the Sigma call; got " + window(ctx).softwareLines()[1]))
                .thenScreenshot(4, "vintage-window")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenWaitUntilServer(level -> "Gate".equals(sensorAt(ctx, level, vintage).name())
                                && sensorAt(ctx, level, vintage).setBy().isEmpty(), SCREEN_WAIT,
                        "the name to be taken when the window closes, and no mark on a setting made there",
                        level -> "it is called '" + sensorAt(ctx, level, vintage).name() + "', set by '"
                                + sensorAt(ctx, level, vintage).setBy() + "'")
                .thenRightClick(SETTLE, new BlockPos(FIRST_COLUMN + 1, LIVE_Y, ROW_Z))
                .thenAwaitScreen(RedstoneInterfaceScreen.class, SCREEN_WAIT)
                .thenScreenshot(4, "legacy-window")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenRightClick(SETTLE, new BlockPos(FIRST_COLUMN + 2, LIVE_Y, ROW_Z))
                .thenAwaitScreen(RedstoneInterfaceScreen.class, SCREEN_WAIT)
                .thenScreenshot(4, "transition-window")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenRightClick(SETTLE, new BlockPos(FIRST_COLUMN + 3, LIVE_Y, ROW_Z))
                .thenAwaitScreen(RedstoneInterfaceScreen.class, SCREEN_WAIT)
                .thenScreenshot(4, "standard-window")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenRightClick(SETTLE, new BlockPos(FIRST_COLUMN + 4, LIVE_Y, ROW_Z))
                .thenAwaitScreen(RedstoneInterfaceScreen.class, SCREEN_WAIT)
                .thenScreenshot(4, "advanced-window")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE));
    }

    /*
     * The two rows of interfaces and the computer behind the lower one, the player standing before them, the lower row
     * linked.
     */
    private static ClientTestContext atTheRows(final ClientTestContext ctx) {
        final List<Supplier<? extends Block>> eras = List.of(ComputingModule.VINTAGE_REDSTONE_INTERFACE,
                ComputingModule.LEGACY_REDSTONE_INTERFACE, ComputingModule.TRANSITION_REDSTONE_INTERFACE,
                ComputingModule.STANDARD_REDSTONE_INTERFACE, ComputingModule.ADVANCED_REDSTONE_INTERFACE);
        return ctx.thenBuild(0, world -> {
                    /*
                     * A peripheral cable runs from one computer to one device, so the row hangs from a chain of hubs
                     * instead, each interface against the hub behind it and each hub on a port of the next.
                     */
                    for (int x = FIRST_COLUMN; x < COMPUTER.getX(); x++) {
                        world.setBlock(new BlockPos(x, LIVE_Y, HUB_Z), ComputingModule.STANDARD_HUB.get());
                    }
                    world.placeRunningPersonalComputer(COMPUTER);
                    for (int i = 0; i < eras.size(); i++) {
                        final BlockState lensNorth = facing(eras.get(i), Direction.NORTH);
                        world.setBlock(new BlockPos(FIRST_COLUMN + i, LIVE_Y, ROW_Z), lensNorth);
                        world.setBlock(new BlockPos(FIRST_COLUMN + i, LIVE_Y + 1, ROW_Z), lensNorth);
                    }
                    world.setBlock(LOOKING_UP, facing(ComputingModule.STANDARD_REDSTONE_INTERFACE, Direction.UP));
                    world.setBlock(LOOKING_EAST, facing(ComputingModule.LEGACY_REDSTONE_INTERFACE, Direction.EAST));
                })
                .thenTeleport(4, STAND, Direction.SOUTH)
                .thenWaitUntilServer(level -> unlinked(ctx, level, eras.size()).isEmpty(), LINK_WAIT,
                        "the lower row to link to the computer behind it",
                        level -> "unlinked in the row: " + unlinked(ctx, level, eras.size()));
    }

    private static BlockState facing(final Supplier<? extends Block> block, final Direction lens) {
        return block.get().defaultBlockState().setValue(RedstoneInterfaceBlock.FACING, lens);
    }

    private static RedstoneInterfaceScreen window(final ClientTestContext ctx) {
        return ctx.screen(RedstoneInterfaceScreen.class);
    }

    /* The interface at {@code relative}, as the server holds it: asked on the server's thread, the one it answers. */
    private static RedstoneInterfaceBlockEntity sensorAt(final ClientTestContext ctx, final ServerLevel level,
                                                         final BlockPos relative) {
        if (!(level.getBlockEntity(ctx.abs(relative)) instanceof RedstoneInterfaceBlockEntity sensor)) {
            throw new ClientTestFailure("no Redstone Interface at " + relative);
        }
        return sensor;
    }

    /* The columns of the lower row whose interface is not linked yet, as the server sees them. */
    private static List<Integer> unlinked(final ClientTestContext ctx, final ServerLevel level, final int count) {
        final List<Integer> waiting = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (!(level.getBlockEntity(ctx.abs(new BlockPos(FIRST_COLUMN + i, LIVE_Y, ROW_Z)))
                    instanceof RedstoneInterfaceBlockEntity sensor) || !sensor.live()) {
                waiting.add(FIRST_COLUMN + i);
            }
        }
        return waiting;
    }

    /* Whether the client's copies of the lower row show the strength each was set to. */
    private static boolean shownOnTheClient(final ClientTestContext ctx) {
        for (int i = 0; i < SETTINGS.length; i++) {
            final Level seen = ctx.mc().level;
            if (seen == null || !(seen.getBlockEntity(ctx.abs(new BlockPos(FIRST_COLUMN + i, LIVE_Y, ROW_Z)))
                    instanceof RedstoneInterfaceBlockEntity sensor)
                    || sensor.shownStrength() != Math.max(0, SETTINGS[i])
                    || sensor.emits() != SETTINGS[i] >= 0) {
                return false;
            }
        }
        return true;
    }
}
