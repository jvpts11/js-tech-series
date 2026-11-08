/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.storage.DiskUsage;
import dev.jstech.computers.storage.DriveVolumes;
import dev.jstech.computers.storage.MatterChannel;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.computers.storage.StorageVolume;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.tests.JsTests;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A storage key for every state of matter of J's Core (computers/docs/design/storage.md): a gas, a plasma and the
 * rest are keys of their own kind, weigh as fluids do, survive a save and the wire, a chemical mod's chemical J's Core
 * links to one of its materials is that material, J's Core's own stores move them, and a disk counts them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MatterKeyGameTests {

    private static final String ARENA = "empty";

    private MatterKeyGameTests() {
    }

    /** A gas is a key of its own kind that weighs as a fluid and survives the save and the wire. */
    @GameTest(template = ARENA)
    public static void storageKey_aGasIsItsOwnKindAndSurvivesTheSaveAndTheWire(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        final StorageKey gas = StorageKey.matter(MatterState.GAS, hydrogen);
        final StorageKey plasma = StorageKey.matter(MatterState.PLASMA, hydrogen);
        helper.assertTrue(gas.isMatter() && gas.kind() == StorageKey.Kind.GAS && !gas.isChemical(),
                "a gas key is a gas");
        helper.assertTrue(gas.weight(250) == 250 && gas.batch() == 1000, "it weighs its millibuckets, like a fluid");
        helper.assertFalse(gas.equals(plasma), "the same material in another state is another key");
        helper.assertTrue(gas.material().filter(hydrogen::equals).isPresent(), "it holds its material");
        helper.assertTrue(gas.equals(StorageKey.byId(gas.id())) && gas.id().startsWith("gas|"),
                "its text id names it again; got " + gas.id());
        final RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, helper.getLevel().registryAccess());
        for (final StorageKey key : new StorageKey[]{gas, plasma}) {
            final StorageKey saved = StorageKey.CODEC.parse(ops, StorageKey.CODEC.encodeStart(ops, key).getOrThrow())
                    .getOrThrow();
            helper.assertTrue(key.equals(saved), key + " survives the save; got " + saved);
            final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                    helper.getLevel().registryAccess());
            StorageKey.STREAM_CODEC.encode(buf, key);
            final StorageKey wire = StorageKey.STREAM_CODEC.decode(buf);
            helper.assertTrue(key.equals(wire), key + " survives the wire; got " + wire);
        }
        helper.succeed();
    }

    /** A chemical mod's hydrogen is J's Core's hydrogen gas, and a chemical with no material stays a chemical. */
    @GameTest(template = ARENA)
    public static void storageKey_aLinkedChemicalIsItsMaterial(final GameTestHelper helper) {
        final ResourceLocation mekanismHydrogen = ResourceLocation.fromNamespaceAndPath("mekanism", "hydrogen");
        final StorageKey linked = StorageKey.chemical(mekanismHydrogen);
        helper.assertTrue(linked.equals(StorageKey.matter(MatterState.GAS, Elements.of(Element.HYDROGEN))),
                "the linked chemical is the gas; got " + linked);
        helper.assertTrue(mekanismHydrogen.equals(linked.chemicalId()), "known to its mod by its own id");
        helper.assertTrue(StorageKey.chemical(ResourceLocation.fromNamespaceAndPath("mekanism", "brine")).isChemical(),
                "a chemical with no material stays a chemical");
        helper.succeed();
    }

    /** J's Core's own store of a state moves that state's keys, and refuses every other kind. */
    @GameTest(template = ARENA)
    public static void matterChannel_movesItsStateInAndOutOfACoreStore(final GameTestHelper helper) {
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        final StorageKey gas = StorageKey.matter(MatterState.GAS, hydrogen);
        final MatterChannel channel = new MatterChannel(new MatterTank(MatterState.GAS, 4_000L, 1L, () -> { }),
                StorageKey.Kind.GAS);
        helper.assertValueEqual(channel.insert(gas, 500L, false), 500L, "the store takes the gas");
        helper.assertValueEqual(channel.count(gas), 500L, "and counts it");
        helper.assertTrue(channel.available().contains(gas), "and lists it");
        helper.assertValueEqual(channel.extract(gas, 200L, false), 200L, "and gives it back");
        helper.assertValueEqual(channel.insert(StorageKey.matter(MatterState.PLASMA, hydrogen), 100L, false), 0L,
                "a plasma does not go in a gas store");
        helper.succeed();
    }

    /** A disk keeps a gas as data and counts it with the other matter, by the millibucket. */
    @GameTest(template = ARENA)
    public static void volume_keepsAGasAndCountsIt(final GameTestHelper helper) {
        final ItemStack disk = new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500));
        final StorageVolume volume = DriveVolumes.of(disk);
        final StorageKey gas = StorageKey.matter(MatterState.GAS, Elements.of(Element.HYDROGEN));
        volume.add(gas, 2_000L);
        DriveVolumes.refreshUsage(disk, volume);
        final DiskUsage usage = DriveVolumes.usage(disk);
        helper.assertTrue(usage.chemicalMb() == 2_000L && usage.usedWeight() == 2_000L,
                "the disk counts 2,000 mB of other matter; got " + usage);
        helper.succeed();
    }
}
