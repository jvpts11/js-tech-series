/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest;

import dev.jstech.computers.client.os.NetworkInteractorApp;
import dev.jstech.testkit.client.ClientTests;
import dev.jstech.tests.JsTests;
import dev.jstech.tests.clienttest.computers.AssemblyScreensClientTests;
import dev.jstech.tests.clienttest.computers.AutomationManagerClientTests;
import dev.jstech.tests.clienttest.computers.BookPagesClientTests;
import dev.jstech.tests.clienttest.computers.BookStylesClientTests;
import dev.jstech.tests.clienttest.computers.BootSplashClientTests;
import dev.jstech.tests.clienttest.computers.BusModelClientTests;
import dev.jstech.tests.clienttest.computers.BusWindowClientTests;
import dev.jstech.tests.clienttest.computers.CabinetItemClientTests;
import dev.jstech.tests.clienttest.computers.CableLineClientTests;
import dev.jstech.tests.clienttest.computers.CdeClientTests;
import dev.jstech.tests.clienttest.computers.ComputerCaseClientTests;
import dev.jstech.tests.clienttest.computers.CraftingChainClientTests;
import dev.jstech.tests.clienttest.computers.CrashPagesClientTests;
import dev.jstech.tests.clienttest.computers.CreativeTabsClientTests;
import dev.jstech.tests.clienttest.computers.DesktopMenuClientTests;
import dev.jstech.tests.clienttest.computers.DeviceIconClientTests;
import dev.jstech.tests.clienttest.computers.DeviceManagerClientTests;
import dev.jstech.tests.clienttest.computers.DockStationClientTests;
import dev.jstech.tests.clienttest.computers.EraBuildClientTests;
import dev.jstech.tests.clienttest.computers.EraEndClientTests;
import dev.jstech.tests.clienttest.computers.EraEverydayClientTests;
import dev.jstech.tests.clienttest.computers.EraFabricationClientTests;
import dev.jstech.tests.clienttest.computers.EraInstallClientTests;
import dev.jstech.tests.clienttest.computers.EraInstalledProgramsClientTests;
import dev.jstech.tests.clienttest.computers.EraNetworkClientTests;
import dev.jstech.tests.clienttest.computers.EraNetworkedProgramsClientTests;
import dev.jstech.tests.clienttest.computers.EraOperationsClientTests;
import dev.jstech.tests.clienttest.computers.EraOtherShellsClientTests;
import dev.jstech.tests.clienttest.computers.EraPeripheralsClientTests;
import dev.jstech.tests.clienttest.computers.EraProgrammingClientTests;
import dev.jstech.tests.clienttest.computers.EraProgramsClientTests;
import dev.jstech.tests.clienttest.computers.EraServiceInstallClientTests;
import dev.jstech.tests.clienttest.computers.EraShellClientTests;
import dev.jstech.tests.clienttest.computers.EraStorageClientTests;
import dev.jstech.tests.clienttest.computers.EraStorageFlowClientTests;
import dev.jstech.tests.clienttest.computers.EraSystemProgramsClientTests;
import dev.jstech.tests.clienttest.computers.EraSystemSetupClientTests;
import dev.jstech.tests.clienttest.computers.EraSystemsClientTests;
import dev.jstech.tests.clienttest.computers.ExposureClientTests;
import dev.jstech.tests.clienttest.computers.ExtremesClientTests;
import dev.jstech.tests.clienttest.computers.FilesSyncClientTests;
import dev.jstech.tests.clienttest.computers.FramesSevenTenClientTests;
import dev.jstech.tests.clienttest.computers.FreeBsdClientTests;
import dev.jstech.tests.clienttest.computers.FullJourneyClientTests;
import dev.jstech.tests.clienttest.computers.GatewayManagerClientTests;
import dev.jstech.tests.clienttest.computers.GpuClientTests;
import dev.jstech.tests.clienttest.computers.GuideScenesClientTests;
import dev.jstech.tests.clienttest.computers.GuidedInstallerClientTests;
import dev.jstech.tests.clienttest.computers.HelpTerminalClientTests;
import dev.jstech.tests.clienttest.computers.HelpWindowClientTests;
import dev.jstech.tests.clienttest.computers.InfoCenterClientTests;
import dev.jstech.tests.clienttest.computers.InstallMediaClientTests;
import dev.jstech.tests.clienttest.computers.InteracTuiClientTests;
import dev.jstech.tests.clienttest.computers.IsmsClientTests;
import dev.jstech.tests.clienttest.computers.LeftoverProgramsClientTests;
import dev.jstech.tests.clienttest.computers.LiveInstallClientTests;
import dev.jstech.tests.clienttest.computers.MachineSoundClientTests;
import dev.jstech.tests.clienttest.computers.MainframeClientTests;
import dev.jstech.tests.clienttest.computers.McNetSpaceClientTests;
import dev.jstech.tests.clienttest.computers.MediaDriveClientTests;
import dev.jstech.tests.clienttest.computers.MekanismClientTests;
import dev.jstech.tests.clienttest.computers.MemberRunClientTests;
import dev.jstech.tests.clienttest.computers.MemoryCrashClientTests;
import dev.jstech.tests.clienttest.computers.MenuSwapCursorClientTests;
import dev.jstech.tests.clienttest.computers.MonitorClientTests;
import dev.jstech.tests.clienttest.computers.MotionClientTests;
import dev.jstech.tests.clienttest.computers.MsdClientTests;
import dev.jstech.tests.clienttest.computers.MultipartModelClientTests;
import dev.jstech.tests.clienttest.computers.MusicImportClientTests;
import dev.jstech.tests.clienttest.computers.NetworkDeviceClientTests;
import dev.jstech.tests.clienttest.computers.NetworkEventLogClientTests;
import dev.jstech.tests.clienttest.computers.NetworkInteractorClientTests;
import dev.jstech.tests.clienttest.computers.NetworkLinksClientTests;
import dev.jstech.tests.clienttest.computers.NetworkServicesClientTests;
import dev.jstech.tests.clienttest.computers.NetworkSharesClientTests;
import dev.jstech.tests.clienttest.computers.NextgreClientTests;
import dev.jstech.tests.clienttest.computers.OffscreenDesktopClientTests;
import dev.jstech.tests.clienttest.computers.OpenWithClientTests;
import dev.jstech.tests.clienttest.computers.OutOfMemoryClientTests;
import dev.jstech.tests.clienttest.computers.PanelStylesClientTests;
import dev.jstech.tests.clienttest.computers.PatternEncoderClientTests;
import dev.jstech.tests.clienttest.computers.PeripheralCableClientTests;
import dev.jstech.tests.clienttest.computers.PrinterClientTests;
import dev.jstech.tests.clienttest.computers.ProphetClientTests;
import dev.jstech.tests.clienttest.computers.RackClientTests;
import dev.jstech.tests.clienttest.computers.RackCrashClientTests;
import dev.jstech.tests.clienttest.computers.RedstoneInterfaceClientTests;
import dev.jstech.tests.clienttest.computers.SettingsPersonalizeClientTests;
import dev.jstech.tests.clienttest.computers.SettingsScreenClientTests;
import dev.jstech.tests.clienttest.computers.SettingsSharingClientTests;
import dev.jstech.tests.clienttest.computers.SigmaConsoleClientTests;
import dev.jstech.tests.clienttest.computers.SigmaEditorClientTests;
import dev.jstech.tests.clienttest.computers.SigmaWidgetsClientTests;
import dev.jstech.tests.clienttest.computers.SoundfoundryClientTests;
import dev.jstech.tests.clienttest.computers.SoundfoundryShareClientTests;
import dev.jstech.tests.clienttest.computers.SoundfoundryStandardClientTests;
import dev.jstech.tests.clienttest.computers.SourceBuildClientTests;
import dev.jstech.tests.clienttest.computers.SupercomputerJourneyClientTests;
import dev.jstech.tests.clienttest.computers.SystemMotionClientTests;
import dev.jstech.tests.clienttest.computers.SystemUiClientTests;
import dev.jstech.tests.clienttest.computers.TaskbarClientTests;
import dev.jstech.tests.clienttest.computers.TerminalEditorClientTests;
import dev.jstech.tests.clienttest.computers.TerminalFontClientTests;
import dev.jstech.tests.clienttest.computers.TerminalSelectionClientTests;
import dev.jstech.tests.clienttest.computers.TerminalWidthClientTests;
import dev.jstech.tests.clienttest.computers.TransitionDesktopsClientTests;
import dev.jstech.tests.clienttest.computers.TrashClientTests;
import dev.jstech.tests.clienttest.computers.UiSweepClientTests;
import dev.jstech.tests.clienttest.computers.UnixClientTests;
import dev.jstech.tests.clienttest.computers.UnixFreeBsdEditorsClientTests;
import dev.jstech.tests.clienttest.computers.UpdateClientTests;
import dev.jstech.tests.clienttest.computers.VintageShellsClientTests;
import dev.jstech.tests.clienttest.computers.VolumeClientTests;
import dev.jstech.tests.clienttest.computers.WorkshopClientTests;
import dev.jstech.tests.clienttest.computers.WreckedMachineClientTests;
import dev.jstech.tests.clienttest.core.AcousticsClientTests;
import dev.jstech.tests.clienttest.core.AdvancementIconClientTests;
import dev.jstech.tests.clienttest.core.AudioClientTests;
import dev.jstech.tests.clienttest.core.AwayOwnersClientTests;
import dev.jstech.tests.clienttest.core.BigPayloadClientTests;
import dev.jstech.tests.clienttest.core.ConnectedPacksClientTests;
import dev.jstech.tests.clienttest.core.ConveyorClientTests;
import dev.jstech.tests.clienttest.core.CoreStateClientTests;
import dev.jstech.tests.clienttest.core.DataRegistryClientTests;
import dev.jstech.tests.clienttest.core.ComponentsClientTests;
import dev.jstech.tests.clienttest.core.DataScreenClientTests;
import dev.jstech.tests.clienttest.core.MaterialsClientTests;
import dev.jstech.tests.clienttest.core.NotificationsClientTests;
import dev.jstech.tests.clienttest.core.SettingsKitClientTests;
import dev.jstech.tests.clienttest.core.FluidClientTests;
import dev.jstech.tests.clienttest.core.FormedShapeClientTests;
import dev.jstech.tests.clienttest.core.GridPainterClientTests;
import dev.jstech.tests.clienttest.core.HazardDevicesClientTests;
import dev.jstech.tests.clienttest.core.BodyTemperatureClientTests;
import dev.jstech.tests.clienttest.core.FireClientTests;
import dev.jstech.tests.clienttest.core.MovingStructureClientTests;
import dev.jstech.tests.clienttest.core.MovingStructurePhysicsClientTests;
import dev.jstech.tests.clienttest.core.MovingStructureLifeClientTests;
import dev.jstech.tests.clienttest.core.MovingStructureToolsClientTests;
import dev.jstech.tests.clienttest.core.HudHologramClientTests;
import dev.jstech.tests.clienttest.core.InspectClientTests;
import dev.jstech.tests.clienttest.core.ItemStateClientTests;
import dev.jstech.tests.clienttest.core.ToolClientTests;
import dev.jstech.tests.clienttest.core.TooltipClientTests;
import dev.jstech.tests.clienttest.core.KeyboardFocusClientTests;
import dev.jstech.tests.clienttest.core.HudEditorClientTests;
import dev.jstech.tests.clienttest.core.ScreenTouchClientTests;
import dev.jstech.tests.clienttest.core.StillPartsClientTests;
import dev.jstech.tests.clienttest.core.ScreenToolsClientTests;
import dev.jstech.tests.clienttest.core.McpClientTests;
import dev.jstech.tests.clienttest.core.McpPlayClientTests;
import dev.jstech.tests.clienttest.core.StudioClientTests;
import dev.jstech.tests.clienttest.core.TextSizeClientTests;
import dev.jstech.tests.clienttest.core.CameraClientTests;
import dev.jstech.tests.clienttest.core.KeyActionClientTests;
import dev.jstech.tests.clienttest.core.EffectsClientTests;
import dev.jstech.tests.clienttest.core.LightingClientTests;
import dev.jstech.tests.clienttest.core.TerrainClientTests;
import dev.jstech.tests.clienttest.core.TestKitClientTests;
import dev.jstech.tests.clienttest.core.PlacesClientTests;
import dev.jstech.tests.clienttest.core.LandDrawingClientTests;
import dev.jstech.tests.clienttest.core.SkyClientTests;
import dev.jstech.tests.clienttest.core.PhysicsClientTests;
import dev.jstech.tests.clienttest.core.RoomClientTests;
import dev.jstech.tests.clienttest.core.TravelClientTests;
import dev.jstech.tests.clienttest.core.WorldChangesClientTests;
import dev.jstech.tests.clienttest.core.LiveReloadClientTests;
import dev.jstech.tests.clienttest.core.ContentPackClientTests;
import dev.jstech.tests.clienttest.core.FoundRockClientTests;
import dev.jstech.tests.clienttest.core.VariantClientTests;
import dev.jstech.tests.clienttest.core.RecipeChooserClientTests;
import dev.jstech.tests.clienttest.core.ReiRecipeViewClientTests;
import dev.jstech.tests.clienttest.core.InstancingClientTests;
import dev.jstech.tests.clienttest.core.FadeLayerClientTests;
import dev.jstech.tests.clienttest.core.TabPictureClientTests;
import dev.jstech.tests.clienttest.core.WideBackgroundClientTests;
import dev.jstech.tests.clienttest.core.ComponentMotionClientTests;
import dev.jstech.tests.clienttest.core.OutlineFontClientTests;
import dev.jstech.tests.clienttest.core.JadeStyleClientTests;
import dev.jstech.tests.clienttest.core.MachineLooksClientTests;
import dev.jstech.tests.clienttest.core.MachineScreenClientTests;
import dev.jstech.tests.clienttest.core.MediaClientTests;
import dev.jstech.tests.clienttest.core.ModelKitClientTests;
import dev.jstech.tests.clienttest.core.ModelKitEntitiesClientTests;
import dev.jstech.tests.clienttest.core.MultiblockToolsClientTests;
import dev.jstech.tests.clienttest.core.MultipartClientTests;
import dev.jstech.tests.clienttest.core.ProgressionClientTests;
import dev.jstech.tests.clienttest.core.ReachClientTests;
import dev.jstech.tests.clienttest.core.RenderTargetsClientTests;
import dev.jstech.tests.clienttest.core.SoundMixerClientTests;
import dev.jstech.tests.clienttest.core.WorldStabilityClientTests;
import dev.jstech.tests.clienttest.industrial.DiagnosticsClientTests;
import dev.jstech.tests.clienttest.industrial.CrcHandsClientTests;
import dev.jstech.tests.clienttest.industrial.CrcModelClientTests;
import dev.jstech.tests.clienttest.industrial.CrcScreenClientTests;
import dev.jstech.tests.clienttest.industrial.IndustrialControllerClientTests;
import dev.jstech.tests.clienttest.industrial.MachineMonitorClientTests;
import dev.jstech.tests.clienttest.industrial.IndustrialScreensClientTests;
import dev.jstech.tests.clienttest.industrial.RecipeViewClientTests;
import dev.jstech.tests.clienttest.series.CableBlockClientTests;
import dev.jstech.tests.clienttest.series.CableDragClientTests;
import dev.jstech.tests.clienttest.series.ClampMeterClientTests;
import dev.jstech.tests.clienttest.series.EndScreenClientTests;
import dev.jstech.tests.clienttest.series.EnergyConverterClientTests;
import dev.jstech.tests.clienttest.series.NetworkProbeClientTests;
import dev.jstech.tests.clienttest.series.OverheadLinesClientTests;
import dev.jstech.tests.clienttest.series.PipeClientTests;
import dev.jstech.tests.clienttest.series.SignalProbeClientTests;
import dev.jstech.tests.clienttest.series.SpacePersistorClientTests;
import dev.jstech.tests.clienttest.series.ManualClientTests;
import dev.jstech.tests.clienttest.series.ManualRecipeViewClientTests;
import dev.jstech.tests.clienttest.series.ManualImmersionClientTests;
import dev.jstech.tests.clienttest.series.ManualSiteClientTests;
import dev.jstech.tests.clienttest.series.HeldModelsClientTests;
import dev.jstech.tests.clienttest.series.SeriesManualsClientTests;
import dev.jstech.tests.clienttest.space.ClimbClientTests;
import dev.jstech.tests.clienttest.space.CrawlerClientTests;
import dev.jstech.tests.clienttest.space.FlightComputerClientTests;
import dev.jstech.tests.clienttest.space.GroundSkyClientTests;
import dev.jstech.tests.clienttest.space.LandingGroundClientTests;
import dev.jstech.tests.clienttest.space.LauncherClientTests;
import dev.jstech.tests.clienttest.space.LooseStonesClientTests;
import dev.jstech.tests.clienttest.space.LunarSuitClientTests;
import dev.jstech.tests.clienttest.space.MarsClientTests;
import dev.jstech.tests.clienttest.space.MoonClientTests;
import dev.jstech.tests.clienttest.space.RocketBuildClientTests;
import dev.jstech.tests.clienttest.space.RocketCabinClientTests;
import dev.jstech.tests.clienttest.space.RocketDesignClientTests;
import dev.jstech.tests.clienttest.space.RocketHangarClientTests;
import dev.jstech.tests.clienttest.space.RocketHologramClientTests;
import dev.jstech.tests.clienttest.space.RocketPartsClientTests;
import dev.jstech.tests.clienttest.space.SpaceOresClientTests;
import dev.jstech.tests.clienttest.space.SpaceRocksClientTests;
import dev.jstech.tests.clienttest.space.SuitControlsClientTests;
import dev.jstech.tests.clienttest.space.SystemSpaceClientTests;
import dev.jstech.tests.testkit.TestSuites;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The explicit list of the series' client test classes, handed to the Core's test kit, which runs them: their tests
 * are read from these classes only, with no scan of the class path, each in the suite its package names
 * ({@link TestSuites}), and each run with the series' helper, {@link ClientTestContext}.
 */
public final class ClientTestSuite {

    private static final List<Class<?>> CLASSES = List.of(
            AudioClientTests.class,
            AcousticsClientTests.class,
            AutomationManagerClientTests.class,
            BootSplashClientTests.class,
            SoundMixerClientTests.class,
            MachineSoundClientTests.class,
            MainframeClientTests.class,
            RackClientTests.class,
            MediaClientTests.class,
            MusicImportClientTests.class,
            SoundfoundryClientTests.class,
            SoundfoundryShareClientTests.class,
            SoundfoundryStandardClientTests.class,
            SigmaEditorClientTests.class,
            SigmaConsoleClientTests.class,
            CabinetItemClientTests.class,
            AssemblyScreensClientTests.class,
            CdeClientTests.class,
            ComputerCaseClientTests.class,
            CoreStateClientTests.class,
            CrashPagesClientTests.class,
            CraftingChainClientTests.class,
            DesktopMenuClientTests.class,
            DeviceManagerClientTests.class,
            DeviceIconClientTests.class,
            ExposureClientTests.class,
            FilesSyncClientTests.class,
            FramesSevenTenClientTests.class,
            TransitionDesktopsClientTests.class,
            CreativeTabsClientTests.class,
            RecipeViewClientTests.class,
            HudHologramClientTests.class,
            HazardDevicesClientTests.class,
            BodyTemperatureClientTests.class,
            FireClientTests.class,
            MovingStructureClientTests.class,
            MovingStructurePhysicsClientTests.class,
            MovingStructureLifeClientTests.class,
            MovingStructureToolsClientTests.class,
            WorldStabilityClientTests.class,
            ManualClientTests.class,
            ManualRecipeViewClientTests.class,
            ManualSiteClientTests.class,
            ManualImmersionClientTests.class,
            SeriesManualsClientTests.class,
            HeldModelsClientTests.class,
            BookStylesClientTests.class,
            GuideScenesClientTests.class,
            BookPagesClientTests.class,
            RenderTargetsClientTests.class,
            HelpWindowClientTests.class,
            HelpTerminalClientTests.class,
            FreeBsdClientTests.class,
            FullJourneyClientTests.class,
            EraSystemsClientTests.class,
            EraShellClientTests.class,
            EraProgramsClientTests.class,
            EraStorageClientTests.class,
            EraInstallClientTests.class,
            EraEverydayClientTests.class,
            EraSystemProgramsClientTests.class,
            EraInstalledProgramsClientTests.class,
            EraNetworkedProgramsClientTests.class,
            EraStorageFlowClientTests.class,
            EraServiceInstallClientTests.class,
            EraOperationsClientTests.class,
            EraBuildClientTests.class,
            EraSystemSetupClientTests.class,
            EraOtherShellsClientTests.class,
            EraNetworkClientTests.class,
            EraProgrammingClientTests.class,
            EraFabricationClientTests.class,
            EraPeripheralsClientTests.class,
            SupercomputerJourneyClientTests.class,
            EraEndClientTests.class,
            ExtremesClientTests.class,
            GatewayManagerClientTests.class,
            GpuClientTests.class,
            GridPainterClientTests.class,
            TerminalFontClientTests.class,
            GuidedInstallerClientTests.class,
            IndustrialScreensClientTests.class,
            CrcScreenClientTests.class,
            CrcModelClientTests.class,
            CrcHandsClientTests.class,
            FormedShapeClientTests.class,
            IndustrialControllerClientTests.class,
            MachineMonitorClientTests.class,
            InfoCenterClientTests.class,
            InstallMediaClientTests.class,
            InteracTuiClientTests.class,
            LeftoverProgramsClientTests.class,
            LiveInstallClientTests.class,
            MachineScreenClientTests.class,
            McNetSpaceClientTests.class,
            MotionClientTests.class,
            MenuSwapCursorClientTests.class,
            MediaDriveClientTests.class,
            MemoryCrashClientTests.class,
            OutOfMemoryClientTests.class,
            RackCrashClientTests.class,
            MonitorClientTests.class,
            MsdClientTests.class,
            MultipartModelClientTests.class,
            CableBlockClientTests.class,
            ClampMeterClientTests.class,
            EnergyConverterClientTests.class,
            PipeClientTests.class,
            EndScreenClientTests.class,
            NetworkProbeClientTests.class,
            CableDragClientTests.class,
            OverheadLinesClientTests.class,
            SignalProbeClientTests.class,
            SpacePersistorClientTests.class,
            MultiblockToolsClientTests.class,
            MultipartClientTests.class,
            ConveyorClientTests.class,
            ProgressionClientTests.class,
            ReachClientTests.class,
            ModelKitClientTests.class,
            ModelKitEntitiesClientTests.class,
            AwayOwnersClientTests.class,
            InstancingClientTests.class,
            FadeLayerClientTests.class,
            AdvancementIconClientTests.class,
            TabPictureClientTests.class,
            WideBackgroundClientTests.class,
            ComponentMotionClientTests.class,
            OutlineFontClientTests.class,
            JadeStyleClientTests.class,
            MachineLooksClientTests.class,
            ConnectedPacksClientTests.class,
            LightingClientTests.class,
            EffectsClientTests.class,
            TerrainClientTests.class,
            PlacesClientTests.class,
            LandDrawingClientTests.class,
            SkyClientTests.class,
            PhysicsClientTests.class,
            RoomClientTests.class,
            TravelClientTests.class,
            WorldChangesClientTests.class,
            ItemStateClientTests.class,
            ToolClientTests.class,
            CameraClientTests.class,
            TooltipClientTests.class,
            KeyboardFocusClientTests.class,
            HudEditorClientTests.class,
            ScreenTouchClientTests.class,
            StillPartsClientTests.class,
            ScreenToolsClientTests.class,
            TextSizeClientTests.class,
            DataRegistryClientTests.class,
            KeyActionClientTests.class,
            DiagnosticsClientTests.class,
            InspectClientTests.class,
            StudioClientTests.class,
            McpClientTests.class,
            McpPlayClientTests.class,
            FluidClientTests.class,
            BigPayloadClientTests.class,
            LiveReloadClientTests.class,
            ContentPackClientTests.class,
            FoundRockClientTests.class,
            VariantClientTests.class,
            RecipeChooserClientTests.class,
            ReiRecipeViewClientTests.class,
            DataScreenClientTests.class,
            ComponentsClientTests.class,
            SettingsKitClientTests.class,
            NotificationsClientTests.class,
            MaterialsClientTests.class,
            TaskbarClientTests.class,
            TerminalEditorClientTests.class,
            MekanismClientTests.class,
            CableLineClientTests.class,
            NetworkDeviceClientTests.class,
            NetworkLinksClientTests.class,
            BusWindowClientTests.class,
            BusModelClientTests.class,
            PeripheralCableClientTests.class,
            RedstoneInterfaceClientTests.class,
            NetworkInteractorClientTests.class,
            NetworkServicesClientTests.class,
            NetworkEventLogClientTests.class,
            MemberRunClientTests.class,
            NetworkSharesClientTests.class,
            OffscreenDesktopClientTests.class,
            SystemMotionClientTests.class,
            VintageShellsClientTests.class,
            OpenWithClientTests.class,
            PanelStylesClientTests.class,
            PatternEncoderClientTests.class,
            PrinterClientTests.class,
            DockStationClientTests.class,
            WorkshopClientTests.class,
            UpdateClientTests.class,
            IsmsClientTests.class,
            NextgreClientTests.class,
            ProphetClientTests.class,
            SettingsPersonalizeClientTests.class,
            SettingsScreenClientTests.class,
            SettingsSharingClientTests.class,
            SourceBuildClientTests.class,
            SystemUiClientTests.class,
            SigmaWidgetsClientTests.class,
            TerminalSelectionClientTests.class,
            TerminalWidthClientTests.class,
            TrashClientTests.class,
            UiSweepClientTests.class,
            VolumeClientTests.class,
            WreckedMachineClientTests.class,
            UnixClientTests.class,
            UnixFreeBsdEditorsClientTests.class,
            SystemSpaceClientTests.class,
            GroundSkyClientTests.class,
            ClimbClientTests.class,
            SpaceRocksClientTests.class,
            SpaceOresClientTests.class,
            LooseStonesClientTests.class,
            MoonClientTests.class,
            MarsClientTests.class,
            LunarSuitClientTests.class,
            SuitControlsClientTests.class,
            RocketPartsClientTests.class,
            RocketCabinClientTests.class,
            FlightComputerClientTests.class,
            LandingGroundClientTests.class,
            RocketHangarClientTests.class,
            RocketDesignClientTests.class,
            RocketBuildClientTests.class,
            RocketHologramClientTests.class,
            LauncherClientTests.class,
            CrawlerClientTests.class);

    private ClientTestSuite() {
    }

    /**
     * Hands the kit every test of the classes listed, a suite for each mod's package, and what every test must
     * start without: the view the Network Interactor keeps of what a player left it at, which hid from one test what
     * another typed into its search. Once, from the test mod's client constructor.
     *
     * @throws IllegalStateException when a class is in no suite's package, or a test does not take the series' helper
     */
    public static void register() {
        final Map<String, List<ClientTests.Test>> bySuite = new LinkedHashMap<>();
        for (final Class<?> type : CLASSES) {
            final String suite = TestSuites.of(type);
            if (suite == null) {
                throw new IllegalStateException("the client tests " + type.getName() + " are in no suite's package");
            }
            for (final Method method : type.getDeclaredMethods()) {
                final ClientTest marked = method.getAnnotation(ClientTest.class);
                if (marked != null) {
                    bySuite.computeIfAbsent(suite, unused -> new ArrayList<>()).add(ClientTests.testOf(type, method,
                            marked.timeoutTicks(), ClientTestContext.class));
                }
            }
        }
        bySuite.forEach((suite, tests) -> ClientTests.register(JsTests.MODID, suite, tests, ClientTestContext::new));
        // And the test kit's own tests, written with nothing of the series', as a mod built on the Core writes them.
        ClientTests.register(JsTests.MODID, "core", List.of(TestKitClientTests.class));
        ClientTests.beforeEach(NetworkInteractorApp::forgetView);
    }
}
