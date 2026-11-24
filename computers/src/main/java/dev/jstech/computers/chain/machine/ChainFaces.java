/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.machine.FacePicture;
import dev.jstech.core.machine.FaceSlot;
import dev.jstech.core.machine.MachineScreenLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Each chain machine's own face (computers/docs/design/survival.md): the machine drawn at work on its Main tab, as its
 * approved design draws it, its ports let into its body where the menu places its slots. The picture and where its
 * parts stand are read from the mod's resources, dev/jstech/computers/chain/faces/{@code <face>}.json, on both sides
 * alike, each port with the slot it shows. A machine's face is named after the machine; a machine that draws another
 * face for some of its recipes has that one too, named after the machine and the recipes (the Optical Polisher's
 * fibre). A machine without one keeps the standard face.
 */
public final class ChainFaces {

    /** The face the Optical Polisher shows while it draws fibre. */
    public static final String POLISHER_FIBRE = "optical_polisher_fibre";

    private static final String FOLDER = "/dev/jstech/computers/chain/faces/";
    private static final Map<String, Optional<FacePicture>> READ = new HashMap<>();

    private ChainFaces() {
    }

    /** {@code machine}'s own face, or none for a machine that keeps the standard one. */
    public static Optional<FacePicture> of(final ChainMachine machine) {
        return of(machine.id());
    }

    /** The face named {@code face}, or none when the mod has none by that name. */
    public static synchronized Optional<FacePicture> of(final String face) {
        return READ.computeIfAbsent(face, name -> FacePicture.read(ChainFaces.class, FOLDER + name + ".json"));
    }

    /**
     * Where {@code machine}'s own face shows its slots, the items' top left in the screen's pixels: each of its
     * picture's ports with its slot, then each bin of its row under the window; none for a machine with no face of
     * its own.
     */
    public static List<FaceSlot> slots(final ChainMachine machine) {
        final Optional<FacePicture> picture = of(machine);
        if (picture.isEmpty()) {
            return List.of();
        }
        final List<FaceSlot> out = new ArrayList<>();
        for (final FacePicture.Port port : picture.get().ports()) {
            // The port's slot is an 18-pixel frame; the item stands a pixel in from its corner.
            out.add(new FaceSlot(port.slot(), MachineScreenLayout.faceX(port.x()) + 1,
                    MachineScreenLayout.faceY(port.y()) + 1, machine.layout().isOutput(port.slot())));
        }
        picture.get().tray().ifPresent(tray -> {
            for (int i = 0; i < tray.slots().size(); i++) {
                final int slot = tray.slots().get(i);
                final GuiLayout.SlotPosition at = MachineScreenLayout.traySlot(i);
                out.add(new FaceSlot(slot, at.x(), at.y(), machine.layout().isOutput(slot)));
            }
        });
        return out;
    }
}
