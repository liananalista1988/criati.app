package br.app.criati.admin;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SuperAdministradorBootstrapRunner implements ApplicationRunner {

	private final SuperAdministradorBootstrapService bootstrapService;
	private final String nome;
	private final String email;
	private final String senha;

	public SuperAdministradorBootstrapRunner(
			SuperAdministradorBootstrapService bootstrapService,
			@Value("${criati.bootstrap.nome:}") String nome,
			@Value("${criati.bootstrap.email:}") String email,
			@Value("${criati.bootstrap.password:}") String senha) {
		this.bootstrapService = bootstrapService;
		this.nome = nome;
		this.email = email;
		this.senha = senha;
	}

	@Override
	public void run(ApplicationArguments args) {
		bootstrapService.executar(nome, email, senha);
	}
}
