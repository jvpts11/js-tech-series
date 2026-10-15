/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries;

import dev.jstech.core.material.MaterialKind;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.Locale;
import java.util.Optional;

/**
 * The 118 elements of the periodic table as it is known, as data: atomic number, symbol, name, the kind each is at
 * room temperature (a metal, a crystal grown as silicon is, a non-metal solid, a liquid as bromine and mercury, a gas),
 * and the colour its things are drawn in, near the pure element's. The elements beyond 118 are not here: their names
 * can be given in a world ({@link Superheavies}). Each is a material of {@link Elements}.
 */
@TextHolder
public enum Element {
    HYDROGEN(1, "H", "Hydrogen", MaterialKind.GAS, 0xE8F0FF),
    HELIUM(2, "He", "Helium", MaterialKind.GAS, 0xF2E6FF),
    LITHIUM(3, "Li", "Lithium", MaterialKind.METAL, 0xC8CCD0),
    BERYLLIUM(4, "Be", "Beryllium", MaterialKind.METAL, 0xA8AEB2),
    BORON(5, "B", "Boron", MaterialKind.NONMETAL_SOLID, 0x5F5A55),
    CARBON(6, "C", "Carbon", MaterialKind.NONMETAL_SOLID, 0x3A3A3C),
    NITROGEN(7, "N", "Nitrogen", MaterialKind.GAS, 0xE6EEFF),
    OXYGEN(8, "O", "Oxygen", MaterialKind.GAS, 0xDDEBFF),
    FLUORINE(9, "F", "Fluorine", MaterialKind.GAS, 0xE8E89A),
    NEON(10, "Ne", "Neon", MaterialKind.GAS, 0xFFDCC8),
    SODIUM(11, "Na", "Sodium", MaterialKind.METAL, 0xD6D8DA),
    MAGNESIUM(12, "Mg", "Magnesium", MaterialKind.METAL, 0xC8CACC),
    ALUMINIUM(13, "Al", "Aluminium", MaterialKind.METAL, 0xC9D0D9),
    SILICON(14, "Si", "Silicon", MaterialKind.CRYSTAL, 0x56606F),
    PHOSPHORUS(15, "P", "Phosphorus", MaterialKind.NONMETAL_SOLID, 0xD8CFA8),
    SULFUR(16, "S", "Sulfur", MaterialKind.NONMETAL_SOLID, 0xE6D84A),
    CHLORINE(17, "Cl", "Chlorine", MaterialKind.GAS, 0xC7D94A),
    ARGON(18, "Ar", "Argon", MaterialKind.GAS, 0xEADCFF),
    POTASSIUM(19, "K", "Potassium", MaterialKind.METAL, 0xCED0D2),
    CALCIUM(20, "Ca", "Calcium", MaterialKind.METAL, 0xD8D8D0),
    SCANDIUM(21, "Sc", "Scandium", MaterialKind.METAL, 0xC9C9C4),
    TITANIUM(22, "Ti", "Titanium", MaterialKind.METAL, 0x9893A8),
    VANADIUM(23, "V", "Vanadium", MaterialKind.METAL, 0xA7AAB4),
    CHROMIUM(24, "Cr", "Chromium", MaterialKind.METAL, 0xC3CBD3),
    MANGANESE(25, "Mn", "Manganese", MaterialKind.METAL, 0xA6A29B),
    IRON(26, "Fe", "Iron", MaterialKind.METAL, 0xA8A9AB),
    COBALT(27, "Co", "Cobalt", MaterialKind.METAL, 0x8D97AE),
    NICKEL(28, "Ni", "Nickel", MaterialKind.METAL, 0xBDB89E),
    COPPER(29, "Cu", "Copper", MaterialKind.METAL, 0xD9773F),
    ZINC(30, "Zn", "Zinc", MaterialKind.METAL, 0xB8C2CC),
    GALLIUM(31, "Ga", "Gallium", MaterialKind.METAL, 0xC6CCD6),
    GERMANIUM(32, "Ge", "Germanium", MaterialKind.CRYSTAL, 0x8E949B),
    ARSENIC(33, "As", "Arsenic", MaterialKind.NONMETAL_SOLID, 0x6E6E70),
    SELENIUM(34, "Se", "Selenium", MaterialKind.NONMETAL_SOLID, 0x7B3B36),
    BROMINE(35, "Br", "Bromine", MaterialKind.LIQUID, 0x8B2A1E),
    KRYPTON(36, "Kr", "Krypton", MaterialKind.GAS, 0xF0F4FF),
    RUBIDIUM(37, "Rb", "Rubidium", MaterialKind.METAL, 0xC8C4BA),
    STRONTIUM(38, "Sr", "Strontium", MaterialKind.METAL, 0xD4D0C4),
    YTTRIUM(39, "Y", "Yttrium", MaterialKind.METAL, 0xC4C6C8),
    ZIRCONIUM(40, "Zr", "Zirconium", MaterialKind.METAL, 0xB8BDC2),
    NIOBIUM(41, "Nb", "Niobium", MaterialKind.METAL, 0x9CA0B8),
    MOLYBDENUM(42, "Mo", "Molybdenum", MaterialKind.METAL, 0xA6AAB0),
    TECHNETIUM(43, "Tc", "Technetium", MaterialKind.METAL, 0xA9ABAE),
    RUTHENIUM(44, "Ru", "Ruthenium", MaterialKind.METAL, 0xB2B4B8),
    RHODIUM(45, "Rh", "Rhodium", MaterialKind.METAL, 0xC9CBCF),
    PALLADIUM(46, "Pd", "Palladium", MaterialKind.METAL, 0xC7C6C1),
    SILVER(47, "Ag", "Silver", MaterialKind.METAL, 0xD8DADC),
    CADMIUM(48, "Cd", "Cadmium", MaterialKind.METAL, 0xA9AEB4),
    INDIUM(49, "In", "Indium", MaterialKind.METAL, 0xB7BAC4),
    TIN(50, "Sn", "Tin", MaterialKind.METAL, 0xB9C0C9),
    ANTIMONY(51, "Sb", "Antimony", MaterialKind.METAL, 0x8F959C),
    TELLURIUM(52, "Te", "Tellurium", MaterialKind.NONMETAL_SOLID, 0x8A8D90),
    IODINE(53, "I", "Iodine", MaterialKind.NONMETAL_SOLID, 0x4B3A6A),
    XENON(54, "Xe", "Xenon", MaterialKind.GAS, 0xE2E8FF),
    CAESIUM(55, "Cs", "Caesium", MaterialKind.METAL, 0xE8D49A),
    BARIUM(56, "Ba", "Barium", MaterialKind.METAL, 0xC2C0B4),
    LANTHANUM(57, "La", "Lanthanum", MaterialKind.METAL, 0xC3C4C0),
    CERIUM(58, "Ce", "Cerium", MaterialKind.METAL, 0xBDB9AA),
    PRASEODYMIUM(59, "Pr", "Praseodymium", MaterialKind.METAL, 0xB8BAAC),
    NEODYMIUM(60, "Nd", "Neodymium", MaterialKind.METAL, 0xB9BBB6),
    PROMETHIUM(61, "Pm", "Promethium", MaterialKind.METAL, 0xB0B4AF),
    SAMARIUM(62, "Sm", "Samarium", MaterialKind.METAL, 0xC0BDB0),
    EUROPIUM(63, "Eu", "Europium", MaterialKind.METAL, 0xC5C2B0),
    GADOLINIUM(64, "Gd", "Gadolinium", MaterialKind.METAL, 0xBBBDBE),
    TERBIUM(65, "Tb", "Terbium", MaterialKind.METAL, 0xC0C1BE),
    DYSPROSIUM(66, "Dy", "Dysprosium", MaterialKind.METAL, 0xBEC0C1),
    HOLMIUM(67, "Ho", "Holmium", MaterialKind.METAL, 0xC2C3C1),
    ERBIUM(68, "Er", "Erbium", MaterialKind.METAL, 0xC0C1C3),
    THULIUM(69, "Tm", "Thulium", MaterialKind.METAL, 0xBFC2C3),
    YTTERBIUM(70, "Yb", "Ytterbium", MaterialKind.METAL, 0xC8C8BC),
    LUTETIUM(71, "Lu", "Lutetium", MaterialKind.METAL, 0xBDBFC2),
    HAFNIUM(72, "Hf", "Hafnium", MaterialKind.METAL, 0xA3A7AD),
    TANTALUM(73, "Ta", "Tantalum", MaterialKind.METAL, 0x8E97A4),
    TUNGSTEN(74, "W", "Tungsten", MaterialKind.METAL, 0x8C9094),
    RHENIUM(75, "Re", "Rhenium", MaterialKind.METAL, 0x9CA1A6),
    OSMIUM(76, "Os", "Osmium", MaterialKind.METAL, 0xAFC3D6),
    IRIDIUM(77, "Ir", "Iridium", MaterialKind.METAL, 0xC9CED4),
    PLATINUM(78, "Pt", "Platinum", MaterialKind.METAL, 0xD5D5CF),
    GOLD(79, "Au", "Gold", MaterialKind.METAL, 0xF2C33A),
    MERCURY(80, "Hg", "Mercury", MaterialKind.LIQUID, 0xC8CCD2),
    THALLIUM(81, "Tl", "Thallium", MaterialKind.METAL, 0x9FA4A8),
    LEAD(82, "Pb", "Lead", MaterialKind.METAL, 0x6F7788),
    BISMUTH(83, "Bi", "Bismuth", MaterialKind.METAL, 0xC7B6C8),
    POLONIUM(84, "Po", "Polonium", MaterialKind.METAL, 0xA8ACAE),
    ASTATINE(85, "At", "Astatine", MaterialKind.NONMETAL_SOLID, 0x4A4A4E),
    RADON(86, "Rn", "Radon", MaterialKind.GAS, 0xF0F0F0),
    FRANCIUM(87, "Fr", "Francium", MaterialKind.METAL, 0xC8C0A8),
    RADIUM(88, "Ra", "Radium", MaterialKind.METAL, 0xE0E2E6),
    ACTINIUM(89, "Ac", "Actinium", MaterialKind.METAL, 0xC6CAD0),
    THORIUM(90, "Th", "Thorium", MaterialKind.METAL, 0x8E8E86),
    PROTACTINIUM(91, "Pa", "Protactinium", MaterialKind.METAL, 0xA4A6A4),
    URANIUM(92, "U", "Uranium", MaterialKind.METAL, 0x7A7F86),
    NEPTUNIUM(93, "Np", "Neptunium", MaterialKind.METAL, 0x8A8C90),
    PLUTONIUM(94, "Pu", "Plutonium", MaterialKind.METAL, 0x8A8F8A),
    AMERICIUM(95, "Am", "Americium", MaterialKind.METAL, 0xC9CBCF),
    CURIUM(96, "Cm", "Curium", MaterialKind.METAL, 0xB9BBBE),
    BERKELIUM(97, "Bk", "Berkelium", MaterialKind.METAL, 0xB4B6B8),
    CALIFORNIUM(98, "Cf", "Californium", MaterialKind.METAL, 0xB0B2B4),
    EINSTEINIUM(99, "Es", "Einsteinium", MaterialKind.METAL, 0xB8BABC),
    FERMIUM(100, "Fm", "Fermium", MaterialKind.METAL, 0xB2B4B6),
    MENDELEVIUM(101, "Md", "Mendelevium", MaterialKind.METAL, 0xB0B2B4),
    NOBELIUM(102, "No", "Nobelium", MaterialKind.METAL, 0xAEB0B2),
    LAWRENCIUM(103, "Lr", "Lawrencium", MaterialKind.METAL, 0xACAEB0),
    RUTHERFORDIUM(104, "Rf", "Rutherfordium", MaterialKind.METAL, 0xA8AAAC),
    DUBNIUM(105, "Db", "Dubnium", MaterialKind.METAL, 0xA8AAAC),
    SEABORGIUM(106, "Sg", "Seaborgium", MaterialKind.METAL, 0xA8AAAC),
    BOHRIUM(107, "Bh", "Bohrium", MaterialKind.METAL, 0xA8AAAC),
    HASSIUM(108, "Hs", "Hassium", MaterialKind.METAL, 0xA8AAAC),
    MEITNERIUM(109, "Mt", "Meitnerium", MaterialKind.METAL, 0xA8AAAC),
    DARMSTADTIUM(110, "Ds", "Darmstadtium", MaterialKind.METAL, 0xA8AAAC),
    ROENTGENIUM(111, "Rg", "Roentgenium", MaterialKind.METAL, 0xA8AAAC),
    COPERNICIUM(112, "Cn", "Copernicium", MaterialKind.METAL, 0xA8AAAC),
    NIHONIUM(113, "Nh", "Nihonium", MaterialKind.METAL, 0xA8AAAC),
    FLEROVIUM(114, "Fl", "Flerovium", MaterialKind.METAL, 0xA8AAAC),
    MOSCOVIUM(115, "Mc", "Moscovium", MaterialKind.METAL, 0xA8AAAC),
    LIVERMORIUM(116, "Lv", "Livermorium", MaterialKind.METAL, 0xA8AAAC),
    TENNESSINE(117, "Ts", "Tennessine", MaterialKind.METAL, 0xA8AAAC),
    OGANESSON(118, "Og", "Oganesson", MaterialKind.NONMETAL_SOLID, 0x9A9CA0);

    private final int number;
    private final String symbol;
    private final MaterialKind kind;
    private final int colour;
    private final TextKey name;

    /* Every element's name is a material's, under the same keys any material's name is. */
    private static final String NAME_KEYS = "jscore.material.";

    Element(final int number, final String symbol, final String english, final MaterialKind kind, final int colour) {
        this.number = number;
        this.symbol = symbol;
        this.kind = kind;
        this.colour = colour;
        this.name = TextKey.of(NAME_KEYS + english.toLowerCase(Locale.ROOT), english);
    }

    /** The element of atomic number {@code number}, from 1 to 118, or none. */
    public static Optional<Element> byNumber(final int number) {
        for (final Element element : values()) {
            if (element.number == number) {
                return Optional.of(element);
            }
        }
        return Optional.empty();
    }

    /** Its atomic number: how many protons its nucleus holds. */
    public int number() {
        return this.number;
    }

    /** Its chemical symbol: Fe. */
    public String symbol() {
        return this.symbol;
    }

    /** What it is at room temperature. */
    public MaterialKind kind() {
        return this.kind;
    }

    /** The colour its things are drawn in, as {@code 0xRRGGBB}. */
    public int colour() {
        return this.colour;
    }

    /** What it is called. */
    public TextKey nameKey() {
        return this.name;
    }

    /** The path of its material's id: {@code iron} for {@code jscore:iron}. */
    public String path() {
        return this.name.key().substring(NAME_KEYS.length());
    }
}
