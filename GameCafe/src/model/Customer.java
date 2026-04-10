package model;

import core.SoundManager;
import util.AStar;
import util.Node;

import java.util.ArrayList;

public class Customer {

    private ArrayList<String> orders = new ArrayList<>();
    private ArrayList<String> servedFoods = new ArrayList<>();
    
    private int direction = 0; // 0 down, 1 left, 2 right, 3 up
    private int animFrame = 0;
    private int animTick = 0;

    public double x, y;
    
    public double tableTargetX, tableTargetY;
    public double activeTargetX, activeTargetY;
    
    private boolean hasSelfServiceRequest = false;
    private boolean completedService = false;

    public boolean seated = false;
    
    public boolean isEating = false;
    public long eatStartTime = 0;
    
    public boolean isVIP = false;

    public int partySize = 1;
    public int[] skinIds = new int[4];

    public void startEating() {
        isEating = true;
        eatStartTime = System.currentTimeMillis();
    }

    private long spawnTime = System.currentTimeMillis();
    private long waitTime; 

    // AStar structures
    private AStar pathFinder;
    private ArrayList<Node> currentPath;
    private int pathIndex = 0;

    public Customer(double startX, double startY, int level) {
        
        // Randomize party size!
        // Level 1: 1-2 people. Level 3+: 1-3 people. Level 5+: 1-4 people.
        int maxParty = 2;
        if (level > 2) maxParty = 3;
        if (level > 4) maxParty = 4;
        partySize = 1 + (int)(Math.random() * maxParty);
        
        for (int i = 0; i < 4; i++) {
            skinIds[i] = (int)(Math.random() * 4); 
        }
        
        // Generate order logic based on partySize!
        String[] fullMenu = {"Cafe Sua", "Tra Dao", "Banh Mi"};
        
        int orderAmount = partySize;
        // Sometimes hungry parties order more (each person orders up to 2 items)
        if (Math.random() < 0.3) {
            orderAmount = partySize + (int)(Math.random() * partySize);
        }
        
        for (int i=0; i<orderAmount; i++) {
            int r = (int)(Math.random() * fullMenu.length);
            // Allow duplicates! "I want 3 Coffees!"
            orders.add(fullMenu[r]);
        }
        
        this.x = startX;
        this.y = startY;
        this.activeTargetX = startX;
        this.activeTargetY = startY;
        
        // Base wait time + extra time for larger parties (they are more patient taking up a table)
        this.waitTime = Math.max(30000, 80000 - (level * 3000)) + (partySize * 10000L);
        
        if (Math.random() < 0.15) {
            this.isVIP = true;
            this.waitTime /= 2;
        }
        
        pathFinder = new AStar(30, 20); // 1200x800 map at 40x40 tiles
    }

    private void syncGridObstacles() {
        pathFinder.resetNodes();
        
        // --- Tường trên cùng (Khu bếp) ---
        for (int c = 0; c < 30; c++) {
            pathFinder.setSolidNode(c, 0);
            pathFinder.setSolidNode(c, 1);
            pathFinder.setSolidNode(c, 2);
            pathFinder.setSolidNode(c, 3);
        }
        
        // --- Quầy bánh ở giữa bên trái ---
        // x từ ~320 đến 460 (c=8..11), y từ 240 đến 540 (r=6..13)
        // (ừa ra r=3,4,5 làm lối đi thẳng từ cửa đến bàn trên giống hình vẽ)
        for (int c = 8; c <= 11; c++) {
            for (int r = 6; r <= 13; r++) {
                pathFinder.setSolidNode(c, r);
            }
        }
        
        // --- Cây cảnh bên trái ---
        // x từ 0 đến 120 (c=0..3)
        for(int c=0; c<=3; c++) {
            for(int r=3; r<=20; r++) pathFinder.setSolidNode(c, r);
        }

        // --- Bàn ghế ăn (khớp với Player hitbox) ---
        // Bàn 1 (x: 550..750, y: 280..400) -> c: 13..18, r: 7..10
        for(int c=13; c<=18; c++) {
            for(int r=7; r<=10; r++) pathFinder.setSolidNode(c, r); 
        }
        // Bàn 2 (x: 800..1000, y: 280..400) -> c: 20..25, r: 7..10
        for(int c=20; c<=25; c++) {
            for(int r=7; r<=10; r++) pathFinder.setSolidNode(c, r); 
        }
        // Bàn 3 (x: 550..750, y: 540..660) -> c: 13..18, r: 13..16
        for(int c=13; c<=18; c++) {
            for(int r=13; r<=16; r++) pathFinder.setSolidNode(c, r);
        }
        // Bàn 4 (x: 800..1000, y: 540..660) -> c: 20..25, r: 13..16
        for(int c=20; c<=25; c++) {
            for(int r=13; r<=16; r++) pathFinder.setSolidNode(c, r);
        }
    }

    public void setTarget(double tx, double ty) {
        this.tableTargetX = tx;
        this.tableTargetY = ty;
        this.activeTargetX = tx;
        this.activeTargetY = ty;
        this.seated = false;
        
        generatePath(activeTargetX, activeTargetY);
    }
    
    public void setSelfServiceTarget(double serviceX, double serviceY, double finalTableX, double finalTableY) {
        this.hasSelfServiceRequest = true;
        this.completedService = false;
        
        this.tableTargetX = finalTableX;
        this.tableTargetY = finalTableY;
        
        this.activeTargetX = serviceX;
        this.activeTargetY = serviceY;
        this.seated = false;
        
        generatePath(activeTargetX, activeTargetY);
    }
    
    private void generatePath(double destX, double destY) {
        syncGridObstacles();
        
        int startCol = (int)x / 40;
        int startRow = (int)y / 40;
        int goalCol = (int)destX / 40;
        int goalRow = (int)destY / 40;
        
        pathFinder.setNodes(startCol, startRow, goalCol, goalRow);
        
        if (pathFinder.search()) {
            currentPath = pathFinder.pathList;
            pathIndex = 0;
        } else {
            currentPath = null;
        }
    }

    public void update() {
        boolean moving = false;

        if (!seated) {
            double currentTargetX;
            double currentTargetY;
            
            if (currentPath != null && pathIndex < currentPath.size()) {
                currentTargetX = currentPath.get(pathIndex).col * 40 + 20; 
                currentTargetY = currentPath.get(pathIndex).row * 40 + 20;
            } else {
                currentTargetX = activeTargetX;
                currentTargetY = activeTargetY;
            }

            double dx = currentTargetX - x;
            double dy = currentTargetY - y;
            double speed = 1.6;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist > speed) {
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
                if (Math.abs(dx) > Math.abs(dy)) {
                    direction = (dx > 0) ? 2 : 1; 
                } else {
                    direction = (dy > 0) ? 0 : 3; 
                }
                moving = true;
            } else {
                x = currentTargetX;
                y = currentTargetY;
                
                if (currentPath != null && pathIndex < currentPath.size()) {
                    pathIndex++;
                } else {
                    if (hasSelfServiceRequest && !completedService) {
                        completedService = true;
                        SoundManager.playSound("water.wav");
                        spawnTime += 5000;
                        
                        this.activeTargetX = tableTargetX;
                        this.activeTargetY = tableTargetY;
                        generatePath(activeTargetX, activeTargetY);
                    } else {
                        seated = true;
                        spawnTime = System.currentTimeMillis();
                        direction = 2; // Face table
                    }
                }
            }
        }

        // Animation
        animTick++;
        if (moving) {
            if (animTick % 10 == 0) {
                animFrame = (animFrame + 1) % 4;
            }
        } else {
            animFrame = 0;
        }
    }

    public ArrayList<String> getOrders() {
        return orders;
    }
    
    public void fulfillOrder(String food) {
        orders.remove(food);
        servedFoods.add(food);
    }
    
    public ArrayList<String> getServedFoods() {
        return servedFoods;
    }
    
    public boolean isFullyServed() {
        return orders.isEmpty();
    }
    
    public void increasePatience(long amount) {
        spawnTime += amount;
    }

    public float getTimeLeft() {
        long now = System.currentTimeMillis();
        return Math.max(0, (waitTime - (now - spawnTime)) / (float) waitTime);
    }

    public String getEmotion() {
        String base;
        if (isEating) {
            base = "😋";
        } else {
            float t = getTimeLeft();
            if (t > 0.6) base = "😊";
            else if (t > 0.3) base = "😐";
            else base = "😡";
        }
        return isVIP ? "👑 " + base : base;
    }

    public int getDirection() {
        return direction;
    }

    public int getSpriteRow() {
        switch (direction) {
            case 0: return 0; // DOWN
            case 1: return 1; // LEFT
            case 2: return 2; // RIGHT
            case 3: return 3; // UP
        }
        return 0;
    }

    public int getAnimFrame() {
        return animFrame;
    }

    public int getBounceOffset() {
        if (animFrame == 0 || seated) {
            return (int)(Math.sin(animTick * 0.05) * 3); // Idle breathing
        }
        return (int)(Math.sin(animTick * 0.3) * 4); // Walking bounce
    }
}