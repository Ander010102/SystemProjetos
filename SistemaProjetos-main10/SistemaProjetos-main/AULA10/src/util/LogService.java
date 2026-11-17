package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Escrita simples e centralizada dos cinco arquivos de log do projeto. */
public final class LogService {
    private static final Path PASTA = Path.of("logs");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LogService() { }

    private static synchronized void escrever(String arquivo, String nivel, String mensagem) {
        try {
            Files.createDirectories(PASTA);
            String linha = LocalDateTime.now().format(FORMATO) + " " + nivel + " " + limpar(mensagem) + System.lineSeparator();
            Files.writeString(PASTA.resolve(arquivo), linha, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Não foi possível gravar logs/" + arquivo + ": " + e.getMessage());
        }
    }

    private static String limpar(String valor) { return valor == null ? "-" : valor.replace('\n', ' ').replace('\r', ' '); }
    public static void aplicacao(String nivel, String mensagem) { escrever("application.log", nivel, mensagem); }
    public static void acesso(String usuario, String acao) { escrever("access.log", "ACCESS", "USUARIO=" + limpar(usuario) + " ACAO=" + limpar(acao)); }
    public static void auditoria(String mensagem) { escrever("audit.log", "AUDIT", mensagem); }
    public static void erro(String mensagem) { escrever("error.log", "ERROR", mensagem); }
    public static void api(String metodo, String rota, int status) { escrever("api.log", "API", metodo + " " + rota + " STATUS=" + status); }
}
