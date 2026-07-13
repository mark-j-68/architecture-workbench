package com.architectureworkbench.discovery;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Discovers Spring Data repositories and persistence entities without schema inference. */
public class SpringDataDiscoveryPlugin implements DiscoveryPlugin {
    public static final DiscoveryPluginId ID = DiscoveryPluginId.of("spring.data");
    private static final Pattern REPOSITORY_BASE = Pattern.compile("\\b(JpaRepository|CrudRepository|PagingAndSortingRepository|MongoRepository)\\s*<\\s*([^,>]+)\\s*,\\s*([^>]+)>");

    @Override
    public DiscoveryPluginMetadata metadata() {
        return new DiscoveryPluginMetadata(ID, "Spring Data Discovery Plugin", "0.2.3", "Framework Plugin",
                List.of("java", "spring-data", "jpa", "mongodb"),
                List.of(DiscoveryPluginCapability.DETECT_SPRING_DATA), SpringApplicationDiscoveryPlugin.dependencies(), true);
    }

    @Override
    public DiscoveryPluginResult discover(DiscoveryInput input, DiscoveryExecutionContext context) {
        Instant started = Instant.now();
        List<DiscoveryEvidence> evidence = new ArrayList<>();
        List<DiscoveryObservation> observations = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (SpringDiscoverySupport.Source source : SpringDiscoverySupport.sources(input.rootDirectory(), diagnostics))
            inspect(source, evidence, observations);
        return DiscoveryPluginResult.succeeded(ID, new DiscoveryOutput(evidence, observations, diagnostics), Duration.between(started, Instant.now()));
    }

    private static void inspect(SpringDiscoverySupport.Source source, List<DiscoveryEvidence> evidence,
                                List<DiscoveryObservation> observations) {
        SpringDiscoverySupport.annotation(source.annotations(), "Entity").ifPresent(annotation -> {
            DiscoveryEvidence entity = SpringDiscoverySupport.evidence(ID, "spring-data-entity", source, source.qualifiedName(),
                    annotation.line(), DiscoveryConfidence.observedFact("@Entity is explicitly present."), true,
                    SpringDiscoverySupport.details("annotation", "Entity", "frameworkMarker", "@Entity", "symbol", source.className(),
                            "entityType", source.className()));
            evidence.add(entity);
            observations.add(SpringDiscoverySupport.observation(ID, "spring-data-entity-declared",
                    "Class " + source.className() + " is annotated with @Entity.", entity));
            SpringDiscoverySupport.annotation(source.annotations(), "Table").ifPresent(table -> {
                String tableName = SpringDiscoverySupport.firstString(SpringDiscoverySupport.namedValue(table.arguments(), "name"));
                if (tableName.isBlank()) tableName = SpringDiscoverySupport.firstString(table.arguments());
                DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-data-table-mapping", source,
                        source.className() + "->" + tableName, table.line(), DiscoveryConfidence.observedFact("@Table is explicitly present."), true,
                        SpringDiscoverySupport.details("annotation", "Table", "frameworkMarker", "@Table", "symbol", source.className(),
                                "entityType", source.className(), "tableName", tableName));
                evidence.add(item);
                observations.add(SpringDiscoverySupport.observation(ID, "spring-data-table-mapped",
                        source.className() + " is explicitly mapped to table " + tableName + ".", item));
            });
            for (SpringDiscoverySupport.Field field : source.fields())
                SpringDiscoverySupport.annotation(field.annotations(), "Id").ifPresent(id -> {
                    DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-data-entity-id", source,
                            source.className() + "." + field.name(), id.line(), DiscoveryConfidence.observedFact("@Id is explicitly present."), true,
                            SpringDiscoverySupport.details("annotation", "Id", "frameworkMarker", "@Id", "symbol", source.className() + "." + field.name(),
                                    "entityType", source.className(), "idField", field.name(), "idType", SpringDiscoverySupport.simpleType(field.type())));
                    evidence.add(item);
                    observations.add(SpringDiscoverySupport.observation(ID, "spring-data-entity-id-declared",
                            source.className() + "." + field.name() + " is the entity identifier.", item));
                });
        });

        Matcher repository = REPOSITORY_BASE.matcher(source.header());
        if (source.kind().equals("interface") && repository.find()) {
            String base = repository.group(1);
            String entityType = SpringDiscoverySupport.simpleType(repository.group(2));
            String identifierType = SpringDiscoverySupport.simpleType(repository.group(3));
            DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-data-repository", source,
                    source.qualifiedName(), source.typeLine(), DiscoveryConfidence.observedFact("Spring Data base repository interface is explicitly extended."), true,
                    SpringDiscoverySupport.details("annotation", "", "frameworkMarker", base, "symbol", source.className(),
                            "repositoryBase", base, "entityType", entityType, "identifierType", identifierType));
            evidence.add(item);
            observations.add(SpringDiscoverySupport.observation(ID, "spring-data-repository-declared",
                    source.className() + " manages " + entityType + " using " + identifierType + " identifiers.", item));
            DiscoveryEvidence association = SpringDiscoverySupport.evidence(ID, "spring-data-repository-entity-association", source,
                    source.className() + "->" + entityType, source.typeLine(),
                    DiscoveryConfidence.high("Repository-to-entity association follows from explicit generic type arguments."), false,
                    SpringDiscoverySupport.details("annotation", "", "frameworkMarker", base, "symbol", source.className(),
                            "repositoryType", source.className(), "entityType", entityType, "identifierType", identifierType));
            evidence.add(association);
            observations.add(SpringDiscoverySupport.observation(ID, "spring-data-repository-manages-entity",
                    source.className() + " is associated with entity " + entityType + ".", association));
        }
        for (SpringDiscoverySupport.Method method : source.methods())
            SpringDiscoverySupport.annotation(method.annotations(), "Query").ifPresent(query -> {
                String value = SpringDiscoverySupport.firstString(query.arguments());
                boolean dynamic = SpringDiscoverySupport.dynamic(value);
                DiscoveryEvidence item = SpringDiscoverySupport.evidence(ID, "spring-data-explicit-query", source,
                        method.symbol(source), query.line(), dynamic
                                ? DiscoveryConfidence.inferred(0.7, "Query contains a dynamic expression.")
                                : DiscoveryConfidence.observedFact("@Query is explicitly present."), !dynamic,
                        SpringDiscoverySupport.details("annotation", "Query", "frameworkMarker", "@Query", "methodName", method.name(),
                                "symbol", method.symbol(source), "query", value, "uncertainty", dynamic ? "dynamic-expression" : ""));
                evidence.add(item);
                observations.add(SpringDiscoverySupport.observation(ID, "spring-data-query-declared",
                        method.symbol(source) + " declares an explicit repository query.", item));
            });
    }
}
