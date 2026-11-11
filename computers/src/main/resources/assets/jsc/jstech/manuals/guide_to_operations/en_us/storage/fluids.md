---
section: storage
order: 122
icon: minecraft:water_bucket
---
# Fluids and chemicals

The network keeps fluids the way it keeps items: as data, counted in millibuckets (mB). A bucket holds 1,000 millibuckets, so a full bucket takes exactly as much room as one item ([](jsc:items_as_data)).

> [!DEFINE] Millibucket (mB)
> One thousandth of a bucket. Fluids and chemicals are counted in these, and each one takes one thousandth of an item's room.

An External Storage Bus can face a tank of any mod. The network then uses the tank's fluid as storage, as it does with items.

Fluid also comes in and goes out in containers. You can use a bucket or a tank for it, and each behaves its own way.

Table: Containers and the network

| Container | What happens |
| --- | --- |
| Bucket | Gives up its whole 1,000 mB, or nothing. The empty bucket comes back. |
| Tank item | Gives up as much as fits, up to what the item allows at once. The emptied tank comes back. |

Taking fluid out works the other way. A bucket is filled only with a full bucket's worth. Any fluid the network could not take goes back into the container.

## Chemicals

> [!DEFINE] Chemical
> A substance that a mod such as Mekanism keeps in its own tanks.

> [!DEFINE] Bridge
> The piece of this mod that teaches the network how to read a chemical mod's tanks.

Chemicals take room in millibuckets, like a fluid. They need Mekanism: install it, and a chemical bridge lets the network read its chemical tanks ([](jsc:mekanism)).

## If something goes wrong

> [!PROBLEM] The fluid does not go in.
> The network's storage has no room left. Add disks or servers ([](jsc:storage_trouble)).

> [!PROBLEM] No chemical shows in the network's storage.
> Install Mekanism. The network reads chemicals only through it.

> [!SEE] jsc:items_as_data, jsc:external_storage_bus, jsc:mekanism
