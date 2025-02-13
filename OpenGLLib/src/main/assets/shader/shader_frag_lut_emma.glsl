#version 300 es
precision lowp float;

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform float intensity;
uniform sampler2D uTexture; // Original texture
uniform sampler2D inputImageTexture1; // Lookup texture

void main()
{
    vec4 color = texture(uTexture, fTexCoord);
    vec3 originalTexel = color.rgb;

    vec3 processedTexel = vec3(
        texture(inputImageTexture1, vec2(originalTexel.r, .16666)).r,
        texture(inputImageTexture1, vec2(originalTexel.g, .5)).g,
        texture(inputImageTexture1, vec2(originalTexel.b, .83333)).b);

    vec3 finalTexel = mix(originalTexel, processedTexel, intensity);
    finalTexel.rgb *= color.a;
    fragColor = vec4(finalTexel, color.a);
}
