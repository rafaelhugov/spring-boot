package br.gov.mppb.ceaf_backend.service;

import br.gov.mppb.ceaf_backend.domain.Curso;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class InscricaoService {

    /**
     * Valida se a data atual está dentro do período de inscrição do curso.
     * @param curso O curso para o qual a inscrição está sendo feita.
     * @return true se o período for válido, false caso contrário.
     */
    public boolean isPeriodoInscricaoValido(Curso curso) {
        if (curso.getInscricaoDataInicial() == null || curso.getInscricaoDataFinal() == null) {
            // Se não há datas definidas, consideramos o período como inválido para inscrição.
            return false;
        }
        LocalDateTime agora = LocalDateTime.now();
        return !agora.isBefore(curso.getInscricaoDataInicial()) && !agora.isAfter(curso.getInscricaoDataFinal());
    }

    /**
     * Valida se o participante pertence ao público-alvo do curso.
     * (Método placeholder para implementação futura)
     * @param email O e-mail do participante.
     * @param curso O curso.
     * @return true se o público for válido.
     */
    public boolean isPublicoAlvoValido(String email, Curso curso) {
        // LÓGICA FUTURA:
        // 1. Verificar se o curso tem um público-alvo restrito (Membros/Servidores).
        // 2. Se sim, consultar a base do RH com o e-mail/CPF para validar o vínculo.
        // 3. Retornar true ou false com base na validação.
        System.out.println("Validando público-alvo para: " + email); // Log de placeholder
        return true; // Por enquanto, sempre retorna válido.
    }
}