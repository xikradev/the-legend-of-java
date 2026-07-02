package com.legendofjava.core.managers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Overlay de fim de jogo (morte ou vitória).
 *
 * Exibe um fundo preto semi-transparente cobrindo toda a tela,
 * um título centralizado e um botão de ação (Tentar Novamente / Jogar Novamente).
 *
 * O usuário pode clicar no botão ou pressionar Enter/Space para acionar o callback.
 */
public class GameOverOverlay {

    public enum Type {
        GAME_OVER,   // Você morreu!
        GAME_CLEAR   // Parabéns, você concluiu o jogo!
    }

    // Dimensões virtuais (iguais ao HUD)
    private static final float W = 256f;
    private static final float H = 176f;

    // Dimensões do botão
    private static final float BTN_W = 110f;
    private static final float BTN_H = 16f;
    private static final float BTN_X = (W - BTN_W) / 2f;
    private static final float BTN_Y = H / 2f - 30f;

    private final OrthographicCamera camera;
    private final Viewport viewport;
    private final ShapeRenderer shape;
    private final SpriteBatch batch;
    private final BitmapFont titleFont;
    private final BitmapFont btnFont;
    private final GlyphLayout layout;

    private boolean visible = false;
    private Type type = Type.GAME_OVER;

    /** Chamado quando o jogador clica / pressiona Enter no botão. */
    private Runnable onAction;

    // Estado de hover do botão
    private boolean btnHovered = false;

    public GameOverOverlay() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        camera.position.set(W / 2f, H / 2f, 0);
        camera.update();

        shape = new ShapeRenderer();
        batch = new SpriteBatch();

        titleFont = new BitmapFont();
        titleFont.getData().setScale(0.9f);

        btnFont = new BitmapFont();
        btnFont.getData().setScale(0.65f);

        layout = new GlyphLayout();
    }

    public void resize(int screenW, int screenH) {
        viewport.update(screenW, screenH);
        camera.position.set(W / 2f, H / 2f, 0);
        camera.update();
    }

    /** Exibe o overlay com o tipo especificado e o callback de ação. */
    public void show(Type type, Runnable onAction) {
        this.type = type;
        this.onAction = onAction;
        this.visible = true;
    }

    public void hide() {
        this.visible = false;
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Atualiza estado do botão (hover, clique por teclado).
     * Deve ser chamado no update() do GameScreen quando o overlay estiver visível.
     */
    public void update() {
        if (!visible) return;

        // Verifica hover do mouse
        float mx = screenToVirtualX(Gdx.input.getX());
        float my = screenToVirtualY(Gdx.input.getY());
        btnHovered = mx >= BTN_X && mx <= BTN_X + BTN_W && my >= BTN_Y && my <= BTN_Y + BTN_H;

        // Clique do mouse
        if (Gdx.input.justTouched() && btnHovered) {
            triggerAction();
        }

        // Teclado: Enter ou Espaço
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            triggerAction();
        }
    }

    private void triggerAction() {
        if (onAction != null) {
            onAction.run();
        }
    }

    /**
     * Renderiza o overlay. Chamar APÓS tudo o que pertence ao mundo (inclusive HUD).
     */
    public void render() {
        if (!visible) return;

        viewport.apply();
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);

        // ── Fundo semi-transparente ───────────────────────────────────────────
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.72f);
        shape.rect(0, 0, W, H);
        shape.end();

        // ── Textos ────────────────────────────────────────────────────────────
        batch.begin();

        String title = (type == Type.GAME_CLEAR)
                ? "Parabens! Voce concluiu o jogo!"
                : "Voce morreu!";

        // Título — posição centralizada horizontalmente, acima do centro
        titleFont.setColor(type == Type.GAME_CLEAR
                ? new Color(1f, 0.92f, 0.3f, 1f)   // dourado
                : new Color(0.9f, 0.1f, 0.1f, 1f)); // vermelho

        layout.setText(titleFont, title);
        float titleX = (W - layout.width) / 2f;
        float titleY = H / 2f + 20f;
        titleFont.draw(batch, layout, titleX, titleY);

        // Texto do botão
        String btnLabel = (type == Type.GAME_CLEAR)
                ? "  Jogar Novamente  "
                : "  Tentar Novamente  ";

        btnFont.setColor(btnHovered
                ? new Color(0.2f, 0.9f, 0.3f, 1f)   // verde hover
                : Color.WHITE);

        layout.setText(btnFont, btnLabel);
        float labelX = BTN_X + (BTN_W - layout.width) / 2f;
        float labelY = BTN_Y + BTN_H - (BTN_H - layout.height) / 2f;
        btnFont.draw(batch, layout, labelX, labelY);

        batch.end();

        // ── Borda do botão ───────────────────────────────────────────────────
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(btnHovered
                ? new Color(0.2f, 0.9f, 0.3f, 1f)
                : new Color(0.8f, 0.8f, 0.8f, 1f));
        shape.rect(BTN_X, BTN_Y, BTN_W, BTN_H);
        shape.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        if (shape != null) shape.dispose();
        if (batch != null) batch.dispose();
        if (titleFont != null) titleFont.dispose();
        if (btnFont != null) btnFont.dispose();
    }

    // ── Conversão de coordenadas de tela → virtual ───────────────────────────

    private float screenToVirtualX(float screenX) {
        // viewport.getScreenX() / getScreenWidth() define a área renderizada
        float scaleX = W / viewport.getScreenWidth();
        return (screenX - viewport.getScreenX()) * scaleX;
    }

    private float screenToVirtualY(float screenY) {
        // Y da tela cresce para baixo; Y virtual cresce para cima
        float scaleY = H / viewport.getScreenHeight();
        float flippedY = Gdx.graphics.getHeight() - screenY;
        return (flippedY - viewport.getScreenY()) * scaleY;
    }
}
