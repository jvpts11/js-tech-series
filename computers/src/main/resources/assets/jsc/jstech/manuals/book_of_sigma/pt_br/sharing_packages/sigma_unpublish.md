---
section: sharing_packages
order: 496
---
# Tirar um pacote do Mirror

Despublicar remove um pacote do Mirror, então nenhum computador pode instalá-lo depois disso. Você nomeia o pacote, não o arquivo. O comando é sgpack unpublish, seguido do nome do pacote.

1. Digite sgpack unpublish mypkg e pressione [[Enter]].
2. Leia a resposta: removeu mypkg do Mirror.

Qualquer jogador da rede pode tirar qualquer pacote do Mirror. O Mirror não guarda registro de quem publicou um pacote, então nada impede um jogador de remover o pacote de outro. Converse com os outros jogadores antes de tirar um pacote que eles usam.

## Se algo der errado

> [!PROBLEM] O comando diz usage: sgpack unpublish <name>.
> Digite o nome do pacote depois de unpublish.

> [!PROBLEM] O comando diz que o Mirror não está atendendo o nome que você digitou.
> O nome não está no Mirror. Confira a grafia e tente de novo.

> [!PROBLEM] O comando diz que não foi possível resolver mirror://.
> Nenhum Mirror atende esta rede. Conecte-se a uma rede cujo Mainframe execute o Mirror ([](jsc:mirror)).

> [!SEE] jsc:sigma_publish_mirror
