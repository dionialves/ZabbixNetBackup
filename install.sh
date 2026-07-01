#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# ZabbixNetBackup - Installation script
# ============================================================================
# This script:
#   1. Verifies Java 21 is installed (asks permission to install if not)
#   2. Clones the ZabbixNetBackup repository
#   3. Builds the project with Maven
#   4. Generates the self-executable `znb` file
#   5. Installs it into ~/.local/bin/
#   6. Instructs the user how to add it to PATH and run it
# ============================================================================

REPO_URL="https://github.com/dionialves/ZabbixNetBackup.git"
INSTALL_DIR="${HOME}/.local/bin"
CLONE_DIR="${HOME}/.zabbix-net-backup-src"

# ----------------------------------------------------------------------------
# Helpers
# ----------------------------------------------------------------------------
info()  { printf "\033[1;34m==>\033[0m %s\n" "$1"; }
ok()    { printf "\033[1;32m  OK\033[0m  %s\n" "$1"; }
warn()  { printf "\033[1;33m  !!\033[0m  %s\n" "$1"; }
fail()  { printf "\033[1;31m  ERROR\033[0m %s\n" "$1"; exit 1; }

command_exists() { command -v "$1" >/dev/null 2>&1; }

# ----------------------------------------------------------------------------
# Step 1: Verify Java 21
# ----------------------------------------------------------------------------
verify_java() {
    info "Verifying Java 21..."

    if command_exists java; then
        java_version=$(java -version 2>&1 | head -n1 | sed -E 's/.*version "([0-9]+).*/\1/')
        if [[ "${java_version}" -ge 21 ]]; then
            ok "Java ${java_version} found."
            return 0
        fi
        warn "Java ${java_version} found, but Java 21 or higher is required."
    else
        warn "Java is not installed."
    fi

    # Ask the user for permission to install Java 21
    printf "\n"
    printf "Java 21 is required to build and run znb.\n"
    printf "Detected OS: %s\n" "$(uname -s)"
    printf "\n"

    OS="$(uname -s)"
    case "${OS}" in
        Darwin)
            if command_exists brew; then
                printf "Java 21 can be installed via Homebrew.\n"
                read -rp "Do you want to install OpenJDK 21 via Homebrew? [y/N] " answer
                if [[ "${answer}" =~ ^[Yy]$ ]]; then
                    info "Installing OpenJDK 21 via Homebrew..."
                    brew install openjdk@21
                    brew link --force openjdk@21
                    ok "OpenJDK 21 installed."
                    return 0
                fi
            else
                warn "Homebrew is not installed. Install it from https://brew.sh and re-run this script."
                fail "Cannot install Java 21 without Homebrew on macOS."
            fi
            ;;
        Linux)
            if command_exists apt-get; then
                printf "Java 21 can be installed via apt.\n"
                read -rp "Do you want to install OpenJDK 21 via apt (sudo required)? [y/N] " answer
                if [[ "${answer}" =~ ^[Yy]$ ]]; then
                    info "Installing OpenJDK 21 via apt..."
                    sudo apt-get update
                    sudo apt-get install -y openjdk-21-jdk
                    ok "OpenJDK 21 installed."
                    return 0
                fi
            elif command_exists dnf; then
                printf "Java 21 can be installed via dnf.\n"
                read -rp "Do you want to install OpenJDK 21 via dnf (sudo required)? [y/N] " answer
                if [[ "${answer}" =~ ^[Yy]$ ]]; then
                    info "Installing OpenJDK 21 via dnf..."
                    sudo dnf install -y java-21-openjdk-devel
                    ok "OpenJDK 21 installed."
                    return 0
                fi
            elif command_exists pacman; then
                printf "Java 21 can be installed via pacman.\n"
                read -rp "Do you want to install OpenJDK 21 via pacman (sudo required)? [y/N] " answer
                if [[ "${answer}" =~ ^[Yy]$ ]]; then
                    info "Installing OpenJDK 21 via pacman..."
                    sudo pacman -S --noconfirm jdk-openjdk
                    ok "OpenJDK 21 installed."
                    return 0
                fi
            else
                warn "No supported package manager found (apt/dnf/pacman)."
                fail "Please install Java 21 manually and re-run this script."
            fi
            ;;
        *)
            fail "Unsupported OS: ${OS}. Please install Java 21 manually."
            ;;
    esac

    fail "Java 21 is required. Install it manually and re-run this script."
}

# ----------------------------------------------------------------------------
# Step 2: Clone the repository
# ----------------------------------------------------------------------------
clone_repo() {
    info "Cloning ZabbixNetBackup repository..."

    if [[ -d "${CLONE_DIR}" ]]; then
        warn "Source directory already exists at ${CLONE_DIR}"
        read -rp "Remove it and clone again? [y/N] " answer
        if [[ "${answer}" =~ ^[Yy]$ ]]; then
            rm -rf "${CLONE_DIR}"
        else
            fail "Cannot continue without a fresh clone. Aborting."
        fi
    fi

    git clone "${REPO_URL}" "${CLONE_DIR}"
    ok "Repository cloned to ${CLONE_DIR}"
}

# ----------------------------------------------------------------------------
# Step 3 & 4: Build the project and generate the znb executable
# ----------------------------------------------------------------------------
build_project() {
    info "Building ZabbixNetBackup with Maven..."

    cd "${CLONE_DIR}"

    # Ensure Maven is available; use the Maven Wrapper if present, otherwise
    # fall back to a system mvn.
    if [[ -x "./mvnw" ]]; then
        ok "Using Maven Wrapper (./mvnw)."
        ./mvnw -q clean package
    elif command_exists mvn; then
        ok "Using system Maven (mvn)."
        mvn -q clean package
    else
        warn "Maven not found. Installing Maven Wrapper..."
        # Bootstrap the Maven Wrapper without a system Maven by downloading it.
        fail "Maven is required to build. Install Maven (e.g. 'brew install maven' or 'sudo apt install maven') and re-run this script."
    fi

    if [[ ! -f "${CLONE_DIR}/target/znb" ]]; then
        fail "Build finished but target/znb was not generated."
    fi

    ok "znb executable generated at ${CLONE_DIR}/target/znb"
}

# ----------------------------------------------------------------------------
# Step 5: Install znb into ~/.local/bin/
# ----------------------------------------------------------------------------
install_znb() {
    info "Installing znb into ${INSTALL_DIR}/..."

    mkdir -p "${INSTALL_DIR}"

    # Remove any existing znb to avoid conflicts
    if [[ -f "${INSTALL_DIR}/znb" ]]; then
        rm -f "${INSTALL_DIR}/znb"
    fi

    cp "${CLONE_DIR}/target/znb" "${INSTALL_DIR}/znb"
    chmod 755 "${INSTALL_DIR}/znb"

    ok "znb installed at ${INSTALL_DIR}/znb"
}

# ----------------------------------------------------------------------------
# Step 5b: Remove cloned source tree (no longer needed after install)
# ----------------------------------------------------------------------------
cleanup() {
    info "Cleaning up cloned source files..."

    if [[ -d "${CLONE_DIR}" ]]; then
        rm -rf "${CLONE_DIR}"
        ok "Removed ${CLONE_DIR}"
    fi
}

# ----------------------------------------------------------------------------
# Step 6: Instructions for the user
# ----------------------------------------------------------------------------
print_instructions() {
    printf "\n"
    printf "\033[1;32m========================================\033[0m\n"
    printf "\033[1;32m  ZabbixNetBackup installed successfully!\033[0m\n"
    printf "\033[1;32m========================================\033[0m\n"
    printf "\n"
    printf "The 'znb' command is now available at: %s/znb\n" "${INSTALL_DIR}"
    printf "\n"

    # Check if ~/.local/bin is already in PATH
    if [[ ":${PATH}:" == *":${INSTALL_DIR}:"* ]]; then
        ok "${INSTALL_DIR} is already in your PATH."
        printf "\nYou can now run:\n"
    else
        warn "${INSTALL_DIR} is NOT in your PATH."
        printf "\nAdd it to your PATH by appending this line to your shell config:\n\n"

        case "$(basename "${SHELL}")" in
            zsh)
                printf "  echo 'export PATH=\"%s:\$PATH\"' >> ~/.zshrc && source ~/.zshrc\n" "${INSTALL_DIR}"
                ;;
            bash)
                printf "  echo 'export PATH=\"%s:\$PATH\"' >> ~/.bashrc && source ~/.bashrc\n" "${INSTALL_DIR}"
                ;;
            fish)
                printf "  fish_add_path %s\n" "${INSTALL_DIR}"
                ;;
            *)
                printf "  export PATH=\"%s:\$PATH\"\n" "${INSTALL_DIR}"
                ;;
        esac

        printf "\nThen run:\n"
    fi

    printf "\n"
    printf "  znb --help              # Show available commands\n"
    printf "  znb init                # Configure Zabbix credentials\n"
    printf "  znb backup mikrotik -u <user> -p -g <group-id>\n"
    printf "\n"
    printf "To update znb later, just re-run this installer script.\n"
    printf "\n"
}

# ----------------------------------------------------------------------------
# Main flow
# ----------------------------------------------------------------------------
main() {
    printf "\n"
    printf "\033[1;36m============================================\033[0m\n"
    printf "\033[1;36m  ZabbixNetBackup - Installer\033[0m\n"
    printf "\033[1;36m============================================\033[0m\n"
    printf "\n"

    verify_java
    clone_repo
    build_project
    install_znb
    cleanup
    print_instructions
}

main "$@"