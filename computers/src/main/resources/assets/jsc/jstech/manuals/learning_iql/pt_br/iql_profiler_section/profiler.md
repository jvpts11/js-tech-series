---
section: iql_profiler_section
order: 397
---
# O IQL Server Profiler

O IQL Server Profiler registra o trabalho da rede conforme ele acontece, uma linha para cada evento. Quando uma fabricação ou uma consulta faz algo inesperado, o Profiler mostra o que a rede fez, linha por linha.

Abra-o pelo menu Ferramentas do estúdio, no item IQL Server Profiler, ou pela área de trabalho. Ele abre na sua própria janela, e não como uma aba do estúdio.

A gravação é feita em um rastreamento: uma sessão que você inicia, pausa, para e salva como arquivo. Cada novo rastreamento recebe um número.

> [!DEFINE] Rastreamento
> Uma sessão de gravação do Profiler. Ela inicia, pausa e para, e suas linhas podem ser salvas em um arquivo.

## O que ele registra

Table: Os eventos que o Profiler registra

| Evento | Grupo | O que registra |
| --- | --- | --- |
| `StatementStarting` | Instruções | uma instrução chega pela entrada da rede |
| `StatementCompleted` | Instruções | uma instrução voltou, com as linhas que leu |
| `OperationCreated` | Operações | o Mainframe assumiu uma Operação |
| `OperationSettled` | Operações | uma Operação terminou: suas movimentações, seu tempo e o motivo da falha, se houve |
| `LockAcquired` | Bloqueios | uma Operação pegou um bloqueio, ou um item foi retido à mão |
| `PlanChosen` | Planos | o motor escolheu como uma fabricação é feita |
| `BusMoved` | Barramentos | um Barramento de Importação ou um Barramento de Exportação moveu itens |

Cada linha tem as colunas EventClass, TextData, Requester, Computer, Items, Duration e StartTime.

## Iniciando, pausando e parando

Table: Os botões do Profiler

| Botão | O que faz |
| --- | --- |
| Novo Rastreamento | Para o rastreamento, esvazia as linhas e numera o novo rastreamento. [[Ctrl]]+[[N]] faz o mesmo. |
| Abrir | Lê um arquivo de rastreamento de volta para a janela. |
| Salvar | Grava as linhas em um arquivo. |
| Iniciar | Inicia o rastreamento, ou o retoma, com os grupos que você escolheu. |
| Pausar | Para de escutar e mantém as linhas. O servidor para de enviar linhas. |
| Parar | Encerra o rastreamento. As linhas permanecem até você escolher Novo Rastreamento ou Limpar. |
| Limpar | Esvazia as linhas. |
| Localizar | Pesquisa as linhas como texto. |
| Eventos | Escolhe os grupos a registrar: Instruções, Operações, Bloqueios, Planos e Barramentos. |

Mudar os grupos enquanto um rastreamento está rodando o reinicia com os novos grupos. Um grupo que não for escolhido não registra nada.

A linha de status mostra "O rastreamento está em execução.", "O rastreamento está pausado." ou "O rastreamento está parado." Um novo rastreamento sem linhas mostra "Nenhum evento ainda. Inicie o rastreamento para registrar o que a rede faz."

## Lendo as linhas

Abaixo da grade, aparecem as linhas de detalhe da linha escolhida. Um evento mostra quem o pediu, como "pedido por: <requester> em <computer>", e quanto tempo levou, como "levou <n> ticks (<n> s)". Vinte ticks formam um segundo. Quando ninguém pediu, o solicitante aparece como "network".

O Profiler guarda no máximo 2.000 linhas. Quando o rastreamento está cheio, as linhas mais antigas são descartadas. A barra inferior mostra "Ln <n>, Col 1" para a linha selecionada, onde Ln é o número da linha, e "Linhas: <n>" para a contagem de linhas.

## Localizando e reproduzindo

Localizar pesquisa as linhas como texto. Localizar Próximo vai para a próxima ocorrência, e Esc fecha a barra de localização. Quando nada mais corresponde, aparece "Nada mais contém <text>."

Reproduzir Comando, no menu Reproduzir, executa de novo uma instrução gravada. Ele só fica ativo quando você selecionou uma linha de StatementStarting ou de StatementCompleted. Ele abre o estúdio e executa a instrução em uma nova consulta.

## Salvando e abrindo

Salvar Como... grava as linhas em um arquivo. O nome do arquivo é o nome do rastreamento, com espaços trocados por sublinhados, e a extensão .log. O arquivo tem uma linha de cabeçalho com os sete nomes de coluna, depois uma linha para cada evento, e seus valores são separados por tabulações.

As linhas de detalhe de cada linha são unidas com " | ", e uma quebra de linha dentro de uma linha vira um espaço.

## Se algo der errado

> [!PROBLEM] A janela diz "Nenhum evento ainda. Inicie o rastreamento para registrar o que a rede faz."
> Pressione Iniciar. O Profiler grava apenas o que acontece depois que o rastreamento começa.

> [!PROBLEM] O rastreamento parou sozinho.
> Um rastreamento termina quando o jogador que o iniciou sai do jogo, e quando o servidor para. Use Novo Rastreamento para iniciar outro.

> [!PROBLEM] Ao salvar, aparece a mensagem "O rastreamento foi salvo em <file>, mas estava longo demais: foi cortado para <n> linhas e <n> linhas ficaram de fora."
> Grave um rastreamento mais curto, ou escolha menos grupos, para que menos linhas sejam mantidas.

> [!PROBLEM] Reproduzir Comando está desativado no menu Reproduzir.
> Selecione primeiro uma linha de StatementStarting ou de StatementCompleted.

> [!PROBLEM] O botão Salvar está desativado.
> Salvar fica desativado até o rastreamento ter linhas. Inicie o rastreamento e deixe alguns eventos chegarem.

> [!SEE] jsc:operations_catalogue, jsc:buses
