package com.legendofjava.core.managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.legendofjava.core.hud.HudRenderer;

public class CameraManager {
    private OrthographicCamera camera;
    private Viewport viewport;
    private Vector2 initialSpawn;

    public CameraManager(TiledMap map) {
        camera = new OrthographicCamera();
        viewport = new FitViewport(256, 176, camera);

        float cx = -1;
        float cy = -1;

        // Try to find first_spawn point
        if (map.getLayers().get("warps") != null) {
            for (MapObject object : map.getLayers().get("warps").getObjects()) {
                String type = (String) object.getProperties().get("type");
                if (type == null) {
                    type = (String) object.getProperties().get("class");
                }
                if ("first_spawn".equals(type)) {
                    Float x = object.getProperties().get("x", Float.class);
                    Float y = object.getProperties().get("y", Float.class);
                    if (x != null && y != null) {
                        cx = x;
                        cy = y;
                        break;
                    }
                }
            }
        }

        // Fallback: Cálculo da média para colocar o player/câmera próximo às primeiras colisões
        if (cx == -1 && cy == -1) {
            cx = 0;
            cy = 0;
            int count = 0;
            if (map.getLayers().get("colisoes") != null) {
                for (MapObject object : map.getLayers().get("colisoes").getObjects()) {
                    if (object instanceof RectangleMapObject) {
                        Rectangle r = ((RectangleMapObject) object).getRectangle();
                        cx += r.x + r.width / 2f;
                        cy += r.y + r.height / 2f;
                        count++;
                    }
                }
            }

            if (count > 0) {
                cx /= count;
                cy /= count;
            } else {
                cx = 256 / 2f;
                cy = 176 / 2f;
            }
        }

        initialSpawn = new Vector2(cx, cy);

        // Câmera fixa na área central inicial
        camera.position.set(cx, cy, 0);
        camera.update();
    }

    public void update(Vector2 playerPosition) {
        // Lógica de câmera estilo Zelda (por quadrantes)
        float tileWidth = 256f;
        float tileHeight = 176f;
        float border = 1f; // Borda verde de 1 pixel entre as telas

        float totalWidth = tileWidth + border;
        float totalHeight = tileHeight + border;

        int coluna = (int) (playerPosition.x / totalWidth);

        // A imagem do mapa é desenhada a partir do topo do mundo (Y = 4000) para baixo.
        // O grid do Tiled inverte o Y, então a linha 0 começa no topo (4000).
        float mapTop = 4000f;
        float distFromTop = mapTop - playerPosition.y;
        int linha = (int) (distFromTop / totalHeight);

        // Define a posição da câmera para o centro exato desse quadrante
        camera.position.set(
                (coluna * totalWidth) + border + (tileWidth / 2f),
                mapTop - (linha * totalHeight) - border - (tileHeight / 2f),
                0);

        camera.update();
    }

    /**
     * Atualiza o viewport do jogo levando em conta a área do HUD acima.
     * O jogo (256x176) e o HUD (256x56) ocupam juntos 256x232 pixels virtuais.
     * Escalonamos mantendo a proporção e centralizamos na janela.
     *
     * @param width  largura real da janela
     * @param height altura real da janela
     */
    public void resize(int width, int height) {
        float totalVirtualW = HudRenderer.HUD_WIDTH;   // 256
        float totalVirtualH = HudRenderer.GAME_HEIGHT + HudRenderer.HUD_HEIGHT; // 176 + 56 = 232

        // Calcular escala que mantém a proporção total (256x232) dentro da janela
        float scaleX = width  / totalVirtualW;
        float scaleY = height / totalVirtualH;
        float scale  = Math.min(scaleX, scaleY);

        int gamePixelW = (int) (HudRenderer.GAME_WIDTH  * scale);
        int gamePixelH = (int) (HudRenderer.GAME_HEIGHT * scale);
        int hudPixelH  = (int) (HudRenderer.HUD_HEIGHT  * scale);

        // Centralizar horizontalmente
        int offsetX = (width - gamePixelW) / 2;
        // O jogo fica na base (Y=0 até gamePixelH); o HUD ficará acima
        int gameOffsetY = (height - gamePixelH - hudPixelH) / 2;

        viewport.update(gamePixelW, gamePixelH, false);
        viewport.setScreenBounds(offsetX, gameOffsetY, gamePixelW, gamePixelH);
        camera.position.set(camera.position.x, camera.position.y, 0);
        camera.update();

        // Salva para uso externo (HudRenderer.resize)
        this.lastScale = scale;
        this.lastOffsetX = offsetX;
        this.lastGameOffsetY = gameOffsetY;
        this.lastGamePixelW = gamePixelW;
        this.lastGamePixelH = gamePixelH;
    }

    // Dados do último resize, usados pelo HudRenderer
    private float lastScale = 1f;
    private int lastOffsetX = 0;
    private int lastGameOffsetY = 0;
    private int lastGamePixelW = 256;
    private int lastGamePixelH = 176;

    public float getLastScale()       { return lastScale; }
    public int   getLastOffsetX()     { return lastOffsetX; }
    public int   getLastGameOffsetY() { return lastGameOffsetY; }
    public int   getLastGamePixelW()  { return lastGamePixelW; }
    public int   getLastGamePixelH()  { return lastGamePixelH; }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public Viewport getViewport() {
        return viewport;
    }

    public Vector2 getInitialSpawn() {
        return initialSpawn;
    }
}
