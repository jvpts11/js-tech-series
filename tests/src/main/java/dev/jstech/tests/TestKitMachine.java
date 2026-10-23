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
import dev.jstech.core.machine.MachineFamily;
import dev.jstech.core.machine.MachineInstallerItem;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineParameter;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingMachineBlockEntity;
import dev.jstech.core.machine.RepairKitItem;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * A machine of the test mod built on the whole of the Core's machine kit: a device block, energy of the test mod's
 * own through a port of the low class with a fuse and a battery, a tank of gas in and one out, a value of its own the
 * outside may set; it turns cobblestone into stone in twenty ticks, and works the refining recipes. A repair kit for
 * it, and a catalyst that wears into its spent form. And a factory of three lanes on FE, working the choosing recipes.
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
    /** How many recipes the test catalyst lasts. */
    public static final int CATALYST_USES = 3;
    public static final ProcessingMachineBlockEntity.Layout LAYOUT = ProcessingMachineBlockEntity.Layout.items(2, 2)
            .withMatter(MatterState.GAS, 1, 1, 1_000L, 10L).withUpgrades(2);
    /** Its first output slot. */
    public static final int OUT = 2;
    public static final TextKey DIAL = TextKey.of("jstests.kit_machine.dial", "Dial");

    /** Its second tier: a third input and a third upgrade bay, and twice the energy. */
    public static final ProcessingMachineBlockEntity.Layout LAYOUT_MK2 = ProcessingMachineBlockEntity.Layout
            .items(3, 2).withMatter(MatterState.GAS, 1, 1, 1_000L, 10L).withUpgrades(3);
    /** The second tier's first output slot. */
    public static final int OUT_MK2 = 3;

    /* Its block states and models are written by hand: the test mod writes no models of its own. */
    public static final BlockEntry<DeviceBlock> BLOCK = TestSounds.CONTENT.block("test_kit_machine",
            Device.of(() -> TestKitMachine.BLOCK_ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new)::block)
            .named("Test kit machine").look(IBlockLook.cubeAll("test_kit_machine")).item()
            .itemLook(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final BlockEntry<DeviceBlock> BLOCK_MK2 = TestSounds.CONTENT.block("test_kit_machine_mk2",
            Device.of(() -> TestKitMachine.BLOCK_ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new)::block)
            .named("Test kit machine Mk2").look(IBlockLook.cubeAll("test_kit_machine_mk2")).item()
            .itemLook(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    /* One block entity for both tiers, which reads its tier from its block. */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KitMachineBlockEntity>> BLOCK_ENTITY =
            TestSounds.CONTENT.blockEntity("test_kit_machine", KitMachineBlockEntity::new, BLOCK, BLOCK_MK2);
    /** The kit machine's two tiers, as one family. */
    public static final MachineFamily FAMILY = MachineFamily.of(ResourceLocation.fromNamespaceAndPath(JsTests.MODID,
            "test_kit_machine"), List.of(BLOCK, BLOCK_MK2));
    /** Grows a kit machine into its second tier. */
    public static final ItemEntry<MachineInstallerItem> INSTALLER = TestSounds.CONTENT.item("test_kit_installer",
            properties -> new MachineInstallerItem(properties, () -> FAMILY, 1)).named("Test Kit Installer")
            .look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final ItemEntry<RepairKitItem> REPAIR_KIT = TestSounds.CONTENT.item("test_repair_kit",
            properties -> new RepairKitItem(properties, KIT_USES, KIT_PER_SECOND)).named("Test Repair Kit")
            .look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    /** A catalyst that lasts {@value #CATALYST_USES} recipes and then becomes its spent form, as data names it. */
    public static final ItemEntry<Item> CATALYST = TestSounds.CONTENT.item("test_catalyst", Item::new)
            .named("Test Catalyst").look(IItemLook.parent("minecraft:item/blaze_powder")).durability(CATALYST_USES)
            .tab(TestItems.ITEMS).register();
    public static final ItemEntry<Item> SPENT_CATALYST = TestSounds.CONTENT.item("test_spent_catalyst", Item::new)
            .named("Test Spent Catalyst").look(IItemLook.parent("minecraft:item/gunpowder")).tab(TestItems.ITEMS)
            .register();
    /** The kit machine's recipes: a catalyst, a gas in and out, a byproduct, the pressure and temperature it needs. */
    public static final ProcessingKind REFINING = TestSounds.CONTENT.processing("test_refining", "Test Refining",
            () -> TestKitMachine.BLOCK);
    /** Two recipes of the same input, gravel, for the player to pick between. */
    public static final ProcessingKind CHOOSING = TestSounds.CONTENT.processing("test_choosing", "Test Choosing",
            () -> TestKitMachine.FACTORY);
    /** A factory of three lanes, which sorts its inputs among them. */
    public static final int LANES = 3;
    public static final BlockEntry<DeviceBlock> FACTORY = TestSounds.CONTENT.block("test_factory",
            Device.of(() -> TestKitMachine.FACTORY_ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new)::block)
            .named("Test factory").look(IBlockLook.cubeAll("test_factory")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FactoryBlockEntity>> FACTORY_ENTITY =
            TestSounds.CONTENT.blockEntity("test_factory", FactoryBlockEntity::new, FACTORY);

    private TestKitMachine() {
    }

    /** Declares the machine and its kit, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /* Cobblestone into stone, in twenty ticks. */
    private static Optional<ProcessingMachineBlockEntity.Processing> stone(final ProcessingInput input) {
        return input.items().getFirst().is(Items.COBBLESTONE) ? Optional.of(ProcessingMachineBlockEntity.Processing
                .single(0, new ItemStack(Items.STONE), TICKS)) : Optional.empty();
    }

    /** The machine at work. */
    public static final class KitMachineBlockEntity extends ProcessingMachineBlockEntity {

        private double dial = 1.0;

        public KitMachineBlockEntity(final BlockPos pos, final BlockState state) {
            super(BLOCK_ENTITY.get(), pos, state, mk2(state) ? LAYOUT_MK2 : LAYOUT, MachineEnergy.of(
                    TestEnergies.VOLTS, TestEnergies.LOW, mk2(state) ? 2 * CAPACITY : CAPACITY, TAKES), SPENDS);
        }

        /** The value of its own the outside may set. */
        public double dial() {
            return dial;
        }

        @Override
        protected Optional<Processing> process(final Level level, final ProcessingInput input) {
            return byKind(level, REFINING, input).or(() -> stone(input));
        }

        @Override
        protected void declareParameters(final List<MachineParameter> declared) {
            declared.add(MachineParameter.set("dial", DIAL, null, 1, 10, () -> dial, value -> dial = value));
        }

        /* Whether the block is the second tier. */
        private static boolean mk2(final BlockState state) {
            return state.is(BLOCK_MK2.get());
        }
    }

    /** The factory at work: the choosing recipes, and cobblestone into stone, in each of its lanes. */
    public static final class FactoryBlockEntity extends ProcessingMachineBlockEntity {

        public FactoryBlockEntity(final BlockPos pos, final BlockState state) {
            super(FACTORY_ENTITY.get(), pos, state, ProcessingMachineBlockEntity.Layout.items(1, 1).withLanes(LANES),
                    MachineEnergy.fe((int) CAPACITY, (int) TAKES, 0), SPENDS);
        }

        @Override
        protected Optional<Processing> process(final Level level, final ProcessingInput input) {
            return byKind(level, CHOOSING, input).or(() -> stone(input));
        }
    }
}
