---
section: mod_integrations
order: 160
---
# Mekanism

Mekanism is a mod with its own machines and its own chemicals: gases and other substances that its machines handle. When Mekanism is installed, J's Computers reads those chemicals and handles them as data, the way it handles fluids.

A chemical moves through the network and sits on disks, counted in millibuckets (mB), the same unit as a fluid. So you store a chemical the way you store a fluid, and the disk's usage line shows it as a number of mB of chemical.

The network names a chemical by its registry id, with the prefix chemical/. The oxygen of Mekanism, for example, is named chemical/mekanism:oxygen.

Table: Chemical names

| Name | What it names |
| --- | --- |
| `chemical/mekanism:oxygen` | The oxygen chemical of Mekanism. |
| `chemical/<registry id>` | Any other chemical, by its registry id, from the mod that adds it. |

> [!DEFINE] Chemical (Mekanism)
> A gas or similar substance that Mekanism handles like a fluid.

Computers adds no Mekanism recipe and no Mekanism machine of its own. Autocrafting works with any machine that offers the usual item, fluid, energy and chemical access, from any mod ([](jsc:how_autocrafting_works)).

The buffer of a Network Gateway takes items only, so a chemical pulled through a Gateway moves nothing into its buffer ([](jsc:computercraft_peripheral)).

## If something goes wrong

> [!PROBLEM] The network says "unknown chemical:" followed by a name.
> The name is not a chemical the game knows. Check that Mekanism is installed, and that the name is chemical/ followed by the chemical's registry id.

> [!PROBLEM] A chemical does not move.
> Check that Mekanism is installed. Without it, chemicals are kept by their id, but every query answers that the chemical is unknown.

> [!SEE] jsc:items_as_data, jsc:how_autocrafting_works
