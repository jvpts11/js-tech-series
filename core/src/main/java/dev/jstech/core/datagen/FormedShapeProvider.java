/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.model.GeometryBaker;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.core.multiblock.shape.MeshVoxels;
import dev.jstech.core.multiblock.shape.ModelTriangles;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the shape file of every formed model a mod declares, {@code data/<mod>/formed_shape/<path>.json}: the
 * model's faces at rest shared out among the blocks of its pattern, as the structure stands facing north round its
 * terminal, and each block's boxes worked out from its own faces. A server has no models, so this is how it knows what
 * the blocks collide with. The run stops when a block of the pattern has no face of the model, or faces lie more than
 * a block outside the pattern: a model and a pattern that have drifted apart. A model of blocks a plan lays is shared
 * out among its own blocks and the blocks beside them it reaches into, the same way.
 */
public final class FormedShapeProvider implements DataProvider {

    private final PackOutput.PathProvider paths;
    private final ModContent content;
    private final ExistingFileHelper files;

    public FormedShapeProvider(final PackOutput output, final ModContent content, final ExistingFileHelper files) {
        this.paths = output.createPathProvider(PackOutput.Target.DATA_PACK, "formed_shape");
        this.content = content;
        this.files = files;
    }

    /** Whether the mod {@code content} declares a formed model, which this writes the shape file of. */
    public static boolean declares(final ModContent content) {
        return !FormedShapes.declaredBy(content.modid()).isEmpty() || !FormedShapes.laidBy(content.modid()).isEmpty();
    }

    @Override
    public String getName() {
        return this.content.modid() + ":formed_shapes";
    }

    @Override
    public CompletableFuture<?> run(final CachedOutput cache) {
        final List<CompletableFuture<?>> written = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, ResourceLocation> formed
                : FormedShapes.declaredBy(this.content.modid()).entrySet()) {
            final FormedShape shape = shape(formed.getKey(), formed.getValue());
            final JsonElement json = FormedShape.CODEC.encodeStart(JsonOps.INSTANCE, shape)
                    .getOrThrow(problem -> new IllegalStateException(formed.getKey() + ": " + problem));
            written.add(DataProvider.saveStable(cache, json, this.paths.json(formed.getKey())));
        }
        // Every laid model that does not fit is told at once, so a whole set of them is mended in one go.
        final List<Misfit> misfits = new ArrayList<>();
        for (final Map.Entry<ResourceLocation, FormedShapes.Laid> laid
                : FormedShapes.laidBy(this.content.modid()).entrySet()) {
            final Optional<FormedShape> shape = laid(laid.getKey(), laid.getValue(), misfits);
            if (shape.isPresent()) {
                final JsonElement json = FormedShape.CODEC.encodeStart(JsonOps.INSTANCE, shape.get())
                        .getOrThrow(problem -> new IllegalStateException(laid.getKey() + ": " + problem));
                written.add(DataProvider.saveStable(cache, json, this.paths.json(laid.getKey())));
            }
        }
        if (!misfits.isEmpty()) {
            throw DeclarationError.of(this.content.modid(), "the laid models " + misfits.stream().map(misfit ->
                    misfit.id() + " (its own blocks " + places(misfit.bare()) + " have no face of it, and faces lie"
                            + " more than a block outside its blocks and those beside them at "
                            + places(misfit.outside()) + ")").collect(Collectors.joining("; ")),
                    "do not fit their blocks", "make each model fill every one of its blocks, and reach no further "
                            + "than the blocks it is declared to reach into");
        }
        return CompletableFuture.allOf(written.toArray(CompletableFuture[]::new));
    }

    /*
     * The shapes of the blocks of the model {@code id} a plan lays: its faces shared out among its own blocks and the
     * blocks beside them they may reach into, every one of its own needing a face, and none more than a block out.
     */
    private Optional<FormedShape> laid(final ResourceLocation id, final FormedShapes.Laid laid,
                                       final List<Misfit> misfits) {
        final DeclaredModel model = declared(id);
        final Set<Long> shaped = new LinkedHashSet<>(laid.own());
        shaped.addAll(laid.reach());
        final MeshCells.Split split = MeshCells.split(faces(model), laid.origin(), shaped, Set.of());
        final List<Long> bare = split.bare().stream().filter(cell -> laid.own().contains(cell)
                && !laid.hollow().contains(cell)).toList();
        if (!bare.isEmpty() || !split.outside().isEmpty()) {
            misfits.add(new Misfit(id, bare, split.outside().keySet()));
            return Optional.empty();
        }
        final Map<Long, int[]> cells = new TreeMap<>();
        split.pieces().forEach((cell, pieces) -> cells.put(cell, MeshVoxels.boxes(pieces)));
        // A hollow block with none of the model's faces is written with no boxes: it merges with no shape at all.
        for (final long cell : laid.hollow()) {
            cells.putIfAbsent(cell, new int[0]);
        }
        return Optional.of(new FormedShape(laid.origin(), Set.of(), cells, FormedShapes.aboardDeclared(id)));
    }

    /* The declared model {@code id}, which a formed model must be. */
    private DeclaredModel declared(final ResourceLocation id) {
        return this.content.declaredModels().stream().filter(each -> each.id().equals(id)).findFirst()
                .orElseThrow(() -> DeclarationError.of(id.getNamespace(), "the formed model " + id,
                        "is declared formed but not declared a model", "declare it with model(...)"));
    }

    /* The faces of {@code model} at rest, nine numbers to a triangle, in blocks. */
    private float[] faces(final DeclaredModel model) {
        final List<ModelData> read = ModelDefinitionProvider.read(model, model.definition().model(), this.files);
        final ModelData data = read.getFirst();
        return ModelTriangles.of(data, GeometryBaker.bake(data, Set.of()), model.definition().detail().scale());
    }

    /* The shapes of the blocks of the model {@code id}, formed from the pattern {@code patternId}. */
    private FormedShape shape(final ResourceLocation id, final ResourceLocation patternId) {
        final String namespace = id.getNamespace();
        final DeclaredModel model = declared(id);
        final MultiblockPattern pattern = MultiblockPatterns.get(patternId).orElseThrow(() -> DeclarationError.of(
                namespace, "the formed model " + id, "is formed from the pattern " + patternId + ", which is"
                        + " declared nowhere", "declare the pattern, or name the one it is formed from"));
        if (pattern.repeat().isPresent()) {
            throw DeclarationError.of(namespace, "the formed model " + id, "is formed from " + patternId
                    + ", which repeats, so is of no one size", "form it from a pattern of one size");
        }
        final Set<Long> shaped = new LinkedHashSet<>();
        final Set<Long> apart = new LinkedHashSet<>();
        final Character inside = pattern.inside().map(MultiblockPattern.Inside::letter).orElse(null);
        for (int y = 0; y < pattern.sizeY(); y++) {
            for (int z = 0; z < pattern.sizeZ(); z++) {
                for (int x = 0; x < pattern.sizeX(); x++) {
                    final char letter = pattern.charAt(x, y, z);
                    if (letter == MultiblockPattern.IGNORE_CHAR || inside != null && letter == inside) {
                        continue;
                    }
                    final long cell = MeshCells.key(x - pattern.terminalX(), y - pattern.terminalY(),
                            z - pattern.terminalZ());
                    (letter == MultiblockPattern.TERMINAL_CHAR ? apart : shaped).add(cell);
                }
            }
        }
        final MeshCells.Split split = MeshCells.split(faces(model), FormedShapes.patternOrigin(), shaped, apart);
        if (!split.fits()) {
            throw DeclarationError.of(namespace, "the formed model " + id, "does not fit its pattern " + patternId
                    + ": the blocks " + places(split.bare()) + " from its terminal have no face of it, and faces lie"
                    + " more than a block outside it at " + places(split.outside().keySet()), "make the model fill"
                    + " every block of the pattern, and reach no more than a block outside it");
        }
        final Map<Long, int[]> cells = new TreeMap<>();
        split.pieces().forEach((cell, pieces) -> cells.put(cell, MeshVoxels.boxes(pieces)));
        return new FormedShape(FormedShapes.patternOrigin(), apart, cells, FormedShapes.aboardDeclared(id));
    }

    /* The places {@code keys} as people read them, or a dash for none. */
    private static String places(final Collection<Long> keys) {
        return keys.isEmpty() ? "-" : keys.stream().map(MeshCells::name).collect(Collectors.joining(" "));
    }

    /* A laid model that does not fit: its own blocks with none of its faces, the blocks its faces lie out at. */
    private record Misfit(ResourceLocation id, Collection<Long> bare, Collection<Long> outside) {
    }
}
