package com.legendofjava.core.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import java.util.List;

public class Player {
    private Vector2 position;
    private Vector2 velocity;
    private final float speed = 100f; // pixels per second

    // Sistema de vida: 10 HP = 5 corações (cada coração = 2 HP)
    private int health    = 10;
    private int maxHealth = 10;
    
    private float invulnerabilityTimer = 0f;
    private static final float INVULNERABILITY_DURATION = 1.0f; // 1 segundo

    private boolean hasSword = false;

    private Texture spriteSheet;

    private Animation<TextureRegion> walkDown;
    private Animation<TextureRegion> walkRight;
    private Animation<TextureRegion> walkUp;
    private Animation<TextureRegion> walkLeft;

    private Animation<TextureRegion> attackDown;
    private Animation<TextureRegion> attackRight;
    private Animation<TextureRegion> attackUp;
    private Animation<TextureRegion> attackLeft;

    private TextureRegion pickUpFrame;
    private TextureRegion swordPickupSprite;

    private float stateTime;
    private State lastDirection = State.WALKING_DOWN;

    public enum State {
        IDLE, WALKING_UP, WALKING_DOWN, WALKING_LEFT, WALKING_RIGHT,
        ATTACKING_UP, ATTACKING_DOWN, ATTACKING_LEFT, ATTACKING_RIGHT,
        PICKING_UP
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

        // --- ATTACK ANIMATIONS ---
        TextureRegion atkDown1 = new TextureRegion(spriteSheet, 0, 47, 16, 27);
        TextureRegion atkDown2 = new TextureRegion(spriteSheet, 17, 47, 16, 27);
        TextureRegion atkDown3 = new TextureRegion(spriteSheet, 34, 47, 16, 27);
        TextureRegion atkDown4 = new TextureRegion(spriteSheet, 51, 47, 16, 27);

        TextureRegion atkRight1 = new TextureRegion(spriteSheet, 0, 77, 16, 16);
        TextureRegion atkRight2 = new TextureRegion(spriteSheet, 17, 77, 28, 16);
        TextureRegion atkRight3 = new TextureRegion(spriteSheet, 46, 77, 24, 16);
        TextureRegion atkRight4 = new TextureRegion(spriteSheet, 70, 77, 20, 16);

        TextureRegion atkUp1 = new TextureRegion(spriteSheet, 0, 97, 16, 28);
        TextureRegion atkUp2 = new TextureRegion(spriteSheet, 17, 97, 16, 28);
        TextureRegion atkUp3 = new TextureRegion(spriteSheet, 34, 97, 16, 28);
        TextureRegion atkUp4 = new TextureRegion(spriteSheet, 51, 97, 16, 28);

        TextureRegion atkLeft1 = new TextureRegion(atkRight1); atkLeft1.flip(true, false);
        TextureRegion atkLeft2 = new TextureRegion(atkRight2); atkLeft2.flip(true, false);
        TextureRegion atkLeft3 = new TextureRegion(atkRight3); atkLeft3.flip(true, false);
        TextureRegion atkLeft4 = new TextureRegion(atkRight4); atkLeft4.flip(true, false);

        float attackDuration = 0.08f;
        attackDown = new Animation<>(attackDuration, atkDown1, atkDown2, atkDown3, atkDown4);
        attackRight = new Animation<>(attackDuration, atkRight1, atkRight2, atkRight3, atkRight4);
        attackUp = new Animation<>(attackDuration, atkUp1, atkUp2, atkUp3, atkUp4);
        attackLeft = new Animation<>(attackDuration, atkLeft1, atkLeft2, atkLeft3, atkLeft4);

        pickUpFrame = new TextureRegion(spriteSheet, 214, 11, 16, 16);
        swordPickupSprite = new TextureRegion(spriteSheet, 1, 154, 7, 16);

        currentFrame = down1; // Default looking down
    }

    public void update(float delta, List<Rectangle> collisionRects) {
        if (invulnerabilityTimer > 0) {
            invulnerabilityTimer -= delta;
        }

        if (currentState == State.PICKING_UP) {
            velocity.set(0, 0);
        } else {
            handleInput();
        }

        if (!isAttacking() && currentState != State.PICKING_UP) {
            float oldX = position.x;
            float oldY = position.y;

            position.x += velocity.x * delta;
            if (collides(collisionRects)) {
                position.x = oldX;
            }

            position.y += velocity.y * delta;
            if (collides(collisionRects)) {
                position.y = oldY;
            }

            if ((Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || Gdx.input.isKeyJustPressed(Input.Keys.Z)) && hasSword) {
                startAttack();
            } else if (velocity.x > 0) {
                currentState = State.WALKING_RIGHT;
                lastDirection = State.WALKING_RIGHT;
            } else if (velocity.x < 0) {
                currentState = State.WALKING_LEFT;
                lastDirection = State.WALKING_LEFT;
            } else if (velocity.y > 0) {
                currentState = State.WALKING_UP;
                lastDirection = State.WALKING_UP;
            } else if (velocity.y < 0) {
                currentState = State.WALKING_DOWN;
                lastDirection = State.WALKING_DOWN;
            } else {
                currentState = State.IDLE;
            }
        }

        if (currentState != previousState) {
            stateTime = 0;
        } else {
            stateTime += delta;
        }

        if (isAttacking()) {
            Animation<TextureRegion> anim = getAttackAnimation();
            if (anim.isAnimationFinished(stateTime)) {
                currentState = State.IDLE;
            }
        } else if (currentState == State.PICKING_UP) {
            if (stateTime > 1.5f) { // 1.5 segundos de animação
                currentState = State.IDLE;
            }
        }

        previousState = currentState;
        updateFrame();
    }

    private boolean isAttacking() {
        return currentState == State.ATTACKING_UP || currentState == State.ATTACKING_DOWN || 
               currentState == State.ATTACKING_LEFT || currentState == State.ATTACKING_RIGHT;
    }

    public Rectangle getHitbox() {
        // Hitbox menor, cobrindo apenas a parte dos pés do personagem
        // para dar o efeito pseudo-3D comum em jogos do Zelda
        return new Rectangle(position.x + 2, position.y, 12, 8);
    }

    /**
     * Retorna o hitbox ativo da espada durante o ataque, ou null se não está atacando.
     * O hitbox é projetado à frente do player na direção do ataque.
     */
    public Rectangle getSwordHitbox() {
        if (!isAttacking()) return null;
        float cx = position.x + 8; // centro horizontal do player
        float cy = position.y + 8; // centro vertical do player
        float reach = 18f;         // alcance da espada em pixels
        float half  = 8f;          // metade da largura da área de acerto
        switch (currentState) {
            case ATTACKING_UP:
                return new Rectangle(cx - half, cy + 2f, half * 2, reach);
            case ATTACKING_DOWN:
                return new Rectangle(cx - half, cy - reach - 2f, half * 2, reach);
            case ATTACKING_RIGHT:
                return new Rectangle(cx + 2f, cy - half, reach, half * 2);
            case ATTACKING_LEFT:
                return new Rectangle(cx - reach - 2f, cy - half, reach, half * 2);
            default:
                return null;
        }
    }

    private boolean collides(List<Rectangle> collisionRects) {
        Rectangle playerRect = getHitbox();
        for (Rectangle rect : collisionRects) {
            if (playerRect.overlaps(rect)) {
                return true;
            }
        }
        return false;
    }

    private void startAttack() {
        if (lastDirection == State.WALKING_UP) {
            currentState = State.ATTACKING_UP;
        } else if (lastDirection == State.WALKING_DOWN) {
            currentState = State.ATTACKING_DOWN;
        } else if (lastDirection == State.WALKING_RIGHT) {
            currentState = State.ATTACKING_RIGHT;
        } else if (lastDirection == State.WALKING_LEFT) {
            currentState = State.ATTACKING_LEFT;
        }
        stateTime = 0;
    }

    private Animation<TextureRegion> getAttackAnimation() {
        switch (currentState) {
            case ATTACKING_UP: return attackUp;
            case ATTACKING_DOWN: return attackDown;
            case ATTACKING_RIGHT: return attackRight;
            case ATTACKING_LEFT: return attackLeft;
            default: return attackDown;
        }
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
            case ATTACKING_UP:
                currentFrame = attackUp.getKeyFrame(stateTime);
                break;
            case ATTACKING_DOWN:
                currentFrame = attackDown.getKeyFrame(stateTime);
                break;
            case ATTACKING_RIGHT:
                currentFrame = attackRight.getKeyFrame(stateTime);
                break;
            case ATTACKING_LEFT:
                currentFrame = attackLeft.getKeyFrame(stateTime);
                break;
            case PICKING_UP:
                currentFrame = pickUpFrame;
                break;
            case IDLE:
                if (lastDirection == State.WALKING_UP)
                    currentFrame = walkUp.getKeyFrames()[0];
                else if (lastDirection == State.WALKING_DOWN)
                    currentFrame = walkDown.getKeyFrames()[0];
                else if (lastDirection == State.WALKING_RIGHT)
                    currentFrame = walkRight.getKeyFrames()[0];
                else if (lastDirection == State.WALKING_LEFT)
                    currentFrame = walkLeft.getKeyFrames()[0];
                break;
        }
    }

    public void render(SpriteBatch batch) {
        if (currentFrame != null) {
            // Efeito de piscar quando invulnerável
            if (invulnerabilityTimer > 0) {
                if ((int)(invulnerabilityTimer * 15) % 2 == 0) {
                    return; // Pula o desenho para criar o efeito de piscar
                }
            }

            float drawX = position.x;
            float drawY = position.y;
            
            if (currentState == State.ATTACKING_LEFT) {
                drawX -= (currentFrame.getRegionWidth() - 16);
            }
            if (currentState == State.ATTACKING_DOWN) {
                drawY -= (currentFrame.getRegionHeight() - 16);
            }
            
            batch.draw(currentFrame, drawX, drawY, currentFrame.getRegionWidth(), currentFrame.getRegionHeight());

            if (currentState == State.PICKING_UP && swordPickupSprite != null) {
                float swordX = position.x + (16 - swordPickupSprite.getRegionWidth()) / 2f;
                float swordY = position.y + 16;
                batch.draw(swordPickupSprite, swordX, swordY, swordPickupSprite.getRegionWidth(), swordPickupSprite.getRegionHeight());
            }
        }
    }

    public void dispose() {
        if (spriteSheet != null) {
            spriteSheet.dispose();
        }
    }

    public State getCurrentState() {
        return currentState;
    }

    public boolean hasSword() {
        return hasSword;
    }

    public void giveSword() {
        this.hasSword = true;
        this.currentState = State.PICKING_UP;
        this.stateTime = 0;
    }

    // -------- Vida --------

    /** Reduz a vida do player. Cada dano = 1 ponto (meio coração). */
    public void takeDamage(int amount) {
        if (invulnerabilityTimer > 0) return;
        health = Math.max(0, health - amount);
        invulnerabilityTimer = INVULNERABILITY_DURATION;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public boolean isDead() {
        return health <= 0;
    }

    /** Define a vida do player diretamente (usado por itens de cura). */
    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(maxHealth, health));
    }

    // -------- Posição --------

    public Vector2 getPosition() {
        return position;
    }

    public void setPosition(float x, float y) {
        position.set(x, y);
    }
}
