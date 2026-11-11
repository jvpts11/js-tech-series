---
section: fab
order: 37
icon: jsc:machine_terminal
covers_family: jsc:building_chain_machines
---
# Montando uma máquina da cadeia

O que uma bancada faz à mão, uma peça de cada vez, as máquinas da cadeia fazem em quantidade: elas britam e fundem o minério, laminam e trefilam o metal, fazem a química, crescem o silício, fazem os chips e prensam as placas e as telas. Cada máquina é um multibloco que você constrói bloco a bloco, na forma que é só dela.

Uma máquina é feita de quatro tipos de bloco. A sua carcaça, uma de cinco pelo trabalho que ela aguenta: a Carcaça de Máquina para as máquinas mecânicas, a Carcaça Refratária para o calor, a Carcaça Revestida para o que corrói, a Carcaça de Vácuo para vácuo e gases, e a Carcaça de Sala Limpa para as Estações de Litografia. Os seus blocos de trabalho, que fazem dela o que ela é: os rolos de um Britador, a teia de fios de uma Serra de Wafers. As suas entradas e saídas, por onde as coisas entram e saem. E o Terminal de Máquina, que a opera.

> [!DEFINE] Terminal de Máquina
> O armário de controle que você coloca no lugar do terminal de uma máquina: quando a forma inteira da máquina está de pé em volta dele, ele se torna o próprio terminal dessa máquina e a opera.

Você distingue as entradas e saídas pelo conector e pela faixa: verde recebe, laranja entrega, amarelo é a energia, e a Porta Periférica ciano liga uma Estação de Litografia ao seu computador. A Entrada de Itens e a Saída de Itens recebem e entregam itens, a Entrada de Líquido e a Saída de Líquido líquidos, a Entrada de Gás e a Saída de Gás gases, e a Entrada de Energia recebe energia.

Construa cada bloco no seu lugar, com o Terminal de Máquina no lugar do terminal, virado para onde você quer a frente da máquina. Um [Projetor de Estruturas](jscore:structure_projector) mostra a forma antes de você construí-la. O terminal procura uma forma inteira em volta dele quando você o coloca, quando um bloco ao lado muda e quando você o usa, e diz quando nenhuma está de pé.

Formada, a máquina é desenhada como uma só: a sua carcaça e os seus blocos de trabalho se fundem no seu modelo, as suas entradas e saídas não desenham nada próprio, e o seu giroflex gira enquanto ela trabalha. Ela mostra no que está trabalhando, com as cores do que entra e do que sai.

Use o terminal para abrir a janela da máquina, com os seus slots, os seus tanques e a energia que ela guarda. As coisas entram pelas suas entradas e saem pelas suas saídas, vindas de e indo para o que as move: um funil, um cano, os barramentos da rede ou uma [Interface de Fabricação](jsc:crafting_interfaces), que a opera para a fabricação automática da rede.

Toda máquina, menos o Forno de Coque, consome a sua energia em baixa tensão pela sua Entrada de Energia, e só enquanto trabalha. O Forno de Coque queima parte do seu próprio carvão, como um de verdade. Num servidor configurado para deixar os seus computadores funcionarem sem energia, as máquinas também funcionam sem.

## Se algo der errado

> [!PROBLEM] O terminal diz que nenhuma máquina está de pé em volta dele.
> Falta um bloco, um está fora do lugar, ou outro está onde a forma pede o seu: compare a forma com a de um Projetor de Estruturas, a partir da frente da máquina.

> [!PROBLEM] A máquina está formada mas não faz nada.
> Nenhuma energia chega à sua Entrada de Energia, o que está nas suas entradas não faz nada que ela conheça, ou as suas saídas estão cheias.

> [!PROBLEM] Quebrar um bloco desmontou a máquina.
> Cada bloco da forma a mantém inteira: ponha o bloco de volta e o terminal a forma de novo. Quebrar o terminal devolve a você o Terminal de Máquina.

> [!SEE] jsc:ore_and_metal_machines, jsc:lithography_stations
