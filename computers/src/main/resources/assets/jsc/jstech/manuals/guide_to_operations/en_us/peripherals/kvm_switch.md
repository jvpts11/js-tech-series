---
section: peripherals
order: 77
icon: jsc:kvm_switch
---
# The KVM Switch

A KVM Switch lets one monitor reach every computer of a rack. It is a rack unit that you mount in a Server Rack ([](jsc:server_racks)).

> [!DEFINE] Rack unit
> One slot of height in a rack, counted in U. A KVM Switch takes one.

Each computer of the rack is a channel. A rack with one computer needs no KVM Switch: the monitor shows that computer. With two or more, the monitor first shows the channel bar, and you pick the machine you want.

> [!DEFINE] Channel
> One computer of a rack, chosen on the bar of a KVM Switch.

Table: The channel bar

| Part | What it shows |
| --- | --- |
| KVM SWITCH | The title of the bar. |
| Count | How many machines the rack holds, for example "2 machines". |
| F1 to F8 | The label of each channel, one row for each computer. |
| Name | The name of the machine. A machine with no name shows its place in the rack, such as "bay 3U". |
| Status | ONLINE when the machine runs, and OFF when it does not. |

Pick a channel with a click on its row, or with the number keys 1 to 8. The machine's own screen then opens on top. The bar holds 8 channels at most.

## If something goes wrong

> [!PROBLEM] This rack holds several computers - mount a KVM Switch to pick one
> Mount a KVM Switch in a free unit slot of the rack ([](jsc:server_racks)).

> [!PROBLEM] The rack holds no computer to show
> Put a computer in the rack.

> [!PROBLEM] The channel says OFF.
> That machine is not running: switch it on at its case.

> [!SEE] jsc:server_racks, jsc:rack_equipment, jsc:monitors
