---
section: mod_integrations
order: 162
---
# Create

O Create é um mod de máquinas que giram, e de contraptions: blocos que um rolamento, um pistão, uma polia ou um trem leva embora e assenta de novo. O Create é opcional.

## Computadores em contraptions

Uma contraption pode carregar um Personal Computer, um Crafting Computer ou outro computador pequeno. O computador desce desligado onde a contraption o assenta, com o sistema, as peças e os arquivos dele. Ligue-o de novo quando quiser.

Um computador que copia arquivos ou instala um sistema não é carregado até terminar. Se a contraption parte dele, ou se move para dentro dele, o Create diz que o bloco não pode ser movido e a contraption não se monta. Se ele só está colado ao lado, fica onde está.

As máquinas grandes nunca andam numa contraption. O Mainframe, o Server Rack, o Mining Computer e o WAN Gateway ficam onde estão.

## Computadores em naves

O Create Aeronautics constrói naves que voam com os seus blocos. Um computador a bordo continua funcionando em voo. Uma nave fala com o chão por Wi-Fi: um computador a bordo com uma placa Wi-Fi entra num ponto de acesso no chão dentro do alcance dele, e um ponto de acesso a bordo alcança computadores no chão ([](jsc:wireless)). Um cabo de dados nunca passa entre eles.

## As máquinas do Create na rede

Com o J's Industrial instalado também, o cabo de controle industrial dele lê os blocos que giram do Create, como um motor, um eixo ou um Rotation Speed Controller. A rede os lista na tabela create_kinetics: cada bloco com a velocidade dele, a tensão na rede cinética dele, a capacidade dessa rede e se ela está sobrecarregada.

Table: As máquinas do Create em IQL

| Instrução | O que faz |
| --- | --- |
| `QUERY create_kinetics` | Lista cada bloco do Create que os computadores da rede leem. |
| `UPDATE create_kinetics SET target_speed = 64` | Aponta cada Rotation Speed Controller que ele mantém para 64 RPM. |

Embreagens e câmbios são acionados por redstone. Use uma Redstone Interface para eles ([](jsc:redstone_interfaces)).

> [!DEFINE] Contraption
> Um grupo de blocos que o Create move junto e depois assenta de novo.

> [!DEFINE] RPM
> Voltas por minuto: a rapidez com que um bloco do Create gira.

> [!DEFINE] Unidade de tensão (su)
> A medida do Create de quanto uma rede cinética carrega e de quanto ela aguenta carregar.

## Se algo der errado

> [!PROBLEM] Um rolamento não se monta, e o Create diz que um computador não pode ser movido.
> O computador está copiando arquivos ou instalando um sistema. Espere até ele terminar e então ponha o rolamento para girar de novo.

> [!PROBLEM] A tabela create_kinetics está vazia.
> Um computador da rede precisa ler os blocos. Ligue a eles um computador com uma placa fieldbus pelo cabo de controle industrial.

> [!PROBLEM] A rede diz "a tabela create_kinetics só existe com o Create".
> O Create não está instalado, ou a versão dele não é uma com que o J's Computers trabalha. O log diz qual.

> [!SEE] jsc:wireless, jsc:redstone_interfaces, jsc:update_cards
