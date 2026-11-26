/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.cylinder;

import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;

/** What the Gas Cylinder says of its mark, and the list it is marked from. */
@TextHolder
public final class CylinderTexts {

    /* Under an empty cylinder marked for a gas. */
    public static final TextKey FILLS_ONLY = TextKey.of("jscore.gas_cylinder.fills_only",
            "Fills only with it. In a pattern it stands for the gas");
    public static final TextKey CLEAR = TextKey.of("jscore.gas_cylinder.clear", "Sneak and use in the air to clear it");
    /* The list an empty cylinder used in the air opens. */
    public static final TextKey MARK_TITLE = TextKey.of("jscore.gas_cylinder.mark.title", "Mark Gas Cylinder");
    public static final TextKey MARK_SEARCH = TextKey.of("jscore.gas_cylinder.mark.search", "Search gases");
    public static final TextKey MARK_EVERY = TextKey.of("jscore.gas_cylinder.mark.every",
            "Every gas of the Core, made yet or not");
    public static final TextKey MARK = TextKey.of("jscore.gas_cylinder.mark.mark", "Mark");
    public static final TextKey CANCEL = TextKey.of("jscore.gas_cylinder.mark.cancel", "Cancel");
    /* A gas in the viewers' lists: its formula, and what it is for. */
    public static final TextKey A_GAS = TextKey.of("jscore.gas_cylinder.a_gas", "%s, a gas");
    public static final TextKey JUST_A_GAS = TextKey.of("jscore.gas_cylinder.just_a_gas", "A gas");
    public static final TextKey DRAG = TextKey.of("jscore.gas_cylinder.drag", "Drag it into a pattern's slot");

    private CylinderTexts() {
    }

    /** A gas's formula and what it is, as a viewer's tooltip says it under its name: "HCl, a gas". */
    public static Text aGas(final Material gas) {
        return gas.value(MaterialProperties.FORMULA).or(() -> gas.value(MaterialProperties.SYMBOL))
                .map(formula -> A_GAS.with(Text.literal(formula))).orElseGet(JUST_A_GAS::text);
    }
}
