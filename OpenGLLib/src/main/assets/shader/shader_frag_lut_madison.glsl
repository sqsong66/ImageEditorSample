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
     vec4 textureColor = texture(uTexture, fTexCoord);
     vec3 texel = textureColor.rgb;
     vec2 tc = (2.0 * fTexCoord) - 1.0;
     float d = dot(tc, tc);
     vec2 lookup = vec2(d, texel.r);
     texel.r = texture(inputImageTexture2, lookup).r;
     lookup.y = texel.g;
     texel.g = texture(inputImageTexture2, lookup).g;
     lookup.y = texel.b;
     texel.b= texture(inputImageTexture2, lookup).b;
     
     vec2 red = vec2(texel.r, 0.16666);
     vec2 green = vec2(texel.g, 0.5);
     vec2 blue = vec2(texel.b, .83333);
     texel.r = texture(inputImageTexture1, red).r;
     texel.g = texture(inputImageTexture1, green).g;
     texel.b = texture(inputImageTexture1, blue).b;

     vec3 result = mix(textureColor.rgb, texel, intensity);
     result *= textureColor.a;
     fragColor = vec4(result, textureColor.a);
 }

