package br.com.lettergames.backlog.dominio;

import static br.com.lettergames.backlog.dominio.StatusBacklog.ABANDONADO;
import static br.com.lettergames.backlog.dominio.StatusBacklog.JOGANDO;
import static br.com.lettergames.backlog.dominio.StatusBacklog.QUERO_JOGAR;
import static br.com.lettergames.backlog.dominio.StatusBacklog.ZERADO;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Regras de transição de status permitidas para um item no backlog do jogador.
 */
public final class TransicoesStatus {

    private static final Map<StatusBacklog, Set<StatusBacklog>> PERMITIDAS = new EnumMap<>(StatusBacklog.class);

    static {
        PERMITIDAS.put(QUERO_JOGAR, EnumSet.of(JOGANDO, ABANDONADO));
        PERMITIDAS.put(JOGANDO, EnumSet.of(ZERADO, ABANDONADO, QUERO_JOGAR));
        PERMITIDAS.put(ABANDONADO, EnumSet.of(JOGANDO, QUERO_JOGAR));
        PERMITIDAS.put(ZERADO, EnumSet.of(JOGANDO));
    }

    private TransicoesStatus() {
    }

    public static boolean permite(StatusBacklog de, StatusBacklog para) {
        return PERMITIDAS.get(de).contains(para);
    }

    public static Set<StatusBacklog> destinosPermitidos(StatusBacklog de) {
        return EnumSet.copyOf(PERMITIDAS.get(de));
    }

    public static void validar(StatusBacklog de, StatusBacklog para) {
        if (de == para) {
            throw new TransicaoInvalidaException("O item já está em " + para);
        }
        if (!permite(de, para)) {
            throw new TransicaoInvalidaException(
                    "Transição inválida: " + de + " -> " + para
                            + ". A partir de " + de + " só é permitido: " + destinosPermitidos(de)
            );
        }
    }
}
