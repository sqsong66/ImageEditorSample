#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float saturation; // 饱和度调整参数，大于1.0增加饱和度，小于1.0减少饱和度，等于1.0时无变化

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    float luminance = 0.299 * textureColor.r + 0.587 * textureColor.g + 0.114 * textureColor.b;  // 计算灰度
    vec4 gray = vec4(luminance, luminance, luminance, 1.0);
    fragColor = vec4(mix(gray.rgb, textureColor.rgb, saturation), textureColor.a);
}