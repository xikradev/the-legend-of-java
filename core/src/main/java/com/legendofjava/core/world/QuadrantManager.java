package com.legendofjava.core.world;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.legendofjava.core.entities.Item;
import com.legendofjava.core.entities.HostNPC;
import com.legendofjava.core.entities.Fire;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuadrantManager {

    public static final float TILE_WIDTH = 256f;
    public static final float TILE_HEIGHT = 176f;
    public static final float BORDER = 1f;

    public static final float TOTAL_WIDTH = TILE_WIDTH + BORDER;
    public static final float TOTAL_HEIGHT = TILE_HEIGHT + BORDER;
    public static final float MAP_TOP = 4000f;

    private Map<String, Quadrant> quadrants;

    public QuadrantManager() {
        this.quadrants = new HashMap<>();
    }

    public void loadFromMap(TiledMap map, Texture npcSpriteSheet) {
        if (map.getLayers().get("colisoes") != null) {
            for (MapObject object : map.getLayers().get("colisoes").getObjects()) {
                if (object instanceof RectangleMapObject) {
                    Rectangle r = ((RectangleMapObject) object).getRectangle();

                    int minCol = getCol(r.x);
                    int maxCol = getCol(r.x + r.width);

                    // O eixo Y do libGDX cresce para cima, logo r.y + r.height é o topo lógico do retângulo.
                    // O grid do jogo tem Y=0 no topo do mundo (Y=4000 do LibGDX) e desce.
                    int minRow = getRow(r.y + r.height);
                    int maxRow = getRow(r.y);

                    for (int col = minCol; col <= maxCol; col++) {
                        for (int row = minRow; row <= maxRow; row++) {
                            Quadrant q = getOrCreateQuadrant(col, row);
                            q.addCollision(r);
                        }
                    }
                }
            }
        }

        // Carrega NPCs e fogos da camada "npcs"
        if (map.getLayers().get("npcs") != null) {
            for (MapObject object : map.getLayers().get("npcs").getObjects()) {
                String type = (String) object.getProperties().get("type");
                if (type == null) {
                    type = (String) object.getProperties().get("class");
                }

                Float objX = object.getProperties().get("x", Float.class);
                Float objY = object.getProperties().get("y", Float.class);

                if (objX != null && objY != null && type != null) {
                    int col = getCol(objX);
                    int row = getRow(objY);
                    Quadrant q = getOrCreateQuadrant(col, row);

                    if ("host_npc".equals(type)) {
                        HostNPC npc = new HostNPC(objX, objY, npcSpriteSheet);
                        q.addHostNpc(npc);
                        // Adiciona o hitbox do NPC como colisão para impedir passagem
                        q.addCollision(npc.getHitbox());
                    } else if ("fire".equals(type)) {
                        Fire fire = new Fire(objX, objY, npcSpriteSheet);
                        q.addFire(fire);
                        // Adiciona o hitbox do fogo como colisão para impedir passagem
                        q.addCollision(fire.getHitbox());
                    }
                }
            }
        }
    }

    public int getCol(float x) {
        return (int) (x / TOTAL_WIDTH);
    }

    public int getRow(float y) {
        float distFromTop = MAP_TOP - y;
        return (int) (distFromTop / TOTAL_HEIGHT);
    }

    public Quadrant getOrCreateQuadrant(int col, int row) {
        String key = col + "_" + row;
        if (!quadrants.containsKey(key)) {
            quadrants.put(key, new Quadrant(col, row));
        }
        return quadrants.get(key);
    }

    public void addItem(Item item) {
        int col = getCol(item.getPosition().x);
        int row = getRow(item.getPosition().y);
        Quadrant q = getOrCreateQuadrant(col, row);
        q.addItem(item);
    }

    public void removeItem(Item item) {
        int centerCol = getCol(item.getPosition().x);
        int centerRow = getRow(item.getPosition().y);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                String key = (centerCol + i) + "_" + (centerRow + j);
                if (quadrants.containsKey(key)) {
                    quadrants.get(key).removeItem(item);
                }
            }
        }
    }

    public List<Rectangle> getActiveCollisions(Vector2 playerPosition) {
        List<Rectangle> active = new ArrayList<>();
        int centerCol = getCol(playerPosition.x);
        int centerRow = getRow(playerPosition.y);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                String key = (centerCol + i) + "_" + (centerRow + j);
                if (quadrants.containsKey(key)) {
                    for (Rectangle r : quadrants.get(key).getCollisions()) {
                        if (!active.contains(r)) {
                            active.add(r);
                        }
                    }
                }
            }
        }
        return active;
    }

    public List<Item> getActiveItems(Vector2 playerPosition) {
        List<Item> active = new ArrayList<>();
        int centerCol = getCol(playerPosition.x);
        int centerRow = getRow(playerPosition.y);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                String key = (centerCol + i) + "_" + (centerRow + j);
                if (quadrants.containsKey(key)) {
                    for (Item item : quadrants.get(key).getItems()) {
                        if (!active.contains(item)) {
                            active.add(item);
                        }
                    }
                }
            }
        }
        return active;
    }

    public List<HostNPC> getActiveHostNpcs(Vector2 playerPosition) {
        List<HostNPC> active = new ArrayList<>();
        int centerCol = getCol(playerPosition.x);
        int centerRow = getRow(playerPosition.y);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                String key = (centerCol + i) + "_" + (centerRow + j);
                if (quadrants.containsKey(key)) {
                    for (HostNPC npc : quadrants.get(key).getHostNpcs()) {
                        if (!active.contains(npc)) {
                            active.add(npc);
                        }
                    }
                }
            }
        }
        return active;
    }

    public List<Fire> getActiveFires(Vector2 playerPosition) {
        List<Fire> active = new ArrayList<>();
        int centerCol = getCol(playerPosition.x);
        int centerRow = getRow(playerPosition.y);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                String key = (centerCol + i) + "_" + (centerRow + j);
                if (quadrants.containsKey(key)) {
                    for (Fire fire : quadrants.get(key).getFires()) {
                        if (!active.contains(fire)) {
                            active.add(fire);
                        }
                    }
                }
            }
        }
        return active;
    }
}
