package com.cloth.objects;

/**
 * Created by Brennan on 1/5/2020.
 */
public class ItemData {

    private double price;

    private int slot;

    private String name;

    public ItemData(double price, int slot, String name) {
        this.price = price;
        this.slot = slot;
        this.name = name;
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
}
