package br.app.criati.trabalho.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import br.app.criati.shared.enums.SituacaoTarefaTrabalho;
import br.app.criati.trabalho.model.ProcessoEmpresarial;

public interface ProcessoEmpresarialRepository extends JpaRepository<ProcessoEmpresarial, UUID>,
		JpaSpecificationExecutor<ProcessoEmpresarial> {

	@Override
	@EntityGraph(attributePaths = {"responsavel", "responsavel.usuario", "criadoPor", "atualizadoPor"})
	Page<ProcessoEmpresarial> findAll(Specification<ProcessoEmpresarial> spec, Pageable pageable);

	Optional<ProcessoEmpresarial> findByIdAndEmpresaId(UUID id, UUID empresaId);

	@org.springframework.data.jpa.repository.Query("""
			select (count(t) > 0) from TarefaEmpresarial t
			where t.processo.id = :processoId and t.empresa.id = :empresaId
			and t.status = br.app.criati.shared.enums.StatusCadastro.ATIVO and t.situacao in :situacoes
			""")
	boolean existemTarefasAtivasNaSituacao(@org.springframework.data.repository.query.Param("processoId") UUID processoId,
			@org.springframework.data.repository.query.Param("empresaId") UUID empresaId,
			@org.springframework.data.repository.query.Param("situacoes") java.util.Collection<SituacaoTarefaTrabalho> situacoes);
}
