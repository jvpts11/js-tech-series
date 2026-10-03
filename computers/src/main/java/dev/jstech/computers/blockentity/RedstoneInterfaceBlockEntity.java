/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.blockentity;

import com.mojang.serialization.Codec;
import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.block.RedstoneInterfaceBlock;
import dev.jstech.computers.menu.RedstoneInterfaceMenu;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.peripheral.IPeripheralEndpoint;
import dev.jstech.core.peripheral.IPeripheralOwner;
import dev.jstech.core.peripheral.PeripheralCableType;
import dev.jstech.core.peripheral.PeripheralLink;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Locale;
import java.util.Optional;

/**
 * A Redstone Interface's own state: the computer it hangs from, the name a program finds it by, whether it reads or
 * emits, the strength it emits, the strength it reads, and the program that last set it, if one did.
 *
 * <p>It is live only while linked: its computer powers it through the cable, as a computer powers what it has on a
 * USB or parallel port. A live interface reads the signal coming into its lens's face every tick, or emits its
 * strength there; one that is not reads nothing and emits nothing, but keeps its mode and its strength for when it is
 * cabled again.
 */
@TextHolder
public class RedstoneInterfaceBlockEntity extends SyncedBlockEntity implements IPeripheralEndpoint, GeoBlockEntity {

    private final PeripheralLink link = new PeripheralLink(fields(), PeripheralCableType.COMPUTING,
            PeripheralLinks.COMPUTING);
    private final AnimatableInstanceCache geckoCache = GeckoLibUtil.createInstanceCache(this);
    /** The name a player gave it; empty until one is given, when it answers to the word for it. */
    private final ValueField<String> name = fields().value("InterfaceName", Codec.STRING, "").save().toClient();
    /** Whether it emits; it reads otherwise. */
    private final BoolField emits = fields().flag("Emits", false).save().toClient();
    /** The strength it emits while it emits, 0 to 15. */
    private final IntField strength = fields().integer("Strength", 0).save().toClient();
    /** The strength it read at its last tick, while live and reading. */
    private final IntField reading = fields().integer("Reading", 0).toClient();
    /** The program that last set its mode or strength; empty when a player did, or nothing has. */
    private final ValueField<String> setBy = fields().value("SetBy", Codec.STRING, "").save().toClient();
    /** Whether the name last asked for on its screen is one another interface of its computer already has. */
    private final BoolField clash = fields().flag("NameClash", false).toMenu();
    /** Whether its computer disabled it, which leaves it linked and holding its port but unpowered. */
    private final BoolField disabled = fields().flag("Disabled", false).toClient();
    /** What it emits at this moment, which the game asks for and its neighbours were last told of. */
    private int output;
    /** The name being typed on its screen, taken when the screen closes; null while nothing is being typed. */
    @Nullable
    private String asked;

    /** The longest name an interface takes, the same as a speaker's. */
    public static final int MAX_NAME = 32;
    /** The strongest signal redstone carries. */
    public static final int MAX_STRENGTH = 15;

    /** What an interface with no name of its own answers to. */
    public static final TextKey DEFAULT_NAME = TextKey.of("jsc.redstone_interface.default_name",
            "Redstone Interface");

    public RedstoneInterfaceBlockEntity(final BlockPos pos, final BlockState state) {
        super(ComputingModule.REDSTONE_INTERFACE_BE.get(), pos, state);
    }

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state,
                                  final RedstoneInterfaceBlockEntity sensor) {
        if (level instanceof ServerLevel server) {
            sensor.link.tick(server, pos);
            sensor.settle(server, pos, state);
        }
    }

    /**
     * The interface linked to {@code owner} that answers to {@code name}, whatever the case of its letters, or null
     * when none does: how what runs on a computer finds one of its interfaces. A disabled one is not found.
     */
    @Nullable
    public static RedstoneInterfaceBlockEntity linkedTo(final ServerLevel level, final IPeripheralOwner owner,
                                                       final String name) {
        for (final long endpoint : owner.enabledEndpoints()) {
            final BlockPos at = BlockPos.of(endpoint);
            if (level.isLoaded(at) && level.getBlockEntity(at) instanceof RedstoneInterfaceBlockEntity sensor
                    && sensor.answersTo().equalsIgnoreCase(name)) {
                return sensor;
            }
        }
        return null;
    }

    @Override
    public PeripheralCableType cableType() {
        return link.cableType();
    }

    @Override
    public Optional<Long> linkedOwner() {
        return link.linkedOwner();
    }

    @Override
    public void onOwnerLinked(final long ownerPos) {
        link.linked(ownerPos);
    }

    @Override
    public void onOwnerUnlinked() {
        link.unlinked();
    }

    /** The computer it hangs from, or null. */
    @Nullable
    public BlockPos ownerPos() {
        return link.ownerPos();
    }

    /** Whether its computer powers it: whether it is linked to one that has not disabled it. */
    public boolean live() {
        return link.linkedOwner().isPresent() && !disabled.get();
    }

    /** The name a player gave it; empty while it has none. */
    public String name() {
        final String given = name.get();
        return given == null ? "" : given;
    }

    /** The name a program finds it by: the one a player gave it, or the word for it while it has none. */
    public String answersTo() {
        final String given = name.get();
        return given == null || given.isEmpty() ? DEFAULT_NAME.text().english() : given;
    }

    /**
     * Gives it {@code wanted} as its name, unless another interface of its computer is already called that, whatever
     * the case of its letters; an empty name gives it back the word for it. Whether the name was taken.
     */
    public boolean rename(final String wanted) {
        final String stripped = wanted.strip();
        final String clipped = stripped.length() > MAX_NAME ? stripped.substring(0, MAX_NAME) : stripped;
        if (!clipped.isEmpty() && level instanceof ServerLevel server && anotherIsCalled(server, clipped)) {
            return false;
        }
        name.set(clipped);
        return true;
    }

    /**
     * The name being typed on its screen, looked at as it is typed: its screen says at once whether another interface
     * of its computer already answers to it, whatever the case of its letters. Nothing is taken until the screen
     * closes, so the names a player passes through on the way to one are never given.
     */
    public void ask(final ServerLevel level, final String requested) {
        final String stripped = requested.strip();
        asked = stripped.length() > MAX_NAME ? stripped.substring(0, MAX_NAME) : stripped;
        clash.set(!asked.isEmpty() && anotherIsCalled(level, asked));
    }

    /**
     * Takes the name asked for when its screen closes, unless it clashes; then the interface keeps the name it had. An
     * empty name gives it back the word for it and never clashes.
     */
    public void takeAskedName() {
        if (asked != null && !clash.get()) {
            name.set(asked);
        }
        asked = null;
        clash.set(false);
    }

    /** Whether the name last asked for on its screen is one another interface of its computer already has. */
    public boolean nameClashes() {
        return clash.get();
    }

    /** What its screen opens with: its name, and the computer it hangs from. */
    public RedstoneInterfaceMenu.Opening opening(final ServerLevel level) {
        String computerName = "";
        String computerKind = "";
        final BlockPos owner = link.ownerPos();
        if (owner != null && Loaded.blockEntity(level, owner) instanceof AbstractComputerBlockEntity computer) {
            computerName = computer.customName();
            computerKind = computer.getBlockState().getBlock().getDescriptionId();
        }
        return new RedstoneInterfaceMenu.Opening(worldPosition, name(), computerName, computerKind, era());
    }

    /** Whether it emits; it reads otherwise. */
    public boolean emits() {
        return emits.get();
    }

    /** The strength it emits while it emits. */
    public int strength() {
        return strength.get();
    }

    /** The strength it read at its last tick: what is coming into its lens's face, while it is live and reading. */
    public int reading() {
        return reading.get();
    }

    /** What it emits at this moment: its strength while it is live and emitting, else nothing. */
    public int output() {
        return output;
    }

    /** What its lens shows: the strength it reads or emits while live, and nothing while it is not. */
    public int shownStrength() {
        if (!live()) {
            return 0;
        }
        return emits() ? strength() : reading();
    }

    /** The program that last set it; empty when a player did, or nothing has. */
    public String setBy() {
        final String by = setBy.get();
        return by == null ? "" : by;
    }

    /** Makes it read; {@code by} is the program that asked, or empty for a player. */
    public void read(final String by) {
        emits.set(false);
        setBy.set(by);
    }

    /**
     * Makes it emit {@code level}, clamped to the strengths redstone has; {@code by} is the program that asked, or
     * empty for a player.
     */
    public void emit(final int level, final String by) {
        emits.set(true);
        strength.set(Math.max(0, Math.min(MAX_STRENGTH, level)));
        setBy.set(by);
    }

    /** The era of its housing. */
    public HardwareEra era() {
        return getBlockState().getBlock() instanceof RedstoneInterfaceBlock sensor ? sensor.era()
                : HardwareEra.STANDARD;
    }

    /* Nothing on it moves: its lens and lamps are shown and hidden by its renderer. */
    @Override
    public void registerControllers(final AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geckoCache;
    }

    /*
     * Once a tick, after its link: reads the signal at its lens while it reads, and tells the block in front when what
     * it emits changed, so a signal follows the computer within the tick it was set.
     */
    private void settle(final ServerLevel level, final BlockPos pos, final BlockState state) {
        final BlockPos owner = link.ownerPos();
        disabled.set(owner != null && Loaded.blockEntity(level, owner) instanceof IPeripheralOwner linkedTo
                && linkedTo.isDisabled(pos.asLong()));
        final boolean powered = live();
        reading.set(powered && !emits() ? readFront(level, pos, state) : 0);
        final int now = powered && emits() ? strength() : 0;
        if (now != output) {
            output = now;
            setChanged();
            if (state.getBlock() instanceof RedstoneInterfaceBlock sensor) {
                sensor.updateFront(level, pos, state);
            }
        }
    }

    /*
     * The signal coming into its lens's face, read as a repeater reads its input: what the block there emits toward
     * it, or the power of a redstone wire lying there.
     */
    private static int readFront(final ServerLevel level, final BlockPos pos, final BlockState state) {
        final Direction facing = state.getValue(RedstoneInterfaceBlock.FACING);
        final BlockPos front = pos.relative(facing);
        final int signal = level.getSignal(front, facing);
        if (signal >= MAX_STRENGTH) {
            return signal;
        }
        final BlockState there = level.getBlockState(front);
        return Math.max(signal, there.is(Blocks.REDSTONE_WIRE) ? there.getValue(RedStoneWireBlock.POWER) : 0);
    }

    private boolean anotherIsCalled(final ServerLevel level, final String wanted) {
        final BlockPos owner = link.ownerPos();
        if (owner == null || !(Loaded.blockEntity(level, owner) instanceof IPeripheralOwner linkedTo)) {
            return false;
        }
        final String folded = wanted.toLowerCase(Locale.ROOT);
        for (final long endpoint : linkedTo.linkedEndpoints()) {
            if (endpoint != worldPosition.asLong()
                    && Loaded.blockEntity(level, BlockPos.of(endpoint)) instanceof RedstoneInterfaceBlockEntity other
                    && other.answersTo().toLowerCase(Locale.ROOT).equals(folded)) {
                return true;
            }
        }
        return false;
    }
}
