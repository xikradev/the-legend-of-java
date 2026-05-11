# Especificação Técnica: HUD (Heads-Up Display)

Este documento descreve a implementação do sistema de HUD do jogo, posicionado acima da viewport do jogo no estilo do Zelda NES original.

---

## 1. Visão Geral

O HUD é renderizado em uma **faixa separada acima da área do jogo**, usando uma câmera e viewport independentes da câmera do mundo. O visual é extraído diretamente do `hub-spritesheet.png`, que contém o HUD original do Zelda NES.

```
┌────────────────────────┐  ← Janela física (ex: 1200×720 @ scale 3x)
│   HUD   256×56 virtual │  ← FitViewport do HUD  (HudRenderer)
├────────────────────────┤
│                        │
│  Jogo   256×176 virtual│  ← FitViewport do jogo (CameraManager)
│                        │
└────────────────────────┘
   Total virtual: 256×232
```

---

## 2. Dimensões Virtuais

| Área     | Largura | Altura | Constante                          |
|----------|---------|--------|------------------------------------|
| HUD      | 256 px  | 56 px  | `HudRenderer.HUD_WIDTH/HEIGHT`     |
| Jogo     | 256 px  | 176 px | `HudRenderer.GAME_WIDTH/HEIGHT`    |
| **Total**| **256** | **232**| soma HUD + Jogo                    |

> [!IMPORTANT]
> As constantes `HUD_WIDTH`, `HUD_HEIGHT`, `GAME_WIDTH` e `GAME_HEIGHT` em `HudRenderer` são `public static final` e usadas pelo `CameraManager.resize()` para calcular o layout. **Não altere esses valores sem atualizar ambas as classes.**

---

## 3. Spritesheet do HUD

**Arquivo:** `assets/sprites/hub-spritesheet.png`
**Dimensões do sheet:** 752 × 208 pixels, modo RGBA

### 3.1. Região do HUD completo

A região usada para renderizar o HUD inteiro (minimapa + slots B/A + corações) é:

```
x=264, y=11, w=250, h=54
```

Essa região é esticada para preencher os `256×56` virtuais do HUD.

```java
// Em HudRenderer.java:
hudRegion = new TextureRegion(hudSheet, 264, 11, 250, 54);
```

### 3.2. Elementos visuais na região do HUD

O HUD extraído contém, da esquerda para a direita:

| Elemento          | Posição no sheet (aprox.) | Descrição                          |
|-------------------|---------------------------|------------------------------------|
| Minimapa          | x=264..336, y=11..60      | Área com borda preta, fundo transparente |
| Contador de itens | x=264..340, y=60..104     | Ícones de rupees e bombas (X0, X3) |
| Slot B (escudo)   | x=346..384, y=28..58      | Borda azul, ícone do escudo        |
| Slot A (espada)   | x=385..422, y=28..58      | Borda azul, ícone da espada        |
| Label "-LIFE-"    | x=430..490, y=11..26      | Texto em rosa/laranja              |
| Corações          | x=442..487, y=27..33      | Corações cheios em vermelho (8×8px cada) |

### 3.3. Corações individuais (para uso futuro)

Caso seja necessário desenhar corações dinamicamente (ex: sistema de vida), as posições individuais no sheet são:

| Tipo           | x no sheet | y no sheet | Tamanho |
|----------------|------------|------------|---------|
| Coração cheio  | 451        | 24         | 8×8 px  |
| Coração vazio  | 483        | 24         | 8×8 px  |

> [!NOTE]
> Atualmente o HUD é renderizado como **bloco único** (sem corações dinâmicos). Para implementar vida variável, será necessário:
> 1. Renderizar o fundo do HUD sem os corações (ou cobri-los com um retângulo preto)
> 2. Desenhar os corações individualmente em cima, de acordo com `player.getHearts()`

---

## 4. Arquitetura de Classes

### 4.1. `HudRenderer` (`core/hud/HudRenderer.java`)

Responsável exclusivamente pela renderização do HUD.

**Campos principais:**
```java
private final Texture hudSheet;          // gerenciado pelo GameScreen
private final OrthographicCamera hudCamera;
private final Viewport hudViewport;      // FitViewport(256, 56)
private final TextureRegion hudRegion;   // (264, 11, 250, 54)
```

**Métodos públicos:**

| Método | Quando chamar | Descrição |
|--------|--------------|-----------|
| `HudRenderer(Texture hudSheet)` | Construção | Recebe a textura gerenciada externamente |
| `resize(CameraManager cam)` | Em `GameScreen.resize()`, **após** `cameraManager.resize()` | Posiciona o viewport do HUD acima do jogo |
| `render(SpriteBatch batch, Player player)` | Em `GameScreen.draw()`, **após** `batch.end()` do mundo | Aplica viewport do HUD e desenha |
| `dispose()` | Em `GameScreen.dispose()` | Não disposa o `hudSheet` (responsabilidade do chamador) |

**Ciclo de vida do `render()`:**
```
hudViewport.apply()
batch.setProjectionMatrix(hudCamera.combined)
batch.begin()
  batch.draw(hudRegion, 0, 0, 256, 56)
batch.end()
```

### 4.2. `CameraManager` (`core/managers/CameraManager.java`)

Além de gerenciar a câmera do jogo, o `CameraManager` é responsável pelo **cálculo do layout de tela** que coordena jogo e HUD.

**Lógica do `resize(int width, int height)`:**

```java
// Escala total que cabe 256×232 na janela
float scale = Math.min(width / 256f, height / 232f);

int gamePixelW = (int)(256 * scale);
int gamePixelH = (int)(176 * scale);
int hudPixelH  = (int)(56  * scale);

int offsetX     = (width  - gamePixelW) / 2;        // centraliza horizontalmente
int gameOffsetY = (height - gamePixelH - hudPixelH) / 2;  // centraliza verticalmente

// Viewport do jogo ocupa a faixa INFERIOR
viewport.setScreenBounds(offsetX, gameOffsetY, gamePixelW, gamePixelH);
```

**Getters de layout** (usados por `HudRenderer.resize()`):

| Getter | Tipo | Descrição |
|--------|------|-----------|
| `getLastScale()` | `float` | Escala aplicada |
| `getLastOffsetX()` | `int` | Offset X da área centralizada |
| `getLastGameOffsetY()` | `int` | Y inicial do viewport do jogo em pixels físicos |
| `getLastGamePixelW()` | `int` | Largura do viewport do jogo em pixels físicos |
| `getLastGamePixelH()` | `int` | Altura do viewport do jogo em pixels físicos |

**Como `HudRenderer` usa esses dados:**
```java
// HUD fica logo ACIMA do viewport do jogo
int hudOffsetY = cam.getLastGameOffsetY() + cam.getLastGamePixelH();
hudViewport.setScreenBounds(hudOffsetX, hudOffsetY, hudPixelW, hudPixelH);
```

### 4.3. `GameScreen` (`core/screens/GameScreen.java`)

Integra o HUD ao ciclo de vida da tela.

**Campos:**
```java
private Texture hudSpriteSheet;   // "sprites/hub-spritesheet.png"
private HudRenderer hudRenderer;
```

**Ordem de inicialização (construtor):**
```
initMap()        → carrega hudSpriteSheet, cria HudRenderer
initManagers()   → cria cameraManager
initPlayer()
// batch, shapeRenderer, font...
resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight())  // ← configura ambos os viewports
```

**Ordem de renderização (`draw()`):**
```
1. mapRenderer.render()                  // mapa do mundo
2. batch.begin() ... batch.end()         // entidades, player, coords debug
3. hudRenderer.render(batch, player)     // HUD (aplica própria viewport)
4. cameraManager.getViewport().apply()   // restaura viewport do jogo
5. shapeRenderer (debug hitboxes)        // debug visual
```

> [!WARNING]
> Após chamar `hudRenderer.render()`, o viewport ativo da LibGDX muda para o do HUD. É **obrigatório** chamar `cameraManager.getViewport().apply()` antes de usar o `ShapeRenderer` ou qualquer outro sistema que dependa do viewport do jogo.

---

## 5. Fluxo de `resize`

```
GameScreen.resize(w, h)
  └─► cameraManager.resize(w, h)
        └─► calcula scale, offsets, seta viewport do jogo
        └─► salva lastScale, lastOffsetX, lastGameOffsetY, lastGamePixelW, lastGamePixelH
  └─► hudRenderer.resize(cameraManager)
        └─► lê os dados salvos do cameraManager
        └─► posiciona hudViewport logo acima do gameViewport
```

---

## 6. O que falta implementar (melhorias futuras)

- [ ] **Corações dinâmicos**: renderizar corações individualmente de acordo com HP do player
- [ ] **Minimapa funcional**: exibir posição do player no minimapa do HUD
- [ ] **Itens equipados dinâmicos**: mostrar no slot B/A os itens que o player carrega
- [ ] **Contador de rupees e bombas**: integrar com o inventário do player
- [ ] **Suporte a mais corações**: o Zelda original suporta até 16 corações em 2 linhas

---

## 7. Notas de Debug / Gotchas

1. **O `resize()` é chamado no construtor do `GameScreen`** (via `Gdx.graphics.getWidth/Height`) para garantir que os viewports estejam configurados desde o primeiro frame, mesmo antes de qualquer evento `resize` da janela.

2. **O `hudSheet` NÃO é disposed pelo `HudRenderer`** — é responsabilidade do `GameScreen` chamar `hudSpriteSheet.dispose()` no `dispose()`.

3. **Coordenadas do HUD são locais (0,0 = canto inferior-esquerdo)**: a câmera do HUD é posicionada em `(128, 28)` (centro de 256×56), então ao desenhar elementos no HUD, use coordenadas virtuais de `0..256` (X) e `0..56` (Y).

4. **O sheet tem fundo parcialmente transparente**: o processo de remoção do fundo verde/cinza foi feito em conversação anterior. A região `(264, 11, 250, 54)` usa fundo **preto opaco** — não transparente. Se precisar de fundo transparente no HUD, será necessário tratar o preto como transparente via shader ou pré-processamento.
