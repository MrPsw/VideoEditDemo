precision mediump float;
uniform sampler2D u_TextureUnit;
varying vec2 v_TextureCoordinates;

uniform float texelWidthOffset;
uniform float texelHeightOffset;

const highp vec3 W = vec3(0.2125, 0.7154, 0.0721);
void main()
{
    float cx = v_TextureCoordinates.x;
    float cy = v_TextureCoordinates.y;

    vec3 finalColor;
    if(cy < 0.5){
        finalColor = texture2D(u_TextureUnit, vec2(cx, cy * 2.0)).rgb;
    }else{
        finalColor = texture2D(u_TextureUnit, vec2(cx, (cy -0.5) * 2.0)).rgb;
        float luminance = dot(finalColor.rgb, W);

        finalColor = vec3(luminance);
    }

    gl_FragColor = vec4(finalColor, 1.0);
}