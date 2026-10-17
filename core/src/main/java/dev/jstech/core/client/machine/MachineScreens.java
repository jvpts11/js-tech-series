/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The screens of the machines that have one of their own: every machine opens the same menu, and the standard screen
 * ({@link MachineScreen}) unless a screen is registered here for it. A machine with a face too different for the
 * standard screen (a chunk loader's map) draws its own, and still shows the tabs every machine shares where it has
 * them.
 *
 * <pre>{@code
 * MachineScreens.register(machine -> machine instanceof MyLoaderBlockEntity, MyLoaderScreen::new);  // client setup
 * }</pre>
 */
public final class MachineScreens {

    private static final List<Entry> SCREENS = new CopyOnWriteArrayList<>();

    private MachineScreens() {
    }

    /** Opens {@code factory}'s screen for every machine {@code machines} accepts, from the mod's client setup. */
    public static void register(final Predicate<MachineBlockEntity> machines, final IScreenFactory factory) {
        SCREENS.add(new Entry(Objects.requireNonNull(machines, "machines"), Objects.requireNonNull(factory,
                "factory")));
    }

    /** The screen of the machine {@code menu} is open on: its own, or the standard one. */
    public static CoreContainerScreen<MachineMenu> open(final MachineMenu menu, final Inventory inventory,
                                                       final Component title) {
        for (final Entry entry : SCREENS) {
            if (entry.machines().test(menu.machine())) {
                return entry.factory().create(menu, inventory, title);
            }
        }
        return new MachineScreen(menu, inventory, title);
    }

    /** Makes a machine's own screen. */
    @FunctionalInterface
    public interface IScreenFactory {

        CoreContainerScreen<MachineMenu> create(MachineMenu menu, Inventory inventory, Component title);
    }

    private record Entry(Predicate<MachineBlockEntity> machines, IScreenFactory factory) {
    }
}
