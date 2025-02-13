#version 300 es
precision lowp float;

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform float intensity;
uniform sampler2D uTexture;
uniform sampler2D inputImageTexture1;  //edgeBurn
uniform sampler2D inputImageTexture2;  //hefeMap
uniform sampler2D inputImageTexture3;  //hefeGradientMap
uniform sampler2D inputImageTexture4;  //hefeSoftLight
uniform sampler2D inputImageTexture5;  //hefeMetal

void main() {
    vec4 textureColor = texture(uTexture, fTexCoord);
    vec3 texel = textureColor.rgb;
    vec3 edge = texture(inputImageTexture1, fTexCoord).rgb;
    texel = texel * edge;
    texel = vec3(
        texture(inputImageTexture2, vec2(texel.r, .16666)).r,
        texture(inputImageTexture2, vec2(texel.g, .5)).g,
        texture(inputImageTexture2, vec2(texel.b, .83333)).b);
    vec3 luma = vec3(.30, .59, .11);
    vec3 gradSample = texture(inputImageTexture3, vec2(dot(luma, texel), .5)).rgb;
    vec3 final = vec3(
        texture(inputImageTexture4, vec2(gradSample.r, texel.r)).r,
        texture(inputImageTexture4, vec2(gradSample.g, texel.g)).g,
        texture(inputImageTexture4, vec2(gradSample.b, texel.b)).b
    );
    vec3 metal = texture(inputImageTexture5, fTexCoord).rgb;
    vec3 metaled = vec3(
        texture(inputImageTexture4, vec2(metal.r, texel.r)).r,
        texture(inputImageTexture4, vec2(metal.g, texel.g)).g,
        texture(inputImageTexture4, vec2(metal.b, texel.b)).b
    );

    vec3 result = mix(textureColor.rgb, metaled, intensity);
    result.rgb *= textureColor.a;
    fragColor = vec4(result, textureColor.a);
}

