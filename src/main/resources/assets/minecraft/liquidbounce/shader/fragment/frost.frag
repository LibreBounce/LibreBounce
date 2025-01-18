#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform float radius;
uniform float alpha;
uniform float intensity;
uniform vec3 tintColor;

// Improved Gaussian weights for better blur quality
const float weights[5] = float[5](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

float hash(vec2 p) {
    float h = dot(p, vec2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

vec4 blur13(sampler2D image, vec2 uv, vec2 direction) {
    vec4 color = texture2D(image, uv) * weights[0];
    
    for(int i = 1; i < 5; i++) {
        vec2 offset = direction * texelSize * float(i) * radius;
        color += texture2D(image, uv + offset) * weights[i];
        color += texture2D(image, uv - offset) * weights[i];
    }
    
    return color;
}

void main() {
    vec2 uv = gl_TexCoord[0].xy;
    
    // Apply two-pass gaussian blur
    vec4 color = blur13(texture, uv, vec2(1.0, 0.0));
    color = blur13(texture, uv, vec2(0.0, 1.0));
    
    // Create frost pattern
    float frost = noise(uv * 10.0) * noise(uv * 15.0);
    vec4 frostColor = vec4(tintColor * (0.85 + 0.15 * frost), 1.0);
    
    // Mix blur and frost
    color = mix(color, frostColor, intensity * 0.6);
    
    // Add crystalline highlights
    float highlight = pow(frost, 4.0) * 0.6;
    color.rgb += vec3(highlight) * intensity;
    
    gl_FragColor = vec4(color.rgb, color.a * alpha);
} 