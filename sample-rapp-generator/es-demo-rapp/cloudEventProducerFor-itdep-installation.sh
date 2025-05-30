#!/bin/bash
#  ============LICENSE_START===============================================
#  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#

while getopts "u:p:" opt; do
  case $opt in
    u) KAFKA_USER="$OPTARG" ;;
    p) KAFKA_PASSWORD="$OPTARG" ;;
    *) echo "Usage: $0 [-u KAFKA_USER] [-p KAFKA_PASSWORD]" >&2; exit 1 ;;
  esac
done

if [ -z "$KAFKA_USER" ] || [ -z "$KAFKA_PASSWORD" ]; then
  echo "Error: All arguments (-u, -p) are required." >&2
  echo "Usage: $0 [-u KAFKA_USER] [-p KAFKA_PASSWORD]" >&2
  exit 1
fi

KAFKA_NAMESPACE="${1:-onap}"
KAFKA_TOPIC="${2:-topology-inventory-ingestion}"
KAFKA_POD_NAME="${3:-onap-strimzi-kafka-0}"
FILE_DUPLICATION_FACTOR="${4:-1}" # How many times to use the same CloudEvent file
FOLDER_OF_CLOUDEVENTS="${5:-events}" # All files in this folder are written to kafka
# number of generated files == number of files in the $FOLDER_OF_CLOUDEVENTS * $FILE_DUPLICATION_FACTOR

echo "Producing messages to Kafka topic '$KAFKA_TOPIC'..."

# Loop to produce multiple messages
for ((i=0; i<$FILE_DUPLICATION_FACTOR; i++)); do
  for file in "$FOLDER_OF_CLOUDEVENTS"/*; do
    if [ -f "$file" ]; then
    # Replace new lines and write to kafka
    sed ':a;N;$!ba;s/\n/ /g' $file | \
      kubectl exec -i "$KAFKA_POD_NAME" -n "$KAFKA_NAMESPACE" -- /opt/kafka/bin/kafka-console-producer.sh \
      --bootstrap-server onap-strimzi-kafka-bootstrap.onap:9092 \
      --topic "$KAFKA_TOPIC" \
      --producer-property security.protocol=SASL_PLAINTEXT \
      --producer-property sasl.mechanism=SCRAM-SHA-512 \
      --producer-property "sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${KAFKA_USER}\" password=\"${KAFKA_PASSWORD}\";" \
      --property parse.headers=true \
      --property headers.key.separator=::: \
      --property headers.delimiter=,,, \
      --property parse.key=false || {
        echo "Failed to produce message from file $file"
        exit 1
      }
  fi
  done
  echo "$((i+1))/$FILE_DUPLICATION_FACTOR rounds completed"
done

echo "Message production completed."
