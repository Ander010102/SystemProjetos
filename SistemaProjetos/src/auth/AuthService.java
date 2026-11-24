package auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import util.LogService;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Gerencia cadastro, autenticação e sessões compartilhadas pelo Swing e pela Web. */
public class AuthService {

    private static final int ITERACOES = 120_000;
    private static final int TAMANHO_HASH = 256;
    private static final int TAMANHO_SALT = 16;
    private static final Pattern EMAIL_VALIDO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Path arquivoUsuarios = Path.of("dados", "usuarios.csv");
    private final Map<String, UsuarioArmazenado> usuarios = new HashMap<>();
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();

    public AuthService() throws IOException {
        carregarUsuarios();
    }

    public synchronized Sessao cadastrar(String nome, String email, String senha) throws IOException {
        String nomeNormalizado = nome == null ? "" : nome.trim();
        String emailNormalizado = normalizarEmail(email);

        validarCadastro(nomeNormalizado, emailNormalizado, senha);

        if (usuarios.containsKey(emailNormalizado)) {
            throw new IllegalStateException("Já existe uma conta cadastrada com esse e-mail.");
        }

        byte[] salt = new byte[TAMANHO_SALT];
        RANDOM.nextBytes(salt);
        byte[] hash = gerarHash(senha.toCharArray(), salt);

        UsuarioArmazenado usuario = new UsuarioArmazenado(
                nomeNormalizado, emailNormalizado, salt, hash);
        usuarios.put(emailNormalizado, usuario);
        salvarUsuarios();

        return criarSessao(usuario);
    }

    public Sessao entrar(String email, String senha) {
        String emailNormalizado = normalizarEmail(email);
        if (senha == null || senha.isEmpty()) {
            throw new IllegalArgumentException("Informe o e-mail e a senha.");
        }

        UsuarioArmazenado usuario;
        synchronized (this) {
            usuario = usuarios.get(emailNormalizado);
        }

        if (usuario == null) {
            throw new SecurityException("E-mail ou senha incorretos.");
        }

        byte[] hashInformado = gerarHash(senha.toCharArray(), usuario.salt);
        if (!MessageDigest.isEqual(usuario.hash, hashInformado)) {
            throw new SecurityException("E-mail ou senha incorretos.");
        }

        return criarSessao(usuario);
    }

    public Sessao buscarSessao(String token) {
        if (token == null || token.isBlank()) return null;
        return sessoes.get(token);
    }

    public void sair(String token) {
        if (token != null) sessoes.remove(token);
    }

    private Sessao criarSessao(UsuarioArmazenado usuario) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Sessao sessao = new Sessao(token, usuario.nome, usuario.email);
        sessoes.put(token, sessao);
        return sessao;
    }

    private void validarCadastro(String nome, String email, String senha) {
        if (nome.length() < 2) {
            throw new IllegalArgumentException("Informe seu nome (pelo menos 2 caracteres).");
        }
        if (!EMAIL_VALIDO.matcher(email).matches()) {
            throw new IllegalArgumentException("Informe um endereço de e-mail válido.");
        }
        if (senha == null || senha.length() < 8) {
            throw new IllegalArgumentException("A senha deve possuir pelo menos 8 caracteres.");
        }
    }

    private String normalizarEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private byte[] gerarHash(char[] senha, byte[] salt) {
        PBEKeySpec especificacao = new PBEKeySpec(senha, salt, ITERACOES, TAMANHO_HASH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(especificacao).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível processar a senha.", e);
        } finally {
            especificacao.clearPassword();
            java.util.Arrays.fill(senha, '\0');
        }
    }

    private synchronized void carregarUsuarios() throws IOException {
        Files.createDirectories(arquivoUsuarios.getParent());
        if (!Files.exists(arquivoUsuarios)) {
            Files.writeString(arquivoUsuarios, "email|nome|salt|hash" + System.lineSeparator(), StandardCharsets.UTF_8);
            return;
        }

        for (String linha : Files.readAllLines(arquivoUsuarios, StandardCharsets.UTF_8)) {
            if (linha.isBlank() || linha.startsWith("email|")) continue;
            String[] partes = linha.split("\\|", -1);
            if (partes.length != 4) continue;
            try {
                String email = decodificar(partes[0]);
                String nome = decodificar(partes[1]);
                byte[] salt = Base64.getDecoder().decode(partes[2]);
                byte[] hash = Base64.getDecoder().decode(partes[3]);
                usuarios.put(email, new UsuarioArmazenado(nome, email, salt, hash));
            } catch (IllegalArgumentException ex) {
                LogService.erro("Registro de usuário ignorado: formato inválido no usuarios.csv.");
            }
        }
    }

    private synchronized void salvarUsuarios() throws IOException {
        StringBuilder conteudo = new StringBuilder("email|nome|salt|hash");
        for (UsuarioArmazenado usuario : usuarios.values()) {
            conteudo.append(System.lineSeparator())
                    .append(codificar(usuario.email)).append('|')
                    .append(codificar(usuario.nome)).append('|')
                    .append(Base64.getEncoder().encodeToString(usuario.salt)).append('|')
                    .append(Base64.getEncoder().encodeToString(usuario.hash));
        }
        Files.writeString(arquivoUsuarios, conteudo.toString(), StandardCharsets.UTF_8);
    }

    private String codificar(String valor) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }

    private String decodificar(String valor) {
        return new String(Base64.getUrlDecoder().decode(valor), StandardCharsets.UTF_8);
    }

    private static class UsuarioArmazenado {
        private final String nome;
        private final String email;
        private final byte[] salt;
        private final byte[] hash;

        private UsuarioArmazenado(String nome, String email, byte[] salt, byte[] hash) {
            this.nome = nome;
            this.email = email;
            this.salt = salt;
            this.hash = hash;
        }
    }

    public record Sessao(String token, String nome, String email) { }
}
