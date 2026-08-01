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
import br.app.criati.financeiro.model.TransacaoBancariaImportada;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.LoteImportacaoBancariaRepository;
import br.app.criati.financeiro.repository.TransacaoBancariaImportadaRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.SituacaoTransacaoImportada;
import br.app.criati.shared.enums.StatusLoteImportacao;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class ConfirmacaoImportacaoBancariaService {

	private final LoteImportacaoBancariaRepository loteRepository;
	private final TransacaoBancariaImportadaRepository transacaoRepository;
	private final CategoriaFinanceiraRepository categoriaRepository;
	private final LancamentoFinanceiroRepository lancamentoRepository;
	private final UsuarioRepository usuarioRepository;

	public ConfirmacaoImportacaoBancariaService(LoteImportacaoBancariaRepository loteRepository,
			TransacaoBancariaImportadaRepository transacaoRepository,
			CategoriaFinanceiraRepository categoriaRepository,
			LancamentoFinanceiroRepository lancamentoRepository,
			UsuarioRepository usuarioRepository) {
		this.loteRepository = loteRepository;
		this.transacaoRepository = transacaoRepository;
		this.categoriaRepository = categoriaRepository;
		this.lancamentoRepository = lancamentoRepository;
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
			CategoriaFinanceira categoria = buscarCategoria(comando.categoriaId(), contexto.empresaId(), categorias);
			if (categoria.getTipo() != tipo) {
				throw new DadosInvalidosException("Categoria deve possuir o mesmo tipo financeiro da transacao");
			}
			String descricao = normalizarDescricao(comando.descricaoFinal());
			validadas.add(new ConfirmacaoValidada(transacao, categoria, tipo, descricao));
		}
		for (ConfirmacaoValidada validada : validadas) {
			LancamentoFinanceiro lancamento = LancamentoFinanceiro.gerarDeImportacao(
					lote.getEmpresa(), lote.getConta(), validada.categoria(), validada.tipo(), validada.descricao(),
					validada.transacao().getValor().abs(), validada.transacao().getDataTransacao(), autor);
			lancamentoRepository.save(lancamento);
			validada.transacao().confirmar(lancamento, autor);
		}
		lancamentoRepository.flush();
		transacaoRepository.flush();
		List<TransacaoBancariaImportada> todas = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(contexto.empresaId(), lote.getId());
		return new ResultadoConfirmacaoImportacao(transacoes, resumir(todas));
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
			TipoFinanceiro tipo, String descricao) {
	}
}
