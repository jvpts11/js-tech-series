/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import com.mojang.serialization.Codec;
import dev.jstech.core.audio.SoundCue;
import dev.jstech.core.audio.SoundKey;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.energy.IEnergyHolder;
import dev.jstech.core.font.CellFont;
import dev.jstech.core.guide.ModGuide;
import dev.jstech.core.item.ItemStates;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * What one mod puts in the game, declared in one place: its blocks, its items and the components they carry, the
 * block entities its blocks make, its creative tabs, its sounds and fonts, and the registries datapacks fill.
 *
 * <p>A block or an item is declared once, with everything about it, and nothing else lists it again: the
 * generator writes its block state, models, name, loot and tags from the declaration, the tab shows it from the
 * declaration, and the checks read the declarations to prove that every registered thing has all of those. Each mod
 * keeps one of these, made before any declaration and handed its event bus by {@link #register(IEventBus)}.
 */
public final class ModContent {

    private final String modid;
    private final DeferredRegister.Blocks blocks;
    private final DeferredRegister.Items items;
    private final DeferredRegister<BlockEntityType<?>> blockEntities;
    private final DeferredRegister<CreativeModeTab> tabs;
    private final DeferredRegister<SoundEvent> sounds;
    private final List<BlockEntry<?>> declaredBlocks = new ArrayList<>();
    private final List<ItemEntry<?>> declaredItems = new ArrayList<>();
    private final List<ContentTab> declaredTabs = new ArrayList<>();
    private final List<SoundKey> declaredSounds = new ArrayList<>();
    private final List<SoundCue> declaredCues = new ArrayList<>();
    private final List<DeferredHolder<BlockEntityType<?>, ? extends BlockEntityType<?>>> declaredBlockEntities =
            new ArrayList<>();
    private final List<CableEntry> declaredCables = new ArrayList<>();
    private final List<FluidEntry> declaredFluids = new ArrayList<>();
    private final List<CellFont> declaredFonts = new ArrayList<>();
    private final List<Consumer<DataPackRegistryEvent.NewRegistry>> datapackRegistries = new ArrayList<>();
    private @Nullable DeferredRegister<CableType> cables;
    private DeferredRegister.@Nullable DataComponents components;
    private @Nullable DeferredRegister<FluidType> fluidTypes;
    private @Nullable DeferredRegister<Fluid> fluids;
    private @Nullable DeferredRegister<RecipeType<?>> recipeTypes;
    private @Nullable DeferredRegister<RecipeSerializer<?>> recipeSerializers;
    private final List<ProcessingKind> declaredProcessing = new ArrayList<>();
    private @Nullable DeferredRegister<EntityType<?>> entityTypes;
    private final List<EntityEntry<?>> declaredEntities = new ArrayList<>();
    private @Nullable ModGuide guide;

    /* Every mod's content, by mod id, in the order the mods made theirs. */
    private static final Map<String, ModContent> BY_MOD = Collections.synchronizedMap(new LinkedHashMap<>());

    public ModContent(final String modid) {
        this.modid = modid;
        this.blocks = DeferredRegister.createBlocks(modid);
        this.items = DeferredRegister.createItems(modid);
        this.blockEntities = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, modid);
        this.tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modid);
        this.sounds = DeferredRegister.create(Registries.SOUND_EVENT, modid);
        if (BY_MOD.putIfAbsent(modid, this) != null) {
            throw new IllegalStateException("the content of " + modid + " is declared twice");
        }
    }

    /** Every mod's content. */
    public static Collection<ModContent> all() {
        synchronized (BY_MOD) {
            return List.copyOf(BY_MOD.values());
        }
    }

    /**
     * That mod's content.
     *
     * @throws IllegalStateException when the mod declared none
     */
    public static ModContent of(final String modid) {
        final ModContent content = BY_MOD.get(modid);
        if (content == null) {
            throw new IllegalStateException(modid + " declared no content");
        }
        return content;
    }

    public String modid() {
        return modid;
    }

    /**
     * What this mod writes in the manuals: its chapter, sections and entries, and any manual or style of its own,
     * which the data generation writes out with the rest of its content.
     */
    public ModGuide guide() {
        if (this.guide == null) {
            this.guide = new ModGuide(this.modid);
        }
        return this.guide;
    }

    /** What this mod declared for the manuals, when it declared anything. */
    public Optional<ModGuide> declaredGuide() {
        return this.guide == null || this.guide.isEmpty() ? Optional.empty() : Optional.of(this.guide);
    }

    /** Starts declaring a block made from its properties. */
    public <B extends Block> BlockBuilder<B> block(final String id,
                                                  final Function<BlockBehaviour.Properties, ? extends B> factory) {
        return new BlockBuilder<>(this, id, factory);
    }

    /** Starts declaring an item that is not a block's, made from its properties. */
    public <I extends Item> ItemBuilder<I> item(final String id, final Function<Item.Properties, ? extends I> factory) {
        return new ItemBuilder<>(this, id, factory);
    }

    /** Starts declaring a fluid: its type, its still and flowing fluid, and for a liquid its block and bucket. */
    public FluidBuilder fluid(final String id) {
        return new FluidBuilder(this, id);
    }

    /** Starts declaring a cable laid in the Core's cable block, and the item that lays it, under one id. */
    public CableBuilder cable(final String id, final CableType.Builder type) {
        return new CableBuilder(this, id, type);
    }

    /**
     * Registers the block entity made by those blocks. Every block that makes it is named here, so a variant of a
     * block (another era of the same machine) cannot be left out of its entity's valid blocks.
     */
    public <T extends BlockEntity> DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> blockEntity(
            final String id, final BlockEntityType.BlockEntitySupplier<? extends T> factory,
            final DeferredBlock<?>... madeBy) {
        final List<DeferredBlock<?>> makers = List.of(madeBy);
        final DeferredHolder<BlockEntityType<?>, BlockEntityType<T>> type = blockEntities.register(id,
                () -> BlockEntityType.Builder.<T>of(factory,
                        makers.stream().map(DeferredBlock::get).toArray(Block[]::new)).build(null));
        declaredBlockEntities.add(type);
        return type;
    }

    /** The block entity types this mod declared, once they are registered. */
    public List<BlockEntityType<?>> declaredBlockEntityTypes() {
        return declaredBlockEntities.stream().<BlockEntityType<?>>map(DeferredHolder::get).toList();
    }

    /**
     * Declares a creative tab, called that in English, showing that icon. A mod's tabs stand side by side in the
     * order it declares them.
     */
    public ContentTab tab(final String id, final String englishTitle, final Supplier<? extends ItemLike> icon) {
        final ContentTab tab = new ContentTab(modid, id, englishTitle, icon,
                declaredTabs.isEmpty() ? null : declaredTabs.getLast());
        tabs.register(id, tab::build);
        declaredTabs.add(tab);
        return tab;
    }

    /** Starts declaring a sound, known by that path under the mod's namespace ({@code computer/power_on}). */
    public SoundBuilder sound(final String path) {
        return new SoundBuilder(this, path);
    }

    /** Starts declaring a cue, known by that path under the mod's namespace ({@code computer/boot}). */
    public CueBuilder cue(final String path) {
        return new CueBuilder(this, path);
    }

    /** The blocks declared so far, in declaration order. */
    public List<BlockEntry<?>> declaredBlocks() {
        return Collections.unmodifiableList(declaredBlocks);
    }

    /** The items declared so far that are not blocks', in declaration order. */
    public List<ItemEntry<?>> declaredItems() {
        return Collections.unmodifiableList(declaredItems);
    }

    public List<ContentTab> declaredTabs() {
        return Collections.unmodifiableList(declaredTabs);
    }

    /** The sounds declared so far, in declaration order. */
    public List<SoundKey> declaredSounds() {
        return Collections.unmodifiableList(declaredSounds);
    }

    /** The cues declared so far, in declaration order. */
    public List<SoundCue> declaredCues() {
        return Collections.unmodifiableList(declaredCues);
    }

    /** Starts declaring a font, known by that path under the mod's namespace ({@code terminal}). */
    public FontBuilder font(final String path) {
        return new FontBuilder(this, path);
    }

    /** The fonts declared so far, in declaration order. */
    public List<CellFont> declaredFonts() {
        return Collections.unmodifiableList(declaredFonts);
    }

    /** The cables declared so far, in declaration order. */
    public List<CableEntry> declaredCables() {
        return Collections.unmodifiableList(declaredCables);
    }

    /** The fluids declared so far, in declaration order. */
    public List<FluidEntry> declaredFluids() {
        return Collections.unmodifiableList(declaredFluids);
    }

    /**
     * Declares a kind of processing machine: a recipe type of this mod, named {@code id}, whose recipes are
     * {@link ProcessingRecipe}s read and sent by a serializer of the same name, called that in English and worked by
     * that machine, which the recipe viewers show beside its recipes.
     */
    public ProcessingKind processing(final String id, final String englishName,
                                     final Supplier<? extends ItemLike> machine) {
        if (recipeTypes == null) {
            recipeTypes = DeferredRegister.create(Registries.RECIPE_TYPE, modid);
            recipeSerializers = DeferredRegister.create(Registries.RECIPE_SERIALIZER, modid);
        }
        final ResourceLocation typeId = ResourceLocation.fromNamespaceAndPath(modid, id);
        final DeferredHolder<RecipeType<?>, RecipeType<ProcessingRecipe>> type =
                recipeTypes.register(id, () -> RecipeType.simple(typeId));
        final ProcessingKind[] kind = new ProcessingKind[1];
        final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ProcessingRecipe>> serializer =
                recipeSerializers.register(id, () -> kind[0].newSerializer());
        kind[0] = new ProcessingKind(typeId, TextKey.of(modid + ".recipe_kind." + id, englishName), type,
                serializer).alsoWorkedBy(machine);
        declaredProcessing.add(kind[0]);
        return kind[0];
    }

    /** The kinds of processing machine declared so far, in declaration order. */
    public List<ProcessingKind> declaredProcessing() {
        return Collections.unmodifiableList(declaredProcessing);
    }

    /** Starts declaring a kind of entity, made by {@code factory} and counted under {@code category}. */
    public <E extends Entity> EntityBuilder<E> entity(final String id, final EntityType.EntityFactory<E> factory,
                                                      final MobCategory category) {
        return new EntityBuilder<>(this, id, factory, category);
    }

    /** The kinds of entity declared so far, in declaration order. */
    public List<EntityEntry<?>> declaredEntities() {
        return Collections.unmodifiableList(declaredEntities);
    }

    /**
     * Declares a component the mod's items can carry, saved with {@code codec} and sent to players with
     * {@code streamCodec}; an item starts with it through {@link ItemBuilder#component}.
     */
    public <T> DeferredHolder<DataComponentType<?>, DataComponentType<T>> component(
            final String id, final Codec<T> codec, final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
        if (components == null) {
            components = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, modid);
        }
        return components.registerComponentType(id, builder -> builder.persistent(codec)
                .networkSynchronized(streamCodec));
    }

    /**
     * Declares a registry whose entries come from datapacks, each a file under
     * {@code data/<namespace>/<modid>/<name>/}, read with {@code codec} when a world loads and sent to every player
     * as they join; read its entries from the level's registries with the key this gives.
     */
    public <T> ResourceKey<Registry<T>> datapackRegistry(final String name, final Codec<T> codec) {
        final ResourceKey<Registry<T>> key =
                ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(modid, name));
        datapackRegistries.add(event -> event.dataPackRegistry(key, codec, codec));
        return key;
    }

    /** Hands the registrations to the mod's event bus; call it once, after every declaration class has loaded. */
    public void register(final IEventBus modEventBus) {
        Arrays.asList(blocks, items, blockEntities, tabs, sounds).forEach(register -> register.register(modEventBus));
        if (cables != null) {
            cables.register(modEventBus);
        }
        if (components != null) {
            components.register(modEventBus);
        }
        if (fluidTypes != null) {
            fluidTypes.register(modEventBus);
            fluids.register(modEventBus);
        }
        if (recipeTypes != null) {
            recipeTypes.register(modEventBus);
            recipeSerializers.register(modEventBus);
        }
        if (entityTypes != null) {
            entityTypes.register(modEventBus);
            modEventBus.addListener(EntityAttributeCreationEvent.class, this::giveEntitiesTheirAttributes);
        }
        if (!datapackRegistries.isEmpty()) {
            modEventBus.addListener(DataPackRegistryEvent.NewRegistry.class,
                    event -> datapackRegistries.forEach(registry -> registry.accept(event)));
        }
        modEventBus.addListener(RegisterCapabilitiesEvent.class, this::giveItemsTheirCapabilities);
        modEventBus.addListener(ModifyDefaultComponentsEvent.class, this::giveItemsTheirComponents);
    }

    DeferredRegister<EntityType<?>> entityRegister() {
        if (entityTypes == null) {
            entityTypes = DeferredRegister.create(Registries.ENTITY_TYPE, modid);
        }
        return entityTypes;
    }

    void declare(final EntityEntry<?> entry) {
        declaredEntities.add(entry);
    }

    DeferredRegister<CableType> cableRegister() {
        if (cables == null) {
            cables = DeferredRegister.create(CoreCables.KEY, modid);
        }
        return cables;
    }

    void declare(final CableEntry entry) {
        declaredCables.add(entry);
    }

    DeferredRegister<FluidType> fluidTypeRegister() {
        if (fluidTypes == null) {
            fluidTypes = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, modid);
            fluids = DeferredRegister.create(Registries.FLUID, modid);
        }
        return fluidTypes;
    }

    DeferredRegister<Fluid> fluidRegister() {
        fluidTypeRegister();
        return fluids;
    }

    void declare(final FluidEntry entry) {
        declaredFluids.add(entry);
    }

    DeferredRegister.Blocks blockRegister() {
        return blocks;
    }

    DeferredRegister.Items itemRegister() {
        return items;
    }

    void declare(final BlockEntry<?> entry) {
        declaredBlocks.add(entry);
    }

    void declare(final ItemEntry<?> entry) {
        declaredItems.add(entry);
    }

    DeferredRegister<SoundEvent> soundRegister() {
        return sounds;
    }

    void declare(final SoundKey key) {
        declaredSounds.add(key);
    }

    void declare(final SoundCue cue) {
        declaredCues.add(cue);
    }

    void declare(final CellFont font) {
        if (declaredFonts.stream().anyMatch(declared -> declared.id().equals(font.id()))) {
            throw new IllegalStateException("the font " + font.id() + " is declared twice");
        }
        declaredFonts.add(font);
    }

    /*
     * An item that holds something gets the game's capability for each thing it holds, and an entity that holds energy
     * the energy capability.
     */
    private void giveItemsTheirCapabilities(final RegisterCapabilitiesEvent event) {
        for (final ItemEntry<?> item : declaredItems) {
            if (!item.state().isNothing()) {
                ItemStates.registerCapabilities(event, item, item.state());
            }
        }
        for (final EntityEntry<?> entity : declaredEntities) {
            if (entity.holdsEnergy()) {
                event.registerEntity(Capabilities.EnergyStorage.ENTITY, entity.get(),
                        (held, context) -> held instanceof IEnergyHolder holder ? holder.energy() : null);
            }
        }
    }

    /* A living entity starts with the attributes it was declared with. */
    @SuppressWarnings("unchecked")
    private void giveEntitiesTheirAttributes(final EntityAttributeCreationEvent event) {
        for (final EntityEntry<?> entity : declaredEntities) {
            if (entity.attributes() != null) {
                event.put((EntityType<? extends LivingEntity>) entity.get(), entity.attributes().get().build());
            }
        }
    }

    /* An item declared with components starts with them. */
    private void giveItemsTheirComponents(final ModifyDefaultComponentsEvent event) {
        for (final ItemEntry<?> item : declaredItems) {
            if (!item.defaults().isEmpty()) {
                event.modify(item, patch -> item.defaults().forEach(given -> given.applyTo(patch)));
            }
        }
    }
}
