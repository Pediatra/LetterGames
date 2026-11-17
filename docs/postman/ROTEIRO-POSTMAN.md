# Roteiro e Coleção Postman — LetterGames Quarkus

Este guia reúne a coleção importável ([LetterGames-Quarkus.postman_collection.json](LetterGames-Quarkus.postman_collection.json)) e o passo a passo completo em texto (URLs, credenciais HTTP Basic, headers, payloads JSON e scripts `Post-response`) para validar **todos os endpoints** do `catalogo-service` (`http://localhost:8081`) e do `backlog-service` (`http://localhost:8080`).

## Como usar no Postman

1. Suba os dois serviços em modo `dev` (para carregar os usuários fictícios e o catálogo inicial):
   - `catalogo-service` na porta `8081`
   - `backlog-service` na porta `8080`
2. No Postman, clique em **Import** e selecione o arquivo `docs/postman/LetterGames-Quarkus.postman_collection.json`.
3. Para rodar toda a suíte automaticamente, abra a pasta **1**, **2** e **3** no **Collection Runner** (mantenha desmarcada a pasta **4. Manual - Derrubar catalogo-service**, pois ela exige parar o `catalogo-service` manualmente).
4. A coleção cria seus próprios registros de teste e os remove ao final da pasta 3, permitindo rodar o Runner várias vezes seguidas sem conflito `409`.

---

## Variáveis da Coleção

| Variável | Valor inicial | Uso |
|---|---|---|
| `catalogoUrl` | `http://localhost:8081` | Base URL do `catalogo-service` |
| `backlogUrl` | `http://localhost:8080` | Base URL do `backlog-service` |
| `novoJogoId` | *(preenchida via script)* | ID do jogo criado pelo `ADMIN` |
| `itemBacklogId` | *(preenchida via script)* | ID do item principal no backlog de `joao` |
| `itemParaRemoverId` | *(preenchida via script)* | ID do 2º item adicionado ao backlog de `joao` |

---

## Lista de Requests, Payloads e Scripts (`Post-response`)

### Pasta 1 — Observabilidade e OpenAPI (Auth: `No Auth`)

1. `GET {{catalogoUrl}}/q/health/live` → `200` (`status == "UP"`)
2. `GET {{catalogoUrl}}/q/health/ready` → `200` (`status == "UP"`)
3. `GET {{backlogUrl}}/q/health/live` → `200` (`status == "UP"`)
4. `GET {{backlogUrl}}/q/health/ready` → `200` (`status == "UP"`)
5. `GET {{catalogoUrl}}/q/openapi` (`Header: Accept: application/json`) → `200`
6. `GET {{backlogUrl}}/q/openapi` (`Header: Accept: application/json`) → `200`

Script `Post-response` padrão para os Health Checks:
```javascript
pm.test("Status 200 e UP", function () {
    pm.response.to.have.status(200);
    pm.expect(pm.response.json().status).to.eql("UP");
});
```

---

### Pasta 2 — `catalogo-service` (`:8081`)

1. **401 Sem Credenciais** — `GET {{catalogoUrl}}/jogos` · Auth: `No Auth` → `401` (`NAO_AUTENTICADO`)
2. **403 Papel Errado** — `POST {{catalogoUrl}}/jogos` · Auth: Basic `joao` / `joao123` · `Content-Type: application/json`
   ```json
   {
     "titulo": "Tentativa Jogador",
     "plataforma": "PC",
     "genero": "Indie",
     "duracaoMediaHoras": 8
   }
   ```
   → `403` (`ACESSO_NEGADO`)
3. **201 Criar Jogo (`ADMIN`)** — `POST {{catalogoUrl}}/jogos` · Auth: Basic `admin` / `admin123` · `Content-Type: application/json`
   ```json
   {
     "titulo": "Sea of Stars Postman",
     "plataforma": "PC",
     "genero": "RPG",
     "duracaoMediaHoras": 30
   }
   ```
   Script `Post-response`:
   ```javascript
   pm.test("Status 201 com Location e ID salvo", function () {
       pm.response.to.have.status(201);
       pm.response.to.have.header("Location");
       const json = pm.response.json();
       pm.expect(json.id).to.be.a("number");
       pm.collectionVariables.set("novoJogoId", json.id);
   });
   ```
4. **400 Validação** — `POST {{catalogoUrl}}/jogos` · Auth: Basic `admin` / `admin123`
   ```json
   {
     "titulo": " ",
     "plataforma": "PC",
     "genero": "RPG",
     "duracaoMediaHoras": -5
   }
   ```
   → `400` (`ERRO_VALIDACAO`)
5. **409 Duplicado** — `POST {{catalogoUrl}}/jogos` · Auth: Basic `admin` / `admin123`
   ```json
   {
     "titulo": "hollow knight",
     "plataforma": "pc",
     "genero": "Metroidvania",
     "duracaoMediaHoras": 27
   }
   ```
   → `409` (`JOGO_DUPLICADO`)
6. **422 Duração Acima do Limite** — `POST {{catalogoUrl}}/jogos` · Auth: Basic `admin` / `admin123`
   ```json
   {
     "titulo": "Campanha Gigante",
     "plataforma": "PC",
     "genero": "MMO",
     "duracaoMediaHoras": 600
   }
   ```
   → `422` (`DURACAO_ACIMA_DO_LIMITE`)
7. **200 Listar com Filtros** — `GET {{catalogoUrl}}/jogos?plataforma=PC&genero=RPG&termo=witcher` · Auth: Basic `joao` / `joao123` → `200` (`The Witcher 3`)
8. **200 Buscar por ID** — `GET {{catalogoUrl}}/jogos/{{novoJogoId}}` · Auth: Basic `joao` / `joao123` → `200`
9. **404 Jogo Inexistente** — `GET {{catalogoUrl}}/jogos/999999` · Auth: Basic `joao` / `joao123` → `404` (`JOGO_NAO_ENCONTRADO`)
10. **200 Atualizar Jogo (`ADMIN`)** — `PUT {{catalogoUrl}}/jogos/{{novoJogoId}}` · Auth: Basic `admin` / `admin123`
    ```json
    {
      "titulo": "Sea of Stars: Throes of the Watchmaker",
      "plataforma": "Switch",
      "genero": "RPG",
      "duracaoMediaHoras": 34
    }
    ```
    → `200`

---

### Pasta 3 — `backlog-service` (`:8080`), Limpeza e Métricas

1. **401 Sem Credenciais** — `GET {{backlogUrl}}/usuarios/joao/backlog` · Auth: `No Auth` → `401` (`NAO_AUTENTICADO`)
2. **201 Adicionar Jogo Criado (REST Client → Catálogo)** — `POST {{backlogUrl}}/usuarios/joao/backlog` · Auth: Basic `joao` / `joao123`
   ```json
   {
     "jogoId": {{novoJogoId}}
   }
   ```
   Script `Post-response`:
   ```javascript
   pm.test("Status 201, Location e snapshot gravado", function () {
       pm.response.to.have.status(201);
       pm.response.to.have.header("Location");
       const json = pm.response.json();
       pm.expect(json.status).to.eql("QUERO_JOGAR");
       pm.collectionVariables.set("itemBacklogId", json.id);
   });
   ```
3. **201 Adicionar Jogo 3 (Celeste)** — `POST {{backlogUrl}}/usuarios/joao/backlog` · Auth: Basic `joao` / `joao123`
   ```json
   {
     "jogoId": 3
   }
   ```
   Salva `itemParaRemoverId`.
4. **409 Duplicado no Backlog** — `POST {{backlogUrl}}/usuarios/joao/backlog` com `{"jogoId": 3}` · Auth: Basic `joao` / `joao123` → `409` (`JOGO_JA_NO_BACKLOG`)
5. **422 Jogo Inexistente no Catálogo** — `POST {{backlogUrl}}/usuarios/joao/backlog` com `{"jogoId": 999999}` · Auth: Basic `joao` / `joao123` → `422` (`JOGO_INEXISTENTE`)
6. **400 Validação (`jogoId` negativo)** — `POST {{backlogUrl}}/usuarios/joao/backlog` com `{"jogoId": -1}` · Auth: Basic `joao` / `joao123` → `400` (`ERRO_VALIDACAO`)
7. **422 Transição Inválida (`QUERO_JOGAR` → `ZERADO`)** — `PATCH {{backlogUrl}}/usuarios/joao/backlog/{{itemBacklogId}}` com `{"status": "ZERADO"}` · Auth: Basic `joao` / `joao123` → `422` (`TRANSICAO_INVALIDA`)
8. **200 Alterar Status → `JOGANDO`** — `PATCH {{backlogUrl}}/usuarios/joao/backlog/{{itemBacklogId}}` com `{"status": "JOGANDO"}` · Auth: Basic `joao` / `joao123` → `200`
9. **200 Alterar Status → `ZERADO`** — `PATCH {{backlogUrl}}/usuarios/joao/backlog/{{itemBacklogId}}` com `{"status": "ZERADO"}` · Auth: Basic `joao` / `joao123` → `200`
10. **200 Listar por Status** — `GET {{backlogUrl}}/usuarios/joao/backlog?status=ZERADO` · Auth: Basic `joao` / `joao123` → `200`
11. **200 Buscar Item por ID** — `GET {{backlogUrl}}/usuarios/joao/backlog/{{itemBacklogId}}` · Auth: Basic `joao` / `joao123` → `200`
12. **404 Item Inexistente** — `GET {{backlogUrl}}/usuarios/joao/backlog/999999` · Auth: Basic `joao` / `joao123` → `404` (`ITEM_NAO_ENCONTRADO`)
13. **200 Detalhes Enriquecidos** — `GET {{backlogUrl}}/usuarios/joao/backlog/{{itemBacklogId}}/detalhes` · Auth: Basic `joao` / `joao123` → `200` (`catalogoDisponivel: true`)
14. **200 Resumo do Jogador** — `GET {{backlogUrl}}/usuarios/joao/backlog/resumo` · Auth: Basic `joao` / `joao123` → `200`
15. **403 Regra de Posse** — `GET {{backlogUrl}}/usuarios/maria/backlog` · Auth: Basic `joao` / `joao123` → `403` (`ACESSO_NEGADO`)
16. **403 `JOGADOR` em Rota `ADMIN`** — `GET {{backlogUrl}}/admin/backlogs/estatisticas` · Auth: Basic `joao` / `joao123` → `403` (`ACESSO_NEGADO`)
17. **200 Estatísticas Globais (`ADMIN`)** — `GET {{backlogUrl}}/admin/backlogs/estatisticas` · Auth: Basic `admin` / `admin123` → `200`
18. **204 Remover Item 1** — `DELETE {{backlogUrl}}/usuarios/joao/backlog/{{itemBacklogId}}` · Auth: Basic `joao` / `joao123` → `204`
19. **204 Remover Item 2** — `DELETE {{backlogUrl}}/usuarios/joao/backlog/{{itemParaRemoverId}}` · Auth: Basic `joao` / `joao123` → `204`
20. **204 Remover Jogo de Teste** — `DELETE {{catalogoUrl}}/jogos/{{novoJogoId}}` · Auth: Basic `admin` / `admin123` → `204`
21. **200 Métricas Catálogo** — `GET {{catalogoUrl}}/q/metrics` · Auth: `No Auth` → `200`
22. **200 Métricas Backlog** — `GET {{backlogUrl}}/q/metrics` · Auth: `No Auth` → `200`

---

### Pasta 4 — Teste Manual de Queda do `catalogo-service` (Fault Tolerance)

1. Com os dois serviços ligados, envie `POST {{backlogUrl}}/usuarios/joao/backlog` (`joao:joao123`) com `{"jogoId": 1}` → `201` (salva `itemBacklogId`).
2. Pare o processo do `catalogo-service` (`Ctrl+C`).
3. Envie `POST {{backlogUrl}}/usuarios/joao/backlog` (`joao:joao123`) com `{"jogoId": 2}` → **`503 CATALOGO_INDISPONIVEL`**.
4. Envie `GET {{backlogUrl}}/usuarios/joao/backlog/{{itemBacklogId}}/detalhes` (`joao:joao123`) → **`200 OK`** com `catalogoDisponivel: false` e o snapshot local.
