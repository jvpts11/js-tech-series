/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.vm.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.cannon.CannonCompiler;
import dev.jstech.computers.cannon.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import dev.jstech.computers.vm.listing.ListingProblem;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A Cannon program opening a window on the machine's desktop: what it builds is its own to hold, what a
 * player does with it reaches its handlers, and all of it comes back after a save.
 */
class UiWidgetsTest {

    private static final long ROOM = 256L * 1024;
    private static final int PLENTY = 1_000_000;

    /** A machine with a desktop to open windows on. */
    private static IHost desktop(final boolean has) {
        return new IHost() {
            @Override
            public long tick() {
                return 0;
            }

            @Override
            public long dayTime() {
                return 0;
            }

            @Override
            public long day() {
                return 0;
            }

            @Override
            public boolean provides(final String owner) {
                return "Computer".equals(owner);
            }

            @Override
            public Reply call(final String owner, final String member, final List<Object> arguments,
                              final String caller, final int line) {
                return Reply.of("Desktop".equals(member) ? has : "a machine", 1);
            }
        };
    }

    private static final String PRELUDE = "using System.*; using System.UI.*; using System.IO.*; "
            + "namespace Tests; ";

    private static ProgramImage load(final String source) {
        final CannonCompiler.Result built =
                CannonCompiler.compile(List.of(new SourceFile("Panel.can", PRELUDE + source)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram program = reader.read();
        assertFalse(reader.hasProblems(), () -> String.join("\n",
                reader.problems().stream().map(ListingProblem::format).toList()) + "\n" + built.assembly());
        return ProgramImage.of(program);
    }

    private static Process start(final ProgramImage program, final IHost host) {
        final Process process = new Process(program, ROOM, host);
        process.begin(process.create(program.entryPoint()), "OnInit");
        process.step(PLENTY);
        return process;
    }

    /** The panel the tests build: a row of a label and a bar, a check box, a button and a list. */
    private static final String PANEL = """
            class Panel : IScript {
                Window window;
                Label heat;
                ProgressBar fuel;
                CheckBox live;
                Button scram;
                ListBox log;
                public int scrams = 0;

                public void OnInit() {
                    heat = new Label("812 K");
                    fuel = new ProgressBar(0, 100);
                    Row top = new Row();
                    top.Add(heat);
                    top.Add(fuel, 1);

                    live = new CheckBox("Live", true);
                    scram = new Button("SCRAM");
                    scram.OnClick += Scram;
                    Row buttons = new Row();
                    buttons.Add(live);
                    buttons.Add(scram);

                    log = new ListBox();
                    log.Add("started", "0 s");

                    Column page = new Column();
                    page.Add(top);
                    page.Add(buttons);
                    page.Add(log, 1);

                    window = new Window("Reactor", 260, 170);
                    window.Content = page;
                    window.Show();
                }

                void Scram() {
                    scrams = scrams + 1;
                    heat.Text = "cold";
                    log.Add("scram " + scrams, "now");
                }

                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    private static Values.Obj widgetOf(final Process process, final String type) {
        for (final Values.Obj widget : UiWidgets.inside(process.windows().getFirst())) {
            if (type.equals(widget.type())) {
                return widget;
            }
        }
        return null;
    }

    @Test
    void show_putsTheWindowTheProgramBuiltOnTheMachinesDesktop() {
        final Process process = start(load(PANEL), desktop(true));
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        assertEquals(1, process.windows().size());
        final Values.Obj window = process.windows().getFirst();
        assertEquals("Reactor", window.get(UiWidgets.TITLE));
        assertEquals(260, window.get(UiWidgets.WIDTH));
        assertEquals(Boolean.TRUE, window.get(UiWidgets.OPEN));
        assertEquals(1L, window.get(UiWidgets.ID));
        // The column, its two rows and the five widgets in them.
        assertEquals(8, UiWidgets.inside(window).size());
        assertEquals("812 K", widgetOf(process, UiWidgets.LABEL).get(UiWidgets.TEXT));
    }

    @Test
    void add_givesAWeightToWhatShouldTakeTheRoomLeftOver() {
        final Process process = start(load(PANEL), desktop(true));
        final Values.Obj column = widgetOf(process, UiWidgets.COLUMN);
        assertNotNull(column);
        final Values.ListValue weights = (Values.ListValue) column.get(UiWidgets.WEIGHTS);
        assertEquals(List.of(0, 0, 1), weights.items(), "the list takes what the rows leave");
    }

    @Test
    void click_reachesTheHandlerAndWhatItChangedIsOnTheWidget() {
        final Process process = start(load(PANEL), desktop(true));
        final Values.Obj window = process.windows().getFirst();
        final Values.Obj button = widgetOf(process, UiWidgets.BUTTON);
        assertTrue(process.deliverUiEvent(1L, (Long) button.get(UiWidgets.ID), "click", List.of()),
                "the click reaches the program");
        process.step(PLENTY);
        assertEquals("cold", widgetOf(process, UiWidgets.LABEL).get(UiWidgets.TEXT));
        final Values.Obj list = widgetOf(process, UiWidgets.LIST_BOX);
        assertEquals(2, list.get(UiWidgets.COUNT));
        assertEquals(List.of("started", "scram 1"), ((Values.ListValue) list.get(UiWidgets.ITEMS)).items());
        assertEquals(Boolean.TRUE, window.get(UiWidgets.OPEN));
    }

    @Test
    void click_reachesEveryHandlerJoinedToTheButtonInTheOrderTheyWereJoined() {
        final Process process = start(load("""
                class Panel : IScript {
                    Window window;
                    Button fire;
                    public void OnInit() {
                        fire = new Button("Fire");
                        fire.OnClick += First;
                        fire.OnClick += Second;
                        Column page = new Column();
                        page.Add(fire);
                        window = new Window("Both", 200, 100);
                        window.Content = page;
                        window.Show();
                    }
                    void First() { Console.PrintLine("first"); }
                    void Second() { Console.PrintLine("second"); }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """), desktop(true));
        final Values.Obj button = widgetOf(process, UiWidgets.BUTTON);
        assertTrue(process.deliverUiEvent(1L, (Long) button.get(UiWidgets.ID), "click", List.of()));
        process.step(PLENTY);
        assertEquals(List.of("first", "second"), process.console());
    }

    @Test
    void click_thatFindsNoRoomIsDroppedAndCounted() {
        final Process process = start(load(PANEL), desktop(true));
        final long button = (Long) widgetOf(process, UiWidgets.BUTTON).get(UiWidgets.ID);
        for (int i = 0; i < CallbackQueue.MOST_CALLS + 4; i++) {
            assertTrue(process.deliverUiEvent(1L, button, "click", List.of()), "the button is pressed either way");
        }
        process.step(PLENTY);
        assertEquals(CallbackQueue.MOST_CALLS, process.script().get("scrams"));
        assertEquals(4, process.droppedEvents());
    }

    @Test
    void close_isHeardAheadOfTheClicksStillWaiting() {
        final Process process = start(load("""
                class Panel : IScript {
                    Window window;
                    Button fire;
                    public void OnInit() {
                        fire = new Button("Fire");
                        fire.OnClick += Fire;
                        Column page = new Column();
                        page.Add(fire);
                        window = new Window("Late", 200, 100);
                        window.Content = page;
                        window.OnClose += Closed;
                        window.Show();
                    }
                    void Fire() { Console.PrintLine("click"); }
                    void Closed() { Console.PrintLine("closed"); }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """), desktop(true));
        final long button = (Long) widgetOf(process, UiWidgets.BUTTON).get(UiWidgets.ID);
        assertTrue(process.deliverUiEvent(1L, button, "click", List.of()));
        assertTrue(process.deliverUiEvent(1L, button, "click", List.of()));
        assertTrue(process.deliverUiEvent(1L, 0L, "close", List.of()));
        process.step(PLENTY);
        assertEquals(List.of("closed", "click", "click"), process.console());
    }

    @Test
    void toggleAndSelect_changeTheWidgetBeforeTheProgramHearsOfThem() {
        final Process process = start(load(PANEL), desktop(true));
        final Values.Obj check = widgetOf(process, UiWidgets.CHECK_BOX);
        process.deliverUiEvent(1L, (Long) check.get(UiWidgets.ID), "toggle", List.of(Boolean.FALSE));
        assertEquals(Boolean.FALSE, check.get(UiWidgets.CHECKED));
        final Values.Obj list = widgetOf(process, UiWidgets.LIST_BOX);
        process.deliverUiEvent(1L, (Long) list.get(UiWidgets.ID), "select", List.of(1));
        assertEquals(1, list.get(UiWidgets.SELECTED));
    }

    @Test
    void close_takesTheWindowOffTheDesktopAndEndsTheProgramWithIt() {
        final Process process = start(load(PANEL), desktop(true));
        assertTrue(process.deliverUiEvent(1L, 0L, "close", List.of()));
        assertTrue(process.windows().isEmpty());
        process.step(PLENTY);
        assertTrue(process.exited(), "a program whose last window was shut is over");
    }

    @Test
    void close_leavesAProgramThatOpensAnotherWindowRunning() {
        final Process process = start(load("""
                class Panel : IScript {
                    Window window;
                    public void OnInit() {
                        window = new Window("first", 200, 100);
                        window.OnClose += Again;
                        window.Show();
                    }
                    void Again() {
                        window = new Window("second", 200, 100);
                        window.Show();
                    }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """), desktop(true));
        assertTrue(process.deliverUiEvent(1L, 0L, "close", List.of()));
        process.step(PLENTY);
        assertFalse(process.exited(), "it opened another window, so it carries on");
        assertEquals(1, process.windows().size());
        assertEquals("second", process.windows().getFirst().get(UiWidgets.TITLE));
    }

    @Test
    void save_keepsThatClosingTheLastWindowEndsTheProgram() {
        final ProgramImage program = load(PANEL);
        final Process process = start(program, desktop(true));
        assertTrue(process.deliverUiEvent(1L, 0L, "close", List.of()));

        final Process restored = Process.restore(program, process.save(), desktop(true));
        restored.step(PLENTY);

        assertTrue(restored.exited(), "the last window was shut before the save, so the program ends after it");
    }

    @Test
    void show_saysSoOnAMachineWithNoDesktop() {
        final Process process = start(load(PANEL), desktop(false));
        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().contains("no desktop to open a window on"), process.message());
    }

    @Test
    void save_bringsBackTheWindowAndWhatItShowed() {
        final ProgramImage program = load(PANEL);
        Process process = start(program, desktop(true));
        final Snapshot shot = process.save();
        process = Process.restore(program, shot, desktop(true));
        assertEquals(1, process.windows().size());
        final Values.Obj window = process.windows().getFirst();
        assertEquals("Reactor", window.get(UiWidgets.TITLE));
        final Values.Obj button = widgetOf(process, UiWidgets.BUTTON);
        assertNotNull(button, "the widgets come back with it");
        assertTrue(process.deliverUiEvent(1L, (Long) button.get(UiWidgets.ID), "click", List.of()));
        process.step(PLENTY);
        assertEquals("cold", widgetOf(process, UiWidgets.LABEL).get(UiWidgets.TEXT));
    }

    @Test
    void messageBox_opensASmallWindowOfItsOwn() {
        final Process process = start(load("""
                class Panel : IScript {
                    public void OnInit() { MessageBox.Show("Careful", "the core is hot"); }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """), desktop(true));
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        final Values.Obj window = process.windows().getFirst();
        assertEquals("Careful", window.get(UiWidgets.TITLE));
        assertEquals(Boolean.TRUE, window.get(UiWidgets.ASK));
        assertEquals("the core is hot", ((Values.Obj) window.get(UiWidgets.CONTENT)).get(UiWidgets.TEXT));
    }

    @Test
    void canvas_keepsWhatItWasAskedToDraw() {
        final Process process = start(load("""
                class Panel : IScript {
                    Window window;
                    public void OnInit() {
                        Canvas paper = new Canvas(120, 80);
                        paper.Clear(15);
                        paper.FillRect(2, 2, 40, 20, 3);
                        paper.DrawText("hot", 4, 24, 1);
                        window = new Window("Draw", 140, 110);
                        window.Content = paper;
                        window.Show();
                    }
                    public void OnTick() { }
                    public void OnDestroy() { }
                }
                """), desktop(true));
        assertEquals(Process.State.FINISHED, process.state(), process::message);
        final Values.Obj paper = widgetOf(process, UiWidgets.CANVAS);
        final Values.ListValue drawing = (Values.ListValue) paper.get(UiWidgets.DRAWING);
        assertEquals(3, drawing.items().size());
        assertEquals("FillRect", ((Values.Obj) drawing.items().get(1)).get("Kind"));
        assertEquals(120, paper.get(UiWidgets.WIDTH));
    }

    /** A panel the save tests share: a canvas drawn on, a text box, a list and a label placed by hand. */
    private static final String SKETCH = """
            class Panel : IScript {
                Window window;
                Canvas paper;
                TextBox name;
                ListBox log;
                public string seen = "";
                public void OnInit() {
                    paper = new Canvas(120, 80);
                    paper.FillRect(2, 2, 40, 20, 3);
                    paper.DrawText("hot", 4, 24, 1);
                    name = new TextBox("");
                    name.OnChange += Changed;
                    log = new ListBox();
                    log.Add("first", "1 s");
                    Column page = new Column();
                    page.Add(paper);
                    page.Add(name);
                    page.Add(log);
                    window = new Window("Sketch", 300, 200);
                    window.Content = page;
                    window.Add(new Label("placed"), 4, 4, 60, 12);
                    window.Show();
                }
                void Changed() { seen = name.Text; }
                public void OnTick() { }
                public void OnDestroy() { }
            }
            """;

    @Test
    void save_bringsBackWhatWasDrawnPlacedAndListed() {
        final ProgramImage program = load(SKETCH);
        final Process process = start(program, desktop(true));

        final Process restored = Process.restore(program, process.save(), desktop(true));

        final Values.ListValue drawing =
                (Values.ListValue) widgetOf(restored, UiWidgets.CANVAS).get(UiWidgets.DRAWING);
        assertEquals("FillRect", ((Values.Obj) drawing.items().get(0)).get("Kind"));
        assertEquals("hot", ((Values.Obj) drawing.items().get(1)).get(UiWidgets.TEXT));
        assertTrue(UiWidgets.inside(restored.windows().getFirst()).stream()
                        .anyMatch(widget -> "placed".equals(widget.get(UiWidgets.TEXT))),
                "the label placed by hand comes back");
        assertEquals(List.of("first"),
                ((Values.ListValue) widgetOf(restored, UiWidgets.LIST_BOX).get(UiWidgets.ITEMS)).items());
    }

    @Test
    void save_keepsWhatAPlayerTypedAndWhatTheProgramReadFromIt() {
        final ProgramImage program = load(SKETCH);
        final Process process = start(program, desktop(true));
        final long box = (Long) widgetOf(process, UiWidgets.TEXT_BOX).get(UiWidgets.ID);
        assertTrue(process.deliverUiEvent(1L, box, "text", List.of("Ada")));
        process.step(PLENTY);
        assertTrue(process.deliverUiEvent(1L, box, "text", List.of("Ada L")));

        final Process restored = Process.restore(program, process.save(), desktop(true));

        assertEquals("Ada L", widgetOf(restored, UiWidgets.TEXT_BOX).get(UiWidgets.TEXT));
        assertEquals("Ada", restored.script().get("seen"), "what the program read stays the program's");
    }

    @Test
    void clearingACanvasAndTypingTakeNoMoreMemoryTickAfterTick() {
        final Process process = start(load("""
                class Panel : IScript {
                    Window window;
                    Canvas paper;
                    TextBox name;
                    public void OnInit() {
                        paper = new Canvas(120, 80);
                        name = new TextBox("");
                        Column page = new Column();
                        page.Add(paper);
                        page.Add(name);
                        window = new Window("Busy", 200, 150);
                        window.Content = page;
                        window.Show();
                    }
                    public void OnTick() {
                        paper.Clear(0);
                        paper.FillRect(1, 1, 10, 10, 2);
                        paper.SetPixel(5, 5, 1);
                    }
                    public void OnDestroy() { }
                }
                """), desktop(true));
        final long box = (Long) widgetOf(process, UiWidgets.TEXT_BOX).get(UiWidgets.ID);
        assertTrue(process.deliverUiEvent(1L, box, "text", List.of("x")));
        process.begin(process.script(), "OnTick");
        process.step(PLENTY);
        final long used = process.heap().used();

        for (int i = 0; i < 20; i++) {
            assertTrue(process.deliverUiEvent(1L, box, "text", List.of("y")));
            process.begin(process.script(), "OnTick");
            process.step(PLENTY);
        }

        assertEquals(used, process.heap().used());
    }
}
