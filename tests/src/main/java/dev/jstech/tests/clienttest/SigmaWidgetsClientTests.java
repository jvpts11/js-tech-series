/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.AddonDesktopApp;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.OsSkin;
import dev.jstech.computers.client.os.SigmaWindowApp;
import dev.jstech.computers.gui.CdeScheme;
import dev.jstech.computers.machine.MachinePrograms;
import dev.jstech.computers.operation.payload.UiWindowPayload;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.testkit.TestDesktopPrograms;
import dev.jstech.tests.testkit.TestWorldBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.lwjgl.glfw.GLFW;

/**
 * The widgets of the second version of Σ#: drawn by every system in its own look, answering a player on a desktop,
 * drawn in letters on a machine that only has its terminal, a component of another mod's kind drawn by that mod or by a
 * placeholder, and a program another mod wrote drawing its own picture for whoever opened it.
 */
public final class SigmaWidgetsClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 80;
    private static final int BOOT_WAIT = 400;
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final BlockPos PLAYER_AT_MONITOR = new BlockPos(8, 2, 2);

    /** Every system that draws a program's window, by the name its screenshot goes under. */
    private static final Map<String, OsSkin> LOOKS = Map.of(
            "frames_95", OsSkin.forDesktop(jsc("frames_95")),
            "frames_xp", OsSkin.forDesktop(jsc("frames_xp")),
            "frames_11", OsSkin.forDesktop(jsc("frames_11")),
            "kde_2", OsSkin.forDesktop(jsc("kde_plasma"), HardwareEra.LEGACY),
            "gnome_1", OsSkin.forDesktop(jsc("gnome"), HardwareEra.LEGACY),
            "kde_plasma", OsSkin.forDesktop(jsc("kde_plasma"), HardwareEra.STANDARD),
            "gnome", OsSkin.forDesktop(jsc("gnome"), HardwareEra.STANDARD),
            "cinnamon", OsSkin.forDesktop(jsc("cinnamon"), HardwareEra.STANDARD),
            "cde", OsSkin.motif(CdeScheme.DEFAULT));
    /** The kinds the gallery window holds, each of which has to land inside its room in every look. */
    private static final List<String> SHOWN = List.of("MenuBar", "TabView", "GroupBox", "TextArea", "NumberBox",
            "Slider", "RadioGroup", "ComboBox", "Table", "TreeView", "Chart", "Image", "LogView", "ItemSlot",
            "ItemPicker", "OperationView", "GenericComponent", "StatusBar");

    /** One of every widget the second version brought, laid out the way the approved gallery has them. */
    private static final String GALLERY = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Gallery : IScript {
                Window window;
                StatusBar status;
                ComboBox combo;
                RadioGroup radios;
                TabView tabs;
                NumberBox copies;
                Table table;
                ContextMenu menu;
                MenuBar bar;
                TreeView tree;
                TextArea notes;
                GenericComponent dial;
                ItemPicker picker;
                OpenFileDialog opener;
                int at = 90;
                public void OnInit() {
                    bar = new MenuBar();
                    bar.Add("File", "Open");
                    bar.Add("File", "Exit");
                    bar.Add("Edit", "Copy");
                    bar.Add("View", "Details");
                    bar.Add("Help", "About");
                    bar.OnPick += Barred;
                    notes = new TextArea("Back up every night");
                    notes.OnChange += Noted;
                    copies = new NumberBox(1, 9);
                    copies.Value = 7;
                    copies.OnChange += Counted;
                    Slider speed = new Slider(0, 100);
                    speed.Value = 60;
                    radios = new RadioGroup();
                    radios.Add("Fast");
                    radios.Add("Normal");
                    radios.Add("Quiet");
                    radios.Selected = 1;
                    radios.OnSelect += Chose;
                    combo = new ComboBox();
                    combo.Add("Vintage");
                    combo.Add("Legacy");
                    combo.Selected = 2;
                    combo.OnSelect += Combed;
                    Button open = new Button("Open...");
                    open.OnClick += Opened;
                    Column controls = new Column();
                    controls.Add(notes);
                    controls.Add(row("Copies", copies));
                    controls.Add(row("Speed", speed));
                    controls.Add(radios);
                    controls.Add(row("Era", combo));
                    controls.Add(open);
                    GroupBox backup = new GroupBox("Backup");
                    backup.Content = controls;
                    tabs = new TabView();
                    tabs.Add("Controls", backup);
                    tabs.Add("Data", new Label("Nothing here"));
                    tabs.OnSelect += Tabbed;

                    table = new Table();
                    table.AddColumn("Name");
                    table.AddColumn("Size");
                    table.AddRow("backup.sh", "1 KB");
                    table.AddRow("notes.txt", "2 KB");
                    table.AddRow("photo.pix", "212 KB");
                    table.OnSelect += Rowed;
                    menu = new ContextMenu(table);
                    menu.Add("Open");
                    menu.Add("Rename");
                    menu.OnPick += Menued;
                    tree = new TreeView();
                    int pc = tree.Add("This PC");
                    int disk = tree.Add("Local Disk (C:)", pc);
                    tree.Add("Programs", disk);
                    tree.Add("Backup (D:)", pc);
                    tree.OnSelect += Noded;
                    Chart cpu = new Chart(0, 100);
                    cpu.Text = "CPU, last minute";
                    for (int i = 0; i < 20; i = i + 1) { cpu.Add(30 + (i * 7) % 40); }

                    Image photo = new Image("/photo.pix");
                    photo.Height = 24;
                    LogView log = new LogView();
                    log.Add("10:14:02 INFO Backup started");
                    log.Add("10:15:31 WARN Disk D: is 90% full");
                    log.Add("10:15:44 ERROR photo.pix is locked");
                    log.Height = 32;
                    Row parts = new Row();
                    parts.Add(new ItemSlot("minecraft:diamond", 64));
                    parts.Add(new ItemSlot("minecraft:redstone", 12));
                    parts.Add(new ItemSlot());
                    picker = new ItemPicker();
                    picker.Add("minecraft:diamond");
                    picker.Add("minecraft:redstone");
                    picker.Add("minecraft:iron_ingot");
                    picker.Add("minecraft:gold_ingot");
                    picker.OnSelect += Picked;
                    OperationView view = new OperationView();
                    view.Text = "SELECT 64 diamond FROM storage";
                    view.State = "PROCESSING";
                    view.Done = 3;
                    view.Total = 5;
                    view.Computer = "Mainframe";
                    dial = new GenericComponent("jstests:dial");
                    dial.Data = at;
                    dial.OnAction(Turned);
                    dial.Height = 24;
                    GenericComponent gone = new GenericComponent("gone:dial");
                    gone.Height = 24;
                    Row components = new Row();
                    components.Add(dial, 1);
                    components.Add(gone, 1);

                    Column middle = new Column();
                    middle.Add(table);
                    middle.Add(tree);
                    middle.Add(cpu);
                    Column right = new Column();
                    right.Add(photo);
                    right.Add(log);
                    right.Add(parts);
                    right.Add(picker);
                    right.Add(view);
                    right.Add(components);
                    Row body = new Row();
                    body.Add(tabs, 1);
                    body.Add(middle, 1);
                    body.Add(right, 1);
                    status = new StatusBar("Ready");
                    status.Add("4 files");
                    status.Add("12:30");
                    Column page = new Column();
                    page.Add(bar);
                    page.Add(body, 1);
                    page.Add(status);
                    window = new Window("Widget Gallery", 460, 250);
                    window.Content = page;
                    window.Show();
                    opener = new OpenFileDialog("Open a file");
                    opener.Path = "/";
                    opener.OnCancel += Cancelled;
                }
                Row row(string said, Widget widget) {
                    Row line = new Row();
                    line.Add(new Label(said));
                    line.Add(widget, 1);
                    return line;
                }
                void Barred() { status.Text = "bar " + bar.PickedMenu + "/" + bar.Picked; }
                void Noted() { status.Text = "notes " + notes.Text; }
                void Counted() { status.Text = "copies " + copies.Value; }
                void Chose() { status.Text = "radio " + radios.Selected; }
                void Combed() { status.Text = "combo " + combo.Selected; }
                void Tabbed() { status.Text = "tab " + tabs.Selected; }
                void Rowed() { status.Text = "row " + table.Selected; }
                void Menued() { status.Text = "menu " + menu.Picked; }
                void Noded() { status.Text = "node " + tree.Selected; }
                void Picked() { status.Text = "picked " + picker.Picked; }
                void Opened() { opener.Show(window); }
                void Cancelled() { status.Text = "cancelled"; }
                void Turned(ComponentAction action) {
                    at = at + (int) action.Value;
                    dial.Data = at;
                    status.Text = "dial " + at;
                }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    /** The same widgets in a window small enough for a monitor's desktop, for the player to work every one of. */
    private static final String PANEL = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Panel : IScript {
                Window window;
                StatusBar status;
                ComboBox combo;
                RadioGroup radios;
                TabView tabs;
                NumberBox copies;
                Table table;
                ContextMenu menu;
                MenuBar bar;
                TreeView tree;
                TextArea notes;
                GenericComponent dial;
                ItemPicker picker;
                OpenFileDialog opener;
                int at = 90;
                public void OnInit() {
                    bar = new MenuBar();
                    bar.Add("File", "Open");
                    bar.Add("Edit", "Copy");
                    bar.OnPick += Barred;
                    combo = new ComboBox();
                    combo.Add("Vintage");
                    combo.Add("Legacy");
                    combo.Selected = 2;
                    combo.OnSelect += Combed;
                    radios = new RadioGroup();
                    radios.Add("Fast");
                    radios.Add("Normal");
                    radios.Add("Quiet");
                    radios.Across = false;
                    radios.OnSelect += Chose;
                    copies = new NumberBox(1, 9);
                    copies.Value = 7;
                    copies.OnChange += Counted;
                    notes = new TextArea("Back up");
                    notes.Height = 28;
                    notes.OnChange += Noted;
                    Button open = new Button("Open...");
                    open.OnClick += Opened;
                    Column controls = new Column();
                    controls.Add(combo);
                    controls.Add(radios);
                    controls.Add(copies);
                    controls.Add(notes);
                    controls.Add(open);
                    tabs = new TabView();
                    tabs.Add("Controls", controls);
                    tabs.Add("Data", new Label("Nothing here"));
                    tabs.OnSelect += Tabbed;
                    table = new Table();
                    table.AddColumn("Name");
                    table.AddColumn("Size");
                    table.AddRow("backup.sh", "1 KB");
                    table.AddRow("notes.txt", "2 KB");
                    table.Height = 50;
                    table.OnSelect += Rowed;
                    menu = new ContextMenu(table);
                    menu.Add("Open");
                    menu.Add("Rename");
                    menu.OnPick += Menued;
                    tree = new TreeView();
                    int pc = tree.Add("This PC");
                    int disk = tree.Add("Local Disk (C:)", pc);
                    tree.Add("Programs", disk);
                    tree.Height = 50;
                    tree.OnSelect += Noded;
                    picker = new ItemPicker();
                    picker.Add("minecraft:diamond");
                    picker.Add("minecraft:redstone");
                    picker.Add("minecraft:iron_ingot");
                    picker.OnSelect += Picked;
                    dial = new GenericComponent("jstests:dial");
                    dial.Data = at;
                    dial.Height = 24;
                    dial.OnAction(Turned);
                    Column middle = new Column();
                    middle.Add(table);
                    middle.Add(tree);
                    Column right = new Column();
                    right.Add(picker);
                    right.Add(dial);
                    Row body = new Row();
                    body.Add(tabs, 1);
                    body.Add(middle, 1);
                    body.Add(right, 1);
                    status = new StatusBar("Ready");
                    Column page = new Column();
                    page.Add(bar);
                    page.Add(body, 1);
                    page.Add(status);
                    window = new Window("Panel", 360, 190);
                    window.Content = page;
                    window.Show();
                    opener = new OpenFileDialog("Open a file");
                    opener.Path = "/";
                    opener.OnCancel += Cancelled;
                }
                void Barred() { status.Text = "bar " + bar.PickedMenu + "/" + bar.Picked; }
                void Noted() { status.Text = "notes " + notes.Text; }
                void Counted() { status.Text = "copies " + copies.Value; }
                void Chose() { status.Text = "radio " + radios.Selected; }
                void Combed() { status.Text = "combo " + combo.Selected; }
                void Tabbed() { status.Text = "tab " + tabs.Selected; }
                void Rowed() { status.Text = "row " + table.Selected; }
                void Menued() { status.Text = "menu " + menu.Picked; }
                void Noded() { status.Text = "node " + tree.Selected; }
                void Picked() { status.Text = "picked " + picker.Picked; }
                void Opened() { opener.Show(window); }
                void Cancelled() { status.Text = "cancelled"; }
                void Turned(ComponentAction action) {
                    at = at + (int) action.Value;
                    dial.Data = at;
                    status.Text = "dial " + at;
                }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    /** A form drawn in letters on a machine with only its terminal. */
    private static final String LETTERS = """
            using System.*;
            using System.UI.*;
            namespace Art;
            class Letters : IScript {
                Window window;
                StatusBar status;
                TextBox name;
                public void OnInit() {
                    MenuBar bar = new MenuBar();
                    bar.Add("File", "Open");
                    bar.Add("File", "Exit");
                    name = new TextBox("");
                    name.OnChange += Typed;
                    CheckBox loud = new CheckBox("Loud");
                    RadioGroup speed = new RadioGroup();
                    speed.Add("Fast");
                    speed.Add("Slow");
                    ListBox list = new ListBox();
                    list.Add("backup.sh");
                    list.Add("notes.txt");
                    Button ok = new Button("OK");
                    ok.OnClick += Pressed;
                    status = new StatusBar("Ready");
                    status.Add("12:30");
                    Row line = new Row();
                    line.Add(new Label("Name:"));
                    line.Add(name, 1);
                    Column page = new Column();
                    page.Add(bar);
                    page.Add(line);
                    page.Add(loud);
                    page.Add(speed);
                    page.Add(list);
                    page.Add(ok);
                    page.Add(status);
                    window = new Window("Backup", 240, 160);
                    window.Content = page;
                    window.Show();
                }
                void Typed() { status.Text = "typed " + name.Text; }
                void Pressed() { status.Text = "pressed"; }
                public void OnTick() { }
                public void OnDestroy() { window.Close(); }
            }
            """;

    private SigmaWidgetsClientTests() {
    }

    /**
     * The same window in the look of each system that draws one: every widget lands inside the window's room, and a
     * screenshot of each look is taken for the eye.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void gallery_isDrawnByEverySystemInItsOwnLook(final ClientTestContext ctx) {
        final AtomicReference<UiWindowPayload> gallery = new AtomicReference<>();
        ctx.thenBuild(0, world -> world.placeRunningCraftingComputer(COMPUTER))
                .thenServer(SETTLE, level -> gallery.set(run(level, ctx, GALLERY)))
                .thenWaitUntil(() -> gallery.get() != null, SCREEN_WAIT, "the gallery program to open its window")
                .then(0, () -> ctx.mc().setScreen(new SigmaGalleryScreen(gallery.get())));
        for (final Map.Entry<String, OsSkin> look : LOOKS.entrySet()) {
            ctx.then(SETTLE, () -> ctx.screen(SigmaGalleryScreen.class).show(look.getValue()))
                    .then(2, () -> {
                        final String out = outside(ctx.screen(SigmaGalleryScreen.class));
                        ctx.assertTrue(out.isEmpty(), "every widget lands inside the window in " + look.getKey()
                                + "; outside: " + out);
                    })
                    .thenScreenshot(1, "gallery_" + look.getKey());
        }
        ctx.then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    /**
     * On a desktop, every widget answers the player: a combo box's list, a tab, a choice, a number's steps, a row of a
     * table and its context menu, a menu bar, a tree, a text area, an item picker, the dial another mod draws, and the
     * system's own dialog shut without an answer.
     */
    @ClientTest(timeoutTicks = 4000)
    public static void widgets_answerThePlayerOnADesktop(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.placeRunningCraftingComputer(COMPUTER);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenServer(SETTLE, level -> run(level, ctx, PANEL))
                .thenWaitUntil(() -> window(ctx) != null, SCREEN_WAIT, "the panel to open on the desktop")
                .thenScreenshot(SETTLE, "desktop")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).pointAt("ComboBox")))
                .thenWaitUntil(() -> window(ctx).popupOpen(), SCREEN_WAIT, "the combo box's list to drop")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).popupEntryPoint(0)))
                .thenWaitUntil(() -> said(ctx, "combo 1"), SCREEN_WAIT, "the choice to reach the program")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).entryPoint("RadioGroup", 2)))
                .thenWaitUntil(() -> said(ctx, "radio 3"), SCREEN_WAIT, "the third choice to reach the program")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).entryPoint("NumberBox", 0)))
                .thenWaitUntil(() -> said(ctx, "copies 8"), SCREEN_WAIT, "the step up to reach the program")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).entryPoint("Table", 1)))
                .thenWaitUntil(() -> said(ctx, "row 2"), SCREEN_WAIT, "the row to reach the program")
                .then(SETTLE, () -> ctx.rightClickDesktop(window(ctx).entryPoint("Table", 1)))
                .thenWaitUntil(() -> window(ctx).popupOpen(), SCREEN_WAIT, "the context menu to open")
                .thenScreenshot(1, "context_menu")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).popupEntryPoint(1)))
                .thenWaitUntil(() -> said(ctx, "menu Rename"), SCREEN_WAIT, "the menu's entry to reach the program")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).entryPoint("MenuBar", 1)))
                .thenWaitUntil(() -> window(ctx).popupOpen(), SCREEN_WAIT, "the Edit menu to drop")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).popupEntryPoint(0)))
                .thenWaitUntil(() -> said(ctx, "bar Edit/Copy"), SCREEN_WAIT, "the menu bar's entry to reach it")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).entryPoint("TreeView", 1)))
                .thenWaitUntil(() -> said(ctx, "node 2"), SCREEN_WAIT, "the node to reach the program")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).entryPoint("ItemPicker", 2)))
                .thenWaitUntil(() -> said(ctx, "picked minecraft:iron_ingot"), SCREEN_WAIT,
                        "the item picked to reach the program")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).pointAt("GenericComponent")))
                .thenWaitUntil(() -> said(ctx, "dial " + (90 + TestClientParts.TURN_BY)), SCREEN_WAIT,
                        "the dial another mod draws to tell the program it was turned")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).pointAt("TextArea")))
                .then(1, () -> ctx.type("!"))
                .thenWaitUntil(() -> said(ctx, "notes Back up!"), SCREEN_WAIT,
                        "what is typed in the text area to reach the program")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).entryPoint("TabView", 1)))
                .thenWaitUntil(() -> said(ctx, "tab 2"), SCREEN_WAIT, "the second tab to reach the program")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).entryPoint("TabView", 0)))
                .thenWaitUntil(() -> window(ctx).pickedOf("TabView") == 1, SCREEN_WAIT, "the first tab again")
                .then(SETTLE, () -> ctx.clickDesktop(window(ctx).pointAt("Button")))
                .thenWaitUntil(() -> window(ctx).openDialog() != null, SCREEN_WAIT, "the system's dialog to open")
                .thenScreenshot(SETTLE, "file_dialog")
                .then(SETTLE, () -> window(ctx).openDialog().close())
                .thenWaitUntil(() -> said(ctx, "cancelled"), SCREEN_WAIT, "the dialog shut to reach the program");
    }

    /** On a machine with only its terminal, the window is drawn in letters and answers the keyboard. */
    @ClientTest(timeoutTicks = 3000)
    public static void window_isDrawnInLettersOnATerminal(final ClientTestContext ctx) {
        ctx.thenBuild(0, world -> {
                    world.setBlock(COMPUTER, ComputingModule.CRAFTING_COMPUTER.get());
                    final CraftingComputerBlockEntity computer =
                            world.blockEntity(COMPUTER, CraftingComputerBlockEntity.class);
                    final ItemStackHandler hw = computer.getHardware();
                    hw.setStackInSlot(CraftingComputerBlockEntity.MOTHERBOARD_SLOT,
                            new ItemStack(HardwareItems.MOTHERBOARD_ATX_STANDARD_LGA1150.get()));
                    hw.setStackInSlot(CraftingComputerBlockEntity.CPU_SLOT,
                            new ItemStack(HardwareItems.CPU_INTEGRA_CENTRO_C7_4790K.get()));
                    hw.setStackInSlot(CraftingComputerBlockEntity.RAM_SLOTS_START,
                            new ItemStack(ComputingModule.RAM_DDR3_8192.get()));
                    hw.setStackInSlot(CraftingComputerBlockEntity.PSU_SLOT,
                            new ItemStack(ComputingModule.PSU_650G.get()));
                    hw.setStackInSlot(CraftingComputerBlockEntity.DISK_SLOTS_START,
                            new ItemStack(ComputingModule.disk(StorageTier.SSD, DiskSize.GB_500)));
                    computer.installOs(jsc("mc_dos"));
                    computer.togglePower();
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .thenServer(SETTLE * 4, level -> run(level, ctx, LETTERS))
                .thenWaitUntil(() -> letters(ctx) != null, SCREEN_WAIT, "the window to take the terminal")
                .thenAssert(SETTLE, () -> letters(ctx).text().contains("Backup")
                        && letters(ctx).text().contains("< OK >"), "the window is drawn in letters, its button too")
                .thenAssert(0, () -> letters(ctx).text().contains("Ready"), "with its status line along the bottom")
                .thenScreenshot(1, "letters")
                .then(SETTLE, () -> ctx.type("hi"))
                .thenWaitUntil(() -> letters(ctx).text().contains("typed hi"), SCREEN_WAIT,
                        "what is typed to reach the program and come back on its status line");
        for (int tab = 0; tab < 4; tab++) {
            ctx.then(1, () -> ctx.key(GLFW.GLFW_KEY_TAB));
        }
        ctx.then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> letters(ctx).text().contains("pressed"), SCREEN_WAIT,
                        "Enter on the button to press it")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenWaitUntil(() -> letters(ctx) == null, SCREEN_WAIT, "Escape to shut the window")
                .thenAssert(SETTLE, () -> ctx.screen(CommandPromptScreen.class) != null,
                        "and the terminal is the prompt's again");
    }

    /**
     * A program another mod wrote draws its own picture, no faster than its renderer allows, for whoever opened it;
     * the same window opened any other way shows the placeholder.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void addonProgram_drawsItsPictureForWhoeverOpenedIt(final ClientTestContext ctx) {
        final int[] framesBefore = new int[1];
        ctx.thenBuild(0, world -> {
                    world.placeRunningCraftingComputer(COMPUTER);
                    world.placeMonitor(MONITOR, Direction.EAST);
                })
                .thenTeleport(SETTLE, PLAYER_AT_MONITOR, Direction.WEST)
                .thenRightClick(SETTLE, MONITOR)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> DesktopScreen.requestOpen(TestDesktopPrograms.PIXELS.toString()))
                .thenWaitUntil(() -> pixels(ctx) != null, SCREEN_WAIT, "the program's window to open")
                .thenAssert(SETTLE, () -> pixels(ctx).showsHere(), "it runs for the player who opened it")
                .then(SETTLE, () -> framesBefore[0] = TestClientParts.FRAMES.get())
                .thenAssert(40, () -> {
                    final int drawn = TestClientParts.FRAMES.get() - framesBefore[0];
                    return drawn > 0 && drawn <= TestClientParts.FRAME_CAP * 2 + 2;
                }, "it is asked for frames, and no more than its cap allows")
                .thenScreenshot(1, "pixels")
                .thenAssert(0, () -> !new AddonDesktopApp(TestDesktopPrograms.PIXELS,
                        new TestClientParts.Probe()).showsHere(),
                        "a window opened any other way shows the placeholder");
    }

    /* Starts a program on the machine and runs it until its window is up, handing back the window as it goes over. */
    private static UiWindowPayload run(final ServerLevel level, final ClientTestContext ctx, final String source) {
        final CraftingComputerBlockEntity computer = TestWorldBuilder.at(level, ctx.origin())
                .blockEntity(COMPUTER, CraftingComputerBlockEntity.class);
        final MachinePrograms.Started started = computer.programs().start("gallery.sgs", source, 1, computer);
        ctx.assertTrue(started.ok(), "the program starts: " + started.message());
        computer.programs().tick(1_000_000);
        final var windows = computer.programs().windowsOf(started.id());
        ctx.assertTrue(!windows.isEmpty(), "the program has its window; it says "
                + computer.programs().byId(started.id()).process().message().english());
        return UiWindowPayload.of(computer.getBlockPos(), started.id(), windows.getFirst());
    }

    /* The widgets the gallery shows that did not land inside the window's room, with where they landed. */
    private static String outside(final SigmaGalleryScreen screen) {
        final int[] room = screen.room();
        final StringBuilder out = new StringBuilder();
        for (final String kind : SHOWN) {
            final int[] at = screen.app().boundsOf(kind);
            if (at == null || at[0] < room[0] || at[1] < room[1] || at[0] + at[2] > room[0] + room[2]
                    || at[1] + at[3] > room[1] + room[3]) {
                out.append(kind).append(at == null ? " (not placed)" : " " + Arrays.toString(at)).append(' ');
            }
        }
        return out.length() == 0 ? "" : out + "room " + Arrays.toString(room);
    }

    private static boolean said(final ClientTestContext ctx, final String expected) {
        final SigmaWindowApp window = window(ctx);
        return window != null && expected.equals(window.said("StatusBar"));
    }

    private static SigmaWindowApp window(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        return desktop == null ? null : desktop.programWindow();
    }

    private static TextScreen letters(final ClientTestContext ctx) {
        final CommandPromptScreen<?> prompt = ctx.screen(CommandPromptScreen.class);
        return prompt == null ? null : prompt.windowsScreen();
    }

    private static AddonDesktopApp pixels(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.screen(DesktopScreen.class);
        final var window = desktop == null ? null : desktop.windowFor("Pixels");
        return window != null && window.app() instanceof AddonDesktopApp app ? app : null;
    }

    private static ResourceLocation jsc(final String path) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path);
    }
}
