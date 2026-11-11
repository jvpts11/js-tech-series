---
section: disk_tools
order: 305
---
# Dividindo um disco (fdisk)

O fdisk é o editor que grava a tabela de partições de um disco: a lista de onde cada partição começa e termina. Você o abre sobre um disco inteiro, e ele pergunta o que fazer em seguida.

O editor trabalha na memória. Nada chega ao disco até você digitar w, então você pode tentar coisas e descartá-las. Sair com q, ou com [[Ctrl]]+[[C]], descarta tudo o que você digitou desde que o editor foi aberto.

> [!WARNING] Gravar com w substitui a tabela de partições do disco, e as partições que ela tinha antes somem.

Table: Os comandos do fdisk

| Letra | O que faz |
| --- | --- |
| `m` | Imprime o menu de comandos; help faz o mesmo. |
| `g` | Cria uma tabela de partições GUID (GPT) nova e vazia, e remove as partições mostradas. |
| `n` | Adiciona uma partição, por um tamanho ou por três perguntas. |
| `t` | Altera o tipo de uma partição. |
| `d` | Apaga uma partição. |
| `p` | Imprime a tabela: o disco, o rótulo dele e suas partições. |
| `w` | Grava a tabela no disco e sai do editor. |
| `q` | Sai do editor sem gravar nada. |

## Criando uma tabela nova (g)

Um disco novo não tem tabela de partições, e o fdisk avisa isso quando você o abre. Digite g para criar uma GPT, o tipo moderno que as máquinas UEFI (Unified Extensible Firmware Interface) leem. O tipo antigo, que o p imprime como dos, não é um que este editor consegue criar.

A linha que g imprime mostra o GUID do disco, um identificador longo que permanece o mesmo para aquele disco. Somente g cria um novo GPT, embora um disco que já tenha partições também possa ser GPT.

## Criando uma partição (n)

Com um tamanho na linha, o n cria a partição de imediato. Digite n +512M para 512 megabytes, ou n +1G para um gigabyte. Um número simples, como "n 1024", também significa megabytes.

Digitado sozinho, o n faz três perguntas em sequência. Para as duas primeiras, pressione [[Enter]]: o editor escolhe o número e o setor inicial para você. A terceira pergunta onde a partição termina, e a pergunta mostra o intervalo. Em um disco de 1G, ela aparece assim:

"Último setor, +/-setores ou +/-tamanho{K,M,G,T,P} (2048-2097118, padrão 2095103):"

Pressione [[Enter]] para usar o resto do disco, ou digite um tamanho como +512M. Um setor tem 512 bytes, e um megabyte tem 2.048 setores, então a pergunta também aceita um número de setor.

Quando a partição é criada, o editor avisa: "Criada uma nova partição 1 do tipo 'Linux filesystem' e de tamanho 512 MiB." Toda partição nova começa como Linux filesystem. Você a altera com t, quando ela for a partição de sistema EFI (Extensible Firmware Interface).

## Escolhendo o tipo (t)

Table: As palavras que definem um tipo

| Tipo | Palavras que você pode digitar |
| --- | --- |
| EFI System | `1, uefi, efi, ef` |
| Linux filesystem | `20, linux, 83` |

Digite L para ver os tipos que o editor conhece. Ele imprime duas linhas, EFI System e Linux filesystem, e depois pergunta de novo. Com uma única partição no disco, t a seleciona e pede o tipo. Com várias, ele pergunta primeiro qual partição, e Enter escolhe a última.

Quando a mudança funciona, o editor imprime o tipo antigo e o novo, exatamente como os escreve: "Tipo da partição 'Linux filesystem' alterado para 'EFI System'."

## Imprimindo, apagando e gravando (p, d, w)

p imprime a tabela como o editor a vê, antes de qualquer coisa ser gravada. Ela começa com o tamanho do disco e depois uma linha para cada partição. As linhas mostram Device, Start, End, Sectors, Size e Type, nessa ordem.

d apaga uma partição e renumera as restantes a partir da partição 1. Depois o editor imprime "A partição 2 foi excluída.", com o número da que você escolheu.

w grava a tabela no disco. O editor então diz que a tabela de partições foi alterada e que está sincronizando os discos. Depois disso as partições aparecem no lsblk, e o mkfs pode usá-las.

## As etapas para um disco UEFI

1. Digite fdisk /dev/sda e pressione [[Enter]].
2. Digite g e pressione [[Enter]], para criar uma GPT.
3. Digite n +512M e pressione [[Enter]], para a primeira partição.
4. Digite t e pressione [[Enter]], depois digite 1 para a partição de sistema EFI.
5. Digite n e pressione [[Enter]] três vezes, para criar a segunda partição com o resto do disco.
6. Digite w e pressione [[Enter]], para gravar a tabela e sair.

> [!DEFINE] GPT
> O tipo moderno de tabela de partições, que as máquinas UEFI leem.

> [!DEFINE] GUID
> Um identificador globalmente único: um código longo, que parece aleatório, dado a uma tabela de partições ou a uma partição para que ela possa ser distinguida de outras.

> [!DEFINE] Setor
> A menor parte de um disco que a tabela conta: 512 bytes.

> [!DEFINE] Partição de sistema EFI
> Uma partição pequena que o firmware lê para encontrar o gerenciador de inicialização. Só máquinas UEFI precisam dela.

## Se algo der errado

> [!PROBLEM] fdisk: uso incorreto
> Você não informou um disco. Digite fdisk /dev/sda, com o nome do seu disco que o lsblk mostrou.

> [!PROBLEM] fdisk: não foi possível abrir /dev/sda2: Arquivo ou diretório inexistente
> O fdisk abre apenas um disco inteiro, não uma partição. Use o nome do disco, como sda.

> [!PROBLEM] O dispositivo não contém uma tabela de partições reconhecida.
> Isto é uma informação, não uma recusa: o disco ainda não tem partições. Digite g para criar uma GPT, depois n.

> [!PROBLEM] Nenhuma partição foi definida ainda!
> t e d precisam de uma partição para atuar. Crie uma com n primeiro.

> [!PROBLEM] Valor fora do intervalo.
> O tamanho ou o número é grande ou pequeno demais para este disco. Use um tamanho menor, ou um número da lista que o p imprime.

> [!PROBLEM] Nenhum setor livre disponível.
> O disco está cheio, ou uma partição anterior ocupou o resto dele, ou há 128 partições. Apague a última partição com d. Uma partição que ocupou o resto precisa ser apagada e criada de novo com um tamanho.

> [!PROBLEM] Falha ao interpretar o tipo de partição 'swap'.
> Digite 1 ou uefi para uma partição de sistema EFI, 20 ou linux para um sistema de arquivos Linux, ou L para ver a lista.

> [!PROBLEM] x: comando desconhecido
> A letra x não é um comando do editor. Digite m para ver os comandos que ele tem.

> [!PROBLEM] mkfs.fat: sda1 não é uma partição EFI System.
> O tipo da partição nunca foi definido como EFI. Execute fdisk /dev/sda, depois digite t, depois digite 1 para EFI System, depois digite w.

> [!PROBLEM] Você sai com q ou [[Ctrl]]+[[C]], e suas alterações se perdem.
> Os dois saem sem gravar. Só o w mantém as alterações, então digite w antes de sair.

> [!SEE] jsc:firmware, jsc:eras, jsc:arch_bootloader
