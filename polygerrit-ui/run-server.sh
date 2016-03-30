#!/bin/bash
# Copyright (C) 2015 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -eu

while [[ ! -f .buckconfig && "$PWD" != / ]]; do
  cd ..
done
if [[ ! -f .buckconfig ]]; then
  echo "$(basename "$0"): must be run from a gerrit checkout" 1>&2
  exit 1
fi

buck build //polygerrit-ui/app:polygerrit_tests
cd polygerrit-ui/app
rm -rf bower_components
unzip -q ../../buck-out/gen/polygerrit-ui/app/test_components/test_components.bower_components.zip
cd ..
exec go run server.go "$@"
