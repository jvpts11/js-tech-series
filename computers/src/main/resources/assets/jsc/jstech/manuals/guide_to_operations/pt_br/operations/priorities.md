---
section: operations
order: 136
icon: jsc:mainframe
---
# Prioridades

Toda Operação tem uma prioridade, que decide qual Operação em espera recebe uma fila livre primeiro. Há cinco níveis, do mais baixo ao mais alto. Uma nova Operação começa em MEDIUM, a menos que você escolha outro nível.

> [!DEFINE] prioridade
> O nível, de LOW a HIGH, que decide qual Operação em espera recebe uma fila livre primeiro.

Quando duas Operações têm o mesmo nível, a que chegou primeiro vai primeiro.

Table: Os cinco níveis

| Nível | Palavra-chave em IQL | Rótulo curto |
| --- | --- | --- |
| `LOW` | `low` | LOW |
| `MEDIUM_LOW` | `medium_low` | MED- |
| `MEDIUM` | `medium` | MED |
| `MEDIUM_HIGH` | `medium_high` | MED+ |
| `HIGH` | `high` | HIGH |

As palavras-chave não diferenciam maiúsculas de minúsculas, e você pode escrever medium-low ou medium-high com hífen. A palavra-chave normal significa MEDIUM. O rótulo curto é o que as listas compactas mostram.

> [!DEFINE] envelhecimento
> A subida do nível de uma Operação em espera: um degrau a cada período em que ela espera por uma fila.

Uma Operação pronta que não recebe uma fila livre sobe um nível a cada 30 segundos de espera. Por padrão, isso são 600 ticks. Ela não sobe além de HIGH, então trabalhos de baixa prioridade são adiados, mas nunca ficam esquecidos para sempre. A configuração do mod define esse período, e um período de 0 desliga o envelhecimento.

O envelhecimento não muda o nível que você escolheu. A cada tick, o Mainframe calcula o nível que usa naquele tick: o nível escolhido, mais o que o envelhecimento acrescentou.

## Alterando uma prioridade

1. Abra a aba Processos do Gerenciador de Rede e clique na linha da Operação.
2. Pressione > para subir o nível um degrau, ou < para descê-lo.

Em IQL, termine uma instrução com PRIORITY e um nível, como SELECT 64 iron_ingot PRIORITY HIGH. No programa de rede, a janela pop-up de uma requisição tem uma opção PRIORITY que sobe ou desce o nível um degrau por vez. Um CRAFT pega seu nível da instrução ou da sua requisição ([](jsc:asking_for_a_craft)).

## Se algo der errado

> [!PROBLEM] Uma Operação está WAITING, e aumentar o nível dela não muda nada.
> Uma Operação em espera aguarda uma retenção sobre itens, não uma fila. Espere a retenção terminar, ou use UNLOCK no item ([](jsc:lock_unlock)).

> [!PROBLEM] O resultado diz que PRIORITY não é válido em uma leitura.
> Tire PRIORITY da QUERY. Ele define o nível de uma Operação, e uma QUERY apenas lê ([](jsc:clauses_priority_if_with)).

> [!PROBLEM] O resultado diz que o nível de prioridade é desconhecido e lista os cinco nomes.
> Escreva um dos cinco nomes da tabela, como HIGH.

> [!SEE] jsc:states_and_queues, jsc:iql
