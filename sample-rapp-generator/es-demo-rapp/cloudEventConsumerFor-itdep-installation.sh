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
GROUP_ID="${4:-topology-inventory-ingestion-consumer}"
OFFSET_RESET="${5:-earliest}"

echo "Consuming messages from Kafka topic '$KAFKA_TOPIC'..."

kubectl exec -it "$KAFKA_POD_NAME" -n "$KAFKA_NAMESPACE" -- /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server onap-strimzi-kafka-bootstrap.onap:9092 \
  --topic "$KAFKA_TOPIC" \
  --consumer-property security.protocol=SASL_PLAINTEXT \
  --consumer-property sasl.mechanism=SCRAM-SHA-512 \
  --consumer-property "sasl.jaas.config=org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${KAFKA_USER}\" password=\"${KAFKA_PASSWORD}\";" \
  --group "$GROUP_ID" \
  --from-beginning \
  --timeout-ms 60000 \
  --consumer-property auto.offset.reset="$OFFSET_RESET"