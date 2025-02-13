#version 300 es
precision lowp float;

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform float intensity;
uniform sampler2D uTexture;
uniform sampler2D inputImageTexture1; //map
uniform sampler2D inputImageTexture2; //gradMap

mat3 saturateMatrix = mat3(
    1.1402,
    -0.0598,
    -0.061,
    -0.1174,
    1.0826,
    -0.1186,
    -0.0228,
    -0.0228,
    1.1772);

vec3 lumaCoeffs = vec3(.3, .59, .11);

void main()
{
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec3 texel = textureColor.rgb;

    texel = vec3(
        texture(inputImageTexture1, vec2(texel.r, .1666666)).r,
        texture(inputImageTexture1, vec2(texel.g, .5)).g,
        texture(inputImageTexture1, vec2(texel.b, .8333333)).b
    );

    texel = saturateMatrix * texel;
    float luma = dot(lumaCoeffs, texel);
    texel = vec3(
        texture(inputImageTexture2, vec2(luma, texel.r)).r,
        texture(inputImageTexture2, vec2(luma, texel.g)).g,
        texture(inputImageTexture2, vec2(luma, texel.b)).b);

    vec3 result = mix(textureColor.rgb, texel, intensity);
    result.rgb *= textureColor.a;
    fragColor = vec4(result, textureColor.a);
}


