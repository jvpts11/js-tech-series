/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.mojang.authlib.GameProfile;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.multiblock.ShapeMatch;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestStructures;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Multiblocks that form in the world: a tank the player builds block by block forms once it stands whole, runs a
 * recipe at once for each valve, comes apart when a block of it is broken and forms again when the block is back,
 * names the wrong place when one is, keeps its blocks from whoever its owner does not let break them, and keeps
 * whether it is formed in its save.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class StructureGameTests {

    private static final String ARENA = "empty";
    /* The tank from one corner to the other, its controller in the middle of its north face. */
    private static final BlockPos LOW = new BlockPos(1, 1, 1);
    private static final BlockPos HIGH = new BlockPos(3, 3, 3);
    private static final BlockPos CONTROLLER = new BlockPos(2, 2, 1);
    private static final BlockPos CORNER = new BlockPos(1, 1, 1);

    private StructureGameTests() {
    }

    @GameTest(template = ARENA)
    public static void tank_formsWhenBuiltWholeAndRunsAsManyAsItsValves(final GameTestHelper helper) {
        build(helper);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.HOPPER);
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.HOPPER);
        helper.succeedWhen(() -> {
            final TestStructures.TankBlockEntity tank = tank(helper);
            final MachineStructure structure = tank.structure();
            helper.assertTrue(structure != null && structure.formed(), "the tank forms once it stands whole");
            helper.assertTrue(structure.parallel() == 2, "it runs a recipe at once for each valve; got "
                    + structure.parallel());
            helper.assertTrue(structure.size()[0] == 3 && structure.size()[1] == 3 && structure.size()[2] == 3,
                    "it knows its size");
            helper.assertTrue(tank.status() == MachineStatus.WORKING, "and works; got " + tank.status());
            final List<Text> lines = new ArrayList<>();
            tank.describe(lines, true);
            helper.assertTrue(lines.stream().anyMatch(line -> line.english().equals("Formed: 3 by 3 by 3"))
                    && lines.stream().anyMatch(line -> line.english().equals("Runs 2 at once")),
                    "whoever looks at it is told it is formed, how large and how many at once; got " + lines);
        });
    }

    @GameTest(template = ARENA)
    public static void tank_comesApartWhenABlockGoesAndFormsAgainWhenItIsBack(final GameTestHelper helper) {
        build(helper);
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(formed(helper), "the tank forms first"))
                .thenExecute(() -> helper.setBlock(CORNER, Blocks.AIR))
                .thenWaitUntil(() -> {
                    final TestStructures.TankBlockEntity tank = tank(helper);
                    helper.assertTrue(!formed(helper), "a corner gone undoes it");
                    helper.assertTrue(tank.status() == MachineStatus.NOT_FORMED, "it stands not formed; got "
                            + tank.status());
                    final ShapeMatch.Failure failure = tank.structure().failure().orElseThrow();
                    helper.assertTrue(failure.wanted().equals(ShapeMatch.FRAME) && failure.found()
                            .equals("minecraft:air"), "it names the frame that is missing; got " + failure);
                })
                .thenExecute(() -> helper.setBlock(CORNER, Blocks.IRON_BLOCK))
                .thenWaitUntil(() -> helper.assertTrue(formed(helper), "the corner back forms it again"))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void tank_namesTheWrongPlaceAndNeverForms(final GameTestHelper helper) {
        build(helper);
        helper.setBlock(new BlockPos(3, 3, 2), Blocks.DIRT);
        helper.runAfterDelay(20, () -> {
            final TestStructures.TankBlockEntity tank = tank(helper);
            helper.assertTrue(!formed(helper), "a tank with dirt on an edge does not form");
            final ShapeMatch.Failure failure = tank.structure().failure().orElseThrow();
            helper.assertTrue(failure.found().equals("minecraft:dirt") && failure.relX() == 1
                    && failure.relY() == 1 && failure.relZ() == 1, "the failure names the dirt and where it is; got "
                    + failure);
            final List<Text> lines = new ArrayList<>();
            tank.describe(lines, false);
            helper.assertTrue(lines.stream().anyMatch(line -> line.english().startsWith("Needs ")
                    && line.english().endsWith("Dirt")), "whoever looks is told what it needs; got " + lines);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void tank_keepsItsBlocksFromThoseItsOwnerDoesNotLet(final GameTestHelper helper) {
        final FakePlayer owner = player(helper, "tank_owner");
        final FakePlayer stranger = player(helper, "tank_stranger");
        build(helper);
        tank(helper).own(Ownership.of(owner.getUUID()));
        helper.succeedWhen(() -> {
            helper.assertTrue(formed(helper), "the tank forms");
            final BlockPos edge = helper.absolutePos(CORNER);
            final BlockState iron = helper.getLevel().getBlockState(edge);
            helper.assertTrue(LevelStructures.of(helper.getLevel()).controllerHolding(edge)
                    .equals(helper.absolutePos(CONTROLLER)), "its corner is known as the tank's");
            helper.assertTrue(LevelStructures.of(helper.getLevel()).controllerHolding(
                    helper.absolutePos(new BlockPos(5, 1, 5))) == null, "a block apart from it is not");
            final BlockEvent.BreakEvent byStranger = new BlockEvent.BreakEvent(helper.getLevel(), edge, iron,
                    stranger);
            NeoForge.EVENT_BUS.post(byStranger);
            helper.assertTrue(byStranger.isCanceled(), "a stranger cannot break a block of another's tank");
            final BlockEvent.BreakEvent byOwner = new BlockEvent.BreakEvent(helper.getLevel(), edge, iron, owner);
            NeoForge.EVENT_BUS.post(byOwner);
            helper.assertTrue(!byOwner.isCanceled(), "its owner can");
        });
    }

    @GameTest(template = ARENA)
    public static void tank_keepsWhetherItIsFormedInItsSave(final GameTestHelper helper) {
        build(helper);
        helper.setBlock(new BlockPos(2, 3, 2), Blocks.HOPPER);
        helper.succeedWhen(() -> {
            helper.assertTrue(formed(helper), "the tank forms");
            final TestStructures.TankBlockEntity tank = tank(helper);
            final CompoundTag saved = tank.saveWithFullMetadata(helper.getLevel().registryAccess());
            final BlockEntity back = BlockEntity.loadStatic(tank.getBlockPos(), tank.getBlockState(), saved,
                    helper.getLevel().registryAccess());
            helper.assertTrue(back instanceof TestStructures.TankBlockEntity loaded && loaded.structure() != null
                    && loaded.structure().formed() && loaded.structure().parallel() == 1,
                    "a tank read back from its save is formed, with what it runs at once");
        });
    }

    /* The tank: iron on its edges, glass on its faces, air inside, the controller on its north face. */
    private static void build(final GameTestHelper helper) {
        for (int x = LOW.getX(); x <= HIGH.getX(); x++) {
            for (int y = LOW.getY(); y <= HIGH.getY(); y++) {
                for (int z = LOW.getZ(); z <= HIGH.getZ(); z++) {
                    final int bounds = (x == LOW.getX() || x == HIGH.getX() ? 1 : 0)
                            + (y == LOW.getY() || y == HIGH.getY() ? 1 : 0)
                            + (z == LOW.getZ() || z == HIGH.getZ() ? 1 : 0);
                    final BlockPos at = new BlockPos(x, y, z);
                    if (bounds >= 2) {
                        helper.setBlock(at, Blocks.IRON_BLOCK);
                    } else if (bounds == 1) {
                        helper.setBlock(at, Blocks.GLASS);
                    }
                }
            }
        }
        helper.setBlock(CONTROLLER, TestStructures.TANK.get());
    }

    private static TestStructures.TankBlockEntity tank(final GameTestHelper helper) {
        if (helper.getBlockEntity(CONTROLLER) instanceof TestStructures.TankBlockEntity tank) {
            return tank;
        }
        throw new IllegalStateException("no tank controller where the test put it");
    }

    private static boolean formed(final GameTestHelper helper) {
        final MachineStructure structure = tank(helper).structure();
        return structure != null && structure.formed();
    }

    private static FakePlayer player(final GameTestHelper helper, final String name) {
        return FakePlayerFactory.get(helper.getLevel(), new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(
                StandardCharsets.UTF_8)), name));
    }
}
