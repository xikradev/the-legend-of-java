package com.legendofjava.core.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

public class Fire {
    private Vector2 position;
    private Animation<TextureRegion> animation;
    private float stateTime;
    private Rectangle hitbox;

    public Fire(float x, float y, Texture spriteSheet) {
        this.position = new Vector2(x, y);

        // Os dois frames do fogo no npc-spritesheet.png estão em (52, 11, 16, 16) e (69, 11, 16, 16)
        TextureRegion frame1 = new TextureRegion(spriteSheet, 52, 11, 16, 16);
        TextureRegion frame2 = new TextureRegion(spriteSheet, 69, 11, 16, 16);

        // Frame duration de 0.2f é padrão para uma animação suave
        this.animation = new Animation<>(0.2f, frame1, frame2);
        this.animation.setPlayMode(Animation.PlayMode.LOOP);
        this.stateTime = 0f;

        // Hitbox usado para impedir que o player passe por cima do fogo
        this.hitbox = new Rectangle(x, y, 16, 16);
    }

    public void update(float delta) {
        stateTime += delta;
    }

    public void render(SpriteBatch batch) {
        TextureRegion currentFrame = animation.getKeyFrame(stateTime);
        if (currentFrame != null) {
            batch.draw(currentFrame, position.x, position.y);
        }
    }

    public Vector2 getPosition() {
        return position;
    }

    public Rectangle getHitbox() {
        return hitbox;
    }
}
