/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.cable;

import dev.jstech.core.connect.Connection;
import dev.jstech.core.energy.EnergyLoss;
import dev.jstech.core.fluid.PipeLimits;
import dev.jstech.core.fluid.PipeMarks;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridMember;
import dev.jstech.core.progression.IAxisStep;
import dev.jstech.core.text.TextKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

/**
 * A cable a mod lays in the Core's cable block: the line it carries and its generation, the grid it is a part of, the
 * lane it takes when it shares a block, how thick it is, how much it carries, how far a run of it reaches and what it
 * loses of what crosses it, and how it looks. Registered in {@link CoreCables#REGISTRY}, each with the item that lays
 * it, by a mod's own content.
 *
 * <p>A pipe, a cable of the fluid grid, also says what it is made for: the temperatures it stands and the marks it
 * takes, and whether it is insulated against the cold. How much a pipe carries in a tick is the pressure it holds; a
 * gas needs a pipe that takes gases. A pipe of a state held under pressure may say the most pressure it holds: what
 * comes at more is carried none of and reaches it as a breach.
 *
 * <p>A cable that never shares a block takes no lane: a block that holds it holds nothing else.
 *
 * <p>Some cables keep a shape: one that runs only straight joins along a single axis in a block, so it never turns a
 * corner; one that joins at most so many faces in a block never branches past that, so a cable that joins two runs
 * from one end to the other with nothing branching off it.
 */
public final class CableType {

    /** The thickest a cable is, in pixels: three quarters of a block, a pipe alone in its block. */
    public static final int THICKEST = 12;
    /* The widest a wire in an outer lane can be and still stay inside the block, half a block less the lane pitch. */
    private static final int MOST_PIXELS_IN_A_LANE = 2 * (8 - Lane.PITCH);

    private final Connection line;
    private final GridKind grid;
    private final @Nullable Lane lane;
    private final int thickness;
    private final long throughput;
    private final int range;
    private final int loss;
    private final PipeLimits pipe;
    private final ResourceLocation jacket;
    private final ResourceLocation plug;
    private final Map<String, ResourceLocation> plugs;
    private final Supplier<? extends Item> item;
    private final boolean straight;
    private final int mostJoins;
    private final @Nullable TextKey what;
    private final @Nullable IAxisStep step;
    private final @Nullable TextKey renewal;
    private final IBreachRule breach;
    private final @Nullable ResourceLocation window;
    private final @Nullable ResourceLocation fitting;
    private final int span;
    private final boolean insulated;
    private final long holds;
    private final SoundType sound;

    private CableType(final Builder builder, final Supplier<? extends Item> item) {
        this.line = Objects.requireNonNull(builder.line, "a cable carries a line");
        this.grid = Objects.requireNonNull(builder.grid, "a cable is a part of a grid");
        if (!builder.alone && builder.lane == null) {
            throw new IllegalStateException("a cable that shares blocks takes a lane: " + this.line.line());
        }
        if (!builder.alone && builder.thickness > MOST_PIXELS_IN_A_LANE) {
            // Past this a wire in an outer lane would stick out of the block, and wires in lanes side by side overlap.
            throw new IllegalStateException("a cable that takes a lane is at most " + MOST_PIXELS_IN_A_LANE
                    + " pixels thick, not " + builder.thickness + ": " + this.line.line());
        }
        this.lane = builder.alone ? null : builder.lane;
        this.straight = builder.straight;
        this.mostJoins = builder.mostJoins;
        this.thickness = builder.thickness;
        this.throughput = builder.throughput;
        this.range = builder.range;
        this.loss = builder.loss;
        this.pipe = new PipeLimits(builder.coldest, builder.hottest, builder.takes, builder.wornBy);
        this.jacket = Objects.requireNonNull(builder.jacket, "a cable has a jacket");
        this.plug = Objects.requireNonNull(builder.plug, "a cable has a plug");
        this.plugs = builder.plugs.isEmpty() ? Map.of() : Map.copyOf(builder.plugs);
        this.item = Objects.requireNonNull(item, "item");
        this.what = builder.what;
        this.step = builder.step;
        this.renewal = builder.renewal;
        this.breach = builder.breach;
        this.window = builder.window;
        this.fitting = builder.fitting;
        this.span = builder.span;
        this.insulated = builder.insulated;
        this.holds = builder.holds;
        this.sound = builder.sound;
    }

    /** What the cable is for, as its item's tooltip says it; empty for a cable that says nothing. */
    public Optional<TextKey> what() {
        return Optional.ofNullable(this.what);
    }

    /** What starts a new run of it when its reach is not enough, as its item's tooltip says it; empty for none. */
    public Optional<TextKey> renewal() {
        return Optional.ofNullable(this.renewal);
    }

    /**
     * The step of progression the cable belongs to (an era of hardware, say), as its item's tooltip says it in the
     * step's colour; empty for a cable of none.
     */
    public Optional<IAxisStep> step() {
        return Optional.ofNullable(this.step);
    }

    /** Starts declaring a cable of {@code line}. */
    public static Builder builder(final Connection line) {
        return new Builder(line);
    }

    /** The line it carries, in its generation. */
    public Connection line() {
        return this.line;
    }

    /** The grid it is a part of. */
    public GridKind grid() {
        return this.grid;
    }

    /** The lane it takes in a block it shares, or empty for a cable that never shares a block. */
    public Optional<Lane> lane() {
        return Optional.ofNullable(this.lane);
    }

    /** Whether a block that holds it holds nothing else. */
    public boolean alone() {
        return this.lane == null;
    }

    /** How thick it is, in pixels. */
    public int thickness() {
        return this.thickness;
    }

    /** How much it carries, in its grid's unit. */
    public long throughput() {
        return this.throughput;
    }

    /** How many cables a run of it reaches before it has to be renewed; 0 for no limit. */
    public int range() {
        return this.range;
    }

    /**
     * The millionths of what crosses one block of it that it loses besides its class's loss by distance; 0 for a cable
     * that loses nothing of its own.
     */
    public int loss() {
        return this.loss;
    }

    /** What it is made for as a pipe: the temperatures it stands and the marks it takes. */
    public PipeLimits pipe() {
        return this.pipe;
    }

    /**
     * The glass in its windows, in its jacket's layout: a pipe or a tube whose jacket's texture leaves windows clear
     * shows through them what flows inside it; none for a cable that shows nothing.
     */
    public Optional<ResourceLocation> window() {
        return Optional.ofNullable(this.window);
    }

    /** The texture of the solid fitting it turns and branches in, in its jacket's layout; none for its jacket's. */
    public Optional<ResourceLocation> fitting() {
        return Optional.ofNullable(this.fitting);
    }

    /** What befalls a wire of it that something it is not made for reaches; by default it holds. */
    public IBreachRule breach() {
        return this.breach;
    }

    /** Whether something it is not made for does more than stay where it is: its rule is not to hold. */
    public boolean breaks() {
        return this.breach != BreachRules.HOLDS;
    }

    /** Whether it runs only straight: in a block it joins along one axis, so it never turns a corner. */
    public boolean runsStraight() {
        return this.straight;
    }

    /** Whether its wire can be hung between insulators, as an overhead line. */
    public boolean hangs() {
        return this.span > 0;
    }

    /** The longest span its wire is hung across, in blocks, from insulator to insulator; 0 for a cable never hung. */
    public int span() {
        return this.span;
    }

    /** The most faces it joins in a block; 0 for no limit. */
    public int mostJoins() {
        return this.mostJoins;
    }

    /** Whether, as a pipe, it is insulated: what it carries never freezes in it, however cold the place. */
    public boolean insulated() {
        return this.insulated;
    }

    /** The most pressure it holds as a pipe, in times the open air's; {@link Long#MAX_VALUE} for no limit. */
    public long holds() {
        return this.holds;
    }

    /** How it sounds as it is laid, taken out, struck and walked on: as what it is made of, metal unless said. */
    public SoundType sound() {
        return this.sound;
    }

    /**
     * Its jacket's texture, 32 pixels square: the side with the length along u in the top left (16 by its thickness),
     * the same side with the length along v beside it, and the cut end below.
     */
    public ResourceLocation jacket() {
        return this.jacket;
    }

    /** The standalone block model of the plug it ends in at a device, against the north face. */
    public ResourceLocation plug() {
        return this.plug;
    }

    /**
     * The plug it ends in at a device whose port is of the kind named {@code kind} ({@link IPlugged}): its own for that
     * kind, or its plug.
     */
    public ResourceLocation plug(final String kind) {
        return this.plugs.getOrDefault(kind, this.plug);
    }

    /** Every plug it can end in: its own, and those it has for some kinds of port. */
    public Set<ResourceLocation> allPlugs() {
        final Set<ResourceLocation> all = new HashSet<>(this.plugs.values());
        all.add(this.plug);
        return all;
    }

    /** The item that lays it. */
    public Item item() {
        return this.item.get();
    }

    /** One of the item that lays it. */
    public ItemStack stack() {
        return new ItemStack(item());
    }

    /** What a wire of it, in {@code colour} or none, stands for in its grid. */
    public GridMember member(final Optional<DyeColor> colour) {
        final GridMember plain = GridMember.cable(this.line.line().toString(), this.line.generation(),
                this.throughput, this.range);
        return colour.map(dye -> plain.coloured(dye.getId())).orElse(plain);
    }

    /** The id this cable is registered under, or null when it is not registered. */
    public @Nullable ResourceLocation id() {
        return CoreCables.REGISTRY.getKey(this);
    }

    /** Declares a cable one property at a time. */
    public static final class Builder {

        private final Connection line;
        private GridKind grid = GridKind.DATA;
        private @Nullable Lane lane;
        private boolean alone;
        private int thickness = STANDARD_THICKNESS;
        private long throughput;
        private int range;
        private int loss;
        private int coldest = PipeLimits.PLAIN.coldest();
        private int hottest = PipeLimits.PLAIN.hottest();
        private final Set<TagKey<Fluid>> takes = new HashSet<>();
        private final Map<TagKey<Fluid>, Integer> wornBy = new HashMap<>();
        private @Nullable ResourceLocation jacket;
        private @Nullable ResourceLocation plug;
        private final Map<String, ResourceLocation> plugs = new HashMap<>();
        private boolean straight;
        private int mostJoins;
        private @Nullable TextKey what;
        private @Nullable IAxisStep step;
        private @Nullable TextKey renewal;
        private IBreachRule breach = BreachRules.HOLDS;
        private @Nullable ResourceLocation window;
        private @Nullable ResourceLocation fitting;
        private int span;
        private boolean insulated;
        private long holds = Long.MAX_VALUE;
        private SoundType sound = SoundType.METAL;

        /** How thick a cable is unless it says otherwise, in pixels. */
        private static final int STANDARD_THICKNESS = 4;
        /* No thicker than three quarters of a block: a pipe alone in its block, as pipes are in every mod. */
        private static final int MOST_PIXELS = THICKEST;

        private Builder(final Connection line) {
            this.line = Objects.requireNonNull(line, "line");
        }

        /** The grid it is a part of; data unless said. */
        public Builder grid(final GridKind kind) {
            this.grid = Objects.requireNonNull(kind, "kind");
            return this;
        }

        /** The lane it takes in a block it shares. */
        public Builder lane(final Lane taken) {
            this.lane = Objects.requireNonNull(taken, "taken");
            this.alone = false;
            return this;
        }

        /** It never shares a block: a block that holds it holds nothing else. */
        public Builder alone() {
            this.alone = true;
            this.lane = null;
            return this;
        }

        /**
         * How thick it is, in pixels; four unless said. A cable that takes a lane is at most six, so wires side by side
         * never meet; one alone in its block, as a pipe, at most twelve.
         */
        public Builder thickness(final int pixels) {
            if (pixels <= 0 || pixels > MOST_PIXELS) {
                throw new IllegalArgumentException("a cable is between 1 and " + MOST_PIXELS + " pixels thick, not "
                        + pixels);
            }
            this.thickness = pixels;
            return this;
        }

        /** How much it carries and how many cables a run of it reaches; a range of 0 reaches without limit. */
        public Builder carries(final long amount, final int reach) {
            if (amount < 0 || reach < 0) {
                throw new IllegalArgumentException("a cable carries and reaches no less than nothing");
            }
            this.throughput = amount;
            this.range = reach;
            return this;
        }

        /**
         * The millionths of what crosses one block of it that it loses besides its class's loss by distance; nothing
         * unless said.
         */
        public Builder loses(final int millionths) {
            if (millionths < 0 || millionths > EnergyLoss.WHOLE) {
                throw new IllegalArgumentException("a cable loses between 0 and " + EnergyLoss.WHOLE
                        + " millionths, not " + millionths);
            }
            this.loss = millionths;
            return this;
        }

        /** As a pipe, the coldest and the hottest fluid it stands, in kelvin; any temperature unless said. */
        public Builder withstands(final int coldest, final int hottest) {
            if (coldest < 0 || hottest < coldest) {
                throw new IllegalArgumentException("a pipe stands from " + coldest + " K to " + hottest + " K");
            }
            this.coldest = coldest;
            this.hottest = hottest;
            return this;
        }

        /** As a pipe, it is made for what is marked {@code mark} ({@link PipeMarks}): a gas, a corrosive fluid. */
        public Builder takes(final TagKey<Fluid> mark) {
            this.takes.add(Objects.requireNonNull(mark, "mark"));
            return this;
        }

        /**
         * As a pipe, what is marked {@code mark} wears it through once it has carried it {@code ticks} ticks in all,
         * which reaches it as a breach ({@link LineBreach.Reason#WORN_OUT}): hydrogen embrittles a steel pipe, a slurry
         * abrades an unlined one.
         */
        public Builder wornBy(final TagKey<Fluid> mark, final int ticks) {
            if (ticks < 1) {
                throw new IllegalArgumentException("a pipe stands a tick at least of what wears it, not " + ticks);
            }
            this.wornBy.put(Objects.requireNonNull(mark, "mark"), ticks);
            return this;
        }

        /**
         * As a pipe, it is insulated: a liquid in it never freezes, where a pipe that is not freezes in a place colder
         * than the liquid's melting point (a server may turn freezing off).
         */
        public Builder insulated() {
            this.insulated = true;
            return this;
        }

        /**
         * As a pipe of a state held under pressure, the most pressure it holds, in times the open air's: what comes at
         * more, from a store holding it at more, is carried none of and reaches it as a breach ({@link #beyond}); any
         * pressure unless said.
         */
        public Builder holds(final long atmospheres) {
            if (atmospheres < 1) {
                throw new IllegalArgumentException("a pipe holds at least the open air's pressure, not " + atmospheres);
            }
            this.holds = atmospheres;
            return this;
        }

        /**
         * What befalls a wire of it that something it is not made for reaches, each tick it does: a pipe too hot or
         * too cold for what reaches it, or not made for its mark ({@link BreachRules}); unless said, it holds and
         * carries none of it.
         */
        public Builder beyond(final IBreachRule rule) {
            this.breach = Objects.requireNonNull(rule, "rule");
            return this;
        }

        /**
         * Its wire is also hung between insulators, as an overhead line, across at most {@code longestSpan} blocks from
         * one insulator to the next; its item hangs it when used on an insulator. Each line that hangs is its mod's
         * decision.
         */
        public Builder hangs(final int longestSpan) {
            if (longestSpan <= 0) {
                throw new IllegalArgumentException("a hung wire spans at least a block, not " + longestSpan);
            }
            this.span = longestSpan;
            return this;
        }

        /** It sounds as {@code made} sounds, laid, taken out, struck and walked on; metal unless said. */
        public Builder sounds(final SoundType made) {
            this.sound = Objects.requireNonNull(made, "made");
            return this;
        }

        /** It runs only straight: in a block it joins along one axis, and something else has to turn it. */
        public Builder runsStraight() {
            this.straight = true;
            return this;
        }

        /**
         * What the cable is for, in a sentence its item's tooltip shows ("Access line: joins the small computers to
         * a router"), and the step of progression it belongs to, an era of hardware say, which the tooltip names in
         * the step's colour; null for a cable of none.
         */
        public Builder describe(final TextKey job, @Nullable final IAxisStep ofStep) {
            this.what = Objects.requireNonNull(job, "job");
            this.step = ofStep;
            return this;
        }

        /**
         * What starts a new run of it, in a sentence its item's tooltip shows under its reach ("A Repeater starts a new
         * run"): the device a player lays in a run that would be too long. The library names none, since the devices
         * are the mods'.
         */
        public Builder renewedBy(final TextKey sentence) {
            this.renewal = Objects.requireNonNull(sentence, "sentence");
            return this;
        }

        /** It joins at most {@code faces} faces in a block, so it branches no further; two runs it end to end. */
        public Builder joinsAtMost(final int faces) {
            if (faces < 1 || faces > Direction.values().length) {
                throw new IllegalArgumentException("a cable joins between 1 and 6 faces, not " + faces);
            }
            this.mostJoins = faces;
            return this;
        }

        /** Its jacket's texture. */
        public Builder jacket(final ResourceLocation texture) {
            this.jacket = Objects.requireNonNull(texture, "texture");
            return this;
        }

        /**
         * Its jacket leaves windows clear, and {@code glass}, a texture in the jacket's layout, fills them: through them
         * a pipe shows its fluid at its level, and a tube its items moving.
         */
        public Builder windows(final ResourceLocation glass) {
            this.window = Objects.requireNonNull(glass, "glass");
            return this;
        }

        /** It turns and branches in a solid fitting of {@code texture}, in the jacket's layout, as a pipe does. */
        public Builder fitting(final ResourceLocation texture) {
            this.fitting = Objects.requireNonNull(texture, "texture");
            return this;
        }

        /** The standalone model of the plug it ends in at a device. */
        public Builder plug(final ResourceLocation model) {
            this.plug = Objects.requireNonNull(model, "model");
            return this;
        }

        /**
         * The standalone model of the plug it ends in at a device whose port is of the kind named {@code kind}
         * ({@link IPlugged}): a video plug at a screen, say, where the others take the cable's own plug.
         */
        public Builder plug(final String kind, final ResourceLocation model) {
            this.plugs.put(Objects.requireNonNull(kind, "kind"), Objects.requireNonNull(model, "model"));
            return this;
        }

        /** The cable, laid by {@code item}. */
        public CableType build(final Supplier<? extends Item> item) {
            return new CableType(this, item);
        }
    }
}
