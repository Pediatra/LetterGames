#!/usr/bin/env bash
set -euo pipefail

CATALOGO_URL="${CATALOGO_URL:-http://127.0.0.1:8081}"
BACKLOG_URL="${BACKLOG_URL:-http://127.0.0.1:8080}"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

pass() { printf "✔ %s\n" "$1"; }
fail() { printf "✘ %s\n" "$1" >&2; exit 1; }

assert_status() {
  local esperado="$1"
  local obtido="$2"
  local contexto="$3"
  if [[ "$obtido" != "$esperado" ]]; then
    fail "$contexto — esperado HTTP $esperado, obtido $obtido"
  fi
  pass "$contexto (HTTP $obtido)"
}

echo "=== 1. Health Checks (sem autenticação) ==="
LIVE_CAT=$(curl -sS -o "$TMP_DIR/cat_live.json" -w "%{http_code}" "$CATALOGO_URL/q/health/live")
assert_status "200" "$LIVE_CAT" "catalogo-service /q/health/live"
grep -q '"UP"' "$TMP_DIR/cat_live.json" || fail "catalogo-service live não retornou UP"

READY_CAT=$(curl -sS -o "$TMP_DIR/cat_ready.json" -w "%{http_code}" "$CATALOGO_URL/q/health/ready")
assert_status "200" "$READY_CAT" "catalogo-service /q/health/ready"
grep -q '"UP"' "$TMP_DIR/cat_ready.json" || fail "catalogo-service ready não retornou UP"

LIVE_BACK=$(curl -sS -o "$TMP_DIR/back_live.json" -w "%{http_code}" "$BACKLOG_URL/q/health/live")
assert_status "200" "$LIVE_BACK" "backlog-service /q/health/live"
grep -q '"UP"' "$TMP_DIR/back_live.json" || fail "backlog-service live não retornou UP"

READY_BACK=$(curl -sS -o "$TMP_DIR/back_ready.json" -w "%{http_code}" "$BACKLOG_URL/q/health/ready")
assert_status "200" "$READY_BACK" "backlog-service /q/health/ready"
grep -q '"UP"' "$TMP_DIR/back_ready.json" || fail "backlog-service ready não retornou UP"

echo ""
echo "=== 2. Segurança: 401 sem credencial e 403 por papel ==="
UNAUTH=$(curl -sS -o "$TMP_DIR/unauth.json" -w "%{http_code}" "$CATALOGO_URL/jogos")
assert_status "401" "$UNAUTH" "GET /jogos sem credenciais"
grep -q 'NAO_AUTENTICADO' "$TMP_DIR/unauth.json" || fail "401 não trouxe ErroPadrao NAO_AUTENTICADO"

LISTAR_CAT=$(curl -sS -u "joao:joao123" -o "$TMP_DIR/jogos.json" -w "%{http_code}" "$CATALOGO_URL/jogos?plataforma=PC")
assert_status "200" "$LISTAR_CAT" "JOGADOR (joao) lista jogos do catálogo"

FORBIDDEN_CAT=$(curl -sS -u "joao:joao123" -o "$TMP_DIR/forb_cat.json" -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d '{"titulo":"Bloqueado","plataforma":"PC","genero":"Indie","duracaoMediaHoras":5}' \
  "$CATALOGO_URL/jogos")
assert_status "403" "$FORBIDDEN_CAT" "JOGADOR (joao) impedido de criar jogo no catálogo"
grep -q 'ACESSO_NEGADO' "$TMP_DIR/forb_cat.json" || fail "403 não trouxe ACESSO_NEGADO"

echo ""
echo "=== 3. ADMIN cadastra jogo no catálogo (201 + Location) ==="
TITULO_DEMO="Sea of Stars Demo $(date +%s)"
CREATED_CAT=$(curl -sS -u "admin:admin123" -o "$TMP_DIR/novo_jogo.json" -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d "{\"titulo\":\"$TITULO_DEMO\",\"plataforma\":\"PC\",\"genero\":\"RPG\",\"duracaoMediaHoras\":30}" \
  "$CATALOGO_URL/jogos")
assert_status "201" "$CREATED_CAT" "ADMIN cria jogo no catálogo"
JOGO_ID=$(sed -n 's/.*"id":\([0-9]*\).*/\1/p' "$TMP_DIR/novo_jogo.json")
[[ -n "$JOGO_ID" ]] || fail "Não foi possível extrair id do jogo criado"

echo ""
echo "=== 4. Integração síncrona backlog-service -> catalogo-service ==="
CREATED_BACK=$(curl -sS -u "joao:joao123" -o "$TMP_DIR/novo_item.json" -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d "{\"jogoId\":$JOGO_ID}" \
  "$BACKLOG_URL/usuarios/joao/backlog")
assert_status "201" "$CREATED_BACK" "joao adiciona jogo $JOGO_ID ao backlog (REST Client -> catálogo)"
ITEM_ID=$(sed -n 's/.*"id":\([0-9]*\).*/\1/p' "$TMP_DIR/novo_item.json")
[[ -n "$ITEM_ID" ]] || fail "Não foi possível extrair id do item no backlog"

PATCH_1=$(curl -sS -u "joao:joao123" -X PATCH -o "$TMP_DIR/patch1.json" -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d '{"status":"JOGANDO"}' \
  "$BACKLOG_URL/usuarios/joao/backlog/$ITEM_ID")
assert_status "200" "$PATCH_1" "Evolui status QUERO_JOGAR -> JOGANDO"

PATCH_2=$(curl -sS -u "joao:joao123" -X PATCH -o "$TMP_DIR/patch2.json" -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d '{"status":"ZERADO"}' \
  "$BACKLOG_URL/usuarios/joao/backlog/$ITEM_ID")
assert_status "200" "$PATCH_2" "Evolui status JOGANDO -> ZERADO"

RESUMO=$(curl -sS -u "joao:joao123" -o "$TMP_DIR/resumo.json" -w "%{http_code}" \
  "$BACKLOG_URL/usuarios/joao/backlog/resumo")
assert_status "200" "$RESUMO" "Consulta resumo do backlog de joao"

DETALHES=$(curl -sS -u "joao:joao123" -o "$TMP_DIR/detalhes.json" -w "%{http_code}" \
  "$BACKLOG_URL/usuarios/joao/backlog/$ITEM_ID/detalhes")
assert_status "200" "$DETALHES" "Consulta detalhes enriquecidos (catálogo vivo)"
grep -q '"catalogoDisponivel":true' "$TMP_DIR/detalhes.json" || fail "detalhes não trouxe catalogoDisponivel:true"

echo ""
echo "=== 5. Regra de posse por usuário (RBAC + dono) e rota ADMIN ==="
FORBIDDEN_POSSE=$(curl -sS -u "joao:joao123" -o "$TMP_DIR/forb_posse.json" -w "%{http_code}" \
  "$BACKLOG_URL/usuarios/maria/backlog")
assert_status "403" "$FORBIDDEN_POSSE" "joao bloqueado ao consultar backlog de maria"

ADMIN_STATS=$(curl -sS -u "admin:admin123" -o "$TMP_DIR/admin_stats.json" -w "%{http_code}" \
  "$BACKLOG_URL/admin/backlogs/estatisticas")
assert_status "200" "$ADMIN_STATS" "ADMIN consulta estatísticas globais"

echo ""
echo "=== 6. Caminhos de erro padronizados (400 e 422) ==="
BAD_REQ=$(curl -sS -u "joao:joao123" -o "$TMP_DIR/bad_req.json" -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d '{"jogoId":-1}' \
  "$BACKLOG_URL/usuarios/joao/backlog")
assert_status "400" "$BAD_REQ" "Validação de entrada inválida retorna 400 ERRO_VALIDACAO"
grep -q 'ERRO_VALIDACAO' "$TMP_DIR/bad_req.json" || fail "400 não trouxe ERRO_VALIDACAO"

NOT_IN_CAT=$(curl -sS -u "joao:joao123" -o "$TMP_DIR/not_in_cat.json" -w "%{http_code}" \
  -H "Content-Type: application/json" \
  -d '{"jogoId":999999}' \
  "$BACKLOG_URL/usuarios/joao/backlog")
assert_status "422" "$NOT_IN_CAT" "Jogo inexistente no catálogo retorna 422 JOGO_INEXISTENTE"
grep -q 'JOGO_INEXISTENTE' "$TMP_DIR/not_in_cat.json" || fail "422 não trouxe JOGO_INEXISTENTE"

echo ""
echo "=== 7. Métricas de negócio em /q/metrics ==="
curl -sS "$CATALOGO_URL/q/metrics" | grep -q 'lettergames_catalogo_jogos_criados_total' \
  || fail "Métrica do catálogo ausente em /q/metrics"
pass "Métricas próprias expostas em catalogo-service /q/metrics"

curl -sS "$BACKLOG_URL/q/metrics" | grep -q 'lettergames_backlog_jogos_zerados_total' \
  || fail "Métrica do backlog ausente em /q/metrics"
pass "Métricas próprias expostas em backlog-service /q/metrics"

echo ""
echo "Demonstração completa finalizada com sucesso!"
