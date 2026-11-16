package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.ADULT;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.CHILD;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.INFANT;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    private static final long ACCOUNT_ID = 1L;

    @Mock
    private TicketPaymentService paymentService;
    @Mock
    private SeatReservationService seatReservationService;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketServiceImpl(paymentService, seatReservationService);
    }

    private static TicketTypeRequest adults(int n) {
        return new TicketTypeRequest(ADULT, n);
    }

    private static TicketTypeRequest children(int n) {
        return new TicketTypeRequest(CHILD, n);
    }

    private static TicketTypeRequest infants(int n) {
        return new TicketTypeRequest(INFANT, n);
    }

    @Nested
    class ValidPurchases {

        static Stream<Arguments> validPurchases() {
            return Stream.of(
                    arguments("single adult", new TicketTypeRequest[]{adults(1)}, 25, 1),
                    arguments("adult and child", new TicketTypeRequest[]{adults(1), children(1)}, 40, 2),
                    arguments("infant takes no seat and pays nothing", new TicketTypeRequest[]{adults(1), infants(1)}, 25, 1),
                    arguments("family mix", new TicketTypeRequest[]{adults(2), children(3), infants(2)}, 95, 5),
                    arguments("same type split across requests is summed",
                            new TicketTypeRequest[]{adults(2), children(1), adults(3)}, 140, 6),
                    arguments("exactly 25 tickets including infants",
                            new TicketTypeRequest[]{adults(10), children(5), infants(10)}, 325, 15),
                    arguments("25 adults", new TicketTypeRequest[]{adults(25)}, 625, 25)
            );
        }

        @ParameterizedTest(name = "{0}: pays £{2}, reserves {3} seats")
        @MethodSource("validPurchases")
        void paysThenReservesCorrectAmounts(String scenario, TicketTypeRequest[] requests,
                                            int expectedAmount, int expectedSeats) {
            ticketService.purchaseTickets(ACCOUNT_ID, requests);

            InOrder order = inOrder(paymentService, seatReservationService);
            order.verify(paymentService).makePayment(ACCOUNT_ID, expectedAmount);
            order.verify(seatReservationService).reserveSeat(ACCOUNT_ID, expectedSeats);
            order.verifyNoMoreInteractions();
        }

        @Test
        void passesAccountIdThrough() {
            ticketService.purchaseTickets(42L, adults(1));

            InOrder order = inOrder(paymentService, seatReservationService);
            order.verify(paymentService).makePayment(42L, 25);
            order.verify(seatReservationService).reserveSeat(42L, 1);
        }
    }

    @Nested
    class InvalidPurchases {

        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {0L, -1L, Long.MIN_VALUE})
        void rejectsInvalidAccountId(Long accountId) {
            assertRejected(accountId, adults(1));
        }

        @Test
        void rejectsNullRequestArray() {
            assertRejected(ACCOUNT_ID, (TicketTypeRequest[]) null);
        }

        @Test
        void rejectsNoRequests() {
            assertRejected(ACCOUNT_ID);
        }

        static Stream<Arguments> invalidRequests() {
            return Stream.of(
                    arguments("null request element", new TicketTypeRequest[]{adults(1), null}),
                    arguments("null ticket type", new TicketTypeRequest[]{new TicketTypeRequest(null, 1)}),
                    arguments("zero tickets", new TicketTypeRequest[]{adults(0)}),
                    arguments("negative tickets", new TicketTypeRequest[]{adults(1), children(-1)}),
                    arguments("negative count used to offset a large one",
                            new TicketTypeRequest[]{adults(30), adults(-10)}),
                    arguments("26 adults", new TicketTypeRequest[]{adults(26)}),
                    arguments("26 tickets counting infants", new TicketTypeRequest[]{adults(13), infants(13)}),
                    arguments("over 25 across several requests",
                            new TicketTypeRequest[]{adults(10), children(10), adults(6)}),
                    arguments("int overflow does not bypass the limit",
                            new TicketTypeRequest[]{adults(Integer.MAX_VALUE), adults(Integer.MAX_VALUE)}),
                    arguments("child without adult", new TicketTypeRequest[]{children(1)}),
                    arguments("infant without adult", new TicketTypeRequest[]{infants(1)}),
                    arguments("child and infant without adult", new TicketTypeRequest[]{children(2), infants(1)}),
                    arguments("more infants than adults", new TicketTypeRequest[]{adults(1), infants(2)})
            );
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("invalidRequests")
        void rejectsInvalidRequests(String scenario, TicketTypeRequest[] requests) {
            assertRejected(ACCOUNT_ID, requests);
        }

        private void assertRejected(Long accountId, TicketTypeRequest... requests) {
            assertThrowsExactly(InvalidPurchaseException.class,
                    () -> ticketService.purchaseTickets(accountId, requests));
            verifyNoInteractions(paymentService, seatReservationService);
        }
    }

    @Test
    void rejectsMissingCollaborators() {
        assertThrows(NullPointerException.class, () -> new TicketServiceImpl(null, seatReservationService));
        assertThrows(NullPointerException.class, () -> new TicketServiceImpl(paymentService, null));
    }
}
