package com.legendofjava.core.hud;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.legendofjava.core.entities.Player;
import com.legendofjava.core.managers.CameraManager;

/**
 * Renderiza o HUD estilo Zelda NES acima da viewport do jogo.
 *
 * Usa uma região do hub-spritesheet.png que contém o HUD completo do Zelda
 * (minimapa, slots B/A, corações), renderizando-o numa viewport separada
 * posicionada exatamente acima da área do jogo.
 *
 * Dimensões virtuais:
 *   - HUD:  256 x 56 pixels (virtual)
 *   - Jogo: 256 x 176 pixels (virtual)
 *   - Total: 256 x 232 pixels (virtual)
 */
public class HudRenderer implements Disposable {

    /** Largura virtual do HUD e do viewport do jogo (devem ser iguais). */
    public static final float HUD_WIDTH   = 256f;
    /** Altura virtual da faixa do HUD. */
    public static final float HUD_HEIGHT  = 56f;
    /** Largura virtual do viewport do jogo. */
    public static final float GAME_WIDTH  = 256f;
    /** Altura virtual do viewport do jogo. */
    public static final float GAME_HEIGHT = 176f;

    // ---------------------------------------------------------------
    // Mapeamento do hub-spritesheet.png (752x208 pixels, RGBA)
    //
    // O HUD completo do Zelda NES está na região:
    //   x=264, y=11, w=250, h=54
    //
    // Elementos individuais para overlay/customização:
    //   Corações (8x8 cada, fundo preto, y=27..33 no sheet):
    //     Coração cheio:  x=451..458, y=24  (8x8)
    //     Coração vazio:  x=483..490, y=24  (8x8) — área sem pixels vermelhos
    //     Nota: no sheet original os corações são semi-preenchidos ao longo do y=24..35
    //
    // Para a implementação inicial, renderizamos o HUD completo como um bloco.
    // ---------------------------------------------------------------

    private final Texture hudSheet;
    private final OrthographicCamera hudCamera;
    private final Viewport hudViewport;

    /**
     * Região do HUD completo no spritesheet.
     * Inclui minimapa, slots B/A e corações com fundo preto.
     */
    private final TextureRegion hudRegion;

    /**
     * @param hudSheet  Textura do hub-spritesheet.png (gerenciada externamente)
     */
    public HudRenderer(Texture hudSheet) {
        this.hudSheet = hudSheet;

        hudCamera   = new OrthographicCamera();
        hudViewport = new FitViewport(HUD_WIDTH, HUD_HEIGHT, hudCamera);

        // Região exata do HUD no sheet (analisada via pixel inspection):
        //   x=264, y=11, w=250, h=54
        // Renderizamos esticado para preencher os 256x56 virtuais do HUD.
        hudRegion = new TextureRegion(hudSheet, 264, 11, 250, 54);
    }

    /**
     * Deve ser chamado no resize() do GameScreen, APÓS cameraManager.resize().
     * Usa os dados calculados pelo CameraManager para posicionar o HUD exatamente
     * acima do viewport do jogo.
     *
     * @param cam  CameraManager já atualizado com o novo tamanho de janela
     */
    public void resize(CameraManager cam) {
        int hudPixelW = cam.getLastGamePixelW();
        int hudPixelH = (int) (HUD_HEIGHT * cam.getLastScale());

        // HUD fica logo acima do viewport do jogo
        int hudOffsetX = cam.getLastOffsetX();
        int hudOffsetY = cam.getLastGameOffsetY() + cam.getLastGamePixelH();

        hudViewport.update(hudPixelW, hudPixelH, false);
        hudViewport.setScreenBounds(hudOffsetX, hudOffsetY, hudPixelW, hudPixelH);
        hudCamera.position.set(HUD_WIDTH / 2f, HUD_HEIGHT / 2f, 0);
        hudCamera.update();
    }

    /**
     * Renderiza o HUD. Deve ser chamado APÓS o draw do mundo, com batch NÃO iniciado.
     *
     * @param batch   SpriteBatch compartilhado com o GameScreen
     * @param player  Player atual (para informações de estado como espada/vida)
     */
    public void render(SpriteBatch batch, Player player) {
        hudViewport.apply();
        batch.setProjectionMatrix(hudCamera.combined);

        batch.begin();
        drawHud(batch, player);
        batch.end();
    }

    private void drawHud(SpriteBatch batch, Player player) {
        // Renderiza o HUD completo do spritesheet, esticando para 256x56 virtuais.
        // O HUD original tem 250x54, escalado para 256x56 mantendo a proporção visual.
        batch.draw(hudRegion, 0f, 0f, HUD_WIDTH, HUD_HEIGHT);
    }

    @Override
    public void dispose() {
        // hudSheet é gerenciado pelo GameScreen, não fazemos dispose aqui.
    }
}
