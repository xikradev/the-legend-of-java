package com.legendofjava.core.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

/**
 * Item coração: dropa ao matar inimigos e recupera 1 coração completo (2 HP) do player.
 *
 * Sprite: items-zelda.png — região (0, 0, 8, 8) = coração vermelho cheio.
 * Animação: flutua levemente no eixo Y para destacar o item no mundo.
 */
public class HeartItem extends Item {

    /** Quantidade de HP recuperada ao coletar (1 coração = 2 HP). */
    private static final int HEAL_AMOUNT = 2;

    /** Largura/altura do sprite do coração na spritesheet. */
    public static final int SPRITE_W = 8;
    public static final int SPRITE_H = 8;

    // ── Animação de flutuação ──────────────────────────────────────
    private static final float FLOAT_SPEED    = 2.5f;  // radianos por segundo
    private static final float FLOAT_AMPLITUDE = 2.0f; // pixels de deslocamento

    private float floatTimer = 0f;
    private final float baseY;

    /**
     * Cria um HeartItem na posição (x, y) usando a spritesheet de itens.
     *
     * @param x           posição X no mundo
     * @param y           posição Y no mundo
     * @param itemsSheet  textura "sprites/items-zelda.png"
     */
    public HeartItem(float x, float y, Texture itemsSheet) {
        super(x, y, new TextureRegion(itemsSheet, 0, 0, SPRITE_W, SPRITE_H));
        this.baseY = y;
    }

    @Override
    public void update(float delta) {
        floatTimer += delta;
        // Oscila o sprite no eixo Y
        position.y = baseY + (float) Math.sin(floatTimer * FLOAT_SPEED) * FLOAT_AMPLITUDE;
    }

    @Override
    public void render(SpriteBatch batch) {
        if (active && sprite != null) {
            batch.draw(sprite, position.x, position.y, SPRITE_W, SPRITE_H);
        }
    }

    /**
     * Cura o player em 2 HP (1 coração completo) e desativa o item.
     */
    @Override
    public void onCollect(Player player) {
        int newHealth = Math.min(player.getMaxHealth(), player.getHealth() + HEAL_AMOUNT);
        player.setHealth(newHealth);
        setActive(false);
    }
}
