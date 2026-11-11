---
section: peripherals
order: 73
icon: jsc:printer
covers_family: jsc:printers
---
# Printers

A printer puts a document on paper. Each era has its own printer, and each one draws the picture on paper its own way, as the real printers of that time did. Join a printer to a computer, put plain paper in its tray, and print from a program.

A picture can come out on paper too. A sheet with a picture can hang in an item frame, where it fills the face of the frame. Right-click a Printed Paper sheet to read it.

Table: The printers

| Printer | Era | Ink | Colour |
| --- | --- | --- | --- |
| Epsilon FX-80 | Vintage | Dot matrix | Black and white |
| Pakard DeskJot 940 | Legacy | Inkjet, in coarse dots | Colour |
| Pakard FotoSmart C4280 | Transition | Inkjet, in fine dots | Colour |
| Pakard LaserJot 1102 | Standard | Laser, with toner | Black and white |
| Epsilon EcoTonk ET-2720 | Advanced | Ink tank, the picture as it is | Colour |

The printer's tooltip, and the print dialog, say which printers print in black and white: "Prints in black and white", or "Prints in color". The dialog also shows the word Mono or Color beside the printer's name.

The FX-80 draws in black dots, one pass for every eight rows. The DeskJot leaves bands of colour, and the FotoSmart prints the same inks in finer dots. The LaserJot uses toner, and the EcoTonk prints the picture as it is.

## How long a page takes

Table: Ticks for one page

| Printer | Ticks per page |
| --- | --- |
| Epsilon FX-80 | 144 ticks |
| Pakard DeskJot 940 | 120 ticks |
| Pakard FotoSmart C4280 | 100 ticks |
| Pakard LaserJot 1102 | 70 ticks |
| Epsilon EcoTonk ET-2720 | 110 ticks |

A tick is a twentieth of a second, so 20 ticks make a second. The fewer ticks a page takes, the faster the printer.

## Paper and the output

The tray takes plain paper, and empty maps for copying a map. Right-click the printer with paper to fill its tray. Each page takes one sheet from the tray. When the tray is empty, the job waits with the status "no paper".

The printer puts finished pages in its three output places. When they are full, the status says "take the sheets". Take the sheets from the printer's window: shift-click a sheet to move it into your inventory.

## The queue

The queue is the list of jobs waiting to print, and the first job is the one printing. The printer keeps 16 jobs, and a job can ask for 1 to 99 copies. A job sent to a full queue is refused. In the printer's window, PAUSE stops the printing, RESUME starts it again, and CANCEL JOB cancels the job.

The print dialog names the printer's state: "Ready", "Printing", "Paused", "Out of paper" or "Output tray full".

## The printer's window

Table: Part of the window

| Part | What it shows |
| --- | --- |
| QUEUE | The jobs waiting to print. |
| NOW | The job that is printing. |
| SHEET | The progress of the job, sheet by sheet. |
| OUT | The output places, and the sheets in them. |
| PAPER | The sheets left in the tray, with the note "sheets from the tray, one a page". |
| PAUSE, RESUME, CANCEL JOB | Buttons: stop the printing, start it again, or cancel the job. |

## Printing from the command line

On the command line, a file goes to a printer with a command. The MC-DOS prompt uses PRINT, and the shells of Linux, FreeBSD and UNIX System V use lp. The commands are explained in full in the MC-DOS book ([](jsc:dos_printing)) and in the Linux books ([](jsc:printing)).

Table: The print commands

| Command | System | What it does |
| --- | --- | --- |
| `PRINT` | MC-DOS | Prints text files in the background. |
| `lp` | Linux, FreeBSD and UNIX System V | Sends files to a printer. Its -d option names the printer, and -n the copies. |
| `lpstat` | Linux, FreeBSD and UNIX System V | Lists the print queue, and whether each printer is idle. |

For lp, each printer has a name. Give it with -d. The name is also the start of the request id that lp answers with: the name, a dash and a number.

Table: Printer and its name for lp

| Printer | Name for lp |
| --- | --- |
| Epsilon FX-80 | `epsilon` |
| Pakard DeskJot 940 | `deskjot` |
| Pakard FotoSmart C4280 | `fotosmart` |
| Pakard LaserJot 1102 | `laserjot` |
| Epsilon EcoTonk ET-2720 | `ecotonk` |

## Copying a map, and books

A .map file, which a scanner reads from a map ([](jsc:scanners)), prints onto an empty map in the tray: print it from the computer, or send it with PRINT or lp, and the printer gives the same map back, drawn. With no empty map in the tray, the job waits with the status "no empty map". A .map prints only in the world whose scanner read it.

Books are printed by the Large Scale Printer alone, a machine of its own ([](jsc:large_scale_printer)).

## If something goes wrong

> [!PROBLEM] Nothing comes out.
> Read the status. "no paper" means the tray is empty, so add plain paper. "take the sheets" means the output is full, so take the sheets out.

> [!PROBLEM] No printer is connected to this computer.
> Join the printer to the computer with a peripheral cable ([](jsc:peripheral_cables)).

> [!PROBLEM] The printer says it cannot take more documents: its queue is full.
> Wait for the queue to empty, or cancel the job with CANCEL JOB.

> [!PROBLEM] A picture in colour came out in black and white.
> Print it on a printer that prints in colour: the Legacy, Transition or Advanced printer.

> [!PROBLEM] The lp command says the destination is "non-existent".
> Use the name of a printer joined to the computer. lpstat shows them.

> [!PROBLEM] The file is not a map a scanner of this world read.
> A .map prints only as it was scanned in this world; one that was changed, or comes from another world, does not.

> [!SEE] jsc:peripheral_cables, jsc:dos_printing, jsc:printing
