package cl.usach.tingeso.util;

/**
 * RunValidatorUtil validates Chilean National Identity Numbers (RUN/RUT)
 * using the official Modulo 11 check-digit algorithm.
 */
public final class RunValidatorUtil {

    private RunValidatorUtil() {
        // Utility class
    }

    /**
     * Validates a Chilean RUN with hyphen format (e.g., "12345678-9").
     *
     * @param rawRun the raw RUN string to validate
     * @return true if valid according to Modulo 11, false otherwise
     */
    public static boolean isValid(String rawRun) {
        if (rawRun == null || rawRun.isBlank()) {
            return false;
        }

        String cleaned = rawRun.trim().toUpperCase().replace(".", "");
        if (!cleaned.matches("^[0-9]{7,8}-[0-9K]$")) {
            return false;
        }

        String[] parts = cleaned.split("-");
        String body = parts[0];
        char expectedDv = parts[1].charAt(0);

        return calculateCheckDigit(body) == expectedDv;
    }

    /**
     * Calculates the expected check digit (DV) for a RUN body using Modulo 11.
     *
     * @param body numerical string of the RUN without DV or hyphen
     * @return the expected check digit character ('0'-'9' or 'K')
     */
    public static char calculateCheckDigit(String body) {
        int sum = 0;
        int multiplier = 2;

        for (int i = body.length() - 1; i >= 0; i--) {
            int digit = Character.getNumericValue(body.charAt(i));
            sum += digit * multiplier;
            multiplier = (multiplier == 7) ? 2 : multiplier + 1;
        }

        int remainder = 11 - (sum % 11);
        if (remainder == 11) {
            return '0';
        } else if (remainder == 10) {
            return 'K';
        } else {
            return Character.forDigit(remainder, 10);
        }
    }

    /**
     * Cleans and standardizes the RUN string to format 12345678-9.
     *
     * @param rawRun raw string with or without dots
     * @return cleaned standardized RUN
     */
    public static String cleanFormat(String rawRun) {
        if (rawRun == null) {
            return null;
        }
        return rawRun.trim().toUpperCase().replace(".", "");
    }
}
