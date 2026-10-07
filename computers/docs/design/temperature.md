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
makes is the watts of its parts.

- **A block computer** gets its budget from its **case** (the Neutral, High Performance and Aesthetic cases take heat
  away differently), from the processor's **cooler** and the case's **fans**, which are parts, and from **water
  cooling** (an all-in-one cooler) in the Standard and the Advanced, also a part.
- **A rack** keeps its budget as it is today, and the Vent Panel adds a little
  ([Servers and racks](servers-and-racks.md)); every machine in the cabinet shares the budget.
- **The surroundings:** a hot biome (the desert, the Nether) takes a little off the budget, and a cold one adds a little
  (small estimates).

All of this is in J's Computers, without J's Industrial. J's Industrial's machines (heat pipes, chillers, towers) can
join later, trading heat through a heat capability of J's Core.

### Temperature and its thresholds

The **temperature** comes from the load: the surroundings' temperature plus the rise the load causes. With the load
equal to the budget, the machine is at 80 °C. The thresholds follow a real PC's (estimates):

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
- There is no switch to turn temperature off ([Overview](overview.md)); its numbers are in the settings.

Today only the rack's thermal budget exists, in watts, with its cut down to 25%; block computers have no temperature.
