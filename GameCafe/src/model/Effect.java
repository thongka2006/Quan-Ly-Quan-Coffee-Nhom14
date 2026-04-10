package model;

import java.awt.Color;

public class Effect {
    public double x, y;
    public double life;
    public double maxLife;
    public String text;
    public Color color;
    public boolean isParticle; // True if just a smoke particle

    public Effect(String text, double x, double y, Color color, double maxLife) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.isParticle = false;
    }

    public Effect(double x, double y, Color color, double maxLife) {
        this.text = "";
        this.x = x;
        this.y = y;
        this.color = color;
        this.maxLife = maxLife;
        this.life = maxLife;
        this.isParticle = true;
    }

    public void update() {
        this.life -= 1;
        this.y -= 0.5; // Float upwards
        this.x += (Math.random() - 0.5) * 0.5; // Slight jitter
    }

    public boolean isDead() {
        return this.life <= 0;
    }
}
