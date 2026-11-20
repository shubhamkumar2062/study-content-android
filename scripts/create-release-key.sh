#!/bin/bash
# Creates the key that signs every StudyDesk release, then writes keystore.properties
# so ./gradlew :app:assembleRelease signs with it. Run it once, in your own terminal.
#
#   ./scripts/create-release-key.sh
#
# Keep a backup of the key file and the password somewhere safe (a password manager
# plus a second copy of the file). If you lose either, you can never update the app
# for people who installed it; they would have to uninstall and reinstall.
set -euo pipefail

KEY_DIR="$HOME/StudyDeskKeys"
KEY_FILE="$KEY_DIR/studydesk-release.jks"
ALIAS="studydesk"
PROPS="$(cd "$(dirname "$0")/.." && pwd)/keystore.properties"

if [ -e "$KEY_FILE" ]; then
  echo "A key already exists at $KEY_FILE. Not overwriting it."
  exit 1
fi

KEYTOOL="${JAVA_HOME:-/Users/$USER/Library/Java/JavaVirtualMachines/jbr-21.0.11/Contents/Home}/bin/keytool"
[ -x "$KEYTOOL" ] || KEYTOOL="$(command -v keytool)"

read -r -s -p "Choose a password (at least 12 characters): " PASSWORD; echo
read -r -s -p "Type it again: " AGAIN; echo
if [ "$PASSWORD" != "$AGAIN" ]; then echo "The two passwords differ. Nothing was created."; exit 1; fi
if [ ${#PASSWORD} -lt 12 ]; then echo "Use at least 12 characters. Nothing was created."; exit 1; fi
case "$PASSWORD" in " "*|*" ") echo "The password can't start or end with a space. Nothing was created."; exit 1;; esac

mkdir -p "$KEY_DIR"
chmod 700 "$KEY_DIR"
export STUDYDESK_KEY_PASSWORD="$PASSWORD"
"$KEYTOOL" -genkeypair -keystore "$KEY_FILE" -storetype PKCS12 \
  -alias "$ALIAS" -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=StudyDesk" \
  -storepass:env STUDYDESK_KEY_PASSWORD -keypass:env STUDYDESK_KEY_PASSWORD
unset STUDYDESK_KEY_PASSWORD
chmod 600 "$KEY_FILE"

# keystore.properties is read as a Java properties file, where "\" is an escape character.
ESCAPED=$(printf '%s' "$PASSWORD" | sed 's/\\/\\\\/g')
umask 077
cat > "$PROPS" <<EOF
storeFile=$KEY_FILE
storePassword=$ESCAPED
keyAlias=$ALIAS
keyPassword=$ESCAPED
EOF
chmod 600 "$PROPS"

echo
echo "Created $KEY_FILE and $PROPS (both readable only by you; neither goes into git)."
echo "Back up the key file and the password now."
