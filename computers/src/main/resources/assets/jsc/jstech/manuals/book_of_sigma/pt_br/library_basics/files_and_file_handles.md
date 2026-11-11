---
section: library_basics
order: 440
---
# Arquivos: arquivos inteiros e arquivos abertos

Um arquivo é um dado com nome guardado em um disco. Seu caminho é o nome completo: a letra da unidade que nomeia o disco, depois as pastas e, por fim, o nome do próprio arquivo.

Há duas maneiras de trabalhar com um arquivo. File trabalha com um arquivo inteiro de uma vez: lê tudo ou escreve tudo. File.Open entrega um FILE, que você lê e escreve um pedaço por vez até fechá-lo.

## Arquivos inteiros

Table: Membros de File

| Membro | O que faz |
| --- | --- |
| `File.Exists(path)` | Sim quando o arquivo pode ser lido. |
| `File.Read(path)` | O texto inteiro do arquivo. Interrompe o programa quando o arquivo está ausente. |
| `File.TryRead(path, out text)` | True e o texto quando o arquivo é lido. False e texto vazio caso contrário. |
| `File.Write(path, text)` | Substitui o arquivo pelo texto. False quando a escrita não aconteceu. |
| `File.Append(path, text)` | Adiciona o texto ao final do arquivo. False quando o acréscimo não aconteceu. |
| `File.Delete(path)` | Remove o arquivo. False quando recusado. |
| `File.MkDir(path)` | Cria uma pasta. False quando recusado. |
| `File.List(path)` | Os nomes de uma pasta, em forma de lista. |
| `File.Open(path, mode)` | Abre um arquivo para trabalhar nele pedaço por pedaço: devolve um FILE, ou null. Versão 2. |
| `File.Move(from, to)` | Renomeia o arquivo. False quando recusado. Versão 2. |

Exists, Read e TryRead apenas olham um arquivo. Write, Append, Delete, MkDir e Move alteram o disco, e cada um deles responde True quando a alteração aconteceu.

## Um arquivo ausente

File.Read interrompe o programa quando o arquivo está ausente ou não pode ser lido. O console então mostra a mensagem do arquivo, como "<caminho>: arquivo não encontrado". Use File.TryRead quando quiser perguntar antes e seguir de qualquer jeito.

## Arquivos que você abre: FILE

Um FILE é um arquivo que o programa abriu. Ele surgiu na versão 2, e todos os seus membros também são da versão 2. O programa trabalha com a cópia própria do texto, e nada chega ao disco até você chamar Close, ou até o programa terminar com o arquivo ainda aberto.

Table: Membros de FILE

| Membro | O que faz |
| --- | --- |
| `FILE.Close()` | Grava o texto no disco, se ele mudou, e fecha o arquivo. |
| `FILE.ReadLine(out line)` | Devolve a próxima linha. False no fim, com uma linha vazia. |
| `FILE.Read()` | O próximo caractere como número, ou -1 no fim. |
| `FILE.Scan(out value)` | Lê um valor do tipo da variável. |
| `FILE.Write(text)` | Escreve o texto no arquivo. |
| `FILE.Write(char)` | Escreve um caractere. |
| `FILE.Seek(place)` | Move para uma posição, contada em caracteres a partir do início e mantida entre 0 e o tamanho do arquivo. |
| `FILE.Position` | A posição atual, em caracteres a partir do início. |
| `FILE.AtEnd` | Sim quando a posição está no fim do arquivo. |

## Modos

O modo diz como um arquivo é aberto. As letras b e t são ignoradas, então rb significa r.

Table: Modos de File.Open

| Modo | O que faz |
| --- | --- |
| `r` | Lê um arquivo que existe. Devolve null quando ele está ausente. |
| `w` | Escreve um arquivo e o inicia vazio. |
| `a` | Acrescenta texto ao final do arquivo. |
| `r+` | Lê e escreve um arquivo que existe. |
| `w+` | Escreve um arquivo que começa vazio, e também o lê. |
| `a+` | Acrescenta ao final, e também lê. |

> [!WARNING] Abrir um arquivo com w o inicia vazio, então o texto que ele tinha é substituído pelo que o programa escrever.

> [!DEFINE] Modo
> O modo como um arquivo é aberto: leitura, escrita, acréscimo no final, ou os dois, com um sinal de mais.

## Se algo der errado

> [!PROBLEM] O programa para e diz que o arquivo não foi encontrado.
> Confira o caminho, ou use File.TryRead para perguntar antes, sem parar o programa.

> [!PROBLEM] Write ou Append devolve False, e a mensagem do arquivo diz que não há espaço livre suficiente no disco.
> Libere espaço no disco, ou escreva um texto menor.

> [!PROBLEM] Write devolve False, e a mensagem diz que o arquivo é somente leitura.
> Escolha outro caminho. Um arquivo com terminação .dat é somente leitura, e a mensagem indica o Interator de Rede para os itens.

> [!PROBLEM] Write devolve False, e a mensagem diz que o nome do arquivo não é válido para este sistema de arquivos.
> Use um nome que esta unidade aceite.

> [!PROBLEM] Delete devolve False para um arquivo .dat.
> Use o Interator de Rede, como diz a mensagem. Arquivos .dat não podem ser excluídos daqui.

> [!PROBLEM] Move devolve False, e a mensagem diz que já existe um arquivo com esse nome.
> Escolha um novo nome, ou exclua antes o arquivo que está lá.

> [!PROBLEM] Move devolve False: um arquivo em outra máquina é copiado, não movido.
> Leia o arquivo com File.Read, escreva-o com File.Write e depois exclua o original com File.Delete.

> [!PROBLEM] MkDir devolve False: a unidade não tem pastas.
> Crie a pasta em um disco que aceite pastas.

> [!PROBLEM] Open devolve null.
> Confira se o modo está na lista acima, se o caminho não está em branco e se um arquivo lido no modo r existe. Um programa também pode manter só uma certa quantidade de arquivos abertos ao mesmo tempo: 8 em uma máquina Vintage, 20 em uma máquina Legacy e 64 em todas as outras eras. Feche um arquivo que você não precise mais.

> [!PROBLEM] O programa para e diz que o arquivo foi aberto com "r", que não escreve.
> Abra o arquivo em um modo que escreve, como w, a ou r+.

> [!PROBLEM] O programa para e diz que o arquivo foi aberto com "w", que não lê.
> Abra o arquivo em um modo que lê, como r, r+ ou a+.

> [!PROBLEM] O programa para e diz que o arquivo está fechado.
> Abra o arquivo de novo com File.Open antes de usá-lo.

> [!SEE] jsc:disks, jsc:drives
