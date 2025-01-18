#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform float radius;
uniform float alpha;
uniform float intensity;
uniform vec3 tintColor;

// Improved Gaussian weights for better blur quality
const float weights[5] = float[5](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

// Improved noise function for better frost pattern
float noise(vec2 p) {
    vec2 ip = floor(p);
    vec2 u = fract(p);
    u = u * u * (3.0 - 2.0 * u);
    
    float res = mix(
        mix(hash(ip), hash(ip + vec2(1.0, 0.0)), u.x),
        mix(hash(ip + vec2(0.0, 1.0)), hash(ip + vec2(1.0, 1.0)), u.x), u.y);
    return res * res;
}

float hash(vec2 p) {
    float h = dot(p, vec2(127.1, 311.7));
    return fract(sin(h) * 43758.5453123);
}

// Improved blur function with better sampling
vec4 blur(sampler2D image, vec2 uv, vec2 direction) {
    vec4 color = texture2D(image, uv) * weights[0];
    
    for(int i = 1; i < 5; i++) {
        vec2 offset = direction * texelSize * float(i) * radius;
        color += texture2D(image, uv + offset) * weights[i];
        color += texture2D(image, uv - offset) * weights[i];
    }
    
    return color;
}

// Frost distortion function
vec2 frostDistort(vec2 uv) {
    float t = gl_FragCoord.x * gl_FragCoord.y;
    vec2 offset = vec2(
        noise(uv * 8.0 + t * 0.05),
        noise(uv * 8.0 + t * 0.05 + 3.14)
    );
    return uv + (offset - 0.5) * 0.015 * intensity;
}

void main() {
    vec2 uv = gl_TexCoord[0].xy;
    
    // Apply frost distortion
    vec2 distortedUV = frostDistort(uv);
    
    // Two-pass gaussian blur with distortion
    vec4 color = blur(texture, distortedUV, vec2(1.0, 0.0));
    color = blur(texture, distortedUV, vec2(0.0, 1.0));
    
    // Create frost pattern
    float frost = noise(uv * 10.0) * noise(uv * 15.0);
    vec4 frostColor = vec4(tintColor * (0.85 + 0.15 * frost), 1.0);
    
    // Layer effects
    color = mix(color, frostColor, intensity * 0.6);
    
    // Add crystalline highlights
    float highlight = pow(frost, 4.0) * 0.6;
    color.rgb += vec3(highlight) * intensity;
    
    // Add subtle edge frost
    float edge = 1.0 - smoothstep(0.0, 0.4, frost);
    color.rgb += tintColor * edge * intensity * 0.3;
    
    gl_FragColor = vec4(color.rgb, color.a * alpha);
} 