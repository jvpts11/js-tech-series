---
section: examples_interface
order: 513
---
# Windows

A window is a box on the desktop, with a title. This example builds a window, puts a row and a column of widgets in it, and shows it. A widget is a piece of a window, such as a label, a button or a box to type in. You make a window with a title, a width and a height.

The machine's own system draws the widgets, so the same program looks like the system it runs under. The example closes its window near the end of Main, and then shows a message box.

## The program

```sgs Ui.sgs
using System.*;
using System.IO.*;
using System.UI.*;
namespace Corpus;
class UiCorpus {
    static void Main() {
        Window window = new Window("Corpus", 240, 150);
        window.Title = "Corpus";
        Row row = new Row();
        Column column = new Column();
        row.Spacing = 2;
        column.Spacing = 2;

        Label label = new Label();
        Label named = new Label("name");
        named.Text = "name";
        named.Visible = true;
        named.Enabled = true;
        named.Width = 40;
        named.Height = 10;

        Button button = new Button();
        Button pressed = new Button("press");
        pressed.Text = "press";
        pressed.OnClick += Clicked;

        TextBox box = new TextBox();
        TextBox typed = new TextBox("x");
        typed.Text = "y";
        typed.OnChange += Clicked;
        typed.OnSubmit += Clicked;

        CheckBox check = new CheckBox();
        CheckBox labelled = new CheckBox("on");
        CheckBox ticked = new CheckBox("on", true);
        ticked.Text = "on";
        ticked.Checked = false;
        ticked.OnToggle += Clicked;

        ProgressBar bar = new ProgressBar();
        ProgressBar ranged = new ProgressBar(0, 10);
        ranged.Value = 5;
        ranged.Least = 0;
        ranged.Most = 10;

        ListBox listBox = new ListBox();
        listBox.Add("one");
        listBox.Add("two", "second");
        listBox.Selected = 0;
        listBox.OnSelect += Clicked;

        Canvas canvas = new Canvas();
        Canvas sized = new Canvas(100, 50);
        sized.Clear(0);
        sized.FillRect(0, 0, 10, 10, 1);
        sized.DrawLine(0, 0, 10, 10, 2);
        sized.DrawText("hi", 1, 1, 3);
        sized.SetPixel(2, 2, 4);
        sized.OnClick += Clicked;

        row.Add(label);
        row.Add(named, 1);
        column.Add(row);
        column.Add(sized, 1);
        column.Add(box);
        column.Add(check);
        column.Add(labelled);
        column.Add(bar);
        column.Add(canvas);
        window.Content = column;
        window.Add(button, 0, 0, 20, 10);
        window.OnClose += Clicked;
        window.Show();
        Console.PrintLine("" + window.Open + listBox.Count + sized.ClickX + sized.ClickY);
        row.Clear();
        column.Clear();
        listBox.Clear();
        window.Close();
        MessageBox.Show("title", "text");
    }

    static void Clicked() {
    }
}
```

## What the lines do

Table: Lines of the program

| Line | What it does |
| --- | --- |
| `new Window("Corpus", 240, 150)` | Makes a window with the title Corpus, a width of 240 and a height of 150. |
| `new Row(), new Column(), Spacing` | A row lines its widgets up across, and a column lines them up down. Spacing sets the room between them. |
| `new Label("name"), Text, Width, Height` | A label shows a text. Its size is set by Width and Height. |
| `new Button("press"), OnClick` | A button with a text on it. OnClick names the method called when the player clicks it. |
| `new TextBox("x"), OnChange, OnSubmit` | A box to type in. OnChange runs when its text changes, and OnSubmit when the player submits it. |
| `new CheckBox("on", true), Checked` | A tick box with a text. Checked says whether it is ticked. |
| `new ProgressBar(0, 10), Value, Least, Most` | A bar that shows a value between a least and a most. |
| `ListBox: Add, Selected, OnSelect` | A list of lines. Selected is the chosen line, and OnSelect runs when one is picked. |
| `new Canvas(100, 50), Clear, FillRect, DrawLine` | A blank area to draw on, 100 wide and 50 high. Each drawing call paints one shape or text in a colour from 0 to 15. |
| `DrawText, SetPixel, ClickX, ClickY` | Draw a text or one point. ClickX and ClickY give where the player last clicked. |
| `row.Add(named, 1)` | Adds the label with a weight of 1, which takes a share of the room left over. |
| `window.Content = column` | Makes the column the main content of the window. |
| `window.Add(button, 0, 0, 20, 10)` | Places the button at position 0, 0, with a width of 20 and a height of 10. |
| `window.OnClose` | Names the method called when the window closes. |
| `window.Show()` | Opens the window on the desktop and its taskbar. The window stays open after the tick that opened it. |
| `window.Open, window.Close()` | Open says whether the window is open. Close closes it. |
| `MessageBox.Show("title", "text")` | Shows a small box with a title and a line of text. |

> [!DEFINE] Window
> A box on the desktop, with a title and widgets inside it.

> [!DEFINE] Row
> A box that lines up its widgets across, one after another.

> [!DEFINE] Column
> A box that lines up its widgets down, one under another.

> [!DEFINE] Weight
> A number that gives a widget a share of the room left over. Only widgets with a weight share that room.

> [!DEFINE] Canvas
> A blank area that a program draws on, with shapes, lines and text.

> [!DEFINE] Handler
> A method that the program names, and that is called when something happens.

## If something goes wrong

> [!PROBLEM] A window holds more than 256 widgets, counting the ones inside rows and columns, and the program stops with "a window holds at most 256 widgets, counting the ones inside rows and columns".
> Put fewer widgets in the window. Widgets inside rows and columns count too, so remove some of those as well.

> [!PROBLEM] A row or a column holds more than 256 widgets, and the program stops with "a row or a column holds at most 256 widgets".
> Split the widgets between two rows or two columns.

> [!PROBLEM] A widget is put inside itself, and the program stops with "a widget cannot hold itself or a widget it is inside".
> Put each widget in one place only, and never inside itself.

> [!PROBLEM] A canvas gets more than 4,096 strokes, and the program stops with "a canvas holds at most 4096 strokes".
> Draw fewer shapes, or use a second canvas for the rest.

> [!SEE] jsc:sigma_example_widgets, jsc:sigma_library
