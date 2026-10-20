package mppb.restapi.mobile.feature.contracheque.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import mppb.restapi.mobile.feature.contracheque.dto.ContrachequeResponseDto;
import mppb.restapi.mobile.feature.contracheque.dto.LancamentoDto;

@Service
public class ContrachequePdfService {

    public byte[] generatePdf(ContrachequeResponseDto dto) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                
                // Draw Header Table
                float margin = 30;
                float yStart = 760;
                float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
                float headerHeight = 150;
                
                // Outer border for header
                content.addRect(margin, yStart - headerHeight, tableWidth, headerHeight);
                content.stroke();
                
                // Vertical line for logo
                float logoColWidth = 150;
                content.moveTo(margin + logoColWidth, yStart);
                content.lineTo(margin + logoColWidth, yStart - headerHeight);
                content.stroke();
                
                // Horizontal lines in right column
                float rowHeight = 25;
                for (int i = 1; i <= 4; i++) {
                    content.moveTo(margin + logoColWidth, yStart - (i * rowHeight));
                    content.lineTo(margin + tableWidth, yStart - (i * rowHeight));
                    content.stroke();
                }
                
                // Sub-vertical lines in Mês/Ano row
                float mesY = yStart - (3 * rowHeight);
                float matX = margin + logoColWidth + 120;
                float tempX = margin + logoColWidth + 240;
                content.moveTo(matX, mesY);
                content.lineTo(matX, mesY - rowHeight);
                content.stroke();
                content.moveTo(tempX, mesY);
                content.lineTo(tempX, mesY - rowHeight);
                content.stroke();
                
                // Left Column: Image and Text
                try {
                    ClassPathResource imgRes = new ClassPathResource("images/brasao_mppb.png");
                    PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imgRes.getContentAsByteArray(), "brasao");
                    float imgWidth = 60;
                    float imgHeight = 70;
                    content.drawImage(pdImage, margin + 45, yStart - 80, imgWidth, imgHeight);
                } catch (Exception e) {
                    // Ignore if image not found
                }
                
                PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                
                drawText(content, fontRegular, 10, margin + 35, yStart - 95, "Ministerio Público");
                drawText(content, fontRegular, 10, margin + 45, yStart - 108, "da Paraíba");
                drawText(content, fontRegular, 10, margin + 40, yStart - 121, "Contra-cheque");
                
                // Right Column: Texts
                float textX = margin + logoColWidth + 5;
                drawText(content, fontRegular, 9, textX, yStart - 10, "Nome");
                drawText(content, fontBold, 10, textX, yStart - 20, dto.getNome() != null ? dto.getNome().toUpperCase() : "");
                
                drawText(content, fontRegular, 9, textX, yStart - 35, "Órgão");
                drawText(content, fontBold, 10, textX, yStart - 45, "MINISTÉRIO PÚBLICO DO ESTADO DA PARAÍBA - MPPB");
                
                drawText(content, fontRegular, 9, textX, yStart - 60, "Cargo");
                drawText(content, fontBold, 10, textX, yStart - 70, dto.getCargo() != null ? dto.getCargo().toUpperCase() : "");
                
                drawText(content, fontRegular, 9, textX, yStart - 85, "Mês/Ano");
                drawText(content, fontBold, 10, textX, yStart - 95, formatMesAno(dto.getMesAno()));
                
                drawText(content, fontRegular, 9, matX + 5, yStart - 85, "Matrícula");
                drawText(content, fontBold, 10, matX + 5, yStart - 95, dto.getMatricula() != null ? dto.getMatricula() : "");
                
                drawText(content, fontRegular, 9, tempX + 5, yStart - 85, "T. Serviço");
                drawText(content, fontRegular, 10, tempX + 5, yStart - 95, "-");
                
                drawText(content, fontRegular, 9, textX, yStart - 110, "Autenticação");
                String authCode = dto.getCodigoValidacao() != null ? dto.getCodigoValidacao() : "N/A";
                drawText(content, fontRegular, 10, textX, yStart - 122, authCode);
                
                // Generate QR Code
                try {
                    String validationUrl = "https://gep.mppb.mp.br/public/validacontracheque.jsf?codigo=" + authCode;
                    QRCodeWriter qrCodeWriter = new QRCodeWriter();
                    com.google.zxing.qrcode.QRCodeWriter writer = new com.google.zxing.qrcode.QRCodeWriter();
                    java.util.Map<com.google.zxing.EncodeHintType, Object> hints = new java.util.HashMap<>();
                    hints.put(com.google.zxing.EncodeHintType.CHARACTER_SET, "UTF-8");
                    hints.put(com.google.zxing.EncodeHintType.MARGIN, 0);
                    
                    BitMatrix bitMatrix = writer.encode(validationUrl, BarcodeFormat.QR_CODE, 100, 100, hints);
                    ByteArrayOutputStream qrOut = new ByteArrayOutputStream();
                    MatrixToImageWriter.writeToStream(bitMatrix, "PNG", qrOut);
                    PDImageXObject qrImage = PDImageXObject.createFromByteArray(document, qrOut.toByteArray(), "qrcode");
                    content.drawImage(qrImage, margin + tableWidth - 50, yStart - headerHeight + 3, 45, 45);
                } catch (Exception e) {
                    // Ignore QR generation error
                }
                
                // Lançamentos Table Header
                float yTableTop = yStart - headerHeight - 10;
                content.addRect(margin, yTableTop - 15, tableWidth, 15);
                content.stroke();
                
                float colDesc = margin + 50;
                float colPrazo = margin + 300;
                float colVant = margin + 360;
                float colDescValor = margin + 440;
                
                drawText(content, fontRegular, 10, margin + 2, yTableTop - 11, "CÓDIGO");
                drawText(content, fontRegular, 10, colDesc, yTableTop - 11, "DESCRIÇÃO");
                drawText(content, fontRegular, 10, colPrazo, yTableTop - 11, "PRAZO");
                drawText(content, fontRegular, 10, colVant, yTableTop - 11, "VANTAGEM");
                drawText(content, fontRegular, 10, colDescValor, yTableTop - 11, "DESCONTO");
                
                float yCurrent = yTableTop - 30;
                
                DecimalFormat df = new DecimalFormat("#,##0.00", new DecimalFormatSymbols(new Locale("pt", "BR")));
                
                // List items (vantagens primeiro, depois descontos)
                List<LancamentoDto> lancamentosOrdenados = ordenarLancamentos(dto.getLancamentos());
                for (LancamentoDto l : lancamentosOrdenados) {
                    drawText(content, fontRegular, 10, margin + 2, yCurrent, l.getCodigo());
                    drawText(content, fontRegular, 10, colDesc, yCurrent, l.getDescricao());
                    
                    String valorStr = df.format(l.getValor() != null ? l.getValor() : BigDecimal.ZERO);
                    if ("V".equalsIgnoreCase(l.getTipo())) {
                        drawTextRightAligned(content, fontRegular, 10, colVant + 65, yCurrent, valorStr);
                    } else {
                        drawTextRightAligned(content, fontRegular, 10, colDescValor + 65, yCurrent, valorStr);
                    }
                    
                    yCurrent -= 15;
                }
                
                // Totais Row
                yCurrent -= 5;
                content.addRect(margin, yCurrent - 15, tableWidth, 15);
                content.stroke();
                
                drawText(content, fontRegular, 10, margin + 2, yCurrent - 11, "TOTAIS");
                drawText(content, fontRegular, 10, colVant, yCurrent - 11, "VANTAGEM");
                drawText(content, fontRegular, 10, colDescValor, yCurrent - 11, "DESCONTO");
                drawText(content, fontRegular, 10, margin + tableWidth - 48, yCurrent - 11, "LÍQUIDO");
                
                yCurrent -= 30;
                
                // Totais Values
                String totVant = df.format(dto.getTotalVantagens() != null ? dto.getTotalVantagens() : BigDecimal.ZERO);
                String totDesc = df.format(dto.getTotalDescontos() != null ? dto.getTotalDescontos() : BigDecimal.ZERO);
                String liq = df.format(dto.getSalarioLiquido() != null ? dto.getSalarioLiquido() : BigDecimal.ZERO);
                
                content.addRect(margin, yCurrent - 5, tableWidth, 20);
                content.stroke();
                
                drawTextRightAligned(content, fontRegular, 10, colVant + 65, yCurrent, totVant);
                drawTextRightAligned(content, fontRegular, 10, colDescValor + 65, yCurrent, totDesc);
                drawTextRightAligned(content, fontRegular, 10, margin + tableWidth - 5, yCurrent, liq);
                
                yCurrent -= 30;
                
                // Footer
                drawText(content, fontRegular, 10, margin, yCurrent, "Consulta realizada em " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date()));
                yCurrent -= 15;
                drawText(content, fontRegular, 10, margin, yCurrent, "Para validar a autenticidade deste contracheque, digitar o código de validação " + authCode + " no endereço");
                yCurrent -= 12;
                drawText(content, fontRegular, 10, margin, yCurrent, "http://www.mppb.mp.br/validacaocontracheque");
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
    
    private void drawText(PDPageContentStream content, PDType1Font font, int fontSize, float x, float y, String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text != null ? text : "");
        content.endText();
    }
    
    private void drawTextRightAligned(PDPageContentStream content, PDType1Font font, int fontSize, float x, float y, String text) throws IOException {
        float textWidth = font.getStringWidth(text != null ? text : "") / 1000 * fontSize;
        drawText(content, font, fontSize, x - textWidth, y, text);
    }
    
    private String formatMesAno(String mesAno) {
        if (mesAno == null) return "";
        String[] p = mesAno.split("/");
        if (p.length == 2) {
            String mesStr = "";
            switch (p[0]) {
                case "01": mesStr = "Janeiro"; break;
                case "02": mesStr = "Fevereiro"; break;
                case "03": mesStr = "Março"; break;
                case "04": mesStr = "Abril"; break;
                case "05": mesStr = "Maio"; break;
                case "06": mesStr = "Junho"; break;
                case "07": mesStr = "Julho"; break;
                case "08": mesStr = "Agosto"; break;
                case "09": mesStr = "Setembro"; break;
                case "10": mesStr = "Outubro"; break;
                case "11": mesStr = "Novembro"; break;
                case "12": mesStr = "Dezembro"; break;
            }
            return mesStr + " de " + p[1];
        }
        return mesAno;
    }

    public List<LancamentoDto> ordenarLancamentos(List<LancamentoDto> lancamentos) {
        if (lancamentos == null || lancamentos.isEmpty()) {
            return Collections.emptyList();
        }
        return lancamentos.stream()
                .sorted(Comparator.comparing((LancamentoDto l) -> !"V".equalsIgnoreCase(l.getTipo())))
                .toList();
    }
}
