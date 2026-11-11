---
section: autocrafting
order: 147
---
# O Gerenciador de Fabricação

O Gerenciador de Fabricação é o programa que carrega receitas nas placas e nas interfaces de um Computador de Fabricação, e que mostra o que as peças de fabricação dele estão fazendo. Ele só roda em um Computador de Fabricação e não vem com nenhum sistema. Instale-o pelo CD dele, ou, numa área de trabalho Linux, pelo Mirror, em um sistema com janelas: o Frames XP ou mais novo, ou uma área de trabalho Linux, FreeBSD ou UNIX ([](jsc:programs)).

Você precisa de uma Placa de Fabricação para gerenciar receitas, e só um Computador de Fabricação carrega receitas ([](jsc:crafting_computers)).

O Gerenciador de Fabricação tem duas abas, "Receitas" e "Interfaces".

## A aba Receitas

O painel da esquerda lista os arquivos .craft de uma mídia em uma unidade vinculada. Sem mídia em uma unidade vinculada, o painel mostra "Mídia removível: nenhuma" e pede para inserir um disco em uma unidade vinculada. Uma mídia sem arquivos de padrão mostra "Nenhum arquivo .craft".

O painel da direita lista os lugares onde o computador guarda receitas: a ROM de cada placa, e cada interface com as receitas da sua máquina. As receitas de bancada vão para a ROM de uma placa, e as receitas de máquina e de pipeline vão para uma interface.

Table: Os botões da aba Receitas

| Botão | O que faz |
| --- | --- |
| Carregar | Coloca a receita selecionada no lugar escolhido em "into", ou no primeiro lugar com espaço. |
| Carregar tudo | Carrega todas as receitas da mídia. |
| Download | Grava as receitas de volta na mídia. |
| Remover | Remove a receita selecionada. |
| Mover para | Move a receita selecionada para outro lugar. |

## Carregando uma receita de um disco

1. Insira o disco com os arquivos .craft em uma unidade vinculada.
2. Selecione a receita no painel da esquerda.
3. Escolha o lugar em "into", ou deixe no primeiro lugar com espaço.
4. Pressione Carregar.

## A aba Interfaces

A aba Interfaces tem uma linha para cada Interface de Fabricação no cabo. A linha de resumo acima das linhas informa quantas das interfaces do cabo as placas comandam, e o número de Placas de Fabricação.

Table: As colunas da aba Interfaces

| Coluna | O que mostra |
| --- | --- |
| INTERFACE | O nome da interface. |
| MÁQUINA | A máquina em frente da interface, ou "Não encosta em nenhuma máquina" quando não há nada na frente. |
| PADRÕES | As receitas que ela guarda, como dois números: as receitas guardadas e a capacidade. |
| MODO | "Exclusiva" ou "Não exclusiva", como são os seus modos ([](jsc:crafting_interfaces)). |
| ESTADO | "Ociosa" ou "Pausada". |
| JOBS | "Auto" quando o limite é 0, senão o próprio limite. |

Clique no modo para trocá-lo, e clique nas tarefas para aumentá-las em uma. As tarefas sobem até 16, depois voltam para Auto, e um clique com o botão direito as diminui. A tela da própria interface permite até 64 tarefas. Os botões Pausar e Retomar pausam e reiniciam uma interface.

Cada placa aparece listada com as interfaces que comanda e com o quanto a ROM dela está cheia. Aparece a linha "N a mais no Cabo de Fabricação aguardam, sem comando de nenhuma, até que outra placa seja colocada" quando algumas interfaces não têm uma placa para comandá-las.

## Se algo der errado

> [!PROBLEM] A tela diz "É necessária uma Placa de Fabricação para gerenciar receitas."
> Coloque uma Placa de Fabricação no Computador de Fabricação.

> [!PROBLEM] A tela diz "Nenhuma Interface de Fabricação no cabo de fabricação deste computador."
> Coloque uma Interface de Fabricação no cabo de fabricação deste computador.

> [!PROBLEM] A tela diz "Nenhuma placa e nenhuma interface".
> Dê ao computador uma Placa de Fabricação, e coloque interfaces no cabo dele.

> [!PROBLEM] A tela diz "Mídia removível: nenhuma".
> Insira um disco em uma unidade vinculada.

> [!PROBLEM] A tela diz "Nenhum arquivo .craft".
> A mídia não tem nenhum padrão. Grave o padrão de novo, ou insira a mídia certa.

> [!SEE] jsc:crafting_computers, jsc:crafting_interfaces, jsc:crafting_cards, jsc:drives, jsc:pattern_encoders, jsc:input_routers_and_receiving_buses
