/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.datagen;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.GeometryBaker;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.multiblock.MultiblockPattern;
import dev.jstech.core.multiblock.MultiblockPatterns;
import dev.jstech.core.multiblock.shape.FormedPiece;
import dev.jstech.core.multiblock.shape.FormedShape;
import dev.jstech.core.multiblock.shape.FormedShapes;
import dev.jstech.core.multiblock.shape.MeshCells;
import dev.jstech.core.multiblock.shape.MeshSolid;
import dev.jstech.core.multiblock.shape.MeshVoxels;
import dev.jstech.core.multiblock.shape.ModelTriangles;
import dev.jstech.core.util.DeclarationError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Writes the shape file of every formed model a mod declares, {@code data/<mod>/formed_shape/<path>.json}: the
 * model's faces at rest shared out among the blocks of its pattern, as the structure stands facing north round its
 * terminal, and each block's boxes worked out from its own faces and the solids the model's faces close. A server has
 * no models, so this is how it knows what the blocks collide with. The run stops when a block of the pattern has no
 * face of the model, or faces lie more than a block outside the pattern: a model and a pattern that have drifted
 * apart. A model of blocks a plan lays is shared
 * out among its own blocks and the blocks beside them it reaches into, the same way.
 */
public final class FormedShapeProvider implements DataProvider {

    private final PackOutput.PathProvider paths;
    private final ModContent content;
    private final ExistingFileHelper files;

    /* What a model's pieces file is called after its model's file, and how it reads: {"pieces": [...]}. */
    private static final String PIECES_FILE = ".pieces.json";
    private static final Codec<List<FormedPiece>> PIECES = FormedPiece.CODEC.listOf().fieldOf("pieces").codec();

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
        final float[] faces = faces(model);
        final MeshCells.Split split = MeshCells.split(faces, laid.origin(), shaped, Set.of());
        final List<Long> bare = split.bare().stream().filter(cell -> laid.own().contains(cell)
                && !laid.hollow().contains(cell)).toList();
        if (!bare.isEmpty() || !split.outside().isEmpty()) {
            misfits.add(new Misfit(id, bare, split.outside().keySet()));
            return Optional.empty();
        }
        final Map<Long, int[]> cells = boxes(faces, laid.origin(), split);
        // A hollow block with none of the model's faces is written with no boxes: it merges with no shape at all.
        for (final long cell : laid.hollow()) {
            cells.putIfAbsent(cell, new int[0]);
        }
        return Optional.of(new FormedShape(laid.origin(), Set.of(), cells, FormedShapes.aboardDeclared(id),
                pieces(model)));
    }

    /* The declared model {@code id}, which a formed model must be. */
    private DeclaredModel declared(final ResourceLocation id) {
        return this.content.declaredModels().stream().filter(each -> each.id().equals(id)).findFirst()
                .orElseThrow(() -> DeclarationError.of(id.getNamespace(), "the formed model " + id,
                        "is declared formed but not declared a model", "declare it with model(...)"));
    }

    /*
     * The faces of {@code model} at rest, nine numbers to a triangle, in blocks; but for the bones that move with its
     * doors, so their blocks take a leaf's shape from the door's shut leaf alone.
     */
    private float[] faces(final DeclaredModel model) {
        final List<ModelData> read = ModelDefinitionProvider.read(model, model.definition().model(), this.files);
        final ModelData data = read.getFirst();
        final Set<String> moving = new LinkedHashSet<>();
        for (final FormedPiece piece : pieces(model)) {
            moving.addAll(piece.moves());
        }
        final BakedGeometry baked = GeometryBaker.bake(data, Set.of());
        if (moving.isEmpty()) {
            return ModelTriangles.of(data, baked, model.definition().detail().scale());
        }
        // A moving bone, and every bone hanging from it, keeps no faces here.
        final List<List<BakedGeometry.Part>> kept = new ArrayList<>(baked.bones());
        final Set<String> left = new LinkedHashSet<>();
        for (int i = 0; i < data.bones().size(); i++) {
            final ModelBone bone = data.bones().get(i);
            if (moving.contains(bone.name()) || bone.parent() != null && left.contains(bone.parent())) {
                left.add(bone.name());
                kept.set(i, List.of());
            }
        }
        return ModelTriangles.of(data, new BakedGeometry(kept, baked.deformed()), model.definition().detail()
                .scale());
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
        final float[] faces = faces(model);
        final MeshCells.Split split = MeshCells.split(faces, FormedShapes.patternOrigin(), shaped, apart);
        if (!split.fits()) {
            throw DeclarationError.of(namespace, "the formed model " + id, "does not fit its pattern " + patternId
                    + ": the blocks " + places(split.bare()) + " from its terminal have no face of it, and faces lie"
                    + " more than a block outside it at " + places(split.outside().keySet()), "make the model fill"
                    + " every block of the pattern, and reach no more than a block outside it");
        }
        final Map<Long, int[]> cells = boxes(faces, FormedShapes.patternOrigin(), split);
        return new FormedShape(FormedShapes.patternOrigin(), apart, cells, FormedShapes.aboardDeclared(id),
                pieces(model));
    }

    /*
     * The pieces of {@code model} a player takes for one thing each: those declared, and those the file beside its
     * model's file holds, {@code <file>.pieces.json}, as a program making the model writes them out.
     */
    private List<FormedPiece> pieces(final DeclaredModel model) {
        final List<FormedPiece> out = new ArrayList<>(FormedShapes.piecesDeclared(model.id()));
        final ResourceLocation file = ResourceLocation.parse(model.definition().model());
        final String path = file.getPath();
        final int dot = path.lastIndexOf('.');
        final ResourceLocation beside = ResourceLocation.fromNamespaceAndPath(file.getNamespace(),
                (dot < 0 ? path : path.substring(0, dot)) + PIECES_FILE);
        if (!this.files.exists(beside, PackType.CLIENT_RESOURCES)) {
            return out;
        }
        try (Reader in = new InputStreamReader(this.files.getResource(beside, PackType.CLIENT_RESOURCES).open(),
                StandardCharsets.UTF_8)) {
            out.addAll(PIECES.parse(JsonOps.INSTANCE, JsonParser.parseReader(in)).getOrThrow(problem ->
                    DeclarationError.of(model.id().getNamespace(), "the pieces file " + beside, "does not read: "
                            + problem, "write it again from the program that made the model")));
        } catch (final IOException e) {
            throw new UncheckedIOException("the pieces file " + beside + " could not be read", e);
        }
        return out;
    }

    /*
     * Each block's boxes: the sixteenths its own faces pass through, and those within the solids the whole model's
     * faces close, so a wall is solid through its thickness and a seat through and through.
     */
    private static Map<Long, int[]> boxes(final float[] faces, final double[] origin, final MeshCells.Split split) {
        final MeshSolid solid = MeshSolid.of(faces);
        final Map<Long, int[]> cells = new TreeMap<>();
        split.pieces().forEach((cell, pieces) -> {
            final double[] corner = {origin[0] + MeshCells.x(cell), origin[1] + MeshCells.y(cell),
                origin[2] + MeshCells.z(cell)};
            cells.put(cell, MeshVoxels.boxes(pieces, grid -> solid.within(corner, grid)));
        });
        return cells;
    }

    /* The places {@code keys} as people read them, or a dash for none. */
    private static String places(final Collection<Long> keys) {
        return keys.isEmpty() ? "-" : keys.stream().map(MeshCells::name).collect(Collectors.joining(" "));
    }

    /* A laid model that does not fit: its own blocks with none of its faces, the blocks its faces lie out at. */
    private record Misfit(ResourceLocation id, Collection<Long> bare, Collection<Long> outside) {
    }
}
