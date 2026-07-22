package br.app.criati.financeiro.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.OcorrenciaCompromisso;
import br.app.criati.shared.enums.StatusOcorrenciaCompromisso;

public interface OcorrenciaCompromissoRepository extends JpaRepository<OcorrenciaCompromisso, UUID> {

	List<OcorrenciaCompromisso> findAllByEmpresaId(UUID empresaId);

	Optional<OcorrenciaCompromisso> findByIdAndEmpresaId(UUID id, UUID empresaId);

	List<OcorrenciaCompromisso> findAllByEmpresaIdAndCompetencia(UUID empresaId, LocalDate competencia);

	List<OcorrenciaCompromisso> findAllByEmpresaIdAndVencimentoBetween(UUID empresaId, LocalDate inicio, LocalDate fim);

	List<OcorrenciaCompromisso> findAllByEmpresaIdAndStatus(UUID empresaId, StatusOcorrenciaCompromisso status);

	List<OcorrenciaCompromisso> findAllByEmpresaIdAndCompromissoId(UUID empresaId, UUID compromissoId);

	List<OcorrenciaCompromisso> findAllByEmpresaIdAndPessoaFinanceiraId(UUID empresaId, UUID pessoaId);

	List<OcorrenciaCompromisso> findAllByEmpresaIdAndCategoriaId(UUID empresaId, UUID categoriaId);

	List<OcorrenciaCompromisso> findAllByEmpresaIdAndParteFinanceiraId(UUID empresaId, UUID parteId);

	List<OcorrenciaCompromisso> findAllByEmpresaIdAndDescricaoContainingIgnoreCase(UUID empresaId, String descricao);

	boolean existsByRecorrenciaIdAndCompetencia(UUID recorrenciaId, LocalDate competencia);

	Optional<OcorrenciaCompromisso> findByRecorrenciaIdAndCompetencia(UUID recorrenciaId, LocalDate competencia);

	boolean existsByCategoriaIdAndEmpresaId(UUID categoriaId, UUID empresaId);

	boolean existsByPessoaFinanceiraIdAndEmpresaId(UUID pessoaId, UUID empresaId);

	boolean existsByParteFinanceiraIdAndEmpresaId(UUID parteId, UUID empresaId);

	boolean existsByContaPrevistaIdAndEmpresaId(UUID contaId, UUID empresaId);

	boolean existsByCompromissoIdAndEmpresaId(UUID compromissoId, UUID empresaId);
}
