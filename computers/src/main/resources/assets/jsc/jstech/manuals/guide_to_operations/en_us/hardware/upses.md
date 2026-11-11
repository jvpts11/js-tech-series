---
section: hardware
order: 25
icon: jsc:standard_ups
covers_family: jsc:upses
---
# UPSes

A UPS keeps your computers running when the power goes. The MF PowerGuard is a whole block: you plug the energy cable into the inlet in the middle of its back or of either side, and every computer standing against it on any face draws from it.

> [!DEFINE] UPS
> Uninterruptible power supply: a battery between the line and the computer, which carries the computer when the line goes.

Table: The UPSes

| UPS | Era | Battery | Carries |
| --- | --- | --- | --- |
| MF PowerGuard 300 | Vintage, Legacy | 90 kJ | 300 W |
| MF PowerGuard 500 | Transition | 240 kJ | 500 W |
| MF PowerGuard 800 | Standard | 480 kJ | 800 W |
| MF PowerGuard 1500 | Advanced | 1.35 MJ | 1,500 W |

While the line brings what the computers draw, the UPS feeds them and charges its battery on top: an empty battery is full in ten game minutes. When the line goes, or brings too little, the battery carries them, the amber lamp lights, and each system says so in its own way: the battery in the notification area and a notice on a desktop, two lines on MC-DOS, the UPS daemon's broadcast on a Unix console.

The battery lasts its joules over the draw: 480 kJ carry 800 W for ten minutes. At 10% of the battery the systems shut themselves down cleanly, so nothing is lost and the next start checks no disk.

Above what it carries the UPS goes into bypass, and the green lamp blinks: the computers run straight from the line, and the battery protects nothing.

It also stands between the computers and a line of the wrong class. A line one class too high, or a surge, does not reach them: the UPS takes nothing from it and carries them on its battery. Two classes too high, not even a UPS holds.

A rack has its own UPS, the Rack UPS, which takes a rack unit ([](jsc:rack_equipment)).

## If something goes wrong

> [!PROBLEM] The computers went dark when the power went, with the UPS beside them.
> They drew more than it carries, so it was in bypass. Use a bigger UPS, or fewer computers.

> [!PROBLEM] The computers shut themselves down while the power was out.
> The battery fell to 10%. Bring the line back, and the battery charges again.

> [!PROBLEM] The cable does not plug into the UPS.
> It plugs in on the back and the sides only. Turn the UPS, or bring the cable to one of those.

> [!SEE] jsc:power_supplies, jsc:rack_equipment
