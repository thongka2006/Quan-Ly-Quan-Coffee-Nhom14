package input;

import core.GameManager;
import model.Table;
import model.Food;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.Color;

public class InputHandler extends KeyAdapter {

    private GameManager gameManager;
    public boolean up, down, left, right;

    public InputHandler(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) up = true;
        if (e.getKeyCode() == KeyEvent.VK_S) down = true;
        if (e.getKeyCode() == KeyEvent.VK_A) left = true;
        if (e.getKeyCode() == KeyEvent.VK_D) right = true;

        if (e.getKeyCode() == KeyEvent.VK_SPACE) interact();
        if (e.getKeyCode() == KeyEvent.VK_Q) dropFood();
        
        if (e.getKeyCode() == KeyEvent.VK_B) {
            if (gameManager.state == GameManager.GameState.PLAYING) {
                gameManager.state = GameManager.GameState.SHOP;
            } else if (gameManager.state == GameManager.GameState.SHOP) {
                gameManager.state = GameManager.GameState.PLAYING;
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (gameManager.state == GameManager.GameState.GAME_OVER) {
                gameManager.resetGame();
            } else if (gameManager.state == GameManager.GameState.MENU) {
                gameManager.state = GameManager.GameState.PLAYING;
            }
        }

        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (gameManager.state == GameManager.GameState.PLAYING) {
                gameManager.state = GameManager.GameState.SETTINGS;
            } else if (gameManager.state == GameManager.GameState.SETTINGS || gameManager.state == GameManager.GameState.HELP || gameManager.state == GameManager.GameState.SHOP) {
                gameManager.state = GameManager.GameState.MENU;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_W) up = false;
        if (e.getKeyCode() == KeyEvent.VK_S) down = false;
        if (e.getKeyCode() == KeyEvent.VK_A) left = false;
        if (e.getKeyCode() == KeyEvent.VK_D) right = false;
    }
    
    private void dropFood() {
        if (!gameManager.player.getHoldingFoods().isEmpty()) {
            gameManager.player.getHoldingFoods().remove(gameManager.player.getHoldingFoods().size() - 1);
            core.SoundManager.playSound("error.wav");
        }
    }

    private void interact() {
        if (gameManager.state != GameManager.GameState.PLAYING) return;
        
        int px = (int) gameManager.player.x;
        int py = (int) gameManager.player.y;

        // Interaction Zones Overhaul
        
        // 1. ZONES PHÍA DƯỚI (Dock Nguyên Liệu Góc Dưới Phải)
        if (py > 450) {
            // Dựa vào UI Dock: Lá Trà, Hạt Cafe, Bánh Mì, Nước Pha (X từ 600 đến 1022)
            if (px >= 550 && px < 700) {
                // Lá Trà
                if (gameManager.player.getHoldingFoods().size() < gameManager.trayCapacity) {
                    gameManager.player.addFood("Raw Leaves");
                    core.SoundManager.playSound("pickup.wav");
                }
            } else if (px >= 700 && px < 800) {
                // Hạt Cafe
                if (gameManager.player.getHoldingFoods().size() < gameManager.trayCapacity) {
                    gameManager.player.addFood("Raw Beans");
                    core.SoundManager.playSound("pickup.wav");
                }
            } else if (px >= 800 && px < 900) {
                // Bánh Mì
                if (gameManager.player.getHoldingFoods().size() < gameManager.trayCapacity) {
                    gameManager.player.addFood("Banh Mi");
                    core.SoundManager.playSound("pickup.wav");
                }
            } else if (px >= 900) {
                // Nước Pha
                if (gameManager.player.getHoldingFoods().size() < gameManager.trayCapacity) {
                    gameManager.player.addFood("Nước Pha");
                    core.SoundManager.playSound("pickup.wav");
                }
            }
        }
        
        // 2. ZONES PHÍA TRÊN (Quầy Pha Chế Kếp Ở Trên Bản Đồ)
        if (py < 250) {
            if (px < 250) {
                // Thùng Rác (Bên trái cùng)
                if (playerHas("Rác")) {
                    gameManager.player.removeFoodByName("Rác");
                    core.SoundManager.playSound("pickup.wav");
                    gameManager.score += 5;
                    gameManager.addEffect("+5 Sạch Sẽ", px, py, Color.GREEN);
                }
            } else if (px >= 250 && px < 450) {
                // Tea Machine (Máy bên trái quầy xanh)
                if (gameManager.teaMachine.isReady) {
                    gameManager.pickFood("Tra Dao");
                } else if (!gameManager.teaMachine.isCooking) {
                    if (playerHas("Raw Leaves") && playerHas("Nước Pha")) {
                        gameManager.player.removeFoodByName("Raw Leaves");
                        gameManager.player.removeFoodByName("Nước Pha");
                        gameManager.startCooking("Tra Dao");
                    } else {
                        gameManager.addEffect("Cần Lá Trà & Nước!", px, py, Color.RED);
                        core.SoundManager.playSound("error.wav");
                    }
                }
            } else if (px >= 450 && px < 750) {
                // Cafe Machine (Máy lớn giữa quầy xanh)
                if (gameManager.cafeMachine.isReady) {
                    gameManager.pickFood("Cafe Sua");
                } else if (!gameManager.cafeMachine.isCooking) {
                    if (playerHas("Raw Beans") && playerHas("Nước Pha")) {
                        gameManager.player.removeFoodByName("Raw Beans");
                        gameManager.player.removeFoodByName("Nước Pha");
                        gameManager.startCooking("Cafe Sua");
                    } else {
                        gameManager.addEffect("Cần Cà Phê & Nước!", px, py, Color.RED);
                        core.SoundManager.playSound("error.wav");
                    }
                }
            }
        }

        // bàn - serving customers
        for (Table t : gameManager.tables) {
            if (Math.abs(px - t.x) < 80 && Math.abs(py - t.y) < 80) {
                if (t.isDirty) {
                    if (gameManager.player.getHoldingFoods().size() < gameManager.trayCapacity) {
                        gameManager.player.addFood("Rác");
                        t.clean();
                        core.SoundManager.playSound("pickup.wav");
                    } else {
                        gameManager.addEffect("Khay Đầy!", px, py, Color.RED);
                        core.SoundManager.playSound("error.wav");
                    }
                } else if (!t.isLocked) {
                    gameManager.serve(t);
                }
            }
        }
    }
    
    private boolean playerHas(String name) {
        for (Food f : gameManager.player.getHoldingFoods()) {
            if (f.getName().equals(name)) return true;
        }
        return false;
    }
}