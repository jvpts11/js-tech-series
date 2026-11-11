---
section: installing_linux
order: 288
---
# Instalando o Fedora

O Fedora tem seu próprio instalador, intitulado "Fedora installer". Ele não passa página por página. Ele mostra um hub: uma lista de todas as perguntas, cada uma marcada como respondida ou como pendente. Você responde às perguntas uma a uma, e volta ao hub depois de cada uma.

Abra uma pergunta pelo seu número, de 1 a 9, ou mova o cursor com UP e DOWN e pressione ENTER. Quando o hub não listar nada como pendente, pressione B para começar a instalação. O rodapé do hub diz isso: "Uma letra inicia a instalação quando nada mais é necessário."

Table: As perguntas do instalador do Fedora

| Página | Título na página | O que você faz |
| --- | --- | --- |
| Hub | Instalação | Lista todas as perguntas. Abra uma, responda e volte. |
| Disco | Destino da Instalação | Escolha o disco e depois pressione ENTER. |
| Área de trabalho | Seleção de software | Escolha uma área de trabalho ou nenhuma. Só aparece quando um Mirror responde. |
| Nome | Nome de host | Digite o nome do computador e depois pressione ENTER. |
| Cópia | Progresso | Espere pelas etapas, do ambiente até o nome de host. |
| Concluído | Concluído! | Retire a mídia de instalação e depois pressione ENTER para reiniciar. |

A dica na parte de baixo do hub diz "Faça uma escolha entre as opções acima ['b' para começar a instalação, 'q' para sair, 'r' para atualizar]:". O 'q' e o 'r' não fazem nada aqui. Para sair, pressione F3 antes que qualquer coisa seja gravada em um disco.

1. Inicialize o computador a partir da mídia de instalação do Fedora.
2. Pressione 1 para abrir a primeira pergunta.
3. Responda e depois pressione ENTER.
4. Abra cada pergunta que o hub ainda lista como pendente, e responda.
5. Pressione B para começar a instalação.

## Se algo der errado

> [!PROBLEM] O hub ainda lista uma pergunta como pendente.
> Abra essa pergunta pelo seu número, responda e depois pressione B.

> [!PROBLEM] A dica diz 'q' para sair, mas pressionar q não faz nada.
> Pressione F3 para sair. Isso só funciona antes de qualquer coisa ser gravada em um disco.

> [!SEE] jsc:installing_linux_keys, jsc:dnf
