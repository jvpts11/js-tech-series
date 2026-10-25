/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.content;

import dev.jstech.core.item.ItemMode;
import dev.jstech.core.item.ItemState;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperty;
import dev.jstech.core.tool.ToolSpec;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.DoubleToIntFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.jetbrains.annotations.Nullable;

/**
 * Everything about one item that is not a block's, said once: what makes it, what it is called, how it looks, which
 * tab shows it, the components it starts with, and what it holds besides itself (its modes, energy, a fluid, stacks).
 * {@link #register()} registers it and keeps the declaration for the generator and the checks; the Core gives an
 * item that holds something the game's capability for it, keeps what it holds in its components and shows it in its
 * tooltip.
 * @param <I> the item's class
 */
public final class ItemBuilder<I extends Item> {

    private final ModContent content;
    private final String id;
    private final Function<Item.Properties, ? extends I> factory;
    private final List<ItemEntry.Default<?>> defaults = new ArrayList<>();
    private final List<TagKey<Item>> tags = new ArrayList<>();
    private @Nullable String english;
    private IItemLook look = IItemLook.FLAT;
    private ContentTab.@Nullable Section section;
    private ItemState state = ItemState.NOTHING;
    private int durability;
    private Optional<ToolSpec> tool = Optional.empty();
    /* How hard a blow of it harms and how many it gives a second, as its tooltip shows them; none for no weapon. */
    private double damage;
    private double speed;

    /*
     * The game's own names of a tool's harm and swing, and what a bare hand has of each: a blow of it is shown and
     * counted as an axe's is, the hand's own counted in.
     */
    private static final ResourceLocation TOOL_DAMAGE = ResourceLocation.withDefaultNamespace("base_attack_damage");
    private static final ResourceLocation TOOL_SPEED = ResourceLocation.withDefaultNamespace("base_attack_speed");
    private static final double HAND_DAMAGE = 1;
    private static final double HAND_SPEED = 4;

    ItemBuilder(final ModContent content, final String id, final Function<Item.Properties, ? extends I> factory) {
        this.content = content;
        this.id = id;
        this.factory = factory;
    }

    /** What the item is called in English. */
    public ItemBuilder<I> named(final String name) {
        this.english = name;
        return this;
    }

    /** How the item looks; without this it is a flat sprite of its own texture. */
    public ItemBuilder<I> look(final IItemLook itemLook) {
        this.look = itemLook;
        return this;
    }

    /** The section of a tab it is shown in. */
    public ItemBuilder<I> tab(final ContentTab.Section inSection) {
        this.section = inSection;
        return this;
    }

    /** An item tag it is in, besides those of its tab: a common tag other mods know it by, as c:tools/wrench. */
    public ItemBuilder<I> tag(final TagKey<Item> in) {
        this.tags.add(Objects.requireNonNull(in, "in"));
        return this;
    }

    /** A component every new one of it carries, and its value. */
    public <T> ItemBuilder<I> component(final Supplier<? extends DataComponentType<T>> type, final T value) {
        this.defaults.add(new ItemEntry.Default<>(Objects.requireNonNull(type, "type"),
                Objects.requireNonNull(value, "value")));
        return this;
    }

    /** The modes it switches between with the mode key, in the order the key goes through them; the first to start. */
    public ItemBuilder<I> modes(final ItemMode... modes) {
        if (modes.length < 2) {
            throw new IllegalArgumentException("an item with modes has two or more");
        }
        this.state = this.state.withModes(List.of(modes));
        return this;
    }

    /** It holds up to {@code capacity} FE, taking {@code in} and giving {@code out} at a time. */
    public ItemBuilder<I> holdsEnergy(final long capacity, final int in, final int out) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("an item that holds energy holds some");
        }
        this.state = this.state.withEnergy(capacity, in, out);
        return this;
    }

    /** It holds up to {@code capacity} millibuckets of one fluid. */
    public ItemBuilder<I> holdsFluid(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("an item that holds a fluid holds some");
        }
        this.state = this.state.withFluid(capacity);
        return this;
    }

    /** It holds {@code slots} stacks, up to {@value ItemState#MOST_SLOTS}. */
    public ItemBuilder<I> holdsItems(final int slots) {
        if (slots <= 0) {
            throw new IllegalArgumentException("an item that holds stacks holds one or more");
        }
        this.state = this.state.withSlots(slots);
        return this;
    }

    /**
     * It has a gas tank of {@code volume} millibuckets at {@code pressure} times the pressure of the open air, which
     * holds their product of one gas at a time, counted at the open air's pressure.
     */
    public ItemBuilder<I> holdsGas(final long volume, final long pressure) {
        if (volume <= 0 || pressure < 1) {
            throw new IllegalArgumentException("an item that holds a gas has a volume and a pressure");
        }
        this.state = this.state.withGas(volume, pressure);
        return this;
    }

    /** It wears out after {@code uses} uses, as the game counts a tool's; it stacks alone. */
    public ItemBuilder<I> durability(final int uses) {
        if (uses <= 0) {
            throw new IllegalArgumentException("an item that wears out lasts one use or more, not " + uses);
        }
        this.durability = uses;
        return this;
    }

    /**
     * It wears out after the uses {@code rule} makes of {@code material}'s value of {@code property}, as its mod
     * chooses: a mod's own hardness or toughness of its metals read into how long its tools last. The material's
     * declared value is read once, here, since the game fixes how long an item lasts as it starts.
     *
     * @throws DeclarationError when the material has no value of the property
     */
    public ItemBuilder<I> durabilityFrom(final Material material, final MaterialProperty<? extends Number> property,
                                         final DoubleToIntFunction rule) {
        final Optional<? extends Number> value = material.value(property);
        if (value.isEmpty()) {
            throw DeclarationError.of(content.modid(), "the item " + content.modid() + ":" + id,
                    "lasts by the " + property.id() + " of " + material.id() + ", which has none",
                    "give the material that property where it is declared, or use durability(uses)");
        }
        return durability(rule.applyAsInt(value.get().doubleValue()));
    }

    /**
     * It strikes as a weapon does, held in the main hand: {@code damage} points of harm a blow and {@code speed}
     * blows a second, as its tooltip shows them, a bare hand's own counted in, as an axe's 9 and 0.9.
     */
    public ItemBuilder<I> strikes(final double damage, final double speed) {
        if (damage <= 0 || speed <= 0) {
            throw new IllegalArgumentException("an item that strikes harms and swings, not " + damage + " at "
                    + speed);
        }
        this.damage = damage;
        this.speed = speed;
        return this;
    }

    /** What it does as a tool: its areas, what it builds, what it works on, its heat, its wait and its charge. */
    public ItemBuilder<I> tool(final ToolSpec spec) {
        this.tool = Optional.of(spec);
        return this;
    }

    /**
     * Registers the item and keeps what was declared. An item that keeps anything inside, or wears out, stacks alone.
     *
     * @throws DeclarationError when the item was given no name, or its tool asks for what it does not have
     */
    public ItemEntry<I> register() {
        if (english == null) {
            throw DeclarationError.of(content.modid(), "the item " + content.modid() + ":" + id, "has no name",
                    "give it its English name with named(...) before register()");
        }
        tool.ifPresent(this::check);
        final boolean alone = state.stacksAlone();
        final int uses = durability;
        final double harm = damage;
        final double swing = speed;
        final ItemEntry<I> entry = new ItemEntry<>(content.itemRegister().register(id,
                () -> factory.apply(properties(alone, uses, harm, swing))).getId(), english, look, state, defaults,
                tags, tool);
        if (section != null) {
            section.add(entry);
        }
        content.declare(entry);
        return entry;
    }

    /*
     * The properties an item is made with: alone in its stack, lasting {@code uses} when it wears out, and striking
     * {@code harm} a blow, {@code swing} blows a second, when it is a weapon.
     */
    private static Item.Properties properties(final boolean alone, final int uses, final double harm,
                                              final double swing) {
        final Item.Properties properties = new Item.Properties();
        if (harm > 0) {
            properties.attributes(ItemAttributeModifiers.builder()
                    .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(TOOL_DAMAGE, harm - HAND_DAMAGE,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .add(Attributes.ATTACK_SPEED, new AttributeModifier(TOOL_SPEED, swing - HAND_SPEED,
                            AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                    .build());
        }
        if (uses > 0) {
            return properties.durability(uses);
        }
        return alone ? properties.stacksTo(1) : properties;
    }

    /* A tool's modes are the item's, and what it works on is what the item holds. */
    private void check(final ToolSpec spec) {
        final List<String> modes = state.modes().stream().map(ItemMode::id).toList();
        for (final String mode : spec.mining().keySet()) {
            if (!modes.contains(mode)) {
                throw DeclarationError.of(content.modid(), "the item " + content.modid() + ":" + id,
                        "mines in a mode " + mode + " it does not have",
                        "declare the mode with modes(...), or mine in one it has");
            }
        }
        for (final String mode : spec.building().keySet()) {
            if (!modes.contains(mode)) {
                throw DeclarationError.of(content.modid(), "the item " + content.modid() + ":" + id,
                        "builds in a mode " + mode + " it does not have",
                        "declare the mode with modes(...), or build in one it has");
            }
        }
        if (spec.power().isPresent() && !spec.power().get().heldBy(state)) {
            throw DeclarationError.of(content.modid(), "the item " + content.modid() + ":" + id,
                    "works on what it does not hold",
                    "declare what it holds (holdsEnergy, holdsFluid or holdsGas) before tool(...)");
        }
    }
}
