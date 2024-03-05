#!/bin/bash

IFS='.' read -r -a nums <<< "${1//-dev/}.0"
VERSIONCODE=$((nums[0] * 100000000 + nums[1] * 100000 + nums[2] * 100 + nums[3]))
sed -i "/^version/c\\version: $1+$VERSIONCODE" pubspec.yaml
