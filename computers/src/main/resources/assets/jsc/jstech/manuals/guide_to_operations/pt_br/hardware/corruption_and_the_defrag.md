---
section: hardware
order: 23
icon: jsc:disk_vaultis_trench_20m
---
# Corrupção e o Defrag

O armazenamento nunca se corrompe no jogo normal. Duas coisas o corrompem: um conflito de rede, dois Mainframes numa rede ([](jsc:network_identity)), que corrompe um décimo do que as duas redes guardam; e um corte térmico ([](jsc:cooling)), que pode corromper os discos do computador que desligou.

> [!DEFINE] Item corrompido
> Um item que o disco ainda guarda mas que ninguém consegue ler: sai do índice da rede até o Defrag devolvê-lo ou perdê-lo.

> [!DEFINE] Setores defeituosos
> A parte de um disco que uma corrupção pesada estragou: o disco guarda isso a menos.

Os itens corrompidos continuam ocupando o seu espaço no disco, e o This PC e o Storage Insights mostram quanto de cada disco está corrompido, em vermelho. A corrupção também pode atingir os arquivos do próprio sistema, e um sistema sem o arquivo que o inicia não inicia. A parte privada de um Personal Computer nunca é tocada.

Table: Quanto um disco se estraga

| Corrompido | O que acontece |
| --- | --- |
| Até a metade | Só os itens, à espera do Defrag |
| Mais da metade | Os setores defeituosos tiram o que passa da metade: a 70%, o disco guarda um quinto a menos |
| Mais de nove décimos | O disco morre, com tudo o que guardava |

Table: O Defrag de cada sistema

| Sistema | Programa | Como começar |
| --- | --- | --- |
| MC-DOS e MC-NET | `DEFRAG` | Digite defrag no prompt e aperte Enter |
| Frames 95, XP e 7 | Desfragmentador de Disco | Abra pelo menu Iniciar |
| Frames 10 e 11 | Otimizar Unidades | Abra pelo menu Iniciar |
| Linux, FreeBSD e UNIX | `fsck` | Digite fsck -y e o dispositivo do disco, como /dev/sdb1 |

> [!DEFINE] Desfragmentador de Disco
> O Defrag do Frames 95, do XP e do 7; o Frames 10 e o 11 o chamam de Otimizar Unidades.

Quando você roda o Defrag, uma passagem tenta cada item corrompido uma vez. O que volta faz parte do seu disco de novo; o que não volta se perde. Cada passagem também conserta um quarto dos setores defeituosos que restam. Enquanto uma passagem roda, o computador continua trabalhando, mas aquele disco responde à rede duas vezes mais devagar.

Table: Uma passagem em cada tipo de disco

| Disco | Chance de um item voltar | Uma passagem leva |
| --- | --- | --- |
| Disco rígido | 95% | 3 minutos |
| SSD | 70%, 10 a menos a cada décimo corrompido; nada a partir de 70% | 1 minuto |
| NVMe | 60%, 15 a menos a cada décimo corrompido; nada a partir de 50% | 30 segundos |

O Frames 7, o 10 e o 11 rodam o Defrag sozinhos uma vez por semana num disco que precise; o botão Configurar agendamento ou Alterar configurações liga e desliga isso. Um sistema que não inicia mais se conserta levando o disco a uma Dock Station de outro computador e rodando o Defrag lá, ou instalando o sistema de novo.

As configurações do servidor decidem o que uma rede que falha custa, no jscomputers-server.toml em [item_loss]: corruption desliga toda a corrupção, network_conflict o que um conflito custa, e network_split se uma Operation perde o que carregava quando a rede se parte debaixo dela, um cabo puxado entre o Mainframe e um servidor.

## Se algo der errado

> [!PROBLEM] Faltam itens na rede depois que dois Mainframes se encontraram nela.
> O conflito os corrompeu. Separe os Mainframes e depois rode o Defrag em cada disco.

> [!PROBLEM] O Defrag não devolveu nada num disco NVMe.
> Mais da metade dele estava corrompida, e o NVMe não devolve nada a partir daí. Um disco rígido devolveria.

> [!PROBLEM] O This PC diz que um disco falhou.
> Mais de nove décimos dele estavam corrompidos, e ele morreu com tudo o que guardava. Troque-o.

> [!SEE] jsc:disks, jsc:network_identity, jsc:cooling, jsc:storage_insights
