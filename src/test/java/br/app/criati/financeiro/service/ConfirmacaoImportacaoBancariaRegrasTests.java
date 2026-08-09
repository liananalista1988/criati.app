package br.app.criati.financeiro.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.CompraCartao;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.EmprestimoConcedido;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.ParcelaCompraCartao;
import br.app.criati.financeiro.model.ParcelaEmprestimo;
import br.app.criati.financeiro.model.RegraClassificacaoImportacao;
import br.app.criati.financeiro.model.TransacaoBancariaImportada;
import br.app.criati.financeiro.repository.CartaoCreditoRepository;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.CompraCartaoRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.EmprestimoConcedidoRepository;
import br.app.criati.financeiro.repository.FaturaCartaoRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.ParcelaCompraCartaoRepository;
import br.app.criati.financeiro.repository.ParcelaEmprestimoRepository;
import br.app.criati.financeiro.repository.RecebimentoParcelaEmprestimoRepository;
import br.app.criati.financeiro.repository.RegraClassificacaoImportacaoRepository;
import br.app.criati.financeiro.repository.TransacaoBancariaImportadaRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.AplicacaoRegraClassificacaoImportacao;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.EncaminhamentoSugeridoRegraImportacao;
import br.app.criati.shared.enums.EstrategiaComparacaoRegraImportacao;
import br.app.criati.shared.enums.FormaPagamentoEmprestimo;
import br.app.criati.shared.enums.NivelConfiancaRegraImportacao;
import br.app.criati.shared.enums.OrigemClassificacaoTransacaoImportada;
import br.app.criati.shared.enums.OrigemLancamentoFinanceiro;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.SituacaoTransacaoImportada;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusFaturaCartao;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Cobre a integracao de regras de classificacao com a confirmacao de
 * transacoes importadas (CRIATI-IMP-002A): aceite de sugestao/automatica,
 * alteracao material voltando para MANUAL, e o encaminhamento de pagamento de
 * fatura sem duplicar despesa.
 */
// @Transactional na classe (rollback automatico por metodo, evita vazar
// dados committed para outros testes que fazem contagem global, ex.:
// ContaFinanceiraControllerTests). O teste de pagamento de fatura pre-cria a
// categoria tecnica "PAGAMENTO_FATURA_CARTAO" (mesmo codigo usado por
// PagamentoFaturaCartaoService) para que o service a encontre por
// findByEmpresaIdAndCodigoSistema e nunca precise cria-la sozinho - isso evita
// depender do caminho PROPAGATION_REQUIRES_NEW de
// PagamentoFaturaCartaoService.criarCategoriaTecnica, que roda em conexao
// separada e so enxergaria dados ja commitados (incompativel com a transacao
// unica e nao commitada deste teste).
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConfirmacaoImportacaoBancariaRegrasTests {

	private static final String SENHA = "senha-correta";

	@Autowired private ImportacaoBancariaService importacaoService;
	@Autowired private ConfirmacaoImportacaoBancariaService confirmacaoService;
	@Autowired private RegraClassificacaoImportacaoService regraService;
	@Autowired private RegraClassificacaoImportacaoRepository regraRepository;
	@Autowired private TransacaoBancariaImportadaRepository transacaoRepository;
	@Autowired private LancamentoFinanceiroRepository lancamentoRepository;
	@Autowired private EmpresaRepository empresaRepository;
	@Autowired private UsuarioRepository usuarioRepository;
	@Autowired private UsuarioEmpresaRepository usuarioEmpresaRepository;
	@Autowired private PessoaFinanceiraRepository pessoaRepository;
	@Autowired private InstituicaoFinanceiraRepository instituicaoRepository;
	@Autowired private ContaFinanceiraRepository contaRepository;
	@Autowired private CategoriaFinanceiraRepository categoriaRepository;
	@Autowired private CartaoCreditoRepository cartaoRepository;
	@Autowired private CompraCartaoRepository compraRepository;
	@Autowired private ParcelaCompraCartaoRepository parcelaRepository;
	@Autowired private FaturaCartaoRepository faturaRepository;
	@Autowired private FaturaCartaoService faturaService;
	@Autowired private AplicacaoService aplicacaoService;
	@Autowired private PasswordEncoder passwordEncoder;
	@Autowired private ParteFinanceiraRepository parteRepository;
	@Autowired private EmprestimoConcedidoRepository emprestimoRepository;
	@Autowired private ParcelaEmprestimoRepository parcelaEmprestimoRepository;
	@Autowired private RecebimentoParcelaEmprestimoRepository recebimentoParcelaEmprestimoRepository;

	@Test
	void aceitarSugestaoIncrementaContadorERegistraOrigemRegraSugerida() {
		Fixture f = fixture("21111111000801");
		CategoriaFinanceira categoria = categoria(f, "Mercado", TipoFinanceiro.DESPESA);
		RegraClassificacaoImportacao regra = criarRegra(f, categoria, "SUPERMERCADO",
				EstrategiaComparacaoRegraImportacao.CONTEM, AplicacaoRegraClassificacaoImportacao.SUGESTAO);
		TransacaoBancariaImportada transacao = importarUmaTransacao(f, "-50.00", "SUPERMERCADO CENTRAL");

		confirmacaoService.confirmar(transacao.getLote().getId(), List.of(new ConfirmacaoTransacaoImportada(
				transacao.getId(), categoria.getId(), null, null, regra.getId(), "Compra mercado", false)), f.contexto());

		TransacaoBancariaImportada confirmada = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(f.empresa().getId(), transacao.getLote().getId())
				.get(0);
		assertThat(confirmada.getOrigemClassificacao()).isEqualTo(OrigemClassificacaoTransacaoImportada.REGRA_SUGERIDA);
		assertThat(confirmada.getRegraClassificacao().getId()).isEqualTo(regra.getId());
		RegraClassificacaoImportacao regraAtualizada = regraRepository
				.findByIdAndEmpresaId(regra.getId(), f.empresa().getId()).orElseThrow();
		assertThat(regraAtualizada.getQuantidadeUtilizacoes()).isEqualTo(1);
		assertThat(regraAtualizada.getUltimaUtilizacaoEm()).isNotNull();
	}

	@Test
	void regraAutomaticaSoPreencheNuncaConfirmaOuMovimentaSozinha() {
		Fixture f = fixture("22222222000802");
		CategoriaFinanceira categoria = categoria(f, "Assinaturas", TipoFinanceiro.DESPESA);
		RegraClassificacaoImportacao regra = criarRegra(f, categoria, "NETFLIX",
				EstrategiaComparacaoRegraImportacao.IGUAL, AplicacaoRegraClassificacaoImportacao.AUTOMATICA);
		TransacaoBancariaImportada transacao = importarUmaTransacao(f, "-39.90", "NETFLIX");

		// A sugestao existe (regra automatica bateria com a descricao), mas
		// nada foi confirmado ainda - a transacao continua pendente e nenhum
		// lancamento foi criado so por a regra existir (ajuste obrigatorio 1).
		var sugestao = regraService.sugerirParaTransacao(
				transacao.getDescricao(), f.conta().getId(), TipoFinanceiro.DESPESA, f.contexto());
		assertThat(sugestao).isPresent();
		assertThat(sugestao.get().getId()).isEqualTo(regra.getId());
		TransacaoBancariaImportada aindaPendente = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(f.empresa().getId(), transacao.getLote().getId())
				.get(0);
		assertThat(aindaPendente.getSituacao()).isEqualTo(SituacaoTransacaoImportada.PENDENTE);
		assertThat(lancamentoRepository.findAllByEmpresaId(f.empresa().getId())).isEmpty();

		// So apos confirmacao explicita e que o lancamento e criado.
		confirmacaoService.confirmar(transacao.getLote().getId(), List.of(new ConfirmacaoTransacaoImportada(
				transacao.getId(), categoria.getId(), null, null, regra.getId(), "Netflix", false)), f.contexto());

		TransacaoBancariaImportada confirmada = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(f.empresa().getId(), transacao.getLote().getId())
				.get(0);
		assertThat(confirmada.getOrigemClassificacao()).isEqualTo(OrigemClassificacaoTransacaoImportada.REGRA_AUTOMATICA);
		assertThat(lancamentoRepository.findAllByEmpresaId(f.empresa().getId())).hasSize(1);
		assertThat(regraRepository.findByIdAndEmpresaId(regra.getId(), f.empresa().getId())
				.orElseThrow().getQuantidadeUtilizacoes()).isEqualTo(1);
	}

	@Test
	void alterarCategoriaSugeridaAntesDeConfirmarVoltaParaManualSemIncrementarRegra() {
		Fixture f = fixture("23333333000803");
		CategoriaFinanceira categoriaSugerida = categoria(f, "Mercado", TipoFinanceiro.DESPESA);
		CategoriaFinanceira categoriaEscolhida = categoria(f, "Lazer", TipoFinanceiro.DESPESA);
		RegraClassificacaoImportacao regra = criarRegra(f, categoriaSugerida, "SUPERMERCADO",
				EstrategiaComparacaoRegraImportacao.CONTEM, AplicacaoRegraClassificacaoImportacao.SUGESTAO);
		TransacaoBancariaImportada transacao = importarUmaTransacao(f, "-50.00", "SUPERMERCADO CENTRAL");

		// Usuario recebeu a sugestao (regraClassificacaoId enviado) mas mudou a
		// categoria antes de confirmar - alteracao material invalida a
		// sugestao (ajuste obrigatorio 9).
		confirmacaoService.confirmar(transacao.getLote().getId(), List.of(new ConfirmacaoTransacaoImportada(
				transacao.getId(), categoriaEscolhida.getId(), null, null, regra.getId(), "Cinema", false)), f.contexto());

		TransacaoBancariaImportada confirmada = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(f.empresa().getId(), transacao.getLote().getId())
				.get(0);
		assertThat(confirmada.getOrigemClassificacao()).isEqualTo(OrigemClassificacaoTransacaoImportada.MANUAL);
		assertThat(confirmada.getRegraClassificacao()).isNull();
		assertThat(regraRepository.findByIdAndEmpresaId(regra.getId(), f.empresa().getId())
				.orElseThrow().getQuantidadeUtilizacoes()).isZero();
	}

	@Test
	void regraDeOutraEmpresaReferenciadaNaConfirmacaoEIgnoradaSilenciosamenteViraManual() {
		Fixture a = fixture("24444444000804");
		Fixture b = fixture("25555555000805");
		CategoriaFinanceira categoriaB = categoria(b, "Mercado B", TipoFinanceiro.DESPESA);
		RegraClassificacaoImportacao regraDeB = criarRegra(b, categoriaB, "MERCADO",
				EstrategiaComparacaoRegraImportacao.CONTEM, AplicacaoRegraClassificacaoImportacao.SUGESTAO);
		CategoriaFinanceira categoriaA = categoria(a, "Mercado A", TipoFinanceiro.DESPESA);
		TransacaoBancariaImportada transacao = importarUmaTransacao(a, "-30.00", "MERCADO QUALQUER");

		confirmacaoService.confirmar(transacao.getLote().getId(), List.of(new ConfirmacaoTransacaoImportada(
				transacao.getId(), categoriaA.getId(), null, null, regraDeB.getId(), "Compra", false)), a.contexto());

		TransacaoBancariaImportada confirmada = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(a.empresa().getId(), transacao.getLote().getId())
				.get(0);
		assertThat(confirmada.getOrigemClassificacao()).isEqualTo(OrigemClassificacaoTransacaoImportada.MANUAL);
		assertThat(confirmada.getRegraClassificacao()).isNull();
	}

	@Test
	void pagamentoDeFaturaViaConfirmacaoNaoDuplicaDespesaEQuitaFatura() {
		Fixture f = fixture("26666666000806");
		// Pre-cria a categoria tecnica que PagamentoFaturaCartaoService usaria
		// (mesmo codigo de sistema) para que o service a encontre em vez de
		// precisar cria-la sozinho numa transacao PROPAGATION_REQUIRES_NEW -
		// essa transacao roda em conexao separada e so enxergaria dados ja
		// commitados, incompativel com a transacao unica deste teste.
		categoriaRepository.saveAndFlush(CategoriaFinanceira.criarTecnica(
				f.empresa(), "PAGAMENTO_FATURA_CARTAO", "Pagamento de fatura de cartao", TipoFinanceiro.DESPESA));
		CompraCartao compra = compraRepository.saveAndFlush(new CompraCartao(f.empresa(), f.cartao(), f.cartao(),
				f.pessoa(), categoria(f, "Compras cartao", TipoFinanceiro.DESPESA), null, "Compra",
				LocalDate.of(2026, 1, 2), new BigDecimal("100.00"), 1, null, f.usuario()));
		ParcelaCompraCartao parcela = new ParcelaCompraCartao(f.empresa(), compra, 1, 1, new BigDecimal("100.00"),
				LocalDate.of(2026, 1, 12), f.usuario());
		compra.adicionarParcela(parcela);
		parcelaRepository.saveAndFlush(parcela);
		var aberta = faturaService.abrir(f.cartao().getId(), LocalDate.of(2026, 1, 12), f.contexto());
		faturaService.fechar(aberta.fatura().getId(), f.contexto());
		UUID faturaId = aberta.fatura().getId();

		TransacaoBancariaImportada transacaoPagamento = importarUmaTransacao(f, "-100.00", "PAGAMENTO FATURA CARTAO");

		confirmacaoService.confirmar(transacaoPagamento.getLote().getId(), List.of(new ConfirmacaoTransacaoImportada(
				transacaoPagamento.getId(), null, faturaId, null, null, "Pagamento fatura", false)), f.contexto());

		assertThat(faturaRepository.findByIdAndEmpresaId(faturaId, f.empresa().getId()).orElseThrow().getStatus())
				.isEqualTo(StatusFaturaCartao.PAGA);
		// Nenhum LancamentoFinanceiro generico de importacao foi criado - so o
		// unico lancamento gerado pelo pagamento da fatura (categoria tecnica),
		// nunca duplicando a despesa da compra original.
		List<LancamentoFinanceiro> lancamentos = lancamentoRepository.findAllByEmpresaId(f.empresa().getId());
		assertThat(lancamentos).hasSize(1);
		assertThat(lancamentos.get(0).getOrigem()).isNotEqualTo(OrigemLancamentoFinanceiro.IMPORTACAO);
		TransacaoBancariaImportada confirmada = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(f.empresa().getId(), transacaoPagamento.getLote().getId())
				.get(0);
		assertThat(confirmada.getLancamentoFinanceiro().getId()).isEqualTo(lancamentos.get(0).getId());
	}

	@Test
	void confirmarComCategoriaEFaturaJuntosOuNenhumERejeitado() throws Exception {
		Fixture f = fixture("27777777000807");
		CategoriaFinanceira categoria = categoria(f, "Mercado", TipoFinanceiro.DESPESA);
		TransacaoBancariaImportada transacao = importarUmaTransacao(f, "-10.00", "QUALQUER");

		assertThatThrownBy(() -> confirmacaoService.confirmar(transacao.getLote().getId(),
				List.of(new ConfirmacaoTransacaoImportada(transacao.getId(), categoria.getId(),
						UUID.randomUUID(), null, null, "Ambiguo", false)), f.contexto()))
				.isInstanceOf(DadosInvalidosException.class);
		assertThatThrownBy(() -> confirmacaoService.confirmar(transacao.getLote().getId(),
				List.of(new ConfirmacaoTransacaoImportada(transacao.getId(), null,
						null, null, null, "Nenhum", false)), f.contexto()))
				.isInstanceOf(DadosInvalidosException.class);
	}

	@Test
	void faturaDeOutraEmpresaNaConfirmacaoNaoEEncontrada() {
		Fixture a = fixture("28888888000808");
		Fixture b = fixture("29999999000809");
		CompraCartao compra = compraRepository.saveAndFlush(new CompraCartao(b.empresa(), b.cartao(), b.cartao(),
				b.pessoa(), categoria(b, "Compras B", TipoFinanceiro.DESPESA), null, "Compra",
				LocalDate.of(2026, 1, 2), new BigDecimal("50.00"), 1, null, b.usuario()));
		ParcelaCompraCartao parcela = new ParcelaCompraCartao(b.empresa(), compra, 1, 1, new BigDecimal("50.00"),
				LocalDate.of(2026, 1, 12), b.usuario());
		compra.adicionarParcela(parcela);
		parcelaRepository.saveAndFlush(parcela);
		var aberta = faturaService.abrir(b.cartao().getId(), LocalDate.of(2026, 1, 12), b.contexto());
		faturaService.fechar(aberta.fatura().getId(), b.contexto());

		TransacaoBancariaImportada transacao = importarUmaTransacao(a, "-50.00", "PAGAMENTO FATURA");

		assertThatThrownBy(() -> confirmacaoService.confirmar(transacao.getLote().getId(),
				List.of(new ConfirmacaoTransacaoImportada(transacao.getId(), null,
						aberta.fatura().getId(), null, null, "Pagamento", false)), a.contexto()))
				.isInstanceOf(br.app.criati.exception.FaturaCartaoNaoEncontradaException.class);
		assertThat(lancamentoRepository.findAllByEmpresaId(a.empresa().getId())).isEmpty();
	}

	@Test
	void recebimentoDeParcelaDeEmprestimoViaConfirmacaoVinculaMesmoLancamentoEQuitaParcela() {
		Fixture f = fixture("30000000000810");
		ParcelaEmprestimo parcela = emprestimoComParcela(f, new BigDecimal("200.00"));
		TransacaoBancariaImportada transacao = importarUmaTransacao(f, "200.00", "RECEBIMENTO EMPRESTIMO JOAO");

		confirmacaoService.confirmar(transacao.getLote().getId(), List.of(new ConfirmacaoTransacaoImportada(
				transacao.getId(), null, null, parcela.getId(), null, "Recebimento parcela emprestimo", false)),
				f.contexto());

		ParcelaEmprestimo parcelaAtualizada = parcelaEmprestimoRepository
				.findByIdAndEmpresaId(parcela.getId(), f.empresa().getId()).orElseThrow();
		assertThat(parcelaAtualizada.getStatus()).isEqualTo(StatusParcelaEmprestimo.PAGO);
		assertThat(parcelaAtualizada.getSaldoPendente()).isEqualByComparingTo(BigDecimal.ZERO);
		TransacaoBancariaImportada confirmada = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(f.empresa().getId(), transacao.getLote().getId())
				.get(0);
		assertThat(confirmada.getSituacao()).isEqualTo(SituacaoTransacaoImportada.CONFIRMADA);
		assertThat(confirmada.getLancamentoFinanceiro()).isNotNull();
		// Vinculo persistente e auditavel: o mesmo LancamentoFinanceiro fica
		// referenciado pelos dois lados (transacao importada e recebimento da
		// parcela) - sem entidade/FK nova (CRIATI-FIN-FEAT-015).
		assertThat(recebimentoParcelaEmprestimoRepository
				.existsByLancamentoFinanceiroId(confirmada.getLancamentoFinanceiro().getId())).isTrue();
	}

	@Test
	void parcelaDeEmprestimoDeOutraEmpresaNaConfirmacaoNaoEEncontrada() {
		Fixture a = fixture("31111111000811");
		Fixture b = fixture("32222222000812");
		ParcelaEmprestimo parcelaDeB = emprestimoComParcela(b, new BigDecimal("80.00"));
		TransacaoBancariaImportada transacao = importarUmaTransacao(a, "80.00", "RECEBIMENTO SUSPEITO");

		assertThatThrownBy(() -> confirmacaoService.confirmar(transacao.getLote().getId(),
				List.of(new ConfirmacaoTransacaoImportada(transacao.getId(), null, null, parcelaDeB.getId(), null,
						"Recebimento", false)), a.contexto()))
				.isInstanceOf(br.app.criati.exception.ParcelaEmprestimoNaoEncontradaException.class);
		assertThat(lancamentoRepository.findAllByEmpresaId(a.empresa().getId())).isEmpty();
	}

	@Test
	void segundaTransacaoParaMesmaParcelaJaQuitadaNaoGeraRecebimentoDuplicado() {
		Fixture f = fixture("33333333000813");
		ParcelaEmprestimo parcela = emprestimoComParcela(f, new BigDecimal("100.00"));
		TransacaoBancariaImportada primeira = importarUmaTransacao(f, "100.00", "RECEBIMENTO EMPRESTIMO");
		confirmacaoService.confirmar(primeira.getLote().getId(), List.of(new ConfirmacaoTransacaoImportada(
				primeira.getId(), null, null, parcela.getId(), null, "Recebimento 1", false)), f.contexto());

		// Segunda transacao (outro lote, ex.: extrato reimportado com um ajuste
		// de descricao) tentando se vincular a mesma parcela ja totalmente
		// quitada - deve falhar, nunca gerar um segundo recebimento/lancamento
		// para a mesma parcela (ajuste "impedir recebimento financeiro
		// duplicado" de CRIATI-FIN-FEAT-015).
		TransacaoBancariaImportada segunda = importarUmaTransacao(f, "100.00", "RECEBIMENTO EMPRESTIMO DUPLICADO");
		assertThatThrownBy(() -> confirmacaoService.confirmar(segunda.getLote().getId(),
				List.of(new ConfirmacaoTransacaoImportada(segunda.getId(), null, null, parcela.getId(), null,
						"Recebimento 2", false)), f.contexto()))
				.isInstanceOf(DadosInvalidosException.class);
		assertThat(recebimentoParcelaEmprestimoRepository
				.findAllByEmpresaIdAndParcelaIdOrderByDataRecebimentoDesc(f.empresa().getId(), parcela.getId()))
				.hasSize(1);
	}

	@Test
	void transacaoDeDebitoNaoPodeSerConfirmadaComoRecebimentoDeParcelaDeEmprestimo() {
		Fixture f = fixture("34444444000814");
		ParcelaEmprestimo parcela = emprestimoComParcela(f, new BigDecimal("60.00"));
		TransacaoBancariaImportada transacao = importarUmaTransacao(f, "-60.00", "SAIDA QUALQUER");

		assertThatThrownBy(() -> confirmacaoService.confirmar(transacao.getLote().getId(),
				List.of(new ConfirmacaoTransacaoImportada(transacao.getId(), null, null, parcela.getId(), null,
						"Tentativa invalida", false)), f.contexto()))
				.isInstanceOf(DadosInvalidosException.class);
	}

	@Test
	void confirmarComParcelaEmprestimoJuntoDeCategoriaOuFaturaERejeitado() {
		Fixture f = fixture("35555555000815");
		CategoriaFinanceira categoria = categoria(f, "Mercado", TipoFinanceiro.RECEITA);
		ParcelaEmprestimo parcela = emprestimoComParcela(f, new BigDecimal("40.00"));
		TransacaoBancariaImportada transacao = importarUmaTransacao(f, "40.00", "QUALQUER");

		assertThatThrownBy(() -> confirmacaoService.confirmar(transacao.getLote().getId(),
				List.of(new ConfirmacaoTransacaoImportada(transacao.getId(), categoria.getId(), null, parcela.getId(),
						null, "Ambiguo", false)), f.contexto()))
				.isInstanceOf(DadosInvalidosException.class);
		assertThatThrownBy(() -> confirmacaoService.confirmar(transacao.getLote().getId(),
				List.of(new ConfirmacaoTransacaoImportada(transacao.getId(), null, UUID.randomUUID(), parcela.getId(),
						null, "Ambiguo", false)), f.contexto()))
				.isInstanceOf(DadosInvalidosException.class);
	}

	private ParcelaEmprestimo emprestimoComParcela(Fixture f, BigDecimal valor) {
		ParteFinanceira parte = parteRepository.saveAndFlush(
				new ParteFinanceira(f.empresa(), "Devedor", TipoParteFinanceira.PESSOA, null, null, null, f.usuario()));
		CategoriaFinanceira categoria = categoria(f, "Emprestimos concedidos", TipoFinanceiro.RECEITA);
		EmprestimoConcedido emprestimo = emprestimoRepository.saveAndFlush(new EmprestimoConcedido(f.empresa(), parte,
				categoria, "Emprestimo teste", valor, LocalDate.of(2026, 1, 1), TipoCobrancaEmprestimo.SEM_JUROS, null,
				null, FormaPagamentoEmprestimo.UNICO, 1, f.usuario()));
		return parcelaEmprestimoRepository.saveAndFlush(
				new ParcelaEmprestimo(f.empresa(), emprestimo, 1, 1, valor, LocalDate.of(2026, 2, 1), f.usuario()));
	}

	private RegraClassificacaoImportacao criarRegra(Fixture f, CategoriaFinanceira categoria, String padrao,
			EstrategiaComparacaoRegraImportacao estrategia, AplicacaoRegraClassificacaoImportacao aplicacao) {
		DadosRegraClassificacaoImportacao dados = new DadosRegraClassificacaoImportacao(null, categoria.getId(), null,
				padrao + " REFERENCIA", padrao, estrategia, 0, categoria.getTipo(), null,
				(EncaminhamentoSugeridoRegraImportacao) null, NivelConfiancaRegraImportacao.ALTA, aplicacao);
		return regraService.criar(dados, f.contexto());
	}

	private CategoriaFinanceira categoria(Fixture f, String nome, TipoFinanceiro tipo) {
		return categoriaRepository.saveAndFlush(new CategoriaFinanceira(f.empresa(), nome, tipo, StatusCadastro.ATIVO));
	}

	private TransacaoBancariaImportada importarUmaTransacao(Fixture f, String valor, String memo) {
		String ofx = "OFXHEADER:100\nENCODING:UTF-8\n\n<OFX><BANKTRANLIST>"
				+ "<STMTTRN><TRNTYPE>DEBIT<DTPOSTED>20260731<TRNAMT>" + valor
				+ "<FITID>fit-" + UUID.randomUUID() + "<MEMO>" + memo + "</STMTTRN>"
				+ "</BANKTRANLIST></OFX>";
		MockMultipartFile arquivo = new MockMultipartFile("arquivo", "extrato.ofx", "application/x-ofx",
				ofx.getBytes(StandardCharsets.UTF_8));
		PreviaImportacaoBancaria previa = importacaoService.importar(f.conta().getId(), arquivo, f.contexto());
		return previa.transacoes().get(0);
	}

	private Fixture fixture(String cnpj) {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Regras Confirmacao", "Empresa Regras Confirmacao", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		Usuario usuario = usuarioRepository.saveAndFlush(new Usuario("Usuario Regras Confirmacao",
				"regras.confirmacao." + cnpj + "@criati.test", passwordEncoder.encode(SENHA), StatusCadastro.ATIVO));
		usuarioEmpresaRepository.saveAndFlush(
				new UsuarioEmpresa(usuario, empresa, PerfilUsuario.ADMINISTRADOR, StatusCadastro.ATIVO));
		PessoaFinanceira pessoa = pessoaRepository.saveAndFlush(
				new PessoaFinanceira(empresa, "Titular", null, null, usuario));
		InstituicaoFinanceira instituicao = instituicaoRepository.saveAndFlush(
				new InstituicaoFinanceira(empresa, "Banco", "111", usuario));
		ContaFinanceira conta = contaRepository.saveAndFlush(new ContaFinanceira(empresa, pessoa, instituicao,
				"Conta", TipoContaFinanceira.CONTA_CORRENTE, BigDecimal.ZERO, LocalDate.of(2026, 1, 1), true, usuario));
		CartaoCredito cartao = cartaoRepository.saveAndFlush(new CartaoCredito(empresa, pessoa, instituicao,
				"Cartao", TipoCartao.FISICO, null, Bandeira.VISA, "1234", new BigDecimal("5000.00"),
				new BigDecimal("3000.00"), 5, 12, null, usuario));
		return new Fixture(empresa, usuario, pessoa, conta, cartao,
				new ContextoEmpresaAtual(usuario.getId(), empresa.getId(), UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR));
	}

	private record Fixture(Empresa empresa, Usuario usuario, PessoaFinanceira pessoa, ContaFinanceira conta,
			CartaoCredito cartao, ContextoEmpresaAtual contexto) {
	}
}
