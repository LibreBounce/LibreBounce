/*
 * Modified from:
 * https://vitaliburkov.wordpress.com/2016/09/17/simple-and-fast-high-quality-antialiased-lines-with-opengl/
 */
#version 120

uniform float lineWidth;
uniform vec4 color;
uniform float blendFactor; // [1.5, 2.5]

varying vec2 lineCenter;

void main(void) {
    float distance = length(lineCenter - gl_FragCoord.xy);
    float width = lineWidth;

    if (distance > width) {
        // if the distance exceeds the line width, discard the fragment
        discard;
        //gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0); // TODO remove debug color
    } else {
        // apply the blend factor
        float alpha = color.w * pow((width - distance) / width, blendFactor); // TODO pow is slow... maybe use exp() or smoothstep()
        gl_FragColor = vec4(color.x, color.y, color.z, alpha);
    }
}
