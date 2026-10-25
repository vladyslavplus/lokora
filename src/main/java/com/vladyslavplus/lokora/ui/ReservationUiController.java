package com.vladyslavplus.lokora.ui;

import com.vladyslavplus.lokora.common.exception.ReservationForbiddenException;
import com.vladyslavplus.lokora.common.exception.ReservationNotFoundException;
import com.vladyslavplus.lokora.common.exception.ReservationStateConflictException;
import com.vladyslavplus.lokora.reservation.ReservationService;
import com.vladyslavplus.lokora.reservation.dto.ReservationPageResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

@Controller
@RequestMapping("/reservations")
public class ReservationUiController {

    private final ReservationService reservationService;

    public ReservationUiController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/mine")
    public String listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size,
            Model model
    ) {
        ReservationPageResponse pageResponse = reservationService.listMine(page, size);
        model.addAttribute("page", pageResponse);
        return "reservations/mine";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        try {
            reservationService.cancel(id);
            redirectAttributes.addFlashAttribute("message", "Reservation cancelled.");
            return "redirect:/reservations/mine";
        } catch (ReservationNotFoundException exception) {
            model.addAttribute("message", exception.getMessage());
            return "reservations/not-found";
        } catch (ReservationForbiddenException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/reservations/mine";
        } catch (ReservationStateConflictException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/reservations/mine";
        }
    }
}
