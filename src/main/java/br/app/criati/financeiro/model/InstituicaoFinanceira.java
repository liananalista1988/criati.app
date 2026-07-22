package br.app.criati.financeiro.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.StatusCadastro;
import br.app.criati.usuario.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "instituicao_financeira")
public class InstituicaoFinanceira {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "empresa_id", updatable = false)
	private Empresa empresa;

	@Column(name = "nome", nullable = false, length = 150)
	private String nome;

	@Column(name = "codigo", length = 20)
	private String codigo;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusCadastro status;

	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@Column(name = "atualizado_em", nullable = false)
	private OffsetDateTime atualizadoEm;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "criado_por_usuario_id", updatable = false)
	private Usuario criadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atualizado_por_usuario_id")
	private Usuario atualizadoPor;

	public InstituicaoFinanceira(Empresa empresa, String nome, String codigo, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula para cadastro local");
		this.nome = Objects.requireNonNull(nome, "nome nao pode ser nulo");
		this.codigo = codigo;
		this.status = StatusCadastro.ATIVO;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public boolean estaDisponivelPara(UUID empresaId) {
		return status == StatusCadastro.ATIVO && (empresa == null || empresa.getId().equals(empresaId));
	}
}
