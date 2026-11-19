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

import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.machine.MachineStructure;
import dev.jstech.core.material.Material;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.multiblock.PortKind;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * One port per product (computers/docs/design/survival.md): a machine's repeated ports are each bound to the product
 * their tag names, so a pipe on one carries that alone. The Siemens Reactor's HCL inlet takes hydrogen chloride and
 * no hydrogen, its H2 inlet hydrogen and no hydrogen chloride; the Air Separation Unit's four outlets give krypton,
 * argon, nitrogen and oxygen, one each.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ChainPortProductsGameTests {

    private static final String ARENA = "empty";
    /* What a test pours in, in millibuckets. */
    private static final long POUR = 100;

    private ChainPortProductsGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void siemens_eachGasInletTakesItsOwnGasAlone(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("siemens_reactor"));
        final Material hydrogen = Elements.of(Element.HYDROGEN);
        final Material chloride = SeriesChemicals.HYDROGEN_CHLORIDE;
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    final ChainMachineBlockEntity reactor = machineAt(helper, terminal);
                    final List<BlockPos> inlets = ports(reactor, PortKind.GAS_INPUT);
                    helper.assertTrue(inlets.size() == 2, "the reactor has two gas inlets: " + inlets.size());
                    for (final BlockPos inlet : inlets) {
                        final Set<ResourceLocation> own = reactor.portProducts(inlet);
                        helper.assertTrue(own.size() == 1, "each inlet is bound to one gas: " + own);
                        final Material taken = own.contains(chloride.id()) ? chloride : hydrogen;
                        final Material refused = taken == chloride ? hydrogen : chloride;
                        final IMatterHandler gas = helper.getLevel().getCapability(MatterCapabilities.GAS, inlet,
                                Direction.UP);
                        helper.assertTrue(gas != null, "the inlet opens onto the reactor's gas");
                        helper.assertTrue(gas.insert(new MatterStack(refused, POUR), true) == 0, "the "
                                + taken.id().getPath() + " inlet takes no " + refused.id().getPath());
                        helper.assertTrue(gas.insert(new MatterStack(taken, POUR), true) > 0, "the "
                                + taken.id().getPath() + " inlet takes its own");
                    }
                    final Set<ResourceLocation> bound = new HashSet<>();
                    inlets.forEach(inlet -> bound.addAll(reactor.portProducts(inlet)));
                    helper.assertTrue(bound.equals(Set.of(hydrogen.id(), chloride.id())),
                            "one inlet each for hydrogen chloride and hydrogen: " + bound);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void airSeparation_eachGasOutletGivesItsOwnGas(final GameTestHelper helper) {
        final BlockPos terminal = formed(helper, machine("air_separation_unit"));
        helper.startSequence()
                .thenIdle(20)
                .thenExecute(() -> {
                    final ChainMachineBlockEntity unit = machineAt(helper, terminal);
                    final List<BlockPos> outlets = ports(unit, PortKind.GAS_OUTPUT);
                    final List<Set<ResourceLocation>> bound = new ArrayList<>();
                    outlets.forEach(outlet -> bound.add(unit.portProducts(outlet)));
                    final Set<ResourceLocation> gases = new HashSet<>();
                    bound.forEach(gases::addAll);
                    helper.assertTrue(outlets.size() == 4 && bound.stream().allMatch(own -> own.size() == 1)
                            && gases.equals(Set.of(Elements.of(Element.KRYPTON).id(), Elements.of(Element.ARGON).id(),
                                    Elements.of(Element.NITROGEN).id(), Elements.of(Element.OXYGEN).id())),
                            "four outlets, one gas each: krypton, argon, nitrogen, oxygen: " + bound);
                })
                .thenSucceed();
    }

    /* Where {@code machine}'s ports of {@code kind} stand, in the world. */
    private static List<BlockPos> ports(final ChainMachineBlockEntity machine, final PortKind kind) {
        final MachineStructure structure = machine.structure();
        final List<BlockPos> out = new ArrayList<>();
        if (structure != null) {
            structure.ports().stream().filter(port -> port.kind() == kind).forEach(port -> out.add(port.pos()));
        }
        return out;
    }
}
