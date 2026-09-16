/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import com.mojang.authlib.GameProfile;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.vm.program.UiWidgets;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.tests.JsTests;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A Σ# program with a window of its own on a real machine: it opens on a system with a desktop and
 * refuses on one without, what a player does reaches its handlers, the machine has it to send to whoever
 * is looking, and all of it comes back after a save.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SystemUiGameTests {

    private SystemUiGameTests() {
    }

    private static final String ARENA = "empty";
    private static final int SETTLE = 2;

    /** A panel with a label, a button that changes it, and a list it writes to. */
    private static final String PANEL = """
            using System.*;
            using System.UI.*;
            namespace Plant;
            class Panel : IScript {
                Window window;
                Label heat;
                Button scram;
                ListBox log;
                public void OnInit() {
                    heat = new Label("812 K");
                    scram = new Button("SCRAM");
                    scram.OnClick += Scram;
                    log = new ListBox();
                    Row top = new Row();
                    top.Add(heat);
                    top.Add(scram);
                    Column page = new Column();
                    page.Add(top);
                    page.Add(log, 1);
                    window = new Window("Reactor", 240, 150);
                    window.Content = page;
                    window.Show();
                }
                void Scram() { heat.Text = "cold"; log.Add("scram", "now"); }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    /** A canvas that gains a stroke every tick, and a button that wipes it clean. */
    private static final String PAINTER = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Painter : IScript {
                Window window;
                Canvas paper;
                Button wipe;
                public void OnInit() {
                    paper = new Canvas();
                    wipe = new Button("WIPE");
                    wipe.OnClick += Wipe;
                    Column page = new Column();
                    page.Add(wipe);
                    page.Add(paper, 1);
                    window = new Window("Paint", 200, 120);
                    window.Content = page;
                    window.Show();
                    paper.SetPixel(0, 0, 7);
                }
                void Wipe() { paper.Clear(0); }
                public void OnTick() { paper.SetPixel(1, 1, 7); }
                public void OnDestroy() { window.Close(); }
            }
            """;

    private static CraftingComputerBlockEntity computer(final GameTestHelper helper, final BlockPos at,
                                                        final String os) {
        helper.setBlock(at, ComputingModule.CRAFTING_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof CraftingComputerBlockEntity computer)) {
            helper.fail("no computer at " + at);
            return null;
        }
        final ItemStackHandler hw = computer.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(ComputingModule.MOTHERBOARD_ATX_P.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT, new ItemStack(ComputingModule.CPU_ASCENT_965.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT, new ItemStack(ComputingModule.PSU_650G.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
        computer.installOs(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, os));
        return computer;
    }

    private static Values.Obj widgetOf(final Values.Obj window, final String kind) {
        for (final Values.Obj widget : UiWidgets.inside(window)) {
            if (kind.equals(widget.type())) {
                return widget;
            }
        }
        return null;
    }

    /** The window opens on a system with a desktop, and the machine has it whole to send to a player. */
    @GameTest(template = ARENA)
    public static void window_opensOnASystemWithADesktopAndIsThereToSend(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.programs().start("panel.sgs", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    computer.programs().tick(8192);
                    final List<Values.Obj> windows = computer.programs().windowsOf(started.id());
                    helper.assertTrue(windows.size() == 1, "the program has a window; got " + windows.size());
                    final UiWindowPayload payload =
                            UiWindowPayload.of(computer.getBlockPos(), started.id(), windows.getFirst());
                    helper.assertTrue(payload != null && "Reactor".equals(payload.title()),
                            "the window goes over with its name");
                    helper.assertTrue(payload.widgets().size() == 5,
                            "the column, the row and the three widgets; got " + payload.widgets().size());
                    helper.assertTrue(payload.widgets().stream().anyMatch(w -> "Button".equals(w.kind())
                            && "SCRAM".equals(w.text())), "the button goes over with its words");
                })
                .thenSucceed();
    }

    /** The window reaches a second player who opens the desktop later, not only whoever was there first. */
    @GameTest(template = ARENA)
    public static void windows_reachAPlayerWhoOpensAfterAnother(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        final ServerPlayer first = viewer(helper.getLevel(), "first");
        final ServerPlayer second = viewer(helper.getLevel(), "second");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.programs().start("panel.sgs", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(8192);
                    helper.assertTrue(computer.takeWindowsOwed(first).size() == 1,
                            "the player at the desktop is owed the window");
                    helper.assertTrue(computer.takeWindowsOwed(first).isEmpty(),
                            "and is owed nothing once it has gone to them");
                    helper.assertTrue(computer.takeWindowsOwed(second).size() == 1,
                            "while one who opens later is owed it whole, instead of facing an empty desktop");
                })
                .thenSucceed();
    }

    /** A window goes over when something in it has moved, and a tick where nothing did sends nothing. */
    @GameTest(template = ARENA)
    public static void windows_goOverOnlyWhenSomethingInThemMoves(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        final ServerPlayer watcher = viewer(helper.getLevel(), "watcher");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.programs().start("panel.sgs", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(8192);
                    helper.assertTrue(computer.takeWindowsOwed(watcher).size() == 1, "the window goes over once");
                    computer.programs().tick(8192);
                    helper.assertTrue(computer.takeWindowsOwed(watcher).isEmpty(),
                            "and a tick where nothing in it moved owes nothing at all");
                    final Values.Obj window = computer.programs().windowsOf(started.id()).getFirst();
                    final Values.Obj button = widgetOf(window, UiWidgets.BUTTON);
                    helper.assertTrue(computer.programs().deliverUiEvent(started.id(), 1L,
                            (Long) button.get(UiWidgets.ID), "click", List.of()), "the click is taken");
                    computer.programs().tick(8192);
                    helper.assertTrue(computer.takeWindowsOwed(watcher).size() == 1,
                            "while a click that changed what it shows owes it again");
                })
                .thenSucceed();
    }

    /** A canvas sends only what has been drawn on it since it last went over, until somebody clears it. */
    @GameTest(template = ARENA)
    public static void canvas_sendsOnlyWhatWasDrawnSinceItLastWent(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        final ServerPlayer watcher = viewer(helper.getLevel(), "painter");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started =
                            computer.programs().start("paint.sgs", PAINTER, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(8192);
                    final UiWindowPayload.Widget first = canvasOf(computer.takeWindowsOwed(watcher));
                    helper.assertTrue(first != null, "the window has a canvas");
                    helper.assertFalse(first.appends(), "the first one goes whole");
                    helper.assertTrue(!first.drawing().isEmpty(),
                            "carrying what has been drawn so far; got " + first.drawing().size());

                    computer.programs().tick(8192);
                    final UiWindowPayload.Widget next = canvasOf(computer.takeWindowsOwed(watcher));
                    helper.assertTrue(next != null && next.appends(), "the next one only adds to it");
                    helper.assertTrue(next.drawing().size() == 1,
                            "one stroke, the one drawn since; got " + next.drawing().size());

                    final Values.Obj window = computer.programs().windowsOf(started.id()).getFirst();
                    final Values.Obj button = widgetOf(window, UiWidgets.BUTTON);
                    helper.assertTrue(computer.programs().deliverUiEvent(started.id(), 1L,
                            (Long) button.get(UiWidgets.ID), "click", List.of()), "the wipe is taken");
                    computer.programs().tick(8192);
                    final UiWindowPayload.Widget wiped = canvasOf(computer.takeWindowsOwed(watcher));
                    helper.assertTrue(wiped != null, "the window is still there");
                    helper.assertFalse(wiped.appends(),
                            "a canvas that was cleared goes whole again, not added to the drawing it had");
                })
                .thenSucceed();
    }

    /** The canvas of the one window a machine owed a player, or null when there is none. */
    private static UiWindowPayload.Widget canvasOf(final List<UiWindowPayload> owed) {
        for (final UiWindowPayload payload : owed) {
            for (final UiWindowPayload.Widget widget : payload.widgets()) {
                if ("Canvas".equals(widget.kind())) {
                    return widget;
                }
            }
        }
        return null;
    }

    /*
     * A server player of the test's own, never put on the server's player list: a listed player is announced to
     * every mod when it leaves, and keeps chunks loaded and packets flowing for the rest of the run.
     */
    private static ServerPlayer viewer(final ServerLevel level, final String name) {
        return new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), name),
                ClientInformation.createDefault());
    }

    /** What a player does to a widget reaches the program's handler, and what it changed goes back. */
    @GameTest(template = ARENA)
    public static void click_reachesTheProgramAndChangesWhatTheWindowShows(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.programs().start("panel.sgs", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(8192);
                    final Values.Obj window = computer.programs().windowsOf(started.id()).getFirst();
                    final Values.Obj button = widgetOf(window, UiWidgets.BUTTON);
                    helper.assertTrue(computer.programs().deliverUiEvent(started.id(), 1L,
                            (Long) button.get(UiWidgets.ID), "click", List.of()), "the click is taken");
                    computer.programs().tick(8192);
                    final Values.Obj label = widgetOf(window, UiWidgets.LABEL);
                    helper.assertTrue("cold".equals(label.get(UiWidgets.TEXT)),
                            "the handler ran; the label says " + label.get(UiWidgets.TEXT));
                    final Values.Obj list = widgetOf(window, UiWidgets.LIST_BOX);
                    helper.assertTrue(Integer.valueOf(1).equals(list.get(UiWidgets.COUNT)),
                            "and the list has its row");
                })
                .thenSucceed();
    }

    /** A machine that boots to a prompt has nowhere to put a window, and the program stops saying so. */
    @GameTest(template = ARENA)
    public static void window_refusesOnAMachineWithNoDesktop(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "mc_dos");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.programs().start("panel.sgs", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(8192);
                    final var one = computer.programs().byId(started.id());
                    helper.assertTrue(one.process().state() == dev.jstech.core.language.ILanguageProcess.State.HALTED,
                            "it stops; state " + one.process().state());
                    helper.assertTrue(one.process().message().contains("no desktop to open a window on"),
                            "with the reason; got " + one.process().message());
                    helper.assertTrue(computer.programs().windowsOf(started.id()).isEmpty(), "and opens nothing");
                })
                .thenSucceed();
    }

    /** The window and everything in it come back after a save, ready to be used again. */
    @GameTest(template = ARENA)
    public static void window_carriesThroughASave(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms before = computer.programs();
                    final MachinePrograms.Started started = before.start("panel.sgs", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    before.tick(8192);
                    final CompoundTag tag = new CompoundTag();
                    before.save(tag);

                    final MachinePrograms after = new MachinePrograms();
                    after.load(tag, computer);
                    final int id = after.view().getFirst().id();
                    final List<Values.Obj> windows = after.windowsOf(id);
                    helper.assertTrue(windows.size() == 1, "the window comes back; got " + windows.size());
                    final Values.Obj button = widgetOf(windows.getFirst(), UiWidgets.BUTTON);
                    helper.assertTrue(button != null, "and what it holds with it");
                    helper.assertTrue(after.deliverUiEvent(id, 1L, (Long) button.get(UiWidgets.ID), "click",
                            List.of()), "and it still answers");
                    after.tick(8192);
                    helper.assertTrue("cold".equals(widgetOf(windows.getFirst(), UiWidgets.LABEL)
                            .get(UiWidgets.TEXT)), "the handler ran after the save");
                })
                .thenSucceed();
    }
}
