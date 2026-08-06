package br.gov.mppb.ncap.feature.formtemplate.converter;

import br.gov.mppb.ncap.feature.formtemplate.*;
import br.gov.mppb.ncap.feature.formtemplate.dto.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class FormTemplateConverter {

    private final FormTemplateDecorationService decorationService;

    public FormTemplateConverter(@Lazy FormTemplateDecorationService decorationService) {
        this.decorationService = decorationService;
    }

    public FormTemplateDto toDto(FormTemplate entity) {
        if (entity == null) {
            return null;
        }

        FormTemplateDto dto = FormTemplateDto.builder()
                .id(entity.getId())
                .resolucao(entity.getResolucao())
                .tipoentidade(entity.getTipoentidade())
                .ativo(entity.getAtivo())
                .criadoem(entity.getCriadoem())
                .secoes(entity.getSecoes() == null ? null : entity.getSecoes().stream()
                        .map(this::toDto)
                        .collect(Collectors.toList()))
                .build();

        decorationService.decorar(dto);
        return dto;
    }

    public SecaoDto toDto(Secao entity) {
        if (entity == null) {
            return null;
        }

        return SecaoDto.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .ordem(entity.getOrdem())
                .blocos(entity.getBlocos() == null ? null : entity.getBlocos().stream()
                        .map(this::toDto)
                        .collect(Collectors.toList()))
                .build();
    }

    public BlocoDto toDto(Bloco entity) {
        if (entity == null) {
            return null;
        }

        return BlocoDto.builder()
                .id(entity.getId())
                .nome(entity.getNome())
                .ordem(entity.getOrdem())
                .perguntas(entity.getPerguntas() == null ? null : entity.getPerguntas().stream()
                        .map(this::toDto)
                        .collect(Collectors.toList()))
                .build();
    }

    public PerguntaDto toDto(Pergunta entity) {
        if (entity == null) {
            return null;
        }

        return PerguntaDto.builder()
                .id(entity.getId())
                .idcnmp(entity.getIdcnmp())
                .label(entity.getLabel())
                .campotipo(entity.getCampotipo())
                .obrigatorio(entity.getObrigatorio())
                .exportarcnmp(entity.getExportarcnmp())
                .ordem(entity.getOrdem())
                .tabulacao(entity.getTabulacao())
                .dependenciapaiId(entity.getDependenciapai() != null ? entity.getDependenciapai().getId() : null)
                .dependenciavalor(entity.getDependenciavalor())
                .opcoes(entity.getOpcoes() == null ? null : entity.getOpcoes().stream()
                        .map(this::toDto)
                        .collect(Collectors.toList()))
                .build();
    }

    public OpcaoRespostaDto toDto(OpcaoResposta entity) {
        if (entity == null) {
            return null;
        }

        return OpcaoRespostaDto.builder()
                .id(entity.getId())
                .descricao(entity.getDescricao())
                .valor(entity.getValor())
                .build();
    }
}
