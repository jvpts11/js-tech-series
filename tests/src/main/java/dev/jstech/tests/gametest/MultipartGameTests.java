/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.PartBoxes;
import dev.jstech.core.multipart.PartType;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Parts on a cable block's faces: a bus mounted on a cable, aimed at, taken off with its item when the block goes,
 * read back as a restart reads it, and the kinds registered with the Core.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MultipartGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos CABLE = new BlockPos(2, 2, 2);

    private MultipartGameTests() {
    }

    @GameTest(template = ARENA)
    public static void partTypes_areRegisteredWithTheCore(final GameTestHelper helper) {
        final List<PartType<?>> buses = List.of(ComputingParts.IMPORT.get(), ComputingParts.EXPORT.get(),
                ComputingParts.ROUTER.get(), ComputingParts.RECEIVING.get(), ComputingParts.INTERFACE.get(),
                ComputingParts.VINTAGE_INTERFACE.get(), ComputingParts.ADVANCED_INTERFACE.get());
        for (final PartType<?> bus : buses) {
            helper.assertTrue(bus.id() != null && CoreParts.REGISTRY.get(bus.id()) == bus, bus.id() + " is registered");
        }
        same(helper, ComputingParts.IMPORT.get(), CoreParts.REGISTRY.get(ComputingParts.IMPORT.getId()),
                "the Import Bus by its id");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faceParts_mountOnceAFaceAndShapeTheCable(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, ComputingModule.ETHERNET_CABLE);
        cable.addPart(Direction.NORTH, ComputingParts.IMPORT.get().create());

        helper.assertTrue(cable.hasPart(Direction.NORTH) && !cable.hasPart(Direction.SOUTH), "the north face only");
        same(helper, ComputingParts.IMPORT.get(), cable.partType(Direction.NORTH), "the kind on the north face");
        helper.assertTrue(Shapes.joinIsNotEmpty(cable.voxelShape(), PartBoxes.shape(Direction.NORTH),
                (a, b) -> a && b), "the part's plate in the block's shape");
        try {
            cable.addPart(Direction.NORTH, ComputingParts.EXPORT.get().create());
            helper.fail("a second part on the north face was not refused");
        } catch (final IllegalStateException expected) {
            // One part a face.
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faceParts_aimTheNearestPartAlongALook(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, ComputingModule.ETHERNET_CABLE);
        cable.addPart(Direction.NORTH, ComputingParts.IMPORT.get().create());
        cable.addPart(Direction.SOUTH, ComputingParts.EXPORT.get().create());
        final BlockPos pos = helper.absolutePos(CABLE);
        final Vec3 fromNorth = Vec3.atCenterOf(pos).add(0, 0, -2);
        final Vec3 throughBlock = Vec3.atCenterOf(pos).add(0, 0, 2);

        same(helper, Direction.NORTH, cable.aim(fromNorth, throughBlock).part(), "the part met first from the north");
        same(helper, Direction.SOUTH, cable.aim(throughBlock, fromNorth).part(), "the part met first from the south");
        same(helper, Direction.NORTH, PartBoxes.faceAt(pos, Vec3.atCenterOf(pos).add(0, 0, -0.45),
                cable::hasPart), "the part a click on the north plate lands on");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faceParts_comeOffWithTheirItem(final GameTestHelper helper) {
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, ComputingModule.ETHERNET_CABLE);
        cable.addPart(Direction.EAST, ComputingParts.EXPORT.get().create());

        helper.getLevel().destroyBlock(helper.absolutePos(CABLE), true);

        helper.assertItemEntityPresent(ComputingModule.EXPORT_BUS_ITEM.get(), CABLE, 2.0);
        helper.assertItemEntityPresent(ComputingModule.ETHERNET_CABLE.asItem(), CABLE, 2.0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faceParts_surviveAReload(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final CableBlockEntity cable = TestCables.lay(helper, CABLE, ComputingModule.ETHERNET_CABLE);
        final AbstractBusPart bus = ComputingParts.IMPORT.get().create();
        cable.addPart(Direction.UP, bus);
        bus.setName("intake");

        final CompoundTag saved = cable.saveWithoutMetadata(registries);
        final CableBlockEntity read = new CableBlockEntity(cable.getBlockPos(), cable.getBlockState());
        read.loadWithComponents(saved, registries);

        same(helper, ComputingParts.IMPORT.get(), read.partType(Direction.UP), "the kind read back");
        helper.assertTrue(read.getPart(Direction.UP) instanceof AbstractBusPart again && "intake".equals(again.name()),
                "the bus's name read back");
        same(helper, cable.fields().layout().version(), SaveLayout.versionOf(saved), "the cable's layout version");
        helper.succeed();
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
