# Contribuindo com a Criati.app

Obrigado pelo interesse em contribuir. A Criati.app é uma plataforma SaaS modular e multiempresa; segurança, isolamento de dados e simplicidade arquitetural são requisitos de todas as mudanças.

## Antes de começar

1. Leia `AGENTS.md` e os documentos da pasta `docs`.
2. Confirme se já existe uma issue ou Pull Request relacionado.
3. Discuta previamente mudanças de arquitetura, escopo ou dependências estruturais.
4. Nunca inclua credenciais, dados reais de clientes ou outras informações sensíveis.

## Fluxo de desenvolvimento

1. Atualize a branch `main`.
2. Crie uma branch descritiva a partir da `main` atualizada.
3. Implemente somente o escopo aprovado.
4. Faça commits pequenos no padrão Conventional Commits.
5. Execute o build e os testes relevantes.
6. Abra um Pull Request para `main` e aguarde revisão humana.

Exemplos de branches:

```text
feat/cadastro-empresa
fix/isolamento-tarefa
docs/seguranca
chore/pipeline-ci
```

Exemplos de commits:

```text
feat(empresa): adiciona cadastro inicial
fix(tarefa): valida empresa ao buscar registro
docs: documenta fluxo de contribuição
chore(ci): configura verificação do Maven
```

## Qualidade e testes

No Windows:

```cmd
mvnw.cmd clean verify
```

No Linux ou macOS:

```bash
./mvnw clean verify
```

Consulte o [Criati Quality Assurance](docs/CQA.md) para os critérios de revisão e validação. Mudanças em dados empresariais devem incluir testes proporcionais ao risco e comprovar o isolamento entre empresas.

## Pull Requests

O Pull Request deve apresentar:

- objetivo e contexto;
- alterações realizadas;
- instruções de validação;
- resultado dos testes;
- riscos e limitações;
- checklist CQA preenchido.

Mantenha o Pull Request pequeno e focado. Não misture refatorações, atualizações de dependências ou alterações de negócio sem relação com o objetivo principal.

## Revisão

Todo código enviado para `main` passa por Pull Request e revisão humana. Comentários devem ser objetivos, respeitosos e orientados à segurança, correção e manutenção do produto.

Ao participar, siga também o [Código de Conduta](CODE_OF_CONDUCT.md).
