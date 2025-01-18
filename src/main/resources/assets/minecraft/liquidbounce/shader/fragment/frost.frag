#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform float radius;
uniform float alpha;
uniform float intensity;
uniform vec3 tintColor;

// Gaussian blur function
vec4 blur13(sampler2D image, vec2 uv, vec2 resolution, vec2 direction) {
    vec4 color = vec4(0.0);
    vec2 off1 = vec2(1.411764705882353) * direction;
    vec2 off2 = vec2(3.2941176470588234) * direction;
    vec2 off3 = vec2(5.176470588235294) * direction;
    
    color += texture2D(image, uv) * 0.1964825501511404;
    color += texture2D(image, uv + (off1 * texelSize)) * 0.2969069646728344;
    color += texture2D(image, uv - (off1 * texelSize)) * 0.2969069646728344;
    color += texture2D(image, uv + (off2 * texelSize)) * 0.09447039785044732;
    color += texture2D(image, uv - (off2 * texelSize)) * 0.09447039785044732;
    color += texture2D(image, uv + (off3 * texelSize)) * 0.010381362401148057;
    color += texture2D(image, uv - (off3 * texelSize)) * 0.010381362401148057;
    
    return color;
}

void main() {
    vec2 uv = gl_TexCoord[0].xy;
    
    // Apply two-pass Gaussian blur
    vec4 color = blur13(texture, uv, 1.0 / texelSize, vec2(1.0, 0.0));
    color = blur13(texture, uv, 1.0 / texelSize, vec2(0.0, 1.0));
    
    // Add frost effect
    float noise = fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453);
    vec4 frostColor = vec4(tintColor * (0.8 + 0.2 * noise), 1.0);
    
    // Mix blur and frost
    color = mix(color, frostColor, intensity * 0.7);
    
    // Add some sparkle
    float sparkle = pow(noise, 20.0) * 0.8;
    color.rgb += vec3(sparkle) * intensity;
    
    gl_FragColor = vec4(color.rgb, color.a * alpha);
} 