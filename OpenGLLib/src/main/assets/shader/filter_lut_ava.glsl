#version 300 es
precision lowp float;

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform float intensity;
uniform sampler2D uTexture;
uniform sampler2D inputImageTexture1;  //processImage
uniform sampler2D inputImageTexture2;  //blowout
uniform sampler2D inputImageTexture3;  //contrast
uniform sampler2D inputImageTexture4;  //luma
uniform sampler2D inputImageTexture5;  //screen

mat3 saturateMatrix = mat3(
    1.105150,
    -0.044850,
    -0.046000,
    -0.088050,
    1.061950,
    -0.089200,
    -0.017100,
    -0.017100,
    1.132900);

vec3 luma = vec3(.3, .59, .11);

void main()
{
    vec4 originalColor = texture(uTexture, fTexCoord);
    vec3 texel = originalColor.rgb;

    vec2 lookup;
    lookup.y = 0.5;
    lookup.x = texel.r;
    texel.r = texture(inputImageTexture1, lookup).r;
    lookup.x = texel.g;
    texel.g = texture(inputImageTexture1, lookup).g;
    lookup.x = texel.b;
    texel.b = texture(inputImageTexture1, lookup).b;

    texel = saturateMatrix * texel;


    vec2 tc = (2.0 * fTexCoord) - 1.0;
    float d = dot(tc, tc);
    vec3 sampled;
    lookup.y = 0.5;
    lookup.x = texel.r;
    sampled.r = texture(inputImageTexture2, lookup).r;
    lookup.x = texel.g;
    sampled.g = texture(inputImageTexture2, lookup).g;
    lookup.x = texel.b;
    sampled.b = texture(inputImageTexture2, lookup).b;
    float value = smoothstep(0.0, 1.0, d);
    texel = mix(sampled, texel, value);

    lookup.x = texel.r;
    texel.r = texture(inputImageTexture3, lookup).r;
    lookup.x = texel.g;
    texel.g = texture(inputImageTexture3, lookup).g;
    lookup.x = texel.b;
    texel.b = texture(inputImageTexture3, lookup).b;


    lookup.x = dot(texel, luma);
    texel = mix(texture(inputImageTexture4, lookup).rgb, texel, .5);

    lookup.x = texel.r;
    texel.r = texture(inputImageTexture5, lookup).r;
    lookup.x = texel.g;
    texel.g = texture(inputImageTexture5, lookup).g;
    lookup.x = texel.b;
    texel.b = texture(inputImageTexture5, lookup).b;

    vec3 result = mix(originalColor.rgb, texel, intensity);
    result.rgb *= originalColor.a;
    fragColor = vec4(result, originalColor.a);
}
