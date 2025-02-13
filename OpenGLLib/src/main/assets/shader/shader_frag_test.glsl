attribute vec4 position;
attribute vec4 inputTextureCoordinate;
 
varying vec2 textureCoordinate;
 
void main()
{
    gl_Position = position;
    textureCoordinate = inputTextureCoordinate.xy;
}

precision highp float;
varying vec2 textureCoordinate;
uniform float imageWidthFactor;
uniform float imageHeightFactor;
uniform sampler2D inputImageTexture;
uniform float pixel;
void main()
{
  vec2 uv  = textureCoordinate.xy;
  float dx = pixel * imageWidthFactor;
  float dy = pixel * imageHeightFactor;
  vec2 coord = vec2(dx * floor(uv.x / dx), dy * floor(uv.y / dy));
  vec3 tc = texture2D(inputImageTexture, coord).xyz;
  gl_FragColor = vec4(tc, 1.0);
}