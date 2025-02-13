#version 300 es
precision lowp float;

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform float intensity;
uniform sampler2D uTexture;
uniform sampler2D inputImageTexture1;

void main()
{
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec3 texel = textureColor.rgb;
    vec2 lookup;
    lookup.y = .5;
    lookup.x = texel.r;
    texel.r = texture(inputImageTexture1, lookup).r;
    lookup.x = texel.g;
    texel.g = texture(inputImageTexture1, lookup).g;
    lookup.x = texel.b;
    texel.b = texture(inputImageTexture1, lookup).b;

    vec3 result = mix(textureColor.rgb, texel, intensity);
    result *= textureColor.a;
    fragColor = vec4(result, textureColor.a);
}

