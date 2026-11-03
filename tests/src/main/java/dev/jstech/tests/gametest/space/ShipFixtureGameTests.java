/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.multiblock.Orientation;
import dev.jstech.core.multiblock.plan.IPlanMember;
import dev.jstech.core.multiblock.shape.FormedPlace;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.room.GasMath;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.SpaceSuit;
import dev.jstech.space.api.SpaceSuits;
import dev.jstech.space.life.LifeSupport;
import dev.jstech.space.life.LunarSuit;
import dev.jstech.space.ship.BunkBlock;
import dev.jstech.space.ship.IFixtureEntity;
import dev.jstech.space.ship.OxygenPanelBlock;
import dev.jstech.space.ship.ShipFixtureBlock;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.ship.SuitLockerEntity;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A starship's fixtures at work: the airlock's oxygen panel fills the tanks of a worn suit from the cabin's air, taking
 * its oxygen; the quarters' bunk is a respawn point in a cabin of breathable air and refuses one of nitrogen; the suit
 * locker keeps what is put in it and gives it back when broken; a formed part's fixture stops drawing itself and lets
 * the light through, and stands as its block of its part's model; and the workshop's vanilla blocks keep themselves
 * in a ship's plan.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipFixtureGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_ship_fixtures";
    private static final BlockPos LOW = new BlockPos(2, 1, 2);
    private static final BlockPos HIGH_CORNER = new BlockPos(6, 4, 6);
    private static final BlockPos INSIDE = new BlockPos(4, 3, 4);
    private static final BlockPos FIXTURE = new BlockPos(3, 2, 3);
    private static final double ATMOSPHERE = 101.3;

    private ShipFixtureGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void oxygenPanel_fillsAWornSuitFromTheCabinsAir(final GameTestHelper helper) {
        hut(helper);
        helper.setBlock(FIXTURE, ShipKitBlocks.OXYGEN_PANEL.get());
        SealedRooms.track(helper.getLevel(), helper.absolutePos(INSIDE));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper) != null, "the hut is a cabin"))
                .thenExecute(() -> {
                    final ServerLevel level = helper.getLevel();
                    final ServerPlayer crew = GameTestPlayers.join(level, UUID.randomUUID(), "crew");
                    final int bare = OxygenPanelBlock.fill(level, helper.absolutePos(FIXTURE), crew);
                    final SpaceSuit suit = SpaceSuits.declared().get(LunarSuit.ID);
                    final ItemStack chest = new ItemStack(LunarSuit.SUIT.get());
                    LifeSupport.setAir(level, chest, suit, 0);
                    crew.setItemSlot(EquipmentSlot.CHEST, chest);
                    final double before = room(helper).moles(AirMix.OXYGEN);
                    final int filled = OxygenPanelBlock.fill(level, helper.absolutePos(FIXTURE), crew);
                    final double taken = before - room(helper).moles(AirMix.OXYGEN);
                    final int air = LifeSupport.air(level, crew.getItemBySlot(EquipmentSlot.CHEST), suit);
                    GameTestPlayers.leave(crew);
                    SealedRooms.untrack(level, helper.absolutePos(INSIDE));
                    helper.assertTrue(bare == 0, "no suit worn, nothing is filled");
                    helper.assertTrue(filled > 0 && air == filled, "the worn suit's tanks are filled: " + filled
                            + " seconds, the tanks hold " + air);
                    helper.assertTrue(Math.abs(taken - filled * 0.01) < 0.02, "taking the oxygen out of the cabin: "
                            + taken + " mol");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 200)
    public static void bunk_isARespawnPointInACabinOfBreathableAirOnly(final GameTestHelper helper) {
        hut(helper);
        helper.setBlock(FIXTURE, ShipKitBlocks.BUNK_FRAME.get());
        SealedRooms.track(helper.getLevel(), helper.absolutePos(INSIDE));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(room(helper) != null, "the hut is a cabin"))
                .thenExecute(() -> {
                    final ServerLevel level = helper.getLevel();
                    final BlockPos bunk = helper.absolutePos(FIXTURE);
                    fill(room(helper), AirMix.NITROGEN, 0.79, AirMix.OXYGEN, 0.21);
                    final boolean airy = BunkBlock.breathable(level, bunk) && level.getBlockState(bunk)
                            .getRespawnPosition(EntityType.PLAYER, level, bunk, 0).isPresent();
                    fill(room(helper), AirMix.NITROGEN, 1.0, AirMix.OXYGEN, 0.0);
                    final boolean stale = BunkBlock.breathable(level, bunk) || level.getBlockState(bunk)
                            .getRespawnPosition(EntityType.PLAYER, level, bunk, 0).isPresent();
                    SealedRooms.untrack(level, helper.absolutePos(INSIDE));
                    helper.assertTrue(airy, "in a cabin of air the bunk is a respawn point, its sleeper waking beside "
                            + "it");
                    helper.assertTrue(!stale, "in a cabin of nitrogen it wakes no one");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void suitLocker_keepsWhatIsPutInItAndFormedFixturesHide(final GameTestHelper helper) {
        helper.setBlock(FIXTURE, ShipKitBlocks.SUIT_LOCKER.get());
        final SuitLockerEntity locker = (SuitLockerEntity) helper.getBlockEntity(FIXTURE);
        locker.setItem(0, new ItemStack(LunarSuit.HELMET.get()));
        helper.assertTrue(locker.getContainerSize() == 9 && locker.getItem(0).is(LunarSuit.HELMET.get()),
                "the locker keeps a helmet");
        final ShipFixtureBlock fixture = (ShipFixtureBlock) ShipKitBlocks.SUIT_LOCKER.get();
        fixture.partFormed(helper.getLevel(), helper.absolutePos(FIXTURE), true);
        helper.assertTrue(helper.getBlockState(FIXTURE).getValue(ShipFixtureBlock.FORMED)
                        && helper.getBlockState(FIXTURE).getOcclusionShape(helper.getLevel(),
                        helper.absolutePos(FIXTURE)).isEmpty(),
                "formed, the fixture is drawn by its part's model and lets the light through");
        helper.destroyBlock(FIXTURE);
        final boolean dropped = !helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                new AABB(helper.absolutePos(FIXTURE)).inflate(2), item -> item.getItem().is(LunarSuit.HELMET.get()))
                .isEmpty();
        helper.assertTrue(dropped, "broken, it gives the helmet back");
        helper.assertTrue(IPlanMember.keepsItself(Blocks.CRAFTING_TABLE.defaultBlockState())
                        && IPlanMember.keepsItself(Blocks.FURNACE.defaultBlockState())
                        && IPlanMember.keepsItself(Blocks.SMITHING_TABLE.defaultBlockState())
                        && IPlanMember.keepsItself(Blocks.BARREL.defaultBlockState())
                        && IPlanMember.keepsItself(ShipKitBlocks.BUNK_FRAME.get().defaultBlockState())
                        && !IPlanMember.keepsItself(ShipKitBlocks.HULL_FRAME.get().defaultBlockState()),
                "the workshop's vanilla blocks and the fixtures keep themselves in a plan; the hull merges");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void fixture_formedStandsAsItsBlockOfItsPartsModel(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(FIXTURE);
        for (final Block block : List.of(ShipKitBlocks.SUIT_LOCKER.get(), ShipKitBlocks.OXYGEN_PANEL.get())) {
            final FormedPlace place = placeOf(level, SmallShipParts.id("small_airlock"), block);
            helper.setBlock(FIXTURE, block);
            ((ShipFixtureBlock) block).partFormed(level, pos, true, place);
            final VoxelShape wanted = FormedShapes.get(level, place.model()).orElseThrow().shape(place);
            final VoxelShape stands = level.getBlockState(pos).getCollisionShape(level, pos);
            helper.assertTrue(level.getBlockEntity(pos) instanceof IFixtureEntity fixture
                    && fixture.places().equals(List.of(place)), "formed, " + block + " keeps its block of the model");
            helper.assertTrue(!Shapes.joinIsNotEmpty(wanted, stands, BooleanOp.NOT_SAME)
                    && Shapes.joinIsNotEmpty(stands, Shapes.block(), BooleanOp.NOT_SAME),
                    "and stands as the model draws it there, not as a whole block: " + stands.toAabbs());
            ((ShipFixtureBlock) block).partFormed(level, pos, false, place);
            helper.assertTrue(!Shapes.joinIsNotEmpty(level.getBlockState(pos).getCollisionShape(level, pos),
                    Shapes.block(), BooleanOp.NOT_SAME), "come apart, " + block + " is a whole block again");
        }
        helper.succeed();
    }

    /* The block of the formed model of {@code part}'s kit that {@code block} stands for there, standing north. */
    private static FormedPlace placeOf(final ServerLevel level, final ResourceLocation part, final Block block) {
        final ShipKit kit = ShipKits.all(level).get(part);
        final ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        for (final ShipKit.Piece piece : kit.pieces()) {
            for (int y = 0; y < piece.layers().size(); y++) {
                final List<String> layer = piece.layers().get(y);
                for (int z = 0; z < layer.size(); z++) {
                    final String row = layer.get(z);
                    for (int x = 0; x < row.length(); x++) {
                        if (id.equals(kit.key().get(String.valueOf(row.charAt(x))))) {
                            return new FormedPlace(ShipPartModels.shape(part, kit, piece),
                                    FormedShapes.ORIENTATIONS.indexOf(Orientation.NORTH), piece.x() + x,
                                    piece.y() + y, piece.z() + z);
                        }
                    }
                }
            }
        }
        throw new IllegalStateException(id + " is not in the kit of " + part);
    }

    /* A closed box of stone from LOW to HIGH_CORNER, its inside air. */
    private static void hut(final GameTestHelper helper) {
        for (int x = LOW.getX(); x <= HIGH_CORNER.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH_CORNER.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH_CORNER.getZ(); z++) {
                    final boolean shell = x == LOW.getX() || x == HIGH_CORNER.getX() || y == LOW.getY()
                            || y == HIGH_CORNER.getY() || z == LOW.getZ() || z == HIGH_CORNER.getZ();
                    helper.setBlock(new BlockPos(x, y, z), shell ? Blocks.STONE : Blocks.AIR);
                }
            }
        }
    }

    private static Room room(final GameTestHelper helper) {
        return SealedRooms.roomAt(helper.getLevel(), helper.absolutePos(INSIDE)).orElse(null);
    }

    /* The room's air made of nitrogen and oxygen in those shares, at the sea's pressure. */
    private static void fill(final Room room, final ResourceLocation first, final double firstShare,
                             final ResourceLocation second, final double secondShare) {
        room.empty();
        room.add(first, GasMath.moles(ATMOSPHERE * firstShare, room.gasVolume(), GasMath.TEMPERED));
        if (secondShare > 0) {
            room.add(second, GasMath.moles(ATMOSPHERE * secondShare, room.gasVolume(), GasMath.TEMPERED));
        }
    }
}
