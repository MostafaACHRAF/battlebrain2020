package com.sqli.safeSeat.services.impl;

import com.sqli.safeSeat.enums.Availability;
import com.sqli.safeSeat.models.Floor;
import com.sqli.safeSeat.models.Seat;
import com.sqli.safeSeat.repositories.SeatRepository;
import com.sqli.safeSeat.services.FloorService;
import com.sqli.safeSeat.services.ReservationService;
import com.sqli.safeSeat.services.SeatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final FloorService floorService;
    private final ReservationService reservationService;

    @Autowired public SeatServiceImpl(SeatRepository seatRepository,
                                      FloorService floorService,
                                      ReservationService reservationService) {
        this.seatRepository = seatRepository;
        this.floorService = floorService;
        this.reservationService = reservationService;
    }

    @Override public double distanceBetween(Seat seat1, Seat seat2) {
        if (seat1 == null || seat2 == null)
            return -1;
        final int x1 = seat1.getxposition();
        final int y1 = seat1.getyposition();
        final int x2 = seat2.getxposition();
        final int y2 = seat2.getyposition();
        return Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2));
    }

    @Override public boolean isEmpty(Seat seat) {
        return seat == null || seat.getAvailability().equals(Availability.AVAILABLE);
    }

    @Override public void save(Seat seat) {
        this.seatRepository.save(seat);
    }

    @Override public boolean canBeReserved(Floor floor, Seat seat) {
        long
                nbrOfReservedSeats =
                floor.getSeats().stream().filter(s -> s.getAvailability().equals(Availability.RESERVED)).count();
        if (nbrOfReservedSeats <= Math.floor(floor.getSeats().size() * 0.5) && seat.getAvailability()
                .equals(Availability.AVAILABLE)) {
            int x = seat.getxposition();
            int y = seat.getyposition();
            Seat frontSeat = this.findByPosition(x, y - 10);
            Seat behindSeat = this.findByPosition(x, y + 10);
            Seat leftSeat = this.findByPosition(x - 10, y);
            Seat rightSeat = this.findByPosition(x + 10, y);
            return isEmpty(frontSeat) && isEmpty(behindSeat) && isEmpty(leftSeat) && isEmpty(rightSeat);
        }
        return false;
    }

    @Override public Seat findByPosition(int x, int y) {
        return this.seatRepository.findByXpositionAndYposition(x, y);
    }

    @Override public Seat findById(int id) {
        Optional<Seat> searchableSeat = this.seatRepository.findById(id);
        return searchableSeat.orElse(null);
    }

    @Override public List<Seat> availableSeatsByFloor(int floorId) {
        return this.floorService.findById(floorId)
                .getSeats()
                .stream()
                .filter(seat -> seat.getAvailability().equals(Availability.AVAILABLE))
                .collect(Collectors.toList());
    }

    @Override public List<Seat> reservedSeatsByTeamAndFloor(int floorId, int teamId) {
        List<Seat> seats = new ArrayList<>();
        final Floor floor = this.floorService.findById(floorId);
        this.reservationService.findAllByTeam(teamId).forEach(reservation -> {
            if (floor.getSeats().contains(reservation.getSeat())) {
                seats.add(reservation.getSeat());
            }
        });
        return seats;
    }


}
