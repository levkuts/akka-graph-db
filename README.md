## Akka Graph Database

## Running

1 Publish local docker container

```
sbt docker:publishLocal
```

2 Start cluster

```
docker-compose up -d
```

## Try it

1 Create graph node

```
curl --location --request POST 'http://127.0.0.1:8051/graph/nodes' \
--header 'Content-Type: application/json' \
--data-raw '{
	"nodeId": "john",
	"nodeType": "Person",
	"attributes": {
		"name": "John",
		"age": 38
	}
}'

curl --location --request POST 'http://127.0.0.1:8051/graph/nodes' \
--header 'Content-Type: application/json' \
--data-raw '{
	"nodeId": "nick",
	"nodeType": "Person",
	"attributes": {
		"name": "Nick",
		"age": 30
	}
}'

curl --location --request POST 'http://127.0.0.1:8051/graph/nodes' \
--header 'Content-Type: application/json' \
--data-raw '{
	"nodeId": "kate",
	"nodeType": "Person",
	"attributes": {
		"name": "Kate",
		"age": 31
	}
}'
```

2 Create relation

```
curl --location --request PUT 'http://127.0.0.1:8051/graph/nodes/nick/relations/friendof' \
--header 'Content-Type: application/json' \
--data-raw '{
    "nodeId": "kate"
}'

curl --location --request PUT 'http://127.0.0.1:8051/graph/nodes/nick/relations/friendof' \
--header 'Content-Type: application/json' \
--data-raw '{
    "nodeId": "john"
}'
```

3 Get node attributes

```
curl --location --request GET 'http://127.0.0.1:8052/graph/nodes/nick/attributes' \
--data-raw ''
```

4 Get node relations

```
curl --location --request GET 'http://127.0.0.1:8052/graph/nodes/nick/relations' \
--data-raw ''
```

5 Search nodes by query

```
curl --location --request GET 'http://127.0.0.1:8052/graph/nodes' \
--header 'Content-Type: application/json' \
--data-raw '{
	"query": "OR(EQ(FIELD(\"age\"),NUMBER(38)),EQ(FIELD(\"age\"),NUMBER(31)))"
}'
```