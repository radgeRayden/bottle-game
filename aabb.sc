using import struct
using import Option
using import glm

import bottle
import .renderer

from renderer let SpriteBatch PrimitiveBatch Quad
let cb = (import bottle.src.sysevents.callbacks)
let sysevents = (import bottle.src.sysevents)

struct InputState
    Left  : bool
    Right : bool
    Up    : bool
    Down  : bool

struct GameState
    # x y hw hh
    aabb1 = (vec4 -200 -200 50 50)
    aabb2 = (vec4 0 0 50 50)
    collision? : bool
    projection : vec4
    show-debug? : bool

    geometry : PrimitiveBatch

global input-state : InputState
global gamestate : (Option GameState)

inline update-input (key value)
    using import bottle.src.sysevents.keyconstants
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

@@ 'on cb.on-key-pressed
fn (key)
    gamestate := ('force-unwrap gamestate)

    using import bottle.src.sysevents.keyconstants
    inline kbind (binding action ...)
        if (key == (getattr Key binding))
            action ...

    kbind 'Escape sysevents.quit
    kbind 'Space restart
    kbind 'x     (() -> (gamestate.show-debug? = (not gamestate.show-debug?)))

    kbind 'Left  input-down key
    kbind 'Right input-down key
    kbind 'Up    input-down key
    kbind 'Down  input-down key

@@ 'on cb.on-key-released
fn (key)
    using import bottle.src.sysevents.keyconstants
    inline kbind (binding action ...)
        if (key == (getattr Key binding))
            action ...

    kbind 'Left  input-up key
    kbind 'Right input-up key
    kbind 'Up    input-up key
    kbind 'Down  input-up key

@@ 'on bottle.load
fn ()
    renderer.init;
    local geometry = (PrimitiveBatch)

    gamestate =
        GameState
            geometry = geometry

# intersection tests
fn AABBvsAABB (a b)
    and
        (a.z + b.z) > (abs (a.x - b.x))
        (a.w + b.w) > (abs (a.y - b.y))

fn resolve-collision (a b)
    # push back along movement vector
    dist := b.xy - a.xy # distance vector between colliders
    sdist := a.zw + b.zw # max distance vector (separation) if colliders were touching on both axis
    penetration :=
        vec2
            ? (dist.x > 0) (sdist.x - dist.x) (-sdist.x - dist.x)
            ? (dist.y > 0) (sdist.y - dist.y) (-sdist.y - dist.y)

    if ((abs penetration.x) < (abs penetration.y))
        vec4 (a.xy - penetration.x0) a.zw
    else
        vec4 (a.xy - penetration.0y) a.zw

@@ 'on bottle.update
fn (dt)
    let gamestate = ('force-unwrap gamestate)
    let aabb1 aabb2 = gamestate.aabb1 gamestate.aabb2

    local dir : vec2
    if input-state.Left
        dir.x = -1
    if input-state.Right
        dir.x = 1
    if input-state.Up
        dir.y = 1
    if input-state.Down
        dir.y = -1

    let speed = 600:f32
    if (dir != (vec2))
        new-pos := aabb1.xy + ((normalize dir) * speed * (vec2 dt))
        projection := (vec4 new-pos aabb1.zw)
        if (AABBvsAABB projection aabb2)
            aabb1 = (resolve-collision projection aabb2)
            gamestate.projection = projection
        else
            aabb1 = projection

@@ 'on bottle.draw
fn (rp)
    let gamestate = ('force-unwrap gamestate)
    let aabb1 aabb2 projection = gamestate.aabb1 gamestate.aabb2 gamestate.projection
    let geometry = gamestate.geometry

    'clear geometry
    'add-rectangle geometry (aabb1.xy - (aabb1.zw / 2)) (aabb1.zw * 2) (vec4 0.7 0.25 0 1)
    'add-rectangle geometry (aabb2.xy - (aabb2.zw / 2)) (aabb2.zw * 2) (vec4 0 0.7 0.25 1)

    if gamestate.show-debug?
        'add-rectangle geometry (projection.xy - (projection.zw / 2)) (projection.zw * 2) (vec4 1 0.7 0.25 0.5)

    renderer.set-camera-position (vec2 0 0)
    'draw geometry rp

fn restart ()
    print "restart"

bottle.run;
