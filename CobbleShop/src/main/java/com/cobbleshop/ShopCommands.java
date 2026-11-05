package com.cobbleshop;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class ShopCommands {
    private ShopCommands() {}

    private static final SuggestionProvider<CommandSourceStack> SHOPS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(ShopStore.shops.keySet(), builder);

    private static boolean op(CommandSourceStack s) {
        return s.hasPermission(2);
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        long max = ShopStore.MAX;

        // ------------------------------------------------------------ /shop
        LiteralArgumentBuilder<CommandSourceStack> shop = Commands.literal("shop")
                .executes(c -> openMain(c.getSource()))

                .then(Commands.literal("open")
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOPS)
                                .executes(c -> openShop(c.getSource(), StringArgumentType.getString(c, "shop")))))

                .then(Commands.literal("help").requires(ShopCommands::op).executes(c -> help(c.getSource())))
                .then(Commands.literal("list").requires(ShopCommands::op).executes(c -> list(c.getSource())))

                .then(Commands.literal("info").requires(ShopCommands::op)
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOPS)
                                .executes(c -> info(c))))

                .then(Commands.literal("create").requires(ShopCommands::op)
                        .then(Commands.argument("shop", StringArgumentType.word())
                                .executes(c -> create(c))))

                .then(Commands.literal("delete").requires(ShopCommands::op)
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOPS)
                                .executes(c -> delete(c))))

                .then(Commands.literal("title").requires(ShopCommands::op)
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOPS)
                                .then(Commands.argument("title", StringArgumentType.greedyString())
                                        .executes(c -> title(c)))))

                .then(Commands.literal("icon").requires(ShopCommands::op)
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOPS)
                                .executes(c -> icon(c))))

                .then(Commands.literal("add").requires(ShopCommands::op)
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOPS)
                                .then(Commands.argument("buy", LongArgumentType.longArg(0, max))
                                        .executes(c -> add(c, false))
                                        .then(Commands.argument("sell", LongArgumentType.longArg(0, max))
                                                .executes(c -> add(c, true))))))

                .then(Commands.literal("addcmd").requires(ShopCommands::op)
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOPS)
                                .then(Commands.argument("buy", LongArgumentType.longArg(1, max))
                                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                                .executes(c -> addCmd(c))))))

                .then(Commands.literal("remove").requires(ShopCommands::op)
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOPS)
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .executes(c -> remove(c)))))

                .then(Commands.literal("price").requires(ShopCommands::op)
                        .then(Commands.argument("shop", StringArgumentType.word()).suggests(SHOPS)
                                .then(Commands.argument("index", IntegerArgumentType.integer(1))
                                        .then(Commands.argument("buy", LongArgumentType.longArg(0, max))
                                                .executes(c -> price(c, false))
                                                .then(Commands.argument("sell", LongArgumentType.longArg(0, max))
                                                        .executes(c -> price(c, true)))))));
        d.register(shop);

        // ------------------------------------------------------------ economie
        d.register(Commands.literal("balance").executes(c -> balance(c.getSource())));
        d.register(Commands.literal("bal").executes(c -> balance(c.getSource())));

        d.register(Commands.literal("pay")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, max))
                                .executes(c -> pay(c)))));

        d.register(Commands.literal("eco").requires(ShopCommands::op)
                .then(Commands.literal("get")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(c -> ecoGet(c))))
                .then(Commands.literal("give")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, max))
                                        .executes(c -> ecoChange(c, "give")))))
                .then(Commands.literal("take")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, max))
                                        .executes(c -> ecoChange(c, "take")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0, max))
                                        .executes(c -> ecoChange(c, "set"))))));
    }

    // ================================================================ helpers

    private static int ok(CommandSourceStack s, String text) {
        s.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    private static int fail(CommandSourceStack s, String text) {
        s.sendFailure(Component.literal(text));
        return 0;
    }

    private static Shop find(CommandContext<CommandSourceStack> c) {
        String id = StringArgumentType.getString(c, "shop").toLowerCase();
        Shop s = ShopStore.shops.get(id);
        if (s == null) fail(c.getSource(), "Boutique introuvable : " + id);
        return s;
    }

    // ================================================================ joueurs

    private static int openMain(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (ShopStore.shops.isEmpty()) return fail(src, "Aucune boutique n'est configuree.");
        if (ShopStore.shops.size() == 1) {
            ShopMenu.open(p, ShopStore.shops.keySet().iterator().next(), false);
        } else {
            ShopMenu.open(p, null, false);
        }
        return 1;
    }

    private static int openShop(CommandSourceStack src, String id) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        id = id.toLowerCase();
        if (!ShopStore.shops.containsKey(id)) return fail(src, "Boutique introuvable : " + id);
        ShopMenu.open(p, id, false);
        return 1;
    }

    private static int balance(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        return ok(src, "Solde : " + Util.money(Economy.get(p.getUUID())));
    }

    private static int pay(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer from = c.getSource().getPlayerOrException();
        ServerPlayer to = EntityArgument.getPlayer(c, "player");
        long amount = LongArgumentType.getLong(c, "amount");
        if (from.getUUID().equals(to.getUUID())) return fail(c.getSource(), "Vous ne pouvez pas vous payer vous-meme.");
        if (!Economy.take(from.getUUID(), amount)) return fail(c.getSource(), "Solde insuffisant.");
        Economy.add(to.getUUID(), amount);
        Util.say(to, from.getGameProfile().getName() + " vous a envoye " + Util.money(amount) + ".", ChatFormatting.GREEN);
        return ok(c.getSource(), "Vous avez envoye " + Util.money(amount) + " a " + to.getGameProfile().getName() + ".");
    }

    // ================================================================ admin : economie

    private static int ecoGet(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer t = EntityArgument.getPlayer(c, "player");
        return ok(c.getSource(), t.getGameProfile().getName() + " : " + Util.money(Economy.get(t.getUUID())));
    }

    private static int ecoChange(CommandContext<CommandSourceStack> c, String mode) throws CommandSyntaxException {
        ServerPlayer t = EntityArgument.getPlayer(c, "player");
        long amount = LongArgumentType.getLong(c, "amount");
        switch (mode) {
            case "give" -> Economy.add(t.getUUID(), amount);
            case "take" -> Economy.add(t.getUUID(), -amount);
            default -> Economy.set(t.getUUID(), amount);
        }
        return ok(c.getSource(), t.getGameProfile().getName() + " : " + Util.money(Economy.get(t.getUUID())));
    }

    // ================================================================ admin : boutiques

    private static int help(CommandSourceStack s) {
        String[] lines = {
                "/shop create <id>                    cree une boutique",
                "/shop title <id> <titre>             change le titre",
                "/shop icon <id>                      icone = objet en main",
                "/shop add <id> <achat> [vente]       ajoute l'objet en main (la quantite en main = lot)",
                "/shop addcmd <id> <prix> <commande>  article qui lance une commande ({player} = acheteur)",
                "/shop price <id> <n> <achat> [vente] change les prix de l'article n",
                "/shop remove <id> <n>                retire l'article n",
                "/shop delete <id>                    supprime la boutique",
                "/shop info <id> | /shop list         affiche le contenu",
                "/eco give|take|set <joueur> <montant>",
                "Prix 0 = non achetable (ou non vendable)."
        };
        for (String l : lines) ok(s, l);
        return 1;
    }

    private static int list(CommandSourceStack s) {
        if (ShopStore.shops.isEmpty()) return ok(s, "Aucune boutique.");
        ShopStore.shops.forEach((id, sh) -> ok(s, id + " : " + sh.title + " (" + sh.items.size() + " articles)"));
        return 1;
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        Shop sh = find(c);
        if (sh == null) return 0;
        CommandSourceStack s = c.getSource();
        ok(s, "== " + sh.title + " ==");
        for (int i = 0; i < sh.items.size(); i++) {
            ShopEntry e = sh.items.get(i);
            ItemStack st = e.stack();
            String name = st.isEmpty() ? "(introuvable)" : st.getHoverName().getString();
            String extra = e.isCommand() ? "  -> /" + e.command : "";
            ok(s, (i + 1) + ". " + name + " x" + (st.isEmpty() ? 0 : st.getCount())
                    + "  achat " + Util.money(e.buy) + "  vente " + Util.money(e.sell) + extra);
        }
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> c) {
        String id = StringArgumentType.getString(c, "shop").toLowerCase();
        if (!id.matches("[a-z0-9_-]{1,32}")) return fail(c.getSource(), "Identifiant invalide (a-z, 0-9, - et _).");
        if (ShopStore.shops.containsKey(id)) return fail(c.getSource(), "Cette boutique existe deja.");
        Shop sh = new Shop();
        sh.title = id;
        sh.icon = ShopEntry.encode(new ItemStack(Items.CHEST));
        ShopStore.shops.put(id, sh);
        ShopStore.saveShops();
        return ok(c.getSource(), "Boutique '" + id + "' creee. Tenez un objet et faites /shop add " + id + " <prix>");
    }

    private static int delete(CommandContext<CommandSourceStack> c) {
        Shop sh = find(c);
        if (sh == null) return 0;
        ShopStore.shops.remove(StringArgumentType.getString(c, "shop").toLowerCase());
        ShopStore.saveShops();
        return ok(c.getSource(), "Boutique supprimee.");
    }

    private static int title(CommandContext<CommandSourceStack> c) {
        Shop sh = find(c);
        if (sh == null) return 0;
        sh.title = StringArgumentType.getString(c, "title");
        ShopStore.saveShops();
        return ok(c.getSource(), "Titre modifie.");
    }

    private static int icon(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Shop sh = find(c);
        if (sh == null) return 0;
        ItemStack hand = c.getSource().getPlayerOrException().getMainHandItem();
        if (hand.isEmpty()) return fail(c.getSource(), "Tenez un objet en main.");
        ItemStack copy = hand.copy();
        copy.setCount(1);
        sh.icon = ShopEntry.encode(copy);
        ShopStore.saveShops();
        return ok(c.getSource(), "Icone modifiee.");
    }

    private static int add(CommandContext<CommandSourceStack> c, boolean withSell) throws CommandSyntaxException {
        Shop sh = find(c);
        if (sh == null) return 0;
        ItemStack hand = c.getSource().getPlayerOrException().getMainHandItem();
        if (hand.isEmpty()) return fail(c.getSource(), "Tenez l'objet a vendre en main.");
        long buy = LongArgumentType.getLong(c, "buy");
        long sell = withSell ? LongArgumentType.getLong(c, "sell") : 0;
        sh.items.add(new ShopEntry(hand.copy(), buy, sell, null));
        ShopStore.saveShops();
        return ok(c.getSource(), "Article ajoute (n°" + sh.items.size() + ") : " + hand.getHoverName().getString()
                + " x" + hand.getCount() + ", achat " + Util.money(buy) + ", vente " + Util.money(sell));
    }

    private static int addCmd(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Shop sh = find(c);
        if (sh == null) return 0;
        ItemStack hand = c.getSource().getPlayerOrException().getMainHandItem();
        if (hand.isEmpty()) return fail(c.getSource(), "Tenez en main l'objet qui servira d'icone (renommez-le a l'enclume si besoin).");
        long buy = LongArgumentType.getLong(c, "buy");
        String cmd = StringArgumentType.getString(c, "command");
        ItemStack icon = hand.copy();
        icon.setCount(1);
        sh.items.add(new ShopEntry(icon, buy, 0, cmd));
        ShopStore.saveShops();
        return ok(c.getSource(), "Article-commande ajoute (n°" + sh.items.size() + ") : /" + cmd + " pour " + Util.money(buy));
    }

    private static int remove(CommandContext<CommandSourceStack> c) {
        Shop sh = find(c);
        if (sh == null) return 0;
        int i = IntegerArgumentType.getInteger(c, "index") - 1;
        if (i < 0 || i >= sh.items.size()) return fail(c.getSource(), "Numero invalide.");
        sh.items.remove(i);
        ShopStore.saveShops();
        return ok(c.getSource(), "Article retire.");
    }

    private static int price(CommandContext<CommandSourceStack> c, boolean withSell) {
        Shop sh = find(c);
        if (sh == null) return 0;
        int i = IntegerArgumentType.getInteger(c, "index") - 1;
        if (i < 0 || i >= sh.items.size()) return fail(c.getSource(), "Numero invalide.");
        ShopEntry e = sh.items.get(i);
        e.buy = LongArgumentType.getLong(c, "buy");
        if (withSell) e.sell = LongArgumentType.getLong(c, "sell");
        ShopStore.saveShops();
        return ok(c.getSource(), "Prix modifies : achat " + Util.money(e.buy) + ", vente " + Util.money(e.sell));
    }
}
