# Relatório de qualidade, métricas e auditoria

## O que foi testado?

Foram documentados e executados 31 casos de teste para ID, nome, descrição, categoria, status, prazo e data de criação. O executor `testes/ProjetoServiceTestes.java` valida as regras da camada de serviço sem dependência de JUnit. Também foram verificados os endpoints da API para cadastro, consulta, alteração e exclusão, incluindo respostas de erro para dados inválidos.

## Quais erros foram encontrados e como o sistema reage?

Os testes verificam a rejeição dos seguintes casos:

- ID não numérico, zero, negativo ou duplicado.
- Nome vazio, com menos de 3 ou mais de 100 caracteres.
- Descrição com menos de 10 ou mais de 500 caracteres.
- Categoria fora do catálogo `Software`, `Mobile`, `Web`, `Desktop`.
- Status fora da lista `Planejado`, `Em Desenvolvimento`, `Concluído`, `Cancelado`.
- Data inexistente, como `31/02/2026`, ou prazo anterior à data de criação.
- Erros de leitura do CSV, falhas de conexão e requisições inválidas à API.

Quando uma validação falha, a API responde com HTTP 400 e registra o erro em `error.log`. Falhas de autenticação ou ausência de sessão são recusadas pela API; as requisições são registradas em `api.log`.

## Quais métricas o dashboard mostra?

Os indicadores mostram total de projetos, concluídos, ativos (planejados + em desenvolvimento) e cancelados. A área de relatórios apresenta quatro gráficos: distribuição por status, projetos por categoria, evolução mensal com base em `dataCriacao` e cumprimento de prazos (no prazo/atrasados). Projetos sem prazo informado não entram no gráfico de cumprimento de prazos.

## O que os logs permitem auditar?

- `application.log`: inicialização, encerramento e eventos gerais da aplicação.
- `access.log`: cadastro, login, logout e tentativas de acesso negadas, com usuário e data/hora.
- `audit.log`: criação, alteração e exclusão de projetos, com ID.
- `error.log`: validações recusadas, erros de CSV, erros da API e falhas informadas pela interface.
- `api.log`: método HTTP, rota e código de resposta.

Os logs são gravados automaticamente na pasta `logs/`, usando data/hora no formato `yyyy-MM-dd HH:mm:ss`.
