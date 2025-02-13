#version 300 es
precision mediump float; // 添加默认精度

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform sampler2D inputImageTexture1;
uniform lowp float intensity;

void main() {
    highp vec4 textureColor = texture(uTexture, fTexCoord);
    highp float blueColor = textureColor.b * 63.0;

    highp vec2 quad1;
    quad1.y = floor(floor(blueColor) / 8.0);
    quad1.x = floor(blueColor) - (quad1.y * 8.0);

    highp vec2 quad2;
    quad2.y = floor(ceil(blueColor) / 8.0);
    quad2.x = ceil(blueColor) - (quad2.y * 8.0);

    highp vec2 texPos1;
    texPos1.x = (quad1.x * 0.125) + 0.5 / 512.0 + ((0.125 - 1.0 / 512.0) * textureColor.r);
    texPos1.y = (quad1.y * 0.125) + 0.5 / 512.0 + ((0.125 - 1.0 / 512.0) * textureColor.g);

    highp vec2 texPos2;
    texPos2.x = (quad2.x * 0.125) + 0.5 / 512.0 + ((0.125 - 1.0 / 512.0) * textureColor.r);
    texPos2.y = (quad2.y * 0.125) + 0.5 / 512.0 + ((0.125 - 1.0 / 512.0) * textureColor.g);

    lowp vec4 newColor1 = texture(inputImageTexture1, texPos1);
    lowp vec4 newColor2 = texture(inputImageTexture1, texPos2);
    lowp vec4 newColor = mix(newColor1, newColor2, fract(blueColor));
    // fragColor = mix(textureColor, vec4(newColor.rgb, textureColor.w), intensity);
    vec3 result = mix(textureColor.rgb, newColor.rgb, intensity);
    // 对输出颜色的rgb需要进行预乘alpha处理，否则PNG图片会出现边缘异常问题
    result.rgb *= textureColor.a;
    fragColor = vec4(result, textureColor.a);
}