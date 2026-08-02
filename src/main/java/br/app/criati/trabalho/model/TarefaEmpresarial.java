package br.app.criati.trabalho.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.acesso.model.UsuarioEmpresa;
import br.app.criati.empresa.model.Empresa;
import br.app.criati.exception.TrabalhoStatusInvalidoException;
import br.app.criati.shared.enums.PrioridadeTrabalho;
import br.app.criati.shared.enums.SituacaoTarefaTrabalho;
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
@Table(name = "tarefa_empresarial")
public class TarefaEmpresarial {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "processo_id")
	private ProcessoEmpresarial processo;

	@Column(name = "titulo", nullable = false, length = 200)
	private String titulo;

	@Column(name = "descricao", columnDefinition = "text")
	private String descricao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "responsavel_usuario_empresa_id")
	private UsuarioEmpresa responsavel;

	@Enumerated(EnumType.STRING)
	@Column(name = "situacao", nullable = false, length = 20)
	private SituacaoTarefaTrabalho situacao;

	@Enumerated(EnumType.STRING)
	@Column(name = "prioridade", nullable = false, length = 10)
	private PrioridadeTrabalho prioridade;

	@Column(name = "prazo")
	private LocalDate prazo;

	@Column(name = "data_conclusao")
	private OffsetDateTime dataConclusao;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private StatusCadastro status;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "criado_em", nullable = false, updatable = false)
	private OffsetDateTime criadoEm;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "atualizado_em", nullable = false)
	private OffsetDateTime atualizadoEm;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "criado_por_usuario_id", updatable = false)
	private Usuario criadoPor;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "atualizado_por_usuario_id")
	private Usuario atualizadoPor;

	public TarefaEmpresarial(Empresa empresa, ProcessoEmpresarial processo, String titulo, String descricao,
			UsuarioEmpresa responsavel, PrioridadeTrabalho prioridade, LocalDate prazo, Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.processo = processo;
		this.titulo = Objects.requireNonNull(titulo, "titulo nao pode ser nulo");
		this.descricao = descricao;
		this.responsavel = responsavel;
		this.situacao = SituacaoTarefaTrabalho.PENDENTE;
		this.prioridade = Objects.requireNonNull(prioridade, "prioridade nao pode ser nula");
		this.prazo = prazo;
		this.status = StatusCadastro.ATIVO;
		this.criadoEm = OffsetDateTime.now();
		this.atualizadoEm = this.criadoEm;
		this.criadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoPor = autor;
	}

	public void atualizarDados(String titulo, String descricao, PrioridadeTrabalho prioridade, LocalDate prazo,
			Usuario autor) {
		this.titulo = Objects.requireNonNull(titulo, "titulo nao pode ser nulo");
		this.descricao = descricao;
		this.prioridade = Objects.requireNonNull(prioridade, "prioridade nao pode ser nula");
		this.prazo = prazo;
		registrarAlteracao(autor);
	}

	public void atribuirResponsavel(UsuarioEmpresa responsavel, Usuario autor) {
		this.responsavel = responsavel;
		registrarAlteracao(autor);
	}

	public void iniciar(Usuario autor) {
		exigirAtivo();
		if (situacao != SituacaoTarefaTrabalho.PENDENTE) {
			throw new TrabalhoStatusInvalidoException("Tarefa so pode ser iniciada a partir de PENDENTE");
		}
		this.situacao = SituacaoTarefaTrabalho.EM_ANDAMENTO;
		registrarAlteracao(autor);
	}

	public void concluir(Usuario autor) {
		exigirAtivo();
		if (situacao != SituacaoTarefaTrabalho.PENDENTE && situacao != SituacaoTarefaTrabalho.EM_ANDAMENTO) {
			throw new TrabalhoStatusInvalidoException("Tarefa nao pode ser concluida a partir da situacao atual");
		}
		this.situacao = SituacaoTarefaTrabalho.CONCLUIDA;
		this.dataConclusao = OffsetDateTime.now();
		registrarAlteracao(autor);
	}

	public void reabrir(Usuario autor) {
		exigirAtivo();
		if (situacao != SituacaoTarefaTrabalho.CONCLUIDA && situacao != SituacaoTarefaTrabalho.CANCELADA) {
			throw new TrabalhoStatusInvalidoException("Tarefa nao pode ser reaberta a partir da situacao atual");
		}
		this.situacao = SituacaoTarefaTrabalho.EM_ANDAMENTO;
		this.dataConclusao = null;
		registrarAlteracao(autor);
	}

	public void cancelar(Usuario autor) {
		exigirAtivo();
		if (situacao == SituacaoTarefaTrabalho.CONCLUIDA || situacao == SituacaoTarefaTrabalho.CANCELADA) {
			throw new TrabalhoStatusInvalidoException("Tarefa nao pode ser cancelada a partir da situacao atual");
		}
		this.situacao = SituacaoTarefaTrabalho.CANCELADA;
		registrarAlteracao(autor);
	}

	public void inativar(Usuario autor) {
		if (status == StatusCadastro.INATIVO) {
			throw new TrabalhoStatusInvalidoException("Tarefa ja esta inativa");
		}
		this.status = StatusCadastro.INATIVO;
		registrarAlteracao(autor);
	}

	public boolean estaAtiva() {
		return status == StatusCadastro.ATIVO;
	}

	public boolean estaAtrasada() {
		return prazo != null && dataConclusao == null
				&& situacao != SituacaoTarefaTrabalho.CONCLUIDA && situacao != SituacaoTarefaTrabalho.CANCELADA
				&& prazo.isBefore(LocalDate.now());
	}

	private void exigirAtivo() {
		if (status != StatusCadastro.ATIVO) {
			throw new TrabalhoStatusInvalidoException("Tarefa inativa nao pode ser alterada");
		}
	}

	private void registrarAlteracao(Usuario autor) {
		this.atualizadoPor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
		this.atualizadoEm = OffsetDateTime.now();
	}
}
