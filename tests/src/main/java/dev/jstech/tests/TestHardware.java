/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.computers.api.ComputersRegisterEvent;
import dev.jstech.computers.block.CaseStyle;
import dev.jstech.computers.block.SmallComputerBlock;
import dev.jstech.computers.hardware.CaseParts;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.hardware.CpuDesign;
import dev.jstech.computers.hardware.CpuSocketId;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.Isas;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PcieGeneration;
import dev.jstech.computers.hardware.RamGeneration;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.os.HostScope;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Set;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;

/**
 * Hardware of the test era, brought the way another mod brings it: a generation of memory, a socket, a processor, a
 * board and a memory module of the Quantum era, a machine of its own, and a small computer of that era and machine,
 * all declared through the computers' register event. Its case counts fewer parts than a Personal Computer's, so the
 * computer's block entity is seen to read them from its entry.
 */
@TextHolder
public final class TestHardware {

    public static final TextKey QUANTUM_LAB_NAME = TextKey.of("jstests.machine.quantum_lab", "a quantum lab");
    public static final TextKey QUANTUM_LAB_ONLY =
            TextKey.of("jstests.machine.quantum_lab_only", "%s only installs in a quantum lab");
    /** A memory generation no machine of the series takes, with no wait of its own. */
    public static final RamGeneration QRAM = new RamGeneration("jstests:qram", Text.literal("QRAM"), 0);
    public static final CpuSocketId QUANTUM_SOCKET = new CpuSocketId("jstests:q1");
    /** The machine the Quantum computer is, which only the programs made for it install on. */
    public static final HostScope QUANTUM_LAB = new HostScope("jstests:quantum_lab", QUANTUM_LAB_NAME,
            QUANTUM_LAB_ONLY);
    /* One processor, two memory modules, two cards, one disk and one bay: fewer than a Personal Computer's. */
    public static final CaseParts QUANTUM_PARTS = new CaseParts(1, 2, 2, 1, 1, 0);
    public static final CpuSpec QUANTUM_CPU_SPEC = new CpuSpec(TestProgression.QUANTUM, Isas.X86_64, QUANTUM_SOCKET,
            32, 6_000, 95, false, CpuDesign.UNSPECIFIED);
    public static final MotherboardSpec QUANTUM_BOARD_SPEC = new MotherboardSpec(FormFactor.ATX,
            TestProgression.QUANTUM, QUANTUM_SOCKET, 1, Set.of(QRAM), 2, PcieGeneration.PCIE_6_0, 2, 1, null);
    public static final RamSpec QUANTUM_RAM_SPEC = new RamSpec(TestProgression.QUANTUM, QRAM, 65_536L, 4);
    public static final TextKey TEST_BUS_ONLY = TextKey.of("jstests.card.test_bus_only",
            "Only a Test Quantum Computer seats a test bus card");
    /** A kind of card made for the Quantum computer alone, as a mod's field bus card is made for its controller. */
    public static final ExpansionCardKind TEST_BUS = new ExpansionCardKind("jstests:test_bus")
            .onlyIn(TEST_BUS_ONLY, QUANTUM_LAB);
    /** How many machines a test bus card lets its computer read on the industrial control cable. */
    public static final int TEST_BUS_MACHINES = 2;

    public static final ItemEntry<Item> QUANTUM_CPU = TestSounds.CONTENT.item("quantum_cpu", Item::new)
            .named("Test Quantum Processor").look(IItemLook.parent("minecraft:item/amethyst_shard"))
            .tab(TestItems.ITEMS).register();
    public static final ItemEntry<Item> QUANTUM_BOARD = TestSounds.CONTENT.item("quantum_board", Item::new)
            .named("Test Quantum Board").look(IItemLook.parent("minecraft:item/map")).tab(TestItems.ITEMS)
            .register();
    public static final ItemEntry<Item> QUANTUM_RAM = TestSounds.CONTENT.item("quantum_ram", Item::new)
            .named("Test Quantum Memory").look(IItemLook.parent("minecraft:item/prismarine_shard"))
            .tab(TestItems.ITEMS).register();
    /**
     * A memory module the test mod registers but never declares: the startup script in {@code tests/src/kubejs}
     * declares it, in a run with KubeJS, the way a modpack declares a part as an object.
     */
    public static final ItemEntry<Item> SCRIPTED_RAM = TestSounds.CONTENT.item("scripted_ram", Item::new)
            .named("Test Scripted Memory").look(IItemLook.parent("minecraft:item/quartz")).tab(TestItems.ITEMS)
            .register();
    public static final ItemEntry<Item> TEST_BUS_CARD = TestSounds.CONTENT.item("test_bus_card", Item::new)
            .named("Test Bus Card").look(IItemLook.parent("minecraft:item/comparator")).tab(TestItems.ITEMS)
            .register();
    /** A small computer of the Quantum era, in the cases this mod ships: the Advanced's, its nearest earlier era's. */
    public static final BlockEntry<SmallComputerBlock> QUANTUM_COMPUTER = TestSounds.CONTENT.block(
                    "quantum_computer", properties -> new SmallComputerBlock(properties, TestProgression.QUANTUM,
                            CaseStyle.NEUTRAL))
            .named("Test Quantum Computer").properties(properties -> properties.strength(2.0F).noOcclusion())
            .look(IBlockLook.fixed(new IBlockModel.Handmade("quantum_computer")))
            .item().itemLook(IItemLook.parent("minecraft:item/observer")).tab(TestItems.ITEMS).register();

    private TestHardware() {
    }

    /** Declares it all through the computers' register event, from the mod's constructor. */
    public static void register(final IEventBus modEventBus) {
        modEventBus.addListener(ComputersRegisterEvent.class, TestHardware::declare);
    }

    private static void declare(final ComputersRegisterEvent event) {
        event.memoryGeneration(QRAM);
        event.machine(QUANTUM_LAB);
        event.processor(QUANTUM_CPU.get(), QUANTUM_CPU_SPEC);
        event.motherboard(QUANTUM_BOARD.get(), QUANTUM_BOARD_SPEC);
        event.memory(QUANTUM_RAM.get(), QUANTUM_RAM_SPEC);
        event.computer(QUANTUM_COMPUTER.get(), new ComputerDef(TestProgression.QUANTUM, Set.of(FormFactor.ATX),
                QUANTUM_LAB, QUANTUM_PARTS));
        event.cardKind(TEST_BUS);
        event.expansionCard(TEST_BUS_CARD.get(), new TestBusCardSpec(), TestProgression.QUANTUM);
    }

    /** The test bus card: a card of the Quantum computer alone, which reads two machines on the control cable. */
    private record TestBusCardSpec() implements IExpansionCardSpec {

        @Override
        public PcieGeneration bus() {
            return PcieGeneration.PCIE_6_0;
        }

        @Override
        public int tdpWatts() {
            return 10;
        }

        @Override
        public ExpansionCardKind kind() {
            return TEST_BUS;
        }

        @Override
        public int controlPorts() {
            return TEST_BUS_MACHINES;
        }
    }
}
