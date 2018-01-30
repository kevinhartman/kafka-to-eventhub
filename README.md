# Notes
- At-least-once semantics are guaranteed, meaning each event in Kafka will be placed into the EventHub topic at least once, but not exactly once (duplicates are possible). Consequently, downstream EventHub topic consumers must be able to handle duplicates.
