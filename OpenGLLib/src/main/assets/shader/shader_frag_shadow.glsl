#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float shadowStrength;

vec3 luminanceWeighting = vec3(0.3, 0.3, 0.3);

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    float brightness = dot(textureColor.rgb, vec3(0.299, 0.587, 0.114)); // 计算亮度
    float shadow = 1.0 - (1.0 - brightness) * shadowStrength; // 应用阴影滤镜
    fragColor = vec4(textureColor.rgb * shadow, textureColor.a);
}