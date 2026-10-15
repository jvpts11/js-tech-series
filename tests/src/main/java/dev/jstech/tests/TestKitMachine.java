/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineEnergy;
import dev.jstech.core.machine.MachineParameter;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.machine.RepairKitItem;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A machine of the test mod built on the whole of the Core's machine kit: a device block, energy of the test mod's
 * own through a port of the low class with a fuse and a battery, a tank of gas in and one out, a value of its own the
 * outside may set; it turns cobblestone into stone in twenty ticks. And a repair kit for it.
 */
public final class TestKitMachine {

    /** What the machine holds and takes, and spends a tick. */
    public static final long CAPACITY = 10_000L;
    public static final long TAKES = 1_000L;
    public static final long SPENDS = 10L;
    /** How long its work takes, in ticks. */
    public static final int TICKS = 20;
    /** What the test repair kit holds and gives back a second. */
    public static final int KIT_USES = 100;
    public static final int KIT_PER_SECOND = 25;
    public static final ProcessingMachineBlockEntity.Layout LAYOUT = ProcessingMachineBlockEntity.Layout.items(1, 1)
            .withMatter(MatterState.GAS, 1, 1, 1_000L, 10L);
    public static final TextKey PRESSURE = TextKey.of("jstests.kit_machine.pressure", "Pressure");

    /* Its block state and model are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> BLOCK = TestSounds.CONTENT.block("test_kit_machine",
            Device.of(() -> TestKitMachine.BLOCK_ENTITY.get()).ticks(MachineBlockEntity::serverTick)::block)
            .named("Test kit machine").look(IBlockLook.cubeAll("test_kit_machine")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KitMachineBlockEntity>> BLOCK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_kit_machine", KitMachineBlockEntity::new, BLOCK);
    public static final ItemEntry<RepairKitItem> REPAIR_KIT = TestSounds.CONTENT.item("test_repair_kit",
            properties -> new RepairKitItem(properties, KIT_USES, KIT_PER_SECOND)).named("Test Repair Kit")
            .look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();

    private TestKitMachine() {
    }

    /** Declares the machine and its kit, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** The machine at work. */
    public static final class KitMachineBlockEntity extends ProcessingMachineBlockEntity {

        private double pressure = 1.0;

        public KitMachineBlockEntity(final BlockPos pos, final BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state, LAYOUT, MachineEnergy.of(TestEnergies.VOLTS, TestEnergies.LOW,
                    CAPACITY, TAKES), SPENDS);
        }

        /** The value of its own the outside may set. */
        public double pressure() {
            return pressure;
        }

        @Override
        protected Optional<Processing> process(final Level level, final ProcessingInput input) {
            return input.items().getFirst().is(Items.COBBLESTONE)
                    ? Optional.of(Processing.single(0, new ItemStack(Items.STONE), TICKS)) : Optional.empty();
        }

        @Override
        protected void declareParameters(final List<MachineParameter> declared) {
            declared.add(MachineParameter.set("pressure", PRESSURE, null, 1, 10, () -> pressure,
                    value -> pressure = value));
        }
    }
}
