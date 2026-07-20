package br.app.criati.admin;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.repository.UsuarioEmpresaRepository;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.repository.EmpresaRepository;
import br.app.criati.empresa.service.CadastrarEmpresaService;
import br.app.criati.shared.enums.PerfilUsuario;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import br.app.criati.usuario.service.CadastrarUsuarioService;

@Service
public class AdminEmpresaService {

	private final CadastrarEmpresaService cadastrarEmpresaService;
	private final CadastrarUsuarioService cadastrarUsuarioService;
	private final UsuarioEmpresaRepository usuarioEmpresaRepository;
	private final EmpresaRepository empresaRepository;

	public AdminEmpresaService(
			CadastrarEmpresaService cadastrarEmpresaService,
			CadastrarUsuarioService cadastrarUsuarioService,
			UsuarioEmpresaRepository usuarioEmpresaRepository,
			EmpresaRepository empresaRepository) {
		this.cadastrarEmpresaService = cadastrarEmpresaService;
		this.cadastrarUsuarioService = cadastrarUsuarioService;
		this.usuarioEmpresaRepository = usuarioEmpresaRepository;
		this.empresaRepository = empresaRepository;
	}

	// Transacional: se a empresa OU o usuario administrador falharem
	// (CNPJ/e-mail duplicado, dados invalidos), nada e persistido. O perfil e
	// sempre fixado como ADMINISTRADOR no backend, nunca aceito do cliente.
	@Transactional
	public UsuarioEmpresa criarComAdministrador(
			String nomeEmpresa,
			String nomeFantasia,
			String cnpj,
			String nomeAdministrador,
			String email,
			String senha) {
		Empresa empresa = cadastrarEmpresaService.executar(nomeEmpresa, nomeFantasia, cnpj);
		Usuario administrador = cadastrarUsuarioService.executar(nomeAdministrador, email, senha);

		UsuarioEmpresa vinculo = new UsuarioEmpresa(
				administrador,
				empresa,
				PerfilUsuario.ADMINISTRADOR,
				StatusCadastro.ATIVO);
		return usuarioEmpresaRepository.save(vinculo);
	}

	@Transactional(readOnly = true)
	public List<Empresa> listarTodas() {
		return empresaRepository.findAll();
	}
}
