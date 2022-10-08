using import struct
using import Option
using import glm

import bottle
import .renderer
using import .helpers
from renderer let SpriteBatch PrimitiveBatch Quad
let cb = (import bottle.src.sysevents.callbacks)
using import bottle.src.sysevents.keyconstants
using bottle.gpu.types

struct GameState
    sprites : SpriteBatch
    geometry : PrimitiveBatch
    image : GPUTexture
    animation-timer : f64

global gamestate : (Option GameState)

@@ 'on bottle.configure
fn (cfg)
    cfg.window.title = "2d renderer test"

fn pixels-to-UV (quad size)
    Quad (quad.xy / size) ((quad.zw - quad.xy) / size)

fn update-animation ()
    let gamestate = ('force-unwrap gamestate)
    let animation-frame = (((gamestate.animation-timer * 6) % 16) as i32)
    'clear gamestate.sprites
    for i in (range 1025)
        'add-sprite gamestate.sprites (vec2 (36 * i) 0) (vec2 36 30)
            (pixels-to-UV (vec4 (36 * animation-frame) 0 (36 * (animation-frame + 1)) 30) (vec2 576 30))

@@ 'on cb.on-key-pressed
fn (key)
    if (key == Key.Space)
        update-animation;

@@ 'on bottle.load
fn ()
    renderer.init;

    let image = (load-image "assets/pig_walk.png")
    local spritebatch = (SpriteBatch image)
    'add-sprite spritebatch (vec2) (vec2 36 30)
        pixels-to-UV (vec4 0 0 36 30) (vec2 576 30)

    local geometry = (PrimitiveBatch)
    for i in (range 1025)
        'add-rectangle geometry (vec2 (36 * i) 36) (vec2 36 30)

    gamestate =
        GameState
            sprites = spritebatch
            geometry = geometry
            image = image
    ;

@@ 'on bottle.update
fn (dt)
    update-animation;
    let gamestate = ('force-unwrap gamestate)
    let spritebatch = gamestate.sprites
    gamestate.animation-timer += dt
    ;

@@ 'on bottle.draw
fn (rp)
    let gamestate = ('force-unwrap gamestate)

    renderer.set-camera-position (vec2 0 0)
    'draw gamestate.sprites rp
    'draw gamestate.geometry rp
    ;

bottle.run;
