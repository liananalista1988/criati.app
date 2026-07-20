package br.app.criati.aplicacao.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.empresa.service.CadastrarEmpresaService;
import br.app.criati.shared.enums.CodigoAplicacao;

/**
 * Carga opcional de demonstracao: cria APENAS a empresa "Clinica Vida Demo"
 * com a aplicacao CLINICA habilitada. Nunca cria empresa demo para
 * FINANCEIRO, nunca cria usuario ou senha, nunca cria outro
 * Superadministrador. So executa quando explicitamente habilitada (ver
 * {@link ClinicaVidaDemoRunner}); nunca roda em producao por padrao.
 */
@Service
public class ClinicaVidaDemoService {

	private static final Logger log = LoggerFactory.getLogger(ClinicaVidaDemoService.class);

	// CNPJ de demonstracao com digitos verificadores validos (11.222.333/0001-81),
	// usado apenas para permitir a criacao idempotente da empresa demo; nao
	// pertence a nenhuma empresa real.
	private static final String CNPJ_DEMO = "11222333000181";
	private static final String NOME_EMPRESA_DEMO = "Clinica Vida Demo";

	private final EmpresaRepository empresaRepository;
	private final CadastrarEmpresaService cadastrarEmpresaService;
	private final AplicacaoService aplicacaoService;

	public ClinicaVidaDemoService(
			EmpresaRepository empresaRepository,
			CadastrarEmpresaService cadastrarEmpresaService,
			AplicacaoService aplicacaoService) {
		this.empresaRepository = empresaRepository;
		this.cadastrarEmpresaService = cadastrarEmpresaService;
		this.aplicacaoService = aplicacaoService;
	}

	@Transactional
	public void executar() {
		Empresa empresa = empresaRepository.findByCnpj(CNPJ_DEMO)
				.orElseGet(() -> cadastrarEmpresaService.executar(NOME_EMPRESA_DEMO, NOME_EMPRESA_DEMO, CNPJ_DEMO));

		aplicacaoService.habilitar(empresa.getId(), CodigoAplicacao.CLINICA.name());
		log.info("Carga demo Clinica Vida Demo confirmada (empresa e aplicacao CLINICA habilitada).");
	}
}
