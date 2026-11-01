---
section: world_data
order: 1
icon: jsspace:mare_regolith
covers: [jsspace:mare_basalt, jsspace:mare_basalt_stones, jsspace:anorthosite, jsspace:anorthosite_stones, jsspace:mare_regolith, jsspace:highland_regolith, jsspace:icy_regolith]
---
# The Moon

## What it is

The Overworld's moon: a round world of rock and dust with no air, under a black sky where the stars show even by day. Its ground is laid as its sphere is seen from orbit: dark maria where its map has them, bright highlands everywhere else, Tycho in the south of the near side, and round each pole craters whose floors never see the Sun. Nothing lives there.

![From orbit](jsspace:gui/guide/moon "96x96")

> [!DEFINE] Regolith
> The loose dust and broken rock over a world with no soil. It falls as sand does.

> [!DEFINE] Mare
> One of the Moon's dark plains, flooded with lava long ago; more than one are maria.

## What it is for

A crew finds the Moon's own ground there, and nowhere else:

- Mare Regolith, the dark dust of the maria, and Highland Regolith, the pale dust of the highlands.
- Mare Basalt, the dark lava rock under the maria's regolith, bare on their steep walls.
- Anorthosite, the pale rock of the highlands, almost all plagioclase. It is the Moon's bedrock, under the maria's basalt as well.
- Icy Regolith, regolith held hard by its ice, on the floors of the polar craters.
- Mare Basalt Stones and Anorthosite Stones, small stones lying loose on the maria and the highlands.

Ores lie in veins in those rocks: ilmenite, the Moon's titanium, with armalcolite and tranquillityite, in the maria's basalt and regolith; merrillite, which carries rare earths, in the maria's basalt; and the native iron of meteorites in the highlands' regolith.

## How to get it

A crew reaches the Moon by rocket. Count the launch down at [the launch panel](jsspace:launch_panel) and the rocket climbs from its pad to orbit. On the Flight Computer's XFER page, choose the Moon: there is no transfer window to wait for, so a crew leaves when it likes. The usual cruise takes {{jsspace:moon_trip}}, and spending more than the least shortens it. With no air to brake in, the rocket burns its engines to stop in the Moon's orbit.

From orbit, mark a spot on the LAND page and the rocket [comes down](jsspace:coming_down) somewhere in the landing ellipse round it. With no air, it needs no heat shield. It does need legs, Landing Legs or a Lunar Lander Descent Stage, and propellant for {{jsspace:moon_landing_delta_v}} to brake down onto them.

Every team knows where the Moon is from the start. A server may also ask that a team knows what kills there before its crew lands: its atmosphere, temperature, radiation, gravity and relief.

The blocks of this page are found on the Moon and mined there, each dropping itself: the regoliths with a shovel, Mare Basalt and Anorthosite with a pickaxe, Icy Regolith with anything, and loose stones at a touch. They are also in the creative menu, J's Space tab.

To leave, set a transfer on the Flight Computer: when its time comes, the rocket lights its lowest stage left and climbs from the ground, with no pad. A Lunar Lander Ascent Stage leaves its descent stage behind as its launch platform.

## How to use it

### Figures

Table: The Moon's figures

| Figure | Value |
| --- | --- |
| Diameter | {{jsspace:moon_diameter}} |
| Gravity | {{jsspace:moon_gravity}} |
| Air | None ({{jsspace:moon_pressure}}) |
| Temperature | {{jsspace:moon_temperature}} |
| Day | {{jsspace:moon_day}}, half of it night |

Gravity is a share of the Overworld's pull. The hour follows the longitude, as on a globe: the farther east you stand, the later it is.

### Landmarks

- Tycho, a great crater, young and bright, in the south of the near side: a bowl under a raised rim with a peak in its middle, its rays laid across the ground round it.
- The Overworld, which hangs still over the near side: high in the sky near the middle of the Moon's map, lower the farther you go, on the horizon a quarter of the way round, and hidden on the far side.

### Biomes

- Mare: plains of old lava, low and dark. Mare Regolith over a thick layer of Mare Basalt, the basalt bare on steep walls and its stones strewn about. Its craters are fewer and smaller than the highlands', their rims of the basalt they broke.
- Rille: a mare cut by narrow winding channels, the troughs left by rivers of lava whose roofs fell in.
- Lava Tube Skylight: a mare over lava tubes, tunnels walled in mare basalt, open to the sky where their roofs fell in.
- Highlands: the bright cratered land everywhere else, crater on crater. Highland Regolith over Anorthosite, the rock bare on steep walls and its stones strewn about.
- Young Crater: a fresh crater with a sharp rim, its bright ejecta and rays of highland regolith lying over what they fell on.
- Polar Craters: craters round the poles whose floors never see the Sun, Icy Regolith on their floors.

### Caves

- Caves wind under all the Moon's ground: a chamber with tunnels running out of it, a crack over the chamber open to the sky, and a mouth where one tunnel comes out, in a slope or a plain.
- Lava tubes under the maria, up to twenty blocks wide and walled in Mare Basalt, a row of skylights open in their roofs, the regolith that fell heaped under each in the light.
- Pits in the maria, as Mare Tranquillitatis', shafts with their rims broken in steps, down to a hollow under the basalt; smaller ones in the highlands.

## What can go wrong

> [!WARNING]
> There is no air. Out of a sealed room your air runs down as under water, and then you drown. Outside, the tanks of [the Lunar Suit](jsspace:lunar_suit) worn whole are your only air.

> [!CAUTION]
> Flames need air. Torches, lanterns, candles and campfires burn only in a sealed room with air, using up its oxygen, and furnaces burn their fuel only where a fire could. Light a base with lights that burn nothing, such as glowstone or a redstone lamp. J's Space's settings can let torches, lanterns, candles and campfires burn anywhere.

> [!NOTE]
> Everything living there falls by the Moon's pull, {{jsspace:moon_gravity}}: a jump carries you far higher, and you come down slower.

> [!NOTE]
> Die on the Moon and you come back at your bed there, if it stands in a sealed room with air; else in the cabin of the module you landed in, if it is still whole and has air; else at home, in the Overworld.

> [!PROBLEM] A slow dose of radiation builds up.
> The sky's radiation reaches the open ground. Every solid block overhead stops most of it, so live under a roof of rock or regolith; the Radiation Vest, fitted to the suit, stops part of it too.

> [!PROBLEM] The landing is held in orbit.
> The server asks that your team knows what kills on the Moon first, and it does not yet: its atmosphere, temperature, radiation, gravity and relief. Or the rocket has no legs, or too little propellant to brake, and the server holds such landings rather than let them crash.

> [!PROBLEM] The rocket crashes at touchdown.
> It had no legs, or too little propellant left to brake the {{jsspace:moon_landing_delta_v}} down. Fit Landing Legs or a Lunar Lander Descent Stage, and keep that much in the tanks.
