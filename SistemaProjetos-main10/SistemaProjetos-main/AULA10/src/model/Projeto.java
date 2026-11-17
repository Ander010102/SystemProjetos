package model;

/** Modelo de domínio de um projeto. As datas são persistidas em ISO (yyyy-MM-dd). */
public class Projeto {
    private int id;
    private String nome;
    private String descricao;
    private String categoria;
    private String status;
    private String dataCriacao;
    private String prazo;

    public Projeto() { }

    public Projeto(String nome, String descricao, String categoria, String status) {
        this(0, nome, descricao, categoria, status, null, null);
    }

    public Projeto(int id, String nome, String descricao, String categoria, String status) {
        this(id, nome, descricao, categoria, status, null, null);
    }

    public Projeto(int id, String nome, String descricao, String categoria, String status,
                   String dataCriacao, String prazo) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.status = status;
        this.dataCriacao = dataCriacao;
        this.prazo = prazo;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getDataCriacao() { return dataCriacao; }
    public void setDataCriacao(String dataCriacao) { this.dataCriacao = dataCriacao; }
    public String getPrazo() { return prazo; }
    public void setPrazo(String prazo) { this.prazo = prazo; }

    public void exibirDados() {
        System.out.println("ID: " + id);
        System.out.println("Nome: " + nome);
        System.out.println("Descrição: " + descricao);
        System.out.println("Categoria: " + categoria);
        System.out.println("Status: " + status);
        System.out.println("Data de criação: " + dataCriacao);
        System.out.println("Prazo: " + prazo);
    }
}
