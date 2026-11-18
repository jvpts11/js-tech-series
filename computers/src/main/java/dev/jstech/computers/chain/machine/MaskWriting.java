/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableNames;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity.Processing;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Writing a mask on a Lithography Station from the Legacy on (computers/docs/design/survival.md, "Masks"): the computer
 * linked to the station writes the mask its lithography program picks on the Mask Blank in the station, in one layer's
 * time, half on a computer two eras newer, and never while the station exposes a wafer; the mask then stands where its
 * blank was, to expose through. A station offers the masks of the eras it serves that its computer may write, an
 * era's chips being made by the computers of the era before: a processor and a graphics mask for each
 * microarchitecture, the portables' families, the hashing die, the memory and controller chips, and the masks of the
 * laser diodes and the LEDs.
 */
@TextHolder
public final class MaskWriting {

    /** Why a mask is not written: the station exposes a wafer. */
    public static final TextKey EXPOSING = TextKey.of("jsc.lithography.refused.exposing",
            "The station is exposing: change its mask when the wafer leaves");
    /** Why a mask is not written: the station holds no Mask Blank. */
    public static final TextKey NO_BLANK = TextKey.of("jsc.lithography.refused.no_blank",
            "Put a Mask Blank in the station to write a mask on it");
    /** Why a mask is not written: the station writes one already. */
    public static final TextKey WRITING = TextKey.of("jsc.lithography.refused.writing",
            "The station is writing a mask: wait for it to finish");
    /** Why a mask is not written: the station does not write it, or not for this computer. */
    public static final TextKey NOT_OFFERED = TextKey.of("jsc.lithography.refused.not_offered",
            "This station does not write that mask for this computer");
    private static final TextKey PROCESSORS = TextKey.of("jsc.lithography.kind.processors", "Processors");
    private static final TextKey GRAPHICS = TextKey.of("jsc.lithography.kind.graphics", "Graphics");
    private static final TextKey MEMORY = TextKey.of("jsc.lithography.kind.memory", "Memory");
    private static final TextKey PORTABLES = TextKey.of("jsc.lithography.kind.portables", "Portables");
    private static final TextKey HASHING = TextKey.of("jsc.lithography.kind.hashing", "Hashing");
    private static final TextKey LIGHT = TextKey.of("jsc.lithography.kind.light", "Light");
    private static final TextKey PROCESSOR_MASK = TextKey.of("jsc.lithography.mask.processor", "%s processor");
    private static final TextKey GRAPHICS_MASK = TextKey.of("jsc.lithography.mask.graphics", "%s graphics");
    private static final TextKey HASHING_MASK = TextKey.of("jsc.lithography.mask.hashing", "Hashing chip");
    private static final TextKey MEMORY_MASK = TextKey.of("jsc.lithography.mask.memory", "Memory chip");
    private static final TextKey CONTROLLER_MASK = TextKey.of("jsc.lithography.mask.controller", "Controller chip");
    private static final TextKey LASER_MASK = TextKey.of("jsc.lithography.mask.laser_diode", "Laser diode");
    private static final TextKey LED_MASK = TextKey.of("jsc.lithography.mask.led", "LED");
    private static final TextKey OF_ERA = TextKey.of("jsc.lithography.mask.of_era", "%s (%s)");
    /* The eras whose wafers of light are exposed through the laser diodes' mask; the LEDs' is the Standard's. */
    private static final List<Era> LASER_ERAS = List.of(HardwareEra.LEGACY, HardwareEra.TRANSITION,
            HardwareEra.STANDARD);
    private static final int TICKS_A_SECOND = 20;

    private MaskWriting() {
    }

    /**
     * The masks {@code station} writes for a computer of the era {@code computer}, in the order of the program's lists;
     * none for a station that works without a computer.
     */
    public static List<Offer> offers(final ChainMachine station, final Era computer) {
        if (!station.linked()) {
            return List.of();
        }
        final List<Era> eras = ChainMachines.workedBy(station).stream().map(ChainMachines.Worked::era)
                .filter(Objects::nonNull).distinct().filter(era -> computer.isAtLeast(LithoLink.before(era)))
                .toList();
        // A mask is named with its era only where the computer is offered the masks of two.
        final boolean several = eras.size() > 1;
        final List<Offer> out = new ArrayList<>();
        for (final Era era : eras) {
            for (final Offer offer : ofEra(era)) {
                // The light's masks serve every era whose light takes them, so a station of two eras writes each once.
                if (out.stream().noneMatch(each -> each.mask() == offer.mask())) {
                    out.add(several && offer.kind() != Kind.LIGHT ? new Offer(offer.kind(), era, offer.mask(),
                            OF_ERA.with(offer.name(), era.name())) : offer);
                }
            }
        }
        out.sort(Comparator.comparing(Offer::kind));
        return List.copyOf(out);
    }

    /**
     * The mask {@code mask} as the lithography program names it, of the era it is of, whichever station writes it; a
     * Vintage mask, which the Vintage station writes from a Layout, too; none for an item that is no mask.
     */
    public static Optional<Offer> offerOf(final Item mask) {
        for (final Era era : ChainParts.ERAS) {
            for (final Offer offer : ofEra(era)) {
                if (offer.mask() == mask) {
                    return Optional.of(offer);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Has {@code station} write {@code mask} for its computer of the era {@code computer} on the Mask Blank it holds;
     * why it does not, when it does not: a mask it does not write for that computer, a wafer it exposes, a mask it is
     * writing already, or no blank.
     */
    public static Optional<TextKey> write(final WaferMachineBlockEntity station, final Era computer, final Item mask) {
        if (offers(station.machine(), computer).stream().noneMatch(offer -> offer.mask() == mask)) {
            return Optional.of(NOT_OFFERED);
        }
        if (station.exposing()) {
            return Optional.of(EXPOSING);
        }
        if (station.writes() != null && station.getProgress() > 0) {
            return Optional.of(WRITING);
        }
        if (blankIn(station.inputs()) < 0) {
            return Optional.of(NO_BLANK);
        }
        station.write(mask);
        return Optional.empty();
    }

    /** What the program calls the list of {@code kind}. */
    public static Text title(final Kind kind) {
        return switch (kind) {
            case PROCESSORS -> PROCESSORS.text();
            case GRAPHICS -> GRAPHICS.text();
            case MEMORY -> MEMORY.text();
            case PORTABLES -> PORTABLES.text();
            case HASHING -> HASHING.text();
            case LIGHT -> LIGHT.text();
        };
    }

    /**
     * The work of {@code station} writing {@code mask} on the blank among {@code input}: the blank taken, the mask
     * made, in one layer's time at the station's draw for the mask's era; none without a blank or for a mask it does
     * not write.
     */
    static Optional<Writing> work(final ChainMachine station, final ProcessingInput input, @Nullable final Item mask) {
        if (mask == null) {
            return Optional.empty();
        }
        final int blank = blankIn(input.items());
        final Optional<Offer> offer = offers(station, HardwareEra.SERIES.getLast()).stream()
                .filter(each -> each.mask() == mask).findFirst();
        if (blank < 0 || offer.isEmpty()) {
            return Optional.empty();
        }
        final Era era = offer.get().era();
        final int joules = ChainMachines.workedBy(station).stream().filter(worked -> worked.era() == era)
                .mapToInt(ChainMachines.Worked::joulesATick).findFirst().orElse(0);
        final Processing work = Processing.single(blank, new ItemStack(mask), station.seconds() * TICKS_A_SECOND)
                .withEnergyPerTick(joules);
        return Optional.of(new Writing(blank, mask, era, work));
    }

    /* The first of {@code items} that is a Mask Blank, by its place; -1 for none. */
    private static int blankIn(final List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).is(ChainChips.MASK_BLANK.get())) {
                return i;
            }
        }
        return -1;
    }

    /*
     * The masks of {@code era}'s chips, named without their era: its dies' and its memory and controller chips', and
     * the light's masks of the eras whose light takes them.
     */
    private static List<Offer> ofEra(final Era era) {
        final List<Offer> out = new ArrayList<>();
        for (final ChainChips.Die die : ChainChips.all()) {
            if (die.era() == era) {
                out.add(new Offer(kindOf(die), era, die.mask().get(), nameOf(die)));
            }
        }
        final int at = ChainParts.ERAS.indexOf(era);
        out.add(new Offer(Kind.MEMORY, era, ChainParts.MEMORY_MASKS.get(at).get(), MEMORY_MASK.text()));
        out.add(new Offer(Kind.MEMORY, era, ChainParts.CONTROLLER_MASKS.get(at).get(), CONTROLLER_MASK.text()));
        if (LASER_ERAS.contains(era)) {
            out.add(new Offer(Kind.LIGHT, era, ChainChips.LASER_DIODE_MASK.get(), LASER_MASK.text()));
        }
        if (era == HardwareEra.STANDARD) {
            out.add(new Offer(Kind.LIGHT, era, ChainChips.LED_MASK.get(), LED_MASK.text()));
        }
        return out;
    }

    private static Kind kindOf(final ChainChips.Die die) {
        return switch (die.kind()) {
            case PROCESSOR -> Kind.PROCESSORS;
            case GRAPHICS -> Kind.GRAPHICS;
            case HASHING -> Kind.HASHING;
            case MOBILE -> Kind.PORTABLES;
        };
    }

    private static Text nameOf(final ChainChips.Die die) {
        return switch (die.kind()) {
            case PROCESSOR -> PROCESSOR_MASK.with(die.english());
            case GRAPHICS -> GRAPHICS_MASK.with(die.english());
            case HASHING -> HASHING_MASK.text();
            case MOBILE -> Text.literal(die.english());
        };
    }

    /** The lists of the lithography program, a kind of chip each, in the order it shows them. */
    public enum Kind implements IStableName {
        PROCESSORS("processors"),
        GRAPHICS("graphics"),
        MEMORY("memory"),
        PORTABLES("portables"),
        HASHING("hashing"),
        LIGHT("light");

        private static final StableNames<Kind> NAMES = StableNames.of(Kind.class);

        private final String serializedName;

        Kind(final String serializedName) {
            this.serializedName = serializedName;
        }

        /** The kind of that name; the processors' for a name of none. */
        public static Kind of(final String name) {
            return NAMES.byName(name, PROCESSORS);
        }

        @Override
        public String serializedName() {
            return serializedName;
        }
    }

    /**
     * One mask a station writes.
     *
     * @param kind the program's list it is on
     * @param era  the era it is of, whose computer it asks for the one before
     * @param mask the mask
     * @param name what the program calls it: its microarchitecture's or its family's name and its kind, its era too
     *             on a station of two eras
     */
    public record Offer(Kind kind, Era era, Item mask, Text name) {

        /** The era of the computer that writes it, at the least: the one before its own. */
        public Era computer() {
            return LithoLink.before(era);
        }

        /** The era of the computer that writes it in half the time, two eras newer; none past the last era. */
        public Optional<Era> faster() {
            final int at = HardwareEra.SERIES.indexOf(computer()) + 2;
            return at < HardwareEra.WITH_PARTS.size() ? Optional.of(HardwareEra.SERIES.get(at)) : Optional.empty();
        }
    }

    /**
     * A mask being written.
     *
     * @param slot the input slot its blank stands in
     * @param mask the mask
     * @param era  the era it is of
     * @param work the station's work writing it
     */
    record Writing(int slot, Item mask, Era era, Processing work) {
    }
}
