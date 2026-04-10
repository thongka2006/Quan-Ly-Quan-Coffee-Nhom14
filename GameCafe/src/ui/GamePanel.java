package ui;

import core.GameManager;
import input.InputHandler;
import model.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class GamePanel extends JPanel {

    private GameManager gameManager;

    private Image mapImg;
    private Image playerImg;
    private Image customerImg;
    private Image[] customerSkins = new Image[4]; // 4 different character PNGs

    int camX, camY;
    double currentCamX = 0, currentCamY = 0;

    Rectangle btnStart = new Rectangle(350, 260, 200, 60);
    Rectangle btnHelp  = new Rectangle(350, 340, 200, 60);
    Rectangle btnSettings = new Rectangle(350, 420, 200, 60);
    Rectangle btnExit  = new Rectangle(350, 500, 200, 60);

    // Intro & Settings Variables
    private float introAlpha = 0f;
    private long introStartTime = 0;
    
    // Shop Buttons
    Rectangle btnBuyEspresso  = new Rectangle();
    Rectangle btnBuyTray      = new Rectangle();
    Rectangle btnBuyChairs    = new Rectangle();
    Rectangle btnBuyTable     = new Rectangle();
    Rectangle btnHireStaff    = new Rectangle();
    Rectangle btnCloseShop    = new Rectangle();

    int mouseX = 0, mouseY = 0;

    public GamePanel() {

        gameManager = new GameManager();

        mapImg = new ImageIcon(getClass().getResource("/assets/map.png")).getImage();
        playerImg = new ImageIcon(getClass().getResource("/assets/player.png")).getImage();
        // Load 4 distinct character PNG sprite sheets
        customerSkins[0] = new ImageIcon(getClass().getResource("/assets/customer.png")).getImage();   // Girl - green jacket
        customerSkins[1] = new ImageIcon(getClass().getResource("/assets/customer2.png")).getImage();  // Boy - red jacket
        customerSkins[2] = new ImageIcon(getClass().getResource("/assets/customer3.png")).getImage();  // Girl - pink hoodie
        customerSkins[3] = new ImageIcon(getClass().getResource("/assets/customer4.png")).getImage();  // Man - blue suit
        customerImg = customerSkins[0]; // reference for frame size

        InputHandler input = new InputHandler(gameManager);
        gameManager.input = input;

        addKeyListener(input);
        setFocusable(true);
        requestFocusInWindow();

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                double scale = Math.min((double) getWidth() / 1022.0, (double) getHeight() / 680.0);
                double dx = (getWidth() - (1022 * scale)) / 2.0;
                double dy = (getHeight() - (680 * scale)) / 2.0;
                int mx = (int) ((e.getX() - dx) / scale);
                int my = (int) ((e.getY() - dy) / scale);

                if (gameManager.state == GameManager.GameState.MENU) {
                    if (btnStart.contains(mx, my)) gameManager.state = GameManager.GameState.PLAYING;
                    if (btnHelp.contains(mx, my)) gameManager.state = GameManager.GameState.HELP;
                    if (btnSettings.contains(mx, my)) gameManager.state = GameManager.GameState.SETTINGS;
                    if (btnExit.contains(mx, my)) System.exit(0);
                } else if (gameManager.state == GameManager.GameState.HELP) {
                    gameManager.state = GameManager.GameState.MENU;
                } else if (gameManager.state == GameManager.GameState.SETTINGS) {
                    int centerX = 1022 / 2;
                    int optY    = 200;
                    Rectangle btnMusic = new Rectangle(centerX + 20, optY - 30, 120, 40);
                    optY += 80;
                    Rectangle btnSound = new Rectangle(centerX + 20, optY - 30, 120, 40);

                    if (btnMusic.contains(mx, my)) core.SoundManager.toggleMusic();
                    else if (btnSound.contains(mx, my)) core.SoundManager.toggleSound();
                    else gameManager.state = GameManager.GameState.MENU;
                } else if (gameManager.state == GameManager.GameState.INTRO) {
                    gameManager.state = GameManager.GameState.MENU;
                } else if (gameManager.state == GameManager.GameState.GAME_OVER) {
                    gameManager.resetGame();
                } else if (gameManager.state == GameManager.GameState.SHOP) {
                    if (btnBuyEspresso.contains(mx, my)) gameManager.buyUpgrade("Espresso");
                    if (btnBuyTray.contains(mx, my))     gameManager.buyUpgrade("SilverTray");
                    if (btnBuyChairs.contains(mx, my))   gameManager.buyUpgrade("ComfyChairs");
                    if (btnBuyTable.contains(mx, my))    gameManager.buyUpgrade("BuyTable");
                    if (btnHireStaff.contains(mx, my))   gameManager.buyUpgrade("HireStaff");
                    if (btnCloseShop.contains(mx, my))   gameManager.state = GameManager.GameState.PLAYING;
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
                double scale = Math.min((double) getWidth() / 1022.0, (double) getHeight() / 680.0);
                double dx = (getWidth() - (1022 * scale)) / 2.0;
                double dy = (getHeight() - (680 * scale)) / 2.0;
                mouseX = (int) ((e.getX() - dx) / scale);
                mouseY = (int) ((e.getY() - dy) / scale);
            }
        });

        Timer timer = new Timer(16, e -> {
            gameManager.update();
            repaint();
        });

        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.BLACK); // Letterboxing background
        g2.fillRect(0, 0, getWidth(), getHeight());

        double scale = Math.min((double) getWidth() / 1022.0, (double) getHeight() / 680.0);
        double dx = (getWidth() - (1022 * scale)) / 2.0;
        double dy = (getHeight() - (680 * scale)) / 2.0;

        g2.translate(dx, dy);
        g2.scale(scale, scale);

        g2.setRenderingHint( RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );

        // Bounding clip inside virtual area
        g2.setClip(0, 0, 1022, 680);

        if (gameManager.state == GameManager.GameState.INTRO) {
            drawIntro(g2);
            return;
        }

        if (gameManager.state == GameManager.GameState.MENU) {
            drawMenu(g2);
            return;
        }

        if (gameManager.state == GameManager.GameState.HELP) {
            drawHelp(g2);
            return;
        }

        if (gameManager.state == GameManager.GameState.SETTINGS) {
            drawSettings(g2);
            return;
        }

        if (gameManager.state == GameManager.GameState.PAUSE) {
            g2.setColor(new Color(0,0,0,150));
            g2.fillRect(0,0,1022,680);
            g2.setColor(Color.YELLOW);
            g2.setFont(new Font("SansSerif", Font.BOLD, 40));
            g2.drawString("⏸ TẠM DỪNG", 1022/2 - 120, 250);
            return;
        }

        if (gameManager.state == GameManager.GameState.GAME_OVER) {
            drawGameOver(g2);
            return;
        }

        // ================= CAMERA =================
        double px = gameManager.player.x;
        double py = gameManager.player.y;

        double targetCamX = px - 1022 / 2.0;
        double targetCamY = py - 680 / 2.0;

        int mapW = mapImg.getWidth(null);
        int mapH = mapImg.getHeight(null);

        targetCamX = Math.max(0, Math.min(targetCamX, mapW - 1022));
        targetCamY = Math.max(0, Math.min(targetCamY, mapH - 680));

        if (currentCamX == 0 && currentCamY == 0) {
            currentCamX = targetCamX;
            currentCamY = targetCamY;
        } else {
            currentCamX += (targetCamX - currentCamX) * 0.1;
            currentCamY += (targetCamY - currentCamY) * 0.1;
        }

        camX = (int) currentCamX;
        camY = (int) currentCamY;

        // ================= MAP =================
        // Nền ngoài map khớp với màu cỏ bên ngoài của map.png (88,120,72)
        GradientPaint bgGrad = new GradientPaint(
            0, 0,          new Color(78, 110, 65),   // xanh cỏ đậm hơn ở trên
            0, 680, new Color(95, 128, 78)    // xanh cỏ sáng hơn ở dưới
        );
        g2.setPaint(bgGrad);
        g2.fillRect(0, 0, 1022, 680);
        g2.setPaint(null);
        g2.drawImage(mapImg, -camX, -camY, mapW, mapH, null);


        // ================= STATIONS (Moved to UI) =================

        // ================= STATIONS (Moved to UI) =================
        // Các hộp nguyên liệu trên Map đã được dời xuống UI Dock góc trái/phải để làm đẹp map

        // ================= CUSTOMERS & TABLES =================
        for (Table t : gameManager.tables) {
            if (t.isLocked) {
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRoundRect(t.x - camX, t.y - camY, 80, 80, 10, 10);
                g2.setFont(new Font("SansSerif", Font.BOLD, 24));
                g2.setColor(Color.WHITE);
                g2.drawString("🔒", t.x + 25 - camX, t.y + 45 - camY);
            } else if (t.isDirty) {
                // Realistic Dirty Table Effect (Stains, Crumbs, Empty Cups)
                int tx = t.x - camX;
                int ty = t.y - camY;
                
                // 1. Stain spot
                g2.setColor(new Color(101, 67, 33, 100)); // Brown liquid stain
                g2.fillOval(tx + 25, ty + 30, 20, 12);
                g2.fillOval(tx + 40, ty + 20, 12, 8);
                
                // 2. Crumpled White Napkin
                g2.setColor(new Color(245, 245, 245));
                g2.fillOval(tx + 18, ty + 20, 14, 12);
                g2.setColor(new Color(200, 200, 200));
                g2.drawOval(tx + 18, ty + 20, 14, 12);
                g2.drawArc(tx + 20, ty + 22, 6, 6, 0, 180); // crumpled detail
                
                // 3. Empty Coffee Cup
                g2.setColor(new Color(230, 230, 230)); // Cup body
                g2.fillRoundRect(tx + 45, ty + 35, 12, 16, 4, 4);
                g2.setColor(new Color(150, 150, 150));
                g2.drawRoundRect(tx + 45, ty + 35, 12, 16, 4, 4);
                g2.setColor(new Color(120, 80, 50, 180)); // Coffee leftover / sleeve
                g2.fillRoundRect(tx + 45, ty + 43, 12, 6, 2, 2);
                
                // 4. Bread Crumbs scattered
                g2.setColor(new Color(205, 133, 63)); // Peru color crumbs
                g2.fillOval(tx + 20, ty + 45, 3, 3);
                g2.fillOval(tx + 25, ty + 42, 4, 3);
                g2.fillOval(tx + 15, ty + 35, 3, 4);
                g2.fillOval(tx + 30, ty + 48, 2, 2);
                
                // 5. Blinking indicator "Cần dọn dẹp" (Clean!)
                if (System.currentTimeMillis() % 1000 < 500) {
                    g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                    g2.setColor(new Color(0, 0, 0, 150));
                    g2.drawString("CLEAN!", tx + 18, ty + 10);
                    g2.setColor(Color.WHITE);
                    g2.drawString("CLEAN!", tx + 17, ty + 9);
                }
            }
            if (t.getCustomer() != null) drawCustomer(g2, t.getCustomer(), camX, camY);
        }
        for (Customer c : gameManager.waitingQueue) {
            drawCustomer(g2, c, camX, camY);
        }

        // ================= PLAYER =================
        int pXInt = (int) px;
        int pYInt = (int) py;
        int pBounce = gameManager.player.getBounceOffset();

        g2.setColor(new Color(0,0,0,80));
        g2.fillOval(pXInt + 25 - camX, pYInt + 70 - camY, 30, 10);

        int frameW = playerImg.getWidth(null) / 4;
        int frameH = playerImg.getHeight(null) / 4;

        g2.drawImage(playerImg,
                pXInt - camX, pYInt - pBounce - camY,
                pXInt + 80 - camX, pYInt + 80 - pBounce - camY,
                gameManager.player.getAnimFrame() * frameW,
                gameManager.player.getSpriteRow() * frameH,
                (gameManager.player.getAnimFrame()+1)*frameW,
                (gameManager.player.getSpriteRow()+1)*frameH,
                null);

        drawApplianceProgress(g2, gameManager.cafeMachine, camX, camY);
        drawApplianceProgress(g2, gameManager.teaMachine, camX, camY);

        // ================= STAFF AI =================
        drawStaff(g2, camX, camY);

        // ================= EFFECTS =================
        for (Effect ef : gameManager.effects) {
            float alpha = (float)Math.max(0, ef.life / ef.maxLife);
            if (ef.isParticle) {
                g2.setColor(new Color(ef.color.getRed(), ef.color.getGreen(), ef.color.getBlue(), (int)(255 * alpha)));
                g2.fillOval((int)ef.x - camX, (int)ef.y - camY, 12, 12);
            } else {
                g2.setFont(new Font("SansSerif", Font.BOLD, 22));
                g2.setColor(new Color(0,0,0, (int)(255 * alpha)));
                g2.drawString(ef.text, (int)ef.x - camX + 2, (int)ef.y - camY + 2);
                g2.setColor(new Color(ef.color.getRed(), ef.color.getGreen(), ef.color.getBlue(), (int)(255 * alpha)));
                g2.drawString(ef.text, (int)ef.x - camX, (int)ef.y - camY);
            }
        }

        drawHUD(g2);
        
        if (gameManager.state == GameManager.GameState.SHOP) {
            drawShop(g2);
        }
    }
    
    private void drawMenu(Graphics2D g2) {
        int centerX = 1022 / 2;
        GradientPaint gradient = new GradientPaint( 0, 0, new Color(40, 20, 60), 0, 680, new Color(90, 40, 90) );
        g2.setPaint(gradient);
        g2.fillRect(0, 0, 1022, 680);

        g2.setFont(new Font("SansSerif", Font.BOLD, 65));
        g2.setColor(new Color(0,0,0,150));
        g2.drawString("CAFE GAME", centerX - 190 + 5, 120 + 5);

        g2.setColor(Color.WHITE);
        g2.drawString("CAFE GAME", centerX - 190, 120);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 40));
        g2.drawString("☕", centerX - 250, 120);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g2.setColor(new Color(255, 200, 200));
        g2.drawString("Pha chế - Phục vụ - Thành công", centerX - 120, 160);

        // High Score
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("🏆 KỶ LỤC: " + gameManager.highScore, centerX - 80, 210);

        int btnWidth = 220;
        int btnHeight = 55;
        int startX = centerX - btnWidth / 2;
        int startY = 250;
        int helpY = 325;
        int settY = 400;
        int exitY = 475;

        btnStart.setBounds(startX, startY, btnWidth, btnHeight);
        btnHelp.setBounds(startX, helpY, btnWidth, btnHeight);
        btnSettings.setBounds(startX, settY, btnWidth, btnHeight);
        btnExit.setBounds(startX, exitY, btnWidth, btnHeight);

        drawMenuBtn(g2, btnStart, "BẮT ĐẦU", new Color(0,200,100), new Color(0,255,120), Color.BLACK);
        drawMenuBtn(g2, btnHelp, "HƯỚNG DẪN", new Color(50,150,200), new Color(100,200,255), Color.WHITE);
        drawMenuBtn(g2, btnSettings, "CÀI ĐẶT", new Color(150,100,200), new Color(200,150,255), Color.WHITE);
        drawMenuBtn(g2, btnExit, "THOÁT", new Color(200,50,50), new Color(255,80,80), Color.WHITE);
    }

    private void drawMenuBtn(Graphics2D g2, Rectangle r, String text, Color base, Color hover, Color textCol) {
        boolean isHover = r.contains(mouseX, mouseY);
        g2.setColor(isHover ? hover : base);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 30, 30);
        g2.setColor(textCol);
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, r.x + (r.width/2) - (tw/2), r.y + 36);
    }

    private void drawIntro(Graphics2D g2) {
        if (introStartTime == 0) introStartTime = System.currentTimeMillis();
        long elapsed = System.currentTimeMillis() - introStartTime;

        if (elapsed < 1500) { // Fade In
            introAlpha = Math.min(1.0f, elapsed / 1500.0f);
        } else if (elapsed < 3500) { // Stay
            introAlpha = 1.0f;
        } else if (elapsed < 5000) { // Fade Out
            introAlpha = Math.max(0.0f, 1.0f - (elapsed - 3500) / 1500.0f);
        } else {
            gameManager.state = GameManager.GameState.MENU;
            return;
        }

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, 1022, 680);

        int centerX = 1022 / 2;
        int centerY = 680 / 2;

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, introAlpha));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 50));
        g2.drawString("☕ CAFE GAME Studio", centerX - 250, centerY - 20);
        
        g2.setFont(new Font("SansSerif", Font.ITALIC, 20));
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawString("Presents", centerX - 40, centerY + 30);
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void drawSettings(Graphics2D g2) {
        int centerX = 1022 / 2;
        g2.setColor(new Color(30, 30, 40));
        g2.fillRect(0, 0, 1022, 680);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 45));
        g2.drawString("CÀI ĐẶT", centerX - 80, 100);

        int optY = 200;
        g2.setFont(new Font("SansSerif", Font.BOLD, 24));
        
        // Music Toggle
        g2.drawString("Âm nhạc:", centerX - 150, optY);
        boolean musicOn = core.SoundManager.isMusicOn();
        Rectangle btnMusic = new Rectangle(centerX + 20, optY - 30, 120, 40);
        drawToggleBtn(g2, btnMusic, musicOn ? "BẬT" : "TẮT", musicOn);

        // Sound Toggle
        optY += 80;
        g2.drawString("Hiệu ứng:", centerX - 150, optY);
        boolean soundOn = core.SoundManager.isSoundOn();
        Rectangle btnSound = new Rectangle(centerX + 20, optY - 30, 120, 40);
        drawToggleBtn(g2, btnSound, soundOn ? "BẬT" : "TẮT", soundOn);

        // High Score display again
        optY += 100;
        g2.setColor(new Color(255, 215, 0));
        g2.drawString("ĐIỂM CAO NHẤT: " + gameManager.highScore, centerX - 120, optY);

        g2.setColor(new Color(200, 255, 100));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
        g2.drawString("> Nhấp chuột vào khu vực bất kỳ để Quay lại Menu <", centerX - 210, 680 - 100);
    }

    private void drawToggleBtn(Graphics2D g2, Rectangle r, String text, boolean active) {
        g2.setColor(active ? new Color(0, 200, 0) : new Color(200, 0, 0));
        if (r.contains(mouseX, mouseY)) g2.setColor(g2.getColor().brighter());
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        int tw = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, r.x + (r.width/2) - (tw/2), r.y + 26);
    }

    private void drawHelp(Graphics2D g2) {
        int centerX = 1022 / 2;
        GradientPaint gradient = new GradientPaint( 0, 0, new Color(20, 60, 80), 0, 680, new Color(10, 30, 50) );
        g2.setPaint(gradient);
        g2.fillRect(0, 0, 1022, 680);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 45));
        g2.drawString("HƯỚNG DẪN CHƠI", centerX - 215, 80);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2.setColor(new Color(200, 255, 255));
        
        int y = 140;
        int lineSpacing = 40;
        g2.drawString("🟢 Sử dụng phím W, A, S, D để di chuyển nhân vật.", 80, y); y += lineSpacing;
        g2.drawString("🟢 Nhấn SPACE đứng gần bàn hoặc quầy để tương tác.", 80, y); y += lineSpacing;
        g2.drawString("🟢 Nhấn phím Q để vứt nguyên liệu thừa đang cầm trên tay.", 80, y); y += lineSpacing;
        g2.drawString("🟢 Nhấn phím B để mở Cửa Hàng nâng cấp thiết bị và nhân viên.", 80, y); y += lineSpacing;
        g2.drawString("🔴 Hãy phục vụ đồ ăn/nước uống chính xác trước khi khách nổi giận!", 80, y); y += lineSpacing;
        g2.drawString("🔥 Mẹo: Ghép các combo liên tiếp bằng cách phục vụ nhanh để nhân điểm lên x2, x3.", 80, y); y += lineSpacing;
        
        g2.setColor(new Color(255, 255, 100));
        g2.setFont(new Font("SansSerif", Font.BOLD, 22));
        g2.drawString("> Nhấp chuột vào khu vực bất kỳ để Quay lại Menu <", centerX - 270, 680 - 60);
    }
    
    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,200));
        g2.fillRect(0,0,1022,680);
        g2.setColor(Color.RED);
        g2.setFont(new Font("SansSerif", Font.BOLD, 50));
        g2.drawString("💀 GAME OVER!", 1022/2 - 165, 250);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
        g2.drawString("Click anywhere to restart", 1022/2 - 110, 320);
    }
    
    private void drawShop(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 220));
        g2.fillRect(0, 0, 1022, 680);

        int centerX = 1022 / 2;

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 42));
        g2.drawString("NÂNG CẤP TÀI SẢN", centerX - 215, 95);

        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        g2.setColor(Color.YELLOW);
        g2.drawString("TIỀN: " + gameManager.money + " VNĐ", centerX - 70, 135);

        int btnW = 520;
        int btnH = 48;
        int bx   = centerX - btnW / 2;

        // Hàng 0: Thuê nhân viên
        btnHireStaff.setBounds(bx, 165, btnW, btnH);
        // Hàng 1-4: nâng cấp khác
        btnBuyEspresso.setBounds(bx, 223, btnW, btnH);
        btnBuyTray.setBounds    (bx, 281, btnW, btnH);
        btnBuyChairs.setBounds  (bx, 339, btnW, btnH);
        btnBuyTable.setBounds   (bx, 397, btnW, btnH);

        btnCloseShop.setBounds(centerX - 100, 462, 200, 52);

        // --- Nút Thuê Nhân Viên ---
        drawShopBtn(g2, btnHireStaff,
            "👩‍🍳 Thuê Nhân Viên (" + GameManager.STAFF_COST + " VNĐ) - Tự pha & phục vụ",
            gameManager.hasStaff, gameManager.money >= GameManager.STAFF_COST);

        drawShopBtn(g2, btnBuyEspresso,
            "Máy Espresso (50,000 VNĐ) - Pha chế x2",
            gameManager.hasEspresso, gameManager.money >= 50000);
        drawShopBtn(g2, btnBuyTray,
            "Khay Bạc (100,000 VNĐ) - Bê 4 Món",
            gameManager.hasSilverTray, gameManager.money >= 100000);
        drawShopBtn(g2, btnBuyChairs,
            "Ghế Đệm mềm (150,000 VNĐ) - Tăng Kiên Nhẫn",
            gameManager.hasComfyChairs, gameManager.money >= 150000);

        int lockedTables = 0;
        for (Table t : gameManager.tables) if (t.isLocked) lockedTables++;
        drawShopBtn(g2, btnBuyTable,
            "Mở Bàn Mới (200,000 VNĐ) - Còn " + lockedTables,
            lockedTables == 0, gameManager.money >= 200000);

        // Nút đóng
        boolean hoverClose = btnCloseShop.contains(mouseX, mouseY);
        g2.setColor(hoverClose ? Color.LIGHT_GRAY : Color.GRAY);
        g2.fillRoundRect(btnCloseShop.x, btnCloseShop.y, btnCloseShop.width, btnCloseShop.height, 20, 20);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("ĐÓNG CỬA HÀNG", btnCloseShop.x + 18, btnCloseShop.y + 34);
    }
    
    private void drawShopBtn(Graphics2D g2, Rectangle r, String text, boolean owned, boolean canAfford) {
        boolean hover = r.contains(mouseX, mouseY);
        
        if (owned) {
            g2.setColor(new Color(100, 255, 100, 150)); // Bought
        } else if (!canAfford) {
            g2.setColor(new Color(255, 100, 100, 150)); // Too expensive
        } else {
            g2.setColor(hover ? new Color(150, 150, 150) : new Color(100, 100, 100));
        }
        
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 20, 20);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString(text + (owned ? " [SỞ HỮU]" : ""), r.x + 20, r.y + 35);
    }
    
    private void drawHUD(Graphics2D g2) {
        // 1. Top-Left Panel
        int panelWidth = 230;
        int panelHeight = 130; 
        g2.setColor(new Color(50, 50, 50, 200));
        g2.fillRoundRect(20, 20, panelWidth, panelHeight, 20, 20);
        
        g2.setColor(new Color(200, 200, 50, 200));
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(20, 20, panelWidth, panelHeight, 20, 20);

        g2.setFont(new Font("SansSerif", Font.PLAIN, 22));
        g2.setColor(Color.WHITE);
        g2.drawString("CẤP ĐỘ: ", 40, 55);
        g2.setColor(new Color(245, 100, 245)); // Magenta
        g2.drawString("" + gameManager.level, 140, 55);
        
        g2.setColor(Color.WHITE);
        g2.drawString("ĐIỂM: ", 40, 90);
        g2.setColor(new Color(100, 200, 255)); // Light Blue
        g2.drawString("+" + gameManager.score, 110, 90);
        
        g2.setColor(Color.WHITE);
        g2.drawString("TIỀN: ", 40, 125);
        g2.setColor(new Color(255, 215, 0)); // Gold
        g2.drawString(gameManager.money + " VNĐ", 105, 125);
        
        if (gameManager.comboCount > 1) {
            g2.setColor(new Color(0, 255, 100)); // Bright Green
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2.drawString("COMBO x" + gameManager.comboCount, 40, 155);
        }
        
        // 2. Help Hint
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
        g2.drawString("Nhấn 'B' để MỞ CỬA HÀNG", 20, 180);
        g2.drawString("Nhấn 'Q' để Vứt Đồ", 20, 205);

        // 2b. Staff status
        if (gameManager.hasStaff && gameManager.staff != null) {
            model.Staff s = gameManager.staff;
            int spY = 220;
            g2.setColor(new Color(20, 80, 160, 200));
            g2.fillRoundRect(20, spY, 200, 50, 14, 14);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.drawString("NV: " + s.getStatusIcon() + " " + s.getTaskLabel(), 32, spY + 20);
        }

        // 3. Top-Right Panel (Holding Foods Array)
        ArrayList<Food> holding = gameManager.player.getHoldingFoods();
        int holdPanelWidth = 300;
        int holdPanelHeight = 90;
        int startXUI = 1022 - holdPanelWidth - 20;

        g2.setColor(new Color(60, 60, 60, 230)); 
        g2.fillRoundRect(startXUI, 20, holdPanelWidth, holdPanelHeight, 20, 20);
        
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.setColor(Color.WHITE);
        g2.drawString("TRÊN KHAY (" + holding.size() + "/" + gameManager.trayCapacity + "):", startXUI + 20, 45);
        
        if (holding.isEmpty()) {
            g2.setColor(Color.LIGHT_GRAY);
            g2.drawString("TRỐNG!", startXUI + 110, 75);
        } else {
            g2.setColor(new Color(200, 200, 200));
            String items = "";
            for(Food f : holding) items += f.getName() + " ";
            g2.drawString(items.trim(), startXUI + 20, 75);
        }

        drawDock(g2);
    }

    private void drawDock(Graphics2D g2) {
        int w = 1022;
        int h = 680;

        // --- Bottom Right: Ingredient Stations ---
        String[] titles = {"Lá Trà", "Hạt Cafe", "Bánh Mì", "Nước Pha"};
        String[] icons = {"🌿", "🌰", "🥖", "💧"};
        int boxW = 85; 
        int boxH = 100;
        int spacing = 10;
        int totalW = (boxW * 4) + (spacing * 3);
        int startX = w - totalW - 20;
        int startY = h - boxH - 20;

        for (int i = 0; i < 4; i++) {
            int cx = startX + i * (boxW + spacing);
            // Box back
            g2.setColor(new Color(110, 60, 30));
            g2.fillRoundRect(cx, startY, boxW, boxH, 15, 15);
            g2.setColor(new Color(200, 200, 200, 100)); // Lighter transparent border
            g2.setStroke(new BasicStroke(2));
            g2.drawRoundRect(cx, startY, boxW, boxH, 15, 15);

            // Icon
            g2.setFont(new Font("SansSerif", Font.PLAIN, 32));
            g2.drawString(icons[i], cx + 25, startY + 40);

            // Text
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(Color.WHITE);
            int tw = g2.getFontMetrics().stringWidth(titles[i]);
            g2.drawString(titles[i], cx + (boxW/2) - (tw/2), startY + 65);

            // Status bar at bottom
            g2.setColor(new Color(40, 50, 30));
            g2.fillRoundRect(cx, startY + boxH - 25, boxW, 25, 15, 15); // Cover bottom rounded part
            g2.fillRect(cx, startY + boxH - 25, boxW, 15); // Make top part straight, leaving bottom rounded
            
            g2.setColor(new Color(150, 255, 100)); // Vivid light green
            g2.setFont(new Font("SansSerif", Font.BOLD, 10));
            String unlocked = "UNLOCKED";
            int uw = g2.getFontMetrics().stringWidth(unlocked);
            g2.drawString(unlocked, cx + (boxW/2) - (uw/2), startY + boxH - 8);
        }
    }


    private void drawCustomer(Graphics2D g2, Customer c, int camX, int camY) {
        int cx = (int) c.x;
        int cy = (int) c.y;
        int bounce = c.getBounceOffset();

        int frameW = customerImg.getWidth(null) / 4;
        int frameH = customerImg.getHeight(null) / 4;
        
        // Calculate positions for standing vs seated
        int drawX = cx;
        int drawY = cy;
        int drawDir = c.getDirection();
        
        if (c.seated) {
            // Ngồi vào ghế trên để bị che như user vừa yều cầu
            drawX = cx; 
            drawY = cy - 35; 
            drawDir = 0; 
        }
        
        g2.setColor(new Color(0,0,0,50));
        g2.fillOval(drawX + 20 - camX, drawY + 60 - camY, 30, 10);
        
        int row = 0;
        switch (drawDir) {
            case 0: row = 0; break;
            case 1: row = 1; break;
            case 2: row = 2; break;
            case 3: row = 3; break;
        }
        
        Image skinToDraw = customerSkins[c.skinIds[0]];

        // Setup crop for occlusion if sitting behind the table
        int destY2 = drawY + 80 - camY - bounce;
        int srcY2 = (row + 1) * frameH;
        
        if (c.seated) {
            destY2 -= 30; // Crop 30 pixels off the bottom
            srcY2 -= (int)(30.0f * frameH / 80.0f);
        }
        
        g2.drawImage(skinToDraw, 
                drawX - camX, drawY - camY - bounce, 
                drawX + 80 - camX, destY2,
                c.getAnimFrame() * frameW, row * frameH,
                (c.getAnimFrame()+1)*frameW, srcY2, null);

        // Draw served foods visually placed on the table
        if (c.seated) {
            ArrayList<String> servedFoods = c.getServedFoods();
            int foodX = cx + 20 - camX;
            int foodY = cy + 5 - camY;
            
            for (int i = 0; i < servedFoods.size(); i++) {
                int dx = (i % 2 == 0) ? -15 : 15;
                int dy = (i / 2) * 15;
                drawFoodVisual(g2, servedFoods.get(i), foodX + dx, foodY + dy);
            }
        }

        // Draw emotion and Orders List over the Leader!
        Font fallbackFont = new Font("SansSerif", Font.PLAIN, 20);
        g2.setFont(fallbackFont);
        g2.drawString(c.getEmotion(), cx + 30 - camX, cy - 10 - camY - bounce);
        
        // Draw physical orders directly over their heads!
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        ArrayList<String> orders = c.getOrders();
        int ordersY = cy - 35 - bounce;
        
        int colOffset = 0;
        int count = 0;
        for (int i=0; i < orders.size(); i++) {
            g2.setColor(new Color(30, 30, 30, 200));
            g2.fillRect(cx - 15 - camX + colOffset, ordersY - 14, 108, 18);
            
            g2.setColor(Color.WHITE);
            g2.drawString(orders.get(i), cx - 10 - camX + colOffset, ordersY);
            ordersY -= 20;
            count++;
            if (count == 4) { // Next column
                colOffset = 110;
                ordersY = cy - 35 - bounce;
            }
        }
    }

    // ================= STAFF NPC DRAW =================
    private void drawStaff(Graphics2D g2, int camX, int camY) {
        if (!gameManager.hasStaff || gameManager.staff == null) return;
        model.Staff s = gameManager.staff;
        int sx = (int) s.x;
        int sy = (int) s.y;
        int bounce = s.getBounceOffset();

        // Shadow
        g2.setColor(new Color(0, 0, 0, 70));
        g2.fillOval(sx + 25 - camX, sy + 68 - camY, 30, 10);

        // Sprite
        Image skin = customerSkins[s.skinId % customerSkins.length];
        int fW = skin.getWidth(null)  / 4;
        int fH = skin.getHeight(null) / 4;
        int row = s.getSpriteRow();
        int col = s.getAnimFrame();

        g2.drawImage(skin,
                sx - camX,       sy - bounce - camY,
                sx + 80 - camX,  sy + 80 - bounce - camY,
                col * fW, row * fH, (col + 1) * fW, (row + 1) * fH, null);

        // Badge "NV"
        int badgeX = sx + 44 - camX;
        int badgeY = sy - bounce - 4 - camY;
        g2.setColor(new Color(20, 100, 220));
        g2.fillRoundRect(badgeX, badgeY - 15, 34, 16, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
        g2.drawString("NV", badgeX + 9, badgeY - 3);

        // Icon trạng thái
        String icon = s.getStatusIcon();
        if (!icon.isEmpty()) {
            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g2.drawString(icon, sx + 28 - camX, sy - bounce - 20 - camY);
        }

        // Món đang mang
        if (s.holdingFood != null) {
            drawFoodVisual(g2, s.holdingFood, sx + 54 - camX, sy + 24 - camY - bounce);
        }
    }

    private void drawApplianceProgress(Graphics2D g2, Appliance app, int camX, int camY) {
        if (app.isCooking) {
            int px = app.bounds.x - camX + 25;
            int py = app.bounds.y - camY + 20;
            g2.setColor(new Color(0,0,0, 150));
            g2.fillRoundRect(px, py, 100, 12, 5, 5);
            g2.setColor(new Color(0, 255, 100));
            g2.fillRoundRect(px, py, (int)(100 * app.getProgress()), 12, 5, 5);
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(1));
            g2.drawRoundRect(px, py, 100, 12, 5, 5);
            
            // Draw visually preparing food
            drawFoodVisual(g2, app.name, px + 35, py - 20);
            
            // Draw current step name
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(new Color(255, 255, 150));
            String stepName = app.getCurrentStepName();
            int textWidth = g2.getFontMetrics().stringWidth(stepName);
            g2.drawString(stepName, px + 50 - (textWidth / 2), py + 26);
            
        } else if (app.isReady) {
            int px = app.bounds.x - camX + 25;
            int py = app.bounds.y - camY + 20;
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            String stepName = app.getCurrentStepName();
            int textWidth = g2.getFontMetrics().stringWidth(stepName);
            g2.drawString(stepName, px + 51 - (textWidth / 2), py + 12);
            g2.setColor(Color.GREEN);
            g2.drawString(stepName, px + 50 - (textWidth / 2), py + 11);
            
            // Draw visually ready food
            drawFoodVisual(g2, app.name, px + 35, py - 20);
        }
    }
    
    private void drawFoodVisual(Graphics2D g2, String food, int x, int y) {
        Color bgColor = Color.GRAY;
        String icon = "🍽️";
        
        if (food.equals("Cafe Sua")) {
            bgColor = new Color(139, 69, 19); // SaddleBrown
            icon = "☕";
        } else if (food.equals("Tra Dao")) {
            bgColor = new Color(255, 140, 0); // DarkOrange
            icon = "🍹";
        } else if (food.equals("Banh Mi")) {
            bgColor = new Color(244, 164, 96); // SandyBrown
            icon = "🥖";
        } else if (food.equals("Nước Pha")) {
            bgColor = new Color(0, 191, 255); // DeepSkyBlue
            icon = "💧";
        } else if (food.equals("Rác")) {
            bgColor = new Color(105, 105, 105); // DimGray
            icon = "🤢";
        }
        
        g2.setColor(bgColor);
        g2.fillRoundRect(x, y, 32, 24, 8, 8);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2));
        g2.drawRoundRect(x, y, 32, 24, 8, 8);
        
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.drawString(icon, x + 6, y + 17);
    }
}