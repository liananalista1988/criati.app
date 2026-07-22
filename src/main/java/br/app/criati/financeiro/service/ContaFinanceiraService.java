package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.ContaFinanceiraComLancamentosException;
import br.app.criati.exception.ContaFinanceiraNaoEncontradaException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.FinanceiroStatusInvalidoException;
import br.app.criati.exception.PessoaFinanceiraInativaException;
import br.app.criati.exception.PessoaFinanceiraNaoEncontradaException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.model.InstituicaoFinanceira;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.repository.LancamentoFinanceiroRepository;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class ContaFinanceiraService {

	private final ContaFinanceiraRepository contaRepository;
	private final LancamentoFinanceiroRepository lancamentoRepository;
	private final PessoaFinanceiraRepository pessoaRepository;
	private final InstituicaoFinanceiraService instituicaoService;
	private final EmpresaRepository empresaRepository;
	private final UsuarioRepository usuarioRepository;

	public ContaFinanceiraService(
			ContaFinanceiraRepository contaRepository,
			LancamentoFinanceiroRepository lancamentoRepository,
			PessoaFinanceiraRepository pessoaRepository,
			InstituicaoFinanceiraService instituicaoService,
			EmpresaRepository empresaRepository,
			UsuarioRepository usuarioRepository) {
		this.contaRepository = contaRepository;
		this.lancamentoRepository = lancamentoRepository;
		this.pessoaRepository = pessoaRepository;
		this.instituicaoService = instituicaoService;
		this.empresaRepository = empresaRepository;
		this.usuarioRepository = usuarioRepository;
	}

	@Transactional(readOnly = true)
	public List<ContaFinanceira> listar(
			ContextoEmpresaAtual contexto,
			StatusCadastro status,
			TipoContaFinanceira tipo,
			UUID titularId,
			UUID instituicaoId,
			String busca) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		StatusCadastro statusEfetivo = status == null ? StatusCadastro.ATIVO : status;
		String buscaNormalizada = busca == null ? "" : busca.trim();
		return contaRepository.findAllByEmpresaIdAndStatusOrderByNomeAsc(contexto.empresaId(), statusEfetivo).stream()
				.filter(conta -> tipo == null || conta.getTipo() == tipo)
				.filter(conta -> titularId == null || conta.getTitular() != null && titularId.equals(conta.getTitular().getId()))
				.filter(conta -> instituicaoId == null
						|| conta.getInstituicao() != null && instituicaoId.equals(conta.getInstituicao().getId()))
				.filter(conta -> buscaNormalizada.isBlank()
						|| conta.getNome().toLowerCase().contains(buscaNormalizada.toLowerCase()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ContaFinanceira> listar(
			ContextoEmpresaAtual contexto, StatusCadastro status, TipoContaFinanceira tipo) {
		return listar(contexto, status, tipo, null, null, null);
	}

	@Transactional(readOnly = true)
	public ContaFinanceira buscar(UUID contaId, ContextoEmpresaAtual contexto) {
		return buscarDaEmpresa(contaId, contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public boolean possuiPossivelDuplicidade(ContaFinanceira conta) {
		return contaRepository.existsByEmpresaIdAndNomeIgnoreCaseAndStatusAndIdNot(
				conta.getEmpresa().getId(), conta.getNome(), StatusCadastro.ATIVO, conta.getId());
	}

	@Transactional(readOnly = true)
	public ResumoContasFinanceiras resumir(ContextoEmpresaAtual contexto) {
		List<ContaFinanceira> contas = contaRepository.findAllByEmpresaIdAndStatusOrderByNomeAsc(
				contexto.empresaId(), StatusCadastro.ATIVO);
		BigDecimal total = contas.stream().map(ContaFinanceira::getSaldoInicial)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		var valores = new LinkedHashMap<UUID, BigDecimal>();
		var nomes = new LinkedHashMap<UUID, String>();
		for (ContaFinanceira conta : contas) {
			if (conta.getTitular() != null) {
				UUID titularId = conta.getTitular().getId();
				valores.merge(titularId, conta.getSaldoInicial(), BigDecimal::add);
				nomes.put(titularId, conta.getTitular().getNome());
			}
		}
		List<SaldoInicialPorTitular> porTitular = valores.entrySet().stream()
				.map(item -> new SaldoInicialPorTitular(item.getKey(), nomes.get(item.getKey()), item.getValue()))
				.toList();
		return new ResumoContasFinanceiras(contas.size(), total, porTitular);
	}

	@Transactional
	public ContaFinanceira criar(
			String nome,
			UUID titularId,
			UUID instituicaoId,
			TipoContaFinanceira tipo,
			String moeda,
			BigDecimal saldoInicial,
			LocalDate dataSaldoInicial,
			boolean permiteConciliacao,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		DadosConta dados = validarDados(nome, titularId, instituicaoId, tipo, moeda, saldoInicial,
				dataSaldoInicial, permiteConciliacao, contexto, null);
		Empresa empresa = empresaRepository.findById(contexto.empresaId())
				.orElseThrow(EmpresaNaoEncontradaException::new);
		Usuario autor = buscarAutor(contexto.usuarioId());
		return contaRepository.save(new ContaFinanceira(
				empresa, dados.titular(), dados.instituicao(), dados.nome(), tipo,
				dados.saldoInicial(), dataSaldoInicial, permiteConciliacao, autor));
	}

	@Transactional
	public ContaFinanceira editar(
			UUID contaId,
			String nome,
			UUID titularId,
			UUID instituicaoId,
			TipoContaFinanceira tipo,
			String moeda,
			BigDecimal saldoInicial,
			LocalDate dataSaldoInicial,
			boolean permiteConciliacao,
			ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		ContaFinanceira conta = buscarDaEmpresa(contaId, contexto.empresaId());
		DadosConta dados = validarDados(nome, titularId, instituicaoId, tipo, moeda, saldoInicial,
				dataSaldoInicial, permiteConciliacao, contexto, conta);
		if (lancamentoRepository.existsByContaIdAndEmpresaId(conta.getId(), contexto.empresaId())
				&& dadosFinanceirosAlterados(conta, dados, tipo, dataSaldoInicial)) {
			throw new ContaFinanceiraComLancamentosException();
		}
		conta.atualizarDados(dados.titular(), dados.instituicao(), dados.nome(), tipo,
				dados.saldoInicial(), dataSaldoInicial, permiteConciliacao, buscarAutor(contexto.usuarioId()));
		return contaRepository.save(conta);
	}

	// Contratos legados mantidos para integrações anteriores. Novas operações devem informar titular e data.
	@Transactional
	public ContaFinanceira criar(
			String nome, TipoContaFinanceira tipo, BigDecimal saldoInicial, ContextoEmpresaAtual contexto) {
		throw new DadosInvalidosException("Titular e data do saldo inicial sao obrigatorios");
	}

	@Transactional
	public ContaFinanceira editar(
			UUID contaId, String nome, TipoContaFinanceira tipo, BigDecimal saldoInicial, ContextoEmpresaAtual contexto) {
		ContaFinanceira conta = buscarDaEmpresa(contaId, contexto.empresaId());
		if (conta.getTitular() == null) {
			throw new DadosInvalidosException("Informe o titular da conta");
		}
		return editar(contaId, nome, conta.getTitular().getId(),
				conta.getInstituicao() == null ? null : conta.getInstituicao().getId(), tipo, "BRL", saldoInicial,
				conta.getDataSaldoInicial(), conta.isPermiteConciliacao(), contexto);
	}

	@Transactional
	public ContaFinanceira inativar(UUID contaId, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		ContaFinanceira conta = buscarDaEmpresa(contaId, contexto.empresaId());
		if (!conta.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Conta financeira ja esta inativa");
		}
		conta.inativar(buscarAutor(contexto.usuarioId()));
		return contaRepository.save(conta);
	}

	@Transactional
	public ContaFinanceira reativar(UUID contaId, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		ContaFinanceira conta = buscarDaEmpresa(contaId, contexto.empresaId());
		if (conta.estaAtiva()) {
			throw new FinanceiroStatusInvalidoException("Conta financeira ja esta ativa");
		}
		if (conta.getTitular() != null && !conta.getTitular().estaAtiva()) {
			throw new PessoaFinanceiraInativaException();
		}
		conta.reativar(buscarAutor(contexto.usuarioId()));
		return contaRepository.save(conta);
	}

	private DadosConta validarDados(
			String nome,
			UUID titularId,
			UUID instituicaoId,
			TipoContaFinanceira tipo,
			String moeda,
			BigDecimal saldoInicial,
			LocalDate dataSaldoInicial,
			boolean permiteConciliacao,
			ContextoEmpresaAtual contexto,
			ContaFinanceira contaAtual) {
		String nomeNormalizado = normalizarNome(nome);
		if (tipo == null) {
			throw new DadosInvalidosException("Tipo e obrigatorio");
		}
		if (!"BRL".equalsIgnoreCase(moeda)) {
			throw new DadosInvalidosException("A moeda suportada nesta etapa e BRL");
		}
		if (saldoInicial == null) {
			throw new DadosInvalidosException("Saldo inicial e obrigatorio");
		}
		if (dataSaldoInicial == null) {
			throw new DadosInvalidosException("Data do saldo inicial e obrigatoria");
		}
		if (dataSaldoInicial.isAfter(LocalDate.now())) {
			throw new DadosInvalidosException("Data do saldo inicial nao pode ser futura");
		}
		PessoaFinanceira titular = pessoaRepository.findByIdAndEmpresaId(titularId, contexto.empresaId())
				.orElseThrow(PessoaFinanceiraNaoEncontradaException::new);
		boolean titularAtual = contaAtual != null && contaAtual.getTitular() != null
				&& titular.getId().equals(contaAtual.getTitular().getId());
		if (!titular.estaAtiva() && !titularAtual) {
			throw new PessoaFinanceiraInativaException();
		}
		InstituicaoFinanceira instituicao = instituicaoService.buscarDisponivel(instituicaoId, contexto);
		if (exigeInstituicao(tipo) && instituicao == null) {
			throw new DadosInvalidosException("Instituicao financeira e obrigatoria para este tipo de conta");
		}
		if ((tipo == TipoContaFinanceira.DINHEIRO || tipo == TipoContaFinanceira.CARTEIRA) && permiteConciliacao) {
			throw new DadosInvalidosException("Dinheiro e carteira nao permitem conciliacao");
		}
		return new DadosConta(nomeNormalizado, titular, instituicao, MoedaUtils.normalizar(saldoInicial));
	}

	private boolean dadosFinanceirosAlterados(
			ContaFinanceira conta, DadosConta dados, TipoContaFinanceira tipo, LocalDate dataSaldoInicial) {
		return conta.getTipo() != tipo
				|| conta.getSaldoInicial().compareTo(dados.saldoInicial()) != 0
				|| !conta.getDataSaldoInicial().equals(dataSaldoInicial);
	}

	private boolean exigeInstituicao(TipoContaFinanceira tipo) {
		return tipo == TipoContaFinanceira.CONTA_CORRENTE
				|| tipo == TipoContaFinanceira.CONTA_PAGAMENTO
				|| tipo == TipoContaFinanceira.POUPANCA
				|| tipo == TipoContaFinanceira.INVESTIMENTO;
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

	private ContaFinanceira buscarDaEmpresa(UUID contaId, UUID empresaId) {
		if (contaId == null) {
			throw new ContaFinanceiraNaoEncontradaException();
		}
		return contaRepository.findByIdAndEmpresaId(contaId, empresaId)
				.orElseThrow(ContaFinanceiraNaoEncontradaException::new);
	}

	private Usuario buscarAutor(UUID usuarioId) {
		return usuarioRepository.findById(usuarioId).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto, "contexto e obrigatorio");
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}

	private record DadosConta(
			String nome, PessoaFinanceira titular, InstituicaoFinanceira instituicao, BigDecimal saldoInicial) {
	}
}
