package com.architectureworkbench.api;
import com.architectureworkbench.api.ApiDtos.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/workspaces/{workspaceId}/products")
final class ProductController {
 private final ProductCompositionService service;
 private final com.architectureworkbench.workspace.WorkspaceService workspaces;
 ProductController(ProductCompositionService service,com.architectureworkbench.workspace.WorkspaceService workspaces){this.service=service;this.workspaces=workspaces;}
 private void requireWorkspace(String id){workspaces.getWorkspaceGraph(com.architectureworkbench.workspace.WorkspaceId.of(id));}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) ProductView create(@PathVariable("workspaceId") String workspaceId,@RequestBody CreateProductRequest request){requireWorkspace(workspaceId);return service.create(workspaceId,request);}
 @GetMapping List<ProductView> list(@PathVariable("workspaceId") String workspaceId){requireWorkspace(workspaceId);return service.list(workspaceId);}
 @GetMapping("/{productId}") ProductView get(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId){return service.get(workspaceId,productId);}
 @PutMapping("/{productId}") ProductView update(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId,@RequestBody UpdateProductMetadataRequest request){return service.update(workspaceId,productId,request);}
 @DeleteMapping("/{productId}") @ResponseStatus(HttpStatus.NO_CONTENT) void archive(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId){service.archive(workspaceId,productId);}
 @PostMapping("/{productId}/repositories") @ResponseStatus(HttpStatus.CREATED) ProductView addRepository(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId,@RequestBody AddProductRepositoryRequest request){return service.addRepository(workspaceId,productId,request);}
 @DeleteMapping("/{productId}/repositories/{repositoryId}") ProductView removeRepository(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId,@PathVariable("repositoryId") String repositoryId,@RequestParam(name="actorRef",defaultValue="api-user") String actorRef){return service.removeRepository(workspaceId,productId,repositoryId,actorRef);}
 @PostMapping("/{productId}/repositories/{repositoryId}/discovery-runs") ProductView attachRun(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId,@PathVariable("repositoryId") String repositoryId,@RequestBody AttachDiscoveryRunRequest request){return service.attachRun(workspaceId,productId,repositoryId,request);}
 @PostMapping("/{productId}/modules") @ResponseStatus(HttpStatus.CREATED) ProductView createModule(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId,@RequestBody CreateProductModuleRequest request){return service.createModule(workspaceId,productId,request);}
 @PostMapping("/{productId}/modules/{moduleId}/repositories/{repositoryId}") ProductView assign(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId,@PathVariable("moduleId") String moduleId,@PathVariable("repositoryId") String repositoryId,@RequestParam(name="actorRef",defaultValue="api-user") String actorRef){return service.assign(workspaceId,productId,moduleId,repositoryId,actorRef);}
 @PostMapping("/{productId}/compose") ProductCompositionView compose(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId,@RequestParam(name="actorRef",defaultValue="api-user") String actorRef){return service.compose(workspaceId,productId,actorRef);}
 @GetMapping("/{productId}/composition") ProductCompositionView composition(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId){return service.composition(workspaceId,productId);}
 @GetMapping("/{productId}/relationships") List<ProductRelationshipView> relationships(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId){return service.composition(workspaceId,productId).relationships();}
 @GetMapping("/{productId}/metrics") ProductCompositionMetrics metrics(@PathVariable("workspaceId") String workspaceId,@PathVariable("productId") String productId){return service.composition(workspaceId,productId).metrics();}
}
