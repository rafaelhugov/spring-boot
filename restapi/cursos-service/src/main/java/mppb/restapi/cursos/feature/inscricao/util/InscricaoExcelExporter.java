package mppb.restapi.cursos.feature.inscricao.util;

import mppb.restapi.cursos.feature.inscricao.dto.InscricaoDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InscricaoExcelExporter {

    private static final String[] HEADERS = {"Nome do Participante", "E-mail", "Cargo", "Situação", "Data de Inscrição"};
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

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

                row.createCell(0).setCellValue(inscricao.getParticipante().getNome());
                row.createCell(1).setCellValue(inscricao.getParticipante().getEmail());
                row.createCell(2).setCellValue(inscricao.getParticipante().getCargo());
                row.createCell(3).setCellValue(inscricao.getSituacao());
                
                if (inscricao.getDataInscricao() != null) {
                    row.createCell(4).setCellValue(inscricao.getDataInscricao().format(DATE_FORMATTER));
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
