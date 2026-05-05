package com.legendofjava.core.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.legendofjava.core.entities.Item;
import com.legendofjava.core.entities.Player;
import com.legendofjava.core.entities.WoodenSword;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarpManager {

    private Vector2 returnPosition;
    private String currentCaveId;
    private Map<String, Boolean> clearedCaves = new HashMap<>();
    private List<RectangleMapObject> warps = new ArrayList<>();
    private List<RectangleMapObject> caveExits = new ArrayList<>();
    private float spawnX = 0;
    private float spawnY = 0;
    private Texture spriteSheet;

    public WarpManager(TiledMap map, Texture spriteSheet) {
        this.spriteSheet = spriteSheet;

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

    public void checkTeleports(Player player, QuadrantManager quadrantManager) {
        Rectangle pRect = player.getHitbox();

        // Lógica de Colisão com Warps (Teletransporte)
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
    }

    public void markCaveCleared() {
        if (currentCaveId != null) {
            clearedCaves.put(currentCaveId, true);
        }
    }
}
