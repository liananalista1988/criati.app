package br.app.criati.empresa.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.CnpjJaCadastradoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.shared.enums.StatusCadastro;

@Service
public class CadastrarEmpresaService {

	private final EmpresaRepository empresaRepository;

	public CadastrarEmpresaService(EmpresaRepository empresaRepository) {
		this.empresaRepository = empresaRepository;
	}

	@Transactional
	public Empresa executar(String nome, String nomeFantasia, String cnpj) {
		validarObrigatorio(nome, "Nome e obrigatorio");
		validarObrigatorio(cnpj, "CNPJ e obrigatorio");

		String cnpjNormalizado = cnpj.replaceAll("\\D", "");
		if (cnpjNormalizado.length() != 14) {
			throw new DadosInvalidosException("CNPJ deve possuir exatamente 14 digitos");
		}

		if (empresaRepository.existsByCnpj(cnpjNormalizado)) {
			throw new CnpjJaCadastradoException();
		}

		Empresa empresa = new Empresa(nome, nomeFantasia, cnpjNormalizado, StatusCadastro.ATIVO);
		return empresaRepository.save(empresa);
	}

	private void validarObrigatorio(String valor, String mensagem) {
		if (valor == null || valor.isBlank()) {
			throw new DadosInvalidosException(mensagem);
		}
	}
}
