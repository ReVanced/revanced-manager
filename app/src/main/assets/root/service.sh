#!/system/bin/sh
DIR=${0%/*}

package_name="__PKG_NAME__"
version="__VERSION__"
sanitized_package_name=$(echo "$package_name" | sed 's/\./_/g')

rm -f "$DIR/log"

{
echo "Induction check for $package_name"

until [ "$(getprop sys.boot_completed)" = 1 ]; do sleep 5; done
# Wait a bit more for package manager to settle
sleep 10

base_path="$DIR/system/app/$sanitized_package_name/base.apk"
if [ ! -f "$base_path" ]; then
    # Fallback to old path for compatibility during transition
    base_path="$DIR/$package_name.apk"
fi

stock_path="$(pm path "$package_name" | grep base | sed 's/package://g' | head -n 1)"
stock_version="$(dumpsys package "$package_name" | grep versionName | cut -d "=" -f2 | head -n 1 | sed 's/ //g')"

echo "base_path: $base_path"
echo "stock_path: $stock_path"
echo "base_version: $version"
echo "stock_version: $stock_version"

if [ -z "$stock_path" ]; then
  echo "App $package_name is not installed. System app induction might have failed or still being processed."
  exit 1
fi

if echo "$stock_path" | grep -q "^/system/"; then
  echo "App is already running from system partition (likely our Magisk overlay). Skipping bind mount."
  exit 0
fi

if mount | grep -q "$stock_path" ; then
  echo "Not mounting as stock path is already mounted"
  exit 1
fi

if [ "$version" != "$stock_version" ]; then
  echo "Version mismatch: base=$version, stock=$stock_version. Attempting to mount anyway as it might be a minor diff."
  # Optional: exit 1 if you want to be strict
fi

echo "Mounting $base_path over $stock_path"
mount -o bind "$base_path" "$stock_path"

} >> "$DIR/log"
