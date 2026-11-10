/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.registry.ComputingComponents;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * A computer drawn where no computer stands (computers/docs/design/survival.md): the one a machine works on, from the
 * item it will be, drawn by the small computers' own renderer with the parts it has so far, as the Preinstaller's
 * cradle carries it and the Prebuilder puts it together. One kept for each machine, the item's parts put in it again
 * only when what it shows changes.
 */
final class ComputerDummies {

    private static final Map<BlockEntity, Dummy> KEPT = new WeakHashMap<>();

    private ComputerDummies() {
    }

    /**
     * Draws {@code computer}, an item carrying its parts, its block's corner where {@code poses} stands, facing north,
     * with the parts in the slots {@code seated} takes, its side taken off when {@code open}.
     */
    static void draw(final BlockEntity owner, final ItemStack computer, final IntPredicate seated, final boolean open,
                     final PoseStack poses, final MultiBufferSource buffers, final int light, final int overlay,
                     final float partialTick) {
        final AbstractSmallComputerBlockEntity dummy = dummy(owner, computer, seated, open);
        if (dummy == null) {
            return;
        }
        final BlockEntityRenderer<AbstractSmallComputerBlockEntity> renderer =
                Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(dummy);
        if (renderer != null) {
            renderer.render(dummy, partialTick, poses, buffers, light, overlay);
        }
    }

    /** Where each part of {@code computer} sits, by its slot's key, from its block's corner; none for others. */
    static Map<String, Matrix4f> seats(final BlockEntity owner, final ItemStack computer) {
        final AbstractSmallComputerBlockEntity dummy = dummy(owner, computer, slot -> true, true);
        return dummy == null ? Map.of() : ComputerRenderer.seats(dummy);
    }

    /* The machine's computer, made again for another item, its parts set again only when the parts shown change. */
    private static @Nullable AbstractSmallComputerBlockEntity dummy(final BlockEntity owner, final ItemStack computer,
                                                                    final IntPredicate seated, final boolean open) {
        final ItemContainerContents parts = computer.getOrDefault(ComputingComponents.ASSEMBLED_HARDWARE.get(),
                ItemContainerContents.EMPTY);
        Dummy kept = KEPT.get(owner);
        // Parts are only ever put in: a computer that shows fewer is made again, as a part taken out of a computer is
        // a part taken from a machine that might be running.
        if (kept == null || !ItemStack.matches(kept.computer(), computer) || takesOut(kept.entity(), seated)) {
            kept = make(owner, computer);
            if (kept == null) {
                KEPT.remove(owner);
                return null;
            }
            KEPT.put(owner, kept);
        }
        final AbstractSmallComputerBlockEntity entity = kept.entity();
        if (entity.sidePanelOff() != open) {
            entity.toggleSidePanel();
        }
        final ItemStackHandler hardware = entity.getHardware();
        for (int slot = 0; slot < hardware.getSlots(); slot++) {
            final ItemStack wanted = slot < parts.getSlots() && seated.test(slot) ? parts.getStackInSlot(slot)
                    : ItemStack.EMPTY;
            if (!ItemStack.matches(hardware.getStackInSlot(slot), wanted)) {
                hardware.setStackInSlot(slot, wanted.copy());
            }
        }
        return entity;
    }

    /* Whether showing only the slots {@code seated} takes would take a part out of {@code entity}. */
    private static boolean takesOut(final AbstractSmallComputerBlockEntity entity, final IntPredicate seated) {
        final ItemStackHandler hardware = entity.getHardware();
        for (int slot = 0; slot < hardware.getSlots(); slot++) {
            if (!hardware.getStackInSlot(slot).isEmpty() && !seated.test(slot)) {
                return true;
            }
        }
        return false;
    }

    private static @Nullable Dummy make(final BlockEntity owner, final ItemStack computer) {
        if (!(computer.getItem() instanceof BlockItem item)
                || !(item.getBlock() instanceof AbstractSmallComputerBlock<?> block)
                || !(block.newBlockEntity(owner.getBlockPos(), block.defaultBlockState())
                instanceof AbstractSmallComputerBlockEntity entity)) {
            return null;
        }
        entity.setLevel(owner.getLevel());
        return new Dummy(computer.copy(), entity);
    }

    /* A machine's computer: the item it was made for, and the computer drawn. */
    private record Dummy(ItemStack computer, AbstractSmallComputerBlockEntity entity) {
    }
}
