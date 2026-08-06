package br.gov.mppb.ncap.feature.formtemplate;

import mppb.restapi.common.core.repository.BaseRepository;
import java.util.Optional;

public interface FormTemplateRepository extends BaseRepository<FormTemplate, Long> {
    Optional<FormTemplate> findByResolucaoAndTipoentidadeAndAtivoTrue(String resolucao, String tipoentidade);
    Optional<FormTemplate> findFirstByTipoentidadeAndAtivoTrueOrderByCriadoemDesc(String tipoentidade);
}
