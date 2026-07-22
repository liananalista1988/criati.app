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
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.OcorrenciaCompromissoStatusInvalidoException;
import br.app.criati.exception.PagamentoOcorrenciaNaoEncontradoException;
import br.app.criati.exception.PagamentoOcorrenciaStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.LancamentoFinanceiro;
import br.app.criati.financeiro.model.OcorrenciaCompromisso;
import br.app.criati.financeiro.model.PagamentoOcorrenciaCompromisso;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.repository.PagamentoOcorrenciaCompromissoRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusOcorrenciaCompromisso;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Liquidacao (integral ou parcial) e estorno de ocorrencias de compromisso.
 * Regra central: cada pagamento gera exatamente um LancamentoFinanceiro de
 * despesa liquidado (origem CONTA_A_PAGAR); o estorno cancela esse mesmo
 * lancamento (LancamentoFinanceiro.cancelar, ja existente) em vez de criar um
 * mecanismo de reversao paralelo. Ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-F2-007.md.
 */
@Service
public class PagamentoOcorrenciaCompromissoService {

	private final PagamentoOcorrenciaCompromissoRepository pagamentoRepository;
	private final OcorrenciaCompromissoService ocorrenciaService;
	private final LancamentoFinanceiroRepository lancamentoRepository;
	private final UsuarioRepository usuarioRepository;

	public PagamentoOcorrenciaCompromissoService(PagamentoOcorrenciaCompromissoRepository pagamentoRepository,
			OcorrenciaCompromissoService ocorrenciaService, LancamentoFinanceiroRepository lancamentoRepository,
			UsuarioRepository usuarioRepository) {
		this.pagamentoRepository = pagamentoRepository;
		this.ocorrenciaService = ocorrenciaService;
		this.lancamentoRepository = lancamentoRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<PagamentoOcorrenciaCompromisso> listar(UUID ocorrenciaId, ContextoEmpresaAtual contexto) {
		ocorrenciaService.buscarDaEmpresa(ocorrenciaId, contexto.empresaId());
		return pagamentoRepository.findAllByEmpresaIdAndOcorrenciaIdOrderByDataPagamentoDesc(contexto.empresaId(),
				ocorrenciaId);
	}

	@Transactional
	public PagamentoOcorrenciaCompromisso pagarIntegral(UUID ocorrenciaId, UUID contaId, LocalDate dataPagamento,
			br.app.criati.shared.enums.FormaPagamentoLancamento formaPagamento, String observacao,
			ContextoEmpresaAtual contexto) {
		OcorrenciaCompromisso ocorrencia = ocorrenciaService.buscarDaEmpresa(ocorrenciaId, contexto.empresaId());
		BigDecimal saldo = ocorrencia.getSaldoPendente();
		if (saldo.signum() <= 0) {
			throw new OcorrenciaCompromissoStatusInvalidoException("Ocorrencia nao possui saldo pendente");
		}
		return registrarPagamento(ocorrencia, contaId, saldo, dataPagamento, formaPagamento, observacao, contexto);
	}

	@Transactional
	public PagamentoOcorrenciaCompromisso pagarParcial(UUID ocorrenciaId, UUID contaId, BigDecimal valor,
			LocalDate dataPagamento, br.app.criati.shared.enums.FormaPagamentoLancamento formaPagamento,
			String observacao, ContextoEmpresaAtual contexto) {
		OcorrenciaCompromisso ocorrencia = ocorrenciaService.buscarDaEmpresa(ocorrenciaId, contexto.empresaId());
		if (valor == null || valor.signum() <= 0) {
			throw new DadosInvalidosException("Valor do pagamento deve ser maior que zero");
		}
		BigDecimal valorNormalizado = valor.setScale(2, RoundingMode.HALF_UP);
		if (valorNormalizado.compareTo(ocorrencia.getSaldoPendente()) > 0) {
			throw new DadosInvalidosException("Valor do pagamento nao pode exceder o saldo pendente");
		}
		return registrarPagamento(ocorrencia, contaId, valorNormalizado, dataPagamento, formaPagamento, observacao,
				contexto);
	}

	@Transactional
	public PagamentoOcorrenciaCompromisso estornar(UUID ocorrenciaId, UUID pagamentoId, String motivo,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		OcorrenciaCompromisso ocorrencia = ocorrenciaService.buscarDaEmpresa(ocorrenciaId, contexto.empresaId());
		PagamentoOcorrenciaCompromisso pagamento = pagamentoRepository
				.findByIdAndOcorrenciaIdAndEmpresaId(pagamentoId, ocorrencia.getId(), contexto.empresaId())
				.orElseThrow(PagamentoOcorrenciaNaoEncontradoException::new);
		if (!pagamento.estaAtivo()) {
			throw new PagamentoOcorrenciaStatusInvalidoException("Pagamento ja esta estornado");
		}
		Usuario autor = buscarAutor(contexto);
		LancamentoFinanceiro lancamento = pagamento.getLancamentoFinanceiro();
		lancamento.cancelar(autor);
		lancamentoRepository.save(lancamento);
		pagamento.estornar(motivo, autor);
		pagamentoRepository.save(pagamento);
		ocorrencia.estornarPagamento(pagamento.getValor(), autor);
		return pagamento;
	}

	private PagamentoOcorrenciaCompromisso registrarPagamento(OcorrenciaCompromisso ocorrencia, UUID contaId,
			BigDecimal valor, LocalDate dataPagamento, br.app.criati.shared.enums.FormaPagamentoLancamento formaPagamento,
			String observacao, ContextoEmpresaAtual contexto) {
		ocorrenciaService.exigirEscrita(contexto);
		if (ocorrencia.getStatus() == StatusOcorrenciaCompromisso.CANCELADA) {
			throw new OcorrenciaCompromissoStatusInvalidoException("Ocorrencia cancelada nao pode receber pagamento");
		}
		if (dataPagamento == null) {
			throw new DadosInvalidosException("Data de pagamento e obrigatoria");
		}
		ContaFinanceira conta = ocorrenciaService.buscarConta(contaId, contexto.empresaId(), null);
		if (conta == null) {
			throw new DadosInvalidosException("Conta e obrigatoria");
		}
		Usuario autor = buscarAutor(contexto);
		LancamentoFinanceiro lancamento = LancamentoFinanceiro.gerarDeContaAPagar(ocorrencia.getEmpresa(), conta,
				ocorrencia.getCategoria(), ocorrencia.getPessoaFinanceira(), ocorrencia.getParteFinanceira(),
				ocorrencia.getDescricao(), valor, ocorrencia.getCompetencia().atDay(1), ocorrencia.getVencimento(),
				dataPagamento, formaPagamento, autor);
		LancamentoFinanceiro lancamentoSalvo = lancamentoRepository.save(lancamento);
		PagamentoOcorrenciaCompromisso pagamento = new PagamentoOcorrenciaCompromisso(ocorrencia.getEmpresa(),
				ocorrencia, conta, valor, dataPagamento, formaPagamento,
				observacao == null || observacao.isBlank() ? null : observacao.trim(), lancamentoSalvo, autor);
		PagamentoOcorrenciaCompromisso pagamentoSalvo = pagamentoRepository.save(pagamento);
		ocorrencia.registrarPagamento(valor, autor);
		return pagamentoSalvo;
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}
}
