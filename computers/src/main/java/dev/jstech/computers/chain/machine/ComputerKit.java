/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.CaseFanSpec;
import dev.jstech.computers.hardware.CoolerSpec;
import dev.jstech.computers.hardware.CpuSpec;
import dev.jstech.computers.hardware.DiskSpec;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.hardware.MotherboardSpec;
import dev.jstech.computers.hardware.PsuSpec;
import dev.jstech.computers.hardware.RamSpec;
import dev.jstech.computers.item.ServerHardwareHandler;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

/**
 * A computer's case and its parts put together by the rules of the computer's own assembly window, as the Prebuilder
 * puts them together (computers/docs/design/survival.md): the board first, then each part in the first seat that
 * takes it, the processors before their coolers and the coolers before the fans. A part no seat takes refuses the
 * whole kit, saying why, as the assembly window says it. It builds every computer that is one block, and every rack
 * server; what comes out carries its parts inside.
 */
@TextHolder
public final class ComputerKit {

    /** What it says when none of what it was given is a case it builds in. */
    public static final TextKey NOT_A_CASE = TextKey.of("jsc.prebuilder.not_a_case",
            "No empty computer case among its parts: it builds Personal, Crafting and Cluster Management Computers"
                    + " and rack servers");
    /** What it says of a part no seat of the case takes, when the case names no rule. */
    public static final TextKey NO_PLACE = TextKey.of("jsc.prebuilder.no_place", "No place in the case for %s");
    /* The order parts go in, each kind's place in it: the board first, the fans last. */
    private static final List<Class<?>> ORDER = List.of(MotherboardSpec.class, CpuSpec.class, CoolerSpec.class,
            RamSpec.class, IExpansionCardSpec.class, PsuSpec.class, DiskSpec.class, BayDriveSpec.class,
            CaseFanSpec.class);

    private ComputerKit() {
    }

    /** Whether {@code stack} is an empty case it builds in: a one-block computer's, or a rack server's. */
    public static boolean isCase(final ItemStack stack) {
        if (stack.getItem() instanceof ServerItem) {
            return !ServerItem.hardware(stack).nonEmptyItems().iterator().hasNext();
        }
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof AbstractSmallComputerBlock<?>
                && !stack.has(ComputingComponents.ASSEMBLED_HARDWARE.get());
    }

    /** {@code theCase}, an empty case, with every one of {@code parts} in its seat, or why they do not go in. */
    public static Built assemble(final ItemStack theCase, final List<ItemStack> parts) {
        final List<ItemStack> ordered = new ArrayList<>();
        for (final ItemStack part : parts) {
            for (int i = 0; i < part.getCount(); i++) {
                ordered.add(part.copyWithCount(1));
            }
        }
        ordered.sort(Comparator.comparingInt(ComputerKit::rank));
        if (theCase.getItem() instanceof ServerItem) {
            final ItemStack server = theCase.copyWithCount(1);
            final ServerHardwareHandler seats = new ServerHardwareHandler(server);
            final Text refused = seat(seats, ordered, seats::isItemValid, seats::refusal);
            return refused.isEmpty() ? new Built(server, Text.EMPTY) : new Built(ItemStack.EMPTY, refused);
        }
        if (!(theCase.getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof AbstractSmallComputerBlock<?> block)) {
            return new Built(ItemStack.EMPTY, NOT_A_CASE.text());
        }
        // The rules are the computer's own: a computer that stands nowhere is put together and asked.
        final BlockEntity entity = block.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        if (!(entity instanceof AbstractSmallComputerBlockEntity computer)) {
            return new Built(ItemStack.EMPTY, NOT_A_CASE.text());
        }
        final Text refused = seat(computer.getHardware(), ordered, computer::isValidForSlot, computer::refusal);
        if (!refused.isEmpty()) {
            return new Built(ItemStack.EMPTY, refused);
        }
        final ItemStack built = new ItemStack(block);
        built.applyComponents(computer.collectComponents());
        built.set(ComputingComponents.NEVER_STARTED.get(), Unit.INSTANCE);
        return new Built(built, Text.EMPTY);
    }

    /** Where {@code part} comes in the order parts go in: the board first, the fans last, anything else after. */
    public static int rank(final ItemStack part) {
        final HardwareRegistry.Part found = HardwareRegistry.part(part.getItem());
        if (found != null) {
            for (int i = 0; i < ORDER.size(); i++) {
                if (ORDER.get(i).isInstance(found.spec())) {
                    return i;
                }
            }
        }
        return ORDER.size();
    }

    /*
     * Each part in the first empty seat {@code fits} takes it; for a part none takes, why the first seat that names a
     * rule refused it, or none. Nothing when every part is in.
     */
    private static Text seat(final IItemHandlerModifiable seats, final List<ItemStack> parts,
                             final BiPredicate<Integer, ItemStack> fits,
                             final BiFunction<Integer, ItemStack, TextKey> refusal) {
        for (final ItemStack part : parts) {
            int seat = -1;
            TextKey why = null;
            for (int slot = 0; slot < seats.getSlots() && seat < 0; slot++) {
                if (!seats.getStackInSlot(slot).isEmpty()) {
                    continue;
                }
                if (fits.test(slot, part)) {
                    seat = slot;
                } else if (why == null) {
                    final TextKey refused = refusal.apply(slot, part);
                    why = refused == PartRefusals.NOT_THIS_SLOT ? null : refused;
                }
            }
            if (seat < 0) {
                return why != null ? why.text() : NO_PLACE.with(GameText.of(part.getHoverName()));
            }
            seats.setStackInSlot(seat, part);
        }
        return Text.EMPTY;
    }

    /**
     * What putting a kit together came to: the computer with its parts inside, or why it was refused.
     *
     * @param computer the computer built; empty when it was refused
     * @param refusal  why it was refused; empty when it was built
     */
    public record Built(ItemStack computer, Text refusal) {

        /** Whether the kit went together. */
        public boolean built() {
            return !computer.isEmpty();
        }
    }
}
