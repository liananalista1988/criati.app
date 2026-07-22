package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.shared.enums.StatusCadastro;

public interface InstituicaoFinanceiraRepository extends JpaRepository<InstituicaoFinanceira, UUID> {

	@Query("""
			select i from InstituicaoFinanceira i
			where i.status = :status and (i.empresa is null or i.empresa.id = :empresaId)
			order by i.nome
			""")
	List<InstituicaoFinanceira> listarDisponiveis(
			@Param("empresaId") UUID empresaId, @Param("status") StatusCadastro status);

	@Query("""
			select i from InstituicaoFinanceira i
			where i.id = :id and i.status = :status and (i.empresa is null or i.empresa.id = :empresaId)
			""")
	Optional<InstituicaoFinanceira> buscarDisponivel(
			@Param("id") UUID id,
			@Param("empresaId") UUID empresaId,
			@Param("status") StatusCadastro status);

	@Query("""
			select count(i) > 0 from InstituicaoFinanceira i
			where upper(i.nome) = upper(:nome) and (i.empresa is null or i.empresa.id = :empresaId)
			""")
	boolean existeNomeDisponivel(@Param("empresaId") UUID empresaId, @Param("nome") String nome);

	boolean existsByEmpresaIdAndNomeIgnoreCase(UUID empresaId, String nome);
}
