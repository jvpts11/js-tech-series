---
section: dos_programs
order: 207
---
# Midsoft Lithography and its keys

Midsoft Lithography shows the Lithography Station linked to the computer and writes the masks it exposes through ([](jsc:mask_writing)). Type litho at the prompt to open it. The program takes the whole screen, and it works on MC-DOS and on [MC-NET](jsc:mc_net). On a desktop it opens as a window.

1. Put a Mask Blank in the station.
2. Type litho at the prompt and press [[Enter]].
3. Press [[Tab]] until the kind of chip you want is lit, then pick its mask with the up and down arrows.
4. Press [[W]] to write the mask on the blank. The line over the keys shows how far the writing is.
5. Press [[F3]] to give the terminal back.

The left of the screen shows the station: what it is doing, the wafer and the layer it exposes, what the computer holds for it, its liquids and gases, and what stands in its mask slot. When several stations are linked to the computer, [[S]] shows the next.

```command
id: jsc:command/mc_dos/litho
```

## If something goes wrong

> [!PROBLEM] The screen says no Lithography Station is linked to this computer.
> Lay a peripheral cable from a Legacy or later station's Peripheral Port to this computer.

> [!PROBLEM] W says the station is exposing.
> The station writes no mask while it exposes a wafer. Wait for the wafer to leave, then change the mask.

> [!SEE] jsc:mask_writing, jsc:lithography_stations
