using import String
let stbi = (import stb.image)
import bottle
using bottle.gpu.types

let stdio = (include "stdio.h")

fn read-whole-file (path)
    using stdio.extern
    using stdio.define

    fhandle := (fopen path "rb")
    assert (fhandle != null)
    local buf : String
    fseek fhandle 0 SEEK_END
    flen := (ftell fhandle) as u64
    fseek fhandle 0 SEEK_SET

    'resize buf flen

    fread buf flen 1 fhandle
    fclose fhandle
    buf

fn load-image (filename)
    local w : i32
    local h : i32
    local channels : i32

    let data = (stbi.load filename &w &h &channels 4)
    assert (data != null)
    let texture = (GPUTexture data w h)
    stbi.image_free data

    texture

do
    let read-whole-file load-image
    locals;
