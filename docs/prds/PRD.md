# Product Requirements Document (PRD) & Architecture - The Legend of Java

## 1. Visão Geral do Projeto
**Nome do Projeto:** The Legend of Java
**Gênero:** RPG de Ação Top-Down 2D (Inspirado em The Legend of Zelda)
**Plataforma Alvo Primária:** Desktop (Linux, Windows, macOS)
**Linguagem:** Java 21
**Engine/Framework:** LibGDX 1.13.0
**Build System:** Gradle (Multi-módulo, Wrapper incluso)

O projeto visa recriar a sensação clássica de jogos 2D top-down, com movimentação baseada em grid/pixels, transição de telas para exploração de mundo e masmorras, gerenciamento de entidades interativas e uso de spritesheets clássicas.

---

## 2. Arquitetura do Sistema

O projeto adota uma arquitetura limpa e escalável exigida por jogos modernos feitos em Java, aproveitando a divisão em submódulos que o LibGDX propõe nativamente.

### 2.1. Estrutura Multi-Módulo (Gradle)
A base do projeto está dividida em dois módulos principais para isolar a lógica de jogo da plataforma de execução:

- **Módulo `core` (Lógica do Jogo):** Contém 100% da lógica de negócio, renderização e estado do jogo. Ele não tem dependência de componentes específicos de sistema operacional, sendo totalmente multiplataforma.
- **Módulo `desktop` (Lançador):** Contém a inicialização (método `main`) para Desktop, utilizando o backend LWJGL3. Configura o tamanho da janela, v-sync, tratamento de OpenGL (Wayland/X11 no Linux) e inicia a classe raiz do módulo `core`.

### 2.2. Fluxo de Estado (Screens)
O controle de cenas do jogo utiliza o padrão State (através das classes `Game` e `Screen` do LibGDX).
- **`LegendOfJavaGame.java`:** É a aplicação central. Atua como máquina de estados, retendo referências para `SpriteBatch` genéricos, gerenciadores de fontes e delegando as requisições de ciclo de vida (render, dispose, pause) para a `Screen` atual.
- **`GameScreen.java`:** Tela orquestradora da lógica in-game. Delega as responsabilidades de estado, físicas e de mundo para gerenciadores especializados (`CameraManager`, `WarpManager`, `QuadrantManager`), focando em dividir o fluxo em sub-métodos de `update` e `draw`.

### 2.3. Estrutura de Pacotes do Core
O código-fonte em `core/src/main/java/com/legendofjava/core/` é organizado nos seguintes domínios:

- `entities`: Contém os atores do jogo (Player, NPCs, Inimigos). Futuramente pode abrigar os componentes caso o projeto migre formalmente para uma arquitetura ECS (Entity Component System) com o framework Ashley.
- `world`: Gerenciamento e lógica do cenário. Contém classes como `QuadrantManager` para particionamento espacial, e `WarpManager` para transição de salas/cavernas e leitura do mapa `Tiled` (.tmx).
- `physics`: Sistemas responsáveis pela detecção de colisão manual genérica ou instâncias de `Box2D` caso a física se torne estrita.
- `managers`: Gerenciadores de sistemas do jogo, como o `CameraManager` (que cuida da transição de telas e viewport), `AssetManager` nativo do LibGDX, gerenciamento de estado de som e salvamento do progresso.
- `screens`: Controladores de estado da interface. (Menu Principal, Tela de Jogo, Inventário de Pausa).
- `utils`: Classes auxiliares e definição de constantes globais (ex: `Constants.java` configurando resolução virtual base 400x240).

---

## 3. Gestão de Assets e Recursos

Para manter um padrão organizado em relação aos recursos do jogo, a pasta raiz `assets/` segue a convenção:
- `assets/sprites/`: Texturas limpas e planilhas de sprites em .png.
- `assets/maps/`: Arquivos gerados pelo Tiled Map Editor (.tmx e tilesets .tsx).
- `assets/audio/music/`: Trilhas sonoras de fundo (BGM).
- `assets/audio/sfx/`: Efeitos curtos em memória (.wav ou .ogg).
- `assets/ui/`: Elementos de interface como fontes (.ttf/.fnt), cursores ou peles de janelas (Scene2D.ui).

---

## 4. Decisões Técnicas Críticas

1. **Java 21:** Permite uso extensivo das novas features como *Pattern Matching*, *Records* (ideal para classes de dados do jogo), e *Switch Expressions*, deixando o código menos verboso do que em projetos Java antigos.
2. **Resolução Virtual Fixa:** Estabelecida provisoriamente em `400x240` (inspirada nas proporções 16:9 retrô, mas escalada). O redimensionamento usará uma Viewport no LibGDX (como `FitViewport` ou `ExtendViewport`) para garantir que os pixels pareçam corretos em telas maiores.
3. **Sandbox do Snap & Gradle Wrapper:** O projeto inclui scripts do Gradle Wrapper (`gradlew`). **É obrigatório que o jogo seja compilado e executado por este wrapper** (`./gradlew`), a fim de escapar de sandboxes de permissões de pacotes (como as versões Snap do Linux que impedem janelas GLFW de acessarem sockets Wayland nativos).

---
*Gerado automaticamente para documentação da fase fundacional.*
