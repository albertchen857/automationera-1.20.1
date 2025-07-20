// com.automationera.ui.IsometricRenderState.java
package com.automationera.ui;

public class IsometricRenderState {
    public float rotation = 45f; // 旋转角度（度）
    public float pitch = 30f;    // 仰角（度）
    public float scale = 2f;     // 缩放倍数

    public void addRotation(float delta) { rotation += delta; }
    public void addPitch(float delta) { pitch = Math.max(-89, Math.min(89, pitch + delta)); }
    public void addScale(float factor) { scale = Math.max(0.3f, Math.min(8f, scale * factor)); }
}
