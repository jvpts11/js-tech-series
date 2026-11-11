---
section: saving
order: 483
---
# Programs and the world's save

Your programs are kept when the world is saved. The computer writes its running programs into its own save data, which is the part of the world's save that belongs to that computer. Its disks and its settings are saved there too.

Each program is written down in full, so that it can carry on. When the world comes back, a program carries on from where it stopped. It does not start again from the beginning.

Table: What a save keeps for each program

| Part | What it means |
| --- | --- |
| Memory | Every object the program holds, and the links between them. |
| Threads | Each thread, and what it was doing. |
| Console | The lines on the console, and the lines typed ahead. |
| Waiting calls | The calls that wait to run in the program. |
| Windows | The windows the program has open. |
| Listing | The program's listing, identified by its checksum. |
| Parent | The program that started it, if another program did. |

The next free number is saved too, so no number is given out twice.

## Switching the computer off

Switching a computer off is different. Its programs are stopped, each with its goodbye, and they are not kept for when it comes back on. When you turn it on again, you start the programs again.

## Reading the save back

When the save is read back, the computer checks it. If the save came from another version of the mod, or a listing changed since its program started, the computer leaves out every program of that computer.

A program whose language is no longer installed is left out alone. So is a program whose language does not take what it wrote. The other programs are not affected.

A program started from another computer keeps its link to the program that started it. After a reload, that parent is still told when the child ends.

> [!DEFINE] Checksum
> A number worked out from a text. The same text always gives the same number, and a changed text gives another one.

## If something goes wrong

> [!PROBLEM] The prompt says "the programs that were running could not be brought back from the save".
> The save came from another version of the mod, or a listing changed since it started. Start the programs again.

> [!PROBLEM] A program in a language that is no longer installed is gone after a reload.
> Its language is no longer installed, so the program is left out. Install that language.

> [!PROBLEM] A program is gone after the computer was switched off.
> It was stopped, not kept. Start it again.

> [!SEE] jsc:sigma_run, jsc:running_programs
