#!/usr/bin/env bash
if [ -z ${EO_URL+x} ]; then
    EO_URL="https://gist.githubusercontent.com/radgeRayden/e1dca7981335254f750d50fae6943645/raw/9883f61aa57ece45f78a3220d2bb1a94a93cc9d3/eo";
fi

EO=$(mktemp)
wget $EO_URL -O $EO
chmod +x $EO

#clean everytyhing
rm -rf .eo
rm -rf ./lib
rm -rf ./recipes

echo "y" | $EO init "https://raw.githubusercontent.com/ScopesCommunity/eo-packages/main/scopes-community.eo"
$EO import sdl2
$EO import wgpu
$EO import stb
$EO install -y sdl2 wgpu-native stb

BOTTLE_DIR=$(mktemp -d)
pushd $BOTTLE_DIR
wget "https://github.com/radgeRayden/bottle/archive/master.tar.gz"
tar -zxvf master.tar.gz
popd

mkdir -p ./lib/scopes/packages
mv -f $BOTTLE_DIR/bottle-main ./lib/scopes/packages/bottle
wget "https://raw.githubusercontent.com/radgeRayden/strfmt.sc/main/strfmt.sc" -O ./lib/scopes/packages/strfmt.sc
