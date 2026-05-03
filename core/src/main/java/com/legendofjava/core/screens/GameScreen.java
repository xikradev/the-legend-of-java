package com.legendofjava.core.screens;

import com.badlogic.gdx.Screen;
import com.legendofjava.core.LegendOfJavaGame;

public class GameScreen implements Screen {
    
    private LegendOfJavaGame game;

    public GameScreen(LegendOfJavaGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        // Carregar mapa, iniciar câmera e player
    }

    @Override
    public void render(float delta) {
        // Atualiza lógica (update)
        // Renderiza mapa e entidades
    }

    @Override
    public void resize(int width, int height) {
        // Atualiza viewport
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
    }
}
