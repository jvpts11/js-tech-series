---
section: racks
order: 58
icon: jsc:kvm_switch
covers_family: jsc:rack_equipment
---
# Rack equipment

Not everything in a rack computes. Some units serve the rack: the KVM Switch (keyboard, video, mouse), the Rack UPS (uninterruptible power supply) and its battery expansions, the PDU (power distribution unit), the Cooling Unit, the Vent Panel, the Fibre Optic Interface and the Drive Shelf take rack units, as a server does.

Table: Rack units that are not computers

| Unit | Takes | What it does |
| --- | --- | --- |
| KVM Switch | 1 U | Lets one monitor reach every machine in the rack |
| Rack UPS | 1 U | Carries the rack on its battery when the power goes: 2.7 MJ, up to 3 kW |
| Cooling Unit | 1 U | Adds to the heat budget, more in each newer era |
| Vent Panel | 1 U | Adds 500 W to the heat budget, in a rack of any era |
| Fibre Optic Interface | 1 U | Lets the rack's back take the Fibre Optic Cable |
| Advanced Fibre Optic Interface | 1 U | Lets the rack's back take the Fibre Optic Cable and the OM5 Fibre Cable |
| UPS Battery Expansion | 1 U | Adds 2.7 MJ to the Rack UPS's battery; they stack |
| PDU | 1 U | Measures each bay's draw, and lets IQL and jobs switch the bays |
| Drive Shelf | 2 U | Ten more drive bays for the server right above it |
| Tape Library | 2 U | Ten tapes of its era, where the Backup Service keeps its backups |

The KVM Switch, the Rack UPS and the Cooling Unit come in every era, from the Vintage to the Advanced. A rack seats the units of its own era and of the earlier ones, as it does servers: a Vintage KVM Switch goes in a Standard rack, a Standard one never in a Vintage rack.

The KVM Switch lets one monitor reach every machine in the rack, so you do not need a screen for each. A rack with two or more computers needs one to choose between them.

The Cooling Unit raises the heat the rack can carry, a newer era's more than an older one's, as explained in ([](jsc:rack_cooling_and_heat)). The Vent Panel is a perforated blank that adds 500 W, and only one is made for every era: it takes the colour of the rack it is mounted in.

From the Standard on, the backbone is fibre, and a rack takes it only through a Fibre Optic Interface. The Standard one takes the Fibre Optic Cable; the Advanced one, which only an Advanced rack seats, takes the OM5 Fibre Cable as well. One in any row of the rack is enough, and the servers in it need no network card ([](jsc:data_cables)).

A server's storage cards go in its expansion slots, not in the rack: the RAID controller joins the server's drives into one array, and the read cache cuts the time each read from them takes ([](jsc:raid_arrays)).

The Rack UPS carries the whole rack when the power goes: every one mounted holds 2.7 MJ and carries 3 kW, and every UPS Battery Expansion adds 2.7 MJ more, as many as the rack has room for. While the line brings enough, it charges on top of what the machines draw. On the battery, the rack's screen says ON BATTERY, each server's system says so, and at 10% they shut themselves down cleanly. Above what it carries the UPS is in bypass and protects nothing. Click a unit's faceplate to see its card.

The PDU measures what each bay draws, and its card lists it. It lays the bays' power open to the network: QUERY power lists every server of a rack with a PDU, with its watts and whether it is on, and UPDATE power SET on = no WHERE server = 'VAULT-01' switches it off, from a prompt or from a job ([](jsc:iql_jobs)).

A Drive Shelf adds its ten bays to the server mounted right above it: their drives join the server's storage and its array. A shelf with no server right above it does nothing.

A Tape Library holds ten tapes of its own era, and a drive that winds them: cold storage for the network's backups, huge and slow ([](jsc:backups)). It serves the network of the servers in its rack. Its card lists the tapes, what they hold and what the drive is doing: idle, winding, reading or writing.

## If something goes wrong

> [!PROBLEM] A rack with several computers has no way to pick one on the monitor.
> Mount a KVM Switch in the rack. It takes one rack unit.

> [!PROBLEM] A unit does not go in, and the rack says it belongs in a newer rack.
> The unit is of a newer era than the rack. Use the rack's own era's unit or an older one.

> [!PROBLEM] The OM5 Fibre Cable does not join the rack.
> The rack holds the Standard Fibre Optic Interface, which takes only the Fibre Optic Cable. Mount the Advanced one, in an Advanced rack.

> [!PROBLEM] The RAID controller does nothing.
> Its server has no mode set, or fewer drives than the mode needs ([](jsc:raid_arrays)).

> [!PROBLEM] The drives in a Drive Shelf do not show on any server.
> The shelf extends only the server right above it. Mount it in the row under the server.

> [!PROBLEM] The servers shut themselves down while the power was out.
> The Rack UPS's battery fell to 10%. Add UPS Battery Expansions to last longer.

> [!PROBLEM] UPDATE power says no bay is so.
> The rack needs a PDU, and the server has to be on the network.

> [!PROBLEM] A tape does not go into the Tape Library.
> The library takes only the tapes of its own era: the reel in the Vintage's, the DLT in the Legacy's, the LTO-3 in the Transition's, the LTO-6 in the Standard's and the LTO-9 in the Advanced's.

> [!SEE] jsc:server_racks, jsc:rack_cooling_and_heat, jsc:upses
