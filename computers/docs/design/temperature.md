# Temperature

Every computer makes heat, and what it can take away from itself decides how hard it can work.

## The rack's thermal budget

A rack is a cabinet with a **thermal budget** in watts: the heat it can take away. Every machine turned on in it puts
its draw in watts into the cabinet ([Power](power.md)). The cabinet takes away 1,000 W on its own, and every Cooling
Unit mounted in it adds 1,500 W.

Over the budget, **every** machine in the cabinet loses the same fraction (the budget over the load), never going below
25%: a hot rack slows down, it doesn't shut down. The cut applies to the machines' capacity and to the index's
throughput ceiling, and the rack's window shows "THROTTLED x%". The numbers are estimates.

A rack over its budget also halves the life of its power supplies once they wear ([Power](power.md)).

## The thermal budget of every computer

Every computer, a block or in a rack, gets a thermal budget in watts: the heat it can take away from itself. The heat it
makes is the watts of its parts. Every number here is an estimate.

- **A block computer** adds up what its case, its case fans and its processors' coolers take away. The case takes some
  away on its own and has slots for fans:

  | Case | Takes away | Fan slots | Rear, top, front |
  | --- | ---: | ---: | --- |
  | Vintage AT minitower | 60 W | 1 | 0, 0, 1 |
  | Legacy ATX tower | 100 W | 2 | 1, 0, 1 |
  | Transition mid tower | 150 W | 3 | 1, 0, 2 |
  | Neutral (Standard, Advanced) | 200 W | 3 | 1, 0, 2 |
  | High Performance, a mesh front (Standard, Advanced) | 300 W | 6 | 1, 2, 3 |
  | Aesthetic, glass panels: handsome and hotter (Standard, Advanced) | 150 W | 4 | 1, 1, 2 |

- **The cooling parts**, in the watts each takes away:

  | Part | Vintage | Legacy | Transition | Standard | Advanced |
  | --- | ---: | ---: | ---: | ---: | ---: |
  | Stock processor cooler (a bare heatsink in the Vintage) | 20 | 60 | 100 | 150 | 200 |
  | Tower cooler | | | | 250 | 300 |
  | All-in-one water cooler, the MF AquaLoop 240 or 360 | | | | 300 | 400 |
  | Case fan (80, 80, 92, 120 and 120 mm) | 20 | 30 | 40 | 50 | 60 |

  Every processor has a cooler slot over its socket. A water cooler takes the cooler slot and two of the case's fan
  slots for its radiator: the two on top when the case has them, otherwise the two at the front; the 360 mm one
  takes two as well. In the cases with a 5.25-inch bay, the drive there runs across the upper front fan, as it does
  in the High Performance and Aesthetic cases. A processor with no cooler adds nothing to the budget.
- **Quince's all-in-ones** have the budget of their era's case, fixed: their insides are closed
  ([Quince](quince.md)).
- **A Mainframe's** cabinet takes away 1,500 W, plus its processors' coolers, one over each socket.
- **A server in a rack** has no cooler of its own: the rack cools it, and the rack's budget counts no coolers.
- **A rack** has 1,000 W, plus 1,500 W for every Cooling Unit, and every Vent Panel adds 500 W
  ([Servers and racks](servers-and-racks.md)). The rack has one temperature for the whole cabinet, shared by every
  machine in it as the budget is, and follows the same thresholds as every other computer: its cut (the budget over
  the load, down to 25%) is exactly what the throttling at 90 °C does.
- **Portable devices** (PDAs, smartphones, tablets) have no temperature.

An example: a Standard gaming PC (a 95 W processor, a 250 W graphics card and 50 W for the rest) in a Neutral case with
three fans and the stock cooler has a 500 W budget, and sits at about 68 °C.

**Where the player fits it:** the assembly window has two faces, BOARD and COOLING, in tabs under its lamps. COOLING
shows the case's fan slots where they are (rear, top and front) and a cooler slot over each processor; the tab's lamp
lights when the machine lacks cooling for what it holds. The Standard and Advanced towers always have a second row of
seats, for a second processor and memory modules 5 to 8, shown only when a two-socket board is in
([Hardware](hardware.md)).

All of this is in J's Computers, without J's Industrial. J's Industrial's machines (heat pipes, chillers, towers) can
join later, trading heat through a heat capability of J's Core.

## Temperature and its thresholds

The **temperature** is the surroundings' temperature plus the rise the load causes: the surroundings + 60 × (the load ÷
the budget). With the surroundings at 20 °C and the load equal to the budget, the machine is at 80 °C.

The **surroundings** come from the biome: 20 + 15 × (the biome's vanilla temperature − 0.8), and the Nether adds 10.
That gives about 20 °C in the plains, 38 °C in the desert and 8 °C in the snow: a closed PC in the Nether really heats
up.

The thresholds follow a real PC's:

| Temperature | What happens |
| --- | --- |
| up to 80 °C | full performance |
| 80 to 90 °C | performance drops little by little, like a processor that stops holding its boost (down to about -15% at 90 °C) |
| 90 °C | **thermal throttling**: the machine cuts its clock so as not to go past 90 °C; its capacity and its programs' credits drop by whatever fraction is needed, down to a floor of 25% |
| 100 °C | **thermal trip**: if not even 25% holds the temperature, it shuts down suddenly, like a real processor's emergency cut-off |

A thermal trip is a sudden shutdown ([Power](power.md)): the items in transit are lost (if the settings say so), and
each disk has a 25% chance of being corrupted, in 5% of what it holds (estimates, [Storage](storage.md)). The machine
only starts again once it has cooled below 60 °C, or below its surroundings plus 5 °C where they are hotter than 55 °C,
and the POST shows the real warning, "CPU over temperature". This only happens to whoever gets it wrong (a processor
with no cooler, a closed case in the Nether, too few fans for what is inside): a machine sized well stays under 80 °C.

## Where it shows, and what else it weighs on

- The temperature shows in the Local header, in the Task Manager's graph ([Interface](interface.md)), in the Device
  Manager, in the rack's window ("THROTTLED x%" with the degrees) and in the firmware, whose hardware page shows the
  processor's temperature, the case fans that turn and the power supply's wear, as real BIOSes do. The POST's warnings
  (a degraded power supply, a thermal trip at the last shutdown) never stop the boot.
- A machine slowing down from heat wears its power supply twice as fast ([Power](power.md)).
- **Cost:** everything is worked out from the draw kept when the machine changes state; nothing looks at the neighbours
  every tick.
- There is no switch to turn temperature off ([Overview](overview.md)); the surroundings and the restart are in the
  settings:

  ```toml
  [temperature]
  ambient_base = 20                        # the surroundings in a biome of vanilla temperature 0.8, in °C
  biome_factor = 15                        # °C for every point of the biome's vanilla temperature
  nether_extra = 10                        # °C added in the Nether
  restart_below_c = 60                     # a machine that tripped starts again under this
  ```

## To build

Nothing on this page is left to build.
