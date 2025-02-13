#version 300 es
precision lowp float;

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform float intensity;
uniform sampler2D uTexture;
uniform sampler2D inputImageTexture1; //map
uniform sampler2D inputImageTexture2; //vigMap

void main()
{
    vec4 originalTexel = texture(uTexture, fTexCoord).rgba;
    vec3 texel = originalTexel.rgb;

    texel = vec3(
        texture(inputImageTexture1, vec2(texel.r, .16666)).r,
        texture(inputImageTexture1, vec2(texel.g, .5)).g,
        texture(inputImageTexture1, vec2(texel.b, .83333)).b);

    vec2 tc = (2.0 * fTexCoord) - 1.0;
    float d = dot(tc, tc);
    vec2 lookup = vec2(d, texel.r);
    texel.r = texture(inputImageTexture2, lookup).r;
    lookup.y = texel.g;
    texel.g = texture(inputImageTexture2, lookup).g;
    lookup.y = texel.b;
    texel.b = texture(inputImageTexture2, lookup).b;

    // Use mix to blend the original color and the filtered color based on the intensity
    texel = mix(originalTexel.rgb, texel, intensity);
    texel.rgb *= originalTexel.a;
    fragColor = vec4(texel, originalTexel.a);
}
