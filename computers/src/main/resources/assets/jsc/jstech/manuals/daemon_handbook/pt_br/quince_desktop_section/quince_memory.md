---
section: quince_desktop_section
order: 358
---
# A memória nos sistemas da Quince

## Os sistemas clássicos: um bloco para cada programa

No Quince System e no Quince OS 9, cada programa que você abre ganha um bloco de memória só seu, que guarda enquanto roda. No Quince System o bloco tem um tamanho só. No Quince OS 9 dois tamanhos o definem, o tamanho preferido e o tamanho mínimo: o programa abre no tamanho preferido enquanto houver tanto livre, no que sobra acima do mínimo caso contrário, e não abre abaixo do mínimo.

> [!DEFINE] Tamanho preferido
> O bloco em que um programa abre enquanto houver essa memória livre.

> [!DEFINE] Tamanho mínimo
> O menor bloco em que um programa abre no Quince OS 9.

Com a janela de um programa na frente, escolha Obter Informações no menu Arquivo para mudar os seus tamanhos. O tamanho sugerido ao lado deles é o que o fabricante do programa sugere. Sobre Este Computador, no menu da fruta, mostra o bloco de cada programa aberto como uma barra, a parte em uso mais escura.

> [!DEFINE] Obter Informações
> A janela que mostra o que um arquivo ou programa é, e define a memória de um programa num sistema clássico.

## O Quince OS X e o quinceOS: Forçar Encerrar

Os sistemas mais novos repartem a memória como os outros sistemas. Quando ela acaba, não encerram nada por conta própria: todo programa pausa, e Forçar Encerrar Aplicativos pergunta quais encerrar. Escolha um ou mais e aperte Forçar Encerrar. Quando houver espaço, os programas continuam.

> [!DEFINE] Forçar Encerrar Aplicativos
> A janela que pergunta quais programas encerrar quando a memória acaba.

## Se algo der errado

> [!PROBLEM] Um sistema clássico diz que não há memória suficiente para abrir um programa.
> Encerre um programa para liberar o bloco dele, ou baixe o tamanho mínimo do programa em Obter Informações.

> [!PROBLEM] Todos os programas do Quince OS X param de se mexer.
> A memória acabou: escolha o que encerrar em Forçar Encerrar Aplicativos.

> [!SEE] jsc:quince_systems, jsc:quince_crashes
