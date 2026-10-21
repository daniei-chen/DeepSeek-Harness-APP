#!/usr/bin/env sh
set -eu

DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec node "$DIR/dsh-mobile-setup.mjs" install "$@"
