package com.legendofjava.core.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class WoodenSword extends Weapon {

    public WoodenSword(float x, float y, Texture spriteSheet) {
        // As coordenadas (1, 154) correspondem à primeira espada de pé do sprite sheet
        // que é a espada de madeira de acordo com o padrão clássico.
        super(x, y, new TextureRegion(spriteSheet, 1, 154, 7, 16), 1); 
    }

    @Override
    public void onCollect(Player player) {
        player.giveSword();
        this.setActive(false);
    }
}
