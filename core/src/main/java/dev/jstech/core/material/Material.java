/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.material;

import dev.jstech.core.config.ConfigColours;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * What the world is made of: an element, an alloy, a mineral, a compound, a liquid, a gas, an exotic form of matter.
 * A material has a kind, which says the forms it can take; the state it is in at room temperature and the others it
 * may take (a molten metal is a liquid); its colour, which its things are drawn in; and the real data it carries, its
 * properties. It has no tier: when it becomes available is for each mod's recipes to say. Declared once, with
 * {@link Materials}.
 */
public final class Material {

    private final ResourceLocation id;
    private final MaterialKind kind;
    private final TextKey name;
    private final int colour;
    private final Set<MatterState> states;
    private final Map<MaterialProperty<?>, Object> values;
    private final Map<MaterialProperty<?>, MaterialProperty.Shown> shown;
    private final Map<Form, Supplier<? extends ItemLike>> provided;
    private final Map<HostRock, Supplier<? extends Block>> providedOres;
    private final OreLook oreLook;

    /* A material that gives no colour is drawn in a plain grey. */
    private static final int GREY = 0x9A9A9A;

    private Material(final Builder builder) {
        this.id = builder.id;
        this.kind = builder.kind;
        this.name = builder.name;
        this.colour = builder.colour;
        final Set<MatterState> all = EnumSet.of(builder.kind.roomState());
        all.addAll(builder.states);
        this.states = Collections.unmodifiableSet(all);
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(builder.values));
        this.shown = Collections.unmodifiableMap(new LinkedHashMap<>(builder.shown));
        this.provided = Collections.unmodifiableMap(new LinkedHashMap<>(builder.provided));
        this.providedOres = Collections.unmodifiableMap(new LinkedHashMap<>(builder.providedOres));
        this.oreLook = builder.oreLook;
    }

    public ResourceLocation id() {
        return this.id;
    }

    public MaterialKind kind() {
        return this.kind;
    }

    /** What it is called: "Iron". */
    public Text name() {
        return this.name.text();
    }

    /** The key of its name, as it was declared. */
    public TextKey nameKey() {
        return this.name;
    }

    /** Its colour, as {@code 0xRRGGBB}: what its things are drawn in. */
    public int colour() {
        return this.colour;
    }

    /** The state it is in at room temperature. */
    public MatterState roomState() {
        return this.kind.roomState();
    }

    /** Every state it can take, the one at room temperature among them. */
    public Set<MatterState> states() {
        return this.states;
    }

    /** Whether it can take {@code state}. */
    public boolean takes(final MatterState state) {
        return this.states.contains(state);
    }

    /** Whether it can take {@code form}: whether the form makes sense for its kind. */
    public boolean fits(final Form form) {
        return form.fits(this.kind);
    }

    /**
     * The value its declaration gives {@code property}, or none. A data pack may give another, which
     * {@link MaterialValues} reads over this one.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> value(final MaterialProperty<T> property) {
        return Optional.ofNullable((T) this.values.get(property));
    }

    /** Every value its declaration gives, by property, in the order they were given. */
    public Map<MaterialProperty<?>, Object> values() {
        return this.values;
    }

    /** When {@code property}'s line shows in its tooltip: as it says here, or as the property says. */
    public MaterialProperty.Shown shown(final MaterialProperty<?> property) {
        return this.shown.getOrDefault(property, property.shown());
    }

    /** The thing another mod already has for {@code form}, the game's iron ingot for iron's ingot, or none. */
    public Optional<Supplier<? extends ItemLike>> provided(final Form form) {
        return Optional.ofNullable(this.provided.get(form));
    }

    /** The forms another mod already has a thing for. */
    public Set<Form> providedForms() {
        return this.provided.keySet();
    }

    /** The ore another mod already has in {@code rock}, the game's deepslate iron ore for iron, or none. */
    public Optional<Supplier<? extends Block>> providedOre(final HostRock rock) {
        return Optional.ofNullable(this.providedOres.get(rock));
    }

    /** The rocks another mod already has its ore in. */
    public Set<HostRock> providedOreRocks() {
        return this.providedOres.keySet();
    }

    /** How its ores look. */
    public OreLook oreLook() {
        return this.oreLook;
    }

    /**
     * The form its ores drop: its raw form for a metal or a mineral, the gem for a gem, the crystal for a crystal, the
     * dust for any other solid.
     */
    public Form oreDrop() {
        return switch (this.kind) {
            case GEM -> Forms.GEM;
            case CRYSTAL -> Forms.CRYSTAL;
            case NONMETAL_SOLID -> Forms.DUST;
            default -> Forms.RAW;
        };
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    /** How a material is declared. */
    public static final class Builder {

        private final ResourceLocation id;
        private final MaterialKind kind;
        private final TextKey name;
        private final Set<MatterState> states = EnumSet.noneOf(MatterState.class);
        private final Map<MaterialProperty<?>, Object> values = new LinkedHashMap<>();
        private final Map<MaterialProperty<?>, MaterialProperty.Shown> shown = new LinkedHashMap<>();
        private final Map<Form, Supplier<? extends ItemLike>> provided = new LinkedHashMap<>();
        private final Map<HostRock, Supplier<? extends Block>> providedOres = new LinkedHashMap<>();
        private int colour = GREY;
        private OreLook oreLook = OreLook.PLAIN;

        Builder(final ResourceLocation id, final MaterialKind kind, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.kind = Objects.requireNonNull(kind, "kind");
            this.name = Objects.requireNonNull(name, "name");
            // Its kind says its state at room temperature, and every material has one.
            this.values.put(MaterialProperties.ROOM_STATE, kind.roomState());
        }

        /** Its colour, as {@code 0xRRGGBB}. */
        public Builder colour(final int rgb) {
            this.colour = rgb & 0xFFFFFF;
            this.values.put(MaterialProperties.COLOUR, ConfigColours.write(this.colour));
            return this;
        }

        /** A state it can take besides the one at room temperature: a metal molten, water as steam. */
        public Builder also(final MatterState... more) {
            this.states.addAll(Set.of(more));
            return this;
        }

        /** The value of {@code property}: its symbol, its atomic number. */
        public <T> Builder with(final MaterialProperty<T> property, final T value) {
            this.values.put(Objects.requireNonNull(property, "property"), Objects.requireNonNull(value, "value"));
            return this;
        }

        /** When {@code property}'s line shows in its tooltip, other than the property says. */
        public Builder showing(final MaterialProperty<?> property, final MaterialProperty.Shown when) {
            this.shown.put(Objects.requireNonNull(property, "property"), Objects.requireNonNull(when, "when"));
            return this;
        }

        /**
         * The thing another mod already has for {@code form}, which uses of that form get instead of a thing of the
         * Core's: the game's iron ingot for iron.
         */
        public Builder provided(final Form form, final Supplier<? extends ItemLike> thing) {
            this.provided.put(Objects.requireNonNull(form, "form"), Objects.requireNonNull(thing, "thing"));
            return this;
        }

        /** The ore another mod already has in {@code rock}: the game's iron ore in stone, for iron. */
        public Builder providedOre(final HostRock rock, final Supplier<? extends Block> ore) {
            this.providedOres.put(Objects.requireNonNull(rock, "rock"), Objects.requireNonNull(ore, "ore"));
            return this;
        }

        /** How its ores look, when not as the game's iron ore does. */
        public Builder ore(final OreLook look) {
            this.oreLook = Objects.requireNonNull(look, "look");
            return this;
        }

        /**
         * Declares the material.
         *
         * @throws DeclarationError when a material of that id is declared already, or it is given a thing for a form
         *                          its kind cannot take
         */
        public Material declare() {
            for (final Form form : this.provided.keySet()) {
                if (!form.fits(this.kind)) {
                    throw DeclarationError.of(this.id.getNamespace(), "the material " + this.id,
                            "is given a thing for the form " + form + ", which a " + this.kind.serializedName()
                                    + " cannot take",
                            "give things only for the forms its kind takes");
                }
            }
            return Materials.declare(new Material(this));
        }
    }
}
