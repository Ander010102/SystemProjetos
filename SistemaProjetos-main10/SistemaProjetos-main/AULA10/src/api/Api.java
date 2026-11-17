package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import auth.AuthService;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import model.Projeto;
import service.ProjetoService;
import util.JsonUtil;
import util.LogService;

public class Api {

    private static ProjetoService service;
    private static AuthService authService;

    public static void main(String[] args) throws Exception {

        service = new ProjetoService();
        try {
            service.carregar();
        } catch (Exception e) {
            LogService.erro("Falha ao carregar CSV de projetos: " + e.getMessage());
            throw e;
        }
        authService = new AuthService();
        LogService.aplicacao("INFO", "Sistema iniciado");

        HttpServer server = HttpServer.create(
                new InetSocketAddress(7070),
                0);

        // GET /
        server.createContext("/", exchange -> {

            try {

                configurarCors(exchange);

                if (responderOptions(exchange)) {
                    return;
                }

                if (!exchange.getRequestURI().getPath().equals("/")) {

                    enviarJson(
                            exchange,
                            404,
                            JsonUtil.erro("Rota não encontrada"));

                    return;
                }

                enviar(
                        exchange,
                        200,
                        "text/plain",
                        "API Sistema de Projetos");

            } finally {

                exchange.close();
            }
        });

        // Cadastro, login e logout compartilhados entre Swing e Web.
        server.createContext("/api/auth", exchange -> {
            try {
                configurarCors(exchange);
                if (responderOptions(exchange)) return;
                tratarAutenticacao(exchange);
            } catch (IllegalArgumentException e) {
                LogService.erro("Validação de autenticação: " + e.getMessage());
                enviarJson(exchange, 400, JsonUtil.erro(e.getMessage()));
            } catch (IllegalStateException e) {
                LogService.erro("Conflito de autenticação: " + e.getMessage());
                enviarJson(exchange, 409, JsonUtil.erro(e.getMessage()));
            } catch (SecurityException e) {
                LogService.acesso("tentativa inválida", "LOGIN_NEGADO");
                enviarJson(exchange, 401, JsonUtil.erro(e.getMessage()));
            } catch (Exception e) {
                LogService.aplicacao("ERROR", "Erro de autenticação: " + e.getMessage());
                LogService.erro("Erro de autenticação: " + e.getMessage());
                e.printStackTrace();
                enviarJson(exchange, 500, JsonUtil.erro("Erro interno do servidor"));
            } finally {
                LogService.api(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), exchange.getResponseCode());
                exchange.close();
            }
        });

        // Recebe falhas de validação identificadas na interface Web.
        server.createContext("/api/logs/error", exchange -> {
            try {
                configurarCors(exchange);
                if (responderOptions(exchange)) return;
                String token = obterToken(exchange);
                if (authService.buscarSessao(token) == null) {
                    enviarJson(exchange, 401, JsonUtil.erro("Acesso não autorizado."));
                    return;
                }
                if (!"POST".equals(exchange.getRequestMethod())) {
                    enviarJson(exchange, 405, JsonUtil.erro("Método não permitido."));
                    return;
                }
                String mensagem = JsonUtil.extrairCampo(lerCorpo(exchange), "mensagem");
                LogService.erro("Validação na interface Web: " + (mensagem == null ? "sem detalhes" : mensagem));
                exchange.sendResponseHeaders(204, -1);
            } catch (Exception e) {
                LogService.erro("Não foi possível registrar erro enviado pela Web: " + e.getMessage());
                enviarJson(exchange, 500, JsonUtil.erro("Não foi possível registrar o erro."));
            } finally {
                LogService.api(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), exchange.getResponseCode());
                exchange.close();
            }
        });

        // CRUD /api/projetos
        server.createContext("/api/projetos", exchange -> {

            try {

                configurarCors(exchange);

                if (responderOptions(exchange)) {
                    return;
                }

                String token = obterToken(exchange);
                if (authService.buscarSessao(token) == null) {
                    enviarJson(exchange, 401, JsonUtil.erro("Acesso não autorizado. Faça login para continuar."));
                    return;
                }

                tratarProjetos(exchange);

            } catch (IllegalArgumentException e) {
                LogService.erro("Campo inválido na API: " + e.getMessage());
                enviarJson(exchange, 400, JsonUtil.erro(e.getMessage()));
            } catch (Exception e) {
                LogService.aplicacao("ERROR", "Erro geral na API: " + e.getMessage());
                LogService.erro("Erro geral na API: " + e.getMessage());
                e.printStackTrace();
                enviarJson(exchange, 500, JsonUtil.erro("Erro interno do servidor"));
            } finally {
                LogService.api(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        exchange.getResponseCode());
                exchange.close();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                LogService.aplicacao("INFO", "Sistema encerrado")));
        server.start();

        System.out.println("Servidor iniciado em http://localhost:7070");
    }

    private static void tratarAutenticacao(HttpExchange exchange) throws Exception {
        String caminho = exchange.getRequestURI().getPath();
        String metodo = exchange.getRequestMethod();
        String corpo = lerCorpo(exchange);

        if ("/api/auth/register".equals(caminho) && "POST".equals(metodo)) {
            String nome = JsonUtil.extrairCampo(corpo, "nome");
            String email = JsonUtil.extrairCampo(corpo, "email");
            String senha = JsonUtil.extrairCampo(corpo, "senha");
            AuthService.Sessao sessao = authService.cadastrar(nome, email, senha);
            LogService.acesso(sessao.email(), "CADASTRO");
            enviarJson(exchange, 201, sessaoParaJson(sessao));
            return;
        }

        if ("/api/auth/login".equals(caminho) && "POST".equals(metodo)) {
            String email = JsonUtil.extrairCampo(corpo, "email");
            String senha = JsonUtil.extrairCampo(corpo, "senha");
            AuthService.Sessao sessao = authService.entrar(email, senha);
            LogService.acesso(sessao.email(), "LOGIN");
            enviarJson(exchange, 200, sessaoParaJson(sessao));
            return;
        }

        if ("/api/auth/logout".equals(caminho) && "POST".equals(metodo)) {
            String token = obterToken(exchange);
            if (authService.buscarSessao(token) == null) {
                enviarJson(exchange, 401, JsonUtil.erro("Sessão não encontrada ou expirada."));
                return;
            }
            AuthService.Sessao sessao = authService.buscarSessao(token);
            authService.sair(token);
            LogService.acesso(sessao == null ? "desconhecido" : sessao.email(), "LOGOUT");
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        enviarJson(exchange, 404, JsonUtil.erro("Rota de autenticação não encontrada."));
    }

    private static String sessaoParaJson(AuthService.Sessao sessao) {
        return "{"
                + "\"token\":" + JsonUtil.textoJson(sessao.token()) + ","
                + "\"nome\":" + JsonUtil.textoJson(sessao.nome()) + ","
                + "\"email\":" + JsonUtil.textoJson(sessao.email())
                + "}";
    }

    private static String obterToken(HttpExchange exchange) {
        String autorizacao = exchange.getRequestHeaders().getFirst("Authorization");
        if (autorizacao == null || !autorizacao.startsWith("Bearer ")) return null;
        return autorizacao.substring(7).trim();
    }

    private static void tratarProjetos(HttpExchange exchange)
            throws Exception {

        String metodo = exchange.getRequestMethod();

        String caminho = exchange.getRequestURI().getPath();

        if (!caminho.equals("/api/projetos")
                && !caminho.startsWith("/api/projetos/")) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Rota não encontrada"));

            return;
        }

        String[] partes = caminho.split("/");

        if (partes.length > 4) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Rota não encontrada"));

            return;
        }

        boolean temId = partes.length == 4;
        int id = 0;

        if (temId) {

            try {

                id = ProjetoService.parseId(partes[3]);

            } catch (IllegalArgumentException e) {
                LogService.erro("ID inválido recebido na API: " + partes[3]);
                enviarJson(exchange, 400, JsonUtil.erro(e.getMessage()));

                return;
            }
        }

        switch (metodo) {

            case "GET" ->
                get(exchange, temId, id);

            case "POST" ->
                post(exchange, temId);

            case "PUT" ->
                put(exchange, temId, id);

            case "DELETE" ->
                delete(exchange, temId, id);

            default ->
                exchange.sendResponseHeaders(405, -1);
        }
    }

    // GET /api/projetos
    // GET /api/projetos/{id}
    private static void get(
            HttpExchange exchange,
            boolean temId,
            int id) throws IOException {

        if (!temId) {

            enviarJson(
                    exchange,
                    200,
                    JsonUtil.listaParaJson(service.listar()));

            return;
        }

        Projeto projeto = service.buscarPorId(id);

        if (projeto == null) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Projeto não encontrado"));

            return;
        }

        enviarJson(
                exchange,
                200,
                JsonUtil.paraJson(projeto));
    }

    // POST /api/projetos
    private static void post(
            HttpExchange exchange,
            boolean temId) throws Exception {

        if (temId) {

            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String corpo = lerCorpo(exchange);
        Projeto projeto = JsonUtil.jsonParaProjeto(corpo);
        // A data de criação é definida pelo servidor no cadastro.
        projeto.setDataCriacao(java.time.LocalDate.now().toString());
        ProjetoService.validarProjeto(projeto);

        // Sem ID informado, o servidor gera o próximo. Se informado, deve ser positivo e único.
        if (JsonUtil.temCampo(corpo, "id")) {
            ProjetoService.validarId(projeto.getId());
            ProjetoService.validarIdUnico(projeto.getId(), service.listar(), null);
        } else {
            projeto.setId(proximoId());
        }

        service.adicionar(projeto);
        service.salvar();
        LogService.auditoria("PROJETO " + projeto.getId() + " CRIADO");

        enviarJson(
                exchange,
                201,
                JsonUtil.paraJson(projeto));
    }

    // PUT /api/projetos/{id}
    private static void put(
            HttpExchange exchange,
            boolean temId,
            int id) throws Exception {

        if (!temId) {

            enviarJson(
                    exchange,
                    400,
                    JsonUtil.erro("Informe o ID na URL"));

            return;
        }

        String corpo = lerCorpo(exchange);
        Projeto projeto = JsonUtil.jsonParaProjeto(corpo);

        // O ID utilizado é o da URL; a data de criação original não pode ser alterada.
        Projeto atual = service.buscarPorId(id);
        if (atual == null) {
            enviarJson(exchange, 404, JsonUtil.erro("Projeto não encontrado"));
            return;
        }
        projeto.setId(id);
        projeto.setDataCriacao(atual.getDataCriacao());
        ProjetoService.validarProjeto(projeto);

        if (!service.alterar(projeto)) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Projeto não encontrado"));

            return;
        }

        service.salvar();
        LogService.auditoria("PROJETO " + id + " ALTERADO");

        enviarJson(exchange, 200, JsonUtil.paraJson(service.buscarPorId(id)));
    }

    // DELETE /api/projetos/{id}
    private static void delete(
            HttpExchange exchange,
            boolean temId,
            int id) throws Exception {

        if (!temId) {

            enviarJson(
                    exchange,
                    400,
                    JsonUtil.erro("Informe o ID na URL"));

            return;
        }

        if (!service.removerPorId(id)) {

            enviarJson(
                    exchange,
                    404,
                    JsonUtil.erro("Projeto não encontrado"));

            return;
        }

        service.salvar();
        LogService.auditoria("PROJETO " + id + " EXCLUIDO");

        exchange.sendResponseHeaders(204, -1);
    }

    // ---------- CORS e OPTIONS ----------

    private static void configurarCors(HttpExchange exchange) {

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Origin",
                "*");

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS");

        exchange.getResponseHeaders().set(
                "Access-Control-Allow-Headers",
                "Content-Type, Authorization");
    }

    private static boolean responderOptions(HttpExchange exchange)
            throws IOException {

        if (exchange.getRequestMethod().equals("OPTIONS")) {

            exchange.sendResponseHeaders(204, -1);

            return true;
        }

        return false;
    }

    // ---------- Auxiliares ----------

    private static int proximoId() {

        int proximo = 1;

        for (Projeto projeto : service.listar()) {

            if (projeto.getId() >= proximo) {
                proximo = projeto.getId() + 1;
            }
        }

        return proximo;
    }

    private static String lerCorpo(HttpExchange exchange)
            throws IOException {

        return new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8);
    }

    private static void enviarJson(
            HttpExchange exchange,
            int status,
            String json) throws IOException {

        enviar(
                exchange,
                status,
                "application/json",
                json);
    }

    private static void enviar(
            HttpExchange exchange,
            int status,
            String tipo,
            String corpo) throws IOException {

        byte[] bytes = corpo.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
                "Content-Type",
                tipo + "; charset=UTF-8");

        exchange.sendResponseHeaders(status, bytes.length);

        exchange.getResponseBody().write(bytes);
    }
}