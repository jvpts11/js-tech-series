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
 * How a network probe is set, kept in the item: the page its display shows, the row chosen on the members' page, and
 * its keys: HOLD keeps the reading still, SHOW puts it beside the crosshair while the probe is held, LIGHT lights the
 * display, and POWER switches it on and off.
 *
 * @param page  the display's page
 * @param row   the row chosen on the members' page, from 0
 * @param hold  whether the reading is held still
 * @param show  whether the reading shows beside the crosshair
 * @param light whether the display is lit
 * @param on    whether the probe is on
 */
public record ProbeState(Page page, int row, boolean hold, boolean show, boolean light, boolean on) {

    /** How a probe comes: on, on its summary, on the HUD, the rest off. */
    public static final ProbeState DEFAULT = new ProbeState(Page.SUMMARY, 0, false, true, false, true);

    public static final Codec<ProbeState> CODEC = RecordCodecBuilder.create(state -> state.group(
            StableCodecs.byName(Page.class).fieldOf("page").forGetter(ProbeState::page),
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("row").forGetter(ProbeState::row),
            Codec.BOOL.fieldOf("hold").forGetter(ProbeState::hold),
            Codec.BOOL.fieldOf("show").forGetter(ProbeState::show),
            Codec.BOOL.fieldOf("light").forGetter(ProbeState::light),
            Codec.BOOL.fieldOf("on").forGetter(ProbeState::on)
    ).apply(state, ProbeState::new));

    public static final StreamCodec<ByteBuf, ProbeState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodec(StableCodecs.byName(Page.class)), ProbeState::page,
            ByteBufCodecs.VAR_INT, ProbeState::row,
            ByteBufCodecs.BOOL, ProbeState::hold,
            ByteBufCodecs.BOOL, ProbeState::show,
            ByteBufCodecs.BOOL, ProbeState::light,
            ByteBufCodecs.BOOL, ProbeState::on,
            ProbeState::new);

    public ProbeState {
        row = Math.max(0, row);
    }

    /** The same, on {@code to}. */
    public ProbeState onPage(final Page to) {
        return new ProbeState(to, this.row, this.hold, this.show, this.light, this.on);
    }

    /** The same, its page {@code by} pages along, round past the last. */
    public ProbeState paged(final int by) {
        final List<Page> pages = Page.order();
        return onPage(pages.get(Math.floorMod(pages.indexOf(this.page) + by, pages.size())));
    }

    /** The same, the row {@code by} rows along, never above the first or below the {@code rows}th. */
    public ProbeState rowMoved(final int by, final int rows) {
        return new ProbeState(this.page, Math.max(0, Math.min(Math.max(0, rows - 1), this.row + by)), this.hold,
                this.show, this.light, this.on);
    }

    /** The same, its HOLD key pressed. */
    public ProbeState toggleHold() {
        return new ProbeState(this.page, this.row, !this.hold, this.show, this.light, this.on);
    }

    /** The same, its SHOW key pressed. */
    public ProbeState toggleShow() {
        return new ProbeState(this.page, this.row, this.hold, !this.show, this.light, this.on);
    }

    /** The same, its LIGHT key pressed. */
    public ProbeState toggleLight() {
        return new ProbeState(this.page, this.row, this.hold, this.show, !this.light, this.on);
    }

    /** The same, its POWER key pressed. */
    public ProbeState togglePower() {
        return new ProbeState(this.page, this.row, this.hold, this.show, this.light, !this.on);
    }

    /** A page of the display. */
    public enum Page implements IStableName {
        /** The line at a glance: its flow, its narrowest cable, its losses and its members. */
        SUMMARY("summary"),
        /** Every block it plugs into, by what it is to the line. */
        MEMBERS("members"),
        /** Its flow over the last minute. */
        FLOW("flow"),
        /** What its cables lose. */
        LOSSES("losses");

        private final String serializedName;

        Page(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }

        /** The pages in the order of the soft keys under the display. */
        public static List<Page> order() {
            return List.of(SUMMARY, MEMBERS, FLOW, LOSSES);
        }
    }
}
