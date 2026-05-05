package com.legendofjava.core.managers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

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

    public void resize(int width, int height) {
        viewport.update(width, height);
    }

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
