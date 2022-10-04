using import struct
using import Option
using import glm

import bottle
import .renderer
from renderer let SpriteBatch Quad
let cb = (import bottle.src.sysevents.callbacks)
using import bottle.src.sysevents.keyconstants

struct GameState
    sprites : SpriteBatch
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

    local spritebatch = (SpriteBatch "assets/pig_walk.png")
    'add-sprite spritebatch (vec2) (vec2 36 30)
        pixels-to-UV (vec4 0 0 36 30) (vec2 576 30)
    gamestate =
        GameState
            sprites = spritebatch
    ;

global do-once = true
@@ 'on bottle.update
fn (dt)
    if do-once
        do-once = false
        return;
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
    ;

bottle.run;
