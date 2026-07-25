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
import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.CompraCartao;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.model.ParcelaCompraCartao;
import br.app.criati.financeiro.model.ValorAReceberParcelaCartao;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Prova, com transacoes reais em threads distintas (nao mocks, nao apenas
 * verificacao de nome de metodo), que
 * ValorAReceberParcelaCartaoRepository#findForUpdateByIdAndEmpresaId bloqueia
 * uma segunda transacao concorrente ate a primeira liberar a linha — o
 * mecanismo usado por RessarcimentoParcelaCartaoService para impedir dois
 * ressarcimentos simultaneos sobre o mesmo saldo (CRIATI-FIN-013). Mesmo
 * desenho ja usado e validado por ParcelaEmprestimoLockPessimistaTests
 * (CRIATI-FIN-010A).
 *
 * Deliberadamente sem @Transactional na classe/metodos de concorrencia: cada
 * thread abre sua propria transacao via TransactionTemplate, em conexoes
 * distintas do pool. Limitacao conhecida: H2 (mesmo em MODE=PostgreSQL) nao e
 * garantidamente identico ao mecanismo MVCC+locks do PostgreSQL real; a
 * garantia definitiva depende da validacao em PostgreSQL real (ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-CRIATI-FIN-013.md).
 */
@SpringBootTest
@ActiveProfiles("test")
class ValorAReceberParcelaCartaoLockPessimistaTests {

	@Autowired
	private EmpresaRepository empresaRepository;
	@Autowired
	private UsuarioRepository usuarioRepository;
	@Autowired
	private PessoaFinanceiraRepository pessoaRepository;
	@Autowired
	private InstituicaoFinanceiraRepository instituicaoRepository;
	@Autowired
	private CartaoCreditoRepository cartaoRepository;
	@Autowired
	private CategoriaFinanceiraRepository categoriaRepository;
	@Autowired
	private ParteFinanceiraRepository parteRepository;
	@Autowired
	private CompraCartaoRepository compraRepository;
	@Autowired
	private ParcelaCompraCartaoRepository parcelaRepository;
	@Autowired
	private ValorAReceberParcelaCartaoRepository valorARepository;
	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	@Transactional
	void findForUpdateRetornaOValorAReceberCorreto() {
		ValorAReceberParcelaCartao valor = criarValorAReceber("81111111000401");

		var encontrado = valorARepository.findForUpdateByIdAndEmpresaId(valor.getId(), valor.getEmpresa().getId());

		assertThat(encontrado).isPresent();
		assertThat(encontrado.get().getId()).isEqualTo(valor.getId());
	}

	@Test
	@Transactional
	void findForUpdateRespeitaIsolamentoPorEmpresaMesmoComLock() {
		ValorAReceberParcelaCartao valor = criarValorAReceber("82222222000402");

		var deOutraEmpresa = valorARepository.findForUpdateByIdAndEmpresaId(valor.getId(), UUID.randomUUID());

		assertThat(deOutraEmpresa).isEmpty();
	}

	@Test
	void segundaTransacaoFicaBloqueadaAteAPrimeiraLiberarALinha() throws InterruptedException {
		ValorAReceberParcelaCartao valor = criarValorAReceber("83333333000403");
		UUID valorId = valor.getId();
		UUID empresaId = valor.getEmpresa().getId();

		TransactionTemplate transacao = new TransactionTemplate(transactionManager);
		CountDownLatch primeiraAdquiriuOLock = new CountDownLatch(1);
		AtomicLong duracaoEsperaSegundaNanos = new AtomicLong(-1);
		long duracaoSeguraMs = 400;

		Thread primeira = new Thread(() -> transacao.executeWithoutResult(status -> {
			valorARepository.findForUpdateByIdAndEmpresaId(valorId, empresaId);
			primeiraAdquiriuOLock.countDown();
			dormir(duracaoSeguraMs);
		}), "primeira-transacao-lock-terceiro");

		Thread segunda = new Thread(() -> {
			aguardar(primeiraAdquiriuOLock);
			long inicio = System.nanoTime();
			transacao.executeWithoutResult(
					status -> valorARepository.findForUpdateByIdAndEmpresaId(valorId, empresaId));
			duracaoEsperaSegundaNanos.set(System.nanoTime() - inicio);
		}, "segunda-transacao-lock-terceiro");

		primeira.start();
		segunda.start();
		primeira.join(Duration.ofSeconds(10).toMillis());
		segunda.join(Duration.ofSeconds(10).toMillis());

		assertThat(duracaoEsperaSegundaNanos.get()).isGreaterThan(0);
		Duration esperaSegunda = Duration.ofNanos(duracaoEsperaSegundaNanos.get());
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

	private ValorAReceberParcelaCartao criarValorAReceber(String cnpj) {
		Empresa empresa = empresaRepository
				.saveAndFlush(new Empresa("Empresa Lock Terceiro", "Empresa Lock Terceiro", cnpj, StatusCadastro.ATIVO));
		Usuario autor = usuarioRepository.saveAndFlush(
				new Usuario("Autor Lock", "autor.lock.terceiro." + cnpj + "@criati.test", "hash", StatusCadastro.ATIVO));
		PessoaFinanceira titular = pessoaRepository.saveAndFlush(new PessoaFinanceira(empresa, "Titular", null, null, autor));
		InstituicaoFinanceira instituicao = instituicaoRepository
				.saveAndFlush(new InstituicaoFinanceira(empresa, "Banco " + cnpj, "000", autor));
		CartaoCredito cartao = cartaoRepository.saveAndFlush(new CartaoCredito(empresa, titular, instituicao, "Cartao",
				TipoCartao.FISICO, null, Bandeira.VISA, "1234", new BigDecimal("5000.00"), null, 5, 12, null, autor));
		CategoriaFinanceira categoria = categoriaRepository
				.saveAndFlush(new CategoriaFinanceira(empresa, "Compras", TipoFinanceiro.DESPESA, StatusCadastro.ATIVO));
		ParteFinanceira terceiro = parteRepository
				.saveAndFlush(new ParteFinanceira(empresa, "Amigo", TipoParteFinanceira.PESSOA, null, null, null, autor));
		CompraCartao compra = compraRepository.saveAndFlush(new CompraCartao(empresa, cartao, cartao, titular,
				categoria, terceiro, "Presente", LocalDate.of(2026, 8, 1), new BigDecimal("100.00"), 1, null, autor));
		ParcelaCompraCartao parcela = parcelaRepository.saveAndFlush(
				new ParcelaCompraCartao(empresa, compra, 1, 1, new BigDecimal("100.00"), LocalDate.of(2026, 9, 1), autor));
		return valorARepository.saveAndFlush(new ValorAReceberParcelaCartao(empresa, parcela, autor));
	}
}
