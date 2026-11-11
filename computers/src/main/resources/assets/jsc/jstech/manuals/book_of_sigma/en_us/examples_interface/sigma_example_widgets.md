---
section: examples_interface
order: 514
---
# Widgets

This example shows the rest of the widgets: text areas and number boxes, choices, tabs, tables, trees, menus, status bars, pictures, charts, logs, file dialogs, item slots, operations and components that a mod adds.

Some widgets draw pictures, charts, items or components. A machine with only its terminal cannot draw them, so making one there stops the program, and the message says so.

The handler is a method that you write and name on the widget.

Widgets have events such as OnChange, OnSelect and OnClick. Each one names the method called when the player acts.

## The program

```sgs Widgets.sgs
using System.*;
using System.IO.*;
using System.UI.*;
using System.Operations.*;
namespace Corpus;
class WidgetsCorpus {
    static void Main() {
        Window window = new Window("Widgets", 320, 200);
        Column column = new Column();

        TextArea area = new TextArea();
        TextArea notes = new TextArea("notes");
        notes.Text = "more";
        notes.OnChange += Heard;

        NumberBox numbers = new NumberBox();
        NumberBox copies = new NumberBox(1, 9);
        copies.Value = 3;
        copies.Least = 1;
        copies.Most = 9;
        copies.Step = 1;
        copies.OnChange += Heard;

        Slider slider = new Slider();
        Slider speed = new Slider(0, 100);
        speed.Value = 50;
        speed.Least = 0;
        speed.Most = 100;
        speed.Step = 5;
        speed.OnChange += Heard;

        RadioGroup radios = new RadioGroup();
        radios.Add("Fast");
        radios.Add("Slow");
        radios.Selected = 1;
        radios.Across = true;
        radios.OnSelect += Heard;

        ComboBox combo = new ComboBox();
        combo.Add("Legacy");
        combo.Selected = 1;
        combo.OnSelect += Heard;

        GroupBox group = new GroupBox();
        GroupBox backup = new GroupBox("Backup");
        backup.Text = "Backup";
        backup.Content = notes;
        TabView tabs = new TabView();
        tabs.Add("Controls", backup);
        tabs.Selected = 1;
        tabs.OnSelect += Heard;

        ScrollView scroll = new ScrollView();
        scroll.Content = area;

        Table table = new Table();
        table.AddColumn("Name");
        List<string> cells = new List<string>();
        cells.Add("a");
        table.AddRow(cells);
        table.AddRow("a");
        table.AddRow("a", "b");
        table.AddRow("a", "b", "c");
        table.AddRow("a", "b", "c", "d");
        table.Selected = 1;
        table.OnSelect += Heard;
        string cell = table.Cell(1, 1);

        TreeView tree = new TreeView();
        int top = tree.Add("This PC");
        int under = tree.Add("C:", top);
        string node = tree.NodeText(under);
        tree.Selected = under;
        tree.OnSelect += Heard;

        MenuBar bar = new MenuBar();
        bar.Add("File", "Open");
        bar.OnPick += Heard;
        ContextMenu menu = new ContextMenu(table);
        menu.Add("Rename");
        menu.OnPick += Heard;

        StatusBar status = new StatusBar();
        StatusBar ready = new StatusBar("Ready");
        ready.Text = "Ready";
        ready.Add("12:30");

        Image image = new Image();
        Image photo = new Image("/photo.pix");
        photo.Path = "/photo.pix";

        Chart chart = new Chart();
        Chart cpu = new Chart(0, 100);
        cpu.Text = "CPU";
        cpu.Least = 0;
        cpu.Most = 100;
        cpu.Add(61.0);

        LogView log = new LogView();
        log.Add("10:14 INFO started");

        OpenFileDialog open = new OpenFileDialog();
        OpenFileDialog opener = new OpenFileDialog("Open");
        opener.Title = "Open";
        opener.Path = "/";
        opener.Filter = "*.txt";
        opener.OnChoose += Heard;
        opener.OnCancel += Heard;
        SaveFileDialog save = new SaveFileDialog();
        SaveFileDialog saver = new SaveFileDialog("Save");
        saver.Title = "Save";
        saver.Path = "/";
        saver.Filter = "*.txt";
        saver.Name = "a.txt";
        saver.OnChoose += Heard;
        saver.OnCancel += Heard;

        ItemSlot slot = new ItemSlot();
        ItemSlot diamond = new ItemSlot("minecraft:diamond");
        ItemSlot stack = new ItemSlot("minecraft:diamond", 64);
        stack.Item = "minecraft:diamond";
        stack.Amount = 64;
        stack.OnClick += Heard;

        ItemPicker picker = new ItemPicker();
        picker.Add("minecraft:diamond");
        picker.Selected = 1;
        picker.OnSelect += Heard;

        OperationView view = new OperationView();
        view.Text = "SELECT 64 diamond";
        view.State = "PROCESSING";
        view.Done = 3;
        view.Total = 5;
        view.Computer = "Mainframe";
        view.Show(Operations.Get("op"));

        GenericComponent dial = new GenericComponent("corpus:dial");
        dial.Data = 5;
        dial.OnAction(Acted);

        column.Add(bar);
        column.Add(tabs, 1);
        column.Add(scroll);
        column.Add(copies);
        column.Add(speed);
        column.Add(radios);
        column.Add(combo);
        column.Add(table);
        column.Add(tree);
        column.Add(photo);
        column.Add(cpu);
        column.Add(log);
        column.Add(stack);
        column.Add(picker);
        column.Add(view);
        column.Add(dial);
        column.Add(ready);
        window.Content = column;
        window.Show();
        opener.Show(window);
        saver.Show(window);
        Console.PrintLine(cell + node + radios.Count + combo.Count + tabs.Count + table.Count + tree.Count
            + cpu.Count + log.Count + picker.Count + picker.Picked + bar.Picked + bar.PickedMenu + menu.Picked
            + dial.Kind + dial.Data + view.Done + numbers.Value + slider.Value + group.Text + status.Text
            + image.Path + chart.Count + open.Path + save.Name + slot.Amount);
        radios.Clear();
        combo.Clear();
        tabs.Clear();
        table.Clear();
        tree.Clear();
        bar.Clear();
        menu.Clear();
        ready.Clear();
        cpu.Clear();
        log.Clear();
        picker.Clear();
    }

    static void Heard() {
    }

    static void Acted(ComponentAction action) {
        Console.PrintLine(action.Name + action.Value);
    }
}
```

## The widgets

Table: Widgets and what they do

| Widget | What it does |
| --- | --- |
| `TextArea` | A box for several lines of text. OnChange runs when its text changes. |
| `NumberBox, Slider` | A whole number between a least and a most, moved a step at a time. |
| `RadioGroup` | A set of choices, one of which is picked. Selected is the picked choice, counted from 1, and 0 for none. |
| `ComboBox` | A box with a list of choices. Selected is the picked choice. |
| `GroupBox` | A box with a caption, and a widget inside it. |
| `TabView` | Tabs, each with a title and a widget. The first one added shows first. |
| `ScrollView` | Shows a widget that is taller than its room, and the player scrolls it. |
| `Table` | Rows and columns of text. Cell gives the text in one cell. A table holds at most 8 columns. |
| `TreeView` | A tree of text. Add gives back the number of the new node, and NodeText gives its text. |
| `MenuBar` | A bar of menus with entries. Picked and PickedMenu tell what the player picked. |
| `ContextMenu` | A menu that opens on its widget when the player clicks it with the other button. |
| `StatusBar` | A line of text, with a few sections at its right. |
| `Image` | A picture read from the machine's disk, as the paint program saves it. |
| `Chart` | A line drawn through the numbers added to it, between a least and a most. |
| `LogView` | Lines one under another, the newest at the bottom. A line with WARN or ERROR is picked out. |
| `OpenFileDialog, SaveFileDialog` | A dialog in which the player chooses a file. Show opens it on a window. |
| `ItemSlot` | One slot that holds an item, shown by its item id and how many. |
| `ItemPicker` | A grid of items with a search line above it. The player picks one. |
| `OperationView` | Shows an Operation: its text, its state, how much is done, and its computer. |
| `GenericComponent` | A widget that a mod adds, named by an id such as corpus:dial. OnAction runs when it acts. |

> [!DEFINE] Event
> Something that happens, such as a click or a change, which a method can answer.

> [!DEFINE] Generic component
> A widget that a mod adds, identified by its own id, such as mod:name.

> [!DEFINE] Item id
> The name of an item in the game, such as minecraft:diamond.

## If something goes wrong

> [!PROBLEM] A picture, a chart, an item slot or a component is made on a machine with only its terminal, and the program stops.
> Run the program on a machine that draws its windows, not only its terminal.

> [!PROBLEM] A window holds more than 256 widgets, and the program stops with "a window holds at most 256 widgets, counting the ones inside rows and columns".
> Put fewer widgets in the window, and count the ones inside rows and columns.

> [!PROBLEM] A table gets more than 8 columns, and the program stops with "a table has at most 8 columns".
> Use at most 8 columns in a table.

> [!PROBLEM] A menu gets more than 64 entries, and the program stops with "a menu holds at most 64 entries".
> Keep each menu to 64 entries or fewer.

> [!PROBLEM] A list gets more than 1024 rows, and the program stops with "a list holds at most 1024 rows".
> Keep each list to 1024 rows or fewer.

> [!SEE] jsc:sigma_example_windows, jsc:operations, jsc:soundfoundry, jsc:sigma_library
