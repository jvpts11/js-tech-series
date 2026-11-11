---
section: desktop_apps
order: 82
icon: jsc:monitor
---
# Calculator, Paint and Exceed

Three programs of the desktop work with numbers and pictures: a Calculator for expressions, Paint for pictures, and Exceed for a sheet of cells that can ask the network what it holds.

## The Calculator

The Calculator works out the expression you type, with brackets, powers, roots, logarithms and the trigonometric functions. Type with the keys or the keyboard, and press = or [[Enter]] to get the answer. The expression holds at most 256 characters.

Table: Calculator keys

| Key | What it does |
| --- | --- |
| `DEG` | Switches sin, cos and tan between degrees and radians. |
| `C` | Clears the expression. |
| `<-` | Deletes one character. |
| `=` | Works out the answer. Enter does the same. |
| `sin, cos, tan` | The trigonometric functions. |
| `^` | Raises a number to a power. |
| `sqrt` | The square root. |
| `ln` | The natural logarithm. |
| `log` | The logarithm. |
| `!` | The factorial of the number before it. |
| `pi, e` | The constants pi and e. |

## If something goes wrong

> [!PROBLEM] The display shows Error.
> Check the brackets, the signs and the function names, then type the expression again.

## Paint

Paint draws a picture on a canvas of up to 128 by 128 pixels, on one layer, in a palette of 256 colours. A saved picture is a .pix file of about a kilobyte.

Table: Paint tools

| Tool | What it does |
| --- | --- |
| Pencil | Draws with the chosen colour. |
| Eraser | Erases part of the picture. |
| Fill | Fills an area with the chosen colour. |
| Dropper | Takes a colour from the picture. |
| Line, Rectangle, Ellipse | Draws a shape of that kind. |

The buttons along the top are New, Open, Save, Undo, the zoom, Wallpaper and Print. Undo goes back through up to 24 steps. Wallpaper makes the picture the desktop background. Print sends the picture to a printer, one picture to a page ([](jsc:printers)).

## If something goes wrong

> [!PROBLEM] Paint says "Picture too large to save".
> Make the picture smaller, then save it again.

> [!PROBLEM] Paint says "That picture is too large to open here".
> Open a smaller picture.

> [!PROBLEM] Paint refuses a file and says it is not a picture.
> Open a file that ends in .pix, or a .map a scanner read, which Paint saves as a .pix ([](jsc:scanners)).

## Exceed

Exceed is a sheet of cells, as a spreadsheet is. It has 26 columns, from A to Z, and 99 rows. Each cell has a name made of its column letter and its row number, such as B7. Its files are plain comma-separated text (.csv).

> [!DEFINE] Cell
> One box of a sheet, named by its column letter and its row number.

> [!DEFINE] Formula
> The text of a cell that starts with an equals sign, and works out a value from other cells.

> [!DEFINE] Live cell
> A cell whose number comes from the network, and so shows in its own colour.

A formula uses the four arithmetic signs, brackets, the power sign, the names of other cells, and the functions below. A live cell is one that uses QTY, FREE or SERVERS. Exceed asks the network again after a wait of 3 seconds, so typing does not send a request for every key.

Table: Exceed functions

| Function | What it gives |
| --- | --- |
| `SUM` | The total of a range of cells. |
| `MIN` | The smallest value of a range. |
| `MAX` | The largest value of a range. |
| `AVG` | The average of a range. |
| `COUNT` | How many values a range holds. |
| `QTY("item_id")` | How many of an item the network holds, given by the item's id in quotes. |
| `FREE()` | How much room the network has left. |
| `SERVERS()` | How many servers are on the network. |

Table: Errors in a cell

| Shown | What it means |
| --- | --- |
| `#ERR` | The formula cannot be worked out: bad syntax, a bad reference or an unknown function. |
| `#LOOP` | The cell refers to itself, directly or through other cells. |
| `#NONE` | The cell asks the network for something the machine has not reported yet. |

The buttons of Exceed are Open, Save, Save As and Refresh.

## If something goes wrong

> [!PROBLEM] Exceed says "Sheet too large to save".
> Remove the cells you do not need, then save again.

> [!PROBLEM] Exceed asks whether to open another sheet and lose your changes.
> Answer no, and save the sheet first.

> [!PROBLEM] A cell shows #ERR.
> Check the formula's brackets, the function names and the cell names.

> [!PROBLEM] A cell shows #LOOP.
> Remove the reference that leads back to the cell.

> [!PROBLEM] A cell shows #NONE.
> Check the item's id, or wait for the network to report.

> [!SEE] jsc:programs, jsc:help_on_a_computer
