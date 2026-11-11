---
section: peripherals
order: 74
icon: jsc:scanner
covers_family: jsc:scanners
---
# Scanners

A scanner reads what lies on its glass into a file, on the computer at the other end of its peripheral cable. There is a flatbed scanner in each era. The item stays as it was: scanning only reads it.

```table
{"caption": "The scanners", "heads": ["Scanner", "Era", "A page takes"], "rows": [[{"words": "Pakard ScanJot"}, {"words": "Vintage"}, {"amount": 8, "unit": "s"}], [{"words": "Epsilon Perfecta 1240U"}, {"words": "Legacy"}, {"amount": 4, "unit": "s"}], [{"words": "Pakard ScanJot G4050"}, {"words": "Transition"}, {"amount": 2, "unit": "s"}], [{"words": "Epsilon Perfecta V600"}, {"words": "Standard"}, {"amount": 1, "unit": "s"}], [{"words": "Epsilon Perfecta V39"}, {"words": "Advanced"}, {"words": "0.5 s"}]]}
```

Table: What it reads

| On the glass | File | Opens in |
| --- | --- | --- |
| Written book | `.book` | Editor |
| Book and quill | .book, unsigned | Editor |
| Enchanted book | `.ench` | Editor |
| Map | `.map` | Paint |
| Printed Paper | .txt, or .pix for a picture | Editor, or Paint |

1. Right-click the scanner with the item to lay it on the glass.
2. Open the scanner and press SCAN. Its lid opens and it reads page by page.
3. The file is in the scans folder of the computer's system.

> [!DEFINE] Scans folder
> Where a scanner puts its files: Users/Public/Documents/Scans on Frames and MC-DOS, and Documents/Scans in the home folder on Linux, FreeBSD and UNIX System V.

A .book starts with its title and its author, and each page under its heading. Edit it as you like, and the Large Scale Printer prints it ([](jsc:large_scale_printer)); an unsigned one prints back as a book and quill. A .ench and a .map are sealed to the world that scanned them: changed, or carried to another world, they no longer print. Paint opens a .map to look at, and saves it as a .pix.

## If something goes wrong

> [!PROBLEM] The scanner says GLASS EMPTY.
> Lay a book, a map or a printed sheet on the glass.

> [!PROBLEM] The scanner says NO COMPUTER.
> Join it to a computer with a peripheral cable ([](jsc:peripheral_cables)).

> [!PROBLEM] The scanner says NO SYSTEM.
> The computer has no system to keep the file: install one.

> [!PROBLEM] The scanner says DISK FULL.
> Make room on the computer's disk.

> [!SEE] jsc:printers, jsc:large_scale_printer
