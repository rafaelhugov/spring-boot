package mppb.microservices.common.util;
import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfUtil {

    /**
     * Concatena múltiplos PDFs recebidos como byte arrays em um único PDF.
     *
     * @param pdfs Lista de arquivos PDF em byte[]
     * @return Byte array do PDF concatenado
     * @throws IOException Se algum erro ocorrer na leitura ou escrita
     */
    public static byte[] concatenaPdfs(List<byte[]> pdfs) throws IOException {
        PDFMergerUtility merger = new PDFMergerUtility();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        List<RandomAccessRead> buffers = new ArrayList<>();

        try {
            for (byte[] pdf : pdfs) {
                RandomAccessRead rar = new RandomAccessReadBuffer(pdf);
                buffers.add(rar);
                merger.addSource(rar);
            }

            merger.setDestinationStream(outputStream);
            merger.mergeDocuments(null);
            return outputStream.toByteArray();

        } finally {
            for (RandomAccessRead rar : buffers) {
                rar.close(); // fecha buffers mesmo em caso de exceção
            }
            outputStream.close(); // sem efeito prático, mas é boa prática
        }
    }
}