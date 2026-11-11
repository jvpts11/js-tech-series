---
section: bus_settings
order: 127
---
# Manter, máximo, prioridade e modo

Cinco configurações decidem quanto o seu Barramento move e quando. KEEP e MAX definem as quantidades. PRIORITY decide quem vai primeiro quando barramentos disputam. MODE e POWER decidem se um Barramento move algo.

Table: Os ajustes de um barramento

| Ajuste | O que faz |
| --- | --- |
| KEEP | Quantos ficam no baú num Barramento de Importação, ou até quanto o baú é preenchido num Barramento de Exportação, de 0 a 1.000.000. |
| MAX | O máximo que um movimento leva, onde zero significa o quanto a velocidade permite em um segundo. |
| PRIORITY | Quem vai primeiro quando dois barramentos querem a mesma coisa, de -99 a 99 na janela, só em barramentos Standard ou superiores. |
| MODE | CONTINUOUS move o tempo todo, e ON DEMAND move só enquanto o bloco do cabo tiver um sinal de redstone. |
| POWER | ON deixa o barramento se mover, e OFF o para. |

## KEEP e MAX

KEEP funciona de maneiras opostas nos dois barramentos. Um Barramento de Importação deixa essa quantidade de itens no baú para o qual está voltado e pega só o que estiver acima desse número. Um Barramento de Exportação enche o baú até esse número e então para.

MAX é o máximo que um movimento leva, não um total. Zero significa sem limite: o Barramento move o quanto a sua velocidade permite em um segundo, que são 20 ticks. A janela mostra "qualquer" com a nota "de cada vez" quando MAX é zero.

## Prioridade

Cada barramento que quer algo avisa isso com a sua prioridade, a cada tick em que quer. Um barramento espera enquanto um barramento de prioridade mais alta na mesma rede quis a mesma coisa nos últimos 2 ticks. Prioridades iguais passam as duas. A prioridade não torna um barramento mais rápido. Ela só decide quem vai primeiro ([](jsc:priorities)).

Um Barramento de Exportação quer o item que envia. Um Barramento de Importação quer espaço na rede, e conta como querendo isso enquanto a rede não tiver tido espaço para os seus movimentos nos últimos 40 ticks.

## Modo e energia

Um barramento em ON DEMAND verifica o bloco do seu cabo à procura de um sinal de redstone. Um barramento em CONTINUOUS não verifica. Um barramento em OFF não se move, qualquer que seja o seu modo. MODE e POWER existem em todo Barramento de Importação e de Exportação, e não no Barramento de Armazenamento Externo ([](jsc:external_storage_bus)).

## Manter e máximo por item

Um barramento Transition não tem manter nem máximo para o barramento inteiro. Cada item do seu filtro tem o seu próprio manter e máximo, mostrados como uma linha na janela. Um item com manter ou máximo igual a zero usa o valor do barramento inteiro, e não existe esse valor num barramento Transition, então zero significa sem manter e sem limite.

## Se algo der errado

> [!PROBLEM] O barramento não se move de jeito nenhum.
> Verifique POWER: um barramento em OFF para. Depois verifique MODE: ON DEMAND precisa de um sinal de redstone no bloco do cabo.

> [!PROBLEM] Um barramento de prioridade mais baixa espera.
> Um barramento de prioridade mais alta na mesma rede quis a mesma coisa há pouco. Aumente a prioridade deste barramento ou diminua a do outro.

> [!PROBLEM] A janela não tem a linha PRIORITY.
> Só barramentos Standard ou superiores têm prioridade. Os barramentos Vintage, Legacy e Transition não.

> [!PROBLEM] A janela mostra um número diferente do que você digitou.
> A janela mantém KEEP e MAX entre 0 e 1.000.000, e PRIORITY entre -99 e 99.

> [!SEE] jsc:bus_conditions, jsc:bus_filters_and_tags
