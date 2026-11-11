---
section: prompt_basics
order: 233
---
# The Command Prompt window and its keys

The Command Prompt is the window where you type a command and read what the machine answers. A command is a word that names a program, followed by the words that say what it works on. You press [[Enter]] to run the line.

> [!DEFINE] Command
> A program that you call by its name, with the words after the name that say what it works on.

> [!DEFINE] Program
> A set of instructions that the computer runs when you call it by name.

A monitor opens the screen made for its system. A Frames monitor, or a monitor with no system, opens the Command Prompt screen. MC-DOS, the disk operating system (DOS), opens its own text screen, MC-NET opens the network terminal, and Linux and the other Unix systems open their own terminal.

On a Frames desktop, the Command Prompt is one of the desktop's apps, so you can open it from there too.

Above the line you type, the window keeps what the machine has answered. That kept output is the scrollback, and the mouse wheel scrolls back through it. On a monitor, the scrollback holds 512 lines.

> [!DEFINE] Scrollback
> The output that the window keeps above the prompt, so that you can read back what has scrolled off the top.

## The keys

Table: Keys at the prompt

| Key | What it does |
| --- | --- |
| `Enter` | Runs the typed line. |
| `Tab` | Fills in the rest of a command's name from the letters you typed. When several names fit, a second Tab in a row lists every candidate above the prompt. |
| `Up` | Shows the line you typed before it, one step back. |
| `Down` | Shows the line you typed after it, one step forward. |
| `Esc` | Closes the window. An editor that is open takes the key first, when the editor uses it. |
| `Ctrl+C` | Copies the text you have picked out. With nothing picked out, it interrupts a tool that is running. |
| `Ctrl+V` | Pastes text: up to 16 lines, each cut to 512 characters. Every line but the last runs as it arrives. |
| Mouse wheel | Scrolls back through the output. |

> [!DEFINE] Tab completion
> Filling in the rest of a command's name from the letters you have typed, so that you need not type the whole name.

> [!DEFINE] Interrupt
> A signal that asks a running tool to stop.

```text
{"text": "While a tool runs, the window reads only Ctrl+C. Wait for the tool to finish, or press Ctrl+C to interrupt it. The bottom line of the window reminds you of the keys: while you type, it reads \"ENTER run    UP/DOWN history    wheel scroll    ESC close\", and while a tool runs, it reads \"CTRL+C interrupt    wheel scroll    ESC close\"."}
```

When you have scrolled back, the bottom line shows "scrolled +" and a number of lines. Once the first word is a command the machine knows, its usage appears dimmed on the right of your line, cut short with two dots if it does not fit.

## Limits and history

A line holds at most 256 characters. The window keeps the lines you type as history for this machine, so that the Up and Down keys bring them back. An empty line is never kept, and a line typed twice in a row is kept once. Opening the window starts an empty scrollback, but the line history stays.

## If something goes wrong

> [!PROBLEM] The line is cut off.
> A line holds at most 256 characters. Split it into two lines.

> [!PROBLEM] Typing does nothing while a tool runs.
> While a tool runs, the window reads only Ctrl+C. Wait for the tool to finish, or press Ctrl+C.

> [!PROBLEM] Ctrl+C does not stop a tool.
> The tool must be running in this window. A tool left running in the background does not answer Ctrl+C.

> [!PROBLEM] Esc does not close an editor.
> An editor that uses Esc keeps the key. Leave the editor with its own exit key ([](jsc:editors_overview)), then press Esc.

> [!SEE] jsc:help_on_a_computer, jsc:the_systems
