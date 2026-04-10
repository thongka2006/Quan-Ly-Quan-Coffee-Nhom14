package model;

import core.GameManager;

/**
 * Staff – NPC nhân viên AI tự động phục vụ trong quán.
 *
 * Vòng đời nhiệm vụ đầy đủ:
 *  IDLE → GOING_TO_COOK  → COOKING  → PICKING_FOOD → SERVING_TABLE → IDLE
 *       → CLEANING                                                   → IDLE
 *       → PATROLLING                                                 → IDLE
 *
 *  Ưu tiên:
 *   1. Dọn bàn bẩn
 *   2. Nếu máy pha đã xong → đi lấy & giao khách
 *   3. Nếu khách cần món nhưng máy rảnh & chưa pha → tới máy, pha, rồi lấy & giao
 */
public class Staff {

    // ─── Vị trí & hướng ───────────────────────────────────────────────
    public double x = 200;
    public double y = 450;

    private int direction = 0;   // 0 down | 1 left | 2 right | 3 up
    private int animFrame  = 0;
    private int animTick   = 0;

    // ─── State machine ────────────────────────────────────────────────
    public enum Task {
        IDLE,
        PATROLLING,
        CLEANING,
        GOING_TO_COOK,   // đang đi đến máy để bắt đầu pha
        COOKING,         // đang đứng chờ máy pha xong
        PICKING_FOOD,    // đang đi lấy đồ đã pha
        SERVING_TABLE    // đang đi giao đồ cho khách
    }
    public Task currentTask = Task.IDLE;

    // ─── Dữ liệu nhiệm vụ ────────────────────────────────────────────
    public String holdingFood   = null;   // món đang mang trên tay
    public Table  targetTable   = null;   // bàn mục tiêu
    public Appliance targetApp  = null;   // máy pha mục tiêu

    // ─── Các điểm tuần tra ───────────────────────────────────────────
    private static final double[][] PATROL = {
        {150, 450}, {550, 450}, {150, 650}, {550, 650}
    };
    private int patrolIdx = 0;
    private int idleTicks = 0;

    // Tốc độ di chuyển
    private static final double SPEED = 2.2;

    // ─── Skin (dùng chung mảng customerSkins ở GamePanel) ────────────
    public int skinId = 2;   // skin customer3 (girl – pink hoodie)

    // ─── Cook speed (được GameManager truyền vào) ─────────────────────
    public float cookSpeedMod = 1.0f;

    private GameManager gm;
    
    public Staff(GameManager gm) {
        this.gm = gm;
    }

    // ─── Update mỗi frame ─────────────────────────────────────────────
    public void update() {
        animTick++;
        switch (currentTask) {
            case CLEANING:      tickCleaning();    break;
            case GOING_TO_COOK: tickGoToCook();    break;
            case COOKING:       tickCooking();     break;
            case PICKING_FOOD:  tickPickFood();    break;
            case SERVING_TABLE: tickServing();     break;
            case PATROLLING:    tickPatrol();      break;
            default:            tickIdle();        break;
        }
    }

    // ─── Các bước tick ────────────────────────────────────────────────

    private void tickCleaning() {
        if (targetTable == null || !targetTable.isDirty) {
            finishTask(); return;
        }
        if (moveTo(targetTable.x + 40, targetTable.y + 40)) {
            targetTable.isDirty = false;
            targetTable = null;
            finishTask();
        }
    }

    private void tickGoToCook() {
        if (targetApp == null) { finishTask(); return; }
        // Vị trí phía trước máy
        double appFrontX = targetApp.bounds.x + targetApp.bounds.width / 2.0;
        double appFrontY = targetApp.bounds.y + targetApp.bounds.height + 10;
        if (moveTo(appFrontX, appFrontY)) {
            // Đã đến máy — bắt đầu pha nếu máy rảnh
            if (!targetApp.isCooking && !targetApp.isReady) {
                long cookTime = (long)(2000 * cookSpeedMod);
                targetApp.startCooking(cookTime);
            }
            currentTask = Task.COOKING;
        }
    }

    private void tickCooking() {
        if (targetApp == null) { finishTask(); return; }
        // Đứng yên chờ
        animFrame = 0;
        if (targetApp.isReady) {
            currentTask = Task.PICKING_FOOD;
        }
    }

    private void tickPickFood() {
        if (targetApp == null || !targetApp.isReady) { finishTask(); return; }
        double fx = targetApp.bounds.x + targetApp.bounds.width / 2.0;
        double fy = targetApp.bounds.y + targetApp.bounds.height + 10;
        if (moveTo(fx, fy)) {
            holdingFood = targetApp.name;
            targetApp.reset();
            targetApp = null;
            // Nếu có bàn mục tiêu thì đi giao, ngược lại về idle
            if (targetTable != null) {
                currentTask = Task.SERVING_TABLE;
            } else {
                finishTask();
            }
        }
    }

    private void tickServing() {
        if (targetTable == null) { holdingFood = null; finishTask(); return; }
        Customer c = targetTable.getCustomer();
        if (c == null) { holdingFood = null; targetTable = null; finishTask(); return; }

        if (moveTo(targetTable.x + 40, targetTable.y - 15)) {
            // Giao đồ
            if (c.seated && !c.isEating && holdingFood != null
                    && c.getOrders().contains(holdingFood)) {
                gm.serveByStaff(c, holdingFood);
            }
            holdingFood   = null;
            targetTable   = null;
            finishTask();
        }
    }

    private void tickPatrol() {
        double[] pt = PATROL[patrolIdx];
        if (moveTo(pt[0], pt[1])) {
            patrolIdx = (patrolIdx + 1) % PATROL.length;
            finishTask();
        }
    }

    private void tickIdle() {
        animFrame = 0;
        idleTicks++;
        if (idleTicks > 100) {
            idleTicks = 0;
            currentTask = Task.PATROLLING;
        }
    }

    private void finishTask() {
        currentTask = Task.IDLE;
        idleTicks   = 0;
    }

    // ─── Di chuyển đơn giản ──────────────────────────────────────────
    private boolean moveTo(double tx, double ty) {
        double dx = tx - x;
        double dy = ty - y;
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist <= SPEED) {
            x = tx; y = ty;
            animFrame = 0;
            return true;
        }
        x += (dx / dist) * SPEED;
        y += (dy / dist) * SPEED;
        if (Math.abs(dx) > Math.abs(dy)) {
            direction = (dx > 0) ? 2 : 1;
        } else {
            direction = (dy > 0) ? 0 : 3;
        }
        if (animTick % 8 == 0) animFrame = (animFrame + 1) % 4;
        return false;
    }

    // ─── API cho GameManager ──────────────────────────────────────────

    /** Dọn bàn bẩn */
    public void assignClean(Table t) {
        currentTask = Task.CLEANING;
        targetTable = t;
        targetApp   = null;
        holdingFood = null;
    }

    /**
     * Đi đến máy pha, kích hoạt pha chế, sau đó lấy đồ và giao bàn t.
     * Gọi khi máy app chưa pha (isCooking == false && isReady == false).
     */
    public void assignCookAndServe(Appliance app, Table t) {
        currentTask = Task.GOING_TO_COOK;
        targetApp   = app;
        targetTable = t;
        holdingFood = null;
    }

    /**
     * Chỉ đi lấy đồ đã pha xong và giao cho khách bàn t.
     * Gọi khi máy app đã isReady == true.
     */
    public void assignPickAndServe(Appliance app, Table t) {
        currentTask = Task.PICKING_FOOD;
        targetApp   = app;
        targetTable = t;
        holdingFood = null;
    }

    /** Nhân viên có đang bận việc thực sự không? */
    public boolean isBusy() {
        return currentTask != Task.IDLE && currentTask != Task.PATROLLING;
    }

    // ─── Rendering helpers ────────────────────────────────────────────
    public int getDirection()  { return direction; }
    public int getAnimFrame()  { return animFrame; }

    public int getSpriteRow() {
        switch (direction) {
            case 1: return 1;
            case 2: return 2;
            case 3: return 3;
            default: return 0;
        }
    }

    public int getBounceOffset() {
        if (animFrame == 0) return (int)(Math.sin(animTick * 0.05) * 3);
        return (int)(Math.sin(animTick * 0.3) * 4);
    }

    /** Icon trạng thái hiện tại để hiển thị trên đầu nhân viên */
    public String getStatusIcon() {
        switch (currentTask) {
            case CLEANING:      return "🧹";
            case GOING_TO_COOK: return "🚶";
            case COOKING:       return "⏳";
            case PICKING_FOOD:  return "🤲";
            case SERVING_TABLE: return "🤝";
            case PATROLLING:    return "👣";
            default:            return "😊";
        }
    }

    /** Nhãn chữ ngắn cho HUD */
    public String getTaskLabel() {
        switch (currentTask) {
            case CLEANING:      return "Dọn bàn";
            case GOING_TO_COOK: return "Đi pha chế";
            case COOKING:       return "Đang pha...";
            case PICKING_FOOD:  return "Lấy đồ";
            case SERVING_TABLE: return "Phục vụ";
            case PATROLLING:    return "Tuần tra";
            default:            return "Rảnh";
        }
    }
}
