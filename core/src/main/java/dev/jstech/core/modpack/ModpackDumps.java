/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.modpack;

import com.mojang.brigadier.context.CommandContext;
import dev.jstech.core.command.SeriesCommands;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.dimension.DimensionHazardEvent;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.Hazards;
import dev.jstech.core.hazard.ItemHazards;
import dev.jstech.core.material.Form;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.material.Materials;
import dev.jstech.core.pack.ContentPacks;
import dev.jstech.core.protection.ProtectionCommands;
import dev.jstech.core.text.GameText;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.fml.loading.FMLPaths;

/**
 * {@code /jstech dump}: what the game holds, written to files under {@code jstech-dump/} in the game's folder for
 * those who build a modpack, each as a page of plain Markdown: every material with its forms and properties, every
 * form, every kind of recipe with how many recipes the data holds of it, every tag of items, blocks and fluids with
 * what is in it, every hazard, and every id the series' mods and the content packs registered.
 * {@code /jstech dump reference} writes the generated reference, a small website ({@link ReferenceSite});
 * {@code /jstech dump all} writes them all, and {@code /jstech dump permissions} the series' permissions as before.
 */
public final class ModpackDumps {

    /** The folder in the game's folder the dumps are written to. */
    public static final String FOLDER = "jstech-dump";
    /** The folder in it the reference's website is written to. */
    public static final String REFERENCE = "reference";
    private static final String ROW_END = " |\n";
    private static final String CELL = " | ";

    private ModpackDumps() {
    }

    /** Declares {@code /jstech dump}, from the Core's constructor. */
    public static void declare() {
        SeriesCommands.declare("dump", SeriesCommands.ADMINS, (node, context) -> node
                .then(Commands.literal("permissions").executes(ProtectionCommands::dumpPermissions))
                .then(Commands.literal("materials").executes(source -> page(source, "materials.md",
                        server -> materials())))
                .then(Commands.literal("forms").executes(source -> page(source, "forms.md", server -> forms())))
                .then(Commands.literal("recipes").executes(source -> page(source, "recipes.md",
                        ModpackDumps::recipes)))
                .then(Commands.literal("tags").executes(source -> page(source, "tags.md", server -> tags())))
                .then(Commands.literal("hazards").executes(source -> page(source, "hazards.md",
                        server -> hazards())))
                .then(Commands.literal("ids").executes(source -> page(source, "ids.md", server -> ids())))
                .then(Commands.literal(REFERENCE).executes(ModpackDumps::reference))
                .then(Commands.literal("all").executes(ModpackDumps::all)));
    }

    /** Every material: its kind, the forms it has things of, and its properties. */
    public static String materials() {
        final StringBuilder out = new StringBuilder("# Materials\n\n| Material | Kind | Forms | Properties |\n")
                .append("| --- | --- | --- | --- |\n");
        for (final Material material : sorted(Materials.all(), material -> material.id().toString())) {
            final List<String> forms = new ArrayList<>();
            for (final Form form : Forms.all()) {
                if (material.provided(form).isPresent()
                        || BuiltInRegistries.ITEM.containsKey(form.thingId(material))) {
                    forms.add(form.id().getPath());
                }
            }
            final List<String> properties = new ArrayList<>();
            for (final Map.Entry<MaterialProperty<?>, Object> value : material.values().entrySet()) {
                properties.add(value.getKey().id() + " = " + value.getValue());
            }
            properties.sort(null);
            out.append("| ").append(material.id()).append(CELL).append(kindOf(material.kind())).append(CELL)
                    .append(String.join(", ", forms)).append(CELL).append(String.join(", ", properties))
                    .append(ROW_END);
        }
        return out.toString();
    }

    /** Every form a material may take: what it is in the game, its common tags' folder and the kinds it fits. */
    public static String forms() {
        final StringBuilder out = new StringBuilder("# Forms\n\n| Form | Shape | Common tags | Fits |\n")
                .append("| --- | --- | --- | --- |\n");
        for (final Form form : sorted(Forms.all(), form -> form.id().toString())) {
            final List<String> kinds = new ArrayList<>();
            form.kinds().forEach(kind -> kinds.add(kindOf(kind)));
            kinds.sort(null);
            out.append("| ").append(form.id()).append(CELL).append(form.shape().name().toLowerCase(Locale.ROOT))
                    .append(CELL).append("c:").append(form.tagFolder()).append("/<material>").append(CELL)
                    .append(String.join(", ", kinds)).append(ROW_END);
        }
        return out.toString();
    }

    /** Every kind of recipe the data holds recipes of: how many, and the serializers that wrote them. */
    public static String recipes(final MinecraftServer server) {
        final Map<String, Integer> counts = new TreeMap<>();
        final Map<String, Set<String>> serializers = new TreeMap<>();
        for (final RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
            final String type = String.valueOf(BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()));
            counts.merge(type, 1, Integer::sum);
            serializers.computeIfAbsent(type, unused -> new TreeSet<>())
                    .add(String.valueOf(BuiltInRegistries.RECIPE_SERIALIZER.getKey(holder.value().getSerializer())));
        }
        final StringBuilder out = new StringBuilder("# Recipes\n\n| Kind of recipe | Recipes | Written by |\n")
                .append("| --- | --- | --- |\n");
        counts.forEach((type, count) -> out.append("| ").append(type).append(CELL).append(count).append(CELL)
                .append(String.join(", ", serializers.get(type))).append(ROW_END));
        return out.toString();
    }

    /** Every tag of items, blocks and fluids, with what is in it. */
    public static String tags() {
        final StringBuilder out = new StringBuilder("# Tags\n");
        tagsOf(out, "Items", BuiltInRegistries.ITEM);
        tagsOf(out, "Blocks", BuiltInRegistries.BLOCK);
        tagsOf(out, "Fluids", BuiltInRegistries.FLUID);
        return out.toString();
    }

    /** Every hazard: those of bodies, those items carry and the items that carry them, and those of dimensions. */
    public static String hazards() {
        final StringBuilder out = new StringBuilder("# Hazards\n\n## Hazards to bodies\n\n| Hazard | Name | Unit |\n")
                .append("| --- | --- | --- |\n");
        for (final HazardKind kind : sorted(Hazards.all(), kind -> kind.id().toString())) {
            out.append("| ").append(kind.id()).append(CELL).append(kind.name().english()).append(CELL)
                    .append(kind.unit().english()).append(ROW_END);
        }
        out.append("\n## Hazards of items\n\n| Hazard | Name |\n| --- | --- |\n");
        for (final ItemHazards.Kind kind : sorted(ItemHazards.all(), kind -> kind.id().toString())) {
            out.append("| ").append(kind.id()).append(CELL).append(kind.name().english()).append(ROW_END);
        }
        out.append("\n## Items that carry a hazard\n\n| Item | Hazards |\n| --- | --- |\n");
        final Map<String, String> carried = new TreeMap<>();
        BuiltInRegistries.ITEM.getDataMap(ItemHazards.ITEMS).forEach((item, hazards) -> carried.put(
                item.location().toString(), new TreeMap<>(hazards).toString()));
        carried.forEach((item, hazards) -> out.append("| ").append(item).append(CELL).append(hazards)
                .append(ROW_END));
        out.append("\n## Hazards of dimensions\n\n");
        for (final DimensionHazardEvent.Hazard hazard : DimensionHazardEvent.Hazard.values()) {
            out.append("- ").append(hazard.name().toLowerCase(Locale.ROOT)).append('\n');
        }
        return out.toString();
    }

    /** Every id the series' mods and the content packs registered, by namespace and by registry. */
    public static String ids() {
        final Set<String> namespaces = new TreeSet<>(ContentPacks.namespaces());
        for (final ModContent content : ModContent.all()) {
            namespaces.add(content.modid());
        }
        final StringBuilder out = new StringBuilder("# Ids\n");
        for (final String namespace : namespaces) {
            out.append("\n## ").append(namespace).append('\n');
            for (final Registry<?> registry : sorted(registryList(), r -> r.key().location().toString())) {
                final List<String> ids = new ArrayList<>();
                for (final ResourceLocation id : registry.keySet()) {
                    if (id.getNamespace().equals(namespace)) {
                        ids.add(id.toString());
                    }
                }
                if (!ids.isEmpty()) {
                    ids.sort(null);
                    out.append("\n### ").append(registry.key().location()).append(" (").append(ids.size())
                            .append(")\n\n");
                    ids.forEach(id -> out.append("- ").append(id).append('\n'));
                }
            }
        }
        return out.toString();
    }

    /** Writes every page and the reference under {@code folder}; how many files. */
    public static int writeAll(final MinecraftServer server, final Path folder) throws IOException {
        final Map<String, String> pages = new TreeMap<>();
        pages.put("materials.md", materials());
        pages.put("forms.md", forms());
        pages.put("recipes.md", recipes(server));
        pages.put("tags.md", tags());
        pages.put("hazards.md", hazards());
        pages.put("ids.md", ids());
        for (final Map.Entry<String, String> page : pages.entrySet()) {
            write(folder.resolve(page.getKey()), page.getValue());
        }
        return pages.size() + writeReference(server, folder.resolve(REFERENCE));
    }

    /** Writes the reference's website into {@code folder}; how many files. */
    public static int writeReference(final MinecraftServer server, final Path folder) throws IOException {
        final Map<String, String> files = ReferenceSite.files(References.sections(server));
        for (final Map.Entry<String, String> file : files.entrySet()) {
            write(folder.resolve(file.getKey()), file.getValue());
        }
        return files.size();
    }

    private static int page(final CommandContext<CommandSourceStack> context, final String name,
                            final Function<MinecraftServer, String> made) {
        final Path file = FMLPaths.GAMEDIR.get().resolve(FOLDER).resolve(name);
        try {
            write(file, made.apply(context.getSource().getServer()));
        } catch (final IOException e) {
            context.getSource().sendFailure(GameText.component(ModpackTexts.NOT_DUMPED.with(name, e.getMessage())));
            return 0;
        }
        context.getSource().sendSuccess(() -> GameText.component(ModpackTexts.DUMPED.with(name,
                file.toString())), false);
        return 1;
    }

    private static int reference(final CommandContext<CommandSourceStack> context) {
        final Path folder = FMLPaths.GAMEDIR.get().resolve(FOLDER).resolve(REFERENCE);
        final int written;
        try {
            written = writeReference(context.getSource().getServer(), folder);
        } catch (final IOException e) {
            context.getSource().sendFailure(GameText.component(ModpackTexts.NOT_DUMPED.with(REFERENCE,
                    e.getMessage())));
            return 0;
        }
        context.getSource().sendSuccess(() -> GameText.component(ModpackTexts.REFERENCE_WRITTEN.with(written,
                folder.toString())), false);
        return written;
    }

    private static int all(final CommandContext<CommandSourceStack> context) {
        final Path folder = FMLPaths.GAMEDIR.get().resolve(FOLDER);
        final int written;
        try {
            written = writeAll(context.getSource().getServer(), folder);
        } catch (final IOException e) {
            context.getSource().sendFailure(GameText.component(ModpackTexts.NOT_DUMPED.with(FOLDER,
                    e.getMessage())));
            return 0;
        }
        context.getSource().sendSuccess(() -> GameText.component(ModpackTexts.DUMPED.with(written,
                folder.toString())), false);
        return written;
    }

    private static <T> void tagsOf(final StringBuilder out, final String title, final Registry<T> registry) {
        out.append("\n## ").append(title).append("\n\n| Tag | Holds |\n| --- | --- |\n");
        final Map<String, String> tags = new TreeMap<>();
        registry.getTags().forEach(pair -> {
            final List<String> members = new ArrayList<>();
            for (final Holder<T> holder : pair.getSecond()) {
                holder.unwrapKey().map(ResourceKey::location).ifPresent(id -> members.add(id.toString()));
            }
            members.sort(null);
            tags.put(pair.getFirst().location().toString(), String.join(", ", members));
        });
        tags.forEach((tag, members) -> out.append("| #").append(tag).append(CELL).append(members).append(ROW_END));
    }

    private static List<Registry<?>> registryList() {
        final List<Registry<?>> out = new ArrayList<>();
        BuiltInRegistries.REGISTRY.forEach(out::add);
        return out;
    }

    private static String kindOf(final MaterialKind kind) {
        return kind.name().toLowerCase(Locale.ROOT);
    }

    private static <T> List<T> sorted(final Iterable<T> things, final Function<T, String> by) {
        final List<T> out = new ArrayList<>();
        things.forEach(out::add);
        out.sort((one, other) -> by.apply(one).compareTo(by.apply(other)));
        return out;
    }

    private static void write(final Path file, final String text) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }
}
