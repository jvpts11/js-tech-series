/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.energy;

import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPolicies;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.util.DeclarationError;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * An energy a mod uses, declared once with {@link Energies#builder}: the unit it is counted in and what that is worth
 * in FE, the classes of its lines if it has any (as voltage classes), what each class loses by distance, and what
 * befalls a port that meets a live line of another class. The Core gives it the rest: a grid for each class whose lines
 * never join another's, the sharing along them, the stores and ports ({@link EnergyStore}), the conversion to and from
 * FE ({@link EnergyConversion}) and the tooltips ({@link EnergyTooltips}).
 *
 * <p>A mod that only wants FE uses {@link Energies#FE}, whose one grid is the Core's power grid, and the game's own
 * energy capability.
 *
 * <pre>{@code
 * public static final Energy VOLTS = Energies.builder(rl("volts"), VOLTS_NAME).unit(JOULE)
 *         .classes(LOW, MEDIUM, HIGH).loss(LOW, 100).loss(MEDIUM, 10).loss(HIGH, 1)
 *         .wrongClass(IWrongClassRule.STANDARD).declare();
 * }</pre>
 */
public final class Energy {

    private final ResourceLocation id;
    private final TextKey name;
    private final Supplier<EnergyUnit> unit;
    private final @Nullable Supplier<EnergyRatio> feRate;
    private final BooleanSupplier acceptsFe;
    private final @Nullable TextKey powerSymbol;
    private final List<EnergyClass> classes;
    private final boolean classless;
    private final Map<String, Integer> loss;
    private final IWrongClassRule rule;
    private final Map<String, GridKind> grids = new HashMap<>();
    private final Map<GridKind, EnergyClass> classOfGrid = new HashMap<>();

    /** How many ticks make a second, for an energy shown as power. */
    public static final int TICKS_PER_SECOND = 20;

    private Energy(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.unit = builder.unit;
        this.feRate = builder.feRate;
        this.acceptsFe = builder.acceptsFe;
        this.powerSymbol = builder.powerSymbol;
        this.classless = builder.classes.isEmpty();
        this.classes = this.classless ? List.of(EnergyClass.of(builder.id.getPath(), builder.name))
                : List.copyOf(builder.classes);
        this.loss = Map.copyOf(builder.loss);
        this.rule = builder.rule;
    }

    public ResourceLocation id() {
        return this.id;
    }

    /** What a player calls it. */
    public Text name() {
        return this.name.text();
    }

    /** The unit it is counted in, whose ratio says what it is worth in FE. */
    public EnergyUnit unit() {
        return this.unit.get();
    }

    /** Its classes of line, from the lowest to the highest; one, named as the energy, for an energy with none. */
    public List<EnergyClass> classes() {
        return this.classes;
    }

    /** Whether it has no classes of line of its own, so a tooltip names none. */
    public boolean classless() {
        return this.classless;
    }

    /** Its lowest class, the one class of an energy with none. */
    public EnergyClass lowest() {
        return this.classes.getFirst();
    }

    /** Its class named {@code path}, or none. */
    public Optional<EnergyClass> classNamed(final String path) {
        for (final EnergyClass each : this.classes) {
            if (each.path().equals(path)) {
                return Optional.of(each);
            }
        }
        return Optional.empty();
    }

    /** The millionths of what crosses it that each block of a line of {@code of} loses by distance. */
    public int lossPerBlock(final EnergyClass of) {
        return this.loss.getOrDefault(checked(of).path(), 0);
    }

    /** The grid the lines of {@code of} make, which joins no line of another class. */
    public GridKind grid(final EnergyClass of) {
        final GridKind grid = this.grids.get(checked(of).path());
        if (grid == null) {
            throw new IllegalStateException("the energy " + this.id + " is not declared yet");
        }
        return grid;
    }

    /** The grid of its lowest class, the one grid of an energy with no classes. */
    public GridKind grid() {
        return grid(lowest());
    }

    /** The class whose lines make {@code grid}, or none for a grid of another energy. */
    public Optional<EnergyClass> classOf(final GridKind grid) {
        return Optional.ofNullable(this.classOfGrid.get(grid));
    }

    /** How many classes {@code line} is above {@code port}; below it when negative. */
    public int steps(final EnergyClass port, final EnergyClass line) {
        return this.classes.indexOf(checked(line)) - this.classes.indexOf(checked(port));
    }

    /** What befalls a port of {@code port} that meets a live line of {@code line}, as the energy's rule says. */
    public WrongClass meeting(final EnergyClass port, final EnergyClass line) {
        final int steps = steps(port, line);
        return steps == 0 ? WrongClass.NONE : this.rule.at(steps);
    }

    /**
     * Whether its lines take and give FE now: whether a block with only the game's energy capability is met by its lines
     * as a port of any class, at its worth in FE.
     */
    public boolean acceptsFe() {
        return this.acceptsFe.getAsBoolean();
    }

    /** What it is worth in FE: its rate where it declares one (a server's setting), its unit's ratio otherwise. */
    public EnergyRatio ratio() {
        return this.feRate == null ? unit().ratio() : this.feRate.get();
    }

    /** How many FE {@code amount} of it is worth, rounded down. */
    public long toFe(final long amount) {
        return ratio().toFe(amount);
    }

    /** How much of it {@code amount} FE is worth, rounded down. */
    public long fromFe(final long amount) {
        return ratio().fromFe(amount);
    }

    /** {@code amount} of it as a player reads it, at the SI scale that suits it: "1.5 kJ". */
    public Text amount(final long amount) {
        final UnitFormatter.Scaled scaled = UnitFormatter.forCurrentLocale().si(amount);
        return EnergyTexts.AMOUNT.with(scaled.number(), scaled.prefix(), unit().symbol());
    }

    /**
     * {@code perTick} of it a tick as a player reads it: as power, "7.9 kW", for an energy that declares a unit of
     * power, or so much a tick otherwise.
     */
    public Text rate(final long perTick) {
        if (this.powerSymbol == null) {
            final UnitFormatter.Scaled scaled = UnitFormatter.forCurrentLocale().si(perTick);
            return EnergyTexts.PER_TICK.with(scaled.number(), scaled.prefix(), unit().symbol());
        }
        final UnitFormatter.Scaled scaled = UnitFormatter.forCurrentLocale().si(perSecond(perTick));
        return EnergyTexts.AMOUNT.with(scaled.number(), scaled.prefix(), this.powerSymbol.text());
    }

    /** {@code perTick} a tick, made a second's worth. */
    public static long perSecond(final long perTick) {
        return perTick > Long.MAX_VALUE / TICKS_PER_SECOND ? Long.MAX_VALUE : perTick * TICKS_PER_SECOND;
    }

    @Override
    public String toString() {
        return this.id.toString();
    }

    /* Declares the grid of each class, once the energy is registered. */
    void declareGrids(final @Nullable GridKind only) {
        for (final EnergyClass each : this.classes) {
            final GridKind grid = only != null ? only : GridKind.declare(this.classless ? this.id
                    : ResourceLocation.fromNamespaceAndPath(this.id.getNamespace(),
                    this.id.getPath() + "_" + each.path()), GridPolicies.energy(this, each));
            this.grids.put(each.path(), grid);
            this.classOfGrid.put(grid, each);
        }
    }

    private EnergyClass checked(final EnergyClass of) {
        if (!this.classes.contains(Objects.requireNonNull(of, "of"))) {
            throw new IllegalArgumentException("the energy " + this.id + " has no class " + of.path());
        }
        return of;
    }

    /** Declares an energy one property at a time. */
    public static final class Builder {

        private final ResourceLocation id;
        private final TextKey name;
        private Supplier<EnergyUnit> unit = CoreEnergy.FE;
        private @Nullable Supplier<EnergyRatio> feRate;
        private BooleanSupplier acceptsFe = () -> false;
        private @Nullable TextKey powerSymbol;
        private final List<EnergyClass> classes = new ArrayList<>();
        private final Map<String, Integer> loss = new LinkedHashMap<>();
        private IWrongClassRule rule = IWrongClassRule.STANDARD;

        Builder(final ResourceLocation id, final TextKey name) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
        }

        /** The unit it is counted in; FE unless said. */
        public Builder unit(final Supplier<EnergyUnit> counted) {
            this.unit = Objects.requireNonNull(counted, "counted");
            return this;
        }

        /**
         * What it is worth in FE, asked each time: a rate a server sets, never a player, since a converter that changed
         * it would make energy from nothing. Its unit's ratio unless said.
         */
        public Builder feRate(final Supplier<EnergyRatio> rate) {
            this.feRate = Objects.requireNonNull(rate, "rate");
            return this;
        }

        /**
         * Whether its lines take and give FE, asked each time: a block with only the game's energy capability is then a
         * port of any class to them, so the energy speaks FE with the rest of a pack. Never unless said.
         */
        public Builder acceptsFe(final BooleanSupplier when) {
            this.acceptsFe = Objects.requireNonNull(when, "when");
            return this;
        }

        /** The symbol of its power, what a second of it is shown in ("W" for joules); a rate a tick unless said. */
        public Builder power(final TextKey symbol) {
            this.powerSymbol = Objects.requireNonNull(symbol, "symbol");
            return this;
        }

        /** Its classes of line, from the lowest to the highest; none unless said. */
        public Builder classes(final EnergyClass... lowestFirst) {
            for (final EnergyClass each : lowestFirst) {
                if (this.classes.stream().anyMatch(known -> known.path().equals(each.path()))) {
                    throw DeclarationError.of(this.id.getNamespace(), "the energy " + this.id,
                            "has two classes named " + each.path(), "name each class once");
                }
                this.classes.add(each);
            }
            return this;
        }

        /** The millionths of what crosses it that each block of a line of {@code of} loses; nothing unless said. */
        public Builder loss(final EnergyClass of, final int millionths) {
            if (millionths < 0 || millionths > EnergyLoss.WHOLE) {
                throw DeclarationError.of(this.id.getNamespace(), "the energy " + this.id,
                        "loses " + millionths + " millionths a block on " + of.path(),
                        "give a loss between 0 and " + EnergyLoss.WHOLE);
            }
            this.loss.put(of.path(), millionths);
            return this;
        }

        /** The millionths each block of its lines loses, for an energy with no classes; nothing unless said. */
        public Builder loss(final int millionths) {
            return loss(EnergyClass.of(this.id.getPath(), this.name), millionths);
        }

        /** What befalls a port that meets a live line of another class; the standard rule unless said. */
        public Builder wrongClass(final IWrongClassRule what) {
            this.rule = Objects.requireNonNull(what, "what");
            return this;
        }

        /**
         * The energy, registered and with a grid for each class.
         *
         * @throws DeclarationError when an energy of that id is declared already, or a loss names no class of it
         */
        public Energy declare() {
            for (final String lossy : this.loss.keySet()) {
                final boolean ofTheOneClass = this.classes.isEmpty() && lossy.equals(this.id.getPath());
                if (!ofTheOneClass && this.classes.stream().noneMatch(each -> each.path().equals(lossy))) {
                    throw DeclarationError.of(this.id.getNamespace(), "the energy " + this.id,
                            "gives a loss to " + lossy + ", which is none of its classes",
                            "declare the classes before their losses");
                }
            }
            return Energies.register(new Energy(this), null);
        }

        /* The energy whose one grid is {@code grid}, already declared: FE and the Core's power grid. */
        Energy declareOn(final GridKind grid) {
            return Energies.register(new Energy(this), grid);
        }
    }
}
