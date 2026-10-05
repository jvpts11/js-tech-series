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
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.computers.vm.program.UiWidgets;
import dev.jstech.computers.vm.program.Values;
import dev.jstech.core.language.ILanguageProcess;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestComponents;
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

    /** Two windows, each with a canvas too full for both of them to cross in one tick. */
    private static final String WALL = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Wall : IScript {
                Window left;
                Window right;
                Canvas one;
                Canvas two;
                public void OnInit() {
                    one = new Canvas();
                    two = new Canvas();
                    Column a = new Column();
                    a.Add(one, 1);
                    left = new Window("Left", 200, 120);
                    left.Content = a;
                    left.Show();
                    Column b = new Column();
                    b.Add(two, 1);
                    right = new Window("Right", 200, 120);
                    right.Content = b;
                    right.Show();
                    for (int i = 0; i < 300; i = i + 1) {
                        one.SetPixel(i, 1, 7);
                        two.SetPixel(i, 2, 7);
                    }
                }
                public void OnTick() { }
                public void OnDestroy() { left.Close(); right.Close(); }
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
                new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
        hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
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

    /** A machine sends what a tick can carry and the rest waits for the next one, which serves it first. */
    @GameTest(template = ARENA)
    public static void windows_waitTheirTurnWhenOneTickCannotCarryThemAll(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        final ServerPlayer watcher = viewer(helper.getLevel(), "crowd");
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started =
                            computer.programs().start("wall.sgs", WALL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(1_000_000);
                    computer.programs().tick(1_000_000);
                    final int open = computer.programs().windowsOf(started.id()).size();
                    helper.assertTrue(open == 2, "both windows are open; got " + open);
                    final List<UiWindowPayload> first = computer.takeWindowsOwed(watcher);
                    helper.assertTrue(first.size() == 1, "one window fills the tick's budget; got " + first.size());
                    final List<UiWindowPayload> next = computer.takeWindowsOwed(watcher);
                    helper.assertTrue(next.size() == 1, "the one that waited goes next; got " + next.size());
                    helper.assertFalse(first.getFirst().title().equals(next.getFirst().title()),
                            "the other window, not the same one over again");
                    helper.assertTrue(computer.takeWindowsOwed(watcher).isEmpty(), "and then nothing is owed");
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

    /** A machine that boots to the network's interface has nowhere to put a window, and the program says so. */
    @GameTest(template = ARENA)
    public static void window_refusesOnAMachineWithNoScreenForIt(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "mc_net");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.programs().start("panel.sgs", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(8192);
                    final var one = computer.programs().byId(started.id());
                    helper.assertTrue(one.process().state() == ILanguageProcess.State.HALTED,
                            "it stops; state " + one.process().state());
                    helper.assertTrue(one.process().message().english().contains("no desktop to open a window on"),
                            "with the reason; got " + one.process().message().english());
                    helper.assertTrue(computer.programs().windowsOf(started.id()).isEmpty(), "and opens nothing");
                })
                .thenSucceed();
    }

    /**
     * A machine that only has its terminal draws a program's window there in letters, so the window opens; a picture
     * has no letters to be drawn in, and a program making one stops saying so.
     */
    @GameTest(template = ARENA)
    public static void window_opensInLettersOnAMachineWithOnlyItsTerminal(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "mc_dos");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.programs().start("panel.sgs", PANEL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(8192);
                    helper.assertTrue(computer.programs().windowsOf(started.id()).size() == 1,
                            "the window opens, to be drawn on the terminal");
                    final MachinePrograms.Started picture =
                            computer.programs().start("photo.sgs", PICTURE, 1, computer);
                    helper.assertTrue(picture.ok(), picture.message());
                    computer.programs().tick(8192);
                    final var one = computer.programs().byId(picture.id());
                    helper.assertTrue(one.process().state() == ILanguageProcess.State.HALTED,
                            "a picture stops it; state " + one.process().state());
                    helper.assertTrue(one.process().message().english().contains("no Image on a screen of letters"),
                            "with the reason; got " + one.process().message().english());
                })
                .thenSucceed();
    }

    /** Every widget of the second version goes over with what it holds, each in the shape a screen reads it in. */
    @GameTest(template = ARENA)
    public static void widgets_ofTheSecondVersionGoOverWithWhatTheyHold(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started =
                            computer.programs().start("gallery.sgs", GALLERY, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(1_000_000);
                    final var one = computer.programs().byId(started.id());
                    helper.assertTrue(one.process().state() != ILanguageProcess.State.HALTED,
                            "it runs; " + one.process().message().english());
                    final UiWindowPayload payload = UiWindowPayload.of(computer.getBlockPos(), started.id(),
                            computer.programs().windowsOf(started.id()).getFirst());
                    final UiWindowPayload.Widget area = kind(payload, "TextArea");
                    helper.assertTrue(area.rows().size() == 2 && area.joined().length() == 300,
                            "a long text goes as rows; got " + area.rows().size());
                    final UiWindowPayload.Widget table = kind(payload, "Table");
                    helper.assertTrue(table.details().equals(List.of("Name", "Size"))
                            && "notes.txt\t2 KB".equals(table.row(0)), "a table's headers and its rows");
                    final UiWindowPayload.Widget tree = kind(payload, "TreeView");
                    helper.assertTrue(tree.details().equals(List.of("0", "1")), "each node's parent; got "
                            + tree.details());
                    final UiWindowPayload.Widget menu = kind(payload, "ContextMenu");
                    helper.assertTrue(menu.parent() == table.id(), "the menu goes over under its widget");
                    final UiWindowPayload.Widget dial = kind(payload, "GenericComponent");
                    helper.assertTrue("jstests:dial".equals(dial.text()) && "i90;".equals(dial.joined()),
                            "a component's kind and its value; got " + dial.joined());
                    final UiWindowPayload.Widget view = kind(payload, "OperationView");
                    helper.assertTrue(view.number(1) == 3 && view.number(3) == 5 && "PROCESSING".equals(view.row(0)),
                            "an operation's progress and state");
                    final UiWindowPayload.Widget dialog = kind(payload, "OpenFileDialog");
                    helper.assertTrue(dialog != null && dialog.epoch() == 1 && "/docs".equals(dialog.row(0)),
                            "the dialog it showed goes over with the window");
                    helper.assertTrue(kind(payload, "NumberBox").number(5) == 2, "a number box's step goes too");
                })
                .thenSucceed();
    }

    /** A window holding more than one window may carry is cut at the limit, and its widgets say they were. */
    @GameTest(template = ARENA)
    public static void window_isCutAtWhatOneWindowMayCarry(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.programs().start("big.sgs", BIG, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(10_000_000);
                    final UiWindowPayload payload = UiWindowPayload.of(computer.getBlockPos(), started.id(),
                            computer.programs().windowsOf(started.id()).getFirst());
                    helper.assertTrue(payload.weight() <= UiWindowPayload.MOST_BYTES + 1024,
                            "the window stays within its bytes; it weighs " + payload.weight());
                    final UiWindowPayload.Widget table = kind(payload, "Table");
                    helper.assertTrue(table.cut() && table.rows().size() < 1000,
                            "the table says it was cut; it carries " + table.rows().size());
                    helper.assertTrue(kind(payload, "Label") != null, "and a widget after it still goes");
                })
                .thenSucceed();
    }

    /** A box typed into many times in one tick tells its program once, holding every letter. */
    @GameTest(template = ARENA)
    public static void text_reachesTheProgramOnceATick(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started =
                            computer.programs().start("typed.sgs", TYPED, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(8192);
                    final Values.Obj window = computer.programs().windowsOf(started.id()).getFirst();
                    final long box = (Long) widgetOf(window, UiWidgets.TEXT_AREA).get(UiWidgets.ID);
                    for (final String said : List.of("h", "he", "hel", "hell", "hello")) {
                        helper.assertTrue(computer.programs().deliverUiEvent(started.id(), 1L, box, "text",
                                List.of(said)), "every key is taken");
                    }
                    computer.programs().tick(8192);
                    final Values.Obj heard = widgetOf(window, UiWidgets.LABEL);
                    helper.assertTrue("1 hello".equals(heard.get(UiWidgets.TEXT)),
                            "one call, reading every letter; the label says " + heard.get(UiWidgets.TEXT));
                })
                .thenSucceed();
    }

    /** A kind of component another mod added: its value and what a player does are held to what the kind takes. */
    @GameTest(template = ARENA)
    public static void generic_ofAnAddonsKindTakesWhatTheKindTakes(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms.Started started = computer.programs().start("dial.sgs", DIAL, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    computer.programs().tick(8192);
                    final Values.Obj window = computer.programs().windowsOf(started.id()).getFirst();
                    final long dial = (Long) widgetOf(window, UiWidgets.GENERIC).get(UiWidgets.ID);
                    helper.assertTrue(computer.programs().deliverUiEvent(started.id(), 1L, dial, "action",
                            List.of(TestComponents.TURN + "\0i15;")), "a turn the dial takes is heard");
                    helper.assertFalse(computer.programs().deliverUiEvent(started.id(), 1L, dial, "action",
                            List.of("push\0i15;")), "what the kind does not take is not");
                    computer.programs().tick(8192);
                    final Values.Obj angle = widgetOf(window, UiWidgets.LABEL);
                    helper.assertTrue("105".equals(angle.get(UiWidgets.TEXT)),
                            "the handler turned the dial; it reads " + angle.get(UiWidgets.TEXT));

                    final MachinePrograms.Started wrong = computer.programs().start("bad.sgs", BAD_DIAL, 1, computer);
                    computer.programs().tick(8192);
                    final var refused = computer.programs().byId(wrong.id());
                    helper.assertTrue(refused.process().message().english().contains("does not take that value"),
                            "an angle past a full turn is refused; got " + refused.process().message().english());

                    final MachinePrograms.Started web = computer.programs().start("web.sgs", BROWSER, 1, computer);
                    computer.programs().tick(8192);
                    helper.assertTrue(computer.programs().byId(web.id()).process().message().english()
                            .contains("reach outside the game off"), "a kind reaching outside is off by default");
                })
                .thenSucceed();
    }

    /** The second version's widgets come back after a save with what they held. */
    @GameTest(template = ARENA)
    public static void widgets_ofTheSecondVersionCarryThroughASave(final GameTestHelper helper) {
        final CraftingComputerBlockEntity computer = computer(helper, new BlockPos(2, 2, 2), "frames_xp");
        if (computer == null) {
            return;
        }
        helper.startSequence()
                .thenExecuteAfter(SETTLE, () -> {
                    final MachinePrograms before = computer.programs();
                    final MachinePrograms.Started started = before.start("gallery.sgs", GALLERY, 1, computer);
                    helper.assertTrue(started.ok(), started.message());
                    before.tick(1_000_000);
                    final CompoundTag tag = new CompoundTag();
                    before.save(tag);

                    final MachinePrograms after = new MachinePrograms();
                    after.load(tag, computer);
                    helper.assertTrue(!after.view().isEmpty(), "the program comes back after the save");
                    final List<Values.Obj> windows = after.windowsOf(after.view().getFirst().id());
                    helper.assertTrue(!windows.isEmpty(), "with its window");
                    final Values.Obj window = windows.getFirst();
                    final Values.Obj table = widgetOf(window, UiWidgets.TABLE);
                    helper.assertTrue(table != null && Integer.valueOf(1).equals(table.get(UiWidgets.COUNT)),
                            "the table comes back with its row");
                    helper.assertTrue(table.get(UiWidgets.MENU) instanceof Values.Obj,
                            "and with its context menu");
                    helper.assertTrue(window.get(UiWidgets.DIALOG) instanceof Values.Obj,
                            "the dialog it had up is still up");
                    helper.assertTrue(Integer.valueOf(90).equals(widgetOf(window, UiWidgets.GENERIC)
                            .get(UiWidgets.DATA)), "the component holds its value");
                })
                .thenSucceed();
    }

    private static UiWindowPayload.Widget kind(final UiWindowPayload payload, final String kind) {
        for (final UiWindowPayload.Widget widget : payload.widgets()) {
            if (kind.equals(widget.kind())) {
                return widget;
            }
        }
        return null;
    }

    /** A program that makes a picture, which a screen of letters cannot show. */
    private static final String PICTURE = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Photo : IScript {
                public void OnInit() { Image photo = new Image("/a.pix"); }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    /** One of every widget the second version brought, with something in each. */
    private static final String GALLERY = """
            using System.*;
            using System.UI.*;
            using System.Operations.*;
            namespace Art;
            class Gallery : IScript {
                Window window;
                public void OnInit() {
                    TextArea area = new TextArea();
                    string text = "";
                    for (int i = 0; i < 30; i = i + 1) { text = text + "0123456789"; }
                    area.Text = text;
                    NumberBox copies = new NumberBox(0, 10);
                    copies.Step = 2;
                    Table table = new Table();
                    table.AddColumn("Name");
                    table.AddColumn("Size");
                    table.AddRow("notes.txt", "2 KB");
                    ContextMenu menu = new ContextMenu(table);
                    menu.Add("Open");
                    TreeView tree = new TreeView();
                    int top = tree.Add("This PC");
                    tree.Add("C:", top);
                    GenericComponent dial = new GenericComponent("jstests:dial");
                    dial.Data = 90;
                    OperationView view = new OperationView();
                    view.Text = "SELECT 64 diamond";
                    view.State = "PROCESSING";
                    view.Done = 3;
                    view.Total = 5;
                    Column page = new Column();
                    page.Add(area);
                    page.Add(copies);
                    page.Add(table);
                    page.Add(tree);
                    page.Add(dial);
                    page.Add(view);
                    window = new Window("Gallery", 320, 300);
                    window.Content = page;
                    window.Show();
                    OpenFileDialog open = new OpenFileDialog("Open");
                    open.Path = "/docs";
                    open.Show(window);
                }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    /** A table far bigger than one window may carry, and a label after it. */
    private static final String BIG = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Big : IScript {
                Window window;
                public void OnInit() {
                    string cell = "";
                    for (int i = 0; i < 25; i = i + 1) { cell = cell + "0123456789"; }
                    Table table = new Table();
                    table.AddColumn("Wide");
                    for (int i = 0; i < 1000; i = i + 1) { table.AddRow(cell); }
                    Column page = new Column();
                    page.Add(table);
                    page.Add(new Label("after"));
                    window = new Window("Big", 300, 200);
                    window.Content = page;
                    window.Show();
                }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    /** A text area that counts how many times it was told of a change, and what it held then. */
    private static final String TYPED = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Typed : IScript {
                Window window;
                TextArea notes;
                Label heard;
                int times = 0;
                public void OnInit() {
                    notes = new TextArea();
                    notes.OnChange += Changed;
                    heard = new Label("");
                    Column page = new Column();
                    page.Add(notes);
                    page.Add(heard);
                    window = new Window("Typed", 200, 120);
                    window.Content = page;
                    window.Show();
                }
                void Changed() { times = times + 1; heard.Text = "" + times + " " + notes.Text; }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    /** The test mod's dial, turned by what a player does to it. */
    private static final String DIAL = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Dial : IScript {
                Window window;
                GenericComponent dial;
                Label angle;
                int at = 90;
                public void OnInit() {
                    dial = new GenericComponent("jstests:dial");
                    dial.Data = at;
                    dial.OnAction(Turned);
                    angle = new Label("90");
                    Column page = new Column();
                    page.Add(dial);
                    page.Add(angle);
                    window = new Window("Dial", 200, 120);
                    window.Content = page;
                    window.Show();
                }
                void Turned(ComponentAction action) {
                    at = at + (int) action.Value;
                    dial.Data = at;
                    angle.Text = "" + at;
                }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    /** A dial handed an angle past a full turn, which the kind refuses. */
    private static final String BAD_DIAL = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Bad : IScript {
                public void OnInit() {
                    GenericComponent dial = new GenericComponent("jstests:dial");
                    dial.Data = 400;
                }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    /** A page of the web, which a server keeps off unless it is told otherwise. */
    private static final String BROWSER = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Web : IScript {
                public void OnInit() { GenericComponent page = new GenericComponent("jstests:browser"); }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

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
