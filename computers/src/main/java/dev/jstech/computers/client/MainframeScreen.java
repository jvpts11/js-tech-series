/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Box;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.gui.layout.MainframeLayout;
import dev.jstech.computers.menu.MainframeMenu;
import dev.jstech.core.jstechseries.theme.client.JsTechTheme;
import dev.jstech.core.jstechseries.network.FailoverRole;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The Mainframe's assembly screen: its cabinet with its cards in two I/O cages and its disks at the front, the
 * operations it has queued, running and done under the case, and on its front panel what it moves a tick, its parallel
 * queues (each queue's speed in the tooltip), its RAM buffer and its network; its switches are auto start and failover.
 */
public class MainframeScreen extends AbstractMachineAssemblyScreen<MainframeMenu> {

    public MainframeScreen(final MainframeMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    /** Window-relative centre of the power button (client tests press it the way the player does). */
    public static int powerButtonX() {
        final Box power = MainframeLayout.geometry().power();
        return power.x() + power.w() / 2;
    }

    public static int powerButtonY() {
        final Box power = MainframeLayout.geometry().power();
        return power.y() + power.h() / 2;
    }

    @Override
    protected Era caseEra() {
        final Era era = menu.hardwareEra();
        return era == null ? HardwareEra.STANDARD : era;
    }

    @Override
    protected AssemblyGeometry makeGeometry() {
        return MainframeLayout.geometry();
    }

    @Override
    protected String machineTitle() {
        return GameText.resolve(AssemblyTexts.TITLE_MAINFRAME);
    }

    @Override
    protected String stateWord() {
        if (menu.networkState() == MainframeBlockEntity.NET_STATE_CONFLICT) {
            return GameText.resolve(AssemblyTexts.CONFLICT);
        }
        if (!menu.buildValid()) {
            return GameText.resolve(AssemblyTexts.OFFLINE);
        }
        if (menu.isRunning()) {
            // A Passive Failover member is powered and synced, not orchestrating.
            return GameText.resolve(menu.failoverRole() == FailoverRole.PASSIVE ? AssemblyTexts.STANDBY
                    : AssemblyTexts.ONLINE);
        }
        return GameText.resolve(AssemblyTexts.READY);
    }

    @Override
    protected int stateColour() {
        if (menu.networkState() == MainframeBlockEntity.NET_STATE_CONFLICT || !menu.buildValid()) {
            return JsTechTheme.red();
        }
        if (menu.isRunning()) {
            return menu.failoverRole() == FailoverRole.PASSIVE ? JsTechTheme.amber() : JsTechTheme.green();
        }
        return JsTechTheme.amber();
    }

    @Override
    protected List<Readout> readouts() {
        final int net = menu.networkState();
        final String network = net == MainframeBlockEntity.NET_STATE_CONFLICT ? GameText.resolve(AssemblyTexts.CONFLICT)
                : net == MainframeBlockEntity.NET_STATE_LINKED ? GameText.resolve(AssemblyTexts.LINKED) : "--";
        final int networkColour = net == MainframeBlockEntity.NET_STATE_CONFLICT ? JsTechTheme.red()
                : net == MainframeBlockEntity.NET_STATE_LINKED ? JsTechTheme.accent() : JsTechTheme.dim();
        return List.of(
                new Readout(GameText.resolve(AssemblyTexts.CAPACITY),
                        JsTechTheme.fmt(menu.capacity()) + " " + GameText.resolve(AssemblyTexts.ITEMS_PER_TICK),
                        menu.isRunning() ? JsTechTheme.accent() : JsTechTheme.text()),
                // In amber while a weak card holds its queue below the processor's speed; the tooltip says which.
                new Readout(GameText.resolve(AssemblyTexts.QUEUES), String.valueOf(menu.parallelQueues()),
                        anyQueueHeld() ? JsTechTheme.amber() : JsTechTheme.text(), queueLines()),
                new Readout(GameText.resolve(AssemblyTexts.RAM_BUFFER),
                        JsTechTheme.fmt(menu.ramBuffer()) + " " + GameText.resolve(AssemblyTexts.ITEMS),
                        JsTechTheme.text()),
                new Readout(GameText.resolve(AssemblyTexts.NETWORK), network, networkColour));
    }

    @Override
    protected Operations operations() {
        return new Operations(String.valueOf(menu.pendingOps()), String.valueOf(menu.runningOps()),
                JsTechTheme.fmt(menu.completedOps()));
    }

    @Override
    protected List<ServiceSwitch> serviceSwitches() {
        return List.of(
                new ServiceSwitch(GameText.resolve(AssemblyTexts.AUTO_START), menu.isAutoStart(),
                        MainframeMenu.BUTTON_AUTOSTART),
                new ServiceSwitch(GameText.resolve(AssemblyTexts.FAILOVER), menu.failoverEnabled(),
                        MainframeMenu.BUTTON_FAILOVER));
    }

    @Override
    protected boolean[] lampsLit() {
        // The disk lamp shows the Mainframe working its disks, which it does while operations run.
        return new boolean[] {menu.isRunning(), menu.runningOps() > 0,
                menu.networkState() == MainframeBlockEntity.NET_STATE_LINKED};
    }

    @Override
    protected boolean boardIn() {
        return menu.hasBoard();
    }

    @Override
    protected int powerButton() {
        return MainframeMenu.BUTTON_POWER;
    }

    @Override
    protected boolean powerActive() {
        return !menu.isAutoStart();
    }

    @Override
    protected int powerLamp() {
        return menu.isRunning() ? JsTechTheme.green() : JsTechTheme.red();
    }

    @Override
    protected Component refusal(final Slot slot, final ItemStack carried) {
        return menu.refusal(slot, carried);
    }

    @Override
    protected List<Component> powerTooltip() {
        if (!menu.buildValid()) {
            return List.of(GameText.component(AssemblyTexts.NOT_A_COMPUTER),
                    GameText.component(AssemblyTexts.NEEDS_PARTS).withStyle(ChatFormatting.GOLD),
                    GameText.component(AssemblyTexts.THEN_SWITCH_ON).withStyle(ChatFormatting.GRAY));
        }
        if (menu.isAutoStart()) {
            return List.of(GameText.component(AssemblyTexts.AUTO_KEEPS_ON).withStyle(ChatFormatting.GRAY));
        }
        return List.of();
    }

    /* Each queue's speed: the processor's, then each card's in its cage, a card that holds its queue in amber. */
    private List<Component> queueLines() {
        if (menu.parallelQueues() <= 0) {
            return List.of();
        }
        final List<Component> lines = new ArrayList<>();
        final int cpu = menu.queueSpeed(0);
        lines.add(GameText.component(AssemblyTexts.QUEUE_CPU.with(JsTechTheme.fmt(cpu))));
        int queue = 1;
        for (final Seat seat : geometry().seats(SeatKind.CARD)) {
            final Slot slot = seatSlot(seat);
            if (slot == null || !slot.hasItem()) {
                continue;
            }
            final int speed = menu.queueSpeed(queue);
            lines.add(GameText.component(AssemblyTexts.QUEUE_GPU.with(queue, GameText.of(slot.getItem().getHoverName()),
                            JsTechTheme.fmt(speed)))
                    .withStyle(style -> style.withColor(speed < cpu ? JsTechTheme.amber() : JsTechTheme.text())));
            queue++;
        }
        if (anyQueueHeld()) {
            lines.add(GameText.component(AssemblyTexts.QUEUE_HELD).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    /* Whether a card holds any queue below the processor's speed. */
    private boolean anyQueueHeld() {
        final int cpu = menu.queueSpeed(0);
        for (int i = 1; i < menu.parallelQueues(); i++) {
            final int speed = menu.queueSpeed(i);
            if (speed > 0 && speed < cpu) {
                return true;
            }
        }
        return false;
    }
}
