---
section: operations
order: 137
icon: jsc:mainframe
---
# Cancelamento e o log

Você pode parar uma Operação enquanto ela executa ou espera. A aba Processos do Gerenciador de Rede lista as Operações em andamento. Clique em uma linha, e o seu pop-up mostra o tipo, o item, a quantidade, o tempo, as partes do seu trabalho e um botão Cancelar.

1. Abra o Gerenciador de Rede e a sua aba Processos.
2. Clique na linha da Operação que você quer parar.
3. Pressione Cancelar na janela pop-up.

Cancelar devolve o que a Operação tinha em uso. Depois a Operação é concluída como DISCARDED no próximo tick, e o log a registra.

O Prompt de Comando tem o mesmo controle pelo comando interac. As duas formas da próxima tabela atuam sobre as Operações em andamento.

```table
{"caption": "Os comandos do Prompt de Comando", "rows": [[{"words": "Lista as Operações em andamento, com os seus identificadores (IDs)"}, {"literal": "interac ops"}], [{"words": "Cancela uma, pelo início do seu ID"}, {"literal": "interac cancel <id>"}]]}
```

O Mainframe guarda um log das Operações que foram concluídas. Ele mantém as últimas 32 Operações, da mais nova para a mais antiga. A aba Log do Gerenciador de Rede a mostra sob o título "Log da rede".

Table: O que uma linha do log mostra

| Parte da linha | O que ela informa |
| --- | --- |
| Tipo | O tipo de Operação, como SELECT ou MOVE. |
| Item | O item em que ela trabalhou. |
| Quantidade | Quanto foi movido, de quanto foi pedido. |
| Status | O estado dela, como PENDING, COMPLETED ou DISCARDED. |
| Motivo | A linha com o motivo, quando há um. |
| Esperou e executou | Quantos ticks esperou por uma fila, e quantos executou. |

A aba Estatísticas mostra as Operações concluídas na última hora. Ela diz "Nenhuma Operation concluída na última hora." quando nenhuma foi.

Quando o Mainframe é desligado, perde o seu sistema operacional ou encontra um conflito de rede, tudo o que está em andamento termina. Cada Operação é concluída como DISCARDED e registrada, e o que ela tinha em uso é devolvido. Um conflito de rede acontece quando dois Mainframes reivindicam a mesma rede ([](jsc:network_identity)).

Um salvamento mantém as Operações em andamento, e elas retomam na próxima inicialização. Por padrão, um salvamento com mais de 24 horas é descartado e registrado. A configuração do mod pode mudar esse tempo.

## Se algo der errado

> [!PROBLEM] A Operação ainda está na lista depois que eu a cancelei.
> A Operação é finalizada como DISCARDED no próximo tick, e não na hora. Espere um tick, e a lista é atualizada.

> [!PROBLEM] Minha Operação sumiu depois de um recarregamento.
> O salvamento tinha mais de 24 horas, então o Mainframe o descartou e o registrou. Procure-o no log.

> [!PROBLEM] O log diz "Nenhuma Operation registrada ainda."
> O log fica vazio até que uma Operação seja concluída. Espere até que alguma termine.

> [!SEE] jsc:operation_states, jsc:network_identity
