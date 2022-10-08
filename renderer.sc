using import struct
using import glm
using import Array
using import Option
using import String

import bottle
import wgpu
using bottle.gpu.types
import bottle.src.gpu.common
from (import bottle.src.gpu.binding-interface) let GPUResourceBinding
from (import bottle.src.gpu.bindgroup) let GPUBindGroup
using import bottle.src.helpers

import .math
using import .helpers

struct Quad plain
    start : vec2
    extent : vec2

struct Vertex plain
    position : vec2
    texcoords : vec2
    color : vec4

struct Uniforms
    mvp : mat4

struct DrawState
    pipeline : GPUPipeline
    uniform-buffer : (GPUUniformBuffer Uniforms)
    bgroup1 : GPUBindGroup

global draw-state : (Option DrawState)

type StreamingMesh < Struct
    inline __typecall (cls vertexT indexT)
        struct (.. "StreamingMesh<" (tostring vertexT) ", " (tostring indexT) ">") < this-type
            let storage-bufferT = (GPUStorageBuffer vertexT)
            let index-bufferT = (GPUIndexBuffer indexT)

            vertex-buffer : storage-bufferT
            index-buffer : index-bufferT

            vertex-data : (Array vertexT)
            index-data : (Array indexT)

            bgroup0 : GPUBindGroup
            texture-view : GPUResourceBinding

            dirty? : bool

            fn make-bindgroup (vbuffer texture-view)
                let istate = bottle.src.gpu.common.istate
                let dummies = bottle.src.gpu.common.istate.dummy-resources
                let cache = bottle.src.gpu.common.istate.cached-layouts

                let layout =
                    try
                        'get cache.bind-group-layouts S"StreamingMesh"
                    else
                        assert false
                        unreachable;

                let bgroup0 =
                    GPUBindGroup layout
                        GPUResourceBinding.Buffer
                            buffer = vbuffer._handle
                            offset = 0
                            size = vbuffer._size
                        GPUResourceBinding.Sampler
                            wgpu.DeviceCreateSampler istate.device
                                &local wgpu.SamplerDescriptor
                                    label = "Bottle Sampler"
                                    addressModeU = wgpu.AddressMode.Repeat
                                    addressModeV = wgpu.AddressMode.Repeat
                                    addressModeW = wgpu.AddressMode.Repeat
                                    magFilter = wgpu.FilterMode.Nearest
                                    minFilter = wgpu.FilterMode.Nearest
                                    mipmapFilter = wgpu.MipmapFilterMode.Linear
                        texture-view

            fn draw (self rp)
                if self.dirty?
                    'update-buffers self

                let state = ('force-unwrap draw-state)

                'set-pipeline rp state.pipeline
                'set-bindgroup rp 0:u32 self.bgroup0
                'set-bindgroup rp 1:u32 state.bgroup1
                'set-index-buffer rp self.index-buffer

                index-count := (countof self.index-data)

                'draw-indexed rp (index-count as u32) 1:u32 0:u32 0:u32

            fn update-buffers (self)
                let data-size = ((countof self.vertex-data) * (sizeof vertexT))
                if (data-size > self.vertex-buffer._size)
                    let new-element-count = ((self.vertex-buffer._size * 2) // (sizeof vertexT))
                    let vbuffer = (storage-bufferT new-element-count)
                    self.vertex-buffer = vbuffer

                if (((countof self.index-data) * (sizeof indexT)) > self.index-buffer._size)
                    let new-index-count = ((self.index-buffer._size * 2) // (sizeof indexT))
                    let ibuffer = (index-bufferT new-index-count)
                    self.index-buffer = ibuffer

                'write self.vertex-buffer self.vertex-data
                'write self.index-buffer self.index-data
                self.bgroup0 = (make-bindgroup self.vertex-buffer self.texture-view)

                self.dirty? = false

            fn clear (self)
                'clear self.vertex-data
                'clear self.index-data
                ;

            inline __typecall (cls max-vertices max-indices texture)
                let vbuffer ibuffer = (storage-bufferT max-vertices) (index-bufferT max-indices)

                let dummies = bottle.src.gpu.common.istate.dummy-resources
                let texture-view =
                    static-if (not (none? texture))
                        GPUResourceBinding.TextureView texture._view
                    else
                        copy dummies.texture-view

                let bgroup0 = (make-bindgroup vbuffer (view texture-view))

                Struct.__typecall cls
                    vertex-buffer = vbuffer
                    index-buffer = ibuffer
                    bgroup0 = bgroup0
                    texture-view = texture-view

            unlet make-bindgroup

let MeshT = (StreamingMesh Vertex u16)

struct SpriteBatch
    mesh : MeshT

    inline __typecall (cls texture)
        let max-sprites = 1024

        super-type.__typecall cls
            mesh = (MeshT (4 * max-sprites) (6 * max-sprites) texture)

    fn clear (self)
        'clear self.mesh

    fn... add-sprite (self : this-type, position, size, quad = (Quad (vec2 0 0) (vec2 1 1)))
        self.mesh.dirty? = true

        local norm-vertices =
            # 0 - 3
            # | \ |
            # 1 - 2
            arrayof vec2
                vec2 0 1 # top left
                vec2 0 0 # bottom left
                vec2 1 0 # bottom right
                vec2 1 1 # top right

        local texcoords =
            arrayof vec2
                vec2 0 0 # top left
                vec2 0 1 # bottom left
                vec2 1 1 # bottom right
                vec2 1 0 # top right

        inline make-vertex (i)
            Vertex
                position = (position + ((norm-vertices @ i) * size))
                texcoords = (quad.start + ((texcoords @ i) * quad.extent))
                color = (vec4 1)

        let idx-offset = ((countof self.mesh.vertex-data) as u16)
        'append self.mesh.vertex-data
            make-vertex 0
        'append self.mesh.vertex-data
            make-vertex 1
        'append self.mesh.vertex-data
            make-vertex 2
        'append self.mesh.vertex-data
            make-vertex 3

        'append self.mesh.index-data (0:u16 + idx-offset)
        'append self.mesh.index-data (1:u16 + idx-offset)
        'append self.mesh.index-data (2:u16 + idx-offset)
        'append self.mesh.index-data (2:u16 + idx-offset)
        'append self.mesh.index-data (3:u16 + idx-offset)
        'append self.mesh.index-data (0:u16 + idx-offset)
        ;

    fn draw (self rp)
        'draw self.mesh rp

struct PrimitiveBatch
    mesh : MeshT

    inline __typecall (cls)
        super-type.__typecall cls
            mesh = (MeshT 4096 8192)

    fn clear (self)
        'clear self.mesh

    fn... add-rectangle (self : this-type, position : vec2, size : vec2)
        self.mesh.dirty? = true

        local norm-vertices =
            # 0 - 3
            # | \ |
            # 1 - 2
            arrayof vec2
                vec2 0 1 # top left
                vec2 0 0 # bottom left
                vec2 1 0 # bottom right
                vec2 1 1 # top right

        local texcoords =
            arrayof vec2
                vec2 0 0 # top left
                vec2 0 1 # bottom left
                vec2 1 1 # bottom right
                vec2 1 0 # top right

        inline make-vertex (i)
            Vertex
                position = (position + ((norm-vertices @ i) * size))
                texcoords = (vec2)
                color = (vec4 1)

        let idx-offset = ((countof self.mesh.vertex-data) as u16)
        'append self.mesh.vertex-data
            make-vertex 0
        'append self.mesh.vertex-data
            make-vertex 1
        'append self.mesh.vertex-data
            make-vertex 2
        'append self.mesh.vertex-data
            make-vertex 3

        'append self.mesh.index-data (0:u16 + idx-offset)
        'append self.mesh.index-data (1:u16 + idx-offset)
        'append self.mesh.index-data (2:u16 + idx-offset)
        'append self.mesh.index-data (2:u16 + idx-offset)
        'append self.mesh.index-data (3:u16 + idx-offset)
        'append self.mesh.index-data (0:u16 + idx-offset)
        ;

    fn draw (self rp)
        'draw self.mesh rp

fn init ()
    let dummies = bottle.src.gpu.common.istate.dummy-resources
    let cache = bottle.src.gpu.common.istate.cached-layouts

    let shader = (read-whole-file "shader.wgsl")
    let ubuffer = ((GPUUniformBuffer Uniforms) 1)
    let bg1-layout =
        try
            'get cache.bind-group-layouts S"Uniforms"
        else
            error "you made a typo you dofus"

    draw-state =
        DrawState
            pipeline = (GPUPipeline "Basic" (GPUShaderModule shader 'wgsl))
            bgroup1 =
                GPUBindGroup bg1-layout
                    GPUResourceBinding.UniformBuffer
                        buffer = ubuffer._handle
                        offset = 0
                        size = ubuffer._size
            uniform-buffer = ubuffer
    ;

fn set-camera-position (position)
    let draw-state = ('force-unwrap draw-state)

    let ww wh = (bottle.window.get-drawable-size)
    let mvp =
        *
            math.ortho ww wh
            math.translate (vec3 position 0)

    local uniforms : (Array Uniforms)
    'append uniforms (Uniforms mvp)

    'write draw-state.uniform-buffer uniforms

do
    let SpriteBatch PrimitiveBatch Quad init set-camera-position
    locals;
