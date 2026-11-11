/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.RedstoneInterfaceBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.PsuConditions;
import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.core.signal.RadioBlockEntity;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.TestRadios;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * A program reading its own machine through System.Machine on a real computer: the draw and the supply, the heat and
 * the Wi-Fi it reads are the ones the machine keeps for its Task Manager.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SigmaMachineGameTests {

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;
    private static final BlockPos AT = new BlockPos(2, 2, 2);
    /** The namespaces a script brings in, on one line so its source keeps its line numbers. */
    private static final String PRELUDE = "using System.*; using System.IO.*; using System.Machine.*; "
            + "namespace Programs; ";
    private static final BlockPos MAINFRAME = new BlockPos(1, 2, 2);
    /** A Redstone Interface against the Mainframe's east face. */
    private static final BlockPos INTERFACE = new BlockPos(2, 2, 2);
    /** Long enough for an interface to link to its machine. */
    private static final int LINKED = 10;
    /** A program that keeps a setting and reads it, one it never set, and where they are kept. */
    private static final String SETTINGS_WRITER = """
            class Reader {
                static void Main() {
                    Console.PrintLine(Settings.Set("speed", "fast") ? "True" : "False");
                    Console.PrintLine(Settings.Get("speed", "none"));
                    Console.PrintLine(Settings.Get("other", "none"));
                    Console.PrintLine(Settings.Path);
                }
            }
            """;
    /** The same program, run again, reading what it kept. */
    private static final String SETTINGS_READER = """
            class Reader {
                static void Main() {
                    Console.PrintLine(Settings.Get("speed", "none"));
                }
            }
            """;

    private SigmaMachineGameTests() {
    }

    @GameTest(template = ARENA)
    public static void power_aProgramReadsTheDrawAndTheSupplyOfItsMachine(final GameTestHelper helper) {
        // A machine switched on, so it draws: the draw of one that is off is nothing.
        final PersonalComputerBlockEntity computer =
                TestWorldBuilder.forGameTest(helper).placeRunningPersonalComputer(AT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = run(helper, computer, """
                            class Reader {
                                static void Main() {
                                    Console.PrintLine("" + Power.DrawWatts);
                                    SupplyInfo supply = Power.Supply;
                                    Console.PrintLine(supply.RatingWatts + (supply.Failed ? " failed" : " working"));
                                    Console.PrintLine(Power.Ups == null ? "no UPS" : "a UPS");
                                }
                            }
                            """);
                    final int draw = computer.isRunning() ? computer.lineWatts() : 0;
                    helper.assertTrue(draw > 0, "the machine runs and draws; it draws " + draw + " W");
                    helper.assertValueEqual(said, List.of(String.valueOf(draw), computer.psuWatts() + " working",
                            "no UPS"), "what the program read");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void thermal_aProgramReadsWhatItsMachineKnowsOfItsHeat(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = run(helper, computer, """
                            class Reader {
                                static void Main() {
                                    Console.PrintLine(Thermal.Known ? "known" : "unknown");
                                    Console.PrintLine(Thermal.Celsius + " " + Thermal.ClockPercent + " "
                                            + Thermal.BudgetWatts + (Thermal.Throttled ? " throttled" : " full"));
                                }
                            }
                            """);
                    final ThermalState heat = computer.thermal();
                    helper.assertValueEqual(said, List.of(heat.known() ? "known" : "unknown",
                            heat.celsius() + " " + heat.clockPercent() + " " + heat.budgetWatts()
                                    + (heat.throttled() ? " throttled" : " full")), "what the program read");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void wireless_saysSoOnAMachineWithNoCard(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = run(helper, computer, """
                            class Reader {
                                static void Main() {
                                    Console.PrintLine(Wireless.Joined ? "joined" : "not joined");
                                    Console.PrintLine("[" + Wireless.Standard + "][" + Wireless.AccessPoint + "] "
                                            + Wireless.Speed);
                                }
                            }
                            """);
                    helper.assertValueEqual(said, List.of("not joined", "[][] 0"), "what the program read");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void power_aServerInARackReadsItsOwnSupply(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = TestWorldBuilder.forGameTest(helper).placeSeededRack(AT);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertTrue(PsuConditions.spec(rack.unitHost(0).psuStack()) != null,
                            "the seated server's supply is the one it carries");
                    helper.assertTrue(rack.unitHost(ServerRackBlockEntity.CAPACITY_U - 1).psuStack().isEmpty(),
                            "an empty bay has no supply");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void disks_aProgramReadsTheHealthOfItsDrives(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = run(helper, computer, """
                            class Reader {
                                static void Main() {
                                    DiskHealth c = DiskHealth.Of("c");
                                    Console.PrintLine(c.Drive + (c.Dead ? " dead " : " alive ") + c.DamagePercent + " "
                                            + c.BadSectorsPercent + " " + c.CorruptedItems + " " + c.LastDefragTick);
                                    Console.PrintLine(DiskHealth.Of("Q") == null ? "no Q" : "a Q");
                                    foreach (DiskHealth disk in DiskHealth.All()) {
                                        Console.PrintLine("drive " + disk.Drive);
                                    }
                                }
                            }
                            """);
                    helper.assertTrue(said.size() >= 3 && said.subList(0, 3).equals(List.of("C alive 0 0 0 -1",
                            "no Q", "drive C")), "what the program read: " + said);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void settings_framesKeepsThemInItsRegistry(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertValueEqual(run(helper, computer, SETTINGS_WRITER), List.of("True", "fast", "none",
                            "C:\\Frames\\System32\\config\\SOFTWARE"), "what the program read");
                    final String registry =
                            DiskFilesystem.read(computer.systemDisk(), "Frames/System32/config/SOFTWARE").orElse("");
                    helper.assertTrue(registry.startsWith("REGEDIT4\n") && registry.contains(
                                    "[HKEY_LOCAL_MACHINE\\SOFTWARE\\Reader]\n\"speed\"=\"fast\"\n"),
                            "the registry holds the setting under the program's key: " + registry);
                    helper.assertValueEqual(run(helper, computer, SETTINGS_READER), List.of("fast"),
                            "the next run reads it back");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void settings_linuxKeepsThemInTheHomesConfig(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = TestWorldBuilder.forGameTest(helper).placeCraftingComputer(AT,
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "debian"));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    helper.assertValueEqual(run(helper, computer, SETTINGS_WRITER), List.of("True", "fast", "none",
                            "/home/player/.config/Reader/settings"), "what the program read");
                    helper.assertValueEqual(DiskFilesystem.read(computer.systemDisk(),
                            "home/player/.config/Reader/settings").orElse(""), "speed=fast\n", "the file it wrote");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void sensors_aProgramReadsTheSensorInFrontOfAnInterface(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(MAINFRAME);
        placeInterface(helper);
        helper.setBlock(INTERFACE.east(), TestRadios.SENSOR.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> interfaceAt(helper).rename("Eye"))
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = run(helper, mainframe, """
                            class Reader {
                                static void Main() {
                                    foreach (SensorReading reading in Sensors.Read("eye")) {
                                        Console.PrintLine(reading.Name + " " + reading.Least + " " + reading.Most);
                                    }
                                    Console.PrintLine(Sensors.Read("Nobody") == null ? "no Nobody" : "a Nobody");
                                    foreach (string name in Sensors.Names()) {
                                        Console.PrintLine("named " + name);
                                    }
                                }
                            }
                            """);
                    helper.assertTrue(said.size() == 4 && said.get(0).startsWith("Sky light ")
                                    && said.get(1).startsWith("Height ") && said.get(2).equals("no Nobody")
                                    && said.get(3).equals("named Eye"),
                            "it read the sensor in front of the interface; it said " + said);
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 100)
    public static void signals_aProgramSendsThroughATransmitterAndHearsIt(final GameTestHelper helper) {
        final MainframeBlockEntity mainframe = TestWorldBuilder.forGameTest(helper).placeRunningMainframe(MAINFRAME);
        placeInterface(helper);
        helper.setBlock(INTERFACE.east(), TestRadios.TRANSMITTER.get());
        helper.startSequence()
                .thenExecuteAfter(LINKED, () -> interfaceAt(helper).rename("Gate"))
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> said = run(helper, mainframe, """
                            class Sender {
                                static void Main() {
                                    Console.PrintLine(Signals.Send("gate", 9) ? "sent" : "not sent");
                                    RadioInfo radio = Signals.Radio("Gate");
                                    Console.PrintLine(radio.Role + " " + radio.Band + " " + radio.Frequency);
                                    Console.PrintLine(Signals.Radio("Nobody") == null ? "no Nobody" : "a Nobody");
                                }
                            }
                            """);
                    helper.assertValueEqual(said, List.of("sent", "transmitter private 1", "no Nobody"),
                            "what the program said");
                    helper.assertTrue(interfaceAt(helper).emits() && interfaceAt(helper).emitStrength() == 9,
                            "the interface emits what was sent");
                })
                .thenWaitUntil(() -> helper.assertValueEqual(helper.getBlockEntity(INTERFACE.east())
                        instanceof RadioBlockEntity radio ? radio.value() : -1, 9, "the transmitter sends it"))
                .thenExecuteAfter(SETTLE, () -> {
                    final List<String> heard = run(helper, mainframe, """
                            class Listener {
                                static void Main() {
                                    foreach (HeardSignal signal in Signals.Heard()) {
                                        Console.PrintLine(signal.Frequency + " " + signal.Value);
                                    }
                                }
                            }
                            """);
                    helper.assertTrue(heard.contains("1 9"), "the machine hears it; it heard " + heard);
                })
                .thenSucceed();
    }

    /* A Redstone Interface against the Mainframe's east face, its lens facing east. */
    private static void placeInterface(final GameTestHelper helper) {
        helper.setBlock(INTERFACE, ComputingModule.STANDARD_REDSTONE_INTERFACE.get().defaultBlockState()
                .setValue(RedstoneInterfaceBlock.FACING, Direction.EAST));
    }

    private static RedstoneInterfaceBlockEntity interfaceAt(final GameTestHelper helper) {
        if (!(helper.getBlockEntity(INTERFACE) instanceof RedstoneInterfaceBlockEntity found)) {
            throw new IllegalStateException("no Redstone Interface at " + INTERFACE);
        }
        return found;
    }

    /* A Crafting Computer with Frames XP, the oldest system the language runs on with drives to write to. */
    private static CraftingComputerBlockEntity computer(final GameTestHelper helper) {
        return TestWorldBuilder.forGameTest(helper).placeCraftingComputer(AT,
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "frames_xp"));
    }

    /* Runs {@code source} on {@code computer} to its end and gives back what it printed. */
    private static List<String> run(final GameTestHelper helper, final AbstractComputerBlockEntity computer,
                                    final String source) {
        final SigmaCompiler.Result built = SigmaCompiler.compile(List.of(new SourceFile("Script.sgs",
                PRELUDE + source)));
        helper.assertTrue(built.ok(), "the script compiles: " + String.join("\n", built.lines()));
        final MachinePrograms.Started started = computer.programs().start("reader.asm", built.assembly(), 1,
                computer);
        helper.assertTrue(started.ok(), "the program starts: " + started.message());
        computer.programs().tick(100000);
        return computer.programs().byId(started.id()).process().console();
    }
}
