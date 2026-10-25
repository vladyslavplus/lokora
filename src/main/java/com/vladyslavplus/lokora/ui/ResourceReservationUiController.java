package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.common.exception.InvalidReservationPeriodException;
import com.vladyslavplus.lokora.common.exception.ReservationOverlapException;
import com.vladyslavplus.lokora.common.exception.ResourceNotFoundException;
import com.vladyslavplus.lokora.common.exception.ResourceUnavailableForReservationException;
import com.vladyslavplus.lokora.reservation.ReservationService;
import com.vladyslavplus.lokora.reservation.dto.ReservationCreateRequest;
import com.vladyslavplus.lokora.resource.ResourceStatus;
import com.vladyslavplus.lokora.resource.WorkspaceResourceService;
import com.vladyslavplus.lokora.resource.dto.WorkspaceResourceResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Controller
public class ResourceReservationUiController {

    private final ReservationService reservationService;
    private final WorkspaceResourceService resourceService;

    public ResourceReservationUiController(
            ReservationService reservationService,
            WorkspaceResourceService resourceService
    ) {
        this.reservationService = reservationService;
        this.resourceService = resourceService;
    }

    @GetMapping("/resources/{resourceId}/reserve")
    public String reserveForm(@PathVariable UUID resourceId, Model model, RedirectAttributes redirectAttributes) {
        try {
            WorkspaceResourceResponse resource = resourceService.getById(resourceId);
            if (resource.status() != ResourceStatus.AVAILABLE) {
                redirectAttributes.addFlashAttribute("errorMessage", "This resource is not available for reservation.");
                return "redirect:/resources/" + resourceId;
            }
            model.addAttribute("resource", resource);
            model.addAttribute("form", new ReservationForm());
            return "reservations/form";
        } catch (ResourceNotFoundException exception) {
            model.addAttribute("message", exception.getMessage());
            return "resources/not-found";
        }
    }

    @PostMapping("/resources/{resourceId}/reserve")
    public String create(
            @PathVariable UUID resourceId,
            @Valid @ModelAttribute("form") ReservationForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        WorkspaceResourceResponse resource;
        try {
            resource = resourceService.getById(resourceId);
        } catch (ResourceNotFoundException exception) {
            model.addAttribute("message", exception.getMessage());
            return "resources/not-found";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("resource", resource);
            return "reservations/form";
        }

        try {
            ReservationCreateRequest request = new ReservationCreateRequest(
                    resourceId,
                    parseOffsetInstant(form.getStartTime(), "startTime", bindingResult),
                    parseOffsetInstant(form.getEndTime(), "endTime", bindingResult)
            );
            if (bindingResult.hasErrors()) {
                model.addAttribute("resource", resource);
                return "reservations/form";
            }
            reservationService.create(request);
            redirectAttributes.addFlashAttribute("message", "Reservation created.");
            return "redirect:/reservations/mine";
        } catch (InvalidReservationPeriodException | ReservationOverlapException
                 | ResourceUnavailableForReservationException exception) {
            model.addAttribute("resource", resource);
            model.addAttribute("errorMessage", exception.getMessage());
            return "reservations/form";
        }
    }

    private java.time.Instant parseOffsetInstant(
            String value,
            String field,
            BindingResult bindingResult
    ) {
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeException exception) {
            bindingResult.rejectValue(
                    field,
                    "invalid",
                    "Use ISO-8601 with explicit offset, for example 2026-09-22T10:00:00+03:00"
            );
            return null;
        }
    }
}
