#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLANTUML_DIR="$ROOT_DIR/docs/plantuml"
SVG_DIR="$PLANTUML_DIR/svg"
PLANTUML_JAR_PATH="${PLANTUML_JAR_PATH:-$ROOT_DIR/.tools/plantuml.jar}"
PLANTUML_NO_DOCKER="${PLANTUML_NO_DOCKER:-0}"

mkdir -p "$SVG_DIR"

mapfile -t PUML_FILES < <(find "$PLANTUML_DIR" -maxdepth 1 -type f -name '*.puml' | sort)

if [[ "${#PUML_FILES[@]}" -eq 0 ]]; then
  echo "No PlantUML files found in $PLANTUML_DIR"
  exit 0
fi

echo "Generating SVG for ${#PUML_FILES[@]} PlantUML diagrams..."
if [[ "$PLANTUML_NO_DOCKER" != "1" ]] && command -v docker >/dev/null 2>&1; then
  docker run --rm \
    -v "$PLANTUML_DIR:/work" \
    plantuml/plantuml:latest \
    -tsvg /work/*.puml
elif command -v java >/dev/null 2>&1; then
  if [[ ! -f "$PLANTUML_JAR_PATH" ]]; then
    echo "PlantUML jar not found at $PLANTUML_JAR_PATH" >&2
    echo "Set PLANTUML_JAR_PATH to a local PlantUML jar or install docker." >&2
    exit 1
  fi
  java -jar "$PLANTUML_JAR_PATH" -tsvg "$PLANTUML_DIR"/*.puml
else
  echo "Neither docker nor java runtime is available for PlantUML rendering." >&2
  exit 1
fi

for puml in "${PUML_FILES[@]}"; do
  base_name="$(basename "$puml" .puml)"
  if [[ -f "$PLANTUML_DIR/${base_name}.svg" ]]; then
    mv "$PLANTUML_DIR/${base_name}.svg" "$SVG_DIR/${base_name}.svg"
  fi
done

echo "SVG generation complete: $SVG_DIR"
