/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import dev.jstech.core.audio.SoundContext;
import dev.jstech.core.audio.SoundSpace;
import dev.jstech.core.audio.SoundTraits;
import dev.jstech.core.audio.acoustics.AcousticCues;
import dev.jstech.core.audio.acoustics.Acoustics;
import dev.jstech.core.audio.acoustics.EarRing;
import dev.jstech.core.audio.acoustics.ListenerAirPayload;
import dev.jstech.core.gametest.GameTestPlayers;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The server's part of how sound travels: a player told the air round their head, their sealed room's with its size
 * or the place's; a blast ringing the ears of whoever stands near it, less with distance and not at all through ear
 * protection; a blow on a living thing picked by its kind; and what a declared sound says of how it behaves.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class AcousticsGameTests {

    private static final String ARENA = "empty";
    /* Rooms share the world's state, so the room test has the rooms' batch. */
    private static final String ROOMS = "jscore_rooms";
    private static final BlockPos LOW = new BlockPos(2, 1, 2);
    private static final BlockPos HIGH = new BlockPos(5, 4, 5);
    private static final BlockPos INSIDE = new BlockPos(3, 2, 3);

    private AcousticsGameTests() {
    }

    @GameTest(template = ARENA, batch = ROOMS, timeoutTicks = 200)
    public static void airOf_inASealedRoomIsTheRoomsAirAndSize(final GameTestHelper helper) {
        for (int x = LOW.getX(); x <= HIGH.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH.getZ(); z++) {
                    final boolean shell = x == LOW.getX() || x == HIGH.getX() || y == LOW.getY() || y == HIGH.getY()
                            || z == LOW.getZ() || z == HIGH.getZ();
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
        SealedRooms.track(helper.getLevel(), helper.absolutePos(INSIDE));
        final ServerPlayer player = GameTestPlayers.join(helper, "acoustics_room");
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SealedRooms.roomAt(helper.getLevel(),
                        helper.absolutePos(INSIDE)).isPresent(), "the hut is a room"))
                .thenExecute(() -> {
                    // Standing in the hut, the player's head is in its upper half.
                    final Vec3 feet = Vec3.atBottomCenterOf(helper.absolutePos(INSIDE));
                    player.setPos(feet.x, feet.y - 0.2, feet.z);
                    final Room room = SealedRooms.roomAt(helper.getLevel(), helper.absolutePos(INSIDE)).orElseThrow();
                    final ListenerAirPayload air = Acoustics.airOf(player);
                    helper.assertTrue(Math.abs(air.pressure() - room.pressure()) < 0.1,
                            "the room's pressure; " + air + " in " + room.pressure());
                    helper.assertTrue(air.roomVolume() == 8 && air.roomSurface() == 24,
                            "and its eight blocks of air in twenty-four square metres of walls; " + air);
                    room.empty();
                    helper.assertTrue(Acoustics.airOf(player).pressure() < 0.01, "a room emptied is a vacuum");
                })
                .thenExecute(() -> {
                    SealedRooms.untrack(helper.getLevel(), helper.absolutePos(INSIDE));
                    GameTestPlayers.leave(player);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void airOf_outsideARoomIsThePlacesAir(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.join(helper, "acoustics_open");
        final ListenerAirPayload air = Acoustics.airOf(player);
        GameTestPlayers.leave(player);
        helper.assertTrue(Math.abs(air.pressure() - 101.3) < 1 && air.roomVolume() == 0 && air.roomSurface() == 0,
                "the overworld's air at the player's height, and no room; " + air);
        helper.assertTrue(air.same(new ListenerAirPayload(air.pressure() + 0.1F, air.temperature(), 0, 0)),
                "a change too small to hear is not told again");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ringOf_ringsTheEarsNearABlastLessFartherAway(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.join(helper, "acoustics_blast");
        final Vec3 eyes = player.getEyePosition();
        final EarRing close = Acoustics.ringOf(player, eyes.add(1, 0, 0), 4);
        final EarRing far = Acoustics.ringOf(player, eyes.add(8, 0, 0), 4);
        final EarRing beyond = Acoustics.ringOf(player, eyes.add(20, 0, 0), 4);
        GameTestPlayers.leave(player);
        helper.assertTrue(close.rings() && far.rings() && close.strength() > far.strength()
                && close.ticks() > far.ticks(), "a blast close by rings harder and longer; " + close + ", " + far);
        helper.assertFalse(beyond.rings(), "and not at all beyond its reach; " + beyond);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ringOf_doesNotRingEarsTheyProtect(final GameTestHelper helper) {
        final ServerPlayer player = GameTestPlayers.join(helper, "acoustics_helmet");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.TURTLE_HELMET));
        final EarRing ring = Acoustics.ringOf(player, player.getEyePosition().add(1, 0, 0), 4);
        GameTestPlayers.leave(player);
        helper.assertTrue(new ItemStack(Items.TURTLE_HELMET).is(Acoustics.PROTECTS_EARS),
                "the test pack has the turtle shell protect the ears");
        helper.assertFalse(ring.rings(), "and wearing it, a blast close by rings nothing; " + ring);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void impactOf_soundsByTheKindOfThingStruck(final GameTestHelper helper) {
        final Zombie zombie = helper.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 2));
        final SoundContext context = Acoustics.impactOf(zombie);
        zombie.discard();
        helper.assertTrue("minecraft:zombie".equals(context.get(SoundContext.ENTITY)),
                "a blow on a zombie is picked by its kind; " + context);
        helper.assertTrue(AcousticCues.IMPACT.space() == SoundSpace.WORLD,
                "and sounds from the place it strikes");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void soundTraits_sayWhatADeclarationSaid(final GameTestHelper helper) {
        final SoundTraits.Traits blast = SoundTraits.of(TestSounds.BLAST.id());
        helper.assertTrue(blast.matters(), "the blast matters");
        helper.assertTrue(blast.heardAt(60).equals(Optional.of(TestSounds.RUMBLE.id()))
                && blast.heardAt(20).isEmpty(), "and is heard from far away as its rumble; " + blast);
        helper.assertTrue(SoundTraits.of(TestSounds.HUM.id()).equals(SoundTraits.Traits.NONE),
                "a sound that said nothing behaves as any other");
        helper.succeed();
    }
}
