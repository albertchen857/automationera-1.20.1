package com.automationera.ui;

public class IsometricRenderState {
    public float rotation = 45f; // 旋转角度（度）
    public float pitch = -15f;    // 仰角（度）
    public float scale = 0.5f;     // 缩放倍数
    public float yc = 0f;

    public void addRotation(float delta) { rotation += delta; }
    public void addPitch(float delta) { pitch = Math.max(-89, Math.min(89, pitch + delta)); }
    public void addScale(float factor) { scale = Math.max(0.2f, Math.min(2f, scale * factor)); }
    public void addYc(float y) { yc = Math.max(-30f, Math.min(30f, yc + y));}
}
