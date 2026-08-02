package br.app.criati.financeiro.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.RegraClassificacaoImportacao;
import br.app.criati.shared.enums.AplicacaoRegraClassificacaoImportacao;
import br.app.criati.shared.enums.EstrategiaComparacaoRegraImportacao;
import br.app.criati.shared.enums.NivelConfiancaRegraImportacao;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Prova, com transacoes reais em threads distintas, que
 * RegraClassificacaoImportacaoRepository#findForUpdateByIdAndEmpresaId
 * bloqueia uma segunda transacao concorrente ate a primeira liberar a linha -
 * o mecanismo usado por RegraClassificacaoImportacaoService#registrarUso para
 * nao perder incremento de quantidade_utilizacoes em confirmacoes
 * concorrentes que usam a mesma regra (ajuste obrigatorio 8 de
 * CRIATI-IMP-002A). Mesmo desenho ja validado por
 * ValorAReceberParcelaCartaoLockPessimistaTests/FaturaCartaoConcorrenciaTests.
 */
@SpringBootTest
@ActiveProfiles("test")
class RegraClassificacaoImportacaoLockPessimistaTests {

	@Autowired private EmpresaRepository empresaRepository;
	@Autowired private UsuarioRepository usuarioRepository;
	@Autowired private CategoriaFinanceiraRepository categoriaRepository;
	@Autowired private RegraClassificacaoImportacaoRepository regraRepository;
	@Autowired private PlatformTransactionManager transactionManager;

	@Test
	@Transactional
	void findForUpdateRespeitaIsolamentoPorEmpresaMesmoComLock() {
		RegraClassificacaoImportacao regra = criarRegra("41111111000901");

		var deOutraEmpresa = regraRepository.findForUpdateByIdAndEmpresaId(regra.getId(), UUID.randomUUID());

		assertThat(deOutraEmpresa).isEmpty();
	}

	@Test
	void segundaTransacaoFicaBloqueadaAteAPrimeiraLiberarALinha() throws InterruptedException {
		RegraClassificacaoImportacao regra = criarRegra("42222222000902");
		UUID regraId = regra.getId();
		UUID empresaId = regra.getEmpresa().getId();

		TransactionTemplate transacao = new TransactionTemplate(transactionManager);
		CountDownLatch primeiraAdquiriuOLock = new CountDownLatch(1);
		AtomicLong duracaoEsperaSegundaNanos = new AtomicLong(-1);
		long duracaoSeguraMs = 400;

		Thread primeira = new Thread(() -> transacao.executeWithoutResult(status -> {
			regraRepository.findForUpdateByIdAndEmpresaId(regraId, empresaId);
			primeiraAdquiriuOLock.countDown();
			dormir(duracaoSeguraMs);
		}), "primeira-transacao-lock-regra");

		Thread segunda = new Thread(() -> {
			aguardar(primeiraAdquiriuOLock);
			long inicio = System.nanoTime();
			transacao.executeWithoutResult(
					status -> regraRepository.findForUpdateByIdAndEmpresaId(regraId, empresaId));
			duracaoEsperaSegundaNanos.set(System.nanoTime() - inicio);
		}, "segunda-transacao-lock-regra");

		primeira.start();
		segunda.start();
		primeira.join(Duration.ofSeconds(10).toMillis());
		segunda.join(Duration.ofSeconds(10).toMillis());

		assertThat(duracaoEsperaSegundaNanos.get()).isGreaterThan(0);
		Duration esperaSegunda = Duration.ofNanos(duracaoEsperaSegundaNanos.get());
		assertThat(esperaSegunda).isGreaterThanOrEqualTo(Duration.ofMillis(duracaoSeguraMs - 150));
	}

	@Test
	void duasConfirmacoesConcorrentesUsandoAMesmaRegraIncrementamOContadorCorretamente() throws Exception {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Lock Regra", "Empresa Lock Regra", "43333333000903", StatusCadastro.ATIVO));
		Usuario autor = usuarioRepository.saveAndFlush(new Usuario("Autor Lock Regra",
				"autor.lock.regra@criati.test", "hash", StatusCadastro.ATIVO));
		CategoriaFinanceira categoria = categoriaRepository.saveAndFlush(
				new CategoriaFinanceira(empresa, "Categoria Lock", TipoFinanceiro.DESPESA, StatusCadastro.ATIVO));
		RegraClassificacaoImportacao regra = regraRepository.saveAndFlush(new RegraClassificacaoImportacao(
				empresa, null, "REF", "PADRAO", EstrategiaComparacaoRegraImportacao.CONTEM, 0,
				TipoFinanceiro.DESPESA, categoria, null, null, null, NivelConfiancaRegraImportacao.MEDIA,
				AplicacaoRegraClassificacaoImportacao.SUGESTAO, autor));
		UUID regraId = regra.getId();
		UUID empresaId = empresa.getId();
		UUID autorId = autor.getId();

		TransactionTemplate transacao = new TransactionTemplate(transactionManager);
		CountDownLatch prontas = new CountDownLatch(2);
		CountDownLatch iniciar = new CountDownLatch(1);
		Runnable incrementar = () -> transacao.executeWithoutResult(status -> {
			RegraClassificacaoImportacao carregada = regraRepository
					.findForUpdateByIdAndEmpresaId(regraId, empresaId).orElseThrow();
			Usuario autorCarregado = usuarioRepository.findById(autorId).orElseThrow();
			carregada.registrarUso(autorCarregado);
			regraRepository.save(carregada);
		});
		Thread t1 = new Thread(() -> { prontas.countDown(); aguardar(iniciar); incrementar.run(); }, "uso-regra-1");
		Thread t2 = new Thread(() -> { prontas.countDown(); aguardar(iniciar); incrementar.run(); }, "uso-regra-2");

		t1.start();
		t2.start();
		assertThat(prontas.await(5, TimeUnit.SECONDS)).isTrue();
		iniciar.countDown();
		t1.join(Duration.ofSeconds(10).toMillis());
		t2.join(Duration.ofSeconds(10).toMillis());

		RegraClassificacaoImportacao atualizada = regraRepository.findByIdAndEmpresaId(regraId, empresaId).orElseThrow();
		assertThat(atualizada.getQuantidadeUtilizacoes()).isEqualTo(2);
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

	private RegraClassificacaoImportacao criarRegra(String cnpj) {
		Empresa empresa = empresaRepository
				.saveAndFlush(new Empresa("Empresa Lock Regra Terceiro", "Empresa Lock Regra Terceiro", cnpj, StatusCadastro.ATIVO));
		Usuario autor = usuarioRepository.saveAndFlush(
				new Usuario("Autor Lock Regra Terceiro", "autor.lock.regra." + cnpj + "@criati.test", "hash", StatusCadastro.ATIVO));
		CategoriaFinanceira categoria = categoriaRepository.saveAndFlush(
				new CategoriaFinanceira(empresa, "Categoria " + cnpj, TipoFinanceiro.DESPESA, StatusCadastro.ATIVO));
		return regraRepository.saveAndFlush(new RegraClassificacaoImportacao(empresa, null, "REF " + cnpj,
				"PADRAO " + cnpj, EstrategiaComparacaoRegraImportacao.CONTEM, 0, TipoFinanceiro.DESPESA, categoria,
				null, null, null, NivelConfiancaRegraImportacao.MEDIA,
				AplicacaoRegraClassificacaoImportacao.SUGESTAO, autor));
	}
}
