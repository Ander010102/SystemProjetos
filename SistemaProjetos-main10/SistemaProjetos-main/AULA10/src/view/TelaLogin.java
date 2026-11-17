package view;

import client.AuthApiClient;
import client.AuthApiClient.Sessao;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.Callable;

/** Tela inicial: permite entrar ou criar uma conta antes de abrir o Swing. */
public class TelaLogin extends JFrame {

    private final AuthApiClient authApi = new AuthApiClient();
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel painelFormularios = new JPanel(cardLayout);
    private final JLabel mensagem = new JLabel("Entre com sua conta para continuar.");

    private JTextField loginEmail;
    private JPasswordField loginSenha;
    private JTextField cadastroNome;
    private JTextField cadastroEmail;
    private JPasswordField cadastroSenha;
    private JButton botaoEntrar;
    private JButton botaoCadastrar;

    public TelaLogin() {
        setTitle("Sistema de Projetos - Acesso");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 590);
        setMinimumSize(new Dimension(820, 540));
        setLocationRelativeTo(null);
        setContentPane(criarInterface());
    }

    private JPanel criarInterface() {
        JPanel raiz = new JPanel(new GridBagLayout());
        raiz.setBackground(new Color(239, 243, 249));

        JPanel cartao = new JPanel(new BorderLayout());
        cartao.setPreferredSize(new Dimension(780, 470));
        cartao.setBackground(Color.WHITE);
        cartao.setBorder(BorderFactory.createLineBorder(new Color(222, 229, 239)));

        JPanel painelMarca = new JPanel();
        painelMarca.setBackground(new Color(16, 27, 51));
        painelMarca.setPreferredSize(new Dimension(300, 470));
        painelMarca.setBorder(BorderFactory.createEmptyBorder(42, 32, 32, 32));
        painelMarca.setLayout(new BoxLayout(painelMarca, BoxLayout.Y_AXIS));

        JLabel marca = new JLabel("▦  SISTEMA DE PROJETOS");
        marca.setForeground(Color.WHITE);
        marca.setFont(new Font("SansSerif", Font.BOLD, 18));
        marca.setAlignmentX(LEFT_ALIGNMENT);
        painelMarca.add(marca);
        painelMarca.add(Box.createVerticalStrut(72));
        JLabel tituloMarca = new JLabel("Seus projetos,");
        tituloMarca.setForeground(Color.WHITE);
        tituloMarca.setFont(new Font("SansSerif", Font.BOLD, 27));
        tituloMarca.setAlignmentX(LEFT_ALIGNMENT);
        painelMarca.add(tituloMarca);
        JLabel tituloMarca2 = new JLabel("em um só lugar.");
        tituloMarca2.setForeground(new Color(147, 197, 253));
        tituloMarca2.setFont(new Font("SansSerif", Font.BOLD, 27));
        tituloMarca2.setAlignmentX(LEFT_ALIGNMENT);
        painelMarca.add(tituloMarca2);
        painelMarca.add(Box.createVerticalStrut(16));
        JLabel descricao = new JLabel("<html>Entre para organizar, acompanhar<br>e gerenciar seus projetos.</html>");
        descricao.setForeground(new Color(203, 213, 225));
        descricao.setFont(new Font("SansSerif", Font.PLAIN, 13));
        descricao.setAlignmentX(LEFT_ALIGNMENT);
        painelMarca.add(descricao);
        painelMarca.add(Box.createVerticalGlue());
        JLabel rodape = new JLabel("ACESSO SEGURO • SWING E WEB");
        rodape.setForeground(new Color(148, 163, 184));
        rodape.setFont(new Font("SansSerif", Font.BOLD, 9));
        rodape.setAlignmentX(LEFT_ALIGNMENT);
        painelMarca.add(rodape);

        JPanel painelDireito = new JPanel(new BorderLayout());
        painelDireito.setBackground(Color.WHITE);
        painelDireito.setBorder(BorderFactory.createEmptyBorder(42, 48, 32, 48));
        mensagem.setFont(new Font("SansSerif", Font.PLAIN, 11));
        mensagem.setForeground(new Color(100, 116, 139));
        painelDireito.add(painelFormularios, BorderLayout.CENTER);
        painelDireito.add(mensagem, BorderLayout.SOUTH);

        painelFormularios.add(criarFormularioLogin(), "login");
        painelFormularios.add(criarFormularioCadastro(), "cadastro");
        cardLayout.show(painelFormularios, "login");

        cartao.add(painelMarca, BorderLayout.WEST);
        cartao.add(painelDireito, BorderLayout.CENTER);
        raiz.add(cartao);
        return raiz;
    }

    private JPanel criarFormularioLogin() {
        JPanel painel = formularioBase();
        adicionarTitulo(painel, "Bem-vindo de volta", "Acesse sua conta para abrir o painel.");

        loginEmail = new JTextField();
        loginSenha = new JPasswordField();
        adicionarCampo(painel, "E-mail", loginEmail);
        adicionarCampo(painel, "Senha", loginSenha);

        botaoEntrar = criarBotao("Entrar", new Color(37, 99, 235));
        botaoEntrar.addActionListener(e -> entrar());
        painel.add(botaoEntrar);

        JButton irCadastro = criarBotaoTexto("Ainda não tem uma conta? Criar cadastro");
        irCadastro.addActionListener(e -> mostrarCadastro());
        painel.add(Box.createVerticalStrut(12));
        painel.add(irCadastro);
        return painel;
    }

    private JPanel criarFormularioCadastro() {
        JPanel painel = formularioBase();
        adicionarTitulo(painel, "Criar sua conta", "Cadastre-se uma vez para usar o Swing e a Web.");

        cadastroNome = new JTextField();
        cadastroEmail = new JTextField();
        cadastroSenha = new JPasswordField();
        adicionarCampo(painel, "Nome completo", cadastroNome);
        adicionarCampo(painel, "E-mail", cadastroEmail);
        adicionarCampo(painel, "Senha (mínimo de 8 caracteres)", cadastroSenha);

        botaoCadastrar = criarBotao("Criar conta e entrar", new Color(37, 99, 235));
        botaoCadastrar.addActionListener(e -> cadastrar());
        painel.add(botaoCadastrar);

        JButton irLogin = criarBotaoTexto("Já tem uma conta? Fazer login");
        irLogin.addActionListener(e -> mostrarLogin());
        painel.add(Box.createVerticalStrut(12));
        painel.add(irLogin);
        return painel;
    }

    private JPanel formularioBase() {
        JPanel painel = new JPanel();
        painel.setBackground(Color.WHITE);
        painel.setLayout(new BoxLayout(painel, BoxLayout.Y_AXIS));
        painel.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
        return painel;
    }

    private void adicionarTitulo(JPanel painel, String titulo, String subtitulo) {
        JLabel labelTitulo = new JLabel(titulo);
        labelTitulo.setFont(new Font("SansSerif", Font.BOLD, 25));
        labelTitulo.setForeground(new Color(23, 32, 51));
        labelTitulo.setAlignmentX(LEFT_ALIGNMENT);
        painel.add(labelTitulo);
        JLabel labelSubtitulo = new JLabel(subtitulo);
        labelSubtitulo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelSubtitulo.setForeground(new Color(112, 124, 145));
        labelSubtitulo.setAlignmentX(LEFT_ALIGNMENT);
        painel.add(Box.createVerticalStrut(6));
        painel.add(labelSubtitulo);
        painel.add(Box.createVerticalStrut(28));
    }

    private void adicionarCampo(JPanel painel, String rotulo, JTextField campo) {
        JLabel label = new JLabel(rotulo);
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(new Color(48, 59, 78));
        label.setAlignmentX(LEFT_ALIGNMENT);
        campo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        campo.setPreferredSize(new Dimension(300, 40));
        campo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(218, 226, 238)),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        painel.add(label);
        painel.add(Box.createVerticalStrut(7));
        painel.add(campo);
        painel.add(Box.createVerticalStrut(17));
    }

    private JButton criarBotao(String texto, Color cor) {
        JButton botao = new JButton(texto);
        botao.setAlignmentX(LEFT_ALIGNMENT);
        botao.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        botao.setPreferredSize(new Dimension(300, 43));
        botao.setBackground(cor);
        botao.setForeground(Color.WHITE);
        botao.setFont(new Font("SansSerif", Font.BOLD, 13));
        botao.setFocusPainted(false);
        botao.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        botao.setOpaque(true);
        return botao;
    }

    private JButton criarBotaoTexto(String texto) {
        JButton botao = new JButton(texto);
        botao.setAlignmentX(LEFT_ALIGNMENT);
        botao.setHorizontalAlignment(SwingConstants.LEFT);
        botao.setForeground(new Color(37, 99, 235));
        botao.setBackground(Color.WHITE);
        botao.setFont(new Font("SansSerif", Font.BOLD, 11));
        botao.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        botao.setFocusPainted(false);
        return botao;
    }

    private void mostrarCadastro() {
        mensagem.setForeground(new Color(100, 116, 139));
        mensagem.setText("Preencha os dados para criar sua conta.");
        cardLayout.show(painelFormularios, "cadastro");
    }

    private void mostrarLogin() {
        mensagem.setForeground(new Color(100, 116, 139));
        mensagem.setText("Entre com sua conta para continuar.");
        cardLayout.show(painelFormularios, "login");
    }

    private void entrar() {
        String email = loginEmail.getText().trim();
        String senha = new String(loginSenha.getPassword());
        executar(() -> authApi.entrar(email, senha));
    }

    private void cadastrar() {
        String nome = cadastroNome.getText().trim();
        String email = cadastroEmail.getText().trim();
        String senha = new String(cadastroSenha.getPassword());
        executar(() -> authApi.cadastrar(nome, email, senha));
    }

    private void executar(Callable<Sessao> operacao) {
        definirBotoes(false);
        mensagem.setText("Conectando ao servidor...");
        new SwingWorker<Sessao, Void>() {
            @Override protected Sessao doInBackground() throws Exception {
                return operacao.call();
            }

            @Override protected void done() {
                definirBotoes(true);
                try {
                    Sessao sessao = get();
                    abrirSistema(sessao);
                } catch (Exception e) {
                    Throwable causa = e.getCause() == null ? e : e.getCause();
                    mensagem.setForeground(new Color(185, 28, 28));
                    mensagem.setText(causa.getMessage() == null
                            ? "Não foi possível acessar a API. Verifique se ela está ativa."
                            : causa.getMessage());
                }
            }
        }.execute();
    }

    private void definirBotoes(boolean habilitado) {
        botaoEntrar.setEnabled(habilitado);
        botaoCadastrar.setEnabled(habilitado);
    }

    private void abrirSistema(Sessao sessao) {
        SwingUtilities.invokeLater(() -> {
            TelaProjetos tela = new TelaProjetos(sessao.token(), sessao.nome());
            tela.setVisible(true);
            dispose();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TelaLogin().setVisible(true));
    }
}
