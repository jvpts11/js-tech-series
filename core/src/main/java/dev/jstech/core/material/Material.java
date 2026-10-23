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
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

/**
 * What the world is made of: an element, an isotope, an alloy, a mineral, a compound, a liquid, a gas, an exotic form
 * of matter. A material has a kind, which says the forms it can take; the state it is in at room temperature and the
 * others it may take (a molten metal is a liquid); its colour, which its things are drawn in; and the real data it
 * carries, its properties. It has no tier: when it becomes available is for each mod's recipes to say. Declared once,
 * with {@link Materials}.
 */
public final class Material {

    private final ResourceLocation id;
    private final MaterialKind kind;
    private final Text name;
    private final @Nullable TextKey nameKey;
    private final int colour;
    private final MatterState roomState;
    private final Set<MatterState> states;
    private final Map<MaterialProperty<?>, Object> values;
    private final Map<MaterialProperty<?>, MaterialProperty.Shown> shown;
    private final Map<Form, Supplier<? extends ItemLike>> provided;
    private final Map<HostRock, Supplier<? extends Block>> providedOres;
    private final OreLook oreLook;
    private final @Nullable Isotope isotope;
    private final boolean nameable;
    private final @Nullable IMaterialNamer namer;
    private final @Nullable Mineral mineral;

    /* A material that gives no colour is drawn in a plain grey. */
    private static final int GREY = 0x9A9A9A;

    private Material(final Builder builder) {
        this.id = builder.id;
        this.kind = builder.kind;
        this.name = builder.name;
        this.nameKey = builder.nameKey;
        this.colour = builder.colour;
        this.roomState = builder.roomState;
        final Set<MatterState> all = EnumSet.of(builder.roomState);
        all.addAll(builder.states);
        this.states = Collections.unmodifiableSet(all);
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(builder.values));
        this.shown = Collections.unmodifiableMap(new LinkedHashMap<>(builder.shown));
        this.provided = Collections.unmodifiableMap(new LinkedHashMap<>(builder.provided));
        this.providedOres = Collections.unmodifiableMap(new LinkedHashMap<>(builder.providedOres));
        this.oreLook = builder.oreLook;
        this.isotope = builder.isotope;
        this.nameable = builder.nameable;
        this.namer = builder.namer;
        this.mineral = builder.family == null ? null
                : new Mineral(builder.family, builder.contents, builder.rocks);
    }

    public ResourceLocation id() {
        return this.id;
    }

    public MaterialKind kind() {
        return this.kind;
    }

    /**
     * What it is called: "Iron"; the name it was given in this world, for a material that can be named
     * ({@link MaterialNames}); for an isotope, its element's name and its mass number, "Uranium-235".
     */
    public Text name() {
        if (this.namer != null) {
            final Optional<Text> now = this.namer.name(this);
            if (now.isPresent()) {
                return now.get();
            }
        } else if (this.nameable) {
            final Optional<MaterialNames.Given> given = MaterialNames.given(this);
            if (given.isPresent()) {
                return Text.literal(given.get().name());
            }
        }
        if (this.isotope != null && this.nameKey == null) {
            return MaterialTexts.ISOTOPE.with(this.isotope.element().name(), this.isotope.massNumber());
        }
        return this.name;
    }

    /** What it was called as it was declared, before any name given in a world. */
    public Text declaredName() {
        return this.name;
    }

    /** The key of its name, for a name declared as a sentence; none for a name that is the same in every language. */
    public Optional<TextKey> nameKey() {
        return Optional.ofNullable(this.nameKey);
    }

    /** Its colour, as {@code 0xRRGGBB}: what its things are drawn in. */
    public int colour() {
        return this.colour;
    }

    /** The state it is in at room temperature. */
    public MatterState roomState() {
        return this.roomState;
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
     * The value its declaration gives {@code property}, or none; the symbol given in this world for a material named
     * in it. A data pack may give another, which {@link MaterialValues} reads over this one.
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> value(final MaterialProperty<T> property) {
        if (property == MaterialProperties.SYMBOL) {
            final Optional<String> now = this.namer != null ? this.namer.symbol(this)
                    : MaterialNames.given(this).map(MaterialNames.Given::symbol);
            if (now.isPresent()) {
                return Optional.of((T) now.get());
            }
            // An isotope is written as chemists write it, after its element's symbol as that symbol is now: ²³⁵U.
            if (this.isotope != null && !this.values.containsKey(property)) {
                return this.isotope.element().value(MaterialProperties.SYMBOL)
                        .map(symbol -> (T) (Materials.superscript(this.isotope.massNumber()) + symbol));
            }
        }
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
     * dust for any other solid; none for a liquid, whose ore spills it (native mercury), which its mod sees to.
     */
    public Optional<Form> oreDrop() {
        return switch (this.kind) {
            case GEM -> Optional.of(Forms.GEM);
            case CRYSTAL -> Optional.of(Forms.CRYSTAL);
            case NONMETAL_SOLID -> Optional.of(Forms.DUST);
            case METAL, MINERAL -> Optional.of(Forms.RAW);
            default -> Optional.empty();
        };
    }

    /** What it contains and where it lies, for a mineral. */
    public Optional<Mineral> mineral() {
        return Optional.ofNullable(this.mineral);
    }

    /** The element it is an isotope of, with its mass number, for an isotope. */
    public Optional<Isotope> isotope() {
        return Optional.ofNullable(this.isotope);
    }

    /** Whether a player can give it a name in a world, as the discoverer of an element names it. */
    public boolean nameable() {
        return this.nameable;
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    /**
     * What an isotope is of: its element, and its mass number, its protons and neutrons together.
     *
     * @param element    the element
     * @param massNumber its mass number: 235 for uranium-235
     */
    public record Isotope(Material element, int massNumber) {

        public Isotope {
            Objects.requireNonNull(element, "element");
        }
    }

    /** How a material is declared. */
    public static final class Builder {

        private final ResourceLocation id;
        private final MaterialKind kind;
        private final Text name;
        private final @Nullable TextKey nameKey;
        private final Set<MatterState> states = EnumSet.noneOf(MatterState.class);
        private final Map<MaterialProperty<?>, Object> values = new LinkedHashMap<>();
        private final Map<MaterialProperty<?>, MaterialProperty.Shown> shown = new LinkedHashMap<>();
        private final Map<Form, Supplier<? extends ItemLike>> provided = new LinkedHashMap<>();
        private final Map<HostRock, Supplier<? extends Block>> providedOres = new LinkedHashMap<>();
        private int colour = GREY;
        private MatterState roomState;
        private OreLook oreLook = OreLook.PLAIN;
        private @Nullable Isotope isotope;
        private boolean nameable;
        private @Nullable IMaterialNamer namer;
        private @Nullable MineralFamily family;
        private final List<Mineral.Content> contents = new ArrayList<>();
        private final Set<HostRock> rocks = new LinkedHashSet<>();

        Builder(final ResourceLocation id, final MaterialKind kind, final Text name, @Nullable final TextKey nameKey) {
            this.id = Objects.requireNonNull(id, "id");
            this.kind = Objects.requireNonNull(kind, "kind");
            this.name = Objects.requireNonNull(name, "name");
            this.nameKey = nameKey;
            this.roomState = kind.roomState();
            // Its kind says its state at room temperature, and every material has one.
            this.values.put(MaterialProperties.ROOM_STATE, this.roomState);
        }

        /** Its colour, as {@code 0xRRGGBB}. */
        public Builder colour(final int rgb) {
            this.colour = rgb & 0xFFFFFF;
            this.values.put(MaterialProperties.COLOUR, ConfigColours.write(this.colour));
            return this;
        }

        /**
         * The state it is in at room temperature, when not the one its kind says: an exotic form of matter in the
         * state it calls for, a time crystal solid.
         */
        public Builder at(final MatterState state) {
            this.roomState = Objects.requireNonNull(state, "state");
            this.values.put(MaterialProperties.ROOM_STATE, state);
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
         * An isotope of {@code element}, of mass number {@code massNumber}: named after its element ("Uranium-235")
         * unless it was declared with a name of its own, as deuterium.
         */
        public Builder isotopeOf(final Material element, final int massNumber) {
            this.isotope = new Isotope(element, massNumber);
            return this;
        }

        /**
         * A player can give it a name in a world: an element beyond the known table, named by its discoverer. Unless
         * a namer says otherwise, the name given in the world ({@link MaterialNames}) is its name.
         */
        public Builder nameable() {
            this.nameable = true;
            return this;
        }

        /** What it is called as a world goes on, asked before its declaration each time its name is read. */
        public Builder namedBy(final IMaterialNamer now) {
            this.namer = Objects.requireNonNull(now, "now");
            return this;
        }

        /** A mineral of {@code family}, whose veins can cross {@code veinsCross}. */
        public Builder mineral(final MineralFamily of, final HostRock... veinsCross) {
            this.family = Objects.requireNonNull(of, "of");
            this.rocks.addAll(List.of(veinsCross));
            return this;
        }

        /**
         * What the mineral contains, in the order declared, its main element first, the one it is mined for:
         * {@code material}, in a share of the mineral's mass.
         */
        public Builder contains(final Material material, final double share) {
            this.contents.add(new Mineral.Content(material, share));
            return this;
        }

        /**
         * Declares the material.
         *
         * @throws DeclarationError when a material of that id is declared already, it is given a thing for a form its
         *                          kind cannot take, or it is an isotope of no element
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
            if (this.isotope != null && this.isotope.massNumber() <= 0) {
                throw DeclarationError.of(this.id.getNamespace(), "the isotope " + this.id,
                        "has the mass number " + this.isotope.massNumber(), "give it its protons and neutrons");
            }
            checkMineral();
            return Materials.declare(new Material(this));
        }

        private void checkMineral() {
            if (this.family == null && this.contents.isEmpty()) {
                return;
            }
            if (this.kind != MaterialKind.MINERAL || this.family == null) {
                throw DeclarationError.of(this.id.getNamespace(), "the material " + this.id,
                        "says what a mineral contains, but is no mineral of a family",
                        "declare it of the kind MINERAL with mineral(family, rocks...)");
            }
            final double[] shares = new double[this.contents.size()];
            for (int i = 0; i < shares.length; i++) {
                shares[i] = this.contents.get(i).share();
            }
            MineralShares.problem(shares).ifPresent(problem -> {
                throw DeclarationError.of(this.id.getNamespace(), "the mineral " + this.id, switch (problem) {
                    case NOTHING -> "contains nothing";
                    case OUTSIDE_THE_WHOLE -> "has a share that is not between nothing and the whole";
                    case MORE_THAN_THE_WHOLE -> "has shares adding up to more than the whole";
                }, "give its main element first, then its byproducts, adding up to the whole at most");
            });
        }
    }
}
