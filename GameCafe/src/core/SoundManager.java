package core;

import javax.sound.sampled.*;
import java.net.URL;
import java.util.prefs.Preferences;

public class SoundManager {

    private static boolean isSoundOn = true;
    private static boolean isMusicOn = true;
    private static Clip bgmClip;
    private static Preferences prefs = Preferences.userNodeForPackage(SoundManager.class);

    static {
        isSoundOn = prefs.getBoolean("isSoundOn", true);
        isMusicOn = prefs.getBoolean("isMusicOn", true);
    }

    public static void playSound(String fileName) {
        if (!isSoundOn) return;
        try {
            URL url = SoundManager.class.getResource("/assets/" + fileName);
            if (url == null) return;
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            System.out.println("Could not play sound: " + fileName);
        }
    }

    public static void playMusic(String fileName) {
        if (!isMusicOn) return;
        stopMusic();
        try {
            URL url = SoundManager.class.getResource("/assets/" + fileName);
            if (url == null) return;
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            bgmClip = AudioSystem.getClip();
            bgmClip.open(audioIn);
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            bgmClip.start();
        } catch (Exception e) {
            System.out.println("Could not play music: " + fileName);
        }
    }

    public static void stopMusic() {
        if (bgmClip != null && bgmClip.isRunning()) {
            bgmClip.stop();
            bgmClip.close();
        }
    }

    public static boolean isSoundOn() { return isSoundOn; }
    public static boolean isMusicOn() { return isMusicOn; }

    public static void toggleSound() {
        isSoundOn = !isSoundOn;
        prefs.putBoolean("isSoundOn", isSoundOn);
    }

    public static void toggleMusic() {
        isMusicOn = !isMusicOn;
        prefs.putBoolean("isMusicOn", isMusicOn);
        if (!isMusicOn) {
            stopMusic();
        }
    }
}
