package com.legendofjava.core.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import com.badlogic.gdx.math.Vector2;

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
    private BitmapFont font;

    private Texture spriteSheet;

    // Estado do sistema de warps e cavernas
    private Vector2 returnPosition;
    private String currentCaveId;
    private java.util.Map<String, Boolean> clearedCaves = new java.util.HashMap<>();
    private List<RectangleMapObject> warps = new ArrayList<>();
    private List<RectangleMapObject> caveExits = new ArrayList<>();
    private float spawnX = 0;
    private float spawnY = 0;

    public GameScreen(LegendOfJavaGame game) {
        this.game = game;

        // Carregar mapa e câmera
        map = new TmxMapLoader().load("maps/zelda-map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        quadrantManager = new QuadrantManager();
        quadrantManager.loadFromMap(map);

        // Mantendo o cálculo da média para colocar o player próximo às primeiras
        // colisões
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
        font = new BitmapFont();
        font.getData().setScale(0.5f);

        // Colocar o player no centro
        player = new Player(cx - 8, cy - 8);

        spriteSheet = new Texture("sprites/link-spritesheet.png");

        // Carregar warps e locais de spawn de itens
        if (map.getLayers().get("warps") != null) {
            for (MapObject object : map.getLayers().get("warps").getObjects()) {
                if (object instanceof RectangleMapObject) {
                    RectangleMapObject rectObj = (RectangleMapObject) object;
                    String type = (String) rectObj.getProperties().get("type");
                    if (type == null)
                        type = (String) rectObj.getProperties().get("class");

                    if ("warp".equals(type)) {
                        warps.add(rectObj);
                    } else if ("cave_exit".equals(type)) {
                        caveExits.add(rectObj);
                    }
                }
            }
        }

        if (map.getLayers().get("items") != null) {
            for (MapObject object : map.getLayers().get("items").getObjects()) {
                String type = (String) object.getProperties().get("type");
                if (type == null)
                    type = (String) object.getProperties().get("class");

                if ("wooden_sword".equals(type) || "item_spawn".equals(type)) {
                    Float objX = object.getProperties().get("x", Float.class);
                    Float objY = object.getProperties().get("y", Float.class);
                    if (objX != null && objY != null) {
                        spawnX = objX;
                        spawnY = objY;
                    }
                }
            }
        }
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

                    if (currentCaveId != null) {
                        clearedCaves.put(currentCaveId, true);
                    }
                }
            }
        }

        for (Item item : itemsToRemove) {
            quadrantManager.removeItem(item);
        }
        activeItems.removeAll(itemsToRemove);

        // Lógica de Colisão com Warps (Teletransporte)
        Rectangle pRect = player.getHitbox();
        for (RectangleMapObject warpObj : warps) {
            if (pRect.overlaps(warpObj.getRectangle())) {
                Float destX = warpObj.getProperties().get("destX", Float.class);
                Float destY = warpObj.getProperties().get("destY", Float.class);
                String caveId = warpObj.getProperties().get("caveId", String.class);

                if (destX != null && destY != null) {
                    // Salva a posição antes de entrar
                    returnPosition = new Vector2(player.getPosition().x, player.getPosition().y - 16);
                    currentCaveId = caveId;

                    player.setPosition(destX, destY);

                    // Se houver um ID e a caverna não foi limpa, injeta o item
                    if (caveId != null && !clearedCaves.getOrDefault(caveId, false)) {
                        if ("start_sword_cave".equals(caveId)) {
                            Item woodenSword = new WoodenSword(spawnX, spawnY, spriteSheet);
                            quadrantManager.addItem(woodenSword);
                        }
                    }
                    break;
                }
            }
        }

        // Lógica de Saída da Caverna
        for (RectangleMapObject exitObj : caveExits) {
            if (pRect.overlaps(exitObj.getRectangle())) {
                if (returnPosition != null) {
                    player.setPosition(returnPosition.x, returnPosition.y);
                    currentCaveId = null;
                }
                break;
            }
        }

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
                0);

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
        
        // Desenha as coordenadas do player
        String coords = String.format("X: %.0f Y: %.0f", player.getPosition().x, player.getPosition().y);
        font.draw(batch, coords, camera.position.x - 120, camera.position.y + 80);
        
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
        if (font != null) {
            font.dispose();
        }
    }
}
