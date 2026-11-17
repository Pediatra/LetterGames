package br.com.lettergames.catalogo.dominio;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Repositório Panache com consultas de filtro dinâmico, unicidade e contagem por plataforma.
 */
@ApplicationScoped
public class JogoRepository implements PanacheRepository<Jogo> {

    private static final Sort ORDENACAO_PADRAO = Sort.by("titulo").ascending().and("id").ascending();

    public List<Jogo> listarComFiltros(String plataforma, String genero, String termo) {
        List<String> clausulas = new ArrayList<>();
        Map<String, Object> parametros = new HashMap<>();

        if (plataforma != null && !plataforma.isBlank()) {
            clausulas.add("LOWER(plataforma) = :plataforma");
            parametros.put("plataforma", normalizar(plataforma));
        }
        if (genero != null && !genero.isBlank()) {
            clausulas.add("LOWER(genero) = :genero");
            parametros.put("genero", normalizar(genero));
        }
        if (termo != null && !termo.isBlank()) {
            clausulas.add("LOWER(titulo) LIKE :termo");
            parametros.put("termo", "%" + normalizar(termo) + "%");
        }

        if (clausulas.isEmpty()) {
            return listAll(ORDENACAO_PADRAO);
        }
        return find(String.join(" AND ", clausulas), ORDENACAO_PADRAO, parametros).list();
    }

    public boolean existePorTituloEPlataforma(String titulo, String plataforma, Long ignorarId) {
        String tituloNormalizado = normalizar(titulo);
        String plataformaNormalizada = normalizar(plataforma);
        if (ignorarId == null) {
            return count("LOWER(titulo) = ?1 AND LOWER(plataforma) = ?2", tituloNormalizado, plataformaNormalizada) > 0;
        }
        return count(
                "LOWER(titulo) = ?1 AND LOWER(plataforma) = ?2 AND id <> ?3",
                tituloNormalizado,
                plataformaNormalizada,
                ignorarId
        ) > 0;
    }

    public long contarPorPlataforma(String plataforma) {
        return count("LOWER(plataforma) = ?1", normalizar(plataforma));
    }

    private static String normalizar(String valor) {
        return valor.trim().toLowerCase(Locale.ROOT);
    }
}
