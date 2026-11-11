---
section: library_toolkit
order: 463
---
# Json, Csv, Hash e Base64: dados em texto

Os textos muitas vezes carregam dados: um documento JSON, uma tabela CSV, um código. Estes tipos os leem, todos no System.Text, e vieram com o Sigma# 3. Ler custa o que as instruções custam.

Table: Membros do Json e do JsonValue

| Membro | O que dá |
| --- | --- |
| `Json.Parse(text)` | O documento como um JsonValue. Um texto que não é JSON para o programa com uma FormatException. |
| `JsonValue.Get(name)` | Um membro de um objeto, ou null. Você pode escrever value[name] no lugar. |
| `JsonValue.At(number)` | Um elemento de um array, contado a partir de 0. Você pode escrever value[number] no lugar. |
| `JsonValue.Has(name)` | Sim quando o objeto tem esse membro. |
| `JsonValue.Keys()` | Os nomes dos membros de um objeto, em ordem. |
| `JsonValue.AsString()` | Um texto, um número, true ou false, ou null, escrito como texto. |
| `JsonValue.AsNumber()` | Um número, ou um texto que escreve um. |
| `JsonValue.AsBool()` | true ou false. |
| `JsonValue.Kind` | object, array, string, number, bool ou null. |
| `JsonValue.Count` | Quantos membros ou elementos ele tem, ou o comprimento de um texto. |

Um JsonValue é só de leitura. Monte você mesmo o texto de um documento, com um StringBuilder.

```sgs Prices.sgs
JsonValue prices = Json.Parse(File.Read("prices.json"));
double iron = prices["items"][0]["price"].AsNumber();
```

Table: Membros do Csv, do Hash e do Base64

| Membro | O que dá |
| --- | --- |
| `Csv.Parse(text)` | As linhas de uma tabela CSV, cada uma uma lista dos seus campos. Um campo entre aspas pode ter vírgulas e quebras de linha. |
| `Csv.Write(rows)` | As linhas como CSV, uma por linha, pondo entre aspas os campos que precisam. |
| `Hash.Sha256(text)` | A impressão digital SHA-256 do texto, em hexadecimal. |
| `Hash.Crc32(text)` | A soma de verificação CRC-32 do texto, como número. |
| `Base64.Encode(text)` | O texto escrito em Base64. |
| `Base64.Decode(text)` | O texto que o Base64 guarda. O que não é Base64 para o programa com uma FormatException. |

> [!DEFINE] JSON
> Um jeito de escrever dados como texto: objetos de membros com nome, arrays, textos, números, true, false e null.

> [!DEFINE] CSV
> Uma tabela escrita como texto: uma linha da tabela em cada linha, com os campos separados por vírgulas.

## Se algo der errado

> [!PROBLEM] O programa para e diz que o texto não é JSON.
> Confira o documento no caractere que ele indica. Todo texto em JSON é escrito entre aspas duplas.

> [!PROBLEM] Ler um membro dá null.
> O objeto não tem membro com esse nome. Confira com Has, ou liste os membros com Keys.

> [!SEE] jsc:text_tools_library, jsc:files_and_file_handles
