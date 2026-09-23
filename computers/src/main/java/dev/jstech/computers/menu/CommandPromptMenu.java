/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.menu;

import dev.jstech.computers.registry.ComputingMenus;
import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.blockentity.IWatchedConsole;
import dev.jstech.computers.blockentity.MonitorBlockEntity;
import dev.jstech.computers.os.ConsoleIdentity;
import dev.jstech.computers.os.Platform;
import dev.jstech.computers.terminal.IComputerTerminalHost;
import dev.jstech.core.tier.HardwareEra;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A slotless menu for the Command Prompt. It holds no inventory (the console is driven entirely by command payloads) but being a real menu lets the server validate that the player has this prompt open for this host before running a typed line, exactly as the graphical terminal does.
 */
public class CommandPromptMenu extends AbstractContainerMenu {

    private final BlockPos monitorPos;
    private final BlockPos hostPos;
    @Nullable
    private final HardwareEra era;
    private final ContainerLevelAccess access;
    /*
     * Who the console says it is: the shell, the host name, the system and what it runs on, so the client can
     * draw the login banner and the initial prompt before the first server round-trip.
     */
    private final ConsoleIdentity console;
    /**
     * Which run of the machine this terminal belongs to.
     *
     * <p>What a terminal has printed belongs to the run that printed it, and the screen keeps its lines
     * between openings so walking away from one and coming back finds what was there. A restart ends the run,
     * and the lines from before it are not the new system's.
     */
    private final long session;

    /** The longest a shell's or a family's name travels at, and the longest a machine's or a system's does. */
    private static final int SHORT_NAME = 16;
    private static final int LONG_NAME = 48;

    public CommandPromptMenu(final int containerId, final Inventory playerInventory,
                             final BlockPos monitorPos, final BlockPos hostPos,
                             @Nullable final HardwareEra era, final long session) {
        this(containerId, playerInventory, monitorPos, hostPos, era, ConsoleIdentity.NONE, session);
    }

    public CommandPromptMenu(final int containerId, final Inventory playerInventory,
                             final BlockPos monitorPos, final BlockPos hostPos,
                             @Nullable final HardwareEra era, final ConsoleIdentity console, final long session) {
        this(ComputingMenus.COMMAND_PROMPT_MENU.get(), containerId, playerInventory, monitorPos, hostPos,
                era, console, session);
    }

    /**
     * Base constructor for the per-platform terminal menus (the MC-DOS terminal and the Unix TTY carry the
     * same data but open their own screens, so each gets its own menu type over this shared plumbing).
     */
    protected CommandPromptMenu(final MenuType<?> type, final int containerId,
                                final Inventory playerInventory, final BlockPos monitorPos, final BlockPos hostPos,
                                @Nullable final HardwareEra era, final ConsoleIdentity console,
                                final long session) {
        super(type, containerId);
        this.monitorPos = monitorPos;
        this.hostPos = hostPos;
        this.era = era;
        this.console = console == null ? ConsoleIdentity.NONE : console;
        this.session = session;
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), hostPos);
        IWatchedConsole.opened(playerInventory.player, hostPos);
    }

    @Override
    public void removed(final Player player) {
        super.removed(player);
        IWatchedConsole.closed(player, hostPos);
    }

    public static CommandPromptMenu fromNetwork(final int containerId, final Inventory playerInventory,
                                                final RegistryFriendlyByteBuf buf) {
        final OpenData data = readOpenBuffer(buf);
        return new CommandPromptMenu(containerId, playerInventory, data.monitor(), data.host(), data.era(),
                data.console(), data.session());
    }

    /** The shared open-buffer contents, so each terminal menu's {@code fromNetwork} reads them the same way. */
    protected record OpenData(BlockPos monitor, BlockPos host, @Nullable HardwareEra era, ConsoleIdentity console,
                              long session) {
    }

    protected static OpenData readOpenBuffer(final RegistryFriendlyByteBuf buf) {
        final BlockPos monitor = buf.readBlockPos();
        final BlockPos host = buf.readBlockPos();
        final HardwareEra era = HardwareEra.find(buf.readVarInt());
        final String shellId = buf.readUtf(SHORT_NAME);
        final String hostname = buf.readUtf(LONG_NAME);
        final String osLabel = buf.readUtf(LONG_NAME);
        final String platform = buf.readUtf(SHORT_NAME);
        final ConsoleIdentity console = ConsoleIdentity.ofWire(shellId, hostname, osLabel, platform,
                buf.readVarInt());
        return new OpenData(monitor, host, era, console, buf.readVarLong());
    }

    /** Writes the open buffer the client reconstructs from: the two positions plus the host era's id (-1 if none). */
    public static void writeOpenBuffer(final RegistryFriendlyByteBuf buf, final BlockPos monitorPos,
                                       final BlockPos hostPos, @Nullable final HardwareEra era,
                                       final long session) {
        writeOpenBuffer(buf, monitorPos, hostPos, era, ConsoleIdentity.NONE, session);
    }

    /** The full open buffer, including who the console says it is (nothing at all for a DOS-family OS). */
    public static void writeOpenBuffer(final RegistryFriendlyByteBuf buf, final BlockPos monitorPos,
                                       final BlockPos hostPos, @Nullable final HardwareEra era,
                                       final ConsoleIdentity console, final long session) {
        buf.writeBlockPos(monitorPos);
        buf.writeBlockPos(hostPos);
        buf.writeVarInt(era == null ? -1 : era.id());
        // Cut to what the wire takes rather than refused by it: a machine may be named at any length.
        buf.writeUtf(cut(console.shellId(), SHORT_NAME), SHORT_NAME);
        buf.writeUtf(cut(console.hostname(), LONG_NAME), LONG_NAME);
        buf.writeUtf(cut(console.osLabel(), LONG_NAME), LONG_NAME);
        buf.writeUtf(console.platformName(), SHORT_NAME);
        buf.writeVarInt(console.bits());
        buf.writeVarLong(session);
    }

    /** Which run of the machine this terminal belongs to, so its lines are not another run's. */
    public long session() {
        return this.session;
    }

    /** Who the console says it is, whole, for what greets and prompts from it. */
    public ConsoleIdentity console() {
        return this.console;
    }

    /** Whether the host's system is met at a Unix prompt, so the terminal wears a login banner and its prompt. */
    public boolean posixShell() {
        return this.console.posix();
    }

    /** The installed shell id ({@code bash}, {@code zsh}, {@code sh}), or {@code ""} for a DOS-family OS. */
    public String shellId() {
        return this.console.shellId();
    }

    /** The POSIX host name, or {@code ""} for a DOS-family OS. */
    public String hostname() {
        return this.console.hostname();
    }

    /** The family of the host's system, or null when it has none. */
    @Nullable
    public Platform platform() {
        return this.console.platform();
    }

    /** The installed OS display name, or {@code ""} when unknown. */
    public String osLabel() {
        return this.console.osLabel();
    }

    public BlockPos monitorPos() {
        return monitorPos;
    }

    public BlockPos hostPos() {
        return hostPos;
    }

    /** The host computer's board-derived hardware era, captured when the prompt was opened; {@code null} for none. */
    @Nullable
    public HardwareEra hardwareEra() {
        return era;
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(final Player player) {
        /*
         * The player sits at the MONITOR, not at the machine: reach is measured there, so a remote
         * session over a screen keeps working however far the machine itself is. The machine still
         * has to be alive and shown by that screen.
         */
        return access.evaluate((level, pos) -> {
            if (!(level.getBlockEntity(pos) instanceof IComputerTerminalHost) || !sessionAlive(level, pos)) {
                return false;
            }
            if (!(level.getBlockEntity(monitorPos)
                    instanceof MonitorBlockEntity monitor)) {
                // No monitor block (a firmware-opened prompt): fall back to standing at the machine.
                return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0;
            }
            return monitor.shows(pos)
                    && player.distanceToSqr(monitorPos.getX() + 0.5, monitorPos.getY() + 0.5,
                            monitorPos.getZ() + 0.5) <= 64.0;
        }, true);
    }

    /**
     * The console dies with its machine: powering the computer off, pulling the system disk, or ejecting a
     * live installer's medium closes the terminal on the next tick (the monitor then shows the firmware,
     * or nothing, on its next use).
     */
    static boolean sessionAlive(final Level level, final BlockPos pos) {
        if (level.getBlockEntity(pos)
                instanceof AbstractComputerBlockEntity computer) {
            return computer.isRunning() && computer.validateOsSession();
        }
        return true;
    }

    private static String cut(final String text, final int most) {
        return text.length() <= most ? text : text.substring(0, most);
    }
}
