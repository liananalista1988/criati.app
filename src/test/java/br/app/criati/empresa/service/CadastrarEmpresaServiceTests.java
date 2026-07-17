package br.app.criati.empresa.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.CnpjJaCadastradoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.StatusCadastro;

@ExtendWith(MockitoExtension.class)
class CadastrarEmpresaServiceTests {

	@Mock
	private EmpresaRepository empresaRepository;

	@Captor
	private ArgumentCaptor<Empresa> empresaCaptor;

	@InjectMocks
	private CadastrarEmpresaService service;

	@Test
	void deveCadastrarEmpresaValida() {
		when(empresaRepository.save(any(Empresa.class)))
				.thenAnswer(invocacao -> invocacao.getArgument(0));

		Empresa resultado = service.executar(
				"Criati Tecnologia Ltda",
				"Criati",
				"12345678000190");

		verify(empresaRepository).save(empresaCaptor.capture());
		Empresa empresaSalva = empresaCaptor.getValue();
		assertThat(resultado).isSameAs(empresaSalva);
		assertThat(empresaSalva.getNome()).isEqualTo("Criati Tecnologia Ltda");
		assertThat(empresaSalva.getNomeFantasia()).isEqualTo("Criati");
		assertThat(empresaSalva.getCnpj()).isEqualTo("12345678000190");
		assertThat(empresaSalva.getStatus()).isEqualTo(StatusCadastro.ATIVO);
	}

	@Test
	void deveNormalizarCnpjFormatado() {
		service.executar("Empresa Formatada", null, "12.345.678/0001-90");

		verify(empresaRepository).existsByCnpj("12345678000190");
		verify(empresaRepository).save(empresaCaptor.capture());
		assertThat(empresaCaptor.getValue().getCnpj()).isEqualTo("12345678000190");
	}

	@Test
	void deveRejeitarCnpjDuplicado() {
		when(empresaRepository.existsByCnpj("12345678000190")).thenReturn(true);

		assertThatThrownBy(() -> service.executar("Empresa Duplicada", null, "12.345.678/0001-90"))
				.isInstanceOf(CnpjJaCadastradoException.class);

		verify(empresaRepository, never()).save(any(Empresa.class));
	}

	@Test
	void deveRejeitarCnpjInvalido() {
		assertThatThrownBy(() -> service.executar("Empresa Invalida", null, "1234567890123"))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("CNPJ deve possuir exatamente 14 digitos");

		verify(empresaRepository, never()).save(any(Empresa.class));
	}

	@Test
	void deveRejeitarNomeVazio() {
		assertThatThrownBy(() -> service.executar("   ", null, "12345678000190"))
				.isInstanceOf(DadosInvalidosException.class)
				.hasMessage("Nome e obrigatorio");

		verify(empresaRepository, never()).save(any(Empresa.class));
	}
}
