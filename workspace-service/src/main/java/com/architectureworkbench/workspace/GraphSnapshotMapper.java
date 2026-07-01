package com.architectureworkbench.workspace;

import com.architectureworkbench.knowledgegraph.ArchitectureElement;
import com.architectureworkbench.knowledgegraph.ArchitectureElementFactory;
import com.architectureworkbench.knowledgegraph.ArchitectureKnowledgeGraph;
import com.architectureworkbench.knowledgegraph.ElementId;
import com.architectureworkbench.knowledgegraph.Relationship;
import java.lang.reflect.Method;
import java.time.Instant;

public class GraphSnapshotMapper {
    private final ArchitectureElementFactory elementFactory = new ArchitectureElementFactory();

    public GraphSnapshot exportSnapshot(ArchitectureKnowledgeGraph graph) {
        return new GraphSnapshot(
                graph.graphId(),
                Instant.now(),
                graph.elements().stream()
                        .map(element -> new GraphSnapshot.ElementSnapshot(
                                element.id().value(),
                                element.type(),
                                element.name(),
                                element.description(),
                                element.attributes()))
                        .toList(),
                graph.relationships().stream()
                        .map(relationship -> new GraphSnapshot.RelationshipSnapshot(
                                relationship.id(),
                                relationship.sourceId().value(),
                                relationship.targetId().value(),
                                relationship.type(),
                                relationship.label(),
                                relationship.attributes()))
                        .toList()
        );
    }

    public ArchitectureKnowledgeGraph importSnapshot(GraphSnapshot snapshot) {
        ArchitectureKnowledgeGraph graph = new ArchitectureKnowledgeGraph(snapshot.graphId());
        snapshot.elements().forEach(element -> addElement(graph, elementFactory.create(
                element.type(),
                element.name(),
                element.description(),
                element.attributes()
        ), ElementId.of(element.id())));
        snapshot.relationships().forEach(relationship -> addRelationship(graph, new Relationship(
                relationship.id(),
                ElementId.of(relationship.sourceId()),
                ElementId.of(relationship.targetId()),
                relationship.type(),
                relationship.label(),
                relationship.attributes(),
                null
        )));
        return graph;
    }

    private static void addElement(ArchitectureKnowledgeGraph graph, ArchitectureElement element, ElementId id) {
        ArchitectureElement restored = new ArchitectureElementFactory().create(element.type(), element.name(), element.description(), element.attributes());
        ArchitectureElement withSnapshotId = instantiateWithId(restored, id);
        invoke(graph, "addElement", ArchitectureElement.class, withSnapshotId);
    }

    private static void addRelationship(ArchitectureKnowledgeGraph graph, Relationship relationship) {
        invoke(graph, "addRelationship", Relationship.class, relationship);
    }

    private static ArchitectureElement instantiateWithId(ArchitectureElement element, ElementId id) {
        try {
            return element.getClass()
                    .getConstructor(ElementId.class, String.class, String.class, java.util.Map.class)
                    .newInstance(id, element.name(), element.description(), element.attributes());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to restore graph element from snapshot.", e);
        }
    }

    private static <T> void invoke(ArchitectureKnowledgeGraph graph, String methodName, Class<T> parameterType, T value) {
        try {
            Method method = ArchitectureKnowledgeGraph.class.getDeclaredMethod(methodName, parameterType);
            method.setAccessible(true);
            method.invoke(graph, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to restore graph snapshot.", e);
        }
    }
}
