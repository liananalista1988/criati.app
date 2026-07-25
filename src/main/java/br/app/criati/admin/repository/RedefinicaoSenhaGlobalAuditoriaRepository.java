package br.app.criati.admin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.admin.model.RedefinicaoSenhaGlobalAuditoria;

public interface RedefinicaoSenhaGlobalAuditoriaRepository extends JpaRepository<RedefinicaoSenhaGlobalAuditoria, UUID> {

	List<RedefinicaoSenhaGlobalAuditoria> findAllByUsuarioAlvoIdOrderByCriadoEmDesc(UUID usuarioAlvoId);

	List<RedefinicaoSenhaGlobalAuditoria> findAllByAdministradorIdOrderByCriadoEmDesc(UUID administradorId);
}
