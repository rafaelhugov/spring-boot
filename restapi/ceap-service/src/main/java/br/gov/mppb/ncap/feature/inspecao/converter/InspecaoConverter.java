package br.gov.mppb.ncap.feature.inspecao.converter;

import br.gov.mppb.ncap.feature.inspecao.Inspecao;
import br.gov.mppb.ncap.feature.inspecao.dto.InspecaoDto;
import br.gov.mppb.ncap.feature.entidade.Entidade;
import br.gov.mppb.ncap.feature.servidor.ServidorRepository;
import mppb.restapi.common.core.converter.EntityConverterImpl;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class InspecaoConverter extends EntityConverterImpl<InspecaoDto, Inspecao> {

    private final ServidorRepository servidorRepository;

    public InspecaoConverter(ModelMapper modelMapper, ServidorRepository servidorRepository) {
        super(modelMapper);
        this.servidorRepository = servidorRepository;

        // Mapeamentos explícitos seguros para o ModelMapper
        modelMapper.typeMap(Inspecao.class, InspecaoDto.class).addMappings(mapper -> {
            mapper.map(src -> src.getEntidade().getId(), InspecaoDto::setEntidadeId);
            mapper.map(src -> src.getEntidade().getNomecnmp(), InspecaoDto::setEntidadeNomecnmp);
            mapper.map(src -> src.getEntidade().getTipoentidade(), InspecaoDto::setEntidadeTipoentidade);
            mapper.map(src -> src.getEntidade().getMunicipio(), InspecaoDto::setEntidadeMunicipio);
            mapper.map(src -> src.getEntidade().getIdCnmp(), InspecaoDto::setEntidadeIdCnmp);
            mapper.map(src -> src.getEntidade().getNomeComarca(), InspecaoDto::setEntidadeComarca);
        });

        modelMapper.typeMap(InspecaoDto.class, Inspecao.class).addMappings(mapper -> {
            mapper.skip(Inspecao::setEntidade);
        });
    }

    @Override
    public InspecaoDto toDto(Inspecao entity) {
        InspecaoDto dto = super.toDto(entity);
        if (entity != null) {
            if (entity.getTemplate() != null) {
                try {
                    dto.setFormtemplateResolucao(entity.getTemplate().getResolucao());
                } catch (Exception ignored) {}
            }
            if (entity.getIdPromotorresponsavel() != null) {
                try {
                    servidorRepository.obterDetalhesPorId(entity.getIdPromotorresponsavel())
                            .ifPresent(proj -> dto.setPromotorNome(proj.getNome()));
                } catch (Exception ignored) {}
            }
        }
        return dto;
    }

    @Override
    public Inspecao toEntity(InspecaoDto dto) {
        Inspecao entity = super.toEntity(dto);
        if (dto.getEntidadeId() != null) {
            // @Builder não inclui campos herdados de AbstractEntity, por isso usamos setId()
            Entidade entidade = new Entidade();
            entidade.setId(dto.getEntidadeId());
            entity.setEntidade(entidade);
        }
        return entity;
    }
}
