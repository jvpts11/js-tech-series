/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.MonitorBlock;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.machine.PlayerMemory;
import dev.jstech.computers.machine.PlayerSystemBoot;
import dev.jstech.computers.machine.SchedulePolicy;
import dev.jstech.computers.operation.payload.FirmwareStatePayload;
import dev.jstech.computers.operation.payload.firmware.FirmwarePayloads;
import dev.jstech.computers.operation.payload.firmware.NetworkBoot;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.boot.SystemIntegrity;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.os.player.PlayerDesktop;
import dev.jstech.computers.os.player.PlayerSystems;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TerminalHand;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Operating systems and desktops that players write (computers/docs/design/player-systems.md): built at a prompt,
 * published to the Mirror, offered by the firmware through the network, installed and booted as process 1 with the
 * desktop they start, brought down on their own page, and run by the kernel they chose.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class PlayerSystemsGameTests {

    private static final String BENCH = "bench";
    private static final int TIMEOUT = 2_400;
    private static final int TRANSCRIPT = 1_200;

    private static final String DESKTOP = """
            using System.OS.*;
            namespace Lantern;

            class LanternDesktop : IDesktop {
                public string Name => "lantern-desktop";
                public string Version => "1.2";
                public string Author => "Alex";
                public string Menu => "Lantern";
                public string PanelAt => "top";
                public string Frames => "flat";
                public string DesktopColour => "#241733";
                public string PanelColour => "#2E1F44";
                public string TextColour => "#F4E9D8";
                public string AccentColour => "#FFD27A";
                public string WindowColour => "#F6EEE3";
                public string TitleColour => "#FFD27A";
                public string Icons => "lantern";
                public string Wallpaper => "lantern/sky.pix";
                public string Programs => "";
            }
            """;

    private PlayerSystemsGameTests() {
    }

    /** Built at a prompt, the system and its desktop go to the Mirror, which offers the system through the network. */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void build_publishesTheSystemAndTheFirmwareOffersIt(final GameTestHelper helper) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), EraKit.STANDARD);
        final PersonalComputerBlockEntity computer = network.computer();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && computer.networkUuid() != null,
                        "the computer is up on the network"))
                .thenExecute(() -> {
                    network.mainframe().installMirror();
                    final TerminalHand hand = build(helper, computer, "1.2",
                            system("1.2", "Desktop.Start(\"lantern-desktop\");", "Kernel.Of(\"linux\")", ""));
                    final String published = hand.type("sgpack --publish lantern");
                    helper.assertTrue(published.contains("Published to the Mirror"),
                            "the system is published; got " + hand.transcript(TRANSCRIPT));
                    helper.assertTrue(network.mainframe().shelvedPackage("lantern-os") != null,
                            "the Mirror shelves the system");
                    helper.assertTrue(network.mainframe().shelvedPackage("lantern-desktop") != null,
                            "the Mirror shelves the desktop");
                    final NetworkBoot.Listing listing = NetworkBoot.listing(helper.getLevel(), computer);
                    helper.assertTrue(listing != null && listing.offers().stream()
                                    .anyMatch(offer -> offer.community() && offer.name().contains("Lantern OS")),
                            "the firmware offers the player's system, marked community");
                })
                .thenSucceed();
    }

    /** With no Mirror on the network, the Legacy firmware still has its entry and says no Mirror answered. */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void networkBoot_findsNoMirrorWhereNoneServes(final GameTestHelper helper) {
        final EraNetwork network = EraNetwork.lay(TestWorldBuilder.forGameTest(helper), EraKit.LEGACY);
        final PersonalComputerBlockEntity computer = network.computer();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && computer.networkUuid() != null,
                        "the computer is up on the network"))
                .thenExecute(() -> {
                    final FirmwareStatePayload state = FirmwarePayloads.buildFirmwareState(helper.getLevel(),
                            computer, computer.getBlockPos());
                    helper.assertTrue(state.network() != null && state.network().label().english()
                            .contains("PXE"), "the Legacy firmware has Network (PXE)");
                    helper.assertTrue(NetworkBoot.listing(helper.getLevel(), computer) == null,
                            "no Mirror answers on a network whose Mainframe serves none");
                })
                .thenSucceed();
    }

    /** Installed and started, the system runs as process 1, comes up, and brings the desktop it starts. */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void system_bootsAsProcessOneWithItsDesktop(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final EraNetwork network = EraNetwork.lay(world, EraKit.STANDARD);
        final PersonalComputerBlockEntity second = EraNetwork.secondComputer(world, EraKit.STANDARD);
        final PersonalComputerBlockEntity computer = network.computer();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && second.isRunning(),
                        "both computers are up"))
                .thenExecute(() -> install(helper, computer, second, "1.3",
                        system("1.3", "Desktop.Start(\"lantern-desktop\");", "Kernel.Of(\"linux\")", "")))
                .thenWaitUntil(() -> helper.assertTrue(second.isRunning() && second.systemRun() != null
                        && second.systemRun().done(), "the player's system comes up"))
                // Its boot starts the desktop after it says it is up, as Lantern OS does.
                .thenWaitUntil(() -> helper.assertTrue(
                        PlayerDesktop.idOf("lantern-desktop").equals(second.bootedDesktopId()),
                        "the desktop it started comes up; got " + second.bootedDesktopId() + ", " + state(second)))
                .thenExecute(() -> {
                    helper.assertTrue(second.programs() != null
                                    && second.programs().byId(MachinePrograms.SYSTEM_PID) != null,
                            "the system runs as process 1");
                    helper.assertTrue(second.console().communityProgram("lantern-desktop") != null,
                            "the system brought its desktop as a package");
                })
                .thenSucceed();
    }

    /** A system that halts brings the machine down on its own page, with the restart it asked for. */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void system_goesDownOnItsOwnPage(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final EraNetwork network = EraNetwork.lay(world, EraKit.STANDARD);
        final PersonalComputerBlockEntity second = EraNetwork.secondComputer(world, EraKit.STANDARD);
        final PersonalComputerBlockEntity computer = network.computer();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && second.isRunning(),
                        "both computers are up"))
                .thenExecute(() -> install(helper, computer, second, "1.4", system("1.4",
                        "Panic.RebootAfter(20); Panic.Halt(\"The lantern went out.\");", "Kernel.Of(\"linux\")", "")))
                .thenWaitUntil(() -> helper.assertTrue(second.crashPage() != null, "the system goes down"))
                .thenExecute(() -> {
                    final CrashPage page = second.crashPage();
                    helper.assertTrue(page.player() != null, "the page is the system's own");
                    helper.assertTrue(page.player().message().contains("The lantern went out."),
                            "with its message; got " + page.player().message() + " " + page.player().trace());
                    helper.assertValueEqual(page.player().rebootSeconds(), 20, "the restart it asked for");
                })
                .thenSucceed();
    }

    /** A kernel of its own schedules in turn as it chose, and its OnOutOfMemory refuses what it refuses. */
    @GameTest(template = BENCH, timeoutTicks = TIMEOUT)
    public static void ownKernel_schedulesAndRefusesAsItChose(final GameTestHelper helper) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        final EraNetwork network = EraNetwork.lay(world, EraKit.STANDARD);
        final PersonalComputerBlockEntity second = EraNetwork.secondComputer(world, EraKit.STANDARD);
        final PersonalComputerBlockEntity computer = network.computer();
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(computer.isRunning() && second.isRunning(),
                        "both computers are up"))
                .thenExecute(() -> install(helper, computer, second, "1.5", system("1.5", "",
                        "Kernel.Own().Scheduler(\"cooperative\").MemoryRule(\"end\")",
                        "public int OnOutOfMemory(string asker, int mb) { return Memory.Refuse; }")))
                .thenWaitUntil(() -> helper.assertTrue(second.isRunning() && second.systemRun() != null
                        && second.systemRun().done(), "the player's system comes up; " + state(second)))
                .thenExecute(() -> {
                    helper.assertValueEqual(PlayerSystemBoot.policy(second).kind(),
                            SchedulePolicy.Kind.COOPERATIVE, "the scheduler it chose");
                    helper.assertValueEqual(PlayerMemory.rule(second), "end", "the memory rule it chose");
                    helper.assertValueEqual(PlayerMemory.whenShort(second, "big", 1_000_000, "end"),
                            PlayerMemory.Verdict.REFUSED, "its OnOutOfMemory refuses");
                })
                .thenSucceed();
    }

    /*
     * A system of Alex's, Lantern OS at {@code version}, whose boot does {@code boot} on {@code kernel}, with
     * {@code more} in its class. Each test builds a version of its own, since the world keeps one system per version
     * and the tests run side by side.
     */
    private static String system(final String version, final String boot, final String kernel, final String more) {
        return """
                using System.OS.*;
                namespace Lantern;

                class LanternOs : ISystem {
                    public string Name => "Lantern OS";
                    public string Version => "%s";
                    public string Author => "Alex";
                    public Era Era => Era.Standard;
                    public Isa Isa => Isa.X86_64;
                    public int MemoryMb => 48;
                    public Kernel Kernel => %s;

                    public void OnBoot() {
                        Boot.Line("Lighting the lantern...");
                        Boot.Progress(100);
                        Boot.Done();
                        %s
                    }
                    public void OnTick() { }
                    public void OnInput(InputEvent e) { }
                    public void OnShutdown() { }
                    %s
                }
                """.formatted(version, kernel, boot, more);
    }

    /* How a machine running a player's system stands, for a failure to say. */
    private static String state(final PersonalComputerBlockEntity machine) {
        final CrashPage page = machine.crashPage();
        final MachinePrograms programs = machine.programs();
        final String system = programs == null || programs.byId(MachinePrograms.SYSTEM_PID) == null ? "no process 1"
                : "process 1 " + programs.byId(MachinePrograms.SYSTEM_PID).process().state() + " "
                        + programs.byId(MachinePrograms.SYSTEM_PID).process().message();
        final String page1 = page == null ? "none" : page.player() == null ? String.valueOf(page.style())
                : page.player().message() + " " + page.player().trace();
        return "running " + machine.isRunning() + " at " + MonitorBlock.entryFor(machine, false) + " (integrity "
                + SystemIntegrity.check(machine).state() + "), " + system + ", os " + machine.installedOsId()
                + ", page " + page1;
    }

    /* Writes the system and its desktop into "lantern" on the computer, and builds both at its prompt. */
    private static TerminalHand build(final GameTestHelper helper, final PersonalComputerBlockEntity computer,
                                      final String version, final String system) {
        for (final String id : new String[] {"sgsc", "sigma"}) {
            computer.console().install(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, id).toString());
        }
        final FilesystemKind kind = FilesystemKind.HIERARCHICAL;
        DiskFilesystem.write(computer.systemDisk(), "lantern/LanternOs.sgs", FileType.SGS, system, Long.MAX_VALUE,
                kind);
        DiskFilesystem.write(computer.systemDisk(), "lantern/LanternDesktop.sgs", FileType.SGS, DESKTOP,
                Long.MAX_VALUE, kind);
        DiskFilesystem.write(computer.systemDisk(), "lantern/sky.pix", FileType.PIX, new PixImage(1, 1).encode(),
                Long.MAX_VALUE, kind);
        final TerminalHand hand = new TerminalHand(computer, helper.getLevel());
        hand.type("cd \\");
        hand.type("cd /");
        final String built = hand.type("sigma build --os lantern");
        helper.assertTrue(built.contains("Lantern OS " + version + " by Alex"), "the system builds; got "
                + hand.transcript(TRANSCRIPT));
        final String desktop = hand.type("sigma build --desktop lantern-desktop");
        helper.assertTrue(desktop.contains("lantern-desktop 1.2"), "the desktop builds; got "
                + hand.transcript(TRANSCRIPT));
        helper.assertTrue(PlayerSystems.withFiles(idOf(version)) != null, "the world keeps the system");
        return hand;
    }

    /* Builds the system on {@code computer} and installs it on {@code target}, which starts on it. */
    private static void install(final GameTestHelper helper, final PersonalComputerBlockEntity computer,
                                final PersonalComputerBlockEntity target, final String version,
                                final String system) {
        build(helper, computer, version, system);
        target.togglePower();
        target.formatDisk(0);
        helper.assertTrue(target.installOs(idOf(version)), "the player's system installs");
        target.togglePower();
    }

    /* The id the world keeps Alex's Lantern OS at that version under. */
    private static ResourceLocation idOf(final String version) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "player/alex/lantern-os/" + version);
    }
}
