package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.FaturaCartaoNaoEncontradaException;
import br.app.criati.exception.FaturaCartaoStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.FaturaCartao;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.PagamentoFaturaCartao;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.FaturaCartaoRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.PagamentoFaturaCartaoRepository;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.shared.enums.TipoPagamentoFaturaCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Pagamentos (integral, parcial ou minimo) e encargos manuais de FaturaCartao
 * (LES-F3-005). Mesmo desenho ja usado por RecebimentoParcelaEmprestimoService:
 * lock pessimista na fatura (unico ponto de leitura antes de calcular saldo
 * devido, impedindo dois pagamentos concorrentes sobre o mesmo saldo), um
 * LancamentoFinanceiro por pagamento (origem FATURA, nunca reaproveitado),
 * excedente sobre o saldo devido sempre rejeitado (nunca aceito como sobra
 * silenciosa). Pagamento parcial/minimo nunca altera ParcelaCompraCartao
 * individualmente - apenas o status de FaturaCartao muda (ver
 * FaturaCartao#registrarPagamento). Estorno e escopo de LES-F3-006: nao
 * implementado aqui.
 */
@Service
public class PagamentoFaturaCartaoService {

	private static final String NOME_CATEGORIA_TECNICA = "Pagamento de fatura de cartao";

	private final FaturaCartaoRepository faturas;
	private final PagamentoFaturaCartaoRepository pagamentos;
	private final ContaFinanceiraRepository contas;
	private final LancamentoFinanceiroRepository lancamentos;
	private final CategoriaFinanceiraRepository categorias;
	private final UsuarioRepository usuarios;

	public PagamentoFaturaCartaoService(FaturaCartaoRepository faturas, PagamentoFaturaCartaoRepository pagamentos,
			ContaFinanceiraRepository contas, LancamentoFinanceiroRepository lancamentos,
			CategoriaFinanceiraRepository categorias, UsuarioRepository usuarios) {
		this.faturas = faturas;
		this.pagamentos = pagamentos;
		this.contas = contas;
		this.lancamentos = lancamentos;
		this.categorias = categorias;
		this.usuarios = usuarios;
	}

	@Transactional(readOnly = true)
	public List<PagamentoFaturaCartao> listar(UUID faturaId, ContextoEmpresaAtual contexto) {
		faturas.findByIdAndEmpresaId(faturaId, contexto.empresaId())
				.orElseThrow(FaturaCartaoNaoEncontradaException::new);
		return pagamentos.findAllByEmpresaIdAndFaturaIdOrderByDataPagamentoDescCriadoEmDesc(
				contexto.empresaId(), faturaId);
	}

	// Usado por FaturaCartaoController para compor FaturaCartaoResponse (valorPago
	// nunca e um campo persistido em FaturaCartao, ver javadoc da classe).
	@Transactional(readOnly = true)
	public BigDecimal totalPago(UUID faturaId, ContextoEmpresaAtual contexto) {
		faturas.findByIdAndEmpresaId(faturaId, contexto.empresaId())
				.orElseThrow(FaturaCartaoNaoEncontradaException::new);
		return totalPago(contexto.empresaId(), faturaId);
	}

	@Transactional
	public PagamentoFaturaCartao registrarPagamento(UUID faturaId, UUID contaPagamentoId, LocalDate dataPagamento,
			BigDecimal valorInformado, TipoPagamentoFaturaCartao tipo, FormaPagamentoLancamento formaPagamento,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		if (dataPagamento == null) {
			throw new DadosInvalidosException("Data de pagamento e obrigatoria");
		}
		if (tipo == null) {
			throw new DadosInvalidosException("Tipo de pagamento e obrigatorio");
		}
		// Unico ponto de leitura da fatura antes de calcular o saldo devido: uma
		// segunda requisicao concorrente para a mesma fatura fica bloqueada aqui
		// e, ao ser liberada, ve o saldo ja atualizado pela primeira - nunca gera
		// dois pagamentos/lancamentos sobre saldo ja consumido.
		FaturaCartao fatura = faturas.findForUpdateByIdAndEmpresaId(faturaId, contexto.empresaId())
				.orElseThrow(FaturaCartaoNaoEncontradaException::new);
		BigDecimal totalPagoAtual = totalPago(contexto.empresaId(), fatura.getId());
		BigDecimal saldoDevido = fatura.getValorDevido().subtract(totalPagoAtual).setScale(2, RoundingMode.HALF_UP);
		if (saldoDevido.signum() <= 0) {
			throw new FaturaCartaoStatusInvalidoException("Fatura ja esta quitada");
		}
		BigDecimal valor = tipo == TipoPagamentoFaturaCartao.INTEGRAL ? saldoDevido : normalizarValor(valorInformado);
		if (valor.compareTo(saldoDevido) > 0) {
			throw new DadosInvalidosException("Valor do pagamento nao pode exceder o saldo devido da fatura");
		}
		ContaFinanceira conta = buscarConta(contaPagamentoId, contexto.empresaId());
		Usuario autor = buscarAutor(contexto);
		CategoriaFinanceira categoria = categoriaTecnica(fatura.getEmpresa());
		String descricao = descricaoPagamento(fatura);
		LancamentoFinanceiro lancamento = LancamentoFinanceiro.gerarDeFatura(fatura.getEmpresa(), conta, categoria,
				descricao, valor, fatura.getCompetencia(), fatura.getDataVencimento(), dataPagamento, formaPagamento,
				autor);
		LancamentoFinanceiro lancamentoSalvo = lancamentos.save(lancamento);
		PagamentoFaturaCartao pagamento = new PagamentoFaturaCartao(fatura.getEmpresa(), fatura, conta, dataPagamento,
				valor, tipo, lancamentoSalvo, autor);
		PagamentoFaturaCartao pagamentoSalvo = pagamentos.save(pagamento);
		fatura.registrarPagamento(totalPagoAtual.add(valor), autor);
		return pagamentoSalvo;
	}

	@Transactional
	public FaturaCartao aplicarEncargos(UUID faturaId, BigDecimal juros, BigDecimal multa,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		if ((juros != null && juros.signum() < 0) || (multa != null && multa.signum() < 0)) {
			throw new DadosInvalidosException("Juros e multa nao podem ser negativos");
		}
		FaturaCartao fatura = faturas.findForUpdateByIdAndEmpresaId(faturaId, contexto.empresaId())
				.orElseThrow(FaturaCartaoNaoEncontradaException::new);
		BigDecimal totalPagoAtual = totalPago(contexto.empresaId(), fatura.getId());
		fatura.aplicarEncargos(juros, multa, totalPagoAtual, buscarAutor(contexto));
		return fatura;
	}

	private BigDecimal totalPago(UUID empresaId, UUID faturaId) {
		BigDecimal valor = pagamentos.somarValorPago(empresaId, faturaId);
		return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
	}

	private BigDecimal normalizarValor(BigDecimal valor) {
		if (valor == null || valor.signum() <= 0) {
			throw new DadosInvalidosException("Valor do pagamento deve ser maior que zero");
		}
		return valor.setScale(2, RoundingMode.HALF_UP);
	}

	private ContaFinanceira buscarConta(UUID id, UUID empresaId) {
		if (id == null) {
			throw new DadosInvalidosException("Conta de pagamento e obrigatoria");
		}
		ContaFinanceira conta = contas.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva()) {
			throw new ContaFinanceiraInativaException();
		}
		return conta;
	}

	/**
	 * Find-or-create da categoria tecnica reservada de pagamento de fatura.
	 * CategoriaFinanceira nao tem hoje nenhum campo de codigo/reservada/sistema
	 * e nao ha seed de categorias na criacao da empresa (LancamentoFinanceiro
	 * exige categoria nao-nula em todas as fabricas) - por isso a primeira
	 * chamada por empresa cria a categoria sob demanda, usando o construtor
	 * simples (sem autor: e uma categoria de sistema, nao uma acao humana).
	 * Risco aceito e documentado: sem lock/constraint unica dedicados, duas
	 * primeiras chamadas verdadeiramente concorrentes na mesma empresa
	 * poderiam criar duas linhas com o mesmo nome - mitigacao futura, se
	 * necessario, seria uma constraint unica (empresa_id, nome, tipo).
	 */
	private CategoriaFinanceira categoriaTecnica(Empresa empresa) {
		return categorias
				.findByEmpresaIdAndNomeIgnoreCaseAndTipo(empresa.getId(), NOME_CATEGORIA_TECNICA, TipoFinanceiro.DESPESA)
				.orElseGet(() -> categorias.saveAndFlush(
						new CategoriaFinanceira(empresa, NOME_CATEGORIA_TECNICA, TipoFinanceiro.DESPESA, StatusCadastro.ATIVO)));
	}

	private String descricaoPagamento(FaturaCartao fatura) {
		String descricao = "Pagamento fatura " + fatura.getCartaoPrincipal().getNome() + " - " + fatura.getCompetencia();
		return descricao.length() > 200 ? descricao.substring(0, 200) : descricao;
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarios.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
