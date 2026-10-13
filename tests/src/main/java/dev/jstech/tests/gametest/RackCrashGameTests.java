/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.OpenWindow;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.RamLedger;
import dev.jstech.computers.os.SystemProcesses;
import dev.jstech.computers.os.crash.CrashCause;
import dev.jstech.computers.os.crash.CrashPage;
import dev.jstech.computers.os.crash.CrashPages;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.ServerCliComputer;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.ServerStacks;
import dev.jstech.tests.testkit.TestShell;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for the servers in a rack following the rule of their systems when they run short, each on its own: a
 * Frames server goes down on its blue page, a Linux server's kernel ends what holds the most and logs it, ending its
 * init panics it, a service killed stays down until the server starts over, FreeBSD holds its reboot on a key, and the
 * earliest systems refuse. A server in the same rack runs on through all of it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class RackCrashGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos RACK = new BlockPos(2, 2, 2);
    private static final int SETTLE = 4;
    /** The rows of the two servers, with the KVM switch between them. */
    private static final int FIRST = 0;
    private static final int SECOND = 2;
    private static final String CALCULATOR = "jsc:calculator";

    private RackCrashGameTests() {
    }

    /**
     * A Frames server handed more windows than its memory holds goes down on its blue page within a second, its
     * session with it, and starts over once the page has stood its time; the Linux server beside it runs on.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void overcommit_bringsAFramesServerDownAndLeavesItsNeighbourUp(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = twoServers(helper);
        final IOsHost frames = rack.unitHost(FIRST);
        final IOsHost linux = rack.unitHost(SECOND);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(helper, frames, "frames_10");
                    install(helper, linux, "debian");
                })
                .thenWaitUntil(() -> helper.assertTrue(up(frames) && up(linux), "both servers come up"))
                .thenExecute(() -> frames.setOpenWindows(calculators(OpenWindow.MAX)))
                .thenExecuteAfter(25, () -> {
                    final CrashPage page = frames.crashPage();
                    helper.assertTrue(page != null && page.style().frames() && !page.red(),
                            "the Frames server goes down on its blue page; got " + page);
                    helper.assertTrue(frames.goingDown() && frames.openWindows().isEmpty(),
                            "and its session ends with it");
                    helper.assertTrue(linux.crashPage() == null && up(linux), "the server beside it runs on");
                })
                .thenWaitUntil(() -> helper.assertTrue(frames.crashPage() == null && frames.needsPost(),
                        "the page stands its time, and then the server starts over from its self-test"))
                .thenSucceed();
    }

    /**
     * A Linux server handed more windows than its memory holds keeps what fits: its kernel ends the latest, logs each
     * as the real kernel does, and dmesg reads it after the boot; it stays up, with the switch turned to the other.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void overcommit_linuxServerEndsTheLatestWindowsAndDmesgReadsIt(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = twoServers(helper);
        final IOsHost frames = rack.unitHost(FIRST);
        final IOsHost linux = rack.unitHost(SECOND);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(helper, frames, "frames_10");
                    install(helper, linux, "debian");
                })
                .thenWaitUntil(() -> helper.assertTrue(up(frames) && up(linux), "both servers come up"))
                .thenExecute(() -> {
                    helper.assertTrue(rack.shownRow() == FIRST, "the switch shows the other server");
                    linux.setOpenWindows(calculators(OpenWindow.MAX));
                })
                .thenExecuteAfter(25, () -> {
                    final RamLedger ledger = linux.ramLedger();
                    helper.assertTrue(ledger.usedMb() <= ledger.totalMb(), "what is left fits; holding "
                            + ledger.usedMb() + " of " + ledger.totalMb() + " MB");
                    final List<OpenWindow> left = linux.openWindows();
                    helper.assertTrue(!left.isEmpty() && left.size() < OpenWindow.MAX,
                            "some windows went and some stayed; " + left.size() + " stayed");
                    final List<String> log = linux.memoryLog();
                    helper.assertTrue(log.size() == OpenWindow.MAX - left.size()
                                    && log.getFirst().contains("Out of memory: Killed process ")
                                    && log.getFirst().contains(" (calc) "),
                            "one line in the kernel's words for each window ended; got " + log);
                    helper.assertTrue(linux.crashPage() == null && up(linux), "and the server stays up");
                    final List<String> dmesg = rack.asUnit(SECOND,
                            () -> new ServerCliComputer(rack, helper.getLevel()).kernelLog());
                    helper.assertTrue(dmesg.getFirst().matches("\\[ +0\\.000000] Linux version .*")
                                    && dmesg.getLast().contains("Out of memory: Killed process"),
                            "dmesg reads the boot, then what was ended; got " + dmesg);
                    helper.assertTrue(frames.memoryLog().isEmpty() && frames.crashPage() == null,
                            "the other server is untouched");
                })
                .thenSucceed();
    }

    /**
     * At a Linux server's prompt ps lists its init at number 1, and kill -9 1 panics that server, saying so with the
     * line that did it; the server beside it runs on.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void kill_nineToInitPanicsTheServerItWasTypedAt(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = twoServers(helper);
        final IOsHost frames = rack.unitHost(FIRST);
        final IOsHost linux = rack.unitHost(SECOND);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    install(helper, frames, "frames_10");
                    install(helper, linux, "debian");
                })
                .thenWaitUntil(() -> helper.assertTrue(up(frames) && up(linux), "both servers come up"))
                .thenExecute(() -> {
                    rack.setActiveChannel(SECOND);
                    final List<String> ps = TestShell.shell(helper, rack, "ps");
                    helper.assertTrue(ps.size() >= 2 && ps.get(1).matches("1\\s+systemd\\s+/sbin/init\\s+running.*"),
                            "the server's first process is its init; got " + ps);
                    TestShell.shell(helper, rack, "kill -9 1");
                    final CrashPage page = linux.crashPage();
                    helper.assertTrue(page != null && page.lines().stream().anyMatch(line ->
                                    line.endsWith("Kernel panic - not syncing: Attempted to kill init!"
                                            + " exitcode=0x00000009")),
                            "the server panics on its init killed; got " + page);
                    helper.assertTrue(page.lines().getFirst().endsWith("$ kill -9 1"),
                            "with the line that did it at the top; got " + page.lines().getFirst());
                    helper.assertTrue(frames.crashPage() == null && up(frames), "the server beside it runs on");
                })
                .thenWaitUntil(() -> helper.assertTrue(linux.crashPage() == null && linux.needsPost(),
                        "the page stands its time, and then the server starts over"))
                .thenSucceed();
    }

    /**
     * A service's process killed at a server's prompt ends the service until the server starts over: the rack no
     * longer finds it on that server, though it is still installed, and a restart brings it back.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void kill_aServicesProcessEndsItUntilTheServerStartsOver(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = oneServer(helper);
        final IOsHost linux = rack.unitHost(FIRST);
        final ResourceLocation messenger = Programs.MESSENGER_SERVICE;
        final int pid = SystemProcesses.servicePid(OsRegistry.serviceIndex(messenger));
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> install(helper, linux, "debian"))
                .thenWaitUntil(() -> helper.assertTrue(up(linux), "the server comes up"))
                .thenExecute(() -> {
                    rack.consoleOf(FIRST).install(messenger.toString());
                    helper.assertTrue(rack.serviceSlot(messenger) == FIRST, "the rack finds the service running");
                    final List<String> ps = TestShell.shell(helper, rack, "ps");
                    helper.assertTrue(ps.stream().anyMatch(line -> line.startsWith(pid + " ")),
                            "ps lists the service at its number " + pid + "; got " + ps);
                    final List<String> said = TestShell.shell(helper, rack, "kill " + pid);
                    helper.assertTrue(TestShell.says(said, "stopped " + pid), "kill says so; got " + said);
                    helper.assertTrue(rack.serviceSlot(messenger) < 0 && !rack.serviceUp(FIRST, messenger)
                                    && rack.hasService(FIRST, messenger),
                            "the service is down, though still installed");
                    linux.restart();
                })
                .thenWaitUntil(() -> helper.assertTrue(up(linux), "the server comes back"))
                .thenExecute(() -> helper.assertTrue(rack.serviceSlot(messenger) == FIRST,
                        "a server that started over runs its services again"))
                .thenSucceed();
    }

    /**
     * A FreeBSD server's panic offers to stop its automatic reboot from the rack's console: a key stops it and the
     * server waits, and the next key reboots it.
     */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void crash_aFreeBsdServerHoldsItsRebootOnAKey(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = oneServer(helper);
        final IOsHost bsd = rack.unitHost(FIRST);
        final int[] total = new int[1];
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> install(helper, bsd, "freebsd"))
                .thenWaitUntil(() -> helper.assertTrue(up(bsd), "the server comes up"))
                .thenExecute(() -> {
                    helper.assertTrue(rack.crash(CrashCause.INIT_KILLED,
                                    CrashPages.Moment.initKilled("root@freebsd:~ # kill 1", 15)),
                            "killing its first process brings it down");
                    final CrashPage page = rack.crashPage();
                    helper.assertTrue(page != null && page.holdsOnKey()
                                    && page.lines().contains("panic: Going nowhere without my init!"),
                            "on FreeBSD's own panic; got " + page);
                    total[0] = page.totalTicks();
                    rack.crashKey();
                    helper.assertTrue(rack.crashHeld(), "a key stops the reboot");
                })
                .thenIdle(320)
                .thenExecute(() -> helper.assertTrue(bsd.crashPage() != null && bsd.goingDown() && !bsd.needsPost(),
                        "past the " + total[0] + " ticks the page still stands and the server waits"))
                .thenExecute(rack::crashKey)
                .thenWaitUntil(() -> helper.assertTrue(bsd.crashPage() == null && bsd.needsPost(),
                        "the next key reboots it"))
                .thenSucceed();
    }

    /** MC-NET refuses what it has no room for, as it always did: a server on it neither crashes nor ends a thing. */
    @GameTest(template = ARENA, timeoutTicks = 3600)
    public static void memoryShort_anMcNetServerRefuses(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = oneServer(helper);
        final IOsHost net = rack.unitHost(FIRST);
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> install(helper, net, "mc_net"))
                .thenWaitUntil(() -> helper.assertTrue(up(net), "the server comes up"))
                .thenExecute(() -> {
                    helper.assertTrue(!net.memoryShort("SERVER", 1_000_000), "the program is refused");
                    helper.assertTrue(net.crashPage() == null && net.memoryLog().isEmpty() && up(net),
                            "and the server stays as it was");
                    helper.assertTrue(!net.crash(CrashCause.MEMORY, CrashPages.Moment.memory("SERVER", 0, List.of())),
                            "MC-NET has no page to go down on");
                })
                .thenSucceed();
    }

    /* A rack with a server in its top row, a drive in that server's bay. */
    private static ServerRackBlockEntity oneServer(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = placeRack(helper);
        rack.getServers().setStackInSlot(FIRST, ServerStacks.defaultServer());
        rack.insertDrive(FIRST, drive());
        return rack;
    }

    /* A rack with two servers, a KVM switch between them so a monitor can address each, and a drive in each bay. */
    private static ServerRackBlockEntity twoServers(final GameTestHelper helper) {
        final ServerRackBlockEntity rack = oneServer(helper);
        rack.getServers().setStackInSlot(FIRST + 1, new ItemStack(ComputingModule.KVM_SWITCH.get()));
        rack.getServers().setStackInSlot(SECOND, ServerStacks.defaultServer());
        rack.insertDrive(SECOND, drive());
        return rack;
    }

    private static ServerRackBlockEntity placeRack(final GameTestHelper helper) {
        helper.setBlock(RACK, ComputingModule.SERVER_RACK.get());
        if (!(helper.getBlockEntity(RACK) instanceof ServerRackBlockEntity rack)) {
            throw new IllegalStateException("no server rack at " + RACK);
        }
        return rack;
    }

    private static ItemStack drive() {
        return new ItemStack(ComputingModule.disk(StorageTier.NVME, DiskSize.TB_1));
    }

    private static void install(final GameTestHelper helper, final IOsHost server, final String system) {
        helper.assertTrue(server.installOs(ResourceLocation.fromNamespaceAndPath("jsc", system)),
                system + " installs on the server's drive");
    }

    /* That many Calculator windows, as a desktop leaves them open on the machine. */
    private static List<OpenWindow> calculators(final int count) {
        final List<OpenWindow> windows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            windows.add(new OpenWindow(CALCULATOR, 10 + i, 10 + i, 120, 140, false, false, "", 1));
        }
        return windows;
    }

    private static boolean up(final IOsHost server) {
        return server.isRunning() && !server.needsPost() && !server.booting() && !server.atBootMenu()
                && server.crashPage() == null;
    }
}
