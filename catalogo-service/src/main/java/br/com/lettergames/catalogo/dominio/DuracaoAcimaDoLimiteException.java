package br.com.lettergames.catalogo.dominio;

/**
 * Lançada quando a duração média informada excede o limite máximo configurado (HTTP 422).
 */
public class DuracaoAcimaDoLimiteException extends DominioCatalogoException {

    public DuracaoAcimaDoLimiteException(int duracaoInformada, int limiteMaximo) {
        super(422, "DURACAO_ACIMA_DO_LIMITE",
                "Duração média de " + duracaoInformada + "h excede o limite configurado de " + limiteMaximo + "h");
    }
}
