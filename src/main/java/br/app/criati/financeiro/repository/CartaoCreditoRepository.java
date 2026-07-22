package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;

public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, UUID> {

	List<CartaoCredito> findAllByEmpresaId(UUID empresaId);

	List<CartaoCredito> findAllByEmpresaIdAndStatus(UUID empresaId, StatusCadastro status);

	Optional<CartaoCredito> findByIdAndEmpresaId(UUID id, UUID empresaId);

	List<CartaoCredito> findAllByEmpresaIdAndTitularId(UUID empresaId, UUID titularId);

	List<CartaoCredito> findAllByEmpresaIdAndInstituicaoId(UUID empresaId, UUID instituicaoId);

	List<CartaoCredito> findAllByEmpresaIdAndTipo(UUID empresaId, TipoCartao tipo);

	List<CartaoCredito> findAllByEmpresaIdAndBloqueadoTrue(UUID empresaId);

	List<CartaoCredito> findAllByEmpresaIdAndCartaoPrincipalId(UUID empresaId, UUID cartaoPrincipalId);

	List<CartaoCredito> findAllByEmpresaIdAndNomeContainingIgnoreCase(UUID empresaId, String nome);

	long countByCartaoPrincipalId(UUID cartaoPrincipalId);

	boolean existsByTitularIdAndEmpresaId(UUID titularId, UUID empresaId);

	boolean existsByInstituicaoIdAndEmpresaId(UUID instituicaoId, UUID empresaId);

	boolean existsByEmpresaIdAndTitularIdAndInstituicaoIdAndUltimosQuatroDigitosAndTipoAndStatusAndIdNot(
			UUID empresaId, UUID titularId, UUID instituicaoId, String ultimosQuatroDigitos, TipoCartao tipo,
			StatusCadastro status, UUID id);
}
