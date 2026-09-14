package com.bng.marketcoordination.client;

import com.bng.marketcoordination.registry.ModMenus;
import com.bng.marketcoordination.ui.client.StallConfigScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** Client-only wiring for the stall configuration GUI. */
public final class StallConfigClient {

    private StallConfigClient() {}

    public static void init(IEventBus modEventBus) {
        modEventBus.addListener(StallConfigClient::registerScreens);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.STALL_CONFIG.get(), StallConfigScreen::new);
    }
}
