---
section: shell_software
order: 261
---
# Midsoft Lithography at the prompt

Midsoft Lithography is the program of a computer cabled to a Lithography Station from the Legacy on: it shows the station and writes the masks it exposes through ([](jsc:mask_writing)). On the desktop it opens as a window. At a terminal, type litho and it takes the whole screen.

1. Put a Mask Blank in the station.
2. Type litho and press [[Enter]].
3. Press [[Tab]] until the kind of chip you want is lit, and pick its mask with the up and down arrows.
4. Press [[W]] to write the mask on the blank, then [[F3]] to give the terminal back.

```command
id: jsc:command/linux/litho
```

> [!SEE] jsc:mask_writing, jsc:lithography_stations
