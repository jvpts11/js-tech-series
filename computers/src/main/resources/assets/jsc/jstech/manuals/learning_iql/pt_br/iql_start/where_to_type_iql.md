---
section: iql_start
order: 363
---
# Onde você digita IQL

Você pode digitar IQL em cinco lugares. Cada um precisa que a rede esteja pronta primeiro, e cada um lê as instruções de um jeito um pouco diferente. A lista abaixo nomeia os cinco lugares, e as partes seguintes mostram como usar cada um.

> [!DEFINE] script
> Um arquivo de instruções IQL que a rede executa uma após a outra.

Table: Os cinco lugares

| Lugar | Como você chega nele | Para que serve |
| --- | --- | --- |
| O comando iql | Digite iql no prompt, seguido de uma instrução | Uma instrução de cada vez |
| IQL Server Management Studio (ISMS) | Abra o programa | Escrever e rodar scripts inteiros |
| `run` | Digite run e o nome de um arquivo, no MC-NET | Executar um arquivo de instruções |
| A classe Iql do Sigma | Escreva um programa em Sigma que a chame | Programas que perguntam à rede |
| Gerenciador de Automação | Abra o programa e preencha o formulário dele | Trabalho que a rede repete para você |

## O comando iql no prompt

Digite iql, um espaço, depois uma instrução, e pressione [[Enter]]. O comando também responde pelos nomes operation, op e sql, então op SELECT 64 iron_ingot faz o mesmo. Ele precisa de uma rede de dados por baixo do computador. Uma leitura em um computador sem rede responde "não está em uma rede".

Uma leitura responde com linhas, no máximo 64. Uma escrita responde com uma linha, como "SELECT enfileirado: 64 Iron Ingot -> armazenamento local". O item é nomeado como o jogo o mostra, como Iron Ingot, e não pelo nome interno.

> [!DEFINE] armazenamento local
> O armazenamento próprio deste computador, para onde vão os itens que você tira da rede.

## IQL Server Management Studio (ISMS)

O IQL Server Management Studio (ISMS) é o programa para escrever scripts. Um script são várias instruções em um arquivo, com espaço para editá-las. O ISMS abre e salva arquivos que terminam em .iql.

No ISMS, cada instrução termina com um ponto e vírgula. Um comentário começa com dois traços e vai até o fim da linha. Pressione as teclas abaixo para rodar o que você escreveu.

Table: As teclas do ISMS

| Tecla | O comando, como o menu o escreve |
| --- | --- |
| `F5` | Executar |
| `Ctrl+E` | Executar Seleção |
| `Alt+Break` | Cancelar Execução da Consulta |
| `F1` | Referência da IQL |

Um script pode pedir valores antes. Os parâmetros dele são escritos entre colchetes angulares, com um tipo e um padrão, e a caixa de diálogo "Especificar Valores para Parâmetros de Modelo" lista cada um como nome (tipo).

## run, para um arquivo

O comando run executa um arquivo de IQL em um computador que roda o MC-NET ([](jsc:mc_net)). Digite run, um espaço e o nome do arquivo. O computador precisa de um disco, e o run aceita só arquivos que terminam em .iql.

run é mais rigoroso que o ISMS. Ele lê uma instrução por linha, pula toda linha que começa com dois traços e aceita instruções sem ponto e vírgula. Ele não pede valores. Recusa as leituras, QUERY e COUNT. Ele confere o arquivo inteiro antes de a primeira linha rodar, então uma linha ruim não move nada.

## A classe Iql do Sigma

Sigma, uma das linguagens de programação dos computadores, também pode consultar a rede. A classe dele Iql, no namespace System.Network, tem quatro membros.

Table: Os membros da classe Iql

| Membro | O que devolve |
| --- | --- |
| `Run` | Um IqlResult, com Ok, Message e Rows |
| `Query` | As linhas de uma leitura |
| `Exec` | Um IqlResult. Ele também aceita uma lista de strings. |
| `RunFile` | Um IqlResult, a partir de um arquivo |

A forma exata de chamá-la está em [Iql](jsc:iql_library), no Livro do Sigma.

## Gerenciador de Automação

O Gerenciador de Automação ([](jsc:automation_manager)) não aceita IQL digitada. O formulário dele faz trabalho que se repete: Reabastecer abaixo, Fabricação em Lote, Mover ou IQL. Para o tipo IQL, você escolhe um arquivo .iql do disco do sistema do Mainframe, e define com que frequência ele roda no campo A cada (30s).

Digitar IQL exige que o Midsoft IQL Server esteja rodando. Você o inicia e para com o comando iqlengine ([](jsc:engine_services)).

## Se algo der errado

> [!PROBLEM] Você digita só iql, e a resposta mostra uma linha de uso com um exemplo.
> Digite uma instrução depois de iql.

> [!PROBLEM] A resposta diz "não está em uma rede".
> O computador não tem uma rede de dados por baixo. Conecte-o com um cabo de dados ([](jsc:data_cables)).

> [!PROBLEM] run diz "só arquivos .iql podem ser executados".
> Renomeie o arquivo para que termine em .iql, ou escreva as instruções em um novo arquivo .iql.

> [!PROBLEM] run diz "QUERY/COUNT não são suportados por 'run'".
> run não aceita leituras, QUERY nem COUNT. Digite a leitura no prompt, com iql ou operation.

> [!PROBLEM] run diz "erro de sintaxe em", seguido de uma linha.
> O arquivo inteiro é verificado primeiro, então nenhuma linha rodou. Corrija a linha que a mensagem aponta.

> [!PROBLEM] O ISMS diz "Nenhum Midsoft IQL Server compatível foi encontrado nesta rede."
> Confirme que o Mainframe está ligado e que o Midsoft IQL Server está instalado e rodando ([](jsc:engine_services)).

> [!PROBLEM] O ISMS diz que o Midsoft IQL Server não está instalado.
> Instale-o pelo Gerenciador de Rede, nos Serviços de Rede dele, como diz a mensagem ([](jsc:network_manager)).

> [!PROBLEM] Uma instrução responde "Network Operations Service indisponível".
> O Midsoft IQL Server está parado. Inicie-o com iqlengine start.

> [!PROBLEM] O Gerenciador de Automação diz "Nenhum motor - instale o Motor de Automação no Mainframe".
> Instale o Motor de Automação no Mainframe, depois tente de novo.

> [!PROBLEM] A resposta diz "as tarefas são mantidas pelo Motor de Automação".
> Instale o Motor de Automação no Mainframe primeiro.

> [!SEE] jsc:isms_studio, jsc:automation_manager, jsc:iql_engines_overview
