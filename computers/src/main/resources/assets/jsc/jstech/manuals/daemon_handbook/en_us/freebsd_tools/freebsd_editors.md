---
section: freebsd_tools
order: 339
---
# Editors on FreeBSD

FreeBSD comes with two editors for text files: ee, the small one, and vi, the editor of UNIX. Both are already on the system, so you never install them.

Table: The two editors

| Editor | What it is on FreeBSD |
| --- | --- |
| `ee <file>` | FreeBSD's own editor. It shows its shortcuts on the top rows of the screen. |
| `vi <file>` | The vi of FreeBSD is nvi. It has no colour and no ruler. |

Each editor has its own pages with its keys and its steps: [ee](jsc:ee) and [vi](jsc:vi), and both are in [Editors at the prompt](jsc:editors_overview). This page only says what is particular to FreeBSD.

## The messages of each editor

ee shows "<name>" [New file] when the file you named is new. After a save, it shows "<name>" with the number of lines and the number of characters.

vi shows <name>: new file: line 1 for a new file, and <name>: unmodified: line 1 for a file that exists. After a write, it shows the number of lines and characters.

To leave ee, press [[Esc]] to open its menu, then press [[Enter]] to choose leave editor. The screen shows the same tip: ESC-Enter: exit. In vi, press [[Esc]] to return to normal mode, then type :wq to write and quit.

## If something goes wrong

> [!PROBLEM] ee says Unknown command at its command prompt.
> The command prompt takes write, quit and exit. Type one of them.

> [!PROBLEM] vi says E37: no write since last change (add ! to override).
> Type :w to write first, or :q! to leave without writing.

> [!PROBLEM] vi says E492: not an editor command.
> Type a command from the vi page ([](jsc:vi)).

> [!PROBLEM] Your typing does not appear as text in vi.
> Press i to start inserting text, and Esc to stop inserting.

> [!SEE] jsc:ee, jsc:vi, jsc:editors_overview
