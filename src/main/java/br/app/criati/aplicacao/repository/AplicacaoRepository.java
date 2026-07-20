package br.app.criati.aplicacao.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import br.app.criati.aplicacao.model.Aplicacao;

public interface AplicacaoRepository extends JpaRepository<Aplicacao, UUID> {

	Optional<Aplicacao> findByCodigo(String codigo);

	List<Aplicacao> findAllByOrderByNomeAsc();
}
