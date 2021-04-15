#!/bin/bash
#
# Copyright (C) 2020 Mike Hummel (mh@mhus.de)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#         http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# config

REPO_PATH_ZIP="de/mhus/app/mhus-kpush/1.0.0-SNAPSHOT/mhus-kpush-1.0.0-SNAPSHOT-install.zip"
LOCAL_REPO_PATH_ZIP="$HOME/.m2/repository/$REPO_PATH_ZIP"
REMOTE_REPO_PATH_ZIP="https://repo1.maven.org/maven2/$REPO_PATH_ZIP"

# init

if [ ! -d $HOME/.kpush/bin/1.0.0-SNAPSHOT ]; then
  mkdir -p $HOME/.kpush/bin/1.0.0-SNAPSHOT
fi
if [ ! -d $HOME/.kpush/config ]; then
  mkdir -p $HOME/.kpush/config
fi
if [ ! -d $HOME/.kpush/tmp ]; then
  mkdir -p $HOME/.kpush/tmp
fi

# download

if [ ! -e $LOCAL_REPO_PATH_ZIP ]; then
  if command -v mvn &> /dev/null; then
    mvn org.apache.maven.plugins:maven-dependency-plugin:3.1.2:get \
      -Dartifact=de.mhus.app:mhus-kpush:1.0.0-SNAPSHOT:zip:install
  elif command -v curl &> /dev/null; then
    if [ -e $HOME/.kpush/tmp/kpush-install.zip ]; then
      rm $HOME/.conductor/tmp/jpush-install.zip
    fi
    curl --output $HOME/.kpush/tmp/kpush-install.zip $REMOTE_REPO_PATH_ZIP
    LOCAL_REPO_PATH_ZIP=$HOME/.kpush/tmp/kpush-install.zip
  else
     echo "Either mvn nor curl found - exit"
     exit 1
  fi
fi

if [ ! -e $LOCAL_REPO_PATH_ZIP ]; then
  echo "Can't download conductor install zip"
  echo $REMOTE_REPO_PATH_ZIP
  exit 1
fi

# unpack and setup

cd $HOME/.kpush/bin/1.0.0-SNAPSHOT
unzip -o $LOCAL_REPO_PATH_ZIP
chmod +x $HOME/.kpush/bin/1.0.0-SNAPSHOT/*.sh

if [ -e $HOME/.kpush/bin/con ]; then
  rm $HOME/.kpush/bin/con
fi
ln -s $HOME/.kpush/bin/1.0.0-SNAPSHOT/kpush.sh $HOME/.kpush/bin/kpush

# cleanup

if [ -e $HOME/.conductor/tmp/kpush-install.zip ]; then
  rm $HOME/.conductor/tmp/kpush-install.zip
fi

echo "Installed 1.0.0-SNAPSHOT in $HOME/.kpush"
echo "Add directory $HOME/.kpush/bin to \$PATH or link $HOME/.kpush/bin/kpush in a binary directory like /usr/local/bin"
