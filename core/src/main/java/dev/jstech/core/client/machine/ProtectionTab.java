/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import static dev.jstech.core.machine.MachineScreenLayout.BATTERY_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.BATTERY_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.BATTERY_X;
import static dev.jstech.core.machine.MachineScreenLayout.BATTERY_Y;
import static dev.jstech.core.machine.MachineScreenLayout.FUSE_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.FUSE_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.FUSE_X;
import static dev.jstech.core.machine.MachineScreenLayout.FUSE_Y;
import static dev.jstech.core.machine.MachineScreenLayout.KEY_R;
import static dev.jstech.core.machine.MachineScreenLayout.KEY_X;
import static dev.jstech.core.machine.MachineScreenLayout.KEY_Y;
import static dev.jstech.core.machine.MachineScreenLayout.OWNER_PLATE_MOST;
import static dev.jstech.core.machine.MachineScreenLayout.OWNER_PLATE_X;
import static dev.jstech.core.machine.MachineScreenLayout.OWNER_PLATE_Y;

import dev.jstech.core.energy.Energy;
import dev.jstech.core.energy.EnergyClass;
import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.EnergyProtection;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.energy.IFuse;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.team.Access;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The Protection tab, as the approved design draws it: the class of the machine's port and an LCD of the line that
 * meets it, a red lamp flashing while a line too high has struck; the fuse holder, its fuse's rating and what the fuse
 * did; the battery bay with its charge in ten segments, the share left and how long it lasts at what the machine
 * draws; and the key switch of who may use the machine besides its owner, whose name is on the plate beneath.
 *
 * <p>A machine on FE has no port, fuse or battery: the tab says so and keeps the key switch.
 */
public final class ProtectionTab implements IMachineFace {

    private static final List<Access> ACCESSES = List.of(Access.PRIVATE, Access.TEAM, Access.PUBLIC);
    private static final int WORDS_X = 62;
    private static final int PORT_Y = 6;
    private static final int LINE_Y = 16;
    private static final int LINE_WIDTH = 50;
    private static final int LINE_HEIGHT = 10;
    private static final int OVERVOLT_LAMP_X = 120;
    private static final int OVERVOLT_LAMP_Y = 17;
    private static final int OVERVOLT_WORD_X = 130;
    private static final int OVERVOLT_WORD_Y = 18;
    private static final int FUSE_WORD_Y = 34;
    private static final int FUSE_STATE_Y = 72;
    private static final int FUSE_LINE_Y = 81;
    private static final int FUSE_SECOND_LINE_Y = 89;
    private static final int BATTERY_WORD_Y = 34;
    private static final int BATTERY_LEFT_X = 124;
    private static final int BATTERY_LEFT_Y = 88;
    private static final int SEGMENTS = 10;
    private static final int SEGMENT_PITCH = 3;
    private static final int FLASH = 4;
    private static final int CLIMB = 3;
    private static final int PERCENT = 100;
    private static final int SECONDS_A_MINUTE = 60;
    private static final int TICKS_A_SECOND = 20;
    private static final int ACCESS_WORD_ABOVE = 31;
    private static final int PORT_ROOM = 104;
    private static final int STATE_ROOM = 54;
    private static final int NO_PORT_Y = 40;
    private static final int OVERVOLT_ROOM = 36;

    @Override
    public void draw(final MachineView v) {
        final GuiGraphics g = v.g();
        final Font font = v.font();
        final MachineBlockEntity machine = v.machine();
        final EnergyProtection protection = machine.power().protection();
        final Optional<EnergyClass> port = machine.power().portClass();
        if (protection == null || port.isEmpty()) {
            MachineArt.engrave(g, font, MachineArt.fit(font, MachineWords.upper(MachineScreenTexts.NO_PORT),
                    PORT_ROOM), v.x(WORDS_X), v.y(NO_PORT_Y), false);
        } else {
            portAndLine(v, port.get(), protection);
            fuse(v, port.get(), protection);
            battery(v, protection);
        }
        key(v);
    }

    @Override
    public boolean clicked(final MachineView v, final double x, final double y, final int button) {
        final Ownership owned = v.machine().ownership();
        if (owned == null) {
            return false;
        }
        if (MachineScreenLayout.onDisc(x, y, KEY_X, KEY_Y, KEY_R)) {
            v.press().accept(MachineMenu.FIRST_ACCESS + (ACCESSES.indexOf(owned.access()) + 1) % ACCESSES.size());
            return true;
        }
        for (int i = 0; i < ACCESSES.size(); i++) {
            if (MachineScreenLayout.onWord(x, y, MachineScreenLayout.keyWord(KEY_X, KEY_Y,
                    MachineScreenLayout.keyAngle(i)), MachineArt.width(v.font(), MachineWords.upper(word(i))))) {
                v.press().accept(MachineMenu.FIRST_ACCESS + i);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Component> tooltip(final MachineView v, final double x, final double y) {
        final MachineBlockEntity machine = v.machine();
        if (MachineScreenLayout.onDisc(x, y, KEY_X, KEY_Y, KEY_R)) {
            final Ownership owned = machine.ownership();
            return List.of(GameText.component(MachineScreenTexts.TIP_ACCESS.with(owned == null
                    ? MachineScreenTexts.NOBODY.text() : owned.access().text())));
        }
        final Optional<EnergyClass> port = machine.power().portClass();
        if (port.isEmpty()) {
            return List.of();
        }
        if (MachineScreenLayout.on(x, y, WORDS_X, PORT_Y - 2, PORT_ROOM, LINE_Y + LINE_HEIGHT + 3 - PORT_Y)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_PORT.with(className(port.get()))));
        }
        if (MachineScreenLayout.on(x, y, FUSE_X, FUSE_Y, FUSE_WIDTH, FUSE_HEIGHT)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_FUSE));
        }
        if (MachineScreenLayout.on(x, y, BATTERY_X, BATTERY_Y, BATTERY_WIDTH, BATTERY_HEIGHT)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_BATTERY));
        }
        return List.of();
    }

    /* The port's class engraved, the LCD of the line, and the lamp of a line too high. */
    private static void portAndLine(final MachineView v, final EnergyClass port, final EnergyProtection protection) {
        final GuiGraphics g = v.g();
        final MachineBlockEntity machine = v.machine();
        MachineArt.engrave(g, v.font(), MachineArt.fit(v.font(), MachineWords.upper(MachineScreenTexts.PORT.with(
                className(port))), PORT_ROOM), v.x(WORDS_X), v.y(PORT_Y), false);
        final boolean high = protection.cut() || machine.power().burnt();
        final TextKey line = high ? MachineScreenTexts.LINE_HIGH
                : machine.status() == MachineStatus.UNDERVOLTAGE ? MachineScreenTexts.LINE_LOW
                : MachineScreenTexts.LINE_OK;
        MachineArt.lcd(g, v.font(), v.x(WORDS_X), v.y(LINE_Y), LINE_WIDTH, LINE_HEIGHT,
                List.of(GameText.resolve(line)), true, false);
        MachineArt.lamp(g, v.x(OVERVOLT_LAMP_X), v.y(OVERVOLT_LAMP_Y), MachineArt.PALETTE.get().lampFault(),
                high && flash(v));
        MachineArt.engrave(g, v.font(), MachineArt.fit(v.font(), MachineWords.upper(MachineScreenTexts.OVERVOLT),
                OVERVOLT_ROOM), v.x(OVERVOLT_WORD_X), v.y(OVERVOLT_WORD_Y), false);
    }

    /* The fuse holder, its two clips gripping the fuse's slot, the fuse's rating, and what it did. */
    private static void fuse(final MachineView v, final EnergyClass port, final EnergyProtection protection) {
        final GuiGraphics g = v.g();
        final Font font = v.font();
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final int hx = v.x(FUSE_X);
        final int hy = v.y(FUSE_Y);
        MachineArt.drop(g, hx, hy, hx + FUSE_WIDTH, hy + FUSE_HEIGHT);
        MachineArt.shade(g, hx, hy, hx + FUSE_WIDTH, hy + FUSE_HEIGHT, MachineArt.rgb(58, 62, 68), 0.4, 0.5);
        for (final int cx : new int[] {hx + 4, hx + 34}) {
            MachineArt.shade(g, cx, hy + 4, cx + 6, hy + 20, MachineArt.rgb(196, 170, 90), 0.5, 0.4);
            MachineArt.vline(g, cx + 2, hy + 6, hy + 18, MachineArt.rgb(120, 100, 40));
        }
        MachineArt.slot(g, hx + 13, hy + 3);
        final ItemStack held = protection.slots().getStackInSlot(EnergyProtection.FUSE);
        final Text rating = held.getItem() instanceof IFuse fuse ? fuseRating(v.machine().power().energy(), port,
                fuse.breaks(held)) : Text.literal("");
        MachineArt.engrave(g, font, MachineArt.fit(font, MachineWords.upper(held.getItem() instanceof IFuse
                ? MachineScreenTexts.FUSE_RATING.with(rating) : MachineScreenTexts.FUSE.text()), STATE_ROOM),
                v.x(FUSE_X), v.y(FUSE_WORD_Y), false);
        final List<TextKey> lines = new ArrayList<>();
        final TextKey state;
        final int colour;
        if (protection.cut()) {
            state = MachineScreenTexts.BLOWN;
            colour = flash(v) ? c.lampFault() : MachineArt.mix(c.lampFault(), c.black(), 0.3);
            lines.add(MachineScreenTexts.MACHINE_OFF);
            lines.add(MachineScreenTexts.UNHARMED);
        } else if (held.getItem() instanceof IFuse) {
            state = MachineScreenTexts.FUSE_OK;
            colour = c.lampRun();
            lines.add(MachineScreenTexts.GUARDING);
        } else {
            state = MachineScreenTexts.NO_FUSE;
            colour = c.lampWait();
            lines.add(MachineScreenTexts.UNPROTECTED);
        }
        MachineArt.text(g, font, MachineArt.fit(font, MachineWords.upper(state), STATE_ROOM), v.x(FUSE_X),
                v.y(FUSE_STATE_Y), colour);
        final int[] ys = {FUSE_LINE_Y, FUSE_SECOND_LINE_Y};
        for (int i = 0; i < lines.size(); i++) {
            MachineArt.engrave(g, font, MachineArt.fit(font, MachineWords.upper(lines.get(i)), STATE_ROOM),
                    v.x(FUSE_X), v.y(ys[i]), false);
        }
    }

    /* The battery bay: its slot, its charge in ten segments, the one charging climbing, the share and time left. */
    private static void battery(final MachineView v, final EnergyProtection protection) {
        final GuiGraphics g = v.g();
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final MachineBlockEntity machine = v.machine();
        final int bx = v.x(BATTERY_X);
        final int by = v.y(BATTERY_Y);
        MachineArt.bezel(g, bx, by, bx + BATTERY_WIDTH, by + BATTERY_HEIGHT, true);
        MachineArt.slot(g, bx + 5, by + 5);
        final Energy energy = machine.power().energy();
        final long[] charge = charge(protection.slots().getStackInSlot(EnergyProtection.BATTERY), energy);
        final int full = charge[1] <= 0 ? 0 : (int) (charge[0] * SEGMENTS / charge[1]);
        final boolean charging = charge[1] > 0 && charge[0] < charge[1]
                && machine.power().stored() >= machine.power().capacity();
        final int climbing = charging ? full + (int) v.tick() / CLIMB % CLIMB : full;
        final int segX = bx + 28;
        for (int k = 0; k < SEGMENTS; k++) {
            final int y = by + 32 - k * SEGMENT_PITCH;
            final boolean on = k < full || k < climbing;
            MachineArt.hline(g, segX, segX + 12, y, on ? c.lampRun() : MachineArt.mix(c.lampRun(), c.black(), 0.7));
            MachineArt.hline(g, segX, segX + 12, y + 1, on ? MachineArt.mix(c.lampRun(), c.black(), 0.3)
                    : MachineArt.mix(c.lampRun(), c.black(), 0.78));
        }
        MachineArt.engrave(g, v.font(), MachineArt.fit(v.font(), MachineWords.upper(MachineScreenTexts.BATTERY),
                BATTERY_WIDTH), v.x(BATTERY_X), v.y(BATTERY_WORD_Y), false);
        if (charge[1] > 0) {
            final long drawn = machine.drawn();
            final String left = drawn <= 0 ? GameText.resolve(MachineScreenTexts.NO_TIME)
                    : clock(charge[0] / drawn / TICKS_A_SECOND);
            MachineArt.engrave(g, v.font(), GameText.resolve(MachineScreenTexts.BATTERY_LEFT.with(
                    charge[0] * PERCENT / charge[1], left)), v.x(BATTERY_LEFT_X), v.y(BATTERY_LEFT_Y), false);
        }
    }

    /* The key switch of who may use it, and the owner's name on the plate beneath. */
    private static void key(final MachineView v) {
        final GuiGraphics g = v.g();
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final Ownership owned = v.machine().ownership();
        final int kx = v.x(KEY_X);
        final int ky = v.y(KEY_Y);
        MachineArt.engrave(g, v.font(), MachineWords.upper(MachineScreenTexts.ACCESS), kx, ky - ACCESS_WORD_ABOVE,
                true);
        MachineArt.disc(g, kx, ky, KEY_R, MachineArt.rgb(196, 170, 90), true);
        MachineArt.disc(g, kx, ky, KEY_R - 3, MachineArt.rgb(150, 126, 60), false);
        final int chosen = owned == null ? -1 : ACCESSES.indexOf(owned.access());
        final double angle = MachineScreenLayout.keyAngle(Math.max(0, chosen));
        for (int s = -4; s <= 4; s++) {
            MachineArt.dot(g, kx + (int) Math.round(s * Math.cos(angle + Math.PI / 2)),
                    ky + (int) Math.round(s * Math.sin(angle + Math.PI / 2)), MachineArt.rgb(40, 34, 20));
        }
        if (chosen >= 0) {
            final int fx = kx + (int) Math.round(11.5 * Math.cos(angle));
            final int fy = ky + (int) Math.round(11.5 * Math.sin(angle));
            final boolean upright = Math.abs(Math.sin(angle)) > 0.5;
            final int halfW = upright ? 3 : 1;
            final int halfH = upright ? 2 : 3;
            MachineArt.fill(g, fx - halfW, fy - halfH, fx + halfW + 1, fy + halfH + 1, MachineArt.rgb(210, 214, 220));
            MachineArt.frame(g, fx - halfW, fy - halfH, fx + halfW + 1, fy + halfH + 1, MachineArt.rgb(120, 124, 130));
        }
        for (int i = 0; i < ACCESSES.size(); i++) {
            final int[] word = MachineScreenLayout.keyWord(kx, ky, MachineScreenLayout.keyAngle(i));
            final String written = MachineWords.upper(word(i));
            if (i == chosen) {
                MachineArt.textCentred(g, v.font(), written, word[0], word[1], c.lit());
            } else {
                MachineArt.engrave(g, v.font(), written, word[0], word[1], true);
            }
        }
        final String name = owned == null ? MachineWords.upper(MachineScreenTexts.NOBODY) : ownerName(owned);
        MachineArt.plate(g, v.font(), v.x(OWNER_PLATE_X), v.y(OWNER_PLATE_Y),
                MachineArt.fit(v.font(), name, OWNER_PLATE_MOST - 16), c.aluminium(), c.plateInk());
    }

    /* The owner's name, as the players the game knows of here are named; a word for an owner who is away. */
    static String ownerName(final Ownership owned) {
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        final PlayerInfo info = connection == null ? null : connection.getPlayerInfo(owned.owner());
        return info == null ? MachineWords.upper(MachineScreenTexts.OWNER_AWAY)
                : info.getProfile().getName().toUpperCase(Locale.ROOT);
    }

    private static TextKey word(final int index) {
        return switch (ACCESSES.get(index)) {
            case PRIVATE -> MachineScreenTexts.PRIV;
            case TEAM -> MachineScreenTexts.TEAM;
            case PUBLIC -> MachineScreenTexts.PUB;
        };
    }

    /* A class's name and its rating: "LOW 400 V". */
    private static Text className(final EnergyClass of) {
        return of.ratingKey().<Text>map(rating -> Text.literal(GameText.resolve(of.name()) + "  "
                + GameText.resolve(rating))).orElse(of.name().text());
    }

    /* The rating of the highest line a fuse breaks, {@code breaks} classes above {@code port}; any line for all. */
    private static Text fuseRating(final Energy energy, final EnergyClass port, final int breaks) {
        final List<EnergyClass> classes = energy.classes();
        final int at = classes.indexOf(port);
        if (breaks == IFuse.ANY || at < 0 || at + breaks >= classes.size()) {
            return MachineScreenTexts.FUSE_ANY.text();
        }
        final EnergyClass top = classes.get(at + breaks);
        return top.ratingKey().map(TextKey::text).orElse(top.name().text());
    }

    /* What a battery holds and the most it holds, in the machine's energy: {stored, capacity}. */
    private static long[] charge(final ItemStack battery, final Energy energy) {
        if (battery.isEmpty()) {
            return new long[] {0L, 0L};
        }
        final IEnergyPort own = battery.getCapability(EnergyPorts.ITEM);
        if (own != null) {
            return new long[] {own.stored(), own.capacity()};
        }
        final IEnergyStorage fe = battery.getCapability(Capabilities.EnergyStorage.ITEM);
        return fe == null ? new long[] {0L, 0L}
                : new long[] {energy.fromFe(fe.getEnergyStored()), energy.fromFe(fe.getMaxEnergyStored())};
    }

    private static String clock(final long seconds) {
        return seconds / SECONDS_A_MINUTE + ":" + String.format(Locale.ROOT, "%02d", seconds % SECONDS_A_MINUTE);
    }

    private static boolean flash(final MachineView v) {
        return (int) v.tick() / FLASH % 2 == 0;
    }
}
