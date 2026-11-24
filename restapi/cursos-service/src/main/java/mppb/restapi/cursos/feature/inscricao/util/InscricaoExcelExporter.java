package mppb.restapi.cursos.feature.inscricao.util;

import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import mppb.restapi.cursos.feature.participante.dto.ParticipanteDto;
import mppb.restapi.cursos.feature.participante.enums.TipoParticipanteEnum;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InscricaoExcelExporter {

    private static final String[] HEADERS = {"Nome do Participante", "E-mail", "Cargo", "Lotação", "Designação", "PcD", "Necessidade Especial", "Situação", "Data de Inscrição"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static ByteArrayInputStream exportToExcel(List<InscricaoDto> inscricoes) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Participantes");

            // Estilo do cabeçalho
            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);

            // Linha do cabeçalho
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // Dados
            int rowIdx = 1;
            for (InscricaoDto inscricao : inscricoes) {
                Row row = sheet.createRow(rowIdx++);
                ParticipanteDto participante = inscricao.getParticipante();
                boolean isInterno = participante != null && TipoParticipanteEnum.INTERNO.equals(participante.getTipo());

                row.createCell(0).setCellValue(participante != null && participante.getNome() != null ? participante.getNome() : "");
                row.createCell(1).setCellValue(participante != null && participante.getEmail() != null ? participante.getEmail() : "");
                row.createCell(2).setCellValue(participante != null && participante.getCargo() != null ? participante.getCargo() : "");
                row.createCell(3).setCellValue(isInterno && participante.getLotacao() != null ? participante.getLotacao() : "");
                row.createCell(4).setCellValue(isInterno && participante.getDesignacao() != null ? participante.getDesignacao() : "");
                row.createCell(5).setCellValue(Boolean.TRUE.equals(inscricao.getPcd()) ? "Sim" : "Não");
                row.createCell(6).setCellValue(inscricao.getNecessidadeEspecial() != null ? inscricao.getNecessidadeEspecial() : "");
                row.createCell(7).setCellValue(inscricao.getSituacao() != null ? inscricao.getSituacao() : "");

                if (inscricao.getDataInscricao() != null) {
                    row.createCell(8).setCellValue(inscricao.getDataInscricao().format(DATE_FORMATTER));
                }
            }

            // Auto-ajuste das colunas
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }
}
