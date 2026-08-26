package com.bng.marketcoordination;

import com.bng.marketcoordination.command.MarketCoordinationCommands;
import com.bng.marketcoordination.config.CategoryBudgetConfig;
import com.bng.marketcoordination.config.CommodityCapConfig;
import com.bng.marketcoordination.config.CommodityCategoryRegistry;
import com.bng.marketcoordination.config.MarketCoordConfig;
import com.bng.marketcoordination.economy.NationRegistry;
import com.bng.marketcoordination.integration.ModPresence;
import com.bng.marketcoordination.integration.vc.VcVersionProbe;
import com.bng.marketcoordination.market.MarketDayCycleHandler;
import com.bng.marketcoordination.market.MarketDiscoveryHandler;
import com.bng.marketcoordination.market.MarketRegistry;
import com.bng.marketcoordination.network.MarketNetworking;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MarketCoordinationMod.MOD_ID)
public class MarketCoordinationMod {
    public static final String MOD_ID = "marketcoordination";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public MarketCoordinationMod(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, MarketCoordConfig.SPEC);

        modEventBus.addListener(MarketNetworking::register);

        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> MarketCoordinationCommands.register(event));
        NeoForge.EVENT_BUS.addListener(MarketDayCycleHandler::onServerTick);
        NeoForge.EVENT_BUS.addListener(MarketDiscoveryHandler::onRightClickBlock);
        ModPresence.logStartupPresence();
        VcVersionProbe.probe();
    }

    private void onServerStarting(ServerStartingEvent event) {
        var resources = event.getServer().getResourceManager();
        CommodityCapConfig.load(resources);
        CommodityCategoryRegistry.load(resources);
        CategoryBudgetConfig.load(resources);
        MarketRegistry.init(event.getServer());
        NationRegistry.init(event.getServer().overworld());
        MarketDayCycleHandler.reset();
        LOGGER.info("Market Coordination initialized on server start");
    }

    private void onServerStopping(ServerStoppingEvent event) {
        MarketRegistry.persist();
        NationRegistry.persist();
        LOGGER.info("Market Coordination persisted market data on server stop");
    }
}
