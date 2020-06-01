package com.cloth.objects;

import java.util.List;

/**
 * Created by Brennan on 1/5/2020.
 */
public class ItemData {

    private double price;
    private int slot;
    private String name;
    private List<String> lore;

    public ItemData(double price, int slot, String name, List<String> lore) {
        this.price = price;
        this.slot = slot;
        this.name = name;
        this.lore = lore;
    }

    public double getPrice() {
        return price;
    }

    public int getSlot() {
        return slot;
    }

    public String getName() {
        return name;
    }

    public List<String> getLore() {
        return lore;
    }
}
