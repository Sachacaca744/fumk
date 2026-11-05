package com.cobbleshop;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Interface de boutique : un coffre de 6 lignes cote serveur (menu vanilla),
 * donc les joueurs n'ont aucun mod a installer.
 */
public class ShopMenu extends ChestMenu {
    private static final int PER_PAGE = 45;

    private final SimpleContainer box;
    private final ServerPlayer sp;
    private final String shopId;     // null = liste des boutiques
    private final boolean fromList;  // true = bouton "Retour" affiche
    private int page = 0;

    private ShopMenu(int id, Inventory inv, SimpleContainer box, ServerPlayer sp, String shopId, boolean fromList) {
        super(MenuType.GENERIC_9x6, id, inv, box, 6);
        this.box = box;
        this.sp = sp;
        this.shopId = shopId;
        this.fromList = fromList;
        render();
    }

    /** Ouvre la liste des boutiques (shopId = null) ou une boutique precise. */
    public static void open(ServerPlayer p, String shopId, boolean fromList) {
        Component title;
        if (shopId == null) {
            title = Component.literal("Boutiques");
        } else {
            Shop s = ShopStore.shops.get(shopId);
            title = Component.literal(s != null ? s.title : "Boutique");
        }
        p.openMenu(new SimpleMenuProvider(
                (id, inv, pl) -> new ShopMenu(id, inv, new SimpleContainer(54), (ServerPlayer) pl, shopId, fromList),
                title));
    }

    // ------------------------------------------------------------------ affichage

    private int totalItems() {
        if (shopId == null) return ShopStore.shops.size();
        Shop s = ShopStore.shops.get(shopId);
        return s == null ? 0 : s.items.size();
    }

    private int pages() {
        return Math.max(1, (totalItems() + PER_PAGE - 1) / PER_PAGE);
    }

    private void render() {
        box.clearContent();
        if (page >= pages()) page = pages() - 1;
        int start = page * PER_PAGE;

        if (shopId == null) {
            List<String> ids = new ArrayList<>(ShopStore.shops.keySet());
            for (int i = 0; i < PER_PAGE && start + i < ids.size(); i++) {
                box.setItem(i, shopIcon(ids.get(start + i)));
            }
        } else {
            Shop s = ShopStore.shops.get(shopId);
            if (s != null) {
                for (int i = 0; i < PER_PAGE && start + i < s.items.size(); i++) {
                    box.setItem(i, display(s.items.get(start + i)));
                }
            }
        }

        ItemStack filler = Util.named(new ItemStack(Items.GRAY_STAINED_GLASS_PANE), " ", ChatFormatting.GRAY);
        for (int i = 45; i < 54; i++) box.setItem(i, filler.copy());

        if (page > 0) box.setItem(45, Util.named(new ItemStack(Items.ARROW), "Page precedente", ChatFormatting.YELLOW));
        if (page < pages() - 1) box.setItem(53, Util.named(new ItemStack(Items.ARROW), "Page suivante", ChatFormatting.YELLOW));
        if (shopId != null && fromList) {
            box.setItem(48, Util.named(new ItemStack(Items.OAK_DOOR), "Retour aux boutiques", ChatFormatting.YELLOW));
        }
        box.setItem(49, balanceItem());
    }

    private ItemStack balanceItem() {
        ItemStack st = Util.named(new ItemStack(Items.GOLD_INGOT),
                "Solde : " + Util.money(Economy.get(sp.getUUID())), ChatFormatting.GOLD);
        List<Component> lore = new ArrayList<>();
        lore.add(Util.line("Page " + (page + 1) + "/" + pages(), ChatFormatting.GRAY));
        if (shopId != null) {
            lore.add(Util.line("Clic gauche : acheter x1", ChatFormatting.GRAY));
            lore.add(Util.line("Maj + clic gauche : acheter x64", ChatFormatting.GRAY));
            lore.add(Util.line("Clic droit : vendre x1", ChatFormatting.GRAY));
            lore.add(Util.line("Maj + clic droit : tout vendre", ChatFormatting.GRAY));
        }
        Util.addLore(st, lore);
        return st;
    }

    private ItemStack shopIcon(String id) {
        Shop s = ShopStore.shops.get(id);
        ItemStack icon = ShopEntry.decode(s.icon);
        if (icon.isEmpty()) icon = new ItemStack(Items.CHEST);
        icon.setCount(1);
        Util.named(icon, s.title, ChatFormatting.GOLD);
        List<Component> lore = new ArrayList<>();
        lore.add(Util.line(s.items.size() + " article(s)", ChatFormatting.GRAY));
        lore.add(Util.line("Cliquez pour ouvrir", ChatFormatting.YELLOW));
        Util.addLore(icon, lore);
        return icon;
    }

    private ItemStack display(ShopEntry e) {
        ItemStack st = e.stack();
        if (st.isEmpty()) {
            return Util.named(new ItemStack(Items.BARRIER), "Objet introuvable (mod retire ?)", ChatFormatting.RED);
        }
        if (st.getCount() > st.getMaxStackSize()) st.setCount(st.getMaxStackSize());

        List<Component> lore = new ArrayList<>();
        lore.add(Util.line(" ", ChatFormatting.GRAY));
        if (e.buy > 0) lore.add(Util.line("Achat : " + Util.money(e.buy), ChatFormatting.GREEN));
        if (e.sell > 0 && !e.isCommand()) lore.add(Util.line("Vente : " + Util.money(e.sell), ChatFormatting.AQUA));
        if (e.buy <= 0 && e.sell <= 0) lore.add(Util.line("Indisponible", ChatFormatting.RED));
        if (!e.isCommand() && e.stack().getCount() > 1) {
            lore.add(Util.line("Lot de " + e.stack().getCount(), ChatFormatting.GRAY));
        }
        if (e.isCommand() && e.buy > 0) lore.add(Util.line("Clic gauche pour acheter", ChatFormatting.GRAY));
        Util.addLore(st, lore);
        return st;
    }

    // ------------------------------------------------------------------ clics

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId >= 0 && slotId < 54) {
            handleClick(slotId, button, type);
            render();
            sendAllDataToRemote();
            return;
        }
        // Interdit tout ce qui pourrait deplacer des objets vers la boutique.
        if (type == ClickType.QUICK_MOVE || type == ClickType.PICKUP_ALL || type == ClickType.QUICK_CRAFT) {
            sendAllDataToRemote();
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void handleClick(int slot, int button, ClickType type) {
        if (type != ClickType.PICKUP && type != ClickType.QUICK_MOVE) return;
        boolean shift = type == ClickType.QUICK_MOVE;

        if (slot == 45 && page > 0) { page--; return; }
        if (slot == 53 && page < pages() - 1) { page++; return; }
        if (slot == 48 && shopId != null && fromList) {
            later(() -> open(sp, null, false));
            return;
        }
        if (slot >= PER_PAGE) return;

        int idx = page * PER_PAGE + slot;

        if (shopId == null) {
            List<String> ids = new ArrayList<>(ShopStore.shops.keySet());
            if (idx < ids.size()) {
                String id = ids.get(idx);
                later(() -> open(sp, id, true));
            }
            return;
        }

        Shop s = ShopStore.shops.get(shopId);
        if (s == null || idx >= s.items.size()) return;
        ShopEntry e = s.items.get(idx);

        if (button == 0) buy(e, shift ? 64 : 1);
        else if (button == 1) sell(e, shift);
    }

    private void later(Runnable r) {
        MinecraftServer server = sp.getServer();
        if (server != null) server.execute(r);
    }

    // ------------------------------------------------------------------ achat / vente

    private void buy(ShopEntry e, int mult) {
        if (e.buy <= 0) {
            Util.say(sp, "Cet article n'est pas a vendre.", ChatFormatting.RED);
            return;
        }
        ItemStack base = e.stack();
        if (base.isEmpty() && !e.isCommand()) {
            Util.say(sp, "Cet article est indisponible.", ChatFormatting.RED);
            return;
        }
        if (e.isCommand()) mult = 1;

        long cost;
        try {
            cost = Math.multiplyExact(e.buy, (long) mult);
        } catch (ArithmeticException ex) {
            return;
        }
        if (!Economy.take(sp.getUUID(), cost)) {
            Util.say(sp, "Pas assez d'argent : il faut " + Util.money(cost) + ".", ChatFormatting.RED);
            sp.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }

        if (e.isCommand()) {
            String cmd = e.command.trim();
            if (cmd.startsWith("/")) cmd = cmd.substring(1);
            cmd = cmd.replace("{player}", sp.getGameProfile().getName());
            try {
                MinecraftServer server = sp.getServer();
                CommandSourceStack src = server.createCommandSourceStack().withSuppressedOutput();
                server.getCommands().getDispatcher().execute(cmd, src);
            } catch (CommandSyntaxException ex) {
                Economy.add(sp.getUUID(), cost); // rembourse
                Util.say(sp, "Achat impossible : " + ex.getMessage(), ChatFormatting.RED);
                return;
            } catch (Exception ex) {
                Economy.add(sp.getUUID(), cost);
                CobbleShop.LOGGER.error("Commande de boutique en erreur : {}", cmd, ex);
                Util.say(sp, "Achat impossible (erreur du serveur).", ChatFormatting.RED);
                return;
            }
            Util.say(sp, "Achat effectue pour " + Util.money(cost) + ".", ChatFormatting.GREEN);
        } else {
            int remaining = base.getCount() * mult;
            int total = remaining;
            while (remaining > 0) {
                int n = Math.min(remaining, base.getMaxStackSize());
                ItemStack give = base.copy();
                give.setCount(n);
                if (!sp.getInventory().add(give)) {
                    sp.drop(give, false);
                }
                remaining -= n;
            }
            Util.say(sp, "Achete : " + total + " x " + base.getHoverName().getString()
                    + " pour " + Util.money(cost) + ".", ChatFormatting.GREEN);
        }
        sp.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    private void sell(ShopEntry e, boolean all) {
        if (e.sell <= 0 || e.isCommand()) {
            Util.say(sp, "Cet objet n'est pas rachete.", ChatFormatting.RED);
            return;
        }
        ItemStack base = e.stack();
        if (base.isEmpty()) return;

        int unit = Math.max(1, base.getCount());
        int have = 0;
        for (ItemStack s : sp.getInventory().items) {
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, base)) have += s.getCount();
        }
        int units = have / unit;
        if (units <= 0) {
            Util.say(sp, "Vous n'avez pas cet objet" + (unit > 1 ? " (il en faut " + unit + ")." : "."), ChatFormatting.RED);
            sp.playNotifySound(SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
            return;
        }
        if (!all) units = 1;

        long gain;
        try {
            gain = Math.multiplyExact(e.sell, (long) units);
        } catch (ArithmeticException ex) {
            return;
        }

        int toRemove = units * unit;
        for (ItemStack s : sp.getInventory().items) {
            if (toRemove <= 0) break;
            if (!s.isEmpty() && ItemStack.isSameItemSameTags(s, base)) {
                int n = Math.min(s.getCount(), toRemove);
                s.shrink(n);
                toRemove -= n;
            }
        }
        sp.getInventory().setChanged();
        Economy.add(sp.getUUID(), gain);
        Util.say(sp, "Vendu : " + (units * unit) + " x " + base.getHoverName().getString()
                + " pour " + Util.money(gain) + ".", ChatFormatting.AQUA);
        sp.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
    }
}
