/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.core;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.JsCore;
import dev.jstech.core.dimension.Wind;
import dev.jstech.core.dimension.Winds;
import dev.jstech.core.effect.EffectDefinition;
import dev.jstech.core.effect.EffectPayload;
import dev.jstech.core.effect.Effects;
import dev.jstech.core.effect.Emitter;
import dev.jstech.core.energy.EnergyFaults;
import dev.jstech.tests.JsTests;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The effects kit as the server sees it: an effect is a file of emitters that reads, with its anchor and defaults; what
 * the server fires reaches the players small and whole, aimed, coloured, scaled, following or on a block; the wind of
 * a world with air blows a few metres a second; and the effects editor's commands are there.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class EffectsGameTests {

    private static final String ARENA = "empty";
    private static final double CLOSE = 1e-6;
    private static final String STEAM = """
            { "anchor": {"face": "up"}, "reach": 48, "emitters": [ { "name": "steam",
              "emission": {"shape": {"type": "disc", "radius": 0.4}, "rate": 40, "life": [3, 5], "speed": 1.2,
                           "bursts": [{"at": 0.5, "count": 10}], "children": ["drops"]},
              "look": {"sprite": "minecraft:big_smoke", "frames": 12, "size": [0.4, 2],
                       "colour": ["#F0F4F8E0", "#F0F4F800"], "blend": "additive", "trail": 0.3},
              "motion": {"buoyancy": 1.6, "drag": 1.2, "expands": true, "medium": "air",
                         "moved_by": {"wind": false}} },
              { "name": "drops", "kind": "particles" },
              { "name": "zap", "kind": "arc", "extra": {"branches": 3, "jagged": 0.3} } ] }
            """;

    private EffectsGameTests() {
    }

    @GameTest(template = ARENA)
    public static void definition_readsEveryPartOfItsFile(final GameTestHelper helper) {
        final EffectDefinition read = EffectDefinition.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString(STEAM)).getOrThrow();
        helper.assertTrue(read.reach() == 48 && read.emitters().size() == 3, "its reach and three emitters");
        helper.assertTrue(read.anchor().flatMap(EffectDefinition.Anchor::face).orElse(null) == Direction.UP,
                "anchored on the top face");
        final Emitter steam = read.emitters().getFirst();
        helper.assertTrue(steam.emission().rate() == 40 && steam.emission().life().min() == 3
                && steam.emission().life().max() == 5 && steam.emission().speed().min() == 1.2,
                "its rate, life and speed: " + steam.emission());
        helper.assertTrue(steam.emission().bursts().getFirst().count() == 10, "its burst");
        helper.assertTrue(steam.emission().children().equals(List.of("drops")), "what its particles carry");
        helper.assertTrue(steam.look().colour().getFirst() == (0xE0 << 24 | 0xF0F4F8), "its colour, alpha last");
        helper.assertTrue(steam.look().blend() == Emitter.Blend.ADDITIVE && steam.look().frames() == 12,
                "its blend and frames");
        helper.assertTrue(steam.motion().expands() && !steam.motion().moves().wind() && steam.motion().moves().air(),
                "it widens, and the wind leaves it alone");
        helper.assertTrue(read.emitters().get(2).kind() == Emitter.Kind.ARC
                && read.emitters().get(2).extra().branches() == 3, "an arc and its branches");
        final EffectDefinition again = EffectDefinition.CODEC.parse(JsonOps.INSTANCE, EffectDefinition.CODEC
                .encodeStart(JsonOps.INSTANCE, read).getOrThrow()).getOrThrow();
        helper.assertTrue(again.equals(read), "written and read again, it is the same");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void payload_arrivesWhole(final GameTestHelper helper) {
        final ResourceLocation laser = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "laser");
        final Vec3 from = new Vec3(10.5, 64.25, -3.75);
        final EffectPayload sent = Effects.at(helper.getLevel(), laser, from).facing(new Vec3(0, 0, 2))
                .to(new Vec3(20, 70, -3)).colour(0xFF << 24 | 0x20FF20).scale(2.5F).payload();
        final EffectPayload read = roundTrip(helper, sent);
        helper.assertTrue(read.effect().equals(laser) && read.at().equals(from), "which and where");
        helper.assertTrue(Math.abs(read.way().z() - 1) < CLOSE, "facing south, made a unit");
        helper.assertTrue(read.to().equals(Optional.of(new Vec3(20, 70, -3))), "aimed");
        helper.assertTrue(read.colour() == (0xFF << 24 | 0x20FF20) && read.scale() == 2.5F && read.follow() < 0
                && !read.block(), "coloured and scaled, following nothing");
        final EffectPayload plain = roundTrip(helper, Effects.onBlock(helper.getLevel(),
                EnergyFaults.ARC_FLASH_EFFECT, new BlockPos(1, 2, 3)).payload());
        helper.assertTrue(plain.block() && plain.colour() == 0 && plain.scale() == 1.0F && plain.to().isEmpty(),
                "played on a block, the rest plain");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void wind_blowsAFewMetresASecondInTheOverworld(final GameTestHelper helper) {
        final Wind wind = Winds.at(helper.getLevel(), Vec3.atCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))));
        helper.assertTrue(wind.speed() > 0.5 && wind.speed() < 15, "a breeze: " + wind.speed() + " m/s");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void commands_ofTheEffectsEditorAreThere(final GameTestHelper helper) {
        for (final String tool : List.of("edit", "new", "play")) {
            helper.assertTrue(helper.getLevel().getServer().getCommands().getDispatcher().findNode(List.of("jstech",
                    "effect", tool)) != null, "/jstech effect " + tool);
        }
        helper.succeed();
    }

    private static EffectPayload roundTrip(final GameTestHelper helper, final EffectPayload sent) {
        final RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        EffectPayload.STREAM_CODEC.encode(buffer, sent);
        return EffectPayload.STREAM_CODEC.decode(buffer);
    }
}
