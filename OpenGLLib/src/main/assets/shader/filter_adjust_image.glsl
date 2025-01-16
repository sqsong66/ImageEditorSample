#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;
uniform sampler2D uTexture;

uniform int width;
uniform int height;
uniform float brightness;   // 亮度参数调整，范围可以是从-1.0（更暗）到1.0（更亮）
uniform float contrast;     // 对比度参数调整，大于1.0增加对比度，小于1.0减少对比度，等于1.0时无变化
uniform float saturation;   // 饱和度参数调整，大于1.0增加饱和度，小于1.0减少饱和度，等于1.0时无变化
uniform float exposure;     // 曝光度参数调整，默认为0
uniform float shadows;      // 调节阴影， 默认为1
uniform float highlights;   // 调节高光，默认为1
uniform float temperature;  // 色温调整, 默认为0
uniform float hue;          // 色调调整，默认为0
uniform float sharpen;      // 锐化，默认为0
uniform float vignette;     // 暗角，默认为0

const mediump vec3 satLuminanceWeighting = vec3(0.2126, 0.7152, 0.0722);
const mediump vec3 hsLuminanceWeighting = vec3(0.3, 0.3, 0.3);
const mediump vec3 warmFilter = vec3(0.93, 0.54, 0.0);

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

const mediump vec4 kRGBToYPrime = vec4(0.299, 0.587, 0.114, 0.0);
const mediump vec4 kRGBToI = vec4(0.595716, -0.274453, -0.321263, 0.0);
const mediump vec4 kRGBToQ = vec4(0.211456, -0.522591, 0.31135, 0.0);
const mediump vec4 kYIQToR = vec4(1.0, 0.9563, 0.6210, 0.0);
const mediump vec4 kYIQToG = vec4(1.0, -0.2721, -0.6474, 0.0);
const mediump vec4 kYIQToB = vec4(1.0, -1.1070, 1.7046, 0.0);

void colorHueWarmth(inout vec4 color, float hueValue, float warthValue) {
    // 转换为YIQ
    highp float YPrime = dot(color, kRGBToYPrime);
    highp float I = dot(color, kRGBToI);
    highp float Q = dot(color, kRGBToQ);
    // 计算色相和色度
    highp float hueAngle = atan(Q, I);
    highp float chroma = sqrt(I * I + Q * Q);
    // 调整色相
    hueAngle += hueValue;
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
    color.rgb = mix(rgb, processed, warthValue);
}

vec4 colorSharpen(vec2 uv, vec4 color, float sharpenValue) {
    float kernel[9];
    vec2 tex_offset = 1.0 / vec2(textureSize(uTexture, 0)); // 获取纹理大小的倒数
    // 使用锐化强度参数调整锐化内核
    kernel[0] = 0.0; kernel[1] = -1.0 * sharpenValue; kernel[2] = 0.0;
    kernel[3] = -1.0 * sharpenValue; kernel[4] = 4.0 * sharpenValue + 1.0; kernel[5] = -1.0 * sharpenValue;
    kernel[6] = 0.0; kernel[7] = -1.0 * sharpenValue; kernel[8] = 0.0;

    vec3 newColor = vec3(0.0);
    for (int i = 0; i < 3; i++) {
        for (int j = 0; j < 3; j++) {
            vec3 sampleTex = texture(uTexture, fTexCoord + tex_offset * vec2(i - 1, j - 1)).rgb;
            newColor += sampleTex * kernel[i * 3 + j];
        }
    }
    return vec4(newColor, color.a);
}

void colorHighlightsShadows(inout vec4 color, float highlights, float shadows) {
    mediump float hsLuminance = dot(color.rgb, hsLuminanceWeighting);
    mediump float shadow = clamp((pow(hsLuminance, 1.0 / shadows) + (-0.76) * pow(hsLuminance, 2.0 / shadows)) - hsLuminance, 0.0, 1.0);
    mediump float highlight = clamp((1.0 - (pow(1.0 - hsLuminance, 1.0 / (2.0 - highlights)) + (-0.8) * pow(1.0 - hsLuminance, 2.0 / (2.0 - highlights)))) - hsLuminance, -1.0, 0.0);
    lowp vec3 hsresult = vec3(0.0, 0.0, 0.0) + ((hsLuminance + shadow + highlight) - 0.0) * ((color.rgb - vec3(0.0, 0.0, 0.0)) / (hsLuminance - 0.0));
    mediump float contrastedLuminance = ((hsLuminance - 0.5) * 1.5) + 0.5;
    mediump float whiteInterp = contrastedLuminance * contrastedLuminance * contrastedLuminance;
    mediump float whiteTarget = clamp(highlights, 1.0, 2.0) - 1.0;
    hsresult = mix(hsresult, vec3(1.0), whiteInterp * whiteTarget);
    mediump float invContrastedLuminance = 1.0 - contrastedLuminance;
    mediump float blackInterp = invContrastedLuminance * invContrastedLuminance * invContrastedLuminance;
    mediump float blackTarget = 1.0 - clamp(shadows, 0.0, 1.0);
    hsresult = mix(hsresult, vec3(0.0), blackInterp * blackTarget);
    color = vec4(hsresult.rgb, color.a);
}

void colorVignette(inout vec4 color, float vignette) {
    vec2 center = vec2(0.5, 0.5); // 纹理的中心点为(0.5, 0.5)
    float distance = distance(fTexCoord, center);
    // 计算暗角系数，根据距离增加暗角的强度
    float edge0 = 1.0 - vignette;
    float vignetteEffect = 1.0 - smoothstep(edge0, 1.0, distance);
    // 应用暗角效果
    color.rgb *= vignetteEffect; // 降低边缘的亮度
}

void colorExplosure(inout vec4 color, float exposure) {
    mediump float mag = exposure * 1.045;
    mediump float exppower = 1.0 + abs(mag);
    if (mag < 0.0) {
        exppower = 1.0 / exppower;
    }
    color.r = 1.0 - pow((1.0 - color.r), exppower);
    color.g = 1.0 - pow((1.0 - color.g), exppower);
    color.b = 1.0 - pow((1.0 - color.b), exppower);
}

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    // 锐化比较特殊，先进行锐化
    vec4 resultColor = colorSharpen(fTexCoord, textureColor, sharpen);

    // 调节亮度
    resultColor.rgb = clamp(resultColor.rgb + vec3(brightness, brightness, brightness), 0.0, 1.0);

    // 调节对比度
    resultColor.rgb = ((resultColor.rgb - 0.5) * max(contrast, 0.0)) + 0.5;
    resultColor = vec4(clamp(((resultColor.rgb - vec3(0.5)) * contrast + vec3(0.5)), 0.0, 1.0), resultColor.a);

    // 调节饱和度
    lowp float satLuminance = dot(resultColor.rgb, satLuminanceWeighting);
    lowp vec3 greyScaleColor = vec3(satLuminance);
    resultColor = vec4(clamp(mix(greyScaleColor, resultColor.rgb, saturation), 0.0, 1.0), resultColor.a);

    // 调节曝光
    colorExplosure(resultColor, exposure);

    // 调节高光、阴影
    colorHighlightsShadows(resultColor, highlights, shadows);

    // 调节色调、色温
    colorHueWarmth(resultColor, hue, temperature);

    // 暗角
    colorVignette(resultColor, vignette);

    // 预乘alpha
    resultColor.rgb *= textureColor.a;

    fragColor = resultColor;
}