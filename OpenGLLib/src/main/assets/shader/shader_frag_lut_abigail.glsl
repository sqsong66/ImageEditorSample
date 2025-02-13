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
    texel = vec3(
        texture(inputImageTexture1, vec2(texel.r, .16666)).r,
        texture(inputImageTexture1, vec2(texel.g, .5)).g,
        texture(inputImageTexture1, vec2(texel.b, .83333)).b);
    vec3 result = mix(textureColor.rgb, texel, intensity);
    result *= textureColor.a;
    fragColor = vec4(result, textureColor.a);
}


