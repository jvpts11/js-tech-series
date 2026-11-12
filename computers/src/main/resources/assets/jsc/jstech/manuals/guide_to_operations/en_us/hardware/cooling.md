---
section: hardware
order: 26
icon: jsc:tower_cooler_standard
covers_family: jsc:cooling
---
# Cooling and temperature

Every part you put in a computer turns the watts it draws into heat, and the computer has to take that heat away. What it can take away is its heat budget: what its case takes away on its own, and what the coolers on its processors and its case fans add.

> [!DEFINE] Heat budget
> The watts of heat a computer can take away from itself.

Table: What each case takes away

| Case | Takes away | Fan seats |
| --- | --- | --- |
| Vintage AT minitower | 60 W | 1 front |
| Legacy ATX tower | 100 W | 1 rear, 1 front |
| Transition mid tower | 150 W | 1 rear, 2 front |
| Neutral, Standard and Advanced | 200 W | 1 rear, 2 front |
| High Performance, Standard and Advanced | 300 W | 1 rear, 2 top, 3 front |
| Aesthetic, Standard and Advanced | 150 W | 1 rear, 1 top, 2 front |

Table: What each cooling part takes away

| Part | Vintage | Legacy | Transition | Standard | Advanced |
| --- | --- | --- | --- | --- | --- |
| Stock cooler | 20 W | 60 W | 100 W | 150 W | 200 W |
| Tower cooler | none | none | none | 250 W | 300 W |
| MF AquaLoop 240 or 360 | none | none | none | 300 W | 400 W |
| Case fan | 20 W | 30 W | 40 W | 50 W | 60 W |

You fit them on the COOLING face of the assembly window: open it with its tab under the lamps. Each processor has a cooler seat over its socket, and the case's fan seats are where the case has them, at the rear, on the top and at the front. The tab's lamp lights while the computer lacks cooling for what it holds. A cooler or a fan of the case's era or an older one goes in; a later one does not.

A water cooler, the MF AquaLoop, takes its processor's cooler seat and two of the case's fan seats for its radiator: the two on the top when the case has them, otherwise the two at the front. The 360 mm one takes two as well. Its radiator brings its own fans.

The temperature is the surroundings' plus 60 °C times the heat over the heat budget: with the surroundings at 20 °C and the heat equal to the budget, the computer is at 80 °C. The surroundings come from the biome, about 20 °C in the plains, 38 °C in the desert and 8 °C in the snow, and the Nether adds 10 °C.

Table: What the temperature does

| Temperature | What happens |
| --- | --- |
| Up to 80 °C | Full speed |
| 80 °C to 90 °C | The speed drops little by little, to 85% at 90 °C |
| 90 °C | Throttling: it slows down just enough to stay at 90 °C, down to 25% of its speed |
| 100 °C | Thermal trip: when not even 25% holds the heat, it shuts down at once |

Slowing down cuts the computer's capacity and its programs' credits, and wears its power supply twice as fast ([](jsc:power_supplies)). A thermal trip is a sudden shutdown: the disks are checked at the next start, and the computer does not start again until it has cooled below 60 °C, or to within 5 °C of surroundings warmer than 55 °C. Its firmware's self-test then warns of the trip, and the boot goes on.

The temperature takes about half a minute to get most of the way to where a change leaves it. You read it in the assembly window, on the firmware's hardware page, under Cooling in the Device Manager, and on the TEMP tile of the terminal's Local tab.

A Mainframe's cabinet takes away 1,500 W on its own, and a cooler over each of its sockets adds to it. A server in a rack has no cooler of its own: the rack cools it ([](jsc:rack_cooling_and_heat)).

## If something goes wrong

> [!PROBLEM] The computer is slower than it should be, and reads 90 °C.
> It makes more heat than it takes away. Fit a better cooler or more case fans, or take a part out.

> [!PROBLEM] The computer went dark and does not start.
> It tripped at 100 °C. Wait until it has cooled below 60 °C, and cool it better before you start it again.

> [!PROBLEM] A cooler or a fan does not go in.
> It is from a later era than the case, the seat is under a water cooler's radiator, or a water cooler finds no two free seats for its radiator.

> [!SEE] jsc:power_supplies, jsc:rack_cooling_and_heat
