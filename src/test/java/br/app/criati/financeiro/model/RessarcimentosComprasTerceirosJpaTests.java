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
import br.app.criati.financeiro.repository.CartaoCreditoRepository;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.CompraCartaoRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.ParcelaCompraCartaoRepository;
import br.app.criati.financeiro.repository.RessarcimentoParcelaCartaoRepository;
import br.app.criati.financeiro.repository.ValorAReceberParcelaCartaoRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.OrigemLancamentoFinanceiro;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ActiveProfiles("test")
@DataJpaTest
class RessarcimentosComprasTerceirosJpaTests {

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
	private PessoaFinanceiraRepository pessoaRepository;
	@Autowired
	private InstituicaoFinanceiraRepository instituicaoRepository;
	@Autowired
	private CartaoCreditoRepository cartaoRepository;
	@Autowired
	private CompraCartaoRepository compraRepository;
	@Autowired
	private ParcelaCompraCartaoRepository parcelaRepository;
	@Autowired
	private ValorAReceberParcelaCartaoRepository valorARepository;
	@Autowired
	private RessarcimentoParcelaCartaoRepository ressarcimentoRepository;
	@Autowired
	private LancamentoFinanceiroRepository lancamentoRepository;

	@Test
	void devePersistirValorAReceberComUuidEAuditoria() {
		Empresa empresa = criarEmpresa("61111111000201");
		Usuario autor = criarUsuario("jpa.valorareceber@criati.test");
		ParcelaCompraCartao parcela = criarParcela(empresa, autor);

		ValorAReceberParcelaCartao valor = valorARepository.saveAndFlush(
				new ValorAReceberParcelaCartao(empresa, parcela, autor));

		assertThat(valor.getId()).isNotNull();
		assertThat(valor.getCriadoEm()).isNotNull();
		assertThat(valor.getVencimento()).isEqualTo(parcela.getCompetencia());
		assertThat(valor.getValorTotal()).isEqualByComparingTo(parcela.getValor());
	}

	@Test
	void unicidadeDeValorAReceberPorParcelaEBloqueadaPeloBanco() {
		Empresa empresa = criarEmpresa("62222222000202");
		Usuario autor = criarUsuario("jpa.valorareceber.duplicado@criati.test");
		ParcelaCompraCartao parcela = criarParcela(empresa, autor);

		valorARepository.saveAndFlush(new ValorAReceberParcelaCartao(empresa, parcela, autor));

		assertThrows(DataIntegrityViolationException.class,
				() -> valorARepository.saveAndFlush(new ValorAReceberParcelaCartao(empresa, parcela, autor)));
	}

	@Test
	void devePersistirRessarcimentoEVincularUmUnicoLancamento() {
		Empresa empresa = criarEmpresa("63333333000203");
		Usuario autor = criarUsuario("jpa.ressarcimento@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoriaReceita = criarCategoriaReceita(empresa, "Ressarcimentos");
		ParcelaCompraCartao parcela = criarParcela(empresa, autor);
		ParteFinanceira terceiro = parcela.getCompra().getParteFinanceira();
		ValorAReceberParcelaCartao valor = valorARepository.saveAndFlush(
				new ValorAReceberParcelaCartao(empresa, parcela, autor));

		LancamentoFinanceiro lancamento = lancamentoRepository.saveAndFlush(
				LancamentoFinanceiro.gerarDeRessarcimentoCompraTerceiro(empresa, conta, categoriaReceita, terceiro,
						"Ressarcimento parcela 1/1", parcela.getValor(), valor.getVencimento().withDayOfMonth(1),
						valor.getVencimento(), LocalDate.of(2026, 8, 28), FormaPagamentoLancamento.PIX, autor));

		assertThat(lancamento.getOrigem()).isEqualTo(OrigemLancamentoFinanceiro.RESSARCIMENTO_COMPRA_TERCEIRO);
		assertThat(lancamento.getTipo()).isEqualTo(TipoFinanceiro.RECEITA);
		assertThat(lancamento.compoeSaldoRealizado()).isTrue();

		RessarcimentoParcelaCartao ressarcimento = ressarcimentoRepository.saveAndFlush(new RessarcimentoParcelaCartao(
				empresa, valor, conta, parcela.getValor(), LocalDate.of(2026, 8, 28), FormaPagamentoLancamento.PIX,
				null, lancamento, autor));

		assertThat(ressarcimento.getId()).isNotNull();
		assertThat(ressarcimento.estaAtivo()).isTrue();
		assertThat(ressarcimentoRepository.existsByLancamentoFinanceiroId(lancamento.getId())).isTrue();
	}

	@Test
	void unicidadeDeRessarcimentoPorLancamentoEBloqueadaPeloBanco() {
		Empresa empresa = criarEmpresa("64444444000204");
		Usuario autor = criarUsuario("jpa.ressarcimento.duplicado@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoriaReceita = criarCategoriaReceita(empresa, "Ressarcimentos");
		ParcelaCompraCartao parcela = criarParcela(empresa, autor);
		ParteFinanceira terceiro = parcela.getCompra().getParteFinanceira();
		ValorAReceberParcelaCartao valor = valorARepository.saveAndFlush(
				new ValorAReceberParcelaCartao(empresa, parcela, autor));
		LancamentoFinanceiro lancamento = lancamentoRepository.saveAndFlush(
				LancamentoFinanceiro.gerarDeRessarcimentoCompraTerceiro(empresa, conta, categoriaReceita, terceiro,
						"Ressarcimento parcela 1/1", parcela.getValor(), valor.getVencimento().withDayOfMonth(1),
						valor.getVencimento(), LocalDate.of(2026, 8, 28), FormaPagamentoLancamento.PIX, autor));

		ressarcimentoRepository.saveAndFlush(new RessarcimentoParcelaCartao(empresa, valor, conta, parcela.getValor(),
				LocalDate.of(2026, 8, 28), FormaPagamentoLancamento.PIX, null, lancamento, autor));

		assertThrows(DataIntegrityViolationException.class, () -> ressarcimentoRepository.saveAndFlush(
				new RessarcimentoParcelaCartao(empresa, valor, conta, parcela.getValor(), LocalDate.of(2026, 8, 28),
						FormaPagamentoLancamento.PIX, null, lancamento, autor)));
	}

	private ParcelaCompraCartao criarParcela(Empresa empresa, Usuario autor) {
		PessoaFinanceira titular = pessoaRepository.saveAndFlush(new PessoaFinanceira(empresa, "Titular", null, null, autor));
		InstituicaoFinanceira instituicao = instituicaoRepository
				.saveAndFlush(new InstituicaoFinanceira(empresa, "Banco " + empresa.getCnpj(), "000", autor));
		CartaoCredito cartao = cartaoRepository.saveAndFlush(new CartaoCredito(empresa, titular, instituicao,
				"Cartao", TipoCartao.FISICO, null, Bandeira.VISA, "1234", new BigDecimal("5000.00"), null, 5, 12,
				null, autor));
		CategoriaFinanceira categoriaDespesa = categoriaRepository
				.saveAndFlush(new CategoriaFinanceira(empresa, "Compras", TipoFinanceiro.DESPESA, StatusCadastro.ATIVO));
		ParteFinanceira terceiro = parteRepository
				.saveAndFlush(new ParteFinanceira(empresa, "Amigo", TipoParteFinanceira.PESSOA, null, null, null, autor));
		CompraCartao compra = compraRepository.saveAndFlush(new CompraCartao(empresa, cartao, cartao, titular,
				categoriaDespesa, terceiro, "Presente", LocalDate.of(2026, 8, 1), new BigDecimal("150.00"), 1, null, autor));
		return parcelaRepository.saveAndFlush(
				new ParcelaCompraCartao(empresa, compra, 1, 1, new BigDecimal("150.00"), LocalDate.of(2026, 9, 1), autor));
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
}
