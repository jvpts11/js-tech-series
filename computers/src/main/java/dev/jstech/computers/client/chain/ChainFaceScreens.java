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
import dev.jstech.computers.printer.LargeScalePrinterBlockEntity;
import dev.jstech.core.client.machine.IMachineFace;
import dev.jstech.core.client.machine.MachineFaces;
import dev.jstech.core.client.machine.MachineView;
import dev.jstech.core.client.machine.PictureFace;
import dev.jstech.core.machine.Byproduct;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.text.GameText;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
    private static final String NO_CHANCE = " --%";
    private static final int PERCENT = 100;

    private ChainFaceScreens() {
    }

    /** Gives every machine with a face of its own that face, once the game's client is set up. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(ChainFaceScreens::register));
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
        }
    }

    private static ResourceLocation frames(final String face) {
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
                return String.format(Locale.ROOT, "%3d%%", Math.round(byproducts.get().getFirst().chance()
                        * PERCENT));
            }
        }
        return NO_CHANCE;
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
