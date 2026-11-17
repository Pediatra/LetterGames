package br.com.lettergames.backlog.dominio;

import br.com.lettergames.backlog.api.EstatisticasGlobaisResponse;
import br.com.lettergames.backlog.api.ResumoBacklogResponse;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repositório Panache com consultas por usuário, contagens de regra e agregações de estatísticas.
 */
@ApplicationScoped
public class ItemBacklogRepository implements PanacheRepository<ItemBacklog> {

    private static final Sort ORDENACAO_PADRAO = Sort.by("id").ascending();

    public List<ItemBacklog> listarPorUsuario(String usuarioId, StatusBacklog status) {
        if (status == null) {
            return find("usuarioId", ORDENACAO_PADRAO, usuarioId).list();
        }
        return find("usuarioId = ?1 AND status = ?2", ORDENACAO_PADRAO, usuarioId, status).list();
    }

    public Optional<ItemBacklog> buscarPorIdEUsuario(Long id, String usuarioId) {
        return find("id = ?1 AND usuarioId = ?2", id, usuarioId).firstResultOptional();
    }

    public boolean existePorUsuarioEJogo(String usuarioId, Long jogoId) {
        return count("usuarioId = ?1 AND jogoId = ?2", usuarioId, jogoId) > 0;
    }

    public long contarPorUsuarioEStatus(String usuarioId, StatusBacklog status) {
        return count("usuarioId = ?1 AND status = ?2", usuarioId, status);
    }

    public ResumoBacklogResponse calcularResumoUsuario(String usuarioId) {
        List<Object[]> linhas = getEntityManager().createQuery(
                        "SELECT i.status, COUNT(i), COALESCE(SUM(i.duracaoMediaHoras), 0) "
                                + "FROM ItemBacklog i WHERE i.usuarioId = :usuarioId GROUP BY i.status",
                        Object[].class)
                .setParameter("usuarioId", usuarioId)
                .getResultList();
        AgregadoBacklog agregado = agregar(linhas);
        return new ResumoBacklogResponse(
                usuarioId,
                agregado.total(),
                agregado.porStatus(),
                agregado.horasPendentes(),
                agregado.horasZeradas(),
                agregado.percentualZerado()
        );
    }

    public EstatisticasGlobaisResponse calcularEstatisticasGerais() {
        List<Object[]> linhas = getEntityManager().createQuery(
                        "SELECT i.status, COUNT(i), COALESCE(SUM(i.duracaoMediaHoras), 0) "
                                + "FROM ItemBacklog i GROUP BY i.status",
                        Object[].class)
                .getResultList();
        long totalJogadores = getEntityManager().createQuery(
                        "SELECT COUNT(DISTINCT i.usuarioId) FROM ItemBacklog i",
                        Long.class)
                .getSingleResult();
        AgregadoBacklog agregado = agregar(linhas);
        return new EstatisticasGlobaisResponse(
                totalJogadores,
                agregado.total(),
                agregado.porStatus(),
                agregado.horasPendentes(),
                agregado.horasZeradas(),
                agregado.percentualZerado()
        );
    }

    private static AgregadoBacklog agregar(List<Object[]> linhas) {
        Map<StatusBacklog, Integer> porStatus = new EnumMap<>(StatusBacklog.class);
        for (StatusBacklog status : StatusBacklog.values()) {
            porStatus.put(status, 0);
        }

        int total = 0;
        int horasPendentes = 0;
        int horasZeradas = 0;

        for (Object[] linha : linhas) {
            StatusBacklog status = (StatusBacklog) linha[0];
            int quantidade = ((Number) linha[1]).intValue();
            int horas = ((Number) linha[2]).intValue();

            porStatus.put(status, quantidade);
            total += quantidade;
            if (status == StatusBacklog.QUERO_JOGAR || status == StatusBacklog.JOGANDO) {
                horasPendentes += horas;
            } else if (status == StatusBacklog.ZERADO) {
                horasZeradas += horas;
            }
        }

        int zerados = porStatus.get(StatusBacklog.ZERADO);
        double percentual = total == 0
                ? 0.0
                : BigDecimal.valueOf(zerados * 100L)
                        .divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP)
                        .doubleValue();

        return new AgregadoBacklog(total, porStatus, horasPendentes, horasZeradas, percentual);
    }

    private record AgregadoBacklog(
            int total,
            Map<StatusBacklog, Integer> porStatus,
            int horasPendentes,
            int horasZeradas,
            double percentualZerado) {
    }
}
