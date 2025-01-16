#version 300 es
precision mediump float; // 添加默认精度

out vec4 fragColor;
in vec2 fTexCoord;

uniform sampler2D uTexture;
uniform vec2 texOffset;        // 纹理坐标的偏移量，取决于纹理的尺寸
uniform float sharpness; // 色调调整角度，以度为单位

// 边缘检测函数，使用Sobel算子
float edgeDetection(vec2 texCoords) {
    float Gx[9] = float[9](
        -1.0, 0.0, 1.0,
        -2.0, 0.0, 2.0,
        -1.0, 0.0, 1.0
    );
    float Gy[9] = float[9](
        -1.0, -2.0, -1.0,
        0.0,  0.0,  0.0,
        1.0,  2.0,  1.0
    );

    float edgeX = 0.0;
    float edgeY = 0.0;
    int index = 0;
    for(int i = -1; i <= 1; i++) {
        for(int j = -1; j <= 1; j++) {
            vec2 offset = vec2(float(i), float(j)) * texOffset;
            float texValue = texture(uTexture, texCoords + offset).r; // Assuming a grayscale image
            edgeX += texValue * Gx[index];
            edgeY += texValue * Gy[index];
            index++;
        }
    }
    return sqrt(edgeX * edgeX + edgeY * edgeY);
}

void main() {
    // 检测当前像素的边缘强度
    float edgeStrength = edgeDetection(fTexCoord);

    // 计算锐化核心权重，考虑边缘强度
    float edgeFactor = smoothstep(0.2, 1.0, edgeStrength);
    float kernelCenter = 1.0 + 4.0 * sharpness * (1.0 - edgeFactor);

    float kernel[9] = float[9](
        0.0, -1.0 * sharpness * (1.0 - edgeFactor), 0.0,
        -1.0 * sharpness * (1.0 - edgeFactor), kernelCenter, -1.0 * sharpness * (1.0 - edgeFactor),
        0.0, -1.0 * sharpness * (1.0 - edgeFactor), 0.0
    );

    vec3 colorSum = vec3(0.0);
    int index = 0;
    for(int i = -1; i <= 1; i++) {
        for(int j = -1; j <= 1; j++) {
            vec2 offset = vec2(float(i), float(j)) * texOffset;
            colorSum += texture(uTexture, fTexCoord + offset).rgb * kernel[index];
            index++;
        }
    }
    fragColor = vec4(colorSum, 1.0);
}