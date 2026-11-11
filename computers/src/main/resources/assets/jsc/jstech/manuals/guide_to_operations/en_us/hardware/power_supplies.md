---
section: hardware
order: 24
icon: jsc:psu_200
covers_family: jsc:power_supplies
---
# Power supplies

Most parts draw power, and the power supply gives it. The processors, the memory, the graphics cards, the disks and the expansion cards each draw watts. The supply has to give at least their total.

There are twelve supplies, from 200 W to 3,000 W, each with its own efficiency.

> [!DEFINE] Efficiency
> The share of the power a supply takes from the line that reaches the parts: 300 W of parts on an 80% supply take 375 W from the line. The build check does not use it.

Table: The power supplies

| Power supply | Era | Power | Efficiency |
| --- | --- | --- | --- |
| MF PowerBasic 200 | Vintage | 200 W | 80% |
| MF PowerBasic 300 | Vintage | 300 W | 80% |
| MF PowerBasic 350 | Legacy | 350 W | 80% |
| MF PowerBasic 500B | Legacy | 500 W | 80% |
| MF PowerBasic 450B | Transition | 450 W | 85% |
| MF PowerGold 650G | Standard | 650 W | 90% |
| MF PowerGold 850G | Standard | 850 W | 90% |
| MF PowerGold 1000G | Advanced | 1,000 W | 90% |
| MF PowerPlat 1200P | Advanced | 1,200 W | 92% |
| MF PowerPlat 1600P | Advanced | 1,600 W | 92% |
| MF ServerPSU 2000P | Advanced | 2,000 W | 94% |
| MF ServerPSU 3000P (Redundant) | Advanced | 3,000 W | 94% |

The build adds up the draw of the processors, the memory, the disks and the expansion cards. If the sum is more than the supply gives, the build fails with PSU insufficient ([When the build is refused](jsc:build_refusals)).

Every computer's assembly screen has a DRAW readout: the draw against the supply's watts, amber near the rating and red past it, so you can see the sum while you choose. Its tooltip gives the watts the supply takes from the line.

A computer whose supply gives less than its parts draw does not start. It stays off, and a server says why.

A running computer draws its energy from the grid through its supply: its parts' watts over the supply's efficiency, as J's Energy in the low voltage class or as FE, from any cable that brings energy. Without energy it does not start. When energy stops reaching it, it goes dark at once, and the network's event log says so. A server can turn the draw off.

A supply wears out while it is on, and in the end fails. The nearer its rating it runs, the faster it wears: up to half its rating it lives eight times as long as at full load, up to four fifths three times. At full load a PowerBasic lasts 60 game days, a PowerGold 100, a PowerPlat 150 and a ServerPSU 250, and a rack running too hot halves that. The wear shows as a bar under the supply, as an item's durability does. From 80% worn the supply is degraded, and you are warned at every boot; at 100% it fails, and the computer goes dark until you put in a new one. The 3000P has two units: one can fail, and the other carries the load.

A computer that loses its power instead of being switched off shuts down suddenly. A line that still brings something, but less than the computer draws, is unstable power: the system goes down on its red page first, and a supply that fails puts up its own. What the Mainframe's Operations were carrying is lost, and the log says what. The next time the system starts, it checks the disks it had in use: two seconds with a journal, up to a minute without one, and nothing on MC-DOS. Switch your computers off with their button and you lose nothing.

## If something goes wrong

> [!PROBLEM] The computer does not start, or the build says "PSU insufficient".
> Its parts draw more than its supply gives. Use a supply with more watts.

> [!PROBLEM] The computer does not start, and its DRAW readout says NO POWER.
> No energy reaches it. Run a cable to it from a generator or a battery.

> [!PROBLEM] The computer went dark, and its power button says "PSU failure".
> Its supply wore out. Put in a new supply, and run it further under its rating.

> [!PROBLEM] The computer goes down on a red page about unstable power.
> The line gives it less than it draws. Add a generator or a battery, or take machines off the line.

> [!PROBLEM] The computer starts with a long check of its disk.
> It went dark suddenly the last time. Let it finish: the check erases nothing.

> [!SEE] jsc:build_refusals, jsc:disks, jsc:processors
