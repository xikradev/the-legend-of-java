# Especificação Técnica: Câmera e Colisões (The Legend of Java)

Este documento descreve o funcionamento técnico dos sistemas de Câmera e Colisões implementados no jogo, inspirados nas mecânicas clássicas de *The Legend of Zelda* do NES.

---

## 1. Sistema de Câmera (Navegação por Quadrantes)

A câmera do jogo não segue o personagem de forma contínua (smooth scrolling). Em vez disso, ela permanece estática enquanto o personagem se move dentro de uma área visível e "salta" (transição instantânea) para a próxima tela quando o personagem ultrapassa os limites do quadrante atual.

### 1.1. Dimensões e Viewport
- **Viewport**: Utiliza-se um `FitViewport` com a resolução interna clássica de **256x176 pixels**.
- **Grid do Mapa**: O mapa global é uma grande imagem (`zelda-overworld.png`). No entanto, o mapa é logicamente dividido em "telas" (quadrantes) de 256x176 pixels.
- **Bordas (Borders)**: Para separar visualmente cada tela, a imagem original possui linhas verdes de 1 pixel de espessura separando as áreas. Portanto, o tamanho matemático de cada "bloco" computado na memória é de **257x177 pixels** (256 + 1 de borda).

### 1.2. Sistema de Coordenadas Invertido (Eixo Y)
O grande desafio técnico resolvido na câmera é o conflito de sistemas de coordenadas:
1. **LibGDX**: O ponto `(0,0)` fica no canto **inferior-esquerdo** da tela e cresce para cima.
2. **Tiled Map (Image Layer)**: O ponto `(0,0)` lógico da imagem começa no topo e cresce para baixo. 

O mapa base (`zelda-map.tmx`) tem uma altura total de **4000 pixels** (20 tiles de 200px). Como a camada de imagem é pendurada no topo do mapa, a linha 0 real do grid de quadrantes começa no `Y = 4000` do LibGDX e vai descendo.

### 1.3. Lógica Matemática da Câmera
Na classe `CameraManager` (método `update`), calculamos em qual quadrante o personagem está a cada frame:
```java
// Eixo X funciona normalmente (da esquerda pra direita)
int coluna = (int) (player.getPosition().x / 257f);

// Eixo Y é invertido (calculado a partir da distância do topo do mundo: 4000px)
float distFromTop = 4000f - player.getPosition().y;
int linha = (int) (distFromTop / 177f);
```

Uma vez encontrada a `coluna` e a `linha`, o centro da câmera é atualizado para "travar" exatamente no meio do quadrante encontrado, mantendo o alinhamento pixel-perfect com as linhas verdes da imagem de fundo.

---

## 2. Sistema de Colisão

O sistema de colisão utiliza a técnica de AABB (Axis-Aligned Bounding Box) resolvendo os eixos X e Y separadamente para evitar travamentos nas quinas.

### 2.1. Extração de Dados do Tiled Map
Os limites físicos do mapa não são definidos por "Tiles Sólidos", mas sim por formas geométricas desenhadas livremente sobre a imagem de fundo:
- Os retângulos de colisão são lidos dinamicamente no início do jogo da camada `ObjectGroup` chamada `"colisoes"`.
- Apenas objetos do tipo `RectangleMapObject` são processados e convertidos para uma lista de `Rectangle` (da biblioteca `com.badlogic.gdx.math.Rectangle`), que é então enviada ao método `update` do `Player`.

### 2.2. Hitbox do Personagem (Efeito Pseudo-3D)
Em jogos *top-down* estilo Zelda, a colisão não envolve o sprite inteiro do personagem. Isso permite que a cabeça e os ombros do herói passem por cima das paredes ("overlaps"), criando uma ilusão de profundidade.

Para implementar isso:
- O sprite do Player tem tamanho de **16x16 pixels**.
- O método `getHitbox()` retorna um retângulo menor: `width: 12, height: 8`, focado apenas nos "pés" do personagem (com um offset horizontal de +2 pixels para centralizar).

### 2.3. Resolução de Colisão
A detecção de colisão no `Player.java` ocorre em dois passos (movimento em eixos separados):
1. **Eixo X**: O personagem move sua posição `x` baseado na velocidade atual. Se a nova posição sobrepor (overlap) algum retângulo do cenário, a posição `x` é desfeita para a posição anterior.
2. **Eixo Y**: O mesmo ocorre independentemente para o eixo `y`.

Essa abordagem garante que, se o personagem colidir diagonalmente contra uma parede, ele "deslizará" pela parede no eixo livre, em vez de travar completamente.

---

## 3. Ferramenta de Debug Visual (ShapeRenderer)

Para garantir que a lógica complexa de coordenadas e a precisão do Tiled Map estejam corretas, foi adicionado um sistema de debug nativo usando `ShapeRenderer`.

Ele injeta linhas na projeção atual da câmera desenhando:
- **Linhas Vermelhas**: Todos os retângulos invisíveis de colisão puxados do TMX.
- **Linha Azul**: O exato retângulo do Hitbox dos pés do personagem.

Isso provou ser essencial para alinhar a discrepância visual entre as posições calculadas e a imagem de fundo.

---

## 4. Otimização por Particionamento Espacial (Quadrantes)

Para manter o desempenho ideal em um mapa aberto grande como o *overworld*, o jogo utiliza uma técnica de **Particionamento Espacial** estruturada em quadrantes, garantindo que objetos invisíveis não consumam CPU.

### 4.1. Estrutura de Quadrantes (`Quadrant` e `QuadrantManager`)
Em vez de iterar sobre todas as entidades e blocos de colisão do mapa a cada frame, a lógica foi encapsulada no módulo `core.world`:
- **`Quadrant`**: Representa logicamente um "bloco de tela" de dimensões `257x177` pixels (a mesma proporção usada pela câmera). Cada objeto `Quadrant` mantém sua própria lista de colisões (`Rectangle`), itens e futuramente inimigos.
- **`QuadrantManager`**: Durante a inicialização (`loadFromMap`), esta classe processa todos os objetos do Tiled Map. Ela calcula matematicamente (usando as mesmas fórmulas de `coluna` e `linha` da câmera) a qual quadrante cada retângulo pertence e os distribui. Retângulos que interceptam bordas são duplicados nos quadrantes afetados para evitar problemas de colisão durante as transições de tela.

### 4.2. Carregamento em Buffer (Adjacências)
Na classe `GameScreen`, no método `render()`, o jogo consulta o `QuadrantManager` para obter os objetos ativos. Para evitar *pop-in* de entidades e garantir que o *Player* não atravesse paredes ao pisar exatamente na linha de transição de duas telas, o sistema utiliza um **Buffer de Adjacência**:
- O `QuadrantManager` varre não apenas o quadrante atual do *Player* `(x, y)`, mas também os 8 quadrantes vizinhos (de `x-1, y-1` a `x+1, y+1`).
- As colisões e itens retornados pertencem apenas a esse bloco de 3x3 telas centradas no jogador, reduzindo drasticamente as checagens matemáticas no método `player.update()`.
