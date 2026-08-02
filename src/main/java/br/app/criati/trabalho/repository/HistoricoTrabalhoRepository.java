package br.app.criati.trabalho.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.trabalho.model.HistoricoTrabalho;

public interface HistoricoTrabalhoRepository extends JpaRepository<HistoricoTrabalho, UUID> {

	@EntityGraph(attributePaths = {"autor"})
	List<HistoricoTrabalho> findAllByProcessoIdAndEmpresaIdOrderByOcorridoEmDesc(UUID processoId, UUID empresaId);

	@EntityGraph(attributePaths = {"autor"})
	List<HistoricoTrabalho> findAllByTarefaIdAndEmpresaIdOrderByOcorridoEmDesc(UUID tarefaId, UUID empresaId);
}
