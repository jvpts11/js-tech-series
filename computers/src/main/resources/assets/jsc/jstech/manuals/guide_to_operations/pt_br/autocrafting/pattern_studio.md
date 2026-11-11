---
section: autocrafting
order: 143
---
# O Pattern Studio

O Pattern Studio é o programa de área de trabalho em que você escreve padrões. Ele guarda três rascunhos no computador, um para cada aba: "Bancada", "Máquina" e "Multietapas". Um rascunho não é uma receita até estar completo, então você pode deixar um meio escrito e voltar a ele depois.

O Studio não vem com nenhum sistema. Instale-o pelo CD dele, ou, numa área de trabalho Linux, pelo Mirror, em um sistema com janelas: o Frames XP ou mais novo, ou uma área de trabalho Linux, FreeBSD ou UNIX ([](jsc:programs)).

Nada é gasto no Studio. Um clique em uma célula do editor registra uma cópia do que o seu cursor segura, e o seu inventário fica em uma faixa embaixo do editor.

O botão Load funciona somente em um Computador de Fabricação, porque só esse computador carrega receitas ([](jsc:crafting_computers)).

Table: As abas do Studio

| Aba | O que ela grava |
| --- | --- |
| Bancada | Um padrão de bancada: uma grade 3x3 e o seu resultado. |
| Máquina | Um padrão de máquina: as suas entradas, as suas saídas e o seu tempo limite. |
| Multietapas | Um pipeline de etapas, a partir dos seus rascunhos ou de arquivos em uma mídia. |

À direita, a barra lista os arquivos das unidades do computador e do Codificador de Padrões conectado, nas abas "Arquivos" e "Codificador". As dicas da barra dizem "Clique para adicionar um estágio" e "Clique para abrir o arquivo".

## Os botões

Table: Os botões do Studio

| Botão | O que faz |
| --- | --- |
| \+ Bancada | Adiciona o rascunho de bancada como uma etapa da aba multietapas. O Studio responde "Estágio de bancada adicionado". |
| \+ Máquina | Adiciona o rascunho de máquina como uma etapa. O Studio responde "Estágio de máquina adicionado". |
| Gravar | Grava o rascunho na mídia do Codificador de Padrões conectado ([](jsc:pattern_encoders)). |
| Salvar no disco | Grava o rascunho no disco do sistema do computador. O Studio responde "Salvo" e o nome do arquivo. |
| Carregar | Carrega o rascunho na ROM deste Computador de Fabricação, onde ficam guardadas as receitas de bancada ([](jsc:crafting_cards)). |

## Escrevendo um padrão de bancada

1. Abra a aba Bancada.
2. Segure um item no cursor e clique em uma célula da grade para registrar uma cópia dele.
3. Preencha a grade até ela gerar um resultado. A prévia mostra o que a rede usaria.
4. Digite um nome no campo Nome.
5. Pressione Carregar, ou Salvar no disco, para guardar o rascunho.

Os campos Nome e Nota dão nome ao padrão, com até 64 e 256 caracteres. O campo Tempo limite define quantos ticks um padrão de máquina espera, e ele é de pelo menos 1 tick.

A prévia mostra o que a rede usaria para o rascunho. As suas linhas dizem "A rede usaria:" e "Em estoque:". Uma célula que aceita uma tag mostra "Qualquer #" e a tag.

Enquanto você não adiciona uma etapa, a aba multietapas mostra "Nenhum estágio ainda". Cada etapa então mostra o seu número, o seu tipo entre colchetes, "[bancada]" ou "[máquina]", e o seu nome.

Se o Just Enough Items (JEI), o visualizador de receitas, estiver instalado, o botão de transferência de uma receita envia essa receita para o Studio aberto. Uma receita de bancada vai para o rascunho de bancada, e qualquer receita com entradas e saídas vai para o rascunho de máquina.

O Studio responde "Receita colocada na bancada" ou "Receita colocada no rascunho de máquina". Se o Studio não estiver aberto, a mensagem é "Abra o Pattern Studio para transferir receitas".

## Se algo der errado

> [!PROBLEM] A tela diz "O rascunho de bancada não é uma receita".
> A grade não gera resultado. Preencha-a para que exista um.

> [!PROBLEM] A tela diz "O rascunho de máquina precisa de uma entrada e uma saída".
> Adicione pelo menos uma entrada e uma saída.

> [!PROBLEM] O Studio diz "Não é uma receita de máquina" ou "Não é um pipeline".
> O arquivo é do tipo errado para esta aba. Abra um arquivo do tipo que a aba aceita.

> [!SEE] jsc:patterns, jsc:pattern_encoders, jsc:crafting_manager, jsc:crafting_cards
