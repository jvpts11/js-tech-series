---
section: bus_settings
order: 126
icon: jsc:export_bus
---
# Filters, tags and matching

A filter is the list of things a bus is told to move. It has five slots, and each slot holds one item, one fluid or one chemical. A filled fluid container selects its fluid.

The two options for the list are ONLY THESE and ALL BUT THESE. ONLY THESE moves what the list names. ALL BUT THESE moves everything the list does not name.

## What moves

Table: What moves through a filter

| Filter | What moves |
| --- | --- |
| Empty, ONLY THESE, on an Import Bus | Everything, because an empty list takes everything. |
| Empty, ONLY THESE, on an Export Bus | Nothing. An Export Bus sends only what it lists. |
| Empty, ALL BUT THESE | Everything. |
| Items listed, ONLY THESE | Only the listed items. |
| Items listed, ALL BUT THESE | Everything except the listed items. |

## Setting the filter in the window

1. Hold an item in your hand.
2. Click a filter cell with the item, and the cell lists it.
3. To clear a cell, empty your hand and click the cell.
4. Choose ONLY THESE or ALL BUT THESE.

On a Transition bus, the + item button lists the item in the first free cell.

## Tags

Advanced buses also match tags. A tag names a group of items, such as c:ores, and a tag on the bus matches every item that carries it. The window takes up to 4 tags. Click + tag and type the tag, with or without the # sign.

A tag counts as a listed entry for ONLY THESE and ALL BUT THESE, the same way an item does.

## Loose matching

A filter normally matches an item exactly. An Advanced bus can match loosely instead, which the window calls FUZZY.

> [!DEFINE] Fuzzy matching
> Matching an item as the plain item it is, whatever its damage and its components are.

With FUZZY, a damaged or enchanted tool matches the plain tool. A fluid matches its fluid, not its bucket. The window's note reads "Fuzzy: an item matches whatever its damage and its components."

## If something goes wrong

> [!PROBLEM] The Export Bus sends nothing.
> Its filter is empty and set to ONLY THESE, so it lists no item. List the items, or choose ALL BUT THESE.

> [!PROBLEM] The Import Bus takes more than you wanted.
> Its filter is empty, and an empty list takes everything. List the items you want, with ONLY THESE.

> [!PROBLEM] A damaged or enchanted tool does not move.
> Choose FUZZY for MATCH on an Advanced bus, so a damaged or enchanted tool matches its plain item.

> [!SEE] jsc:bus_keep_max_and_priority
