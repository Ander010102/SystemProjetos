package client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import util.JsonUtil;

/** Cliente HTTP para cadastro, login e encerramento da sessão. */
public class AuthApiClient {

    private static final String BASE_URL = "http://localhost:7070/api/auth";
    private final HttpClient client = HttpClient.newHttpClient();

    public Sessao entrar(String email, String senha) throws Exception {
        return enviarCredenciais("/login", null, email, senha);
    }

    public Sessao cadastrar(String nome, String email, String senha) throws Exception {
        return enviarCredenciais("/register", nome, email, senha);
    }

    private Sessao enviarCredenciais(String rota, String nome, String email, String senha) throws Exception {
        StringBuilder json = new StringBuilder("{");
        if (nome != null) json.append("\"nome\":").append(JsonUtil.textoJson(nome)).append(',');
        json.append("\"email\":").append(JsonUtil.textoJson(email)).append(',')
                .append("\"senha\":").append(JsonUtil.textoJson(senha)).append('}');

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + rota))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int esperado = "/register".equals(rota) ? 201 : 200;
        if (response.statusCode() != esperado) {
            throw new Exception(mensagemErro(response.body(), response.statusCode()));
        }

        String token = JsonUtil.extrairCampo(response.body(), "token");
        String nomeResposta = JsonUtil.extrairCampo(response.body(), "nome");
        String emailResposta = JsonUtil.extrairCampo(response.body(), "email");
        if (token == null || token.isBlank()) {
            throw new Exception("A API não retornou uma sessão válida.");
        }
        return new Sessao(token, nomeResposta, emailResposta);
    }

    public void sair(String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/logout"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 204 && response.statusCode() != 401) {
            throw new Exception(mensagemErro(response.body(), response.statusCode()));
        }
    }

    private String mensagemErro(String json, int status) {
        String mensagem = JsonUtil.extrairCampo(json, "erro");
        return mensagem == null ? "Não foi possível autenticar. Código HTTP " + status + "." : mensagem;
    }

    public record Sessao(String token, String nome, String email) { }
}
