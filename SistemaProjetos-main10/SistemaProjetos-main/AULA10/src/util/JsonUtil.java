package util;

import model.Projeto;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conversor JSON simples, sem bibliotecas externas. */
public class JsonUtil {

    // ---------- Java -> JSON ----------

    public static String paraJson(Projeto p) {
        return "{"
            + "\"id\":" + p.getId() + ","
            + "\"nome\":" + textoJson(p.getNome()) + ","
            + "\"descricao\":" + textoJson(p.getDescricao()) + ","
            + "\"categoria\":" + textoJson(p.getCategoria()) + ","
            + "\"status\":" + textoJson(p.getStatus()) + ","
            + "\"dataCriacao\":" + textoJson(p.getDataCriacao()) + ","
            + "\"prazo\":" + textoJson(p.getPrazo())
            + "}";
    }

    /** JSON de cadastro sem ID: a API gera o próximo identificador. */
    public static String paraJsonSemId(Projeto p) {
        return "{" 
            + "\"nome\":" + textoJson(p.getNome()) + ","
            + "\"descricao\":" + textoJson(p.getDescricao()) + ","
            + "\"categoria\":" + textoJson(p.getCategoria()) + ","
            + "\"status\":" + textoJson(p.getStatus()) + ","
            + "\"dataCriacao\":" + textoJson(p.getDataCriacao()) + ","
            + "\"prazo\":" + textoJson(p.getPrazo())
            + "}";
    }

    public static String listaParaJson(List<Projeto> lista) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < lista.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(paraJson(lista.get(i)));
        }
        return sb.append("]").toString();
    }

    public static String erro(String mensagem) {
        return "{\"erro\":" + textoJson(mensagem) + "}";
    }

    public static String textoJson(String s) {
        if (s == null) return "null";
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default   -> sb.append(c);
            }
        }
        return sb.append("\"").toString();
    }

    // ---------- JSON -> Java ----------

    public static Projeto jsonParaProjeto(String json) {
        if (json == null || !json.trim().startsWith("{")) {
            throw new IllegalArgumentException("JSON inválido");
        }

        Projeto p = new Projeto();

        String id = valor(json, "id");
        if (id != null) {
            try {
                p.setId(Integer.parseInt(id.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("id inválido");
            }
        }

        p.setNome(valor(json, "nome"));
        p.setDescricao(valor(json, "descricao"));
        p.setCategoria(valor(json, "categoria"));
        p.setStatus(valor(json, "status"));
        p.setDataCriacao(valor(json, "dataCriacao"));
        p.setPrazo(valor(json, "prazo"));
        return p;
    }

    public static boolean temCampo(String json, String campo) {
        return Pattern.compile("\"" + Pattern.quote(campo) + "\"\\s*:").matcher(json).find();
    }

    /** Extrai um campo textual ou numérico de um objeto JSON simples. */
    public static String extrairCampo(String json, String campo) {
        return valor(json, campo);
    }

    /** Extrai o valor de um campo (texto entre aspas ou número). */
    private static String valor(String json, String campo) {
        Pattern pattern = Pattern.compile(
            "\"" + campo + "\"\\s*:\\s*(\"((?:\\\\.|[^\"\\\\])*)\"|-?\\d+|null)"
        );
        Matcher m = pattern.matcher(json);

        if (!m.find()) return null;
        if (m.group(2) != null) return desescapar(m.group(2));

        String bruto = m.group(1);
        return bruto.equals("null") ? null : bruto;
    }

    private static String desescapar(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 4 < s.length()) {
                            sb.append((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
                            i += 4;
                        }
                    }
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
