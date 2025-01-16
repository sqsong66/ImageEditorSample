#version 300 es
precision mediump float; // 添加默认精度

out vec4 fragColor;
in vec2 fTexCoord;

uniform sampler2D uTexture;
uniform float vignetteSize; // 暗角的大小

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec3 texColor = textureColor.rgb;
    float alpha = textureColor.a;

    // 计算当前片段的纹理坐标与中心的距离（归一化坐标）
    vec2 center = vec2(0.5, 0.5); // 纹理的中心点为(0.5, 0.5)
    float distance = distance(fTexCoord, center);

    // 计算暗角系数，根据距离增加暗角的强度
    float edge0 = 1.0 - vignetteSize;
    float vignetteEffect = 1.0 - smoothstep(edge0, 1.0, distance);

    // 应用暗角效果
    texColor *= vignetteEffect; // 降低边缘的亮度
    texColor.rgb *= alpha; // 乘以alpha值，使得透明区域不显示

    fragColor = vec4(texColor, alpha);
}