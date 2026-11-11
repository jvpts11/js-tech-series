---
section: fab
order: 45
icon: jsc:prebuilder_robot_arm
covers: [jsc:prebuilder_case_lift, jsc:prebuilder_robot_arm, jsc:prebuilder_parts_tray, jsc:preinstaller_cradle, jsc:preinstaller_console, jsc:preinstaller_disk_dock, jsc:preinstaller_board_fixture]
---
# A Pré-Montadora e a Pré-Instaladora

Estas duas máquinas fazem computadores inteiros, prontos para colocar e ligar, para você ou para o autocrafting da rede: a Pré-Montadora monta um computador com as peças dele, e a Pré-Instaladora grava o que ele roda.

Dê à Pré-Montadora um gabinete vazio e as peças que vão nele, em qualquer ordem: o gabinete de um computador de um bloco, ou um servidor de rack. Ela põe cada peça no seu lugar pelas mesmas regras da tela de montagem ([](jsc:assembly_screen)), a placa primeiro, e entrega o computador com todas as peças dentro. Quando uma peça não cabe, ela não monta nada e diz por quê, na tela do terminal e na janela dela.

Uma placa-mãe pode vir sem firmware, e um computador montado nela só gira as ventoinhas. A Pré-Instaladora grava o firmware da era da placa, depois um sistema e os programas dele no disco do sistema, como um instalador faria ([](jsc:installing)). Você põe as mídias de instalação entre as entradas dela: ela as lê e as deixa lá, sem gastá-las.

Ela trabalha numa coisa de cada vez: um computador com as peças dele, uma placa-mãe solta, um disco solto ou um servidor de rack. Um disco solto é como você dá um sistema às unidades das baias de um servidor de rack. Um trabalho leva a etapa da máquina e mais um segundo a cada 100 MB que ela grava.

```table
{"caption": "O tamanho de cada máquina, a sua etapa e o seu consumo", "heads": ["Máquina", "Tamanho", "Etapa", "Consumo"], "rows": [[{"words": "Pré-Montadora"}, {"words": "3 × 3 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 1000, "unit": "W"}], [{"words": "Pré-Instaladora"}, {"words": "2 × 2 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 1000, "unit": "W"}]]}
```

## Se algo der errado

> [!PROBLEM] A Pré-Montadora não monta nada e cita uma peça.
> Essa peça não tem mais lugar no gabinete, ou as regras do gabinete a recusam. Tire-a, ou dê um gabinete e uma placa que a aceitem.

> [!PROBLEM] A Pré-Instaladora diz que não há nada para gravar.
> O firmware da placa já está gravado, e nenhuma mídia traz um sistema ou um programa que falte ao computador.

> [!PROBLEM] A Pré-Instaladora recusa um sistema.
> O sistema precisa de um computador de uma era mais nova, de outro processador, de mais espaço no disco, ou de todos os seus discos de instalação entre as entradas.

> [!SEE] jsc:building_chain_machines, jsc:firmware, jsc:crafting_interfaces
