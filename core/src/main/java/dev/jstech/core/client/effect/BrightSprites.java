/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.JsCore;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;
import org.slf4j.Logger;

/**
 * Pictures of the particle sheet made from others as the sheet is made: each frame of a picture the resource packs
 * hold, brightened towards white by a share, keeping its shape and its alpha; so the game's own smoke, as the packs
 * draw it, becomes the white of steam, with no picture of its own. The Core's resources name it in
 * {@code atlases/particles.json}.
 *
 * <pre>{@code
 * { "type": "jscore:brightened", "from": "minecraft:big_smoke", "frames": 12, "to": "jscore:steam", "share": 0.75 }
 * }</pre>
 *
 * @param from   the picture's frames' name, {@code _0} and on after it, under {@code textures/particle/}
 * @param frames how many frames
 * @param to     the new frames' name
 * @param share  how far towards white each pixel goes, 0 none, 1 all
 */
record BrightSprites(ResourceLocation from, int frames, ResourceLocation to, double share) implements SpriteSource {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MOST_FRAMES = 64;
    private static final int CHANNEL = 0xFF;
    private static final int ALPHA_MASK = 0xFF << 24;
    private static final String PARTICLES = "textures/particle/";
    private static final String PNG = ".png";
    static final MapCodec<BrightSprites> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("from").forGetter(BrightSprites::from),
            Codec.intRange(1, MOST_FRAMES).fieldOf("frames").forGetter(BrightSprites::frames),
            ResourceLocation.CODEC.fieldOf("to").forGetter(BrightSprites::to),
            Codec.doubleRange(0, 1).optionalFieldOf("share", 0.75).forGetter(BrightSprites::share))
            .apply(instance, BrightSprites::new));
    static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "brightened");

    /* Its type, once the game made it. */
    private static volatile SpriteSourceType type;

    /** Registers its type, from the event the game asks for sources' types in. */
    static void register(final RegisterSpriteSourceTypesEvent event) {
        type = event.register(ID, CODEC);
    }

    @Override
    public void run(final ResourceManager resources, final Output output) {
        for (int frame = 0; frame < this.frames; frame++) {
            final int at = frame;
            final ResourceLocation made = this.to.withSuffix("_" + frame);
            final Optional<Resource> found = resources.getResource(this.from.withPath(path -> PARTICLES + path + "_"
                    + at + PNG));
            if (found.isPresent()) {
                output.add(made, loader -> brightened(made, found.get()));
            } else {
                LOGGER.warn("No picture {}_{} to brighten into {}", this.from, at, made);
            }
        }
    }

    @Override
    public SpriteSourceType type() {
        return type;
    }

    private SpriteContents brightened(final ResourceLocation made, final Resource resource) {
        try (InputStream in = resource.open()) {
            final NativeImage image = NativeImage.read(in);
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    final int abgr = image.getPixelRGBA(x, y);
                    int out = abgr & ALPHA_MASK;
                    for (int shift = 0; shift <= 16; shift += 8) {
                        final int channel = abgr >> shift & CHANNEL;
                        out |= (int) Math.round(channel + (CHANNEL - channel) * this.share) << shift;
                    }
                    image.setPixelRGBA(x, y, out);
                }
            }
            return new SpriteContents(made, new FrameSize(image.getWidth(), image.getHeight()), image,
                    ResourceMetadata.EMPTY);
        } catch (final IOException broken) {
            LOGGER.warn("The picture {} could not be brightened: {}", made, broken.getMessage());
            return null;
        }
    }
}
