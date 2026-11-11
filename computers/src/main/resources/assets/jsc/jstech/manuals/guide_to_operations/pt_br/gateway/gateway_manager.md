---
section: gateway
order: 165
---
# O Gerenciador de Gateway

O Gerenciador de Gateway é um programa de área de trabalho. Ele lista os Gateways de Rede nas portas deste computador, e é ali que você define o que o ComputerCraft pode fazer por meio de cada um. É um programa da era Legacy ([](jsc:eras)).

> [!DEFINE] Teto
> A prioridade mais alta que qualquer requisição de um programa ComputerCraft pode ter.

> [!DEFINE] Limite de chamadas
> O número de chamadas por tick do jogo que o Gateway atende para um programa ComputerCraft.

Selecione um Gateway na lista à esquerda, e depois use as abas e os botões à direita. Até que um Gateway esteja conectado a uma das portas deste computador, a lista diz nenhum conectado ainda.

Table: As quatro abas

| Aba | O que mostra ou define |
| --- | --- |
| Status | O link, o Mainframe, o orçamento, as chamadas atendidas e as últimas linhas do log. |
| Permissões | As quatro configurações: leitura, operações, teto de prioridade e chamadas por tick. |
| Computadores | Os computadores ComputerCraft conectados ao Gateway, com ID, LABEL, STATE, AGENT e VISTO POR ÚLTIMO. |
| Log | O que o Gateway fez, com as colunas WHEN, WHO e RESULT. |

A aba Status tem dois cartões. ESTE LADO diz "Conectado a" e o nome do host, ou "Não conectado", e diz se o Mainframe está online ou offline. LADO DO COMPUTERCRAFT diz CC: Tweaked com a sua versão, ou que CC: Tweaked não está instalado.

A linha de orçamento mostra a parcela de cada tick que as chamadas do Gateway tomam dos programas do host, como "Orçamento: 12% de um tick".

## Os botões

Table: Os botões

| Botão | O que faz |
| --- | --- |
| Renomear | Abre a caixa de diálogo RENOMEAR GATEWAY, com APPLY e CANCEL. |
| Identificar | Pisca as luzes do Gateway por 3 segundos. |
| Limpar buffer para a rede | Envia os itens do buffer para a rede. |
| Abrir o log | Abre o log do Gateway. |

Na aba Computadores, Ligar, Reiniciar, Desligar e Evento de teste atuam nos computadores ComputerCraft conectados ao Gateway.

1. Abra o Gerenciador de Gateway no computador que tem o Gateway em sua porta.
2. Selecione o Gateway na lista à esquerda.
3. Abra a aba Permissões e ative as opções de que você precisa.
4. Escolha o teto de prioridade e as chamadas por tick.
5. Abra a aba Status para verificar o link e o Mainframe.

## Se algo der errado

> [!PROBLEM] A mensagem "selecione um gateway primeiro" aparece.
> Nenhum Gateway está selecionado. Clique em um na lista à esquerda e depois pressione o botão de novo.

> [!PROBLEM] A mensagem "CC: Tweaked não está instalado" aparece.
> O lado do ComputerCraft está faltando. Instale o CC: Tweaked para usá-lo.

> [!PROBLEM] A mensagem diz que uma ação precisa do agente no computador ComputerCraft.
> A ação precisa de um programa nesse computador ComputerCraft, e nenhum está lá. Tente uma ação que não precise de um.

> [!PROBLEM] A mensagem "nenhum Mainframe nesta rede" aparece.
> As operações precisam de um Mainframe em execução. Coloque um na rede ([](jsc:mainframes)).

> [!PROBLEM] A mensagem "o buffer está vazio" aparece depois de Limpar buffer para a rede.
> Não há nada no buffer para enviar. Coloque itens nele você mesmo, ou faça um programa ComputerCraft puxá-los para dentro.

> [!SEE] jsc:gateway_permissions, jsc:gateway_command
