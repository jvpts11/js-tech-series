/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.audio.MusicImports;
import dev.jstech.computers.audio.MusicPlayer;
import dev.jstech.computers.audio.SongFiles;
import dev.jstech.computers.audio.SoundfoundryCovers;
import dev.jstech.computers.audio.SoundfoundryPlaylists;
import dev.jstech.computers.audio.SoundfoundryServers;
import dev.jstech.computers.audio.SoundfoundryTexts;
import dev.jstech.computers.audio.catalog.SoundfoundryCatalog;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.blockentity.ServerServices;
import dev.jstech.computers.operation.payload.SoundfoundryActionPayload;
import dev.jstech.computers.operation.payload.SoundfoundryPagePayload;
import dev.jstech.computers.operation.payload.music.SoundfoundryPagePayloads;
import dev.jstech.computers.os.IOsHost;
import dev.jstech.computers.os.fs.FsPaths;
import dev.jstech.computers.program.Programs;
import dev.jstech.computers.program.SongDownload;
import dev.jstech.computers.program.SongRefs;
import dev.jstech.computers.program.SoundfoundryListeners;
import dev.jstech.core.audio.media.MediaId;
import dev.jstech.core.audio.media.MediaInfo;
import dev.jstech.core.audio.media.MediaSessions;
import dev.jstech.core.audio.media.MediaStore;
import dev.jstech.core.text.Text;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.testkit.TestMedia;
import dev.jstech.tests.testkit.TestWorldBuilder;
import io.netty.buffer.Unpooled;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The Soundfoundry Server, and a Standard Soundfoundry streaming from it: the network finds the servers running it,
 * each server's library is its own music folder, a computer playing from it is a listener that costs the server
 * memory and is turned away when there is none left, songs go up to it and come down from it at the speed of the
 * slowest cable, and the pages of the Standard Soundfoundry list the catalogue only through a server.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SoundfoundryServerGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos COMPUTER = new BlockPos(5, 2, 2);
    private static final BlockPos MONITOR = new BlockPos(6, 2, 2);
    private static final String SERVER_NAME = "sf-server-01";
    private static final String COMPUTER_NAME = "attic-pc";
    /** The album of the test mod's own catalogue, which has a cover beside its songs. */
    private static final String TEST_ALBUM = "jstests/test_tones";

    private SoundfoundryServerGameTests() {
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void server_isFoundOnTheNetworkAndItsLibraryIsItsMusicFolder(final GameTestHelper helper) {
        final Base base = wire(helper, true);
        final ServerLevel level = helper.getLevel();
        linked(helper, base)
                .thenExecute(() -> {
                    final List<ServerServices.Host> found = SoundfoundryServers.of(level, base.pc());
                    helper.assertTrue(found.size() == 1, "the network finds the one server running it; found "
                            + found.size());
                    final ServerServices.Host host = found.getFirst();
                    final String path = keep(helper, host.machine(), "", "Anvil Song.wav", 300);
                    helper.assertTrue(SoundfoundryServers.library(level, host).contains(path),
                            "a song in the server's music folder is in the network's library");
                    helper.assertTrue(SoundfoundryServers.fromOf(host, path).equals(host.hostname()),
                            "and one put there by other means is the server's own; from "
                                    + SoundfoundryServers.fromOf(host, path));
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void stream_playsFromTheServerAsAListenerThatCostsItMemory(final GameTestHelper helper) {
        final Base base = wire(helper, true);
        final ServerLevel level = helper.getLevel();
        linked(helper, base)
                .thenExecute(() -> {
                    final ServerServices.Host host = SoundfoundryServers.chosen(level, base.pc());
                    final String path = keep(helper, host.machine(), "", "Tin Roof.wav", 9000);
                    final int before = host.machine().ramLedger().usedMb();
                    play(helper, base.pc(), SongRefs.network(path));
                    final MusicPlayer music = base.pc().musicPlayer();
                    helper.assertTrue(music.playing(level) && music.streaming(level),
                            "the song streams from the server; trouble " + music.trouble().english());
                    helper.assertTrue(SoundfoundryServers.listeners(level, host) == 1,
                            "the computer is a listener of the server");
                    helper.assertTrue(host.machine().ramLedger().usedMb()
                                    == before + SoundfoundryListeners.RAM_PER_LISTENER_MB,
                            "whose stream the server holds in memory; used " + host.machine().ramLedger().usedMb()
                                    + " after " + before);
                    music.stop(level);
                    helper.assertTrue(SoundfoundryServers.listeners(level, host) == 0,
                            "and stopping lets the stream go");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void stream_isTurnedAwayByAServerWithNoMemoryLeft(final GameTestHelper helper) {
        final Base base = wire(helper, true);
        final ServerLevel level = helper.getLevel();
        linked(helper, base)
                .thenExecute(() -> {
                    final ServerServices.Host host = SoundfoundryServers.chosen(level, base.pc());
                    final String path = keep(helper, host.machine(), "", "Slow Furnace.wav", 9000);
                    final SoundfoundryListeners listeners = base.rack().listenersAt(0);
                    final int others = host.machine().ramLedger().freeMb() / SoundfoundryListeners.RAM_PER_LISTENER_MB
                            + 1;
                    for (int i = 0; i < others; i++) {
                        listeners.listen(-1L - i, level.getGameTime());
                    }
                    play(helper, base.pc(), SongRefs.network(path));
                    final MusicPlayer music = base.pc().musicPlayer();
                    helper.assertTrue(!music.playing(level) && music.trouble().equals(
                                    SoundfoundryTexts.SERVER_FULL.with(host.hostname())),
                            "a server with no memory left streams to no one more; said "
                                    + music.trouble().english());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void stream_stopsWhenTheServerGoesAway(final GameTestHelper helper) {
        final Base base = wire(helper, true);
        final ServerLevel level = helper.getLevel();
        final String[] name = {""};
        linked(helper, base)
                .thenExecute(() -> {
                    final ServerServices.Host host = SoundfoundryServers.chosen(level, base.pc());
                    name[0] = host.hostname();
                    final String path = keep(helper, host.machine(), "", "3 AM Backup.wav", 9000);
                    play(helper, base.pc(), SongRefs.network(path));
                    helper.assertTrue(base.pc().musicPlayer().streaming(level), "the song streams");
                    base.rack().toggleBayPower(0);
                })
                .thenWaitUntil(() -> helper.assertTrue(!MediaSessions.has(level, base.pc().musicPlayer().key())
                                && base.pc().musicPlayer().trouble()
                                .equals(SoundfoundryTexts.SERVER_GONE.with(name[0])),
                        "a server switched off stops the song, which says why"))
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void upload_sendsASongToTheLibraryInAFolderNamedAfterTheComputer(final GameTestHelper helper) {
        final Base base = wire(helper, true);
        final ServerLevel level = helper.getLevel();
        final String[] local = {""};
        linked(helper, base)
                .thenExecute(() -> {
                    local[0] = keep(helper, base.pc(), "", "Copper Rain.wav", 300);
                    final Text said = base.pc().musicDownloads().upload(level, local[0]);
                    helper.assertTrue(said.isEmpty(), "the song is on its way up; said " + said.english());
                    helper.assertTrue(downloads(base.pc()).getFirst().upload(), "as an upload");
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        downloads(base.pc()).getFirst().status() == SongDownload.Status.DONE, "it goes up whole"))
                .thenExecute(() -> {
                    final ServerServices.Host host = SoundfoundryServers.chosen(level, base.pc());
                    final String kept = FsPaths.join(SoundfoundryServers.folderFor(host, COMPUTER_NAME),
                            "Copper Rain.wav");
                    helper.assertTrue(SoundfoundryServers.library(level, host).contains(kept),
                            "it is in the library, in a folder named after the computer; the library holds "
                                    + SoundfoundryServers.library(level, host));
                    helper.assertTrue(SoundfoundryServers.fromOf(host, kept).equals(COMPUTER_NAME),
                            "and the library says it is from that computer");
                    final Text again = base.pc().musicDownloads().upload(level, local[0]);
                    helper.assertTrue(again.equals(SoundfoundryTexts.IN_LIBRARY.with("Copper Rain.wav")),
                            "the same song is not sent twice; said " + again.english());
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 400)
    public static void download_bringsASongOfTheLibraryToTheComputer(final GameTestHelper helper) {
        final Base base = wire(helper, true);
        final ServerLevel level = helper.getLevel();
        final String[] path = {""};
        linked(helper, base)
                .thenExecute(() -> {
                    final ServerServices.Host host = SoundfoundryServers.chosen(level, base.pc());
                    path[0] = keep(helper, host.machine(), "", "Pain.wav", 300);
                    act(helper, base.pc(), SoundfoundryActionPayload.DOWNLOAD, SongRefs.network(path[0]));
                    helper.assertTrue(downloads(base.pc()).size() == 1
                                    && downloads(base.pc()).getFirst().kind() == SongDownload.Kind.SERVER,
                            "the song comes down from the server");
                })
                .thenWaitUntil(() -> helper.assertTrue(
                        downloads(base.pc()).getFirst().status() == SongDownload.Status.DONE, "it comes in whole"))
                .thenExecute(() -> {
                    final String kept = FsPaths.join(MusicImports.musicFolderOf(base.pc()), "Pain.wav");
                    helper.assertTrue(SongFiles.read(level, base.pc(), kept) != null,
                            "and is kept in the music folder");
                    final SoundfoundryPagePayload home = page(helper, base.pc(), SoundfoundryPagePayload.HOME, "");
                    final SoundfoundryPagePayload.Row row = home.section(SoundfoundryPagePayload.NETWORK).getFirst();
                    helper.assertTrue(row.state() == SoundfoundryPagePayload.ON_DISK,
                            "the library's song is marked as on the disk; state " + row.state());
                    play(helper, base.pc(), SongRefs.network(path[0]));
                    helper.assertTrue(base.pc().musicPlayer().playing(level)
                                    && !base.pc().musicPlayer().streaming(level),
                            "and plays from the disk, not through the server");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 300)
    public static void pages_reachTheCatalogueOnlyThroughAServer(final GameTestHelper helper) {
        final Base without = wire(helper, false);
        final ServerLevel level = helper.getLevel();
        linked(helper, without)
                .thenWaitUntil(() -> helper.assertTrue(SoundfoundryCatalog.current().songs() > 0,
                        "the test catalogue is read"))
                .thenExecute(() -> {
                    final SoundfoundryPagePayload home = page(helper, without.pc(), SoundfoundryPagePayload.HOME, "");
                    helper.assertTrue(home.sidebar().servers().isEmpty() && !home.sidebar().catalog()
                                    && home.albums().isEmpty(),
                            "with no Soundfoundry Server on the network there is no catalogue");
                    without.rack().consoleOf(0).install(Programs.SOUNDFOUNDRY_SERVER.toString());
                    final SoundfoundryPagePayload served = page(helper, without.pc(), SoundfoundryPagePayload.HOME,
                            "");
                    helper.assertTrue(served.sidebar().servers().size() == 1 && served.sidebar().catalog()
                                    && !served.albums().isEmpty()
                                    && served.sidebar().servers().getFirst().name().equals(SERVER_NAME),
                            "with one, the sidebar names it and the catalogue's albums are listed");
                    final SoundfoundryPagePayload.Album album = served.albums().getFirst();
                    final SoundfoundryPagePayload page = page(helper, without.pc(), SoundfoundryPagePayload.ALBUM,
                            album.id());
                    helper.assertTrue(page.rows().size() == album.songs() && album.songs() > 0,
                            "an album's page lists its songs");
                    act(helper, without.pc(), SoundfoundryActionPayload.PLAY_PAGE, SoundfoundryPagePayload.ALBUM,
                            1L, album.id());
                    helper.assertTrue(without.pc().console().soundfoundry().size() == album.songs()
                                    && without.pc().console().soundfoundry().current() == 1
                                    && SongRefs.fromCatalog(without.pc().console().soundfoundry().song(0)),
                            "playing it from its second song puts the album on the list, on that song");
                    helper.assertTrue(without.pc().musicPlayer().streaming(level),
                            "which streams through the server; trouble "
                                    + without.pc().musicPlayer().trouble().english());
                    final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                            level.registryAccess());
                    SoundfoundryPagePayload.STREAM_CODEC.encode(buf, page);
                    final SoundfoundryPagePayload read = SoundfoundryPagePayload.STREAM_CODEC.decode(buf);
                    helper.assertTrue(read.equals(page) && buf.readableBytes() == 0, "and a page arrives as sent");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void playlists_keepLikedSongsAndTheOnesThePlayerMakes(final GameTestHelper helper) {
        final Base base = wire(helper, true);
        final ServerLevel level = helper.getLevel();
        linked(helper, base)
                .thenExecute(() -> {
                    final String song = keep(helper, base.pc(), "", "Riot.wav", 300);
                    act(helper, base.pc(), SoundfoundryActionPayload.LIKE, song);
                    helper.assertTrue(SoundfoundryPlaylists.read(level, base.pc(), SoundfoundryPlaylists.LIKED)
                            .equals(List.of(song)), "a liked song is in the liked songs");
                    act(helper, base.pc(), SoundfoundryActionPayload.LIKE, song);
                    helper.assertTrue(SoundfoundryPlaylists.read(level, base.pc(), SoundfoundryPlaylists.LIKED)
                            .isEmpty(), "and liking it again takes it out");
                    act(helper, base.pc(), SoundfoundryActionPayload.PLAYLIST_NEW, "Mining Mix");
                    act(helper, base.pc(), SoundfoundryActionPayload.PLAYLIST_ADD, "Mining Mix", song);
                    final SoundfoundryPagePayload mix = page(helper, base.pc(), SoundfoundryPagePayload.PLAYLIST,
                            "Mining Mix");
                    helper.assertTrue(mix.sidebar().playlists().equals(List.of(SoundfoundryPlaylists.LIKED,
                                    "Mining Mix")),
                            "a new playlist is listed after the liked songs; listed " + mix.sidebar().playlists());
                    helper.assertTrue(mix.rows().size() == 1 && mix.rows().getFirst().ref().equals(song)
                                    && mix.rows().getFirst().state() == SoundfoundryPagePayload.LOCAL,
                            "and holds what was added, one of the computer's own files; holds " + mix.rows());
                    act(helper, base.pc(), SoundfoundryActionPayload.PLAYLIST_REMOVE, "Mining Mix");
                    helper.assertTrue(SoundfoundryPlaylists.read(level, base.pc(), "Mining Mix").isEmpty(),
                            "and a song taken out is gone from it");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void pickServer_choosesWhichOfTwoServersToStreamFrom(final GameTestHelper helper) {
        final Base base = wire(helper, true);
        TestWorldBuilder.mountDefaultServer(base.rack(), 4);
        final ServerLevel level = helper.getLevel();
        linked(helper, base)
                .thenExecute(() -> {
                    base.rack().unitHost(4).installOs(ResourceLocation.fromNamespaceAndPath("jsc", "debian"));
                    base.rack().consoleOf(4).install(Programs.SOUNDFOUNDRY_SERVER.toString());
                })
                .thenWaitUntil(() -> helper.assertTrue(SoundfoundryServers.of(level, base.pc()).size() == 2,
                        "both servers run it"))
                .thenExecute(() -> {
                    final List<ServerServices.Host> both = SoundfoundryServers.of(level, base.pc());
                    helper.assertTrue(SoundfoundryServers.chosen(level, base.pc()).id().equals(both.getFirst().id()),
                            "the first found is streamed from until the player picks");
                    act(helper, base.pc(), SoundfoundryActionPayload.PICK_SERVER, both.get(1).id());
                    helper.assertTrue(SoundfoundryServers.chosen(level, base.pc()).id().equals(both.get(1).id()),
                            "and then the one picked");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void cover_ofACatalogueAlbumIsTheCoverBesideItsSongs(final GameTestHelper helper) {
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(SoundfoundryCatalog.current().cover(TEST_ALBUM) != null,
                        "the test album's cover is read with the catalogue"))
                .thenExecute(() -> {
                    final byte[] cover = SoundfoundryCovers.cover(SoundfoundryCovers.albumKey(TEST_ALBUM));
                    helper.assertTrue(png(cover), "the cover travels as a PNG");
                    final MediaId track = SoundfoundryCatalog.current().albums().stream()
                            .filter(album -> album.id().equals(TEST_ALBUM)).findFirst().orElseThrow()
                            .tracks().getFirst().media();
                    helper.assertTrue(SoundfoundryCovers.catalogKey(TEST_ALBUM, track)
                                    .equals(SoundfoundryCovers.albumKey(TEST_ALBUM)),
                            "and its songs are shown with it");
                })
                .thenSucceed();
    }

    @GameTest(template = ARENA)
    public static void cover_ofARecordingIsThePictureItCarries(final GameTestHelper helper) {
        final MediaId plain = put(TestMedia.wav(300, "a song with no picture " + helper.absolutePos(COMPUTER)));
        final MediaId pictured = put(withPicture(TestMedia.wav(300, "a song with a picture "
                + helper.absolutePos(COMPUTER))));
        helper.assertTrue(png(SoundfoundryCovers.cover(SoundfoundryCovers.mediaKey(pictured))),
                "a recording that carries a picture has it for its cover, made a PNG");
        helper.assertTrue(SoundfoundryCovers.cover(SoundfoundryCovers.mediaKey(plain)).length == 0,
                "and one that carries none has no cover, which the window makes of colours");
        helper.assertTrue(SoundfoundryCovers.cover("media:not a recording").length == 0,
                "a key that names nothing is no cover either");
        helper.succeed();
    }

    /*
     * A Mainframe with a server rack and a Standard personal computer with its monitor on one network, the server
     * named and given a system, and running the Soundfoundry Server when {@code serving}.
     */
    private static Base wire(final GameTestHelper helper, final boolean serving) {
        final TestWorldBuilder world = TestWorldBuilder.forGameTest(helper);
        world.placeRunningMainframe(new BlockPos(1, 2, 2));
        world.setBlock(new BlockPos(2, 2, 2), ComputingModule.HBW_CABLE);
        final ServerRackBlockEntity rack = world.placeSeededRack(new BlockPos(2, 2, 1));
        world.setBlock(new BlockPos(3, 2, 2), ComputingModule.PERSONAL_ROUTER.get());
        world.setBlock(new BlockPos(4, 2, 2), ComputingModule.ETHERNET_CABLE);
        final PersonalComputerBlockEntity pc = world.placeRunningPersonalComputer(COMPUTER);
        world.placeMonitor(MONITOR, Direction.EAST);
        pc.console().install(Programs.SOUNDFOUNDRY.toString());
        pc.console().setComputerName(COMPUTER_NAME);
        rack.unitHost(0).installOs(ResourceLocation.fromNamespaceAndPath("jsc", "debian"));
        rack.consoleOf(0).setComputerName(SERVER_NAME);
        if (serving) {
            rack.consoleOf(0).install(Programs.SOUNDFOUNDRY_SERVER.toString());
        }
        return new Base(rack, pc);
    }

    /* Waits for the computer to be on the network and its monitor to play. */
    private static GameTestSequence linked(final GameTestHelper helper, final Base base) {
        return helper.startSequence().thenWaitUntil(() -> helper.assertTrue(base.pc().networkUuid() != null
                        && base.pc().playsRecordings() && base.rack().unitRunning(0),
                "the computer joins the network, its monitor links itself and the server runs"));
    }

    /* A song of that length kept on that machine, in that folder or its music folder; its path there. */
    private static String keep(final GameTestHelper helper, final IOsHost machine, final String folder,
                               final String name, final int millis) {
        final MediaId song = put(TestMedia.wav(millis, name + " " + helper.absolutePos(COMPUTER).asLong()));
        final ServerLevel level = helper.getLevel();
        helper.assertTrue(MusicImports.keep(level, machine, folder, false, name, song, info(song)).accepted(),
                "the song is kept");
        final String path = FsPaths.join(folder.isEmpty() ? MusicImports.musicFolderOf(machine) : folder, name);
        helper.assertTrue(SongFiles.read(level, machine, path) != null, "at " + path);
        return path;
    }

    /* The song goes on the computer's list alone, and plays. */
    private static void play(final GameTestHelper helper, final PersonalComputerBlockEntity pc, final String ref) {
        pc.console().soundfoundry().replace(List.of(ref), 0);
        pc.musicPlayer().play(helper.getLevel(), 0);
    }

    private static void act(final GameTestHelper helper, final PersonalComputerBlockEntity pc, final int action,
                            final String... paths) {
        act(helper, pc, action, 0, 0L, paths);
    }

    private static void act(final GameTestHelper helper, final PersonalComputerBlockEntity pc, final int action,
                            final int index, final long value, final String... paths) {
        SoundfoundryPagePayloads.act(helper.getLevel(), pc, new SoundfoundryActionPayload(pc.getBlockPos(),
                action, index, value, List.of(paths), List.of()));
    }

    private static SoundfoundryPagePayload page(final GameTestHelper helper, final PersonalComputerBlockEntity pc,
                                                final int page, final String arg) {
        return SoundfoundryPagePayloads.pageOf(helper.getLevel(), pc, page, arg);
    }

    private static List<SongDownload> downloads(final PersonalComputerBlockEntity pc) {
        return pc.console().soundfoundry().downloads();
    }

    private static boolean png(final byte[] bytes) {
        return bytes.length > 8 && bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
    }

    /* The Wave file with an ID3 tag of its own attaching a small picture as its front cover. */
    private static byte[] withPicture(final byte[] wav) {
        final ByteArrayOutputStream picture = new ByteArrayOutputStream();
        try {
            ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", picture);
        } catch (final IOException unexpected) {
            throw new IllegalStateException(unexpected);
        }
        final ByteArrayOutputStream frame = new ByteArrayOutputStream();
        frame.write(0);
        frame.writeBytes("image/png".getBytes(StandardCharsets.US_ASCII));
        frame.write(0);
        frame.write(3);
        frame.write(0);
        frame.writeBytes(picture.toByteArray());
        final byte[] body = frame.toByteArray();
        final ByteArrayOutputStream tag = new ByteArrayOutputStream();
        tag.writeBytes("ID3".getBytes(StandardCharsets.US_ASCII));
        tag.writeBytes(new byte[] {3, 0, 0});
        final int size = 10 + body.length;
        tag.writeBytes(new byte[] {(byte) (size >> 21 & 0x7F), (byte) (size >> 14 & 0x7F),
                (byte) (size >> 7 & 0x7F), (byte) (size & 0x7F)});
        tag.writeBytes("APIC".getBytes(StandardCharsets.US_ASCII));
        tag.writeBytes(new byte[] {(byte) (body.length >> 24), (byte) (body.length >> 16),
                (byte) (body.length >> 8), (byte) body.length, 0, 0});
        tag.writeBytes(body);
        final byte[] id3 = tag.toByteArray();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(wav);
        out.writeBytes("id3 ".getBytes(StandardCharsets.US_ASCII));
        out.writeBytes(new byte[] {(byte) id3.length, (byte) (id3.length >> 8), (byte) (id3.length >> 16),
                (byte) (id3.length >> 24)});
        out.writeBytes(id3);
        if ((id3.length & 1) == 1) {
            out.write(0);
        }
        final byte[] whole = out.toByteArray();
        final int riff = whole.length - 8;
        whole[4] = (byte) riff;
        whole[5] = (byte) (riff >> 8);
        whole[6] = (byte) (riff >> 16);
        whole[7] = (byte) (riff >> 24);
        return whole;
    }

    private static MediaId put(final byte[] wav) {
        try {
            return MediaStore.current().orElseThrow().put(wav, "wav");
        } catch (final IOException unexpected) {
            throw new IllegalStateException("a WAV of silence is a recording", unexpected);
        }
    }

    private static MediaInfo info(final MediaId media) {
        try {
            return MediaStore.current().orElseThrow().info(media);
        } catch (final IOException unexpected) {
            throw new IllegalStateException("a kept recording can be read", unexpected);
        }
    }

    /** The rack with the Soundfoundry Server in row 0, and the computer listening to it. */
    private record Base(ServerRackBlockEntity rack, PersonalComputerBlockEntity pc) {
    }
}
