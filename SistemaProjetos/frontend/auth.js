const AUTH_URL = "http://localhost:7070/api/auth";
const loginView = document.getElementById("loginView");
const registerView = document.getElementById("registerView");
const feedback = document.getElementById("authFeedback");
const loginForm = document.getElementById("loginForm");
const registerForm = document.getElementById("registerForm");

function mostrarFormulario(tipo) {
    const login = tipo === "login";
    loginView.hidden = !login;
    registerView.hidden = login;
    ocultarFeedback();
}

function mostrarFeedback(texto, tipo = "info") {
    feedback.textContent = texto;
    feedback.className = `auth-feedback ${tipo}`;
    feedback.hidden = false;
}

function ocultarFeedback() {
    feedback.hidden = true;
    feedback.textContent = "";
}

async function lerErro(resposta) {
    try {
        const dados = await resposta.json();
        return dados.erro || `Erro HTTP ${resposta.status}.`;
    } catch (_) {
        return `Erro HTTP ${resposta.status}.`;
    }
}

async function autenticar(rota, dados, botao, textoOriginal) {
    botao.disabled = true;
    botao.textContent = "Aguarde...";
    mostrarFeedback("Validando seus dados de acesso...", "info");
    try {
        const resposta = await fetch(`${AUTH_URL}/${rota}`, {
            method: "POST",
            headers: { "Content-Type": "application/json", "Accept": "application/json" },
            body: JSON.stringify(dados)
        });
        if (!resposta.ok) throw new Error(await lerErro(resposta));
        const sessao = await resposta.json();
        if (!sessao.token) throw new Error("O servidor não retornou uma sessão válida.");

        sessionStorage.setItem("sp_access_token", sessao.token);
        sessionStorage.setItem("sp_user", JSON.stringify({ nome: sessao.nome, email: sessao.email }));
        window.location.replace("projetos.html");
    } catch (erro) {
        mostrarFeedback(erro.message || "Não foi possível conectar à API. Verifique se o servidor está ativo.", "error");
    } finally {
        botao.disabled = false;
        botao.textContent = textoOriginal;
    }
}

loginForm.addEventListener("submit", evento => {
    evento.preventDefault();
    const email = document.getElementById("loginEmail").value.trim();
    const senha = document.getElementById("loginSenha").value;
    if (!email || !senha) {
        mostrarFeedback("Informe seu e-mail e sua senha.", "error");
        return;
    }
    autenticar("login", { email, senha }, document.getElementById("btnEntrar"), "Entrar");
});

registerForm.addEventListener("submit", evento => {
    evento.preventDefault();
    const nome = document.getElementById("cadastroNome").value.trim();
    const email = document.getElementById("cadastroEmail").value.trim();
    const senha = document.getElementById("cadastroSenha").value;
    if (nome.length < 2) {
        mostrarFeedback("Informe seu nome completo.", "error");
        return;
    }
    if (!email) {
        mostrarFeedback("Informe um e-mail válido.", "error");
        return;
    }
    if (senha.length < 8) {
        mostrarFeedback("A senha deve possuir pelo menos 8 caracteres.", "error");
        return;
    }
    autenticar("register", { nome, email, senha }, document.getElementById("btnCadastrar"), "Criar conta e entrar");
});

document.getElementById("irCadastro").addEventListener("click", () => mostrarFormulario("cadastro"));
document.getElementById("irLogin").addEventListener("click", () => mostrarFormulario("login"));

try {
    if (sessionStorage.getItem("sp_access_token")) {
        window.location.replace("projetos.html");
    }
} catch (_) { /* o navegador pode bloquear o armazenamento de sessão */ }
