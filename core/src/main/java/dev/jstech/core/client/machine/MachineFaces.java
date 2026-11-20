/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The faces machines show on their screen: on the Main tab the one a mod gave each kind of machine of its own, or the
 * standard face for every other; and the faces of the tabs of a machine's own after the five every machine has, which
 * its block entity counts ({@link MachineBlockEntity#ownTabs()}) and whose buttons it answers
 * ({@link MachineBlockEntity#pressed(int)}).
 *
 * <pre>{@code
 * MachineFaces.register(MY_CRUSHER.get(), CrusherFace::new);   // from the mod's client setup
 * MachineFaces.registerTabs(MY_PRINTER.get(), List.of(new MachineFaces.OwnTab(new ItemStack(Items.BOOK),
 *         QUEUE_NAME, QueueTab::new)));
 * }</pre>
 */
public final class MachineFaces {

    private static final Map<BlockEntityType<?>, Function<MachineBlockEntity, IMachineFace>> FACES =
            new ConcurrentHashMap<>();
    private static final Map<BlockEntityType<?>, List<OwnTab>> TABS = new ConcurrentHashMap<>();

    private MachineFaces() {
    }

    /** Gives the machines of {@code type} the face {@code face} makes for each, in place of the standard one. */
    public static void register(final BlockEntityType<?> type, final Function<MachineBlockEntity, IMachineFace> face) {
        FACES.put(Objects.requireNonNull(type, "type"), Objects.requireNonNull(face, "face"));
    }

    /** Gives the machines of {@code type} their own tabs, in the order they follow the five every machine has. */
    public static void registerTabs(final BlockEntityType<?> type, final List<OwnTab> tabs) {
        TABS.put(Objects.requireNonNull(type, "type"), List.copyOf(tabs));
    }

    /** The face {@code machine} shows on its Main tab. */
    public static IMachineFace of(final MachineBlockEntity machine) {
        final Function<MachineBlockEntity, IMachineFace> own = FACES.get(machine.getType());
        return own != null ? own.apply(machine) : new StandardFace();
    }

    /** The tabs of its own {@code machine}'s screen shows after the five; none for most machines. */
    public static List<OwnTab> tabsOf(final MachineBlockEntity machine) {
        return TABS.getOrDefault(machine.getType(), List.of());
    }

    /**
     * A tab of a machine's own.
     *
     * @param icon what its tab shows, drawn as an item
     * @param name its name, for the mouse over its tab
     * @param face the face it shows, made for each machine
     */
    public record OwnTab(ItemStack icon, TextKey name, Function<MachineBlockEntity, IMachineFace> face) {
    }
}
