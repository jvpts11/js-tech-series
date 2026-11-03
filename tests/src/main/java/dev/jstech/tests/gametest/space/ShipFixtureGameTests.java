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
import dev.jstech.space.ship.ShipBarrelEntity;
import dev.jstech.space.ship.ShipFixtureBlock;
import dev.jstech.space.ship.ShipFurnaceBlock;
import dev.jstech.space.ship.ShipFurnaceEntity;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipPartModels;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.space.ship.SuitLockerEntity;
import dev.jstech.testkit.gametest.GameTestPlayers;
import dev.jstech.tests.JsTests;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A starship's fixtures at work: the airlock's oxygen panel fills the tanks of a worn suit from the cabin's air, taking
 * its oxygen; the quarters' bunk is a respawn point in a cabin of breathable air and refuses one of nitrogen; the suit
 * locker keeps what is put in it and gives it back when broken; a formed part's fixture stops drawing itself and lets
 * the light through, and stands as its block of its part's model; the workshop's working pieces keep themselves in a
 * ship's plan, and work as the game's own blocks do: the crafting grid, the smithing slots, a furnace that smelts and
 * gives light as it burns, a barrel of 27 stacks.
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
        helper.assertTrue(IPlanMember.keepsItself(ShipKitBlocks.SHIP_CRAFTING_TABLE.get().defaultBlockState())
                        && IPlanMember.keepsItself(ShipKitBlocks.SHIP_FURNACE.get().defaultBlockState())
                        && IPlanMember.keepsItself(ShipKitBlocks.SHIP_SMITHING_TABLE.get().defaultBlockState())
                        && IPlanMember.keepsItself(ShipKitBlocks.SHIP_BARREL.get().defaultBlockState())
                        && IPlanMember.keepsItself(ShipKitBlocks.BUNK_FRAME.get().defaultBlockState())
                        && !IPlanMember.keepsItself(ShipKitBlocks.HULL_FRAME.get().defaultBlockState()),
                "the workshop's working pieces and the fixtures keep themselves in a plan; the hull merges");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH, timeoutTicks = 600)
    public static void workingPieces_workAsTheGamesOwnBlocksDo(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // Logged in over a connection, as a menu opened is sent to the player's game.
        final ServerPlayer crew = GameTestPlayers.connected(helper, "crew");
        final BlockPos table = new BlockPos(2, 2, 2);
        final BlockPos smithy = new BlockPos(4, 2, 2);
        final BlockPos furnace = new BlockPos(2, 2, 4);
        final BlockPos barrel = new BlockPos(4, 2, 4);
        helper.setBlock(table, ShipKitBlocks.SHIP_CRAFTING_TABLE.get());
        helper.setBlock(smithy, ShipKitBlocks.SHIP_SMITHING_TABLE.get());
        helper.setBlock(furnace, ShipKitBlocks.SHIP_FURNACE.get());
        helper.setBlock(barrel, ShipKitBlocks.SHIP_BARREL.get());
        crew.setPos(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(3, 2, 3))));
        helper.startSequence()
                .thenExecute(() -> {
                    use(helper, crew, table);
                    helper.assertTrue(crew.containerMenu instanceof CraftingMenu menu && menu.stillValid(crew),
                            "the crafting table opens the crafting grid, and keeps it open by it");
                    crew.closeContainer();
                    use(helper, crew, smithy);
                    helper.assertTrue(crew.containerMenu instanceof SmithingMenu menu && menu.stillValid(crew),
                            "the smithing table opens the smithing slots, and keeps them open by it");
                    helper.setBlock(smithy, Blocks.STONE);
                    helper.assertTrue(!crew.containerMenu.stillValid(crew), "and closes them once it is gone");
                    crew.closeContainer();
                    final ShipFurnaceEntity fire = (ShipFurnaceEntity) helper.getBlockEntity(furnace);
                    fire.setItem(0, new ItemStack(Items.RAW_IRON));
                    fire.setItem(1, new ItemStack(Items.COAL));
                })
                .thenWaitUntil(() -> helper.assertTrue(helper.getBlockState(furnace).getValue(ShipFurnaceBlock.LIT)
                        && helper.getBlockState(furnace).getLightEmission(level, helper.absolutePos(furnace)) > 0,
                        "the furnace burns, and gives light"))
                .thenWaitUntil(() -> helper.assertTrue(((ShipFurnaceEntity) helper.getBlockEntity(furnace))
                        .getItem(2).is(Items.IRON_INGOT), "the furnace smelts the raw iron"))
                .thenExecute(() -> {
                    final ShipBarrelEntity drum = (ShipBarrelEntity) helper.getBlockEntity(barrel);
                    helper.assertTrue(drum.getContainerSize() == 27, "the barrel keeps 27 stacks");
                    drum.setItem(26, new ItemStack(Items.DIAMOND));
                    use(helper, crew, barrel);
                    helper.assertTrue(crew.containerMenu instanceof ChestMenu menu && menu.stillValid(crew)
                            && menu.getContainer() == drum, "the barrel opens what it keeps");
                    crew.closeContainer();
                    helper.destroyBlock(barrel);
                    helper.assertTrue(!level.getEntitiesOfClass(ItemEntity.class,
                            new AABB(helper.absolutePos(barrel)).inflate(2), item -> item.getItem().is(Items.DIAMOND))
                            .isEmpty(), "broken, it gives back what it kept");
                    GameTestPlayers.leave(crew);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void fixture_formedStandsAsItsBlockOfItsPartsModel(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(FIXTURE);
        final Map<Block, String> parts = new LinkedHashMap<>();
        parts.put(ShipKitBlocks.SUIT_LOCKER.get(), "small_airlock");
        parts.put(ShipKitBlocks.OXYGEN_PANEL.get(), "small_airlock");
        parts.put(ShipKitBlocks.SHIP_CRAFTING_TABLE.get(), "small_workshop");
        parts.put(ShipKitBlocks.SHIP_FURNACE.get(), "small_workshop");
        parts.put(ShipKitBlocks.SHIP_BARREL.get(), "small_bay");
        for (final Block block : parts.keySet()) {
            final FormedPlace place = placeOf(level, SmallShipParts.id(parts.get(block)), block);
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

    /* {@code player} clicks the block at {@code at}, holding nothing, as a click on its top uses it. */
    private static void use(final GameTestHelper helper, final ServerPlayer player, final BlockPos at) {
        final BlockPos pos = helper.absolutePos(at);
        helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(), player,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
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
