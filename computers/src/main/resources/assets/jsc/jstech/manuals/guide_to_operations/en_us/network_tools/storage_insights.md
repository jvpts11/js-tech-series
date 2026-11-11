---
section: network_tools
order: 88
icon: jsc:cd_rom
---
# Storage Insights

Storage Insights shows where the network's storage went. It is a dashboard of the network's contents: the totals, the biggest types of item, the types that are running low, and how full each server is. You open it when you want to know what the network is short of.

Table: Tiles of Storage Insights

| Tile | What it counts |
| --- | --- |
| TYPES | The number of item types the network holds. |
| TOTAL | The total of the network's contents. |
| LOW | The number of item types running low. |

Table: Sections of Storage Insights

| Section | What it shows |
| --- | --- |
| TOP ITEMS | The biggest types, as a bar chart. |
| LOW STOCK | The types under the threshold, or all stocked when none are. |
| BY SERVER | How full each server is. |

> [!DEFINE] Threshold
> The count under which an item type is listed as running low. It starts at 64 items.

Use the control labelled Threshold to change the count. Click a type to see its detail view. The detail view has a Back button, the total of the type, the places it is STORED IN, what it is USED TO MAKE, and the [buses](jsc:buses) that filter it. A type no bus filters is marked as not on any bus filter.

The search field, which says search item..., narrows the list. The dashboard asks the network again on its own, every so often, so the numbers keep up with the network.

## If something goes wrong

> [!PROBLEM] The list says no match.
> No item type fits the search. Try a shorter word.

> [!PROBLEM] The dashboard shows no numbers.
> The network holds no items yet. Put items on the network, and the numbers will follow.

> [!SEE] jsc:buses, jsc:items_as_data, jsc:storage
