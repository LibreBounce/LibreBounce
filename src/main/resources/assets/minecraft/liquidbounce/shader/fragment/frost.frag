#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform float radius;
uniform float alpha;
uniform float intensity;
uniform vec3 tintColor;
uniform float noiseScale;

float random(vec2 coords) {
    return fract(sin(dot(coords.xy, vec2(12.9898,78.233))) * 43758.5453);
}

void main() {
    vec4 color = vec4(0.0);
    float count = 0.0;
    
    // Sample pixels in a radius for the blur effect
    for(float x = -radius; x <= radius; x++) {
        for(float y = -radius; y <= radius; y++) {
            vec2 offset = vec2(x, y) * texelSize;
            color += texture2D(texture, gl_TexCoord[0].xy + offset);
            count += 1.0;
        }
    }
    
    // Average the sampled colors
    color = color / count;
    
    // Add noise pattern
    vec2 noiseCoord = gl_TexCoord[0].xy * noiseScale;
    float noise = random(noiseCoord) * 0.2 - 0.1;
    
    // Mix with tint color and add noise
    vec3 tinted = mix(color.rgb, tintColor, intensity);
    tinted += noise;
    
    gl_FragColor = vec4(tinted, color.a * alpha);
} 