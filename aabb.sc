using import Array
using import glm
using import Option
using import struct

import bottle
import .renderer

from renderer let SpriteBatch PrimitiveBatch Quad

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

fn resolve-collision (a b)
    # push back along movement vector
    dist := b.position - a.position # distance vector between colliders
    sdist := a.half-size + b.half-size # max distance vector (separation) if colliders were touching on both axis
    penetration :=
        vec2
            ? (dist.x > 0) (sdist.x - dist.x) (-sdist.x - dist.x)
            ? (dist.y > 0) (sdist.y - dist.y) (-sdist.y - dist.y)

    if ((abs penetration.x) < (abs penetration.y))
        AABB (a.position - penetration.x0) a.half-size
    else
        AABB (a.position - penetration.0y) a.half-size

@@ 'on bottle.update
fn (dt)
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
    gravity := -100:f32
    local new-pos = player.position + (vec2 0 (gravity * dt))
    if (dir != (vec2))
        new-pos += (normalize dir) * speed * (vec2 dt)

    gamestate.projection := (AABB new-pos player.half-size)
    player =
        fold (projection = gamestate.projection) for ent in world
            if (AABBvsAABB projection ent)
                (resolve-collision projection ent)
            else
                projection

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
    print "restart"

bottle.run;
