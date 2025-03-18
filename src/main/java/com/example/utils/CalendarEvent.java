package com.example.utils;

import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CalendarEvent {
    private String title;
    private String start;
    private String end;
    @Setter
    private String color;
    private String inviteLink;  // ✅ Added inviteLink field

    // 🎨 Predefined color palette
    private static final List<String> COLOR_PALETTE = List.of(
            "#D50000", "#F4511E", "#F6BF26", "#33B679", "#0B8043",
            "#3F51B5", "#7986CB", "#8E24AA"
    );

    private static final Random RANDOM = new Random();
    private static int colorIndex = 0;
    private static final Map<String, String> eventColorMap = new HashMap<>();

    public CalendarEvent(String title, String start, String end, String inviteLink) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.inviteLink = inviteLink; // ✅ Store invite link
        this.color = assignColor(title);
    }

    private static synchronized String assignColor(String eventName) {
        if (eventColorMap.containsKey(eventName)) {
            return eventColorMap.get(eventName);
        }

        String baseColor = COLOR_PALETTE.get(colorIndex % COLOR_PALETTE.size());
        colorIndex++;

        if (eventColorMap.containsValue(baseColor)) {
            baseColor = adjustBrightness(baseColor, (RANDOM.nextBoolean() ? 20 : -20));
        }

        eventColorMap.put(eventName, baseColor);
        return baseColor;
    }

    private static String adjustBrightness(String hexColor, int adjustment) {
        int r = Integer.parseInt(hexColor.substring(1, 3), 16);
        int g = Integer.parseInt(hexColor.substring(3, 5), 16);
        int b = Integer.parseInt(hexColor.substring(5, 7), 16);

        r = Math.max(0, Math.min(255, r + adjustment));
        g = Math.max(0, Math.min(255, g + adjustment));
        b = Math.max(0, Math.min(255, b + adjustment));

        return String.format("#%02X%02X%02X", r, g, b);
    }

    // Getters
    public String getTitle() { return title; }
    public String getStart() { return start; }
    public String getEnd() { return end; }
    public String getColor() { return color; }
    public String getInviteLink() { return inviteLink; }  // ✅ Add getter for inviteLink

    @Override
    public String toString() {
        return "CalendarEvent{title='" + title + "', start='" + start + "', end='" + end + "', color='" + color + "', inviteLink='" + inviteLink + "'}";
    }

}
