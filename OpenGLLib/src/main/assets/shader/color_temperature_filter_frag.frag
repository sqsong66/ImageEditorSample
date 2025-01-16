#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float temperature;

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    // 基本色彩调整
    float blueAdjustment = temperature * 0.1;
    float redAdjustment = -temperature * 0.1;
    // 应用色温调整
    vec3 adjustedColor = vec3(textureColor.r + redAdjustment, textureColor.g, textureColor.b + blueAdjustment);

    // 确保色彩值在合理范围内
    adjustedColor = clamp(adjustedColor, 0.0, 1.0);
    fragColor = vec4(adjustedColor, textureColor.a);
}