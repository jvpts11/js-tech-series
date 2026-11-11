---
section: library_windows
order: 473
---
# Canvas, images, charts, logs, items and components

Some widgets show pictures, numbers and records. A Canvas is an area you draw on, an Image shows a picture from the disk, and a Chart plots numbers. Each colour argument is a whole number.

> [!DEFINE] Stroke
> One drawing command on a canvas.

> [!DEFINE] Component
> A part that a mod adds to a window, with a kind name. It holds values, not handlers.

Table: Pictures, charts and logs

| Widget | Makes | Members |
| --- | --- | --- |
| `Canvas` | `Canvas.new(), Canvas.new(width, height)` | `Canvas.Clear(colour), Canvas.FillRect, Canvas.DrawLine, Canvas.DrawText, Canvas.SetPixel, Canvas.ClickX, Canvas.ClickY, Canvas.OnClick` |
| `Image` | `Image.new(), Image.new(path)` | `Image.Path` |
| `Chart` | `Chart.new(), Chart.new(least, most)` | `Chart.Text, Chart.Least, Chart.Most, Chart.Add(number), Chart.Clear, Chart.Count` |
| `LogView` | `LogView.new()` | `LogView.Add(text), LogView.Clear, LogView.Count` |

A chart holds 120 points, and when it is full the oldest point goes. A log view holds 256 lines, and the oldest line goes first. Image, Chart, LogView and the widgets below are version 2.

Table: Items and components

| Widget | Makes | Members |
| --- | --- | --- |
| `ItemSlot` | `ItemSlot.new(), ItemSlot.new(item, amount)` | `ItemSlot.Item, ItemSlot.Amount, ItemSlot.OnClick` |
| `ItemPicker` | `ItemPicker.new()` | `ItemPicker.Add(item), ItemPicker.Clear, ItemPicker.Count, ItemPicker.Selected, ItemPicker.Picked, ItemPicker.OnSelect` |
| `OperationView` | `OperationView.new()` | `OperationView.Show(OperationInfo), OperationView.Text, OperationView.State, OperationView.Done, OperationView.Total, OperationView.Computer` |
| `GenericComponent` | `GenericComponent.new(kind)` | `GenericComponent.Kind, GenericComponent.Data, GenericComponent.OnAction` |

An ItemSlot shows an item the way the game shows it. An ItemPicker shows a grid of items, with a search line above it. An OperationView shows one Operation as it stands, with its text, state, done and total amounts, and computer.

A GenericComponent is a kind a mod adds. Its Data is a copy of the value you give it, and OnAction gives you a ComponentAction, with its ComponentAction.Name and ComponentAction.Value. A component's value weighs at most 32 kilobytes (KB), nests at most 16 levels deep, and has at most 4,096 parts.

## If something goes wrong

> [!PROBLEM] The canvas stops taking strokes.
> A canvas holds 4,096 strokes at most. Clear it, and draw fewer strokes.

> [!PROBLEM] The program stops and says a component holds values, not handlers.
> Give the component a value, and listen for its actions with OnAction.

> [!PROBLEM] The program stops and says the component's value weighs too much.
> Keep the value under 32 kilobytes (KB), or split it into several components.

> [!PROBLEM] The program stops and says a value that holds itself cannot be handed to a component.
> Build a value that does not contain itself.

> [!SEE] jsc:monitors, jsc:operations_catalogue
