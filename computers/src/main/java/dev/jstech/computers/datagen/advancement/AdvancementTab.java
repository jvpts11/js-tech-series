/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen.advancement;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.advancement.JscEventTrigger;
import dev.jstech.computers.advancement.OsFirstBootTrigger;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One tab of the mod's advancements, written down once and read twice: by the advancement provider, which builds
 * them, and by the language provider, which takes their words. Keeping both in one place is what stops an
 * advancement from shipping without a title.
 *
 * <p>A subclass lists its advancements in its constructor, the root first and every parent before its children.
 * Criteria keep the order they are listed in, so the generated files come out the same on every run.
 */
public abstract class AdvancementTab implements AdvancementProvider.AdvancementGenerator {

    private final String tab;
    private final ResourceLocation background;
    private final List<AdvancementSpec> specs = new ArrayList<>();

    private static final String ROOT = "root";
    private static final String DONE = "done";

    protected AdvancementTab(final String tab, final ResourceLocation background) {
        this.tab = tab;
        this.background = background;
    }

    @Override
    public void generate(final HolderLookup.Provider registries, final Consumer<AdvancementHolder> saver,
                         final ExistingFileHelper existingFiles) {
        for (final AdvancementSpec spec : this.specs) {
            if (spec.requiredMod() == null) {
                this.builder(spec).save(saver, this.id(spec.name()), existingFiles);
            }
        }
    }

    /** Builds the advancements that exist only beside another mod, each handed over with the mod it needs. */
    public void generateConditional(final BiConsumer<AdvancementHolder, String> out) {
        for (final AdvancementSpec spec : this.specs) {
            if (spec.requiredMod() != null) {
                out.accept(this.builder(spec).build(this.id(spec.name())), spec.requiredMod());
            }
        }
    }

    /** Hands over every title and description of the tab under its translation key. */
    public void translations(final BiConsumer<String, String> add) {
        for (final AdvancementSpec spec : this.specs) {
            add.accept(this.key(spec.name(), "title"), spec.title());
            add.accept(this.key(spec.name(), "description"), spec.description());
        }
    }

    /** Every advancement of the tab, in the order they were written down. */
    public List<AdvancementSpec> specs() {
        return List.copyOf(this.specs);
    }

    /** The tab's id space, such as {@code hardware}: every advancement of it is {@code jsc:<tab>/<name>}. */
    public String tab() {
        return this.tab;
    }

    /** Earned by the event, whatever its detail. */
    protected static Supplier<Criterion<?>> on(final String event) {
        return () -> JscEventTrigger.on(event);
    }

    /** Earned by the event with exactly that detail. */
    protected static Supplier<Criterion<?>> on(final String event, final String detail) {
        return () -> JscEventTrigger.on(event, detail);
    }

    /** Earned the first time the named system of this mod comes up in front of somebody. */
    protected static Supplier<Criterion<?>> booted(final String system) {
        return () -> OsFirstBootTrigger.booted(Optional.of(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                system)));
    }

    /** Earned the first time any system comes up in front of somebody. */
    protected static Supplier<Criterion<?>> bootedAny() {
        return () -> OsFirstBootTrigger.booted(Optional.empty());
    }

    /** The tab's root, the one every other hangs from, whose background the whole tab shows. */
    protected final void root(final ItemLike icon, final String title, final String description,
                              final Supplier<Criterion<?>> criterion) {
        this.one(ROOT, null, icon, AdvancementType.TASK, false, title, description, criterion, null);
    }

    protected final void task(final String name, final String parent, final ItemLike icon, final String title,
                              final String description, final Supplier<Criterion<?>> criterion) {
        this.one(name, parent, icon, AdvancementType.TASK, false, title, description, criterion, null);
    }

    protected final void goal(final String name, final String parent, final ItemLike icon, final String title,
                              final String description, final Supplier<Criterion<?>> criterion) {
        this.one(name, parent, icon, AdvancementType.GOAL, false, title, description, criterion, null);
    }

    protected final void challenge(final String name, final String parent, final ItemLike icon, final String title,
                                   final String description, final Supplier<Criterion<?>> criterion) {
        this.one(name, parent, icon, AdvancementType.CHALLENGE, false, title, description, criterion, null);
    }

    /** A task nobody sees in the tab until they have earned it. */
    protected final void secret(final String name, final String parent, final ItemLike icon, final String title,
                                final String description, final Supplier<Criterion<?>> criterion) {
        this.one(name, parent, icon, AdvancementType.TASK, true, title, description, criterion, null);
    }

    /** A goal that exists only when {@code mod} is present beside this one. */
    protected final void goalWith(final String mod, final String name, final String parent, final ItemLike icon,
                                  final String title, final String description,
                                  final Supplier<Criterion<?>> criterion) {
        this.one(name, parent, icon, AdvancementType.GOAL, false, title, description, criterion, mod);
    }

    /** A challenge asking for every one of several things, each its own criterion, in the order given. */
    protected final void challengeOfAll(final String name, final String parent, final ItemLike icon,
                                        final String title, final String description,
                                        final Map<String, Supplier<Criterion<?>>> criteria) {
        this.specs.add(new AdvancementSpec(name, parent, icon, AdvancementType.CHALLENGE, false, title, description,
                new LinkedHashMap<>(criteria), null));
    }

    private void one(final String name, final String parent, final ItemLike icon, final AdvancementType frame,
                     final boolean hidden, final String title, final String description,
                     final Supplier<Criterion<?>> criterion, final String requiredMod) {
        this.specs.add(new AdvancementSpec(name, parent, icon, frame, hidden, title, description,
                Map.of(DONE, criterion), requiredMod));
    }

    private Advancement.Builder builder(final AdvancementSpec spec) {
        final Advancement.Builder builder = Advancement.Builder.advancement();
        if (spec.parent() != null) {
            builder.parent(this.id(spec.parent()));
        }
        builder.display(new ItemStack(spec.icon()),
                Component.translatable(this.key(spec.name(), "title")),
                Component.translatable(this.key(spec.name(), "description")),
                spec.parent() == null ? this.background : null,
                spec.frame(), true, true, spec.hidden());
        spec.criteria().forEach((name, criterion) -> builder.addCriterion(name, criterion.get()));
        return builder;
    }

    private ResourceLocation id(final String name) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, this.tab + "/" + name);
    }

    private String key(final String name, final String part) {
        return "advancements." + JsComputers.MODID + "." + this.tab + "." + name + "." + part;
    }
}
