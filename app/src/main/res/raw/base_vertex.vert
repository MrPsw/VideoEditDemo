// 顶点着色器

//顶点位置，需要传入4个点
attribute vec4 vPosition;

//接受纹理坐标，接受采样器采样图片的坐标
attribute vec4 vCoord;

//变换矩阵
uniform mat4 vMatrix;

//传给片元着色器 像素点
varying vec2 aCoord;

void main(){
   gl_Position = vPosition;
   aCoord = vCoord.xy;
}