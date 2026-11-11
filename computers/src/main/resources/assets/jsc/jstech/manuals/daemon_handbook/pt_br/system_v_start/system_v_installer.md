---
section: system_v_start
order: 350
---
# O instalador

Você instala o UNIX System V pelo seu disquete, com o instalador do System V. O instalador pergunta duas coisas, o disco e o nome do computador, e então copia o sistema para o disco. Ele roda em uma janela, com um banner em vídeo invertido o tempo todo.

Você precisa de um computador Vintage ou mais recente, com uma Unidade de Disquete ligada a ele, e de um disco com pelo menos 10 MB de espaço livre. Ao terminar, o disco tem o UNIX System V, e o computador o inicia depois de reiniciar.

1. Coloque o disquete do System V em uma Unidade de Disquete ligada ao computador, e inicie o computador por ele ([](jsc:installing)).
2. Na página de configurações, pressione [[Up]] ou [[Down]] para escolher o disco.
3. Digite o nome do computador na caixa Nome do computador.
4. Pressione [[Enter]] para continuar. Se o disco já tiver um sistema, pressione [[1]] ou [[2]] antes.
5. Espere a cópia terminar. Fechar o monitor não a interrompe.
6. Na página de conclusão, retire o disquete e pressione [[Enter]] para reiniciar.

Table: As três páginas

| Página | O que mostra |
| --- | --- |
| Configurações | A pergunta sobre o disco, a lista de discos e a caixa Nome do computador. |
| Cópia | Quatro etapas, cada uma marcada como copiando, aguardando ou com uma marca verde: Sistema base, Utilitários, Ajuda online e Páginas de manual. |
| Concluído | O nome, o espaço usado e, quando você instalou ao lado de um sistema, a ordem de inicialização. |

A página de configurações pergunta "Escolha o disco para instalar o UNIX System V:" e mostra cada disco em uma linha. Uma linha começa com o número do slot e o rótulo do disco, e depois mostra o tamanho, o espaço livre e o que ele contém, ou "Nada".

> [!DEFINE] Compartimento de disco
> O lugar no computador onde um disco fica. O instalador mostra o lugar como um número.

Um disco que já tem um sistema pergunta antes. A pergunta diz "<rótulo do disco> contém <o que ele contém>. O que deve acontecer com ele?"

Table: Sua escolha em um disco que tem um sistema

| Escolha | O que acontece |
| --- | --- |
| `Erase <disk> and install the UNIX System V` | O disco é apagado, e o novo sistema fica nele sozinho. |
| `Install the UNIX System V beside <what it holds>` | O novo sistema vai para o mesmo disco, ao lado do antigo. Esta opção fica desativada quando o disco não tem espaço para ela. |

Enquanto a cópia roda, a página da cópia mostra o nome do disco que você escolheu. Abra o monitor de novo para ver o progresso.

A página de conclusão diz "O UNIX System V está instalado em <disk>." e depois mostra o nome do computador e o espaço usado no disco.

## Se algo der errado

> [!PROBLEM] A página de configurações diz "Sem espaço aqui. Apague este disco, ou escolha outro."
> O disco tem pouco espaço livre para o sistema. Escolha outro disco da lista, com pelo menos 10 MB livres.

> [!PROBLEM] A página de configurações diz "Nenhum disco desta máquina tem espaço para ele."
> Coloque um disco com pelo menos 10 MB livres, depois escolha-o na página de configurações.

> [!SEE] jsc:installpkg, jsc:firmware, jsc:two_systems
