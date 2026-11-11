---
section: autocrafting
order: 144
icon: jsc:pattern_encoder
covers_family: jsc:pattern_encoders
---
# The Pattern Encoder

A pattern on one computer is no use to another, so a pattern must travel. The Pattern Encoder is the device that burns patterns onto media, such as floppy disks, discs and USB sticks, and you carry the medium from one computer to another. The encoder joins to a computer with a Peripheral Cable ([](jsc:peripheral_cables)).

1. Write the pattern in the Pattern Studio ([](jsc:pattern_studio)).
2. Put a blank medium in a Pattern Encoder that is joined to the computer, and burn the pattern onto it.
3. On the Crafting Computer, open the Crafting Manager and load the pattern from the medium ([](jsc:crafting_manager)).

A bench pattern goes into a card, and a machine pattern goes into the interface of its machine. The plain Pattern Encoder is the Standard one, and each era has its own encoder.

Table: Media each encoder writes

| Encoder era | Media it writes |
| --- | --- |
| Vintage | Floppy Disk |
| Legacy | CD-RW |
| Transition | DVD-RW and CD-RW |
| Standard | DVD-RW, CD-RW and USB Flash Drive |
| Advanced | BD-RE and USB Flash Drive |

Table: The media of patterns

| Medium | Era | Can the encoder write it? |
| --- | --- | --- |
| Floppy Disk | Vintage | yes |
| CD-RW | Legacy | yes |
| CD-ROM | Legacy | no |
| DVD-RW | Transition | yes |
| DVD-ROM | Transition | no |
| USB Flash Drive | Standard | yes |
| BD-RE | Advanced | yes |
| BD-ROM | Advanced | no |

A read-only medium, marked no, is refused, because a pattern needs a medium the encoder can write.

From the Legacy on, the encoder burns discs, and the discs lie on a tray. Press the eject button to bring it out, put the disc on it, and press the button again, or push the tray in with an empty hand. Nothing is burned while the tray is out, and the tray stays shut while a pattern is written. A floppy disk or a USB stick does not use the tray: it goes into its slot whatever the tray does.

```scene
{"balloons": [{"number": 1, "part": {"x": 1, "y": 0, "z": 1}, "text": "The Pattern Encoder"}, {"detail": "Press the eject button to bring it out or to take it in.", "number": 2, "part": {"box": [2, 11, 12, 12, 13, 16], "x": 1, "y": 0, "z": 1}, "text": "The disc tray"}, {"number": 3, "part": {"box": [12, 9, 15, 14, 11, 16], "x": 1, "y": 0, "z": 1}, "text": "The eject button"}], "blocks": [{"data": "{Media:{Size:1,Items:[{Slot:0,id:\"jsc:dvd_rw\",count:1}]},TrayOpen:1b}", "state": "jsc:pattern_encoder[facing=south]", "x": 1, "y": 0, "z": 1}], "caption": "A Pattern Encoder with its tray out and a DVD-RW lying on it. Drag it to see the other sides.", "height": 110, "view": {"pitch": 20.0, "yaw": -25.0, "zoom": 1.2}}
```

A burn runs in three phases: seeking, writing and verifying. The display shows "Seeking", then "Writing" and the file's name, then "Verifying" and the name, and last "Done:" and the name.

Table: How fast a pattern is burned

| Medium | Writing speed | Seek | Verify |
| --- | --- | --- | --- |
| Floppy Disk | 250 bytes a tick | 20 ticks | 10 ticks |
| CD | 1,024 bytes a tick | 30 ticks | 10 ticks |
| DVD | 4,096 bytes a tick | 15 ticks | 5 ticks |
| Blu-ray | 16,384 bytes a tick | 12 ticks | 4 ticks |
| USB Flash Drive | no limit | 4 ticks | 2 ticks |

Seek is the time to find the place on the medium, and verify is the read-back check. A tick is a twentieth of a second, so 20 ticks make one second.

The queue holds up to 8 burns. A burn is refused when the queue is full. The file is named after the pattern, with .craft at its end, and its name is made unique on the medium. When the write ends, the encoder reads the medium back and compares it. A mismatch shows "Verify failed" and the file's name.

The bay is locked while a burn runs, so you cannot take the disc out until the burn has finished. The panel reads "Not linked" when no computer is joined, and "Linked to" with the computer's name when one is.

Table: The panel of the encoder

| Line on the panel | What it means |
| --- | --- |
| Ready | The encoder waits for a job. |
| Insert media | No medium to write is in the bay. |
| Tray open | The tray is out and holds no medium to write. |
| Seeking | The encoder is finding the place on the medium. |
| Writing, then the file's name | The pattern file is being written. |
| Verifying, then the file's name | The written file is being read back and compared. |
| Done, then the file's name | The file is on the medium. |
| Cancelled | The burn was stopped before it finished. |
| Write failed, with the reason | The write did not finish: the medium is full, or there is no medium. |

Table: Messages of the encoder

| Message | What it means |
| --- | --- |
| The tray is closed - press the eject button to open it. | The tray does not open or take a disc until you press the eject button. |
| The bay already holds a disc - sneak-click to eject it. | The bay holds one disc, so take it out first. |
| The encoder is writing - wait for it to finish. | A burn is running, so the bay and the eject button are locked. |

## If something goes wrong

> [!PROBLEM] The Pattern Studio sees no encoder.
> It is not joined to that computer. Join it with a Peripheral Cable.

> [!PROBLEM] The disc is not read, and the burn does not start.
> The disc lies on an open tray. Close the tray with the eject button, or push it in with an empty hand.

> [!PROBLEM] The encoder says "A Vintage encoder writes floppy disks only."
> Put in a medium that the table above lists for this encoder.

> [!PROBLEM] The encoder says "A Legacy encoder writes CDs only."
> Put in a medium that the table above lists for this encoder.

> [!PROBLEM] The encoder says "A Transition encoder writes DVDs and CDs only."
> Put in a medium that the table above lists for this encoder.

> [!PROBLEM] The encoder says "This encoder writes DVDs, CDs and USB sticks, not that."
> Put in a medium that the table above lists for this encoder.

> [!PROBLEM] The encoder says "An Advanced encoder writes Blu-ray discs and USB sticks only."
> Put in a medium that the table above lists for this encoder.

> [!PROBLEM] The encoder says "That disc is read-only and cannot be written."
> Use a disc that can be written, as the table above shows.

> [!SEE] jsc:peripheral_cables, jsc:drives, jsc:pattern_studio, jsc:crafting_manager
