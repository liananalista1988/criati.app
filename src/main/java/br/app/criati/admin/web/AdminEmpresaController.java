package br.app.criati.admin.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.acesso.web.UsuarioEmpresaResponse;
import br.app.criati.admin.AdminEmpresaService;
import br.app.criati.admin.AdminEmpresaService.EmpresaComConviteAdministrador;
import br.app.criati.admin.AdminEmpresaService.EmpresaDetalheDados;
import br.app.criati.convite.model.Convite;
import br.app.criati.convite.web.ConviteResponse;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.empresa.web.EmpresaResponse;
import br.app.criati.security.UsuarioPrincipal;
import br.app.criati.usuario.web.UsuarioResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/empresas")
public class AdminEmpresaController {

	private final AdminEmpresaService adminEmpresaService;
	private final boolean exporTokenBruto;

	public AdminEmpresaController(
			AdminEmpresaService adminEmpresaService,
			@Value("${criati.convite.expor-token-bruto:false}") boolean exporTokenBruto) {
		this.adminEmpresaService = adminEmpresaService;
		this.exporTokenBruto = exporTokenBruto;
	}

	@PostMapping
	public ResponseEntity<EmpresaComAdministradorResponse> criar(
			@Valid @RequestBody CriarEmpresaComAdministradorRequest request) {
		UsuarioEmpresa vinculo = adminEmpresaService.criarComAdministrador(
				request.empresa().nome(),
				request.empresa().nomeFantasia(),
				request.empresa().cnpj(),
				request.administrador().nome(),
				request.administrador().email(),
				request.administrador().senha());

		EmpresaComAdministradorResponse response = new EmpresaComAdministradorResponse(
				paraEmpresaResponse(vinculo.getEmpresa()),
				paraUsuarioResponse(vinculo.getUsuario()),
				paraVinculoResponse(vinculo));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// Onboarding: vincula um usuario global ja existente como Administrador da
	// empresa recem-criada (sem definir/alterar senha).
	@PostMapping("/com-administrador-existente")
	public ResponseEntity<EmpresaComAdministradorExistenteResponse> criarComAdministradorExistente(
			@Valid @RequestBody CriarEmpresaComAdministradorExistenteRequest request) {
		UsuarioEmpresa vinculo = adminEmpresaService.criarComAdministradorExistente(
				request.empresa().nome(),
				request.empresa().nomeFantasia(),
				request.empresa().cnpj(),
				request.aplicacoesIniciais(),
				request.administradorUsuarioId());

		EmpresaComAdministradorExistenteResponse response = new EmpresaComAdministradorExistenteResponse(
				paraEmpresaResponse(vinculo.getEmpresa()),
				request.aplicacoesIniciais(),
				paraUsuarioResponse(vinculo.getUsuario()),
				paraVinculoResponse(vinculo));
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	// Onboarding: cria a empresa e gera um convite de Administrador para uma
	// pessoa nova; a senha e sempre definida pelo proprio convidado ao aceitar.
	@PostMapping("/com-administrador-convidado")
	public ResponseEntity<EmpresaComConviteAdministradorResponse> criarComAdministradorConvidado(
			@Valid @RequestBody CriarEmpresaComAdministradorConviteRequest request,
			@AuthenticationPrincipal UsuarioPrincipal principal) {
		EmpresaComConviteAdministrador resultado = adminEmpresaService.criarComAdministradorConvidado(
				request.empresa().nome(),
				request.empresa().nomeFantasia(),
				request.empresa().cnpj(),
				request.aplicacoesIniciais(),
				request.administrador().nome(),
				request.administrador().email(),
				principal.getUsuario().getId());

		EmpresaComConviteAdministradorResponse response = new EmpresaComConviteAdministradorResponse(
				paraEmpresaResponse(resultado.empresa()),
				request.aplicacoesIniciais(),
				paraConviteResponse(resultado.conviteCriado().convite()),
				exporTokenBruto ? resultado.conviteCriado().tokenBruto() : null);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<List<EmpresaResponse>> listar() {
		List<EmpresaResponse> empresas = adminEmpresaService.listarTodas().stream()
				.map(AdminEmpresaController::paraEmpresaResponse)
				.toList();
		return ResponseEntity.ok(empresas);
	}

	@GetMapping("/{empresaId}")
	public ResponseEntity<EmpresaDetalheResponse> detalhar(@PathVariable UUID empresaId) {
		EmpresaDetalheDados dados = adminEmpresaService.buscarDetalhe(empresaId);
		Empresa empresa = dados.empresa();
		EmpresaDetalheResponse response = new EmpresaDetalheResponse(
				empresa.getId(),
				empresa.getNome(),
				empresa.getNomeFantasia(),
				empresa.getCnpj(),
				empresa.getStatus(),
				empresa.getCriadoEm(),
				empresa.getAtualizadoEm(),
				dados.quantidadeUsuariosAtivos(),
				dados.administradoresAtivos(),
				dados.aplicacoesHabilitadas(),
				dados.convitesPendentes(),
				dados.pronta()
						? EmpresaDetalheResponse.SituacaoOperacional.PRONTA
						: EmpresaDetalheResponse.SituacaoOperacional.PENDENTE);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{empresaId}/ativar")
	public ResponseEntity<EmpresaResponse> ativar(@PathVariable UUID empresaId) {
		return ResponseEntity.ok(paraEmpresaResponse(adminEmpresaService.ativar(empresaId)));
	}

	@PostMapping("/{empresaId}/inativar")
	public ResponseEntity<EmpresaResponse> inativar(@PathVariable UUID empresaId) {
		return ResponseEntity.ok(paraEmpresaResponse(adminEmpresaService.inativar(empresaId)));
	}

	private static EmpresaResponse paraEmpresaResponse(Empresa empresa) {
		return new EmpresaResponse(
				empresa.getId(),
				empresa.getNome(),
				empresa.getNomeFantasia(),
				empresa.getCnpj(),
				empresa.getStatus());
	}

	private static UsuarioResponse paraUsuarioResponse(br.app.criati.usuario.model.Usuario usuario) {
		return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getStatus());
	}

	private static UsuarioEmpresaResponse paraVinculoResponse(UsuarioEmpresa vinculo) {
		return new UsuarioEmpresaResponse(
				vinculo.getId(),
				vinculo.getUsuario().getId(),
				vinculo.getEmpresa().getId(),
				vinculo.getPerfil(),
				vinculo.getStatus());
	}

	private static ConviteResponse paraConviteResponse(Convite convite) {
		OffsetDateTime agora = OffsetDateTime.now();
		return new ConviteResponse(
				convite.getId(),
				convite.getEmail(),
				convite.getPerfil(),
				convite.getStatusEfetivo(agora),
				convite.getExpiraEm(),
				convite.getCriadoEm(),
				convite.getUtilizadoEm());
	}
}
