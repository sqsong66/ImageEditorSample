#version 300 es
precision lowp float;

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform float intensity;
uniform sampler2D uTexture;
uniform sampler2D inputImageTexture1;
uniform sampler2D inputImageTexture2;
uniform sampler2D inputImageTexture3;

void main()
{
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec3 texel = textureColor.rgb;
    vec3 bbTexel = texture(inputImageTexture1, fTexCoord).rgb;

    texel.r = texture(inputImageTexture2, vec2(bbTexel.r, texel.r)).r;
    texel.g = texture(inputImageTexture2, vec2(bbTexel.g, texel.g)).g;
    texel.b = texture(inputImageTexture2, vec2(bbTexel.b, texel.b)).b;

    vec3 mapped;
    mapped.r = texture(inputImageTexture3, vec2(texel.r, .16666)).r;
    mapped.g = texture(inputImageTexture3, vec2(texel.g, .5)).g;
    mapped.b = texture(inputImageTexture3, vec2(texel.b, .83333)).b;

    vec3 result = mix(textureColor.rgb, mapped, intensity);
    result *= textureColor.a;
    fragColor = vec4(result, textureColor.a);
}
