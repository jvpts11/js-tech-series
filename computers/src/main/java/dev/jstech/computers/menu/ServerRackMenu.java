/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.gui.layout.ServerRackLayout;
import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.item.DiskItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.item.StorageCardItem;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsDisks;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.rack.IMountableRackUnit;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.Nullable;

/**
 * Menu for the Server Rack: one row per rack unit holding the server slot and the five front-panel
 * hotswap slots that belong to that row (the rack owns them; the mounted chassis decides which are
 * cabled), plus the player inventory. Slot positions come from {@link ServerRackLayout}, the same
 * source the screen draws from.
 */
@TextHolder
public class ServerRackMenu extends CoreMenu {

    private final ServerRackBlockEntity rack;

    /** How a unit's drive array stands, which is what the rack's rows colour it by. */
    public enum ArrayHealth { NONE, HEALTHY, DEGRADED, FAILED }

    private static final TextKey RAID_DEGRADED = TextKey.of("jsc.rack.raid_degraded", "%s DEGRADED");
    private static final TextKey RAID_FAILED = TextKey.of("jsc.rack.raid_failed", "%s FAILED");
    /**
     * How many of the menu's slots are the rack's own (the server bays); the front hotswap bays follow them,
     * and the player inventory follows those.
     */
    public static final int RACK_SLOTS = ServerRackBlockEntity.CAPACITY_U;
    /** How many of the menu's slots are the rack's front hotswap bays, right after the server slots. */
    public static final int FRONT_SLOT_COUNT = RACK_SLOTS * RackLayout.SLOTS_PER_U;
    private static final RackLayout LAYOUT = new RackLayout(RACK_SLOTS);

    public ServerRackMenu(final int containerId, final Inventory playerInventory,
                          final ServerRackBlockEntity be) {
        super(ComputingMenus.SERVER_RACK_MENU.get(), containerId, playerInventory,
                MenuValidity.block(be.getLevel(), be.getBlockPos(), ServerRackBlock.class));
        this.rack = be;

        final GuiLayout layout = ServerRackLayout.layout();
        /*
         * Server slot i = rack-unit row i; a taller chassis claims the rows below its slot (the
         * handler rejects them).
         */
        final IItemHandler servers = be.getServers();
        final Slot[] serverSlots = new Slot[RACK_SLOTS];
        for (int i = 0; i < RACK_SLOTS; i++) {
            final GuiLayout.SlotPosition at = layout.slotAt("server" + i);
            serverSlots[i] = new ServerBaySlot(servers, i, at.x(), at.y());
        }
        final SlotGroup serverGroup = slots(serverSlots);
        // The rack's hotswap slots, row-major: index = row * 5 + column.
        final IItemHandler front = be.getFrontSlots();
        final Slot[] frontSlots = new Slot[FRONT_SLOT_COUNT];
        for (int index = 0; index < FRONT_SLOT_COUNT; index++) {
            final int row = index / RackLayout.SLOTS_PER_U;
            final int column = index % RackLayout.SLOTS_PER_U;
            final GuiLayout.SlotPosition at = layout.slotAt("front" + row + "_" + column);
            frontSlots[index] = new FrontBaySlot(front, index, at.x(), at.y());
        }
        final SlotGroup frontGroup = slots(frontSlots);
        final PlayerSlots player = playerInventory(playerInventory, layout.playerInventoryAt());
        shiftClick(serverGroup, player.all());
        shiftClick(frontGroup, player.all());
        shiftClick(player.all(), serverGroup, frontGroup);
        data(be.fields().menuData());
    }

    public boolean networkLinked() {
        return rack.linkedShown();
    }

    /** The cabinet's era, so its screen wears that decade's materials rather than one look for all three. */
    public HardwareEra rackEra() {
        return rack.rackEra();
    }

    /** Whether this cabinet is a compute one, which its screen says instead of calling everything a rack. */
    public RackChassis.RackType rackType() {
        return rack.rackType();
    }

    public boolean bayPowerOn(final int slot) {
        return (rack.bayPowerShown() & (1 << slot)) != 0;
    }

    /** The cabinet's thermal throttle in percent (100 = running free). */
    public int throttlePercent() {
        final int percent = rack.throttleShown();
        return percent <= 0 ? 100 : percent;
    }

    /** Rebuild progress of the unit's array in permille, or 0 when it is not rebuilding. */
    public int rebuildPermille(final int slot) {
        return slot < 0 || slot >= RACK_SLOTS ? 0 : rack.rebuildShown(slot);
    }

    public ItemStack serverInBay(final int i) {
        return i >= 0 && i < RACK_SLOTS ? slots.get(i).getItem() : ItemStack.EMPTY;
    }

    public ItemStack frontSlotStack(final int index) {
        return index >= 0 && index < FRONT_SLOT_COUNT ? slots.get(RACK_SLOTS + index).getItem() : ItemStack.EMPTY;
    }

    /**
     * The role of a front slot given the currently mounted chassis. Derived from the menu's own
     * synced slots rather than from the block entity: on the client the rack's handlers are empty
     * (the rack sends no update tag), and only the container's slots carry the real contents.
     */
    public RackLayout.SlotRole frontSlotRole(final int index) {
        return LAYOUT.roleAt(index / RackLayout.SLOTS_PER_U, index % RackLayout.SLOTS_PER_U,
                mountedUnits());
    }

    /**
     * Why {@code stack} stays out of {@code slot}, when it is one of the rack's rows or front bays; null otherwise,
     * and for a blocked bay, which the screen says in its own words.
     */
    @Nullable
    public Component refusal(final Slot slot, final ItemStack stack) {
        if (stack.isEmpty() || slot.mayPlace(stack) || !(slot instanceof SlotItemHandler part)) {
            return null;
        }
        final TextKey why = part.getItemHandler() == rack.getServers() ? rack.rowRefusal(part.getSlotIndex(), stack)
                : part.getItemHandler() == rack.getFrontSlots() ? rack.bayRefusal(part.getSlotIndex(), stack) : null;
        return why == null ? null : GameText.component(why);
    }

    /**
     * Everything mounted (servers and the rack's own units alike), read from the synced server slots so the client
     * agrees with the server.
     */
    public List<RackLayout.Unit> mountedUnits() {
        final List<RackLayout.Unit> mounted = new ArrayList<>();
        for (int i = 0; i < RACK_SLOTS; i++) {
            final RackLayout.Unit unit = IMountableRackUnit.unitAt(serverInBay(i), i);
            if (unit != null) {
                mounted.add(unit);
            }
        }
        return mounted;
    }

    /** The top row of the unit occupying {@code row}, or -1 when nothing does. */
    public int unitTopOf(final int row) {
        final RackLayout.Unit unit = RackLayout.unitAt(row, mountedUnits());
        return unit == null ? -1 : unit.topU();
    }

    /** How many rack units the unit topped at {@code topRow} takes, or 1 when nothing is mounted there. */
    public int unitHeightAt(final int topRow) {
        final IMountableRackUnit unit = IMountableRackUnit.of(serverInBay(topRow));
        return unit == null ? 1 : unit.heightU();
    }

    /** The row whose machine a linked monitor shows, or -1 when it can show none. */
    public int screenRow() {
        return rack.screenShown();
    }

    /**
     * The disk the machine topped at {@code topRow} boots from, counted among its drive bays from 0, or -1 when none
     * of its disks carries a system: the one it prefers when that one carries a system, else the first that does.
     */
    public int bootDisk(final int topRow) {
        final List<ItemStack> drives = driveStacks(topRow);
        return OsDisks.systemDiskSlot(drives.size(), drives::get, preferredBootDisk(topRow));
    }

    /** The system the machine topped at {@code topRow} boots, or null when none of its disks carries one. */
    @Nullable
    public OsDef system(final int topRow) {
        final int disk = bootDisk(topRow);
        if (disk < 0) {
            return null;
        }
        final ResourceLocation osId = OsDisks.systemOn(driveStacks(topRow).get(disk));
        return osId == null ? null : OsRegistry.getOs(osId);
    }

    public BlockPos rackPos() {
        return rack.getBlockPos();
    }

    // RAID readout, derived from the synced slots so the client can draw it

    /** The front-slot indices the unit topped at {@code topRow} claims with the given role. */
    private List<Integer> claimedSlots(final int topRow, final RackLayout.SlotRole role) {
        final RackChassis chassis =
                ServerItem.chassisOf(serverInBay(topRow));
        final List<Integer> out = new ArrayList<>();
        if (chassis == null) {
            return out;
        }
        final List<RackLayout.Unit> mounted = mountedUnits();
        for (int row = topRow; row < topRow + chassis.heightU() && row < RACK_SLOTS; row++) {
            for (int column = 0; column < RackLayout.SLOTS_PER_U; column++) {
                if (LAYOUT.roleAt(row, column, mounted) == role) {
                    out.add(row * RackLayout.SLOTS_PER_U + column);
                }
            }
        }
        return out;
    }

    /** The RAID mode the unit topped at {@code topRow} runs, as its RAID controller card keeps it. */
    public RaidMode raidModeAt(final int topRow) {
        return StorageCardItem.raidMode(controllerAt(topRow));
    }

    /* A copy of the RAID controller card in the server topped at {@code topRow}, or an empty stack. */
    private ItemStack controllerAt(final int topRow) {
        return ServerItem.card(serverInBay(topRow), ExpansionCardKind.RAID_CONTROLLER);
    }

    /**
     * A short label for the unit's array state ({@code RAID5}, {@code RAID5 DEGRADED},
     * {@code RAID0 FAILED}), or null when the unit runs no array.
     */
    @Nullable
    public Text raidLabel(final int topRow) {
        // The level goes by its name, which reads the same in every language.
        final String mode = raidModeAt(topRow).name();
        return switch (raidHealth(topRow)) {
            case NONE -> null;
            case HEALTHY -> Text.literal(mode);
            case DEGRADED -> RAID_DEGRADED.with(mode);
            case FAILED -> RAID_FAILED.with(mode);
        };
    }

    /** How the unit's array stands: whole, short of a drive it can do without, or lost. */
    public ArrayHealth raidHealth(final int topRow) {
        final RaidMode mode = raidModeAt(topRow);
        if (mode == RaidMode.NONE) {
            return ArrayHealth.NONE;
        }
        final Integer stored = controllerAt(topRow).get(ComputingComponents.RAID_MEMBERS.get());
        final int members = stored == null ? 0 : stored;
        int present = 0;
        for (final int index : claimedSlots(topRow, RackLayout.SlotRole.DRIVE)) {
            if (frontSlotStack(index).getItem()
                    instanceof DiskItem) {
                present++;
            }
        }
        if (members > 0 && !mode.survives(members, present)) {
            return ArrayHealth.FAILED;
        }
        return members > present ? ArrayHealth.DEGRADED : ArrayHealth.HEALTHY;
    }

    public static ServerRackMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                             final RegistryFriendlyByteBuf buf) {
        return new ServerRackMenu(containerId, playerInventory,
                MenuOpening.blockEntity(playerInventory, buf, ServerRackBlockEntity.class));
    }

    /* The stacks in the drive bays the unit topped at topRow cables, in bay order. */
    private List<ItemStack> driveStacks(final int topRow) {
        final List<ItemStack> out = new ArrayList<>();
        for (final int index : claimedSlots(topRow, RackLayout.SlotRole.DRIVE)) {
            out.add(frontSlotStack(index));
        }
        return out;
    }

    /*
     * The disk the machine's firmware was last told to boot from, as its Server item carries it; -1 for none chosen.
     * The item is written back when the machine's session is, so this can lag a choice just made, and a stale or
     * missing choice only means the first disk with a system answers, which is what the firmware does too.
     */
    private int preferredBootDisk(final int topRow) {
        final CompoundTag console = serverInBay(topRow).get(ComputingComponents.SERVER_CONSOLE.get());
        return console != null && console.contains("BootDiskSlot") ? console.getInt("BootDiskSlot") : -1;
    }

    /** A row's server slot: hidden while the unit mounted above takes its row, which leaves it nothing to hold. */
    private final class ServerBaySlot extends SlotItemHandler {

        ServerBaySlot(final IItemHandler handler, final int index, final int x, final int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean isActive() {
            final int top = unitTopOf(getSlotIndex());
            return hasItem() || top < 0 || top == getSlotIndex();
        }
    }

    /**
     * A front-panel slot: there only while a mounted unit cables it, or while it still holds what was put in it before
     * its unit came out, so that can always be taken back.
     */
    private final class FrontBaySlot extends SlotItemHandler {

        FrontBaySlot(final IItemHandler handler, final int index, final int x, final int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean isActive() {
            final RackLayout.SlotRole role = frontSlotRole(getSlotIndex());
            return hasItem() || role == RackLayout.SlotRole.DRIVE;
        }
    }
}
