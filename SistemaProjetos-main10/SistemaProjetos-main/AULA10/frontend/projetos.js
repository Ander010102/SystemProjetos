const API_URL = "http://localhost:7070/api/projetos";
const AUTH_URL = "http://localhost:7070/api/auth";
const AUTH_TOKEN = sessionStorage.getItem("sp_access_token");
let ITENS_POR_PAGINA = 6;
const STATUS_VALIDOS = ["Planejado", "Em Desenvolvimento", "Concluído", "Cancelado"];
const CATEGORIAS_VALIDAS = ["Software", "Mobile", "Web", "Desktop"];

if (!AUTH_TOKEN) window.location.replace("login.html");

const formProjeto = document.getElementById("formProjeto");
const listaTabela = document.getElementById("corpoTabela");
const mensagem = document.getElementById("mensagem");
const campoBusca = document.getElementById("campoBusca");
const tipoBusca = document.getElementById("tipoBusca");
const btnSalvar = document.getElementById("btnSalvar");
const btnCancelar = document.getElementById("btnCancelar");
const btnExcluirFormulario = document.getElementById("btnExcluirFormulario");
const tituloFormulario = document.getElementById("tituloFormulario");
const btnTema = document.getElementById("btnTema");
const apiIndicator = document.getElementById("apiIndicator");
const apiStatus = document.getElementById("apiStatus");

let todosProjetos = [];
let projetosFiltrados = [];
let idEmEdicao = null;
let ocupado = false;
let paginaAtual = 0;
let colunaOrdenada = "id";
let ordemAscendente = false;
let temporizadorMensagem = null;

function cabecalhosAutenticados(cabecalhos = {}) {
    return { ...cabecalhos, "Authorization": `Bearer ${AUTH_TOKEN}` };
}

function tratarSessaoInvalida(resposta) {
    if (resposta.status !== 401) return false;
    encerrarSessaoLocal();
    window.location.replace("login.html");
    return true;
}

function encerrarSessaoLocal() {
    sessionStorage.removeItem("sp_access_token");
    sessionStorage.removeItem("sp_user");
}

function atualizarDataHora() {
    const agora = new Date();
    const data = new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "long", year: "numeric" }).format(agora);
    const hora = new Intl.DateTimeFormat("pt-BR", { weekday: "long", hour: "2-digit", minute: "2-digit" }).format(agora);
    document.getElementById("dataAtual").textContent = data;
    document.getElementById("horaAtual").textContent = hora;
}

function carregarUsuarioLogado() {
    let usuario = {};
    try { usuario = JSON.parse(sessionStorage.getItem("sp_user") || "{}"); } catch (_) { usuario = {}; }
    const nome = usuario.nome || "Usuário";
    const iniciais = nome.trim().split(/\s+/).slice(0, 2).map(parte => parte.charAt(0)).join("").toUpperCase() || "US";
    document.getElementById("nomeUsuario").textContent = nome;
    document.getElementById("nomeUsuarioHeader").textContent = nome;
    document.getElementById("avatarUsuario").textContent = iniciais;
    document.getElementById("avatarUsuarioHeader").textContent = iniciais;
    const configNome = document.getElementById("configNomeUsuario");
    const configEmail = document.getElementById("configEmailUsuario");
    if (configNome) configNome.textContent = nome;
    if (configEmail) configEmail.textContent = usuario.email || "Não informado";
}

function definirOcupado(valor) {
    ocupado = valor;
    document.querySelectorAll("#formProjeto input, #formProjeto textarea, #formProjeto select, #formProjeto button, #campoBusca, #tipoBusca, #btnAtualizar, #btnAnterior, #btnProxima")
        .forEach(elemento => { elemento.disabled = valor; });
    btnExcluirFormulario.disabled = valor || idEmEdicao === null;
    atualizarPaginacao();
}

function mostrarMensagem(texto, tipo = "info") {
    if (temporizadorMensagem) clearTimeout(temporizadorMensagem);
    mensagem.textContent = texto;
    mensagem.className = `alert feedback-message ${tipo}`;
    mensagem.hidden = false;
    // Mensagens de sucesso desaparecem sozinhas para não deslocar o dashboard.
    if (tipo === "success") {
        temporizadorMensagem = setTimeout(ocultarMensagem, 3200);
    }
}

function ocultarMensagem() {
    mensagem.hidden = true;
    mensagem.textContent = "";
}

function definirStatusApi(online) {
    apiIndicator.classList.toggle("online", online);
    apiIndicator.classList.toggle("offline", !online);
    apiStatus.textContent = online ? "API conectada" : "API indisponível";
    const indicadorConfig = document.getElementById("apiIndicatorConfig");
    const statusConfig = document.getElementById("apiStatusConfig");
    if (indicadorConfig) {
        indicadorConfig.classList.toggle("online", online);
        indicadorConfig.classList.toggle("offline", !online);
    }
    if (statusConfig) statusConfig.textContent = online ? "API conectada" : "API indisponível";
}

async function lerErro(resposta) {
    try {
        const dados = await resposta.json();
        return dados.erro || dados.mensagem || `Erro HTTP ${resposta.status}.`;
    } catch (_) {
        return `Erro HTTP ${resposta.status}.`;
    }
}

async function listarProjetos(opcoes = {}) {
    const mostrarCarregamento = opcoes.mostrarCarregamento !== false;
    const preservarMensagem = opcoes.preservarMensagem === true;
    if (mostrarCarregamento && !preservarMensagem) mostrarMensagem("Carregando projetos...", "info");

    try {
        const resposta = await fetch(API_URL, { headers: cabecalhosAutenticados({ "Accept": "application/json" }) });
        if (tratarSessaoInvalida(resposta)) return;
        if (!resposta.ok) throw new Error(await lerErro(resposta));
        const dados = await resposta.json();
        if (!Array.isArray(dados)) throw new Error("A API retornou uma lista de projetos inválida.");

        todosProjetos = dados;
        definirStatusApi(true);
        atualizarEstatisticas();
        aplicarFiltros();
        renderizarCategorias();
        renderizarRecentes();
        atualizarResumoIndicadores();
        if (!preservarMensagem && mostrarCarregamento) {
            mostrarMensagem(`Projetos carregados com sucesso. ${todosProjetos.length} registro(s) encontrado(s).`, "success");
        }
    } catch (erro) {
        definirStatusApi(false);
        mostrarMensagem(`Erro de conexão: não foi possível acessar a API em ${API_URL}. Verifique se o servidor Java está em execução. ${erro.message || ""}`, "error");
        if (todosProjetos.length === 0) renderizarTabela([]);
        console.error(erro);
    }
}

function aplicarFiltros() {
    const termo = campoBusca.value.trim().toLocaleLowerCase("pt-BR");
    const tipo = tipoBusca.value;
    projetosFiltrados = todosProjetos.filter(projeto => {
        const nome = texto(projeto.nome).toLocaleLowerCase("pt-BR");
        const categoria = texto(projeto.categoria).toLocaleLowerCase("pt-BR");
        const status = texto(projeto.status).toLocaleLowerCase("pt-BR");
        if (!termo) return true;
        switch (tipo) {
            case "nome": return nome.includes(termo);
            case "categoria": return categoria.includes(termo);
            case "status": return status.includes(termo);
            default: return nome.includes(termo) || categoria.includes(termo) || status.includes(termo);
        }
    });
    ordenarProjetos();
    paginaAtual = 0;
    renderizarTabela(projetosFiltrados);
}

function ordenarProjetos() {
    projetosFiltrados.sort((a, b) => {
        let comparacao;
        if (colunaOrdenada === "id") comparacao = Number(a.id) - Number(b.id);
        else comparacao = texto(a[colunaOrdenada]).localeCompare(texto(b[colunaOrdenada]), "pt-BR", { sensitivity: "base" });
        return ordemAscendente ? comparacao : -comparacao;
    });
    document.querySelectorAll("[data-sort]").forEach(botao => {
        const chave = botao.dataset.sort;
        const seta = botao.querySelector(".sort-indicator i");
        if (seta) {
            seta.className = chave === colunaOrdenada
                ? `bi ${ordemAscendente ? "bi-arrow-up" : "bi-arrow-down"}`
                : "bi bi-arrow-down-up";
        }
    });
}

function renderizarTabela(projetos) {
    listaTabela.replaceChildren();
    const total = projetos.length;
    const totalPaginas = Math.max(1, Math.ceil(total / ITENS_POR_PAGINA));
    if (paginaAtual >= totalPaginas) paginaAtual = totalPaginas - 1;
    const inicio = paginaAtual * ITENS_POR_PAGINA;
    const pagina = projetos.slice(inicio, inicio + ITENS_POR_PAGINA);

    if (pagina.length === 0) {
        const linha = document.createElement("tr");
        const celula = document.createElement("td");
        celula.colSpan = 5;
        celula.className = "table-empty";
        celula.textContent = termoBuscaAtivo() ? "Nenhum projeto corresponde à sua busca." : "Nenhum projeto cadastrado. Use o formulário para adicionar o primeiro projeto.";
        linha.appendChild(celula);
        listaTabela.appendChild(linha);
    } else {
        pagina.forEach(projeto => listaTabela.appendChild(criarLinhaProjeto(projeto)));
    }

    const exibidosInicio = total === 0 ? 0 : inicio + 1;
    const exibidosFim = Math.min(inicio + ITENS_POR_PAGINA, total);
    document.getElementById("resumoPaginacao").textContent = `Mostrando ${exibidosInicio}–${exibidosFim} de ${total} projeto(s)`;
    document.getElementById("paginaAtual").textContent = `${paginaAtual + 1} / ${totalPaginas}`;
    atualizarPaginacao();
}

function criarLinhaProjeto(projeto) {
    const linha = document.createElement("tr");
    const id = document.createElement("td");
    id.className = "project-id";
    id.textContent = String(projeto.id ?? "—").padStart(2, "0");

    const nome = document.createElement("td");
    const nomeTexto = document.createElement("div");
    nomeTexto.className = "project-name";
    nomeTexto.textContent = texto(projeto.nome) || "Sem nome";
    nomeTexto.title = texto(projeto.nome);
    nome.appendChild(nomeTexto);

    const categoria = document.createElement("td");
    categoria.textContent = texto(projeto.categoria) || "—";

    const status = document.createElement("td");
    const badge = document.createElement("span");
    badge.className = `status-badge ${classeStatus(projeto.status)}`;
    badge.textContent = texto(projeto.status) || "Sem status";
    status.appendChild(badge);

    const acoes = document.createElement("td");
    const botoes = document.createElement("div");
    botoes.className = "action-buttons";
    const editar = document.createElement("button");
    editar.type = "button";
    editar.className = "row-action";
    editar.title = `Editar ${texto(projeto.nome)}`;
    editar.setAttribute("aria-label", `Editar ${texto(projeto.nome)}`);
    editar.innerHTML = '<i class="bi bi-pencil-fill" aria-hidden="true"></i>';
    editar.disabled = ocupado;
    editar.addEventListener("click", () => editarProjeto(projeto));

    const excluir = document.createElement("button");
    excluir.type = "button";
    excluir.className = "row-action delete";
    excluir.title = `Excluir ${texto(projeto.nome)}`;
    excluir.setAttribute("aria-label", `Excluir ${texto(projeto.nome)}`);
    excluir.innerHTML = '<i class="bi bi-trash-fill" aria-hidden="true"></i>';
    excluir.disabled = ocupado;
    excluir.addEventListener("click", () => excluirProjeto(projeto.id));

    botoes.append(editar, excluir);
    acoes.appendChild(botoes);
    linha.append(id, nome, categoria, status, acoes);
    return linha;
}

function classeStatus(status) {
    switch (texto(status).toLocaleLowerCase("pt-BR")) {
        case "concluído": return "status-concluido";
        case "em desenvolvimento": return "status-em-andamento";
        case "cancelado": return "status-cancelado";
        case "planejado": return "status-planejado";
        default: return "status-planejado";
    }
}

function atualizarEstatisticas() {
    const total = todosProjetos.length;
    const contar = status => todosProjetos.filter(p => texto(p.status).toLocaleLowerCase("pt-BR") === status.toLocaleLowerCase("pt-BR")).length;
    const concluidos = contar("Concluído");
    const ativos = contar("Planejado") + contar("Em Desenvolvimento");
    const cancelados = contar("Cancelado");
    document.getElementById("totalProjetos").textContent = total;
    document.getElementById("totalConcluidos").textContent = concluidos;
    document.getElementById("totalAndamento").textContent = ativos;
    document.getElementById("totalPlanejados").textContent = cancelados;
    renderizarGraficos();
}

function renderizarCategorias() {
    const categorias = ["Software", "Mobile", "Web", "Desktop"];
    const ids = { Software: "qtdSoftware", Mobile: "qtdMobile", Web: "qtdWeb", Desktop: "qtdDesktop" };
    categorias.forEach(categoria => {
        const total = todosProjetos.filter(p => texto(p.categoria).toLocaleLowerCase("pt-BR") === categoria.toLocaleLowerCase("pt-BR")).length;
        const elemento = document.getElementById(ids[categoria]);
        if (elemento) elemento.textContent = `${total} ${total === 1 ? "projeto cadastrado" : "projetos cadastrados"}`;
    });
}

function renderizarRecentes() {
    const corpo = document.getElementById("corpoRecentes");
    if (!corpo) return;
    corpo.replaceChildren();
    const recentes = [...todosProjetos].sort((a, b) => texto(b.dataCriacao).localeCompare(texto(a.dataCriacao))).slice(0, 5);
    if (recentes.length === 0) {
        const linha = document.createElement("tr");
        const celula = document.createElement("td");
        celula.colSpan = 5;
        celula.className = "table-empty";
        celula.textContent = "Nenhum projeto cadastrado. Acesse Projetos para adicionar o primeiro.";
        linha.appendChild(celula);
        corpo.appendChild(linha);
        return;
    }
    recentes.forEach(projeto => {
        const linha = document.createElement("tr");
        const id = document.createElement("td");
        id.className = "project-id";
        id.textContent = String(projeto.id ?? "—").padStart(2, "0");
        const nome = document.createElement("td");
        nome.className = "project-name";
        nome.textContent = texto(projeto.nome) || "Sem nome";
        const categoria = document.createElement("td");
        categoria.textContent = texto(projeto.categoria) || "—";
        const status = document.createElement("td");
        const badge = document.createElement("span");
        badge.className = `status-badge ${classeStatus(projeto.status)}`;
        badge.textContent = texto(projeto.status) || "Sem status";
        status.appendChild(badge);
        const prazo = document.createElement("td");
        prazo.textContent = projeto.prazo ? new Date(`${projeto.prazo}T00:00:00`).toLocaleDateString("pt-BR") : "—";
        linha.append(id, nome, categoria, status, prazo);
        corpo.appendChild(linha);
    });
}

function atualizarResumoIndicadores() {
    const alvo = document.getElementById("resumoIndicadores");
    if (!alvo) return;
    const total = todosProjetos.length;
    const concluidos = todosProjetos.filter(p => texto(p.status) === "Concluído").length;
    const ativos = todosProjetos.filter(p => ["Planejado", "Em Desenvolvimento"].includes(texto(p.status))).length;
    const cancelados = todosProjetos.filter(p => texto(p.status) === "Cancelado").length;
    const hoje = dataHojeISO();
    const comPrazo = todosProjetos.filter(p => p.prazo);
    const atrasados = comPrazo.filter(p => p.prazo < hoje).length;
    const metricas = [
        ["Total de projetos", total],
        ["Concluídos", concluidos],
        ["Ativos", ativos],
        ["Cancelados", cancelados],
        ["Com prazo informado", comPrazo.length],
        ["Atrasados", atrasados]
    ];
    alvo.replaceChildren();
    metricas.forEach(([nome, valor]) => {
        const card = document.createElement("div");
        card.className = "report-metric";
        const titulo = document.createElement("span");
        titulo.textContent = nome;
        const numero = document.createElement("strong");
        numero.textContent = String(valor);
        card.append(titulo, numero);
        alvo.appendChild(card);
    });
}

function navegarPara(pagina) {
    const titulos = {
        dashboard: "Dashboard",
        projects: "Projetos",
        categories: "Categorias",
        reports: "Relatórios",
        settings: "Configurações"
    };
    document.querySelectorAll("[data-page]").forEach(secao => {
        const ativa = secao.dataset.page === pagina;
        secao.hidden = !ativa;
        secao.classList.toggle("active", ativa);
    });
    document.querySelectorAll(".sidebar [data-nav]").forEach(item => {
        item.classList.toggle("active", item.dataset.nav === pagina);
    });
    const titulo = document.getElementById("tituloPaginaTopo");
    if (titulo) titulo.textContent = `Sistema de Projetos / ${titulos[pagina] || "Dashboard"}`;
    if (pagina === "reports") {
        requestAnimationFrame(() => renderizarGraficos());
        atualizarResumoIndicadores();
    }
    if (pagina === "categories") renderizarCategorias();
    if (pagina === "dashboard") renderizarRecentes();
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function prepararCanvas(id) {
    const canvas = document.getElementById(id);
    if (!canvas) return null;
    const largura = Math.max(260, canvas.clientWidth || 360);
    const altura = 220;
    const escala = window.devicePixelRatio || 1;
    canvas.width = Math.round(largura * escala);
    canvas.height = Math.round(altura * escala);
    canvas.style.height = `${altura}px`;
    const ctx = canvas.getContext("2d");
    ctx.setTransform(escala, 0, 0, escala, 0, 0);
    ctx.clearRect(0, 0, largura, altura);
    return { ctx, largura, altura };
}

const CORES_GRAFICOS = ["#2864e8", "#22a65a", "#f59e0b", "#dc4545", "#8b5cf6"];

function desenharPizza(id, dados, cores = CORES_GRAFICOS, rosca = false) {
    const alvo = prepararCanvas(id);
    if (!alvo) return;
    const { ctx, largura, altura } = alvo;
    const total = dados.reduce((soma, item) => soma + item.valor, 0);
    const cx = largura * 0.32, cy = altura / 2, raio = 72;
    if (total === 0) {
        ctx.fillStyle = "#8491a5"; ctx.font = "12px sans-serif"; ctx.textAlign = "center";
        ctx.fillText("Sem dados para exibir", largura / 2, altura / 2);
    } else {
        let inicio = -Math.PI / 2;
        dados.forEach((item, index) => {
            const angulo = item.valor / total * Math.PI * 2;
            ctx.beginPath(); ctx.moveTo(cx, cy); ctx.arc(cx, cy, raio, inicio, inicio + angulo); ctx.closePath();
            ctx.fillStyle = cores[index % cores.length]; ctx.fill();
            inicio += angulo;
        });
        if (rosca) {
            ctx.beginPath(); ctx.arc(cx, cy, 40, 0, Math.PI * 2); ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--superficie").trim() || "#fff"; ctx.fill();
            ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--texto").trim() || "#172033";
            ctx.font = "bold 20px sans-serif"; ctx.textAlign = "center"; ctx.fillText(String(total), cx, cy + 7);
        }
    }
    ctx.textAlign = "left"; ctx.font = "11px sans-serif";
    dados.forEach((item, index) => {
        const y = 54 + index * 29;
        ctx.fillStyle = cores[index % cores.length]; ctx.fillRect(largura * 0.61, y - 9, 10, 10);
        ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--texto").trim() || "#334155";
        ctx.fillText(`${item.nome}: ${item.valor}`, largura * 0.61 + 17, y);
    });
}

function desenharBarras(id, dados) {
    const alvo = prepararCanvas(id);
    if (!alvo) return;
    const { ctx, largura } = alvo;
    const maximo = Math.max(1, ...dados.map(item => item.valor));
    dados.forEach((item, index) => {
        const y = 28 + index * 43;
        const nome = item.nome;
        ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--texto-suave").trim() || "#64748b";
        ctx.font = "11px sans-serif"; ctx.textAlign = "left"; ctx.fillText(nome, 10, y + 10);
        const x = 90, larguraMax = largura - 125, barra = larguraMax * item.valor / maximo;
        ctx.fillStyle = "#e8edf5"; ctx.fillRect(x, y + 17, larguraMax, 12);
        ctx.fillStyle = CORES_GRAFICOS[index % CORES_GRAFICOS.length]; ctx.fillRect(x, y + 17, barra, 12);
        ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--texto").trim() || "#172033";
        ctx.fillText(String(item.valor), Math.min(x + barra + 6, largura - 23), y + 27);
    });
}

function desenharLinha(id, dados) {
    const alvo = prepararCanvas(id);
    if (!alvo) return;
    const { ctx, largura, altura } = alvo;
    const margem = { esquerda: 34, direita: 14, topo: 20, baixo: 36 };
    const areaW = largura - margem.esquerda - margem.direita;
    const areaH = altura - margem.topo - margem.baixo;
    const maximo = Math.max(1, ...dados.map(item => item.valor));
    ctx.strokeStyle = "#e4eaf2"; ctx.lineWidth = 1;
    for (let i = 0; i <= 4; i++) { const y = margem.topo + areaH * i / 4; ctx.beginPath(); ctx.moveTo(margem.esquerda, y); ctx.lineTo(largura - margem.direita, y); ctx.stroke(); }
    ctx.strokeStyle = "#2864e8"; ctx.lineWidth = 2.5; ctx.beginPath();
    dados.forEach((item, index) => {
        const x = margem.esquerda + areaW * index / Math.max(1, dados.length - 1);
        const y = margem.topo + areaH - areaH * item.valor / maximo;
        if (index === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        ctx.fillStyle = "#2864e8"; ctx.beginPath(); ctx.arc(x, y, 3.5, 0, Math.PI * 2); ctx.fill();
        ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--texto-suave").trim() || "#64748b";
        ctx.font = "10px sans-serif"; ctx.textAlign = "center"; ctx.fillText(item.nome, x, altura - 12);
        if (index < dados.length - 1) { ctx.beginPath(); ctx.moveTo(x, y); }
    });
    // Desenha a linha depois dos marcadores para manter a conexão visual.
    ctx.beginPath();
    dados.forEach((item, index) => { const x = margem.esquerda + areaW * index / Math.max(1, dados.length - 1); const y = margem.topo + areaH - areaH * item.valor / maximo; if (index === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y); });
    ctx.stroke();
}

function renderizarGraficos() {
    const status = ["Planejado", "Em Desenvolvimento", "Concluído", "Cancelado"].map(nome => ({ nome, valor: todosProjetos.filter(p => texto(p.status).toLowerCase() === nome.toLowerCase()).length }));
    const categorias = ["Web", "Software", "Mobile", "Desktop"].map(nome => ({ nome, valor: todosProjetos.filter(p => texto(p.categoria).toLowerCase() === nome.toLowerCase()).length }));
    const hoje = dataHojeISO();
    const prazo = [
        { nome: "No prazo", valor: todosProjetos.filter(p => p.prazo && p.prazo >= hoje).length },
        { nome: "Atrasados", valor: todosProjetos.filter(p => p.prazo && p.prazo < hoje).length }
    ];
    const mesAtual = new Date();
    const evolucao = Array.from({ length: 6 }, (_, index) => {
        const data = new Date(mesAtual.getFullYear(), mesAtual.getMonth() - 5 + index, 1);
        const chave = `${data.getFullYear()}-${String(data.getMonth() + 1).padStart(2, "0")}`;
        return { nome: data.toLocaleDateString("pt-BR", { month: "short" }), valor: todosProjetos.filter(p => texto(p.dataCriacao).startsWith(chave)).length };
    });
    desenharPizza("graficoStatus", status);
    desenharBarras("graficoCategorias", categorias);
    desenharLinha("graficoEvolucao", evolucao);
    desenharPizza("graficoPrazos", prazo, ["#22a65a", "#dc4545"], true);
}

function atualizarPaginacao() {
    const totalPaginas = Math.max(1, Math.ceil(projetosFiltrados.length / ITENS_POR_PAGINA));
    document.getElementById("btnAnterior").disabled = ocupado || paginaAtual <= 0;
    document.getElementById("btnProxima").disabled = ocupado || paginaAtual + 1 >= totalPaginas;
}

function editarProjeto(projeto) {
    if (ocupado) return;
    idEmEdicao = Number(projeto.id);
    document.getElementById("nome").value = texto(projeto.nome);
    document.getElementById("descricao").value = texto(projeto.descricao);
    document.getElementById("categoria").value = texto(projeto.categoria);
    document.getElementById("status").value = normalizarStatus(projeto.status);
    document.getElementById("dataCriacao").value = texto(projeto.dataCriacao) || dataHojeISO();
    document.getElementById("prazo").value = texto(projeto.prazo);
    tituloFormulario.textContent = `Editar projeto #${projeto.id}`;
    btnSalvar.innerHTML = '<i class="bi bi-floppy-fill me-1" aria-hidden="true"></i> Salvar alterações';
    btnCancelar.hidden = false;
    btnExcluirFormulario.disabled = false;
    ocultarMensagem();
    navegarPara("projects");
    document.getElementById("form-card").scrollIntoView({ behavior: "smooth", block: "start" });
    document.getElementById("nome").focus({ preventScroll: true });
}

function cancelarEdicao() {
    idEmEdicao = null;
    formProjeto.reset();
    document.getElementById("status").value = "Planejado";
    document.getElementById("categoria").value = "Software";
    document.getElementById("dataCriacao").value = dataHojeISO();
    document.getElementById("prazo").value = "";
    tituloFormulario.textContent = "Cadastro de projeto";
    btnSalvar.innerHTML = '<i class="bi bi-floppy-fill me-1" aria-hidden="true"></i> Salvar';
    btnCancelar.hidden = true;
    btnExcluirFormulario.disabled = true;
    formProjeto.classList.remove("was-validated");
}

function validarFormulario(projeto) {
    const campoNome = document.getElementById("nome");
    const campoDescricao = document.getElementById("descricao");
    const campoCategoria = document.getElementById("categoria");
    const campoPrazo = document.getElementById("prazo");

    campoNome.setCustomValidity(projeto.nome.length >= 3 && projeto.nome.length <= 100 ? "" : "O nome deve ter entre 3 e 100 caracteres.");
    campoDescricao.setCustomValidity(projeto.descricao.length >= 10 && projeto.descricao.length <= 500 ? "" : "A descrição deve ter entre 10 e 500 caracteres.");
    campoCategoria.setCustomValidity(CATEGORIAS_VALIDAS.includes(projeto.categoria) ? "" : "Selecione uma categoria válida.");
    campoPrazo.setCustomValidity( !projeto.prazo || !projeto.dataCriacao || projeto.prazo >= projeto.dataCriacao ? "" : "O prazo não pode ser anterior à data de criação.");

    formProjeto.classList.add("was-validated");
    if (!formProjeto.checkValidity()) {
        const erro = "Verifique os campos: nome (3 a 100 caracteres), descrição (10 a 500), categoria e prazo.";
        mostrarMensagem(erro, "error");
        registrarErroCliente(erro);
        return false;
    }
    if (!STATUS_VALIDOS.includes(projeto.status)) {
        const erro = "Selecione um status válido.";
        mostrarMensagem(erro, "error");
        registrarErroCliente(erro);
        return false;
    }
    return true;
}

function dataHojeISO() {
    const agora = new Date();
    const local = new Date(agora.getTime() - agora.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 10);
}

function normalizarStatus(status) {
    const valor = texto(status).toLocaleLowerCase("pt-BR");
    if (valor === "em desenvolvimento" || valor === "em andamento") return "Em Desenvolvimento";
    return STATUS_VALIDOS.find(item => item.toLocaleLowerCase("pt-BR") === valor) || "Planejado";
}

async function registrarErroCliente(mensagemErro) {
    try {
        await fetch("http://localhost:7070/api/logs/error", {
            method: "POST",
            headers: cabecalhosAutenticados({ "Content-Type": "application/json" }),
            body: JSON.stringify({ mensagem: mensagemErro })
        });
    } catch (_) { /* Se a API estiver indisponível, não há como persistir o erro no servidor. */ }
}

formProjeto.addEventListener("submit", async evento => {
    evento.preventDefault();
    if (ocupado) return;
    const projeto = {
        nome: document.getElementById("nome").value.trim(),
        descricao: document.getElementById("descricao").value.trim(),
        categoria: document.getElementById("categoria").value.trim(),
        status: document.getElementById("status").value,
        dataCriacao: document.getElementById("dataCriacao").value || dataHojeISO(),
        prazo: document.getElementById("prazo").value
    };
    if (!validarFormulario(projeto)) return;

    const editando = idEmEdicao !== null;
    const url = editando ? `${API_URL}/${idEmEdicao}` : API_URL;
    definirOcupado(true);
    mostrarMensagem(editando ? "Salvando alterações..." : "Cadastrando projeto...", "info");
    try {
        const resposta = await fetch(url, {
            method: editando ? "PUT" : "POST",
            headers: cabecalhosAutenticados({ "Content-Type": "application/json", "Accept": "application/json" }),
            body: JSON.stringify(projeto)
        });
        if (tratarSessaoInvalida(resposta)) return;
        if (!resposta.ok) throw new Error(await lerErro(resposta));
        const dados = resposta.status === 204 ? null : await resposta.json();
        definirStatusApi(true);
        const mensagemSucesso = editando ? "Projeto alterado com sucesso." : `Projeto criado com sucesso${dados?.id ? ` (ID ${dados.id})` : ""}.`;
        cancelarEdicao();
        await listarProjetos({ mostrarCarregamento: false, preservarMensagem: true });
        mostrarMensagem(mensagemSucesso, "success");
    } catch (erro) {
        definirStatusApi(false);
        mostrarMensagem(`Não foi possível salvar o projeto. ${erro.message || "Verifique a conexão com a API."}`, "error");
        console.error(erro);
    } finally {
        definirOcupado(false);
        renderizarTabela(projetosFiltrados);
    }
});

async function excluirProjeto(id) {
    if (ocupado) return;
    const projeto = todosProjetos.find(p => Number(p.id) === Number(id));
    const nome = projeto ? `“${texto(projeto.nome)}”` : `ID ${id}`;
    if (!window.confirm(`Deseja realmente excluir o projeto ${nome}?`)) return;

    definirOcupado(true);
    mostrarMensagem("Excluindo projeto...", "info");
    try {
        const resposta = await fetch(`${API_URL}/${id}`, { method: "DELETE", headers: cabecalhosAutenticados() });
        if (tratarSessaoInvalida(resposta)) return;
        if (!resposta.ok) throw new Error(await lerErro(resposta));
        if (Number(idEmEdicao) === Number(id)) cancelarEdicao();
        definirStatusApi(true);
        await listarProjetos({ mostrarCarregamento: false, preservarMensagem: true });
        mostrarMensagem("Projeto excluído com sucesso.", "success");
    } catch (erro) {
        definirStatusApi(false);
        mostrarMensagem(`Não foi possível excluir o projeto. ${erro.message || "Verifique a conexão com a API."}`, "error");
        console.error(erro);
    } finally {
        definirOcupado(false);
        renderizarTabela(projetosFiltrados);
    }
}

function novoProjeto() {
    cancelarEdicao();
    ocultarMensagem();
    navegarPara("projects");
    document.getElementById("form-card").scrollIntoView({ behavior: "smooth", block: "start" });
    document.getElementById("nome").focus({ preventScroll: true });
}

function termoBuscaAtivo() { return campoBusca.value.trim().length > 0; }
function texto(valor) { return valor === null || valor === undefined ? "" : String(valor); }

async function sairDoSistema() {
    const botaoSair = document.getElementById("btnSair");
    botaoSair.disabled = true;
    try {
        await fetch(`${AUTH_URL}/logout`, {
            method: "POST",
            headers: cabecalhosAutenticados()
        });
    } catch (erro) {
        console.warn("Não foi possível encerrar a sessão no servidor.", erro);
    } finally {
        encerrarSessaoLocal();
        window.location.replace("login.html");
    }
}

document.getElementById("btnSair").addEventListener("click", sairDoSistema);
carregarUsuarioLogado();
document.getElementById("dataCriacao").value = dataHojeISO();
atualizarDataHora();
window.setInterval(atualizarDataHora, 60000);

campoBusca.addEventListener("input", aplicarFiltros);
tipoBusca.addEventListener("change", aplicarFiltros);
document.getElementById("btnAtualizar").addEventListener("click", () => listarProjetos());
document.getElementById("btnLimpar").addEventListener("click", () => {
    cancelarEdicao();
    mostrarMensagem("Formulário limpo.", "info");
});
btnExcluirFormulario.addEventListener("click", () => {
    if (idEmEdicao === null) {
        mostrarMensagem("Selecione um projeto na tabela para excluí-lo.", "info");
        return;
    }
    excluirProjeto(idEmEdicao);
});
btnCancelar.addEventListener("click", () => {
    cancelarEdicao();
    mostrarMensagem("Edição cancelada.", "info");
});
document.getElementById("btnAnterior").addEventListener("click", () => {
    if (paginaAtual > 0) { paginaAtual--; renderizarTabela(projetosFiltrados); }
});
document.getElementById("btnProxima").addEventListener("click", () => {
    const totalPaginas = Math.max(1, Math.ceil(projetosFiltrados.length / ITENS_POR_PAGINA));
    if (paginaAtual + 1 < totalPaginas) { paginaAtual++; renderizarTabela(projetosFiltrados); }
});
document.querySelectorAll("[data-sort]").forEach(botao => botao.addEventListener("click", () => {
    if (colunaOrdenada === botao.dataset.sort) ordemAscendente = !ordemAscendente;
    else { colunaOrdenada = botao.dataset.sort; ordemAscendente = true; }
    ordenarProjetos();
    paginaAtual = 0;
    renderizarTabela(projetosFiltrados);
}));

// Navegação entre as cinco áreas do sistema.
document.querySelectorAll(".sidebar [data-nav]").forEach(link => {
    link.addEventListener("click", evento => {
        evento.preventDefault();
        navegarPara(link.dataset.nav);
    });
});

document.querySelectorAll("[data-go]").forEach(botao => botao.addEventListener("click", () => navegarPara(botao.dataset.go)));
document.querySelectorAll("[data-category-filter]").forEach(botao => botao.addEventListener("click", () => {
    tipoBusca.value = "categoria";
    campoBusca.value = botao.dataset.categoryFilter;
    aplicarFiltros();
    navegarPara("projects");
}));

document.getElementById("btnTemaConfig").addEventListener("click", () => alternarTema());
document.getElementById("temaConfiguracoes").addEventListener("change", evento => aplicarTema(evento.target.value));
document.getElementById("itensPorPaginaConfig").addEventListener("change", evento => {
    const valor = Number(evento.target.value);
    if (![5, 6, 10, 20].includes(valor)) return;
    ITENS_POR_PAGINA = valor;
    paginaAtual = 0;
    try { localStorage.setItem("sistemaProjetosItensPorPagina", String(valor)); } catch (_) { /* armazenamento opcional */ }
    renderizarTabela(projetosFiltrados);
    mostrarMensagem(`A lista agora exibe ${valor} projetos por página.`, "success");
});
document.getElementById("btnTestarApi").addEventListener("click", () => listarProjetos());
document.getElementById("btnExportarCsv").addEventListener("click", exportarProjetosCsv);
document.getElementById("btnSairConfiguracoes").addEventListener("click", sairDoSistema);
document.getElementById("btnNovoProjeto").addEventListener("click", novoProjeto);
document.getElementById("btnNovoProjetoPagina").addEventListener("click", novoProjeto);
btnTema.addEventListener("click", () => alternarTema());

function exportarProjetosCsv() {
    const cabecalho = ["id", "nome", "descricao", "categoria", "status", "dataCriacao", "prazo"];
    const escapar = valor => `"${texto(valor).replaceAll('"', '""')}"`;
    const linhas = [cabecalho.join(";")];
    todosProjetos.forEach(projeto => linhas.push(cabecalho.map(campo => escapar(projeto[campo])).join(";")));
    const arquivo = new Blob(["\uFEFF" + linhas.join("\r\n")], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(arquivo);
    const link = document.createElement("a");
    link.href = url;
    link.download = "relatorio_projetos.csv";
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
    mostrarMensagem("Relatório CSV exportado com sucesso.", "success");
}

function aplicarTema(novoTema) {
    if (!["light", "dark"].includes(novoTema)) return;
    document.documentElement.dataset.theme = novoTema;
    const icone = novoTema === "dark" ? "bi-sun-fill" : "bi-moon-stars-fill";
    btnTema.innerHTML = `<i class="bi ${icone}" aria-hidden="true"></i>`;
    btnTema.title = novoTema === "dark" ? "Ativar tema claro" : "Ativar tema escuro";
    const seletorTema = document.getElementById("temaConfiguracoes");
    if (seletorTema && seletorTema.value !== novoTema) seletorTema.value = novoTema;
    try { localStorage.setItem("sistemaProjetosTema", novoTema); } catch (_) { /* armazenamento opcional */ }
    if (document.querySelector('[data-page="reports"]:not([hidden])')) requestAnimationFrame(renderizarGraficos);
}

function alternarTema() {
    const novoTema = document.documentElement.dataset.theme === "dark" ? "light" : "dark";
    aplicarTema(novoTema);
}

try {
    const temaSalvo = localStorage.getItem("sistemaProjetosTema");
    if (temaSalvo === "dark" || temaSalvo === "light") document.documentElement.dataset.theme = temaSalvo;
    const seletorTema = document.getElementById("temaConfiguracoes");
    seletorTema.value = document.documentElement.dataset.theme;
    const paginaSalva = Number(localStorage.getItem("sistemaProjetosItensPorPagina"));
    if ([5, 6, 10, 20].includes(paginaSalva)) ITENS_POR_PAGINA = paginaSalva;
    document.getElementById("itensPorPaginaConfig").value = String(ITENS_POR_PAGINA);
    if (document.documentElement.dataset.theme === "dark") {
        btnTema.innerHTML = '<i class="bi bi-sun-fill" aria-hidden="true"></i>';
    }
} catch (_) { /* armazenamento opcional */ }

navegarPara("dashboard");

window.addEventListener("resize", () => renderizarGraficos());
// Abre a página já com os projetos carregados pela API.
listarProjetos();
