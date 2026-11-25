/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import static dev.jstech.tests.testkit.computers.ChainMachineSite.formed;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machine;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.machineAt;
import static dev.jstech.tests.testkit.computers.ChainMachineSite.place;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.LithoStations;
import dev.jstech.computers.chain.machine.MaskWriting;
import dev.jstech.computers.chain.machine.WaferMachineBlockEntity;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.computers.lithography.LithographyDesk;
import dev.jstech.computers.lithography.LithographyLook;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A mask written by a Lithography Station's computer (computers/docs/design/survival.md, "Masks"): a station offers the
 * masks of the eras it serves that its computer may write; the mask picked is written on the Mask Blank in the
 * station in one layer's time, half on a computer two eras newer, and stands in the blank's slot; a station exposing a
 * wafer, or holding no blank, writes none; and a computer finds the stations linked to it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MaskWritingGameTests {

    private static final String ARENA = "empty";
    /* A Legacy station's layer: twenty seconds. */
    private static final int LAYER = 400;
    private static final int SETTLE = 20;
    private static final int BUCKET = 1_000;
    private static final long KRYPTON = 1_000L;

    private MaskWritingGameTests() {
    }

    @GameTest(template = ARENA)
    public static void offers_areTheMasksOfTheStationsErasItsComputerMayWrite(final GameTestHelper helper) {
        final List<MaskWriting.Offer> legacy = MaskWriting.offers(machine("lithography_legacy"), HardwareEra.STANDARD);
        helper.assertTrue(offers(legacy, k8()), "the Legacy station writes the K8 processor mask");
        helper.assertTrue(offers(legacy, ChainParts.MEMORY_MASKS.get(1).get())
                && offers(legacy, ChainParts.CONTROLLER_MASKS.get(1).get()), "and the Legacy memory and controller"
                + " masks");
        helper.assertTrue(offers(legacy, ChainChips.LASER_DIODE_MASK.get())
                && !offers(legacy, ChainChips.LED_MASK.get()), "the laser diodes' mask, not yet the LEDs'");
        helper.assertTrue(legacy.stream().allMatch(offer -> offer.era() == HardwareEra.LEGACY),
                "nothing of another era: " + legacy);
        helper.assertTrue(MaskWriting.offers(machine("lithography_legacy"), HardwareEra.VINTAGE).size()
                == legacy.size(), "a Vintage computer, the era before the station's, writes them all");
        final List<MaskWriting.Offer> arfOld = MaskWriting.offers(machine("lithography_arf"), HardwareEra.LEGACY);
        helper.assertTrue(!arfOld.isEmpty() && arfOld.stream().allMatch(offer -> offer.era()
                == HardwareEra.TRANSITION), "on a Legacy computer the ArF station writes the Transition's masks only");
        final List<MaskWriting.Offer> arf = MaskWriting.offers(machine("lithography_arf"), HardwareEra.TRANSITION);
        helper.assertTrue(arf.stream().anyMatch(offer -> offer.era() == HardwareEra.STANDARD)
                && offers(arf, ChainChips.LED_MASK.get()), "on a Transition computer the Standard's too, the LEDs'"
                + " among them");
        helper.assertTrue(arf.stream().filter(offer -> offer.mask() == ChainChips.LASER_DIODE_MASK.get()).count()
                == 1, "the laser diodes' mask once");
        helper.assertTrue(MaskWriting.offers(machine("lithography_vintage"), HardwareEra.STANDARD).isEmpty(),
                "the Vintage station writes its masks from a Layout, not from a computer");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = LAYER * 2)
    public static void write_turnsTheBlankIntoTheMaskInOneLayersTime(final GameTestHelper helper) {
        final BlockPos terminal = linkedLegacyStation(helper, EraKit.LEGACY);
        final Item k8 = k8();
        helper.runAfterDelay(SETTLE, () -> {
            station(helper, terminal).getInventory().insertItem(0, new ItemStack(ChainChips.MASK_BLANK.get()), false);
            final Optional<TextKey> refused = MaskWriting.write(station(helper, terminal), HardwareEra.LEGACY, k8);
            helper.assertTrue(refused.isEmpty(), "the write starts: " + refused);
        });
        helper.runAfterDelay(SETTLE + LAYER * 3 / 4, () -> helper.assertTrue(station(helper, terminal)
                .getInventory().getStackInSlot(0).is(ChainChips.MASK_BLANK.get()), "three quarters of a layer on, the"
                + " blank is still being written"));
        helper.runAfterDelay(SETTLE + LAYER + SETTLE * 3, () -> {
            final WaferMachineBlockEntity station = station(helper, terminal);
            helper.assertTrue(station.getInventory().getStackInSlot(0).is(k8), "a layer's time on, the K8 processor"
                    + " mask stands where its blank was: " + station.getInventory().getStackInSlot(0));
            helper.assertTrue(station.writes() == null, "and the station writes nothing more");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = LAYER * 2)
    public static void write_onAComputerTwoErasNewerTakesHalfTheTime(final GameTestHelper helper) {
        final BlockPos terminal = linkedLegacyStation(helper, EraKit.STANDARD);
        final Item k8 = k8();
        helper.runAfterDelay(SETTLE, () -> {
            station(helper, terminal).getInventory().insertItem(0, new ItemStack(ChainChips.MASK_BLANK.get()), false);
            helper.assertTrue(MaskWriting.write(station(helper, terminal), HardwareEra.STANDARD, k8).isEmpty(),
                    "the write starts");
        });
        helper.runAfterDelay(SETTLE + LAYER / 2 + SETTLE * 3, () -> {
            helper.assertTrue(station(helper, terminal).getInventory().getStackInSlot(0).is(k8), "half a layer on,"
                    + " the mask is written");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void write_isRefusedWithNoBlankInTheStation(final GameTestHelper helper) {
        final BlockPos terminal = linkedLegacyStation(helper, EraKit.LEGACY);
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(MaskWriting.write(station(helper, terminal), HardwareEra.LEGACY, k8())
                    .equals(Optional.of(MaskWriting.NO_BLANK)), "with no blank it writes nothing, and says why");
            helper.assertTrue(station(helper, terminal).writes() == null, "nothing is kept to write");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = LAYER)
    public static void write_isRefusedWhileTheStationExposes(final GameTestHelper helper) {
        final ChainMachine legacy = machine("lithography_legacy");
        final BlockPos terminal = linkedLegacyStation(helper, EraKit.STANDARD);
        helper.runAfterDelay(SETTLE, () -> {
            final WaferMachineBlockEntity station = station(helper, terminal);
            station.getInventory().insertItem(0, new ItemStack(ChainParts.MEMORY_MASKS.get(1).get()), false);
            station.getInventory().insertItem(1, new ItemStack(MaterialUses.item(Elements.of(Element.SILICON),
                    Forms.WAFER).get()), false);
            final IFluidHandler inlet = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK,
                    helper.absolutePos(place(legacy, "liquid_inlet")), Direction.UP);
            helper.assertTrue(inlet != null, "the liquid inlet opens onto the station");
            inlet.fill(new FluidStack(MaterialLiquids.fluid(SeriesChemicals.PHOTORESIST).get(), BUCKET),
                    IFluidHandler.FluidAction.EXECUTE);
            station.matter().inputs(MatterState.GAS).getFirst().insert(new MatterStack(Elements.of(Element.KRYPTON),
                    KRYPTON), false);
        });
        helper.runAfterDelay(SETTLE * 4, () -> {
            final WaferMachineBlockEntity station = station(helper, terminal);
            helper.assertTrue(station.exposing(), "the station exposes the wafer");
            helper.assertTrue(MaskWriting.write(station, HardwareEra.STANDARD, k8())
                    .equals(Optional.of(MaskWriting.EXPOSING)), "a mask is not written while it exposes");
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void stations_ofAComputerAreTheOnesLinkedToIt(final GameTestHelper helper) {
        final BlockPos terminal = linkedLegacyStation(helper, EraKit.LEGACY);
        helper.runAfterDelay(SETTLE, () -> {
            final List<ChainMachineBlockEntity> stations = LithoStations.of(helper.getLevel(), computer(helper));
            helper.assertTrue(stations.size() == 1 && stations.getFirst() == station(helper, terminal),
                    "the computer finds the station on its cable: " + stations);
            helper.succeed();
        });
    }

    @GameTest(template = ARENA, timeoutTicks = LAYER)
    public static void look_showsTheStationItsBlankItsMasksAndTheWriting(final GameTestHelper helper) {
        final BlockPos terminal = linkedLegacyStation(helper, EraKit.LEGACY);
        helper.runAfterDelay(SETTLE, () -> {
            final AbstractComputerBlockEntity computer = computer(helper);
            station(helper, terminal).getInventory().insertItem(0, new ItemStack(ChainChips.MASK_BLANK.get()), false);
            final LithographyLook idle = LithographyDesk.look(helper.getLevel(), computer, 0);
            helper.assertTrue(idle.stations() == 1 && idle.station().english().equals("Legacy, KrF"),
                    "the station, its era and its light: " + idle.station().english());
            helper.assertTrue(idle.status().english().equals("Idle") && idle.blank()
                    && idle.slot().english().equals("Mask Blank"), "idle, a Mask Blank in its slot: "
                    + idle.status().english() + ", " + idle.slot().english());
            helper.assertTrue(idle.picks().stream().anyMatch(pick -> pick.name().english().equals("K8 processor")
                    && pick.kind().equals(MaskWriting.Kind.PROCESSORS.serializedName())),
                    "the K8 processor among its masks");
            helper.assertTrue(idle.supplies().size() == 2, "its photoresist and its krypton: " + idle.supplies());
            final ResourceLocation k8 = BuiltInRegistries.ITEM.getKey(k8());
            helper.assertTrue(LithographyDesk.write(helper.getLevel(), computer, 0, k8).isEmpty(), "W writes it");
        });
        helper.runAfterDelay(SETTLE * 4, () -> {
            final LithographyLook writing = LithographyDesk.look(helper.getLevel(), computer(helper), 0);
            helper.assertTrue(writing.status().english().equals("Writing a mask") && writing.writes()
                    && writing.writing().english().equals("K8 processor") && writing.writingPermille() > 0
                    && writing.lithoMb() == 2, "writing the K8 processor mask, the computer holding 2 MB for it: "
                    + writing.status().english() + ", " + writing.writingPermille() + ", " + writing.lithoMb());
            helper.succeed();
        });
    }

    @GameTest(template = ARENA)
    public static void look_ofAComputerWithNoStationShowsNone(final GameTestHelper helper) {
        EraNetwork.runningComputer(TestWorldBuilder.forGameTest(helper), EraKit.LEGACY, computerPlace());
        helper.runAfterDelay(SETTLE, () -> {
            helper.assertTrue(LithographyDesk.look(helper.getLevel(), computer(helper), 0).stations() == 0,
                    "no station, nothing to show");
            helper.succeed();
        });
    }

    /*
     * A Legacy station formed, its peripheral port cabled to a running computer of {@code kit}'s era beside it; its
     * terminal's place.
     */
    private static BlockPos linkedLegacyStation(final GameTestHelper helper, final EraKit kit) {
        final ChainMachine legacy = machine("lithography_legacy");
        final BlockPos terminal = formed(helper, legacy);
        TestCables.lay(helper, place(legacy, "peripheral_port").east(), ComputingModule.VINTAGE_PERIPHERAL_CABLE);
        EraNetwork.runningComputer(TestWorldBuilder.forGameTest(helper), kit, computerPlace());
        return terminal;
    }

    /* Where the station's computer stands: two blocks east of its peripheral port, past the cable. */
    private static BlockPos computerPlace() {
        return place(machine("lithography_legacy"), "peripheral_port").east(2);
    }

    private static AbstractComputerBlockEntity computer(final GameTestHelper helper) {
        return (AbstractComputerBlockEntity) helper.getBlockEntity(computerPlace());
    }

    private static WaferMachineBlockEntity station(final GameTestHelper helper, final BlockPos terminal) {
        return (WaferMachineBlockEntity) machineAt(helper, terminal);
    }

    /* The Legacy K8 processor mask. */
    private static Item k8() {
        return ChainChips.PROCESSORS.stream().filter(die -> die.era() == HardwareEra.LEGACY
                && die.path().equals("k8")).findFirst().orElseThrow().mask().get();
    }

    private static boolean offers(final List<MaskWriting.Offer> offers, final Item mask) {
        return offers.stream().anyMatch(offer -> offer.mask() == mask);
    }
}
