---
section: sigma_objects
order: 418
---
# Herança: virtual, override e abstract

A herança permite que uma classe se construa sobre outra. Uma classe que herda de uma classe base tem todos os membros da classe base, e pode acrescentar novos ou substituir alguns. Você escreve a classe base depois de dois-pontos, como em class Car : Vehicle. Uma classe tem no máximo uma classe base.

Um método que uma classe derivada pode substituir precisa ser marcado como virtual na classe base. A classe derivada o substitui com override, e mantém os mesmos parâmetros. Sem override, um método com o mesmo nome e os mesmos parâmetros de um método acima dele é recusado, assim um método nunca fica oculto por acidente.

```sg Garage.sg
namespace Garage;

class Vehicle
{
    public virtual string Name() { return "vehicle"; }
}

class Car : Vehicle
{
    public override string Name() { return "car"; }
}
```

Table: Palavras da herança

| Palavra | Sigma | Sigma# |
| --- | --- | --- |
| `virtual` | Sim | Sim |
| `override` | Sim | Sim |
| `abstract` | Não | Sim |

Um método abstrato não tem corpo. Ele diz que cada classe que deriva desta deve escrever um. Um método marcado como abstract exige uma classe abstract, e uma classe abstract não pode ser criada com new. O Sigma tem virtual e override, mas não tem classes abstratas nem métodos abstratos. O seu compilador os recusa e sugere uma classe com métodos virtuais que não fazem nada.

Para chamar a versão de um método que você substituiu, escreva base.Name(args). Isso chama a versão da classe base. Uma struct não pode ter classe base, e um método estático não pode ser virtual, override ou abstract.

> [!DEFINE] Herança
> Uma forma de uma classe herdar os membros de outra classe, e depois acrescentar a eles ou substituir alguns deles.

> [!DEFINE] Virtual
> Marca um método que uma classe derivada pode substituir.

> [!DEFINE] Override
> Marca um método que substitui um método virtual da classe base.

> [!DEFINE] Abstrato
> Marca um método sem corpo, ou uma classe que não pode ser criada com new, o que exige uma classe derivada para completá-lo.

## Se algo der errado

> [!PROBLEM] O compilador diz que atrás dos dois-pontos só pode vir uma classe ou uma interface.
> Indique uma classe como classe base. As interfaces depois dos dois-pontos existem apenas no Sigma#.

> [!PROBLEM] O compilador diz que 'Name' já existe em 'Vehicle', então escreva com override para substituí-lo.
> Acrescente override antes do método.

> [!PROBLEM] O compilador diz que 'Name' em 'Vehicle' não é virtual, então nada pode substituí-lo.
> Acrescente virtual ao método da classe base.

> [!PROBLEM] O compilador diz que 'Name' está escrito com override, mas nada acima dele o tem.
> Remova override, ou verifique se o nome e os parâmetros coincidem com os do método acima.

> [!PROBLEM] O compilador diz que 'Draw' não tem corpo, então 'Shape' precisa ser declarada abstract.
> No Sigma#, declare a classe como abstract, ou dê um corpo ao método.

> [!PROBLEM] O compilador diz que 'Shape' é abstract, então não pode ser criada com new.
> Crie um objeto de uma classe que derive dela, e dê a essa classe os corpos de que ela precisa.

> [!SEE] jsc:sigma_entry_points, jsc:sigma_classes
