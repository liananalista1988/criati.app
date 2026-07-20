package br.app.criati.convite.service;

import br.app.criati.convite.model.Convite;

// Abstracao de entrega do convite. Nao ha integracao de e-mail nesta fase;
// permite plugar uma implementacao real (e-mail) depois sem alterar
// ConviteService.
public interface ConviteNotificador {

	void notificar(Convite convite, String tokenBruto);
}
