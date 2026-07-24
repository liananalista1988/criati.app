package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CompraCartao;
import br.app.criati.financeiro.model.ValorAReceberParcelaCartao;
import br.app.criati.financeiro.repository.ValorAReceberParcelaCartaoRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusValorAReceberCompraCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Compras feitas nos cartoes da residencia para terceiros (amigos,
 * familiares ou outras pessoas): a mesma {@link CompraCartao} usada para
 * compras da residencia, com a diferenca de que aqui a ParteFinanceira
 * (pessoa responsavel pelo ressarcimento) e obrigatoria e cada parcela
 * resultante ganha um {@link ValorAReceberParcelaCartao} para rastrear o
 * ressarcimento. Reaproveita CompraCartaoService.criar() integralmente (nao
 * duplica limite de cartao, parcelamento ou persistencia da compra em si) —
 * esta classe so acrescenta a obrigatoriedade da ParteFinanceira e a criacao
 * dos registros de valor a receber. Ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-CRIATI-FIN-013.md.
 */
@Service
public class CompraTerceiroService {

	private final CompraCartaoService compraCartaoService;
	private final ValorAReceberParcelaCartaoRepository valorARepository;
	private final UsuarioRepository usuarioRepository;

	public CompraTerceiroService(CompraCartaoService compraCartaoService,
			ValorAReceberParcelaCartaoRepository valorARepository, UsuarioRepository usuarioRepository) {
		this.compraCartaoService = compraCartaoService;
		this.valorARepository = valorARepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional
	public ResultadoCompraTerceiro registrar(UUID cartaoId, UUID pessoaResponsavelId, UUID categoriaId, UUID parteId,
			String descricao, LocalDate dataCompra, BigDecimal valorTotal, Integer quantidadeParcelas,
			String observacao, ContextoEmpresaAtual contexto) {
		if (parteId == null) {
			throw new DadosInvalidosException("Pessoa responsavel pelo ressarcimento e obrigatoria para compra para terceiro");
		}
		CompraCartaoService.ResultadoCompra resultadoCompra = compraCartaoService.criar(cartaoId, pessoaResponsavelId,
				categoriaId, parteId, descricao, dataCompra, valorTotal, quantidadeParcelas, observacao, contexto);
		Usuario autor = buscarAutor(contexto);
		CompraCartao compra = resultadoCompra.compra();
		List<ValorAReceberParcelaCartao> valores = compra.getParcelas().stream()
				.map(parcela -> valorARepository.save(new ValorAReceberParcelaCartao(compra.getEmpresa(), parcela, autor)))
				.toList();
		return new ResultadoCompraTerceiro(resultadoCompra, valores);
	}

	@Transactional(readOnly = true)
	public List<CompraCartao> listar(ContextoEmpresaAtual contexto, UUID parteId, String busca) {
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		return comprasParaTerceiro(contexto.empresaId()).stream()
				.filter(c -> parteId == null || (c.getParteFinanceira() != null && parteId.equals(c.getParteFinanceira().getId())))
				.filter(c -> termo.isBlank() || c.getDescricao().toLowerCase(Locale.ROOT).contains(termo))
				.sorted((a, b) -> b.getDataCompra().compareTo(a.getDataCompra()))
				.toList();
	}

	@Transactional(readOnly = true)
	public DetalheCompraTerceiro buscar(UUID compraId, ContextoEmpresaAtual contexto) {
		CompraCartao compra = compraCartaoService.buscar(compraId, contexto);
		List<ValorAReceberParcelaCartao> valores = valorARepository
				.findAllByEmpresaIdAndParcelaCompraId(contexto.empresaId(), compraId);
		if (valores.isEmpty()) {
			throw new DadosInvalidosException("Compra nao e uma compra para terceiro");
		}
		return new DetalheCompraTerceiro(compra, valores);
	}

	@Transactional
	public List<ValorAReceberParcelaCartao> cancelar(UUID compraId, String motivo, ContextoEmpresaAtual contexto) {
		exigirAdmin(contexto);
		compraCartaoService.buscar(compraId, contexto);
		List<ValorAReceberParcelaCartao> valores = valorARepository
				.findAllByEmpresaIdAndParcelaCompraId(contexto.empresaId(), compraId);
		if (valores.isEmpty()) {
			throw new DadosInvalidosException("Compra nao e uma compra para terceiro");
		}
		if (motivo == null || motivo.isBlank() || motivo.trim().length() > 500) {
			throw new DadosInvalidosException("Motivo e obrigatorio e deve possuir ate 500 caracteres");
		}
		Usuario autor = buscarAutor(contexto);
		for (ValorAReceberParcelaCartao valor : valores) {
			if (valor.getStatus() != StatusValorAReceberCompraCartao.RESSARCIDA
					&& valor.getStatus() != StatusValorAReceberCompraCartao.CANCELADA) {
				valor.cancelar(autor);
			}
		}
		return valores;
	}

	private List<CompraCartao> comprasParaTerceiro(UUID empresaId) {
		Map<UUID, CompraCartao> porCompra = new LinkedHashMap<>();
		for (ValorAReceberParcelaCartao valor : valorARepository.findAllByEmpresaId(empresaId)) {
			CompraCartao compra = valor.getParcela().getCompra();
			porCompra.putIfAbsent(compra.getId(), compra);
		}
		return List.copyOf(porCompra.values());
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private void exigirAdmin(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}

	public record ResultadoCompraTerceiro(CompraCartaoService.ResultadoCompra resultadoCompra,
			List<ValorAReceberParcelaCartao> valoresAReceber) {
	}

	public record DetalheCompraTerceiro(CompraCartao compra, List<ValorAReceberParcelaCartao> valoresAReceber) {
	}
}
