# Formulas

The arithmetic behind J's Space, gathered in one place. The numbers that feed it are in
[Implementation](implementation.md); every number of balance is an estimate, set by playing.

## What exists today

Only the arithmetic of rockets and of orbits is built, and it lives in the game's code, not in a page of its own. A
rocket's delta-v is Tsiolkovsky's, and it lifts off only when its thrust beats its weight on the world it stands on.
An astronomical unit is 10,000 blocks of a system's space and the Earth's year is a week of Overworld days, so each
planet keeps its real pace and the launch windows come round as they really do, Mars about every five real hours. A
launch out of the window costs more delta-v, up to twice from the worst place, and a trip is shortened by the delta-v
spent on it. A transfer can also use a gravity assist, which spends less and takes longer, and aerobraking where the
destination has air and the rocket has a heat shield, which saves the capture burn.

The rest of this page is not built yet: the light of a star, the arithmetic of measuring the stars, of knowing, of
keeping and of big things.

## To build

### Rockets

- **Delta-v of a stage**, Tsiolkovsky's rocket equation (1903):

  ```
  delta-v = Isp × g0 × ln(full mass / empty mass)
  ```

  with `g0` = 9.81 m/s²; a rocket's delta-v is the sum of its stages', each counted with the stages above it as its
  payload.
- **Thrust-to-weight ratio** on the world a rocket leaves from:

  ```
  ratio = thrust / (mass × gravity of the world)
  ```

  Under 1, the rocket doesn't leave the ground. A sea-level and a vacuum engine give different thrust and Isp in air and
  in vacuum.
- **Climb time** falls as the ratio rises: a rocket with plenty of thrust climbs fast.
- **A trip's time** falls as the delta-v spent above the minimum rises; the reference times are those of a trip of the
  usual kind ([Travel](travel.md)). Outside a launch window, the delta-v asked for rises.

### Time and distance

- **A world's day**, in the game: `the real day × 20 minutes / 24 hours`; the Overworld keeps its 20 minutes.
- **A year of the Earth** lasts 7 Overworld days. A **launch window** recurs every synodic period of the two worlds:
  `1 / |1/T1 − 1/T2|`, with their years `T1` and `T2`.
- **A system's space**: about 10,000 blocks for an astronomical unit; worlds are drawn far larger than to scale.

### Light and heat

- **Light from a star** at a distance `d`, for a star of luminosity `L`:

  ```
  light = L / d²     (the Sun at 1 AU = 1)
  ```

  A solar panel gives its rating × its efficiency × that light. Light is also shown as so many full moons: the full Moon
  is about 1/400,000 of sunlight at the Earth.
- **A world's gravity**: `g = G × M / r²`.
- **A world's equilibrium temperature**, for reference: `T = 278 K × ((1 − albedo) × L)^(1/4) / √d`, before its air
  warms it.

### Measuring the stars

- **Parallax**: a star's distance in parsecs is `1 / parallax` in arcseconds, measured from a baseline of 1 AU. From
  Terminus, 60 AU out, the same star shifts 60 times as much, which is what makes navigation-grade measurements
  ([Exploration](exploration.md)).

### Knowing

- **A world's discovery**: `properties known / properties of the world`.
- **A specimen** gives science only the first few times for each species.

### Keeping

- **Boil-off**: a cryogenic tank loses a share of its contents per day, set by its insulation; with active cooling it
  loses nothing and spends energy.
- **Radiation dose**: what a world gives per day, weakened by the shielding in between (J's Core's radiation crosses
  blocks weakened by their material), building up while a player stays.

### Big things

- **A Dyson swarm** gives energy in proportion to its collectors and to its star's luminosity; collectors wear out and
  are replaced.
- **A Mass Driver's** exit speed grows with the length of its rail.
