/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.crafting.ProcessingPattern;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.program.IqlBusSetter;
import dev.jstech.computers.program.IqlEngine;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestMachines;
import dev.jstech.tests.testkit.CraftingRig;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The parts of the crafting network set from software: IQL statements and a program's calls set a Crafting Interface's
 * mode, most jobs, state and routes and a Crafting Input Router's filter and name, refused where the part, the pattern
 * or the router is not there, and marked with what set them.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class CraftingSoftwareGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 6;
    private static final int QUERY_ROWS = 16;
    private static final String SETTER = """
            using System.*;
            using System.IO.*;
            using System.Network.*;
            namespace Programs;
            class Mixers {
                static void Main() {
                    CraftInterface m = craftInterface("Mixer");
                    m.Exclusive(true).MaxJobs(2).Pause();
                    craftRouter("North").Only("gravel");
                    m.Route("Coarse dirt", "gravel", craftRouter("North"));
                    Console.PrintLine("set " + m.Name);
                }
            }
            """;

    private CraftingSoftwareGameTests() {
    }

    /** IQL sets the interface and its router, marked as typed, and refuses what is not there. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void iql_setsAnInterfaceAndItsRouters(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = mixerRig(world, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MainframeBlockEntity mainframe = net.mainframe();
                    final CraftingInterfacePart part = rig.part();
                    ok(helper, mainframe, "SET INTERFACE 'Mixer' EXCLUSIVE OFF");
                    helper.assertFalse(part.exclusive(true), "the interface runs several recipes now");
                    helper.assertTrue(IqlBusSetter.TYPED.equals(part.setBy(CraftingInterfacePart.MODE)),
                            "marked as set by IQL");
                    ok(helper, mainframe, "SET INTERFACE 'mixer' MAX JOBS 3");
                    helper.assertTrue(part.maxJobs() == 3, "the name is matched whatever its case; most jobs 3");
                    ok(helper, mainframe, "PAUSE INTERFACE 'Mixer'");
                    helper.assertTrue(part.paused(), "paused");
                    ok(helper, mainframe, "RESUME INTERFACE 'Mixer'");
                    helper.assertFalse(part.paused(), "running again");
                    ok(helper, mainframe, "SET INTERFACE 'Mixer' ROUTE 'Coarse dirt' INPUT gravel TO ROUTER 'North'");
                    helper.assertTrue(rig.router(1).id().equals(part.patterns().get(0).routes()
                            .get(StorageKey.of(Items.GRAVEL).id())), "the gravel is routed through the north router");
                    ok(helper, mainframe, "SET INTERFACE 'Mixer' ROUTE 'Coarse dirt' INPUT gravel AUTO");
                    helper.assertTrue(part.patterns().get(0).routes().isEmpty(), "and back to the filters");
                    ok(helper, mainframe, "SET ROUTER 'West' FILTER ONLY dirt");
                    helper.assertTrue(rig.router(0).filterKeys().equals(List.of(StorageKey.of(Items.DIRT))),
                            "the west router carries dirt; got " + rig.router(0).filterKeys());
                    ok(helper, mainframe, "RENAME ROUTER 'West' TO 'Dirt in'");
                    helper.assertTrue("Dirt in".equals(rig.router(0).name()), "the router is renamed");
                    ok(helper, mainframe, "RENAME INTERFACE 'Mixer' TO 'Mixer A'");
                    helper.assertTrue("Mixer A".equals(part.name()), "the interface is renamed");
                    refused(helper, mainframe, "SET INTERFACE 'Nobody' EXCLUSIVE ON");
                    refused(helper, mainframe, "SET ROUTER 'Nobody' FILTER NONE");
                    refused(helper, mainframe, "SET INTERFACE 'Mixer A' ROUTE 'Nothing' INPUT gravel AUTO");
                    refused(helper, mainframe, "SET INTERFACE 'Mixer A' ROUTE 'Coarse dirt' INPUT sand AUTO");
                    refused(helper, mainframe,
                            "SET INTERFACE 'Mixer A' ROUTE 'Coarse dirt' INPUT gravel TO ROUTER 'Elsewhere'");
                })
                .thenSucceed();
    }

    /** A program's calls set the interface and its router, chained, marked with the program. */
    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void sigma_setsAnInterfaceThroughItsCalls(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final TestWorldBuilder.CraftingNetwork net = world.buildCraftingNetwork();
        final CraftingRig rig = mixerRig(world, net);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final SigmaCompiler.Result built =
                            SigmaCompiler.compile(List.of(new SourceFile("Mixers.sgs", SETTER)));
                    helper.assertTrue(built.ok(), "the program compiles: " + String.join("\n", built.lines()));
                    final MainframeBlockEntity mainframe = net.mainframe();
                    final MachinePrograms programs = mainframe.programs();
                    final MachinePrograms.Started started =
                            programs.start("mixers.asm", built.assembly(), 1, mainframe);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    programs.tick(4096);
                    final List<String> said = programs.byId(started.id()).process().console();
                    helper.assertTrue(said.equals(List.of("set Mixer")), "it ran to its end; it said " + said);
                    final CraftingInterfacePart part = rig.part();
                    helper.assertTrue(part.exclusive(false) && part.maxJobs() == 2 && part.paused(),
                            "exclusive, most jobs 2, paused; got exclusive " + part.exclusive(false) + ", most jobs "
                                    + part.maxJobs() + ", paused " + part.paused() + ", named " + part.name()
                                    + ", set by " + part.setBy(CraftingInterfacePart.STATE));
                    helper.assertFalse(part.setBy(CraftingInterfacePart.STATE).isEmpty(), "marked with the program");
                    helper.assertTrue(rig.router(1).filterKeys().equals(List.of(StorageKey.of(Items.GRAVEL))),
                            "the north router carries gravel");
                    helper.assertTrue(rig.router(1).id().equals(part.patterns().get(0).routes()
                            .get(StorageKey.of(Items.GRAVEL).id())), "and the gravel is routed through it");
                })
                .thenSucceed();
    }

    /* The routed mixer, its interface, routers and pattern named as software names them. */
    private static CraftingRig mixerRig(final TestWorldBuilder world, final TestWorldBuilder.CraftingNetwork net) {
        final CraftingRig rig = CraftingRig.routed(world, net.cc(), TestMachines.MIXER.get(), true);
        rig.part().setName("Mixer");
        rig.router(0).setName("West");
        rig.router(1).setName("North");
        final ProcessingPattern mix = CraftingRig.mix(Items.DIRT, Items.GRAVEL, Items.COARSE_DIRT, 2)
                .withName("Coarse dirt", "");
        rig.hold(mix);
        return rig;
    }

    private static void ok(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                           final String statement) {
        final IqlEngine.Outcome outcome = run(helper, mainframe, statement);
        helper.assertTrue(outcome.ok(), statement + " is taken; it said " + outcome.said());
    }

    private static void refused(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                final String statement) {
        helper.assertFalse(run(helper, mainframe, statement).ok(), statement + " is refused");
    }

    private static IqlEngine.Outcome run(final GameTestHelper helper, final MainframeBlockEntity mainframe,
                                         final String statement) {
        return mainframe.networkOperations().query(IqlEngine.viewOf(new ServerCliComputer(mainframe,
                helper.getLevel())), statement, QUERY_ROWS);
    }
}
