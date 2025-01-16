#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float exposure; // 曝光度； 大于0增加曝光，小于0减少曝光

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec4 exposedColor = textureColor * pow(2.0, exposure);
    fragColor = vec4(exposedColor.rgb, textureColor.a);
}