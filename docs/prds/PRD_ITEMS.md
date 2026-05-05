# Product Requirements Document (PRD) Técnico - Sistema de Itens e Armas

## 1. Visão Geral
Como parte da evolução da arquitetura do jogo "The Legend of Java", foi desenvolvido um sistema de entidades escalável focado em **Itens** e **Armas**. 
O objetivo deste sistema é afastar a lógica de gerenciamento de itens ("hardcoded") da tela de jogo (`GameScreen`) e criar uma fundação orientada a objetos que permita adicionar uma variedade infinita de coletáveis, armamentos e relíquias com comportamentos customizados.

---

## 2. Arquitetura do Sistema de Itens

A nova estrutura foi posicionada no pacote `com.legendofjava.core.entities` e baseia-se fortemente em polimorfismo e herança para garantir reaproveitamento de código.

### 2.1. A Classe Abstrata `Item`
A fundação de todo coletável é a classe abstrata `Item.java`. Ela encapsula os atributos e métodos genéricos aplicáveis a qualquer coisa que fique no chão do cenário aguardando interação.

**Atributos Principais:**
- `Vector2 position`: Determina onde no mapa o item será renderizado e onde a colisão ocorrerá.
- `TextureRegion sprite`: O recorte visual extraído do `spritesheet` correspondente àquele item.
- `boolean active`: Flag booleana que dita se o item ainda está em cena e deve ser processado.

**Métodos Principais:**
- `update(float delta)`: Método em branco (hook) projetado para itens que precisam ter animações ou gravidade/movimentação flutuante no futuro.
- `render(SpriteBatch batch)`: Cuida do desenho do sprite caso o item esteja ativo.
- `abstract void onCollect(Player player)`: **O núcleo lógico.** É um método abstrato que força cada tipo de item a definir sua própria regra ao ser tocado pelo jogador.

### 2.2. A Extensão Abstrata `Weapon`
Construída como uma camada intermediária, a classe abstrata `Weapon.java` herda de `Item`. A finalidade dela é tipar e agrupar itens cujo propósito principal é causar dano aos inimigos.

**Atributos Principais:**
- `int damage`: O poder base da arma.

Sistemas futuros de combate (`CombatManager` ou a lógica de checagem contra inimigos) verificarão se a arma atual do Player é uma instância de `Weapon` para aplicar o dano corretamente através de getters padronizados (`getDamage()`).

### 2.3. Implementação Concreta: `WoodenSword`
A `WoodenSword.java` materializa o conceito de arma, aplicando as abstrações descritas acima.

**Especificações Técnicas:**
- **Recorte Visual (Sprite):** No construtor, um `TextureRegion` específico da espada é extraído baseado em coordenadas fixadas conhecidas do `link-spritesheet.png`. As coordenadas mapeadas são `(1, 154)` com a dimensão clássica `7x16`.
- **Dano Base:** Inicia chamando o construtor da superclasse `Weapon` aplicando dano base `1`.
- **Comportamento (`onCollect`):** Ao ser encostada, executa `player.giveSword()` habilitando os ataques no próprio player e, em seguida, assinala `setActive(false)` em si mesma.

---

## 3. Gerenciamento de Itens (`GameScreen` e `WarpManager`)

A lógica de verificação de mundo precisa suportar a presença de dezenas de itens.

- **Agrupamento Dinâmico (`QuadrantManager`):** Os itens ativos são carregados sob demanda baseados no quadrante em que o jogador está, evitando processamento desnecessário de todo o mapa.
- **Loop de Colisão e Despawn (`processItems`):** Na classe `GameScreen`, o método auxiliar `processItems(delta)` varre os itens ativos do mapa. É calculada a distância escalar entre o `Vector2` central do `Player` e o do `Item`.
- Caso o jogador encoste (raio estipulado provisoriamente em `12f` pixels de colisão), o item invoca seu `onCollect`, aplica seus efeitos localmente e é inserido em uma fila de remoção (`itemsToRemove`) limpa imediatamente do mapa, prevenindo coletas duplicadas.
- **Injeção de Itens em Cavernas:** Itens especiais em interiores são dinamicamente instanciados pelo `WarpManager` (ex: `WoodenSword`) no momento em que o jogador é teletransportado para uma sala que ainda não foi limpa.

---

## 4. Evolução do Estado do Jogador (`Player`)

O estado da classe `Player.java` atua como consumidor dos efeitos do item.

- A presença do inventário/habilidade é demarcada pela flag `hasSword`.
- A entrada de botões de combate (`SPACE` ou `Z`) encontra-se agora envolvida em um bloco lógico `&& hasSword`, garantindo que os ataques `startAttack()` fiquem bloqueados nativamente até que a espada correspondente seja consumida pelo `onCollect`.

---

## 5. Próximos Passos (Expansão do Sistema)

Essa base permite rápidas inserções para futuras mecânicas, por exemplo:
- **`Rupee` (Dinheiro):** Uma classe `Rupee` pode ser criada estendendo `Item`. Seu `onCollect` simplesmente rodará `player.addMoney(1)`.
- **`Heart` (Vida):** O `onCollect` executa `player.heal(1)`.
- **Inventário Complexo:** Com o escalonamento do projeto, o `onCollect` da `Weapon` pode repassar a arma inteira para um `List<Weapon> inventory` no `Player`, permitindo a troca entre diferentes tipos de armas via menus ou atalhos (Espada Branca, Espada Mágica, etc).
