# Heat

Temperature held in blocks, carried by conduction and by heat pipes, lost to the surroundings, turned into steam and
back, and felt by those who stand too close. It is in the spirit of Mekanism's heat, with real physics.

## What exists today

The cable block knows a heat grid, and machines have a heat port and can overheat by design ([Machines](machines.md));
there is no physics of heat behind them yet.

## To build

### Real temperature

- **Every block that holds heat has a temperature, in kelvin.** Its heat capacity comes from its mass and the specific
  heat of its material, real data of the material catalogue ([Materials](materials.md)).
- **Conduction between neighbours by the real conductivity of their materials**: copper carries heat, stone hardly.
- **Loss to the surroundings** by the temperature of the place: the biome's climate, the dimension's rules and its
  altitude, a sealed room's temperature ([The world](world.md), [Sealed rooms](sealed-rooms.md)).
- **In a vacuum heat leaves only by radiation**, so a ship or a base in space needs **radiators**, as real ones do, and
  the vacuum itself insulates.

### Sources, sinks and exchange

- **Sources and sinks** that blocks declare: burners, electric heaters (J's Energy into heat), reactors, coolers,
  radiators, cooling towers.
- **Heat exchangers** between fluids, and **boilers** with the real enthalpy: 2.26 MJ for every kilogram of water
  boiled.
- **Changes of state by temperature and pressure**: water boils lower where the pressure is low (on Mars), and **pipes
  of water freeze** in deep cold unless insulated, a consequence setting.
- **Insulation**: materials that insulate, such as rock wool and aerogel.
- **Overheating**: past its limit a machine is damaged; heat passes to its neighbours and can **light what burns**
  ([Explosions and fire](explosions-and-fire.md)).

### Seeing and feeling it

- **What is hot glows**, steel red-hot by the real colour of a body at that temperature, with the heat haze of the
  effects kit ([Lighting and effects](lighting-and-effects.md)).
- **A thermal view**: goggles that show temperatures in false colour, as a thermal camera does.
- **Thermometers and thermocouples** as readings for sensors; Jade shows the temperature.
- **The body feels it**: standing beside something very hot burns slowly without protection.

### Cost

**Only blocks that hold heat take part**, worked out in batches, and what is in balance sleeps.
