package com.legendofjava.core.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.legendofjava.core.LegendOfJavaGame;
import com.legendofjava.core.entities.Item;
import com.legendofjava.core.entities.Player;
import com.legendofjava.core.entities.WoodenSword;
import com.legendofjava.core.utils.Constants;
import com.legendofjava.core.world.QuadrantManager;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;

import java.util.ArrayList;
import java.util.List;

public class GameScreen implements Screen {

    private LegendOfJavaGame game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;

    private Player player;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private QuadrantManager quadrantManager;
    private ShapeRenderer shapeRenderer;

    private Texture spriteSheet;

    public GameScreen(LegendOfJavaGame game) {
        this.game = game;

        // Carregar mapa e câmera
        map = new TmxMapLoader().load("maps/zelda-map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        quadrantManager = new QuadrantManager();
        quadrantManager.loadFromMap(map);

        // Mantendo o cálculo da média para colocar o player próximo às primeiras colisões
        float cx = 0;
        float cy = 0;
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

        camera = new OrthographicCamera();
        viewport = new FitViewport(256, 176, camera);
        // Câmera fixa na área central inicial
        camera.position.set(cx, cy, 0);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Colocar o player no centro
        player = new Player(cx - 8, cy - 8);

        // Carregar itens
        spriteSheet = new Texture("sprites/link-spritesheet.png");

        // Adiciona a espada de madeira próxima ao player usando o novo manager
        Item woodenSword = new WoodenSword(cx + 32, cy - 8, spriteSheet);
        quadrantManager.addItem(woodenSword);
    }

    @Override
    public void show() {
        // Carregar mapa, iniciar câmera e player
    }

    @Override
    public void render(float delta) {
        // Obter os itens e colisões do quadrante atual e adjacentes
        List<Rectangle> activeCollisions = quadrantManager.getActiveCollisions(player.getPosition());
        List<Item> activeItems = quadrantManager.getActiveItems(player.getPosition());

        // Atualiza lógica (update)
        player.update(delta, activeCollisions);

        // Checar colisão com itens
        List<Item> itemsToRemove = new ArrayList<>();
        for (Item item : activeItems) {
            if (item.isActive()) {
                item.update(delta);
                // Checa distância
                if (player.getPosition().dst(item.getPosition()) < 12f) {
                    item.onCollect(player);
                    itemsToRemove.add(item);
                }
            }
        }
        
        for (Item item : itemsToRemove) {
            quadrantManager.removeItem(item);
        }
        activeItems.removeAll(itemsToRemove);

        // Lógica de câmera estilo Zelda (por quadrantes)
        float tileWidth = 256f;
        float tileHeight = 176f;
        float border = 1f; // Borda verde de 1 pixel entre as telas
        
        float totalWidth = tileWidth + border;
        float totalHeight = tileHeight + border;

        int coluna = (int) (player.getPosition().x / totalWidth);
        
        // A imagem do mapa é desenhada a partir do topo do mundo (Y = 4000) para baixo.
        // O grid do Tiled inverte o Y, então a linha 0 começa no topo (4000).
        float mapTop = 4000f;
        float distFromTop = mapTop - player.getPosition().y;
        int linha = (int) (distFromTop / totalHeight);

        // Define a posição da câmera para o centro exato desse quadrante
        camera.position.set(
            (coluna * totalWidth) + border + (tileWidth / 2f),
            mapTop - (linha * totalHeight) - border - (tileHeight / 2f),
            0
        );

        camera.update();

        mapRenderer.setView(camera);
        mapRenderer.render();

        batch.setProjectionMatrix(camera.combined);

        // Renderiza mapa e entidades
        batch.begin();
        for (Item item : activeItems) {
            item.render(batch);
        }
        player.render(batch);
        batch.end();

        // Desenhar os Hitboxes para debug visual
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Desenha as colisões ativas em Vermelho
        shapeRenderer.setColor(Color.RED);
        for (Rectangle rect : activeCollisions) {
            shapeRenderer.rect(rect.x, rect.y, rect.width, rect.height);
        }

        // Desenha o hitbox do Player em Azul
        shapeRenderer.setColor(Color.BLUE);
        Rectangle pRect = player.getHitbox();
        shapeRenderer.rect(pRect.x, pRect.y, pRect.width, pRect.height);

        shapeRenderer.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        player.dispose();
        if (spriteSheet != null) {
            spriteSheet.dispose();
        }
        if (map != null) {
            map.dispose();
        }
        if (mapRenderer != null) {
            mapRenderer.dispose();
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}
