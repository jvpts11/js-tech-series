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
import dev.jstech.computers.blockentity.DataCableBlockEntity;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.multipart.Bundle;
import dev.jstech.core.multipart.CoreParts;
import dev.jstech.core.multipart.FaceParts;
import dev.jstech.core.multipart.PartBoxes;
import dev.jstech.core.multipart.Wire;
import dev.jstech.core.persistence.SaveLayout;
import dev.jstech.tests.JsTests;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Parts on a block's faces and wires through its middle: a bus mounted on a cable, aimed at, taken off with its item,
 * read back as a restart reads it and from a save of before, the kinds registered with the Core, and a bundle laying
 * each wire once in its nine lanes.
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
        for (final String id : ComputingParts.FORMER_NUMBERS.values()) {
            helper.assertTrue(CoreParts.REGISTRY.containsKey(ResourceLocation.parse(id)), id + " is registered");
        }
        same(helper, ComputingParts.IMPORT.get(), CoreParts.REGISTRY.get(ComputingParts.IMPORT.getId()),
                "the Import Bus by its id");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faceParts_mountOnceAFaceAndShapeTheCable(final GameTestHelper helper) {
        final DataCableBlockEntity cable = cable(helper);
        cable.addPart(Direction.NORTH, ComputingParts.IMPORT.get().create());

        helper.assertTrue(cable.hasPart(Direction.NORTH) && !cable.hasPart(Direction.SOUTH), "the north face only");
        same(helper, ComputingParts.IMPORT.get(), cable.partType(Direction.NORTH), "the kind on the north face");
        helper.assertTrue(Shapes.joinIsNotEmpty(cable.parts().shape(), PartBoxes.shape(Direction.NORTH),
                (a, b) -> a && b), "the part's plate in the shape");
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
        final DataCableBlockEntity cable = cable(helper);
        cable.addPart(Direction.NORTH, ComputingParts.IMPORT.get().create());
        cable.addPart(Direction.SOUTH, ComputingParts.EXPORT.get().create());
        final BlockPos pos = helper.absolutePos(CABLE);
        final Vec3 fromNorth = Vec3.atCenterOf(pos).add(0, 0, -2);
        final Vec3 throughBlock = Vec3.atCenterOf(pos).add(0, 0, 2);

        same(helper, Direction.NORTH, PartBoxes.aimed(pos, fromNorth, throughBlock, Shapes.empty(),
                cable::hasPart), "the part met first from the north");
        same(helper, Direction.SOUTH, PartBoxes.aimed(pos, throughBlock, fromNorth, Shapes.empty(),
                cable::hasPart), "the part met first from the south");
        // From the south, past the south part, the cable's core is met before the north part.
        same(helper, null, PartBoxes.aimed(pos, throughBlock, fromNorth,
                Shapes.box(5 / 16.0, 5 / 16.0, 5 / 16.0, 11 / 16.0, 11 / 16.0, 11 / 16.0),
                face -> face == Direction.NORTH), "nothing, when the middle is met first");
        same(helper, Direction.NORTH, PartBoxes.faceAt(pos, Vec3.atCenterOf(pos).add(0, 0, -0.45),
                cable::hasPart), "the part a click on the north plate lands on");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faceParts_comeOffWithTheirItem(final GameTestHelper helper) {
        final DataCableBlockEntity cable = cable(helper);
        cable.addPart(Direction.EAST, ComputingParts.EXPORT.get().create());

        cable.dropAllParts(helper.getLevel());

        helper.assertTrue(!cable.hasAnyPart(), "no part left on the cable");
        helper.assertItemEntityPresent(ComputingModule.EXPORT_BUS_ITEM.get(), CABLE, 2.0);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faceParts_surviveAReload(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final DataCableBlockEntity cable = cable(helper);
        final AbstractBusPart bus = ComputingParts.IMPORT.get().create();
        cable.addPart(Direction.UP, bus);
        bus.setName("intake");

        final CompoundTag saved = cable.saveWithoutMetadata(registries);
        final DataCableBlockEntity read = new DataCableBlockEntity(cable.getBlockPos(), cable.getBlockState());
        read.loadWithComponents(saved, registries);

        same(helper, ComputingParts.IMPORT.get(), read.partType(Direction.UP), "the kind read back");
        helper.assertTrue(read.getPart(Direction.UP) instanceof AbstractBusPart again && "intake".equals(again.name()),
                "the bus's name read back");
        same(helper, 2, SaveLayout.versionOf(saved), "the cable's layout version");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void faceParts_readACableSavedWithPartNumbers(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final DataCableBlockEntity cable = cable(helper);
        cable.addPart(Direction.WEST, ComputingParts.RECEIVING.get().create());
        final CompoundTag before = cable.saveWithoutMetadata(registries);
        // Version 1 numbered the kinds: the Receiving Bus was 3.
        before.putInt(SaveLayout.VERSION_KEY, 1);
        for (final Tag entry : before.getList(FaceParts.KEY, Tag.TAG_COMPOUND)) {
            ((CompoundTag) entry).put("Type", ByteTag.valueOf((byte) 3));
        }

        final DataCableBlockEntity read = new DataCableBlockEntity(cable.getBlockPos(), cable.getBlockState());
        read.loadWithComponents(before, registries);

        same(helper, ComputingParts.RECEIVING.get(), read.partType(Direction.WEST), "the bus saved as number 3");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void bundle_laysEachWireOnceInNineLanes(final GameTestHelper helper) {
        final HolderLookup.Provider registries = helper.getLevel().registryAccess();
        final AtomicInteger changes = new AtomicInteger();
        final Bundle bundle = new Bundle(changes::incrementAndGet);
        final Wire access = Wire.of(Connection.of(line("access")));
        final Wire red = new Wire(Connection.of(line("access")), Optional.of(DyeColor.RED));

        helper.assertTrue(bundle.add(access) && bundle.add(red), "two wires, apart by their colour");
        helper.assertTrue(!bundle.add(access), "the same wire twice");
        for (int i = 0; bundle.size() < Bundle.LANES; i++) {
            bundle.add(Wire.of(new Connection(line("line" + i), 0)));
        }
        helper.assertTrue(!bundle.add(Wire.of(Connection.of(line("tenth")))), "a tenth wire");
        helper.assertTrue(bundle.remove(access) && bundle.at(0) == red, "the wires after one taken out move up");

        final CompoundTag saved = new CompoundTag();
        bundle.save(saved, registries);
        final Bundle read = new Bundle(() -> { });
        read.load(saved, registries);
        same(helper, bundle.wires(), read.wires(), "the wires read back, lane by lane");
        same(helper, Bundle.LANES + 1, changes.get(), "a change for every wire laid and the one taken out");
        helper.succeed();
    }

    private static DataCableBlockEntity cable(final GameTestHelper helper) {
        helper.setBlock(CABLE, ComputingModule.ETHERNET_CABLE.get());
        return Objects.requireNonNull((DataCableBlockEntity) helper.getBlockEntity(CABLE), "the cable");
    }

    private static ResourceLocation line(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsTests.MODID, name);
    }

    private static void same(final GameTestHelper helper, final Object expected, final Object actual,
                             final String what) {
        helper.assertTrue(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }
}
