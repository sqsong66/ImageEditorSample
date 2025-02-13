#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform lowp float temperature;
uniform lowp float hue; // 添加色相调整

const lowp vec3 warmFilter = vec3(0.93, 0.54, 0.0);

const mediump mat3 RGBtoYIQ = mat3(
    0.299, 0.587, 0.114,
    0.596, -0.274, -0.322,
    0.212, -0.523, 0.311
);

const mediump mat3 YIQtoRGB = mat3(
    1.0, 0.956, 0.621,
    1.0, -0.272, -0.647,
    1.0, -1.105, 1.702
);

const highp vec4 kRGBToYPrime = vec4(0.299, 0.587, 0.114, 0.0);
const highp vec4 kRGBToI = vec4(0.595716, -0.274453, -0.321263, 0.0);
const highp vec4 kRGBToQ = vec4(0.211456, -0.522591, 0.31135, 0.0);

const highp vec4 kYIQToR = vec4(1.0, 0.9563, 0.6210, 0.0);
const highp vec4 kYIQToG = vec4(1.0, -0.2721, -0.6474, 0.0);
const highp vec4 kYIQToB = vec4(1.0, -1.1070, 1.7046, 0.0);

void main()
{
    // 采样输入像素
    highp vec4 color = texture(uTexture, fTexCoord);

    // 转换为YIQ
    highp float YPrime = dot(color, kRGBToYPrime);
    highp float I = dot(color, kRGBToI);
    highp float Q = dot(color, kRGBToQ);

    // 计算色相和色度
    highp float hueAngle = atan(Q, I);
    highp float chroma = sqrt(I * I + Q * Q);

    // 调整色相
    hueAngle += hue;

    // 转换回YIQ
    Q = chroma * sin(hueAngle);
    I = chroma * cos(hueAngle);

    // 转换回RGB
    highp vec4 yIQ = vec4(YPrime, I, Q, 0.0);
    color.r = dot(yIQ, kYIQToR);
    color.g = dot(yIQ, kYIQToG);
    color.b = dot(yIQ, kYIQToB);

    // 调整色温
    mediump vec3 yiq = RGBtoYIQ * color.rgb;
    lowp vec3 rgb = YIQtoRGB * yiq;

    lowp vec3 processed = vec3(
        (rgb.r < 0.5 ? (2.0 * rgb.r * warmFilter.r) : (1.0 - 2.0 * (1.0 - rgb.r) * (1.0 - warmFilter.r))),
        (rgb.g < 0.5 ? (2.0 * rgb.g * warmFilter.g) : (1.0 - 2.0 * (1.0 - rgb.g) * (1.0 - warmFilter.g))),
        (rgb.b < 0.5 ? (2.0 * rgb.b * warmFilter.b) : (1.0 - 2.0 * (1.0 - rgb.b) * (1.0 - warmFilter.b)))
    );

    color.rgb = mix(rgb, processed, temperature);

    fragColor = vec4(color.rgb, color.a);
}
