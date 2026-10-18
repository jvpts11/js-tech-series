/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.resource;

import com.google.gson.JsonObject;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.ApiStatus;

/**
 * Packs a mod makes as the game loads: resources for the player's game, or data for the server, whose files the mod
 * makes from what the installed mods declared ({@link IRuntimeFiles}). Each pack is always on, below every pack a
 * player or a server adds, so a resource pack or a data pack can still replace any of its files. Declared once, from
 * the mod's constructor:
 *
 * <pre>{@code
 * RuntimePacks.declare(rl("generated"), PackType.CLIENT_RESOURCES, GENERATED_TITLE, MyArt::files);
 * }</pre>
 */
public final class RuntimePacks {

    private static final List<Declared> DECLARED = new CopyOnWriteArrayList<>();
    /* The textures packs of the player's game promise to make, said on both sides so the server's data can know. */
    private static final Set<ResourceLocation> PROMISED = ConcurrentHashMap.newKeySet();

    private RuntimePacks() {
    }

    /**
     * Says the texture {@code texture} ({@code mymod:block/charred_log_side}) is made as the player's game loads, by
     * a pack declared there: a model may name it, and the check of the mod's generated data finds it. Said from the
     * mod's common code, which the data generation runs.
     */
    public static void promise(final ResourceLocation texture) {
        PROMISED.add(Objects.requireNonNull(texture, "texture"));
    }

    /** Whether a pack promised to make the texture {@code texture} as the player's game loads. */
    public static boolean promised(final ResourceLocation texture) {
        return PROMISED.contains(texture);
    }

    /**
     * Declares the pack {@code id} of {@code type}, called {@code title}, its files made by {@code files}.
     *
     * @throws DeclarationError when a pack of that id and type is declared already
     */
    public static void declare(final ResourceLocation id, final PackType type, final TextKey title,
                               final IRuntimeFiles files) {
        final Declared pack = new Declared(Objects.requireNonNull(id, "id"), Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(title, "title"), Objects.requireNonNull(files, "files"));
        for (final Declared before : DECLARED) {
            if (before.id().equals(id) && before.type() == type) {
                throw DeclarationError.of(id.getNamespace(), "the generated pack " + id, "is declared twice",
                        "declare it once, from the mod's constructor");
            }
        }
        DECLARED.add(pack);
    }

    /** Opens the pack {@code id} of {@code type} as the game would, for a test to read its files. */
    public static Optional<PackResources> open(final ResourceLocation id, final PackType type) {
        for (final Declared pack : DECLARED) {
            if (pack.id().equals(id) && pack.type() == type) {
                return Optional.of(pack.open(pack.location()));
            }
        }
        return Optional.empty();
    }

    /** Hands the mod's bus the adding of the packs, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(AddPackFindersEvent.class, RuntimePacks::onAddPackFinders);
    }

    private static void onAddPackFinders(final AddPackFindersEvent event) {
        for (final Declared pack : DECLARED) {
            if (pack.type() != event.getPackType()) {
                continue;
            }
            event.addRepositorySource(packs -> {
                final Pack found = Pack.readMetaAndCreate(pack.location(), new Pack.ResourcesSupplier() {
                    @Override
                    public PackResources openPrimary(final PackLocationInfo location) {
                        return pack.open(location);
                    }

                    @Override
                    public PackResources openFull(final PackLocationInfo location, final Pack.Metadata metadata) {
                        return pack.open(location);
                    }
                }, pack.type(), new PackSelectionConfig(true, Pack.Position.BOTTOM, false));
                if (found != null) {
                    packs.accept(found);
                }
            });
        }
    }

    /* One declared pack. */
    private record Declared(ResourceLocation id, PackType type, TextKey title, IRuntimeFiles files) {

        private PackLocationInfo location() {
            return new PackLocationInfo(this.id.toString(), Component.translatableWithFallback(this.title.key(),
                    this.title.english()), PackSource.BUILT_IN, Optional.empty());
        }

        private RuntimePackResources open(final PackLocationInfo location) {
            return new RuntimePackResources(location, this.type, metadata(), this.files.files());
        }

        private byte[] metadata() {
            final JsonObject description = new JsonObject();
            description.addProperty("translate", this.title.key());
            description.addProperty("fallback", this.title.english());
            final JsonObject pack = new JsonObject();
            pack.add("description", description);
            pack.addProperty("pack_format", SharedConstants.getCurrentVersion().getPackVersion(this.type));
            final JsonObject root = new JsonObject();
            root.add("pack", pack);
            return root.toString().getBytes(StandardCharsets.UTF_8);
        }
    }
}
