package com.cobbleshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Boutiques + configuration, sauvegardees dans config/cobbleshop/ */
public final class ShopStore {
    private ShopStore() {}

    public static final long MAX = 1_000_000_000_000_000L;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static Map<String, Shop> shops = new LinkedHashMap<>();
    public static Config config = new Config();

    public static class Config {
        public String symbol = "$";
        public long startBalance = 0;
    }

    public static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve("cobbleshop");
    }

    public static void load() {
        try {
            Files.createDirectories(dir());

            Path cf = dir().resolve("config.json");
            if (Files.exists(cf)) {
                Config c = GSON.fromJson(Files.readString(cf, StandardCharsets.UTF_8), Config.class);
                if (c != null) config = c;
                if (config.symbol == null) config.symbol = "$";
            } else {
                Files.writeString(cf, GSON.toJson(config), StandardCharsets.UTF_8);
            }

            Path sf = dir().resolve("shops.json");
            if (Files.exists(sf)) {
                Type t = new TypeToken<LinkedHashMap<String, Shop>>() {}.getType();
                Map<String, Shop> m = GSON.fromJson(Files.readString(sf, StandardCharsets.UTF_8), t);
                shops = m != null ? m : new LinkedHashMap<>();
                for (Map.Entry<String, Shop> e : shops.entrySet()) {
                    Shop s = e.getValue();
                    if (s.title == null) s.title = e.getKey();
                    if (s.items == null) s.items = new ArrayList<>();
                    if (s.icon == null) s.icon = ShopEntry.encode(new ItemStack(Items.CHEST));
                }
            } else {
                shops = new LinkedHashMap<>();
                example();
                saveShops();
            }
        } catch (Exception e) {
            CobbleShop.LOGGER.error("Chargement de CobbleShop impossible", e);
        }
        Economy.load();
    }

    private static void example() {
        Shop s = new Shop();
        s.title = "Boutique exemple";
        s.icon = ShopEntry.encode(new ItemStack(Items.EMERALD));
        s.items.add(new ShopEntry(new ItemStack(Items.BREAD), 10, 3, null));
        s.items.add(new ShopEntry(new ItemStack(Items.COOKED_BEEF), 25, 8, null));
        s.items.add(new ShopEntry(new ItemStack(Items.DIAMOND), 500, 150, null));
        shops.put("exemple", s);
    }

    public static void saveShops() {
        try {
            Files.createDirectories(dir());
            Files.writeString(dir().resolve("shops.json"), GSON.toJson(shops), StandardCharsets.UTF_8);
        } catch (Exception e) {
            CobbleShop.LOGGER.error("Sauvegarde des boutiques impossible", e);
        }
    }
}
