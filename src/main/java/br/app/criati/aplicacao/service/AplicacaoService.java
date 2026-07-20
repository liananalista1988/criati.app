package br.app.criati.aplicacao.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.aplicacao.model.Aplicacao;
import br.app.criati.aplicacao.model.EmpresaAplicacao;
import br.app.criati.aplicacao.repository.AplicacaoRepository;
import br.app.criati.aplicacao.repository.EmpresaAplicacaoRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AplicacaoInativaException;
import br.app.criati.exception.AplicacaoNaoEncontradaException;
import br.app.criati.exception.EmpresaInativaException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.shared.enums.StatusCadastro;

@Service
public class AplicacaoService {

	private final AplicacaoRepository aplicacaoRepository;
	private final EmpresaAplicacaoRepository empresaAplicacaoRepository;
	private final EmpresaRepository empresaRepository;

	public AplicacaoService(
			AplicacaoRepository aplicacaoRepository,
			EmpresaAplicacaoRepository empresaAplicacaoRepository,
			EmpresaRepository empresaRepository) {
		this.aplicacaoRepository = aplicacaoRepository;
		this.empresaAplicacaoRepository = empresaAplicacaoRepository;
		this.empresaRepository = empresaRepository;
	}

	@Transactional(readOnly = true)
	public List<Aplicacao> listarCatalogo() {
		return aplicacaoRepository.findAllByOrderByNomeAsc();
	}

	@Transactional(readOnly = true)
	public List<SituacaoAplicacaoEmpresa> listarParaEmpresa(UUID empresaId) {
		Empresa empresa = buscarEmpresa(empresaId);
		List<EmpresaAplicacao> vinculos = empresaAplicacaoRepository.findAllByEmpresaId(empresa.getId());

		return aplicacaoRepository.findAllByOrderByNomeAsc().stream()
				.map(aplicacao -> new SituacaoAplicacaoEmpresa(
						aplicacao,
						vinculos.stream()
								.filter(v -> v.getAplicacao().getId().equals(aplicacao.getId()))
								.findFirst()
								.map(EmpresaAplicacao::getStatus)
								.orElse(null)))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<Aplicacao> listarAtivasDaEmpresa(UUID empresaId) {
		return empresaAplicacaoRepository.findAllByEmpresaIdAndStatus(empresaId, StatusCadastro.ATIVO).stream()
				.map(EmpresaAplicacao::getAplicacao)
				.filter(Aplicacao::estaAtiva)
				.toList();
	}

	@Transactional(readOnly = true)
	public boolean possuiAplicacaoAtiva(UUID empresaId, String codigo) {
		return aplicacaoRepository.findByCodigo(codigo)
				.filter(Aplicacao::estaAtiva)
				.map(aplicacao -> empresaAplicacaoRepository.existsByEmpresaIdAndAplicacaoIdAndStatus(
						empresaId, aplicacao.getId(), StatusCadastro.ATIVO))
				.orElse(false);
	}

	// Idempotente: habilitar uma aplicacao ja habilitada nao duplica vinculo
	// nem falha, apenas confirma o estado ATIVO (mesmo padrao de reativacao
	// silenciosa usado pela carga demo).
	@Transactional
	public EmpresaAplicacao habilitar(UUID empresaId, String codigo) {
		Empresa empresa = buscarEmpresaAtiva(empresaId);
		Aplicacao aplicacao = buscarAplicacaoAtiva(codigo);

		EmpresaAplicacao vinculo = empresaAplicacaoRepository
				.findByEmpresaIdAndAplicacaoId(empresa.getId(), aplicacao.getId())
				.orElseGet(() -> new EmpresaAplicacao(empresa, aplicacao, StatusCadastro.INATIVO));

		vinculo.habilitar();
		return empresaAplicacaoRepository.save(vinculo);
	}

	// Idempotente: desabilitar uma aplicacao que nunca foi habilitada ou que ja
	// esta inativa nao falha; nunca ha exclusao fisica do vinculo.
	@Transactional
	public void desabilitar(UUID empresaId, String codigo) {
		Empresa empresa = buscarEmpresa(empresaId);
		Aplicacao aplicacao = buscarAplicacao(codigo);

		empresaAplicacaoRepository.findByEmpresaIdAndAplicacaoId(empresa.getId(), aplicacao.getId())
				.ifPresent(vinculo -> {
					vinculo.desabilitar();
					empresaAplicacaoRepository.save(vinculo);
				});
	}

	private Empresa buscarEmpresa(UUID empresaId) {
		return empresaRepository.findById(empresaId).orElseThrow(EmpresaNaoEncontradaException::new);
	}

	private Empresa buscarEmpresaAtiva(UUID empresaId) {
		Empresa empresa = buscarEmpresa(empresaId);
		if (empresa.getStatus() != StatusCadastro.ATIVO) {
			throw new EmpresaInativaException();
		}
		return empresa;
	}

	private Aplicacao buscarAplicacao(String codigo) {
		return aplicacaoRepository.findByCodigo(codigo).orElseThrow(AplicacaoNaoEncontradaException::new);
	}

	private Aplicacao buscarAplicacaoAtiva(String codigo) {
		Aplicacao aplicacao = buscarAplicacao(codigo);
		if (!aplicacao.estaAtiva()) {
			throw new AplicacaoInativaException();
		}
		return aplicacao;
	}
}
