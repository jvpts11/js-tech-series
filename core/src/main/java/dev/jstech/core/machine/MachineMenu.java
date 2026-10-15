/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.machine;

import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.energy.EnergyProtection;
import dev.jstech.core.gui.layout.GuiLayout;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.menu.CoreMenu;
import dev.jstech.core.menu.MenuOpening;
import dev.jstech.core.menu.MenuValidity;
import dev.jstech.core.menu.PlayerSlots;
import dev.jstech.core.menu.SlotGroup;
import dev.jstech.core.team.Access;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The menu every machine of every mod opens, behind its five tabs: its Main tab's slots where its face shows them,
 * its upgrade bays on the Upgrades tab, its fuse and its battery on the Protection tab, and the player's inventory
 * under every tab. Each setting of its tabs is a button the server answers, for a player the machine lets set it up:
 * its switch, its redstone mode and what its comparator reads, who may use it, what each face does with each resource,
 * what it pushes and pulls by itself, and its input slots' locks.
 */
public final class MachineMenu extends CoreMenu {

    private final MachineBlockEntity machine;
    private final List<Slot> faceSlots = new ArrayList<>();
    private int tab;

    /** The tabs, in their order along the top. */
    public static final int MAIN = 0;
    public static final int SIDES = 1;
    public static final int REDSTONE = 2;
    public static final int UPGRADES = 3;
    public static final int PROTECTION = 4;
    /** The buttons: a tab, the switch, a redstone mode, what the comparator reads, who may use it. */
    public static final int FIRST_TAB = 0;
    public static final int SWITCH = 10;
    public static final int FIRST_MODE = 11;
    public static final int FIRST_READS = 15;
    public static final int FIRST_ACCESS = 19;
    /** What a face does with a resource: {@code FIRST_SIDE + resource * 32 + face * 4 + mode}. */
    public static final int FIRST_SIDE = 100;
    /**
     * What it does by itself with a resource: {@code FIRST_AUTOMATION + resource * 16 + what}, what being the push's
     * switch, the pull's, then each wheel's three digits up and down.
     */
    public static final int FIRST_AUTOMATION = 400;
    public static final int PUSH = 0;
    public static final int PULL = 1;
    public static final int PUSH_UP = 2;
    public static final int PUSH_DOWN = 5;
    public static final int PULL_UP = 8;
    public static final int PULL_DOWN = 11;
    /** An input slot's lock: locked to what it holds, or unlocked. */
    public static final int FIRST_LOCK = 600;
    /**
     * What a resource's pushing and pulling lets pass, {@code FIRST_FILTER + resource}: only the thing in the player's
     * hand, or everything for an empty hand.
     */
    public static final int FIRST_FILTER = 700;
    /** The most a wheel shows: three digits. */
    public static final int WHEEL_MOST = 999;

    /** The menu of {@code machine} for the player whose {@code inventory} it is. */
    public MachineMenu(final int containerId, final Inventory inventory, final MachineBlockEntity machine) {
        super(Machines.MENU.get(), containerId, inventory, MenuValidity.blockEntity(machine));
        this.machine = machine;
        final List<Slot> mains = new ArrayList<>();
        for (final FaceSlot at : machine.mainSlots()) {
            final Slot slot = new TabSlot(machine.getInventory(), at.index(), at.x(), at.y(), MAIN, at.output());
            mains.add(slot);
            faceSlots.add(slot);
        }
        final SlotGroup main = slots(mains.toArray(Slot[]::new));
        final List<Slot> bays = new ArrayList<>();
        if (machine instanceof ProcessingMachineBlockEntity processing) {
            final ProcessingMachineBlockEntity.Layout layout = processing.layout();
            for (int i = 0; i < Math.min(layout.upgrades(), MachineScreenLayout.BAYS); i++) {
                final GuiLayout.SlotPosition at = MachineScreenLayout.bay(i);
                bays.add(new TabSlot(machine.getInventory(), layout.firstUpgrade() + i, at.x(), at.y(), UPGRADES,
                        false));
            }
        }
        final SlotGroup upgrades = slots(bays.toArray(Slot[]::new));
        final List<Slot> guards = new ArrayList<>();
        final EnergyProtection protection = machine.power().protection();
        if (protection != null) {
            guards.add(new TabSlot(protection.slots(), EnergyProtection.FUSE, MachineScreenLayout.fuse().x(),
                    MachineScreenLayout.fuse().y(), PROTECTION, false));
            guards.add(new TabSlot(protection.slots(), EnergyProtection.BATTERY, MachineScreenLayout.battery().x(),
                    MachineScreenLayout.battery().y(), PROTECTION, false));
        }
        final SlotGroup protections = slots(guards.toArray(Slot[]::new));
        final PlayerSlots player = playerInventory(inventory, MachineScreenLayout.playerInventory());
        shiftClick(player.main(), main, upgrades, protections, player.hotbar());
        shiftClick(player.hotbar(), main, upgrades, protections, player.main());
        shiftClick(main, player.main(), player.hotbar());
        shiftClick(upgrades, player.main(), player.hotbar());
        shiftClick(protections, player.main(), player.hotbar());
        data(machine.fields().menuData());
        declareButtons();
    }

    /** The menu on the player's side, for the machine whose place the server wrote. */
    public MachineMenu(final int containerId, final Inventory inventory, final RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, MenuOpening.blockEntity(inventory, buf, MachineBlockEntity.class));
    }

    /** The machine. */
    public MachineBlockEntity machine() {
        return machine;
    }

    /** The tab shown. */
    public int tab() {
        return tab;
    }

    /** Shows tab {@code index}: its slots only are there to click. */
    public void showTab(final int index) {
        this.tab = Math.max(0, Math.min(MachineScreenLayout.TABS - 1, index));
    }

    /** The slots its Main tab shows. */
    public List<Slot> faceSlots() {
        return List.copyOf(faceSlots);
    }

    /** The button that sets what {@code face} does with {@code resource} to {@code mode}. */
    public static int sideButton(final MachineResource resource, final RelativeFace face, final FaceMode mode) {
        return FIRST_SIDE + MachineResource.round().indexOf(resource) * 32
                + List.of(RelativeFace.values()).indexOf(face) * 4 + FaceModes.round().indexOf(mode);
    }

    /** The button of {@code what} for {@code resource}'s pushing and pulling. */
    public static int automationButton(final MachineResource resource, final int what) {
        return FIRST_AUTOMATION + MachineResource.round().indexOf(resource) * 16 + what;
    }

    private void declareButtons() {
        for (int i = 0; i < MachineScreenLayout.TABS; i++) {
            final int index = i;
            button(FIRST_TAB + i, player -> showTab(index));
        }
        button(SWITCH, player -> settable(player, () -> machine.control().setOn(!machine.control().on())));
        for (int i = 0; i < RedstoneMode.round().size(); i++) {
            final RedstoneMode mode = RedstoneMode.round().get(i);
            button(FIRST_MODE + i, player -> settable(player, () -> machine.control().setMode(mode)));
        }
        for (int i = 0; i < ComparatorReads.round().size(); i++) {
            final ComparatorReads reads = ComparatorReads.round().get(i);
            button(FIRST_READS + i, player -> settable(player, () -> machine.control().setReads(reads)));
        }
        final List<Access> accesses = List.of(Access.PRIVATE, Access.TEAM, Access.PUBLIC);
        for (int i = 0; i < accesses.size(); i++) {
            final Access access = accesses.get(i);
            button(FIRST_ACCESS + i, player -> owner(player, () -> machine.setAccess(access)));
        }
        final List<MachineResource> resources = MachineResource.round();
        final List<RelativeFace> faces = List.of(RelativeFace.values());
        for (final MachineResource resource : resources) {
            for (final RelativeFace face : faces) {
                for (final FaceMode mode : FaceModes.round()) {
                    button(sideButton(resource, face, mode), player -> settable(player,
                            () -> machine.sides().set(resource, face, mode)));
                }
            }
            for (int what = PUSH; what <= PULL_DOWN + 2; what++) {
                final int chosen = what;
                button(automationButton(resource, what), player -> settable(player,
                        () -> machine.sides().setAutomation(resource, automate(machine.sides().automation(resource),
                                chosen, resource))));
            }
            button(FIRST_FILTER + resources.indexOf(resource), player -> settable(player,
                    () -> machine.sides().setAutomation(resource, machine.sides().automation(resource)
                            .filtered(filterOf(getCarried(), resource)))));
        }
        if (machine instanceof ProcessingMachineBlockEntity processing) {
            for (int slot = 0; slot < processing.layout().firstOutput(); slot++) {
                final int index = slot;
                button(FIRST_LOCK + slot, player -> settable(player, () -> {
                    final ItemStack held = machine.getInventory().getStackInSlot(index);
                    if (held.isEmpty() || !processing.locks().locked(index).isEmpty()) {
                        processing.locks().unlock(index);
                    } else {
                        processing.locks().lock(index, held);
                    }
                }));
            }
        }
    }

    /* Runs {@code change} for a player the machine lets set it up. */
    private void settable(final Player player, final Runnable change) {
        if (player instanceof ServerPlayer server && machine.maySetUp(server)) {
            change.run();
        }
    }

    /* Runs {@code change} for a player the machine lets break it, its owner's trust: who may use it is theirs. */
    private void owner(final Player player, final Runnable change) {
        if (player instanceof ServerPlayer server && machine.mayBreak(server)) {
            change.run();
        }
    }

    /*
     * The filter a hand holding {@code held} sets for {@code resource}: only its liquid, for liquids, only the item
     * otherwise, and everything for an empty hand or a container with no liquid.
     */
    private static ItemFilter filterOf(final ItemStack held, final MachineResource resource) {
        if (held.isEmpty()) {
            return ItemFilter.EVERYTHING;
        }
        if (resource == MachineResource.FLUIDS) {
            return FluidUtil.getFluidContained(held).map(fluid -> ItemFilter.only(new ItemFilter.Fuzzy(
                    BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString(), 0L))).orElse(ItemFilter.EVERYTHING);
        }
        return ItemFilter.only(new ItemFilter.Fuzzy(BuiltInRegistries.ITEM.getKey(held.getItem()).toString(), 0L));
    }

    /* What one press of a pushing and pulling control makes of {@code now}. */
    private static MachineSides.Automation automate(final MachineSides.Automation now, final int what,
                                                    final MachineResource resource) {
        final int unit = MachineWheels.unit(resource);
        if (what == PUSH) {
            return now.pushing(!now.push(), now.pushRate());
        }
        if (what == PULL) {
            return now.pulling(!now.pull(), now.pullRate());
        }
        final boolean push = what < PULL_UP;
        final int digit = (what - (push ? PUSH_UP : PULL_UP)) % 3;
        final boolean up = push ? what < PUSH_DOWN : what < PULL_DOWN;
        final int shown = (push ? now.pushRate() : now.pullRate()) / unit;
        final int next = MachineWheels.turn(shown, digit, up);
        return push ? now.pushing(now.push(), next * unit) : now.pulling(now.pull(), next * unit);
    }

    /** A slot shown on one tab only. */
    private final class TabSlot extends SlotItemHandler {

        private final int shownOn;
        private final boolean output;

        private TabSlot(final IItemHandler handler, final int index, final int x, final int y, final int shownOn,
                        final boolean output) {
            super(handler, index, x, y);
            this.shownOn = shownOn;
            this.output = output;
        }

        @Override
        public boolean isActive() {
            return tab == this.shownOn;
        }

        @Override
        public boolean mayPlace(final ItemStack stack) {
            return !this.output && super.mayPlace(stack);
        }
    }
}
