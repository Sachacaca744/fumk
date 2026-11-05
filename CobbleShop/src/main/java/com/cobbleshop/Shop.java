package com.cobbleshop;

import java.util.ArrayList;
import java.util.List;

public class Shop {
    public String title;
    public String icon; // ItemStack au format SNBT
    public List<ShopEntry> items = new ArrayList<>();
}
