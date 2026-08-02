package br.app.criati.financeiro.service;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CategoriaFinanceiraInativaException;
import br.app.criati.exception.CategoriaFinanceiraNaoEncontradaException;
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.FinanceiroStatusInvalidoException;
import br.app.criati.exception.PessoaFinanceiraInativaException;
import br.app.criati.exception.PessoaFinanceiraNaoEncontradaException;
import br.app.criati.exception.RegraClassificacaoImportacaoNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.RegraClassificacaoImportacao;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.RegraClassificacaoImportacaoRepository;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.AplicacaoRegraClassificacaoImportacao;
import br.app.criati.shared.enums.EncaminhamentoSugeridoRegraImportacao;
import br.app.criati.shared.enums.EstrategiaComparacaoRegraImportacao;
import br.app.criati.shared.enums.FormaPagamentoLancamento;
import br.app.criati.shared.enums.NivelConfiancaRegraImportacao;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

/**
 * CRUD e aplicacao de regras de classificacao de importacao bancaria
 * (CRIATI-IMP-002A). Nunca gera lancamento, pagamento de fatura ou
 * compromisso sozinha - so reconhece/sugere; quem decide e sempre
 * ConfirmacaoImportacaoBancariaService, mediante confirmacao explicita do
 * usuario.
 */
@Service
public class RegraClassificacaoImportacaoService {

	// Ajuste obrigatorio 3 (CRIATI-IMP-002A): padroes de uma palavra so,
	// reconhecidamente ambiguos, nunca podem ser usados com aplicacao
	// AUTOMATICA - esta e a protecao PRINCIPAL (semantica); o CHECK de
	// tamanho minimo no banco e so complementar (ver migration V24).
	private static final Set<String> PADROES_GENERICOS_PROIBIDOS_EM_AUTOMATICA = Set.of(
			"PAGAMENTO", "PIX", "TRANSFERENCIA", "COMPRA", "CREDITO", "DEBITO",
			"DEPOSITO", "SAQUE", "TARIFA", "BOLETO", "RECEBIMENTO", "ENVIO");

	private final RegraClassificacaoImportacaoRepository regras;
	private final ContaFinanceiraRepository contas;
	private final CategoriaFinanceiraRepository categorias;
	private final PessoaFinanceiraRepository pessoas;
	private final EmpresaRepository empresas;
	private final UsuarioRepository usuarios;

	public RegraClassificacaoImportacaoService(RegraClassificacaoImportacaoRepository regras,
			ContaFinanceiraRepository contas, CategoriaFinanceiraRepository categorias,
			PessoaFinanceiraRepository pessoas, EmpresaRepository empresas, UsuarioRepository usuarios) {
		this.regras = regras;
		this.contas = contas;
		this.categorias = categorias;
		this.pessoas = pessoas;
		this.empresas = empresas;
		this.usuarios = usuarios;
	}

	@Transactional(readOnly = true)
	public List<RegraClassificacaoImportacao> listar(ContextoEmpresaAtual contexto, StatusCadastro status) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		return status == null
				? regras.findAllByEmpresaIdOrderByCriadoEmDesc(contexto.empresaId())
				: regras.findAllByEmpresaIdAndStatusOrderByCriadoEmDesc(contexto.empresaId(), status);
	}

	@Transactional(readOnly = true)
	public RegraClassificacaoImportacao buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional
	public RegraClassificacaoImportacao criar(DadosRegraClassificacaoImportacao dados, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		Empresa empresa = empresas.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario autor = buscarAutor(contexto.usuarioId());
		DadosValidados validados = validarDados(dados, contexto.empresaId(), null);
		verificarDuplicidade(contexto.empresaId(), validados.contaId(), validados.padraoNormalizado(),
				validados.tipo(), null);
		RegraClassificacaoImportacao regra = new RegraClassificacaoImportacao(empresa, validados.conta(),
				validados.descricaoReferencia(), validados.padraoNormalizado(), validados.estrategiaComparacao(),
				validados.prioridade(), validados.tipo(), validados.categoria(), validados.pessoa(),
				validados.formaPagamento(), validados.encaminhamentoSugerido(), validados.nivelConfianca(),
				validados.aplicacao(), autor);
		try {
			return regras.saveAndFlush(regra);
		} catch (DataIntegrityViolationException excecao) {
			throw new DadosInvalidosException("Ja existe uma regra ativa com este padrao para esta conta/tipo");
		}
	}

	@Transactional
	public RegraClassificacaoImportacao editar(
			UUID id, DadosRegraClassificacaoImportacao dados, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		RegraClassificacaoImportacao regra = buscarDaEmpresa(id, contexto.empresaId());
		Usuario autor = buscarAutor(contexto.usuarioId());
		DadosValidados validados = validarDados(dados, contexto.empresaId(), id);
		verificarDuplicidade(contexto.empresaId(), validados.contaId(), validados.padraoNormalizado(),
				validados.tipo(), id);
		regra.atualizarDados(validados.conta(), validados.descricaoReferencia(), validados.padraoNormalizado(),
				validados.estrategiaComparacao(), validados.prioridade(), validados.tipo(), validados.categoria(),
				validados.pessoa(), validados.formaPagamento(), validados.encaminhamentoSugerido(),
				validados.nivelConfianca(), validados.aplicacao(), autor);
		try {
			return regras.saveAndFlush(regra);
		} catch (DataIntegrityViolationException excecao) {
			throw new DadosInvalidosException("Ja existe uma regra ativa com este padrao para esta conta/tipo");
		}
	}

	@Transactional
	public RegraClassificacaoImportacao inativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		RegraClassificacaoImportacao regra = buscarDaEmpresa(id, contexto.empresaId());
		if (!regra.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Regra ja esta inativa");
		}
		// Exclusao logica apenas (ajuste obrigatorio 6): a regra permanece na
		// base para que o historico (regra_classificacao_id em transacoes ja
		// confirmadas) continue integro e consultavel; so deixa de ser
		// candidata a novas sugestoes (RegraClassificacaoImportacaoRepository.
		// findCandidatasParaClassificacao filtra status ATIVO).
		regra.desativar(buscarAutor(contexto.usuarioId()));
		return regras.save(regra);
	}

	@Transactional
	public RegraClassificacaoImportacao reativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		RegraClassificacaoImportacao regra = buscarDaEmpresa(id, contexto.empresaId());
		if (regra.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Regra ja esta ativa");
		}
		regra.reativar(buscarAutor(contexto.usuarioId()));
		return regras.save(regra);
	}

	/**
	 * Melhor regra candidata para uma transacao ainda pendente, ou vazio se
	 * nenhuma casar. Somente leitura/sugestao - nunca persiste nada aqui;
	 * quem decide usar (e incrementa o contador) e
	 * ConfirmacaoImportacaoBancariaService, e somente apos confirmacao
	 * explicita do usuario (ajuste obrigatorio 7).
	 */
	@Transactional(readOnly = true)
	public Optional<RegraClassificacaoImportacao> sugerirParaTransacao(
			String descricaoTransacao, UUID contaId, TipoFinanceiro tipo, ContextoEmpresaAtual contexto) {
		if (descricaoTransacao == null || descricaoTransacao.isBlank() || tipo == null) {
			return Optional.empty();
		}
		String descricaoNormalizada = normalizar(descricaoTransacao);
		List<RegraClassificacaoImportacao> candidatas = regras.findCandidatasParaClassificacao(
				contexto.empresaId(), contaId, tipo, StatusCadastro.ATIVO);
		return candidatas.stream().filter(regra -> casa(regra, descricaoNormalizada)).findFirst();
	}

	/**
	 * Efetiva o uso da regra (contador + ultima utilizacao), com lock
	 * pessimista para nao perder incremento em confirmacoes concorrentes
	 * (ajuste obrigatorio 8) - chamado exclusivamente por
	 * ConfirmacaoImportacaoBancariaService, dentro da mesma transacao da
	 * confirmacao, e somente quando a classificacao foi efetivamente aceita
	 * (nunca so sugerida).
	 */
	@Transactional
	public void registrarUso(UUID regraId, ContextoEmpresaAtual contexto) {
		RegraClassificacaoImportacao regra = regras.findForUpdateByIdAndEmpresaId(regraId, contexto.empresaId())
				.orElseThrow(RegraClassificacaoImportacaoNaoEncontradaException::new);
		regra.registrarUso(buscarAutor(contexto.usuarioId()));
		regras.save(regra);
	}

	private boolean casa(RegraClassificacaoImportacao regra, String descricaoNormalizada) {
		return switch (regra.getEstrategiaComparacao()) {
			case IGUAL -> descricaoNormalizada.equals(regra.getPadraoNormalizado());
			case PREFIXO -> descricaoNormalizada.startsWith(regra.getPadraoNormalizado());
			case CONTEM -> descricaoNormalizada.contains(regra.getPadraoNormalizado());
		};
	}

	private DadosValidados validarDados(DadosRegraClassificacaoImportacao dados, UUID empresaId, UUID idAtual) {
		if (dados.tipo() == null) {
			throw new DadosInvalidosException("Natureza (Receita/Despesa) e obrigatoria");
		}
		if (dados.estrategiaComparacao() == null) {
			throw new DadosInvalidosException("Estrategia de comparacao e obrigatoria");
		}
		if (dados.nivelConfianca() == null) {
			throw new DadosInvalidosException("Nivel de confianca e obrigatorio");
		}
		if (dados.aplicacao() == null) {
			throw new DadosInvalidosException("Aplicacao (Automatica/Sugestao) e obrigatoria");
		}
		String descricaoReferencia = normalizarObrigatorio(dados.descricaoReferencia(), "Descricao de referencia", 500);
		String padraoNormalizado = normalizar(exigirPadraoNaoVazio(dados.descricaoReferencia(), dados.padrao()));
		if (padraoNormalizado.length() < 2) {
			throw new DadosInvalidosException("Padrao normalizado deve possuir ao menos 2 caracteres");
		}
		if (dados.prioridade() < 0) {
			throw new DadosInvalidosException("Prioridade deve ser maior ou igual a zero");
		}

		// Ajuste obrigatorio 2: CONTEM nunca pode ser AUTOMATICA - decisao de
		// negocio, nao apenas questao de tamanho. So IGUAL/PREFIXO podem.
		if (dados.aplicacao() == AplicacaoRegraClassificacaoImportacao.AUTOMATICA
				&& dados.estrategiaComparacao() == EstrategiaComparacaoRegraImportacao.CONTEM) {
			throw new DadosInvalidosException(
					"Estrategia CONTEM nao pode ser usada com aplicacao AUTOMATICA; use SUGESTAO ou troque para IGUAL/PREFIXO");
		}
		// Ajuste obrigatorio 3: validacao semantica de padrao generico -
		// protecao PRINCIPAL contra automacao de termos ambiguos. O CHECK de
		// tamanho minimo no banco (migration V24) e so complementar.
		if (dados.aplicacao() == AplicacaoRegraClassificacaoImportacao.AUTOMATICA
				&& PADROES_GENERICOS_PROIBIDOS_EM_AUTOMATICA.contains(padraoNormalizado)) {
			throw new DadosInvalidosException(
					"Padrao \"" + dados.padrao() + "\" e generico demais para aplicacao AUTOMATICA; use SUGESTAO ou "
							+ "torne o padrao mais especifico");
		}

		ContaFinanceira conta = buscarContaOpcional(dados.contaId(), empresaId);
		CategoriaFinanceira categoria = buscarCategoria(dados.categoriaId(), empresaId);
		if (categoria.getTipo() != dados.tipo()) {
			throw new DadosInvalidosException("Categoria deve possuir a mesma natureza (Receita/Despesa) da regra");
		}
		PessoaFinanceira pessoa = buscarPessoaOpcional(dados.pessoaFinanceiraId(), empresaId);

		return new DadosValidados(conta, dados.contaId(), descricaoReferencia, padraoNormalizado,
				dados.estrategiaComparacao(), dados.prioridade(), dados.tipo(), categoria, pessoa,
				dados.formaPagamento(), dados.encaminhamentoSugerido(), dados.nivelConfianca(), dados.aplicacao());
	}

	private String exigirPadraoNaoVazio(String descricaoReferencia, String padrao) {
		String base = (padrao == null || padrao.isBlank()) ? descricaoReferencia : padrao;
		if (base == null || base.isBlank()) {
			throw new DadosInvalidosException("Padrao (ou descricao de referencia) e obrigatorio");
		}
		return base;
	}

	private void verificarDuplicidade(
			UUID empresaId, UUID contaId, String padraoNormalizado, TipoFinanceiro tipo, UUID idAtual) {
		boolean duplicada = contaId != null
				? (idAtual == null
						? regras.existsByEmpresaIdAndContaIdAndPadraoNormalizadoAndTipo(empresaId, contaId, padraoNormalizado, tipo)
						: regras.existsByEmpresaIdAndContaIdAndPadraoNormalizadoAndTipoAndIdNot(
								empresaId, contaId, padraoNormalizado, tipo, idAtual))
				: (idAtual == null
						? regras.existsByEmpresaIdAndContaIdIsNullAndPadraoNormalizadoAndTipo(empresaId, padraoNormalizado, tipo)
						: regras.existsByEmpresaIdAndContaIdIsNullAndPadraoNormalizadoAndTipoAndIdNot(
								empresaId, padraoNormalizado, tipo, idAtual));
		if (duplicada) {
			throw new DadosInvalidosException("Ja existe uma regra ativa com este padrao para esta conta/tipo");
		}
	}

	private ContaFinanceira buscarContaOpcional(UUID contaId, UUID empresaId) {
		if (contaId == null) {
			return null;
		}
		ContaFinanceira conta = contas.findByIdAndEmpresaId(contaId, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva()) {
			throw new ContaFinanceiraInativaException();
		}
		return conta;
	}

	private CategoriaFinanceira buscarCategoria(UUID categoriaId, UUID empresaId) {
		if (categoriaId == null) {
			throw new CategoriaFinanceiraNaoEncontradaException();
		}
		CategoriaFinanceira categoria = categorias.findByIdAndEmpresaId(categoriaId, empresaId)
				.orElseThrow(CategoriaFinanceiraNaoEncontradaException::new);
		if (!categoria.estaAtiva()) {
			throw new CategoriaFinanceiraInativaException();
		}
		return categoria;
	}

	private PessoaFinanceira buscarPessoaOpcional(UUID pessoaId, UUID empresaId) {
		if (pessoaId == null) {
			return null;
		}
		PessoaFinanceira pessoa = pessoas.findByIdAndEmpresaId(pessoaId, empresaId)
				.orElseThrow(PessoaFinanceiraNaoEncontradaException::new);
		if (!pessoa.estaAtiva()) {
			throw new PessoaFinanceiraInativaException();
		}
		return pessoa;
	}

	private RegraClassificacaoImportacao buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new RegraClassificacaoImportacaoNaoEncontradaException();
		}
		return regras.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(RegraClassificacaoImportacaoNaoEncontradaException::new);
	}

	private Usuario buscarAutor(UUID usuarioId) {
		return usuarios.findById(usuarioId).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarObrigatorio(String valor, String campo, int tamanhoMaximo) {
		if (valor == null || valor.isBlank()) {
			throw new DadosInvalidosException(campo + " e obrigatoria");
		}
		String normalizado = valor.trim();
		if (normalizado.length() > tamanhoMaximo) {
			throw new DadosInvalidosException(campo + " deve possuir no maximo " + tamanhoMaximo + " caracteres");
		}
		return normalizado;
	}

	// Mesma normalizacao usada por ImportacaoBancariaService.chaveDuplicidade
	// (NFKC + trim + colapso de espacos + maiusculas) - garante que o padrao
	// da regra e a descricao da transacao sejam comparados de forma
	// consistente com o restante do modulo de importacao.
	private String normalizar(String valor) {
		if (valor == null) {
			return "";
		}
		return Normalizer.normalize(valor, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ")
				.toUpperCase(Locale.ROOT);
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}

	private record DadosValidados(
			ContaFinanceira conta,
			UUID contaId,
			String descricaoReferencia,
			String padraoNormalizado,
			EstrategiaComparacaoRegraImportacao estrategiaComparacao,
			int prioridade,
			TipoFinanceiro tipo,
			CategoriaFinanceira categoria,
			PessoaFinanceira pessoa,
			FormaPagamentoLancamento formaPagamento,
			EncaminhamentoSugeridoRegraImportacao encaminhamentoSugerido,
			NivelConfiancaRegraImportacao nivelConfianca,
			AplicacaoRegraClassificacaoImportacao aplicacao) {
	}
}
