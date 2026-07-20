package br.app.criati.aplicacao.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import br.app.criati.aplicacao.repository.AplicacaoRepository;
import br.app.criati.aplicacao.repository.EmpresaAplicacaoRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.shared.enums.StatusCadastro;
import jakarta.persistence.EntityManager;

@ActiveProfiles("test")
@DataJpaTest
class AplicacaoEmpresaAplicacaoJpaTests {

	@Autowired
	private AplicacaoRepository aplicacaoRepository;

	@Autowired
	private EmpresaAplicacaoRepository empresaAplicacaoRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void devePersistirAplicacaoComUuidGerado() {
		Aplicacao aplicacao = aplicacaoRepository.saveAndFlush(
				new Aplicacao("TESTE_UUID", "Aplicacao Teste", "descricao", StatusCadastro.ATIVO));

		assertThat(aplicacao.getId()).isNotNull();
		assertThat(aplicacao.getCriadoEm()).isNotNull();
		assertThat(aplicacao.getAtualizadoEm()).isNotNull();
	}

	@Test
	void naoDevePermitirCodigoDuplicado() {
		aplicacaoRepository.saveAndFlush(new Aplicacao("DUPLICADO", "Primeira", null, StatusCadastro.ATIVO));
		entityManager.clear();

		assertThatThrownBy(() -> aplicacaoRepository
				.saveAndFlush(new Aplicacao("DUPLICADO", "Segunda", null, StatusCadastro.ATIVO)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void naoDevePermitirVinculoDuplicadoDeEmpresaComAplicacao() {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Vinculo Ltda", "Empresa Vinculo", "11111111000111", StatusCadastro.ATIVO));
		Aplicacao aplicacao = aplicacaoRepository.saveAndFlush(
				new Aplicacao("VINCULO_UNICO", "Aplicacao Vinculo", null, StatusCadastro.ATIVO));

		empresaAplicacaoRepository.saveAndFlush(new EmpresaAplicacao(empresa, aplicacao, StatusCadastro.ATIVO));
		entityManager.clear();

		assertThatThrownBy(() -> empresaAplicacaoRepository
				.saveAndFlush(new EmpresaAplicacao(empresa, aplicacao, StatusCadastro.INATIVO)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void devePersistirEmpresaAplicacaoComUuidGerado() {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Vinculo Uuid Ltda", "Empresa Vinculo Uuid", "22222222000122", StatusCadastro.ATIVO));
		Aplicacao aplicacao = aplicacaoRepository.saveAndFlush(
				new Aplicacao("VINCULO_UUID", "Aplicacao Vinculo Uuid", null, StatusCadastro.ATIVO));

		EmpresaAplicacao vinculo = empresaAplicacaoRepository.saveAndFlush(
				new EmpresaAplicacao(empresa, aplicacao, StatusCadastro.ATIVO));

		assertThat(vinculo.getId()).isNotNull();
		assertThat(vinculo.getCriadoEm()).isNotNull();
		assertThat(vinculo.getAtualizadoEm()).isNotNull();
	}
}
