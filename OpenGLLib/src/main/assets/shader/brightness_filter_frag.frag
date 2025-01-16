#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float brightness; // 亮度调整参数，范围可以是从-1.0（更暗）到1.0（更亮）

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec4 brightnessColor = textureColor + vec4(brightness, brightness, brightness, 0.0);
    fragColor = vec4(clamp(brightnessColor.rgb, 0.0, 1.0), textureColor.a);
}