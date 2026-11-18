---
section: fab
order: 42
icon: jsc:mask_blank
covers: [jsc:mask_blank]
---
# Writing a mask

A Lithography Station exposes each layer of a wafer through a mask, and a mask is never used up: you write one mask and it serves every wafer of its chip. Every mask is written on a Mask Blank, a mask plate from the Optical Polisher with a chromium nugget laid on it in the Deposition Chamber.

The Vintage station writes its masks by photo-reduction: put a blank and the chip's Layout in it, and it gives the mask, the Layout used up.

From the Legacy on, you write the mask from the computer linked to the station, with Midsoft Lithography.

> [!DEFINE] Midsoft Lithography
> The program of a computer linked to a Lithography Station from the Legacy on: it shows the station and writes its masks. On MC-DOS and MC-NET it takes the whole screen; on a desktop it is a window.

1. Install Midsoft Lithography on the computer linked to the station, from its install disc.
2. Put a Mask Blank in the station.
3. Open the program: type litho at the prompt of MC-DOS or MC-NET, or open Lithography on the desktop.
4. Pick the kind of chip, then the mask: with the arrow keys and [[Tab]] on the text screen, with the tabs and the list in the window.
5. Press [[W]] on the text screen, or Write Mask in the window.

The station writes the mask in one layer's time, 20 seconds, and in half of it on a computer two eras newer than the one the station asks for. The computer holds the station's memory while it writes, as while it exposes. The mask then stands where the blank was, and the station exposes through it.

A station writes no mask while it exposes a wafer: change its mask when the wafer leaves. It writes the masks of the eras it serves that its computer makes chips for, each era's chips being made by the computers of the era before. The ArF station writes the Transition's masks on a Legacy computer, and the Standard's too on a Transition or later one.

```table
{"caption": "The program's lists", "heads": ["List", "Its masks"], "rows": [[{"words": "Processors"}, {"words": "a processor mask for each microarchitecture of the era"}], [{"words": "Graphics"}, {"words": "a graphics mask for each microarchitecture of the era"}], [{"words": "Memory"}, {"words": "the era's memory chip and controller chip"}], [{"words": "Portables"}, {"words": "the portables' families of the era"}], [{"words": "Hashing"}, {"words": "Blockwright's hashing chip, in the Standard and the Advanced"}], [{"words": "Light"}, {"words": "the laser diodes' mask, from the Legacy to the Standard, and the LEDs', in the Standard"}]]}
```

```command
id: jsc:command/mc_net/litho
```

## If something goes wrong

> [!PROBLEM] The program says no Lithography Station is linked to this computer.
> Lay a peripheral cable from a Legacy or later station's Peripheral Port to this computer. A Vintage station needs no computer: it writes its masks from a Layout.

> [!PROBLEM] It says to put a Mask Blank in the station.
> The station holds no blank to write on. Put a Mask Blank in it, then write the mask again.

> [!PROBLEM] It says the station is exposing.
> The station is exposing a wafer through the mask it holds. Wait for the wafer to leave, take the mask out, put a blank in, and write the new mask.

> [!PROBLEM] The mask I want is not in the list.
> The station writes only its own eras' masks that this computer makes chips for. On the ArF station, a Transition or later computer lists the Standard's masks too.

> [!SEE] jsc:lithography_stations, jsc:chip_factory_machines
