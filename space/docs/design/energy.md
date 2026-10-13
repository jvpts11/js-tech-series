# Energy

How J's Space makes the energy its machines use, on the Earth, on other worlds and in space. The energy is J's Energy,
J's Core's own, in watts and joules; J's Industrial makes it on an industrial scale, and J's Space has generators of its
own, the ones real spaceflight uses, so it plays on its own.

## What exists today

Nothing of J's Space exists yet. J's Core's energy is still Forge Energy today; J's Energy, its voltage classes and the
FE converters are designed with J's Industrial, in its Energy page.

## To build

### On its own

- **Everything is J's Energy.** The numbers are set from real equipment: a launch is driven by its propellant, and what
  spends energy on the ground is the propellant pumps, the cryogenic cooling and the deluge. The great consumers (the
  shipyards, the building of megastructures, the Mass Driver) take high or extra-high voltage directly, as J's
  Industrial's do.
- **J's Core's basic lines and basic battery** carry and store it with no other mod: a low-voltage cable, and an energy
  storage block, both with recipes of vanilla materials ([Propellants](propellants.md) for the basic pipes). With J's
  Industrial installed they are the first rung of its cables and banks.
- **There is no setting to turn the draw off**, since J's Space has generators from the start. Forge Energy from other
  mods comes in through J's Core's converters.

### Solar panels

| Panel | Efficiency | The real thing |
| --- | ---: | --- |
| Silicon | about 10% | Vanguard 1, in 1958, the first solar-powered satellite and the oldest human-made object still in orbit |
| Improved silicon | 15 to 20% | the wings of the International Space Station |
| Triple-junction gallium arsenide | about 30% | the standard of spacecraft today; Juno's work at Jupiter |
| Multi-junction with a concentrator | 40% and more | the laboratory record is 47.6% (Fraunhofer, 2022) |

- **Output falls with the square of the distance to the star**: 43% at Mars, 3.7% at Jupiter, 1.1% at Saturn. Juno
  needed huge panels to work at Jupiter.
- **Night is night**: on the Moon, about five real hours without sun ([The universe](universe.md)). On a world that
  always faces its star, the day side has sun forever and the night side never.
- **Dust on Mars.** A panel loses output as dust settles on it. Opportunity lived fourteen years because dust devils
  swept its panels from time to time; InSight died covered in dust in 2022. In the game, a **panel cleaner** sweeps the
  dust off. A **global dust storm** cuts the sun for a while, as the one that ended Opportunity in 2018. A consequence
  setting turns the dust off.

### Fuel cells and batteries

- A **fuel cell** turns hydrogen and oxygen into electricity and **water**, which goes to life support, as Apollo's
  three fuel cells and the Space Shuttle's did.
- With the Electrolyser it becomes a **regenerative fuel cell**: by day the sun splits water, by night the cell burns
  what was made. It is the real answer studied to get through the lunar night.
- **Batteries** are lithium-ion, as those that replaced the nickel-hydrogen batteries of the International Space Station
  between 2017 and 2020.

### Radioisotope generators

A **radioisotope thermoelectric generator** (RTG) turns the heat of decaying plutonium-238 (half-life 87.7 years) into
electricity through thermocouples. It gives little, Curiosity's about 110 W, but steadily for decades: the Voyager
probes have run on theirs since 1977. It loses output slowly over the game's years. It is the power where the sun
doesn't reach.

J's Space alone finds RTGs **in the human ruins**; with J's Industrial, plutonium-238 is made, from neptunium-237
irradiated in a reactor, as at Oak Ridge.

### Fission for bases

With J's Industrial, which has the fuel, a **small fission reactor** powers bases where the sun fails, as NASA's
Kilopower, tested as KRUSTY in 2018, gives 1 to 10 kW for bases on the Moon and Mars.

### Energy from space

**Solar power satellites** send their energy down as microwaves to a **rectenna** on the ground; in 2023, Caltech's
MAPLE experiment sent power wirelessly in space for the first time. The Dyson Sphere is a megastructure
([Megastructures](megastructures.md)). With J's Industrial installed, both deliver J's Energy like any generator.

### Satellites and ships power themselves

Satellites carry their own panels and batteries, and RTGs beyond Jupiter, as real ones do; the antenna on the ground is
what draws from the grid ([Satellites](satellites.md)). Rockets carry fuel cells or panels in their service module;
starships have reactors of their own ([Starships](starships.md)).
