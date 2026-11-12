# Create

[Create](https://modrinth.com/mod/create) is a mod of machines that turn, and of contraptions: blocks that a bearing,
a piston, a pulley or a train carries off and sets down again. J's Computers works with it when it is installed, on
the versions it is tested against (Create 6.0.11 and the 6.0 line after it); on another version this work stays off
and the log says so. Without Create, nothing here is there, and nothing else about the mod changes.

## Computers on contraptions

A contraption takes its blocks out of the world while it moves, so a computer cannot run on one. It rides, and comes
down where the contraption sets it **switched off**: shut down cleanly, as switching it off does, with its system,
its parts, its files and its node on the network. It stays off until someone turns it on.

A computer doing what it cannot lose is not carried: one copying files, or installing a system. When the contraption
starts from that computer, or moves into it, Create says the block cannot be moved and the contraption does not
assemble; the computer's own screens show what it is doing. Glued on beside, it is left where it stands, as Create
leaves any block it may not move.

The big machines ride whole: the Mainframe, the Server Rack, the Mining Computer and the WAN Gateway. Each of their
blocks holds on to the others, so a contraption that takes one takes them all, and each part keeps the way to the
machine's origin from itself, turned as the contraption turned it, so the machine works where it is set down. The
Mainframe, the Mining Computer and the WAN Gateway come down switched off, and a rack with its bays switched off.

## Computers on ships

[Create Aeronautics](https://modrinth.com/mod/create-aeronautics) builds ships whose blocks stay real while they fly,
on Sable's sub-levels. A computer aboard keeps running in flight. A ship talks to the
ground by [Wi-Fi](NETWORK.md#wi-fi): a computer aboard with a Wi-Fi card joins an access point on the ground within
its reach, measured from where the ship flies now, and an access point aboard reaches computers on the ground the same
way. A data cable never crosses between them.

## Create's machines on the network

With [J's Industrial](../../industrial/README.md) installed as well, its industrial control cable reads Create's
turning blocks (a motor, a shaft, a gearbox, a Rotation Speed Controller...) for the computer with a fieldbus card it
is linked to, and the network lists them in the `create_kinetics` table:

| Column | What it holds |
| --- | --- |
| `block` | The block's name. |
| `speed` | How fast it turns, in RPM; negative the other way. |
| `stress` | The stress its kinetic network carries, in stress units (su). |
| `capacity` | The stress that network can carry, in su. |
| `overstressed` | Whether it carries more than it can: yes or no. |
| `target_speed` | On a Rotation Speed Controller, the speed it aims at, in RPM. |
| `at` | Where it stands. |

```
QUERY create_kinetics WHERE overstressed = yes
UPDATE create_kinetics SET target_speed = 64 WHERE block = 'Rotation Speed Controller'
```

`UPDATE create_kinetics SET target_speed = ...` aims every Rotation Speed Controller its `WHERE` keeps at that speed,
from -256 to 256 RPM; a block it keeps that is no Rotation Speed Controller is left as it is, and the reply counts it.
Clutches and gearshifts are worked by redstone: a [Redstone Interface](SIGMA.md#redstone-redstone) works them.
