package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CategoriaFinanceiraInativaException;
import br.app.criati.exception.CategoriaFinanceiraNaoEncontradaException;
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.RessarcimentoParcelaCartaoNaoEncontradoException;
import br.app.criati.exception.RessarcimentoParcelaCartaoStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.exception.ValorAReceberParcelaCartaoNaoEncontradoException;
import br.app.criati.exception.ValorAReceberParcelaCartaoStatusInvalidoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.CompraCartao;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.RessarcimentoParcelaCartao;
import br.app.criati.financeiro.model.ValorAReceberParcelaCartao;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.RessarcimentoParcelaCartaoRepository;
import br.app.criati.financeiro.repository.ValorAReceberParcelaCartaoRepository;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusValorAReceberCompraCartao;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Liquidacao (integral ou parcial) e estorno de ressarcimentos de compras
 * para terceiros. Regra central: cada ressarcimento gera exatamente um
 * LancamentoFinanceiro de receita liquidado (origem
 * RESSARCIMENTO_COMPRA_TERCEIRO) — ver a limitacao patrimonial documentada em
 * docs/empresas/financeiro-les/IMPLEMENTACAO-CRIATI-FIN-013.md. O estorno
 * cancela esse mesmo lancamento em vez de criar um mecanismo de reversao
 * paralelo, mesmo desenho ja usado por RecebimentoParcelaEmprestimoService.
 */
@Service
public class RessarcimentoParcelaCartaoService {

	private final RessarcimentoParcelaCartaoRepository ressarcimentoRepository;
	private final ValorAReceberParcelaCartaoRepository valorARepository;
	private final ContaFinanceiraRepository contaRepository;
	private final CategoriaFinanceiraRepository categoriaRepository;
	private final LancamentoFinanceiroRepository lancamentoRepository;
	private final UsuarioRepository usuarioRepository;

	public RessarcimentoParcelaCartaoService(RessarcimentoParcelaCartaoRepository ressarcimentoRepository,
			ValorAReceberParcelaCartaoRepository valorARepository, ContaFinanceiraRepository contaRepository,
			CategoriaFinanceiraRepository categoriaRepository, LancamentoFinanceiroRepository lancamentoRepository,
			UsuarioRepository usuarioRepository) {
		this.ressarcimentoRepository = ressarcimentoRepository;
		this.valorARepository = valorARepository;
		this.contaRepository = contaRepository;
		this.categoriaRepository = categoriaRepository;
		this.lancamentoRepository = lancamentoRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<RessarcimentoParcelaCartao> listar(UUID valorAReceberId, ContextoEmpresaAtual contexto) {
		buscarSemLock(valorAReceberId, contexto.empresaId());
		return ressarcimentoRepository.findAllByEmpresaIdAndValorAReceberIdOrderByDataRessarcimentoDesc(
				contexto.empresaId(), valorAReceberId);
	}

	@Transactional
	public RessarcimentoParcelaCartao receberIntegral(UUID valorAReceberId, UUID contaId, UUID categoriaId,
			LocalDate dataRessarcimento, FormaPagamentoLancamento formaPagamento, String observacao,
			ContextoEmpresaAtual contexto) {
		ValorAReceberParcelaCartao valor = prepararParaRecebimento(valorAReceberId, dataRessarcimento, contexto);
		BigDecimal saldo = valor.getSaldoPendente();
		if (saldo.signum() <= 0) {
			throw new ValorAReceberParcelaCartaoStatusInvalidoException("Valor a receber nao possui saldo pendente");
		}
		return registrarRecebimento(valor, contaId, categoriaId, saldo, dataRessarcimento, formaPagamento, observacao,
				contexto);
	}

	@Transactional
	public RessarcimentoParcelaCartao receberParcial(UUID valorAReceberId, UUID contaId, UUID categoriaId,
			BigDecimal valorInformado, LocalDate dataRessarcimento, FormaPagamentoLancamento formaPagamento,
			String observacao, ContextoEmpresaAtual contexto) {
		ValorAReceberParcelaCartao valor = prepararParaRecebimento(valorAReceberId, dataRessarcimento, contexto);
		if (valorInformado == null || valorInformado.signum() <= 0) {
			throw new DadosInvalidosException("Valor do ressarcimento deve ser maior que zero");
		}
		BigDecimal valorNormalizado = valorInformado.setScale(2, RoundingMode.HALF_UP);
		if (valorNormalizado.compareTo(valor.getSaldoPendente()) > 0) {
			throw new DadosInvalidosException("Valor do ressarcimento nao pode exceder o saldo pendente");
		}
		return registrarRecebimento(valor, contaId, categoriaId, valorNormalizado, dataRessarcimento, formaPagamento,
				observacao, contexto);
	}

	@Transactional
	public RessarcimentoParcelaCartao estornar(UUID valorAReceberId, UUID ressarcimentoId, String motivo,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		ValorAReceberParcelaCartao valor = buscarSemLock(valorAReceberId, contexto.empresaId());
		RessarcimentoParcelaCartao ressarcimento = ressarcimentoRepository
				.findByIdAndValorAReceberIdAndEmpresaId(ressarcimentoId, valor.getId(), contexto.empresaId())
				.orElseThrow(RessarcimentoParcelaCartaoNaoEncontradoException::new);
		if (!ressarcimento.estaAtivo()) {
			throw new RessarcimentoParcelaCartaoStatusInvalidoException("Ressarcimento ja esta estornado");
		}
		Usuario autor = buscarAutor(contexto);
		LancamentoFinanceiro lancamento = ressarcimento.getLancamentoFinanceiro();
		lancamento.cancelar(autor);
		lancamentoRepository.save(lancamento);
		ressarcimento.estornar(motivo, autor);
		ressarcimentoRepository.save(ressarcimento);
		valor.estornarRecebimento(ressarcimento.getValor(), autor);
		return ressarcimento;
	}

	/**
	 * Carrega o valor a receber com PESSIMISTIC_WRITE
	 * (findForUpdateByIdAndEmpresaId) — unico ponto de leitura em todo o fluxo
	 * de ressarcimento (receberIntegral/receberParcial). A partir daqui, dentro
	 * da mesma transacao @Transactional do metodo publico chamador, a linha
	 * fica bloqueada ate o commit: uma segunda requisicao concorrente para o
	 * mesmo valor a receber fica bloqueada nesta mesma leitura e, ao ser
	 * liberada, ve o saldo ja atualizado pela primeira — nunca gera um segundo
	 * ressarcimento/lancamento sobre saldo que ja foi consumido. Mesmo padrao
	 * ja usado por RecebimentoParcelaEmprestimoService.
	 */
	private ValorAReceberParcelaCartao prepararParaRecebimento(UUID valorAReceberId, LocalDate dataRessarcimento,
			ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		if (valorAReceberId == null) {
			throw new ValorAReceberParcelaCartaoNaoEncontradoException();
		}
		ValorAReceberParcelaCartao valor = valorARepository
				.findForUpdateByIdAndEmpresaId(valorAReceberId, contexto.empresaId())
				.orElseThrow(ValorAReceberParcelaCartaoNaoEncontradoException::new);
		if (valor.getStatus() == StatusValorAReceberCompraCartao.CANCELADA) {
			throw new ValorAReceberParcelaCartaoStatusInvalidoException("Valor a receber cancelado nao pode ser ressarcido");
		}
		if (dataRessarcimento == null) {
			throw new DadosInvalidosException("Data de ressarcimento e obrigatoria");
		}
		return valor;
	}

	private RessarcimentoParcelaCartao registrarRecebimento(ValorAReceberParcelaCartao valor, UUID contaId,
			UUID categoriaId, BigDecimal valorRessarcido, LocalDate dataRessarcimento,
			FormaPagamentoLancamento formaPagamento, String observacao, ContextoEmpresaAtual contexto) {
		ContaFinanceira conta = buscarConta(contaId, contexto.empresaId());
		CategoriaFinanceira categoria = buscarCategoriaReceita(categoriaId, contexto.empresaId());
		Usuario autor = buscarAutor(contexto);
		CompraCartao compra = valor.getParcela().getCompra();
		String descricao = descricaoRessarcimento(compra, valor);
		LancamentoFinanceiro lancamento = LancamentoFinanceiro.gerarDeRessarcimentoCompraTerceiro(compra.getEmpresa(),
				conta, categoria, compra.getParteFinanceira(), descricao, valorRessarcido,
				valor.getVencimento().withDayOfMonth(1), valor.getVencimento(), dataRessarcimento, formaPagamento,
				autor);
		LancamentoFinanceiro lancamentoSalvo = lancamentoRepository.save(lancamento);
		RessarcimentoParcelaCartao ressarcimento = new RessarcimentoParcelaCartao(compra.getEmpresa(), valor, conta,
				valorRessarcido, dataRessarcimento, formaPagamento, normalizarOpcional(observacao), lancamentoSalvo,
				autor);
		RessarcimentoParcelaCartao ressarcimentoSalvo = ressarcimentoRepository.save(ressarcimento);
		valor.registrarRecebimento(valorRessarcido, dataRessarcimento, autor);
		return ressarcimentoSalvo;
	}

	private String descricaoRessarcimento(CompraCartao compra, ValorAReceberParcelaCartao valor) {
		String descricao = "Ressarcimento parcela " + valor.getParcela().getNumero() + "/"
				+ valor.getParcela().getTotalParcelas() + " - " + compra.getDescricao();
		return descricao.length() > 200 ? descricao.substring(0, 200) : descricao;
	}

	private ValorAReceberParcelaCartao buscarSemLock(UUID id, UUID empresaId) {
		if (id == null) {
			throw new ValorAReceberParcelaCartaoNaoEncontradoException();
		}
		return valorARepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ValorAReceberParcelaCartaoNaoEncontradoException::new);
	}

	private ContaFinanceira buscarConta(UUID id, UUID empresaId) {
		if (id == null) {
			throw new DadosInvalidosException("Conta e obrigatoria");
		}
		ContaFinanceira conta = contaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva()) {
			throw new ContaFinanceiraInativaException();
		}
		return conta;
	}

	private CategoriaFinanceira buscarCategoriaReceita(UUID id, UUID empresaId) {
		if (id == null) {
			throw new DadosInvalidosException("Categoria e obrigatoria");
		}
		CategoriaFinanceira categoria = categoriaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
		if (!categoria.estaAtiva()) {
			throw new CategoriaFinanceiraInativaException();
		}
		if (categoria.getTipo() != TipoFinanceiro.RECEITA) {
			throw new DadosInvalidosException("Ressarcimento exige categoria do tipo RECEITA");
		}
		return categoria;
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	private void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR && contexto.perfil() != PerfilUsuario.GESTOR) {
			throw new AcessoNegadoException();
		}
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
