DROP DATABASE IF EXISTS "graph-db";
CREATE DATABASE "graph-db";
\c "graph-db";

DROP TABLE IF EXISTS public."GRAPH_NODES";
CREATE TABLE IF NOT EXISTS public."GRAPH_NODES" (
    "NODE_ID" character varying COLLATE pg_catalog."default" NOT NULL,
    "NODE_TYPE" character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT "GRAPH_NODES_pkey" PRIMARY KEY ("NODE_ID")
);

DROP TABLE IF EXISTS public."GRAPH_NODE_ATTRIBUTES";
CREATE TABLE IF NOT EXISTS public."GRAPH_NODE_ATTRIBUTES" (
    "NODE_ID" character varying COLLATE pg_catalog."default" NOT NULL,
    "ATTRIBUTE_NAME" character varying COLLATE pg_catalog."default" NOT NULL,
    "ATTRIBUTE_VALUE" character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT "GRAPH_NODE_ATTRIBUTES_PK" PRIMARY KEY ("NODE_ID", "ATTRIBUTE_NAME")
);

DROP TABLE IF EXISTS public."GRAPH_NODE_RELATIONS";
CREATE TABLE IF NOT EXISTS public."GRAPH_NODE_RELATIONS"
(
    "SOURCE_NODE_ID" character varying COLLATE pg_catalog."default" NOT NULL,
    "TARGET_NODE_ID" character varying COLLATE pg_catalog."default" NOT NULL,
    "RELATION_NAME" character varying COLLATE pg_catalog."default" NOT NULL,
    CONSTRAINT "GRAPH_NODE_RELATIONS_PK" PRIMARY KEY ("SOURCE_NODE_ID", "TARGET_NODE_ID", "RELATION_NAME")
);