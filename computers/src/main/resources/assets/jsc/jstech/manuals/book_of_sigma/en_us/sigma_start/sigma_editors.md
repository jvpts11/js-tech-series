---
section: sigma_start
order: 410
---
# The editors

You write a Sigma program in a text editor. Five programs open Sigma code: the Editor, vim, Virtual Studio, Virtual Studio Code and Exposure. They differ in where they run and how much they help you.

Table: Where each editor runs

| Program | Where it runs | Disk and memory |
| --- | --- | --- |
| Editor | The desktops of Frames, Linux, FreeBSD and UNIX | Comes preinstalled |
| vim | The terminal of every system | 8 MB of disk, 4 MB of memory |
| Virtual Studio | Frames only | 512 MB of disk, 256 MB of memory |
| Virtual Studio Code | Frames, Linux and FreeBSD | 128 MB of disk, 48 MB of memory |
| Exposure | Frames, Linux and FreeBSD | 192 MB of disk, 64 MB of memory |

vim takes over the terminal it was started from, so it works where there is no desktop. Virtual Studio, Virtual Studio Code and Exposure are installed like any other program ([](jsc:programs)). All three need a Legacy computer or later. On Frames, they also need Frames XP or later.

Virtual Studio is the whole Sigma# workshop in one window, and it runs on Frames only. It works with projects and solutions, and you can read about it in [](jsc:virtual_studio).

> [!DEFINE] solution
> A folder that groups several projects, with one file that lists them.

Table: Keys of Virtual Studio

| Key | What it does |
| --- | --- |
| `F5` | Start: builds and runs the open file, or the startup project when a solution is open. |
| `Ctrl+Shift+B` | Builds each project of the solution that makes a listing, in turn, and shows the output in the Output tab; with no solution, builds the open file. |
| `Ctrl+Shift+N` | Opens the New Project wizard. |
| `Ctrl+S` | Saves the file. |
| `Ctrl+F` | Finds text in the file. |
| `Ctrl+G` | Goes to a line. |
| `Ctrl+W` | Closes the tab. |

Virtual Studio Code is a light editor with the terminal built in, so you can write, compile and run without leaving it. It runs on Frames, Linux and FreeBSD.

> [!DEFINE] Virtual Studio Code
> A light editor with a terminal built in, where you write, compile and run a program in one window.

Table: Keys of Virtual Studio Code

| Key | What it does |
| --- | --- |
| `F5` | Run File: builds the file, then types sigma run at the terminal, with the listing in the build folder. |
| `Ctrl+Shift+B` | Build File: saves the file, then types the compiler's command, which writes its listing to the build folder beside the file. |
| `Ctrl+P` | Goes to a file by its name. |
| `Ctrl+S` | Saves the file. |

Exposure compiles every program in a folder at once, so a change shows what else it broke. It offers nothing while you type, and that is its trade.

> [!DEFINE] Exposure
> An editor that compiles every program in a folder at once, so you see what else your change broke.

Table: Keys of Exposure

| Key | What it does |
| --- | --- |
| `F5` | Surveys the folder: it asks for every program in it at once and lists the problems. |
| `F6` | Saves, compiles and runs the file at the terminal, with the listing written beside the source file. |

The window of Exposure shows PROBLEMS, a count of the problems the survey found, and WHAT THE COMPILER SAID, the compiler's own words about the problem. The Rebuild all button compiles everything again.

Table: Which editor opens which file

| File | Opened first by |
| --- | --- |
| `.sgs and .sg` | Virtual Studio Code, then Virtual Studio, Exposure and the Editor |
| `.asm` | The Sigma runtime, which runs it, then Virtual Studio Code and the others |
| `.sgsproj, .sgproj and .sln` | Virtual Studio, then the Editor |
| `.txt, .cfg, .csv, .cmd and .log` | The Editor |

## If something goes wrong

> [!PROBLEM] Virtual Studio Code says the file is not a program to build.
> Open a file that ends in .sg or .sgs, then press [[Ctrl]]+[[Shift]]+[[B]] or [[F5]].

> [!PROBLEM] "No project to start: none builds a listing" appears in Virtual Studio.
> Choose a startup project in the solution, one that builds a listing.

> [!PROBLEM] Exposure says the file is not a program to run.
> Open a Sigma source file, which ends in .sg or .sgs.

> [!SEE] jsc:sigma_projects, jsc:sigma_compile
