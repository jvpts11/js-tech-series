/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.connect.texture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.jstech.core.connect.ConnectedQuadrants;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CtmLogicTest {

    private static final int EVERY = 255;
    private static final float EPSILON = 1.0e-6F;
    /* A logic of two quarters, its own and its companion's, by whether the face is joined above. */
    private static final String HALVES = "{\"positions\": [{\"id\": \"TOP\", \"directions\": [\"up\"]},"
            + " {\"id\": \"TOP_RIGHT\", \"directions\": [\"up\", \"east\"]}],"
            + " \"submaps\": {\"\": {\"type\": \"grid\", \"width\": 2, \"height\": 1},"
            + " \"band\": {\"type\": \"pixel\", \"width\": 16, \"height\": 4, \"x_offset\": 0, \"y_offset\": 12}},"
            + " \"faces\": {\"\": {\"type\": \"grid\", \"width\": 1, \"height\": 2}},"
            + " \"rules\": [{\"output\": \"1,0\", \"from\": 1, \"at\": \"0,0\", \"connected\": [\"TOP\"]},"
            + " {\"output\": \"0,0\", \"at\": \"0,0\", \"unconnected\": [\"TOP\"]},"
            + " {\"output\": \"band\", \"at\": \"0,1\", \"connected\": [\"TOP_RIGHT\"]}]}";

    @Test
    void optifineFull_picksTheTileOfTheFullMethodFromItsSheet() {
        final CtmLogic logic = CtmLogic.optifineFull();
        for (int mask = 0; mask <= EVERY; mask++) {
            final List<CtmLogic.Pick> picks = logic.pick(mask);
            assertEquals(1, picks.size(), "one pick for the whole face, mask " + mask);
            final int tile = OptifineLayouts.full(mask);
            final CtmLogic.Region cell = picks.getFirst().cell();
            assertEquals((tile % 12) / 12.0F, cell.x(), EPSILON, "across, mask " + mask);
            assertEquals((tile / 12) / 4.0F, cell.y(), EPSILON, "down, mask " + mask);
            assertEquals(CtmLogic.Region.WHOLE, picks.getFirst().face());
        }
    }

    @Test
    void parse_readsPositionsCellsFacesAndRules() {
        final CtmLogic logic = CtmLogic.parse(json(HALVES));
        final List<CtmLogic.Pick> joined = logic.pick(ConnectedQuadrants.UP | ConnectedQuadrants.RIGHT
                | ConnectedQuadrants.UP_RIGHT);
        assertEquals(2, joined.size(), "both cells of the face");
        assertEquals(1, joined.getFirst().texture(), "the top half from the companion while joined above");
        assertEquals(0.5F, joined.getFirst().cell().x(), EPSILON);
        assertEquals(0.75F, joined.get(1).cell().y(), EPSILON, "the band four pixels from the bottom");
        assertEquals(0.5F, joined.get(1).face().y(), EPSILON, "on the face's lower half");
        final List<CtmLogic.Pick> alone = logic.pick(0);
        assertEquals(1, alone.size(), "the lower half has no rule that holds alone");
        assertEquals(0, alone.getFirst().texture());
    }

    @Test
    void parse_refusesANeighbourItDoesNotName() {
        assertThrows(IllegalArgumentException.class, () -> CtmLogic.parse(json("{\"positions\": [],"
                + " \"submaps\": {\"\": {\"type\": \"grid\", \"width\": 1, \"height\": 1}},"
                + " \"rules\": [{\"output\": \"0,0\", \"connected\": [\"TOP\"]}]}")));
        assertThrows(IllegalArgumentException.class, () -> CtmLogic.parse(json("{\"positions\": [],"
                + " \"submaps\": {\"\": {\"type\": \"spiral\"}}, \"rules\": []}")));
    }

    @Test
    void ctmTextures_readATypeByIdAsALogic() {
        final CtmTexture texture = CtmTextures.of(json("{\"type\": \"ctm:optifine_full\"}"));
        assertEquals(CtmTexture.Type.LOGIC, texture.type());
        assertEquals(Optional.of(CtmLogic.OPTIFINE_FULL), texture.logic());
        assertTrue(texture.type().joins());
    }

    private static JsonObject json(final String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }
}
