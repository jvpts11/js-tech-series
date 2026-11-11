---
section: mod_integrations
order: 159
---
# Visualizadores de receitas: JEI e EMI

Um visualizador de receitas é uma tela que lista como as coisas são feitas. JEI e EMI são dois deles, e cada um é opcional. Com o JEI instalado, você pode enviar uma receita do visualizador ao Pattern Studio, que escreve os padrões para a autofabricação ([](jsc:pattern_studio)).

No JEI, cada receita que você pode enviar tem um botão de transferência. Pressioná-lo move os ingredientes da receita para o Pattern Studio. O Studio tem duas abas para isso, e a receita vai para a que couber.

## Onde a receita cai

Table: Onde cai uma receita

| Receita | Cai em | O que você vê |
| --- | --- | --- |
| Uma receita de fabricação | A aba Bancada | Os seus ingredientes preenchem a grade na ordem do visualizador. |
| Qualquer outra receita | A aba Máquina | Uma célula de dados para cada entrada e cada saída. |

A aba Bancada tem uma grade de nove células, três por três. Os ingredientes a preenchem linha por linha, na ordem em que o visualizador os mostra. Qualquer ingrediente além do nono fica de fora.

A aba Máquina recebe as outras receitas, como as das máquinas do J's Industrial. Cada entrada e cada saída vira ali uma célula de dados: um item com a sua quantidade, ou um fluido em milibaldes (mB).

> [!DEFINE] Visualizador de receitas
> Uma tela de um mod que lista receitas: quais ingredientes formam qual resultado.

> [!DEFINE] Botão de transferência
> O botão de um visualizador de receitas que envia os ingredientes de uma receita ao Pattern Studio.

> [!DEFINE] Lista de ingredientes
> O painel lateral de um visualizador de receitas que lista os itens que você pode usar como ingredientes.

> [!DEFINE] Milibalde (mB)
> A unidade de volume de fluidos na rede: um milésimo de balde.

## Antes de a transferência funcionar

O Pattern Studio precisa ser a janela da frente da sua área de trabalho, ou o botão de transferência recusa e o visualizador mostra um erro.

Você também pode arrastar um item da lista de ingredientes para uma célula do Studio. A célula recebe esse item, como se você o tivesse clicado com o item no cursor.

## EMI e Industrial

O EMI lista as mesmas receitas e pode mostrar uma das suas categorias. Uma receita não passa do EMI para o Pattern Studio, então use o JEI quando quiser uma transferência.

Quando o J's Industrial está instalado, as suas receitas de maceração e de compressão também aparecem no JEI e no EMI, cada uma na sua própria categoria.

## Se algo der errado

> [!PROBLEM] O botão de transferência mostra "Abra o Pattern Studio para transferir receitas".
> Abra o Studio na área de trabalho, traga-o para a frente e pressione o botão de novo.

> [!PROBLEM] A receita cai nas células erradas.
> A aba Bancada preenche a grade na ordem do visualizador. Reorganize as células na aba Bancada.

> [!PROBLEM] A aba Máquina diz "O rascunho de máquina precisa de uma entrada e uma saída."
> Uma receita sem itens ou fluidos que possam ser mapeados deixa o rascunho incompleto. Adicione a entrada ou a saída que faltam na aba Máquina.

> [!PROBLEM] O Studio diz "Não é uma receita de máquina: <name>".
> Só as receitas que as máquinas fabricam cabem na aba Máquina. Escolha uma receita que uma máquina fabrique.

> [!SEE] jsc:pattern_studio, jsc:pattern_encoders
