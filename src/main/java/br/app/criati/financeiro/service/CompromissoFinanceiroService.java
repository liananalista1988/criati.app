package br.app.criati.financeiro.service;

import java.math.BigDecimal;
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
import br.app.criati.exception.CompromissoFinanceiroNaoEncontradoException;
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.ParteFinanceiraNaoEncontradaException;
import br.app.criati.exception.PessoaFinanceiraInativaException;
import br.app.criati.exception.PessoaFinanceiraNaoEncontradaException;
import br.app.criati.exception.RecorrenciaFinanceiraNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.CompromissoFinanceiro;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.RecorrenciaFinanceira;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.CompromissoFinanceiroRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.RecorrenciaFinanceiraRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.shared.enums.TipoValorCompromisso;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * Regra ou origem de uma obrigacao a pagar. Ver
 * docs/empresas/financeiro-les/IMPLEMENTACAO-F2-007.md para a distincao entre
 * compromisso (esta entidade), ocorrencia e pagamento.
 */
@Service
public class CompromissoFinanceiroService {

	private final CompromissoFinanceiroRepository compromissoRepository;
	private final CategoriaFinanceiraRepository categoriaRepository;
	private final PessoaFinanceiraRepository pessoaRepository;
	private final ParteFinanceiraRepository parteRepository;
	private final ContaFinanceiraRepository contaRepository;
	private final RecorrenciaFinanceiraRepository recorrenciaRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;

	public CompromissoFinanceiroService(CompromissoFinanceiroRepository compromissoRepository,
			CategoriaFinanceiraRepository categoriaRepository, PessoaFinanceiraRepository pessoaRepository,
			ParteFinanceiraRepository parteRepository, ContaFinanceiraRepository contaRepository,
			RecorrenciaFinanceiraRepository recorrenciaRepository, EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository) {
		this.compromissoRepository = compromissoRepository;
		this.categoriaRepository = categoriaRepository;
		this.pessoaRepository = pessoaRepository;
		this.parteRepository = parteRepository;
		this.contaRepository = contaRepository;
		this.recorrenciaRepository = recorrenciaRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<CompromissoFinanceiro> listar(ContextoEmpresaAtual contexto, Boolean ativo, UUID pessoaId,
			UUID categoriaId, String busca) {
		String termo = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
		return compromissoRepository.findAllByEmpresaId(contexto.empresaId()).stream()
				.filter(c -> ativo == null || c.isAtivo() == ativo)
				.filter(c -> pessoaId == null || pessoaId.equals(c.getPessoaFinanceira().getId()))
				.filter(c -> categoriaId == null || categoriaId.equals(c.getCategoria().getId()))
				.filter(c -> termo.isBlank() || c.getDescricao().toLowerCase(Locale.ROOT).contains(termo))
				.sorted((a, b) -> a.getDescricao().compareToIgnoreCase(b.getDescricao()))
				.toList();
	}

	@Transactional(readOnly = true)
	public CompromissoFinanceiro buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional
	public CompromissoFinanceiro criar(String descricao, UUID categoriaId, UUID pessoaId, UUID parteId,
			UUID contaPadraoId, UUID recorrenciaId, TipoValorCompromisso tipoValor, BigDecimal valorPadrao,
			Integer diaVencimentoPadrao, FormaPagamentoLancamento formaPagamentoPadrao, String observacao,
			ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		validarBasicos(descricao, tipoValor, valorPadrao, diaVencimentoPadrao);
		CategoriaFinanceira categoria = buscarCategoria(categoriaId, contexto.empresaId(), null);
		PessoaFinanceira pessoa = buscarPessoa(pessoaId, contexto.empresaId(), null);
		ParteFinanceira parte = buscarParte(parteId, contexto.empresaId(), null);
		ContaFinanceira contaPadrao = buscarConta(contaPadraoId, contexto.empresaId(), null);
		RecorrenciaFinanceira recorrencia = buscarRecorrencia(recorrenciaId, contexto.empresaId(), null);
		Empresa empresa = empresaRepository.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		CompromissoFinanceiro compromisso = new CompromissoFinanceiro(empresa, normalizarDescricao(descricao),
				categoria, pessoa, parte, contaPadrao, recorrencia, tipoValor,
				valorPadrao == null ? null : valorPadrao.setScale(2, java.math.RoundingMode.HALF_UP),
				diaVencimentoPadrao, formaPagamentoPadrao, normalizarOpcional(observacao), buscarAutor(contexto));
		return compromissoRepository.save(compromisso);
	}

	@Transactional
	public CompromissoFinanceiro editar(UUID id, String descricao, UUID categoriaId, UUID pessoaId, UUID parteId,
			UUID contaPadraoId, TipoValorCompromisso tipoValor, BigDecimal valorPadrao, Integer diaVencimentoPadrao,
			FormaPagamentoLancamento formaPagamentoPadrao, String observacao, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		CompromissoFinanceiro atual = buscarDaEmpresa(id, contexto.empresaId());
		validarBasicos(descricao, tipoValor, valorPadrao, diaVencimentoPadrao);
		CategoriaFinanceira categoria = buscarCategoria(categoriaId, contexto.empresaId(), atual.getCategoria());
		PessoaFinanceira pessoa = buscarPessoa(pessoaId, contexto.empresaId(), atual.getPessoaFinanceira());
		ParteFinanceira parte = buscarParte(parteId, contexto.empresaId(), atual.getParteFinanceira());
		ContaFinanceira contaPadrao = buscarConta(contaPadraoId, contexto.empresaId(), atual.getContaPadrao());
		atual.atualizarDados(normalizarDescricao(descricao), categoria, pessoa, parte, contaPadrao, tipoValor,
				valorPadrao == null ? null : valorPadrao.setScale(2, java.math.RoundingMode.HALF_UP),
				diaVencimentoPadrao, formaPagamentoPadrao, normalizarOpcional(observacao), buscarAutor(contexto));
		return compromissoRepository.save(atual);
	}

	@Transactional
	public CompromissoFinanceiro ativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		CompromissoFinanceiro c = buscarDaEmpresa(id, contexto.empresaId());
		c.ativar(buscarAutor(contexto));
		return compromissoRepository.save(c);
	}

	@Transactional
	public CompromissoFinanceiro desativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		CompromissoFinanceiro c = buscarDaEmpresa(id, contexto.empresaId());
		c.desativar(buscarAutor(contexto));
		return compromissoRepository.save(c);
	}

	CompromissoFinanceiro buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new CompromissoFinanceiroNaoEncontradoException();
		}
		return compromissoRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(CompromissoFinanceiroNaoEncontradoException::new);
	}

	private CategoriaFinanceira buscarCategoria(UUID id, UUID empresaId, CategoriaFinanceira atual) {
		if (id == null) {
			throw new DadosInvalidosException("Categoria e obrigatoria");
		}
		CategoriaFinanceira categoria = categoriaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
		if (!categoria.estaAtiva() && (atual == null || !categoria.getId().equals(atual.getId()))) {
			throw new CategoriaFinanceiraInativaException();
		}
		if (categoria.getTipo() != TipoFinanceiro.DESPESA) {
			throw new DadosInvalidosException("Compromisso a pagar exige categoria do tipo DESPESA");
		}
		return categoria;
	}

	private PessoaFinanceira buscarPessoa(UUID id, UUID empresaId, PessoaFinanceira atual) {
		if (id == null) {
			throw new DadosInvalidosException("Pessoa financeira e obrigatoria");
		}
		PessoaFinanceira pessoa = pessoaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(PessoaFinanceiraNaoEncontradaException::new);
		if (!pessoa.estaAtiva() && (atual == null || !pessoa.getId().equals(atual.getId()))) {
			throw new PessoaFinanceiraInativaException();
		}
		return pessoa;
	}

	private ParteFinanceira buscarParte(UUID id, UUID empresaId, ParteFinanceira atual) {
		if (id == null) {
			return null;
		}
		ParteFinanceira parte = parteRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ParteFinanceiraNaoEncontradaException::new);
		if (!parte.estaAtiva() && (atual == null || !parte.getId().equals(atual.getId()))) {
			throw new DadosInvalidosException("Contato financeiro inativo");
		}
		return parte;
	}

	private ContaFinanceira buscarConta(UUID id, UUID empresaId, ContaFinanceira atual) {
		if (id == null) {
			return null;
		}
		ContaFinanceira conta = contaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva() && (atual == null || !conta.getId().equals(atual.getId()))) {
			throw new ContaFinanceiraInativaException();
		}
		return conta;
	}

	private RecorrenciaFinanceira buscarRecorrencia(UUID id, UUID empresaId, RecorrenciaFinanceira atual) {
		if (id == null) {
			return null;
		}
		RecorrenciaFinanceira recorrencia = recorrenciaRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(RecorrenciaFinanceiraNaoEncontradaException::new);
		boolean mesmaRecorrenciaJaVinculada = atual != null && atual.getId().equals(recorrencia.getId());
		if (!mesmaRecorrenciaJaVinculada && compromissoRepository.existsByRecorrenciaId(recorrencia.getId())) {
			throw new DadosInvalidosException("Recorrencia ja esta vinculada a outro compromisso a pagar");
		}
		if (recorrencia.getTipo() != TipoFinanceiro.DESPESA) {
			throw new DadosInvalidosException("Compromisso a pagar exige recorrencia do tipo DESPESA");
		}
		return recorrencia;
	}

	private void validarBasicos(String descricao, TipoValorCompromisso tipoValor, BigDecimal valorPadrao,
			Integer diaVencimentoPadrao) {
		if (descricao == null || descricao.isBlank()) {
			throw new DadosInvalidosException("Descricao e obrigatoria");
		}
		if (descricao.trim().length() > 200) {
			throw new DadosInvalidosException("Descricao deve possuir no maximo 200 caracteres");
		}
		if (tipoValor == null) {
			throw new DadosInvalidosException("Tipo de valor e obrigatorio");
		}
		if (valorPadrao != null && valorPadrao.signum() <= 0) {
			throw new DadosInvalidosException("Valor padrao deve ser maior que zero");
		}
		if (diaVencimentoPadrao != null && (diaVencimentoPadrao < 1 || diaVencimentoPadrao > 31)) {
			throw new DadosInvalidosException("Dia padrao de vencimento deve estar entre 1 e 31");
		}
	}

	private Usuario buscarAutor(ContextoEmpresaAtual contexto) {
		return usuarioRepository.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarDescricao(String valor) {
		return valor.trim().replaceAll("\\s+", " ");
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
}
