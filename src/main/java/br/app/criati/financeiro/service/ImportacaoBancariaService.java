package br.app.criati.financeiro.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.ArquivoImportacaoDuplicadoException;
import br.app.criati.exception.ContaFinanceiraInativaException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.LoteImportacaoNaoEncontradoException;
import br.app.criati.exception.LoteImportacaoStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.model.LoteImportacaoBancaria;
import br.app.criati.financeiro.model.TransacaoBancariaImportada;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.InstituicaoFinanceiraRepository;
import br.app.criati.financeiro.repository.LoteImportacaoBancariaRepository;
import br.app.criati.financeiro.repository.TransacaoBancariaImportadaRepository;
import br.app.criati.shared.enums.FormatoArquivoImportacao;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.StatusLoteImportacao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class ImportacaoBancariaService {

	private final LoteImportacaoBancariaRepository loteRepository;
	private final TransacaoBancariaImportadaRepository transacaoRepository;
	private final ContaFinanceiraRepository contaRepository;
	private final InstituicaoFinanceiraRepository instituicaoRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final OfxParser ofxParser;
	private final CsvBancarioParser csvParser;
	private final XlsxBancarioParser xlsxParser;
	private final long tamanhoMaximoOfxBytes;
	private final long tamanhoMaximoCsvBytes;
	private final long tamanhoMaximoXlsxBytes;

	public ImportacaoBancariaService(
			LoteImportacaoBancariaRepository loteRepository,
			TransacaoBancariaImportadaRepository transacaoRepository,
			ContaFinanceiraRepository contaRepository,
			InstituicaoFinanceiraRepository instituicaoRepository,
			EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository,
			OfxParser ofxParser,
			CsvBancarioParser csvParser,
			XlsxBancarioParser xlsxParser,
			@Value("${criati.financeiro.importacao-ofx.tamanho-maximo-bytes:1048576}") long tamanhoMaximoOfxBytes,
			@Value("${criati.financeiro.importacao-csv.tamanho-maximo-bytes:1048576}") long tamanhoMaximoCsvBytes,
			@Value("${criati.financeiro.importacao-xlsx.tamanho-maximo-bytes:2097152}") long tamanhoMaximoXlsxBytes) {
		this.loteRepository = loteRepository;
		this.transacaoRepository = transacaoRepository;
		this.contaRepository = contaRepository;
		this.instituicaoRepository = instituicaoRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.ofxParser = ofxParser;
		this.csvParser = csvParser;
		this.xlsxParser = xlsxParser;
		if (tamanhoMaximoOfxBytes <= 0 || tamanhoMaximoCsvBytes <= 0 || tamanhoMaximoXlsxBytes <= 0) {
			throw new IllegalArgumentException("Limites de importacao bancaria devem ser positivos");
		}
		this.tamanhoMaximoOfxBytes = tamanhoMaximoOfxBytes;
		this.tamanhoMaximoCsvBytes = tamanhoMaximoCsvBytes;
		this.tamanhoMaximoXlsxBytes = tamanhoMaximoXlsxBytes;
	}

	@Transactional
	public PreviaImportacaoBancaria importar(UUID contaId, MultipartFile arquivo, ContextoEmpresaAtual contexto) {
		return importar(contaId, arquivo, contexto, FormatoArquivoImportacao.OFX, ".ofx",
				tamanhoMaximoOfxBytes, ofxParser::parse, ofxParser::identificarConta);
	}

	@Transactional
	public PreviaImportacaoBancaria importarCsv(UUID contaId, MultipartFile arquivo, ContextoEmpresaAtual contexto) {
		return importar(contaId, arquivo, contexto, FormatoArquivoImportacao.CSV, ".csv",
				tamanhoMaximoCsvBytes, csvParser::parse, bytes -> Optional.empty());
	}

	@Transactional
	public PreviaImportacaoBancaria importarXlsx(UUID contaId, MultipartFile arquivo, ContextoEmpresaAtual contexto) {
		return importar(contaId, arquivo, contexto, FormatoArquivoImportacao.XLSX, ".xlsx",
				tamanhoMaximoXlsxBytes, xlsxParser::parse, bytes -> Optional.empty());
	}

	/**
	 * contaId e sempre opcional (CRIATI-IMP-FEAT-004): sem ele, o lote e as
	 * transacoes ficam sem conta ate a revisao resolver (resolverConta) - a
	 * confirmacao financeira continua exigindo conta resolvida
	 * (ConfirmacaoImportacaoBancariaService). Quando o formato fornece
	 * identificacao bancaria (hoje so OFX) e nenhuma conta foi informada,
	 * tenta-se sugerir automaticamente uma unica conta compativel da empresa
	 * atual - nunca confirma nada sozinha, e nunca escolhe se houver 0, 2+
	 * candidatas ou dado incompleto.
	 */
	private PreviaImportacaoBancaria importar(UUID contaId, MultipartFile arquivo, ContextoEmpresaAtual contexto,
			FormatoArquivoImportacao formato, String extensao, long tamanhoMaximoBytes,
			Function<byte[], List<TransacaoBancariaExtraida>> parser,
			Function<byte[], Optional<IdentificacaoBancariaOfx>> identificador) {
		exigirEscrita(contexto);
		ContaFinanceira conta = contaId == null ? null : buscarConta(contaId, contexto.empresaId());
		String nomeOriginal = validarNomeOriginal(arquivo == null ? null : arquivo.getOriginalFilename(), formato,
				extensao);
		if (arquivo == null || arquivo.isEmpty() || arquivo.getSize() == 0) {
			throw new DadosInvalidosException("Arquivo " + formato + " esta vazio");
		}
		if (arquivo.getSize() > tamanhoMaximoBytes) {
			throw new DadosInvalidosException("Arquivo " + formato + " excede o tamanho maximo permitido");
		}
		byte[] bytes = lerBytes(arquivo, formato);
		if (bytes.length == 0) {
			throw new DadosInvalidosException("Arquivo " + formato + " esta vazio");
		}
		if (bytes.length > tamanhoMaximoBytes) {
			throw new DadosInvalidosException("Arquivo " + formato + " excede o tamanho maximo permitido");
		}

		String hashArquivo = sha256(bytes);
		if (loteRepository.existsByEmpresaIdAndHashArquivo(contexto.empresaId(), hashArquivo)) {
			throw new ArquivoImportacaoDuplicadoException();
		}
		List<TransacaoBancariaExtraida> extraidas = parser.apply(bytes);
		Empresa empresa = empresaRepository.findById(contexto.empresaId())
				.orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario autor = buscarAutor(contexto.usuarioId());

		// Metadados do extrato (BANKID/BRANCHID/ACCTID/ACCTTYPE) sao persistidos
		// sempre que o formato fornecer, tenha ou nao contaId sido informado no
		// upload (CRIATI-IMP-FIX-007) - autodetecao (contaSugerida), por outro
		// lado, so faz sentido quando ainda nao ha conta resolvida.
		IdentificacaoBancariaOfx identificacao = identificador.apply(bytes).orElse(null);
		ContaFinanceira contaSugerida = conta == null ? autodetectarConta(identificacao, contexto.empresaId()) : null;

		Set<String> chavesNoArquivo = new HashSet<>();
		List<TransacaoPreparada> preparadas = new ArrayList<>();
		int duplicadasArquivo = 0;
		int possiveisDuplicadas = 0;
		for (int indice = 0; indice < extraidas.size(); indice++) {
			TransacaoBancariaExtraida extraida = extraidas.get(indice);
			String chave = chaveDuplicidade(extraida);
			boolean duplicadaNoArquivo = !chavesNoArquivo.add(chave);
			// Sem conta resolvida nao ha como comparar contra o historico de outros
			// lotes (a checagem e por empresa+conta+chave) - a deduplicacao dentro do
			// proprio arquivo acima independe de conta e continua funcionando.
			boolean possivelmenteJaImportada = conta != null && transacaoRepository
					.existsByEmpresaIdAndContaIdAndChaveDuplicidade(
							contexto.empresaId(), conta.getId(), chave);
			if (duplicadaNoArquivo) {
				duplicadasArquivo++;
			}
			if (possivelmenteJaImportada) {
				possiveisDuplicadas++;
			}
			preparadas.add(new TransacaoPreparada(indice + 1, extraida, chave, duplicadaNoArquivo,
					possivelmenteJaImportada));
		}

		LoteImportacaoBancaria lote = new LoteImportacaoBancaria(empresa, conta, formato, hashArquivo, nomeOriginal,
				bytes.length, preparadas.size(), duplicadasArquivo, possiveisDuplicadas, autor, contaSugerida,
				identificacao == null ? null : identificacao.bankId(),
				identificacao == null ? null : identificacao.branchId(),
				identificacao == null ? null : identificacao.acctId(),
				identificacao == null ? null : identificacao.acctType());
		try {
			lote = loteRepository.saveAndFlush(lote);
		} catch (DataIntegrityViolationException excecao) {
			throw new ArquivoImportacaoDuplicadoException();
		}
		List<TransacaoBancariaImportada> transacoes = new ArrayList<>();
		for (TransacaoPreparada preparada : preparadas) {
			TransacaoBancariaExtraida item = preparada.extraida();
			transacoes.add(new TransacaoBancariaImportada(empresa, lote, conta, preparada.sequencia(), item.data(),
					item.valor(), item.tipoBancario(), item.descricao(), item.identificadorBancario(), item.documento(),
					preparada.chave(), preparada.duplicadaNoArquivo(), preparada.possivelmenteJaImportada()));
		}
		return new PreviaImportacaoBancaria(lote, transacaoRepository.saveAll(transacoes));
	}

	// Sempre escopada por empresa (contaRepository.findAll...EmpresaId...) -
	// conta de outra empresa jamais pode ser sugerida, mesmo com banco/agencia/
	// numero identicos. So sugere quando ha EXATAMENTE uma conta compativel.
	private ContaFinanceira autodetectarConta(IdentificacaoBancariaOfx identificacao, UUID empresaId) {
		if (identificacao == null || !identificacao.completaParaAutodetecao()) {
			return null;
		}
		String bancoNormalizado = normalizarCodigoBanco(identificacao.bankId());
		List<UUID> instituicaoIds = instituicaoRepository.listarDisponiveis(empresaId, StatusCadastro.ATIVO).stream()
				.filter(instituicao -> instituicao.getCodigo() != null
						&& normalizarCodigoBanco(instituicao.getCodigo()).equals(bancoNormalizado))
				.map(InstituicaoFinanceira::getId)
				.toList();
		List<ContaFinanceira> candidatas = new ArrayList<>();
		for (UUID instituicaoId : instituicaoIds) {
			candidatas.addAll(contaRepository
					.findAllByEmpresaIdAndInstituicaoIdAndAgenciaBancariaAndNumeroContaBancariaAndStatus(
							empresaId, instituicaoId, identificacao.branchId(), identificacao.acctId(),
							StatusCadastro.ATIVO));
		}
		return candidatas.size() == 1 ? candidatas.get(0) : null;
	}

	private String normalizarCodigoBanco(String codigo) {
		String semZeros = codigo.replaceFirst("^0+", "");
		return semZeros.isEmpty() ? "0" : semZeros;
	}

	/**
	 * Resolve a conta de um lote criado sem contaId (CRIATI-IMP-FEAT-004) e,
	 * na mesma operacao transacional, recalcula a duplicidade historica de
	 * TODAS as suas transacoes contra empresa+conta+chave - agora que a
	 * conta e finalmente conhecida (CRIATI-IMP-FIX-007). Antes da resolucao
	 * nenhuma transacao deste lote podia ter sido comparada contra o
	 * historico; nunca confirmar a partir de um estado de duplicidade
	 * desatualizado exige que essa checagem aconteca aqui, nao so no upload.
	 */
	@Transactional
	public PreviaImportacaoBancaria resolverConta(UUID loteId, UUID contaId, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		LoteImportacaoBancaria lote = buscarLoteParaAtualizar(loteId, contexto.empresaId());
		if (lote.getStatus() == StatusLoteImportacao.DESCARTADO) {
			throw new LoteImportacaoStatusInvalidoException("Lote descartado nao pode ser processado");
		}
		ContaFinanceira conta = buscarConta(contaId, contexto.empresaId());
		lote.resolverConta(conta);

		List<TransacaoBancariaImportada> transacoes = transacaoRepository
				.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(contexto.empresaId(), lote.getId());
		int possiveisDuplicadas = 0;
		for (TransacaoBancariaImportada transacao : transacoes) {
			transacao.resolverConta(conta);
			boolean possivelmenteJaImportada = transacaoRepository
					.existsByEmpresaIdAndContaIdAndChaveDuplicidadeAndLoteIdNot(
							contexto.empresaId(), conta.getId(), transacao.getChaveDuplicidade(), lote.getId());
			transacao.atualizarPossivelmenteJaImportada(possivelmenteJaImportada);
			if (possivelmenteJaImportada) {
				possiveisDuplicadas++;
			}
		}
		lote.atualizarQuantidadePossiveisDuplicadas(possiveisDuplicadas);

		loteRepository.save(lote);
		transacaoRepository.saveAll(transacoes);
		return new PreviaImportacaoBancaria(lote, transacoes);
	}

	@Transactional(readOnly = true)
	public List<LoteImportacaoBancaria> listar(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		return loteRepository.findAllByEmpresaIdOrderByCriadoEmDesc(contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public PreviaImportacaoBancaria buscar(UUID loteId, ContextoEmpresaAtual contexto) {
		LoteImportacaoBancaria lote = buscarLote(loteId, contexto.empresaId());
		return new PreviaImportacaoBancaria(lote,
				transacaoRepository.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(contexto.empresaId(), lote.getId()));
	}

	@Transactional
	public PreviaImportacaoBancaria descartar(UUID loteId, ContextoEmpresaAtual contexto) {
		exigirEscrita(contexto);
		LoteImportacaoBancaria lote = buscarLoteParaAtualizar(loteId, contexto.empresaId());
		if (lote.getStatus() == StatusLoteImportacao.DESCARTADO) {
			throw new LoteImportacaoStatusInvalidoException("Lote de importacao ja esta descartado");
		}
		lote.descartar(buscarAutor(contexto.usuarioId()));
		loteRepository.save(lote);
		return new PreviaImportacaoBancaria(lote,
				transacaoRepository.findAllByEmpresaIdAndLoteIdOrderBySequenciaAsc(contexto.empresaId(), lote.getId()));
	}

	private ContaFinanceira buscarConta(UUID contaId, UUID empresaId) {
		if (contaId == null) {
			throw new ContaFinanceiraNaoEncontradaException();
		}
		ContaFinanceira conta = contaRepository.findByIdAndEmpresaId(contaId, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
		if (!conta.estaAtiva()) {
			throw new ContaFinanceiraInativaException();
		}
		return conta;
	}

	private LoteImportacaoBancaria buscarLote(UUID loteId, UUID empresaId) {
		if (loteId == null) {
			throw new LoteImportacaoNaoEncontradoException();
		}
		return loteRepository.findByIdAndEmpresaId(loteId, empresaId)
				.orElseThrow(LoteImportacaoNaoEncontradoException::new);
	}

	private LoteImportacaoBancaria buscarLoteParaAtualizar(UUID loteId, UUID empresaId) {
		if (loteId == null) {
			throw new LoteImportacaoNaoEncontradoException();
		}
		return loteRepository.findForUpdateByIdAndEmpresaId(loteId, empresaId)
				.orElseThrow(LoteImportacaoNaoEncontradoException::new);
	}

	private String validarNomeOriginal(String nome, FormatoArquivoImportacao formato, String extensao) {
		if (nome == null || nome.isBlank()) {
			throw new DadosInvalidosException("Nome do arquivo " + formato + " e obrigatorio");
		}
		String normalizado = nome.trim();
		if (normalizado.length() > 255 || normalizado.contains("/") || normalizado.contains("\\")
				|| normalizado.contains("..") || normalizado.chars().anyMatch(Character::isISOControl)
				|| !normalizado.toLowerCase(Locale.ROOT).endsWith(extensao)) {
			throw new DadosInvalidosException("Arquivo deve possuir nome seguro e extensao " + extensao);
		}
		return normalizado;
	}

	private byte[] lerBytes(MultipartFile arquivo, FormatoArquivoImportacao formato) {
		try {
			return arquivo.getBytes();
		} catch (IOException excecao) {
			throw new DadosInvalidosException("Nao foi possivel ler o arquivo " + formato);
		}
	}

	private String chaveDuplicidade(TransacaoBancariaExtraida transacao) {
		String identificador = normalizarChave(transacao.identificadorBancario());
		String material = identificador != null
				? "FITID|" + identificador
				: String.join("|", transacao.data().toString(), valorCanonico(transacao.valor()),
						normalizarChave(transacao.tipoBancario()), valorOuVazio(normalizarChave(transacao.descricao())),
						valorOuVazio(normalizarChave(transacao.documento())));
		return sha256(material.getBytes(StandardCharsets.UTF_8));
	}

	private String normalizarChave(String valor) {
		if (valor == null || valor.isBlank()) {
			return null;
		}
		return Normalizer.normalize(valor, Normalizer.Form.NFKC).trim().replaceAll("\\s+", " ")
				.toUpperCase(Locale.ROOT);
	}

	private String valorCanonico(BigDecimal valor) {
		return valor.setScale(2).toPlainString();
	}

	private String valorOuVazio(String valor) {
		return valor == null ? "" : valor;
	}

	private String sha256(byte[] bytes) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException excecao) {
			throw new IllegalStateException("Algoritmo SHA-256 indisponivel", excecao);
		}
	}

	private Usuario buscarAutor(UUID usuarioId) {
		return usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private void exigirEscrita(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR && contexto.perfil() != PerfilUsuario.GESTOR) {
			throw new AcessoNegadoException();
		}
	}

	private record TransacaoPreparada(int sequencia, TransacaoBancariaExtraida extraida, String chave,
			boolean duplicadaNoArquivo, boolean possivelmenteJaImportada) {
	}
}
