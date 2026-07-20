package br.app.criati.convite.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import br.app.criati.convite.model.Convite;

// Implementacao temporaria: ainda nao ha envio de e-mail. Registra apenas
// que um convite foi criado, sem nunca incluir o token bruto no log.
@Component
public class ConviteNotificadorTemporario implements ConviteNotificador {

	private static final Logger log = LoggerFactory.getLogger(ConviteNotificadorTemporario.class);

	@Override
	public void notificar(Convite convite, String tokenBruto) {
		log.info(
				"Convite criado (id={}) para a empresa (id={}); entrega por e-mail ainda nao implementada.",
				convite.getId(), convite.getEmpresa().getId());
	}
}
