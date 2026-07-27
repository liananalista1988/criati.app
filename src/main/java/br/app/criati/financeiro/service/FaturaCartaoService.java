package br.app.criati.financeiro.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.exception.AcessoNegadoException;
import br.app.criati.exception.CartaoCreditoNaoEncontradoException;
import br.app.criati.exception.DadosInvalidosException;
import br.app.criati.exception.EmpresaNaoEncontradaException;
import br.app.criati.exception.FaturaCartaoNaoEncontradaException;
import br.app.criati.exception.FaturaCartaoStatusInvalidoException;
import br.app.criati.exception.UsuarioNaoEncontradoException;
import br.app.criati.financeiro.model.CartaoCredito;
import br.app.criati.financeiro.model.FaturaCartao;
import br.app.criati.financeiro.model.ParcelaCompraCartao;
import br.app.criati.financeiro.repository.CartaoCreditoRepository;
import br.app.criati.financeiro.repository.FaturaCartaoRepository;
import br.app.criati.financeiro.repository.ParcelaCompraCartaoRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusFaturaCartao;
import br.app.criati.shared.enums.StatusParcelaCartao;
import br.app.criati.tenant.ContextoEmpresaAtual;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@Service
public class FaturaCartaoService {

	private final FaturaCartaoRepository faturas;
	private final ParcelaCompraCartaoRepository parcelas;
	private final CartaoCreditoRepository cartoes;
	private final EmpresaRepository empresas;
	private final UsuarioRepository usuarios;

	public FaturaCartaoService(FaturaCartaoRepository faturas, ParcelaCompraCartaoRepository parcelas,
			CartaoCreditoRepository cartoes, EmpresaRepository empresas, UsuarioRepository usuarios) {
		this.faturas = faturas;
		this.parcelas = parcelas;
		this.cartoes = cartoes;
		this.empresas = empresas;
		this.usuarios = usuarios;
	}

	@Transactional(readOnly = true)
	public List<FaturaCartao> listar(ContextoEmpresaAtual contexto, UUID cartaoPrincipalId,
			StatusFaturaCartao status) {
		Objects.requireNonNull(contexto);
		if (cartaoPrincipalId != null) {
			return faturas.findAllByEmpresaIdAndCartaoPrincipalIdOrderByCompetenciaDesc(
					contexto.empresaId(), cartaoPrincipalId).stream()
					.filter(f -> status == null || f.getStatus() == status)
					.toList();
		}
		if (status != null) {
			return faturas.findAllByEmpresaIdAndStatusOrderByCompetenciaDesc(contexto.empresaId(), status);
		}
		return faturas.findAllByEmpresaIdOrderByCompetenciaDesc(contexto.empresaId());
	}

	@Transactional(readOnly = true)
	public ResultadoFatura buscar(UUID id, ContextoEmpresaAtual contexto) {
		FaturaCartao fatura = faturas.findByIdAndEmpresaId(id, contexto.empresaId())
				.orElseThrow(FaturaCartaoNaoEncontradaException::new);
		return resultado(fatura);
	}

	@Transactional
	public ResultadoFatura abrir(UUID cartaoId, LocalDate competenciaInformada, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		if (competenciaInformada == null) {
			throw new DadosInvalidosException("Competencia e obrigatoria");
		}
		CartaoCredito informado = cartoes.findByIdAndEmpresaId(cartaoId, contexto.empresaId())
				.orElseThrow(CartaoCreditoNaoEncontradoException::new);
		UUID principalId = informado.ehVirtual() ? informado.getCartaoPrincipal().getId() : informado.getId();
		CartaoCredito principal = cartoes.findForUpdateByIdAndEmpresaId(principalId, contexto.empresaId())
				.orElseThrow(CartaoCreditoNaoEncontradoException::new);
		validarPrincipal(principal);
		LocalDate competencia = competenciaEfetiva(principal, competenciaInformada);

		var existente = faturas.findByEmpresaIdAndCartaoPrincipalIdAndCompetencia(
				contexto.empresaId(), principal.getId(), competencia);
		if (existente.isPresent()) {
			FaturaCartao fatura = existente.get();
			if (fatura.estaAberta()) {
				recomporInterno(fatura, autor(contexto));
			}
			return resultado(fatura);
		}

		Usuario autor = autor(contexto);
		YearMonth mes = YearMonth.from(competencia);
		LocalDate fechamento = diaNoMes(mes, principal.getDiaFechamento());
		LocalDate fechamentoAnterior = diaNoMes(mes.minusMonths(1), principal.getDiaFechamento());
		Empresa empresa = empresas.findById(contexto.empresaId()).orElseThrow(EmpresaNaoEncontradaException::new);
		FaturaCartao fatura = new FaturaCartao(empresa, principal, competencia,
				fechamentoAnterior.plusDays(1), fechamento, fechamento, competencia, autor);
		faturas.saveAndFlush(fatura);
		recomporInterno(fatura, autor);
		return resultado(fatura);
	}

	@Transactional
	public ResultadoFatura recompor(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		FaturaCartao referencia = faturas.findByIdAndEmpresaId(id, contexto.empresaId())
				.orElseThrow(FaturaCartaoNaoEncontradaException::new);
		cartoes.findForUpdateByIdAndEmpresaId(referencia.getCartaoPrincipal().getId(), contexto.empresaId())
				.orElseThrow(CartaoCreditoNaoEncontradoException::new);
		FaturaCartao fatura = faturas.findForUpdateByIdAndEmpresaId(id, contexto.empresaId())
				.orElseThrow(FaturaCartaoNaoEncontradaException::new);
		recomporInterno(fatura, autor(contexto));
		return resultado(fatura);
	}

	@Transactional
	public ResultadoFatura fechar(UUID id, ContextoEmpresaAtual contexto) {
		exigirAdministrador(contexto);
		FaturaCartao referencia = faturas.findByIdAndEmpresaId(id, contexto.empresaId())
				.orElseThrow(FaturaCartaoNaoEncontradaException::new);
		cartoes.findForUpdateByIdAndEmpresaId(referencia.getCartaoPrincipal().getId(), contexto.empresaId())
				.orElseThrow(CartaoCreditoNaoEncontradoException::new);
		FaturaCartao fatura = faturas.findForUpdateByIdAndEmpresaId(id, contexto.empresaId())
				.orElseThrow(FaturaCartaoNaoEncontradaException::new);
		if (!fatura.estaAberta()) {
			throw new FaturaCartaoStatusInvalidoException("Fatura ja esta fechada");
		}
		if (LocalDate.now().isBefore(fatura.getDataFechamento())) {
			throw new FaturaCartaoStatusInvalidoException("Fatura nao pode ser fechada antes da data de fechamento");
		}
		Usuario autor = autor(contexto);
		recomporInterno(fatura, autor);
		fatura.fechar(autor);
		return resultado(fatura);
	}

	private void recomporInterno(FaturaCartao fatura, Usuario autor) {
		if (!fatura.estaAberta()) {
			throw new FaturaCartaoStatusInvalidoException("Fatura fechada nao pode ser alterada");
		}
		List<ParcelaCompraCartao> elegiveis = parcelas.buscarElegiveisParaFatura(
				fatura.getEmpresa().getId(), fatura.getCartaoPrincipal().getId(), fatura.getCompetencia(),
				StatusParcelaCartao.ABERTA, fatura.getId());
		elegiveis.forEach(parcela -> parcela.associarFatura(fatura.getId(), autor));
		BigDecimal total = elegiveis.stream()
				.map(ParcelaCompraCartao::getValor)
				.reduce(BigDecimal.ZERO, BigDecimal::add)
				.setScale(2, RoundingMode.HALF_UP);
		fatura.recompor(total, autor);
	}

	private ResultadoFatura resultado(FaturaCartao fatura) {
		return new ResultadoFatura(fatura,
				parcelas.findAllByEmpresaIdAndFaturaIdOrderById(fatura.getEmpresa().getId(), fatura.getId()));
	}

	private void validarPrincipal(CartaoCredito principal) {
		if (!principal.ehPrincipal()) {
			throw new DadosInvalidosException("Cartao faturador deve ser principal");
		}
		if (principal.getDiaFechamento() == null || principal.getDiaVencimento() == null) {
			throw new DadosInvalidosException("Cartao principal deve possuir fechamento e vencimento");
		}
	}

	private LocalDate competenciaEfetiva(CartaoCredito principal, LocalDate informada) {
		return diaNoMes(YearMonth.from(informada), principal.getDiaVencimento());
	}

	private LocalDate diaNoMes(YearMonth mes, int dia) {
		return mes.atDay(Math.min(dia, mes.lengthOfMonth()));
	}

	private Usuario autor(ContextoEmpresaAtual contexto) {
		return usuarios.findById(contexto.usuarioId()).orElseThrow(UsuarioNaoEncontradoException::new);
	}

	private void exigirAdministrador(ContextoEmpresaAtual contexto) {
		Objects.requireNonNull(contexto);
		if (contexto.perfil() != PerfilUsuario.ADMINISTRADOR) {
			throw new AcessoNegadoException();
		}
	}

	public record ResultadoFatura(FaturaCartao fatura, List<ParcelaCompraCartao> parcelas) {
	}
}
