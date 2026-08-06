package br.gov.mppb.ncap.feature.entidade.integration;

import br.gov.mppb.ncap.feature.entidade.Entidade;
import br.gov.mppb.ncap.feature.entidade.EntidadeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntidadeSincronizacaoService {

    private final CnmpClient cnmpClient;
    private final EntidadeRepository entidadeRepository;

    @Transactional
    public void sincronizar() {
        log.info("Iniciando a sincronização automática de entidades...");
        List<CnmpEntidadeDto> oficiais = cnmpClient.obterUnidadesOficiais();

        if (oficiais == null || oficiais.isEmpty()) {
            log.warn("Nenhuma entidade oficial recebida da API CNMP. Sincronização cancelada.");
            return;
        }

        List<Entidade> locais = entidadeRepository.findAll();

        // Mapeia entidades locais por idCnmp (se existir)
        Map<Long, Entidade> locaisPorCnmpId = locais.stream()
                .filter(u -> u.getIdCnmp() != null)
                .collect(Collectors.toMap(
                        Entidade::getIdCnmp,
                        u -> u,
                        (existing, replacement) -> existing
                ));

        // Mapeia entidades locais sem idCnmp por nome (normalizado)
        Map<String, Entidade> locaisSemCnmpIdPorNome = locais.stream()
                .filter(u -> u.getIdCnmp() == null)
                .collect(Collectors.toMap(
                        u -> u.getNomecnmp().trim().toLowerCase(),
                        u -> u,
                        (existing, replacement) -> existing
                ));

        Set<Long> idsOficiais = oficiais.stream()
                .map(CnmpEntidadeDto::getId)
                .collect(Collectors.toSet());

        Set<String> nomesOficiaisNormalized = oficiais.stream()
                .map(o -> o.getNomecnmp().trim().toLowerCase())
                .collect(Collectors.toSet());

        int inseridas = 0;
        int atualizadas = 0;
        int inativadas = 0;

        // 1. Inserir ou Atualizar entidades oficiais
        for (CnmpEntidadeDto oficial : oficiais) {
            Long cnmpId = oficial.getId();
            String nameKey = oficial.getNomecnmp().trim().toLowerCase();

            // Tenta obter por idCnmp
            Entidade local = locaisPorCnmpId.get(cnmpId);

            // Caso não encontre por idCnmp, tenta por nome (para entidades legadas/cadastradas sem idCnmp)
            if (local == null) {
                local = locaisSemCnmpIdPorNome.get(nameKey);
                if (local != null) {
                    log.info("Associando idCnmp={} à entidade local '{}' existente.", cnmpId, local.getNomecnmp());
                    local.setIdCnmp(cnmpId);
                }
            }

            // Resolve o idComarca usando a geografia baseada no município
            Long idComarca = entidadeRepository.findComarcaIdByMunicipioNome(oficial.getMunicipio())
                    .orElseGet(() -> {
                        log.warn("Não foi possível mapear a comarca para o município '{}'. Usando fallback comarca ID 1L.", oficial.getMunicipio());
                        return 1L;
                    });

            if (local == null) {
                // Nova entidade
                Entidade nova = Entidade.builder()
                        .idCnmp(cnmpId)
                        .nomecnmp(oficial.getNomecnmp())
                        .tipoentidade(oficial.getTipoentidade())
                        .idComarca(idComarca)
                        .municipio(oficial.getMunicipio())
                        .ativa(true)
                        .build();
                entidadeRepository.save(nova);
                inseridas++;
            } else {
                // Atualizar dados oficiais, mantendo descricaomppb intacto
                local.setNomecnmp(oficial.getNomecnmp());
                local.setTipoentidade(oficial.getTipoentidade());
                local.setIdComarca(idComarca);
                local.setMunicipio(oficial.getMunicipio());
                local.setAtiva(true); // Se veio na API ativa, assegura ativa
                entidadeRepository.save(local);
                atualizadas++;
            }
        }

        // 2. Inativar entidades que não estão na lista oficial
        for (Entidade local : locais) {
            boolean deveInativar = false;

            if (local.getIdCnmp() != null) {
                if (!idsOficiais.contains(local.getIdCnmp())) {
                    deveInativar = true;
                }
            } else {
                String key = local.getNomecnmp().trim().toLowerCase();
                if (!nomesOficiaisNormalized.contains(key)) {
                    deveInativar = true;
                }
            }

            if (deveInativar && Boolean.TRUE.equals(local.getAtiva())) {
                local.setAtiva(false);
                entidadeRepository.save(local);
                inativadas++;
            }
        }

        log.info("Sincronização concluída com sucesso. Resumo: Inseridas={}, Atualizadas={}, Inativadas={}",
                inseridas, atualizadas, inativadas);
    }
}
