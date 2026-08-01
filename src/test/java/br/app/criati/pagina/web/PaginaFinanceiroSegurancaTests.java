package br.app.criati.pagina.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaFinanceiroSegurancaTests {

	private static final String SENHA = "senha-correta";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private AplicacaoService aplicacaoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void todasAsPaginasFinanceirasAnonimoRedirecionamParaLogin() throws Exception {
		String[] rotas = {
				"/app/financeiro", "/app/financeiro/contas", "/app/financeiro/categorias",
				"/app/financeiro/lancamentos", "/app/financeiro/pessoas", "/app/financeiro/contatos",
				"/app/financeiro/recorrencias", "/app/financeiro/contas-a-pagar",
				"/app/financeiro/cartoes", "/app/financeiro/compras-cartao",
				"/app/financeiro/parcelas-cartao", "/app/financeiro/emprestimos",
				"/app/financeiro/faturas", "/app/financeiro/faturas/" + UUID.randomUUID(),
				"/app/financeiro/importacoes", "/app/financeiro/importacoes/nova",
				"/app/financeiro/importacoes/" + UUID.randomUUID()
		};
		for (String rota : rotas) {
			mockMvc.perform(get(rota))
					.andExpect(status().is3xxRedirection())
					.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
		}
	}

	@Test
	void empresaSemFinanceiroHabilitadoRedirecionaParaAplicacoes() throws Exception {
		Empresa empresa = criarEmpresa("11111111000251");
		Usuario usuario = criarUsuario("pagina.fin.sem.app@criati.test");
		criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/financeiro").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/contas").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/categorias").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/lancamentos").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/pessoas").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/contatos").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/recorrencias").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/contas-a-pagar").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/cartoes").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/compras-cartao").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/parcelas-cartao").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/emprestimos").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/faturas").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/faturas/" + UUID.randomUUID()).session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/importacoes").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/importacoes/nova").session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
		mockMvc.perform(get("/app/financeiro/importacoes/" + UUID.randomUUID()).session(session))
				.andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void empresaComApenasClinicaNaoAcessaFinanceiro() throws Exception {
		Empresa empresa = criarEmpresa("22222222000252");
		aplicacaoService.habilitar(empresa.getId(), "CLINICA");
		Usuario usuario = criarUsuario("pagina.fin.clinica@criati.test");
		criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/financeiro").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void empresaComFinanceiroHabilitadoAcessaDashboard() throws Exception {
		Empresa empresa = criarEmpresa("33333333000253");
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = criarUsuario("pagina.fin.ok@criati.test");
		criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/financeiro").session(session))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/app/financeiro/pessoas")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/app/financeiro/contatos")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-dashboard-cards")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-card-saldo-atual")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-card-receitas")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-card-despesas")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-card-resultado")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/app/financeiro/lancamentos")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("financeiro-dashboard-acoes"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Novo lançamento"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Despesas por categoria")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Pendências financeiras")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Lançamentos recentes")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-grafico-vazio")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-ultimos-lancamentos-vazio")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("financeiro-navegacao"))));
		mockMvc.perform(get("/app/financeiro/contas").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma conta financeira cadastrada")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Saldo inicial consolidado")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("O saldo informado representa a posição inicial")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("contas-filtros-painel")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-operacional-cards")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nova conta")));
		mockMvc.perform(get("/app/financeiro/categorias").session(session)).andExpect(status().isOk());
		mockMvc.perform(get("/app/financeiro/lancamentos").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-filtros-recolhiveis")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("data-filtros-painel hidden")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Novo lançamento")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-operacional-cards")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("resumo-quantidade-lancamentos")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Aplicar filtros")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Limpar filtros")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-tabela-operacional")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"dialog\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("financeiro-navegacao"))));
		mockMvc.perform(get("/app/financeiro/pessoas").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma pessoa cadastrada")));
		mockMvc.perform(get("/app/financeiro/contatos").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhum contato financeiro cadastrado")));
		mockMvc.perform(get("/app/financeiro/recorrencias").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma recorrencia cadastrada")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("recorrencias-filtros-painel")));
		mockMvc.perform(get("/app/financeiro/contas-a-pagar").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma conta a pagar encontrada")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("ocorrencias-filtros-painel")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-abas")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nova obrigação")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"dialog\"")));
		mockMvc.perform(get("/app/financeiro/cartoes").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Cadastrar cartão")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("cartoes-filtros-painel")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Novo cartão")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-operacional-cards")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("role=\"dialog\"")));
		mockMvc.perform(get("/app/financeiro/compras-cartao").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-formulario-operacional")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Ver parcelas")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("Compra realizada para terceiro"))))
				.andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Pessoa relacionada"))))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Compras para terceiros")));
		mockMvc.perform(get("/app/financeiro/parcelas-cartao").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Parcelas de cartão")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma parcela encontrada")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("financeiro-painel-filtros")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Movimentações")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Crédito e terceiros")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Cadastros")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("aria-current=\"page\"")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("financeiro-navegacao"))));
		mockMvc.perform(get("/app/financeiro/emprestimos").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Empréstimos concedidos")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhum empréstimo concedido encontrado")));
		mockMvc.perform(get("/app/financeiro/compras-terceiros").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Compras para terceiros")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Nenhuma compra para terceiro encontrada")));
		mockMvc.perform(get("/app/financeiro/faturas").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Faturas de cartão")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("faturas-filtro-cartao")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("faturas-filtro-competencia")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("faturas-filtro-status")))
				// Usuario com perfil USUARIO (nao ADMINISTRADOR): botao de escrita
				// nao pode ser renderizado, mesma regra de
				// FaturaCartaoService#exigirAdministrador no backend.
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("faturas-abrir-botao"))));
		mockMvc.perform(get("/app/financeiro/faturas/" + UUID.randomUUID()).session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Detalhes da fatura")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("fatura-detalhe-nao-encontrada")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("fatura-pagar-abrir"))));
		mockMvc.perform(get("/app/financeiro/importacoes").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("Importação OFX")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("importacoes-tabela-wrap")))
				// Usuario com perfil USUARIO: link de escrita nao pode ser renderizado,
				// mesma regra de ImportacaoBancariaService#exigirEscrita no backend.
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("/app/financeiro/importacoes/nova"))));
		mockMvc.perform(get("/app/financeiro/importacoes/nova").session(session))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/app/financeiro/importacoes/" + UUID.randomUUID()).session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("importacao-detalhe-nao-encontrada")))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("id=\"importacao-descartar\""))))
				// Usuario com perfil USUARIO: nenhum controle de confirmacao (selecao
				// em massa, checkbox "selecionar todas") pode ser renderizado, mesma
				// regra de ConfirmacaoImportacaoBancariaService#exigirEscrita no backend.
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("id=\"importacao-transacoes-toolbar\""))))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("id=\"importacao-confirmar-selecionadas\""))))
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("id=\"importacao-selecionar-todas\""))));
	}

	@Test
	void administradorAcessaAcoesDeGerenciamentoDeFaturas() throws Exception {
		Empresa empresa = criarEmpresa("66666666000256");
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = criarUsuario("pagina.fin.faturas.admin@criati.test");
		UsuarioEmpresa vinculo = criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(vinculo, "perfil", PerfilUsuario.ADMINISTRADOR);
		usuarioEmpresaRepository.saveAndFlush(vinculo);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/financeiro/faturas").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"faturas-abrir-botao\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("fatura-abrir-cartao")));
		mockMvc.perform(get("/app/financeiro/faturas/" + UUID.randomUUID()).session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"fatura-pagar-abrir\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"fatura-encargos-abrir\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"fatura-fechar\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"fatura-recompor\"")));
	}

	@Test
	void gestorNaoAcessaAcoesDeGerenciamentoDeFaturas() throws Exception {
		Empresa empresa = criarEmpresa("77777777000257");
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = criarUsuario("pagina.fin.faturas.gestor@criati.test");
		UsuarioEmpresa vinculo = criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(vinculo, "perfil", PerfilUsuario.GESTOR);
		usuarioEmpresaRepository.saveAndFlush(vinculo);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		// GESTOR gerencia emprestimos/compras-terceiros, mas faturas de cartao
		// exigem especificamente ADMINISTRADOR (unico perfil aceito por
		// FaturaCartaoService/PagamentoFaturaCartaoService#exigirAdministrador).
		mockMvc.perform(get("/app/financeiro/faturas").session(session)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("faturas-abrir-botao"))));
	}

	@Test
	void administradorEGestorAcessamAcoesDeGerenciamentoDeImportacoes() throws Exception {
		Empresa empresa = criarEmpresa("88888888000258");
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario administrador = criarUsuario("pagina.fin.importacoes.admin@criati.test");
		UsuarioEmpresa vinculoAdmin = criarVinculo(administrador, empresa, StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(vinculoAdmin, "perfil", PerfilUsuario.ADMINISTRADOR);
		usuarioEmpresaRepository.saveAndFlush(vinculoAdmin);
		MockHttpSession sessaoAdmin = autenticarNaEmpresa(administrador.getEmail(), empresa.getId());

		mockMvc.perform(get("/app/financeiro/importacoes").session(sessaoAdmin)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("/app/financeiro/importacoes/nova")));
		mockMvc.perform(get("/app/financeiro/importacoes/nova").session(sessaoAdmin)).andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"importacao-conta\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"importacao-arquivo\"")));
		mockMvc.perform(get("/app/financeiro/importacoes/" + UUID.randomUUID()).session(sessaoAdmin))
				.andExpect(status().isOk())
				// Controles de confirmacao (toolbar de selecao em massa e checkbox
				// "selecionar todas") sao renderizados para quem pode escrever,
				// mesma regra de ConfirmacaoImportacaoBancariaService#exigirEscrita.
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"importacao-transacoes-toolbar\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"importacao-confirmar-selecionadas\"")))
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"importacao-selecionar-todas\"")));

		Usuario gestor = criarUsuario("pagina.fin.importacoes.gestor@criati.test");
		UsuarioEmpresa vinculoGestor = criarVinculo(gestor, empresa, StatusCadastro.ATIVO);
		ReflectionTestUtils.setField(vinculoGestor, "perfil", PerfilUsuario.GESTOR);
		usuarioEmpresaRepository.saveAndFlush(vinculoGestor);
		MockHttpSession sessaoGestor = autenticarNaEmpresa(gestor.getEmail(), empresa.getId());

		// GESTOR gerencia importacoes OFX, diferente de faturas (exigem
		// especificamente ADMINISTRADOR) - mesma regra de
		// ImportacaoBancariaService#exigirEscrita (ADMINISTRADOR ou GESTOR).
		mockMvc.perform(get("/app/financeiro/importacoes/nova").session(sessaoGestor)).andExpect(status().isOk());
		mockMvc.perform(get("/app/financeiro/importacoes/" + UUID.randomUUID()).session(sessaoGestor))
				.andExpect(status().isOk())
				.andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"importacao-confirmar-selecionadas\"")));
	}

	@Test
	void vinculoInativoBloqueiaAcessoAoFinanceiro() throws Exception {
		Empresa empresa = criarEmpresa("44444444000254");
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = criarUsuario("pagina.fin.vinculo.inativo@criati.test");
		UsuarioEmpresa vinculo = criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		ReflectionTestUtils.setField(vinculo, "status", StatusCadastro.INATIVO);
		usuarioEmpresaRepository.saveAndFlush(vinculo);

		mockMvc.perform(get("/app/financeiro").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
	}

	@Test
	void empresaInativaBloqueiaAcessoAoFinanceiro() throws Exception {
		Empresa empresa = criarEmpresa("55555555000255");
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = criarUsuario("pagina.fin.empresa.inativa@criati.test");
		criarVinculo(usuario, empresa, StatusCadastro.ATIVO);
		MockHttpSession session = autenticarNaEmpresa(usuario.getEmail(), empresa.getId());

		ReflectionTestUtils.setField(empresa, "status", StatusCadastro.INATIVO);
		empresaRepository.saveAndFlush(empresa);

		mockMvc.perform(get("/app/financeiro").session(session))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/app/aplicacoes"));
	}

	private Empresa criarEmpresa(String cnpj) {
		return empresaRepository.saveAndFlush(new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, StatusCadastro status) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, PerfilUsuario.USUARIO, status);
		return usuarioEmpresaRepository.saveAndFlush(vinculo);
	}

	private MockHttpSession autenticarNaEmpresa(String email, UUID empresaId) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(email, SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresaId)))
				.andExpect(status().isOk());
		return session;
	}
}
