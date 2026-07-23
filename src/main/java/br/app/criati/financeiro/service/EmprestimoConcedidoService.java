package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CategoriaFinanceiraInativaException;
import br.app.criati.exception.CategoriaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.EmprestimoConcedidoNaoEncontradoException;
import br.app.criati.exception.EmprestimoConcedidoStatusInvalidoException;
import br.app.criati.exception.ParteFinanceiraNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.EmprestimoConcedido;
import br.app.criati.financeiro.model.ParcelaEmprestimo;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.EmprestimoConcedidoRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.shared.enums.FormaPagamentoEmprestimo;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusEmprestimoConcedido;
import br.app.criati.shared.enums.StatusParcelaEmprestimo;
import br.app.criati.shared.enums.TipoCobrancaEmprestimo;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Cadastro do emprestimo concedido (a "regra": devedor, principal, cobranca,
 * forma de pagamento) e geracao das parcelas correspondentes. Reaproveita
 * ParcelamentoCartaoService para dividir o principal em parcelas e calcular os
 * vencimentos mensais — a mesma logica generica de arredondamento e
 * distribuicao de datas ja usada por compras parceladas no cartao de credito,
 * sem duplicar esse calculo aqui. Ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-CRIATI-FIN-010.md.
 */
@Service
public class EmprestimoConcedidoService {

	private final EmprestimoConcedidoRepository emprestimoRepository;
	private final ParteFinanceiraRepository parteRepository;
	private final CategoriaFinanceiraRepository categoriaRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final ParcelamentoCartaoService parcelamentoService;

	public EmprestimoConcedidoService(EmprestimoConcedidoRepository emprestimoRepository,
			ParteFinanceiraRepository parteRepository, CategoriaFinanceiraRepository categoriaRepository,
			EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
			ParcelamentoCartaoService parcelamentoService) {
		this.emprestimoRepository = emprestimoRepository;
		this.parteRepository = parteRepository;
		this.categoriaRepository = categoriaRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.parcelamentoService = parcelamentoService;
	}

	@Transactional(readOnly = true)
	public List<EmprestimoConcedido> listar(ContextoEmpresaAtual contexto, StatusEmprestimoConcedido status,
			UUID parteId, UUID categoriaId, String busca) {
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		return emprestimoRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(e -> status == null || e.getStatus() == status)
				.filter(e -> parteId == null || parteId.equals(e.getParteFinanceira().getId()))
				.filter(e -> categoriaId == null || categoriaId.equals(e.getCategoria().getId()))
				.filter(e -> termo.isBlank() || e.getDescricao() != null && e.getDescricao().toLowerCase(Locale.ROOT).contains(termo))
				.sorted((a, b) -> b.getDataConcessao().compareTo(a.getDataConcessao()))
				.toList();
	}

	@Transactional(readOnly = true)
	public EmprestimoConcedido buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional
	public EmprestimoConcedido criar(UUID parteId, UUID categoriaId, String descricao, BigDecimal valorPrincipal,
			LocalDate dataConcessao, TipoCobrancaEmprestimo tipoCobranca, BigDecimal percentualJuros,
			BigDecimal percentualMulta, FormaPagamentoEmprestimo formaPagamento, Integer quantidadeParcelas,
			ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		if (valorPrincipal == null || valorPrincipal.signum() <= 0) {
			throw new DadosInvalidosException("Valor principal deve ser maior que zero");
		}
		if (dataConcessao == null) {
			throw new DadosInvalidosException("Data de concessao e obrigatoria");
		}
		if (tipoCobranca == null) {
			throw new DadosInvalidosException("Configuracao de cobranca e obrigatoria");
		}
		if (formaPagamento == null) {
			throw new DadosInvalidosException("Forma de pagamento e obrigatoria");
		}
		if (descricao != null && descricao.trim().length() > 500) {
			throw new DadosInvalidosException("Descricao deve possuir no maximo 500 caracteres");
		}
		ParteFinanceira parte = buscarParte(parteId, contexto.empresaId());
		CategoriaFinanceira categoria = buscarCategoria(categoriaId, contexto.empresaId());
		Empresa empresa = empresaRepository.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario autor = buscarAutor(contexto);
		// Nao forcar qtd=1 para UNICO aqui: um valor explicitamente diferente de 1 deve chegar
		// intacto ate EmprestimoConcedido, cuja validacao rejeita a combinacao (ver
		// validarQuantidadeParcelas). So preenche um padrao quando o cliente omite o campo.
		int qtd = quantidadeParcelas != null ? quantidadeParcelas
				: formaPagamento == FormaPagamentoEmprestimo.UNICO ? 1 : 0;

		EmprestimoConcedido emprestimo = new EmprestimoConcedido(empresa, parte, categoria,
				normalizarOpcional(descricao), valorPrincipal, dataConcessao, tipoCobranca, percentualJuros,
				percentualMulta, formaPagamento, qtd, autor);

		List<BigDecimal> valoresParcelas = parcelamentoService.dividir(emprestimo.getValorPrincipal(), qtd);
		List<LocalDate> vencimentos = parcelamentoService.competencias(dataConcessao.plusMonths(1), qtd);
		for (int i = 0; i < qtd; i++) {
			emprestimo.adicionarParcela(new ParcelaEmprestimo(empresa, emprestimo, i + 1, qtd,
					valoresParcelas.get(i), vencimentos.get(i), autor));
		}
		return emprestimoRepository.save(emprestimo);
	}

	@Transactional
	public EmprestimoConcedido cancelar(UUID id, String motivo, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		EmprestimoConcedido emprestimo = buscarDaEmpresa(id, contexto.empresaId());
		if (emprestimo.getStatus() == StatusEmprestimoConcedido.CANCELADO) {
			throw new EmprestimoConcedidoStatusInvalidoException("Emprestimo ja esta cancelado");
		}
		if (motivo == null || motivo.isBlank()) {
			throw new DadosInvalidosException("Motivo e obrigatorio");
		}
		if (motivo.trim().length() > 500) {
			throw new DadosInvalidosException("Motivo deve possuir no maximo 500 caracteres");
		}
		Usuario autor = buscarAutor(contexto);
		emprestimo.cancelar(motivo.trim(), autor);
		for (ParcelaEmprestimo parcela : emprestimo.getParcelas()) {
			if (parcela.getStatus() != StatusParcelaEmprestimo.PAGO
					&& parcela.getStatus() != StatusParcelaEmprestimo.CANCELADO) {
				parcela.cancelar(autor);
			}
		}
		return emprestimoRepository.save(emprestimo);
	}

	EmprestimoConcedido buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new EmprestimoConcedidoNaoEncontradoException();
		}
		return emprestimoRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(EmprestimoConcedidoNaoEncontradoException::new);
	}

	private ParteFinanceira buscarParte(UUID id, UUID empresaId) {
		if (id == null) {
			throw new DadosInvalidosException("Pessoa devedora e obrigatoria");
		}
		ParteFinanceira parte = parteRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ParteFinanceiraNaoEncontradaException::new);
		if (!parte.estaAtiva()) {
			throw new DadosInvalidosException("Pessoa devedora esta inativa");
		}
		return parte;
	}

	private CategoriaFinanceira buscarCategoria(UUID id, UUID empresaId) {
		if (id == null) {
			throw new DadosInvalidosException("Categoria e obrigatoria");
		}
		CategoriaFinanceira categoria = categoriaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
		if (!categoria.estaAtiva()) {
			throw new CategoriaFinanceiraInativaException();
		}
		if (categoria.getTipo() != TipoFinanceiro.RECEITA) {
			throw new DadosInvalidosException("Emprestimo concedido exige categoria do tipo RECEITA");
		}
		return categoria;
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarOpcional(String valor) {
		return valor == null || valor.isBlank() ? null : valor.trim();
	}

	void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR && contexto.perfil() != PerfilUsuario.GESTOR) {
			throw new AcessoNegadoException();
		}
	}
}
