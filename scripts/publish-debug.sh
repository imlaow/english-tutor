#!/usr/bin/env bash
#
# Build the debug APK, delete the previously published one, upload the new one
# to the Cloudflare R2 bucket `zhongzhuan`, and verify the upload byte-for-byte.
#
# Usage:
#   scripts/publish-debug.sh [--dry-run]
#
# Why this script looks the way it does — every point below is a bug that was
# hit for real with wrangler 4.120.0, not a precaution:
#
#   1. `wrangler r2 object` has only get / put / delete. There is no `list`, so
#      the bucket cannot be enumerated. Deleting the previous upload therefore
#      relies on this script's own ledger file. It NEVER deletes by filename
#      pattern: `zhongzhuan` is a shared scratch bucket holding unrelated
#      objects (including release APKs), and deletion is irreversible.
#
#   2. Every r2 command must pass --remote. Without it wrangler silently writes
#      to the local simulator under .wrangler/ and still prints "Upload
#      complete". The only hint is a "Resource location: local" line, so this
#      script greps for it and treats it as a hard failure.
#
#   3. READS ARE CACHED, AND MUTATIONS DO NOT INVALIDATE THE CACHE. This is the
#      root cause behind most of the "silent failure" folklore about this
#      bucket, and it dictates the shape of everything below.
#
#      Measured with two identical small objects deleted seconds apart, the
#      only difference being whether the key had been read beforehand:
#        - never read before the delete  -> gone immediately, as expected
#        - read once before the delete   -> still fully readable 5 minutes
#                                           later, with no reads in between
#      And a fresh upload whose key had been read (404) just before the put
#      was reported "Upload complete", read back as "key does not exist", then
#      appeared on its own about a minute later. The put had worked all along.
#
#      So a read of a key POISONS later observations of that key. The rules
#      that follow from it:
#        - never probe a key before writing or deleting it; a "does it exist?"
#          check is not free, it is what breaks the next operation
#        - never re-put over a key that has been read — hence the fresh
#          timestamped key every run, which also makes probing unnecessary
#        - treat a delete as best-effort and do NOT read back to confirm it;
#          that read is what makes a working delete look permanently broken
#
#   4. "Upload complete" is still not proof, so the object is downloaded again
#      and compared on md5 and byte count. This read is safe precisely because
#      the key is brand new and has never been read. Because propagation can
#      lag by around a minute, verification retries before giving up rather
#      than trusting a single answer.
#
#   5. The build needs no JAVA_HOME pinning. Java 17/21/25 all work.

set -euo pipefail

BUCKET="zhongzhuan"
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
LEDGER=".publish-debug-state"

DRY_RUN=0

# ---------------------------------------------------------------- output ----

step()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
warn()  { printf '\033[1;33mwarning:\033[0m %s\n' "$*" >&2; }
die()   { printf '\033[1;31merror:\033[0m %s\n' "$*" >&2; exit 1; }
plan()  { printf '\033[1;36m[dry-run]\033[0m %s\n' "$*"; }

# --------------------------------------------------------------- helpers ----

# There is deliberately no object_exists() helper here. An earlier version had
# one and used it to check that a key was free before uploading and that a key
# was gone after deleting. Both checks were actively harmful: see note 3 above.
# The read itself is the mutation-breaking act, so the only read this script
# performs is the md5 verification of a freshly written, never-before-read key.

# wrangler reports success even when it wrote to, or read from, the local
# simulator. The location line is the only tell.
assert_remote() {
  if printf '%s' "$1" | grep -qi 'Resource location: *local'; then
    printf '%s\n' "$1" >&2
    die "$2 used LOCAL simulated storage despite --remote"
  fi
}

# ------------------------------------------------------------------ args ----

while [ $# -gt 0 ]; do
  case "$1" in
    --dry-run) DRY_RUN=1 ;;
    -h|--help)
      sed -n '3,7p' "$0" | sed 's/^# \{0,1\}//'
      exit 0
      ;;
    *) die "unknown argument: $1 (supported: --dry-run)" ;;
  esac
  shift
done

# --------------------------------------------------------------- cleanup ----

WORK_DIR=""
# Must end in a success status: this runs as the EXIT trap, and a trap that
# returns non-zero becomes the script's own exit code.
cleanup() { if [ -n "$WORK_DIR" ]; then rm -rf "$WORK_DIR"; fi; return 0; }
trap cleanup EXIT

# ------------------------------------------------------------ preflight -----

step "Checking prerequisites"

command -v wrangler >/dev/null 2>&1 || die "wrangler is not on PATH"
command -v md5sum   >/dev/null 2>&1 || die "md5sum is not on PATH"

REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null)" \
  || die "not inside a git repository"
cd "$REPO_ROOT"

[ -x ./gradlew ] || die "no executable ./gradlew in $REPO_ROOT"

if ! WHOAMI_OUT="$(wrangler whoami 2>&1)"; then
  printf '%s\n' "$WHOAMI_OUT" >&2
  die "wrangler whoami failed — run 'wrangler login' first"
fi
if printf '%s' "$WHOAMI_OUT" | grep -qi "not authenticated\|You are not logged in"; then
  die "wrangler is not logged in — run 'wrangler login' first"
fi
ACCOUNT_EMAIL="$(printf '%s' "$WHOAMI_OUT" \
  | grep -o '[A-Za-z0-9._%+-]\+@[A-Za-z0-9-]\+\(\.[A-Za-z0-9-]\+\)*' \
  | head -1 || true)"
step "wrangler authenticated${ACCOUNT_EMAIL:+ as $ACCOUNT_EMAIL}"

# ----------------------------------------------- working tree cleanliness ---

SHA="$(git rev-parse --short HEAD)"
BRANCH="$(git rev-parse --abbrev-ref HEAD)"
DIRTY_SUFFIX=""

if [ -n "$(git status --porcelain)" ]; then
  DIRTY_SUFFIX="-dirty"
  warn "working tree has uncommitted changes."
  warn "The APK is built from the WORKING TREE, not from commit $SHA."
  warn "The upload key will carry a '-dirty' marker to record that."
fi

VERSION_NAME="$(sed -n 's/.*versionName *= *"\([^"]*\)".*/\1/p' \
  app/build.gradle.kts | head -1)"
[ -n "$VERSION_NAME" ] || VERSION_NAME="unknown"

STAMP="$(date -u +%Y%m%d-%H%M%S)"
KEY="app-debug-${VERSION_NAME}-${SHA}${DIRTY_SUFFIX}-${STAMP}.apk"

step "Branch $BRANCH at $SHA${DIRTY_SUFFIX}, version $VERSION_NAME"

# Read the ledger before doing anything destructive. Everything listed here was
# uploaded by this script on this machine; nothing else is ever a delete target.
# PREV_KEY is the only delete target, and STALE_KEYS keeps the loop below able
# to handle a ledger left behind by an older version of this script.
PREV_KEY=""
STALE_KEYS=()
if [ -f "$LEDGER" ]; then
  PREV_KEY="$(sed -n 's/^LAST_KEY=//p' "$LEDGER" | tail -1)"
  if [ -n "$PREV_KEY" ]; then STALE_KEYS+=("$PREV_KEY"); fi
  # Older ledgers recorded ORPHAN= lines for deletes that only LOOKED like they
  # failed. Retry them once so they are not left behind, then let them go.
  while IFS= read -r line; do
    if [ -n "$line" ]; then STALE_KEYS+=("$line"); fi
  done < <(sed -n 's/^ORPHAN=//p' "$LEDGER")
fi

# --------------------------------------------------------------- dry run ----

if [ "$DRY_RUN" -eq 1 ]; then
  plan "./gradlew assembleDebug           # produces $APK_PATH"
  if [ ${#STALE_KEYS[@]} -gt 0 ]; then
    for k in "${STALE_KEYS[@]}"; do
      plan "wrangler r2 object delete $BUCKET/$k --remote"
    done
    plan "  (recorded in $LEDGER as this script's own uploads; best-effort,"
    plan "   never read back to confirm — that read is what breaks a delete)"
  else
    plan "no keys recorded in $LEDGER — nothing would be deleted"
  fi
  plan "wrangler r2 object put $BUCKET/$KEY --file $APK_PATH --remote \\"
  plan "    --content-type application/vnd.android.package-archive"
  plan "wrangler r2 object get $BUCKET/$KEY --file <tmp> --remote"
  plan "  then compare md5 + byte count, failing the run on any mismatch"
  plan "record LAST_KEY=$KEY into $LEDGER"
  step "Dry run complete. Nothing was built, uploaded or deleted."
  exit 0
fi

# ----------------------------------------------------------------- build ----

step "Building debug APK (./gradlew assembleDebug)"
./gradlew assembleDebug || die "gradle build failed"

[ -f "$APK_PATH" ] || die "build reported success but $APK_PATH is missing"

LOCAL_MD5="$(md5sum "$APK_PATH" | awk '{print $1}')"
LOCAL_SIZE="$(wc -c < "$APK_PATH" | tr -d ' ')"
step "Built $APK_PATH ($LOCAL_SIZE bytes, md5 $LOCAL_MD5)"

# ------------------------------------------------------ delete previous -----

# Best-effort by design. The delete is NOT read back to confirm it: that read
# is exactly what makes a working delete look permanently broken (note 3), and
# a failed delete is not worth aborting a publish over.
if [ ${#STALE_KEYS[@]} -gt 0 ]; then
  for k in "${STALE_KEYS[@]}"; do
    step "Deleting previous upload $BUCKET/$k"
    if DELETE_OUT="$(wrangler r2 object delete "$BUCKET/$k" --remote 2>&1)"; then
      assert_remote "$DELETE_OUT" "delete"
      step "Deleted $k"
    else
      printf '%s\n' "$DELETE_OUT" >&2
      warn "could not delete $k (already removed by hand?) — continuing"
    fi
  done
else
  step "No previous upload recorded in $LEDGER — nothing to delete"
fi

# ---------------------------------------------------------------- upload ----

# $KEY carries a UTC timestamp, so it is new by construction and has never been
# read. That is what makes the verification below trustworthy — and it is why
# there is no "does this key already exist?" probe here.
step "Uploading to $BUCKET/$KEY"
if ! PUT_OUT="$(wrangler r2 object put "$BUCKET/$KEY" \
      --file "$APK_PATH" \
      --content-type application/vnd.android.package-archive \
      --remote 2>&1)"; then
  printf '%s\n' "$PUT_OUT" >&2
  die "upload failed"
fi
assert_remote "$PUT_OUT" "upload"

# ---------------------------------------------------------------- verify ----

WORK_DIR="$(mktemp -d)"
ROUNDTRIP="$WORK_DIR/roundtrip.apk"

# A just-written object can take up to about a minute to become readable, so a
# single failed read is not evidence of a bad upload. Retry before judging.
VERIFY_ATTEMPTS=6
VERIFY_DELAY=30
REMOTE_MD5=""
REMOTE_SIZE=""

for attempt in $(seq 1 "$VERIFY_ATTEMPTS"); do
  step "Verifying upload by downloading it back (attempt $attempt/$VERIFY_ATTEMPTS)"
  rm -f "$ROUNDTRIP"

  if GET_OUT="$(wrangler r2 object get "$BUCKET/$KEY" \
        --file "$ROUNDTRIP" --remote 2>&1)"; then
    assert_remote "$GET_OUT" "verification read"
    if [ -f "$ROUNDTRIP" ]; then
      REMOTE_MD5="$(md5sum "$ROUNDTRIP" | awk '{print $1}')"
      REMOTE_SIZE="$(wc -c < "$ROUNDTRIP" | tr -d ' ')"
      if [ "$REMOTE_SIZE" = "$LOCAL_SIZE" ] && [ "$REMOTE_MD5" = "$LOCAL_MD5" ]; then
        step "Verified: remote object matches the local APK"
        break
      fi
      warn "read back $REMOTE_SIZE bytes / md5 $REMOTE_MD5, expected $LOCAL_SIZE / $LOCAL_MD5"
    else
      warn "verification download produced no file"
    fi
  else
    printf '%s\n' "$GET_OUT" >&2
    warn "could not read $KEY back yet"
  fi

  if [ "$attempt" -eq "$VERIFY_ATTEMPTS" ]; then
    die "upload of $KEY could not be verified after $VERIFY_ATTEMPTS attempts. The object may still appear later, but this run is not trustworthy — check the Cloudflare dashboard before sharing this key."
  fi
  step "Retrying in ${VERIFY_DELAY}s (R2 reads can lag a fresh write)"
  sleep "$VERIFY_DELAY"
done

# ---------------------------------------------------------------- ledger ----

{
  printf '# Written by scripts/publish-debug.sh. Local machine state.\n'
  printf '# The next run deletes LAST_KEY. Do not point it at anything this\n'
  printf '# script did not upload: the bucket is shared, cannot be listed, and\n'
  printf '# deletes are irreversible.\n'
  printf 'LAST_KEY=%s\n' "$KEY"
  printf 'LAST_MD5=%s\n' "$LOCAL_MD5"
  printf 'LAST_SIZE=%s\n' "$LOCAL_SIZE"
  printf 'LAST_COMMIT=%s\n' "$SHA$DIRTY_SUFFIX"
  printf 'LAST_UPLOAD_UTC=%s\n' "$STAMP"
} > "$LEDGER"
step "Recorded new key in $LEDGER"

# ---------------------------------------------------------------- report ----

printf '\n'
printf 'Published to R2 bucket %s\n' "$BUCKET"
printf '  key   %s\n' "$KEY"
printf '  md5   %s\n' "$LOCAL_MD5"
printf '  size  %s bytes (%s MiB)\n' "$LOCAL_SIZE" "$((LOCAL_SIZE / 1048576))"
