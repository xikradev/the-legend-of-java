package com.legendofjava.core;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.ScreenUtils;
import com.legendofjava.core.screens.GameScreen;

public class LegendOfJavaGame extends Game {

    @Override
    public void create() {
        // Inicializa assets e carrega a tela principal
        this.setScreen(new GameScreen(this));
    }

    @Override
    public void render() {
        // Limpa a tela com uma cor de fundo verde escura (lembrando grama)
        ScreenUtils.clear(new Color(0.1f, 0.4f, 0.1f, 1));
        super.render(); // Importante: delega o render para a tela atual
    }
    
    @Override
    public void dispose() {
        super.dispose();
    }
}
