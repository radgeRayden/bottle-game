using import Array
using import glm
using import Option
using import struct

import bottle
import .renderer

from renderer let SpriteBatch PrimitiveBatch Quad

USE_DT_ACCUMULATOR := true

struct InputState plain
    Left  : bool
    Right : bool
    Up    : bool
    Down  : bool

struct AABB plain
    position : vec2
    half-size : vec2

struct GameState
    player = (AABB (vec2 -200 250) (vec2 20))
    projection : AABB
    world : (Array AABB)

    collision? : bool
    show-debug? : bool

    geometry : PrimitiveBatch

global input-state : InputState
global gamestate : (Option GameState)

inline update-input (key value)
    from bottle.enums let Key

    switch key
    case Key.Left
        input-state.Left = value
    case Key.Right
        input-state.Right = value
    case Key.Up
        input-state.Up = value
    case Key.Down
        input-state.Down = value
    default
        unreachable;

fn input-down (key)
    update-input key true

fn input-up (key)
    update-input key false

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "AABB collision test"

fn restart

@@ 'on bottle.key-pressed
fn (key)
    gamestate := ('force-unwrap gamestate)

    from bottle.enums let Key
    inline kbind (binding action ...)
        if (key == (getattr Key binding))
            action ...

    kbind 'Escape bottle.quit!
    kbind 'Space restart
    kbind 'x     (() -> (gamestate.show-debug? = (not gamestate.show-debug?)))

    kbind 'Left  input-down key
    kbind 'Right input-down key
    kbind 'Up    input-down key
    kbind 'Down  input-down key

@@ 'on bottle.key-released
fn (key)
    from bottle.enums let Key
    inline kbind (binding action ...)
        if (key == (getattr Key binding))
            action ...

    kbind 'Left  input-up key
    kbind 'Right input-up key
    kbind 'Up    input-up key
    kbind 'Down  input-up key

fn generate-world ()
    local world : (Array AABB)
    for i in (range 20)
        'append world
            AABB (vec2 ((i * 40) - 400) 0) (vec2 20)
    world

@@ 'on bottle.load
fn ()
    renderer.init;

    gamestate =
        GameState
            geometry = (PrimitiveBatch)
            world = (generate-world)

# intersection tests
fn AABBvsAABB (a b)
    intersect? :=
        a.half-size + b.half-size > (abs (a.position - b.position))
    intersect? @ 0 and intersect? @ 1

fn calculate-msv (a b)
    dist := b.position - a.position # distance vector between colliders
    sdist := a.half-size + b.half-size # max distance vector (separation) if colliders were touching on both axis
    penetration :=
        vec2
            ? (dist.x > 0) (sdist.x - dist.x) (-sdist.x - dist.x)
            ? (dist.y > 0) (sdist.y - dist.y) (-sdist.y - dist.y)

    if ((abs penetration.x) < (abs penetration.y))
        -penetration.x0
    else
        -penetration.0y

inline smax (a b)
    if ((abs a) > (abs b))
        a
    else
        b

fn try-move (col)
    world := ('force-unwrap gamestate) . world

    vvv bind response
    fold (response = (vec2)) for ent in world
        if (AABBvsAABB col ent)
            msv := (calculate-msv col ent)
            vec2 (smax response.x msv.x) (smax response.y msv.y)
        else
            response

    let response =
        if ((abs response.x) > (abs response.y))
            response.x0
        else
            response.0y

    AABB (col.position + response) col.half-size

global frame-acc : f64
FIXED_TIMESTEP := 1:f64 / 60

fn simulate (dt)
    let gamestate = ('force-unwrap gamestate)
    let player world = gamestate.player gamestate.world

    local dir : vec2
    if input-state.Left
        dir.x = -1
    if input-state.Right
        dir.x = 1
    if input-state.Up
        dir.y = 1
    if input-state.Down
        dir.y = -1

    speed   := 200:f32
    gravity := -600:f32
    local new-pos = player.position + (vec2 0 (gravity * dt))
    if (dir != (vec2))
        new-pos += (normalize dir) * speed * (vec2 dt)

    gamestate.projection := (AABB new-pos player.half-size)
    player = (try-move gamestate.projection)

@@ 'on bottle.update
fn (dt)
    static-if USE_DT_ACCUMULATOR
        frame-acc += dt
        while (frame-acc > FIXED_TIMESTEP)
            simulate FIXED_TIMESTEP
            frame-acc -= FIXED_TIMESTEP
    else
        simulate dt

fn linear-to-srgb (c)
    c ** 2.2

@@ 'on bottle.draw
fn (rp)
    let gamestate = ('force-unwrap gamestate)
    let player world projection = gamestate.player gamestate.world gamestate.projection
    let geometry = gamestate.geometry

    'clear geometry
    'add-rectangle geometry (player.position - player.half-size) (player.half-size * 2) (vec4 0.7 0.25 0 1)
    for i ent in (enumerate world)
        l := i / (countof world)
        'add-rectangle geometry (ent.position - ent.half-size) (ent.half-size * 2) (vec4 (linear-to-srgb (vec3 l)) 1)

    if gamestate.show-debug?
        'add-rectangle geometry (projection.position - projection.half-size) (projection.half-size * 2) (vec4 1 0.7 0.25 0.5)

    renderer.set-camera-position (vec2 0 0)
    'draw geometry rp

fn restart ()
    ('force-unwrap gamestate) . player = (AABB (vec2 -200 250) (vec2 20))

bottle.run;
