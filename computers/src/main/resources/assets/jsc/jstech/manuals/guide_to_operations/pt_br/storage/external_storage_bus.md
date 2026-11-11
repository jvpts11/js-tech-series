---
section: storage
order: 121
icon: jsc:external_storage_bus
---
# O Barramento de Armazenamento Externo

Um Barramento de Armazenamento Externo permite que a rede use um baú, um tanque ou uma máquina como armazenamento, sem disco nenhum dentro. Você coloca o barramento em um cabo de dados ao lado do inventário, e a rede conta o que o inventário guarda como armazenamento próprio.

> [!DEFINE] Inventário
> Qualquer baú, tanque ou máquina que guarde itens ou fluido, e que um barramento consiga encarar.

O texto de dica diz "Clique com o botão direito em um cabo de dados ao lado de um inventário; a rede usa o inventário como armazenamento seu". Um barramento se conecta a cabos da sua própria era ou de uma anterior.

O armazenamento dele é muito mais lento que os discos próprios da rede. A velocidade abaixo mostra o quanto.

As versões Vintage, Legacy, Transition e Advanced levam a era no nome, como "Barramento de Armazenamento Externo Vintage". A versão Standard tem o nome simples.

## O que cada era pode fazer

Table: O Barramento de Armazenamento Externo por era

| Era | Filtro e acesso | Prioridade | Tags e correspondência mais solta |
| --- | --- | --- | --- |
| Vintage | nenhum | não | não |
| Legacy e Transition | sim | não | não |
| Standard | sim | sim | não |
| Advanced | sim | sim | sim |

O Barramento de Armazenamento Externo não tem condições em nenhuma era. As condições pertencem ao Barramento de Importação e ao Barramento de Exportação ([](jsc:bus_conditions)).

## Acesso e filtro

O acesso define o que a rede pode fazer com o inventário. Ele começa na era Legacy, e um barramento Vintage sempre lê e escreve.

Table: Acesso do barramento

| Configuração | O que a rede faz |
| --- | --- |
| LER E ESCREVER | Tira itens do Inventário e põe itens nele (o padrão). |
| SÓ LER | Apenas tira itens, e nunca preenche o Inventário. |
| SÓ ESCREVER | Apenas põe itens; o que está no Inventário nunca é retirado nem visto. |

O filtro decide quais itens o barramento deixa passar. Sem nenhum item listado, o barramento deixa tudo passar. Com itens listados, só esses passam, ou, com "EXCETO ESTES", tudo menos eles.

A janela do barramento mostra o que o inventário guarda em "HOLDS", como slots e quantos estão usados, com a nota "mostrado à rede como armazenamento seu".

## Velocidade

O barramento move um décimo do que o seu cabo carrega a cada tick, e pelo menos um item. Sua latência de leitura é de 100 ticks, que são 5 segundos: dez vezes os 10 ticks de um disco rígido (HDD). Uma unidade de estado sólido (SSD) responde cerca de 33 vezes mais rápido que o barramento, e um disco NVMe, 100 vezes mais rápido.

A janela do barramento diz "10x mais lento que o armazenamento da própria rede." Isso vale só contra um disco rígido. Um servidor de discos de estado sólido ou NVMe é ainda mais rápido.

A rede olha o inventário de novo a cada 10 ticks. Por isso, uma mudança no inventário pode chegar à lista de itens da rede com até 10 ticks de atraso.

## Espaço e recusas

O espaço que o barramento oferece é o espaço livre do inventário. Um barramento SÓ LER não oferece nenhum. Um INSERT pula o barramento quando ele está SÓ LER, ou quando o filtro dele não deixa o item passar.

## Se algo der errado

> [!PROBLEM] A janela diz "nenhum inventário à frente".
> O barramento não encara nada. Coloque-o em um cabo de dados ao lado de um inventário.

> [!PROBLEM] A janela diz "em nenhuma rede".
> Conecte o cabo do barramento a uma rede ([](jsc:data_cables)).

> [!PROBLEM] A janela diz "o baú está cheio".
> O inventário não tem espaço livre. Tire itens dele.

> [!PROBLEM] A rede nunca coloca itens no barramento.
> O barramento está em SÓ LER. Coloque-o em LER E ESCREVER.

> [!PROBLEM] A rede nunca tira itens do barramento, e nunca os vê.
> O barramento está em SÓ ESCREVER. Coloque-o em LER E ESCREVER ou SÓ LER.

> [!PROBLEM] O barramento não tem a configuração SÓ LER.
> O acesso começa na era Legacy. Use um Barramento de Armazenamento Externo Legacy ou mais novo.

> [!SEE] jsc:buses, jsc:storage_priority, jsc:storage_trouble
