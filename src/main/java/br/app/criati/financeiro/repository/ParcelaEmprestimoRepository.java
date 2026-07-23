package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.financeiro.model.ParcelaEmprestimo;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;

public interface ParcelaEmprestimoRepository extends JpaRepository<ParcelaEmprestimo, UUID> {

	List<ParcelaEmprestimo> findAllByEmpresaId(UUID empresaId);

	Optional<ParcelaEmprestimo> findByIdAndEmpresaId(UUID id, UUID empresaId);

	List<ParcelaEmprestimo> findAllByEmpresaIdAndEmprestimoIdOrderByNumero(UUID empresaId, UUID emprestimoId);

	List<ParcelaEmprestimo> findAllByEmpresaIdAndStatus(UUID empresaId, StatusParcelaEmprestimo status);
}
