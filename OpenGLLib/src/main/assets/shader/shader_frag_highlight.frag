#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float threshold;    // 高光阈值，决定了什么亮度级别的像素会被视为高光
uniform float highlight;    // 高光亮度增强系数

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    float luminance = dot(textureColor.rgb, vec3(0.299, 0.587, 0.114));  // 计算亮度（灰度）
    float mask = smoothstep(threshold, threshold + 0.1, luminance);  // 创建一个平滑阈值掩码

    // 仅在高光区域增强亮度
    vec3 brightened = textureColor.rgb + mask * highlight * textureColor.rgb;
    fragColor = vec4(brightened, textureColor.a);
}