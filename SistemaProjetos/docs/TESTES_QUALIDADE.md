# Casos de teste - Qualidade do Sistema de Projetos

Os casos abaixo cobrem as regras do documento `ProjetoQualidade.pdf`. O executor automatizado fica em `testes/ProjetoServiceTestes.java` e não exige bibliotecas externas.

| Nº | Campo | Entrada / cenário | Resultado esperado |
|---:|---|---|---|
| 01 | ID | `1` | Aceitar ID positivo. |
| 02 | ID | `0` | Rejeitar ID zero. |
| 03 | ID | `-1` | Rejeitar ID negativo. |
| 04 | ID | `ABC` | Rejeitar valor não numérico. |
| 05 | ID | Repetir um ID já cadastrado | Rejeitar duplicidade. |
| 06 | Nome | `  Portal do Cliente  ` | Aceitar e remover espaços externos. |
| 07 | Nome | Vazio | Rejeitar campo obrigatório. |
| 08 | Nome | `A` | Rejeitar menos de 3 caracteres. |
| 09 | Nome | Apenas espaços | Rejeitar após trim. |
| 10 | Nome | 101 caracteres | Rejeitar acima do limite de 100. |
| 11 | Descrição | Exatamente 10 caracteres | Aceitar limite mínimo. |
| 12 | Descrição | 9 caracteres | Rejeitar abaixo do mínimo. |
| 13 | Descrição | 501 caracteres | Rejeitar acima do máximo de 500. |
| 14 | Descrição | Caracteres especiais | Aceitar texto válido com caracteres especiais. |
| 15 | Categoria | `Software` | Aceitar valor do catálogo. |
| 16 | Categoria | `Mobile` | Aceitar valor do catálogo. |
| 17 | Categoria | `Web` | Aceitar valor do catálogo. |
| 18 | Categoria | `Desktop` | Aceitar valor do catálogo. |
| 19 | Categoria | Vazia | Rejeitar campo obrigatório. |
| 20 | Categoria | `Dados` | Rejeitar valor fora do catálogo. |
| 21 | Status | `Planejado` | Aceitar status válido. |
| 22 | Status | `Em Desenvolvimento` | Aceitar status válido. |
| 23 | Status | `Concluído` | Aceitar status válido. |
| 24 | Status | `Cancelado` | Aceitar status válido. |
| 25 | Status | `Finalizado` | Rejeitar status fora da lista fechada. |
| 26 | Prazo | Data futura | Aceitar quando não anterior à criação. |
| 27 | Prazo | Data atual | Aceitar prazo igual à criação. |
| 28 | Prazo | Data passada posterior à criação | Aceitar; a regra não proíbe atraso por si só. |
| 29 | Prazo | Anterior à data de criação | Rejeitar. |
| 30 | Prazo | `31/02/2026` | Rejeitar data inexistente. |
| 31 | Data de criação | Campo não informado no cadastro | Preencher automaticamente com a data atual. |

## Como executar os testes automatizados

Na pasta `AULA10`, execute:

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
javac -encoding UTF-8 -cp out -d out testes/ProjetoServiceTestes.java
java -cp out testes.ProjetoServiceTestes
```

Os testes chamam a mesma camada `ProjetoService` utilizada pela API e pelo Swing. A Web repete as regras no formulário para dar retorno imediato; a API valida novamente no servidor, garantindo que as duas interfaces tenham o mesmo resultado.

## Observação sobre datas

As datas são armazenadas em `yyyy-MM-dd`. Para entrada manual no Swing, também é aceito `dd/MM/yyyy`. O prazo é opcional; quando informado, deve ser uma data real e não pode ser anterior à data de criação. Um prazo passado é válido apenas quando continua igual ou posterior à data de criação.
