/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.industrial;

import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.hardware.ComputerDef;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.registry.ComputerRegistry;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.peripheral.ControlLinks;
import dev.jstech.core.jstechseries.peripheral.PeripheralReconciler;
import dev.jstech.core.jstechseries.peripheral.PortKind;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.industrial.IndustrialModule;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.industrial.integration.computers.FieldbusCardItem;
import dev.jstech.industrial.integration.computers.FieldbusCardSpec;
import dev.jstech.industrial.integration.computers.FieldbusCards;
import dev.jstech.industrial.integration.computers.IndustrialControlCable;
import dev.jstech.industrial.integration.computers.IndustrialControllerBlock;
import dev.jstech.industrial.integration.computers.IndustrialControllers;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraParts;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * J's Industrial with J's Computers installed: the Industrial Controller Computer of each case, a computer of its own
 * machine built as a Personal Computer of its era; the Fieldbus Cards, which go in it alone and give it as many
 * machines as they read; and the industrial control cable, which links it the nearest machines first and ends in its
 * card's plug at it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class IndustrialControllerGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(1, 2, 2);
    /* The cable out of the controller's east face, and a machine beside each block of it, the nearest first. */
    private static final List<BlockPos> RUN = List.of(new BlockPos(2, 2, 2), new BlockPos(3, 2, 2),
            new BlockPos(4, 2, 2));
    private static final List<BlockPos> MACHINES = List.of(new BlockPos(2, 2, 1), new BlockPos(3, 2, 1),
            new BlockPos(4, 2, 1));
    private static final int SETTLE = 4;

    private IndustrialControllerGameTests() {
    }

    @GameTest(template = ARENA)
    public static void everyController_isAComputerOfItsMachineWithAPersonalComputersParts(
            final GameTestHelper helper) {
        helper.assertTrue(HostScope.byId("jsindustrial:industrial_controller") == IndustrialControllers.MACHINE,
                "the Industrial Controller is a machine J's Computers knows");
        helper.assertTrue(IndustrialControllers.ALL.size() == 9, "one controller in each of the nine cases");
        for (final BlockEntry<IndustrialControllerBlock> entry : IndustrialControllers.ALL) {
            final IndustrialControllerBlock block = entry.get();
            final ComputerDef def = ComputerRegistry.computer(block);
            final ComputerDef personal = PersonalComputerBlockEntity.computer(block.era());
            helper.assertTrue(def != null && def.machine() == IndustrialControllers.MACHINE,
                    entry.getId() + " is a computer of the Industrial Controller");
            helper.assertTrue(def.parts().equals(personal.parts()) && def.formFactors().equals(personal.formFactors()),
                    entry.getId() + " takes the boards and parts of a Personal Computer of its era");
            helper.assertTrue(block.caseModel().equals(JsIndustrial.MODID + ":computer_"
                    + block.caseStyle().caseName(block.era()) + "_industrial_controller_computer"),
                    "its case is drawn from this mod's model: " + block.caseModel());
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void everyController_andEveryCard_haveTheirModelsInTheModsFiles(final GameTestHelper helper) {
        for (final BlockEntry<IndustrialControllerBlock> entry : IndustrialControllers.ALL) {
            final String model = entry.get().caseModel();
            assertShipped(helper, ComputingLooks.COMPUTER.modelFile(model));
            assertShipped(helper, ComputingLooks.COMPUTER.textureFile(model));
        }
        for (final ItemEntry<FieldbusCardItem> card : FieldbusCards.ALL) {
            final String name = card.getId().getPath();
            assertShipped(helper, industrial("geo/part/" + name + ".geo.json"));
            assertShipped(helper, industrial("textures/block/part/" + name + ".png"));
            assertShipped(helper, industrial("textures/item/" + name + ".png"));
        }
        for (final String plug : List.of("m12", "fieldbus_rj45", "profibus", "rs485")) {
            assertShipped(helper, industrial("models/block/cable/plug/" + plug + ".json"));
        }
        helper.succeed();
    }

    /** Each card goes in the controller of its own era, on the board a player of that era builds it on. */
    @GameTest(template = ARENA)
    public static void everyFieldbusCard_goesInTheControllerOfItsEra(final GameTestHelper helper) {
        int x = 0;
        for (final ItemEntry<FieldbusCardItem> card : FieldbusCards.ALL) {
            final Era era = card.get().spec().era();
            final EraKit kit = Arrays.stream(EraKit.values()).filter(k -> k.era() == era).findFirst().orElseThrow();
            final IndustrialControllerBlock block = IndustrialControllers.ALL.stream().map(BlockEntry::get)
                    .filter(controller -> controller.era() == era).findFirst().orElseThrow();
            final PersonalComputerBlockEntity controller = place(helper, block, new BlockPos(x, 2, 2));
            controller.getHardware().setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                    new ItemStack(kit.computerBoard()));
            helper.assertTrue(controller.isValidForSlot(controller.hardwareLayout().pcieStart(),
                    new ItemStack(card.get())), card.getId() + " goes in a " + era.id() + " controller");
            x += 2;
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void aFieldbusCard_goesInAControllerAlone(final GameTestHelper helper) {
        final PersonalComputerBlockEntity controller = place(helper,
                IndustrialControllers.INDUSTRIAL_CONTROLLER_COMPUTER.get(), CONTROLLER);
        final BlockPos pcAt = CONTROLLER.east(2);
        final PersonalComputerBlockEntity pc = place(helper, ComputingModule.PERSONAL_COMPUTER.get(), pcAt);
        final ItemStack card = new ItemStack(FieldbusCards.ETHERNET_IP.get());
        helper.assertTrue(HardwareRegistry.card(card) == FieldbusCards.ETHERNET_IP.get().spec(),
                "the item is a card because its entry says so");
        helper.assertTrue(controller.isValidForSlot(controller.hardwareLayout().pcieStart(), card),
                "a Fieldbus Card goes in an Industrial Controller Computer");
        helper.assertFalse(pc.isValidForSlot(pc.hardwareLayout().pcieStart(), card), "and in no other computer");
        helper.assertTrue(pc.refusal(pc.hardwareLayout().pcieStart(), card) == FieldbusCards.FIELDBUS_ONLY,
                "the slot says which computer it is made for");
        helper.succeed();
    }

    /**
     * A Vintage controller with its RS-485 card reads two machines: of the three its cable reaches, the two nearest
     * are linked to it, and the third to nothing.
     */
    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void aController_linksTheMachinesItsCardsRead_theNearestFirst(final GameTestHelper helper) {
        final PersonalComputerBlockEntity controller = place(helper,
                IndustrialControllers.VINTAGE_INDUSTRIAL_CONTROLLER_COMPUTER.get(), CONTROLLER);
        assemble(helper, controller, EraKit.VINTAGE.computerBoard(), FieldbusCards.RS485.get());
        for (int i = 0; i < RUN.size(); i++) {
            TestCables.lay(helper, RUN.get(i), IndustrialControlCable.CABLE);
            helper.setBlock(MACHINES.get(i), IndustrialModule.COMPRESSOR.get());
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> helper.assertTrue(controller.ports(PortKind.CONTROL) == 2,
                        "its card reads two machines: " + controller.ports(PortKind.CONTROL)))
                .thenWaitUntil(() -> {
                    helper.assertTrue(controllerOf(helper, MACHINES.get(0)).equals(Optional.of(controllerAt(helper))),
                            "the nearest machine is linked to it");
                    helper.assertTrue(controllerOf(helper, MACHINES.get(1)).equals(Optional.of(controllerAt(helper))),
                            "and the next one");
                })
                .thenExecute(() -> helper.assertTrue(controllerOf(helper, MACHINES.get(2)).isEmpty(),
                        "the third is past what its card reads"))
                .thenSucceed();
    }

    /*
     * A controller keeps the machines it reads past its checks of the screens and devices it holds ports for: a
     * machine keeps nothing of the controller, and only the walk of the cables lets it go.
     */
    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void aController_keepsItsMachines_pastTheChecksOfItsDevices(final GameTestHelper helper) {
        final PersonalComputerBlockEntity controller = place(helper,
                IndustrialControllers.VINTAGE_INDUSTRIAL_CONTROLLER_COMPUTER.get(), CONTROLLER);
        assemble(helper, controller, EraKit.VINTAGE.computerBoard(), FieldbusCards.RS485.get());
        for (int i = 0; i < 2; i++) {
            TestCables.lay(helper, RUN.get(i), IndustrialControlCable.CABLE);
            helper.setBlock(MACHINES.get(i), IndustrialModule.COMPRESSOR.get());
        }
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(controller.portsInUse(PortKind.CONTROL) == 2,
                        "it reads both machines"))
                .thenExecuteFor((int) (2 * PeripheralReconciler.INTERVAL_TICKS), () -> helper.assertTrue(
                        controller.portsInUse(PortKind.CONTROL) == 2, "and keeps them, tick after tick: "
                                + controller.portsInUse(PortKind.CONTROL)))
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void theControlCable_endsInTheCardsPlugAtTheController(final GameTestHelper helper) {
        final PersonalComputerBlockEntity controller = place(helper,
                IndustrialControllers.LEGACY_INDUSTRIAL_CONTROLLER_COMPUTER.get(), CONTROLLER);
        helper.assertTrue(controller.plugKind().isEmpty(), "without a card every cable ends in its own plug");
        assemble(helper, controller, EraKit.LEGACY.computerBoard(), FieldbusCards.PROFIBUS.get());
        helper.assertTrue(controller.plugKind().equals(FieldbusCardSpec.PROFIBUS),
                "with a PROFIBUS card the control cable plugs into its D-sub: " + controller.plugKind());
        helper.assertTrue(IndustrialControlCable.CABLE.get().plug(controller.plugKind()).equals(
                industrial("block/cable/plug/profibus")), "the cable's PROFIBUS plug");
        helper.assertTrue(IndustrialControlCable.CABLE.get().plug().equals(industrial("block/cable/plug/m12")),
                "and its own plug, at a machine, an M12");
        helper.assertTrue(FieldbusCards.ETHERCAT.get().spec().controlPlug().equals(FieldbusCardSpec.RJ45),
                "the field buses on Ethernet plug into an RJ45");
        helper.succeed();
    }

    private static PersonalComputerBlockEntity place(final GameTestHelper helper, final Block block,
                                                     final BlockPos at) {
        helper.setBlock(at, block);
        return (PersonalComputerBlockEntity) helper.getBlockEntity(at);
    }

    /*
     * The era's board with its processor, memory, supply and graphics card, and the Fieldbus Card in the next slot,
     * which the slot takes as a player's hand would put it there.
     */
    private static void assemble(final GameTestHelper helper, final PersonalComputerBlockEntity computer,
                                 final Item board, final Item card) {
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(board));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT, new ItemStack(EraParts.cpuFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START, new ItemStack(EraParts.ramFor(board)));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT,
                new ItemStack(EraParts.psuFor(EraParts.board(board).era())));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(EraParts.cardFor(board, GpuSpec.class)));
        final ItemStack fieldbus = new ItemStack(card);
        helper.assertTrue(computer.isValidForSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 1, fieldbus),
                "the card goes in the slot");
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START + 1, fieldbus);
    }

    private static Optional<BlockPos> controllerOf(final GameTestHelper helper, final BlockPos machine) {
        return ControlLinks.controllerOf(helper.getLevel(), helper.absolutePos(machine));
    }

    private static BlockPos controllerAt(final GameTestHelper helper) {
        return helper.absolutePos(CONTROLLER);
    }

    private static ResourceLocation industrial(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsIndustrial.MODID, path);
    }

    private static void assertShipped(final GameTestHelper helper, final ResourceLocation file) {
        helper.assertTrue(Files.exists(ModList.get().getModFileById(file.getNamespace()).getFile()
                .findResource("assets", file.getNamespace(), file.getPath())), "the mod ships " + file);
    }
}
