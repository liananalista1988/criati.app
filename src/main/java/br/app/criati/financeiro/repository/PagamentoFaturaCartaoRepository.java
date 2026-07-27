package br.app.criati.financeiro.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.app.criati.financeiro.model.PagamentoFaturaCartao;

public interface PagamentoFaturaCartaoRepository extends JpaRepository<PagamentoFaturaCartao, UUID> {

	List<PagamentoFaturaCartao> findAllByEmpresaIdAndFaturaIdOrderByDataPagamentoDescCriadoEmDesc(
			UUID empresaId, UUID faturaId);

	@Query("select coalesce(sum(p.valor), 0) from PagamentoFaturaCartao p "
			+ "where p.empresa.id = :empresaId and p.fatura.id = :faturaId")
	BigDecimal somarValorPago(@Param("empresaId") UUID empresaId, @Param("faturaId") UUID faturaId);
}
