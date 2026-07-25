package mppb.restapi.cursos.testsupport;

import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.cursos.feature.curso.entity.Curso;
import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import mppb.restapi.cursos.feature.frequencia.entity.Aula;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;
import mppb.restapi.cursos.feature.ministrante.entity.Ministrante;
import mppb.restapi.cursos.feature.ministrante.enums.TipoMinistranteEnum;
import mppb.restapi.cursos.feature.participante.entity.Participante;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public final class CursoTestDataFactory {

    private CursoTestDataFactory() {
    }

    public static Curso cursoPublicado() {
        Curso curso = new Curso();
        curso.setId(1L);
        curso.setTitulo("Curso de Testes Automatizados");
        curso.setDescricao("Formacao pratica em qualidade de software");
        curso.setCargaHoraria("08:00:00");
        curso.setPercentualChAprovacao(75);
        curso.setInscricaoDataInicial(LocalDateTime.of(2026, 8, 1, 8, 0));
        curso.setInscricaoDataFinal(LocalDateTime.of(2026, 8, 10, 18, 0));
        curso.setRealizacaoDataInicial(LocalDateTime.of(2026, 8, 20, 8, 0));
        curso.setRealizacaoDataFinal(LocalDateTime.of(2026, 8, 20, 17, 0));
        curso.setStatus(StatusCursoEnum.PUBLICADO);
        curso.setModalidade(modalidadePresencial());
        curso.setTipoEvento(tipoEventoCurso());
        curso.setEntidadeTutora(entidadeTutoraCeaf());
        return curso;
    }

    public static Aula aula(Curso curso) {
        return Aula.builder()
                .curso(curso)
                .descricao("Aula de abertura")
                .data(LocalDate.of(2026, 8, 20))
                .horaInicio(LocalTime.of(8, 0))
                .horaFim(LocalTime.of(10, 0))
                .quantidadeChamadas(2)
                .build();
    }

    public static Participante participanteExterno() {
        Participante participante = new Participante();
        participante.setId(1L);
        participante.setNome("Participante Externo");
        participante.setCpf("52998224725");
        participante.setEmail("participante@example.org");
        participante.setTelefone("83999999999");
        return participante;
    }

    public static Ministrante ministranteExterno() {
        return Ministrante.builder()
                .nome("MARIA EXTERNA")
                .cpf("52998224725")
                .nomeOrgaoExterno("Instituicao Parceira")
                .tipo(TipoMinistranteEnum.EXTERNO)
                .email("maria@example.org")
                .telefone("83999999999")
                .build();
    }

    private static Modalidade modalidadePresencial() {
        Modalidade modalidade = new Modalidade();
        modalidade.setId(1L);
        modalidade.setDescricao("Presencial");
        return modalidade;
    }

    private static TipoEvento tipoEventoCurso() {
        TipoEvento tipoEvento = new TipoEvento();
        tipoEvento.setId(1L);
        tipoEvento.setDescricao("Curso");
        return tipoEvento;
    }

    private static EntidadeTutora entidadeTutoraCeaf() {
        EntidadeTutora entidadeTutora = new EntidadeTutora();
        entidadeTutora.setId(1L);
        entidadeTutora.setNome("CEAF");
        return entidadeTutora;
    }
}
