package view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import model.Projeto;

/** Dashboard gráfico Swing: pizza por status, barras por categoria, linha mensal e rosca de prazo. */
public class PainelGraficos extends JPanel {
    private static final Color AZUL = new Color(37, 99, 235);
    private static final Color VERDE = new Color(34, 197, 94);
    private static final Color AMARELO = new Color(245, 158, 11);
    private static final Color ROXO = new Color(139, 92, 246);
    private static final Color VERMELHO = new Color(239, 68, 68);
    private List<Projeto> projetos;
    private boolean temaEscuro;
    private JLabel titulo;
    private JScrollPane rolagem;
    private final List<Grafico> graficos = new ArrayList<>();

    public PainelGraficos(List<Projeto> projetos) {
        this(projetos, "Relatórios de projetos");
    }

    public PainelGraficos(List<Projeto> projetos, String tituloPainel) {
        this.projetos = new ArrayList<>(projetos);
        setLayout(new java.awt.BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        setBackground(new Color(245, 247, 251));
        titulo = new JLabel(tituloPainel, SwingConstants.LEFT);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 19));
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 2, 4, 2));
        add(titulo, java.awt.BorderLayout.NORTH);

        JPanel grade = new JPanel(new java.awt.GridLayout(2, 2, 12, 12));
        grade.setOpaque(false);
        adicionarGrafico(grade, new Grafico("Distribuição por status", Tipo.PIZZA));
        adicionarGrafico(grade, new Grafico("Projetos por categoria", Tipo.BARRAS));
        adicionarGrafico(grade, new Grafico("Evolução mensal de criação", Tipo.LINHA));
        adicionarGrafico(grade, new Grafico("Cumprimento de prazos", Tipo.ROSCA));
        rolagem = new JScrollPane(grade);
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        rolagem.getVerticalScrollBar().setUnitIncrement(14);
        add(rolagem, java.awt.BorderLayout.CENTER);
        setPreferredSize(new Dimension(900, 620));
    }

    private void adicionarGrafico(JPanel grade, Grafico grafico) {
        graficos.add(grafico);
        grade.add(grafico);
    }

    /** Aplica o mesmo tema visual utilizado pelo restante da interface Swing. */
    public void setTemaEscuro(boolean temaEscuro) {
        this.temaEscuro = temaEscuro;
        Color fundo = temaEscuro ? new Color(10, 18, 31) : new Color(245, 247, 251);
        Color superficie = temaEscuro ? new Color(20, 32, 49) : Color.WHITE;
        Color borda = temaEscuro ? new Color(43, 59, 80) : new Color(220, 228, 238);
        setBackground(fundo);
        if (titulo != null) titulo.setForeground(temaEscuro ? new Color(232, 239, 249) : new Color(25, 45, 75));
        if (rolagem != null) {
            rolagem.setBackground(fundo);
            rolagem.getViewport().setBackground(fundo);
        }
        for (Grafico grafico : graficos) {
            grafico.setBackground(superficie);
            grafico.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borda),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            grafico.repaint();
        }
        revalidate();
        repaint();
    }

    /** Atualiza os dados dos gráficos sem recriar a tela. */
    public void setProjetos(List<Projeto> projetos) {
        this.projetos = new ArrayList<>(projetos == null ? List.of() : projetos);
        repaint();
    }

    private enum Tipo { PIZZA, BARRAS, LINHA, ROSCA }

    private class Grafico extends JPanel {
        private final String titulo;
        private final Tipo tipo;
        private final Color[] cores = {AZUL, VERDE, AMARELO, VERMELHO, ROXO};

        Grafico(String titulo, Tipo tipo) {
            this.titulo = titulo;
            this.tipo = tipo;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(220, 228, 238)),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));
            setPreferredSize(new Dimension(420, 245));
        }

        @Override protected void paintComponent(Graphics original) {
            super.paintComponent(original);
            Graphics2D g = (Graphics2D) original.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(temaEscuro ? new Color(232, 239, 249) : new Color(25, 45, 75));
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(titulo, 12, 22);
            switch (tipo) {
                case PIZZA -> desenharPizza(g, false);
                case ROSCA -> desenharPizza(g, true);
                case BARRAS -> desenharBarras(g);
                case LINHA -> desenharLinha(g);
            }
            g.dispose();
        }

        private void desenharPizza(Graphics2D g, boolean rosca) {
            String[] labels = rosca ? new String[]{"No prazo", "Atrasados"}
                    : new String[]{"Planejado", "Em desenvolvimento", "Concluído", "Cancelado"};
            Color[] paleta = rosca ? new Color[]{VERDE, VERMELHO}
                    : new Color[]{ROXO, AMARELO, VERDE, VERMELHO};
            int[] valores = new int[labels.length];
            if (rosca) {
                LocalDate hoje = LocalDate.now();
                for (Projeto p : projetos) {
                    if (p.getPrazo() == null || p.getPrazo().isBlank()) continue;
                    try {
                        if (LocalDate.parse(p.getPrazo()).isBefore(hoje)) valores[1]++; else valores[0]++;
                    } catch (Exception ignored) { }
                }
            } else {
                for (Projeto p : projetos) {
                    for (int i = 0; i < labels.length; i++) if (labels[i].equalsIgnoreCase(p.getStatus())) valores[i]++;
                }
            }
            int total = 0;
            for (int v : valores) total += v;
            int x = 25, y = 48, diametro = Math.min(150, getHeight() - 72);
            if (total == 0) {
                g.setColor(new Color(148, 163, 184));
                g.drawString(rosca ? "Cadastre prazos para visualizar." : "Cadastre projetos para visualizar.", x, y + 70);
            } else {
                int inicio = 90;
                for (int i = 0; i < valores.length; i++) {
                    int angulo = (int) Math.round(360.0 * valores[i] / total);
                    g.setColor(paleta[i]);
                    g.fillArc(x, y, diametro, diametro, inicio, angulo);
                    inicio += angulo;
                }
                if (rosca) {
                    g.setColor(temaEscuro ? new Color(20, 32, 49) : Color.WHITE);
                    g.fillOval(x + diametro / 3, y + diametro / 3, diametro / 3, diametro / 3);
                    g.setColor(temaEscuro ? new Color(232, 239, 249) : new Color(31, 41, 55));
                    g.setFont(new Font("SansSerif", Font.BOLD, 16));
                    g.drawString(String.valueOf(total), x + diametro / 2 - 5, y + diametro / 2 + 5);
                }
            }
            desenharLegenda(g, labels, valores, paleta, x + diametro + 22, y + 28);
        }

        private void desenharLegenda(Graphics2D g, String[] labels, int[] valores, Color[] paleta, int x, int y) {
            for (int i = 0; i < labels.length; i++) {
                g.setColor(paleta[i]);
                g.fillRoundRect(x, y + i * 27, 11, 11, 3, 3);
                g.setColor(temaEscuro ? new Color(190, 203, 220) : new Color(55, 65, 81));
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.drawString(labels[i] + "  " + valores[i], x + 18, y + 10 + i * 27);
            }
        }

        private void desenharBarras(Graphics2D g) {
            String[] cats = {"Web", "Software", "Mobile", "Desktop"};
            int[] valores = new int[cats.length];
            int max = 1;
            for (Projeto p : projetos) for (int i = 0; i < cats.length; i++) if (cats[i].equalsIgnoreCase(p.getCategoria())) valores[i]++;
            for (int v : valores) max = Math.max(max, v);
            int x0 = 90, y0 = 58, larguraMax = Math.max(120, getWidth() - 145);
            for (int i = 0; i < cats.length; i++) {
                int y = y0 + i * 37;
                g.setColor(temaEscuro ? new Color(165, 180, 200) : new Color(71, 85, 105));
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.drawString(cats[i], 15, y + 14);
                int largura = valores[i] == 0 ? 0 : (int) (larguraMax * valores[i] / (double) max);
                g.setColor(temaEscuro ? new Color(42, 58, 78) : new Color(226, 232, 240)); g.fillRoundRect(x0, y, larguraMax, 18, 8, 8);
                g.setColor(AZUL); g.fillRoundRect(x0, y, largura, 18, 8, 8);
                g.setColor(temaEscuro ? new Color(220, 230, 242) : new Color(31, 41, 55)); g.drawString(String.valueOf(valores[i]), x0 + largura + 6, y + 14);
            }
        }

        private void desenharLinha(Graphics2D g) {
            YearMonth atual = YearMonth.now();
            int[] valores = new int[6];
            String[] meses = new String[6];
            for (int i = 0; i < 6; i++) {
                YearMonth mes = atual.minusMonths(5 - i);
                meses[i] = mes.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
                for (Projeto p : projetos) {
                    try { if (p.getDataCriacao() != null && YearMonth.from(LocalDate.parse(p.getDataCriacao())).equals(mes)) valores[i]++; }
                    catch (Exception ignored) { }
                }
            }
            int max = 1; for (int v : valores) max = Math.max(max, v);
            int left = 32, top = 52, width = getWidth() - 62, height = getHeight() - 95;
            g.setColor(temaEscuro ? new Color(42, 58, 78) : new Color(226, 232, 240));
            for (int i = 0; i <= 4; i++) { int y = top + i * height / 4; g.drawLine(left, y, left + width, y); }
            g.setColor(AZUL); g.setStroke(new BasicStroke(2.5f));
            int prevX = -1, prevY = -1;
            for (int i = 0; i < 6; i++) {
                int x = left + i * width / 5;
                int y = top + height - (int) (height * valores[i] / (double) max);
                if (prevX >= 0) g.drawLine(prevX, prevY, x, y);
                g.fillOval(x - 4, y - 4, 8, 8);
                g.setColor(temaEscuro ? new Color(165, 180, 200) : new Color(71, 85, 105)); g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.drawString(meses[i], x - 10, top + height + 19);
                g.setColor(AZUL); prevX = x; prevY = y;
            }
        }
    }
}
