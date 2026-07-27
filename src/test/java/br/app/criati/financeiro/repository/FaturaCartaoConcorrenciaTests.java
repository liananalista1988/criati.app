package br.app.criati.financeiro.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.FaturaCartaoStatusInvalidoException;
import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.CompraCartao;
import br.app.criati.financeiro.model.FaturaCartao;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.model.ParcelaCompraCartao;
import br.app.criati.financeiro.service.CompraCartaoService;
import br.app.criati.financeiro.service.FaturaCartaoService;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusCompraCartao;
import br.app.criati.shared.enums.StatusFaturaCartao;
import br.app.criati.shared.enums.StatusParcelaCartao;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@ActiveProfiles("test")
class FaturaCartaoConcorrenciaTests {

	@Autowired private EmpresaRepository empresas;
	@Autowired private UsuarioRepository usuarios;
	@Autowired private PessoaFinanceiraRepository pessoas;
	@Autowired private InstituicaoFinanceiraRepository instituicoes;
	@Autowired private CartaoCreditoRepository cartoes;
	@Autowired private CategoriaFinanceiraRepository categorias;
	@Autowired private CompraCartaoRepository compras;
	@Autowired private ParcelaCompraCartaoRepository parcelas;
	@Autowired private FaturaCartaoRepository faturas;
	@Autowired private CompraCartaoService compraService;
	@Autowired private FaturaCartaoService faturaService;
	@Autowired private PlatformTransactionManager transactionManager;

	@Test
	void lockDeFechamentoBloqueiaSegundaTransacao() throws InterruptedException {
		FaturaCartao fatura = criarFatura();
		TransactionTemplate transacao = new TransactionTemplate(transactionManager);
		CountDownLatch primeiraTravou = new CountDownLatch(1);
		AtomicLong espera = new AtomicLong(-1);
		long esperaSeguraMs = 400;

		Thread primeira = new Thread(() -> transacao.executeWithoutResult(status -> {
			faturas.findForUpdateByIdAndEmpresaId(fatura.getId(), fatura.getEmpresa().getId()).orElseThrow();
			primeiraTravou.countDown();
			dormir(esperaSeguraMs);
		}), "fatura-lock-1");
		Thread segunda = new Thread(() -> {
			aguardar(primeiraTravou);
			long inicio = System.nanoTime();
			transacao.executeWithoutResult(status ->
					faturas.findForUpdateByIdAndEmpresaId(fatura.getId(), fatura.getEmpresa().getId()).orElseThrow());
			espera.set(System.nanoTime() - inicio);
		}, "fatura-lock-2");

		primeira.start();
		segunda.start();
		primeira.join(Duration.ofSeconds(10).toMillis());
		segunda.join(Duration.ofSeconds(10).toMillis());

		assertThat(Duration.ofNanos(espera.get())).isGreaterThanOrEqualTo(Duration.ofMillis(esperaSeguraMs - 150));
	}

	@Test
	void fechamentoVersusCancelamentoSerializaPeloCartaoPrincipal() throws InterruptedException {
		validarFechamentoVersusAlteracao(
				(compraId, contexto) -> compraService.cancelar(compraId, "Cancelamento concorrente", contexto));
	}

	@Test
	void fechamentoVersusEstornoSerializaPeloCartaoPrincipal() throws InterruptedException {
		validarFechamentoVersusAlteracao(
				(compraId, contexto) -> compraService.estornar(compraId, "Estorno concorrente", contexto));
	}

	private void validarFechamentoVersusAlteracao(AlteracaoCompra alteracao) throws InterruptedException {
		Cenario cenario = criarCenarioComCompraEmFaturaAberta();
		TransactionTemplate transacao = new TransactionTemplate(transactionManager);
		CountDownLatch fechamentoBloqueouCartao = new CountDownLatch(1);
		CountDownLatch alteracaoIniciou = new CountDownLatch(1);
		AtomicBoolean alteracaoTerminou = new AtomicBoolean();
		AtomicBoolean alteracaoTerminouAntesDoFechamento = new AtomicBoolean();
		AtomicReference<Throwable> erroFechamento = new AtomicReference<>();
		AtomicReference<Throwable> erroAlteracao = new AtomicReference<>();

		Thread fechamento = new Thread(() -> {
			try {
				transacao.executeWithoutResult(status -> {
					cartoes.findForUpdateByIdAndEmpresaId(cenario.cartaoId(), cenario.empresaId()).orElseThrow();
					fechamentoBloqueouCartao.countDown();
					aguardarOuFalhar(alteracaoIniciou);
					dormir(400);
					alteracaoTerminouAntesDoFechamento.set(alteracaoTerminou.get());
					faturaService.fechar(cenario.faturaId(), cenario.contexto());
				});
			} catch (Throwable erro) {
				erroFechamento.set(erro);
			}
		}, "fechamento-fatura");
		Thread alteracaoConcorrente = new Thread(() -> {
			aguardarOuFalhar(fechamentoBloqueouCartao);
			try {
				transacao.executeWithoutResult(status -> {
					compras.findByIdAndEmpresaId(cenario.compraId(), cenario.empresaId()).orElseThrow();
					alteracaoIniciou.countDown();
					alteracao.executar(cenario.compraId(), cenario.contexto());
				});
			} catch (Throwable erro) {
				erroAlteracao.set(erro);
			} finally {
				alteracaoTerminou.set(true);
			}
		}, "alteracao-compra");
		fechamento.setDaemon(true);
		alteracaoConcorrente.setDaemon(true);

		fechamento.start();
		alteracaoConcorrente.start();
		fechamento.join(Duration.ofSeconds(10).toMillis());
		alteracaoConcorrente.join(Duration.ofSeconds(10).toMillis());

		assertThat(fechamento.isAlive()).as("fechamento sem deadlock").isFalse();
		assertThat(alteracaoConcorrente.isAlive()).as("alteracao sem deadlock").isFalse();
		assertThat(erroFechamento.get()).isNull();
		assertThat(alteracaoTerminouAntesDoFechamento.get()).isFalse();
		assertThat(erroAlteracao.get()).isInstanceOf(FaturaCartaoStatusInvalidoException.class);

		transacao.executeWithoutResult(status -> {
			CompraCartao compra = compras.findByIdAndEmpresaId(cenario.compraId(), cenario.empresaId()).orElseThrow();
			FaturaCartao fatura = faturas.findByIdAndEmpresaId(cenario.faturaId(), cenario.empresaId()).orElseThrow();
			java.util.List<ParcelaCompraCartao> parcelasDaCompra =
					parcelas.findAllByCompraIdAndEmpresaIdOrderByNumero(cenario.compraId(), cenario.empresaId());

			assertThat(fatura.getStatus()).isEqualTo(StatusFaturaCartao.FECHADA);
			assertThat(compra.getStatus()).isEqualTo(StatusCompraCartao.ATIVA);
			assertThat(parcelasDaCompra).hasSize(1);
			assertThat(parcelasDaCompra.get(0).getStatus()).isEqualTo(StatusParcelaCartao.ABERTA);
			assertThat(parcelasDaCompra.get(0).getFaturaId()).isEqualTo(fatura.getId());
		});
	}

	private Cenario criarCenarioComCompraEmFaturaAberta() {
		TransactionTemplate transacao = new TransactionTemplate(transactionManager);
		CenarioBase base = transacao.execute(status -> {
			String sufixo = UUID.randomUUID().toString();
			Empresa empresa = empresas.saveAndFlush(new Empresa("Empresa Corrida Fatura", "Empresa Corrida",
					sufixo.substring(0, 14), StatusCadastro.ATIVO));
			Usuario usuario = usuarios.saveAndFlush(new Usuario("Autor Corrida",
					"autor." + sufixo + "@criati.test", "hash", StatusCadastro.ATIVO));
			PessoaFinanceira pessoa = pessoas.saveAndFlush(
					new PessoaFinanceira(empresa, "Titular Corrida", null, null, usuario));
			InstituicaoFinanceira instituicao = instituicoes.saveAndFlush(
					new InstituicaoFinanceira(empresa, "Banco " + sufixo, null, usuario));
			CartaoCredito cartao = cartoes.saveAndFlush(new CartaoCredito(empresa, pessoa, instituicao,
					"Principal Corrida", TipoCartao.FISICO, null, Bandeira.VISA, "1234",
					new BigDecimal("5000.00"), new BigDecimal("3000.00"), 5, 12, null, usuario));
			CategoriaFinanceira categoria = categorias.saveAndFlush(new CategoriaFinanceira(
					empresa, "Compras Corrida", TipoFinanceiro.DESPESA, StatusCadastro.ATIVO));
			ContextoEmpresaAtual contexto = new ContextoEmpresaAtual(
					usuario.getId(), empresa.getId(), UUID.randomUUID(), PerfilUsuario.ADMINISTRADOR);
			return new CenarioBase(empresa.getId(), cartao.getId(), pessoa.getId(), categoria.getId(), contexto);
		});
		var compra = compraService.criar(base.cartaoId(), base.pessoaId(), base.categoriaId(), null,
				"Compra concorrente", LocalDate.of(2025, 12, 10), new BigDecimal("42.00"), 1, null,
				base.contexto());
		var fatura = faturaService.abrir(base.cartaoId(), LocalDate.of(2026, 1, 12), base.contexto());
		return new Cenario(base.empresaId(), base.cartaoId(), compra.compra().getId(),
				fatura.fatura().getId(), base.contexto());
	}

	private FaturaCartao criarFatura() {
		TransactionTemplate transacao = new TransactionTemplate(transactionManager);
		return transacao.execute(status -> {
			String sufixo = java.util.UUID.randomUUID().toString();
			Empresa empresa = empresas.saveAndFlush(new Empresa("Empresa Lock Fatura", "Empresa Lock",
					sufixo.substring(0, 14), StatusCadastro.ATIVO));
			Usuario usuario = usuarios.saveAndFlush(new Usuario("Autor", "autor." + sufixo + "@criati.test",
					"hash", StatusCadastro.ATIVO));
			PessoaFinanceira pessoa = pessoas.saveAndFlush(new PessoaFinanceira(empresa, "Titular", null, null, usuario));
			InstituicaoFinanceira instituicao = instituicoes.saveAndFlush(
					new InstituicaoFinanceira(empresa, "Banco " + sufixo, null, usuario));
			CartaoCredito cartao = cartoes.saveAndFlush(new CartaoCredito(empresa, pessoa, instituicao, "Principal",
					TipoCartao.FISICO, null, Bandeira.VISA, null, null, null, 5, 12, null, usuario));
			return faturas.saveAndFlush(new FaturaCartao(empresa, cartao, LocalDate.of(2026, 8, 12),
					LocalDate.of(2026, 7, 6), LocalDate.of(2026, 8, 5), LocalDate.of(2026, 8, 5),
					LocalDate.of(2026, 8, 12), usuario));
		});
	}

	private void dormir(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private void aguardarOuFalhar(CountDownLatch latch) {
		try {
			if (!latch.await(5, TimeUnit.SECONDS)) {
				throw new IllegalStateException("Timeout aguardando coordenacao do teste concorrente");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Teste concorrente interrompido", e);
		}
	}

	private void aguardar(CountDownLatch latch) {
		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@FunctionalInterface
	private interface AlteracaoCompra {
		void executar(UUID compraId, ContextoEmpresaAtual contexto);
	}

	private record CenarioBase(UUID empresaId, UUID cartaoId, UUID pessoaId, UUID categoriaId,
			ContextoEmpresaAtual contexto) {
	}

	private record Cenario(UUID empresaId, UUID cartaoId, UUID compraId, UUID faturaId,
			ContextoEmpresaAtual contexto) {
	}
}
