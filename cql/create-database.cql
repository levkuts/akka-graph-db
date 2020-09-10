CREATE KEYSPACE IF NOT EXISTS graph_db
WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 }

CREATE TABLE IF NOT EXISTS graph_db.offsets (
   processorId text,
   tag text,
   timeUuidOffset timeuuid,
   PRIMARY KEY (processorId, tag)
 )