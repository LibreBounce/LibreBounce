/*
 * Modified from:
 * https://vitaliburkov.wordpress.com/2016/09/17/simple-and-fast-high-quality-antialiased-lines-with-opengl/
 */
#version 120

uniform mat4 modelViewMat;
uniform mat4 projMat;
uniform vec2 viewPort;

varying vec2 lineCenter;

const float VIEW_SHRINK = 1.0 - (1.0 / 256.0);
const mat4 VIEW_SCALE = mat4(
VIEW_SHRINK, 0.0, 0.0, 0.0,
0.0, VIEW_SHRINK, 0.0, 0.0,
0.0, 0.0, VIEW_SHRINK, 0.0,
0.0, 0.0, 0.0, 1.0
);

void main(void) {
    vec4 clipPosition = projMat * VIEW_SCALE * modelViewMat * vec4(gl_Vertex.xyz, 1.0);
    gl_Position = clipPosition;

    vec3 ndcPosition = clipPosition.xyz / clipPosition.w;
    vec2 screenPosition = 0.5 * (ndcPosition.xy + vec2(1.0)) * viewPort;
    lineCenter = screenPosition;
}
