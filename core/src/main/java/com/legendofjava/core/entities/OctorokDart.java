package com.legendofjava.core.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.List;

public class OctorokDart {

    private static final float SPEED        = 120f;
    private static final float MAX_DISTANCE = 128f;

    // Dart sprite region in enemies-spritesheet.png
    // Small 8×8 rock sprite — adjust srcX/srcY if needed
    private static final int DART_SRC_X = 76;
    private static final int DART_SRC_Y = 11;
    private static final int DART_W     = 8;
    private static final int DART_H     = 8;

    private final Vector2 position;
    private final Vector2 startPosition;
    private final Vector2 direction;
    private final TextureRegion sprite;
    private boolean active;

    public OctorokDart(float x, float y, float dirX, float dirY, Texture spriteSheet) {
        this.position      = new Vector2(x, y);
        this.startPosition = new Vector2(x, y);
        this.direction     = new Vector2(dirX, dirY).nor();
        this.sprite        = new TextureRegion(spriteSheet, DART_SRC_X, DART_SRC_Y, DART_W, DART_H);
        this.active        = true;
    }

    public void update(float delta, List<Rectangle> collisions) {
        if (!active) return;

        position.x += direction.x * SPEED * delta;
        position.y += direction.y * SPEED * delta;

        // Deactivate when max range reached
        if (position.dst(startPosition) > MAX_DISTANCE) {
            active = false;
            return;
        }

        // Deactivate on collision with map
        Rectangle hitbox = getHitbox();
        for (Rectangle rect : collisions) {
            if (hitbox.overlaps(rect)) {
                active = false;
                return;
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (!active) return;
        batch.draw(sprite, position.x - DART_W / 2f, position.y - DART_H / 2f, DART_W, DART_H);
    }

    public Rectangle getHitbox() {
        return new Rectangle(position.x - 3, position.y - 3, 6, 6);
    }

    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
    }
}
