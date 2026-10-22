/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.TextArea;
import dev.jstech.core.developer.DevSources;
import dev.jstech.core.developer.studio.LogRing;
import dev.jstech.core.developer.studio.StudioLogs;
import dev.jstech.core.developer.studio.RecipeConflicts;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.Nullable;

/**
 * The recipe bench: the recipes that make the thing chosen, and a new one; the file of the one chosen, written in its
 * place and tried as a recipe, with the preview the recipe viewer would show; and against the others, the recipes that
 * take the same inputs, those that did not load, and what the pack changed or added.
 */
final class RecipesPage extends StudioPage {

    private final TextArea file = this.panel.add(new TextArea());
    private final Button tryIt;
    private final Button save;
    private final Button unifying;
    private final UnificationView unification;
    private final List<RecipeHolder<?>> making = new ArrayList<>();
    private final Map<String, String> packs = new HashMap<>();
    private final List<Against> against = new ArrayList<>();
    private List<List<String>> conflicts = List.of();
    private @Nullable StudioThing thing;
    private int chosen;
    private String tried = "";
    private boolean right;
    private @Nullable Recipe<?> preview;
    private @Nullable CompoundTag changes;

    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final int ROW = 22;
    private static final int SLOT = 18;
    private static final int AGAINST_ROW = 26;
    /* The room at the list's foot for the button to unification. */
    private static final int UNIFY_ROOM = 22;
    private static final String MOD_PACK = "mod/";
    private static final String MOD_DATA = "mod_data";
    private static final String VANILLA = "vanilla";
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    /** The log the game writes a recipe that did not load to. */
    private static final String RECIPE_LOG = "net.minecraft.world.item.crafting.RecipeManager";

    RecipesPage(final StudioScreen studio) {
        super(studio);
        this.tryIt = this.panel.add(new Button(() -> words(ToolTexts.TRY), this::tryIt));
        this.save = this.panel.add(new Button(() -> words(ToolTexts.SAVE), this::save));
        this.unification = new UnificationView(studio, this.panel);
        this.unifying = this.panel.add(new Button(() -> words(UnificationTexts.OPEN), this.unification::open));
    }

    @Override
    TextKey tab() {
        return StudioTool.RECIPES.title();
    }

    @Override
    List<String> crumbs() {
        if (this.unification.isOpen()) {
            return List.of(words(this.tab()), this.unification.crumb());
        }
        return this.thing == null ? List.of(words(this.tab())) : List.of(words(this.tab()), this.thing.name()
                .getString());
    }

    @Override
    List<String> lines() {
        if (this.unification.isOpen()) {
            return this.unification.lines();
        }
        return this.making.stream().map(recipe -> recipe.id().toString()).toList();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final List<StudioLayout.Rect> columns = this.columns();
        final StudioLayout.Rect list = columns.get(0);
        final StudioLayout.Rect middle = columns.get(1);
        place(this.file, new StudioLayout.Rect(middle.x() + 4, middle.y() + 18, middle.width() - 8, 104));
        place(this.tryIt, new StudioLayout.Rect(middle.x() + 4, middle.y() + 126, 60, 16));
        place(this.save, new StudioLayout.Rect(middle.x() + 68, middle.y() + 126, 60, 16));
        place(this.unifying, new StudioLayout.Rect(list.x() + 4, list.bottom() - UNIFY_ROOM, list.width() - 8, 16));
        this.unification.layout(laid.body(), font());
    }

    @Override
    void shown() {
        this.unification.close();
        this.thing = this.studio.thing().orElse(null);
        this.making.clear();
        this.packs.clear();
        this.changes = null;
        this.chosen = 0;
        final Minecraft game = Minecraft.getInstance();
        if (game.level == null) {
            return;
        }
        final List<RecipeConflicts.Inputs> inputs = new ArrayList<>();
        for (final RecipeHolder<?> recipe : game.level.getRecipeManager().getRecipes()) {
            inputs.add(inputsOf(recipe));
            if (this.thing != null && this.thing.item() != null
                    && recipe.value().getResultItem(game.level.registryAccess()).is(this.thing.item())) {
                this.making.add(recipe);
            }
        }
        this.conflicts = RecipeConflicts.of(inputs);
        this.choose(0);
        this.askSources();
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final boolean benching = !this.unification.isOpen();
        this.tryIt.setVisible(benching && this.thing != null);
        this.save.setVisible(benching && this.thing != null);
        this.file.setVisible(benching && this.thing != null);
        this.unifying.setVisible(benching);
        this.unification.render(g, font(), this.regions.body());
        if (!benching) {
            return;
        }
        final StudioPalette.Colours colours = StudioPalette.get();
        final List<StudioLayout.Rect> columns = this.columns();
        final StudioLayout.Rect list = columns.get(0);
        StudioDraw.panel(g, font(), list, this.thing == null ? words(this.tab())
                : GameText.resolve(ToolTexts.MAKE.with(this.thing.name().getString())));
        if (this.thing == null) {
            StudioDraw.small(g, font(), words(ToolTexts.CHOOSE_FIRST), list.x() + StudioDraw.INSET,
                    list.y() + StudioDraw.HEAD + 4, list.width() - 2 * StudioDraw.INSET, colours.dim());
        }
        for (int i = 0; i <= this.making.size() && this.thing != null; i++) {
            final int y = list.y() + StudioDraw.HEAD + 4 + i * ROW;
            if (y + ROW > list.bottom() - UNIFY_ROOM) {
                break;
            }
            final boolean on = i == this.chosen;
            if (on) {
                g.fill(list.x() + 3, y - 2, list.right() - 3, y + ROW - 4, colours.selected());
                Draw.outline(g, list.x() + 3, y - 2, list.width() - 6, ROW - 2, colours.outline());
            }
            final String name = i < this.making.size() ? this.making.get(i).id().getPath() : words(
                    ToolTexts.NEW_RECIPE);
            final String under = i < this.making.size() ? this.making.get(i).id().getNamespace() : words(
                    ToolTexts.WRITTEN_HERE);
            StudioDraw.text(g, font(), name, list.x() + 8, y, list.width() - 12, on ? colours.text()
                    : colours.label());
            StudioDraw.small(g, font(), under, list.x() + 8, y + 9, list.width() - 12, colours.dim());
        }
        final StudioLayout.Rect middle = columns.get(1);
        StudioDraw.panel(g, font(), middle, this.fileName());
        StudioDraw.small(g, font(), this.tried, this.save.x() + this.save.width() + 6, this.save.y() + 5,
                middle.right() - this.save.x() - this.save.width() - 10, this.right ? colours.fine() : colours.error());
        this.preview(g, middle, colours);
        this.against(g, columns.get(2), colours);
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (this.unification.isOpen()) {
            return this.unification.mouseClicked(mouseX, mouseY, this.regions.body());
        }
        final StudioLayout.Rect list = this.columns().get(0);
        if (!list.contains(mouseX, mouseY) || this.thing == null) {
            return false;
        }
        final int row = (int) ((mouseY - list.y() - StudioDraw.HEAD - 4 + 2) / ROW);
        if (row >= 0 && row <= this.making.size()) {
            this.choose(row);
        }
        return true;
    }

    @Override
    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
        return this.unification.mouseScrolled(mouseX, mouseY, scrollY, this.regions.body());
    }

    private List<StudioLayout.Rect> columns() {
        return StudioLayout.columns(this.regions.body(), 0.23, 0.46);
    }

    private void choose(final int row) {
        this.chosen = row;
        this.tried = "";
        this.preview = null;
        if (this.thing == null) {
            this.file.setText("");
            return;
        }
        if (row < this.making.size()) {
            final RecipeHolder<?> recipe = this.making.get(row);
            this.file.setText(this.writtenFile(recipe.id()).orElseGet(() -> encoded(recipe.value())));
            this.preview = recipe.value();
        } else {
            this.file.setText(PRETTY.toJson(JsonParser.parseString("{\"type\":\"minecraft:crafting_shapeless\","
                    + "\"ingredients\":[],\"result\":{\"id\":\"" + BuiltInRegistries.ITEM.getKey(this.thing.item())
                    + "\",\"count\":1}}")));
        }
        this.against.clear();
        this.readAgainst();
    }

    /* The recipe's file as its mod's source writes it, when this run has the source. */
    private Optional<String> writtenFile(final ResourceLocation id) {
        final Optional<Path> source = StudioFiles.sourceOf(StudioFiles.data(recipeFile(id)));
        if (source.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readString(source.get(), StandardCharsets.UTF_8));
        } catch (final IOException unread) {
            return Optional.empty();
        }
    }

    private void tryIt() {
        final Minecraft game = Minecraft.getInstance();
        if (game.level == null) {
            return;
        }
        try {
            final JsonElement json = JsonParser.parseString(this.file.text());
            final DataResult<Recipe<?>> read = Recipe.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE,
                    game.level.registryAccess()), json);
            this.right = read.result().isPresent();
            this.preview = read.result().orElse(null);
            this.tried = this.right ? GameText.resolve(ToolTexts.RIGHT.with(BuiltInRegistries.RECIPE_TYPE.getKey(
                    this.preview.getType()))) : GameText.resolve(ToolTexts.WRONG.with(read.error().map(
                    DataResult.Error::message).orElse("")));
        } catch (final RuntimeException unparsed) {
            this.right = false;
            this.tried = GameText.resolve(ToolTexts.WRONG.with(String.valueOf(unparsed.getMessage())));
        }
    }

    /* Writes the file in its place in the mod's source, the new recipe under the thing's own name. */
    private void save() {
        if (this.thing == null) {
            return;
        }
        final ResourceLocation id = this.chosen < this.making.size() ? this.making.get(this.chosen).id()
                : ResourceLocation.fromNamespaceAndPath(this.thing.id().getNamespace(), this.thing.id().getPath());
        final ResourceLocation path = recipeFile(id);
        final Optional<Path> source = DevSources.resourcesOf(id.getNamespace());
        if (source.isEmpty()) {
            this.studio.say(words(ToolTexts.NO_SOURCE));
            return;
        }
        final Path written = StudioFiles.sourceOf(StudioFiles.data(path)).orElse(source.get().resolve(
                StudioFiles.data(path).path()));
        try {
            Files.createDirectories(written.getParent());
            Files.writeString(written, this.file.text(), StandardCharsets.UTF_8);
            this.studio.say(GameText.resolve(ToolTexts.SAVED_TO.with(written.toString())));
        } catch (final IOException unwritten) {
            this.studio.say(String.valueOf(unwritten.getMessage()));
        }
    }

    private String fileName() {
        if (this.thing == null) {
            return "";
        }
        final ResourceLocation id = this.chosen < this.making.size() ? this.making.get(this.chosen).id()
                : this.thing.id();
        return recipeFile(id).getPath().substring(recipeFile(id).getPath().indexOf('/') + 1);
    }

    /* The recipe as the recipe viewer shows it: its inputs, an arrow, and what it makes. */
    private void preview(final GuiGraphics g, final StudioLayout.Rect middle, final StudioPalette.Colours colours) {
        final Minecraft game = Minecraft.getInstance();
        final StudioLayout.Rect area = new StudioLayout.Rect(middle.x() + 4, middle.y() + 148, middle.width() - 8,
                Math.max(40, middle.bottom() - middle.y() - 152));
        g.fill(area.x(), area.y(), area.right(), area.bottom(), colours.viewer());
        StudioDraw.small(g, font(), words(ToolTexts.VIEWER), area.x() + 6, area.y() + 4, area.width() - 12,
                colours.dim());
        if (this.preview == null || game.level == null) {
            return;
        }
        final List<Ingredient> inputs = this.preview.getIngredients();
        int x = area.x() + 16;
        final int y = area.y() + 24;
        for (final Ingredient input : inputs.subList(0, Math.min(9, inputs.size()))) {
            g.blitSprite(SLOT_SPRITE, x, y, SLOT, SLOT);
            final ItemStack[] shown = input.getItems();
            if (shown.length > 0) {
                g.renderItem(shown[0], x + 1, y + 1);
            }
            x += SLOT;
        }
        x += 10;
        g.fill(x, y + 7, x + 16, y + 11, colours.arrow());
        for (int i = 0; i < 5; i++) {
            g.fill(x + 16 + i, y + 4 + i, x + 17 + i, y + 14 - i, colours.arrow());
        }
        x += 30;
        final ItemStack made = this.preview.getResultItem(game.level.registryAccess());
        g.blitSprite(SLOT_SPRITE, x, y, SLOT, SLOT);
        g.renderItem(made, x + 1, y + 1);
        g.renderItemDecorations(font(), made, x + 1, y + 1);
        StudioDraw.text(g, font(), made.getHoverName().getString(), x + SLOT + 4, y + 5, area.right() - x - SLOT - 8,
                colours.label());
        StudioDraw.small(g, font(), String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(this.preview.getType())),
                area.x() + 16, y + SLOT + 6, area.width() - 32, colours.dim());
    }

    private void against(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        StudioDraw.panel(g, font(), area, words(ToolTexts.AGAINST));
        if (this.thing == null) {
            return;
        }
        if (this.against.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NOTHING_AGAINST), area.x() + StudioDraw.INSET,
                    area.y() + StudioDraw.HEAD + 4, area.width() - 2 * StudioDraw.INSET, colours.dim());
        }
        for (int i = 0; i < this.against.size(); i++) {
            final int y = area.y() + StudioDraw.HEAD + 4 + i * AGAINST_ROW;
            if (y + AGAINST_ROW > area.bottom()) {
                break;
            }
            final Against one = this.against.get(i);
            StudioMarks.draw(g, font(), area.x() + StudioDraw.INSET, y + 1, one.mark());
            final int x = area.x() + StudioDraw.INSET + StudioMarks.WIDTH + 5;
            StudioDraw.text(g, font(), one.title(), x, y, area.right() - x - StudioDraw.INSET, colours.text());
            StudioDraw.small(g, font(), one.detail(), x, y + 10, area.right() - x - StudioDraw.INSET, colours.dim());
        }
    }

    /* What stands against the recipe chosen: the same inputs, the recipes that did not load, the pack's changes. */
    private void readAgainst() {
        this.against.clear();
        if (this.chosen < this.making.size()) {
            final String id = this.making.get(this.chosen).id().toString();
            for (final List<String> group : this.conflicts) {
                if (group.contains(id)) {
                    group.stream().filter(other -> !other.equals(id)).forEach(other -> this.against.add(new Against(
                            StudioSheet.Mark.ERROR, words(ToolTexts.SAME_INPUTS),
                            GameText.resolve(ToolTexts.SAME_INPUTS_DETAIL.with(other)))));
                }
            }
        }
        for (final LogRing.Line line : StudioLogs.lines(RECIPE_LOG)) {
            this.against.add(new Against(StudioSheet.Mark.WARNING, words(ToolTexts.DID_NOT_LOAD), line.message()));
        }
        if (this.changes != null) {
            for (final RecipeHolder<?> recipe : this.making) {
                final String id = recipe.id().toString();
                if (listed(this.changes.getList(StudioKeys.REPLACED, Tag.TAG_STRING), id)) {
                    this.against.add(new Against(StudioSheet.Mark.WARNING, words(ToolTexts.CHANGED_BY_PACK),
                            GameText.resolve(ToolTexts.PACK_REPLACED.with(id))));
                }
                if (listed(this.changes.getList(StudioKeys.SWAPPED, Tag.TAG_STRING), id)) {
                    this.against.add(new Against(StudioSheet.Mark.WARNING, words(ToolTexts.CHANGED_BY_PACK),
                            GameText.resolve(ToolTexts.PACK_SWAPPED.with(id))));
                }
                final String pack = this.packs.getOrDefault(id, "");
                if (!pack.isEmpty() && !pack.startsWith(MOD_PACK) && !pack.equals(MOD_DATA) && !pack.equals(VANILLA)) {
                    this.against.add(new Against(StudioSheet.Mark.FINE, words(ToolTexts.ADDED_BY_PACK),
                            GameText.resolve(ToolTexts.FROM_PACK.with(id, pack))));
                }
            }
            final ListTag removed = this.changes.getList(StudioKeys.REMOVED, Tag.TAG_STRING);
            for (int i = 0; i < removed.size(); i++) {
                if (this.thing != null && removed.getString(i).contains(this.thing.id().getPath())) {
                    this.against.add(new Against(StudioSheet.Mark.WARNING, words(ToolTexts.CHANGED_BY_PACK),
                            GameText.resolve(ToolTexts.PACK_REMOVED.with(removed.getString(i)))));
                }
            }
        }
    }

    private void askSources() {
        if (this.making.isEmpty() || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        final CompoundTag args = new CompoundTag();
        final ListTag ids = new ListTag();
        this.making.forEach(recipe -> ids.add(StringTag.valueOf(recipe.id().toString())));
        args.put(StudioKeys.RECIPES, ids);
        StudioAnswers.ask(StudioServices.SOURCES, args, answered -> {
            if (answered.contains(StudioServices.REFUSED)) {
                return;
            }
            final ListTag recipes = answered.getList(StudioKeys.RECIPES, Tag.TAG_COMPOUND);
            for (int i = 0; i < recipes.size(); i++) {
                this.packs.put(recipes.getCompound(i).getString(StudioKeys.ID), recipes.getCompound(i).getString(
                        StudioKeys.PACK));
            }
            this.changes = answered;
            this.readAgainst();
        });
    }

    private static boolean listed(final ListTag ids, final String id) {
        for (int i = 0; i < ids.size(); i++) {
            if (ids.getString(i).equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String encoded(final Recipe<?> recipe) {
        final Minecraft game = Minecraft.getInstance();
        if (game.level == null) {
            return "";
        }
        return Recipe.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, game.level.registryAccess()), recipe)
                .result().map(PRETTY::toJson).orElse("");
    }

    private static ResourceLocation recipeFile(final ResourceLocation id) {
        return ResourceLocation.fromNamespaceAndPath(id.getNamespace(), "recipe/" + id.getPath() + ".json");
    }

    /* What a recipe takes, as the conflicts are found among: its kind, its width and each place's items. */
    private static RecipeConflicts.Inputs inputsOf(final RecipeHolder<?> recipe) {
        final List<List<String>> places = new ArrayList<>();
        for (final Ingredient ingredient : recipe.value().getIngredients()) {
            places.add(Arrays.stream(ingredient.getItems()).map(stack -> BuiltInRegistries.ITEM.getKey(
                    stack.getItem()).toString()).distinct().toList());
        }
        final int width = recipe.value() instanceof ShapedRecipe shaped ? shaped.getWidth() : 0;
        return new RecipeConflicts.Inputs(recipe.id().toString(), String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(
                recipe.value().getType())), width, places);
    }

    /** One thing against the recipe chosen. */
    private record Against(StudioSheet.Mark mark, String title, String detail) {
    }
}
