package br.app.criati.aplicacao.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.shared.enums.CodigoAplicacao;
import br.app.criati.tenant.ContextoEmpresaAtual;

/**
 * Catálogo técnico e disponibilidade de módulos para o contexto empresarial.
 * A habilitação persistida em EmpresaAplicacao representa disponibilidade para
 * a empresa; contratação, plano e assinatura continuam conceitos comerciais
 * futuros e não são inferidos por este serviço.
 */
@Service
public class ModuloDisponibilidadeService {

	private final AplicacaoService aplicacaoService;

	public ModuloDisponibilidadeService(AplicacaoService aplicacaoService) {
		this.aplicacaoService = aplicacaoService;
	}

	public List<CodigoAplicacao> listarCatalogoTecnico() {
		return CodigoAplicacao.catalogoOrdenado();
	}

	@Transactional(readOnly = true)
	public List<ModuloDisponivelEmpresa> listarDisponiveis(ContextoEmpresaAtual contexto) {
		exigirContexto(contexto);
		return aplicacaoService.listarAtivasDaEmpresa(contexto.empresaId()).stream()
				.map(aplicacao -> CodigoAplicacao.porCodigo(aplicacao.getCodigo()))
				.flatMap(Optional::stream)
				.filter(CodigoAplicacao::estaDisponivelTecnicamente)
				.distinct()
				.sorted(java.util.Comparator.comparingInt(CodigoAplicacao::getOrdemExibicao))
				.map(ModuloDisponivelEmpresa::new)
				.toList();
	}

	@Transactional(readOnly = true)
	public boolean podeVisualizar(CodigoAplicacao modulo, ContextoEmpresaAtual contexto) {
		if (modulo == null || contexto == null || contexto.perfil() == null || !modulo.estaDisponivelTecnicamente()) {
			return false;
		}
		return aplicacaoService.possuiAplicacaoAtiva(contexto.empresaId(), modulo.name());
	}

	@Transactional(readOnly = true)
	public Optional<String> rotaInicialQuandoUnico(ContextoEmpresaAtual contexto) {
		List<ModuloDisponivelEmpresa> modulos = listarDisponiveis(contexto);
		return modulos.size() == 1 ? Optional.of(modulos.get(0).modulo().getRotaInicial()) : Optional.empty();
	}

	private void exigirContexto(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		Objects.requireNonNull(contexto.perfil(), "perfil do contexto e obrigatorio");
	}
}
