package br.app.criati.financeiro.shared.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.FinanceiroStatusInvalidoException;
import br.app.criati.exception.ParteFinanceiraNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class ParteFinanceiraService {

	private final ParteFinanceiraRepository parteRepository;
	private final UsuarioRepository usuarioRepository;
	private final EmpresaRepository empresaRepository;

	public ParteFinanceiraService(
			ParteFinanceiraRepository parteRepository,
			UsuarioRepository usuarioRepository,
			EmpresaRepository empresaRepository) {
		this.parteRepository = parteRepository;
		this.usuarioRepository = usuarioRepository;
		this.empresaRepository = empresaRepository;
	}

	@Transactional(readOnly = true)
	public List<ParteFinanceira> listar(
			ContextoEmpresaAtual contexto, StatusCadastro status, TipoParteFinanceira tipo, String busca) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		StatusCadastro statusEfetivo = status == null ? StatusCadastro.ATIVO : status;
		String buscaNormalizada = busca == null ? null : busca.trim();
		List<ParteFinanceira> partes = buscaNormalizada == null || buscaNormalizada.isBlank()
				? parteRepository.findAllByEmpresaIdAndStatusOrderByNomeAsc(contexto.empresaId(), statusEfetivo)
				: parteRepository.findAllByEmpresaIdAndStatusAndNomeContainingIgnoreCaseOrderByNomeAsc(
						contexto.empresaId(), statusEfetivo, buscaNormalizada);
		return partes.stream().filter(parte -> tipo == null || parte.getTipo() == tipo).toList();
	}

	@Transactional(readOnly = true)
	public ParteFinanceira buscar(UUID id, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(id, contexto.empresaId());
	}

	@Transactional
	public ParteFinanceira criar(
			String nome,
			TipoParteFinanceira tipo,
			String documento,
			String apelido,
			String observacao,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		DadosParte dados = validar(nome, tipo, documento, apelido, observacao);
		Empresa empresa = empresaRepository.findById(contexto.empresaId())
				.orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario autor = buscarAutor(contexto.usuarioId());
		return parteRepository.save(new ParteFinanceira(
				empresa, dados.nome(), tipo, dados.documento(), dados.apelido(), dados.observacao(), autor));
	}

	@Transactional
	public ParteFinanceira atualizar(
			UUID id,
			String nome,
			TipoParteFinanceira tipo,
			String documento,
			String apelido,
			String observacao,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		DadosParte dados = validar(nome, tipo, documento, apelido, observacao);
		ParteFinanceira parte = buscarDaEmpresa(id, contexto.empresaId());
		parte.atualizar(
				dados.nome(), tipo, dados.documento(), dados.apelido(), dados.observacao(), buscarAutor(contexto.usuarioId()));
		return parteRepository.save(parte);
	}

	@Transactional
	public ParteFinanceira desativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		ParteFinanceira parte = buscarDaEmpresa(id, contexto.empresaId());
		if (!parte.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Contato financeiro ja esta inativo");
		}
		parte.desativar(buscarAutor(contexto.usuarioId()));
		return parteRepository.save(parte);
	}

	@Transactional
	public ParteFinanceira reativar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		ParteFinanceira parte = buscarDaEmpresa(id, contexto.empresaId());
		if (parte.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Contato financeiro ja esta ativo");
		}
		parte.reativar(buscarAutor(contexto.usuarioId()));
		return parteRepository.save(parte);
	}

	private DadosParte validar(
			String nome, TipoParteFinanceira tipo, String documento, String apelido, String observacao) {
		if (tipo == null) {
			throw new DadosInvalidosException("Tipo e obrigatorio");
		}
		return new DadosParte(
				normalizarObrigatorio(nome, "Nome", 150),
				normalizarDocumento(documento),
				normalizarOpcional(apelido, "Apelido", 100),
				normalizarOpcional(observacao, "Observacao", 500));
	}

	private String normalizarDocumento(String documento) {
		if (documento == null || documento.isBlank()) {
			return null;
		}
		String normalizado = documento.replaceAll("[^\\p{L}\\p{N}]", "").toUpperCase(Locale.ROOT);
		if (normalizado.isBlank()) {
			return null;
		}
		if (normalizado.length() > 30) {
			throw new DadosInvalidosException("Documento deve possuir no maximo 30 caracteres");
		}
		return normalizado;
	}

	private ParteFinanceira buscarDaEmpresa(UUID id, UUID empresaId) {
		if (id == null) {
			throw new ParteFinanceiraNaoEncontradaException();
		}
		return parteRepository.findByIdAndEmpresaId(id, empresaId)
				.orElseThrow(ParteFinanceiraNaoEncontradaException::new);
	}

	private Usuario buscarAutor(UUID usuarioId) {
		return usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private String normalizarObrigatorio(String valor, String campo, int tamanhoMaximo) {
		if (valor == null || valor.isBlank()) {
			throw new DadosInvalidosException(campo + " e obrigatorio");
		}
		String normalizado = valor.trim();
		if (normalizado.length() > tamanhoMaximo) {
			throw new DadosInvalidosException(campo + " deve possuir no maximo " + tamanhoMaximo + " caracteres");
		}
		return normalizado;
	}

	private String normalizarOpcional(String valor, String campo, int tamanhoMaximo) {
		return valor == null || valor.isBlank() ? null : normalizarObrigatorio(valor, campo, tamanhoMaximo);
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}

	private record DadosParte(String nome, String documento, String apelido, String observacao) {
	}
}
