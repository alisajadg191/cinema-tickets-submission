package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketOrder;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Objects;

import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */

    private static final int MAX_TICKETS_PER_PURCHASE = 25;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService,
                             SeatReservationService seatReservationService) {
        this.ticketPaymentService = Objects.requireNonNull(ticketPaymentService, "ticketPaymentService");
        this.seatReservationService = Objects.requireNonNull(seatReservationService, "seatReservationService");
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        validateAccountId(accountId);
        validateRequests(ticketTypeRequests);

        TicketOrder order = TicketOrder.of(ticketTypeRequests);
        validateOrder(order);

        // Safe narrowing: max 25 tickets bounds cost and seats well inside int.
        ticketPaymentService.makePayment(accountId, (int) order.totalCost());
        seatReservationService.reserveSeat(accountId, (int) order.totalSeats());
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Account id must be greater than zero");
        }
    }

    private void validateRequests(TicketTypeRequest[] requests) {
        if (requests == null || requests.length == 0) {
            throw new InvalidPurchaseException("At least one ticket request is required");
        }
        for (TicketTypeRequest request : requests) {
            if (request == null) {
                throw new InvalidPurchaseException("Ticket request must not be null");
            }
            if (request.getTicketType() == null) {
                throw new InvalidPurchaseException("Ticket type must not be null");
            }
            if (request.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException("Number of tickets must be greater than zero");
            }
        }
    }

    private void validateOrder(TicketOrder order) {
        if (order.totalTickets() > MAX_TICKETS_PER_PURCHASE) {
            throw new InvalidPurchaseException(
                    "Cannot purchase more than " + MAX_TICKETS_PER_PURCHASE + " tickets at a time");
        }
        long adults = order.count(ADULT);
        if (adults == 0 && (order.count(CHILD) > 0 || order.count(INFANT) > 0)) {
            throw new InvalidPurchaseException("Child and infant tickets require at least one adult ticket");
        }
        if (order.count(INFANT) > adults) {
            throw new InvalidPurchaseException("Each infant must sit on an adult's lap");
        }
    }

}
