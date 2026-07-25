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
	void devePersistirRessarcimentoSemGerarLancamentoFinanceiro() {
		Empresa empresa = criarEmpresa("63333333000203");
		Usuario autor = criarUsuario("jpa.ressarcimento@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		ParcelaCompraCartao parcela = criarParcela(empresa, autor);
		ValorAReceberParcelaCartao valor = valorARepository.saveAndFlush(
				new ValorAReceberParcelaCartao(empresa, parcela, autor));

		RessarcimentoParcelaCartao ressarcimento = ressarcimentoRepository.saveAndFlush(new RessarcimentoParcelaCartao(
				empresa, valor, conta, parcela.getValor(), LocalDate.of(2026, 8, 28), FormaPagamentoLancamento.PIX,
				null, autor));

		assertThat(ressarcimento.getId()).isNotNull();
		assertThat(ressarcimento.estaAtivo()).isTrue();
		assertThat(ressarcimento.getValor()).isEqualByComparingTo(parcela.getValor());
		assertThat(ressarcimento.getConta().getId()).isEqualTo(conta.getId());
		// nenhum LancamentoFinanceiro (RECEITA ou DESPESA) e criado pelo ressarcimento — CRIATI-FIN-013A.
		assertThat(lancamentoRepository.findAllByEmpresaId(empresa.getId())).isEmpty();
	}

	@Test
	void findAllPorContaERestritoAoTenantEUsadoPeloCalculoDeSaldo() {
		Empresa empresaA = criarEmpresa("64444444000204");
		Empresa empresaB = criarEmpresa("65555555000205");
		Usuario autorA = criarUsuario("jpa.ressarcimento.conta.a@criati.test");
		Usuario autorB = criarUsuario("jpa.ressarcimento.conta.b@criati.test");
		ContaFinanceira contaA = criarConta(empresaA);
		ContaFinanceira contaB = criarConta(empresaB);
		ParcelaCompraCartao parcelaA = criarParcela(empresaA, autorA);
		ParcelaCompraCartao parcelaB = criarParcela(empresaB, autorB);
		ValorAReceberParcelaCartao valorA = valorARepository.saveAndFlush(
				new ValorAReceberParcelaCartao(empresaA, parcelaA, autorA));
		ValorAReceberParcelaCartao valorB = valorARepository.saveAndFlush(
				new ValorAReceberParcelaCartao(empresaB, parcelaB, autorB));
		ressarcimentoRepository.saveAndFlush(new RessarcimentoParcelaCartao(empresaA, valorA, contaA,
				parcelaA.getValor(), LocalDate.of(2026, 8, 28), FormaPagamentoLancamento.PIX, null, autorA));
		ressarcimentoRepository.saveAndFlush(new RessarcimentoParcelaCartao(empresaB, valorB, contaB,
				parcelaB.getValor(), LocalDate.of(2026, 8, 28), FormaPagamentoLancamento.PIX, null, autorB));

		assertThat(ressarcimentoRepository.findAllByEmpresaIdAndContaId(empresaA.getId(), contaA.getId())).hasSize(1);
		assertThat(ressarcimentoRepository.findAllByEmpresaIdAndContaId(empresaA.getId(), contaB.getId())).isEmpty();
		assertThat(ressarcimentoRepository.findAllByEmpresaId(empresaA.getId())).hasSize(1);
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
}
