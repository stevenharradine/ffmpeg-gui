#!/bin/bash
export PATH_TO_FX=/usr/share/openjfx/lib/
javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.media CutterJoiner.java FFmpegGUI.java && \
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.media FFmpegGUI