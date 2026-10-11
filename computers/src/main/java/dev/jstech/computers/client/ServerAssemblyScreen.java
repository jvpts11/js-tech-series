/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.gui.layout.ServerAssemblyLayout;
import dev.jstech.computers.hardware.BuildValidation;
import dev.jstech.computers.hardware.ComputerBuild;
import dev.jstech.computers.menu.ServerAssemblyMenu;
import dev.jstech.computers.operation.payload.RenameServerPayload;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.format.Unit;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * A rack server's assembly screen, for the server in the player's hand: its chassis with its cards in two I/O cages and
 * its wall of fans, and on its front panel what it sends out a tick, its queues, its RAM buffer, what it draws against
 * its supply and its storage (or what its graphics cards add). A server is switched on in its rack, so the panel's top
 * is its checks: a lamp for whether the build is sound, and every problem in the tooltip.
 */
public class ServerAssemblyScreen extends AbstractMachineAssemblyScreen<ServerAssemblyMenu> {

    private final UnitFormatter fmt = UnitFormatter.forCurrentLocale();

    /* The build the slots hold and its checks, taken at the start of each frame. */
    @Nullable
    private ComputerBuild frameBuild;
    @Nullable
    private BuildValidation frameChecks;

    public ServerAssemblyScreen(final ServerAssemblyMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The build cannot change within a frame, so it is made and checked once for every part that draws it.
        this.frameBuild = menu.currentBuild();
        this.frameChecks = this.frameBuild == null ? null : this.frameBuild.validate();
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    protected HardwareEra caseEra() {
        final HardwareEra era = menu.hardwareEra();
        return era == null ? HardwareEra.STANDARD : era;
    }

    @Override
    protected AssemblyGeometry makeGeometry() {
        return ServerAssemblyLayout.geometry();
    }

    @Override
    protected String machineTitle() {
        // A supercomputer's node is built on the same screen as a server, but it is not one.
        return GameText.resolve(node() ? AssemblyTexts.TITLE_SUPERCOMPUTER_NODE : AssemblyTexts.TITLE_SERVER);
    }

    @Override
    protected NameField nameField() {
        // Each keystroke names the Server in the hand.
        return new NameField(GameText.component(node() ? AssemblyTexts.NAME_THIS_NODE : AssemblyTexts.NAME_THIS_SERVER),
                menu.serverName(),
                RenameServerPayload.MAX_LEN,
                s -> PacketDistributor.sendToServer(new RenameServerPayload(s)));
    }

    @Override
    protected String stateWord() {
        return GameText.resolve(frameBuild == null ? AssemblyTexts.UNASSEMBLED
                : !frameChecks.valid() ? AssemblyTexts.ERROR
                : frameBuild.rams().isEmpty() ? AssemblyTexts.WARN : AssemblyTexts.READY);
    }

    @Override
    protected int stateColour() {
        return frameBuild == null ? JsTechTheme.dim() : !frameChecks.valid() ? JsTechTheme.red()
                : frameBuild.rams().isEmpty() ? JsTechTheme.amber() : JsTechTheme.green();
    }

    @Override
    protected List<Readout> readouts() {
        final ComputerBuild build = frameBuild;
        final int draw = build == null ? 0 : build.powerDraw();
        final int watt = build == null ? 0 : build.psu().wattage();
        final Readout storage = build != null && build.serverGpuBonus() > 0
                // A server's graphics cards add to what it sends out: what they add, against its processor's capacity.
                ? new Readout(GameText.resolve(AssemblyTexts.GPU_BONUS),
                        GameText.resolve(AssemblyTexts.GPU_BONUS_VALUE.with(build.serverGpuBonusPercent(),
                                fmt.compact(build.serverGpuBonus(), Unit.IT_PER_TICK))), JsTechTheme.accent())
                // Its drives live in the rack's hot-swap bays, so the assembly has no storage of its own to meter.
                : new Readout(GameText.resolve(AssemblyTexts.STORAGE), GameText.resolve(AssemblyTexts.IN_THE_RACK),
                        JsTechTheme.dim(), List.of(GameText.component(AssemblyTexts.DRIVES_IN_BAYS)));
        return List.of(
                new Readout(GameText.resolve(AssemblyTexts.ORCHESTRATION),
                        build == null ? "0" : fmt.compact(build.totalCapacity(), Unit.IT_PER_TICK), JsTechTheme.text()),
                new Readout(GameText.resolve(AssemblyTexts.QUEUES),
                        build == null ? "0" : String.valueOf(build.parallelQueues()), JsTechTheme.text()),
                new Readout(GameText.resolve(AssemblyTexts.RAM_BUFFER),
                        build == null ? "0" : JsTechTheme.fmt(build.ramBuffer()) + " "
                                + GameText.resolve(AssemblyTexts.ITEMS), JsTechTheme.text()),
                new Readout(GameText.resolve(AssemblyTexts.DRAW), GameText.resolve(build == null
                        ? AssemblyTexts.NO_WATTS.text() : AssemblyTexts.DRAW_OF.with(draw, watt)),
                        draw > watt ? JsTechTheme.red() : JsTechTheme.text()),
                storage);
    }

    @Override
    protected List<ServiceSwitch> serviceSwitches() {
        return List.of();
    }

    @Override
    protected boolean[] lampsLit() {
        return new boolean[] {frameBuild != null && frameChecks.valid(), false, false};
    }

    @Override
    protected boolean boardIn() {
        final Slot board = seatSlot(geometry().seat(SeatKind.BOARD));
        return board != null && board.hasItem();
    }

    @Override
    protected String powerWord() {
        return GameText.resolve(AssemblyTexts.CHECKS);
    }

    @Override
    protected int powerLamp() {
        return stateColour();
    }

    @Override
    protected List<Component> powerTooltip() {
        if (frameBuild == null) {
            return List.of(GameText.component(AssemblyTexts.INSERT_TO_BEGIN).withStyle(ChatFormatting.GRAY));
        }
        final List<Text> problems = frameChecks.problems();
        if (problems.isEmpty()) {
            return List.of(GameText.component(AssemblyTexts.ALL_CHECKS_PASSED).withStyle(ChatFormatting.GREEN));
        }
        final List<Component> lines = new ArrayList<>();
        for (final Text problem : problems) {
            lines.add(GameText.component(problem).withStyle(ChatFormatting.RED));
        }
        return lines;
    }

    /* Whether the server in the hand is a supercomputer's node. */
    private boolean node() {
        return menu.chassis().rackType() == RackChassis.RackType.SUPERCOMPUTER;
    }
}
