---
section: storage
order: 120
icon: jsc:disk_vaultis_trench_20m
---
# Prioridade de armazenamento

Quando a rede guarda itens, ela precisa escolher um armazenamento para eles, e a prioridade de armazenamento decide a ordem. O armazenamento com a maior prioridade é preenchido primeiro. Isso não é a prioridade de uma Operação ([](jsc:states_and_queues)), que ordena os próprios trabalhos.

> [!DEFINE] Prioridade de armazenamento
> O número que ordena os armazenamentos na hora de guardar itens. O de maior valor é preenchido primeiro, e o armazenamento próprio da rede conta como prioridade 0.

## Quando os itens entram

1. Verifique o espaço livre de cada armazenamento e pule os que recusam o item.
2. Entregue os itens ao armazenamento com a maior prioridade, até o limite do espaço livre dele.
3. Entre armazenamentos com a mesma prioridade, prefira o mais rápido e, depois, o que tiver mais espaço.
4. Entregue o restante ao próximo armazenamento nessa ordem e grave os itens.

Alguns itens voltam primeiro ao servidor de onde vieram, desde que esse servidor tenha espaço. É assim que os resultados de uma fabricação retornam ([](jsc:asking_for_a_craft)).

## Quando os itens saem

Para tirar itens, vale outra ordem: primeiro o armazenamento mais rápido, depois o mais cheio.

Table: Latência de leitura dos discos da rede

| Disco | Latência de leitura |
| --- | --- |
| Disco rígido (HDD) | 10 ticks |
| Unidade de estado sólido (SSD) | 3 ticks |
| NVMe (non-volatile memory express) | 1 tick |

> [!DEFINE] Latência de leitura
> Os ticks que um armazenamento espera antes de liberar itens. Menos ticks é mais rápido.

Estes são os valores padrão, e a configuração do mod pode alterá-los.

## Prioridade nos barramentos

Só um Barramento de Armazenamento Externo tem prioridade de armazenamento, e somente a partir da era Standard. Servidores, racks e computadores sempre contam como 0. Você define a prioridade de um barramento de -99 a 99. O mesmo número também decide qual barramento reivindica um item primeiro ([](jsc:bus_keep_max_and_priority)).

Um barramento com prioridade negativa é preenchido depois do armazenamento próprio da rede, e um com prioridade positiva é preenchido antes dele.

## Leituras e escritas mais rápidas

Uma placa de cache de leitura, a Vaultis Cache da era, num slot de expansão de um servidor reduz a latência de leitura dos discos do servidor, mais a cada era mais nova ([](jsc:raid_arrays)). O serviço Cache Preditivo reduz em 15%. Os cortes se somam.

Dentro de um servidor, o serviço Balanceador de Carga muda a ordem de gravação. Os itens novos vão primeiro para o disco mais vazio, em vez de encher os discos um após o outro ([](jsc:network_services)).

## Se algo der errado

> [!PROBLEM] O barramento não tem configuração de prioridade.
> A prioridade começa na era Standard. Use um Barramento de Armazenamento Externo da era Standard ou mais recente.

> [!SEE] jsc:external_storage_bus, jsc:states_and_queues, jsc:network_services
