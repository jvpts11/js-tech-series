/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity;
import dev.jstech.computers.blockentity.PowerShown;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.hardware.ThermalState;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.COOLER_SLOTS;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.COOLER_SLOTS_START;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.CPU_SLOTS;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.CPU_SLOTS_START;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.DISK_SLOTS;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.DISK_SLOTS_START;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.GPU_SLOTS;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.GPU_SLOTS_START;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.MOTHERBOARD_SLOT;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.PSU_SLOT;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.RAM_SLOTS;
import static dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity.RAM_SLOTS_START;

/**
 * The assembly menu of a cabinet computer, the Mainframe and the WAN Gateway Computer alike: the hardware slots
 * (motherboard, processors, memory, cards, supply and disks, each restricted to its kind of part by the block entity's
 * item handler, on the board's face; a cooler over each socket on the cooling face) and the player's inventory, with
 * the power, the auto start and the two faces as its buttons, and what every cabinet shows synced for the screen.
 *
 * @param <B> the cabinet's block entity
 */
public abstract class AbstractCabinetMenu<B extends AbstractCabinetComputerBlockEntity> extends CoreMenu {

    private final B blockEntity;
    /* The cooling face's slots, a cooler's over each socket. */
    private final Set<Slot> coolingSlots = new HashSet<>();
    /* The face the screen shows, which decides whose slots are there to click. */
    private AssemblyGeometry.Face face = AssemblyGeometry.Face.BOARD;

    public static final int BUTTON_POWER = 0;
    public static final int BUTTON_AUTOSTART = 1;
    /** The tabs that turn the screen to the board's face and to the cooling face. */
    public static final int BUTTON_FACE_BOARD = 3;
    public static final int BUTTON_FACE_COOLING = 4;

    protected AbstractCabinetMenu(final MenuType<?> type, final int containerId, final Inventory playerInventory,
                                  final B be, final AssemblyGeometry geometry) {
        super(type, containerId, playerInventory, MenuValidity.blockEntity(be));
        this.blockEntity = be;
        final GuiLayout layout = geometry.layout("gpu");
        final IItemHandler hardware = be.getInventory();
        final BooleanSupplier board = () -> face == AssemblyGeometry.Face.BOARD;

        final List<Slot> hardwareSlots = new ArrayList<>();
        hardwareSlots.add(faced(hardware, MOTHERBOARD_SLOT, layout.slotAt("mobo"), board));
        hardwareSlots.add(faced(hardware, PSU_SLOT, layout.slotAt("psu"), board));
        for (int i = 0; i < CPU_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("cpu_" + i);
            hardwareSlots.add(new BoardSlot(hardware, CPU_SLOTS_START + i, at.x(), at.y(), i, be::boardCpuSlots,
                    board));
        }
        for (int i = 0; i < RAM_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("ram_" + i);
            hardwareSlots.add(new BoardSlot(hardware, RAM_SLOTS_START + i, at.x(), at.y(), i, be::boardRamSlots,
                    board));
        }
        for (int i = 0; i < GPU_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("gpu_" + i);
            hardwareSlots.add(new BoardSlot(hardware, GPU_SLOTS_START + i, at.x(), at.y(), i, be::boardPcieSlots,
                    board));
        }
        for (int i = 0; i < DISK_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("disk_" + i);
            hardwareSlots.add(new BoardSlot(hardware, DISK_SLOTS_START + i, at.x(), at.y(), i, be::boardDiskSlots,
                    board));
        }
        // A cooler over each socket the board seats, on the cooling face.
        final GuiLayout cooling = geometry.coolingLayout();
        for (int i = 0; i < COOLER_SLOTS; i++) {
            final GuiLayout.SlotPosition at = cooling.slotAt("cooler_" + i);
            final Slot slot = new BoardSlot(hardware, COOLER_SLOTS_START + i, at.x(), at.y(), i,
                    be::boardCpuSlots, () -> face == AssemblyGeometry.Face.COOLING);
            hardwareSlots.add(slot);
            coolingSlots.add(slot);
        }
        final SlotGroup hw = slots(hardwareSlots.toArray(Slot[]::new));

        final PlayerSlots player = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(hw, player.all());
        shiftClick(player.all(), hw);

        data(be.fields().menuData());

        button(BUTTON_POWER, p -> blockEntity.togglePower());
        button(BUTTON_AUTOSTART, p -> blockEntity.toggleAutoStart());
        button(BUTTON_FACE_BOARD, p -> showFace(AssemblyGeometry.Face.BOARD));
        button(BUTTON_FACE_COOLING, p -> showFace(AssemblyGeometry.Face.COOLING));
    }

    /** The face the screen shows. */
    public AssemblyGeometry.Face face() {
        return face;
    }

    /** Turns the screen to {@code face}: only its slots are there to click, on the player's side and the server's. */
    public void showFace(final AssemblyGeometry.Face face) {
        this.face = face;
    }

    /** Whether {@code slot} is one of the cooling face's, a cooler's. */
    public boolean coolingSlot(final Slot slot) {
        return coolingSlots.contains(slot);
    }

    /** How the cabinet's heat stands, as the server last told the open screen. */
    public ThermalState thermal() {
        return blockEntity.thermalShown();
    }

    @Nullable
    public Era hardwareEra() {
        return blockEntity.displayEra();
    }

    public boolean hasBoard() {
        return slots.get(0).hasItem();
    }

    public boolean hasPsu() {
        return slots.get(1).hasItem();
    }

    /** The supply in its slot, for the DRAW readout to name. */
    public ItemStack psu() {
        return slots.get(1).getItem();
    }

    /** What the cabinet's power draws, as the server last told the open screen. */
    public PowerShown powerShown() {
        return blockEntity.powerShown();
    }

    public BlockPos blockPos() {
        return blockEntity.getBlockPos();
    }

    /** Why {@code stack} stays out of {@code slot}, when it is one of the cabinet's part slots; null otherwise. */
    @Nullable
    public Component refusal(final Slot slot, final ItemStack stack) {
        return PartSlots.refusal(slot, blockEntity.getHardware(), stack, blockEntity::refusal);
    }

    public int boardCpuSlots() {
        return blockEntity.boardCpuSlots();
    }

    public int boardRamSlots() {
        return blockEntity.boardRamSlots();
    }

    public int boardPcieSlots() {
        return blockEntity.boardPcieSlots();
    }

    public int boardDiskSlots() {
        return blockEntity.boardDiskSlots();
    }

    public boolean isRunning() {
        return blockEntity.assemblyRunning();
    }

    public boolean buildValid() {
        return blockEntity.assemblyBuildValid();
    }

    public long capacity() {
        return blockEntity.assemblyCapacity();
    }

    public boolean isAutoStart() {
        return blockEntity.assemblyAutoStart();
    }

    public boolean isManualOn() {
        return blockEntity.assemblyManualOn();
    }

    /** The cabinet the menu is open on. */
    protected B blockEntity() {
        return blockEntity;
    }

    /* A slot shown while {@code shown} says so. */
    private static Slot faced(final IItemHandler handler, final int index, final GuiLayout.SlotPosition at,
                              final BooleanSupplier shown) {
        return new FacedSlot(handler, index, at.x(), at.y(), shown);
    }
}
