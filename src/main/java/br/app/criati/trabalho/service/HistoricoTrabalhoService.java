package br.app.criati.trabalho.service;

import org.springframework.stereotype.Service;

import br.app.criati.shared.enums.TipoEventoHistoricoTrabalho;
import br.app.criati.trabalho.model.HistoricoTrabalho;
import br.app.criati.trabalho.model.ProcessoEmpresarial;
import br.app.criati.trabalho.model.TarefaEmpresarial;
import br.app.criati.trabalho.repository.HistoricoTrabalhoRepository;
import br.app.criati.usuario.model.Usuario;

@Service
public class HistoricoTrabalhoService {

	private final HistoricoTrabalhoRepository historicoRepository;

	public HistoricoTrabalhoService(HistoricoTrabalhoRepository historicoRepository) {
		this.historicoRepository = historicoRepository;
	}

	public void registrarProcesso(ProcessoEmpresarial processo, TipoEventoHistoricoTrabalho tipoEvento,
			String descricao, String valorAnterior, String valorNovo, Usuario autor) {
		historicoRepository.save(
				HistoricoTrabalho.doProcesso(processo, tipoEvento, descricao, valorAnterior, valorNovo, autor));
	}

	public void registrarTarefa(TarefaEmpresarial tarefa, TipoEventoHistoricoTrabalho tipoEvento,
			String descricao, String valorAnterior, String valorNovo, Usuario autor) {
		historicoRepository.save(
				HistoricoTrabalho.daTarefa(tarefa, tipoEvento, descricao, valorAnterior, valorNovo, autor));
	}
}
