/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.variant;

import dev.jstech.core.config.ConfigFiles;
import dev.jstech.core.config.IConfigValues;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.material.VariantSettings;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

/**
 * Variants for the woods, stones and metals every installed mod brings, in the spirit of Every Compat: each mod
 * declares its kinds ({@link VariantKind}), and the Core makes one of each kind for every material of its family
 * found through the common tags ({@link FoundMaterials}), as the game registers its items, once every mod's blocks
 * are. The settings file {@code jstech-variants.toml} turns them off, or leaves out mods, materials and kinds, so a
 * game with many mods is not flooded. Microblocks need no variants: they are cut from any whole block of any mod.
 */
public final class Variants {

    private static final Logger LOGGER = CoreLogs.CONTENT.logger();
    private static final List<VariantKind> KINDS = new CopyOnWriteArrayList<>();
    private static volatile List<Variant> registered = List.of();
    private static volatile boolean closed;

    private Variants() {
    }

    /**
     * Starts declaring the kind {@code name} of the mod {@code modid}, made for every material of {@code family}, its
     * variants called {@code named} with the material's name where {@code %s} is ("%s Casing").
     */
    public static VariantKind.Builder kind(final String modid, final String name, final VariantFamily family,
                                           final TextKey named) {
        return new VariantKind.Builder(ResourceLocation.fromNamespaceAndPath(modid, name), family, named);
    }

    /** Every kind declared, in the order they were. */
    public static List<VariantKind> kinds() {
        return List.copyOf(KINDS);
    }

    /** Every variant the game registered, kind after kind; none before the game registers its items. */
    public static List<Variant> registered() {
        return registered;
    }

    /** The variants of {@code kind}, in the order the materials were found. */
    public static List<Variant> of(final VariantKind kind) {
        final List<Variant> out = new ArrayList<>();
        for (final Variant variant : registered) {
            if (variant.kind() == kind) {
                out.add(variant);
            }
        }
        return out;
    }

    /** The variant of {@code kind} made of the material {@code material}, when one was made. */
    public static Optional<Variant> of(final VariantKind kind, final ResourceLocation material) {
        for (final Variant variant : registered) {
            if (variant.kind() == kind && variant.material().id().equals(material)) {
                return Optional.of(variant);
            }
        }
        return Optional.empty();
    }

    /** Hands the mod's bus the registering of the variants, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterEvent.class, Variants::onRegister);
    }

    static void add(final VariantKind kind) {
        if (closed) {
            throw DeclarationError.of(kind.id().getNamespace(), "the kind of variant " + kind.id(),
                    "comes after the game registered its things", "declare kinds from the mod's constructor");
        }
        for (final VariantKind other : KINDS) {
            if (other.id().equals(kind.id())) {
                throw DeclarationError.of(kind.id().getNamespace(), "the kind of variant " + kind.id(),
                        "is declared twice", "give each kind its own name");
            }
        }
        KINDS.add(kind);
    }

    private static void onRegister(final RegisterEvent event) {
        /*
         * A variant copies its material's block, so it is made once every mod has registered its blocks: when the
         * game registers items, which comes after every mod's blocks.
         */
        event.register(Registries.ITEM, helper -> {
            closed = true;
            if (KINDS.isEmpty()) {
                return;
            }
            final List<FoundMaterial> found = FoundMaterials.all();
            final Map<String, FoundMaterial> byId = new HashMap<>();
            final List<VariantPlan.Found> plannable = new ArrayList<>();
            for (final FoundMaterial material : found) {
                byId.putIfAbsent(material.family() + " " + material.id(), material);
                plannable.add(new VariantPlan.Found(material.family(), material.id().toString(),
                        material.source().toString()));
            }
            final Map<String, VariantKind> kinds = new HashMap<>();
            final List<VariantPlan.Kind> declared = new ArrayList<>();
            for (final VariantKind kind : KINDS) {
                kinds.put(kind.id().toString(), kind);
                declared.add(new VariantPlan.Kind(kind.id().toString(), kind.family()));
            }
            final List<Variant> made = new ArrayList<>();
            for (final VariantPlan.Planned planned : VariantPlan.of(declared, plannable, skipped())) {
                final VariantKind kind = kinds.get(planned.kind());
                final FoundMaterial material = byId.get(kind.family() + " " + planned.material());
                final ResourceLocation id = ResourceLocation.parse(planned.id());
                if (BuiltInRegistries.BLOCK.containsKey(id)) {
                    continue;
                }
                final Text name = kind.nameOf(material);
                final Block block = kind.maker().make(BlockBehaviour.Properties.ofFullCopy(material.block()),
                        material, name);
                Registry.register(BuiltInRegistries.BLOCK, id, block);
                final Item item = new VariantBlockItem(block, new Item.Properties(), name);
                helper.register(id, item);
                made.add(new Variant(kind, material, id, block, item));
            }
            registered = Collections.unmodifiableList(made);
            LOGGER.info("Variants: {} made of {} kinds for {} woods, stones and metals found by their tags",
                    made.size(), KINDS.size(), found.size());
        });
    }

    /* What the settings leave out, read from the file on disk, since the game reads its settings later. */
    private static VariantPlan.Skipped skipped() {
        final IConfigValues settings = ConfigFiles.atStartup(VariantSettings.FILE);
        return new VariantPlan.Skipped(settings.get(VariantSettings.VARIANTS), Set.copyOf(settings.get(
                VariantSettings.VARIANTS_SKIPPED_MODS)), Set.copyOf(settings.get(
                VariantSettings.VARIANTS_SKIPPED_MATERIALS)), Set.copyOf(settings.get(
                VariantSettings.VARIANTS_SKIPPED_KINDS)));
    }
}
