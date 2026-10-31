/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructureEntity;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.moving.RailsMotion;
import dev.jstech.core.moving.SpinMotion;
import dev.jstech.core.moving.StructurePose;
import dev.jstech.core.moving.StructureFootingPayload;
import dev.jstech.core.moving.StructureResult;
import dev.jstech.core.moving.StructureSync;
import dev.jstech.core.moving.StructureTexts;
import dev.jstech.core.moving.StructureYard;
import dev.jstech.core.moving.TowardMotion;
import dev.jstech.core.moving.TurnAboutMotion;
import dev.jstech.core.moving.YardLayout;
import dev.jstech.core.multiblock.FormedPartBlockEntity;
import dev.jstech.core.multiblock.FormedParts;
import dev.jstech.core.protection.PlayerLimits;
import dev.jstech.core.protection.ProtectionBalance;
import dev.jstech.core.region.Box;
import dev.jstech.core.region.ChunkLoaders;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

/**
 * The Core's moving structures in a real world: blocks made into a structure whole and refused when they must stay
 * put, the yard empty and born of nothing, a zombie standing on a structure's floor and carried as it rises, an arrow
 * hitting its block, a chest on it staying open, a player standing on its deck staying on the server, the rules that
 * move it (toward a place, spinning, swinging on a hinge, running on rails), setting it down on the grid turned, its
 * save in the middle of a move, going to the Nether with what stands on it, the world's border, the rain and the
 * news of its blocks, and its chunks of the yard sent to who sees it. The arena's floor is its layer at y 1.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MovingStructureGameTests {

    private static final String ARENA = "empty";
    /* How close, in blocks, two places count as one. */
    private static final double CLOSE = 1.0e-3;
    /* How far a walker goes a tick through the doorway, and for how many ticks: from before it to past it. */
    private static final double DOORWAY_STRIDE = 0.15;
    private static final int DOORWAY_STRIDES = 20;
    private static final TextKey LOADER_ABOARD = TextKey.of("jstests.moving.loader_aboard",
            "A loader aboard keeps the world under it loaded");

    private MovingStructureGameTests() {
    }

    @GameTest(template = ARENA, batch = "jscore_moving_assembly")
    public static void assembly_movesTheBlocksWholeIntoTheYard(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos chestAt = new BlockPos(2, 3, 2);
        helper.setBlock(chestAt, Blocks.CHEST);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);
        final ChestBlockEntity chest = (ChestBlockEntity) level.getBlockEntity(helper.absolutePos(chestAt));
        helper.assertTrue(chest != null, "a chest was set down");
        chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(chestAt),
                helper.absolutePos(new BlockPos(3, 3, 2)));
        helper.assertTrue(made.ok() && made.blocks() == 2, "two blocks make a structure: " + refusal(made));
        final MovingStructure structure = made.structure();
        helper.assertBlockPresent(Blocks.AIR, chestAt);
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(3, 3, 2));
        final BlockPos inYard = find(level, structure, state -> state.is(Blocks.CHEST));
        helper.assertTrue(inYard != null && YardLayout.isYardBlock(inYard.getX()), "the chest is in the yard");
        final BlockEntity kept = level.getBlockEntity(inYard);
        helper.assertTrue(kept instanceof Container box && box.getItem(0).is(Items.DIAMOND)
                && box.getItem(0).getCount() == 7, "with what it held");
        final Vec3 there = MovingStructures.inWorld(level, Vec3.atCenterOf(inYard));
        helper.assertTrue(there.distanceTo(Vec3.atCenterOf(helper.absolutePos(chestAt))) < CLOSE,
                "and it stands where it stood: " + there);
        final AABB box = structure.worldBox();
        helper.assertTrue(Math.abs(box.getXsize() - 2) < CLOSE && Math.abs(box.getYsize() - 1) < CLOSE,
                "the structure fills the box its blocks filled: " + box);
        MovingStructures.takeAway(structure);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_assembly")
    public static void assemblyFor_countsAgainstThePlayersLimitUntilItGoes(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final UUID player = UUID.randomUUID();
        final int most = ProtectionBalance.structuresPerPlayer();
        ProtectionBalance.setStructuresPerPlayer(1);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(5, 3, 5), Blocks.STONE);
        warm(level);
        try {
            final StructureResult first = MovingStructures.assembleFor(level, helper.absolutePos(new BlockPos(2, 3, 2)),
                    helper.absolutePos(new BlockPos(2, 3, 2)), player);
            helper.assertTrue(first.ok() && first.structure().maker().orElse(null) == player,
                    "the first is made, and is theirs: " + refusal(first));
            helper.assertTrue(PlayerLimits.count(level.getServer(), player, PlayerLimits.MOVING_STRUCTURES) == 1,
                    "and counted against their limit");
            final StructureResult second = MovingStructures.assembleFor(level,
                    helper.absolutePos(new BlockPos(5, 3, 5)), helper.absolutePos(new BlockPos(5, 3, 5)), player);
            helper.assertTrue(!second.ok() && second.refusal() instanceof Text.Translated said
                    && said.key() == PlayerLimits.REACHED, "a second goes past their limit of one: "
                    + refusal(second));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(5, 3, 5));
            MovingStructures.takeAway(first.structure());
            helper.assertTrue(PlayerLimits.count(level.getServer(), player, PlayerLimits.MOVING_STRUCTURES) == 0,
                    "taken away, it is given back to their count");
            final StructureResult again = MovingStructures.assembleFor(level,
                    helper.absolutePos(new BlockPos(5, 3, 5)), helper.absolutePos(new BlockPos(5, 3, 5)), player);
            helper.assertTrue(again.ok(), "so another is made: " + refusal(again));
            MovingStructures.takeAway(again.structure());
            helper.setBlock(new BlockPos(1, 3, 1), Blocks.STONE);
            final StructureResult nobodys = MovingStructures.assemble(level,
                    helper.absolutePos(new BlockPos(1, 3, 1)), helper.absolutePos(new BlockPos(1, 3, 1)));
            helper.assertTrue(nobodys.ok() && nobodys.structure().maker().isEmpty(),
                    "one made for nobody is nobody's, and counts for nobody");
            MovingStructures.takeAway(nobodys.structure());
        } finally {
            ProtectionBalance.setStructuresPerPlayer(most);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_assembly", timeoutTicks = 200)
    public static void born_standsOfItsTemplateWithItsBlocksNeverInTheWorld(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        // A template of two blocks, read from the arena and the arena cleared: what a hangar holds of a rocket.
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.GLASS);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);
        final StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, helper.absolutePos(new BlockPos(2, 3, 2)), new Vec3i(2, 1, 1), false,
                Blocks.STRUCTURE_VOID);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.AIR);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.AIR);
        warm(level);
        final Vec3 at = helper.absoluteVec(new Vec3(5, 4, 5));
        final StructureResult made = MovingStructures.born(level, template, at);
        helper.assertTrue(made.ok() && made.blocks() == 2, "a template of two blocks is born a structure: "
                + refusal(made));
        final MovingStructure structure = made.structure();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(structure.ready() && !structure.arriving(),
                        "its plot has loaded and its blocks are laid in it"))
                .thenExecute(() -> {
                    helper.assertTrue(find(level, structure, state -> state.is(Blocks.GLASS)) != null
                            && find(level, structure, state -> state.is(Blocks.STONE)) != null,
                            "both its blocks are in its plot of the yard");
                    helper.assertTrue(structure.blocks() == 2, "and it is made of the two: " + structure.blocks());
                    helper.assertBlockPresent(Blocks.AIR, new BlockPos(5, 4, 5));
                    final AABB box = structure.worldBox();
                    helper.assertTrue(Math.abs(box.minY - at.y) < CLOSE
                            && Math.abs((box.minX + box.maxX) / 2 - at.x) < CLOSE,
                            "it stands with the middle of its bottom where it was born: " + box);
                    MovingStructures.takeAway(structure);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_assembly")
    public static void born_refusesATemplateOfNothingOrOfWhatStaysPut(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final StructureResult empty = MovingStructures.born(level, new StructureTemplate(),
                helper.absoluteVec(new Vec3(4, 4, 4)));
        helper.assertTrue(!empty.ok() && empty.refusal() instanceof Text.Translated said
                && said.key() == StructureTexts.NOTHING, "a template of nothing is born nothing: " + refusal(empty));
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.BEDROCK);
        final StructureTemplate bedrock = new StructureTemplate();
        bedrock.fillFromWorld(level, helper.absolutePos(new BlockPos(2, 3, 2)), new Vec3i(1, 1, 1), false,
                Blocks.STRUCTURE_VOID);
        final StructureResult kept = MovingStructures.born(level, bedrock, helper.absoluteVec(new Vec3(4, 4, 4)));
        helper.assertTrue(!kept.ok() && kept.refusal() instanceof Text.Translated said
                && said.key() == StructureTexts.IMMOVABLE, "and bedrock stays put: " + refusal(kept));
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_assembly")
    public static void assembly_refusesWhatMustStayPut(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.BEDROCK);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.STONE);
        final StructureResult refused = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(2, 3, 2)),
                helper.absolutePos(new BlockPos(3, 3, 2)));
        helper.assertTrue(!refused.ok() && refused.refusal() instanceof Text.Translated said
                && said.key() == StructureTexts.IMMOVABLE, "bedrock stays put: " + refusal(refused));
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(3, 3, 2));
        final StructureResult nothing = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(5, 3, 5)),
                helper.absolutePos(new BlockPos(6, 3, 6)));
        helper.assertTrue(!nothing.ok(), "air makes no structure");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_yard")
    public static void yard_isEmptyAirOverTheVoid(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final LevelChunk chunk = level.getChunk(YardLayout.FIRST_CHUNK_X + 9_000, 4_321);
        final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y += 7) {
            at.set(chunk.getPos().getMinBlockX() + 5, y, chunk.getPos().getMinBlockZ() + 9);
            helper.assertTrue(chunk.getBlockState(at).isAir(), "the yard holds no land at " + at.toShortString());
        }
        final Holder<Biome> biome = level.getBiome(at.set(chunk.getPos().getMinBlockX(), 64,
                chunk.getPos().getMinBlockZ()));
        helper.assertTrue(biome.is(Biomes.THE_VOID), "the yard is the void's: " + biome);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_yard")
    public static void yard_givesBirthToNothing(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final LevelChunk chunk = level.getChunk(YardLayout.FIRST_CHUNK_X + 9_100, -2_222);
        final int x0 = chunk.getPos().getMinBlockX();
        final int z0 = chunk.getPos().getMinBlockZ();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                level.setBlock(new BlockPos(x0 + x, 100, z0 + z), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x0 + x, 104, z0 + z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
        final AABB room = new AABB(x0, 101, z0, x0 + 16, 104, z0 + 16);
        final NaturalSpawner.SpawnState state = NaturalSpawner.createState(1, List.of(), (pos, consumer) -> { },
                new LocalMobCapCalculator(level.getChunkSource().chunkMap));
        for (int i = 0; i < 64; i++) {
            NaturalSpawner.spawnForChunk(level, chunk, state, true, true, true);
        }
        helper.assertTrue(level.getEntities((Entity) null, room, any -> true).isEmpty(),
                "nothing is born in a dark room of the yard");
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                level.setBlock(new BlockPos(x0 + x, 100, z0 + z), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x0 + x, 104, z0 + z), Blocks.AIR.defaultBlockState(), 3);
            }
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_standing", timeoutTicks = 200)
    public static void zombie_standsOnTheFloor(final GameTestHelper helper) {
        final MovingStructure floor = floor(helper);
        final Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(4.5, 6, 4.5));
        final double top = helper.absolutePos(new BlockPos(0, 4, 0)).getY();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(zombie.onGround() && Math.abs(zombie.getY() - top) < CLOSE,
                        "the zombie stands on the structure's floor at " + top + ": " + zombie.getY()))
                .thenExecute(() -> {
                    zombie.discard();
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_standing", timeoutTicks = 300)
    public static void structure_carriesWhatStandsOnIt(final GameTestHelper helper) {
        final MovingStructure floor = floor(helper);
        final Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new Vec3(4.5, 4.2, 4.5));
        final double top = helper.absolutePos(new BlockPos(0, 4, 0)).getY();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(zombie.onGround(), "the zombie stands on the floor"))
                .thenExecute(() -> floor.move(new TowardMotion(floor.pose().movedTo(floor.pose().x(),
                        floor.pose().y() + 2, floor.pose().z()), 4, 0)))
                .thenWaitUntil(() -> helper.assertTrue(floor.resting(), "the floor arrives"))
                .thenExecuteAfter(5, () -> helper.assertTrue(Math.abs(zombie.getY() - (top + 2)) < 0.01,
                        "the zombie rose with it: " + zombie.getY() + " for " + (top + 2)))
                .thenExecute(() -> {
                    zombie.discard();
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_standing", timeoutTicks = 200)
    public static void creeper_stepsOverASillUnderALowLintel(final GameTestHelper helper) {
        final MovingStructure doorway = doorway(helper);
        final Creeper creeper = helper.spawnWithNoFreeWill(EntityType.CREEPER, new Vec3(4.5, 4.0, 3.5));
        final double floor = helper.absolutePos(new BlockPos(0, 4, 0)).getY();
        final double through = helper.absolutePos(new BlockPos(0, 0, 6)).getZ();
        // Too tall to rise the whole step under the lintel, it rises only as high as the sill.
        helper.startSequence()
                .thenExecuteFor(DOORWAY_STRIDES, () -> creeper.move(MoverType.SELF, new Vec3(0, -0.08,
                        DOORWAY_STRIDE)))
                .thenExecute(() -> helper.assertTrue(creeper.getZ() > through && Math.abs(creeper.getY() - floor)
                        < 0.1, "the creeper steps over the doorway's sill and on: at " + creeper.position()))
                .thenExecute(() -> {
                    creeper.discard();
                    MovingStructures.takeAway(doorway);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_rays", timeoutTicks = 200)
    public static void arrow_hitsTheBlock(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure wall = wall(helper);
        final Vec3 from = Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 4, 4)));
        final BlockHitResult hit = level.clip(new ClipContext(from, from.add(8, 0, 0), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        helper.assertTrue(hit.getType() == HitResult.Type.BLOCK && YardLayout.isYardBlock(hit.getBlockPos().getX()),
                "a ray hits the structure's own block: " + hit.getBlockPos());
        final double face = helper.absolutePos(new BlockPos(6, 4, 4)).getX();
        helper.assertTrue(Math.abs(hit.getLocation().x - face) < CLOSE,
                "at its face in the world: " + hit.getLocation());
        final Arrow arrow = new Arrow(EntityType.ARROW, level);
        arrow.setPos(from);
        arrow.setDeltaMovement(1.5, 0, 0);
        level.addFreshEntity(arrow);
        final AtomicReference<Vec3> stuck = new AtomicReference<>();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(arrow.getX() > face - 1, "the arrow reaches the wall"))
                .thenExecuteAfter(5, () -> stuck.set(arrow.position()))
                .thenExecuteAfter(5, () -> helper.assertTrue(arrow.position().distanceTo(stuck.get()) < CLOSE
                        && arrow.getX() < face + CLOSE, "the arrow sticks in the structure's wall: "
                        + arrow.position() + " for the face at " + face))
                .thenExecute(() -> {
                    arrow.discard();
                    MovingStructures.takeAway(wall);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_players", timeoutTicks = 200)
    public static void chest_staysOpenFarFromThePlayer(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.CHEST);
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(4, 3, 4)),
                helper.absolutePos(new BlockPos(4, 3, 4)));
        helper.assertTrue(made.ok(), "the chest is a structure: " + refusal(made));
        final MovingStructure structure = made.structure();
        final BlockPos chestAt = find(level, structure, state -> state.is(Blocks.CHEST));
        final ServerPlayer player = player(helper, new Vec3(4.5, 2, 2.5), null);
        helper.assertTrue(player.canInteractWithBlock(chestAt, 1.0), "the player reaches the chest where it is");
        player.openMenu((ChestBlockEntity) level.getBlockEntity(chestAt));
        helper.startSequence()
                .thenExecuteAfter(20, () -> helper.assertTrue(player.containerMenu instanceof ChestMenu,
                        "the chest stays open 20 ticks on: " + player.containerMenu))
                .thenExecute(() -> {
                    player.closeContainer();
                    level.getServer().getPlayerList().remove(player);
                    MovingStructures.takeAway(structure);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_players", timeoutTicks = 400)
    public static void deck_keepsAPlayerOnIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure deck = floor(helper);
        final Vec3 standing = new Vec3(helper.absolutePos(new BlockPos(4, 4, 4)).getX() + 0.5,
                helper.absolutePos(new BlockPos(4, 4, 4)).getY(), helper.absolutePos(new BlockPos(4, 4, 4)).getZ()
                + 0.5);
        final ServerPlayer player = player(helper, standing.subtract(helper.absolutePos(BlockPos.ZERO).getX(),
                helper.absolutePos(BlockPos.ZERO).getY(), helper.absolutePos(BlockPos.ZERO).getZ()), null);
        final UUID id = player.getUUID();
        helper.onEachTick(() -> {
            if (player.connection != null && !player.isRemoved()) {
                player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(standing.x, standing.y,
                        standing.z, true));
                player.connection.tick();
            }
        });
        helper.startSequence()
                .thenExecuteAfter(200, () -> helper.assertTrue(level.getServer().getPlayerList().getPlayer(id) != null,
                        "a player standing 200 ticks on a deck is still on the server"))
                .thenExecute(() -> {
                    level.getServer().getPlayerList().remove(player);
                    MovingStructures.takeAway(deck);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_motion", timeoutTicks = 200)
    public static void motion_towardArrivesAndRests(final GameTestHelper helper) {
        final MovingStructure floor = floor(helper);
        final StructurePose start = floor.pose();
        floor.move(new TowardMotion(start.movedTo(start.x() + 3, start.y() + 1, start.z()), 4, 0));
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(floor.resting(), "it comes to rest"))
                .thenExecute(() -> {
                    helper.assertTrue(floor.pose().distanceTo(start.movedTo(start.x() + 3, start.y() + 1,
                            start.z())) < CLOSE, "where it was sent: " + floor.pose());
                    final double[] speed = floor.velocity();
                    helper.assertTrue(speed[0] == 0 && speed[1] == 0 && speed[2] == 0, "and at rest moves no more");
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_motion", timeoutTicks = 200)
    public static void motion_spinsAndSwingsOnItsHinge(final GameTestHelper helper) {
        final MovingStructure floor = floor(helper);
        final StructurePose start = floor.pose();
        floor.move(new SpinMotion(0, 1, 0, 90));
        final AtomicReference<Double> spun = new AtomicReference<>();
        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    floor.move(null);
                    spun.set(Math.toDegrees(floor.pose().angleTo(start)));
                    helper.assertTrue(Math.abs(spun.get() - 90) < 6, "a second at 90 a second turns it 90: "
                            + spun.get());
                    final AABB box = floor.worldBox();
                    floor.teleport(start);
                    floor.move(new TurnAboutMotion(box.minX, box.minY, (box.minZ + box.maxZ) / 2, 0, 0, 1, 90, 180));
                })
                .thenWaitUntil(() -> helper.assertTrue(floor.resting(), "the swing ends"))
                .thenExecute(() -> {
                    final AABB box = floor.worldBox();
                    helper.assertTrue(Math.abs(box.getYsize() - 3) < 0.01 && Math.abs(box.getXsize() - 1) < 0.01,
                            "the floor stands up on its hinge: " + box);
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_rails", timeoutTicks = 400)
    public static void motion_runsOnTheRails(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        for (int z = 1; z <= 7; z++) {
            helper.setBlock(new BlockPos(4, 2, z), Blocks.STONE);
            helper.setBlock(new BlockPos(4, 3, z), Blocks.RAIL);
        }
        helper.setBlock(new BlockPos(4, 5, 1), Blocks.STONE);
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(4, 5, 1)),
                helper.absolutePos(new BlockPos(4, 5, 1)));
        helper.assertTrue(made.ok(), "a car is made: " + refusal(made));
        final MovingStructure car = made.structure();
        final RailsMotion rails = RailsMotion.onRails(level, car.pose(), Direction.SOUTH, 6);
        helper.assertTrue(rails != null, "the car finds the rails under it");
        car.move(rails);
        final double end = helper.absolutePos(new BlockPos(4, 3, 8)).getZ();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(car.resting(), "the car runs to the end of the track"))
                .thenExecute(() -> {
                    helper.assertTrue(Math.abs(car.pose().z() - end) < 0.01, "and stops at its end: "
                            + car.pose().z() + " for " + end);
                    helper.assertTrue(Math.abs(car.pose().x() - (helper.absolutePos(new BlockPos(4, 3, 4)).getX()
                            + 0.5)) < 0.01, "over the track all the way");
                    MovingStructures.takeAway(car);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_setdown")
    public static void setDown_alignsToTheGridAndTurnsTheBlocks(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(3, 3, 3), Blocks.OAK_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, Direction.NORTH));
        helper.setBlock(new BlockPos(4, 3, 3), Blocks.STONE);
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(3, 3, 3)),
                helper.absolutePos(new BlockPos(4, 3, 3)));
        helper.assertTrue(made.ok(), "two blocks make a structure: " + refusal(made));
        final MovingStructure structure = made.structure();
        final StructurePose at = structure.pose();
        structure.teleport(StructurePose.turned(at.x() + 0.2, at.y(), at.z() - 0.3, 0, 1, 0, Math.toRadians(88)));
        final StructureResult down = MovingStructures.setDown(structure);
        helper.assertTrue(down.ok() && down.blocks() == 2, "it is set down: " + refusal(down));
        BlockPos stairs = null;
        for (final BlockPos each : BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 2, 0)),
                helper.absolutePos(new BlockPos(8, 5, 8)))) {
            if (level.getBlockState(each).is(Blocks.OAK_STAIRS)) {
                stairs = each.immutable();
            }
        }
        helper.assertTrue(stairs != null, "the stairs are on the ground again");
        helper.assertTrue(level.getBlockState(stairs).getValue(StairBlock.FACING) == Direction.WEST,
                "turned a quarter as the structure was: " + level.getBlockState(stairs));
        helper.assertTrue(level.getBlockState(stairs.north()).is(Blocks.STONE),
                "and the stone beside them turned with them");
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_setdown")
    public static void setDown_refusesWhatIsInTheWay(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.STONE);
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, helper.absolutePos(new BlockPos(4, 3, 4)),
                helper.absolutePos(new BlockPos(4, 3, 4)));
        helper.assertTrue(made.ok(), "a structure is made: " + refusal(made));
        helper.setBlock(new BlockPos(4, 3, 4), Blocks.GOLD_BLOCK);
        final StructureResult down = MovingStructures.setDown(made.structure());
        helper.assertTrue(!down.ok() && down.refusal() instanceof Text.Translated said
                && said.key() == StructureTexts.BLOCKED, "the gold is in the way: " + refusal(down));
        helper.assertBlockPresent(Blocks.GOLD_BLOCK, new BlockPos(4, 3, 4));
        MovingStructures.takeAway(made.structure());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_saves", timeoutTicks = 200)
    public static void save_keepsTheStructureInTheMiddleOfAMove(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure floor = floor(helper);
        final StructurePose far = floor.pose().movedTo(floor.pose().x(), floor.pose().y() + 40, floor.pose().z());
        floor.move(new TowardMotion(far, 2, 0));
        helper.startSequence()
                .thenExecuteAfter(10, () -> {
                    final CompoundTag saved = new CompoundTag();
                    floor.entity().saveWithoutId(saved);
                    final MovingStructureEntity copy = MovingStructures.ENTITY.get().create(level);
                    copy.load(saved);
                    final MovingStructure loaded = copy.structure();
                    helper.assertTrue(loaded != null && loaded.id().equals(floor.id())
                            && loaded.plot() == floor.plot() && loaded.pose().distanceTo(floor.pose()) < CLOSE,
                            "the save keeps the structure as it stands");
                    helper.assertTrue(loaded.motion() instanceof TowardMotion toward
                            && toward.target().distanceTo(far) < CLOSE, "and the rule moving it, mid-way");
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    /*
     * The land it arrives at is made by the game's own generation, which takes real seconds while the test server
     * runs thousands of ticks a second: the test loads that land itself first, as its own doing, and the structure
     * then waits only for the chunks' entities and ticking (a structure made where its plot is not loaded waits for
     * it, as yard_madeWhereThePlotIsNotLoadedWaitsForIt shows).
     */
    @GameTest(template = ARENA, batch = "jscore_moving_travel", timeoutTicks = 6000)
    public static void travel_takesEveryoneAboardToTheNether(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final ServerLevel nether = level.getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "there is a Nether");
        final MovingStructure floor = floor(helper);
        // A pig, which the sun does not burn while the structure waits for the land it goes to; nor anything else.
        final Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(4.5, 4.2, 4.5));
        pig.setInvulnerable(true);
        final Vec3 there = new Vec3(helper.absolutePos(BlockPos.ZERO).getX() + 0.5, 180,
                helper.absolutePos(BlockPos.ZERO).getZ() + 0.5);
        final int landX = Mth.floor(there.x) >> 4;
        final int landZ = Mth.floor(there.z) >> 4;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                nether.getChunk(landX + dx, landZ + dz);
            }
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(pig.onGround(), "the pig stands on the floor"))
                .thenExecute(() -> {
                    final StructureResult sent = MovingStructures.travel(floor, nether, there);
                    helper.assertTrue(sent.ok() && floor.departing(), "the structure is sent to the Nether: "
                            + refusal(sent));
                    helper.assertTrue(!MovingStructures.travel(floor, nether, there).ok(),
                            "and is not sent twice");
                })
                .thenWaitUntil(() -> {
                    final MovingStructure arrived = MovingStructures.byId(nether, floor.id());
                    helper.assertTrue(arrived != null && arrived.ready() && floor.entity().isRemoved(),
                            "the structure arrives in the Nether as itself, once the chunks it goes to have loaded;"
                                    + " still departing " + floor.departing() + ", left " + floor.entity().isRemoved()
                                    + ", there " + (arrived == null ? "none" : "ready " + arrived.ready())
                                    + "; " + netherChunks(nether, floor, there));
                })
                .thenExecute(() -> {
                    final MovingStructure arrived = MovingStructures.byId(nether, floor.id());
                    helper.assertTrue(arrived.blocks() == 9 && arrived.pose().distanceTo(StructurePose.at(there.x,
                            there.y, there.z)) < 1, "whole, where it was sent: " + arrived.pose());
                    final List<Pig> aboard = nether.getEntitiesOfClass(Pig.class, new AABB(there, there).inflate(4));
                    helper.assertTrue(!aboard.isEmpty(), "with the pig that stood on it; the pig left behind "
                            + pig.getRemovalReason() + " at " + pig.position() + ", the Nether's pigs "
                            + StreamSupport.stream(nether.getAllEntities().spliterator(), false)
                            .filter(Pig.class::isInstance).map(Entity::position).toList() + " for " + there);
                    for (final Pig one : aboard) {
                        one.discard();
                    }
                    MovingStructures.takeAway(arrived);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_world")
    public static void spawns_landWhereTheStructureIs(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure floor = floor(helper);
        final BlockPos block = find(level, floor, state -> !state.isAir());
        final ItemEntity item = new ItemEntity(level, block.getX() + 0.5, block.getY() + 1.2, block.getZ() + 0.5,
                new ItemStack(Items.APPLE));
        level.addFreshEntity(item);
        final Vec3 expected = MovingStructures.inWorld(level, new Vec3(block.getX() + 0.5, block.getY() + 1.2,
                block.getZ() + 0.5));
        helper.assertTrue(!YardLayout.isYard(item.getX()) && item.position().distanceTo(expected) < CLOSE,
                "an item dropped in the yard lands where its block is in the world: " + item.position());
        item.discard();
        MovingStructures.takeAway(floor);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_world")
    public static void structure_fillsThePlacesItsBlocksAreIn(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure floor = floor(helper);
        helper.assertTrue(!level.noCollision(new AABB(helper.absolutePos(new BlockPos(4, 3, 4))).deflate(0.1)),
                "a place a structure's block fills is not free");
        helper.assertTrue(level.noCollision(new AABB(helper.absolutePos(new BlockPos(4, 4, 4))).deflate(0.1)),
                "and the place above it is");
        MovingStructures.takeAway(floor);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_world")
    public static void border_holdsTheYard(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final double size = level.getWorldBorder().getSize();
        try {
            level.getWorldBorder().setSize(100);
            helper.assertTrue(level.getWorldBorder().isWithinBounds(new BlockPos(YardLayout.FIRST_BLOCK_X + 40, 64,
                    1_000)), "the yard is inside the smallest border");
            helper.assertTrue(!level.getWorldBorder().isWithinBounds(new BlockPos(10_000, 64, 0)),
                    "and the world outside it is not");
        } finally {
            level.getWorldBorder().setSize(size);
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_world")
    public static void rain_fallsWhereTheStructureIs(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure floor = floor(helper);
        // Up in the open sky, over the arena and whatever roofs it.
        floor.teleport(floor.pose().movedTo(floor.pose().x(), level.getMaxBuildHeight() - 20, floor.pose().z()));
        final BlockPos block = find(level, floor, state -> !state.isAir());
        final BlockPos above = block.above();
        final BlockPos worldAbove = MovingStructures.inWorld(level, above);
        // The test world is of a biome that never rains; the column over the structure is made plains for the test.
        final LevelChunk ground = level.getChunkAt(worldAbove);
        final Holder<Biome> was = level.getBiome(worldAbove);
        final Holder<Biome> plains = level.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(Biomes.PLAINS);
        ground.fillBiomesFromNoise((x, y, z, sampler) -> plains, level.getChunkSource().randomState().sampler());
        try {
            level.setWeatherParameters(0, 6_000, true, false);
            level.setRainLevel(1);
            helper.assertTrue(level.isRainingAt(above) == level.isRainingAt(worldAbove) && level.isRainingAt(above),
                    "it rains on the structure where it rains on its place in the world: " + level.isRainingAt(above)
                            + ", " + level.isRainingAt(worldAbove) + " at " + worldAbove.toShortString());
            level.setWeatherParameters(6_000, 0, false, false);
            level.setRainLevel(0);
            helper.assertTrue(!level.isRainingAt(above), "and not when it is dry");
        } finally {
            level.setWeatherParameters(6_000, 0, false, false);
            level.setRainLevel(0);
            ground.fillBiomesFromNoise((x, y, z, sampler) -> was, level.getChunkSource().randomState().sampler());
        }
        MovingStructures.takeAway(floor);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_players", timeoutTicks = 300)
    public static void news_ofTheStructuresBlocksReachThePlayersNearIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure floor = floor(helper);
        final EmbeddedChannel[] channel = new EmbeddedChannel[1];
        final ServerPlayer player = player(helper, new Vec3(4.5, 4, 1.5), channel);
        final BlockPos block = find(level, floor, state -> !state.isAir());
        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    final int before = channel[0].outboundMessages().size();
                    helper.assertTrue(player.connection.getConnection().isConnected(), "the player is connected");
                    channel[0].outboundMessages().clear();
                    level.playSound(null, block, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1, 1);
                    // The server sends a tick's packets out at its end; they are let out here, to be read.
                    channel[0].flushOutbound();
                    helper.assertTrue(channel[0].outboundMessages().stream()
                            .anyMatch(sent -> sent instanceof ClientboundSoundPacket),
                            "a sound of the structure's block is heard by the player beside it; sent "
                                    + channel[0].outboundMessages().stream().map(sent -> sent.getClass()
                                    .getSimpleName()).toList() + " to the player at " + player.position() + " after "
                                    + before + " before");
                    final ChunkPos yardChunk = new ChunkPos(block);
                    helper.assertTrue(level.getChunkSource().chunkMap.getPlayers(yardChunk, false).contains(player),
                            "who sees the structure follows the chunk of the yard its blocks are in");
                })
                .thenExecute(() -> {
                    level.getServer().getPlayerList().remove(player);
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_players", timeoutTicks = 300)
    public static void chunks_ofTheYardAreSentToWhoSeesTheStructure(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure floor = floor(helper);
        final EmbeddedChannel[] channel = new EmbeddedChannel[1];
        final ServerPlayer player = player(helper, new Vec3(4.5, 4, 1.5), channel);
        final ChunkPos yardChunk = new ChunkPos(find(level, floor, state -> !state.isAir()));
        final List<ChunkPos> sent = new ArrayList<>();
        helper.startSequence()
                .thenWaitUntil(() -> {
                    channel[0].flushOutbound();
                    Object message;
                    while ((message = channel[0].readOutbound()) != null) {
                        chunksIn(message, sent);
                    }
                    helper.assertTrue(sent.contains(yardChunk), "the structure's chunk of the yard " + yardChunk
                            + " is sent to the player who sees it; seen by " + StructureSync.watchers(floor).size()
                            + ", sent " + sent.size() + " chunks, the yard's " + sent.stream()
                            .filter(chunk -> YardLayout.isYardChunk(chunk.x)).toList());
                })
                .thenExecute(() -> {
                    level.getServer().getPlayerList().remove(player);
                    MovingStructures.takeAway(floor);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_far", timeoutTicks = 400)
    public static void structures_areSeenAsFarAsTheyReach(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final int view = level.getServer().getPlayerList().getViewDistance();
        final int length = 64;
        final List<BlockPos> line = new ArrayList<>();
        for (int x = 0; x < length; x++) {
            final BlockPos at = helper.absolutePos(new BlockPos(1, 3, 4)).offset(x, 0, 0);
            level.setBlock(at, Blocks.STONE.defaultBlockState(), 3);
            line.add(at);
        }
        // Lifting reads the land a block round the beam, which reaches past the chunks its stone was set in when the
        // test stands at a chunk's edge.
        for (final BlockPos round : BlockPos.betweenClosed(line.getFirst().offset(-1, 0, -1),
                line.getLast().offset(1, 0, 1))) {
            level.getChunkAt(round);
        }
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, line.getFirst(), line.getLast());
        helper.assertTrue(made.ok(), "a long beam is made: " + refusal(made));
        final MovingStructure beam = made.structure();
        final double away = view * 16 + length / 4.0;
        final ServerPlayer player = player(helper, new Vec3(1 + length / 2.0 - away, 4, 4.5), null);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getChunkSource().chunkMap
                        .getPlayersWatching(beam.entity()).contains(player),
                        "a player beyond the middle's reach but within the beam's sees it"))
                .thenExecute(() -> {
                    level.getServer().getPlayerList().remove(player);
                    MovingStructures.takeAway(beam);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_building")
    public static void building_ontoItGrowsItsBox(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure floor = floor(helper);
        final Box before = floor.bounds();
        final int blocks = floor.blocks();
        final BlockPos onTop = new BlockPos(before.minX(), before.maxY() + 1, before.minZ());
        level.setBlock(onTop, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        helper.assertTrue(floor.bounds().maxY() == before.maxY() + 1 && floor.blocks() == blocks + 1,
                "a block set on the structure is part of it: " + floor.bounds() + ", " + floor.blocks());
        helper.assertTrue(Math.abs(floor.worldBox().getYsize() - 2) < CLOSE, "and it fills the world's box higher");
        MovingStructures.takeAway(floor);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_building")
    public static void fields_followTheBlocksTheyHoldThePlacesOf(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final BlockPos part = helper.absolutePos(new BlockPos(3, 3, 3));
        final BlockPos terminal = helper.absolutePos(new BlockPos(4, 3, 3));
        level.setBlock(part, Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(terminal, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
        FormedParts.merge(level, terminal, List.of(part.asLong()));
        warm(level);
        final StructureResult made = MovingStructures.assemble(level, part, terminal);
        helper.assertTrue(made.ok(), "the two are made a structure: " + refusal(made));
        final BlockPos gold = find(level, made.structure(), state -> state.is(Blocks.GOLD_BLOCK));
        final BlockPos merged = find(level, made.structure(), state -> state.is(FormedParts.BLOCK.get()));
        helper.assertTrue(merged != null && level.getBlockEntity(merged) instanceof FormedPartBlockEntity formed
                && gold.equals(formed.terminal()), "the part's terminal is the gold block where it went: "
                + gold + " for " + (merged == null ? null : level.getBlockEntity(merged)));
        MovingStructures.takeAway(made.structure());
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_keeping", timeoutTicks = 200)
    public static void keeping_aLoaderAboardHoldsTheWorldUnderIt(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure floor = floor(helper);
        final BlockPos aboard = find(level, floor, state -> !state.isAir());
        ChunkLoaders.load(level, aboard, UUID.randomUUID(), LOADER_ABOARD, new ChunkPos(aboard), true);
        helper.startSequence()
                .thenExecuteAfter(25, () -> helper.assertTrue(floor.keepsItsPlace(),
                        "with a loader aboard the structure keeps the world under it loaded"))
                .thenExecute(() -> ChunkLoaders.releaseAll(level, aboard))
                .thenExecuteAfter(25, () -> helper.assertTrue(!floor.keepsItsPlace(), "and lets go without one"))
                .thenExecute(() -> MovingStructures.takeAway(floor))
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_deck", timeoutTicks = 400)
    public static void deck_carriesAPlayerWhoSaysWhereTheyStand(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure deck = floor(helper);
        final ServerPlayer player = player(helper, new Vec3(4.5, 4, 4.5), null);
        final Vec3 stood = player.position();
        final double[] place = {deck.frame().yardX(stood.x, stood.y, stood.z),
                deck.frame().yardY(stood.x, stood.y, stood.z), deck.frame().yardZ(stood.x, stood.y, stood.z)};
        final double top = stood.y;
        deck.move(new TowardMotion(deck.pose().movedTo(deck.pose().x(), deck.pose().y() + 3, deck.pose().z()), 2,
                0));
        helper.onEachTick(() -> {
            if (player.connection != null && !player.isRemoved()) {
                player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(
                        new StructureFootingPayload(deck.entity().getId(), place, false)));
                // The player's game says the place it stood at: the server puts it on the deck as it is now.
                player.connection.handleMovePlayer(new ServerboundMovePlayerPacket.Pos(stood.x, stood.y, stood.z,
                        true));
                player.connection.tick();
            }
        });
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(deck.resting(), "the deck arrives"))
                .thenExecuteAfter(5, () -> helper.assertTrue(Math.abs(player.getY() - (top + 3)) < 0.01,
                        "the player rose with the deck: " + player.getY() + " for " + (top + 3)))
                .thenExecute(() -> {
                    level.getServer().getPlayerList().remove(player);
                    MovingStructures.takeAway(deck);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, batch = "jscore_moving_again", timeoutTicks = 1200)
    public static void yard_plotsAreTakenAgainAfterTheirChunksUnload(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final MovingStructure first = floor(helper);
        final int plot = first.plot();
        final ChunkPos chunk = new ChunkPos(find(level, first, state -> !state.isAir()));
        MovingStructures.takeAway(first);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null,
                        "the plot's chunk unloads once no structure keeps it"))
                .thenExecute(() -> {
                    final MovingStructure again = floor(helper);
                    helper.assertTrue(again.plot() == plot, "the plot given back is taken again: " + again.plot());
                    MovingStructures.takeAway(again);
                })
                .thenWaitUntil(() -> helper.assertTrue(level.getChunkSource().getChunkNow(chunk.x, chunk.z) == null,
                        "and its chunk, loaded from its save, unloads again"))
                .thenExecuteAfter(20, () -> helper.assertTrue(level.getServer().getTickCount() > 0,
                        "the server goes on ticking"))
                .thenSucceed();
    }

    /*
     * Loads the chunks round the middle of the plot the next structure gets, as the test's own doing, so a small
     * structure made next has its blocks come into the plot at once; one made into a plot not loaded waits for it,
     * as yard_madeWhereThePlotIsNotLoadedWaitsForIt shows. The middle is on a chunk's edge, and the chunks round a
     * structure's are waited for too: two each way.
     */
    private static void warm(final ServerLevel level) {
        final int plot = StructureYard.nextPlot(level);
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }

    /* A floor of stone three blocks a side at y 3, made a structure. */
    private static MovingStructure floor(final GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(),
                helper.absolutePos(new BlockPos(3, 3, 3)), helper.absolutePos(new BlockPos(5, 3, 5)));
        helper.assertTrue(made.ok() && made.structure() != null, "a floor is made: " + refusal(made));
        return made.structure();
    }

    /*
     * A floor of stone three wide and five long, a doorway across its middle at z 5: a pillar either side, a lintel
     * over the way whose underside is two blocks over the floor, and a carpet for its sill; made a structure.
     */
    private static MovingStructure doorway(final GameTestHelper helper) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 7; z++) {
                helper.setBlock(new BlockPos(x, 3, z), Blocks.STONE);
            }
        }
        for (int y = 4; y <= 6; y++) {
            helper.setBlock(new BlockPos(3, y, 5), Blocks.STONE);
            helper.setBlock(new BlockPos(5, y, 5), Blocks.STONE);
        }
        helper.setBlock(new BlockPos(4, 6, 5), Blocks.STONE);
        helper.setBlock(new BlockPos(4, 4, 5), Blocks.WHITE_CARPET);
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(),
                helper.absolutePos(new BlockPos(3, 3, 3)), helper.absolutePos(new BlockPos(5, 6, 7)));
        helper.assertTrue(made.ok() && made.structure() != null, "a doorway is made: " + refusal(made));
        return made.structure();
    }

    /* A wall of stone one block thick at x 6, three high and three wide, made a structure. */
    private static MovingStructure wall(final GameTestHelper helper) {
        for (int y = 3; y <= 5; y++) {
            for (int z = 3; z <= 5; z++) {
                helper.setBlock(new BlockPos(6, y, z), Blocks.STONE);
            }
        }
        warm(helper.getLevel());
        final StructureResult made = MovingStructures.assemble(helper.getLevel(),
                helper.absolutePos(new BlockPos(6, 3, 3)), helper.absolutePos(new BlockPos(6, 5, 5)));
        helper.assertTrue(made.ok() && made.structure() != null, "a wall is made: " + refusal(made));
        return made.structure();
    }

    /* The first block of the structure's yard that is so, or null. */
    private static BlockPos find(final ServerLevel level, final MovingStructure structure,
                                 final Predicate<BlockState> wanted) {
        final Box box = structure.bounds();
        for (final BlockPos at : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(),
                box.maxZ())) {
            if (wanted.test(level.getBlockState(at))) {
                return at.immutable();
            }
        }
        return null;
    }

    /*
     * A player of the server who plays in survival, at the arena's place {@code at}, with a connection that keeps
     * what is sent to it in {@code channel}'s first slot when one is given.
     */
    private static ServerPlayer player(final GameTestHelper helper, final Vec3 at, final EmbeddedChannel[] channel) {
        final ServerLevel level = helper.getLevel();
        final CommonListenerCookie cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(),
                "deck-walker"), false);
        final ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(),
                cookie.clientInformation()) {
            @Override
            public boolean isSpectator() {
                return false;
            }

            @Override
            public boolean isCreative() {
                return false;
            }
        };
        final Connection connection = new Connection(PacketFlow.SERVERBOUND);
        final EmbeddedChannel kept = new EmbeddedChannel(connection);
        // A player's game with every mod's channels, as the Core's payloads are sent to every player.
        NetworkRegistry.configureMockConnection(connection);
        if (channel != null) {
            channel[0] = kept;
        }
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        for (int id = 0; id <= 8; id++) {
            player.connection.handleAcceptTeleportPacket(new ServerboundAcceptTeleportationPacket(id));
        }
        final Vec3 there = helper.absoluteVec(at);
        player.moveTo(there.x, there.y, there.z, 0, 0);
        // The game works out what a player sees as they move; this one was put there.
        level.getChunkSource().move(player);
        // A player's game says each batch of chunks arrived before the server sends the next; this one says so too.
        helper.onEachTick(() -> {
            if (player.connection != null && !player.isRemoved()) {
                player.connection.handleChunkBatchReceived(new ServerboundChunkBatchReceivedPacket(64));
            }
        });
        return player;
    }

    /* The chunks a packet sent, a bundle of packets looked into: the game sends a chunk bundled with its light. */
    private static void chunksIn(final Object message, final List<ChunkPos> into) {
        if (message instanceof ClientboundLevelChunkWithLightPacket chunk) {
            into.add(new ChunkPos(chunk.getX(), chunk.getZ()));
        } else if (message instanceof ClientboundBundlePacket bundle) {
            for (final Packet<?> inside : bundle.subPackets()) {
                chunksIn(inside, into);
            }
        }
    }

    /* Which chunks a structure on its way to the Nether waits for are loaded there, for a failure to say. */
    private static String netherChunks(final ServerLevel nether, final MovingStructure structure, final Vec3 there) {
        int plot = -1;
        for (int each = 0; each < 64 && plot < 0; each++) {
            if (StructureYard.owner(nether, each).map(structure.id()::equals).orElse(false)) {
                plot = each;
            }
        }
        if (plot < 0) {
            return "no plot of the Nether's yard is the structure's";
        }
        final int cx = YardLayout.centreX(plot) >> 4;
        final int cz = YardLayout.centreZ(plot) >> 4;
        final StringBuilder loaded = new StringBuilder("plot " + plot + " loaded round its middle:");
        for (int dx = -2; dx <= 1; dx++) {
            loaded.append(' ').append(nether.getChunkSource().getChunkNow(cx + dx, cz) != null ? '1' : '0');
        }
        return loaded + "; the land it arrives at " + (nether.getChunkSource().getChunkNow(
                (int) Math.floor(there.x) >> 4, (int) Math.floor(there.z) >> 4) != null);
    }

    private static String refusal(final StructureResult result) {
        return result.refusal() == null ? "done" : result.refusal().english();
    }
}
