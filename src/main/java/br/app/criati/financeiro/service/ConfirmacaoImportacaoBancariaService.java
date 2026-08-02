package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CategoriaFinanceiraInativaException;
import br.app.criati.exception.CategoriaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.LoteImportacaoNaoEncontradoException;
import br.app.criati.exception.LoteImportacaoStatusInvalidoException;
import br.app.criati.exception.TransacaoImportadaNaoEncontradaException;
import br.app.criati.exception.TransacaoImportadaStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.LoteImportacaoBancaria;
import br.app.criati.financeiro.model.PagamentoFaturaCartao;
import br.app.criati.financeiro.model.RegraClassificacaoImportacao;
import br.app.criati.financeiro.model.TransacaoBancariaImportada;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.LoteImportacaoBancariaRepository;
import br.app.criati.financeiro.repository.RegraClassificacaoImportacaoRepository;
import br.app.criati.financeiro.repository.TransacaoBancariaImportadaRepository;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.shared.enums.AplicacaoRegraClassificacaoImportacao;
import br.app.criati.shared.enums.EncaminhamentoSugeridoRegraImportacao;
import br.app.criati.shared.enums.OrigemClassificacaoTransacaoImportada;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.SituacaoTransacaoImportada;
import br.app.criati.shared.enums.StatusLoteImportacao;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.shared.enums.TipoPagamentoFaturaCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Confirma (ou ignora) transacoes bancarias importadas, sempre por acao
 * explicita do usuario (CRIATI-FIN-018, evoluido por CRIATI-IMP-002A).
 *
 * <p>Cada transacao confirmada segue exatamente um de dois caminhos,
 * decidido pelo comando recebido (nunca inferido automaticamente):
 * <ul>
 *   <li>{@code categoriaId} informado: cria um LancamentoFinanceiro comum
 *       (LancamentoFinanceiro.gerarDeImportacao);</li>
 *   <li>{@code faturaId} informado: a transacao representa o pagamento de
 *       uma fatura de cartao ja existente - o pagamento e registrado via
 *       PagamentoFaturaCartaoService.registrarPagamento (que ja cuida de
 *       lock pessimista, saldo devido e cria seu proprio LancamentoFinanceiro),
 *       nunca criando um LancamentoFinanceiro generico que duplicaria a
 *       despesa. A fatura especifica e sempre escolhida explicitamente pelo
 *       usuario - nenhuma correspondencia automatica por valor/data e feita
 *       aqui; se o usuario nao souber qual fatura corresponde, a transacao
 *       simplesmente nao e submetida nesta rodada e permanece pendente para
 *       revisao posterior.</li>
 * </ul>
 *
 * <p>regraClassificacaoId (opcional) so e considerado "efetivamente usado"
 * -e so entao conta para OrigemClassificacaoTransacaoImportada e para o
 * contador de uso da regra- quando o que foi de fato confirmado bate com o
 * que a regra sugeria (mesma categoria, ou mesmo encaminhamento de fatura).
 * Qualquer alteracao material volta a origem para MANUAL, sem incrementar
 * a regra (ajuste obrigatorio 9 de CRIATI-IMP-002A).
 */
@Service
public class ConfirmacaoImportacaoBancariaService {

	private final LoteImportacaoBancariaRepository loteRepository;
	private final TransacaoBancariaImportadaRepository transacaoRepository;
	private final CategoriaFinanceiraRepository categoriaRepository;
	private final LancamentoFinanceiroRepository lancamentoRepository;
	private final RegraClassificacaoImportacaoRepository regraRepository;
	private final RegraClassificacaoImportacaoService regraService;
	private final PagamentoFaturaCartaoService pagamentoFaturaCartaoService;
	private final UsuarioRepository usuarioRepository;

	public ConfirmacaoImportacaoBancariaService(LoteImportacaoBancariaRepository loteRepository,
			TransacaoBancariaImportadaRepository transacaoRepository,
			CategoriaFinanceiraRepository categoriaRepository,
			LancamentoFinanceiroRepository lancamentoRepository,
			RegraClassificacaoImportacaoRepository regraRepository,
			RegraClassificacaoImportacaoService regraService,
			PagamentoFaturaCartaoService pagamentoFaturaCartaoService,
			UsuarioRepository usuarioRepository) {
		this.loteRepository = loteRepository;
		this.transacaoRepository = transacaoRepository;
		this.categoriaRepository = categoriaRepository;
		this.lancamentoRepository = lancamentoRepository;
		this.regraRepository = regraRepository;
		this.regraService = regraService;
		this.pagamentoFaturaCartaoService = pagamentoFaturaCartaoService;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<TransacaoBancariaImportada> listarPendentes(UUID loteId, ContextoEmpresaAtual contexto) {
		LoteImportacaoBancaria lote = buscarLote(loteId, exigirContexto(contexto).empresaId());
		return transacaoRepository.findAllByEmpresaIdAndLoteIdAndSituacaoOrderBySequenciaAsc(
				contexto.empresaId(), lote.getId(), SituacaoTransacaoImportada.PENDENTE);
	}

	@Transactional(readOnly = true)
	public ResumoImportacaoBancaria resumir(UUID loteId, ContextoEmpresaAtual contexto) {
		LoteImportacaoBancaria lote = buscarLote(loteId, exigirContexto(contexto).empresaId());
		return resumir(transacaoRepository.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(
				contexto.empresaId(), lote.getId()));
	}

	/**
	 * Sugestao/pre-preenchimento por regra para cada transacao pendente do
	 * lote - somente leitura, nunca confirma nem persiste nada. Usada pela
	 * tela de revisao para pre-preencher a linha (CRIATI-IMP-002A).
	 */
	@Transactional(readOnly = true)
	public Map<UUID, RegraClassificacaoImportacao> sugestoesPorTransacao(UUID loteId, ContextoEmpresaAtual contexto) {
		LoteImportacaoBancaria lote = buscarLote(loteId, exigirContexto(contexto).empresaId());
		List<TransacaoBancariaImportada> pendentes = transacaoRepository
				.findAllByEmpresaIdAndLoteIdAndSituacaoOrderBySequenciaAsc(
						contexto.empresaId(), lote.getId(), SituacaoTransacaoImportada.PENDENTE);
		Map<UUID, RegraClassificacaoImportacao> sugestoes = new HashMap<>();
		for (TransacaoBancariaImportada transacao : pendentes) {
			TipoFinanceiro tipo = tipoDa(transacao);
			regraService.sugerirParaTransacao(transacao.getDescricao(), lote.getConta().getId(), tipo, contexto)
					.ifPresent(regra -> sugestoes.put(transacao.getId(), regra));
		}
		return sugestoes;
	}

	@Transactional
	public ResultadoConfirmacaoImportacao confirmar(UUID loteId, List<ConfirmacaoTransacaoImportada> comandos,
			ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		if (comandos == null || comandos.isEmpty()) {
			throw new DadosInvalidosException("Selecione ao menos uma transacao para confirmar");
		}
		Set<UUID> ids = new HashSet<>();
		Map<UUID, ConfirmacaoTransacaoImportada> porId = new HashMap<>();
		for (ConfirmacaoTransacaoImportada comando : comandos) {
			if (comando == null || comando.transacaoId() == null || !ids.add(comando.transacaoId())) {
				throw new DadosInvalidosException("Transacoes selecionadas devem ser unicas e validas");
			}
			if ((comando.categoriaId() == null) == (comando.faturaId() == null)) {
				throw new DadosInvalidosException(
						"Informe categoria (lancamento comum) ou fatura (pagamento de fatura), nunca os dois nem nenhum");
			}
			porId.put(comando.transacaoId(), comando);
		}

		LoteImportacaoBancaria lote = buscarLoteParaAtualizar(loteId, contexto.empresaId());
		exigirLoteConfirmavel(lote);
		List<TransacaoBancariaImportada> transacoes = transacaoRepository.findAllForUpdate(
				contexto.empresaId(), lote.getId(), new ArrayList<>(ids));
		if (transacoes.size() != ids.size()) {
			throw new TransacaoImportadaNaoEncontradaException();
		}

		Usuario autor = usuarioRepository.findById(contexto.usuarioId())
				.orElseThrow(UsuarioNaoEncontradoException::new);
		Map<UUID, CategoriaFinanceira> categorias = new HashMap<>();
		List<ConfirmacaoValidada> validadas = new ArrayList<>();
		for (TransacaoBancariaImportada transacao : transacoes) {
			if (transacao.getSituacao() == SituacaoTransacaoImportada.CONFIRMADA) {
				continue;
			}
			if (transacao.getSituacao() == SituacaoTransacaoImportada.IGNORADA) {
				throw new TransacaoImportadaStatusInvalidoException("Transacao ignorada nao pode ser confirmada");
			}
			ConfirmacaoTransacaoImportada comando = porId.get(transacao.getId());
			if (transacao.estaSinalizadaComoDuplicada() && !comando.confirmarDuplicidade()) {
				throw new TransacaoImportadaStatusInvalidoException(
						"Duplicidade sinalizada exige confirmacao explicita");
			}
			TipoFinanceiro tipo = tipoDa(transacao);
			RegraClassificacaoImportacao regraReferenciada = buscarRegraAtivaOpcional(
					comando.regraClassificacaoId(), contexto.empresaId());
			String descricao = normalizarDescricao(comando.descricaoFinal());
			if (comando.faturaId() != null) {
				validadas.add(new ConfirmacaoValidada(transacao, null, tipo, descricao, comando.faturaId(), regraReferenciada));
			} else {
				CategoriaFinanceira categoria = buscarCategoria(comando.categoriaId(), contexto.empresaId(), categorias);
				if (categoria.getTipo() != tipo) {
					throw new DadosInvalidosException("Categoria deve possuir o mesmo tipo financeiro da transacao");
				}
				validadas.add(new ConfirmacaoValidada(transacao, categoria, tipo, descricao, null, regraReferenciada));
			}
		}
		for (ConfirmacaoValidada validada : validadas) {
			aplicarConfirmacao(validada, lote, autor, contexto);
		}
		lancamentoRepository.flush();
		transacaoRepository.flush();
		List<TransacaoBancariaImportada> todas = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(contexto.empresaId(), lote.getId());
		return new ResultadoConfirmacaoImportacao(transacoes, resumir(todas));
	}

	private void aplicarConfirmacao(ConfirmacaoValidada validada, LoteImportacaoBancaria lote, Usuario autor,
			ContextoEmpresaAtual contexto) {
		LancamentoFinanceiro lancamento;
		boolean regraEfetivamenteUsada;
		if (validada.faturaId() != null) {
			// Pagamento fatura: PagamentoFaturaCartaoService ja garante lock
			// pessimista na fatura, saldo devido recalculado e cria seu proprio
			// LancamentoFinanceiro (categoria tecnica propria) - nunca criamos
			// um LancamentoFinanceiro generico aqui, o que duplicaria a
			// despesa ja registrada pela compra no cartao.
			PagamentoFaturaCartao pagamento = pagamentoFaturaCartaoService.registrarPagamento(
					validada.faturaId(), lote.getConta().getId(), validada.transacao().getDataTransacao(),
					validada.transacao().getValor().abs(), TipoPagamentoFaturaCartao.PARCIAL, null, contexto);
			lancamento = pagamento.getLancamentoFinanceiro();
			regraEfetivamenteUsada = validada.regra() != null
					&& validada.regra().getEncaminhamentoSugerido() == EncaminhamentoSugeridoRegraImportacao.FATURA_CARTAO;
		} else {
			PessoaFinanceira pessoaSugerida = null;
			boolean categoriaBateComRegra = validada.regra() != null
					&& validada.regra().getCategoria().getId().equals(validada.categoria().getId());
			if (categoriaBateComRegra) {
				pessoaSugerida = validada.regra().getPessoaFinanceira();
			}
			lancamento = LancamentoFinanceiro.gerarDeImportacao(lote.getEmpresa(), lote.getConta(),
					validada.categoria(), pessoaSugerida, validada.tipo(), validada.descricao(),
					validada.transacao().getValor().abs(), validada.transacao().getDataTransacao(), autor);
			lancamentoRepository.save(lancamento);
			regraEfetivamenteUsada = categoriaBateComRegra;
		}

		OrigemClassificacaoTransacaoImportada origem = OrigemClassificacaoTransacaoImportada.MANUAL;
		RegraClassificacaoImportacao regraParaPersistir = null;
		if (regraEfetivamenteUsada) {
			regraParaPersistir = validada.regra();
			origem = validada.regra().getAplicacao() == AplicacaoRegraClassificacaoImportacao.AUTOMATICA
					? OrigemClassificacaoTransacaoImportada.REGRA_AUTOMATICA
					: OrigemClassificacaoTransacaoImportada.REGRA_SUGERIDA;
			// So incrementa o contador quando a classificacao foi efetivamente
			// confirmada (ajuste obrigatorio 7) - nunca so por ter sido
			// sugerida/pre-preenchida.
			regraService.registrarUso(validada.regra().getId(), contexto);
		}
		validada.transacao().confirmar(lancamento, autor, regraParaPersistir, origem);
	}

	@Transactional
	public ResultadoConfirmacaoImportacao ignorar(UUID loteId, UUID transacaoId, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		LoteImportacaoBancaria lote = buscarLoteParaAtualizar(loteId, contexto.empresaId());
		exigirLoteConfirmavel(lote);
		TransacaoBancariaImportada transacao = transacaoRepository.findForUpdateById(
				contexto.empresaId(), lote.getId(), transacaoId)
				.orElseThrow(TransacaoImportadaNaoEncontradaException::new);
		if (transacao.getSituacao() == SituacaoTransacaoImportada.CONFIRMADA) {
			throw new TransacaoImportadaStatusInvalidoException("Transacao confirmada nao pode ser ignorada");
		}
		transacao.ignorar(usuarioRepository.findById(contexto.usuarioId())
				.orElseThrow(UsuarioNaoEncontradoException::new));
		transacaoRepository.flush();
		List<TransacaoBancariaImportada> todas = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(contexto.empresaId(), lote.getId());
		return new ResultadoConfirmacaoImportacao(List.of(transacao), resumir(todas));
	}

	private CategoriaFinanceira buscarCategoria(UUID categoriaId, UUID empresaId,
			Map<UUID, CategoriaFinanceira> categorias) {
		if (categoriaId == null) {
			throw new CategoriaFinanceiraNaoEncontradaException();
		}
		CategoriaFinanceira categoria = categorias.computeIfAbsent(categoriaId, id -> categoriaRepository
				.findByIdAndEmpresaId(id, empresaId).orElseThrow(CategoriaFinanceiraNaoEncontradaException::new));
		if (!categoria.estaAtiva()) {
			throw new CategoriaFinanceiraInativaException();
		}
		return categoria;
	}

	// Regra referenciada por uma confirmacao e sempre opcional e tolerante:
	// se foi desativada ou removida entre a sugestao ser mostrada e a
	// confirmacao ser enviada, simplesmente deixa de contar como "regra
	// usada" (origem cai para MANUAL) em vez de falhar a confirmacao inteira.
	private RegraClassificacaoImportacao buscarRegraAtivaOpcional(UUID regraId, UUID empresaId) {
		if (regraId == null) {
			return null;
		}
		return regraRepository.findByIdAndEmpresaId(regraId, empresaId)
				.filter(RegraClassificacaoImportacao::estaAtiva)
				.orElse(null);
	}

	private TipoFinanceiro tipoDa(TransacaoBancariaImportada transacao) {
		return transacao.getValor().compareTo(BigDecimal.ZERO) > 0 ? TipoFinanceiro.RECEITA : TipoFinanceiro.DESPESA;
	}

	private String normalizarDescricao(String descricao) {
		if (descricao == null || descricao.isBlank()) {
			throw new DadosInvalidosException("Descricao final e obrigatoria");
		}
		String normalizada = descricao.trim();
		if (normalizada.length() > 200) {
			throw new DadosInvalidosException("Descricao final deve possuir no maximo 200 caracteres");
		}
		return normalizada;
	}

	private ResumoImportacaoBancaria resumir(List<TransacaoBancariaImportada> transacoes) {
		long pendentes = transacoes.stream().filter(t -> t.getSituacao() == SituacaoTransacaoImportada.PENDENTE).count();
		long confirmadas = transacoes.stream().filter(t -> t.getSituacao() == SituacaoTransacaoImportada.CONFIRMADA).count();
		long ignoradas = transacoes.stream().filter(t -> t.getSituacao() == SituacaoTransacaoImportada.IGNORADA).count();
		long duplicadas = transacoes.stream().filter(TransacaoBancariaImportada::estaSinalizadaComoDuplicada).count();
		return new ResumoImportacaoBancaria(pendentes, confirmadas, ignoradas, duplicadas);
	}

	private LoteImportacaoBancaria buscarLote(UUID loteId, UUID empresaId) {
		if (loteId == null) {
			throw new LoteImportacaoNaoEncontradoException();
		}
		return loteRepository.findByIdAndEmpresaId(loteId, empresaId)
				.orElseThrow(LoteImportacaoNaoEncontradoException::new);
	}

	private LoteImportacaoBancaria buscarLoteParaAtualizar(UUID loteId, UUID empresaId) {
		if (loteId == null) {
			throw new LoteImportacaoNaoEncontradoException();
		}
		return loteRepository.findForUpdateByIdAndEmpresaId(loteId, empresaId)
				.orElseThrow(LoteImportacaoNaoEncontradoException::new);
	}

	private void exigirLoteConfirmavel(LoteImportacaoBancaria lote) {
		if (lote.getStatus() == StatusLoteImportacao.DESCARTADO) {
			throw new LoteImportacaoStatusInvalidoException("Lote descartado nao pode ser processado");
		}
	}

	private ContextoEmpresaAtual exigirContexto(ContextoEmpresaAtual contexto) {
		return Objects.requireNonNull(contexto, "contexto e obrigatorio");
	}

	private void exigirEscrita(ContextoEmpresaAtual contexto) {
		exigirContexto(contexto);
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR && contexto.perfil() != PerfilUsuario.GESTOR) {
			throw new AcessoNegadoException();
		}
	}

	private record ConfirmacaoValidada(TransacaoBancariaImportada transacao, CategoriaFinanceira categoria,
			TipoFinanceiro tipo, String descricao, UUID faturaId, RegraClassificacaoImportacao regra) {
	}
}
