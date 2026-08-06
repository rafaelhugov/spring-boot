package br.gov.mppb.ncap.feature.formtemplate;

import jakarta.persistence.*;
import lombok.*;
import mppb.restapi.common.core.entity.AbstractEntity;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "formtemplates", schema = "ncap")
public class FormTemplate {

    @Id
    @Column(name = "id_formtemplate")
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "resolucao", nullable = false, length = 50)
    private String resolucao;

    @Column(name = "tipoentidade", nullable = false, length = 150)
    private String tipoentidade;

    @Builder.Default
    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    @CreationTimestamp
    @Column(name = "criadoem", nullable = false, updatable = false)
    private Instant criadoem;

    @Builder.Default
    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("ordem ASC")
    private List<Secao> secoes = new ArrayList<>();
}
