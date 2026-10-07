# The cosmological simulator

The simulator is fixed in outline; its details are designed with the Singularity's hardware. Nothing of it exists yet.

## To build

What is decided:

- The simulator is a system of J's Computers, of the **Singularity** era: the endgame system, and the one that most
  needs that era's hardware ([Eras](eras.md)).
- The simulation cluster is part of the main network: the Mainframe dispatches the SIMULATE Operation, and the heavy
  work runs on the nodes, not on the Mainframe ([Operations](operations.md)). The cluster's nodes are **Simulation
  Nodes**, computers of their own ([Computers](computers.md)).
- It generates **materials**, registered in J's Core (the series' periodic table belongs to it): ores in enormous
  amounts and the exotic materials. J's Industrial uses those materials in its recipes, and neither mod depends on the
  other. Which combination of parameters gives which material is designed together with J's Industrial, which says what
  each material is for ([The series](series.md)).
- It does **not** generate Teracoin: it is not a second way of mining ([Teracoin](teracoin.md)).

What is designed with the Singularity's hardware: the nodes' hardware (their motherboard, processors and memory), the
cluster's cable, the switch that joins the nodes into a cluster, the Simulation Interface card, the simulation's
parameters and times, and the virtual probes.
