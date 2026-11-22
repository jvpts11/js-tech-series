/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.machine.ChainFaceWord;
import dev.jstech.computers.chain.machine.ChainFaces;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.client.PrinterQueueTab;
import dev.jstech.computers.plant.CoolingPlant;
import dev.jstech.computers.printer.LargeScalePrinterBlockEntity;
import com.google.common.base.Suppliers;
import dev.jstech.core.client.machine.IMachineFace;
import dev.jstech.core.client.machine.MachineChrome;
import dev.jstech.core.client.machine.MachineFaces;
import dev.jstech.core.client.machine.MachineView;
import dev.jstech.core.client.machine.PictureFace;
import dev.jstech.core.client.recipeview.RecipeFaces;
import dev.jstech.core.machine.Byproduct;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.ProcessingRecipe;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Each chain machine's Main tab as its own face (ChainFaces): the machine drawn at work, its frames from
 * textures/gui/machine/face/{@code <face>}.png, its stencilled words in the player's language and its readings live;
 * a machine without one keeps the standard face.
 */
public final class ChainFaceScreens {

    /* The word of a face's file the game puts the running recipe's byproduct chance in for. */
    private static final String BYPRODUCT = "{byproduct}";
    /* The word of the printer's face the game puts the sheets in its feeder in for, on its counter. */
    private static final String SHEETS = "{sheets}";
    private static final String PRINTER = "large_scale_printer";
    private static final String CDU = "cdu";
    private static final String NO_CHANCE = " --%";
    private static final int PERCENT = 100;

    private ChainFaceScreens() {
    }

    /**
     * Gives every machine with a face of its own that face, on its screen and on its recipes' pages in the recipe
     * viewers, once the game's client is set up.
     */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(ChainFaceScreens::register));
    }

    /** A word written on a face in the player's language: its stencilled words and its name plate; data as written. */
    public static String translated(final String written) {
        // A name plate's few words are one sentence; anything else, each word on its own.
        final Optional<ChainFaceWord> whole = ChainFaceWord.of(written);
        if (whole.isPresent()) {
            return GameText.resolve(whole.get().text().text());
        }
        final String[] words = written.split(" ", -1);
        for (int i = 0; i < words.length; i++) {
            final Optional<ChainFaceWord> word = ChainFaceWord.of(words[i]);
            if (word.isPresent()) {
                words[i] = GameText.resolve(word.get().text().text());
            }
        }
        return String.join(" ", words);
    }

    private static void register() {
        for (final ChainMachine machine : ChainMachines.all()) {
            final Optional<FacePicture> picture = ChainFaces.of(machine);
            if (picture.isEmpty()) {
                continue;
            }
            final PictureFace own = new PictureFace(frames(machine.id()), picture.get(), ChainFaceScreens::say);
            final Optional<FacePicture> fibre = machine.id().equals("optical_polisher")
                    ? ChainFaces.of(ChainFaces.POLISHER_FIBRE) : Optional.empty();
            if (fibre.isPresent()) {
                final PictureFace drawing = new PictureFace(frames(ChainFaces.POLISHER_FIBRE), fibre.get(),
                        ChainFaceScreens::say);
                MachineFaces.register(ChainMachines.typeOf(machine), terminal -> new Polisher(own, drawing));
            } else {
                MachineFaces.register(ChainMachines.typeOf(machine), terminal -> own);
            }
            if (machine.id().equals(PRINTER)) {
                // The printer's queue, pause, cancel and lever on a tab of its own after the five.
                MachineFaces.registerTabs(ChainMachines.typeOf(machine), List.of(PrinterQueueTab.tab()));
            }
            recipeFaces(machine, picture.get(), fibre);
        }
        // The cooling plant's CDU, a big block rather than a chain machine, drawn on the same faces.
        ChainFaces.of(CDU).ifPresent(picture -> {
            final PictureFace own = new PictureFace(frames(CDU), picture, ChainFaceScreens::say);
            MachineFaces.register(CoolingPlant.CDU_BE.get(), cdu -> own);
        });
    }

    /*
     * Lays each kind the machine is the first machine of on its face in the recipe viewers, the polisher's fibres (the
     * recipes that take two things) on its fibre drawing tower. A kind first worked elsewhere, as a bench's, keeps the
     * page its own machine gives it.
     */
    private static void recipeFaces(final ChainMachine machine, final FacePicture picture,
                                    final Optional<FacePicture> fibre) {
        final Item terminal = ChainMachines.terminalOf(machine).get().asItem();
        final Supplier<String> rating = rating(machine);
        for (final ChainMachines.Worked worked : ChainMachines.workedBy(machine)) {
            final List<ItemLike> machines = worked.kind().machines();
            if (machines.isEmpty() || machines.getFirst().asItem() != terminal) {
                continue;
            }
            fibre.ifPresent(drawing -> RecipeFaces.register(worked.kind().id(), recipeFace(ChainFaces.POLISHER_FIBRE,
                    drawing, machine, rating), recipe -> recipe.inputs().size() > 1));
            RecipeFaces.register(worked.kind().id(), recipeFace(machine.id(), picture, machine, rating));
        }
    }

    private static RecipeFaces.RecipeFace recipeFace(final String face, final FacePicture picture,
                                                     final ChainMachine machine, final Supplier<String> rating) {
        return new RecipeFaces.RecipeFace(frames(face), picture, machine.layout(), picture.plate()
                .orElse(machine.english()), rating, ChainFaceScreens::said);
    }

    /* What {@code machine}'s class plate says, read from a block entity of it made the first time it is asked. */
    private static Supplier<String> rating(final ChainMachine machine) {
        final Supplier<Optional<ChainMachineBlockEntity>> made = Suppliers.memoize(() -> Optional.ofNullable(
                ChainMachines.typeOf(machine).create(BlockPos.ZERO, ChainMachines.terminalOf(machine).get()
                        .defaultBlockState())));
        return () -> made.get().map(MachineChrome::rating).orElse("");
    }

    /* The frames of the face {@code face}, as the machine's screen and its recipes' pages draw them. */
    static ResourceLocation frames(final String face) {
        return ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "textures/gui/machine/face/" + face + ".png");
    }

    /*
     * What a word of a face says on {@code machine}: its words in the player's language, its data as written; the
     * byproduct's place, the chance the recipe of what the machine holds gives it.
     */
    private static String say(final MachineBlockEntity machine, final String written) {
        if (written.equals(BYPRODUCT)) {
            return byproductChance(machine);
        }
        if (written.equals(SHEETS)) {
            return machine instanceof LargeScalePrinterBlockEntity printer
                    ? String.format(Locale.ROOT, "%04d", printer.sheets()) : "";
        }
        return translated(written);
    }

    /*
     * What a word of a face says on a recipe viewer's page of {@code recipe}: its words in the player's language; the
     * byproduct's place, the chance the recipe gives its first byproduct; the printer's counter, nothing.
     */
    private static String said(final ProcessingRecipe recipe, final String written) {
        if (written.equals(BYPRODUCT)) {
            return recipe.byproducts().isEmpty() ? NO_CHANCE : chance(recipe.byproducts().getFirst());
        }
        return written.equals(SHEETS) ? "" : translated(written);
    }

    /* The chance, as a share of a hundred, of the first byproduct of the recipe the machine's inputs make. */
    private static String byproductChance(final MachineBlockEntity machine) {
        final Level level = machine.getLevel();
        if (!(machine instanceof ChainMachineBlockEntity chain) || level == null) {
            return NO_CHANCE;
        }
        for (final ChainMachines.Worked worked : ChainMachines.workedBy(chain.machine())) {
            final Optional<List<Byproduct>> byproducts = worked.kind().find(level, chain.input())
                    .map(holder -> holder.value().byproducts());
            if (byproducts.isPresent() && !byproducts.get().isEmpty()) {
                return chance(byproducts.get().getFirst());
            }
        }
        return NO_CHANCE;
    }

    /* How likely {@code byproduct} comes, as a share of a hundred. */
    private static String chance(final Byproduct byproduct) {
        return String.format(Locale.ROOT, "%3d%%", Math.round(byproduct.chance() * PERCENT));
    }

    /*
     * The Optical Polisher's face: the lens polisher, or the fibre drawing tower while its second slot holds
     * something (only the fibres' recipes take two things).
     */
    private record Polisher(PictureFace lens, PictureFace fibre) implements IMachineFace {

        private static final int SECOND_SLOT = 1;

        @Override
        public void draw(final MachineView v) {
            shown(v).draw(v);
        }

        @Override
        public void drawOver(final MachineView v) {
            shown(v).drawOver(v);
        }

        @Override
        public boolean clicked(final MachineView v, final double x, final double y, final int button) {
            return shown(v).clicked(v, x, y, button);
        }

        @Override
        public List<Component> tooltip(final MachineView v, final double x, final double y) {
            return shown(v).tooltip(v, x, y);
        }

        private PictureFace shown(final MachineView v) {
            return v.machine() instanceof ChainMachineBlockEntity chain
                    && !chain.getInventory().getStackInSlot(SECOND_SLOT).isEmpty() ? this.fibre : this.lens;
        }
    }
}
