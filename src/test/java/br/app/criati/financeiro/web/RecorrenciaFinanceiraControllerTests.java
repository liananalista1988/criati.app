package br.app.criati.financeiro.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.aplicacao.service.AplicacaoService;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.financeiro.model.CategoriaFinanceira;
import br.app.criati.financeiro.model.ContaFinanceira;
import br.app.criati.financeiro.repository.CategoriaFinanceiraRepository;
import br.app.criati.financeiro.repository.ContaFinanceiraRepository;
import br.app.criati.financeiro.shared.model.ParteFinanceira;
import br.app.criati.financeiro.shared.model.PessoaFinanceira;
import br.app.criati.financeiro.shared.model.TipoParteFinanceira;
import br.app.criati.financeiro.shared.repository.ParteFinanceiraRepository;
import br.app.criati.financeiro.shared.repository.PessoaFinanceiraRepository;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.shared.enums.TipoContaFinanceira;
import br.app.criati.shared.enums.TipoFinanceiro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RecorrenciaFinanceiraControllerTests {

	private static final String SENHA = "senha-correta";
	private static final String URL_BASE = "/api/contexto/financeiro/recorrencias";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private EmpresaRepository empresaRepository;

	@Autowired
	private UsuarioEmpresaRepository usuarioEmpresaRepository;

	@Autowired
	private ContaFinanceiraRepository contaFinanceiraRepository;

	@Autowired
	private CategoriaFinanceiraRepository categoriaFinanceiraRepository;

	@Autowired
	private PessoaFinanceiraRepository pessoaFinanceiraRepository;

	@Autowired
	private ParteFinanceiraRepository parteFinanceiraRepository;

	@Autowired
	private AplicacaoService aplicacaoService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	void deveCriarRecorrenciaMensalDeReceita() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000311");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("rec.mensal.receita@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Salario mensal","valorPadrao":5000.00,
						 "contaId":"%s","categoriaId":"%s","pessoaFinanceiraId":"%s",
						 "periodicidade":"MENSAL","intervalo":1,"dia":5,"dataInicial":"2026-01-01",
						 "gerarAutomaticamente":false}
						""".formatted(conta.getId(), categoria.getId(), pessoa.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ATIVA"))
				.andExpect(jsonPath("$.periodicidade").value("MENSAL"))
				.andExpect(jsonPath("$.proximaCompetencia").value("2026-01"))
				.andExpect(jsonPath("$.quantidadeOcorrencias").value(0));
	}

	@Test
	void deveCriarRecorrenciaMensalDeDespesaEAnualComMes() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000312");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Condominio", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("rec.mensal.despesa@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"DESPESA","descricao":"Condominio","valorPadrao":450.00,
						 "contaId":"%s","categoriaId":"%s","pessoaFinanceiraId":"%s",
						 "periodicidade":"MENSAL","intervalo":1,"dia":10,"dataInicial":"2026-01-01",
						 "gerarAutomaticamente":false}
						""".formatted(conta.getId(), categoria.getId(), pessoa.getId())))
				.andExpect(status().isCreated());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"DESPESA","descricao":"Assinatura anual","valorPadrao":199.90,
						 "contaId":"%s","categoriaId":"%s","pessoaFinanceiraId":"%s",
						 "periodicidade":"ANUAL","intervalo":1,"dia":15,"mes":3,"dataInicial":"2026-01-01",
						 "gerarAutomaticamente":false}
						""".formatted(conta.getId(), categoria.getId(), pessoa.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.periodicidade").value("ANUAL"))
				.andExpect(jsonPath("$.mesReferencia").value(3))
				.andExpect(jsonPath("$.proximaCompetencia").value("2026-03"));
	}

	@Test
	void validacoesBasicasRetornam400() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("33333333000313");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoriaReceita = criarCategoria(empresa, "Renda", TipoFinanceiro.RECEITA);
		CategoriaFinanceira categoriaDespesa = criarCategoria(empresa, "Contas", TipoFinanceiro.DESPESA);
		Usuario admin = criarUsuario("rec.validacoes@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());

		// descricao ausente
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","valorPadrao":10,"contaId":"%s","categoriaId":"%s","pessoaFinanceiraId":"%s",
						 "periodicidade":"MENSAL","intervalo":1,"dia":5,"dataInicial":"2026-01-01"}
						""".formatted(conta.getId(), categoriaReceita.getId(), pessoa.getId())))
				.andExpect(status().isBadRequest());

		// valor zero
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Invalido","valorPadrao":0,"contaId":"%s","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","periodicidade":"MENSAL","intervalo":1,"dia":5,"dataInicial":"2026-01-01"}
						""".formatted(conta.getId(), categoriaReceita.getId(), pessoa.getId())))
				.andExpect(status().isBadRequest());

		// data final anterior a data inicial
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Datas invalidas","valorPadrao":10,"contaId":"%s","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","periodicidade":"MENSAL","intervalo":1,"dia":5,
						 "dataInicial":"2026-06-01","dataFinal":"2026-01-01"}
						""".formatted(conta.getId(), categoriaReceita.getId(), pessoa.getId())))
				.andExpect(status().isBadRequest());

		// categoria de despesa com tipo receita
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Tipo incompativel","valorPadrao":10,"contaId":"%s","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","periodicidade":"MENSAL","intervalo":1,"dia":5,"dataInicial":"2026-01-01"}
						""".formatted(conta.getId(), categoriaDespesa.getId(), pessoa.getId())))
				.andExpect(status().isBadRequest());

		// anual sem mes de referencia
		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Anual sem mes","valorPadrao":10,"contaId":"%s","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","periodicidade":"ANUAL","intervalo":1,"dia":5,"dataInicial":"2026-01-01"}
						""".formatted(conta.getId(), categoriaReceita.getId(), pessoa.getId())))
				.andExpect(status().isBadRequest());
	}

	@Test
	void contaCategoriaPessoaEParteDeOutraEmpresaSaoRejeitadas() throws Exception {
		Empresa a = criarEmpresaComFinanceiro("44444444000314");
		Empresa b = criarEmpresaComFinanceiro("55555555000315");
		Usuario adminA = criarUsuario("rec.tenant.a@criati.test");
		criarVinculo(adminA, a, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("rec.tenant.b@criati.test");
		criarVinculo(adminB, b, PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira contaA = criarConta(a, "Conta A");
		ContaFinanceira contaB = criarConta(b, "Conta B");
		CategoriaFinanceira categoriaA = criarCategoria(a, "Renda A", TipoFinanceiro.RECEITA);
		CategoriaFinanceira categoriaB = criarCategoria(b, "Renda B", TipoFinanceiro.RECEITA);
		PessoaFinanceira pessoaA = criarPessoa(a, adminA, "Pessoa A");
		PessoaFinanceira pessoaB = criarPessoa(b, adminB, "Pessoa B");
		ParteFinanceira parteB = criarParte(b, adminB, "Parte B");
		MockHttpSession session = autenticarNaEmpresa(adminA.getEmail(), a.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Conta de outro tenant","valorPadrao":10,"contaId":"%s",
						 "categoriaId":"%s","pessoaFinanceiraId":"%s","periodicidade":"MENSAL","intervalo":1,"dia":5,
						 "dataInicial":"2026-01-01"}
						""".formatted(contaB.getId(), categoriaA.getId(), pessoaA.getId())))
				.andExpect(status().isNotFound());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Categoria de outro tenant","valorPadrao":10,"contaId":"%s",
						 "categoriaId":"%s","pessoaFinanceiraId":"%s","periodicidade":"MENSAL","intervalo":1,"dia":5,
						 "dataInicial":"2026-01-01"}
						""".formatted(contaA.getId(), categoriaB.getId(), pessoaA.getId())))
				.andExpect(status().isNotFound());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Pessoa de outro tenant","valorPadrao":10,"contaId":"%s",
						 "categoriaId":"%s","pessoaFinanceiraId":"%s","periodicidade":"MENSAL","intervalo":1,"dia":5,
						 "dataInicial":"2026-01-01"}
						""".formatted(contaA.getId(), categoriaA.getId(), pessoaB.getId())))
				.andExpect(status().isNotFound());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Parte de outro tenant","valorPadrao":10,"contaId":"%s",
						 "categoriaId":"%s","pessoaFinanceiraId":"%s","parteFinanceiraId":"%s","periodicidade":"MENSAL",
						 "intervalo":1,"dia":5,"dataInicial":"2026-01-01"}
						""".formatted(contaA.getId(), categoriaA.getId(), pessoaA.getId(), parteB.getId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void usuarioComumNaoPodeCriarRecorrenciaEAnonimoRecebe401() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("66666666000316");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Renda", TipoFinanceiro.RECEITA);
		Usuario usuarioComum = criarUsuario("rec.usuario.comum@criati.test");
		criarVinculo(usuarioComum, empresa, PerfilUsuario.USUARIO);
		Usuario admin = criarUsuario("rec.usuario.comum.admin@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(usuarioComum.getEmail(), empresa.getId());

		mockMvc.perform(post(URL_BASE).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"RECEITA","descricao":"Bloqueado","valorPadrao":10,"contaId":"%s","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","periodicidade":"MENSAL","intervalo":1,"dia":5,"dataInicial":"2026-01-01"}
						""".formatted(conta.getId(), categoria.getId(), pessoa.getId())))
				.andExpect(status().isForbidden());

		mockMvc.perform(get(URL_BASE)).andExpect(status().isUnauthorized());
	}

	@Test
	void deveGerarOcorrenciaIntegradaAoLancamentoDeFormaIdempotente() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("77777777000317");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("rec.gerar.idempotente@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		LocalDate dataInicial = LocalDate.now().withDayOfMonth(1);
		String id = criarRecorrencia(session, "RECEITA", conta, categoria, pessoa, "Salario", "5000.00", 5,
				dataInicial, false);
		YearMonth competenciaAtual = YearMonth.now();

		MvcResult primeira = mockMvc.perform(post(URL_BASE + "/" + id + "/gerar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.origem").value("RECORRENCIA"))
				.andExpect(jsonPath("$.status").value("PENDENTE"))
				.andReturn();
		String lancamentoId = com.jayway.jsonpath.JsonPath.read(primeira.getResponse().getContentAsString(), "$.id");

		// reexecucao (ex.: retry apos falha de rede) apontando para a mesma competencia ja gerada
		// nao duplica - o mesmo lancamento e devolvido, seja via /gerar-competencia, seja via nova
		// chamada de geracao automatica em lote.
		MvcResult retentativa = mockMvc.perform(post(URL_BASE + "/" + id + "/gerar-competencia").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"competencia\":\"" + competenciaAtual + "\"}"))
				.andExpect(status().isOk())
				.andReturn();
		String lancamentoIdRepetido = com.jayway.jsonpath.JsonPath.read(retentativa.getResponse().getContentAsString(), "$.id");
		assertThat(lancamentoIdRepetido).isEqualTo(lancamentoId);

		mockMvc.perform(get(URL_BASE + "/" + id + "/ocorrencias").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));
	}

	@Test
	void naoGeraQuandoPausadaOuEncerrada() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("88888888000318");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("rec.pausada.encerrada@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		LocalDate dataInicial = LocalDate.now().withDayOfMonth(1);
		String id = criarRecorrencia(session, "RECEITA", conta, categoria, pessoa, "Salario", "5000.00", 5,
				dataInicial, false);

		mockMvc.perform(post(URL_BASE + "/" + id + "/pausar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAUSADA"));
		mockMvc.perform(post(URL_BASE + "/" + id + "/gerar").session(session).with(csrf()))
				.andExpect(status().isConflict());

		mockMvc.perform(post(URL_BASE + "/" + id + "/encerrar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENCERRADA"));
		mockMvc.perform(post(URL_BASE + "/" + id + "/gerar").session(session).with(csrf()))
				.andExpect(status().isConflict());
		mockMvc.perform(post(URL_BASE + "/" + id + "/pausar").session(session).with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void naoGeraOcorrenciaForaDoPeriodo() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("11111111000411");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("rec.fora.periodo@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		LocalDate dataInicial = LocalDate.now().plusYears(1).withDayOfMonth(1);
		String id = criarRecorrencia(session, "RECEITA", conta, categoria, pessoa, "Futuro", "100.00", 5,
				dataInicial, false);

		mockMvc.perform(post(URL_BASE + "/" + id + "/gerar").session(session).with(csrf()))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveGerarCompetenciaEspecificaRetroativaSemDuplicar() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("22222222000412");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("rec.retroativa@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		LocalDate dataInicial = LocalDate.now().minusMonths(3).withDayOfMonth(1);
		String id = criarRecorrencia(session, "RECEITA", conta, categoria, pessoa, "Salario", "5000.00", 5,
				dataInicial, false);
		YearMonth competenciaPassada = YearMonth.from(dataInicial).plusMonths(1);

		mockMvc.perform(post(URL_BASE + "/" + id + "/gerar-competencia").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"competencia\":\"" + competenciaPassada + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dataCompetencia").value(competenciaPassada + "-01"));

		mockMvc.perform(post(URL_BASE + "/" + id + "/gerar-competencia").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"competencia\":\"" + competenciaPassada + "\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get(URL_BASE + "/" + id + "/ocorrencias").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		YearMonth futura = YearMonth.now().plusMonths(2);
		mockMvc.perform(post(URL_BASE + "/" + id + "/gerar-competencia").session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"competencia\":\"" + futura + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void deveGerarAutomaticasEmLoteApenasParaElegiveis() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("33333333000413");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("rec.automatica@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		LocalDate dataInicial = LocalDate.now().withDayOfMonth(1);
		String automatica = criarRecorrencia(session, "RECEITA", conta, categoria, pessoa, "Automatica", "100.00",
				5, dataInicial, true);
		String manual = criarRecorrencia(session, "RECEITA", conta, categoria, pessoa, "Manual", "200.00", 5,
				dataInicial, false);

		mockMvc.perform(post(URL_BASE + "/gerar-automaticas").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ocorrenciasGeradas").value(1));

		mockMvc.perform(get(URL_BASE + "/" + automatica + "/ocorrencias").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(get(URL_BASE + "/" + manual + "/ocorrencias").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

		mockMvc.perform(post(URL_BASE + "/gerar-automaticas").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ocorrenciasGeradas").value(0));
	}

	@Test
	void devePausarERetomarRecalculandoProximaCompetenciaSemDuplicar() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("44444444000414");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("rec.pausa.retomada@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		LocalDate dataInicial = LocalDate.now().minusMonths(4).withDayOfMonth(1);
		String id = criarRecorrencia(session, "RECEITA", conta, categoria, pessoa, "Salario", "100.00", 5,
				dataInicial, false);

		mockMvc.perform(post(URL_BASE + "/" + id + "/pausar").session(session).with(csrf()))
				.andExpect(status().isOk());
		mockMvc.perform(post(URL_BASE + "/" + id + "/retomar").session(session).with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ATIVA"))
				.andExpect(jsonPath("$.proximaCompetencia").value(YearMonth.now().toString()));
	}

	@Test
	void encerrarPreservaHistoricoENaoPermiteReativarViaPausarOuEditar() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("55555555000415");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("rec.encerrar@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		LocalDate dataInicial = LocalDate.now().withDayOfMonth(1);
		String id = criarRecorrencia(session, "RECEITA", conta, categoria, pessoa, "Salario", "100.00", 5,
				dataInicial, false);
		mockMvc.perform(post(URL_BASE + "/" + id + "/gerar").session(session).with(csrf())).andExpect(status().isOk());

		mockMvc.perform(post(URL_BASE + "/" + id + "/encerrar").session(session).with(csrf()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ENCERRADA"));

		mockMvc.perform(get(URL_BASE + "/" + id + "/ocorrencias").session(session))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
		mockMvc.perform(post(URL_BASE + "/" + id + "/encerrar").session(session).with(csrf()))
				.andExpect(status().isConflict());
		mockMvc.perform(put(URL_BASE + "/" + id).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Tentativa","valorPadrao":100,"contaId":"%s","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","intervalo":1,"dia":5,"gerarAutomaticamente":false}
						""".formatted(conta.getId(), categoria.getId(), pessoa.getId())))
				.andExpect(status().isConflict());
	}

	@Test
	void edicaoDaSerieNaoAlteraOcorrenciasJaGeradasEAlteraApenasFuturas() throws Exception {
		Empresa empresa = criarEmpresaComFinanceiro("66666666000416");
		ContaFinanceira conta = criarConta(empresa, "Conta");
		CategoriaFinanceira categoria = criarCategoria(empresa, "Salario", TipoFinanceiro.RECEITA);
		Usuario admin = criarUsuario("rec.edicao.serie@criati.test");
		criarVinculo(admin, empresa, PerfilUsuario.ADMINISTRADOR);
		PessoaFinanceira pessoa = criarPessoa(empresa, admin, "Pessoa A");
		MockHttpSession session = autenticarNaEmpresa(admin.getEmail(), empresa.getId());
		LocalDate dataInicial = LocalDate.now().withDayOfMonth(1);
		String id = criarRecorrencia(session, "RECEITA", conta, categoria, pessoa, "Salario", "1000.00", 5,
				dataInicial, false);
		MvcResult gerado = mockMvc.perform(post(URL_BASE + "/" + id + "/gerar").session(session).with(csrf()))
				.andExpect(status().isOk()).andReturn();
		String valorGerado = com.jayway.jsonpath.JsonPath.read(gerado.getResponse().getContentAsString(), "$.valor").toString();
		assertThat(Double.parseDouble(valorGerado)).isEqualTo(1000.0);

		mockMvc.perform(put(URL_BASE + "/" + id).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"descricao":"Salario reajustado","valorPadrao":1200.00,"contaId":"%s","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","intervalo":1,"dia":5,"gerarAutomaticamente":false}
						""".formatted(conta.getId(), categoria.getId(), pessoa.getId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.valorPadrao").value(1200.00));

		mockMvc.perform(get(URL_BASE + "/" + id + "/ocorrencias").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].valor").value(1000.00));
	}

	@Test
	void listaResumoEIsolamentoMultiempresa() throws Exception {
		Empresa a = criarEmpresaComFinanceiro("77777777000417");
		Empresa b = criarEmpresaComFinanceiro("88888888000418");
		Usuario adminA = criarUsuario("rec.resumo.a@criati.test");
		criarVinculo(adminA, a, PerfilUsuario.ADMINISTRADOR);
		Usuario adminB = criarUsuario("rec.resumo.b@criati.test");
		criarVinculo(adminB, b, PerfilUsuario.ADMINISTRADOR);
		ContaFinanceira contaA = criarConta(a, "Conta A");
		CategoriaFinanceira receitaA = criarCategoria(a, "Renda", TipoFinanceiro.RECEITA);
		CategoriaFinanceira despesaA = criarCategoria(a, "Contas", TipoFinanceiro.DESPESA);
		PessoaFinanceira pessoaA = criarPessoa(a, adminA, "Pessoa A");
		ContaFinanceira contaB = criarConta(b, "Conta B");
		CategoriaFinanceira receitaB = criarCategoria(b, "Renda B", TipoFinanceiro.RECEITA);
		PessoaFinanceira pessoaB = criarPessoa(b, adminB, "Pessoa B");
		MockHttpSession sessionA = autenticarNaEmpresa(adminA.getEmail(), a.getId());
		MockHttpSession sessionB = autenticarNaEmpresa(adminB.getEmail(), b.getId());
		LocalDate dataInicial = LocalDate.now().withDayOfMonth(1);
		criarRecorrencia(sessionA, "RECEITA", contaA, receitaA, pessoaA, "Salario A", "3000.00", 5, dataInicial, false);
		String idDespesaA = criarRecorrencia(sessionA, "DESPESA", contaA, despesaA, pessoaA, "Condominio A", "500.00", 10, dataInicial, false);
		criarRecorrencia(sessionB, "RECEITA", contaB, receitaB, pessoaB, "Salario B", "9999.00", 5, dataInicial, false);

		mockMvc.perform(get(URL_BASE).session(sessionA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2));

		mockMvc.perform(get(URL_BASE + "/resumo").session(sessionA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.ativas").value(2))
				.andExpect(jsonPath("$.receitasRecorrentesPrevistas").value(3000.00))
				.andExpect(jsonPath("$.despesasRecorrentesPrevistas").value(500.00));

		mockMvc.perform(get(URL_BASE).session(sessionA).param("tipo", "DESPESA"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].id").value(idDespesaA));

		mockMvc.perform(get(URL_BASE + "/" + idDespesaA).session(sessionB)).andExpect(status().isNotFound());
	}

	private String criarRecorrencia(MockHttpSession session, String tipo, ContaFinanceira conta,
			CategoriaFinanceira categoria, PessoaFinanceira pessoa, String descricao, String valor, int dia,
			LocalDate dataInicial, boolean gerarAutomaticamente) throws Exception {
		MvcResult resultado = mockMvc.perform(post(URL_BASE).session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"tipo":"%s","descricao":"%s","valorPadrao":%s,"contaId":"%s","categoriaId":"%s",
						 "pessoaFinanceiraId":"%s","periodicidade":"MENSAL","intervalo":1,"dia":%d,
						 "dataInicial":"%s","gerarAutomaticamente":%s}
						""".formatted(tipo, descricao, valor, conta.getId(), categoria.getId(), pessoa.getId(), dia,
						dataInicial, gerarAutomaticamente))
				)
				.andExpect(status().isCreated())
				.andReturn();
		return com.jayway.jsonpath.JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
	}

	private PessoaFinanceira criarPessoa(Empresa empresa, Usuario autor, String nome) {
		return pessoaFinanceiraRepository.saveAndFlush(new PessoaFinanceira(empresa, nome, null, null, autor));
	}

	private ParteFinanceira criarParte(Empresa empresa, Usuario autor, String nome) {
		return parteFinanceiraRepository.saveAndFlush(new ParteFinanceira(
				empresa, nome, TipoParteFinanceira.ESTABELECIMENTO, null, null, null, autor));
	}

	private Empresa criarEmpresaComFinanceiro(String cnpj) {
		Empresa empresa = empresaRepository.saveAndFlush(
				new Empresa("Empresa Financeiro Ltda", "Empresa Financeiro", cnpj, StatusCadastro.ATIVO));
		aplicacaoService.habilitar(empresa.getId(), "FINANCEIRO");
		return empresa;
	}

	private ContaFinanceira criarConta(Empresa empresa, String nome) {
		return contaFinanceiraRepository.saveAndFlush(
				new ContaFinanceira(empresa, nome, TipoContaFinanceira.CAIXA, java.math.BigDecimal.ZERO, StatusCadastro.ATIVO));
	}

	private CategoriaFinanceira criarCategoria(Empresa empresa, String nome, TipoFinanceiro tipo) {
		return categoriaFinanceiraRepository.saveAndFlush(
				new CategoriaFinanceira(empresa, nome, tipo, StatusCadastro.ATIVO));
	}

	private Usuario criarUsuario(String email) {
		Usuario usuario = new Usuario("Usuario Teste", email, passwordEncoder.encode(SENHA), StatusCadastro.ATIVO);
		return usuarioRepository.saveAndFlush(usuario);
	}

	private UsuarioEmpresa criarVinculo(Usuario usuario, Empresa empresa, PerfilUsuario perfil) {
		UsuarioEmpresa vinculo = new UsuarioEmpresa(usuario, empresa, perfil, StatusCadastro.ATIVO);
		return usuarioEmpresaRepository.saveAndFlush(vinculo);
	}

	private MockHttpSession autenticarNaEmpresa(String email, UUID empresaId) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "senha": "%s" }
						""".formatted(email, SENHA)))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

		mockMvc.perform(post("/api/contexto/empresa-ativa")
				.session(session).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "empresaId": "%s" }
						""".formatted(empresaId)))
				.andExpect(status().isOk());
		return session;
	}
}
