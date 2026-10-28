#version 150

// A square over the whole screen; each pixel finds its own ray.

in vec3 Position;

out vec2 screen;

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    screen = Position.xy;
}
