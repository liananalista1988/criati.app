package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;

public interface CategoriaFinanceiraRepository extends JpaRepository<CategoriaFinanceira, UUID> {

	List<CategoriaFinanceira> findAllByEmpresaId(UUID empresaId);

	List<CategoriaFinanceira> findAllByEmpresaIdAndStatusOrderByOrdemExibicaoAscNomeAsc(
			UUID empresaId, StatusCadastro status);

	List<CategoriaFinanceira> findAllByEmpresaIdAndCategoriaPaiIdOrderByOrdemExibicaoAscNomeAsc(
			UUID empresaId, UUID categoriaPaiId);

	List<CategoriaFinanceira> findAllByEmpresaIdAndCategoriaPaiIsNullOrderByOrdemExibicaoAscNomeAsc(UUID empresaId);

	Optional<CategoriaFinanceira> findByIdAndEmpresaId(UUID id, UUID empresaId);

	@Query("""
			select (count(c) > 0) from CategoriaFinanceira c
			where c.empresa.id = :empresaId and c.tipo = :tipo and c.status = :status
			and ((:paiId is null and c.categoriaPai is null) or c.categoriaPai.id = :paiId)
			and upper(trim(c.nome)) = upper(trim(:nome))
			and (:ignorarId is null or c.id <> :ignorarId)
			""")
	boolean existeDuplicada(@Param("empresaId") UUID empresaId, @Param("paiId") UUID paiId,
			@Param("tipo") TipoFinanceiro tipo, @Param("nome") String nome,
			@Param("status") StatusCadastro status, @Param("ignorarId") UUID ignorarId);

	boolean existsByEmpresaIdAndCategoriaPaiIdAndStatus(UUID empresaId, UUID categoriaPaiId, StatusCadastro status);

	long countByEmpresaIdAndCategoriaPaiId(UUID empresaId, UUID categoriaPaiId);
}
