---
section: library_systems
order: 476
---
# Boot, Panic, Screen and Input

With these you draw your system's pages: the page it shows while it comes up, the page it shows when it goes down, and its own text screen. Only the system itself, process 1, draws its boot and panic pages.

Table: Boot members

| Member | What it does |
| --- | --- |
| `Boot.Logo(path)` | Shows a picture from the disk in the middle of the boot page. |
| `Boot.Line(text)` | Writes the line under the progress bar. |
| `Boot.Progress(percent)` | Fills the progress bar, from 0 to 100 percent (%). |
| `Boot.Done()` | Says the system is up, which ends the boot page. |

Table: Panic members

| Member | What it does |
| --- | --- |
| `Panic.Halt(message)` | Brings the machine down on the panic page, with that message at the top. |
| `Panic.Page(kind)` | The look of the panic page: blue, red, console or own. |
| `Panic.Colours(background, text)` | The colours of your own panic page, each written #RRGGBB. |
| `Panic.RebootAfter(seconds)` | Restarts the machine that many seconds after it goes down. With none, the page stays until you restart the machine. |

Table: Screen members

| Member | What it does |
| --- | --- |
| `Screen.Write(text), Screen.WriteLine(text)` | Writes on the system's text screen, the second one ending the line. |
| `Screen.Clear()` | Clears the screen. |
| `Screen.Cursor(column, row)` | Moves the cursor. |
| `Screen.Colours(text, background)` | The colours of what is written next: one of the 16 named colours, or #RRGGBB. |
| `Screen.Columns, Screen.Rows` | The size of the screen: 80 columns by 25 rows. |

Table: Input members

| Member | What it does |
| --- | --- |
| `Input.Focus(process)` | Hands what is typed to that program, a line at a time. Hand it back with Processes.Current. |
| `InputEvent.Kind` | key, char, move or click. |
| `InputEvent.Key, InputEvent.Char` | The key pressed, and the letter typed. |
| `InputEvent.X, InputEvent.Y, InputEvent.Button` | Where the mouse is, in columns and rows, and the button clicked. |

The text screen is shown when the system's shell is its own and it starts no desktop. A system with no text driver has no screen, so what it writes goes nowhere.

## If something goes wrong

> [!PROBLEM] The program stops and says Boot.Line answers only to the system itself.
> Call Boot, Panic and Desktop.Start from the system's own class, not from a service.

> [!SEE] jsc:system_os_library, jsc:system_processes_library
