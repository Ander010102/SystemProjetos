package testes;

import java.time.LocalDate;
import java.util.List;
import model.Projeto;
import service.ProjetoService;

/** Executa os casos essenciais sem dependência de JUnit. */
public class ProjetoServiceTestes {
    private static int aprovados = 0;
    private static int falhas = 0;

    public static void main(String[] args) {
        testar("ID positivo", () -> ProjetoService.validarId(1));
        testarErro("ID zero", () -> ProjetoService.validarId(0));
        testarErro("ID negativo", () -> ProjetoService.validarId(-1));
        testarErro("ID não numérico", () -> ProjetoService.parseId("ABC"));
        testarErro("ID duplicado", () -> ProjetoService.validarIdUnico(1, List.of(new Projeto(1, "Projeto", "Descrição válida de teste", "Software", "Planejado")), null));

        testar("Nome com espaços externos normalizado", () -> {
            Projeto p = valido(); p.setNome("  Portal do Cliente  "); ProjetoService.validarProjeto(p);
            exigir("Portal do Cliente".equals(p.getNome()), "Nome não foi normalizado.");
        });
        testarErro("Nome vazio", () -> { Projeto p = valido(); p.setNome(""); ProjetoService.validarProjeto(p); });
        testarErro("Nome com um caractere", () -> { Projeto p = valido(); p.setNome("A"); ProjetoService.validarProjeto(p); });
        testarErro("Nome somente espaços", () -> { Projeto p = valido(); p.setNome("   "); ProjetoService.validarProjeto(p); });
        testarErro("Nome acima de 100 caracteres", () -> { Projeto p = valido(); p.setNome("N".repeat(101)); ProjetoService.validarProjeto(p); });

        testar("Descrição com 10 caracteres", () -> { Projeto p = valido(); p.setDescricao("1234567890"); ProjetoService.validarProjeto(p); });
        testarErro("Descrição abaixo de 10 caracteres", () -> { Projeto p = valido(); p.setDescricao("123456789"); ProjetoService.validarProjeto(p); });
        testarErro("Descrição acima de 500 caracteres", () -> { Projeto p = valido(); p.setDescricao("D".repeat(501)); ProjetoService.validarProjeto(p); });
        testar("Descrição com caracteres especiais", () -> { Projeto p = valido(); p.setDescricao("Tela: gestão, 'projetos' & dados!"); ProjetoService.validarProjeto(p); });

        testar("Categoria Software", () -> { Projeto p = valido(); p.setCategoria("Software"); ProjetoService.validarProjeto(p); });
        testar("Categoria Mobile", () -> { Projeto p = valido(); p.setCategoria("Mobile"); ProjetoService.validarProjeto(p); });
        testar("Categoria Web", () -> { Projeto p = valido(); p.setCategoria("Web"); ProjetoService.validarProjeto(p); });
        testar("Categoria Desktop", () -> { Projeto p = valido(); p.setCategoria("Desktop"); ProjetoService.validarProjeto(p); });
        testarErro("Categoria vazia", () -> { Projeto p = valido(); p.setCategoria(""); ProjetoService.validarProjeto(p); });
        testarErro("Categoria fora do catálogo", () -> { Projeto p = valido(); p.setCategoria("Dados"); ProjetoService.validarProjeto(p); });

        testar("Status Planejado", () -> { Projeto p = valido(); p.setStatus("Planejado"); ProjetoService.validarProjeto(p); });
        testar("Status Em Desenvolvimento", () -> { Projeto p = valido(); p.setStatus("Em Desenvolvimento"); ProjetoService.validarProjeto(p); });
        testar("Status Concluído", () -> { Projeto p = valido(); p.setStatus("Concluído"); ProjetoService.validarProjeto(p); });
        testar("Status Cancelado", () -> { Projeto p = valido(); p.setStatus("Cancelado"); ProjetoService.validarProjeto(p); });
        testarErro("Status não permitido", () -> { Projeto p = valido(); p.setStatus("Finalizado"); ProjetoService.validarProjeto(p); });

        testar("Prazo futuro", () -> { Projeto p = valido(); p.setDataCriacao(LocalDate.now().toString()); p.setPrazo(LocalDate.now().plusDays(10).toString()); ProjetoService.validarProjeto(p); });
        testar("Prazo na data atual", () -> { Projeto p = valido(); p.setDataCriacao(LocalDate.now().toString()); p.setPrazo(LocalDate.now().toString()); ProjetoService.validarProjeto(p); });
        testar("Prazo passado, mas posterior à criação", () -> { Projeto p = valido(); p.setDataCriacao(LocalDate.now().minusDays(5).toString()); p.setPrazo(LocalDate.now().minusDays(1).toString()); ProjetoService.validarProjeto(p); });
        testarErro("Prazo anterior à criação", () -> { Projeto p = valido(); p.setDataCriacao(LocalDate.now().toString()); p.setPrazo(LocalDate.now().minusDays(1).toString()); ProjetoService.validarProjeto(p); });
        testarErro("Data inexistente 31/02/2026", () -> { Projeto p = valido(); p.setPrazo("31/02/2026"); ProjetoService.validarProjeto(p); });
        testar("Data de criação automática", () -> { Projeto p = valido(); p.setDataCriacao(null); ProjetoService.validarProjeto(p); exigir(LocalDate.now().toString().equals(p.getDataCriacao()), "Data de criação não foi preenchida."); });

        System.out.println("\nResultado: " + aprovados + " aprovados; " + falhas + " falhas.");
        if (falhas > 0) System.exit(1);
    }

    private static Projeto valido() {
        return new Projeto(1, "Sistema Acadêmico", "Descrição de projeto válida", "Software", "Planejado",
                LocalDate.now().toString(), LocalDate.now().plusDays(7).toString());
    }

    private static void testar(String nome, Runnable teste) {
        try { teste.run(); aprovados++; System.out.println("[OK] " + nome); }
        catch (Throwable e) { falhas++; System.out.println("[FALHA] " + nome + " -> " + e.getMessage()); }
    }

    private static void testarErro(String nome, Runnable teste) {
        testar(nome, () -> {
            boolean lancou = false;
            try { teste.run(); } catch (IllegalArgumentException esperado) { lancou = true; }
            exigir(lancou, "Era esperado IllegalArgumentException.");
        });
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) throw new AssertionError(mensagem);
    }
}
