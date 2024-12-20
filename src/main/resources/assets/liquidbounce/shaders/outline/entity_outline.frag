/**
 * Author: ccetl
 * Created: 2024
 * License: GPL-3.0
 */
#version 410 core

in vec2 fragTexCoord;
out vec4 fragColor;

uniform sampler2D texture0;

uniform int radius;

void main() {
    vec2 uv = fragTexCoord.xy;

    vec4 color = textureLod(texture0, uv, 0.0);
    if (color.a != 0.0) {
        // inside of the entity
        discard;
    }

    if (glowColor.a == 0.0) {
        discard;
    }

    vec2 texelSize = vec2(1.0) / textureSize(texture0, 0).xy;
    vec3 outColor = vec3(0.0);
    float iterations = 0.0;

    for (int ix = -radius; ix <= radius; ix++) {
        for (int iy = -radius; iy <= radius; iy++) {
            if (ix == 0 && iy == 0) {
                continue;
            }

            float x = float(ix);
            float y = float(iy);

            vec2 offset = vec2(texelSize.x * x, texelSize.y * y);
            vec3 positionColor = textureLod(texture0, uv + offset, 0.0).rgb;

            outColor += positionColor;
            iterations++;
        }
    }

    if (color.a == 0.0) {
        discard;
    }

    fragColor = vec4(outColor.rgb / iterations, 1.0);
}
