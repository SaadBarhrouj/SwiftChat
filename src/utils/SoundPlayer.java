package utils;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class SoundPlayer {

    /**
     * Joue un son à partir d'un fichier audio.
     *
     * @param soundFilePath Chemin du fichier audio.
     */
    public static void playSound(String soundFilePath) {
        try {
            // Charger le fichier audio
            File soundFile = new File(soundFilePath);
            if (!soundFile.exists()) {
                System.err.println("Le fichier audio n'existe pas : " + soundFilePath);
                return;
            }

            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(soundFile);

            // Obtenir un Clip pour jouer le son
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);

            // Jouer le son
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Erreur lors de la lecture du son : " + e.getMessage());
            e.printStackTrace();
        }
    }
}