package com.legendofjava.core.entities;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

public abstract class Weapon extends Item {
    protected int damage;

    public Weapon(float x, float y, TextureRegion sprite, int damage) {
        super(x, y, sprite);
        this.damage = damage;
    }

    public int getDamage() {
        return damage;
    }
}
