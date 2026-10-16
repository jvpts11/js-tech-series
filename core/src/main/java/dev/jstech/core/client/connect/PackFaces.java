/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.connect;

import dev.jstech.core.connect.texture.CtmLayouts;
import dev.jstech.core.connect.texture.CtmLogic;
import dev.jstech.core.connect.texture.CtmTexture;
import dev.jstech.core.connect.texture.OptifineLayouts;
import dev.jstech.core.connect.texture.OptifineRule;
import dev.jstech.core.connect.texture.TilePicks;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a face of a block model as the packs' connected textures say, from what stands around its block: CTM's
 * texture of the face if it has one, else every OptiFine or Continuity overlay laid over it and the first of their
 * rules that draws it. A face no rule draws stays as it was.
 */
final class PackFaces {

    private static final int QUARTERS = 4;
    private static final float HALF = 0.5F;
    private static final int NO_LIGHT = -1;
    /* How far the middle of an eldritch face is moved at most, a share of its texture each way. */
    private static final float WARP = 0.06F;

    private PackFaces() {
    }

    /**
     * What {@code quad} of {@code state}'s model is drawn as, among the blocks {@code around}, its model's overrides
     * by tint index {@code overrides}: each quad with its layer, none for the face's own.
     */
    static List<Drawn> draw(final BakedQuad quad, final BlockState state, final PackSurroundings around,
                            final Map<Integer, CtmTexture> overrides) {
        final PackCandidates.Plan plan = PackCandidates.of(quad);
        if (plan == null) {
            return List.of(new Drawn(quad, null));
        }
        final Optional<PackCandidates.Ctm> ctm = PackCandidates.ctm(plan.sprite(),
                Optional.ofNullable(overrides.get(quad.getTintIndex())));
        if (ctm.isPresent()) {
            return ctm(plan.pieces(), ctm.get(), state, around);
        }
        final List<PackCandidates.Rule> rules = new ArrayList<>(plan.byTile());
        rules.addAll(PackCandidates.ofBlock(state.getBlock()));
        final List<Drawn> out = new ArrayList<>();
        boolean drawn = false;
        for (final PackCandidates.Rule rule : rules) {
            if (!applies(rule.rule(), state, around, quad.getDirection())) {
                continue;
            }
            if (rule.rule().method().overlays()) {
                overlay(plan.pieces(), rule, state, around, plan.sprite(), out);
            } else if (!drawn) {
                final List<BakedQuad> faces = optifine(plan.pieces(), rule, state, around, plan.sprite());
                if (faces != null) {
                    faces.forEach(face -> out.add(new Drawn(face, null)));
                    drawn = true;
                }
            }
        }
        if (!drawn) {
            out.addFirst(new Drawn(quad, null));
        }
        return out;
    }

    /* Whether the rule draws this face of this block, here. */
    private static boolean applies(final OptifineRule rule, final BlockState state, final PackSurroundings around,
                                   final Direction face) {
        if (!rule.onFace(face.get3DDataValue()) || !rule.where().atHeight(around.pos().getY())
                || !rule.where().inBiome(around.biome())) {
            return false;
        }
        if (!rule.name().isEmpty() && !PackNames.matches(rule.name(), around)) {
            return false;
        }
        return rule.byTiles() || rule.matchBlocks().stream().anyMatch(filter -> PackJoins.matches(filter, state));
    }

    /* The face drawn by an OptiFine or Continuity rule that draws in its place; none where the rule leaves it. */
    private static @Nullable List<BakedQuad> optifine(final QuadPieces pieces, final PackCandidates.Rule rule,
                                                      final BlockState state, final PackSurroundings around,
                                                      final String sprite) {
        final OptifineRule method = rule.rule();
        final Direction face = pieces.quad().getDirection();
        final TextureAtlasSprite own = pieces.quad().getSprite();
        if (method.method() == OptifineRule.Method.COMPACT) {
            final int mask = mask(around, pieces, face, PackJoins.optifine(method, state, sprite), method);
            final Integer whole = method.overrides().get(OptifineLayouts.full(mask));
            if (whole != null) {
                return whole(pieces, rule.tile(whole, own));
            }
            final int[] tiles = OptifineLayouts.compact(mask);
            final List<BakedQuad> out = new ArrayList<>(QUARTERS);
            for (int q = 0; q < QUARTERS; q++) {
                final TextureAtlasSprite tile = rule.tile(tiles[q], own);
                if (tile != null) {
                    final BakedQuad piece = pieces.quarter(q, tile, quarterOf(q), NO_LIGHT);
                    if (piece != null) {
                        out.add(piece);
                    }
                }
            }
            return out;
        }
        final int tile = tileOf(method, pieces, face, state, around, sprite);
        return tile < 0 ? null : whole(pieces, rule.tile(tile, own));
    }

    /* The tile a rule that draws a face whole picks; below 0 where it leaves the face as it was. */
    private static int tileOf(final OptifineRule rule, final QuadPieces pieces, final Direction face,
                              final BlockState state, final PackSurroundings around, final String sprite) {
        final PackJoins.IJoin joins = PackJoins.optifine(rule, state, sprite);
        return switch (rule.method()) {
            case CTM, OVERLAY_CTM -> OptifineLayouts.full(mask(around, pieces, face, joins, rule));
            case HORIZONTAL -> OptifineLayouts.horizontal(mask(around, pieces, face, joins, rule));
            case VERTICAL -> OptifineLayouts.vertical(mask(around, pieces, face, joins, rule));
            case HORIZONTAL_VERTICAL -> OptifineLayouts.horizontalVertical(mask(around, pieces, face, joins, rule));
            case VERTICAL_HORIZONTAL -> OptifineLayouts.verticalHorizontal(mask(around, pieces, face, joins, rule));
            case TOP -> face.getAxis() != Direction.Axis.Y && joins.joins(around.at(Direction.UP),
                    around.extraAt(Direction.UP), face) ? 0 : -1;
            case RANDOM, OVERLAY_RANDOM -> random(rule, face, state, around);
            case REPEAT, OVERLAY_REPEAT -> repeat(rule, pieces, around);
            case FIXED, OVERLAY_FIXED -> 0;
            case COMPACT, OVERLAY -> -1;
        };
    }

    /* The overlays a rule lays over the face, each in the rule's layer, tinted as it says. */
    private static void overlay(final QuadPieces pieces, final PackCandidates.Rule rule, final BlockState state,
                                final PackSurroundings around, final String sprite, final List<Drawn> out) {
        final OptifineRule method = rule.rule();
        final Direction face = pieces.quad().getDirection();
        final int[] tiles;
        if (method.method() == OptifineRule.Method.OVERLAY) {
            tiles = OptifineLayouts.overlay(sources(around, pieces, face, PackJoins.overlaySource(method, state,
                    sprite)));
        } else {
            final int tile = tileOf(method, pieces, face, state, around, sprite);
            tiles = tile < 0 ? new int[0] : new int[] {tile};
        }
        final RenderType layer = "translucent".equals(method.overlay().layer()) ? RenderType.translucent()
                : RenderType.cutout();
        for (final int tile : tiles) {
            final TextureAtlasSprite drawn = rule.tile(tile, pieces.quad().getSprite());
            if (drawn != null) {
                out.add(new Drawn(PackTints.tinted(pieces.whole(drawn, QuadPieces.Cell.WHOLE, 0, NO_LIGHT),
                        method.overlay(), around), layer));
            }
        }
    }

    /* The face drawn by CTM's texture. */
    private static List<Drawn> ctm(final QuadPieces pieces, final PackCandidates.Ctm ctm, final BlockState state,
                                   final PackSurroundings around) {
        final CtmTexture texture = ctm.texture();
        final RenderType layer = texture.layer().map(PackFaces::layerOf).orElse(null);
        final int light = texture.light().map(glow -> QuadPieces.light(glow.block(), glow.sky())).orElse(NO_LIGHT);
        final List<BakedQuad> quads = ctmQuads(pieces, ctm, state, around, light);
        final List<Drawn> out = new ArrayList<>(quads.size());
        quads.forEach(quad -> out.add(new Drawn(quad, layer)));
        return out;
    }

    private static List<BakedQuad> ctmQuads(final QuadPieces pieces, final PackCandidates.Ctm ctm,
                                            final BlockState state, final PackSurroundings around, final int light) {
        final CtmTexture texture = ctm.texture();
        final Direction face = pieces.quad().getDirection();
        final PackJoins.IJoin joins = PackJoins.ctm(texture, state);
        final boolean seams = !texture.joinsInside();
        return switch (texture.type()) {
            case NORMAL -> List.of(pieces.whole(ctm.own(), QuadPieces.Cell.WHOLE, 0, light));
            case CTM -> quarters(pieces, ctm, PackJoins.mask(around, pieces, face, joins, seams), false, light);
            case HORIZONTAL -> List.of(cell(pieces, ctm, CtmLayouts.horizontal(PackJoins.mask(around, pieces, face,
                    joins, seams)), 0, light));
            case VERTICAL -> List.of(cell(pieces, ctm, CtmLayouts.vertical(PackJoins.mask(around, pieces, face,
                    joins, seams)), 0, light));
            case SIMPLE -> List.of(cell(pieces, ctm, CtmLayouts.simple(PackJoins.mask(around, pieces, face, joins,
                    seams)), 0, light));
            case PILLAR -> List.of(PackPillars.draw(pieces, ctm, joins, around, light));
            case RANDOM -> List.of(grid(pieces, ctm, TilePicks.any(TilePicks.seed(around.pos().getX(),
                    around.pos().getY(), around.pos().getZ(), face.get3DDataValue(), TilePicks.EACH_FACE, 0),
                    texture.grid().width() * texture.grid().height()), light));
            case PATTERN -> List.of(grid(pieces, ctm, TilePicks.pattern(across(pieces, around), down(pieces, around),
                    texture.grid().width(), texture.grid().height(), texture.grid().offsetX(),
                    texture.grid().offsetY()), light));
            case EDGES -> edges(pieces, ctm, joins, around, light);
            case EDGES_FULL -> {
                final boolean covered = joins.joins(around.at(face), around.extraAt(face), face);
                yield List.of(cell(pieces, ctm, CtmLayouts.edgesFull(changes(around, pieces, face, joins),
                        covered), 0, light));
            }
            case ELDRITCH -> eldritch(pieces, ctm.own(), around, light);
            case LOGIC -> logic(pieces, ctm, PackJoins.mask(around, pieces, face, joins, seams), light);
        };
    }

    /* A logic written as data: each cell of the face from the cell its first rule that holds gives. */
    private static List<BakedQuad> logic(final QuadPieces pieces, final PackCandidates.Ctm ctm, final int mask,
                                         final int light) {
        final CtmLogic logic = PackRules.current().logics().get(ctm.texture().logic().orElse(""));
        if (logic == null) {
            return List.of(pieces.whole(ctm.own(), QuadPieces.Cell.WHOLE, 0, light));
        }
        final List<BakedQuad> out = new ArrayList<>();
        for (final CtmLogic.Pick pick : logic.pick(mask)) {
            final CtmLogic.Region face = pick.face();
            final CtmLogic.Region cell = pick.cell();
            final BakedQuad piece = pieces.region(face.x(), face.y(), face.x() + face.width(),
                    face.y() + face.height(), pick.texture() == 0 ? ctm.own() : ctm.companion(pick.texture() - 1),
                    new QuadPieces.Cell(cell.x(), cell.y(), cell.width(), cell.height()), light);
            if (piece != null) {
                out.add(piece);
            }
        }
        return out.isEmpty() ? List.of(pieces.whole(ctm.own(), QuadPieces.Cell.WHOLE, 0, light)) : out;
    }

    /* CTM's edges type: wholly its second companion where a block that changes it covers the face. */
    private static List<BakedQuad> edges(final QuadPieces pieces, final PackCandidates.Ctm ctm,
                                         final PackJoins.IJoin joins, final PackSurroundings around,
                                         final int light) {
        final Direction face = pieces.quad().getDirection();
        if (joins.joins(around.at(face), around.extraAt(face), face)) {
            return List.of(pieces.whole(ctm.companion(1), QuadPieces.Cell.WHOLE, 0, light));
        }
        return quarters(pieces, ctm, changes(around, pieces, face, joins), true, light);
    }

    /* The neighbours that change a face of the edges types: those that join, there or in front of themselves. */
    private static int changes(final PackSurroundings around, final QuadPieces pieces, final Direction face,
                               final PackJoins.IJoin joins) {
        return PackJoins.mask(around, pieces, face, joins, false) | inFront(around, pieces, face, joins);
    }

    private static int inFront(final PackSurroundings around, final QuadPieces pieces, final Direction face,
                               final PackJoins.IJoin joins) {
        final Direction up = pieces.up();
        final Direction right = pieces.right();
        final Direction[][] ways = {{up, face}, {up, right, face}, {right, face}, {up.getOpposite(), right, face},
            {up.getOpposite(), face}, {up.getOpposite(), right.getOpposite(), face}, {right.getOpposite(), face},
            {up, right.getOpposite(), face}};
        int mask = 0;
        for (int i = 0; i < ways.length; i++) {
            if (joins.joins(around.at(ways[i]), around.extraAt(ways[i]), face)) {
                mask |= 1 << i;
            }
        }
        return mask;
    }

    private static List<BakedQuad> quarters(final QuadPieces pieces, final PackCandidates.Ctm ctm, final int mask,
                                            final boolean edges, final int light) {
        final List<BakedQuad> out = new ArrayList<>(QUARTERS);
        for (int q = 0; q < QUARTERS; q++) {
            final CtmLayouts.Cell cell = edges ? CtmLayouts.edgesQuarter(q, mask) : CtmLayouts.quarter(q, mask);
            final BakedQuad piece = pieces.quarter(q, cell.texture() == CtmLayouts.OWN ? ctm.own()
                    : ctm.companion(0), cellOf(cell), light);
            if (piece != null) {
                out.add(piece);
            }
        }
        return out;
    }

    private static BakedQuad cell(final QuadPieces pieces, final PackCandidates.Ctm ctm, final CtmLayouts.Cell cell,
                                  final int turns, final int light) {
        return pieces.whole(cell.texture() == CtmLayouts.OWN ? ctm.own() : ctm.companion(0), cellOf(cell), turns,
                light);
    }

    private static BakedQuad grid(final QuadPieces pieces, final PackCandidates.Ctm ctm, final int tile,
                                  final int light) {
        final CtmTexture.Grid grid = ctm.texture().grid();
        return pieces.whole(ctm.own(), new QuadPieces.Cell((float) (tile % grid.width()) / grid.width(),
                (float) (tile / grid.width()) / grid.height(), 1.0F / grid.width(), 1.0F / grid.height()), 0, light);
    }

    /* The eldritch type: its four quarters, their meeting point moved a little by the place. */
    private static List<BakedQuad> eldritch(final QuadPieces pieces, final TextureAtlasSprite own,
                                            final PackSurroundings around, final int light) {
        final long seed = TilePicks.seed(around.pos().getX(), around.pos().getY(), around.pos().getZ(),
                pieces.quad().getDirection().get3DDataValue(), TilePicks.EACH_FACE, 1);
        final float du = ((seed & 0xFF) / 255.0F - HALF) * 2 * WARP;
        final float dv = (((seed >>> 8) & 0xFF) / 255.0F - HALF) * 2 * WARP;
        final List<BakedQuad> out = new ArrayList<>(QUARTERS);
        for (int q = 0; q < QUARTERS; q++) {
            final BakedQuad piece = pieces.warpedQuarter(q, own, du, dv, light);
            if (piece != null) {
                out.add(piece);
            }
        }
        return out;
    }

    private static List<BakedQuad> whole(final QuadPieces pieces, @Nullable final TextureAtlasSprite tile) {
        return tile == null ? List.of() : List.of(pieces.whole(tile, QuadPieces.Cell.WHOLE, 0, NO_LIGHT));
    }

    private static int mask(final PackSurroundings around, final QuadPieces pieces, final Direction face,
                            final PackJoins.IJoin joins, final OptifineRule rule) {
        return PackJoins.mask(around, pieces, face, joins, rule.innerSeams());
    }

    /* The neighbours an overlay is laid for: those it names, whose own face in front is not covered. */
    private static int sources(final PackSurroundings around, final QuadPieces pieces, final Direction face,
                               final PackJoins.IJoin joins) {
        return PackJoins.mask(around, pieces, face, joins, false) & ~inFrontCovered(around, pieces, face);
    }

    private static int inFrontCovered(final PackSurroundings around, final QuadPieces pieces, final Direction face) {
        return inFront(around, pieces, face, (other, extra, side) -> other.canOcclude());
    }

    private static int random(final OptifineRule rule, final Direction face, final BlockState state,
                              final PackSurroundings around) {
        int y = around.pos().getY();
        if (rule.random().linked() && around.at(Direction.DOWN).getBlock() == state.getBlock()) {
            // The upper half of a block two high picks as its lower half does.
            y--;
        }
        final long seed = TilePicks.seed(around.pos().getX(), y, around.pos().getZ(), face.get3DDataValue(),
                rule.random().symmetry(), rule.random().loops());
        return TilePicks.weighted(seed, rule.random().weightsFor(rule.tiles().size()));
    }

    private static int repeat(final OptifineRule rule, final QuadPieces pieces, final PackSurroundings around) {
        return TilePicks.pattern(across(pieces, around), down(pieces, around), rule.random().width(),
                rule.random().height(), 0, 0);
    }

    /* How many blocks right the block is, along its face's texture. */
    private static int across(final QuadPieces pieces, final PackSurroundings around) {
        final Direction right = pieces.right();
        return around.pos().getX() * right.getStepX() + around.pos().getY() * right.getStepY()
                + around.pos().getZ() * right.getStepZ();
    }

    /* How many blocks down it is, along its face's texture. */
    private static int down(final QuadPieces pieces, final PackSurroundings around) {
        final Direction up = pieces.up();
        return -(around.pos().getX() * up.getStepX() + around.pos().getY() * up.getStepY()
                + around.pos().getZ() * up.getStepZ());
    }

    private static QuadPieces.Cell cellOf(final CtmLayouts.Cell cell) {
        return new QuadPieces.Cell(cell.u0(), cell.v0(), cell.width(), cell.height());
    }

    /* The quarter {@code quarter} of a whole tile. */
    private static QuadPieces.Cell quarterOf(final int quarter) {
        return new QuadPieces.Cell(quarter == 1 || quarter == 2 ? HALF : 0, quarter >= 2 ? HALF : 0, HALF, HALF);
    }

    /** The layer CTM names: {@code SOLID}, {@code CUTOUT}, {@code CUTOUT_MIPPED} or {@code TRANSLUCENT}. */
    static RenderType layerOf(final String layer) {
        return switch (layer) {
            case "SOLID" -> RenderType.solid();
            case "CUTOUT_MIPPED" -> RenderType.cutoutMipped();
            case "TRANSLUCENT" -> RenderType.translucent();
            default -> RenderType.cutout();
        };
    }

    /**
     * A quad a face is drawn as, and its layer.
     *
     * @param quad  the quad
     * @param layer the layer it is drawn in; none for its model's own
     */
    record Drawn(BakedQuad quad, @Nullable RenderType layer) {
    }
}
