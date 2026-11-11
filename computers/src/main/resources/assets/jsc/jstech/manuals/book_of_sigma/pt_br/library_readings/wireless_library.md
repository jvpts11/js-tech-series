---
section: library_readings
order: 457
---
# Wireless: o Wi-Fi da máquina

Um computador com uma placa Wi-Fi entra na rede dele por um ponto de acesso. Um programa pode ler como: se entrou, por qual ponto de acesso, em qual padrão e o quão rápido o link carrega. Você lê isso pelo Wireless, um tipo embutido do System.Machine. O Wireless veio com o Sigma# 3.

Table: Membros do Wireless

| Membro | O que dá |
| --- | --- |
| `Wireless.Joined` | Sim quando a máquina está na rede dela por um ponto de acesso. |
| `Wireless.AccessPoint` | O nome do ponto de acesso, como a rede dele o lista, ou texto vazio quando a máquina não entrou em nenhum. |
| `Wireless.Standard` | O padrão da placa Wi-Fi da máquina, como 802.11n, ou texto vazio quando ela não tem placa. |
| `Wireless.Speed` | Quantos itens por tick o link carrega: a velocidade do mais velho entre a placa e o ponto de acesso. Dá 0 quando a máquina não entrou. |
| `Wireless.Signal` | O quanto o sinal do ponto de acesso é forte onde a máquina está, de 100 ao lado dele a 0 na borda do alcance. Dá 0 quando a máquina não entrou. |

O sinal só diz onde a máquina está: o link carrega na velocidade cheia em qualquer ponto do alcance do ponto de acesso.

Uma máquina num cabo de dados não entrou por Wi-Fi, mesmo com uma placa nela: o cabo a carrega.

> [!DEFINE] Ponto de acesso
> Um aparelho de rede que dá a rede sem fio aos computadores e aos dispositivos portáteis em volta dele.

## Se algo der errado

> [!PROBLEM] Wireless.Joined dá não, mesmo com a máquina tendo uma placa.
> Nenhum ponto de acesso a alcança, ou um cabo de dados a carrega no lugar.

> [!PROBLEM] Wireless.Standard dá texto vazio.
> A máquina não tem placa Wi-Fi.

> [!SEE] jsc:wireless, jsc:computer_info
