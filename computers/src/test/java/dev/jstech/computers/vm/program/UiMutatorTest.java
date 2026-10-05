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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.jstech.computers.sigma.SigmaCompiler;
import dev.jstech.computers.sigma.SourceFile;
import dev.jstech.computers.vm.listing.AsmProgram;
import dev.jstech.computers.vm.listing.AsmReader;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The widgets the second version of Σ# brought, as the one door every change goes through takes them: what a program
 * may write on them, what a player may do to them, and the two rules that keep a window cheap, one text event a tick
 * per box and a generic component holding only values.
 */
class UiMutatorTest {

    private static final long ROOM = 256L * 1024;
    private static final int PLENTY = 1_000_000;
    private static final String PRELUDE = "using System.*; using System.UI.*; using System.IO.*; "
            + "using System.Operations.*; namespace Tests; ";
    /** A handler that prints what a player did to a generic component. */
    private static final String ACTED =
            "void Acted(ComponentAction action) { Console.PrintLine(action.Name + \"=\" + action.Value); }";

    @Test
    void numberBox_keepsItsValueBetweenItsLeastAndMost() {
        final Process process = start("""
                NumberBox copies = new NumberBox(1, 9);
                copies.Value = 50;
                copies.OnChange += Heard;
                show(copies);
                """);
        final Values.Obj box = widgetOf(process, UiWidgets.NUMBER_BOX);
        assertEquals(9, box.get(UiWidgets.VALUE), "a value past the most is brought back to it");
        final long id = (Long) box.get(UiWidgets.ID);

        assertFalse(process.deliverUiEvent(1L, id, "number", List.of(12)), "nothing past the most is taken");
        assertTrue(process.deliverUiEvent(1L, id, "number", List.of(4)));
        process.step(PLENTY);

        assertEquals(4, box.get(UiWidgets.VALUE));
        assertEquals(List.of("heard"), process.console());
    }

    @Test
    void table_keepsItsRowsAndHandsBackACell() {
        final Process process = start("""
                Table table = new Table();
                table.AddColumn("Name");
                table.AddColumn("Size");
                table.AddRow("notes.txt", "2 KB");
                table.AddRow("tab\there", "1 KB");
                Console.PrintLine(table.Cell(1, 2) + "|" + table.Cell(2, 1) + "|" + table.Count);
                show(table);
                """);

        assertEquals(List.of("2 KB|tab here|2"), process.console(), "a tab in a cell is written as a space");
        final Values.Obj table = widgetOf(process, UiWidgets.TABLE);
        assertTrue(process.deliverUiEvent(1L, (Long) table.get(UiWidgets.ID), "select", List.of(2)));
        assertFalse(process.deliverUiEvent(1L, (Long) table.get(UiWidgets.ID), "select", List.of(3)));
        assertEquals(2, table.get(UiWidgets.SELECTED));
    }

    @Test
    void tree_handsBackEachNodesNumberAndRefusesAParentItLacks() {
        final Process process = start("""
                TreeView tree = new TreeView();
                int top = tree.Add("This PC");
                int disk = tree.Add("C:", top);
                Console.PrintLine(top + " " + disk + " " + tree.NodeText(disk));
                show(tree);
                tree.Add("lost", 7);
                """);

        assertEquals("1 2 C:", process.console().getFirst());
        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().english().contains("no node 7"), String.valueOf(process.message()));
    }

    @Test
    void tabView_showsItsFirstPageAndTakesOnlyATabItHas() {
        final Process process = start("""
                TabView tabs = new TabView();
                tabs.Add("One", new Label("first"));
                tabs.Add("Two", new Label("second"));
                show(tabs);
                """);
        final Values.Obj tabs = widgetOf(process, UiWidgets.TAB_VIEW);
        assertEquals(1, tabs.get(UiWidgets.SELECTED), "the first page added is the one showing");
        final long id = (Long) tabs.get(UiWidgets.ID);

        assertFalse(process.deliverUiEvent(1L, id, "select", List.of(0)), "a tab view always shows a page");
        assertTrue(process.deliverUiEvent(1L, id, "select", List.of(2)));

        assertEquals(2, tabs.get(UiWidgets.SELECTED));
        assertEquals(4, UiWidgets.inside(process.windows().getFirst()).size(), "column, tabs, both pages");
    }

    @Test
    void contextMenu_isTheWidgetsOwnAndTellsWhatWasPicked() {
        final Process process = start("""
                ListBox files = new ListBox();
                menu = new ContextMenu(files);
                menu.Add("Open");
                menu.Add("Rename");
                menu.OnPick += Picked;
                show(files);
                """, "void Picked() { Console.PrintLine(menu.Picked); }", "ContextMenu menu;");
        final Values.Obj files = widgetOf(process, UiWidgets.LIST_BOX);
        final Values.Obj menu = widgetOf(process, UiWidgets.CONTEXT_MENU);
        assertNotNull(menu, "the window holds the menu through the widget it belongs to");
        assertSame(menu, files.get(UiWidgets.MENU));

        assertTrue(process.deliverUiEvent(1L, (Long) menu.get(UiWidgets.ID), "pick", List.of(2)));
        process.step(PLENTY);

        assertEquals(List.of("Rename"), process.console());
    }

    @Test
    void contextMenu_isNotPutInAWindowItself() {
        final Process process = start("""
                ContextMenu menu = new ContextMenu(new Label("x"));
                show(menu);
                """);

        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().english().contains("opens over one"), String.valueOf(process.message()));
    }

    @Test
    void menuBar_saysWhichMenuAnEntryWasUnder() {
        final Process process = start("""
                bar = new MenuBar();
                bar.Add("File", "Open");
                bar.Add("Edit", "Copy");
                bar.OnPick += Picked;
                show(bar);
                """, "void Picked() { Console.PrintLine(bar.PickedMenu + \"/\" + bar.Picked); }", "MenuBar bar;");
        final Values.Obj bar = widgetOf(process, UiWidgets.MENU_BAR);

        assertTrue(process.deliverUiEvent(1L, (Long) bar.get(UiWidgets.ID), "pick", List.of(2)));
        process.step(PLENTY);

        assertEquals(List.of("Edit/Copy"), process.console());
    }

    @Test
    void logView_keepsItsNewestLinesAndChartItsNewestNumbers() {
        final Process process = start("""
                LogView log = new LogView();
                Chart chart = new Chart(0, 100);
                for (int i = 0; i < 300; i++) { log.Add("line " + i); chart.Add(i); }
                Console.PrintLine(log.Count + " " + chart.Count);
                show(log);
                """);

        assertEquals(List.of(UiWidgets.MOST_LOG + " " + UiWidgets.MOST_POINTS), process.console());
        final Values.ListValue lines = (Values.ListValue) widgetOf(process, UiWidgets.LOG_VIEW).get(UiWidgets.ITEMS);
        assertEquals("line 44", lines.items().getFirst(), "the oldest went first");
    }

    @Test
    void fileDialog_goesUpOverAnOpenWindowAndTheAnswerTakesItDown() {
        final Process process = start("""
                opener = new OpenFileDialog("Open");
                opener.Path = "/docs";
                opener.OnChoose += Chosen;
                show(new Label("x"));
                opener.Show(window);
                """, "void Chosen() { Console.PrintLine(opener.Path); }", "OpenFileDialog opener;");
        final Values.Obj window = process.windows().getFirst();
        final Values.Obj dialog = (Values.Obj) window.get(UiWidgets.DIALOG);
        assertNotNull(dialog);
        assertEquals(1, dialog.get(UiWidgets.EPOCH));

        assertFalse(process.deliverUiEvent(1L, (Long) dialog.get(UiWidgets.ID), "file", List.of("")),
                "an empty answer is no answer");
        assertTrue(process.deliverUiEvent(1L, (Long) dialog.get(UiWidgets.ID), "file", List.of("/docs/a.txt")));
        process.step(PLENTY);

        assertNull(window.get(UiWidgets.DIALOG));
        assertEquals(List.of("/docs/a.txt"), process.console());
        assertFalse(process.deliverUiEvent(1L, (Long) dialog.get(UiWidgets.ID), "cancel", List.of()),
                "a dialog that is down cannot be answered again");
    }

    @Test
    void fileDialog_opensOnlyOverAWindowThatIsOpen() {
        final Process process = start("""
                SaveFileDialog saver = new SaveFileDialog();
                saver.Show(new Window("shut", 100, 100));
                """);

        assertEquals(Process.State.HALTED, process.state());
        assertTrue(process.message().english().contains("window that is open"), String.valueOf(process.message()));
    }

    @Test
    void text_tellsAHandlerOnceATickHoweverFastABoxIsTypedIn() {
        final long[] tick = {5};
        final Process process = start(load(page("""
                TextArea notes = new TextArea();
                notes.OnChange += Heard;
                show(notes);
                """, "", "")), host(tick, false, false));
        final long id = (Long) widgetOf(process, UiWidgets.TEXT_AREA).get(UiWidgets.ID);

        assertTrue(process.deliverUiEvent(1L, id, "text", List.of("a")));
        assertTrue(process.deliverUiEvent(1L, id, "text", List.of("ab")));
        assertTrue(process.deliverUiEvent(1L, id, "text", List.of("abc")));
        process.step(PLENTY);
        tick[0] = 6;
        assertTrue(process.deliverUiEvent(1L, id, "text", List.of("abcd")));
        process.step(PLENTY);

        assertEquals(List.of("heard", "heard"), process.console(), "one call a tick");
        assertEquals("abcd", widgetOf(process, UiWidgets.TEXT_AREA).get(UiWidgets.TEXT), "every letter is kept");
    }

    @Test
    void textArea_takesMoreThanALineAndNoMoreThanItHolds() {
        final Process process = start("""
                TextArea notes = new TextArea();
                show(notes);
                """);
        final long id = (Long) widgetOf(process, UiWidgets.TEXT_AREA).get(UiWidgets.ID);

        assertTrue(process.deliverUiEvent(1L, id, "text", List.of("x".repeat(UiWidgets.MOST_TEXT * 4))));
        assertFalse(process.deliverUiEvent(1L, id, "text", List.of("x".repeat(UiWidgets.MOST_AREA + 1))));
    }

    @Test
    void generic_holdsACopyOfWhatItIsHanded() {
        final Process process = start("""
                List<int> numbers = new List<int>();
                numbers.Add(1);
                GenericComponent dial = new GenericComponent("tests:dial");
                dial.Data = numbers;
                numbers.Add(2);
                List<int> read = (List<int>) dial.Data;
                Console.PrintLine("" + read.Count + numbers.Count);
                show(dial);
                """);

        assertEquals(List.of("12"), process.console(), () -> String.valueOf(process.message()));
        final Object held = widgetOf(process, UiWidgets.GENERIC).get(UiWidgets.DATA);
        assertEquals("[i1;]", ComponentValues.encode(held));
    }

    @Test
    void generic_refusesAHandlerOrAWidget() {
        final Process handler = start("""
                GenericComponent dial = new GenericComponent("tests:dial");
                Action act = Heard;
                dial.Data = act;
                """);
        assertEquals(Process.State.HALTED, handler.state());
        assertTrue(handler.message().english().contains("not handlers"), String.valueOf(handler.message()));

        final Process widget = start("""
                GenericComponent dial = new GenericComponent("tests:dial");
                dial.Data = new Label("x");
                """);
        assertEquals(Process.State.HALTED, widget.state());
        assertTrue(widget.message().english().contains("a Label is not one"), String.valueOf(widget.message()));
    }

    @Test
    void generic_tellsItsHandlerWhatAPlayerDidWhenItsKindTakesIt() {
        ComponentRules.install(id -> "tests:dial".equals(id) ? new Rule(id, false) : null);
        try {
            final Process process = start("""
                    dial = new GenericComponent("tests:dial");
                    dial.OnAction(Acted);
                    show(dial);
                    """, ACTED, "GenericComponent dial;");
            final long id = (Long) widgetOf(process, UiWidgets.GENERIC).get(UiWidgets.ID);

            assertTrue(process.deliverUiEvent(1L, id, "action", List.of("turn\0i7;")));
            assertFalse(process.deliverUiEvent(1L, id, "action", List.of("refused\0i7;")), "the kind refuses that");
            assertFalse(process.deliverUiEvent(1L, id, "action", List.of("turn\0[[[")), "unreadable");
            process.step(PLENTY);

            assertEquals(List.of("turn=7"), process.console());
        } finally {
            ComponentRules.install(null);
        }
    }

    @Test
    void generic_ofAKindNobodyAddedHearsNothing() {
        final Process process = start("""
                GenericComponent dial = new GenericComponent("gone:dial");
                dial.Data = "kept";
                show(dial);
                """);
        final Values.Obj dial = widgetOf(process, UiWidgets.GENERIC);

        assertEquals(Process.State.FINISHED, process.state(), () -> String.valueOf(process.message()));
        assertEquals("kept", dial.get(UiWidgets.DATA), "the value stays the program's");
        assertFalse(process.deliverUiEvent(1L, (Long) dial.get(UiWidgets.ID), "action", List.of("turn\0n")));
    }

    @Test
    void generic_reachingOutsideTheGameIsOffUnlessTheServerTurnsItOn() {
        ComponentRules.install(id -> new Rule(id, true));
        try {
            final String making = """
                    GenericComponent page = new GenericComponent("tests:browser");
                    show(page);
                    """;
            final Process off = start(load(page(making, "", "")), host(new long[] {0}, false, false));
            assertEquals(Process.State.HALTED, off.state());
            assertTrue(off.message().english().contains("keeps components that reach outside the game off"),
                    String.valueOf(off.message()));

            final Process on = start(load(page(making, "", "")), host(new long[] {0}, false, true));
            assertEquals(Process.State.FINISHED, on.state(), () -> String.valueOf(on.message()));
        } finally {
            ComponentRules.install(null);
        }
    }

    @Test
    void textMode_opensWindowsAndRefusesWhatLettersCannotShow() {
        final Process letters = start(load(page("""
                show(new Label("in letters"));
                """, "", "")), host(new long[] {0}, true, false));
        assertEquals(Process.State.FINISHED, letters.state(), () -> String.valueOf(letters.message()));
        assertEquals(1, letters.windows().size());

        final Process picture = start(load(page("""
                Image photo = new Image("/a.pix");
                """, "", "")), host(new long[] {0}, true, false));
        assertEquals(Process.State.HALTED, picture.state());
        assertTrue(picture.message().english().contains("there is no Image on a screen of letters"),
                String.valueOf(picture.message()));
    }

    @Test
    void save_bringsBackTheNewWidgetsAndWhatTheyHold() {
        final ProgramImage program = load(page("""
                Table table = new Table();
                table.AddColumn("Name");
                table.AddRow("a", "b");
                TreeView tree = new TreeView();
                tree.Add("top");
                GenericComponent dial = new GenericComponent("tests:dial");
                Map<string, int> state = new Map<string, int>();
                state["angle"] = 90;
                dial.Data = state;
                Column all = new Column();
                all.Add(table);
                all.Add(tree);
                all.Add(dial);
                show(all);
                """, "", ""));
        final Process process = start(program, host(new long[] {0}, false, false));

        final Process restored = Process.restore(program, process.save(), host(new long[] {0}, false, false));

        assertEquals(List.of("a\tb"), ((Values.ListValue) widgetOf(restored, UiWidgets.TABLE)
                .get(UiWidgets.ITEMS)).items());
        assertEquals(List.of("top"), ((Values.ListValue) widgetOf(restored, UiWidgets.TREE_VIEW)
                .get(UiWidgets.ITEMS)).items());
        assertEquals("{s5:anglei90;}", ComponentValues.encode(widgetOf(restored, UiWidgets.GENERIC)
                .get(UiWidgets.DATA)));
    }

    /** A kind of component that takes everything but an action called "refused". */
    private record Rule(String id, boolean reachesOutside) implements IComponentRule {

        @Override
        public boolean takesData(final Object value) {
            return true;
        }

        @Override
        public boolean takesAction(final String name, final Object value) {
            return !"refused".equals(name);
        }
    }

    private static Process start(final String body) {
        return start(body, "", "");
    }

    private static Process start(final String body, final String methods, final String fields) {
        return start(load(page(body, methods, fields)), host(new long[] {0}, false, false));
    }

    /* A script whose OnInit runs the body; show(widget) puts it in a column in an open window. */
    private static String page(final String body, final String methods, final String fields) {
        return "class Panel : IScript { Window window; " + fields + " public void OnInit() { " + body + " } "
                + "void show(Widget shown) { Column page = new Column(); page.Add(shown); "
                + "window = new Window(\"Test\", 300, 200); window.Content = page; window.Show(); } "
                + "void Heard() { Console.PrintLine(\"heard\"); } " + methods
                + " public void OnTick() { } public void OnDestroy() { } }";
    }

    private static ProgramImage load(final String source) {
        final SigmaCompiler.Result built =
                SigmaCompiler.compile(List.of(new SourceFile("Panel.sgs", PRELUDE + source)));
        assertTrue(built.ok(), () -> String.join("\n", built.lines()));
        final AsmReader reader = new AsmReader(built.assembly());
        final AsmProgram program = reader.read();
        assertFalse(reader.hasProblems(), built::assembly);
        return ProgramImage.of(program);
    }

    private static Process start(final ProgramImage program, final IHost host) {
        final Process process = new Process(program, ROOM, host);
        process.begin(process.create(program.entryPoint()), "OnInit");
        process.step(PLENTY);
        return process;
    }

    /** A machine whose clock reads that tick, with a desktop or only letters, and outside components on or off. */
    private static IHost host(final long[] tick, final boolean letters, final boolean outside) {
        return new IHost() {
            @Override
            public long tick() {
                return tick[0];
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
            public boolean hasDesktop() {
                return !letters;
            }

            @Override
            public boolean textMode() {
                return letters;
            }

            @Override
            public boolean outsideComponents() {
                return outside;
            }
        };
    }

    private static Values.Obj widgetOf(final Process process, final String type) {
        for (final Values.Obj widget : UiWidgets.inside(process.windows().getFirst())) {
            if (type.equals(widget.type())) {
                return widget;
            }
        }
        return null;
    }
}
