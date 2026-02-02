#!/usr/bin/env bash
# Simple helper script to POST a MessageSuggestionEntity to the SSE endpoint and write events to a file.
# Usage: ./stream_suggestions.sh '<system message>' '<user message>' output.txt

set -euo pipefail

SYSTEM_MESSAGE=${1:-"You are a helpful assistant."}
USER_MESSAGE=${2:-"Suggest reply options for: I need help with my order"}
OUTFILE=${3:-sse_output.txt}

JSON_PAYLOAD=$(jq -n --arg s "$SYSTEM_MESSAGE" --arg u "$USER_MESSAGE" '{systemMessage: $s, userMessage: $u}')

echo "Posting to http://localhost:8080/ai/recommender and streaming to $OUTFILE"

# Use curl -N to avoid buffering; use --no-buffer for some curl versions
curl -N -H "Content-Type: application/json" \
  -X POST http://localhost:8080/ai/recommender \
  -d "$JSON_PAYLOAD" \
  | tee "$OUTFILE"

echo "Done. Output saved to $OUTFILE"
# Example Ollama properties for ms-suggestions
# Copy this file to application.properties or adapt as needed

ollama.base-url=http://localhost:11434
ollama.llama.model=ollama/local-model
ollama.llama.temperature=0.7
ollama.llama.numPredict=128
ollama.llama.keepAlive=none
ollama.connect-timeout-ms=10000
ollama.response-timeout-s=60

