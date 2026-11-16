package uk.gov.dwp.uc.pairtest.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Per-type ticket totals for one purchase. Counts are long so summing large requests cannot overflow.
 */
public record TicketOrder(Map<TicketTypeRequest.Type, Long> counts) {

    public TicketOrder {
        EnumMap<TicketTypeRequest.Type, Long> copy = new EnumMap<>(TicketTypeRequest.Type.class);
        copy.putAll(counts);
        counts = Collections.unmodifiableMap(copy);
    }

    /** Assumes requests are already null-checked and positive. */
    public static TicketOrder of(TicketTypeRequest... requests) {
        EnumMap<TicketTypeRequest.Type, Long> counts = new EnumMap<>(TicketTypeRequest.Type.class);
        for (TicketTypeRequest request : requests) {
            counts.merge(request.getTicketType(), (long) request.getNoOfTickets(), Long::sum);
        }
        return new TicketOrder(counts);
    }

    public long count(TicketTypeRequest.Type type) {
        return counts.getOrDefault(type, 0L);
    }

    public long totalTickets() {
        return counts.values().stream().mapToLong(Long::longValue).sum();
    }

    public long totalCost() {
        return counts.entrySet().stream()
                .mapToLong(e -> e.getKey().getPriceInPounds() * e.getValue())
                .sum();
    }

    public long totalSeats() {
        return counts.entrySet().stream()
                .filter(e -> e.getKey().isAllocatedSeat())
                .mapToLong(Map.Entry::getValue)
                .sum();
    }
}
