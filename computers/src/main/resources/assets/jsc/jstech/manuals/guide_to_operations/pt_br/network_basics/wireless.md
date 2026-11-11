---
section: network_basics
order: 100
icon: jsc:standard_access_point
covers_family: jsc:wireless
---
# Wi-Fi: access points e placas Wi-Fi

A partir da era Legacy, um computador pequeno pode entrar na rede sem cabo próprio. Você põe uma placa Wi-Fi nele e instala um Access Point Sem Fio na rede, ao alcance dele. O access point dá a rede a que está ligado por cabo a todo computador com placa ao seu redor.

> [!DEFINE] Access Point Sem Fio
> Um aparelho de rede que dá a sua rede aos computadores com placa Wi-Fi ao seu alcance.

> [!DEFINE] Placa Wi-Fi
> Uma placa de expansão que liga um computador pequeno à rede por um access point em vez de um cabo.

Table: Os access points

| Era | Padrão | Alcance | Velocidade | Consumo |
| --- | --- | --- | --- | --- |
| Legacy | 802.11b/g | 24 blocos | 4 itens por tick | 10 W |
| Transition | 802.11n | 32 blocos | 16 itens por tick | 12 W |
| Standard | 802.11ac | 48 blocos | 64 itens por tick | 15 W |
| Advanced | Wi-Fi 6 | 64 blocos | 128 itens por tick | 20 W |

## Instalando um access point

Um access point recebe duas linhas: o cabo de acesso da sua era ou de uma mais antiga, e o cabo de energia. As duas chegam pela mesma face. O do Legacy e o do Transition ficam sobre uma mesa, virados para você, e recebem os cabos por trás. O do Standard e o do Advanced são presos no teto ou numa parede pela placa de montagem, e recebem os cabos pela face onde estão presos; tire esse bloco e o access point cai junto.

As luzes dizem como ele está: a de energia enquanto tem energia, e as duas de tráfego piscando enquanto está numa rede. Vários access points no mesmo lugar não brigam: cada computador entra pelo mais próximo que o alcança, e um access point aceita qualquer número de computadores.

## As placas

Cada era tem a sua placa Wi-Fi: PCI no Legacy, PCI Express depois. Ela vai num Computador Pessoal, num Computador de Crafting ou num Computador de Gerenciamento de Cluster; o Mainframe e os racks ficam no backbone, por cabo. A placa não consome nada. Um computador que tem cabo continua a usar o cabo, com placa ou sem.

Uma ligação anda na velocidade do mais velho dos dois lados, como no Wi-Fi de verdade: uma placa Standard por um access point Legacy anda a 4 itens por tick. As cópias de arquivos e de músicas pela rede não vão mais rápido que isso. O Network Manager mostra cada access point como um nó ACCESS POINT no seu cabo, e a ligação do computador como Wi-Fi; escolha o computador na lista Devices para ler qual lado dita a velocidade ([](jsc:network_manager)).

## Se algo der errado

> [!PROBLEM] Um computador com placa não entra na rede.
> Ponha-o ao alcance do access point, ou confira se o access point tem energia e cabo: a luz de energia acesa e as de tráfego piscando.

> [!PROBLEM] Um access point de fixar não vai para onde você clica.
> Ele vai no teto ou numa parede, nunca no chão, e a face onde ele se prende tem de ser sólida.

> [!SEE] jsc:routers, jsc:data_cables, jsc:network_manager
