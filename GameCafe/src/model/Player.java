package model;

import java.util.ArrayList;

public class Player {

    public double x = 200;
    public double y = 350;

    private int direction = 0; // 0:down,1:left,2:right,3:up
    private int animFrame = 0;
    private int animTick = 0;

    private ArrayList<model.Food> holdingFoods = new ArrayList<>();

    public void move(double dx, double dy) {
        double speed = 3;
        double dist = Math.sqrt(dx * dx + dy * dy);
        
        if (dist > 0) {
            dx = (dx / dist) * speed;
            dy = (dy / dist) * speed;
        }

        double newX = x;
        double newY = y;

        if (dx != 0) newX += dx;
        if (dy != 0) newY += dy;

        if (!isColliding(newX, y)) x = newX;
        if (!isColliding(x, newY)) y = newY;

        final int LEFT_WALL   = 30;    
        final int RIGHT_WALL  = 960;   
        final int TOP_WALL    = 140;   
        final int BOTTOM_WALL = 670;   

        x = Math.max(LEFT_WALL,  Math.min(x, RIGHT_WALL  - 60));
        y = Math.max(TOP_WALL,   Math.min(y, BOTTOM_WALL - 60));

        if (dx > 0) direction = 2;   // RIGHT
        if (dx < 0) direction = 1;   // LEFT
        if (dy > 0) direction = 0;   // DOWN
        if (dy < 0) direction = 3;   // UP
    }

    public void updateAnimation(boolean moving) {
        animTick++;
        if (moving) {
            if (animTick % 8 == 0) {
                animFrame = (animFrame + 1) % 4;
            }
        } else {
            animFrame = 0;
        }
    }

    public int getDirection() {
        return direction;
    }

    public int getAnimFrame() {
        return animFrame;
    }
    
    public int getBounceOffset() {
        if (animFrame == 0) {
            return (int)(Math.sin(animTick * 0.05) * 3); // Idle breathing
        }
        return (int)(Math.sin(animTick * 0.3) * 4); // Walking bob
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

    public void addFood(String food) {
        holdingFoods.add(new model.Food(food));
    }

    public ArrayList<model.Food> getHoldingFoods() {
        return holdingFoods;
    }

    public void removeFood(model.Food food) {
        holdingFoods.remove(food);
    }
    
    public void removeFoodByName(String name) {
        for (int i = 0; i < holdingFoods.size(); i++) {
            if (holdingFoods.get(i).getName().equals(name)) {
                holdingFoods.remove(i);
                break;
            }
        }
    }

    public void clearFoods() {
        holdingFoods.clear();
    }

    public boolean isColliding(double x, double y) {
        int size = 60; // hitbox nhân vật

        // ── Tường biên map (1022x680, sàn trong: x=116–894, y=107–621) ──
        if (x < 20)              return true;   // tường trái
        if (x + size > 980)      return true;   // tường phải
        if (y < 140)             return true;   // tường trên (quầy bếp)
        if (y + size > 670)      return true;   // tường dưới

        // ── Vùng bàn ăn (thu nhỏ hitbox giúp lách dễ dàng) ──────────
        if (x + size > 520 && x < 580 && y + size > 270 && y < 330) return true; // Bàn 1
        if (x + size > 770 && x < 830 && y + size > 270 && y < 330) return true; // Bàn 2
        if (x + size > 520 && x < 580 && y + size > 530 && y < 590) return true; // Bàn 3
        if (x + size > 770 && x < 830 && y + size > 530 && y < 590) return true; // Bàn 4

        // ── Vùng quầy bánh ở giữa ──────────
        if (x + size > 370 && x < 430 && y + size > 240 && y < 510) return true; // Giải phóng lối đi ngang mép dưới tủ bánh

        return false;
    }
}