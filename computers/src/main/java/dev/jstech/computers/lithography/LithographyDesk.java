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
import dev.jstech.computers.chain.machine.LithoLink;
import dev.jstech.computers.chain.machine.LithoStations;
import dev.jstech.computers.chain.machine.MaskWriting;
import dev.jstech.computers.chain.machine.MoulderBlockEntity;
import dev.jstech.computers.chain.machine.MouldCutting;
import dev.jstech.computers.chain.machine.WaferMachineBlockEntity;
import dev.jstech.computers.chain.machine.WaferWork;
import dev.jstech.computers.printer.Printers;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The lithography program's side on the computer (computers/docs/design/survival.md, "Masks"): what it shows of the
 * Lithography Stations and the Injection Moulders linked to its computer and of its printers, read off each machine,
 * and the mask it has a station write, the mould it has a moulder cut or the film it has a printer print. Both of the
 * program's forms, its text screen on MC-DOS and its window on the desktop systems, show what this reads.
 */
@TextHolder
public final class LithographyDesk {

    private static final TextKey STATION = TextKey.of("jsc.lithography.station", "%s, %s");
    private static final TextKey KRF = TextKey.of("jsc.lithography.light.krf", "KrF");
    private static final TextKey ARF = TextKey.of("jsc.lithography.light.arf", "ArF");
    private static final TextKey EUV = TextKey.of("jsc.lithography.light.euv", "EUV");
    private static final TextKey IDLE = TextKey.of("jsc.lithography.status.idle", "Idle");
    private static final TextKey WRITING = TextKey.of("jsc.lithography.status.writing", "Writing a mask");
    private static final TextKey EXPOSING = TextKey.of("jsc.lithography.status.exposing", "Exposing layer %s of %s");
    private static final TextKey NONE = TextKey.of("jsc.lithography.none", "none");
    private static final TextKey EMPTY = TextKey.of("jsc.lithography.slot.empty", "empty");
    private static final TextKey MASK = TextKey.of("jsc.lithography.slot.mask", "%s mask");
    private static final TextKey MOULD = TextKey.of("jsc.lithography.slot.mould", "%s mould");
    private static final TextKey TRAY = TextKey.of("jsc.lithography.tray", "%s x%s");
    private static final TextKey PHOTORESIST = TextKey.of("jsc.lithography.supply.photoresist", "Photoresist");
    private static final TextKey KRYPTON = TextKey.of("jsc.lithography.supply.krypton", "Krypton");
    private static final TextKey ARGON = TextKey.of("jsc.lithography.supply.argon", "Argon");
    private static final TextKey WATER = TextKey.of("jsc.lithography.supply.water", "Water");
    private static final int PERMILLE = 1000;
    private static final int TICKS_A_SECOND = 20;

    private LithographyDesk() {
    }

    /**
     * What the program shows of the station {@code index} among those {@code computer} works for, the first when
     * there are fewer; nothing when it works for none.
     */
    public static LithographyLook look(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                       final int index) {
        final List<BlockEntity> stations = worksFor(level, computer);
        if (stations.isEmpty()) {
            return LithographyLook.NONE;
        }
        final int shown = index >= 0 && index < stations.size() ? index : 0;
        if (stations.get(shown) instanceof MoulderBlockEntity moulder) {
            return moulder(level, computer, stations.size(), shown, moulder);
        }
        if (stations.get(shown) instanceof PrinterBlockEntity printer) {
            return printer(stations.size(), shown, printer);
        }
        final WaferMachineBlockEntity station = (WaferMachineBlockEntity) stations.get(shown);
        final List<MaskWriting.Offer> offers = MaskWriting.offers(station.machine(), computer.displayEra());
        final List<LithographyLook.Pick> picks = new ArrayList<>();
        for (final MaskWriting.Offer offer : offers) {
            picks.add(new LithographyLook.Pick(offer.kind().serializedName(),
                    BuiltInRegistries.ITEM.getKey(offer.mask()), offer.name()));
        }
        final ItemStack mask = maskIn(station);
        final boolean blank = mask.is(ChainChips.MASK_BLANK.get());
        final Item writes = station.writes();
        final boolean writing = writes != null && blank && !station.exposing();
        final Optional<WaferWork> exposure = station.exposure();
        final int permille = station.getMaxProgress() <= 0 ? 0
                : (int) ((long) station.getProgress() * PERMILLE / station.getMaxProgress());
        final int seconds = Math.max(0, station.getMaxProgress() - station.getProgress()) / TICKS_A_SECOND;
        final Text status = exposure.<Text>map(work -> EXPOSING.with(work.layer(), work.layers()))
                .orElse(writing ? WRITING.text() : IDLE.text());
        return new LithographyLook(stations.size(), shown, STATION.with(eraOf(station).name(), light(station)),
                status, exposure.isPresent() ? nameOf(offers, mask) : NONE.text(), exposure.isPresent(),
                exposure.isPresent() ? permille : 0, exposure.isPresent() ? seconds : 0,
                LithoLink.heldMb(computer, station.getBlockPos(), level.getGameTime()), supplies(station),
                slotOf(offers, mask), blank, !mask.isEmpty() && !blank, picks,
                writing ? nameOf(offers, new ItemStack(writes)) : Text.EMPTY,
                writing ? permille : 0, writing ? seconds : 0, LithographyLook.MachineKind.STATION);
    }

    /**
     * Has the station {@code index} among those {@code computer} works for write the mask {@code mask}; why it does
     * not, when it does not, or that no such station is there.
     */
    public static Optional<TextKey> write(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                          final int index, final ResourceLocation mask) {
        final List<BlockEntity> stations = worksFor(level, computer);
        final Item item = BuiltInRegistries.ITEM.get(mask);
        if (index < 0 || index >= stations.size() || item == Items.AIR) {
            return Optional.of(MaskWriting.NOT_OFFERED);
        }
        // An Injection Moulder cuts the mould picked; a printer prints the film; a station writes the mask.
        if (stations.get(index) instanceof MoulderBlockEntity moulder) {
            return MouldCutting.cut(moulder, item);
        }
        if (stations.get(index) instanceof PrinterBlockEntity printer) {
            return FilmPrinting.print(level, computer, printer, item);
        }
        return MaskWriting.write((WaferMachineBlockEntity) stations.get(index), computer.displayEra(), item);
    }

    /* The machines {@code computer} works for: the stations and moulders linked to it, then its printers. */
    private static List<BlockEntity> worksFor(final ServerLevel level, final AbstractComputerBlockEntity computer) {
        final List<BlockEntity> out = new ArrayList<>(LithoStations.of(level, computer));
        out.addAll(Printers.of(level, computer));
        return out;
    }

    /*
     * What the program shows of a printer: what it is doing, what its tray holds, the films it prints on a Blank Film
     * and the one it is printing, how far through its page.
     */
    private static LithographyLook printer(final int machines, final int shown, final PrinterBlockEntity printer) {
        final List<LithographyLook.Pick> picks = new ArrayList<>();
        for (final FilmPrinting.Offer offer : FilmPrinting.offers()) {
            picks.add(new LithographyLook.Pick(offer.kind().serializedName(),
                    BuiltInRegistries.ITEM.getKey(offer.film()), offer.name()));
        }
        final ItemStack tray = printer.paper().getStackInSlot(0);
        final Optional<PrinterBlockEntity.PrintJob> film = printer.jobs().stream().findFirst()
                .filter(PrinterBlockEntity.PrintJob::printsFilm);
        final Text named = film.flatMap(job -> FilmPrinting.offerOf(BuiltInRegistries.ITEM.get(job.film())))
                .map(FilmPrinting.Offer::name).orElse(Text.EMPTY);
        final int page = printer.model().pageTicks();
        final boolean printing = film.isPresent() && printer.printing();
        return new LithographyLook(machines, shown, printer.model().displayName().text(), Printers.status(printer),
                tray.isEmpty() ? EMPTY.text() : TRAY.with(GameText.of(tray.getHoverName()), tray.getCount()), false,
                0, 0, 0, List.of(), Text.EMPTY, tray.is(ChainChips.BLANK_FILM.get()), false, picks, named,
                printing ? printer.pageTicksDone() * PERMILLE / page : 0,
                printing ? Math.max(0, page - printer.pageTicksDone()) / TICKS_A_SECOND : 0,
                LithographyLook.MachineKind.PRINTER);
    }

    /*
     * What the program shows of an Injection Moulder: what it is doing, the plastic it holds and the part it moulds,
     * what its computer holds for it while it cuts, what stands in its mould slot, the moulds it cuts and the one it is
     * cutting.
     */
    private static LithographyLook moulder(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                           final int machines, final int shown, final MoulderBlockEntity moulder) {
        final List<MouldCutting.Offer> offers = MouldCutting.offers();
        final List<LithographyLook.Pick> picks = new ArrayList<>();
        for (final MouldCutting.Offer offer : offers) {
            picks.add(new LithographyLook.Pick(offer.kind().serializedName(),
                    BuiltInRegistries.ITEM.getKey(offer.mould()), offer.name()));
        }
        final ItemStack tool = firstOf(moulder.inputs(), LithographyDesk::mouldTool);
        final ItemStack plastic = firstOf(moulder.inputs(), held -> !held.isEmpty() && !mouldTool(held));
        final boolean blank = tool.is(ChainChips.MOULD_BLANK.get());
        final Item cuts = moulder.cuts();
        final boolean cutting = cuts != null && blank && !moulder.moulding();
        final int permille = moulder.getMaxProgress() <= 0 ? 0
                : (int) ((long) moulder.getProgress() * PERMILLE / moulder.getMaxProgress());
        final int seconds = Math.max(0, moulder.getMaxProgress() - moulder.getProgress()) / TICKS_A_SECOND;
        final Text status = moulder.moulding() ? MouldCutting.MOULDING_A_PART.text()
                : cutting ? MouldCutting.CUTTING_A_MOULD.text() : IDLE.text();
        final Text slot = tool.isEmpty() ? EMPTY.text() : blank ? GameText.of(tool.getHoverName())
                : MouldCutting.offerOf(tool.getItem()).map(offer -> MOULD.with(offer.name()))
                        .orElseGet(() -> GameText.of(tool.getHoverName()));
        final Text named = cutting ? MouldCutting.offerOf(cuts).map(MouldCutting.Offer::name).orElse(Text.EMPTY)
                : Text.EMPTY;
        return new LithographyLook(machines, shown, moulder.machine().nameKey().text(), status,
                plastic.isEmpty() ? NONE.text() : GameText.of(plastic.getHoverName()), moulder.moulding(),
                moulder.moulding() ? permille : 0, moulder.moulding() ? seconds : 0,
                MouldCutting.heldMb(computer, moulder, level.getGameTime()), List.of(), slot, blank,
                !tool.isEmpty() && !blank, picks, named, cutting ? permille : 0, cutting ? seconds : 0,
                LithographyLook.MachineKind.MOULDER);
    }

    /* Whether {@code stack} is a moulder's tool or what one is cut on: a Mould Blank or a mould. */
    private static boolean mouldTool(final ItemStack stack) {
        return stack.is(ChainChips.MOULD_BLANK.get()) || MouldCutting.offerOf(stack.getItem()).isPresent();
    }

    /* The first of {@code stacks} that {@code wanted} accepts; nothing when none does. */
    private static ItemStack firstOf(final List<ItemStack> stacks, final Predicate<ItemStack> wanted) {
        return stacks.stream().filter(wanted).findFirst().orElse(ItemStack.EMPTY);
    }

    /* The mask or the blank among the station's inputs, the first; nothing when it holds neither. */
    private static ItemStack maskIn(final WaferMachineBlockEntity station) {
        for (final ItemStack stack : station.inputs()) {
            if (stack.is(ChainChips.MASK_BLANK.get()) || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()
                    .startsWith("mask_")) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /* What stands in the mask slot: the blank's name, the mask by what the program calls it, or nothing. */
    private static Text slotOf(final List<MaskWriting.Offer> offers, final ItemStack mask) {
        if (mask.isEmpty()) {
            return EMPTY.text();
        }
        if (mask.is(ChainChips.MASK_BLANK.get())) {
            return GameText.of(mask.getHoverName());
        }
        return offers.stream().filter(offer -> mask.is(offer.mask())).findFirst()
                .map(offer -> MASK.with(offer.name())).orElseGet(() -> GameText.of(mask.getHoverName()));
    }

    /* What the program calls the mask {@code mask}, its item's name for one the station does not write. */
    private static Text nameOf(final List<MaskWriting.Offer> offers, final ItemStack mask) {
        return offers.stream().filter(offer -> mask.is(offer.mask())).findFirst().map(MaskWriting.Offer::name)
                .orElseGet(() -> GameText.of(mask.getHoverName()));
    }

    /* The era the station is named by: its own, the first it serves. */
    private static Era eraOf(final WaferMachineBlockEntity station) {
        final Era era = station.machine().era();
        return era == null ? HardwareEra.LEGACY : era;
    }

    /* The light it exposes with, as the program names it. */
    private static Text light(final WaferMachineBlockEntity station) {
        final Era era = eraOf(station);
        if (era == HardwareEra.LEGACY) {
            return KRF.text();
        }
        return era == HardwareEra.ADVANCED ? EUV.text() : ARF.text();
    }

    /*
     * The liquids and gases the station takes for each layer, with what it holds of each: photoresist always; the
     * Legacy's krypton for its laser; the ArF station's argon and its ultrapure water for the immersion.
     */
    private static List<LithographyLook.Supply> supplies(final WaferMachineBlockEntity station) {
        final List<LithographyLook.Supply> out = new ArrayList<>();
        out.add(liquid(station, SeriesChemicals.PHOTORESIST, PHOTORESIST));
        final Era era = eraOf(station);
        if (era == HardwareEra.LEGACY) {
            out.add(gas(station, Elements.of(Element.KRYPTON), KRYPTON));
        } else if (era == HardwareEra.TRANSITION || era == HardwareEra.STANDARD) {
            out.add(gas(station, Elements.of(Element.ARGON), ARGON));
            out.add(liquid(station, SeriesChemicals.ULTRAPURE_WATER, WATER));
        }
        return out;
    }

    private static LithographyLook.Supply liquid(final WaferMachineBlockEntity station, final Material liquid,
                                                 final TextKey named) {
        long held = 0;
        for (int i = 0; i < station.layout().inputTanks(); i++) {
            if (station.inputTank(i).getFluid().getFluid() == MaterialLiquids.fluid(liquid).get()) {
                held += station.inputTank(i).getFluidAmount();
            }
        }
        return new LithographyLook.Supply(named.text(), held);
    }

    private static LithographyLook.Supply gas(final WaferMachineBlockEntity station, final Material gas,
                                              final TextKey named) {
        long held = 0;
        for (final MatterTank tank : station.matter().inputs(MatterState.GAS)) {
            held += tank.held().filter(stack -> stack.material() == gas).map(stack -> stack.amount()).orElse(0L);
        }
        return new LithographyLook.Supply(named.text(), held);
    }
}
