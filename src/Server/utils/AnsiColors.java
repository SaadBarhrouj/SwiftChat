package Server.utils;

public class AnsiColors {
    // Public static final pour être accessibles partout
    public static final String RESET = "\033[0m";
    public static final String RED = "\033[31m";
    public static final String GREEN = "\033[32m";
    public static final String YELLOW = "\033[33m";
    public static final String BLUE = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String CYAN = "\033[36m";
    public static final String WHITE = "\033[37m";
    public static final String GRAY = "\033[90m";

    // Pour effacer l'écran (Clear Screen)
    public static final String ANSI_CLS = "\033[H\033[2J";

    // Empêcher l'instanciation
    private AnsiColors() {}
}