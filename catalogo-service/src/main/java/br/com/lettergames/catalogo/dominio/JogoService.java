package br.com.lettergames.catalogo.dominio;

import br.com.lettergames.catalogo.api.JogoRequest;
import br.com.lettergames.catalogo.api.JogoResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Regras de negócio e métricas do catálogo de jogos.
 */
@Startup
@ApplicationScoped
public class JogoService {

    private static final Logger LOG = Logger.getLogger(JogoService.class);

    private final JogoRepository jogoRepository;
    private final int duracaoMaximaHoras;
    private final Counter contadorJogosCriados;
    private final Counter contadorConsultasJogo;

    @Inject
    public JogoService(
            JogoRepository jogoRepository,
            MeterRegistry meterRegistry,
            @ConfigProperty(name = "catalogo.duracao-maxima-horas") int duracaoMaximaHoras) {
        this.jogoRepository = jogoRepository;
        this.duracaoMaximaHoras = duracaoMaximaHoras;
        this.contadorJogosCriados = Counter.builder("lettergames_catalogo_jogos_criados_total")
                .description("Total de jogos cadastrados no catálogo")
                .register(meterRegistry);
        this.contadorConsultasJogo = Counter.builder("lettergames_catalogo_consultas_jogo_total")
                .description("Total de consultas individuais de jogos no catálogo")
                .register(meterRegistry);
    }

    @Transactional
    public JogoResponse criar(JogoRequest requisicao) {
        validarDuracao(requisicao.duracaoMediaHoras());
        validarUnicidade(requisicao.titulo(), requisicao.plataforma(), null);

        Jogo jogo = new Jogo(
                requisicao.titulo(),
                requisicao.plataforma(),
                requisicao.genero(),
                requisicao.duracaoMediaHoras()
        );
        jogoRepository.persist(jogo);
        contadorJogosCriados.increment();
        LOG.infof("Jogo cadastrado no catálogo: id=%d titulo=%s", jogo.getId(), jogo.getTitulo());
        return JogoResponse.de(jogo);
    }

    public JogoResponse buscarPorId(Long id) {
        Jogo jogo = jogoRepository.findByIdOptional(id)
                .orElseThrow(() -> new JogoNaoEncontradoException(id));
        contadorConsultasJogo.increment();
        return JogoResponse.de(jogo);
    }

    public List<JogoResponse> listar(String plataforma, String genero, String termo) {
        return jogoRepository.listarComFiltros(plataforma, genero, termo).stream()
                .map(JogoResponse::de)
                .toList();
    }

    @Transactional
    public JogoResponse atualizar(Long id, JogoRequest requisicao) {
        validarDuracao(requisicao.duracaoMediaHoras());
        Jogo jogo = jogoRepository.findByIdOptional(id)
                .orElseThrow(() -> new JogoNaoEncontradoException(id));
        validarUnicidade(requisicao.titulo(), requisicao.plataforma(), id);

        jogo.atualizar(
                requisicao.titulo(),
                requisicao.plataforma(),
                requisicao.genero(),
                requisicao.duracaoMediaHoras()
        );
        LOG.infof("Jogo atualizado no catálogo: id=%d titulo=%s", jogo.getId(), jogo.getTitulo());
        return JogoResponse.de(jogo);
    }

    @Transactional
    public void remover(Long id) {
        Jogo jogo = jogoRepository.findByIdOptional(id)
                .orElseThrow(() -> new JogoNaoEncontradoException(id));
        jogoRepository.delete(jogo);
        LOG.infof("Jogo removido do catálogo: id=%d", id);
    }

    private void validarDuracao(Integer duracaoMediaHoras) {
        if (duracaoMediaHoras > duracaoMaximaHoras) {
            throw new DuracaoAcimaDoLimiteException(duracaoMediaHoras, duracaoMaximaHoras);
        }
    }

    private void validarUnicidade(String titulo, String plataforma, Long ignorarId) {
        if (jogoRepository.existePorTituloEPlataforma(titulo, plataforma, ignorarId)) {
            throw new JogoDuplicadoException(titulo.trim(), plataforma.trim());
        }
    }
}
