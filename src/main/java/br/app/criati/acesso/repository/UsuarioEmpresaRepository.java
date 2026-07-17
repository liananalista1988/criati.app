package br.app.criati.acesso.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.acesso.model.UsuarioEmpresa;

public interface UsuarioEmpresaRepository extends JpaRepository<UsuarioEmpresa, UUID> {

	boolean existsByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

	Optional<UsuarioEmpresa> findByUsuarioIdAndEmpresaId(UUID usuarioId, UUID empresaId);

	List<UsuarioEmpresa> findAllByUsuarioId(UUID usuarioId);

	List<UsuarioEmpresa> findAllByEmpresaId(UUID empresaId);
}
