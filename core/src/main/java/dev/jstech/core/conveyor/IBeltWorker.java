/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.conveyor;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A machine that works the items on a belt as they pass under it: a press, a washer, a painter. Its block entity
 * implements this and stands on top of a flat belt; the belt stops each item the machine works at its middle, under
 * the machine, keeps the items behind it waiting a gap back, and lets it go on as what the machine made of it once the
 * machine is done. The belt asks only while it moves.
 *
 * <pre>{@code
 * public class PressBlockEntity extends BlockEntity implements IBeltWorker {
 *     private int pressed;
 *
 *     public boolean works(ItemStack item) {
 *         return item.is(Items.IRON_INGOT);
 *     }
 *
 *     public @Nullable ItemStack work(ItemStack item) {
 *         if (++pressed < 20) {
 *             return null;               // a second under the press
 *         }
 *         pressed = 0;
 *         return new ItemStack(MyItems.IRON_PLATE.get(), item.getCount());
 *     }
 *
 *     public void abandoned() {
 *         pressed = 0;
 *     }
 * }
 * }</pre>
 */
public interface IBeltWorker {

    /** Whether it works {@code item} when it comes under it. It should not work again what it made. */
    boolean works(ItemStack item);

    /**
     * Works {@code item}, held under it, for a tick.
     *
     * @return what the item becomes once the work is done, empty when nothing is left of it; none while work goes on
     */
    @Nullable ItemStack work(ItemStack item);

    /** The item it was working went away before it was done, taken off the belt or the belt broken: it starts over. */
    default void abandoned() {
    }
}
