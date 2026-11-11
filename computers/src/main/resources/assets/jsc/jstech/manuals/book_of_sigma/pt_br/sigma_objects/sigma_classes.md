---
section: sigma_objects
order: 417
---
# Classes, structs e enums

Uma classe é um molde, e um objeto é algo feito a partir dele. Você declara uma classe com a palavra class, e cria um objeto dela com new. Um programa declara os seus tipos no topo de um namespace, e cada tipo pode ter campos, construtores e métodos.

Table: Os tipos existentes

| Tipo | Como os seus valores se comportam | Sigma | Sigma# |
| --- | --- | --- | --- |
| class | Cada valor é um objeto, e os nomes compartilham esse objeto. | Sim | Sim |
| struct | Cada valor é copiado ao ser armazenado ou passado adiante, e comparado pelos seus campos. | Sim | Sim |
| enum | Uma lista de números nomeados, como Low e High. | Sim | Sim |
| interface | Uma lista de métodos que uma classe precisa implementar. | Não | Sim |
| record | Um tipo semelhante a uma classe, formado por campos somente leitura, comparado pelos seus valores. | Não | Sim |
| delegate | Uma assinatura nomeada de método, que um valor pode guardar. | Não | Sim |

Um struct é um tipo de valor. Os seus valores são copiados quando você os armazena ou passa adiante, então uma mudança na cópia não altera o original. Um enum é uma lista de números nomeados. Cada nome é um valor do tipo enum, e você escreve Level.Mid para usar um deles. O primeiro nome recebe o número 0, e cada nome depois dele recebe o número seguinte, a menos que você escreva o número por conta própria.

```sg Shop.sg
namespace Shop;

enum Level { Low, Mid = 5, High }     // Low is 0, Mid is 5, High is 6

struct Money { public int Cents; }     // copied when stored

class Till
{
    private int sales;
    public Till(int start) { sales = start; }
    public void Add(int cents) { sales += cents; }
    public int Total() { return sales; }
}
```

Um construtor tem o nome da sua classe e não devolve nada. Ele é executado quando você cria um objeto com new e o prepara. Till till = new Till(100); cria um Till que começa com 100 centavos. Uma classe sem construtor recebe um construtor vazio. Um construtor pode passar o controle para outro construtor da própria classe com a palavra-chave this, ou para a classe base com a palavra-chave base.

Você acessa um membro com um ponto. O membro de um objeto é escrito com o nome do objeto, como till.Total(). Um membro estático é escrito com o nome do tipo, como Level.Mid para um valor de enum. A palavra-chave this significa o objeto atual, e base significa a classe base. Um campo marcado como readonly só pode ser escrito onde é declarado ou em um construtor. A palavra const fica guardada para uma versão futura da linguagem: por enquanto, escreva um campo static readonly.

As palavras public, private, protected e internal dizem quem pode chegar a um membro, como no C#. Um membro public ou internal é alcançado de qualquer parte do programa. Um private só é alcançado de dentro do seu próprio tipo e dos tipos aninhados nele, e um membro escrito sem palavra de acesso é private. Um protected é alcançado do seu próprio tipo e dos tipos que se apoiam nele. Chegar a um membro de onde a sua palavra não deixa é o erro S3060.

O Sigma# também tem interfaces, records e delegates, que são explicados em [Interfaces e records](jsc:sigma_interfaces_records) e [Propriedades, eventos, delegates e lambdas](jsc:sigma_delegates_events).

> [!DEFINE] Struct
> Um tipo de valor, declarado com a palavra struct, que é copiado sempre que é armazenado ou passado adiante.

> [!DEFINE] Enum
> Um tipo cujos valores são uma lista de nomes, e cada nome tem um número.

> [!DEFINE] Campo
> Uma variável declarada dentro de uma classe ou de um struct, que pertence a cada objeto.

> [!DEFINE] Construtor
> Um método especial que é executado quando você cria um objeto com new, com o mesmo nome da classe.

> [!DEFINE] Namespace
> Um grupo nomeado de tipos, como Shop. Um arquivo coloca os seus tipos em um mesmo namespace, com uma linha namespace.

> [!DEFINE] Interface
> Uma lista de assinaturas de métodos que uma classe se compromete a implementar.

> [!DEFINE] Classe base
> A classe da qual outra classe herda, escrita depois de dois-pontos.

> [!DEFINE] Record
> Um tipo semelhante a uma classe, formado por campos somente leitura, comparado pelos seus valores.

> [!DEFINE] Delegate
> Uma assinatura nomeada de método. Um valor de um tipo delegate pode conter qualquer método com essa assinatura.

## Se algo der errado

> [!PROBLEM] O compilador informa que 'Till' não tem nenhum membro chamado 'Count'.
> Confira a grafia e confirme que a classe declara esse membro.

> [!PROBLEM] O compilador informa que 'Add' pertence a um objeto, não ao tipo.
> Chame-o em um objeto, como till.Add(50).

> [!PROBLEM] Você marca um campo como readonly, e depois o escreve em um método: o compilador diz que 'count' é readonly, então ele só pode ser escrito onde foi declarado ou em um construtor.
> Escreva-o no construtor, ou retire o readonly.

> [!PROBLEM] O compilador informa que o número de um enum precisa ser escrito como número.
> Escreva o número como um inteiro simples, como Mid = 5.

> [!PROBLEM] O compilador informa que 'Add' já está declarado aqui.
> Renomeie um dos dois métodos, ou dê ao segundo parâmetros diferentes.

> [!SEE] jsc:sigma_inheritance, jsc:sigma_interfaces_records
