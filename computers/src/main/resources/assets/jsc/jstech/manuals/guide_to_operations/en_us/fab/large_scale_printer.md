---
section: fab
order: 46
icon: jsc:large_scale_printer_print_unit
covers: [jsc:large_scale_printer_paper_feeder, jsc:large_scale_printer_perfect_binder, jsc:large_scale_printer_print_unit, jsc:large_scale_printer_inking_unit]
---
# The Large Scale Printer

The Large Scale Printer prints books in numbers, the one machine that prints them. It is a machine of the Legacy, 4 blocks wide, 3 tall and 3 deep, of the Machine Casing and four working blocks: the Paper Feeder, the Print Unit, the Inking Unit and the Perfect Binder. It draws its energy through the Power Entry at its back.

You put paper into its feeder by hand, through its Item Inlet from a bus, or from a hopper. A book takes 3 paper and nothing else, and 2 seconds. A peripheral cable from a computer to its Peripheral Port makes it a printer of that computer: it shows in the Print dialog of the computer's system, where a .book or a .ench file goes to it, and the help prints its own books on it.

A written book comes out as a copy of the one scanned, and a book and quill as a book and quill. An enchanted book comes out with every enchantment at the level it was scanned with. A switch in its window sends what it prints into the network of the computer at its cable, or into a chest beside its Item Outlet; with neither, the books wait in the machine. It keeps 16 jobs, each of up to 64 copies, and its window pauses the printing and cancels the job that is printing.

```table
{"caption": "Each machine's size, its step and its draw", "heads": ["Machine", "Size", "Step", "Draw"], "rows": [[{"words": "Large Scale Printer"}, {"words": "4 × 3 × 3"}, {"amount": 2, "unit": "s"}, {"amount": 2000, "unit": "W"}]]}
```

## If something goes wrong

> [!PROBLEM] No Large Scale Printer is linked to this computer.
> Run a peripheral cable from the computer to the machine's Peripheral Port, and form the machine.

> [!PROBLEM] This server does not let enchanted books be printed.
> The server's printing.enchanted_books setting is off: enchanted books are not printed.

> [!PROBLEM] The file is not an enchanted book a scanner of this world read.
> A .ench file prints only as it was scanned in this world; one that was changed, or comes from another world, does not.

> [!PROBLEM] Its feeder needs 3 sheets of paper for a book.
> Put paper in the feeder. A job that prints nothing for long also waits for energy, or for room for its books: the network or the chest beside its outlet.

> [!SEE] jsc:building_chain_machines, jsc:printers, jsc:scanners
