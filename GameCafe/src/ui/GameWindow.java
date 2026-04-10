package ui;

import javax.swing.JFrame;

public class GameWindow extends JFrame {

    public GameWindow() {
        setTitle("Cafe Game - Overcooked Mini");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        add(new GamePanel());

        setVisible(true);
    }
}