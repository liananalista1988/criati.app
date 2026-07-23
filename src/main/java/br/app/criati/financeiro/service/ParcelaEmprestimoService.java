package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.ParcelaEmprestimoNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.ParcelaEmprestimo;
import br.app.criati.financeiro.repository.ParcelaEmprestimoRepository;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Consulta e pequenas edicoes (data prometida) das parcelas de um emprestimo
 * concedido. Ver docs/empresas/financeiro-les/IMPLEMENTACAO-CRIATI-FIN-010.md.
 */
@Service
public class ParcelaEmprestimoService {

	/** Antecedencia padrao para "parcelas proximas do vencimento" (mesmo criterio usado em contas a pagar). */
	public static final int ALERTA_DIAS_ANTECEDENCIA = OcorrenciaCompromissoService.ALERTA_DIAS_ANTECEDENCIA;

	private final ParcelaEmprestimoRepository parcelaRepository;
	private final EmprestimoConcedidoService emprestimoConcedidoService;
	private final UsuarioRepository usuarioRepository;

	public ParcelaEmprestimoService(ParcelaEmprestimoRepository parcelaRepository,
			EmprestimoConcedidoService emprestimoConcedidoService, UsuarioRepository usuarioRepository) {
		this.parcelaRepository = parcelaRepository;
		this.emprestimoConcedidoService = emprestimoConcedidoService;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<ParcelaEmprestimo> listar(ContextoEmpresaAtual contexto, UUID emprestimoId,
			StatusParcelaEmprestimo status, Boolean atrasadas) {
		LocalDate hoje = LocalDate.now();
		return parcelaRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(p -> emprestimoId == null || emprestimoId.equals(p.getEmprestimo().getId()))
				.filter(p -> status == null || p.getStatus() == status)
				.filter(p -> atrasadas == null || p.estaAtrasada(hoje) == atrasadas)
				.sorted((a, b) -> a.getVencimento().compareTo(b.getVencimento()))
				.toList();
	}

	@Transactional(readOnly = true)
	public ParcelaEmprestimo buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public List<ParcelaEmprestimo> listarVencidas(ContextoEmpresaAtual contexto) {
		LocalDate hoje = LocalDate.now();
		return parcelaRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(p -> p.estaAtrasada(hoje))
				.sorted((a, b) -> a.getVencimento().compareTo(b.getVencimento()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ParcelaEmprestimo> listarProximasDoVencimento(ContextoEmpresaAtual contexto, Integer diasAntecedencia) {
		LocalDate hoje = LocalDate.now();
		LocalDate limite = hoje.plusDays(diasAntecedencia == null ? ALERTA_DIAS_ANTECEDENCIA : diasAntecedencia);
		return parcelaRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(p -> p.getStatus() == StatusParcelaEmprestimo.PENDENTE
						|| p.getStatus() == StatusParcelaEmprestimo.PARCIALMENTE_PAGO)
				.filter(p -> p.getSaldoPendente().signum() > 0)
				.filter(p -> !p.getVencimento().isBefore(hoje) && !p.getVencimento().isAfter(limite))
				.sorted((a, b) -> a.getVencimento().compareTo(b.getVencimento()))
				.toList();
	}

	@Transactional(readOnly = true)
	public ResumoEmprestimosConcedidos resumir(ContextoEmpresaAtual contexto) {
		LocalDate hoje = LocalDate.now();
		List<ParcelaEmprestimo> parcelas = parcelaRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(p -> p.getStatus() != StatusParcelaEmprestimo.CANCELADO)
				.toList();
		BigDecimal totalPrincipal = somar(parcelas, ParcelaEmprestimo::getValorPrincipal);
		BigDecimal totalJuros = somar(parcelas, ParcelaEmprestimo::getJuros);
		BigDecimal totalMultas = somar(parcelas, ParcelaEmprestimo::getMulta);
		BigDecimal totalRecebido = somar(parcelas, ParcelaEmprestimo::getValorRecebido);
		BigDecimal saldoAReceber = somar(parcelas, ParcelaEmprestimo::getSaldoPendente);
		long pendentes = parcelas.stream().filter(p -> p.getStatus() == StatusParcelaEmprestimo.PENDENTE).count();
		long parciais = parcelas.stream().filter(p -> p.getStatus() == StatusParcelaEmprestimo.PARCIALMENTE_PAGO).count();
		long pagas = parcelas.stream().filter(p -> p.getStatus() == StatusParcelaEmprestimo.PAGO).count();
		long vencidas = parcelas.stream().filter(p -> p.estaAtrasada(hoje)).count();
		LocalDate limiteAlerta = hoje.plusDays(ALERTA_DIAS_ANTECEDENCIA);
		long vencendoEmBreve = parcelas.stream()
				.filter(p -> p.getSaldoPendente().signum() > 0)
				.filter(p -> !p.getVencimento().isBefore(hoje) && !p.getVencimento().isAfter(limiteAlerta))
				.count();
		return new ResumoEmprestimosConcedidos(totalPrincipal, totalJuros, totalMultas, totalRecebido, saldoAReceber,
				pendentes, parciais, pagas, vencidas, vencendoEmBreve);
	}

	@Transactional
	public ParcelaEmprestimo registrarDataPrometida(UUID id, LocalDate dataPrometida, ContextoEmpresaAtual contexto) {
		emprestimoConcedidoService.exigirEscrita(contexto);
		ParcelaEmprestimo parcela = buscarDaEmpresa(id, contexto.empresaId());
		if (parcela.getStatus() == StatusParcelaEmprestimo.CANCELADO) {
			throw new DadosInvalidosException("Parcela cancelada nao pode ser alterada");
		}
		Usuario autor = usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
		parcela.registrarDataPrometida(dataPrometida, autor);
		return parcelaRepository.save(parcela);
	}

	/** Delega para EmprestimoConcedidoService.exigirEscrita — nao expor o campo diretamente: um
	 * acesso de campo cruzado entre beans com proxy AOP (@Transactional) le o valor no proxy,
	 * nao no alvo real, e volta nulo. Chamar um metodo passa pelo proxy corretamente. */
	void exigirEscritaEmprestimo(ContextoEmpresaAtual contexto) {
		emprestimoConcedidoService.exigirEscrita(contexto);
	}

	ParcelaEmprestimo buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new ParcelaEmprestimoNaoEncontradaException();
		}
		return parcelaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ParcelaEmprestimoNaoEncontradaException::new);
	}

	private BigDecimal somar(List<ParcelaEmprestimo> parcelas, java.util.function.Function<ParcelaEmprestimo, BigDecimal> extrator) {
		return parcelas.stream().map(extrator).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public record ResumoEmprestimosConcedidos(BigDecimal totalPrincipal, BigDecimal totalJuros, BigDecimal totalMultas,
			BigDecimal totalRecebido, BigDecimal saldoAReceber, long quantidadePendente,
			long quantidadeParcialmentePaga, long quantidadePaga, long quantidadeVencida, long quantidadeVencendoEmBreve) {
	}
}
