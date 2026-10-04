package mppb.restapi.mobile.feature.synthetic.dataset;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mppb.restapi.mobile.feature.carteirafuncional.dto.CarteiraFuncionalTO;
import mppb.restapi.mobile.feature.contracheque.dto.CompetenciaDto;
import mppb.restapi.mobile.feature.contracheque.dto.ContrachequeResponseDto;
import mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto;
import mppb.restapi.mobile.feature.designacao.dto.DesignacaoAtivaItemResponse;
import mppb.restapi.mobile.feature.designacao.dto.DesignacoesAtivasResponse;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentoItemDto;
import mppb.restapi.mobile.feature.pagamentos.dto.PagamentosResponseDto;
import mppb.restapi.mobile.feature.perfil.dto.PerfilUsuarioDto;
import mppb.restapi.mobile.feature.synthetic.audit.AuditoriaUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.auth.AutenticadorUsuarioSintetico;
import mppb.restapi.mobile.feature.synthetic.model.IdentidadeSintetica;
import mppb.restapi.mobile.feature.synthetic.model.OrigemConta;
import mppb.restapi.mobile.feature.synthetic.model.PerfilFuncional;

@Slf4j
@Service
@ConditionalOnProperty(name = "uat.synthetic-users.enabled", havingValue = "true")
@RequiredArgsConstructor
public class SyntheticDataService {

    public static final String HASH_MD5_MEMBRO = "a1b2c3d4e5f600112233445566778899";
    public static final String HASH_MD5_SERVIDOR = "b2c3d4e5f6a111223344556677889900";

    private final AutenticadorUsuarioSintetico autenticador;
    private final AuditoriaUsuarioSintetico auditoria;

    private byte[] fotoMembroBytes;
    private byte[] fotoServidorBytes;

    public boolean isHabilitado() {
        return autenticador.isHabilitado();
    }

    public boolean isSyntheticToken(String token) {
        return autenticador.isTokenSintetico(token);
    }

    public boolean isSyntheticLogin(String login) {
        return autenticador.isLoginSintetico(login);
    }

    public boolean isSyntheticMatricula(String matricula) {
        return autenticador.isMatriculaSintetica(matricula);
    }

    public boolean isSyntheticMatricula(Integer matricula) {
        return autenticador.isMatriculaSintetica(matricula);
    }

    public boolean isSyntheticCpf(String cpf) {
        return autenticador.isCpfSintetico(cpf);
    }

    public boolean isSyntheticHash(String hash) {
        if (!isHabilitado() || hash == null) return false;
        return HASH_MD5_MEMBRO.equalsIgnoreCase(hash.trim())
                || HASH_MD5_SERVIDOR.equalsIgnoreCase(hash.trim());
    }

    public boolean isSyntheticCarteira(CarteiraFuncionalTO carteira) {
        if (carteira == null) return false;
        return isSyntheticLogin(carteira.getLogin())
                || isSyntheticMatricula(carteira.getMatricula())
                || isSyntheticHash(carteira.getMd5());
    }

    public boolean isSyntheticTokenOrLogin(String tokenOrLogin) {
        if (tokenOrLogin == null || tokenOrLogin.isBlank()) return false;
        return isSyntheticToken(tokenOrLogin) || isSyntheticLogin(tokenOrLogin);
    }

    public String getMatriculaByTokenOrLogin(String tokenOrLogin) {
        if (tokenOrLogin == null || tokenOrLogin.isBlank()) return null;
        if (isSyntheticToken(tokenOrLogin)) {
            return getMatriculaByToken(tokenOrLogin);
        }
        if (isSyntheticLogin(tokenOrLogin)) {
            return getMatriculaByLogin(tokenOrLogin);
        }
        return null;
    }

    public String getMatriculaByToken(String token) {
        return autenticador.validarToken(token)
                .map(IdentidadeSintetica::getMatricula)
                .orElse(null);
    }

    public String getMatriculaByLogin(String login) {
        return IdentidadeSintetica.porLogin(login)
                .map(IdentidadeSintetica::getMatricula)
                .orElse(null);
    }

    public Integer getMatriculaNumerica(String login) {
        return IdentidadeSintetica.porLogin(login)
                .map(IdentidadeSintetica::getMatriculaNumerica)
                .orElse(null);
    }

    public String getCpfByLogin(String login) {
        return IdentidadeSintetica.porLogin(login)
                .map(IdentidadeSintetica::getCpfSemFormatacao)
                .orElse(null);
    }

    public PerfilUsuarioDto getPerfilByToken(String token) {
        Optional<IdentidadeSintetica> optIdentidade = autenticador.validarToken(token);
        if (optIdentidade.isEmpty()) return null;
        IdentidadeSintetica identidade = optIdentidade.get();
        auditoria.registrar(identidade, "CONSULTA_PERFIL");
        return buildPerfil(identidade);
    }

    public PerfilUsuarioDto getPerfilByLogin(String login) {
        Optional<IdentidadeSintetica> optIdentidade = IdentidadeSintetica.porLogin(login);
        if (optIdentidade.isEmpty()) return null;
        IdentidadeSintetica identidade = optIdentidade.get();
        auditoria.registrar(identidade, "CONSULTA_PERFIL");
        return buildPerfil(identidade);
    }

    public CarteiraFuncionalTO getCarteiraByLogin(String login) {
        Optional<IdentidadeSintetica> optIdentidade = IdentidadeSintetica.porLogin(login);
        if (optIdentidade.isEmpty()) return null;
        IdentidadeSintetica identidade = optIdentidade.get();
        auditoria.registrar(identidade, "CONSULTA_CARTEIRA");
        return buildCarteira(identidade);
    }

    public CarteiraFuncionalTO getCarteiraByHash(String hash) {
        if (HASH_MD5_MEMBRO.equalsIgnoreCase(hash)) {
            auditoria.registrar(IdentidadeSintetica.UAT_MEMBRO, "CONSULTA_CARTEIRA_POR_HASH");
            return buildCarteira(IdentidadeSintetica.UAT_MEMBRO);
        } else if (HASH_MD5_SERVIDOR.equalsIgnoreCase(hash)) {
            auditoria.registrar(IdentidadeSintetica.UAT_SERVIDOR, "CONSULTA_CARTEIRA_POR_HASH");
            return buildCarteira(IdentidadeSintetica.UAT_SERVIDOR);
        }
        return null;
    }

    public byte[] getFotoByLogin(String login) {
        Optional<IdentidadeSintetica> optIdentidade = IdentidadeSintetica.porLogin(login);
        if (optIdentidade.isEmpty()) return getFotoMembro();
        IdentidadeSintetica identidade = optIdentidade.get();
        auditoria.registrar(identidade, "CONSULTA_FOTO");
        return identidade.getPerfil() == PerfilFuncional.MEMBRO ? getFotoMembro() : getFotoServidor();
    }

    public List<CompetenciaDto> getCompetencias(Integer matricula) {
        auditoria.registrar(OrigemConta.UAT, matricula != null && matricula.equals(900001) ? PerfilFuncional.MEMBRO : PerfilFuncional.SERVIDOR, "CONSULTA_COMPETENCIAS_CONTRACHEQUE");
        List<CompetenciaDto> list = new ArrayList<>();
        list.add(new CompetenciaDto(9, 2026));
        list.add(new CompetenciaDto(8, 2026));
        list.add(new CompetenciaDto(7, 2026));
        return list;
    }

    public ContrachequeResponseDto getContracheque(Integer matricula, Integer mes, Integer ano) {
        boolean isMembro = matricula != null && matricula.equals(900001);
        auditoria.registrar(OrigemConta.UAT, isMembro ? PerfilFuncional.MEMBRO : PerfilFuncional.SERVIDOR,
                "CONSULTA_DETALHE_CONTRACHEQUE_" + mes + "_" + ano);
        return isMembro ? buildContrachequeMembro(mes, ano) : buildContrachequeServidor(mes, ano);
    }

    public List<Integer> getAnosPagamento(String cpf) {
        boolean isMembro = cpf != null && (cpf.contains("001") || cpf.contains("191"));
        auditoria.registrar(OrigemConta.UAT, isMembro ? PerfilFuncional.MEMBRO : PerfilFuncional.SERVIDOR, "CONSULTA_EXERCICIOS_PAGAMENTO");
        return List.of(2026, 2025);
    }

    public PagamentosResponseDto getPagamentos(String exercicio, String cpf) {
        boolean isMembro = cpf != null && (cpf.contains("001") || cpf.contains("191"));
        auditoria.registrar(OrigemConta.UAT, isMembro ? PerfilFuncional.MEMBRO : PerfilFuncional.SERVIDOR,
                "CONSULTA_PAGAMENTOS_EXERCICIO_" + exercicio);

        PagamentosResponseDto dto = new PagamentosResponseDto();
        dto.setNuExercicio(exercicio);

        if (!"2026".equals(exercicio) && !"2025".equals(exercicio)) {
            dto.setPagamentos(new ArrayList<>());
            dto.setTotalPagamentos(0);
            dto.setTotalBruto(BigDecimal.ZERO);
            dto.setTotalLiquido(BigDecimal.ZERO);
            return dto;
        }

        List<PagamentoItemDto> itens = new ArrayList<>();
        if ("2026".equals(exercicio)) {
            itens.add(buildPagamentoItem(exercicio, "FOLHA_09_2026", "20/09/2026",
                    isMembro ? new BigDecimal("34889.11") : new BigDecimal("13159.22"),
                    isMembro ? new BigDecimal("22922.51") : new BigDecimal("9369.52"),
                    "Folha Mensal - Setembro/2026"));
            itens.add(buildPagamentoItem(exercicio, "FOLHA_08_2026", "20/08/2026",
                    isMembro ? new BigDecimal("34889.11") : new BigDecimal("13159.22"),
                    isMembro ? new BigDecimal("22922.51") : new BigDecimal("9369.52"),
                    "Folha Mensal - Agosto/2026"));
        } else {
            itens.add(buildPagamentoItem(exercicio, "FOLHA_12_2025", "20/12/2025",
                    isMembro ? new BigDecimal("34889.11") : new BigDecimal("13159.22"),
                    isMembro ? new BigDecimal("22922.51") : new BigDecimal("9369.52"),
                    "Folha Mensal - Dezembro/2025"));
        }

        dto.setPagamentos(itens);
        dto.setTotalPagamentos(itens.size());
        BigDecimal totalBruto = itens.stream().map(PagamentoItemDto::getVlBruto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalLiquido = itens.stream().map(PagamentoItemDto::getVlLiquido).reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setTotalBruto(totalBruto);
        dto.setTotalLiquido(totalLiquido);

        return dto;
    }

    public DesignacoesAtivasResponse getDesignacoesAtivas(String matricula) {
        boolean isMembro = matricula != null && (matricula.equals("900001") || matricula.contains("001"));
        auditoria.registrar(OrigemConta.UAT, isMembro ? PerfilFuncional.MEMBRO : PerfilFuncional.SERVIDOR, "CONSULTA_DESIGNACOES_ATIVAS");

        DesignacoesAtivasResponse response = new DesignacoesAtivasResponse();
        if (isMembro) {
            response.setTipoUsuario("MEMBRO");
            List<DesignacaoAtivaItemResponse> itens = new ArrayList<>();
            itens.add(new DesignacaoAtivaItemResponse(
                    101L,
                    "T",
                    "Titularidade",
                    "Promotoria de Justiça de Homologação",
                    "Titular",
                    "Promotoria de Justiça de Homologação",
                    "Promotor de Justiça",
                    LocalDate.of(2025, 1, 1),
                    null,
                    "Ato PGJ nº 101/2025",
                    LocalDate.of(2024, 12, 28)));
            response.setDesignacoes(itens);
        } else {
            response.setTipoUsuario("SERVIDOR");
            response.setDesignacoes(new ArrayList<>());
        }
        return response;
    }

    // --- Private Builders ---

    private PerfilUsuarioDto buildPerfil(IdentidadeSintetica id) {
        PerfilUsuarioDto dto = new PerfilUsuarioDto();
        dto.setLogin(id.getLogin());
        dto.setNome(id.getNome());
        dto.setMatricula(id.getMatricula());
        dto.setEmail(id.getEmail());
        dto.setSituacao("Ativo");

        if (id.getPerfil() == PerfilFuncional.MEMBRO) {
            dto.setTipoCargo("MEMBRO");
            dto.setCargo("Promotor de Justiça");
            dto.setTituloCargo("Promotor de Justiça Substituto");
            dto.setEspecialidade("Geral");
            dto.setDataPosse(toDate(LocalDate.of(2018, 2, 15)));
            dto.setDataAdmissao(toDate(LocalDate.of(2018, 2, 15)));
            dto.setDataExercicio(toDate(LocalDate.of(2018, 2, 15)));
            dto.setTelefone("(83) 3100-0001");
            dto.setCelular("(83) 99100-0001");
            dto.setSetorNome("Promotoria de Justiça de Homologação");
            dto.setSetorSigla("PJ-HOMOLOG");
            dto.setOrgaoNome("Ministério Público da Paraíba");
            dto.setOrgaoSigla("MPPB");
        } else {
            dto.setTipoCargo("SERVIDOR");
            dto.setCargo("Analista Ministerial");
            dto.setTituloCargo("Analista Ministerial - TI");
            dto.setEspecialidade("Desenvolvimento de Sistemas");
            dto.setDataPosse(toDate(LocalDate.of(2020, 8, 1)));
            dto.setDataAdmissao(toDate(LocalDate.of(2020, 8, 1)));
            dto.setDataExercicio(toDate(LocalDate.of(2020, 8, 1)));
            dto.setTelefone("(83) 3100-0002");
            dto.setCelular("(83) 99100-0002");
            dto.setSetorNome("DITEC — Ambiente de Homologação");
            dto.setSetorSigla("DITEC-HOMOLOG");
            dto.setOrgaoNome("Ministério Público da Paraíba");
            dto.setOrgaoSigla("MPPB");
        }
        return dto;
    }

    private CarteiraFuncionalTO buildCarteira(IdentidadeSintetica id) {
        CarteiraFuncionalTO to = new CarteiraFuncionalTO();
        to.setNome(id.getNome());
        to.setLogin(id.getLogin());
        to.setMatricula(id.getMatricula());
        to.setCpf(id.getCpfFormatado());
        to.setNomePgj("DADOS SINTÉTICOS — UAT SEM VALOR FUNCIONAL");

        if (id.getPerfil() == PerfilFuncional.MEMBRO) {
            to.setCargo("Promotor de Justiça");
            to.setTipoCargo("MEMBRO");
            to.setTituloCargo("Promotor de Justiça Substituto");
            to.setTitulo("Dr.");
            to.setEspecialidade("Geral");
            to.setRg("9.000.001-SSP/PB");
            to.setOrgaoExp("SSP/PB");
            to.setNaturalidade("João Pessoa/PB");
            to.setNascimento(toDate(LocalDate.of(1985, 6, 10)));
            to.setNomePai("José Monteiro Sintético");
            to.setNomeMae("Ana Almeida Monteiro Sintética");
            to.setDataPosse(toDate(LocalDate.of(2018, 2, 15)));
            to.setMd5(HASH_MD5_MEMBRO);
            to.setIdPessoa(900001L);
            to.setIdServidor(900001L);
        } else {
            to.setCargo("Analista Ministerial");
            to.setTipoCargo("SERVIDOR");
            to.setTituloCargo("Analista Ministerial - TI");
            to.setTitulo("Sra.");
            to.setEspecialidade("Desenvolvimento de Sistemas");
            to.setRg("9.000.002-SSP/PB");
            to.setOrgaoExp("SSP/PB");
            to.setNaturalidade("Campina Grande/PB");
            to.setNascimento(toDate(LocalDate.of(1992, 11, 20)));
            to.setNomePai("Carlos Ribeiro Sintético");
            to.setNomeMae("Helena Costa Ribeiro Sintética");
            to.setDataPosse(toDate(LocalDate.of(2020, 8, 1)));
            to.setMd5(HASH_MD5_SERVIDOR);
            to.setIdPessoa(900002L);
            to.setIdServidor(900002L);
        }
        return to;
    }

    private ContrachequeResponseDto buildContrachequeMembro(Integer mes, Integer ano) {
        ContrachequeResponseDto dto = new ContrachequeResponseDto();
        dto.setNome(IdentidadeSintetica.UAT_MEMBRO.getNome());
        dto.setMatricula(IdentidadeSintetica.UAT_MEMBRO.getMatricula());
        dto.setCargo("Promotor de Justiça");
        dto.setMesAno(String.format("%02d/%04d", mes, ano));

        List<LancamentoDto> lancamentos = new ArrayList<>();
        lancamentos.add(buildLancamento("101", "SUBSIDIO MEMBRO", new BigDecimal("33689.11"), "V"));
        lancamentos.add(buildLancamento("201", "AUXILIO ALIMENTACAO", new BigDecimal("1200.00"), "V"));
        lancamentos.add(buildLancamento("501", "PREVIDENCIA - PBPREV", new BigDecimal("4716.48"), "D"));
        lancamentos.add(buildLancamento("502", "IMPOSTO DE RENDA RETIDO NA FONTE", new BigDecimal("7250.12"), "D"));

        dto.setLancamentos(lancamentos);
        dto.setTotalVantagens(new BigDecimal("34889.11"));
        dto.setTotalDescontos(new BigDecimal("11966.60"));
        dto.setSalarioLiquido(new BigDecimal("22922.51"));
        dto.setCodigoValidacao("UAT-SYNTH-" + ano + String.format("%02d", mes) + "-SEM-VALOR-FUNCIONAL");

        return dto;
    }

    private ContrachequeResponseDto buildContrachequeServidor(Integer mes, Integer ano) {
        ContrachequeResponseDto dto = new ContrachequeResponseDto();
        dto.setNome(IdentidadeSintetica.UAT_SERVIDOR.getNome());
        dto.setMatricula(IdentidadeSintetica.UAT_SERVIDOR.getMatricula());
        dto.setCargo("Analista Ministerial");
        dto.setMesAno(String.format("%02d/%04d", mes, ano));

        List<LancamentoDto> lancamentos = new ArrayList<>();
        lancamentos.add(buildLancamento("102", "VENCIMENTO BASICO", new BigDecimal("8542.30"), "V"));
        lancamentos.add(buildLancamento("103", "GAJ - GRATIF. ATIVIDADE JUDICIARIA", new BigDecimal("3416.92"), "V"));
        lancamentos.add(buildLancamento("201", "AUXILIO ALIMENTACAO", new BigDecimal("1200.00"), "V"));
        lancamentos.add(buildLancamento("501", "PREVIDENCIA - PBPREV", new BigDecimal("1674.30"), "D"));
        lancamentos.add(buildLancamento("502", "IMPOSTO DE RENDA RETIDO NA FONTE", new BigDecimal("2115.40"), "D"));

        dto.setLancamentos(lancamentos);
        dto.setTotalVantagens(new BigDecimal("13159.22"));
        dto.setTotalDescontos(new BigDecimal("3789.70"));
        dto.setSalarioLiquido(new BigDecimal("9369.52"));
        dto.setCodigoValidacao("UAT-SYNTH-" + ano + String.format("%02d", mes) + "-SEM-VALOR-FUNCIONAL");

        return dto;
    }

    private LancamentoDto buildLancamento(String codigo, String descricao, BigDecimal valor, String tipo) {
        LancamentoDto l = new LancamentoDto();
        l.setCodigo(codigo);
        l.setDescricao(descricao);
        l.setValor(valor);
        l.setTipo(tipo);
        return l;
    }

    private PagamentoItemDto buildPagamentoItem(String exercicio, String doc, String data,
                                                BigDecimal bruto, BigDecimal liquido, String desc) {
        PagamentoItemDto p = new PagamentoItemDto();
        p.setNuExercicio(exercicio);
        p.setDocumento(doc);
        p.setDtPagamento(data);
        p.setVlBruto(bruto);
        p.setVlLiquido(liquido);
        p.setDescricaoElemento(desc);
        p.setDsModalidadePag("ORDINARIO");
        p.setCdOrgao("40.101");
        p.setSigla("PGJ");
        p.setNuProcesso("MPPB-HOMOLOG-2026/001");
        p.setCdElemento("319011");
        return p;
    }

    public synchronized byte[] getFotoMembro() {
        if (fotoMembroBytes == null) {
            fotoMembroBytes = gerarFotoSintetica("LM", "Lucas Monteiro", new Color(30, 58, 138));
        }
        return fotoMembroBytes;
    }

    public synchronized byte[] getFotoServidor() {
        if (fotoServidorBytes == null) {
            fotoServidorBytes = gerarFotoSintetica("MR", "Mariana Ribeiro", new Color(15, 118, 110));
        }
        return fotoServidorBytes;
    }

    private byte[] gerarFotoSintetica(String iniciais, String nomeExibicao, Color corFundo) {
        try {
            int width = 300;
            int height = 300;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(corFundo);
            g.fillRect(0, 0, width, height);

            g.setColor(new Color(255, 255, 255, 30));
            g.fillOval(40, 30, 220, 220);

            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 72));
            int strWidth = g.getFontMetrics().stringWidth(iniciais);
            g.drawString(iniciais, (width - strWidth) / 2, 145);

            g.setFont(new Font("SansSerif", Font.PLAIN, 16));
            int nameWidth = g.getFontMetrics().stringWidth(nomeExibicao);
            g.drawString(nomeExibicao, (width - nameWidth) / 2, 200);

            g.setColor(new Color(255, 255, 255, 180));
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            String badge = "USUÁRIO SINTÉTICO UAT";
            int badgeWidth = g.getFontMetrics().stringWidth(badge);
            g.drawString(badge, (width - badgeWidth) / 2, 240);

            g.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.warn("Nao foi possivel gerar foto sintetica proceduralmente. Usando fallback 1x1.", e);
            return new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, (byte)0x00, (byte)0x10,
                    0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x01, 0x00, 0x48, 0x00, 0x48, 0x00, 0x00,
                    (byte)0xFF, (byte)0xDB, 0x00, 0x43, 0x00, (byte)0xFF, (byte)0xC0, 0x00, 0x0B, 0x08,
                    0x00, 0x01, 0x00, 0x01, 0x01, 0x01, 0x11, 0x00, (byte)0xFF, (byte)0xC4, 0x00, 0x14,
                    0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x09, (byte)0xFF, (byte)0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00,
                    0x3F, 0x00, (byte)0xBF, (byte)0xFF, (byte)0xD9 };
        }
    }

    private Date toDate(LocalDate date) {
        if (date == null) return null;
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
