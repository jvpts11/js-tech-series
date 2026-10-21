/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.developer.GeoBones;
import dev.jstech.core.developer.StudioTests;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.progression.IKnowledgeGated;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * What the studio reads once for every thing, each the first time a reader asks: the language files of each mod, the
 * recipes by what they make and by the research they need, the GameTests' names, the items of the creative tabs, and
 * the JSON of a file of the loaded resources. Made again on "Reload all".
 */
public final class StudioContext {

    private final ResourceManager resources;
    private final Map<String, Map<String, Map<String, String>>> languages = new HashMap<>();
    private final Map<ResourceLocation, Optional<JsonObject>> json = new HashMap<>();
    private final Map<String, Set<String>> bones = new HashMap<>();
    private Map<Item, List<RecipeHolder<?>>> making;
    private Map<ResourceLocation, List<RecipeHolder<?>>> needing;
    private @Nullable Set<Item> inTabs;
    private boolean tabsRead;
    private StudioTests.Index tests;

    private static final Logger LOGGER = CoreLogs.DEVELOPER.logger();
    private static final String LANG = "lang";
    private static final String JSON = ".json";
    private static final String GEO = "geo";
    private static final String GEO_JSON = ".geo.json";

    /** What the studio reads from {@code resources}, the player's game's own. */
    public StudioContext(final ResourceManager resources) {
        this.resources = resources;
    }

    /** The loaded resources. */
    public ResourceManager resources() {
        return this.resources;
    }

    /** Whether a file of that name is among the loaded resources. */
    public boolean exists(final ResourceLocation file) {
        return this.resources.getResource(file).isPresent();
    }

    /** The JSON object of a file of the loaded resources, the topmost pack's; none when there is none, or not one. */
    public Optional<JsonObject> json(final ResourceLocation file) {
        return this.json.computeIfAbsent(file, this::readJson);
    }

    /**
     * The languages {@code modid} names things in, each with the keys it names, by the language's code: every pack's
     * file of that language for the mod, merged.
     */
    public Map<String, Set<String>> languages(final String modid) {
        final Map<String, Set<String>> out = new TreeMap<>();
        this.texts(modid).forEach((code, texts) -> out.put(code, texts.keySet()));
        return out;
    }

    /** The same languages with each key's words, as the files write them. */
    public Map<String, Map<String, String>> texts(final String modid) {
        return this.languages.computeIfAbsent(modid, this::readLanguages);
    }

    /** The bones of every GeckoLib model of {@code modid}, in {@code assets/<mod>/geo/}. */
    public Set<String> geoBones(final String modid) {
        return this.bones.computeIfAbsent(modid, this::readBones);
    }

    /** The recipes that make {@code item}. */
    public List<RecipeHolder<?>> recipesMaking(final Item item) {
        if (this.making == null) {
            this.indexRecipes();
        }
        return this.making.getOrDefault(item, List.of());
    }

    /** The recipes that need the research {@code knowledge}. */
    public List<RecipeHolder<?>> recipesNeeding(final ResourceLocation knowledge) {
        if (this.needing == null) {
            this.indexRecipes();
        }
        return this.needing.getOrDefault(knowledge, List.of());
    }

    /** The names of every GameTest of this run, made ready to be asked which name a thing. */
    public StudioTests.Index tests() {
        if (this.tests == null) {
            this.tests = StudioTests.index(GameTestRegistry.getAllTestFunctions().stream()
                    .map(TestFunction::testName).toList());
        }
        return this.tests;
    }

    /** Whether {@code item} is shown in a creative tab; true when no world is open to build the tabs in. */
    public boolean inATab(final Item item) {
        if (!this.tabsRead) {
            this.inTabs = this.readTabs();
            this.tabsRead = true;
        }
        return this.inTabs == null || this.inTabs.contains(item);
    }

    private Optional<JsonObject> readJson(final ResourceLocation file) {
        final Optional<Resource> resource = this.resources.getResource(file);
        if (resource.isEmpty()) {
            return Optional.empty();
        }
        try (Reader reader = resource.get().openAsReader()) {
            final JsonElement read = JsonParser.parseReader(reader);
            return read.isJsonObject() ? Optional.of(read.getAsJsonObject()) : Optional.empty();
        } catch (final IOException | RuntimeException unread) {
            LOGGER.warn("The studio could not read {}: {}", file, unread.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Map<String, String>> readLanguages(final String modid) {
        final Map<String, Map<String, String>> out = new TreeMap<>();
        final Map<ResourceLocation, List<Resource>> files = this.resources.listResourceStacks(LANG,
                file -> file.getNamespace().equals(modid) && file.getPath().endsWith(JSON));
        files.forEach((file, stack) -> {
            final String path = file.getPath();
            final String code = path.substring(path.lastIndexOf('/') + 1, path.length() - JSON.length());
            final Map<String, String> texts = out.computeIfAbsent(code, any -> new HashMap<>());
            for (final Resource resource : stack) {
                try (Reader reader = resource.openAsReader()) {
                    final JsonElement read = JsonParser.parseReader(reader);
                    if (read.isJsonObject()) {
                        read.getAsJsonObject().entrySet().forEach(entry -> texts.put(entry.getKey(),
                                entry.getValue().isJsonPrimitive() ? entry.getValue().getAsString() : ""));
                    }
                } catch (final IOException | RuntimeException unread) {
                    LOGGER.warn("The studio could not read {}: {}", file, unread.getMessage());
                }
            }
        });
        return out;
    }

    private Set<String> readBones(final String modid) {
        final Set<String> out = new HashSet<>();
        this.resources.listResources(GEO, file -> file.getNamespace().equals(modid)
                && file.getPath().endsWith(GEO_JSON)).keySet()
                .forEach(file -> this.json(file).map(GeoBones::ofModel).ifPresent(out::addAll));
        return out;
    }

    private void indexRecipes() {
        this.making = new IdentityHashMap<>();
        this.needing = new HashMap<>();
        final Minecraft game = Minecraft.getInstance();
        if (game.level == null) {
            return;
        }
        for (final RecipeHolder<?> recipe : game.level.getRecipeManager().getRecipes()) {
            final ItemStack made = recipe.value().getResultItem(game.level.registryAccess());
            if (!made.isEmpty()) {
                this.making.computeIfAbsent(made.getItem(), any -> new ArrayList<>()).add(recipe);
            }
            if (recipe.value() instanceof IKnowledgeGated gated) {
                gated.knowledge().ifPresent(knowledge -> this.needing
                        .computeIfAbsent(knowledge, any -> new ArrayList<>()).add(recipe));
            }
        }
    }

    /* The tabs are built as the creative inventory builds them, for the player as they are. */
    private @Nullable Set<Item> readTabs() {
        final Minecraft game = Minecraft.getInstance();
        if (game.player == null || game.level == null) {
            return null;
        }
        CreativeModeTabs.tryRebuildTabContents(game.player.connection.enabledFeatures(),
                game.player.canUseGameMasterBlocks(), game.level.registryAccess());
        final Set<Item> items = Collections.newSetFromMap(new IdentityHashMap<>());
        for (final CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            tab.getDisplayItems().forEach(stack -> items.add(stack.getItem()));
        }
        return items;
    }
}
