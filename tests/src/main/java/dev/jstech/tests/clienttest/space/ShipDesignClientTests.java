/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.space.client.shipyard.ShipDesignScreen;
import dev.jstech.space.gui.ShipDesignLayout;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipDesigner;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The Starship Design on a player's game, opened at a formed Small Shipyard's terminal: the approved builder's ship on
 * its dotted floor through the middle, its panels round it; the Workshop taken from its card and held over the joint
 * between the quarters and the reactor, where it snaps; let go there, it is set in the spine, and the shipyard keeps
 * the design. Held, the part is a hologram: cyan under the mouse over the void, green where it would be set over a
 * joint it snaps to, red over the nose it does not fit, the words by the mouse in the same colour.
 */
public final class ShipDesignClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    /* The booth's floor behind the terminal, in the test's place: the dock's (11, 4, 10). */
    private static final BlockPos BOOTH = CORNER.offset(11 + 11, 4 + 3, 10 + 13);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    /* The joint between the quarters and the reactor, the fourth along the spine. */
    private static final int QUARTERS_BACK = 3;
    private static final int ARRIVE = 600;
    private static final int OPEN = 200;
    private static final int SETTLE = 20;
    private static final int CHANNEL = 0xFF;
    /* How far the panels' cyan stands over red at the right panel's title. */
    private static final int CYAN = 60;
    /* A point of the frame over the void above the ship, away from every joint. */
    private static final int VOID_X = 300;
    private static final int VOID_Y = 100;
    /*
     * Where the colours are counted about the mouse, in the GUI's pixels: over it, as far up and to either side;
     * the words' band under it, from and to, as far left and right; and by how much a channel stands over another.
     */
    private static final int ABOVE = 16;
    private static final int AROUND = 14;
    private static final int WORDS_FROM = 8;
    private static final int WORDS_TO = 16;
    private static final int WORDS_LEFT = 60;
    private static final int WORDS_RIGHT = 16;
    private static final int HUE = 40;
    /* The colours' places in a count, the words' after the hologram's, and how many more pixels prove each. */
    private static final int CYAN_AT = 0;
    private static final int GREEN_AT = 1;
    private static final int RED_AT = 2;
    private static final int WORDS = 3;
    private static final int HOLOGRAM = 120;
    private static final int WORDS_SEEN = 20;

    private ShipDesignClientTests() {
    }

    @ClientTest(timeoutTicks = 2000)
    public static void design_drawsTheShipAndSnapsTheWorkshopToAJoint(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final float[][] joint = new float[1][];
        final ShipDesign ship = new ShipDesign("JS-01", List.of(part("orbiter_cockpit"), part("airlock"),
                part("quarters"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
                part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            ctx.server().execute(() -> {
                if (built[0] != null) {
                    clear(ctx, built[0]);
                }
            });
        });
        ctx.thenServer(0, level -> {
            built[0] = level;
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                // In the control booth, at the back of it, facing the terminal and the bay beyond.
                .thenTeleport(SETTLE, BOOTH, Direction.WEST)
                .thenServer(SETTLE, level -> ShipDesigner.open(ctx.serverPlayer(),
                        (ShipyardTerminalBlockEntity) level.getBlockEntity(terminal[0])))
                .thenAwaitScreen(ShipDesignScreen.class, OPEN)
                .then(SETTLE, () -> screen(ctx).edit(ship))
                .thenScreenshot(SETTLE, "ship-design")
                .then(0, () -> {
                    // Under the letters of the screen's tag, filled cyan.
                    final int[] title = frame(ctx, ShipDesignLayout.TAG_X1 - 10, ShipDesignLayout.TAG_Y1 - 2);
                    ctx.assertTrue(screen(ctx).design().equals(ship), "the ship is on the screen");
                    final int argb = pixel(ctx, title[0], title[1]);
                    ctx.assertTrue((argb >> 8 & CHANNEL) - (argb >> 16 & CHANNEL) > CYAN
                            || (argb & CHANNEL) - (argb >> 16 & CHANNEL) > CYAN, "the panels are drawn in cyan: "
                            + Integer.toHexString(argb));
                })
                .then(0, () -> {
                    final ShipDesignScreen screen = screen(ctx);
                    final int[] card = frame(ctx, ShipDesignLayout.cardX(0) + 30, ShipDesignLayout.cardY(0) + 20);
                    screen.mouseClicked(card[0], card[1], InputConstants.MOUSE_BUTTON_LEFT);
                    joint[0] = screen.joint(QUARTERS_BACK).orElseThrow();
                    ctx.pointAt(joint[0][0], joint[0][1]);
                })
                .thenScreenshot(SETTLE, "ship-design-drag")
                .then(0, () -> screen(ctx).mouseReleased(joint[0][0], joint[0][1],
                        InputConstants.MOUSE_BUTTON_LEFT))
                .thenAssert(SETTLE, () -> screen(ctx).design().spine().equals(List.of(part("orbiter_cockpit"),
                        part("airlock"), part("quarters"), part("workshop"), part("reactor_flush"))),
                        "the workshop let go on the joint is set in the spine there")
                .thenScreenshot(0, "ship-design-ready")
                .thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity
                        dock && dock.design().spine().contains(part("workshop")), ARRIVE,
                        "the shipyard to keep the design", level -> "the shipyard's design: "
                                + ((ShipyardTerminalBlockEntity) level.getBlockEntity(terminal[0])).design());
    }

    @ClientTest(timeoutTicks = 2000)
    public static void held_isAHologramCyanLooseGreenSnappedRedWhereItDoesNotFit(final ClientTestContext ctx) {
        final ServerLevel[] built = new ServerLevel[1];
        final BlockPos[] terminal = new BlockPos[1];
        final ShipDesign ship = new ShipDesign("JS-01", List.of(part("orbiter_cockpit"), part("airlock"),
                part("quarters"), part("reactor_flush")), List.of(part("twin_bells"), part("side_intakes"),
                part("blended_delta"), part("twin_canted_fins"), part("blisters"), part("wheels")));
        // Where the mouse rests: over the void above the ship, on the joint the workshop snaps to, on the nose.
        final float[][] spot = new float[3][];
        final int[][] before = new int[3][];
        ctx.afterTest(() -> {
            ctx.mc().setScreen(null);
            ctx.server().execute(() -> {
                if (built[0] != null) {
                    clear(ctx, built[0]);
                }
            });
        });
        ctx.thenServer(0, level -> {
            built[0] = level;
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenTeleport(SETTLE, BOOTH, Direction.WEST)
                .thenServer(SETTLE, level -> ShipDesigner.open(ctx.serverPlayer(),
                        (ShipyardTerminalBlockEntity) level.getBlockEntity(terminal[0])))
                .thenAwaitScreen(ShipDesignScreen.class, OPEN)
                .then(SETTLE, () -> screen(ctx).edit(ship))
                .then(SETTLE, () -> {
                    final int[] free = frame(ctx, VOID_X, VOID_Y);
                    spot[0] = new float[] {free[0], free[1]};
                    spot[1] = screen(ctx).joint(QUARTERS_BACK).orElseThrow();
                    spot[2] = screen(ctx).joint(0).orElseThrow();
                    ctx.pointAt(spot[0][0], spot[0][1]);
                })
                // Nothing held yet: what each place shows of each colour, to be told from the hologram's.
                .then(SETTLE, () -> {
                    for (int i = 0; i < spot.length; i++) {
                        before[i] = colours(ctx, spot[i]);
                    }
                })
                .then(0, () -> {
                    final int[] card = frame(ctx, ShipDesignLayout.cardX(0) + 30, ShipDesignLayout.cardY(0) + 20);
                    screen(ctx).mouseClicked(card[0], card[1], InputConstants.MOUSE_BUTTON_LEFT);
                    ctx.pointAt(spot[0][0], spot[0][1]);
                })
                .thenScreenshot(SETTLE, "ship-design-held-loose")
                .then(0, () -> {
                    final int[] now = colours(ctx, spot[0]);
                    ctx.assertTrue(now[CYAN_AT] - before[0][CYAN_AT] > HOLOGRAM, "the workshop held over the void is "
                            + "a cyan hologram under the mouse: " + now[CYAN_AT] + " cyan pixels, " + before[0][CYAN_AT]
                            + " before");
                    ctx.assertTrue(now[WORDS + CYAN_AT] - before[0][WORDS + CYAN_AT] > WORDS_SEEN, "and the words "
                            + "by the mouse are cyan: " + now[WORDS + CYAN_AT]);
                    ctx.pointAt(spot[1][0], spot[1][1]);
                })
                .thenScreenshot(SETTLE, "ship-design-held-snaps")
                .then(0, () -> {
                    final int[] now = colours(ctx, spot[1]);
                    ctx.assertTrue(now[GREEN_AT] - before[1][GREEN_AT] > HOLOGRAM, "over the joint it snaps to, it "
                            + "stands where it would be set, green: " + now[GREEN_AT] + " green pixels, "
                            + before[1][GREEN_AT] + " before");
                    ctx.assertTrue(now[WORDS + GREEN_AT] - before[1][WORDS + GREEN_AT] > WORDS_SEEN, "and the words "
                            + "by the mouse are green: " + now[WORDS + GREEN_AT]);
                    ctx.pointAt(spot[2][0], spot[2][1]);
                })
                .thenScreenshot(SETTLE, "ship-design-held-wrong")
                .then(0, () -> {
                    final int[] now = colours(ctx, spot[2]);
                    ctx.assertTrue(now[RED_AT] - before[2][RED_AT] > HOLOGRAM, "over the nose, which only a cockpit "
                            + "fits, it is red: " + now[RED_AT] + " red pixels, " + before[2][RED_AT] + " before");
                    ctx.assertTrue(now[WORDS + RED_AT] - before[2][WORDS + RED_AT] > WORDS_SEEN, "and the words by "
                            + "the mouse saying why are red: " + now[WORDS + RED_AT]);
                    screen(ctx).mouseReleased(spot[2][0], spot[2][1], InputConstants.MOUSE_BUTTON_LEFT);
                })
                .thenAssert(SETTLE, () -> screen(ctx).design().equals(ship), "let go on the nose, the workshop is set "
                        + "nowhere");
    }

    private static ShipDesignScreen screen(final ClientTestContext ctx) {
        return ctx.screen(ShipDesignScreen.class);
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
    }

    /* The window's point of the frame's pixel ({@code x}, {@code y}). */
    private static int[] frame(final ClientTestContext ctx, final int x, final int y) {
        final ShipDesignScreen screen = screen(ctx);
        final float scale = screen.getXSize() / (float) ShipDesignLayout.WIDTH;
        return new int[] {Math.round(screen.getGuiLeft() + (x + 0.5F) * scale),
            Math.round(screen.getGuiTop() + (y + 0.5F) * scale)};
    }

    /*
     * How many pixels about {@code at} are cyan, green and red: first over it, where the hologram stands, then in the
     * band under it where the words by the mouse are written.
     */
    private static int[] colours(final ClientTestContext ctx, final float[] at) {
        final int[] out = new int[2 * WORDS];
        final int cx = Math.round(at[0]);
        final int cy = Math.round(at[1]);
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = cy - ABOVE; y <= cy + 4; y++) {
                for (int x = cx - AROUND; x <= cx + AROUND; x++) {
                    tally(frame.pixel(x, y), out, 0);
                }
            }
            for (int y = cy + WORDS_FROM; y <= cy + WORDS_TO; y++) {
                for (int x = cx - WORDS_LEFT; x <= cx + WORDS_RIGHT; x++) {
                    tally(frame.pixel(x, y), out, WORDS);
                }
            }
        }
        return out;
    }

    private static void tally(final int argb, final int[] out, final int from) {
        final int r = argb >> 16 & CHANNEL;
        final int g = argb >> 8 & CHANNEL;
        final int b = argb & CHANNEL;
        if (b > r + HUE && g > r + HUE) {
            out[from + CYAN_AT]++;
        } else if (g > r + HUE && g > b + HUE / 2) {
            out[from + GREEN_AT]++;
        } else if (r > g + HUE && r > b + HUE) {
            out[from + RED_AT]++;
        }
    }

    private static int pixel(final ClientTestContext ctx, final int x, final int y) {
        try (ClientTestContext.FrameSampler frame = ctx.sampleFrame()) {
            return frame.pixel(x, y);
        }
    }

    private static void clear(final ClientTestContext ctx, final ServerLevel level) {
        for (int x = X0; x <= X1; x++) {
            for (int z = Z0; z <= Z1; z++) {
                for (int y = Y_BOTTOM; y <= Y_TOP; y++) {
                    final Block block = y == Y_BOTTOM ? Blocks.BEDROCK : y < Y_GRASS ? Blocks.DIRT
                            : y == Y_GRASS ? Blocks.GRASS_BLOCK : Blocks.AIR;
                    level.setBlock(ctx.abs(new BlockPos(x, y, z)), block.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
            }
        }
    }
}
