---
section: frames_machine_tools
order: 224
---
# O Monitor do Sistema

O Monitor do Sistema é um painel da máquina de relance: o processador, a memória, os gráficos e uma barra para cada disco. Ele apenas lê a máquina e a mostra, então você não pode alterar nada nele.

Abra-o antes de iniciar um programa grande, para ver se a máquina tem memória sobrando. A parte de memória mostra a memória de acesso aleatório (RAM) que os programas ocupam, e quanto está livre.

## O que você lê

A carga do processador é o quanto o processador está ocupado, em porcentagem. A memória mostra a memória em uso, em relação ao total em MB, e a memória livre abaixo dela.

Gráficos mostra "sem GPU" quando a máquina não tem unidade de processamento gráfico (GPU). Caso contrário, mostra a memória de vídeo em uso, em relação ao total, e a carga da placa em porcentagem.

Armazenamento desenha uma barra para cada disco, e marca o disco do sistema com a palavra (sistema). Ele também conta os programas instalados na máquina.

Energia mostra o que as peças consomem diante da potência nominal da fonte e o que a linha fornece em FE por tick, e Temperatura o calor do processador diante dos 90 graus Celsius em que ele começa a desacelerar, cada um sobre um pequeno gráfico. Você escolhe o período dos gráficos no menu ao lado de Energia: a última hora, o último dia ou os últimos 7 dias do jogo.

> [!DEFINE] Carga
> O quanto uma parte da máquina está ocupada, em porcentagem.

> [!DEFINE] Em uso
> Memória que os programas reservaram para si. A memória livre é o que sobra.

## Se algo der errado

> [!PROBLEM] Uma barra mostra um valor com um pequeno atraso.
> A janela lê a máquina de novo a cada cerca de 40 quadros da tela. Espere um instante e olhe de novo.

> [!SEE] jsc:task_manager, jsc:this_pc
