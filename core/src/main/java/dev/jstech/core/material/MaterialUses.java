/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.JsCore;
import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.IConfigValues;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * What of the materials each installed mod uses. A form of a material has a thing in the game only if some installed
 * mod says it uses it: mods declare their uses from their constructors, and the Core registers the union, each thing
 * in its common tag ({@code c:dusts/iron}), drawn in its material's colour as the game loads. A form another mod
 * already has, the game's iron ingot, stays that mod's.
 *
 * <pre>{@code
 * public static final Supplier<Item> MYTHRIL_PLATE = MaterialUses.use(MODID, MYTHRIL, Forms.PLATE);
 * MaterialUses.ore(MODID, MYTHRIL, HostRocks.STONE, HostRocks.DEEPSLATE);
 * }</pre>
 */
public final class MaterialUses {

    private static final Logger LOGGER = CoreLogs.CONTENT.logger();
    private static final List<MaterialUse> USES = new CopyOnWriteArrayList<>();
    private static final List<Kept> KEPT = new CopyOnWriteArrayList<>();
    private static final Map<Item, MaterialThing> THINGS = new ConcurrentHashMap<>();

    /* An ore is a little harder to break than its rock, as the game's are, and as hard to blow up as stone ore. */
    private static final float ORE_HARDER = 1.5f;
    private static final float ORE_RESISTANCE = 3.0f;
    private static final float BLOCK_HARDNESS = 5.0f;
    private static final float BLOCK_RESISTANCE = 6.0f;

    private static volatile List<RegisteredThing> registered = List.of();
    private static volatile Set<MaterialThing> sharedThings = Set.of();
    private static volatile boolean closed;

    private MaterialUses() {
    }

    /**
     * Declares that the mod {@code modid} uses {@code material} in {@code form}, as content the series' mods share,
     * and gives its item: the Core's, or the one another mod already has.
     *
     * @throws DeclarationError when the form does not fit the material, the form is an ore (declared with
     *                          {@link #ore}), or the game has already registered its things
     */
    public static Supplier<Item> use(final String modid, final Material material, final Form form) {
        if (form.shape() == Form.Shape.ORE) {
            throw DeclarationError.of(modid, "the use of " + material + " as " + form,
                    "names no rock", "declare an ore with MaterialUses.ore(modid, material, rocks...)");
        }
        add(new MaterialUse(modid, material, form, null, true));
        return item(material, form);
    }

    /**
     * Declares that the mod {@code modid} uses {@code material}'s ore in each of {@code rocks}, and so what the ore
     * drops: its raw form, its gem, its crystal or its dust; nothing but itself with Silk Touch for a liquid's ore.
     *
     * @throws DeclarationError when the material has no ore, no rock is named, or the game has already registered its
     *                          things
     */
    public static void ore(final String modid, final Material material, final HostRock... rocks) {
        if (rocks.length == 0) {
            throw DeclarationError.of(modid, "the ore of " + material, "names no rock",
                    "name the rocks its veins cross: HostRocks.STONE, HostRocks.DEEPSLATE...");
        }
        for (final HostRock rock : rocks) {
            add(new MaterialUse(modid, material, Forms.ORE, rock, true));
        }
        material.oreDrop().ifPresent(drop -> add(new MaterialUse(modid, material, drop, null, true)));
    }

    /** {@code material}'s item in {@code form}: the Core's, or the one another mod already has. */
    public static Supplier<Item> item(final Material material, final Form form) {
        final Optional<Supplier<? extends ItemLike>> provided = material.provided(form);
        if (provided.isPresent()) {
            final Supplier<? extends ItemLike> thing = provided.get();
            return () -> thing.get().asItem();
        }
        return DeferredItem.createItem(form.thingId(material));
    }

    /** {@code material}'s ore in {@code rock}: the Core's, or the one another mod already has. */
    public static Supplier<Block> oreBlock(final Material material, final HostRock rock) {
        final Optional<Supplier<? extends Block>> provided = material.providedOre(rock);
        if (provided.isPresent()) {
            final Supplier<? extends Block> ore = provided.get();
            return ore::get;
        }
        return DeferredBlock.createBlock(rock.oreId(material));
    }

    /** {@code material}'s block in {@code form}, a form that is a block: the block of tin. */
    public static Supplier<Block> block(final Material material, final Form form) {
        final Optional<Supplier<? extends ItemLike>> provided = material.provided(form);
        if (provided.isPresent()) {
            final Supplier<? extends ItemLike> thing = provided.get();
            return () -> Block.byItem(thing.get().asItem());
        }
        return DeferredBlock.createBlock(form.thingId(material));
    }

    /** Every use declared, in the order they were. */
    public static List<MaterialUse> all() {
        return List.copyOf(USES);
    }

    /** Whether some installed mod uses {@code material} in {@code form}. */
    public static boolean used(final Material material, final Form form) {
        for (final MaterialUse use : USES) {
            if (use.material() == material && use.form() == form) {
                return true;
            }
        }
        return false;
    }

    /** The mods that declared a use of shared content. */
    public static Set<String> users() {
        final Set<String> out = new LinkedHashSet<>();
        USES.forEach(use -> {
            if (use.shared()) {
                out.add(use.modid());
            }
        });
        return out;
    }

    /** Whether {@code item} is a thing of shared content some installed mod uses, one the Core registered. */
    public static boolean shared(final Item item) {
        final MaterialThing thing = THINGS.get(item);
        return thing != null && sharedThings.contains(thing);
    }

    /**
     * The items of shared content the Core registered, material by material in the order they were declared, form by
     * form, rock by rock. Asked once the game's registries are filled.
     */
    public static List<Item> sharedItems() {
        final List<Item> out = new ArrayList<>();
        for (final RegisteredThing thing : registered) {
            final Item item = BuiltInRegistries.ITEM.get(thing.id());
            if (shared(item)) {
                out.add(item);
            }
        }
        return out;
    }

    /** What the Core registered for the uses, in the order {@link #sharedItems()} gives. */
    public static List<RegisteredThing> registered() {
        return registered;
    }

    /** The material and the form {@code item} is, when the Core registered it. */
    public static Optional<MaterialThing> thingOf(final Item item) {
        return Optional.ofNullable(THINGS.get(item));
    }

    /**
     * Keeps a thing of the old fixed catalogue registered, by its ids, without sharing it: the items J's Industrial
     * still names through the old catalogue keep their ids until it declares its uses.
     */
    @ApiStatus.Internal
    public static DeferredItem<Item> keep(final ResourceLocation material, final ResourceLocation form) {
        KEPT.add(new Kept(material, form));
        return DeferredItem.createItem(ResourceLocation.fromNamespaceAndPath(material.getNamespace(),
                material.getPath() + "_" + form.getPath()));
    }

    /** Hands the mod's bus the registering of the union, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterEvent.class, MaterialUses::onRegister);
    }

    private static void add(final MaterialUse use) {
        if (closed) {
            throw DeclarationError.of(use.modid(), "the use of " + use.material() + " as " + use.form(),
                    "comes after the game registered its things", "declare uses from the mod's constructor");
        }
        if (!use.material().fits(use.form())) {
            throw DeclarationError.of(use.modid(), "the use of " + use.material() + " as " + use.form(),
                    "is of a form a " + use.material().kind().serializedName() + " cannot take",
                    "use only the forms its kind takes");
        }
        USES.add(use);
    }

    private static void onRegister(final RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            closed = true;
            registered = union();
            final Set<MaterialThing> shared = new HashSet<>();
            USES.forEach(use -> {
                if (use.shared()) {
                    shared.add(use.thing());
                }
            });
            sharedThings = Set.copyOf(shared);
            for (final RegisteredThing thing : registered) {
                if (thing.isBlock()) {
                    helper.register(thing.id(), new MaterialFormBlock(blockProperties(thing.thing(), thing.rock()),
                            thing.thing(), thing.rock()));
                }
            }
        });
        event.register(Registries.ITEM, helper -> {
            /*
             * An ore in another mod's rock takes its hardness and its sound from the rock, so it is made once every mod
             * has registered its blocks: when the game registers items, which comes after every mod's blocks.
             */
            final List<RegisteredThing> found = oresInFoundRocks();
            for (final RegisteredThing thing : found) {
                Registry.register(BuiltInRegistries.BLOCK, thing.id(), new MaterialFormBlock(blockProperties(
                        thing.thing(), thing.rock()), thing.thing(), thing.rock()));
            }
            if (!found.isEmpty()) {
                final Set<String> rocks = new TreeSet<>();
                found.forEach(thing -> rocks.add(String.valueOf(thing.rock())));
                LOGGER.info("Ores in other mods' rocks: {} ores in {} rocks found by their tags: {}", found.size(),
                        rocks.size(), rocks);
                final List<RegisteredThing> all = new ArrayList<>(registered);
                all.addAll(found);
                registered = Collections.unmodifiableList(all);
            }
            for (final RegisteredThing thing : registered) {
                final Item item;
                if (thing.isBlock()) {
                    final Block block = BuiltInRegistries.BLOCK.get(thing.id());
                    item = new MaterialFormBlockItem((MaterialFormBlock) block, new Item.Properties());
                } else {
                    item = new MaterialFormItem(new Item.Properties(), thing.thing());
                }
                helper.register(thing.id(), item);
                THINGS.put(item, thing.thing());
            }
        });
    }

    /* The things the uses call for that no mod has already, each once, in the order sharedItems() gives. */
    private static List<RegisteredThing> union() {
        final List<MaterialUse> all = new ArrayList<>(USES);
        for (final Kept kept : KEPT) {
            final Material material = Materials.byId(kept.material()).orElseThrow(() -> DeclarationError.of(
                    JsCore.MODID, "the kept thing " + kept.material() + "/" + kept.form(),
                    "names a material nobody declared", "declare the material"));
            final Form form = Forms.byId(kept.form()).orElseThrow(() -> DeclarationError.of(JsCore.MODID,
                    "the kept thing " + kept.material() + "/" + kept.form(), "names a form nobody declared",
                    "declare the form"));
            all.add(new MaterialUse(JsCore.MODID, material, form, null, false));
        }
        final List<Material> materials = Materials.all();
        final List<Form> forms = Forms.all();
        final List<HostRock> rocks = HostRocks.all();
        all.sort(Comparator.<MaterialUse>comparingInt(use -> materials.indexOf(use.material()))
                .thenComparingInt(use -> forms.indexOf(use.form()))
                .thenComparingInt(use -> use.rock() == null ? -1 : rocks.indexOf(use.rock())));
        final Map<ResourceLocation, RegisteredThing> out = new LinkedHashMap<>();
        for (final MaterialUse use : all) {
            final RegisteredThing thing = toRegister(use);
            if (thing == null) {
                continue;
            }
            final RegisteredThing before = out.putIfAbsent(thing.id(), thing);
            if (before != null && !before.equals(thing)) {
                throw DeclarationError.of(use.modid(), "the thing " + thing.id(), "is the id of both "
                        + before.thing() + " and " + thing.thing(), "give one of the materials another id");
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(out.values()));
    }

    /*
     * The ores of the materials in the rocks of other mods their tags name: every material with an ore in one of the
     * game's rocks of the same world, in every such rock the settings do not leave out, but where a mod already has
     * the ore or declared the rock itself.
     */
    private static List<RegisteredThing> oresInFoundRocks() {
        final IConfigValues settings = ConfigFiles.atStartup(VariantSettings.FILE);
        if (!settings.get(VariantSettings.ORES_IN_OTHER_ROCKS)) {
            return List.of();
        }
        final Set<String> skippedMods = Set.copyOf(settings.get(VariantSettings.SKIPPED_MODS));
        final Set<String> skippedRocks = Set.copyOf(settings.get(VariantSettings.SKIPPED_ROCKS));
        final Set<String> skippedMaterials = Set.copyOf(settings.get(VariantSettings.SKIPPED_MATERIALS));
        final Set<Block> declared = new HashSet<>();
        for (final HostRock rock : HostRocks.all()) {
            if (!rock.found()) {
                declared.add(rock.rock());
            }
        }
        final Set<ResourceLocation> taken = new HashSet<>();
        registered.forEach(thing -> taken.add(thing.id()));
        final List<RegisteredThing> out = new ArrayList<>();
        FoundRocks.inTags().forEach((id, realm) -> {
            if (id.getNamespace().equals(ResourceLocation.DEFAULT_NAMESPACE) || skippedMods.contains(id.getNamespace())
                    || skippedRocks.contains(id.toString())) {
                return;
            }
            final Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
            if (block.isEmpty() || declared.contains(block.get()) || !isRock(block.get())) {
                return;
            }
            final HostRock rock = HostRocks.found(id);
            for (final Material material : withOresIn(realm == FoundRocks.Realm.OVERWORLD ? HostRocks.OVERWORLD
                    : HostRocks.NETHER)) {
                if (skippedMaterials.contains(material.id().toString()) || material.providedOre(rock).isPresent()) {
                    continue;
                }
                final RegisteredThing thing = new RegisteredThing(new MaterialThing(material, Forms.ORE), rock,
                        rock.oreId(material));
                if (taken.add(thing.id())) {
                    out.add(thing);
                }
            }
        });
        return out;
    }

    /* The materials some mod has an ore of in one of {@code rocks}, in the order they were declared. */
    private static List<Material> withOresIn(final List<HostRock> rocks) {
        final Set<Material> out = new LinkedHashSet<>();
        for (final MaterialUse use : USES) {
            if (use.rock() != null && rocks.contains(use.rock())) {
                out.add(use.material());
            }
        }
        return new ArrayList<>(out);
    }

    /* A block ores can sit in: a whole cube with no block entity, as rock is. */
    private static boolean isRock(final Block block) {
        return !(block instanceof EntityBlock) && Block.isShapeFullBlock(block.defaultBlockState().getShape(
                EmptyBlockGetter.INSTANCE, BlockPos.ZERO));
    }

    /* What the Core registers for a use, or null when another mod already has its thing. */
    private static @Nullable RegisteredThing toRegister(final MaterialUse use) {
        final Material material = use.material();
        if (use.rock() != null) {
            return material.providedOre(use.rock()).isPresent() ? null
                    : new RegisteredThing(use.thing(), use.rock(), use.rock().oreId(material));
        }
        return material.provided(use.form()).isPresent() ? null
                : new RegisteredThing(use.thing(), null, use.form().thingId(material));
    }

    private static BlockBehaviour.Properties blockProperties(final MaterialThing thing, @Nullable final HostRock rock) {
        if (rock != null) {
            final Block stone = rock.rock();
            return BlockBehaviour.Properties.of().mapColor(stone.defaultMapColor())
                    .instrument(NoteBlockInstrument.BASEDRUM).requiresCorrectToolForDrops()
                    .strength(stone.defaultDestroyTime() + ORE_HARDER, ORE_RESISTANCE)
                    .sound(stone.defaultBlockState().getSoundType());
        }
        final BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().requiresCorrectToolForDrops()
                .strength(BLOCK_HARDNESS, BLOCK_RESISTANCE);
        return switch (thing.material().kind()) {
            case METAL -> properties.mapColor(MapColor.METAL).instrument(NoteBlockInstrument.IRON_XYLOPHONE)
                    .sound(SoundType.METAL);
            case GEM, CRYSTAL -> properties.mapColor(MapColor.DIAMOND).instrument(NoteBlockInstrument.BIT)
                    .sound(SoundType.METAL);
            default -> properties.mapColor(MapColor.STONE).instrument(NoteBlockInstrument.BASEDRUM)
                    .sound(SoundType.STONE);
        };
    }

    /* A thing of the old catalogue, by its ids. */
    private record Kept(ResourceLocation material, ResourceLocation form) {
    }
}
