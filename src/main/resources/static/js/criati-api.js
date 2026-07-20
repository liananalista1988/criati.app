/* Criati - cliente HTTP central: fetch com CSRF, same-origin e tratamento padrao de erros. */
(function (window) {
	"use strict";

	function metaContent(name) {
		var el = document.querySelector('meta[name="' + name + '"]');
		return el ? el.getAttribute("content") : null;
	}

	function isMutating(method) {
		return ["POST", "PUT", "PATCH", "DELETE"].indexOf((method || "GET").toUpperCase()) !== -1;
	}

	function CriatiApiError(status, body, network) {
		this.name = "CriatiApiError";
		this.status = status || 0;
		this.body = body || null;
		this.network = Boolean(network);
		this.message = (body && body.message) || "Erro inesperado.";
	}
	CriatiApiError.prototype = Object.create(Error.prototype);

	function mensagemPorStatus(status) {
		switch (status) {
			case 400:
				return "Verifique os dados informados.";
			case 403:
				return "Voce nao tem acesso a este recurso.";
			case 404:
				return "Recurso nao encontrado.";
			case 409:
				return "Conflito ao processar a solicitacao.";
			default:
				return status >= 500 ? "Erro interno. Tente novamente em instantes." : "Nao foi possivel completar a solicitacao.";
		}
	}

	/**
	 * @param {string} path
	 * @param {object} [options]
	 * @param {string} [options.method]
	 * @param {object} [options.body]
	 * @param {boolean} [options.redirectOn401=true] Redireciona para /login?motivo=sessao em 401.
	 */
	function request(path, options) {
		options = options || {};
		var method = (options.method || "GET").toUpperCase();
		var redirectOn401 = options.redirectOn401 !== false;

		var headers = {
			Accept: "application/json"
		};
		if (options.body !== undefined) {
			headers["Content-Type"] = "application/json";
		}
		if (isMutating(method)) {
			var headerName = metaContent("_csrf_header");
			var token = metaContent("_csrf");
			if (headerName && token) {
				headers[headerName] = token;
			}
		}

		return fetch(path, {
			method: method,
			headers: headers,
			credentials: "same-origin",
			body: options.body !== undefined ? JSON.stringify(options.body) : undefined
		})
			.catch(function () {
				throw new CriatiApiError(0, { message: "Sem conexao com o servidor." }, true);
			})
			.then(function (response) {
				if (response.status === 204) {
					return { status: response.status, data: null };
				}
				var contentType = response.headers.get("content-type") || "";
				var parsePromise = contentType.indexOf("application/json") !== -1
					? response.json().catch(function () {
							return null;
						})
					: Promise.resolve(null);

				return parsePromise.then(function (data) {
					if (response.ok) {
						return { status: response.status, data: data };
					}

					if (response.status === 401 && redirectOn401) {
						window.location.href = "/login?motivo=sessao";
						// Mantem a promise pendente: a navegacao ja foi iniciada.
						return new Promise(function () {});
					}

					var body = data || { message: mensagemPorStatus(response.status) };
					throw new CriatiApiError(response.status, body, false);
				});
			});
	}

	window.CriatiApi = {
		request: request,
		get: function (path, options) {
			return request(path, Object.assign({}, options, { method: "GET" }));
		},
		post: function (path, body, options) {
			return request(path, Object.assign({}, options, { method: "POST", body: body }));
		},
		delete: function (path, options) {
			return request(path, Object.assign({}, options, { method: "DELETE" }));
		},
		Error: CriatiApiError
	};
})(window);
