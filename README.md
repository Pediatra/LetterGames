# LetterGames Quarkus — Backlog de Jogos

Versão enxuta em **Quarkus 3.15.7** e **Java 21** do projeto final **LetterGames** (BE-JV-010), desenvolvida para o módulo **Introdução ao Quarkus (PE-JV-001)**.

## Problema e Domínio

Jogadores acumulam títulos em bibliotecas digitais sem acompanhar o que já iniciaram, abandonaram ou quantas horas têm pendentes. O **LetterGames** resolve isso mantendo um catálogo central de jogos e o backlog pessoal de cada jogador (`QUERO_JOGAR`, `JOGANDO`, `ZERADO`, `ABANDONADO`), controlando transições válidas de estado, limite de jogos simultâneos e cálculo consolidado de horas pendentes e concluídas.

## Grupo (3 Integrantes)

| Integrante | Papel |
|---|---|
| **João Pedro Guimarães dos Santos Santana** | Arquiteto & Integração/Resiliência |
| **Marcos Emanuel Carvalho dos S D Farias** | Dev APIs REST & Persistência Panache |
| **Gabriel Lustosa Queiroz** | Dev Segurança, Observabilidade & Testes |

## Versões Declaradas

- **Quarkus:** `3.15.7` (`io.quarkus.platform:quarkus-bom`)
- **Java:** `21`
- **Build:** Maven multi-módulo (`./mvnw`) sem dependência de Docker (H2 em memória)

---

## Porte do BE-JV-010 (Spring Boot → Quarkus)

### O que mudou e por quê

1. **Remoção de mensageria, cache e contratos assíncronos:** Kafka, Transactional Outbox, `estatisticas-service`, cache Redis, `shared-contracts`/Pact e infraestrutura Docker/PostgreSQL ficaram de fora, conforme o escopo do PE-JV-001.
2. **Estatísticas síncronas via Panache:** Em vez de projeções assíncronas por eventos Kafka, o `backlog-service` calcula resumos individuais (`GET /usuarios/{usuarioId}/backlog/resumo`) e visão administrativa (`GET /admin/backlogs/estatisticas`) diretamente com consultas agregadas JPQL em `ItemBacklogRepository`.
3. **Dois microsserviços e portas:** O `backlog-service` passou da porta `8082` para `8080`, consumindo o `catalogo-service` na porta `8081` por `REST Client` síncrono com SmallRye Fault Tolerance.
4. **Segurança e posse por usuário:** Adicionamos autenticação em banco com `quarkus-security-jpa` + BCrypt (`BcryptUtil`) e autorização por papel (`ADMIN`, `JOGADOR`) combinada com regra de posse em `AutorizadorBacklog`.

### Comparativo Técnico: Spring Boot vs Quarkus

| Aspecto | Spring Boot (BE-JV-010) | Quarkus 3.15.7 (PE-JV-001) |
|---|---|---|
| Injeção de dependências | Spring IoC / `@Service` / `@Autowired` | CDI ArC (`@ApplicationScoped`, `@Inject`, `@Startup`) |
| Configuração e perfis | `application.yml` + `spring.profiles.active` | `application.properties` + prefixos `%dev.`, `%test.`, padrão `prod` |
| Persistência | Spring Data JPA (`JpaRepository`) + Flyway | Hibernate ORM com Panache (`PanacheRepository`) + H2 |
| Chamada HTTP entre serviços | `RestClient` imperativo | Interface declarativa `@RegisterRestClient` + `ClientRequestFilter` + `ResponseExceptionMapper` |
| Tolerância a falhas | Resilience4j (`@Retry`, `@CircuitBreaker`) | SmallRye Fault Tolerance (`@Timeout`, `@Retry`, `@CircuitBreaker`, `@Fallback`) |
| Tratamento de erros | `@RestControllerAdvice` | `@Provider` `ExceptionMapper` e `@ServerExceptionMapper` com `ErroPadrao` único |
| Segurança | Não havia autenticação | `quarkus-security-jpa` (`@UserDefinition`, BCrypt) + `@RolesAllowed` |
| Testes | `@SpringBootTest` + `MockMvc` + `HttpServer` | `@QuarkusTest` + REST Assured + `@InjectMock @RestClient` + `QuarkusTestResourceLifecycleManager` |

---

## Arquitetura e Modelagem REST

```text
                       HTTP Basic + REST Client (Timeout / Retry / CircuitBreaker / Fallback)
[Cliente] ──► [backlog-service :8080] ──────────────────────────────────────────────────────► [catalogo-service :8081]
                   │ H2 em memória (backlog)                                                       │ H2 em memória (catalogo)
```

Mais detalhes e diagramas Mermaid: [docs/arquitetura.md](docs/arquitetura.md) · Auto-avaliação: [AVALIACAO.md](AVALIACAO.md).

### Justificativa da Modelagem REST (Nível 2 de Richardson)

- **Recursos por substantivo e escopo:** `/jogos` e `/jogos/{id}` no catálogo; `/usuarios/{usuarioId}/backlog`, `/usuarios/{usuarioId}/backlog/{itemId}`, `/resumo`, `/detalhes` e `/admin/backlogs/estatisticas` no backlog.
- **Verbos e códigos HTTP semânticos:** `POST` cria e retorna `201 Created` com header `Location`; `GET` retorna `200 OK`; `PUT`/`PATCH` retornam `200 OK`; `DELETE` retorna `204 No Content`. Erros usam `400` (validação/JSON), `401` (sem credencial/senha inválida), `403` (papel ou posse), `404` (ausente), `409` (duplicidade), `422` (regra de domínio ou jogo inexistente), `502` (recusa do upstream) e `503` (catálogo indisponível), sempre no formato único `ErroPadrao`.

### O que o usuário vê quando o `catalogo-service` cai

- **Ao adicionar um novo jogo (`POST /usuarios/{usuarioId}/backlog`):** após esgotar timeout e retries (ou imediatamente com o disjuntor aberto), o fallback impede gravar um item sem validar o `jogoId` e responde `503 Service Unavailable` com código `CATALOGO_INDISPONIVEL`.
- **Ao consultar detalhes enriquecidos (`GET /usuarios/{usuarioId}/backlog/{itemId}/detalhes`):** o fallback degrada graciosamente devolvendo `200 OK` com `catalogoDisponivel: false`, os dados do *snapshot* local (`titulo` e `duracaoMediaHoras`) e o campo `aviso`.
- **Demais operações (`GET` lista, `/resumo`, `PATCH` status, `DELETE`):** continuam funcionando normalmente usando os dados locais.

---

## Usuários e Papéis Fictícios de Teste

Carregados automaticamente em `%dev` e `%test` (`lettergames.seed.habilitado=true`) com hash BCrypt gerado no banco; **em produção (`lettergames.seed.habilitado=false`), nenhum usuário ou jogo de exemplo é inserido**.

| Usuário | Senha | Papel | Onde atua |
|---|---|---|---|
| `admin` | `admin123` | `ADMIN` | Cadastra/atualiza/remove jogos no catálogo; lê backlogs e `/admin/backlogs/estatisticas` |
| `joao` | `joao123` | `JOGADOR` | Lê catálogo; gerencia exclusivamente `/usuarios/joao/backlog/**` |
| `maria` | `maria123` | `JOGADOR` | Lê catálogo; gerencia exclusivamente `/usuarios/maria/backlog/**` |
| `sistema-backlog` | `sistema123` | `JOGADOR` | Conta técnica usada pelo `backlog-service` para consultar `GET /jogos/{id}` (menor privilégio) |

---

## Como Rodar (Sem Docker)

Pré-requisito: **JDK 21** instalado na máquina (o Maven é baixado pelo wrapper `./mvnw`).

### 1. Subir os dois serviços em modo desenvolvimento (`%dev`)

Em um primeiro terminal, suba o `catalogo-service` na porta `8081`:

```bash
./mvnw quarkus:dev -pl catalogo-service
```

Em um segundo terminal, suba o `backlog-service` na porta `8080`:

```bash
./mvnw quarkus:dev -pl backlog-service
```

Alternativa empacotando primeiro e executando os JARs diretamente com perfil `dev`:

```bash
./mvnw package -DskipTests
java -Dquarkus.profile=dev -jar catalogo-service/target/quarkus-app/quarkus-run.jar
java -Dquarkus.profile=dev -jar backlog-service/target/quarkus-app/quarkus-run.jar
```

### 2. Alterar parâmetros sem recompilar (variáveis de ambiente)

Todas as regras parametrizáveis são externalizadas em `application.properties`:

```bash
BACKLOG_LIMITE_JOGANDO=5 CATALOGO_URL=http://127.0.0.1:8081 java -Dquarkus.profile=dev -jar backlog-service/target/quarkus-app/quarkus-run.jar
CATALOGO_DURACAO_MAXIMA_HORAS=200 java -Dquarkus.profile=dev -jar catalogo-service/target/quarkus-app/quarkus-run.jar
```

### 3. Rodar todos os testes (`mvn verify`)

```bash
./mvnw clean verify
```

---

## Roteiro de Demonstração (`curl`)

Com os dois serviços em pé, execute o script completo ou importe a coleção no Postman:

```bash
./scripts/demo-quarkus.sh
```

- **Coleção enxuta para a Banca (12 requests):** [docs/postman/LetterGames-Banca-Demo.postman_collection.json](docs/postman/LetterGames-Banca-Demo.postman_collection.json)
- **Coleção Postman completa (todos os endpoints):** [docs/postman/LetterGames-Quarkus.postman_collection.json](docs/postman/LetterGames-Quarkus.postman_collection.json)
- **Roteiro textual com payloads e scripts:** [docs/postman/ROTEIRO-POSTMAN.md](docs/postman/ROTEIRO-POSTMAN.md)

Ou rode os passos manualmente:

```bash
# 1. Health checks públicos
curl -s http://127.0.0.1:8081/q/health/ready
curl -s http://127.0.0.1:8080/q/health/ready

# 2. Sem credenciais -> 401 NAO_AUTENTICADO
curl -i -s http://127.0.0.1:8081/jogos

# 3. Papel JOGADOR tentando criar jogo no catálogo -> 403 ACESSO_NEGADO
curl -i -s -u joao:joao123 -X POST http://127.0.0.1:8081/jogos \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Teste","plataforma":"PC","genero":"Indie","duracaoMediaHoras":5}'

# 4. ADMIN cadastra jogo -> 201 Created + Location
curl -i -s -u admin:admin123 -X POST http://127.0.0.1:8081/jogos \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Sea of Stars","plataforma":"PC","genero":"RPG","duracaoMediaHoras":30}'

# 5. joao adiciona jogo 1 (Hollow Knight) ao próprio backlog -> 201 Created
curl -i -s -u joao:joao123 -X POST http://127.0.0.1:8080/usuarios/joao/backlog \
  -H "Content-Type: application/json" -d '{"jogoId":1}'

# 6. Evolui status e consulta resumo e detalhes
curl -s -u joao:joao123 -X PATCH http://127.0.0.1:8080/usuarios/joao/backlog/1 \
  -H "Content-Type: application/json" -d '{"status":"JOGANDO"}'
curl -s -u joao:joao123 http://127.0.0.1:8080/usuarios/joao/backlog/resumo
curl -s -u joao:joao123 http://127.0.0.1:8080/usuarios/joao/backlog/1/detalhes

# 7. Regra de posse: joao tentando acessar backlog de maria -> 403 ACESSO_NEGADO
curl -i -s -u joao:joao123 http://127.0.0.1:8080/usuarios/maria/backlog

# 8. Erro de negócio: jogo inexistente no catálogo -> 422 JOGO_INEXISTENTE
curl -i -s -u joao:joao123 -X POST http://127.0.0.1:8080/usuarios/joao/backlog \
  -H "Content-Type: application/json" -d '{"jogoId":999999}'

# 9. Métricas e OpenAPI
curl -s http://127.0.0.1:8080/q/metrics | grep lettergames_
curl -s http://127.0.0.1:8081/q/openapi | head -n 30
```

---

## Uso Crítico de IA

Utilizamos IA para acelerar a estruturação do porte Spring Boot → Quarkus 3.15.7 e validamos cada saída manualmente via compilação, leitura de logs e execução real. Os erros detectados e corrigidos (como `@Schema(examples=...)` incompatível com Quarkus 3.15.7, `quarkus.http.test-port=0` nos logs, janela deslizante do `@CircuitBreaker` e assinatura de `QuarkusTransaction`) estão documentados em [docs/ia/registro-uso-ia.md](docs/ia/registro-uso-ia.md).
