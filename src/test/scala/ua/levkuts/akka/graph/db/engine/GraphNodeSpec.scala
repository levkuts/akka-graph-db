package ua.levkuts.akka.graph.db.engine

import akka.actor.testkit.typed.scaladsl.{LogCapturing, ScalaTestWithActorTestKit}
import akka.persistence.testkit.scaladsl.EventSourcedBehaviorTestKit
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike
import ua.levkuts.akka.graph.db.engine.GraphNode._

class GraphNodeSpec extends ScalaTestWithActorTestKit(
  EventSourcedBehaviorTestKit.config.withFallback(ConfigFactory.load("application-test.conf"))
) with AnyWordSpecLike with BeforeAndAfterEach with LogCapturing {

  private val nodeId = "TestGraphNodeId"
  private val nodeType = "TestGraphNodeType"
  private val graphNodeSourcedTestKit = EventSourcedBehaviorTestKit[GraphNode.Command, GraphNode.Event, GraphNode.State](
    system,
    GraphNode(GraphNode.persistenceId("GraphNode", nodeId), Set("tag1"))
  )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    graphNodeSourcedTestKit.clear()
  }

  "GraphNode" must {
    "be created with node id, type and attributes" in {
      val attributes = Map(
        "name1" -> StrAttributeValue("value1"),
        "name2" -> StrAttributeValue("value2"),
        "name3" -> StrAttributeValue("value3")
      )
      val initializeResult = graphNodeSourcedTestKit.runCommand(Initialize(nodeId, nodeType, attributes, _))
      initializeResult.event shouldBe Initialized(nodeId, nodeType, attributes)
      initializeResult.reply shouldBe InitializationSuccess(nodeId, nodeType, attributes)
      initializeResult.stateOfType[InitializedState].nodeId shouldBe nodeId
      initializeResult.stateOfType[InitializedState].nodeType shouldBe nodeType
      initializeResult.stateOfType[InitializedState].attributes shouldBe attributes
    }

    "handle add attribute" in {
      val attributeName = "name1"
      val attributeValue = StrAttributeValue("value1")
      graphNodeSourcedTestKit.runCommand(Initialize(nodeId, nodeType, Map(), _))
      val addAttributeResult = graphNodeSourcedTestKit.runCommand(AddAttribute(attributeName, attributeValue, _))
      addAttributeResult.event shouldBe AttributeAdded(nodeId, attributeName, attributeValue)
      addAttributeResult.reply shouldBe AddAttributeSuccess(nodeId, attributeName, attributeValue)
      addAttributeResult.stateOfType[InitializedState].nodeId shouldBe nodeId
      addAttributeResult.stateOfType[InitializedState].nodeType shouldBe nodeType
      addAttributeResult.stateOfType[InitializedState].attributes shouldBe Map(attributeName -> attributeValue)
    }

    "handle delete attribute" in {
      val attributeName = "name1"
      val attributeValue = StrAttributeValue("value1")
      graphNodeSourcedTestKit.runCommand(Initialize(nodeId, nodeType, Map(), _))
      graphNodeSourcedTestKit.runCommand(AddAttribute(attributeName, attributeValue, _))
      val deleteAttributeResult = graphNodeSourcedTestKit.runCommand(DeleteAttribute(attributeName, _))
      deleteAttributeResult.event shouldBe AttributeDeleted(nodeId, attributeName)
      deleteAttributeResult.reply shouldBe DeleteAttributeSuccess(nodeId, attributeName)
      deleteAttributeResult.stateOfType[InitializedState].nodeId shouldBe nodeId
      deleteAttributeResult.stateOfType[InitializedState].nodeType shouldBe nodeType
      deleteAttributeResult.stateOfType[InitializedState].attributes shouldBe Map()
    }

    "handle establish relation" in {
      graphNodeSourcedTestKit.runCommand(Initialize(nodeId, nodeType, Map(), _))
      val relation = Relation("relation1", To, "toNodeId")
      val establishRelationResult = graphNodeSourcedTestKit.runCommand(EstablishRelation(relation, _))
      establishRelationResult.event shouldBe RelationEstablished(nodeId, relation)
      establishRelationResult.reply shouldBe EstablishRelationSuccess(nodeId, relation)
      establishRelationResult.stateOfType[InitializedState].nodeId shouldBe nodeId
      establishRelationResult.stateOfType[InitializedState].nodeType shouldBe nodeType
      establishRelationResult.stateOfType[InitializedState].attributes shouldBe Map()
      establishRelationResult.stateOfType[InitializedState].relations shouldBe Map("relation1" -> Map(To -> Set("toNodeId")))
    }
  }

}
