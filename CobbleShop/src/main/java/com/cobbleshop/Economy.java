package com.cobbleshop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Economie interne : un solde par joueur, sauvegarde dans config/cobbleshop/balances.json */
public final class Economy {
    private Economy() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Map<UUID, Long> balances = new HashMap<>();
    private static boolean dirty = false;

    public static long get(UUID id) {
        Long v = balances.get(id);
        if (v == null) {
            v = ShopStore.config.startBalance;
            balances.put(id, v);
            dirty = true;
        }
        return v;
    }

    public static void set(UUID id, long amount) {
        balances.put(id, Math.max(0L, Math.min(amount, ShopStore.MAX)));
        dirty = true;
    }

    public static void add(UUID id, long amount) {
        long cur = get(id);
        set(id, cur + amount);
    }

    /** Retire l'argent si le joueur en a assez. */
    public static boolean take(UUID id, long amount) {
        long cur = get(id);
        if (cur < amount) return false;
        set(id, cur - amount);
        return true;
    }

    public static void load() {
        balances = new HashMap<>();
        try {
            Path f = ShopStore.dir().resolve("balances.json");
            if (Files.exists(f)) {
                Type t = new TypeToken<Map<String, Long>>() {}.getType();
                Map<String, Long> raw = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), t);
                if (raw != null) {
                    for (Map.Entry<String, Long> e : raw.entrySet()) {
                        try {
                            balances.put(UUID.fromString(e.getKey()), e.getValue());
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            CobbleShop.LOGGER.error("Lecture des soldes impossible", e);
        }
        dirty = false;
    }

    public static void saveIfDirty() {
        if (dirty) save();
    }

    public static void save() {
        try {
            Files.createDirectories(ShopStore.dir());
            Map<String, Long> raw = new HashMap<>();
            for (Map.Entry<UUID, Long> e : balances.entrySet()) raw.put(e.getKey().toString(), e.getValue());
            Files.writeString(ShopStore.dir().resolve("balances.json"), GSON.toJson(raw), StandardCharsets.UTF_8);
            dirty = false;
        } catch (Exception e) {
            CobbleShop.LOGGER.error("Sauvegarde des soldes impossible", e);
        }
    }
}
