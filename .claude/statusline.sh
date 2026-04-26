#!/usr/bin/env bash
# Claude Code Status Line for ciff project
# Displays: model | context usage | cwd | git branch
# Uses ANSI escape codes rendered via printf '%b'

input=$(cat)

model=$(echo "$input" | jq -r '.model.display_name // .model.id // "?"')
used=$(echo "$input" | jq -r '.context_window.used_percentage // empty')
cwd=$(echo "$input" | jq -r '.workspace.current_dir // empty')
project_dir=$(echo "$input" | jq -r '.workspace.project_dir // empty')

# Show relative path if cwd is under project_dir
if [ -n "$project_dir" ] && [ -n "$cwd" ]; then
  if [ "$cwd" = "$project_dir" ]; then
    dir=$(basename "$cwd")
  else
    dir="${cwd#"$project_dir"/}"
  fi
else
  dir=$(basename "$cwd")
fi

# Git branch (skip optional locks for speed)
branch=$(git -C "$cwd" --no-optional-locks rev-parse --abbrev-ref HEAD 2>/dev/null || echo "no git")

# Context percentage
ctx=""
if [ -n "$used" ]; then
  used_int=$(printf "%.0f" "$used")
  ctx="ctx:${used_int}%"
else
  ctx="ctx:-"
fi

# Color definitions
MAGENTA='\033[35m'
GREEN='\033[32m'
CYAN='\033[36m'
YELLOW='\033[33m'
DIM='\033[2m'
BOLD='\033[1m'
RESET='\033[0m'

# Dynamic context color based on usage
if [ -n "$used" ]; then
  used_int=$(printf "%.0f" "$used")
  if [ "$used_int" -gt 80 ]; then
    ctx_color="$YELLOW"
  elif [ "$used_int" -gt 50 ]; then
    ctx_color="$CYAN"
  else
    ctx_color="$GREEN"
  fi
else
  ctx_color="$DIM"
fi

printf '%b' "${MAGENTA}${model}${DIM} | ${RESET}${ctx_color}${ctx}${DIM} | ${RESET}${BOLD}${dir}${DIM} | ${RESET}${GREEN}${branch}${RESET}\n"
