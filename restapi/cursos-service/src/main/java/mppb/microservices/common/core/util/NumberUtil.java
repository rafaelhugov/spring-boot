package mppb.microservices.common.core.util;

public class NumberUtil {

    public static  String calcularDVModulo11(Long numeroBase) {
        if (numeroBase == null) {
            throw new IllegalArgumentException("Número base não pode ser nulo.");
        }

        String contaSemDV = String.format("%08d", numeroBase); // preenche até 8 dígitos com zeros à esquerda

        int[] pesos = {9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;

        for (int i = 0; i < 8; i++) {
            int digito = Character.getNumericValue(contaSemDV.charAt(i));
            soma += digito * pesos[i];
        }

        int resto = soma % 11;
        String dv;

        if (resto == 0 || resto == 1) {
            dv = "0";
        } else {
            int valor = 11 - resto;
            dv = (valor == 10) ? "X" : String.valueOf(valor);
        }

        System.out.println("Conta formatada: " + contaSemDV);
        System.out.println("Dígito verificador: " + dv);
        return dv;
    }
}
