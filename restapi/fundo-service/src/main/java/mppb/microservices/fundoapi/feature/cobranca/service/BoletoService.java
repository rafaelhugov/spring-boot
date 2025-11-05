package mppb.microservices.fundoapi.feature.cobranca.service;

import com.google.zxing.WriterException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.log4j.Log4j2;

import mppb.microservices.common.core.error.AppException;
import mppb.microservices.common.core.error.BusinessRuleException;
import mppb.microservices.common.core.error.NotFoundException;
import mppb.microservices.common.core.error.ValidationException;
import mppb.microservices.common.core.error.feign.ClientApiException;
import mppb.microservices.common.infrastructure.report.JasperService;
import mppb.microservices.common.core.service.AbstractService;
import mppb.microservices.common.core.util.NumberUtil;
import mppb.microservices.common.core.util.PdfUtil;
import mppb.microservices.fundoapi.infrastructure.client.bb.api.BbCobrancaFddService;
import mppb.microservices.fundoapi.infrastructure.client.bb.api.BbCobrancaFedcService;
import mppb.microservices.fundoapi.infrastructure.client.bb.dto.request.BbBoletoRequestDto;
import mppb.microservices.fundoapi.infrastructure.client.bb.dto.request.BbPagadorDto;
import mppb.microservices.fundoapi.infrastructure.client.bb.dto.response.BbBoletoResponseDto;
import mppb.microservices.fundoapi.feature.cobranca.dto.ValorCorrigidoDto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Boleto;
import mppb.microservices.fundoapi.feature.cobranca.entity.Fundo;
import mppb.microservices.fundoapi.feature.cobranca.entity.Solicitacao;
import mppb.microservices.fundoapi.feature.cobranca.enums.TipoSolicitacaoEnum;
import mppb.microservices.fundoapi.feature.cobranca.repository.BoletoRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;

import javax.imageio.ImageIO;

@Service
@Log4j2
public class BoletoService extends AbstractService<Boleto, Long> {

    private final BoletoRepository boletoRepository;

    private final EntityManager entityManager;
    private final BbCobrancaFedcService bbCobrancaFedcService;
    private final BbCobrancaFddService bbCobrancaFddService;
    private final RefisService refisService;

    private final JasperService jasperService;


    public BoletoService(EntityManager entityManager, BoletoRepository boletoRepository, BbCobrancaFddService bbCobrancaFddService, BbCobrancaFedcService bbCobrancaFedcService, RefisService refisService, JasperService jasperService){
        super(entityManager, boletoRepository);
        this.entityManager = entityManager;
        this.boletoRepository = boletoRepository;
        this.bbCobrancaFddService = bbCobrancaFddService;
        this.bbCobrancaFedcService = bbCobrancaFedcService;
        this.jasperService = jasperService;
        this.refisService = refisService;
    }

    public Optional<Boleto> findByIdentificador(UUID  uuid){
        return boletoRepository.findByIdentificador(uuid.toString());
    }


    public byte[] gerarBoletos(Solicitacao solicitacao) {
        List<Boleto> boletos = solicitacao.getBoletos();

        try {

            List<byte[]> bytesList = new ArrayList<byte[]>();
            for(Boleto boleto : boletos) {
                byte[] bytes = this.gerarBoleto(boleto);
                bytesList.add(bytes);
            }


            byte[] bytesConcatenados = PdfUtil.concatenaPdfs(bytesList);
            return bytesConcatenados;
        } catch (Exception e) {
            log.error(e);
            throw new AppException(e);
        }
    }

    public byte[] gerarBoleto(Boleto boleto) {
        atualizarValoresBoletoSeNecessario(boleto);

        if(podeCadastrarNoBancoDoBrasil(boleto)) {
            BbBoletoResponseDto response = this.gerarBoletoNoBanco(boleto);
            log.info(response);

        }

        if(boleto.isFaltaPersistir()){
            save(boleto);
        }

        byte[] pdf = gerarPdfBoleto(boleto);
        return pdf;
    }

    private boolean estaSalvoNoBancoDoBrasil(Boleto boleto){
        return boleto.getPagamentoLinhaDigitavel() != null && !boleto.getPagamentoLinhaDigitavel().isBlank();
    }

    private boolean podeCadastrarNoBancoDoBrasil(Boleto boleto) {
        boolean foiSalvoNoBancoDoBrasil = estaSalvoNoBancoDoBrasil(boleto);

        if(foiSalvoNoBancoDoBrasil){
            return false;
        }

        Boolean refis = boleto.getSolicitacao().getRefis();

        if(refis != null && refis){

            LocalDate dataInicioGeracao = boleto.getDataInicialGeracao();
            if(dataInicioGeracao == null){
                return false; // nao permite cadastrar caso nao tenha chegado a data ainda.
            }

            LocalDate hoje = LocalDate.now();
            boolean podeGerarBoleto = dataInicioGeracao.isBefore(hoje) || dataInicioGeracao.isEqual(hoje);

            return podeGerarBoleto;
        } else {
            // se nao for refis, pode cadastrar no banco do brasil pq nao precisa esperar pela data inicial de geracao.
            return true;
        }
    }

    private byte[] gerarPdfBoleto(Boleto boleto) {
        Map<String, Object> parametros = new HashMap<>();
        fillParams(boleto, parametros);
        byte[] pdf = jasperService.gerarPdfEmptyDatasource("boleto.jasper", parametros);

        return pdf;
    }

    private BbBoletoResponseDto gerarBoletoNoBanco(Boleto boleto) {
        BbBoletoRequestDto requestDto = fillupRequestDto(boleto);
        BbBoletoResponseDto response = gerarBoletoBbCobranca(boleto, requestDto);

        if(response == null ) {
            throw new ClientApiException("Sem resposta do BB.");
        }
        boleto.setDataAtualizacaoValores(LocalDateTime.now());
        persistirBoleto(boleto, response);
        return response;
    }

    private void atualizarValoresBoletoSeNecessario(Boleto boleto) {

        if(boleto.getDataPagamento() != null) {
            // já foi pago, não há o que atualizar para fazer pagamento.
            return ;
        }

    //    boolean passouTempoSuficienteParaNovaRequisicaoDeAtualizacao = boleto.getDataAtualizacaoValores() != null && boleto.getDataAtualizacaoValores().isAfter(LocalDateTime.now().minusMinutes(30));

        //  if(!passouTempoSuficienteParaNovaRequisicaoDeAtualizacao){
            // não pode atualizar se faz menos de 30 minutos desde última atualização do boleto
        //  log.info("Data de atualização menor que 30 minutos.");
        //   return ;
        //}

        if(boleto.getSolicitacao().getRefis() != null && boleto.getSolicitacao().getRefis()){
            boolean dataGeracaoNoPassado = boleto.getDataInicialGeracao() == null || boleto.getDataInicialGeracao().isBefore(LocalDate.now()) || boleto.getDataInicialGeracao().equals(LocalDate.now());

            if(dataGeracaoNoPassado){
                atualizarValor(boleto);
            } else {
                String dataInicialGeracaoFormatada = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy").format(boleto.getDataInicialGeracao());
                throw new BusinessRuleException("Este boleto só pode ser gerado a partir de " + dataInicialGeracaoFormatada);
            }
        }

        /* Calcular o novo valor do boleto. Para isso preciso pegar o valor original do boleto vencido
           e somar uma "multa", que será no valor da taxa SELIC entre a data de atualizacao de valores
           e a data atual. Se a data de atualizacao for null, ela deve ser atualizada para a data atual e
           a multa calculada usando a data inicial geração no lugar.
         */
        // TODO frontend: data de adesao. OK
        // TODO frontend: refis (boolean).  OK
        // TODO frontend: link direto para tela que filtra por CNPJ/CPF.
        // TODO frontend: deixar geração de boleto cinza se datainiciogeracao nao tiver chegado ainda (se for diferente de null).
        // TODO backend: integrar código do serviço da SELIC.
        // TODO backend: permitir imprimir boleto incompleto faltando codigo de barras, qrcode, etc.

    }

    private void atualizarValor(Boleto boleto) {
        boolean estaSalvoNoBancoDoBrasil = estaSalvoNoBancoDoBrasil(boleto);
        boolean vencido = estaVencido(boleto);

        if(estaSalvoNoBancoDoBrasil && !vencido){
            log.info("Boleto " + boleto.getNossoNumero() + " não vencido e salvo: não atualizar vencimento ou valores.");
            return ;
        }

        boolean refis = boleto.getSolicitacao().getRefis() != null && boleto.getSolicitacao().getRefis();
        if(refis){
            enviarRequestsAtualizarValor(boleto);
        }

        if(vencido){
            enviarRequestsAtualizarVencimento(boleto);
        }
    }

    private void enviarRequestsAtualizarVencimento(Boleto boleto) {

        LocalDate hoje = LocalDate.now();
        LocalDate amanha = hoje.plusDays(1);
        LocalDate novoVencimento =  amanha.getMonth() == hoje.getMonth() ? amanha : hoje;

        LocalDate vencimento = boleto.getDataVencimento();
        boolean estaSalvoNoBancoDoBrasil = estaSalvoNoBancoDoBrasil(boleto);
        boolean vencimentoMudou = !vencimento.equals(novoVencimento);

        if(vencimentoMudou){
            boleto.setDataVencimento(novoVencimento);
            boleto.setFaltaPersistir(true);

            if(estaSalvoNoBancoDoBrasil){
                if(boleto.getSolicitacao().getTipo().equals(TipoSolicitacaoEnum.FEDC)){
                    bbCobrancaFedcService.alterarDataVencimento(boleto.getNossoNumero(), boleto.getSolicitacao().getFundo().getConvenio(), novoVencimento);
                } else if(boleto.getSolicitacao().getTipo().equals(TipoSolicitacaoEnum.FDD)){
                    bbCobrancaFddService.alterarDataVencimento(boleto.getNossoNumero(), boleto.getSolicitacao().getFundo().getConvenio(), novoVencimento);
                } else {
                    throw new NotFoundException("Tipo de solicitação não existente.");
                }
                save(boleto);
                boleto.setFaltaPersistir(false);
            } else {
                boleto.setFaltaPersistir(true);
            }
        }
    }

    private void  enviarRequestsAtualizarValor(Boleto boleto) {
        boolean estaSalvoNoBancoDoBrasil = estaSalvoNoBancoDoBrasil(boleto);
        ValorCorrigidoDto valorCorrigidoDto = calcularValorCorrigido(boleto);

        Double valorAtual = boleto.getValor();
        Double novoValor = valorCorrigidoDto.getValorFinalCorrigido();

        //if(boleto.getNossoNumero().equals("00031285570056562737")){
            //    novoValor = 90.0;
        //}
        boolean valorMudou = ehValorDiferente(valorAtual, novoValor);

        if(valorMudou){
            boleto.setValor(novoValor);
            //boleto.setDataAtualizacaoValores(LocalDateTime.now());
            boleto.setFaltaPersistir(true);

            if(estaSalvoNoBancoDoBrasil) {
                if(boleto.getSolicitacao().getTipo().equals(TipoSolicitacaoEnum.FEDC)){
                    bbCobrancaFedcService.alterarValorNominal(boleto.getNossoNumero(), boleto.getSolicitacao().getFundo().getConvenio(), novoValor);
                } else if(boleto.getSolicitacao().getTipo().equals(TipoSolicitacaoEnum.FDD)){
                    bbCobrancaFddService.alterarValorNominal(boleto.getNossoNumero(), boleto.getSolicitacao().getFundo().getConvenio(), novoValor);
                } else {
                    throw new NotFoundException("Tipo de solicitação não existente.");
                }
                // salva caso mude o valor via api do BB
                save(boleto);
                boleto.setFaltaPersistir(false);

            } else {
                // se não estiver salvo no BB, vai ser gerado pela primeira vez mais tarde, lá que o boleto será persistido.
                boleto.setFaltaPersistir(true);
            }
        }

    }

    private boolean ehValorDiferente(Double valorAtual, Double novoValor) {
        BigDecimal valorAtualBig = BigDecimal.valueOf(valorAtual).setScale(2, RoundingMode.HALF_UP);
        BigDecimal novoValorBig = BigDecimal.valueOf(novoValor).setScale(2, RoundingMode.HALF_UP);
        boolean valorMudou = !valorAtualBig.equals(novoValorBig);

        return valorMudou;
    }

    private boolean estaVencido(Boleto boleto) {
        return boleto.getDataVencimento().isBefore(LocalDate.now());
    }

    private ValorCorrigidoDto calcularValorCorrigido(Boleto boleto) {
        /*
         * "§ 1º As parcelas serão corrigidas com base na taxa referencial SELIC, acumulada
         * mensalmente, calculada a partir do primeiro dia do mês subsequente à adesão até o mês
         * anterior ao da liquidação, acrescida de 1% (um por cento) no mês do pagamento."
        */
        LocalDate primeiroDiaMesSubsequenteAdesao = boleto.getSolicitacao().getDataAdesao().withDayOfMonth(1).plusMonths(1);
        LocalDate ultimoDiaMesAnteriorPagamento = LocalDate.now().withDayOfMonth(1).minusDays(1);

        LocalDate dataInicial = primeiroDiaMesSubsequenteAdesao;
        LocalDate dataFinal = ultimoDiaMesAnteriorPagamento;

        if(dataInicial.isAfter(dataFinal)){
            dataInicial = LocalDate.now();
            dataFinal = LocalDate.now();
        }
        Double valorOriginal = boleto.getValorOriginal();
        ValorCorrigidoDto valorCorrigidoDto = refisService.corrigirValor(valorOriginal, dataInicial, dataFinal);
        return valorCorrigidoDto;
    }

    private BbBoletoResponseDto gerarBoletoBbCobranca(Boleto boleto, BbBoletoRequestDto requestDto ) {
        BbBoletoResponseDto response = null;
        if(boleto.getSolicitacao().getTipo().equals(TipoSolicitacaoEnum.FDD)){
            response = bbCobrancaFddService.gerarBoleto(requestDto);
        } else if(boleto.getSolicitacao().getTipo().equals(TipoSolicitacaoEnum.FEDC)){
            response = bbCobrancaFedcService.gerarBoleto(requestDto);
        }

        return response;
    }

    @Transactional
    private void persistirBoleto(Boleto boleto, BbBoletoResponseDto response) {
        boleto.setPixQrcode(response.getQrCode().getEmv());
        boleto.setPixUrl(response.getQrCode().getUrl());
        boleto.setPixTxId(response.getQrCode().getTxId());
        boleto.setPagamentoCodigoBarras(response.getCodigoBarraNumerico());
        boleto.setPagamentoLinhaDigitavel(response.getLinhaDigitavel());
        this.save(boleto);
    }

    private BbBoletoRequestDto  fillupRequestDto( Boleto boleto) {

        if(boleto == null || boleto.getSolicitacao() == null || boleto.getSolicitacao().getFundo() == null){
            return null;
        }

        Fundo fundo = boleto.getSolicitacao().getFundo();

        BbBoletoRequestDto dto = new BbBoletoRequestDto();
        dto.setNumeroConvenio(fundo.getConvenio());

        dto.setNumeroCarteira(fundo.getCarteira());
        dto.setNumeroVariacaoCarteira(fundo.getVariacaoCarteira());
        dto.setCodigoModalidade(1L);

        DateTimeFormatter formatDdMmYyyy= DateTimeFormatter.ofPattern("dd.MM.yyyy");

        dto.setDataEmissao(boleto.getSolicitacao().getDataRegistro().format(formatDdMmYyyy));
        dto.setDataVencimento(boleto.getDataVencimento().format(formatDdMmYyyy));

        dto.setValorOriginal(boleto.getValor());

        dto.setCodigoAceite("A");
        dto.setCodigoTipoTitulo("04");
        dto.setDescricaoTipoTitulo("DS");
        dto.setIndicadorPermissaoRecebimentoParcial("N");
        dto.setNumeroTituloCliente(boleto.getNossoNumero());

        dto.setIndicadorPix("S"); // habilita = S
        dto.setTextoEnderecoEmail(boleto.getSolicitacao().getEmail());

        // Pagador
        BbPagadorDto pagador = convertPagador(boleto.getSolicitacao());
        dto.setPagador(pagador);

        // Beneficiario Final
//        BbBeneficiarioFinalDto beneficiarioFinal = new BbBeneficiarioFinalDto();
//        beneficiarioFinal.setTipoInscricao("2");
//        beneficiarioFinal.setNumeroInscricao("2518688000121");
//        beneficiarioFinal.setNome("CLINICADEREABILITA");
//        dto.setBeneficiarioFinal(beneficiarioFinal);

        return dto;
    }

    private BbPagadorDto convertPagador(Solicitacao solicitacao) {
        String cnpj = solicitacao.getCnpj();
        String cpf = solicitacao.getCpf();
        BbPagadorDto pagador = new BbPagadorDto();
        if(cpf != null && !cpf.isBlank()){
            pagador.setTipoInscricao(1L);
            pagador.setNumeroInscricao(Long.valueOf(retiraSimbolos(cpf)));
        } else if(cnpj != null && !cnpj.isBlank()){
            pagador.setTipoInscricao(2L);
            pagador.setNumeroInscricao(Long.valueOf(retiraSimbolos(cnpj)));
        }
        pagador.setNome(solicitacao.getNome());
        pagador.setTelefone(solicitacao.getTelefone());
//        pagador.setEndereço(solicitacao.getEndereco());
        pagador.setCidade(solicitacao.getEnderecoCidade().getNome());
        pagador.setUf(solicitacao.getEnderecoCidade().getEstado().getSigla().toUpperCase());
        return pagador;
    }

    private String retiraSimbolos(String input) {
        if (input == null) return null;
        return input.replaceAll("[./-]", "");
    }


    private void fillParams(Boleto boleto, Map<String, Object> parametros) {
        if(boleto.getSolicitacao().getFundo() == null){
            throw new BusinessRuleException("Necessário que solicitação esteja associada a um fundo.");
        }
        Fundo fundo = boleto.getSolicitacao().getFundo();

        // linha     A
        parametros.put("numeroBoleto", formataNumeroBoleto(boleto.getPagamentoLinhaDigitavel()));
        parametros.put("dataVencimento", boleto.getDataVencimento());

        // linha B
        String razaoSocial = fundo.getRazaoSocial();
        if(razaoSocial.length() > 40){
            razaoSocial  = razaoSocial.substring(0, 40);
        }

        parametros.put("beneficiarioNome", razaoSocial + " CNPJ: " + fundo.getCnpj());
        parametros.put("beneficiarioAgendaCodigo", agenciaCodigo(fundo.getAgencia(), fundo.getConta()));
        parametros.put("beneficiarioEndereco", "R RODRIGUES DE AQUINO SN, JOAO PESSOA - PB CEP: 58013030");

        // linha C
        parametros.put("dataDocumento", boleto.getSolicitacao().getDataRegistro().toLocalDate());

        if(boleto.getNossoNumero() != null){
            Long numeroDocumento = parseLongOrNull(boleto.getNossoNumero());
            parametros.put("numeroDocumento", numeroDocumento+"");
        }

        // linha D
        parametros.put("especieDoc", "DS");
        parametros.put("aceite", "N"); // não aceitar pagamento parcial
        parametros.put("dataProcessamento", boleto.getSolicitacao().getDataRegistro().toLocalDate());
        parametros.put("nossoNumero", boleto.getNossoNumero());

        // linha E
        parametros.put("usoBanco", "");
        parametros.put("carteira", fundo.getCarteira()+"");
        parametros.put("especie", "R$");
        parametros.put("quantidade", "");
        parametros.put("xValor", "");
        parametros.put("valorDocumento", boleto.getValor());

        // linha F
        parametros.put("informacoes", montarInformacoes(boleto));
        parametros.put("descontoAbatimento", null);

        // linha G
        parametros.put("jurosMulta", null);

        //Double valorCobrado = boleto.getJurosValor() == null ? 0.00 : boleto.getJurosValor() ;
        //valorCobrado += boleto.getValor();

        // linha H
        parametros.put("valorCobrado", null);

        // linha I
        if(boleto.getSolicitacao().getCpf() != null && !boleto.getSolicitacao().getCpf().isBlank()){
            parametros.put("nomePagadorLinha1", boleto.getSolicitacao().getNome()+ " CPF: " + boleto.getSolicitacao().getCpf());
        } else if(boleto.getSolicitacao().getCnpj() != null && !boleto.getSolicitacao().getCnpj().isBlank()){
            parametros.put("nomePagadorLinha1", boleto.getSolicitacao().getNome()+ " CNPJ: " + boleto.getSolicitacao().getCnpj());
        }

        parametros.put("nomePagadorLinha2", boleto.getSolicitacao().getEndereco()+ ",");
        parametros.put("nomePagadorLinha3", boleto.getSolicitacao().getEnderecoCidade().getNome()+"-"+boleto.getSolicitacao().getEnderecoEstado().getSigla() + " CEP:" + boleto.getSolicitacao().getCep());

        // linha J
        parametros.put("codigoBarras", geraImagem(boleto.getPagamentoCodigoBarras(), 600 , 50 ,  BarcodeFormat.CODE_128));
        parametros.put("pix", geraImagem(boleto.getPixQrcode(), 300 , 300 ,  BarcodeFormat.QR_CODE));

        try {
            java.awt.Image backgroundImage = ImageIO.read(new ClassPathResource("reports/img/boletofundo.png").getInputStream());
            parametros.put("backgroundImage", backgroundImage);
        } catch (IOException e) {
            throw new NotFoundException(e, "boletofundo.png não encontrado.");
        }

        try {
            java.awt.Image backgroundImage = ImageIO.read(new ClassPathResource("reports/img/recibopagador.png").getInputStream());
            parametros.put("backgroundImage2", backgroundImage);
        } catch (IOException e) {
            throw new NotFoundException(e, "recibopagador.png não encontrado.");
        }

    }

    private String agenciaCodigo(Long agencia, Long conta) {

        String agenciaCodigo = "";
        agenciaCodigo +=  agencia + "-" + NumberUtil.calcularDVModulo11(agencia);
        agenciaCodigo += " / ";
        agenciaCodigo +=  conta + "-" + NumberUtil.calcularDVModulo11(conta );

        return agenciaCodigo;
    }


    public static Long parseLongOrNull(String input) {
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String montarInformacoes(Boleto boleto) {
        Solicitacao solicitacao = boleto.getSolicitacao();


        String tipoCodigo = solicitacao.getFundo() != null ? solicitacao.getFundo().getSigla() : "";
        String tipoLabel = solicitacao.getFundo() != null ? solicitacao.getFundo().getNome() : "";
        String areaNome = solicitacao.getArea() != null ? solicitacao.getArea().getNome() : "";
        String naturezaNome = solicitacao.getNatureza() != null ? solicitacao.getNatureza().getNome() : "";
        String comarcaNome = solicitacao.getComarca() != null ? solicitacao.getComarca().getNome() : "";
        String municipioNome = solicitacao.getMunicipio() != null ? solicitacao.getMunicipio().getNome() : "";

        String objeto = Optional.ofNullable(solicitacao.getObjeto()).orElse("").trim();
        objeto = objeto.substring(0, Math.min(80, objeto.length()));

        String processoNumero = solicitacao.getNumeroProcesso() != null ? solicitacao.getNumeroProcesso() : "";

        String boletoNumeroBoletos = "";
        Long numeroBoletos = solicitacao.getNumeroBoletos();

        if(numeroBoletos != null && numeroBoletos > 1){
            boletoNumeroBoletos = boleto.getOrdem() + " / " + numeroBoletos;
        }

        String areaTexto = areaNome.equals("") ? "" : "ÁREA: " + areaNome + ". ";
        String comarcaTexto = comarcaNome.equals("") ? "" : "COMARCA DE " + comarcaNome + ". ";
        String processoNumeroTexto = processoNumero.equals("") ? "" : "N. DO PROCESSO: " + processoNumero + ". ";

        String sobreAtualizacoesValoresEVencimento = geraTextoAtualizacoes(boleto);

        return "BOLETO " + boletoNumeroBoletos + " DO PAGAMENTO DE GUIA DE " + tipoCodigo + " (" + tipoLabel + "). "
                + "MINISTÉRIO PUBLICO DO ESTADO DA PARAÍBA. "
                + areaTexto
                + "NATUREZA: " + naturezaNome + ". "
                + comarcaTexto
                + "MUNICÍPIO: " + municipioNome + ". "
                + processoNumeroTexto
                + "OBJETO: " + objeto + "." + "\n" + sobreAtualizacoesValoresEVencimento;
    }

    private String geraTextoAtualizacoes(Boleto boleto) {
        boolean valorAtualizado = ehValorDiferente(boleto.getValorOriginal(), boleto.getValor());
        boolean vencimentoAtualizado = !boleto.getDataVencimento().equals(boleto.getDataVencimentoOriginal());

        String texto = "";

        if(valorAtualizado){
            BigDecimal diferenca = BigDecimal.valueOf(boleto.getValor() - boleto.getValorOriginal()).setScale(2, RoundingMode.HALF_UP);
            String diferencaFormatada = new java.text.DecimalFormat("0.00", new java.text.DecimalFormatSymbols(new java.util.Locale("pt", "BR"))).format(diferenca);
            texto += "VALOR DO BOLETO ATUALIZADO EM R$ " + diferencaFormatada + " CONFORME PORTARIA CONJUNTA PGJ/MPPROCON N. 001/2025. ";
        }

        if(vencimentoAtualizado){
            texto += "DATA DE VENCIMENTO ATUALIZADA. ";
        }

        return (texto.length() > 0 ? "OBS: " : "") + texto;
    }

    public Image geraImagemCodigoBarras(String content)  {
        int width = 400;
        int height = 50;

        BitMatrix bitMatrix = null;
        try {
            bitMatrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.CODE_128, width, height);
        } catch (WriterException e) {
            log.error(e);
            return null;
        }

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    public Image geraImagem(String content, int width, int height, BarcodeFormat format)  {

        if(content == null || content.isBlank()){
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }

        BitMatrix bitMatrix = null;
        try {
            bitMatrix = new MultiFormatWriter()
                    .encode(content, format, width, height);
        } catch (WriterException e) {
            log.error(e);
            return null;
        }

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private String formataNumeroBoleto(String linha) {

        if(linha == null){
            return "";
        }

        if (!linha.matches("\\d{47}")) {
            throw new ValidationException("A linha digitável deve conter exatamente 47 dígitos numéricos.");
        }

        return String.format("%s.%s %s.%s %s.%s %s %s",
                linha.substring(0, 5), linha.substring(5, 10),
                linha.substring(10, 15), linha.substring(15, 20),
                linha.substring(20, 25), linha.substring(25, 30),
                linha.substring(30, 31), linha.substring(31));

    }

}
