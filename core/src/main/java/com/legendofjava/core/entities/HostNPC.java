package com.legendofjava.core.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class HostNPC {
    private Vector2 position;
    private TextureRegion sprite;
    private Rectangle hitbox;

    public HostNPC(float x, float y, Texture spriteSheet) {
        this.position = new Vector2(x, y);
        // O sprite do Old Man no npc-spritesheet.png está na coordenada (1, 11) com tamanho 16x16
        this.sprite = new TextureRegion(spriteSheet, 1, 11, 16, 16);
        // O hitbox é usado para impedir que o player passe por cima do NPC
        this.hitbox = new Rectangle(x, y, 16, 16);
    }

    public void render(SpriteBatch batch) {
        if (sprite != null) {
            batch.draw(sprite, position.x, position.y);
        }
    }

    public Vector2 getPosition() {
        return position;
    }

    public Rectangle getHitbox() {
        return hitbox;
    }
}
