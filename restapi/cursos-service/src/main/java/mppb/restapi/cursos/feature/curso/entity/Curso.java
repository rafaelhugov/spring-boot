package mppb.restapi.cursos.feature.curso.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import mppb.restapi.common.core.util.Esquemas;
import mppb.restapi.cursos.feature.cadastro.entities.EntidadeTutora;
import mppb.restapi.cursos.feature.lookup.entities.Modalidade;
import mppb.restapi.cursos.feature.lookup.entities.PublicoAlvo;
import mppb.restapi.cursos.feature.lookup.entities.TipoEvento;
import org.hibernate.annotations.CreationTimestamp;

import mppb.restapi.cursos.feature.curso.enums.StatusCursoEnum;
import mppb.restapi.cursos.feature.curso.converter.enums.StatusCursoEnumConverter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "curso", schema = Esquemas.SCHEMA_CURSOS)
@Builder
public class Curso extends AbstractEntity<Long> {

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descricao")
    private String descricao;

    @ManyToOne
    @JoinColumn(name = "id_tipoevento")
    private TipoEvento tipoEvento;

    @ManyToOne
    @JoinColumn(name = "id_entidadetutora")
    private EntidadeTutora entidadeTutora;

    @CreationTimestamp
    @Column(name = "datacriacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "inscricaodatainicial")
    private LocalDateTime inscricaoDataInicial;

    @Column(name = "inscricaodatafinal")
    private LocalDateTime inscricaoDataFinal;

    @Column(name = "realizacaodatainicial")
    private LocalDateTime realizacaoDataInicial;

    @Column(name = "realizacaodatafinal")
    private LocalDateTime realizacaoDataFinal;

    @Pattern(regexp = "^\\d+:[0-5]\\d:[0-5]\\d$", message = "Formato de carga horária inválido. Use o formato HH:MM:SS.")
    @Column(name = "cargahoraria", length = 20)
    private String cargaHoraria;

    @Column(name = "percentualchaprovacao")
    private Integer percentualChAprovacao;

    @Column(name = "detalhacargahoraria")
    private String detalhaCargaHoraria;

    @Column(name = "linkprogramacao")
    private String linkProgramacao;

    @ManyToOne
    @JoinColumn(name = "id_modalidade")
    private Modalidade modalidade;

    @Column(name = "progressaofuncional", length = 1)
    private String progressaoFuncional;

    @Column(name = "hashinscricao", unique = true)
    private UUID hashInscricao;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "cursopublicoalvo",
            schema = "curso",
            joinColumns = @JoinColumn(name = "id_curso"),
            inverseJoinColumns = @JoinColumn(name = "id_publicoalvo")
    )
    private Set<PublicoAlvo> publicosAlvo ;

    @Column(name = "datamodificacao")
    private LocalDateTime dataModificacao;

    @Column(name = "id_arquivo_certificado")
    private Long certificateTemplateId;

    @Column(name = "status")
    @Convert(converter = StatusCursoEnumConverter.class)
    @Builder.Default
    private StatusCursoEnum status = StatusCursoEnum.EM_CONSTRUCAO;

    @PrePersist
    private void onPrePersist() {
        this.dataModificacao = LocalDateTime.now();
    }

    @PreUpdate
    private void onPreUpdate() {
        this.dataModificacao = LocalDateTime.now();
    }

    @Transient
    public Long getCargaHorariaMinutos() {
        if (cargaHoraria == null || cargaHoraria.trim().isEmpty()) {
            return 0L;
        }
        String[] parts = cargaHoraria.split(":");
        if (parts.length >= 2) {
            try {
                long hours = Long.parseLong(parts[0]);
                long minutes = Long.parseLong(parts[1]);
                return (hours * 60) + minutes;
            } catch (NumberFormatException e) {
                return 0L;
            }
        }
        return 0L;
    }
}
