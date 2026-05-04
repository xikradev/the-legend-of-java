package com.legendofjava.core.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public abstract class Item {
    protected Vector2 position;
    protected TextureRegion sprite;
    protected boolean active = true;

    public Item(float x, float y, TextureRegion sprite) {
        this.position = new Vector2(x, y);
        this.sprite = sprite;
    }

    public void update(float delta) {
        // Lógica de atualização base do item, caso ele seja animado
    }

    public void render(SpriteBatch batch) {
        if (active && sprite != null) {
            batch.draw(sprite, position.x, position.y);
        }
    }

    public Vector2 getPosition() {
        return position;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public abstract void onCollect(Player player);
}
