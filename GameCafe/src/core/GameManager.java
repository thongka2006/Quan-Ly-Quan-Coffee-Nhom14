package core;

import model.*;
import input.InputHandler;

import java.util.ArrayList;
import java.awt.Rectangle;
import java.awt.Color;

public class GameManager {

    public Player player = new Player();
    // Nhân viên AI – null cho đến khi được thuê
    public Staff staff = null;
    public boolean hasStaff = false;
    public static final int STAFF_COST = 80000;
    public ArrayList<Table> tables = new ArrayList<>();
    public InputHandler input;

    private long lastSpawn = 0;

    public enum GameState {
        MENU, PLAYING, PAUSE, GAME_OVER, SHOP, HELP, INTRO, SETTINGS
    }

    public GameState state = GameState.INTRO;

    public int score = 0;
    public int money = 0;
    public int highScore = 0;

    private static java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(GameManager.class);

    // --- NEW FEATURES ---
    public int level = 1;
    public int comboCount = 0;
    public long lastServeTime = 0;
    
    // --- SHOP UPGRADES ---
    public int trayCapacity = 2;
    public float cookSpeedMod = 1.0f;
    public long patienceBoost = 0;
    
    public boolean hasEspresso = false;
    public boolean hasSilverTray = false;
    public boolean hasComfyChairs = false;
    
    public ArrayList<Effect> effects = new ArrayList<>();
    
    // Waiting queue
    public ArrayList<Customer> waitingQueue = new ArrayList<>();

    // Cooking appliances
    public Appliance cafeMachine = new Appliance("Cafe Sua", new Rectangle(100, 500, 150, 100)); 
    public Appliance teaMachine = new Appliance("Tra Dao", new Rectangle(300, 500, 150, 100));

    public GameManager() {
        loadHighScore();
        SoundManager.playMusic("background_music.wav");
        tables.add(new Table(515, 260));
        tables.add(new Table(765, 260));
        Table t3 = new Table(515, 520); t3.isLocked = true;
        tables.add(t3);
        Table t4 = new Table(765, 520); t4.isLocked = true;
        tables.add(t4);
    }

    public void loadHighScore() {
        highScore = prefs.getInt("highScore", 0);
    }

    public void saveHighScore() {
        if (score > highScore) {
            highScore = score;
            prefs.putInt("highScore", highScore);
        }
    }

    public void update() {
        boolean moving = false;
        if (input != null && state == GameState.PLAYING) {
            if (input.up) { player.move(0, -1); moving = true; }
            if (input.down) { player.move(0, 1); moving = true; }
            if (input.left) { player.move(-1, 0); moving = true; }
            if (input.right) { player.move(1, 0); moving = true; }
        }
        
        if (state != GameState.PLAYING) return;

        // === NHÂN VIÊN AI (chỉ khi đã thuê) ===
        if (hasStaff && staff != null) {
            staff.cookSpeedMod = cookSpeedMod; // đồng bộ tốc độ pha
            staff.update();
            dispatchStaffAI();
        }
        if (score <= -100) {
            saveHighScore();
            state = GameState.GAME_OVER;
        }

        player.updateAnimation(moving);

        long now = System.currentTimeMillis();
        
        // Spawn logic: scale linearly with level! Max 4 Customers in queue.
        long currentSpawnDelay = Math.max(800, 4000 - (level * 400));
        if (now - lastSpawn > currentSpawnDelay) {
            if (waitingQueue.size() < 4) {
                spawnCustomer();
            }
            lastSpawn = now;
        }
        
        // Combo Timeout
        if (comboCount > 0 && now - lastServeTime > 15000) {
            comboCount = 0; 
            addEffect("Mất Combo!", player.x, player.y - 20, Color.RED);
        }

        // Update appliances
        cafeMachine.update();
        teaMachine.update();
        
        // Random smoke particles when cooking
        if (cafeMachine.isCooking && Math.random() < 0.1) {
            effects.add(new Effect(cafeMachine.bounds.x + 50, cafeMachine.bounds.y + 20, Color.WHITE, 30));
        }
        if (teaMachine.isCooking && Math.random() < 0.1) {
            effects.add(new Effect(teaMachine.bounds.x + 50, teaMachine.bounds.y + 20, Color.WHITE, 30));
        }

        // Update effects
        for (int i = effects.size() - 1; i >= 0; i--) {
            Effect ef = effects.get(i);
            ef.update();
            if (ef.isDead()) effects.remove(i);
        }

        // Promote queue if table is empty
        Table emptyTable = getEmptyTable();
        if (emptyTable != null && !waitingQueue.isEmpty()) {
            Customer next = waitingQueue.remove(0);
            
            if (Math.random() < 0.4) {
                next.setSelfServiceTarget(150, 200, emptyTable.x, emptyTable.y);
            } else {
                next.setTarget(emptyTable.x, emptyTable.y);
            }
            
            emptyTable.setCustomer(next);
            reorderQueue();
        }

        // Update guests in waiting line
        for (int i = waitingQueue.size() - 1; i >= 0; i--) {
            Customer c = waitingQueue.get(i);
            c.update();
            
            if (c.getTimeLeft() <= 0) {
                waitingQueue.remove(i);
                comboCount = 0;
                int penalty = c.isVIP ? 50 : 10;
                score -= penalty;
                addEffect("-" + penalty + " Hết kiên nhẫn", c.x, c.y, Color.RED);
                SoundManager.playSound("angry.wav");
                reorderQueue();
            }
        }

        for (Table t : tables) {
            if (t.getCustomer() != null) {
                Customer c = t.getCustomer();
                c.update();
                
                if (c.isEating) {
                    if (System.currentTimeMillis() - c.eatStartTime > 4000) {
                        t.clear(); // Guest leaves happily
                    }
                } else if (c.seated && c.getTimeLeft() <= 0) {
                    t.clear();
                    comboCount = 0;
                    int penalty = c.isVIP ? 50 : 10;
                    score -= penalty;
                    addEffect("-" + penalty + " Hết kiên nhẫn", c.x, c.y, Color.RED);
                    SoundManager.playSound("angry.wav");
                }
            }
        }
    }
    
    private void reorderQueue() {
        for (int i = 0; i < waitingQueue.size(); i++) {
            waitingQueue.get(i).setTarget(-40 - (i * 60), 380);
        }
    }

    /**
     * Điều phối nhiệm vụ tự động cho nhân viên AI.
     * Ưu tiên:
     *  1. Dọn bàn bẩn
     *  2. Lấy đồ máy đã pha xong → giao khách đúng món
     *  3. Đi pha chế món mà khách cần (máy đang rảnh)
     */
    private void dispatchStaffAI() {
        if (staff == null || staff.isBusy()) return;

        // --- Ưu tiên 1: Dọn bàn bẩn ---
        for (Table t : tables) {
            if (t.isDirty) {
                staff.assignClean(t);
                return;
            }
        }

        // --- Ưu tiên 2: Máy đã pha xong → lấy & giao ---
        if (cafeMachine.isReady) {
            Table best = findTableNeedingFood(cafeMachine.name);
            if (best != null) {
                staff.assignPickAndServe(cafeMachine, best);
                return;
            }
        }
        if (teaMachine.isReady) {
            Table best = findTableNeedingFood(teaMachine.name);
            if (best != null) {
                staff.assignPickAndServe(teaMachine, best);
                return;
            }
        }

        // --- Ưu tiên 3: Pha chế món khách cần (máy rảnh) ---
        if (!cafeMachine.isCooking && !cafeMachine.isReady) {
            Table best = findTableNeedingFood(cafeMachine.name);
            if (best != null) {
                staff.assignCookAndServe(cafeMachine, best);
                SoundManager.playSound("cook.wav");
                return;
            }
        }
        if (!teaMachine.isCooking && !teaMachine.isReady) {
            Table best = findTableNeedingFood(teaMachine.name);
            if (best != null) {
                staff.assignCookAndServe(teaMachine, best);
                SoundManager.playSound("cook.wav");
                return;
            }
        }
    }

    /** Tìm bàn có khách đang chờ món foodName */
    private Table findTableNeedingFood(String foodName) {
        for (Table t : tables) {
            Customer c = t.getCustomer();
            if (c != null && c.seated && !c.isEating && c.getOrders().contains(foodName)) {
                return t;
            }
        }
        return null;
    }

    public void addEffect(String text, double x, double y, Color color) {
        effects.add(new Effect(text, x, y, color, 60));
    }

    public void resetGame() {
        player  = new Player();
        staff   = null;        // Phải thuê lại từ đầu
        hasStaff = false;
        tables.clear();
        tables.add(new Table(515, 260));
        tables.add(new Table(765, 260));
        Table t3 = new Table(515, 520); t3.isLocked = true;
        tables.add(t3);
        Table t4 = new Table(765, 520); t4.isLocked = true;
        tables.add(t4);
        score = 0;
        money = 0;
        level = 1;
        comboCount = 0;
        waitingQueue.clear();
        effects.clear();
        cafeMachine.reset();
        teaMachine.reset();
        
        hasEspresso = false;
        hasSilverTray = false;
        hasComfyChairs = false;
        trayCapacity = 2;
        cookSpeedMod = 1.0f;
        patienceBoost = 0;
        
        state = GameState.PLAYING;
    }

    private void spawnCustomer() {
        int queueIndex = waitingQueue.size();
        double startX = -60;
        double startY = 380; 
        
        Customer c = new Customer(startX, startY, level);
        c.increasePatience(patienceBoost); // Apply shop upgrades
        
        c.setTarget(-40 - (queueIndex * 60), 380);
        
        SoundManager.playSound("doorbell.wav");
        waitingQueue.add(c);
    }
    
    private Table getEmptyTable() {
        for (Table t : tables) {
            if (t.getCustomer() == null && !t.isLocked && !t.isDirty) return t;
        }
        return null;
    }

    public void startCooking(String food) {
        long finalTime = (long)(2000 * cookSpeedMod);
        if (food.equals("Cafe Sua") && !cafeMachine.isCooking && !cafeMachine.isReady) {
            cafeMachine.startCooking(finalTime);
            SoundManager.playSound("cook.wav");
        } else if (food.equals("Tra Dao") && !teaMachine.isCooking && !teaMachine.isReady) {
            teaMachine.startCooking(finalTime);
            SoundManager.playSound("cook.wav");
        }
    }

    public void pickFood(String food) {
        if (player.getHoldingFoods().size() >= trayCapacity) {
            addEffect("Khay Đầy!", player.x, player.y, Color.RED);
            SoundManager.playSound("error.wav");
            return;
        }
        
        if (food.equals("Cafe Sua") && cafeMachine.isReady) {
            player.addFood(food);
            cafeMachine.reset();
            SoundManager.playSound("pickup.wav");
        } else if (food.equals("Tra Dao") && teaMachine.isReady) {
            player.addFood(food);
            teaMachine.reset();
            SoundManager.playSound("pickup.wav");
        }
    }

    public void serve(Table table) {
        if (table.getCustomer() == null) return;
        Customer c = table.getCustomer();
        if (!c.seated) return;
        
        if (c.isEating) {
            addEffect("Đang ăn!", c.x, c.y, Color.WHITE);
            return;
        }
        
        ArrayList<Food> tray = player.getHoldingFoods();
        if (tray.isEmpty()) return;

        // Find a matching food on the tray
        Food matchedFood = null;
        for (Food f : tray) {
            if (c.getOrders().contains(f.getName())) {
                matchedFood = f;
                break;
            }
        }
        
        if (matchedFood != null) {
            // Partial fulfillment successful!
            player.removeFood(matchedFood);
            c.fulfillOrder(matchedFood.getName());
            
            addEffect("Đã phục vụ " + matchedFood.getName() + "!", c.x, c.y - 20, Color.YELLOW);
            SoundManager.playSound("pickup.wav");
            
            // Check if fully served to grant big rewards
            if (c.isFullyServed()) {
                c.startEating(); // Start eating instead of leaving immediately
                
                long now = System.currentTimeMillis();
                if (now - lastServeTime < 15000 || comboCount == 0) {
                    comboCount++;
                } else {
                    comboCount = 1;
                }
                lastServeTime = now;
                
                int baseScore = 20;
                int baseCash = 10000;
                
                int scoreEarned = baseScore * comboCount * (c.isVIP ? 3 : 1) * c.partySize;
                int cashEarned = baseCash * comboCount * (c.isVIP ? 3 : 1) * c.partySize;
    
                score += scoreEarned;
                money += cashEarned;
                
                // Check Level Progression
                int newLevel = 1 + (score / 150);
                if (newLevel > level) {
                    level = newLevel;
                    SoundManager.playSound("levelup.wav");
                    addEffect("TĂNG CẤP: " + level, player.x, player.y - 40, Color.MAGENTA);
                }
    
                addEffect("+" + scoreEarned + " (x" + comboCount + ")", c.x, c.y, Color.GREEN);
                SoundManager.playSound("coin.wav");
            }
        } else {
            comboCount = 0;
            score -= 5;
            addEffect("Sai món!", c.x, c.y, Color.RED);
            SoundManager.playSound("error.wav");
            // we could remove item from player tray as penalty, but let's just deduct score
        }
    }
    
    public void serveByStaff(Customer c, String food) {
        c.fulfillOrder(food);
        
        addEffect("Nhân viên phục vụ " + food + "!", c.x, c.y - 20, Color.YELLOW);
        SoundManager.playSound("pickup.wav");
        
        // Check if fully served to grant big rewards
        if (c.isFullyServed()) {
            c.startEating(); // Start eating instead of leaving immediately
            
            long now = System.currentTimeMillis();
            if (now - lastServeTime < 15000 || comboCount == 0) {
                comboCount++;
            } else {
                comboCount = 1;
            }
            lastServeTime = now;
            
            int baseScore = 20;
            int baseCash = 10000; // Tiền kiếm được giảm xuống để cân bằng đồ hoạ VNĐ
            
            int scoreEarned = baseScore * comboCount * (c.isVIP ? 3 : 1) * c.partySize;
            int cashEarned = baseCash * comboCount * (c.isVIP ? 3 : 1) * c.partySize;

            score += scoreEarned;
            money += cashEarned;
            
            // Check Level Progression
            int newLevel = 1 + (score / 150);
            if (newLevel > level) {
                level = newLevel;
                SoundManager.playSound("levelup.wav");
                addEffect("TĂNG CẤP: " + level, player.x, player.y - 40, Color.MAGENTA);
            }

            addEffect("+" + scoreEarned + " (x" + comboCount + ")", c.x, c.y, Color.GREEN);
            SoundManager.playSound("coin.wav");
        }
    }
    
    // Shop transactions
    public void buyUpgrade(String upgradeName) {
        if (upgradeName.equals("HireStaff") && !hasStaff && money >= STAFF_COST) {
            money    -= STAFF_COST;
            hasStaff  = true;
            staff     = new Staff(this);
            staff.cookSpeedMod = cookSpeedMod;
            SoundManager.playSound("coin.wav");
            addEffect("👩‍🍳 Đã thuê nhân viên!", 600, 400, new Color(0, 255, 180));
        } else if (upgradeName.equals("Espresso") && !hasEspresso && money >= 50000) {
            money -= 50000;
            hasEspresso = true;
            cookSpeedMod = 0.5f;
            if (staff != null) staff.cookSpeedMod = cookSpeedMod;
            SoundManager.playSound("coin.wav");
        } else if (upgradeName.equals("SilverTray") && !hasSilverTray && money >= 100000) {
            money -= 100000;
            hasSilverTray = true;
            trayCapacity = 4;
            SoundManager.playSound("coin.wav");
        } else if (upgradeName.equals("ComfyChairs") && !hasComfyChairs && money >= 150000) {
            money -= 150000;
            hasComfyChairs = true;
            patienceBoost  = 10000;
            for (Customer c : waitingQueue) c.increasePatience(10000);
            for (Table t : tables) {
                if (t.getCustomer() != null) t.getCustomer().increasePatience(10000);
            }
            SoundManager.playSound("coin.wav");
        } else if (upgradeName.equals("BuyTable") && money >= 200000) {
            for (Table t : tables) {
                if (t.isLocked) {
                    t.isLocked = false;
                    money -= 200000;
                    SoundManager.playSound("coin.wav");
                    break;
                }
            }
        } else {
            SoundManager.playSound("error.wav");
        }
    }
}