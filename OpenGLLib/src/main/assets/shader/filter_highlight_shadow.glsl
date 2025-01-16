#version 300 es
precision mediump float; // 添加默认精度

in vec2 fTexCoord;
out vec4 fragColor;

uniform sampler2D uTexture;
uniform lowp float shadows;
uniform lowp float highlights;

const mediump vec3 hsLuminanceWeighting = vec3(0.3, 0.3, 0.3);

void main() {
    lowp vec4 result = texture(uTexture, fTexCoord);
    mediump float hsLuminance = dot(result.rgb, hsLuminanceWeighting);
    mediump float shadow = clamp((pow(hsLuminance, 1.0 / shadows) + (-0.76) * pow(hsLuminance, 2.0 / shadows)) - hsLuminance, 0.0, 1.0);
    mediump float highlight = clamp((1.0 - (pow(1.0 - hsLuminance, 1.0 / (2.0 - highlights)) + (-0.8) * pow(1.0 - hsLuminance, 2.0 / (2.0 - highlights)))) - hsLuminance, -1.0, 0.0);
    lowp vec3 hsresult = vec3(0.0, 0.0, 0.0) + ((hsLuminance + shadow + highlight) - 0.0) * ((result.rgb - vec3(0.0, 0.0, 0.0)) / (hsLuminance - 0.0));
    mediump float contrastedLuminance = ((hsLuminance - 0.5) * 1.5) + 0.5;
    mediump float whiteInterp = contrastedLuminance * contrastedLuminance * contrastedLuminance;
    mediump float whiteTarget = clamp(highlights, 1.0, 2.0) - 1.0;
    hsresult = mix(hsresult, vec3(1.0), whiteInterp * whiteTarget);
    mediump float invContrastedLuminance = 1.0 - contrastedLuminance;
    mediump float blackInterp = invContrastedLuminance * invContrastedLuminance * invContrastedLuminance;
    mediump float blackTarget = 1.0 - clamp(shadows, 0.0, 1.0);
    hsresult = mix(hsresult, vec3(0.0), blackInterp * blackTarget);
    fragColor = vec4(hsresult.rgb, result.a);
}