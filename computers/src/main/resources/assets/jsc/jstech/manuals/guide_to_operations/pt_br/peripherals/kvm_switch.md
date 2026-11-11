---
section: peripherals
order: 77
icon: jsc:kvm_switch
---
# O KVM Switch

Um KVM Switch permite que um monitor alcance todos os computadores de um rack. Ele é uma unidade de rack que você monta em um Rack de Servidores ([](jsc:server_racks)).

> [!DEFINE] Unidade de rack
> Uma unidade de altura em um rack, contada em U. Um KVM Switch ocupa uma.

Cada computador do rack é um canal. Um rack com um computador não precisa de KVM Switch: o monitor mostra esse computador. Com dois ou mais, o monitor primeiro mostra a barra de canais, e você escolhe a máquina que quer.

> [!DEFINE] Canal
> Um computador de um rack, escolhido na barra de um KVM Switch.

Table: A barra de canais

| Parte | O que mostra |
| --- | --- |
| KVM SWITCH | O título da barra. |
| Contagem | Quantas máquinas o rack contém, por exemplo "2 máquinas". |
| F1 a F8 | O rótulo de cada canal, uma linha para cada computador. |
| Nome | O nome da máquina. Uma máquina sem nome mostra a sua posição no rack, como "baia 3U". |
| Status | ONLINE quando a máquina está em execução, e OFF quando não está. |

Escolha um canal clicando na sua linha, ou com as teclas numéricas de 1 a 8. A tela da própria máquina então se abre por cima. A barra tem no máximo 8 canais.

## Se algo der errado

> [!PROBLEM] Este rack contém vários computadores - instale um KVM Switch para escolher um
> Instale um KVM Switch em um espaço livre de unidade no rack ([](jsc:server_racks)).

> [!PROBLEM] O rack não contém nenhum computador para mostrar
> Coloque um computador no rack.

> [!PROBLEM] O canal diz OFF.
> Essa máquina não está em execução: ligue-a no seu gabinete.

> [!SEE] jsc:server_racks, jsc:rack_equipment, jsc:monitors
