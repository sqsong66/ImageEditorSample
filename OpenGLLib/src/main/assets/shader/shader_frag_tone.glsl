#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform float tone; // 色调调整角度，以度为单位

// 函数：将RGB颜色转换为HSV
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// 函数：将HSV颜色转换回RGB
vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    // 转换RGB到HSV
    vec3 hsv = rgb2hsv(textureColor.rgb);
    // 调整色调
    hsv.x += tone / 360.0; // 将度转换为归一化的值
    hsv.x = mod(hsv.x, 1.0); // 确保色调值在0到1之间
    // 转换HSV回RGB
    vec3 rgb = hsv2rgb(hsv);
    fragColor = vec4(rgb, 1.0);
}