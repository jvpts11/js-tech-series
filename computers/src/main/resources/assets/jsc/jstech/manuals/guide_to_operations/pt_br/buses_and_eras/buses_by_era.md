---
section: buses_and_eras
order: 125
---
# Barramentos por era

Cada era de hardware tem os seus próprios barramentos, e os mais novos carregam mais. Você obtém cada barramento nas abas criativas, como acontece com tudo neste mod. Exa e Singularity usam o barramento Advanced.

Table: Itens do barramento por era

| Era | Barramento de Importação | Barramento de Exportação | Barramento de Armazenamento Externo |
| --- | --- | --- | --- |
| Vintage | Barramento de Importação Vintage | Barramento de Exportação Vintage | Barramento de Armazenamento Externo Vintage |
| Legacy | Barramento de Importação Legacy | Barramento de Exportação Legacy | Barramento de Armazenamento Externo Legacy |
| Transition | Barramento de Importação Transition | Barramento de Exportação Transition | Barramento de Armazenamento Externo Transition |
| Standard | Barramento de Importação | Barramento de Exportação | Barramento de Armazenamento Externo |
| Advanced | Barramento de Importação Advanced | Barramento de Exportação Advanced | Barramento de Armazenamento Externo Advanced |

Quanto mais nova a era, mais você pode definir no barramento, e mais rápido ele se move. A tabela mostra a velocidade do barramento de cada era e o que a sua janela pode definir.

```table
{"caption": "Velocidade e recursos por era", "heads": ["Era", "Velocidade", "O que a janela pode definir"], "rows": [[{"words": "Vintage"}, {"amount": 1, "unit": "it/t"}, {"words": "Sem filtro: um Barramento de Exportação envia o único item do seu espaço, e um Barramento de Importação pega o que encontrar primeiro."}], [{"words": "Legacy"}, {"amount": 8, "unit": "it/t"}, {"words": "Um filtro de até 5 itens, SÓ ESTES ou EXCETO ESTES, e um manter e um máximo para o barramento inteiro."}], [{"words": "Transition"}, {"amount": 16, "unit": "it/t"}, {"words": "Um filtro de até 5 itens, cada um com o seu próprio manter e máximo."}], [{"words": "Standard"}, {"amount": 32, "unit": "it/t"}, {"words": "Um filtro, um manter e um máximo para o barramento inteiro, uma prioridade e condições."}], [{"words": "Advanced"}, {"amount": 64, "unit": "it/t"}, {"words": "Tudo o que Standard tem, mais até 4 tags e correspondência aproximada."}]]}
```

> [!DEFINE] Filtro
> A lista de até cinco itens, fluidos ou substâncias químicas que um barramento recebe ordem de mover.

> [!DEFINE] Tag
> Um grupo de itens com um nome comum, como c:ores. Um barramento pode listar uma tag no lugar de itens individuais.

> [!DEFINE] Manter
> Quantos itens um barramento deixa no bloco para o qual está voltado (Importação), ou até quanto ele enche esse bloco (Exportação).

> [!DEFINE] Máximo
> O máximo de itens que um movimento leva. Zero significa quantos a velocidade permitir.

A velocidade de um barramento é o menor de dois números: o que a sua era permite e o que o seu cabo transporta. Um Barramento de Importação Standard em um cabo de acesso Standard move 32 itens por tick. Em um cabo de acesso Legacy, move 16 itens por tick, porque esse cabo transporta 16 itens por tick. Um barramento Vintage move 1 item por tick em qualquer cabo.

```table
{"caption": "O que um cabo transporta", "heads": ["Era", "Linha de acesso", "Linha de backbone"], "rows": [[{"words": "Vintage"}, {"amount": 4, "unit": "it/t"}, {"amount": 16, "unit": "it/t"}], [{"words": "Legacy"}, {"amount": 16, "unit": "it/t"}, {"amount": 64, "unit": "it/t"}], [{"words": "Transition"}, {"amount": 64, "unit": "it/t"}, {"amount": 256, "unit": "it/t"}], [{"words": "Standard"}, {"amount": 128, "unit": "it/t"}, {"amount": 512, "unit": "it/t"}], [{"words": "Advanced"}, {"amount": 256, "unit": "it/t"}, {"amount": 2048, "unit": "it/t"}]]}
```

Barramentos de armazenamento não se instalam em linhas de longa distância ou HPC. Um cabo de era mais nova transporta mais, então um cabo mais novo eleva a velocidade de um barramento nele.

O Barramento de Armazenamento Externo não tem velocidade própria. A rede alcança o seu inventário com um décimo do que o cabo transporta, e no mínimo 1 item por tick. Ele não tem ajuste de POWER nem de MODE. A sua própria página o explica ([](jsc:external_storage_bus)).

## Se algo der errado

> [!PROBLEM] O barramento move menos itens do que a tabela diz.
> O cabo é o limite. Use um cabo de uma era mais nova, ou uma linha de acesso ou de backbone ([](jsc:data_cables)).

> [!SEE] jsc:buses, jsc:eras
