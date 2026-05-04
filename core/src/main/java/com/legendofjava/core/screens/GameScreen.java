package com.legendofjava.core.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.legendofjava.core.LegendOfJavaGame;
import com.legendofjava.core.entities.Player;
import com.legendofjava.core.utils.Constants;

public class GameScreen implements Screen {
    
    private LegendOfJavaGame game;
    private OrthographicCamera camera;
    private Viewport viewport;
    private SpriteBatch batch;
    
    private Player player;

    public GameScreen(LegendOfJavaGame game) {
        this.game = game;
        
        camera = new OrthographicCamera();
        viewport = new FitViewport(Constants.V_WIDTH, Constants.V_HEIGHT, camera);
        camera.position.set(viewport.getWorldWidth() / 2f, viewport.getWorldHeight() / 2f, 0);
        
        batch = new SpriteBatch();
        
        // Colocar o player no centro da tela
        player = new Player(Constants.V_WIDTH / 2f - 8, Constants.V_HEIGHT / 2f - 8);
    }

    @Override
    public void show() {
        // Carregar mapa, iniciar câmera e player
    }

    @Override
    public void render(float delta) {
        // Atualiza lógica (update)
        player.update(delta);
        
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        
        // Renderiza mapa e entidades
        batch.begin();
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
    }
}
