---
section: library_windows
order: 470
---
# Windows, rows and columns

A window is a box on the desktop, with a title and one widget inside it. A program makes a Window, puts a widget in it, and shows it. The taskbar lists the open windows. The screens that show them are explained in [](jsc:monitors) and [](jsc:desktops).

> [!DEFINE] Widget
> One part of a window, such as a button, a label or a list.

> [!DEFINE] Weight
> A row's or column's rule for sharing spare room among its widgets.

Table: Window members

| Member | What it does |
| --- | --- |
| `Window.new(title, width, height)` | Makes a window with that title and size. Sigma# only. |
| `Window.Title` | The title on the window. |
| `Window.Content` | The widget the window holds. |
| `Window.Open` | Yes while the window is shown. |
| `Window.Show()` | Shows the window on the desktop and in the taskbar. |
| `Window.Close()` | Closes the window. |
| `Window.Add(widget, x, y, width, height)` | Places a widget at an exact place in the window. |
| `Window.OnClose` | The event that runs when the window closes. |

## Rows and columns

A row puts its widgets side by side, and a column stacks them. A row shares its width among its widgets, and a column shares its height.

Table: Row and Column members

| Member | What it does |
| --- | --- |
| `Row.new()` | Makes an empty row. Column.new() makes an empty column. |
| `Row.Add(widget)` | Adds a widget at its own size. |
| `Row.Add(widget, weight)` | Adds a widget that shares the room left over, by its weight (1 or more). |
| `Row.Spacing` | The gap between the widgets, 4 by default. |
| `Row.Clear()` | Removes the widgets from the row. |
| `Column.Add` | The same as Row.Add, for a column. |
| `Column.Spacing` | The same as Row.Spacing, for a column. |
| `Column.Clear()` | Removes the widgets from the column. |

MessageBox.Show(title, text) opens a window the machine makes for a title and one line of text. Sigma# only.

## Widgets in general

Every widget has Widget.Visible and Widget.Enabled, which are yes or no, and Widget.Width and Widget.Height, which are whole numbers. A width or height of 0 takes what the widget needs. Any other value stays between 8 and the most the window allows.

Table: Limits of windows and widgets

| Thing | Most |
| --- | --- |
| Open windows for one program | 8 |
| Widgets in a window, counting those in rows and columns | 256 |
| Widgets in one row or column | 256 |
| Window width | 60 to 640, 240 by default |
| Window height | 60 to 360, 160 by default |

## If something goes wrong

> [!PROBLEM] The program stops and says a program may have 8 windows open at once.
> Close a window you no longer need with Window.Close(), before you show another.

> [!PROBLEM] The program stops and says a window holds at most 256 widgets.
> Use fewer widgets. Widgets inside rows and columns count too.

> [!PROBLEM] The program stops and says a row or a column cannot hold itself.
> Do not put a row or a column inside itself, or inside a row or column that it holds.

> [!PROBLEM] The program stops and says a widget cannot hold itself.
> Give each widget one place in the window, and never put it inside itself.

> [!PROBLEM] The program stops and says a dialog opens over a window that is open.
> Show the window first, then open the dialog over it.

> [!PROBLEM] The program stops and says this window already has a dialog open over it.
> Close the open dialog first, then open the next one.

> [!PROBLEM] The program stops and says a value is not a program's to write.
> Write only the values the program may write, such as the ones it made.

> [!PROBLEM] The program stops and says "a <kind> component does not take that value".
> Check the kind of widget. Each kind takes only its own values.

> [!SEE] jsc:monitors, jsc:desktops, jsc:widgets_basic
