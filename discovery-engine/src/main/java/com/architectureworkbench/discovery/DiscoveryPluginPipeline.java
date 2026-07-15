package com.architectureworkbench.discovery;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Executes the Release 0.2 deterministic plugins with isolated failures and retained prior outputs. */
public class DiscoveryPluginPipeline {
    private final List<DiscoveryPlugin> plugins;

    public DiscoveryPluginPipeline() {
        this(defaultPlugins());
    }

    public DiscoveryPluginPipeline(List<DiscoveryPlugin> plugins) {
        this.plugins = List.copyOf(plugins);
    }

    public List<DiscoveryPluginExecutionRecord> execute(DiscoveryExecutionContext context) {
        List<DiscoveryPluginExecutionRecord> executions = new ArrayList<>();
        List<DiscoveryOutput> outputs = new ArrayList<>();
        Map<DiscoveryPluginId, DiscoveryPluginStatus> statuses = new LinkedHashMap<>();
        for (DiscoveryPlugin plugin : plugins) {
            Instant started = Instant.now();
            DiscoveryPluginResult result;
            List<String> missingRequired = plugin.metadata().dependencies().stream().filter(DiscoveryPluginDependency::required)
                    .filter(dependency -> unavailable(statuses.get(dependency.pluginId()))).map(dependency -> dependency.pluginId().value()).toList();
            if (!missingRequired.isEmpty()) {
                result = new DiscoveryPluginResult(plugin.metadata().id(), DiscoveryPluginStatus.SKIPPED,
                        new DiscoveryOutput(List.of(), List.of(), List.of("Required plugin dependency unavailable: " + String.join(", ", missingRequired))),
                        Duration.between(started, Instant.now()), "Required dependency unavailable");
            } else {
                try {
                    result = plugin.discover(DiscoveryInput.root(context.rootDirectory()).withPriorOutputs(outputs), context);
                    List<String> missingOptional = plugin.metadata().dependencies().stream().filter(dependency -> !dependency.required())
                            .filter(dependency -> unavailable(statuses.get(dependency.pluginId()))).map(dependency -> dependency.pluginId().value()).toList();
                    if (!missingOptional.isEmpty()) {
                        List<String> diagnostics = new ArrayList<>(result.output().diagnostics());
                        diagnostics.add("Optional plugin dependency unavailable: " + String.join(", ", missingOptional));
                        result = new DiscoveryPluginResult(result.pluginId(), result.status(),
                                new DiscoveryOutput(result.output().evidence(), result.output().observations(), diagnostics),
                                result.elapsed(), result.errorMessage());
                    }
                } catch (Exception exception) {
                    result = DiscoveryPluginResult.failed(plugin.metadata().id(), Duration.between(started, Instant.now()),
                            exception.getClass().getSimpleName() + ": " + String.valueOf(exception.getMessage()));
                }
            }
            Instant completed = Instant.now();
            executions.add(new DiscoveryPluginExecutionRecord(plugin.metadata(), result, started, completed));
            statuses.put(plugin.metadata().id(), result.status());
            if (result.status() != DiscoveryPluginStatus.FAILED) outputs.add(result.output());
        }
        return List.copyOf(executions);
    }

    private static boolean unavailable(DiscoveryPluginStatus status) {
        return status == null || status == DiscoveryPluginStatus.FAILED || status == DiscoveryPluginStatus.SKIPPED;
    }

    public List<DiscoveryPlugin> plugins() {
        return plugins;
    }

    private static List<DiscoveryPlugin> defaultPlugins() {
        return List.of(
                new RepositoryDiscoveryPlugin(), new MavenDiscoveryPlugin(), new JavaStructureDiscoveryPlugin(),
                new PackageDependencyDiscoveryPlugin(), new SpringApplicationDiscoveryPlugin(), new SpringWebDiscoveryPlugin(),
                new SpringComponentDiscoveryPlugin(), new SpringDataDiscoveryPlugin(), new SpringTransactionDiscoveryPlugin(),
                new SpringMessagingDiscoveryPlugin(), new OpenApiContractDiscoveryPlugin(), new EventContractDiscoveryPlugin(),
                new CommandContractDiscoveryPlugin(), new MessagingTopologyDiscoveryPlugin(), new ContractVersionDiscoveryPlugin(),
                new ContractOwnershipEvidencePlugin(), new PackageCycleAnalysisPlugin(), new ModuleDependencyAnalysisPlugin(),
                new LayerStructureAnalysisPlugin(), new ComponentDependencyAnalysisPlugin(), new ContractVersionAnalysisPlugin(),
                new MessagingTopologyAnalysisPlugin(), new DependencyMetricsPlugin()
        );
    }
}
