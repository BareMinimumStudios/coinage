#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
loader="${1:-both}"
target="${2:-all}"
case "$loader" in
  both) task=build ;;
  fabric|neoforge) task=":$loader:build" ;;
  *) echo "Usage: $0 [both|fabric|neoforge] [all|1.21.1|26.3]" >&2; exit 2 ;;
esac
case "$target" in
  all) targets=(1.21.1 26.3) ;;
  1.21.1|26.3) targets=("$target") ;;
  *) echo "Unknown Minecraft target: $target" >&2; exit 2 ;;
esac
for selected in "${targets[@]}"; do
  bash ./gradlew "-PcoinageTarget=$selected" "-PcoinageLoader=$loader" "$task" --console=plain
  if [[ "$loader" == both ]]; then
    COINAGE_TARGET="$selected" python3 scripts/verify_release.py
  else
    RELEASE_LOADER="$loader" COINAGE_TARGET="$selected" python3 scripts/verify_release.py
  fi
done
