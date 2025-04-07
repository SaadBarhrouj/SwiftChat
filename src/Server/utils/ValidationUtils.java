package Server.utils;

public class ValidationUtils {

    // Empêcher l'instanciation
    private ValidationUtils() {}


    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        // Regex simple mais courante
        return email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    }


    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        return password.length() >= 8 &&
                password.matches(".*[A-Z].*") && // Au moins une majuscule
                password.matches(".*[a-z].*") && // Au moins une minuscule
                password.matches(".*[0-9].*");   // Au moins un chiffre

    }
}