/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.pack;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.JsCore;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.machine.DataMachineBlock;
import dev.jstech.core.machine.DataMachineBlockEntity;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.core.material.Materials;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.registry.CoreItems;
import dev.jstech.core.resource.RuntimePacks;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.slf4j.Logger;

/**
 * Content packs: blocks, items, materials, kinds of processing machine and the controllers of machines declared as
 * data, written in files rather than in Java, so a modpack adds them with no code. Since the game registers what it
 * holds before any world's data packs are read, the files are read as the game starts: from the game's folder, under
 * {@code jstech/content/}, and from every mod's own files, under {@code jstech_content/}, each file of
 * {@link PackReader}'s form. Each namespace a pack names is one content of its own, with its own creative tab.
 *
 * <p>What a pack does not bring is made as the game loads, under every pack a player or a server adds, so a resource
 * pack or a data pack can replace any of it: the English names, block states and models naming the textures
 * {@code <namespace>:block/<path>} and {@code <namespace>:item/<path>}, each block's drop and the tags of the tool it
 * needs. A material's things are the Core's own, drawn in its colour. What a machine does, its shape, slots and
 * recipes, is its machine file in a data pack, as for any machine declared as data.
 */
@TextHolder
public final class ContentPacks {

    public static final TextKey ART_TITLE = TextKey.of("jscore.pack.content_art", "J's Core: the content packs' art");
    public static final TextKey DATA_TITLE = TextKey.of("jscore.pack.content_data",
            "J's Core: the content packs' data");
    /** The folder of the game's own files, and of each mod's, the content packs are read from. */
    public static final String GAME_FOLDER = "jstech/content";
    public static final String MOD_FOLDER = "jstech_content";

    private static final Logger LOGGER = CoreLogs.CONTENT.logger();
    private static final String JSON = ".json";
    private static final List<Declared> DECLARED = new ArrayList<>();
    private static final Map<String, ModContent> CONTENTS = new LinkedHashMap<>();
    private static final Map<String, ContentTab.Section> TABS = new LinkedHashMap<>();

    private static volatile String digest = "";
    private static volatile DeferredHolder<BlockEntityType<?>, BlockEntityType<DataMachineBlockEntity>> machineEntity;

    private ContentPacks() {
    }

    /**
     * Reads every content pack and declares what each holds, once, from the Core's constructor, after the Core's own
     * catalogue and before its registries are handed to the game.
     */
    public static void declare(final IEventBus modBus) {
        final List<Source> sources = sources();
        digest = digestOf(sources);
        final Set<String> ids = new LinkedHashSet<>();
        final List<DeferredBlock<?>> machines = new ArrayList<>();
        for (final Source source : sources) {
            for (final PackDeclaration declaration : PackReader.read(source.name(), source.text())) {
                if (!ids.add(declaration.id())) {
                    throw DeclarationError.of(null, "the " + declaration.id() + " of " + source.name(),
                            "is declared twice", "keep one declaration of each id across the packs");
                }
                DECLARED.add(new Declared(declaration, source.name()));
            }
        }
        for (final Declared each : DECLARED) {
            declareOne(each.declaration(), machines);
        }
        if (!machines.isEmpty()) {
            machineEntity = CoreItems.CONTENT.blockEntity("pack_machine", (pos, state) -> new DataMachineBlockEntity(
                    machineEntity.get(), pos, state), machines.toArray(DeferredBlock<?>[]::new));
        }
        CONTENTS.values().forEach(content -> content.register(modBus));
        RuntimePacks.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "content_art"),
                PackType.CLIENT_RESOURCES, ART_TITLE, new PackArt(List.copyOf(DECLARED)));
        RuntimePacks.declare(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "content_data"),
                PackType.SERVER_DATA, DATA_TITLE, new PackData(List.copyOf(DECLARED)));
        if (!DECLARED.isEmpty()) {
            LOGGER.info("Content packs declared {} things in {} namespaces from {} files", DECLARED.size(),
                    CONTENTS.size(), sources.size());
        }
    }

    /** Everything the content packs declared, in the order they were read. */
    public static List<PackDeclaration> declared() {
        return DECLARED.stream().map(Declared::declaration).toList();
    }

    /** The namespaces the content packs gave their things. */
    public static Set<String> namespaces() {
        return Set.copyOf(CONTENTS.keySet());
    }

    /** Whether the namespace is a content pack's, not a mod's. */
    public static boolean isPack(final String namespace) {
        return CONTENTS.containsKey(namespace);
    }

    /**
     * A fingerprint of every content pack file as the game read them at its start, which tells whether they changed
     * since: a change waits for the game to start again, since what they declare is registered once.
     */
    public static String digest() {
        return digest;
    }

    /** A fingerprint of the content pack files as they stand now. */
    public static String digestNow() {
        return digestOf(sources());
    }

    /* Each declaration made in the content of its namespace, every one of them in the namespace's tab. */
    private static void declareOne(final PackDeclaration declaration, final List<DeferredBlock<?>> machines) {
        final ResourceLocation id = ResourceLocation.parse(declaration.id());
        final ModContent content = contentOf(id.getNamespace());
        switch (declaration) {
            case PackDeclaration.Block block -> {
                final var builder = content.block(id.getPath(), properties -> new Block(configured(properties,
                        block))).named(block.name()).look(IBlockLook.cubeAll(id.getPath()));
                if (block.item()) {
                    builder.item().tab(sectionOf(id.getNamespace()));
                } else {
                    // With no item to drop, it drops nothing.
                    builder.drops(Drops.NONE);
                }
                builder.register();
            }
            case PackDeclaration.Item item -> content.item(id.getPath(), properties -> new Item(properties
                    .stacksTo(item.stack()))).named(item.name()).tab(sectionOf(id.getNamespace())).register();
            case PackDeclaration.Material material -> declareMaterial(id, material);
            case PackDeclaration.Processing processing -> {
                final Supplier<Item> machine = processing.machine().isEmpty() ? () -> Items.AIR
                        : () -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(processing.machine()));
                content.processing(id.getPath(), processing.name(), machine);
            }
            case PackDeclaration.Machine machine -> machines.add(content.block(id.getPath(),
                            properties -> new DataMachineBlock(properties.strength(machine.strength())
                                    .sound(SoundType.METAL).requiresCorrectToolForDrops(),
                                    Device.of(() -> machineEntity.get()).ticks(MachineBlockEntity::serverTick)
                                            .opensMenu(MachineMenu::new), id))
                    .named(machine.name()).look(IBlockLook.orientable(id.getPath())).item()
                    .tab(sectionOf(id.getNamespace())).register());
            case PackDeclaration.Tab tab -> {
                // A namespace's tab is made with its first thing; a tab declared names it and its icon.
            }
        }
    }

    private static void declareMaterial(final ResourceLocation id, final PackDeclaration.Material declared) {
        final String pack = id.getNamespace();
        final MaterialKind kind = MaterialKind.byName(declared.kind()).orElseThrow();
        final Material.Builder builder = Materials.material(id, kind, TextKey.of(nameKey(id), declared.name()))
                .colour(declared.colour());
        if (declared.liquid()) {
            builder.also(MatterState.LIQUID);
        }
        for (final Map.Entry<String, JsonElement> each : declared.properties().entrySet()) {
            final MaterialProperty<?> property = MaterialProperties.byId(idIn(each.getKey()))
                    .orElseThrow(() -> DeclarationError.of(pack, "the material " + id, "has the property "
                            + each.getKey() + ", which no mod declares", "name a property the Core or a mod has, "
                            + "such as jscore:density"));
            with(builder, property, each.getValue(), id);
        }
        final Material material = builder.declare();
        for (final String name : declared.forms()) {
            final Form form = Forms.byId(idIn(name)).orElseThrow(() -> DeclarationError.of(pack, "the material "
                    + id, "is used in the form " + name + ", which no mod declares", "name a form such as ingot, "
                    + "dust, plate or block"));
            if (form.shape() == Form.Shape.ORE) {
                throw DeclarationError.of(pack, "the material " + id, "names the ore among its forms",
                        "give its ore the rocks it is in, \"ores\": [\"stone\", \"deepslate\"]");
            }
            MaterialUses.use(pack, material, form);
        }
        if (!declared.ores().isEmpty()) {
            final List<HostRock> rocks = new ArrayList<>();
            for (final String name : declared.ores()) {
                rocks.add(HostRocks.byId(idIn(name, ResourceLocation.DEFAULT_NAMESPACE)).orElseThrow(() ->
                        DeclarationError.of(pack, "the material " + id, "has its ore in the rock " + name
                                + ", which no mod declares", "name a rock such as stone, deepslate or granite")));
            }
            MaterialUses.ore(pack, material, rocks.toArray(HostRock[]::new));
        }
    }

    private static <T> void with(final Material.Builder builder, final MaterialProperty<T> property,
                                 final JsonElement value, final ResourceLocation material) {
        final T read = property.codec().parse(JsonOps.INSTANCE, value).getOrThrow(problem -> DeclarationError.of(
                material.getNamespace(), "the material " + material, "has the property " + property.id() + " as "
                        + value + ", which it cannot read (" + problem + ")",
                "write it as the property is written in a material's file"));
        builder.with(property, read);
    }

    private static BlockBehaviour.Properties configured(final BlockBehaviour.Properties properties,
                                                        final PackDeclaration.Block block) {
        BlockBehaviour.Properties made = properties.strength(block.strength(), block.resistance())
                .sound(soundOf(block.sound())).lightLevel(state -> block.light());
        if (!block.needs().isEmpty()) {
            made = made.requiresCorrectToolForDrops();
        }
        return made;
    }

    private static SoundType soundOf(final String name) {
        return switch (name) {
            case "wood" -> SoundType.WOOD;
            case "metal" -> SoundType.METAL;
            case "glass" -> SoundType.GLASS;
            case "wool" -> SoundType.WOOL;
            case "gravel" -> SoundType.GRAVEL;
            case "sand" -> SoundType.SAND;
            case "grass" -> SoundType.GRASS;
            case "copper" -> SoundType.COPPER;
            case "deepslate" -> SoundType.DEEPSLATE;
            case "amethyst" -> SoundType.AMETHYST;
            default -> SoundType.STONE;
        };
    }

    /* The content of a pack's namespace, made the first time the namespace is named; never a mod's own. */
    private static ModContent contentOf(final String namespace) {
        final ModContent known = CONTENTS.get(namespace);
        if (known != null) {
            return known;
        }
        if (ModList.get().isLoaded(namespace) || ModContent.all().stream().anyMatch(content
                -> content.modid().equals(namespace))) {
            throw DeclarationError.of(null, "its things in " + namespace, "are in the namespace of the mod "
                    + namespace, "give the pack's things a namespace of its own, such as mypack");
        }
        final ModContent made = new ModContent(namespace);
        CONTENTS.put(namespace, made);
        return made;
    }

    /* The tab of a namespace: the one a pack declared, named and drawn as it said, or one named for the namespace. */
    private static ContentTab.Section sectionOf(final String namespace) {
        return TABS.computeIfAbsent(namespace, ns -> {
            final PackDeclaration.Tab declared = DECLARED.stream().map(Declared::declaration)
                    .filter(PackDeclaration.Tab.class::isInstance).map(PackDeclaration.Tab.class::cast)
                    .filter(tab -> ResourceLocation.parse(tab.id()).getNamespace().equals(ns)).findFirst()
                    .orElse(null);
            final String path = declared == null ? "content" : ResourceLocation.parse(declared.id()).getPath();
            final String title = declared == null ? ns : declared.name();
            final String icon = declared == null || declared.icon().isEmpty() ? firstThingOf(ns) : declared.icon();
            return contentOf(ns).tab(path, title, () -> BuiltInRegistries.ITEM.get(ResourceLocation.parse(icon)))
                    .section();
        });
    }

    /* The first block with an item, or item, a namespace's packs declare, drawn on its tab. */
    private static String firstThingOf(final String namespace) {
        for (final Declared each : DECLARED) {
            final PackDeclaration declaration = each.declaration();
            final boolean thing = declaration instanceof PackDeclaration.Item
                    || declaration instanceof PackDeclaration.Machine
                    || declaration instanceof PackDeclaration.Block block && block.item();
            if (thing && ResourceLocation.parse(declaration.id()).getNamespace().equals(namespace)) {
                return declaration.id();
            }
        }
        return BuiltInRegistries.ITEM.getKey(Items.BOOK).toString();
    }

    /** The key a pack's material is named by. */
    static String nameKey(final ResourceLocation material) {
        return material.getNamespace() + ".material." + material.getPath();
    }

    /* An id as a pack writes it: with its namespace, or the Core's when it has none. */
    private static ResourceLocation idIn(final String written) {
        return idIn(written, JsCore.MODID);
    }

    /* An id as a pack writes it: with its namespace, or {@code namespace} when it has none, as the game's rocks do. */
    private static ResourceLocation idIn(final String written, final String namespace) {
        return written.contains(":") ? ResourceLocation.parse(written)
                : ResourceLocation.fromNamespaceAndPath(namespace, written);
    }

    /* Every content pack file, the game's folder first and then each mod's, each folder's files in name order. */
    private static List<Source> sources() {
        final List<Source> out = new ArrayList<>();
        readFolder(FMLPaths.GAMEDIR.get().resolve(GAME_FOLDER), GAME_FOLDER, out);
        for (final IModFileInfo file : ModList.get().getModFiles()) {
            final Path root = file.getFile().findResource(MOD_FOLDER);
            readFolder(root, file.getFile().getFileName() + "/" + MOD_FOLDER, out);
        }
        return out;
    }

    private static void readFolder(final Path folder, final String named, final List<Source> out) {
        if (!Files.isDirectory(folder)) {
            return;
        }
        try (Stream<Path> files = Files.walk(folder)) {
            for (final Path file : files.filter(path -> path.toString().endsWith(JSON)).sorted().toList()) {
                out.add(new Source(named + "/" + folder.relativize(file).toString().replace('\\', '/'),
                        Files.readString(file, StandardCharsets.UTF_8)));
            }
        } catch (final IOException e) {
            throw new UncheckedIOException("the content packs in " + folder + " could not be read", e);
        }
    }

    private static String digestOf(final List<Source> sources) {
        try {
            final MessageDigest sha = MessageDigest.getInstance("SHA-256");
            for (final Source source : sources) {
                sha.update(source.name().getBytes(StandardCharsets.UTF_8));
                sha.update(source.text().getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(sha.digest());
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalStateException("the game has no SHA-256", e);
        }
    }

    /* A file a content pack was read from: where, and what it holds. */
    private record Source(String name, String text) {
    }

    /**
     * A declaration with the file it was read from.
     *
     * @param declaration what is declared
     * @param source      the file
     */
    record Declared(PackDeclaration declaration, String source) {
    }
}
