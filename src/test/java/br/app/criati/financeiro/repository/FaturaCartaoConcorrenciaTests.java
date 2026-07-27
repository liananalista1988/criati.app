package br.app.criati.financeiro.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.financeiro.model.FaturaCartao;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;
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
	@Autowired private FaturaCartaoRepository faturas;
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

	private void aguardar(CountDownLatch latch) {
		try {
			latch.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
