#version 120

uniform sampler2D texture;
uniform vec2 texelSize;
uniform float radius;
uniform float alpha;
uniform float intensity;
uniform vec4 tintColor;

void main() {
    vec4 centerCol = texture2D(texture, gl_TexCoord[0].xy);
    
    // Apply frost effect
    vec4 color = vec4(0.0);
    float totalWeight = 0.0;
    
    for (float x = -radius; x <= radius; x++) {
        for (float y = -radius; y <= radius; y++) {
            vec2 offset = vec2(x, y) * texelSize;
            vec4 sampleCol = texture2D(texture, gl_TexCoord[0].xy + offset);
            float weight = 1.0 - length(offset) / radius;
            
            if (weight < 0.0) continue;
            
            weight = pow(weight, 2.0);
            color += sampleCol * weight;
            totalWeight += weight;
        }
    }
    
    color /= totalWeight;
    
    // Mix with tint color
    vec4 frostColor = mix(color, tintColor * color, intensity);
    
    gl_FragColor = vec4(frostColor.rgb, frostColor.a * alpha);
} 