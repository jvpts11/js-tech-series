/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import com.mojang.logging.LogUtils;
import dev.jstech.core.JsCore;
import dev.jstech.core.registry.StableNamespaces;
import dev.jstech.tests.testkit.ChunkLoadWatch;
import dev.jstech.tests.testkit.TestSettings;
import dev.jstech.tests.testkit.TestShowcaseSettings;
import dev.jstech.tests.testkit.TestStates;
import dev.jstech.tests.testkit.ToyLanguage;
import dev.jstech.tests.testkit.computers.TestComponents;
import dev.jstech.tests.testkit.computers.TestDesktopPrograms;
import dev.jstech.tests.testkit.computers.TestEngines;
import dev.jstech.tests.testkit.computers.TestFetchCommand;
import dev.jstech.tests.testkit.computers.TestPlanner;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.gametest.GameTestHooks;
import org.slf4j.Logger;

/**
 * The test mod of the J's Tech Series: a development tool, never published and never to be installed. It
 * carries the GameTests, the client tests and the test kit for every mod, so a test may span several mods
 * and no shipped jar holds test code. It adds nothing to the game.
 */
@Mod(JsTests.MODID)
public final class JsTests {

    public static final String MODID = "jstests";

    public static final Logger LOGGER = LogUtils.getLogger();

    public JsTests(final IEventBus modEventBus, final ModContainer modContainer) {
        LOGGER.warn("J's Tech Series Tests {} loaded. This is a development-only test mod: it is not part of the"
                + " series, adds nothing to the game and must not be installed.", modContainer.getModInfo().getVersion());
        // The test mod's data is no experiment, as a mod of the series' is: its moon opens a world with no warning.
        StableNamespaces.declare(MODID);
        // The sounds the tests play through the series' sound system, on files the game already has.
        TestBlocks.declare();
        // A thruster, a wheel, a burner, a light block, a levitator and a magnet, to prove the structures' physics.
        TestPhysics.declare();
        // A decoupler, a clamp, tools and seats, for what moving structures do aboard.
        TestAboard.declare();
        // A kiln and a mixer that work in a few ticks, for the autocraft tests to run real recipes through.
        TestMachines.declare();
        // A furnace of several blocks whose casing has ports, to prove the Core's multiblocks.
        TestMultiblocks.declare();
        // A vein and a structure that generate nowhere, and a small moon, to prove the Core's world generation.
        TestWorldGen.declare();
        // A press of two inputs, two outputs, two tanks and an upgrade slot, to prove the Core's machines.
        TestPress.declare();
        // A transmitter, a receiver, a relay and a light sensor, to prove the Core's redstone and signals.
        TestRadios.declare();
        // A machine on the whole machine kit, with energy of a class and tanks of gas, and a repair kit.
        TestKitMachine.declare();
        // Upgrades of every effect, some with a limit of how many fit in a machine.
        TestUpgrades.declare();
        // Litres of copper and iron, a radiator and a heater, to prove the Core's heat.
        TestHeat.declare();
        // A cold store whose places a test chills, to prove what deep cold does.
        TestClimate.declare();
        // A machine that makes heat and a thermocouple, to prove the machines' heat and its readings.
        TestHotMachine.declare();
        // Shafts, gears, a gearbox, a crank, a load, a motor and a dynamo, to prove the Core's mechanical power.
        TestMechanical.declare();
        // A tank of any size the player builds block by block, to prove multiblocks that form in the world.
        TestStructures.declare();
        // A robot and a bolt, to prove the Core's robots and projectiles.
        TestEntities.declare();
        // A van, a trailer, a drone, a boat, their parts and fuels and a fuel pump, to prove the Core's vehicles.
        TestVehicles.declare(modEventBus);
        // Items that hold everything an item can, to prove the Core's items with state.
        TestItems.declare();
        // Two fuses, to prove the Core's protection slot.
        TestFuses.declare();
        // A kind of part declared the Core's way, its item and its plate's model.
        TestParts.declare();
        // Conveyors of every drive and end, to prove the Core's conveyors.
        TestConveyors.declare();
        // A reactor the network reads and an accelerator it gives work to, members through the Core alone.
        TestNetworkMembers.declare(modEventBus);
        // An era between two of the series', an axis, milestones and knowledge, to prove the Core's progression.
        TestProgression.declare();
        // A corrosive liquid and a hot gas, to prove the Core's fluids and pipes.
        TestFluids.declare();
        // Materials in every form and ores in several rocks, to prove the Core's catalogue and the art it draws.
        TestMaterials.declare();
        // Large values sent either way, in pieces.
        TestBigPayloads.declare();
        // A registry datapacks fill and notes read from datapack files, both sent to the players.
        TestData.declare();
        // A chapter and a manual, with an entry of every kind of block, to prove the Core's manuals.
        TestGuide.declare();
        // A press, a crate, a walker and a helmet drawn with the Core's models, from files of four formats.
        TestModels.declare();
        // Blocks a resource pack in the test mod draws with connected textures, in both formats packs write.
        TestConnected.declare();
        // An alarm lamp, a powered lamp, a floodlight, a beam and a lamp that breaks, to prove the lighting kit.
        TestLights.declare();
        // A landmark, a layer of map markers, a lore and a seal that opens only to a team that knows it.
        TestPlaces.declare();
        // Generation for old chunks, changes of the surface, a storm and fields over the chunks.
        TestWorldChanges.declare();
        // An acid rain and a storm of lightning, the weathers the sky kit is tried with.
        TestSkies.declare();
        // A hazard of the test mod's own and a contamination of the land, to prove the Core's hazard kit.
        TestHazards.declare();
        TestSounds.CONTENT.register(modEventBus);
        // A cable that never shares a block, to show the shared block refusing it company.
        TestCableTypes.register(modEventBus);
        // An energy of the test mod's own with four classes and a unit, to prove the Core's energy kit.
        TestEnergies.register(modEventBus);
        // A state of every scope, on both sides, since the client tests watch them arrive.
        TestStates.register();
        // Two kinds of component for programs' windows, the way another mod adds them, on both sides.
        TestComponents.register(modEventBus);
        // A capability, a part offering it and a screen's call of the test mod's own, to prove the Core's toolkit.
        TestToolkit.register(modEventBus);
        // A menu whose screen is declared in a file, to prove screens as data, values bound by name and styles.
        TestScreens.register(modEventBus);
        // A player's settings with every kind of control, which the settings screen's tests open.
        TestShowcaseSettings.register(modEventBus, modContainer);
        if (GameTestHooks.isGametestServer()) {
            /*
             * The language API's tests need a language that is not the series' own, and languages are only taken
             * while the game loads; a client run stays as a player sees it.
             */
            JsCore.languages().register(new ToyLanguage());
            // A world's settings file in every format, where the settings tests can read and write them.
            TestSettings.register(modEventBus, modContainer);
        }
        if (GameTestHooks.isGametestServer() || Boolean.getBoolean("jsc.clienttests")) {
            /*
             * An engine another mod brings, so a Mainframe can be swapped onto one that is not the series' own: on
             * the GameTest server, and in the client tests that replace an engine from the Network Manager.
             */
            TestEngines.register(modEventBus);
            // A rule, a hint, a statistic and a note added to NextgreIQL's planner the way another mod adds them.
            TestPlanner.register(modEventBus);
            // A command that holds the terminal for a while, added the way another mod adds one.
            TestFetchCommand.register();
            // A program with a window of its own that draws a picture, the way another mod adds one.
            TestDesktopPrograms.register(modEventBus);
            // Any chunk the series' code loads by reading it is reported with the line that read it.
            ChunkLoadWatch.register();
        }
    }
}
