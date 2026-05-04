package com.legendofjava.core.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class Player {
    private Vector2 position;
    private Vector2 velocity;
    private final float speed = 100f; // pixels per second

    private Texture spriteSheet;

    private Animation<TextureRegion> walkDown;
    private Animation<TextureRegion> walkRight;
    private Animation<TextureRegion> walkUp;
    private Animation<TextureRegion> walkLeft; // Flipped version of right

    private float stateTime;

    public enum State {
        IDLE, WALKING_UP, WALKING_DOWN, WALKING_LEFT, WALKING_RIGHT
    }

    private State currentState;
    private State previousState;
    private TextureRegion currentFrame;

    public Player(float startX, float startY) {
        position = new Vector2(startX, startY);
        velocity = new Vector2(0, 0);
        currentState = State.IDLE;
        previousState = State.IDLE;

        loadAnimations();
    }

    private void loadAnimations() {
        spriteSheet = new Texture("sprites/link-spritesheet.png");

        // Frame size is 16x16, gaps are 1px. Start Y is 11.
        TextureRegion down1 = new TextureRegion(spriteSheet, 0, 11, 16, 16);
        TextureRegion down2 = new TextureRegion(spriteSheet, 17, 11, 16, 16);

        TextureRegion right1 = new TextureRegion(spriteSheet, 34, 11, 16, 16);
        TextureRegion right2 = new TextureRegion(spriteSheet, 51, 11, 16, 16);

        TextureRegion up1 = new TextureRegion(spriteSheet, 68, 11, 16, 16);
        int up2Width = (85 + 16 > spriteSheet.getWidth()) ? spriteSheet.getWidth() - 85 : 16;
        TextureRegion up2 = new TextureRegion(spriteSheet, 85, 11, up2Width, 16);

        // Flipped left animations
        TextureRegion left1 = new TextureRegion(right1);
        left1.flip(true, false);
        TextureRegion left2 = new TextureRegion(right2);
        left2.flip(true, false);

        float frameDuration = 0.2f;
        walkDown = new Animation<>(frameDuration, down1, down2);
        walkDown.setPlayMode(Animation.PlayMode.LOOP);

        walkRight = new Animation<>(frameDuration, right1, right2);
        walkRight.setPlayMode(Animation.PlayMode.LOOP);

        walkUp = new Animation<>(frameDuration, up1, up2);
        walkUp.setPlayMode(Animation.PlayMode.LOOP);

        walkLeft = new Animation<>(frameDuration, left1, left2);
        walkLeft.setPlayMode(Animation.PlayMode.LOOP);

        currentFrame = down1; // Default looking down
    }

    public void update(float delta) {
        handleInput();

        // Update position
        position.x += velocity.x * delta;
        position.y += velocity.y * delta;

        // Determine state
        if (velocity.x > 0) {
            currentState = State.WALKING_RIGHT;
        } else if (velocity.x < 0) {
            currentState = State.WALKING_LEFT;
        } else if (velocity.y > 0) {
            currentState = State.WALKING_UP;
        } else if (velocity.y < 0) {
            currentState = State.WALKING_DOWN;
        } else {
            currentState = State.IDLE;
        }

        // Animation state time
        if (currentState != previousState) {
            stateTime = 0;
        } else if (currentState != State.IDLE) {
            stateTime += delta;
        }

        previousState = currentState;
        updateFrame();
    }

    private void handleInput() {
        velocity.set(0, 0);

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.isKeyPressed(Input.Keys.A)) {
            velocity.x = -speed;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.isKeyPressed(Input.Keys.D)) {
            velocity.x = speed;
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP) || Gdx.input.isKeyPressed(Input.Keys.W)) {
            velocity.y = speed;
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN) || Gdx.input.isKeyPressed(Input.Keys.S)) {
            velocity.y = -speed;
        }
    }

    private void updateFrame() {
        switch (currentState) {
            case WALKING_UP:
                currentFrame = walkUp.getKeyFrame(stateTime);
                break;
            case WALKING_DOWN:
                currentFrame = walkDown.getKeyFrame(stateTime);
                break;
            case WALKING_RIGHT:
                currentFrame = walkRight.getKeyFrame(stateTime);
                break;
            case WALKING_LEFT:
                currentFrame = walkLeft.getKeyFrame(stateTime);
                break;
            case IDLE:
                // Keep the first frame of the last direction
                if (previousState == State.WALKING_UP)
                    currentFrame = walkUp.getKeyFrames()[0];
                else if (previousState == State.WALKING_DOWN)
                    currentFrame = walkDown.getKeyFrames()[0];
                else if (previousState == State.WALKING_RIGHT)
                    currentFrame = walkRight.getKeyFrames()[0];
                else if (previousState == State.WALKING_LEFT)
                    currentFrame = walkLeft.getKeyFrames()[0];
                break;
        }
    }

    public void render(SpriteBatch batch) {
        if (currentFrame != null) {
            // Draw scaled down to 16x16 to fit the 400x240 screen properly
            batch.draw(currentFrame, position.x, position.y, 16, 16);
        }
    }

    public void dispose() {
        if (spriteSheet != null) {
            spriteSheet.dispose();
        }
    }
}
