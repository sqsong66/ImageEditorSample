#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float contrast;       // 对比度调整参数，大于1.0增加对比度，小于1.0减少对比度，等于1.0时无变化

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec3 rgb = ((textureColor.rgb - 0.5) * max(contrast, 0.0)) + 0.5;
    fragColor = vec4(rgb, textureColor.a);
}