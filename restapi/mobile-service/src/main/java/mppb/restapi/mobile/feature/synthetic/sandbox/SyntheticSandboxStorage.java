package mppb.restapi.mobile.feature.synthetic.sandbox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.concessaofolga.dto.AusenciaCalendarioDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.ConcessaoFolgaDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.OperacaoSolicitacaoRequestDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoConcessaoDto;
import mppb.restapi.mobile.feature.concessaofolga.dto.SolicitacaoMarcacaoFeriasRequestDto;
import mppb.restapi.mobile.feature.notificacao.dto.DispositivoNotificacaoResponse;
import mppb.restapi.mobile.feature.notificacao.dto.RegistrarDispositivoRequest;
import mppb.restapi.mobile.feature.notificacao.model.DispositivoFcm;
import mppb.restapi.mobile.feature.synthetic.audit.AuditoriaUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;
import mppb.restapi.mobile.feature.synthetic.model.OrigemConta;
import mppb.restapi.mobile.feature.synthetic.model.PerfilFuncional;

@Slf4j
@Component
@ConditionalOnProperty(name = "uat.synthetic-users.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SyntheticSandboxStorage {

    private final AuditoriaUsuarioSintetico auditoria;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Map<String, List<ConcessaoFolgaDto>> concessoesPorMatricula = new ConcurrentHashMap<>();
    private final Map<String, List<AusenciaCalendarioDto>> ausenciasPorMatricula = new ConcurrentHashMap<>();
    private final Map<Long, SolicitacaoConcessaoDto> solicitacoesPorId = new ConcurrentHashMap<>();
    private final Map<Long, String> matriculaPorSolicitacaoId = new ConcurrentHashMap<>();
    private final Map<String, List<DispositivoFcm>> dispositivosPorMatricula = new ConcurrentHashMap<>();

    private final AtomicLong nextSolicitacaoId = new AtomicLong(5000L);
    private final AtomicLong nextDispositivoId = new AtomicLong(1L);

    @PostConstruct
    public void init() {
        reset();
    }

    public synchronized void reset() {
        concessoesPorMatricula.clear();
        ausenciasPorMatricula.clear();
        solicitacoesPorId.clear();
        matriculaPorSolicitacaoId.clear();
        dispositivosPorMatricula.clear();
        nextSolicitacaoId.set(5000L);
        nextDispositivoId.set(1L);

        // Inicializar Membro (900001)
        List<ConcessaoFolgaDto> concessoesMembro = new ArrayList<>();
        ConcessaoFolgaDto c1 = new ConcessaoFolgaDto();
        c1.setId(1001L);
        c1.setTipo("Férias");
        c1.setDescricaoOriginal("Férias Anuais Remuneradas");
        c1.setDiasConcedidos(30);
        c1.setSaldoRestante(15);
        c1.setPeriodo(1);
        c1.setAnoReferencia(2025);
        c1.setInicioPeriodoAquisitivo(LocalDate.of(2025, 1, 1));
        c1.setFinalPeriodoAquisitivo(LocalDate.of(2025, 12, 31));

        SolicitacaoConcessaoDto s1 = new SolicitacaoConcessaoDto();
        s1.setId(5001L);
        s1.setTipo("Férias");
        s1.setTipoOriginal("Férias");
        s1.setInicio(LocalDate.of(2026, 11, 1));
        s1.setFim(LocalDate.of(2026, 11, 15));
        s1.setStatus("HOMOLOGADA");
        s1.setStatusOriginal("HOMOLOGADA");
        s1.setDiasInformados(15);
        c1.getSolicitacoes().add(s1);
        solicitacoesPorId.put(s1.getId(), s1);
        matriculaPorSolicitacaoId.put(s1.getId(), "900001");

        ConcessaoFolgaDto c2 = new ConcessaoFolgaDto();
        c2.setId(1002L);
        c2.setTipo("PLANTÃO");
        c2.setDescricaoOriginal("PLANTÃO 10/11/2025 - 16/11/2025");
        c2.setDiasConcedidos(5);
        c2.setSaldoRestante(5);
        c2.setInicioPeriodoAquisitivo(LocalDate.of(2025, 11, 10));
        c2.setFinalPeriodoAquisitivo(LocalDate.of(2025, 11, 16));

        ConcessaoFolgaDto c3 = new ConcessaoFolgaDto();
        c3.setId(1003L);
        c3.setTipo("LICENÇA");
        c3.setDescricaoOriginal("LICENÇA 01/01/2015 - 31/12/2019");
        c3.setDiasConcedidos(90);
        c3.setSaldoRestante(60);
        c3.setInicioPeriodoAquisitivo(LocalDate.of(2015, 1, 1));
        c3.setFinalPeriodoAquisitivo(LocalDate.of(2019, 12, 31));

        ConcessaoFolgaDto c4 = new ConcessaoFolgaDto();
        c4.setId(1004L);
        c4.setTipo("ELEITORAL");
        c4.setDescricaoOriginal("ELEITORAL 1/2024");
        c4.setDiasConcedidos(10);
        c4.setSaldoRestante(10);
        c4.setPeriodo(1);
        c4.setAnoReferencia(2024);

        concessoesMembro.add(c1);
        concessoesMembro.add(c2);
        concessoesMembro.add(c3);
        concessoesMembro.add(c4);
        concessoesPorMatricula.put("900001", concessoesMembro);

        // Ausências Membro
        List<AusenciaCalendarioDto> ausenciasMembro = new ArrayList<>();
        AusenciaCalendarioDto a1 = new AusenciaCalendarioDto();
        a1.setId("AUS-001");
        a1.setTipo("Férias");
        a1.setDescricao("Férias Anuais - 1º Período");
        a1.setDataInicio(LocalDate.of(2026, 11, 1));
        a1.setDataFim(LocalDate.of(2026, 11, 15));
        a1.setStatus("HOMOLOGADA");
        a1.setObservacao("Portaria PGJ UAT");
        ausenciasMembro.add(a1);
        ausenciasPorMatricula.put("900001", ausenciasMembro);

        // Inicializar Servidor (900002) - Espelhado fielmente em dados reais de recursoshumanos.concessoes_vigentes_vw
        List<ConcessaoFolgaDto> concessoesServidor = new ArrayList<>();

        // 1. FÉRIAS (2025) - Com solicitação agendada de 10 dias
        ConcessaoFolgaDto cs1 = new ConcessaoFolgaDto();
        cs1.setId(2001L);
        cs1.setTipo("FÉRIAS");
        cs1.setDescricaoOriginal("FÉRIAS 1/2025");
        cs1.setDiasConcedidos(30);
        cs1.setSaldoRestante(20);
        cs1.setPeriodo(1);
        cs1.setAnoReferencia(2025);
        cs1.setInicioPeriodoAquisitivo(LocalDate.of(2025, 1, 1));
        cs1.setFinalPeriodoAquisitivo(LocalDate.of(2025, 12, 31));

        SolicitacaoConcessaoDto ss1 = new SolicitacaoConcessaoDto();
        ss1.setId(6001L);
        ss1.setTipo("FÉRIAS");
        ss1.setTipoOriginal("FÉRIAS");
        ss1.setInicio(LocalDate.of(2026, 12, 1));
        ss1.setFim(LocalDate.of(2026, 12, 10));
        ss1.setStatus("HOMOLOGADA");
        ss1.setStatusOriginal("HOMOLOGADA");
        ss1.setDiasInformados(10);
        cs1.getSolicitacoes().add(ss1);
        solicitacoesPorId.put(ss1.getId(), ss1);
        matriculaPorSolicitacaoId.put(ss1.getId(), "900002");

        // 2. FÉRIAS (2024) - Período anterior com saldo remanescente
        ConcessaoFolgaDto cs2 = new ConcessaoFolgaDto();
        cs2.setId(2002L);
        cs2.setTipo("FÉRIAS");
        cs2.setDescricaoOriginal("FÉRIAS 2/2024");
        cs2.setDiasConcedidos(30);
        cs2.setSaldoRestante(10);
        cs2.setPeriodo(2);
        cs2.setAnoReferencia(2024);
        cs2.setInicioPeriodoAquisitivo(LocalDate.of(2024, 1, 1));
        cs2.setFinalPeriodoAquisitivo(LocalDate.of(2024, 12, 31));

        // 3. LICENÇA (Licença Prêmio / Especial) - 90 concedidos, 60 restantes
        ConcessaoFolgaDto cs3 = new ConcessaoFolgaDto();
        cs3.setId(2003L);
        cs3.setTipo("LICENÇA");
        cs3.setDescricaoOriginal("LICENÇA 21/11/2016 - 20/11/2021");
        cs3.setDiasConcedidos(90);
        cs3.setSaldoRestante(60);
        cs3.setInicioPeriodoAquisitivo(LocalDate.of(2016, 11, 21));
        cs3.setFinalPeriodoAquisitivo(LocalDate.of(2021, 11, 20));

        // 4. PLANTÃO (Folga de Plantão / Recesso) - 5 concedidos, 3 restantes
        ConcessaoFolgaDto cs4 = new ConcessaoFolgaDto();
        cs4.setId(2004L);
        cs4.setTipo("PLANTÃO");
        cs4.setDescricaoOriginal("PLANTÃO 10/11/2025 - 16/11/2025");
        cs4.setDiasConcedidos(5);
        cs4.setSaldoRestante(3);
        cs4.setInicioPeriodoAquisitivo(LocalDate.of(2025, 11, 10));
        cs4.setFinalPeriodoAquisitivo(LocalDate.of(2025, 11, 16));

        // 5. ELEITORAL (Folga Eleitoral) - 8 concedidos, 8 restantes
        ConcessaoFolgaDto cs5 = new ConcessaoFolgaDto();
        cs5.setId(2005L);
        cs5.setTipo("ELEITORAL");
        cs5.setDescricaoOriginal("ELEITORAL 2/2024");
        cs5.setDiasConcedidos(8);
        cs5.setSaldoRestante(8);
        cs5.setPeriodo(2);
        cs5.setAnoReferencia(2024);

        concessoesServidor.add(cs1);
        concessoesServidor.add(cs2);
        concessoesServidor.add(cs3);
        concessoesServidor.add(cs4);
        concessoesServidor.add(cs5);
        concessoesPorMatricula.put("900002", concessoesServidor);

        // Ausências do Servidor para o Calendário
        List<AusenciaCalendarioDto> ausenciasServidor = new ArrayList<>();
        AusenciaCalendarioDto as1 = new AusenciaCalendarioDto();
        as1.setId("AUS-002");
        as1.setTipo("FÉRIAS");
        as1.setDescricao("Férias Anuais - 1º Período 2025");
        as1.setDataInicio(LocalDate.of(2026, 12, 1));
        as1.setDataFim(LocalDate.of(2026, 12, 10));
        as1.setStatus("HOMOLOGADA");
        as1.setObservacao("Portaria Servidor UAT");
        ausenciasServidor.add(as1);
        ausenciasPorMatricula.put("900002", ausenciasServidor);

        log.info("[SANDBOX-SINTETICO] Datasets e sandbox de operacoes resetados com sucesso.");
    }

    public List<ConcessaoFolgaDto> listarConcessoes(String matricula) {
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matricula), "SANDBOX_LISTAR_CONCESSOES");
        return concessoesPorMatricula.getOrDefault(matricula, Collections.emptyList());
    }

    public List<AusenciaCalendarioDto> listarAusencias(String matricula, Integer mes, Integer ano) {
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matricula), "SANDBOX_LISTAR_AUSENCIAS");
        List<AusenciaCalendarioDto> todas = ausenciasPorMatricula.getOrDefault(matricula, Collections.emptyList());
        if (mes == null || ano == null) {
            return todas;
        }
        return todas.stream()
                .filter(a -> (a.getDataInicio().getMonthValue() == mes && a.getDataInicio().getYear() == ano)
                        || (a.getDataFim().getMonthValue() == mes && a.getDataFim().getYear() == ano))
                .toList();
    }

    LocalDate parseFlexibleDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now().plusDays(30);
        }
        String s = dateStr.trim();
        try {
            if (s.contains("/")) {
                return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            }
            if (s.contains("T")) {
                return java.time.LocalDateTime.parse(s, DateTimeFormatter.ISO_DATE_TIME).toLocalDate();
            }
            if (s.length() >= 10 && s.charAt(4) == '-' && s.charAt(7) == '-') {
                return LocalDate.parse(s.substring(0, 10));
            }
            return LocalDate.parse(s);
        } catch (Exception e) {
            log.warn("Formato de data nao reconhecido no sandbox sintetico: '{}', aplicando fallback seguro", dateStr);
            return LocalDate.now().plusDays(30);
        }
    }

    public ResponseEntity<String> sugestoesDatasFinais(Long concessaoId, String dataInicial) {
        auditoria.registrar(OrigemConta.UAT, PerfilFuncional.MEMBRO, "SANDBOX_SUGESTOES_DATAS_FINAIS");
        try {
            LocalDate dtInicio = parseFlexibleDate(dataInicial);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String dataInicialFmt = dtInicio.format(fmt);

            ArrayNode array = objectMapper.createArrayNode();
            int[] diasOpcoes = new int[]{10, 15, 20, 30};
            for (int dias : diasOpcoes) {
                LocalDate dtFim = dtInicio.plusDays(dias - 1);
                ObjectNode item = objectMapper.createObjectNode();
                item.put("numeroDias", dias);
                item.put("dataInicial", dataInicialFmt);
                item.put("dataFinal", dtFim.format(fmt));
                array.add(item);
            }
            return ResponseEntity.ok(objectMapper.writeValueAsString(array));
        } catch (Exception e) {
            log.error("Erro ao gerar sugestoes sinteticas", e);
            return ResponseEntity.ok("[]");
        }
    }

    public synchronized ResponseEntity<String> cadastrarSolicitacao(
            Long concessaoId,
            SolicitacaoMarcacaoFeriasRequestDto request,
            String token) {
        auditoria.registrar(OrigemConta.UAT, PerfilFuncional.MEMBRO, "SANDBOX_CADASTRAR_SOLICITACAO_FOLGA");

        long id = nextSolicitacaoId.incrementAndGet();
        SolicitacaoConcessaoDto dto = new SolicitacaoConcessaoDto();
        dto.setId(id);

        LocalDate inicio;
        try {
            inicio = request != null && request.getDataInicial() != null
                    ? parseFlexibleDate(request.getDataInicial())
                    : LocalDate.now().plusDays(30);
        } catch (Exception e) {
            inicio = LocalDate.now().plusDays(30);
        }
        int dias = request != null && request.getNumeroDias() != null ? request.getNumeroDias() : 15;
        LocalDate fim = inicio.plusDays(dias - 1);

        dto.setTipo("Férias");
        dto.setTipoOriginal("Férias");
        dto.setInicio(inicio);
        dto.setFim(fim);
        dto.setStatus("SOLICITADA");
        dto.setStatusOriginal("SOLICITADA");
        dto.setDiasInformados(dias);

        solicitacoesPorId.put(id, dto);

        String ownerMatricula = null;
        for (Map.Entry<String, List<ConcessaoFolgaDto>> entry : concessoesPorMatricula.entrySet()) {
            for (ConcessaoFolgaDto c : entry.getValue()) {
                if (c.getId().equals(concessaoId)) {
                    c.getSolicitacoes().add(dto);
                    ownerMatricula = entry.getKey();
                    if (c.getSaldoRestante() != null && c.getSaldoRestante() >= dto.getDiasInformados()) {
                        c.setSaldoRestante(c.getSaldoRestante() - dto.getDiasInformados());
                    }
                }
            }
        }
        if (ownerMatricula == null && token != null && token.contains("servidor")) {
            ownerMatricula = "900002";
        }
        matriculaPorSolicitacaoId.put(id, ownerMatricula != null ? ownerMatricula : "900001");

        try {
            ObjectNode resp = objectMapper.createObjectNode();
            resp.put("id", id);
            resp.put("status", "SOLICITADA");
            resp.put("mensagem", "Solicitação cadastrada com sucesso no sandbox sintético.");
            return ResponseEntity.ok(objectMapper.writeValueAsString(resp));
        } catch (Exception e) {
            return ResponseEntity.ok("{\"id\":" + id + ",\"status\":\"SOLICITADA\"}");
        }
    }

    private boolean isOwner(Long solicitacaoId, String callerMatricula) {
        if (callerMatricula == null || callerMatricula.isBlank()) {
            return true;
        }
        String ownerMatricula = matriculaPorSolicitacaoId.get(solicitacaoId);
        if (ownerMatricula == null) {
            return true;
        }
        return ownerMatricula.equals(callerMatricula.trim());
    }

    public ResponseEntity<String> consultarSolicitacao(Long solicitacaoId, String callerMatricula) {
        SolicitacaoConcessaoDto dto = solicitacoesPorId.get(solicitacaoId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isOwner(solicitacaoId, callerMatricula)) {
            auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(callerMatricula), "SANDBOX_CONSULTAR_SOLICITACAO_ACESSO_NEGADO_" + solicitacaoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"erro\":\"Acesso negado: a solicitacao nao pertence ao usuario autenticado.\"}");
        }
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matriculaPorSolicitacaoId.get(solicitacaoId)), "SANDBOX_CONSULTAR_SOLICITACAO_" + solicitacaoId);
        try {
            return ResponseEntity.ok(objectMapper.writeValueAsString(dto));
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    public ResponseEntity<String> consultarSolicitacao(Long solicitacaoId) {
        return consultarSolicitacao(solicitacaoId, (String) null);
    }

    public ResponseEntity<String> consultarOperacoesSolicitacao(Long solicitacaoId, String callerMatricula) {
        SolicitacaoConcessaoDto dto = solicitacoesPorId.get(solicitacaoId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isOwner(solicitacaoId, callerMatricula)) {
            auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(callerMatricula), "SANDBOX_CONSULTAR_OPERACOES_ACESSO_NEGADO_" + solicitacaoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"erro\":\"Acesso negado: a solicitacao nao pertence ao usuario autenticado.\"}");
        }
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matriculaPorSolicitacaoId.get(solicitacaoId)), "SANDBOX_CONSULTAR_OPERACOES_" + solicitacaoId);
        ArrayNode ops = objectMapper.createArrayNode();
        ops.add("CANCELAR");
        ops.add("ADIAR");
        ops.add("ANTECIPAR");
        ops.add("INTERROMPER");
        try {
            return ResponseEntity.ok(objectMapper.writeValueAsString(ops));
        } catch (Exception e) {
            return ResponseEntity.ok("[\"CANCELAR\",\"ADIAR\"]");
        }
    }

    public ResponseEntity<String> consultarOperacoesSolicitacao(Long solicitacaoId) {
        return consultarOperacoesSolicitacao(solicitacaoId, (String) null);
    }

    public synchronized ResponseEntity<String> cancelarSolicitacao(Long solicitacaoId, String callerMatricula) {
        SolicitacaoConcessaoDto dto = solicitacoesPorId.get(solicitacaoId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isOwner(solicitacaoId, callerMatricula)) {
            auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(callerMatricula), "SANDBOX_CANCELAR_SOLICITACAO_ACESSO_NEGADO_" + solicitacaoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"erro\":\"Acesso negado: a solicitacao nao pertence ao usuario autenticado.\"}");
        }
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matriculaPorSolicitacaoId.get(solicitacaoId)), "SANDBOX_CANCELAR_SOLICITACAO_" + solicitacaoId);
        dto.setStatus("CANCELADA");
        dto.setStatusOriginal("CANCELADA");
        return ResponseEntity.ok("{\"status\":\"CANCELADA\",\"mensagem\":\"Solicitação cancelada no sandbox.\"}");
    }

    public synchronized ResponseEntity<String> cancelarSolicitacao(Long solicitacaoId) {
        return cancelarSolicitacao(solicitacaoId, (String) null);
    }

    public synchronized ResponseEntity<String> adiarSolicitacao(Long solicitacaoId, OperacaoSolicitacaoRequestDto request, String callerMatricula) {
        SolicitacaoConcessaoDto dto = solicitacoesPorId.get(solicitacaoId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isOwner(solicitacaoId, callerMatricula)) {
            auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(callerMatricula), "SANDBOX_ADIAR_SOLICITACAO_ACESSO_NEGADO_" + solicitacaoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"erro\":\"Acesso negado: a solicitacao nao pertence ao usuario autenticado.\"}");
        }
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matriculaPorSolicitacaoId.get(solicitacaoId)), "SANDBOX_ADIAR_SOLICITACAO_" + solicitacaoId);
        if (request != null && request.getDataAdiamento() != null) {
            try {
                dto.setInicio(parseFlexibleDate(request.getDataAdiamento()));
                if (dto.getDiasInformados() != null) {
                    dto.setFim(dto.getInicio().plusDays(dto.getDiasInformados() - 1));
                }
            } catch (Exception ignored) {}
        }
        dto.setStatus("ADIADA");
        return ResponseEntity.ok("{\"status\":\"ADIADA\",\"mensagem\":\"Solicitação adiada no sandbox.\"}");
    }

    public synchronized ResponseEntity<String> adiarSolicitacao(Long solicitacaoId, OperacaoSolicitacaoRequestDto request) {
        return adiarSolicitacao(solicitacaoId, request, (String) null);
    }

    public synchronized ResponseEntity<String> anteciparSolicitacao(Long solicitacaoId, OperacaoSolicitacaoRequestDto request, String callerMatricula) {
        SolicitacaoConcessaoDto dto = solicitacoesPorId.get(solicitacaoId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isOwner(solicitacaoId, callerMatricula)) {
            auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(callerMatricula), "SANDBOX_ANTECIPAR_SOLICITACAO_ACESSO_NEGADO_" + solicitacaoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"erro\":\"Acesso negado: a solicitacao nao pertence ao usuario autenticado.\"}");
        }
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matriculaPorSolicitacaoId.get(solicitacaoId)), "SANDBOX_ANTECIPAR_SOLICITACAO_" + solicitacaoId);
        if (request != null && request.getDataAntecipacao() != null) {
            try {
                dto.setInicio(parseFlexibleDate(request.getDataAntecipacao()));
                if (dto.getDiasInformados() != null) {
                    dto.setFim(dto.getInicio().plusDays(dto.getDiasInformados() - 1));
                }
            } catch (Exception ignored) {}
        }
        dto.setStatus("ANTECIPADA");
        return ResponseEntity.ok("{\"status\":\"ANTECIPADA\",\"mensagem\":\"Solicitação antecipada no sandbox.\"}");
    }

    public synchronized ResponseEntity<String> anteciparSolicitacao(Long solicitacaoId, OperacaoSolicitacaoRequestDto request) {
        return anteciparSolicitacao(solicitacaoId, request, (String) null);
    }

    public synchronized ResponseEntity<String> interromperSolicitacao(Long solicitacaoId, OperacaoSolicitacaoRequestDto request, String callerMatricula) {
        SolicitacaoConcessaoDto dto = solicitacoesPorId.get(solicitacaoId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isOwner(solicitacaoId, callerMatricula)) {
            auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(callerMatricula), "SANDBOX_INTERROMPER_SOLICITACAO_ACESSO_NEGADO_" + solicitacaoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"erro\":\"Acesso negado: a solicitacao nao pertence ao usuario autenticado.\"}");
        }
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matriculaPorSolicitacaoId.get(solicitacaoId)), "SANDBOX_INTERROMPER_SOLICITACAO_" + solicitacaoId);
        dto.setStatus("INTERROMPIDA");
        return ResponseEntity.ok("{\"status\":\"INTERROMPIDA\",\"mensagem\":\"Solicitação interrompida no sandbox.\"}");
    }

    public synchronized ResponseEntity<String> interromperSolicitacao(Long solicitacaoId, OperacaoSolicitacaoRequestDto request) {
        return interromperSolicitacao(solicitacaoId, request, (String) null);
    }

    public synchronized ResponseEntity<String> suspenderIntegralmenteSolicitacao(Long solicitacaoId, String callerMatricula) {
        SolicitacaoConcessaoDto dto = solicitacoesPorId.get(solicitacaoId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isOwner(solicitacaoId, callerMatricula)) {
            auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(callerMatricula), "SANDBOX_SUSPENDER_INTEGRAL_SOLICITACAO_ACESSO_NEGADO_" + solicitacaoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"erro\":\"Acesso negado: a solicitacao nao pertence ao usuario autenticado.\"}");
        }
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matriculaPorSolicitacaoId.get(solicitacaoId)), "SANDBOX_SUSPENDER_INTEGRAL_SOLICITACAO_" + solicitacaoId);
        dto.setStatus("SUSPENSA");
        return ResponseEntity.ok("{\"status\":\"SUSPENSA\",\"mensagem\":\"Solicitação suspensa no sandbox.\"}");
    }

    public synchronized ResponseEntity<String> suspenderIntegralmenteSolicitacao(Long solicitacaoId) {
        return suspenderIntegralmenteSolicitacao(solicitacaoId, (String) null);
    }

    public synchronized ResponseEntity<String> suspenderParcialmenteSolicitacao(Long solicitacaoId, OperacaoSolicitacaoRequestDto request, String callerMatricula) {
        SolicitacaoConcessaoDto dto = solicitacoesPorId.get(solicitacaoId);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        if (!isOwner(solicitacaoId, callerMatricula)) {
            auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(callerMatricula), "SANDBOX_SUSPENDER_PARCIAL_SOLICITACAO_ACESSO_NEGADO_" + solicitacaoId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"erro\":\"Acesso negado: a solicitacao nao pertence ao usuario autenticado.\"}");
        }
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matriculaPorSolicitacaoId.get(solicitacaoId)), "SANDBOX_SUSPENDER_PARCIAL_SOLICITACAO_" + solicitacaoId);
        dto.setStatus("SUSPENSA_PARCIAL");
        return ResponseEntity.ok("{\"status\":\"SUSPENSA_PARCIAL\",\"mensagem\":\"Solicitação suspensa parcialmente no sandbox.\"}");
    }

    public synchronized ResponseEntity<String> suspenderParcialmenteSolicitacao(Long solicitacaoId, OperacaoSolicitacaoRequestDto request) {
        return suspenderParcialmenteSolicitacao(solicitacaoId, request, (String) null);
    }

    // --- Sandbox FCM ---

    public synchronized DispositivoNotificacaoResponse registrarDispositivo(String matricula, RegistrarDispositivoRequest request) {
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matricula), "SANDBOX_REGISTRAR_DISPOSITIVO_FCM");
        String idStr = request.identificador() != null && !request.identificador().isBlank()
                ? request.identificador().trim()
                : "sandbox-fcm-" + UUID.randomUUID().toString().substring(0, 8);

        LocalDateTime now = LocalDateTime.now();
        DispositivoFcm dispositivo = new DispositivoFcm(
                nextDispositivoId.incrementAndGet(),
                idStr,
                matricula,
                request.tokenFcm(),
                "sandbox-token-hash",
                request.plataforma() != null ? request.plataforma().toUpperCase() : "ANDROID",
                true,
                now,
                now,
                now);

        List<DispositivoFcm> lista = dispositivosPorMatricula.computeIfAbsent(matricula, k -> new ArrayList<>());
        lista.removeIf(d -> d.identificador().equalsIgnoreCase(idStr));
        lista.add(dispositivo);

        return new DispositivoNotificacaoResponse(
                dispositivo.identificador(),
                dispositivo.plataforma(),
                dispositivo.ativo(),
                dispositivo.dataCriacao(),
                dispositivo.dataUltimaAtualizacao(),
                dispositivo.dataUltimoUso());
    }

    public synchronized boolean desativarDispositivo(String matricula, String identificador) {
        auditoria.registrar(OrigemConta.UAT, perfilPorMatricula(matricula), "SANDBOX_DESATIVAR_DISPOSITIVO_FCM");
        List<DispositivoFcm> lista = dispositivosPorMatricula.get(matricula);
        if (lista == null) return false;
        boolean found = false;
        LocalDateTime now = LocalDateTime.now();
        List<DispositivoFcm> atualizada = new ArrayList<>();
        for (DispositivoFcm d : lista) {
            if (d.identificador().equalsIgnoreCase(identificador)) {
                atualizada.add(new DispositivoFcm(
                        d.id(), d.identificador(), d.servidorMatricula(), d.tokenFcm(),
                        d.tokenFcmHash(), d.plataforma(), false, d.dataCriacao(),
                        now, d.dataUltimoUso()));
                found = true;
            } else {
                atualizada.add(d);
            }
        }
        dispositivosPorMatricula.put(matricula, atualizada);
        return found;
    }

    private PerfilFuncional perfilPorMatricula(String matricula) {
        return "900001".equals(matricula) ? PerfilFuncional.MEMBRO : PerfilFuncional.SERVIDOR;
    }
}
