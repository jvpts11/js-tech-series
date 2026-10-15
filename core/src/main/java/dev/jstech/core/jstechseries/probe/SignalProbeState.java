/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.probe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How a signal probe is set, kept in the item: the page its display shows, the frequency chosen on the radio's page,
 * and its keys, as on the network probe: HOLD keeps the reading still, SHOW puts it beside the crosshair while the
 * probe is held, LIGHT lights the display, and POWER switches it on and off.
 *
 * @param page  the display's page
 * @param row   the frequency chosen on the radio's page, from 0
 * @param hold  whether the reading is held still
 * @param show  whether the reading shows beside the crosshair
 * @param light whether the display is lit
 * @param on    whether the probe is on
 */
public record SignalProbeState(Page page, int row, boolean hold, boolean show, boolean light, boolean on) {

    /** How a probe comes: on, on what is here, on the HUD, the rest off. */
    public static final SignalProbeState DEFAULT = new SignalProbeState(Page.HERE, 0, false, true, false, true);

    public static final Codec<SignalProbeState> CODEC = RecordCodecBuilder.create(state -> state.group(
            StableCodecs.byName(Page.class).fieldOf("page").forGetter(SignalProbeState::page),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("row").forGetter(SignalProbeState::row),
            Codec.BOOL.fieldOf("hold").forGetter(SignalProbeState::hold),
            Codec.BOOL.fieldOf("show").forGetter(SignalProbeState::show),
            Codec.BOOL.fieldOf("light").forGetter(SignalProbeState::light),
            Codec.BOOL.fieldOf("on").forGetter(SignalProbeState::on)
    ).apply(state, SignalProbeState::new));

    public static final StreamCodec<ByteBuf, SignalProbeState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(StableCodecs.byName(Page.class)), SignalProbeState::page,
            ByteBufCodecs.VAR_INT, SignalProbeState::row,
            ByteBufCodecs.BOOL, SignalProbeState::hold,
            ByteBufCodecs.BOOL, SignalProbeState::show,
            ByteBufCodecs.BOOL, SignalProbeState::light,
            ByteBufCodecs.BOOL, SignalProbeState::on,
            SignalProbeState::new);

    public SignalProbeState {
        row = Math.max(0, row);
    }

    /** The same, on {@code to}. */
    public SignalProbeState onPage(final Page to) {
        return new SignalProbeState(to, this.row, this.hold, this.show, this.light, this.on);
    }

    /** The same, its page {@code by} pages along, round past the last. */
    public SignalProbeState paged(final int by) {
        final List<Page> pages = Page.order();
        return onPage(pages.get(Math.floorMod(pages.indexOf(this.page) + by, pages.size())));
    }

    /** The same, the row {@code by} rows along, never above the first or below the {@code rows}th. */
    public SignalProbeState rowMoved(final int by, final int rows) {
        return new SignalProbeState(this.page, Math.max(0, Math.min(Math.max(0, rows - 1), this.row + by)),
                this.hold, this.show, this.light, this.on);
    }

    /** The same, its HOLD key pressed. */
    public SignalProbeState toggleHold() {
        return new SignalProbeState(this.page, this.row, !this.hold, this.show, this.light, this.on);
    }

    /** The same, its SHOW key pressed. */
    public SignalProbeState toggleShow() {
        return new SignalProbeState(this.page, this.row, this.hold, !this.show, this.light, this.on);
    }

    /** The same, its LIGHT key pressed. */
    public SignalProbeState toggleLight() {
        return new SignalProbeState(this.page, this.row, this.hold, this.show, !this.light, this.on);
    }

    /** The same, its POWER key pressed. */
    public SignalProbeState togglePower() {
        return new SignalProbeState(this.page, this.row, this.hold, this.show, this.light, !this.on);
    }

    /** A page of the display. */
    public enum Page implements IStableName {
        /** The redstone on the wire it reads, and the frequencies heard where the player stands. */
        HERE("here"),
        /** The frequencies heard, one chosen: whose it is, where it comes from, and how well it arrives. */
        RADIO("radio");

        private final String serializedName;

        Page(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** The pages in the order of their tabs, each over two of the soft keys. */
        public static List<Page> order() {
            return List.of(HERE, RADIO);
        }
    }
}
