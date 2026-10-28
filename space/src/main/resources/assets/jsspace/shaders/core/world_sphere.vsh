#version 150

// A square facing the eye that covers a world's sphere and the glow of its air: its corners sit round the camera, in
// the world's directions, and the fragment shader finds the sphere along each pixel's ray.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 worldPos;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    worldPos = Position;
}
