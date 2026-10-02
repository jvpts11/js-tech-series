/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.blockentity.ValueField;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A small computer, one block: the Personal Computer, the Crafting Computer and the Cluster Management Computer. Its
 * case is drawn by its block entity, from the model its block names: the tower of its age, or in the later ages the
 * one of three cases it comes in; with the parts put in it, its lamps lit as it runs and works its disk, and its
 * fans turning while it runs.
 */
public abstract class AbstractSmallComputerBlockEntity extends AbstractComputerBlockEntity implements GeoBlockEntity {

    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    /** The left side taken off the case, showing what the player put inside; the world and the screen both show it. */
    private final BoolField sidePanelOff = fields().flag("SidePanelOff", false).save().toClient().toMenu();
    /**
     * The item in each hardware slot, air where there is none, told to the players who see the machine so they see
     * each part inside it. It is the hardware's own record, worked out again whenever the hardware changes or is read.
     */
    private final ValueField<List<ResourceLocation>> installed =
            fields().value("Installed", ResourceLocation.CODEC.listOf(), List.of()).toClient();
    /** The lamps on the front, worked out on the server: the power lamp's and the disk lamp's. */
    private final DerivedInt lamps =
            fields().derived("Lamps", () -> (isRunning() ? POWER_LAMP : 0) | (diskBusy() ? DISK_LAMP : 0)).toClient();

    private static final int POWER_LAMP = 1;
    private static final int DISK_LAMP = 2;
    private static final RawAnimation WORK = RawAnimation.begin().thenLoop("animation.computer.work");

    protected AbstractSmallComputerBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                               final BlockState state, final ComputerHardwareLayout layout) {
        super(type, pos, state, layout);
    }

    /** Whether the left side is off the case. */
    public boolean sidePanelOff() {
        return sidePanelOff.get();
    }

    /** Takes the left side off the case, or puts it back. */
    public void toggleSidePanel() {
        sidePanelOff.set(!sidePanelOff.get());
    }

    /** Which hardware slot takes which part: the board's, the processor's, the memory's, and on. */
    public ComputerHardwareLayout hardwareLayout() {
        return layout();
    }

    /** The id of the item in hardware slot {@code slot}, as the players who see the machine know it, or null. */
    public @Nullable ResourceLocation installedPart(final int slot) {
        final List<ResourceLocation> parts = installed.get();
        if (slot < 0 || slot >= parts.size()) {
            return null;
        }
        final ResourceLocation part = parts.get(slot);
        return part.equals(BuiltInRegistries.ITEM.getKey(Items.AIR)) ? null : part;
    }

    /** Whether the machine runs, as its power lamp shows it on every side. */
    public boolean visualRunning() {
        return (lamps.getAsInt() & POWER_LAMP) != 0;
    }

    /** Whether the machine worked its disk a moment ago, as its disk lamp shows it, blinking, on every side. */
    public boolean visualDiskBusy() {
        return (lamps.getAsInt() & DISK_LAMP) != 0;
    }

    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
        // The case's fans turn while the machine runs; its lamps and the parts inside are the renderer's to draw.
        controllers.add(new AnimationController<>(this, "work", 0,
                state -> visualRunning() ? state.setAndContinue(WORK) : PlayState.STOP));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    @Override
    protected void partsChanged() {
        final ItemStackHandler slots = getHardware();
        final List<ResourceLocation> parts = new ArrayList<>(slots.getSlots());
        for (int slot = 0; slot < slots.getSlots(); slot++) {
            parts.add(BuiltInRegistries.ITEM.getKey(slots.getStackInSlot(slot).getItem()));
        }
        installed.set(List.copyOf(parts));
    }
}
