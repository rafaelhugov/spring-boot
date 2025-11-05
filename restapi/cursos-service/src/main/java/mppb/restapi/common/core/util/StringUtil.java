package mppb.restapi.common.core.util;

public class StringUtil {

    /**
     * TODO mover metodo para classe utilitaria
     * Método auxiliar para mascarar um endereço de e-mail, protegendo os dados do utilizador.
     * Exemplo: "joao.silva@email.com" -> "j...a@email.com"
     * @param email O e-mail a ser mascarado.
     * @return O e-mail mascarado.
     */
    public static String mascararEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];

        if (localPart.length() <= 2) {
            return localPart + "@" + domainPart;
        }

        return localPart.charAt(0) +
                "... " + // Utiliza "..." em vez de "*" para evitar confusão com wildcards
                localPart.charAt(localPart.length() - 1) +
                "@" +
                domainPart;
    }


    /**
     * Formata uma string de carga horária (HH:MM:SS) para um formato amigável.
     * Se os minutos forem "00", exibe apenas as horas (ex: "40h").
     * Caso contrário, exibe horas e minutos (ex: "40h 30m").
     *
     * @param hhmmss A string de carga horária, ex: "40:00:00" ou "40:30:00"
     * @return A string formatada.
     */
    public static String formatCargaHoraria(String hhmmss) {
        if (hhmmss == null || hhmmss.isBlank()) {
            return "Não informada";
        }

        String[] parts = hhmmss.split(":");

        if (parts.length < 2) {
            return hhmmss; // Formato desconhecido, retorna o original
        }

        String horas = parts[0];
        String minutos = parts[1];

        // NOVA LÓGICA: Verifica se os minutos são "00"
        if ("00".equals(minutos)) {
            return String.format("%sh", horas);
        } else {
            return String.format("%sh %sm", horas, minutos);
        }
    }

}
