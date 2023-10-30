package ru.practicum.explore_with_me.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.explore_with_me.model.ParticipationRequest;

import java.util.List;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    @Query("select p from ParticipationRequest p where p.requester.id = :userId order by p.id")
    List<ParticipationRequest> findAllByRequesterIdOrderByIdAsc(Long userId);

    @Query("select count(p) from ParticipationRequest p where p.requester.id = :userId and p.event.id = :eventId")
    int countAllByRequester_IdAndEvent_Id(Long userId, Long eventId);

    @Query("select p from ParticipationRequest p where p.event.id = :eventId and p.statusRequest = 'CONFIRMED'")
    List<ParticipationRequest> findConfirmedRequests(Long eventId);

    @Query("select p from ParticipationRequest p where p.statusRequest = 'CONFIRMED' and p.event.id in :ids")
    List<ParticipationRequest> findConfirmedRequests(List<Long> ids);

    List<ParticipationRequest> findByIdInOrderByIdAsc(List<Long> requestIds);

    List<ParticipationRequest> findAllByEvent_Id(Long eventId);
}
