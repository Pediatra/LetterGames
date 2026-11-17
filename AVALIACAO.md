# Auto-avaliação — LetterGames Quarkus

Grupo: João Pedro Guimarães dos Santos Santana (Arquiteto & Integração/Resiliência) · Marcos Emanuel Carvalho dos S D Farias (Dev APIs REST & Persistência Panache) · Gabriel Lustosa Queiroz (Dev Segurança, Observabilidade & Testes)
Tema/domínio: Backlog de jogos portado do BE-JV-010 (Spring Boot) para Quarkus 3.15.7 sem mensageria ou cache. O jogador organiza jogos validados em catálogo por status, respeita regras de transição e limite simultâneo, e consulta resumo consolidado de horas pendentes e zeradas.
Versões: Quarkus 3.15.7 · Java 21

## Evidências por critério

1. **Serviços e API REST — nível auto-atribuído: 3**
   Evidência: Projeto Maven multi-módulo ([pom.xml](pom.xml)) com dois serviços independentes usando CDI (`@ApplicationScoped`, `@Inject`): [catalogo-service](catalogo-service/pom.xml) (porta `8081`) e [backlog-service](backlog-service/pom.xml) (porta `8080`). Recursos REST no nível 2 de maturidade em `JogoResource` (201 com `Location`, 200, 204, 404, 409, 422), `BacklogResource` e `AdminBacklogResource`. Contratos OpenAPI anotados com `@OpenAPIDefinition`, `@SecurityScheme`, `@Operation`, `@APIResponse` e `@Schema` (`CatalogoOpenApiConfig`, `BacklogOpenApiConfig`) testados em `JogoResourceTest#deveExporContratoOpenApiComRotasDeJogos` e `BacklogResourceTest#deveExporEstatisticasGlobaisParaAdminEContratoOpenApi`. Justificativa da modelagem REST no [README.md](README.md#justificativa-da-modelagem-rest-nível-2-de-richardson) e em [docs/arquitetura.md](docs/arquitetura.md).

2. **Integração entre serviços e tolerância a falhas — nível auto-atribuído: 3**
   Evidência: Cliente declarativo `@RegisterRestClient(configKey = "catalogo-api")` em `CatalogoRestClient` com `CatalogoBasicAuthFilter` e `CatalogoClientExceptionMapper`; resiliência isolada no bean CDI `CatalogoIntegracaoService` (`@Timeout`, `@Retry` apenas sobre `GET` idempotente com `abortOn` para 404/4xx e `CircuitBreakerOpenException`, `@CircuitBreaker` com `requestVolumeThreshold = 6 > maxRetries + 1`, `@Fallback` distinto para escrita `503` vs leitura degradada `200` com snapshot local). Parâmetros externalizados por método em `backlog-service/src/main/resources/application.properties` e explicados em [docs/arquitetura.md](docs/arquitetura.md) e no [README.md](README.md#o-que-o-usuário-vê-quando-o-catalogo-service-cai). Coberto por `CatalogoResilienciaTest` (5 testes com `@InjectMock @RestClient` e `CircuitBreakerMaintenance`) e `CatalogoRestClientWireTest` (2 testes sobre HTTP real com `HttpServer` local).

3. **Persistência com Panache — nível auto-atribuído: 3**
   Evidência: Entidades `Jogo`, `ItemBacklog`, `UsuarioCatalogo` e `UsuarioBacklog` com `GenerationType.IDENTITY` e repositórios `PanacheRepository` (`JogoRepository`, `ItemBacklogRepository`) escolhidos de forma justificada em [docs/arquitetura.md](docs/arquitetura.md#decisões-de-persistência-panache-e-segurança). Consultas além do CRUD: filtro dinâmico case-insensitive com ordenação e contagem por plataforma (`JogoRepository`), listagem filtrada por status e duas consultas JPQL de agregação `GROUP BY` com cálculo de horas e percentual (`ItemBacklogRepository#calcularResumoUsuario` e `calcularEstatisticasGerais`). Escritas isoladas em `@Transactional` (`JogoService`, `BacklogTransacionalService`); seed habilitado apenas em `%dev`/`%test` (`InicializadorCatalogo`, `InicializadorBacklog`). Provado por `JogoPanacheTest`, `BacklogPanacheTest`, `CatalogoPerfilProdSemSeedTest` e `BacklogPerfilProdSemSeedTest`.

4. **Validação e tratamento de erros — nível auto-atribuído: 3**
   Evidência: Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`, `@Size`, `@Pattern` contra controles ASCII/Unicode e NEL `\u0085`) em `JogoRequest`, `AdicionarItemRequest`, `AlterarStatusRequest` e `@PathParam("usuarioId")` de `BacklogResource`. Corpo único `ErroPadrao` (status, codigo, mensagem, caminho, timestamp, detalhes) nos dois serviços via `DominioCatalogoExceptionMapper`, `DominioBacklogExceptionMapper`, `ValidacaoExceptionMapper`, `JsonInvalidoExceptionMapper` (`@ServerExceptionMapper` para `JacksonException` e `MismatchedInputException`), `SegurancaExceptionMapper`, `WebApplicationExceptionMapper` e `ErroInesperadoExceptionMapper`, sem vazar stack trace ou SQL. Coberto para todos os códigos de contrato em `CatalogoValidacaoErrosTest` (400, 404, 409, 422) e `BacklogValidacaoErrosTest` (400, 404, 409, 422, 502, 503).

5. **Segurança — nível auto-atribuído: 3**
   Evidência: Autenticação JPA (`quarkus-security-jpa`) com `UsuarioCatalogo` e `UsuarioBacklog` armazenando senhas com `BcryptUtil.bcryptHash(...)` e `quarkus.http.auth.proactive=false`. Papéis distintos (`ADMIN`, `JOGADOR`) via `@RolesAllowed` e conta técnica `sistema-backlog` com papel mínimo `JOGADOR` na integração entre serviços. Regra adicional de posse em `AutorizadorBacklog` garantindo que cada jogador só acessa e altera o próprio backlog (`/usuarios/{usuarioId}/backlog/**`). Provado por `CatalogoSegurancaTest` (4 testes: 401 sem credencial e senha errada, 403 jogador mutando catálogo, sucesso por papel, verificação do prefixo BCrypt `$2`) e `BacklogSegurancaTest` (3 testes: 401, RBAC e bloqueio cruzado `joao` ↔ `maria` com `403 ACESSO_NEGADO`).

6. **Config, health e métricas — nível auto-atribuído: 3**
   Evidência: `application.properties` nos dois serviços com perfis `%dev`, `%test` e padrão (`prod`), parâmetros de negócio e integração via variáveis de ambiente (`CATALOGO_DURACAO_MAXIMA_HORAS`, `BACKLOG_LIMITE_JOGANDO`, `CATALOGO_URL`, `CATALOGO_AUTH_USUARIO`, `CATALOGO_AUTH_SENHA`) documentados no [README.md](README.md#2-alterar-parâmetros-sem-recompilar-variáveis-de-ambiente). Health checks próprios `CatalogoLivenessCheck`, `CatalogoReadinessCheck`, `BacklogLivenessCheck`, `BacklogReadinessCheck` em `/q/health/live` e `/q/health/ready`; métricas Micrometer próprias (`lettergames_catalogo_jogos_criados_total`, `lettergames_catalogo_consultas_jogo_total`, `lettergames_backlog_itens_adicionados_total`, `lettergames_backlog_jogos_zerados_total`, `lettergames_backlog_catalogo_falhas_total`) em `/q/metrics`. Provado por `CatalogoObservabilidadeConfigTest`, `BacklogObservabilidadeConfigTest`, `CatalogoPerfilProdSemSeedTest` e `BacklogPerfilProdSemSeedTest`.

7. **Testes — nível auto-atribuído: 3**
   Evidência: Suíte `@QuarkusTest` independente de ordem (limpeza por teste e `CircuitBreakerMaintenance.resetAll()`) e independente de porta fixa (`quarkus.http.test-port=0`), sem depender do serviço real de catálogo no ar (`@InjectMock @RestClient` e `HttpServer` local em `CatalogoRestClientWireTest`). Contagem verificada em `mvn clean verify`: **39 testes executados (0 falhas, 0 erros, 0 pulados)** — sendo **19 testes em `catalogo-service`** (6 classes em [catalogo-service/src/test/java/br/com/lettergames/catalogo](catalogo-service/src/test/java/br/com/lettergames/catalogo)) e **20 testes em `backlog-service`** (8 classes em [backlog-service/src/test/java/br/com/lettergames/backlog](backlog-service/src/test/java/br/com/lettergames/backlog)), com cobertura de caminhos felizes e de erro para todos os critérios de 1 a 6.

8. **Uso crítico de IA — nível auto-atribuído: 3**
   Como usamos IA e o que validamos manualmente: Registro detalhado em [docs/ia/registro-uso-ia.md](docs/ia/registro-uso-ia.md) e resumo no [README.md](README.md#uso-crítico-de-ia). Documenta 8 achados concretos em que a primeira saída da IA falhou ou gerou código incompatível com Quarkus 3.15.7 (como `@Schema(examples=...)`, `quarkus.http.test-port=0`, `BuiltinMismatchedInputExceptionMapper`, `TimeoutConfigImpl` por método, `QuarkusTransaction.requiringNew().run(...)` e janela do disjuntor), como foram detectados via compilação, leitura de logs, `javap` e testes, e as regras adotadas pelo grupo.

9. **Execução — nível auto-atribuído: 3**
   Como rodar: Instruções reproduzíveis sem Docker no [README.md](README.md#como-rodar-sem-docker) subindo `catalogo-service` na porta `8081` e `backlog-service` na porta `8080`, roteiro de demonstração passo a passo com `curl` e script automatizado [scripts/demo-quarkus.sh](scripts/demo-quarkus.sh) validado contra os dois serviços em execução.

## Opcionais entregues

Nosso grupo possui **3 integrantes**, portanto nenhum item opcional é exigido pela regra do PE-JV-001 (apenas o CORE obrigatório). Ainda assim, para cumprir integralmente os descritores de nível 3 do CORE, entregamos documentação OpenAPI completa com Swagger UI (`/q/openapi` e `/q/swagger-ui`), métricas de negócio próprias no Micrometer (`/q/metrics`) e health checks próprios (`@Liveness` e `@Readiness`).

## Histórico de Commits Relevantes

- `634c213` — `chore: estrutura maven multi-modulo com Quarkus 3.15.7 e Java 21`
- `bd94cc2` — `feat(catalogo): implementa API REST, Panache, seguranca BCrypt e observabilidade`
- `da75929` — `test(catalogo): adiciona suite QuarkusTest cobrindo criterios 1, 3, 4, 5 e 6`
- `5716bc0` — `feat(backlog): implementa backlog com REST Client, Fault Tolerance e RBAC por posse`
- `d841e29` — `test(backlog): adiciona suite QuarkusTest de resiliencia, REST Client, Panache e seguranca`
- Documentação, script de demonstração e auto-avaliação adicionados nos commits seguintes do ramo `main`.
