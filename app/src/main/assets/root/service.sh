#!/system/bin/sh
DIR=${0%/*}

package_name="__PKG_NAME__"
version="__VERSION__"

rm "$DIR/log"

{

until [ "$(getprop sys.boot_completed)" = 1 ]; do sleep 5; done
sleep 5

base_path="$DIR/$package_name.apk"
stock_path="$(pm path "$package_name" | grep base | sed 's/package://g')"
stock_version="$(dumpsys package "$package_name" | grep versionName | cut -d "=" -f2)"

echo "base_path: $base_path"
echo "stock_path: $stock_path"
echo "base_version: $version"
echo "stock_version: $stock_version"

if mount | grep -q "$stock_path" ; then
  echo "Not mounting as stock path is already mounted"
  exit 1
fi

if [ "$version" != "$stock_version" ]; then
  echo "Not mounting as versions don't match"
  exit 1
fi

if [ -z "$stock_path" ]; then
  echo "Not mounting as app info could not be loaded"
  exit 1
fi

mount -o bind "$base_path" "$stock_path"

} >> "$DIR/log"
