---
section: racks
order: 58
icon: jsc:kvm_switch
covers_family: jsc:rack_equipment
---
# Equipamentos do rack

Nem tudo em um rack computa. Algumas unidades servem ao rack: o KVM Switch (teclado, vídeo, mouse), o Nobreak de Rack (fonte de energia ininterrupta) e suas expansões de bateria, o PDU (unidade de distribuição de energia), a Unidade de Refrigeração, o Painel de Ventilação, a Interface de Fibra Óptica e a Gaveta de Discos ocupam unidades de rack, como um servidor faz.

Table: Unidades de rack que não são computadores

| Unidade | Ocupa | O que faz |
| --- | --- | --- |
| KVM Switch | 1 U | Permite que um monitor alcance todas as máquinas do rack |
| Nobreak de Rack | 1 U | Mantém o rack na bateria quando a energia cai: 2,7 MJ, até 3 kW |
| Unidade de Refrigeração | 1 U | Acrescenta ao orçamento de calor, mais a cada era mais nova |
| Painel de Ventilação | 1 U | Soma 500 W ao orçamento de calor, num rack de qualquer era |
| Interface de Fibra Óptica | 1 U | Deixa a traseira do rack aceitar o Cabo de Fibra Óptica |
| Interface de Fibra Óptica Advanced | 1 U | Deixa a traseira do rack aceitar o Cabo de Fibra Óptica e o Cabo de Fibra OM5 |
| Expansão de Bateria do Nobreak | 1 U | Soma 2,7 MJ à bateria do Nobreak de Rack; elas se somam |
| PDU | 1 U | Mede o consumo de cada baia e deixa o IQL e os jobs ligarem e desligarem as baias |
| Gaveta de Discos | 2 U | Mais dez baias de disco para o servidor logo acima dela |
| Biblioteca de Fitas | 2 U | Dez fitas da sua era, onde o Serviço de Backup guarda os seus backups |

O KVM Switch, o Nobreak de Rack e a Unidade de Refrigeração existem em todas as eras, da Vintage à Advanced. Um rack recebe as unidades da sua própria era e das anteriores, como faz com os servidores: um KVM Switch Vintage entra num rack Standard, mas um Standard nunca entra num rack Vintage.

O KVM Switch permite que um monitor alcance todas as máquinas do rack, então você não precisa de uma tela para cada uma. Um rack com dois ou mais computadores precisa dele para escolher entre eles.

A Unidade de Refrigeração aumenta o calor que o rack consegue suportar, a de uma era mais nova mais do que a de uma mais antiga, como explicado em ([](jsc:rack_cooling_and_heat)). O Painel de Ventilação é uma tampa perfurada que soma 500 W, e só um é feito para todas as eras: ele pega a cor do rack em que está montado.

Da era Standard em diante, o backbone é de fibra, e um rack só o aceita por uma Interface de Fibra Óptica. A Standard aceita o Cabo de Fibra Óptica; a Advanced, que só um rack Advanced recebe, aceita também o Cabo de Fibra OM5. Uma em qualquer fileira do rack basta, e os servidores dele não precisam de placa de rede ([](jsc:data_cables)).

As placas de armazenamento de um servidor vão nos slots de expansão dele, não no rack: a controladora RAID junta as unidades do servidor num único array, e o cache de leitura reduz o tempo que cada leitura delas leva ([](jsc:raid_arrays)).

O Nobreak de Rack mantém o rack inteiro funcionando quando a energia cai: cada um montado guarda 2,7 MJ e aguenta 3 kW, e cada Expansão de Bateria do Nobreak soma mais 2,7 MJ, quantas couberem no rack. Enquanto a linha traz o suficiente, ele carrega por cima do que as máquinas consomem. Na bateria, a tela do rack diz ON BATTERY, o sistema de cada servidor avisa, e a 10% eles se desligam corretamente. Acima do que aguenta, o nobreak entra em bypass e não protege nada. Clique no painel de uma unidade para ver o cartão dela.

O PDU mede o que cada baia consome, e o cartão dele lista isso. Ele abre a energia das baias para a rede: QUERY power lista cada servidor de um rack com PDU, com seus watts e se está ligado, e UPDATE power SET on = no WHERE server = 'VAULT-01' o desliga, de um prompt ou de um job ([](jsc:iql_jobs)).

Uma Gaveta de Discos soma suas dez baias às do servidor montado logo acima dela: os discos dela entram no armazenamento do servidor e no arranjo dele. Uma gaveta sem servidor logo acima não faz nada.

Uma Biblioteca de Fitas guarda dez fitas da sua própria era e uma unidade que as enrola: armazenamento frio para os backups da rede, enorme e lento ([](jsc:backups)). Ela serve a rede dos servidores do seu rack. O seu cartão lista as fitas, o que guardam e o que a unidade está fazendo: parada, enrolando, lendo ou gravando.

## Se algo der errado

> [!PROBLEM] Um rack com vários computadores não tem como escolher um no monitor.
> Monte um KVM Switch no rack. Ele ocupa uma unidade de rack.

> [!PROBLEM] Uma unidade não entra, e o rack diz que ela pertence a um rack mais novo.
> A unidade é de uma era mais nova que a do rack. Use a unidade da era do próprio rack ou uma mais antiga.

> [!PROBLEM] O Cabo de Fibra OM5 não entra no rack.
> O rack tem a Interface de Fibra Óptica Standard, que só aceita o Cabo de Fibra Óptica. Monte a Advanced, num rack Advanced.

> [!PROBLEM] A controladora RAID não faz nada.
> O servidor dela não tem modo definido, ou tem menos unidades do que o modo exige ([](jsc:raid_arrays)).

> [!PROBLEM] Os discos de uma Gaveta de Discos não aparecem em nenhum servidor.
> A gaveta só estende o servidor logo acima dela. Monte-a na fileira embaixo do servidor.

> [!PROBLEM] Os servidores se desligaram sozinhos enquanto a energia estava fora.
> A bateria do Nobreak de Rack chegou a 10%. Adicione Expansões de Bateria do Nobreak para durar mais.

> [!PROBLEM] UPDATE power diz que nenhuma baia está assim.
> O rack precisa de um PDU, e o servidor precisa estar na rede.

> [!PROBLEM] Uma fita não entra na Biblioteca de Fitas.
> A biblioteca aceita só as fitas da sua própria era: o rolo na da Vintage, a DLT na da Legacy, a LTO-3 na da Transition, a LTO-6 na da Standard e a LTO-9 na da Advanced.

> [!SEE] jsc:server_racks, jsc:rack_cooling_and_heat, jsc:upses
