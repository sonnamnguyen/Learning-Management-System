package com.example.location;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping
    public String listLocations(Model model) {
        model.addAttribute("locations", locationService.findAll());
        return "location/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("location", new Location());
        return "location/create";
    }

    @PostMapping("/create")
    public String create(@ModelAttribute Location location) {
        locationService.save(location);
        return "redirect:/locations";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model) {
        Location location = locationService.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid location Id:" + id));
        model.addAttribute("location", location);
        return "location/edit";
    }

    @PostMapping("/edit/{id}")
    public String update(@PathVariable Long id, @ModelAttribute Location location) {
        location.setId(id);
        locationService.save(location);
        return "redirect:/locations";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        locationService.deleteById(id);
        return "redirect:/locations";
    }
}
