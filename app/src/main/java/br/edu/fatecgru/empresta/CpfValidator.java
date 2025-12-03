package br.edu.fatecgru.empresta;

public class CpfValidator {

    public static boolean isValid(String cpf) {
        cpf = cpf.replaceAll("[^\\d]", "");

        if (cpf.length() != 11 || cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        try {
            // First digit checker
            int sum = 0;
            for (int i = 0; i < 9; i++) {
                sum += Integer.parseInt(String.valueOf(cpf.charAt(i))) * (10 - i);
            }
            int firstDigit = 11 - (sum % 11);
            if (firstDigit > 9) {
                firstDigit = 0;
            }

            if (Integer.parseInt(String.valueOf(cpf.charAt(9))) != firstDigit) {
                return false;
            }

            // Second digit checker
            sum = 0;
            for (int i = 0; i < 10; i++) {
                sum += Integer.parseInt(String.valueOf(cpf.charAt(i))) * (11 - i);
            }
            int secondDigit = 11 - (sum % 11);
            if (secondDigit > 9) {
                secondDigit = 0;
            }

            return Integer.parseInt(String.valueOf(cpf.charAt(10))) == secondDigit;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
