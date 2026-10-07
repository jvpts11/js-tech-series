# Power

Power supplies, energy, what happens when a power supply fails or the power goes, and the UPS.

## Power supplies

Every part draws watts: processors, expansion cards, memory modules and disks. A computer's draw is the sum of its
parts, and its power supply has to give at least that: if the parts ask for more than the supply's rating, the assembly
is refused and the computer doesn't start, with the message "PSU insufficient". A power supply has to be sized for the
hardware it feeds.

A power supply has a rating in watts and an efficiency. Power supplies of any era go in any computer: a power supply is
just power ([Eras](eras.md)). The mod has twelve, from the MF PowerBasic 200 to the MF ServerPSU 3000P, all in
[the catalogue](catalogue.md).

A power supply can be declared self-sizing (it never refuses, and its rating is only a label). None of the mod's own
power supplies is; the option is there for add-ons ([The API](api.md)).

## Shutting down

A computer that is shut down cleanly (its button, the system's `shutdown`) loses nothing:

- every Operation running on it is abandoned: what it held goes back where it came from (a craft's pool goes back to
  storage, for example), and it is logged as DISCARDED with whatever it had left;
- the locks those Operations held are released.

On a Mainframe, the network's index lives in memory: it is cleared when the Mainframe stops and rebuilt when it starts
(ANALYZE), before the persistent Operations resume. Unloading the chunk or leaving the world doesn't discard persistent
Operations: they are saved on the Mainframe and resume when it loads again, unless they outlive the time an orphaned
Operation is kept ([Operations](operations.md)).

## To build

### Energy

Computers draw FE, which the power supply takes from any cable or block next to it that gives energy (J's Core's energy
capability, from any mod).

```
FE per tick = watts of the parts / efficiency of the power supply × the watt to FE factor
```

- The watt to FE factor is a balance key; at 0, computers run for free, for whoever plays without any energy mod.
- The rule for starting compares the parts' watts with the supply's **rating** (real power supplies are rated by what
  they give out); the efficiency only weighs on what is drawn from the grid.
- Without energy a computer doesn't start; losing the energy while it runs is a power loss (below).
- If J's Industrial ever has an energy system of its own, J's Computers gets a setting to use only that energy, or FE.

Today computers draw no energy, and the efficiency is only shown in the tooltip.

### Power supply failure

A power supply wears out and in the end fails. Failure is a consequence of the player's choices (a power supply too
small, a hot rack), not bad luck: whoever sizes power supplies well almost never sees it.

- **Wear** goes from 0 to 100%. It is kept on the item (it goes with the power supply from machine to machine) and only
  grows while the supply is on. How fast depends on the **load** (the parts' watts over the rating): up to 50%, the
  supply lives 8 times its base life; from 50 to 80%, 3 times; from 80 to 100%, its base life. In a rack over its
  thermal budget the life is halved ([Servers and racks](servers-and-racks.md)); with temperature, in block computers
  too ([Temperature](temperature.md)).
- **Base life**, at full load, by line (estimates; a game day is 20 real minutes):

  | Line | Base life | At 50% load or less |
  | --- | ---: | ---: |
  | PowerBasic | 60 game days | 480 |
  | PowerGold | 100 game days | 800 |
  | PowerPlat | 150 game days | 1,200 |
  | ServerPSU | 250 game days | 2,000 |

  Each power supply's life varies by ±20%, drawn once, the first time it is turned on: nobody knows the exact minute,
  and there is no roll per tick.
- **Warnings:** from 80% wear the power supply is **degraded**. The assembly window and the tooltip say so, the Device
  Manager marks it, the POST writes a warning at every boot, the Network Manager's log records it, and the supply starts
  a coil whine.
- **Failure:** at 100% the power supply **fails**. The computer shuts down suddenly, the way it does on a power loss
  (below), and doesn't start again ("PSU failure") until the supply is replaced. The item becomes "Failed" and can't be
  repaired (J's Industrial may recycle it).
- **Redundancy:** the MF ServerPSU 3000P (Redundant) has two units, each with its own wear, sharing the load. When one
  fails, the other takes the whole load without shutting anything down, and wears faster for it; the Device Manager and
  the window show "1 of 2 units failed". The server only shuts down with both failed. Redundancy protects against a
  power supply failing, not against the power going: for that there is the UPS.
- **Settings** (`jscomputers-server.toml`, `[hardware]`): `psu_failures = true` turns failures on or off (off, there is
  no wear and no warnings); `psu_life_multiplier = 1.0` (from 0.1 to 10) stretches or shortens every life.
- **Cost:** wear is worked out from the time on and the load when the machine changes state, or once a game minute;
  nothing per tick.

Today power supplies don't wear or fail, and the 3000P is an ordinary power supply named "(Redundant)".

### Shutting down suddenly

A computer also shuts down **suddenly**: on a **power loss** (the energy source goes away, a generator runs out of fuel,
an energy cable breaks) or a **power supply failure**. The Operations running stop and their locks are released as on a
clean shutdown, but the items they were carrying (in the memory buffer, on their way to a disk or to an inventory) **are
lost**, and the log says how many and of what. Windows close and Σ# programs stop.

At the next boot, the system checks its disk in its own way: ScanDisk on Frames 95, chkdsk from Frames XP on, fsck on
Linux, FreeBSD and UNIX. It takes a few seconds and erases nothing. A clean shutdown skips the check.

Today shutting down only exists in its clean form, and there are no disk checks.

### Items lost when the network fails

A player who lets the network fail pays for it. When the network goes down in the middle of work, the items in transit
are lost, in each situation the settings turn on:

- a power loss or a power supply failure (above);
- the network splitting in the middle of an Operation (a cable pulled between the Mainframe and a server, a router that
  disappears);
- a network conflict (two active Mainframes, [The network](network.md));
- storage corruption ([Storage](storage.md)).

There is one key per situation (`jscomputers-server.toml`, `[item_loss]`: `power_loss`, `network_split`,
`network_conflict`, `corruption`), all on by default. With a key off, that situation gives the items back like a clean
shutdown. What protects against the loss is what real life uses: the UPS, the redundant power supply and failover
between Mainframes ([Computers](computers.md)).

### The UPS

A UPS block per era, like the other peripherals, and the Rack UPS (1U) for a whole rack.

- It sits between the energy and the computer: energy goes into the UPS, the computer draws from it, and it charges its
  battery while energy flows.
- On a power loss, the machine keeps running from the battery, and the system shows it in its own way (the battery icon
  and "On battery" on Frames, a notification on Linux, a line on MC-DOS).
- At 10% battery, the system shuts itself down cleanly, as real UPS software does: no item is lost, and the next boot
  doesn't check the disk.
- The battery is in FE; how long it lasts is the battery divided by the draw. Estimates: about 5 game minutes at 300 W
  on a Vintage or Legacy UPS, 10 minutes at 800 W on a Standard one, 15 minutes at 3 kW on the Rack UPS. The rack's UPS
  Battery Expansions add battery ([Servers and racks](servers-and-racks.md)).

Today there are no UPS blocks, and the Rack UPS takes 1U with no effect.
