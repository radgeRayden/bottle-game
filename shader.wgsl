struct VertexAttributes {
   position: vec2<f32>,
   texcoords: vec2<f32>,
   color: vec4<f32>,
};

struct Uniforms {
    mvp: mat4x4<f32>,
};

@group(0)
@binding(0)
var<storage, read> vertexData: array<VertexAttributes>;

@group(0)
@binding(1)
var s : sampler;

@group(0)
@binding(2)
var t : texture_2d<f32>;

@group(1)
@binding(0)
var<uniform> uniforms: Uniforms;

struct VertexOutput {
    @location(0) vcolor: vec4<f32>,
    @location(1) texcoords: vec2<f32>,
    @builtin(position) position: vec4<f32>,
};

@vertex
fn vs_main(@builtin(vertex_index) vindex: u32) -> VertexOutput {
    var out: VertexOutput;
    let attr = vertexData[vindex];
    out.position = uniforms.mvp * vec4<f32>(attr.position, 0.0, 1.0);
    out.texcoords = attr.texcoords;
    out.vcolor = attr.color;
    return out;
}

@fragment
fn fs_main(vertex: VertexOutput) -> @location(0) vec4<f32> {
    return textureSample(t, s, vertex.texcoords) * vertex.vcolor;
}
