#!/usr/bin/env bash

KTFMT_VERSION="0.54" # Change this to the desired version. Note that Homebrew will always install the latest version

# Check if ktfmt is not installed
if ! command -v ktfmt &>/dev/null; then
  
  echo "Installing ktfmt $KTFMT_VERSION..."
  
  # install proper version based on OS and architecture
  if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    echo "Detected macOS system"
    brew install ktfmt
  else
    # Linux
    echo "Detected Linux system"
    # Create a temporary directory
    TMP_DIR=$(mktemp -d)
    cd "$TMP_DIR" || exit 1
    
    curl -L -o ktfmt.jar "https://github.com/facebook/ktfmt/releases/download/$KTFMT_VERSION/ktfmt-$KTFMT_VERSION-jar-with-dependencies.jar"
    mkdir -p "$HOME/bin"
    # Move jar to a permanent location
    mv ktfmt.jar "$HOME/bin/"
    
    # Create wrapper script
    cat > "$HOME/bin/ktfmt" << EOF
#!/usr/bin/env bash
java -jar "$HOME/bin/ktfmt.jar" "\$@"
EOF
    chmod +x "$HOME/bin/ktfmt"

    # Add to PATH if not already there
    if [[ ":$PATH:" != *":$HOME/bin:"* ]]; then
      echo "export PATH=\"\$HOME/bin:\$PATH\"" >> "$HOME/.bashrc"
      echo "export PATH=\"\$HOME/bin:\$PATH\"" >> "$HOME/.bash_profile"
      export PATH="$HOME/bin:$PATH"
    fi
  fi

  echo "ktfmt $KTFMT_VERSION installed successfully!"
else
  echo "ktfmt is already installed"
fi
