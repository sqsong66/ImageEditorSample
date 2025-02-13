#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float strength; // 锐化强度

void main() {
    float kernel[9];
    vec2 tex_offset = 1.0 / vec2(textureSize(uTexture, 0)); // 显式类型转换，保证类型匹配

    // 使用锐化强度参数调整锐化内核
    kernel[0] = 0.0; kernel[1] = -1.0 * strength; kernel[2] = 0.0;
    kernel[3] = -1.0 * strength; kernel[4] = 4.0 * strength + 1.0; kernel[5] = -1.0 * strength;
    kernel[6] = 0.0; kernel[7] = -1.0 * strength; kernel[8] = 0.0;

    vec3 color = vec3(0.0);
    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
            vec3 sampleTex = texture(uTexture, fTexCoord + tex_offset * vec2(i - 1, j - 1)).rgb;
            color += sampleTex * kernel[i * 3 + j];
        }
    }
    float alpha = texture(uTexture, fTexCoord).a;
    fragColor = vec4(color, alpha);
}
