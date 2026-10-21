/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core Test Kit.
 */
package dev.jstech.testkit.kit;

import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.testkit.gametest.Expect;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A scene for sealed rooms: a closed box built in the arena, its walls of a block a test names and its inside air;
 * the room found there; and the room filled with gases at a pressure, emptied first.
 *
 * <pre>{@code
 * RoomScenes.seal(helper, new BlockPos(1, 1, 1), new BlockPos(4, 4, 4), Blocks.STONE);
 * helper.startSequence()
 *         .thenWaitUntil(() -> RoomScenes.expectRoom(helper, INSIDE))
 *         .thenExecute(() -> RoomScenes.fill(RoomScenes.expectRoom(helper, INSIDE), 50.0,
 *                 Map.of(AirMix.OXYGEN, 1.0)))
 *         .thenSucceed();
 * }</pre>
 *
 * <p>Rooms are kept by the level, beside every test running in it: a test of rooms runs best in a batch of its own.
 */
public final class RoomScenes {

    private RoomScenes() {
    }

    /**
     * Builds a closed box from the arena's place {@code low} to {@code high}, both corners included: its shell of
     * {@code wall}, its inside air; and asks the rooms kit for the room inside it, as a block placed there would,
     * until {@link #clear}.
     */
    public static void seal(final GameTestHelper helper, final BlockPos low, final BlockPos high, final Block wall) {
        final BlockPos from = BlockPos.min(low, high);
        final BlockPos to = BlockPos.max(low, high);
        for (int x = from.getX(); x <= to.getX(); x++) {
            for (int y = from.getY(); y <= to.getY(); y++) {
                for (int z = from.getZ(); z <= to.getZ(); z++) {
                    final boolean shell = x == from.getX() || x == to.getX() || y == from.getY() || y == to.getY()
                            || z == from.getZ() || z == to.getZ();
                    helper.setBlock(new BlockPos(x, y, z), shell ? wall : Blocks.AIR);
                }
            }
        }
        SealedRooms.track(helper.getLevel(), helper.absolutePos(middle(from, to)));
    }

    /** Stops asking for the room in the box from {@code low} to {@code high}, and forgets every room in it. */
    public static void clear(final GameTestHelper helper, final BlockPos low, final BlockPos high) {
        final BlockPos from = helper.absolutePos(BlockPos.min(low, high));
        final BlockPos to = helper.absolutePos(BlockPos.max(low, high));
        SealedRooms.untrack(helper.getLevel(), middle(from, to));
        SealedRooms.forget(helper.getLevel(), new AABB(Vec3.atLowerCornerOf(from), Vec3.atLowerCornerOf(to)
                .add(1, 1, 1)));
    }

    /** The sealed room that holds the arena's place {@code inside}, when there is one. */
    public static Optional<Room> roomAt(final GameTestHelper helper, final BlockPos inside) {
        return SealedRooms.roomAt(helper.getLevel(), helper.absolutePos(inside));
    }

    /** The sealed room that holds {@code inside}; else a failure saying there is none. */
    public static Room expectRoom(final GameTestHelper helper, final BlockPos inside) {
        return Expect.present(helper, inside, "a sealed room", roomAt(helper, inside));
    }

    /**
     * Empties {@code room} and fills it with gases at {@code kilopascals} in all, each its share of them (the shares
     * add up to one), at the temperature of a room kept for people.
     */
    public static void fill(final Room room, final double kilopascals, final Map<ResourceLocation, Double> shares) {
        room.empty();
        for (final Map.Entry<ResourceLocation, Double> gas : shares.entrySet()) {
            room.add(gas.getKey(), GasMath.moles(kilopascals * gas.getValue(), room.gasVolume(), GasMath.TEMPERED));
        }
    }

    private static BlockPos middle(final BlockPos from, final BlockPos to) {
        return new BlockPos((from.getX() + to.getX()) / 2, (from.getY() + to.getY()) / 2,
                (from.getZ() + to.getZ()) / 2);
    }
}
