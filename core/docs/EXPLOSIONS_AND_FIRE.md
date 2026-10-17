# Explosions and fire

Explosions with real physics, decided by their energy and not by a radius: the shock wave leaves at the speed of
sound, pushes through blocks by the strength of each, and hurts and throws what it reaches by the overpressure there.
A casing breaks into fragments, broken blocks fly as debris, explosives it breaks go off, and the ground round the
crater is scorched. The medium decides the rest: a vacuum carries no wave, water carries it far. It is in the spirit
of the explosions of HBM's Nuclear Tech, with the figures of real blasts.

The design behind this page is [Explosions and fire](design/explosions-and-fire.md).

## The words

- A **blast** is an explosion of the Core's: its **energy** in joules, and its **TNT equivalent**, the kilograms of
  TNT that give that energy (4.184 MJ each).
- **Overpressure** is how far the shock wave lifts the air's pressure above its own as it passes, in kilopascals. It
  is what breaks and what hurts.
- A block's **strength** is the overpressure it stands before it breaks.
- The **front** is how far the shock wave has gone.
- The **medium** is what the blast goes out through: air at its pressure and temperature, water, or a vacuum.
- A **consequence** is what a blast does to the world: everything, all but breaking blocks, or nothing but its show.

## Setting one off

*Added 2026-10-17.*

```java
Optional<Detonation> blast = Explosions.blast(level, centre, MyMod.id("demolition_charge"), 20.0e6)
        .by(player)                          // who set it off: blamed for it, held to their claims
        .casing(SeriesMaterials.IRON, 0.5)   // half a kilogram of iron, flung out as fragments
        .incendiary()                        // its fireball sets fires where there is air to burn
        .detonate();
```

The id names the explosive, and its mod's setting decides what it does to the world. Remove the explosive's own block
before setting it off: the centre's block never shields, but a block left there still stands in the world. Nothing
comes back when another mod or a protection cancelled it as it began.

`Detonation` says how it goes: `front()`, `reach()`, `bodyReach()`, `fireball()`, `brokenBlocks()`, `waiting()`,
`thrownDebris()`, `struckByFragments()` and `finished()`. `Explosions.running(level)` lists the blasts going out.

| Charge | TNT | Breaks stone to | Hurts a body in the open to | Breaks windows to |
| --- | --- | --- | --- | --- |
| A hand grenade's filling | 0.2 kg | 1.2 blocks | 6 blocks | 25 blocks |
| A demolition charge | 5 kg | 3.5 blocks | 17 blocks | 72 blocks |
| An aircraft bomb | 250 kg | 13 blocks | 63 blocks | 256 blocks, the farthest any blast reaches |

## The shock wave

*Added 2026-10-17.*

The overpressure at a distance is the Kinney and Graham fit to real blasts in air at sea level: about a thousand
kilopascals a metre from a kilogram of TNT, ten at ten metres; a thousand times the charge gives the same pressure ten
times as far. `BlastMath` holds the figures, pure: `overpressure(tnt, metres)`, `reach(tnt, kilopascals)`,
`arrival(metres, speed)`.

The wave goes out along lines spread evenly over the sphere, walked block by block. Where it meets a block it breaks
it if the overpressure there is more than the block stands, and spends that much of itself; a whole block it cannot
break stops that line, so behind a strong wall one is safe. It never breaks a block nothing may break (bedrock), nor
one a protection keeps.

The wave reaches each place in its own time: a tick at a time, it goes as far as the medium's speed of sound takes it,
so glass breaks far away a moment after the flash, and the boom arrives after both, by the sound kit's own delay.

### How strong a block is

The data map `jscore:blast_strength` on blocks gives what each stands, in kilopascals:

```json
{ "values": { "mymod:blast_door": 50000, "#c:glass_panes": 2 } }
```

The Core fills it for the game's blocks near what the real things stand: a window's pane 2, a glass block 4, a wooden
door 20, earth 60, planks 80, logs 120, brick and cobblestone 150, stone 200, concrete 700, iron blocks 5,000,
obsidian 20,000. A block it says nothing of stands what its resistance to the game's explosions says, from 15 for a
flower or a torch up.

## Bodies, fragments and debris

*Added 2026-10-17.*

A body the wave reaches takes the overpressure where it stands, less what each whole block between it and the blast
stood. Past 10 kPa it hurts, a half heart for each 5 kPa more, and throws the body away from the blast; the game's
blast protection and explosion knockback resistance count as they do for the game's explosions. Inside the fireball
a body catches fire where there is air to burn, and in a vacuum the heat of it scorches.

A casing breaks into fragments of its own material, about two grams each, at the speed Gurney's equation gives for
its weight against the charge's. They fly straight out and outrun the wave; the medium's drag slows them, the denser
the fragment the less, and in a vacuum nothing does. The first body on a fragment's line takes what it still carries,
a half heart for each 50 joules, as `jscore:shrapnel`; a block on the way stops it. Shrapnel is a projectile for
armour's projectile protection, and of the blast kind of harm.

Blocks in `jscore:thrown_by_blasts` (earth, sand, gravel, stone, wood, brick, concrete) close enough to the blast are
now and then thrown as debris instead of broken: they fly, fall and land as blocks, hurting what they fall on.

## The medium

*Added 2026-10-17.*

`Explosions.mediumAt(level, at)` finds it: water where the blast goes off in water; else the air of the sealed room
or of the place, at its pressure and temperature. The speed of sound follows the air's temperature. Thin air carries a
weaker wave, by Sachs's scaling; below 1 kPa it is a vacuum and carries none, only the fragments and the heat (and
whatever a mod gives off itself, as radiation through the radiation kit). Under water the wave travels at 1,480 m/s
and carries three times as far. Where the laws of a place slow explosions, the wave goes at their pace.

`.through(medium)` gives a blast its medium, for a mod's own place with its own air: `BlastMedium.air(kilopascals,
celsius)`, `BlastMedium.WATER`, `BlastMedium.VACUUM`.

## The crater

*Added 2026-10-17.*

What the wave breaks drops what the game's explosions drop of it, the farther it reaches the less. An explosive it
breaks goes off: the game's TNT is lit with a short fuse, and a mod's explosive goes off as its block does when the
game's explosions break it. The ground round the crater is scorched out to two and a half times the fireball's radius:
grass, moss and mycelium (the tag `jscore:scorched_by_blasts`) turn to coarse earth. An incendiary blast lights a
share of the crater's floor. The explosion effect `jscore:explosion` shows the fireball, the smoke, the sparks, the
flash, the shake of the screen and the scorch marks, as large as the fireball. The game's own explosions show it too.

## Other mods, protections and the server

*Added 2026-10-17.*

A blast is the game's explosion to the rest of the game: `Explosions.isBlast(explosion)` tells one apart. As it begins
it raises `ExplosionEvent.Start`, which may cancel it; each tick it raises `ExplosionEvent.Detonate` with the blocks it
is about to break and the bodies it is about to strike, which another mod or a claim may take out. A machine of any
mod is damaged instead of broken, as by the game's explosions.

Claimed land is respected: a block a protection keeps from `EXPLODE` stands, and shields as a wall, and a body is hurt
only as the protections allow whoever set the blast off. A blast set off by no one may not reach into a claim from
outside it.

The destruction is spread over ticks within a budget: the blasts of a level look at so many blocks a tick and break so
many, and the rest goes on over the next ticks, so a great blast never stalls the server.

## Settings

*Added 2026-10-17.*

In the world's balance file, under `explosions`:

| Setting | Default | What it does |
| --- | --- | --- |
| `consequences` | none | What each mod's explosions do, by its id or by an explosive's: `full`, `spare_blocks` or `harmless`. |
| `blocks_looked_at` | `32768` | How many blocks the blasts of a level look at in a tick. |
| `blocks_broken` | `1024` | How many blocks the blasts of a level break in a tick. |
| `show_game_explosions` | `true` | Whether the game's own explosions show the fireball, the smoke and the scorch marks. |

```toml
[explosions.consequences]
jsindustrial = "spare_blocks"
"jswarfare:test_charge" = "harmless"
```

A mod with a setting of its own passes it with `.atMost(consequence)`; the server's still holds.
