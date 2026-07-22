package br.app.criati.financeiro.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.repository.CartaoCreditoRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@ActiveProfiles("test")
@DataJpaTest
class CartaoCreditoJpaTests {

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

	@Test
	void devePersistirCartaoFisicoComUuidEAuditoria() {
		Empresa empresa = criarEmpresa("11111111000171");
		Usuario autor = criarUsuario("jpa.cartao.fisico@criati.test");
		PessoaFinanceira titular = criarPessoa(empresa, autor, "Pessoa");
		var instituicao = criarInstituicao(empresa, autor);

		CartaoCredito cartao = cartaoRepository.saveAndFlush(new CartaoCredito(empresa, titular, instituicao,
				"Cartao BB", TipoCartao.FISICO, null, Bandeira.VISA, "1234", new BigDecimal("5000.00"),
				new BigDecimal("2000.00"), 5, 12, null, autor));

		assertThat(cartao.getId()).isNotNull();
		assertThat(cartao.getCriadoEm()).isNotNull();
		assertThat(cartao.getAtualizadoEm()).isNotNull();
		assertThat(cartao.getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void devePersistirCartaoVirtualComVinculoAoPrincipal() {
		Empresa empresa = criarEmpresa("22222222000172");
		Usuario autor = criarUsuario("jpa.cartao.virtual@criati.test");
		PessoaFinanceira titular = criarPessoa(empresa, autor, "Pessoa");
		var instituicao = criarInstituicao(empresa, autor);

		CartaoCredito principal = cartaoRepository.saveAndFlush(new CartaoCredito(empresa, titular, instituicao,
				"Cartao Principal", TipoCartao.FISICO, null, Bandeira.MASTERCARD, "1111", new BigDecimal("3000.00"),
				null, 10, 17, null, autor));
		CartaoCredito virtual = cartaoRepository.saveAndFlush(new CartaoCredito(empresa, titular, instituicao,
				"Cartao Virtual Compras Online", TipoCartao.VIRTUAL, principal, Bandeira.MASTERCARD, "2222", null,
				null, null, null, null, autor));

		assertThat(virtual.getId()).isNotNull();
		assertThat(virtual.getCartaoPrincipal().getId()).isEqualTo(principal.getId());
		assertThat(virtual.getLimiteTotal()).isNull();
		assertThat(virtual.getLimiteTotalEfetivo()).isEqualByComparingTo("3000.00");
		assertThat(cartaoRepository.countByCartaoPrincipalId(principal.getId())).isEqualTo(1);
	}

	private Empresa criarEmpresa(String cnpj) {
		return empresaRepository.saveAndFlush(new Empresa("Empresa Teste Ltda", "Empresa Teste", cnpj, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		return usuarioRepository.saveAndFlush(new Usuario("Autor Teste", email, "hash", StatusCadastro.ATIVO));
	}

	private PessoaFinanceira criarPessoa(Empresa empresa, Usuario autor, String nome) {
		return pessoaRepository.saveAndFlush(new PessoaFinanceira(empresa, nome, null, null, autor));
	}

	private br.app.criati.financeiro.model.InstituicaoFinanceira criarInstituicao(Empresa empresa, Usuario autor) {
		return instituicaoRepository.saveAndFlush(new br.app.criati.financeiro.model.InstituicaoFinanceira(
				empresa, "Instituicao Teste " + empresa.getCnpj(), "000", autor));
	}
}
