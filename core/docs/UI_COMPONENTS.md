# UI components

The programs that run on a desktop, and the dialogs a screen opens, are built from the components in
`dev.jstech.core.client.gui.component`. A component keeps its own state (what is typed in a field, which
tab is selected, how far a list is scrolled), draws itself through the desktop's skin, and reports what
the player did to it through a callback. A program composes components instead of painting pixels and
hit-testing rectangles by hand, and it looks like whichever desktop it runs on, because the skin does the
painting.

## The model

Components are retained, layout is immediate. A program creates its components once, keeps them in a
`Panel`, and on every frame places them from the window's current size with `setBounds`, then renders the
panel. Input arrives in the same coordinates the bounds were given in, so nothing is translated twice.

```java
private final Panel root = new Panel();
private final TabStrip tabs = root.add(new TabStrip(List.of("Files", "Encoder")).setOnSelect(this::selectTab));
private final ListView<Row> list = root.add(new ListView<>(this::rows, ROW_H, this::renderRow).setOnClick(this::rowClicked));

void renderContent(GuiGraphics g, Font font, int x, int y, int width, int height, int mouseX, int mouseY, float partialTick) {
    tabs.setBounds(x, y, width, TAB_H);
    list.setBounds(x, y + TAB_H, width, height - TAB_H);
    root.render(g, new UiContext(skin, font, mouseX, mouseY, partialTick));
}

void mouseClicked(DesktopWindow window, double mouseX, double mouseY, int button) {
    root.mouseClicked(mouseX, mouseY, button);
}
```

The panel routes a click to the frontmost child under the cursor, hands the keyboard to a focusable child
that took a click (a field, an editor, a command line) and takes it away from whoever had it, sends keys to
the focused child, drags and releases to the child that took the press, and the wheel to the child under
the cursor. A component that took a click returns `true`; a panel returns `false` when no child did, which
is how a program knows a click landed on empty space.

Every drawing call goes through the `Skin` interface: the colours (text, dim, accent, edge, backgrounds) and
the primitives (panel, button, field, tab, list row, scroll thumb, status bar, window frame). J's
Computers implements it once per operating system, so a button on Frames 95 has a bevel and the same
button on Frames 11 is flat, with no change to the program.

## The components

| Component | What it is |
|---|---|
| `Panel` | The container: children in z-order, focus routing, pressed child. |
| `ScrollPanel` | A panel whose content is taller than it: wheel scrolling, a thumb, clipping. |
| `Popup` | A modal dialog centred in a rectangle: title, layouter for its children, dims what is behind, Escape and an outside click close it. |
| `Label` | Text with a tone (text, dim, accent) or a colour, an alignment, a scale; clipped to its width. |
| `Button` | Fires on press; primary or plain; can be disabled. |
| `Checkbox` | A box with a label, read and toggled through callbacks. |
| `TextField` | One line of text: shows the committed value, edits on focus, commits on blur, Escape reverts; a placeholder, a suffix that is not edited. |
| `SearchField` | A field whose text filters something as it is typed. |
| `TextArea` | Multi-line text with a caret: Enter and Backspace split and join lines, the arrows and a click move the caret. |
| `CommandLine` | A terminal line: a prompt, Enter submits, the arrows recall history, an idle line shows the last output; `setCellFont` writes it in a font of cells, as the terminal above it. |
| `TabStrip` | Tabs of equal width or fitted to their labels. |
| `ListView<T>` | A scrolling list of rows of one height, drawn by a renderer from a live supplier; a click reports the row or the empty space. |
| `ColumnHeader` | The header over a table: sortable columns with the direction, or fixed names. |
| `CellGrid` | A grid of cells: inventory wells or plain tiles, square or rectangular, scrolled by rows, with per-cell tooltips. |
| `ContextMenu` | The menu a right-click opens at the cursor, kept inside a rectangle, closed by any click. |
| `Breadcrumbs` | An address as a trail of crumbs, each a navigation target. |
| `ScrollBar` | A thin bar beside a list, driven by a maximum, a value and a callback. |
| `ProgressBar` | A bar filled by a percentage. |
| `AmountStepper` | A number with halve, double, minus and plus. |

Helpers: `FlowLayout` places components in a row or a column; `Texts` clips, trims and scales strings;
`Draw` outlines, fades a disabled control and clips to a rectangle under the current pose. The pure logic
behind the fields and the editor (`TextEditState`, `TextDocument`, `StepperMath`, `ScrollState`) lives in
`dev.jstech.core.client.gui.logic` and is covered by unit tests.

## Controls, data and views

*Added 2026-10-15.*

Each draws through the skin, so it wears the system's look inside a computer and the player's style on a screen of
the Core; each also has a kind of its own in a screen's file ([Screens](SCREENS.md#screens-declared-in-files)).

| Component | What it is | In a file |
|---|---|---|
| `Slider` | Dragged, clicked or stepped with the arrows; free or in steps, with a tick under each step; its value written on it. | `slider` |
| `Switch` | On or off, with its label; a click or Space turns it. | `switch` |
| `RadioGroup` | Option buttons one under the other; a click or the arrows choose. | `radio` |
| `Dropdown` | A button saying what is chosen; its list opens over everything, the chosen line marked; a long list scrolls. | `dropdown` |
| `ColorPicker` | A swatch and its `#RRGGBB`; opens a square of saturation and value, a bar of hues, ready colours, and the number typed in. | `color` |
| `TreeView` | Nodes in rows with dotted guides, opened and closed by their arrow, each with its item and a count. | `tree` |
| `TableView<R>` | A head of titles that sorts by a click and the other way by a second, rows told apart by shade, filtered by words. | `table` |
| `ChartView` | Lines over time with their legend and a limit, bars with their values, or a pie with its shares. | `chart` |
| `GraphEditor` | Nodes and their ports; wires drawn from an output to an input of the same kind; the right button adds a node or takes a wire away. | `graph` |
| `ModelView` | A 3D view of a block, a few blocks, an item or an entity: dragged to turn, the wheel to zoom, a layer at a time. | `model` |
| `MapView` | The land round a place in the game's map colours, its places marked, brought closer and taken further. | `map` |
| `TankGauge` | A fluid's own texture rising in a well, with a scale; the pointer says the fluid and the amount. | `tank` |
| `EnergyBar` | Segments filled from the bottom in the look's energy colour. | `energy` |
| `GhostSlot` | An item shown and not held: a click with an item sets it, the right button clears it. | `ghost_slot` |

Something drawn over the others while it is open (a drop-down's list, a colour picker) says so with `overlayOpen`;
its panel draws it after every other child and hands it the pointer first, and a click outside it or Escape closes
it. A component that holds something outside itself, a map's picture, lets go of it in `dispose`.

The models behind the data views are pure and tested: `TreeModel`, `TableModel`, `GraphModel`, with `ChartMath`,
`SliderMath` and `ColorMath`. In a file, a tree, a table, a chart, a graph, a map and a 3D view are bound to what the
screen's code hands over under their name (`screen.value("tree", () -> tree)`), and what a control hands back goes to
the handler its `action` names (`screen.onNumber("speed", speed -> ...)`, `onChoice`, `onColour`, `onItem`,
`onPoint`).

A tooltip's line can hold items and pictures in its text: `RichLine.builder().text(...).item(stack).image(sprite, w,
h).build()`, handed to a tooltip as the game hands any picture to one.

## Writing a program

- Create the components in the constructor, add them to the panel in the order they should stack (later
  children are in front), and keep a field for each one you lay out or read.
- Lay everything out in `renderContent` from the content rectangle, every frame; hide what the current
  state has no use for with `setVisible`.
- A program with a dialog overrides `modalActive` and `renderModal`, renders the open popup there with
  `renderIn`, and calls `placeIn` right after `open()`, so a click that arrives before the next frame
  already finds the dialog's controls in place.
- Give the client tests something to click: a method that returns a component's `center()` in the content's
  coordinates, so a test never hard-codes a position.
- Ship a new component with a real consumer. A component nothing uses is not finished.

## What can go wrong

- **An open list is drawn under the items of the slots.** It is drawn by hand instead of through `renderOverlay`;
  the panel lifts what is open over the slots' items.
- **A map leaks a texture each time its screen opens.** Its screen does not call `dispose` when it closes; the
  screens of the Core do.
- **A tree, a table or a chart shows its kind in brackets.** What its name is bound to is not its model: hand over a
  `TreeModel`, a `TableModel` or a list of `ChartView.Series`.
