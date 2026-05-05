package com.legendofjava.core.world;

import com.badlogic.gdx.math.Rectangle;
import com.legendofjava.core.entities.Item;

import java.util.ArrayList;
import java.util.List;

public class Quadrant {
    public final int col;
    public final int row;

    private List<Rectangle> collisions;
    private List<Item> items;

    public Quadrant(int col, int row) {
        this.col = col;
        this.row = row;
        this.collisions = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    public List<Rectangle> getCollisions() {
        return collisions;
    }

    public void addCollision(Rectangle rect) {
        collisions.add(rect);
    }

    public List<Item> getItems() {
        return items;
    }

    public void addItem(Item item) {
        items.add(item);
    }

    public void removeItem(Item item) {
        items.remove(item);
    }
}
