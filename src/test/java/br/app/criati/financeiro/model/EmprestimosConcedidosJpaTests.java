package br.app.criati.financeiro.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.EmprestimoConcedidoRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.ParcelaEmprestimoRepository;
import br.app.criati.financeiro.repository.RecebimentoParcelaEmprestimoRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.shared.enums.FormaPagamentoEmprestimo;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.OrigemLancamentoFinanceiro;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ActiveProfiles("test")
@DataJpaTest
class EmprestimosConcedidosJpaTests {

	@Autowired
	private EmpresaRepository empresaRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private ContaFinanceiraRepository contaRepository;
	@Autowired
	private CategoriaFinanceiraRepository categoriaRepository;
	@Autowired
	private ParteFinanceiraRepository parteRepository;
	@Autowired
	private EmprestimoConcedidoRepository emprestimoRepository;
	@Autowired
	private ParcelaEmprestimoRepository parcelaRepository;
	@Autowired
	private RecebimentoParcelaEmprestimoRepository recebimentoRepository;
	@Autowired
	private LancamentoFinanceiroRepository lancamentoRepository;

	@Test
	void devePersistirEmprestimoConcedidoComUuidEAuditoria() {
		Empresa empresa = criarEmpresa("11111111000271");
		Usuario autor = criarUsuario("jpa.emprestimo@criati.test");
		CategoriaFinanceira categoria = criarCategoriaReceita(empresa, "Emprestimos");
		ParteFinanceira parte = criarParte(empresa, autor, "Devedor");

		EmprestimoConcedido emprestimo = emprestimoRepository.saveAndFlush(new EmprestimoConcedido(empresa, parte,
				categoria, "Ajuda emergencial", new BigDecimal("500.00"), LocalDate.of(2026, 8, 1),
				TipoCobrancaEmprestimo.SEM_JUROS, null, null, FormaPagamentoEmprestimo.UNICO, 1, autor));

		assertThat(emprestimo.getId()).isNotNull();
		assertThat(emprestimo.getCriadoEm()).isNotNull();
		assertThat(emprestimo.estaAtivo()).isTrue();
	}

	@Test
	void devePersistirParcelasEmCascataAoSalvarEmprestimo() {
		Empresa empresa = criarEmpresa("22222222000272");
		Usuario autor = criarUsuario("jpa.emprestimo.parcelas@criati.test");
		CategoriaFinanceira categoria = criarCategoriaReceita(empresa, "Emprestimos");
		ParteFinanceira parte = criarParte(empresa, autor, "Devedor");

		EmprestimoConcedido emprestimo = new EmprestimoConcedido(empresa, parte, categoria, "Parcelado", new BigDecimal("300.00"),
				LocalDate.of(2026, 8, 1), TipoCobrancaEmprestimo.SEM_JUROS, null, null, FormaPagamentoEmprestimo.PARCELADO,
				3, autor);
		emprestimo.adicionarParcela(new ParcelaEmprestimo(empresa, emprestimo, 1, 3, new BigDecimal("100.00"),
				LocalDate.of(2026, 9, 1), autor));
		emprestimo.adicionarParcela(new ParcelaEmprestimo(empresa, emprestimo, 2, 3, new BigDecimal("100.00"),
				LocalDate.of(2026, 10, 1), autor));
		emprestimo.adicionarParcela(new ParcelaEmprestimo(empresa, emprestimo, 3, 3, new BigDecimal("100.00"),
				LocalDate.of(2026, 11, 1), autor));
		EmprestimoConcedido salvo = emprestimoRepository.saveAndFlush(emprestimo);

		assertThat(parcelaRepository.findAllByEmpresaIdAndEmprestimoIdOrderByNumero(empresa.getId(), salvo.getId()))
				.hasSize(3);
	}

	@Test
	void unicidadeDeParcelaPorNumeroEBloqueadaPeloBanco() {
		Empresa empresa = criarEmpresa("33333333000273");
		Usuario autor = criarUsuario("jpa.parcela.duplicada@criati.test");
		CategoriaFinanceira categoria = criarCategoriaReceita(empresa, "Emprestimos");
		ParteFinanceira parte = criarParte(empresa, autor, "Devedor");
		EmprestimoConcedido emprestimo = emprestimoRepository.saveAndFlush(new EmprestimoConcedido(empresa, parte,
				categoria, "Parcelado", new BigDecimal("200.00"), LocalDate.of(2026, 8, 1),
				TipoCobrancaEmprestimo.SEM_JUROS, null, null, FormaPagamentoEmprestimo.PARCELADO, 2, autor));

		parcelaRepository.saveAndFlush(new ParcelaEmprestimo(empresa, emprestimo, 1, 2, new BigDecimal("100.00"),
				LocalDate.of(2026, 9, 1), autor));

		assertThrows(DataIntegrityViolationException.class, () -> parcelaRepository.saveAndFlush(new ParcelaEmprestimo(
				empresa, emprestimo, 1, 2, new BigDecimal("100.00"), LocalDate.of(2026, 9, 1), autor)));
	}

	@Test
	void devePersistirRecebimentoEVincularUmUnicoLancamento() {
		Empresa empresa = criarEmpresa("44444444000274");
		Usuario autor = criarUsuario("jpa.recebimento@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoriaReceita(empresa, "Emprestimos");
		ParteFinanceira parte = criarParte(empresa, autor, "Devedor");
		EmprestimoConcedido emprestimo = emprestimoRepository.saveAndFlush(new EmprestimoConcedido(empresa, parte,
				categoria, "Unico", new BigDecimal("150.00"), LocalDate.of(2026, 8, 1), TipoCobrancaEmprestimo.SEM_JUROS,
				null, null, FormaPagamentoEmprestimo.UNICO, 1, autor));
		ParcelaEmprestimo parcela = parcelaRepository.saveAndFlush(new ParcelaEmprestimo(empresa, emprestimo, 1, 1,
				new BigDecimal("150.00"), LocalDate.of(2026, 9, 1), autor));

		LancamentoFinanceiro lancamento = lancamentoRepository.saveAndFlush(LancamentoFinanceiro.gerarDeEmprestimoConcedido(
				empresa, conta, categoria, parte, "Recebimento parcela 1/1", new BigDecimal("150.00"),
				parcela.getVencimento().withDayOfMonth(1), parcela.getVencimento(), LocalDate.of(2026, 8, 28),
				FormaPagamentoLancamento.PIX, autor));

		assertThat(lancamento.getOrigem()).isEqualTo(OrigemLancamentoFinanceiro.EMPRESTIMO_CONCEDIDO);
		assertThat(lancamento.getTipo()).isEqualTo(TipoFinanceiro.RECEITA);
		assertThat(lancamento.compoeSaldoRealizado()).isTrue();

		RecebimentoParcelaEmprestimo recebimento = recebimentoRepository.saveAndFlush(new RecebimentoParcelaEmprestimo(
				empresa, parcela, conta, new BigDecimal("150.00"), LocalDate.of(2026, 8, 28),
				FormaPagamentoLancamento.PIX, null, lancamento, autor));

		assertThat(recebimento.getId()).isNotNull();
		assertThat(recebimento.estaAtivo()).isTrue();
		assertThat(recebimentoRepository.existsByLancamentoFinanceiroId(lancamento.getId())).isTrue();
	}

	@Test
	void unicidadeDeRecebimentoPorLancamentoEBloqueadaPeloBanco() {
		Empresa empresa = criarEmpresa("55555555000275");
		Usuario autor = criarUsuario("jpa.recebimento.duplicado@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoriaReceita(empresa, "Emprestimos");
		ParteFinanceira parte = criarParte(empresa, autor, "Devedor");
		EmprestimoConcedido emprestimo = emprestimoRepository.saveAndFlush(new EmprestimoConcedido(empresa, parte,
				categoria, "Unico", new BigDecimal("90.00"), LocalDate.of(2026, 8, 1), TipoCobrancaEmprestimo.SEM_JUROS,
				null, null, FormaPagamentoEmprestimo.UNICO, 1, autor));
		ParcelaEmprestimo parcela = parcelaRepository.saveAndFlush(new ParcelaEmprestimo(empresa, emprestimo, 1, 1,
				new BigDecimal("90.00"), LocalDate.of(2026, 9, 1), autor));
		LancamentoFinanceiro lancamento = lancamentoRepository.saveAndFlush(LancamentoFinanceiro.gerarDeEmprestimoConcedido(
				empresa, conta, categoria, parte, "Recebimento parcela 1/1", new BigDecimal("90.00"),
				parcela.getVencimento().withDayOfMonth(1), parcela.getVencimento(), LocalDate.of(2026, 8, 28),
				FormaPagamentoLancamento.PIX, autor));

		recebimentoRepository.saveAndFlush(new RecebimentoParcelaEmprestimo(empresa, parcela, conta,
				new BigDecimal("90.00"), LocalDate.of(2026, 8, 28), FormaPagamentoLancamento.PIX, null, lancamento, autor));

		assertThrows(DataIntegrityViolationException.class, () -> recebimentoRepository.saveAndFlush(
				new RecebimentoParcelaEmprestimo(empresa, parcela, conta, new BigDecimal("90.00"),
						LocalDate.of(2026, 8, 28), FormaPagamentoLancamento.PIX, null, lancamento, autor)));
	}

	private Empresa criarEmpresa(String cnpj) {
		return empresaRepository.saveAndFlush(new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		return usuarioRepository.saveAndFlush(new Usuario("Autor Teste", email, "hash", StatusCadastro.ATIVO));
	}

	private ContaFinanceira criarConta(Empresa empresa) {
		return contaRepository.saveAndFlush(
				new ContaFinanceira(empresa, "Conta", TipoContaFinanceira.CAIXA, BigDecimal.ZERO, StatusCadastro.ATIVO));
	}

	private CategoriaFinanceira criarCategoriaReceita(Empresa empresa, String nome) {
		return categoriaRepository.saveAndFlush(new CategoriaFinanceira(empresa, nome, TipoFinanceiro.RECEITA, StatusCadastro.ATIVO));
	}

	private ParteFinanceira criarParte(Empresa empresa, Usuario autor, String nome) {
		return parteRepository.saveAndFlush(
				new ParteFinanceira(empresa, nome, TipoParteFinanceira.PESSOA, null, null, null, autor));
	}
}
