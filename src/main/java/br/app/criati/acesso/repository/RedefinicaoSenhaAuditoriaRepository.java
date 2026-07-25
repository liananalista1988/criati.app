package br.app.criati.acesso.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.acesso.model.RedefinicaoSenhaAuditoria;

public interface RedefinicaoSenhaAuditoriaRepository extends JpaRepository<RedefinicaoSenhaAuditoria, UUID> {

	List<RedefinicaoSenhaAuditoria> findAllByEmpresaIdAndUsuarioAfetadoIdOrderByCriadoEmDesc(
			UUID empresaId, UUID usuarioAfetadoId);
}
