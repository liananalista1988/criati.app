package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CartaoCreditoNaoEncontradoException;
import br.app.criati.exception.CartaoCreditoStatusInvalidoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.PessoaFinanceiraInativaException;
import br.app.criati.exception.PessoaFinanceiraNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.repository.CartaoCreditoRepository;
import br.app.criati.financeiro.repository.CompraCartaoRepository;
import br.app.criati.shared.enums.StatusCompraCartao;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.Bandeira;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Gestao estrutural (cadastral) de cartoes de credito: titularidade,
 * instituicao, bandeira, limites e vinculo fisico/virtual. Compras, parcelas
 * e faturas nao existem nesta entrega — o limite comprometido e sempre zero
 * (ver {@link CartaoCredito#getLimiteComprometidoEfetivo()}). Ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-F3-001.md.
 */
@Service
public class CartaoCreditoService {

	private final CartaoCreditoRepository cartaoRepository;
	private final PessoaFinanceiraRepository pessoaRepository;
	private final InstituicaoFinanceiraService instituicaoService;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final CompraCartaoRepository compraRepository;

	public CartaoCreditoService(CartaoCreditoRepository cartaoRepository, PessoaFinanceiraRepository pessoaRepository,
			InstituicaoFinanceiraService instituicaoService, EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository, CompraCartaoRepository compraRepository) {
		this.cartaoRepository = cartaoRepository;
		this.pessoaRepository = pessoaRepository;
		this.instituicaoService = instituicaoService;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.compraRepository = compraRepository;
	}

	@Transactional(readOnly = true)
	public List<CartaoCredito> listar(ContextoEmpresaAtual contexto, StatusCadastro status, TipoCartao tipo,
			UUID titularId, UUID instituicaoId, Boolean bloqueado, String busca) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		return cartaoRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(c -> status == null || c.getStatus() == status)
				.filter(c -> tipo == null || c.getTipo() == tipo)
				.filter(c -> titularId == null || titularId.equals(c.getTitular().getId()))
				.filter(c -> instituicaoId == null || instituicaoId.equals(c.getInstituicao().getId()))
				.filter(c -> bloqueado == null || c.isBloqueado() == bloqueado)
				.filter(c -> termo.isBlank() || c.getNome().toLowerCase(Locale.ROOT).contains(termo))
				.sorted((a, b) -> a.getNome().compareToIgnoreCase(b.getNome()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<CartaoCredito> listarVirtuais(UUID cartaoPrincipalId, ContextoEmpresaAtual contexto) {
		buscarDaEmpresa(cartaoPrincipalId, contexto.empresaId());
		return cartaoRepository.findAllByEmpresaIdAndCartaoPrincipalId(contexto.empresaId(), cartaoPrincipalId);
	}

	@Transactional(readOnly = true)
	public long contarVirtuais(UUID cartaoPrincipalId) {
		return cartaoRepository.countByCartaoPrincipalId(cartaoPrincipalId);
	}

	@Transactional(readOnly = true)
	public CartaoCredito buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public boolean possuiPossivelDuplicidade(CartaoCredito cartao) {
		if (cartao.getUltimosQuatroDigitos() == null) {
			return false;
		}
		return cartaoRepository
				.existsByEmpresaIdAndTitularIdAndInstituicaoIdAndUltimosQuatroDigitosAndTipoAndStatusAndIdNot(
						cartao.getEmpresa().getId(), cartao.getTitular().getId(), cartao.getInstituicao().getId(),
						cartao.getUltimosQuatroDigitos(), cartao.getTipo(), StatusCadastro.ATIVO, cartao.getId());
	}

	@Transactional(readOnly = true)
	public ResumoCartoesCredito resumir(ContextoEmpresaAtual contexto) {
		List<CartaoCredito> ativos = cartaoRepository.findAllByEmpresaIdAndStatus(contexto.empresaId(), StatusCadastro.ATIVO);
		long fisicos = ativos.stream().filter(CartaoCredito::ehPrincipal).count();
		long virtuais = ativos.stream().filter(CartaoCredito::ehVirtual).count();
		long bloqueados = ativos.stream().filter(CartaoCredito::isBloqueado).count();
		// Apenas cartoes principais entram no consolidado: virtuais compartilham o
		// mesmo limite e nunca sao somados novamente (ver CartaoCredito, javadoc).
		List<CartaoCredito> principaisAtivos = ativos.stream().filter(CartaoCredito::ehPrincipal).toList();
		BigDecimal limiteTotal = somar(principaisAtivos, CartaoCredito::getLimiteTotal);
		BigDecimal limiteSaudavel = somar(principaisAtivos, CartaoCredito::getLimiteSaudavel);
		BigDecimal limiteComprometido = principaisAtivos.stream().map(this::limiteComprometido)
				.reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
		BigDecimal limiteDisponivel = limiteTotal.subtract(limiteComprometido);
		Map<UUID, Long> porTitularQtd = new LinkedHashMap<>();
		Map<UUID, String> porTitularNome = new LinkedHashMap<>();
		Map<UUID, Long> porInstituicaoQtd = new LinkedHashMap<>();
		Map<UUID, String> porInstituicaoNome = new LinkedHashMap<>();
		for (CartaoCredito c : ativos) {
			porTitularQtd.merge(c.getTitular().getId(), 1L, Long::sum);
			porTitularNome.putIfAbsent(c.getTitular().getId(), c.getTitular().getNome());
			porInstituicaoQtd.merge(c.getInstituicao().getId(), 1L, Long::sum);
			porInstituicaoNome.putIfAbsent(c.getInstituicao().getId(), c.getInstituicao().getNome());
		}
		List<ResumoCartoesCredito.CartoesPorTitular> porTitular = porTitularQtd.entrySet().stream()
				.map(e -> new ResumoCartoesCredito.CartoesPorTitular(e.getKey(), porTitularNome.get(e.getKey()), e.getValue()))
				.toList();
		List<ResumoCartoesCredito.CartoesPorInstituicao> porInstituicao = porInstituicaoQtd.entrySet().stream()
				.map(e -> new ResumoCartoesCredito.CartoesPorInstituicao(e.getKey(), porInstituicaoNome.get(e.getKey()), e.getValue()))
				.toList();
		return new ResumoCartoesCredito(ativos.size(), fisicos, virtuais, bloqueados, limiteTotal, limiteSaudavel,
				limiteDisponivel, porTitular, porInstituicao);
	}

	@Transactional(readOnly = true)
	public BigDecimal limiteComprometido(CartaoCredito cartao) {
		CartaoCredito principal = cartao.ehVirtual() ? cartao.getCartaoPrincipal() : cartao;
		BigDecimal valor = compraRepository.somarComprometido(cartao.getEmpresa().getId(), principal.getId(),
				StatusCompraCartao.ATIVA);
		return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
	}

	@Transactional
	public CartaoCredito criar(String nome, UUID titularId, UUID instituicaoId, TipoCartao tipo,
			UUID cartaoPrincipalId, Bandeira bandeira, String ultimosQuatroDigitos, BigDecimal limiteTotal,
			BigDecimal limiteSaudavel, Integer diaFechamento, Integer diaVencimento, String observacao,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		String nomeNormalizado = normalizarNome(nome);
		if (tipo == null) {
			throw new DadosInvalidosException("Tipo e obrigatorio");
		}
		CartaoCredito principal = resolverPrincipal(tipo, cartaoPrincipalId, contexto);
		UUID titularIdEfetivo = titularId != null ? titularId : (principal != null ? principal.getTitular().getId() : null);
		PessoaFinanceira titular = buscarTitular(titularIdEfetivo, contexto.empresaId(), null);
		UUID instituicaoIdEfetivo = instituicaoId != null ? instituicaoId
				: (principal != null ? principal.getInstituicao().getId() : null);
		InstituicaoFinanceira instituicao = instituicaoService.buscarDisponivel(instituicaoIdEfetivo, contexto);
		if (instituicao == null) {
			throw new DadosInvalidosException("Instituicao financeira e obrigatoria");
		}
		Bandeira bandeiraEfetiva = bandeira != null ? bandeira : (principal != null ? principal.getBandeira() : null);
		if (bandeiraEfetiva == null) {
			throw new DadosInvalidosException("Bandeira e obrigatoria");
		}
		String digitosValidados = validarUltimosQuatroDigitos(ultimosQuatroDigitos);
		ValoresEstruturais valores = validarValoresEstruturais(tipo, limiteTotal, limiteSaudavel, diaFechamento, diaVencimento);
		Empresa empresa = empresaRepository.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario autor = buscarAutor(contexto);
		CartaoCredito cartao = new CartaoCredito(empresa, titular, instituicao, nomeNormalizado, tipo, principal,
				bandeiraEfetiva, digitosValidados, valores.limiteTotal(), valores.limiteSaudavel(),
				valores.diaFechamento(), valores.diaVencimento(), normalizarOpcional(observacao), autor);
		return cartaoRepository.save(cartao);
	}

	@Transactional
	public CartaoCredito editar(UUID id, String nome, UUID titularId, UUID instituicaoId, Bandeira bandeira,
			String ultimosQuatroDigitos, BigDecimal limiteTotal, BigDecimal limiteSaudavel, Integer diaFechamento,
			Integer diaVencimento, String observacao, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CartaoCredito atual = buscarDaEmpresa(id, contexto.empresaId());
		String nomeNormalizado = normalizarNome(nome);
		PessoaFinanceira titular = buscarTitular(titularId, contexto.empresaId(), atual.getTitular());
		InstituicaoFinanceira instituicao = instituicaoService.buscarDisponivel(instituicaoId, contexto);
		if (instituicao == null) {
			throw new DadosInvalidosException("Instituicao financeira e obrigatoria");
		}
		if (bandeira == null) {
			throw new DadosInvalidosException("Bandeira e obrigatoria");
		}
		String digitosValidados = validarUltimosQuatroDigitos(ultimosQuatroDigitos);
		ValoresEstruturais valores = validarValoresEstruturais(atual.getTipo(), limiteTotal, limiteSaudavel,
				diaFechamento, diaVencimento);
		atual.atualizarDados(titular, instituicao, nomeNormalizado, bandeira, digitosValidados, valores.limiteTotal(),
				valores.limiteSaudavel(), valores.diaFechamento(), valores.diaVencimento(),
				normalizarOpcional(observacao), buscarAutor(contexto));
		return cartaoRepository.save(atual);
	}

	@Transactional
	public CartaoCredito inativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CartaoCredito cartao = buscarDaEmpresa(id, contexto.empresaId());
		if (!cartao.estaAtivo()) {
			throw new CartaoCreditoStatusInvalidoException("Cartao ja esta inativo");
		}
		cartao.inativar(buscarAutor(contexto));
		return cartaoRepository.save(cartao);
	}

	@Transactional
	public CartaoCredito reativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CartaoCredito cartao = buscarDaEmpresa(id, contexto.empresaId());
		if (cartao.estaAtivo()) {
			throw new CartaoCreditoStatusInvalidoException("Cartao ja esta ativo");
		}
		if (!cartao.getTitular().estaAtiva()) {
			throw new PessoaFinanceiraInativaException();
		}
		cartao.reativar(buscarAutor(contexto));
		return cartaoRepository.save(cartao);
	}

	@Transactional
	public CartaoCredito bloquear(UUID id, String motivo, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CartaoCredito cartao = buscarDaEmpresa(id, contexto.empresaId());
		if (cartao.isBloqueado()) {
			throw new CartaoCreditoStatusInvalidoException("Cartao ja esta bloqueado");
		}
		cartao.bloquear(normalizarOpcional(motivo), buscarAutor(contexto));
		return cartaoRepository.save(cartao);
	}

	@Transactional
	public CartaoCredito desbloquear(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		CartaoCredito cartao = buscarDaEmpresa(id, contexto.empresaId());
		if (!cartao.isBloqueado()) {
			throw new CartaoCreditoStatusInvalidoException("Cartao nao esta bloqueado");
		}
		cartao.desbloquear(buscarAutor(contexto));
		return cartaoRepository.save(cartao);
	}

	private CartaoCredito resolverPrincipal(TipoCartao tipo, UUID cartaoPrincipalId, ContextoEmpresaAtual contexto) {
		if (tipo == TipoCartao.FISICO) {
			if (cartaoPrincipalId != null) {
				throw new DadosInvalidosException("Cartao fisico nao pode possuir cartao principal");
			}
			return null;
		}
		if (cartaoPrincipalId == null) {
			throw new DadosInvalidosException("Cartao virtual exige um cartao principal");
		}
		CartaoCredito principal = buscarDaEmpresa(cartaoPrincipalId, contexto.empresaId());
		if (!principal.ehPrincipal()) {
			throw new DadosInvalidosException("Cartao principal deve ser um cartao fisico");
		}
		if (!principal.estaAtivo()) {
			throw new DadosInvalidosException("Cartao principal precisa estar ativo para novo vinculo");
		}
		return principal;
	}

	private PessoaFinanceira buscarTitular(UUID id, UUID empresaId, PessoaFinanceira atual) {
		if (id == null) {
			throw new DadosInvalidosException("Titular e obrigatorio");
		}
		PessoaFinanceira titular = pessoaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(PessoaFinanceiraNaoEncontradaException::new);
		boolean titularAtual = atual != null && titular.getId().equals(atual.getId());
		if (!titular.estaAtiva() && !titularAtual) {
			throw new PessoaFinanceiraInativaException();
		}
		return titular;
	}

	private String validarUltimosQuatroDigitos(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		String normalizado = valor.trim();
		if (!normalizado.matches("\\d{4}")) {
			throw new DadosInvalidosException("Ultimos quatro digitos devem conter exatamente 4 numeros");
		}
		return normalizado;
	}

	private ValoresEstruturais validarValoresEstruturais(TipoCartao tipo, BigDecimal limiteTotal,
			BigDecimal limiteSaudavel, Integer diaFechamento, Integer diaVencimento) {
		if (tipo == TipoCartao.VIRTUAL) {
			// Cartao virtual nunca gera limite/fechamento/vencimento proprios: os
			// valores efetivos vem sempre do principal (CartaoCredito.getXxxEfetivo()).
			return new ValoresEstruturais(null, null, null, null);
		}
		if (limiteTotal == null) {
			throw new DadosInvalidosException("Limite total e obrigatorio para cartao fisico");
		}
		if (limiteTotal.signum() < 0) {
			throw new DadosInvalidosException("Limite total nao pode ser negativo");
		}
		BigDecimal limiteTotalNormalizado = limiteTotal.setScale(2, RoundingMode.HALF_UP);
		BigDecimal limiteSaudavelNormalizado = null;
		if (limiteSaudavel != null) {
			if (limiteSaudavel.signum() < 0) {
				throw new DadosInvalidosException("Limite saudavel nao pode ser negativo");
			}
			limiteSaudavelNormalizado = limiteSaudavel.setScale(2, RoundingMode.HALF_UP);
			if (limiteSaudavelNormalizado.compareTo(limiteTotalNormalizado) > 0) {
				throw new DadosInvalidosException("Limite saudavel nao pode superar o limite total");
			}
		}
		if (diaFechamento == null || diaFechamento < 1 || diaFechamento > 31) {
			throw new DadosInvalidosException("Dia de fechamento deve estar entre 1 e 31");
		}
		if (diaVencimento == null || diaVencimento < 1 || diaVencimento > 31) {
			throw new DadosInvalidosException("Dia de vencimento deve estar entre 1 e 31");
		}
		if (diaFechamento.equals(diaVencimento)) {
			throw new DadosInvalidosException("Fechamento e vencimento devem ser dias distintos");
		}
		return new ValoresEstruturais(limiteTotalNormalizado, limiteSaudavelNormalizado, diaFechamento, diaVencimento);
	}

	private BigDecimal somar(List<CartaoCredito> itens, java.util.function.Function<CartaoCredito, BigDecimal> extrator) {
		return itens.stream().map(extrator).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	CartaoCredito buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new CartaoCreditoNaoEncontradoException();
		}
		return cartaoRepository.findByIdAndEmpresaId(id, empresaId).orElseThrow(CartaoCreditoNaoEncontradoException::new);
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarNome(String nome) {
		if (nome == null || nome.isBlank()) {
			throw new DadosInvalidosException("Nome e obrigatorio");
		}
		String normalizado = nome.trim();
		if (normalizado.length() > 150) {
			throw new DadosInvalidosException("Nome deve possuir no maximo 150 caracteres");
		}
		return normalizado;
	}

	private String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}

	private record ValoresEstruturais(
			BigDecimal limiteTotal, BigDecimal limiteSaudavel, Integer diaFechamento, Integer diaVencimento) {
	}
}
