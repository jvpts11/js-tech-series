/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.cannon.machine.MachinePrograms;
import dev.jstech.computers.cannon.run.Values;
import dev.jstech.computers.cannon.ui.UiWidgets;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A Cannon program with a window of its own on a real machine: it opens on a system with a desktop and
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

    private static CraftingComputerBlockEntity computer(final GameTestHelper helper, final BlockPos at,
                                                        final String os) {
        helper.setBlock(at, ComputingModule.CRAFTING_COMPUTER.get());
        if (!(helper.getBlockEntity(at) instanceof CraftingComputerBlockEntity computer)) {
            helper.fail("no computer at " + at);
            return null;
        }
        final ItemStackHandler hw = computer.getHardware();
        hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT, new ItemStack(ComputingModule.MOTHERBOARD_ATX_P.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT, new ItemStack(ComputingModule.CPU_ASCENT_965.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START, new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
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
                    final MachinePrograms.Started started = computer.cannon().start("panel.can", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), "it starts: " + started.message());
                    computer.cannon().tick(8192);
                    final List<Values.Obj> windows = computer.cannon().windowsOf(started.id());
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

    /** What a player does to a widget reaches the program's handler, and what it changed goes back. */
    @GameTest(template = ARENA)
    public static void click_reachesTheProgramAndChangesWhatTheWindowShows(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.cannon().start("panel.can", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.cannon().tick(8192);
                    final Values.Obj window = computer.cannon().windowsOf(started.id()).getFirst();
                    final Values.Obj button = widgetOf(window, UiWidgets.BUTTON);
                    helper.assertTrue(computer.cannon().deliverUiEvent(started.id(), 1L,
                            (Long) button.get(UiWidgets.ID), "click", List.of()), "the click is taken");
                    computer.cannon().tick(8192);
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
                    final MachinePrograms.Started started = computer.cannon().start("panel.can", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.cannon().tick(8192);
                    final MachinePrograms.Live one = computer.cannon().byId(started.id());
                    helper.assertTrue(one.process().state() == dev.jstech.core.language.ILanguageProcess.State.HALTED,
                            "it stops; state " + one.process().state());
                    helper.assertTrue(one.process().message().contains("no desktop to open a window on"),
                            "with the reason; got " + one.process().message());
                    helper.assertTrue(computer.cannon().windowsOf(started.id()).isEmpty(), "and opens nothing");
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
                    final MachinePrograms before = computer.cannon();
                    final MachinePrograms.Started started = before.start("panel.can", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    before.tick(8192);
                    final CompoundTag tag = new CompoundTag();
                    before.save(tag);

                    final MachinePrograms after = new MachinePrograms();
                    after.load(tag, computer);
                    final int id = after.all().getFirst().id();
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
