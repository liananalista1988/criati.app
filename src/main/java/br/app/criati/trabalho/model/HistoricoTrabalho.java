package br.app.criati.trabalho.model;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import br.app.criati.empresa.model.Empresa;
import br.app.criati.shared.enums.TipoEventoHistoricoTrabalho;
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

/**
 * Registro imutavel de historico de processo/tarefa: somente insert, sem
 * atualizado_em nem setters, mesmo padrao de RedefinicaoSenhaAuditoria.
 * Exatamente um entre processo/tarefa e preenchido (ver ck_historico_trabalho_alvo).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "historico_trabalho")
public class HistoricoTrabalho {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", nullable = false, updatable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "empresa_id", nullable = false, updatable = false)
	private Empresa empresa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "processo_id", updatable = false)
	private ProcessoEmpresarial processo;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "tarefa_id", updatable = false)
	private TarefaEmpresarial tarefa;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_evento", nullable = false, length = 30, updatable = false)
	private TipoEventoHistoricoTrabalho tipoEvento;

	@Column(name = "descricao", length = 500, updatable = false)
	private String descricao;

	@Column(name = "valor_anterior", length = 200, updatable = false)
	private String valorAnterior;

	@Column(name = "valor_novo", length = 200, updatable = false)
	private String valorNovo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "autor_usuario_id", updatable = false)
	private Usuario autor;

	@Generated(event = EventType.INSERT)
	@ColumnDefault("CURRENT_TIMESTAMP")
	@Column(name = "ocorrido_em", nullable = false, updatable = false)
	private OffsetDateTime ocorridoEm;

	private HistoricoTrabalho(Empresa empresa, ProcessoEmpresarial processo, TarefaEmpresarial tarefa,
			TipoEventoHistoricoTrabalho tipoEvento, String descricao, String valorAnterior, String valorNovo,
			Usuario autor) {
		this.empresa = Objects.requireNonNull(empresa, "empresa nao pode ser nula");
		this.processo = processo;
		this.tarefa = tarefa;
		this.tipoEvento = Objects.requireNonNull(tipoEvento, "tipoEvento nao pode ser nulo");
		this.descricao = descricao;
		this.valorAnterior = valorAnterior;
		this.valorNovo = valorNovo;
		this.autor = Objects.requireNonNull(autor, "autor nao pode ser nulo");
	}

	public static HistoricoTrabalho doProcesso(ProcessoEmpresarial processo, TipoEventoHistoricoTrabalho tipoEvento,
			String descricao, String valorAnterior, String valorNovo, Usuario autor) {
		Objects.requireNonNull(processo, "processo nao pode ser nulo");
		return new HistoricoTrabalho(processo.getEmpresa(), processo, null, tipoEvento, descricao, valorAnterior,
				valorNovo, autor);
	}

	public static HistoricoTrabalho daTarefa(TarefaEmpresarial tarefa, TipoEventoHistoricoTrabalho tipoEvento,
			String descricao, String valorAnterior, String valorNovo, Usuario autor) {
		Objects.requireNonNull(tarefa, "tarefa nao pode ser nula");
		return new HistoricoTrabalho(tarefa.getEmpresa(), null, tarefa, tipoEvento, descricao, valorAnterior,
				valorNovo, autor);
	}
}
