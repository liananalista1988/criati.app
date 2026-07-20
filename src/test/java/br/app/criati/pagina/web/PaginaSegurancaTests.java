package br.app.criati.pagina.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Confirma que as novas rotas de pagina (/login, /app/**) nao alteram o
 * comportamento ja coberto por AuthControllerTests/ErrorHandlingSecurityTests
 * para a API: /api/** continua respondendo 401 em JSON, enquanto paginas
 * anonimas sao redirecionadas para /login (sem corpo JSON, sem sessao).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class PaginaSegurancaTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void loginEPublicaParaAnonimo() throws Exception {
		mockMvc.perform(get("/login")).andExpect(status().isOk());
	}

	@Test
	void appDashboardAnonimoRedirecionaParaLoginSemJson() throws Exception {
		MvcResult resultado = mockMvc.perform(get("/app/dashboard"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")))
				.andReturn();

		String contentType = resultado.getResponse().getContentType();
		assertThat(contentType).isNotEqualTo(MediaType.APPLICATION_JSON_VALUE);
		assertThat(resultado.getRequest().getSession(false)).isNull();
	}

	@Test
	void appAnonimoRedirecionaParaLogin() throws Exception {
		mockMvc.perform(get("/app"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/login")));
	}

	@Test
	void apiContinuaRespondendo401EmJsonParaAnonimo() throws Exception {
		MvcResult resultado = mockMvc.perform(get("/api/usuarios"))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertThat(resultado.getResponse().getContentType()).contains(MediaType.APPLICATION_JSON_VALUE);
		assertThat(resultado.getRequest().getSession(false)).isNull();
	}

	@Test
	void recursosEstaticosSaoAcessiveisSemAutenticacao() throws Exception {
		mockMvc.perform(get("/css/criati-base.css")).andExpect(status().isOk());
		mockMvc.perform(get("/js/criati-api.js")).andExpect(status().isOk());
	}
}
