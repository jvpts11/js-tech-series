---
section: hardware
order: 20
icon: jsc:ram_simm_4
covers_family: jsc:memory
---
# Memory

Memory, or RAM, is where a computer keeps what it works on right now: the system, the programs that are open, the file you are editing. RAM is fast, and it forgets everything when the computer turns off, which is why the disk is there too.

The Task Manager shows how much memory is in use, on its Performance view.

> [!DEFINE] Memory generation
> The standard a memory module follows, such as SIMM, DDR3 or DDR4.

Memory comes in generations: SIMM, EDO and SDRAM in the Vintage; SDRAM, DDR and DDR2 in the Legacy; then DDR2, DDR3, DDR4 and DDR5 in the later eras.

A board takes only some generations, and its tooltip lists them. A module must also match the board's era. A module of another generation or era is refused at its slot.

The Vintage and Legacy modules begin with the words Stratix Layer, as in Stratix Layer SIMM-4. The table gives the rest of each name.

```table
{"caption": "The memory modules", "heads": ["Module", "Era and generation", "RAM buffer", "Power"], "rows": [[{"words": "SIMM-4"}, {"words": "Vintage, SIMM"}, {"words": "1 item"}, {"amount": 1, "unit": "W"}], [{"words": "SIMM-16"}, {"words": "Vintage, SIMM"}, {"words": "4 items"}, {"amount": 2, "unit": "W"}], [{"words": "EDO-16"}, {"words": "Vintage, EDO"}, {"words": "4 items"}, {"amount": 2, "unit": "W"}], [{"words": "EDO-32"}, {"words": "Vintage, EDO"}, {"words": "8 items"}, {"amount": 3, "unit": "W"}], [{"words": "EDO-64"}, {"words": "Vintage, EDO"}, {"words": "16 items"}, {"amount": 4, "unit": "W"}], [{"words": "SDRAM-32"}, {"words": "Vintage, SDRAM"}, {"words": "8 items"}, {"amount": 3, "unit": "W"}], [{"words": "SDRAM-64"}, {"words": "Vintage, SDRAM"}, {"words": "16 items"}, {"amount": 4, "unit": "W"}], [{"words": "SDRAM-128"}, {"words": "Legacy, SDRAM"}, {"words": "32 items"}, {"amount": 5, "unit": "W"}], [{"words": "SDRAM-256"}, {"words": "Legacy, SDRAM"}, {"words": "64 items"}, {"amount": 6, "unit": "W"}], [{"words": "DDR-256"}, {"words": "Legacy, DDR"}, {"words": "64 items"}, {"amount": 7, "unit": "W"}], [{"words": "DDR-512"}, {"words": "Legacy, DDR"}, {"words": "128 items"}, {"amount": 10, "unit": "W"}], [{"words": "DDR-1024"}, {"words": "Legacy, DDR"}, {"words": "256 items"}, {"amount": 12, "unit": "W"}], [{"words": "DDR2-512"}, {"words": "Legacy, DDR2"}, {"words": "128 items"}, {"amount": 9, "unit": "W"}], [{"words": "DDR2-1024"}, {"words": "Transition, DDR2"}, {"words": "256 items"}, {"amount": 10, "unit": "W"}], [{"words": "DDR2-2048"}, {"words": "Transition, DDR2"}, {"words": "512 items"}, {"amount": 12, "unit": "W"}], [{"words": "DDR2 4096 RDIMM"}, {"words": "Transition, DDR2"}, {"words": "1,024 items"}, {"amount": 15, "unit": "W"}], [{"words": "DDR3-1024"}, {"words": "Transition, DDR3"}, {"words": "256 items"}, {"amount": 8, "unit": "W"}], [{"words": "DDR3-2048"}, {"words": "Transition, DDR3"}, {"words": "512 items"}, {"amount": 10, "unit": "W"}], [{"words": "DDR3-4096"}, {"words": "Transition, DDR3"}, {"words": "1,024 items"}, {"amount": 12, "unit": "W"}], [{"words": "DDR3-8192"}, {"words": "Standard, DDR3"}, {"words": "2,048 items"}, {"amount": 15, "unit": "W"}], [{"words": "DDR3L-4096"}, {"words": "Standard, DDR3"}, {"words": "1,024 items"}, {"amount": 10, "unit": "W"}], [{"words": "DDR3 16384 RDIMM"}, {"words": "Standard, DDR3"}, {"words": "4,096 items"}, {"amount": 18, "unit": "W"}], [{"words": "DDR4 8192"}, {"words": "Advanced, DDR4"}, {"words": "2,048 items"}, {"amount": 12, "unit": "W"}], [{"words": "DDR4 16384"}, {"words": "Advanced, DDR4"}, {"words": "4,096 items"}, {"amount": 13, "unit": "W"}], [{"words": "DDR4 32768"}, {"words": "Advanced, DDR4"}, {"words": "8,192 items"}, {"amount": 15, "unit": "W"}], [{"words": "DDR4 65536 RDIMM"}, {"words": "Advanced, DDR4"}, {"words": "16,384 items"}, {"amount": 18, "unit": "W"}], [{"words": "DDR5 16384"}, {"words": "Advanced, DDR5"}, {"words": "4,096 items"}, {"amount": 12, "unit": "W"}], [{"words": "DDR5 32768"}, {"words": "Advanced, DDR5"}, {"words": "8,192 items"}, {"amount": 14, "unit": "W"}], [{"words": "DDR5 65536 RDIMM"}, {"words": "Advanced, DDR5"}, {"words": "16,384 items"}, {"amount": 18, "unit": "W"}], [{"words": "DDR5 131072 RDIMM"}, {"words": "Advanced, DDR5"}, {"words": "32,768 items"}, {"amount": 22, "unit": "W"}]]}
```

In a Mainframe, the RAM buffer has one more job: its items wait in the buffer on their way in or out of the network ([Mainframes](jsc:mainframes)).

## If something goes wrong

> [!PROBLEM] The module is refused.
> It is of a generation the board does not take, or of another era. Use a module the board lists, of its own era.

> [!PROBLEM] The build says the RAM generation is not accepted by the board.
> Use a module of a generation the board lists.

> [!PROBLEM] The build says there is no RAM.
> Put in a module of a kind the board takes.

> [!SEE] jsc:processors, jsc:motherboards, jsc:mainframes, jsc:swap
