package com.legendofjava.core.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Octorok enemy: chases the player when within CHASE_RADIUS,
 * respects map collisions and shoots darts periodically.
 *
 * Sprite region in enemies-spritesheet.png (x≈0, y=11, w=76, h=33):
 *   Frame size  : 16×16 px
 *   Stride      : 19 px  (16px visible + 3px gap)
 *   Row 1 y=11  : down_A | down_B | up_A | up_B
 *   Row 2 y=28  : right_A | right_B | (left = flip of right)
 */
public class Octorok {

    // ── Tuning constants ───────────────────────────────────────────
    private static final float SPEED         = 40f;
    private static final float CHASE_RADIUS  = 96f;
    private static final float DART_COOLDOWN = 2.0f;
    private static final float FRAME_DUR     = 0.25f;

    private static final int SPRITE_W  = 16;
    private static final int SPRITE_H  = 16;
    private static final int STRIDE    = 19;     // pixels between frame starts
    private static final int ROW1_Y    = 11;     // walk down / up row
    private static final int ROW2_Y    = 28;     // walk right row

    // ── Vida ───────────────────────────────────────────────────────
    /** 1,5 corações = 3 HP (1 HP = meio coração) */
    private static final int   MAX_HEALTH           = 3;
    private static final float INVULNERABILITY_DUR  = 0.4f;
    private static final float FLASH_RATE           = 20f; // piscadas/seg

    // ── Explosão ───────────────────────────────────────────────────
    private static final int   PARTICLE_COUNT = 8;
    private static final float PARTICLE_SPEED = 80f;
    private static final float PARTICLE_LIFE  = 0.5f;

    /** Partícula simples para a explosão de morte */
    private static class Particle {
        float x, y, vx, vy, life;
        Particle(float x, float y, float vx, float vy) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.life = PARTICLE_LIFE;
        }
    }

    // ── State ──────────────────────────────────────────────────────
    private final Vector2 position;
    private final float   hitDamage;
    private final Texture spriteSheet;

    private int   health             = MAX_HEALTH;
    private float invulnerabilityTimer = 0f;
    private boolean dead             = false;

    private final List<Particle> particles = new ArrayList<>();
    private float explosionTimer = 0f;
    private static final float EXPLOSION_DURATION = PARTICLE_LIFE;

    private Animation<TextureRegion> walkDown, walkUp, walkRight, walkLeft;
    private Animation<TextureRegion> currentAnim;
    private float stateTime   = 0f;
    private float dartCooldown = 0f;

    private enum Dir { DOWN, UP, RIGHT, LEFT }
    private Dir facing = Dir.DOWN;

    private final List<OctorokDart> darts = new ArrayList<>();

    /** Hitbox reutilizável — mesma instância para evitar alloc e permitir comparação por referência */
    private final Rectangle hitboxRect = new Rectangle();

    // ── Constructor ────────────────────────────────────────────────
    public Octorok(float x, float y, float hitDamage, Texture spriteSheet) {
        this.position    = new Vector2(x, y);
        this.hitDamage   = hitDamage;
        this.spriteSheet = spriteSheet;
        loadAnimations();
        currentAnim = walkDown;
        updateHitbox();
    }

    private void updateHitbox() {
        hitboxRect.set(position.x + HB_OFFSET_X, position.y + HB_OFFSET_Y, HB_W, HB_H);
    }

    private void loadAnimations() {
        // Row 1: down (cols 0-1) and up (cols 2-3)
        TextureRegion dA = new TextureRegion(spriteSheet, 0,          ROW1_Y, SPRITE_W, SPRITE_H);
        TextureRegion dB = new TextureRegion(spriteSheet, STRIDE,     ROW1_Y, SPRITE_W, SPRITE_H);
        TextureRegion uA = new TextureRegion(spriteSheet, STRIDE * 2, ROW1_Y, SPRITE_W, SPRITE_H);
        TextureRegion uB = new TextureRegion(spriteSheet, STRIDE * 3, ROW1_Y, SPRITE_W, SPRITE_H);

        // Row 2: right (cols 0-1); left = horizontally flipped
        TextureRegion rA = new TextureRegion(spriteSheet, 0,      ROW2_Y, SPRITE_W, SPRITE_H);
        TextureRegion rB = new TextureRegion(spriteSheet, STRIDE, ROW2_Y, SPRITE_W, SPRITE_H);
        TextureRegion lA = new TextureRegion(rA); lA.flip(true, false);
        TextureRegion lB = new TextureRegion(rB); lB.flip(true, false);

        walkDown  = loopAnim(FRAME_DUR, dA, dB);
        walkUp    = loopAnim(FRAME_DUR, uA, uB);
        walkRight = loopAnim(FRAME_DUR, rA, rB);
        walkLeft  = loopAnim(FRAME_DUR, lA, lB);
    }

    @SafeVarargs
    private static Animation<TextureRegion> loopAnim(float dur, TextureRegion... frames) {
        Animation<TextureRegion> a = new Animation<>(dur, frames);
        a.setPlayMode(Animation.PlayMode.LOOP);
        return a;
    }

    // ── Update ─────────────────────────────────────────────────────
    public void update(float delta, List<Rectangle> collisions, Vector2 playerPos) {
        if (dead) {
            // Só atualiza as partículas da explosão
            updateParticles(delta);
            return;
        }

        if (invulnerabilityTimer > 0) invulnerabilityTimer -= delta;

        stateTime    += delta;
        dartCooldown -= delta;

        float dist = position.dst(playerPos);

        if (dist <= CHASE_RADIUS) {
            Vector2 dir = new Vector2(playerPos).sub(position).nor();
            moveWithCollision(dir, delta, collisions);
            updateFacing(dir);

            if (dartCooldown <= 0f) {
                spawnDart(dir);
                dartCooldown = DART_COOLDOWN;
            }
        }

        updateAnimFromFacing();
        updateDarts(delta, collisions);
        updateHitbox(); // Sincroniza hitbox com nova posição
    }

    /** Recebe dano da espada do player (1 HP = meio coração). */
    public void takeDamage(int amount) {
        if (dead || invulnerabilityTimer > 0) return;
        health -= amount;
        invulnerabilityTimer = INVULNERABILITY_DUR;
        if (health <= 0) {
            health = 0;
            die();
        }
    }

    private void die() {
        dead = true;
        darts.clear();
        // Spawna partículas em leque ao redor do centro do sprite
        float cx = position.x + SPRITE_W / 2f;
        float cy = position.y + SPRITE_H / 2f;
        for (int i = 0; i < PARTICLE_COUNT; i++) {
            float angle = (float)(i * 2 * Math.PI / PARTICLE_COUNT);
            float vx = MathUtils.cos(angle) * PARTICLE_SPEED;
            float vy = MathUtils.sin(angle) * PARTICLE_SPEED;
            particles.add(new Particle(cx, cy, vx, vy));
        }
        explosionTimer = EXPLOSION_DURATION;
    }

    private void updateParticles(float delta) {
        explosionTimer -= delta;
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.life -= delta;
            p.x += p.vx * delta;
            p.y += p.vy * delta;
            if (p.life <= 0) it.remove();
        }
    }

    public boolean isDead()                  { return dead; }
    public boolean isExplosionFinished()     { return dead && particles.isEmpty(); }
    public int     getHealth()               { return health; }

    private void moveWithCollision(Vector2 dir, float delta, List<Rectangle> cols) {
        float oldX = position.x;
        position.x += dir.x * SPEED * delta;
        if (collidesMap(cols)) position.x = oldX;

        float oldY = position.y;
        position.y += dir.y * SPEED * delta;
        if (collidesMap(cols)) position.y = oldY;
    }

    private void updateFacing(Vector2 dir) {
        if (Math.abs(dir.x) >= Math.abs(dir.y)) {
            facing = dir.x >= 0 ? Dir.RIGHT : Dir.LEFT;
        } else {
            facing = dir.y >= 0 ? Dir.UP : Dir.DOWN;
        }
    }

    private void updateAnimFromFacing() {
        switch (facing) {
            case DOWN:  currentAnim = walkDown;  break;
            case UP:    currentAnim = walkUp;    break;
            case RIGHT: currentAnim = walkRight; break;
            case LEFT:  currentAnim = walkLeft;  break;
        }
    }

    private void spawnDart(Vector2 dir) {
        float cx = position.x + SPRITE_W / 2f;
        float cy = position.y + SPRITE_H / 2f;
        darts.add(new OctorokDart(cx, cy, dir.x, dir.y, spriteSheet));
    }

    private void updateDarts(float delta, List<Rectangle> collisions) {
        Iterator<OctorokDart> it = darts.iterator();
        while (it.hasNext()) {
            OctorokDart dart = it.next();
            dart.update(delta, collisions);
            if (!dart.isActive()) it.remove();
        }
    }

    private boolean collidesMap(List<Rectangle> cols) {
        Rectangle hb = getHitbox();
        for (Rectangle r : cols) if (hb.overlaps(r)) return true;
        return false;
    }

    // ── Render ─────────────────────────────────────────────────────
    public void render(SpriteBatch batch) {
        if (!dead) {
            // Piscar durante invulnerabilidade
            boolean visible = invulnerabilityTimer <= 0
                || (int)(invulnerabilityTimer * FLASH_RATE) % 2 == 0;
            if (visible) {
                TextureRegion frame = currentAnim.getKeyFrame(stateTime);
                batch.draw(frame, position.x, position.y, SPRITE_W, SPRITE_H);
            }
        }
        for (OctorokDart dart : darts) dart.render(batch);
    }

    /**
     * Renderiza as partículas da explosão usando ShapeRenderer.
     * Deve ser chamado entre shapeRenderer.begin() e shapeRenderer.end().
     */
    public void renderExplosion(ShapeRenderer sr) {
        if (particles.isEmpty()) return;
        for (Particle p : particles) {
            float alpha = p.life / PARTICLE_LIFE;
            // Cores quentes: laranja → vermelho à medida que a partícula some
            sr.setColor(1f, alpha * 0.6f, 0f, alpha);
            float size = 3f * alpha + 1f;
            sr.rect(p.x - size / 2f, p.y - size / 2f, size, size);
        }
    }

    // ── Accessors ──────────────────────────────────────────────────
    /** Retorna a hitbox reutilizável (mesma instância, coordenadas atualizadas a cada frame). */
    public Rectangle getHitbox() {
        return hitboxRect;
    }

    /** Tamanho da hitbox sólida usada tanto para colisão com o Player quanto para receber dano. */
    public static final float HB_W = 14f;
    public static final float HB_H = 12f;
    public static final float HB_OFFSET_X = 1f;
    public static final float HB_OFFSET_Y = 1f;

    public List<OctorokDart> getDarts() { return darts; }

    /** Damage in HP units (1 unit = ½ heart). hit=0.5 hearts → 1 HP */
    public int getHitDamageHP() {
        return Math.max(1, Math.round(hitDamage * 2f));
    }

    public Vector2 getPosition() { return position; }
}
