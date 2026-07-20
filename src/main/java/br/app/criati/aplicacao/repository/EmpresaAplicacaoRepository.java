package br.app.criati.aplicacao.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.aplicacao.model.EmpresaAplicacao;
import br.app.criati.shared.enums.StatusCadastro;

public interface EmpresaAplicacaoRepository extends JpaRepository<EmpresaAplicacao, UUID> {

	Optional<EmpresaAplicacao> findByEmpresaIdAndAplicacaoId(UUID empresaId, UUID aplicacaoId);

	List<EmpresaAplicacao> findAllByEmpresaId(UUID empresaId);

	List<EmpresaAplicacao> findAllByEmpresaIdAndStatus(UUID empresaId, StatusCadastro status);

	boolean existsByEmpresaIdAndAplicacaoIdAndStatus(UUID empresaId, UUID aplicacaoId, StatusCadastro status);
}
