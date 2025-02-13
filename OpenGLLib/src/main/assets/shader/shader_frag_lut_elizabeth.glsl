#version 300 es
precision lowp float;

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform float intensity;
uniform sampler2D uTexture;
uniform sampler2D inputImageTexture1; //sutroMap;
uniform sampler2D inputImageTexture2; //sutroMetal;
uniform sampler2D inputImageTexture3; //softLight
uniform sampler2D inputImageTexture4; //sutroEdgeburn
uniform sampler2D inputImageTexture5; //sutroCurves

void main()
{
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec3 texel = textureColor.rgb;

    vec2 tc = (2.0 * fTexCoord) - 1.0;
    float d = dot(tc, tc);
    vec2 lookup = vec2(d, texel.r);
    texel.r = texture(inputImageTexture1, lookup).r;
    lookup.y = texel.g;
    texel.g = texture(inputImageTexture1, lookup).g;
    lookup.y = texel.b;
    texel.b = texture(inputImageTexture1, lookup).b;

    vec3 rgbPrime = vec3(0.1019, 0.0, 0.0);
    float m = dot(vec3(.3, .59, .11), texel.rgb) - 0.03058;
    texel = mix(texel, rgbPrime + m, 0.32);

    vec3 metal = texture(inputImageTexture2, fTexCoord).rgb;
    texel.r = texture(inputImageTexture3, vec2(metal.r, texel.r)).r;
    texel.g = texture(inputImageTexture3, vec2(metal.g, texel.g)).g;
    texel.b = texture(inputImageTexture3, vec2(metal.b, texel.b)).b;

    texel = texel * texture(inputImageTexture4, fTexCoord).rgb;

    texel.r = texture(inputImageTexture5, vec2(texel.r, .16666)).r;
    texel.g = texture(inputImageTexture5, vec2(texel.g, .5)).g;
    texel.b = texture(inputImageTexture5, vec2(texel.b, .83333)).b;

    vec3 result = mix(textureColor.rgb, texel, intensity);
    result *= textureColor.a;
    fragColor = vec4(result, textureColor.a);
}
