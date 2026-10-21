/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.module;

import dev.jstech.core.tool.MiningShape;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * What the modules working in a tool make of it, told to each module's effects in turn: the attributes it changes
 * for the hand that holds it, the area or vein it mines, and the enchantments it works as though it had. The largest
 * area or vein a module gives wins, and of one enchantment the highest level.
 */
public final class ModuleTool {

    private final List<Change> changes = new ArrayList<>();
    private final Map<ResourceKey<Enchantment>, Integer> enchantments = new LinkedHashMap<>();
    private Optional<MiningShape> mining = Optional.empty();
    /* The socket whose module is being told, which names the changes it makes. */
    private String socket = "";
    private int count;

    ModuleTool() {
    }

    /** Changes the holder's {@code attribute} by {@code amount}, as {@code operation} adds it, while it is held. */
    public void attribute(final Holder<Attribute> attribute, final double amount,
                          final AttributeModifier.Operation operation) {
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Modules.NAMESPACE,
                "tool_" + this.socket + "_" + this.count++);
        this.changes.add(new Change(attribute, new AttributeModifier(id, amount, operation)));
    }

    /** Mines {@code shape} besides the block broken, unless another module gives a larger one. */
    public void mines(final MiningShape shape) {
        if (this.mining.isEmpty() || shape.most() > this.mining.get().most()) {
            this.mining = Optional.of(shape);
        }
    }

    /** Works as though enchanted with {@code enchantment} at {@code level}, unless already at a higher one. */
    public void enchant(final ResourceKey<Enchantment> enchantment, final int level) {
        this.enchantments.merge(enchantment, level, Math::max);
    }

    /** The changes to the holder's attributes. */
    public List<Change> changes() {
        return Collections.unmodifiableList(this.changes);
    }

    /** The area or vein it mines besides the block broken, or none. */
    public Optional<MiningShape> mining() {
        return this.mining;
    }

    /** The enchantments it works as though it had, and their levels. */
    public Map<ResourceKey<Enchantment>, Integer> enchantments() {
        return Collections.unmodifiableMap(this.enchantments);
    }

    /* The module of socket {@code next} is told from here on. */
    void socket(final String next) {
        this.socket = next;
        this.count = 0;
    }

    /**
     * One change to an attribute of the hand holding the tool.
     *
     * @param attribute the attribute
     * @param modifier  the change
     */
    public record Change(Holder<Attribute> attribute, AttributeModifier modifier) {
    }
}
