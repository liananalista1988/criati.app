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
import br.app.criati.financeiro.model.LoteImportacaoBancaria;
import br.app.criati.financeiro.model.TransacaoBancariaImportada;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LoteImportacaoBancariaRepository;
import br.app.criati.financeiro.repository.TransacaoBancariaImportadaRepository;
import br.app.criati.shared.enums.FormatoArquivoImportacao;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusLoteImportacao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class ImportacaoBancariaService {

	private final LoteImportacaoBancariaRepository loteRepository;
	private final TransacaoBancariaImportadaRepository transacaoRepository;
	private final ContaFinanceiraRepository contaRepository;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;
	private final OfxParser ofxParser;
	private final CsvBancarioParser csvParser;
	private final long tamanhoMaximoOfxBytes;
	private final long tamanhoMaximoCsvBytes;

	public ImportacaoBancariaService(
			LoteImportacaoBancariaRepository loteRepository,
			TransacaoBancariaImportadaRepository transacaoRepository,
			ContaFinanceiraRepository contaRepository,
			EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository,
			OfxParser ofxParser,
			CsvBancarioParser csvParser,
			@Value("${criati.financeiro.importacao-ofx.tamanho-maximo-bytes:1048576}") long tamanhoMaximoOfxBytes,
			@Value("${criati.financeiro.importacao-csv.tamanho-maximo-bytes:1048576}") long tamanhoMaximoCsvBytes) {
		this.loteRepository = loteRepository;
		this.transacaoRepository = transacaoRepository;
		this.contaRepository = contaRepository;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
		this.ofxParser = ofxParser;
		this.csvParser = csvParser;
		if (tamanhoMaximoOfxBytes <= 0 || tamanhoMaximoCsvBytes <= 0) {
			throw new IllegalArgumentException("Limites de importacao bancaria devem ser positivos");
		}
		this.tamanhoMaximoOfxBytes = tamanhoMaximoOfxBytes;
		this.tamanhoMaximoCsvBytes = tamanhoMaximoCsvBytes;
	}

	@Transactional
	public PreviaImportacaoBancaria importar(UUID contaId, MultipartFile arquivo, ContextoEmpresaAtual contexto) {
		return importar(contaId, arquivo, contexto, FormatoArquivoImportacao.OFX, ".ofx",
				tamanhoMaximoOfxBytes, ofxParser::parse);
	}

	@Transactional
	public PreviaImportacaoBancaria importarCsv(UUID contaId, MultipartFile arquivo, ContextoEmpresaAtual contexto) {
		return importar(contaId, arquivo, contexto, FormatoArquivoImportacao.CSV, ".csv",
				tamanhoMaximoCsvBytes, csvParser::parse);
	}

	private PreviaImportacaoBancaria importar(UUID contaId, MultipartFile arquivo, ContextoEmpresaAtual contexto,
			FormatoArquivoImportacao formato, String extensao, long tamanhoMaximoBytes,
			Function<byte[], List<TransacaoBancariaExtraida>> parser) {
		exigirEscrita(contexto);
		ContaFinanceira conta = buscarConta(contaId, contexto.empresaId());
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

		Set<String> chavesNoArquivo = new HashSet<>();
		List<TransacaoPreparada> preparadas = new ArrayList<>();
		int duplicadasArquivo = 0;
		int possiveisDuplicadas = 0;
		for (int indice = 0; indice < extraidas.size(); indice++) {
			TransacaoBancariaExtraida extraida = extraidas.get(indice);
			String chave = chaveDuplicidade(extraida);
			boolean duplicadaNoArquivo = !chavesNoArquivo.add(chave);
			boolean possivelmenteJaImportada = transacaoRepository
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
				bytes.length, preparadas.size(), duplicadasArquivo, possiveisDuplicadas, autor);
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
