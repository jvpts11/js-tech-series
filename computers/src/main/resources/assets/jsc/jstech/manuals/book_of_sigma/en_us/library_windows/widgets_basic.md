---
section: library_windows
order: 471
---
# Labels, buttons, boxes and bars

A widget is one part of a window. The simplest ones show text, and the others take what the player types or clicks. Making a widget costs nothing. Changing a value that the player sees costs credits, see [](jsc:call_costs).

Table: Basic widgets

| Widget | Makes | Values | Events |
| --- | --- | --- | --- |
| `Label` | `Label.new(), Label.new(text)` | `Label.Text` | none |
| `Button` | `Button.new(), Button.new(text)` | `Button.Text` | `Button.OnClick` |
| `TextBox` | `TextBox.new(), TextBox.new(text)` | `TextBox.Text` | `TextBox.OnChange, TextBox.OnSubmit` |
| `CheckBox` | `CheckBox.new(), CheckBox.new(text, checked)` | `CheckBox.Text, CheckBox.Checked` | `CheckBox.OnToggle` |
| `ProgressBar` | `ProgressBar.new(), ProgressBar.new(least, most)` | `ProgressBar.Value, ProgressBar.Least, ProgressBar.Most` | none |

An event tells your program that something happened, and it comes with no argument. OnChange runs on every change to a text box. OnSubmit runs when the player presses Enter in the box.

A text box keeps at most 256 characters of text.

## If something goes wrong

> [!PROBLEM] The text box stops taking characters.
> Shorten the text, or split it between two boxes.

> [!PROBLEM] The program does not react when the player presses Enter.
> Listen for OnSubmit, which runs when the player presses Enter in the box.

> [!SEE] jsc:windows_and_layout, jsc:widgets_lists
