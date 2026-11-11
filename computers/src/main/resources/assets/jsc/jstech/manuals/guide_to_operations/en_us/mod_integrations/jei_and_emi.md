---
section: mod_integrations
order: 159
---
# Recipe viewers: JEI and EMI

A recipe viewer is a screen that lists how things are made. JEI and EMI are two of them, and each one is optional. With JEI installed, you can send a recipe from the viewer to the Pattern Studio, which writes the patterns for autocrafting ([](jsc:pattern_studio)).

In JEI, each recipe you can send has a transfer button. Pressing it moves the recipe's ingredients into the Pattern Studio. The Studio has two tabs for this, and the recipe goes to the one it fits.

## Where the recipe lands

Table: Where a recipe lands

| Recipe | Lands on | What you see |
| --- | --- | --- |
| A crafting recipe | The Bench tab | Its ingredients fill the grid in the viewer's order. |
| Any other recipe | The Machine tab | One data cell for each input and each output. |

The Bench tab has a grid of nine cells, three by three. The ingredients fill it row by row, in the order the viewer shows them. Any ingredient beyond the ninth is left out.

The Machine tab takes the other recipes, such as the ones of J's Industrial machines. Each input and output becomes a data cell there: an item with its count, or a fluid in millibuckets (mB).

> [!DEFINE] Recipe viewer
> A screen in a mod that lists recipes: which ingredients make which result.

> [!DEFINE] Transfer button
> The button of a recipe viewer that sends a recipe's ingredients to the Pattern Studio.

> [!DEFINE] Ingredient list
> The side panel of a recipe viewer that lists the items you can use as ingredients.

> [!DEFINE] Millibucket (mB)
> The unit of volume for fluids on the network: one thousandth of a bucket.

## Before a transfer works

The Pattern Studio must be the front window of its desktop, or the transfer button refuses and the viewer shows an error.

You can also drag an item from the ingredient list onto a cell of the Studio. The cell takes that item, as if you had clicked it with the item on your cursor.

## EMI and Industrial

EMI lists the same recipes and can show one of its categories. A recipe does not move from EMI into the Pattern Studio, so use JEI when you want a transfer.

When J's Industrial is installed, its macerating and compressing recipes also appear in JEI and EMI, each in its own category.

## If something goes wrong

> [!PROBLEM] The transfer button shows "Open the Pattern Studio to transfer recipes".
> Open the Studio on the desktop, bring it to the front, and press the button again.

> [!PROBLEM] The recipe lands on the wrong cells.
> The Bench tab fills the grid in the viewer's order. Rearrange the cells in the Bench tab.

> [!PROBLEM] The Machine tab says "The machine draft needs an input and an output."
> A recipe with no items or fluids that could be mapped leaves the draft incomplete. Add the missing input or output in the Machine tab.

> [!PROBLEM] The Studio says "Not a machine recipe: <name>".
> Only recipes that machines make fit the Machine tab. Choose a recipe that a machine makes.

> [!SEE] jsc:pattern_studio, jsc:pattern_encoders
