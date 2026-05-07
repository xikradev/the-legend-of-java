package com.legendofjava.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.legendofjava.core.LegendOfJavaGame;
import com.legendofjava.core.entities.Item;
import com.legendofjava.core.entities.Player;
import com.legendofjava.core.entities.HostNPC;
import com.legendofjava.core.entities.Fire;
import com.legendofjava.core.hud.HudRenderer;
import com.legendofjava.core.managers.CameraManager;
import com.legendofjava.core.world.QuadrantManager;
import com.legendofjava.core.world.WarpManager;

import java.util.ArrayList;
import java.util.List;

public class GameScreen implements Screen {

    private LegendOfJavaGame game;
    private SpriteBatch batch;

    private Player player;

    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    
    private QuadrantManager quadrantManager;
    private CameraManager cameraManager;
    private WarpManager warpManager;
    
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture spriteSheet;
    private Texture npcSpriteSheet;
    private Texture hudSpriteSheet;
    private HudRenderer hudRenderer;

    public GameScreen(LegendOfJavaGame game) {
        this.game = game;
        
        initMap();
        initManagers();
        initPlayer();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(0.5f);

        // Aciona o resize inicial para configurar os bounds dos viewports
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void initMap() {
        map = new TmxMapLoader().load("maps/zelda-map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        spriteSheet = new Texture("sprites/link-spritesheet.png");
        npcSpriteSheet = new Texture("sprites/npc-spritesheet.png");
        hudSpriteSheet = new Texture("sprites/hub-spritesheet.png");
        hudRenderer = new HudRenderer(hudSpriteSheet);
    }
    
    private void initManagers() {
        quadrantManager = new QuadrantManager();
        quadrantManager.loadFromMap(map, npcSpriteSheet);
        
        cameraManager = new CameraManager(map);
        warpManager = new WarpManager(map, spriteSheet);
    }
    
    private void initPlayer() {
        Vector2 spawn = cameraManager.getInitialSpawn();
        player = new Player(spawn.x - 8, spawn.y - 8);
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        update(delta);
        draw();
    }
    
    private void update(float delta) {
        // Obter as colisões do quadrante atual e adjacentes
        List<Rectangle> activeCollisions = quadrantManager.getActiveCollisions(player.getPosition());
        
        // Atualiza lógica
        player.update(delta, activeCollisions);

        // Atualiza os fogos ativos no quadrante
        List<Fire> activeFires = quadrantManager.getActiveFires(player.getPosition());
        for (Fire fire : activeFires) {
            fire.update(delta);
        }

        processItems(delta);

        warpManager.checkTeleports(player, quadrantManager);
        
        cameraManager.update(player.getPosition());
    }
    
    private void processItems(float delta) {
        List<Item> activeItems = quadrantManager.getActiveItems(player.getPosition());
        List<Item> itemsToRemove = new ArrayList<>();
        
        for (Item item : activeItems) {
            if (item.isActive()) {
                item.update(delta);
                // Checa distância
                if (player.getPosition().dst(item.getPosition()) < 12f) {
                    item.onCollect(player);
                    itemsToRemove.add(item);
                    
                    // Sinaliza para o warpManager que pegou item e pode limpar a caverna
                    warpManager.markCaveCleared();
                }
            }
        }

        for (Item item : itemsToRemove) {
            quadrantManager.removeItem(item);
        }
        // activeItems pode não precisar de .removeAll dependendo de como quadrantManager a devolve
        // mas remover localmente evita bugs de re-processamento se usado na mesma frame
        activeItems.removeAll(itemsToRemove);
    }

    private void draw() {
        // Obter os itens ativos para desenhar
        List<Item> activeItems = quadrantManager.getActiveItems(player.getPosition());
        List<HostNPC> activeNpcs = quadrantManager.getActiveHostNpcs(player.getPosition());
        List<Fire> activeFires = quadrantManager.getActiveFires(player.getPosition());
        List<Rectangle> activeCollisions = quadrantManager.getActiveCollisions(player.getPosition());
        
        mapRenderer.setView(cameraManager.getCamera());
        mapRenderer.render();

        batch.setProjectionMatrix(cameraManager.getCamera().combined);

        // Renderiza entidades
        batch.begin();
        for (Item item : activeItems) {
            item.render(batch);
        }
        for (HostNPC npc : activeNpcs) {
            npc.render(batch);
        }
        for (Fire fire : activeFires) {
            fire.render(batch);
        }
        player.render(batch);
        
        // Desenha as coordenadas do player
        String coords = String.format("X: %.0f Y: %.0f", player.getPosition().x, player.getPosition().y);
        font.draw(batch, coords, cameraManager.getCamera().position.x - 120, cameraManager.getCamera().position.y + 80);
        
        batch.end();

        // Renderiza o HUD acima do viewport do jogo
        hudRenderer.render(batch, player);

        // Restaura o viewport do jogo para os ShapeRenderers de debug
        cameraManager.getViewport().apply();

        // Desenhar os Hitboxes para debug visual
        shapeRenderer.setProjectionMatrix(cameraManager.getCamera().combined);
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
        cameraManager.resize(width, height);
        hudRenderer.resize(cameraManager);
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
        if (npcSpriteSheet != null) {
            npcSpriteSheet.dispose();
        }
        if (hudSpriteSheet != null) {
            hudSpriteSheet.dispose();
        }
        if (hudRenderer != null) {
            hudRenderer.dispose();
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
