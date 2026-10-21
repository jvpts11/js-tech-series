/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.recipe;

import dev.jstech.core.client.recipe.RecipeChooserClient;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.mixin.AbstractFurnaceMenuAccessor;
import dev.jstech.core.registry.CoreAttachments;
import dev.jstech.core.stepaside.StepAside;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Choosing between recipes that overlap, at the crafting grid and the furnace, in the spirit of Polymorph: when more
 * than one recipe fits what is in a grid, the player chooses which one makes the result, and the choice is kept with
 * the player for the same recipes, so the same grid gives the same result next time; a recipe placed from the recipe
 * book counts as chosen. A furnace keeps, for each item it smelts, the recipe it was set to, and smelts by it while it
 * fits. The player sees a chooser beside the result while more than one fits. It steps aside for Polymorph.
 */
public final class RecipeChoices {

    /** Whether choosing steps aside for Polymorph, as the world's settings say. */
    public static final StepAside STEP_ASIDE = StepAside.of("recipe_choice", List.of("polymorph"),
            RecipeChoiceSettings.FILE, RecipeChoiceSettings.STEP_ASIDE);

    private static final String VERSION = "1";
    /** How often an open furnace's input is looked at for recipes to choose between, in ticks. */
    private static final int FURNACE_LOOK = 10;
    /** How many choices a player keeps; the oldest goes when one more comes. */
    private static final int MOST_KEPT = 512;
    /** The window whose grid the game is working out on this thread, and who is at it. */
    private static final ThreadLocal<Grid> GRID = new ThreadLocal<>();
    /** What each player was last told they can choose between, by player. */
    private static final Map<UUID, Offer> OFFERS = new ConcurrentHashMap<>();

    private RecipeChoices() {
    }

    /** Hands the buses what this answers, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, RecipeChoices::onRegisterPayloads);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, RecipeChoices::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(PlayerContainerEvent.Close.class,
                event -> OFFERS.remove(event.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                event -> OFFERS.remove(event.getEntity().getUUID()));
    }

    /** Whether players choose between recipes in this world: on by its settings, and not stepped aside. */
    public static boolean active() {
        return ConfigFiles.forData(RecipeChoiceSettings.FILE).get(RecipeChoiceSettings.ENABLED) && !STEP_ASIDE.steps();
    }

    /** Runs {@code work}, the game working out the result of {@code menu}'s grid for {@code player}. */
    @ApiStatus.Internal
    public static void atGrid(final AbstractContainerMenu menu, final Player player, final Runnable work) {
        final Grid before = GRID.get();
        GRID.set(new Grid(menu, player));
        try {
            work.run();
        } finally {
            if (before == null) {
                GRID.remove();
            } else {
                GRID.set(before);
            }
        }
    }

    /**
     * The recipe that makes the result of the grid being worked out, {@code found} being the one the game found: the
     * one its player chose when more than one fits ({@code placed} is the one the recipe book placed, if it did), and
     * the player is told what fits.
     */
    @ApiStatus.Internal
    public static Optional<RecipeHolder<CraftingRecipe>> crafting(final RecipeManager manager,
                                                                  final CraftingInput input, final Level level,
                                                                  @Nullable final RecipeHolder<CraftingRecipe> placed,
                                                                  final Optional<RecipeHolder<CraftingRecipe>> found) {
        final Grid grid = GRID.get();
        if (grid == null || !(grid.player() instanceof ServerPlayer player)) {
            return found;
        }
        final int window = grid.menu().containerId;
        if (found.isEmpty() || !active()) {
            tellNone(player, window);
            return found;
        }
        final List<RecipeHolder<CraftingRecipe>> fits = manager.getRecipesFor(RecipeType.CRAFTING, input, level);
        if (fits.size() < 2) {
            tellNone(player, window);
            return found;
        }
        final List<String> ids = idsOf(fits);
        final String key = RecipeChoice.keyOf(ids);
        if (placed != null && ids.contains(placed.id().toString())) {
            keep(player, key, placed.id());
        }
        final ResourceLocation kept = player.getData(CoreAttachments.RECIPE_CHOICES).get(key);
        final int at = RecipeChoice.chosen(ids, kept == null ? null : kept.toString(), found.get().id().toString());
        final List<ItemStack> results = new ArrayList<>();
        for (final RecipeHolder<CraftingRecipe> recipe : fits) {
            results.add(recipe.value().assemble(input, level.registryAccess()));
        }
        tell(player, new Offer(window, holderIds(fits), key, null, null, ""), new RecipeChoicesPayload(window,
                holderIds(fits), results, at, false, ItemStack.EMPTY));
        return Optional.of(fits.get(at));
    }

    /**
     * The recipe {@code furnace} smelts {@code input} by, {@code found} being the one the game found: the one it was
     * set to for that item, while it still fits. Looked at every tick a furnace works, so it does nothing more than a
     * look up for a furnace never set.
     */
    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    public static <T extends Recipe<?>> Optional<RecipeHolder<T>> furnace(final AbstractFurnaceBlockEntity furnace,
                                                                         final SingleRecipeInput input,
                                                                         final Level level,
                                                                         final Optional<RecipeHolder<T>> found) {
        if (found.isEmpty() || !furnace.hasData(CoreAttachments.FURNACE_CHOICES)) {
            return found;
        }
        final ResourceLocation set = furnace.getData(CoreAttachments.FURNACE_CHOICES).get(BuiltInRegistries.ITEM
                .getKey(input.item().getItem()));
        if (set == null || set.equals(found.get().id()) || !active()) {
            return found;
        }
        final Optional<RecipeHolder<?>> chosen = level.getRecipeManager().byKey(set);
        if (chosen.isPresent() && chosen.get().value() instanceof AbstractCookingRecipe cooking
                && cooking.getType() == found.get().value().getType() && cooking.matches(input, level)) {
            return Optional.of((RecipeHolder<T>) chosen.get());
        }
        return found;
    }

    /**
     * Takes {@code player}'s choice of {@code recipe} in their open window {@code window}, when it is one of the
     * recipes they were told fit there: kept with them for a grid, whose result is worked out again, or with the
     * furnace for what it smelts. Whether it was taken.
     */
    public static boolean choose(final ServerPlayer player, final int window, final ResourceLocation recipe) {
        final Offer offer = OFFERS.get(player.getUUID());
        final AbstractContainerMenu menu = player.containerMenu;
        if (offer == null || offer.window() != window || menu.containerId != window
                || !offer.recipes().contains(recipe)) {
            return false;
        }
        if (offer.furnace() == null) {
            keep(player, offer.key(), recipe);
            final Container grid = gridOf(menu);
            if (grid != null) {
                menu.slotsChanged(grid);
            }
            return true;
        }
        final BlockEntity at = player.level().getBlockEntity(offer.furnace());
        if (!(at instanceof AbstractFurnaceBlockEntity furnace) || offer.input() == null) {
            return false;
        }
        final Map<ResourceLocation, ResourceLocation> set = new LinkedHashMap<>(furnace.getData(
                CoreAttachments.FURNACE_CHOICES));
        set.put(offer.input(), recipe);
        furnace.setData(CoreAttachments.FURNACE_CHOICES, Map.copyOf(set));
        furnace.setChanged();
        // Told again at the next look, with the choice in place.
        OFFERS.remove(player.getUUID());
        return true;
    }

    /** The recipes {@code player} was last told fit their open window, by id; none when one fits, or none. */
    public static List<ResourceLocation> offered(final ServerPlayer player) {
        final Offer offer = OFFERS.get(player.getUUID());
        return offer == null ? List.of() : offer.recipes();
    }

    /** Looks at the input of the furnace {@code player} has open, and tells them what fits it when that changes. */
    @SuppressWarnings("unchecked")
    public static void lookAtFurnace(final ServerPlayer player) {
        if (!(player.containerMenu instanceof AbstractFurnaceMenu menu)
                || !(((AbstractFurnaceMenuAccessor) menu).jscore$container() instanceof AbstractFurnaceBlockEntity
                furnace)) {
            return;
        }
        final int window = menu.containerId;
        final ItemStack in = menu.getSlot(AbstractFurnaceMenu.INGREDIENT_SLOT).getItem();
        final ResourceLocation item = BuiltInRegistries.ITEM.getKey(in.getItem());
        final ResourceLocation set = furnace.hasData(CoreAttachments.FURNACE_CHOICES)
                ? furnace.getData(CoreAttachments.FURNACE_CHOICES).get(item) : null;
        final String seen = item + " " + set;
        final Offer before = OFFERS.get(player.getUUID());
        if (before != null && before.window() == window && before.seen().equals(seen)) {
            return;
        }
        final Level level = player.level();
        final List<String> ids;
        final List<RecipeHolder<AbstractCookingRecipe>> fits;
        if (in.isEmpty() || !active()) {
            fits = List.of();
        } else {
            fits = fitting(level, ((AbstractFurnaceMenuAccessor) menu).jscore$recipeType(), new SingleRecipeInput(in));
        }
        if (fits.size() < 2) {
            OFFERS.put(player.getUUID(), new Offer(window, List.of(), "", null, null, seen));
            send(player, RecipeChoicesPayload.none(window));
            return;
        }
        ids = idsOf(fits);
        final Optional<RecipeHolder<AbstractCookingRecipe>> first = level.getRecipeManager().getRecipeFor(
                (RecipeType<AbstractCookingRecipe>) ((AbstractFurnaceMenuAccessor) menu).jscore$recipeType(),
                new SingleRecipeInput(in), level);
        final int at = RecipeChoice.chosen(ids, set == null ? null : set.toString(),
                first.map(recipe -> recipe.id().toString()).orElse(null));
        final List<ItemStack> results = new ArrayList<>();
        for (final RecipeHolder<AbstractCookingRecipe> recipe : fits) {
            results.add(recipe.value().getResultItem(level.registryAccess()).copy());
        }
        tell(player, new Offer(window, holderIds(fits), "", furnace.getBlockPos(), item, seen),
                new RecipeChoicesPayload(window, holderIds(fits), results, at, true, in.copyWithCount(1)));
    }

    private static void onRegisterPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(RecipeChoicesPayload.TYPE, RecipeChoicesPayload.STREAM_CODEC,
                RecipeChoices::onChoices);
        registrar.playToServer(RecipeChosenPayload.TYPE, RecipeChosenPayload.STREAM_CODEC, RecipeChoices::onChosen);
    }

    /* Runs on a client only: the payload is only ever sent to one. */
    private static void onChoices(final RecipeChoicesPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> RecipeChooserClient.told(payload));
    }

    private static void onChosen(final RecipeChosenPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                choose(player, payload.containerId(), payload.recipe());
            }
        });
    }

    private static void onPlayerTick(final PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % FURNACE_LOOK == 0
                && player.containerMenu instanceof AbstractFurnaceMenu) {
            lookAtFurnace(player);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<RecipeHolder<AbstractCookingRecipe>> fitting(final Level level,
                                                                     final RecipeType<?> type,
                                                                     final SingleRecipeInput input) {
        return level.getRecipeManager().getRecipesFor((RecipeType<AbstractCookingRecipe>) type, input, level);
    }

    /* Keeps {@code recipe} as {@code player}'s choice among the recipes kept under {@code key}. */
    private static void keep(final ServerPlayer player, final String key, final ResourceLocation recipe) {
        final Map<String, ResourceLocation> kept = new LinkedHashMap<>(player.getData(CoreAttachments.RECIPE_CHOICES));
        kept.remove(key);
        kept.put(key, recipe);
        while (kept.size() > MOST_KEPT) {
            kept.remove(kept.keySet().iterator().next());
        }
        player.setData(CoreAttachments.RECIPE_CHOICES, Map.copyOf(kept));
    }

    private static void tell(final ServerPlayer player, final Offer offer, final RecipeChoicesPayload payload) {
        OFFERS.put(player.getUUID(), offer);
        send(player, payload);
    }

    /* Tells {@code player} there is nothing to choose in {@code window}, unless they were told so already. */
    private static void tellNone(final ServerPlayer player, final int window) {
        final Offer before = OFFERS.get(player.getUUID());
        if (before != null && before.window() == window && before.recipes().isEmpty()) {
            return;
        }
        OFFERS.put(player.getUUID(), new Offer(window, List.of(), "", null, null, ""));
        send(player, RecipeChoicesPayload.none(window));
    }

    private static void send(final ServerPlayer player, final RecipeChoicesPayload payload) {
        if (!(player instanceof FakePlayer) && player.connection != null
                && player.connection.hasChannel(RecipeChoicesPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /* The container of {@code menu}'s crafting grid, which its result is worked out from. */
    private static @Nullable Container gridOf(final AbstractContainerMenu menu) {
        for (final Slot slot : menu.slots) {
            if (slot.container instanceof CraftingContainer grid) {
                return grid;
            }
        }
        return null;
    }

    private static <T extends Recipe<?>> List<String> idsOf(final List<RecipeHolder<T>> recipes) {
        final List<String> out = new ArrayList<>();
        recipes.forEach(recipe -> out.add(recipe.id().toString()));
        return out;
    }

    private static <T extends Recipe<?>> List<ResourceLocation> holderIds(final List<RecipeHolder<T>> recipes) {
        final List<ResourceLocation> out = new ArrayList<>();
        recipes.forEach(recipe -> out.add(recipe.id()));
        return out;
    }

    /* The grid being worked out on a thread, and its player. */
    private record Grid(AbstractContainerMenu menu, Player player) {
    }

    /*
     * What a player was told they can choose between: in which window, the recipes, what a grid's choice is kept
     * under, the furnace and the item it smelts for a furnace's, and what was seen of the furnace when it was told.
     */
    private record Offer(int window, List<ResourceLocation> recipes, String key, @Nullable BlockPos furnace,
                         @Nullable ResourceLocation input, String seen) {

        Offer {
            recipes = List.copyOf(recipes);
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(seen, "seen");
        }
    }
}
