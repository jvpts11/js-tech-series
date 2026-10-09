# Propellants

What rockets and starships burn, from a rocket of gunpowder to antimatter, how J's Space makes each one, and where it is
kept. The engines that burn them are in [Rockets](rockets.md) and [Starships](starships.md).

## What exists today

Nothing of J's Space exists yet. J's Core's catalogue holds the fluids every mod shares; the propellants live there, and
J's Space only says it uses them.

## To build

### The ladder

The specific impulse (Isp) is how far a propellant pushes a rocket for its mass, in seconds; the higher, the better. The
numbers are the real ones, in vacuum.

| Propellant | Isp | Where it shines | J's Space alone | With J's Industrial |
| --- | ---: | --- | --- | --- |
| **Gunpowder** | about 80 s | the **first rocket**, a sounding rocket that only goes up and comes down: the first milestone | vanilla's gunpowder | |
| **Composite solid** (perchlorate, aluminium, a rubber binder) | about 270 s | **boosters**, as the Space Shuttle's; once lit, it can't be stopped | perchlorate from salt by electrolysis, or natural from Mars | J's Industrial's chain |
| **Hydrolox** (liquid hydrogen and oxygen) | about 450 s | the most efficient of the chemical propellants, but hydrogen is bulky and must be kept at −253 °C | **from water alone**: the Electrolyser and the Liquefier | |
| **Methalox** (liquid methane and oxygen) | about 380 s | it can be **made on Mars**, from the carbon dioxide of the air, the real reason Starship burns methane | the Sabatier Reactor and the Liquefier | natural gas |
| **Kerolox** (kerosene and oxygen) | about 350 s | dense and cheap, for first stages (Saturn V, Falcon 9) | none | the refinery |
| **Hypergolics** (hydrazine and nitrogen tetroxide) | about 320 s | they **light on touching each other** and keep for years with no cooling: landers and manoeuvres, as the Apollo service module's; **toxic** | hydrazine from the ammonia of the worlds that have it (Ceres, Proxima b, Andromeda I); nitrogen tetroxide from air, by electric arc | the Haber process and the nitric acid of J's Industrial |
| **Hydrazine alone; cold gas** | about 230 s; about 70 s | the small thrusters of satellites and probes | the same hydrazine; nitrogen from the Atmosphere Extractor | |
| **High-test peroxide** | about 160 s | an alternative with no cold and no poison | none | the Peroxide Plant |
| **Ion propellant** (xenon, krypton, argon) | 1,500 to 3,000 s | a tiny thrust and almost no propellant spent: probes, satellites, tugs, never a take-off. Xenon is best; Starlink satellites fly on krypton and argon, cheaper | the **Atmosphere Extractor** on the Earth's air (at the game's rate, a thousand times the real one, as in J's Industrial), or the clathrates of the Deep Miner | the Air Separation Unit |
| **Nuclear thermal** (hydrogen heated by a reactor) | about 850 s | twice hydrolox; tested for real (NERVA, in the 1960s) | none: it is nuclear | fuel from J's Industrial's nuclear chain |
| **Metallic hydrogen** | about 1,700 s, predicted | the best "chemical" propellant possible | a deep aeroprobe in Jupiter and Saturn ([Minerals](minerals.md)) | the Diamond Anvil Press |
| **Deuterium and helium-3 fusion** | about 10,000 s | fast trips to the edge of the Solar System; starships | deuterium (from the Electrolyser's heavy water, from ices rich in it, from the giants) and helium-3 (the Moon, the giants) | J's Industrial's fusion |
| **Antimatter** | tens of thousands to millions of seconds | starships | the antiproton belts ([Minerals](minerals.md)) | the Antimatter Factory |

The warp drive's negative-mass exotic matter is in [Starships](starships.md).

**The ladder:** gunpowder (suborbital) → solids, and hydrolox or methalox (orbit) → hypergolics (landing on the Moon and
Mars) → ion, metallic hydrogen and, with J's Industrial, nuclear thermal (the outer Solar System) → fusion (the edge) →
antimatter and warp (the stars).

### The machines

| Machine | What it does | The real thing |
| --- | --- | --- |
| **Electrolyser** | water into hydrogen and oxygen, with **heavy water** gathering in what is left (deuterium); from brine, chlorine, caustic soda and **hypochlorite**, and by deeper electrolysis **chlorate and perchlorate**, the oxidiser of solid fuel | chlor-alkali cells; industrial perchlorate is made this way |
| **Liquefier** | hydrogen at −253 °C, oxygen at −183 °C, methane at −162 °C, nitrogen | the Linde and Claude cycles |
| **Sabatier Reactor** | carbon dioxide and hydrogen into **methane** and water | the one on the International Space Station, working since 2010 |
| **Atmosphere Extractor** | draws in the air of any world and separates its gases: on the Earth nitrogen, oxygen, argon, neon, krypton and xenon; on Mars carbon dioxide, nitrogen and argon; in MOXIE's way, **oxygen from carbon dioxide** | Perseverance's MOXIE made 122 g of oxygen on Mars, from 2021 to 2023 |
| **Arc Nitrogen Fixer** | air and an electric arc into **nitrogen tetroxide**, the hypergolic oxidiser | the Birkeland-Eyde process, Norway, 1903 |
| **Hydrazine Plant** | ammonia and hypochlorite into **hydrazine** | the Raschig process, 1907 |
| **Regolith Oven** | heats the Moon's regolith and frees the **helium-3**, hydrogen and neon of the solar wind | helium-3 comes out at about 700 °C |
| **Ilmenite Reducer** | ilmenite and hydrogen into iron, titanium oxide and **water**, and from the water oxygen | the real plan for lunar oxygen, at about 1,000 °C |

**Heavy water comes free.** As the Electrolyser splits water, heavy water gathers in what remains: that is how the
Vemork plant in Norway made heavy water in the 1940s, as a byproduct of electrolysis for ammonia. A player who
electrolyses a lot gets some deuterium thrown in.

### Moving propellants

J's Core has **basic lines** any mod uses on its own, as it has the FE converters: a low-voltage power cable, a liquid
pipe, a gas pipe and a **cryogenic pipe**, double-walled with a vacuum between, with recipes of vanilla materials. With
J's Industrial installed they are the first rung of its cables and pipes, and it adds all the others.

### Keeping propellants

- **Cold escapes.** Liquid hydrogen boils away slowly inside its tank, its "boil-off", so a tank loses a little over
  time. Better tanks lose less, and a tank with **active cooling** loses nothing but spends energy, as NASA's research
  on zero-boil-off tanks aims at. Oxygen loses less, methane less still, and **hypergolics lose nothing**: that is why
  long missions use them. It becomes a real design choice.
- **The pad's tank farm**: storage spheres in the launch complex, as the Kennedy Space Center's liquid hydrogen sphere
  (3.2 million litres), filling the rocket through the tower's hoses.
- **Dewars**: portable tanks to refuel in the field.
- **Making propellant where you land.** Robert Zubrin's Mars Direct (1990) proposed making the propellant for the way
  home **on Mars**. The same machines work on other worlds: methalox on Mars, liquid oxygen on the Moon, methane from
  the lakes of Titan, liquid oxygen from the seas of Terminus. Whoever brings the factory doesn't need to bring the fuel
  home.
