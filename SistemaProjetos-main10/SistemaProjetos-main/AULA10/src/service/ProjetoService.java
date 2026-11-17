package service;

import dao.ProjetoCSV;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import model.Projeto;
import util.LogService;

/** Regras de negócio e validações compartilhadas pela API, Swing e Web. */
public class ProjetoService {
    public static final List<String> CATEGORIAS_VALIDAS = List.of("Software", "Mobile", "Web", "Desktop");
    public static final List<String> STATUS_VALIDOS = List.of("Planejado", "Em Desenvolvimento", "Concluído", "Cancelado");

    private final ProjetoCSV dao;
    private List<Projeto> projetos;

    public ProjetoService() {
        projetos = new ArrayList<>();
        dao = new ProjetoCSV();
    }

    public void carregar() throws Exception { projetos = dao.listar(); }
    public void salvar() throws Exception { dao.salvar(projetos); }

    public static void validarId(int id) {
        if (id <= 0) throw new IllegalArgumentException("ID deve ser um número inteiro positivo.");
    }

    public static int parseId(String valor) {
        if (valor == null || !valor.trim().matches("\\d+")) {
            throw new IllegalArgumentException("ID deve ser numérico.");
        }
        try {
            int id = Integer.parseInt(valor.trim());
            validarId(id);
            return id;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID fora do limite permitido.", e);
        }
    }

    public static void validarIdUnico(int id, List<Projeto> lista, Integer idIgnorado) {
        validarId(id);
        boolean repetido = lista.stream().anyMatch(p -> p.getId() == id && (idIgnorado == null || p.getId() != idIgnorado));
        if (repetido) throw new IllegalArgumentException("Já existe um projeto com esse ID.");
    }

    /** Valida e normaliza todos os campos do projeto. */
    public static void validarProjeto(Projeto projeto) {
        if (projeto == null) throw new IllegalArgumentException("Dados do projeto não informados.");

        String nome = limpar(projeto.getNome());
        if (nome.length() < 3 || nome.length() > 100) {
            throw new IllegalArgumentException("Nome obrigatório: informe entre 3 e 100 caracteres.");
        }

        String descricao = limpar(projeto.getDescricao());
        if (descricao.length() < 10 || descricao.length() > 500) {
            throw new IllegalArgumentException("A descrição deve ter entre 10 e 500 caracteres.");
        }

        String categoria = normalizarCatalogo(projeto.getCategoria(), CATEGORIAS_VALIDAS, "Categoria");
        String status = normalizarCatalogo(projeto.getStatus(), STATUS_VALIDOS, "Status");

        String dataCriacao = limpar(projeto.getDataCriacao());
        if (dataCriacao.isEmpty()) dataCriacao = LocalDate.now().toString();
        LocalDate dataCriacaoParsed = parseData(dataCriacao, "Data de criação");

        String prazo = limpar(projeto.getPrazo());
        if (!prazo.isEmpty()) {
            LocalDate prazoParsed = parseData(prazo, "Prazo");
            if (prazoParsed.isBefore(dataCriacaoParsed)) {
                throw new IllegalArgumentException("O prazo não pode ser anterior à data de criação.");
            }
            prazo = prazoParsed.toString();
        }

        projeto.setNome(nome);
        projeto.setDescricao(descricao);
        projeto.setCategoria(categoria);
        projeto.setStatus(status);
        projeto.setDataCriacao(dataCriacaoParsed.toString());
        projeto.setPrazo(prazo);
    }

    private static String limpar(String valor) { return valor == null ? "" : valor.trim(); }

    private static String normalizarCatalogo(String valor, List<String> catalogo, String campo) {
        String informado = limpar(valor);
        for (String permitido : catalogo) {
            if (permitido.equalsIgnoreCase(informado)) return permitido;
        }
        throw new IllegalArgumentException(campo + " inválido. Valores permitidos: " + String.join(", ", catalogo) + ".");
    }

    /** Aceita ISO (yyyy-MM-dd) e brasileiro (dd/MM/yyyy), normalizando para ISO. */
    public static LocalDate parseData(String valor, String nomeCampo) {
        String data = limpar(valor);
        if (data.isEmpty()) throw new IllegalArgumentException(nomeCampo + " inválida.");
        try {
            return LocalDate.parse(data, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException ignored) {
            try {
                DateTimeFormatter br = DateTimeFormatter.ofPattern("dd/MM/uuuu", new Locale("pt", "BR"));
                return LocalDate.parse(data, br);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(nomeCampo + " inválida. Use AAAA-MM-DD ou DD/MM/AAAA.");
            }
        }
    }

    public boolean adicionar(Projeto projeto) {
        validarProjeto(projeto);
        validarIdUnico(projeto.getId(), projetos, null);
        projetos.add(projeto);
        return true;
    }

    public boolean alterar(Projeto atualizado) {
        validarProjeto(atualizado);
        Projeto atual = buscarPorId(atualizado.getId());
        if (atual == null) return false;
        atual.setNome(atualizado.getNome());
        atual.setDescricao(atualizado.getDescricao());
        atual.setCategoria(atualizado.getCategoria());
        atual.setStatus(atualizado.getStatus());
        atual.setPrazo(atualizado.getPrazo());
        // A data de criação é imutável depois do cadastro.
        return true;
    }

    public List<Projeto> listar() { return projetos; }

    public Projeto buscarPorId(int id) {
        for (Projeto projeto : projetos) if (projeto.getId() == id) return projeto;
        return null;
    }

    public List<Projeto> buscarPorCategoria(String categoria) {
        List<Projeto> resultado = new ArrayList<>();
        for (Projeto p : projetos) if (p.getCategoria() != null && p.getCategoria().equalsIgnoreCase(categoria)) resultado.add(p);
        return resultado;
    }

    public List<Projeto> buscarPorStatus(String status) {
        List<Projeto> resultado = new ArrayList<>();
        for (Projeto p : projetos) if (p.getStatus() != null && p.getStatus().equalsIgnoreCase(status)) resultado.add(p);
        return resultado;
    }

    public boolean removerPorId(int id) {
        Projeto projeto = buscarPorId(id);
        return projeto != null && projetos.remove(projeto);
    }
}
