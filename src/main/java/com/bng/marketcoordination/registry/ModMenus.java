package com.bng.marketcoordination.registry;

import com.bng.marketcoordination.MarketCoordinationMod;
import com.bng.marketcoordination.menu.StallConfigMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registers our custom container menu types. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MarketCoordinationMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<StallConfigMenu>> STALL_CONFIG =
            MENUS.register("stall_config", () -> IMenuTypeExtension.create(StallConfigMenu::new));

    private ModMenus() {}

    public static void register(IEventBus modEventBus) {
        MENUS.register(modEventBus);
    }
}
