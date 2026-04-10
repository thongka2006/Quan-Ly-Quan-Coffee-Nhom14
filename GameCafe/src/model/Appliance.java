package model;

import java.awt.Rectangle;

public class Appliance {
    public String name;
    public long startTime = 0;
    public long totalTime = 3000; // default 3s
    public boolean isCooking = false;
    public boolean isReady = false;
    
    public String[] steps = {"Lấy hạt", "Xay", "Pha", "Đổ nước", "Hoàn thành"};
    
    // Physical bounds for rendering progress bar or interacting
    public Rectangle bounds;

    public Appliance(String name, Rectangle bounds) {
        this.name = name;
        this.bounds = bounds;
        if (name.equals("Tra Dao")) {
            steps = new String[]{"Lấy lá trà", "Giã lá", "Pha trà", "Thêm đào", "Hoàn thành"};
        }
    }

    public void startCooking(long timeMs) {
        this.totalTime = timeMs;
        this.startTime = System.currentTimeMillis();
        this.isCooking = true;
        this.isReady = false;
    }

    public void update() {
        if (isCooking && System.currentTimeMillis() - startTime >= totalTime) {
            isCooking = false;
            isReady = true;
        }
    }

    public float getProgress() {
        if (!isCooking) return 0f;
        long elapsed = System.currentTimeMillis() - startTime;
        return Math.min(1f, (float)elapsed / totalTime);
    }
    
    public String getCurrentStepName() {
        if (!isCooking && !isReady) return "";
        if (isReady) return steps.length + ". " + steps[steps.length - 1];
        
        float p = getProgress();
        int idx = (int) (p * steps.length);
        if (idx >= steps.length) idx = steps.length - 1;
        return (idx + 1) + ". " + steps[idx];
    }
    
    public void reset() {
        isCooking = false;
        isReady = false;
    }
}
