package com.uninu.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Écran 2 : récompenses d'un coffre */
public class RewardScreen extends Screen {
    private static final int PER = 5;
    private final Screen parent;
    private final CrateStore.Crate crate;
    private final List<CrateStore.Crate> allCrates;
    private final List<EditBox> nameBoxes = new ArrayList<>();
    private final List<EditBox> dataBoxes = new ArrayList<>();
    private final List<EditBox> weightBoxes = new ArrayList<>();
    private int page = 0;

    public RewardScreen(Screen parent, CrateStore.Crate crate, List<CrateStore.Crate> allCrates) {
        super(Component.literal("Récompenses : " + crate.name));
        this.parent = parent;
        this.crate = crate;
        this.allCrates = allCrates;
    }

    @Override
    protected void init() {
        nameBoxes.clear(); dataBoxes.clear(); weightBoxes.clear();
        int left = width / 2 - 160, top = 45, start = page * PER;

        for (int i = 0; i < PER; i++) {
            int idx = start + i;
            if (idx >= crate.rewards.size()) break;
            int y = top + i * 24;
            final CrateStore.Reward r = crate.rewards.get(idx);

            EditBox n = new EditBox(font, left, y, 80, 20, Component.literal("nom"));
            n.setMaxLength(40);
            n.setValue(r.name);
            addRenderableWidget(n);
            nameBoxes.add(n);

            addRenderableWidget(Button.builder(Component.literal("COMMAND".equals(r.type) ? "CMD" : "ITEM"), b -> {
                commit();
                r.type = "COMMAND".equals(r.type) ? "ITEM" : "COMMAND";
                rebuildWidgets();
            }).bounds(left + 85, y, 45, 20).build());

            EditBox d = new EditBox(font, left + 135, y, 125, 20, Component.literal("donnée"));
            d.setMaxLength(4096);
            d.setValue(r.data);
            addRenderableWidget(d);
            dataBoxes.add(d);

            EditBox w = new EditBox(font, left + 265, y, 30, 20, Component.literal("poids"));
            w.setMaxLength(4);
            w.setFilter(s -> s.matches("\\d*"));
            w.setValue(String.valueOf(r.weight));
            addRenderableWidget(w);
            weightBoxes.add(w);

            addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                commit();
                crate.rewards.remove(r);
                if (page > 0 && page * PER >= crate.rewards.size()) page--;
                rebuildWidgets();
            }).bounds(left + 300, y, 20, 20).build());
        }

        int by = top + PER * 24 + 6;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            commit();
            if (page > 0) { page--; rebuildWidgets(); }
        }).bounds(left, by, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            commit();
            if ((page + 1) * PER < crate.rewards.size()) { page++; rebuildWidgets(); }
        }).bounds(left + 24, by, 20, 20).build());

        addRenderableWidget(Button.builder(Component.literal("+ Item en main"), b -> {
            commit();
            var player = Minecraft.getInstance().player;
            ItemStack s = player == null ? ItemStack.EMPTY : player.getMainHandItem();
            if (s.isEmpty()) {
                if (player != null) player.displayClientMessage(Component.literal("§cTiens un item en main d'abord."), true);
                return;
            }
            String data = s.save(new CompoundTag()).toString();
            crate.rewards.add(new CrateStore.Reward(s.getHoverName().getString(), "ITEM", data, 10));
            page = (crate.rewards.size() - 1) / PER;
            rebuildWidgets();
        }).bounds(left + 50, by, 120, 20).build());

        addRenderableWidget(Button.builder(Component.literal("+ Commande"), b -> {
            commit();
            crate.rewards.add(new CrateStore.Reward("Nouvelle récompense", "COMMAND", "give %player% minecraft:apple", 10));
            page = (crate.rewards.size() - 1) / PER;
            rebuildWidgets();
        }).bounds(left + 175, by, 100, 20).build());

        addRenderableWidget(Button.builder(Component.literal("§6DONNER LE FINAL"), b -> {
            commit();
            Net.CHANNEL.sendToServer(new Net.Final(allCrates, crate.id));
            Minecraft.getInstance().setScreen(null);
        }).bounds(left, by + 24, 190, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Retour"), b -> onClose())
                .bounds(left + 195, by + 24, 80, 20).build());
    }

    private void commit() {
        int start = page * PER;
        for (int i = 0; i < nameBoxes.size(); i++) {
            int idx = start + i;
            if (idx >= crate.rewards.size()) continue;
            CrateStore.Reward r = crate.rewards.get(idx);
            r.name = nameBoxes.get(i).getValue();
            r.data = dataBoxes.get(i).getValue();
            try { r.weight = Math.max(1, Integer.parseInt(weightBoxes.get(i).getValue())); }
            catch (NumberFormatException e) { r.weight = 1; }
        }
    }

    @Override
    public void onClose() {
        commit();
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        int left = width / 2 - 160;
        g.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);
        g.drawCenteredString(font, "ITEM: id + quantité (ou item en main) | CMD: commande, %player% = joueur", width / 2, 26, 0xAAAAAA);
        g.drawString(font, "Nom", left, 36, 0x888888);
        g.drawString(font, "Type", left + 90, 36, 0x888888);
        g.drawString(font, "Donnée", left + 138, 36, 0x888888);
        g.drawString(font, "Poids", left + 266, 36, 0x888888);
        int pages = Math.max(1, (crate.rewards.size() + PER - 1) / PER);
        g.drawString(font, (page + 1) + "/" + pages, left + 280, 45 + PER * 24 + 12, 0xAAAAAA);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
