package br.app.criati.financeiro.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
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
import br.app.criati.financeiro.model.EmprestimoConcedido;
import br.app.criati.financeiro.model.ParcelaEmprestimo;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.shared.enums.FormaPagamentoEmprestimo;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Prova, com transacoes reais em threads distintas (nao mocks, nao apenas
 * verificacao de nome de metodo), que
 * ParcelaEmprestimoRepository#findForUpdateByIdAndEmpresaId bloqueia uma
 * segunda transacao concorrente ate a primeira liberar a linha — o mecanismo
 * usado por RecebimentoParcelaEmprestimoService para impedir dois
 * recebimentos simultaneos sobre o mesmo saldo (CRIATI-FIN-010A).
 *
 * Deliberadamente sem @Transactional na classe/metodos: cada thread abre sua
 * propria transacao via TransactionTemplate, em conexoes distintas do pool.
 * Um @DataJpaTest (que embrulha cada teste numa unica transacao sempre
 * desfeita ao final) nao serviria aqui — threads filhas nao enxergariam os
 * dados de fixture ainda nao commitados pela thread principal.
 *
 * Limitacao conhecida e documentada: H2 (mesmo em MODE=PostgreSQL) nao e
 * garantidamente identico ao mecanismo MVCC+locks do PostgreSQL real. O teste
 * abaixo mede bloqueio efetivo (duracao de espera da segunda transacao) como
 * a aproximacao mais proxima possivel dentro da infraestrutura de testes
 * atual; a garantia definitiva depende da validacao em PostgreSQL real
 * (ver Etapa 6/7 do relatorio CRIATI-FIN-010A).
 */
@SpringBootTest
@ActiveProfiles("test")
class ParcelaEmprestimoLockPessimistaTests {

	@Autowired
	private EmpresaRepository empresaRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private ParteFinanceiraRepository parteRepository;
	@Autowired
	private CategoriaFinanceiraRepository categoriaRepository;
	@Autowired
	private EmprestimoConcedidoRepository emprestimoRepository;
	@Autowired
	private ParcelaEmprestimoRepository parcelaRepository;
	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	@Transactional
	void findForUpdateRetornaAParcelaCorreta() {
		ParcelaEmprestimo parcela = criarParcela("41111111000391");

		var encontrada = parcelaRepository.findForUpdateByIdAndEmpresaId(parcela.getId(), parcela.getEmpresa().getId());

		assertThat(encontrada).isPresent();
		assertThat(encontrada.get().getId()).isEqualTo(parcela.getId());
	}

	@Test
	@Transactional
	void findForUpdateRespeitaIsolamentoPorEmpresaMesmoComLock() {
		ParcelaEmprestimo parcela = criarParcela("42222222000392");

		var deOutraEmpresa = parcelaRepository.findForUpdateByIdAndEmpresaId(parcela.getId(), UUID.randomUUID());

		assertThat(deOutraEmpresa).isEmpty();
	}

	@Test
	void segundaTransacaoFicaBloqueadaAteAPrimeiraLiberarALinha() throws InterruptedException {
		ParcelaEmprestimo parcela = criarParcela("43333333000393");
		UUID parcelaId = parcela.getId();
		UUID empresaId = parcela.getEmpresa().getId();

		TransactionTemplate transacao = new TransactionTemplate(transactionManager);
		CountDownLatch primeiraAdquiriuOLock = new CountDownLatch(1);
		AtomicLong duracaoEsperaSegundaNanos = new AtomicLong(-1);
		long duracaoSeguraMs = 400;

		Thread primeira = new Thread(() -> transacao.executeWithoutResult(status -> {
			parcelaRepository.findForUpdateByIdAndEmpresaId(parcelaId, empresaId);
			primeiraAdquiriuOLock.countDown();
			dormir(duracaoSeguraMs);
		}), "primeira-transacao-lock");

		Thread segunda = new Thread(() -> {
			aguardar(primeiraAdquiriuOLock);
			long inicio = System.nanoTime();
			transacao.executeWithoutResult(
					status -> parcelaRepository.findForUpdateByIdAndEmpresaId(parcelaId, empresaId));
			duracaoEsperaSegundaNanos.set(System.nanoTime() - inicio);
		}, "segunda-transacao-lock");

		primeira.start();
		segunda.start();
		primeira.join(Duration.ofSeconds(10).toMillis());
		segunda.join(Duration.ofSeconds(10).toMillis());

		assertThat(duracaoEsperaSegundaNanos.get()).isGreaterThan(0);
		Duration esperaSegunda = Duration.ofNanos(duracaoEsperaSegundaNanos.get());
		// A segunda so consegue completar sua leitura depois que a primeira dorme e
		// libera o lock ao commitar: se PESSIMISTIC_WRITE realmente bloquear, a
		// espera da segunda fica proxima da duracao total do sleep da primeira,
		// nunca instantanea (o que aconteceria se o lock nao fizesse efeito).
		assertThat(esperaSegunda).isGreaterThanOrEqualTo(Duration.ofMillis(duracaoSeguraMs - 150));
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

	private ParcelaEmprestimo criarParcela(String cnpj) {
		Empresa empresa = empresaRepository
				.saveAndFlush(new Empresa("Empresa Lock Teste", "Empresa Lock Teste", cnpj, StatusCadastro.ATIVO));
		Usuario autor = usuarioRepository.saveAndFlush(
				new Usuario("Autor Lock", "autor.lock." + cnpj + "@criati.test", "hash", StatusCadastro.ATIVO));
		CategoriaFinanceira categoria = categoriaRepository
				.saveAndFlush(new CategoriaFinanceira(empresa, "Emprestimos", TipoFinanceiro.RECEITA, StatusCadastro.ATIVO));
		ParteFinanceira parte = parteRepository
				.saveAndFlush(new ParteFinanceira(empresa, "Devedor", TipoParteFinanceira.PESSOA, null, null, null, autor));
		EmprestimoConcedido emprestimo = emprestimoRepository.saveAndFlush(new EmprestimoConcedido(empresa, parte,
				categoria, "Lock", new BigDecimal("100.00"), LocalDate.of(2026, 8, 1), TipoCobrancaEmprestimo.SEM_JUROS,
				null, null, FormaPagamentoEmprestimo.UNICO, 1, autor));
		return parcelaRepository.saveAndFlush(new ParcelaEmprestimo(empresa, emprestimo, 1, 1, new BigDecimal("100.00"),
				LocalDate.of(2026, 9, 1), autor));
	}
}
