package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.exception.ValorAReceberParcelaCartaoNaoEncontradoException;
import br.app.criati.financeiro.model.ValorAReceberParcelaCartao;
import br.app.criati.financeiro.repository.ValorAReceberParcelaCartaoRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusValorAReceberCompraCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Consulta e pequenas edicoes (data prometida) dos valores a receber de
 * compras para terceiros. Ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-CRIATI-FIN-013.md.
 */
@Service
public class ValorAReceberParcelaCartaoService {

	/** Antecedencia padrao para "proximas do vencimento" — mesmo criterio ja usado em contas a pagar e emprestimos. */
	public static final int ALERTA_DIAS_ANTECEDENCIA = OcorrenciaCompromissoService.ALERTA_DIAS_ANTECEDENCIA;

	private final ValorAReceberParcelaCartaoRepository repository;
	private final UsuarioRepository usuarioRepository;

	public ValorAReceberParcelaCartaoService(ValorAReceberParcelaCartaoRepository repository,
			UsuarioRepository usuarioRepository) {
		this.repository = repository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<ValorAReceberParcelaCartao> listar(ContextoEmpresaAtual contexto, UUID compraId,
			StatusValorAReceberCompraCartao status, Boolean atrasadas) {
		LocalDate hoje = LocalDate.now();
		return repository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(v -> compraId == null || compraId.equals(v.getParcela().getCompra().getId()))
				.filter(v -> status == null || v.getStatus() == status)
				.filter(v -> atrasadas == null || v.estaAtrasada(hoje) == atrasadas)
				.sorted((a, b) -> a.getVencimento().compareTo(b.getVencimento()))
				.toList();
	}

	@Transactional(readOnly = true)
	public ValorAReceberParcelaCartao buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public List<ValorAReceberParcelaCartao> listarVencidas(ContextoEmpresaAtual contexto) {
		LocalDate hoje = LocalDate.now();
		return repository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(v -> v.estaAtrasada(hoje))
				.sorted((a, b) -> a.getVencimento().compareTo(b.getVencimento()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ValorAReceberParcelaCartao> listarProximasDoVencimento(ContextoEmpresaAtual contexto,
			Integer diasAntecedencia) {
		LocalDate hoje = LocalDate.now();
		LocalDate limite = hoje.plusDays(diasAntecedencia == null ? ALERTA_DIAS_ANTECEDENCIA : diasAntecedencia);
		return repository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(v -> v.getStatus() == StatusValorAReceberCompraCartao.PENDENTE
						|| v.getStatus() == StatusValorAReceberCompraCartao.PARCIALMENTE_RESSARCIDA)
				.filter(v -> v.getSaldoPendente().signum() > 0)
				.filter(v -> !v.getVencimento().isBefore(hoje) && !v.getVencimento().isAfter(limite))
				.sorted((a, b) -> a.getVencimento().compareTo(b.getVencimento()))
				.toList();
	}

	@Transactional(readOnly = true)
	public ResumoValoresAReceberCompraTerceiro resumir(ContextoEmpresaAtual contexto) {
		LocalDate hoje = LocalDate.now();
		List<ValorAReceberParcelaCartao> valores = repository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(v -> v.getStatus() != StatusValorAReceberCompraCartao.CANCELADA)
				.toList();
		BigDecimal totalPrincipal = somar(valores, ValorAReceberParcelaCartao::getValorTotal);
		BigDecimal totalRessarcido = somar(valores, ValorAReceberParcelaCartao::getValorRecebido);
		BigDecimal saldoAReceber = somar(valores, ValorAReceberParcelaCartao::getSaldoPendente);
		long pendentes = valores.stream().filter(v -> v.getStatus() == StatusValorAReceberCompraCartao.PENDENTE).count();
		long parciais = valores.stream().filter(v -> v.getStatus() == StatusValorAReceberCompraCartao.PARCIALMENTE_RESSARCIDA).count();
		long ressarcidas = valores.stream().filter(v -> v.getStatus() == StatusValorAReceberCompraCartao.RESSARCIDA).count();
		long vencidas = valores.stream().filter(v -> v.estaAtrasada(hoje)).count();
		LocalDate limiteAlerta = hoje.plusDays(ALERTA_DIAS_ANTECEDENCIA);
		long vencendoEmBreve = valores.stream()
				.filter(v -> v.getSaldoPendente().signum() > 0)
				.filter(v -> !v.getVencimento().isBefore(hoje) && !v.getVencimento().isAfter(limiteAlerta))
				.count();
		return new ResumoValoresAReceberCompraTerceiro(totalPrincipal, totalRessarcido, saldoAReceber, pendentes,
				parciais, ressarcidas, vencidas, vencendoEmBreve);
	}

	@Transactional
	public ValorAReceberParcelaCartao registrarDataPrometida(UUID id, LocalDate dataPrometida, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		ValorAReceberParcelaCartao valor = buscarDaEmpresa(id, contexto.empresaId());
		if (valor.getStatus() == StatusValorAReceberCompraCartao.CANCELADA) {
			throw new DadosInvalidosException("Valor a receber cancelado nao pode ser alterado");
		}
		Usuario autor = usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
		valor.registrarDataPrometida(dataPrometida, autor);
		return repository.save(valor);
	}

	void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR && contexto.perfil() != PerfilUsuario.GESTOR) {
			throw new AcessoNegadoException();
		}
	}

	ValorAReceberParcelaCartao buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new ValorAReceberParcelaCartaoNaoEncontradoException();
		}
		return repository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ValorAReceberParcelaCartaoNaoEncontradoException::new);
	}

	private BigDecimal somar(List<ValorAReceberParcelaCartao> valores,
			java.util.function.Function<ValorAReceberParcelaCartao, BigDecimal> extrator) {
		return valores.stream().map(extrator).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public record ResumoValoresAReceberCompraTerceiro(BigDecimal totalPrincipal, BigDecimal totalRessarcido,
			BigDecimal saldoAReceber, long quantidadePendente, long quantidadeParcialmenteRessarcida,
			long quantidadeRessarcida, long quantidadeVencida, long quantidadeVencendoEmBreve) {
	}
}
