

precision mediump float;

//采样点的坐标
varying vec2 aCoord;

//采样器,因为SurfaceTexture的特殊性，所以需要用这个采样器
uniform sampler2D vTexture;

void main(){

   //  texture2D:采样器采集采样点的坐标，赋值给gl_FragColor这个内置变量
    gl_FragColor = texture2D(vTexture,aCoord);
}