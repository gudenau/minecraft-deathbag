package net.gudenau.deathbag;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.gudenau.deathbag.mixin.LevelStorage$SessionAccessor;
import net.gudenau.deathbag.mixin.MinecraftServerAccessor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class DeathBag implements ModInitializer {
    public static final String MODID = "gud_deathbag";
    
    public static Identifier identify(String name) {
        return new Identifier(MODID, name);
    }
    
    @Override
    public void onInitialize() {
        Items.init();
        registerEvents();
    }
    
    private void registerEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register((rawServer) -> {
            var server = (MinecraftServer & MinecraftServerAccessor) rawServer;
            var saveDir = ((LevelStorage$SessionAccessor) server.getSession()).getDirectory();
            PlayerData.init(saveDir.resolve(MODID));
        });
        ServerLifecycleEvents.SERVER_STOPPING.register((server) -> PlayerData.deinit());
        
        //FIXME Find a more elegant way of handling this.
        ServerTickEvents.END_SERVER_TICK.register(new ServerTickEvents.EndTick() {
            int ticks = 0;
            
            @Override
            public void onEndTick(MinecraftServer server) {
                // Once a minute
                if (ticks++ == 20 * 60) {
                    ticks = 0;
                    PlayerData.flushAbsentPlayers(server);
                }
            }
        });
    }
    
    public static final class Items {
        public static final Item DEATH_BAG = new DeathBagItem(new FabricItemSettings().maxCount(1).group(ItemGroup.MISC));
        
        private static void init() {
            register("death_bag", DEATH_BAG);
        }
        
        private static void register(String name, Item item) {
            Registry.register(Registry.ITEM, identify(name), item);
        }
    }
}
