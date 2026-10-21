/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.unify;

import com.google.gson.JsonElement;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.IConfigValues;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.stepaside.StepAside;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * One item for each material's copies made by different mods: for every common tag of a material's form held by more
 * than one item ({@code c:ingots/tin} with the Core's tin ingot and another mod's), the item of the first mod the
 * world's settings prefer, the Core's after the game's own by default, stands for the others. Each time the world's
 * data loads, the recipes' files are rewritten before the game reads them ({@link RecipeUnifier}), so every recipe
 * makes that item and asks for it; and what blocks, creatures and chests drop comes out as it too
 * ({@link UnifiedDrops}). Ores are never unified, since an ore differs by the rock it is in. It steps aside for Almost
 * Unified, which does the same.
 */
public final class Unification {

    private static final Logger LOGGER = CoreLogs.CONTENT.logger();
    /** The common tags' namespace. */
    private static final String COMMON = "c";
    /** A recipe left alone with all of its mod's, as {@code mymod:*}. */
    private static final String WHOLE_MOD = ":*";
    /** The system that steps aside for Almost Unified. */
    public static final StepAside STEP_ASIDE = StepAside.of("unification", List.of("almostunified"),
            UnificationSettings.FILE, UnificationSettings.STEP_ASIDE);

    /** Each item replaced in what drops, and the item that stands for it, as the world's data last loaded. */
    private static volatile Map<Item, Item> drops = Map.of();
    /** The plan the world's data last loaded with. */
    private static volatile UnifyPlan current = UnifyPlan.NONE;

    private Unification() {
    }

    /**
     * Rewrites {@code recipes}, every recipe file by its id, before the game reads them, with the tags as they are
     * loading ({@code tags}); and keeps the plan for what drops. Called as the recipes load.
     */
    @ApiStatus.Internal
    public static void unifyRecipes(final Map<ResourceLocation, JsonElement> recipes, final ICondition.IContext tags) {
        final IConfigValues settings = ConfigFiles.forData(UnificationSettings.FILE);
        if (!settings.get(UnificationSettings.ENABLED) || STEP_ASIDE.steps()) {
            keep(UnifyPlan.NONE, false);
            return;
        }
        final UnifyPlan plan = UnifyPlan.of(tagsOf(tags, settings), settings.get(UnificationSettings.PREFER),
                Set.copyOf(settings.get(UnificationSettings.KEPT_ITEMS)));
        keep(plan, settings.get(UnificationSettings.DROPS));
        if (plan.isEmpty()) {
            return;
        }
        final List<String> skipped = settings.get(UnificationSettings.SKIPPED_RECIPES);
        int changed = 0;
        for (final Map.Entry<ResourceLocation, JsonElement> recipe : recipes.entrySet()) {
            if (!skips(skipped, recipe.getKey()) && RecipeUnifier.rewrite(recipe.getValue(), plan.replace())) {
                changed++;
            }
        }
        LOGGER.info("Unification: {} items stand for {} others across {} tags; {} recipes changed",
                new HashSet<>(plan.replace().values()).size(), plan.replace().size(), plan.chosen().size(), changed);
    }

    /** The plan the world's data last loaded with: which item stands for which, and for which tags. */
    public static UnifyPlan current() {
        return current;
    }

    /** {@code stack} as the item that stands for it, as a drop comes out; the stack itself when none does. */
    public static ItemStack unified(final ItemStack stack) {
        final Item stands = drops.get(stack.getItem());
        return stands == null ? stack : stack.transmuteCopy(stands);
    }

    /** Forgets the plan, as a world stops. */
    @ApiStatus.Internal
    public static void forget() {
        keep(UnifyPlan.NONE, false);
    }

    private static void keep(final UnifyPlan plan, final boolean unifyDrops) {
        current = plan;
        if (!unifyDrops) {
            drops = Map.of();
            return;
        }
        final Map<Item, Item> items = new HashMap<>();
        plan.replace().forEach((from, to) -> {
            final Item fromItem = itemOf(from);
            final Item toItem = itemOf(to);
            if (fromItem != Items.AIR && toItem != Items.AIR) {
                items.put(fromItem, toItem);
            }
        });
        drops = Map.copyOf(items);
    }

    /* The tags unified, each with the ids of its items: the materials' forms', those added, less those left out. */
    private static Map<String, List<String>> tagsOf(final ICondition.IContext context, final IConfigValues settings) {
        final Set<String> folders = new HashSet<>();
        for (final Form form : Forms.all()) {
            if (form.shape() != Form.Shape.ORE) {
                folders.add(form.tagFolder());
            }
        }
        final Set<String> extra = Set.copyOf(settings.get(UnificationSettings.EXTRA_TAGS));
        final Set<String> skipped = Set.copyOf(settings.get(UnificationSettings.SKIPPED_TAGS));
        final Map<ResourceLocation, Collection<Holder<Item>>> all;
        try {
            all = context.getAllTags(Registries.ITEM);
        } catch (final IllegalStateException | UnsupportedOperationException notLoaded) {
            LOGGER.warn("Unification found no tags loaded and leaves the recipes as they are");
            return Map.of();
        }
        final Map<String, List<String>> out = new HashMap<>();
        all.forEach((id, holders) -> {
            final String written = id.toString();
            if (skipped.contains(written) || !(extra.contains(written) || isFormTag(id, folders))) {
                return;
            }
            final List<String> items = new ArrayList<>(holders.size());
            for (final Holder<Item> holder : holders) {
                holder.unwrapKey().ifPresent(key -> items.add(key.location().toString()));
            }
            out.put(written, items);
        });
        return out;
    }

    /* Whether {@code tag} is a common tag of a form in {@code folders}: c:<folder>/<material>. */
    private static boolean isFormTag(final ResourceLocation tag, final Set<String> folders) {
        if (!tag.getNamespace().equals(COMMON)) {
            return false;
        }
        final int slash = tag.getPath().indexOf('/');
        return slash > 0 && slash < tag.getPath().length() - 1 && tag.getPath().indexOf('/', slash + 1) < 0
                && folders.contains(tag.getPath().substring(0, slash));
    }

    /* Whether the recipe {@code id} is one the settings leave alone. */
    private static boolean skips(final List<String> skipped, final ResourceLocation id) {
        for (final String written : skipped) {
            if (written.endsWith(WHOLE_MOD) ? id.getNamespace().equals(written.substring(0,
                    written.length() - WHOLE_MOD.length())) : id.toString().equals(written)) {
                return true;
            }
        }
        return false;
    }

    private static Item itemOf(final String id) {
        final ResourceLocation parsed = ResourceLocation.tryParse(id);
        return parsed == null ? Items.AIR : BuiltInRegistries.ITEM.get(parsed);
    }
}
