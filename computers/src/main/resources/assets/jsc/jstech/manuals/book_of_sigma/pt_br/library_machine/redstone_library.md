---
section: library_machine
order: 452
---
# Redstone Interfaces a partir de um programa

Uma Redstone Interface lê o sinal de redstone para uma máquina, ou o emite, e é ligada à máquina por cabo. Seu programa a lê e a define por meio de Redstone. O bloco em si é explicado em [](jsc:redstone_interfaces).

> [!DEFINE] Sinal de redstone
> Uma potência de 0 a 15: potência 0 significa desligado, e potência 15 é a mais forte.

> [!DEFINE] Ativo
> Com energia e ligada à máquina. Uma interface ativa mostra a sua potência; uma que não está ativa mostra potência 0.

Table: Membros de Redstone

| Membro | O que faz |
| --- | --- |
| `Redstone.Named(name)` | A interface com esse nome, ou null quando nenhuma interface ligada a esta máquina responde a ele, ignorando maiúsculas e minúsculas. |
| `Redstone.Name` | O nome da interface. |
| `Redstone.In()` | Define a interface para ler o sinal. Ela para de emitir. |
| `Redstone.Out(strength)` | Define a interface para emitir essa potência, de 0 a 15. |
| `Redstone.Level()` | A potência que a interface mostra agora. |

In define a interface a ler, e Out a define para emitir. Vale a última que você chamar. Cada configuração é marcada com o nome do programa que a fez, ou com Sigma quando esse programa não tem nome próprio.

Level mostra a potência emitida enquanto a interface está ativa, ou a potência que ela lê quando não emite. Devolve 0 quando a interface não está ativa.

Uma interface sem nome próprio responde por Redstone Interface. Um nome tem até 32 caracteres, e duas interfaces de uma mesma máquina não podem ter o mesmo nome.

## Se algo der errado

> [!PROBLEM] O programa para e diz que o número não é uma força de redstone.
> Dê a Out um número inteiro de 0 a 15.

> [!PROBLEM] O programa para e diz que nenhuma Redstone Interface com esse nome está ligada a esta máquina.
> Ligue a interface à máquina com cabo, e confira o nome dela na janela.

> [!PROBLEM] Redstone.Named devolve null.
> Nenhuma interface habilitada ligada a esta máquina tem esse nome. Confira o nome na janela.

> [!PROBLEM] Redstone.Level devolve 0.
> A interface não está ativa. Confira se ela tem energia e se está ligada a esta máquina.

> [!SEE] jsc:redstone_interfaces, jsc:disable_device
