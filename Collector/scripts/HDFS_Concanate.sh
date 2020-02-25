#!/bin/bash

###
# This script will concatenate files by sub-directory from a specific starting point.
#
# 20170228  Updated script to process all directories
#
DATE_COM="date  --iso-8601=s -u"

for DIR in $(hadoop fs -ls /openke/openke/*/json/ | sed '1d;s/  */ /g' | cut -d\  -f8)
do
  #DIR=$1
  TMP_DIR=/tmp$DIR"/"
  TODAY_DIR=$DIR/`date +%Y%m%d`

  echo `$DATE_COM`": processing "$DIR

  mkdir -p $TMP_DIR

  # get the subdirectories in HDFS
  for line in  `hadoop fs -ls $DIR | grep "^d" | cut -d " " -f 19 | grep -v $TODAY_DIR`; do
    echo `$DATE_COM`": Concatenating files in "$line
    OUTPUT_FILE=$TMP_DIR$(basename $line)".dat"
    hadoop fs -getmerge -nl $line $OUTPUT_FILE
    hadoop fs -put -f $OUTPUT_FILE $DIR
    hadoop fs -rm -r $line
    rm $OUTPUT_FILE
  done

  rm -rf $TMP_DIR

  echo `$DATE_COM`": finished "$DIR
done
echo 'script complete'
