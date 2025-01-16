#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform int uRadius;
uniform float uWidthOffset;
uniform float uHeightOffset;

mediump float getGaussWeight(mediump float currentPos, mediump float sigma) {
    return 1.0 / sigma * exp(-(currentPos * currentPos) / (2.0 * sigma * sigma));
}

void main() {
    int diameter = 2 * uRadius + 1;
    vec4 sampleTex = vec4(0, 0, 0, 0);
    vec3 col = vec3(0, 0, 0);
    float weightSum = 0.0;
    for (int i = 0; i < diameter; i++) {
        vec2 offset = vec2(float(i - uRadius) * uWidthOffset, float(i - uRadius) * uHeightOffset);
        sampleTex = vec4(texture(uTexture, fTexCoord.st + offset));
        float index = float(i);
        float gaussWeight = getGaussWeight(index - float(diameter - 1) / 2.0, (float(diameter - 1) / 2.0 + 1.0) / 2.0);
        col += sampleTex.rgb * gaussWeight;
        weightSum += gaussWeight;
    }
    fragColor = vec4(col / weightSum, sampleTex.a);
}