#!/bin/bash
PATH_TO_BIN=bin/
PATH_TO_FX=/usr/share/openjfx/lib/
javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.media -d $PATH_TO_BIN \
	src/CutterJoiner.java \
	src/FFmpegGUI.java \
	&& \
java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.media -classpath $PATH_TO_BIN FFmpegGUI