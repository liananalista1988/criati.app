package br.app.criati.shared.validacao;

import org.springframework.stereotype.Component;

import br.app.criati.exception.DadosInvalidosException;

// Centralizado para reaproveitamento futuro (ex.: redefinicao de senha).
// Tamanho minimo segue a politica ja documentada em docs/SEGURANCA.MD
// ("minimo de 15 caracteres enquanto nao houver MFA"), nao reduzido aqui.
// Sem exigencia de classes de caractere (mai/min/numero/simbolo) - politica
// de comprimento e frase-senha, nao de complexidade forcada.
@Component
public class SenhaValidador {

	private static final int TAMANHO_MINIMO = 15;
	private static final int TAMANHO_MAXIMO = 72;

	public void validar(String senha, String confirmacaoSenha) {
		if (senha == null || senha.isBlank()) {
			throw new DadosInvalidosException("Senha e obrigatoria");
		}
		if (senha.length() < TAMANHO_MINIMO) {
			throw new DadosInvalidosException("Senha deve possuir no minimo " + TAMANHO_MINIMO + " caracteres");
		}
		if (senha.length() > TAMANHO_MAXIMO) {
			throw new DadosInvalidosException("Senha deve possuir no maximo " + TAMANHO_MAXIMO + " caracteres");
		}
		if (!senha.equals(confirmacaoSenha)) {
			throw new DadosInvalidosException("Confirmacao de senha nao corresponde a senha");
		}
	}
}
