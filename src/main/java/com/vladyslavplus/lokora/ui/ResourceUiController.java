package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.common.exception.ResourceHasReservationsException;
import com.vladyslavplus.lokora.common.exception.ResourceNotFoundException;
import com.vladyslavplus.lokora.resource.WorkspaceResourceService;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceCreateRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourcePageResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceStatusChangeRequest;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/resources")
public class ResourceUiController {

    private final WorkspaceResourceService resourceService;

    public ResourceUiController(WorkspaceResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model
    ) {
        WorkspaceResourcePageResponse pageResponse = resourceService.list(page, size);
        model.addAttribute("page", pageResponse);
        return "resources/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        try {
            model.addAttribute("resource", resourceService.getById(id));
            return "resources/detail";
        } catch (ResourceNotFoundException exception) {
            model.addAttribute("message", exception.getMessage());
            return "resources/not-found";
        }
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public String createForm(Model model) {
        model.addAttribute("form", new ResourceForm());
        model.addAttribute("formAction", "/resources");
        model.addAttribute("formTitle", "Create resource");
        return "resources/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public String create(
            @Valid @ModelAttribute("form") ResourceForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formAction", "/resources");
            model.addAttribute("formTitle", "Create resource");
            return "resources/form";
        }
        WorkspaceResourceResponse created = resourceService.create(toCreateRequest(form));
        redirectAttributes.addFlashAttribute("message", "Resource created.");
        return "redirect:/resources/" + created.id();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public String editForm(@PathVariable UUID id, Model model) {
        try {
            WorkspaceResourceResponse resource = resourceService.getById(id);
            ResourceForm form = new ResourceForm();
            form.setName(resource.name());
            form.setType(resource.type());
            form.setLocation(resource.location());
            form.setCapacity(resource.capacity());
            model.addAttribute("form", form);
            model.addAttribute("resourceId", id);
            model.addAttribute("formAction", "/resources/" + id + "/edit");
            model.addAttribute("formTitle", "Edit resource");
            return "resources/form";
        } catch (ResourceNotFoundException exception) {
            model.addAttribute("message", exception.getMessage());
            return "resources/not-found";
        }
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public String update(
            @PathVariable UUID id,
            @Valid @ModelAttribute("form") ResourceForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("resourceId", id);
            model.addAttribute("formAction", "/resources/" + id + "/edit");
            model.addAttribute("formTitle", "Edit resource");
            return "resources/form";
        }
        try {
            resourceService.update(id, toUpdateRequest(form));
            redirectAttributes.addFlashAttribute("message", "Resource updated.");
            return "redirect:/resources/" + id;
        } catch (ResourceNotFoundException exception) {
            model.addAttribute("message", exception.getMessage());
            return "resources/not-found";
        }
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public String statusForm(@PathVariable UUID id, Model model) {
        try {
            WorkspaceResourceResponse resource = resourceService.getById(id);
            ResourceStatusForm form = new ResourceStatusForm();
            form.setStatus(resource.status());
            model.addAttribute("resource", resource);
            model.addAttribute("form", form);
            return "resources/status";
        } catch (ResourceNotFoundException exception) {
            model.addAttribute("message", exception.getMessage());
            return "resources/not-found";
        }
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public String changeStatus(
            @PathVariable UUID id,
            @Valid @ModelAttribute("form") ResourceStatusForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            try {
                model.addAttribute("resource", resourceService.getById(id));
                return "resources/status";
            } catch (ResourceNotFoundException exception) {
                model.addAttribute("message", exception.getMessage());
                return "resources/not-found";
            }
        }
        try {
            resourceService.changeStatus(id, new WorkspaceResourceStatusChangeRequest(form.getStatus()));
            redirectAttributes.addFlashAttribute("message", "Resource status updated.");
            return "redirect:/resources/" + id;
        } catch (ResourceNotFoundException exception) {
            model.addAttribute("message", exception.getMessage());
            return "resources/not-found";
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasRole('WORKSPACE_ADMIN')")
    public String delete(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            resourceService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Resource deleted.");
            return "redirect:/resources";
        } catch (ResourceNotFoundException exception) {
            model.addAttribute("message", exception.getMessage());
            return "resources/not-found";
        } catch (ResourceHasReservationsException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/resources/" + id;
        }
    }

    private WorkspaceResourceCreateRequest toCreateRequest(ResourceForm form) {
        return new WorkspaceResourceCreateRequest(
                form.getName(),
                form.getType(),
                form.getLocation(),
                form.getCapacity()
        );
    }

    private WorkspaceResourceUpdateRequest toUpdateRequest(ResourceForm form) {
        return new WorkspaceResourceUpdateRequest(
                form.getName(),
                form.getType(),
                form.getLocation(),
                form.getCapacity()
        );
    }
}
