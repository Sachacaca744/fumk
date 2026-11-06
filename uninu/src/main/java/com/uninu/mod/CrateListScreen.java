package com.uninu.mod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Écran 1 : liste des coffres */
public class CrateListScreen extends Screen {
    private static final int PER = 6;
    private final List<CrateStore.Crate> crates;
    private final List<EditBox> nameBoxes = new ArrayList<>();
    private int page = 0;

    public CrateListScreen(List<CrateStore.Crate> crates) {
        super(Component.literal("UNINU - Coffres"));
        this.crates = crates;
    }

    @Override
    protected void init() {
        nameBoxes.clear();
        int left = width / 2 - 150, top = 45, start = page * PER;

        for (int i = 0; i < PER; i++) {
            int idx = start + i;
            if (idx >= crates.size()) break;
            int y = top + i * 24;
            final CrateStore.Crate c = crates.get(idx);

            EditBox n = new EditBox(font, left, y, 150, 20, Component.literal("nom"));
            n.setMaxLength(40);
            n.setValue(c.name);
            addRenderableWidget(n);
            nameBoxes.add(n);

            addRenderableWidget(Button.builder(Component.literal("Récompenses (" + c.rewards.size() + ")"), b -> {
                commit();
                Minecraft.getInstance().setScreen(new RewardScreen(this, c, crates));
            }).bounds(left + 155, y, 115, 20).build());

            addRenderableWidget(Button.builder(Component.literal("X"), b -> {
                commit();
                crates.remove(c);
                if (page > 0 && page * PER >= crates.size()) page--;
                rebuildWidgets();
            }).bounds(left + 275, y, 25, 20).build());
        }

        int by = top + PER * 24 + 8;
        addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            commit();
            if (page > 0) { page--; rebuildWidgets(); }
        }).bounds(left, by, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            commit();
            if ((page + 1) * PER < crates.size()) { page++; rebuildWidgets(); }
        }).bounds(left + 24, by, 20, 20).build());
        addRenderableWidget(Button.builder(Component.literal("+ Coffre"), b -> {
            commit();
            crates.add(new CrateStore.Crate("c" + Long.toString(System.currentTimeMillis(), 36), "Nouveau coffre"));
            page = (crates.size() - 1) / PER;
            rebuildWidgets();
        }).bounds(left + 50, by, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Sauvegarder"), b -> {
            commit();
            Net.CHANNEL.sendToServer(new Net.Save(crates));
            onClose();
        }).bounds(left + 125, by, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Fermer"), b -> onClose())
                .bounds(left + 230, by, 70, 20).build());
    }

    private void commit() {
        int start = page * PER;
        for (int i = 0; i < nameBoxes.size(); i++) {
            int idx = start + i;
            if (idx < crates.size()) crates.get(idx).name = nameBoxes.get(i).getValue();
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
        g.drawCenteredString(font, "Un coffre = une clef + une liste de récompenses aléatoires", width / 2, 30, 0xAAAAAA);
        int pages = Math.max(1, (crates.size() + PER - 1) / PER);
        g.drawCenteredString(font, "Page " + (page + 1) + "/" + pages, width / 2, 45 + PER * 24 + 34, 0xAAAAAA);
        super.render(g, mx, my, pt);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
