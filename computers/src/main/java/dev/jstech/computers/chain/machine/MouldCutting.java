/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.chain.BoardParts;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.core.machine.ProcessingInput;
import dev.jstech.core.machine.ProcessingMachineBlockEntity.Processing;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Cutting a mould at an Injection Moulder: the computer linked to the moulder's peripheral port cuts the mould its
 * lithography program picks on the Mould Blank in the moulder, in one step's time, while the moulder moulds nothing;
 * the mould then stands where its blank was, the tool a job lends the moulder to mould its body in. Any running
 * computer cuts any mould, a list a family: the sockets' bodies, the slots' and the connectors'.
 */
@TextHolder
public final class MouldCutting {

    /** Why a mould is not cut: the moulder moulds a part. */
    public static final TextKey MOULDING = TextKey.of("jsc.lithography.refused.moulding",
            "The moulder is moulding: cut a mould when the part leaves");
    /** Why a mould is not cut: the moulder holds no Mould Blank. */
    public static final TextKey NO_BLANK = TextKey.of("jsc.lithography.refused.no_mould_blank",
            "Put a Mould Blank in the moulder to cut a mould on it");
    /** Why a mould is not cut: the moulder cuts one already. */
    public static final TextKey CUTTING = TextKey.of("jsc.lithography.refused.cutting",
            "The moulder is cutting a mould: wait for it to finish");
    /** Why a mould is not cut: there is no such mould. */
    public static final TextKey NOT_OFFERED = TextKey.of("jsc.lithography.refused.no_mould",
            "The moulder cuts no such mould");
    /** What a moulder is doing while it cuts. */
    public static final TextKey CUTTING_A_MOULD = TextKey.of("jsc.lithography.status.cutting", "Cutting a mould");
    /** What a moulder is doing while it moulds. */
    public static final TextKey MOULDING_A_PART = TextKey.of("jsc.lithography.status.moulding", "Moulding");
    private static final TextKey SOCKETS = TextKey.of("jsc.lithography.kind.sockets", "Sockets");
    private static final TextKey SLOTS = TextKey.of("jsc.lithography.kind.slots", "Slots");
    private static final TextKey CONNECTORS = TextKey.of("jsc.lithography.kind.connectors", "Connectors");
    private static final TextKey SOCKET_BODY = TextKey.of("jsc.lithography.mould.socket_body", "%s socket body");
    private static final TextKey BODY = TextKey.of("jsc.lithography.mould.body", "%s body");
    /** The process the computer runs the cutting as, and what it holds while it does. */
    static final String PROCESS = "mould";
    private static final int MEMORY_MB = 8;
    /* How long the process stays held after the moulder last asked, in ticks: a second. */
    private static final long HELD_TICKS = 20;
    private static final int TICKS_A_SECOND = 20;

    private MouldCutting() {
    }

    /** Every mould a moulder cuts, in the order of the program's lists: a family a list, the oldest part first. */
    public static List<Offer> offers() {
        final List<BoardParts.Component> components = new ArrayList<>(BoardParts.all());
        components.sort(Comparator.<BoardParts.Component, Kind>comparing(Kind::of)
                .thenComparingInt(component -> ChainParts.ERAS.indexOf(component.era()))
                .thenComparingInt(BoardParts.Component::pins));
        final List<Offer> out = new ArrayList<>();
        for (final BoardParts.Component component : components) {
            final Text part = GameText.of(component.item().get().getDescription());
            out.add(new Offer(Kind.of(component), component.mould().get(),
                    (component.kind() == BoardParts.Kind.SOCKET ? SOCKET_BODY : BODY).with(part)));
        }
        return List.copyOf(out);
    }

    /** The mould {@code mould} as the lithography program names it; none for an item that is no mould. */
    public static Optional<Offer> offerOf(final Item mould) {
        return offers().stream().filter(offer -> offer.mould() == mould).findFirst();
    }

    /**
     * Has {@code moulder} cut {@code mould} on the Mould Blank it holds; why it does not, when it does not: no such
     * mould, a part it moulds, a mould it is cutting already, or no blank.
     */
    public static Optional<TextKey> cut(final MoulderBlockEntity moulder, final Item mould) {
        if (offerOf(mould).isEmpty()) {
            return Optional.of(NOT_OFFERED);
        }
        if (moulder.moulding()) {
            return Optional.of(MOULDING);
        }
        if (moulder.cuts() != null && moulder.getProgress() > 0) {
            return Optional.of(CUTTING);
        }
        if (blankIn(moulder.inputs()) < 0) {
            return Optional.of(NO_BLANK);
        }
        moulder.cut(mould);
        return Optional.empty();
    }

    /** What the program calls the list of {@code kind}. */
    public static Text title(final Kind kind) {
        return switch (kind) {
            case SOCKETS -> SOCKETS.text();
            case SLOTS -> SLOTS.text();
            case CONNECTORS -> CONNECTORS.text();
        };
    }

    /** What {@code computer} holds now for the moulder at {@code moulder}'s place while it cuts, in megabytes. */
    public static int heldMb(final AbstractComputerBlockEntity computer, final MoulderBlockEntity moulder,
                             final long now) {
        return computer.hostedMb(PROCESS, moulder.getBlockPos().asLong(), now);
    }

    /**
     * The cutting of {@code mould} on the blank among {@code input}'s items, in one of the moulder's steps, while its
     * computer runs and holds the process; none without a blank, a mould asked for or a running computer.
     */
    static Optional<Cutting> work(final ServerLevel level, final MoulderBlockEntity moulder,
                                  final ProcessingInput input, @Nullable final Item mould) {
        if (mould == null || offerOf(mould).isEmpty()) {
            return Optional.empty();
        }
        final int blank = blankIn(input.items());
        final Optional<AbstractComputerBlockEntity> computer = LithoLink.computer(level, moulder);
        if (blank < 0 || computer.isEmpty() || !computer.get().isRunning()) {
            return Optional.empty();
        }
        final long self = moulder.getBlockPos().asLong();
        final long now = level.getGameTime();
        final int held = computer.get().hostedMb(PROCESS, self, now);
        if (MEMORY_MB > held && !computer.get().ramLedger().fits(MEMORY_MB - held)) {
            return Optional.empty();
        }
        computer.get().host(PROCESS, self, MEMORY_MB, now + HELD_TICKS);
        return Optional.of(new Cutting(blank, mould, Processing.single(blank, new ItemStack(mould),
                moulder.machine().seconds() * TICKS_A_SECOND)));
    }

    /* The first of {@code items} that is a Mould Blank, by its place; -1 for none. */
    static int blankIn(final List<ItemStack> items) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).is(ChainChips.MOULD_BLANK.get())) {
                return i;
            }
        }
        return -1;
    }

    /** A family of moulds, a list of the program. */
    public enum Kind {
        SOCKETS,
        SLOTS,
        CONNECTORS;

        /** Its name as the program's picks carry it. */
        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** The family of {@code name}, or none for a name of no family of moulds. */
        public static Optional<Kind> named(final String name) {
            for (final Kind kind : values()) {
                if (kind.serializedName().equals(name)) {
                    return Optional.of(kind);
                }
            }
            return Optional.empty();
        }

        /* The family a component's mould is listed in. */
        static Kind of(final BoardParts.Component component) {
            return switch (component.kind()) {
                case SOCKET -> SOCKETS;
                case MEMORY_SLOT, EXPANSION_SLOT -> SLOTS;
                case STORAGE, POWER -> CONNECTORS;
            };
        }
    }

    /**
     * A mould the program offers: its family, its item and what the program calls it, "LGA 1700 socket body".
     *
     * @param kind  its family
     * @param mould its item
     * @param name  what the program calls it
     */
    public record Offer(Kind kind, Item mould, Text name) {
    }

    /* A mould being cut: the slot its blank stands in, the mould, and the work. */
    record Cutting(int slot, Item mould, Processing work) {
    }
}
