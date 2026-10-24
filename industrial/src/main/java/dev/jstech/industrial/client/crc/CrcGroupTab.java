/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.client.crc;

import static dev.jstech.industrial.crc.CrcLayout.CABINETS_SHOWN;
import static dev.jstech.industrial.crc.CrcLayout.CABINET_WIDTH;
import static dev.jstech.industrial.crc.CrcLayout.CABINET_Y;
import static dev.jstech.industrial.crc.CrcLayout.CABLE_Y;
import static dev.jstech.industrial.crc.CrcLayout.COLUMN_X;
import static dev.jstech.industrial.crc.CrcLayout.FACE_WIDTH;
import static dev.jstech.industrial.crc.CrcLayout.FACE_X;
import static dev.jstech.industrial.crc.CrcLayout.FACE_Y;
import static dev.jstech.industrial.crc.CrcLayout.HEAD_Y;
import static dev.jstech.industrial.crc.CrcLayout.PLATE_X;
import static dev.jstech.industrial.crc.CrcLayout.ROWS_SHOWN;
import static dev.jstech.industrial.crc.CrcLayout.ROW_PITCH;
import static dev.jstech.industrial.crc.CrcLayout.ROW_Y;
import static dev.jstech.industrial.crc.CrcLayout.RULE_Y;
import static dev.jstech.industrial.crc.CrcLayout.SCHEMATIC_BOTTOM;
import static dev.jstech.industrial.crc.CrcLayout.SCHEMATIC_LEFT;
import static dev.jstech.industrial.crc.CrcLayout.SCHEMATIC_RIGHT;
import static dev.jstech.industrial.crc.CrcLayout.SCHEMATIC_TOP;
import static dev.jstech.industrial.crc.CrcLayout.TABLE_BOTTOM;
import static dev.jstech.industrial.crc.CrcLayout.TABLE_LEFT;
import static dev.jstech.industrial.crc.CrcLayout.TABLE_RIGHT;
import static dev.jstech.industrial.crc.CrcLayout.TABLE_TOP;
import static dev.jstech.industrial.crc.CrcLayout.TALL_FACE_HEIGHT;
import static dev.jstech.industrial.crc.CrcLayout.TEAMS_SHOWN;
import static dev.jstech.industrial.crc.CrcLayout.TOTALS_PITCH;
import static dev.jstech.industrial.crc.CrcLayout.TOTALS_RULE_Y;
import static dev.jstech.industrial.crc.CrcLayout.TOTALS_Y;

import dev.jstech.core.client.machine.MachineArt;
import dev.jstech.core.client.machine.MachineWords;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.research.Discoveries;
import dev.jstech.core.research.Discovery;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.industrial.crc.CrcLayout;
import dev.jstech.industrial.crc.CrcReadings;
import dev.jstech.industrial.crc.CrcScreenTexts;
import dev.jstech.industrial.research.GroupPower;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

/**
 * The Group tab, as the approved design draws it: its plates, RESEARCH GROUP and the colour of the cable that joins
 * it; the group as it is joined, its CRCs on their run of cable, this one marked; the table of what each computes,
 * how much of it its team counts and what it gives, with each team's pace and how far it is on the group's discovery;
 * and the rule of a group, engraved. More CRCs than its table's rows turn with the wheel.
 */
final class CrcGroupTab implements ICrcTab {

    private int rowScroll;

    /* The rule of a group, a line each. */
    private static final List<TextKey> RULE = List.of(CrcScreenTexts.RULE_ONE, CrcScreenTexts.RULE_TWO,
            CrcScreenTexts.RULE_THREE, CrcScreenTexts.RULE_FOUR, CrcScreenTexts.RULE_FIVE);
    private static final List<TextKey> HEADS = List.of(CrcScreenTexts.PLACE, CrcScreenTexts.TEAM,
            CrcScreenTexts.COMPUTES, CrcScreenTexts.COUNTED, CrcScreenTexts.GIVES, CrcScreenTexts.STATE);
    /* The labels by the first cabinets, when there is room for them: up to three CRCs. */
    private static final int LABELLED = 3;
    private static final int LABEL_Y = CABINET_Y + 2;
    private static final double WHOLE_FROM = 0.995;

    @Override
    public void draw(final CrcView view) {
        final GuiGraphics g = view.g();
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        CrcArt.face(g, view.x(FACE_X), view.y(FACE_Y), view.x(FACE_X + FACE_WIDTH), view.y(FACE_Y
                + TALL_FACE_HEIGHT));
        final CrcReadings.Group group = view.read().group();
        final Optional<DyeColor> dye = group.dye();
        final int ground = cableColour(group);
        final Text cable = group.cable() == CrcReadings.Group.NO_CABLE ? CrcScreenTexts.NO_CABLE.text()
                : dye.map(colour -> CrcScreenTexts.CABLE.with(GameText.of(Component.translatable("color.minecraft."
                + colour.getName())))).orElse(CrcScreenTexts.UNDYED.text());
        CrcTop.plates(view, MachineWords.upper(CrcScreenTexts.RESEARCH_GROUP), MachineWords.upper(cable),
                group.cable() == CrcReadings.Group.NO_CABLE ? m.aluminium() : ground,
                MachineArt.mix(ground, m.black(), 0.75));
        CrcTop.lamp(view, CrcLayout.TOP_LAMP_X, c.lampRun(), view.crc().running(),
                MachineWords.upper(MachineScreenTexts.RUN), CrcLayout.TOP_RUN_ROOM);
        schematic(view, group, ground);
        table(view, group);
        for (int i = 0; i < RULE.size(); i++) {
            CrcArt.engrave(g, view.font(), MachineWords.upper(RULE.get(i).with(Math.round(GroupPower.EACH_OTHER
                    * 100))), view.x(PLATE_X), view.y(RULE_Y[i]), FACE_X + FACE_WIDTH - 4 - PLATE_X);
        }
    }

    @Override
    public boolean scrolled(final CrcView view, final double x, final double y, final double amount) {
        if (!CrcLayout.on(x, y, TABLE_LEFT, TABLE_TOP, TABLE_RIGHT, TABLE_BOTTOM)) {
            return false;
        }
        final int rows = view.read().group().members().size();
        this.rowScroll = Math.clamp(this.rowScroll + (amount > 0 ? -1 : 1), 0, Math.max(0, rows - ROWS_SHOWN));
        return true;
    }

    @Override
    public List<Component> tooltip(final CrcView view, final double x, final double y) {
        final int rows = view.read().group().members().size();
        if (rows > ROWS_SHOWN && CrcLayout.on(x, y, TABLE_LEFT, TABLE_TOP, TABLE_RIGHT, TOTALS_RULE_Y)) {
            final int first = Math.min(this.rowScroll, rows - ROWS_SHOWN);
            return List.of(GameText.component(CrcScreenTexts.MORE_ROWS.with(first + 1, first + ROWS_SHOWN, rows)));
        }
        return List.of();
    }

    /* The group as it is joined: its CRCs on their run of cable, this one in blue, another team's marked. */
    private static void schematic(final CrcView view, final CrcReadings.Group group, final int cable) {
        final GuiGraphics g = view.g();
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        MachineArt.bezel(g, view.x(SCHEMATIC_LEFT), view.y(SCHEMATIC_TOP), view.x(SCHEMATIC_RIGHT),
                view.y(SCHEMATIC_BOTTOM), false);
        MachineArt.fill(g, view.x(SCHEMATIC_LEFT + 3), view.y(SCHEMATIC_TOP + 3), view.x(SCHEMATIC_RIGHT - 3),
                view.y(SCHEMATIC_BOTTOM - 3), c.schematic());
        final List<CrcReadings.Member> members = group.members();
        final int shown = Math.min(members.size(), CABINETS_SHOWN);
        if (shown == 0) {
            return;
        }
        if (shown > 1 && group.cable() != CrcReadings.Group.NO_CABLE) {
            final int x0 = view.x(CrcLayout.cabinetX(0, shown) + CABINET_WIDTH);
            final int x1 = view.x(CrcLayout.cabinetX(shown - 1, shown));
            MachineArt.hline(g, x0, x1, view.y(CABLE_Y), cable);
            MachineArt.hline(g, x0, x1, view.y(CABLE_Y + 1), MachineArt.mix(cable, m.black(), 0.3));
        }
        final String team = members.getFirst().team();
        for (int i = 0; i < shown; i++) {
            final int x = CrcLayout.cabinetX(i, shown);
            CrcArt.cabinet(g, view.font(), view.x(x), view.y(CABINET_Y), letter(i), i == 0);
            if (shown > LABELLED) {
                continue;
            }
            if (i == 0) {
                MachineArt.text(g, view.font(), GameText.resolve(CrcScreenTexts.THIS_ONE),
                        view.x(x + CABINET_WIDTH + 7), view.y(LABEL_Y), c.blue());
            } else if (!members.get(i).team().equals(team)) {
                final String other = GameText.resolve(CrcScreenTexts.OTHER_TEAM);
                MachineArt.text(g, view.font(), other, view.x(x - 5) - MachineArt.width(view.font(), other),
                        view.y(LABEL_Y), c.pencil());
            }
        }
    }

    /* The table: a row for each CRC shown, then each team's pace and how far it is on the group's discovery. */
    private void table(final CrcView view, final CrcReadings.Group group) {
        final GuiGraphics g = view.g();
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final MachineArt.Colours m = MachineArt.PALETTE.get();
        MachineArt.fill(g, view.x(TABLE_LEFT), view.y(TABLE_TOP), view.x(TABLE_RIGHT), view.y(TABLE_BOTTOM),
                c.card());
        MachineArt.frame(g, view.x(TABLE_LEFT), view.y(TABLE_TOP), view.x(TABLE_RIGHT), view.y(TABLE_BOTTOM),
                MachineArt.mix(c.enamel(), m.black(), 0.35));
        for (int i = 0; i < HEADS.size(); i++) {
            cell(view, MachineWords.upper(HEADS.get(i)), i + 1, HEAD_Y, c.pencil());
        }
        MachineArt.hline(g, view.x(TABLE_LEFT + 2), view.x(TABLE_RIGHT - 2), view.y(HEAD_Y + 8),
                MachineArt.mix(c.card(), m.black(), 0.25));
        final List<CrcReadings.Member> members = group.members();
        this.rowScroll = Math.clamp(this.rowScroll, 0, Math.max(0, members.size() - ROWS_SHOWN));
        for (int row = 0; row < ROWS_SHOWN && this.rowScroll + row < members.size(); row++) {
            final int index = this.rowScroll + row;
            final CrcReadings.Member member = members.get(index);
            final int y = ROW_Y + row * ROW_PITCH;
            final int ink = index == 0 ? c.blue() : c.ink();
            cell(view, letter(index), 0, y, ink);
            cell(view, index == 0 ? GameText.resolve(CrcScreenTexts.THIS_ONE) : GameText.resolve(
                    CrcScreenTexts.PLACE_AT.with(member.pos().getX(), member.pos().getY(), member.pos().getZ())),
                    1, y, ink);
            cell(view, member.teamName(), 2, y, ink);
            cell(view, GameText.resolve(CrcScreenTexts.RATE.with(CrcResearchTab.pace(member.computes()))), 3, y, ink);
            cell(view, counted(member), 4, y, ink);
            cell(view, GameText.resolve(CrcScreenTexts.RATE.with(CrcResearchTab.pace(member.gives()))), 5, y, ink);
            MachineArt.lamp(g, view.x(COLUMN_X[6] + 2), view.y(y - 1), lampColour(member.status().lamp()), true);
        }
        MachineArt.hline(g, view.x(TABLE_LEFT + 2), view.x(TABLE_RIGHT - 2), view.y(TOTALS_RULE_Y),
                MachineArt.mix(c.card(), m.black(), 0.25));
        totals(view, group);
    }

    /* Each team's pace, its CRCs' gifts summed, and how far it is on the group's discovery; this CRC's team first. */
    private static void totals(final CrcView view, final CrcReadings.Group group) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        final Map<String, List<CrcReadings.Member>> teams = new LinkedHashMap<>();
        for (final CrcReadings.Member member : group.members()) {
            if (!member.team().isEmpty()) {
                teams.computeIfAbsent(member.team(), team -> new ArrayList<>()).add(member);
            }
        }
        final Optional<Discovery> chosen = view.crc().chosenDiscovery().flatMap(Discoveries::byId);
        int line = 0;
        for (final Map.Entry<String, List<CrcReadings.Member>> team : teams.entrySet()) {
            if (line >= TEAMS_SHOWN) {
                break;
            }
            final List<CrcReadings.Member> crcs = team.getValue();
            final int y = TOTALS_Y + line * TOTALS_PITCH;
            cell(view, crcs.getFirst().teamName(), 2, y, c.ink());
            double pace = 0.0;
            final List<String> gifts = new ArrayList<>();
            for (final CrcReadings.Member crc : crcs) {
                pace += crc.gives();
                gifts.add(CrcResearchTab.pace(crc.gives()));
            }
            final Text sum;
            if (chosen.isEmpty()) {
                sum = CrcScreenTexts.TEAM_IDLE.with(CrcResearchTab.pace(pace));
            } else {
                final int percent = (int) Math.floor(group.share(team.getKey()) * 100);
                sum = crcs.size() > 1 ? CrcScreenTexts.TEAM_SUM.with(String.join(" + ", gifts),
                        CrcResearchTab.pace(pace), chosen.get().name().text(), percent)
                        : CrcScreenTexts.TEAM_ON.with(CrcResearchTab.pace(pace), chosen.get().name().text(), percent);
            }
            MachineArt.text(view.g(), view.font(), MachineArt.fit(view.font(), GameText.resolve(sum), TABLE_RIGHT - 4
                    - COLUMN_X[3]), view.x(COLUMN_X[3]), view.y(y), c.ink());
            line++;
        }
    }

    /* What a team counts of a CRC: the whole of it, or the share it gives. */
    private static String counted(final CrcReadings.Member member) {
        if (member.team().isEmpty() || member.computes() <= 0.0F) {
            return "-";
        }
        final double share = member.gives() / member.computes();
        return share >= WHOLE_FROM ? GameText.resolve(CrcScreenTexts.WHOLE)
                : GameText.resolve(CrcScreenTexts.PERCENT.with(Math.round(GroupPower.EACH_OTHER * 100)));
    }

    /* A cell of the table: {@code text} in column {@code column}, cut to the room before the next. */
    private static void cell(final CrcView view, final String text, final int column, final int y, final int colour) {
        final int right = column + 1 < COLUMN_X.length ? COLUMN_X[column + 1] - 3 : TABLE_RIGHT - 3;
        MachineArt.text(view.g(), view.font(), MachineArt.fit(view.font(), text, right - COLUMN_X[column]),
                view.x(COLUMN_X[column]), view.y(y), colour);
    }

    /* The cable's colour: its dye's, or the cable's own when it is not dyed. */
    private static int cableColour(final CrcReadings.Group group) {
        return group.dye().map(dye -> 0xFF << 24 | dye.getTextureDiffuseColor() & 0xFFFFFF)
                .orElse(CrcArt.PALETTE.get().cable());
    }

    /* The colour of a CRC's lamp in the table, by its state's lamp. */
    private static int lampColour(final MachineStatus.Lamp lamp) {
        final CrcArt.Colours c = CrcArt.PALETTE.get();
        return switch (lamp) {
            case RUN -> c.lampRun();
            case WAIT -> c.lampWait();
            case FAULT -> c.lampHot();
        };
    }

    /* The letter of the CRC at {@code index}: A, B, C. */
    private static String letter(final int index) {
        return String.valueOf((char) ('A' + index % 26));
    }
}
