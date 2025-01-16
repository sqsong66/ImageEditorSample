#version 300 es
precision lowp float;

in highp vec2 fTexCoord;
out vec4 fragColor;

uniform float intensity;
uniform sampler2D uTexture;
uniform sampler2D inputImageTexture1; //toasterMetal
uniform sampler2D inputImageTexture2; //toasterSoftlight
uniform sampler2D inputImageTexture3; //toasterCurves
uniform sampler2D inputImageTexture4; //toasterOverlayMapWarm
uniform sampler2D inputImageTexture5; //toasterColorshift

void main()
{
    lowp vec3 texel;
    mediump vec2 lookup;
    vec2 blue;
    vec2 green;
    vec2 red;
    vec4 color = texture(uTexture, fTexCoord);
    if (color.a > 0.0) {
        color.rgb /= color.a;
    }
    lowp vec4 originalColor = color; //texture(uTexture, fTexCoord);
    texel = originalColor.xyz;
    lowp vec4 tmpvar_2;
    tmpvar_2 = texture(inputImageTexture1, fTexCoord);
    lowp vec2 tmpvar_3;
    tmpvar_3.x = tmpvar_2.x;
    tmpvar_3.y = originalColor.x;
    texel.x = texture(inputImageTexture2, tmpvar_3).x;
    lowp vec2 tmpvar_4;
    tmpvar_4.x = tmpvar_2.y;
    tmpvar_4.y = originalColor.y;
    texel.y = texture(inputImageTexture2, tmpvar_4).y;
    lowp vec2 tmpvar_5;
    tmpvar_5.x = tmpvar_2.z;
    tmpvar_5.y = originalColor.z;
    texel.z = texture(inputImageTexture2, tmpvar_5).z;
    red.x = texel.x;
    red.y = 0.16666;
    green.x = texel.y;
    green.y = 0.5;
    blue.x = texel.z;
    blue.y = 0.833333;
    texel.x = texture(inputImageTexture3, red).x;
    texel.y = texture(inputImageTexture3, green).y;
    texel.z = texture(inputImageTexture3, blue).z;
    mediump vec2 tmpvar_6;
    tmpvar_6 = ((2.0 * fTexCoord) - 1.0);
    mediump vec2 tmpvar_7;
    tmpvar_7.x = dot(tmpvar_6, tmpvar_6);
    tmpvar_7.y = texel.x;
    lookup = tmpvar_7;
    texel.x = texture(inputImageTexture4, tmpvar_7).x;
    lookup.y = texel.y;
    texel.y = texture(inputImageTexture4, lookup).y;
    lookup.y = texel.z;
    texel.z = texture(inputImageTexture4, lookup).z;
    red.x = texel.x;
    green.x = texel.y;
    blue.x = texel.z;
    texel.x = texture(inputImageTexture5, red).x;
    texel.y = texture(inputImageTexture5, green).y;
    texel.z = texture(inputImageTexture5, blue).z;
    lowp vec3 filteredColor = texel;
    lowp vec4 tmpvar_8;
    tmpvar_8.w = originalColor.w;
    tmpvar_8.xyz = mix(originalColor.xyz, filteredColor, intensity);
    tmpvar_8.xyz *= tmpvar_8.w;
    fragColor = tmpvar_8;
}