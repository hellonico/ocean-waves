#!/bin/bash

# Path to the wave.json file
WAVE_JSON="wave.json"
BINARY="./waves"

# Use an environment variable to control SKIP_FIRST_COMPILATION, default to false if not set
SKIP_FIRST_COMPILATION=${SKIP_FIRST_COMPILATION:-false}

# File to log the output for debugging
DEBUG_LOG="graal_output.log"

while true; do

    if [ "$SKIP_FIRST_COMPILATION" = false ]; then
    # Step 1: Compile using GraalVM
    echo "Starting GraalVM compilation..."
    clj -A:native-image

    if [ $? -ne 0 ]; then
      echo "Compilation failed. Exiting."
      exit 1
    fi

    fi
  export SKIP_FIRST_COMPILATION=false

  # Step 2: Run the binary
  echo "Running the binary..."
  OUTPUT=$($BINARY 2>&1)
  EXIT_CODE=$?

  # Append output to debug log
    echo "$(date): Running $BINARY" >> "$DEBUG_LOG"
    echo "$OUTPUT" >> "$DEBUG_LOG"


#  if [ $EXIT_CODE -eq 0 ]; then
#    echo "Binary executed successfully. Exiting loop."
#    break
#  fi
# Step 3: Parse errors from the stack trace
  CLASS_NAME=$(echo "$OUTPUT" | grep -Eo 'java.lang.NoSuchMethodError: [[:alnum:].$]+\.<init>' | sed -E 's/java.lang.NoSuchMethodError: ([[:alnum:].$]+)\.<init>/\1/')
  FIELD_CLASS_NAME=$(echo "$OUTPUT" | grep -Eo 'java.lang.IllegalArgumentException: No matching field found: [[:alnum:]]+ for class [[:alnum:].$]+' | sed -E 's/java.lang.IllegalArgumentException: No matching field found: [[:alnum:]]+ for class ([[:alnum:].$]+)/\1/')

  if [ -n "$CLASS_NAME" ]; then
    echo "Found problematic constructor class: $CLASS_NAME"

    # Step 4: Create the new JSON line for constructors
    NEW_JSON_LINE="    {\"name\": \"$CLASS_NAME\", \"allDeclaredConstructors\": true},
"

    # Step 5: Insert the new line as the second line in wave.json
    echo "Updating $WAVE_JSON with constructor fix..."

    if [ ! -f "$WAVE_JSON" ]; then
      echo "$WAVE_JSON not found. Creating a new one..."
      echo -e "[\n$NEW_JSON_LINE\n]" > "$WAVE_JSON"
    else
      # Insert the line after the first line (using temporary file for macOS compatibility)
      sed "1 a\\
$NEW_JSON_LINE" "$WAVE_JSON" > "$WAVE_JSON.tmp" && mv "$WAVE_JSON.tmp" "$WAVE_JSON"
    fi
   elif [ -n "$FIELD_CLASS_NAME" ]; then
      echo "Found problematic field class: $FIELD_CLASS_NAME"

      # Step 4: Update the JSON for fields
      echo "Updating $WAVE_JSON with field fix..."

      if [ -f "$WAVE_JSON" ]; then
        # Remove the existing line for the class
        sed "/\"name\": \"$FIELD_CLASS_NAME\"/d" "$WAVE_JSON" > "$WAVE_JSON.tmp" && mv "$WAVE_JSON.tmp" "$WAVE_JSON"

        # Add the updated line after the first line
        # should do fields but doing methods instead
        UPDATED_JSON_LINE="    {\"name\": \"$FIELD_CLASS_NAME\", \"allDeclaredConstructors\": true, \"allDeclaredMethods\": true},
  "
        sed "1 a\\
  $UPDATED_JSON_LINE" "$WAVE_JSON" > "$WAVE_JSON.tmp" && mv "$WAVE_JSON.tmp" "$WAVE_JSON"
      else
        echo "$WAVE_JSON not found. Unable to apply field fix."
      fi
    else
      echo "No matching errors found in the stack trace. Restarting from Step 3."
      continue
    fi

done
