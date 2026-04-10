package core;

import ui.GamePanel;

public class GameLoop implements Runnable {

    private Thread thread;
    private boolean running = false;

    private final int FPS = 60;
    private final double TIME_PER_FRAME = 1_000_000_000.0 / FPS;

    private GameManager gameManager;
    private GamePanel gamePanel;

    public GameLoop(GameManager gameManager, GamePanel gamePanel) {
        this.gameManager = gameManager;
        this.gamePanel = gamePanel;
    }

    public void start() {
        running = true;
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double delta = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / TIME_PER_FRAME;
            lastTime = now;

            if (delta >= 1) {
                gameManager.update();

                // 👇 GỌI REPAINT (đúng chuẩn Swing)
                gamePanel.repaint();

                delta--;
            }
        }
    }
}