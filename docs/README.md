# The Legend of Java

> Trabalho acadêmico desenvolvido como projeto de disciplina. Um jogo de aventura 2D inspirado em *The Legend of Zelda* (NES, 1986), construído inteiramente em Java utilizando o framework LibGDX.

---

## Sumário

1. [Sobre o Projeto](#sobre-o-projeto)
2. [Funcionalidades Implementadas](#funcionalidades-implementadas)
3. [Tecnologias e Arquitetura](#tecnologias-e-arquitetura)
4. [Estrutura do Projeto](#estrutura-do-projeto)
5. [Como Executar na sua Máquina](#como-executar-na-sua-máquina)
6. [Como Jogar](#como-jogar)
7. [Decisões de Design e Arquitetura](#decisões-de-design-e-arquitetura)
8. [Diagramas](#diagramas)

---

## Sobre o Projeto

**The Legend of Java** é um jogo de aventura em perspectiva top-down (visão de cima) que recria a experiência clássica do primeiro *The Legend of Zelda*. O projeto foi desenvolvido com fins acadêmicos e explora conceitos fundamentais de desenvolvimento de jogos, como:

- Game loop e gerenciamento de estado
- Física de colisão por AABB (Axis-Aligned Bounding Box)
- Animação de sprites via spritesheets
- Carregamento e renderização de mapas tile-based
- Sistema de entidades (player, inimigos, itens, NPCs)
- Sistema de câmera com seguimento de personagem
- HUD (Heads-Up Display) com representação de vida

O jogo possui um mapa do overworld (mundo exterior) com câmeras, colisões, inimigos e cavernas interativas. O objetivo é explorar o mapa, encontrar a espada dentro de uma caverna, derrotar inimigos e chegar à zona de fim de jogo.

---

## Funcionalidades Implementadas

| Funcionalidade | Descrição |
|---|---|
| **Movimentação do Player** | Movimento nos 4 sentidos com suporte a WASD e setas direcionais |
| **Sistema de Animação** | Animações de caminhada, ataque e coleta de item via spritesheet |
| **Sistema de Vida (HUD)** | 5 corações representando 10 HP, desenhados pixel a pixel |
| **Slot de Arma (HUD)** | Caixa no canto esquerdo que exibe a espada quando coletada |
| **Colisão com Mapa** | Retângulos de colisão carregados do mapa Tiled (.tmx) |
| **Espada (WoodenSword)** | Item coletável em caverna; habilita o ataque corpo-a-corpo |
| **Ataque com Espada** | Hitbox direcional projetada à frente do player |
| **Inimigo Octorok** | IA de perseguição, disparo de dardos e sistema de vida próprio |
| **Dardos (OctorokDart)** | Projéteis que causam dano ao player ao colidir |
| **Explosão de Morte** | Partículas laranja/vermelhas ao derrotar um Octorok |
| **Drop de Coração** | A cada 3 Octoroks derrotados, um coração é dropado no mapa |
| **Cavernas (Warp System)** | Teletransporte ao entrar/sair de cavernas via zonas no mapa |
| **Câmera Suave** | Câmera que segue o player com interpolação e limites do mapa |
| **Música e Sons** | Tema do overworld em loop; efeito de som ao coletar item |
| **Game Over** | Tela de derrota ao morrer (HP = 0) com opção de reiniciar |
| **Game Clear** | Tela de vitória ao chegar na zona de fim de jogo |
| **NPC (Host NPC)** | Personagens estáticos no mapa (ex: guardião da caverna) |
| **Fogo** | Obstáculo animado que causa dano contínuo ao ser tocado |
| **Sistema de Quadrantes** | Otimização de performance: só processa entidades próximas ao player |

---

## Tecnologias e Arquitetura

### Stack Principal

| Tecnologia | Versão | Função |
|---|---|---|
| **Java** | 21 | Linguagem principal do projeto |
| **LibGDX** | 1.13.0 | Framework de desenvolvimento de jogos 2D |
| **Gradle** | (wrapper incluso) | Build tool e gerenciador de dependências |
| **Tiled Map Editor** | — | Editor de mapas (.tmx / .tsx) |

### Módulos LibGDX utilizados

- `gdx` — núcleo: rendering, input, audio, assets
- `gdx-box2d` — suporte a física (incluído como dependência base)
- `gdx-freetype` — renderização de fontes TrueType
- `gdx-backend-lwjgl3` — backend desktop (OpenGL via LWJGL 3)

---

## Estrutura do Projeto

```
the-legend-of-java/
├── core/                          # Módulo principal do jogo
│   └── src/main/java/com/legendofjava/core/
│       ├── LegendOfJavaGame.java  # Ponto de entrada; inicializa a GameScreen
│       ├── screens/
│       │   └── GameScreen.java    # Loop principal: update() + draw()
│       ├── entities/              # Todas as entidades do jogo
│       │   ├── Player.java        # Personagem jogável (movimento, ataque, vida)
│       │   ├── Octorok.java       # Inimigo com IA de perseguição e dardos
│       │   ├── OctorokDart.java   # Projétil do Octorok
│       │   ├── HeartItem.java     # Item de cura dropado por inimigos
│       │   ├── WoodenSword.java   # Item coletável que habilita o ataque
│       │   ├── HostNPC.java       # NPC estático (guardião de caverna)
│       │   ├── Fire.java          # Obstáculo de fogo animado
│       │   ├── Item.java          # Interface/base abstrata de itens
│       │   └── Weapon.java        # Interface base de armas
│       ├── managers/              # Gerenciadores de subsistemas
│       │   ├── CameraManager.java # Câmera com interpolação e limite de borda
│       │   ├── HudRenderer.java   # Renderização da HUD (corações + slot de arma)
│       │   └── GameOverOverlay.java # Telas de Game Over e Game Clear
│       ├── world/                 # Gerenciamento do mundo
│       │   ├── QuadrantManager.java # Divisão do mapa em quadrantes para otimização
│       │   ├── Quadrant.java      # Contém colisões, itens e inimigos por setor
│       │   └── WarpManager.java   # Sistema de teletransporte (warps e cavernas)
│       └── physics/               # (reservado para física futura)
├── desktop/                       # Launcher para desktop (LWJGL 3)
├── assets/                        # Recursos estáticos do jogo
│   ├── maps/                      # Mapas Tiled (.tmx, .tsx)
│   ├── sprites/                   # Spritesheets (Link, NPCs, inimigos, itens)
│   ├── audio/
│   │   ├── music/                 # Trilha sonora (overworld-theme.mp3)
│   │   └── sfx/                   # Efeitos sonoros (receive-item.mp3)
│   └── ui/                        # Assets da interface
└── docs/                          # Documentação do projeto
    └── prds/                      # Documentos de requisitos
```

---

## Como Executar na sua Máquina

### Pré-requisitos

Antes de rodar o projeto, verifique se você possui:

- **Java 21** ou superior instalado  
  Verifique com: `java -version`
- **Git** instalado (para clonar o repositório)

> O projeto utiliza o **Gradle Wrapper** (`gradlew`), portanto **não é necessário** ter o Gradle instalado globalmente.

---

### Passo a Passo

**1. Clone o repositório**

```bash
git clone https://github.com/xikradev/the-legend-of-java.git
cd the-legend-of-java
```

**2. Execute o jogo (desktop)**

No **Linux / macOS**:
```bash
./gradlew desktop:run
```

No **Windows (PowerShell ou CMD)**:
```cmd
gradlew.bat desktop:run
```

O Gradle irá baixar automaticamente todas as dependências necessárias na primeira execução. Após o download, a janela do jogo será aberta.

---

### Possíveis Problemas

| Problema | Solução |
|---|---|
| `java: command not found` | Instale o JDK 21: `sudo apt install openjdk-21-jdk` (Ubuntu/Debian) |
| Permissão negada no `gradlew` | Execute: `chmod +x gradlew` |
| Erro de OpenGL | Certifique-se de que os drivers de vídeo estão atualizados |
| Tela em branco ao iniciar | Aguarde alguns segundos; o LibGDX inicializa o contexto OpenGL |

---

## Como Jogar

### Objetivo

Explore o mapa, encontre a **Espada de Madeira** dentro de uma caverna, derrote os inimigos **Octorok** e chegue à **zona de fim de jogo** para vencer.

---

### Controles

| Tecla | Ação |
|---|---|
| `W` ou `↑` | Mover para cima |
| `S` ou `↓` | Mover para baixo |
| `A` ou `←` | Mover para a esquerda |
| `D` ou `→` | Mover para a direita |
| `Espaço` ou `Z` | Atacar com a espada (somente após coletá-la) |

---

### Mecânicas

#### ❤️ Sistema de Vida
- O player possui **5 corações** na HUD, representando **10 pontos de vida (HP)**.
- Cada coração equivale a **2 HP** — meio coração é exibido quando restam 1 HP naquele coração.
- Ao levar dano, o player fica **invulnerável por 1 segundo** (efeito de piscar).
- Se o HP chegar a **zero**, a tela de **Game Over** é exibida.

#### 🗡️ Espada
- A espada fica guardada em uma **caverna** no mapa. Encontre a entrada e entre nela.
- Ao coletar o item, uma animação de "pegar item" é executada e a música para momentaneamente.
- Com a espada, pressione `Espaço` ou `Z` para atacar na direção em que o player está virado.

#### 🐙 Inimigo: Octorok
- O Octorok começa a perseguir o player quando este entra em seu **raio de detecção** (~96 pixels).
- A cada **2 segundos**, o Octorok dispara um **dardo** em direção ao player.
- O Octorok possui **3 HP** (1,5 corações). Cada acerto da espada remove 1 HP.
- Ao morrer, o Octorok explode em **partículas laranja**.
- A cada **3 Octoroks** derrotados, um **coração** é dropado no local da morte. Caminhe sobre ele para recuperar **2 HP**.

#### 🔥 Fogo
- Tochas de fogo em certas áreas causam **1 HP de dano** ao encostar. Desvie delas!

#### 🚪 Cavernas (Warps)
- Entre em uma **entrada de caverna** para ser teleportado para dentro dela.
- Saia pela saída da caverna para voltar ao overworld na posição original.
- A caverna com a espada só contém o item **uma vez** — após coletada, ela não reaparece.

#### 🏁 Fim de Jogo
- Ao alcançar a **zona de fim de jogo** no mapa, a tela de **"Game Clear"** é exibida.
- Tanto no Game Over quanto no Game Clear, é possível **reiniciar** o jogo.

---

## Decisões de Design e Arquitetura

### 1. Framework: LibGDX

O LibGDX foi escolhido por ser uma das bibliotecas Java de jogos mais robustas e maduras disponíveis. Ele abstrai o acesso ao OpenGL, gerencia o ciclo de vida da aplicação (create → render → resize → dispose), e oferece módulos prontos para áudio, input e assets. Isso permitiu focar na lógica do jogo em vez de na infraestrutura de baixo nível.

### 2. Arquitetura por Subsistemas (Managers)

O código foi organizado em responsabilidades bem definidas:

- **`GameScreen`**: orquestra o game loop (chama `update()` e `draw()` a cada frame).
- **`CameraManager`**: isola toda a lógica de câmera (posição, limites, interpolação).
- **`QuadrantManager`**: divide o mapa em setores para otimizar a detecção de colisão e a renderização de entidades — apenas os quadrantes próximos ao player são processados a cada frame.
- **`WarpManager`**: gerencia os teletransportes entre overworld e cavernas, incluindo o estado de "caverna limpa".
- **`HudRenderer`**: renderiza a interface do usuário em uma câmera separada (viewport ortogonal independente), garantindo que a HUD não seja afetada pelo zoom ou posição da câmera do mundo.

### 3. Colisão por AABB (Hitboxes Retangulares)

O sistema de colisão utiliza retângulos alinhados aos eixos (*Axis-Aligned Bounding Boxes*). As colisões são definidas no editor de mapas **Tiled** em uma camada chamada `colisoes` e carregadas em tempo de execução. Isso permite editar o mapa sem modificar o código Java.

A física de colisão funciona em dois passos:
1. Move o player no eixo X e verifica colisão → reverte se colidir.
2. Move o player no eixo Y e verifica colisão → reverte se colidir.

Isso garante que o player deslize ao longo de paredes sem travar.

### 4. Mapa Tiled (.tmx)

O mapa do jogo foi criado no **Tiled Map Editor** e exportado no formato `.tmx` (XML). O LibGDX o processa via `TmxMapLoader` e `OrthogonalTiledMapRenderer`. O mapa possui várias camadas:

| Camada | Conteúdo |
|---|---|
| `colisoes` | Retângulos de colisão (invisíveis no jogo) |
| `npcs` | Posições de NPCs e tochas de fogo |
| `inemies` | Posições e propriedades dos Octoroks |
| `warps` | Zonas de teletransporte para cavernas |
| `items` | Posições de spawn de itens dentro das cavernas |

### 5. Sistema de Quadrantes

Para jogos com mapas grandes, verificar a colisão de **todas** as entidades do mapa a cada frame seria ineficiente. O `QuadrantManager` divide o mapa em setores fixos de **256×176 pixels** (o tamanho de uma tela). A cada frame, apenas as entidades dos quadrantes **adjacentes** ao player são consultadas, reduzindo drasticamente o número de verificações.

### 6. Sprites e Animações

Todos os sprites do jogo estão em **spritesheets** (uma única imagem com vários frames). O LibGDX permite recortar regiões específicas de uma textura com `TextureRegion`, e a classe `Animation<TextureRegion>` avança os frames automaticamente com base no tempo.

O personagem *Link* possui animações para:
- Caminhar em 4 direções
- Atacar com a espada em 4 direções (cada uma com 4 frames)
- Pegar um item

### 7. Invulnerabilidade e Efeito de Piscar

Após levar dano, o player (e o Octorok) ficam temporariamente invulneráveis. Durante esse período, o sprite "pisca" alternando entre visível e invisível a cada poucos milissegundos. Isso é implementado verificando se `(int)(invulnerabilityTimer * frequência) % 2 == 0` — uma técnica simples e eficiente sem necessidade de timers adicionais.

---

## Diagramas

### Diagrama Simplificado de Classes

```
LegendOfJavaGame
└── GameScreen
    ├── Player               (movimento, ataque, vida)
    ├── QuadrantManager      (otimização por setores)
    │   └── Quadrant[]       (colisões, entidades, itens)
    ├── CameraManager        (câmera do mundo)
    ├── WarpManager          (cavernas e teletransportes)
    ├── HudRenderer          (corações, slot de arma)
    ├── GameOverOverlay      (telas de Game Over / Clear)
    └── Entidades:
        ├── Octorok          (IA, dardos, explosão)
        │   └── OctorokDart  (projétil)
        ├── HeartItem        (cura +2 HP)
        ├── WoodenSword      (habilita ataque)
        ├── HostNPC          (NPC guardião)
        └── Fire             (obstáculo de fogo)
```

### Fluxo do Game Loop

```
create()
  └── GameScreen()
        ├── initMap()       ← carrega .tmx, texturas
        ├── initManagers()  ← QuadrantManager, Camera, Warp
        └── initPlayer()    ← posição inicial do player

render(delta) a cada frame:
  ├── update(delta)
  │     ├── Verifica Game Over / Game Clear
  │     ├── Obtém colisões do quadrante atual
  │     ├── Atualiza Player (input + física)
  │     ├── Atualiza Octoroks (IA + dardos + combate)
  │     ├── Atualiza itens (coleta)
  │     ├── Verifica warps (teletransporte)
  │     └── Atualiza câmera
  └── draw()
        ├── Renderiza mapa (tiles)
        ├── Renderiza entidades (itens, NPCs, inimigos, player)
        ├── Renderiza explosões (ShapeRenderer)
        ├── Renderiza HUD (câmera separada)
        └── Renderiza overlay (Game Over / Clear)
```

---

*Projeto desenvolvido para fins acadêmicos — disciplina de Programação Orientada a Objetos / Desenvolvimento de Jogos.*
