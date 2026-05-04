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

import java.util.ArrayList;
import java.util.List;

public class GameScreen implements Screen {
    
    private LegendOfJavaGame game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    
    private Player player;
    
    private Texture spriteSheet;
    private List<Item> itemsOnMap;

    public GameScreen(LegendOfJavaGame game) {
        this.game = game;
        
        camera = new OrthographicCamera();
        viewport = new FitViewport(Constants.V_WIDTH, Constants.V_HEIGHT, camera);
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        
        batch = new SpriteBatch();
        
        // Colocar o player no centro da tela
        player = new Player(Constants.V_WIDTH / 2f - 8, Constants.V_HEIGHT / 2f - 8);
        
        // Carregar itens
        spriteSheet = new Texture("sprites/link-spritesheet.png");
        itemsOnMap = new ArrayList<>();
        
        // Adiciona a espada de madeira no cenário
        Item woodenSword = new WoodenSword(Constants.V_WIDTH / 2f + 32, Constants.V_HEIGHT / 2f - 8, spriteSheet);
        itemsOnMap.add(woodenSword);
    }

    @Override
    public void show() {
        // Carregar mapa, iniciar câmera e player
    }

    @Override
    public void render(float delta) {
        // Atualiza lógica (update)
        player.update(delta);
        
        // Checar colisão com itens
        List<Item> itemsToRemove = new ArrayList<>();
        for (Item item : itemsOnMap) {
            if (item.isActive()) {
                item.update(delta);
                // Checa distância
                if (player.getPosition().dst(item.getPosition()) < 12f) {
                    item.onCollect(player);
                    itemsToRemove.add(item);
                }
            }
        }
        itemsOnMap.removeAll(itemsToRemove);
        
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        
        // Renderiza mapa e entidades
        batch.begin();
        for (Item item : itemsOnMap) {
            item.render(batch);
        }
        player.render(batch);
        batch.end();
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
    }
}
