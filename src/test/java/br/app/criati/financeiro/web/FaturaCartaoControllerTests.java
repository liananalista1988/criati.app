package br.app.criati.financeiro.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.FaturaCartaoNaoEncontradaException;
import br.app.criati.exception.FaturaCartaoStatusInvalidoException;
import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.CompraCartao;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.model.ParcelaCompraCartao;
import br.app.criati.financeiro.repository.CartaoCreditoRepository;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.CompraCartaoRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.repository.ParcelaCompraCartaoRepository;
import br.app.criati.financeiro.service.FaturaCartaoService;
import br.app.criati.financeiro.service.CompraCartaoService;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusFaturaCartao;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FaturaCartaoControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL = "/api/contexto/financeiro/faturas";

	@Autowired private MockMvc mockMvc;
	@Autowired private FaturaCartaoService service;
	@Autowired private CompraCartaoService compraService;
	@Autowired private EmpresaRepository empresas;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private UsuarioEmpresaRepository vinculos;
	@Autowired private PessoaFinanceiraRepository pessoas;
	@Autowired private InstituicaoFinanceiraRepository instituicoes;
	@Autowired private CartaoCreditoRepository cartoes;
	@Autowired private CategoriaFinanceiraRepository categorias;
	@Autowired private CompraCartaoRepository compras;
	@Autowired private ParcelaCompraCartaoRepository parcelas;
	@Autowired private AplicacaoService aplicacoes;
	@Autowired private PasswordEncoder encoder;

	@Test
	void aberturaSemParcelasEIdempotente() {
		Fixture fixture = fixture("51111111000401", PerfilUsuario.ADMINISTRADOR);
		YearMonth mesFuturo = YearMonth.from(LocalDate.now()).plusMonths(1);
		LocalDate competencia = mesFuturo.atDay(12);
		LocalDate fechamento = mesFuturo.atDay(5);
		LocalDate fechamentoAnterior = mesFuturo.minusMonths(1).atDay(5);

		var primeira = service.abrir(fixture.principal().getId(), mesFuturo.atDay(1), fixture.contexto());
		var segunda = service.abrir(fixture.principal().getId(), mesFuturo.atEndOfMonth(), fixture.contexto());

		assertThat(primeira.fatura().getId()).isEqualTo(segunda.fatura().getId());
		assertThat(primeira.fatura().getCompetencia()).isEqualTo(competencia);
		assertThat(primeira.fatura().getPeriodoInicial()).isEqualTo(fechamentoAnterior.plusDays(1));
		assertThat(primeira.fatura().getPeriodoFinal()).isEqualTo(fechamento);
		assertThat(primeira.fatura().getValorTotal()).isEqualByComparingTo("0.00");
		assertThat(primeira.parcelas()).isEmpty();
		assertThatThrownBy(() -> service.fechar(primeira.fatura().getId(), fixture.contexto()))
				.isInstanceOf(FaturaCartaoStatusInvalidoException.class)
				.hasMessageContaining("antes da data");
	}

	@Test
	void composicaoSomaCompetenciaEVirtualNaFaturaDoPrincipalSemDuplicar() {
		Fixture fixture = fixture("52222222000402", PerfilUsuario.ADMINISTRADOR);
		CartaoCredito virtual = criarVirtual(fixture);
		criarCompraComParcela(fixture, fixture.principal(), new BigDecimal("10.10"), LocalDate.of(2026, 8, 12));
		criarCompraComParcela(fixture, virtual, new BigDecimal("20.20"), LocalDate.of(2026, 8, 12));
		criarCompraComParcela(fixture, fixture.principal(), new BigDecimal("99.00"), LocalDate.of(2026, 9, 12));

		var aberta = service.abrir(virtual.getId(), LocalDate.of(2026, 8, 12), fixture.contexto());
		var recomposta = service.recompor(aberta.fatura().getId(), fixture.contexto());

		assertThat(aberta.fatura().getCartaoPrincipal().getId()).isEqualTo(fixture.principal().getId());
		assertThat(recomposta.fatura().getValorTotal()).isEqualByComparingTo("30.30");
		assertThat(recomposta.parcelas()).hasSize(2);
		assertThat(recomposta.parcelas()).allMatch(p -> aberta.fatura().getId().equals(p.getFaturaId()));
	}

	@Test
	void fechamentoCongelaFaturaEImpedeRecomposicao() {
		Fixture fixture = fixture("53333333000403", PerfilUsuario.ADMINISTRADOR);
		criarCompraComParcela(fixture, fixture.principal(), new BigDecimal("42.00"), LocalDate.of(2026, 1, 12));
		var aberta = service.abrir(fixture.principal().getId(), LocalDate.of(2026, 1, 12), fixture.contexto());

		var fechada = service.fechar(aberta.fatura().getId(), fixture.contexto());

		assertThat(fechada.fatura().getStatus()).isEqualTo(StatusFaturaCartao.FECHADA);
		assertThat(fechada.fatura().getValorTotal()).isEqualByComparingTo("42.00");
		assertThat(fechada.fatura().getFechadoEm()).isNotNull();
		assertThatThrownBy(() -> service.recompor(aberta.fatura().getId(), fixture.contexto()))
				.isInstanceOf(FaturaCartaoStatusInvalidoException.class);
		assertThatThrownBy(() -> service.fechar(aberta.fatura().getId(), fixture.contexto()))
				.isInstanceOf(FaturaCartaoStatusInvalidoException.class);
	}

	@Test
	void faturaFechadaImpedeCompraRetroativaECancelamentoDaComposicao() {
		Fixture fixture = fixture("53535353000403", PerfilUsuario.ADMINISTRADOR);
		ParcelaCompraCartao parcela = criarCompraComParcela(
				fixture, fixture.principal(), new BigDecimal("42.00"), LocalDate.of(2026, 1, 12));
		var fatura = service.abrir(fixture.principal().getId(), LocalDate.of(2026, 1, 12), fixture.contexto());
		service.fechar(fatura.fatura().getId(), fixture.contexto());

		assertThatThrownBy(() -> compraService.cancelar(
				parcela.getCompra().getId(), "Cancelamento tardio", fixture.contexto()))
				.isInstanceOf(FaturaCartaoStatusInvalidoException.class);
		assertThatThrownBy(() -> compraService.criar(fixture.principal().getId(), fixture.pessoa().getId(),
				fixture.categoria().getId(), null, "Compra retroativa", LocalDate.of(2026, 1, 1),
				new BigDecimal("10.00"), 1, null, fixture.contexto()))
				.isInstanceOf(FaturaCartaoStatusInvalidoException.class);
	}

	@Test
	void isolamentoEntreEmpresasNaoRevelaFatura() {
		Fixture empresaA = fixture("54444444000404", PerfilUsuario.ADMINISTRADOR);
		Fixture empresaB = fixture("55555555000405", PerfilUsuario.ADMINISTRADOR);
		var faturaA = service.abrir(empresaA.principal().getId(), LocalDate.of(2026, 8, 12), empresaA.contexto());

		assertThatThrownBy(() -> service.buscar(faturaA.fatura().getId(), empresaB.contexto()))
				.isInstanceOf(FaturaCartaoNaoEncontradaException.class);
		assertThatThrownBy(() -> service.abrir(empresaA.principal().getId(), LocalDate.of(2026, 8, 12),
				empresaB.contexto())).isInstanceOf(br.app.criati.exception.CartaoCreditoNaoEncontradoException.class);
	}

	@Test
	void apiAbreListaBuscaRecompoeEFecha() throws Exception {
		Fixture fixture = fixture("56666666000406", PerfilUsuario.ADMINISTRADOR);
		criarCompraComParcela(fixture, fixture.principal(), new BigDecimal("75.50"), LocalDate.of(2026, 1, 12));
		MockHttpSession session = autenticar(fixture.usuario(), fixture.empresa());

		MvcResult resultado = mockMvc.perform(post(URL).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"cartaoId":"%s","competencia":"2026-01-01"}
						""".formatted(fixture.principal().getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ABERTA"))
				.andExpect(jsonPath("$.valorTotal").value(75.50))
				.andExpect(jsonPath("$.parcelas.length()").value(1))
				.andReturn();
		String id = com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(URL).session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get(URL + "/" + id).session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.cartaoPrincipalId")
						.value(fixture.principal().getId().toString()));
		mockMvc.perform(post(URL + "/" + id + "/recompor").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.valorTotal").value(75.50));
		mockMvc.perform(post(URL + "/" + id + "/fechar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FECHADA"));
		mockMvc.perform(post(URL + "/" + id + "/recompor").session(session).with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void apiExigeAutenticacaoCsrfEAdministradorParaEscrita() throws Exception {
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());

		Fixture gestor = fixture("57777777000407", PerfilUsuario.GESTOR);
		MockHttpSession session = autenticar(gestor.usuario(), gestor.empresa());
		String json = """
				{"cartaoId":"%s","competencia":"2026-08-12"}
				""".formatted(gestor.principal().getId());

		mockMvc.perform(post(URL).session(session).contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isForbidden());
		mockMvc.perform(post(URL).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isForbidden());
	}

	private Fixture fixture(String cnpj, PerfilUsuario perfil) {
		Empresa empresa = empresas.saveAndFlush(new Empresa("Empresa Fatura", "Empresa Fatura", cnpj,
				StatusCadastro.ATIVO));
		aplicacoes.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = usuarios.saveAndFlush(new Usuario("Usuario Fatura",
				"fatura." + cnpj + "@criati.test", encoder.encode(SENHA), StatusCadastro.ATIVO));
		vinculos.saveAndFlush(new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO));
		PessoaFinanceira pessoa = pessoas.saveAndFlush(new PessoaFinanceira(empresa, "Titular", null, null, usuario));
		InstituicaoFinanceira instituicao = instituicoes.saveAndFlush(
				new InstituicaoFinanceira(empresa, "Banco", "999", usuario));
		CartaoCredito principal = cartoes.saveAndFlush(new CartaoCredito(empresa, pessoa, instituicao, "Principal",
				TipoCartao.FISICO, null, Bandeira.VISA, "1234", new BigDecimal("5000.00"),
				new BigDecimal("3000.00"), 5, 12, null, usuario));
		CategoriaFinanceira categoria = categorias.saveAndFlush(new CategoriaFinanceira(empresa, "Compras",
				TipoFinanceiro.DESPESA, StatusCadastro.ATIVO));
		return new Fixture(empresa, usuario, pessoa, instituicao, principal, categoria,
				new ContextoEmpresaAtual(usuario.getId(), empresa.getId(), UUID.randomUUID(), perfil));
	}

	private CartaoCredito criarVirtual(Fixture fixture) {
		return cartoes.saveAndFlush(new CartaoCredito(fixture.empresa(), fixture.pessoa(), fixture.instituicao(),
				"Virtual", TipoCartao.VIRTUAL, fixture.principal(), Bandeira.VISA, "9999", null, null,
				null, null, null, fixture.usuario()));
	}

	private ParcelaCompraCartao criarCompraComParcela(Fixture fixture, CartaoCredito utilizado,
			BigDecimal valor, LocalDate competencia) {
		CompraCartao compra = compras.saveAndFlush(new CompraCartao(fixture.empresa(), utilizado,
				fixture.principal(), fixture.pessoa(), fixture.categoria(), null, "Compra", competencia.minusDays(10),
				valor, 1, null, fixture.usuario()));
		ParcelaCompraCartao parcela = new ParcelaCompraCartao(fixture.empresa(), compra, 1, 1, valor,
				competencia, fixture.usuario());
		compra.adicionarParcela(parcela);
		return parcelas.saveAndFlush(parcela);
	}

	private MockHttpSession autenticar(Usuario usuario, Empresa empresa) throws Exception {
		MvcResult login = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"%s","senha":"%s"}
						""".formatted(usuario.getEmail(), SENHA)))
				.andExpect(status().isOk()).andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		mockMvc.perform(post("/api/contexto/empresa-ativa").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"empresaId":"%s"}
						""".formatted(empresa.getId())))
				.andExpect(status().isOk());
		return session;
	}

	private record Fixture(Empresa empresa, Usuario usuario, PessoaFinanceira pessoa,
			InstituicaoFinanceira instituicao, CartaoCredito principal, CategoriaFinanceira categoria,
			ContextoEmpresaAtual contexto) {
	}
}
