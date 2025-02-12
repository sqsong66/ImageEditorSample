#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;

void main() {
    fragColor = texture(uTexture, fTexCoord);
}