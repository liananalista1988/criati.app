package br.app.criati.convite.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.convite.model.Convite;
import br.app.criati.shared.enums.StatusConvite;

public interface ConviteRepository extends JpaRepository<Convite, UUID> {

	Optional<Convite> findByTokenHash(String tokenHash);

	Optional<Convite> findByIdAndEmpresaId(UUID id, UUID empresaId);

	List<Convite> findAllByEmpresaId(UUID empresaId);

	Optional<Convite> findByEmpresaIdAndEmailIgnoreCaseAndStatus(
			UUID empresaId, String email, StatusConvite status);
}
