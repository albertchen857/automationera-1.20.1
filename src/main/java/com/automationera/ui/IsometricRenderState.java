package com.automationera.ui;

public class IsometricRenderState {
    public float rotation = 60f; // 旋转角度（度）
    public float pitch = -30f;    // 仰角（度）
    public float scale = 0.5f;     // 缩放倍数
    public float yc = 0f;

    public void addRotation(float delta) { rotation += delta; }
    public void addPitch(float delta) { pitch -= delta; }
    public void addScale(float factor) { scale = Math.max(0.1f, Math.min(2f, scale * factor)); }
    public void addYc(float y) { yc = Math.max(-50f, Math.min(30f, yc + y));}
}
