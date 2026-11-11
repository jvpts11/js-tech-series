---
section: after_the_reboot
order: 331
---
# O que a máquina verifica ao iniciar

Quando a máquina inicia, ela executa primeiro o autoteste de inicialização (POST). Ao fim do teste, a máquina verifica o disco do sistema uma vez e, então, decide o que fazer em seguida.

A verificação procura duas coisas no disco: uma pasta chamada boot, e o arquivo do kernel boot/vmlinuz dentro dela. O arquivo do kernel é o único arquivo que inicia um sistema Linux. Se os dois estiverem ali, a máquina inicia o sistema.

Table: Como a máquina decide

| O que a verificação encontra no disco | O que você vê | O que acontece |
| --- | --- | --- |
| A pasta boot e o arquivo do kernel estão ambos ali. | A inicialização segue. | A máquina inicia o sistema. Com mais de um sistema no disco, ela pode mostrar primeiro o Menu de Inicialização. |
| Não há pasta chamada boot, ou nenhum sistema no disco, e nenhuma mídia de instalação está em uma unidade. | As linhas de dispositivo não encontrado do firmware, abaixo. | A máquina para no autoteste e espera. |
| A pasta boot está ali, mas o arquivo do kernel sumiu, e nenhuma mídia de instalação está em uma unidade. | `kernel panic - not syncing: no init found` | A máquina para no autoteste e espera, com a linha de reparo abaixo. |

## As linhas do firmware quando nada inicia

As linhas dependem da aparência do firmware da máquina, que segue a sua era. Os sistemas Linux iniciam em máquinas Legacy e posteriores.

Table: A aparência do firmware

| Aparência do firmware | O que a tela mostra |
| --- | --- |
| Vintage | "Disco sem sistema ou erro de disco", depois "Substitua e pressione qualquer tecla quando estiver pronto" |
| Legacy e Transition | `DISK BOOT FAILURE, INSERT SYSTEM DISK AND PRESS ENTER` |
| Standard, Advanced e posteriores | "Nenhum dispositivo inicializável encontrado", depois "Pressione qualquer tecla para entrar na Configuração" |

Quando a máquina para com uma reclamação, o cabeçalho grande da tela mostra a reclamação. Sem reclamação, o cabeçalho diz "Nenhum dispositivo inicializável". Com reclamação, a tela também mostra a linha "Insira uma mídia de instalação e instale por cima para reparar."

## Uma máquina que para

Uma máquina parada mantém a sua mensagem na tela, e um monitor que você abra depois ainda a mostra. A máquina verifica o disco de novo a cada segundo. Ela inicia de novo sozinha quando o disco está íntegro, ou quando uma mídia de instalação está em uma unidade.

Para consertar um sistema destruído, coloque uma mídia de instalação em uma unidade e instale por cima do sistema ([](jsc:installing)). A instalação grava de volta no disco a pasta boot e o arquivo do kernel, para que a máquina possa iniciar de novo.

Se você tirar a mídia de instalação da unidade de onde a máquina iniciou, a máquina reinicia e não encontra nada para iniciar.

> [!DEFINE] Autoteste (POST)
> O teste de inicialização que o firmware executa primeiro a cada vez que o computador liga, antes de qualquer sistema começar.

Um computador também pode se recusar a ligar por falta de energia, que é uma recusa diferente ([](jsc:power_supplies)).

## Se algo der errado

> [!PROBLEM] A tela mostra "kernel panic - not syncing: no init found" e fica esperando.
> O arquivo do kernel boot/vmlinuz sumiu do disco. Instale por cima do sistema a partir de uma mídia de instalação ([](jsc:installing)).

> [!PROBLEM] A tela mostra as linhas de dispositivo não encontrado do firmware e fica esperando.
> O disco não tem sistema, ou a sua pasta boot sumiu. Instale um sistema no disco a partir de uma mídia de instalação ([](jsc:installing)).

> [!SEE] jsc:firmware, jsc:installing, jsc:two_systems, jsc:disks
