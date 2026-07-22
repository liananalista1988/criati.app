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
import br.app.criati.financeiro.repository.CompromissoFinanceiroRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.OcorrenciaCompromissoRepository;
import br.app.criati.financeiro.repository.PagamentoOcorrenciaCompromissoRepository;
import br.app.criati.financeiro.repository.RecorrenciaFinanceiraRepository;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.OrigemLancamentoFinanceiro;
import br.app.criati.shared.enums.PeriodicidadeRecorrencia;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.shared.enums.TipoValorCompromisso;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ActiveProfiles("test")
@DataJpaTest
class ContasAPagarJpaTests {

	@Autowired
	private EmpresaRepository empresaRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private ContaFinanceiraRepository contaRepository;
	@Autowired
	private CategoriaFinanceiraRepository categoriaRepository;
	@Autowired
	private PessoaFinanceiraRepository pessoaRepository;
	@Autowired
	private RecorrenciaFinanceiraRepository recorrenciaRepository;
	@Autowired
	private CompromissoFinanceiroRepository compromissoRepository;
	@Autowired
	private OcorrenciaCompromissoRepository ocorrenciaRepository;
	@Autowired
	private PagamentoOcorrenciaCompromissoRepository pagamentoRepository;
	@Autowired
	private LancamentoFinanceiroRepository lancamentoRepository;

	@Test
	void devePersistirCompromissoFinanceiroComUuidEAuditoria() {
		Empresa empresa = criarEmpresa("11111111000161");
		Usuario autor = criarUsuario("jpa.compromisso@criati.test");
		CategoriaFinanceira categoria = criarCategoriaDespesa(empresa, "Energia");
		PessoaFinanceira pessoa = criarPessoa(empresa, autor, "Pessoa");

		CompromissoFinanceiro compromisso = compromissoRepository.saveAndFlush(new CompromissoFinanceiro(empresa,
				"Energia eletrica", categoria, pessoa, null, null, null, TipoValorCompromisso.VARIAVEL, null, 15,
				null, null, autor));

		assertThat(compromisso.getId()).isNotNull();
		assertThat(compromisso.getCriadoEm()).isNotNull();
		assertThat(compromisso.isAtivo()).isTrue();
	}

	@Test
	void devePersistirOcorrenciaAvulsaSemCompromisso() {
		Empresa empresa = criarEmpresa("22222222000162");
		Usuario autor = criarUsuario("jpa.ocorrencia.avulsa@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoriaDespesa(empresa, "Manutencao");
		PessoaFinanceira pessoa = criarPessoa(empresa, autor, "Pessoa");

		OcorrenciaCompromisso ocorrencia = ocorrenciaRepository.saveAndFlush(new OcorrenciaCompromisso(empresa, null,
				null, LocalDate.of(2026, 8, 1), "Manutencao residencial", categoria, pessoa, null, conta, null,
				new BigDecimal("450.00"), LocalDate.of(2026, 8, 25), null, null, null, null, null, autor));

		assertThat(ocorrencia.getId()).isNotNull();
		assertThat(ocorrencia.getCompromisso()).isNull();
		assertThat(ocorrencia.getValorTotal()).isEqualByComparingTo("450.00");
	}

	@Test
	void unicidadeDeCompromissoPorRecorrenciaEBloqueadaPeloBanco() {
		Empresa empresa = criarEmpresa("33333333000163");
		Usuario autor = criarUsuario("jpa.compromisso.recorrencia@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoriaDespesa(empresa, "Condominio");
		PessoaFinanceira pessoa = criarPessoa(empresa, autor, "Pessoa");
		RecorrenciaFinanceira recorrencia = criarRecorrenciaDespesa(empresa, conta, categoria, pessoa, autor);

		compromissoRepository.saveAndFlush(new CompromissoFinanceiro(empresa, "Condominio", categoria, pessoa, null,
				null, recorrencia, TipoValorCompromisso.FIXO, new BigDecimal("450.00"), 10, null, null, autor));

		assertThrows(DataIntegrityViolationException.class, () -> compromissoRepository.saveAndFlush(
				new CompromissoFinanceiro(empresa, "Condominio duplicado", categoria, pessoa, null, null, recorrencia,
						TipoValorCompromisso.FIXO, new BigDecimal("450.00"), 10, null, null, autor)));
	}

	@Test
	void unicidadeDeOcorrenciaPorRecorrenciaECompetenciaEBloqueadaPeloBanco() {
		Empresa empresa = criarEmpresa("44444444000164");
		Usuario autor = criarUsuario("jpa.ocorrencia.recorrencia@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoriaDespesa(empresa, "Internet");
		PessoaFinanceira pessoa = criarPessoa(empresa, autor, "Pessoa");
		RecorrenciaFinanceira recorrencia = criarRecorrenciaDespesa(empresa, conta, categoria, pessoa, autor);
		LocalDate competencia = LocalDate.of(2026, 8, 1);

		ocorrenciaRepository.saveAndFlush(new OcorrenciaCompromisso(empresa, null, recorrencia, competencia,
				"Internet 08/2026", categoria, pessoa, null, conta, null, new BigDecimal("120.00"),
				LocalDate.of(2026, 8, 10), null, null, null, null, null, autor));

		assertThrows(DataIntegrityViolationException.class, () -> ocorrenciaRepository.saveAndFlush(
				new OcorrenciaCompromisso(empresa, null, recorrencia, competencia, "Internet duplicada", categoria,
						pessoa, null, conta, null, new BigDecimal("120.00"), LocalDate.of(2026, 8, 10), null, null,
						null, null, null, autor)));
	}

	@Test
	void devePersistirPagamentoEVincularUmUnicoLancamento() {
		Empresa empresa = criarEmpresa("55555555000165");
		Usuario autor = criarUsuario("jpa.pagamento@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoriaDespesa(empresa, "Agua");
		PessoaFinanceira pessoa = criarPessoa(empresa, autor, "Pessoa");
		OcorrenciaCompromisso ocorrencia = ocorrenciaRepository.saveAndFlush(new OcorrenciaCompromisso(empresa, null,
				null, LocalDate.of(2026, 8, 1), "Agua 08/2026", categoria, pessoa, null, conta, null,
				new BigDecimal("80.00"), LocalDate.of(2026, 8, 20), null, null, null, null, null, autor));

		LancamentoFinanceiro lancamento = lancamentoRepository.saveAndFlush(LancamentoFinanceiro.gerarDeContaAPagar(
				empresa, conta, categoria, pessoa, null, ocorrencia.getDescricao(), new BigDecimal("80.00"),
				ocorrencia.getCompetencia().atDay(1), ocorrencia.getVencimento(), LocalDate.of(2026, 8, 18),
				FormaPagamentoLancamento.PIX, autor));

		assertThat(lancamento.getOrigem()).isEqualTo(OrigemLancamentoFinanceiro.CONTA_A_PAGAR);
		assertThat(lancamento.compoeSaldoRealizado()).isTrue();

		PagamentoOcorrenciaCompromisso pagamento = pagamentoRepository.saveAndFlush(new PagamentoOcorrenciaCompromisso(
				empresa, ocorrencia, conta, new BigDecimal("80.00"), LocalDate.of(2026, 8, 18),
				FormaPagamentoLancamento.PIX, null, lancamento, autor));

		assertThat(pagamento.getId()).isNotNull();
		assertThat(pagamento.estaAtivo()).isTrue();
		assertThat(pagamentoRepository.existsByLancamentoFinanceiroId(lancamento.getId())).isTrue();
	}

	@Test
	void unicidadeDePagamentoPorLancamentoEBloqueadaPeloBanco() {
		Empresa empresa = criarEmpresa("66666666000166");
		Usuario autor = criarUsuario("jpa.pagamento.duplicado@criati.test");
		ContaFinanceira conta = criarConta(empresa);
		CategoriaFinanceira categoria = criarCategoriaDespesa(empresa, "Plano de saude");
		PessoaFinanceira pessoa = criarPessoa(empresa, autor, "Pessoa");
		OcorrenciaCompromisso ocorrencia = ocorrenciaRepository.saveAndFlush(new OcorrenciaCompromisso(empresa, null,
				null, LocalDate.of(2026, 8, 1), "Plano de saude 08/2026", categoria, pessoa, null, conta, null,
				new BigDecimal("500.00"), LocalDate.of(2026, 8, 20), null, null, null, null, null, autor));
		LancamentoFinanceiro lancamento = lancamentoRepository.saveAndFlush(LancamentoFinanceiro.gerarDeContaAPagar(
				empresa, conta, categoria, pessoa, null, ocorrencia.getDescricao(), new BigDecimal("500.00"),
				ocorrencia.getCompetencia().atDay(1), ocorrencia.getVencimento(), LocalDate.of(2026, 8, 18),
				FormaPagamentoLancamento.PIX, autor));

		pagamentoRepository.saveAndFlush(new PagamentoOcorrenciaCompromisso(empresa, ocorrencia, conta,
				new BigDecimal("500.00"), LocalDate.of(2026, 8, 18), FormaPagamentoLancamento.PIX, null, lancamento,
				autor));

		assertThrows(DataIntegrityViolationException.class, () -> pagamentoRepository.saveAndFlush(
				new PagamentoOcorrenciaCompromisso(empresa, ocorrencia, conta, new BigDecimal("500.00"),
						LocalDate.of(2026, 8, 18), FormaPagamentoLancamento.PIX, null, lancamento, autor)));
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

	private CategoriaFinanceira criarCategoriaDespesa(Empresa empresa, String nome) {
		return categoriaRepository.saveAndFlush(new CategoriaFinanceira(empresa, nome, TipoFinanceiro.DESPESA, StatusCadastro.ATIVO));
	}

	private PessoaFinanceira criarPessoa(Empresa empresa, Usuario autor, String nome) {
		return pessoaRepository.saveAndFlush(new PessoaFinanceira(empresa, nome, null, null, autor));
	}

	private RecorrenciaFinanceira criarRecorrenciaDespesa(Empresa empresa, ContaFinanceira conta,
			CategoriaFinanceira categoria, PessoaFinanceira pessoa, Usuario autor) {
		return recorrenciaRepository.saveAndFlush(new RecorrenciaFinanceira(empresa, TipoFinanceiro.DESPESA,
				"Recorrencia de despesa", new BigDecimal("100.00"), conta, categoria, pessoa, null, null,
				PeriodicidadeRecorrencia.MENSAL, 1, 10, null, LocalDate.of(2026, 1, 1), null, false, null, autor));
	}
}
