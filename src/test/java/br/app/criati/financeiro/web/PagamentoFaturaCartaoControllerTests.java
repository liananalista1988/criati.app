package br.app.criati.financeiro.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.FaturaCartaoStatusInvalidoException;
import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.CompraCartao;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.FaturaCartao;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.PagamentoFaturaCartao;
import br.app.criati.financeiro.model.ParcelaCompraCartao;
import br.app.criati.financeiro.repository.CartaoCreditoRepository;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.CompraCartaoRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.FaturaCartaoRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.repository.PagamentoFaturaCartaoRepository;
import br.app.criati.financeiro.repository.ParcelaCompraCartaoRepository;
import br.app.criati.financeiro.service.CompraCartaoService;
import br.app.criati.financeiro.service.DashboardFinanceiroService;
import br.app.criati.financeiro.service.FaturaCartaoService;
import br.app.criati.financeiro.service.PagamentoFaturaCartaoService;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusFaturaCartao;
import br.app.criati.shared.enums.StatusParcelaCartao;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.shared.enums.TipoPagamentoFaturaCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

// LES-F3-005: pagamentos (integral/parcial/minimo) de FaturaCartao. Reaproveita
// o mesmo fixture de FaturaCartaoControllerTests (LES-F3-004). Datas escolhidas
// deliberadamente no passado (relativas a "hoje") para que fechar() seja
// sempre valido sem hacks; os dois testes que precisam distinguir
// PARCIALMENTE_PAGA de ATRASADA usam fixtureComFaturaFechadaNaoVencida, que
// calcula fechamento/vencimento relativos a "hoje" em vez de um calendario
// fixo (CRIATI-FIN-015A).
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PagamentoFaturaCartaoControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL = "/api/contexto/financeiro/faturas";

	@Autowired private MockMvc mockMvc;
	@Autowired private FaturaCartaoService faturaService;
	@Autowired private CompraCartaoService compraService;
	@Autowired private PagamentoFaturaCartaoService pagamentoService;
	@Autowired private EmpresaRepository empresas;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private UsuarioEmpresaRepository vinculos;
	@Autowired private PessoaFinanceiraRepository pessoas;
	@Autowired private InstituicaoFinanceiraRepository instituicoes;
	@Autowired private CartaoCreditoRepository cartoes;
	@Autowired private CategoriaFinanceiraRepository categorias;
	@Autowired private CompraCartaoRepository compras;
	@Autowired private ParcelaCompraCartaoRepository parcelas;
	@Autowired private ContaFinanceiraRepository contas;
	@Autowired private FaturaCartaoRepository faturas;
	@Autowired private PagamentoFaturaCartaoRepository pagamentos;
	@Autowired private AplicacaoService aplicacoes;
	@Autowired private PasswordEncoder encoder;
	@Autowired private DashboardFinanceiroService dashboardService;
	@Autowired private PlatformTransactionManager transactionManager;

	@Test
	void pagamentoIntegralQuitaFaturaLiberaLimiteEGeraUmLancamentoForaDoGastoReal() throws Exception {
		Fixture f = fixture("61111111000501");
		criarCompraComParcela(f, new BigDecimal("100.00"), LocalDate.of(2026, 1, 12));
		var aberta = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 1, 12), f.contexto());
		faturaService.fechar(aberta.fatura().getId(), f.contexto());
		BigDecimal comprometidoAntes = compraService.comprometido(f.empresa().getId(), f.principal().getId());
		assertThat(comprometidoAntes).isEqualByComparingTo("100.00");

		PagamentoFaturaCartao pagamento = pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(),
				LocalDate.of(2026, 1, 20), null, TipoPagamentoFaturaCartao.INTEGRAL, null, f.contexto());

		FaturaCartao fatura = faturas.findByIdAndEmpresaId(aberta.fatura().getId(), f.empresa().getId()).orElseThrow();
		assertThat(fatura.getStatus()).isEqualTo(StatusFaturaCartao.PAGA);
		assertThat(pagamento.getValor()).isEqualByComparingTo("100.00");
		assertThat(pagamentos.findAllByEmpresaIdAndFaturaIdOrderByDataPagamentoDescCriadoEmDesc(
				f.empresa().getId(), fatura.getId())).hasSize(1);

		LancamentoFinanceiro lancamento = pagamento.getLancamentoFinanceiro();
		assertThat(lancamento.getTipo()).isEqualTo(TipoFinanceiro.DESPESA);
		assertThat(lancamento.getCategoria().getNome()).isEqualTo("Pagamento de fatura de cartao");
		assertThat(lancamento.compoeSaldoRealizado()).isTrue();

		// pagamento integral libera o limite das parcelas da fatura paga
		BigDecimal comprometidoDepois = compraService.comprometido(f.empresa().getId(), f.principal().getId());
		assertThat(comprometidoDepois).isEqualByComparingTo("0.00");

		List<ParcelaCompraCartao> parcelasDaFatura = parcelas.findAllByEmpresaIdAndFaturaIdOrderById(
				f.empresa().getId(), fatura.getId());
		assertThat(parcelasDaFatura).extracting(ParcelaCompraCartao::getStatus)
				.allMatch(status -> status == StatusParcelaCartao.ABERTA);

		// gasto real do mes nao duplica o consumo ja reconhecido na compra
		var dashboard = dashboardService.gerar(f.contexto(), YearMonth.of(2026, 1));
		assertThat(dashboard.despesasPagas()).isEqualByComparingTo("0.00");
	}

	@Test
	void pagamentoParcialNaoLiberaLimiteNemAlteraParcelasIndividualmente() throws Exception {
		CenarioFaturaFechadaNaoVencida cenario = fixtureComFaturaFechadaNaoVencida(
				"62222222000502", new BigDecimal("100.00"));
		Fixture f = cenario.fixture();
		var aberta = cenario.fatura();

		pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(), LocalDate.now(),
				new BigDecimal("40.00"), TipoPagamentoFaturaCartao.PARCIAL, null, f.contexto());

		FaturaCartao fatura = faturas.findByIdAndEmpresaId(aberta.fatura().getId(), f.empresa().getId()).orElseThrow();
		assertThat(fatura.getStatus()).isEqualTo(StatusFaturaCartao.PARCIALMENTE_PAGA);
		BigDecimal comprometido = compraService.comprometido(f.empresa().getId(), f.principal().getId());
		assertThat(comprometido).isEqualByComparingTo("100.00");
		List<ParcelaCompraCartao> parcelasDaFatura = parcelas.findAllByEmpresaIdAndFaturaIdOrderById(
				f.empresa().getId(), fatura.getId());
		assertThat(parcelasDaFatura).extracting(ParcelaCompraCartao::getStatus)
				.allMatch(status -> status == StatusParcelaCartao.ABERTA);
	}

	@Test
	void pagamentoMinimoAcumulaIgualAoParcial() throws Exception {
		CenarioFaturaFechadaNaoVencida cenario = fixtureComFaturaFechadaNaoVencida(
				"62233333000512", new BigDecimal("100.00"));
		Fixture f = cenario.fixture();
		var aberta = cenario.fatura();

		pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(), LocalDate.now(),
				new BigDecimal("15.00"), TipoPagamentoFaturaCartao.MINIMO, null, f.contexto());

		FaturaCartao fatura = faturas.findByIdAndEmpresaId(aberta.fatura().getId(), f.empresa().getId()).orElseThrow();
		assertThat(fatura.getStatus()).isEqualTo(StatusFaturaCartao.PARCIALMENTE_PAGA);
		assertThat(pagamentoService.totalPago(fatura.getId(), f.contexto())).isEqualByComparingTo("15.00");
	}

	@Test
	void segundoPagamentoSomaAoPrimeiroSemTipoProprioEQuitaFatura() throws Exception {
		Fixture f = fixture("63333333000503");
		criarCompraComParcela(f, new BigDecimal("100.00"), LocalDate.of(2026, 3, 12));
		var aberta = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 3, 12), f.contexto());
		faturaService.fechar(aberta.fatura().getId(), f.contexto());

		pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(), LocalDate.of(2026, 3, 15),
				new BigDecimal("60.00"), TipoPagamentoFaturaCartao.PARCIAL, null, f.contexto());
		pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(), LocalDate.of(2026, 3, 20),
				new BigDecimal("40.00"), TipoPagamentoFaturaCartao.PARCIAL, null, f.contexto());

		FaturaCartao fatura = faturas.findByIdAndEmpresaId(aberta.fatura().getId(), f.empresa().getId()).orElseThrow();
		assertThat(fatura.getStatus()).isEqualTo(StatusFaturaCartao.PAGA);
		assertThat(pagamentos.findAllByEmpresaIdAndFaturaIdOrderByDataPagamentoDescCriadoEmDesc(
				f.empresa().getId(), fatura.getId())).hasSize(2);
	}

	@Test
	void pagamentoExcedenteERejeitado() throws Exception {
		Fixture f = fixture("64444444000504");
		criarCompraComParcela(f, new BigDecimal("50.00"), LocalDate.of(2026, 4, 12));
		var aberta = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 4, 12), f.contexto());
		faturaService.fechar(aberta.fatura().getId(), f.contexto());

		assertThatThrownBy(() -> pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(),
				LocalDate.of(2026, 4, 20), new BigDecimal("50.01"), TipoPagamentoFaturaCartao.PARCIAL, null,
				f.contexto())).isInstanceOf(DadosInvalidosException.class);
	}

	@Test
	void faturaAbertaRejeitaPagamento() throws Exception {
		Fixture f = fixture("65555555000505");
		criarCompraComParcela(f, new BigDecimal("30.00"), LocalDate.of(2026, 5, 12));
		var aberta = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 5, 12), f.contexto());

		assertThatThrownBy(() -> pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(),
				LocalDate.of(2026, 5, 20), new BigDecimal("30.00"), TipoPagamentoFaturaCartao.INTEGRAL, null,
				f.contexto())).isInstanceOf(FaturaCartaoStatusInvalidoException.class);
	}

	@Test
	void faturaJaPagaRejeitaNovoPagamento() throws Exception {
		Fixture f = fixture("66666666000506");
		criarCompraComParcela(f, new BigDecimal("20.00"), LocalDate.of(2026, 6, 12));
		var aberta = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 6, 12), f.contexto());
		faturaService.fechar(aberta.fatura().getId(), f.contexto());
		pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(), LocalDate.of(2026, 6, 20),
				null, TipoPagamentoFaturaCartao.INTEGRAL, null, f.contexto());

		assertThatThrownBy(() -> pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(),
				LocalDate.of(2026, 6, 21), new BigDecimal("1.00"), TipoPagamentoFaturaCartao.PARCIAL, null,
				f.contexto())).isInstanceOf(FaturaCartaoStatusInvalidoException.class);
	}

	@Test
	void saldoFinanciadoEntraAutomaticaEIdempotentementeNaProximaFatura() throws Exception {
		Fixture f = fixture("67777777000507");
		criarCompraComParcela(f, new BigDecimal("100.00"), LocalDate.of(2026, 1, 12));
		var faturaJaneiro = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 1, 12), f.contexto());
		faturaService.fechar(faturaJaneiro.fatura().getId(), f.contexto());
		pagamentoService.registrarPagamento(faturaJaneiro.fatura().getId(), f.conta().getId(),
				LocalDate.of(2026, 1, 20), new BigDecimal("30.00"), TipoPagamentoFaturaCartao.PARCIAL, null,
				f.contexto());

		criarCompraComParcela(f, new BigDecimal("10.00"), LocalDate.of(2026, 2, 12));
		var faturaFevereiro = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 2, 12), f.contexto());
		FaturaCartao fevereiro = faturas.findByIdAndEmpresaId(faturaFevereiro.fatura().getId(), f.empresa().getId())
				.orElseThrow();
		assertThat(fevereiro.getSaldoFinanciadoAnterior()).isEqualByComparingTo("70.00");
		assertThat(fevereiro.getValorDevido()).isEqualByComparingTo("80.00");

		// idempotencia: reabrir a mesma competencia nao duplica o saldo herdado
		var faturaFevereiroNovamente = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 2, 12),
				f.contexto());
		FaturaCartao fevereiroNovamente = faturas.findByIdAndEmpresaId(
				faturaFevereiroNovamente.fatura().getId(), f.empresa().getId()).orElseThrow();
		assertThat(fevereiroNovamente.getId()).isEqualTo(fevereiro.getId());
		assertThat(fevereiroNovamente.getSaldoFinanciadoAnterior()).isEqualByComparingTo("70.00");
	}

	@Test
	void aplicarEncargosAumentaValorDevidoENaoAlteraPagamentosJaFeitos() throws Exception {
		Fixture f = fixture("68888888000508");
		criarCompraComParcela(f, new BigDecimal("100.00"), LocalDate.of(2026, 3, 12));
		var aberta = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 3, 12), f.contexto());
		faturaService.fechar(aberta.fatura().getId(), f.contexto());

		FaturaCartao faturaComEncargos = pagamentoService.aplicarEncargos(aberta.fatura().getId(),
				new BigDecimal("5.00"), new BigDecimal("2.00"), f.contexto());

		assertThat(faturaComEncargos.getValorDevido()).isEqualByComparingTo("107.00");
		assertThatThrownBy(() -> pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(),
				LocalDate.of(2026, 3, 20), new BigDecimal("107.01"), TipoPagamentoFaturaCartao.PARCIAL, null,
				f.contexto())).isInstanceOf(DadosInvalidosException.class);

		pagamentoService.registrarPagamento(aberta.fatura().getId(), f.conta().getId(), LocalDate.of(2026, 3, 20),
				null, TipoPagamentoFaturaCartao.INTEGRAL, null, f.contexto());
		FaturaCartao quitada = faturas.findByIdAndEmpresaId(aberta.fatura().getId(), f.empresa().getId()).orElseThrow();
		assertThat(quitada.getStatus()).isEqualTo(StatusFaturaCartao.PAGA);
	}

	@Test
	void isolamentoMultiempresaNaoRevelaFaturaAlheiaAoPagar() throws Exception {
		Fixture empresaA = fixture("69999999000509");
		Fixture empresaB = fixture("61010101000510");
		criarCompraComParcela(empresaA, new BigDecimal("50.00"), LocalDate.of(2026, 4, 12));
		var faturaA = faturaService.abrir(empresaA.principal().getId(), LocalDate.of(2026, 4, 12), empresaA.contexto());
		faturaService.fechar(faturaA.fatura().getId(), empresaA.contexto());

		assertThatThrownBy(() -> pagamentoService.registrarPagamento(faturaA.fatura().getId(),
				empresaB.conta().getId(), LocalDate.of(2026, 4, 20), null, TipoPagamentoFaturaCartao.INTEGRAL, null,
				empresaB.contexto()))
				.isInstanceOf(br.app.criati.exception.FaturaCartaoNaoEncontradaException.class);
	}

	@Test
	void concorrenciaDoisPagamentosSimultaneosNaoQuitamAlemDoDevido() throws Exception {
		Fixture f = fixture("62020202000511");
		criarCompraComParcela(f, new BigDecimal("100.00"), LocalDate.of(2026, 5, 12));
		var aberta = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 5, 12), f.contexto());
		faturaService.fechar(aberta.fatura().getId(), f.contexto());
		UUID faturaId = aberta.fatura().getId();

		TransactionTemplate transacao = new TransactionTemplate(transactionManager);
		CountDownLatch primeiraTravou = new CountDownLatch(1);
		AtomicReference<Throwable> erroSegunda = new AtomicReference<>();

		Thread primeira = new Thread(() -> transacao.executeWithoutResult(status -> {
			pagamentoService.registrarPagamento(faturaId, f.conta().getId(), LocalDate.of(2026, 5, 20),
					new BigDecimal("70.00"), TipoPagamentoFaturaCartao.PARCIAL, null, f.contexto());
			primeiraTravou.countDown();
			dormir(400);
		}), "pagamento-1");
		Thread segunda = new Thread(() -> {
			aguardar(primeiraTravou);
			try {
				transacao.executeWithoutResult(status -> pagamentoService.registrarPagamento(faturaId,
						f.conta().getId(), LocalDate.of(2026, 5, 21), new BigDecimal("70.00"),
						TipoPagamentoFaturaCartao.PARCIAL, null, f.contexto()));
			} catch (Throwable erro) {
				erroSegunda.set(erro);
			}
		}, "pagamento-2");

		primeira.start();
		segunda.start();
		primeira.join(Duration.ofSeconds(10).toMillis());
		segunda.join(Duration.ofSeconds(10).toMillis());

		// a segunda so enxerga o saldo apos a primeira liberar o lock (commit) -
		// nesse momento restam 30.00 de saldo devido, entao 70.00 excede e e
		// rejeitada: o lock pessimista impede que as duas somem 140.00 sobre uma
		// fatura de 100.00.
		assertThat(erroSegunda.get()).isInstanceOf(DadosInvalidosException.class);
		BigDecimal totalPago = pagamentoService.totalPago(faturaId, f.contexto());
		assertThat(totalPago).isEqualByComparingTo("70.00");
	}

	@Test
	void pagamentosConcorrentesCriamUmaUnicaCategoriaTecnicaPorEmpresa() throws Exception {
		Fixture f = fixture("62121212000514");
		criarCompraComParcela(f, new BigDecimal("50.00"), LocalDate.of(2026, 1, 12));
		var janeiro = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 1, 12), f.contexto());
		faturaService.fechar(janeiro.fatura().getId(), f.contexto());
		criarCompraComParcela(f, new BigDecimal("50.00"), LocalDate.of(2026, 2, 12));
		var fevereiro = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 2, 12), f.contexto());
		faturaService.fechar(fevereiro.fatura().getId(), f.contexto());

		CountDownLatch prontas = new CountDownLatch(2);
		CountDownLatch iniciar = new CountDownLatch(1);
		List<Throwable> erros = new CopyOnWriteArrayList<>();
		Thread primeira = pagamentoConcorrente(janeiro.fatura().getId(), f, prontas, iniciar, erros, "categoria-1");
		Thread segunda = pagamentoConcorrente(fevereiro.fatura().getId(), f, prontas, iniciar, erros, "categoria-2");

		primeira.start();
		segunda.start();
		assertThat(prontas.await(5, TimeUnit.SECONDS)).isTrue();
		iniciar.countDown();
		primeira.join(Duration.ofSeconds(10).toMillis());
		segunda.join(Duration.ofSeconds(10).toMillis());

		assertThat(primeira.isAlive()).isFalse();
		assertThat(segunda.isAlive()).isFalse();
		assertThat(erros).isEmpty();
		assertThat(categorias.findAllByEmpresaId(f.empresa().getId()).stream()
				.filter(categoria -> "PAGAMENTO_FATURA_CARTAO".equals(categoria.getCodigoSistema())))
				.hasSize(1);
		assertThat(pagamentos.findAllByEmpresaIdAndFaturaIdOrderByDataPagamentoDescCriadoEmDesc(
				f.empresa().getId(), janeiro.fatura().getId())).hasSize(1);
		assertThat(pagamentos.findAllByEmpresaIdAndFaturaIdOrderByDataPagamentoDescCriadoEmDesc(
				f.empresa().getId(), fevereiro.fatura().getId())).hasSize(1);
	}

	@Test
	void apiRegistraEListaPagamentoExigindoAdministradorECsrf() throws Exception {
		Fixture f = fixture("63030303000513");
		criarCompraComParcela(f, new BigDecimal("80.00"), LocalDate.of(2026, 6, 12));
		var aberta = faturaService.abrir(f.principal().getId(), LocalDate.of(2026, 6, 12), f.contexto());
		faturaService.fechar(aberta.fatura().getId(), f.contexto());
		MockHttpSession session = autenticar(f.usuario(), f.empresa());
		String faturaId = aberta.fatura().getId().toString();

		MvcResult resultado = mockMvc.perform(post(URL + "/" + faturaId + "/pagamentos").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaPagamentoId":"%s","dataPagamento":"2026-06-20","tipo":"INTEGRAL"}
						""".formatted(f.conta().getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.tipo").value("INTEGRAL"))
				.andExpect(jsonPath("$.valor").value(80.00))
				.andReturn();
		assertThat(resultado.getResponse().getContentAsString()).doesNotContain("senha");

		mockMvc.perform(get(URL + "/" + faturaId).session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PAGA"))
				.andExpect(jsonPath("$.valorPago").value(80.00))
				.andExpect(jsonPath("$.saldoDevido").value(0.00));
		mockMvc.perform(get(URL + "/" + faturaId + "/pagamentos").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(post(URL + "/" + faturaId + "/pagamentos").session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"contaPagamentoId":"%s","dataPagamento":"2026-06-21","tipo":"PARCIAL","valor":1}
						""".formatted(f.conta().getId())))
				.andExpect(status().isForbidden());
	}

	private Fixture fixture(String cnpj) {
		return fixtureComCiclo(cnpj, 5, 12);
	}

	private Fixture fixtureComCiclo(String cnpj, int diaFechamento, int diaVencimento) {
		Empresa empresa = empresas.saveAndFlush(new Empresa("Empresa Pagamento Fatura", "Empresa Pagamento Fatura", cnpj,
				StatusCadastro.ATIVO));
		aplicacoes.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = usuarios.saveAndFlush(new Usuario("Usuario Pagamento",
				"pagamento." + cnpj + "@criati.test", encoder.encode(SENHA), StatusCadastro.ATIVO));
		vinculos.saveAndFlush(new UsuarioEmpresa(usuario, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));
		PessoaFinanceira pessoa = pessoas.saveAndFlush(new PessoaFinanceira(empresa, "Titular", null, null, usuario));
		InstituicaoFinanceira instituicao = instituicoes.saveAndFlush(
				new InstituicaoFinanceira(empresa, "Banco", "999", usuario));
		CartaoCredito principal = cartoes.saveAndFlush(new CartaoCredito(empresa, pessoa, instituicao, "Principal",
				TipoCartao.FISICO, null, Bandeira.VISA, "1234", new BigDecimal("5000.00"),
				new BigDecimal("3000.00"), diaFechamento, diaVencimento, null, usuario));
		CategoriaFinanceira categoria = categorias.saveAndFlush(new CategoriaFinanceira(empresa, "Compras",
				TipoFinanceiro.DESPESA, StatusCadastro.ATIVO));
		ContaFinanceira conta = contas.saveAndFlush(new ContaFinanceira(empresa, "Conta Corrente",
				TipoContaFinanceira.CONTA_CORRENTE, BigDecimal.ZERO, StatusCadastro.ATIVO));
		return new Fixture(empresa, usuario, pessoa, instituicao, principal, categoria, conta,
				new ContextoEmpresaAtual(usuario.getId(), empresa.getId(), UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR));
	}

	// Fecha a fatura no mesmo dia da execucao do teste e com vencimento apenas
	// um dia a frente (dentro do mesmo mes, clampado no proprio "hoje" quando
	// "hoje" ja e o ultimo dia do mes) - assim fechar() permanece sempre
	// permitido (hoje >= dataFechamento) e o status apos um pagamento
	// parcial/minimo permanece deterministicamente PARCIALMENTE_PAGA
	// (hoje <= dataVencimento), em vez de depender de um vencimento fixo no
	// calendario que se torna ATRASADA assim que a execucao real ultrapassa
	// aquela data (ver FaturaCartao#recalcularStatus). Usado apenas pelos
	// dois testes que precisam distinguir PARCIALMENTE_PAGA de ATRASADA; os
	// demais testes do arquivo nao fazem essa distincao e por isso toleram
	// datas absolutas no passado sem ficarem frageis.
	private CenarioFaturaFechadaNaoVencida fixtureComFaturaFechadaNaoVencida(String cnpj, BigDecimal valorParcela)
			throws Exception {
		LocalDate hoje = LocalDate.now();
		YearMonth mesAtual = YearMonth.from(hoje);
		int diaFechamento = hoje.getDayOfMonth();
		int diaVencimento = Math.min(diaFechamento + 1, mesAtual.lengthOfMonth());
		LocalDate competencia = mesAtual.atDay(diaVencimento);
		Fixture f = fixtureComCiclo(cnpj, diaFechamento, diaVencimento);
		criarCompraComParcela(f, valorParcela, competencia);
		var aberta = faturaService.abrir(f.principal().getId(), competencia, f.contexto());
		faturaService.fechar(aberta.fatura().getId(), f.contexto());
		return new CenarioFaturaFechadaNaoVencida(f, aberta);
	}

	private record CenarioFaturaFechadaNaoVencida(Fixture fixture, FaturaCartaoService.ResultadoFatura fatura) {
	}

	private ParcelaCompraCartao criarCompraComParcela(Fixture fixture, BigDecimal valor, LocalDate competencia) {
		CompraCartao compra = compras.saveAndFlush(new CompraCartao(fixture.empresa(), fixture.principal(),
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

	private void dormir(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void aguardar(CountDownLatch latch) {
		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private Thread pagamentoConcorrente(UUID faturaId, Fixture fixture, CountDownLatch prontas,
			CountDownLatch iniciar, List<Throwable> erros, String nome) {
		return new Thread(() -> {
			prontas.countDown();
			aguardar(iniciar);
			try {
				pagamentoService.registrarPagamento(faturaId, fixture.conta().getId(), LocalDate.of(2026, 2, 20),
						new BigDecimal("10.00"), TipoPagamentoFaturaCartao.PARCIAL, null, fixture.contexto());
			} catch (Throwable erro) {
				erros.add(erro);
			}
		}, nome);
	}

	private record Fixture(Empresa empresa, Usuario usuario, PessoaFinanceira pessoa,
			InstituicaoFinanceira instituicao, CartaoCredito principal, CategoriaFinanceira categoria,
			ContaFinanceira conta, ContextoEmpresaAtual contexto) {
	}
}
