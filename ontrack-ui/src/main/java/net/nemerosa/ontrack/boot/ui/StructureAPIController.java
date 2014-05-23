package net.nemerosa.ontrack.boot.ui;

import net.nemerosa.ontrack.model.form.Form;
import net.nemerosa.ontrack.model.security.*;
import net.nemerosa.ontrack.model.structure.*;
import net.nemerosa.ontrack.ui.controller.AbstractResourceController;
import net.nemerosa.ontrack.ui.resource.Link;
import net.nemerosa.ontrack.ui.resource.Resource;
import net.nemerosa.ontrack.ui.resource.ResourceCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

@RestController
@RequestMapping("/structure")
public class StructureAPIController extends AbstractResourceController implements StructureAPI {

    private final StructureService structureService;
    private final SecurityService securityService;

    @Autowired
    public StructureAPIController(StructureService structureService, SecurityService securityService) {
        this.structureService = structureService;
        this.securityService = securityService;
    }

    @Override
    @RequestMapping(value = "projects", method = RequestMethod.GET)
    public ResourceCollection<Project> getProjectList() {
        return ResourceCollection.of(
                structureService.getProjectList().stream().map(this::toProjectResource),
                uri(on(StructureAPIController.class).getProjectList())
        )
                .with(Link.CREATE, uri(on(StructureAPIController.class).newProject(null)), securityService.isGlobalFunctionGranted(ProjectCreation.class));
    }

    @Override
    @RequestMapping(value = "projects/create", method = RequestMethod.GET)
    public Form newProjectForm() {
        return Project.form();
    }

    @Override
    @RequestMapping(value = "projects/create", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Resource<Project> newProject(@RequestBody NameDescription nameDescription) {
        // Creates a new project instance
        Project project = Project.of(nameDescription);
        // Saves it into the repository
        project = structureService.newProject(project);
        // OK
        return toProjectResource(project);
    }

    @Override
    @RequestMapping(value = "projects/{projectId}", method = RequestMethod.GET)
    public Resource<Project> getProject(@PathVariable ID projectId) {
        // Gets from the repository
        Project project = structureService.getProject(projectId);
        // As resource
        return toProjectResourceWithActions(project);
    }

    @Override
    @RequestMapping(value = "projects/{projectId}/update", method = RequestMethod.GET)
    public Form saveProjectForm(@PathVariable ID projectId) {
        return structureService.getProject(projectId).asForm();
    }

    @Override
    @RequestMapping(value = "projects/{projectId}/update", method = RequestMethod.PUT)
    public Resource<Project> saveProject(@PathVariable ID projectId, @RequestBody NameDescription nameDescription) {
        // Gets from the repository
        Project project = structureService.getProject(projectId);
        // Updates
        project = project.update(nameDescription);
        // Saves in repository
        structureService.saveProject(project);
        // As resource
        return toProjectResource(project);
    }

    @Override
    @RequestMapping(value = "projects/{projectId}/branches", method = RequestMethod.GET)
    public ResourceCollection<Branch> getBranchListForProject(@PathVariable ID projectId) {
        return ResourceCollection.of(
                structureService.getBranchesForProject(projectId).stream().map(this::toBranchResource),
                uri(on(StructureAPIController.class).getBranchListForProject(projectId))
        )
                // Create
                .with(
                        Link.CREATE,
                        uri(on(StructureAPIController.class).newBranch(projectId, null)),
                        securityService.isProjectFunctionGranted(projectId.getValue(), BranchCreate.class)
                )
                ;
    }

    @Override
    @RequestMapping(value = "projects/{projectId}/branches/create", method = RequestMethod.GET)
    public Form newBranchForm(@PathVariable ID projectId) {
        // Checks the project exists
        structureService.getProject(projectId);
        // Returns the form
        return Branch.form();
    }

    @Override
    @RequestMapping(value = "projects/{projectId}/branches/create", method = RequestMethod.POST)
    public Resource<Branch> newBranch(@PathVariable ID projectId, @RequestBody NameDescription nameDescription) {
        // Gets the project
        Project project = structureService.getProject(projectId);
        // Creates a new branch instance
        Branch branch = Branch.of(project, nameDescription);
        // Saves it into the repository
        branch = structureService.newBranch(branch);
        // OK
        return toBranchResource(branch);
    }

    @Override
    @RequestMapping(value = "branches/{branchId}", method = RequestMethod.GET)
    public Resource<Branch> getBranch(@PathVariable ID branchId) {
        return toBranchResourceWithActions(
                structureService.getBranch(branchId)
        );
    }

    @Override
    @RequestMapping(value = "branches/{branchId}/view", method = RequestMethod.GET)
    public BranchBuildView buildView(@PathVariable ID branchId) {
        // TODO Defines the filter for the service
        return structureService.getBranchBuildView(branchId);
    }

    // Builds

    @Override
    @RequestMapping(value = "branches/{branchId}/builds/create", method = RequestMethod.GET)
    public Form newBuildForm(@PathVariable ID branchId) {
        // Checks the branch does exist
        structureService.getBranch(branchId);
        // Returns the form
        return Build.form();
    }

    @Override
    @RequestMapping(value = "branches/{branchId}/builds/create", method = RequestMethod.POST)
    public Resource<Build> newBuild(@PathVariable ID branchId, @RequestBody NameDescription nameDescription) {
        // Gets the holding branch
        Branch branch = structureService.getBranch(branchId);
        // Build signature
        Signature signature = securityService.getCurrentSignature();
        // Creates a new build
        Build build = Build.of(branch, nameDescription, signature);
        // Saves it into the repository
        build = structureService.newBuild(build);
        // OK
        return toBuildResource(build);
    }

    @Override
    @RequestMapping(value = "builds/{buildId}", method = RequestMethod.GET)
    public Resource<Build> getBuild(@PathVariable ID buildId) {
        return toBuildResourceWithActions(
                structureService.getBuild(buildId)
        );
    }

    // Promotion levels

    @RequestMapping(value = "branches/{branchId}/promotionLevels", method = RequestMethod.GET)
    @Override
    public ResourceCollection<PromotionLevel> getPromotionLevelListForBranch(@PathVariable ID branchId) {
        Branch branch = structureService.getBranch(branchId);
        return ResourceCollection.of(
                structureService.getPromotionLevelListForBranch(branchId).stream().map(this::toPromotionLevelResource),
                uri(on(StructureAPIController.class).getPromotionLevelListForBranch(branchId))
        )
                // Create
                .with(
                        Link.CREATE,
                        uri(on(StructureAPIController.class).newPromotionLevelForm(branchId)),
                        securityService.isProjectFunctionGranted(branch.getProject().id(), PromotionLevelCreate.class)
                )
                ;
    }

    @Override
    @RequestMapping(value = "branches/{branchId}/promotionLevels/create", method = RequestMethod.GET)
    public Form newPromotionLevelForm(@PathVariable ID branchId) {
        structureService.getBranch(branchId);
        return PromotionLevel.form();
    }

    @Override
    @RequestMapping(value = "branches/{branchId}/promotionLevels/create", method = RequestMethod.POST)
    public Resource<PromotionLevel> newPromotionLevel(@PathVariable ID branchId, @RequestBody NameDescription nameDescription) {
        // Gets the holding branch
        Branch branch = structureService.getBranch(branchId);
        // Creates a new promotion level
        PromotionLevel promotionLevel = PromotionLevel.of(branch, nameDescription);
        // Saves it into the repository
        promotionLevel = structureService.newPromotionLevel(promotionLevel);
        // OK
        return toPromotionLevelResource(promotionLevel);
    }

    @Override
    @RequestMapping(value = "promotionLevels/{promotionLevelId}", method = RequestMethod.GET)
    public Resource<PromotionLevel> getPromotionLevel(@PathVariable ID promotionLevelId) {
        return toPromotionLevelResourceWithActions(
                structureService.getPromotionLevel(promotionLevelId)
        );
    }

    @RequestMapping(value = "promotionLevels/{promotionLevelId}/image", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getPromotionLevelImage_(@PathVariable ID promotionLevelId) {
        // Gets the file
        Document file = getPromotionLevelImage(promotionLevelId);
        if (file == null) {
            return new ResponseEntity<>(new byte[0], HttpStatus.NO_CONTENT);
        } else {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentLength(file.getContent().length);
            responseHeaders.setContentType(MediaType.parseMediaType(file.getType()));
            return new ResponseEntity<>(file.getContent(), responseHeaders, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "promotionLevels/{promotionLevelId}/image", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void setPromotionLevelImage(@PathVariable ID promotionLevelId, @RequestParam MultipartFile file) throws IOException {
        setPromotionLevelImage(
                promotionLevelId,
                new Document(
                        file.getContentType(),
                        file.getBytes()
                )
        );
    }

    @Override
    public Document getPromotionLevelImage(ID promotionLevelId) {
        return structureService.getPromotionLevelImage(promotionLevelId);
    }

    @Override
    public void setPromotionLevelImage(ID promotionLevelId, Document document) {
        structureService.setPromotionLevelImage(promotionLevelId, document);
    }

    // Validation stamps

    @RequestMapping(value = "branches/{branchId}/validationStamps", method = RequestMethod.GET)
    @Override
    public ResourceCollection<ValidationStamp> getValidationStampListForBranch(@PathVariable ID branchId) {
        Branch branch = structureService.getBranch(branchId);
        return ResourceCollection.of(
                structureService.getValidationStampListForBranch(branchId).stream().map(this::toValidationStampResource),
                uri(on(StructureAPIController.class).getValidationStampListForBranch(branchId))
        )
                // Create
                .with(
                        Link.CREATE,
                        uri(on(StructureAPIController.class).newValidationStampForm(branchId)),
                        securityService.isProjectFunctionGranted(branch.getProject().id(), ValidationStampCreate.class)
                )
                ;
    }

    @Override
    @RequestMapping(value = "branches/{branchId}/validationStamps/create", method = RequestMethod.GET)
    public Form newValidationStampForm(@PathVariable ID branchId) {
        structureService.getBranch(branchId);
        return ValidationStamp.form();
    }

    @Override
    @RequestMapping(value = "branches/{branchId}/validationStamps/create", method = RequestMethod.POST)
    public Resource<ValidationStamp> newValidationStamp(@PathVariable ID branchId, @RequestBody NameDescription nameDescription) {
        // Gets the holding branch
        Branch branch = structureService.getBranch(branchId);
        // Creates a new promotion level
        ValidationStamp validationStamp = ValidationStamp.of(branch, nameDescription);
        // Saves it into the repository
        validationStamp = structureService.newValidationStamp(validationStamp);
        // OK
        return toValidationStampResource(validationStamp);
    }

    @Override
    @RequestMapping(value = "validationStamps/{validationStampId}", method = RequestMethod.GET)
    public Resource<ValidationStamp> getValidationStamp(@PathVariable ID validationStampId) {
        return toValidationStampResourceWithActions(
                structureService.getValidationStamp(validationStampId)
        );
    }

    @RequestMapping(value = "validationStamps/{validationStampId}/image", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getValidationStampImage_(@PathVariable ID validationStampId) {
        // Gets the file
        Document file = getValidationStampImage(validationStampId);
        if (file == null) {
            return new ResponseEntity<>(new byte[0], HttpStatus.NO_CONTENT);
        } else {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentLength(file.getContent().length);
            responseHeaders.setContentType(MediaType.parseMediaType(file.getType()));
            return new ResponseEntity<>(file.getContent(), responseHeaders, HttpStatus.OK);
        }
    }

    @RequestMapping(value = "validationStamps/{validationStampId}/image", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void setValidationStampImage(@PathVariable ID validationStampId, @RequestParam MultipartFile file) throws IOException {
        setValidationStampImage(
                validationStampId,
                new Document(
                        file.getContentType(),
                        file.getBytes()
                )
        );
    }

    @Override
    public Document getValidationStampImage(ID validationStampId) {
        return structureService.getValidationStampImage(validationStampId);
    }

    @Override
    public void setValidationStampImage(ID validationStampId, Document document) {
        structureService.setValidationStampImage(validationStampId, document);
    }

    // Resource assemblers

    private Resource<Project> toProjectResourceWithActions(Project project) {
        return toProjectResource(project)
                // Update
                .with(
                        Link.UPDATE,
                        uri(on(StructureAPIController.class).saveProject(project.getId(), null)),
                        securityService.isProjectFunctionGranted(project.id(), ProjectEdit.class)
                )
                        // Branch list
                .with("branches", uri(on(StructureAPIController.class).getBranchListForProject(project.getId())))
                ;
        // TODO Delete link
        // TODO View link
    }

    private Resource<Project> toProjectResource(Project project) {
        return Resource.of(
                project,
                uri(on(StructureAPIController.class).getProject(project.getId()))
        );
    }

    private Resource<Branch> toBranchResourceWithActions(Branch branch) {
        return toBranchResource(branch)
                // TODO Update link (with authorisation)
                // TODO Delete link
                // TODO View link
                // TODO Builds link
                // Build creation
                .with(
                        "createBuild",
                        uri(on(StructureAPIController.class).newBuild(branch.getId(), null)),
                        securityService.isProjectFunctionGranted(branch.getProject().id(), BuildCreate.class)
                )
                        // Promotion level creation
                .with(
                        "createPromotionLevel",
                        uri(on(StructureAPIController.class).newPromotionLevelForm(branch.getId())),
                        securityService.isProjectFunctionGranted(branch.getProject().id(), PromotionLevelCreate.class)
                )
                        // Promotion level list
                .with(
                        "promotionLevels",
                        uri(on(StructureAPIController.class).getPromotionLevelListForBranch(branch.getId()))
                )
                        // Validation stamp creation
                .with(
                        "createValidationStamp",
                        uri(on(StructureAPIController.class).newValidationStampForm(branch.getId())),
                        securityService.isProjectFunctionGranted(branch.getProject().id(), ValidationStampCreate.class)
                )
                        // Validation stamp list
                .with(
                        "validationStamps",
                        uri(on(StructureAPIController.class).getValidationStampListForBranch(branch.getId()))
                )
                ;
    }

    private Resource<Branch> toBranchResource(Branch branch) {
        return Resource.of(
                branch,
                uri(on(StructureAPIController.class).getBranch(branch.getId()))
        )
                // Branch's project
                .with("projectLink", uri(on(StructureAPIController.class).getProject(branch.getProject().getId())))
                ;
    }

    private Resource<Build> toBuildResource(Build build) {
        return Resource.of(
                build,
                uri(on(getClass()).getBuild(build.getId()))
        );
    }

    private Resource<Build> toBuildResourceWithActions(Build build) {
        return toBuildResource(build)
                // TODO Update
                // TODO Delete
                ;
    }

    private Resource<PromotionLevel> toPromotionLevelResourceWithActions(PromotionLevel promotionLevel) {
        return toPromotionLevelResource(promotionLevel);
        // TODO Update
        // TODO Delete
        // TODO Next promotion level
        // TODO Previous promotion level
    }

    private Resource<PromotionLevel> toPromotionLevelResource(PromotionLevel promotionLevel) {
        return Resource.of(
                promotionLevel,
                uri(on(StructureAPIController.class).getPromotionLevel(promotionLevel.getId()))
        )
                // Branch link
                .with("branchLink", uri(on(StructureAPIController.class).getBranch(promotionLevel.getBranch().getId())))
                        // Project link
                .with("projectLink", uri(on(StructureAPIController.class).getProject(promotionLevel.getBranch().getProject().getId())))
                        // Image link
                .with("imageLink", uri(on(StructureAPIController.class).getPromotionLevelImage_(promotionLevel.getId())))
                ;
    }

    private Resource<ValidationStamp> toValidationStampResourceWithActions(ValidationStamp validationStamp) {
        return toValidationStampResource(validationStamp);
        // TODO Update
        // TODO Delete
        // TODO Next validation stamp
        // TODO Previous validation stamp
    }

    private Resource<ValidationStamp> toValidationStampResource(ValidationStamp validationStamp) {
        return Resource.of(
                validationStamp,
                uri(on(StructureAPIController.class).getValidationStamp(validationStamp.getId()))
        )
                // Branch link
                .with("branchLink", uri(on(StructureAPIController.class).getBranch(validationStamp.getBranch().getId())))
                        // Project link
                .with("projectLink", uri(on(StructureAPIController.class).getProject(validationStamp.getBranch().getProject().getId())))
                        // Image link
                .with("imageLink", uri(on(StructureAPIController.class).getValidationStampImage_(validationStamp.getId())))
                ;
    }
}
