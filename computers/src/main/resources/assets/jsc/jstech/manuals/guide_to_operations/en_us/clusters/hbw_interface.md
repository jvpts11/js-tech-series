---
section: clusters
order: 63
icon: jsc:hbw_interface
covers_family: jsc:hbw_interface
---
# HBW Interface

A supercomputer is not one block. It is every Supercomputer Rack tied together by a fabric cable, and you meet it at the HBW Interface, the one point where it meets the rest of the network through the backbone.

The Standard has one, and the Advanced has another that takes the OSFP cable.

The interface looks along the high compute cable from it, through up to 256 cable steps, and counts each Supercomputer Rack on that run. Racks on any other run do not count.

1. Join the supercomputer racks to the interface with high compute cable.
2. Join the interface to the network's backbone.

## If something goes wrong

> [!PROBLEM] The supercomputer is not seen.
> Its racks are not all on the high compute cable that reaches the interface. Join every rack to that same high compute run.

> [!SEE] jsc:data_cables, jsc:supercomputers
