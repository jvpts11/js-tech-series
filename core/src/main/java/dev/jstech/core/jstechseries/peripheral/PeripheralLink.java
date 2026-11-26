/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.peripheral;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.util.Loaded;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.OptionalLong;

/**
 * The link from a peripheral to the owner at the other end of its cable, declared as one of its block entity's
 * fields: the owner's position, saved and sent to the players who see the peripheral, so a lamp that shows the link
 * is right on every side. Once a tick it finds an owner to link to, or drops a link that no longer holds.
 */
public final class PeripheralLink {

    /** How many ticks a peripheral waits between two searches of its cables. */
    public static final int SEARCH_INTERVAL_TICKS = 10;

    private final ValueField<Long> owner;
    /*
     * Whether its last search found no owner the cables reach, sent to the players who see it so a monitor can say
     * so on its glass; not saved, since a loaded peripheral searches at once.
     */
    private final BoolField noneInReach;
    private final PeripheralCableType type;
    private final ILinkWorld world;
    /* Ticks left before the next search; not saved, so a loaded peripheral checks its link at once. */
    private int cooldown;

    /** How near a player stands to a peripheral to be told, over the hotbar, what it linked to. */
    private static final int TELL_RANGE = 16;

    public PeripheralLink(final BlockEntityFields fields, final PeripheralCableType type, final ILinkWorld world) {
        this.owner = fields.nullable("LinkedOwner", Codec.LONG).save().toClient();
        this.noneInReach = fields.flag("NoneInReach", false).toClient();
        this.type = type;
        this.world = world;
        // A peripheral broken or replaced frees its place on its owner for another.
        fields.whenBroken(this::unlink);
        // Moved with a moving structure, it stays linked to an owner that moved with it.
        fields.whenMoved((was, move) -> {
            final Long at = owner.get();
            if (at != null && move.movedHere(BlockPos.of(at))) {
                owner.set(move.where(BlockPos.of(at)).asLong());
            }
        });
    }

    public PeripheralCableType cableType() {
        return type;
    }

    /** The owner's position, packed, while linked. */
    public Optional<Long> linkedOwner() {
        return Optional.ofNullable(owner.get());
    }

    /** The owner's position while linked, or null. */
    public @Nullable BlockPos ownerPos() {
        final Long at = owner.get();
        return at == null ? null : BlockPos.of(at);
    }

    /**
     * Whether, unlinked, its last search found no owner its cables reach, as opposed to one that had no room for it;
     * on both sides.
     */
    @ApiStatus.Experimental
    public boolean noneInReach() {
        return owner.get() == null && noneInReach.get();
    }

    /** The validator linked this peripheral to the owner at {@code ownerPos}. */
    public void linked(final long ownerPos) {
        owner.set(ownerPos);
    }

    /** The link is gone, from either side. */
    public void unlinked() {
        owner.set(null);
    }

    /**
     * Once a tick on the server: links to an owner the cables reach, or drops a link whose owner or cable is gone, or
     * whose port the owner no longer has (its card taken out), to wait for a free one.
     */
    public void tick(final ServerLevel level, final BlockPos self) {
        final long here = self.asLong();
        final Long at = owner.get();
        if (at == null) {
            /*
             * Looking for an owner walks the whole reachable network, so an unlinked peripheral does not pay for it on
             * every tick: after a search the next waits a few ticks, spread by position so peripherals do not all
             * search on the same tick. A peripheral just placed searches at once, and a linked one checks its link
             * every tick, so a cut cable is seen the moment it is cut.
             */
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            cooldown = SEARCH_INTERVAL_TICKS + (int) Math.floorMod(here, 4L);
            final OptionalLong found = world.discoverOwner(level, here);
            noneInReach.set(found.isEmpty());
            if (found.isPresent() && world.validator(level).tryEstablishLink(found.getAsLong(), here)
                    instanceof ILinkResult.Established made) {
                tellNear(level, self, made);
            }
            return;
        }
        final BlockPos ownerAt = BlockPos.of(at);
        if (!level.isLoaded(ownerAt)) {
            // An owner whose chunk is away cannot be asked; the link stands until it can be, and nothing is loaded.
            return;
        }
        final boolean holds = level.getBlockEntity(ownerAt) instanceof IPeripheralOwner linkedTo
                && linkedTo.holdsPort(here);
        if (!holds || !world.validator(level).isLinkStillValid(at, here, type)) {
            unlink(level, self);
        }
    }

    /** Breaks the link from this side, freeing the owner's place for another peripheral; nothing when unlinked. */
    public void unlink(final ServerLevel level, final BlockPos self) {
        final Long at = owner.get();
        if (at != null && Loaded.blockEntity(level, BlockPos.of(at)) instanceof IPeripheralOwner linkedTo) {
            linkedTo.onEndpointUnlinked(self.asLong());
        }
        unlinked();
    }

    /*
     * Tells the players near a peripheral that just linked what it linked to, where that stands and over how many
     * cables: a monitor placed only lit, or did not, and a player could not tell which computer it hung from.
     */
    private static void tellNear(final ServerLevel level, final BlockPos self, final ILinkResult.Established made) {
        final BlockPos at = BlockPos.of(made.ownerPos());
        final Text what = GameText.of(level.getBlockState(self).getBlock().getName());
        final Text to = GameText.of(level.getBlockState(at).getBlock().getName());
        final Text where = Text.literal(at.getX() + ", " + at.getY() + ", " + at.getZ());
        final Text said = switch (made.pathLength()) {
            case 0 -> LinkTexts.AGAINST.with(what, to, where);
            case 1 -> LinkTexts.ONE_CABLE.with(what, to, where);
            default -> LinkTexts.CABLES.with(what, to, where, made.pathLength());
        };
        final Component line = GameText.component(said);
        for (final ServerPlayer player : level.players()) {
            if (player.blockPosition().distSqr(self) <= (double) TELL_RANGE * TELL_RANGE) {
                player.displayClientMessage(line, true);
            }
        }
    }

    /** How the links of one kind of cable are found in a world; the mod that lays the cables says. */
    public interface ILinkWorld {

        /** The owner the cables reach from the peripheral at {@code endpoint}, if any. */
        OptionalLong discoverOwner(ServerLevel level, long endpoint);

        /** The validator that establishes and checks links in {@code level}. */
        PeripheralLinkValidator validator(ServerLevel level);
    }
}
