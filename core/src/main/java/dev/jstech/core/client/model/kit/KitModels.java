/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.JsCore;
import dev.jstech.core.content.DeclaredModel;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.GeometryBaker;
import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelFormatException;
import dev.jstech.core.model.ModelReaders;
import dev.jstech.core.model.ModelTexture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.InactiveProfiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.slf4j.Logger;

/**
 * The models drawn with the Core's models, as the player's game holds them: read with the game's resources, from the
 * definition every declared model has beside its file ({@code assets/<mod>/jsmodels/<name>.model.json}), each file
 * through the Core's own reader of its format, its shape worked out once, the pictures a file carries made textures.
 * A model whose files do not read is left out with a line in the log, and the rest are drawn. A model file no
 * definition names, as the preview screen opens, is read when it is first asked for.
 */
public final class KitModels extends SimplePreparableReloadListener<KitModels.Loaded> {

    private static final Logger LOGGER = CoreLogs.MODELS.logger();
    private static final KitModels INSTANCE = new KitModels();
    private static final String TEXTURES = "textures/";
    private static final String PNG = ".png";
    private static final Map<ResourceLocation, KitModel> LOOSE = new ConcurrentHashMap<>();
    private static final Set<ResourceLocation> BROKEN_LOOKS = ConcurrentHashMap.newKeySet();
    /* The textures made from pictures the files carry, on the render thread, kept to be let go at the next load. */
    private static final List<ResourceLocation> MADE = new ArrayList<>();

    private static volatile Map<ResourceLocation, KitModel> models = Map.of();
    private static volatile List<String> problems = List.of();
    private static volatile long reloadedAt;

    private KitModels() {
    }

    /** Hands the mod's bus the reading, once, from the client setup. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class, event -> event.registerReloadListener(INSTANCE));
    }

    /** The model named {@code id}, as its definition declares it, if it read. */
    public static Optional<KitModel> get(final ResourceLocation id) {
        return Optional.ofNullable(models.get(id));
    }

    /** The names of the models read, declared ones only. */
    public static Collection<ResourceLocation> ids() {
        return models.keySet();
    }

    /**
     * The model in the file {@code file}, a declared one's if a definition names it, else read now as its file has
     * it; on the render thread.
     *
     * @throws ModelFormatException when it does not read
     */
    public static KitModel file(final ResourceLocation file) {
        for (final KitModel model : models.values()) {
            if (model.file().equals(file)) {
                return model;
            }
        }
        final KitModel cached = LOOSE.get(file);
        if (cached != null) {
            return cached;
        }
        final Map<ResourceLocation, NativeImage> images = new LinkedHashMap<>();
        final KitModel read = read(Minecraft.getInstance().getResourceManager(), file,
                ModelDefinition.of(file.toString(), List.of()), images);
        upload(images);
        LOOSE.put(file, read);
        return read;
    }

    /**
     * The model in the file {@code file} painted with {@code texture} and playing the animations of
     * {@code animations}, as a family of GeckoLib's files lays them out, read the first time it is asked for; on the
     * render thread. None when it does not read, said once in the log.
     */
    public static Optional<KitModel> look(final ResourceLocation file, final ResourceLocation texture,
                                          final ResourceLocation animations) {
        final KitModel cached = LOOSE.get(file);
        if (cached != null) {
            return Optional.of(cached);
        }
        if (BROKEN_LOOKS.contains(file)) {
            return Optional.empty();
        }
        final ResourceManager manager = Minecraft.getInstance().getResourceManager();
        final List<String> animationFiles = manager.getResource(animations).isPresent()
                ? List.of(animations.toString()) : List.of();
        final ModelDefinition definition = new ModelDefinition(file.toString(), List.of(texture.toString()),
                animationFiles, false, Set.of(), true, List.of(), 0, ModelDefinition.WHITE);
        try {
            final Map<ResourceLocation, NativeImage> images = new LinkedHashMap<>();
            final KitModel read = read(manager, file, definition, images);
            upload(images);
            LOOSE.put(file, read);
            return Optional.of(read);
        } catch (final ModelFormatException broken) {
            BROKEN_LOOKS.add(file);
            LOGGER.warn("The model {} does not read: {}", file, broken.getMessage());
            return Optional.empty();
        }
    }

    /** When the models were last read, by the clock of {@link System#currentTimeMillis}. */
    public static long reloadedAt() {
        return reloadedAt;
    }

    /** What did not read the last time, one line each. */
    public static List<String> problems() {
        return problems;
    }

    /** Reads the models again now, and nothing else of the resources; on the render thread, in development. */
    public static void reloadNow() {
        final ResourceManager manager = Minecraft.getInstance().getResourceManager();
        INSTANCE.apply(INSTANCE.prepare(manager, InactiveProfiler.INSTANCE), manager, InactiveProfiler.INSTANCE);
    }

    @Override
    protected Loaded prepare(final ResourceManager manager, final ProfilerFiller profiler) {
        final Map<ResourceLocation, KitModel> read = new LinkedHashMap<>();
        final Map<ResourceLocation, NativeImage> images = new LinkedHashMap<>();
        final List<String> wrong = new ArrayList<>();
        final Map<ResourceLocation, Resource> definitions = manager.listResources(DeclaredModel.FOLDER,
                path -> path.getPath().endsWith(DeclaredModel.DEFINITION_ENDING));
        for (final Map.Entry<ResourceLocation, Resource> entry : definitions.entrySet()) {
            final ResourceLocation id = idOf(entry.getKey());
            try (InputStream in = entry.getValue().open()) {
                final JsonObject json = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8))
                        .getAsJsonObject();
                final ModelDefinition definition = ModelDefinition.fromJson(id.toString(), json);
                read.put(id, read(manager, id, ResourceLocation.parse(definition.model()), definition, images));
            } catch (final IOException | RuntimeException broken) {
                wrong.add(id + ": " + broken.getMessage());
                LOGGER.warn("The model {} is left out: {}", id, broken.getMessage());
            }
        }
        return new Loaded(read, images, wrong);
    }

    @Override
    protected void apply(final Loaded loaded, final ResourceManager manager, final ProfilerFiller profiler) {
        for (final ResourceLocation made : MADE) {
            Minecraft.getInstance().getTextureManager().release(made);
        }
        MADE.clear();
        LOOSE.clear();
        BROKEN_LOOKS.clear();
        upload(loaded.images);
        models = Map.copyOf(loaded.models);
        problems = List.copyOf(loaded.problems);
        reloadedAt = System.currentTimeMillis();
        KitAnimations.forgetModels();
        KitTextures.forget();
    }

    private static KitModel read(final ResourceManager manager, final ResourceLocation file,
                                 final ModelDefinition definition, final Map<ResourceLocation, NativeImage> images) {
        return read(manager, file, file, definition, images);
    }

    /* A model, its file read, its shape worked out, its textures found, its variants' files read too. */
    private static KitModel read(final ResourceManager manager, final ResourceLocation id, final ResourceLocation file,
                                 final ModelDefinition definition, final Map<ResourceLocation, NativeImage> images) {
        final ModelData data = readFile(manager, file);
        final BakedGeometry geometry = GeometryBaker.bake(data, definition.glow());
        final List<ModelAnimation> animations = new ArrayList<>(data.animations());
        for (final String animationFile : definition.animations()) {
            final ResourceLocation location = ResourceLocation.parse(animationFile);
            animations.addAll(ModelReaders.animations(animationFile, bytes(manager, location)));
        }
        final List<ResourceLocation> textures = textures(manager, id, file, definition, data, images);
        final Map<String, KitModel> variants = new HashMap<>();
        for (final ModelDefinition.Variant variant : definition.variants()) {
            if (!variant.model().isEmpty() && !variants.containsKey(variant.model())) {
                final ResourceLocation variantFile = ResourceLocation.parse(variant.model());
                final ModelData variantData = readFile(manager, variantFile);
                final BakedGeometry variantGeometry = GeometryBaker.bake(variantData, definition.glow());
                variants.put(variant.model(), new KitModel(id, definition, variantData, variantGeometry, animations,
                        textures, Map.of(), variantFile, KitModel.restBounds(variantData, variantGeometry)));
            }
        }
        return new KitModel(id, definition, data, geometry, animations, textures, variants, file,
                KitModel.restBounds(data, geometry));
    }

    /* The model a file holds: the first of a geometry file that holds several. */
    private static ModelData readFile(final ResourceManager manager, final ResourceLocation file) {
        final String folder = folderOf(file.getPath());
        return ModelReaders.read(file.getPath(), bytes(manager, file), beside -> {
            final Optional<Resource> found = manager.getResource(ResourceLocation.fromNamespaceAndPath(
                    file.getNamespace(), resolve(folder, beside)));
            return found.map(resource -> read(resource, file)).orElse(null);
        }).getFirst();
    }

    /*
     * The texture of each of a model's pictures: those its definition names; else those its file carries, made
     * textures of their own, or names beside it; and the game's missing texture for any it cannot find.
     */
    private static List<ResourceLocation> textures(final ResourceManager manager, final ResourceLocation id,
                                                   final ResourceLocation file, final ModelDefinition definition,
                                                   final ModelData data,
                                                   final Map<ResourceLocation, NativeImage> images) {
        final List<ResourceLocation> out = new ArrayList<>();
        for (final String texture : definition.textures()) {
            out.add(ResourceLocation.parse(texture));
        }
        if (!out.isEmpty()) {
            return out;
        }
        final String folder = folderOf(file.getPath());
        int index = 0;
        for (final ModelTexture texture : data.textures()) {
            if (texture.embedded()) {
                final ResourceLocation made = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "kit/"
                        + id.getNamespace() + "/" + id.getPath().replace('.', '_') + "/" + index);
                try {
                    images.put(made, NativeImage.read(texture.bytes()));
                    out.add(made);
                } catch (final IOException broken) {
                    LOGGER.warn("A picture {} of the model {} does not read", texture.name(), id, broken);
                    out.add(MissingTextureAtlasSprite.getLocation());
                }
            } else {
                out.add(besideTexture(manager, file.getNamespace(), folder, texture.file()));
            }
            index++;
        }
        if (out.isEmpty()) {
            out.add(MissingTextureAtlasSprite.getLocation());
        }
        return out;
    }

    /* A texture a file names beside it, in the textures folder by its name, or the missing one. */
    private static ResourceLocation besideTexture(final ResourceManager manager, final String namespace,
                                                  final String folder, final String named) {
        if (named.isEmpty()) {
            return MissingTextureAtlasSprite.getLocation();
        }
        final ResourceLocation beside = ResourceLocation.fromNamespaceAndPath(namespace, resolve(folder, named));
        if (manager.getResource(beside).isPresent()) {
            return beside;
        }
        final String written = named.replace('\\', '/');
        final int textures = written.lastIndexOf(TEXTURES);
        if (textures >= 0) {
            final ResourceLocation inTextures = ResourceLocation.fromNamespaceAndPath(namespace,
                    written.substring(textures));
            if (manager.getResource(inTextures).isPresent()) {
                return inTextures;
            }
        }
        final String bare = written.substring(written.lastIndexOf('/') + 1);
        final ResourceLocation byName = ResourceLocation.fromNamespaceAndPath(namespace, TEXTURES + "block/" + bare
                + (bare.endsWith(PNG) ? "" : PNG));
        return manager.getResource(byName).isPresent() ? byName : MissingTextureAtlasSprite.getLocation();
    }

    private static void upload(final Map<ResourceLocation, NativeImage> images) {
        images.forEach((location, image) -> {
            Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
            MADE.add(location);
        });
    }

    private static byte[] bytes(final ResourceManager manager, final ResourceLocation file) {
        final Resource resource = manager.getResource(file).orElseThrow(() -> new ModelFormatException(
                "the file " + file + " is not there"));
        return read(resource, file);
    }

    private static byte[] read(final Resource resource, final ResourceLocation file) {
        try (InputStream in = resource.open()) {
            return in.readAllBytes();
        } catch (final IOException broken) {
            throw new ModelFormatException("the file " + file + " could not be read", broken);
        }
    }

    /* The model a definition file is of: its path under the folder, its ending taken off. */
    private static ResourceLocation idOf(final ResourceLocation definition) {
        final String path = definition.getPath();
        return ResourceLocation.fromNamespaceAndPath(definition.getNamespace(), path.substring(
                DeclaredModel.FOLDER.length() + 1, path.length() - DeclaredModel.DEFINITION_ENDING.length()));
    }

    private static String folderOf(final String path) {
        final int slash = path.lastIndexOf('/');
        return slash < 0 ? "" : path.substring(0, slash + 1);
    }

    /* A name a file writes beside it, made a path from the namespace's root: its dots and its slashes walked. */
    static String resolve(final String folder, final String named) {
        final Deque<String> parts = new ArrayDeque<>();
        for (final String part : (folder + named.replace('\\', '/')).split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                parts.pollLast();
            } else {
                parts.addLast(part);
            }
        }
        return String.join("/", parts);
    }

    /** What a load read: the models, the pictures to make textures of, and what did not read. */
    record Loaded(Map<ResourceLocation, KitModel> models, Map<ResourceLocation, NativeImage> images,
                  List<String> problems) {
    }
}
