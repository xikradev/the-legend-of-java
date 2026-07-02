package com.legendofjava.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
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
import com.legendofjava.core.entities.HeartItem;
import com.legendofjava.core.entities.Octorok;
import com.legendofjava.core.managers.CameraManager;
import com.legendofjava.core.managers.HudRenderer;
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
    private HudRenderer hudRenderer;

    private Texture spriteSheet;
    private Texture npcSpriteSheet;
    private Texture enemiesSpriteSheet;
    private Texture itemsSpriteSheet;

    /** Contador global de Octoroks mortos — a cada 3 mortes dropa um coração. */
    private int octorokKillCount = 0;
    private static final int KILLS_PER_HEART_DROP = 3;

    private Music overworldTheme;
    private Sound receiveItemSound;
    private boolean wasPickingUp = false;

    public GameScreen(LegendOfJavaGame game) {
        this.game = game;
        
        initMap();
        initManagers();
        initPlayer();
        initAudio();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        font.getData().setScale(0.5f);
        hudRenderer = new HudRenderer();
    }

    private void initAudio() {
        overworldTheme = Gdx.audio.newMusic(Gdx.files.internal("audio/music/overworld-theme.mp3"));
        overworldTheme.setLooping(true);
        overworldTheme.setVolume(0.5f); // Optional starting volume
        overworldTheme.play();

        receiveItemSound = Gdx.audio.newSound(Gdx.files.internal("audio/sfx/receive-item.mp3"));
    }

    private void initMap() {
        map = new TmxMapLoader().load("maps/zelda-map.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        spriteSheet        = new Texture("sprites/link-spritesheet.png");
        npcSpriteSheet     = new Texture("sprites/npc-spritesheet.png");
        enemiesSpriteSheet = new Texture("sprites/enemies-spritesheet.png");
        itemsSpriteSheet   = new Texture("sprites/items-zelda.png");
    }
    
    private void initManagers() {
        quadrantManager = new QuadrantManager();
        quadrantManager.loadFromMap(map, npcSpriteSheet, enemiesSpriteSheet);
        
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

        // ── Octoroks ── (deve ser antes do player.update para incluir na colisão)
        List<Octorok> activeOctoroks = quadrantManager.getActiveOctoroks(player.getPosition());

        // Adiciona hitboxes dos Octoroks VIVOS como obstáculo sólido ao player
        for (Octorok octorok : activeOctoroks) {
            if (!octorok.isDead()) {
                activeCollisions.add(octorok.getHitbox());
            }
        }

        // Atualiza lógica do player (já com hitboxes dos inimigos na lista)
        player.update(delta, activeCollisions);

        boolean isPickingUp = player.getCurrentState() == Player.State.PICKING_UP;
        if (isPickingUp && !wasPickingUp) {
            if (overworldTheme.isPlaying()) {
                overworldTheme.pause();
                overworldTheme.setPosition(0);
            }
            receiveItemSound.play(0.8f);
        } else if (!isPickingUp && wasPickingUp) {
            overworldTheme.play();
        }
        wasPickingUp = isPickingUp;

        // Atualiza os fogos ativos no quadrante
        List<Fire> activeFires = quadrantManager.getActiveFires(player.getPosition());
        Rectangle playerHitbox = player.getHitbox();
        
        // Cria uma hitbox ligeiramente expandida (1 pixel) para detectar quando o player
        // apenas "encosta" no fogo, já que a física normal impede a sobreposição total.
        Rectangle damageHitbox = new Rectangle(
            playerHitbox.x - 1, playerHitbox.y - 1, 
            playerHitbox.width + 2, playerHitbox.height + 2
        );
        
        for (Fire fire : activeFires) {
            fire.update(delta);
            if (damageHitbox.overlaps(fire.getHitbox())) {
                player.takeDamage(1);
            }
        }

        processItems(delta);

        warpManager.checkTeleports(player, quadrantManager);
        
        cameraManager.update(player.getPosition());

        // Processa Octoroks: dano, espada e remoção
        Rectangle playerDamageBox = new Rectangle(
            player.getHitbox().x - 1, player.getHitbox().y - 1,
            player.getHitbox().width + 2, player.getHitbox().height + 2
        );
        Rectangle swordBox = player.getSwordHitbox(); // null se não está atacando

        List<Octorok> octoroksToRemove = new ArrayList<>();
        for (Octorok octorok : activeOctoroks) {
            // Colisões sem a hitbox do próprio Octorok, mas COM a hitbox do player
            // (evita auto-bloqueio E impede o Octorok de andar por cima do player)
            Rectangle ownHitbox = octorok.isDead() ? null : octorok.getHitbox();
            List<Rectangle> collisionsForOctorok = new ArrayList<>(activeCollisions);
            if (ownHitbox != null) {
                final Rectangle own = ownHitbox;
                collisionsForOctorok.removeIf(r -> r == own);
            }
            // Adiciona hitbox do player para o Octorok evitar atravessá-lo
            if (!octorok.isDead()) {
                collisionsForOctorok.add(player.getHitbox());
            }
            octorok.update(delta, collisionsForOctorok, player.getPosition());

            // Push-out: se o Octorok ainda sobrepõe o player após o update, empurra o player para fora
            if (!octorok.isDead() && player.getHitbox().overlaps(octorok.getHitbox())) {
                Rectangle ph = player.getHitbox();
                Rectangle oh = octorok.getHitbox();
                // Calcula a menor sobreposição e empurra o player para fora
                float overlapLeft   = (ph.x + ph.width)  - oh.x;
                float overlapRight  = (oh.x + oh.width)  - ph.x;
                float overlapBottom = (ph.y + ph.height) - oh.y;
                float overlapTop    = (oh.y + oh.height) - ph.y;
                float minX = overlapLeft < overlapRight  ? -overlapLeft  : overlapRight;
                float minY = overlapBottom < overlapTop  ? -overlapBottom : overlapTop;
                Vector2 pos = player.getPosition();
                if (Math.abs(minX) < Math.abs(minY)) {
                    player.setPosition(pos.x + minX, pos.y);
                } else {
                    player.setPosition(pos.x, pos.y + minY);
                }
            }

            // Dano dos dardos ao player
            for (var dart : octorok.getDarts()) {
                if (dart.isActive() && playerDamageBox.overlaps(dart.getHitbox())) {
                    player.takeDamage(octorok.getHitDamageHP());
                    dart.deactivate();
                }
            }

            // Dano da espada do player ao Octorok (1 HP = meio coração)
            if (swordBox != null && !octorok.isDead()
                    && swordBox.overlaps(octorok.getHitbox())) {
                octorok.takeDamage(1);
            }

            // Remove Octorok após explosão terminar
            if (octorok.isExplosionFinished()) {
                octoroksToRemove.add(octorok);
            }
        }
        for (Octorok dead : octoroksToRemove) {
            quadrantManager.removeOctorok(dead);

            // Contabiliza a morte e verifica se deve dropar coração
            octorokKillCount++;
            if (octorokKillCount % KILLS_PER_HEART_DROP == 0) {
                HeartItem heart = new HeartItem(
                    dead.getPosition().x,
                    dead.getPosition().y,
                    itemsSpriteSheet
                );
                quadrantManager.addItem(heart);
            }
        }
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
                    // (apenas para itens de caverna, não para corações droppados por inimigos)
                    if (!(item instanceof HeartItem)) {
                        warpManager.markCaveCleared();
                    }
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
        List<Octorok> activeOctoroks = quadrantManager.getActiveOctoroks(player.getPosition());
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
        for (Octorok octorok : activeOctoroks) {
            octorok.render(batch);
        }
        player.render(batch);
        
        // Desenha as coordenadas do player
        String coords = String.format("X: %.0f Y: %.0f", player.getPosition().x, player.getPosition().y);
        font.draw(batch, coords, cameraManager.getCamera().position.x - 120, cameraManager.getCamera().position.y + 80);
        
        batch.end();

        // Desenhar os Hitboxes para debug visual
        shapeRenderer.setProjectionMatrix(cameraManager.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Renderiza explosões dos Octoroks (partículas)
        for (Octorok octorok : activeOctoroks) {
            octorok.renderExplosion(shapeRenderer);
        }

        shapeRenderer.end();

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

        // ── HUD (sempre por cima do mundo) ──────────────────────────────────
        hudRenderer.render(player);
    }

    @Override
    public void resize(int width, int height) {
        cameraManager.resize(width, height);
        hudRenderer.resize(width, height);
    }

    @Override
    public void pause() {
        if (overworldTheme != null && overworldTheme.isPlaying()) {
            overworldTheme.pause();
        }
    }

    @Override
    public void resume() {
        if (overworldTheme != null) {
            overworldTheme.play();
        }
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
        if (enemiesSpriteSheet != null) {
            enemiesSpriteSheet.dispose();
        }
        if (itemsSpriteSheet != null) {
            itemsSpriteSheet.dispose();
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
        if (hudRenderer != null) {
            hudRenderer.dispose();
        }
        if (overworldTheme != null) {
            overworldTheme.dispose();
        }
        if (receiveItemSound != null) {
            receiveItemSound.dispose();
        }
    }
}
