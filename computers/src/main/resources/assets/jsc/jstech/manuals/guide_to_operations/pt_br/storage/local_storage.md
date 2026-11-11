---
section: storage
order: 119
icon: jsc:disk_vaultis_trench_20m
---
# Armazenamento local

Um disco não carrega seu conteúdo dentro do item. A rede guarda o que os discos contêm em um único armazenamento para o save inteiro, e cada item de disco carrega apenas um número que encontra a sua parte desse armazenamento. Assim, o que um disco contém viaja com ele: tire-o de um computador e leve-o para outra máquina, um rack ou outra dimensão.

O que um disco contém é contado nas mesmas unidades de tudo na rede: itens, fluidos e químicos ([](jsc:items_as_data)). Quando um disco tem algo, sua dica de ferramenta mostra uma linha que começa com "Used".

Cada computador guarda os próprios discos como armazenamento local, e esse armazenamento funciona sem nenhuma rede. O computador mostra o seu espaço em slots de armazenamento.

> [!DEFINE] Slot de armazenamento
> Uma unidade que o jogo usa para mostrar o espaço de um computador, 64 itens cada. Não é um limite do que um disco guarda.

Table: Slots de armazenamento por computador

| Computador | Slots de disco | Slots de armazenamento mostrados |
| --- | --- | --- |
| Computador Pessoal | 2 | até 18 |
| Computador de Fabricação | 2 | até 18 |
| Computador de Gerenciamento de Cluster | 2 | até 18 |
| Mainframe | 4 | até 27 |

> [!WARNING] Formatar um disco apaga tudo nele: os itens que a rede guarda ali, e seus arquivos e sistemas instalados. Retire antes o que você precisa.

Tirar um disco de um computador não o apaga. O disco mantém os seus dados para a próxima máquina em que for colocado. Na baia de um servidor, o modo RAID do array decide o que acontece com esses dados ([](jsc:raid_arrays)).

## Quem pode ler um disco

A rede não lê os discos de um computador a menos que você permita. Cada disco tem um compartilhamento público: a parte dele que a rede pode ler, medida em por mil (partes de mil). Um disco novo é totalmente privado, então seu compartilhamento público começa em 0 por mil.

> [!DEFINE] Por mil
> Partes de mil: 1.000 é o disco inteiro, 100 é um décimo dele.

Table: Compartilhamento público de um disco

| Compartilhamento, em por mil | O que a rede faz |
| --- | --- |
| `0` | Não lê nada deste disco. Este é o padrão. |
| `500` | Lê metade do disco. A outra metade continua privada. |
| `1000` | Lê o disco inteiro. |

Em um computador, você define o compartilhamento público do seu disco de sistema no programa Configurações, em Rede, como "Compartilhamento público do disco de sistema" ([](jsc:this_pc_and_desktop_tools)). Cada passo o altera em 5%, o que equivale a 50 por mil.

A rede pode retirar itens da parte pública de um computador, mas nunca coloca itens nela. Os discos de um servidor são sempre totalmente públicos.

No Gerenciador de Rede ([](jsc:network_manager)), a linha de cada computador mostra o compartilhamento dele como "Private". Então "Private 90%" significa que um décimo desse disco é público.

## Se algo der errado

> [!PROBLEM] O Gerenciador de Rede diz "Private 100%" para o seu computador.
> O disco de sistema dele não compartilha nada. Aumente o compartilhamento público do disco de sistema nas Configurações, em Rede.

> [!SEE] jsc:items_as_data, jsc:personal_computers, jsc:mainframes, jsc:disks
