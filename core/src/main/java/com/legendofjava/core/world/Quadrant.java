package com.legendofjava.core.world;

import com.badlogic.gdx.math.Rectangle;
import com.legendofjava.core.entities.Item;
import com.legendofjava.core.entities.HostNPC;
import com.legendofjava.core.entities.Fire;
import com.legendofjava.core.entities.Octorok;

import java.util.ArrayList;
import java.util.List;

public class Quadrant {
    public final int col;
    public final int row;

    private List<Rectangle> collisions;
    private List<Item> items;
    private List<HostNPC> hostNpcs;
    private List<Fire> fires;
    private List<Octorok> octoroks;

    public Quadrant(int col, int row) {
        this.col = col;
        this.row = row;
        this.collisions = new ArrayList<>();
        this.items = new ArrayList<>();
        this.hostNpcs = new ArrayList<>();
        this.fires = new ArrayList<>();
        this.octoroks = new ArrayList<>();
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

    public List<HostNPC> getHostNpcs() {
        return hostNpcs;
    }

    public void addHostNpc(HostNPC npc) {
        hostNpcs.add(npc);
    }

    public List<Fire> getFires() {
        return fires;
    }

    public void addFire(Fire fire) {
        fires.add(fire);
    }

    public List<Octorok> getOctoroks() {
        return octoroks;
    }

    public void addOctorok(Octorok octorok) {
        octoroks.add(octorok);
    }

    public void removeOctorok(Octorok octorok) {
        octoroks.remove(octorok);
    }
}

