---
section: operations
order: 135
icon: jsc:mainframe
---
# Filas e placas de vídeo

Uma fila é uma faixa que move itens para uma Operação por vez. O Mainframe tem uma fila para o seu processador, e mais uma para cada placa de vídeo nele. Um Mainframe com um processador e três placas de vídeo trabalha com quatro filas ao mesmo tempo.

> [!DEFINE] Motor
> O programa do Mainframe que lê IQL e planeja o trabalho da rede.

Os Subframes da mesma rede também emprestam as filas de suas placas de vídeo, se rodarem o mesmo motor. Um Subframe que roda outro motor não empresta nenhuma ([](jsc:subframes)).

> [!DEFINE] Itens por tick (it/t)
> Quantos itens uma fila ou o Mainframe move em um tick do jogo.

Cada fila roda na sua própria velocidade, medida em itens por tick (it/t). A fila do processador roda na capacidade do processador. A fila de uma placa de vídeo roda na menor velocidade entre a capacidade do processador e a velocidade da própria placa. Uma placa fraca, ou uma placa num slot mais antigo, é mais lenta.

Table: A velocidade de cada fila

| Fila | Velocidade |
| --- | --- |
| A fila do processador | A capacidade do processador. |
| A fila de cada placa de vídeo | O menor valor entre a capacidade do processador e a velocidade da placa. |

As filas não dividem a capacidade entre si. Uma segunda placa de vídeo lhe dá uma segunda faixa. Ela não deixa a primeira faixa mais rápida.

O ritmo do Mainframe inteiro é o menor entre dois números. Um é a sua capacidade de orquestração: quantos itens por tick o Mainframe, com os seus Subframes, consegue mover. O outro é o buffer da memória de acesso aleatório (RAM), a memória de trabalho do Mainframe em itens por tick ([](jsc:memory)).

O Gerenciador de Rede mostra as filas em uso, como Filas paralelas, e a Capacidade de orquestração, em itens por tick. Um passo de máquina de uma autofabricação não ocupa nenhuma dessas filas ([](jsc:how_autocrafting_works)).

Você pode fazer a troca a quente de uma placa de vídeo sem perder trabalho. O número de filas muda no lugar, e as Operações continuam rodando.

## Se algo der errado

> [!PROBLEM] A rede parece lenta.
> Olhe primeiro para o Mainframe. O processador e as placas de vídeo dele definem quantas Operações rodam ao mesmo tempo, e com que velocidade ([](jsc:mainframes)).

> [!SEE] jsc:operation_states, jsc:priorities
