package br.app.criati.financeiro.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.app.criati.financeiro.model.RegraClassificacaoImportacao;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;
import jakarta.persistence.LockModeType;

public interface RegraClassificacaoImportacaoRepository extends JpaRepository<RegraClassificacaoImportacao, UUID> {

	Optional<RegraClassificacaoImportacao> findByIdAndEmpresaId(UUID id, UUID empresaId);

	List<RegraClassificacaoImportacao> findAllByEmpresaIdOrderByCriadoEmDesc(UUID empresaId);

	List<RegraClassificacaoImportacao> findAllByEmpresaIdAndStatusOrderByCriadoEmDesc(
			UUID empresaId, StatusCadastro status);

	// Candidatas para sugestao/aplicacao numa transacao: ativas, do mesmo tipo
	// (Receita/Despesa), sem conta (validas para qualquer conta da empresa) ou
	// da conta especifica da transacao. A comparacao do padrao em si acontece
	// em memoria no service (RegraClassificacaoImportacaoService), nao aqui -
	// volume de regras por empresa e pequeno.
	@Query("""
			select r from RegraClassificacaoImportacao r
			where r.empresa.id = :empresaId
			  and r.status = :status
			  and r.tipo = :tipo
			  and (r.conta.id is null or r.conta.id = :contaId)
			order by r.prioridade desc, r.criadoEm desc
			""")
	List<RegraClassificacaoImportacao> findCandidatasParaClassificacao(
			@Param("empresaId") UUID empresaId,
			@Param("contaId") UUID contaId,
			@Param("tipo") TipoFinanceiro tipo,
			@Param("status") StatusCadastro status);

	// Lock pessimista: usado ao efetivar o uso de uma regra numa confirmacao,
	// para incrementar quantidade_utilizacoes/ultima_utilizacao_em sem perder
	// incremento em confirmacoes concorrentes sobre a mesma regra (mesmo
	// padrao ja usado por FaturaCartaoRepository/LoteImportacaoBancariaRepository).
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RegraClassificacaoImportacao> findForUpdateByIdAndEmpresaId(UUID id, UUID empresaId);

	boolean existsByEmpresaIdAndContaIdAndPadraoNormalizadoAndTipo(
			UUID empresaId, UUID contaId, String padraoNormalizado, TipoFinanceiro tipo);

	boolean existsByEmpresaIdAndContaIdAndPadraoNormalizadoAndTipoAndIdNot(
			UUID empresaId, UUID contaId, String padraoNormalizado, TipoFinanceiro tipo, UUID idAtual);

	boolean existsByEmpresaIdAndContaIdIsNullAndPadraoNormalizadoAndTipo(
			UUID empresaId, String padraoNormalizado, TipoFinanceiro tipo);

	boolean existsByEmpresaIdAndContaIdIsNullAndPadraoNormalizadoAndTipoAndIdNot(
			UUID empresaId, String padraoNormalizado, TipoFinanceiro tipo, UUID idAtual);
}
