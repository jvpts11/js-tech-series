---
section: iql_verbs
order: 370
---
# UPDATE com as placas

UPDATE muda um item que a rede guarda. Quem faz o trabalho é uma placa: ela fica em um Computador Pessoal ([](jsc:personal_computers)), e você executa a instrução a partir desse computador. As placas são explicadas na página delas ([](jsc:workshop)).

Só as placas do próprio Computador Pessoal mudam itens para a rede. A Placa de Fornalha funde, a Placa de Encantamento encanta e a Placa de Bigorna repara, combina e dá nomes.

```iql name.iql
UPDATE diamond_sword SET NAME 'Old Faithful'
```

Table: As cinco ações do UPDATE

| Ação | Placa, e o que ela precisa |
| --- | --- |
| `SMELT` | Placa de Fornalha, para um item que funde, sem custo de experiência e sem WITH. |
| `ENCHANT` | Placa de Encantamento, para um item que aceita encantamento, paga com experiência, e OFFER escolhe uma das ofertas. |
| `REPAIR` | Placa de Bigorna, para um item danificado, paga com experiência, e WITH pode dar o nome do material. |
| `COMBINE` | Placa de Bigorna, paga com experiência, e WITH dá o nome do item com que ela combina. |
| `NAME` | Placa de Bigorna, paga com experiência, para um nome entre aspas de no máximo 50 caracteres. |

Uma ação paga usa a experiência do jogador que enviou a instrução, então alguém precisa enviá-la. A Placa de Bancada de Trabalho não tem a ação UPDATE: uma grade preenchida a partir da rede é para isso que serve o CRAFT ([](jsc:iql_craft)).

## ENCHANT e suas ofertas

Sem OFFER, ENCHANT lista as três ofertas e não muda nada. Cada oferta mostra uma pista, o nível que ela exige e os níveis que custa. Digite OFFER com 1, 2 ou 3 para escolher uma.

```iql enchant.iql
UPDATE diamond_sword SET ENCHANT OFFER 2
```

## Qual cópia, e quantas

Um item pode existir em várias cópias, com dano, encantamentos ou nomes diferentes. Sem ORDER BY, REPAIR escolhe a cópia mais desgastada, e as outras ações escolhem a cópia que a rede tem em maior quantidade. Sem quantidade, o UPDATE muda um item.

REPAIR sem WITH usa o material que a rede tem em maior quantidade.

1. Coloque a placa no Computador Pessoal que executa a instrução.
2. Digite UPDATE, o item, SET e a ação, como em UPDATE iron_sword SET REPAIR.
3. Pressione [[Enter]] e depois leia a resposta.

Table: As respostas do UPDATE

| Resposta | O que significa |
| --- | --- |
| `UPDATE queued: <quantity> <item>, SET <ACTION>` | A mudança foi iniciada. |
| `The offers for the <item>:` | A resposta para ENCHANT sem OFFER. Segue uma linha para cada oferta. |
| `the network holds no <item>` | Nenhuma cópia do item está na rede. |
| `could not start the UPDATE` | A rede não iniciou a mudança. |

## A energia das baias de um rack

A tabela power aceita um UPDATE próprio: as baias dos racks com PDU. SET on = no desliga cada baia que o WHERE dela mantém, e SET on = yes as liga, como os interruptores PWR delas fariam; não precisa de placa. Um job que o roda liga e desliga servidores por regra. Um servidor para o qual o rack não tem energia, ou cuja fonte falhou, não liga, e a resposta o conta.

```iql power.iql
UPDATE power SET on = no WHERE server = 'VAULT-01'
```

## A velocidade das máquinas do Create

Com o Create instalado, a tabela create_kinetics também aceita um UPDATE ([](jsc:create)). SET target_speed = 64 aponta cada Rotation Speed Controller que o WHERE dela mantém para 64 RPM, e um sinal de menos o faz girar para o outro lado. Um bloco mantido que não é Rotation Speed Controller, como um motor, fica como está, e a resposta o conta.

```iql kinetics.iql
UPDATE create_kinetics SET target_speed = 64
```

## Se algo der errado

> [!PROBLEM] este computador não tem <card name>
> Coloque essa placa neste Computador Pessoal, e depois execute a instrução de novo.

> [!PROBLEM] só as placas do próprio Personal Computer mudam itens para a rede
> Execute a instrução em um Computador Pessoal que tenha a placa.

> [!PROBLEM] alguém precisa pedir: a placa faz o trabalho para essa pessoa, como ela faria no bloco
> Execute a instrução a partir do terminal de um jogador, para que a placa trabalhe para esse jogador.

> [!PROBLEM] o <item> não funde
> Escolha um item que funde.

> [!PROBLEM] o <item> não aceita encantamento
> Escolha um item que aceite encantamento.

> [!PROBLEM] o <item> não está desgastado
> REPAIR precisa de uma cópia danificada, então escolha um item desgastado.

> [!PROBLEM] a rede não guarda nada que conserte o <item>
> Coloque um material na rede, ou dê o nome de um depois de WITH.

> [!PROBLEM] COMBINE precisa de WITH e do item que acrescenta
> Adicione WITH e o segundo item.

> [!PROBLEM] um nome tem no máximo 50 caracteres
> Encurte o nome para 50 caracteres ou menos.

> [!PROBLEM] ação desconhecida: <word> (esperado SMELT, ENCHANT, REPAIR, COMBINE ou NAME)
> Digite uma das cinco palavras de ação.

> [!PROBLEM] nenhuma baia de um rack com PDU nesta rede está assim
> Monte um PDU no rack e confira o WHERE antes com QUERY power.

> [!PROBLEM] nenhum bloco do Create que os computadores desta rede leem é assim
> Confira o WHERE com QUERY create_kinetics primeiro.

> [!SEE] jsc:workshop, jsc:clauses_priority_if_with, jsc:personal_computers, jsc:rack_equipment
