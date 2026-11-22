/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.chain.machine;

import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The words stencilled on the parts of the chain machines' faces (ChainFaces), and the machines' names as their name
 * plates write them, each a sentence of its own, so a player reads them in their language. A face's file writes each
 * in English; what it writes besides (numbers, units, chemical formulas) is data, drawn as it is.
 */
@TextHolder
public enum ChainFaceWord {

    AGC(TextKey.of("jsc.machine_face_word.agc", "AGC")),
    AIR(TextKey.of("jsc.machine_face_word.air", "AIR")),
    ANNEAL(TextKey.of("jsc.machine_face_word.anneal", "ANNEAL")),
    AOI(TextKey.of("jsc.machine_face_word.aoi", "AOI")),
    BAGS(TextKey.of("jsc.machine_face_word.bags", "BAGS")),
    BRINE(TextKey.of("jsc.machine_face_word.brine", "BRINE")),
    CASE(TextKey.of("jsc.machine_face_word.case", "CASE")),
    COAT(TextKey.of("jsc.machine_face_word.coat", "COAT")),
    COLD(TextKey.of("jsc.machine_face_word.cold", "COLD")),
    COMP(TextKey.of("jsc.machine_face_word.comp", "COMP")),
    COMPUTER(TextKey.of("jsc.machine_face_word.computer", "COMPUTER")),
    COND(TextKey.of("jsc.machine_face_word.cond", "COND")),
    CRYO(TextKey.of("jsc.machine_face_word.cryo", "CRYO")),
    DIA(TextKey.of("jsc.machine_face_word.dia", "DIA")),
    DIE(TextKey.of("jsc.machine_face_word.die", "DIE")),
    DRY(TextKey.of("jsc.machine_face_word.dry", "DRY")),
    ETCH(TextKey.of("jsc.machine_face_word.etch", "ETCH")),
    EVAP(TextKey.of("jsc.machine_face_word.evap", "EVAP")),
    FILL(TextKey.of("jsc.machine_face_word.fill", "FILL")),
    FIRMWARE(TextKey.of("jsc.machine_face_word.firmware", "FIRMWARE")),
    FLAT(TextKey.of("jsc.machine_face_word.flat", "FLAT")),
    GEAR(TextKey.of("jsc.machine_face_word.gear", "GEAR")),
    GEARBOX(TextKey.of("jsc.machine_face_word.gearbox", "GEARBOX")),
    HEAT(TextKey.of("jsc.machine_face_word.heat", "HEAT")),
    HOT(TextKey.of("jsc.machine_face_word.hot", "HOT")),
    HPU(TextKey.of("jsc.machine_face_word.hpu", "HPU")),
    HV(TextKey.of("jsc.machine_face_word.hv", "HV")),
    LC(TextKey.of("jsc.machine_face_word.lc", "LC")),
    LOAD(TextKey.of("jsc.machine_face_word.load", "LOAD")),
    LOCK(TextKey.of("jsc.machine_face_word.lock", "LOCK")),
    MCC(TextKey.of("jsc.machine_face_word.mcc", "MCC")),
    MFC(TextKey.of("jsc.machine_face_word.mfc", "MFC")),
    MOULD(TextKey.of("jsc.machine_face_word.mould", "MOULD")),
    MS(TextKey.of("jsc.machine_face_word.ms", "MS")),
    OVEN(TextKey.of("jsc.machine_face_word.oven", "OVEN")),
    PAPER(TextKey.of("jsc.machine_face_word.paper", "PAPER")),
    PARTS(TextKey.of("jsc.machine_face_word.parts", "PARTS")),
    PASS(TextKey.of("jsc.machine_face_word.pass", "PASS")),
    PLACED(TextKey.of("jsc.machine_face_word.placed", "PLACED")),
    PLANT(TextKey.of("jsc.machine_face_word.plant", "PLANT")),
    RACKS(TextKey.of("jsc.machine_face_word.racks", "RACKS")),
    RES(TextKey.of("jsc.machine_face_word.res", "RES")),
    RINSE(TextKey.of("jsc.machine_face_word.rinse", "RINSE")),
    SAW(TextKey.of("jsc.machine_face_word.saw", "SAW")),
    SWAGE(TextKey.of("jsc.machine_face_word.swage", "SWAGE")),
    SYSTEM(TextKey.of("jsc.machine_face_word.system", "SYSTEM")),
    TAR(TextKey.of("jsc.machine_face_word.tar", "TAR")),
    TEST(TextKey.of("jsc.machine_face_word.test", "TEST")),
    TIC(TextKey.of("jsc.machine_face_word.tic", "TIC")),
    TOWER(TextKey.of("jsc.machine_face_word.tower", "TOWER")),
    VAC(TextKey.of("jsc.machine_face_word.vac", "VAC")),
    WIRE(TextKey.of("jsc.machine_face_word.wire", "WIRE")),
    WORK(TextKey.of("jsc.machine_face_word.work", "WORK")),
    X_RAY(TextKey.of("jsc.machine_face_word.x_ray", "X-RAY")),
    // The machines' names as their name plates write them, short.
    AIR_SEPARATION(TextKey.of("jsc.machine_face_word.air_separation", "AIR SEPARATION")),
    ASSEMBLY_ADVANCED(TextKey.of("jsc.machine_face_word.assembly_advanced", "ASSEMBLY ADVANCED")),
    ASSEMBLY_LEGACY(TextKey.of("jsc.machine_face_word.assembly_legacy", "ASSEMBLY LEGACY")),
    ASSEMBLY_STANDARD(TextKey.of("jsc.machine_face_word.assembly_standard", "ASSEMBLY STANDARD")),
    ASSEMBLY_TRANSITION(TextKey.of("jsc.machine_face_word.assembly_transition", "ASSEMBLY TRANSITION")),
    ASSEMBLY_VINTAGE(TextKey.of("jsc.machine_face_word.assembly_vintage", "ASSEMBLY VINTAGE")),
    BOARD_ETCHER(TextKey.of("jsc.machine_face_word.board_etcher", "BOARD ETCHER")),
    CDU(TextKey.of("jsc.machine_face_word.cdu", "CDU")),
    CERAMIC_KILN(TextKey.of("jsc.machine_face_word.ceramic_kiln", "CERAMIC KILN")),
    CHEM_REACTOR(TextKey.of("jsc.machine_face_word.chem_reactor", "CHEM REACTOR")),
    CHILLER(TextKey.of("jsc.machine_face_word.chiller", "CHILLER")),
    COIL_WINDER(TextKey.of("jsc.machine_face_word.coil_winder", "COIL WINDER")),
    COKE_OVEN(TextKey.of("jsc.machine_face_word.coke_oven", "COKE OVEN")),
    COOLING_TOWER(TextKey.of("jsc.machine_face_word.cooling_tower", "COOLING TOWER")),
    CRUSHER(TextKey.of("jsc.machine_face_word.crusher", "CRUSHER")),
    CRYSTAL_PULLER(TextKey.of("jsc.machine_face_word.crystal_puller", "CRYSTAL PULLER")),
    DEPOSITION(TextKey.of("jsc.machine_face_word.deposition", "DEPOSITION")),
    DISPLAY_LINE(TextKey.of("jsc.machine_face_word.display_line", "DISPLAY LINE")),
    ELECTROLYTIC(TextKey.of("jsc.machine_face_word.electrolytic", "ELECTROLYTIC")),
    GRINDER(TextKey.of("jsc.machine_face_word.grinder", "GRINDER")),
    INDUCTION(TextKey.of("jsc.machine_face_word.induction", "INDUCTION")),
    ION_IMPLANTER(TextKey.of("jsc.machine_face_word.ion_implanter", "ION IMPLANTER")),
    LAMINATOR(TextKey.of("jsc.machine_face_word.laminator", "LAMINATOR")),
    LITHOGRAPHY(TextKey.of("jsc.machine_face_word.lithography", "LITHOGRAPHY")),
    LS_PRINTER(TextKey.of("jsc.machine_face_word.ls_printer", "LS PRINTER")),
    OPTICAL_POLISHER(TextKey.of("jsc.machine_face_word.optical_polisher", "OPTICAL POLISHER")),
    PACKAGING_LINE(TextKey.of("jsc.machine_face_word.packaging_line", "PACKAGING LINE")),
    PLASMA_ETCHER(TextKey.of("jsc.machine_face_word.plasma_etcher", "PLASMA ETCHER")),
    PREBUILDER(TextKey.of("jsc.machine_face_word.prebuilder", "PREBUILDER")),
    PREINSTALLER(TextKey.of("jsc.machine_face_word.preinstaller", "PREINSTALLER")),
    ROLLING_MILL(TextKey.of("jsc.machine_face_word.rolling_mill", "ROLLING MILL")),
    SIEMENS(TextKey.of("jsc.machine_face_word.siemens", "SIEMENS")),
    WAFER_SAW(TextKey.of("jsc.machine_face_word.wafer_saw", "WAFER SAW")),
    WIRE_DRAWER(TextKey.of("jsc.machine_face_word.wire_drawer", "WIRE DRAWER"));

    private final TextKey text;

    private static final Map<String, ChainFaceWord> BY_ENGLISH = new HashMap<>();

    static {
        for (final ChainFaceWord word : values()) {
            BY_ENGLISH.put(word.text.english(), word);
        }
    }

    ChainFaceWord(final TextKey text) {
        this.text = text;
    }

    /** The word a face's file writes as {@code english}, or none for data (a number, a unit, a formula). */
    public static Optional<ChainFaceWord> of(final String english) {
        return Optional.ofNullable(BY_ENGLISH.get(english));
    }

    /** Its sentence. */
    public TextKey text() {
        return this.text;
    }
}
