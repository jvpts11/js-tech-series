---
section: library_windows
order: 472
---
# Lists, choices, tables, trees and menus

Lists and choices hold many entries, and the player picks one. The Selected value tells your program which one is picked. On a RadioGroup, a ComboBox or a TabView the count starts at 1, and 0 means nothing is picked on a RadioGroup or a ComboBox.

> [!DEFINE] Node
> An entry in a tree. A node can be the child of another node.

> [!DEFINE] Section
> A part of a status bar, with its own text.

Table: Lists and choices

| Widget | Makes | Members |
| --- | --- | --- |
| `ListBox` | `ListBox.new()` | `ListBox.Add(text), ListBox.Add(text, right text), ListBox.Clear, ListBox.Count, ListBox.Selected, ListBox.OnSelect` |
| `RadioGroup` | `RadioGroup.new()` | `RadioGroup.Add, RadioGroup.Clear, RadioGroup.Count, RadioGroup.Selected, RadioGroup.Across, RadioGroup.OnSelect` |
| `ComboBox` | `ComboBox.new()` | `ComboBox.Add, ComboBox.Clear, ComboBox.Count, ComboBox.Selected, ComboBox.OnSelect` |
| `TabView` | `TabView.new()` | `TabView.Add(title, page), TabView.Clear, TabView.Count, TabView.Selected, TabView.OnSelect` |

RadioGroup.Across is yes to lay the choices out across instead of down. A ComboBox is a drop-down and has no Across. TabView.Selected runs from 1 to the count, and the first page added is the one shown first. RadioGroup, ComboBox and TabView are version 2.

Table: Tables and trees

| Widget | Makes | Members |
| --- | --- | --- |
| `Table` | `Table.new()` | `Table.AddColumn, Table.AddRow, Table.Clear, Table.Cell, Table.Count, Table.Selected, Table.OnSelect` |
| `TreeView` | `TreeView.new()` | `TreeView.Add, TreeView.Clear, TreeView.NodeText, TreeView.Count, TreeView.Selected, TreeView.OnSelect` |

Table.AddRow takes a list of texts, or one to four texts. Table.Cell(row, column) gives the text in that place. TreeView.Add gives the number of the node it made, and TreeView.Add(text, parent) puts the node under another. TreeView.NodeText gives the text of a node by its number.

Table: Menus and bars

| Widget | Makes | Members |
| --- | --- | --- |
| `MenuBar` | `MenuBar.new()` | `MenuBar.Add(menu, entry), MenuBar.Clear, MenuBar.Picked, MenuBar.PickedMenu, MenuBar.OnPick` |
| `ContextMenu` | `ContextMenu.new(owner)` | `ContextMenu.Add, ContextMenu.Clear, ContextMenu.Picked, ContextMenu.OnPick` |
| `StatusBar` | `StatusBar.new(), StatusBar.new(text)` | `StatusBar.Text, StatusBar.Add, StatusBar.Clear` |

A ContextMenu opens with the other mouse button on the widget it was made for. A menu holds 64 entries.

Table: Other widgets

| Widget | Makes | Members |
| --- | --- | --- |
| `TextArea` | `TextArea.new()` | `TextArea.Text, TextArea.OnChange` |
| `NumberBox` | `NumberBox.new()` | `NumberBox.Value, NumberBox.Least, NumberBox.Most, NumberBox.Step, NumberBox.OnChange` |
| `Slider` | `Slider.new()` | `Slider.Value, Slider.Least, Slider.Most, Slider.Step, Slider.OnChange` |
| `GroupBox` | `GroupBox.new()` | `GroupBox.Text, GroupBox.Content` |
| `ScrollView` | `ScrollView.new()` | `ScrollView.Content` |

TextArea keeps at most 4,096 characters. A list, a table, a tree or an item picker holds at most 1,024 rows. A tab view holds 64 pages, a table 8 columns, and a status bar 4 sections.

## If something goes wrong

> [!PROBLEM] The program stops and says a list holds at most 1024 rows.
> Clear the list, or show fewer rows.

> [!PROBLEM] The program stops and says a menu holds at most 64 entries.
> Use fewer entries, or split them between two menus.

> [!PROBLEM] The program stops and says a tab view holds at most 64 pages.
> Show fewer pages, or put some of them in another window.

> [!PROBLEM] The program stops and says a table has at most 8 columns.
> Use 8 columns or fewer.

> [!PROBLEM] The program stops and says a status bar shows at most 4 sections.
> Use 4 sections or fewer.

> [!PROBLEM] The program stops and says there is no node with that number in this tree.
> Use a number that TreeView.Add gave you.

> [!PROBLEM] The program stops and says there is no row and column with those numbers in this table.
> Check the row and column numbers against Table.Count first.

> [!SEE] jsc:widgets_basic, jsc:windows_and_layout
