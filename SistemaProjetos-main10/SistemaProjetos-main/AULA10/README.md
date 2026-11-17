# Sistema de Projetos

Aplicação acadêmica para cadastro, organização e acompanhamento de projetos. O sistema possui duas interfaces — **Java Swing (desktop)** e **Web (navegador)** — que utilizam a mesma API HTTP em Java e compartilham os arquivos CSV de dados.

O acesso é protegido por cadastro e login. Após autenticar, o usuário encontra cinco áreas principais: **Dashboard, Projetos, Categorias, Relatórios e Configurações**.

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Tecnologias utilizadas](#tecnologias-utilizadas)
- [Estrutura do projeto](#estrutura-do-projeto)
- [Requisitos](#requisitos)
- [Como executar no Windows](#como-executar-no-windows)
- [Como executar no Linux ou macOS](#como-executar-no-linux-ou-macos)
- [Como executar os testes](#como-executar-os-testes)
- [Áreas do sistema](#áreas-do-sistema)
- [Regras de validação](#regras-de-validação)
- [API HTTP](#api-http)
- [Armazenamento dos dados](#armazenamento-dos-dados)
- [Logs e auditoria](#logs-e-auditoria)
- [Documentação complementar](#documentação-complementar)
- [Observações e limitações](#observações-e-limitações)

---

## Funcionalidades

### Autenticação

- Cadastro de conta com nome, e-mail e senha.
- Login e logout nas versões Swing e Web.
- A mesma conta pode ser utilizada nas duas interfaces.
- Senhas armazenadas como hash PBKDF2-HMAC-SHA256 com salt individual; a senha original não é gravada no CSV.
- Rotas de projetos protegidas por token de sessão.

### Gerenciamento de projetos

- Cadastro, listagem, consulta, alteração e exclusão (CRUD).
- Pesquisa por nome, categoria e status.
- Ordenação das colunas e paginação.
- Identificação visual dos status.
- Registro da data de criação e de um prazo opcional.
- Persistência dos registros em CSV.

### Qualidade, métricas e auditoria

- Validação dos campos na camada de serviço e novamente na API.
- Dashboard com indicadores gerais.
- Relatórios com gráficos por status, categoria, evolução mensal e cumprimento de prazos.
- Exportação da relação de projetos em CSV.
- Registro de eventos de aplicação, acesso, auditoria, erros e requisições da API.
- 31 cenários de teste documentados e um executor automatizado sem JUnit.

---

## Tecnologias utilizadas

| Área | Tecnologia |
|---|---|
| Linguagem principal | Java |
| Interface desktop | Java Swing e Java2D |
| Servidor HTTP | `com.sun.net.httpserver.HttpServer` |
| Cliente HTTP | `java.net.http.HttpClient` |
| Interface Web | HTML5, CSS3 e JavaScript |
| Componentes Web | Bootstrap 5.3.3 via CDN |
| Ícones Web | Bootstrap Icons 1.11.3 via CDN |
| Persistência | Arquivos CSV |
| Testes | Java, sem biblioteca externa de testes |

O Swing desenha os ícones da aplicação por meio da classe `IconeSistema.java`. A versão Web carrega Bootstrap e Bootstrap Icons por CDN; para carregar esses recursos, o navegador precisa ter acesso à internet.

O projeto não utiliza Maven ou Gradle. A compilação é feita diretamente com `javac`.

---

## Estrutura do projeto

Abra a pasta `AULA10`, que contém as pastas `src`, `frontend`, `dados`, `logs`, `docs` e `testes`.

```text
AULA10/
├── src/
│   ├── api/
│   │   └── Api.java
│   ├── auth/
│   │   └── AuthService.java
│   ├── client/
│   │   ├── ApiClient.java
│   │   └── AuthApiClient.java
│   ├── dao/
│   │   └── ProjetoCSV.java
│   ├── model/
│   │   └── Projeto.java
│   ├── service/
│   │   └── ProjetoService.java
│   ├── util/
│   │   ├── JsonUtil.java
│   │   └── LogService.java
│   ├── view/
│   │   ├── IconeSistema.java
│   │   ├── PainelGraficos.java
│   │   ├── TelaLogin.java
│   │   ├── TelaProjetos.java
│   │   └── TelaTesteApi.java
│   └── Main.java
├── frontend/
│   ├── login.html
│   ├── auth.css
│   ├── auth.js
│   ├── projetos.html
│   ├── projetos.css
│   └── projetos.js
├── dados/
│   ├── projetos.csv
│   └── usuarios.csv
├── logs/
│   ├── application.log
│   ├── access.log
│   ├── audit.log
│   ├── error.log
│   └── api.log
├── testes/
│   └── ProjetoServiceTestes.java
├── docs/
│   ├── TESTES_QUALIDADE.md
│   └── RELATORIO_QUALIDADE.md
└── README.md
```

### Responsabilidade dos principais pacotes

- `api`: inicialização do servidor e definição das rotas HTTP.
- `auth`: cadastro, autenticação e controle das sessões.
- `client`: comunicação das interfaces Java com a API.
- `dao`: leitura e gravação dos projetos no CSV.
- `model`: representação da entidade `Projeto`.
- `service`: regras de negócio e validações dos projetos.
- `util`: conversão JSON e gravação dos logs.
- `view`: telas Swing, navegação, ícones e gráficos.
- `frontend`: páginas, estilos e scripts da versão Web.
- `dados`: arquivos de persistência.
- `logs`: registros de execução e auditoria.
- `testes`: executor automatizado dos testes de validação.
- `docs`: documentação dos casos de teste e relatório de qualidade.

**Organização das telas:** no Swing, as cinco áreas ficam na classe `view/TelaProjetos.java` e são alternadas com `CardLayout`. Na Web, as cinco seções ficam em `frontend/projetos.html` e a navegação é controlada por `frontend/projetos.js`.

---

## Requisitos

- **JDK 17 ou superior** instalado e configurado no `PATH`.
- Navegador atualizado.
- Python 3 para iniciar um servidor HTTP local para a interface Web.
- Acesso à internet no navegador para carregar Bootstrap e Bootstrap Icons via CDN.

Para conferir as instalações:

```bash
java -version
javac -version
python --version
```

---

## Como executar no Windows

> **Importante:** execute os comandos a partir da pasta `AULA10`, e não da pasta externa do arquivo ZIP. A API utiliza caminhos relativos para acessar `dados/` e `logs/`.

### 1. Abrir a pasta do projeto

Extraia o ZIP e abra a pasta `AULA10` no terminal ou no PowerShell. Ela deve conter diretamente as pastas `src`, `frontend`, `dados` e `logs`.

### 2. Compilar os arquivos Java

No PowerShell:

```powershell
New-Item -ItemType Directory -Force out

$fontes = Get-ChildItem -Path src -Filter *.java -Recurse |
    ForEach-Object { $_.FullName }

javac -encoding UTF-8 -d out $fontes
```

Se a compilação terminar sem mensagens de erro, as classes serão geradas na pasta `out`.

### 3. Iniciar a API

No primeiro terminal:

```powershell
java -cp out api.Api
```

A mensagem esperada é semelhante a:

```text
Servidor iniciado em http://localhost:7070
```

**Mantenha esse terminal aberto** enquanto utilizar o Swing ou a Web.

### 4. Executar o Java Swing

Abra um segundo terminal, também na pasta `AULA10`:

```powershell
java -cp out view.TelaLogin
```

A tela de acesso permite entrar com uma conta existente ou criar uma nova. Depois da autenticação, o sistema abre o painel principal.

### 5. Executar a versão Web

Abra um terceiro terminal, na pasta `AULA10`:

```powershell
python -m http.server 5500 --directory frontend
```

No navegador, acesse:

[http://localhost:5500/login.html](http://localhost:5500/login.html)

Cadastre uma conta ou utilize uma conta existente. Uma conta criada no Swing também pode ser utilizada na Web, e vice-versa.

---

## Como executar no Linux ou macOS

Abra um terminal na pasta `AULA10`.

### 1. Compilar

```bash
mkdir -p out
find src -name "*.java" -print0 | xargs -0 javac -encoding UTF-8 -d out
```

### 2. Iniciar a API

```bash
java -cp out api.Api
```

### 3. Executar o Swing

Em outro terminal, na pasta `AULA10`:

```bash
java -cp out view.TelaLogin
```

### 4. Executar a Web

Em outro terminal, na pasta `AULA10`:

```bash
python3 -m http.server 5500 --directory frontend
```

Acesse `http://localhost:5500/login.html` no navegador.

---

## Como executar os testes

Os casos de teste estão documentados em `docs/TESTES_QUALIDADE.md`. O executor está em `testes/ProjetoServiceTestes.java` e não exige JUnit.

Depois de compilar o código principal, execute na pasta `AULA10`:

```powershell
javac -encoding UTF-8 -cp out -d out testes/ProjetoServiceTestes.java
java -cp out testes.ProjetoServiceTestes
```

O conjunto documenta **31 cenários** para ID, nome, descrição, categoria, status, prazo e data de criação. Os testes exercitam a camada `ProjetoService`; eles não substituem testes completos de interface gráfica ou testes de carga da API.

---

## Áreas do sistema

### Dashboard

Apresenta indicadores calculados com base nos projetos cadastrados:

- Total de projetos.
- Projetos concluídos.
- Projetos ativos (Planejado + Em Desenvolvimento).
- Projetos cancelados.
- Data atual e atividade recente.

### Projetos

Concentra as operações de cadastro, listagem, consulta, edição e exclusão. Permite pesquisar por nome, categoria ou status, ordenar a tabela e navegar pelas páginas. O formulário contém nome, descrição, categoria, status, data de criação e prazo.

### Categorias

Apresenta a quantidade de projetos nas categorias fixas `Software`, `Mobile`, `Web` e `Desktop`. A ação **Ver projetos** abre a listagem já filtrada pela categoria escolhida.

### Relatórios

Apresenta quatro gráficos:

1. **Status:** distribuição dos projetos por situação.
2. **Categorias:** comparação da quantidade de projetos por categoria.
3. **Evolução mensal:** quantidade de projetos criados ao longo dos últimos seis meses, usando `dataCriacao`.
4. **Prazos:** comparação entre projetos no prazo e atrasados.

A relação de projetos pode ser exportada em CSV. Projetos sem prazo informado não entram no cálculo do gráfico de cumprimento de prazos.

### Configurações

Disponibiliza informações da conta autenticada, alternância de tema claro/escuro, preferência de quantidade de projetos por página, teste de conexão com a API e informações sobre os arquivos de dados e logs. Também permite encerrar a sessão.

---

## Regras de validação

As regras centrais são implementadas em `src/service/ProjetoService.java` e aplicadas novamente pela API.

| Campo | Regra |
|---|---|
| ID | Inteiro positivo e único. Nas operações normais da interface, o ID é gerado pelo sistema. |
| Nome | Obrigatório, entre 3 e 100 caracteres. Espaços externos são removidos. |
| Descrição | Entre 10 e 500 caracteres. Espaços externos são removidos. |
| Categoria | Apenas `Software`, `Mobile`, `Web` ou `Desktop`. |
| Status | Apenas `Planejado`, `Em Desenvolvimento`, `Concluído` ou `Cancelado`. |
| Data de criação | Preenchida automaticamente no cadastro e preservada nas alterações. |
| Prazo | Opcional; quando informado, deve ser uma data válida e não pode ser anterior à data de criação. |

As datas são persistidas no formato ISO `AAAA-MM-DD`. A aplicação também aceita `DD/MM/AAAA` em entradas de data previstas para esse formato. Um prazo que já passou pode ser registrado quando não for anterior à data de criação; nesse caso, o relatório pode classificá-lo como atrasado.

---

## API HTTP

A API é executada localmente em `http://localhost:7070`.

### Autenticação

| Método | Rota | Descrição | Acesso |
|---|---|---|---|
| `POST` | `/api/auth/register` | Cadastrar usuário | Público |
| `POST` | `/api/auth/login` | Autenticar e gerar token | Público |
| `POST` | `/api/auth/logout` | Encerrar sessão | Autenticado |

### Projetos

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/api/projetos` | Listar projetos |
| `GET` | `/api/projetos/{id}` | Consultar projeto pelo ID |
| `POST` | `/api/projetos` | Cadastrar projeto |
| `PUT` | `/api/projetos/{id}` | Alterar projeto |
| `DELETE` | `/api/projetos/{id}` | Excluir projeto |

As rotas de projetos exigem o cabeçalho de autenticação:

```http
Authorization: Bearer <token>
```

A Web também utiliza `POST /api/logs/error` para encaminhar à API erros de validação identificados no formulário.

---

## Armazenamento dos dados

O sistema utiliza arquivos CSV, sem banco de dados externo.

### `dados/projetos.csv`

Armazena os projetos com os campos:

```text
id;nome;descricao;categoria;status;dataCriacao;prazo
```

### `dados/usuarios.csv`

Armazena os dados necessários à autenticação: e-mail, nome, salt e hash da senha. A senha original não é armazenada.

### Compatibilidade com CSV antigo

O sistema ainda consegue ler o formato anterior de projetos com cinco colunas (`id;nome;descricao;categoria;status`). Para registros antigos, a data de criação é preenchida com a data da migração/leitura e o prazo fica vazio. Na próxima gravação, o arquivo é salvo com as sete colunas atuais.

---

## Logs e auditoria

Os registros são gravados automaticamente na pasta `logs/`:

| Arquivo | Conteúdo |
|---|---|
| `application.log` | Inicialização, encerramento e eventos gerais da aplicação. |
| `access.log` | Cadastro, login, logout e tentativas de login negadas. |
| `audit.log` | Inclusão, alteração e exclusão de projetos, com identificação do projeto. |
| `error.log` | Erros de validação, leitura do CSV e falhas da aplicação/API. |
| `api.log` | Método HTTP, rota e código de resposta. |

Os arquivos são úteis para acompanhar o funcionamento e consultar operações realizadas no sistema. Evite apagar os logs enquanto estiver verificando uma ocorrência.

---

## Documentação complementar

- `docs/TESTES_QUALIDADE.md`: descrição dos 31 cenários de teste e instruções para executá-los.
- `docs/RELATORIO_QUALIDADE.md`: resumo do que foi testado, erros tratados, métricas do dashboard e finalidade dos logs.

---

## Observações e limitações

- A API precisa estar em execução antes de abrir o Swing ou utilizar a Web.
- Execute o servidor a partir da pasta `AULA10` para que os caminhos relativos de `dados/` e `logs/` sejam resolvidos corretamente.
- As sessões são mantidas em memória pela API. Ao reiniciar o servidor, os usuários precisam entrar novamente.
- Os projetos são compartilhados entre as contas autenticadas; esta versão não separa os projetos por proprietário.
- O armazenamento em CSV atende ao escopo acadêmico do projeto, mas não oferece os recursos de concorrência e administração de um banco de dados.
- A API está configurada para uso local na porta `7070`. O servidor Web de desenvolvimento usa a porta `5500`.
- Para uso fora de ambiente local ou acadêmico, seria necessário revisar aspectos de segurança, configuração, persistência e implantação.

---

## Autor / equipe

Projeto acadêmico desenvolvido para praticar Java, programação orientada a objetos, interfaces gráficas, APIs HTTP, HTML, CSS, JavaScript, validação de dados, testes e auditoria.
