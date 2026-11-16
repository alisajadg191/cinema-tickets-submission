package uk.gov.dwp.uc.pairtest.domain;

/**
 * Immutable Object
 */
public final class TicketTypeRequest {

    private final int noOfTickets;
    private final Type type;

    public TicketTypeRequest(Type type, int noOfTickets) {
        this.type = type;
        this.noOfTickets = noOfTickets;
    }

    public int getNoOfTickets() {
        return noOfTickets;
    }

    public Type getTicketType() {
        return type;
    }

    public enum Type {
        ADULT(25, true),
        CHILD(15, true),
        INFANT(0, false);

        private final int priceInPounds;
        private final boolean allocatedSeat;

        Type(int priceInPounds, boolean allocatedSeat) {
            this.priceInPounds = priceInPounds;
            this.allocatedSeat = allocatedSeat;
        }

        public int getPriceInPounds() {
            return priceInPounds;
        }

        public boolean isAllocatedSeat() {
            return allocatedSeat;
        }
    }

}
