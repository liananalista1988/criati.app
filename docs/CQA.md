# Criati Quality Assurance — CQA

## Objetivo

O Criati Quality Assurance (CQA) é o processo de qualidade aplicado às contribuições da Criati.app. Ele combina validação automática, revisão humana e critérios proporcionais ao risco para proteger a arquitetura modular, a segurança e o isolamento multiempresa.

O CQA não substitui as decisões registradas em `AGENTS.md` e nos demais documentos do projeto. Ele organiza evidências para que uma mudança possa ser revisada e aprovada com segurança.

## Princípios

- escopo pequeno, explícito e rastreável;
- segurança e privacidade desde o início;
- isolamento por empresa validado no backend;
- automação para verificações reproduzíveis;
- revisão humana antes de alterações na `main`;
- documentação consistente com o comportamento;
- nenhuma credencial ou dado real de cliente no Git;
- testes proporcionais ao risco da mudança.

## Fluxo de qualidade

1. **Preparação:** ler as regras do projeto, atualizar a `main` e criar uma branch dedicada.
2. **Implementação:** alterar somente o escopo aprovado e preservar decisões arquiteturais.
3. **Verificação local:** revisar o diff, validar arquivos de configuração e executar `clean verify`.
4. **Pull Request:** registrar objetivo, alterações, validação, testes e riscos.
5. **CI:** confirmar que o workflow executa o build e os testes com Java 17 e Maven Wrapper.
6. **Revisão humana:** avaliar correção, segurança, manutenção, documentação e impacto multiempresa.
7. **Conclusão:** realizar o merge somente após aprovações e verificações exigidas.

## Níveis de risco

| Nível | Exemplos | Evidência mínima |
|---|---|---|
| Baixo | Documentação, templates e textos sem comportamento | Revisão do diff, links e formatação |
| Médio | Configuração, interface ou refatoração localizada | Build completo e testes afetados |
| Alto | Autenticação, permissões, banco, arquivos ou regras de negócio | Testes automatizados positivos e negativos, revisão de segurança |
| Crítico | Isolamento multiempresa, migrations destrutivas ou dados sensíveis | Revisão especializada, plano de reversão e validação explícita entre empresas |

## Critérios obrigatórios

### Escopo e arquitetura

- a mudança corresponde ao objetivo declarado;
- não existem alterações oportunistas ou dependências sem justificativa;
- arquitetura, tecnologias e decisões aprovadas foram preservadas;
- documentos existentes não foram removidos ou renomeados sem aprovação.

### Segurança e multiempresa

- nenhuma entrada do navegador é tratada como contexto empresarial confiável;
- consultas empresariais combinam o recurso com a empresa atual;
- permissões e módulos ativos são verificados no backend;
- logs, testes e exemplos não contêm segredos ou dados reais;
- mudanças relevantes incluem casos negativos de acesso indevido.

### Código e dados

- controllers, services e repositories mantêm suas responsabilidades;
- validações importantes existem no backend;
- migrations são novas, imutáveis e revisadas quanto à integridade e preservação de dados;
- erros não expõem detalhes internos ou dados de outra empresa.

### Testes e build

O comando padrão de validação é:

```bash
./mvnw clean verify
```

No Windows, o equivalente é:

```cmd
mvnw.cmd clean verify
```

O Pull Request deve registrar o comando executado, o resultado, a quantidade de testes e qualquer limitação do ambiente. Falhas não podem ser omitidas.

### Documentação

- README, guias e decisões permanecem consistentes;
- novas configurações e variáveis de ambiente estão documentadas;
- mudanças estruturais possuem aprovação e registro correspondente;
- instruções de validação podem ser reproduzidas por outra pessoa.

## Checklist para Pull Requests

- [ ] Objetivo e escopo estão claros.
- [ ] O diff contém somente alterações relacionadas.
- [ ] Arquitetura e decisões do projeto foram respeitadas.
- [ ] Segurança e privacidade foram consideradas.
- [ ] Isolamento multiempresa foi testado quando aplicável.
- [ ] Nenhuma dependência desnecessária foi adicionada.
- [ ] Nenhum segredo ou dado sensível foi incluído.
- [ ] Build e testes relevantes foram executados.
- [ ] Arquivos YAML e configurações foram validados.
- [ ] Documentação foi atualizada quando necessário.
- [ ] Riscos e limitações estão declarados.
- [ ] O Pull Request aguarda revisão humana e não fará merge automático.

## Evidências e aprovação

A aprovação CQA é baseada nas evidências do Pull Request e do CI. Uma verificação automática aprovada não elimina a necessidade de revisão humana, e uma revisão humana não autoriza ignorar falhas automáticas sem justificativa explícita.

Pendências conhecidas devem permanecer visíveis no Pull Request. Quando uma mudança não puder ser validada integralmente, o risco e o plano de validação complementar devem ser registrados antes do merge.
