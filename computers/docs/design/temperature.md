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

## To build

### The thermal budget of every computer

Every computer, a block or in a rack, gets a thermal budget in watts: the heat it can take away from itself. The heat it
makes is the watts of its parts. Every number here is an estimate.

- **A block computer** adds up what its case, its case fans and its processors' coolers take away. The case takes some
  away on its own and has slots for fans:

  | Case | Takes away | Fan slots |
  | --- | ---: | ---: |
  | Vintage AT minitower | 60 W | 1 |
  | Legacy ATX tower | 100 W | 2 |
  | Transition mid tower | 150 W | 3 |
  | Neutral (Standard, Advanced) | 200 W | 3 |
  | High Performance, a mesh front (Standard, Advanced) | 300 W | 6 |
  | Aesthetic, glass panels: handsome and hotter (Standard, Advanced) | 150 W | 4 |

- **The cooling parts**, in the watts each takes away:

  | Part | Vintage | Legacy | Transition | Standard | Advanced |
  | --- | ---: | ---: | ---: | ---: | ---: |
  | Stock processor cooler (a bare heatsink in the Vintage) | 20 | 60 | 100 | 150 | 200 |
  | Tower cooler | | | | 250 | 300 |
  | All-in-one water cooler, 240 or 360 mm | | | | 300 | 400 |
  | Case fan | 20 | 30 | 40 | 50 | 60 |

  Every processor has a cooler slot on the motherboard. A water cooler takes the cooler slot and two of the case's fan
  slots, where its radiator goes. A processor with no cooler adds nothing to the budget.
- **Quince's all-in-ones** have the budget of their era's case, fixed: their insides are closed
  ([Quince](quince.md)).
- **A Mainframe's** cabinet takes away 1,500 W, plus its processors' coolers.
- **A rack** keeps its budget as it is today (1,000 W, plus 1,500 W for every Cooling Unit), and every Vent Panel adds
  500 W ([Servers and racks](servers-and-racks.md)). The rack has one temperature for the whole cabinet, shared by every
  machine in it as the budget is, and follows the same thresholds as every other computer: the cut it has today (the
  budget over the load, down to 25%) is exactly what the throttling at 90 °C does.
- **Portable devices** (PDAs, smartphones, tablets) have no temperature.

An example: a Standard gaming PC (a 95 W processor, a 250 W graphics card and 50 W for the rest) in a Neutral case with
three fans and the stock cooler has a 500 W budget, and sits at about 68 °C.

All of this is in J's Computers, without J's Industrial. J's Industrial's machines (heat pipes, chillers, towers) can
join later, trading heat through a heat capability of J's Core.

### Temperature and its thresholds

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
there is a **chance of the disk being corrupted** ([Storage](storage.md)). The machine only starts again once it has
cooled below 60 °C, and the POST shows the real warning, "CPU over temperature". This only happens to whoever gets it
wrong (a processor with no cooler, a closed case in the Nether, too few fans for what is inside): a machine sized well
stays under 80 °C.

### Where it shows, and what else it weighs on

- The temperature shows in the Local header, in the Task Manager's graph ([Interface](interface.md)), in the Device
  Manager, in the rack's window ("THROTTLED x%" with the degrees) and in the firmware, which shows the processor's
  temperature, as real BIOSes do.
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

Today only the rack's thermal budget exists, in watts, with its cut down to 25%; block computers have no temperature.
