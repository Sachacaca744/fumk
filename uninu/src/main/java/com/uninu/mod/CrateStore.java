package com.uninu.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Coffres + récompenses, sauvegardés dans config/uninu_crates.json */
public class CrateStore {
    public static class Reward {
        public String name;
        public String type;   // "ITEM" ou "COMMAND"
        public String data;   // ITEM: "minecraft:diamond 3" ou SNBT complet | COMMAND: commande avec %player%
        public int weight;    // poids (chance relative)
        public Reward(String name, String type, String data, int weight) {
            this.name = name; this.type = type; this.data = data; this.weight = weight;
        }
    }

    public static class Crate {
        public String id;
        public String name;
        public List<Reward> rewards = new ArrayList<>();
        public Crate(String id, String name) { this.id = id; this.name = name; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static List<Crate> crates = null;

    private static Path file() { return FMLPaths.CONFIGDIR.get().resolve("uninu_crates.json"); }

    public static synchronized List<Crate> all() {
        if (crates == null) {
            crates = new ArrayList<>();
            try {
                if (Files.exists(file())) {
                    try (Reader r = Files.newBufferedReader(file())) {
                        List<Crate> l = GSON.fromJson(r, new TypeToken<List<Crate>>() {}.getType());
                        if (l != null) crates.addAll(l);
                    }
                } else {
                    Crate c = new Crate("exemple", "Coffre Exemple");
                    c.rewards.add(new Reward("Diamant x3", "ITEM", "minecraft:diamond 3", 10));
                    c.rewards.add(new Reward("Pomme dorée", "COMMAND", "give %player% minecraft:golden_apple", 30));
                    crates.add(c);
                    save();
                }
            } catch (Exception e) { e.printStackTrace(); }
        }
        return new ArrayList<>(crates);
    }

    public static synchronized Crate find(String id) {
        all();
        for (Crate c : crates) if (c.id.equals(id)) return c;
        return null;
    }

    public static synchronized void set(List<Crate> list) {
        crates = new ArrayList<>(list);
        save();
    }

    /** Nettoie ce qui vient d'un client. */
    public static List<Crate> sanitize(List<Crate> in) {
        List<Crate> out = new ArrayList<>();
        for (Crate c : in) {
            if (c.id == null || c.id.isBlank()) continue;
            Crate nc = new Crate(c.id.trim(), c.name == null || c.name.isBlank() ? "Coffre" : c.name.trim());
            for (Reward r : c.rewards) {
                if (r.data == null || r.data.isBlank()) continue;
                String type = "COMMAND".equals(r.type) ? "COMMAND" : "ITEM";
                String name = r.name == null || r.name.isBlank() ? "Récompense" : r.name.trim();
                String data = r.data.trim();
                if (type.equals("COMMAND") && data.startsWith("/")) data = data.substring(1);
                nc.rewards.add(new Reward(name, type, data, Math.max(1, r.weight)));
            }
            out.add(nc);
        }
        return out;
    }

    private static void save() {
        try (Writer w = Files.newBufferedWriter(file())) {
            GSON.toJson(crates, w);
        } catch (Exception e) { e.printStackTrace(); }
    }
}
