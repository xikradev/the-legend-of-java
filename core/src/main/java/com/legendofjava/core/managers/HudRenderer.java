package com.legendofjava.core.managers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.legendofjava.core.entities.Player;

/**
 * HudRenderer
 *
 * Renderiza a faixa preta no topo da tela com 5 corações pixel art.
 *
 * Layout do coração (9×8 pixels, escala 2×):
 *
 *   . X X . . . X X .
 *   X X X X . X X X X
 *   X X X X X X X X X
 *   X X X X X X X X X
 *   . X X X X X X X .
 *   . . X X X X X . .
 *   . . . X X X . . .
 *   . . . . X . . . .
 *
 * Coração cheio  = vermelho vibrante (#E8212F)
 * Meio coração   = lado esquerdo cheio, lado direito cinza
 * Coração vazio  = cinza escuro (#444444)
 */
public class HudRenderer {

    // Resolução virtual da HUD (mesma do viewport principal)
    private static final float HUD_WIDTH  = 256f;
    private static final float HUD_HEIGHT = 176f;

    // Altura da faixa preta do HUD
    public static final float HUD_BAR_HEIGHT = 16f;

    // Tamanho de cada pixel do coração (escala pixel-art)
    private static final float PX = 1.2f;

    // Dimensões do coração desenhado (9 colunas × 8 linhas de pixels)
    private static final float HEART_W = 9 * PX;
    private static final float HEART_H = 8 * PX;

    // Espaço entre corações
    private static final float HEART_GAP = 2f;

    // Número de corações
    private static final int NUM_HEARTS = 5;

    // Cores
    private static final Color COLOR_BAR        = new Color(0f, 0f, 0f, 1f);
    private static final Color COLOR_HEART_FULL  = new Color(0.91f, 0.13f, 0.18f, 1f);   // #E8212F
    private static final Color COLOR_HEART_EMPTY = new Color(0.27f, 0.27f, 0.27f, 1f);   // #444444
    private static final Color COLOR_HEART_SHINE = new Color(1f,   0.55f, 0.60f, 1f);    // brilho topo esquerdo

    private OrthographicCamera hudCamera;
    private Viewport            hudViewport;
    private ShapeRenderer       shape;

    public HudRenderer() {
        hudCamera = new OrthographicCamera();
        hudViewport = new FitViewport(HUD_WIDTH, HUD_HEIGHT, hudCamera);
        hudCamera.position.set(HUD_WIDTH / 2f, HUD_HEIGHT / 2f, 0);
        hudCamera.update();

        shape = new ShapeRenderer();
    }

    /**
     * Deve ser chamado em GameScreen.resize().
     */
    public void resize(int screenW, int screenH) {
        hudViewport.update(screenW, screenH);
        hudCamera.position.set(HUD_WIDTH / 2f, HUD_HEIGHT / 2f, 0);
        hudCamera.update();
    }

    /**
     * Renderiza a HUD. Chamar APÓS o batch.end() do mundo.
     *
     * @param player  referência ao player para ler health
     */
    public void render(Player player) {
        hudViewport.apply();
        shape.setProjectionMatrix(hudCamera.combined);

        int hp = player.getHealth(); // 0..10

        // ── Faixa preta ──────────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(COLOR_BAR);
        shape.rect(0, HUD_HEIGHT - HUD_BAR_HEIGHT, HUD_WIDTH, HUD_BAR_HEIGHT);
        shape.end();

        // ── Corações ─────────────────────────────────────────────────────────
        // Total de largura dos 5 corações com espaçamentos
        float totalW = NUM_HEARTS * HEART_W + (NUM_HEARTS - 1) * HEART_GAP;

        // Canto direito da faixa, com margem de 3px
        float startX = HUD_WIDTH - totalW - 3f;
        float startY = HUD_HEIGHT - HUD_BAR_HEIGHT + (HUD_BAR_HEIGHT - HEART_H) / 2f;

        shape.begin(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < NUM_HEARTS; i++) {
            float hx = startX + i * (HEART_W + HEART_GAP);
            float hy = startY;

            // Quantos HP "cabem" neste coração? 2 por coração.
            int hpForThisHeart = Math.max(0, Math.min(2, hp - i * 2));

            if (hpForThisHeart == 2) {
                // Coração cheio
                drawHeart(hx, hy, COLOR_HEART_FULL, COLOR_HEART_SHINE, HeartFill.FULL);
            } else if (hpForThisHeart == 1) {
                // Meio coração: esquerda cheia, direita vazia
                drawHeart(hx, hy, COLOR_HEART_FULL,  COLOR_HEART_SHINE, HeartFill.HALF);
                drawHeart(hx, hy, COLOR_HEART_EMPTY, null,               HeartFill.RIGHT_ONLY);
            } else {
                // Coração vazio
                drawHeart(hx, hy, COLOR_HEART_EMPTY, null, HeartFill.FULL);
            }
        }

        shape.end();
    }

    private enum HeartFill { FULL, HALF, RIGHT_ONLY }

    /**
     * Desenha o coração pixel a pixel conforme a máscara de 9×8.
     *
     * A máscara define quais colunas (0-8) são parte do coração em cada linha (0-7, topo→base).
     * Para HALF: só pinta colunas 0..4 (lado esquerdo).
     * Para RIGHT_ONLY: só pinta colunas 5..8 (lado direito).
     */
    private void drawHeart(float ox, float oy, Color color, Color shineColor, HeartFill fill) {
        shape.setColor(color);

        // Máscara do coração: cada int[] é uma linha (row 0 = topo).
        // 1 = pixel pintado, 0 = transparente.
        int[][] mask = {
            {0,1,1,0,0,0,1,1,0},
            {1,1,1,1,0,1,1,1,1},
            {1,1,1,1,1,1,1,1,1},
            {1,1,1,1,1,1,1,1,1},
            {0,1,1,1,1,1,1,1,0},
            {0,0,1,1,1,1,1,0,0},
            {0,0,0,1,1,1,0,0,0},
            {0,0,0,0,1,0,0,0,0},
        };

        int rows = mask.length;       // 8
        int cols = mask[0].length;    // 9

        for (int row = 0; row < rows; row++) {
            // Coração é desenhado de cima (row=0) para baixo, mas coordenadas Y sobem.
            float py = oy + (rows - 1 - row) * PX;

            for (int col = 0; col < cols; col++) {
                if (mask[row][col] == 0) continue;

                // Filtra metades
                if (fill == HeartFill.HALF       && col >= 5) continue;
                if (fill == HeartFill.RIGHT_ONLY && col < 5)  continue;

                float px = ox + col * PX;

                // Pixel de brilho no canto superior esquerdo (row=0/1, col=1/2)
                if (shineColor != null && row <= 1 && col >= 1 && col <= 2) {
                    shape.setColor(shineColor);
                    shape.rect(px, py, PX, PX);
                    shape.setColor(color);
                } else {
                    shape.rect(px, py, PX, PX);
                }
            }
        }
    }

    public void dispose() {
        if (shape != null) shape.dispose();
    }
}
