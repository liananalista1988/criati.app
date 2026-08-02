package br.app.criati.trabalho.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.app.criati.shared.enums.SituacaoTarefaTrabalho;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.trabalho.model.TarefaEmpresarial;

public interface TarefaEmpresarialRepository extends JpaRepository<TarefaEmpresarial, UUID>,
		JpaSpecificationExecutor<TarefaEmpresarial> {

	@Override
	@EntityGraph(attributePaths = {"processo", "responsavel", "responsavel.usuario", "criadoPor", "atualizadoPor"})
	Page<TarefaEmpresarial> findAll(Specification<TarefaEmpresarial> spec, Pageable pageable);

	Optional<TarefaEmpresarial> findByIdAndEmpresaId(UUID id, UUID empresaId);

	List<TarefaEmpresarial> findAllByProcessoIdAndEmpresaId(UUID processoId, UUID empresaId);

	long countByProcessoIdAndEmpresaIdAndStatus(UUID processoId, UUID empresaId, StatusCadastro status);

	long countByProcessoIdAndEmpresaIdAndStatusAndSituacao(UUID processoId, UUID empresaId, StatusCadastro status,
			SituacaoTarefaTrabalho situacao);
}
