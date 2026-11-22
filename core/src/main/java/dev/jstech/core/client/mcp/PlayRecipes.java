/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.mcp;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.jstech.core.client.recipeview.RecipeFaces;
import dev.jstech.core.client.recipeview.ViewKinds;
import dev.jstech.core.developer.mcp.McpCall;
import dev.jstech.core.developer.mcp.McpPermission;
import dev.jstech.core.developer.mcp.McpResult;
import dev.jstech.core.developer.mcp.McpSchema;
import dev.jstech.core.developer.mcp.McpTool;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.recipeview.RecipeView;
import dev.jstech.core.recipeview.RecipeViewKind;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The recipes the game holds, for an agent playing it: every page a recipe viewer shows of the kinds the mods on J's
 * Core declared (their machines' and the rest), each kind with the stations that work it, then every other recipe the
 * game holds, the crafting table's, the furnaces' and the stonecutter's among them. Each recipe says what it takes,
 * what it needs and does not use up, and what it makes, a slot as the items or fluids any one of which does. Read as
 * the player's own game holds them, so on a server too.
 */
final class PlayRecipes {

    private static final String MOD = "mod";
    private static final String KIND = "kind";
    private static final String MAKES = "makes";
    private static final String MOST = "most";
    private static final String ITEMS = "items";
    private static final String FLUIDS = "fluids";
    private static final String MATTER = "matter";
    private static final int MOST_RECIPES = 10_000;
    private static final int SOME_RECIPES = 100;

    private PlayRecipes() {
    }

    /** The tools. */
    static List<McpTool> all() {
        return List.of(new McpTool("recipes_read", "The recipes the game holds, as a recipe viewer shows them: each "
                + "with its id, its kind, what it takes, what it needs and does not use up, and what it makes, a slot "
                + "being the items, fluids or matter (a machine's gas, by its material) any one of which does; and "
                + "the kinds the mods on J's Core declared, each "
                + "with the stations that work it. Of a mod, of a kind, or making an item or a fluid. For following "
                + "what a thing is made of down to what the world gives.",
                McpSchema.object()
                        .words(MOD, "Only the recipes of this mod, by the namespace of their ids.", false)
                        .words(KIND, "Only the recipes of a kind whose id holds these words.", false)
                        .words(MAKES, "Only the recipes making this item or fluid, by its id.", false)
                        .whole(MOST, "At most this many recipes.", 1, MOST_RECIPES, false).build(),
                McpPermission.READ, PlayRecipes::read));
    }

    private static CompletableFuture<McpResult> read(final McpCall call) {
        final Asked asked = new Asked(lower(call, MOD), lower(call, KIND), lower(call, MAKES));
        final int most = (int) (long) call.whole(MOST).orElse((long) SOME_RECIPES);
        final Minecraft game = Minecraft.getInstance();
        return game.submit(() -> {
            final ClientLevel level = game.level;
            if (level == null) {
                return McpResult.failed(McpTexts.NO_WORLD.text());
            }
            final JsonArray kinds = new JsonArray();
            final JsonArray recipes = new JsonArray();
            for (final RecipeViewKind<?> kind : ViewKinds.all()) {
                viewed(kind, level, asked, kinds, recipes);
            }
            // The machines' recipes are the viewers' pages above; the rest are read as the game holds them.
            for (final RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
                if (!(holder.value() instanceof ProcessingRecipe) && !holder.value().isSpecial()) {
                    held(holder, level, asked, recipes);
                }
            }
            final JsonArray shown = new JsonArray();
            for (int i = 0; i < recipes.size() && i < most; i++) {
                shown.add(recipes.get(i));
            }
            final JsonObject out = new JsonObject();
            out.add("kinds", kinds);
            out.addProperty("found", recipes.size());
            out.add("recipes", shown);
            return McpResult.json(out);
        });
    }

    private static <T> void viewed(final RecipeViewKind<T> kind, final Level level, final Asked asked,
                                   final JsonArray kinds, final JsonArray recipes) {
        final String kindId = kind.id().toString();
        if (asked.kind().isPresent() && !kindId.contains(asked.kind().get())) {
            return;
        }
        final JsonObject about = new JsonObject();
        about.addProperty("id", kindId);
        final JsonArray stations = new JsonArray();
        kind.workstations().forEach(station -> stations.add(idOf(station)));
        about.add("stations", stations);
        kinds.add(about);
        for (final T recipe : kind.recipes(level)) {
            final RecipeView view = kind.view(recipe);
            final JsonObject one = recipe(kind.idOf(recipe), kindId);
            final JsonArray takes = slots(view, RecipeView.Role.INPUT);
            final JsonArray makes = slots(view, RecipeView.Role.OUTPUT);
            // A machine's gases and other matter are no slots: read from the recipe itself, or, on a page that is no
            // machine's recipe, from the face it is drawn on, by whether its tank takes or gives.
            if (recipe instanceof RecipeHolder<?> holder && holder.value() instanceof ProcessingRecipe processing) {
                processing.matterInputs().forEach(matter -> takes.add(matter(matter)));
                processing.matterOutputs().forEach(matter -> makes.add(matter(matter)));
            } else {
                view.face().ifPresent(face -> face.matter().forEach((tank, matter) -> (RecipeFaces.taking(view, tank)
                        .orElse(true) ? takes : makes).add(matter(matter))));
            }
            one.add("takes", takes);
            one.add("needs", slots(view, RecipeView.Role.CATALYST));
            one.add("makes", makes);
            if (asked.wants(one)) {
                recipes.add(one);
            }
        }
    }

    private static void held(final RecipeHolder<?> holder, final Level level, final Asked asked,
                             final JsonArray recipes) {
        final ResourceLocation type = BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType());
        final String kindId = type == null ? "" : type.toString();
        if (asked.kind().isPresent() && !kindId.contains(asked.kind().get())) {
            return;
        }
        final JsonObject one = recipe(holder.id(), kindId);
        final JsonArray takes = new JsonArray();
        for (final Ingredient ingredient : holder.value().getIngredients()) {
            if (!ingredient.isEmpty()) {
                takes.add(items(List.of(ingredient.getItems())));
            }
        }
        one.add("takes", takes);
        one.add("needs", new JsonArray());
        final JsonArray makes = new JsonArray();
        final ItemStack made = holder.value().getResultItem(level.registryAccess());
        if (!made.isEmpty()) {
            makes.add(items(List.of(made)));
        }
        one.add("makes", makes);
        if (asked.wants(one)) {
            recipes.add(one);
        }
    }

    private static JsonObject recipe(final @Nullable ResourceLocation id, final String kind) {
        final JsonObject one = new JsonObject();
        if (id != null) {
            one.addProperty("id", id.toString());
        }
        one.addProperty("kind", kind);
        return one;
    }

    private static JsonArray slots(final RecipeView view, final RecipeView.Role role) {
        final JsonArray out = new JsonArray();
        for (final RecipeView.Slot slot : view.slots(role)) {
            final JsonObject one;
            if (slot.holdsFluid()) {
                one = new JsonObject();
                final JsonArray fluids = new JsonArray();
                slot.fluids().forEach(fluid -> fluids.add(BuiltInRegistries.FLUID.getKey(fluid.getFluid())
                        .toString()));
                one.add(FLUIDS, fluids);
                one.addProperty("amount", slot.fluids().isEmpty() ? 0 : slot.fluids().getFirst().getAmount());
            } else {
                one = items(slot.items());
            }
            if (slot.chance() < 1) {
                one.addProperty("chance", slot.chance());
            }
            out.add(one);
        }
        return out;
    }

    /* A machine's matter, a gas or another state, by its material's id, in millibuckets at the open air's pressure. */
    private static JsonObject matter(final MatterStack stack) {
        final JsonObject one = new JsonObject();
        final JsonArray matter = new JsonArray();
        matter.add(stack.material().id().toString());
        one.add(MATTER, matter);
        one.addProperty("amount", stack.amount());
        return one;
    }

    /* A slot of items any one of which does, by their ids, with how many the first is. */
    private static JsonObject items(final List<ItemStack> stacks) {
        final JsonObject one = new JsonObject();
        final JsonArray ids = new JsonArray();
        stacks.stream().map(PlayRecipes::idOf).distinct().forEach(ids::add);
        one.add(ITEMS, ids);
        one.addProperty("count", stacks.isEmpty() ? 0 : stacks.getFirst().getCount());
        return one;
    }

    private static String idOf(final ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static Optional<String> lower(final McpCall call, final String name) {
        return call.text(name).map(words -> words.toLowerCase(Locale.ROOT));
    }

    /* What the agent asked for: a mod, a kind and a thing made, each when given. */
    private record Asked(Optional<String> mod, Optional<String> kind, Optional<String> makes) {

        /* Whether {@code recipe}, its kind already asked about, is one the agent wants. */
        boolean wants(final JsonObject recipe) {
            final String id = recipe.has("id") ? recipe.get("id").getAsString() : recipe.get("kind").getAsString();
            if (this.mod.isPresent() && !id.startsWith(this.mod.get() + ":")) {
                return false;
            }
            return this.makes.isEmpty() || recipe.getAsJsonArray("makes").asList().stream()
                    .map(JsonElement::getAsJsonObject).anyMatch(slot -> holds(slot, ITEMS) || holds(slot, FLUIDS)
                            || holds(slot, MATTER));
        }

        private boolean holds(final JsonObject slot, final String what) {
            return slot.has(what) && slot.getAsJsonArray(what).asList().stream()
                    .anyMatch(id -> id.getAsString().equals(this.makes.get()));
        }
    }
}
