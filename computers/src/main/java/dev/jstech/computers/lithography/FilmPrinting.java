/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.lithography;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.Printers;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;

/**
 * Printing a film on the computer's printer (computers/docs/design/survival.md): the lithography program has a
 * printer linked to its computer print the film it picks on a Blank Film from the printer's tray, as a board's artwork
 * was plotted on film. The films are the Vintage's Layouts, each chip's drawing, which its Lithography Station writes
 * the chip's mask from, and each era's phototool, its etched board's layers, which a job lends the Board Etcher and
 * never uses up. A list a kind: the Layouts and the phototools.
 */
@TextHolder
public final class FilmPrinting {

    /** Why a film is not printed: there is no such film. */
    public static final TextKey NOT_OFFERED = TextKey.of("jsc.lithography.refused.no_film",
            "The printer prints no such film");
    /** Why a film is not printed: the printer's tray holds no Blank Film. */
    public static final TextKey NO_BLANK = TextKey.of("jsc.lithography.refused.no_blank_film",
            "Put Blank Film in the printer's tray to print a film on it");
    /** Why a film is not printed: the printer's queue has no room. */
    public static final TextKey QUEUE_FULL = TextKey.of("jsc.lithography.refused.queue_full",
            "The printer's queue is full: print the film when it has room");
    private static final TextKey LAYOUTS = TextKey.of("jsc.lithography.kind.layouts", "Layouts");
    private static final TextKey PHOTOTOOLS = TextKey.of("jsc.lithography.kind.phototools", "Phototools");
    private static final TextKey MEMORY_LAYOUT = TextKey.of("jsc.lithography.film.memory_layout", "Memory layout");
    private static final TextKey CONTROLLER_LAYOUT = TextKey.of("jsc.lithography.film.controller_layout",
            "Controller layout");
    private static final TextKey PROCESSOR_LAYOUT = TextKey.of("jsc.lithography.film.processor_layout",
            "%s processor layout");
    private static final TextKey GRAPHICS_LAYOUT = TextKey.of("jsc.lithography.film.graphics_layout",
            "%s graphics layout");
    private static final TextKey PHOTOTOOL = TextKey.of("jsc.lithography.film.phototool", "%s board phototool");
    /* The program a film's job comes from, as the printer's queue keeps it. */
    private static final String PROGRAM = "lithography";

    private FilmPrinting() {
    }

    /** Every film a printer prints, in the order of the program's lists: the Layouts, then the phototools by era. */
    public static List<Offer> offers() {
        final List<Offer> out = new ArrayList<>();
        out.add(new Offer(Kind.LAYOUTS, ChainChips.MEMORY_LAYOUT.get(), MEMORY_LAYOUT.text()));
        out.add(new Offer(Kind.LAYOUTS, ChainChips.CONTROLLER_LAYOUT.get(), CONTROLLER_LAYOUT.text()));
        // A die is named by its microarchitecture, the same in every language.
        for (final ChainChips.Layout layout : ChainChips.PROCESSOR_LAYOUTS) {
            out.add(new Offer(Kind.LAYOUTS, layout.item().get(),
                    PROCESSOR_LAYOUT.with(Text.literal(layout.english()))));
        }
        for (final ChainChips.Layout layout : ChainChips.GRAPHICS_LAYOUTS) {
            out.add(new Offer(Kind.LAYOUTS, layout.item().get(),
                    GRAPHICS_LAYOUT.with(Text.literal(layout.english()))));
        }
        for (int i = 0; i < ChainParts.ERAS.size(); i++) {
            final Era era = ChainParts.ERAS.get(i);
            out.add(new Offer(Kind.PHOTOTOOLS, ChainParts.PHOTOTOOLS.get(i).get(), PHOTOTOOL.with(era.name())));
        }
        return List.copyOf(out);
    }

    /** The film {@code film} as the lithography program names it; none for an item that is no film. */
    public static Optional<Offer> offerOf(final Item film) {
        return offers().stream().filter(offer -> offer.film() == film).findFirst();
    }

    /**
     * Has {@code printer} print {@code film} for {@code computer}, on a Blank Film from its tray; why it does not, when
     * it does not: no such film, no Blank Film in the tray, or no room in its queue.
     */
    public static Optional<TextKey> print(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                          final PrinterBlockEntity printer, final Item film) {
        if (offerOf(film).isEmpty()) {
            return Optional.of(NOT_OFFERED);
        }
        if (!printer.paper().getStackInSlot(0).is(ChainChips.BLANK_FILM.get())) {
            return Optional.of(NO_BLANK);
        }
        final String machine = Printers.machineName(computer);
        final PrintedDocument named = PrintedDocument.text(BuiltInRegistries.ITEM.getKey(film).getPath(), machine,
                PROGRAM, List.of(""));
        return printer.submitFilm(named, film, machine, level.getGameTime()) == null ? Optional.of(QUEUE_FULL)
                : Optional.empty();
    }

    /** What the program calls the list of {@code kind}. */
    public static Text title(final Kind kind) {
        return switch (kind) {
            case LAYOUTS -> LAYOUTS.text();
            case PHOTOTOOLS -> PHOTOTOOLS.text();
        };
    }

    /** A kind of film, a list of the program. */
    public enum Kind {
        LAYOUTS,
        PHOTOTOOLS;

        /** Its name as the program's picks carry it. */
        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The kind of {@code name}, or none for a name of no kind of film. */
        public static Optional<Kind> named(final String name) {
            for (final Kind kind : values()) {
                if (kind.serializedName().equals(name)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }
    }

    /**
     * A film the program offers: its kind, its item and what the program calls it, "Vintage board phototool".
     *
     * @param kind its kind
     * @param film its item
     * @param name what the program calls it
     */
    public record Offer(Kind kind, Item film, Text name) {
    }
}
