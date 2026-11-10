/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import dev.jstech.computers.block.IEraChassisBlock;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.CasePorts;
import dev.jstech.computers.hardware.IExpansionCardSpec;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.cable.IPlugged;
import dev.jstech.core.blockentity.DerivedInt;
import dev.jstech.core.model.network.ModelAnimations;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * A small computer, one block: the Personal Computer, the Crafting Computer and the Cluster Management Computer. Its
 * case is drawn by its block entity, from the model its block names: the tower of its age, or in the later ages the
 * one of three cases it comes in; with the parts put in it, its lamps lit as it runs and works its disk, and its
 * fans turning while it runs.
 */
public abstract class AbstractSmallComputerBlockEntity extends AbstractComputerBlockEntity implements IPlugged {

    /** The left side taken off the case, showing what the player put inside; the world and the screen both show it. */
    private final BoolField sidePanelOff = fields().flag("SidePanelOff", false).save().toClient().toMenu();
    /*
     * The lamps on the front, worked out on the server: the power lamp's, lit while it is switched on with its fans
     * turning, a blank board's too, and the disk lamp's.
     */
    private final DerivedInt lamps =
            fields().derived("Lamps", () -> (poweredUp() ? POWER_LAMP : 0) | (diskBusy() ? DISK_LAMP : 0)).toClient();
    /* What its assembly window shows of a board with no firmware: switched on, its fans turn and nothing more. */
    private final DerivedInt assemblyPoweredUp = fields().derived("AssemblyPoweredUp", this::poweredUp).toMenu();
    private final DerivedInt assemblyFirmwareMissing =
            fields().derived("AssemblyFirmwareMissing", this::firmwareMissing).toMenu();
    /* Made by a machine and never started since: its first start runs its self-test. */
    private final BoolField neverStarted = fields().flag("NeverStarted", false).save();
    /** What the case's bays and front ports hold, and the trays of the drives in its bays. */
    private final CaseFront front = new CaseFront(this, fields());
    /** The plug its control cables were last drawn ending in, on the client, so a card changed redraws them. */
    private String drawnPlug = "";

    /** The controller the power button's press plays on, and the press. */
    public static final String POWER = "power";
    public static final String PRESS = "animation.computer.press_power";
    private static final int POWER_LAMP = 1;
    private static final int DISK_LAMP = 2;

    protected AbstractSmallComputerBlockEntity(final BlockEntityType<?> type, final BlockPos pos,
                                               final BlockState state) {
        super(type, pos, state);
    }

    /** The front of the case: its bays, its USB ports and what they hold. */
    public CaseFront front() {
        return front;
    }

    /** How many USB ports the case has on its front: its era's. */
    public int casePorts() {
        return getBlockState().getBlock() instanceof IEraChassisBlock chassis ? CasePorts.of(chassis.chassisEra()) : 0;
    }

    /** Spills what the case's bays and ports hold, as the case breaks. */
    public void spillFront(final ServerLevel level, final BlockPos pos) {
        front.spill(level, pos);
    }

    /**
     * The whole computer as an item, as it is given back when it breaks (computers/docs/design/survival.md): its case
     * with its parts inside, its name, and whether it was never started.
     */
    public ItemStack asItem() {
        final ItemStack whole = new ItemStack(getBlockState().getBlock());
        whole.applyComponents(collectComponents());
        return whole;
    }

    /** Whether it was made by a machine and never started since. */
    public boolean neverStarted() {
        return neverStarted.get();
    }

    /* Its first start is its self-test: from then on it is no longer a machine that never started. */
    @Override
    protected void tickBootPhases(final ServerLevel level) {
        super.tickBootPhases(level);
        if (neverStarted.get() && isRunning()) {
            neverStarted.set(false);
        }
    }

    /* Placed from an assembled computer, it stands with the parts, the name and the mark the item carries. */
    @Override
    protected void applyImplicitComponents(final DataComponentInput input) {
        super.applyImplicitComponents(input);
        final ItemContainerContents parts = input.getOrDefault(ComputingComponents.ASSEMBLED_HARDWARE.get(),
                ItemContainerContents.EMPTY);
        final ItemStackHandler hardware = getHardware();
        for (int slot = 0; slot < parts.getSlots() && slot < hardware.getSlots(); slot++) {
            hardware.setStackInSlot(slot, parts.getStackInSlot(slot).copy());
        }
        final String name = input.get(ComputingComponents.COMPUTER_NAME.get());
        if (name != null && !name.isBlank()) {
            setCustomName(name);
        }
        neverStarted.set(input.get(ComputingComponents.NEVER_STARTED.get()) != null);
    }

    /* As an item, it carries its parts, its name and whether it was never started. */
    @Override
    protected void collectImplicitComponents(final DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        final ItemStackHandler hardware = getHardware();
        final List<ItemStack> parts = new ArrayList<>();
        boolean any = false;
        for (int slot = 0; slot < hardware.getSlots(); slot++) {
            parts.add(hardware.getStackInSlot(slot).copy());
            any |= !hardware.getStackInSlot(slot).isEmpty();
        }
        if (any) {
            components.set(ComputingComponents.ASSEMBLED_HARDWARE.get(), ItemContainerContents.fromItems(parts));
        }
        if (!customName().isEmpty()) {
            components.set(ComputingComponents.COMPUTER_NAME.get(), customName());
        }
        if (neverStarted.get()) {
            components.set(ComputingComponents.NEVER_STARTED.get(), Unit.INSTANCE);
        }
    }

    /** The power button pressed on the front: the button goes in for whoever sees the case. */
    public void showPowerPressed() {
        ModelAnimations.replay(this, POWER, PRESS);
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

    /**
     * The port a control cable plugs into on it: the connector of its first card that reads machines, as a PROFIBUS
     * card's violet D-sub; read off the parts the players who see it are told of, so it is the same on both sides.
     * Every other cable ends at it in its own plug.
     */
    @Override
    public String plugKind() {
        for (final ResourceLocation part : installedParts()) {
            final IExpansionCardSpec card = HardwareRegistry.card(new ItemStack(BuiltInRegistries.ITEM.get(part)));
            if (card != null && !card.controlPlug().isEmpty()) {
                return card.controlPlug();
            }
        }
        return "";
    }

    /** Whether the machine is switched on with its fans turning, as its power lamp shows it on every side. */
    public boolean visualRunning() {
        return (lamps.getAsInt() & POWER_LAMP) != 0;
    }

    /** Whether the assembly window shows the machine switched on with its fans turning; mirrored while it is open. */
    public boolean assemblyPoweredUp() {
        return assemblyPoweredUp.isSet();
    }

    /** Whether the assembly window shows a board with no firmware: NO FIRMWARE, its readings blank. */
    public boolean assemblyFirmwareMissing() {
        return assemblyFirmwareMissing.isSet();
    }

    /** Whether the machine worked its disk a moment ago, as its disk lamp shows it, blinking, on every side. */
    public boolean visualDiskBusy() {
        return (lamps.getAsInt() & DISK_LAMP) != 0;
    }

    /*
     * Every bay up to the last hot-swap one counts, a hot-swap bay or not, so a bay's disk slot keeps its number
     * whatever sits in the bays before it; a case with none adds no slot at all, and a bay past the last hot-swap
     * one (or one the case does not have) adds none either, so no slot shows that could never hold a disk.
     */
    @Override
    protected int frontDiskSlots() {
        for (int bay = CaseFront.BAYS - 1; bay >= 0; bay--) {
            if (front.kind(bay) == BayDriveSpec.Kind.HOT_SWAP) {
                return bay + 1;
            }
        }
        return 0;
    }

    @Override
    protected ItemStack frontDisk(final int bay) {
        return front.hotSwapDisk(bay);
    }

    /**
     * The disks the machine keeps its items on: those in its own disk slots, then those in its case's hot-swap bays,
     * empty where a slot holds none, so a disk's place in the list is its place in the machine.
     */
    protected List<ItemStack> storeDisks() {
        final ComputerHardwareLayout layout = hardwareLayout();
        final int front = frontDiskSlots();
        final List<ItemStack> disks = new ArrayList<>(layout.diskCount() + front);
        for (int i = 0; i < layout.diskCount(); i++) {
            disks.add(getHardware().getStackInSlot(layout.diskStart() + i));
        }
        for (int bay = 0; bay < front; bay++) {
            disks.add(frontDisk(bay));
        }
        return disks;
    }

    @Override
    protected void putFrontDisk(final int bay, final ItemStack disk) {
        front.putHotSwapDisk(bay, disk);
    }

    @Override
    protected void beforeClientUpdate() {
        super.beforeClientUpdate();
        front.beforeClientUpdate();
    }

    /*
     * A card that reads machines put in or taken out changes the plug its control cables end in, which they choose as
     * they are drawn: its block is drawn again, and the cable blocks around it with it.
     */
    @Override
    protected void afterClientUpdate() {
        super.afterClientUpdate();
        front.afterClientUpdate();
        final String plug = plugKind();
        if (!plug.equals(drawnPlug) && level != null) {
            drawnPlug = plug;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void partsChanged(final List<ResourceLocation> before) {
        // A part taken out of a bay, or swapped for another, lets fall what it held.
        front.partsChanged(before, installedParts());
    }
}
