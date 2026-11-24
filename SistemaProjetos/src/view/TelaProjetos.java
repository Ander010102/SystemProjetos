package view;

import client.ApiClient;
import client.AuthApiClient;
import model.Projeto;
import service.ProjetoService;
import util.LogService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JFileChooser;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.prefs.Preferences;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/** Interface desktop do Sistema de Projetos. */
public class TelaProjetos extends JFrame {

    private int itensPorPagina = 6;
    private final Preferences preferencias = Preferences.userNodeForPackage(TelaProjetos.class);
    private static final String[] STATUS_VALIDOS = {
            "Planejado", "Em Desenvolvimento", "Concluído", "Cancelado"
    };
    private static final String[] CATEGORIAS_VALIDAS = {"Software", "Mobile", "Web", "Desktop"};

    private final ApiClient api;
    private final String authToken;
    private final String nomeUsuario;
    private final List<Projeto> todosProjetos = new ArrayList<>();
    private final List<Projeto> projetosFiltrados = new ArrayList<>();
    private final List<Projeto> projetosPagina = new ArrayList<>();

    private JTextField campoNome;
    private JTextArea campoDescricao;
    private JTextField campoDataCriacao;
    private JTextField campoPrazo;
    private JComboBox<String> comboCategoria;
    private JComboBox<String> comboStatus;
    private JComboBox<String> comboFiltro;
    private JTextField campoBusca;

    private JTable tabela;
    private DefaultTableModel modelo;
    private JLabel mensagem;
    private JLabel labelPagina;
    private JLabel valorTotal;
    private JLabel valorConcluidos;
    private JLabel valorAndamento;
    private JLabel valorPlanejados;

    private JButton botaoCadastrar;
    private JButton botaoAlterar;
    private JButton botaoExcluir;
    private JButton botaoLimpar;
    private JButton botaoAtualizar;
    private JButton botaoAnterior;
    private JButton botaoProxima;
    private JButton botaoTema;

    private JPanel raiz;
    private JPanel painelConteudo;
    private JPanel painelTopo;
    private JPanel painelCorpo;
    private JPanel painelStats;
    private JPanel painelBusca;
    private JPanel painelFormulario;
    private JPanel painelLista;
    private JPanel painelSidebar;
    private CardLayout layoutPaginas;
    private JLabel tituloTopo;
    private PainelGraficos graficosRelatorios;
    private DefaultTableModel modeloRecentes;
    private JTable tabelaRecentes;
    private JPanel painelAtividadeRecente;
    private JComboBox<String> comboTemaConfiguracoes;
    private JComboBox<String> comboItensPagina;
    private JLabel labelStatusApi;
    private final Map<String, JButton> botoesMenu = new LinkedHashMap<>();
    private final Map<String, JLabel> contagemCategorias = new LinkedHashMap<>();
    private final List<JPanel> cartoesTematicos = new ArrayList<>();
    private final List<JPanel> cartoesEstatisticos = new ArrayList<>();
    private final List<JButton> botoesContorno = new ArrayList<>();
    private final List<JButton> botoesSolidosTematicos = new ArrayList<>();
    private String paginaAtiva = "DASHBOARD";

    private int idSelecionado = -1;
    private int paginaAtual = 0;
    private int colunaOrdenada = 1;
    private boolean ordemAscendente = true;
    private boolean ocupado;
    private boolean temaEscuro;

    private final Color azul = new Color(37, 99, 235);
    private final Color verde = new Color(22, 163, 74);
    private final Color amarelo = new Color(217, 119, 6);
    private final Color roxo = new Color(147, 51, 234);
    private final Color sidebarEscura = new Color(13, 48, 82);

    public TelaProjetos(String authToken, String nomeUsuario) {
        this.authToken = authToken;
        this.nomeUsuario = nomeUsuario == null || nomeUsuario.isBlank() ? "Usuário" : nomeUsuario;
        this.api = new ApiClient(authToken);
        this.temaEscuro = preferencias.getBoolean("temaEscuro", false);
        this.itensPorPagina = preferencias.getInt("itensPorPagina", 6);
        if (this.itensPorPagina != 5 && this.itensPorPagina != 6 && this.itensPorPagina != 10 && this.itensPorPagina != 20) {
            this.itensPorPagina = 6;
        }
        setTitle("Sistema de Projetos");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 680));
        setSize(1280, 760);
        setLocationRelativeTo(null);
        setContentPane(criarInterface());
        criarEventos();
        aplicarTema();
        carregarTabela();
    }

    private JPanel criarInterface() {
        raiz = new JPanel(new BorderLayout());
        painelConteudo = new JPanel(new BorderLayout());
        painelTopo = criarBarraSuperior();

        layoutPaginas = new CardLayout();
        painelCorpo = new JPanel(layoutPaginas);
        painelCorpo.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        // A tela de projetos é construída primeiro porque cria os campos e a tabela
        // compartilhados com as demais áreas (busca, filtros e seleção de projeto).
        painelCorpo.add(criarPainelProjetos(), "PROJETOS");
        painelCorpo.add(criarPainelDashboard(), "DASHBOARD");
        painelCorpo.add(criarPainelCategorias(), "CATEGORIAS");
        painelCorpo.add(criarPainelRelatorios(), "RELATORIOS");
        painelCorpo.add(criarPainelConfiguracoes(), "CONFIGURACOES");

        JScrollPane rolagemCorpo = new JScrollPane(painelCorpo);
        rolagemCorpo.setBorder(BorderFactory.createEmptyBorder());
        rolagemCorpo.getVerticalScrollBar().setUnitIncrement(16);
        rolagemCorpo.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        painelConteudo.add(painelTopo, BorderLayout.NORTH);
        painelConteudo.add(rolagemCorpo, BorderLayout.CENTER);

        painelSidebar = criarSidebar();
        raiz.add(painelSidebar, BorderLayout.WEST);
        raiz.add(painelConteudo, BorderLayout.CENTER);
        mostrarPagina("DASHBOARD");
        return raiz;
    }

    private JPanel criarPainelProjetos() {
        JPanel pagina = new JPanel(new BorderLayout(0, 10));
        pagina.setOpaque(false);
        pagina.add(criarCabecalhoPagina("Projetos", "Cadastre, pesquise, edite e acompanhe seus projetos."), BorderLayout.NORTH);

        JPanel conteudo = new JPanel(new BorderLayout(0, 7));
        conteudo.setOpaque(false);
        conteudo.add(criarAreaPrincipal(), BorderLayout.CENTER);
        mensagem = new JLabel("Conectando à API...");
        mensagem.setFont(new Font("SansSerif", Font.PLAIN, 11));
        mensagem.setBorder(BorderFactory.createEmptyBorder(3, 2, 2, 2));
        conteudo.add(mensagem, BorderLayout.SOUTH);
        pagina.add(conteudo, BorderLayout.CENTER);
        return pagina;
    }

    private JPanel criarPainelDashboard() {
        JPanel pagina = new JPanel(new BorderLayout(0, 12));
        pagina.setOpaque(false);
        pagina.add(criarCabecalhoPagina("Dashboard", "Visão geral dos projetos e da atividade recente."), BorderLayout.NORTH);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setOpaque(false);
        centro.add(criarCardsEstatisticos(), BorderLayout.NORTH);
        centro.add(criarPainelAtividadeRecente(), BorderLayout.CENTER);
        pagina.add(centro, BorderLayout.CENTER);
        return pagina;
    }

    private JPanel criarPainelAtividadeRecente() {
        painelAtividadeRecente = new JPanel(new BorderLayout(0, 9));
        JPanel painel = painelAtividadeRecente;
        painel.setOpaque(true);
        painel.setBackground(Color.WHITE);
        painel.setBorder(bordaCartao(new Color(226, 232, 240), 12, 14, 12, 14));
        cartoesTematicos.add(painel);
        JLabel titulo = new JLabel("Projetos recentes");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel subtitulo = new JLabel("Últimos projetos registrados no sistema.");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 10));
        marcarTextoSecundario(subtitulo);
        subtitulo.setForeground(new Color(100, 116, 139));
        JPanel cabecalho = new JPanel();
        cabecalho.setOpaque(false);
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        cabecalho.add(titulo);
        cabecalho.add(Box.createVerticalStrut(3));
        cabecalho.add(subtitulo);
        painel.add(cabecalho, BorderLayout.NORTH);

        modeloRecentes = new DefaultTableModel(new Object[]{"ID", "Nome do Projeto", "Categoria", "Status", "Prazo"}, 0) {
            @Override public boolean isCellEditable(int linha, int coluna) { return false; }
        };
        tabelaRecentes = new JTable(modeloRecentes);
        tabelaRecentes.setRowHeight(29);
        tabelaRecentes.setFillsViewportHeight(true);
        tabelaRecentes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaRecentes.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());
        tabelaRecentes.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tabelaRecentes.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10));
        tabelaRecentes.getTableHeader().setReorderingAllowed(false);
        tabelaRecentes.getColumnModel().getColumn(0).setPreferredWidth(45);
        tabelaRecentes.getColumnModel().getColumn(1).setPreferredWidth(270);
        tabelaRecentes.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabelaRecentes.getColumnModel().getColumn(3).setPreferredWidth(130);
        tabelaRecentes.getColumnModel().getColumn(4).setPreferredWidth(100);
        JScrollPane scroll = new JScrollPane(tabelaRecentes);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        painel.add(scroll, BorderLayout.CENTER);

        JButton abrirProjetos = new JButton("Ver todos os projetos", new IconeSistema("folder", 13, Color.WHITE));
        estilizarBotao(abrirProjetos, azul);
        abrirProjetos.addActionListener(e -> mostrarPagina("PROJETOS"));
        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rodape.setOpaque(false);
        rodape.add(abrirProjetos);
        painel.add(rodape, BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarPainelCategorias() {
        JPanel pagina = new JPanel(new BorderLayout(0, 12));
        pagina.setOpaque(false);
        pagina.add(criarCabecalhoPagina("Categorias", "Explore os projetos agrupados por categoria."), BorderLayout.NORTH);

        // Cards compactos, em duas colunas, como na interface Web.
        JPanel grade = new JPanel(new GridLayout(2, 2, 12, 12));
        grade.setOpaque(false);
        grade.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        adicionarCardCategoria(grade, "Software", "monitor", azul);
        adicionarCardCategoria(grade, "Mobile", "mobile", verde);
        adicionarCardCategoria(grade, "Web", "globe", amarelo);
        adicionarCardCategoria(grade, "Desktop", "desktop", roxo);
        JPanel corpo = new JPanel(new BorderLayout(0, 12));
        corpo.setOpaque(false);
        corpo.add(grade, BorderLayout.NORTH);
        pagina.add(corpo, BorderLayout.CENTER);
        return pagina;
    }

    private void adicionarCardCategoria(JPanel grade, String categoria, String icone, Color cor) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(390, 86));
        card.setBorder(bordaCartao(new Color(226, 232, 240), 12, 14, 12, 14));
        cartoesTematicos.add(card);

        // O ícone fica dentro de um quadrado fixo; não acompanha a altura do cartão.
        JPanel areaIcone = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        areaIcone.setOpaque(false);
        areaIcone.setPreferredSize(new Dimension(42, 42));
        areaIcone.setMaximumSize(new Dimension(42, 42));
        JLabel simbolo = new JLabel(new IconeSistema(icone, 21, Color.WHITE), SwingConstants.CENTER);
        simbolo.setOpaque(true);
        simbolo.setBackground(cor);
        simbolo.setPreferredSize(new Dimension(38, 38));
        simbolo.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        areaIcone.add(simbolo);

        JPanel texto = new JPanel();
        texto.setOpaque(false);
        texto.setLayout(new BoxLayout(texto, BoxLayout.Y_AXIS));
        JLabel nome = new JLabel(categoria);
        nome.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel quantidade = new JLabel("0 projetos cadastrados");
        quantidade.setFont(new Font("SansSerif", Font.PLAIN, 11));
        marcarTextoSecundario(quantidade);
        quantidade.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        contagemCategorias.put(categoria, quantidade);
        texto.add(nome);
        texto.add(quantidade);

        JButton ver = new JButton("Ver projetos", new IconeSistema("folder", 13, cor));
        estilizarBotaoContorno(ver, cor);
        ver.setPreferredSize(new Dimension(126, 34));
        ver.setMaximumSize(new Dimension(126, 34));
        ver.addActionListener(e -> {
            comboFiltro.setSelectedItem("Categoria");
            campoBusca.setText(categoria);
            mostrarPagina("PROJETOS");
            campoBusca.requestFocusInWindow();
        });

        card.add(areaIcone, BorderLayout.WEST);
        card.add(texto, BorderLayout.CENTER);
        card.add(ver, BorderLayout.EAST);
        grade.add(card);
    }

    private JPanel criarPainelRelatorios() {
        JPanel pagina = new JPanel(new BorderLayout(0, 10));
        pagina.setOpaque(false);
        JPanel cabecalho = criarCabecalhoPagina("Relatórios", "Indicadores, gráficos e distribuição dos projetos.");
        JButton exportar = new JButton("Exportar CSV", new IconeSistema("download", 13, Color.WHITE));
        estilizarBotao(exportar, azul);
        exportar.addActionListener(e -> exportarProjetosCsv());
        cabecalho.add(exportar, BorderLayout.EAST);
        pagina.add(cabecalho, BorderLayout.NORTH);
        graficosRelatorios = new PainelGraficos(todosProjetos, "Análise dos projetos");
        pagina.add(graficosRelatorios, BorderLayout.CENTER);
        return pagina;
    }

    private JPanel criarPainelConfiguracoes() {
        JPanel pagina = new JPanel(new BorderLayout(0, 12));
        pagina.setOpaque(false);
        pagina.add(criarCabecalhoPagina("Configurações", "Gerencie sua conta e personalize o comportamento do sistema."), BorderLayout.NORTH);

        // Grid com altura natural: evita que combos e campos sejam esticados verticalmente.
        JPanel grade = new JPanel(new GridLayout(3, 2, 12, 12));
        grade.setOpaque(false);

        JPanel conta = criarCartaoConfiguracao("Minha conta", "user");
        conta.add(criarLinhaInformacao("Nome", nomeUsuario));
        conta.add(Box.createVerticalStrut(7));
        conta.add(criarLinhaInformacao("Sessão", "Autenticada"));
        grade.add(conta);

        JPanel aparencia = criarCartaoConfiguracao("Aparência", "palette");
        JLabel ajudaTema = new JLabel("Escolha como deseja visualizar o sistema.");
        ajudaTema.setFont(new Font("SansSerif", Font.PLAIN, 11));
        marcarTextoSecundario(ajudaTema);
        ajudaTema.setAlignmentX(Component.LEFT_ALIGNMENT);
        aparencia.add(ajudaTema);
        aparencia.add(Box.createVerticalStrut(8));
        comboTemaConfiguracoes = new JComboBox<>(new String[]{"Claro", "Escuro"});
        comboTemaConfiguracoes.setSelectedItem(temaEscuro ? "Escuro" : "Claro");
        configurarComboVisual(comboTemaConfiguracoes);
        comboTemaConfiguracoes.addActionListener(e -> {
            if (comboTemaConfiguracoes == null) return;
            boolean escuro = "Escuro".equals(comboTemaConfiguracoes.getSelectedItem());
            if (temaEscuro != escuro) {
                temaEscuro = escuro;
                preferencias.putBoolean("temaEscuro", temaEscuro);
                aplicarTema();
                atualizarIconeTema();
            }
        });
        aparencia.add(criarCampo("Tema do sistema", comboTemaConfiguracoes));
        grade.add(aparencia);

        JPanel preferenciasPainel = criarCartaoConfiguracao("Preferências de listagem", "sliders");
        JLabel ajudaListagem = new JLabel("Defina quantos projetos aparecem em cada página.");
        ajudaListagem.setFont(new Font("SansSerif", Font.PLAIN, 11));
        marcarTextoSecundario(ajudaListagem);
        ajudaListagem.setAlignmentX(Component.LEFT_ALIGNMENT);
        preferenciasPainel.add(ajudaListagem);
        preferenciasPainel.add(Box.createVerticalStrut(8));
        comboItensPagina = new JComboBox<>(new String[]{"5", "6", "10", "20"});
        comboItensPagina.setSelectedItem(String.valueOf(itensPorPagina));
        configurarComboVisual(comboItensPagina);
        comboItensPagina.addActionListener(e -> {
            try {
                itensPorPagina = Integer.parseInt(String.valueOf(comboItensPagina.getSelectedItem()));
                preferencias.putInt("itensPorPagina", itensPorPagina);
                paginaAtual = 0;
                atualizarTabelaVisivel();
            } catch (NumberFormatException ignored) { }
        });
        preferenciasPainel.add(criarCampo("Projetos por página", comboItensPagina));
        grade.add(preferenciasPainel);

        JPanel conexao = criarCartaoConfiguracao("Conexão com a API", "server");
        labelStatusApi = new JLabel("Aguardando verificação...");
        labelStatusApi.setFont(new Font("SansSerif", Font.BOLD, 11));
        marcarTextoSecundario(labelStatusApi);
        labelStatusApi.setAlignmentX(Component.LEFT_ALIGNMENT);
        conexao.add(labelStatusApi);
        conexao.add(Box.createVerticalStrut(5));
        JLabel enderecoApi = new JLabel("Servidor configurado em http://localhost:7070.");
        enderecoApi.setFont(new Font("SansSerif", Font.PLAIN, 10));
        marcarTextoSecundario(enderecoApi);
        enderecoApi.setAlignmentX(Component.LEFT_ALIGNMENT);
        conexao.add(enderecoApi);
        conexao.add(Box.createVerticalStrut(9));
        JButton testar = new JButton("Testar conexão", new IconeSistema("refresh", 13, azul));
        estilizarBotaoContorno(testar, azul);
        testar.setAlignmentX(Component.LEFT_ALIGNMENT);
        testar.addActionListener(e -> executar("Testando conexão com a API...", api::listar, lista -> {
            labelStatusApi.setText("API conectada · " + lista.size() + " projeto(s) disponível(is).");
            labelStatusApi.putClientProperty("corStatus", "sucesso");
            aplicarTema();
            if (mensagem != null) mensagem.setText("Conexão com a API verificada com sucesso.");
        }));
        conexao.add(testar);
        grade.add(conexao);

        JPanel dados = criarCartaoConfiguracao("Dados e armazenamento", "database");
        JLabel descricaoDados = new JLabel("Projetos armazenados pelo servidor em arquivo CSV.");
        descricaoDados.setFont(new Font("SansSerif", Font.PLAIN, 11));
        marcarTextoSecundario(descricaoDados);
        descricaoDados.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel descricaoLogs = new JLabel("Logs de aplicação, acesso, auditoria, erros e API na pasta logs.");
        descricaoLogs.setFont(new Font("SansSerif", Font.PLAIN, 11));
        marcarTextoSecundario(descricaoLogs);
        descricaoLogs.setAlignmentX(Component.LEFT_ALIGNMENT);
        dados.add(descricaoDados);
        dados.add(Box.createVerticalStrut(5));
        dados.add(descricaoLogs);
        grade.add(dados);

        JPanel seguranca = criarCartaoConfiguracao("Segurança", "logout");
        JLabel descricaoSeguranca = new JLabel("Encerre sua sessão neste dispositivo com segurança.");
        descricaoSeguranca.setFont(new Font("SansSerif", Font.PLAIN, 11));
        marcarTextoSecundario(descricaoSeguranca);
        descricaoSeguranca.setAlignmentX(Component.LEFT_ALIGNMENT);
        seguranca.add(descricaoSeguranca);
        seguranca.add(Box.createVerticalStrut(10));
        JButton encerrar = new JButton("Encerrar sessão", new IconeSistema("logout", 13, new Color(220, 38, 38)));
        estilizarBotaoContorno(encerrar, new Color(220, 38, 38));
        encerrar.setAlignmentX(Component.LEFT_ALIGNMENT);
        encerrar.addActionListener(e -> sair());
        seguranca.add(encerrar);
        grade.add(seguranca);

        JPanel corpo = new JPanel(new BorderLayout(0, 12));
        corpo.setOpaque(false);
        corpo.add(grade, BorderLayout.NORTH);
        pagina.add(corpo, BorderLayout.CENTER);
        return pagina;
    }

    private JPanel criarCartaoConfiguracao(String titulo, String icone) {
        JPanel card = new JPanel();
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setPreferredSize(new Dimension(420, 154));
        card.setBorder(bordaCartao(new Color(226, 232, 240), 12, 14, 12, 14));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        cartoesTematicos.add(card);

        JLabel heading = new JLabel(titulo, new IconeSistema(icone, 16, azul), SwingConstants.LEFT);
        heading.setIconTextGap(9);
        heading.setFont(new Font("SansSerif", Font.BOLD, 13));
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(heading);
        card.add(Box.createVerticalStrut(11));
        return card;
    }

    private JPanel criarLinhaInformacao(String rotulo, String valor) {
        JPanel linha = new JPanel(new BorderLayout(8, 0));
        linha.setOpaque(false);
        JLabel chave = new JLabel(rotulo);
        chave.setFont(new Font("SansSerif", Font.PLAIN, 11));
        marcarTextoSecundario(chave);
        JLabel conteudo = new JLabel(valor);
        conteudo.setFont(new Font("SansSerif", Font.BOLD, 12));
        linha.add(chave, BorderLayout.WEST);
        linha.add(conteudo, BorderLayout.EAST);
        linha.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        linha.setAlignmentX(Component.LEFT_ALIGNMENT);
        return linha;
    }

    private Border bordaCartao(Color cor, int top, int left, int bottom, int right) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor),
                BorderFactory.createEmptyBorder(top, left, bottom, right));
    }

    private void marcarTextoSecundario(JLabel label) {
        label.putClientProperty("textoSecundario", Boolean.TRUE);
    }

    private void estilizarBotaoContorno(JButton botao, Color cor) {
        botao.setBackground(Color.WHITE);
        botao.setForeground(cor);
        botao.setFont(new Font("SansSerif", Font.BOLD, 11));
        botao.setIconTextGap(7);
        botao.setFocusPainted(false);
        botao.setOpaque(true);
        botao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        botao.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        botao.putClientProperty("botaoContorno", Boolean.TRUE);
        botao.putClientProperty("corDestaque", cor);
        botoesContorno.add(botao);
    }

    private void configurarComboVisual(JComboBox<?> combo) {
        combo.setPreferredSize(new Dimension(220, 34));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                           boolean selecionado, boolean foco) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        lista, valor, indice, selecionado, foco);
                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (selecionado) {
                    label.setBackground(temaEscuro ? new Color(37, 99, 235) : new Color(219, 234, 254));
                    label.setForeground(temaEscuro ? Color.WHITE : new Color(30, 64, 120));
                } else {
                    label.setBackground(temaEscuro ? new Color(16, 29, 47) : Color.WHITE);
                    label.setForeground(temaEscuro ? new Color(226, 232, 240) : new Color(31, 41, 55));
                }
                return label;
            }
        });
    }

    private JPanel criarCabecalhoPagina(String tituloTexto, String subtituloTexto) {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setOpaque(false);
        JLabel titulo = new JLabel(tituloTexto);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel subtitulo = new JLabel(subtituloTexto);
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        marcarTextoSecundario(subtitulo);
        subtitulo.setForeground(new Color(100, 116, 139));
        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(3));
        textos.add(subtitulo);
        painel.add(textos, BorderLayout.WEST);
        return painel;
    }

    private void mostrarPagina(String pagina) {
        if (layoutPaginas == null || painelCorpo == null) return;
        paginaAtiva = pagina;
        layoutPaginas.show(painelCorpo, pagina);
        String titulo = switch (pagina) {
            case "PROJETOS" -> "Projetos";
            case "CATEGORIAS" -> "Categorias";
            case "RELATORIOS" -> "Relatórios";
            case "CONFIGURACOES" -> "Configurações";
            default -> "Dashboard";
        };
        if (tituloTopo != null) tituloTopo.setText("Sistema de Projetos  /  " + titulo);
        botoesMenu.forEach((chave, botao) -> {
            boolean ativo = chave.equals(pagina);
            botao.putClientProperty("menuAtivo", ativo);
            botao.setBackground(ativo ? new Color(7, 89, 166) : sidebarEscura);
        });
        if ("CATEGORIAS".equals(pagina)) atualizarContagensCategorias();
        if ("RELATORIOS".equals(pagina) && graficosRelatorios != null) graficosRelatorios.setProjetos(todosProjetos);
        painelCorpo.revalidate();
        painelCorpo.repaint();
    }

    private void atualizarContagensCategorias() {
        for (Map.Entry<String, JLabel> entrada : contagemCategorias.entrySet()) {
            long total = todosProjetos.stream().filter(p -> entrada.getKey().equalsIgnoreCase(seguro(p.getCategoria()))).count();
            entrada.getValue().setText(total + (total == 1 ? " projeto cadastrado" : " projetos cadastrados"));
        }
    }

    private void atualizarAtividadeRecente() {
        if (modeloRecentes == null) return;
        modeloRecentes.setRowCount(0);
        List<Projeto> recentes = new ArrayList<>(todosProjetos);
        recentes.sort(Comparator.comparing((Projeto p) -> seguro(p.getDataCriacao())).reversed());
        for (Projeto p : recentes.stream().limit(5).toList()) {
            modeloRecentes.addRow(new Object[]{p.getId(), p.getNome(), p.getCategoria(), p.getStatus(), dataParaTela(p.getPrazo())});
        }
    }

    private void exportarProjetosCsv() {
        JFileChooser seletor = new JFileChooser();
        seletor.setDialogTitle("Exportar relatório de projetos");
        seletor.setSelectedFile(new java.io.File("relatorio_projetos.csv"));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        Path destino = seletor.getSelectedFile().toPath();
        if (!destino.toString().toLowerCase(Locale.ROOT).endsWith(".csv")) destino = Path.of(destino + ".csv");
        try (BufferedWriter escritor = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            escritor.write("id;nome;descricao;categoria;status;dataCriacao;prazo");
            escritor.newLine();
            for (Projeto p : todosProjetos) {
                escritor.write(csv(p.getId()) + ";" + csv(p.getNome()) + ";" + csv(p.getDescricao()) + ";"
                        + csv(p.getCategoria()) + ";" + csv(p.getStatus()) + ";" + csv(p.getDataCriacao()) + ";" + csv(p.getPrazo()));
                escritor.newLine();
            }
            JOptionPane.showMessageDialog(this, "Relatório exportado com sucesso.", "Exportação", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            LogService.erro("Falha ao exportar relatório CSV: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Não foi possível exportar o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String csv(Object valor) {
        String texto = valor == null ? "" : String.valueOf(valor);
        return "\"" + texto.replace("\"", "\"\"") + "\"";
    }

    private JPanel criarSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setBackground(sidebarEscura);
        sidebar.setPreferredSize(new Dimension(198, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(18, 10, 14, 10));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));

        JLabel logo = new JLabel("<html><b>Sistema</b><br>de Projetos</html>", new IconeSistema("layers", 24, Color.WHITE), SwingConstants.LEFT);
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("SansSerif", Font.BOLD, 13));
        logo.setIconTextGap(10);
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 4, 22, 0));
        sidebar.add(logo);

        JButton dashboard = criarBotaoMenu("Dashboard", "home", () -> mostrarPagina("DASHBOARD"));
        JButton projetos = criarBotaoMenu("Projetos", "folder", () -> mostrarPagina("PROJETOS"));
        JButton categorias = criarBotaoMenu("Categorias", "grid", () -> mostrarPagina("CATEGORIAS"));
        JButton relatorios = criarBotaoMenu("Relatórios", "chart", () -> mostrarPagina("RELATORIOS"));
        JButton configuracoes = criarBotaoMenu("Configurações", "gear", () -> mostrarPagina("CONFIGURACOES"));
        botoesMenu.put("DASHBOARD", dashboard);
        botoesMenu.put("PROJETOS", projetos);
        botoesMenu.put("CATEGORIAS", categorias);
        botoesMenu.put("RELATORIOS", relatorios);
        botoesMenu.put("CONFIGURACOES", configuracoes);
        sidebar.add(dashboard);
        sidebar.add(projetos);
        sidebar.add(categorias);
        sidebar.add(relatorios);
        sidebar.add(configuracoes);

        sidebar.add(Box.createVerticalGlue());
        JLabel usuario = new JLabel(this.nomeUsuario, new IconeSistema("user", 18, new Color(203, 213, 225)), SwingConstants.LEFT);
        usuario.setIconTextGap(8);
        usuario.setForeground(new Color(203, 213, 225));
        usuario.setFont(new Font("SansSerif", Font.PLAIN, 12));
        usuario.setBorder(BorderFactory.createEmptyBorder(18, 4, 8, 0));
        usuario.setAlignmentX(Component.LEFT_ALIGNMENT);
        sidebar.add(usuario);
        sidebar.add(criarBotaoMenu("Sair", "logout", this::sair));
        return sidebar;
    }

    private JButton criarBotaoMenu(String texto, String nomeIcone, Runnable acao) {
        JButton botao = new JButton(texto, new IconeSistema(nomeIcone, 15, new Color(226, 232, 240)));
        botao.setHorizontalAlignment(SwingConstants.LEFT);
        botao.setIconTextGap(12);
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        botao.setPreferredSize(new Dimension(178, 38));
        botao.setFont(new Font("SansSerif", Font.PLAIN, 12));
        botao.setForeground(new Color(226, 232, 240));
        botao.setBackground(sidebarEscura);
        botao.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 8));
        botao.setFocusPainted(false);
        botao.setOpaque(true);
        botao.addActionListener(e -> acao.run());
        botao.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (!Boolean.TRUE.equals(botao.getClientProperty("menuAtivo"))) {
                    botao.setBackground(new Color(19, 66, 108));
                }
            }
            @Override public void mouseExited(MouseEvent e) {
                botao.setBackground(Boolean.TRUE.equals(botao.getClientProperty("menuAtivo"))
                        ? new Color(7, 89, 166) : sidebarEscura);
            }
        });
        return botao;
    }

    private JPanel criarBarraSuperior() {
        JPanel topo = new JPanel(new BorderLayout());
        topo.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        topo.setPreferredSize(new Dimension(0, 50));
        tituloTopo = new JLabel("Sistema de Projetos  /  Dashboard");
        tituloTopo.setFont(new Font("SansSerif", Font.BOLD, 14));

        JPanel ladoDireito = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        JLabel usuarioTopo = new JLabel(nomeUsuario, new IconeSistema("user", 15, new Color(71, 85, 105)), SwingConstants.LEFT);
        usuarioTopo.setIconTextGap(7);
        usuarioTopo.setFont(new Font("SansSerif", Font.PLAIN, 11));
        botaoTema = new JButton(new IconeSistema("moon", 16, Color.WHITE));
        botaoTema.setToolTipText("Alternar tema claro/escuro");
        botaoTema.setPreferredSize(new Dimension(34, 30));
        botaoTema.setText("");
        estilizarBotao(botaoTema, new Color(71, 85, 105));
        botaoTema.addActionListener(e -> alternarTema());
        ladoDireito.add(usuarioTopo);
        ladoDireito.add(botaoTema);
        topo.add(tituloTopo, BorderLayout.WEST);
        topo.add(ladoDireito, BorderLayout.EAST);
        return topo;
    }

    private JPanel criarCardsEstatisticos() {
        painelStats = new JPanel(new GridLayout(1, 5, 9, 0));
        painelStats.setOpaque(false);
        valorTotal = new JLabel("0");
        valorConcluidos = new JLabel("0");
        valorAndamento = new JLabel("0");
        valorPlanejados = new JLabel("0");
        painelStats.add(criarCard("Projetos totais", valorTotal, azul, "folder"));
        painelStats.add(criarCard("Concluídos", valorConcluidos, verde, "check"));
        painelStats.add(criarCard("Projetos ativos", valorAndamento, amarelo, "clock"));
        painelStats.add(criarCard("Cancelados", valorPlanejados, roxo, "ban"));
        painelStats.add(criarCardData());
        painelStats.setPreferredSize(new Dimension(900, 76));
        painelStats.setMaximumSize(new Dimension(Integer.MAX_VALUE, 76));
        return painelStats;
    }

    private JPanel criarCard(String titulo, JLabel valor, Color destaque, String icone) {
        JPanel card = new JPanel(new BorderLayout(9, 0));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(bordaCartao(new Color(226, 232, 240), 8, 9, 8, 9));
        cartoesEstatisticos.add(card);
        JLabel simbolo = new JLabel(new IconeSistema(icone, 17, Color.WHITE), SwingConstants.CENTER);
        simbolo.setOpaque(true);
        simbolo.setBackground(destaque);
        simbolo.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        simbolo.setPreferredSize(new Dimension(34, 34));
        JPanel informacoes = new JPanel();
        informacoes.setOpaque(false);
        informacoes.setLayout(new BoxLayout(informacoes, BoxLayout.Y_AXIS));
        valor.setFont(new Font("SansSerif", Font.BOLD, 21));
        valor.setForeground(destaque);
        JLabel labelTitulo = new JLabel(titulo);
        labelTitulo.setFont(new Font("SansSerif", Font.PLAIN, 9));
        marcarTextoSecundario(labelTitulo);
        labelTitulo.setForeground(new Color(100, 116, 139));
        informacoes.add(valor);
        informacoes.add(labelTitulo);
        card.add(simbolo, BorderLayout.WEST);
        card.add(informacoes, BorderLayout.CENTER);
        return card;
    }

    private JPanel criarCardData() {
        JPanel card = new JPanel(new BorderLayout(8, 2));
        card.setOpaque(true);
        card.setBackground(Color.WHITE);
        card.setBorder(bordaCartao(new Color(226, 232, 240), 8, 10, 7, 10));
        cartoesEstatisticos.add(card);
        JLabel icone = new JLabel(new IconeSistema("calendar", 17, Color.WHITE), SwingConstants.CENTER);
        icone.setOpaque(true);
        icone.setBackground(azul);
        icone.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        icone.setPreferredSize(new Dimension(34, 34));
        JPanel informacoes = new JPanel();
        informacoes.setOpaque(false);
        informacoes.setLayout(new BoxLayout(informacoes, BoxLayout.Y_AXIS));
        JLabel data = new JLabel(LocalDate.now().format(
                DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))));
        data.setFont(new Font("SansSerif", Font.BOLD, 10));
        JLabel hora = new JLabel(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("EEEE, HH:mm", Locale.forLanguageTag("pt-BR"))));
        hora.setFont(new Font("SansSerif", Font.PLAIN, 9));
        marcarTextoSecundario(hora);
        hora.setForeground(new Color(100, 116, 139));
        informacoes.add(data);
        informacoes.add(Box.createVerticalStrut(3));
        informacoes.add(hora);
        card.add(icone, BorderLayout.WEST);
        card.add(informacoes, BorderLayout.CENTER);
        return card;
    }

    private JPanel criarBarraBusca() {
        painelBusca = new JPanel(new BorderLayout(10, 0));
        painelBusca.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        campoBusca = new JTextField();
        campoBusca.setToolTipText("Pesquisar por nome, categoria ou status");
        campoBusca.setPreferredSize(new Dimension(200, 30));
        comboFiltro = new JComboBox<>(new String[]{
                "Todos", "Nome", "Categoria", "Status"
        });
        comboFiltro.setPreferredSize(new Dimension(88, 30));
        JPanel campoPesquisa = new JPanel(new BorderLayout(8, 0));
        campoPesquisa.setOpaque(false);
        JLabel lupa = new JLabel(new IconeSistema("search", 16, new Color(100, 116, 139)));
        campoPesquisa.add(lupa, BorderLayout.WEST);
        campoPesquisa.add(campoBusca, BorderLayout.CENTER);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controles.setOpaque(false);
        comboFiltro.setVisible(false); // O campo pesquisa em todos os dados, como no layout de referência.
        botaoAtualizar = new JButton(new IconeSistema("refresh", 16, Color.WHITE));
        botaoAtualizar.setText("");
        botaoAtualizar.setToolTipText("Atualizar lista");
        botaoAtualizar.setPreferredSize(new Dimension(34, 30));
        estilizarBotao(botaoAtualizar, azul);
        controles.add(botaoAtualizar);
        painelBusca.add(campoPesquisa, BorderLayout.CENTER);
        painelBusca.add(controles, BorderLayout.EAST);
        return painelBusca;
    }

    private JSplitPane criarAreaPrincipal() {
        painelFormulario = new JPanel();
        painelFormulario.setLayout(new BoxLayout(painelFormulario, BoxLayout.Y_AXIS));
        painelFormulario.setOpaque(true);
        painelFormulario.setBackground(Color.WHITE);
        painelFormulario.setBorder(bordaCartao(new Color(226, 232, 240), 12, 12, 11, 12));
        painelFormulario.setMinimumSize(new Dimension(300, 0));

        JLabel tituloForm = new JLabel("Cadastro de projeto");
        tituloForm.setFont(new Font("SansSerif", Font.BOLD, 13));
        tituloForm.setAlignmentX(Component.LEFT_ALIGNMENT);
        painelFormulario.add(tituloForm);
        painelFormulario.add(Box.createVerticalStrut(12));

        JPanel campos = new JPanel();
        campos.setOpaque(false);
        campos.setLayout(new BoxLayout(campos, BoxLayout.Y_AXIS));
        campoNome = new JTextField();
        campoNome.setPreferredSize(new Dimension(240, 30));
        campoNome.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        campoDescricao = new JTextArea(3, 20);
        campoDescricao.setLineWrap(true);
        campoDescricao.setWrapStyleWord(true);
        JScrollPane scrollDescricao = new JScrollPane(campoDescricao);
        scrollDescricao.setPreferredSize(new Dimension(240, 72));
        scrollDescricao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        comboCategoria = new JComboBox<>(CATEGORIAS_VALIDAS);
        comboCategoria.setEditable(false);
        comboCategoria.setPreferredSize(new Dimension(120, 30));
        comboStatus = new JComboBox<>(STATUS_VALIDOS);
        comboStatus.setPreferredSize(new Dimension(120, 30));
        campoDataCriacao = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        campoDataCriacao.setEditable(false);
        campoPrazo = new JTextField();
        campoPrazo.setToolTipText("Informe DD/MM/AAAA ou AAAA-MM-DD. Campo opcional.");
        campoPrazo.setPreferredSize(new Dimension(120, 30));

        JPanel campoNomePainel = criarCampo("Nome do projeto *", campoNome);
        campoNomePainel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 53));
        JPanel campoDescricaoPainel = criarCampo("Descrição (mínimo 10 caracteres) *", scrollDescricao);
        campoDescricaoPainel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        campos.add(campoNomePainel);
        campos.add(Box.createVerticalStrut(4));
        campos.add(campoDescricaoPainel);
        campos.add(Box.createVerticalStrut(4));
        JPanel linhaCategoriaStatus = new JPanel(new GridLayout(1, 2, 8, 0));
        linhaCategoriaStatus.setOpaque(false);
        linhaCategoriaStatus.add(criarCampo("Categoria", comboCategoria));
        linhaCategoriaStatus.add(criarCampo("Status *", comboStatus));
        linhaCategoriaStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        linhaCategoriaStatus.setPreferredSize(new Dimension(300, 56));
        campos.add(linhaCategoriaStatus);
        campos.add(Box.createVerticalStrut(3));
        JPanel linhaDatas = new JPanel(new GridLayout(1, 2, 8, 0));
        linhaDatas.setOpaque(false);
        linhaDatas.add(criarCampo("Data de criação", campoDataCriacao));
        linhaDatas.add(criarCampo("Prazo (opcional)", campoPrazo));
        linhaDatas.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        linhaDatas.setPreferredSize(new Dimension(300, 56));
        campos.add(linhaDatas);
        campos.setAlignmentX(Component.LEFT_ALIGNMENT);
        campos.setMaximumSize(new Dimension(Integer.MAX_VALUE, 286));
        painelFormulario.add(campos);
        painelFormulario.add(Box.createVerticalStrut(8));

        JPanel botoes = new JPanel(new GridLayout(1, 3, 6, 0));
        botoes.setOpaque(false);
        botaoCadastrar = new JButton("Salvar", new IconeSistema("save", 14, Color.WHITE));
        botaoAlterar = new JButton("Alterar", new IconeSistema("pencil", 14, Color.WHITE));
        botaoExcluir = new JButton("Excluir", new IconeSistema("trash", 14, Color.WHITE));
        botaoLimpar = new JButton("Limpar", new IconeSistema("eraser", 14, Color.WHITE));
        estilizarBotao(botaoCadastrar, azul);
        estilizarBotao(botaoAlterar, new Color(71, 85, 105));
        estilizarBotao(botaoExcluir, new Color(220, 38, 38));
        estilizarBotao(botaoLimpar, new Color(100, 116, 139));
        botoes.add(botaoCadastrar);
        botoes.add(botaoLimpar);
        botoes.add(botaoExcluir);
        botoes.setAlignmentX(Component.LEFT_ALIGNMENT);
        botoes.setPreferredSize(new Dimension(300, 36));
        botoes.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        painelFormulario.add(botoes);
        painelFormulario.add(Box.createVerticalGlue());

        painelLista = new JPanel(new BorderLayout(0, 12));
        painelLista.setOpaque(true);
        painelLista.setBackground(Color.WHITE);
        painelLista.setBorder(bordaCartao(new Color(226, 232, 240), 12, 12, 9, 12));
        JLabel tituloLista = new JLabel("Lista de projetos");
        tituloLista.setFont(new Font("SansSerif", Font.BOLD, 14));
        JLabel dicaOrdenacao = new JLabel("Clique no cabeçalho para ordenar");
        dicaOrdenacao.setFont(new Font("SansSerif", Font.PLAIN, 9));
        JPanel topoLista = new JPanel(new BorderLayout());
        topoLista.setOpaque(false);
        topoLista.add(tituloLista, BorderLayout.WEST);
        topoLista.add(dicaOrdenacao, BorderLayout.EAST);
        JPanel cabecalhoLista = new JPanel(new BorderLayout(0, 6));
        cabecalhoLista.setOpaque(false);
        cabecalhoLista.add(topoLista, BorderLayout.NORTH);
        cabecalhoLista.add(criarBarraBusca(), BorderLayout.SOUTH);
        painelLista.add(cabecalhoLista, BorderLayout.NORTH);

        modelo = new DefaultTableModel(new Object[]{"ID", "Nome", "Categoria", "Status", "Ações"}, 0) {
            @Override public boolean isCellEditable(int linha, int coluna) { return false; }
        };
        tabela = new JTable(modelo);
        tabela.setRowHeight(30);
        tabela.setFont(new Font("SansSerif", Font.PLAIN, 11));
        tabela.setFillsViewportHeight(true);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setAutoCreateRowSorter(false);
        tabela.getTableHeader().setReorderingAllowed(false);
        tabela.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 10));
        tabela.getColumnModel().getColumn(0).setPreferredWidth(32);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(150);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(112);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(58);
        tabela.getColumnModel().getColumn(4).setMinWidth(54);
        tabela.getColumnModel().getColumn(4).setMaxWidth(64);
        tabela.getColumnModel().getColumn(3).setCellRenderer(new StatusRenderer());
        tabela.getColumnModel().getColumn(4).setCellRenderer(new AcoesRenderer());
        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        painelLista.add(scroll, BorderLayout.CENTER);

        JPanel paginacao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        paginacao.setOpaque(false);
        botaoAnterior = new JButton("‹ Anterior");
        botaoProxima = new JButton("Próxima ›");
        labelPagina = new JLabel("Página 1 de 1");
        estilizarBotao(botaoAnterior, new Color(100, 116, 139));
        estilizarBotao(botaoProxima, new Color(100, 116, 139));
        paginacao.add(botaoAnterior);
        paginacao.add(labelPagina);
        paginacao.add(botaoProxima);
        painelLista.add(paginacao, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelFormulario, painelLista);
        // Divisória própria: evita o traço branco padrão do Look and Feel no tema escuro.
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(java.awt.Graphics g) {
                        g.setColor(temaEscuro ? new Color(43, 60, 82) : new Color(226, 232, 240));
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });
        split.setResizeWeight(0.42);
        split.setPreferredSize(new Dimension(900, 390));
        split.setAlignmentX(Component.LEFT_ALIGNMENT);
        split.setDividerSize(4);
        SwingUtilities.invokeLater(() -> split.setDividerLocation(0.42));
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setContinuousLayout(true);
        return split;
    }

    private JPanel criarCampo(String rotulo, Component campo) {
        JPanel painel = new JPanel(new BorderLayout(0, 5));
        painel.setOpaque(false);
        JLabel label = new JLabel(rotulo);
        label.setFont(new Font("SansSerif", Font.BOLD, 11));
        painel.add(label, BorderLayout.NORTH);
        painel.add(campo, BorderLayout.CENTER);
        painel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));
        return painel;
    }

    private void estilizarBotao(JButton botao, Color cor) {
        botao.putClientProperty("corBotaoBase", cor);
        if (!botoesSolidosTematicos.contains(botao)) botoesSolidosTematicos.add(botao);
        botao.setBackground(cor);
        botao.setForeground(Color.WHITE);
        botao.setFont(new Font("SansSerif", Font.BOLD, 11));
        botao.setIconTextGap(6);
        botao.setFocusPainted(false);
        botao.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
        botao.setOpaque(true);
    }

    private void criarEventos() {
        botaoAtualizar.addActionListener(e -> carregarTabela());
        botaoLimpar.addActionListener(e -> limparFormulario());
        botaoCadastrar.addActionListener(e -> salvar());
        botaoAlterar.addActionListener(e -> alterar());
        botaoExcluir.addActionListener(e -> excluir());
        botaoAnterior.addActionListener(e -> {
            if (paginaAtual > 0) {
                paginaAtual--;
                atualizarTabelaVisivel();
            }
        });
        botaoProxima.addActionListener(e -> {
            if ((paginaAtual + 1) * itensPorPagina < projetosFiltrados.size()) {
                paginaAtual++;
                atualizarTabelaVisivel();
            }
        });
        campoBusca.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { pesquisar(); }
            public void removeUpdate(DocumentEvent e) { pesquisar(); }
            public void changedUpdate(DocumentEvent e) { pesquisar(); }
        });
        comboFiltro.addActionListener(e -> pesquisar());
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) selecionarProjeto();
        });
        tabela.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int linha = tabela.rowAtPoint(e.getPoint());
                int coluna = tabela.columnAtPoint(e.getPoint());
                if (linha < 0 || coluna != 4 || linha >= projetosPagina.size()) return;
                Projeto projeto = projetosPagina.get(linha);
                selecionarProjeto(projeto);
                java.awt.Rectangle celula = tabela.getCellRect(linha, coluna, true);
                int posicao = e.getX() - celula.x;
                if (posicao >= celula.width / 2) excluir();
                else mensagem.setText("Projeto selecionado. Faça as alterações e clique em Salvar.");
            }
        });
        tabela.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int coluna = tabela.columnAtPoint(e.getPoint());
                if (coluna < 0 || coluna == 4) return;
                if (coluna == colunaOrdenada) ordemAscendente = !ordemAscendente;
                else {
                    colunaOrdenada = coluna;
                    ordemAscendente = true;
                }
                ordenarProjetos();
                atualizarTabelaVisivel();
            }
        });
    }

    private void carregarTabela() {
        executar("Carregando projetos da API...", api::listar, projetos -> {
            todosProjetos.clear();
            todosProjetos.addAll(projetos);
            pesquisar();
            mensagem.setText("Dados atualizados. " + todosProjetos.size() + " projeto(s) encontrado(s).");
            if (labelStatusApi != null) labelStatusApi.setText("API conectada. " + todosProjetos.size() + " projeto(s) disponível(is).");
        });
    }

    private void pesquisar() {
        if (campoBusca == null || comboFiltro == null) return;
        String termo = campoBusca.getText().trim().toLowerCase(Locale.ROOT);
        String tipo = String.valueOf(comboFiltro.getSelectedItem());
        projetosFiltrados.clear();
        for (Projeto p : todosProjetos) {
            String nome = seguro(p.getNome()).toLowerCase(Locale.ROOT);
            String categoria = seguro(p.getCategoria()).toLowerCase(Locale.ROOT);
            String status = seguro(p.getStatus()).toLowerCase(Locale.ROOT);
            boolean corresponde;
            switch (tipo) {
                case "Nome": corresponde = nome.contains(termo); break;
                case "Categoria": corresponde = categoria.contains(termo); break;
                case "Status": corresponde = status.contains(termo); break;
                default: corresponde = nome.contains(termo) || categoria.contains(termo) || status.contains(termo);
            }
            if (corresponde) projetosFiltrados.add(p);
        }
        ordenarProjetos();
        paginaAtual = 0;
        atualizarTabelaVisivel();
        atualizarCards();
    }

    private void ordenarProjetos() {
        Comparator<Projeto> comparador;
        switch (colunaOrdenada) {
            case 0: comparador = Comparator.comparingInt(Projeto::getId); break;
            case 2: comparador = Comparator.comparing(p -> seguro(p.getCategoria()), String.CASE_INSENSITIVE_ORDER); break;
            case 3: comparador = Comparator.comparing(p -> seguro(p.getStatus()), String.CASE_INSENSITIVE_ORDER); break;
            default: comparador = Comparator.comparing(p -> seguro(p.getNome()), String.CASE_INSENSITIVE_ORDER);
        }
        if (!ordemAscendente) comparador = comparador.reversed();
        projetosFiltrados.sort(comparador);
    }

    private void atualizarTabelaVisivel() {
        if (modelo == null) return;
        modelo.setRowCount(0);
        projetosPagina.clear();
        int inicio = paginaAtual * itensPorPagina;
        int fim = Math.min(inicio + itensPorPagina, projetosFiltrados.size());
        for (int i = inicio; i < fim; i++) {
            Projeto p = projetosFiltrados.get(i);
            projetosPagina.add(p);
            modelo.addRow(new Object[]{p.getId(), p.getNome(), p.getCategoria(), p.getStatus(), ""});
        }
        int totalPaginas = Math.max(1, (int) Math.ceil(projetosFiltrados.size() / (double) itensPorPagina));
        if (paginaAtual >= totalPaginas) paginaAtual = totalPaginas - 1;
        labelPagina.setText("Página " + (paginaAtual + 1) + " de " + totalPaginas);
        botaoAnterior.setEnabled(!ocupado && paginaAtual > 0);
        botaoProxima.setEnabled(!ocupado && paginaAtual + 1 < totalPaginas);
        tabela.clearSelection();
        idSelecionado = -1;
    }

    private void atualizarCards() {
        valorTotal.setText(String.valueOf(todosProjetos.size()));
        long concluidos = todosProjetos.stream().filter(p -> "Concluído".equalsIgnoreCase(seguro(p.getStatus()))).count();
        long ativos = todosProjetos.stream().filter(p -> "Em Desenvolvimento".equalsIgnoreCase(seguro(p.getStatus()))
                || "Planejado".equalsIgnoreCase(seguro(p.getStatus()))).count();
        long cancelados = todosProjetos.stream().filter(p -> "Cancelado".equalsIgnoreCase(seguro(p.getStatus()))).count();
        valorConcluidos.setText(String.valueOf(concluidos));
        valorAndamento.setText(String.valueOf(ativos));
        valorPlanejados.setText(String.valueOf(cancelados));
        atualizarContagensCategorias();
        atualizarAtividadeRecente();
        if (graficosRelatorios != null) graficosRelatorios.setProjetos(todosProjetos);
    }

    private void selecionarProjeto() {
        int linha = tabela.getSelectedRow();
        if (linha < 0 || linha >= projetosPagina.size()) return;
        selecionarProjeto(projetosPagina.get(linha));
    }

    private void selecionarProjeto(Projeto p) {
        idSelecionado = p.getId();
        campoNome.setText(seguro(p.getNome()));
        campoDescricao.setText(seguro(p.getDescricao()));
        comboCategoria.setSelectedItem(seguro(p.getCategoria()));
        comboStatus.setSelectedItem(normalizarStatus(p.getStatus()));
        campoDataCriacao.setText(dataParaTela(p.getDataCriacao()));
        campoPrazo.setText(dataParaTela(p.getPrazo()));
        botaoCadastrar.setText("Salvar alteração");
    }

    private void salvar() {
        if (idSelecionado > 0) alterar();
        else cadastrar();
    }

    private void limparFormulario() {
        campoNome.setText("");
        campoDescricao.setText("");
        campoDataCriacao.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        campoPrazo.setText("");
        if (comboCategoria.getItemCount() > 0) comboCategoria.setSelectedIndex(0);
        comboStatus.setSelectedItem("Planejado");
        idSelecionado = -1;
        if (botaoCadastrar != null) botaoCadastrar.setText("Salvar");
        if (tabela != null) tabela.clearSelection();
        if (campoNome != null) campoNome.requestFocusInWindow();
    }

    private Projeto lerFormulario(int id) {
        String nome = campoNome.getText();
        String descricao = campoDescricao.getText();
        String categoria = String.valueOf(comboCategoria.getSelectedItem());
        String status = String.valueOf(comboStatus.getSelectedItem());
        Projeto projeto = new Projeto(id, nome, descricao, categoria, status,
                campoDataCriacao.getText().trim(), campoPrazo.getText().trim());
        try {
            ProjetoService.validarProjeto(projeto);
            return projeto;
        } catch (IllegalArgumentException e) {
            LogService.erro("Validação no Swing: " + e.getMessage());
            mostrarValidacao(e.getMessage());
            if (e.getMessage().toLowerCase(Locale.ROOT).contains("nome")) campoNome.requestFocusInWindow();
            else if (e.getMessage().toLowerCase(Locale.ROOT).contains("descrição")) campoDescricao.requestFocusInWindow();
            else if (e.getMessage().toLowerCase(Locale.ROOT).contains("categoria")) comboCategoria.requestFocusInWindow();
            else if (e.getMessage().toLowerCase(Locale.ROOT).contains("status")) comboStatus.requestFocusInWindow();
            else campoPrazo.requestFocusInWindow();
            return null;
        }
    }

    private String dataParaTela(String data) {
        if (data == null || data.isBlank()) return "";
        try {
            return java.time.LocalDate.parse(data).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return data;
        }
    }

    private void cadastrar() {
        if (ocupado) return;
        Projeto projeto = lerFormulario(0);
        if (projeto == null) return;
        executar("Cadastrando projeto...", () -> api.cadastrar(projeto), cadastrado -> {
            mensagem.setText("Projeto criado com sucesso! ID: " + cadastrado.getId());
            limparFormulario();
            carregarTabela();
        });
    }

    private void alterar() {
        if (ocupado) return;
        if (idSelecionado == -1) {
            mostrarValidacao("Selecione um projeto na tabela antes de alterar.");
            return;
        }
        Projeto projeto = lerFormulario(idSelecionado);
        if (projeto == null) return;
        executar("Salvando alterações...", () -> api.alterar(projeto), alterado -> {
            if (alterado == null) {
                mensagem.setText("O projeto não foi encontrado na API.");
            } else {
                mensagem.setText("Projeto alterado com sucesso!");
                limparFormulario();
                carregarTabela();
            }
        });
    }

    private void excluir() {
        if (ocupado) return;
        if (idSelecionado == -1) {
            mostrarValidacao("Selecione um projeto na tabela antes de excluir.");
            return;
        }
        int id = idSelecionado;
        int resposta = JOptionPane.showConfirmDialog(this,
                "Deseja realmente excluir o projeto de ID " + id + "?",
                "Confirmar exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) return;
        executar("Excluindo projeto...", () -> api.excluir(id), excluido -> {
            mensagem.setText(excluido ? "Projeto excluído com sucesso." : "Projeto não encontrado.");
            limparFormulario();
            carregarTabela();
        });
    }

    private <T> void executar(String texto, Callable<T> operacao, Consumer<T> sucesso) {
        if (ocupado) return;
        definirOcupado(true);
        mensagem.setText(texto);
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return operacao.call(); }
            @Override protected void done() {
                definirOcupado(false);
                try {
                    sucesso.accept(get());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    mostrarErro(e);
                } catch (ExecutionException e) {
                    mostrarErro(e.getCause() == null ? e : e.getCause());
                } catch (RuntimeException e) {
                    mostrarErro(e);
                }
            }
        }.execute();
    }

    private void definirOcupado(boolean valor) {
        ocupado = valor;
        botaoCadastrar.setEnabled(!valor);
        botaoAlterar.setEnabled(!valor);
        botaoExcluir.setEnabled(!valor);
        botaoLimpar.setEnabled(!valor);
        botaoAtualizar.setEnabled(!valor);
        campoNome.setEnabled(!valor);
        campoDescricao.setEnabled(!valor);
        campoDataCriacao.setEnabled(false);
        campoPrazo.setEnabled(!valor);
        comboCategoria.setEnabled(!valor);
        comboStatus.setEnabled(!valor);
        campoBusca.setEnabled(!valor);
        comboFiltro.setEnabled(!valor);
        tabela.setEnabled(!valor);
        botaoTema.setEnabled(true);
        atualizarTabelaPaginacaoSemReset();
    }

    private void atualizarTabelaPaginacaoSemReset() {
        if (botaoAnterior != null && labelPagina != null) {
            int totalPaginas = Math.max(1, (int) Math.ceil(projetosFiltrados.size() / (double) itensPorPagina));
            botaoAnterior.setEnabled(!ocupado && paginaAtual > 0);
            botaoProxima.setEnabled(!ocupado && paginaAtual + 1 < totalPaginas);
        }
    }

    private void mostrarErro(Throwable erro) {
        LogService.erro("Erro na interface Swing: " + (erro.getMessage() == null ? erro.getClass().getSimpleName() : erro.getMessage()));
        String detalhe = erro.getMessage();
        if (detalhe == null || detalhe.isBlank()) detalhe = erro.getClass().getSimpleName();
        mensagem.setText("Erro de conexão. Confira se a API está executando em http://localhost:7070.");
        if (labelStatusApi != null) {
            labelStatusApi.setText("API indisponível. Confira o servidor em localhost:7070.");
            labelStatusApi.putClientProperty("corStatus", "erro");
            labelStatusApi.setForeground(temaEscuro ? new Color(248, 113, 113) : new Color(220, 38, 38));
        }
        JOptionPane.showMessageDialog(this,
                "Não foi possível concluir a operação. Verifique a conexão com a API.\n\nDetalhes: " + detalhe,
                "Erro de conexão", JOptionPane.ERROR_MESSAGE);
    }

    private void mostrarValidacao(String texto) {
        mensagem.setText(texto);
        JOptionPane.showMessageDialog(this, texto, "Verifique os dados", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarRelatorio() {
        mostrarPagina("RELATORIOS");
    }

    private void atualizarIconeTema() {
        if (botaoTema != null) {
            botaoTema.setIcon(new IconeSistema(temaEscuro ? "sun" : "moon", 16, Color.WHITE));
            botaoTema.setToolTipText(temaEscuro ? "Ativar tema claro" : "Ativar tema escuro");
        }
        if (comboTemaConfiguracoes != null) {
            String desejado = temaEscuro ? "Escuro" : "Claro";
            if (!desejado.equals(comboTemaConfiguracoes.getSelectedItem())) comboTemaConfiguracoes.setSelectedItem(desejado);
        }
    }

    private void alternarTema() {
        temaEscuro = !temaEscuro;
        preferencias.putBoolean("temaEscuro", temaEscuro);
        aplicarTema();
        atualizarIconeTema();
    }

    private void aplicarTema() {
        if (raiz == null) return;

        // Paleta noturna em camadas: fundo, superfície, superfície elevada e campos.
        Color fundo = temaEscuro ? new Color(11, 18, 32) : new Color(245, 247, 251);
        Color superficie = temaEscuro ? new Color(19, 30, 47) : Color.WHITE;
        Color superficieElevada = temaEscuro ? new Color(25, 39, 59) : new Color(248, 250, 252);
        Color campo = temaEscuro ? new Color(12, 22, 37) : Color.WHITE;
        Color texto = temaEscuro ? new Color(235, 242, 250) : new Color(31, 41, 55);
        Color textoSuave = temaEscuro ? new Color(157, 174, 196) : new Color(100, 116, 139);
        Color borda = temaEscuro ? new Color(43, 60, 82) : new Color(226, 232, 240);
        Color selecao = temaEscuro ? new Color(36, 69, 108) : new Color(219, 234, 254);
        Color cabecalhoTabela = temaEscuro ? new Color(27, 43, 64) : new Color(241, 245, 249);

        raiz.setBackground(fundo);
        painelConteudo.setBackground(fundo);
        painelTopo.setBackground(superficie);
        painelCorpo.setBackground(fundo);
        if (painelSidebar != null) painelSidebar.setBackground(sidebarEscura);

        // Percorre também JSplitPane, JViewport, scrolls e componentes aninhados.
        atualizarCoresComponentes(painelConteudo, texto, textoSuave, superficie,
                superficieElevada, borda, campo, selecao, cabecalhoTabela);

        // Superfícies estruturais e cartões com bordas consistentes nos dois temas.
        painelConteudo.setBackground(fundo);
        painelTopo.setBackground(superficie);
        painelTopo.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borda));
        painelCorpo.setBackground(fundo);
        if (painelCorpo.getParent() instanceof javax.swing.JViewport viewport) {
            viewport.setBackground(fundo);
        }
        if (painelStats != null) painelStats.setOpaque(false);

        for (JPanel cartao : cartoesTematicos) {
            cartao.setOpaque(true);
            cartao.setBackground(superficie);
            cartao.setBorder(bordaCartao(borda, 12, 14, 12, 14));
        }
        for (JPanel cartao : cartoesEstatisticos) {
            cartao.setOpaque(true);
            cartao.setBackground(superficie);
            cartao.setBorder(bordaCartao(borda, 8, 9, 8, 9));
        }
        if (painelAtividadeRecente != null) {
            painelAtividadeRecente.setBackground(superficie);
            painelAtividadeRecente.setBorder(bordaCartao(borda, 12, 14, 12, 14));
        }
        if (painelFormulario != null) {
            painelFormulario.setBackground(superficie);
            painelFormulario.setBorder(bordaCartao(borda, 12, 12, 11, 12));
        }
        if (painelLista != null) {
            painelLista.setBackground(superficie);
            painelLista.setBorder(bordaCartao(borda, 12, 12, 9, 12));
        }
        if (painelBusca != null) painelBusca.setBackground(superficieElevada);

        // Botões preenchidos: mantém as cores semânticas e suaviza os neutros no tema escuro.
        for (JButton botao : botoesSolidosTematicos) {
            Object baseObj = botao.getClientProperty("corBotaoBase");
            if (!(baseObj instanceof Color base)) continue;
            Color atual = base;
            if (temaEscuro && (base.equals(new Color(71, 85, 105))
                    || base.equals(new Color(100, 116, 139)))) {
                atual = new Color(53, 70, 93);
            }
            botao.setBackground(atual);
            botao.setForeground(Color.WHITE);
            botao.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(temaEscuro && atual.equals(new Color(53, 70, 93))
                            ? new Color(70, 91, 119) : atual),
                    BorderFactory.createEmptyBorder(7, 9, 7, 9)));
        }

        // Botões de contorno usam uma cor de destaque mais luminosa no fundo noturno.
        for (JButton botao : botoesContorno) {
            Object corSalva = botao.getClientProperty("corDestaque");
            Color destaque = corSalva instanceof Color ? (Color) corSalva : azul;
            Color corAtual = temaEscuro ? clarearDestaque(destaque) : destaque;
            botao.setBackground(superficie);
            botao.setForeground(corAtual);
            botao.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(corAtual),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        }

        estilizarCampo(campoNome, texto, campo, borda);
        estilizarCampo(campoDescricao, texto, campo, borda);
        estilizarCampo(campoBusca, texto, campo, borda);
        estilizarCampo(campoPrazo, texto, campo, borda);
        estilizarCampo(campoDataCriacao, textoSuave, superficieElevada, borda);
        estilizarComboTema(comboCategoria, texto, superficieElevada, borda);
        estilizarComboTema(comboStatus, texto, superficieElevada, borda);
        estilizarComboTema(comboFiltro, texto, superficieElevada, borda);
        estilizarComboTema(comboTemaConfiguracoes, texto, superficieElevada, borda);
        estilizarComboTema(comboItensPagina, texto, superficieElevada, borda);

        if (mensagem != null) mensagem.setForeground(textoSuave);
        if (labelStatusApi != null) {
            Object status = labelStatusApi.getClientProperty("corStatus");
            if ("sucesso".equals(status)) labelStatusApi.setForeground(temaEscuro ? new Color(74, 222, 128) : new Color(22, 163, 74));
            else if ("erro".equals(status)) labelStatusApi.setForeground(temaEscuro ? new Color(252, 129, 129) : new Color(220, 38, 38));
            else labelStatusApi.setForeground(textoSuave);
        }
        valorTotal.setForeground(temaEscuro ? new Color(125, 177, 255) : azul);
        valorConcluidos.setForeground(temaEscuro ? new Color(91, 225, 151) : verde);
        valorAndamento.setForeground(temaEscuro ? new Color(255, 198, 92) : amarelo);
        valorPlanejados.setForeground(temaEscuro ? new Color(194, 150, 255) : roxo);

        if (graficosRelatorios != null) graficosRelatorios.setTemaEscuro(temaEscuro);
        atualizarIconeTema();
        raiz.revalidate();
        raiz.repaint();
    }

    private Color clarearDestaque(Color cor) {
        if (cor.equals(azul)) return new Color(112, 174, 255);
        if (cor.equals(verde)) return new Color(91, 225, 151);
        if (cor.equals(amarelo)) return new Color(255, 190, 92);
        if (cor.equals(roxo)) return new Color(194, 150, 255);
        if (cor.getRed() > 180 && cor.getGreen() < 100) return new Color(255, 133, 145);
        return cor.brighter();
    }

    private void estilizarCampo(JTextField componente, Color texto, Color fundo, Color borda) {
        if (componente == null) return;
        componente.setForeground(texto);
        componente.setBackground(fundo);
        componente.setCaretColor(texto);
        componente.setSelectionColor(temaEscuro ? new Color(37, 99, 235) : new Color(191, 219, 254));
        componente.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borda),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    private void estilizarCampo(JTextArea componente, Color texto, Color fundo, Color borda) {
        if (componente == null) return;
        componente.setForeground(texto);
        componente.setBackground(fundo);
        componente.setCaretColor(texto);
        componente.setSelectionColor(temaEscuro ? new Color(37, 99, 235) : new Color(191, 219, 254));
        componente.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    private void estilizarComboTema(JComboBox<?> combo, Color texto, Color superficie, Color borda) {
        if (combo == null) return;
        combo.setForeground(texto);
        combo.setBackground(superficie);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borda),
                BorderFactory.createEmptyBorder(2, 5, 2, 5)));
    }

    private void atualizarCoresComponentes(Component componente, Color texto, Color textoSuave,
                                           Color superficie, Color superficieElevada, Color borda,
                                           Color campo, Color selecao, Color cabecalhoTabela) {
        if (componente == null) return;

        if (componente instanceof JPanel painel && painel.isOpaque()) {
            painel.setBackground(superficie);
        }
        if (componente instanceof JLabel label) {
            label.setForeground(Boolean.TRUE.equals(label.getClientProperty("textoSecundario"))
                    ? textoSuave : texto);
        }
        if (componente instanceof JTextField campoTexto) {
            campoTexto.setForeground(texto);
            campoTexto.setBackground(campo);
            campoTexto.setCaretColor(texto);
            campoTexto.setSelectionColor(temaEscuro ? new Color(37, 99, 235) : new Color(191, 219, 254));
            campoTexto.setSelectedTextColor(Color.WHITE);
        } else if (componente instanceof JTextArea areaTexto) {
            areaTexto.setForeground(texto);
            areaTexto.setBackground(campo);
            areaTexto.setCaretColor(texto);
            areaTexto.setSelectionColor(temaEscuro ? new Color(37, 99, 235) : new Color(191, 219, 254));
            areaTexto.setSelectedTextColor(Color.WHITE);
        }
        if (componente instanceof JComboBox<?> combo) {
            combo.setForeground(texto);
            combo.setBackground(superficieElevada);
        }
        if (componente instanceof JTable tabelaAtual) {
            tabelaAtual.setOpaque(true);
            tabelaAtual.setBackground(superficie);
            tabelaAtual.setForeground(texto);
            tabelaAtual.setGridColor(borda);
            tabelaAtual.setSelectionBackground(selecao);
            tabelaAtual.setSelectionForeground(texto);
            tabelaAtual.setFillsViewportHeight(true);
            tabelaAtual.setShowGrid(true);
            tabelaAtual.setIntercellSpacing(new Dimension(1, 1));
            javax.swing.table.JTableHeader header = tabelaAtual.getTableHeader();
            if (header != null) {
                header.setOpaque(true);
                header.setBackground(cabecalhoTabela);
                header.setForeground(texto);
                header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borda));
                DefaultTableCellRenderer renderizadorCabecalho = new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable tabela, Object valor,
                            boolean selecionado, boolean foco, int linha, int coluna) {
                        JLabel label = (JLabel) super.getTableCellRendererComponent(
                                tabela, valor, selecionado, foco, linha, coluna);
                        label.setOpaque(true);
                        label.setBackground(cabecalhoTabela);
                        label.setForeground(texto);
                        label.setFont(header.getFont().deriveFont(Font.BOLD, 10f));
                        label.setHorizontalAlignment(SwingConstants.CENTER);
                        label.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createMatteBorder(0, 0, 1, 1, borda),
                                BorderFactory.createEmptyBorder(5, 7, 5, 7)));
                        return label;
                    }
                };
                header.setDefaultRenderer(renderizadorCabecalho);
            }
        }
        if (componente instanceof JScrollPane scroll) {
            scroll.setBackground(superficie);
            scroll.getViewport().setBackground(superficie);
            scroll.setBorder(BorderFactory.createLineBorder(borda));
            scroll.getVerticalScrollBar().setBackground(superficie);
            scroll.getHorizontalScrollBar().setBackground(superficie);
        }
        if (componente instanceof javax.swing.JViewport viewport) {
            viewport.setBackground(superficie);
        }
        if (componente instanceof JSplitPane split) {
            split.setBackground(borda);
            split.setBorder(BorderFactory.createEmptyBorder());
            if (split.getUI() instanceof javax.swing.plaf.basic.BasicSplitPaneUI ui
                    && ui.getDivider() != null) {
                ui.getDivider().setBackground(borda);
                ui.getDivider().setBorder(BorderFactory.createEmptyBorder());
            }
        }

        // Atravessa JSplitPane, Box, JViewport, painéis e qualquer outro Container.
        if (componente instanceof Container container) {
            for (Component filho : container.getComponents()) {
                atualizarCoresComponentes(filho, texto, textoSuave, superficie,
                        superficieElevada, borda, campo, selecao, cabecalhoTabela);
            }
        }
    }

    private String normalizarStatus(String status) {
        String informado = seguro(status);
        if (informado.equalsIgnoreCase("Em desenvolvimento") || informado.equalsIgnoreCase("Em andamento")) return "Em Desenvolvimento";
        for (String valido : STATUS_VALIDOS) if (valido.equalsIgnoreCase(informado)) return valido;
        return "Planejado";
    }

    private boolean ehStatusValido(String status) {
        for (String valido : STATUS_VALIDOS) if (valido.equalsIgnoreCase(seguro(status))) return true;
        return false;
    }

    private String seguro(String texto) { return texto == null ? "" : texto; }

    private class StatusRenderer extends JPanel implements TableCellRenderer {
        private final JLabel badge = new JLabel();

        StatusRenderer() {
            setLayout(new java.awt.GridBagLayout());
            setOpaque(true);
            badge.setOpaque(true);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            badge.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            badge.setFont(new Font("SansSerif", Font.BOLD, 9));
            add(badge);
        }

        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selecionado, boolean foco, int linha, int coluna) {
            String status = String.valueOf(value);
            setBackground(selecionado ? table.getSelectionBackground() : table.getBackground());
            if ("Concluído".equalsIgnoreCase(status)) {
                badge.setBackground(temaEscuro ? new Color(20, 65, 52) : new Color(220, 252, 231));
                badge.setForeground(temaEscuro ? new Color(134, 239, 172) : new Color(22, 101, 52));
            } else if ("Em desenvolvimento".equalsIgnoreCase(status)) {
                badge.setBackground(temaEscuro ? new Color(76, 57, 22) : new Color(254, 243, 199));
                badge.setForeground(temaEscuro ? new Color(253, 224, 132) : new Color(146, 64, 14));
            } else if ("Cancelado".equalsIgnoreCase(status)) {
                badge.setBackground(temaEscuro ? new Color(75, 37, 46) : new Color(254, 226, 226));
                badge.setForeground(temaEscuro ? new Color(253, 164, 175) : new Color(153, 27, 27));
            } else if ("Planejado".equalsIgnoreCase(status)) {
                badge.setBackground(temaEscuro ? new Color(54, 41, 78) : new Color(237, 233, 254));
                badge.setForeground(temaEscuro ? new Color(216, 180, 254) : new Color(91, 33, 182));
            } else {
                badge.setBackground(temaEscuro ? new Color(76, 57, 22) : new Color(254, 243, 199));
                badge.setForeground(temaEscuro ? new Color(253, 224, 132) : new Color(146, 64, 14));
            }
            badge.setText(status);
            return this;
        }
    }

    private class AcoesRenderer extends JPanel implements TableCellRenderer {
        private final JLabel editar = new JLabel(new IconeSistema("pencil", 14, new Color(22, 116, 209)));
        private final JLabel excluir = new JLabel(new IconeSistema("trash", 14, new Color(220, 38, 38)));

        AcoesRenderer() {
            setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
            setOpaque(true);
            add(editar);
            add(excluir);
            editar.setToolTipText("Editar projeto");
            excluir.setToolTipText("Excluir projeto");
        }

        @Override public Component getTableCellRendererComponent(JTable table, Object value,
                boolean selecionado, boolean foco, int linha, int coluna) {
            setBackground(selecionado ? table.getSelectionBackground() : table.getBackground());
            return this;
        }
    }

    private void sair() {
        int resposta = JOptionPane.showConfirmDialog(this,
                "Deseja encerrar sua sessão?", "Sair do sistema",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (resposta != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                new AuthApiClient().sair(authToken);
                return null;
            }

            @Override protected void done() {
                dispose();
                new TelaLogin().setVisible(true);
            }
        }.execute();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}
