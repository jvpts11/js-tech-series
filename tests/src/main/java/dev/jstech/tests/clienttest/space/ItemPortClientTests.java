/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.space;

import dev.jstech.core.multiblock.plan.PlannedBuild;
import dev.jstech.space.client.shipyard.ItemPortScreen;
import dev.jstech.space.hangar.HangarBlocks;
import dev.jstech.space.hangar.ItemPortBlockEntity;
import dev.jstech.space.hangar.ItemPortMenu;
import dev.jstech.space.ship.ShipDesign;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.shipyard.ShipyardTerminalBlockEntity;
import dev.jstech.testkit.client.ClientTestHelper;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.space.TestShipyards;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * A shipyard's Item Port on a player's game, as the approved screen draws it: a ship sent to the dock, some of its
 * blocks put in the port's hold, which passes them on to the arms; the player uses the port and its screen opens, its
 * display listing what the ship still lacks in the display's green; a block the ship does not need is refused by the
 * hold, one it lacks taken.
 */
public final class ItemPortClientTests {

    /* The dock dug into the land where the test stands, as the shipyard's client test builds it. */
    private static final BlockPos CORNER = new BlockPos(-11, -1, 6);
    private static final int X0 = -13;
    private static final int X1 = 13;
    private static final int Z0 = 4;
    private static final int Z1 = 34;
    private static final int Y_BOTTOM = -2;
    private static final int Y_GRASS = 1;
    private static final int Y_TOP = 16;
    private static final int ARRIVE = 600;
    private static final int SETTLE = 20;
    /* The kinds of block put in the port's hold before the screen opens. */
    private static final int KINDS = 3;
    /* How many of the display's pixels are its bright green at least: the ship's line and the rows. */
    private static final int GREEN_AT_LEAST = 150;
    private static final int CHANNEL = 0xFF;
    private static final ShipDesign SHIP = new ShipDesign("JS-01", List.of(part("orbiter_cockpit"), part("airlock"),
            part("reactor_flush")), List.of(part("twin_bells"), part("blended_delta"), part("wheels")));

    private ItemPortClientTests() {
    }

    @ClientTest(timeoutTicks = 2400)
    public static void port_showsWhatTheShipLacksAndRefusesWhatItDoesNot(final ClientTestContext ctx) {
        final BlockPos[] terminal = new BlockPos[1];
        final BlockPos[] port = new BlockPos[1];
        final ServerLevel[] built = new ServerLevel[1];
        ctx.afterTest(() -> ctx.server().execute(() -> {
            if (ctx.mc().screen != null) {
                ctx.mc().execute(() -> ctx.player().closeContainer());
            }
            if (built[0] != null) {
                clear(ctx, built[0]);
                built[0].players().forEach(player -> player.setGameMode(GameType.CREATIVE));
            }
        }));
        ctx.thenServer(0, level -> {
            built[0] = level;
            clear(ctx, level);
            terminal[0] = TestShipyards.build(level, ctx.abs(CORNER));
        }).thenWaitUntilServer(level -> level.getBlockEntity(terminal[0]) instanceof ShipyardTerminalBlockEntity dock
                && dock.formed(), ARRIVE, "the dock to form", level -> "not formed")
                .thenServer(SETTLE, level -> {
                    final ShipyardTerminalBlockEntity dock = (ShipyardTerminalBlockEntity) level
                            .getBlockEntity(terminal[0]);
                    dock.design(SHIP);
                    ctx.assertTrue(dock.confirm(), "the ship is sent to the dock");
                    final PlannedBuild plan = dock.build().orElseThrow();
                    ctx.assertTrue(!plan.plan().spots().isEmpty(), "its plan has blocks");
                    port[0] = portOf(level, terminal[0]);
                    final IItemHandler hold = level.getCapability(Capabilities.ItemHandler.BLOCK, port[0],
                            Direction.UP);
                    int kinds = 0;
                    for (final Map.Entry<Item, Integer> need : dock.assembler().needs().entrySet()) {
                        if (kinds++ == KINDS) {
                            break;
                        }
                        hold.insertItem(kinds - 1, new ItemStack(need.getKey(), Math.min(need.getValue(), 64)),
                                false);
                    }
                    ctx.serverPlayer().setGameMode(GameType.CREATIVE);
                    ctx.serverPlayer().getInventory().setItem(0, new ItemStack(Items.COAL, 41));
                    final Vec3 out = Vec3.atCenterOf(port[0]).add(0, 0, 2);
                    ctx.serverPlayer().teleportTo(level, out.x, out.y - 1, out.z, 180F, 0F);
                })
                .thenWaitUntil(() -> ctx.player().position().distanceTo(Vec3.atCenterOf(port[0]).add(0, -1, 2)) < 1,
                        ARRIVE, "the player to stand at the port")
                .then(2, () -> ctx.mc().gameMode.useItemOn(ctx.player(), InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(port[0]), Direction.SOUTH, port[0], false)))
                .thenAwaitScreen(ItemPortScreen.class, ARRIVE)
                .thenWaitUntil(() -> ctx.mc().player.containerMenu instanceof ItemPortMenu menu
                        && menu.view().building(), ARRIVE, "the port's screen to hear what the ship lacks")
                .thenScreenshot(SETTLE, "item-port-hold")
                .then(0, () -> {
                    final ItemPortMenu menu = (ItemPortMenu) ctx.mc().player.containerMenu;
                    ctx.assertTrue(menu.view().ship().equals(SHIP.name()) && !menu.view().rows().isEmpty(),
                            "the display names the ship and lists what it lacks: " + menu.view());
                    ctx.assertTrue(green(ctx) >= GREEN_AT_LEAST, "the display writes it in its bright green: "
                            + green(ctx) + " pixels");
                    ctx.assertTrue(!menu.getSlot(KINDS).mayPlace(new ItemStack(Items.COAL)),
                            "coal, which the ship does not need, does not go in");
                    final Item lacked = menu.view().rows().stream().filter(row -> !row.enough()).findFirst()
                            .orElseThrow().item();
                    ctx.assertTrue(menu.getSlot(KINDS).mayPlace(new ItemStack(lacked)), "a block it still lacks, "
                            + lacked + ", does");
                });
    }

    /* The Item Port of the dock whose terminal is at {@code terminal}, found among the dock's blocks. */
    private static BlockPos portOf(final ServerLevel level, final BlockPos terminal) {
        for (final BlockPos at : BlockPos.betweenClosed(terminal.offset(-24, -8, -28), terminal.offset(24, 8, 28))) {
            if (level.getBlockState(at).is(HangarBlocks.ITEM_PORT.get())
                    && level.getBlockEntity(at) instanceof ItemPortBlockEntity) {
                return at.immutable();
            }
        }
        throw new IllegalStateException("the dock has no Item Port");
    }

    /* How many pixels of the display's area are its bright green. */
    private static int green(final ClientTestContext ctx) {
        final int width = ctx.mc().getWindow().getGuiScaledWidth();
        final int height = ctx.mc().getWindow().getGuiScaledHeight();
        final int left = (width - 176) / 2 + 10;
        final int top = (height - 253) / 2 + 18;
        int count = 0;
        try (ClientTestHelper.FrameSampler frame = ctx.sampleFrame()) {
            for (int y = top; y < top + 88; y++) {
                for (int x = left; x < left + 156; x++) {
                    final int argb = frame.pixel(x, y);
                    if ((argb >> 8 & CHANNEL) > 200 && (argb >> 16 & CHANNEL) < 160 && (argb & CHANNEL) < 160) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private static ResourceLocation part(final String path) {
        return SmallShipParts.id("small_" + path);
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
